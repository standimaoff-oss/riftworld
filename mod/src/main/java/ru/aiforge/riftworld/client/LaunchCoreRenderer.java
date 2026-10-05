package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.block.LaunchCoreBlockEntity;
import ru.aiforge.riftworld.glb.Pose;

/** Draws shaft lid leaves and the capsule (waiting / rising / landed, its lights blinking while the lab powers up). */
public class LaunchCoreRenderer implements BlockEntityRenderer<LaunchCoreBlockEntity> {
    private static final ResourceLocation HATCH = new ResourceLocation(RiftWorld.MODID, "models/glb/shaft_hatch.glb");
    private static final ResourceLocation CAPSULE = new ResourceLocation(RiftWorld.MODID, "models/glb/capsule.glb");

    public LaunchCoreRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(LaunchCoreBlockEntity be, float partial, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) return;
        float t = be.stage == LaunchCoreBlockEntity.IDLE ? 0 : (be.getLevel().getGameTime() - be.start) + partial;
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-be.facing().toYRot()));
        // --- lid
        if (be.stage <= LaunchCoreBlockEntity.RISING) {
            GlbAssets.Loaded h = GlbAssets.get(HATCH);
            if (h != null) {
                float a = Mth.HALF_PI * LaunchCoreBlockEntity.ease(t / LaunchCoreBlockEntity.OPEN_T);
                Pose p = h.scratch();
                p.rotate("leaf_l", -a, 0, 0, 1);
                p.rotate("leaf_r", a, 0, 0, 1);
                p.compute();
                GlbRenderer.render(h, p, ps, buffers, light, OverlayTexture.NO_OVERLAY);
            }
        }
        // --- capsule
        GlbAssets.Loaded c = GlbAssets.get(CAPSULE);
        if (c != null) {
            float off = be.capsuleOffset(t);
            int cl = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().offset(0, Mth.floor(off) + 2, 0));
            if (be.stage != LaunchCoreBlockEntity.IDLE) cl = Math.max(cl, LightTexture.pack(9, 0));
            Pose p = c.scratch();
            boolean flick = be.stage == LaunchCoreBlockEntity.FLICKER && !LaunchCoreBlockEntity.lightsOn(t);
            p.hide("lights_on", be.stage == LaunchCoreBlockEntity.IDLE || flick);
            boolean live = be.stage >= LaunchCoreBlockEntity.FLICKER && !flick;
            p.hide("screens_live", !live);
            p.hide("screens_dead", live);
            float d = Mth.lerp(partial, be.doorAnimPrev, be.doorAnim);
            p.translate("door_l", -1.22F * d, 0, 0);
            p.translate("door_r", 1.22F * d, 0, 0);
            p.compute();
            ps.pushPose();
            ps.translate(0, off, 0);
            GlbRenderer.render(c, p, ps, buffers, cl, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }
        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(LaunchCoreBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 160;
    }
}
