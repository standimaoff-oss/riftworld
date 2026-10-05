package ru.aiforge.riftworld.arena;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import java.util.ArrayList;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;

/** /riftarena status|start|reset - operator tool for the throne hall sequence (loaded arenas in the caller's dimension). */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ArenaCommand {
    private ArenaCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(Commands.literal("riftarena").requires(s -> s.hasPermission(2))
                .then(Commands.literal("status").executes(c -> run(c, 0)))
                .then(Commands.literal("start").executes(c -> run(c, 1)))
                .then(Commands.literal("reset").executes(c -> run(c, 2))));
        // /riftruins - nearest Smouldering Ruins site (chapel) to the caller
        d.register(Commands.literal("riftruins").requires(s -> s.hasPermission(2)).executes(c -> {
            net.minecraft.world.phys.Vec3 p = c.getSource().getPosition();
            ru.aiforge.riftworld.world.EmberRuins.Site st = ru.aiforge.riftworld.world.EmberRuins.nearest(p.x, p.z);
            if (st == null) {
                c.getSource().sendFailure(Component.literal("no ruins nearby"));
                return 0;
            }
            String line = "ruins at " + st.cx + " " + st.y + " " + st.cz + " chapel origin " + st.x0 + " " + st.y + " " + st.z0 + " guards " + st.guards;
            c.getSource().sendSuccess(() -> Component.literal(line), false);
            return 1;
        }));
    }

    private static int run(CommandContext<CommandSourceStack> c, int op) {
        ServerLevel sl = c.getSource().getLevel();
        int n = 0;
        for (ArenaCoreBlockEntity a : new ArrayList<>(ArenaCoreBlockEntity.LOADED)) {
            if (a.getLevel() != sl || a.isRemoved()) continue;
            ++n;
            if (op == 1 && a.state() == ArenaCoreBlockEntity.State.DORMANT) a.start(sl, a.players(sl));
            if (op == 2) a.reset(sl);
            int up = 0, broken = 0;
            for (BossColumnBlockEntity col : a.columns(sl)) {
                if (col.standing()) ++up;
                if (col.shattered()) ++broken;
            }
            StringBuilder sb = new StringBuilder("arena " + a.getBlockPos().toShortString() + ": " + a.state() + ", columns standing " + up + ", broken " + broken);
            for (ru.aiforge.riftworld.entity.MorgrathBoss b : sl.getEntitiesOfClass(ru.aiforge.riftworld.entity.MorgrathBoss.class,
                    new net.minecraft.world.phys.AABB(a.getBlockPos()).inflate(48))) sb.append("; ").append(b.debugLine());
            String line = sb.toString();
            c.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        if (n == 0) c.getSource().sendFailure(Component.literal("no loaded arena in this dimension"));
        return n;
    }
}
