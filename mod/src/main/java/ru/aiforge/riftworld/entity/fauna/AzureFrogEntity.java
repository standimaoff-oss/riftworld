package ru.aiforge.riftworld.entity.fauna;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 06. Лазурная лягушка ({@code azure_frog}) — мирная амфибия Висячих садов.
 *
 * <p>Совершает высокие дуговые прыжки по замшелым уступам и у водопадов, не задыхается под водой,
 * почти не получает урона от падения и периодически щёлкает языком за спорами в воздухе.
 */
public class AzureFrogEntity extends PathfinderMob {
    public static final int TONGUE_SNAP_TICKS = 14;
    private static final EntityDataAccessor<Integer> DATA_TONGUE_TICKS =
            SynchedEntityData.defineId(AzureFrogEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_LEAPING =
            SynchedEntityData.defineId(AzureFrogEntity.class, EntityDataSerializers.BOOLEAN);

    private int prevTongueTicks;
    private int hopCooldown = 50;
    private int tongueCooldown = 140;

    public AzureFrogEntity(EntityType<? extends AzureFrogEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.1F);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_TONGUE_TICKS, 0);
        this.entityData.define(DATA_LEAPING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.35D));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.90D, 50));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 7.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    public int getTongueTicks() {
        return this.entityData.get(DATA_TONGUE_TICKS);
    }

    public boolean isLeaping() {
        return this.entityData.get(DATA_LEAPING);
    }

    public float getTongueProgress(float partialTick) {
        float cur = Mth.lerp(partialTick, this.prevTongueTicks, this.getTongueTicks());
        return Mth.clamp(Mth.sin((cur / TONGUE_SNAP_TICKS) * (float) Math.PI), 0.0F, 1.0F);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public void tick() {
        this.prevTongueTicks = this.getTongueTicks();
        super.tick();
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_LEAPING, !this.onGround() && !ElisarFaunaUtil.isSubmergedInAnyWater(this));
            int t = this.getTongueTicks();
            if (t > 0) this.entityData.set(DATA_TONGUE_TICKS, t - 1);

            if (this.onGround() && --this.hopCooldown <= 0) {
                this.hopCooldown = 65 + this.random.nextInt(80);
                Vec3 dir = Vec3.directionFromRotation(0.0F, this.getYRot()).scale(0.38D);
                this.setDeltaMovement(dir.x, 0.56D, dir.z);
                this.playSound(SoundEvents.FROG_LONG_JUMP, 0.85F, 1.15F + this.random.nextFloat() * 0.2F);
            }

            if (this.onGround() && --this.tongueCooldown <= 0) {
                this.tongueCooldown = 160 + this.random.nextInt(140);
                this.entityData.set(DATA_TONGUE_TICKS, TONGUE_SNAP_TICKS);
                this.playSound(SoundEvents.FROG_TONGUE, 0.8F, 1.2F);
                if (this.level() instanceof ServerLevel sl) {
                    Vec3 front = this.position().add(Vec3.directionFromRotation(0.0F, this.getYRot()).scale(0.75D));
                    sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                            front.x, front.y + 0.35D, front.z, 6, 0.2D, 0.2D, 0.2D, 0.01D);
                }
            }
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        if (distance <= 8.0F) return false;
        return super.causeFallDamage(distance - 7.0F, multiplier * 0.45F, source);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FROG_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FROG_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FROG_DEATH;
    }
}
