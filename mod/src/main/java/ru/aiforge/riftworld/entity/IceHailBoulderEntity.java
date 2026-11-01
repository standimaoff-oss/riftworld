package ru.aiforge.riftworld.entity;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * Кастомная атака Босса #2: Град из 5 огромных острых квадратных ледяных глыб в небе:
 * <ol>
 *   <li>В небе появляются все 5 ледяных глыб, и СНАЧАЛА на земле загораются 5 меток точек их прилёта;</li>
 *   <li>Затем глыбы ПО ОЧЕРЕДИ (1 за 1 с задержкой {@code launchDelay}) стремительно летят каждая в свою точку!</li>
 * </ol>
 */
public class IceHailBoulderEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_AGE =
            SynchedEntityData.defineId(IceHailBoulderEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LAUNCH_TICK =
            SynchedEntityData.defineId(IceHailBoulderEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_TARGET_X =
            SynchedEntityData.defineId(IceHailBoulderEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_Y =
            SynchedEntityData.defineId(IceHailBoulderEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_Z =
            SynchedEntityData.defineId(IceHailBoulderEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_IMPACT_AGE =
            SynchedEntityData.defineId(IceHailBoulderEntity.class, EntityDataSerializers.INT);

    private static final double FLIGHT_SPEED = 1.48;

    @Nullable
    private UUID ownerId;

    public IceHailBoulderEntity(EntityType<? extends IceHailBoulderEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public void configure(@Nullable LivingEntity owner, Vec3 targetGround, int launchDelayTicks) {
        this.ownerId = owner != null ? owner.getUUID() : null;
        this.entityData.set(DATA_TARGET_X, (float) targetGround.x);
        this.entityData.set(DATA_TARGET_Y, (float) targetGround.y);
        this.entityData.set(DATA_TARGET_Z, (float) targetGround.z);
        this.entityData.set(DATA_LAUNCH_TICK, launchDelayTicks);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_AGE, 0);
        this.entityData.define(DATA_LAUNCH_TICK, 30);
        this.entityData.define(DATA_TARGET_X, 0.0F);
        this.entityData.define(DATA_TARGET_Y, 64.0F);
        this.entityData.define(DATA_TARGET_Z, 0.0F);
        this.entityData.define(DATA_IMPACT_AGE, -1);
    }

    public int boulderAge() {
        return this.entityData.get(DATA_AGE);
    }

    public int launchTick() {
        return this.entityData.get(DATA_LAUNCH_TICK);
    }

    public int impactAge() {
        return this.entityData.get(DATA_IMPACT_AGE);
    }

    public boolean hasImpacted() {
        return impactAge() >= 0;
    }

    public Vec3 targetGround() {
        return new Vec3(
                this.entityData.get(DATA_TARGET_X),
                this.entityData.get(DATA_TARGET_Y),
                this.entityData.get(DATA_TARGET_Z));
    }

    @Override
    public void tick() {
        super.tick();
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();

        int a = boulderAge() + 1;
        if (!level().isClientSide) {
            this.entityData.set(DATA_AGE, a);
        } else {
            a = boulderAge();
        }

        Vec3 target = targetGround();
        int launch = launchTick();

        if (!hasImpacted()) {
            if (a < launch) {
                // Фаза 1: глыба парит высоко в небе и покачивается, а на земле горит метка куда она прилетит!
                double hoverBob = Mth.sin(a * 0.22F + getId()) * 0.035;
                setPos(getX(), getY() + hoverBob, getZ());
                if (!level().isClientSide && level() instanceof ServerLevel sl && a % 4 == 0) {
                    sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(), 3, 0.55, 0.55, 0.55, 0.01);
                    sl.sendParticles(ParticleTypes.END_ROD, target.x, target.y + 0.12, target.z, 2, 0.85, 0.05, 0.85, 0.005);
                }
            } else {
                // Фаза 2: глыба срывается с места и стремительно летит точно в свою наземную метку!
                if (!level().isClientSide && a == launch && level() instanceof ServerLevel slLaunch) {
                    slLaunch.playSound(null, BlockPos.containing(getX(), getY(), getZ()),
                            SoundEvents.TRIDENT_THROW, SoundSource.HOSTILE, 1.35F, 0.55F);
                }
                Vec3 cur = position();
                Vec3 toTarget = target.subtract(cur);
                double dist = toTarget.length();
                if (dist <= FLIGHT_SPEED + 0.35) {
                    setPos(target.x, target.y, target.z);
                    if (!level().isClientSide) {
                        onImpact(a, target);
                    }
                } else {
                    Vec3 step = toTarget.scale(FLIGHT_SPEED / dist);
                    setPos(cur.x + step.x, cur.y + step.y, cur.z + step.z);
                    if (!level().isClientSide && level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(), 5, 0.4, 0.4, 0.4, 0.02);
                    }
                }
            }
        } else if (!level().isClientSide && a - impactAge() >= 12) {
            discard();
        }

        if (!level().isClientSide && a > 220) {
            discard();
        }
    }

    private void onImpact(int ageNow, Vec3 target) {
        this.entityData.set(DATA_IMPACT_AGE, ageNow);
        if (!(level() instanceof ServerLevel sl)) return;

        sl.playSound(null, BlockPos.containing(target.x, target.y, target.z),
                SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5F, 0.58F);
        sl.playSound(null, BlockPos.containing(target.x, target.y, target.z),
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.65F, 1.38F);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
                        ru.aiforge.riftworld.registry.ElisarBlocks.CRYSTAL_BLOCK_BRIGHT.get().defaultBlockState()),
                target.x, target.y + 0.6, target.z, 46, 1.15, 0.65, 1.15, 0.18);
        sl.sendParticles(ParticleTypes.END_ROD,
                target.x, target.y + 0.5, target.z, 32, 1.25, 0.55, 1.25, 0.09);

        AABB blast = new AABB(
                target.x - 2.85, target.y - 1.2, target.z - 2.85,
                target.x + 2.85, target.y + 3.2, target.z + 2.85);
        List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, blast,
                v -> v.isAlive() && !(v instanceof RiftObeliskBoss) && !(v instanceof CrystalMinion));
        for (LivingEntity v : victims) {
            if (this.ownerId != null && this.ownerId.equals(v.getUUID())) continue;
            v.hurt(sl.damageSources().freeze(), 7.5F);
            v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        Vec3 t = targetGround();
        return new AABB(
                Math.min(getX(), t.x) - 4.0, Math.min(getY(), t.y) - 2.0, Math.min(getZ(), t.z) - 4.0,
                Math.max(getX(), t.x) + 4.0, Math.max(getY(), t.y) + 4.0, Math.max(getZ(), t.z) + 4.0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSqr) {
        return distSqr < 256.0 * 256.0;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DATA_AGE, tag.getInt("Age"));
        this.entityData.set(DATA_LAUNCH_TICK, tag.getInt("Launch"));
        this.entityData.set(DATA_TARGET_X, tag.getFloat("TX"));
        this.entityData.set(DATA_TARGET_Y, tag.getFloat("TY"));
        this.entityData.set(DATA_TARGET_Z, tag.getFloat("TZ"));
        this.entityData.set(DATA_IMPACT_AGE, tag.getInt("Impact"));
        if (tag.hasUUID("Owner")) this.ownerId = tag.getUUID("Owner");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", boulderAge());
        tag.putInt("Launch", launchTick());
        Vec3 t = targetGround();
        tag.putFloat("TX", (float) t.x);
        tag.putFloat("TY", (float) t.y);
        tag.putFloat("TZ", (float) t.z);
        tag.putInt("Impact", impactAge());
        if (this.ownerId != null) tag.putUUID("Owner", this.ownerId);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
