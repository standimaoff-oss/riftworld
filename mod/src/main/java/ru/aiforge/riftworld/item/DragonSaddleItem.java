package ru.aiforge.riftworld.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Особое кастомное Драконье седло Элисара: даёт значительно лучшую манёвренность,
 * отзывчивость управления и +35% запаса тяги подъёма по сравнению с ванильным седлом.
 */
public class DragonSaddleItem extends Item {
    public DragonSaddleItem(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.riftworld.dragon_saddle.desc1").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.riftworld.dragon_saddle.desc2").withStyle(ChatFormatting.AQUA));
    }
}
