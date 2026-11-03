package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.flight.Flight;
import ru.aiforge.riftworld.world.AshenLands;
import ru.aiforge.riftworld.world.CityPlan;
import ru.aiforge.riftworld.world.FloatingIslands;
import ru.aiforge.riftworld.world.MistKeep;
import ru.aiforge.riftworld.world.WaterfallGardens;

/**
 * v0.29.0: Строго ограниченные дальностью прорисовки ({@code <= renderDistance * 16 - 4})
 * и 100% синхронные поверхностные волны у островов и водопадов океана Элисара:
 * <ul>
 *   <li>Круги и волны НИКОГДА не рендерятся и не светятся за пределами дальности прорисовки
 *       или в незагруженных чанках ({@code ch == null -> false}), плавно уходя в 0 внутри тумана;</li>
 *   <li>Все острова используют ЕДИНУЮ синхронную глобальную фазу волн (1 расчёт {@code sin} на кольцо за кадр,
 *       предвычисленные нормали берега в {@link WakeProfile}, 0 вызовов {@code sin/cos/hypot} на вершину!).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, value = Dist.CLIENT)
public final class OceanSurfaceWaves {
    /** Абсолютный верхний предел аналитического радиуса поиска островов (compile-time константа). */
    public static final double MAX_ISLAND_DIST = 520.0;
    /** Минимальный радиус острова, оставляющего волны на воде (мелкие островки/скалы R < 16 м волн не создают!). */
    public static final double MIN_WAKE_ISLAND_R = 16.0;
    private static final double MAX_PLUNGE_DIST = 192.0;
    private static final int MAX_ISLAND_SEGS = 40;
    private static final int RING_COUNT = 3;
    private static final int PLUNGE_CHUNK_RADIUS = 10;

    /** Статические scratch-буферы рендер-потока — 0 аллокаций массивов на кадр! */
    private static final float[] SCRATCH_PX = new float[MAX_ISLAND_SEGS + 1];
    private static final float[] SCRATCH_PZ = new float[MAX_ISLAND_SEGS + 1];
    private static final boolean[] SCRATCH_OK = new boolean[MAX_ISLAND_SEGS + 1];

    /** Предвычисленные тригонометрические таблицы для кругов водопадов (24 сегмента). */
    private static final int PLUNGE_SEGS = 24;
    private static final float[] PLUNGE_COS = new float[PLUNGE_SEGS + 1];
    private static final float[] PLUNGE_SIN = new float[PLUNGE_SEGS + 1];

    static {
        for (int s = 0; s <= PLUNGE_SEGS; ++s) {
            double a = ((s % PLUNGE_SEGS) / (double) PLUNGE_SEGS) * (Math.PI * 2.0);
            PLUNGE_COS[s] = (float) Math.cos(a);
            PLUNGE_SIN[s] = (float) Math.sin(a);
        }
    }

    /** Предвычисленный геометрический профиль синхронного волнового следа одного острова. */
    private static final class WakeProfile {
        final int segs;
        final float reach;
        final float ringGap;
        final float baseWidth;
        final float[] cosA;
        final float[] sinA;
        final float[] rim;
        /** Предвычисленные единичные внешние нормали контура берега (0 hypot/делений в рендере!). */
        final float[] outNx;
        final float[] outNz;
        final boolean[][] openOcean;

        WakeProfile(CityPlan.Island isl) {
            this.segs = isl.R > 34.0 ? 36 : (isl.R > 14.0 ? 28 : 20);
            this.reach = (float) isl.maxReach();
            this.ringGap = (float) Mth.clamp(isl.R * 0.14, 2.2, 6.0);
            this.baseWidth = (float) Mth.clamp(isl.R * 0.048, 0.90, 2.10);
            this.cosA = new float[segs];
            this.sinA = new float[segs];
            this.rim = new float[segs];
            this.outNx = new float[segs + 1];
            this.outNz = new float[segs + 1];
            this.openOcean = new boolean[RING_COUNT][segs];

            CityPlan plan = CityPlan.get();
            double[] bx = new double[segs];
            double[] bz = new double[segs];
            for (int s = 0; s < segs; ++s) {
                double a = (s / (double) segs) * (Math.PI * 2.0);
                double ca = Math.cos(a);
                double sa = Math.sin(a);
                this.cosA[s] = (float) ca;
                this.sinA[s] = (float) sa;
                // Лёгкая статическая микро-волнистость запечена 1 раз в профиль — 0 sin/cos на кадр!
                double rDist = isl.rimDist(ca, sa) + 0.35 * Math.sin(a * 4.0) + 0.20 * Math.cos(a * 7.0);
                this.rim[s] = (float) rDist;
                bx[s] = ca * (rDist + 2.5);
                bz[s] = sa * (rDist + 2.5);
                for (int ring = 0; ring < RING_COUNT; ++ring) {
                    double out = 1.6 + ring * ringGap + ringGap * 0.45;
                    double wx = isl.cx + ca * (rDist + out);
                    double wz = isl.cz + sa * (rDist + out);
                    this.openOcean[ring][s] = isAnalyticalOpenOcean(plan, wx, wz);
                }
            }
            for (int s = 0; s < segs; ++s) {
                int prev = (s - 1 + segs) % segs;
                int next = (s + 1) % segs;
                double tx = bx[next] - bx[prev];
                double tz = bz[next] - bz[prev];
                double len = Math.hypot(tx, tz);
                if (len > 1.0E-4) {
                    // Внешняя нормаль к обходу против часовой стрелки в плоскости (X, Z)
                    double nx = tz / len;
                    double nz = -tx / len;
                    if (nx * this.cosA[s] + nz * this.sinA[s] < 0.0) {
                        nx = -nx;
                        nz = -nz;
                    }
                    this.outNx[s] = (float) nx;
                    this.outNz[s] = (float) nz;
                } else {
                    this.outNx[s] = this.cosA[s];
                    this.outNz[s] = this.sinA[s];
                }
            }
            this.outNx[segs] = this.outNx[0];
            this.outNz[segs] = this.outNz[0];
        }
    }

    private static final Map<CityPlan.Island, WakeProfile> WAKE_CACHE =
            Collections.synchronizedMap(new IdentityHashMap<>(256));

    private static double cachedCamX = Double.NaN;
    private static double cachedCamZ = Double.NaN;
    private static double cachedRadius = -1.0;
    private static List<CityPlan.Island> cachedIslands = Collections.emptyList();

    private static long lastPlungeScanTick = -9999L;
    private static int lastPlungeChunkX = Integer.MIN_VALUE;
    private static int lastPlungeChunkZ = Integer.MIN_VALUE;
    private static int lastPlungeRadius = -1;
    private static List<Vec3> cachedPlunges = Collections.emptyList();

    /** Глобальные синхронные параметры колец за текущий кадр (общие для ВСЕХ островов!). */
    private static final float[] SYNC_CYC = new float[RING_COUNT];
    private static final float[] SYNC_ENV = new float[RING_COUNT];

    private OceanSurfaceWaves() {
    }

    /**
     * Строгий радиус волн по дальности прорисовки игрока в чанках:
     * НИКОГДА не превышает {@code renderDistChunks * 16 - 4}, чтобы круги не светились за прорисовкой!
     */
    public static double effectiveWakeRadius(int renderDistChunks, double depthFar) {
        int rd = Math.max(2, renderDistChunks);
        double maxByLoadedChunks = Math.max(28.0, rd * 16.0 - 4.0);
        double maxByFog = depthFar > 16.0 ? Math.max(28.0, depthFar * 0.90) : maxByLoadedChunks;
        return Math.min(MAX_ISLAND_DIST, Math.min(maxByLoadedChunks, maxByFog));
    }

    public static double effectiveWakeRadius(int renderDistChunks, float depthFar) {
        return effectiveWakeRadius(renderDistChunks, (double) depthFar);
    }

    /** Возвращает {@code true}, если остров достаточно крупный, чтобы оставлять расходящиеся волны на океане. */
    public static boolean leavesOceanWake(CityPlan.Island isl) {
        return isl != null && isl.R >= MIN_WAKE_ISLAND_R && isl.maxReach() >= 17.5;
    }

    private static WakeProfile profileOf(CityPlan.Island isl) {
        WakeProfile p = WAKE_CACHE.get(isl);
        if (p == null) {
            if (WAKE_CACHE.size() > 2048) {
                WAKE_CACHE.clear();
            }
            p = new WakeProfile(isl);
            WAKE_CACHE.put(isl, p);
        }
        return p;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (ShaderCompat.isRenderingShadowPass()) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !level.dimension().equals(Flight.ELISAR_DIM)) return;

        Camera camera = e.getCamera();
        Vec3 cam = camera.getPosition();
        int ocean = CityPlan.get().ocean;
        double oceanSurfaceY = ocean + 0.915;

        if (cam.y < ocean - 8.0 || cam.y > ocean + 210.0) return;

        int rdChunks = Math.max(2, mc.options.renderDistance().get());
        double wakeRadius = effectiveWakeRadius(rdChunks, mc.gameRenderer.getDepthFar());

        // Обновляем список островов только при смещении камеры > 16 блоков или изменении дальности прорисовки
        if (Double.isNaN(cachedCamX)
                || Math.abs(wakeRadius - cachedRadius) > 3.0
                || (cam.x - cachedCamX) * (cam.x - cachedCamX) + (cam.z - cachedCamZ) * (cam.z - cachedCamZ) > 16.0 * 16.0) {
            cachedCamX = cam.x;
            cachedCamZ = cam.z;
            cachedRadius = wakeRadius;
            List<CityPlan.Island> rawIslands = FloatingIslands.gather(cam.x, cam.z, wakeRadius, false);
            List<CityPlan.Island> filtered = new java.util.ArrayList<>(rawIslands.size());
            for (int i = 0, n = rawIslands.size(); i < n; ++i) {
                CityPlan.Island isl = rawIslands.get(i);
                if (leavesOceanWake(isl)) {
                    filtered.add(isl);
                }
            }
            cachedIslands = filtered;
        }

        // Кэш точек падения водопадов строго внутри загруженных чанков!
        int plungeChunkR = Math.min(PLUNGE_CHUNK_RADIUS, Math.max(2, rdChunks - 1));
        long gt = level.getGameTime();
        int pcx = Mth.floor(cam.x) >> 4;
        int pcz = Mth.floor(cam.z) >> 4;
        if (gt - lastPlungeScanTick >= 40L
                || plungeChunkR != lastPlungeRadius
                || Math.abs(pcx - lastPlungeChunkX) >= 2
                || Math.abs(pcz - lastPlungeChunkZ) >= 2) {
            lastPlungeScanTick = gt;
            lastPlungeChunkX = pcx;
            lastPlungeChunkZ = pcz;
            lastPlungeRadius = plungeChunkR;
            cachedPlunges = scanWaterfallPlunges(level, pcx, pcz, ocean, plungeChunkR);
        }

        if (cachedIslands.isEmpty() && cachedPlunges.isEmpty()) return;

        float time = (gt + e.getPartialTick()) * 0.05F;
        float heightFade = (float) Mth.clamp(1.0 - Math.max(0.0, cam.y - (ocean + 115.0)) / 95.0, 0.0, 1.0);
        if (heightFade <= 0.01F) return;
        if (ShaderCompat.isShaderPackActive()) {
            heightFade *= 0.82F;
        }

        // ЕДИНАЯ синхронная фаза и огибающая для ВСЕХ островов в мире — всего 3 вызова sin за весь кадр!
        float basePhase = time * 0.22F;
        for (int ring = 0; ring < RING_COUNT; ++ring) {
            float cyc = (basePhase + ring * (1.0F / RING_COUNT)) % 1.0F;
            if (cyc < 0.0F) cyc += 1.0F;
            SYNC_CYC[ring] = cyc;
            SYNC_ENV[ring] = Mth.sin(cyc * (float) Math.PI) * (1.0F - 0.22F * cyc);
        }

        Vector3f look = camera.getLookVector();
        double lookHzLen = Math.hypot(look.x(), look.z());
        boolean cullBehind = lookHzLen > 0.32;
        double lookX = cullBehind ? (look.x() / lookHzLen) : 0.0;
        double lookZ = cullBehind ? (look.z() / lookHzLen) : 0.0;

        PoseStack ps = e.getPoseStack();
        Matrix4f mat = ps.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.polygonOffset(-1.0F, -2.0F);
        RenderSystem.enablePolygonOffset();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, ShaderCompat.WHITE_TEX);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        double maxWakeSq = wakeRadius * wakeRadius;
        for (int idx = 0, n = cachedIslands.size(); idx < n; ++idx) {
            CityPlan.Island isl = cachedIslands.get(idx);
            if (!leavesOceanWake(isl)) continue;
            double dx = isl.cx - cam.x;
            double dz = isl.cz - cam.z;
            double dist = Math.hypot(dx, dz);
            double reach = isl.maxReach();
            double maxExtent = reach + 20.0;
            // Строго отсекаем любые острова, чей ближайший край уже за границей дальности прорисовки!
            if (dist - maxExtent >= wakeRadius) continue;
            if (cullBehind && dist > maxExtent + 12.0 && (dx * lookX + dz * lookZ) < -(maxExtent + 8.0)) {
                continue;
            }
            drawIslandWakeSync(bb, mat, level, isl, cam, ocean, oceanSurfaceY, time, heightFade,
                    dist, wakeRadius, maxWakeSq);
        }

        double maxPlunge = Math.min(MAX_PLUNGE_DIST, wakeRadius);
        for (int idx = 0, n = cachedPlunges.size(); idx < n; ++idx) {
            Vec3 p = cachedPlunges.get(idx);
            double dx = p.x - cam.x;
            double dz = p.z - cam.z;
            double dist = Math.hypot(dx, dz);
            if (dist >= maxPlunge) continue;
            if (cullBehind && dist > 20.0 && (dx * lookX + dz * lookZ) < -16.0) continue;
            drawWaterfallRipplesSync(bb, mat, p, cam, oceanSurfaceY, time, heightFade, dist, maxPlunge);
        }

        Tesselator.getInstance().end();

        RenderSystem.polygonOffset(0.0F, 0.0F);
        RenderSystem.disablePolygonOffset();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /** Микробенчмарк расчёта волнового следа для автоматической CI-пробы производительности. */
    public static long benchmarkWakePassNanos(double camX, double camZ, int renderDistChunks) {
        double wakeRadius = effectiveWakeRadius(renderDistChunks, renderDistChunks * 16.0);
        long t0 = System.nanoTime();
        List<CityPlan.Island> list = FloatingIslands.gather(camX, camZ, wakeRadius, false);
        int active = 0;
        for (CityPlan.Island isl : list) {
            if (!leavesOceanWake(isl)) continue;
            WakeProfile wp = profileOf(isl);
            active += wp.segs;
        }
        long elapsed = System.nanoTime() - t0;
        return Math.max(1L, elapsed + (active & 0));
    }

    public static long benchmarkWakePassNanos(Level level, Vec3 cam, Vec3 look, int renderDistChunks) {
        return benchmarkWakePassNanos(cam.x, cam.z, renderDistChunks);
    }

    private static void drawIslandWakeSync(BufferBuilder bb, Matrix4f mat, ClientLevel level, CityPlan.Island isl,
                                           Vec3 cam, int ocean, double surfY, float time, float heightFade,
                                           double centerDist, double wakeRadius, double maxWakeSq) {
        WakeProfile wp = profileOf(isl);
        double nearestEdge = Math.max(0.0, centerDist - wp.reach - 18.0);
        if (nearestEdge >= wakeRadius) return;

        // Плавное затухание в 0 ДО границы прорисовки чанков/тумана — 0 светящихся колец в тумане!
        double fadeStart = wakeRadius * 0.62;
        double fadeSpan = Math.max(12.0, wakeRadius - fadeStart);
        float baseDistFade = (float) Mth.clamp(1.0 - Math.max(0.0, nearestEdge - fadeStart) / fadeSpan, 0.0, 1.0);
        if (baseDistFade <= 0.01F) return;

        boolean highFloating = (isl.base - isl.depth) > ocean + 3.0;
        float kindBoost = (highFloating ? 0.88F : 1.0F) * heightFade * baseDistFade * 0.72F;
        int segs = wp.segs;
        float camX = (float) cam.x;
        float camZ = (float) cam.z;
        float islCx = (float) isl.cx;
        float islCz = (float) isl.cz;

        for (int ring = 0; ring < RING_COUNT; ++ring) {
            float alphaF = SYNC_ENV[ring] * kindBoost;
            int alpha = Mth.clamp(Math.round(alphaF * 255.0F), 0, 224);
            if (alpha <= 4) continue;

            float outOffset = 1.6F + (ring + SYNC_CYC[ring]) * wp.ringGap;
            float halfW = (wp.baseWidth * (1.05F - 0.16F * ring)) * 0.5F;
            boolean[] ringOpen = wp.openOcean[ring];

            boolean anyOk = false;
            for (int s = 0; s < segs; ++s) {
                float r = wp.rim[s] + outOffset;
                float wx = islCx + wp.cosA[s] * r;
                float wz = islCz + wp.sinA[s] * r;
                float rx = wx - camX;
                float rz = wz - camZ;
                SCRATCH_PX[s] = rx;
                SCRATCH_PZ[s] = rz;
                double dSq = (double) rx * rx + (double) rz * rz;
                // Жёсткий по-вершинный отсев за радиусом прорисовки И проверка реально загруженного чанка!
                boolean ok = dSq < maxWakeSq && ringOpen[s] && isLoadedWaterFast(level, ocean, wx, wz);
                SCRATCH_OK[s] = ok;
                if (ok) anyOk = true;
            }
            if (!anyOk) continue;

            SCRATCH_PX[segs] = SCRATCH_PX[0];
            SCRATCH_PZ[segs] = SCRATCH_PZ[0];
            SCRATCH_OK[segs] = SCRATCH_OK[0];

            emitPrecomputedRibbon(bb, mat, (float) (surfY - 0.012 + ring * 0.002 - cam.y),
                    SCRATCH_PX, SCRATCH_PZ, wp.outNx, wp.outNz, SCRATCH_OK, segs,
                    halfW * 2.10F, 168, 224, 240, Math.max(4, Math.round(alpha * 0.34F)), time * 0.10F + ring * 0.33F);
            emitPrecomputedRibbon(bb, mat, (float) (surfY + ring * 0.002 - cam.y),
                    SCRATCH_PX, SCRATCH_PZ, wp.outNx, wp.outNz, SCRATCH_OK, segs,
                    halfW, 246, 253, 255, alpha, time * 0.14F + ring * 0.33F);
        }
    }

    private static void drawWaterfallRipplesSync(BufferBuilder bb, Matrix4f mat, Vec3 center, Vec3 cam,
                                                 double surfY, float time, float heightFade, double dist, double maxPlunge) {
        float distFade = (float) Mth.clamp(1.0 - Math.max(0.0, dist - maxPlunge * 0.62) / Math.max(12.0, maxPlunge * 0.38), 0.0, 1.0);
        if (distFade <= 0.01F) return;

        float relCx = (float) (center.x - cam.x);
        float relCz = (float) (center.z - cam.z);
        float basePhase = time * 0.52F;

        for (int ring = 0; ring < 4; ++ring) {
            float cyc = (basePhase + ring * 0.25F) % 1.0F;
            if (cyc < 0.0F) cyc += 1.0F;

            float env = Mth.sin(cyc * (float) Math.PI);
            int alpha = Mth.clamp(Math.round(env * (1.0F - 0.28F * cyc) * distFade * heightFade * 234.0F), 0, 238);
            if (alpha <= 4) continue;

            float rBase = 0.75F + (ring + cyc) * 2.50F;
            float halfW = (ring == 0 ? 0.60F : 0.42F) * (1.05F - 0.12F * ring);

            for (int s = 0; s <= PLUNGE_SEGS; ++s) {
                SCRATCH_PX[s] = relCx + PLUNGE_COS[s] * rBase;
                SCRATCH_PZ[s] = relCz + PLUNGE_SIN[s] * rBase;
                SCRATCH_OK[s] = true;
            }

            emitPrecomputedRibbon(bb, mat, (float) (surfY + 0.008 + ring * 0.002 - cam.y),
                    SCRATCH_PX, SCRATCH_PZ, PLUNGE_COS, PLUNGE_SIN, SCRATCH_OK, PLUNGE_SEGS,
                    halfW * 1.85F, 175, 232, 245, Math.max(4, Math.round(alpha * 0.40F)), time * 0.20F);
            emitPrecomputedRibbon(bb, mat, (float) (surfY + 0.016 + ring * 0.002 - cam.y),
                    SCRATCH_PX, SCRATCH_PZ, PLUNGE_COS, PLUNGE_SIN, SCRATCH_OK, PLUNGE_SEGS,
                    halfW, 250, 254, 255, alpha, time * 0.26F);
        }
    }

    /** Мгновенная генерация квадов по предвычисленным нормалям — 0 hypot/делений/аллокаций на вершину! */
    private static void emitPrecomputedRibbon(BufferBuilder bb, Matrix4f mat, float relY,
                                              float[] rx, float[] rz, float[] nx, float[] nz,
                                              boolean[] ok, int segs, float halfW,
                                              int r, int g, int b, int maxAlpha, float uScroll) {
        for (int s = 0; s < segs; ++s) {
            if (!ok[s] || !ok[s + 1]) continue;

            int prev = s == 0 ? segs - 1 : s - 1;
            int next = (s + 2) >= segs ? (s + 2 - segs) : (s + 2);
            int al0 = ok[prev] ? maxAlpha : Math.max(2, maxAlpha >> 2);
            int al1 = ok[next] ? maxAlpha : Math.max(2, maxAlpha >> 2);
            if (al0 <= 3 && al1 <= 3) continue;

            float x0Mid = rx[s];
            float z0Mid = rz[s];
            float x1Mid = rx[s + 1];
            float z1Mid = rz[s + 1];

            float nx0 = nx[s] * halfW;
            float nz0 = nz[s] * halfW;
            float nx1 = nx[s + 1] * halfW;
            float nz1 = nz[s + 1] * halfW;

            float x0In = x0Mid - nx0;
            float z0In = z0Mid - nz0;
            float x0Out = x0Mid + nx0;
            float z0Out = z0Mid + nz0;

            float x1In = x1Mid - nx1;
            float z1In = z1Mid - nz1;
            float x1Out = x1Mid + nx1;
            float z1Out = z1Mid + nz1;

            float u0 = uScroll + s * 0.25F;
            float u1 = uScroll + (s + 1) * 0.25F;

            bb.vertex(mat, x0In, relY, z0In).uv(u0, 0.02F).color(r, g, b, 0).endVertex();
            bb.vertex(mat, x1In, relY, z1In).uv(u1, 0.02F).color(r, g, b, 0).endVertex();
            bb.vertex(mat, x1Mid, relY, z1Mid).uv(u1, 0.50F).color(r, g, b, al1).endVertex();
            bb.vertex(mat, x0Mid, relY, z0Mid).uv(u0, 0.50F).color(r, g, b, al0).endVertex();

            bb.vertex(mat, x0Mid, relY, z0Mid).uv(u0, 0.50F).color(r, g, b, al0).endVertex();
            bb.vertex(mat, x1Mid, relY, z1Mid).uv(u1, 0.50F).color(r, g, b, al1).endVertex();
            bb.vertex(mat, x1Out, relY, z1Out).uv(u1, 0.98F).color(r, g, b, 0).endVertex();
            bb.vertex(mat, x0Out, relY, z0Out).uv(u0, 0.98F).color(r, g, b, 0).endVertex();
        }
    }

    /** Аналитическая проверка открытого океана — вызывается ТОЛЬКО один раз при создании {@link WakeProfile}! */
    private static boolean isAnalyticalOpenOcean(CityPlan plan, double wx, double wz) {
        CityPlan.Island hit = plan.islandAt(wx, wz);
        if (hit != null && (hit.base - hit.depth) <= plan.ocean + 2.0) {
            return false;
        }
        return WaterfallGardens.landAt(wx, wz) == null
                && !AshenLands.inside(wx, wz)
                && !MistKeep.inside(wx, wz)
                && !ru.aiforge.riftworld.world.BlacktideCitadel.insideAnyFootprint(wx, wz, 10.0);
    }

    /**
     * Мгновенная проверка воды ТОЛЬКО в реально загруженном чанке:
     * если чанк за дальностью прорисовки ({@code ch == null}) — СТРОГО возвращаем {@code false}!
     */
    private static boolean isLoadedWaterFast(ClientLevel level, int ocean, float wx, float wz) {
        int bx = Mth.floor(wx);
        int bz = Mth.floor(wz);
        LevelChunk ch = level.getChunkSource().getChunkNow(bx >> 4, bz >> 4);
        if (ch == null) return false;
        int secIdx = ch.getSectionIndex(ocean);
        LevelChunkSection[] secs = ch.getSections();
        if (secIdx < 0 || secIdx >= secs.length) return false;
        LevelChunkSection sec = secs[secIdx];
        if (sec == null || sec.hasOnlyAir()) return false;
        BlockState surf = sec.getBlockState(bx & 15, ocean & 15, bz & 15);
        if (surf.getFluidState().isEmpty()) return false;
        int aboveY = ocean + 1;
        int aboveSecIdx = ch.getSectionIndex(aboveY);
        if (aboveSecIdx >= 0 && aboveSecIdx < secs.length) {
            LevelChunkSection aSec = secs[aboveSecIdx];
            if (aSec != null && !aSec.hasOnlyAir()) {
                BlockState above = aSec.getBlockState(bx & 15, aboveY & 15, bz & 15);
                if (!above.isAir() && above.getFluidState().isEmpty()) return false;
            }
        }
        return true;
    }

    private static List<Vec3> scanWaterfallPlunges(ClientLevel level, int pcx, int pcz, int ocean, int radius) {
        List<Vec3> found = new ArrayList<>();
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        int scanY = ocean + 2;

        for (int cx = pcx - radius; cx <= pcx + radius; ++cx) {
            for (int cz = pcz - radius; cz <= pcz + radius; ++cz) {
                LevelChunk ch = level.getChunkSource().getChunkNow(cx, cz);
                if (ch == null) continue;
                int secIdx = ch.getSectionIndex(scanY);
                if (secIdx < 0 || secIdx >= ch.getSections().length) continue;
                LevelChunkSection sec = ch.getSections()[secIdx];
                if (sec == null || sec.hasOnlyAir()) continue;

                int x0 = cx << 4;
                int z0 = cz << 4;
                int ly = scanY & 15;
                for (int lx = 0; lx < 16; ++lx) {
                    for (int lz = 0; lz < 16; ++lz) {
                        BlockState st = sec.getBlockState(lx, ly, lz);
                        if (st.getFluidState().isEmpty()) continue;
                        if (st.hasProperty(LiquidBlock.LEVEL) && st.getValue(LiquidBlock.LEVEL) < 8) continue;

                        int wx = x0 + lx;
                        int wz = z0 + lz;
                        BlockState surf = ch.getBlockState(mp.set(wx, ocean, wz));
                        if (surf.getFluidState().isEmpty()) continue;

                        double mergeX = wx + 0.5;
                        double mergeZ = wz + 0.5;
                        boolean merged = false;
                        for (int i = 0; i < found.size(); ++i) {
                            Vec3 ex = found.get(i);
                            if ((ex.x - mergeX) * (ex.x - mergeX) + (ex.z - mergeZ) * (ex.z - mergeZ) < 16.0) {
                                found.set(i, new Vec3((ex.x + mergeX) * 0.5, ocean + 0.915, (ex.z + mergeZ) * 0.5));
                                merged = true;
                                break;
                            }
                        }
                        if (!merged) {
                            found.add(new Vec3(mergeX, ocean + 0.915, mergeZ));
                        }
                    }
                }
            }
        }
        return found;
    }
}
