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
import ru.aiforge.riftworld.world.AshenLands;
import ru.aiforge.riftworld.world.CityPlan;
import ru.aiforge.riftworld.world.FloatingIslands;
import ru.aiforge.riftworld.world.MistKeep;
import ru.aiforge.riftworld.world.WaterfallGardens;

/**
 * v0.26.0: Непрерывные, далеко видимые (до ~520 блоков!) 2D белые с бирюзовым свечением контурные следы
 * на глади океана под АБСОЛЮТНО ВСЕМИ островами Элисара (включая малые острова, островки-чаши, архипелаги
 * биомов, деревни и маяки/бухты) + расходящиеся пенно-белые следы от водопадов.
 *
 * <p>Чисто клиентский рендер поверх водной глади ({@code y = ocean + 0.905}),
 * 0 изменений блоков в мире ({@code sub_air = 0} полностью сохранён).
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, value = Dist.CLIENT)
public final class OceanSurfaceWaves {
    /** Дальность видимости следов под островами — на всю панораму горизонта (~520 м)! */
    public static final double MAX_ISLAND_DIST = 520.0;
    private static final double FULL_ALPHA_DIST = 360.0;
    private static final int PLUNGE_CHUNK_RADIUS = 12;

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
        if (ShaderCompat.isRenderingShadowPass()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || level.dimension() != Flight.ELISAR_DIM) return;

        Vec3 cam = e.getCamera().getPosition();
        int ocean = CityPlan.get().ocean;
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
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, ShaderCompat.WHITE_TEX);

        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        // 1. Непрерывные, далеко видимые 2D контурные волны под ВСЕМИ островами
        double ySurf = (ocean + 0.906) - cam.y;
        for (CityPlan.Island isl : cachedIslands) {
            double distCam = Math.max(0.0, Math.hypot(isl.cx - cam.x, isl.cz - cam.z) - isl.maxReach());
            if (distCam > MAX_ISLAND_DIST) continue;
            float distFade;
            if (distCam <= FULL_ALPHA_DIST) {
                distFade = 1.0F;
            } else {
                float u = (float) ((distCam - FULL_ALPHA_DIST) / (MAX_ISLAND_DIST - FULL_ALPHA_DIST));
                distFade = 0.5F * (1.0F + Mth.cos(u * Mth.PI));
            }
            if (distFade <= 0.01F) continue;

            drawIslandWaveContours(bb, mat, level, mp, isl, ocean, cam, ySurf, time, distFade);
        }

        // 2. Расходящиеся 2D белые следы на воде от водопадов с островов в океан (также дальше видимые!)
        double yFallSurf = (ocean + 0.916) - cam.y;
        int ccx = SectionPosFloor(cam.x), ccz = SectionPosFloor(cam.z);
        for (int dx = -PLUNGE_CHUNK_RADIUS; dx <= PLUNGE_CHUNK_RADIUS; ++dx) {
            for (int dz = -PLUNGE_CHUNK_RADIUS; dz <= PLUNGE_CHUNK_RADIUS; ++dz) {
                List<Plunge> list = CHUNK_PLUNGES.get(ChunkPos.asLong(ccx + dx, ccz + dz));
                if (list == null || list.isEmpty()) continue;
                for (Plunge pl : list) {
                    double d = Math.hypot(pl.x - cam.x, pl.z - cam.z);
                    if (d > 260.0) continue;
                    float fade = d <= 180.0 ? 1.0F : (float) Mth.clamp((260.0 - d) / 80.0, 0.0, 1.0);
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
     * Рисует непрерывные (без дыр и прерывистости!) плавно дышащие контурные 2D-кольца на воде под островом:
     * мягкий бирюзово-аквамариновый ореол + яркий белоснежный внутренний гребень волны, чётко читаемый даже вдали!
     */
    private static void drawIslandWaveContours(BufferBuilder bb, Matrix4f mat, ClientLevel level,
                                               BlockPos.MutableBlockPos mp, CityPlan.Island isl, int ocean,
                                               Vec3 cam, double yRel, double time, float distFade) {
        double seed = isl.p1 * 3.1 + isl.p2 * 1.7;
        double driftX = Math.sin(time * 0.019 + seed) * 1.10;
        double driftZ = Math.cos(time * 0.016 + seed * 1.3) * 1.10;

        boolean isGardenInSea = (isl.kind == WaterfallGardens.KIND);
        int segs = isl.R < 14.0 ? 32 : (isl.R < 38.0 ? 48 : 64);
        int ringCount = isl.R < 12.0 ? 2 : 3;

        double[] px = new double[segs + 1];
        double[] pz = new double[segs + 1];
        double[] nx = new double[segs + 1];
        double[] nz = new double[segs + 1];
        float[] rawVis = new float[segs + 1];
        float[] vis = new float[segs + 1];

        for (int ring = 0; ring < ringCount; ++ring) {
            double cycle = ((time * 0.0052 + ring * (1.0 / ringCount) + seed * 0.11) % 1.0 + 1.0) % 1.0;
            double scale;
            if (isGardenInSea) {
                // Острова Висячих садов опираются на дно — их контурные волны обнимают внешнюю береговую кромку в море!
                scale = ring == 0
                        ? (1.03 + 0.04 * Math.sin(time * 0.024 + seed))
                        : (1.10 + cycle * 0.34);
            } else {
                scale = ring == 0
                        ? (0.68 + 0.06 * Math.sin(time * 0.024 + seed)) // непрерывный главный контур прямо под островом
                        : (0.82 + cycle * 0.42);                        // плавно расходящиеся внешние круги
            }

            float ringEnv = ring == 0 ? 1.0F : (float) (0.30 + 0.62 * Math.sin(cycle * Math.PI));
            if (ringEnv * distFade < 0.03F) continue;

            for (int i = 0; i < segs; ++i) {
                double a = (Math.PI * 2.0 * i) / segs;
                double ca = Math.cos(a), sa = Math.sin(a);
                double rim = isl.rimDist(ca, sa);

                double wobAmp = Math.min(1.0, Math.max(0.25, isl.R * 0.04));
                double wob = wobAmp * (0.85 * Math.sin(3.0 * a + time * 0.028 + seed)
                        + 0.45 * Math.cos(5.0 * a - time * 0.023 + ring * 1.9));
                double r = Math.max(2.4, rim * scale + wob);

                double wx = isl.cx + driftX + ca * r;
                double wz = isl.cz + driftZ + sa * r;
                px[i] = wx - cam.x;
                pz[i] = wz - cam.z;
                nx[i] = ca;
                nz[i] = sa;

                // Непрерывное дыхание яркости без обнуления дуги (0 прерывистых разрывов!)
                float shimmer = 0.90F + 0.10F * (float) Math.sin(2.0 * a - time * 0.022 + seed + ring * 1.7);
                rawVis[i] = isOpenOcean(level, mp, ocean, wx, wz) ? (shimmer * ringEnv * distFade) : 0.0F;
            }

            // Мягкое 3-точечное сглаживание на стыке с твёрдой скалой без ступенчатых обрывов
            for (int i = 0; i < segs; ++i) {
                if (rawVis[i] <= 0.001F) {
                    vis[i] = 0.0F;
                } else {
                    int prev = (i + segs - 1) % segs;
                    int next = (i + 1) % segs;
                    vis[i] = 0.55F * rawVis[i] + 0.225F * (rawVis[prev] + rawVis[next]);
                }
            }
            px[segs] = px[0]; pz[segs] = pz[0]; nx[segs] = nx[0]; nz[segs] = nz[0]; vis[segs] = vis[0];

            double sizeScale = isl.R < 10.0 ? 0.68 : (isl.R < 20.0 ? 0.84 : 1.0);
            double glowHalfW = (ring == 0 ? 2.15 : 1.55) * sizeScale;
            double coreHalfW = (ring == 0 ? 0.68 : 0.46) * sizeScale;

            // Слой 1: мягкое бирюзово-аквамариновое свечение на воде
            emitRibbonLoop(bb, mat, px, pz, nx, nz, vis, segs, yRel, glowHalfW,
                    72, 246, 228, 122);
            // Слой 2: яркий непрерывный белоснежный 2D-гребень волны внутри ореола
            emitRibbonLoop(bb, mat, px, pz, nx, nz, vis, segs, yRel + 0.004, coreHalfW,
                    252, 255, 255, 238);
        }
    }

    /**
     * Рисует пенную розетку и 4 непрерывных расходящихся 2D белых следа в месте падения водопада в океан.
     */
    private static void drawWaterfallPlungeWaves(BufferBuilder bb, Matrix4f mat, ClientLevel level,
                                                 BlockPos.MutableBlockPos mp, Plunge pl, int ocean,
                                                 Vec3 cam, double yRel, double time, float fade) {
        int segs = 32;
        double[] px = new double[segs + 1];
        double[] pz = new double[segs + 1];
        double[] nx = new double[segs + 1];
        double[] nz = new double[segs + 1];
        float[] vis = new float[segs + 1];

        double baseR = Math.max(1.6, pl.width * 0.75 + 0.9);
        for (int i = 0; i < segs; ++i) {
            double a = (Math.PI * 2.0 * i) / segs;
            double ca = Math.cos(a), sa = Math.sin(a);
            double foamR = baseR + 0.36 * Math.sin(6.0 * a + time * 0.16 + pl.seed)
                    + 0.20 * Math.cos(4.0 * a - time * 0.13);
            double wx = pl.x + ca * foamR;
            double wz = pl.z + sa * foamR;
            px[i] = wx - cam.x;
            pz[i] = wz - cam.z;
            nx[i] = ca;
            nz[i] = sa;
            vis[i] = isOpenOcean(level, mp, ocean, wx, wz) ? fade : 0.0F;
        }
        px[segs] = px[0]; pz[segs] = pz[0]; nx[segs] = nx[0]; nz[segs] = nz[0]; vis[segs] = vis[0];
        emitRibbonLoop(bb, mat, px, pz, nx, nz, vis, segs, yRel, 1.45, 85, 248, 235, 138);
        emitRibbonLoop(bb, mat, px, pz, nx, nz, vis, segs, yRel + 0.004, 0.56, 255, 255, 255, 244);

        for (int k = 0; k < 4; ++k) {
            double u = ((time * 0.015 + k * 0.25 + pl.seed * 0.17) % 1.0 + 1.0) % 1.0;
            float env = (float) (Math.sin(u * Math.PI) * (1.0 - 0.22 * u));
            if (env * fade < 0.04F) continue;

            double r = baseR + 1.2 + u * (12.5 + pl.width * 1.5);
            double drift = u * 4.5;
            double cx = pl.x + pl.pushX * drift;
            double cz = pl.z + pl.pushZ * drift;

            for (int i = 0; i < segs; ++i) {
                double a = (Math.PI * 2.0 * i) / segs;
                double ca = Math.cos(a), sa = Math.sin(a);
                double wob = 0.42 * Math.sin(4.0 * a + time * 0.07 + k * 1.5);
                double wx = cx + ca * (r + wob);
                double wz = cz + sa * (r + wob);
                px[i] = wx - cam.x;
                pz[i] = wz - cam.z;
                nx[i] = ca;
                nz[i] = sa;
                float arc = (float) (0.78 + 0.22 * Math.sin(2.0 * a - time * 0.04 + k * 2.1 + pl.seed));
                vis[i] = isOpenOcean(level, mp, ocean, wx, wz) ? arc * env * fade : 0.0F;
            }
            px[segs] = px[0]; pz[segs] = pz[0]; nx[segs] = nx[0]; nz[segs] = nz[0]; vis[segs] = vis[0];

            double gw = 1.25 * (1.0 - 0.30 * u);
            double cw = 0.42 * (1.0 - 0.30 * u);
            emitRibbonLoop(bb, mat, px, pz, nx, nz, vis, segs, yRel, gw, 78, 245, 232, 108);
            emitRibbonLoop(bb, mat, px, pz, nx, nz, vis, segs, yRel + 0.004, cw, 252, 255, 255, 228);
        }
    }

    /**
     * Двухполосная мягкая 2D-лента с перспективной компенсацией толщины вдали:
     * на дистанциях 150–500 м линия плавно расширяется пропорционально расстоянию,
     * чтобы никогда не распадаться на субпиксельные точки!
     */
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

            double d0 = Math.hypot(px[i], pz[i]);
            double d1 = Math.hypot(px[i + 1], pz[i + 1]);
            double distScale0 = 1.0 + d0 * 0.0085;
            double distScale1 = 1.0 + d1 * 0.0085;

            double hw0 = halfW * (0.65 + 0.35 * v0) * distScale0;
            double hw1 = halfW * (0.65 + 0.35 * v1) * distScale1;

            float in0x = (float) (px[i] - nx[i] * hw0), in0z = (float) (pz[i] - nz[i] * hw0);
            float md0x = (float) px[i],                 md0z = (float) pz[i];
            float ot0x = (float) (px[i] + nx[i] * hw0), ot0z = (float) (pz[i] + nz[i] * hw0);

            float in1x = (float) (px[i + 1] - nx[i + 1] * hw1), in1z = (float) (pz[i + 1] - nz[i + 1] * hw1);
            float md1x = (float) px[i + 1],                     md1z = (float) pz[i + 1];
            float ot1x = (float) (px[i + 1] + nx[i + 1] * hw1), ot1z = (float) (pz[i + 1] + nz[i + 1] * hw1);

            bb.vertex(mat, in0x, yf, in0z).uv(0.5F, 0.5F).color(r, g, b, 0).endVertex();
            bb.vertex(mat, md0x, yf, md0z).uv(0.5F, 0.5F).color(r, g, b, a0).endVertex();
            bb.vertex(mat, md1x, yf, md1z).uv(0.5F, 0.5F).color(r, g, b, a1).endVertex();
            bb.vertex(mat, in1x, yf, in1z).uv(0.5F, 0.5F).color(r, g, b, 0).endVertex();

            bb.vertex(mat, md0x, yf, md0z).uv(0.5F, 0.5F).color(r, g, b, a0).endVertex();
            bb.vertex(mat, ot0x, yf, ot0z).uv(0.5F, 0.5F).color(r, g, b, 0).endVertex();
            bb.vertex(mat, ot1x, yf, ot1z).uv(0.5F, 0.5F).color(r, g, b, 0).endVertex();
            bb.vertex(mat, md1x, yf, md1z).uv(0.5F, 0.5F).color(r, g, b, a1).endVertex();
        }
    }

    private static boolean isOpenOcean(ClientLevel level, BlockPos.MutableBlockPos mp, int ocean, double wx, double wz) {
        int bx = Mth.floor(wx), bz = Mth.floor(wz);
        LevelChunk ch = level.getChunkSource().getChunkNow(bx >> 4, bz >> 4);
        if (ch == null) {
            return WaterfallGardens.landAt(wx, wz) == null
                    && !AshenLands.inside(wx, wz)
                    && !MistKeep.inside(wx, wz);
        }
        mp.set(bx, ocean, bz);
        if (ch.getFluidState(mp).isEmpty()) return false;
        mp.set(bx, ocean + 1, bz);
        BlockState above = ch.getBlockState(mp);
        return above.isAir() || above.getCollisionShape(level, mp).isEmpty();
    }

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
