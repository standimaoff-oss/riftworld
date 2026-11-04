package ru.aiforge.riftworld.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import ru.aiforge.riftworld.flight.PortalRitual;

public class ResonanceStoneItem extends Item {
    public ResonanceStoneItem(Properties props) {
        super(props);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos riftBase = ctx.getClickedPos().above();
        if (ctx.getPlayer() instanceof ServerPlayer sp) {
            if (PortalRitual.start(sp, riftBase, "resonance")) {
                if (!sp.getAbilities().instabuild) {
                    ctx.getItemInHand().shrink(1);
                }
                return InteractionResult.CONSUME;
            }
        }
        return level.isClientSide ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.riftworld.resonance_stone.desc").withStyle(ChatFormatting.DARK_AQUA));
    }
}
