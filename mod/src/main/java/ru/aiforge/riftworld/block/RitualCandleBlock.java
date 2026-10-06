package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Rev 4 item 3: tall ivory temple candle for the mosaic ritual. Right-click with a flint & steel (or a torch) to light. */
public class RitualCandleBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(3, 0, 3, 13, 2, 13), Block.box(5, 2, 5, 11, 15, 11), Block.box(6.5, 15, 6.5, 9.5, 16, 9.5));

    public RitualCandleBlock(Properties p) {
        super(p.lightLevel(s -> s.getValue(LIT) ? 14 : 0));
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(LIT);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!state.getValue(LIT) && (player.getItemInHand(hand).is(Items.FLINT_AND_STEEL) || player.getItemInHand(hand).is(Items.TORCH)
                || player.getItemInHand(hand).is(Items.FIRE_CHARGE))) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(LIT, true), 3);
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.2F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rnd) {
        if (state.getValue(LIT)) {
            double x = pos.getX() + 0.5, z = pos.getZ() + 0.5;
            level.addParticle(ParticleTypes.SMALL_FLAME, x, pos.getY() + 1.1, z, 0, 0.004, 0);
            if (rnd.nextInt(4) == 0) level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, pos.getY() + 1.15, z, 0, 0.005, 0);
        }
    }
}
