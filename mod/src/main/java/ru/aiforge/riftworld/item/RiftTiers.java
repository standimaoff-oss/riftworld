package ru.aiforge.riftworld.item;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/** Material tiers of the castle weapons. */
public enum RiftTiers implements Tier {
    MORGRATH(2400, 9.0F, 0.0F, 4, 18),
    EMBER_IRON(420, 6.0F, 1.0F, 2, 10);

    private final int uses, level, ench;
    private final float speed, damage;

    RiftTiers(int uses, float speed, float damage, int level, int ench) {
        this.uses = uses;
        this.speed = speed;
        this.damage = damage;
        this.level = level;
        this.ench = ench;
    }

    @Override public int getUses() { return uses; }
    @Override public float getSpeed() { return speed; }
    @Override public float getAttackDamageBonus() { return damage; }
    @Override public int getLevel() { return level; }
    @Override public int getEnchantmentValue() { return ench; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
}
