package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import ru.aiforge.riftworld.registry.ModBlocks;

/** 2x3 sliding lab door. Right-click or redstone toggles; open cells have no collision. */
public class LabDoorBlock extends ModelEntityBlock {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public LabDoorBlock(ModelKind kind, Properties props) {
        super(kind, props);
        registerDefaultState(defaultBlockState().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(OPEN);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(OPEN) ? Shapes.empty() : getShape(state, level, pos, ctx);
    }

    @Override
    public void placeParts(LevelAccessor level, BlockPos pos, BlockState state) {
        BlockState part = ModBlocks.PART.get().defaultBlockState().setValue(PartBlock.OPEN, state.getValue(OPEN));
        for (BlockPos off : kind.partOffsets(state.getValue(FACING))) {
            level.setBlock(pos.offset(off), part, 3);
        }
    }

    public void setOpen(Level level, BlockPos pos, BlockState state, boolean open) {
        if (state.getValue(OPEN) == open) return;
        BlockState ns = state.setValue(OPEN, open);
        level.setBlock(pos, ns, 3);
        for (BlockPos off : kind.partOffsets(state.getValue(FACING))) {
            BlockPos p = pos.offset(off);
            BlockState ps = level.getBlockState(p);
            if (ps.is(ModBlocks.PART.get())) level.setBlock(p, ps.setValue(PartBlock.OPEN, open), 3);
        }
        level.playSound(null, pos, open ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, 1.4F);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) setOpen(level, pos, state, !state.getValue(OPEN));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            BlockState ns = state.setValue(POWERED, powered);
            level.setBlock(pos, ns, 2);
            setOpen(level, pos, ns, powered);
        }
    }
}
