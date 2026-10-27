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
    public static final int SILVER = 0, GOLD = 1, STRIPED = 2, JELLY = 3, RAY = 4, ANGLER = 5, EEL = 6,
            SURGEON = 7, TURTLE = 8, GLOOM = 9, STING = 10;
    public static final String[] NAMES = {"silver_school", "gold_disc", "striped", "jelly", "ray", "angler", "eel",
            "surgeon", "turtle", "gloom", "sting"};

    /** Мирные виды моря (стаями и в одиночку) — «добрые рыбы». */
    public static final int[] PEACEFUL = {SILVER, GOLD, STRIPED, JELLY, RAY, SURGEON, TURTLE};
    /** Хищники затопленных пещер — «злые и редкие»: спавнятся только под дном, в темноте. */
    public static final int[] CAVE_HOSTILE = {ANGLER, EEL, GLOOM, STING};

    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(SeaFishEntity.class, EntityDataSerializers.INT);

    /** Точка дома (косяк не разбредается по всему морю). */
    private Vec3 home;
    private Vec3 way;
    private int wayAge;
    private int attackCd;
    /** Пауза между перецеливаниями при испуге (иначе рыба «дёргается» каждые 6 тиков). */
    private int fleeCd;
    /**
     * Рев 13 — курс рыбы как отдельное состояние. Раньше направление каждый тик заново
     * смешивалось из скорости и «желания» (0.35/0.65), из-за чего на смене точки маршрута
     * рыба щёлкала носом на 60–120°. Теперь есть {@link #heading}: он доворачивается
     * к цели с ограниченной угловой скоростью и никогда не разворачивается мгновенно.
     */
    private Vec3 heading;
    /** Текущая скорость (блоков/тик) — тоже подтягивается плавно, без ступенек. */
    private double speed;
    /** Максимальный поворот курса, °/тик: у мирных плавнее, у хищников резче, но тоже без рывков. */
    private static final double TURN_PEACE = 2.4D, TURN_HUNT = 4.2D;
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
        return v == ANGLER || v == EEL || v == GLOOM || v == STING;
    }

    /** Особо редкие виды: глубинный глот, жгучая медуза и гигантская манта. */
    public boolean rare() {
        int v = variant();
        return v == GLOOM || v == STING || v == RAY;
    }

    /** Размер тела (рендер и хитбокс): черепаха и манта крупнее мелочи. */
    public float scale() {
        int v = variant();
        return v == TURTLE ? 1.7F : v == RAY ? 2.1F : v == GLOOM ? 1.5F : v == JELLY ? 1.15F : 1.0F;
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
        if (isNoAi()) {                                   // витрина/декор: держим позицию, только покачивание
            setDeltaMovement(0, Math.sin(tickCount * 0.06) * 0.006, 0);
            return;
        }

        if (fleeCd > 0) --fleeCd;
        // мирные шарахаются от плывущего игрока: подплыл близко — косяк уходит в сторону.
        // Рев 12: цель меняется не чаще раза в 2.5 секунды, иначе рыба вертелась на месте.
        if (!hostile() && fleeCd <= 0 && tickCount % 10 == 0) {
            Player near = level().getNearestPlayer(this, 5.0D);
            if (near != null && !near.isSpectator()) {
                Vec3 away = position().subtract(near.position());
                if (away.lengthSqr() < 1e-4) away = new Vec3(1, 0, 0);
                way = position().add(away.normalize().scale(9)).add(0, (random.nextDouble() - 0.5) * 1.5, 0);
                wayAge = 70;
                fleeCd = 50;
            }
        }

        // хищник ищет игрока в воде рядом
        Player prey = hostile() ? level().getNearestPlayer(this, 16.0D) : null;
        if (prey != null && !prey.isCreative() && !prey.isSpectator() && prey.isInWater()) {
            Vec3 to = prey.position().add(0, 0.6, 0).subtract(position());
            double d = to.length();
            if (d > 0.6 && d < 16) {
                chase(to.normalize(), 0.42);
                if (d < 2.2 && attackCd <= 0) {
                    float dmg = switch (variant()) {
                        case ANGLER -> 5.0F;
                        case GLOOM -> 7.0F;
                        case STING -> 3.0F;
                        default -> 3.5F;
                    };
                    prey.hurt(damageSources().mobAttack(this), dmg);
                    attackCd = variant() == GLOOM ? 46 : variant() == ANGLER ? 34 : 26;
                    level().playSound(null, blockPosition(), SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 0.7F, 0.8F);
                    if (prey instanceof net.minecraft.server.level.ServerPlayer sp) {
                        sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, variant() == STING ? 110 : 60,
                                variant() == STING ? 1 : 0));
                        if (variant() == STING) {
                            sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                    net.minecraft.world.effect.MobEffects.POISON, 80, 0));
                        }
                    }
                }
                faceTravel();
                return;
            }
        }

        // мирное плавание вокруг дома
        // Рев 12: точка выбирается дальше и живёт дольше (4–11 секунд), а подходя к ней рыба
        // плавно сбрасывает скорость — раньше она проскакивала точку и разворачивалась рывком.
        if (wayAge > 0) --wayAge;
        if (way == null) wayAge = 0;
        if (wayAge <= 0) {
            double radius = isRay() ? 18 : (hostile() ? 13 : 10);
            double a = random.nextDouble() * Math.PI * 2;
            double r = radius * (0.45 + random.nextDouble() * 0.55);
            double y = home.y + (random.nextDouble() - 0.5) * (isRay() ? 6 : 3);
            way = new Vec3(home.x + Math.cos(a) * r, y, home.z + Math.sin(a) * r);
            wayAge = 80 + random.nextInt(140);
        }
        Vec3 dv = way.subtract(position());
        double dist = dv.length();
        double cruise = isRay() ? 0.10 : (hostile() ? 0.17 : 0.20);
        // мягкое торможение у цели: за 3 блока скорость падает, поворот не нужен вовсе
        double speed = cruise * Mth.clamp(dist / 3.0, 0.25, 1.0);
        if (dist < 1.1) { way = null; wayAge = 20; }
        else chase(dv.normalize(), speed);
        faceTravel();

        // мягко держимся в воде: не выпрыгиваем и не утыкаемся в породу.
        // Рев 13: вне воды — плавное погружение, без «клевка» вниз (раньше рыба дёргалась у поверхности).
        if (!inWater()) {
            setDeltaMovement(getDeltaMovement().scale(0.94).add(0, -0.012, 0));
        }
    }

    /**
     * Рев 13: собственный {@link #travel}. Ванильный {@code Mob.travel} применяет «сухую» физику
     * (трение о землю, шаг вверх, гравитацию), из-за чего рыба в воде то рывком тормозила,
     * то прыгала. Нам нужно только одно: двигаться ровно туда, куда ведёт курс.
     */
    @Override
    public void travel(Vec3 input) {
        Vec3 v = getDeltaMovement();
        move(MoverType.SELF, v);
        if (horizontalCollision) setDeltaMovement(getDeltaMovement().multiply(0.35D, 1.0D, 0.35D));
        if (verticalCollision) setDeltaMovement(getDeltaMovement().multiply(1.0D, 0.35D, 1.0D));
        setDeltaMovement(getDeltaMovement().scale(0.96D));
    }

    /**
     * Рев 12: руление вместо «телепорта курса». Раньше скорость подмешивалась к текущей с весом 0.25,
     * а на столкновении рыба получала импульс строго назад — отсюда резкие развороты в кадре.
     * Теперь: (1) направление доворачивается не быстрее {@code TURN_*}°/тик, (2) модуль скорости
     * подтягивается плавно, (3) у препятствия рыба выбирает обход, а не отскакивает.
     */
    private void chase(Vec3 dir, double targetSpeed) {
        Vec3 want = dir.lengthSqr() < 1e-8 ? (heading != null ? heading : new Vec3(0, 0, 1)) : dir.normalize();
        double maxTurn = hostile() ? TURN_HUNT : TURN_PEACE;
        // было ли столкновение в прошлом тике — тогда доворачиваем чуть смелее, но всё равно плавно
        if (horizontalCollision || verticalCollision) maxTurn *= 1.8D;
        if (heading == null) heading = want;
        heading = limitTurn(heading, want, maxTurn);
        // скорость подтягиваем к целевой, а не ставим скачком
        speed += (targetSpeed - speed) * 0.08D;
        setDeltaMovement(heading.scale(Math.max(0.004D, speed)));
    }

    /** Поворот вектора {@code from} к {@code to} не более чем на {@code maxDeg} градусов (Родриг). */
    private static Vec3 limitTurn(Vec3 from, Vec3 to, double maxDeg) {
        double cos = Mth.clamp(from.dot(to), -1.0D, 1.0D);
        double ang = Math.toDegrees(Math.acos(cos));
        if (ang <= maxDeg || ang < 1.0E-3) return to;
        Vec3 axis = from.cross(to);
        if (axis.lengthSqr() < 1.0E-8) {
            // ровно назад: поворот через вертикаль, чтобы не «щёлкать» на 180°
            axis = from.cross(new Vec3(0, 1, 0));
            if (axis.lengthSqr() < 1.0E-8) axis = new Vec3(1, 0, 0);
        }
        axis = axis.normalize();
        double a = Math.toRadians(maxDeg);
        return from.scale(Math.cos(a)).add(axis.cross(from).scale(Math.sin(a))).normalize();
    }

    /**
     * Корпус доворачивается за скоростью с ограничением угловой скорости: раньше {@code rotLerp(0.18)}
     * поворачивал модель на 18% остатка за тик — это 60° за полсекунды, и на видео это читалось как
     * «дёрганье». Теперь не быстрее 1.8°/тик по рысканью и 0.9°/тик по тангажу.
     */
    private void faceTravel() {
        Vec3 v = heading != null && heading.lengthSqr() > 1e-6 ? heading : getDeltaMovement();
        if (v.lengthSqr() < 1e-5) return;
        float yaw = (float) (Math.atan2(-v.x, v.z) * Mth.RAD_TO_DEG);
        float dYaw = Mth.wrapDegrees(yaw - getYRot());
        // 1.1°/тик — это ~16°/с: корпус больше не «щёлкает» на смене маршрута
        setYRot(getYRot() + Mth.clamp(dYaw, -1.1F, 1.1F));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        float pitch = (float) -(Math.atan2(v.y, Math.hypot(v.x, v.z)) * Mth.RAD_TO_DEG);
        setXRot(getXRot() + Mth.clamp(pitch - getXRot(), -0.45F, 0.45F));
    }

    // ------------------------------------------------------------------ спавн/сохранение
    /** Ставит рыбу в точку (только сервер). */
    public static SeaFishEntity spawn(net.minecraft.server.level.ServerLevel sl, Vec3 at, int variant) {
        SeaFishEntity f = new SeaFishEntity(ModEntities.SEA_FISH.get(), sl);
        f.setVariant(variant);
        f.setPos(at.x, at.y, at.z);
        f.home = at;
        float hp = switch (variant) {
            case RAY -> 20F;
            case TURTLE -> 26F;
            case ANGLER -> 16F;
            case GLOOM -> 22F;
            case STING -> 8F;
            default -> 10F;
        };
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
