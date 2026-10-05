package ru.aiforge.riftworld.item;

import java.util.function.Consumer;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** «Крюк-серп стража» - the guards' hooked claw-sickle (dropped by them sometimes). */
public class GuardSickleItem extends SwordItem implements GlbItem {
    public GuardSickleItem(Properties p) {
        super(RiftTiers.EMBER_IRON, 5, -2.2F, p);
    }

    @Override
    public String glb() {
        return "guard_sickle";
    }

    @Override
    public float length() {
        return 1.25F;
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.Level level,
                                java.util.List<net.minecraft.network.chat.Component> tip, net.minecraft.world.item.TooltipFlag flag) {
        tip.add(net.minecraft.network.chat.Component.translatable("item.riftworld.guard_sickle.combo").withStyle(net.minecraft.ChatFormatting.RED));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ru.aiforge.riftworld.client.ComboClient.ext(false));
    }

}
