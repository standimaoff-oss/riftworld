package ru.aiforge.riftworld.item;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import ru.aiforge.riftworld.registry.ModItems;

/**
 * v0.48.0: Изогнутый лук Элисара с рифт-тетивой (референс 03_riftworld_weapon_tools_set.png #4).
 * Совместим со всеми ванильными стрелами и чарами лука, повышает урон стрелы и чинится ауритом/рифт-кристаллом.
 */
public class ElisarBowItem extends BowItem {
    public ElisarBowItem(Properties props) {
        super(props);
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow) {
        arrow.setBaseDamage(arrow.getBaseDamage() + 0.75D);
        return arrow;
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.AURITE_INGOT.get())
                || repair.is(ModItems.RIFTCRYSTAL.get())
                || super.isValidRepairItem(toRepair, repair);
    }
}
