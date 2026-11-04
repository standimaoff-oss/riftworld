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
 * Особая бумага Элисара для приручения дикого Бумажного Дракона во время сна на вершине
 * Летающей Перевёрнутой Башни и окрашивания его 2 усов в соответствующий благородный оттенок.
 */
public class DragonPaperItem extends Item {
    public static final int WHISKER_IVORY    = 0;
    public static final int WHISKER_GILDED   = 1;
    public static final int WHISKER_EMERALD  = 2;
    public static final int WHISKER_DIAMOND  = 3;
    public static final int WHISKER_AMETHYST = 4;
    public static final int WHISKER_CRIMSON  = 5;
    public static final int WHISKER_LAPIS    = 6;

    private final int whiskerType;

    public DragonPaperItem(int whiskerType, Properties props) {
        super(props);
        this.whiskerType = whiskerType;
    }

    public int whiskerType() {
        return whiskerType;
    }

    public static int colorForWhiskerType(int type) {
        return switch (type) {
            case WHISKER_GILDED   -> 0xFFD24A;
            case WHISKER_EMERALD  -> 0x22DF72;
            case WHISKER_DIAMOND  -> 0x46E8FF;
            case WHISKER_AMETHYST -> 0xC768FF;
            case WHISKER_CRIMSON  -> 0xEE3B3B;
            case WHISKER_LAPIS    -> 0x3E7BFF;
            default               -> 0xF5EFE2;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.riftworld.dragon_paper.desc1").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.riftworld.dragon_paper.desc2").withStyle(ChatFormatting.GOLD));
    }
}
