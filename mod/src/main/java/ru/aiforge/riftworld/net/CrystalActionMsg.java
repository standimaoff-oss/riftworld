package ru.aiforge.riftworld.net;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import ru.aiforge.riftworld.item.CrystalAegisItem;
import ru.aiforge.riftworld.item.CrystalBarrageItem;

/**
 * Сетевой пакет ЛКМ для одноразовых кристальных предметов:
 * - {@link CrystalBarrageItem}: мгновенный выстрел 1 кристаллом по прицелу;
 * - {@link CrystalAegisItem}: запуск перед игроком таранного щита, отбрасывающего всё живое.
 */
public record CrystalActionMsg(int action) {
    public static final int LEFT_CLICK_MAIN = 0;

    public static void encode(CrystalActionMsg m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.action);
    }

    public static CrystalActionMsg decode(FriendlyByteBuf buf) {
        return new CrystalActionMsg(buf.readVarInt());
    }

    public static void handle(CrystalActionMsg m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ServerPlayer sp = ctx.getSender();
        if (sp != null && sp.isAlive() && !sp.isSpectator() && m.action == LEFT_CLICK_MAIN) {
            ItemStack st = sp.getItemInHand(InteractionHand.MAIN_HAND);
            if (st.getItem() instanceof CrystalBarrageItem barrage) {
                barrage.fireSingleLeftClick(sp, st);
            } else if (st.getItem() instanceof CrystalAegisItem aegis) {
                aegis.launchRamLeftClick(sp, st);
            }
        }
        ctx.setPacketHandled(true);
    }
}
