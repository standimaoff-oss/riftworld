package ru.aiforge.riftworld.item;

import java.util.function.Supplier;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * v0.40.0: уровни инструментов и оружия Элисара.
 *
 * <p>{@code AURITE} — уровень алмаза (копает обсидиан), {@code RIFTCRYSTAL} — уровень незерита,
 * служит заметно дольше и бьёт сильнее. Палки референса 03 тёмно-коричневые, поэтому обе рукояти
 * рисуются одним тоном, а головы — в цвете своего металла.
 */
public enum RiftGearTiers implements Tier {
    AURITE(1561, 8.0F, 3.0F, 3, 12,
            () -> Ingredient.of(ru.aiforge.riftworld.registry.ModItems.AURITE_INGOT.get())),
    RIFTCRYSTAL(3072, 10.5F, 5.0F, 4, 18,
            () -> Ingredient.of(ru.aiforge.riftworld.registry.ModItems.RIFTCRYSTAL.get()));

    private final int uses;
    private final float speed;
    private final float damage;
    private final int level;
    private final int enchantmentValue;
    private final Supplier<Ingredient> repairIngredient;

    RiftGearTiers(int uses, float speed, float damage, int level, int enchantmentValue,
                  Supplier<Ingredient> repairIngredient) {
        this.uses = uses;
        this.speed = speed;
        this.damage = damage;
        this.level = level;
        this.enchantmentValue = enchantmentValue;
        this.repairIngredient = repairIngredient;
    }

    @Override
    public int getUses() {
        return uses;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return damage;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repairIngredient.get();
    }
}
