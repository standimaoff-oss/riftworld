package ru.aiforge.riftworld.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.flight.Flight;
import ru.aiforge.riftworld.registry.ModEntities;
import ru.aiforge.riftworld.world.CityPlan;

/**
 * Рев 5: ЕДИНСТВЕННЫЙ бумажный дракон-оригами летает в тумане вокруг островов Элизара - кружит над ними,
 * и переходит между ними низко над водой. Спавнит только PaperDragonKeeper (один на измерение).
 * Полёт - мягкий орбитальный глайдер (без AI-целей): островная орбита -> транзит над волной -> следующая орбита.
 * Бумажный: от удара не погибает, а резко срывается прочь по ветру и уходит на другую орбиту.
 */
public class PaperDragonEntity extends Monster {
    private static final int MODE_ORBIT = 0, MODE_TRANSIT = 1, MODE_FLEE = 2;
    private int mode = MODE_ORBIT;
    private int islIdx = -1;
    private double orbitA, orbitDir = 1;
    private Vec3 way;
    private int wayAge;
    private int fleeUntil;
    public float bank, bank0;
    public float flap, flap0;

    public PaperDragonEntity(EntityType<? extends PaperDragonEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noCulling = true;
    }

    @Override
    public void tick() {
        super.tick();
        bank0 = bank;
        flap0 = flap;
        if (level().dimension() != Flight.ELISAR_DIM && tickCount % 40 == 0) return;      // keeper owns lifetime
        if (level().isClientSide) {
            if (tickCount % (110 + (Math.floorMod(hashCode(), 64))) == 0) {
                level().playLocalSound(getX(), getY(), getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.AMBIENT,
                        0.55F, 1.3F + random.nextFloat() * 0.25F, false);                 // сухой бумажный шелест
            }
            roll();
            return;
        }
        CityPlan.Island isl = pickIsland();
        if (mode == MODE_FLEE && tickCount >= fleeUntil) mode = MODE_ORBIT;
        if (way == null || --wayAge <= 0) {
            if (isl != null && mode == MODE_ORBIT) {
                // круги вокруг острова: радиус немного дышит, высота - над крышами
                orbitA += 0.010 * orbitDir + random.nextDouble() * 0.004;
                if (random.nextInt(8) == 0) orbitDir = -orbitDir;
                double R = isl.R + 16 + Math.sin(orbitA * 0.7) * 7;
                double y = isl.base + 13 + Math.sin(orbitA * 1.6) * 4;
                way = new Vec3(isl.cx + Math.cos(orbitA) * R, y, isl.cz + Math.sin(orbitA) * R);
                wayAge = 100 + random.nextInt(120);
                if (random.nextInt(6) == 0) { mode = MODE_TRANSIT; islIdx = -1; way = null; }
            } else if (isl != null) {
                // транзит: глисса низко над волной к следующему острову
                way = new Vec3(isl.cx, isl.base + 12, isl.cz);
                wayAge = 200 + random.nextInt(260);
                mode = MODE_ORBIT;
                orbitA = Math.atan2(getZ() - isl.cz, getX() - isl.cx);
            } else {
                wayAge = 120 + random.nextInt(200);
                double a = random.nextDouble() * Math.PI * 2, d = 20 + random.nextDouble() * 32;
                double x = getX() + Math.cos(a) * d, z = getZ() + Math.sin(a) * d;
                way = new Vec3(x, surfaceY(x, z) + 7 + random.nextDouble() * 7, z);
            }
        }
        // штурманство к вейпоинту: мягкое поворачивание, без рывков
        if (way == null) { roll(); return; }
        Vec3 dv = way.subtract(position());
        double dist = dv.length();
        if (dist < 2.0) { way = null; wayAge = 6; roll(); return; }
        double speedCap = mode == MODE_FLEE ? 0.55 : 0.34;
        Vec3 want = dv.normalize().scale(0.30);
        Vec3 vel = getDeltaMovement().add(want.subtract(getDeltaMovement()).scale(0.04));
        if (vel.length() > speedCap) vel = vel.normalize().scale(speedCap);
        double surf = surfaceY(getX(), getZ());
        double bot = mode == MODE_TRANSIT ? surf + 4.5 : surf + 6.5;                     // между островами - почти у воды
        if (getY() < bot) vel = vel.add(0, 0.045, 0);
        if (getY() > surf + 30.0) vel = vel.add(0, -0.04, 0);
        setDeltaMovement(vel);
        move(MoverType.SELF, vel);
        if (vel.lengthSqr() > 0.0004) {
            float yaw = (float) (Math.atan2(-vel.x, vel.z) * Mth.RAD_TO_DEG);
            setYRot(Mth.rotLerp(0.10F, getYRot(), yaw));
            yBodyRot = getYRot();
            setXRot(Mth.lerp(0.06F, getXRot(), (float) -(Math.atan2(vel.y, Math.hypot(vel.x, vel.z)) * Mth.RAD_TO_DEG)));
        }
        roll();
    }

