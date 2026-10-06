package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import ru.aiforge.riftworld.registry.TempleBlocks;

/** A custom, non-colliding piece of the temple's undercroft flora. */
public class TemplePlantBlock extends BushBlock {
    public TemplePlantBlock() {
        super(Properties.of().mapColor(MapColor.PLANT).noCollission().instabreak().noOcclusion());
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(TempleBlocks.GARDEN_SOIL.get()) || state.is(TempleBlocks.MAZE_FLOOR.get())
                || state.is(TempleBlocks.MOSS.get());
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return mayPlaceOn(level.getBlockState(pos.below()), level, pos.below());
    }
}
