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
    public static final int STATE_ICE_PILLARS = 4;
    public static final int STATE_ICE_HAIL = 5;
    public static final int STATE_GLACIAL_WAVE = 6;
    public static final int STATE_FROST_BEAMS = 7;

    private static final EntityDataAccessor<Integer> BOSS_STATE =
            SynchedEntityData.defineId(RiftObeliskBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FROST_BEAM_TICKS =
            SynchedEntityData.defineId(RiftObeliskBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_FROST_BEAM_START_YAW =
            SynchedEntityData.defineId(RiftObeliskBoss.class, EntityDataSerializers.FLOAT);

    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.translatable("entity.riftworld.rift_obelisk_boss"),
            BossEvent.BossBarColor.BLUE,
            BossEvent.BossBarOverlay.NOTCHED_10);

    private Vec3 anchorPos;
    private int actionTimer = 36;
    private int stateTicks = 0;
    private int customAttackCycle = 0;

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
        this.entityData.define(DATA_FROST_BEAM_TICKS, 0);
        this.entityData.define(DATA_FROST_BEAM_START_YAW, 0.0F);
    }

    public int bossState() {
        return this.entityData.get(BOSS_STATE);
    }

    public int frostBeamTicks() {
        return this.entityData.get(DATA_FROST_BEAM_TICKS);
    }

    public float frostBeamStartYaw() {
        return this.entityData.get(DATA_FROST_BEAM_START_YAW);
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
                        this.getX() + Math.cos(ang) * 2.55,
                        this.getY() + 2.95 + Math.sin(ang * 2.0) * 0.55,
                        this.getZ() + Math.sin(ang) * 2.55,
                        0, 0.02, 0);
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

        int bt = frostBeamTicks();
        if (bt > 0) {
            this.entityData.set(DATA_FROST_BEAM_TICKS, bt - 1);
            tickSweepingFrostBeams(bt);
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

        // Чередуем 4 сигнатурные ледяные/призматические атаки с классическими залпами и прислужниками!
        int pick = (this.customAttackCycle++) % 6;
        if (pick == 0) {
            this.entityData.set(BOSS_STATE, STATE_ICE_PILLARS);
            this.stateTicks = 22;
            this.actionTimer = enraged ? 48 : 66;
            castUnjoinedIcePillarCircle(target.position());
        } else if (pick == 1) {
            this.entityData.set(BOSS_STATE, STATE_ICE_HAIL);
            this.stateTicks = 24;
            this.actionTimer = enraged ? 54 : 72;
            castSkyIceHailVolley(target.position());
        } else if (pick == 2) {
            this.entityData.set(BOSS_STATE, STATE_GLACIAL_WAVE);
            this.stateTicks = 18;
            this.actionTimer = enraged ? 42 : 58;
            castGlacialSpikeWave(target.position());
        } else if (pick == 3) {
            this.entityData.set(BOSS_STATE, STATE_FROST_BEAMS);
            this.stateTicks = 24;
            this.actionTimer = enraged ? 56 : 74;
            castSweepingFrostBeams(target);
        } else if (dist2 < 36.0 && this.random.nextFloat() < 0.48F) {
            this.entityData.set(BOSS_STATE, STATE_PULSE);
            this.stateTicks = 20;
            this.actionTimer = enraged ? 42 : 58;
            this.level().playSound(null, this.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.4F, 1.35F);
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.GLOW, this.getX(), this.getY() + 1.8, this.getZ(), 28, 1.2, 1.0, 1.2, 0.05);
            }
        } else if (activeMinions < (enraged ? 5 : 3) && this.random.nextFloat() < 0.55F) {
            this.entityData.set(BOSS_STATE, STATE_SUMMON);
            this.stateTicks = 16;
            this.actionTimer = enraged ? 52 : 68;
            summonMinions(2);
        } else {
            this.entityData.set(BOSS_STATE, STATE_VOLLEY);
            this.stateTicks = 14;
            this.actionTimer = enraged ? 30 : 44;
            firePrismVolley(target, enraged ? 5 : 3);
        }
    }

    private void firePrismVolley(LivingEntity target, int count) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        Vec3 origin = new Vec3(this.getX(), this.getY() + 2.85, this.getZ());
        Vec3 toTarget = new Vec3(target.getX() - origin.x, (target.getY() + target.getEyeHeight() * 0.6) - origin.y, target.getZ() - origin.z).normalize();
        sl.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1.5F, 1.15F);

        int half = count / 2;
        for (int i = -half; i <= half; ++i) {
            Vec3 spread = toTarget.yRot((float) Math.toRadians(i * 9.5)).normalize().scale(1.15);
            CrystalShardEntity shard = new CrystalShardEntity(ModEntities.CRYSTAL_SHARD.get(), sl, this, 7.5F);
            shard.setPos(origin.x + spread.x * 1.35, origin.y, origin.z + spread.z * 1.35);
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
            double sx = this.getX() + Math.cos(ang) * 7.4;
            double sy = this.getY() + 0.4;
            double sz = this.getZ() + Math.sin(ang) * 7.4;
            CrystalMinion minion = ModEntities.CRYSTAL_MINION.get().create(sl);
            if (minion == null) continue;
            minion.moveTo(sx, sy, sz, this.getYRot(), 0.0F);
            minion.finalizeSpawn(sl, sl.getCurrentDifficultyAt(BlockPos.containing(sx, sy, sz)), MobSpawnType.MOB_SUMMONED, null, null);
            if (this.getTarget() != null) minion.setTarget(this.getTarget());
            sl.addFreshEntity(minion);
            sl.sendParticles(ParticleTypes.END_ROD, sx, sy + 1.0, sz, 20, 0.35, 0.65, 0.35, 0.05);
        }
    }

    /**
     * КАСТОМНАЯ АТАКА #1 (v0.23.0): Из-под земли БОЛЬШИМ РАДИУСОМ (7.85 м!) плавно выходят 5 вытянутых
     * светлых кристальных столбов, образуя НЕ СОМКНУТЫЙ круг (широкий открытый проход 108° для выхода),
     * поднимают наверх тех, кто стоит на месте подъёма столба, а затем плавно уходят обратно под землю!
     */
    public void castUnjoinedIcePillarCircle(Vec3 aroundPos) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        sl.playSound(null, this.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 1.35F, 1.32F);

        double radius = 7.85;
        double gapCenterAng = Math.atan2(this.getZ() - aroundPos.z, this.getX() - aroundPos.x) + Math.PI;
        double startAng = gapCenterAng + Math.toRadians(54.0);
        double stepAng = Math.toRadians(63.0); // 4 интервала по 63° = 252°, остаётся разомкнутая дуга 108° для выхода!

        for (int i = 0; i < 5; ++i) {
            double ang = startAng + i * stepAng;
            double px = aroundPos.x + Math.cos(ang) * radius;
            double pz = aroundPos.z + Math.sin(ang) * radius;
            int gy = findArenaFloorY(sl, px, aroundPos.y, pz);

            IcePillarEntity pillar = ModEntities.ICE_PILLAR.get().create(sl);
            if (pillar == null) continue;
            pillar.moveTo(px, gy, pz, (float) Math.toDegrees(ang), 0.0F);
            pillar.configure(this, 7.6F, false, 0);
            sl.addFreshEntity(pillar);
        }
    }

    /**
     * КАСТОМНАЯ АТАКА #2 (v0.23.0): Град из 5 вытянутых светлых кристальных глыб-копий в небе:
     * сначала появляются 5 меток куда они прилетят, и по очереди (1 за 1) глыбы летят в разные точки!
     */
    public void castSkyIceHailVolley(Vec3 centerTarget) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        sl.playSound(null, this.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.4F, 0.78F);

        double baseAng = 0.42;
        double[][] offsets = {
                {0.0, 0.0},
                {Math.cos(baseAng) * 5.8, Math.sin(baseAng) * 5.8},
                {Math.cos(baseAng + 1.32) * 7.2, Math.sin(baseAng + 1.32) * 7.2},
                {Math.cos(baseAng + 2.68) * 6.4, Math.sin(baseAng + 2.68) * 6.4},
                {Math.cos(baseAng + 4.18) * 7.6, Math.sin(baseAng + 4.18) * 7.6}
        };

        for (int i = 0; i < 5; ++i) {
            double tx = centerTarget.x + offsets[i][0];
            double tz = centerTarget.z + offsets[i][1];
            int ty = findArenaFloorY(sl, tx, centerTarget.y, tz);

            double skyArc = baseAng + (i - 2) * 0.50;
            double sx = centerTarget.x + Math.cos(skyArc) * 6.4;
            double sy = ty + 14.2 + (i % 2) * 1.6;
            double sz = centerTarget.z + Math.sin(skyArc) * 6.4;

            IceHailBoulderEntity boulder = ModEntities.ICE_HAIL_BOULDER.get().create(sl);
            if (boulder == null) continue;
            boulder.moveTo(sx, sy, sz, i * 72.0F, 0.0F);
            // Метки на земле загораются СРАЗУ для всех 5, а летят вытянутые кристальные глыбы по очереди 1 за 1 (тик 28, 42, 56, 70, 84)!
            boulder.configure(this, new Vec3(tx, ty, tz), 28 + i * 14);
            sl.addFreshEntity(boulder);
        }
    }

    /** КАСТОМНАЯ АТАКА #3: Расходящаяся 3-лучевая волна вытянутых светлых кристальных шипов из-под земли. */
    public void castGlacialSpikeWave(Vec3 towardPos) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        sl.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.4F, 0.62F);

        double baseAng = Math.atan2(towardPos.z - this.getZ(), towardPos.x - this.getX());
        double[] fan = {-0.34, 0.0, +0.34};
        for (double delta : fan) {
            double ang = baseAng + delta;
            for (int step = 0; step < 8; ++step) {
                double dist = 5.0 + step * 2.85;
                double sx = this.getX() + Math.cos(ang) * dist;
                double sz = this.getZ() + Math.sin(ang) * dist;
                int gy = findArenaFloorY(sl, sx, towardPos.y, sz);

                IcePillarEntity spike = ModEntities.ICE_PILLAR.get().create(sl);
                if (spike == null) continue;
                spike.moveTo(sx, gy, sz, (float) Math.toDegrees(ang), 0.0F);
                spike.configure(this, 4.6F + (step % 2) * 0.85F, true, -step * 3);
                sl.addFreshEntity(spike);
            }
        }
    }

    /** КАСТОМНАЯ АТАКА #4: 4 вращающихся крестообразных морозно-призматических луча вокруг арены. */
    public void castSweepingFrostBeams(LivingEntity target) {
        this.entityData.set(DATA_FROST_BEAM_START_YAW, this.getYRot() + 28.0F);
        this.entityData.set(DATA_FROST_BEAM_TICKS, 64);
        this.level().playSound(null, this.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.4F, 0.72F);
    }

    private void tickSweepingFrostBeams(int remainingTicks) {
        if (!(this.level() instanceof ServerLevel sl) || remainingTicks > 48) return;
        float prog = (48 - remainingTicks) / 48.0F;
        float baseDeg = frostBeamStartYaw() + prog * 115.0F;
        double beamLen = 22.0;
        double beamY = this.getY() + 1.15;

        for (int arm = 0; arm < 4; ++arm) {
            double rad = Math.toRadians(baseDeg + arm * 90.0F);
            double ux = Math.cos(rad), uz = Math.sin(rad);
            if (remainingTicks % 3 == 0) {
                for (double d = 3.0; d <= beamLen; d += 3.5) {
                    sl.sendParticles(ParticleTypes.SNOWFLAKE,
                            this.getX() + ux * d, beamY, this.getZ() + uz * d,
                            1, 0.12, 0.12, 0.12, 0.01);
                }
            }
            if (remainingTicks % 5 == 0) {
                AABB scan = this.getBoundingBox().inflate(beamLen, 3.5, beamLen);
                for (LivingEntity v : sl.getEntitiesOfClass(LivingEntity.class, scan,
                        e -> e.isAlive() && !(e instanceof RiftObeliskBoss) && !(e instanceof CrystalMinion))) {
                    double vx = v.getX() - this.getX(), vz = v.getZ() - this.getZ();
                    double proj = vx * ux + vz * uz;
                    if (proj < 1.8 || proj > beamLen) continue;
                    double perp = Math.abs(-vx * uz + vz * ux);
                    if (perp <= 0.95 && Math.abs((v.getY() + v.getBbHeight() * 0.5) - beamY) <= 1.95) {
                        v.hurt(sl.damageSources().freeze(), 4.5F);
                    }
                }
            }
        }
    }

    private static int findArenaFloorY(ServerLevel sl, double wx, double hintY, double wz) {
        int ix = Mth.floor(wx), iz = Mth.floor(wz);
        int startY = Mth.floor(hintY) + 5;
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int y = startY; y >= startY - 14; --y) {
            mp.set(ix, y, iz);
            if (sl.getBlockState(mp).isSolidRender(sl, mp)) {
                return y + 1;
            }
        }
        return Mth.floor(hintY);
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
