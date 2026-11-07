package ru.aiforge.riftworld.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.world.CityPlan;
import ru.aiforge.riftworld.world.MajaIslandDevourer;

/** A rare ocean titan. It ignores players; no chat or HUD messages are emitted. */
public final class ElGranMajaEntity extends Mob {
    private static final EntityDataAccessor<Boolean> READY = SynchedEntityData.defineId(ElGranMajaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> JAW = SynchedEntityData.defineId(ElGranMajaEntity.class, EntityDataSerializers.FLOAT);

    /** Крейсерская скорость, блоков за тик. */
    private static final float CRUISE = 0.16F;
    /** Предел угловой скорости поворота, градусов за тик. */
    private static final float MAX_TURN_RATE = 0.30F;
    /** Набор угловой скорости, градусов за тик за тик. */
    private static final float TURN_ACCEL = 0.011F;

    private float desiredYaw;
    private int directionTicks;
    private float jawPrevious;
    private float jawCurrent;
    private final MajaIslandDevourer devourer = new MajaIslandDevourer();

    // ---- состояние анимации. Считается одинаково на сервере и на клиенте: ----
    // клиент видит тот же yaw и ту же позицию, поэтому сглаженные значения совпадают
    // и рендереру не нужны лишние пакеты.
    private float turnVelocity;        // текущая угловая скорость руля, градусов за тик
    private float currentSpeed = CRUISE;
    private float verticalVelocity;
    private float turnRate;            // сглаженная угловая скорость для рендера
    private float turnRatePrevious;
    private float swimSpeed;           // сглаженная поступательная скорость
    private float swimSpeedPrevious;
    private float breachAmount;        // 0..1, насколько зверь вышел из воды
    private float breachPrevious;
    private double lastX;
    private double lastZ;
    private int avoidSign = 1;

    public ElGranMajaEntity(EntityType<? extends ElGranMajaEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1200.0)
                .add(Attributes.MOVEMENT_SPEED, 0.18).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(READY, false);
        entityData.define(JAW, 0.15F);
    }

    public boolean isReadyToRender() { return entityData.get(READY); }
    /** Сглаженная угловая скорость поворота, градусов за тик (плюс — вправо). */
    public float turnRate(float partial) { return Mth.lerp(partial, turnRatePrevious, turnRate); }
    /** Сглаженная поступательная скорость, блоков за тик. */
    public float swimSpeed(float partial) { return Mth.lerp(partial, swimSpeedPrevious, swimSpeed); }
    /** Насколько зверь поднялся из воды: 0 — в глубине, 1 — верхняя точка выпрыгивания. */
    public float breachAmount(float partial) { return Mth.lerp(partial, breachPrevious, breachAmount); }
    public float jaw(float partial) { return Mth.lerp(partial, jawPrevious, jawCurrent); }

    /** Do not let normal Mob navigation move the small vanilla anchor hitbox. */
    @Override
    public void travel(Vec3 input) { }

