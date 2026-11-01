package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.block.DemonGateBE;
import ru.aiforge.riftworld.registry.TempleBlocks;

/**
 * v0.21.0: Огромный уходящий в самое небо 3D-Прорыв Пространства ({@code SpatialRift}, высота 168 блоков)
 * вместо обычного квадратного портала из блоков:
 * <ul>
 *   <li>У земли — изящное узкое лезвие пространственного разрыва (~0.2–0.85 м толщиной), к которому можно
 *       подойти, поднести кристалл или коснуться рукой;</li>
 *   <li>Чем выше он поднимается к небу, тем мощнее, толще и шире раскрывается брешь реальности
 *       (до ~42 блоков в поперечнике и ~19 блоков 3D-объёма в вышине небес с ветвящимися трещинами эфира!);</li>
 *   <li>Рендерится честной 3D-геометрией через {@link RenderType#entityTranslucent} (100% совместимо с
 *       IterationT и любыми шейдерпаками Oculus / Iris / OptiFine, с естественным свечением без поломок G-буфера).</li>
 * </ul>
 */
public class DemonGateRenderer implements BlockEntityRenderer<DemonGateBE> {
    private static final ResourceLocation RIFT_TEX =
            new ResourceLocation(RiftWorld.MODID, "textures/environment/spatial_rift.png");

    private static final float TOTAL_HEIGHT = 168.0F;
    private static final float[] Y_STOPS = {
            0.0F, 0.35F, 0.80F, 1.35F, 2.00F, 2.80F, 3.80F, 5.00F,
            6.50F, 8.30F, 10.5F, 13.2F, 16.5F, 20.5F, 25.0F, 30.0F,
            36.0F, 42.5F, 49.5F, 57.0F, 65.0F, 73.5F, 82.5F, 92.0F,
            102.0F, 112.0F, 122.0F, 132.0F, 141.0F, 149.0F, 156.0F, 162.0F, 168.0F
    };

    public DemonGateRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public int getViewDistance() {
        return 512;
    }

    @Override
    public boolean shouldRenderOffScreen(DemonGateBE be) {
        return true;
    }

