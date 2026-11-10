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
 * v0.49.0 — <b>Смотритель Пращи</b> ({@code riftworld:hollowwatcher}): новый хоррор-моб Мороковой Пади.
 *
 * <p>Морок — это бродячий охотник: он кружит, моргает за спину и бьёт. Смотритель —
 * принципиально другое. Он <b>не двигается, пока на него смотрят</b>, и <b>бросается
 * на жертву в тот миг, когда она отводит взгляд</b>. Это старый «дайз-джам»
 * в лесу, где нечего видеть, кроме темноты между стволами.
 *
 * <ul>
 *   <li><b>замирает под взглядом</b> — стоит неподвижно, пока игрок смотрит в его сторону;
 *       стоит поставить спиной к Смотрителю и услышать звук дыхания за спиной;</li>
 *   <li><b>рывок из темноты</b> — как только взгляд отведён, он делает длинный
 *       быстрый рывок к цели и бьёт в спину;</li>
 *   <li><b>высокий и тонкий</b> (3.4 блока) — в кронах Гнилой рощи он виден как
 *       тёмный силуэт между стволами;</li>
 *   <li><b>ледяной</b> — удар накладывает «медленность» и «туман», жертва теряет
 *       преимущество в скорости;</li>
 *   <li><b>не горит</b> и <b>не боится света</b> — его домой является ночной бор.</li>
 * </ul>
 */
public class HollowwatcherEntity extends Monster {
    /** Радиус, в котором Смотритель «чувствует» взгляд. */
    private static final double GAZE_RANGE = 26.0D;
    /** Дальность рывка в блоках. */
    private static final double LUNGE_RANGE = 22.0D;
    /** Сколько тиков Смотритель стоит неподвижно после того, как его заметили. */
    private static final int FREEZE_TICKS = 40;
    /** Тиков рывка. */
    private static final int LUNGE_TICKS = 22;

    private int lungeTimer = 0;
    private int freezeTimer = 0;
    private boolean wasWatched = false;

    public HollowwatcherEntity(EntityType<? extends HollowwatcherEntity> type, Level level) {
        super(type, level);
        this.xpReward = 55;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 58.0D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.45D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Главная цель — ПОДОЖДАТЬ, пока игрок отвернётся, и только потом ударить.
        this.goalSelector.addGoal(1, new WatchAndLungeGoal(this));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 40.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Смотрит ли {@code viewer} в сторону этой сущности прямо сейчас. */
    private static boolean isLookingAt(LivingEntity viewer, LivingEntity target) {
        Vec3 look = viewer.getLookAngle();
        Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D)
                .subtract(viewer.getEyePosition());
        double len = to.length();
        if (len < 0.01D) return true;
        to = to.scale(1.0D / len);
        // косинус угла ~15°: «смотрит в ту сторону» — довольно строгий, но честный критерий
        return look.dot(to) > 0.9659D;
    }

    /** Правда ли, что цель сейчас смотрит на Смотрителя? */
    public boolean targetIsWatching(LivingEntity target) {
        return target != null && target.isAlive()
                && target.distanceTo(this) <= GAZE_RANGE
                && isLookingAt(target, this);
    }

    /**
     * «Ждать взгляда и рвануть»: пока на Смотрителя смотрят — стоять смирно;
     * как только отвели — рывок к цели.
     */
    static final class WatchAndLungeGoal extends Goal {
        private final HollowwatcherEntity watcher;

        WatchAndLungeGoal(HollowwatcherEntity watcher) {
            this.watcher = watcher;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = this.watcher.getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity t = this.watcher.getTarget();
            if (t == null) return;
            boolean watched = this.watcher.targetIsWatching(t);
            this.watcher.getLookControl().setLookAt(t, 30.0F, 30.0F);

            if (watched) {
                // СМОТРЯТ — стоим колом. Ни шага, ни звука шагов.
                this.watcher.getNavigation().stop();
                this.watcher.setDeltaMovement(this.watcher.getDeltaMovement().multiply(0.25D, 1.0D, 0.25D));
                this.watcher.freezeTimer = FREEZE_TICKS;
                this.watcher.wasWatched = true;
                return;
            }

            // НЕ смотрят — рывок.
            double dist = this.watcher.distanceTo(t);
            if (this.watcher.wasWatched && dist <= LUNGE_RANGE && this.watcher.lungeTimer <= 0) {
                this.watcher.lungeTimer = LUNGE_TICKS;
                this.watcher.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.6F, 0.42F);
                if (this.watcher.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SOUL, this.watcher.getX(), this.watcher.getY() + 2.0D,
                            this.watcher.getZ(), 26, 0.5D, 1.1D, 0.5D, 0.03D);
                }
            }
            this.watcher.wasWatched = false;

            if (this.watcher.lungeTimer > 0) {
                Vec3 dir = t.position().subtract(this.watcher.position()).normalize();
                this.watcher.getMoveControl().setWantedPosition(
                        this.watcher.getX() + dir.x * 3.0D, this.watcher.getY(), this.watcher.getZ() + dir.z * 3.0D, 1.6D);
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) return;
        LivingEntity t = this.getTarget();
        if (t == null || !t.isAlive()) return;

        if (this.lungeTimer > 0) {
            --this.lungeTimer;
            Vec3 d = t.position().subtract(this.position()).normalize().scale(0.78D);
            this.setDeltaMovement(d.x, this.getDeltaMovement().y, d.z);
            this.hurtMarked = true;
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 1.0D, this.getZ(),
                        2, 0.2D, 0.2D, 0.2D, 0.01D);
            }
        }

        if (this.freezeTimer > 0) --this.freezeTimer;
    }

    /** Тихое дыхание, когда Смотритель стоит неподвижно и ждёт. */
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        if (this.freezeTimer > 0 && this.getTarget() != null && this.tickCount % 34 == 0) {
            this.playSound(SoundEvents.WARDEN_HEARTBEAT, 0.35F, 0.42F);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1, false, false), this);
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 90, 0, false, false), this);
            this.playSound(SoundEvents.WARDEN_ROAR, 1.15F, 0.85F);
        }
        return hit;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        this.spawnAtLocation(new ItemStack(ru.aiforge.riftworld.registry.ModItems.MOROK_EYE.get()));
        this.spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.AMETHYST_SHARD, 1 + this.random.nextInt(3)));
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    /** Смотритель — ночной охотник: днём он почти не охотится. */
    public boolean isNightHunter() {
        return !this.level().isDay();
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;   // он не «ambient»-ит: тишина — часть его страха
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
        // Шагов почти не слышно: слышно только дыхание.
        this.playSound(SoundEvents.WARDEN_STEP, 0.08F, 0.85F);
    }
}
