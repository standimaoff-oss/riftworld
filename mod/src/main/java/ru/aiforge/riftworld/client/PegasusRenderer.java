package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import ru.aiforge.riftworld.client.geo.GeoModel;
import ru.aiforge.riftworld.entity.PegasusEntity;

/** Dedicated, reference-guided winged-horse rig: saddle, gait and flying wings. */
public final class PegasusRenderer extends EntityRenderer<PegasusEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("riftworld", "textures/entity/pegasus.png");

    public PegasusRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 1.1F;
    }

    @Override
    public ResourceLocation getTextureLocation(PegasusEntity entity) { return TEXTURE; }

    @Override
    public boolean shouldRender(PegasusEntity e, Frustum frustum, double x, double y, double z) {
        return e.shouldRender(x, y, z) && (e.noCulling || frustum.isVisible(e.getBoundingBoxForCulling().inflate(3.5, 3.0, 3.5)));
    }

    @Override
    public void render(PegasusEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        GeoModel model = GeoModel.of("pegasus");
        if (model == null) { super.render(e, yaw, partial, pose, buffers, light); return; }
        float time = e.tickCount + partial;
        float bodyYaw = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
        float pitch = Mth.lerp(partial, e.xRotO, e.getXRot());
        model.animate(time);
        ModelPart body = model.root.getChild("body");
        body.getChild("saddle").visible = e.hasSaddle();
        float gait = Mth.cos(e.walkAnimation.position(partial) * 0.6662F)
                * Math.min(1.0F, e.walkAnimation.speed(partial) * 1.4F) * 0.55F;
        body.getChild("leg_front_l").xRot = gait;
        body.getChild("leg_rear_r").xRot = gait;
        body.getChild("leg_front_r").xRot = -gait;
        body.getChild("leg_rear_l").xRot = -gait;
        // Geometry is authored open like the reference. Wings beat while flying;
        // at rest they only breathe slightly, not flap at full speed forever.
        float wing = (e.isFlying() || (!e.onGround() && !e.isInWater()))
                ? (float) Math.toRadians(28.0F + 42.0F * Mth.sin(time * 0.24F))
                : (float) Math.toRadians(50.0F + 5.0F * Mth.sin(time * 0.08F));
        body.getChild("wing_l").zRot = -wing;
        body.getChild("wing_r").zRot = wing;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.scale(model.renderScale, -model.renderScale, model.renderScale);
        model.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }
}
