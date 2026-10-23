package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import ru.aiforge.riftworld.registry.ElisarBlocks;

/** Рев 8: плоский декаль на дне (ракушки, морские звёзды) — тонкая пластина, пройти можно. */
public class SeaDecalBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public SeaDecalBlock(BlockBehaviour.Properties p) {
        super(p);
    }

    @Override
    protected boolean canSurvive(BlockState s, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(ElisarBlocks.SILT.get()) || below.is(ElisarBlocks.SEABED.get()) || below.is(ElisarBlocks.REEF_STONE.get());
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
