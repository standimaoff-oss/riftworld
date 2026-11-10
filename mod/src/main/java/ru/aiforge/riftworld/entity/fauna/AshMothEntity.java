package ru.aiforge.riftworld.entity.fauna;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.registry.ModItems;

/**
 * 03. Пепельный мотылёк ({@code ash_moth}) — ночной летучий моб Пепельных земель.
 *
 * <p>Тянется к фонарям, факелам и лаве. В темноте или ночью пикирует на игрока (3 урона) и выпускает
 * облако тлеющего пепла с коротким эффектом {@code Darkness} (не ломает спринт игрока!).
 */
public class AshMothEntity extends Monster {
    private static final EntityDataAccessor<Integer> DATA_SWOOP_TICKS =
            SynchedEntityData.defineId(AshMothEntity.class, EntityDataSerializers.INT);

    @Nullable
    private BlockPos beaconPos;
    private int beaconSearchCd = 20;
    private int swoopCooldown = 0;
    private int ashPuffCooldown = 0;

    public AshMothEntity(EntityType<? extends AshMothEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FLYING_SPEED, 0.42D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
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
        this.entityData.define(DATA_SWOOP_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 10.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                target -> !this.level().isDay() || this.level().getMaxLocalRawBrightness(target.blockPosition()) <= 9));
    }

    public int getSwoopTicks() {
        return this.entityData.get(DATA_SWOOP_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.random.nextInt(5) == 0) {
                this.level().addParticle(ParticleTypes.ASH,
                        this.getRandomX(0.65D), this.getRandomY(), this.getRandomZ(0.65D),
                        0.0D, -0.02D, 0.0D);
            }
            return;
        }

        if (this.swoopCooldown > 0) --this.swoopCooldown;
        if (this.ashPuffCooldown > 0) --this.ashPuffCooldown;
        int swoop = this.getSwoopTicks();
        if (swoop > 0) this.entityData.set(DATA_SWOOP_TICKS, swoop - 1);

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            double distSq = this.distanceToSqr(target);
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (this.swoopCooldown <= 0 && distSq <= 64.0D) {
                Vec3 goal = target.position().add(0.0D, target.getBbHeight() * 0.65D, 0.0D);
                Vec3 delta = goal.subtract(this.position());
                double len = Math.max(0.001D, delta.length());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.65D).add(delta.scale(0.14D / len)));
                this.entityData.set(DATA_SWOOP_TICKS, 14);
                if (distSq <= 3.1D) {
                    this.doHurtTarget(target);
                    this.swoopCooldown = 48;
                    Vec3 climb = this.position().subtract(target.position()).normalize().scale(0.45D);
                    this.setDeltaMovement(climb.x, 0.34D, climb.z);
                }
            } else {
                double angle = (this.tickCount * 0.08D) + (this.getId() * 1.3D);
                Vec3 orbit = target.position().add(Math.cos(angle) * 3.8D, 2.4D + Math.sin(angle * 1.5D) * 0.6D, Math.sin(angle) * 3.8D);
                steerToward(orbit, 0.075D);
            }
        } else {
            if (--this.beaconSearchCd <= 0) {
                this.beaconSearchCd = 90 + this.random.nextInt(60);
                this.beaconPos = ElisarFaunaUtil.findNearestBlock(
                        this.level(), this.blockPosition(), 10, 5, 10, ElisarFaunaUtil::isWarmBeacon);
            }
            if (this.beaconPos != null && ElisarFaunaUtil.isWarmBeacon(this.level().getBlockState(this.beaconPos))) {
                double a = (this.tickCount * 0.09D) + (this.getId() * 0.9D);
                Vec3 orbit = Vec3.atCenterOf(this.beaconPos).add(Math.cos(a) * 2.2D, 0.9D + Math.sin(a * 2.0D) * 0.45D, Math.sin(a) * 2.2D);
                steerToward(orbit, 0.06D);
            } else if (this.tickCount % 35 == 0) {
                Vec3 wander = this.position().add(
                        (this.random.nextDouble() - 0.5D) * 6.0D,
                        (this.random.nextDouble() - 0.45D) * 3.0D,
                        (this.random.nextDouble() - 0.5D) * 6.0D);
                steerToward(keepAboveGround(wander), 0.055D);
            }
        }

        Vec3 vel = this.getDeltaMovement();
        if (vel.horizontalDistanceSqr() > 0.002D) {
            float targetYaw = (float) (Mth.atan2(vel.z, vel.x) * (180.0D / Math.PI)) - 90.0F;
            this.setYRot(Mth.rotLerp(0.22F, this.getYRot(), targetYaw));
            this.yBodyRot = this.getYRot();
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.88D));
    }

    private Vec3 keepAboveGround(Vec3 candidate) {
        BlockPos bp = BlockPos.containing(candidate);
        if (this.level().getBlockState(bp.below()).isSolid()) {
            return new Vec3(candidate.x, bp.getY() + 1.8D, candidate.z);
        }
        return candidate;
    }

    private void steerToward(Vec3 target, double accel) {
        Vec3 diff = target.subtract(this.position());
        double d = diff.length();
        if (d > 0.25D) {
            this.setDeltaMovement(this.getDeltaMovement().add(diff.scale(accel / d)));
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living && this.ashPuffCooldown <= 0) {
            releaseAshPuff(living);
        }
        return hit;
    }

    private void releaseAshPuff(LivingEntity target) {
        this.ashPuffCooldown = 140;
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 70, 0), this);
        this.playSound(SoundEvents.FIRE_EXTINGUISH, 0.7F, 1.35F);
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.35D, this.getZ(),
                    14, 0.45D, 0.35D, 0.45D, 0.03D);
            sl.sendParticles(ParticleTypes.ASH, this.getX(), this.getY() + 0.35D, this.getZ(),
                    22, 0.65D, 0.45D, 0.65D, 0.05D);
            sl.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.35D, this.getZ(),
                    6, 0.35D, 0.25D, 0.35D, 0.02D);
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypeTags.IS_FIRE)) return true;
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        int count = 1 + this.random.nextInt(2 + looting);
        this.spawnAtLocation(new ItemStack(ModItems.ASH_WING_DUST.get(), count));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BAT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BAT_DEATH;
    }
}
