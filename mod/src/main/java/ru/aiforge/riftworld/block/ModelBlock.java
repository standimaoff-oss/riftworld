package ru.aiforge.riftworld.block;

import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import ru.aiforge.riftworld.registry.ModBlocks;

/**
 * Master block of a GLB-rendered machine. The visible model is drawn by the block-entity renderer;
 * the remaining cells of the footprint are invisible {@link PartBlock}s.
 */
public class ModelBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty BROKEN = BooleanProperty.create("broken");

    public final ModelKind kind;
    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

    public ModelBlock(ModelKind kind, Properties props) {
        super(props);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH)
                .setValue(POWERED, false).setValue(BROKEN, false));
        for (Direction d : Direction.Plane.HORIZONTAL) {
            shapes.put(d, rotateShape(kind.masterShape, d));
        }
    }

    static VoxelShape rotateShape(VoxelShape s, Direction facing) {
        if (facing.getAxis() == Direction.Axis.Z) return s;
        VoxelShape out = Shapes.empty();
        for (AABB b : s.toAabbs()) {
            out = Shapes.or(out, Shapes.box(1 - b.maxZ, b.minY, b.minX, 1 - b.minZ, b.maxY, b.maxX));
        }
        return out;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, POWERED, BROKEN);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapes.get(state.getValue(FACING));
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection().getOpposite();
        BlockPos pos = ctx.getClickedPos();
        Level level = ctx.getLevel();
        for (BlockPos off : kind.partOffsets(facing)) {
            BlockPos p = pos.offset(off);
            if (!level.isInWorldBounds(p) || !level.getBlockState(p).canBeReplaced(ctx)) return null;
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        if (!level.isClientSide) placeParts(level, pos, state);
    }

    public void placeParts(LevelAccessor level, BlockPos pos, BlockState state) {
        BlockState part = ModBlocks.PART.get().defaultBlockState();
        for (BlockPos off : kind.partOffsets(state.getValue(FACING))) {
            level.setBlock(pos.offset(off), part, 3);
        }
    }

    public boolean partsPassable(BlockState state) {
        return false;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CrateBlockEntity crate) {
                net.minecraft.world.Containers.dropContents(level, pos, crate);
            }
            for (BlockPos off : kind.partOffsets(state.getValue(FACING))) {
                BlockPos p = pos.offset(off);
                if (level.getBlockState(p).is(ModBlocks.PART.get())) {
                    level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 35);
                }
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    /** True if the given world position is one of this master's cells. */
    public boolean covers(BlockState state, BlockPos master, BlockPos p) {
        BlockPos rel = p.subtract(master);
        if (rel.equals(BlockPos.ZERO)) return true;
        return kind.partOffsets(state.getValue(FACING)).contains(rel);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RiftBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state,
            net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide || kind != ModelKind.PORTAL_HELM) return null;
        return (l, p, s, be) -> {
            if (be instanceof RiftBlockEntity r) LabPower.tick((net.minecraft.server.level.ServerLevel) l, p, r);
        };
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
