package ru.aiforge.riftworld.entity;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
 * v0.34.0 — <b>Пегас</b> ({@code riftworld:pegasus}): крылатый скакун «земля/воздух».
 *
 * <p>По референсу {@code reports/mob_refs/04_ref_pegasus.png} и роадмапу
 * {@code reports/ROADMAP_v0.31_plus.md}:
 * <ul>
 *   <li>живёт в живых биомах Элисара (Аметистовые холмы, Висячие сады, Роща Сизо);</li>
 *   <li>приручается яблоками, затем надевается седло — <b>всадник сидит на седле</b>;</li>
 *   <li>управление <b>мышкой</b>: пегас летит туда, куда смотрит всадник
 *       (наклон вниз — снижение, вверх — набор высоты), Shift — быстрое снижение;</li>
 *   <li>никаких надписей в чате — только звуки, как и у дракона после v0.30.0.</li>
 * </ul>
 * Физика считается на сервере (движение транспорта синхронизируется со всадником),
 * поэтому управление одинаково работает в одиночной игре и на сервере.
 */
public class PegasusEntity extends Animal {
    private static final EntityDataAccessor<Boolean> DATA_TAME =
            SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SADDLE =
            SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_FLYING =
            SynchedEntityData.defineId(PegasusEntity.class, EntityDataSerializers.BOOLEAN);

    /** Круизная скорость полёта (блоков за тик). Ровно вдвое медленнее дракона — это конь, а не дракон. */
    private static final double CRUISE = 0.62D;
    private static final double SPRINT_CRUISE = 0.92D;

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

    private void setFlying(boolean v) {
        this.entityData.set(DATA_FLYING, v);
        this.setNoGravity(v);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Tamed", isTamed());
        tag.putBoolean("Saddle", hasSaddle());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_TAME, tag.getBoolean("Tamed"));
        this.entityData.set(DATA_SADDLE, tag.getBoolean("Saddle"));
        setFlying(false);
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
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (hasSaddle() && !this.isVehicle() && !player.isSecondaryUseActive()) {
            if (!this.level().isClientSide) player.startRiding(this);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
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
        if (this.level().isClientSide) return;
        // питомец без всадника медленно опускается и садится на землю
        if (isFlying() && !this.isVehicle()) {
            Vec3 v = this.getDeltaMovement().scale(0.86).add(0, -0.06, 0);
            setDeltaMovement(v);
            move(MoverType.SELF, v);
            if (this.onGround()) setFlying(false);
        }
    }

    @Override
    public void travel(Vec3 input) {
        if (!this.isAlive() || !(getControllingPassenger() instanceof Player rider)) {
            super.travel(input);
            return;
        }
        // ---- управление мышкой: куда смотрит всадник, туда и летим
        this.setYRot(rider.getYRot());
        this.yRotO = this.getYRot();
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
