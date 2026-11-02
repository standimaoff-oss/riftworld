package ru.aiforge.riftworld.entity;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.flight.Flight;
import ru.aiforge.riftworld.item.DragonPaperItem;
import ru.aiforge.riftworld.item.DragonWhistleItem;
import ru.aiforge.riftworld.registry.ModItems;
import ru.aiforge.riftworld.world.CityPlan;
import ru.aiforge.riftworld.world.FloatingIslands;
import ru.aiforge.riftworld.world.InvertedTowers;
import ru.aiforge.riftworld.world.TamedDragonSavedData;

/**
 * v0.26.0: Гигантский (~47.7 м) модульный оригами Бумажный Дракон с полной анимацией конечностей,
 * гнездованием диких особей строго на Летающих Перевёрнутых Башнях, приручением крафтовой бумагой
 * (окрашивающей 2 уса), ванильным и кастомным седлом, управляемым полётом (0 .. 2x Элитр с фейерверками,
 * мышь + W/S + A/D по градусам, пикирование и предел тяги подъёма), режимами питомца и призывом Горном вне симуляции!
 */
public class PaperDragonEntity extends FlyingMob {
    /** 72 spine nodes × 0.54m = 38.9m modular body + 4.3m Imperial Head + 4.5m Tail Fan ≈ 47.7m total length! */
    public static final int SEGS = 72;
    public static final float STEP = 0.54F;
    private static final float MAX_JOINT_COS = 0.9903F; // cos(8.0 deg) — allows smooth coiled spiral AND ribbon flight!
    private static final float MAX_JOINT_SIN = 0.1392F;

    private static final double BASE_SPEED = 0.42;
    /** 2x Vanilla Elytra boosted with Firework Rockets (1.675 * 2.0 = 3.35 blocks/tick = 67.0 m/s)! */
    public static final double MAX_MOUNTED_SPEED = 3.35;
    public static final double MIN_MOUNTED_SPEED = 0.0;

    private static final float MAX_YAW_STEP_DEG = 1.50F;
    private static final float MAX_PITCH_STEP_DEG = 1.05F;

    public static final int FLIGHT_ALONG_ISLAND_RIM = 0;
    public static final int FLIGHT_UNDER_ISLAND     = 1;
    public static final int FLIGHT_OPEN_SKY_SEA     = 2;
    public static final int FLIGHT_OVER_ISLAND      = FLIGHT_ALONG_ISLAND_RIM;
    public static final int FLIGHT_SEEK_TOWER_ROOST = 3;

    public static final int PET_ORBIT_OWNER = 0;
    public static final int PET_SLEEP       = 1;

    public static final double CITY_CENTER_EXCLUSION_R = 335.0;
    public static final double CITY_ISLAND_BUFFER      = 95.0;

