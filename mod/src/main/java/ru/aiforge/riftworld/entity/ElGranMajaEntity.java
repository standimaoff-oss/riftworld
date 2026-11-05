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
import ru.aiforge.riftworld.world.MajaMotion;
import net.minecraft.core.particles.ParticleTypes;

/** A rare ocean titan. It ignores players; no chat or HUD messages are emitted. */
public final class ElGranMajaEntity extends Mob {
    private static final EntityDataAccessor<Boolean> READY = SynchedEntityData.defineId(ElGranMajaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> JAW = SynchedEntityData.defineId(ElGranMajaEntity.class, EntityDataSerializers.FLOAT);
    private float desiredYaw;
    private int directionTicks;
    /** Eased heading rate in degrees per tick: starts and stops smoothly. */
    private float turnRate;
    /** Smoothed cruise speed factor, so it accelerates out of a turn. */
    private float speedFactor = 1.0F;
    private int motionTick = -1;
    private float jawPrevious;
    private float jawCurrent;
    private final MajaIslandDevourer devourer = new MajaIslandDevourer();

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
    public float jaw(float partial) { return Mth.lerp(partial, jawPrevious, jawCurrent); }

    /** Do not let normal Mob navigation move the small vanilla anchor hitbox. */
    @Override
    public void travel(Vec3 input) { }

    @Override
    public void tick() {
        super.tick();
        jawPrevious = jawCurrent;
        jawCurrent += (entityData.get(JAW) - jawCurrent) * 0.12F;
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
            // Chewing stays inside the validated jaw envelope and is interpolated on clients.
            entityData.set(JAW, 0.70F + 0.28F * Mth.sin(tickCount * 0.16F));
            if (!MajaCollision.clear(server, position(), getYRot())) entityData.set(READY, false);
            return;
        }

        if (--directionTicks <= 0) {
            // Wide, lazy course changes instead of constant small zig-zags:
            // the old ±22° every 12 s read as an almost stationary titan.
            desiredYaw = getYRot() + (random.nextFloat() - 0.5F) * 96.0F;
            directionTicks = 300 + random.nextInt(300);
        }
        // Eased steering: the commanded rate is limited by the heading error and
        // by an angular acceleration, so a turn eases in and out instead of
        // snapping to a new direction.
        float error = Mth.wrapDegrees(desiredYaw - getYRot());
        float wantedRate = Mth.clamp(error * 0.055F, -0.85F, 0.85F);
        float maxStep = 0.028F;
        turnRate += Mth.clamp(wantedRate - turnRate, -maxStep, maxStep);
        turnRate = Mth.clamp(turnRate, -0.85F, 0.85F);
        // slow down slightly while carving a turn, speed up on the straight
        float wantedSpeed = 1.0F - 0.28F * Math.min(1.0F, Math.abs(turnRate) / 0.85F);
        speedFactor += Mth.clamp(wantedSpeed - speedFactor, -0.02F, 0.02F);
        float nextYaw = getYRot() + turnRate;
        if (motionTick < 0) motionTick = Math.floorMod(getUUID().hashCode(), MajaMotion.PERIOD);
        motionTick = (motionTick + 1) % MajaMotion.PERIOD;
        double breach = MajaMotion.breach(motionTick);
        int sea = CityPlan.get().ocean;
        double vertical = MajaMotion.verticalSpeed(getY(), MajaMotion.targetY(sea, motionTick));
        double oldY = getY();
        double rad = Math.toRadians(nextYaw);
        // 0.42 blocks/tick (~8.4 blocks/s): the titan visibly cruises instead of
        // drifting; the 6-block collision margin still covers one tick of travel.
        double cruise = 0.42 * speedFactor;
        Vec3 motion = new Vec3(-Math.sin(rad) * cruise, vertical, Math.cos(rad) * cruise);
        Vec3 next = position().add(motion);
        if (MajaCollision.clear(server, next, nextYaw)) {
            setYRot(nextYaw);
            yBodyRot = nextYaw;
            yHeadRot = nextYaw;
            setDeltaMovement(motion);
            move(MoverType.SELF, motion);
            if ((oldY < sea && getY() >= sea) || (oldY > sea && getY() <= sea)) {
                double hx = getX() - Math.sin(rad) * 145.0;
                double hz = getZ() + Math.cos(rad) * 145.0;
                server.sendParticles(ParticleTypes.SPLASH, hx, sea + 0.2, hz, 36, 12.0, 0.5, 6.0, 0.25);
            }
        } else {
            setDeltaMovement(Vec3.ZERO);
            // If terrain changed underneath the existing envelope, hide rather than render through it.
            if (!MajaCollision.clear(server, position(), getYRot())) entityData.set(READY, false);
            if (tickCount % 30 == 0) {
                // blocked: pick a gentler escape course, still eased by turnRate
                desiredYaw = getYRot() + (random.nextBoolean() ? 52.0F : -52.0F);
                directionTicks = 160;
            }
        }
        // A restrained trail follows the moving head underwater. Do not query
        // unloaded chunks or alter block contents for cosmetic effects.
        if (tickCount % 12 == 0 && !server.getFluidState(blockPosition()).isEmpty()
                && getY() < sea - 5 && motion.horizontalDistanceSqr() > 0.001) {
            double bx = getX() - Math.sin(rad) * 105.0;
            double bz = getZ() + Math.cos(rad) * 105.0;
            server.sendParticles(ParticleTypes.BUBBLE, bx, getY() - 3, bz, 12, 8.0, 1.5, 5.0, 0.06);
        }
        if (tickCount % 2 == 0) {
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
    public boolean canDrownInFluidType(net.minecraftforge.fluids.FluidType type) {
        // Forge handles custom fluids separately from vanilla water; the
        // vanilla canBreatheUnderwater override alone is not enough here.
        return type == ru.aiforge.riftworld.registry.ModFluids.TYPE.get()
                ? false : super.canDrownInFluidType(type);
    }
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
        tag.putFloat("majaTurnRate", turnRate);
        tag.putFloat("majaSpeedFactor", speedFactor);
        tag.putInt("majaMotionTick", motionTick);
        tag.putInt("majaTicks", tickCount);
        tag.putDouble("majaBreach", motionTick < 0 ? 0.0 : MajaMotion.breach(motionTick));
        tag.putString("majaFeedStatus", devourer.status());
        tag.putInt("majaBlocksEaten", devourer.blocksEaten());
        // Read-only diagnostics for server operators and the automated collision probe.
        tag.putBoolean("majaReady", isReadyToRender());
        tag.putFloat("majaJaw", entityData.get(JAW));
    }
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        desiredYaw = tag.getFloat("majaDesiredYaw");
        turnRate = tag.contains("majaTurnRate") ? tag.getFloat("majaTurnRate") : 0.0F;
        speedFactor = tag.contains("majaSpeedFactor") ? tag.getFloat("majaSpeedFactor") : 1.0F;
        motionTick = tag.contains("majaMotionTick") ? Math.floorMod(tag.getInt("majaMotionTick"), MajaMotion.PERIOD) : -1;
        // Re-check the loaded world before displaying any part of the model.
        entityData.set(READY, false);
    }
}
