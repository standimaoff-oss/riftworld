package ru.aiforge.riftworld.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.block.RiftAltarBlockEntity;
import ru.aiforge.riftworld.world.RiftTravel;
import ru.aiforge.riftworld.world.RiftZone;

/**
 * /riftrift - operator tool of the Rev 5 portal: where the altar of the callers world stands, what state the ritual is
 * in, and force opening / closing the rift (handy for testing the effects without lighting twelve candles).
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RiftCommand {
    private RiftCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(net.minecraft.commands.Commands.literal("riftrift").requires(s -> s.hasPermission(2))
                .executes(RiftCommand::status)
                .then(net.minecraft.commands.Commands.literal("where").executes(RiftCommand::where))
                .then(net.minecraft.commands.Commands.literal("open").executes(c -> force(c, true)))
                .then(net.minecraft.commands.Commands.literal("close").executes(c -> force(c, false))));
    }

    private static BlockPos altarOf(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        if (level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
            BlockPos known = RiftTravel.overworldAltar(p.getServer());
            return known != null ? known : null;
        }
        return RiftZone.elisarAltar();
    }

    private static int where(CommandContext<net.minecraft.commands.CommandSourceStack> c) {
        ServerPlayer p = c.getSource().getPlayer();
        if (p == null) return 0;
        BlockPos pos = altarOf(p);
        c.getSource().sendSuccess(() -> Component.literal(pos == null ? "no altar known in this world yet" :
                "altar of the Rift at " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " (" + p.serverLevel().dimension().location() + ")"), false);
        return 1;
    }

    private static int status(CommandContext<net.minecraft.commands.CommandSourceStack> c) {
        ServerPlayer p = c.getSource().getPlayer();
        if (p == null) return 0;
        BlockPos pos = altarOf(p);
        ServerLevel level = p.serverLevel();
        RiftAltarBlockEntity be = pos != null && level.getBlockEntity(pos) instanceof RiftAltarBlockEntity a ? a : null;
        String state = be == null ? "no altar loaded at " + pos : "state " + be.state() + " stage " + be.stage() + (be.isOpen() ? " OPEN" : "");
        c.getSource().sendSuccess(() -> Component.literal(state), false);
        return 1;
    }

    private static int force(CommandContext<net.minecraft.commands.CommandSourceStack> c, boolean open) {
        ServerPlayer p = c.getSource().getPlayer();
        if (p == null) return 0;
        BlockPos pos = altarOf(p);
        ServerLevel level = p.serverLevel();
        if (pos == null || !(level.getBlockEntity(pos) instanceof RiftAltarBlockEntity be)) {
            c.getSource().sendFailure(Component.literal("no rift altar loaded here: run /riftrift where"));
            return 0;
        }
        if (open) {
            be.forceOpen(level, pos);
        } else {
            be.shutdown();
        }
        c.getSource().sendSuccess(() -> Component.literal(open ? "the rift is open" : "the rift is closed"), false);
        return 1;
    }
}
