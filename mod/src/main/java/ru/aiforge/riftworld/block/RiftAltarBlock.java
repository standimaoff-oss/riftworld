package ru.aiforge.riftworld.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Rev 5: the altar of the Rift in the middle of the red hexagram.
 * <p>
 * The block rises out of the floor of the ritual hall (its model lives above the floor surface; the 13x13 hexagram is
 * inlaid into that floor with the glowing veins of the rift by the temple builder), it keeps the stage of the glow
 * (0..3) and the rift itself: {@code open = true} means the black rift hangs above the altar and pulls the light out of
 * the hall. The rift is not built from blocks - it is drawn by the client around this altar.
 */
public class RiftAltarBlock extends Block implements EntityBlock {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);
    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    public RiftAltarBlock() {
        super(Properties.of().mapColor(MapColor.COLOR_BLACK).strength(-1.0F, 3600000.0F).sound(SoundType.DEEPSLATE)
                .noLootTable().lightLevel(s -> s.getValue(OPEN) ? 13 : s.getValue(STAGE) * 4)
                .emissiveRendering((s, l, p) -> true).hasPostProcess((s, l, p) -> true)
                .isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false));
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0).setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(STAGE, OPEN);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RiftAltarBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> RiftAltarBlockEntity.tick(l, p, s, (RiftAltarBlockEntity) be);
    }

    /** Right click: the altar reports how much of the ritual is done. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RiftAltarBlockEntity be) {
            be.report(player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rnd) {
        int stage = state.getValue(STAGE);
        if (stage <= 0 && !state.getValue(OPEN)) return;
        double x = pos.getX() + 0.5, y = pos.getY() + 1.05, z = pos.getZ() + 0.5;
        for (int i = 0; i < stage; ++i) {
            double dx = (rnd.nextDouble() - 0.5) * 9.0, dz = (rnd.nextDouble() - 0.5) * 9.0;
            level.addParticle(ParticleTypes.SMALL_FLAME, x + dx, y + 0.15, z + dz, 0, 0.01, 0);
        }
        if (state.getValue(OPEN) && rnd.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x + (rnd.nextDouble() - 0.5) * 3.0, y + 1.6,
                    z + (rnd.nextDouble() - 0.5) * 3.0, 0, 0.02, 0);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof RiftAltarBlockEntity be) {
            be.shutdown();
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(OPEN) ? 13 : state.getValue(STAGE) * 4;
    }

    /** Chat helper used by the block entity (kept here so both sides share the wording). */
    public static Component hint(boolean allLit, int lit, int total, boolean open) {
        if (open) return Component.translatable("riftworld.rift.open");
        if (total == 0) return Component.translatable("riftworld.rift.nocandles");
        if (!allLit) return Component.translatable("riftworld.rift.candles", lit, total);
        return Component.translatable("riftworld.rift.stand");
    }

}
