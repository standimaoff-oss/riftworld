package ru.aiforge.riftworld.net;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server -> client: flight phase, the capsule core position of that phase and ticks elapsed in it. */
public record FlightMsg(int phase, BlockPos pos, int elapsed) {
    public void encode(FriendlyByteBuf b) {
        b.writeVarInt(phase);
        b.writeBlockPos(pos);
        b.writeVarInt(elapsed);
    }

    public static FlightMsg decode(FriendlyByteBuf b) {
        return new FlightMsg(b.readVarInt(), b.readBlockPos(), b.readVarInt());
    }

    public static void handle(FlightMsg m, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ru.aiforge.riftworld.client.FlightClient.onMsg(m));
        ctx.get().setPacketHandled(true);
    }
}
