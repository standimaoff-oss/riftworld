package ru.aiforge.riftworld.entity.fauna;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import ru.aiforge.riftworld.registry.ElisarBlocks;

/**
 * 07. Цветущая улитка ({@code blossom_snail}) — мирный садовый обитатель Висячих садов.
 *
 * <p>Оставляет лёгкий след лепестков при ползании (без засорения мира блоками!).
 * При получении урона прячется в цветущую раковину на 5 секунд, снижая входящий урон на 50%
 * и полностью игнорируя отбрасывание.
 */
public class BlossomSnailEntity extends PathfinderMob {
    public static final int RETRACT_DURATION = 100;
    private static final EntityDataAccessor<Integer> DATA_RETRACT_TICKS =
            SynchedEntityData.defineId(BlossomSnailEntity.class, EntityDataSerializers.INT);

    private int prevRetractTicks;
    private int bloomCooldown = 0;

    public BlossomSnailEntity(EntityType<? extends BlossomSnailEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0F);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.14D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.40D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_RETRACT_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    public int getRetractTicks() {
        return this.entityData.get(DATA_RETRACT_TICKS);
    }

    public boolean isRetracted() {
        return this.getRetractTicks() > 0;
    }

    public float getRetractProgress(float partialTick) {
        float cur = Mth.lerp(partialTick, this.prevRetractTicks, this.getRetractTicks());
        return cur <= 0.0F ? 0.0F : Mth.clamp(cur / 10.0F, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        this.prevRetractTicks = this.getRetractTicks();
        super.tick();
        if (this.level().isClientSide) {
            if (!this.isRetracted() && this.getDeltaMovement().horizontalDistanceSqr() > 0.0002D
                    && this.tickCount % 16 == 0) {
                this.level().addParticle(ParticleTypes.CHERRY_LEAVES,
                        this.getRandomX(0.35D), this.getY() + 0.55D, this.getRandomZ(0.35D),
                        0.0D, 0.01D, 0.0D);
            }
            return;
        }

        if (this.bloomCooldown > 0) --this.bloomCooldown;
        int r = this.getRetractTicks();
        if (r > 0) {
            this.getNavigation().stop();
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
            this.entityData.set(DATA_RETRACT_TICKS, r - 1);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean wasRetracted = this.isRetracted();
        if (wasRetracted && !source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            amount *= 0.50F;
        }
        boolean hit = super.hurt(source, amount);
        if (hit && !this.level().isClientSide) {
            this.entityData.set(DATA_RETRACT_TICKS, RETRACT_DURATION);
            if (!wasRetracted) {
                this.playSound(SoundEvents.SHULKER_CLOSE, 0.85F, 1.35F);
            }
        }
        return hit;
    }

    @Override
    public void knockback(double strength, double x, double z) {
        if (this.isRetracted()) return;
        super.knockback(strength, x, z);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if ((stack.is(Items.BONE_MEAL) || stack.is(ElisarBlocks.BLOSSOM_MOSS.get().asItem()))
                && this.bloomCooldown <= 0 && !this.isRetracted()) {
            if (!this.level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                this.heal(4.0F);
                this.bloomCooldown = 360;
                this.playSound(SoundEvents.BONE_MEAL_USE, 0.9F, 1.1F);
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.CHERRY_LEAVES, this.getX(), this.getY() + 0.75D, this.getZ(),
                            14, 0.4D, 0.3D, 0.4D, 0.03D);
                }
                this.spawnAtLocation(this.random.nextBoolean()
                        ? new ItemStack(ElisarBlocks.WHITE_LILY_CLUSTER.get())
                        : new ItemStack(ElisarBlocks.FLOWERS.get()));
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SLIME_SQUISH_SMALL;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.DECORATED_POT_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH_SMALL;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("RetractTicks", this.getRetractTicks());
        tag.putInt("BloomCd", this.bloomCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_RETRACT_TICKS, tag.getInt("RetractTicks"));
        this.bloomCooldown = tag.getInt("BloomCd");
    }
}
