package ru.aiforge.riftworld.item;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import ru.aiforge.riftworld.registry.ModItems;

/**
 * v0.52.0 — материалы брони мода с ВАНИЛЬНОЙ геометрией.
 *
 * <p>Ключевое отличие от старой «HD-брони» мода (v0.49.0): здесь нет ни одной
 * кастомной 3D-модели. Геометрия — ровно ванильная: стандартный
 * {@code HumanoidArmorLayer} с {@code ModelLayers.PLAYER_INNER_ARMOR} и
 * {@code ModelLayers.PLAYER_OUTER_ARMOR}. Кубы, посадка на тело и UV-развёртка
 * совпадают с ванильной бронёй (diamond/netherite/iron) один в один.
 *
 * <p>Текстуры — по ванильному принципу:
 * {@code assets/riftworld/textures/models/armor/<материал>_layer_1.png} и
 * {@code _layer_2.png} (64×32, раскладка player-skin).
 * Forge 1.20.1 резолвит их сам из {@link #getName()} по шаблону
 * {@code "%s:textures/models/armor/%s_layer_%d%s.png"} — ровно как у ванильной брони
 * (см. {@code HumanoidArmorLayer#getArmorResource} в Forge 1.20.1).
 *
 * <p>В 1.20.1 {@link ArmorMaterial} — это интерфейс (record с полями слоёв
 * появился только в 1.20.5+), а {@code getDurability}/{@code getProtection}
 * принимают {@link ArmorItem.Type}.
 */
public final class RiftArmorMaterials {

    private RiftArmorMaterials() {
    }

    /**
     * Базовая прочность детали по типам брони.
     * Порядок и значения — как в ванильном {@code ArmorMaterials}:
     * BOOTS=13, LEGGINGS=15, CHESTPLATE=16, HELMET=11.
     */
    private static final Map<ArmorItem.Type, Integer> BASE_DURABILITY = new EnumMap<>(ArmorItem.Type.class);

    static {
        BASE_DURABILITY.put(ArmorItem.Type.BOOTS, 13);
        BASE_DURABILITY.put(ArmorItem.Type.LEGGINGS, 15);
        BASE_DURABILITY.put(ArmorItem.Type.CHESTPLATE, 16);
        BASE_DURABILITY.put(ArmorItem.Type.HELMET, 11);
    }

    /** Реализация {@link ArmorMaterial} для Forge 1.20.1. */
    public static final class ModArmorMaterial implements ArmorMaterial {
        private final String name;
        private final Map<ArmorItem.Type, Integer> protection;
        private final int durabilityMultiplier;
        private final int enchantability;
        private final float toughness;
        private final float knockbackResistance;
        private final SoundEvent equipSound;
        private final Supplier<Ingredient> repairIngredient;

        ModArmorMaterial(String name,
                         int[] protectionValues,
                         int durabilityMultiplier,
                         int enchantability,
                         float toughness,
                         float knockbackResistance,
                         SoundEvent equipSound,
                         Supplier<Ingredient> repairIngredient) {
            this.name = name;
            this.protection = new EnumMap<>(ArmorItem.Type.class);
            this.protection.put(ArmorItem.Type.BOOTS, protectionValues[0]);
            this.protection.put(ArmorItem.Type.LEGGINGS, protectionValues[1]);
            this.protection.put(ArmorItem.Type.CHESTPLATE, protectionValues[2]);
            this.protection.put(ArmorItem.Type.HELMET, protectionValues[3]);
            this.durabilityMultiplier = durabilityMultiplier;
            this.enchantability = enchantability;
            this.toughness = toughness;
            this.knockbackResistance = knockbackResistance;
            this.equipSound = equipSound;
            this.repairIngredient = repairIngredient;
        }

        @Override
        public int getDurability(ArmorItem.Type type) {
            return BASE_DURABILITY.getOrDefault(type, 13) * this.durabilityMultiplier;
        }

        @Override
        public int getProtection(ArmorItem.Type type) {
            return this.protection.getOrDefault(type, 0);
        }

        @Override
        public int getEnchantability() {
            return this.enchantability;
        }

        @Override
        public SoundEvent getEquipSound() {
            return this.equipSound;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return this.repairIngredient.get();
        }

        /**
         * Имя материала. Формат {@code "namespace:path"} — Forge сам соберёт путь
         * {@code <namespace>:textures/models/armor/<path>_layer_1.png}.
         */
        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public float getToughness() {
            return this.toughness;
        }

        @Override
        public float getKnockbackResistance() {
            return this.knockbackResistance;
        }
    }

    // ------------------------------------------------------------------
    // Комплекты брони. Характеристики подобраны по ванильным аналогам:
    // кожа (1/2/3/1, множитель 5), алмаз (3/6/8/3, множитель 33),
    // незерит (3/6/8/3, множитель 37).
    // ------------------------------------------------------------------

    /** Кожаная броня Элисара — базовый комплект, чинится кожей. */
    public static final ArmorMaterial LEATHER = new ModArmorMaterial(
            "riftworld:leather",
            new int[]{1, 2, 3, 1},
            6, 15, 0.0F, 0.0F,
            SoundEvents.ARMOR_EQUIP_LEATHER,
            () -> Ingredient.of(Items.LEATHER));

    /** Ауритовая броня — средний комплект, чинится ауритовым слитком. */
    public static final ArmorMaterial AURITE = new ModArmorMaterial(
            "riftworld:aurite",
            new int[]{3, 5, 6, 3},
            24, 17, 1.5F, 0.0F,
            SoundEvents.ARMOR_EQUIP_IRON,
            () -> Ingredient.of(ModItems.AURITE_INGOT.get()));

    /** Броня рифт-кристалла — тяжёлый комплект, чинится рифт-кристаллом. */
    public static final ArmorMaterial RIFTCRYSTAL = new ModArmorMaterial(
            "riftworld:riftcrystal",
            new int[]{3, 6, 8, 3},
            33, 15, 2.5F, 0.1F,
            SoundEvents.ARMOR_EQUIP_DIAMOND,
            () -> Ingredient.of(ModItems.RIFTCRYSTAL.get()));
}
