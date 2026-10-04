package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Walkable see-through floor grate. */
public class GrateBlock extends Block {
    public GrateBlock(Properties props) {
        super(props);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState other, Direction dir) {
        return other.is(this) || super.skipRendering(state, other, dir);
    }
}
