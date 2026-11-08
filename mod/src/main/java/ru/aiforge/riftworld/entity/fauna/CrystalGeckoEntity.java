package ru.aiforge.riftworld.entity.fauna;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 09. Хрустальный геккон ({@code crystal_gecko}) — нейтральная ящерица Хрустальных разломов.
 *
 * <p>Взбирается по кристальным стенам и колоннам (до 5 блоков над опорой, чтобы не застревать под потолком),
 * маскируется в полупрозрачный кристальный силуэт в покое у кристаллов и при атаке отбрасывает ударом хвоста (3 урона).
 */
public class CrystalGeckoEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> DATA_CLIMBING =
            SynchedEntityData.defineId(CrystalGeckoEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CAMOUFLAGED =
            SynchedEntityData.defineId(CrystalGeckoEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_TAIL_WHIP_TICKS =
            SynchedEntityData.defineId(CrystalGeckoEntity.class, EntityDataSerializers.INT);

    public CrystalGeckoEntity(EntityType<? extends CrystalGeckoEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0F);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_CLIMBING, false);
        this.entityData.define(DATA_CAMOUFLAGED, false);
        this.entityData.define(DATA_TAIL_WHIP_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25D, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.92D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 7.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    public boolean isClimbingWall() {
        return this.entityData.get(DATA_CLIMBING);
    }

    public boolean isCamouflaged() {
        return this.entityData.get(DATA_CAMOUFLAGED);
    }

    public int getTailWhipTicks() {
        return this.entityData.get(DATA_TAIL_WHIP_TICKS);
    }

    @Override
    public boolean onClimbable() {
        return this.isClimbingWall();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            int whip = this.getTailWhipTicks();
            if (whip > 0) this.entityData.set(DATA_TAIL_WHIP_TICKS, whip - 1);

            boolean allowClimb = this.horizontalCollision && withinFiveBlocksOfGround();
            this.entityData.set(DATA_CLIMBING, allowClimb);

            if (this.tickCount % 8 == 0) {
                boolean still = this.getDeltaMovement().horizontalDistanceSqr() < 0.0008D && this.getTarget() == null;
                boolean onCrystal = still && nearCrystalSurface();
                this.entityData.set(DATA_CAMOUFLAGED, onCrystal);
            }
        }
    }

    private boolean withinFiveBlocksOfGround() {
        BlockPos pos = this.blockPosition();
        for (int dy = 1; dy <= 5; ++dy) {
            if (this.level().getBlockState(pos.below(dy)).isSolid()) {
                return true;
            }
        }
        return false;
    }

    private boolean nearCrystalSurface() {
        BlockPos pos = this.blockPosition();
        if (ElisarFaunaUtil.isCrystalSurface(this.level().getBlockState(pos.below()))) return true;
        return ElisarFaunaUtil.findNearestBlock(this.level(), pos, 1, 1, 1, ElisarFaunaUtil::isCrystalSurface) != null;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        this.entityData.set(DATA_TAIL_WHIP_TICKS, 14);
        this.entityData.set(DATA_CAMOUFLAGED, false);
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            Vec3 push = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D).normalize().scale(0.68D);
            living.push(push.x, 0.22D, push.z);
            this.playSound(SoundEvents.AMETHYST_CLUSTER_HIT, 0.9F, 1.25F);
        }
        return hit;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        if (distance <= 6.0F) return false;
        return super.causeFallDamage(distance - 5.0F, multiplier * 0.5F, source);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.AMETHYST_CLUSTER_STEP;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_CLUSTER_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AMETHYST_CLUSTER_BREAK;
    }
}
