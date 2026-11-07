package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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
import ru.aiforge.riftworld.client.model.PegasusBlockbenchModel;
import ru.aiforge.riftworld.entity.PegasusEntity;

/**
 * High-detail Blockbench MCP renderer for {@link PegasusEntity}:
 * 31 articulated bones, 177 sculpted cuboids, 512x512 celestial alabaster-dapple & royal tack atlas,
 * folded swan-wings on ground vs 5.3-block 3-tiered feathered wingspan in flight, and dynamic
 * saddle/bridle visibility ({@link PegasusEntity#hasSaddle()} / {@link PegasusEntity#isTamed()}).
 */
public class PegasusRenderer extends EntityRenderer<PegasusEntity> {
    private static final ResourceLocation TEX =
            new ResourceLocation(RiftWorld.MODID, "textures/entity/pegasus.png");

    private final PegasusBlockbenchModel model;
    private boolean geoLogged;

    public PegasusRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 1.1F;
        this.model = new PegasusBlockbenchModel(ctx.bakeLayer(PegasusBlockbenchModel.LAYER_LOCATION));
    }

    @Override
    public ResourceLocation getTextureLocation(PegasusEntity e) {
        return TEX;
    }

    @Override
    public boolean shouldRender(PegasusEntity e, Frustum frustum, double x, double y, double z) {
        if (e.noCulling) return true;
        return frustum.isVisible(e.getBoundingBoxForCulling().inflate(3.5D, 2.5D, 3.5D));
    }

    @Override
    public void render(PegasusEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
        if (!this.geoLogged) {
            this.geoLogged = true;
            GeoModel.of("pegasus");
        }

        float bodyYaw = e.isNoAi()
                ? Mth.rotLerp(partial, e.yRotO, e.getYRot())
                : Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
        float headYaw = Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot) - bodyYaw;
        float pitch = Mth.lerp(partial, e.xRotO, e.getXRot());
        float limbPos = e.walkAnimation.position(partial);
        float limbSpd = e.walkAnimation.speed(partial);
        float age = e.tickCount + partial;
        int overlay = OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(e.hurtTime > 0 || e.deathTime > 0));

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        if (e.isFlying()) {
            pose.mulPose(Axis.XP.rotationDegrees(pitch * 0.35F));
        }
        // Standard Mojang Blockbench Y-up -> Y-down entity transform (hooves sit flush at Y = 0.0!)
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.0F, -1.501F, 0.0F);

        this.model.setupAnim(e, limbPos, limbSpd, age, headYaw, pitch);

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX));
        this.model.renderToBuffer(pose, vc, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);

        pose.popPose();
        super.render(e, yaw, partial, pose, buf, light);
    }
}
