package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.entity.BlossomStagEntity;
import ru.aiforge.riftworld.entity.SkyDartFishEntity;

/**
 * v0.27.0: Высокодетализированные и максимально оптимизированные рендереры двух мирных мобов:
 *  1) {@link SkyDartFish} — фарфорово-белая летающая рыбка со складчатыми селадоновыми плавниками
 *     (по фото 1) и 4 прозрачными жилковатыми крылышками стрекозы;
 *  2) {@link BlossomStag} — белоснежный грациозный олень на длинных 3-суставных ногах с персиковыми
 *     ушками (по фото 2) и ветвистыми рогами, на которых растут объёмные 3D-цветы.
 *
 * <p>Вся геометрия предвычислена ОДИН РАЗ при загрузке класса в плоские массивы {@code float[]}
 * ({@link BakedMesh}) без единой аллокации в кадре!
 */
public final class PeacefulFaunaRenderers {
    public static final ResourceLocation SKY_DART_TEX =
            new ResourceLocation(RiftWorld.MODID, "textures/entity/sky_dart_fish.png");
    public static final ResourceLocation BLOSSOM_STAG_TEX =
            new ResourceLocation(RiftWorld.MODID, "textures/entity/blossom_stag.png");

    private PeacefulFaunaRenderers() {
    }

    // =========================================================================
    // ZERO-ALLOCATION PRE-BAKED RIGID BONE MESH
    // =========================================================================
    private static final class MeshBuilder {
        // 8 floats per vertex * 4 vertices = 32 floats per quad: x, y, z, nx, ny, nz, u, v
        private final List<Float> buf = new ArrayList<>(512);

        void quad(float x0, float y0, float z0, float u0, float v0,
                  float x1, float y1, float z1, float u1, float v1,
                  float x2, float y2, float z2, float u2, float v2,
                  float x3, float y3, float z3, float u3, float v3) {
            float ax = x2 - x0, ay = y2 - y0, az = z2 - z0;
            float bx = x3 - x1, by = y3 - y1, bz = z3 - z1;
            float nx = ay * bz - az * by;
            float ny = az * bx - ax * bz;
            float nz = ax * by - ay * bx;
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len < 1e-6F) {
                nx = 0.0F; ny = 1.0F; nz = 0.0F;
            } else {
                nx /= len; ny /= len; nz /= len;
            }
            vert(x0, y0, z0, nx, ny, nz, u0, v0);
            vert(x1, y1, z1, nx, ny, nz, u1, v1);
            vert(x2, y2, z2, nx, ny, nz, u2, v2);
            vert(x3, y3, z3, nx, ny, nz, u3, v3);
        }

        void doubleSidedQuad(float x0, float y0, float z0, float u0, float v0,
                             float x1, float y1, float z1, float u1, float v1,
                             float x2, float y2, float z2, float u2, float v2,
                             float x3, float y3, float z3, float u3, float v3) {
            quad(x0, y0, z0, u0, v0, x1, y1, z1, u1, v1, x2, y2, z2, u2, v2, x3, y3, z3, u3, v3);
            quad(x3, y3, z3, u3, v3, x2, y2, z2, u2, v2, x1, y1, z1, u1, v1, x0, y0, z0, u0, v0);
        }

        private void vert(float x, float y, float z, float nx, float ny, float nz, float u, float v) {
            buf.add(x); buf.add(y); buf.add(z);
            buf.add(nx); buf.add(ny); buf.add(nz);
            buf.add(u); buf.add(v);
        }

