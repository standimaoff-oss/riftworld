package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.client.geo.GeoModel;
import ru.aiforge.riftworld.entity.fauna.BoneRavenEntity;

/**
 * v0.50.0: рендерер костяного ворона.
 *
 * <p>Модель берётся из geo-файла {@code assets/riftworld/geo/bone_raven.json}
 * через {@link GeoModel} — тем же путём, что и Элисарская Маджа и бумажный
 * дракон. Раньше ворон рисовался java-моделью через общий
 * {@code LivingEntityRenderer}, и в кадрах витрины его не было: сущность
 * доходила до клиента, рендерер вызывался, но геометрия уходила из поля
 * зрения.
 *
 * <p>Geo-модели в Riftworld сделаны в Y-down конвенции, поэтому перед
 * рисованием ось Y отражается — так же, как в {@code ElGranMajaRenderer}.
 */
public final class BoneRavenRenderer extends EntityRenderer<BoneRavenEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(RiftWorld.MODID, "textures/entity/bone_raven.png");

    /** Дополнительный масштаб поверх render_scale из geo-файла. */
    private static final float EXTRA_SCALE = 1.0F;

    public BoneRavenRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.6F;
    }

    @Override
    public ResourceLocation getTextureLocation(BoneRavenEntity entity) {
        return TEXTURE;
    }

    @Override
    public boolean shouldRender(BoneRavenEntity entity, Frustum frustum, double x, double y, double z) {
        // Проверяем расстояние, а не бокс сущности: у ворона в витрине может
        // стоять noGravity, и тогда getBoundingBoxForCulling() отдаёт
        // AABB.ZERO, по которому фрустум не видит птицу вовсе.
        double maxDistance = 96.0D;
        return entity.distanceToSqr(x, y, z) <= maxDistance * maxDistance;
    }

    @Override
    public void render(BoneRavenEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (entity.isInvisible()) return;
        GeoModel model = GeoModel.of("bone_raven");
        if (model == null) return;

        float bodyYaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        float yawDelta = Mth.wrapDegrees(entity.getYRot() - entity.yRotO);

        poseStack.pushPose();
        // Гео-модель Riftworld использует Y-down координаты Minecraft.
        // Отражаем ось Y один раз, чтобы спина смотрела вверх.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch * 0.6F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.clamp(yawDelta * 1.4F, -16.0F, 16.0F)));
        float scale = model.renderScale * EXTRA_SCALE;
        poseStack.scale(scale, -scale, scale);

        // Анимация костей из geo-файла (махи крыльев и прочее).
        model.animate(entity.tickCount + partialTicks);

        int light = LightTexture.pack(Math.max(4, LightTexture.block(packedLight)),
                LightTexture.sky(packedLight));
        RenderType type = model.translucent
                ? RenderType.entityTranslucent(TEXTURE)
                : RenderType.entityCutoutNoCull(TEXTURE);
        model.render(poseStack, buffer.getBuffer(type), light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
