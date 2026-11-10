package ru.aiforge.riftworld.entity;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import ru.aiforge.riftworld.registry.ModItems;

/**
 * v0.48.0: Лучник Элисара (референс 07_elisar_archer.png) — кожаный жилет с заклёпками, бирюзовый
 * наплечник на правом плече, колчан за спиной и изогнутый рифт-лук Элисара в левой руке.
 */
public final class ElisarArcherEntity extends PathfinderMob implements RangedAttackMob {

    public ElisarArcherEntity(EntityType<? extends ElisarArcherEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        if (this.getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
        }
        equipDefaultBow();
    }

    public void equipDefaultBow() {
        // Референс 07: лук в левой руке лучника (MAINHAND при LeftHanded=true)!
        this.setLeftHanded(true);
        ItemStack cur = this.getItemBySlot(EquipmentSlot.MAINHAND);
        if (cur.isEmpty() || !(cur.getItem() instanceof BowItem)) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.ELISAR_BOW.get()));
        }
        this.setDropChance(EquipmentSlot.MAINHAND, 0.07F);
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 28.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 30.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RangedBowAttackGoal<>(this, 1.0D, 22, 18.0F));
        this.goalSelector.addGoal(2, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 0.98D));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.92D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, ElisarGuardEntity.class, ElisarArcherEntity.class).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, e -> e instanceof Enemy));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                  MobSpawnType reason, @Nullable SpawnGroupData spawnData,
                                                  @Nullable CompoundTag dataTag) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        equipDefaultBow();
        if (reason == MobSpawnType.CHUNK_GENERATION || reason == MobSpawnType.STRUCTURE) {
            this.restrictTo(this.blockPosition(), 32);
        }
        return out;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        ItemStack bow = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof BowItem));
        ItemStack ammo = this.getProjectile(bow);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, ammo, distanceFactor);
        if (bow.getItem() instanceof BowItem bowItem) {
            arrow = bowItem.customArrow(arrow);
        }
        arrow.setBaseDamage(4.5D);
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333333333333333D) - arrow.getY();
        double dz = target.getZ() - this.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + horiz * 0.18D, dz, 1.7F, 3.0F);
        this.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(arrow);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }
}