    private static final EntityDataAccessor<Float> DATA_PHASE =
            SynchedEntityData.defineId(PaperDragonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_TAMED =
            SynchedEntityData.defineId(PaperDragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(PaperDragonEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_WHISKER_TYPE =
            SynchedEntityData.defineId(PaperDragonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SADDLE_TIER =
            SynchedEntityData.defineId(PaperDragonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_COILED =
            SynchedEntityData.defineId(PaperDragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_DIVING =
            SynchedEntityData.defineId(PaperDragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_PET_MODE =
            SynchedEntityData.defineId(PaperDragonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_THRUST =
            SynchedEntityData.defineId(PaperDragonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SPEED_RATIO =
            SynchedEntityData.defineId(PaperDragonEntity.class, EntityDataSerializers.FLOAT);

    // Server flight & roost state
    private Vec3 targetPoint;
    private Vec3 smoothAimDir;
    private int waypointTimer;
    private int flightMode = FLIGHT_ALONG_ISLAND_RIM;
    private double orbitCenterX, orbitCenterZ, orbitRadius = 90.0, orbitAltY = 115.0;
    private double orbitAngle;
    private int orbitSign = 1;

    // Wild Inverted-Tower roosting state
    @Nullable
    private InvertedTowers.Site roostTower;
    private int wildRoostTimer = 0;
    private int wildNextRoostCooldown = 500;

    // Tamed & mounted flight state
    private int recallGen = 0;
    private int hornApproachTicks = 0;
    private double mountedSpeed = 0.0;
    private byte riderThrottle = 0;
    private byte riderYawStep = 0;
    private float riderAimYaw = 0.0F;
    private float riderAimPitch = 0.0F;
    private int riderInputGrace = 0;

    // Client-side persistent arc-length-locked spine chain
    public boolean chainInit;
    public final double[] chainX     = new double[SEGS];
    public final double[] chainY     = new double[SEGS];
    public final double[] chainZ     = new double[SEGS];
    public final double[] prevChainX = new double[SEGS];
    public final double[] prevChainY = new double[SEGS];
    public final double[] prevChainZ = new double[SEGS];
    public final float[]  chainRoll     = new float[SEGS];
    public final float[]  prevChainRoll = new float[SEGS];

    public double headFx = 0.0, headFy = 0.0, headFz = 1.0;
    public double prevHeadFx = 0.0, prevHeadFy = 0.0, prevHeadFz = 1.0;
    public float bankRoll, bankRollPrev;
    public float jawAnim, jawAnimPrev;
    public float diveBlend, diveBlendPrev;
    public float coilBlend, coilBlendPrev;
    private float prevSmoothYaw;

    public PaperDragonEntity(EntityType<? extends PaperDragonEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.xpReward = 0;
        this.setMaxUpStep(1.5F);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.MOVEMENT_SPEED, BASE_SPEED)
                .add(Attributes.FLYING_SPEED, BASE_SPEED);
    }

    public static boolean isInsideCityNoFlyZone(CityPlan plan, double x, double z, double extraBuffer) {
        double r0 = CITY_CENTER_EXCLUSION_R + extraBuffer;
        if (x * x + z * z < r0 * r0) return true;
        if (plan == null) return false;
        for (CityPlan.Island ci : plan.islands) {
            double lim = ci.R * 1.18 + CITY_ISLAND_BUFFER + extraBuffer;
            if (lim <= 8.0) continue;
            double dx = x - ci.cx;
            double dz = z - ci.cz;
            if (dx * dx + dz * dz < lim * lim) return true;
        }
        return false;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PHASE, 0.0F);
        this.entityData.define(DATA_TAMED, false);
        this.entityData.define(DATA_OWNER, Optional.empty());
        this.entityData.define(DATA_WHISKER_TYPE, DragonPaperItem.WHISKER_IVORY);
        this.entityData.define(DATA_SADDLE_TIER, 0);
        this.entityData.define(DATA_COILED, false);
        this.entityData.define(DATA_DIVING, false);
        this.entityData.define(DATA_PET_MODE, PET_ORBIT_OWNER);
        this.entityData.define(DATA_THRUST, 1.0F);
        this.entityData.define(DATA_SPEED_RATIO, 0.0F);
    }

    public float wavePhase() {
        return this.entityData.get(DATA_PHASE);
    }

    public boolean isTamed() {
        return this.entityData.get(DATA_TAMED);
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER).orElse(null);
    }

    public boolean isOwnedBy(LivingEntity entity) {
        UUID owner = getOwnerUUID();
        return isTamed() && owner != null && owner.equals(entity.getUUID());
    }

    public int whiskerType() {
        return this.entityData.get(DATA_WHISKER_TYPE);
    }

    public int whiskerColorRgb() {
        return DragonPaperItem.colorForWhiskerType(whiskerType());
    }

    public int saddleTier() {
        return this.entityData.get(DATA_SADDLE_TIER);
    }

    public boolean isCoiled() {
        return this.entityData.get(DATA_COILED);
    }

    public void setCoiled(boolean coiled) {
        this.entityData.set(DATA_COILED, coiled);
    }

    public boolean isDiving() {
        return this.entityData.get(DATA_DIVING);
    }

    public int petMode() {
        return this.entityData.get(DATA_PET_MODE);
    }

    public float upwardThrust() {
        return this.entityData.get(DATA_THRUST);
    }

    public float syncedSpeedRatio() {
        return this.entityData.get(DATA_SPEED_RATIO);
    }

    /** Помещает дикого дракона свернувшимся кольцом прямо на вершину Летающей Перевёрнутой Башни! */
    public void placeCoiledOnWildTower(InvertedTowers.Site site) {
        this.roostTower = site;
        this.wildRoostTimer = 1400 + this.random.nextInt(900);
        this.moveTo(site.cx() + 0.5, site.roostY() + 1.05, site.cz() + 0.5, this.random.nextFloat() * 360.0F, 0.0F);
        this.setDeltaMovement(Vec3.ZERO);
        this.setCoiled(true);
    }

    public void tameForPlayer(Player player, int whiskerColorType) {
        this.entityData.set(DATA_TAMED, true);
        this.entityData.set(DATA_OWNER, Optional.of(player.getUUID()));
        this.entityData.set(DATA_WHISKER_TYPE, whiskerColorType);
        this.entityData.set(DATA_PET_MODE, PET_SLEEP);
        this.setCoiled(true);
        this.setPersistenceRequired();
        syncToSavedData();
    }

    public void equipSaddleTier(int tier) {
        this.entityData.set(DATA_SADDLE_TIER, Mth.clamp(tier, 0, 2));
        syncToSavedData();
    }

    public void restoreFromSavedEntry(TamedDragonSavedData.Entry e) {
        this.entityData.set(DATA_PHASE, this.random.nextFloat() * Mth.TWO_PI);
        this.entityData.set(DATA_TAMED, true);
        this.entityData.set(DATA_OWNER, Optional.of(e.ownerId));
        this.entityData.set(DATA_WHISKER_TYPE, e.whiskerType);
        this.entityData.set(DATA_SADDLE_TIER, e.saddleTier);
        this.entityData.set(DATA_PET_MODE, PET_ORBIT_OWNER);
        this.setCoiled(false);
        this.entityData.set(DATA_DIVING, false);
        this.entityData.set(DATA_THRUST, 1.0F);
        this.recallGen = e.recallGen;
        this.setHealth(Mth.clamp(e.health, 10.0F, this.getMaxHealth()));
        if (e.customNameJson != null && !e.customNameJson.isEmpty()) {
            try {
                Component c = Component.Serializer.fromJson(e.customNameJson);
                if (c != null) this.setCustomName(c);
            } catch (Exception ignored) {
            }
        }
        this.setPersistenceRequired();
    }

    public void beginHornSummonToOwner(ServerPlayer owner) {
        this.entityData.set(DATA_PET_MODE, PET_ORBIT_OWNER);
        this.setCoiled(false);
        this.entityData.set(DATA_DIVING, false);
        this.hornApproachTicks = 180;
        this.targetPoint = owner.position().add(0.0, 5.5, 0.0);
        this.smoothAimDir = null;
        syncToSavedData();
    }

    public void applyRiderInput(byte throttle, byte yawDegStep, float aimYaw, float aimPitch) {
        this.riderThrottle = throttle;
        this.riderYawStep = yawDegStep;
        this.riderAimYaw = aimYaw;
        this.riderAimPitch = Mth.clamp(aimPitch, -88.0F, 88.0F);
        this.riderInputGrace = 6;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Tamed", isTamed());
        UUID owner = getOwnerUUID();
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putInt("WhiskerType", whiskerType());
        tag.putInt("SaddleTier", saddleTier());
        tag.putBoolean("Coiled", isCoiled());
        tag.putInt("PetMode", petMode());
        tag.putFloat("UpwardThrust", upwardThrust());
        tag.putInt("RecallGen", recallGen);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Tamed")) this.entityData.set(DATA_TAMED, tag.getBoolean("Tamed"));
        if (tag.hasUUID("Owner")) this.entityData.set(DATA_OWNER, Optional.of(tag.getUUID("Owner")));
        if (tag.contains("WhiskerType")) this.entityData.set(DATA_WHISKER_TYPE, tag.getInt("WhiskerType"));
        if (tag.contains("SaddleTier")) this.entityData.set(DATA_SADDLE_TIER, tag.getInt("SaddleTier"));
        if (tag.contains("Coiled")) this.entityData.set(DATA_COILED, tag.getBoolean("Coiled"));
        if (tag.contains("PetMode")) this.entityData.set(DATA_PET_MODE, tag.getInt("PetMode"));
        if (tag.contains("UpwardThrust")) this.entityData.set(DATA_THRUST, Mth.clamp(tag.getFloat("UpwardThrust"), 0.0F, 1.0F));
        this.recallGen = tag.getInt("RecallGen");
    }

    private void syncToSavedData() {
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel sl)) return;
        UUID owner = getOwnerUUID();
        if (!isTamed() || owner == null) return;
        String nameJson = this.hasCustomName() && this.getCustomName() != null
                ? Component.Serializer.toJson(this.getCustomName()) : "";
        TamedDragonSavedData.get(sl.getServer()).update(
                this.getUUID(), owner, whiskerType(), saddleTier(), this.getHealth(), nameJson, recallGen);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 1) Приручение дикого Бумажного Дракона ОСОБОЙ БУМАГОЙ — строго когда он спит на Перевёрнутой Башне!
        if (!isTamed()) {
            if (stack.getItem() instanceof DragonPaperItem paper) {
                if (!isCoiled()) {
                    if (!this.level().isClientSide) {
                        player.displayClientMessage(Component.translatable("msg.riftworld.dragon_must_rest")
                                .withStyle(ChatFormatting.YELLOW), true);
                    }
                    return InteractionResult.sidedSuccess(this.level().isClientSide);
                }
                if (!this.level().isClientSide && this.level() instanceof ServerLevel sl) {
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    boolean success = player.getAbilities().instabuild || this.random.nextFloat() < 0.55F;
                    if (success) {
                        tameForPlayer(player, paper.whiskerType());
                        sl.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 1.8, this.getZ(), 14, 1.4, 0.7, 1.4, 0.04);
                        sl.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.5, this.getZ(), 22, 1.8, 0.8, 1.8, 0.05);
                        this.level().playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 2.0F, 1.15F);

                        // Выдаём личный Горн Бумажного Дракона (призыв даже вне симуляции!)
                        ItemStack whistle = new ItemStack(ModItems.DRAGON_WHISTLE.get());
                        DragonWhistleItem.bindToDragon(whistle, this);
                        if (!player.getInventory().add(whistle)) {
                            player.drop(whistle, false);
                        }
                        player.displayClientMessage(Component.translatable("msg.riftworld.dragon_tamed")
                                .withStyle(ChatFormatting.GOLD), true);
                    } else {
                        sl.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1.5, this.getZ(), 8, 1.0, 0.5, 1.0, 0.02);
                        this.level().playSound(null, this.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.NEUTRAL, 1.4F, 1.0F);
                    }
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            return super.mobInteract(player, hand);
        }

        // 2) Взаимодействие с приручённым драконом его владельца
        if (!isOwnedBy(player)) {
            return super.mobInteract(player, hand);
        }

        // Смена окраса 2 усов другой крафтовой особой бумагой
        if (stack.getItem() instanceof DragonPaperItem paper && paper.whiskerType() != whiskerType()) {
            if (!this.level().isClientSide && this.level() instanceof ServerLevel sl) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                this.entityData.set(DATA_WHISKER_TYPE, paper.whiskerType());
                syncToSavedData();
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.6, this.getZ(), 12, 1.2, 0.6, 1.2, 0.03);
                this.level().playSound(null, this.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.NEUTRAL, 1.6F, 1.2F);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // Экипировка кастомного Драконьего седла (уровень 2) или ванильного седла (уровень 1)
        if (stack.is(ModItems.DRAGON_SADDLE.get()) && saddleTier() < 2) {
            if (!this.level().isClientSide) {
                int oldTier = saddleTier();
                if (!player.getAbilities().instabuild) stack.shrink(1);
                if (oldTier == 1) {
                    ItemStack oldVanilla = new ItemStack(Items.SADDLE);
                    if (!player.getInventory().add(oldVanilla)) player.drop(oldVanilla, false);
                }
                equipSaddleTier(2);
                this.level().playSound(null, this.blockPosition(), SoundEvents.HORSE_SADDLE, SoundSource.NEUTRAL, 1.5F, 1.1F);
                player.displayClientMessage(Component.translatable("msg.riftworld.dragon_saddle_custom")
                        .withStyle(ChatFormatting.AQUA), true);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (stack.is(Items.SADDLE) && saddleTier() == 0) {
            if (!this.level().isClientSide) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                equipSaddleTier(1);
                this.level().playSound(null, this.blockPosition(), SoundEvents.HORSE_SADDLE, SoundSource.NEUTRAL, 1.5F, 1.0F);
                player.displayClientMessage(Component.translatable("msg.riftworld.dragon_saddle_vanilla")
                        .withStyle(ChatFormatting.GREEN), true);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // Shift + ПКМ: переключение режима питомца (спать кольцом <-> летать рядом с хозяином!)
        if (player.isSecondaryUseActive()) {
            if (!this.level().isClientSide) {
                int nextMode = petMode() == PET_SLEEP ? PET_ORBIT_OWNER : PET_SLEEP;
                this.entityData.set(DATA_PET_MODE, nextMode);
                if (nextMode == PET_SLEEP) {
                    this.setCoiled(true);
                    this.setDeltaMovement(Vec3.ZERO);
                    player.displayClientMessage(Component.translatable("msg.riftworld.dragon_pet_sleep")
                            .withStyle(ChatFormatting.AQUA), true);
                } else {
                    this.setCoiled(false);
                    player.displayClientMessage(Component.translatable("msg.riftworld.dragon_pet_follow")
                            .withStyle(ChatFormatting.GREEN), true);
                }
                syncToSavedData();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // Обычный ПКМ: оседлать дракона (если надето седло!)
        if (saddleTier() <= 0) {
            if (!this.level().isClientSide) {
                player.displayClientMessage(Component.translatable("msg.riftworld.dragon_need_saddle")
                        .withStyle(ChatFormatting.YELLOW), true);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (!this.level().isClientSide && !this.isVehicle()) {
            this.setCoiled(false);
            this.entityData.set(DATA_PET_MODE, PET_ORBIT_OWNER);
            this.mountedSpeed = 0.45;
            this.riderAimYaw = player.getYRot();
            this.riderAimPitch = player.getXRot();
            player.setYRot(this.getYRot());
            player.startRiding(this);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Entity first = this.getFirstPassenger();
        return (first instanceof Player p && isOwnedBy(p)) ? p : null;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunc) {
        if (!this.hasPassenger(passenger)) return;
        float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
        float pitchRad = this.getXRot() * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yawRad) * Mth.cos(pitchRad);
        double fy = -Mth.sin(pitchRad);
        double fz =  Mth.cos(yawRad) * Mth.cos(pitchRad);
        // Посадка всадника на седле за шеей дракона (~1.35 м позади головы и +1.42 м сверху)
        double rx = this.getX() - fx * 1.35;
        double ry = this.getY() - fy * 1.35 + 1.42 + passenger.getMyRidingOffset();
        double rz = this.getZ() - fz * 1.35;
        moveFunc.accept(passenger, rx, ry, rz);
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        if (saddleTier() == 2) {
            this.spawnAtLocation(ModItems.DRAGON_SADDLE.get());
        } else if (saddleTier() == 1) {
            this.spawnAtLocation(Items.SADDLE);
        }
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        if (!this.level().isClientSide && this.level() instanceof ServerLevel sl && isTamed()) {
            TamedDragonSavedData.get(sl.getServer()).remove(this.getUUID());
        }
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        this.entityData.set(DATA_PHASE, this.random.nextFloat() * Mth.TWO_PI);
        this.orbitSign = this.random.nextBoolean() ? 1 : -1;
        this.orbitAngle = this.random.nextDouble() * Mth.TWO_PI;
        if (reason == MobSpawnType.SPAWN_EGG || reason == MobSpawnType.COMMAND) {
            this.setPersistenceRequired();
            if (reason == MobSpawnType.SPAWN_EGG) {
                this.setPos(this.getX(), this.getY() + 4.5, this.getZ());
            }
        }
        return super.finalizeSpawn(level, diff, reason, data, tag);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        int s = this.isVehicle() ? Math.max(steps, 3) : Math.max(steps, 6);
        super.lerpTo(x, y, z, yRot, xRot, teleport ? 1 : s, teleport);
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity other) {
    }

    @Override
    public boolean removeWhenFarAway(double distSq) {
        return !isTamed() && distSq > 360.0 * 360.0;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSq) {
        return distSq < 384.0 * 384.0;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(96.0, 64.0, 96.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            clientSpineTick();
        } else {
            if (isTamed() && this.level() instanceof ServerLevel sl) {
                TamedDragonSavedData saved = TamedDragonSavedData.get(sl.getServer());
                TamedDragonSavedData.Entry rec = saved.getEntry(this.getUUID());
                // Если эта копия была выгружена и уже отозвана Горном с новым UUID/поколением — тихо удаляем старый дубликат!
                if (rec == null && recallGen > 0) {
                    this.discard();
                    return;
                }
                if (rec != null && rec.recallGen > this.recallGen) {
                    this.discard();
                    return;
                }
                if (this.tickCount % 60 == 0) {
                    syncToSavedData();
                }
            }
            serverFlightTick();
        }
    }

    // =========================================================================
    // CLIENT: PERSISTENT 72-NODE SPINE CHAIN (FLIGHT + COILED SLEEP + DIVE)
    // =========================================================================

    private void clientSpineTick() {
        float curYaw = this.getYRot();
        float pitchLim = isDiving() ? 88.0F : 52.0F;
        float curPitch = Mth.clamp(this.getXRot(), -pitchLim, pitchLim);
        float yawRad = curYaw * Mth.DEG_TO_RAD;
        float pitchRad = curPitch * Mth.DEG_TO_RAD;
        double rawFx = -Mth.sin(yawRad) * Mth.cos(pitchRad);
        double rawFy = -Mth.sin(pitchRad);
        double rawFz =  Mth.cos(yawRad) * Mth.cos(pitchRad);

        if (!chainInit) {
            chainInit = true;
            prevSmoothYaw = curYaw;
            headFx = prevHeadFx = rawFx;
            headFy = prevHeadFy = rawFy;
            headFz = prevHeadFz = rawFz;
            coilBlend = coilBlendPrev = isCoiled() ? 1.0F : 0.0F;
            diveBlend = diveBlendPrev = isDiving() ? 1.0F : 0.0F;
            if (isCoiled()) {
                fillCoiledSpiral(chainX, chainY, chainZ, this.getX(), this.getY(), this.getZ(), yawRad, this.tickCount);
                System.arraycopy(chainX, 0, prevChainX, 0, SEGS);
                System.arraycopy(chainY, 0, prevChainY, 0, SEGS);
                System.arraycopy(chainZ, 0, prevChainZ, 0, SEGS);
            } else {
                for (int i = 0; i < SEGS; ++i) {
                    double d = i * STEP;
                    double px = this.getX() - rawFx * d;
                    double py = this.getY() - rawFy * d;
                    double pz = this.getZ() - rawFz * d;
                    chainX[i] = prevChainX[i] = px;
                    chainY[i] = prevChainY[i] = py;
                    chainZ[i] = prevChainZ[i] = pz;
                }
            }
            return;
        }

        System.arraycopy(chainX, 0, prevChainX, 0, SEGS);
        System.arraycopy(chainY, 0, prevChainY, 0, SEGS);
        System.arraycopy(chainZ, 0, prevChainZ, 0, SEGS);
        System.arraycopy(chainRoll, 0, prevChainRoll, 0, SEGS);
        prevHeadFx = headFx;
        prevHeadFy = headFy;
        prevHeadFz = headFz;
        bankRollPrev = bankRoll;
        jawAnimPrev = jawAnim;
        diveBlendPrev = diveBlend;
        coilBlendPrev = coilBlend;

        diveBlend = Mth.lerp(0.18F, diveBlend, isDiving() ? 1.0F : 0.0F);
        coilBlend = Mth.lerp(0.10F, coilBlend, isCoiled() ? 1.0F : 0.0F);

        float phase = wavePhase();
        float t = this.tickCount;

        // Режим сна кольцом (на Перевёрнутой башне или в режиме питомца PET_SLEEP):
        // плавно сворачиваем все 72 сегмента в величественную спираль радиуса 2.7..5.8 м!
        if (isCoiled() && coilBlend > 0.15F) {
            double[] goalX = new double[SEGS];
            double[] goalY = new double[SEGS];
            double[] goalZ = new double[SEGS];
            fillCoiledSpiral(goalX, goalY, goalZ, this.getX(), this.getY(), this.getZ(), yawRad, t);
            for (int i = 0; i < SEGS; ++i) {
                chainX[i] = Mth.lerp(0.16, chainX[i], goalX[i]);
                chainY[i] = Mth.lerp(0.16, chainY[i], goalY[i]);
                chainZ[i] = Mth.lerp(0.16, chainZ[i], goalZ[i]);
                chainRoll[i] = Mth.lerp(0.16F, chainRoll[i], 0.0F);
            }
            Vec3 coilTangent = new Vec3(goalX[0] - goalX[1], (goalY[0] - goalY[1]) + 0.06, goalZ[0] - goalZ[1]);
            if (coilTangent.lengthSqr() > 1.0E-5) {
                coilTangent = coilTangent.normalize();
                headFx = Mth.lerp(0.16, headFx, coilTangent.x);
                headFy = Mth.lerp(0.16, headFy, coilTangent.y);
                headFz = Mth.lerp(0.16, headFz, coilTangent.z);
            }
            bankRoll = Mth.lerp(0.15F, bankRoll, 0.0F);
            // Тихое спокойное дыхание во сне
            jawAnim = Mth.lerp(0.12F, jawAnim, 0.025F + 0.02F * Mth.sin(t * 0.04F + phase));
            return;
        }

        double vx = this.getX() - this.xo;
        double vy = this.getY() - this.yo;
        double vz = this.getZ() - this.zo;
        double vLen = Math.sqrt(vx * vx + vy * vy + vz * vz);

        double desFx = rawFx, desFy = rawFy, desFz = rawFz;
        if (vLen > 0.035) {
            double maxVy = isDiving() ? 0.98 : 0.72;
            double mvx = vx / vLen, mvy = Mth.clamp(vy / vLen, -maxVy, maxVy), mvz = vz / vLen;
            desFx = Mth.lerp(0.58, rawFx, mvx);
            desFy = Mth.lerp(0.58, rawFy, mvy);
            desFz = Mth.lerp(0.58, rawFz, mvz);
        }

        float swayAtten = 1.0F - 0.85F * diveBlend;
        double sideHx = desFz, sideHz = -desFx;
        double sideHLen = Math.hypot(sideHx, sideHz);
        if (sideHLen > 1.0E-4) {
            sideHx /= sideHLen;
            sideHz /= sideHLen;
            double headSway = Math.sin(t * 0.042F + phase) * 0.065 * swayAtten;
            desFx += sideHx * headSway;
            desFz += sideHz * headSway;
        }
        desFy += Math.cos(t * 0.034F + phase * 0.7F) * 0.030 * swayAtten;
        double dLen = Math.sqrt(desFx * desFx + desFy * desFy + desFz * desFz);
        if (dLen > 1.0E-5) {
            desFx /= dLen;
            desFy /= dLen;
            desFz /= dLen;
        }

        double headLerp = this.isVehicle() ? 0.30 : 0.16;
        headFx = Mth.lerp(headLerp, headFx, desFx);
        headFy = Mth.lerp(headLerp, headFy, desFy);
        headFz = Mth.lerp(headLerp, headFz, desFz);
        double hLen = Math.sqrt(headFx * headFx + headFy * headFy + headFz * headFz);
        if (hLen > 1.0E-6) {
            headFx /= hLen;
            headFy /= hLen;
            headFz /= hLen;
        } else {
            headFx = 0.0;
            headFy = 0.0;
            headFz = 1.0;
        }

        float smoothYaw = Mth.rotLerp(this.isVehicle() ? 0.24F : 0.12F, prevSmoothYaw, curYaw);
        float yawRate = Mth.wrapDegrees(smoothYaw - prevSmoothYaw);
        prevSmoothYaw = smoothYaw;
        float targetRoll = Mth.clamp(-yawRate * (this.isVehicle() ? 6.5F : 11.0F), -28.0F, 28.0F);
        bankRoll = Mth.lerp(0.12F, bankRoll, targetRoll);

        // В пикировании дракон плотно закрывает пасть!
        float normalJaw = 0.055F + 0.055F * Mth.sin(t * 0.058F + phase)
                + (Mth.sin(t * 0.019F + phase * 1.9F) > 0.88F ? 0.15F : 0.0F);
        float targetJaw = Mth.lerp(diveBlend, normalJaw, 0.010F);
        jawAnim = Mth.lerp(0.16F, jawAnim, targetJaw);

        chainX[0] = this.getX();
        chainY[0] = this.getY();
        chainZ[0] = this.getZ();
        chainRoll[0] = bankRoll;

        double refBx = -headFx;
        double refBy = -headFy;
        double refBz = -headFz;

        for (int i = 1; i < SEGS; ++i) {
            float u = (float) i / (float) (SEGS - 1);
            float env = Mth.clamp(u * 2.2F, 0.0F, 1.0F) * (0.35F + 0.65F * u) * swayAtten;

            double latSX = -refBz, latSZ = refBx;
            double latSL = Math.hypot(latSX, latSZ);
            if (latSL > 1.0E-4) {
                latSX /= latSL;
                latSZ /= latSL;
            } else {
                latSX = 1.0;
                latSZ = 0.0;
            }
            double wavePushLat  = Math.sin(t * 0.068F - i * 0.16F + phase) * 0.022 * env;
            double wavePushVert = Math.cos(t * 0.052F - i * 0.13F + phase * 0.8F) * 0.011 * env;

            double dx = (chainX[i] + latSX * wavePushLat) - chainX[i - 1];
            double dy = (chainY[i] + wavePushVert)        - chainY[i - 1];
            double dz = (chainZ[i] + latSZ * wavePushLat) - chainZ[i - 1];
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist < 1.0E-5) {
                dx = refBx;
                dy = refBy;
                dz = refBz;
            } else {
                dx /= dist;
                dy /= dist;
                dz /= dist;
            }

            double dot = dx * refBx + dy * refBy + dz * refBz;
            if (dot < MAX_JOINT_COS) {
                double px = dx - dot * refBx;
                double py = dy - dot * refBy;
                double pz = dz - dot * refBz;
                double pLen = Math.sqrt(px * px + py * py + pz * pz);
                if (pLen > 1.0E-6) {
                    px /= pLen;
                    py /= pLen;
                    pz /= pLen;
                    dx = refBx * MAX_JOINT_COS + px * MAX_JOINT_SIN;
                    dy = refBy * MAX_JOINT_COS + py * MAX_JOINT_SIN;
                    dz = refBz * MAX_JOINT_COS + pz * MAX_JOINT_SIN;
                } else {
                    dx = refBx;
                    dy = refBy;
                    dz = refBz;
                }
            }

            chainX[i] = chainX[i - 1] + dx * STEP;
            chainY[i] = chainY[i - 1] + dy * STEP;
            chainZ[i] = chainZ[i - 1] + dz * STEP;
            chainRoll[i] = Mth.lerp(0.18F, chainRoll[i], chainRoll[i - 1] * 0.965F
                    + Mth.sin(t * 0.068F - i * 0.16F + phase) * 1.1F * env);

            refBx = dx;
            refBy = dy;
            refBz = dz;
        }
    }

    /** Генерирует ровную 1.45-витковую спираль сна (длина каждого звена ровно STEP = 0.54 м!) на площадке башни. */
    private static void fillCoiledSpiral(double[] outX, double[] outY, double[] outZ,
                                         double anchorX, double anchorY, double anchorZ,
                                         float baseAng, float tickTime) {
        double r0 = 2.65;
        double cx = anchorX - Math.cos(baseAng) * r0;
        double cz = anchorZ - Math.sin(baseAng) * r0;
        double theta = baseAng;
        for (int i = 0; i < SEGS; ++i) {
            double u = (double) i / (SEGS - 1);
            double r = Mth.lerp(u, 2.65, 5.75);
            double breathLift = 0.05 * Math.sin(tickTime * 0.05 - i * 0.12) * (1.0 - 0.5 * u);
            double headChin = i < 8 ? (8 - i) * 0.045 : 0.0;
            outX[i] = cx + Math.cos(theta) * r;
            outY[i] = anchorY + headChin + breathLift;
            outZ[i] = cz + Math.sin(theta) * r;
            theta += STEP / r;
        }
    }

    // =========================================================================
    // SERVER: MOUNTED FLIGHT, PET AI, AND WILD INVERTED-TOWER-ONLY ROOSTING
    // =========================================================================

    private void serverFlightTick() {
        // A) Управляемый полёт верхом (0 .. 2x Элитр с фейерверками, мышь + W/S + A/D, пикирование, предел тяги!)
        LivingEntity rider = getControllingPassenger();
        if (rider instanceof Player player) {
            tickMountedFlight(player);
            return;
        }
        this.entityData.set(DATA_DIVING, false);
        if (upwardThrust() < 1.0F) {
            this.entityData.set(DATA_THRUST, Math.min(1.0F, upwardThrust() + 0.008F));
        }

        // B) Приручённый дракон без всадника (питомец: сон кольцом ИЛИ сопровождение хозяина / призыв Горном!)
        if (isTamed()) {
            tickTamedPetFlight();
            return;
        }

        // C) Дикий неприручённый дракон: садится отдыхать ТОЛЬКО на Летающие Перевёрнутые Башни!
        tickWildDragonFlight();
    }

    private void tickMountedFlight(Player rider) {
        this.setCoiled(false);
        boolean customSaddle = saddleTier() >= 2;
        float turnAgility = customSaddle ? 1.55F : 1.0F;

        float wantYaw = riderInputGrace > 0 ? riderAimYaw : rider.getYRot();
        float wantPitch = riderInputGrace > 0 ? riderAimPitch : rider.getXRot();
        if (riderInputGrace > 0) --riderInputGrace;

        // Точный доворот курса A/D по градусам + плавное наведение мышью (как на элитрах)
        float maxYawStep = 5.2F * turnAgility;
        float maxPitchStep = 4.4F * turnAgility;
        float dYaw = Mth.clamp(Mth.wrapDegrees(wantYaw - this.getYRot()), -maxYawStep, maxYawStep);
        float nextYaw = this.getYRot() + dYaw;

        float thrust = upwardThrust();
        // Если тяга подъёма иссякла (thrust <= 0.001), нос плавно опускается — взлетать выше нельзя!
        if (thrust <= 0.005F && wantPitch < 8.0F) {
            wantPitch = Math.max(wantPitch, 9.0F);
        }
        float dPitch = Mth.clamp(wantPitch - this.getXRot(), -maxPitchStep, maxPitchStep);
        float nextPitch = Mth.clamp(this.getXRot() + dPitch, -86.0F, 88.0F);

        // Режим пикирования (полёт круто вниз: закрытая пасть, поджатые лапы, стремительный набор скорости!)
        boolean divingNow = nextPitch >= 48.0F && mountedSpeed >= 0.20;
        this.entityData.set(DATA_DIVING, divingNow);

        // Управление скоростью W / S: от полной остановки (0.0) до 2x скорости элитр с фейерверками (3.35 бл/тик)!
        double accel = customSaddle ? 0.085 : 0.062;
        if (riderThrottle > 0) {
            mountedSpeed = Math.min(MAX_MOUNTED_SPEED, mountedSpeed + accel);
        } else if (riderThrottle < 0) {
            mountedSpeed = Math.max(MIN_MOUNTED_SPEED, mountedSpeed - accel * 1.35);
        } else if (divingNow) {
            double diveBoost = 0.055 * Mth.sin(nextPitch * Mth.DEG_TO_RAD);
            mountedSpeed = Math.min(MAX_MOUNTED_SPEED, mountedSpeed + diveBoost);
        } else {
            // Мягкое аэродинамическое поддержание крейсерского парения (с сохранением 0 при полной остановке!)
            if (mountedSpeed < 0.02) mountedSpeed = 0.0;
            else mountedSpeed *= 0.996;
        }

        // Расход и восстановление тяги подъёма (вверх тяга постепенно заканчивается!)
        if (nextPitch < -8.0F && mountedSpeed > 0.06) {
            float climbFactor = (-nextPitch / 86.0F) * (0.45F + 0.55F * (float) (mountedSpeed / MAX_MOUNTED_SPEED));
            float drain = climbFactor * (customSaddle ? 0.0048F : 0.0072F);
            thrust = Math.max(0.0F, thrust - drain);
        } else {
            float recov = divingNow ? 0.018F : (nextPitch >= 2.0F ? 0.0085F : 0.0045F);
            if (customSaddle) recov *= 1.25F;
            thrust = Math.min(1.0F, thrust + recov);
        }
        this.entityData.set(DATA_THRUST, thrust);
        this.entityData.set(DATA_SPEED_RATIO, (float) (mountedSpeed / MAX_MOUNTED_SPEED));

        this.setYRot(nextYaw);
        this.yBodyRot = nextYaw;
        this.yHeadRot = nextYaw;
        this.setXRot(nextPitch);

        if (mountedSpeed <= 0.001) {
            this.setDeltaMovement(Vec3.ZERO);
        } else {
            Vec3 forward = Vec3.directionFromRotation(nextPitch, nextYaw);
            Vec3 vel = forward.scale(mountedSpeed);
            int maxSkyY = this.level().getMaxBuildHeight() - 18;
            if (this.getY() > maxSkyY && vel.y > 0.0) {
                vel = new Vec3(vel.x, 0.0, vel.z);
            }
            this.setDeltaMovement(vel);
            this.move(MoverType.SELF, vel);
        }

        if (divingNow && this.tickCount % 2 == 0 && this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY(), this.getZ(), 2, 0.6, 0.6, 0.6, 0.02);
        }
    }

    private void tickTamedPetFlight() {
        this.entityData.set(DATA_SPEED_RATIO, 0.0F);
        UUID ownerId = getOwnerUUID();
        Player owner = (ownerId != null && this.level() instanceof ServerLevel sl) ? sl.getPlayerByUUID(ownerId) : null;

        if (hornApproachTicks > 0) {
            --hornApproachTicks;
            this.setCoiled(false);
            if (owner != null) {
                targetPoint = owner.position().add(0.0, 5.0, 0.0);
                if (this.distanceToSqr(owner) < 16.0 * 16.0) {
                    hornApproachTicks = 0;
                }
            }
            steerSmoothlyTowardTarget(BASE_SPEED * 1.65, 2.6F, 2.2F);
            return;
        }

        if (petMode() == PET_SLEEP) {
            this.setCoiled(true);
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }

        // PET_ORBIT_OWNER: плавный грациозный полёт кругами рядом с хозяином
        this.setCoiled(false);
        if (owner != null) {
            orbitCenterX = owner.getX();
            orbitCenterZ = owner.getZ();
            orbitAltY = Mth.clamp(owner.getY() + 18.0, this.level().getMinBuildHeight() + 24.0, this.level().getMaxBuildHeight() - 32.0);
            if (targetPoint == null || --waypointTimer <= 0 || this.position().distanceToSqr(targetPoint) < 100.0) {
                double curAng = Math.atan2(this.getZ() - orbitCenterZ, this.getX() - orbitCenterX);
                orbitAngle = curAng + orbitSign * 0.65;
                double rad = 26.0 + (this.random.nextDouble() * 10.0);
                targetPoint = new Vec3(
                        orbitCenterX + Math.cos(orbitAngle) * rad,
                        orbitAltY + (this.random.nextDouble() - 0.5) * 6.0,
                        orbitCenterZ + Math.sin(orbitAngle) * rad);
                waypointTimer = 50;
            }
            double distOwner = this.distanceTo(owner);
            double petSpd = distOwner > 65.0 ? BASE_SPEED * 1.85 : BASE_SPEED * 0.95;
            steerSmoothlyTowardTarget(petSpd, 2.1F, 1.5F);
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    private void tickWildDragonFlight() {
        this.entityData.set(DATA_SPEED_RATIO, 0.0F);
        boolean isElisar = this.level().dimension().equals(Flight.ELISAR_DIM);
        CityPlan plan = isElisar ? CityPlan.get() : null;
        int ocean = plan != null ? plan.ocean : this.level().getSeaLevel();

        // 1) Сон дикого дракона строго на вершине Летающей Перевёрнутой Башни!
        if (isCoiled()) {
            this.setDeltaMovement(Vec3.ZERO);
            // Пока рядом на башне есть игрок (например, крадущийся с бумагой для приручения), дракон спокойно спит!
            Player nearPlayer = this.level().getNearestPlayer(this, 24.0);
            if (nearPlayer != null && !nearPlayer.isSpectator()) {
                wildRoostTimer = Math.max(wildRoostTimer, 200);
                return;
            }
            if (--wildRoostTimer <= 0) {
                this.setCoiled(false);
                this.roostTower = null;
                this.wildNextRoostCooldown = 900 + this.random.nextInt(700);
                this.flightMode = FLIGHT_OPEN_SKY_SEA;
                this.waypointTimer = 0;
            }
            return;
        }

        // 2) Периодический поиск ближайшей Дикой Летающей Перевёрнутой Башни для отдыха!
        if (isElisar && --wildNextRoostCooldown <= 0 && flightMode != FLIGHT_SEEK_TOWER_ROOST) {
            InvertedTowers.Site wildTow = InvertedTowers.nearest(this.getX(), this.getZ(), false, 2);
            if (wildTow != null && Math.hypot(wildTow.cx() - this.getX(), wildTow.cz() - this.getZ()) < 340.0) {
                AABB check = new AABB(wildTow.cx() - 14, wildTow.roostY() - 6, wildTow.cz() - 14,
                        wildTow.cx() + 14, wildTow.roostY() + 14, wildTow.cz() + 14);
                boolean occupied = !this.level().getEntitiesOfClass(PaperDragonEntity.class, check, d -> d != this && d.isCoiled()).isEmpty();
                if (!occupied) {
                    this.roostTower = wildTow;
                    this.flightMode = FLIGHT_SEEK_TOWER_ROOST;
                    this.targetPoint = new Vec3(wildTow.cx() + 0.5, wildTow.roostY() + 2.2, wildTow.cz() + 0.5);
                    this.waypointTimer = 420;
                }
            }
            wildNextRoostCooldown = 500 + this.random.nextInt(400);
        }

        if (flightMode == FLIGHT_SEEK_TOWER_ROOST && roostTower != null) {
            targetPoint = new Vec3(roostTower.cx() + 0.5, roostTower.roostY() + 1.2, roostTower.cz() + 0.5);
            double dRoost = this.position().distanceTo(targetPoint);
            if (dRoost < 5.2) {
                placeCoiledOnWildTower(roostTower);
                return;
            }
            if (--waypointTimer <= 0) {
                flightMode = FLIGHT_OPEN_SKY_SEA;
                roostTower = null;
            } else {
                steerSmoothlyTowardTarget(BASE_SPEED * 0.90, 2.0F, 1.5F);
                return;
            }
        }

        // 3) Строгий запрет залёта диких драконов в Белый Город
        if (isElisar && isInsideCityNoFlyZone(plan, this.getX(), this.getZ(), 15.0)) {
            double rx = this.getX(), rz = this.getZ();
            double rLen = Math.hypot(rx, rz);
            if (rLen < 1.0) { rx = 1.0; rz = 0.0; rLen = 1.0; }
            rx /= rLen; rz /= rLen;
            targetPoint = new Vec3(this.getX() + rx * 95.0, Math.max(this.getY(), ocean + 44.0), this.getZ() + rz * 95.0);
            waypointTimer = 40;
            flightMode = FLIGHT_OPEN_SKY_SEA;
        } else if (targetPoint == null || --waypointTimer <= 0 || this.position().distanceToSqr(targetPoint) < 196.0) {
            advanceSmoothFlightTarget(plan, ocean, isElisar);
        }

        Vec3 toTarget = targetPoint.subtract(this.position());
        double dist = toTarget.length();
        Vec3 rawDir = dist > 0.01 ? toTarget.scale(1.0 / dist) : Vec3.directionFromRotation(this.getXRot(), this.getYRot());

        float phase = wavePhase();
        float slowT = this.tickCount * 0.028F + phase;
        Vec3 right = new Vec3(-rawDir.z, 0.0, rawDir.x);
        if (right.lengthSqr() > 1.0E-4) right = right.normalize();
        Vec3 desired = rawDir
                .add(right.scale(Math.sin(slowT) * 0.10))
                .add(0.0, Math.cos(slowT * 0.75F) * 0.04, 0.0);

        desired = blendMultiRayClearance(desired, ocean);
        if (desired.lengthSqr() > 1.0E-6) desired = desired.normalize();

        if (smoothAimDir == null || smoothAimDir.lengthSqr() < 1.0E-4) {
            smoothAimDir = desired;
        } else {
            smoothAimDir = smoothAimDir.scale(0.945).add(desired.scale(0.055));
            if (smoothAimDir.lengthSqr() > 1.0E-6) smoothAimDir = smoothAimDir.normalize();
        }

        double horiz = Math.hypot(smoothAimDir.x, smoothAimDir.z);
        float wantYaw = (float) (Mth.atan2(smoothAimDir.z, smoothAimDir.x) * (180.0 / Math.PI)) - 90.0F;
        float wantPitch = Mth.clamp((float) -(Mth.atan2(smoothAimDir.y, Math.max(0.05, horiz)) * (180.0 / Math.PI)), -26.0F, 26.0F);

        float dYaw = Mth.clamp(Mth.wrapDegrees(wantYaw - this.getYRot()), -MAX_YAW_STEP_DEG, MAX_YAW_STEP_DEG);
        float dPitch = Mth.clamp(wantPitch - this.getXRot(), -MAX_PITCH_STEP_DEG, MAX_PITCH_STEP_DEG);
        float nextYaw = this.getYRot() + dYaw;
        float nextPitch = Mth.clamp(this.getXRot() + dPitch, -26.0F, 26.0F);
        this.setYRot(nextYaw);
        this.yBodyRot = nextYaw;
        this.yHeadRot = nextYaw;
        this.setXRot(nextPitch);

        Vec3 forward = Vec3.directionFromRotation(nextPitch, nextYaw);
        double targetSpeed = BASE_SPEED * (1.0 - 0.10 * (Math.abs(dYaw) / MAX_YAW_STEP_DEG));
        Vec3 prevVel = this.getDeltaMovement();
        Vec3 nextVel = prevVel.scale(0.93).add(forward.scale(targetSpeed * 0.07));
        if (nextVel.length() < BASE_SPEED * 0.72) {
            nextVel = forward.scale(BASE_SPEED * 0.72);
        }

        if (this.getY() < ocean + 11.0 && nextVel.y < 0.05) {
            nextVel = new Vec3(nextVel.x, Mth.lerp(0.10, nextVel.y, 0.09), nextVel.z);
        }
        int maxSkyY = this.level().getMaxBuildHeight() - 30;
        if (this.getY() > maxSkyY && nextVel.y > -0.04) {
            nextVel = new Vec3(nextVel.x, Mth.lerp(0.10, nextVel.y, -0.08), nextVel.z);
        }

        this.setDeltaMovement(nextVel);
        this.move(MoverType.SELF, nextVel);

        if (this.horizontalCollision || this.verticalCollision) {
            orbitSign = -orbitSign;
            orbitAngle += orbitSign * 0.45;
        }
    }

    private void steerSmoothlyTowardTarget(double speed, float yawLimit, float pitchLimit) {
        if (targetPoint == null) return;
        Vec3 to = targetPoint.subtract(this.position());
        double d = to.length();
        Vec3 dir = d > 0.01 ? to.scale(1.0 / d) : Vec3.directionFromRotation(this.getXRot(), this.getYRot());
        double horiz = Math.hypot(dir.x, dir.z);
        float wantYaw = (float) (Mth.atan2(dir.z, dir.x) * (180.0 / Math.PI)) - 90.0F;
        float wantPitch = Mth.clamp((float) -(Mth.atan2(dir.y, Math.max(0.05, horiz)) * (180.0 / Math.PI)), -32.0F, 32.0F);
        float nextYaw = this.getYRot() + Mth.clamp(Mth.wrapDegrees(wantYaw - this.getYRot()), -yawLimit, yawLimit);
        float nextPitch = Mth.clamp(this.getXRot() + Mth.clamp(wantPitch - this.getXRot(), -pitchLimit, pitchLimit), -32.0F, 32.0F);
        this.setYRot(nextYaw);
        this.yBodyRot = nextYaw;
        this.yHeadRot = nextYaw;
        this.setXRot(nextPitch);
        Vec3 vel = Vec3.directionFromRotation(nextPitch, nextYaw).scale(speed);
        this.setDeltaMovement(vel);
        this.move(MoverType.SELF, vel);
    }

    private void advanceSmoothFlightTarget(@Nullable CityPlan plan, int ocean, boolean isElisar) {
        CityPlan.Island wildIsl = isElisar ? findWildFloatingIsland(plan, this.getX(), this.getZ()) : null;

        if (wildIsl != null) {
            double vaultBottom = wildIsl.base - wildIsl.depth;
            boolean tallUnderVault = (vaultBottom - ocean) >= 30.0;

            if (this.random.nextFloat() < 0.14F) {
                float roll = this.random.nextFloat();
                if (tallUnderVault && roll < 0.30F) {
                    flightMode = FLIGHT_UNDER_ISLAND;
                } else if (roll < 0.82F) {
                    flightMode = FLIGHT_ALONG_ISLAND_RIM;
                } else {
                    flightMode = FLIGHT_OPEN_SKY_SEA;
                }
            }
            if (flightMode == FLIGHT_UNDER_ISLAND && !tallUnderVault) {
                flightMode = FLIGHT_ALONG_ISLAND_RIM;
            }

            orbitCenterX = wildIsl.cx;
            orbitCenterZ = wildIsl.cz;
            double curAng = Math.atan2(this.getZ() - orbitCenterZ, this.getX() - orbitCenterX);
            orbitAngle = curAng + orbitSign * (0.56 + this.random.nextDouble() * 0.28);

            if (flightMode == FLIGHT_UNDER_ISLAND) {
                orbitRadius = wildIsl.R * (0.45 + this.random.nextDouble() * 0.45);
                orbitAltY = Mth.lerp(0.46 + this.random.nextDouble() * 0.20, ocean + 12.0, vaultBottom - 11.0);
            } else if (flightMode == FLIGHT_ALONG_ISLAND_RIM) {
                orbitRadius = wildIsl.maxReach() + 28.0 + this.random.nextDouble() * 24.0;
                orbitAltY = wildIsl.base - 4.0 + this.random.nextDouble() * 24.0;
            } else {
                orbitRadius = wildIsl.maxReach() + 45.0 + this.random.nextDouble() * 45.0;
                orbitAltY = wildIsl.base + 38.0 + this.random.nextDouble() * 24.0;
            }

            double wx = orbitCenterX + Math.cos(orbitAngle) * orbitRadius;
            double wz = orbitCenterZ + Math.sin(orbitAngle) * orbitRadius;
            if (isElisar && isInsideCityNoFlyZone(plan, wx, wz, 25.0)) {
                orbitSign = -orbitSign;
                orbitAngle = curAng + orbitSign * 0.85;
                wx = orbitCenterX + Math.cos(orbitAngle) * orbitRadius;
                wz = orbitCenterZ + Math.sin(orbitAngle) * orbitRadius;
            }
            double wy = Mth.clamp(orbitAltY, ocean + 13.0, this.level().getMaxBuildHeight() - 34.0);
            targetPoint = new Vec3(wx, wy, wz);
            waypointTimer = 140 + this.random.nextInt(90);
            return;
        }

        float curYawRad = this.getYRot() * Mth.DEG_TO_RAD;
        double baseAng = Math.atan2(Mth.cos(curYawRad), -Mth.sin(curYawRad));
        double nextAng = baseAng + (this.random.nextDouble() - 0.5) * 0.85;
        double dist = 70.0 + this.random.nextDouble() * 55.0;
        double wx = this.getX() + Math.cos(nextAng) * dist;
        double wz = this.getZ() + Math.sin(nextAng) * dist;

        if (isElisar && isInsideCityNoFlyZone(plan, wx, wz, 25.0)) {
            double outAng = Math.atan2(this.getZ(), this.getX());
            wx = this.getX() + Math.cos(outAng) * 90.0;
            wz = this.getZ() + Math.sin(outAng) * 90.0;
        }

        int ix = Mth.floor(wx), iz = Mth.floor(wz);
        int ground = this.level().hasChunk(ix >> 4, iz >> 4)
                ? this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz)
                : ocean;
        double wy = Math.max(ground, ocean) + 34.0 + this.random.nextDouble() * 28.0;
        wy = Mth.clamp(wy, ocean + 16.0, this.level().getMaxBuildHeight() - 34.0);
        targetPoint = new Vec3(wx, wy, wz);
        waypointTimer = 150 + this.random.nextInt(90);
    }

    private Vec3 blendMultiRayClearance(Vec3 desired, int ocean) {
        Vec3 start = this.position();
        Vec3 d = desired.lengthSqr() > 1.0E-5 ? desired.normalize() : new Vec3(0, 0, 1);
        Vec3 right = new Vec3(-d.z, 0.0, d.x);
        if (right.lengthSqr() > 1.0E-4) right = right.normalize();
        else right = new Vec3(1, 0, 0);

        double pushRight = 0.0;
        double pushUp = 0.0;

        double[] angles = {-0.48, -0.24, 0.0, 0.24, 0.48};
        for (double a : angles) {
            Vec3 rayDir = d.scale(Math.cos(a)).add(right.scale(Math.sin(a))).normalize();
            var hit = this.level().clip(new ClipContext(
                    start, start.add(rayDir.scale(30.0)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
            if (hit.getType() != HitResult.Type.MISS) {
                double frac = 1.0 - Math.min(1.0, Math.sqrt(hit.getLocation().distanceToSqr(start)) / 30.0);
                if (a < -0.05) pushRight += frac * 0.32;
                else if (a > 0.05) pushRight -= frac * 0.32;
                else {
                    pushRight += orbitSign * frac * 0.38;
                    pushUp += (flightMode == FLIGHT_UNDER_ISLAND ? -0.16 : 0.28) * frac;
                }
            }
        }

        if (flightMode != FLIGHT_UNDER_ISLAND) {
            int gx = this.getBlockX(), gz = this.getBlockZ();
            if (this.level().hasChunk(gx >> 4, gz >> 4)) {
                int topY = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, gx, gz);
                if (topY > ocean + 4 && this.getY() < topY + 22.0 && this.getY() > topY - 4.0) {
                    pushUp += Mth.clamp((topY + 24.0 - this.getY()) * 0.025, 0.0, 0.35);
                }
            }
        } else {
            var ceilHit = this.level().clip(new ClipContext(
                    start, start.add(0.0, 13.0, 0.0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (ceilHit.getType() != HitResult.Type.MISS) {
                pushUp -= 0.24;
            }
        }

        return d.add(right.scale(Mth.clamp(pushRight, -0.55, 0.55)))
                .add(0.0, Mth.clamp(pushUp, -0.35, 0.40), 0.0);
    }

    public void setHomeAnchor(Vec3 anchor, int mode) {
        this.orbitCenterX = anchor.x;
        this.orbitAltY = anchor.y;
        this.orbitCenterZ = anchor.z;
        this.flightMode = mode;
        this.orbitAngle = Math.atan2(this.getZ() - anchor.z, this.getX() - anchor.x);
        this.orbitRadius = Math.max(40.0, Math.hypot(this.getX() - anchor.x, this.getZ() - anchor.z));
        this.waypointTimer = 240;
    }

    public static double lowestSolidInColumn(Level level, double x, double z, int yBottom, int yTop) {
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        int ix = Mth.floor(x), iz = Mth.floor(z);
        for (int y = yBottom + 1; y <= yTop; ++y) {
            BlockState bs = level.getBlockState(mp.set(ix, y, iz));
            if (!bs.isAir() && bs.getFluidState().isEmpty()) {
                return y;
            }
        }
        return yTop + 1;
    }

    @Nullable
    private static CityPlan.Island findWildFloatingIsland(@Nullable CityPlan plan, double x, double z) {
        CityPlan.Island best = null;
        double bestScore = Double.MAX_VALUE;
        for (CityPlan.Island isl : FloatingIslands.gather(x, z, 280.0, false)) {
            if (isl.kind == 0 || isl.R < 22.0) continue;
            if (isInsideCityNoFlyZone(plan, isl.cx, isl.cz, 55.0)) continue;
            double d = Math.hypot(x - isl.cx, z - isl.cz);
            double score = Math.abs(d - (isl.maxReach() + 36.0)) - isl.R * 0.35;
            if (score < bestScore) {
                bestScore = score;
                best = isl;
            }
        }
        return best;
    }
}
