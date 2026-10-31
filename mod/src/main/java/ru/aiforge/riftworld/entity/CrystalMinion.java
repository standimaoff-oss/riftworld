package ru.aiforge.riftworld.entity;

import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.registry.ModItems;

/**
 * Призываемый Боссом Колоннады парящий кристальный конструкт «Гранёный Прислужник»
 * ({@code reports/boss_crystal_refs/02_crystal_minions_ref.png}).
 * Не имеет ног — парит на бирюзовом эфирном шлейфе и рассекает цель парными клинками-катарами.
 *
 * <p>Дроп при убийстве:
 * <ul>
 *   <li>«Длань Осколка» ({@code crystal_katar}) — многоразовый клинок как на его руке;</li>
 *   <li>«Веер Осколков» ({@code crystal_barrage}) — стакающийся одноразовый кристальный выстрел/каскад;</li>
 *   <li>«Бастион Граней» ({@code crystal_aegis}) — стакающийся одноразовый таранный/защитный щит.</li>
 * </ul>
 */
public class CrystalMinion extends Monster {
    private static final EntityDataAccessor<Integer> SLASH_TICKS =
            SynchedEntityData.defineId(CrystalMinion.class, EntityDataSerializers.INT);

    public CrystalMinion(EntityType<? extends CrystalMinion> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 36.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FOLLOW_RANGE, 36.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.25);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SLASH_TICKS, 0);
    }

    public int slashTicks() {
        return this.entityData.get(SLASH_TICKS);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new HoverSlashGoal(this));
        this.goalSelector.addGoal(2, new HoverIdleDriftGoal(this));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            int st = slashTicks();
            if (st > 0) this.entityData.set(SLASH_TICKS, st - 1);
            // Мягкое гашение инерции в полёте
            this.setDeltaMovement(this.getDeltaMovement().scale(0.88));
        } else if ((this.tickCount & 2) == 0) {
            this.level().addParticle(ParticleTypes.GLOW,
                    this.getX() + (this.random.nextDouble() - 0.5) * 0.5,
                    this.getY() + 0.25,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 0.5,
                    0, -0.02, 0);
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_BLOCK_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AMETHYST_CLUSTER_BREAK;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        float katarChance = 0.32F + looting * 0.08F;
        if (this.random.nextFloat() < katarChance) {
            this.spawnAtLocation(new ItemStack(ModItems.CRYSTAL_KATAR.get()));
        }
        if (this.random.nextFloat() < 0.72F + looting * 0.06F) {
            this.spawnAtLocation(new ItemStack(ModItems.CRYSTAL_BARRAGE.get(), 2 + this.random.nextInt(3 + looting)));
        }
        if (this.random.nextFloat() < 0.62F + looting * 0.06F) {
            this.spawnAtLocation(new ItemStack(ModItems.CRYSTAL_AEGIS.get(), 1 + this.random.nextInt(3 + looting)));
        }
    }

    private static final class HoverSlashGoal extends Goal {
        private final CrystalMinion mob;
        private int attackCooldown;

        HoverSlashGoal(CrystalMinion mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = mob.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void tick() {
            LivingEntity t = mob.getTarget();
            if (t == null) return;
            mob.getLookControl().setLookAt(t, 30.0F, 30.0F);
            double dx = t.getX() - mob.getX();
            double dy = (t.getY() + 0.55) - mob.getY();
            double dz = t.getZ() - mob.getZ();
            double dist2 = dx * dx + dy * dy + dz * dz;
            mob.setYRot((float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F);
            mob.yBodyRot = mob.getYRot();

            if (dist2 > 2.6) {
                Vec3 dir = new Vec3(dx, dy, dz).normalize();
                double speed = dist2 < 25.0 && attackCooldown <= 4 ? 0.14 : 0.085;
                mob.setDeltaMovement(mob.getDeltaMovement().add(dir.scale(speed)));
            }
            if (attackCooldown > 0) --attackCooldown;
            if (dist2 <= 5.2 && attackCooldown <= 0) {
                attackCooldown = 22;
                mob.entityData.set(SLASH_TICKS, 12);
                mob.doHurtTarget(t);
                mob.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.9F, 1.45F);
                if (mob.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.END_ROD, t.getX(), t.getY() + 1.0, t.getZ(), 8, 0.25, 0.25, 0.25, 0.06);
                }
            }
        }
    }

    private static final class HoverIdleDriftGoal extends Goal {
        private final CrystalMinion mob;

        HoverIdleDriftGoal(CrystalMinion mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return mob.getTarget() == null && mob.getRandom().nextInt(18) == 0;
        }

        @Override
        public void start() {
            double ax = (mob.getRandom().nextDouble() - 0.5) * 0.16;
            double az = (mob.getRandom().nextDouble() - 0.5) * 0.16;
            boolean low = !mob.level().getBlockState(mob.blockPosition().below(2)).isAir();
            double ay = low ? 0.05 : -0.03;
            mob.setDeltaMovement(mob.getDeltaMovement().add(ax, ay, az));
        }
    }
}
