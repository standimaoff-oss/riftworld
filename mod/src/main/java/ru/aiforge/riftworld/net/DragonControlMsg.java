package ru.aiforge.riftworld.net;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import ru.aiforge.riftworld.entity.PaperDragonEntity;

/**
 * Клиентский пакет управления полётом на оседланном Бумажном Драконе:
 * W/S — разгон от 0 до 2x скорости элитр с фейерверками, мышь — курс/тангаж как на элитрах,
 * A/D — точная регулировка поворота по градусам.
 */
public record DragonControlMsg(byte throttleInput, byte yawDegInput, float aimYaw, float aimPitch) {
    public static void encode(DragonControlMsg m, FriendlyByteBuf buf) {
        buf.writeByte(m.throttleInput);
        buf.writeByte(m.yawDegInput);
        buf.writeFloat(m.aimYaw);
        buf.writeFloat(m.aimPitch);
    }

    public static DragonControlMsg decode(FriendlyByteBuf buf) {
        return new DragonControlMsg(buf.readByte(), buf.readByte(), buf.readFloat(), buf.readFloat());
    }

    public static void handle(DragonControlMsg m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ServerPlayer sp = ctx.getSender();
        if (sp != null && sp.getVehicle() instanceof PaperDragonEntity dragon && dragon.isOwnedBy(sp)) {
            dragon.applyRiderInput(m.throttleInput, m.yawDegInput, m.aimYaw, m.aimPitch);
        }
        ctx.setPacketHandled(true);
    }
}
