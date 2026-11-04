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
import ru.aiforge.riftworld.entity.MorokEntity;

/**
 * v0.39.0 — рендер <b>Морока</b>: тело из {@code geo/morok.json} + <b>горящие глаза</b>.
 *
 * <p>Глаза — отдельная геометрия {@code geo/morok_eyes.json}, которая рисуется поверх тела
 * ванильным {@link RenderType#eyes} и с {@link LightTexture#FULL_BRIGHT}: в полной темноте
 * видны два раскалённых уголька, а на свету они просто горят ярче тела. Это тот же приём,
 * что у ванильных паука и стражей, поэтому «свечение» не ломает шейдеры и не требует
 * собственного шейдера (совместимо с {@code ShaderCompat}).
 */
public class MorokRenderer extends EntityRenderer<MorokEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(RiftWorld.MODID, "textures/entity/morok.png");
    private static final ResourceLocation EYES =
            new ResourceLocation(RiftWorld.MODID, "textures/entity/morok_eyes.png");

    public MorokRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.7F;
    }

    @Override
    public ResourceLocation getTextureLocation(MorokEntity e) {
        return TEXTURE;
    }

    @Override
    public void render(MorokEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
        GeoModel body = GeoModel.of("morok");
        GeoModel eyes = GeoModel.of("morok_eyes");
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
            VertexConsumer glow = buf.getBuffer(RenderType.entityCutoutNoCull(EYES));
            eyes.render(pose, glow, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        }
        pose.popPose();
        super.render(e, yaw, partial, pose, buf, light);
    }
}
