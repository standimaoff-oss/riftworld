package ru.aiforge.riftworld.world;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;

/**
 * The first time a player arrives on Elisar he receives an ordinary compass bound to the castle in the Ashen Lands
 * (vanilla lodestone-tracking tags without a lodestone), so the castle, the boss and the biome can be found.
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID)
public final class CastleCompass {
    private static final String TAG = "RiftCastleCompass", RUINS = "RiftRuinsMode";

    private CastleCompass() {
    }

    public static ItemStack make() {
        ItemStack s = new ItemStack(Items.COMPASS);
        CompoundTag t = s.getOrCreateTag();
        t.putString("LodestoneDimension", RiftWorld.MODID + ":elisar");
        t.put("LodestonePos", NbtUtils.writeBlockPos(new BlockPos(CastleSite.CX, CastleSite.Y + 1, CastleSite.Z0)));
        t.putBoolean("LodestoneTracked", false);
        t.putBoolean(TAG, true);
        s.setHoverName(Component.translatable("item.riftworld.castle_compass").withStyle(ChatFormatting.GOLD));
        return s;
    }

    /** Sneak + right click toggles the needle between the castle and the nearest Smouldering Ruins. */
    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem e) {
        ItemStack s = e.getItemStack();
        Player p = e.getEntity();
        if (!s.is(Items.COMPASS) || !s.hasTag() || !s.getTag().getBoolean(TAG) || !p.isShiftKeyDown()) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        if (p.level().isClientSide) return;
        CompoundTag t = s.getTag();
        if (!t.getBoolean(RUINS)) {
            EmberRuins.Site site = EmberRuins.nearest(p.getX(), p.getZ());
            if (site == null) {
                p.displayClientMessage(Component.translatable("riftworld.compass.none").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            t.put("LodestonePos", NbtUtils.writeBlockPos(new BlockPos(site.cx, site.y + 1, site.cz)));
            t.putBoolean(RUINS, true);
            p.displayClientMessage(Component.translatable("riftworld.compass.ruins", site.cx, site.cz).withStyle(ChatFormatting.GOLD), true);
        } else {
            t.put("LodestonePos", NbtUtils.writeBlockPos(new BlockPos(CastleSite.CX, CastleSite.Y + 1, CastleSite.Z0)));
            t.putBoolean(RUINS, false);
            p.displayClientMessage(Component.translatable("riftworld.compass.castle").withStyle(ChatFormatting.GOLD), true);
        }
        t.putString("LodestoneDimension", RiftWorld.MODID + ":elisar");
        p.playSound(net.minecraft.sounds.SoundEvents.LODESTONE_COMPASS_LOCK, 1.0F, 1.0F);
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent e) {
        ItemStack s = e.getItemStack();
        if (s.is(Items.COMPASS) && s.hasTag() && s.getTag().getBoolean(TAG))
            e.getToolTip().add(Component.translatable("item.riftworld.castle_compass.tip").withStyle(ChatFormatting.DARK_GRAY));
    }

    // Рев 14.26: компас больше НЕ выдаётся при входе в мир (владелец: «убрать компас при попадании
    // в мир»). Предмет остаётся работающим для тех, кто уже держит его в инвентаре, но новый игрок
    // приходит в Элисар без компаса.
}
