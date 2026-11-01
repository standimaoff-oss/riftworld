package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.entity.SpatialRiftEntity;

/**
 * Рендерер белого 3D-геометрического Пролома в Пространстве ({@link SpatialRiftEntity}):
 * <ul>
 *   <li>Никаких блоков и квадратных рамок — многогранная кристаллическая 3D-геометрия;</li>
 *   <li>Начинается у самой земли тонким белым геометрическим остриём (~0.35 м) и к небу
 *       непрерывно утолщается и расширяется до огромного гранёного белого разлома (~28 м в вышине);</li>
 *   <li>У основания на земле вращается изящный бело-золотистый геометрический узор печати Элисара.</li>
 * </ul>
 */
public class SpatialRiftRenderer extends EntityRenderer<SpatialRiftEntity> {
    public static final ResourceLocation RIFT_TEX =
            new ResourceLocation(RiftWorld.MODID, "textures/environment/spatial_rift.png");
    public static final ResourceLocation SEAL_TEX =
            new ResourceLocation(RiftWorld.MODID, "textures/environment/portal_open_seal.png");

    private static final int FULL = LightTexture.FULL_BRIGHT;
    private static final int OV = OverlayTexture.NO_OVERLAY;
    private static final int RINGS = 30;
    private static final int SIDES = 7; // нечётное число граней -> заведомо НЕ квадратное, живое геометрическое сечение!
    private static final float TOTAL_HEIGHT = 172.0F;

    public SpatialRiftRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(SpatialRiftEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        double dx = entity.getX() - camX;
        double dz = entity.getZ() - camZ;
        return dx * dx + dz * dz < 420.0 * 420.0;
    }

    @Override
    public ResourceLocation getTextureLocation(SpatialRiftEntity entity) {
        return RIFT_TEX;
    }

