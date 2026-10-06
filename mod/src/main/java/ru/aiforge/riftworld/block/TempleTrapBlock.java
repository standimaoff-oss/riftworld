package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Re-arming floor sigils hidden among the undercroft tiles. */
public class TempleTrapBlock extends Block {
    public static final IntegerProperty KIND = IntegerProperty.create("kind", 0, 2);
    public static final BooleanProperty ARMED = BooleanProperty.create("armed");
    private static final int RESET_TICKS = 80;

    public TempleTrapBlock() {
        super(Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.5F, 3.0F).sound(SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(KIND, 0).setValue(ARMED, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(KIND, ARMED);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (level.isClientSide || !state.getValue(ARMED) || !(entity instanceof LivingEntity living)) return;

        level.setBlock(pos, state.setValue(ARMED, false), 3);
        level.scheduleTick(pos, this, RESET_TICKS);
        level.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 0.9F, 0.65F + state.getValue(KIND) * 0.25F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 0.15, pos.getZ() + 0.5,
                    12, 0.3, 0.05, 0.3, 0.05);
        }

        switch (state.getValue(KIND)) {
            case 0 -> living.hurt(level.damageSources().cactus(), 5.0F); // spring-loaded crystal teeth
            case 1 -> {
                living.setSecondsOnFire(4);
                living.hurt(level.damageSources().inFire(), 3.0F);       // a burst from a buried ember vent
            }
            default -> living.hurt(level.damageSources().magic(), 2.0F); // a collapsing rune drains vitality
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ARMED)) level.setBlock(pos, state.setValue(ARMED, true), 3);
    }
}
