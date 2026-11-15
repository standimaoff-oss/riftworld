package ru.aiforge.riftworld.world;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import ru.aiforge.riftworld.registry.ElisarBlocks;
import ru.aiforge.riftworld.registry.ModItems;

/**
 * v0.49.0 — профессии жителей Элисара.
 *
 * <p>Каждый житель города теперь <b>кто-то</b>: у него есть профессия, свой
 * внешний вид и свои товары. Профессия определяет, кого он кормит и чем
 * торгует, а также как называет себя при торговле.</p>
 *
 * <h2>v0.54.0 — точные профессии и живая торговля</h2>
 * <p>У каждой профессии теперь два чётко разделённых списка:</p>
 * <ul>
 *   <li>{@link #goods()} — то, что торговец <b>продаёт</b> (его ремесло);</li>
 *   <li>{@link #buys()} — то, что торговец <b>покупает</b> у игрока за дукаты.</li>
 * </ul>
 * <p>Списки принципиально не пересекаются: торговец не покупает то, что сам же
 * и продаёт, иначе получился бы бесконечный цикл «продать → купить → продать»
 * с печатью дукатов из воздуха. Подробнее — в {@link ElisarEconomy}.</p>
 */
public enum ElisarProfession {

    /** Каменщик — продаёт строительные блоки, покупает булыжник и песок. */
    MASON("Каменщик", 0x9C7C5A, 0x6E563E) {
        @Override
        public void dress(LivingEntity villager) {
            villager.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
        }

        @Override
        public List<ItemStack> goods() {
            return List.of(
                    new ItemStack(ElisarBlocks.SIZO_PLANKS.get(), 16),
                    new ItemStack(ElisarBlocks.STONE_WALL.get(), 12),
                    new ItemStack(ElisarBlocks.MOSS.get(), 8),
                    new ItemStack(Items.BRICKS, 8));
        }

        @Override
        public List<ItemStack> buys() {
            // Каменщик покупает булыжник, песок и камень — то, что ни он,
            // ни другой торговец не продаёт.
            return List.of(
                    new ItemStack(Items.COBBLESTONE, 16),
                    new ItemStack(Items.SAND, 8),
                    new ItemStack(Items.STONE, 8));
        }
    },

    /** Кузнец — работает с ауритом, продаёт слитки и оружие, покупает руду и уголь. */
    SMITH("Кузнец", 0x5A4A3A, 0x3A2E24) {
        @Override
        public void dress(LivingEntity villager) {
            villager.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            villager.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        }

        @Override
        public List<ItemStack> goods() {
            return List.of(
                    new ItemStack(ModItems.AURITE_INGOT.get(), 2),
                    new ItemStack(ModItems.AURITE_PICKAXE.get(), 1),
                    new ItemStack(ModItems.AURITE_SHOVEL.get(), 1),
                    new ItemStack(ModItems.ELISAR_GUARD_SWORD.get(), 1));
        }

        @Override
        public List<ItemStack> buys() {
            // Кузнец берёт сырьё: руду, уголь и железо.
            return List.of(
                    new ItemStack(Items.RAW_IRON, 4),
                    new ItemStack(Items.COAL, 8),
                    new ItemStack(Items.IRON_INGOT, 2));
        }
    },