    @Override
    public void render(DemonGateBE be, float partial, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (ShaderCompat.isRenderingShadowPass()) return;
        Level level = be.getLevel();
        if (level == null) return;

        BlockPos pos = be.getBlockPos();
        // Если рядом (в креативе или старом чанке) стоит несколько якорей подряд, рендерим 1 раз от нижнего
        if (isGate(level, pos.below()) || isGate(level, pos.west()) || isGate(level, pos.north())) {
            return;
        }

        float time = (level.getGameTime() % 240000L) + partial;
        float seed = ((pos.getX() * 73428767L ^ pos.getZ() * 912931L) & 1023) * 0.013F;
        boolean ritualActive = PortalRitualClient.isNearActivePortal(pos);
        float surge = ritualActive
                ? 1.0F + 0.38F * Mth.clamp(PortalRitualClient.currentTime(partial) / 70.0F, 0.0F, 1.0F)
                : 1.0F;

        ps.pushPose();
        ps.translate(0.5D, 0.02D, 0.5D);
        Matrix4f mat = ps.last().pose();
        Matrix3f nrm = ps.last().normal();

        int n = Y_STOPS.length;
        float[] sx = new float[n];
        float[] sz = new float[n];
        float[] hw = new float[n];
        float[] hd = new float[n];

        for (int i = 0; i < n; ++i) {
            float y = Y_STOPS[i];
            float u = y / TOTAL_HEIGHT;
            // У самой земли ось строго закреплена в (0, 0), выше плавно извивается живой молнией пространства
            float swayMask = Mth.clamp((y - 0.8F) / 14.0F, 0.0F, 1.0F);
            float breath = 1.0F + 0.055F * Mth.sin(time * 0.055F - y * 0.045F + seed);

            sx[i] = swayMask * (2.65F * Mth.sin(y * 0.036F + seed + time * 0.012F)
                    + 1.35F * Mth.cos(y * 0.082F - time * 0.019F + seed * 1.7F)) * (0.35F + 1.15F * u);
            sz[i] = swayMask * (2.15F * Mth.cos(y * 0.031F + seed * 1.3F - time * 0.011F)
                    + 1.15F * Mth.sin(y * 0.074F + time * 0.017F)) * (0.35F + 1.15F * u);

            hw[i] = riftHalfWidth(y) * breath * surge;
            hd[i] = riftHalfDepth(y) * breath * surge;
        }

        // 1. Внешнее объёмное 3D-тело Прорыва Пространства (4 скрещённые 3D-плоскости + гранёная призма с текстурой разлома)
        VertexConsumer riftVc = buf.getBuffer(RenderType.entityTranslucent(RIFT_TEX));
        float vScroll = -(time * 0.0085F) % 1.0F;

        for (int i = 0; i < n - 1; ++i) {
            float y0 = Y_STOPS[i], y1 = Y_STOPS[i + 1];
            float v0 = y0 * 0.055F + vScroll;
            float v1 = y1 * 0.055F + vScroll;
            int a0 = crownAlpha(y0), a1 = crownAlpha(y1);

            // 4 диагонально-ортогональных 3D-крыла разрыва (читаются объёмно под ЛЮБЫМ углом 360 градусов!)
            for (int plane = 0; plane < 4; ++plane) {
                double ang = (Math.PI * plane) / 4.0 + (y0 * 0.0045 + seed * 0.5);
                double angNext = (Math.PI * plane) / 4.0 + (y1 * 0.0045 + seed * 0.5);
                float c0 = (float) Math.cos(ang), s0 = (float) Math.sin(ang);
                float c1 = (float) Math.cos(angNext), s1 = (float) Math.sin(angNext);

                float r0 = (plane % 2 == 0) ? hw[i] : hd[i] * 1.32F;
                float r1 = (plane % 2 == 0) ? hw[i + 1] : hd[i + 1] * 1.32F;
                int pa0 = (plane == 0) ? a0 : Math.round(a0 * 0.82F);
                int pa1 = (plane == 0) ? a1 : Math.round(a1 * 0.82F);

                emitDoubleSidedRibbon(riftVc, mat, nrm,
                        sx[i] - c0 * r0, y0, sz[i] - s0 * r0,
                        sx[i] + c0 * r0, y0, sz[i] + s0 * r0,
                        sx[i + 1] + c1 * r1, y1, sz[i + 1] + s1 * r1,
                        sx[i + 1] - c1 * r1, y1, sz[i + 1] - s1 * r1,
                        0.0F, v0, 1.0F, v1,
                        255, 255, 255, pa0, pa1);
            }
        }

        // 2. Внутреннее ослепительное 3D-ядро света Элисара + рваные пространственные гребни-трещины и небесные ветви
        VertexConsumer solidVc = buf.getBuffer(RenderType.entityTranslucent(ShaderCompat.WHITE_TEX));

        // 2a) Восьмигранное 3D-сердце разрыва (тёплое золото-слоновая кость Элисара внутри тёмно-индиговой каймы)
        int sides = 6;
        for (int i = 0; i < n - 1; ++i) {
            float y0 = Y_STOPS[i], y1 = Y_STOPS[i + 1];
            float cw0 = hw[i] * 0.28F, cd0 = hd[i] * 0.34F;
            float cw1 = hw[i + 1] * 0.28F, cd1 = hd[i + 1] * 0.34F;
            int a0 = Math.round(crownAlpha(y0) * 0.94F);
            int a1 = Math.round(crownAlpha(y1) * 0.94F);

            for (int k = 0; k < sides; ++k) {
                float th0 = (Mth.TWO_PI * k) / sides;
                float th1 = (Mth.TWO_PI * (k + 1)) / sides;
                float x00 = sx[i] + Mth.cos(th0) * cw0, z00 = sz[i] + Mth.sin(th0) * cd0;
                float x10 = sx[i] + Mth.cos(th1) * cw0, z10 = sz[i] + Mth.sin(th1) * cd0;
                float x11 = sx[i + 1] + Mth.cos(th1) * cw1, z11 = sz[i + 1] + Mth.sin(th1) * cd1;
                float x01 = sx[i + 1] + Mth.cos(th0) * cw1, z01 = sz[i + 1] + Mth.sin(th0) * cd1;

                emitColorQuad(solidVc, mat, nrm,
                        x00, y0, z00, 255, 250, 234, a0,
                        x10, y0, z10, 255, 250, 234, a0,
                        x11, y1, z11, 252, 236, 188, a1,
                        x01, y1, z01, 252, 236, 188, a1);
            }

            // 2b) Острые зубчатые кромки расколотого пространства по краям 3D-разрыва
            if (i % 2 == 0 && i + 2 < n) {
                float ym = Y_STOPS[i + 1], yt = Y_STOPS[i + 2];
                for (int side = -1; side <= 1; side += 2) {
                    float jag = 1.18F + 0.28F * Mth.sin(i * 1.9F + side + time * 0.04F);
                    float bx0 = sx[i] + side * hw[i] * 0.78F;
                    float bz0 = sz[i];
                    float tipX = sx[i + 1] + side * hw[i + 1] * jag;
                    float tipZ = sz[i + 1] + side * hd[i + 1] * 0.45F * Mth.cos(i * 1.3F);
                    float bx1 = sx[i + 2] + side * hw[i + 2] * 0.78F;
                    float bz1 = sz[i + 2];
                    int ja = Math.round(crownAlpha(ym) * 0.85F);

                    // Тёмно-астральный зубец разрыва с бирюзово-золотым свечением у основания
                    emitDoubleSidedTriAsQuad(solidVc, mat, nrm,
                            bx0, y0, bz0, 92, 220, 248, ja,
                            tipX, ym, tipZ, 26, 20, 56, Math.round(ja * 0.92F),
                            bx1, yt, bz1, 248, 218, 136, ja);
                }
            }
        }

        // 3. Гигантские небесные ветви-молнии Разлома высоко в небе (y = 38 .. 154 блоков),
        //    подчёркивающие колоссальный масштаб прорыва на высоте!
        for (int b = 0; b < 7; ++b) {
            int baseIdx = 15 + b * 2;
            if (baseIdx >= n - 2) break;
            float by = Y_STOPS[baseIdx];
            float ox = sx[baseIdx], oz = sz[baseIdx];
            float dirAng = b * 2.18F + seed;
            float dx = Mth.cos(dirAng), dz = Mth.sin(dirAng);
            float branchLen = (8.5F + b * 4.2F) * surge;
            float baseThick = (0.55F + b * 0.28F) * surge;

            drawSkyBranch(solidVc, mat, nrm, ox, by, oz, dx, 0.58F, dz, branchLen, baseThick, time, b + seed);
        }

        // 4. Парящие 3D-осколки расколотой реальности, медленно утягиваемые вверх вдоль Прорыва
        for (int s = 0; s < 26; ++s) {
            float cycle = ((time * 0.0032F + s / 26.0F) % 1.0F + 1.0F) % 1.0F;
            float sy = cycle * 156.0F + 0.6F;
            float u = sy / TOTAL_HEIGHT;
            float rBase = riftHalfWidth(sy) * (1.08F + 0.35F * Mth.sin(s * 2.7F));
            float orbAng = s * 2.39996F + time * 0.014F * (s % 2 == 0 ? 1 : -1);
            float cx = Mth.cos(orbAng) * rBase;
            float cz = Mth.sin(orbAng) * rBase * 0.65F;
            // Маленькие изящные осколки у земли (0.07 м) -> крупные монолиты в небе (1.25 м)!
            float shardR = (0.065F + 1.18F * u * u) * surge;
            int sa = Math.round(235.0F * Mth.sin(cycle * Mth.PI));
            drawOctahedronShard(solidVc, mat, nrm, cx, sy, cz, shardR, shardR * 2.3F, time * 0.05F + s, sa);
        }

        // 5. Тонкое энергетическое кольцо-печать прямо у земли вокруг точки разрыва (радиус ~0.95 м)
        drawGroundThresholdHalo(solidVc, mat, nrm, time, surge);

        ps.popPose();
    }

