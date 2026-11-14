package ru.aiforge.riftworld.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import ru.aiforge.riftworld.registry.ModItems;

/**
 * v0.48.0: Щит Элисара (референс 03 #5 — обсидианово-золотой щит с циановыми самоцветами,
 * а также щит Стража из референса 06_elisar_guard.png).
 *
 * <p>v0.53.0: щит рисуется собственным рендерером
 * ({@link ru.aiforge.riftworld.client.ShieldItemRenderer}) — в Forge 1.20.1
 * ванильный рендерер щита работает только для {@code minecraft:shield},
 * поэтому кастомный щит без такого рендерера в руке пустой.
 * Геометрия — ванильная (12×22×1 «plate» + 2×6×6 «handle»),
 * текстура — {@code textures/entity/<имя>_base_nopattern.png} в палитре мода.
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

    @Override
    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return ru.aiforge.riftworld.client.ShieldItemRenderer.instance();
            }
        });
    }
}
