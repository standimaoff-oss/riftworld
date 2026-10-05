package ru.aiforge.riftworld.net;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import ru.aiforge.riftworld.item.WeaponCombo;

/** Client -> server: the player started combo step {@code step} (0..2) of the weapon in the main hand. */
public record ComboMsg(int step) {
    public void encode(FriendlyByteBuf b) {
        b.writeByte(step);
    }

    public static ComboMsg decode(FriendlyByteBuf b) {
        return new ComboMsg(b.readByte());
    }

    public static void handle(ComboMsg m, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sp = ctx.get().getSender();
        if (sp != null) WeaponCombo.onStep(sp, m.step());
        ctx.get().setPacketHandled(true);
    }
}
