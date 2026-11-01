package ru.aiforge.riftworld.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.flight.Flight;
import ru.aiforge.riftworld.registry.ModEntities;
import ru.aiforge.riftworld.world.CityPlan;
import ru.aiforge.riftworld.world.FloatingIslands;

/**
 * v0.20.0: Модульный белый оригами-дракон в китайском стиле ({@code 01_origami_dragon_ref.png}).
 *
 * <ul>
 *   <li><b>Средних размеров</b> (~8 блоков в длину с изгибами), полностью <b>безвредный</b> и мирный;</li>
 *   <li><b>Редкий</b> (2–3 особи в окрестности игрока, поддерживаются {@link ru.aiforge.riftworld.world.PaperDragonKeeper});</li>
 *   <li><b>Летает вдоль кромок и ПОД парящими островами</b>, плавно ныряя под каменные конусы островов
 *       над светящимися белыми волнами океана и грациозно изгибаясь всем сегментированным бумажным телом.</li>
 * </ul>
 */
public class PaperDragonEntity extends PathfinderMob {
    public static final int TRAIL_CAPACITY = 128;

    private double anchorCx = Double.NaN;
    private double anchorCz = Double.NaN;
    private double orbitA;
    private double orbitDir = 1.0;
    private double phaseSeed;
    private int roamSwitchTimer = 900;
    private int fleeTicks = 0;

    // Клиентский кольцевой буфер реальной 3D-траектории полёта для настоящего изгиба тела по дуге
    public final double[] trailX = new double[TRAIL_CAPACITY];
    public final double[] trailY = new double[TRAIL_CAPACITY];
    public final double[] trailZ = new double[TRAIL_CAPACITY];
    public int trailHead = 0;
    public int trailCount = 0;

