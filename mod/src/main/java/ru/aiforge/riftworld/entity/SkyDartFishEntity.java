package ru.aiforge.riftworld.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.registry.ElisarBlocks;

/**
 * v0.27.0: «Крылатая рыбка-стрекоза» (Sky-Dart Dragonfly Fish) — дружелюбное воздушное существо
 * для обычных мирных биомов (поляны, луга, леса Обычного мира и мирные острова Элисара).
 * Фарфорово-белое изящное тело с изогнутой шеей, складчатыми селадоновыми плавниками-веерами
 * (по фото 1!) и четырьмя прозрачными переливчатыми крылышками стрекозы.
 */
public class SkyDartFishEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> DATA_HOVER_TICKS =
            SynchedEntityData.defineId(SkyDartFishEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_JOY_TICKS =
            SynchedEntityData.defineId(SkyDartFishEntity.class, EntityDataSerializers.INT);

    private double anchorX, anchorY = Double.NaN, anchorZ;
    private double targetX, targetY, targetZ;
    private int retargetCooldown = 0;
    private float prevRoll = 0.0F;
    private float roll = 0.0F;
    public final float wingPhaseOffset;

    public SkyDartFishEntity(EntityType<? extends SkyDartFishEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noCulling = false;
        this.wingPhaseOffset = (this.getId() * 1.73F) % ((float) Math.PI * 2.0F);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FLYING_SPEED, 0.44D);
    }

    public static boolean checkSkyDartSpawnRules(
            EntityType<SkyDartFishEntity> type, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (spawnType == MobSpawnType.SPAWN_EGG || spawnType == MobSpawnType.COMMAND) return true;
        if (level.getRawBrightness(pos, 0) < 8) return false;
        for (int dy = 1; dy <= 6; ++dy) {
            BlockState below = level.getBlockState(pos.below(dy));
            if (below.is(BlockTags.ANIMALS_SPAWNABLE_ON) || below.is(BlockTags.DIRT)
                    || below.is(ElisarBlocks.GRASS.get()) || below.is(ElisarBlocks.BLOSSOM_MOSS.get())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_HOVER_TICKS, 0);
        this.entityData.define(DATA_JOY_TICKS, 0);
    }

    public boolean isHoveringFlower() {
        return this.entityData.get(DATA_HOVER_TICKS) > 0;
    }

    public int getJoyTicks() {
        return this.entityData.get(DATA_JOY_TICKS);
    }

    public float getRoll(float partialTick) {
        return Mth.lerp(partialTick, this.prevRoll, this.roll);
    }

    @Override
    public void tick() {
        this.setNoGravity(true);
        float oldYaw = this.getYRot();
        super.tick();

        if (!this.level().isClientSide && !this.isNoAi()) {
            if (Double.isNaN(this.anchorY)) {
                this.anchorX = this.getX();
                this.anchorY = this.getY();
                this.anchorZ = this.getZ();
                pickNextFlightTarget();
            }

            int joy = this.entityData.get(DATA_JOY_TICKS);
            if (joy > 0) {
                this.entityData.set(DATA_JOY_TICKS, joy - 1);
            }
            int hover = this.entityData.get(DATA_HOVER_TICKS);
            if (hover > 0) {
                this.entityData.set(DATA_HOVER_TICKS, hover - 1);
            }

            if (--this.retargetCooldown <= 0 || this.position().distanceToSqr(this.targetX, this.targetY, this.targetZ) < 1.35D) {
                if (hover <= 0 && this.random.nextFloat() < 0.28F) {
                    this.entityData.set(DATA_HOVER_TICKS, 26 + this.random.nextInt(26));
                }
                pickNextFlightTarget();
            }

            Vec3 cur = this.getDeltaMovement();
            if (joy > 0) {
                // Игривый спиральный пируэт после угощения цветком!
                double ang = (40 - joy) * 0.36D;
                Vec3 joyVel = new Vec3(Math.cos(ang) * 0.16D, 0.045D * Math.sin(ang * 0.5D), Math.sin(ang) * 0.16D);
                this.setDeltaMovement(cur.scale(0.65D).add(joyVel.scale(0.35D)));
            } else if (this.entityData.get(DATA_HOVER_TICKS) > 0) {
                double bob = Math.sin((this.tickCount + this.getId() * 5) * 0.24D) * 0.018D;
                this.setDeltaMovement(cur.x * 0.72D, bob, cur.z * 0.72D);
            } else {
                Vec3 to = new Vec3(this.targetX - this.getX(), this.targetY - this.getY(), this.targetZ - this.getZ());
                double dist = to.length();
                if (dist > 0.05D) {
                    double desiredSpeed = Mth.clamp(dist * 0.085D, 0.08D, 0.24D);
                    Vec3 desired = to.scale(desiredSpeed / dist);
                    // Плавное огибание земли и воды снизу
                    BlockPos belowPos = BlockPos.containing(this.getX(), this.getY() - 1.2D, this.getZ());
                    BlockState below = this.level().getBlockState(belowPos);
                    if (!below.isAir()) {
                        desired = new Vec3(desired.x, Math.max(desired.y, 0.065D), desired.z);
                    }
                    this.setDeltaMovement(cur.scale(0.84D).add(desired.scale(0.16D)));
                }
            }

            this.move(MoverType.SELF, this.getDeltaMovement());
            Vec3 v = this.getDeltaMovement();
            double horiz = Math.hypot(v.x, v.z);
            if (horiz > 0.012D) {
                float targetYaw = (float) (Mth.atan2(v.z, v.x) * (180.0D / Math.PI)) - 90.0F;
                this.setYRot(Mth.rotLerp(0.22F, this.getYRot(), targetYaw));
                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.getYRot();
            }
            float targetPitch = this.isHoveringFlower()
                    ? -11.0F
                    : (float) Mth.clamp(-Mth.atan2(v.y, Math.max(0.05D, horiz)) * (180.0D / Math.PI), -28.0D, 28.0D);
            this.setXRot(Mth.lerp(0.25F, this.getXRot(), targetPitch));
        } else {
            this.prevRoll = this.roll;
            float yawDelta = Mth.wrapDegrees(this.getYRot() - oldYaw);
            float targetRoll = Mth.clamp(-yawDelta * 2.4F, -26.0F, 26.0F);
            this.roll = Mth.lerp(0.24F, this.roll, targetRoll);
            if (this.getJoyTicks() > 0 && (this.tickCount & 1) == 0) {
                this.level().addParticle(ParticleTypes.END_ROD,
                        this.getX(), this.getY() + 0.2D, this.getZ(),
                        (this.random.nextDouble() - 0.5D) * 0.04D, 0.02D, (this.random.nextDouble() - 0.5D) * 0.04D);
            }
        }
    }

    private void pickNextFlightTarget() {
        this.retargetCooldown = 40 + this.random.nextInt(55);
        // Ищем поблизости цветы, чтобы зависать над ними как стрекоза
        BlockPos origin = BlockPos.containing(this.anchorX, this.anchorY, this.anchorZ);
        for (int tries = 0; tries < 6; ++tries) {
            BlockPos cand = origin.offset(
                    this.random.nextIntBetweenInclusive(-8, 8),
                    this.random.nextIntBetweenInclusive(-3, 3),
                    this.random.nextIntBetweenInclusive(-8, 8));
            BlockState st = this.level().getBlockState(cand);
            if ((st.is(BlockTags.FLOWERS) || st.is(ElisarBlocks.WHITE_LILY_CLUSTER.get())
                    || st.is(ElisarBlocks.FLOWERS.get())) && this.level().isEmptyBlock(cand.above())) {
                this.targetX = cand.getX() + 0.5D;
                this.targetY = cand.getY() + 1.35D + this.random.nextDouble() * 0.8D;
                this.targetZ = cand.getZ() + 0.5D;
                return;
            }
        }
        double ang = this.random.nextDouble() * Math.PI * 2.0D;
        double rad = 2.5D + this.random.nextDouble() * 6.5D;
        double tx = this.anchorX + Math.cos(ang) * rad;
        double tz = this.anchorZ + Math.sin(ang) * rad;
        int groundY = findGroundBelow(tx, this.getY() + 4.0D, tz);
        double minFlightY = groundY > -60 ? groundY + 1.8D : this.anchorY - 1.5D;
        double maxFlightY = groundY > -60 ? groundY + 4.8D : this.anchorY + 2.5D;
        this.targetX = tx;
        this.targetY = Mth.clamp(minFlightY + this.random.nextDouble() * (maxFlightY - minFlightY),
                this.level().getMinBuildHeight() + 4, this.level().getMaxBuildHeight() - 8);
        this.targetZ = tz;
    }

    private int findGroundBelow(double x, double startY, double z) {
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos(Mth.floor(x), Mth.floor(startY), Mth.floor(z));
        for (int i = 0; i < 14; ++i) {
            BlockState st = this.level().getBlockState(mp);
            if (!st.isAir() && (!st.getFluidState().isEmpty() || st.blocksMotion())) {
                return mp.getY();
            }
            mp.move(0, -1, 0);
        }
        return -999;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ItemTags.FLOWERS) || stack.is(Items.GLOW_BERRIES) || stack.is(ElisarBlocks.WHITE_LILY_CLUSTER.get().asItem())) {
            if (!this.level().isClientSide) {
                this.entityData.set(DATA_JOY_TICKS, 40);
                this.heal(4.0F);
                this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 1.45F + this.random.nextFloat() * 0.25F);
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 0.25D, this.getZ(),
                            6, 0.25D, 0.2D, 0.25D, 0.02D);
                }
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean removeWhenFarAway(double distSq) {
        return !this.hasCustomName() && distSq > 96.0D * 96.0D;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!Double.isNaN(this.anchorY)) {
            tag.putDouble("AnchorX", this.anchorX);
            tag.putDouble("AnchorY", this.anchorY);
            tag.putDouble("AnchorZ", this.anchorZ);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AnchorY")) {
            this.anchorX = tag.getDouble("AnchorX");
            this.anchorY = tag.getDouble("AnchorY");
            this.anchorZ = tag.getDouble("AnchorZ");
        }
    }
}