    /**
     * Профиль полуширины Прорыва Пространства от земли (y = 0) до неба (y = 168):
     * у земли узкий (~0.11..0.44 м, общая ширина ~0.88 м на уровне глаз),
     * а с высотой стремительно раскрывается до ~21 м полуширины (~42 м общей ширины!) в небе!
     */
    private static float riftHalfWidth(float y) {
        if (y <= 3.0F) {
            float t = y / 3.0F;
            return 0.10F + 0.48F * (float) Math.pow(t, 0.78);
        }
        if (y <= 140.0F) {
            float u = (y - 3.0F) / 137.0F;
            return 0.58F + 20.4F * (float) Math.pow(u, 1.38);
        }
        float crown = 1.0F - Mth.clamp((y - 140.0F) / (TOTAL_HEIGHT - 140.0F), 0.0F, 1.0F);
        return 20.98F * (float) Math.pow(crown, 0.72);
    }

    private static float riftHalfDepth(float y) {
        return riftHalfWidth(y) * 0.46F;
    }

    private static int crownAlpha(float y) {
        if (y < 0.35F) return Math.round(180 + 75 * (y / 0.35F));
        if (y > 146.0F) {
            float f = 1.0F - Mth.clamp((y - 146.0F) / (TOTAL_HEIGHT - 146.0F), 0.0F, 1.0F);
            return Math.round(255.0F * f);
        }
        return 255;
    }

