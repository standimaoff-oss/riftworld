package ru.aiforge.riftworld.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import ru.aiforge.riftworld.registry.ModItems;

/**
 * v0.48.0: Щит Элисара (референс 03 #5 — обсидианово-золотой щит с циановыми самоцветами,
 * а также щит Стража из референса 06_elisar_guard.png).
 * Рендерится честной 3D воксельной JSON-моделью с отдельным состоянием блокирования.
 */
public class ElisarShieldItem extends ShieldItem {
    public ElisarShieldItem(Properties props) {
        super(props);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        return ToolActions.DEFAULT_SHIELD_ACTIONS.contains(toolAction);
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.AURITE_INGOT.get())
                || repair.is(ModItems.RIFTCRYSTAL.get());
    }
}
