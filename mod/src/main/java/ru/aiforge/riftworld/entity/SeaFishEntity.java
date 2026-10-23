package ru.aiforge.riftworld.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.flight.Flight;
import ru.aiforge.riftworld.registry.ElisarBlocks;
import ru.aiforge.riftworld.registry.ModEntities;

/**
 * Рев 8 «Живое море»: одна сущность на всю ихтиофауну Элисара — вариант задаётся данными.
 *
 * <p>Мирные (море, рифы, ламинария): <b>серебристая стайная</b>, <b>золотой диск</b>, <b>полосатая</b>,
 * <b>медуза</b> (светится), <b>манта</b> (редкая, крупная).
 * <p>Злые и редкие (затопленные пещеры, темнота): <b>удильщик</b> (светящаяся приманка, кусает из темноты),
 * <b>бледный угорь</b> (длинный, цепкий).
 *
 * <p>Физика — без гравитации, как у бумажного дракона: рыба сама ведёт себя в воде и мягко обходит породу.
 * Вся анимация (хвост, плавники, свечение) — на клиенте по скорости и времени, пакетов не тратит.
 */
public class SeaFishEntity extends Mob {
    public static final int SILVER = 0, GOLD = 1, STRIPED = 2, JELLY = 3, RAY = 4, ANGLER = 5, EEL = 6;
    public static final String[] NAMES = {"silver_school", "gold_disc", "striped", "jelly", "ray", "angler", "eel"};

    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(SeaFishEntity.class, EntityDataSerializers.INT);

    /** Точка дома (косяк не разбредается по всему морю). */
    private Vec3 home;
    private Vec3 way;
    private int wayAge;
    private int attackCd;
    public float tail, tail0;
    public float glowPhase;

