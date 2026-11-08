package ru.aiforge.riftworld.entity.fauna;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ToolActions;
import ru.aiforge.riftworld.registry.CastleBlocks;
import ru.aiforge.riftworld.registry.ModItems;

/**
 * 02. Базальтовый жук ({@code basalt_beetle}) — бронированный нейтральный обитатель Пепельных земель.
 *
 * <p>Неуязвим к огню и лаве. Обычное оружие наносит ему на 25% меньше урона, кирки — полный 100% урон.
 * Кормление углём или древесным углём разогревает панцирь и даёт базальт / Базальтовый щиток.
 */
public class BasaltBeetleEntity extends PathfinderMob {
    public static final int RAM_ANIM_TICKS = 16;
    private static final EntityDataAccessor<Integer> DATA_RAM_TICKS =
            SynchedEntityData.defineId(BasaltBeetleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MUNCH_TICKS =
            SynchedEntityData.defineId(BasaltBeetleEntity.class, EntityDataSerializers.INT);

    private int prevRamTicks;
    private int feedCooldown = 0;

    public BasaltBeetleEntity(EntityType<? extends BasaltBeetleEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0F);
        this.setPathfindingMalus(BlockPathTypes.LAVA, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0F);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 36.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.55D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_RAM_TICKS, 0);
        this.entityData.define(DATA_MUNCH_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.32D, false));
        this.goalSelector.addGoal(2, new TemptGoal(this, 1.05D, Ingredient.of(Items.COAL, Items.CHARCOAL), false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 7.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    public int getRamTicks() {
        return this.entityData.get(DATA_RAM_TICKS);
    }

    public int getMunchTicks() {
        return this.entityData.get(DATA_MUNCH_TICKS);
    }

    public float getRamProgress(float partialTick) {
        float cur = Mth.lerp(partialTick, this.prevRamTicks, this.getRamTicks());
        return Mth.clamp(Mth.sin((cur / RAM_ANIM_TICKS) * (float) Math.PI), 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        this.prevRamTicks = this.getRamTicks();
        super.tick();
        if (!this.level().isClientSide) {
            if (this.feedCooldown > 0) --this.feedCooldown;
            int ram = this.getRamTicks();
            if (ram > 0) this.entityData.set(DATA_RAM_TICKS, ram - 1);
            int munch = this.getMunchTicks();
            if (munch > 0) this.entityData.set(DATA_MUNCH_TICKS, munch - 1);
        } else if (this.random.nextInt(14) == 0) {
            this.level().addParticle(ParticleTypes.SMOKE,
                    this.getRandomX(0.55D), this.getY() + 0.55D, this.getRandomZ(0.55D),
                    0.0D, 0.015D, 0.0D);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        this.entityData.set(DATA_RAM_TICKS, RAM_ANIM_TICKS);
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            Vec3 push = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D).normalize().scale(0.85D);
            living.push(push.x, 0.28D, push.z);
            this.playSound(SoundEvents.ANVIL_LAND, 0.45F, 0.75F);
        }
        return hit;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypeTags.IS_FIRE)) return true;
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE)) return false;
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living && !source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            ItemStack weapon = living.getMainHandItem();
            boolean isPick = weapon.getItem() instanceof PickaxeItem
                    || weapon.canPerformAction(ToolActions.PICKAXE_DIG);
            if (!isPick) {
                amount *= 0.75F;
                this.playSound(SoundEvents.BASALT_HIT, 0.8F, 0.65F);
            } else {
                this.playSound(SoundEvents.DEEPSLATE_BREAK, 0.8F, 0.9F);
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if ((stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) && this.getTarget() == null && this.feedCooldown <= 0) {
            if (!this.level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                this.heal(6.0F);
                this.feedCooldown = 400;
                this.entityData.set(DATA_MUNCH_TICKS, 30);
                this.playSound(SoundEvents.BLASTFURNACE_FIRE_CRACKLE, 1.0F, 0.9F);
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY() + 0.65D, this.getZ(),
                            6, 0.3D, 0.2D, 0.3D, 0.02D);
                    sl.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.65D, this.getZ(),
                            10, 0.35D, 0.25D, 0.35D, 0.02D);
                }
                if (this.random.nextFloat() < 0.30F) {
                    this.spawnAtLocation(ModItems.BASALT_SCUTE.get());
                } else {
                    this.spawnAtLocation(this.random.nextBoolean()
                            ? new ItemStack(Blocks.SMOOTH_BASALT, 1 + this.random.nextInt(2))
                            : new ItemStack(CastleBlocks.ASHEN_COBBLESTONE.get(), 1 + this.random.nextInt(2)));
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        this.spawnAtLocation(new ItemStack(Blocks.BASALT, 1 + this.random.nextInt(2 + looting)));
        if (this.random.nextFloat() < 0.65F + 0.15F * looting) {
            this.spawnAtLocation(ModItems.BASALT_SCUTE.get());
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BASALT_STEP;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BASALT_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BASALT_BREAK;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FeedCooldown", this.feedCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.feedCooldown = tag.getInt("FeedCooldown");
    }
}
