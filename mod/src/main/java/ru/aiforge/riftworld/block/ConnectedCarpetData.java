package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

/**
 * 8-neighbor connected carpet data (N, NE, E, SE, S, SW, W, NW + 2x2 weave parity)
 * shared by {@link MergingCarpetBlock} and {@link MergingRugBlock}.
 */
public final class ConnectedCarpetData {
    public static final ModelProperty<Integer> PACKED = new ModelProperty<>();

    public static final int BIT_N  = 1;
    public static final int BIT_NE = 1 << 1;
    public static final int BIT_E  = 1 << 2;
    public static final int BIT_SE = 1 << 3;
    public static final int BIT_S  = 1 << 4;
    public static final int BIT_SW = 1 << 5;
    public static final int BIT_W  = 1 << 6;
    public static final int BIT_NW = 1 << 7;

    private ConnectedCarpetData() {
    }

    public static ModelData compute(Block self, BlockAndTintGetter level, BlockPos pos) {
        int mask = 0;
        if (same(self, level, pos.getX(),     pos.getY(), pos.getZ() - 1)) mask |= BIT_N;
        if (same(self, level, pos.getX() + 1, pos.getY(), pos.getZ() - 1)) mask |= BIT_NE;
        if (same(self, level, pos.getX() + 1, pos.getY(), pos.getZ()))     mask |= BIT_E;
        if (same(self, level, pos.getX() + 1, pos.getY(), pos.getZ() + 1)) mask |= BIT_SE;
        if (same(self, level, pos.getX(),     pos.getY(), pos.getZ() + 1)) mask |= BIT_S;
        if (same(self, level, pos.getX() - 1, pos.getY(), pos.getZ() + 1)) mask |= BIT_SW;
        if (same(self, level, pos.getX() - 1, pos.getY(), pos.getZ()))     mask |= BIT_W;
        if (same(self, level, pos.getX() - 1, pos.getY(), pos.getZ() - 1)) mask |= BIT_NW;
        int parityX = pos.getX() & 1;
        int parityZ = pos.getZ() & 1;
        int packed = mask | (parityX << 8) | (parityZ << 9);
        return ModelData.builder().with(PACKED, packed).build();
    }

    private static boolean same(Block self, BlockAndTintGetter level, int x, int y, int z) {
        return level.getBlockState(new BlockPos(x, y, z)).is(self);
    }

    /** Marks all 8 horizontal neighbors (including diagonals for L/T/+ inner corners) dirty for re-meshing. */
    public static void notifyEightNeighbors(Level level, BlockPos pos, Block self) {
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                if (dx == 0 && dz == 0) continue;
                mp.set(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
                if (!level.isLoaded(mp)) continue;
                BlockState ns = level.getBlockState(mp);
                if (ns.is(self)) {
                    BlockPos imm = mp.immutable();
                    level.sendBlockUpdated(imm, ns, ns, Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
                }
            }
        }
    }
}
