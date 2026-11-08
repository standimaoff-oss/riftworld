package ru.aiforge.riftworld.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.crafting.Ingredient;

/** Armor materials themed after Elisar's coin-metal and the scarce rift crystal. */
public enum RiftArmorMaterials implements ArmorMaterial {
    AURITE("aurite", 31, new int[]{3, 6, 8, 3}, 18, SoundEvents.ARMOR_EQUIP_GOLD, 1.0F, 0.0F),
    RIFTCRYSTAL("riftcrystal", 39, new int[]{4, 7, 9, 4}, 24, SoundEvents.ARMOR_EQUIP_NETHERITE, 2.5F, 0.1F);

    private final String name;
    private final int durability, enchantment;
    private final int[] defense;
    private final SoundEvent equipSound;
    private final float toughness, knockback;

    RiftArmorMaterials(String name, int durability, int[] defense, int enchantment, SoundEvent equipSound,
                       float toughness, float knockback) {
        this.name = name;
        this.durability = durability;
        this.defense = defense;
        this.enchantment = enchantment;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.knockback = knockback;
    }

    @Override public int getDurabilityForType(ArmorItem.Type type) {
        int multiplier = switch (type) {
            case BOOTS -> 13;
            case LEGGINGS -> 15;
            case CHESTPLATE -> 16;
            case HELMET -> 11;
        };
        return multiplier * durability;
    }
    @Override public int getDefenseForType(ArmorItem.Type type) {
        int index = switch (type) {
            case BOOTS -> 0;
            case LEGGINGS -> 1;
            case CHESTPLATE -> 2;
            case HELMET -> 3;
        };
        return defense[index];
    }
    @Override public int getEnchantmentValue() { return enchantment; }
    @Override public SoundEvent getEquipSound() { return equipSound; }
    @Override public Ingredient getRepairIngredient() {
        return Ingredient.of(name.equals("aurite")
                ? ru.aiforge.riftworld.registry.ModItems.AURITE_INGOT.get()
                : ru.aiforge.riftworld.registry.ModItems.RIFTCRYSTAL.get());
    }
    @Override public String getName() { return "riftworld:" + name; }
    @Override public float getToughness() { return toughness; }
    @Override public float getKnockbackResistance() { return knockback; }
}
