package ru.aiforge.riftworld.entity;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import ru.aiforge.riftworld.registry.ModEquipment;

/** Celadon-clad ranged defender. Uses actual Arrow entities rather than a cosmetic attack effect. */
public final class ElisarArcherEntity extends ElisarCitizenEntity implements RangedAttackMob {
    public ElisarArcherEntity(EntityType<? extends AbstractVillager> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return ElisarCitizenEntity.attributes()
                .add(Attributes.MAX_HEALTH, 26.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 28.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new ArcherCombatGoal());
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        ItemStack bow = this.getItemBySlot(EquipmentSlot.MAINHAND);
        ItemStack projectile = new ItemStack(net.minecraft.world.item.Items.ARROW);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, projectile, velocity);
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.333D) - arrow.getY();
        double dz = target.getZ() - this.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + distance * 0.18D, dz, 1.65F * velocity, 1.5F);
        arrow.setBaseDamage(3.5D);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        this.playSound(SoundEvents.SKELETON_SHOOT, 0.9F, 0.95F + this.random.nextFloat() * 0.12F);
        this.level().addFreshEntity(arrow);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData groupData, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, groupData, tag);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModEquipment.AURITE_BOW.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        return result;
    }

    /** RangedBowAttackGoal is Monster-only in 1.20.1; this is the equivalent for an NPC defender. */
    private final class ArcherCombatGoal extends Goal {
        private int cooldown;
        private int seen;

        private ArcherCombatGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = ElisarArcherEntity.this.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            ElisarArcherEntity.this.setAggressive(true);
            this.cooldown = 12;
            this.seen = 0;
        }

        @Override
        public void stop() {
            ElisarArcherEntity.this.setAggressive(false);
            ElisarArcherEntity.this.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ElisarArcherEntity archer = ElisarArcherEntity.this;
            LivingEntity target = archer.getTarget();
            if (target == null) return;
            double distance = archer.distanceTo(target);
            boolean visible = archer.getSensing().hasLineOfSight(target);
            this.seen = visible ? Math.min(20, this.seen + 1) : Math.max(0, this.seen - 2);
            if (distance > 14.0D || !visible) archer.getNavigation().moveTo(target, 1.0D);
            else if (distance < 7.0D) {
                double dx = archer.getX() - target.getX(), dz = archer.getZ() - target.getZ();
                archer.getNavigation().moveTo(archer.getX() + dx, archer.getY(), archer.getZ() + dz, 0.9D);
            } else archer.getNavigation().stop();
            archer.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (--this.cooldown <= 0 && this.seen >= 8 && distance <= 20.0D) {
                archer.performRangedAttack(target, 1.0F);
                this.cooldown = 26 + archer.random.nextInt(9);
            }
        }
    }
}
