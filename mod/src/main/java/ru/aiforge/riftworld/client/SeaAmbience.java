package ru.aiforge.riftworld.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.flight.Flight;
import ru.aiforge.riftworld.registry.ElisarBlocks;

/**
 * Рев 10 «Живое море 2»: атмосфера под водой — планктон, пузыри, искры.
 *
 * <p>Полностью клиентская: ни одного пакета на сервер, частицы живут 1–2 секунды и стоят копейки.
 * <ul>
 *   <li><b>планктон</b> — мелкие светящиеся точки, медленно плывущие в толще воды (заметен на просвет);</li>
 *   <li><b>пузыри</b> — поднимаются от дна и из устьев пещер, у самого дна чаще;</li>
 *   <li><b>искры</b> — у светящихся кораллов и пещерных наростов, чтобы риф «дышал»;</li>
 *   <li><b>взвесь ила</b> — у дна в илистых низинах.</li>
 * </ul>
 * Срабатывает только в Элисаре и только когда камера под водой, поэтому на поверхности не мешает.
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, value = Dist.CLIENT)
public final class SeaAmbience {
    private static final RandomSource RND = RandomSource.create();
    private static int tick;

    private SeaAmbience() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused()) return;
        if (level.dimension() != Flight.ELISAR_DIM) return;
        if (mc.options.hideGui && mc.getCameraEntity() == null) return;
        if (++tick % 2 != 0) return;

        var cam = mc.gameRenderer.getMainCamera();
        boolean under = cam.getFluidInCamera() == FogType.WATER;
        BlockPos at = cam.getBlockPosition();
        var camPos = cam.getPosition();

        if ("true".equalsIgnoreCase(System.getProperty("riftworld.fhd")) && tick <= 60 && tick % 20 == 0) {
            try {
                long win = mc.getWindow().getWindow();
                if (win != 0L && mc.getWindow().getWidth() < 1800) {
                    org.lwjgl.glfw.GLFW.glfwSetWindowPos(win, 0, 0);
                    org.lwjgl.glfw.GLFW.glfwSetWindowSize(win, 1920, 1080);
                }
            } catch (Throwable ignored) {
            }
        }

        // ---- надземные кастомные частицы листьев в воздухе (Фиолетовые холмы + Роща Сизо)
        if (!under) {
            if (ru.aiforge.riftworld.world.VioletHills.regionAt(camPos.x, camPos.z) != null) {
                var look = cam.getLookVector();
                for (int i = 0; i < 8; ++i) {
                    double fwd = 6.0 + RND.nextDouble() * 28.0;
                    double side = (RND.nextDouble() - 0.5) * 38.0;
                    double px = camPos.x + look.x() * fwd - look.z() * side + (RND.nextDouble() - 0.5) * 8.0;
                    double pz = camPos.z + look.z() * fwd + look.x() * side + (RND.nextDouble() - 0.5) * 8.0;
                    double py = camPos.y - 1.0 + RND.nextDouble() * 17.0;
                    level.addAlwaysVisibleParticle(ru.aiforge.riftworld.registry.ModParticles.VIOLET_LEAF.get(),
                            true, px, py, pz,
                            0.018 + RND.nextDouble() * 0.014,
                            -0.005 - RND.nextDouble() * 0.008,
                            0.008 + (RND.nextDouble() - 0.4) * 0.014);
                }
            } else if (ru.aiforge.riftworld.world.SizoGrove.regionAt(camPos.x, camPos.z) != null) {
                var look = cam.getLookVector();
                for (int i = 0; i < 4; ++i) {
                    double fwd = 5.0 + RND.nextDouble() * 24.0;
                    double side = (RND.nextDouble() - 0.5) * 28.0;
                    double px = camPos.x + look.x() * fwd - look.z() * side;
                    double pz = camPos.z + look.z() * fwd + look.x() * side;
                    double py = camPos.y - 1.0 + RND.nextDouble() * 14.0;
                    level.addAlwaysVisibleParticle(ru.aiforge.riftworld.registry.ModParticles.SIZO_LEAF.get(),
                            true, px, py, pz,
                            0.010 + RND.nextDouble() * 0.008,
                            -0.004 - RND.nextDouble() * 0.006,
                            (RND.nextDouble() - 0.5) * 0.008);
                }
            }
        }

        // ---- планктон и пузыри в толще воды
        if (under) {
            for (int i = 0; i < 3; ++i) {
                double dx = (RND.nextDouble() - 0.5) * 14.0, dy = (RND.nextDouble() - 0.5) * 10.0, dz = (RND.nextDouble() - 0.5) * 14.0;
                double x = at.getX() + 0.5 + dx, y = at.getY() + 0.5 + dy, z = at.getZ() + 0.5 + dz;
                if (!isWater(level, BlockPos.containing(x, y, z))) continue;
                if (RND.nextInt(3) == 0) {
                    level.addParticle(ParticleTypes.BUBBLE, x, y, z, 0.0, 0.02 + RND.nextDouble() * 0.03, 0.0);
                } else {
                    level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.0, 0.0);
                }
            }
        }

        // ---- дно и устья пещер: пузырьковые струи, искры светящихся кораллов, взвесь ила
        if (RND.nextInt(3) == 0) {
            int x = at.getX() + RND.nextInt(17) - 8, z = at.getZ() + RND.nextInt(17) - 8;
            int y = at.getY() + RND.nextInt(17) - 8;
            BlockPos p = new BlockPos(x, y, z);
            if (!isWater(level, p)) return;
            var below = level.getBlockState(p.below());
            if (RND.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.BUBBLE_COLUMN_UP, x + 0.5, y + 0.2, z + 0.5, 0.0, 0.06, 0.0);
            } else if (below.is(ElisarBlocks.CORAL_GLOW.get()) || below.is(ElisarBlocks.ANEMONE.get())
                    || below.is(ElisarBlocks.CAVE_GLOW.get()) || below.is(ElisarBlocks.CORAL_CAP_PINK.get())) {
                level.addParticle(ParticleTypes.END_ROD, x + 0.5, y + 0.4, z + 0.5, 0.0, 0.01, 0.0);
            } else if (below.is(ElisarBlocks.SILT.get())) {
                level.addParticle(ParticleTypes.ASH, x + 0.5, y + 0.3, z + 0.5, 0.0, 0.004, 0.0);
            } else if (below.isAir() || below.is(Blocks.AIR)) {
                level.addParticle(ParticleTypes.BUBBLE, x + 0.5, y + 0.1, z + 0.5, 0.0, 0.03, 0.0);
            }
        }
    }

    private static boolean isWater(ClientLevel level, BlockPos p) {
        return level.getBlockState(p).is(ElisarBlocks.WATER.get());
    }
}
