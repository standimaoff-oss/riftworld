package ru.aiforge.riftworld.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ru.aiforge.riftworld.registry.ModEntities;

/** The dropped boss scythe: never despawns, fireproof, lies as a large glowing blade with drifting embers. */
public class ScytheItemEntity extends ItemEntity {
    public ScytheItemEntity(EntityType<? extends ItemEntity> type, Level level) {
        super(type, level);
        setUnlimitedLifetime();
    }

    public ScytheItemEntity(Level level, double x, double y, double z, ItemStack stack) {
        this(ModEntities.SCYTHE_ITEM.get(), level);
        setPos(x, y, z);
        setItem(stack);
        setYRot(random.nextFloat() * 360.0F);
    }

    private net.minecraft.core.BlockPos lit;

    /** A real light block under the blade: the dropped scythe lights the floor around it (works with shaders too). */
    private void clearLight() {
        if (lit != null && level().getBlockState(lit).is(net.minecraft.world.level.block.Blocks.LIGHT)) level().removeBlock(lit, false);
        lit = null;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) clearLight();
        super.remove(reason);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && (tickCount % 10 == 0 || lit == null)) {
            net.minecraft.core.BlockPos p = blockPosition();
            if (!p.equals(lit)) {
                clearLight();
                if (level().getBlockState(p).isAir()) {
                    level().setBlock(p, net.minecraft.world.level.block.Blocks.LIGHT.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 13), 3);
                    lit = p;
                }
            }
        }
        if (level().isClientSide && random.nextInt(3) == 0) {
            double a = getYRot() * Math.PI / 180.0, u = (random.nextDouble() - 0.3) * 2.2;
            level().addParticle(random.nextInt(4) == 0 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME,
                    getX() + Math.cos(a) * u * 0.6, getY() + 0.1, getZ() + Math.sin(a) * u * 0.6, 0, 0.02, 0);
        }
    }

    @Override
    public boolean fireImmune() {
        return true;
    }
}
