package ru.aiforge.riftworld.arena;

import com.mojang.brigadier.CommandDispatcher;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.block.BridgePostBE;
import ru.aiforge.riftworld.block.BridgePostBlock;
import ru.aiforge.riftworld.bridge.BridgeDeckGeom;
import ru.aiforge.riftworld.registry.BridgeBlocks;

/**
 * /rifttreescan [radius] [yMin] [yMax] - counts the mod's own logs and leaves in the LOADED chunks around the caller
 * and prints a few sample positions.
 *
 * <p>Why it exists: proving "the biome has trees" from outside is unreliable. `/locate biome` returns any point inside a
 * biome blob, while the trees only grow on the islands that the generator actually decorates - a scan around a random
 * biome point can be 100+ blocks away from the nearest tree and honestly report zero. This command reads the real world
 * blocks of the real chunks, so a playtest run can say "800 lilac leaves in these chunks, first trunk at x y z" instead
 * of guessing. It never modifies the world and works both for our own biomes and for plain {@code minecraft:plains}.
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TreeScanCommand {
    private TreeScanCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(Commands.literal("rifttreescan").requires(s -> s.hasPermission(2))
                .executes(c -> scan(c.getSource(), 64, 60, 170))
                .then(Commands.argument("radius", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 512))
                        .executes(c -> scan(c.getSource(),
                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "radius"), 60, 170))
                        .then(Commands.argument("ymin", com.mojang.brigadier.arguments.IntegerArgumentType.integer(-64, 319))
                                .then(Commands.argument("ymax", com.mojang.brigadier.arguments.IntegerArgumentType.integer(-64, 319))
                                        .executes(c -> scan(c.getSource(),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "radius"),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "ymin"),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "ymax")))))));
        // /riftbridgebuild <длина> [ширина] [стиль] - собрать мост ровно там, где стоит вызывающий.
        // Нужен и админам, и проверке: стиль настила зависит от измерения, а в обычной игре мост
        // собирается вручную из четырёх опор и верёвки - командой это делается за один шаг.
        d.register(Commands.literal("riftbridgebuild").requires(s2 -> s2.hasPermission(2))
                .then(Commands.argument("len", IntegerArgumentType.integer(4, BridgeDeckGeom.MAX_LEN))
                        .executes(c -> buildBridge(c.getSource(), IntegerArgumentType.getInteger(c, "len"), 2,
                                autoStyle(c.getSource())))
                        .then(Commands.argument("width", IntegerArgumentType.integer(1, BridgeDeckGeom.MAX_WID))
                                .executes(c -> buildBridge(c.getSource(), IntegerArgumentType.getInteger(c, "len"),
                                        IntegerArgumentType.getInteger(c, "width"), autoStyle(c.getSource())))
                                .then(Commands.argument("style", IntegerArgumentType.integer(0, 4))
                                        .executes(c -> buildBridge(c.getSource(),
                                                IntegerArgumentType.getInteger(c, "len"),
                                                IntegerArgumentType.getInteger(c, "width"),
                                                IntegerArgumentType.getInteger(c, "style")))))));
        // /riftblocks <radius> [ymin] [ymax] - все блоки мода в загруженных чанках (мосты, храм, арена, деревья)
        d.register(Commands.literal("riftblocks").requires(s2 -> s2.hasPermission(2))
                .then(Commands.argument("radius", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 512))
                        .executes(c -> count(c.getSource(),
                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "radius"), 40, 200,
                                false))
                        .then(Commands.argument("ymin", com.mojang.brigadier.arguments.IntegerArgumentType.integer(-64, 319))
                                .then(Commands.argument("ymax", com.mojang.brigadier.arguments.IntegerArgumentType.integer(-64, 319))
                                        .executes(c -> count(c.getSource(),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "radius"),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "ymin"),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "ymax"),
                                                false))))));
        // /riftbridgeplan [radius] - какие мосты генератор собирается поставить вокруг (без чтения мира)
        d.register(Commands.literal("riftbridgeplan").requires(s2 -> s2.hasPermission(2))
                .executes(c -> bridgePlan(c.getSource(), 150))
                .then(Commands.argument("radius", com.mojang.brigadier.arguments.IntegerArgumentType.integer(16, 400))
                        .executes(c -> bridgePlan(c.getSource(),
                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "radius")))));
        // /riftprobe - what the biome/gen code thinks about the caller's column (all four biome providers at once)
        d.register(Commands.literal("riftprobe").requires(s2 -> s2.hasPermission(2)).executes(c -> {
            CommandSourceStack st = c.getSource();
            int x = (int) Math.floor(st.getPosition().x), z = (int) Math.floor(st.getPosition().z);
            StringBuilder sb = new StringBuilder("probe " + x + "," + z + ":");
            sb.append(" wg.inside=").append(ru.aiforge.riftworld.world.WaterfallGardens.inside(x + 0.5, z + 0.5));
            sb.append(" wg.region=").append(centre(ru.aiforge.riftworld.world.WaterfallGardens.regionAt(x + 0.5, z + 0.5)));
            sb.append(" vh.inside=").append(ru.aiforge.riftworld.world.VioletHills.inside(x + 0.5, z + 0.5));
            sb.append(" sg.inside=").append(ru.aiforge.riftworld.world.SizoGrove.inside(x + 0.5, z + 0.5));
            sb.append(" mk.inside=").append(ru.aiforge.riftworld.world.MistKeep.inside(x + 0.5, z + 0.5));
            String line = sb.toString();
            st.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }));
        // /rifttreeplan <vh|wg|sg|mk|wi> [radius] - what the generator PLANTS here vs what the world actually holds
        d.register(Commands.literal("rifttreeplan").requires(s -> s.hasPermission(2))
                .then(Commands.argument("biome", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(c -> plan(c.getSource(),
                                com.mojang.brigadier.arguments.StringArgumentType.getString(c, "biome"), 64))
                        .then(Commands.argument("radius", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 256))
                                .executes(c -> plan(c.getSource(),
                                        com.mojang.brigadier.arguments.StringArgumentType.getString(c, "biome"),
                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "radius"))))));
    }

    /** План мостов: те же вычисления, что у генератора чанков, но без записи в мир. */
    private static int bridgePlan(CommandSourceStack src, int radius) {
        int px = (int) Math.floor(src.getPosition().x), pz = (int) Math.floor(src.getPosition().z);
        java.util.Map<String, ru.aiforge.riftworld.bridge.BridgeDeckGeom.Deck> decks = new LinkedHashMap<>();
        int c0x = px >> 4, c0z = pz >> 4, cr = Math.max(1, (radius + 15) >> 4);
        for (int cx = c0x - cr; cx <= c0x + cr; ++cx) {
            for (int cz = c0z - cr; cz <= c0z + cr; ++cz) {
                for (ru.aiforge.riftworld.bridge.BridgeDeckGeom.Deck d
                        : ru.aiforge.riftworld.bridge.WorldBridges.decksNear(cx << 4, cz << 4)) {
                    decks.putIfAbsent(d.a().getX() + "," + d.a().getZ() + "-" + d.b().getX() + "," + d.b().getZ(), d);
                }
            }
        }
        StringBuilder sb = new StringBuilder("bridgeplan r=" + radius + " center=" + px + "," + pz + ": decks=" + decks.size());
        int n = 0;
        for (ru.aiforge.riftworld.bridge.BridgeDeckGeom.Deck d : decks.values()) {
            if (n++ < 4) sb.append("; style=").append(d.style()).append(" ").append(d.a().getX()).append(",")
                    .append(d.a().getZ()).append("->").append(d.b().getX()).append(",").append(d.b().getZ());
        }
        double minW = Double.MAX_VALUE, maxLen = 0;
        java.util.Set<String> mids = new java.util.HashSet<>();
        int dup = 0;
        for (ru.aiforge.riftworld.bridge.BridgeDeckGeom.Deck d : decks.values()) {
            minW = Math.min(minW, d.width());
            maxLen = Math.max(maxLen, d.len());
            String key = Math.round((d.a().getX() + d.b().getX()) / 2.0) + "," + Math.round((d.a().getZ() + d.b().getZ()) / 2.0);
            if (!mids.add(key)) dup++;
        }
        sb.append(" minwidth=").append(minW == Double.MAX_VALUE ? "-" : String.format(java.util.Locale.ROOT, "%.1f", minW))
                .append(" maxlen=").append(String.format(java.util.Locale.ROOT, "%.0f", maxLen)).append(" dup=").append(dup);
        String line = sb.toString();
        src.sendSuccess(() -> Component.literal(line), false);
        return decks.size();
    }

    private static String centre(Object rg) {
        if (rg == null) return "none";
        try {
            java.lang.reflect.Field fx = rg.getClass().getField("cx");
            java.lang.reflect.Field fz = rg.getClass().getField("cz");
            java.lang.reflect.Field fr = rg.getClass().getField("Rr");
            return fx.getInt(rg) + "," + fz.getInt(rg) + " R=" + Math.round(fr.getDouble(rg));
        } catch (ReflectiveOperationException ex) {
            return "?";
        }
    }

    /** the mod's log/leaf family, by registry path. */
    private static boolean isOurLog(net.minecraft.resources.ResourceLocation rl) {
        if (rl == null || !RiftWorld.MODID.equals(rl.getNamespace())) return false;
        String p = rl.getPath();
        return p.endsWith("_log") || p.endsWith("_wood") || p.endsWith("_stem");
    }

    /**
     * Plan vs world: the region planners are pure maths, so we can ask them what they plant around a point and then look
     * for the trunk in the real chunks. This is the check that separates "the biome looks empty because the scan was in
     * the wrong place" from "the generator plans trees but they never reach the world".
     */
    private static int plan(CommandSourceStack src, String biome, int radius) {
        ServerLevel level = src.getLevel();
        int px = (int) Math.floor(src.getPosition().x);
        int pz = (int) Math.floor(src.getPosition().z);
        java.util.Map<Long, ru.aiforge.riftworld.world.CityPlan.Tree> uniq = new LinkedHashMap<>();
        for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dz = -radius; dz <= radius; dz += 2) {
                java.util.List<ru.aiforge.riftworld.world.CityPlan.Tree> near = switch (biome) {
                    case "vh" -> ru.aiforge.riftworld.world.VioletHills.treesNear(px + dx, pz + dz);
                    case "wg" -> ru.aiforge.riftworld.world.WaterfallGardens.treesNear(px + dx, pz + dz);
                    case "sg" -> ru.aiforge.riftworld.world.SizoGrove.treesNear(px + dx, pz + dz);
                    case "mk" -> ru.aiforge.riftworld.world.MistKeep.treesNear(px + dx, pz + dz);
                    case "wi" -> ru.aiforge.riftworld.world.WildIslands.treesNear(px + dx, pz + dz);
                    case "al" -> ru.aiforge.riftworld.world.AshenLands.treesNear(px + dx, pz + dz);
                    default -> java.util.List.of();
                };
                for (ru.aiforge.riftworld.world.CityPlan.Tree t : near) {
                    uniq.putIfAbsent(((long) t.x << 32) ^ (t.z & 0xffffffffL), t);
                }
            }
        }
        int inWorld = 0, unloaded = 0;
        java.util.List<String> samples = new ArrayList<>();
        for (ru.aiforge.riftworld.world.CityPlan.Tree t : uniq.values()) {
            if (!level.hasChunk(t.x >> 4, t.z >> 4)) {
                ++unloaded;
                continue;
            }
            String hit = null;
            for (int y = 50; y <= 190 && hit == null; ++y) {
                BlockState st = level.getBlockState(new net.minecraft.core.BlockPos(t.x, y, t.z));
                if (isOurLog(id(st.getBlock()))) hit = y + " " + id(st.getBlock()).getPath();
            }
            if (hit != null) {
                ++inWorld;
                if (samples.size() < 4) samples.add(t.x + " " + hit.replace(' ', ' ') + " " + t.z);
            } else if (samples.size() < 4) {
                samples.add("MISSING " + t.x + " " + t.z + " kind " + t.kind);
            }
        }
        String reg = switch (biome) {
            case "vh" -> centre(ru.aiforge.riftworld.world.VioletHills.regionAt(px + 0.5, pz + 0.5));
            case "wg" -> centre(ru.aiforge.riftworld.world.WaterfallGardens.regionAt(px + 0.5, pz + 0.5));
            case "sg" -> centre(ru.aiforge.riftworld.world.SizoGrove.regionAt(px + 0.5, pz + 0.5));
            case "mk" -> centre(ru.aiforge.riftworld.world.MistKeep.regionAt(px + 0.5, pz + 0.5));
            default -> "n/a";
        };
        String head = "treeplan " + biome + " r=" + radius + " center=" + px + "," + pz + " region_at_center=" + reg
                + ": planned=" + uniq.size() + " in_world=" + inWorld
                + (unloaded > 0 ? " unloaded_cols=" + unloaded : "");
        StringBuilder sb = new StringBuilder(head);
        for (String s : samples) sb.append("; ").append(s);
        src.sendSuccess(() -> Component.literal(sb.toString()), false);
        return inWorld;
    }

    /** registry name of a block, or null when it is not ours (cached in the registry itself, no global state needed). */
    private static ResourceLocation id(Block b) {
        return ForgeRegistries.BLOCKS.getKey(b);
    }

    /** Style for the place the caller stands in: our own biomes and the vanilla Nether (burnt) are mapped in
     *  {@link BridgeDeckGeom#styleFor(String)}, so «ад = сгоревшие» works for any dimension, not just ours. */
    private static int autoStyle(CommandSourceStack src) {
        String path = src.getLevel().getBiome(BlockPos.containing(src.getPosition()))
                .unwrapKey().map(k -> k.location().getPath()).orElse("");
        return BridgeDeckGeom.styleFor(path);
    }

    /** Builds a real four-post bridge along +X (span) and +Z (width), with deck cells and both BE links. */
    private static int buildBridge(CommandSourceStack src, int len, int width, int style) {
        ServerLevel level = src.getLevel();
        BlockPos a = BlockPos.containing(src.getPosition());
        BlockPos b = a.offset(len - 1, 0, 0);
        BlockPos a2 = a.offset(0, 0, width - 1);
        BlockPos b2 = b.offset(0, 0, width - 1);
        BlockState post = BridgeBlocks.POST.get().defaultBlockState().setValue(BridgePostBlock.STYLE, style);
        for (BlockPos p : new BlockPos[]{a, b, a2, b2}) level.setBlock(p, post, 3);
        BridgeDeckGeom.Deck deck = new BridgeDeckGeom.Deck(a, b, a2, b2, style, 0F, 0F);
        BridgePostBlock.placeCells(level, deck);
        for (BlockPos cp : deck.corners())
            if (level.getBlockEntity(cp) instanceof BridgePostBE be) be.stand(deck);
        src.sendSuccess(() -> Component.literal("bridge built: style=" + style + " len=" + len + " width=" + width
                + " sag=" + String.format(java.util.Locale.ROOT, "%.2f", deck.sag())
                + " planks=" + deck.plankCount() + " cells=" + deck.collisionCells().size()
                + " at " + a.getX() + " " + a.getY() + " " + a.getZ()), false);
        return 1;
    }

    private static int scan(CommandSourceStack src, int radius, int yMin, int yMax) {
        return count(src, radius, yMin, yMax, true);
    }

    /** @param onlyWood true — только брёвна/листва, false — все блоки мода (мосты, храм, арена…) */
    private static int count(CommandSourceStack src, int radius, int yMin, int yMax, boolean onlyWood) {
        ServerLevel level = src.getLevel();
        int px = (int) Math.floor(src.getPosition().x);
        int pz = (int) Math.floor(src.getPosition().z);
        int c0x = px >> 4, c0z = pz >> 4;
        int cr = Math.max(1, (radius + 15) >> 4);
        int lo = Math.max(yMin, level.getMinBuildHeight());
        int hi = Math.min(yMax, level.getMaxBuildHeight() - 1);

        Map<String, Integer> counts = new LinkedHashMap<>();
        List<String> samples = new ArrayList<>();
        int chunks = 0;
        for (int cx = c0x - cr; cx <= c0x + cr; ++cx) {
            for (int cz = c0z - cr; cz <= c0z + cr; ++cz) {
                if (!level.hasChunk(cx, cz)) continue;
                LevelChunk ch = level.getChunk(cx, cz);
                ++chunks;
                for (int x = cx << 4; x < (cx << 4) + 16; ++x) {
                    for (int z = cz << 4; z < (cz << 4) + 16; ++z) {
                        for (int y = lo; y <= hi; ++y) {
                            BlockState st = ch.getBlockState(new net.minecraft.core.BlockPos(x, y, z));
                            if (st.isAir()) continue;
                            ResourceLocation rl = id(st.getBlock());
                            if (rl == null || !RiftWorld.MODID.equals(rl.getNamespace())) continue;
                            String p = rl.getPath();
                            boolean log = isOurLog(rl);
                            boolean leaf = p.endsWith("_leaves") || p.endsWith("_blossom") || p.endsWith("_fronds");
                            if (onlyWood && !log && !leaf) continue;
                            String key = rl.toString();
                            counts.merge(key, 1, Integer::sum);
                            if (samples.size() < (onlyWood ? 8 : 0) && log) samples.add(key + " @" + x + " " + y + " " + z);
                        }
                    }
                }
            }
        }
        String head = (onlyWood ? "treescan" : "blockscan") + " r=" + radius + " y=" + lo + ".." + hi + " chunks=" + chunks + ":" + (counts.isEmpty() ? " nothing" : "");
        StringBuilder sb = new StringBuilder(head);
        for (Map.Entry<String, Integer> en : counts.entrySet()) sb.append(' ').append(en.getKey().replace(RiftWorld.MODID + ":", "")).append('=').append(en.getValue());
        for (String s : samples) sb.append("; ").append(s);
        src.sendSuccess(() -> Component.literal(sb.toString()), false);
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }
}
