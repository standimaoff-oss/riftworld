package ru.aiforge.riftworld.block;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * v0.24.0: Full 8-neighbor connected carpet supporting 1x1, 1xN runners, 2x2, NxM rectangles,
 * L-shapes, T-junctions, +-crosses, hollow frames, and arbitrary diagonal/concave polygons.
 */
public class MergingCarpetBlock extends CarpetBlock {
    public static final BooleanProperty NORTH = PipeBlock.NORTH;
    public static final BooleanProperty EAST  = PipeBlock.EAST;
    public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    public static final BooleanProperty WEST  = PipeBlock.WEST;
    private static final Map<Direction, BooleanProperty> PROP = Map.of(
            Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST);

    public MergingCarpetBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(NORTH, EAST, SOUTH, WEST);
    }

    public BlockState withConnections(LevelAccessor level, BlockPos pos) {
        BlockState s = defaultBlockState();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            s = s.setValue(PROP.get(d), level.getBlockState(pos.relative(d)).is(this));
        }
        return s;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return withConnections(ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState s, Direction dir, BlockState nbr, LevelAccessor level, BlockPos pos, BlockPos nbrPos) {
        BlockState base = super.updateShape(s, dir, nbr, level, pos, nbrPos);
        if (base.isAir() || !dir.getAxis().isHorizontal()) return base;
        return base.setValue(PROP.get(dir), nbr.is(this));
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
