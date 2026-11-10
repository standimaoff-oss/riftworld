package ru.aiforge.riftworld.entity.fauna;

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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.registry.ElisarBlocks;
import ru.aiforge.riftworld.registry.ModParticles;

/**
 * 05. Листокрыл Сизо ({@code sizo_leafwing}) — пернатый лесной планер Рощи Сизо.
 *
 * <p>Нейтрален: отвечает порывом крыла (4 урона) только при нападении. Приручается ягодами или
 * лилиями/папоротником Элисара, поддерживает переключение «Сидеть / Следовать» и спасает
 * падающего хозяина коротким эффектом {@code Slow Falling} (3 с, КД 25 с).
 */
public class SizoLeafwingEntity extends TamableAnimal {
    private static final EntityDataAccessor<Boolean> DATA_GLIDING =
            SynchedEntityData.defineId(SizoLeafwingEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_GUST_TICKS =
            SynchedEntityData.defineId(SizoLeafwingEntity.class, EntityDataSerializers.INT);

    private int slowFallCooldown = 0;

    public SizoLeafwingEntity(EntityType<? extends SizoLeafwingEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.1F);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.29D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_GLIDING, false);
        this.entityData.define(DATA_GUST_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.28D, true));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.15D, 7.0F, 2.5F, true));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.08D, Ingredient.of(
                Items.SWEET_BERRIES, Items.GLOW_BERRIES,
                ElisarBlocks.WHITE_LILY_CLUSTER.get(), ElisarBlocks.CELADON_FERN.get()), false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.95D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    public boolean isGliding() {
        return this.entityData.get(DATA_GLIDING);
    }

    public int getGustTicks() {
        return this.entityData.get(DATA_GUST_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = this.getDeltaMovement();
        boolean glidingNow = !this.onGround() && !this.isInWaterOrBubble() && v.y < -0.08D;
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_GLIDING, glidingNow);
            if (this.slowFallCooldown > 0) --this.slowFallCooldown;
            int gust = this.getGustTicks();
            if (gust > 0) this.entityData.set(DATA_GUST_TICKS, gust - 1);

            if (this.isTame() && this.slowFallCooldown <= 0 && this.getOwner() instanceof Player owner) {
                if (owner.isAlive() && !owner.onGround() && owner.fallDistance >= 4.0F
                        && this.distanceToSqr(owner) <= 144.0D) {
                    owner.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, true));
                    this.slowFallCooldown = 500;
                    this.entityData.set(DATA_GUST_TICKS, 18);
                    this.playSound(SoundEvents.PARROT_FLY, 1.0F, 0.85F);
                    if (this.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ModParticles.SIZO_LEAF.get(),
                                owner.getX(), owner.getY() + 0.8D, owner.getZ(),
                                16, 0.55D, 0.35D, 0.55D, 0.04D);
                    }
                }
            }
        }
        if (glidingNow) {
            Vec3 forward = Vec3.directionFromRotation(0.0F, this.getYRot()).scale(0.035D);
            this.setDeltaMovement(v.x * 0.98D + forward.x, Math.max(v.y * 0.58D, -0.18D), v.z * 0.98D + forward.z);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        this.entityData.set(DATA_GUST_TICKS, 16);
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            Vec3 push = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D).normalize().scale(0.65D);
            living.push(push.x, 0.25D, push.z);
            this.playSound(SoundEvents.PHANTOM_FLAP, 0.7F, 1.3F);
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ModParticles.SIZO_LEAF.get(), this.getX(), this.getY() + 0.8D, this.getZ(),
                        12, 0.45D, 0.35D, 0.45D, 0.05D);
            }
        }
        return hit;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ElisarFaunaUtil.isFoxOrLeafwingTreat(stack)) {
            if (!this.level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                if (!this.isTame()) {
                    if (this.random.nextInt(3) == 0) {
                        this.tame(player);
                        this.navigation.stop();
                        this.setTarget(null);
                        this.level().broadcastEntityEvent(this, (byte) 7);
                    } else {
                        this.level().broadcastEntityEvent(this, (byte) 6);
                    }
                } else {
                    this.heal(4.0F);
                }
                this.playSound(SoundEvents.PARROT_EAT, 0.9F, 1.0F);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (this.isOwnedBy(player) && stack.isEmpty()) {
            if (!this.level().isClientSide) {
                this.setOrderedToSit(!this.isOrderedToSit());
                this.jumping = false;
                this.navigation.stop();
                this.setTarget(null);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SlowFallCd", this.slowFallCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.slowFallCooldown = tag.getInt("SlowFallCd");
    }
}
