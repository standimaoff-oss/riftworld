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
 */
public enum ElisarProfession {

    /** Каменщик — продаёт строительные блоки. */
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
    },

    /** Кузнец — работает с ауритом, продаёт слитки и оружие. */
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
    },

    /** Оружейник — торгует оружием и бронёй. */
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
                    new ItemStack(ModItems.AURITE_HELMET.get(), 1));
        }
    },

    /** Травница — лечит и торгует зельями. */
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
    },

    /** Рыбак — торгует рыбой. */
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
    },

    /** Пекарь — печёт хлеб. */
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
    },

    /** Скульптор — торгует гипсом и редкими камнями. */
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
    },

    /** Скупщик — принимает дукаты и даёт редкие товары. */
    MERCHANT("Скупщик", 0x6E5A9C, 0x4A3E6E) {
        @Override
        public void dress(LivingEntity villager) {
            villager.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.PURPLE_WOOL));
            villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.ELISAR_DUCAT.get()));
        }

        @Override
        public List<ItemStack> goods() {
            return List.of(
                    new ItemStack(ModItems.ELISAR_DUCAT.get(), 1),
                    new ItemStack(ModItems.GILDED_PAPER.get(), 1),
                    new ItemStack(Items.EMERALD, 1),
                    new ItemStack(Items.DIAMOND, 1));
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

    /** Товары профессии — то, чем торгует этот житель. */
    public abstract List<ItemStack> goods();

    public Component label() {
        return Component.literal(this.displayName);
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
