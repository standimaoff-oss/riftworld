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
import ru.aiforge.riftworld.entity.WendigoEntity;

/**
 * v0.39.3 — рендер <b>Вендиго</b>: костяное тело из {@code geo/wendigo.json} + светящиеся
 * глазницы из {@code geo/wendigo_eyes.json}.
 *
 * <p>Свечение — ванильный {@link RenderType#eyes} с {@link LightTexture#FULL_BRIGHT}: болотный
 * огонь в глазницах лосиного черепа виден в полной темноте. Никаких собственных шейдеров
 * (совместимо с {@code ShaderCompat}), как и у Морока.
 */
public class WendigoRenderer extends EntityRenderer<WendigoEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(RiftWorld.MODID, "textures/entity/wendigo.png");
    private static final ResourceLocation EYES =
            new ResourceLocation(RiftWorld.MODID, "textures/entity/wendigo_eyes.png");

    public WendigoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.9F;
    }

    @Override
    public ResourceLocation getTextureLocation(WendigoEntity e) {
        return TEXTURE;
    }

    @Override
    public void render(WendigoEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
        GeoModel body = GeoModel.of("wendigo");
        GeoModel eyes = GeoModel.of("wendigo_eyes");
        if (body == null) {
            super.render(e, yaw, partial, pose, buf, light);
            return;
        }
        float yRot = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
        float xRot = Mth.lerp(partial, e.xRotO, e.getXRot());
        float time = e.tickCount + partial;

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yRot));   // модель смотрит носом в -Z
        pose.mulPose(Axis.XP.rotationDegrees(xRot));
        float s = body.renderScale;
        pose.scale(s, s, s);
        body.animate(time);
        body.render(pose, buf.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        if (eyes != null) {
            eyes.animate(time);
            eyes.render(pose, buf.getBuffer(RenderType.entityCutoutNoCull(EYES)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        }
        pose.popPose();
        super.render(e, yaw, partial, pose, buf, light);
    }
}
