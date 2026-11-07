package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import ru.aiforge.riftworld.client.model.LiopleurodonBlockbenchModel;
import ru.aiforge.riftworld.entity.LiopleurodonEntity;

/**
 * High-detail Blockbench MCP renderer for {@link LiopleurodonEntity}:
 * 24 articulated bones, 234 sculpted cuboids, 1024x1024 pelagic scale/scar atlas +
 * full-bright emissive golden-amber predator eyes ({@code liopleurodon_eyes.png}).
 */
public class LiopleurodonRenderer extends EntityRenderer<LiopleurodonEntity> {
    private static final ResourceLocation TEX =
            new ResourceLocation(RiftWorld.MODID, "textures/entity/liopleurodon.png");
    private static final ResourceLocation EYES_TEX =
            new ResourceLocation(RiftWorld.MODID, "textures/entity/liopleurodon_eyes.png");

    private final LiopleurodonBlockbenchModel model;
    private boolean geoLogged;

    public LiopleurodonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 2.4F;
        this.model = new LiopleurodonBlockbenchModel(ctx.bakeLayer(LiopleurodonBlockbenchModel.LAYER_LOCATION));
    }

    @Override
    public ResourceLocation getTextureLocation(LiopleurodonEntity e) {
        return TEX;
    }

    @Override
    public boolean shouldRender(LiopleurodonEntity e, Frustum frustum, double x, double y, double z) {
        if (e.noCulling) return true;
        return frustum.isVisible(e.getBoundingBoxForCulling().inflate(8.0D, 4.0D, 8.0D));
    }

    @Override
    public void render(LiopleurodonEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
        if (!this.geoLogged) {
            this.geoLogged = true;
            GeoModel.of("liopleurodon");
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
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        // Standard Mojang Blockbench Y-up -> Y-down entity transform
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.0F, -1.501F, 0.0F);

        this.model.setupAnim(e, limbPos, limbSpd, age, headYaw, pitch);

        VertexConsumer bodyVc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX));
        this.model.renderToBuffer(pose, bodyVc, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);

        VertexConsumer eyesVc = buf.getBuffer(RenderType.eyes(EYES_TEX));
        this.model.renderToBuffer(pose, eyesVc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        pose.popPose();
        super.render(e, yaw, partial, pose, buf, light);
    }
}
