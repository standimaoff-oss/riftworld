package ru.aiforge.riftworld.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;

/**
 * Part 11 «Храм полностью»: the carved FACE of a dead faithful sleeping in the temple's bones. It drowses as plain
 * ivory relief - but wake the block while a living player stands near, and the face opens amber eyes: it whispers
 * (a low soul-chief murmur, rarely an old curse) and sheds slow souls. Falls back asleep when the living leave.
 * Self-scheduled ticking (no BlockEntity) so ruins scattered in the overworld wake the same way.
 */
public class TempleSpiritBlock extends Block {
    public static final BooleanProperty AWAKE = BooleanProperty.create("awake");

    public TempleSpiritBlock(Properties p) {
        super(p);
        registerDefaultState(defaultBlockState().setValue(AWAKE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(AWAKE);
    }

    @Override
    public void onPlace(BlockState s, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!level.isClientSide && !s.is(old.getBlock())) {
            level.scheduleTick(pos, this, 20 + level.random.nextInt(20));
        }
    }

    @Override
    public void tick(BlockState s, ServerLevel sl, BlockPos pos, RandomSource rnd) {
        List<Player> near = sl.getEntitiesOfClass(Player.class,
                new AABB(pos).inflate(12), p -> !p.isSpectator() && p.isAlive());
        if (near.isEmpty()) {
            if (s.getValue(AWAKE) && rnd.nextInt(3) == 0) {
                sl.setBlock(pos, s.setValue(AWAKE, false), 3);                          // back to sleep
                sl.playSound(null, pos, SoundEvents.SOUL_SOIL_BREAK, SoundSource.BLOCKS, 0.4F, 0.6F);
            }
            sl.scheduleTick(pos, this, 40 + rnd.nextInt(80));
            return;
        }
        if (!s.getValue(AWAKE)) {
            sl.setBlock(pos, s.setValue(AWAKE, true), 3);
            sl.playSound(null, pos, SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value(), SoundSource.BLOCKS, 0.55F, 0.7F);
            souls(sl, pos, 7);
        } else {
            if (rnd.nextInt(5) == 0)
                sl.playSound(null, pos, SoundEvents.WARDEN_LISTENING, SoundSource.BLOCKS, 0.22F, 0.75F); // the hushed curse
            if (rnd.nextInt(6) == 0) souls(sl, pos, 3);
            if (rnd.nextInt(14) == 0)
                sl.playSound(null, pos, SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value(), SoundSource.BLOCKS, 0.4F, 0.85F);
        }
        sl.scheduleTick(pos, this, 30 + rnd.nextInt(50));
    }

    private static void souls(ServerLevel sl, BlockPos pos, int n) {
        sl.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, n, 0.35, 0.3, 0.35, 0.02);
    }

    @Override
    public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource rnd) {
        if (s.getValue(AWAKE)) {
            if (rnd.nextInt(6) == 0) {
                level.addParticle(ParticleTypes.SOUL, pos.getX() + 0.2 + rnd.nextDouble() * 0.6, pos.getY() + 0.55,
                        pos.getZ() + 0.2 + rnd.nextDouble() * 0.6, 0, 0.02, 0);
            }
            if (rnd.nextInt(9) == 0) {
                level.addParticle(ParticleTypes.GLOW, pos.getX() + 0.5, pos.getY() + 0.44,
                        pos.getZ() + 0.5, 0, 0.005, 0);
            }
        }
    }
}
