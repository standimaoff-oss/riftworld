package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.flight.Flight;
import ru.aiforge.riftworld.world.CityPlan;
import ru.aiforge.riftworld.world.FloatingIslands;
import ru.aiforge.riftworld.world.WaterfallGardens;

/**
 * v0.20.0: Стилизованные 2D белые с бирюзовым свечением контурные волны на глади океана
 * под парящими островами (плавно дышащие и смещающиеся по воде, 1:1 по
 * {@code reports/origami_water_refs/02_island_water_ripples_ref.png}) и расходящиеся
 * пенно-белые следы от водопадов, впадающих с островов в океан.
 *
 * <p>Чисто клиентский рендер поверх водной глади ({@code y = ocean + 0.905}),
 * 0 изменений блоков в мире ({@code sub_air = 0} полностью сохранён).
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, value = Dist.CLIENT)
public final class OceanSurfaceWaves {
    private static final double MAX_ISLAND_DIST = 225.0;
    private static final int PLUNGE_CHUNK_RADIUS = 10;

    private record Plunge(double x, double z, double width, double pushX, double pushZ, double seed) {
    }

    private static ClientLevel cachedLevel;
    private static long lastScanTick = -1000L;
    private static final Map<Long, List<Plunge>> CHUNK_PLUNGES = new HashMap<>();
    private static List<CityPlan.Island> cachedIslands = List.of();
    private static double lastIslCamX = Double.NaN, lastIslCamZ = Double.NaN;

    private OceanSurfaceWaves() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || level.dimension() != Flight.ELISAR_DIM) return;

        Vec3 cam = e.getCamera().getPosition();
        int ocean = CityPlan.get().ocean;
        // Под водой глубоко на дне поверхностные 2D-кольца не нужны
        if (cam.y < ocean - 18.0) return;

        float pt = e.getPartialTick();
        double time = (level.getGameTime() % 240000L) + pt;

        if (level != cachedLevel) {
            cachedLevel = level;
            CHUNK_PLUNGES.clear();
            lastScanTick = -1000L;
            lastIslCamX = Double.NaN;
        }

        if (Double.isNaN(lastIslCamX) || Math.hypot(cam.x - lastIslCamX, cam.z - lastIslCamZ) > 24.0) {
            lastIslCamX = cam.x;
            lastIslCamZ = cam.z;
            cachedIslands = FloatingIslands.gather(cam.x, cam.z, MAX_ISLAND_DIST, false);
        }

        long gameTime = level.getGameTime();
        if (gameTime - lastScanTick >= 40L) {
            lastScanTick = gameTime;
            refreshNearbyPlunges(level, cam, ocean);
        }

        PoseStack ps = e.getPoseStack();
        Matrix4f mat = ps.last().pose();
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // 1. Контурные 2D белые волны под парящими островами (как на 02_island_water_ripples_ref.png)
        double ySurf = (ocean + 0.904) - cam.y;
        for (CityPlan.Island isl : cachedIslands) {
            if (isl.kind == WaterfallGardens.KIND) continue; // сады стоят в воде; их водопады рисуются в п.2
            double distCam = Math.hypot(isl.cx - cam.x, isl.cz - cam.z) - isl.maxReach();
            if (distCam > MAX_ISLAND_DIST) continue;
            float distFade = (float) Mth.clamp((MAX_ISLAND_DIST - distCam) / 55.0, 0.0, 1.0);
            if (distFade <= 0.01F) continue;

            drawIslandWaveContours(bb, mat, level, mp, isl, ocean, cam, ySurf, time, distFade);
        }

        // 2. Красивые 2D белые следы и расходящиеся кольца на воде от водопадов с островов в океан
        double yFallSurf = (ocean + 0.914) - cam.y;
        int ccx = SectionPosFloor(cam.x), ccz = SectionPosFloor(cam.z);
        for (int dx = -PLUNGE_CHUNK_RADIUS; dx <= PLUNGE_CHUNK_RADIUS; ++dx) {
            for (int dz = -PLUNGE_CHUNK_RADIUS; dz <= PLUNGE_CHUNK_RADIUS; ++dz) {
                List<Plunge> list = CHUNK_PLUNGES.get(ChunkPos.asLong(ccx + dx, ccz + dz));
                if (list == null || list.isEmpty()) continue;
                for (Plunge pl : list) {
                    double d = Math.hypot(pl.x - cam.x, pl.z - cam.z);
                    if (d > 168.0) continue;
                    float fade = (float) Mth.clamp((168.0 - d) / 42.0, 0.0, 1.0);
                    drawWaterfallPlungeWaves(bb, mat, level, mp, pl, ocean, cam, yFallSurf, time, fade);
                }
            }
        }

        Tesselator.getInstance().end();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static int SectionPosFloor(double coord) {
        return Mth.floor(coord) >> 4;
    }

    /**
     * Рисует 2–3 плавно дышащих и чуть перемещающихся контурных 2D-кольца на воде строго под островом:
     * мягкий бирюзово-аквамариновый ореол + яркий белоснежный внутренний гребень волны с плавными разрывами.
     */
    private static void drawIslandWaveContours(BufferBuilder bb, Matrix4f mat, ClientLevel level,
                                               BlockPos.MutableBlockPos mp, CityPlan.Island isl, int ocean,
                                               Vec3 cam, double yRel, double time, float distFade) {
        double seed = isl.p1 * 3.1 + isl.p2 * 1.7;
        // Плавное покачивание центра водной тени на течении океана
        double driftX = Math.sin(time * 0.019 + seed) * 1.15;
        double driftZ = Math.cos(time * 0.016 + seed * 1.3) * 1.15;

        int segs = isl.R < 22.0 ? 24 : (isl.R < 45.0 ? 34 : 44);
        int ringCount = isl.R < 20.0 ? 2 : 3;

        double[] px = new double[segs + 1];
        double[] pz = new double[segs + 1];
        double[] nx = new double[segs + 1];
        double[] nz = new double[segs + 1];
        float[] vis = new float[segs + 1];

        for (int ring = 0; ring < ringCount; ++ring) {
            // Медленное дыхание и расхождение волн наружу от проекции конуса острова
            double cycle = ((time * 0.0055 + ring * (1.0 / ringCount) + seed * 0.11) % 1.0 + 1.0) % 1.0;
            double scale = ring == 0
                    ? (0.60 + 0.07 * Math.sin(time * 0.024 + seed)) // яркий главный контур прямо под островом (как на фото!)
                    : (0.74 + cycle * 0.42);                        // вторичные расходящиеся круги-следы

            float ringEnv = ring == 0 ? 1.0F : (float) (Math.sin(cycle * Math.PI) * 0.78);
            if (ringEnv * distFade < 0.03F) continue;

            for (int i = 0; i <= segs; ++i) {
                double a = (Math.PI * 2.0 * i) / segs;
                double ca = Math.cos(a), sa = Math.sin(a);
                double rim = isl.rimDist(ca, sa);

                // Лёгкая угловато-волнистая деформация контура, как у 2D-волн на референсе 02_island_water_ripples_ref.png
                double wob = 0.90 * Math.sin(3.0 * a + time * 0.031 + seed)
                        + 0.55 * Math.cos(5.0 * a - time * 0.026 + ring * 1.9);
                double r = Math.max(3.2, rim * scale + wob);

                double wx = isl.cx + driftX + ca * r;
                double wz = isl.cz + driftZ + sa * r;
                px[i] = wx - cam.x;
                pz[i] = wz - cam.z;
                nx[i] = ca;
                nz[i] = sa;

                // Естественные каллиграфические просветы по дуге + обрезка у твёрдых скал
                double gapWave = Math.sin(2.0 * a - time * 0.021 + seed + ring * 2.3)
                        + 0.45 * Math.cos(3.0 * a + time * 0.017 - seed);
                float arcAlpha = ring == 0
                        ? Mth.clamp((float) ((gapWave + 1.08) * 0.85), 0.0F, 1.0F)
                        : Mth.clamp((float) ((gapWave + 0.55) * 0.78), 0.0F, 1.0F);

                if (arcAlpha > 0.01F && !isOpenOcean(level, mp, ocean, wx, wz)) {
                    arcAlpha = 0.0F;
                }
                vis[i] = arcAlpha * ringEnv * distFade;
            }
            // Замыкаем контур без шва
            px[segs] = px[0]; pz[segs] = pz[0]; nx[segs] = nx[0]; nz[segs] = nz[0]; vis[segs] = vis[0];

            double glowHalfW = (ring == 0 ? 1.85 : 1.35) * (isl.R < 20.0 ? 0.72 : 1.0);
            double coreHalfW = (ring == 0 ? 0.58 : 0.38) * (isl.R < 20.0 ? 0.78 : 1.0);

            // Слой 1: мягкое бирюзово-аквамариновое свечение на воде
            emitRibbonLoop(bb, mat, px, pz, nx, nz, vis, segs, yRel, glowHalfW,
                    72, 246, 228, 108);
            // Слой 2: яркий белоснежный 2D-гребень волны внутри ореола
            emitRibbonLoop(bb, mat, px, pz, nx, nz, vis, segs, yRel + 0.004, coreHalfW,
                    250, 255, 255, 228);
        }
    }

    /**
     * Рисует пенную розетку и 4 расходящихся 2D белых следа-волны в месте падения водопада с острова в океан.
     */
    private static void drawWaterfallPlungeWaves(BufferBuilder bb, Matrix4f mat, ClientLevel level,
                                                 BlockPos.MutableBlockPos mp, Plunge pl, int ocean,
                                                 Vec3 cam, double yRel, double time, float fade) {
        int segs = 28;
        double[] px = new double[segs + 1];
        double[] pz = new double[segs + 1];
        double[] nx = new double[segs + 1];
        double[] nz = new double[segs + 1];
        float[] vis = new float[segs + 1];

        // 1) Бурлящее белое кольцо пены прямо вокруг струи водопада
        double baseR = Math.max(1.6, pl.width * 0.75 + 0.9);
        for (int i = 0; i <= segs; ++i) {
            double a = (Math.PI * 2.0 * i) / segs;
            double ca = Math.cos(a), sa = Math.sin(a);
            double foamR = baseR + 0.38 * Math.sin(6.0 * a + time * 0.16 + pl.seed)
                    + 0.22 * Math.cos(4.0 * a - time * 0.13);
            double wx = pl.x + ca * foamR;
            double wz = pl.z + sa * foamR;
            px[i] = wx - cam.x;
            pz[i] = wz - cam.z;
            nx[i] = ca;
            nz[i] = sa;
            vis[i] = isOpenOcean(level, mp, ocean, wx, wz) ? fade : 0.0F;
        }
        px[segs] = px[0]; pz[segs] = pz[0]; nx[segs] = nx[0]; nz[segs] = nz[0]; vis[segs] = vis[0];
        emitRibbonLoop(bb, mat, px, pz, nx, nz, vis, segs, yRel, 1.35, 85, 248, 235, 130);
        emitRibbonLoop(bb, mat, px, pz, nx, nz, vis, segs, yRel + 0.004, 0.52, 255, 255, 255, 242);

        // 2) Четыре расходящихся по океану 2D белых следа от водопада (вытягиваются по направлению от скалы в море!)
        for (int k = 0; k < 4; ++k) {
            double u = ((time * 0.016 + k * 0.25 + pl.seed * 0.17) % 1.0 + 1.0) % 1.0;
            float env = (float) (Math.sin(u * Math.PI) * (1.0 - 0.25 * u));
            if (env * fade < 0.04F) continue;

            double r = baseR + 1.2 + u * (11.5 + pl.width * 1.4);
            double drift = u * 4.2;
            double cx = pl.x + pl.pushX * drift;
            double cz = pl.z + pl.pushZ * drift;

            for (int i = 0; i <= segs; ++i) {
                double a = (Math.PI * 2.0 * i) / segs;
                double ca = Math.cos(a), sa = Math.sin(a);
                double wob = 0.45 * Math.sin(4.0 * a + time * 0.07 + k * 1.5);
                double wx = cx + ca * (r + wob);
                double wz = cz + sa * (r + wob);
                px[i] = wx - cam.x;
                pz[i] = wz - cam.z;
                nx[i] = ca;
                nz[i] = sa;
                double arc = 0.55 + 0.45 * Math.sin(2.0 * a - time * 0.04 + k * 2.1 + pl.seed);
                vis[i] = isOpenOcean(level, mp, ocean, wx, wz) ? (float) arc * env * fade : 0.0F;
            }
            px[segs] = px[0]; pz[segs] = pz[0]; nx[segs] = nx[0]; nz[segs] = nz[0]; vis[segs] = vis[0];

            double gw = 1.15 * (1.0 - 0.35 * u);
            double cw = 0.36 * (1.0 - 0.35 * u);
            emitRibbonLoop(bb, mat, px, pz, nx, nz, vis, segs, yRel, gw, 78, 245, 232, 96);
            emitRibbonLoop(bb, mat, px, pz, nx, nz, vis, segs, yRel + 0.004, cw, 252, 255, 255, 220);
        }
    }

    /** Двухполосная мягкая 2D-лента (внутренний край alpha=0 -> яркий хребет -> внешний край alpha=0). */
    private static void emitRibbonLoop(BufferBuilder bb, Matrix4f mat,
                                       double[] px, double[] pz, double[] nx, double[] nz, float[] vis,
                                       int segs, double y, double halfW,
                                       int r, int g, int b, int peakAlpha) {
        float yf = (float) y;
        for (int i = 0; i < segs; ++i) {
            float v0 = vis[i], v1 = vis[i + 1];
            if (v0 <= 0.01F && v1 <= 0.01F) continue;
            int a0 = Mth.clamp(Math.round(peakAlpha * v0), 0, 255);
            int a1 = Mth.clamp(Math.round(peakAlpha * v1), 0, 255);

            double hw0 = halfW * (0.45 + 0.55 * v0);
            double hw1 = halfW * (0.45 + 0.55 * v1);

            float in0x = (float) (px[i] - nx[i] * hw0), in0z = (float) (pz[i] - nz[i] * hw0);
            float md0x = (float) px[i],                 md0z = (float) pz[i];
            float ot0x = (float) (px[i] + nx[i] * hw0), ot0z = (float) (pz[i] + nz[i] * hw0);

            float in1x = (float) (px[i + 1] - nx[i + 1] * hw1), in1z = (float) (pz[i + 1] - nz[i + 1] * hw1);
            float md1x = (float) px[i + 1],                     md1z = (float) pz[i + 1];
            float ot1x = (float) (px[i + 1] + nx[i + 1] * hw1), ot1z = (float) (pz[i + 1] + nz[i + 1] * hw1);

            // Внутренняя половина ленты (растушёвка к центру)
            bb.vertex(mat, in0x, yf, in0z).color(r, g, b, 0).endVertex();
            bb.vertex(mat, md0x, yf, md0z).color(r, g, b, a0).endVertex();
            bb.vertex(mat, md1x, yf, md1z).color(r, g, b, a1).endVertex();
            bb.vertex(mat, in1x, yf, in1z).color(r, g, b, 0).endVertex();

            // Внешняя половина ленты (растушёвка наружу)
            bb.vertex(mat, md0x, yf, md0z).color(r, g, b, a0).endVertex();
            bb.vertex(mat, ot0x, yf, ot0z).color(r, g, b, 0).endVertex();
            bb.vertex(mat, ot1x, yf, ot1z).color(r, g, b, 0).endVertex();
            bb.vertex(mat, md1x, yf, md1z).color(r, g, b, a1).endVertex();
        }
    }

    private static boolean isOpenOcean(ClientLevel level, BlockPos.MutableBlockPos mp, int ocean, double wx, double wz) {
        int bx = Mth.floor(wx), bz = Mth.floor(wz);
        LevelChunk ch = level.getChunkSource().getChunkNow(bx >> 4, bz >> 4);
        if (ch == null) {
            return WaterfallGardens.landAt(wx, wz) == null;
        }
        mp.set(bx, ocean, bz);
        if (ch.getFluidState(mp).isEmpty()) return false;
        mp.set(bx, ocean + 1, bz);
        BlockState above = ch.getBlockState(mp);
        return above.isAir() || above.getCollisionShape(level, mp).isEmpty();
    }

    /** Находит все точки падения водопадов с островов в океан в загруженных чанках вокруг камеры. */
    private static void refreshNearbyPlunges(ClientLevel level, Vec3 cam, int ocean) {
        int ccx = SectionPosFloor(cam.x), ccz = SectionPosFloor(cam.z);
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();

        for (int dx = -PLUNGE_CHUNK_RADIUS; dx <= PLUNGE_CHUNK_RADIUS; ++dx) {
            for (int dz = -PLUNGE_CHUNK_RADIUS; dz <= PLUNGE_CHUNK_RADIUS; ++dz) {
                int cx = ccx + dx, cz = ccz + dz;
                long key = ChunkPos.asLong(cx, cz);
                if (CHUNK_PLUNGES.containsKey(key)) continue;

                LevelChunk ch = level.getChunkSource().getChunkNow(cx, cz);
                if (ch == null) continue;

                List<int[]> cells = null;
                int x0 = cx << 4, z0 = cz << 4;
                for (int lx = 0; lx < 16; ++lx) {
                    for (int lz = 0; lz < 16; ++lz) {
                        int wx = x0 + lx, wz = z0 + lz;
                        mp.set(wx, ocean + 2, wz);
                        if (ch.getFluidState(mp).isEmpty()) continue;
                        mp.set(wx, ocean, wz);
                        if (ch.getFluidState(mp).isEmpty()) continue;
                        if (cells == null) cells = new ArrayList<>(8);
                        cells.add(new int[]{wx, wz});
                    }
                }

                if (cells == null || cells.isEmpty()) {
                    CHUNK_PLUNGES.put(key, List.of());
                    continue;
                }

                // Группируем соседние струи водопада в единые центры падения с направлением от скалы в открытое море
                List<Plunge> plunges = new ArrayList<>(3);
                boolean[] used = new boolean[cells.size()];
                for (int i = 0; i < cells.size(); ++i) {
                    if (used[i]) continue;
                    used[i] = true;
                    double sx = cells.get(i)[0] + 0.5, sz = cells.get(i)[1] + 0.5;
                    int cnt = 1;
                    for (int j = i + 1; j < cells.size(); ++j) {
                        if (used[j]) continue;
                        double d = Math.hypot(cells.get(j)[0] + 0.5 - sx / cnt, cells.get(j)[1] + 0.5 - sz / cnt);
                        if (d <= 5.5) {
                            used[j] = true;
                            sx += cells.get(j)[0] + 0.5;
                            sz += cells.get(j)[1] + 0.5;
                            ++cnt;
                        }
                    }
                    double px = sx / cnt, pz = sz / cnt;
                    // Вычисляем вектор в сторону открытой воды (прочь от скальной стены позади водопада)
                    double pushX = 0.0, pushZ = 0.0;
                    for (int dir = 0; dir < 8; ++dir) {
                        double a = (Math.PI * 2.0 * dir) / 8.0;
                        double tx = px + Math.cos(a) * 4.0, tz = pz + Math.sin(a) * 4.0;
                        mp.set(Mth.floor(tx), ocean + 1, Mth.floor(tz));
                        if (level.isLoaded(mp) && level.getBlockState(mp).isAir()
                                && !level.getFluidState(mp.set(Mth.floor(tx), ocean, Mth.floor(tz))).isEmpty()) {
                            pushX += Math.cos(a);
                            pushZ += Math.sin(a);
                        }
                    }
                    double plen = Math.hypot(pushX, pushZ);
                    if (plen > 1.0e-3) {
                        pushX /= plen;
                        pushZ /= plen;
                    }
                    double width = Math.min(4.8, 1.2 + Math.sqrt(cnt) * 0.75);
                    double seed = ((Mth.floor(px) * 73428767L ^ Mth.floor(pz) * 912931L) & 1023) * 0.017;
                    plunges.add(new Plunge(px, pz, width, pushX, pushZ, seed));
                }
                CHUNK_PLUNGES.put(key, plunges);
            }
        }
    }
}
