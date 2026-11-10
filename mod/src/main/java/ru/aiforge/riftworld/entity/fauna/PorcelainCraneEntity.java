package ru.aiforge.riftworld.entity.fauna;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.registry.ElisarBlocks;
import ru.aiforge.riftworld.registry.ModItems;

/**
 * 01. Фарфоровый журавль ({@code porcelain_crane}) — мирная грациозная птица Города Элисара.
 *
 * <p>Не крадёт предметы с земли: вместо этого подходит к цветам Элисара, исполняет поклон-танец
 * под перезвон фарфора и изредка роняет Фарфоровое перо. При испуге совершает короткий плавный взлёт.
 */
public class PorcelainCraneEntity extends PathfinderMob {
    public static final int DANCE_DURATION = 50;
    public static final int FLUTTER_DURATION = 48;

    private static final EntityDataAccessor<Integer> DATA_DANCE_TICKS =
            SynchedEntityData.defineId(PorcelainCraneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FLUTTER_TICKS =
            SynchedEntityData.defineId(PorcelainCraneEntity.class, EntityDataSerializers.INT);

    private int prevDanceTicks;
    private int prevFlutterTicks;
    private int flowerSeekCooldown = 80;
    private int featherGiftCooldown = 600;

    public PorcelainCraneEntity(EntityType<? extends PorcelainCraneEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0F);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_DANCE_TICKS, 0);
        this.entityData.define(DATA_FLUTTER_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.38D));
        this.goalSelector.addGoal(2, new TemptGoal(this, 1.08D, Ingredient.of(
                ElisarBlocks.WHITE_LILY_CLUSTER.get(),
                ElisarBlocks.FLOWERS.get(),
                ElisarBlocks.VIOLET_BLOOM.get(),
                ElisarBlocks.LILAC_BLOOM.get(),
                ElisarBlocks.PALE_BLOOM.get()), false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.92D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    public int getDanceTicks() {
        return this.entityData.get(DATA_DANCE_TICKS);
    }

    public int getFlutterTicks() {
        return this.entityData.get(DATA_FLUTTER_TICKS);
    }

    public float getDanceProgress(float partialTick) {
        float cur = Mth.lerp(partialTick, this.prevDanceTicks, this.getDanceTicks());
        return Mth.clamp(Mth.sin((cur / DANCE_DURATION) * (float) Math.PI), 0.0F, 1.0F);
    }

    public float getFlutterProgress(float partialTick) {
        float cur = Mth.lerp(partialTick, this.prevFlutterTicks, this.getFlutterTicks());
        return cur <= 0.0F ? 0.0F : Mth.clamp(cur / 12.0F, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        this.prevDanceTicks = this.getDanceTicks();
        this.prevFlutterTicks = this.getFlutterTicks();
        super.tick();

        Vec3 v = this.getDeltaMovement();
        if (!this.onGround() && v.y < 0.0D) {
            this.setDeltaMovement(v.multiply(1.0D, 0.62D, 1.0D));
        }

        if (!this.level().isClientSide) {
            if (this.featherGiftCooldown > 0) --this.featherGiftCooldown;
            int dance = this.getDanceTicks();
            if (dance > 0) {
                this.getNavigation().stop();
                this.entityData.set(DATA_DANCE_TICKS, dance - 1);
                if (dance == 24 && this.featherGiftCooldown <= 0 && this.random.nextFloat() < 0.25F) {
                    this.spawnAtLocation(ModItems.PORCELAIN_FEATHER.get());
                    this.featherGiftCooldown = 2400;
                }
            }
            int flutter = this.getFlutterTicks();
            if (flutter > 0) {
                this.entityData.set(DATA_FLUTTER_TICKS, flutter - 1);
                if (flutter > 30 && this.onGround()) {
                    Vec3 away = Vec3.directionFromRotation(0.0F, this.getYRot()).scale(0.32D);
                    this.setDeltaMovement(away.x, 0.36D, away.z);
                }
            } else if (!this.onGround() && this.fallDistance > 0.8F) {
                this.entityData.set(DATA_FLUTTER_TICKS, 12);
            }

            if (dance <= 0 && flutter <= 0 && --this.flowerSeekCooldown <= 0) {
                this.flowerSeekCooldown = 140 + this.random.nextInt(140);
                BlockPos flower = ElisarFaunaUtil.findNearestBlock(
                        this.level(), this.blockPosition(), 7, 2, 7, ElisarFaunaUtil::isElisarFlowerBlock);
                if (flower != null) {
                    if (flower.distSqr(this.blockPosition()) <= 4.5D) {
                        triggerPorcelainDance(false);
                    } else {
                        this.getNavigation().moveTo(flower.getX() + 0.5D, flower.getY(), flower.getZ() + 0.5D, 1.0D);
                    }
                }
            }
        }
    }

    private void triggerPorcelainDance(boolean playerFed) {
        this.entityData.set(DATA_DANCE_TICKS, DANCE_DURATION);
        this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.85F, 1.15F + this.random.nextFloat() * 0.2F);
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.15D, this.getZ(),
                    8, 0.35D, 0.35D, 0.35D, 0.02D);
            if (playerFed && this.featherGiftCooldown <= 0) {
                this.spawnAtLocation(ModItems.PORCELAIN_FEATHER.get());
                this.featherGiftCooldown = 1800;
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ElisarFaunaUtil.isFlowerItem(stack) && this.getDanceTicks() <= 0) {
            if (!this.level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                this.heal(2.0F);
                triggerPorcelainDance(true);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean res = super.hurt(source, amount);
        if (res && !this.level().isClientSide) {
            this.entityData.set(DATA_DANCE_TICKS, 0);
            this.entityData.set(DATA_FLUTTER_TICKS, FLUTTER_DURATION);
            Vec3 lift = this.getDeltaMovement();
            this.setDeltaMovement(lift.x, Math.max(lift.y, 0.42D), lift.z);
        }
        return res;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.AMETHYST_BLOCK_STEP;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.DECORATED_POT_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.DECORATED_POT_SHATTER;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FeatherCooldown", this.featherGiftCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.featherGiftCooldown = tag.getInt("FeatherCooldown");
    }
}
