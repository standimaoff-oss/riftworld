package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * v0.24.0: Seamless 8-neighbor connected Elisar woven rug ({@code riftworld:rug}),
 * preserving {@link ModelBlock} state compatibility for existing structures while connecting
 * across 1x1, 2x2, runners, L-shapes, T-junctions, crosses, and arbitrary shapes.
 */
public class MergingRugBlock extends ModelBlock {
    public MergingRugBlock(ModelKind kind, Properties props) {
        super(kind, props);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !level.isEmptyBlock(pos.below());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState nbr, LevelAccessor level, BlockPos pos, BlockPos nbrPos) {
        if (!state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, dir, nbr, level, pos, nbrPos);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (!oldState.is(this)) {
            ConnectedCarpetData.notifyEightNeighbors(level, pos, this);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);
        if (!newState.is(this)) {
            ConnectedCarpetData.notifyEightNeighbors(level, pos, this);
        }
    }
}
