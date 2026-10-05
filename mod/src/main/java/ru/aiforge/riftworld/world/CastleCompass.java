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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;

/**
 * The first time a player arrives on Elisar he receives an ordinary compass bound to the castle in the Ashen Lands
 * (vanilla lodestone-tracking tags without a lodestone), so the castle, the boss and the biome can be found.
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID)
public final class CastleCompass {
    private static final String KEY = "riftworld_castle_compass";

    private CastleCompass() {
    }

    public static ItemStack make() {
        ItemStack s = new ItemStack(Items.COMPASS);
        CompoundTag t = s.getOrCreateTag();
        t.putString("LodestoneDimension", RiftWorld.MODID + ":elisar");
        t.put("LodestonePos", NbtUtils.writeBlockPos(new BlockPos(CastleSite.CX, CastleSite.Y + 1, CastleSite.Z0)));
        t.putBoolean("LodestoneTracked", false);
        s.setHoverName(Component.translatable("item.riftworld.castle_compass").withStyle(ChatFormatting.GOLD));
        return s;
    }

    @SubscribeEvent
    public static void onDim(PlayerEvent.PlayerChangedDimensionEvent e) {
        Player p = e.getEntity();
        if (p.level().isClientSide || !e.getTo().location().getPath().equals("elisar")) return;
        CompoundTag data = p.getPersistentData();
        if (data.getBoolean(KEY)) return;
        data.putBoolean(KEY, true);
        if (!p.getInventory().add(make())) p.drop(make(), false);
        p.sendSystemMessage(Component.translatable("riftworld.castle_compass.got").withStyle(ChatFormatting.GOLD));
    }
}
