package ru.aiforge.riftworld.item;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import ru.aiforge.riftworld.registry.ModEquipment;

/** Armour tuning for the two Elisar ore sets (Minecraft 1.20.1). */
public enum RiftArmorMaterials implements ArmorMaterial {
    AURITE("aurite", 18, 1.0F, 0.0F, 14, SoundEvents.ARMOR_EQUIP_GOLD,
            protection(2, 5, 6, 3), false),
    RIFTCRYSTAL("riftcrystal", 36, 3.0F, 0.10F, 18, SoundEvents.ARMOR_EQUIP_NETHERITE,
            protection(4, 7, 8, 3), true);

    private final String name;
    private final int durabilityMultiplier;
    private final float toughness;
    private final float knockbackResistance;
    private final int enchantmentValue;
    private final SoundEvent equipSound;
    private final Map<ArmorItem.Type, Integer> protection;
    private final boolean fireResistant;

    RiftArmorMaterials(String name, int durabilityMultiplier, float toughness, float knockbackResistance,
                       int enchantmentValue, SoundEvent equipSound, Map<ArmorItem.Type, Integer> protection,
                       boolean fireResistant) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.enchantmentValue = enchantmentValue;
        this.equipSound = equipSound;
        this.protection = protection;
        this.fireResistant = fireResistant;
    }

    private static Map<ArmorItem.Type, Integer> protection(int boots, int leggings, int chestplate, int helmet) {
        Map<ArmorItem.Type, Integer> result = new EnumMap<>(ArmorItem.Type.class);
        result.put(ArmorItem.Type.BOOTS, boots);
        result.put(ArmorItem.Type.LEGGINGS, leggings);
        result.put(ArmorItem.Type.CHESTPLATE, chestplate);
        result.put(ArmorItem.Type.HELMET, helmet);
        return result;
    }

    public boolean isFireResistant() {
        return fireResistant;
    }

    @Override public int getDurabilityForType(ArmorItem.Type type) { return durabilityMultiplier * switch (type) {
        case BOOTS -> 13; case LEGGINGS -> 15; case CHESTPLATE -> 16; case HELMET -> 11;
    }; }
    @Override public int getDefenseForType(ArmorItem.Type type) { return protection.get(type); }
    @Override public int getEnchantmentValue() { return enchantmentValue; }
    @Override public SoundEvent getEquipSound() { return equipSound; }
    @Override public Ingredient getRepairIngredient() {
        return Ingredient.of(this == AURITE ? ModEquipment.AURITE_INGOT.get() : ModEquipment.RIFTCRYSTAL_INGOT.get());
    }
    /** The mod namespace is required: vanilla resolves textures/models/armor from this value. */
    @Override public String getName() { return "riftworld:" + name; }
    @Override public float getToughness() { return toughness; }
    @Override public float getKnockbackResistance() { return knockbackResistance; }
}
