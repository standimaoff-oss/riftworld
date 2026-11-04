package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
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

/**
 * Tall ivory temple candle for the portal mosaic ritual.
 * Right-click with flint & steel, torch, soul torch, fire charge, lantern, or Fire Aspect item (or hit with a burning projectile)
 * to light; immediately notifies any nearby {@link MosaicCoreBE} so lighting all sanctum candles opens the portal right away!
 */
public class RitualCandleBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(3, 0, 3, 13, 2, 13),
            Block.box(5, 2, 5, 11, 15, 11),
            Block.box(6.5, 15, 6.5, 9.5, 16, 9.5));

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

    public static boolean canIgniteCandle(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(Items.FLINT_AND_STEEL)
                || stack.is(Items.TORCH)
                || stack.is(Items.SOUL_TORCH)
                || stack.is(Items.FIRE_CHARGE)
                || stack.is(Items.LANTERN)
                || stack.is(Items.SOUL_LANTERN)
                || stack.is(Items.LAVA_BUCKET)
                || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, stack) > 0;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!state.getValue(LIT) && canIgniteCandle(held)) {
            if (!level.isClientSide && level instanceof ServerLevel sl) {
                sl.setBlock(pos, state.setValue(LIT, true), 3);
                sl.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.15F);
                sl.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.12, pos.getZ() + 0.5,
                        6, 0.06, 0.08, 0.06, 0.01);
                if (held.is(Items.FLINT_AND_STEEL)) {
                    held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                } else if (held.is(Items.FIRE_CHARGE) && !player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                MosaicCoreBE.notifyCandleLit(sl, pos, player instanceof ServerPlayer sp ? sp : null);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!level.isClientSide && level instanceof ServerLevel sl && projectile.isOnFire() && !state.getValue(LIT)) {
            BlockPos pos = hit.getBlockPos();
            sl.setBlock(pos, state.setValue(LIT, true), 3);
            sl.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.15F);
            ServerPlayer owner = projectile.getOwner() instanceof ServerPlayer sp ? sp : null;
            MosaicCoreBE.notifyCandleLit(sl, pos, owner);
        }
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
