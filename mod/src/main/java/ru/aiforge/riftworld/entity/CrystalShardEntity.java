package ru.aiforge.riftworld.entity;

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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * Стремительный гранёный кристалл Разлома: выпускается как оружием игрока «Веер Осколков»
 * (одиночный выстрел ЛКМ и каскад из 1..5 кристаллов ПКМ), так и Боссом Колоннады.
 */
public class CrystalShardEntity extends Projectile {
    private static final EntityDataAccessor<Float> DAMAGE =
            SynchedEntityData.defineId(CrystalShardEntity.class, EntityDataSerializers.FLOAT);
    private int life;

    public CrystalShardEntity(EntityType<? extends CrystalShardEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public CrystalShardEntity(EntityType<? extends CrystalShardEntity> type, Level level, LivingEntity owner, float damage) {
        this(type, level);
        this.setOwner(owner);
        this.entityData.set(DAMAGE, damage);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DAMAGE, 7.0F);
    }

    public float damage() {
        return this.entityData.get(DAMAGE);
    }

    @Override
    public void tick() {
        super.tick();
        ++this.life;
        if (this.life > 80) {
            this.discard();
            return;
        }

        Vec3 vel = this.getDeltaMovement();
        if (this.xRotO == 0.0F && this.yRotO == 0.0F && vel.lengthSqr() > 1.0E-5) {
            double h = vel.horizontalDistance();
            this.setYRot((float) (Mth.atan2(vel.x, vel.z) * (180.0 / Math.PI)));
            this.setXRot((float) (Mth.atan2(vel.y, h) * (180.0 / Math.PI)));
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
        }

        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            this.onHit(hit);
            if (this.isRemoved()) return;
        }

        double nx = this.getX() + vel.x;
        double ny = this.getY() + vel.y;
        double nz = this.getZ() + vel.z;
        if (vel.lengthSqr() > 1.0E-5) {
            double h = vel.horizontalDistance();
            this.setYRot((float) (Mth.atan2(vel.x, vel.z) * (180.0 / Math.PI)));
            this.setXRot((float) (Mth.atan2(vel.y, h) * (180.0 / Math.PI)));
        }

        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.GLOW, this.getX(), this.getY() + 0.1, this.getZ(), 0, 0, 0);
            if ((this.tickCount & 1) == 0) {
                this.level().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.1, this.getZ(),
                        -vel.x * 0.08, -vel.y * 0.08, -vel.z * 0.08);
            }
        }
        this.setPos(nx, ny, nz);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (!super.canHitEntity(target)) return false;
        Entity owner = this.getOwner();
        if (owner != null && target == owner) return false;
        // Осколки босса не ранят его собственных прислужников
        if ((owner instanceof RiftObeliskBoss || owner instanceof CrystalMinion)
                && (target instanceof RiftObeliskBoss || target instanceof CrystalMinion)) {
            return false;
        }
        return true;
    }

    @Override
    protected void onHitEntity(EntityHitResult res) {
        super.onHitEntity(res);
        if (this.level().isClientSide) return;
        Entity target = res.getEntity();
        Entity owner = this.getOwner();
        boolean hurt = target.hurt(
                owner instanceof LivingEntity le
                        ? this.damageSources().indirectMagic(this, le)
                        : this.damageSources().magic(),
                damage());
        if (hurt && target instanceof LivingEntity le) {
            Vec3 push = this.getDeltaMovement().normalize().scale(0.45);
            le.push(push.x, 0.12, push.z);
        }
        shatter();
    }

    @Override
    protected void onHitBlock(BlockHitResult res) {
        super.onHitBlock(res);
        if (!this.level().isClientSide) {
            shatter();
        }
    }

    private void shatter() {
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.1, this.getZ(), 8, 0.15, 0.15, 0.15, 0.05);
            sl.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.85F, 1.35F);
        }
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Life", this.life);
        tag.putFloat("Dmg", damage());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.life = tag.getInt("Life");
        if (tag.contains("Dmg")) this.entityData.set(DAMAGE, tag.getFloat("Dmg"));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
