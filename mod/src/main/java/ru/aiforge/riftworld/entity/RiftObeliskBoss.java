package ru.aiforge.riftworld.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.registry.ModEntities;
import ru.aiforge.riftworld.registry.ModItems;

/**
 * Главный Босс Колоннады в биоме «Хрустальные разломы» — «Расколотый Обелиск Разлома»
 * ({@code reports/boss_crystal_refs/01_colonnade_boss_ref.png}).
 *
 * <p>Стационарная левитирующая сущность Разлома без ног и СТРОГО БЕЗ человеческой тени:
 * расколотый бело-мраморный обелиск с сияющим геометрическим призматическим ядром и тремя
 * концентрическими вращающимися руническими кольцами астролябии над алтарём призыва Колоннады.
 *
 * <p>Фазы и атаки:
 * <ol>
 *   <li><b>Залп Призмы:</b> веер из 3–5 стремительных кристальных осколков в цель;</li>
 *   <li><b>Призыв Гранёных Прислужников:</b> открывает разломы у колонн и призывает {@link CrystalMinion};</li>
 *   <li><b>Астральный Импульс Колоннады:</b> раскручивает кольца астролябии и выпускает ударную волну.</li>
 * </ol>
 *
 * <p>Гарантированный трофей: «Призма Архонта Разлома» ({@code riftworld:astral_prism}).
 */
public class RiftObeliskBoss extends Monster {
    public static final int STATE_IDLE = 0;
    public static final int STATE_VOLLEY = 1;
    public static final int STATE_SUMMON = 2;
    public static final int STATE_PULSE = 3;

    private static final EntityDataAccessor<Integer> BOSS_STATE =
            SynchedEntityData.defineId(RiftObeliskBoss.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.translatable("entity.riftworld.rift_obelisk_boss"),
            BossEvent.BossBarColor.BLUE,
            BossEvent.BossBarOverlay.NOTCHED_10);

    private Vec3 anchorPos;
    private int actionTimer = 40;
    private int stateTicks = 0;

