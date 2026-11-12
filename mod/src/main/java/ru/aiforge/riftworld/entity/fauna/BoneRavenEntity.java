package ru.aiforge.riftworld.entity.fauna;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.entity.MorokEntity;
import ru.aiforge.riftworld.registry.ModItems;

/**
 * 11. Костяной ворон ({@code bone_raven}) — падальщик-разведчик Мороковой Пади.
 *
 * <p>Строго пикирующий противник (КД 8 с, 4.5 урона, никакого непрерывного «зависания с уроном»!).
 * Если рядом ({@code <= 24} блоков) за игроком уже охотится {@link MorokEntity},
 * ворон держится высоко в небе и зловеще каркает, не мешая дуэли с хозяевами Пади!
 */
public class BoneRavenEntity extends Monster {
    private static final EntityDataAccessor<Integer> DATA_DIVE_TICKS =
            SynchedEntityData.defineId(BoneRavenEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_DEFERRING =
            SynchedEntityData.defineId(BoneRavenEntity.class, EntityDataSerializers.BOOLEAN);

    private int diveCooldown = 80;
    private boolean dealtDiveHit = false;
    /**
     * Заморозка для съёмки кадров: пока птица заморожена, она не летит,
     * не блуждает и не меняет позу. Витрина выставляет флаг, и ворон
     * остаётся ровно там, куда поставили, — иначе он уходил из кадра
     * через пару секунд.
     */
    private boolean frozen = false;
    /** Якорь замороженной птицы: она каждый тик возвращается в эту точку. */
    private double anchorX = 0.0D;
    private double anchorY = 0.0D;
    private double anchorZ = 0.0D;
    private int apexCheckTimer = 0;

    public BoneRavenEntity(EntityType<? extends BoneRavenEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FLYING_SPEED, 0.44D)
                .add(Attributes.ATTACK_DAMAGE, 4.5D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
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
        this.entityData.define(DATA_DIVE_TICKS, 0);
        this.entityData.define(DATA_DEFERRING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 16.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public int getDiveTicks() {
        return this.entityData.get(DATA_DIVE_TICKS);
    }

    /** Замораживает птицу: она перестаёт летать и бродить. */
    public void setFrozen(boolean value) {
        this.frozen = value;
        if (value) {
            this.updateAnchor();
        }
    }

    /** Обновляет якорь замороженной птицы: она держится в этой точке. */
    public void updateAnchor() {
        this.anchorX = this.getX();
        this.anchorY = this.getY();
        this.anchorZ = this.getZ();
    }

    /** Якорь замороженной птицы — в этой точке она и стоит. */
    public void setAnchor(double x, double y, double z) {
        this.anchorX = x;
        this.anchorY = y;
        this.anchorZ = z;
    }

    /** Заморожена ли птица. */
    public boolean isFrozen() {
        return this.frozen;
    }

    /** Отладочный признак: в рендерере вместо модели рисуется красный куб. */
    private boolean showcaseDebugCube = false;

    public void setShowcaseDebugCube(boolean value) {
        this.showcaseDebugCube = value;
    }

    public boolean isShowcaseDebugCube() {
        return this.showcaseDebugCube;
    }

    public boolean isDeferringToApex() {
        return this.entityData.get(DATA_DEFERRING);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        // Заморозка нужна и тем птицам, которых поставили без AI: ворон
        // каждые 35 тиков улетает на случайную точку, и без AI он улетал
        // так же — уходил из кадра за секунды.
        if (this.frozen || this.isNoAi() || this.isNoGravity()) {
            // Замороженная птица стоит на месте: ни цели, ни блуждания,
            // ни сноса гравитацией. Иначе она уходила из кадра за секунды.
            // Возвращаем её и в якорь: контроллер полёта продолжает поднимать
            // птицу каждый тик, и она уплывала вверх из кадра.
            this.setDeltaMovement(0.0D, 0.0D, 0.0D);
            this.setPos(this.anchorX, this.anchorY, this.anchorZ);
            return;
        }

        if (this.diveCooldown > 0) --this.diveCooldown;
        int dive = this.getDiveTicks();
        if (dive > 0) this.entityData.set(DATA_DIVE_TICKS, dive - 1);

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            if (--this.apexCheckTimer <= 0) {
                this.apexCheckTimer = 12;
                this.entityData.set(DATA_DEFERRING, isApexPredatorEngaged(target));
            }

            boolean defer = this.isDeferringToApex();
            if (defer) {
                this.entityData.set(DATA_DIVE_TICKS, 0);
                double angle = (this.tickCount * 0.055D) + (this.getId() * 1.7D);
                Vec3 highOrbit = target.position().add(
                        Math.cos(angle) * 7.5D, 8.5D + Math.sin(angle * 1.3D) * 1.2D, Math.sin(angle) * 7.5D);
                steerToward(highOrbit, 0.065D);
                if (this.tickCount % 70 == 0) {
                    this.playSound(SoundEvents.PARROT_IMITATE_PHANTOM, 0.9F, 0.6F);
                }
            } else if (dive > 0) {
                Vec3 aim = target.position().add(0.0D, target.getBbHeight() * 0.65D, 0.0D);
                steerToward(aim, 0.145D);
                if (!this.dealtDiveHit && this.distanceToSqr(target) <= 5.2D) {
                    this.doHurtTarget(target);
                    this.dealtDiveHit = true;
                    this.entityData.set(DATA_DIVE_TICKS, 0);
                    this.diveCooldown = 160; // strict 8s cooldown
                    Vec3 climb = this.position().subtract(target.position()).normalize().scale(0.5D);
                    this.setDeltaMovement(climb.x, 0.45D, climb.z);
                }
            } else if (this.diveCooldown <= 0 && this.distanceToSqr(target) <= 196.0D) {
                this.dealtDiveHit = false;
                this.entityData.set(DATA_DIVE_TICKS, 26);
                this.diveCooldown = 160;
                this.playSound(SoundEvents.PHANTOM_SWOOP, 0.85F, 1.35F);
            } else {
                double angle = (this.tickCount * 0.075D) + (this.getId() * 1.3D);
                Vec3 patrolOrbit = target.position().add(
                        Math.cos(angle) * 5.8D, 5.8D + Math.sin(angle * 1.5D) * 0.9D, Math.sin(angle) * 5.8D);
                steerToward(patrolOrbit, 0.07D);
            }
        } else if (this.tickCount % 35 == 0) {
            Vec3 wander = this.position().add(
                    (this.random.nextDouble() - 0.5D) * 8.0D,
                    (this.random.nextDouble() - 0.45D) * 3.0D,
                    (this.random.nextDouble() - 0.5D) * 8.0D);
            BlockPos bp = BlockPos.containing(wander);
            if (this.level().getBlockState(bp.below(2)).isSolid()) {
                wander = new Vec3(wander.x, bp.getY() + 3.0D, wander.z);
            }
            steerToward(wander, 0.055D);
        }

        Vec3 vel = this.getDeltaMovement();
        if (vel.horizontalDistanceSqr() > 0.002D) {
            float yaw = (float) (Mth.atan2(vel.z, vel.x) * (180.0D / Math.PI)) - 90.0F;
            this.setYRot(Mth.rotLerp(0.24F, this.getYRot(), yaw));
            this.yBodyRot = this.getYRot();
        }
        this.move(MoverType.SELF, vel);
        this.setDeltaMovement(this.getDeltaMovement().scale(0.87D));
    }

    private boolean isApexPredatorEngaged(LivingEntity target) {
        List<MorokEntity> moroks = this.level().getEntitiesOfClass(
                MorokEntity.class, target.getBoundingBox().inflate(24.0D), m -> m.isAlive() && m.getTarget() != null);
        if (!moroks.isEmpty()) return true;
        return false;
    }

    private void steerToward(Vec3 goal, double accel) {
        Vec3 diff = goal.subtract(this.position());
        double d = diff.length();
        if (d > 0.25D) {
            this.setDeltaMovement(this.getDeltaMovement().add(diff.scale(accel / d)));
        }
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
        this.spawnAtLocation(new ItemStack(ModItems.BONE_PINION.get(), 1 + this.random.nextInt(2 + looting)));
        if (this.random.nextBoolean()) {
            this.spawnAtLocation(new ItemStack(Items.BONE));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_IMITATE_WITHER_SKELETON;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_DEATH;
    }
}