    @Override
    public void render(SpatialRiftEntity entity, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
        float age = entity.riftAge() + partial;
        float unfold = Mth.clamp(age / 14.0F, 0.05F, 1.0F);
        float unfoldEase = 1.0F - (1.0F - unfold) * (1.0F - unfold) * (1.0F - unfold);

        ps.pushPose();

        // 1) Напольный сакрально-геометрический узор вокруг устья белого пролома (2 встречно вращающихся кольца)
        VertexConsumer sealVc = buf.getBuffer(RenderType.entityTranslucent(SEAL_TEX));
        drawGroundSeal(ps, sealVc, 0.045F, 4.6F * unfoldEase, age * 0.018F, 252, 248, 238, (int) (235 * unfoldEase));
        drawGroundSeal(ps, sealVc, 0.062F, 2.55F * unfoldEase, -age * 0.031F, 255, 253, 247, (int) (248 * unfoldEase));

        // 2) Белый 3D-геометрический Пролом от земли до неба (от земли к небу всё толще и толще!)
        VertexConsumer riftVc = buf.getBuffer(RenderType.entityTranslucent(RIFT_TEX));
        Matrix4f pose = ps.last().pose();
        Matrix3f norm = ps.last().normal();

        float maxH = TOTAL_HEIGHT * (0.25F + 0.75F * unfoldEase);

        // Предвычисляем кольца 7-гранной ломаной призмы + внутреннего ослепительно-белого ядра
        float[][] vx = new float[RINGS + 1][SIDES];
        float[][] vy = new float[RINGS + 1][SIDES];
        float[][] vz = new float[RINGS + 1][SIDES];
        float[][] cx = new float[RINGS + 1][SIDES];
        float[][] cz = new float[RINGS + 1][SIDES];

        for (int i = 0; i <= RINGS; ++i) {
            float u = (float) i / (float) RINGS;
            // Нелинейная сетка по высоте: высокая детализация у земли (где заходит игрок) + плавный уход в небо
            float h = -0.10F + maxH * (0.14F * u + 0.86F * u * u);

            // ГЛАВНОЕ ТРЕБОВАНИЕ: от земли к небу толще и толще!
            // У самой земли (u=0): узкое остриё ~0.18 м; на росте игрока (~2 м): ~1.25 м; в небе (u=1): ~15.5 м радиус (~31 м размах!)
            float baseRad = (0.18F + 1.35F * (float) Math.pow(u, 0.52)
                    + 4.40F * (float) Math.pow(u, 1.45)
                    + 10.20F * (float) Math.pow(u, 2.45)) * unfoldEase;

            // Зигзагообразный излом центральной оси пролома (как трещина в самом пространстве!)
            float spineZigX = Mth.sin(u * 13.5F + 0.4F) * (0.12F + 2.6F * u * u)
                    + Mth.sin(u * 29.0F - age * 0.025F) * (0.05F + 0.65F * u);
            float spineZigZ = Mth.cos(u * 11.0F + 1.1F) * (0.10F + 2.1F * u * u);

            float twist = u * 2.35F + age * 0.008F;
            for (int s = 0; s < SIDES; ++s) {
                float ang = twist + (float) (s * Math.PI * 2.0 / SIDES);
                // Острые геометрические рёбра: чередование длинных и коротких граней кристалла (НЕ круг и НЕ квадрат!)
                float facetSharp = (s % 2 == 0 ? 1.24F : 0.74F)
                        + 0.16F * Mth.sin(i * 1.9F + s * 2.4F);
                // Сплюснутость трещины у земли (лезвие разлома) -> объёмный широкий купол разлома в небе
                float anisoX = 1.18F;
                float anisoZ = Mth.lerp(Mth.clamp(u * 1.8F, 0.0F, 1.0F), 0.58F, 1.04F);

                float rx = Mth.cos(ang) * baseRad * facetSharp * anisoX;
                float rz = Mth.sin(ang) * baseRad * facetSharp * anisoZ;
                vx[i][s] = spineZigX + rx;
                vy[i][s] = h;
                vz[i][s] = spineZigZ + rz;

                cx[i][s] = spineZigX + rx * 0.46F;
                cz[i][s] = spineZigZ + rz * 0.46F;
            }
        }

        // 2A) Внутренний чисто-белый светоносный стержень пролома
        float vScroll = -age * 0.012F;
        for (int i = 0; i < RINGS; ++i) {
            float u0 = (float) i / RINGS;
            float u1 = (float) (i + 1) / RINGS;
            int a0 = Mth.clamp(Math.round((1.0F - 0.16F * u0 * u0) * 252.0F), 0, 255);
            int a1 = Mth.clamp(Math.round((1.0F - 0.16F * u1 * u1) * 252.0F), 0, 255);
            for (int s = 0; s < SIDES; ++s) {
                int sn = (s + 1) % SIDES;
                emitQuad(riftVc, pose, norm,
                        cx[i][s], vy[i][s], cz[i][s], 0.35F, u0 * 6.0F + vScroll, 255, 255, 255, a0,
                        cx[i][sn], vy[i][sn], cz[i][sn], 0.65F, u0 * 6.0F + vScroll, 255, 255, 255, a0,
                        cx[i + 1][sn], vy[i + 1][sn], cz[i + 1][sn], 0.65F, u1 * 6.0F + vScroll, 255, 254, 250, a1,
                        cx[i + 1][s], vy[i + 1][s], cz[i + 1][s], 0.35F, u1 * 6.0F + vScroll, 255, 254, 250, a1);
            }
        }

        // 2B) Внешняя гранёная кристаллическая оболочка пролома (чистые белые и алебастрово-жемчужные грани)
        for (int i = 0; i < RINGS; ++i) {
            float u0 = (float) i / RINGS;
            float u1 = (float) (i + 1) / RINGS;
            int baseA0 = Mth.clamp(Math.round((0.92F - 0.22F * u0 * u0) * 244.0F), 0, 255);
            int baseA1 = Mth.clamp(Math.round((0.92F - 0.22F * u1 * u1) * 244.0F), 0, 255);
            for (int s = 0; s < SIDES; ++s) {
                int sn = (s + 1) % SIDES;
                // Лёгкий контраст соседних белых граней, чтобы 3D-геометрия чётко читалась на фоне неба!
                boolean brightFacet = (s + i) % 2 == 0;
                int cr = brightFacet ? 255 : 238;
                int cg = brightFacet ? 253 : 233;
                int cb = brightFacet ? 248 : 222;
                emitQuad(riftVc, pose, norm,
                        vx[i][s], vy[i][s], vz[i][s], 0.14F, u0 * 8.0F + vScroll * 0.6F, cr, cg, cb, baseA0,
                        vx[i][sn], vy[i][sn], vz[i][sn], 0.86F, u0 * 8.0F + vScroll * 0.6F, cr, cg, cb, baseA0,
                        vx[i + 1][sn], vy[i + 1][sn], vz[i + 1][sn], 0.86F, u1 * 8.0F + vScroll * 0.6F, cr, cg, cb, baseA1,
                        vx[i + 1][s], vy[i + 1][s], vz[i + 1][s], 0.14F, u1 * 8.0F + vScroll * 0.6F, cr, cg, cb, baseA1);
            }
        }

        // 2C) Острые лучевые геометрические «крылья-изломы» и парящие белые октаэдры вдоль ствола пролома
        for (int fin = 0; fin < 5; ++fin) {
            float finAng = (float) (fin * Math.PI * 2.0 / 5.0) + 0.35F;
            for (int i = 1; i < RINGS - 1; i += 2) {
                float u = (float) i / RINGS;
                float spikeOut = (0.35F + 5.8F * u * u) * unfoldEase;
                float ang = finAng + u * 1.9F + Mth.sin(age * 0.03F + i) * 0.08F;
                float bx = cx[i][fin % SIDES], by = vy[i][fin % SIDES], bz = cz[i][fin % SIDES];
                float tx = bx + Mth.cos(ang) * spikeOut;
                float tz = bz + Mth.sin(ang) * spikeOut;
                float ty = by + (0.55F + 2.8F * u);
                float byTop = vy[Math.min(RINGS, i + 2)][fin % SIDES];
                emitQuad(riftVc, pose, norm,
                        bx, by, bz, 0.5F, 0.2F, 255, 254, 250, 232,
                        tx, ty, tz, 0.15F, 0.5F, 244, 239, 228, 198,
                        tx, ty + 0.25F + 0.9F * u, tz, 0.15F, 0.8F, 244, 239, 228, 180,
                        cx[Math.min(RINGS, i + 2)][fin % SIDES], byTop, cz[Math.min(RINGS, i + 2)][fin % SIDES], 0.5F, 0.8F, 255, 254, 250, 225);
            }
        }

        // Парящие белые геометрические кристаллы-осколки пространства, всплывающие от земли к небу
        for (int k = 0; k < 18; ++k) {
            float prog = ((age * 0.009F + k * 0.0555F) % 1.0F);
            float shY = (0.6F + 64.0F * prog * prog) * unfoldEase;
            float orbitR = (0.65F + 6.5F * prog * prog) * unfoldEase;
            float oa = k * 2.399F + age * 0.028F;
            float sx = Mth.cos(oa) * orbitR;
            float sz = Mth.sin(oa) * orbitR;
            float rad = (0.14F + 0.58F * prog) * unfoldEase;
            drawWhiteOctahedron(riftVc, pose, norm, sx, shY, sz, rad, rad * 2.1F, age * 0.05F + k);
        }

        ps.popPose();
    }

