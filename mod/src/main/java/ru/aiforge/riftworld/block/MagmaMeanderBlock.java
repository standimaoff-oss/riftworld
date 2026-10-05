package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** «Лавовая извилина» - glowing molten rock of the Ashen Lands' meanders: you sink in a little, it burns and slows. */
public class MagmaMeanderBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);

    public MagmaMeanderBlock(Properties p) {
        super(p);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    public void entityInside(BlockState s, Level level, BlockPos pos, Entity e) {
        e.makeStuckInBlock(s, new Vec3(0.6, 0.8, 0.6));
        if (!level.isClientSide && e instanceof LivingEntity le && !le.fireImmune()) {
            if (le.hurt(level.damageSources().hotFloor(), 3.0F)) le.setSecondsOnFire(4);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState s, Entity e) {
        if (!level.isClientSide && e instanceof LivingEntity le && !le.fireImmune()) le.hurt(level.damageSources().hotFloor(), 2.0F);
        super.stepOn(level, pos, s, e);
    }

    @Override
    public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(24) == 0) {
            double x = pos.getX() + r.nextDouble(), z = pos.getZ() + r.nextDouble();
            level.addParticle(ParticleTypes.LAVA, x, pos.getY() + 0.8, z, 0, 0, 0);
            if (r.nextInt(4) == 0) level.playLocalSound(x, pos.getY() + 0.8, z, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.3F, 0.8F + r.nextFloat() * 0.2F, false);
        }
        if (r.nextInt(10) == 0) level.addParticle(ParticleTypes.SMOKE, pos.getX() + r.nextDouble(), pos.getY() + 0.8, pos.getZ() + r.nextDouble(), 0, 0.03, 0);
    }
}
