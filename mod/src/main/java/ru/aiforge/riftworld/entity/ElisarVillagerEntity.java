package ru.aiforge.riftworld.entity;

import javax.annotation.Nullable;

import java.util.List;

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
import ru.aiforge.riftworld.world.ElisarEconomy;
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
    /**
     * v0.54.0 — плавающие множители цен.
     *
     * <p>{@link #buyFactor} — множитель закупочной цены: падает, когда товар
     * много скупают. {@link #sellFactor} — множитель продажной цены: растёт,
     * когда товар много продают. Вместе они удерживают баланс спроса и
     * предложения без ручного вмешательства.</p>
     */
    private float buyFactor = 1.0F;
    private float sellFactor = 1.0F;
    /** Накопительный счётчик сделок по товару — для плавной подстройки цен. */
    private final java.util.Map<String, Integer> tradeCounts = new java.util.HashMap<>();

    public ElisarVillagerEntity(EntityType<? extends ElisarVillagerEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        if (this.getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
        }
        // Профессия выбирается устойчиво: одна и та же «душа» всегда одна и та же.
        this.profession = ElisarProfession.forSeed(this.getUUID().hashCode() & 0x7FFFFFFF);
        // v0.54.0: сразу надеваем профессиональную одежду и даём имя —
        // иначе житель выглядит безымянным и без профессии, пока кто-то
        // не вызовет setProfession() вручную.
        this.applyProfession();
    }

    /** Надеть профессиональную одежду и показать имя профессии. */
    private void applyProfession() {
        this.profession.dress(this);
        this.setCustomName(this.profession.label());
    }

    /** Профессия жителя. */
    public ElisarProfession profession() {
        return this.profession;
    }

    /** Задать профессию (и переодеть жителя). */
    public void setProfession(ElisarProfession profession) {
        this.profession = profession;
        this.offers = null;
        this.applyProfession();
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
        // Внимание: конструктор MerchantOffer(costA, costB, result, maxUses, xp, price)
        // — ШЕСТЬ аргументов. Пятиаргументная форма (costA, result, ...) продаёт
        // ВТОРОЙ предмет, поэтому цену и товар нельзя путать местами.
        //
        // Базовый набор пропускается, если профессия уже продаёт такой же товар:
        // иначе в списке торговли появляются два одинаковых предложения.
        List<ItemStack> professionGoods = this.profession.goods();
        if (!overlaps(new ItemStack(ElisarBlocks.SIZO_PLANKS.get()), professionGoods)) {
            list.add(new MerchantOffer(
                    ducat.copyWithCount(1), ItemStack.EMPTY,
                    new ItemStack(ElisarBlocks.SIZO_PLANKS.get(), 12), 16, 2, 0.05F));
        }
        if (!overlaps(new ItemStack(ModItems.AURITE_INGOT.get()), professionGoods)) {
            list.add(new MerchantOffer(
                    ducat.copyWithCount(2), ItemStack.EMPTY,
                    new ItemStack(ModItems.AURITE_INGOT.get(), 1), 24, 3, 0.05F));
        }
        if (!overlaps(new ItemStack(Items.BREAD), professionGoods)) {
            list.add(new MerchantOffer(
                    ducat.copyWithCount(3), ItemStack.EMPTY,
                    new ItemStack(Items.BREAD, 5), 16, 2, 0.05F));
        }

        // ---------------------------------------------------------------------------------
        // v0.54.0: «живая» торговля — торговец и продаёт, и покупает.
        //
        // ПРОДАЖА (торговец → игрок): цена = базовая цена категории × редкость / 2,
        // умноженная на плавающий множитель sellFactor. Покупатель всегда платит
        // столько же или больше, чем получает дукат — маржа торговца положительна.
        // ---------------------------------------------------------------------------------
        int idx = 0;
        for (ItemStack goods : professionGoods) {
            int base = categoryPrice(goods);
            int rarity = rarityOf(goods);
            int price = ElisarEconomy.apply(this.sellFactor,
                    ElisarEconomy.sellPrice(base, rarity));
            list.add(new MerchantOffer(
                    ducat.copyWithCount(Math.max(1, price)), ItemStack.EMPTY,
                    goods.copy(), 12 + idx * 2, 4 + idx, 0.08F));
            idx++;
        }

        // ---------------------------------------------------------------------------------
        // ПОКУПКА (игрок → торговец): торговец платит дукатами за принесённый товар.
        //
        // ВАЖНО, как это устроено в ванильном MerchantOffer:
        //   costA  — что игрок ОТДАЁт торговцу (его товар);
        //   costB  — пусто;
        //   result — что игрок ПОЛУЧАЕТ (дукаты). Именно result показывается
        //            в слоте результата, и по нему игрок забирает монеты.
        // Если положить дукаты в costB, а result оставить пустым, слот результата
        // будет пустым и сделку невозможно завершить.
        //
        // Цена закупки ВСЕГДА ниже цены продажи (маржа), а товар закупки не должен
        // совпадать с товаром продажи — иначе игрок крутит цикл в ноль или печатает
        // дукаты из воздуха. Проверку делает overlaps() ниже.
        // ---------------------------------------------------------------------------------
        int buyIdx = 0;
        for (ItemStack want : this.profession.buys()) {
            if (overlaps(want, professionGoods)) {
                continue; // не даём торговцу скупать то, что он же и продаёт
            }
            int base = categoryPrice(want);
            int rarity = rarityOf(want);
            int unit = ElisarEconomy.apply(this.buyFactor,
                    ElisarEconomy.buyPrice(base, rarity));
            int count = Math.max(1, want.getCount());
            // Торговец платит за весь стек сразу: цена за штуку × количество.
            int payout = Math.max(1, unit * count);
            list.add(new MerchantOffer(
                    want.copy(), ItemStack.EMPTY,
                    ducat.copyWithCount(payout), 8 + buyIdx * 2, 3 + buyIdx, 0.06F));
            buyIdx++;
        }
        return list;
    }

    /**
     * Есть ли этот предмет среди продаваемых?
     *
     * <p>Страховка от «торговец покупает то, что сам же продаёт»: такой цикл
     * либо обнуляет сделку, либо печатает дукаты из воздуха.</p>
     */
    private static boolean overlaps(ItemStack item, List<ItemStack> sold) {
        for (ItemStack s : sold) {
            if (s.is(item.getItem())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Базовая цена категории товара.
     *
     * <p>Категория определяется по самому предмету, а не по профессии: так
     * цены остаются осмысленными, если игрок приносит нестандартный товар.</p>
     */
    private static int categoryPrice(ItemStack stack) {
        if (stack.is(Items.BREAD) || stack.is(Items.CAKE) || stack.is(Items.HONEY_BOTTLE)
                || stack.is(Items.POTION)) {
            return ElisarEconomy.FOOD_BASE;
        }
        // Оружие и щиты. В 1.20.1 общих Items.SWORD нет — только материалные
        // (IRON_SWORD, DIAMOND_SWORD), поэтому мечи ловим по тегу, а луки
        // и арбалеты — по конкретным предметам: тегов BOWS/CROSSBOWS в 1.20.1 нет.
        if (stack.is(net.minecraft.tags.ItemTags.SWORDS)
                || stack.is(Items.BOW) || stack.is(Items.CROSSBOW)
                || stack.is(ModItems.ELISAR_BOW.get())
                || stack.is(ModItems.ELISAR_SHIELD.get())
                || stack.is(ModItems.ELISAR_GUARD_SHIELD.get())
                || stack.is(ModItems.ELISAR_GUARD_SWORD.get())) {
            return ElisarEconomy.WEAPON_BASE;
        }
        // Инструменты: кирка, топор, лопата, грабли, удочка.
        if (stack.is(net.minecraft.tags.ItemTags.PICKAXES)
                || stack.is(net.minecraft.tags.ItemTags.AXES)
                || stack.is(net.minecraft.tags.ItemTags.SHOVELS)
                || stack.is(net.minecraft.tags.ItemTags.HOES)
                || stack.is(Items.FISHING_ROD)) {
            return ElisarEconomy.TOOL_BASE;
        }
        // Драгоценные — раньше слитков, иначе бриллиант получил бы цену слитка.
        if (stack.is(ModItems.RIFTCRYSTAL.get()) || stack.is(Items.DIAMOND)
                || stack.is(Items.EMERALD)) {
            return ElisarEconomy.RIFTCRYSTAL_BASE;
        }
        if (stack.is(ModItems.AURITE_INGOT.get()) || stack.is(Items.IRON_INGOT)
                || stack.is(Items.GOLD_INGOT)) {
            return ElisarEconomy.AURITE_INGOT_BASE;
        }
        return ElisarEconomy.BLOCK_BASE;
    }

    /** Множитель редкости товара — определяет, во сколько раз дороже базовой цены. */
    private static int rarityOf(ItemStack stack) {
        if (stack.is(ModItems.RIFTCRYSTAL.get()) || stack.is(Items.DIAMOND)
                || stack.is(Items.EMERALD) || stack.is(ModItems.GILDED_PAPER.get())) {
            return ElisarEconomy.RARITY_RARE;
        }
        if (stack.is(ModItems.AURITE_INGOT.get())
                || stack.is(ModItems.ELISAR_DUCAT.get()) || stack.is(Items.IRON_INGOT)
                || stack.is(Items.GOLD_INGOT)) {
            return ElisarEconomy.RARITY_UNCOMMON;
        }
        return ElisarEconomy.RARITY_COMMON;
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

    /** Текущий множитель закупочной цены (падает, когда товар много скупают). */
    public float buyFactor() {
        return this.buyFactor;
    }

    /** Текущий множитель продажной цены (растёт, когда товар много продают). */
    public float sellFactor() {
        return this.sellFactor;
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
        if (!this.level().isClientSide) {
            this.adjustPrices(offer);
            if (offer.shouldRewardExp()) {
                int xp = 3 + this.random.nextInt(4);
                this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY() + 0.5D, this.getZ(), xp));
            }
        }
    }

    /**
     * v0.54.0 — подстройка цен после сделки.
     *
     * <p>Раскладка сделки в ванильном MerchantOffer: {@code costA} — что игрок
     * отдаёт, {@code result} — что игрок получает.</p>
     * <ul>
     *   <li>Игрок <b>продал</b> торговцу товар (result — дукаты): закупочная цена
     *       падает — товара на рынке стало больше.</li>
     *   <li>Игрок <b>купил</b> у торговца (costA — дукаты, result — товар):
     *       продажная цена растёт — предложение истощается.</li>
     * </ul>
     * <p>Обе цены зажаты сверху и снизу, поэтому экономика не может ни рухнуть,
     * ни взорваться.</p>
     */
    private void adjustPrices(MerchantOffer offer) {
        ItemStack costA = offer.getCostA();
        ItemStack result = offer.getResult();
        boolean playerSoldToMerchant = !result.isEmpty()
                && result.is(ModItems.ELISAR_DUCAT.get())
                && !costA.is(ModItems.ELISAR_DUCAT.get());
        boolean playerBoughtFromMerchant = !costA.isEmpty()
                && costA.is(ModItems.ELISAR_DUCAT.get())
                && !result.isEmpty();

        if (playerSoldToMerchant) {
            String key = costA.getItem().getDescriptionId();
            int n = this.tradeCounts.merge(key, 1, Integer::sum);
            this.buyFactor = ElisarEconomy.priceFactor(this.buyFactor, n);
            this.sellFactor = ElisarEconomy.sellFactor(this.sellFactor, n);
        } else if (playerBoughtFromMerchant) {
            String key = result.getItem().getDescriptionId();
            int n = this.tradeCounts.merge(key, 1, Integer::sum);
            this.sellFactor = ElisarEconomy.sellFactor(this.sellFactor, n);
            this.buyFactor = ElisarEconomy.priceFactor(this.buyFactor, n);
        }
        // Пересобрать предложения: цены изменились.
        this.offers = null;
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

    /**
     * v0.55.0: вместо ванильного «да» жителя — звон монет Элисара.
     *
     * <p>Так сделка слышна: игрок понимает, что монеты перешли из рук в
     * руки, ещё не открывая окно торговли.</p>
     */
    @Override
    public SoundEvent getNotifyTradeSound() {
        return ru.aiforge.riftworld.registry.ModSounds.COIN.get();
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
        // v0.54.0: плавающие цены и счётчики сделок переживают перезапуск мира.
        tag.putFloat("BuyFactor", this.buyFactor);
        tag.putFloat("SellFactor", this.sellFactor);
        if (!this.tradeCounts.isEmpty()) {
            CompoundTag counts = new CompoundTag();
            this.tradeCounts.forEach(counts::putInt);
            tag.put("TradeCounts", counts);
        }
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
                // v0.54.0: при загрузке мира профессия восстанавливается
                // вместе с одеждой и именем — иначе житель остаётся «голым».
                this.applyProfession();
            } catch (IllegalArgumentException ignored) {
                // неизвестная профессия из старого мира — оставляем текущую
            }
        }
        if (tag.contains("BuyFactor", 99)) {
            this.buyFactor = tag.getFloat("BuyFactor");
        }
        if (tag.contains("SellFactor", 99)) {
            this.sellFactor = tag.getFloat("SellFactor");
        }
        if (tag.contains("TradeCounts", 10)) {
            CompoundTag counts = tag.getCompound("TradeCounts");
            for (String key : counts.getAllKeys()) {
                this.tradeCounts.put(key, counts.getInt(key));
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
                // v0.55.0: кузнец и оружейник слышат свой молот — мастер работает
                // на месте, и это слышно. Звук редкий, чтобы не надоедать.
                if (this.idleTicks % 45 == 0) {
                    ElisarProfession prof = this.villager.profession();
                    if (prof == ElisarProfession.SMITH || prof == ElisarProfession.ARMOURER) {
                        this.villager.level().playSound(null,
                                this.villager.getX(), this.villager.getY(), this.villager.getZ(),
                                ru.aiforge.riftworld.registry.ModSounds.HAMMER.get(),
                                net.minecraft.sounds.SoundSource.NEUTRAL, 0.45F,
                                0.92F + this.villager.getRandom().nextFloat() * 0.16F);
                    }
                }
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
