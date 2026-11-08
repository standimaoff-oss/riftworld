package ru.aiforge.riftworld.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;

/** Adds a concise, localised full-set hint without replacing vanilla armour behaviour. */
public final class RiftArmorItem extends ArmorItem {
    private final boolean riftcrystal;

    public RiftArmorItem(RiftArmorMaterials material, Type type, Properties properties) {
        super(material, type, properties);
        this.riftcrystal = material == RiftArmorMaterials.RIFTCRYSTAL;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(riftcrystal
                        ? "item.riftworld.riftcrystal_armor.set_bonus"
                        : "item.riftworld.aurite_armor.set_bonus")
                .withStyle(riftcrystal ? ChatFormatting.DARK_PURPLE : ChatFormatting.GOLD));
    }
}
