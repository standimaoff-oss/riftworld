package ru.aiforge.riftworld.entity;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * v0.34.0+ — <b>Пегас</b> ({@code riftworld:pegasus}): крылатый скакун «земля/воздух»
 * с полным набором биомеханических анимаций (покой, пастьба, 4-тактный шаг, галоп,
 * боевая свеча, плавное раскрытие/складывание 37-костных крыльев при взлёте и посадке,
 * активный машущий полёт, планирование с креном на виражах и зависание в воздухе).
 */
public class PegasusEntity extends Animal {
    private static final EntityDataAccessor<Boolean> DATA_TAME =
            SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SADDLE =
            SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_FLYING =
            SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_REARING =
            SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_GRAZING =
            SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SHOWCASE_POSE =
            SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.INT);

    public static final int POSE_LIVE = 0;
    public static final int POSE_GROUND_FOLDED = 1;
    public static final int POSE_WALK = 2;
    public static final int POSE_GALLOP = 3;
    public static final int POSE_REAR = 4;
    public static final int POSE_GRAZE = 5;
    public static final int POSE_FLY_FLAP = 6;
    public static final int POSE_FLY_GLIDE = 7;
    public static final int POSE_FLY_HOVER = 8;

    /** Круизная скорость полёта (блоков за тик). Ровно вдвое медленнее дракона — это конь, а не дракон. */
    private static final double CRUISE = 0.62D;
    private static final double SPRINT_CRUISE = 0.92D;

    private int rearServerTicks;
    private int grazeServerTicks;

    private float prevFlyProgress;
    private float flyProgress;
    private float prevGlideProgress;
    private float glideProgress;
    private float prevHoverProgress;
    private float hoverProgress;
    private float prevRearProgress;
    private float rearProgress;
    private float prevGrazeProgress;
    private float grazeProgress;
    private float prevRollAngle;
    private float rollAngle;
    private float renderPartialTick = 1.0F;

    public PegasusEntity(EntityType<? extends PegasusEntity> type, Level level) {
        super(type, level);
        this.xpReward = 18;
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 42.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.JUMP_STRENGTH, 0.7D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_TAME, false);
        this.entityData.define(DATA_SADDLE, false);
        this.entityData.define(DATA_FLYING, false);
        this.entityData.define(DATA_REARING, false);
        this.entityData.define(DATA_GRAZING, false);
        this.entityData.define(DATA_SHOWCASE_POSE, POSE_LIVE);
    }

    public boolean isTamed() {
        return this.entityData.get(DATA_TAME);
    }

    public boolean hasSaddle() {
        return this.entityData.get(DATA_SADDLE);
    }

    public boolean isFlying() {
        return this.entityData.get(DATA_FLYING);
    }

    public boolean isRearing() {
        return this.entityData.get(DATA_REARING);
    }

    public boolean isGrazing() {
        return this.entityData.get(DATA_GRAZING);
    }

    public int getShowcasePose() {
        return this.entityData.get(DATA_SHOWCASE_POSE);
    }

    public void setTamed(boolean v) {
        this.entityData.set(DATA_TAME, v);
    }

    public void setSaddled(boolean v) {
        this.entityData.set(DATA_SADDLE, v);
    }

    public void setFlyingState(boolean v) {
        setFlying(v);
        if (v) {
            this.prevFlyProgress = 1.0F;
            this.flyProgress = 1.0F;
        }
    }

    public void setShowcasePose(int pose) {
        this.entityData.set(DATA_SHOWCASE_POSE, pose);
        if (pose == POSE_FLY_FLAP || pose == POSE_FLY_GLIDE || pose == POSE_FLY_HOVER) {
            setFlying(true);
            this.prevFlyProgress = 1.0F;
            this.flyProgress = 1.0F;
        } else if (pose == POSE_REAR) {
            setFlying(false);
            this.entityData.set(DATA_REARING, true);
            this.prevRearProgress = 1.0F;
            this.rearProgress = 1.0F;
        } else if (pose == POSE_GRAZE) {
            setFlying(false);
            this.entityData.set(DATA_GRAZING, true);
            this.prevGrazeProgress = 1.0F;
            this.grazeProgress = 1.0F;
        } else if (pose != POSE_LIVE) {
            setFlying(false);
            this.prevFlyProgress = 0.0F;
            this.flyProgress = 0.0F;
        }
    }

    public void triggerRear(int ticks) {
        this.rearServerTicks = Math.max(this.rearServerTicks, ticks);
        this.grazeServerTicks = 0;
        this.entityData.set(DATA_GRAZING, false);
        this.entityData.set(DATA_REARING, true);
    }

    public void triggerGraze(int ticks) {
        if (!isFlying() && this.rearServerTicks <= 0) {
            this.grazeServerTicks = Math.max(this.grazeServerTicks, ticks);
            this.entityData.set(DATA_GRAZING, true);
        }
    }

    public void setRenderPartialTick(float partialTick) {
        this.renderPartialTick = partialTick;
    }

    public float getRenderPartialTick() {
        return this.renderPartialTick;
    }

    public float getFlyProgress(float partialTick) {
        return Mth.lerp(partialTick, this.prevFlyProgress, this.flyProgress);
    }

    public float getGlideProgress(float partialTick) {
        return Mth.lerp(partialTick, this.prevGlideProgress, this.glideProgress);
    }

    public float getHoverProgress(float partialTick) {
        return Mth.lerp(partialTick, this.prevHoverProgress, this.hoverProgress);
    }

    public float getRearProgress(float partialTick) {
        return Mth.lerp(partialTick, this.prevRearProgress, this.rearProgress);
    }

    public float getGrazeProgress(float partialTick) {
        return Mth.lerp(partialTick, this.prevGrazeProgress, this.grazeProgress);
    }

    public float getRollAngle(float partialTick) {
        return Mth.lerp(partialTick, this.prevRollAngle, this.rollAngle);
    }

    private void setFlying(boolean v) {
        this.entityData.set(DATA_FLYING, v);
        this.setNoGravity(v);
        if (v) {
            this.rearServerTicks = 0;
            this.grazeServerTicks = 0;
            this.entityData.set(DATA_REARING, false);
            this.entityData.set(DATA_GRAZING, false);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Tamed", isTamed());
        tag.putBoolean("Saddle", hasSaddle());
        tag.putInt("ShowcasePose", getShowcasePose());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_TAME, tag.getBoolean("Tamed"));
        this.entityData.set(DATA_SADDLE, tag.getBoolean("Saddle"));
        setFlying(false);
        if (tag.contains("ShowcasePose")) {
            setShowcasePose(tag.getInt("ShowcasePose"));
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.45D));
        this.goalSelector.addGoal(2, new TemptGoal(this, 1.15D,
                Ingredient.of(Items.APPLE, Items.GOLDEN_APPLE), false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.APPLE) || stack.is(Items.GOLDEN_APPLE);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isTamed()) {
            if (isFood(stack)) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                if (!this.level().isClientSide) {
                    if (this.random.nextFloat() < 0.45F) {
                        this.entityData.set(DATA_TAME, true);
                        this.level().broadcastEntityEvent(this, (byte) 7);
                    } else {
                        triggerRear(32);
                        this.playSound(SoundEvents.HORSE_ANGRY, 0.9F, 1.05F);
                        this.level().broadcastEntityEvent(this, (byte) 6);
                    }
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            return super.mobInteract(player, hand);
        }
        if (!hasSaddle() && stack.is(Items.SADDLE)) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            this.entityData.set(DATA_SADDLE, true);
            this.playSound(SoundEvents.HORSE_SADDLE, 0.7F, 1.0F);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (hasSaddle() && !this.isVehicle() && !player.isSecondaryUseActive()) {
            if (!this.level().isClientSide) player.startRiding(this);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean res = super.hurt(source, amount);
        if (res && !this.level().isClientSide) {
            this.grazeServerTicks = 0;
            this.entityData.set(DATA_GRAZING, false);
            if (!isFlying() && this.onGround() && !this.isVehicle() && this.random.nextFloat() < 0.45F) {
                triggerRear(24);
            }
        }
        return res;
    }

    @Override
    public boolean canBeCollidedWith() {
        return !isFlying();
    }

    @Override
    public boolean isPushable() {
        return !isFlying() && super.isPushable();
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected boolean canAddPassenger(net.minecraft.world.entity.Entity passenger) {
        return this.getPassengers().isEmpty();
    }

    @Override
    public double getPassengersRidingOffset() {
        // v0.45.3: the rig now renders at 0.48, so the saddle sits ~1.2 blocks up.
        return 1.22D;
    }

    @Override
    @Nullable
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        updateAnimationStates();

        if (this.level().isClientSide) return;

        int pose = getShowcasePose();
        if (pose != POSE_LIVE || this.isNoAi()) {
            return;
        }

        // Server-side autonomous ground behaviors: rear timer & peaceful Elisar pasture grazing
        Vec3 dm = this.getDeltaMovement();
        double horizSq = dm.x * dm.x + dm.z * dm.z;

        if (this.rearServerTicks > 0) {
            this.rearServerTicks--;
            this.getNavigation().stop();
            if (this.rearServerTicks <= 0) {
                this.entityData.set(DATA_REARING, false);
            }
        } else if (this.grazeServerTicks > 0) {
            if (isFlying() || this.isVehicle() || horizSq > 0.0025D) {
                this.grazeServerTicks = 0;
                this.entityData.set(DATA_GRAZING, false);
            } else {
                this.grazeServerTicks--;
                if (this.grazeServerTicks <= 0) {
                    this.entityData.set(DATA_GRAZING, false);
                }
            }
        } else if (!isFlying() && this.onGround() && !this.isVehicle() && horizSq < 0.0009D) {
            if (this.random.nextInt(240) == 0) {
                triggerGraze(55 + this.random.nextInt(35));
            } else if (this.random.nextInt(720) == 0) {
                triggerRear(28);
                this.playSound(SoundEvents.HORSE_BREATHE, 0.7F, 1.05F);
            }
        }

        // питомец без всадника медленно опускается и садится на землю
        if (isFlying() && !this.isVehicle()) {
            Vec3 v = dm.scale(0.86).add(0, -0.06, 0);
            setDeltaMovement(v);
            move(MoverType.SELF, v);
            if (this.onGround()) setFlying(false);
        }
    }

    private void updateAnimationStates() {
        this.prevFlyProgress = this.flyProgress;
        this.prevGlideProgress = this.glideProgress;
        this.prevHoverProgress = this.hoverProgress;
        this.prevRearProgress = this.rearProgress;
        this.prevGrazeProgress = this.grazeProgress;
        this.prevRollAngle = this.rollAngle;

        int pose = getShowcasePose();
        if (pose != POSE_LIVE) {
            this.flyProgress = (pose == POSE_FLY_FLAP || pose == POSE_FLY_GLIDE || pose == POSE_FLY_HOVER) ? 1.0F : 0.0F;
            this.glideProgress = (pose == POSE_FLY_GLIDE) ? 1.0F : 0.0F;
            this.hoverProgress = (pose == POSE_FLY_HOVER) ? 1.0F : 0.0F;
            this.rearProgress = (pose == POSE_REAR) ? 1.0F : 0.0F;
            this.grazeProgress = (pose == POSE_GRAZE) ? 1.0F : 0.0F;
            this.rollAngle = (pose == POSE_FLY_GLIDE) ? 12.0F : 0.0F;
            return;
        }

        boolean airborne = isFlying() || (!this.onGround() && this.fallDistance > 1.2F);
        this.flyProgress = approach(this.flyProgress, airborne ? 1.0F : 0.0F, 0.11F);

        Vec3 dm = this.getDeltaMovement();
        double horizSpeed = Math.hypot(dm.x, dm.z);
        float targetGlide = 0.0F;
        float targetHover = 0.0F;
        if (airborne) {
            if (horizSpeed < 0.09D && Math.abs(dm.y) < 0.12D) {
                targetHover = 1.0F;
            } else if (dm.y < -0.04D || (horizSpeed > 0.45D && dm.y <= 0.03D)) {
                targetGlide = dm.y < -0.08D ? 1.0F : 0.62F;
            }
        }
        this.glideProgress = approach(this.glideProgress, targetGlide, 0.09F);
        this.hoverProgress = approach(this.hoverProgress, targetHover, 0.10F);

        this.rearProgress = approach(this.rearProgress, isRearing() ? 1.0F : 0.0F, 0.13F);
        this.grazeProgress = approach(this.grazeProgress, isGrazing() ? 1.0F : 0.0F, 0.08F);

        float yawDelta = Mth.wrapDegrees(this.getYRot() - this.yRotO);
        float targetRoll = airborne ? Mth.clamp(-yawDelta * 2.6F, -24.0F, 24.0F) : 0.0F;
        this.rollAngle = Mth.lerp(0.25F, this.rollAngle, targetRoll);
    }

    private static float approach(float current, float target, float step) {
        return current < target ? Math.min(target, current + step) : Math.max(target, current - step);
    }

    @Override
    public void travel(Vec3 input) {
        if (!this.isAlive() || !(getControllingPassenger() instanceof Player rider)) {
            super.travel(input);
            return;
        }
        // ---- управление мышкой: куда смотрит всадник, туда и летим
        this.yRotO = this.getYRot();
        this.setYRot(rider.getYRot());
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
        this.setXRot(rider.getXRot() * 0.5F);
        Vec3 look = rider.getLookAngle();
        double cruise = rider.isSprinting() ? SPRINT_CRUISE : CRUISE;
        double vy = look.y * cruise * 1.25D;
        if (rider.isShiftKeyDown()) vy = -0.55D;                 // быстрое снижение
        double yawRad = Math.toRadians(rider.getYRot());
        double vx = -Math.sin(yawRad) * cruise;
        double vz = Math.cos(yawRad) * cruise;
        setFlying(true);
        Vec3 v = new Vec3(vx, vy, vz);
        setDeltaMovement(v);
        move(MoverType.SELF, v);
        this.fallDistance = 0.0F;
        // мягкая посадка: над землёй гасим снижение
        if (this.onGround() && vy <= 0) {
            setFlying(false);
            setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return isFlying() ? false : super.causeFallDamage(distance, multiplier, source);
    }

    /** Дроп: 1–3 пера пегаса (лёгкие, дают медленное падение в крафтах будущих версий). */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        int n = 1 + this.random.nextInt(3) + looting;
        for (int i = 0; i < n; ++i) {
            this.spawnAtLocation(new ItemStack(ru.aiforge.riftworld.registry.ModItems.PEGASUS_FEATHER.get()));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.HORSE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.HORSE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HORSE_DEATH;
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(SoundEvents.HORSE_STEP, 0.15F, 1.0F);
    }
}
