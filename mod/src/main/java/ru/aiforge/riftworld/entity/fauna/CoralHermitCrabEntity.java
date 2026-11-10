package ru.aiforge.riftworld.entity.fauna;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.registry.ElisarBlocks;

/**
 * 12. Коралловый краб-отшельник ({@code coral_hermit_crab}) — амфибия мелководных рифов Элисара.
 *
 * <p>Свободно шагает по морскому дну в воде Элисара и на берегу, перебирает песок клешнями
 * (изредка находя ракушки, кристаллы призмарина или самородки) и прячется в коралловую раковину (-35% урона).
 */
public class CoralHermitCrabEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> DATA_SIFT_TICKS =
            SynchedEntityData.defineId(CoralHermitCrabEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HUNKER_TICKS =
            SynchedEntityData.defineId(CoralHermitCrabEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PINCH_TICKS =
            SynchedEntityData.defineId(CoralHermitCrabEntity.class, EntityDataSerializers.INT);

    private int siftCooldown = 300;
    private int findsCount = 0;

    public CoralHermitCrabEntity(EntityType<? extends CoralHermitCrabEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 22.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.21D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.35D);
    }

    @Override
    public MobType getMobType() {
        return MobType.ARTHROPOD;
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
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SIFT_TICKS, 0);
        this.entityData.define(DATA_HUNKER_TICKS, 0);
        this.entityData.define(DATA_PINCH_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        // Deliberately omit FloatGoal so the crab walks along the seabed!
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.22D, false));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.85D, 45));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 7.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    public int getSiftTicks() {
        return this.entityData.get(DATA_SIFT_TICKS);
    }

    public int getHunkerTicks() {
        return this.entityData.get(DATA_HUNKER_TICKS);
    }

    public int getPinchTicks() {
        return this.entityData.get(DATA_PINCH_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        // Settle onto the seabed when submerged in Elisar or vanilla water
        if (ElisarFaunaUtil.isSubmergedInAnyWater(this) && !this.onGround()) {
            Vec3 v = this.getDeltaMovement();
            this.setDeltaMovement(v.x, Math.max(v.y - 0.045D, -0.25D), v.z);
        }

        if (!this.level().isClientSide) {
            this.setAirSupply(this.getMaxAirSupply());
            int sift = this.getSiftTicks();
            if (sift > 0) {
                this.getNavigation().stop();
                this.entityData.set(DATA_SIFT_TICKS, sift - 1);
                if (sift % 8 == 0 && this.level() instanceof ServerLevel sl) {
                    BlockState floor = this.level().getBlockState(this.blockPosition().below());
                    if (!floor.isAir()) {
                        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor),
                                this.getX(), this.getY() + 0.15D, this.getZ(), 5, 0.2D, 0.08D, 0.2D, 0.02D);
                    }
                }
                if (sift == 1 && this.findsCount < 3 && this.random.nextFloat() < 0.28F) {
                    ++this.findsCount;
                    ItemStack treasure = switch (this.random.nextInt(4)) {
                        case 0 -> new ItemStack(ElisarBlocks.SEA_SHELLS.get());
                        case 1 -> new ItemStack(Items.PRISMARINE_CRYSTALS);
                        case 2 -> new ItemStack(Items.GOLD_NUGGET);
                        default -> this.random.nextInt(3) == 0
                                ? new ItemStack(Items.NAUTILUS_SHELL)
                                : new ItemStack(ElisarBlocks.SEA_SHELLS.get());
                    };
                    this.spawnAtLocation(treasure);
                    this.playSound(SoundEvents.SAND_BREAK, 0.8F, 1.2F);
                }
            }

            int hunker = this.getHunkerTicks();
            if (hunker > 0) this.entityData.set(DATA_HUNKER_TICKS, hunker - 1);
            int pinch = this.getPinchTicks();
            if (pinch > 0) this.entityData.set(DATA_PINCH_TICKS, pinch - 1);

            if (sift <= 0 && hunker <= 0 && this.getTarget() == null && this.onGround() && --this.siftCooldown <= 0) {
                this.siftCooldown = 500 + this.random.nextInt(300);
                BlockPos below = this.blockPosition().below();
                BlockState bs = this.level().getBlockState(below);
                Player nearPlayer = this.level().getNearestPlayer(this, 20.0D);
                if (nearPlayer != null && (bs.is(ElisarBlocks.SEABED.get())
                        || bs.is(ElisarBlocks.SILT.get()) || bs.is(Blocks.SAND) || bs.is(Blocks.GRAVEL))) {
                    this.entityData.set(DATA_SIFT_TICKS, 40);
                    this.playSound(SoundEvents.SAND_STEP, 0.85F, 1.1F);
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        this.entityData.set(DATA_PINCH_TICKS, 12);
        return super.doHurtTarget(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            amount *= 0.65F; // -35% shell protection
        }
        boolean hit = super.hurt(source, amount);
        if (hit && !this.level().isClientSide) {
            this.entityData.set(DATA_HUNKER_TICKS, 45);
        }
        return hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.TURTLE_AMBIENT_LAND;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SHULKER_HURT_CLOSED;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.TURTLE_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FindsCount", this.findsCount);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.findsCount = tag.getInt("FindsCount");
    }
}
