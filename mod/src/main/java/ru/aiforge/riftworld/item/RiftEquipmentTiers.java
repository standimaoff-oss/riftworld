package ru.aiforge.riftworld.item;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import ru.aiforge.riftworld.registry.ModEquipment;

/** Tool tiers shared by swords, pickaxes and the double-headed war axes. */
public enum RiftEquipmentTiers implements Tier {
    AURITE(720, 7.5F, 3.0F, 2, 14),
    RIFTCRYSTAL(1850, 9.0F, 4.5F, 4, 18);

    private final int uses;
    private final float speed;
    private final float damage;
    private final int level;
    private final int enchantmentValue;

    RiftEquipmentTiers(int uses, float speed, float damage, int level, int enchantmentValue) {
        this.uses = uses;
        this.speed = speed;
        this.damage = damage;
        this.level = level;
        this.enchantmentValue = enchantmentValue;
    }

    @Override public int getUses() { return uses; }
    @Override public float getSpeed() { return speed; }
    @Override public float getAttackDamageBonus() { return damage; }
    @Override public int getLevel() { return level; }
    @Override public int getEnchantmentValue() { return enchantmentValue; }
    @Override public Ingredient getRepairIngredient() {
        return Ingredient.of(this == AURITE ? ModEquipment.AURITE_INGOT.get() : ModEquipment.RIFTCRYSTAL_INGOT.get());
    }
}
