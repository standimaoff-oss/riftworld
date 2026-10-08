package ru.aiforge.riftworld.net;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server -&gt; client: the state of a rift altar. The client needs it for everything the rift does to the view -
 * the black rift itself, the darkness inside the hall, the slow motion of the air and the camera rumble:
 * <ul>
 * <li>{@code state} 0 idle, 1 charging, 2 shattering, 3 open;</li>
 * <li>{@code stage} glow stage 0..3; {@code size} 0..100 how wide the rift has been torn open;</li>
 * <li>{@code shake} camera rumble in ticks; {@code flash} 1 = white flash (crossing over).</li>
 * </ul>
 */
public record RiftFxMsg(BlockPos pos, int state, int stage, int shake, int flash, int size) {
    public void encode(FriendlyByteBuf b) {
        b.writeBlockPos(pos);
        b.writeByte(state);
        b.writeByte(stage);
        b.writeByte(Math.min(200, shake));
        b.writeByte(flash);
        b.writeByte(Math.max(0, Math.min(100, size)));
    }

    public static RiftFxMsg decode(FriendlyByteBuf b) {
        return new RiftFxMsg(b.readBlockPos(), b.readByte(), b.readByte(), b.readByte(), b.readByte(), b.readByte());
    }

    public static void handle(RiftFxMsg m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ru.aiforge.riftworld.client.RiftFx.onMsg(m)));
        ctx.get().setPacketHandled(true);
    }
}
