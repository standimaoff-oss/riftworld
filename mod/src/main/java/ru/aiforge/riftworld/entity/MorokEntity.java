package ru.aiforge.riftworld.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * v0.39.0 — <b>Морок</b> ({@code riftworld:morok}): собственный хоррор-моб «Мороковой Пади».
 *
 * <p>Не вендиго: не идёт напролом. Морок <b>кружит</b> вокруг жертвы на границе видимости в тумане,
 * <b>рвётся</b> короткими рывками и <b>моргает</b> за спину, если его долго не могут достать.
 * Днём почти не показывается (в биоме шанс 5 %), ночью — часто (45 %), см. {@code MorokFaunaKeeper}.
 *
 * <ul>
 *   <li>глаза горят: клиентский {@code MorokRenderer} дорисовывает {@code geo/morok_eyes.json}
 *       ванильным {@code RenderType.eyes} — они светятся в полной темноте;</li>
 *   <li>удар накладывает «тьму» и «тошноту»: жертва теряет ориентацию, а Морок уже сбоку;</li>
 *   <li>быстрый: скорость 0.37 против 0.31 у вендиго, но здоровья меньше — его можно отбить;</li>
 *   <li>не горит на солнце: он живёт в вечной дымке.</li>
 * </ul>
 */
public class MorokEntity extends Monster {
    /** Сколько тиков между «морганиями» (60 тиков = 3 с). */
    private static final int BLINK_COOLDOWN = 70;
    private int blinkTimer = BLINK_COOLDOWN;
    private int dashTimer = 0;

    public MorokEntity(EntityType<? extends MorokEntity> type, Level level) {
        super(type, level);
        this.xpReward = 30;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 42.0D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.37D)
                .add(Attributes.FOLLOW_RANGE, 34.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.25D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // круг вокруг жертвы: «не идёт прям наперёд»
        this.goalSelector.addGoal(2, new StalkGoal(this));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.45D, true));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 26.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Кружит вокруг цели на 5–9 блоков, иногда делает рывок вперёд и тут же отходит. */
    static final class StalkGoal extends Goal {
        private final MorokEntity morok;
        private int strafeDir = 1;
        private int replan = 0;

        StalkGoal(MorokEntity morok) {
            this.morok = morok;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = this.morok.getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            this.strafeDir = this.morok.getRandom().nextBoolean() ? 1 : -1;
            this.replan = 0;
        }

        @Override
        public void tick() {
            LivingEntity t = this.morok.getTarget();
            if (t == null) return;
            this.morok.getLookControl().setLookAt(t, 40.0F, 40.0F);
            double dist = this.morok.distanceTo(t);
            if (--this.replan <= 0) {
                this.replan = 20 + this.morok.getRandom().nextInt(20);
                if (this.morok.getRandom().nextInt(4) == 0) this.strafeDir = -this.strafeDir;
            }
            // ближе 4 блоков — отходим вбок; дальше 9 — подходим по дуге
            Vec3 dir = t.position().subtract(this.morok.position()).normalize();
            Vec3 side = new Vec3(-dir.z, 0, dir.x).scale(this.strafeDir);
            Vec3 step;
            if (dist < 4.0D) {
                step = side.subtract(dir.scale(0.6D)).normalize().scale(1.1D);
            } else if (dist > 9.0D) {
                step = dir.scale(1.0D).add(side.scale(0.7D)).normalize().scale(1.35D);
            } else {
                step = side.scale(1.0D).add(dir.scale(0.15D)).normalize().scale(1.0D);
            }
            this.morok.getMoveControl().setWantedPosition(
                    this.morok.getX() + step.x, this.morok.getY(), this.morok.getZ() + step.z, 1.25D);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) return;
        LivingEntity t = this.getTarget();
        if (t == null || !t.isAlive()) return;

        // ---- рывок: 8 тиков очень быстрого сближения
        if (this.dashTimer > 0) {
            --this.dashTimer;
            Vec3 d = t.position().subtract(this.position()).normalize().scale(0.62D);
            this.setDeltaMovement(d.x, this.getDeltaMovement().y, d.z);
            this.hurtMarked = true;
        }

        if (this.tickCount % 4 != 0) return;
        double dist = this.distanceTo(t);
        // ---- моргание за спину: если цель держит дистанцию и не даёт подойти
        if (--this.blinkTimer <= 0) {
            this.blinkTimer = BLINK_COOLDOWN;
            if (dist > 8.0D && this.getRandom().nextFloat() < 0.55F) {
                blinkBehind(t);
            } else if (dist < 12.0D && this.getRandom().nextFloat() < 0.5F) {
                this.dashTimer = 8;
                this.playSound(SoundEvents.WARDEN_SNIFF, 1.0F, 1.6F);
            }
        }
    }

    /** Короткий телепорт за спину цели: 3–6 блоков позади, лицом к ней. */
    private void blinkBehind(LivingEntity target) {
        Vec3 back = target.position().subtract(target.getLookAngle().scale(4.0D));
        for (int attempt = 0; attempt < 8; ++attempt) {
            double ox = (this.getRandom().nextDouble() - 0.5D) * 4.0D;
            double oz = (this.getRandom().nextDouble() - 0.5D) * 4.0D;
            double nx = back.x + ox, nz = back.z + oz;
            BlockPos p = BlockPos.containing(nx, target.getY(), nz);
            if (!this.level().isEmptyBlock(p) || !this.level().isEmptyBlock(p.above())) continue;
            if (this.level().isEmptyBlock(p.below())) continue;      // нужна опора
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.0D, this.getZ(), 18,
                        0.4D, 0.9D, 0.4D, 0.02D);
            }
            this.teleportTo(nx, p.getY(), nz);
            this.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.7F, 0.55F);
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, nx, p.getY() + 1.0D, nz, 22,
                        0.4D, 1.0D, 0.4D, 0.02D);
            }
            return;
        }
    }

    /** Удар «мороком»: тьма + тошнота, и жертва теряет Морока из виду. */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0, false, false), this);
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 140, 0, false, false), this);
            this.playSound(SoundEvents.WARDEN_ROAR, 1.0F, 1.35F);   // выше и злее, чем у вендиго
        }
        return hit;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        this.spawnAtLocation(new ItemStack(ru.aiforge.riftworld.registry.ModItems.MOROK_EYE.get()));
        this.spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.GLOWSTONE_DUST, 1 + this.random.nextInt(2)));
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    /** Днём Морок «гаснет»: он не горит, но и не охотится так рьяно. */
    public boolean isNightHunter() {
        return !this.level().isDay();
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return this.random.nextInt(5) == 0 ? SoundEvents.WARDEN_AMBIENT : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WARDEN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WARDEN_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.WARDEN_STEP, 0.18F, 1.15F);
    }
}
