package ru.aiforge.riftworld.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * 3D-сущность кристального щита «Бастион Граней» ({@code crystal_aegis}):
 * <ul>
 *   <li><b>Режим RAM (ЛКМ):</b> спавнится перед игроком и стремительно движется вперёд,
 *       нанося урон и мощно отбрасывая всё живое на своём пути.</li>
 *   <li><b>Режим GUARD (ПКМ):</b> спавнится перед игроком и защищает максимум 6 секунд (120 тиков),
 *       перехватывая вражеские снаряды, оттесняя врагов и поглощая фронтальные удары.</li>
 * </ul>
 */
public class CrystalShieldEntity extends Entity {
    public static final int MODE_GUARD = 0;
    public static final int MODE_RAM = 1;
    /** Строго максимум 6 секунд защиты (120 тиков). */
    public static final int MAX_GUARD_TICKS = 120;
    public static final int MAX_RAM_TICKS = 36;

    private static final EntityDataAccessor<Integer> MODE =
            SynchedEntityData.defineId(CrystalShieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(CrystalShieldEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> SHIELD_YAW =
            SynchedEntityData.defineId(CrystalShieldEntity.class, EntityDataSerializers.FLOAT);

    private Vec3 ramVelocity = Vec3.ZERO;
    private float integrity = 45.0F;
    private final Set<Integer> ramHitIds = new HashSet<>();

    public CrystalShieldEntity(EntityType<? extends CrystalShieldEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void initGuard(Player owner) {
        this.entityData.set(MODE, MODE_GUARD);
        this.entityData.set(OWNER, Optional.of(owner.getUUID()));
        this.entityData.set(SHIELD_YAW, owner.getYRot());
        this.setYRot(owner.getYRot());
        Vec3 fwd = Vec3.directionFromRotation(0.0F, owner.getYRot());
        this.setPos(owner.getX() + fwd.x * 1.85, owner.getY() + 0.1, owner.getZ() + fwd.z * 1.85);
    }

    public void initRam(Player owner) {
        this.entityData.set(MODE, MODE_RAM);
        this.entityData.set(OWNER, Optional.of(owner.getUUID()));
        this.entityData.set(SHIELD_YAW, owner.getYRot());
        this.setYRot(owner.getYRot());
        Vec3 look = owner.getLookAngle();
        Vec3 horiz = new Vec3(look.x, look.y * 0.25, look.z);
        if (horiz.lengthSqr() < 1.0E-4) {
            horiz = Vec3.directionFromRotation(0.0F, owner.getYRot());
        } else {
            horiz = horiz.normalize();
        }
        this.ramVelocity = horiz.scale(0.55);
        this.setPos(owner.getX() + horiz.x * 1.5, owner.getY() + 0.15, owner.getZ() + horiz.z * 1.5);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(MODE, MODE_GUARD);
        this.entityData.define(OWNER, Optional.empty());
        this.entityData.define(SHIELD_YAW, 0.0F);
    }

    public int mode() {
        return this.entityData.get(MODE);
    }

    public float shieldYaw() {
        return this.entityData.get(SHIELD_YAW);
    }

    public UUID ownerUuid() {
        return this.entityData.get(OWNER).orElse(null);
    }

    public Player findOwner() {
        UUID id = ownerUuid();
        return id == null ? null : this.level().getPlayerByUUID(id);
    }

    public boolean absorbHit(float amount) {
        this.integrity -= amount;
        if (this.level() instanceof ServerLevel sl) {
            sl.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.25F);
            sl.sendParticles(ParticleTypes.GLOW, this.getX(), this.getY() + 1.0, this.getZ(), 10, 0.6, 0.6, 0.6, 0.04);
        }
        if (this.integrity <= 0.0F) {
            shatter();
            return true;
        }
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (mode() == MODE_RAM) {
            tickRam();
        } else {
            tickGuard();
        }
    }

    private void tickRam() {
        if (this.tickCount > MAX_RAM_TICKS) {
            shatter();
            return;
        }
        if (this.level().isClientSide) {
            for (int i = 0; i < 2; ++i) {
                this.level().addParticle(ParticleTypes.END_ROD,
                        this.getX() + (this.random.nextDouble() - 0.5) * 1.8,
                        this.getY() + 0.3 + this.random.nextDouble() * 1.5,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 1.8,
                        0, 0.02, 0);
            }
            return;
        }

        // Проверяем столкновение с плотной стеной после старта
        Vec3 next = this.position().add(this.ramVelocity);
        if (this.tickCount > 4 && this.level().getBlockState(net.minecraft.core.BlockPos.containing(next.x, next.y + 0.9, next.z)).isSolid()) {
            shatter();
            return;
        }
        this.setPos(next.x, next.y, next.z);

        // Наносим урон и мощно отбрасываем ВСЁ ЖИВОЕ перед таранным щитом!
        Player owner = findOwner();
        AABB hitBox = this.getBoundingBox().inflate(1.25, 0.45, 1.25);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, hitBox,
                le -> le.isAlive() && (owner == null || le != owner) && !this.ramHitIds.contains(le.getId()));
        Vec3 dir = this.ramVelocity.lengthSqr() > 1.0E-5
                ? this.ramVelocity.normalize()
                : Vec3.directionFromRotation(0.0F, shieldYaw());

        for (LivingEntity le : targets) {
            this.ramHitIds.add(le.getId());
            le.hurt(owner != null ? this.damageSources().playerAttack(owner) : this.damageSources().magic(), 8.5F);
            le.knockback(1.55, -dir.x, -dir.z);
            le.setDeltaMovement(le.getDeltaMovement().add(dir.x * 0.45, 0.32, dir.z * 0.45));
            le.hurtMarked = true;
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.END_ROD, le.getX(), le.getY() + 0.9, le.getZ(), 12, 0.3, 0.4, 0.3, 0.08);
                sl.playSound(null, le.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 0.95F, 1.15F);
                sl.playSound(null, le.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.9F);
            }
        }
    }

    private void tickGuard() {
        if (this.tickCount > MAX_GUARD_TICKS) {
            shatter();
            return;
        }
        if (this.level().isClientSide) {
            if ((this.tickCount & 3) == 0) {
                this.level().addParticle(ParticleTypes.GLOW,
                        this.getX() + (this.random.nextDouble() - 0.5) * 1.6,
                        this.getY() + 0.2 + this.random.nextDouble() * 1.6,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 1.6,
                        0, 0.01, 0);
            }
            return;
        }

        UUID oid = ownerUuid();
        if (oid == null) {
            // Если щит вызван командой /summon без владельца — стоит на месте до 6 секунд, повернувшись по YRot
            if (this.tickCount == 1) {
                Player near = this.level().getNearestPlayer(this, 8.0);
                if (near != null) this.setYRot(near.getYRot());
                this.entityData.set(SHIELD_YAW, this.getYRot());
            }
            return;
        }
        Player owner = findOwner();
        if (owner == null || !owner.isAlive() || owner.level() != this.level()) {
            shatter();
            return;
        }

        // Плавно держим защитный щит перед взглядом игрока (макс. 6 секунд)
        float yaw = Mth.rotLerp(0.45F, shieldYaw(), owner.getYRot());
        this.entityData.set(SHIELD_YAW, yaw);
        this.setYRot(yaw);
        Vec3 fwd = Vec3.directionFromRotation(0.0F, yaw);
        this.setPos(owner.getX() + fwd.x * 1.85, owner.getY() + 0.1, owner.getZ() + fwd.z * 1.85);

        // Перехват любых вражеских снарядов
        AABB barrier = this.getBoundingBox().inflate(1.3, 0.5, 1.3);
        for (Projectile proj : this.level().getEntitiesOfClass(Projectile.class, barrier)) {
            if (proj.getOwner() == owner) continue;
            proj.discard();
            absorbHit(5.0F);
            if (this.isRemoved()) return;
        }

        // Оттеснение враждебных существ, пытающихся пройти сквозь щит к игроку
        for (LivingEntity mob : this.level().getEntitiesOfClass(LivingEntity.class, barrier)) {
            if (mob == owner) continue;
            mob.push(fwd.x * 0.22, 0.04, fwd.z * 0.22);
        }
    }

    private void shatter() {
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.0, this.getZ(), 16, 0.6, 0.6, 0.6, 0.06);
            sl.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.9F, 1.1F);
        }
        this.discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(MODE, tag.getInt("Mode"));
        this.entityData.set(SHIELD_YAW, tag.getFloat("Yaw"));
        if (tag.hasUUID("Owner")) this.entityData.set(OWNER, Optional.of(tag.getUUID("Owner")));
        this.ramVelocity = new Vec3(tag.getDouble("Vx"), tag.getDouble("Vy"), tag.getDouble("Vz"));
        this.integrity = tag.contains("Hp") ? tag.getFloat("Hp") : 45.0F;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Mode", mode());
        tag.putFloat("Yaw", shieldYaw());
        UUID id = ownerUuid();
        if (id != null) tag.putUUID("Owner", id);
        tag.putDouble("Vx", this.ramVelocity.x);
        tag.putDouble("Vy", this.ramVelocity.y);
        tag.putDouble("Vz", this.ramVelocity.z);
        tag.putFloat("Hp", this.integrity);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
