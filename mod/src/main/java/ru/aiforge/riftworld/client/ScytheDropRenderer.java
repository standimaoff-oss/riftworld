package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.entity.ScytheItemEntity;

/** The dropped boss scythe lies flat on the floor at full size and glows (full-bright). */
public class ScytheDropRenderer extends EntityRenderer<ScytheItemEntity> {
    static final ResourceLocation GLB = new ResourceLocation(RiftWorld.MODID, "models/glb/scythe.glb");

    public ScytheDropRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.6F;
    }

    @Override
    public ResourceLocation getTextureLocation(ScytheItemEntity e) {
        return GLB;
    }

    @Override
    public void render(ScytheItemEntity e, float yaw, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
        GlbAssets.Loaded lm = GlbAssets.get(GLB);
        if (lm == null) return;
        ps.pushPose();
        ps.translate(0, 0.04, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
        ps.mulPose(Axis.ZP.rotationDegrees(90));         // snath (glTF +Y) along the floor, blade flat
        ps.translate(0, -0.45, 0);                         // centre the length over the entity
        GlbRenderer.render(lm, lm.staticPose(), ps, buffers, 0xF000F0, OverlayTexture.NO_OVERLAY);
        ps.popPose();
        super.render(e, yaw, partial, ps, buffers, light);
    }
}
