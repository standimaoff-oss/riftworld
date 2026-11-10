package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.client.geo.GeoModel;

/**
 * v0.34.0 — общий рендер «моб из JSON»: одна и та же геометрия {@link GeoModel}
 * Анимации костей считаются на клиенте, пакетов не тратит.
 */
public class GeoMobRenderer<T extends LivingEntity> extends EntityRenderer<T> {
    private final ResourceLocation texture;
    private final String model;
    private final float shadow;

    public GeoMobRenderer(EntityRendererProvider.Context ctx, String model, String texturePath, float shadow) {
        super(ctx);
        this.texture = new ResourceLocation(RiftWorld.MODID, texturePath);
        this.model = model;
        this.shadowRadius = shadow;
        this.shadow = shadow;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }

    @Override
    public void render(T e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
        GeoModel geo = GeoModel.of(model);
        if (geo == null) {
            super.render(e, yaw, partial, pose, buf, light);
            return;
        }
        float yRot = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
        float xRot = Mth.lerp(partial, e.xRotO, e.getXRot());
        float time = e.tickCount + partial;

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yRot));   // модель смотрит носом в -Z
        pose.mulPose(Axis.XP.rotationDegrees(xRot));
        float s = geo.renderScale;
        pose.scale(s, s, s);
        geo.animate(time);

        VertexConsumer vc = buf.getBuffer(geo.translucent
                ? RenderType.entityTranslucent(texture) : RenderType.entityCutoutNoCull(texture));
        geo.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(e, yaw, partial, pose, buf, light);
    }
}
