package ru.aiforge.riftworld.net;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import ru.aiforge.riftworld.RiftWorld;

public final class Net {
    private static final String V = "2";
    public static final SimpleChannel CH = NetworkRegistry.newSimpleChannel(new ResourceLocation(RiftWorld.MODID, "main"), () -> V, V::equals, V::equals);

    private Net() {}

    public static void init() {
        CH.messageBuilder(FlightMsg.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FlightMsg::encode).decoder(FlightMsg::decode).consumerMainThread(FlightMsg::handle).add();
        CH.messageBuilder(ComboMsg.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ComboMsg::encode).decoder(ComboMsg::decode).consumerMainThread(ComboMsg::handle).add();
    }

    public static void send(ServerPlayer p, Object msg) {
        CH.send(PacketDistributor.PLAYER.with(() -> p), msg);
    }
}
