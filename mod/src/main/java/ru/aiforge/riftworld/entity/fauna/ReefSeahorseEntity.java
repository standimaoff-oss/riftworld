package ru.aiforge.riftworld.entity.fauna;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.registry.ModFluids;
import ru.aiforge.riftworld.registry.ModItems;

/**
 * 13. Рифовый морской конёк ({@code reef_seahorse}) — мирный обитатель коралловых рифов Элисара.
 *
 * <p>Плавает вертикально небольшими стайками (2–4 особи) возле кораллов в воде Элисара и обычной воде,
 * цепляется хвостом за ветви кораллов в покое, ловится в ведро ({@link Bucketable}) и НЕ испускает пузырьков.
 */
public class ReefSeahorseEntity extends PathfinderMob implements Bucketable {
    private static final EntityDataAccessor<Boolean> DATA_FROM_BUCKET =
            SynchedEntityData.defineId(ReefSeahorseEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ANCHORED =
            SynchedEntityData.defineId(ReefSeahorseEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private Vec3 driftTarget;
    private int retargetTimer = 20;

    public ReefSeahorseEntity(EntityType<? extends ReefSeahorseEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_FROM_BUCKET, false);
        this.entityData.define(DATA_ANCHORED, false);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || this.fromBucket();
    }

    @Override
    public boolean removeWhenFarAway(double distSq) {
        return !this.fromBucket() && !this.hasCustomName();
    }

    public boolean isAnchoredToCoral() {
        return this.entityData.get(DATA_ANCHORED);
    }

    @Override
    public void tick() {
        super.tick();
        boolean inWater = ElisarFaunaUtil.isSubmergedInAnyWater(this);
        this.setNoGravity(inWater);

        if (this.level().isClientSide) return;

        if (inWater) {
            this.setAirSupply(this.getMaxAirSupply());
            if (--this.retargetTimer <= 0) {
                this.retargetTimer = 45 + this.random.nextInt(45);
                BlockPos coral = ElisarFaunaUtil.findNearestBlock(
                        this.level(), this.blockPosition(), 6, 3, 6, ElisarFaunaUtil::isReefCoral);
                if (coral != null && coral.distSqr(this.blockPosition()) <= 2.5D && this.random.nextFloat() < 0.45F) {
                    this.entityData.set(DATA_ANCHORED, true);
                    this.driftTarget = null;
                } else {
                    this.entityData.set(DATA_ANCHORED, false);
                    Vec3 center = coral != null ? Vec3.atCenterOf(coral) : getSchoolCentroid();
                    Vec3 cand = center.add(
                            (this.random.nextDouble() - 0.5D) * 3.6D,
                            (this.random.nextDouble() - 0.45D) * 2.2D,
                            (this.random.nextDouble() - 0.5D) * 3.6D);
                    if (ElisarFaunaUtil.isAnyWater(this.level(), BlockPos.containing(cand))) {
                        this.driftTarget = cand;
                    }
                }
            }

            if (this.isAnchoredToCoral()) {
                this.setDeltaMovement(this.getDeltaMovement().scale(0.65D));
            } else if (this.driftTarget != null) {
                Vec3 diff = this.driftTarget.subtract(this.position());
                double d = diff.length();
                if (d > 0.25D) {
                    this.setDeltaMovement(this.getDeltaMovement().scale(0.84D).add(diff.scale(0.028D / d)));
                }
            } else {
                double bob = Math.sin((this.tickCount + this.getId() * 7) * 0.08D) * 0.008D;
                this.setDeltaMovement(this.getDeltaMovement().scale(0.85D).add(0.0D, bob, 0.0D));
            }

            Vec3 vel = this.getDeltaMovement();
            if (vel.horizontalDistanceSqr() > 0.0006D) {
                float yaw = (float) (Mth.atan2(vel.z, vel.x) * (180.0D / Math.PI)) - 90.0F;
                this.setYRot(Mth.rotLerp(0.14F, this.getYRot(), yaw));
                this.yBodyRot = this.getYRot();
            }
            this.move(MoverType.SELF, vel);
        } else {
            this.entityData.set(DATA_ANCHORED, false);
            if (this.onGround() && this.tickCount % 16 == 0) {
                this.setDeltaMovement((this.random.nextDouble() - 0.5D) * 0.2D, 0.24D, (this.random.nextDouble() - 0.5D) * 0.2D);
            }
        }
    }

    private Vec3 getSchoolCentroid() {
        List<ReefSeahorseEntity> mates = this.level().getEntitiesOfClass(
                ReefSeahorseEntity.class, this.getBoundingBox().inflate(7.0D), s -> s != this && s.isAlive());
        if (mates.isEmpty()) return this.position();
        Vec3 sum = Vec3.ZERO;
        for (ReefSeahorseEntity m : mates) sum = sum.add(m.position());
        return sum.scale(1.0D / mates.size());
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if ((held.is(Items.WATER_BUCKET) || held.is(ModFluids.BUCKET.get())) && this.isAlive()) {
            this.playSound(this.getPickupSound(), 1.0F, 1.0F);
            ItemStack bucketStack = this.getBucketItemStack();
            this.saveToBucketTag(bucketStack);
            ItemStack filled = ItemUtils.createFilledResult(held, player, bucketStack, false);
            player.setItemInHand(hand, filled);
            if (!this.level().isClientSide && player instanceof ServerPlayer sp) {
                CriteriaTriggers.FILLED_BUCKET.trigger(sp, bucketStack);
            }
            this.discard();
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean fromBucket() {
        return this.entityData.get(DATA_FROM_BUCKET);
    }

    @Override
    public void setFromBucket(boolean fromBucket) {
        this.entityData.set(DATA_FROM_BUCKET, fromBucket);
    }

    @Override
    public void saveToBucketTag(ItemStack stack) {
        Bucketable.saveDefaultDataToBucketTag(this, stack);
    }

    @Override
    public void loadFromBucketTag(CompoundTag tag) {
        Bucketable.loadDefaultDataFromBucketTag(this, tag);
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(ModItems.REEF_SEAHORSE_BUCKET.get());
    }

    @Override
    public SoundEvent getPickupSound() {
        return SoundEvents.BUCKET_FILL_FISH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.TROPICAL_FISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.TROPICAL_FISH_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("FromBucket", this.fromBucket());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setFromBucket(tag.getBoolean("FromBucket"));
    }
}
