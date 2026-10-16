package ru.aiforge.riftworld.arena;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
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

    private static int buildTemple(CommandContext<CommandSourceStack> c, net.minecraft.core.BlockPos at) {
        CommandSourceStack src = c.getSource();
        ServerLevel level = src.getLevel();
        net.minecraft.core.BlockPos target = at != null ? at : net.minecraft.core.BlockPos.containing(src.getPosition());
        java.util.Optional<net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate> tpl =
                level.getServer().getStructureManager().get(new net.minecraft.resources.ResourceLocation(RiftWorld.MODID, "city/temple_great"));
        if (tpl.isEmpty()) {
            src.sendFailure(Component.literal("шаблон city/temple_great не найден"));
            return 0;
        }
        net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings s =
                new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings()
                        .setRotation(net.minecraft.world.level.block.Rotation.NONE)
                        .setIgnoreEntities(true);
        // центр храма (40, *, 47), пол собора на nbt-y=30 -> ставим пол под ноги игрока
        net.minecraft.core.BlockPos origin = new net.minecraft.core.BlockPos(target.getX() - 40, target.getY() - 30, target.getZ() - 47);
        tpl.get().placeInWorld(level, origin, origin, s, level.getRandom(), 2);
        String line = "Великий Храм Перехода возведён: центр " + target.getX() + " " + target.getY() + " " + target.getZ()
                + " (ризница/склеп уходят на 30 блоков вниз)";
        src.sendSuccess(() -> Component.literal(line), true);
        return 1;
    }

    private static int repairBridges(CommandContext<CommandSourceStack> c) {
        CommandSourceStack src = c.getSource();
        ServerLevel level = src.getLevel();
        net.minecraft.core.BlockPos p = net.minecraft.core.BlockPos.containing(src.getPosition());
        int decks = 0, planks = 0;
        for (int x = p.getX() - 32; x <= p.getX() + 32; ++x)
            for (int y = p.getY() - 16; y <= p.getY() + 16; ++y)
                for (int z = p.getZ() - 32; z <= p.getZ() + 32; ++z) {
                    net.minecraft.core.BlockPos bp = new net.minecraft.core.BlockPos(x, y, z);
                    if (!(level.getBlockEntity(bp) instanceof ru.aiforge.riftworld.block.BridgePostBE be)) continue;
                    ru.aiforge.riftworld.bridge.BridgeDeckGeom.Deck deck = be.deck();
                    if (deck == null) continue;
                    if (!bp.equals(deck.master())) continue;
                    ++decks;
                    for (net.minecraft.core.BlockPos cell : deck.plankCells()) {
                        net.minecraft.world.level.block.state.BlockState cur = level.getBlockState(cell);
                        if (cur.isAir() || cur.canBeReplaced()) {
                            level.setBlock(cell, ru.aiforge.riftworld.registry.BridgeBlocks.WALK.get().defaultBlockState(), 3);
                            ++planks;
                        }
                    }
                }
        String line = "мостов восстановлено: " + decks + ", настил-ячеек: " + planks;
        src.sendSuccess(() -> Component.literal(line), true);
        return decks;
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        // /rifttemple [x y z] - возвести Великий Храм Перехода (шаблон city/temple_great) в любой точке любого мира.
        // Спасает старые миры, где город уже сгенерировался без храма.
        d.register(Commands.literal("rifttemple").requires(s -> s.hasPermission(2))
                .executes(c -> buildTemple(c, null))
                .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("y", IntegerArgumentType.integer())
                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                        .executes(c -> buildTemple(c, new net.minecraft.core.BlockPos(
                                                IntegerArgumentType.getInteger(c, "x"),
                                                IntegerArgumentType.getInteger(c, "y"),
                                                IntegerArgumentType.getInteger(c, "z"))))))));
        // /riftbridge repair - перестелить настилы всех верёвочных мостов рядом (лечит «мост не собрался»)
        d.register(Commands.literal("riftbridge").requires(s -> s.hasPermission(2))
                .then(Commands.literal("repair").executes(ArenaCommand::repairBridges)));
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
