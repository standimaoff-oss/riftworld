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

    private static int shot(CommandContext<CommandSourceStack> c, String spot) {
        CommandSourceStack src = c.getSource();
        if (src.getLevel().isClientSide()) {
            ru.aiforge.riftworld.client.ShotTour.scheduleShot(40);
            return 1;
        }
        ServerLevel level = src.getLevel();
        net.minecraft.server.level.ServerPlayer sp = src.getPlayer();
        if (sp == null) {
            src.sendFailure(Component.literal("нужен игрок"));
            return 0;
        }
        double x = sp.getX(), y = sp.getY(), z = sp.getZ();
        float yaw = sp.getYRot(), pitch = sp.getXRot();
        long time = 1000;
        switch (spot) {
            case "temple" -> { x = 0; y = 107; z = -74; yaw = 0; pitch = -8; time = 12500; }
            case "city" -> { x = 0; y = 110; z = -30; yaw = 0; pitch = -14; }
            case "ocean" -> { x = 210; y = 63.5; z = 210; yaw = 315; pitch = 0; }
            case "bridge" -> { x = 84; y = 107.5; z = -66; yaw = 308; pitch = -5; time = 9000; }
            case "gardens" -> {
                ru.aiforge.riftworld.world.WaterfallGardens.Region rg = nearestGardens(src.getPosition());
                if (rg == null) {
                    src.sendFailure(Component.literal("сады не найдены рядом"));
                    return 0;
                }
                x = rg.cx + 130; y = 66; z = rg.cz + 130; yaw = 45; pitch = -18;
            }
            default -> { return 0; }
        }
        sp.teleportTo(level, x, y, z, yaw, pitch);
        level.setDayTime(time);
        return 1;
    }

    private static ru.aiforge.riftworld.world.WaterfallGardens.Region nearestGardens(net.minecraft.world.phys.Vec3 p) {
        int sx = Math.floorDiv((int) Math.floor(p.x), ru.aiforge.riftworld.world.WaterfallGardens.SUPER);
        int sz = Math.floorDiv((int) Math.floor(p.z), ru.aiforge.riftworld.world.WaterfallGardens.SUPER);
        ru.aiforge.riftworld.world.WaterfallGardens.Region best = null;
        double bd = Double.MAX_VALUE;
        for (int dx = -3; dx <= 3; ++dx)
            for (int dz = -3; dz <= 3; ++dz) {
                ru.aiforge.riftworld.world.WaterfallGardens.Region rg = ru.aiforge.riftworld.world.WaterfallGardens.region(sx + dx, sz + dz);
                if (rg == null) continue;
                double dd = Math.hypot(rg.cx - p.x, rg.cz - p.z);
                if (dd < bd) { bd = dd; best = rg; }
            }
        return best;
    }

    private static int biome(CommandContext<CommandSourceStack> c, int kind) {
        CommandSourceStack src = c.getSource();
        net.minecraft.world.phys.Vec3 p = src.getPosition();
        int bestX = 0, bestZ = 0;
        double best = Double.MAX_VALUE;
        if (kind == 4) {
            bestX = (int) ru.aiforge.riftworld.world.AshenLands.CX;
            bestZ = (int) ru.aiforge.riftworld.world.AshenLands.CZ;
            best = Math.hypot(bestX - p.x, bestZ - p.z);
        } else {
            int superCell = kind == 0 ? ru.aiforge.riftworld.world.WaterfallGardens.SUPER
                    : kind == 1 ? ru.aiforge.riftworld.world.VioletHills.SUPER
                    : kind == 2 ? ru.aiforge.riftworld.world.SizoGrove.SUPER
                    : ru.aiforge.riftworld.world.MistKeep.SUPER;
            int sx = Math.floorDiv((int) Math.floor(p.x), superCell), sz = Math.floorDiv((int) Math.floor(p.z), superCell);
            for (int dx = -3; dx <= 3; ++dx)
                for (int dz = -3; dz <= 3; ++dz) {
                    int cx, cz;
                    if (kind == 0) {
                        ru.aiforge.riftworld.world.WaterfallGardens.Region rg = ru.aiforge.riftworld.world.WaterfallGardens.region(sx + dx, sz + dz);
                        if (rg == null) continue;
                        cx = rg.cx; cz = rg.cz;
                    } else if (kind == 1) {
                        ru.aiforge.riftworld.world.VioletHills.Region rg = ru.aiforge.riftworld.world.VioletHills.region(sx + dx, sz + dz);
                        if (rg == null) continue;
                        cx = rg.cx; cz = rg.cz;
                    } else if (kind == 2) {
                        ru.aiforge.riftworld.world.SizoGrove.Region rg = ru.aiforge.riftworld.world.SizoGrove.region(sx + dx, sz + dz);
                        if (rg == null) continue;
                        cx = rg.cx; cz = rg.cz;
                    } else {
                        ru.aiforge.riftworld.world.MistKeep.Region rg = ru.aiforge.riftworld.world.MistKeep.region(sx + dx, sz + dz);
                        if (rg == null) continue;
                        cx = rg.cx; cz = rg.cz;
                    }
                    double dd = Math.hypot(cx - p.x, cz - p.z);
                    if (dd < best) { best = dd; bestX = cx; bestZ = cz; }
                }
        }
        if (best == Double.MAX_VALUE) {
            src.sendFailure(Component.literal("регион не найден в радиусе 3 супер-ячеек"));
            return 0;
        }
        String[] names = {"Висячие сады", "Фиолетовые холмы", "Роща Сизо", "Туманная обитель", "Пепельные земли"};
        int fx = bestX, fz = bestZ;
        double fd = best;
        src.sendSuccess(() -> Component.literal(names[kind] + ": центр " + fx + " " + fz + " (~" + (int) fd + " блоков)"), false);
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
        // /riftdragon - телепорт к морской арене дракона-оригами (и дракон там появится)
        d.register(Commands.literal("riftdragon").requires(s2 -> s2.hasPermission(2)).executes(c -> {
            CommandSourceStack src = c.getSource();
            if (src.getLevel().isClientSide()) return 0;
            ServerLevel level = src.getLevel();
            net.minecraft.server.level.ServerPlayer sp = src.getPlayer();
            if (sp == null) return 0;
            sp.teleportTo(level, ru.aiforge.riftworld.world.DragonArena.AX + 34, level.getLevelData().getDayTime() >= 0 ? 78 : 78,
                    ru.aiforge.riftworld.world.DragonArena.AZ - 34, 135, -12);
            src.sendSuccess(() -> Component.literal("арена дракона: " + (int) ru.aiforge.riftworld.world.DragonArena.AX
                    + " " + (int) ru.aiforge.riftworld.world.DragonArena.AZ + " (дракон прилетит сам)"), false);
            return 1;
        }));
        // /riftshot <temple|city|ocean|bridge|gardens|tour> - телепорт в видовую точку + игровой скриншот в <gameDir>/riftshots
        d.register(Commands.literal("riftshot").requires(s2 -> s2.hasPermission(2))
                .then(Commands.literal("temple").executes(c -> shot(c, "temple")))
                .then(Commands.literal("city").executes(c -> shot(c, "city")))
                .then(Commands.literal("ocean").executes(c -> shot(c, "ocean")))
                .then(Commands.literal("bridge").executes(c -> shot(c, "bridge")))
                .then(Commands.literal("gardens").executes(c -> shot(c, "gardens")))
                .then(Commands.literal("tour").executes(c -> {
                    if (c.getSource().getLevel().isClientSide()) {
                        ru.aiforge.riftworld.client.ShotTour.startTour();
                        c.getSource().sendSuccess(() -> Component.literal("тур запущен: 5 кадров в <gameDir>/riftshots"), false);
                        return 1;
                    }
                    return 0;
                })));
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
