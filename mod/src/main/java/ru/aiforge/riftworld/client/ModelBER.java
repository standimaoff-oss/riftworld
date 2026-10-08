package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.block.ModelBlock;
import ru.aiforge.riftworld.block.ModelKind;
import ru.aiforge.riftworld.glb.Pose;

/** Draws the GLB of a model block: after Rev 5 the two crates are the only ones left. */
public class ModelBER<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private static final ResourceLocation[] PATHS = new ResourceLocation[ModelKind.values().length];

    public ModelBER(BlockEntityRendererProvider.Context ctx) {
    }

    public static ResourceLocation path(ModelKind k) {
        ResourceLocation r = PATHS[k.ordinal()];
        if (r == null) PATHS[k.ordinal()] = r = new ResourceLocation(RiftWorld.MODID, "models/glb/" + k.glb + ".glb");
        return r;
    }

    /** Applies node visibility/animation for a block state (may be called for items too). */
    public static void pose(Pose pose, ModelKind kind, float time) {
    }

    @Override
    public void render(T be, float partial, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof ModelBlock mb)) return;
        if (mb.kind.isElisar()) return;                                 // baked OBJ - drawn by the chunk mesher
        GlbAssets.Loaded lm = GlbAssets.get(path(mb.kind));
        if (lm == null) return;
        float time = be.getLevel() == null ? 0 : be.getLevel().getGameTime() + partial;
        Pose pose = lm.scratch();
        pose(pose, mb.kind, time);
        pose.compute();
        Direction f = state.getValue(ModelBlock.FACING);
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-f.toYRot()));
        GlbRenderer.render(lm, pose, ps, buffers, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(T be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
