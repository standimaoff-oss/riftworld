package ru.aiforge.riftworld.entity.fauna;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.registry.ModItems;

/**
 * 08. Призрачный волк ({@code phantom_wolf}) — враждебный туманный хищник Туманного данжа.
 *
 * <p>В темноте и тумане полупрозрачен и использует «Прыжок из тумана» (5 урона, КД 10 с).
 * Яркий свет ({@code light >= 12}, например фонари {@code keep_cage_lantern} / {@code elisar_lantern})
 * проявляет силуэт волка и отключает туманный прыжок! «Призрачный вой» накладывает {@code Slowness I} (3 с)
 * с общим кулдауном стаи 15 с.
 */
public class PhantomWolfEntity extends Monster {
    private static final EntityDataAccessor<Integer> DATA_LEAP_TICKS =
            SynchedEntityData.defineId(PhantomWolfEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HOWL_TICKS =
            SynchedEntityData.defineId(PhantomWolfEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_REVEALED =
            SynchedEntityData.defineId(PhantomWolfEntity.class, EntityDataSerializers.BOOLEAN);

    private int leapCooldown = 40;
    private int howlCooldown = 80;

    public PhantomWolfEntity(EntityType<? extends PhantomWolfEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.1F);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 28.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 22.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_LEAP_TICKS, 0);
        this.entityData.define(DATA_HOWL_TICKS, 0);
        this.entityData.define(DATA_REVEALED, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25D, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.90D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public int getLeapTicks() {
        return this.entityData.get(DATA_LEAP_TICKS);
    }

    public int getHowlTicks() {
        return this.entityData.get(DATA_HOWL_TICKS);
    }

    public boolean isRevealedByLight() {
        return this.entityData.get(DATA_REVEALED);
    }

    public void setPackHowlCooldown(int ticks) {
        this.howlCooldown = Math.max(this.howlCooldown, ticks);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (!this.isRevealedByLight() && this.random.nextInt(6) == 0) {
                this.level().addParticle(ParticleTypes.CLOUD,
                        this.getRandomX(0.5D), this.getY() + 0.45D, this.getRandomZ(0.5D),
                        0.0D, 0.01D, 0.0D);
            }
            return;
        }

        if (this.leapCooldown > 0) --this.leapCooldown;
        if (this.howlCooldown > 0) --this.howlCooldown;
        int leap = this.getLeapTicks();
        if (leap > 0) this.entityData.set(DATA_LEAP_TICKS, leap - 1);
        int howl = this.getHowlTicks();
        if (howl > 0) this.entityData.set(DATA_HOWL_TICKS, howl - 1);

        int selfLight = this.level().getMaxLocalRawBrightness(this.blockPosition());
        LivingEntity target = this.getTarget();
        int targetLight = target != null ? this.level().getMaxLocalRawBrightness(target.blockPosition()) : 0;
        boolean revealed = selfLight >= 12 || targetLight >= 12;
        this.entityData.set(DATA_REVEALED, revealed);

        if (target != null && target.isAlive()) {
            double distSq = this.distanceToSqr(target);
            if (!revealed && this.leapCooldown <= 0 && this.onGround() && distSq >= 12.0D && distSq <= 72.0D) {
                this.leapCooldown = 200;
                this.entityData.set(DATA_LEAP_TICKS, 16);
                Vec3 dir = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D).normalize();
                this.setDeltaMovement(dir.x * 0.92D, 0.42D, dir.z * 0.92D);
                this.playSound(SoundEvents.WOLF_GROWL, 1.0F, 0.72F);
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.5D, this.getZ(),
                            14, 0.4D, 0.25D, 0.4D, 0.04D);
                }
            }

            if (this.howlCooldown <= 0 && distSq <= 81.0D) {
                this.entityData.set(DATA_HOWL_TICKS, 28);
                this.howlCooldown = 300;
                List<PhantomWolfEntity> pack = this.level().getEntitiesOfClass(
                        PhantomWolfEntity.class, this.getBoundingBox().inflate(20.0D));
                for (PhantomWolfEntity mate : pack) {
                    mate.setPackHowlCooldown(300);
                }
                this.playSound(SoundEvents.WOLF_WHINE, 1.1F, 0.62F);
                this.playSound(SoundEvents.WOLF_GROWL, 0.9F, 0.55F);
                if (distSq <= 64.0D) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0), this);
                }
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 0.9D, this.getZ(),
                            10, 0.35D, 0.35D, 0.35D, 0.03D);
                }
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (this.random.nextFloat() < 0.60F + 0.15F * looting) {
            this.spawnAtLocation(new ItemStack(ModItems.PHANTOM_FANG.get()));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WOLF_GROWL;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOLF_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOLF_DEATH;
    }
}
