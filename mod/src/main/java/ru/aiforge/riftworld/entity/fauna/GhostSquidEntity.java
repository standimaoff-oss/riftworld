package ru.aiforge.riftworld.entity.fauna;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.registry.ModItems;

/**
 * 14. Призрачный кальмар ({@code ghost_squid}) — глубоководный биолюминесцентный обитатель Бездны.
 *
 * <p>Избегает яркого света ({@code light >= 10}). При получении урона выпускает плотное светящееся
 * чернильное облако и совершает реактивный рывок в глубину; если же его зажать в угол во время КД чернил,
 * отбивается хлещущим ударом щупалец (4 урона).
 */
public class GhostSquidEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> DATA_JET_TICKS =
            SynchedEntityData.defineId(GhostSquidEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LASH_TICKS =
            SynchedEntityData.defineId(GhostSquidEntity.class, EntityDataSerializers.INT);

    @Nullable
    private Vec3 swimGoal;
    private int inkCooldown = 0;
    private int lashCooldown = 0;

    public GhostSquidEntity(EntityType<? extends GhostSquidEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 26.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_JET_TICKS, 0);
        this.entityData.define(DATA_LASH_TICKS, 0);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    public int getJetTicks() {
        return this.entityData.get(DATA_JET_TICKS);
    }

    public int getLashTicks() {
        return this.entityData.get(DATA_LASH_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        boolean inWater = ElisarFaunaUtil.isSubmergedInAnyWater(this);
        this.setNoGravity(inWater);

        if (this.level().isClientSide) {
            if (inWater && this.random.nextInt(10) == 0) {
                this.level().addParticle(ParticleTypes.GLOW,
                        this.getRandomX(0.55D), this.getRandomY(), this.getRandomZ(0.55D),
                        0.0D, -0.01D, 0.0D);
            }
            return;
        }

        if (this.inkCooldown > 0) --this.inkCooldown;
        if (this.lashCooldown > 0) --this.lashCooldown;
        int jet = this.getJetTicks();
        if (jet > 0) this.entityData.set(DATA_JET_TICKS, jet - 1);
        int lash = this.getLashTicks();
        if (lash > 0) this.entityData.set(DATA_LASH_TICKS, lash - 1);

        if (inWater) {
            this.setAirSupply(this.getMaxAirSupply());
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive() && this.distanceToSqr(target) <= 36.0D) {
                Vec3 toTarget = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).subtract(this.position());
                double dist = Math.max(0.01D, toTarget.length());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.84D).add(toTarget.scale(0.055D / dist)));
                if (dist <= 2.6D && this.lashCooldown <= 0) {
                    this.lashCooldown = 32;
                    this.entityData.set(DATA_LASH_TICKS, 14);
                    this.doHurtTarget(target);
                    this.setTarget(null); // disengages right after cornered lash
                }
            } else {
                // Photophobic rhythmic mantle jetting toward dark deep water
                int localLight = this.level().getMaxLocalRawBrightness(this.blockPosition());
                if (localLight >= 10 || this.swimGoal == null || this.tickCount % 50 == 0) {
                    double dy = localLight >= 10 ? -3.5D : (this.random.nextDouble() - 0.52D) * 4.0D;
                    Vec3 cand = this.position().add(
                            (this.random.nextDouble() - 0.5D) * 9.0D,
                            dy,
                            (this.random.nextDouble() - 0.5D) * 9.0D);
                    if (ElisarFaunaUtil.isAnyWater(this.level(), BlockPos.containing(cand))) {
                        this.swimGoal = cand;
                    }
                }
                if (this.swimGoal != null && this.tickCount % 26 == 0) {
                    Vec3 dir = this.swimGoal.subtract(this.position());
                    double len = Math.max(0.01D, dir.length());
                    this.setDeltaMovement(this.getDeltaMovement().add(dir.scale(0.24D / len)));
                    this.entityData.set(DATA_JET_TICKS, 14);
                }
            }

            Vec3 vel = this.getDeltaMovement();
            if (vel.lengthSqr() > 0.0008D) {
                float yaw = (float) (Mth.atan2(vel.z, vel.x) * (180.0D / Math.PI)) - 90.0F;
                this.setYRot(Mth.rotLerp(0.16F, this.getYRot(), yaw));
                this.yBodyRot = this.getYRot();
            }
            this.move(MoverType.SELF, vel);
            this.setDeltaMovement(this.getDeltaMovement().scale(0.90D));
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hit = super.hurt(source, amount);
        if (hit && !this.level().isClientSide) {
            Entity attacker = source.getEntity();
            if (this.inkCooldown <= 0) {
                this.inkCooldown = 160;
                this.setTarget(null);
                this.entityData.set(DATA_JET_TICKS, 22);
                this.playSound(SoundEvents.GLOW_SQUID_SQUIRT, 1.0F, 0.85F);
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 0.5D, this.getZ(),
                            20, 0.55D, 0.45D, 0.55D, 0.04D);
                    sl.sendParticles(ParticleTypes.GLOW_SQUID_INK, this.getX(), this.getY() + 0.5D, this.getZ(),
                            18, 0.65D, 0.55D, 0.65D, 0.05D);
                }
                Vec3 away = attacker != null
                        ? this.position().subtract(attacker.position()).normalize()
                        : new Vec3(this.random.nextDouble() - 0.5D, -0.4D, this.random.nextDouble() - 0.5D).normalize();
                this.setDeltaMovement(away.x * 0.95D, Math.min(away.y * 0.6D, -0.18D), away.z * 0.95D);
                if (attacker instanceof LivingEntity living && this.distanceToSqr(living) <= 14.0D) {
                    living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0), this);
                }
            } else if (attacker instanceof LivingEntity living && !(attacker instanceof Player p && p.isCreative())) {
                // Cornered while ink is on cooldown: lash once in self-defense!
                this.setTarget(living);
            }
        }
        return hit;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        this.spawnAtLocation(new ItemStack(ModItems.ABYSSAL_INK_SAC.get(), 1 + this.random.nextInt(2 + looting)));
        if (this.random.nextBoolean()) {
            this.spawnAtLocation(new ItemStack(Items.GLOW_INK_SAC));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GLOW_SQUID_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GLOW_SQUID_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GLOW_SQUID_DEATH;
    }
}
