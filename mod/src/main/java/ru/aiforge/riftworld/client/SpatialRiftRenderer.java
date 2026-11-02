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
 * v0.25.0: Рендерер единого монолитного белого 3D-геометрического Пролома в Пространстве ({@link SpatialRiftEntity}):
 * <ul>
 *   <li>Ровно <b>1 замкнутая водонепроницаемая 3D-геометрическая фигура</b> ({@code RenderType.entitySolid(WHITE_TEX)}):
 *       0 прозрачных щелей, 0 горизонтальных швов/разделений, 0 отдельных летающих кусков;</li>
 *   <li>Вырастает из одной острой вершины у самой земли и непрерывно расширяется гранёными кристаллическими
 *       рёбрами высоко в небо;</li>
 *   <li>Непрерывно и плавно движется: живое спиральное вращение граней, бегущая торсионная волна и плавный
 *       дыхательный изгиб оси на 60+ FPS.</li>
 * </ul>
 */
public class SpatialRiftRenderer extends EntityRenderer<SpatialRiftEntity> {
    public static final ResourceLocation RIFT_TEX =
            new ResourceLocation(RiftWorld.MODID, "textures/environment/spatial_rift.png");

    private static final int FULL = LightTexture.FULL_BRIGHT;
    private static final int OV = OverlayTexture.NO_OVERLAY;
    private static final int RINGS = 48;
    private static final int SIDES = 8; // 4 длинных острых хребта + 4 глубокие вогнутые грани единой звёздчатой призмы
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
        return ShaderCompat.WHITE_TEX;
    }

    @Override
    public void render(SpatialRiftEntity entity, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
        float age = entity.riftAge() + partial;
        float unfold = Mth.clamp(age / 16.0F, 0.06F, 1.0F);
        float unfoldEase = 1.0F - (1.0F - unfold) * (1.0F - unfold) * (1.0F - unfold);

        ps.pushPose();

        // ЕДИНАЯ НЕПРОЗРАЧНАЯ ЗАМКНУТАЯ 3D-МОДЕЛЬ (entitySolid + чистая белая текстура -> 0 просветов и 0 швов!)
        VertexConsumer vc = buf.getBuffer(RenderType.entitySolid(ShaderCompat.WHITE_TEX));
        Matrix4f pose = ps.last().pose();
        Matrix3f norm = ps.last().normal();

        float maxH = TOTAL_HEIGHT * (0.28F + 0.72F * unfoldEase);

        float[][] vx = new float[RINGS + 1][SIDES];
        float[][] vy = new float[RINGS + 1][SIDES];
        float[][] vz = new float[RINGS + 1][SIDES];

        for (int i = 0; i <= RINGS; ++i) {
            float u = (float) i / (float) RINGS;
            // Густая сетка у земли и на уровне глаз игрока -> плавный выход в вышину
            float h = -0.06F + maxH * (0.15F * u + 0.85F * u * u);

            // Единый плавный профиль расширения от 0 в нижней вершине до мощного пространственного разлома в небе
            float taperGround = Mth.sin(Mth.clamp(u * 6.5F, 0.0F, 1.0F) * (Mth.PI * 0.5F));
            float breath = 1.0F + 0.085F * Mth.sin(age * 0.095F - u * 7.2F);
            float baseRad = taperGround * (0.34F
                    + 1.32F * (float) Math.pow(u, 0.56)
                    + 4.35F * (float) Math.pow(u, 1.48)
                    + 9.80F * (float) Math.pow(u, 2.45)) * breath * unfoldEase;

            // Плавное живое волнообразное движение центральной оси единой фигуры
            float swayAmp = (0.06F + 2.35F * u * u) * unfoldEase;
            float spineX = Mth.sin(u * 7.4F - age * 0.062F) * swayAmp
                    + Mth.sin(u * 15.0F + age * 0.041F) * (0.04F + 0.42F * u);
            float spineZ = Mth.cos(u * 6.6F - age * 0.054F + 0.7F) * (swayAmp * 0.82F);

            // Непрерывное вращение и спиральное скручивание граней во времени
            float twist = u * 2.85F - age * 0.052F + 0.22F * Mth.sin(age * 0.038F + u * 4.5F);
            float anisoX = 1.16F + 0.08F * Mth.sin(age * 0.047F + u * 3.0F);
            float anisoZ = Mth.lerp(Mth.clamp(u * 1.9F, 0.0F, 1.0F), 0.68F, 1.05F);

            for (int s = 0; s < SIDES; ++s) {
                float ang = twist + (float) (s * Math.PI * 2.0 / SIDES);
                // Чёткая скульптурная звёздчато-кристаллическая геометрия: острые хребты и вогнутые грани БЕЗ разрывов!
                boolean ridge = (s & 1) == 0;
                float pulse = 0.075F * Mth.sin(age * 0.11F - u * 8.5F + s * 1.5707963F);
                float facetR = (ridge ? 1.28F : 0.72F) + pulse;

                vx[i][s] = spineX + Mth.cos(ang) * baseRad * facetR * anisoX;
                vy[i][s] = h;
                vz[i][s] = spineZ + Mth.sin(ang) * baseRad * facetR * anisoZ;
            }
        }

        // 1) Нижняя общая вершина-остриё (абсолютно герметичное схождение всех 8 граней в 1 точку у печати!)
        float botX = 0.0F, botY = -0.12F, botZ = 0.0F;
        for (int s = 0; s < SIDES; ++s) {
            int sn = (s + 1) % SIDES;
            int[] rgb = shadeFacet(
                    botX, botY, botZ,
                    vx[0][sn], vy[0][sn], vz[0][sn],
                    vx[0][s], vy[0][s], vz[0][s]);
            emitSolidQuad(vc, pose, norm,
                    botX, botY, botZ,
                    vx[0][sn], vy[0][sn], vz[0][sn],
                    vx[0][s], vy[0][s], vz[0][s],
                    botX, botY, botZ,
                    rgb[0], rgb[1], rgb[2]);
        }

        // 2) Основное бесшовное тело единой 3D-фигуры (соседние грани делят точные координаты вершин -> 0 щелей!)
        for (int i = 0; i < RINGS; ++i) {
            for (int s = 0; s < SIDES; ++s) {
                int sn = (s + 1) % SIDES;
                // Двусторонний обход или внешний обход замкнутого многогранника с чистым геометрическим шейдингом граней
                int[] rgb = shadeFacet(
                        vx[i][s], vy[i][s], vz[i][s],
                        vx[i][sn], vy[i][sn], vz[i][sn],
                        vx[i + 1][sn], vy[i + 1][sn], vz[i + 1][sn]);
                // Наружная грань (против часовой стрелки снаружи)
                emitSolidQuad(vc, pose, norm,
                        vx[i][s], vy[i][s], vz[i][s],
                        vx[i][sn], vy[i][sn], vz[i][sn],
                        vx[i + 1][sn], vy[i + 1][sn], vz[i + 1][sn],
                        vx[i + 1][s], vy[i + 1][s], vz[i + 1][s],
                        rgb[0], rgb[1], rgb[2]);
                // Обратная грань (на случай взгляда прямо изнутри при входе игрока -> 100% отсутствие просветов!)
                emitSolidQuad(vc, pose, norm,
                        vx[i + 1][s], vy[i + 1][s], vz[i + 1][s],
                        vx[i + 1][sn], vy[i + 1][sn], vz[i + 1][sn],
                        vx[i][sn], vy[i][sn], vz[i][sn],
                        vx[i][s], vy[i][s], vz[i][s],
                        255, 254, 250);
            }
        }

        // 3) Верхняя замыкающая вершина купола разлома в небе (полностью замкнутый многогранник!)
        float topX = 0.0F, topZ = 0.0F;
        for (int s = 0; s < SIDES; ++s) {
            topX += vx[RINGS][s];
            topZ += vz[RINGS][s];
        }
        topX /= SIDES;
        topZ /= SIDES;
        float topY = vy[RINGS][0] + 6.0F * unfoldEase;
        for (int s = 0; s < SIDES; ++s) {
            int sn = (s + 1) % SIDES;
            emitSolidQuad(vc, pose, norm,
                    vx[RINGS][s], vy[RINGS][s], vz[RINGS][s],
                    vx[RINGS][sn], vy[RINGS][sn], vz[RINGS][sn],
                    topX, topY, topZ,
                    topX, topY, topZ,
                    252, 250, 244);
        }

        ps.popPose();
    }

    /**
     * Мягкий скульптурный оттенок грани по её 3D-нормали (от фарфорово-белого #F2EFE7 в тени до #FFFFFF на свету),
     * чтобы грани единой белой фигуры чётко читались в объёме без единого шва или полосы!
     */
    private static int[] shadeFacet(float x0, float y0, float z0,
                                     float x1, float y1, float z1,
                                     float x2, float y2, float z2) {
        float ux = x1 - x0, uy = y1 - y0, uz = z1 - z0;
        float wx = x2 - x0, wy = y2 - y0, wz = z2 - z0;
        float nx = uy * wz - uz * wy;
        float ny = uz * wx - ux * wz;
        float nz = ux * wy - uy * wx;
        float inv = (float) Mth.fastInvSqrt(Math.max(1.0e-6F, nx * nx + ny * ny + nz * nz));
        nx *= inv;
        ny *= inv;
        nz *= inv;

        float ndotl = Mth.clamp(nx * 0.52F + ny * 0.28F + nz * 0.81F, -1.0F, 1.0F);
        float k = 0.5F + 0.5F * ndotl; // 0..1
        int r = Mth.clamp(Math.round(Mth.lerp(k, 238.0F, 255.0F)), 0, 255);
        int g = Mth.clamp(Math.round(Mth.lerp(k, 235.0F, 255.0F)), 0, 255);
        int b = Mth.clamp(Math.round(Mth.lerp(k, 226.0F, 252.0F)), 0, 255);
        return new int[]{r, g, b};
    }

    private static void emitSolidQuad(VertexConsumer vc, Matrix4f pose, Matrix3f norm,
                                      float x0, float y0, float z0,
                                      float x1, float y1, float z1,
                                      float x2, float y2, float z2,
                                      float x3, float y3, float z3,
                                      int r, int g, int b) {
        // Фиксированная верхняя нормаль (0,1,0) для шейдера сущности убирает жёсткое ванильное затемнение тыльной стороны
        vc.vertex(pose, x0, y0, z0).color(r, g, b, 255).uv(0.5F, 0.5F).overlayCoords(OV).uv2(FULL).normal(norm, 0, 1, 0).endVertex();
        vc.vertex(pose, x1, y1, z1).color(r, g, b, 255).uv(0.5F, 0.5F).overlayCoords(OV).uv2(FULL).normal(norm, 0, 1, 0).endVertex();
        vc.vertex(pose, x2, y2, z2).color(r, g, b, 255).uv(0.5F, 0.5F).overlayCoords(OV).uv2(FULL).normal(norm, 0, 1, 0).endVertex();
        vc.vertex(pose, x3, y3, z3).color(r, g, b, 255).uv(0.5F, 0.5F).overlayCoords(OV).uv2(FULL).normal(norm, 0, 1, 0).endVertex();
    }
}