        /**
         * Гладкий лофтинг вдоль набора колец {@code {cx, cy, cz, rx, ry}} с мягкими нормалями.
         */
        void loft(float[][] rings, int sides, float uMin, float vMin, float uMax, float vMax, boolean capEnds) {
            int nRings = rings.length;
            float[][][] pts = new float[nRings][sides][3];
            float[][][] nms = new float[nRings][sides][3];
            for (int r = 0; r < nRings; ++r) {
                float cx = rings[r][0], cy = rings[r][1], cz = rings[r][2];
                float rx = rings[r][3], ry = rings[r][4];
                // Касательный вектор оси для ориентации кольца в плоскости шеи/конечности
                int rPrev = Math.max(0, r - 1), rNext = Math.min(nRings - 1, r + 1);
                float tx = rings[rNext][0] - rings[rPrev][0];
                float ty = rings[rNext][1] - rings[rPrev][1];
                float tz = rings[rNext][2] - rings[rPrev][2];
                float tLen = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
                if (tLen < 1e-5F) { tx = 0; ty = 0; tz = 1; } else { tx /= tLen; ty /= tLen; tz /= tLen; }
                // Ортонормированный базис (sideX, upY) вокруг касательной
                float refUx = 0.0F, refUy = 1.0F, refUz = 0.0F;
                if (Math.abs(ty) > 0.92F) { refUx = 0.0F; refUy = 0.0F; refUz = 1.0F; }
                float sx = refUy * tz - refUz * ty;
                float sy = refUz * tx - refUx * tz;
                float sz = refUx * ty - refUy * tx;
                float sLen = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
                sx /= sLen; sy /= sLen; sz /= sLen;
                float ux = ty * sz - tz * sy;
                float uy = tz * sx - tx * sz;
                float uz = tx * sy - ty * sx;

                for (int s = 0; s < sides; ++s) {
                    double a = (s * 2.0 * Math.PI) / sides;
                    float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
                    pts[r][s][0] = cx + sx * (ca * rx) + ux * (sa * ry);
                    pts[r][s][1] = cy + sy * (ca * rx) + uy * (sa * ry);
                    pts[r][s][2] = cz + sz * (ca * rx) + uz * (sa * ry);
                    float nwx = sx * ca + ux * sa;
                    float nwy = sy * ca + uy * sa;
                    float nwz = sz * ca + uz * sa;
                    float nl = (float) Math.sqrt(nwx * nwx + nwy * nwy + nwz * nwz);
                    nms[r][s][0] = nwx / Math.max(1e-5F, nl);
                    nms[r][s][1] = nwy / Math.max(1e-5F, nl);
                    nms[r][s][2] = nwz / Math.max(1e-5F, nl);
                }
            }
            for (int r = 0; r < nRings - 1; ++r) {
                float v0 = Mth.lerp((float) r / (nRings - 1), vMin, vMax);
                float v1 = Mth.lerp((float) (r + 1) / (nRings - 1), vMin, vMax);
                for (int s = 0; s < sides; ++s) {
                    int s1 = (s + 1) % sides;
                    float u0 = Mth.lerp((float) s / sides, uMin, uMax);
                    float u1 = Mth.lerp((float) (s + 1) / sides, uMin, uMax);
                    vert(pts[r][s][0], pts[r][s][1], pts[r][s][2], nms[r][s][0], nms[r][s][1], nms[r][s][2], u0, v0);
                    vert(pts[r][s1][0], pts[r][s1][1], pts[r][s1][2], nms[r][s1][0], nms[r][s1][1], nms[r][s1][2], u1, v0);
                    vert(pts[r + 1][s1][0], pts[r + 1][s1][1], pts[r + 1][s1][2], nms[r + 1][s1][0], nms[r + 1][s1][1], nms[r + 1][s1][2], u1, v1);
                    vert(pts[r + 1][s][0], pts[r + 1][s][1], pts[r + 1][s][2], nms[r + 1][s][0], nms[r + 1][s][1], nms[r + 1][s][2], u0, v1);
                }
            }
            if (capEnds && sides >= 4) {
                float uMid = 0.5F * (uMin + uMax), vMid = 0.5F * (vMin + vMax);
                float[] c0 = rings[0];
                for (int s = 0; s < sides; s += 2) {
                    int s1 = (s + 1) % sides, s2 = (s + 2) % sides;
                    quad(c0[0], c0[1], c0[2], uMid, vMid,
                            pts[0][s2][0], pts[0][s2][1], pts[0][s2][2], uMax, vMin,
                            pts[0][s1][0], pts[0][s1][1], pts[0][s1][2], uMax, vMax,
                            pts[0][s][0], pts[0][s][1], pts[0][s][2], uMin, vMax);
                }
                float[] cN = rings[nRings - 1];
                int last = nRings - 1;
                for (int s = 0; s < sides; s += 2) {
                    int s1 = (s + 1) % sides, s2 = (s + 2) % sides;
                    quad(cN[0], cN[1], cN[2], uMid, vMid,
                            pts[last][s][0], pts[last][s][1], pts[last][s][2], uMin, vMax,
                            pts[last][s1][0], pts[last][s1][1], pts[last][s1][2], uMax, vMax,
                            pts[last][s2][0], pts[last][s2][1], pts[last][s2][2], uMax, vMin);
                }
            }
        }