    private static boolean isGate(Level level, BlockPos p) {
        return level.isLoaded(p) && level.getBlockState(p).is(TempleBlocks.DEMON_GATE.get());
    }

    private static void drawSkyBranch(VertexConsumer vc, Matrix4f mat, Matrix3f nrm,
                                      float ox, float oy, float oz,
                                      float dx, float dy, float dz,
                                      float length, float baseW, float time, float seed) {
        int segs = 5;
        float px = ox, py = oy, pz = oz;
        for (int i = 0; i < segs; ++i) {
            float t0 = (float) i / segs, t1 = (float) (i + 1) / segs;
            float step = length / segs;
            float jx = Mth.sin(i * 2.3F + seed + time * 0.03F) * 1.4F;
            float jz = Mth.cos(i * 1.9F + seed - time * 0.025F) * 1.4F;
            float nx = px + dx * step + jx;
            float ny = py + dy * step * (1.0F + 0.25F * t0);
            float nz = pz + dz * step + jz;

            float w0 = baseW * (1.0F - t0 * 0.85F);
            float w1 = baseW * (1.0F - t1 * 0.95F);
            int a0 = Math.round(220 * (1.0F - t0 * 0.55F));
            int a1 = Math.round(220 * (1.0F - t1 * 0.92F));

            // Два скрещённых 3D-ребра ветви
            emitColorDoubleQuad(vc, mat, nrm,
                    px - dz * w0, py, pz + dx * w0, 250, 232, 168, a0,
                    px + dz * w0, py, pz - dx * w0, 96, 218, 248, a0,
                    nx + dz * w1, ny, nz - dx * w1, 32, 24, 68, a1,
                    nx - dz * w1, ny, nz + dx * w1, 250, 232, 168, a1);
            emitColorDoubleQuad(vc, mat, nrm,
                    px, py - w0, pz, 96, 218, 248, a0,
                    px, py + w0, pz, 255, 250, 236, a0,
                    nx, ny + w1, nz, 96, 218, 248, a1,
                    nx, ny - w1, nz, 28, 22, 62, a1);

            px = nx;
            py = ny;
            pz = nz;
        }
    }

    private static void drawOctahedronShard(VertexConsumer vc, Matrix4f mat, Matrix3f nrm,
                                            float cx, float cy, float cz, float r, float h, float rot, int alpha) {
        if (alpha <= 4) return;
        float hh = h * 0.5F;
        for (int k = 0; k < 4; ++k) {
            float a0 = rot + k * Mth.HALF_PI;
            float a1 = rot + (k + 1) * Mth.HALF_PI;
            float x0 = cx + Mth.cos(a0) * r, z0 = cz + Mth.sin(a0) * r;
            float x1 = cx + Mth.cos(a1) * r, z1 = cz + Mth.sin(a1) * r;
            int cr = (k % 2 == 0) ? 250 : 108;
            int cg = (k % 2 == 0) ? 230 : 224;
            int cb = (k % 2 == 0) ? 168 : 248;
            emitDoubleSidedTriAsQuad(vc, mat, nrm,
                    cx, cy + hh, cz, 255, 252, 242, alpha,
                    x0, cy, z0, cr, cg, cb, alpha,
                    x1, cy, z1, 34, 26, 70, alpha);
            emitDoubleSidedTriAsQuad(vc, mat, nrm,
                    cx, cy - hh, cz, 34, 26, 70, alpha,
                    x1, cy, z1, cr, cg, cb, alpha,
                    x0, cy, z0, 250, 230, 168, alpha);
        }
    }

