package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import ru.aiforge.riftworld.registry.ElisarBlocks;

/** Рев 8: плоский декаль на дне (ракушки, морские звёзды) — тонкая пластина, пройти можно. */
public class SeaDecalBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public SeaDecalBlock(BlockBehaviour.Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(WATERLOGGED);
    }

    @Override
    public FluidState getFluidState(BlockState s) {
        return s.getValue(WATERLOGGED)
                ? ru.aiforge.riftworld.registry.ModFluids.SOURCE.get().defaultFluidState()
                : net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public BlockState updateShape(BlockState s, Direction dir, BlockState neighbor, LevelAccessor lvl, BlockPos pos, BlockPos npos) {
        if (s.getValue(WATERLOGGED)) {
            lvl.scheduleTick(pos, ru.aiforge.riftworld.registry.ModFluids.SOURCE.get(),
                    ru.aiforge.riftworld.registry.ModFluids.SOURCE.get().getTickDelay(lvl));
        } else if (!s.canSurvive(lvl, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(s, dir, neighbor, lvl, pos, npos);
    }

    @Override
    public boolean canSurvive(BlockState s, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(ElisarBlocks.SILT.get()) || below.is(ElisarBlocks.SEABED.get()) || below.is(ElisarBlocks.REEF_STONE.get())
                || below.is(ElisarBlocks.CORAL_CAP_PINK.get()) || below.is(ElisarBlocks.CORAL_CAP_TEAL.get())
                || below.is(ElisarBlocks.CORAL_CAP_VIOLET.get()) || below.is(ElisarBlocks.COBBLE.get());
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
