package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.registry.ElisarBlocks;

/**
 * ЭПИЧНАЯ ВОДА (rev 7, п. 2): бегущие гребни волн и прибойная пена на берегу Элисара.
 * Полностью клиентский эффект: ноль пакетов на сервер, ноль изменений мира.
 * - Гребни: две бегущие синусоиды + косая зыбь; там, где суммарная волна выше порога,
 *   над поверхностью воды рисуется белый «барашек» (quad), высота и яркость дышат от фазы.
 * - Прибой: у колонок воды, соседних с твёрдым блоком, пена пульсирует ярче.
 * - Брызги/туман: клиентский тик сеет vanilla-частицы (CLOUD у берега, POOF у падающей воды).
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WaveFx {
    private static final int RAD = 30;

    private WaveFx() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;
        Vec3 cam = e.getCamera().getPosition();
        float t = level.getGameTime() + e.getPartialTick();
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        VertexConsumer vc = mc.renderBuffers().bufferSource().getBuffer(RenderType.debugQuads());
        int bx = (int) Math.floor(cam.x), bz = (int) Math.floor(cam.z);
        int drawn = 0;
        for (int x = bx - RAD; x <= bx + RAD; ++x) {
            for (int z = bz - RAD; z <= bz + RAD; ++z) {
                int hy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);
                int sy = hy - 1;
                if (sy < 1) continue;
                BlockState st = level.getBlockState(new BlockPos(x, sy, z));
                if (st.getBlock() != ElisarBlocks.WATER.get()) continue;
                if (!level.getBlockState(new BlockPos(x, hy, z)).isAir()) continue;
                // swell: two travelling swells + a diagonal chop
                double w = Math.sin(x * 0.31 + t * 0.052) * Math.cos(z * 0.26 - t * 0.041)
                        + 0.55 * Math.sin((x + z) * 0.115 + t * 0.083);
                boolean shore = false;
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockPos np = new BlockPos(x + d.getStepX(), sy, z + d.getStepZ());
                    if (level.getBlockState(np).isSolidRender(level, np)) { shore = true; break; }
                }
                if (shore) {
                    float pulse = 0.55F + 0.35F * (float) Math.sin(t * 0.09 + ((x * 7 + z * 13) & 15));
                    hquad(vc, ps, x, sy + 0.90F, z, 0.92F, 0.97F, 1.0F, pulse);
                    ++drawn;
                } else if (w > 0.86) {
                    float k = (float) ((w - 0.86) / 0.7);
                    float y = sy + 0.84F + 0.12F * k;
                    hquad(vc, ps, x, y, z, 0.80F + 0.15F * k, 0.92F + 0.06F * k, 1.0F, 0.35F + 0.5F * k);
                    ++drawn;
                }
                if (drawn > 6000) break;
            }
            if (drawn > 6000) break;
        }
        ps.popPose();
        mc.renderBuffers().bufferSource().endBatch(RenderType.debugQuads());
    }

    private static void hquad(VertexConsumer vc, PoseStack ps, float x, float y, float z, float r, float g, float b, float a) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        vc.vertex(m, x, y, z).color(r, g, b, a).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, x, y, z + 1).color(r, g, b, a).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, x + 1, y, z + 1).color(r, g, b, a).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, x + 1, y, z).color(r, g, b, a).normal(n, 0, 1, 0).endVertex();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused() || mc.player == null) return;
        long gt = level.getGameTime();
        if ((gt & 3) != 0) return;
        java.util.Random r = new java.util.Random(gt);
        for (int i = 0; i < 10; ++i) {
            int x = (int) (mc.player.getX() + (r.nextDouble() - 0.5) * 44);
            int z = (int) (mc.player.getZ() + (r.nextDouble() - 0.5) * 44);
            int hy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);
            BlockPos bp = new BlockPos(x, hy - 1, z);
            BlockState st = level.getBlockState(bp);
            if (st.getBlock() != ElisarBlocks.WATER.get()) continue;
            boolean shore = false;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos np = bp.relative(d);
                if (level.getBlockState(np).isSolidRender(level, np)) { shore = true; break; }
            }
            if (shore) level.addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD, x + 0.5, hy + 0.06, z + 0.5, 0, 0.015, 0);
            if (level.getBlockState(bp.below()).isAir())
                level.addParticle(net.minecraft.core.particles.ParticleTypes.POOF, x + 0.5, hy - 0.3, z + 0.5, 0, -0.06, 0);
        }
    }
}
