package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.ForgeFlowingFluid;

/**
 * Elisar ocean water with CURRENTS (Rev 4 item 6: epic water): every drop, even a still source, drifts with the sea -
 * a slow planetary gyre around the archipelago plus a gentle meander. Boats, swimmers and drops all feel it.
 */
public final class ElisarWaterFluid {
    private ElisarWaterFluid() {
    }

    /** Horizontal drift field of the Elisar sea (blocks/tick acceleration scale, applied on top of vanilla flow). */
    public static Vec3 drift(double x, double z) {
        // the great gyre turns around the deep bay west-southwest of the main island
        double dx = x + 60.0, dz = z + 60.0;
        double r = Math.hypot(dx, dz) + 42.0;
        double wave = Math.sin(x * 0.017 + z * 0.011);                    // slow meander so the field is not a perfect circle
        double s = 0.030 + 0.010 * wave;
        return new Vec3(-dz / r * s, 0, dx / r * s);
    }

    public static final class Source extends ForgeFlowingFluid.Source {
        public Source(Properties p) {
            super(p);
        }

        @Override
        public Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState state) {
            return super.getFlow(level, pos, state).add(drift(pos.getX() + 0.5, pos.getZ() + 0.5));
        }
    }

    public static final class Flowing extends ForgeFlowingFluid.Flowing {
        public Flowing(Properties p) {
            super(p);
        }

        @Override
        public Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState state) {
            return super.getFlow(level, pos, state).add(drift(pos.getX() + 0.5, pos.getZ() + 0.5));
        }
    }
}
