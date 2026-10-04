package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import ru.aiforge.riftworld.block.ModelBlock;
import ru.aiforge.riftworld.block.ModelKind;
import ru.aiforge.riftworld.glb.Pose;

/** Inventory/hand rendering of model blocks: the GLB scaled to fit one block. */
public class ModelItemRenderer extends BlockEntityWithoutLevelRenderer {
    public ModelItemRenderer(BlockEntityRenderDispatcher d, EntityModelSet m) {
        super(d, m);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof BlockItem bi) || !(bi.getBlock() instanceof ModelBlock mb)) return;
        ModelKind k = mb.kind;
        GlbAssets.Loaded lm = GlbAssets.get(ModelBER.path(k));
        if (lm == null) return;
        Pose pose = lm.scratch();
        ModelBER.pose(pose, k, mb.defaultBlockState(), null, 0);
        pose.compute();
        float s = 1.0F / Math.max(Math.max(k.width(), k.depth()), k.h);
        ps.pushPose();
        ps.translate(0.5, 0.5 - 0.5 * k.h * s, 0.5);
        ps.scale(s, s, s);
        ps.translate(-(k.x0 + k.x1) * 0.5, 0, k.back * 0.5);
        ps.translate(0, 0, 0);
        GlbRenderer.render(lm, pose, ps, buffers, light, overlay);
        ps.popPose();
    }
}