    public SeaFishEntity(EntityType<? extends SeaFishEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
        noCulling = true;
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 18.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_VARIANT, SILVER);
    }

    public int variant() {
        return entityData.get(DATA_VARIANT);
    }

    public void setVariant(int v) {
        entityData.set(DATA_VARIANT, Mth.clamp(v, 0, NAMES.length - 1));
    }

    public boolean hostile() {
        int v = variant();
        return v == ANGLER || v == EEL;
    }

    public boolean isJelly() {
        return variant() == JELLY;
    }

    public boolean isRay() {
        return variant() == RAY;
    }

    // ------------------------------------------------------------------ вода
    /** Вода Элисара (своя жидкость, а не ванильная). */
    public static boolean elisarWater(Level level, BlockPos p) {
        return level.getBlockState(p).is(ElisarBlocks.WATER.get());
    }

    public boolean inWater() {
        return elisarWater(level(), blockPosition()) || elisarWater(level(), blockPosition().above());
    }

    // ------------------------------------------------------------------ поведение
    @Override
    public void tick() {
        super.tick();
        tail0 = tail;
        glowPhase += 0.05F;
        if (level().isClientSide) {
            tail += 0.18F + (float) Math.min(0.5, getDeltaMovement().length() * 1.6F);
            return;
        }
        if (level().dimension() != Flight.ELISAR_DIM) {           // вне Элисара рыба не живёт
            if (tickCount > 200) discard();
            return;
        }
        if (home == null) home = position();
        if (attackCd > 0) --attackCd;

        // хищник ищет игрока в воде рядом
        Player prey = hostile() ? level().getNearestPlayer(this, 16.0D) : null;
        if (prey != null && !prey.isCreative() && !prey.isSpectator() && prey.isInWater()) {
            Vec3 to = prey.position().add(0, 0.6, 0).subtract(position());
            double d = to.length();
            if (d > 0.6 && d < 16) {
                chase(to.normalize(), 0.42);
                if (d < 2.2 && attackCd <= 0) {
                    prey.hurt(damageSources().mobAttack(this), variant() == ANGLER ? 5.0F : 3.5F);
                    attackCd = variant() == ANGLER ? 34 : 26;
                    level().playSound(null, blockPosition(), SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 0.7F, 0.8F);
                    if (prey instanceof net.minecraft.server.level.ServerPlayer sp) {
                        sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
                    }
                }
                faceTravel();
                return;
            }
        }

        // мирное плавание вокруг дома
        if (way == null || --wayAge <= 0) {
            double radius = isRay() ? 16 : (hostile() ? 12 : 9);
            double a = random.nextDouble() * Math.PI * 2;
            double r = radius * (0.35 + random.nextDouble() * 0.65);
            double y = home.y + (random.nextDouble() - 0.5) * (isRay() ? 7 : 4);
            way = new Vec3(home.x + Math.cos(a) * r, y, home.z + Math.sin(a) * r);
            wayAge = 60 + random.nextInt(90);
        }
        Vec3 dv = way.subtract(position());
        if (dv.length() < 1.2) { way = null; wayAge = 8; }
        else chase(dv.normalize(), isRay() ? 0.10 : (hostile() ? 0.17 : 0.20));
        faceTravel();

        // мягко держимся в воде: не выпрыгиваем и не утыкаемся в породу
        if (!inWater()) {
            setDeltaMovement(getDeltaMovement().scale(0.85).add(0, -0.03, 0));
        }
    }

    private void chase(Vec3 dir, double speed) {
        Vec3 want = dir.scale(speed);
        Vec3 vel = getDeltaMovement().add(want.subtract(getDeltaMovement()).scale(0.25));
        if (vel.length() > speed * 1.4) vel = vel.normalize().scale(speed * 1.4);
        setDeltaMovement(vel);
        move(MoverType.SELF, vel);
        // упёрлись в породу — ищем другой путь, а не долбимся в стену
        if (horizontalCollision || verticalCollision) {
            way = null;
            wayAge = 4;
            setDeltaMovement(getDeltaMovement().scale(-0.25));
        }
    }

    private void faceTravel() {
        Vec3 v = getDeltaMovement();
        if (v.lengthSqr() < 1e-5) return;
        float yaw = (float) (Math.atan2(-v.x, v.z) * Mth.RAD_TO_DEG);
        setYRot(Mth.rotLerp(0.18F, getYRot(), yaw));
        yBodyRot = getYRot();
        setXRot(Mth.lerp(0.15F, getXRot(), (float) -(Math.atan2(v.y, Math.hypot(v.x, v.z)) * Mth.RAD_TO_DEG)));
    }

    // ------------------------------------------------------------------ спавн/сохранение
    /** Ставит рыбу в точку (только сервер). */
    public static SeaFishEntity spawn(net.minecraft.server.level.ServerLevel sl, Vec3 at, int variant) {
        SeaFishEntity f = new SeaFishEntity(ModEntities.SEA_FISH.get(), sl);
        f.setVariant(variant);
        f.setPos(at.x, at.y, at.z);
        f.home = at;
        float hp = variant == RAY ? 20F : variant == ANGLER ? 16F : 10F;
        f.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        f.setHealth(hp);
        sl.addFreshEntity(f);
        return f;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType type,
                                        SpawnGroupData data, CompoundTag tag) {
        return super.finalizeSpawn(level, diff, type, data, tag);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("sf_variant", variant());
        if (home != null) {
            tag.putDouble("sf_hx", home.x);
            tag.putDouble("sf_hy", home.y);
            tag.putDouble("sf_hz", home.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(tag.getInt("sf_variant"));
        if (tag.contains("sf_hx")) home = new Vec3(tag.getDouble("sf_hx"), tag.getDouble("sf_hy"), tag.getDouble("sf_hz"));
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        boolean hit = super.hurt(src, amount);
        if (hit && !level().isClientSide && !hostile()) {
            // мирные удирают от обидчика, а не тычутся рядом
            if (src.getEntity() != null) {
                Vec3 away = position().subtract(src.getEntity().position()).normalize();
                home = position().add(away.scale(10));
                way = home;
                wayAge = 40;
            }
        }
        return hit;
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
    public boolean removeWhenFarAway(double d) {
        return false;
    }
}