    public PaperDragonEntity(EntityType<? extends PaperDragonEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noCulling = true;
        this.xpReward = 0;
        this.orbitA = this.random.nextDouble() * Math.PI * 2.0;
        this.orbitDir = this.random.nextBoolean() ? 1.0 : -1.0;
        this.phaseSeed = this.random.nextDouble() * Math.PI * 2.0;
        this.roamSwitchTimer = 800 + this.random.nextInt(600);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.36)
                .add(Attributes.FLYING_SPEED, 0.55)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        // Полностью мирное воздушное существо оригами — никаких боевых или преследующих целей.
    }

    /** Инициализация плавной S-образной кривой хвоста сразу при появлении сущности в кадре. */
    public void ensureTrailInitialized() {
        if (this.trailCount > 0) return;
        double rad = Math.toRadians(this.getYRot());
        double fx = -Math.sin(rad), fz = Math.cos(rad);
        double rx = fz, rz = -fx;
        int initSamples = 64;
        for (int k = initSamples - 1; k >= 0; --k) {
            double back = k * 0.18;
            double sw = Math.sin(k * 0.24 + this.phaseSeed) * 0.55;
            double vw = Math.cos(k * 0.20 + this.phaseSeed * 0.7) * 0.38;
            pushTrailSample(
                    this.getX() - fx * back + rx * sw,
                    this.getY() + vw,
                    this.getZ() - fz * back + rz * sw);
        }
    }

    private void pushTrailSample(double x, double y, double z) {
        if (this.trailCount > 0) {
            int last = (this.trailHead - 1 + TRAIL_CAPACITY) % TRAIL_CAPACITY;
            double dx = x - this.trailX[last], dy = y - this.trailY[last], dz = z - this.trailZ[last];
            if (dx * dx + dy * dy + dz * dz < 1.0e-4) return;
        }
        this.trailX[this.trailHead] = x;
        this.trailY[this.trailHead] = y;
        this.trailZ[this.trailHead] = z;
        this.trailHead = (this.trailHead + 1) % TRAIL_CAPACITY;
        if (this.trailCount < TRAIL_CAPACITY) {
            ++this.trailCount;
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.fallDistance = 0.0F;

        if (this.level().isClientSide) {
            ensureTrailInitialized();
            pushTrailSample(this.getX(), this.getY(), this.getZ());
            return;
        }

        if (this.isNoAi()) {
            return;
        }

        if (this.level().dimension() != Flight.ELISAR_DIM) {
            return;
        }

        CityPlan plan = CityPlan.get();
        int ocean = plan.ocean;

        CityPlan.Island isl = resolveTargetIsland();
        if (isl == null) {
            // Плавное кружение над морем, если рядом ещё не выбран парящий остров
            this.orbitA += 0.016 * this.orbitDir;
            double ty = ocean + 11.0 + Math.sin(this.tickCount * 0.05 + this.phaseSeed) * 2.8;
            Vec3 fallback = new Vec3(
                    this.getX() + Math.cos(this.orbitA) * 6.0,
                    ty,
                    this.getZ() + Math.sin(this.orbitA) * 6.0);
            steerToward(fallback, ocean, 0.34);
            return;
        }

        if (--this.roamSwitchTimer <= 0) {
            this.roamSwitchTimer = 850 + this.random.nextInt(650);
            pickNeighborIsland(isl);
            isl = resolveTargetIsland();
            if (isl == null) return;
        }

        // Угловая скорость вдоль острова: плавный обход кромки + заныривание под свод острова!
        double effR = Math.max(22.0, isl.R);
        double angStep = (this.fleeTicks > 0 ? 0.62 : 0.42) / effR;
        if (this.fleeTicks > 0) --this.fleeTicks;
        this.orbitA += this.orbitDir * angStep;

        double cosA = Math.cos(this.orbitA);
        double sinA = Math.sin(this.orbitA);
        double rim = isl.rimDist(cosA, sinA);

        // Чередуем проход ПОД островом (rFactor ~ 0.62..0.86) и проход ВДОЛЬ скалистой кромки (rFactor ~ 0.96..1.14),
        // плюс изящная синусоидальная змейка второго порядка!
        double underWave = Math.sin(this.orbitA * 2.0 + this.phaseSeed);
        double snakeWave = Math.sin(this.tickCount * 0.075 + this.phaseSeed * 1.7) * 0.08;
        double rFactor = 0.87 + 0.23 * underWave + snakeWave;
        double dist = rim * rFactor;

        double wx = isl.cx + cosA * dist;
        double wz = isl.cz + sinA * dist;

        // Вычисляем безопасную высоту ПОД сводом острова или вдоль его края
        double approxCeil = FloatingIslands.approxUndersideY(isl, wx, wz, ocean);
        double realCeil = scanRealCeilingY(wx, wz, ocean, (int) Math.ceil(isl.base + 8.0));
        double ceilY = Math.min(approxCeil, realCeil);

        double vertArch = Math.sin(this.tickCount * 0.065 + this.phaseSeed) * 1.85;
        double targetY;
        if (rFactor < 0.96) {
            // Под парящим островом: скользим в воздушном гроте между водой (ocean + 5) и каменным конусом (ceilY - 4.8)
            double idealUnder = Math.min(ceilY - 4.8, isl.base - 6.5) + vertArch * 0.75;
            targetY = Mth.clamp(idealUnder, ocean + 5.0, Math.max(ocean + 5.5, ceilY - 3.6));
        } else {
            // Вдоль внешней скалистой стены острова: поднимаемся ближе к кромке террас и снова ныряем вниз
            double rimBlend = Mth.clamp((rFactor - 0.96) / 0.20, 0.0, 1.0);
            double underH = Math.min(ceilY - 4.5, isl.base - 6.0);
            double cliffH = isl.base - 2.5 + vertArch;
            targetY = Mth.lerp(rimBlend, underH, cliffH);
            targetY = Math.max(ocean + 5.2, targetY);
        }

        steerToward(new Vec3(wx, targetY, wz), ocean, this.fleeTicks > 0 ? 0.56 : 0.39);

        // Тихий шелест складывающейся бумаги на виражах
        if (this.tickCount % 110 == 0 && this.random.nextInt(2) == 0) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.AMBIENT,
                    0.45F, 1.35F + this.random.nextFloat() * 0.25F);
        }
    }

    private void steerToward(Vec3 goal, int ocean, double speed) {
        Vec3 pos = this.position();
        Vec3 toGoal = goal.subtract(pos);
        double len = toGoal.length();
        Vec3 desired = len > 1.0e-3 ? toGoal.scale(speed / len) : new Vec3(0.0, 0.0, speed);

        // Мягкое огибание каменных сталактитов, опор мостов и деревьев впереди по курсу
        BlockPos aheadNear = BlockPos.containing(pos.add(desired.normalize().scale(3.2)));
        BlockPos aheadFar = BlockPos.containing(pos.add(desired.normalize().scale(6.2)));
        if (isSolidOrFluid(aheadNear) || isSolidOrFluid(aheadFar)) {
            // Пробуем уйти вниз к глади моря (под свод острова) или вбок по касательной
            BlockPos belowCheck = BlockPos.containing(pos.x, Math.max(ocean + 4.8, pos.y - 3.2), pos.z);
            if (pos.y > ocean + 7.5 && !isSolidOrFluid(belowCheck)) {
                desired = new Vec3(-desired.z * 0.55, -0.28, desired.x * 0.55).normalize().scale(speed);
            } else {
                desired = new Vec3(-desired.z * this.orbitDir, 0.16, desired.x * this.orbitDir).normalize().scale(speed);
            }
        }

        Vec3 v = this.getDeltaMovement().scale(0.84).add(desired.scale(0.16));
        if (pos.y + v.y < ocean + 4.2) {
            v = new Vec3(v.x, Math.max(0.08, (ocean + 4.6 - pos.y) * 0.25), v.z);
        }
        double hs = Math.hypot(v.x, v.z);
        if (hs < 0.14) {
            double a = Math.toRadians(this.getYRot());
            v = new Vec3(-Math.sin(a) * 0.22, v.y, Math.cos(a) * 0.22);
            hs = Math.hypot(v.x, v.z);
        }

        this.setDeltaMovement(v);
        if (!this.level().noCollision(this.getBoundingBox())) {
            // Защита от зацепления за сталактит под островом: плавный выход наружу и чуть вниз в открытый воздух
            double outX = Double.isNaN(this.anchorCx) ? v.x : (pos.x - this.anchorCx);
            double outZ = Double.isNaN(this.anchorCz) ? v.z : (pos.z - this.anchorCz);
            double ol = Math.max(0.001, Math.hypot(outX, outZ));
            double ny = Math.max(ocean + 5.8, pos.y - 0.35);
            this.setPos(pos.x + (outX / ol) * 0.45 + v.x * 0.5, ny, pos.z + (outZ / ol) * 0.45 + v.z * 0.5);
        } else {
            this.move(net.minecraft.world.entity.MoverType.SELF, v);
        }

        float wantYaw = (float) (Mth.atan2(-v.x, v.z) * (180.0 / Math.PI));
        float newYaw = Mth.rotLerp(0.18F, this.getYRot(), wantYaw);
        this.setYRot(newYaw);
        this.yBodyRot = newYaw;
        this.yHeadRot = newYaw;
        float wantPitch = (float) (-Mth.atan2(v.y, Math.max(0.05, hs)) * (180.0 / Math.PI));
        this.setXRot(Mth.rotLerp(0.18F, this.getXRot(), Mth.clamp(wantPitch, -42.0F, 42.0F)));
    }

    private boolean isSolidOrFluid(BlockPos p) {
        if (!this.level().isLoaded(p)) return false;
        BlockState st = this.level().getBlockState(p);
        return !st.isAir() && (!st.getCollisionShape(this.level(), p).isEmpty() || !st.getFluidState().isEmpty());
    }

    private double scanRealCeilingY(double wx, double wz, int ocean, int maxTopY) {
        return lowestSolidInColumn(this.level(), wx, wz, ocean, maxTopY);
    }

    public static double lowestSolidInColumn(Level level, double wx, double wz, int ocean, int maxTopY) {
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        int bx = Mth.floor(wx), bz = Mth.floor(wz);
        if (!level.isLoaded(mp.set(bx, ocean + 4, bz))) {
            return maxTopY;
        }
        int minSolid = maxTopY;
        for (int ox = -1; ox <= 1; ++ox) {
            for (int oz = -1; oz <= 1; ++oz) {
                if (!level.isLoaded(mp.set(bx + ox, ocean + 4, bz + oz))) continue;
                for (int y = ocean + 4; y <= minSolid; ++y) {
                    mp.set(bx + ox, y, bz + oz);
                    BlockState st = level.getBlockState(mp);
                    if (!st.isAir() && !st.getCollisionShape(level, mp).isEmpty()) {
                        if (y < minSolid) minSolid = y;
                        break;
                    }
                }
            }
        }
        return minSolid;
    }

    private CityPlan.Island resolveTargetIsland() {
        List<CityPlan.Island> nearby = FloatingIslands.gather(this.getX(), this.getZ(), 300.0, true);
        if (nearby.isEmpty()) return null;
        if (!Double.isNaN(this.anchorCx)) {
            for (CityPlan.Island cand : nearby) {
                if (Math.hypot(cand.cx - this.anchorCx, cand.cz - this.anchorCz) < 4.0) {
                    return cand;
                }
            }
        }
        CityPlan.Island best = null;
        double bestDist = Double.MAX_VALUE;
        for (CityPlan.Island cand : nearby) {
            double d = Math.abs(Math.hypot(this.getX() - cand.cx, this.getZ() - cand.cz) - cand.R * 0.85);
            if (d < bestDist) {
                bestDist = d;
                best = cand;
            }
        }
        if (best != null) {
            setAnchorIsland(best);
        }
        return best;
    }

    private void pickNeighborIsland(CityPlan.Island current) {
        List<CityPlan.Island> nearby = FloatingIslands.gather(this.getX(), this.getZ(), 280.0, true);
        CityPlan.Island next = null;
        double bestScore = Double.MAX_VALUE;
        for (CityPlan.Island cand : nearby) {
            if (cand == current) continue;
            double d = Math.hypot(cand.cx - current.cx, cand.cz - current.cz);
            if (d < 35.0 || d > 250.0) continue;
            double score = d + this.random.nextDouble() * 65.0;
            if (score < bestScore) {
                bestScore = score;
                next = cand;
            }
        }
        if (next != null) {
            setAnchorIsland(next);
        }
    }

    public void setAnchorIsland(CityPlan.Island isl) {
        this.anchorCx = isl.cx;
        this.anchorCz = isl.cz;
        this.orbitA = Math.atan2(this.getZ() - isl.cz, this.getX() - isl.cx);
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(src, amount);
        }
        if (!this.level().isClientSide) {
            this.fleeTicks = 140;
            this.orbitDir = -this.orbitDir;
            this.level().playSound(null, this.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.AMBIENT, 0.9F, 1.35F);
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity e) {
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!Double.isNaN(this.anchorCx)) {
            tag.putDouble("pd_acx", this.anchorCx);
            tag.putDouble("pd_acz", this.anchorCz);
        }
        tag.putDouble("pd_oa", this.orbitA);
        tag.putDouble("pd_od", this.orbitDir);
        tag.putDouble("pd_ps", this.phaseSeed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("pd_acx")) {
            this.anchorCx = tag.getDouble("pd_acx");
            this.anchorCz = tag.getDouble("pd_acz");
        }
        this.orbitA = tag.getDouble("pd_oa");
        if (tag.contains("pd_od")) this.orbitDir = tag.getDouble("pd_od");
        if (tag.contains("pd_ps")) this.phaseSeed = tag.getDouble("pd_ps");
    }

    /** Спавн оригами-дракона прямо под сводом/кромкой парящего острова. */
    public static PaperDragonEntity spawnForIsland(ServerLevel sl, CityPlan.Island isl, double angleRad) {
        int ocean = CityPlan.get().ocean;
        double cosA = Math.cos(angleRad), sinA = Math.sin(angleRad);
        double rim = isl.rimDist(cosA, sinA);
        double sx = isl.cx + cosA * (rim * 0.88);
        double sz = isl.cz + sinA * (rim * 0.88);
        double approxCeil = FloatingIslands.approxUndersideY(isl, sx, sz, ocean);
        double realCeil = lowestSolidInColumn(sl, sx, sz, ocean, (int) Math.ceil(isl.base + 8.0));
        double ceilY = Math.min(approxCeil, realCeil);
        double sy = Mth.clamp(Math.min(ceilY - 5.2, isl.base - 7.0), ocean + 6.0, Math.max(ocean + 6.2, ceilY - 3.6));

        PaperDragonEntity d = new PaperDragonEntity(ModEntities.PAPER_DRAGON.get(), sl);
        d.setPos(sx, sy, sz);
        d.setAnchorIsland(isl);
        float tangentYaw = (float) (Mth.atan2(sinA * d.orbitDir, cosA * d.orbitDir) * (180.0 / Math.PI));
        d.setYRot(tangentYaw);
        d.yBodyRot = tangentYaw;
        d.yHeadRot = tangentYaw;
        sl.addFreshEntity(d);
        return d;
    }

    /** Совместимый спавн в произвольной точке. */
    public static PaperDragonEntity spawn(ServerLevel sl, BlockPos at) {
        PaperDragonEntity d = new PaperDragonEntity(ModEntities.PAPER_DRAGON.get(), sl);
        d.setPos(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
        float yaw = sl.random.nextFloat() * 360.0F;
        d.setYRot(yaw);
        d.yBodyRot = yaw;
        d.yHeadRot = yaw;
        sl.addFreshEntity(d);
        return d;
    }
}
