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
         * Строгий ортогональный кубоид в классическом стиле Minecraft (6 прямоугольных граней с чёткими прямыми углами).
         */
        void box(float x0, float y0, float z0, float x1, float y1, float z1,
                 float u0, float v0, float u1, float v1) {
            // Front (+Z)
            quad(x0, y0, z1, u0, v1,  x1, y0, z1, u1, v1,  x1, y1, z1, u1, v0,  x0, y1, z1, u0, v0);
            // Back (-Z)
            quad(x1, y0, z0, u0, v1,  x0, y0, z0, u1, v1,  x0, y1, z0, u1, v0,  x1, y1, z0, u0, v0);
            // Left (+X)
            quad(x1, y0, z1, u0, v1,  x1, y0, z0, u1, v1,  x1, y1, z0, u1, v0,  x1, y1, z1, u0, v0);
            // Right (-X)
            quad(x0, y0, z0, u0, v1,  x0, y0, z1, u1, v1,  x0, y1, z1, u1, v0,  x0, y1, z0, u0, v0);
            // Top (+Y)
            quad(x0, y1, z1, u0, v1,  x1, y1, z1, u1, v1,  x1, y1, z0, u1, v0,  x0, y1, z0, u0, v0);
            // Bottom (-Y)
            quad(x0, y0, z0, u0, v1,  x1, y0, z0, u1, v1,  x1, y0, z1, u1, v0,  x0, y0, z1, u0, v0);
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
    // 2. SNOW-WHITE BLOSSOM STAG (v0.28.0: CLASSIC BLOCKY MINECRAFT CUBOID STYLE!)
    // =========================================================================
    private static final float PX = 1.0F / 16.0F; // 1 ванильный пиксель Minecraft = 0.0625 м

    private static final BakedMesh STAG_TORSO;
    private static final BakedMesh STAG_TAIL;
    private static final BakedMesh STAG_NECK;
    private static final BakedMesh STAG_HEAD;
    private static final BakedMesh STAG_EAR_LEFT;
    private static final BakedMesh STAG_EAR_RIGHT;
    private static final BakedMesh STAG_FRONT_LEG;
    private static final BakedMesh STAG_HIND_LEG;
    private static final BakedMesh STAG_ANTLER_LEFT;
    private static final BakedMesh STAG_ANTLER_RIGHT;
    private static final BakedMesh[] STAG_BLOSSOMS_LEFT = new BakedMesh[2];
    private static final BakedMesh[] STAG_BLOSSOMS_RIGHT = new BakedMesh[2];

    static {
        // UV layout in blossom_stag.png (64x64):
        //   White coat:       u=0.04..0.46, v=0.04..0.46
        //   Peach inner ear:  u=0.53..0.72, v=0.04..0.21
        //   Ivory antler/hoof:u=0.77..0.97, v=0.03..0.22
        //   Green calyx:      u=0.54..0.94, v=0.28..0.46
        //   Sakura/white flw: u=0.04..0.46, v=0.54..0.96
        //   Lilac-pearl flw:  u=0.53..0.72, v=0.53..0.72
        //   Dark pixel eye:   u=0.77..0.97, v=0.77..0.97

        // 2A) Квадратное туловище оленя в классическом стиле Minecraft (7x7x14 px + грудинка 5x1.5x5 px)
        MeshBuilder torso = new MeshBuilder();
        torso.box(-3.5F * PX, 12.0F * PX, -7.0F * PX, 3.5F * PX, 19.0F * PX, 7.0F * PX,
                0.04F, 0.04F, 0.46F, 0.46F);
        torso.box(-2.6F * PX, 10.8F * PX, 2.0F * PX, 2.6F * PX, 12.2F * PX, 7.2F * PX,
                0.06F, 0.06F, 0.44F, 0.44F);
        STAG_TORSO = torso.bake();

        // 2B) Прямоугольный пушистый хвостик (2.4 x 3.2 x 4.2 px)
        MeshBuilder tail = new MeshBuilder();
        tail.box(-1.2F * PX, -1.6F * PX, -4.2F * PX, 1.2F * PX, 1.6F * PX, 0.2F * PX,
                0.06F, 0.06F, 0.44F, 0.44F);
        STAG_TAIL = tail.bake();

        // 2C) Строгая прямоугольная шея в стиле ванильных лошадей/коз (4.0 x 8.5 x 4.4 px)
        MeshBuilder neck = new MeshBuilder();
        neck.box(-2.0F * PX, -0.8F * PX, -2.2F * PX, 2.0F * PX, 7.7F * PX, 2.2F * PX,
                0.05F, 0.05F, 0.45F, 0.45F);
        STAG_NECK = neck.bake();

        // 2D) Квадратная голова оленя (5x5x5.6 px) + мордочка (3.2x3.0x3.8 px) + розовый пиксельный носик и квадратные глазки!
        MeshBuilder head = new MeshBuilder();
        // Черепная коробка
        head.box(-2.5F * PX, -1.0F * PX, -2.4F * PX, 2.5F * PX, 4.0F * PX, 3.2F * PX,
                0.05F, 0.05F, 0.45F, 0.45F);
        // Вытянутая квадратная мордочка
        head.box(-1.6F * PX, -0.9F * PX, 3.2F * PX, 1.6F * PX, 2.1F * PX, 6.8F * PX,
                0.08F, 0.08F, 0.42F, 0.42F);
        // Нежно-персиковый пиксельный носик на кончике мордочки
        head.box(-1.0F * PX, 0.5F * PX, 6.45F * PX, 1.0F * PX, 2.15F * PX, 7.05F * PX,
                0.54F, 0.05F, 0.71F, 0.20F);
        // Выразительные квадратные пиксельные глаза по бокам головы + белый пиксельный блик
        for (int side : new int[]{-1, 1}) {
            float ex0 = side > 0 ? 2.42F * PX : -2.56F * PX;
            float ex1 = side > 0 ? 2.56F * PX : -2.42F * PX;
            head.box(ex0, 0.8F * PX, 0.4F * PX, ex1, 2.4F * PX, 2.2F * PX,
                    0.80F, 0.80F, 0.94F, 0.94F);
            // Маленький белый блик в верхне-переднем уголке глаза
            float gx0 = side > 0 ? 2.44F * PX : -2.59F * PX;
            float gx1 = side > 0 ? 2.59F * PX : -2.44F * PX;
            head.box(gx0, 1.7F * PX, 1.5F * PX, gx1, 2.35F * PX, 2.15F * PX,
                    0.10F, 0.10F, 0.25F, 0.25F);
        }
        STAG_HEAD = head.bake();

        // 2E) Прямоугольные ушки в стиле Minecraft с персиковой внутренней стороной
        STAG_EAR_LEFT = buildBlockyDeerEar(1.0F);
        STAG_EAR_RIGHT = buildBlockyDeerEar(-1.0F);

        // 2F) Четыре прямые квадратные ноги в классическом стиле Minecraft (высота ровно 12 px = 0.75 м до земли!)
        MeshBuilder fLeg = new MeshBuilder();
        // Белая верхняя часть ноги (y = 0 .. -10 px)
        fLeg.box(-1.2F * PX, -10.0F * PX, -1.2F * PX, 1.2F * PX, 0.4F * PX, 1.2F * PX,
                0.06F, 0.06F, 0.44F, 0.44F);
        // Квадратное костяное копытце снизу (y = -10 .. -12 px)
        fLeg.box(-1.25F * PX, -12.0F * PX, -1.25F * PX, 1.25F * PX, -10.0F * PX, 1.25F * PX,
                0.78F, 0.04F, 0.96F, 0.21F);
        STAG_FRONT_LEG = fLeg.bake();

        MeshBuilder hLeg = new MeshBuilder();
        // Квадратное бедро (чуть шире сверху, как у ванильных копытных, y = +0.5 .. -5.0 px)
        hLeg.box(-1.35F * PX, -5.0F * PX, -1.6F * PX, 1.35F * PX, 0.6F * PX, 1.5F * PX,
                0.06F, 0.06F, 0.44F, 0.44F);
        // Голень (y = -5.0 .. -10.0 px)
        hLeg.box(-1.2F * PX, -10.0F * PX, -1.2F * PX, 1.2F * PX, -5.0F * PX, 1.2F * PX,
                0.06F, 0.06F, 0.44F, 0.44F);
        // Квадратное костяное копытце (y = -10.0 .. -12.0 px)
        hLeg.box(-1.25F * PX, -12.0F * PX, -1.25F * PX, 1.25F * PX, -10.0F * PX, 1.25F * PX,
                0.78F, 0.04F, 0.96F, 0.21F);
        STAG_HIND_LEG = hLeg.bake();

        // 2G) Ступенчатые воксельные ветвистые рога (100% из прямоугольных брусков!) + пиксельные цветы
        STAG_ANTLER_LEFT = buildBlockyAntlerBranches(1.0F);
        STAG_ANTLER_RIGHT = buildBlockyAntlerBranches(-1.0F);
        for (int pal = 0; pal < 2; ++pal) {
            STAG_BLOSSOMS_LEFT[pal] = buildBlockyAntlerBlossoms(1.0F, pal == 1);
            STAG_BLOSSOMS_RIGHT[pal] = buildBlockyAntlerBlossoms(-1.0F, pal == 1);
        }
    }

    private static BakedMesh buildBlockyDeerEar(float side) {
        MeshBuilder m = new MeshBuilder();
        float xIn = side > 0 ? 0.0F : -3.4F * PX;
        float xOut = side > 0 ? 3.4F * PX : 0.0F;
        // Основная белая пластинка уха (3.4 x 4.2 x 0.9 px)
        m.box(xIn, 0.0F, -0.5F * PX, xOut, 4.2F * PX, 0.4F * PX,
                0.06F, 0.06F, 0.44F, 0.44F);
        // Персиковая внутренняя вставка на передней грани уха (+Z)
        float pxIn = side > 0 ? 0.55F * PX : -2.85F * PX;
        float pxOut = side > 0 ? 2.85F * PX : -0.55F * PX;
        m.box(pxIn, 0.55F * PX, 0.32F * PX, pxOut, 3.55F * PX, 0.48F * PX,
                0.54F, 0.05F, 0.71F, 0.20F);
        return m.bake();
    }

    private static BakedMesh buildBlockyAntlerBranches(float side) {
        MeshBuilder m = new MeshBuilder();
        float u0 = 0.78F, v0 = 0.04F, u1 = 0.96F, v1 = 0.21F;
        // Ступенчатые воксельные бруски рога (сечение ~0.9..1.1 px, строгая ортогональная сетка!)
        // 1. Основание рога у темени
        addSymBox(m, side, 0.0F, 0.0F, -0.5F, 1.1F, 2.6F, 0.6F, u0, v0, u1, v1);
        // 2. Вторая ступень вбок-назад-вверх
        addSymBox(m, side, 0.8F, 2.0F, -1.3F, 1.9F, 5.2F, -0.2F, u0, v0, u1, v1);
        // 3. Передний надглазничный отросток (brow tine) вперёд-вверх
        addSymBox(m, side, 1.0F, 2.8F, -0.3F, 1.9F, 3.8F, 1.6F, u0, v0, u1, v1);
        addSymBox(m, side, 1.0F, 3.4F, 1.0F, 1.85F, 5.0F, 1.9F, u0, v0, u1, v1);
        // 4. Третья ступень главного ствола
        addSymBox(m, side, 1.6F, 4.6F, -2.0F, 2.65F, 7.6F, -0.95F, u0, v0, u1, v1);
        // 5. Внутренний королевский отросток вверх
        addSymBox(m, side, 1.1F, 6.2F, -1.2F, 1.95F, 8.8F, -0.35F, u0, v0, u1, v1);
        // 6. Внешняя развилка короны
        addSymBox(m, side, 2.4F, 6.8F, -2.3F, 3.4F, 9.2F, -1.35F, u0, v0, u1, v1);
        addSymBox(m, side, 3.1F, 7.8F, -2.2F, 4.0F, 9.8F, -1.35F, u0, v0, u1, v1);
        return m.bake();
    }

    private static void addSymBox(MeshBuilder m, float side,
                                  float px0, float py0, float pz0,
                                  float px1, float py1, float pz1,
                                  float u0, float v0, float u1, float v1) {
        float x0 = (side > 0 ? px0 : -px1) * PX;
        float x1 = (side > 0 ? px1 : -px0) * PX;
        m.box(x0, py0 * PX, pz0 * PX, x1, py1 * PX, pz1 * PX, u0, v0, u1, v1);
    }

    private static BakedMesh buildBlockyAntlerBlossoms(float side, boolean lilacPalette) {
        MeshBuilder m = new MeshBuilder();
        float u0 = lilacPalette ? 0.53F : 0.04F;
        float v0 = 0.54F;
        float u1 = lilacPalette ? 0.72F : 0.46F;
        float v1 = lilacPalette ? 0.72F : 0.96F;

        // 5 воксельно-крестовых ванильных цветочков с маленькими квадратными бутонами и листочками на ветвях рогов
        float[][] spots = {
                {side * 1.45F * PX, 5.1F * PX, 1.45F * PX, 1.15F * PX},
                {side * 2.15F * PX, 5.4F * PX, -1.45F * PX, 1.25F * PX},
                {side * 1.50F * PX, 8.9F * PX, -0.75F * PX, 1.20F * PX},
                {side * 2.90F * PX, 9.2F * PX, -1.80F * PX, 1.30F * PX},
                {side * 3.65F * PX, 9.9F * PX, -1.75F * PX, 1.10F * PX}
        };
        for (float[] sp : spots) {
            float cx = sp[0], cy = sp[1], cz = sp[2], r = sp[3];
            // Плоская зелёная чашечка-листик под цветком (тонкий воксельный квадратик)
            m.box(cx - r * 0.75F, cy - 0.35F * PX, cz - r * 0.75F,
                    cx + r * 0.75F, cy - 0.05F * PX, cz + r * 0.75F,
                    0.56F, 0.30F, 0.92F, 0.45F);
            // Воксельная сердцевина цветка (кубик в центре)
            m.box(cx - r * 0.48F, cy - 0.05F * PX, cz - r * 0.48F,
                    cx + r * 0.48F, cy + r * 0.78F, cz + r * 0.48F,
                    u0, v0, u1, v1);
            // Классический ванильный X-крестик лепестков (как цветы в Minecraft!)
            m.doubleSidedQuad(
                    cx - r, cy - 0.05F * PX, cz - r, u0, v1,
                    cx + r, cy - 0.05F * PX, cz + r, u1, v1,
                    cx + r, cy + r * 1.25F, cz + r, u1, v0,
                    cx - r, cy + r * 1.25F, cz - r, u0, v0);
            m.doubleSidedQuad(
                    cx - r, cy - 0.05F * PX, cz + r, u0, v1,
                    cx + r, cy - 0.05F * PX, cz - r, u1, v1,
                    cx + r, cy + r * 1.25F, cz - r, u1, v0,
                    cx - r, cy + r * 1.25F, cz + r, u0, v0);
        }
        return m.bake();
    }

    public static final class BlossomStag extends EntityRenderer<BlossomStagEntity> {
        public BlossomStag(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.50F;
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

            float strideBob = Math.abs(Mth.sin(walkPos * 0.6662F)) * walkSpeed * 0.025F;
            pose.translate(0.0F, strideBob, 0.0F);
            pose.mulPose(Axis.YP.rotationDegrees(-bodyYaw));

            VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(BLOSSOM_STAG_TEX));

            // 1. Квадратный корпус и хвостик
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(graze * 3.0F));
            STAG_TORSO.emit(pose, vc, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);

            // Tail flick
            pose.pushPose();
            pose.translate(0.0F, 18.0F * PX, -6.9F * PX);
            float tailFlick = 18.0F + Mth.sin(age * 0.14F) * 5.0F + walkSpeed * 16.0F;
            pose.mulPose(Axis.XP.rotationDegrees(tailFlick));
            pose.mulPose(Axis.YP.rotationDegrees(Mth.cos(age * 0.21F) * 7.0F));
            STAG_TAIL.emit(pose, vc, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);
            pose.popPose();

            // 2. Прямоугольная шея -> квадратная голова -> прямоугольные ушки -> воксельные рога с цветами
            pose.pushPose();
            pose.translate(0.0F, 17.6F * PX, 5.2F * PX);
            float neckNod = Mth.sin(walkPos * 0.6662F * 2.0F) * walkSpeed * 3.5F
                    + Mth.sin(age * 0.06F) * 1.2F;
            float neckPitch = 18.0F + neckNod + graze * 66.0F + headPitch * 0.35F * (1.0F - graze);
            pose.mulPose(Axis.YP.rotationDegrees(-netHeadYaw * 0.45F * (1.0F - graze * 0.5F)));
            pose.mulPose(Axis.XP.rotationDegrees(neckPitch));
            STAG_NECK.emit(pose, vc, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);

            // Head at top of neck
            pose.translate(0.0F, 7.2F * PX, 0.2F * PX);
            float finalHeadPitch = -16.0F - graze * 24.0F + nibble * 6.0F + headPitch * 0.55F * (1.0F - graze);
            pose.mulPose(Axis.YP.rotationDegrees(-netHeadYaw * 0.55F));
            pose.mulPose(Axis.XP.rotationDegrees(finalHeadPitch));
            STAG_HEAD.emit(pose, vc, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);

            // Independent blocky ear twitches
            float earTwitchL = (Mth.sin(age * 0.19F + entity.getId()) > 0.92F) ? Mth.sin(age * 1.4F) * 14.0F : 0.0F;
            float earTwitchR = (Mth.sin(age * 0.17F + entity.getId() * 3 + 2.1F) > 0.92F) ? Mth.sin(age * 1.5F) * 14.0F : 0.0F;

            pose.pushPose();
            pose.translate(2.1F * PX, 3.3F * PX, -0.8F * PX);
            pose.mulPose(Axis.YP.rotationDegrees(-14.0F + earTwitchL));
            pose.mulPose(Axis.ZP.rotationDegrees(-22.0F));
            STAG_EAR_LEFT.emit(pose, vc, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);
            pose.popPose();

            pose.pushPose();
            pose.translate(-2.1F * PX, 3.3F * PX, -0.8F * PX);
            pose.mulPose(Axis.YP.rotationDegrees(14.0F - earTwitchR));
            pose.mulPose(Axis.ZP.rotationDegrees(22.0F));
            STAG_EAR_RIGHT.emit(pose, vc, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);
            pose.popPose();

            // Voxel Ivory Antlers + Pixel Blossoms (adult stags & hinds!)
            if (!entity.isBaby()) {
                int palIdx = (entity.getBloomVariant() == 2) ? 1 : 0;
                int tintR = 255;
                int tintG = (entity.getBloomVariant() == 1) ? 255 : 248;
                int tintB = (entity.getBloomVariant() == 1) ? 245 : 255;

                pose.pushPose();
                pose.translate(1.2F * PX, 3.8F * PX, -0.4F * PX);
                STAG_ANTLER_LEFT.emit(pose, vc, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);
                STAG_BLOSSOMS_LEFT[palIdx].emit(pose, vc, packedLight, OverlayTexture.NO_OVERLAY, tintR, tintG, tintB, 255);
                pose.popPose();

                pose.pushPose();
                pose.translate(-1.2F * PX, 3.8F * PX, -0.4F * PX);
                STAG_ANTLER_RIGHT.emit(pose, vc, packedLight, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);
                STAG_BLOSSOMS_RIGHT[palIdx].emit(pose, vc, packedLight, OverlayTexture.NO_OVERLAY, tintR, tintG, tintB, 255);
                pose.popPose();
            }

            pose.popPose(); // end neck/head
            pose.popPose(); // end torso

            // 3. Четыре классические прямые квадратные ванильные ноги (точно как у коней/коз/коров в Minecraft!)
            float w = walkPos * 0.6662F;
            renderStraightLeg(pose, vc, packedLight, STAG_FRONT_LEG,  2.2F * PX, 12.0F * PX,  5.0F * PX, Mth.cos(w) * 38.0F * walkSpeed);
            renderStraightLeg(pose, vc, packedLight, STAG_FRONT_LEG, -2.2F * PX, 12.0F * PX,  5.0F * PX, Mth.cos(w + (float) Math.PI) * 38.0F * walkSpeed);
            renderStraightLeg(pose, vc, packedLight, STAG_HIND_LEG,   2.2F * PX, 12.0F * PX, -5.0F * PX, Mth.cos(w + (float) Math.PI) * 36.0F * walkSpeed);
            renderStraightLeg(pose, vc, packedLight, STAG_HIND_LEG,  -2.2F * PX, 12.0F * PX, -5.0F * PX, Mth.cos(w) * 36.0F * walkSpeed);

            pose.popPose();
            super.render(entity, entityYaw, partialTick, pose, buf, packedLight);
        }

        private static void renderStraightLeg(PoseStack pose, VertexConsumer vc, int light, BakedMesh mesh,
                                              float x, float y, float z, float swingDeg) {
            pose.pushPose();
            pose.translate(x, y, z);
            pose.mulPose(Axis.XP.rotationDegrees(swingDeg));
            mesh.emit(pose, vc, light, OverlayTexture.NO_OVERLAY, 255, 255, 255, 255);
            pose.popPose();
        }
    }
}
