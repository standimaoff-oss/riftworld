package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.client.geo.GeoModel;
import ru.aiforge.riftworld.entity.HollowwatcherEntity;

/**
 * v0.49.0 — рендер <b>Смотрителя Пращи</b>: тело из {@code geo/hollowwatcher.json}
 * + <b>ледяные глаза</b>.
 *
 * <p>Как и у Морока, глаза вынесены в отдельную геометрию {@code geo/hollowwatcher_eyes.json}
 * и рисуются ванильным {@link RenderType#eyes} с {@link LightTexture#FULL_BRIGHT}: в полной
 * темноте ночного бора видны две бледно-голубые щели. Остальное тело — обычный cutout
 * со светом сцены, поэтому в тумане Смотритель читается как тёмный силуэт.
 */
public class HollowwatcherRenderer extends EntityRenderer<HollowwatcherEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(RiftWorld.MODID, "textures/entity/hollowwatcher.png");
    private static final ResourceLocation EYES =
            new ResourceLocation(RiftWorld.MODID, "textures/entity/hollowwatcher_eyes.png");

    public HollowwatcherRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.8F;
    }

    @Override
    public ResourceLocation getTextureLocation(HollowwatcherEntity e) {
        return TEXTURE;
    }

    @Override
    public void render(HollowwatcherEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
        GeoModel body = GeoModel.of("hollowwatcher");
        GeoModel eyes = GeoModel.of("hollowwatcher_eyes");
        if (body == null) {
            super.render(e, yaw, partial, pose, buf, light);
            return;
        }
        float yRot = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
        float xRot = Mth.lerp(partial, e.xRotO, e.getXRot());
        float time = e.tickCount + partial;

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yRot));
        pose.mulPose(Axis.XP.rotationDegrees(xRot));
        float s = body.renderScale;
        pose.scale(s, s, s);
        body.animate(time);

        // 1) тело: обычный cutout, освещение сцены
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        body.render(pose, vc, light, OverlayTexture.NO_OVERLAY);

        // 2) глаза: полная яркость + RenderType.eyes (аддитивное «свечение» в темноте)
        if (eyes != null) {
            eyes.animate(time);
            VertexConsumer glow = buf.getBuffer(RenderType.eyes(EYES));
            eyes.render(pose, glow, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        }
        pose.popPose();
        super.render(e, yaw, partial, pose, buf, light);
    }
}
