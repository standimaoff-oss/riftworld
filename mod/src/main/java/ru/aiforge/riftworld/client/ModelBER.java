package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.block.LabDoorBlock;
import ru.aiforge.riftworld.block.ModelBlock;
import ru.aiforge.riftworld.block.ModelKind;
import ru.aiforge.riftworld.block.PortalLeverBlock;
import ru.aiforge.riftworld.block.RiftBlockEntity;
import ru.aiforge.riftworld.glb.Pose;

/** Draws the GLB of any {@link ModelBlock} with its state-driven node visibility and animation. */
public class ModelBER<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private static final ResourceLocation[] PATHS = new ResourceLocation[ModelKind.values().length];

    public ModelBER(BlockEntityRendererProvider.Context ctx) {
    }

    public static ResourceLocation path(ModelKind k) {
        ResourceLocation r = PATHS[k.ordinal()];
        if (r == null) PATHS[k.ordinal()] = r = new ResourceLocation(RiftWorld.MODID, "models/glb/" + k.glb + ".glb");
        return r;
    }

    /** Applies node visibility/animation for a block state (be may be null for items). */
    public static void pose(Pose pose, ModelKind kind, BlockState state, RiftBlockEntity be, float time) {
        boolean powered = state != null && state.getValue(ModelBlock.POWERED);
        pose.hide("screens_live", !powered);
        pose.hide("screens_dead", powered);
        pose.hide("leds_on", !powered);
        pose.hide("lamp_on", !powered);
        switch (kind) {
            case SPECIMEN_TANK -> {
                boolean broken = state != null && state.getValue(ModelBlock.BROKEN);
                pose.hide("glass", broken);
                pose.hide("liquid", broken);
                pose.hide("glass_broken", !broken);
            }
            case PORTAL_HELM -> {
                int mask = be == null ? 1 : be.stones;
                for (int i = 0; i < 5; ++i) {
                    boolean in = (mask & (1 << i)) != 0;
                    pose.hide("stone_" + i, !in);
                    pose.hide("led_" + i, !in);
                }
                if (be != null && be.stoneCount() >= 5) {
                    pose.rotate("wheel", time * 0.01F, 0.0F, 0.342F, 0.940F);
                }
            }
            case PORTAL_LEVER -> {
                float p = 0;
                if (be != null && be.getLevel() != null) {
                    float t = (be.getLevel().getGameTime() - be.animStart) + (time - (int) time);
                    if (t >= 0 && t < PortalLeverBlock.PULL_TICKS) {
                        p = t < 6 ? t / 6F : t < 18 ? 1F : 1F - (t - 18) / 12F;
                    }
                }
                pose.rotate("handle", -Mth.DEG_TO_RAD * 70F * Mth.clamp(p, 0, 1), 1, 0, 0);
                pose.hide("lamp_on", p <= 0.01F && !powered);
            }
            case LAB_DOOR -> {
                float o = be == null ? 0 : be.doorOpen;
                pose.translate("door_l", -0.84F * o, 0, 0);
                pose.translate("door_r", 0.84F * o, 0, 0);
            }
            default -> {
            }
        }
    }

    @Override
    public void render(T be, float partial, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof ModelBlock mb)) return;
        GlbAssets.Loaded lm = GlbAssets.get(path(mb.kind));
        if (lm == null) return;
        RiftBlockEntity rbe = be instanceof RiftBlockEntity r ? r : null;
        float time = be.getLevel() == null ? 0 : be.getLevel().getGameTime() + partial;
        float door = 0;
        if (rbe != null && mb.kind == ModelKind.LAB_DOOR) {
            boolean open = state.getValue(LabDoorBlock.OPEN);
            door = Mth.lerp(partial, rbe.doorOpenPrev, rbe.doorOpen);
            // advance once per frame-tick pair (cheap client-side smoothing)
            long gt = be.getLevel().getGameTime();
            if (gt != lastTick(rbe)) {
                rbe.doorOpenPrev = rbe.doorOpen;
                rbe.doorOpen = Mth.clamp(rbe.doorOpen + (open ? 0.12F : -0.12F), 0, 1);
                TICKS.put(rbe, gt);
            }
        }
        Pose pose = lm.scratch();
        pose(pose, mb.kind, state, rbe, time);
        if (rbe != null && mb.kind == ModelKind.LAB_DOOR) {
            pose.translate("door_l", -0.84F * (door - rbe.doorOpen), 0, 0);
            pose.translate("door_r", 0.84F * (door - rbe.doorOpen), 0, 0);
        }
        pose.compute();
        Direction f = state.getValue(ModelBlock.FACING);
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-f.toYRot()));
        GlbRenderer.render(lm, pose, ps, buffers, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }

    private static final java.util.WeakHashMap<RiftBlockEntity, Long> TICKS = new java.util.WeakHashMap<>();

    private static long lastTick(RiftBlockEntity be) {
        Long l = TICKS.get(be);
        return l == null ? Long.MIN_VALUE : l;
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
