package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Rev 4 item 9: edge-connecting carpet. Single pieces show a fringe border on every free side; a 2x2 (or any N x M)
 * patch reads as ONE big carpet, because borders hide on connected sides and disappear inside the area.
 */
public class MergingCarpetBlock extends CarpetBlock {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    public MergingCarpetBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false)
                .setValue(SOUTH, false).setValue(WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(NORTH, EAST, SOUTH, WEST);
    }

    private boolean merges(BlockState s) {
        return s.getBlock() == this;
    }

    private BlockState withConnections(BlockState s, LevelAccessor level, BlockPos pos) {
        return s.setValue(NORTH, merges(level.getBlockState(pos.north())))
                .setValue(EAST, merges(level.getBlockState(pos.east())))
                .setValue(SOUTH, merges(level.getBlockState(pos.south())))
                .setValue(WEST, merges(level.getBlockState(pos.west())));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return withConnections(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (dir.getAxis().isHorizontal()) {
            BooleanProperty side = switch (dir) {
                case NORTH -> NORTH;
                case EAST -> EAST;
                case SOUTH -> SOUTH;
                default -> WEST;
            };
            return state.setValue(side, merges(neighbor));
        }
        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }
}
