package ru.aiforge.riftworld.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.world.CityPlan;
import ru.aiforge.riftworld.world.ElisarChunkGenerator;

/**
 * Крупный супер-детализированный модульный 3D оригами-дракон в императорском китайском стиле (~26 м с головой и хвостом).
 *
 * <p>4-фазный маршрут плавного полёта вокруг парящих островов Элисара:
 * <ol>
 *   <li><b>ПОД островом ({@link #FLIGHT_UNDER_ISLAND})</b> — плавное скольжение под каменным сводом и сталактитами острова;</li>
 *   <li><b>Восходящая спираль ({@link #FLIGHT_ASCEND_RIM})</b> — мягкий подъём вдоль скальной кромки острова к небу;</li>
 *   <li><b>НАД островом ({@link #FLIGHT_OVER_ISLAND})</b> — величественное парение высоко над кронами деревьев, крышами и вершинами острова;</li>
 *   <li><b>Нисходящее планирование ({@link #FLIGHT_DESCEND_RIM})</b> — плавный вираж вниз к подножию следующего острова.</li>
 * </ol>
 */
public class PaperDragonEntity extends FlyingMob {
    public static final int SEGS = 64;
    public static final float STEP = 0.34F; // ~21.8 м модульного тела + 2.6 м голова + 2.8 м веер хвоста (~27 м!)
    public static final int TRAIL_CAPACITY = 256;

    public static final int FLIGHT_UNDER_ISLAND = 0;
    public static final int FLIGHT_ASCEND_RIM = 1;
    public static final int FLIGHT_OVER_ISLAND = 2;
    public static final int FLIGHT_DESCEND_RIM = 3;

    private static final EntityDataAccessor<Float> DATA_PHASE =
            SynchedEntityData.defineId(PaperDragonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_FLIGHT_MODE =
            SynchedEntityData.defineId(PaperDragonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_BANK_ROLL =
            SynchedEntityData.defineId(PaperDragonEntity.class, EntityDataSerializers.FLOAT);

    // Кольцевой буфер мировой траектории полёта для реального следования 64 модульных колец по кривой пути!
    public final double[] trailX = new double[TRAIL_CAPACITY];
    public final double[] trailY = new double[TRAIL_CAPACITY];
    public final double[] trailZ = new double[TRAIL_CAPACITY];
    public final float[] trailRoll = new float[TRAIL_CAPACITY];
    public int trailHead = 0;
    public int trailCount = 0;

    // Пружинная физика крена, раскрытия пасти и изгиба усов на клиенте
    public float clientBankRoll = 0.0F;
    public float clientBankRollO = 0.0F;
    public float clientJawOpen = 0.0F;
    public float clientJawOpenO = 0.0F;
    private float prevYawForRoll = Float.NaN;

    @Nullable
    private Vec3 homeAnchor;
    @Nullable
    private Vec3 waypoint;
    private int waypointTimer = 0;
    private int modePhaseSteps = 0;
    private float orbitDirection = 1.0F;
    private double orbitAngle = 0.0;

    public PaperDragonEntity(EntityType<? extends PaperDragonEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.noCulling = true;
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.MOVEMENT_SPEED, 0.42)
                .add(Attributes.FLYING_SPEED, 0.56);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PHASE, 0.0F);
        this.entityData.define(DATA_FLIGHT_MODE, FLIGHT_UNDER_ISLAND);
        this.entityData.define(DATA_BANK_ROLL, 0.0F);
    }

    public float wavePhase() {
        return this.entityData.get(DATA_PHASE);
    }

    public int flightMode() {
        return this.entityData.get(DATA_FLIGHT_MODE);
    }

    public void setFlightMode(int mode) {
        this.entityData.set(DATA_FLIGHT_MODE, Mth.clamp(mode, FLIGHT_UNDER_ISLAND, FLIGHT_DESCEND_RIM));
        this.modePhaseSteps = 0;
        this.waypoint = null;
    }

