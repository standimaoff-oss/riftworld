package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import ru.aiforge.riftworld.client.geo.GeoModel;
import ru.aiforge.riftworld.entity.ElGranMajaEntity;

/** 13-bone hierarchy; the head, tail and jaw are animated around their actual pivots. */
public final class ElGranMajaRenderer extends EntityRenderer<ElGranMajaEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("riftworld", "textures/entity/el_gran_maja.png");
    private static final double CENTER_Z = 104.6875; // midpoint of the 322-block model, in blocks

    public ElGranMajaRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(ElGranMajaEntity e) { return TEXTURE; }

    @Override
    public boolean shouldRender(ElGranMajaEntity e, Frustum frustum, double x, double y, double z) {
        if (!e.isReadyToRender()) return false;
        int rd = Minecraft.getInstance().options.renderDistance().get();
        double maxDistance = Math.max(192.0, rd * 16.0 + 180.0);
        return e.distanceToSqr(x, y, z) <= maxDistance * maxDistance
                && frustum.isVisible(e.getBoundingBoxForCulling().inflate(180.0, 50.0, 180.0));
    }

    @Override
    public void render(ElGranMajaEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!e.isReadyToRender()) return;
        GeoModel model = GeoModel.of("el_gran_maja");
        if (model == null) return;
        // Use the same entity yaw that the server's collision envelopes test.
        // Mob body-yaw smoothing can otherwise start at zero after spawn or lag behind.
        float bodyYaw = Mth.rotLerp(partial, e.yRotO, e.getYRot());
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        // Riftworld's GeoModel uses Minecraft's Y-down model coordinates.
        // Reflect once here so the reference's dorsal side points upward.
        pose.scale(model.renderScale, -model.renderScale, model.renderScale);
        pose.translate(0.0, 0.0, -CENTER_Z);
        model.animate(e.tickCount + partial);
        // In Y-up source coordinates: +8 is closed, -13 is fully open.
        float jawDegrees = Mth.lerp(e.jaw(partial), 8.0F, -13.0F);
        model.root.getChild("head").getChild("jaw").xRot = (float) Math.toRadians(-jawDegrees);
        // ModelPart.render recursively draws children; drawing getAllParts()
        // separately duplicates bones and loses their parent transformations.
        model.root.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
