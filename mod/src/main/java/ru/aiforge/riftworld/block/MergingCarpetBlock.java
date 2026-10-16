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
import ru.aiforge.riftworld.block.BridgeWalkBlock;

/**
 * Rev 4 item 9: edge-connecting carpet. Single pieces show a fringe border on every free side; a 2x2 (or any N x M)
 * patch reads as ONE big carpet, because borders hide on connected sides and disappear inside the area.
 * Fixed: doesn't break on bridge_walk or other custom solid blocks, updates connections correctly.
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
        // for vertical, let vanilla handle survival but don't break if below is bridge_walk or other solid custom
        BlockState newState = super.updateShape(state, dir, neighbor, level, pos, neighborPos);
        // if super would break to air because below not solid, check if below is actually walkable
        if (dir == Direction.DOWN && newState.isAir()) {
            BlockState below = level.getBlockState(pos.below());
            if (below.getBlock() instanceof BridgeWalkBlock || below.isSolid() || below.getBlock() instanceof ru.aiforge.riftworld.block.ModelBlock) {
                return state;
            }
        }
        return newState;
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        // allow on bridge_walk, model blocks, and any solid
        if (belowState.getBlock() instanceof BridgeWalkBlock) return true;
        if (belowState.isSolid()) return true;
        return super.canSurvive(state, level, pos);
    }
}