    private static void drawGroundSeal(PoseStack ps, VertexConsumer vc, float y, float radius, float rot,
                                       int r, int g, int b, int a) {
        if (a <= 2 || radius <= 0.05F) return;
        ps.pushPose();
        ps.translate(0.0, y, 0.0);
        ps.mulPose(com.mojang.math.Axis.YP.rotation(rot));
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        vc.vertex(m, -radius, 0.0F, -radius).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OV).uv2(FULL).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, -radius, 0.0F, radius).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OV).uv2(FULL).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, radius, 0.0F, radius).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OV).uv2(FULL).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, radius, 0.0F, -radius).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OV).uv2(FULL).normal(n, 0, 1, 0).endVertex();
        ps.popPose();
    }

    private static void drawWhiteOctahedron(VertexConsumer vc, Matrix4f pose, Matrix3f norm,
                                            float cx, float cy, float cz, float r, float hh, float spin) {
        float[] px = new float[4];
        float[] pz = new float[4];
        for (int i = 0; i < 4; ++i) {
            float a = spin + i * Mth.HALF_PI;
            px[i] = cx + Mth.cos(a) * r;
            pz[i] = cz + Mth.sin(a) * r;
        }
        for (int i = 0; i < 4; ++i) {
            int in = (i + 1) & 3;
            int shade = (i % 2 == 0) ? 255 : 236;
            emitQuad(vc, pose, norm,
                    cx, cy + hh, cz, 0.5F, 0.2F, 255, 255, 255, 238,
                    px[i], cy, pz[i], 0.25F, 0.5F, shade, shade - 2, shade - 8, 224,
                    px[in], cy, pz[in], 0.75F, 0.5F, shade, shade - 2, shade - 8, 224,
                    cx, cy + hh, cz, 0.5F, 0.2F, 255, 255, 255, 238);
            emitQuad(vc, pose, norm,
                    cx, cy - hh, cz, 0.5F, 0.8F, 250, 247, 239, 230,
                    px[in], cy, pz[in], 0.75F, 0.5F, shade, shade - 2, shade - 8, 224,
                    px[i], cy, pz[i], 0.25F, 0.5F, shade, shade - 2, shade - 8, 224,
                    cx, cy - hh, cz, 0.5F, 0.8F, 250, 247, 239, 230);
        }
    }

    private static void emitQuad(VertexConsumer vc, Matrix4f pose, Matrix3f norm,
                                 float x0, float y0, float z0, float u0, float v0, int r0, int g0, int b0, int a0,
                                 float x1, float y1, float z1, float u1, float v1, int r1, int g1, int b1, int a1,
                                 float x2, float y2, float z2, float u2, float v2, int r2, int g2, int b2, int a2,
                                 float x3, float y3, float z3, float u3, float v3, int r3, int g3, int b3, int a3) {
        vc.vertex(pose, x0, y0, z0).color(r0, g0, b0, a0).uv(u0, v0).overlayCoords(OV).uv2(FULL).normal(norm, 0, 1, 0).endVertex();
        vc.vertex(pose, x1, y1, z1).color(r1, g1, b1, a1).uv(u1, v1).overlayCoords(OV).uv2(FULL).normal(norm, 0, 1, 0).endVertex();
        vc.vertex(pose, x2, y2, z2).color(r2, g2, b2, a2).uv(u2, v2).overlayCoords(OV).uv2(FULL).normal(norm, 0, 1, 0).endVertex();
        vc.vertex(pose, x3, y3, z3).color(r3, g3, b3, a3).uv(u3, v3).overlayCoords(OV).uv2(FULL).normal(norm, 0, 1, 0).endVertex();
    }
}
