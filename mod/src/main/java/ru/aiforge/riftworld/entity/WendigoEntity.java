package ru.aiforge.riftworld.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;

/**
 * v0.35.0 — <b>Вендиго</b> ({@code riftworld:wendigo}), хозяин «Мороковой Пади».
 *
 * <p>По референсу {@code reports/biome_concept_morok_hollow/02_night_horror_wendigo.jpg}:
 * высокая измождённая тварь на длинных ногах, лосиный череп с рогами, длинные когтистые руки.
 * <ul>
 *   <li>охотится только в тумане Пади (ночью — втрое чаще);</li>
 *   <li>удар накладывает «тьму» на 12 секунд — жертва перестаёт видеть дальше пары блоков;</li>
 *   <li>слышит бег: если игрок спринтует в 40 блоках, бросается на звук;</li>
 *   <li>не горит на солнце (живёт в вечной дымке), но и не выходит из биома: у края поворачивает назад.</li>
 * </ul>
 */
public class WendigoEntity extends Monster {
    public WendigoEntity(EntityType<? extends WendigoEntity> type, Level level) {
        super(type, level);
        this.xpReward = 26;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 64.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.31D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Удар «по-вендиговски»: тьма в глазах на 12 секунд. */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 240, 0, false, false), this);
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false), this);
            this.playSound(SoundEvents.WARDEN_ROAR, 1.1F, 0.65F);
        }
        return hit;
    }

    /** Слышит бег: спринтующий игрок в 40 блоках становится целью. */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || this.tickCount % 20 != 0) return;
        if (this.getTarget() != null && this.getTarget().isAlive()) return;
        Player best = null;
        double bestD = 40.0D * 40.0D;
        for (Player p : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(40.0D))) {
            if (!p.isSprinting() || p.isCreative() || p.isSpectator()) continue;
            double d = this.distanceToSqr(p);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        if (best != null) this.setTarget(best);
    }

    /** Дроп: рог вендиго и кости — трофей охотника за хозяином Пади. */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        this.spawnAtLocation(new ItemStack(ru.aiforge.riftworld.registry.ModItems.WENDIGO_ANTLER.get()));
        this.spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.BONE, 2 + this.random.nextInt(3)));
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return this.random.nextInt(4) == 0 ? SoundEvents.WARDEN_AMBIENT : null;
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
        this.playSound(SoundEvents.WARDEN_STEP, 0.22F, 0.9F);
    }
}
