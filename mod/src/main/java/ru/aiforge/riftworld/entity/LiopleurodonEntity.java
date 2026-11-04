package ru.aiforge.riftworld.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.registry.ElisarBlocks;
import ru.aiforge.riftworld.registry.ModEntities;

/**
 * v0.32.0 — <b>Лиоплевродон</b> ({@code riftworld:liopleurodon}), хозяин глубин Элисара.
 *
 * <p>По концепту `reports/mob_refs/03_ref_liopleurodon.png` и роадмапу
 * `reports/ROADMAP_v0.31_plus.md`: 11–14 блоков в длину, засадник, а не «милая рыбка».
 * <ul>
 *   <li>патрулирует на глубине (≥ 30 блоков воды) вокруг своей точки дома;</li>
 *   <li>игрока в воде в радиусе 24 блоков берёт в прицел и делает рывок (до 9 блок/с);</li>
 *   <li>укус — 6 ❤, хватает и тянет вниз на 2 с (медленность + «утопление» темпа);</li>
 *   <li>вне воды не живёт: без воды плавно тонет и через 20 с исчезает;</li>
 *   <li>никаких надписей в чате — только звук.</li>
 * </ul>
 * Физика как у морских рыб мода: без гравитации, поворот корпуса с ограничением угловой скорости.
 */
