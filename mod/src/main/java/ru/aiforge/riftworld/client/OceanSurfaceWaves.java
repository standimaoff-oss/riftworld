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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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
 * Живая морская гладь Элисара (v0.28.0 — полная оптимизация под любую дальность прорисовки, включая 2 чанка!):
 * <ol>
 *   <li>Радиус волновых контуров строго адаптируется к {@code options.renderDistance} и {@code depthFar}:
 *       при 2 чанках прорисовки рендерятся только видимые волны вблизи камеры (0 невидимых островов за стеной тумана!),
 *       при 12+ чанках — вся широкая панорама архипелага до {@link #MAX_ISLAND_DIST}.</li>
 *   <li>Профиль береговой кромки ({@code rimDist}, {@code cos/sin}) и аналитическая проверка открытого моря
 *       кэшируются ОДИН РАЗ на каждый остров в {@link #WAKE_CACHE} (0 вызовов генератора регионов и 0 аллокаций
 *       массивов в рендер-цикле!).</li>
 *   <li>Острова позади камеры отсекаются за O(1), а поиск струй водопадов ограничен реальным радиусом чанков.</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class OceanSurfaceWaves {
    private static final int MAX_ISLAND_SEGS = 40;
    private static final int RING_COUNT = 3;
    public static final double MAX_ISLAND_DIST = 520.0;
    private static final double MAX_PLUNGE_DIST = 260.0;
    private static final int PLUNGE_CHUNK_RADIUS = 12;

    /** Статические scratch-буферы рендер-потока — 0 аллокаций массивов на кадр! */
    private static final double[] SCRATCH_PX = new double[MAX_ISLAND_SEGS + 1];
    private static final double[] SCRATCH_PZ = new double[MAX_ISLAND_SEGS + 1];
    private static final boolean[] SCRATCH_OK = new boolean[MAX_ISLAND_SEGS + 1];

    /** Предвычисленный геометрический профиль волнового следа одного острова. */
    private static final class WakeProfile {
        final int segs;
        final double reach;
        final double ringGap;
        final double baseWidth;
        final float seed;
        final float[] cosA;
        final float[] sinA;
        final float[] rim;
        final boolean[][] openOcean;

        WakeProfile(CityPlan.Island isl) {
            this.segs = isl.R > 34.0 ? 40 : (isl.R > 14.0 ? 32 : 24);
            this.reach = isl.maxReach();
            this.ringGap = Mth.clamp(isl.R * 0.14, 2.2, 6.2);
            this.baseWidth = Mth.clamp(isl.R * 0.048, 0.90, 2.15);
            this.seed = (float) ((isl.cx * 0.017 + isl.cz * 0.023) % 6.283);
            this.cosA = new float[segs];
            this.sinA = new float[segs];
            this.rim = new float[segs];
            this.openOcean = new boolean[RING_COUNT][segs];

            CityPlan plan = CityPlan.get();
            for (int s = 0; s < segs; ++s) {
                double a = (s / (double) segs) * (Math.PI * 2.0);
                double ca = Math.cos(a);
                double sa = Math.sin(a);
                this.cosA[s] = (float) ca;
                this.sinA[s] = (float) sa;
                double rDist = isl.rimDist(ca, sa);
                this.rim[s] = (float) rDist;
                for (int ring = 0; ring < RING_COUNT; ++ring) {
                    double out = 1.6 + ring * ringGap + ringGap * 0.45;
                    double wx = isl.cx + ca * (rDist + out);
                    double wz = isl.cz + sa * (rDist + out);
                    this.openOcean[ring][s] = isAnalyticalOpenOcean(plan, wx, wz);
                }
            }
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

    private OceanSurfaceWaves() {
    }

    /** Эффективный радиус волн по настройке дальности прорисовки игрока (в чанках) и дальней плоскости отсечения. */
    public static double effectiveWakeRadius(int renderDistChunks, double depthFar) {
        int rd = Math.max(2, renderDistChunks);
        double byChunks = rd <= 4 ? (rd * 24.0 + 28.0) : (rd * 36.0 + 40.0);
        double byFog = Math.max(76.0, depthFar * 0.96);
        return Math.min(MAX_ISLAND_DIST, Math.min(byChunks, byFog));
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

        if (cam.y < ocean - 8.0 || cam.y > ocean + 230.0) return;

        int rdChunks = Math.max(2, mc.options.renderDistance().get());
        double wakeRadius = effectiveWakeRadius(rdChunks, mc.gameRenderer.getDepthFar());

        // Обновляем список островов только при смещении камеры > 16 блоков или изменении дальности прорисовки
        if (Double.isNaN(cachedCamX)
                || Math.abs(wakeRadius - cachedRadius) > 4.0
                || (cam.x - cachedCamX) * (cam.x - cachedCamX) + (cam.z - cachedCamZ) * (cam.z - cachedCamZ) > 16.0 * 16.0) {
            cachedCamX = cam.x;
            cachedCamZ = cam.z;
            cachedRadius = wakeRadius;
            cachedIslands = FloatingIslands.gather(cam.x, cam.z, wakeRadius, false);
        }

        // Кэш точек падения водопадов строго в пределах реальной прорисовки чанков!
        int plungeChunkR = Math.min(PLUNGE_CHUNK_RADIUS, rdChunks);
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
        float heightFade = (float) Mth.clamp(1.0 - Math.max(0.0, cam.y - (ocean + 120.0)) / 110.0, 0.0, 1.0);
        if (heightFade <= 0.01F) return;
        if (ShaderCompat.isShaderPackActive()) {
            heightFade *= 0.82F;
        }

        Vector3f look = camera.getLookVector();
        double lookHzLen = Math.hypot(look.x(), look.z());
        // Если игрок смотрит почти вертикально вниз/вверх — отсечение по азимуту не применяем
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

        double loadedCheckDist = rdChunks * 16.0;
        for (int idx = 0, n = cachedIslands.size(); idx < n; ++idx) {
            CityPlan.Island isl = cachedIslands.get(idx);
            double dx = isl.cx - cam.x;
            double dz = isl.cz - cam.z;
            double dist = Math.hypot(dx, dz);
            double reach = isl.maxReach();
            double maxExtent = reach + 24.0;
            if (dist - maxExtent > wakeRadius) continue;
            if (cullBehind && dist > maxExtent + 12.0 && (dx * lookX + dz * lookZ) < -(maxExtent + 8.0)) {
                continue;
            }
            drawIslandWake(bb, mat, level, isl, cam, ocean, oceanSurfaceY, time, heightFade,
                    dist, wakeRadius, dist - maxExtent <= loadedCheckDist);
        }

        double maxPlunge = Math.min(MAX_PLUNGE_DIST, wakeRadius);
        for (int idx = 0, n = cachedPlunges.size(); idx < n; ++idx) {
            Vec3 p = cachedPlunges.get(idx);
            double dx = p.x - cam.x;
            double dz = p.z - cam.z;
            double dist = Math.hypot(dx, dz);
            if (dist > maxPlunge) continue;
            if (cullBehind && dist > 20.0 && (dx * lookX + dz * lookZ) < -16.0) continue;
            drawWaterfallRipples(bb, mat, p, cam, oceanSurfaceY, time, heightFade, dist, maxPlunge);
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
        double wakeRadius = effectiveWakeRadius(renderDistChunks, renderDistChunks * 16.0 * 4.0);
        long t0 = System.nanoTime();
        List<CityPlan.Island> list = FloatingIslands.gather(camX, camZ, wakeRadius, false);
        int active = 0;
        for (CityPlan.Island isl : list) {
            WakeProfile wp = profileOf(isl);
            active += wp.segs;
        }
        long elapsed = System.nanoTime() - t0;
        return Math.max(1L, elapsed + (active & 0));
    }

    private static void drawIslandWake(BufferBuilder bb, Matrix4f mat, ClientLevel level, CityPlan.Island isl,
                                       Vec3 cam, int ocean, double surfY, float time, float heightFade,
                                       double dist, double wakeRadius, boolean checkLoadedChunks) {
        WakeProfile wp = profileOf(isl);
        double fadeStart = wakeRadius * 0.65;
        double fadeSpan = Math.max(18.0, wakeRadius - fadeStart + wp.reach);
        float distFade = (float) Mth.clamp(1.0 - Math.max(0.0, dist - fadeStart) / fadeSpan, 0.0, 1.0);
        if (distFade <= 0.01F) return;

        boolean highFloating = (isl.base - isl.depth) > ocean + 3.0;
        float kindBoost = highFloating ? 0.88F : 1.0F;
        int segs = wp.segs;

        for (int ring = 0; ring < RING_COUNT; ++ring) {
            float cyc = (time * 0.24F + wp.seed * 0.15F + ring / (float) RING_COUNT) % 1.0F;
            if (cyc < 0.0F) cyc += 1.0F;

            float env = Mth.sin(cyc * (float) Math.PI);
            float alphaF = env * (1.0F - 0.22F * cyc) * distFade * heightFade * kindBoost * 0.72F;
            int alpha = Mth.clamp(Math.round(alphaF * 255.0F), 0, 228);
            if (alpha <= 4) continue;

            double outOffset = 1.6 + (ring + cyc) * wp.ringGap;
            double halfW = (wp.baseWidth * (1.05 - 0.16 * ring)) * 0.5;
            boolean[] ringOpen = wp.openOcean[ring];

            boolean anyOk = false;
            for (int s = 0; s <= segs; ++s) {
                int idx = s % segs;
                if (s == segs) {
                    SCRATCH_PX[s] = SCRATCH_PX[0];
                    SCRATCH_PZ[s] = SCRATCH_PZ[0];
                    SCRATCH_OK[s] = SCRATCH_OK[0];
                    continue;
                }
                double ca = wp.cosA[idx];
                double sa = wp.sinA[idx];
                double a = (idx / (double) segs) * (Math.PI * 2.0);
                double wobble = 0.65 * Math.sin(a * 4.0 + time * 0.85 + wp.seed)
                        + 0.38 * Math.cos(a * 7.0 - time * 0.62 + ring * 1.4);
                double r = wp.rim[idx] + outOffset + wobble;
                double wx = isl.cx + ca * r;
                double wz = isl.cz + sa * r;
                SCRATCH_PX[s] = wx;
                SCRATCH_PZ[s] = wz;
                boolean ok = ringOpen[idx] && (!checkLoadedChunks || isLoadedWaterFast(level, ocean, wx, wz));
                SCRATCH_OK[s] = ok;
                if (ok) anyOk = true;
            }
            if (!anyOk) continue;

            emitRibbonLoop(bb, mat, cam, surfY - 0.012 + ring * 0.002, SCRATCH_PX, SCRATCH_PZ, SCRATCH_OK, segs,
                    halfW * 2.15, 168, 224, 240, Math.max(4, Math.round(alpha * 0.34F)), time * 0.10F + ring * 0.33F);
            emitRibbonLoop(bb, mat, cam, surfY + ring * 0.002, SCRATCH_PX, SCRATCH_PZ, SCRATCH_OK, segs,
                    halfW, 246, 253, 255, alpha, time * 0.14F + ring * 0.33F);
        }
    }

    private static void drawWaterfallRipples(BufferBuilder bb, Matrix4f mat, Vec3 center, Vec3 cam,
                                             double surfY, float time, float heightFade, double dist, double maxPlunge) {
        float distFade = (float) Mth.clamp(1.0 - Math.max(0.0, dist - maxPlunge * 0.65) / Math.max(16.0, maxPlunge * 0.35), 0.0, 1.0);
        if (distFade <= 0.01F) return;

        int segs = 24;
        float seed = (float) ((center.x * 0.13 + center.z * 0.19) % 6.283);

        for (int ring = 0; ring < 4; ++ring) {
            float cyc = (time * 0.58F + seed * 0.2F + ring * 0.25F) % 1.0F;
            if (cyc < 0.0F) cyc += 1.0F;

            float env = Mth.sin(cyc * (float) Math.PI);
            int alpha = Mth.clamp(Math.round(env * (1.0F - 0.28F * cyc) * distFade * heightFade * 238.0F), 0, 242);
            if (alpha <= 4) continue;

            double rBase = 0.75 + (ring + cyc) * 2.55;
            double halfW = (ring == 0 ? 0.62 : 0.44) * (1.05 - 0.12 * ring);

            for (int s = 0; s <= segs; ++s) {
                double a = (s % segs) / (double) segs * Math.PI * 2.0;
                double wob = 0.22 * Math.sin(a * 5.0 + time * 1.6 + seed);
                double rr = rBase + wob;
                SCRATCH_PX[s] = center.x + Math.cos(a) * rr;
                SCRATCH_PZ[s] = center.z + Math.sin(a) * rr;
                SCRATCH_OK[s] = true;
            }

            emitRibbonLoop(bb, mat, cam, surfY + 0.008 + ring * 0.002, SCRATCH_PX, SCRATCH_PZ, SCRATCH_OK, segs,
                    halfW * 1.9, 175, 232, 245, Math.max(4, Math.round(alpha * 0.40F)), time * 0.22F);
            emitRibbonLoop(bb, mat, cam, surfY + 0.016 + ring * 0.002, SCRATCH_PX, SCRATCH_PZ, SCRATCH_OK, segs,
                    halfW, 250, 254, 255, alpha, time * 0.28F);
        }
    }

    private static void emitRibbonLoop(BufferBuilder bb, Matrix4f mat, Vec3 cam, double worldY,
                                       double[] px, double[] pz, boolean[] ok, int segs, double halfW,
                                       int r, int g, int b, int maxAlpha, float uScroll) {
        float y = (float) (worldY - cam.y);
        for (int s = 0; s < segs; ++s) {
            if (!ok[s] || !ok[s + 1]) continue;

            int prev = (s - 1 + segs) % segs;
            int next = (s + 2) % segs;
            float a0 = ok[prev] ? 1.0F : 0.18F;
            float a1 = ok[next] ? 1.0F : 0.18F;
            int al0 = Math.round(maxAlpha * a0);
            int al1 = Math.round(maxAlpha * a1);
            if (al0 <= 3 && al1 <= 3) continue;

            double dx0 = px[s + 1] - px[s];
            double dz0 = pz[s + 1] - pz[s];
            double len0 = Math.hypot(dx0, dz0);
            if (len0 < 1.0E-4 || len0 > 42.0) continue;

            double nx0 = -dz0 / len0;
            double nz0 = dx0 / len0;

            double dx1 = px[(s + 2) % (segs + 1)] - px[s + 1];
            double dz1 = pz[(s + 2) % (segs + 1)] - pz[s + 1];
            if (s + 1 == segs) {
                dx1 = px[1] - px[0];
                dz1 = pz[1] - pz[0];
            }
            double len1 = Math.hypot(dx1, dz1);
            double nx1 = len1 > 1.0E-4 ? -dz1 / len1 : nx0;
            double nz1 = len1 > 1.0E-4 ? dx1 / len1 : nz0;

            float x0In = (float) (px[s] - nx0 * halfW - cam.x);
            float z0In = (float) (pz[s] - nz0 * halfW - cam.z);
            float x0Mid = (float) (px[s] - cam.x);
            float z0Mid = (float) (pz[s] - cam.z);
            float x0Out = (float) (px[s] + nx0 * halfW - cam.x);
            float z0Out = (float) (pz[s] + nz0 * halfW - cam.z);

            float x1In = (float) (px[s + 1] - nx1 * halfW - cam.x);
            float z1In = (float) (pz[s + 1] - nz1 * halfW - cam.z);
            float x1Mid = (float) (px[s + 1] - cam.x);
            float z1Mid = (float) (pz[s + 1] - cam.z);
            float x1Out = (float) (px[s + 1] + nx1 * halfW - cam.x);
            float z1Out = (float) (pz[s + 1] + nz1 * halfW - cam.z);

            float u0 = uScroll + s * 0.25F;
            float u1 = uScroll + (s + 1) * 0.25F;

            bb.vertex(mat, x0In, y, z0In).uv(u0, 0.02F).color(r, g, b, 0).endVertex();
            bb.vertex(mat, x1In, y, z1In).uv(u1, 0.02F).color(r, g, b, 0).endVertex();
            bb.vertex(mat, x1Mid, y, z1Mid).uv(u1, 0.50F).color(r, g, b, al1).endVertex();
            bb.vertex(mat, x0Mid, y, z0Mid).uv(u0, 0.50F).color(r, g, b, al0).endVertex();

            bb.vertex(mat, x0Mid, y, z0Mid).uv(u0, 0.50F).color(r, g, b, al0).endVertex();
            bb.vertex(mat, x1Mid, y, z1Mid).uv(u1, 0.50F).color(r, g, b, al1).endVertex();
            bb.vertex(mat, x1Out, y, z1Out).uv(u1, 0.98F).color(r, g, b, 0).endVertex();
            bb.vertex(mat, x0Out, y, z0Out).uv(u0, 0.98F).color(r, g, b, 0).endVertex();
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
                && !MistKeep.inside(wx, wz);
    }

    /** Мгновенная проверка воды в уже загруженном чанке (0 аллокаций BlockPos и 0 вызовов генератора!). */
    private static boolean isLoadedWaterFast(ClientLevel level, int ocean, double wx, double wz) {
        int bx = Mth.floor(wx);
        int bz = Mth.floor(wz);
        LevelChunk ch = level.getChunkSource().getChunkNow(bx >> 4, bz >> 4);
        if (ch == null) return true;
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