        BakedMesh bake() {
            float[] arr = new float[buf.size()];
            for (int i = 0; i < arr.length; ++i) arr[i] = buf.get(i);
            return new BakedMesh(arr);
        }
    }

    private static final class BakedMesh {
        private final float[] data;

        BakedMesh(float[] data) {
            this.data = data;
        }

        void emit(PoseStack pose, VertexConsumer vc, int light, int overlay, int r, int g, int b, int a) {
            PoseStack.Pose last = pose.last();
            Matrix4f m4 = last.pose();
            Matrix3f m3 = last.normal();
            float[] d = this.data;
            for (int i = 0, n = d.length; i < n; i += 8) {
                vc.vertex(m4, d[i], d[i + 1], d[i + 2])
                        .color(r, g, b, a)
                        .uv(d[i + 6], d[i + 7])
                        .overlayCoords(overlay)
                        .uv2(light)
                        .normal(m3, d[i + 3], d[i + 4], d[i + 5])
                        .endVertex();
            }
        }
    }

    // =========================================================================
    // 1. SKY-DART DRAGONFLY FISH (PHOTO 1: PORCELAIN NECK + CELADON FINS + 4 DRAGONFLY WINGS)
    // =========================================================================
    private static final BakedMesh FISH_TORSO_AND_NECK;
    private static final BakedMesh FISH_TAIL_1;
    private static final BakedMesh FISH_TAIL_2;
    private static final BakedMesh FISH_TAIL_VEIL;
    private static final BakedMesh FISH_FIN_LEFT;
    private static final BakedMesh FISH_FIN_RIGHT;
    private static final BakedMesh FISH_FOREWING_LEFT;
    private static final BakedMesh FISH_FOREWING_RIGHT;
    private static final BakedMesh FISH_HINDWING_LEFT;
    private static final BakedMesh FISH_HINDWING_RIGHT;

    static {
        // 1A) Гладкое фарфоровое веретено тела + изогнутая лебединая шейка и головка (по фото 1!)
        MeshBuilder body = new MeshBuilder();
        body.loft(new float[][]{
                {0.0F, 0.16F, -0.18F, 0.036F, 0.044F},
                {0.0F, 0.165F, -0.09F, 0.062F, 0.076F},
                {0.0F, 0.17F, 0.01F, 0.072F, 0.088F},
                {0.0F, 0.185F, 0.10F, 0.060F, 0.072F},
                {0.0F, 0.225F, 0.17F, 0.042F, 0.048F},
                {0.0F, 0.275F, 0.22F, 0.036F, 0.040F},
                {0.0F, 0.305F, 0.275F, 0.042F, 0.044F},
                {0.0F, 0.298F, 0.335F, 0.026F, 0.028F},
                {0.0F, 0.286F, 0.372F, 0.008F, 0.009F}
        }, 8, 0.02F, 0.02F, 0.48F, 0.48F, true);
        // Глазки-бусинки с бликом (UV в области (0..15, 56..63) -> u=0..0.24, v=0.875..0.99)
        for (int side : new int[]{-1, 1}) {
            float ex = side * 0.039F;
            body.doubleSidedQuad(
                    ex, 0.292F, 0.268F, 0.02F, 0.99F,
                    ex, 0.292F, 0.304F, 0.23F, 0.99F,
                    ex, 0.318F, 0.304F, 0.23F, 0.88F,
                    ex, 0.318F, 0.268F, 0.02F, 0.88F);
        }
        // Тонкий селадоновый спинной гребешок
        body.doubleSidedQuad(
                0.0F, 0.245F, -0.14F, 0.52F, 0.46F,
                0.0F, 0.252F, 0.06F, 0.96F, 0.46F,
                0.0F, 0.315F, -0.02F, 0.96F, 0.04F,
                0.0F, 0.292F, -0.18F, 0.52F, 0.04F);
        FISH_TORSO_AND_NECK = body.bake();

        // 1B) 3-сегментный гибкий хвостик + полупрозрачный селадоновый веер
        MeshBuilder t1 = new MeshBuilder();
        t1.loft(new float[][]{
                {0.0F, 0.0F, 0.0F, 0.036F, 0.044F},
                {0.0F, 0.0F, -0.065F, 0.027F, 0.034F},
                {0.0F, 0.004F, -0.13F, 0.019F, 0.025F}
        }, 6, 0.05F, 0.05F, 0.45F, 0.45F, false);
        FISH_TAIL_1 = t1.bake();

        MeshBuilder t2 = new MeshBuilder();
        t2.loft(new float[][]{
                {0.0F, 0.0F, 0.0F, 0.019F, 0.025F},
                {0.0F, 0.008F, -0.06F, 0.013F, 0.018F},
                {0.0F, 0.015F, -0.115F, 0.007F, 0.011F}
        }, 6, 0.05F, 0.05F, 0.45F, 0.45F, true);
        FISH_TAIL_2 = t2.bake();

        MeshBuilder tv = new MeshBuilder();
        // Раздвоенный вуалевый селадоновый хвостовой плавник (UV 0.52..0.98, 0.02..0.48)
        tv.doubleSidedQuad(
                0.0F, 0.005F, 0.0F, 0.52F, 0.24F,
                0.018F, 0.095F, -0.165F, 0.98F, 0.02F,
                0.0F, 0.018F, -0.115F, 0.82F, 0.24F,
                -0.018F, -0.075F, -0.155F, 0.98F, 0.48F);
        FISH_TAIL_VEIL = tv.bake();

        // 1C) Складчатые грудные плавники-веера селадонового оттенка (1:1 с фото 1!)
        FISH_FIN_LEFT = buildPleatedFin(1.0F);
        FISH_FIN_RIGHT = buildPleatedFin(-1.0F);

        // 1D) 4 прозрачных крылышка стрекозы с жилкованием (UV 0.0..1.0, 0.50..0.875)
        FISH_FOREWING_LEFT = buildDragonflyWing(1.0F, 0.46F, 0.115F);
        FISH_FOREWING_RIGHT = buildDragonflyWing(-1.0F, 0.46F, 0.115F);
        FISH_HINDWING_LEFT = buildDragonflyWing(1.0F, 0.37F, 0.098F);
        FISH_HINDWING_RIGHT = buildDragonflyWing(-1.0F, 0.37F, 0.098F);
    }

    private static BakedMesh buildPleatedFin(float side) {
        MeshBuilder m = new MeshBuilder();
        int pleats = 5;
        for (int i = 0; i < pleats; ++i) {
            float a0 = (float) i / pleats;
            float a1 = (float) (i + 1) / pleats;
            float ang0 = -0.15F - a0 * 0.95F;
            float ang1 = -0.15F - a1 * 0.95F;
            float rTip0 = 0.165F + 0.025F * (float) Math.sin(a0 * Math.PI);
            float rTip1 = 0.165F + 0.025F * (float) Math.sin(a1 * Math.PI);
            float ridge0 = ((i & 1) == 0) ? 0.012F : -0.008F;
            float ridge1 = (((i + 1) & 1) == 0) ? 0.012F : -0.008F;

            float xTip0 = side * (0.095F + rTip0 * 0.55F * (float) Math.cos(ang0));
            float yTip0 = -0.085F + rTip0 * 0.55F * (float) Math.sin(ang0) + ridge0;
            float zTip0 = -0.02F - a0 * 0.14F;

            float xTip1 = side * (0.095F + rTip1 * 0.55F * (float) Math.cos(ang1));
            float yTip1 = -0.085F + rTip1 * 0.55F * (float) Math.sin(ang1) + ridge1;
            float zTip1 = -0.02F - a1 * 0.14F;

            float u0 = Mth.lerp(a0, 0.52F, 0.98F);
            float u1 = Mth.lerp(a1, 0.52F, 0.98F);
            m.doubleSidedQuad(
                    0.0F, 0.0F, -a0 * 0.03F, u0, 0.02F,
                    0.0F, 0.0F, -a1 * 0.03F, u1, 0.02F,
                    xTip1, yTip1, zTip1, u1, 0.48F,
                    xTip0, yTip0, zTip0, u0, 0.48F);
        }
        return m.bake();
    }

    private static BakedMesh buildDragonflyWing(float side, float span, float maxChord) {
        MeshBuilder m = new MeshBuilder();
        float[] uFrac = {0.0F, 0.22F, 0.55F, 0.82F, 1.0F};
        float[] leadZ = {0.018F, 0.028F, 0.024F, 0.008F, -0.018F};
        float[] trailZ = {-0.016F, -maxChord * 0.68F, -maxChord, -maxChord * 0.76F, -0.038F};
        float[] archY = {0.0F, 0.014F, 0.020F, 0.011F, -0.004F};
        for (int i = 0; i < uFrac.length - 1; ++i) {
            float x0 = side * uFrac[i] * span;
            float x1 = side * uFrac[i + 1] * span;
            float u0 = uFrac[i], u1 = uFrac[i + 1];
            m.doubleSidedQuad(
                    x0, archY[i], leadZ[i], u0, 0.505F,
                    x1, archY[i + 1], leadZ[i + 1], u1, 0.505F,
                    x1, archY[i + 1] - 0.006F, trailZ[i + 1], u1, 0.865F,
                    x0, archY[i] - 0.006F, trailZ[i], u0, 0.865F);
        }
        return m.bake();
    }

    public static final class SkyDartFish extends EntityRenderer<SkyDartFishEntity> {
        public SkyDartFish(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.22F;
        }

        @Override
        public ResourceLocation getTextureLocation(SkyDartFishEntity entity) {
            return SKY_DART_TEX;
        }

        @Override
        public void render(SkyDartFishEntity entity, float entityYaw, float partialTick,
                           PoseStack pose, MultiBufferSource buf, int packedLight) {
            pose.pushPose();
            float age = entity.tickCount + partialTick + entity.wingPhaseOffset * 10.0F;
            float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
            float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
            float roll = entity.getRoll(partialTick);

            float hoverBob = (float) Math.sin(age * 0.22F) * 0.032F;
            pose.translate(0.0F, 0.06F + hoverBob, 0.0F);
            pose.mulPose(Axis.YP.rotationDegrees(-yaw));
            pose.mulPose(Axis.XP.rotationDegrees(pitch));
            pose.mulPose(Axis.ZP.rotationDegrees(roll));

            // 1. Opaque porcelain body + articulated tail + pleated celadon pectoral fans
            VertexConsumer cutout = buf.getBuffer(RenderType.entityCutoutNoCull(SKY_DART_TEX));
            FISH_TORSO_AND_NECK.emit(pose, cutout, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);

            // Undulating 3-link tail
            float tailSpeed = entity.isHoveringFlower() ? 0.34F : 0.52F;
            float tailAmp = entity.isHoveringFlower() ? 11.0F : 17.5F;
            pose.pushPose();
            pose.translate(0.0F, 0.16F, -0.18F);
            pose.mulPose(Axis.YP.rotationDegrees((float) Math.sin(age * tailSpeed) * tailAmp));
            FISH_TAIL_1.emit(pose, cutout, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);

            pose.translate(0.0F, 0.004F, -0.13F);
            pose.mulPose(Axis.YP.rotationDegrees((float) Math.sin(age * tailSpeed - 0.85F) * (tailAmp * 1.25F)));
            FISH_TAIL_2.emit(pose, cutout, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);

            pose.translate(0.0F, 0.015F, -0.115F);
            pose.mulPose(Axis.YP.rotationDegrees((float) Math.sin(age * tailSpeed - 1.55F) * (tailAmp * 1.4F)));
            FISH_TAIL_VEIL.emit(pose, cutout, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);
            pose.popPose();

            // Pleated celadon pectoral fins rippling gently beneath the chest
            float finWave = (float) Math.sin(age * 0.28F) * 10.0F;
            pose.pushPose();
            pose.translate(0.052F, 0.145F, 0.075F);
            pose.mulPose(Axis.ZP.rotationDegrees(finWave));
            FISH_FIN_LEFT.emit(pose, cutout, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);
            pose.popPose();

            pose.pushPose();
            pose.translate(-0.052F, 0.145F, 0.075F);
            pose.mulPose(Axis.ZP.rotationDegrees(-finWave));
            FISH_FIN_RIGHT.emit(pose, cutout, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);
            pose.popPose();

            // 2. Four translucent veined dragonfly wings beating in counter-phase figure-8!
            VertexConsumer trans = buf.getBuffer(RenderType.entityTranslucent(SKY_DART_TEX));
            float wingFreq = (entity.isHoveringFlower() || entity.getJoyTicks() > 0) ? 1.85F : 1.48F;
            float foreFlap = (float) Math.sin(age * wingFreq) * 34.0F + 12.0F;
            float foreFeather = (float) Math.cos(age * wingFreq) * 16.0F;
            float hindFlap = (float) Math.sin(age * wingFreq - 0.95F) * 30.0F + 8.0F;
            float hindFeather = (float) Math.cos(age * wingFreq - 0.95F) * 14.0F;

            // Forewings
            pose.pushPose();
            pose.translate(0.042F, 0.215F, 0.075F);
            pose.mulPose(Axis.YP.rotationDegrees(10.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(foreFlap));
            pose.mulPose(Axis.XP.rotationDegrees(foreFeather));
            FISH_FOREWING_LEFT.emit(pose, trans, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 235);
            pose.popPose();

            pose.pushPose();
            pose.translate(-0.042F, 0.215F, 0.075F);
            pose.mulPose(Axis.YP.rotationDegrees(-10.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(-foreFlap));
            pose.mulPose(Axis.XP.rotationDegrees(foreFeather));
            FISH_FOREWING_RIGHT.emit(pose, trans, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 235);
            pose.popPose();

            // Hindwings
            pose.pushPose();
            pose.translate(0.038F, 0.202F, -0.028F);
            pose.mulPose(Axis.YP.rotationDegrees(-12.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(hindFlap));
            pose.mulPose(Axis.XP.rotationDegrees(hindFeather));
            FISH_HINDWING_LEFT.emit(pose, trans, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 230);
            pose.popPose();

            pose.pushPose();
            pose.translate(-0.038F, 0.202F, -0.028F);
            pose.mulPose(Axis.YP.rotationDegrees(12.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(-hindFlap));
            pose.mulPose(Axis.XP.rotationDegrees(hindFeather));
            FISH_HINDWING_RIGHT.emit(pose, trans, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 230);
            pose.popPose();

            pose.popPose();
            super.render(entity, entityYaw, partialTick, pose, buf, packedLight);
        }
    }

    // =========================================================================
    // 2. SNOW-WHITE BLOSSOM STAG (PHOTO 2: ULTRA-GRACEFUL DEER + FLOWER ANTLERS)
    // =========================================================================
    private static final BakedMesh STAG_TORSO;
    private static final BakedMesh STAG_TAIL;
    private static final BakedMesh STAG_NECK_LOWER;
    private static final BakedMesh STAG_NECK_UPPER;
    private static final BakedMesh STAG_HEAD;
    private static final BakedMesh STAG_EAR_LEFT;
    private static final BakedMesh STAG_EAR_RIGHT;
    private static final BakedMesh STAG_FRONT_THIGH;
    private static final BakedMesh STAG_FRONT_SHIN;
    private static final BakedMesh STAG_CANNON_HOOF;
    private static final BakedMesh STAG_HIND_THIGH;
    private static final BakedMesh STAG_HIND_SHIN;
    private static final BakedMesh STAG_ANTLER_LEFT;
    private static final BakedMesh STAG_ANTLER_RIGHT;
    private static final BakedMesh[] STAG_BLOSSOMS_LEFT = new BakedMesh[2];
    private static final BakedMesh[] STAG_BLOSSOMS_RIGHT = new BakedMesh[2];

    static {
        // UV layout in blossom_stag.png (64x64):
        //   White coat:       u=0.02..0.48, v=0.02..0.48
        //   Peach inner ear:  u=0.51..0.74, v=0.02..0.23
        //   Ivory antler/hoof:u=0.76..0.98, v=0.02..0.23
        //   Green calyx:      u=0.52..0.96, v=0.26..0.48
        //   Sakura/white flw: u=0.02..0.48, v=0.52..0.98
        //   Lilac-pearl flw:  u=0.52..0.73, v=0.52..0.73
        //   Dark almond eye:  u=0.76..0.98, v=0.76..0.98

        // 2A) Стройное изящное туловище оленя (высота спины ~1.16 м, глубокая узкая грудь, подтянутая талия)
        MeshBuilder torso = new MeshBuilder();
        torso.loft(new float[][]{
                {0.0F, 1.13F, -0.37F, 0.055F, 0.068F},
                {0.0F, 1.13F, -0.27F, 0.104F, 0.132F},
                {0.0F, 1.11F, -0.10F, 0.098F, 0.128F},
                {0.0F, 1.10F, 0.08F, 0.108F, 0.148F},
                {0.0F, 1.11F, 0.24F, 0.116F, 0.162F},
                {0.0F, 1.14F, 0.34F, 0.082F, 0.112F}
        }, 8, 0.04F, 0.04F, 0.46F, 0.46F, true);
        STAG_TORSO = torso.bake();

        // Аккуратный белоснежный хвостик оленя
        MeshBuilder tail = new MeshBuilder();
        tail.loft(new float[][]{
                {0.0F, 0.0F, 0.0F, 0.028F, 0.032F},
                {0.0F, -0.03F, -0.075F, 0.034F, 0.026F},
                {0.0F, -0.085F, -0.135F, 0.010F, 0.010F}
        }, 6, 0.05F, 0.05F, 0.45F, 0.45F, true);
        STAG_TAIL = tail.bake();

        // 2B) Длинная S-образная грациозная шея (2 сегмента: нижний и верхний)
        MeshBuilder neckLo = new MeshBuilder();
        neckLo.loft(new float[][]{
                {0.0F, -0.02F, -0.02F, 0.074F, 0.092F},
                {0.0F, 0.10F, 0.03F, 0.060F, 0.072F},
                {0.0F, 0.23F, 0.07F, 0.049F, 0.058F}
        }, 8, 0.04F, 0.04F, 0.46F, 0.46F, false);
        STAG_NECK_LOWER = neckLo.bake();

        MeshBuilder neckUp = new MeshBuilder();
        neckUp.loft(new float[][]{
                {0.0F, 0.0F, 0.0F, 0.049F, 0.058F},
                {0.0F, 0.12F, 0.015F, 0.043F, 0.050F},
                {0.0F, 0.24F, 0.025F, 0.040F, 0.045F}
        }, 8, 0.04F, 0.04F, 0.46F, 0.46F, true);
        STAG_NECK_UPPER = neckUp.bake();

        // 2C) Утончённая голова оленя с нежно-розовым носиком и выразительными тёмными глазами (по фото 2!)
        MeshBuilder head = new MeshBuilder();
        head.loft(new float[][]{
                {0.0F, 0.02F, -0.055F, 0.048F, 0.054F},
                {0.0F, 0.035F, 0.015F, 0.062F, 0.066F},
                {0.0F, 0.025F, 0.085F, 0.052F, 0.055F},
                {0.0F, 0.005F, 0.155F, 0.032F, 0.035F},
                {0.0F, -0.006F, 0.215F, 0.021F, 0.022F}
        }, 8, 0.04F, 0.04F, 0.46F, 0.46F, true);
        // Нежно-персиковый кончик носа
        head.loft(new float[][]{
                {0.0F, -0.005F, 0.208F, 0.021F, 0.022F},
                {0.0F, -0.006F, 0.228F, 0.012F, 0.013F}
        }, 6, 0.54F, 0.04F, 0.72F, 0.20F, true);
        // Миндалевидные тёмные глаза по бокам головы
        for (int side : new int[]{-1, 1}) {
            float ex = side * 0.057F;
            head.doubleSidedQuad(
                    ex, 0.016F, 0.038F, 0.77F, 0.97F,
                    ex * 0.94F, 0.014F, 0.082F, 0.97F, 0.97F,
                    ex * 0.94F, 0.048F, 0.080F, 0.97F, 0.77F,
                    ex, 0.050F, 0.036F, 0.77F, 0.77F);
        }
        STAG_HEAD = head.bake();

        // 2D) Крупные изящные ушки с тёпло-персиковой внутренней раковиной (1:1 с фото 2!)
        STAG_EAR_LEFT = buildDeerEar(1.0F);
        STAG_EAR_RIGHT = buildDeerEar(-1.0F);

        // 2E) Длинные стройные 3-суставные ноги (плечо/бедро -> предплечье/голень -> пясть и светлое копытце)
        MeshBuilder fThigh = new MeshBuilder();
        fThigh.loft(new float[][]{
                {0.0F, 0.02F, 0.0F, 0.046F, 0.056F},
                {0.0F, -0.18F, -0.01F, 0.034F, 0.040F},
                {0.0F, -0.36F, -0.015F, 0.027F, 0.030F}
        }, 6, 0.04F, 0.04F, 0.46F, 0.46F, true);
        STAG_FRONT_THIGH = fThigh.bake();

        MeshBuilder fShin = new MeshBuilder();
        fShin.loft(new float[][]{
                {0.0F, 0.0F, 0.0F, 0.028F, 0.030F},
                {0.0F, -0.18F, 0.005F, 0.021F, 0.023F},
                {0.0F, -0.36F, 0.008F, 0.019F, 0.021F}
        }, 6, 0.04F, 0.04F, 0.46F, 0.46F, false);
        STAG_FRONT_SHIN = fShin.bake();

        MeshBuilder hoof = new MeshBuilder();
        hoof.loft(new float[][]{
                {0.0F, 0.0F, 0.0F, 0.019F, 0.021F},
                {0.0F, -0.24F, 0.010F, 0.017F, 0.019F},
                {0.0F, -0.30F, 0.016F, 0.021F, 0.024F}
        }, 6, 0.04F, 0.04F, 0.46F, 0.46F, false);
        // Светлое слоново-костяное копытце у самой земли (y = -0.30..-0.35)
        hoof.loft(new float[][]{
                {0.0F, -0.30F, 0.016F, 0.022F, 0.025F},
                {0.0F, -0.35F, 0.024F, 0.024F, 0.028F}
        }, 6, 0.77F, 0.03F, 0.97F, 0.22F, true);
        STAG_CANNON_HOOF = hoof.bake();

        MeshBuilder hThigh = new MeshBuilder();
        hThigh.loft(new float[][]{
                {0.0F, 0.03F, 0.0F, 0.056F, 0.072F},
                {0.0F, -0.18F, -0.035F, 0.040F, 0.050F},
                {0.0F, -0.36F, -0.065F, 0.029F, 0.034F}
        }, 6, 0.04F, 0.04F, 0.46F, 0.46F, true);
        STAG_HIND_THIGH = hThigh.bake();

        MeshBuilder hShin = new MeshBuilder();
        hShin.loft(new float[][]{
                {0.0F, 0.0F, 0.0F, 0.029F, 0.034F},
                {0.0F, -0.18F, 0.025F, 0.022F, 0.024F},
                {0.0F, -0.35F, 0.045F, 0.019F, 0.021F}
        }, 6, 0.04F, 0.04F, 0.46F, 0.46F, false);
        STAG_HIND_SHIN = hShin.bake();

        // 2F) Ветвистые светлые рога + живые объёмные 3D-цветы на ветвях!
        STAG_ANTLER_LEFT = buildAntlerBranches(1.0F);
        STAG_ANTLER_RIGHT = buildAntlerBranches(-1.0F);
        for (int pal = 0; pal < 2; ++pal) {
            STAG_BLOSSOMS_LEFT[pal] = buildAntlerBlossoms(1.0F, pal == 1);
            STAG_BLOSSOMS_RIGHT[pal] = buildAntlerBlossoms(-1.0F, pal == 1);
        }
    }

    private static BakedMesh buildDeerEar(float side) {
        MeshBuilder m = new MeshBuilder();
        // Наружная белоснежная поверхность уха
        m.loft(new float[][]{
                {0.0F, 0.0F, 0.0F, 0.016F, 0.014F},
                {side * 0.045F, 0.055F, -0.012F, 0.034F, 0.016F},
                {side * 0.095F, 0.115F, -0.020F, 0.030F, 0.013F},
                {side * 0.132F, 0.162F, -0.024F, 0.008F, 0.006F}
        }, 6, 0.05F, 0.05F, 0.45F, 0.45F, true);
        // Тёпло-персиковая внутренняя ложбинка уха (обращена вперёд, как на фото 2!)
        m.doubleSidedQuad(
                side * 0.022F, 0.028F, -0.002F, 0.53F, 0.21F,
                side * 0.062F, 0.048F, 0.004F, 0.72F, 0.21F,
                side * 0.118F, 0.145F, -0.010F, 0.72F, 0.04F,
                side * 0.072F, 0.118F, -0.006F, 0.53F, 0.04F);
        return m.bake();
    }

    private static BakedMesh buildAntlerBranches(float side) {
        MeshBuilder m = new MeshBuilder();
        // Главный изящно изогнутый ствол рога (вверх-назад-в стороны и затем венцом вверх)
        m.loft(new float[][]{
                {0.0F, 0.0F, 0.0F, 0.018F, 0.018F},
                {side * 0.055F, 0.11F, -0.035F, 0.015F, 0.015F},
                {side * 0.135F, 0.23F, -0.075F, 0.013F, 0.013F},
                {side * 0.185F, 0.36F, -0.065F, 0.010F, 0.010F},
                {side * 0.165F, 0.48F, -0.020F, 0.005F, 0.005F}
        }, 6, 0.77F, 0.03F, 0.97F, 0.22F, true);
        // Надглазничный передний отросток (brow tine)
        m.loft(new float[][]{
                {side * 0.052F, 0.105F, -0.032F, 0.011F, 0.011F},
                {side * 0.085F, 0.175F, 0.045F, 0.008F, 0.008F},
                {side * 0.075F, 0.235F, 0.095F, 0.004F, 0.004F}
        }, 5, 0.77F, 0.03F, 0.97F, 0.22F, true);
        // Средний королевский отросток (royal tine)
        m.loft(new float[][]{
                {side * 0.132F, 0.225F, -0.072F, 0.010F, 0.010F},
                {side * 0.115F, 0.335F, -0.010F, 0.007F, 0.007F},
                {side * 0.092F, 0.425F, 0.035F, 0.004F, 0.004F}
        }, 5, 0.77F, 0.03F, 0.97F, 0.22F, true);
        // Внешний верхний отросток венца
        m.loft(new float[][]{
                {side * 0.175F, 0.335F, -0.068F, 0.009F, 0.009F},
                {side * 0.245F, 0.415F, -0.095F, 0.006F, 0.006F},
                {side * 0.268F, 0.485F, -0.085F, 0.004F, 0.004F}
        }, 5, 0.77F, 0.03F, 0.97F, 0.22F, true);
        return m.bake();
    }

    private static BakedMesh buildAntlerBlossoms(float side, boolean lilacPalette) {
        MeshBuilder m = new MeshBuilder();
        float u0 = lilacPalette ? 0.52F : 0.02F;
        float v0 = 0.52F;
        float u1 = lilacPalette ? 0.73F : 0.48F;
        float v1 = lilacPalette ? 0.73F : 0.98F;

        // 7 распустившихся 3D-цветов и зелёных листочков вдоль ветвей каждого рога!
        float[][] spots = {
                {side * 0.075F, 0.238F, 0.095F, 0.048F},
                {side * 0.118F, 0.245F, -0.068F, 0.056F},
                {side * 0.094F, 0.428F, 0.035F, 0.052F},
                {side * 0.182F, 0.365F, -0.062F, 0.060F},
                {side * 0.165F, 0.485F, -0.018F, 0.055F},
                {side * 0.268F, 0.488F, -0.085F, 0.050F},
                {side * 0.215F, 0.418F, -0.082F, 0.044F}
        };
        for (int idx = 0; idx < spots.length; ++idx) {
            float cx = spots[idx][0], cy = spots[idx][1], cz = spots[idx][2], rad = spots[idx][3];
            // 2 изумрудных чашелистика под цветком
            for (int lf = 0; lf < 2; ++lf) {
                double la = idx * 1.3 + lf * Math.PI * 0.65;
                float lx = (float) Math.cos(la) * rad * 0.85F;
                float lz = (float) Math.sin(la) * rad * 0.85F;
                m.doubleSidedQuad(
                        cx - lz * 0.35F, cy - 0.008F, cz + lx * 0.35F, 0.54F, 0.46F,
                        cx + lz * 0.35F, cy - 0.008F, cz - lx * 0.35F, 0.94F, 0.46F,
                        cx + lx + lz * 0.2F, cy + 0.008F, cz + lz - lx * 0.2F, 0.94F, 0.28F,
                        cx + lx - lz * 0.2F, cy + 0.008F, cz + lz + lx * 0.2F, 0.54F, 0.28F);
            }
            // 5 объёмных раскрытых лепестков чашечкой вверх!
            float uMid = 0.5F * (u0 + u1), vMid = 0.5F * (v0 + v1);
            for (int p = 0; p < 5; ++p) {
                double a = idx * 0.7 + (p * 2.0 * Math.PI) / 5.0;
                double aL = a - 0.48, aR = a + 0.48;
                float pxL = cx + (float) Math.cos(aL) * rad * 0.68F;
                float pzL = cz + (float) Math.sin(aL) * rad * 0.68F;
                float pxT = cx + (float) Math.cos(a) * rad;
                float pzT = cz + (float) Math.sin(a) * rad;
                float pxR = cx + (float) Math.cos(aR) * rad * 0.68F;
                float pzR = cz + (float) Math.sin(aR) * rad * 0.68F;
                float cupY = cy + rad * 0.38F;
                m.doubleSidedQuad(
                        cx, cy, cz, uMid, vMid,
                        pxL, cupY * 0.5F + cy * 0.5F, pzL, u0, vMid,
                        pxT, cupY, pzT, uMid, v0,
                        pxR, cupY * 0.5F + cy * 0.5F, pzR, u1, vMid);
            }
        }
        return m.bake();
    }

    public static final class BlossomStag extends EntityRenderer<BlossomStagEntity> {
        public BlossomStag(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.52F;
        }

        @Override
        public ResourceLocation getTextureLocation(BlossomStagEntity entity) {
            return BLOSSOM_STAG_TEX;
        }

        @Override
        public void render(BlossomStagEntity entity, float entityYaw, float partialTick,
                           PoseStack pose, MultiBufferSource buf, int packedLight) {
            pose.pushPose();
            float age = entity.tickCount + partialTick;
            float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
            float headYaw = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
            float netHeadYaw = Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -55.0F, 55.0F);
            float headPitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

            float walkPos = entity.walkAnimation.position(partialTick);
            float walkSpeed = Math.min(1.0F, entity.walkAnimation.speed(partialTick));
            float graze = entity.getGrazeProgress(partialTick);
            float nibble = entity.getGrazeNibbleWave(partialTick);

            if (entity.isBaby()) {
                pose.scale(0.58F, 0.58F, 0.58F);
            }

            // Лёгкая пружинящая рысь корпуса и дыхание
            float strideBob = Math.abs(Mth.sin(walkPos * 0.6662F)) * walkSpeed * 0.038F;
            float stridePitch = Mth.cos(walkPos * 0.6662F * 2.0F) * walkSpeed * 1.8F;
            pose.translate(0.0F, strideBob, 0.0F);
            pose.mulPose(Axis.YP.rotationDegrees(-bodyYaw));

            VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(BLOSSOM_STAG_TEX));

            // 1. Torso & Tail
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(stridePitch + graze * 4.0F));
            STAG_TORSO.emit(pose, vc, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);

            // Tail flick
            pose.pushPose();
            pose.translate(0.0F, 1.13F, -0.36F);
            float tailFlick = Mth.sin(age * 0.14F) * 6.0F + walkSpeed * 18.0F;
            pose.mulPose(Axis.XP.rotationDegrees(tailFlick));
            pose.mulPose(Axis.YP.rotationDegrees(Mth.cos(age * 0.21F) * 8.0F));
            STAG_TAIL.emit(pose, vc, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);
            pose.popPose();

            // 2. Articulated S-Curved Neck -> Head -> Peach Ears -> Blooming Flower Antlers
            pose.pushPose();
            pose.translate(0.0F, 1.12F, 0.28F);
            float neckNod = Mth.sin(walkPos * 0.6662F * 2.0F) * walkSpeed * 4.5F
                    + Mth.sin(age * 0.06F) * 1.4F;
            float lowerPitch = 14.0F + neckNod + graze * 56.0F + headPitch * 0.28F * (1.0F - graze);
            pose.mulPose(Axis.YP.rotationDegrees(-netHeadYaw * 0.32F * (1.0F - graze * 0.5F)));
            pose.mulPose(Axis.XP.rotationDegrees(lowerPitch));
            STAG_NECK_LOWER.emit(pose, vc, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);

            pose.translate(0.0F, 0.23F, 0.07F);
            float upperPitch = -10.0F + graze * 38.0F + nibble * 4.5F + headPitch * 0.32F * (1.0F - graze);
            pose.mulPose(Axis.YP.rotationDegrees(-netHeadYaw * 0.34F * (1.0F - graze * 0.5F)));
            pose.mulPose(Axis.XP.rotationDegrees(upperPitch));
            STAG_NECK_UPPER.emit(pose, vc, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);

            // Head
            pose.translate(0.0F, 0.24F, 0.025F);
            float finalHeadPitch = -4.0F - graze * 22.0F + nibble * 6.0F + headPitch * 0.40F * (1.0F - graze);
            pose.mulPose(Axis.YP.rotationDegrees(-netHeadYaw * 0.34F));
            pose.mulPose(Axis.XP.rotationDegrees(finalHeadPitch));
            STAG_HEAD.emit(pose, vc, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);

            // Independent graceful ear twitches
            float earTwitchL = (Mth.sin(age * 0.19F + entity.getId()) > 0.92F) ? Mth.sin(age * 1.4F) * 14.0F : 0.0F;
            float earTwitchR = (Mth.sin(age * 0.17F + entity.getId() * 3 + 2.1F) > 0.92F) ? Mth.sin(age * 1.5F) * 14.0F : 0.0F;

            pose.pushPose();
            pose.translate(0.044F, 0.052F, -0.018F);
            pose.mulPose(Axis.YP.rotationDegrees(-12.0F + earTwitchL));
            pose.mulPose(Axis.ZP.rotationDegrees(-6.0F));
            STAG_EAR_LEFT.emit(pose, vc, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);
            pose.popPose();

            pose.pushPose();
            pose.translate(-0.044F, 0.052F, -0.018F);
            pose.mulPose(Axis.YP.rotationDegrees(12.0F - earTwitchR));
            pose.mulPose(Axis.ZP.rotationDegrees(6.0F));
            STAG_EAR_RIGHT.emit(pose, vc, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);
            pose.popPose();

            // Branching Ivory Antlers + 3D Blooming Flowers (adult stags & hinds!)
            if (!entity.isBaby()) {
                int palIdx = (entity.getBloomVariant() == 2) ? 1 : 0;
                int tintR = 255;
                int tintG = (entity.getBloomVariant() == 1) ? 255 : 248;
                int tintB = (entity.getBloomVariant() == 1) ? 245 : 255;

                pose.pushPose();
                pose.translate(0.036F, 0.062F, -0.012F);
                STAG_ANTLER_LEFT.emit(pose, vc, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);
                STAG_BLOSSOMS_LEFT[palIdx].emit(pose, vc, packedLight, OverlayTexture.NO_OVERLAY, tintR, tintG, tintB, 255);
                pose.popPose();

                pose.pushPose();
                pose.translate(-0.036F, 0.062F, -0.012F);
                STAG_ANTLER_RIGHT.emit(pose, vc, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);
                STAG_BLOSSOMS_RIGHT[palIdx].emit(pose, vc, packedLight, OverlayTexture.NO_OVERLAY, tintR, tintG, tintB, 255);
                pose.popPose();
            }

            pose.popPose(); // end neck/head
            pose.popPose(); // end torso

            // 3. Four 3-Jointed Articulated Slender Deer Legs (diagonal 4-beat gait!)
            float w = walkPos * 0.6662F;
            renderFrontLeg(pose, vc, packedLight, 0.088F, 1.05F, 0.23F, w, walkSpeed);
            renderFrontLeg(pose, vc, packedLight, -0.088F, 1.05F, 0.23F, w + (float) Math.PI, walkSpeed);
            renderHindLeg(pose, vc, packedLight, 0.086F, 1.06F, -0.25F, w + (float) Math.PI * 0.85F, walkSpeed);
            renderHindLeg(pose, vc, packedLight, -0.086F, 1.06F, -0.25F, w + (float) Math.PI * 1.85F, walkSpeed);

            pose.popPose();
            super.render(entity, entityYaw, partialTick, pose, buf, packedLight);
        }

        private static void renderFrontLeg(PoseStack pose, VertexConsumer vc, int light,
                                           float x, float y, float z, float phase, float speed) {
            float swing = Mth.cos(phase) * 32.0F * speed;
            float lift = Math.max(0.0F, Mth.sin(phase)) * speed;
            // При выносе передней ноги вперёд запястье оленя грациозно сгибается назад (+XP)
            float kneeBend = lift * 42.0F;
            float fetlockBend = -lift * 24.0F - swing * 0.25F;

            pose.pushPose();
            pose.translate(x, y, z);
            pose.mulPose(Axis.XP.rotationDegrees(swing));
            STAG_FRONT_THIGH.emit(pose, vc, light, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);

            pose.translate(0.0F, -0.36F, -0.015F);
            pose.mulPose(Axis.XP.rotationDegrees(kneeBend));
            STAG_FRONT_SHIN.emit(pose, vc, light, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);

            pose.translate(0.0F, -0.36F, 0.008F);
            pose.mulPose(Axis.XP.rotationDegrees(fetlockBend));
            STAG_CANNON_HOOF.emit(pose, vc, light, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);
            pose.popPose();
        }

        private static void renderHindLeg(PoseStack pose, VertexConsumer vc, int light,
                                          float x, float y, float z, float phase, float speed) {
            float swing = Mth.cos(phase) * 30.0F * speed;
            float lift = Math.max(0.0F, Mth.sin(phase)) * speed;
            // Скакательный сустав задней ноги сгибается в противофазе для пружинящего толчка
            float hockBend = -lift * 38.0F;
            float fetlockBend = lift * 26.0F - swing * 0.22F;

            pose.pushPose();
            pose.translate(x, y, z);
            pose.mulPose(Axis.XP.rotationDegrees(swing));
            STAG_HIND_THIGH.emit(pose, vc, light, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);

            pose.translate(0.0F, -0.36F, -0.065F);
            pose.mulPose(Axis.XP.rotationDegrees(hockBend));
            STAG_HIND_SHIN.emit(pose, vc, light, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);

            pose.translate(0.0F, -0.35F, 0.045F);
            pose.mulPose(Axis.XP.rotationDegrees(fetlockBend));
            STAG_CANNON_HOOF.emit(pose, vc, light, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);
            pose.popPose();
        }
    }
}