public class LiopleurodonEntity extends Mob {
    private static final EntityDataAccessor<Boolean> DATA_BITING =
            SynchedEntityData.defineId(LiopleurodonEntity.class, EntityDataSerializers.BOOLEAN);
    private int biteAnimationTicks;

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_BITING, false);
    }

    public boolean isBiting() { return this.entityData.get(DATA_BITING); }

    /** Точка дома: хищник не уходит из своей впадины через весь океан. */
    private Vec3 home;
    private Vec3 way;
    private int wayAge;
    private int attackCd;
    private int grabTicks;
    private Vec3 heading;
    private double speed;
    private int outOfWater;

    private static final double TURN_HUNT = 3.2D;

    public LiopleurodonEntity(EntityType<? extends LiopleurodonEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        this.xpReward = 40;
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 90.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.FOLLOW_RANGE, 36.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85D);
    }

    /** Вода Элисара — своя жидкость, не ванильная. */
    public static boolean elisarWater(Level level, BlockPos p) {
        return level.getBlockState(p).is(ElisarBlocks.WATER.get());
    }

    public boolean inWater() {
        return isInWaterOrBubble() || elisarWater(level(), blockPosition()) || elisarWater(level(), blockPosition().above());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (home == null) home = position();
        if (attackCd > 0) --attackCd;
        if (biteAnimationTicks > 0 && --biteAnimationTicks == 0) this.entityData.set(DATA_BITING, false);
        if (grabTicks > 0) --grabTicks;

        if (isNoAi()) {                                   // витрина: только покачивание
            setDeltaMovement(0, Math.sin(tickCount * 0.05) * 0.004, 0);
            return;
        }

        if (!inWater()) {
            setDeltaMovement(getDeltaMovement().scale(0.9).add(0, -0.02, 0));
            if (++outOfWater > 400) discard();
            return;
        }
        outOfWater = 0;

        Player prey = null;
        double nearest = 24.0D * 24.0D;
        for (Player candidate : level().players()) {
            double d2 = distanceToSqr(candidate);
            if (candidate.isAlive() && !candidate.isCreative() && !candidate.isSpectator() && d2 < nearest) {
                nearest = d2;
                prey = candidate;
            }
        }
        // Elisar has its own liquid; vanilla isInWater() alone does not
        // necessarily recognise a player swimming in it. Daylight must not
        // suppress hunting in the open ocean.
        boolean preyInWater = prey != null && (prey.isInWaterOrBubble() || prey.isSwimming()
                || elisarWater(level(), prey.blockPosition())
                || elisarWater(level(), prey.blockPosition().above()));
        if (preyInWater) {
            Vec3 to = prey.position().add(0, 0.8, 0).subtract(position());
            double d = to.length();
            if (d < 24) {
                chase(to.normalize(), d < 8 ? 0.62 : 0.45);
                if (d < 7.0 && attackCd <= 0 && hasLineOfSight(prey)) {
                    bite(prey);
                }
                faceTravel();
                return;
            }
        }

        // патруль вокруг дома
        if (wayAge > 0) --wayAge;
        if (way == null) wayAge = 0;
        if (wayAge <= 0) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = 12 + random.nextDouble() * 20;
            way = new Vec3(home.x + Math.cos(a) * r, home.y + (random.nextDouble() - 0.5) * 8,
                    home.z + Math.sin(a) * r);
            wayAge = 160 + random.nextInt(200);
        }
        Vec3 dv = way.subtract(position());
        double dist = dv.length();
        if (dist < 1.4) {
            way = null;
            wayAge = 40;
        } else {
            chase(dv.normalize(), 0.22 * Mth.clamp(dist / 6.0, 0.3, 1.0));
        }
        faceTravel();
    }

    private void bite(Player prey) {
        prey.hurt(damageSources().mobAttack(this), 12.0F);
        attackCd = 50;
        biteAnimationTicks = 12;
        this.entityData.set(DATA_BITING, true);
        grabTicks = 40;
        level().playSound(null, blockPosition(), SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 1.0F, 0.55F);
        prey.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
        // «тянет вниз»: импульс вниз + короткая потеря дыхания у поверхности не нужна — тянем движением
        Vec3 pull = new Vec3(0, -0.55, 0);
        prey.setDeltaMovement(prey.getDeltaMovement().add(pull));
        prey.hurtMarked = true;
    }

    @Override
    public void travel(Vec3 input) {
        move(MoverType.SELF, getDeltaMovement());
        if (horizontalCollision) setDeltaMovement(getDeltaMovement().multiply(0.4D, 1.0D, 0.4D));
        if (verticalCollision) setDeltaMovement(getDeltaMovement().multiply(1.0D, 0.4D, 1.0D));
        setDeltaMovement(getDeltaMovement().scale(0.955D));
    }

    private void chase(Vec3 dir, double targetSpeed) {
        Vec3 want = dir.lengthSqr() < 1e-8 ? (heading != null ? heading : new Vec3(0, 0, 1)) : dir.normalize();
        if (heading == null) heading = want;
        heading = limitTurn(heading, want, TURN_HUNT);
        speed += (targetSpeed - speed) * 0.07D;
        setDeltaMovement(heading.scale(Math.max(0.004D, speed)));
    }

    private static Vec3 limitTurn(Vec3 from, Vec3 to, double maxDeg) {
        double cos = Mth.clamp(from.dot(to), -1.0D, 1.0D);
        double ang = Math.toDegrees(Math.acos(cos));
        if (ang <= maxDeg || ang < 1.0E-3) return to;
        Vec3 axis = from.cross(to);
        if (axis.lengthSqr() < 1.0E-8) {
            axis = from.cross(new Vec3(0, 1, 0));
            if (axis.lengthSqr() < 1.0E-8) axis = new Vec3(1, 0, 0);
        }
        axis = axis.normalize();
        double a = Math.toRadians(maxDeg);
        return from.scale(Math.cos(a)).add(axis.cross(from).scale(Math.sin(a))).normalize();
    }

    private void faceTravel() {
        Vec3 v = heading != null && heading.lengthSqr() > 1e-6 ? heading : getDeltaMovement();
        if (v.lengthSqr() < 1e-5) return;
        float yaw = (float) (Math.atan2(-v.x, v.z) * Mth.RAD_TO_DEG);
        float dYaw = Mth.wrapDegrees(yaw - getYRot());
        setYRot(getYRot() + Mth.clamp(dYaw, -1.1F, 1.1F));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        float pitch = (float) -(Math.atan2(v.y, Math.hypot(v.x, v.z)) * Mth.RAD_TO_DEG);
        setXRot(getXRot() + Mth.clamp(pitch - getXRot(), -0.6F, 0.6F));
    }

    // ------------------------------------------------------------------ прочее
    /** Поставить взрослого лиоплевродона в точку (сервер). */
    public static LiopleurodonEntity spawn(net.minecraft.server.level.ServerLevel sl, Vec3 at) {
        LiopleurodonEntity l = new LiopleurodonEntity(ModEntities.LIOPLEURODON.get(), sl);
        l.setPos(at.x, at.y, at.z);
        l.home = at;
        l.setHealth(90.0F);
        sl.addFreshEntity(l);
        return l;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) {
            tag.putDouble("lp_hx", home.x);
            tag.putDouble("lp_hy", home.y);
            tag.putDouble("lp_hz", home.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("lp_hx")) {
            home = new Vec3(tag.getDouble("lp_hx"), tag.getDouble("lp_hy"), tag.getDouble("lp_hz"));
        }
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        boolean hit = super.hurt(src, amount);
        if (hit && !level().isClientSide && src.getEntity() != null) {
            Vec3 away = position().subtract(src.getEntity().position());
            if (away.lengthSqr() > 1e-4) {
                way = position().add(away.normalize().scale(14));
                wayAge = 60;
            }
        }
        return hit;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int looting, boolean hitByPlayer) {
        super.dropCustomDeathLoot(src, looting, hitByPlayer);
        spawnAtLocation(new net.minecraft.world.item.ItemStack(
                ru.aiforge.riftworld.registry.ModItems.LIOPLEURODON_TOOTH.get()), 0.4F);
        float chance = 0.6F + 0.1F * looting;
        if (random.nextFloat() < chance) {
            spawnAtLocation(new net.minecraft.world.item.ItemStack(
                    ru.aiforge.riftworld.registry.ModItems.SEA_SCALE.get(), 1 + random.nextInt(2)), 0.4F);
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity e) {
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distSq) {
        return !this.isNoAi() && !this.hasCustomName() && distSq > 128.0D * 128.0D;
    }
}