    public void setHomeAnchor(Vec3 anchor, int initialFlightMode) {
        this.homeAnchor = anchor;
        this.orbitDirection = this.random.nextBoolean() ? 1.0F : -1.0F;
        this.orbitAngle = this.random.nextDouble() * Math.PI * 2.0;
        this.entityData.set(DATA_PHASE, this.random.nextFloat() * 6.283F);
        setFlightMode(initialFlightMode);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (this.homeAnchor == null) {
                this.homeAnchor = this.position();
                this.orbitDirection = this.random.nextBoolean() ? 1.0F : -1.0F;
                this.orbitAngle = this.random.nextDouble() * Math.PI * 2.0;
                this.entityData.set(DATA_PHASE, this.random.nextFloat() * 6.283F);
            }
            serverFlightTick();
        } else {
            clientPhysicsTick();
        }
    }

    private void clientPhysicsTick() {
        this.clientBankRollO = this.clientBankRoll;
        this.clientJawOpenO = this.clientJawOpen;

        float curYaw = this.getYRot();
        if (Float.isNaN(this.prevYawForRoll)) {
            this.prevYawForRoll = curYaw;
        }
        float yawVel = Mth.wrapDegrees(curYaw - this.prevYawForRoll);
        this.prevYawForRoll = curYaw;

        // Аэродинамический крен в сторону виража + плавное возвратно-пружинное демпфирование
        float targetRoll = Mth.clamp(-yawVel * 5.2F, -38.0F, 38.0F)
                + Mth.sin((this.tickCount + wavePhase() * 10.0F) * 0.085F) * 4.5F;
        this.clientBankRoll += (targetRoll - this.clientBankRoll) * 0.18F;

        // Живое дыхание и приоткрывание бумажной пасти на подъёмах и крутых виражах
        Vec3 v = this.getDeltaMovement();
        float climbBoost = Mth.clamp((float) v.y * 1.4F, 0.0F, 0.35F);
        float turnBoost = Math.min(0.25F, Math.abs(yawVel) * 0.04F);
        float breath = 0.08F + 0.07F * (0.5F + 0.5F * Mth.sin(this.tickCount * 0.09F + wavePhase()));
        float targetJaw = Mth.clamp(breath + climbBoost + turnBoost, 0.03F, 0.68F);
        this.clientJawOpen += (targetJaw - this.clientJawOpen) * 0.22F;

        recordClientTrail(this.getX(), this.getY(), this.getZ(), this.clientBankRoll);
    }

    private void recordClientTrail(double x, double y, double z, float rollDeg) {
        if (this.trailCount == 0) {
            // Инициализируем хвост развёрнутой плавной дугой позади головы, чтобы дракон никогда не появлялся сжатым комком!
            float rad = this.getYRot() * Mth.DEG_TO_RAD;
            double backX = Math.sin(rad);
            double backZ = -Math.cos(rad);
            int initPts = SEGS + 16;
            for (int k = initPts - 1; k >= 0; --k) {
                double dist = k * STEP;
                double sw = Math.sin(k * 0.24 + wavePhase()) * 0.45;
                double px = x + backX * dist + (-backZ) * sw;
                double py = y + Math.cos(k * 0.18 + wavePhase()) * 0.22;
                double pz = z + backZ * dist + backX * sw;
                this.trailHead = (this.trailHead + 1) % TRAIL_CAPACITY;
                this.trailX[this.trailHead] = px;
                this.trailY[this.trailHead] = py;
                this.trailZ[this.trailHead] = pz;
                this.trailRoll[this.trailHead] = 0.0F;
                if (this.trailCount < TRAIL_CAPACITY) ++this.trailCount;
            }
            return;
        }
        double dx = x - this.trailX[this.trailHead];
        double dy = y - this.trailY[this.trailHead];
        double dz = z - this.trailZ[this.trailHead];
        if (dx * dx + dy * dy + dz * dz < 0.0009) {
            this.trailRoll[this.trailHead] = rollDeg;
            return;
        }
        this.trailHead = (this.trailHead + 1) % TRAIL_CAPACITY;
        this.trailX[this.trailHead] = x;
        this.trailY[this.trailHead] = y;
        this.trailZ[this.trailHead] = z;
        this.trailRoll[this.trailHead] = rollDeg;
        if (this.trailCount < TRAIL_CAPACITY) {
            ++this.trailCount;
        }
    }

    private void serverFlightTick() {
        --this.waypointTimer;
        if (this.waypoint == null || this.waypointTimer <= 0 || this.position().distanceToSqr(this.waypoint) < 20.0) {
            pickNextWaypoint();
        }

        Vec3 cur = this.position();
        Vec3 toWp = this.waypoint.subtract(cur);
        double dist = Math.max(0.001, toWp.length());
        Vec3 desiredDir = toWp.scale(1.0 / dist);

        // Плавная синусоидальная волна серпантина в полёте
        double waveT = this.tickCount * 0.055 + wavePhase();
        Vec3 sideHoriz = new Vec3(-desiredDir.z, 0.0, desiredDir.x);
        if (sideHoriz.lengthSqr() > 1.0E-4) sideHoriz = sideHoriz.normalize();
        desiredDir = desiredDir
                .add(sideHoriz.scale(Math.sin(waveT) * 0.16))
                .add(0.0, Math.cos(waveT * 0.85) * 0.07, 0.0)
                .normalize();

        // Мягкое уклонение вперёд на 5, 10 и 16 блоков — дракон плавно огибает своды островов и скалы, не застревая!
        desiredDir = smoothAvoidObstacles(cur, desiredDir);

        double cruiseSpeed = (flightMode() == FLIGHT_OVER_ISLAND) ? 0.56 : 0.50;
        Vec3 targetVel = desiredDir.scale(cruiseSpeed);
        Vec3 newVel = this.getDeltaMovement().scale(0.86).add(targetVel.scale(0.14));

        // Гарантируем минимальную крейсерскую скорость, чтобы длинное тело всегда красиво струилось в воздухе
        double spd = newVel.length();
        if (spd < 0.30 && spd > 1.0E-4) {
            newVel = newVel.scale(0.30 / spd);
        }

        this.setDeltaMovement(newVel);
        this.move(MoverType.SELF, newVel);

        if (this.horizontalCollision || this.verticalCollision) {
            this.waypointTimer = 0;
        }

        double hLen = Math.hypot(newVel.x, newVel.z);
        if (hLen > 0.005) {
            float targetYaw = (float) (Mth.atan2(newVel.z, newVel.x) * (180.0 / Math.PI)) - 90.0F;
            float targetPitch = (float) (-Mth.atan2(newVel.y, hLen) * (180.0 / Math.PI));
            float newYaw = Mth.rotLerp(0.11F, this.getYRot(), targetYaw);
            float yawDelta = Mth.wrapDegrees(newYaw - this.getYRot());
            this.setYRot(newYaw);
            this.yBodyRot = newYaw;
            this.yHeadRot = newYaw;
            this.setXRot(Mth.lerp(0.14F, this.getXRot(), Mth.clamp(targetPitch, -42.0F, 42.0F)));
            this.entityData.set(DATA_BANK_ROLL, Mth.clamp(-yawDelta * 5.0F, -36.0F, 36.0F));
        }
    }

    private Vec3 smoothAvoidObstacles(Vec3 pos, Vec3 dir) {
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        int oceanY = this.level().dimension() == RiftWorld.RIFT_DIM ? CityPlan.get().ocean : 62;
        Vec3 steer = Vec3.ZERO;

        double[] probes = {4.5, 9.5, 15.0};
        for (double d : probes) {
            double px = pos.x + dir.x * d;
            double py = pos.y + dir.y * d;
            double pz = pos.z + dir.z * d;
            mp.set(Mth.floor(px), Mth.floor(py), Mth.floor(pz));
            if (!this.level().isLoaded(mp)) continue;
            if (!this.level().getBlockState(mp).isAir()) {
                // Ищем, куда свободнее уйти — вверх (над островом) или вниз (под островом) или вбок
                boolean clearAbove = this.level().getBlockState(mp.set(Mth.floor(px), Mth.floor(py + 6.0), Mth.floor(pz))).isAir();
                boolean clearBelow = py - 6.0 > oceanY + 7
                        && this.level().getBlockState(mp.set(Mth.floor(px), Mth.floor(py - 6.0), Mth.floor(pz))).isAir();
                if (flightMode() == FLIGHT_OVER_ISLAND || flightMode() == FLIGHT_ASCEND_RIM || clearAbove && !clearBelow) {
                    steer = steer.add(0.0, 0.45, 0.0);
                } else if (clearBelow) {
                    steer = steer.add(0.0, -0.42, 0.0);
                }
                steer = steer.add(-dir.z * this.orbitDirection * 0.48, 0.0, dir.x * this.orbitDirection * 0.48);
            }
        }

        if (pos.y < oceanY + 8.5) {
            steer = steer.add(0.0, (oceanY + 8.5 - pos.y) * 0.16, 0.0);
        }
        return steer.lengthSqr() > 1.0E-4 ? dir.add(steer).normalize() : dir;
    }

    /**
     * Выбирает следующую путевую точку, чередуя полёт ПОД парящими островами и полёт НАД парящими островами!
     */
    private void pickNextWaypoint() {
        this.waypointTimer = 95 + this.random.nextInt(55);
        this.orbitAngle += this.orbitDirection * (0.58 + this.random.nextDouble() * 0.34);
        ++this.modePhaseSteps;

        int mode = flightMode();
        // Каждые 3..4 путевых сегмента плавно переводим дракона в следующую фазу 4-тактового цикла (ПОД -> ПОДЪЁМ -> НАД -> СПУСК -> ПОД)
        if (this.modePhaseSteps >= 3) {
            mode = (mode + 1) & 3;
            this.entityData.set(DATA_FLIGHT_MODE, mode);
            this.modePhaseSteps = 0;
        }

        if (this.level().dimension() != RiftWorld.RIFT_DIM) {
            double r = 34.0 + this.random.nextDouble() * 18.0;
            double wy = this.homeAnchor.y + (mode >= 2 ? 18.0 : 4.0) + this.random.nextDouble() * 8.0;
            this.waypoint = new Vec3(
                    this.homeAnchor.x + Math.cos(this.orbitAngle) * r,
                    wy,
                    this.homeAnchor.z + Math.sin(this.orbitAngle) * r);
            return;
        }

        CityPlan plan = CityPlan.get();
        int ocean = plan.ocean;

        // Ищем ближайший парящий остров рядом с якорем для красивого облёта И ПОД НИМ, И НАД НИМ!
        CityPlan.Island targetIsl = findScenicFloatingIsland(plan, this.homeAnchor.x, this.homeAnchor.z);
        if (targetIsl != null) {
            double icx = targetIsl.cx;
            double icz = targetIsl.cz;
            double islR = Math.max(24.0, targetIsl.R);

            if (mode == FLIGHT_UNDER_ISLAND) {
                // ФАЗА 0: Плавный полёт прямо ПОД сводом парящего острова (между сталактитами и морем)!
                double rad = islR * (0.28 + this.random.nextDouble() * 0.48);
                double wx = icx + Math.cos(this.orbitAngle) * rad;
                double wz = icz + Math.sin(this.orbitAngle) * rad;
                ElisarChunkGenerator.Col c = ElisarChunkGenerator.column(plan, (int) Math.round(wx), (int) Math.round(wz));
                double underCeiling = (c.isl != null && c.bottom > ocean + 16)
                        ? c.bottom - (7.0 + this.random.nextDouble() * 5.0)
                        : Math.max(ocean + 12.0, targetIsl.base - targetIsl.depth * 0.55 - 8.0);
                double wy = Mth.clamp(underCeiling, ocean + 9.5, targetIsl.base - 6.0);
                this.waypoint = new Vec3(wx, wy, wz);
                return;

            } else if (mode == FLIGHT_ASCEND_RIM) {
                // ФАЗА 1: Спиральный подъём снаружи кромки скал от подножия острова к небу над ним!
                double rad = islR + 14.0 + this.random.nextDouble() * 10.0;
                double wx = icx + Math.cos(this.orbitAngle) * rad;
                double wz = icz + Math.sin(this.orbitAngle) * rad;
                double uClimb = (this.modePhaseSteps + 1) / 3.0;
                double wy = Mth.lerp(uClimb, Math.max(ocean + 16.0, targetIsl.base - 8.0), targetIsl.base + 20.0);
                this.waypoint = new Vec3(wx, wy, wz);
                return;

            } else if (mode == FLIGHT_OVER_ISLAND) {
                // ФАЗА 2: Величественное парение НАД островом (над кронами деревьев, мостами и башнями)!
                double rad = islR * (0.22 + this.random.nextDouble() * 0.68);
                double wx = icx + Math.cos(this.orbitAngle) * rad;
                double wz = icz + Math.sin(this.orbitAngle) * rad;
                ElisarChunkGenerator.Col c = ElisarChunkGenerator.column(plan, (int) Math.round(wx), (int) Math.round(wz));
                double surfTop = (c.isl != null && c.top > ocean) ? c.top : (targetIsl.base + 8.0);
                double wy = surfTop + 16.0 + this.random.nextDouble() * 12.0;
                this.waypoint = new Vec3(wx, wy, wz);
                return;

            } else {
                // ФАЗА 3: Плавное пикирование вдоль внешней кромки скал обратно под свод острова!
                double rad = islR + 15.0 + this.random.nextDouble() * 10.0;
                double wx = icx + Math.cos(this.orbitAngle) * rad;
                double wz = icz + Math.sin(this.orbitAngle) * rad;
                double uDive = (this.modePhaseSteps + 1) / 3.0;
                double wy = Mth.lerp(uDive, targetIsl.base + 14.0, Math.max(ocean + 13.0, targetIsl.base - 14.0));
                this.waypoint = new Vec3(wx, wy, wz);
                return;
            }
        }

        // Если поблизости открытое море — чередуем низкий бреющий полёт у волн и высокие небесные дуги
        double r = 42.0 + this.random.nextDouble() * 24.0;
        double wx = this.homeAnchor.x + Math.cos(this.orbitAngle) * r;
        double wz = this.homeAnchor.z + Math.sin(this.orbitAngle) * r;
        double wy = (mode == FLIGHT_OVER_ISLAND || mode == FLIGHT_ASCEND_RIM)
                ? ocean + 42.0 + this.random.nextDouble() * 24.0
                : ocean + 12.0 + this.random.nextDouble() * 10.0;
        this.waypoint = new Vec3(wx, wy, wz);
    }

    public void setAnchorIsland(CityPlan.Island isl) {
        if (isl != null) {
            setHomeAnchor(new Vec3(isl.cx, isl.base, isl.cz), FLIGHT_UNDER_ISLAND);
        }
    }

    public static PaperDragonEntity spawnForIsland(net.minecraft.server.level.ServerLevel level, CityPlan.Island isl, double ang) {
        PaperDragonEntity d = ru.aiforge.riftworld.registry.ModEntities.PAPER_DRAGON.get().create(level);
        if (d == null) return null;
        double ca = Math.cos(ang), sa = Math.sin(ang);
        double rim = isl.rimDist(ca, sa);
        double dx = isl.cx + ca * (rim * 0.88);
        double dz = isl.cz + sa * (rim * 0.88);
        double dy = Math.max(CityPlan.get().ocean + 10.0, isl.base - 9.0);
        float yaw = (float) (Math.atan2(ca, sa) * (180.0 / Math.PI));
        d.moveTo(dx, dy, dz, yaw, 0.0F);
        d.setHomeAnchor(new Vec3(isl.cx, isl.base, isl.cz), FLIGHT_UNDER_ISLAND);
        level.addFreshEntity(d);
        return d;
    }

    public static double lowestSolidInColumn(Level level, double x, double z, int seaY, int topScanY) {
        int ix = Mth.floor(x), iz = Mth.floor(z);
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int y = seaY + 4; y <= topScanY; ++y) {
            mp.set(ix, y, iz);
            if (!level.getBlockState(mp).isAir() && level.getFluidState(mp).isEmpty()) {
                return y;
            }
        }
        return topScanY + 16;
    }

    @Nullable
    public static CityPlan.Island findScenicFloatingIsland(CityPlan plan, double x, double z) {
        CityPlan.Island best = null;
        double bestScore = Double.MAX_VALUE;
        for (int rStep = 0; rStep <= 220; rStep += 28) {
            int samples = rStep == 0 ? 1 : 12;
            for (int i = 0; i < samples; ++i) {
                double a = (Math.PI * 2.0 * i) / samples;
                double sx = x + Math.cos(a) * rStep;
                double sz = z + Math.sin(a) * rStep;
                ElisarChunkGenerator.Col c = ElisarChunkGenerator.column(plan, (int) Math.round(sx), (int) Math.round(sz));
                if (c.isl != null && c.isl.base >= plan.ocean + 20 && c.bottom >= plan.ocean + 12) {
                    double d = Math.hypot(c.isl.cx - x, c.isl.cz - z);
                    if (d < bestScore) {
                        bestScore = d;
                        best = c.isl;
                    }
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(32.0, 22.0, 32.0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        return distanceSqr < 420.0 * 420.0;
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return dist > 380.0 * 380.0;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        return src.isCreativePlayer() && super.hurt(src, amount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FlightMode", flightMode());
        if (this.homeAnchor != null) {
            tag.putDouble("HomeX", this.homeAnchor.x);
            tag.putDouble("HomeY", this.homeAnchor.y);
            tag.putDouble("HomeZ", this.homeAnchor.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("FlightMode")) {
            this.entityData.set(DATA_FLIGHT_MODE, tag.getInt("FlightMode"));
        }
        if (tag.contains("HomeX")) {
            this.homeAnchor = new Vec3(tag.getDouble("HomeX"), tag.getDouble("HomeY"), tag.getDouble("HomeZ"));
        }
    }
}
