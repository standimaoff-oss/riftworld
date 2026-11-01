package ru.aiforge.riftworld.net;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Сетевой пакет синхронизации кастомного бесшовного ритуала портала (Обычный мир -> Элисар).
 */
public record PortalRitualMsg(int phase, BlockPos center, int elapsed) {
    public static void encode(PortalRitualMsg m, FriendlyByteBuf b) {
        b.writeVarInt(m.phase);
        b.writeBlockPos(m.center);
        b.writeVarInt(m.elapsed);
    }

    public static PortalRitualMsg decode(FriendlyByteBuf b) {
        return new PortalRitualMsg(b.readVarInt(), b.readBlockPos(), b.readVarInt());
    }

    public static void handle(PortalRitualMsg m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ru.aiforge.riftworld.client.PortalRitualClient.onMsg(m)));
        ctx.get().setPacketHandled(true);
    }
}
