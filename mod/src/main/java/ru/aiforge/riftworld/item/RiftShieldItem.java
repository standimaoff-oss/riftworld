package ru.aiforge.riftworld.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import ru.aiforge.riftworld.registry.ModEquipment;

/** Ore shield: blocking, banner decoration and durability use the vanilla ShieldItem rules. */
public final class RiftShieldItem extends ShieldItem {
    private final boolean riftcrystal;

    public RiftShieldItem(Properties properties, boolean riftcrystal) {
        super(properties);
        this.riftcrystal = riftcrystal;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(riftcrystal ? ModEquipment.RIFTCRYSTAL_INGOT.get() : ModEquipment.AURITE_INGOT.get())
                || super.isValidRepairItem(stack, repair);
    }
}
