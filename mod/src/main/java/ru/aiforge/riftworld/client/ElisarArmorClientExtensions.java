package ru.aiforge.riftworld.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** Selects the cached geometric Aurite/Riftcrystal model for the equipped armour slot. */
public final class ElisarArmorClientExtensions implements IClientItemExtensions {
    private final boolean riftcrystal;

    public ElisarArmorClientExtensions(boolean riftcrystal) {
        this.riftcrystal = riftcrystal;
    }

    @Override
    public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
                                                   EquipmentSlot slot, HumanoidModel<?> original) {
        return ElisarArmorModel.get(slot, this.riftcrystal);
    }
}