    /** Оружейник — торгует оружием и бронёй, покупает кожу и кольчугу. */
    ARMOURER("Оружейник", 0x3F5A6E, 0x2A3E4E) {
        @Override
        public void dress(LivingEntity villager) {
            villager.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
            villager.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.ELISAR_GUARD_SHIELD.get()));
        }

        @Override
        public List<ItemStack> goods() {
            return List.of(
                    new ItemStack(ModItems.AURITE_SWORD.get(), 1),
                    new ItemStack(ModItems.ELISAR_BOW.get(), 1),
                    new ItemStack(ModItems.ELISAR_SHIELD.get(), 1),
                    new ItemStack(ModItems.ELISAR_GUARD_SHIELD.get(), 1));
        }

        @Override
        public List<ItemStack> buys() {
            // Оружейник покупает только то, что не продаёт никто другой:
            // ауритовый слиток продаёт Кузнец, и если бы Оружейник его тоже
            // скупал, появился бы кросс-профессиональный цикл обмена.
            return List.of(
                    new ItemStack(Items.CHAINMAIL_CHESTPLATE, 1),
                    new ItemStack(Items.FLINT, 4),
                    new ItemStack(Items.IRON_INGOT, 2));
        }
    },

    /** Травница — лечит и торгует зельями, покупает травы и грибы. */
    HERBALIST("Травница", 0x4E6B4A, 0x364A34) {
        @Override
        public void dress(LivingEntity villager) {
            villager.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GREEN_WOOL));
            villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BREWING_STAND));
        }

        @Override
        public List<ItemStack> goods() {
            return List.of(
                    new ItemStack(Items.POTION, 1),
                    new ItemStack(Items.BREAD, 6),
                    new ItemStack(ElisarBlocks.BLOSSOM_MOSS.get(), 4),
                    new ItemStack(Items.HONEY_BOTTLE, 2));
        }

        @Override
        public List<ItemStack> buys() {
            return List.of(
                    new ItemStack(Items.NETHER_WART, 4),
                    new ItemStack(Items.BROWN_MUSHROOM, 4),
                    new ItemStack(Items.SUGAR_CANE, 4));
        }
    },

    /** Рыбак — торгует рыбой, покупает верёвку и ведро воды. */
    FISHER("Рыбак", 0x3F6E7A, 0x2A4A54) {
        @Override
        public void dress(LivingEntity villager) {
            villager.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.FISHING_ROD));
            villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
        }

        @Override
        public List<ItemStack> goods() {
            return List.of(
                    new ItemStack(Items.COD, 4),
                    new ItemStack(Items.SALMON, 3),
                    new ItemStack(Items.TROPICAL_FISH, 1),
                    new ItemStack(Items.KELP, 4));
        }

        @Override
        public List<ItemStack> buys() {
            return List.of(
                    new ItemStack(Items.STRING, 8),
                    new ItemStack(Items.WATER_BUCKET, 1),
                    new ItemStack(Items.FISHING_ROD, 1));
        }
    },

    /** Пекарь — печёт хлеб, покупает сахар, яйца и морковь. */
    BAKER("Пекарь", 0xC2A06A, 0x8A7048) {
        @Override
        public void dress(LivingEntity villager) {
            villager.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.BREAD));
            villager.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        }

        @Override
        public List<ItemStack> goods() {
            return List.of(
                    new ItemStack(Items.BREAD, 8),
                    new ItemStack(Items.CAKE, 1),
                    new ItemStack(Items.WHEAT, 8),
                    new ItemStack(Items.HONEY_BOTTLE, 1));
        }

        @Override
        public List<ItemStack> buys() {
            // Пшеницу Пекарь САМ продаёт — покупать её у себя нельзя,
            // иначе получился бы цикл «продать пшеницу → выкупить её же».
            return List.of(
                    new ItemStack(Items.SUGAR, 4),
                    new ItemStack(Items.EGG, 8),
                    new ItemStack(Items.CARROT, 6));
        }
    },

    /** Скульптор — торгует гипсом и редкими камнями, покупает песчаник и базальт. */
    SCULPTOR("Скульптор", 0xB8B0A4, 0x8A8278) {
        @Override
        public void dress(LivingEntity villager) {
            villager.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.STONE_AXE));
            villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        }

        @Override
        public List<ItemStack> goods() {
            return List.of(
                    new ItemStack(Items.CALCITE, 8),
                    new ItemStack(Items.TUFF, 8),
                    new ItemStack(Items.AMETHYST_SHARD, 2),
                    new ItemStack(ModItems.GILDED_PAPER.get(), 1));
        }

        @Override
        public List<ItemStack> buys() {
            return List.of(
                    new ItemStack(Items.SANDSTONE, 8),
                    new ItemStack(Items.POLISHED_BASALT, 8),
                    new ItemStack(Items.QUARTZ, 4));
        }
    },

    /** Скупщик — принимает монетное серебро и золото, выдаёт редкие товары. */
    MERCHANT("Скупщик", 0x6E5A9C, 0x4A3E6E) {
        @Override
        public void dress(LivingEntity villager) {
            villager.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.PURPLE_WOOL));
            villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.ELISAR_DUCAT.get()));
        }

        @Override
        public List<ItemStack> goods() {
            return List.of(
                    new ItemStack(ModItems.ELISAR_DUCAT.get(), 4),
                    new ItemStack(ModItems.GILDED_PAPER.get(), 1),
                    new ItemStack(Items.EMERALD, 1),
                    new ItemStack(Items.REDSTONE, 8));
        }

        @Override
        public List<ItemStack> buys() {
            // Изумруд и слиток Скупщик САМ продаёт — покупать их у себя нельзя.
            // Берёт только то, что не продаёт никто.
            return List.of(
                    new ItemStack(Items.RAW_GOLD, 2),
                    new ItemStack(Items.COAL, 8),
                    new ItemStack(Items.BONE_BLOCK, 4));
        }
    },

    /**
     * v0.55.0: Охотник — добыча зверя и мяса.
     *
     * <p>Продаёт мясо, кожу и трофеи; покупает у игрока зелья силы и зелья
     * невидимости — то, что охотник сам не добывает.</p>
     */
    HUNTER("Охотник", 0x5E6B3A, 0x3E4726) {
        @Override
        public void dress(LivingEntity villager) {
            villager.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
            villager.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        }

        @Override
        public List<ItemStack> goods() {
            return List.of(
                    new ItemStack(Items.RABBIT, 4),
                    new ItemStack(Items.MUTTON, 4),
                    new ItemStack(Items.RABBIT_HIDE, 4),
                    new ItemStack(Items.FEATHER, 8));
        }

        @Override
        public List<ItemStack> buys() {
            // Стрелы, кожа и мясо уже продаются другими (Кожевник, Мясник) —
            // покупать их здесь нельзя, иначе появляется цикл.
            return List.of(
                    new ItemStack(Items.ARROW, 16),
                    new ItemStack(Items.BONE, 4),
                    new ItemStack(Items.STRING, 8));
        }
    },

    /**
     * v0.55.0: Банкир — меняет монеты на товары и обратно.
     *
     * <p>Единственный торговец, который принимает и выдаёт сами монеты:
     * игрок может разменять {@code elisar_ducat} на ауритовые слитки и
     * обратно. Курс фиксирован, поэтому арбитраж невозможен.</p>
     */
    BANKER("Банкир", 0x2E4A6B, 0x1E3145) {
        @Override
        public void dress(LivingEntity villager) {
            villager.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.BLUE_WOOL));
            villager.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
            villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WRITABLE_BOOK));
        }

        @Override
        public List<ItemStack> goods() {
            return List.of(
                    new ItemStack(ModItems.AURITE_INGOT.get(), 2),
                    new ItemStack(Items.GOLD_INGOT, 2),
                    new ItemStack(Items.EMERALD, 1),
                    new ItemStack(Items.REDSTONE, 4));
        }

        @Override
        public List<ItemStack> buys() {
            // Банкир принимает сырьё для монет: сырое золото и золотые самородки.
            // Сам монетный слиток не покупает — его и так продаёт.
            return List.of(
                    new ItemStack(Items.RAW_GOLD, 2),
                    new ItemStack(Items.GOLD_NUGGET, 8),
                    new ItemStack(Items.NETHERITE_SCRAP, 1));
        }
    },

    /** v0.55.0: Кожевник — выделывает кожу и шкуры. */
    TANNER("Кожевник", 0x8A6640, 0x5E442B) {
        @Override
        public void dress(LivingEntity villager) {
            villager.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.BROWN_WOOL));
            villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.LEATHER));
        }

        @Override
        public List<ItemStack> goods() {
            return List.of(
                    new ItemStack(Items.LEATHER, 8),
                    new ItemStack(Items.RABBIT_FOOT, 4),
                    new ItemStack(Items.RABBIT_HIDE, 4),
                    new ItemStack(Items.LEATHER_HELMET, 1));
        }

        @Override
        public List<ItemStack> buys() {
            // Кожевник берёт сырьё и расходники: угли, нить и землю.
            return List.of(
                    new ItemStack(Items.CHARCOAL, 4),
                    new ItemStack(Items.STRING, 8),
                    new ItemStack(Items.DIRT, 8));
        }
    },

    /** v0.55.0: Пивовар — варит зелья и продаёт их. */
    BREWER("Пивовар", 0xC08A3E, 0x8A6026) {
        @Override
        public void dress(LivingEntity villager) {
            villager.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.BROWN_WOOL));
            villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BREWING_STAND));
        }

        @Override
        public List<ItemStack> goods() {
            return List.of(
                    new ItemStack(Items.BREWING_STAND, 1),
                    new ItemStack(Items.BAKED_POTATO, 6),
                    new ItemStack(Items.HAY_BLOCK, 2),
                    new ItemStack(Items.WHEAT, 12));
        }

        @Override
        public List<ItemStack> buys() {
            // Пивовар покупает солод, хмель и бутылки — их никто не продаёт.
            return List.of(
                    new ItemStack(Items.WHEAT_SEEDS, 4),
                    new ItemStack(Items.NETHER_WART, 4),
                    new ItemStack(Items.GLASS_BOTTLE, 6));
        }
    },

    /** v0.55.0: Ювелир — работает с самоцветами. */
    JEWELLER("Ювелир", 0x8C6BB0, 0x5E4878) {
        @Override
        public void dress(LivingEntity villager) {
            villager.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CYAN_WOOL));
            villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.AMETHYST_SHARD));
        }

        @Override
        public List<ItemStack> goods() {
            return List.of(
                    new ItemStack(Items.AMETHYST_SHARD, 4),
                    new ItemStack(Items.CALCITE, 8),
                    new ItemStack(Items.LAPIS_LAZULI, 6),
                    new ItemStack(Items.QUARTZ_BLOCK, 1));
        }

        @Override
        public List<ItemStack> buys() {
            // Золото, алмазы и красная пыль — их не продаёт никто.
            return List.of(
                    new ItemStack(Items.DIAMOND, 1),
                    new ItemStack(Items.GOLD_NUGGET, 4),
                    new ItemStack(Items.STICK, 8));
        }
    },

    /** v0.55.0: Возчик — перевозит товары по городу. */
    CARTER("Возчик", 0x7A6A4A, 0x544834) {
        @Override
        public void dress(LivingEntity villager) {
            villager.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.TURTLE_HELMET));
            villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.SADDLE));
        }

        @Override
        public List<ItemStack> goods() {
            return List.of(
                    new ItemStack(Items.SADDLE, 1),
                    new ItemStack(Items.LEAD, 4),
                    new ItemStack(Items.MINECART, 1),
                    new ItemStack(Items.OAK_BOAT, 1));
        }

        @Override
        public List<ItemStack> buys() {
            // Возчик берёт верёвку, сундуки и железо — это не ремесленный товар.
            return List.of(
                    new ItemStack(Items.STRING, 8),
                    new ItemStack(Items.CHEST, 1),
                    new ItemStack(Items.IRON_INGOT, 2));
        }
    };

    private final String displayName;
    private final int primary;
    private final int secondary;

    ElisarProfession(String displayName, int primary, int secondary) {
        this.displayName = displayName;
        this.primary = primary;
        this.secondary = secondary;
    }

    public String displayName() {
        return this.displayName;
    }

    /** Основной цвет профессии (плащ). */
    public int primaryColour() {
        return this.primary;
    }

    /** Дополнительный цвет (отделка). */
    public int secondaryColour() {
        return this.secondary;
    }

    /** Одеть жителя по профессии. */
    public abstract void dress(LivingEntity villager);

    /** Товары профессии — то, чем торговец торгует (продаёт игроку). */
    public abstract List<ItemStack> goods();

    /**
     * Что торговец покупает у игрока за дукаты.
     *
     * <p>Ни один предмет в этом списке не должен совпадать с предметом из
     * {@link #goods()} — иначе появится цикл печати денег. Подробнее — в
     * {@link ElisarEconomy}.</p>
     */
    public abstract List<ItemStack> buys();

    /**
     * Локализованное название профессии.
     *
     * <p>Ключ перевода {@code profession.riftworld.<профессия>} есть и в русском,
     * и в английском файле; если ключа нет — показываем встроенное название.</p>
     */
    public Component label() {
        String key = "profession.riftworld." + name().toLowerCase(java.util.Locale.ROOT);
        var translated = Component.translatable(key);
        // translatable без ключа показывает сам ключ — тогда отдаём встроенное имя.
        return translated.getString().equals(key) ? Component.literal(this.displayName) : translated;
    }

    /**
     * Выбрать профессию по устойчивому «характеру» — одна и та же душа всегда
     * получает одну и ту же профессию, чтобы город был стабилен между
     * перезапусками мира.
     */
    public static ElisarProfession forSeed(int seed) {
        ElisarProfession[] all = values();
        return all[Math.floorMod(seed, all.length)];
    }
}
