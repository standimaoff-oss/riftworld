package ru.aiforge.riftworld.entity;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import ru.aiforge.riftworld.registry.ElisarBlocks;
import ru.aiforge.riftworld.registry.ModItems;

/**
 * v0.48.0: Житель Элисара (референс 05_elisar_villager.png) — мирный горожанин и деревенский ремесленник
 * в сиренево-кремовом кафтане со скрещёнными руками. Торгует за дукаты Элисара и при нападении зовёт Стражей и Лучников.
 */
public final class ElisarVillagerEntity extends PathfinderMob implements Npc, Merchant {
    @Nullable
    private Player tradingPlayer;
    @Nullable
    private MerchantOffers offers;

    public ElisarVillagerEntity(EntityType<? extends ElisarVillagerEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        if (this.getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
        }
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 20.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.38D));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Monster.class, 8.0F, 1.15D, 1.30D));
        this.goalSelector.addGoal(3, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.95D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.90D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                  MobSpawnType reason, @Nullable SpawnGroupData spawnData,
                                                  @Nullable CompoundTag dataTag) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        if (reason == MobSpawnType.CHUNK_GENERATION || reason == MobSpawnType.STRUCTURE) {
            this.restrictTo(this.blockPosition(), 28);
        }
        return out;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean res = super.hurt(source, amount);
        if (res && !this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker) {
            if (!(attacker instanceof Player p && p.isCreative())) {
                alertDefenders(attacker);
            }
        }
        return res;
    }

    private void alertDefenders(LivingEntity attacker) {
        AABB area = this.getBoundingBox().inflate(28.0D, 12.0D, 28.0D);
        for (ElisarGuardEntity guard : this.level().getEntitiesOfClass(ElisarGuardEntity.class, area)) {
            if (guard.getTarget() == null) {
                guard.setTarget(attacker);
            }
        }
        for (ElisarArcherEntity archer : this.level().getEntitiesOfClass(ElisarArcherEntity.class, area)) {
            if (archer.getTarget() == null) {
                archer.setTarget(attacker);
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof net.minecraftforge.common.ForgeSpawnEggItem) {
            return super.mobInteract(player, hand);
        }
        if (this.isAlive() && this.tradingPlayer == null && !this.isBaby()) {
            if (!this.level().isClientSide) {
                if (this.getOffers().isEmpty()) {
                    return InteractionResult.CONSUME;
                }
                this.setTradingPlayer(player);
                this.openTradingScreen(player, this.getDisplayName(), 1);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    private MerchantOffers buildDefaultOffers() {
        MerchantOffers list = new MerchantOffers();
        ItemStack ducat = new ItemStack(ModItems.ELISAR_DUCAT.get());
        list.add(new MerchantOffer(
                new ItemStack(ElisarBlocks.SIZO_PLANKS.get(), 12),
                ducat.copyWithCount(1), 16, 2, 0.05F));
        list.add(new MerchantOffer(
                ducat.copyWithCount(2),
                new ItemStack(ModItems.AURITE_INGOT.get(), 1), 24, 3, 0.05F));
        list.add(new MerchantOffer(
                ducat.copyWithCount(3),
                new ItemStack(Items.BREAD, 5), 16, 2, 0.05F));
        list.add(new MerchantOffer(
                ducat.copyWithCount(4),
                new ItemStack(ModItems.BRIDGE_ROPE.get(), 2), 12, 4, 0.05F));
        list.add(new MerchantOffer(
                ducat.copyWithCount(6),
                new ItemStack(ModItems.GILDED_PAPER.get(), 1), 10, 6, 0.05F));
        list.add(new MerchantOffer(
                ducat.copyWithCount(12),
                new ItemStack(ModItems.AURITE_PICKAXE.get(), 1), 6, 10, 0.10F));
        list.add(new MerchantOffer(
                ducat.copyWithCount(14),
                new ItemStack(ModItems.ELISAR_BOW.get(), 1), 6, 12, 0.10F));
        list.add(new MerchantOffer(
                ducat.copyWithCount(14),
                new ItemStack(ModItems.ELISAR_SHIELD.get(), 1), 6, 12, 0.10F));
        return list;
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
    }

    @Override
    public @Nullable Player getTradingPlayer() {
        return this.tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) {
            this.offers = buildDefaultOffers();
        }
        return this.offers;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
        this.offers = offers;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        this.ambientSoundTime = -this.getAmbientSoundInterval();
        this.playSound(SoundEvents.VILLAGER_YES, this.getSoundVolume(), this.getVoicePitch());
        if (!this.level().isClientSide && offer.shouldRewardExp()) {
            int xp = 3 + this.random.nextInt(4);
            this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY() + 0.5D, this.getZ(), xp));
        }
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
        if (!this.level().isClientSide && this.ambientSoundTime > -this.getAmbientSoundInterval() + 20) {
            this.ambientSoundTime = -this.getAmbientSoundInterval();
            this.playSound(stack.isEmpty() ? SoundEvents.VILLAGER_NO : SoundEvents.VILLAGER_YES,
                    this.getSoundVolume(), this.getVoicePitch());
        }
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public boolean isClientSide() {
        return this.level().isClientSide;
    }

    public int getUnhappyCounter() {
        return 0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.offers != null && !this.offers.isEmpty()) {
            tag.put("Offers", this.offers.createTag());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Offers", 10)) {
            this.offers = new MerchantOffers(tag.getCompound("Offers"));
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.tradingPlayer != null ? SoundEvents.VILLAGER_TRADE : SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    private static final class TradeWithPlayerGoal extends Goal {
        private final ElisarVillagerEntity villager;

        TradeWithPlayerGoal(ElisarVillagerEntity villager) {
            this.villager = villager;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!this.villager.isAlive() || this.villager.isInWater() || !this.villager.onGround()) {
                return false;
            }
            Player p = this.villager.getTradingPlayer();
            if (p == null) return false;
            return this.villager.distanceToSqr(p) <= 16.0D && p.containerMenu != null;
        }

        @Override
        public void start() {
            this.villager.getNavigation().stop();
        }

        @Override
        public void stop() {
            this.villager.setTradingPlayer(null);
        }
    }
}
