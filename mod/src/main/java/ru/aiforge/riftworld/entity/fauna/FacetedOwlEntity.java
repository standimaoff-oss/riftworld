package ru.aiforge.riftworld.entity.fauna;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 10. Гранёная сова ({@code faceted_owl}) — мирный ночной страж Хрустальных разломов.
 *
 * <p>Бесшумно перелетает между хрустальными кронами и шпилями. Когда рядом с игроком скрываются
 * враждебные мобы (в радиусе 12 блоков), сова подаёт «Хрустальный сигнал» — мелодичный аккорд
 * и краткую подсветку {@code Glowing} (3 с) до 3 угроз.
 */
public class FacetedOwlEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> DATA_FLYING =
            SynchedEntityData.defineId(FacetedOwlEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SIGNAL_TICKS =
            SynchedEntityData.defineId(FacetedOwlEntity.class, EntityDataSerializers.INT);

    @Nullable
    private Vec3 flightGoal;
    private int perchTimer = 60;
    private int signalCooldown = 60;

    public FacetedOwlEntity(EntityType<? extends FacetedOwlEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 18, true);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.FLYING_SPEED, 0.38D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_FLYING, false);
        this.entityData.define(DATA_SIGNAL_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    public boolean isOwlFlying() {
        return this.entityData.get(DATA_FLYING);
    }

    public int getSignalTicks() {
        return this.entityData.get(DATA_SIGNAL_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.getSignalTicks() > 0 && this.tickCount % 3 == 0) {
                this.level().addParticle(ParticleTypes.END_ROD,
                        this.getRandomX(0.4D), this.getY() + 0.65D, this.getRandomZ(0.4D),
                        0.0D, 0.03D, 0.0D);
            }
            return;
        }

        if (this.signalCooldown > 0) --this.signalCooldown;
        int sig = this.getSignalTicks();
        if (sig > 0) this.entityData.set(DATA_SIGNAL_TICKS, sig - 1);

        // Crystal Signal: warn nearby players of hostiles within 12 blocks
        if (this.signalCooldown <= 0 && this.tickCount % 15 == 0) {
            Player nearbyPlayer = this.level().getNearestPlayer(this, 16.0D);
            if (nearbyPlayer != null && !nearbyPlayer.isSpectator()) {
                List<Monster> threats = this.level().getEntitiesOfClass(Monster.class,
                        this.getBoundingBox().inflate(12.0D), m -> m.isAlive() && !m.isInvisible());
                if (!threats.isEmpty()) {
                    this.signalCooldown = 280;
                    this.entityData.set(DATA_SIGNAL_TICKS, 36);
                    this.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.35F);
                    int count = Math.min(3, threats.size());
                    for (int i = 0; i < count; ++i) {
                        threats.get(i).addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0), this);
                    }
                    if (this.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.75D, this.getZ(),
                                12, 0.4D, 0.35D, 0.4D, 0.03D);
                    }
                }
            }
        }

        // Perch by day, glide periodically at dusk/night or when disturbed
        if (this.flightGoal != null) {
            this.setNoGravity(true);
            this.entityData.set(DATA_FLYING, true);
            Vec3 delta = this.flightGoal.subtract(this.position());
            double dist = delta.length();
            if (dist < 1.1D || --this.perchTimer <= 0) {
                this.flightGoal = null;
                this.setNoGravity(false);
                this.perchTimer = this.level().isDay() ? 140 + this.random.nextInt(140) : 50 + this.random.nextInt(60);
            } else {
                Vec3 step = delta.scale(0.065D / dist);
                this.setDeltaMovement(this.getDeltaMovement().scale(0.84D).add(step));
                Vec3 vel = this.getDeltaMovement();
                if (vel.horizontalDistanceSqr() > 0.002D) {
                    float yaw = (float) (Mth.atan2(vel.z, vel.x) * (180.0D / Math.PI)) - 90.0F;
                    this.setYRot(Mth.rotLerp(0.2F, this.getYRot(), yaw));
                    this.yBodyRot = this.getYRot();
                }
                this.move(MoverType.SELF, vel);
            }
        } else {
            this.setNoGravity(false);
            this.entityData.set(DATA_FLYING, !this.onGround());
            if (!this.onGround() && this.getDeltaMovement().y < 0.0D) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.65D, 1.0D));
            }
            if (--this.perchTimer <= 0) {
                Vec3 candidate = this.position().add(
                        (this.random.nextDouble() - 0.5D) * 12.0D,
                        1.5D + this.random.nextDouble() * 4.0D,
                        (this.random.nextDouble() - 0.5D) * 12.0D);
                BlockPos bp = BlockPos.containing(candidate);
                if (this.level().isEmptyBlock(bp)) {
                    this.flightGoal = candidate;
                    this.perchTimer = 90;
                } else {
                    this.perchTimer = 40;
                }
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean res = super.hurt(source, amount);
        if (res && !this.level().isClientSide) {
            this.flightGoal = this.position().add(
                    (this.random.nextDouble() - 0.5D) * 10.0D,
                    3.5D + this.random.nextDouble() * 2.5D,
                    (this.random.nextDouble() - 0.5D) * 10.0D);
            this.perchTimer = 80;
        }
        return res;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }
}