    @Override
    public void tick() {
        // yRotO перезаписывается внутри super.tick(), поэтому угловую скорость снимаем до него.
        float yawDelta = Mth.wrapDegrees(getYRot() - yRotO);
        super.tick();
        jawPrevious = jawCurrent;
        jawCurrent += (entityData.get(JAW) - jawCurrent) * 0.12F;

        // ---------- состояние анимации: одинаково на сервере и на клиенте ----------
        turnRatePrevious = turnRate;
        swimSpeedPrevious = swimSpeed;
        breachPrevious = breachAmount;
        turnRate += (yawDelta - turnRate) * 0.10F;
        // Страховка первого тика: lastX/lastZ ещё нулевые, «перемещение» было бы огромным.
        double moved = Math.min(1.0, Math.hypot(getX() - lastX, getZ() - lastZ));
        lastX = getX();
        lastZ = getZ();
        swimSpeed += ((float) moved - swimSpeed) * 0.10F;
        double cycle = (tickCount + (getId() & 255)) % 1600;
        double breach = cycle < 220 ? Math.sin(Math.PI * cycle / 220.0) : 0.0;
        breachAmount += ((float) breach - breachAmount) * 0.18F;

        if (level().isClientSide || !(level() instanceof ServerLevel server)) return;
        if (!isReadyToRender()) {
            // /summon inside rock or an unloaded area never renders half a creature in blocks.
            if (MajaCollision.clear(server, position(), getYRot())) {
                entityData.set(READY, true);
                desiredYaw = getYRot();
            } else {
                setDeltaMovement(Vec3.ZERO);
                // A command-summon inside rock or an entity waiting for chunks stays inert and hidden.
                return;
            }
        }
        if (isNoAi()) {
            setDeltaMovement(Vec3.ZERO);
            if (!MajaCollision.clear(server, position(), getYRot())) entityData.set(READY, false);
            return;
        }

        if (devourer.tick(server, this)) {
            setDeltaMovement(Vec3.ZERO);
            entityData.set(JAW, 1.0F);
            if (!MajaCollision.clear(server, position(), getYRot())) entityData.set(READY, false);
            return;
        }

        if (--directionTicks <= 0) {
            desiredYaw = getYRot() + (random.nextFloat() - 0.5F) * 70.0F;
            directionTicks = 240 + random.nextInt(240);
        }
        // Плавное руление: угловая скорость набирается и гасится, а не прыгает рывком.
        float toTarget = Mth.wrapDegrees(desiredYaw - getYRot());
        float wanted = Mth.clamp(toTarget * 0.022F, -MAX_TURN_RATE, MAX_TURN_RATE);
        turnVelocity += Mth.clamp(wanted - turnVelocity, -TURN_ACCEL, TURN_ACCEL);
        turnVelocity *= 0.94F; // трение воды
        float nextYaw = getYRot() + turnVelocity;

        // В повороте зверь притормаживает: так разворот читается как разворот, а не как телепорт.
        float turning = Math.min(1.0F, Math.abs(turnVelocity) / MAX_TURN_RATE);
        float targetSpeed = CRUISE * (1.0F - 0.45F * turning);
        currentSpeed += Mth.clamp(targetSpeed - currentSpeed, -0.0025F, 0.0025F);

        double desiredY = CityPlan.get().ocean - 13.0 + 18.0 * breach;
        verticalVelocity += Mth.clamp((desiredY - getY()) * 0.045 - verticalVelocity, -0.010, 0.010);
        verticalVelocity *= 0.94F;
        double vertical = Mth.clamp(verticalVelocity, -0.22, 0.22);
        double rad = Math.toRadians(nextYaw);
        Vec3 motion = new Vec3(-Math.sin(rad) * currentSpeed, vertical, Math.cos(rad) * currentSpeed);
        Vec3 next = position().add(motion);
        if (MajaCollision.clear(server, next, nextYaw)) {
            setYRot(nextYaw);
            yBodyRot = nextYaw;
            yHeadRot = nextYaw;
            setDeltaMovement(motion);
            move(MoverType.SELF, motion);
        } else {
            setDeltaMovement(Vec3.ZERO);
            // Упираемся в берег: уводим руль в одну сторону, пока не найдём выход.
            turnVelocity = Mth.clamp(turnVelocity + avoidSign * TURN_ACCEL * 3.0F, -MAX_TURN_RATE, MAX_TURN_RATE);
            currentSpeed *= 0.92F;
            // If terrain changed underneath the existing envelope, hide rather than render through it.
            if (!MajaCollision.clear(server, position(), getYRot())) entityData.set(READY, false);
            if (tickCount % 40 == 0) {
                desiredYaw = getYRot() + avoidSign * 55.0F;
                directionTicks = 120;
                // Если и в эту сторону не пускает — следующий заход попробуем зеркально.
                avoidSign = -avoidSign;
            }
        }
        if (tickCount % 5 == 0) {
            float breathing = 0.12F + 0.12F * (1.0F + Mth.sin(tickCount * 0.026F));
            entityData.set(JAW, Mth.clamp(breach > 0.12 ? (float) (0.35 + breach * 0.55) : breathing, 0.0F, 1.0F));
        }
        if (tickCount % 700 == 0) {
            server.playSound(null, blockPosition(), SoundEvents.ELDER_GUARDIAN_AMBIENT, SoundSource.AMBIENT, 2.8F, 0.42F);
        }
    }

    @Override
    public boolean canBreatheUnderwater() { return true; }
    @Override
    public boolean removeWhenFarAway(double distance) { return false; }
    @Override
    public boolean isPushable() { return false; }
    @Override
    protected void doPush(Entity other) { }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("majaDesiredYaw", desiredYaw);
        tag.putFloat("majaTurnVelocity", turnVelocity);
        tag.putFloat("majaSpeed", currentSpeed);
        // Read-only diagnostics for server operators and the automated collision probe.
        tag.putBoolean("majaReady", isReadyToRender());
        tag.putFloat("majaJaw", entityData.get(JAW));
    }
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        desiredYaw = tag.getFloat("majaDesiredYaw");
        turnVelocity = tag.getFloat("majaTurnVelocity");
        if (tag.contains("majaSpeed")) currentSpeed = tag.getFloat("majaSpeed");
        // Re-check the loaded world before displaying any part of the model.
        entityData.set(READY, false);
    }
}
