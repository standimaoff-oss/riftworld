package ru.aiforge.riftworld.entity;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
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
import ru.aiforge.riftworld.world.ElisarLaw;
import ru.aiforge.riftworld.world.ElisarProfession;
import ru.aiforge.riftworld.world.ElisarWitness;

/**
 * v0.48.0: Житель Элисара (референс 05_elisar_villager.png) — мирный горожанин и деревенский ремесленник
 * в сиренево-кремовом кафтане со скрещёнными руками. Торгует за дукаты Элисара и при нападении зовёт Стражей и Лучников.
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = ru.aiforge.riftworld.RiftWorld.MODID)
public final class ElisarVillagerEntity extends PathfinderMob implements Npc, Merchant {
    @Nullable
    private Player tradingPlayer;
    @Nullable
    private MerchantOffers offers;
    /** Профессия жителя — определяет внешний вид, товары и рабочее место. */
    private ElisarProfession profession;
    /** Тик последнего предупреждения (чтобы не кричать каждый тик). */
    private int lastWarnTick = -100;

    public ElisarVillagerEntity(EntityType<? extends ElisarVillagerEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        if (this.getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
        }
        // Профессия выбирается устойчиво: одна и та же «душа» всегда одна и та же.
        this.profession = ElisarProfession.forSeed(this.getUUID().hashCode() & 0x7FFFFFFF);
    }

    /** Профессия жителя. */
    public ElisarProfession profession() {
        return this.profession;
    }

    /** Задать профессию (и переодеть жителя). */
    public void setProfession(ElisarProfession profession) {
        this.profession = profession;
        this.offers = null;
        this.profession.dress(this);
        this.setCustomName(net.minecraft.network.chat.Component.literal(profession.displayName()));
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
        this.goalSelector.addGoal(6, new WorkGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // Жители помогают городу: при тревоге собираются у места преступления.
        this.goalSelector.addGoal(2, new AlarmGoal(this));
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
        // Закон Элисара: удар по жителю — это предупреждение, а не убийство.
        // Житель кричит, зовёт стражей и отступает, но остаётся жив.
        if (!this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker
                && !(attacker instanceof Player p && p.isCreative())
                && this.tickCount - lastWarnTick > 40) {
            lastWarnTick = this.tickCount;
            if (attacker instanceof Player player) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "\u00a7eЖитель Элисара: «Пощади! Я позову стражей!»"), true);
            }
            alertDefenders(attacker);
            this.hurtTime = 10;
            this.getNavigation().stop();
            // Отступаем от нападавшего, а не бьём в ответ насмерть.
            if (attacker instanceof net.minecraft.world.entity.Mob mob) {
                mob.setTarget(null);
                mob.knockback(0.3F, this.getX() - mob.getX(), this.getZ() - mob.getZ());
            }
            this.knockback(0.25F, this.getX() - attacker.getX(), this.getZ() - attacker.getZ());
            return false;
        }
        boolean res = super.hurt(source, amount);
        if (res && !this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker) {
            if (!(attacker instanceof Player p && p.isCreative())) {
                alertDefenders(attacker);
            }
        }
        return res;
    }

    /**
     * Житель умер. Если это видел кто-то из города, убийца попадает в розыск.
     *
     * <p>В 1.20.1 Forge нет {@code LivingEntity.DeadEntityReport}: смерть приходит
     * событием {@link net.minecraftforge.event.entity.living.LivingDeathEvent},
     * поэтому проверка «видел ли город» живёт здесь, а {@link #onDied}
     * остаётся обычным хуком сущности.
     */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onVillagerDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ElisarVillagerEntity villager)) return;
        if (!(villager.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        Entity killer = event.getSource() == null ? null : event.getSource().getEntity();
        if (killer == null || killer == villager) return;
        int witnesses = ElisarWitness.countWitnesses(level, villager, killer);
        if (witnesses <= 0) return;
        if (killer instanceof net.minecraft.server.level.ServerPlayer player) {
            ElisarLaw law = ElisarLaw.get(level);
            law.reportCrime(level, player, witnesses, null);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "\u00a7cВас увидели. Вы в розыске — стражи города ищут убийцу."), true);
        }
        // Поднимаем тревогу: ближайшие стражи бросаются в погоню.
        var area = villager.getBoundingBox().inflate(40.0, 16.0, 40.0);
        for (ElisarGuardEntity guard : level.getEntitiesOfClass(ElisarGuardEntity.class, area)) {
            guard.setAlarm(true);
            guard.setOnPatrol(false);
        }
    }

    public void alertDefenders(LivingEntity attacker) {
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
        // Базовый набор — то, что есть у любого горожанина.
        list.add(new MerchantOffer(
                new ItemStack(ElisarBlocks.SIZO_PLANKS.get(), 12),
                ducat.copyWithCount(1), 16, 2, 0.05F));
        list.add(new MerchantOffer(
                ducat.copyWithCount(2),
                new ItemStack(ModItems.AURITE_INGOT.get(), 1), 24, 3, 0.05F));
        list.add(new MerchantOffer(
                ducat.copyWithCount(3),
                new ItemStack(Items.BREAD, 5), 16, 2, 0.05F));
        // Товары профессии: у каждого ремесленника свой набор.
        int idx = 0;
        for (ItemStack goods : this.profession.goods()) {
            int price = 3 + idx * 3;
            list.add(new MerchantOffer(
                    ducat.copyWithCount(price),
                    goods.copy(), 12 + idx * 2, 4 + idx, 0.08F));
            idx++;
        }
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
        tag.putString("Profession", this.profession.name());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Offers", 10)) {
            this.offers = new MerchantOffers(tag.getCompound("Offers"));
        }
        if (tag.contains("Profession", 8)) {
            try {
                this.profession = ElisarProfession.valueOf(tag.getString("Profession"));
            } catch (IllegalArgumentException ignored) {
                // неизвестная профессия из старого мира — оставляем текущую
            }
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

    /**
     * v0.49.0: «Рабочий день» жителя.
     *
     * <p>Днём житель идёт к своему рабочему месту — мастерской на дороге
     * города — и стоит там, пока не стемнеет. Вечером он возвращается к
     * площади и гуляет. Это делает город живым: у каждого ремесленника
     * есть своё место и свой распорядок.</p>
     */
    public static final class WorkGoal extends Goal {
        private final ElisarVillagerEntity villager;
        @Nullable
        private net.minecraft.core.BlockPos workplace;
        private int idleTicks;

        public WorkGoal(ElisarVillagerEntity villager) {
            this.villager = villager;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.villager.getTarget() != null) {
                return false;
            }
            if (!this.villager.level().isDay()) {
                return false;
            }
            // Житель работает не каждый день, а через день — иначе все стоят
            // на местах одновременно и город выглядит застывшим.
            if (this.villager.tickCount % 2400 < 1200) {
                return false;
            }
            return this.workplace == null && this.findWorkplace();
        }

        private boolean findWorkplace() {
            if (!(this.villager.level() instanceof net.minecraft.server.level.ServerLevel level)) {
                return false;
            }
            var plan = ru.aiforge.riftworld.world.CityPlan.get();
            // Ищем ближайшую дорогу — рабочее место ремесленника на дороге.
            var routes = ru.aiforge.riftworld.world.PatrolRoute.routesNear(
                    plan, level, this.villager.getX(), this.villager.getZ(), 40.0, 6);
            if (routes.isEmpty()) {
                return false;
            }
            var route = routes.get(this.villager.tickCount % routes.size());
            if (route.isEmpty()) {
                return false;
            }
            var wp = route.points().get(this.villager.tickCount % route.size());
            this.workplace = net.minecraft.core.BlockPos.containing(wp.x(), wp.y(), wp.z());
            return this.workplace != null;
        }

        @Override
        public void tick() {
            if (this.workplace == null) {
                return;
            }
            double dSq = this.villager.distanceToSqr(
                    this.workplace.getX() + 0.5, this.workplace.getY(), this.workplace.getZ() + 0.5);
            if (dSq > 4.0) {
                this.villager.getNavigation().moveTo(
                        this.workplace.getX() + 0.5, this.workplace.getY(), this.workplace.getZ() + 0.5, 1.0);
                this.idleTicks = 0;
            } else {
                // На месте: смотрим по сторонам, как настоящий мастер.
                this.idleTicks++;
                this.villager.getLookControl().setLookAt(
                        this.workplace.getX() + 0.5 + Math.cos(this.idleTicks * 0.03) * 6.0,
                        this.workplace.getY() + 1.5,
                        this.workplace.getZ() + 0.5 + Math.sin(this.idleTicks * 0.03) * 6.0);
                if (this.idleTicks > 600) {
                    this.workplace = null;
                }
            }
        }

        @Override
        public void stop() {
            this.villager.getNavigation().stop();
            this.workplace = null;
        }
    }

    /**
     * v0.49.0: «Тревога» — при преступлении жители стекаются к месту.
     */
    public static final class AlarmGoal extends Goal {
        private final ElisarVillagerEntity villager;
        @Nullable
        private net.minecraft.core.BlockPos alarmPoint;

        public AlarmGoal(ElisarVillagerEntity villager) {
            this.villager = villager;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.villager.getTarget() != null) {
                return false;
            }
            // Ищем стражей, которые подняли тревогу.
            if (this.alarmPoint == null) {
                var area = this.villager.getBoundingBox().inflate(40.0, 16.0, 40.0);
                for (ElisarGuardEntity guard :
                        this.villager.level().getEntitiesOfClass(ElisarGuardEntity.class, area)) {
                    if (guard.alarm()) {
                        this.alarmPoint = guard.blockPosition();
                        break;
                    }
                }
            }
            return this.alarmPoint != null;
        }

        @Override
        public void tick() {
            if (this.alarmPoint == null) {
                return;
            }
            double dSq = this.villager.distanceToSqr(
                    this.alarmPoint.getX(), this.alarmPoint.getY(), this.alarmPoint.getZ());
            if (dSq > 25.0) {
                this.villager.getNavigation().moveTo(
                        this.alarmPoint.getX() + 0.5, this.alarmPoint.getY(), this.alarmPoint.getZ() + 0.5, 1.05);
            }
        }

        @Override
        public void stop() {
            this.villager.getNavigation().stop();
            this.alarmPoint = null;
        }
    }
}
