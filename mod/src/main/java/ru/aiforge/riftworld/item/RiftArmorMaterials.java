package ru.aiforge.riftworld.item;

import java.util.function.Supplier;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * v0.40.0: два комплекта доспехов Элисара.
 *
 * <p>{@code AURITE} — «средний»: выплавляется из ауритовой руды, той самой, что идёт на монеты
 * (референс 01 — тёмная сталь, кожаные ремни, золотистая окантовка).
 *
 * <p>{@code RIFTCRYSTAL} — «сильный»: редкий рифт-кристалл из 2–3 биомов (референс 02 — почти
 * чёрный кристалл, бирюзовое свечение, маджентовые искры).
 *
 * <p>Текстуры лежат там, куда их смотрит Forge:
 * {@code assets/riftworld/textures/models/armor/<name>_layer_1.png} и {@code _layer_2.png}.
 */
public enum RiftArmorMaterials implements ArmorMaterial {
    AURITE("aurite",
            new int[]{240, 275, 300, 210},   // boots, leggings, chestplate, helmet
            new int[]{2, 5, 6, 2},
            12, 1.0F, 0.0F, SoundEvents.ARMOR_EQUIP_IRON,
            () -> Ingredient.of(ru.aiforge.riftworld.registry.ModItems.AURITE_INGOT.get())),

    RIFTCRYSTAL("riftcrystal",
            new int[]{480, 545, 590, 420},
            new int[]{4, 7, 9, 4},
            18, 3.5F, 0.15F, SoundEvents.ARMOR_EQUIP_DIAMOND,
            () -> Ingredient.of(ru.aiforge.riftworld.registry.ModItems.RIFTCRYSTAL.get()));

    private final String name;
    /** Прочность по слотам в порядке {@code BOOTS, LEGGINGS, CHESTPLATE, HELMET}. */
    private final int[] durability;
    private final int[] defense;
    private final int enchantmentValue;
    private final float toughness;
    private final float knockbackResistance;
    private final SoundEvent equipSound;
    private final Supplier<Ingredient> repairIngredient;

    RiftArmorMaterials(String name, int[] durability, int[] defense, int enchantmentValue,
                       float toughness, float knockbackResistance, SoundEvent equipSound,
                       Supplier<Ingredient> repairIngredient) {
        this.name = name;
        this.durability = durability;
        this.defense = defense;
        this.enchantmentValue = enchantmentValue;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.equipSound = equipSound;
        this.repairIngredient = repairIngredient;
    }

    private int index(ArmorItem.Type type) {
        switch (type) {
            case LEGGINGS:
                return 1;
            case CHESTPLATE:
                return 2;
            case HELMET:
                return 3;
            case BOOTS:
            default:
                return 0;
        }
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return durability[index(type)];
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return defense[index(type)];
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public SoundEvent getEquipSound() {
        return equipSound;
    }

    /** Починить можно только своим же материалом; Supplier нужен, чтобы не трогать реестр до его готовности. */
    @Override
    public Ingredient getRepairIngredient() {
        return repairIngredient.get();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public float getToughness() {
        return toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return knockbackResistance;
    }
}