    public RiftObeliskBoss(EntityType<? extends RiftObeliskBoss> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setPersistenceRequired();
        this.xpReward = 150;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 260.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    public void setAnchor(double x, double y, double z) {
        this.anchorPos = new Vec3(x, y, z);
        this.setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(BOSS_STATE, STATE_IDLE);
    }

    public int bossState() {
        return this.entityData.get(BOSS_STATE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 40.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if ((this.tickCount & 1) == 0) {
                double ang = this.tickCount * 0.16;
                this.level().addParticle(ParticleTypes.END_ROD,
                        this.getX() + Math.cos(ang) * 1.45,
                        this.getY() + 1.8 + Math.sin(ang * 2.0) * 0.35,
                        this.getZ() + Math.sin(ang) * 1.45,
                        0, 0.015, 0);
            }
            return;
        }

        if (this.anchorPos == null) {
            this.anchorPos = this.position();
        }
        // Плавная стационарная левитация над печатью призыва Колоннады
        double bobY = this.anchorPos.y + Math.sin(this.tickCount * 0.06) * 0.24;
        this.setDeltaMovement(Vec3.ZERO);
        this.setPos(this.anchorPos.x, bobY, this.anchorPos.z);

        this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());

        LivingEntity target = this.getTarget();
        if (target != null && (!target.isAlive() || target.distanceToSqr(this) > 52.0 * 52.0)) {
            this.setTarget(null);
            target = null;
        }
        if (target != null) {
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            this.setYRot(yaw);
            this.yBodyRot = yaw;
        }

        if (this.stateTicks > 0) {
            --this.stateTicks;
            if (this.stateTicks == 0) {
                if (bossState() == STATE_PULSE) {
                    unleashAstralShockwave();
                }
                this.entityData.set(BOSS_STATE, STATE_IDLE);
            }
            return;
        }

        if (target == null) return;
        if (--this.actionTimer > 0) return;

        boolean enraged = this.getHealth() < this.getMaxHealth() * 0.5F;
        double dist2 = this.distanceToSqr(target);
        int activeMinions = this.level().getEntitiesOfClass(CrystalMinion.class, this.getBoundingBox().inflate(32.0)).size();

        if (dist2 < 36.0 && this.random.nextFloat() < 0.48F) {
            // 1) Астральный Импульс Колоннады (телеграфированная раскрутка колец -> волна отбрасывания)
            this.entityData.set(BOSS_STATE, STATE_PULSE);
            this.stateTicks = 20;
            this.actionTimer = enraged ? 42 : 58;
            this.level().playSound(null, this.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.4F, 1.35F);
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.GLOW, this.getX(), this.getY() + 1.8, this.getZ(), 28, 1.2, 1.0, 1.2, 0.05);
            }
        } else if (activeMinions < (enraged ? 5 : 3) && this.random.nextFloat() < 0.45F) {
            // 2) Призыв Гранёных Прислужников вокруг колоннады
            this.entityData.set(BOSS_STATE, STATE_SUMMON);
            this.stateTicks = 16;
            this.actionTimer = enraged ? 55 : 75;
            summonMinions(enraged ? 2 : 2);
        } else {
            // 3) Веерный залп кристальных осколков из призматического ядра
            this.entityData.set(BOSS_STATE, STATE_VOLLEY);
            this.stateTicks = 14;
            this.actionTimer = enraged ? 32 : 46;
            firePrismVolley(target, enraged ? 5 : 3);
        }
    }

    private void firePrismVolley(LivingEntity target, int count) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        Vec3 origin = new Vec3(this.getX(), this.getY() + 2.0, this.getZ());
        Vec3 toTarget = new Vec3(target.getX() - origin.x, (target.getY() + target.getEyeHeight() * 0.6) - origin.y, target.getZ() - origin.z).normalize();
        sl.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1.5F, 1.15F);

        int half = count / 2;
        for (int i = -half; i <= half; ++i) {
            Vec3 spread = toTarget.yRot((float) Math.toRadians(i * 9.5)).normalize().scale(1.15);
            CrystalShardEntity shard = new CrystalShardEntity(ModEntities.CRYSTAL_SHARD.get(), sl, this, 7.5F);
            shard.setPos(origin.x + spread.x * 0.9, origin.y, origin.z + spread.z * 0.9);
            shard.setDeltaMovement(spread);
            sl.addFreshEntity(shard);
        }
    }

    private void summonMinions(int count) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        sl.playSound(null, this.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 1.4F, 1.3F);
        double baseAng = this.random.nextDouble() * Math.PI * 2.0;
        for (int i = 0; i < count; ++i) {
            double ang = baseAng + (Math.PI * 2.0 * i) / count;
            double sx = this.getX() + Math.cos(ang) * 4.5;
            double sy = this.getY() + 0.8;
            double sz = this.getZ() + Math.sin(ang) * 4.5;
            CrystalMinion minion = ModEntities.CRYSTAL_MINION.get().create(sl);
            if (minion == null) continue;
            minion.moveTo(sx, sy, sz, this.getYRot(), 0.0F);
            minion.finalizeSpawn(sl, sl.getCurrentDifficultyAt(BlockPos.containing(sx, sy, sz)), MobSpawnType.MOB_SUMMONED, null, null);
            if (this.getTarget() != null) minion.setTarget(this.getTarget());
            sl.addFreshEntity(minion);
            sl.sendParticles(ParticleTypes.END_ROD, sx, sy + 1.0, sz, 20, 0.35, 0.65, 0.35, 0.05);
        }
    }

    private void unleashAstralShockwave() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        sl.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 0.9F, 1.55F);
        for (int i = 0; i < 36; ++i) {
            double a = (Math.PI * 2.0 * i) / 36.0;
            sl.sendParticles(ParticleTypes.END_ROD,
                    this.getX() + Math.cos(a) * 4.5, this.getY() + 1.0, this.getZ() + Math.sin(a) * 4.5,
                    3, 0.15, 0.15, 0.15, 0.08);
        }
        AABB wave = this.getBoundingBox().inflate(6.8, 3.0, 6.8);
        List<LivingEntity> list = sl.getEntitiesOfClass(LivingEntity.class, wave,
                le -> le.isAlive() && !(le instanceof RiftObeliskBoss) && !(le instanceof CrystalMinion));
        for (LivingEntity le : list) {
            Vec3 away = le.position().subtract(this.position());
            Vec3 horiz = new Vec3(away.x, 0, away.z);
            if (horiz.lengthSqr() < 1.0E-4) horiz = new Vec3(1, 0, 0);
            horiz = horiz.normalize();
            le.hurt(this.damageSources().mobAttack(this), 9.0F);
            le.knockback(1.4, -horiz.x, -horiz.z);
            le.hurtMarked = true;
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_BLOCK_RESONATE;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BEACON_DEACTIVATE;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        // Гарантированный уникальный сбалансированный трофей с Босса Колоннады!
        this.spawnAtLocation(new ItemStack(ModItems.ASTRAL_PRISM.get()));
        this.spawnAtLocation(new ItemStack(ModItems.CRYSTAL_BARRAGE.get(), 5 + this.random.nextInt(4)));
        this.spawnAtLocation(new ItemStack(ModItems.CRYSTAL_AEGIS.get(), 3 + this.random.nextInt(3)));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.anchorPos != null) {
            tag.putDouble("AnchorX", this.anchorPos.x);
            tag.putDouble("AnchorY", this.anchorPos.y);
            tag.putDouble("AnchorZ", this.anchorPos.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AnchorX")) {
            this.anchorPos = new Vec3(tag.getDouble("AnchorX"), tag.getDouble("AnchorY"), tag.getDouble("AnchorZ"));
        }
        if (this.hasCustomName()) {
            this.bossBar.setName(this.getDisplayName());
        }
    }
}
