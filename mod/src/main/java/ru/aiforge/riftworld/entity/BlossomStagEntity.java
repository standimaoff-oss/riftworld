package ru.aiforge.riftworld.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import ru.aiforge.riftworld.registry.ElisarBlocks;
import ru.aiforge.riftworld.registry.ModEntities;

/**
 * v0.27.0: «Белоснежный цветущий олень» (Snow-White Blossom Stag) — грациозное мирное существо
 * для обычных биомов (поляны, луга и леса Обычного мира и мирные острова Элисара).
 * Утончённый белоснежный силуэт на длинных стройных ногах с тёпло-персиковыми ушками (по фото 2!)
 * и ветвистыми светлыми рогами, на которых распускаются живые 3D-цветы.
 */
public class BlossomStagEntity extends Animal {
    private static final int GRAZE_TOTAL_TICKS = 64;
    private static final EntityDataAccessor<Integer> DATA_GRAZE_TICKS =
            SynchedEntityData.defineId(BlossomStagEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BLOOM_VARIANT =
            SynchedEntityData.defineId(BlossomStagEntity.class, EntityDataSerializers.INT);

    private int prevGrazeTicks = 0;
    private int giftCooldown = 0;

    public BlossomStagEntity(EntityType<? extends BlossomStagEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.1F);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 22.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D);
    }

    public static boolean checkStagSpawnRules(
            EntityType<BlossomStagEntity> type, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (spawnType == MobSpawnType.SPAWN_EGG || spawnType == MobSpawnType.COMMAND) return true;
        if (level.getRawBrightness(pos, 0) < 8) return false;
        BlockState below = level.getBlockState(pos.below());
        return below.is(BlockTags.ANIMALS_SPAWNABLE_ON) || below.is(BlockTags.DIRT)
                || below.is(ElisarBlocks.GRASS.get()) || below.is(ElisarBlocks.BLOSSOM_MOSS.get());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_GRAZE_TICKS, 0);
        this.entityData.define(DATA_BLOOM_VARIANT, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.42D));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.05D));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.15D,
                Ingredient.of(ItemTags.FLOWERS), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.15D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.95D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.5F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public int getBloomVariant() {
        return this.entityData.get(DATA_BLOOM_VARIANT);
    }

    public void setBloomVariant(int v) {
        this.entityData.set(DATA_BLOOM_VARIANT, Mth.clamp(v, 0, 2));
    }

    /**
     * Плавная кривая наклона шеи к траве/цветам (0.0 = гордая осанка, 1.0 = изящный наклон к земле).
     */
    public float getGrazeProgress(float partialTick) {
        float t = Mth.lerp(partialTick, this.prevGrazeTicks, this.entityData.get(DATA_GRAZE_TICKS));
        if (t <= 0.0F) return 0.0F;
        float elapsed = GRAZE_TOTAL_TICKS - t;
        float raw;
        if (elapsed < 14.0F) {
            raw = elapsed / 14.0F;
        } else if (t < 14.0F) {
            raw = t / 14.0F;
        } else {
            raw = 1.0F;
        }
        raw = Mth.clamp(raw, 0.0F, 1.0F);
        return raw * raw * (3.0F - 2.0F * raw);
    }

    public float getGrazeNibbleWave(float partialTick) {
        float t = Mth.lerp(partialTick, this.prevGrazeTicks, this.entityData.get(DATA_GRAZE_TICKS));
        if (t <= 14.0F || t >= GRAZE_TOTAL_TICKS - 14.0F) return 0.0F;
        return Mth.sin(t * 0.65F);
    }

    @Override
    public void tick() {
        this.prevGrazeTicks = this.entityData.get(DATA_GRAZE_TICKS);
        super.tick();

        if (!this.level().isClientSide) {
            if (this.giftCooldown > 0) --this.giftCooldown;
            int gt = this.entityData.get(DATA_GRAZE_TICKS);
            if (gt > 0) {
                this.getNavigation().stop();
                this.entityData.set(DATA_GRAZE_TICKS, gt - 1);
            } else if (this.onGround() && this.getDeltaMovement().horizontalDistanceSqr() < 0.0008D
                    && !this.isInLove() && this.random.nextInt(190) == 0) {
                BlockState below = this.level().getBlockState(this.blockPosition().below());
                if (below.is(BlockTags.DIRT) || below.is(ElisarBlocks.GRASS.get()) || below.is(ElisarBlocks.BLOSSOM_MOSS.get())) {
                    this.entityData.set(DATA_GRAZE_TICKS, GRAZE_TOTAL_TICKS);
                }
            }
        } else if (!this.isBaby() && this.random.nextInt(38) == 0) {
            // Редкая нежная пыльца с цветущих рогов
            this.level().addParticle(ParticleTypes.CHERRY_LEAVES,
                    this.getX() + (this.random.nextDouble() - 0.5D) * 0.55D,
                    this.getY() + 1.85D + this.random.nextDouble() * 0.25D,
                    this.getZ() + (this.random.nextDouble() - 0.5D) * 0.55D,
                    0.0D, -0.01D, 0.0D);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.BONE_MEAL) && !this.isBaby()) {
            if (!this.level().isClientSide) {
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.CHERRY_LEAVES, this.getX(), this.getY() + 1.85D, this.getZ(),
                            14, 0.35D, 0.25D, 0.35D, 0.02D);
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.55D, this.getZ(),
                            8, 0.35D, 0.25D, 0.35D, 0.02D);
                }
                this.playSound(SoundEvents.AZALEA_LEAVES_PLACE, 0.85F, 1.15F);
                if (this.giftCooldown <= 0) {
                    this.spawnAtLocation(new ItemStack(ElisarBlocks.WHITE_LILY_CLUSTER.get()), 1.4F);
                    this.giftCooldown = 200;
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
    public boolean isFood(ItemStack stack) {
        return stack.is(ItemTags.FLOWERS) || stack.is(ElisarBlocks.WHITE_LILY_CLUSTER.get().asItem());
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        BlossomStagEntity fawn = ModEntities.BLOSSOM_STAG.get().create(level);
        if (fawn != null) {
            fawn.setBloomVariant(level.random.nextInt(3));
        }
        return fawn;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
            @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        this.setBloomVariant(level.getRandom().nextInt(3));
        return super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dims) {
        return this.isBaby() ? dims.height * 0.78F : 1.62F;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FOX_AMBIENT;
    }

    @Override
    protected float getSoundVolume() {
        return 0.28F;
    }

    @Override
    public float getVoicePitch() {
        return (this.isBaby() ? 1.55F : 1.25F) + (this.random.nextFloat() - 0.5F) * 0.12F;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BloomVariant", this.getBloomVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("BloomVariant")) {
            this.setBloomVariant(tag.getInt("BloomVariant"));
        }
    }
}