    /** Крен и кивание - считается на обеих сторонах из скорости (без синка). */
    private void roll() {
        Vec3 v = getDeltaMovement();
        double sp = v.length();
        double wantYaw = sp > 0.01 ? Math.atan2(-v.x, v.z) * Mth.RAD_TO_DEG : getYRot();
        float turn = (float) Mth.wrapDegrees(wantYaw - getYRot());
        float wantBank = Mth.clamp(-turn * 2.2F, -38F, 38F);
        bank += (wantBank - bank) * 0.08F;
        flap += (0.25F + (float) Math.min(1.0, sp * 1.6)) * 0.06F;                      // медленные взмахи плоскостей
    }

    /** Ближний/средний остров для кружения (предпочтительно в разумной досягаемости). */
    private CityPlan.Island pickIsland() {
        CityPlan plan = CityPlan.get();
        if (plan.islands.isEmpty()) return null;
        CityPlan.Island best = null;
        double bd = 1e9;
        int idx = 0, bi = -1;
        for (CityPlan.Island i : plan.islands) {
            if (i.kind == 4) { ++idx; continue; }                                       // мелкие дикие чаши - не ориентиры
            double d = Math.hypot(getX() - i.cx, getZ() - i.cz) - i.R;
            if (d < bd) { bd = d; bi = idx; best = i; }
            ++idx;
        }
        if (best == null || bd > 380) return null;
        if (mode == MODE_ORBIT && bi != islIdx && bd < 190) islIdx = bi;
        return best;
    }

    public CityPlan.Island orbitIsland() {
        CityPlan plan = CityPlan.get();
        return plan.islands.isEmpty() ? null : (islIdx >= 0 && islIdx < plan.islands.size() ? plan.islands.get(islIdx) : pickIsland());
    }

    private int surfaceY(double x, double z) {
        return level().getHeight(Heightmap.Types.WORLD_SURFACE, Mth.floor(x), Mth.floor(z));
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        // бумага: не гибнет - срывается прочь по ветру на новую орбиту
        if (!level().isClientSide) {
            if (src.getEntity() != null) {
                Vec3 away = position().subtract(src.getEntity().position()).normalize().scale(2.1);
                setDeltaMovement(getDeltaMovement().add(away.add(0, 1.0, 0)));
            }
            mode = MODE_FLEE;
            fleeUntil = tickCount + 240;
            way = null;
            wayAge = 8;
            level().playSound(null, blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.AMBIENT, 1.2F, 0.9F);
        }
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity e) {
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;                                                                     // единственный в мире - не исчезает
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("pd_mode", mode);
        tag.putInt("pd_isl", islIdx);
        tag.putDouble("pd_oa", orbitA);
        tag.putDouble("pd_od", orbitDir);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        mode = tag.getInt("pd_mode");
        islIdx = tag.getInt("pd_isl");
        orbitA = tag.getDouble("pd_oa");
        orbitDir = tag.getDouble("pd_od");
    }

    /** Спавн (только PaperDragonKeeper): над водой у ближайшего к игроку острова. */
    public static PaperDragonEntity spawn(net.minecraft.server.level.ServerLevel sl, BlockPos at) {
        PaperDragonEntity d = new PaperDragonEntity(ModEntities.PAPER_DRAGON.get(), sl);
        d.setPos(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
        d.setYRot(sl.random.nextFloat() * 360);
        d.yBodyRot = d.getYRot();
        sl.addFreshEntity(d);
        return d;
    }
}
