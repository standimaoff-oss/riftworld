package ru.aiforge.riftworld.item;

import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import ru.aiforge.riftworld.registry.ModEquipment;

/** A normal, fully functional bow with an ore-specific anvil repair material. */
public final class RiftBowItem extends BowItem {
    private final boolean riftcrystal;

    public RiftBowItem(Properties properties, boolean riftcrystal) {
        super(properties);
        this.riftcrystal = riftcrystal;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(riftcrystal ? ModEquipment.RIFTCRYSTAL_INGOT.get() : ModEquipment.AURITE_INGOT.get())
                || super.isValidRepairItem(stack, repair);
    }
}
