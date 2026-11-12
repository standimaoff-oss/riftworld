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
import ru.aiforge.riftworld.client.model.fauna.BoneRavenModel;
import ru.aiforge.riftworld.entity.fauna.BoneRavenEntity;

/**
 * v0.50.0: собственный рендерер костяного ворона.
 *
 * <p>Раньше ворон рисовался через общий {@code BaseRenderer} (наследник
 * {@code LivingEntityRenderer}). В кадрах витрины птицы не было: сущность
 * доходила до клиента, рендерер вызывался, но геометрия уходила из поля
 * зрения. Поэтому ворон рисуется напрямую, как это уже сделано для
 * Элисарской Маджа ({@code ElGranMajaRenderer}): свой {@code EntityRenderer},
 * свой разворот по yaw и явный вызов {@code ModelPart.render}.
 *
 * <p>Модель сделана в Y-down конвенции Blockbench, поэтому перед рисованием
 * ось Y отражается. Смещение {@code root} (24 единицы = 1.5 блока вниз в
 * Blockbench) после отражения становится 1.5 блока вверх, поэтому дополнительно
 * опускаем точку отрисовки на 1.5 блока — тогда тело ворона оказывается на
 * 0.75 блока выше точки сущности, а лапы — на уровне сущности.
 */
public final class BoneRavenRenderer extends EntityRenderer<BoneRavenEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(RiftWorld.MODID, "textures/entity/bone_raven.png");
    private static final ResourceLocation GLOW_TEXTURE =
            new ResourceLocation(RiftWorld.MODID, "textures/entity/bone_raven_glow.png");

    /** Размер модели: 1.0 = ванильный блок на 16 модельных единиц. */
    private static final float MODEL_SCALE = 4.0F;

    /**
     * Вертикальное смещение точки отрисовки (в блоках, вверх положительно).
     *
     * <p>Модель в Y-down конвенции Blockbench, поэтому ось Y отражается и
     * модельные смещения меняют знак: root (+24 = 1.5 блока вниз) после
     * отражения даёт 1.5 блока вверх, а body (-15 = 0.94 вверх) даёт
     * 0.94 блока вниз. Итог: тело оказывается на 0.94 - 1.5 = -0.56 блока
     * относительно корня. Смещение +0.6 поднимает тело на 0.04 блока выше
     * точки сущности, а лапы (ещё на 0.37 ниже) — на -0.33.
     */
    private static final float VERTICAL_OFFSET = 0.60F;

    private final BoneRavenModel model;

    public BoneRavenRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new BoneRavenModel(ctx.bakeLayer(BoneRavenModel.LAYER_LOCATION));
        this.shadowRadius = 0.6F;
    }

    @Override
    public ResourceLocation getTextureLocation(BoneRavenEntity entity) {
        return TEXTURE;
    }

    @Override
    public boolean shouldRender(BoneRavenEntity entity, Frustum frustum, double x, double y, double z) {
        // ВНИМАНИЕ: у ворона в витрине выставлен setNoGravity(true), а
        // Entity#getBoundingBoxForCulling() при noGravity возвращает AABB.ZERO.
        // Фрустум по такому боксу ничего не видит — и рендерер вообще не
        // вызывался, птица не попадала в кадр. Поэтому проверяем
        // расстояние вручную, а не по боксу сущности.
        double maxDistance = 96.0D;
        return entity.distanceToSqr(x, y, z) <= maxDistance * maxDistance;
    }

    private static int renderCount = 0;

    @Override
    public void render(BoneRavenEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (entity.isInvisible()) return;
        renderCount++;
        if (renderCount % 60 == 1) {
            com.mojang.logging.LogUtils.getLogger().info(
                    "RAVEN RENDER count={} pos={},{},{} yaw={} noai={} grav={}",
                    renderCount, entity.getX(), entity.getY(), entity.getZ(),
                    entity.getYRot(), entity.isNoAi(), entity.isNoGravity());
        }

        // Поза: крылья машут, корпус слегка качается — как в living-рендере.
        this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTicks,
                Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()),
                Mth.lerp(partialTicks, entity.xRotO, entity.getXRot()));

        float bodyYaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        float yawDelta = Mth.wrapDegrees(entity.getYRot() - entity.yRotO);

        poseStack.pushPose();
        // Модель в Y-down: отражаем ось Y, как в ElGranMajaRenderer.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch * 0.6F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.clamp(yawDelta * 1.4F, -16.0F, 16.0F)));
        poseStack.scale(MODEL_SCALE, -MODEL_SCALE, MODEL_SCALE);
        // Вертикальное смещение модели (см. VERTICAL_OFFSET).
        poseStack.translate(0.0D, VERTICAL_OFFSET, 0.0D);

        int light = LightTexture.pack(Math.max(4, LightTexture.block(packedLight)),
                LightTexture.sky(packedLight));
        this.model.rootModel().render(poseStack,
                buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        // Свечение: рисуем ту же геометрию в режиме глаз (полная яркость).
        this.model.rootModel().render(poseStack,
                buffer.getBuffer(RenderType.eyes(GLOW_TEXTURE)), 15728640,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }
}
