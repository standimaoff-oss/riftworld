package ru.aiforge.riftworld.entity.fauna;

import java.util.List;
import javax.annotation.Nullable;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.entity.ElGranMajaEntity;
import ru.aiforge.riftworld.registry.ModItems;

/**
 * 15. Серебристый морской змей ({@code silver_sea_serpent}) — благородный нейтральный морской дракон
 * Открытого океана Элисара ({@code maja_ocean} / глубоководье), не конкурирующий с {@link ElGranMajaEntity}.
 *
 * <p>Быстро и плавно рассекает воду бегущей S-волной. Атакует только в ответ на провокацию:
 * укус челюстями спереди (7 урона) и хлещущий удар хвоста в заднем секторе (5 урона + отбрасывание).
 * Через 7 секунд без новых ударов гордо прекращает преследование; при падении HP {@code <= 25%}
 * сбрасывает 1–2 {@code sea_scale} и стремительно уходит на глубину.
 */
public class SilverSeaSerpentEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> DATA_BITE_TICKS =
            SynchedEntityData.defineId(SilverSeaSerpentEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TAIL_SLAP_TICKS =
            SynchedEntityData.defineId(SilverSeaSerpentEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_RETREAT_TICKS =
            SynchedEntityData.defineId(SilverSeaSerpentEntity.class, EntityDataSerializers.INT);

    @Nullable
    private Vec3 patrolPoint;
    private int biteCooldown = 0;
    private int tailSlapCooldown = 0;
    private int ticksSinceProvoked = 0;
    private boolean hasShedScales = false;

    public SilverSeaSerpentEntity(EntityType<? extends SilverSeaSerpentEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.42D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.45D)
                .add(Attributes.FOLLOW_RANGE, 28.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_BITE_TICKS, 0);
        this.entityData.define(DATA_TAIL_SLAP_TICKS, 0);
        this.entityData.define(DATA_RETREAT_TICKS, 0);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    public int getBiteTicks() {
        return this.entityData.get(DATA_BITE_TICKS);
    }

    public int getTailSlapTicks() {
        return this.entityData.get(DATA_TAIL_SLAP_TICKS);
    }

    public int getRetreatTicks() {
        return this.entityData.get(DATA_RETREAT_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        boolean inWater = ElisarFaunaUtil.isSubmergedInAnyWater(this);
        this.setNoGravity(inWater);
        if (this.level().isClientSide) return;

        if (this.biteCooldown > 0) --this.biteCooldown;
        if (this.tailSlapCooldown > 0) --this.tailSlapCooldown;
        int bite = this.getBiteTicks();
        if (bite > 0) this.entityData.set(DATA_BITE_TICKS, bite - 1);
        int slap = this.getTailSlapTicks();
        if (slap > 0) this.entityData.set(DATA_TAIL_SLAP_TICKS, slap - 1);
        int retreat = this.getRetreatTicks();
        if (retreat > 0) this.entityData.set(DATA_RETREAT_TICKS, retreat - 1);

        if (inWater) {
            this.setAirSupply(this.getMaxAirSupply());

            // Steer gently away from El Gran Maja so the ocean colossus keeps clean space
            if (this.tickCount % 30 == 0) {
                List<ElGranMajaEntity> majas = this.level().getEntitiesOfClass(
                        ElGranMajaEntity.class, this.getBoundingBox().inflate(72.0D));
                if (!majas.isEmpty()) {
                    Vec3 awayMaja = this.position().subtract(majas.get(0).position()).normalize().scale(18.0D);
                    this.patrolPoint = this.position().add(awayMaja.x, -2.0D, awayMaja.z);
                }
            }

            if (retreat > 0) {
                this.setTarget(null);
                Vec3 dive = Vec3.directionFromRotation(28.0F, this.getYRot()).scale(0.085D);
                this.setDeltaMovement(this.getDeltaMovement().scale(0.88D).add(dive.x, -0.055D, dive.z));
            } else {
                LivingEntity target = this.getTarget();
                if (target != null && target.isAlive()) {
                    if (++this.ticksSinceProvoked > 140) {
                        // Proud disengage after 7s unprovoked
                        this.setTarget(null);
                    } else {
                        Vec3 toTarget = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).subtract(this.position());
                        double dist = Math.max(0.01D, toTarget.length());
                        Vec3 forward = Vec3.directionFromRotation(0.0F, this.getYRot());
                        double dot = forward.dot(toTarget.scale(1.0D / dist));

                        // Rear-sector tail slap if attacker flanks behind the serpent
                        if (dot < -0.10D && dist <= 4.3D && this.tailSlapCooldown <= 0) {
                            this.tailSlapCooldown = 90;
                            this.entityData.set(DATA_TAIL_SLAP_TICKS, 16);
                            target.hurt(this.damageSources().mobAttack(this), 5.0F);
                            Vec3 kb = toTarget.scale(1.0D / dist).scale(1.05D);
                            target.push(kb.x, 0.25D, kb.z);
                            this.playSound(SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.0F, 0.75F);
                        } else if (dist <= 3.3D && this.biteCooldown <= 0) {
                            this.biteCooldown = 28;
                            this.entityData.set(DATA_BITE_TICKS, 14);
                            this.doHurtTarget(target);
                            this.playSound(SoundEvents.PHANTOM_BITE, 1.0F, 0.65F);
                        }

                        this.setDeltaMovement(this.getDeltaMovement().scale(0.86D).add(toTarget.scale(0.072D / dist)));
                    }
                } else {
                    if (this.patrolPoint == null || this.position().distanceToSqr(this.patrolPoint) < 9.0D || this.tickCount % 70 == 0) {
                        Vec3 cand = this.position().add(
                                (this.random.nextDouble() - 0.5D) * 24.0D,
                                (this.random.nextDouble() - 0.5D) * 6.0D,
                                (this.random.nextDouble() - 0.5D) * 24.0D);
                        if (ElisarFaunaUtil.isAnyWater(this.level(), BlockPos.containing(cand))) {
                            this.patrolPoint = cand;
                        }
                    }
                    if (this.patrolPoint != null) {
                        Vec3 d = this.patrolPoint.subtract(this.position());
                        double len = Math.max(0.01D, d.length());
                        this.setDeltaMovement(this.getDeltaMovement().scale(0.89D).add(d.scale(0.048D / len)));
                    }
                }
            }

            Vec3 vel = this.getDeltaMovement();
            if (vel.horizontalDistanceSqr() > 0.001D) {
                float targetYaw = (float) (Mth.atan2(vel.z, vel.x) * (180.0D / Math.PI)) - 90.0F;
                // v0.49.0: поворот был резким (0.14 за тик = ~8°/тик), змея
                // дёргалась при смене курса. Скорость поворота теперь падает
                // с величиной ошибки курса: чем круче разворот, тем плавнее
                // он проходится, и длинное тело не складывается пополам.
                float yawError = Mth.wrapDegrees(targetYaw - this.getYRot());
                float turnLerp = Mth.clamp(0.055F + Math.abs(yawError) * 0.00055F, 0.055F, 0.14F);
                this.setYRot(Mth.rotLerp(turnLerp, this.getYRot(), targetYaw));
                this.yBodyRot = this.getYRot();
                float targetPitch = Mth.clamp((float) (-vel.y * 45.0D), -28.0F, 28.0F);
                this.setXRot(Mth.lerp(0.18F, this.getXRot(), targetPitch));
            }
            this.move(MoverType.SELF, vel);
        } else if (this.onGround() && this.tickCount % 14 == 0) {
            this.setDeltaMovement((this.random.nextDouble() - 0.5D) * 0.3D, 0.28D, (this.random.nextDouble() - 0.5D) * 0.3D);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hit = super.hurt(source, amount);
        if (hit && !this.level().isClientSide) {
            this.ticksSinceProvoked = 0;
            if (!this.hasShedScales && this.getHealth() > 0.0F && this.getHealth() <= this.getMaxHealth() * 0.25F) {
                this.hasShedScales = true;
                this.setTarget(null);
                this.entityData.set(DATA_RETREAT_TICKS, 160);
                this.spawnAtLocation(new ItemStack(ModItems.SEA_SCALE.get(), 1 + this.random.nextInt(2)));
                this.playSound(SoundEvents.ELDER_GUARDIAN_AMBIENT, 1.1F, 1.35F);
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.7D, this.getZ(),
                            24, 0.8D, 0.4D, 0.8D, 0.08D);
                }
            } else if (this.getRetreatTicks() <= 0) {
                Entity attacker = source.getEntity();
                if (attacker instanceof LivingEntity living && !(attacker instanceof Player p && p.isCreative())) {
                    this.setTarget(living);
                }
            }
        }
        return hit;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        this.spawnAtLocation(new ItemStack(ModItems.SEA_SCALE.get(), 2 + this.random.nextInt(2 + looting)));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.DOLPHIN_AMBIENT_WATER;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ELDER_GUARDIAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ELDER_GUARDIAN_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("HasShedScales", this.hasShedScales);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.hasShedScales = tag.getBoolean("HasShedScales");
    }
}
