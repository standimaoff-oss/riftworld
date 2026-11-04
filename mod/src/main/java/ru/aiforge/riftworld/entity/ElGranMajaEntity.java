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

/** A rare ocean titan. It ignores players; no chat or HUD messages are emitted. */
public final class ElGranMajaEntity extends Mob {
    private static final EntityDataAccessor<Boolean> READY = SynchedEntityData.defineId(ElGranMajaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> JAW = SynchedEntityData.defineId(ElGranMajaEntity.class, EntityDataSerializers.FLOAT);
    private float desiredYaw;
    private int directionTicks;
    private int unsafeTicks;
    private float jawPrevious;
    private float jawCurrent;

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
                unsafeTicks = 0;
            } else {
                setDeltaMovement(Vec3.ZERO);
                // Natural spawns check first. A misplaced command-summon fails closed.
                if (++unsafeTicks > 200) discard();
                return;
            }
        }
        if (isNoAi()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }

        if (--directionTicks <= 0) {
            desiredYaw = getYRot() + (random.nextFloat() - 0.5F) * 44.0F;
            directionTicks = 240 + random.nextInt(240);
        }
        float nextYaw = getYRot() + Mth.clamp(Mth.wrapDegrees(desiredYaw - getYRot()), -0.20F, 0.20F);
        double cycle = (tickCount + (getId() & 255)) % 1600;
        double breach = cycle < 220 ? Math.sin(Math.PI * cycle / 220.0) : 0.0;
        double desiredY = CityPlan.get().ocean - 13.0 + 18.0 * breach;
        double vertical = Mth.clamp((desiredY - getY()) * 0.045, -0.22, 0.22);
        double rad = Math.toRadians(nextYaw);
        Vec3 motion = new Vec3(-Math.sin(rad) * 0.16, vertical, Math.cos(rad) * 0.16);
        Vec3 next = position().add(motion);
        if (MajaCollision.clear(server, next, nextYaw)) {
            setYRot(nextYaw);
            yBodyRot = nextYaw;
            yHeadRot = nextYaw;
            setDeltaMovement(motion);
            move(MoverType.SELF, motion);
        } else {
            setDeltaMovement(Vec3.ZERO);
            // If terrain changed underneath the existing envelope, hide rather than render through it.
            if (!MajaCollision.clear(server, position(), getYRot())) entityData.set(READY, false);
            if (tickCount % 30 == 0) {
                desiredYaw = getYRot() + (random.nextBoolean() ? 35.0F : -35.0F);
                directionTicks = 120;
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
    }
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        desiredYaw = tag.getFloat("majaDesiredYaw");
        // Re-check the loaded world before displaying any part of the model.
        entityData.set(READY, false);
    }
}
