package ru.aiforge.riftworld.entity.fauna;

import java.util.Optional;
import java.util.UUID;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import ru.aiforge.riftworld.registry.ElisarBlocks;
import ru.aiforge.riftworld.registry.ModParticles;

/**
 * 04. Аметистовая лиса ({@code amethyst_fox}) — пугливое дружелюбное животное Фиолетовых холмов.
 *
 * <p>Кормление ягодами или цветами Элисара формирует доверие. Доверяющая лиса принюхивается
 * и отводит игрока к ближайшей жиле аметиста, монетной руды ({@code ducat_ore}), аурита или рифт-кристалла,
 * подавая хрустальный перезвон и искры.
 */
public class AmethystFoxEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> DATA_TRUSTED =
            SynchedEntityData.defineId(AmethystFoxEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_SNIFF_TICKS =
            SynchedEntityData.defineId(AmethystFoxEntity.class, EntityDataSerializers.INT);

    private int trustPoints = 0;
    @Nullable
    private BlockPos oreTarget = null;
    private int searchCooldown = 0;

    public AmethystFoxEntity(EntityType<? extends AmethystFoxEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0F);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_TRUSTED, Optional.empty());
        this.entityData.define(DATA_SNIFF_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.45D));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 7.5F, 1.15D, 1.38D,
                living -> living instanceof Player p && p.isSprinting() && !isTrusted(p)));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1D, Ingredient.of(
                Items.SWEET_BERRIES, Items.GLOW_BERRIES,
                ElisarBlocks.VIOLET_BLOOM.get(), ElisarBlocks.LILAC_BLOOM.get(), ElisarBlocks.PALE_BLOOM.get()), false));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.95D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    public boolean isTrusted(Player player) {
        Optional<UUID> u = this.entityData.get(DATA_TRUSTED);
        return u.isPresent() && u.get().equals(player.getUUID());
    }

    public boolean hasTrustedPlayer() {
        return this.entityData.get(DATA_TRUSTED).isPresent();
    }

    public int getSniffTicks() {
        return this.entityData.get(DATA_SNIFF_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.getSniffTicks() > 0 && this.tickCount % 4 == 0) {
                this.level().addParticle(ModParticles.VIOLET_LEAF.get(),
                        this.getRandomX(0.45D), this.getY() + 0.55D, this.getRandomZ(0.45D),
                        0.0D, 0.03D, 0.0D);
            }
            return;
        }

        if (this.searchCooldown > 0) --this.searchCooldown;
        int sniff = this.getSniffTicks();
        if (sniff > 0) {
            this.entityData.set(DATA_SNIFF_TICKS, sniff - 1);
            if (this.oreTarget != null) {
                double dSq = this.distanceToSqr(this.oreTarget.getX() + 0.5D, this.getY(), this.oreTarget.getZ() + 0.5D);
                if (dSq > 3.5D) {
                    this.getNavigation().moveTo(this.oreTarget.getX() + 0.5D, this.oreTarget.getY() + 1.0D,
                            this.oreTarget.getZ() + 0.5D, 1.18D);
                } else {
                    this.getNavigation().stop();
                    if (sniff % 20 == 0 && this.level() instanceof ServerLevel sl) {
                        this.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.9F, 1.25F);
                        sl.sendParticles(ParticleTypes.END_ROD,
                                this.oreTarget.getX() + 0.5D, this.oreTarget.getY() + 1.05D, this.oreTarget.getZ() + 0.5D,
                                8, 0.3D, 0.35D, 0.3D, 0.02D);
                    }
                }
            }
        }
    }

    private void startCrystalSniff() {
        this.oreTarget = ElisarFaunaUtil.findNearestBlock(
                this.level(), this.blockPosition(), 16, 10, 16, ElisarFaunaUtil::isValuableCrystalOrOre);
        this.entityData.set(DATA_SNIFF_TICKS, this.oreTarget != null ? 160 : 50);
        this.searchCooldown = 160;
        this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.9F, 1.3F);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ElisarFaunaUtil.isFoxOrLeafwingTreat(stack)) {
            if (!this.level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                this.heal(3.0F);
                ++this.trustPoints;
                if (this.level() instanceof ServerLevel sl) {
                    if (this.trustPoints >= 2) {
                        this.entityData.set(DATA_TRUSTED, Optional.of(player.getUUID()));
                        sl.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 0.8D, this.getZ(),
                                6, 0.3D, 0.25D, 0.3D, 0.02D);
                        if (this.searchCooldown <= 0) {
                            startCrystalSniff();
                        }
                    } else {
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 0.7D, this.getZ(),
                                5, 0.3D, 0.2D, 0.3D, 0.02D);
                    }
                }
                this.playSound(SoundEvents.FOX_EAT, 0.9F, 1.15F);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (stack.isEmpty() && isTrusted(player) && this.searchCooldown <= 0) {
            if (!this.level().isClientSide) {
                startCrystalSniff();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FOX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TrustPoints", this.trustPoints);
        this.entityData.get(DATA_TRUSTED).ifPresent(u -> tag.putUUID("TrustedUUID", u));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.trustPoints = tag.getInt("TrustPoints");
        if (tag.hasUUID("TrustedUUID")) {
            this.entityData.set(DATA_TRUSTED, Optional.of(tag.getUUID("TrustedUUID")));
        }
    }
}