    private static void drawGroundThresholdHalo(VertexConsumer vc, Matrix4f mat, Matrix3f nrm, float time, float surge) {
        int segs = 20;
        float rIn = 0.16F * surge, rMid = 0.56F * surge, rOut = 1.05F * surge;
        float y = 0.035F;
        int aMid = Mth.clamp(Math.round((155 + 30 * Mth.sin(time * 0.09F)) * surge), 0, 245);
        for (int i = 0; i < segs; ++i) {
            float a0 = (Mth.TWO_PI * i) / segs + time * 0.015F;
            float a1 = (Mth.TWO_PI * (i + 1)) / segs + time * 0.015F;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1), s1 = Mth.sin(a1);

            emitColorDoubleQuad(vc, mat, nrm,
                    c0 * rIn, y, s0 * rIn, 255, 252, 240, 235,
                    c1 * rIn, y, s1 * rIn, 255, 252, 240, 235,
                    c1 * rMid, y, s1 * rMid, 242, 206, 108, aMid,
                    c0 * rMid, y, s0 * rMid, 242, 206, 108, aMid);
            emitColorDoubleQuad(vc, mat, nrm,
                    c0 * rMid, y, s0 * rMid, 102, 220, 246, aMid,
                    c1 * rMid, y, s1 * rMid, 102, 220, 246, aMid,
                    c1 * rOut, y, s1 * rOut, 28, 22, 58, 0,
                    c0 * rOut, y, s0 * rOut, 28, 22, 58, 0);
        }
    }

    private static void emitDoubleSidedRibbon(VertexConsumer vc, Matrix4f m, Matrix3f n,
                                              float ax, float ay, float az,
                                              float bx, float by, float bz,
                                              float cx, float cy, float cz,
                                              float dx, float dy, float dz,
                                              float u0, float v0, float u1, float v1,
                                              int r, int g, int b, int aBot, int aTop) {
        vtx(vc, m, n, ax, ay, az, u0, v0, r, g, b, aBot);
        vtx(vc, m, n, bx, by, bz, u1, v0, r, g, b, aBot);
        vtx(vc, m, n, cx, cy, cz, u1, v1, r, g, b, aTop);
        vtx(vc, m, n, dx, dy, dz, u0, v1, r, g, b, aTop);

        vtx(vc, m, n, dx, dy, dz, u0, v1, r, g, b, aTop);
        vtx(vc, m, n, cx, cy, cz, u1, v1, r, g, b, aTop);
        vtx(vc, m, n, bx, by, bz, u1, v0, r, g, b, aBot);
        vtx(vc, m, n, ax, ay, az, u0, v0, r, g, b, aBot);
    }

    private static void emitColorQuad(VertexConsumer vc, Matrix4f m, Matrix3f n,
                                      float ax, float ay, float az, int ar, int ag, int ab, int aa,
                                      float bx, float by, float bz, int br, int bg, int bb, int ba,
                                      float cx, float cy, float cz, int cr, int cg, int cb, int ca,
                                      float dx, float dy, float dz, int dr, int dg, int db, int da) {
        vtx(vc, m, n, ax, ay, az, 0.5F, 0.5F, ar, ag, ab, aa);
        vtx(vc, m, n, bx, by, bz, 0.5F, 0.5F, br, bg, bb, ba);
        vtx(vc, m, n, cx, cy, cz, 0.5F, 0.5F, cr, cg, cb, ca);
        vtx(vc, m, n, dx, dy, dz, 0.5F, 0.5F, dr, dg, db, da);
    }

    private static void emitColorDoubleQuad(VertexConsumer vc, Matrix4f m, Matrix3f n,
                                            float ax, float ay, float az, int ar, int ag, int ab, int aa,
                                            float bx, float by, float bz, int br, int bg, int bb, int ba,
                                            float cx, float cy, float cz, int cr, int cg, int cb, int ca,
                                            float dx, float dy, float dz, int dr, int dg, int db, int da) {
        emitColorQuad(vc, m, n, ax, ay, az, ar, ag, ab, aa, bx, by, bz, br, bg, bb, ba, cx, cy, cz, cr, cg, cb, ca, dx, dy, dz, dr, dg, db, da);
        emitColorQuad(vc, m, n, dx, dy, dz, dr, dg, db, da, cx, cy, cz, cr, cg, cb, ca, bx, by, bz, br, bg, bb, ba, ax, ay, az, ar, ag, ab, aa);
    }

    private static void emitDoubleSidedTriAsQuad(VertexConsumer vc, Matrix4f m, Matrix3f n,
                                                 float ax, float ay, float az, int ar, int ag, int ab, int aa,
                                                 float bx, float by, float bz, int br, int bg, int bb, int ba,
                                                 float cx, float cy, float cz, int cr, int cg, int cb, int ca) {
        emitColorDoubleQuad(vc, m, n,
                ax, ay, az, ar, ag, ab, aa,
                bx, by, bz, br, bg, bb, ba,
                cx, cy, cz, cr, cg, cb, ca,
                ax, ay, az, ar, ag, ab, aa);
    }

    private static void vtx(VertexConsumer vc, Matrix4f m, Matrix3f n,
                            float x, float y, float z, float u, float v,
                            int r, int g, int b, int a) {
        vc.vertex(m, x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(n, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
