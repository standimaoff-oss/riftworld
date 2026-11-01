package ru.aiforge.riftworld.entity;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;
import ru.aiforge.riftworld.flight.PortalRitual;

/**
 * Безблочный белый 3D-геометрический Пролом в Пространстве (от узкого острия у земли
 * до широкого гранёного разлома в небе). При входе внутрь плавно переносит игрока в Элисар!
 */
public class SpatialRiftEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_AGE =
            SynchedEntityData.defineId(SpatialRiftEntity.class, EntityDataSerializers.INT);

    public SpatialRiftEntity(EntityType<? extends SpatialRiftEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_AGE, 0);
    }

    public int riftAge() {
        return this.entityData.get(DATA_AGE);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            int a = riftAge() + 1;
            this.entityData.set(DATA_AGE, a);
            if (a > 24000) {
                discard();
                return;
            }
            if (a >= 6) {
                AABB touchBox = new AABB(
                        getX() - 1.35, getY() - 0.75, getZ() - 1.35,
                        getX() + 1.35, getY() + 4.20, getZ() + 1.35);
                List<ServerPlayer> inside = level().getEntitiesOfClass(ServerPlayer.class, touchBox,
                        p -> p.isAlive() && !p.isSpectator());
                for (ServerPlayer sp : inside) {
                    double dx = sp.getX() - getX();
                    double dz = sp.getZ() - getZ();
                    if (dx * dx + dz * dz <= 1.85) {
                        PortalRitual.beginWalkInTransit(sp, this);
                    }
                }
            }
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return new AABB(
                getX() - 36.0, getY() - 4.0, getZ() - 36.0,
                getX() + 36.0, getY() + 184.0, getZ() + 36.0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSqr) {
        return distSqr < 512.0 * 512.0;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DATA_AGE, tag.getInt("RiftAge"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("RiftAge", riftAge());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
