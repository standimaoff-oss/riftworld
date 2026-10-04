package ru.aiforge.riftworld.block;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import ru.aiforge.riftworld.registry.ModBlocks;

/**
 * Lab power-up after the fifth stone: every ceiling lamp and chandelier flickers 3 times and switches on, screens and
 * machine LEDs come alive; afterwards some basement lamps keep flickering now and then.
 */
public final class LabPower {
    public static final int IDLE = 0, POWERING = 1, POWERED = 2;
    /** tick, on/off pairs of the power-up flicker. */
    private static final int[] PATTERN = {8, 1, 11, 0, 20, 1, 23, 0, 25, 1, 28, 0, 44, 1};
    private static final int R = 42, DOWN = 9, UP = 15;

    private LabPower() {
    }

    public static void start(ServerLevel level, BlockPos helm, RiftBlockEntity be) {
        if (be.stage != IDLE) return;
        be.stage = POWERING;
        be.stageTime = level.getGameTime();
        scan(level, helm, be);
        be.sync();
        level.playSound(null, helm, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 3.0F, 0.6F);
    }

    static void scan(ServerLevel level, BlockPos c, RiftBlockEntity be) {
        be.lamps = new ArrayList<>();
        be.basementLamps = new ArrayList<>();
        be.chandeliers = new ArrayList<>();
        be.models = new ArrayList<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dy = -DOWN; dy <= UP; ++dy)
            for (int dx = -R; dx <= R; ++dx)
                for (int dz = -R; dz <= R; ++dz) {
                    m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    if (!level.isLoaded(m)) continue;
                    BlockState s = level.getBlockState(m);
                    if (s.is(ModBlocks.LAB_LAMP.get())) {
                        BlockPos p = m.immutable();
                        be.lamps.add(p);
                        if (dy <= -3) be.basementLamps.add(p);
                    } else if (s.getBlock() instanceof ModelBlock mb) {
                        if (mb.kind == ModelKind.CHANDELIER) be.chandeliers.add(m.immutable());
                        else if (mb.kind != ModelKind.SIREN && mb.kind != ModelKind.LAB_DOOR) be.models.add(m.immutable());
                    }
                }
        be.flickerOn = new long[be.basementLamps.size()];
    }

    private static void setLamp(ServerLevel level, BlockPos p, boolean on) {
        BlockState s = level.getBlockState(p);
        if (s.is(ModBlocks.LAB_LAMP.get()) && s.getValue(LabLampBlock.LIT) != on) {
            level.setBlock(p, s.setValue(LabLampBlock.LIT, on), 2);
        }
    }

    private static void setPowered(ServerLevel level, BlockPos p, boolean on) {
        BlockState s = level.getBlockState(p);
        if (s.getBlock() instanceof ModelBlock && s.getValue(ModelBlock.POWERED) != on) {
            level.setBlock(p, s.setValue(ModelBlock.POWERED, on), 2);
        }
    }

    public static void tick(ServerLevel level, BlockPos pos, RiftBlockEntity be) {
        if (be.stage == IDLE) return;
        if (be.lamps == null) scan(level, pos, be);
        long t = level.getGameTime() - be.stageTime;
        if (be.stage == POWERING) {
            for (int i = 0; i < PATTERN.length; i += 2) {
                if (t == PATTERN[i]) {
                    boolean on = PATTERN[i + 1] == 1;
                    for (BlockPos p : be.lamps) setLamp(level, p, on);
                    for (BlockPos p : be.chandeliers) setPowered(level, p, on);
                    level.playSound(null, pos, on ? SoundEvents.RESPAWN_ANCHOR_CHARGE : SoundEvents.RESPAWN_ANCHOR_DEPLETE.get(),
                            SoundSource.BLOCKS, 2.5F, on ? 1.3F : 0.8F);
                }
            }
            if (t >= PATTERN[PATTERN.length - 2]) {
                for (BlockPos p : be.models) setPowered(level, p, true);
                be.stage = POWERED;
                be.stageTime = level.getGameTime();
                be.sync();
                level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 3.0F, 0.7F);
            }
            return;
        }
        // POWERED: occasional flicker of basement lamps
        long now = level.getGameTime();
        for (int i = 0; i < be.flickerOn.length; ++i) {
            if (be.flickerOn[i] != 0 && now >= be.flickerOn[i]) {
                be.flickerOn[i] = 0;
                setLamp(level, be.basementLamps.get(i), true);
            }
        }
        if (!be.basementLamps.isEmpty() && level.random.nextInt(14) == 0) {
            int i = level.random.nextInt(be.basementLamps.size());
            if (be.flickerOn[i] == 0) {
                setLamp(level, be.basementLamps.get(i), false);
                be.flickerOn[i] = now + 2 + level.random.nextInt(9);
            }
        }
    }
}
