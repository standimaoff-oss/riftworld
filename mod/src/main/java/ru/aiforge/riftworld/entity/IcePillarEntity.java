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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * Кастомная атака Босса #1 (и #3): Ледяной Столб, плавно вырастающий из-под земли и уходящий обратно под землю.
 * <ul>
 *   <li>Твёрдая коллизия ({@link #canBeCollidedWith()} = {@code true});</li>
 *   <li>Если игрок или моб стоит на точке подъёма столба, растущий столб физически и плавно
 *       ПОДНИМАЕТ его наверх, и игрок оказывается стоящим прямо на вершине ледяного столба!</li>
 *   <li>После удержания плавно уходит обратно под землю и исчезает.</li>
 * </ul>
 */
public class IcePillarEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_AGE =
            SynchedEntityData.defineId(IcePillarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_MAX_H =
            SynchedEntityData.defineId(IcePillarEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_WAVE_SPIKE =
            SynchedEntityData.defineId(IcePillarEntity.class, EntityDataSerializers.BOOLEAN);

    private static final float HALF_WIDTH = 0.68F;

    @Nullable
    private UUID ownerId;
    private float prevColHeight = 0.0F;
    private boolean dealtBurstDamage = false;

    public IcePillarEntity(EntityType<? extends IcePillarEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        this.noPhysics = true;
        this.noCulling = true;
    }

    public void configure(@Nullable LivingEntity owner, float maxHeight, boolean waveSpike, int startAgeOffset) {
        this.ownerId = owner != null ? owner.getUUID() : null;
        this.entityData.set(DATA_MAX_H, maxHeight);
        this.entityData.set(DATA_WAVE_SPIKE, waveSpike);
        this.entityData.set(DATA_AGE, startAgeOffset);
        this.prevColHeight = computeHeight(startAgeOffset);
        this.setBoundingBox(makeBoundingBox());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_AGE, 0);
        this.entityData.define(DATA_MAX_H, 7.6F);
        this.entityData.define(DATA_WAVE_SPIKE, false);
    }

    public int pillarAge() {
        return this.entityData.get(DATA_AGE);
    }

    public float maxPillarHeight() {
        return this.entityData.get(DATA_MAX_H);
    }

    public boolean isWaveSpike() {
        return this.entityData.get(DATA_WAVE_SPIKE);
    }

    public int totalLifetime() {
        return isWaveSpike() ? 52 : 156;
    }

    /** Плавная кривая высоты столба над землёй (от 0 до {@code maxH} и обратно в 0 под землю). */
    public float computeHeight(float t) {
        float maxH = maxPillarHeight();
        if (t <= 0.0F) return 0.0F;
        if (isWaveSpike()) {
            // Быстрый ледяной шип волны: 8 тиков телеграф -> 12 тиков подъём -> 14 тиков пауза -> 18 тиков уход под землю
            if (t < 8.0F) return 0.12F * (t / 8.0F);
            if (t < 20.0F) {
                float u = (t - 8.0F) / 12.0F;
                float s = u * u * (3.0F - 2.0F * u);
                return 0.12F + (maxH - 0.12F) * s;
            }
            if (t < 34.0F) return maxH;
            float u = Mth.clamp((t - 34.0F) / 18.0F, 0.0F, 1.0F);
            float s = u * u * (3.0F - 2.0F * u);
            return maxH * (1.0F - s);
        }

        // Классический 5-столбовой разомкнутый круг:
        // 0..14: ледяная трещина на земле; 14..42: плавный подъём (28 тиков); 42..126: стоит (84 тика); 126..154: плавный уход под землю!
        if (t < 14.0F) {
            return 0.18F * (t / 14.0F);
        }
        if (t < 42.0F) {
            float u = (t - 14.0F) / 28.0F;
            float s = u * u * (3.0F - 2.0F * u);
            return 0.18F + (maxH - 0.18F) * s;
        }
        if (t < 126.0F) {
            return maxH;
        }
        float u = Mth.clamp((t - 126.0F) / 28.0F, 0.0F, 1.0F);
        float s = u * u * (3.0F - 2.0F * u);
        return maxH * (1.0F - s);
    }

    @Override
    protected AABB makeBoundingBox() {
        float h = Math.max(0.02F, computeHeight(pillarAge()));
        float hw = isWaveSpike() ? 0.62F : HALF_WIDTH;
        return new AABB(
                getX() - hw, getY(), getZ() - hw,
                getX() + hw, getY() + h, getZ() + hw);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return new AABB(
                getX() - 2.2, getY() - 2.0, getZ() - 2.2,
                getX() + 2.2, getY() + maxPillarHeight() + 2.5, getZ() + 2.2);
    }

    @Override
    public boolean canBeCollidedWith() {
        return isAlive() && computeHeight(pillarAge()) > 0.12F;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(HALF_WIDTH * 2.0F, Math.max(0.1F, computeHeight(pillarAge())));
    }

    @Override
    public void tick() {
        super.tick();
        int nextAge;
        if (!level().isClientSide) {
            nextAge = pillarAge() + 1;
            this.entityData.set(DATA_AGE, nextAge);
        } else {
            nextAge = pillarAge();
        }

        float newH = computeHeight(nextAge);
        float deltaH = newH - this.prevColHeight;
        this.setBoundingBox(makeBoundingBox());

        // ПЛАВНЫЙ ПОДЪЁМ ИГРОКОВ И СУЩЕСТВ ВВЕРХ НА ВЕРШИНУ РАСТУЩЕГО ЛЕДЯНОГО СТОЛБА (И НА СЕРВЕРЕ, И НА КЛИЕНТЕ!)
        float hw = isWaveSpike() ? 0.68F : (HALF_WIDTH + 0.12F);
        if (newH > 0.10F) {
            AABB liftZone = new AABB(
                    getX() - hw, getY() - 0.25, getZ() - hw,
                    getX() + hw, getY() + newH + 0.55, getZ() + hw);
            List<LivingEntity> riders = level().getEntitiesOfClass(LivingEntity.class, liftZone,
                    e -> e.isAlive() && !e.isSpectator() && !(e instanceof RiftObeliskBoss));
            double topY = getY() + newH;
            for (LivingEntity ent : riders) {
                if (deltaH > 0.0005F && ent.getY() < topY - 0.001) {
                    double lift = topY - ent.getY();
                    ent.move(MoverType.SHULKER, new Vec3(0.0, lift, 0.0));
                    if (ent.getY() < topY) {
                        ent.setPos(ent.getX(), topY, ent.getZ());
                    }
                    Vec3 dm = ent.getDeltaMovement();
                    if (dm.y < 0.0) {
                        ent.setDeltaMovement(dm.x, 0.0, dm.z);
                    }
                    ent.setOnGround(true);
                    ent.fallDistance = 0.0F;
                } else if (Math.abs(ent.getY() - topY) <= 0.35 && deltaH < -0.0005F) {
                    // Когда столб плавно уходит обратно под землю, стоящий на нём игрок так же плавно опускается вместе с ним!
                    ent.setPos(ent.getX(), topY, ent.getZ());
                    ent.fallDistance = 0.0F;
                }
            }
        }

        this.prevColHeight = newH;

        if (!level().isClientSide && level() instanceof ServerLevel sl) {
            if (nextAge == 1 || nextAge == 14) {
                sl.playSound(null, BlockPos.containing(getX(), getY(), getZ()),
                        nextAge == 1 ? SoundEvents.AMETHYST_BLOCK_RESONATE : SoundEvents.GLASS_PLACE,
                        SoundSource.HOSTILE, 0.95F, nextAge == 1 ? 0.72F : 0.58F);
            }
            if (nextAge <= 36 && nextAge % 3 == 0) {
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
                                ru.aiforge.riftworld.registry.ElisarBlocks.CRYSTAL_BLOCK_BRIGHT.get().defaultBlockState()),
                        getX(), getY() + 0.15, getZ(), 6, 0.45, 0.12, 0.45, 0.06);
                sl.sendParticles(ParticleTypes.END_ROD,
                        getX(), getY() + Math.max(0.2, newH), getZ(), 4, 0.35, 0.25, 0.35, 0.02);
            }
            // Урон волны ледяных шипов (или лёгкий морозный урон в момент прорыва)
            int burstTick = isWaveSpike() ? 11 : 18;
            if (!this.dealtBurstDamage && nextAge >= burstTick) {
                this.dealtBurstDamage = true;
                if (isWaveSpike()) {
                    AABB hit = new AABB(getX() - 1.15, getY() - 0.3, getZ() - 1.15,
                            getX() + 1.15, getY() + newH + 0.8, getZ() + 1.15);
                    for (LivingEntity victim : sl.getEntitiesOfClass(LivingEntity.class, hit,
                            v -> v.isAlive() && !(v instanceof RiftObeliskBoss) && !(v instanceof CrystalMinion))) {
                        victim.hurt(sl.damageSources().freeze(), 5.5F);
                    }
                }
            }
            if (nextAge == (isWaveSpike() ? 35 : 87)) {
                sl.playSound(null, BlockPos.containing(getX(), getY(), getZ()),
                        SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 0.75F, 0.65F);
            }
            if (nextAge >= totalLifetime()) {
                discard();
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DATA_AGE, tag.getInt("Age"));
        if (tag.contains("MaxH")) this.entityData.set(DATA_MAX_H, tag.getFloat("MaxH"));
        this.entityData.set(DATA_WAVE_SPIKE, tag.getBoolean("Wave"));
        if (tag.hasUUID("Owner")) this.ownerId = tag.getUUID("Owner");
        this.prevColHeight = computeHeight(pillarAge());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", pillarAge());
        tag.putFloat("MaxH", maxPillarHeight());
        tag.putBoolean("Wave", isWaveSpike());
        if (this.ownerId != null) tag.putUUID("Owner", this.ownerId);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
