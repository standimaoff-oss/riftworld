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
        if (stack.getItem() instanceof ru.aiforge.riftworld.item.GlbItem gi) {
            renderGlbItem(gi, ctx, ps, buffers, light, overlay);
            return;
        }
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

    private static final java.util.Map<String, float[]> BOUNDS = new java.util.HashMap<>();

    /** Model-space bounds {minX,minY,minZ,maxX,maxY,maxZ} of a static GLB (grip = origin). */
    static float[] bounds(GlbAssets.Loaded lm, String key) {
        return BOUNDS.computeIfAbsent(key, k -> {
            float[] b = {1e9F, 1e9F, 1e9F, -1e9F, -1e9F, -1e9F};
            Pose p = lm.staticPose();
            org.joml.Vector3f v = new org.joml.Vector3f();
            for (int i = 0; i < lm.model.nodes.size(); ++i) {
                int mi = lm.model.nodes.get(i).mesh;
                if (mi < 0) continue;
                for (var prim : lm.model.meshes.get(mi).prims) {
                    for (int j = 0; j + 2 < prim.pos.length; j += 3) {
                        p.world[i].transformPosition(prim.pos[j], prim.pos[j + 1], prim.pos[j + 2], v);
                        b[0] = Math.min(b[0], v.x); b[1] = Math.min(b[1], v.y); b[2] = Math.min(b[2], v.z);
                        b[3] = Math.max(b[3], v.x); b[4] = Math.max(b[4], v.y); b[5] = Math.max(b[5], v.z);
                    }
                }
            }
            return b;
        });
    }

    /**
     * GLB weapons. glTF frame of the weapon models: grip at the origin, haft along +Y, blade towards +Z.
     * The item is drawn inside the unit cube the ItemRenderer hands us (identity display transforms in the JSON).
     */
    private void renderGlbItem(ru.aiforge.riftworld.item.GlbItem gi, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        GlbAssets.Loaded lm = GlbAssets.get(new net.minecraft.resources.ResourceLocation(ru.aiforge.riftworld.RiftWorld.MODID, "models/glb/" + gi.glb() + ".glb"));
        if (lm == null) return;
        float len = gi.length();
        boolean big = len > 2.0F;
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        switch (ctx) {
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> {
                float side = ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 1 : -1;
                if (big) {
                    ps.translate(0.05 * side, -0.25, -0.05);
                    ps.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(14 * side));
                    ps.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-12));
                    ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(side > 0 ? 160 : 200));
                    ps.scale(0.62F, 0.62F, 0.62F);
                } else {
                    ps.translate(0.0, -0.1, 0.0);
                    ps.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(10 * side));
                    ps.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-25));
                    ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(side > 0 ? 165 : 195));
                    ps.scale(0.85F, 0.85F, 0.85F);
                }
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                // item +Z = up the arm, item +Y = forward when the arm hangs down (see ItemInHandLayer)
                ps.mulPose(com.mojang.math.Axis.XP.rotationDegrees(big ? 70 : 80));
                ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
                float s = big ? 0.95F : 0.9F;
                ps.scale(s, s, s);
            }
            case HEAD -> {
                ps.scale(0.4F, 0.4F, 0.4F);
            }
            default -> {
                // GUI / ground / item frame: fit the whole weapon diagonally into the cube
                float[] b = bounds(lm, gi.glb());
                float cy = (b[1] + b[4]) * 0.5F, cz = (b[2] + b[5]) * 0.5F;
                float ext = Math.max(b[4] - b[1], b[5] - b[2]);
                float fit = (ctx == ItemDisplayContext.GROUND ? 0.62F : ctx == ItemDisplayContext.FIXED ? 1.1F : 1.25F) / ext;
                ps.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-45));
                ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90));
                ps.scale(fit, fit, fit);
                ps.translate(0, -cy, -cz);
            }
        }
        GlbRenderer.render(lm, lm.staticPose(), ps, buffers, light, overlay);
        ps.popPose();
    }
}
