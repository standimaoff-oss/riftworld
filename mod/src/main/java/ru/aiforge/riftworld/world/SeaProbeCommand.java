package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.flight.Flight;
import ru.aiforge.riftworld.registry.ElisarBlocks;
import ru.aiforge.riftworld.registry.MorokBlocks;

/**
 * Рев 10: операторская команда {@code /riftsea} — самопроверка «живого моря» прямо в игре.
 *
 * <p>Печатает одной строкой то, что проверяет CI, и то, что нельзя увидеть глазами в логе:
 * <ul>
 *   <li>{@code floor=} кромка дна и {@code water=} толщина воды под позицией вызывающего;</li>
 *   <li>{@code min=} самая глубокая кромка дна в радиусе проб (абиссальные впадины);</li>
 *   <li>{@code caves=} колонки с полостями, {@code wet=}/{@code air=} — сколько в них воды и воздуха
 *       (воздушные карманы под сводами);</li>
 *   <li>{@code reef=} колонки с рифовым садом, {@code plants=} блоки кораллов/травы/ламинарии;</li>
 *   <li>{@code caveat=} координаты найденной затопленной пещеры — по ним CI телепортирует камеру.</li>
 * </ul>
 * Считает вокруг вызывающего в радиусе 96 блоков (аргументов нет намеренно: команда должна работать
 * из RCON без разбора аргументов).
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SeaProbeCommand {
    private SeaProbeCommand() {
    }

    private static int majaPose(CommandSourceStack src) {
        StringBuilder why = new StringBuilder();
        boolean clear = ru.aiforge.riftworld.entity.MajaCollision.clear(src.getLevel(), src.getPosition(), src.getRotation().y, why);
        String message = "maja_pose clear=" + clear + " reason=" + (clear ? "none" : why);
        src.sendSuccess(() -> Component.literal(message), false);
        return clear ? 1 : 0;
    }

    /** Read-only diagnostic: never generates or loads chunks. */
    private static int majaIsland(CommandSourceStack src) {
        if (!src.getLevel().dimension().equals(Flight.ELISAR_DIM)) {
            src.sendFailure(Component.literal("Use /execute in riftworld:elisar positioned <x> <y> <z> run riftmajaisland"));
            return 0;
        }
        int ox = (int) Math.floor(src.getPosition().x), oz = (int) Math.floor(src.getPosition().z);
        int cx = Math.floorDiv(ox, WildIslands.CELL), cz = Math.floorDiv(oz, WildIslands.CELL);
        CityPlan.Island best = null;
        double score = Double.POSITIVE_INFINITY;
        for (int x = cx - 7; x <= cx + 7; x++) for (int z = cz - 7; z <= cz + 7; z++) {
            for (CityPlan.Island island : WildIslands.cell(x, z).islands) {
                if (island.kind != 2 || island.R > 24.0 || !DeepOcean.contains((int)island.cx, (int)island.cz)) continue;
                DeepOcean.Region ocean = DeepOcean.regionAt((int)island.cx, (int)island.cz);
                double d = Math.hypot(island.cx - ocean.x(), island.cz - ocean.z());
                // The core suppresses islands, and an island crossing its edge is incomplete.
                if (d < 384 + island.maxReach() * 1.5 + 32 || d + island.maxReach() + 16 >= ocean.radius()) continue;
                if (ElisarChunkGenerator.column(CityPlan.get(), (int)island.cx, (int)island.cz).isl != island) continue;
                double q = Math.hypot(island.cx - ox, island.cz - oz);
                if (q < score) { score = q; best = island; }
            }
        }
        if (best == null) { src.sendFailure(Component.literal("No suitable medium ocean island nearby")); return 0; }
        int ix = (int)Math.floor(best.cx), iz = (int)Math.floor(best.cz);
        int ax = ix, az = (int)Math.floor(best.cz - 156 - best.maxReach() - 20);
        int ay = (int)Math.max(67, Math.min(85, best.base - 38));
        String message = String.format(java.util.Locale.ROOT,
                "maja_island x=%d z=%d radius=%.2f reach=%.2f base=%.2f anchor_x=%d anchor_y=%d anchor_z=%d",
                ix, iz, best.R, best.maxReach(), best.base, ax, ay, az);
        src.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }


    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("riftmajapose").requires(s -> s.hasPermission(2))
                .executes(c -> majaPose(c.getSource())));
        e.getDispatcher().register(Commands.literal("riftmajaisland").requires(s -> s.hasPermission(2))
                .executes(c -> majaIsland(c.getSource())));
        e.getDispatcher().register(Commands.literal("riftmajacheck").requires(s -> s.hasPermission(2))
                .executes(c -> majaCheck(c.getSource())));
        e.getDispatcher().register(Commands.literal("riftsea").requires(s -> s.hasPermission(2))
                .executes(c -> run(c.getSource())));
        // Рев 11: /rifttree — ближайшее дерево Элисара и сколько у него ВЕТВЕЙ (проверка «не квадрат»)
        e.getDispatcher().register(Commands.literal("rifttree").requires(s -> s.hasPermission(2))
                .executes(c -> tree(c.getSource(), null))
                .then(Commands.argument("kind", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(c -> tree(c.getSource(),
                                com.mojang.brigadier.arguments.StringArgumentType.getString(c, "kind")))));
        // Рев 14: /riftbiome — где ближайший биом 07 «Висячие сады» и 05 «Роща Сизо». Без этого
        // CI не может ни снять кадры для сверки с референсом, ни посчитать воду/плющ в биоме.
        e.getDispatcher().register(Commands.literal("riftbiome").requires(s2 -> s2.hasPermission(2))
                .then(Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(c -> biome(c.getSource(),
                                com.mojang.brigadier.arguments.StringArgumentType.getString(c, "name")))));
        e.getDispatcher().register(Commands.literal("riftstruct").requires(s2 -> s2.hasPermission(2))
                .then(Commands.argument("kind", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(c -> probeStruct(c.getSource(),
                                com.mojang.brigadier.arguments.StringArgumentType.getString(c, "kind")))));
        // Рев 13.6: /riftdtat — что DT реально поставил в точке посадки. Один вызов RCON вместо
        // сотен «execute if block»: считает все блоки аддона вокруг корня.
        e.getDispatcher().register(Commands.literal("riftdtat").requires(s2 -> s2.hasPermission(2))
                .then(Commands.argument("x", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                        .then(Commands.argument("y", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                .then(Commands.argument("z", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                        .executes(c -> dtAt(c.getSource(),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "x"),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "y"),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "z")))))));
        // Рев 14.36: /riftdtwild [r] — СКОЛЬКО Dynamic Trees реально выросло вокруг игрока.
        // Нужна, потому что аддон «не менял ничего»: фичи биома не запускались вовсе, и в мире
        // не было ни одного динамического дерева/коралла. Теперь CI видит это числом.
        e.getDispatcher().register(Commands.literal("riftdtwild").requires(s2 -> s2.hasPermission(2))
                .executes(c -> dtWild(c.getSource(), 32))
                .then(Commands.argument("r", com.mojang.brigadier.arguments.IntegerArgumentType.integer(4, 48))
                        .executes(c -> dtWild(c.getSource(),
                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "r")))));
        // Рев 13.15: /riftgrow x y z [раз] — «костная мука» по-настоящему: вызываем BonemealableBlock
        // у блока в точке. Это ТОТ ЖЕ путь, которым растёт саженец у игрока, поэтому проверка
        // аддона DT перестаёт зависеть от капризов команды settree.
        e.getDispatcher().register(Commands.literal("riftgrow").requires(s -> s.hasPermission(2))
                .then(Commands.argument("x", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                        .then(Commands.argument("y", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                .then(Commands.argument("z", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                        .executes(c -> grow(c.getSource(),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "x"),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "y"),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "z"), 1))
                                        .then(Commands.argument("n", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 64))
                                                .executes(c -> grow(c.getSource(),
                                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "x"),
                                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "y"),
                                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "z"),
                                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "n"))))))));
        // Рев 13: /riftdt — самопроверка аддона Dynamic Trees БЕЗ зависимости от DT в classpath:
        // смотрим, под каким namespace реально зарегистрировались наши блоки и предметы.
        e.getDispatcher().register(Commands.literal("riftdt").requires(s -> s.hasPermission(2))
                .executes(c -> dt(c.getSource())));
        // Рев 12: /riftcoral — ближайшее КОРАЛЛОВОЕ ДЕРЕВО (растение из многих блоков), его высота и объём
        e.getDispatcher().register(Commands.literal("riftcoral").requires(s -> s.hasPermission(2))
                .executes(c -> coral(c.getSource())));
        // Рев 14.30: /riftwhere x z — что в этой КОЛОНКЕ: биом, верхний блок и его высота.
        // Нужна, чтобы CI проверял не «не вода», а что /locate biome действительно привёл
        // на садовую землю (garden_grass / garden_limestone / листва дуба / плющ).
        e.getDispatcher().register(Commands.literal("riftwhere").requires(s2 -> s2.hasPermission(2))
                .then(Commands.argument("x", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                        .then(Commands.argument("z", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                .executes(c -> where(c.getSource(),
                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "x"),
                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "z"))))));
        // Рев 14.40: /riftaround r — что вокруг игрока НА САМОМ ДЕЛЕ. CI проверяет этим, что
        // /locate biome waterfall_gardens привёл на садовый остров (известняк + трава садов),
        // а не «в структуру вообще другую», и что под островами нет дыр до бедрока.
        // Рев 15.10: /riftdtdbg — где именно обрывается мировой генератор DT:
        // диски Пуассона → грунт → вид → JoCode → сама генерация.
        e.getDispatcher().register(Commands.literal("riftdtdbg").requires(s2 -> s2.hasPermission(2))
                .executes(c -> dtDbg(c.getSource())));
        e.getDispatcher().register(Commands.literal("riftdtwhy").requires(s2 -> s2.hasPermission(2))
                .executes(c -> dtWhy(c.getSource())));
        e.getDispatcher().register(Commands.literal("riftaround").requires(s2 -> s2.hasPermission(2))
                .then(Commands.argument("r", com.mojang.brigadier.arguments.IntegerArgumentType.integer(4, 48))
                        .executes(c -> around(c.getSource(),
                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "r")))));
        e.getDispatcher().register(Commands.literal("riftportal").requires(s2 -> s2.hasPermission(2))
                .then(Commands.argument("mode", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(c -> portalCmd(c.getSource(),
                                com.mojang.brigadier.arguments.StringArgumentType.getString(c, "mode")))));
    }

    /**
     * Рев 11: {@code /rifttree} — ищет настоящее дерево Элисара В МИРЕ (не в плане) вокруг вызывающего:
     * сканирует колонки в радиусе 64, находит бревно sizo/ambra, поднимается к его вершине и считает,
     * сколько у дерева ВЕТВЕЙ (брёвен вне ствола) и листвы. Именно так CI проверяет, что дерево
     * не «сплошной квадрат», а растёт с ветками.
     */
    /** Non-loading diagnostic for operators: why can't Maja spawn at this player? */
    private static int majaCheck(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        var pos = src.getPosition();
        int x = (int) Math.floor(pos.x), z = (int) Math.floor(pos.z);
        int sea = CityPlan.get().ocean;
        DeepOcean.Region region = DeepOcean.regionAt(x, z);
        StringBuilder out = new StringBuilder("maja_check dimension=").append(level.dimension().location())
                .append(" x=").append(x).append(" z=").append(z)
                .append(" ocean=").append(DeepOcean.contains(x, z))
                .append(" biome=").append(level.getBiome(new BlockPos(x, sea - 8, z)).is(MajaOceanKeeper.BIOME))
                .append(" spawned=").append(MajaWorldData.get(level).hasSpawned(region.key()));
        double cx = ((x >> 4) << 4) + 8.0, cz = ((z >> 4) << 4) + 8.0;
        for (int rise = 0; rise <= 40; rise += 8) {
            out.append(" y").append(sea - 13 + rise).append('=');
            for (int i = 0; i < 4; i++) {
                StringBuilder why = new StringBuilder();
                boolean ok = ru.aiforge.riftworld.entity.MajaCollision.clear(level,
                        new net.minecraft.world.phys.Vec3(cx, sea - 13.0 + rise, cz), 45.0F + i * 90, why);
                if (i > 0) out.append('/');
                out.append(ok ? "clear" : why);
            }
        }
        String line = out.toString();
        src.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int tree(CommandSourceStack src, String kindFilter) {
        ServerLevel level = src.getLevel();
        if (level.dimension() != Flight.ELISAR_DIM) {
            src.sendFailure(Component.literal("rifttree: only in riftworld:elisar"));
            return 0;
        }
        CityPlan plan = CityPlan.get();
        BlockPos at = BlockPos.containing(src.getPosition());
        // ближайшее живое дерево из плана мира (без мёртвых: у них ветки есть всегда)
        // В плане есть деревья, которые лежат ВНЕ островов (там ничего не генерируется) — их пропускаем:
        // берём ближайшее дерево, у которого реально есть грунт, и только его считаем.
        CityPlan.Tree best = null;
        double bestD = 1e9;
        int base = -999;
        java.util.List<CityPlan.Tree> cand = new java.util.ArrayList<>(plan.trees);
        cand.sort(java.util.Comparator.comparingDouble(t -> Math.hypot(t.x - at.getX(), t.z - at.getZ())));
        int tries = 0;
        for (CityPlan.Tree t : cand) {
            if (t.kind == 2) continue;
            if (kindFilter != null) {
                String k = t.kind == 0 ? "sizo" : "ambra";
                if (!k.equalsIgnoreCase(kindFilter)) continue;
            }
            if (++tries > 60) break;                            // не бегаем по всему миру
            int b = t.baseY;
            if (b == Integer.MIN_VALUE) {
                ElisarChunkGenerator.Col c = ElisarChunkGenerator.column(plan, t.x, t.z);
                if (c.isl == null) continue;                    // дерево «висит» вне острова
                b = c.top + 1;
                t.baseY = b;
            }
            if (b < 0 || b > 200) continue;
            // чанк подгружаем принудительно: команда должна работать из любой точки мира
            level.getChunk(t.x >> 4, t.z >> 4);
            // выше макушки ствола должно быть свободно (сам ствол — не препятствие для камеры)
            boolean open = true;
            for (int y = b + t.h + 1; y <= b + t.h + 24 && open; ++y) {
                BlockPos sp = new BlockPos(t.x, y, t.z);
                if (sp.getY() >= level.getMaxBuildHeight()) break;
                if (level.getBlockState(sp).isSolidRender(level, sp)) open = false;
            }
            if (!open) continue;
            best = t;
            base = b;
            bestD = Math.hypot(t.x - at.getX(), t.z - at.getZ());
            break;
        }
        if (best == null) {
            src.sendFailure(Component.literal("rifttree: не нашлось дерева на острове"));
            return 0;
        }
        int logs = 0, branches = 0, leaves = 0;
        for (int dx = -12; dx <= 12; ++dx) {
            for (int dz = -12; dz <= 12; ++dz) {
                BlockPos col = new BlockPos(best.x + dx, 0, best.z + dz);
                if (!level.hasChunk(col.getX() >> 4, col.getZ() >> 4)) continue;   // чанк не прогружен — пропускаем
                for (int y = base; y <= base + 40; ++y) {
                    BlockState st = level.getBlockState(new BlockPos(best.x + dx, y, best.z + dz));
                    boolean log = st.is(ElisarBlocks.SIZO_LOG.get()) || st.is(ElisarBlocks.AMBRA_LOG.get());
                    if (log) {
                        ++logs;
                        if (st.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)
                                != net.minecraft.core.Direction.Axis.Y) ++branches;
                    } else if (st.is(ElisarBlocks.SIZO_LEAVES.get()) || st.is(ElisarBlocks.AMBRA_LEAVES.get())) {
                        ++leaves;
                    } else {
                        var rk = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(st.getBlock());
                        if (rk != null && "riftworlddt".equals(rk.getNamespace())) {
                            String rp = rk.getPath();
                            if (rp.contains("branch")) {
                                ++logs;
                                if (dx != 0 || dz != 0) ++branches;
                            } else if (rp.contains("leaves")) {
                                ++leaves;
                            }
                        }
                    }
                }
            }
        }
        String kind = best.kind == 0 ? "sizo" : best.kind == SizoGrove.TREE_GIANT ? "giant" : "ambra";
        // свободная точка для камеры: дерево может стоять между домами, поэтому ищем
        // направление, откуда крона видна и где сама камера не внутри блока
        int[] cam = cameraSpot(level, best.x, base, best.z, base + best.h + 6);
        int[] camTop = cameraSpot(level, best.x, base, best.z, base + best.h + 16);
        String line = "rifttree at=" + best.x + "," + base + "," + best.z + " kind=" + kind + " h=" + best.h
                + " logs=" + logs + " branches=" + branches + " leaves=" + leaves
                + " dist=" + Math.round(bestD) + " loaded=" + level.hasChunk(best.x >> 4, best.z >> 4)
                + " cam=" + cam[0] + "," + cam[1] + "," + cam[2]
                + " camtop=" + camTop[0] + "," + camTop[1] + "," + camTop[2];
        src.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    /**
     * Рев 11.13: точка обзора для камеры CI. Перебираем радиусы 14/18/22 и 16 направлений,
     * требуем, чтобы столбик 5 блоков в точке был не занят и чтобы взгляд на ствол не упирался
     * в стену. Если ничего не нашли — поднимаемся прямо над кроной.
     */
    private static int[] cameraSpot(ServerLevel level, int tx, int base, int tz, int camY) {
        int maxY = level.getMaxBuildHeight() - 4;
        int y = Math.min(camY, maxY);
        int[] radii = {11, 14, 18, 22, 26};
        for (int r : radii) {
            for (int i = 0; i < 16; ++i) {
                double a = i * Math.PI / 8.0;
                int cx = tx + (int) Math.round(r * Math.cos(a));
                int cz = tz + (int) Math.round(r * Math.sin(a));
                level.getChunk(cx >> 4, cz >> 4);
                boolean clear = true;
                for (int dy = -2; dy <= 2 && clear; ++dy) {
                    BlockPos p = new BlockPos(cx, y + dy, cz);
                    if (p.getY() <= level.getMinBuildHeight() || p.getY() >= level.getMaxBuildHeight()) { clear = false; break; }
                    if (blocked(level.getBlockState(p))) clear = false;
                }
                // линия взгляда на уровне камеры не должна проходить сквозь блоки
                for (int s = 2; s <= r - 2 && clear; ++s) {
                    int sx = tx + (int) Math.round((r - s) * Math.cos(a));
                    int sz = tz + (int) Math.round((r - s) * Math.sin(a));
                    BlockPos p = new BlockPos(sx, y, sz);
                    if (blocked(level.getBlockState(p))) clear = false;
                }
                if (clear) return new int[]{cx, y, cz};
            }
        }
        return new int[]{tx, Math.min(base + 30, maxY), tz};
    }

    /**
     * Рев 12: БЛИЗКАЯ точка съёмки — коралл должен занимать кадр, а не быть точкой вдали.
     * Перебираем радиусы 6–14 и 16 направлений, проверяем свободный столбик камеры и прямую видимость.
     */
    private static int[] closeSpot(ServerLevel level, int tx, int y, int tz, int[] radii, int lookY) {
        for (int r : radii) {
            for (int i = 0; i < 16; ++i) {
                double a = i * Math.PI / 8.0;
                int cx = tx + (int) Math.round(r * Math.cos(a));
                int cz = tz + (int) Math.round(r * Math.sin(a));
                level.getChunk(cx >> 4, cz >> 4);
                boolean clear = true;
                for (int dy = -2; dy <= 2 && clear; ++dy) {
                    BlockPos p = new BlockPos(cx, y + dy, cz);
                    if (p.getY() <= level.getMinBuildHeight() || p.getY() >= level.getMaxBuildHeight()) { clear = false; break; }
                    if (blocked(level.getBlockState(p))) clear = false;
                }
                // линия взгляда в 3D: 10 точек от камеры до цели должны быть свободны
                for (int s = 1; s <= 10 && clear; ++s) {
                    double t = s / 11.0;
                    int sx = (int) Math.round(cx + (tx - cx) * t);
                    int sy = (int) Math.round(y + (lookY - y) * t);
                    int sz = (int) Math.round(cz + (tz - cz) * t);
                    if (blocked(level.getBlockState(new BlockPos(sx, sy, sz)))) clear = false;
                }
                if (clear) return new int[]{cx, y, cz};
            }
        }
        return new int[]{tx + 10, y, tz + 10};
    }

    /**
     * v0.37.0: находит НАСТОЯЩУЮ «Мороковую Падь» рядом с заданной точкой.
     * Возвращает {x, surfaceY, z} или {@code null}, если в радиусе поиска Пади нет.
     * Нужно, чтобы Пегас/Вендиго/кадр биома снимались в тумане и на мерзлоте,
     * а не в ясном небе над Белым городом.
     */
    private static int[] morokSpot(ServerLevel level, int nearX, int nearZ, int cellRadius) {
        // v0.38.0: только НАСТОЯЩАЯ суша внутри Пади. Раньше бралась любая точка с весом >= 0.55 —
        // включая открытый океан, где биом не включается (нужен остров), деревьев нет, а проба
        // строила себе платформу прямо на воде (это и было видно на кадре 149).
        return ru.aiforge.riftworld.world.MorokHollow.landSpot(level, nearX, nearZ, Math.max(3, cellRadius));
    }

    /**
     * v0.38.0 — реальные числа модели из {@code assets/riftworld/geo/<name>.json}:
     * {@code {кубоиды, кости, размер атласа}}.
     *
     * <p>Нужно, чтобы проба доказывала, что в сборке лежит именно пересобранная модель, а не
     * печатала захардкоженное «53» (так было в v0.37.0: в гео уже 64 кубоида, в отчёте — 53).
     */
    /**
     * Камера в системе координат животного: {@code {camX, camY, camZ, tgtX, tgtY, tgtZ}}.
     *
     * <p>Раньше кадры Лиоплевродона снимались «на глаз» относительно центра площадки — и на
     * кадре 151 крупным планом оказывался хвост, потому что нос смотрит в −X, а камера стояла
     * с +Z. Здесь смещение задаётся вдоль носа ({@code along}) и вбок ({@code side}).
     *
     * @param yaw    рыскание животного (градусы MC: 0 = +Z, 90 = −X)
     * @param along  сколько блоков вперёд от центра (плюс — к носу)
     * @param side   сколько блоков вправо от носа
     * @param up     сколько блоков вверх
     */
    private static double[] poseCam(double cx, double cy, double cz, float yaw,
                                    double along, double side, double up,
                                    double distAlong, double distSide, double distUp) {
        double rad = Math.toRadians(yaw);
        double fx = -Math.sin(rad), fz = Math.cos(rad);      // вперёд (к носу)
        double rx = Math.cos(rad), rz = Math.sin(rad);       // вправо
        double gx = cx + fx * along + rx * side;
        double gz = cz + fz * along + rz * side;
        double gy = cy + up;
        double camX = gx + fx * distAlong + rx * distSide;
        double camZ = gz + fz * distAlong + rz * distSide;
        double camY = gy + distUp;
        return new double[]{camX, camY, camZ, gx, gy, gz};
    }

    /**
     * v0.40.0: стойка с полным комплектом брони (или с парой инструментов в руках).
     * Стойки — самый честный способ показать доспехи в игре: они носят их как игрок.
     */
    private static void v40Stand(ServerLevel sl, double x, double y, double z, float yaw,
                                boolean aurite, boolean tools) {
        var stand = net.minecraft.world.entity.EntityType.ARMOR_STAND.create(sl);
        if (stand == null) return;
        stand.moveTo(x, y, z, yaw, 0.0F);
        stand.setYRot(yaw);
        stand.yBodyRot = yaw;
        stand.yHeadRot = yaw;
        stand.setNoGravity(true);
        stand.setInvulnerable(true);
        stand.setSilent(true);
        stand.setShowArms(true);
        stand.setNoBasePlate(false);
        if (tools) {
            stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                    new net.minecraft.world.item.ItemStack(
                            (aurite ? ru.aiforge.riftworld.registry.ModItems.AURITE_PICKAXE
                                    : ru.aiforge.riftworld.registry.ModItems.RIFTCRYSTAL_PICKAXE).get()));
            stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,
                    new net.minecraft.world.item.ItemStack(
                            (aurite ? ru.aiforge.riftworld.registry.ModItems.AURITE_AXE
                                    : ru.aiforge.riftworld.registry.ModItems.RIFTCRYSTAL_AXE).get()));
        } else {
            stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
                    new net.minecraft.world.item.ItemStack(
                            (aurite ? ru.aiforge.riftworld.registry.ModItems.AURITE_HELMET
                                    : ru.aiforge.riftworld.registry.ModItems.RIFTCRYSTAL_HELMET).get()));
            stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,
                    new net.minecraft.world.item.ItemStack(
                            (aurite ? ru.aiforge.riftworld.registry.ModItems.AURITE_CHESTPLATE
                                    : ru.aiforge.riftworld.registry.ModItems.RIFTCRYSTAL_CHESTPLATE).get()));
            stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS,
                    new net.minecraft.world.item.ItemStack(
                            (aurite ? ru.aiforge.riftworld.registry.ModItems.AURITE_LEGGINGS
                                    : ru.aiforge.riftworld.registry.ModItems.RIFTCRYSTAL_LEGGINGS).get()));
            stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,
                    new net.minecraft.world.item.ItemStack(
                            (aurite ? ru.aiforge.riftworld.registry.ModItems.AURITE_BOOTS
                                    : ru.aiforge.riftworld.registry.ModItems.RIFTCRYSTAL_BOOTS).get()));
            stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                    new net.minecraft.world.item.ItemStack(
                            (aurite ? ru.aiforge.riftworld.registry.ModItems.AURITE_SWORD
                                    : ru.aiforge.riftworld.registry.ModItems.RIFTCRYSTAL_SWORD).get()));
            stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,
                    new net.minecraft.world.item.ItemStack(
                            (aurite ? ru.aiforge.riftworld.registry.ModItems.ELISAR_SHIELD
                                    : ru.aiforge.riftworld.registry.ModItems.ELISAR_BOW).get()));
        }
        sl.addFreshEntity(stand);
    }

    private static int[] geoStats(String name) {
        // 1) сначала читаем НАПРЯМУЮ из jar мода: ServerResourceManager не отдаёт
        //    нестандартные каталоги вроде assets/riftworld/geo/ (в прогоне #370 это дало -1),
        //    а путь внутри ModFile находится всегда и на клиенте, и на сервере.
        try {
            var modFile = net.minecraftforge.fml.ModList.get().getModFileById("riftworld");
            if (modFile != null) {
                var path = modFile.getFile().findResource("assets", "riftworld", "geo", name + ".json");
                if (path != null) {
                    try (var r = new java.io.InputStreamReader(java.nio.file.Files.newInputStream(path),
                            java.nio.charset.StandardCharsets.UTF_8)) {
                        return parseGeo(r);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        // 2) запасной путь — через менеджер ресурсов сервера
        try {
            var srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (srv != null) {
                var loc = new net.minecraft.resources.ResourceLocation("riftworld", "geo/" + name + ".json");
                var res = srv.getResourceManager().getResource(loc);
                if (res.isPresent()) {
                    try (var r = new java.io.InputStreamReader(res.get().open(),
                            java.nio.charset.StandardCharsets.UTF_8)) {
                        return parseGeo(r);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return new int[]{-1, -1, -1};
    }

    /** Число с тремя знаками: камеры теперь дробные (Vec3, а не целые блоки). */
    private static String r3(double v) {
        return String.valueOf(Math.round(v * 1000.0) / 1000.0);
    }

    /** {@code {кубоиды, кости, размер атласа}} из открытого geo-JSON. */
    private static int[] parseGeo(java.io.Reader reader) {
        try {
            var o = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            int boxes = 0;
            var bones = o.getAsJsonArray("bones");
            for (var b : bones) {
                var bo = b.getAsJsonObject();
                if (bo.has("cubes")) boxes += bo.getAsJsonArray("cubes").size();
            }
            int atlas = o.has("texture_width") ? o.get("texture_width").getAsInt() : 0;
            return new int[]{boxes, bones.size(), atlas};
        } catch (Throwable t) {
            return new int[]{-1, -1, -1};
        }
    }

    /**
     * Рев 12: для камеры «занято» — это любой блок, кроме воздуха и воды. Раньше проверялось только
     * {@code isSolidRender}, и камера вставала внутрь ламинарии/коралла (кадр получался зелёной кашей).
     */
    private static boolean blocked(BlockState s) {
        return !s.isAir() && !s.is(ElisarBlocks.WATER.get());
    }

    private static int run(CommandSourceStack src) {
        final int radius = 96;
        ServerLevel level = src.getLevel();
        if (level.dimension() != Flight.ELISAR_DIM) {
            src.sendFailure(Component.literal("riftsea: only in riftworld:elisar"));
            return 0;
        }
        CityPlan plan = CityPlan.get();
        int sea = plan.ocean;
        BlockPos at = BlockPos.containing(src.getPosition());
        int x0 = at.getX(), z0 = at.getZ();
        int floorHere = ElisarChunkGenerator.oceanFloor(plan, x0, z0);

        int minFloor = Integer.MAX_VALUE, maxFloor = Integer.MIN_VALUE;
        int caveCols = 0, wetCells = 0, airCells = 0, reefCols = 0, cols = 0;
        int floatAir = 0, ceilAir = 0;
        int bx = 0, by = 0, bz = 0, bestWet = 0;
        for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dz = -radius; dz <= radius; dz += 2) {
                int x = x0 + dx, z = z0 + dz;
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                int f = ElisarChunkGenerator.oceanFloor(plan, x, z);
                ++cols;
                minFloor = Math.min(minFloor, f);
                maxFloor = Math.max(maxFloor, f);
                if (ElisarSea.reefness(x, z) > 0.5) ++reefCols;
                int wet = 0, air = 0;
                for (int y = ElisarSea.BOTTOM; y <= f - 6; ++y) {
                    BlockState s = level.getBlockState(new BlockPos(x, y, z));
                    if (s.is(ElisarBlocks.WATER.get())) ++wet;
                    else if (s.isAir()) {
                        ++air;
                        // Рев 12: воздух, над которым СРАЗУ вода, — это «пузырь в толще воды»,
                        // именно он читался на скриншотах как странная дырка. Воздух у свода — норма.
                        BlockPos upPos = new BlockPos(x, y + 1, z);
                        BlockState up = level.getBlockState(upPos);
                        if (up.is(ElisarBlocks.WATER.get())) ++floatAir;
                        else if (up.isSolidRender(level, upPos)) ++ceilAir;   // воздух под камнем — карман у свода
                    }
                }
                wetCells += wet;
                airCells += air;
                if (wet + air >= 8) ++caveCols;
                if (wet >= 6 && wet > bestWet) {                 // запоминаем колонку, где пещера точно затоплена
                    bestWet = wet;
                    bx = x;
                    bz = z;
                    by = -1;
                    for (int y = f - 6; y >= ElisarSea.BOTTOM; --y) {
                        if (level.getBlockState(new BlockPos(x, y, z)).is(ElisarBlocks.WATER.get())) { by = y; break; }
                    }
                }
            }
        }

        int plants = 0, corals = 0;
        for (int dx = -radius; dx <= radius; dx += 4) {
            for (int dz = -radius; dz <= radius; dz += 4) {
                int x = x0 + dx, z = z0 + dz;
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                LevelChunk chunk = level.getChunk(x >> 4, z >> 4);
                int f = ElisarChunkGenerator.oceanFloor(plan, x, z);
                for (int y = f + 1; y <= Math.min(sea, f + 4); ++y) {
                    BlockState s = chunk.getBlockState(new BlockPos(x, y, z));
                    if (isPlant(s)) {
                        ++plants;
                        if (isCoral(s)) ++corals;
                    }
                }
            }
        }

        String line = "riftsea floor=" + floorHere + " water=" + (sea - 1 - floorHere)
                + " sea=" + sea + " min=" + minFloor + " max=" + maxFloor
                + " cols=" + cols + " caves=" + caveCols + " wet=" + wetCells + " air=" + airCells
                + " reef=" + reefCols + " plants=" + plants + " corals=" + corals
                + " float=" + floatAir + " ceilair=" + ceilAir
                + " bottom=" + ElisarSea.BOTTOM
                + (by != -1 ? " caveat=" + bx + "," + by + "," + bz : " caveat=none");
        final String out = line;
        src.sendSuccess(() -> Component.literal(out), false);
        return 1;
    }

    /**
     * Рев 12: {@code /riftcoral} — находит ближайшее КОРАЛЛОВОЕ ДЕРЕВО (растение из многих блоков) и печатает
     * его высоту, объём и «украшения» на ветвях. Именно этой командой CI доказывает, что коралл перестал
     * быть одиночным 3D-кустиком: {@code h>=3} и {@code blocks>=8} на одном растении.
     */
    /**
     * Рев 13.15: {@code /riftgrow x y z [n]} — применить «костную муку» к блоку n раз.
     *
     * <p>Это настоящий путь роста саженца ({@link net.minecraft.world.level.block.BonemealableBlock}),
     * а не отладочная команда Dynamic Trees. Возвращает строку {@code riftgrow at=… block=<до> grew=<n>
     * after=<после>} — по ней CI видит, во что превратился саженец.
     */
    private static int grow(CommandSourceStack src, int x, int y, int z, int n) {
        var level = src.getLevel();
        BlockPos pos = new BlockPos(x, y, z);
        BlockState before = level.getBlockState(pos);
        String b0 = blockId(before);
        int grew = 0;
        for (int i = 0; i < n; ++i) {
            BlockState st = level.getBlockState(pos);
            if (!(st.getBlock() instanceof net.minecraft.world.level.block.BonemealableBlock bonemealable)) break;
            level.setBlock(pos, st, 3);      // «встряхнуть» блок, как это делает игрок
            bonemealable.performBonemeal(level, level.getRandom(), pos, st);
            ++grew;
        }
        String line = "riftgrow at=" + x + "," + y + "," + z + " block=" + b0 + " grew=" + grew
                + " after=" + blockId(level.getBlockState(pos));
        final String out = line;
        src.sendSuccess(() -> Component.literal(out), false);
        return grew;
    }

    /** Рев 14.30: одна строка про колонку — биом, самый верхний непустой блок и его координата. */
    private static int where(CommandSourceStack src, int x, int z) {
        var level = src.getLevel();                     // вызывать: execute in riftworld:elisar run riftwhere x z
        int topY = 0, groundY = Integer.MIN_VALUE;
        String top = "?", ground = "?";
        // Рев 14.35: кроме верхнего блока печатаем ЗЕМЛЮ — первый непустой НЕ-жидкостный блок сверху.
        // Пруд на полке садов — это законно (на референсе вода стоит на полках), а море — нет;
        // по «top» их было не отличить.
        for (int y = 200; y >= 0; --y) {
            BlockState bs = level.getBlockState(new BlockPos(x, y, z));
            if (bs.isAir()) continue;
            var key = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(bs.getBlock());
            String id = key == null ? "?" : key.toString();
            if ("?".equals(top)) { topY = y; top = id; }
            if (bs.getFluidState().isEmpty()) { groundY = y; ground = id; break; }
        }
        String biome = level.getBiome(new BlockPos(x, Math.max(topY, 64), z)).unwrapKey()
                .map(k -> k.location().toString()).orElse("?");
        String line = "riftwhere at=" + x + "," + z + " biome=" + biome + " top=" + topY + ":" + top
                + " ground=" + groundY + ":" + ground
                + " sea=" + CityPlan.get().ocean
                + " garden=" + WaterfallGardens.inside(x + 0.5, z + 0.5);
        src.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    /**
     * Рев 14: {@code /riftbiome gardens|sizo} — ближайший регион биома и цифры для CI.
     *
     * <p>Ищет по сетке супер-ячеек вокруг вызывающего (радиус 6 ячеек), берёт ближайший регион и
     * считает по кубу вокруг центра: колонки с водой, с прудом, водопадные струи, плющ, траву
     * садов, стволы. Плюс {@code planks=}/{@code lantern=} — рукотворные блоки: на референсе
     * Висячих садов их быть НЕ ДОЛЖНО, и CI падает, если они появятся снова.
     */
    private static int biome(CommandSourceStack src, String name) {
        var level = src.getLevel();
        BlockPos at = src.getPlayer() != null ? src.getPlayer().blockPosition() : new BlockPos(0, 0, 0);
        String lname = name.toLowerCase(java.util.Locale.ROOT);
        if (lname.startsWith("ashen")) {
            AshenLands.Region ar = AshenLands.nearestWithRedCastle(at.getX(), at.getZ());
            if (ar == null) {
                src.sendFailure(Component.literal("riftbiome: рядом нет региона ashen"));
                return 0;
            }
            String abio = level.getBiome(new BlockPos(ar.cx, CastleSite.Y + 4, ar.cz)).unwrapKey()
                    .map(k -> k.location().toString()).orElse("?");
            String aline = "riftbiome name=ashen at=" + ar.cx + "," + ar.cz + " bio=" + abio
                    + " r=" + (int) Math.round(ar.R) + " combo=" + ar.comboMode
                    + " top=" + CastleSite.Y + " castle=" + ar.x0 + "," + ar.z0;
            src.sendSuccess(() -> Component.literal(aline), false);
            return 1;
        }
        if (lname.startsWith("crystal")) {
            return probeCrystalRifts(src, level, at);
        }
        // Рев 14.29: /riftbiome violet — Фиолетовые холмы (третий биом в порядке владельца).
        boolean violet = lname.startsWith("violet");
        boolean gardens = !violet && !lname.startsWith("sizo");
        int found = 0, want = violet ? VioletHills.SUPER : gardens ? WaterfallGardens.SUPER : SizoGrove.SUPER;
        int bestX = 0, bestZ = 0, bestR = 0;
        double bestD = Double.MAX_VALUE;
        SizoGrove.Region bestSizo = null;   // Рев 14.3: для счётчика структур рощи
        WaterfallGardens.Region bestGarden = null;   // Рев 14.8: координаты водопада для кадра
        VioletHills.Region bestViolet = null;
        for (int dx = -6; dx <= 6; ++dx) {
            for (int dz = -6; dz <= 6; ++dz) {
                int sx = Math.floorDiv(at.getX(), want) + dx, sz = Math.floorDiv(at.getZ(), want) + dz;
                if (violet) {
                    VioletHills.Region rg = VioletHills.region(sx, sz);
                    if (rg == null) continue;
                    double d = Math.hypot(rg.meadow.cx - at.getX(), rg.meadow.cz - at.getZ());
                    if (d < bestD) { bestD = d; bestViolet = rg; bestX = (int) rg.meadow.cx; bestZ = (int) rg.meadow.cz; bestR = (int) rg.meadow.R; }
                } else if (gardens) {
                    WaterfallGardens.Region rg = WaterfallGardens.region(sx, sz);
                    if (rg == null) continue;
                    double d = Math.hypot(rg.cx - at.getX(), rg.cz - at.getZ());
                    // Рев 14.26: сады — архипелаг. Считаем и снимаем камеры по САМОМУ КРУПНОМУ
                    // острову: центр региона теперь вода между островами (там нечего мерить).
                    if (d < bestD) {
                        bestD = d; bestGarden = rg;
                        ru.aiforge.riftworld.world.CityPlan.Island bi = rg.isles.get(0);
                        for (ru.aiforge.riftworld.world.CityPlan.Island i2 : rg.isles) if (i2.R > bi.R) bi = i2;
                        bestX = (int) bi.cx; bestZ = (int) bi.cz; bestR = (int) bi.R;
                    }
                } else {
                    SizoGrove.Region rg = SizoGrove.region(sx, sz);
                    if (rg == null) continue;
                    double d = Math.hypot(rg.cx - at.getX(), rg.cz - at.getZ());
                    // Предпочитаем ближайшую рощу с руиной башни и малыми формами у тропы, чтобы проверить все элементы референса 05
                    double score = d - (rg.temple ? 2400.0 : 0.0) - (!rg.carts.isEmpty() ? 1100.0 : 0.0)
                            - (!rg.shrines.isEmpty() ? 1100.0 : 0.0);
                    if (score < bestD) { bestD = score; bestX = rg.cx; bestZ = rg.cz; bestR = (int) rg.hub.R; bestSizo = rg; }
                }
            }
        }
        if (bestD == Double.MAX_VALUE) {
            src.sendFailure(Component.literal("riftbiome: рядом нет региона " + name));
            return 0;
        }
        // считаем по кубу ±R/2 вокруг центра: вода, плющ, трава садов, стволы, рукотворное
        // Радиус считаем скромным: куб ±36 по горизонтали и полоса высот 60..150 — иначе RCON-вызов
        // перебирает миллионы блоков и отваливается по таймауту.
        // Рев 14.26: куб — по кромке крупнейшего острова (раньше остров был в 2× больше региона).
        int R = Math.max(22, Math.min(56, (int) Math.ceil(bestR)));   // Рев 16: острова до R=55
        int water = 0, ivy = 0, gardenGrass = 0, logs = 0, leaves = 0, planks = 0, lanterns = 0, top = Integer.MIN_VALUE;
        java.util.Map<String, Integer> solids = new java.util.TreeMap<>();
        var grassTop = ru.aiforge.riftworld.registry.ElisarBlocks.GARDEN_GRASS.get();
        var leafBlk = ru.aiforge.riftworld.registry.ElisarBlocks.GARDEN_LEAVES.get();
        var logBlk = ru.aiforge.riftworld.registry.ElisarBlocks.GARDEN_LOG.get();
        for (int x = bestX - R; x <= bestX + R; x += 1) {
            for (int z = bestZ - R; z <= bestZ + R; z += 1) {
                if (!level.isLoaded(new BlockPos(x, at.getY(), z))) continue;
                for (int y = 60; y <= 150; ++y) {
                    BlockState st = level.getBlockState(new BlockPos(x, y, z));
                    if (st.isAir()) continue;
                    top = Math.max(top, y);
                    var key = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(st.getBlock());
                    if (key == null) continue;
                    String path = key.getPath();
                    // Рев 14.23: вода садов теперь ванильная (её цвет берётся из биома) — считаем обе
                    if (st.is(ru.aiforge.riftworld.registry.ElisarBlocks.WATER.get()) || st.is(net.minecraft.world.level.block.Blocks.WATER)
                            || st.is(ru.aiforge.riftworld.registry.ElisarBlocks.GARDEN_WATER.get())) { water++; continue; }
                    if (st.getBlock() == grassTop) { gardenGrass++; continue; }
                    if (st.getBlock() == leafBlk) { leaves++; continue; }
                    if (st.getBlock() == logBlk) { logs++; continue; }
                    if (st.getBlock() instanceof net.minecraft.world.level.block.VineBlock) { ivy++; continue; }
                    if (path.endsWith("planks") || path.contains("plank") || st.getBlock() instanceof net.minecraft.world.level.block.SlabBlock
                            || st.getBlock() instanceof net.minecraft.world.level.block.StairBlock) planks++;
                    if (path.contains("lantern")) lanterns++;
                    solids.merge(path, 1, Integer::sum);
                }
            }
        }
        // Рев 14.29: отдельные счётчики лаванды, сиреневой листвы, заборов и фонарей —
        // по ним CI отличает «поля рядами» от «веснушек» и видит рукотворное у дорожек.
        int violetBlooms = 0, violetLeaves = 0, fences = 0, lamps = 0, lavender = 0, walls = 0, accentFlowers = 0, sizoInGardens = 0;
        if (violet) {
            var bloom = new java.util.HashSet<net.minecraft.world.level.block.Block>();
            bloom.add(ru.aiforge.riftworld.registry.ElisarBlocks.VIOLET_BLOOM.get());
            bloom.add(ru.aiforge.riftworld.registry.ElisarBlocks.LILAC_BLOOM.get());
            bloom.add(ru.aiforge.riftworld.registry.ElisarBlocks.PALE_BLOOM.get());
            var accents = new java.util.HashSet<net.minecraft.world.level.block.Block>();
            accents.add(net.minecraft.world.level.block.Blocks.PINK_TULIP);
            accents.add(net.minecraft.world.level.block.Blocks.ALLIUM);
            accents.add(net.minecraft.world.level.block.Blocks.ORANGE_TULIP);
            accents.add(net.minecraft.world.level.block.Blocks.OXEYE_DAISY);
            accents.add(net.minecraft.world.level.block.Blocks.CORNFLOWER);
            accents.add(net.minecraft.world.level.block.Blocks.DANDELION);
            var vl = ru.aiforge.riftworld.registry.ElisarBlocks.VIOLET_LEAVES.get();
            var fenceBlk = ru.aiforge.riftworld.registry.ElisarBlocks.FENCE.get();
            var wallBlk = net.minecraft.world.level.block.Blocks.STONE_BRICK_WALL;
            var torchBlk = net.minecraft.world.level.block.Blocks.TORCH;
            var lavenderBlk = ru.aiforge.riftworld.registry.ElisarBlocks.LAVENDER_BED.get();
            for (int x = bestX - R; x <= bestX + R; x += 1) {
                for (int z = bestZ - R; z <= bestZ + R; z += 1) {
                    if (!level.isLoaded(new BlockPos(x, at.getY(), z))) continue;
                    for (int y = 60; y <= 150; ++y) {
                        BlockState bs = level.getBlockState(new BlockPos(x, y, z));
                        if (bs.isAir()) continue;
                        if (bloom.contains(bs.getBlock())) { violetBlooms++; continue; }
                        if (accents.contains(bs.getBlock())) { accentFlowers++; continue; }
                        if (bs.getBlock() == vl) { violetLeaves++; continue; }
                        if (bs.getBlock() == lavenderBlk) { lavender++; continue; }
                        if (bs.getBlock() == fenceBlk) { fences++; continue; }
                        if (bs.getBlock() == wallBlk) { walls++; continue; }
                        if (bs.getBlock() instanceof net.minecraft.world.level.block.LanternBlock
                                || bs.getBlock() == torchBlk) lamps++;
                        var bKey = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(bs.getBlock());
                        if (bKey != null) {
                            String bp = bKey.getPath();
                            if (bp.equals("sizo_log") || bp.equals("sizo_leaves")
                                    || bp.equals("sizo_branch") || bp.equals("sizo_sapling") || bp.equals("giant_sizo_sapling")) {
                                sizoInGardens++;
                            }
                        }
                    }
                }
            }
        }
        String top6 = solids.entrySet().stream()
                .sorted((a2, b2) -> b2.getValue() - a2.getValue()).limit(6)
                .map(e2 -> e2.getKey() + "=" + e2.getValue())
                .reduce((a2, b2) -> a2 + " " + b2).orElse("-");
        // Рев 14.3: редкости рощи по ТЗ — тележка ≈1/3 биомов, памятник и храм ≈1/7, 1–2 пары столбов.
        String struct = "-";
        if (bestSizo != null) {
            struct = "carts=" + bestSizo.carts.size() + " shrines=" + bestSizo.shrines.size()
                    + " temples=" + (bestSizo.temple ? 1 : 0)
                    + " pillar_pairs=" + bestSizo.pillarPairs.size();
        }
        // Рев 14.8: ближайший предвыбранный водопад — по нему CI наводит камеру
        String fallAt = "-";
        // Рев 16: цифры архипелага и ВОДОПАДОВ. Владелец: «водопады должны стекать», «острова
        // радиусом 40–55, разной формы», «сизо в садах не растёт». Всё это видно только пробами.
        String gardenStat = "";
        String river = "-";
        if (bestGarden != null) {
            double rMin = 1e9, rMax = 0;
            for (CityPlan.Island g2 : bestGarden.isles) { rMin = Math.min(rMin, g2.R); rMax = Math.max(rMax, g2.R); }
            gardenStat = " isles=" + bestGarden.isles.size()
                    + " isl_r=" + String.format(java.util.Locale.ROOT, "%.0f..%.0f", rMin, rMax)
                    + " sea=" + CityPlan.get().ocean;
            if (bestGarden.isles.size() >= 2) {
                CityPlan.Island aI = bestGarden.isles.get(0), bI = bestGarden.isles.get(1);
                double bestGap = 1e9;
                for (int i = 0; i < bestGarden.isles.size(); ++i)
                    for (int j = i + 1; j < bestGarden.isles.size(); ++j) {
                        CityPlan.Island p1 = bestGarden.isles.get(i), p2 = bestGarden.isles.get(j);
                        double gap = Math.hypot(p1.cx - p2.cx, p1.cz - p2.cz) - p1.R - p2.R;
                        if (gap < bestGap) { bestGap = gap; aI = p1; bI = p2; }
                    }
                double mx = 0.5 * (aI.cx + bI.cx), mz = 0.5 * (aI.cz + bI.cz);
                double ddx = bI.cx - aI.cx, ddz = bI.cz - aI.cz, dlen = Math.max(1.0, Math.hypot(ddx, ddz));
                double ux = -ddz / dlen, uz = ddx / dlen;
                int oc = CityPlan.get().ocean;
                int rcx = (int) Math.round(mx + ux * 40), rcy = oc + 30, rcz = (int) Math.round(mz + uz * 40);
                int rlx = (int) Math.round(mx - ux * 12), rly = oc + 6, rlz = (int) Math.round(mz - uz * 12);
                river = rcx + "," + rcy + "," + rcz + ":" + rlx + "," + rly + "," + rlz;
            }
        }
        int fallWater = 0, fallSrc = 0, gardenOak = 0, dryCols = 0, submergedAir = 0;
        if (gardens) {
            int ocean = CityPlan.get().ocean;
            var gsrc = ru.aiforge.riftworld.registry.ElisarBlocks.GARDEN_WATER.get();
            for (int x = bestX - R; x <= bestX + R; ++x)
                for (int z = bestZ - R; z <= bestZ + R; ++z) {
                    if (!level.isLoaded(new BlockPos(x, ocean + 2, z))) continue;
                    int colWater = 0;
                    boolean hasSource = false;
                    for (int y = ocean - 4; y <= 150; ++y) {
                        BlockState bs = level.getBlockState(new BlockPos(x, y, z));
                        if (bs.isAir()) continue;
                        if (bs.getBlock() == gsrc) {
                            ++colWater;
                            if (bs.getFluidState().isSource()) hasSource = true;
                            continue;
                        }
                        var key2 = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(bs.getBlock());
                        if (key2 == null) continue;
                        String pth = key2.getPath();
                        if (pth.contains("sizo")) ++sizoInGardens;
                        else if (pth.contains("garden_oak")) ++gardenOak;
                    }
                    if (colWater > 3) { fallWater += colWater; if (hasSource) ++fallSrc; }
                    ElisarChunkGenerator.Col topC = ElisarChunkGenerator.column(CityPlan.get(), x, z);
                    if (topC.garden && topC.isl != null && topC.isl.kind == WaterfallGardens.KIND
                            && topC.top <= ocean && !topC.pond && topC.waterTop == Integer.MIN_VALUE) {
                        ++dryCols;
                        for (int wy = topC.top + 1; wy <= ocean; ++wy) {
                            if (level.getBlockState(new BlockPos(x, wy, z)).isAir()) ++submergedAir;
                        }
                    }
                }
            if (!bestGarden.falls.isEmpty()) {
                WaterfallGardens.Fall f0 = bestGarden.falls.get(0);
                fallAt = f0.x + "," + f0.z;
            }
        }
        // Рев 14.18: точка обзора обрыва — кадр с полки, как на референсе
        String wall = "-";
        if (gardens) {
            int[] wv = ElisarChunkGenerator.gardenWallView(bestX, bestZ, Math.max(24, bestR));
            if (wv != null) wall = wv[0] + "," + wv[1] + "," + wv[2] + ":" + wv[3] + "," + wv[4] + "," + wv[5];
        }
        String sizoExtra = "";
        if (bestSizo != null) {
            int sizoTrail = 0, sizoFlowers = 0, sizoPillars = 0;
            var flw = ru.aiforge.riftworld.registry.ElisarBlocks.FLOWERS.get();
            var vb = ru.aiforge.riftworld.registry.ElisarBlocks.VIOLET_BLOOM.get();
            var lb = ru.aiforge.riftworld.registry.ElisarBlocks.PALE_BLOOM.get();
            var tp = ru.aiforge.riftworld.registry.TempleBlocks.PILLAR.get();
            var tg = ru.aiforge.riftworld.registry.TempleBlocks.GOLD_RUNE.get();
            for (int x = bestX - R; x <= bestX + R; ++x)
                for (int z = bestZ - R; z <= bestZ + R; ++z) {
                    if (!level.isLoaded(new BlockPos(x, at.getY(), z))) continue;
                    for (int y = 60; y <= 150; ++y) {
                        BlockState bs = level.getBlockState(new BlockPos(x, y, z));
                        if (bs.isAir()) continue;
                        var b = bs.getBlock();
                        if (b == net.minecraft.world.level.block.Blocks.COBBLESTONE
                                || b == net.minecraft.world.level.block.Blocks.MOSSY_COBBLESTONE
                                || b == net.minecraft.world.level.block.Blocks.DIRT_PATH) ++sizoTrail;
                        else if (b == flw || b == vb || b == lb) ++sizoFlowers;
                        else if (b == tp || b == tg) ++sizoPillars;
                    }
                }
            CityPlan plan = CityPlan.get();
            double[] c1 = bestSizo.trailPoint(0.12), t1 = bestSizo.trailPoint(0.66);
            int y1c = ElisarChunkGenerator.column(plan, (int) Math.round(c1[0]), (int) Math.round(c1[1])).top + 3;
            int y1t = ElisarChunkGenerator.column(plan, (int) Math.round(t1[0]), (int) Math.round(t1[1])).top + 4;
            String sv1 = Math.round(c1[0]) + "," + y1c + "," + Math.round(c1[1]) + ":"
                    + Math.round(t1[0]) + "," + y1t + "," + Math.round(t1[1]);

            double[] c2 = bestSizo.trailPoint(0.24), t2 = bestSizo.trailPoint(0.76);
            int y2c = ElisarChunkGenerator.column(plan, (int) Math.round(c2[0]), (int) Math.round(c2[1])).top + 2;
            int y2t = ElisarChunkGenerator.column(plan, (int) Math.round(t2[0]), (int) Math.round(t2[1])).top + 5;
            String sv2 = Math.round(c2[0]) + "," + y2c + "," + Math.round(c2[1]) + ":"
                    + Math.round(t2[0]) + "," + y2t + "," + Math.round(t2[1]);

            double[] c3 = bestSizo.trailSidePoint(0.42, -4.0);
            double[] t3 = bestSizo.temple ? new double[]{bestSizo.tx, bestSizo.tz} : bestSizo.trailPoint(0.84);
            int y3c = ElisarChunkGenerator.column(plan, (int) Math.round(c3[0]), (int) Math.round(c3[1])).top + 4;
            int y3t = ElisarChunkGenerator.column(plan, (int) Math.round(t3[0]), (int) Math.round(t3[1])).top + 8;
            String sv3 = Math.round(c3[0]) + "," + y3c + "," + Math.round(c3[1]) + ":"
                    + Math.round(t3[0]) + "," + y3t + "," + Math.round(t3[1]);

            sizoExtra = " sizo_trail=" + sizoTrail + " sizo_flowers=" + sizoFlowers + " sizo_pillars=" + sizoPillars
                    + " sview1=" + sv1 + " sview2=" + sv2 + " sview3=" + sv3;
        }
        String vio = "";
        if (violet) {
            String vroad = "-", vpath = "-", vfields = "-", vtrees = "-", varch = "-";
            int vgY = CityPlan.get().ocean + 16;
            int vislands = 0;
            String vislR = "-";
            if (bestViolet != null) {
                vislands = bestViolet.isles.size();
                double vrMin = 1e9, vrMax = 0.0;
                for (CityPlan.Island vi : bestViolet.isles) {
                    vrMin = Math.min(vrMin, vi.R);
                    vrMax = Math.max(vrMax, vi.R);
                }
                vislR = String.format(java.util.Locale.ROOT, "%.0f..%.0f", vrMin, vrMax);
                int oc = CityPlan.get().ocean;
                double hRim = Math.max(0.28, bestViolet.hubRimU);
                vgY = ElisarChunkGenerator.violetTop(bestViolet.hub, bestX, bestZ, oc);

                // 49_violet_hills: извилистая дорога с оградой и холмами на главном острове
                double[] pCam = bestViolet.roadPoint(0.22 * hRim);
                double[] pTgt = bestViolet.roadPoint(0.76 * hRim);
                int vcy = ElisarChunkGenerator.violetTop(bestViolet.hub, pCam[0], pCam[1], oc) + 6;
                int vty = ElisarChunkGenerator.violetTop(bestViolet.hub, pTgt[0], pTgt[1], oc) + 3;
                vroad = Math.round(pCam[0]) + "," + vcy + "," + Math.round(pCam[1]) + ":"
                        + Math.round(pTgt[0]) + "," + vty + "," + Math.round(pTgt[1]);

                // 49b_violet_archipelago: панорама огромных парящих островов и пустой пропасти, где обрывается дорога
                double[] paCam = bestViolet.roadSidePoint(0.68 * hRim * bestViolet.mainRoadLen(), -24.0);
                double[] paTgt = bestViolet.roadPoint(Math.min(0.92, bestViolet.nextRimU + 0.08));
                CityPlan.Island camIsl = bestViolet.islandAt(paCam[0], paCam[1]);
                int vacy = (camIsl != null ? ElisarChunkGenerator.violetTop(camIsl, paCam[0], paCam[1], oc) : vgY) + 24;
                CityPlan.Island tgtIsl = bestViolet.isles.size() > 1 ? bestViolet.isles.get(1) : bestViolet.hub;
                int vaty = ElisarChunkGenerator.violetTop(tgtIsl, paTgt[0], paTgt[1], oc) + 2;
                varch = Math.round(paCam[0]) + "," + vacy + "," + Math.round(paCam[1]) + ":"
                        + Math.round(paTgt[0]) + "," + vaty + "," + Math.round(paTgt[1]);

                // 50_violet_fields: воздушные лавандовые склоны без плотных нагромождений
                double[] pfCam = bestViolet.roadSidePoint(0.42 * hRim * bestViolet.mainRoadLen(), -19.0);
                double[] pfTgt = bestViolet.roadPoint(0.74 * hRim);
                int vfcy = ElisarChunkGenerator.violetTop(bestViolet.hub, pfCam[0], pfCam[1], oc) + 6;
                int vfty = ElisarChunkGenerator.violetTop(bestViolet.hub, pfTgt[0], pfTgt[1], oc) + 2;
                vfields = Math.round(pfCam[0]) + "," + vfcy + "," + Math.round(pfCam[1]) + ":"
                        + Math.round(pfTgt[0]) + "," + vfty + "," + Math.round(pfTgt[1]);

                // 51_violet_path: прогулочный кадр по дороге к обрыву над пропастью между островами
                double[] ppCam = bestViolet.roadPoint(0.56 * hRim);
                double[] ppTgt = bestViolet.roadPoint(Math.min(0.95, bestViolet.nextRimU + 0.03));
                int vpcy = ElisarChunkGenerator.violetTop(bestViolet.hub, ppCam[0], ppCam[1], oc) + 2;
                int vpty = ElisarChunkGenerator.violetTop(tgtIsl, ppTgt[0], ppTgt[1], oc) + 1;
                vpath = Math.round(ppCam[0]) + "," + vpcy + "," + Math.round(ppCam[1]) + ":"
                        + Math.round(ppTgt[0]) + "," + vpty + "," + Math.round(ppTgt[1]);

                // Ищем ближайшее реальное дерево riftworlddt:lilac на склоне для крупного плана 52_violet_trees_closeup
                int txTree = Integer.MIN_VALUE, tyTree = 0, tzTree = 0;
                double bestTd = 1e9;
                for (int x = bestX - 36; x <= bestX + 36; ++x)
                    for (int z = bestZ - 36; z <= bestZ + 36; ++z) {
                        if (!level.isLoaded(new BlockPos(x, at.getY(), z))) continue;
                        int gy = ElisarChunkGenerator.violetTop(bestViolet.hub, x, z, oc);
                        for (int y = gy + 1; y <= gy + 6; ++y) {
                            BlockState bs = level.getBlockState(new BlockPos(x, y, z));
                            var k = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(bs.getBlock());
                            if (k != null && "riftworlddt".equals(k.getNamespace()) && k.getPath().contains("branch")) {
                                double d2 = Math.hypot(x - bestX, z - bestZ);
                                if (d2 < bestTd && d2 > 6.0) {
                                    bestTd = d2; txTree = x; tyTree = gy; tzTree = z;
                                }
                                break;
                            }
                        }
                    }
                if (txTree != Integer.MIN_VALUE) {
                    double dxT = bestX - txTree, dzT = bestZ - tzTree, lenT = Math.max(1.0, Math.hypot(dxT, dzT));
                    int camX = (int) Math.round(txTree + dxT / lenT * 11.0);
                    int camZ = (int) Math.round(tzTree + dzT / lenT * 11.0);
                    int camY = ElisarChunkGenerator.violetTop(bestViolet.hub, camX, camZ, oc) + 4;
                    vtrees = camX + "," + camY + "," + camZ + ":" + txTree + "," + (tyTree + 4) + "," + tzTree;
                }
            }
            vio = " vislands=" + vislands + " visl_r=" + vislR
                    + " violet_blooms=" + violetBlooms + " violet_leaves=" + violetLeaves + " lavender=" + lavender
                    + " accent_flowers=" + accentFlowers + " walls=" + walls
                    + " fences=" + fences + " lamps=" + lamps + " vground=" + vgY
                    + " road=" + vroad + " varch=" + varch + " vfields=" + vfields + " vpath=" + vpath + " vtrees=" + vtrees;
        }
        // Рев 14.35: каким биомом НА САМОМ ДЕЛЕ числится земля региона. Это ловит перепутанные
        // поля биом-сурса (именно так «сады» оказались на островках данжа в море).
        String hubBio = level.getBiome(new BlockPos(bestX, CityPlan.get().ocean + 6, bestZ)).unwrapKey()
                .map(k -> k.location().toString()).orElse("?");
        String line = "riftbiome name=" + (violet ? "violet" : gardens ? "gardens" : "sizo") + " at=" + bestX + "," + bestZ
                + " bio=" + hubBio
                + " r=" + bestR + " top=" + top + " water=" + water + " garden_grass=" + gardenGrass
                + " garden_leaves=" + leaves + " garden_logs=" + logs + " ivy=" + ivy
                + " planks=" + planks + " lantern=" + lanterns + " struct=" + struct
                + " fall=" + fallAt + " wall=" + wall + " river=" + river + vio + sizoExtra + gardenStat
                + " falls_water=" + fallWater + " falls_src=" + fallSrc
                + " garden_oak=" + gardenOak + " sizo_blocks=" + sizoInGardens
                + " below_sea_cols=" + dryCols + " submerged_air=" + submergedAir
                + " blocks=" + top6;
        src.sendSuccess(() -> Component.literal(line), false);
        found = water;
        return found;
    }

    /**
     * Полная проверка биома «Хрустальные разломы» ({@code /riftbiome crystal}, референс {@code 02_crystal_sanctum.png}):
     * архипелаг 4–6 парящих островов, непрерывные проходимые каменные арочные мосты между островами,
     * гигантские блочные кристаллы, минеральная бирюзовая чаша с водопадами, разрушенная колоннада-астролябия
     * и отсутствие чужих деревьев Сизо, плюс 5 выверенных точек съёмки {@code cview1..cview5}.
     */
    private static int probeCrystalRifts(CommandSourceStack src, ServerLevel level, BlockPos at) {
        CrystalRifts.Region best = null;
        double bestD = Double.MAX_VALUE;
        for (int dx = -8; dx <= 8; ++dx) {
            for (int dz = -8; dz <= 8; ++dz) {
                int sx = Math.floorDiv(at.getX(), CrystalRifts.SUPER) + dx;
                int sz = Math.floorDiv(at.getZ(), CrystalRifts.SUPER) + dz;
                CrystalRifts.Region rg = CrystalRifts.region(sx, sz);
                if (rg == null || !rg.hasColonnade) continue;
                double d = Math.hypot(rg.cx - at.getX(), rg.cz - at.getZ());
                if (d < bestD) { bestD = d; best = rg; }
            }
        }
        if (best == null) {
            src.sendFailure(Component.literal("riftbiome: рядом нет региона crystal_rifts"));
            return 0;
        }
        int hx = (int) Math.round(best.hub.cx), hz = (int) Math.round(best.hub.cz);
        CityPlan.Island isl1 = best.isles.size() > 1 ? best.isles.get(1) : best.hub;
        int i1x = (int) Math.round(isl1.cx), i1z = (int) Math.round(isl1.cz);

        int crystalBlocks = 0, marbleBlocks = 0, crystalFlora = 0, water = 0, ivy = 0, sizoBlocks = 0;
        int riftTurfCnt = 0, riftGrassCnt = 0, vanillaGrassCnt = 0, isl1Trees = 0;
        var cDeep = ElisarBlocks.CRYSTAL_BLOCK_DEEP.get();
        var cMid = ElisarBlocks.CRYSTAL_BLOCK.get();
        var cBright = ElisarBlocks.CRYSTAL_BLOCK_BRIGHT.get();
        var rMarb = ElisarBlocks.RIFT_MARBLE.get();
        var rMarbM = ElisarBlocks.RIFT_MARBLE_MOSSY.get();
        var rSlate = ElisarBlocks.RIFT_SLATE.get();
        var rSlab = ElisarBlocks.RIFT_MARBLE_SLAB.get();
        var rTurf = ElisarBlocks.RIFT_TURF.get();
        var rGrass = ElisarBlocks.RIFT_GRASS.get();
        var cClust = ElisarBlocks.CRYSTAL_CLUSTER.get();
        var cShroom = ElisarBlocks.CRYSTAL_SHROOM.get();
        var gWat = ElisarBlocks.GARDEN_WATER.get();

        // Сканируем как hub (святилище + аллея), так и главный кристальный остров #1 (гигантские кристаллы + чаша + водопады)
        int[][] centers = {{hx, hz, 48}, {i1x, i1z, 52}};
        java.util.Set<Long> visitedCols = new java.util.HashSet<>();
        for (int ci = 0; ci < centers.length; ++ci) {
            int[] c0 = centers[ci];
            int cx = c0[0], cz = c0[1], rad = c0[2];
            for (int x = cx - rad; x <= cx + rad; ++x) {
                for (int z = cz - rad; z <= cz + rad; ++z) {
                    long ck = ((long) x << 32) ^ (z & 0xFFFFFFFFL);
                    if (!visitedCols.add(ck)) continue;
                    if (!level.hasChunk(x >> 4, z >> 4)) continue;
                    CityPlan.Island ownIsl = CityPlan.get().islandAt(x + 0.5, z + 0.5);
                    boolean inCrystalIsl = ownIsl != null && ownIsl.kind == CrystalRifts.KIND;
                    for (int y = 62; y <= 140; ++y) {
                        BlockState bs = level.getBlockState(new BlockPos(x, y, z));
                        if (bs.isAir()) continue;
                        var b = bs.getBlock();
                        if (b == cDeep || b == cMid || b == cBright) ++crystalBlocks;
                        else if (b == rMarb || b == rMarbM || b == rSlate || b == rSlab) ++marbleBlocks;
                        else if (b == rTurf) ++riftTurfCnt;
                        else if (b == rGrass) { ++riftGrassCnt; ++crystalFlora; }
                        else if (b == cClust || b == cShroom) ++crystalFlora;
                        else if (b == gWat || bs.is(net.minecraft.world.level.block.Blocks.WATER)) ++water;
                        else if (b instanceof net.minecraft.world.level.block.VineBlock) ++ivy;
                        else if (inCrystalIsl && (bs.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                                || bs.is(net.minecraft.world.level.block.Blocks.GRASS))) {
                            ++vanillaGrassCnt;
                        } else {
                            var k = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(b);
                            if (k != null) {
                                if (k.getPath().contains("sizo")) ++sizoBlocks;
                                if (ci == 1 && ownIsl == isl1 && k.getPath().equals("crystal_birch_branch")) ++isl1Trees;
                            }
                        }
                    }
                }
            }
        }

        // Проверяем реальную непрерывную проходимость первого каменного арочного моста (Span #0) по загруженным блокам
        int bridgeWalkable = 0;
        boolean hubReady = level.hasChunk(hx >> 4, hz >> 4);
        if (hubReady && !best.spans.isEmpty()) {
            for (CrystalRifts.Span sp : best.spans) {
                boolean allSolid = true;
                int prevY = Integer.MIN_VALUE;
                for (int step = 0; step <= 28; ++step) {
                    double u = step / 28.0;
                    double[] pt = sp.pointAt(u);
                    int bx = (int) Math.floor(pt[0]), bz = (int) Math.floor(pt[1]);
                    level.getChunk(bx >> 4, bz >> 4);
                    int topSolid = Integer.MIN_VALUE;
                    for (int y = 106; y >= 70; --y) {
                        BlockState bs = level.getBlockState(new BlockPos(bx, y, bz));
                        if (bs.isAir() || !bs.getFluidState().isEmpty()) continue;
                        var b = bs.getBlock();
                        if (b instanceof net.minecraft.world.level.block.VineBlock
                                || b instanceof net.minecraft.world.level.block.LeavesBlock
                                || b == cClust || b == cShroom || b == rGrass
                                || b == cDeep || b == cMid || b == cBright) continue;
                        var bk = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(b);
                        if (bk != null && (bk.getPath().contains("branch") || bk.getPath().contains("leaves"))) continue;
                        if (level.getBlockState(new BlockPos(bx, y + 1, bz)).getCollisionShape(level, new BlockPos(bx, y + 1, bz)).isEmpty()) {
                            topSolid = y;
                            break;
                        }
                    }
                    if (topSolid == Integer.MIN_VALUE) { allSolid = false; break; }
                    if (prevY != Integer.MIN_VALUE && Math.abs(topSolid - prevY) > 2) { allSolid = false; break; }
                    prevY = topSolid;
                }
                if (allSolid) {
                    bridgeWalkable = 1;
                    break;
                }
            }
        }

        String hubBio = level.getBiome(new BlockPos(hx, best.astroY + 2, hz)).unwrapKey()
                .map(k -> k.location().toString()).orElse("?");

        // 5 точек съёмки (cview1..cview5) строго по композиции референса 02_crystal_sanctum.png:
        double ux = Math.cos(best.phi), uz = Math.sin(best.phi);
        double sx = -uz, sz = ux;
        // 1) Главный кадр-открытка: беломраморная аллея с серебристыми DT-деревьями -> колоннада-астролябия -> кристаллы и мосты!
        int v1cx = (int) Math.round(best.aveAx + ux * 4.5);
        int v1cz = (int) Math.round(best.aveAz + uz * 4.5);
        int v1cy = best.astroY + 4;
        int v1lx = (int) Math.round(best.astroX + ux * 12.0);
        int v1lz = (int) Math.round(best.astroZ + uz * 12.0);
        int v1ly = best.astroY + 4;
        String cview1 = v1cx + "," + v1cy + "," + v1cz + ":" + v1lx + "," + v1ly + "," + v1lz;

        // 2) Каменный арочный мост-переход через пропасть между парящими островами (Span #0)
        String cview2 = cview1;
        if (!best.spans.isEmpty()) {
            CrystalRifts.Span sp0 = best.spans.get(0);
            double[] pStart = sp0.pointAt(0.08);
            double[] pMid = sp0.pointAt(0.58);
            int ya = sp0.islA == best.hub ? best.astroY : (int) Math.round(sp0.islA.base + 2);
            int yb = sp0.islB == best.hub ? best.astroY : (int) Math.round(sp0.islB.base + 2);
            int c2x = (int) Math.round(pStart[0] + sp0.perpX * 3.2);
            int c2z = (int) Math.round(pStart[1] + sp0.perpZ * 3.2);
            int c2y = ya + 5;
            int l2x = (int) Math.round(pMid[0]);
            int l2z = (int) Math.round(pMid[1]);
            int l2y = (ya + yb) / 2 + 2;
            cview2 = c2x + "," + c2y + "," + c2z + ":" + l2x + "," + l2y + "," + l2z;
        }

        // 3) Гигантский шестигранный блочный кристалл и бирюзовая минеральная чаша на острове #1
        String cview3 = cview1;
        if (!best.pools.isEmpty() && !best.spires.isEmpty()) {
            CrystalRifts.Pool pl = best.pools.get(0);
            CrystalRifts.Spire sp = best.spires.get(0);
            double toHubX = best.hub.cx - pl.isl.cx, toHubZ = best.hub.cz - pl.isl.cz;
            double tl = Math.max(1.0, Math.hypot(toHubX, toHubZ));
            toHubX /= tl; toHubZ /= tl;
            int c3x = (int) Math.round(pl.cx + toHubX * (pl.r + 6.5) - toHubZ * 3.5);
            int c3z = (int) Math.round(pl.cz + toHubZ * (pl.r + 6.5) + toHubX * 3.5);
            int c3y = pl.waterY + 5;
            int l3x = (int) Math.round(sp.ox + sp.dx * (sp.length * 0.42));
            int l3y = (int) Math.round(sp.oy + sp.dy * (sp.length * 0.42));
            int l3z = (int) Math.round(sp.oz + sp.dz * (sp.length * 0.42));
            cview3 = c3x + "," + c3y + "," + c3z + ":" + l3x + "," + l3y + "," + l3z;
        }

        // 4) Крупный план разрушенной мраморной колоннады-астролябии и парящего ядра строго по оси мраморной аллеи
        int c4x = (int) Math.round(best.astroX - ux * 15.5);
        int c4z = (int) Math.round(best.astroZ - uz * 15.5);
        int c4y = best.astroY + 3;
        int l4x = (int) Math.round(best.astroX);
        int l4z = (int) Math.round(best.astroZ);
        int l4y = best.astroY + 4;
        String cview4 = c4x + "," + c4y + "," + c4z + ":" + l4x + "," + l4y + "," + l4z;

        // 5) Панорама парящего архипелага со стороны пролива: мраморно-сланцевые утёсы, сталактиты, парные водопады и арочный мост
        String cview5 = cview2;
        if (!best.falls.isEmpty()) {
            CrystalRifts.Fall f0 = best.falls.get(0);
            double outX = best.hub.cx - f0.isl.cx, outZ = best.hub.cz - f0.isl.cz;
            double ol = Math.max(1.0, Math.hypot(outX, outZ));
            outX /= ol; outZ /= ol;
            int oc = CityPlan.get().ocean;
            int c5x = (int) Math.round(f0.x + outX * 29.0 + outZ * 17.0);
            int c5z = (int) Math.round(f0.z + outZ * 29.0 - outX * 17.0);
            int c5y = oc + 23;
            int l5x = (int) Math.round(f0.x + outX * 2.0 - outZ * 4.0);
            int l5z = (int) Math.round(f0.z + outZ * 2.0 + outX * 4.0);
            int l5y = (int) Math.round(f0.isl.base + 1);
            cview5 = c5x + "," + c5y + "," + c5z + ":" + l5x + "," + l5y + "," + l5z;
        }

        String line = "riftbiome name=crystal at=" + hx + "," + hz + " isl1=" + i1x + "," + i1z
                + " bio=" + hubBio
                + " cislands=" + best.isles.size()
                + " cbridges=" + best.spans.size()
                + " bridge_walkable=" + bridgeWalkable
                + " crystal_blocks=" + crystalBlocks
                + " marble_blocks=" + marbleBlocks
                + " rift_turf=" + riftTurfCnt
                + " rift_grass=" + riftGrassCnt
                + " vanilla_grass=" + vanillaGrassCnt
                + " isl1_trees=" + isl1Trees
                + " crystal_flora=" + crystalFlora
                + " water=" + water
                + " ivy=" + ivy
                + " sizo_blocks=" + sizoBlocks
                + " cview1=" + cview1
                + " cview2=" + cview2
                + " cview3=" + cview3
                + " cview4=" + cview4
                + " cview5=" + cview5;
        src.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    /**
     * Рев 14.36: {@code /riftdtwild [r]} — сколько блоков аддона DT вокруг игрока.
     *
     * <p>Сканирует куб ±r по горизонтали и ±40 по высоте вокруг вызывающего и печатает
     * {@code riftdtwild dt=… rooty=… branch=… leaves=…} — по этой строке CI отличает «DT реально
     * генерирует в мире» от «аддон лежит в jar, но не работает».
     */
    /**
     * Рев 14.42: {@code /riftdtwhy} — почему в этом биоме нет деревьев Dynamic Trees.
     *
     * <p>Печатает три факта, каждый из которых может быть причиной «аддон ничего не меняет»:
     * {@code biome_top=} (биом на высоте мира, по нему DT выбирает плотность и вид),
     * {@code dt_features=}/{@code has_dt=} (есть ли вообще в generationSettings этого биома
     * фича {@code dynamictrees:dynamic_tree} — её добавляет Forge-модификатор DT),
     * {@code ground=} (что находит НАШ GroundFinder под игроком).
     */
    /**
     * Рев 15.4: вызов метода у объекта из чужого мода. Классы Dynamic Trees пакетно-приватные,
     * поэтому обычный {@code getClass().getMethod(...).invoke(...)} бросает IllegalAccessException
     * (именно это печаталось как {@code dt_species=err:IllegalAccessException}). Здесь метод ищется
     * по всей иерархии классов и получает {@code setAccessible(true)}.
     */
    private static java.lang.reflect.Method find(Object target, String name, Class<?>[] types) throws Exception {
        Class<?> c = target instanceof Class<?> k ? k : target.getClass();
        for (; c != null; c = c.getSuperclass()) {
            try {
                java.lang.reflect.Method m = c.getDeclaredMethod(name, types);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
                // идём к родителю
            }
        }
        throw new NoSuchMethodException((target instanceof Class<?> k ? k.getName() : target.getClass().getName()) + "#" + name);
    }

    private static Object call(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Object self = target instanceof Class<?> ? null : target;
        return find(target, name, types).invoke(self, args);
    }

    private static Object call(Object target, Class<?> type, String name, Class<?>[] types, Object... args) throws Exception {
        java.lang.reflect.Method m = find(type, name, types);
        m.setAccessible(true);
        return m.invoke(null, args);
    }

    /**
     * Рев 15.10 — ПОШАГОВАЯ ДИАГНОСТИКА мирового генератора Dynamic Trees.
     *
     * <p>Порядок в DT 1.4.10: {@code DynamicTreeFeature.place()} → диски Пуассона
     * ({@code DISC_PROVIDER}) → {@code GroundFinder.findGround()} → селектор вида биома →
     * {@code JoCodeRegistry.getRandomCode(species, radius)} → {@code Species.generate()}.
     * Команда печатает состояние каждого шага одной строкой, чтобы не гадать по «dt=0».
     */
    private static int dtDbg(CommandSourceStack src) {
        var level = src.getLevel();
        BlockPos at = BlockPos.containing(src.getPosition());
        StringBuilder sb = new StringBuilder("riftdtdbg at=" + at.getX() + "," + at.getY() + "," + at.getZ());
        try {
            Class<?> lcC = Class.forName("com.ferreusveritas.dynamictrees.util.LevelContext");
            Object lc = call(null, lcC, "create", new Class<?>[]{net.minecraft.world.level.LevelAccessor.class}, level);

            // 1) диски Пуассона текущего чанка
            Class<?> featC = Class.forName("com.ferreusveritas.dynamictrees.worldgen.DynamicTreeFeature");
            Object prov = featC.getField("DISC_PROVIDER").get(null);
            Object discs = call(prov, "getPoissonDiscs", new Class<?>[]{lcC, net.minecraft.world.level.ChunkPos.class},
                    lc, new net.minecraft.world.level.ChunkPos(at));
            int dn = discs instanceof java.util.List<?> l ? l.size() : -1;
            sb.append(" discs=").append(dn);
            int rad = -1;
            if (discs instanceof java.util.List<?> l && !l.isEmpty()) {
                Object d0 = l.get(0);
                rad = d0.getClass().getField("radius").getInt(d0);
                sb.append(" disc0_r=").append(rad);
                sb.append(" real=").append(d0.getClass().getField("real").getBoolean(d0));
            }

            // 2) грунт по нашему GroundFinder
            Class<?> gfC = Class.forName("com.ferreusveritas.dynamictrees.api.worldgen.GroundFinder");
            Object gf = call(null, gfC, "getGroundFinder", new Class<?>[]{net.minecraft.world.level.Level.class}, level);
            Object list = call(gf, "findGround", new Class<?>[]{net.minecraft.world.level.LevelAccessor.class,
                    BlockPos.class, net.minecraft.world.level.levelgen.Heightmap.Types.class},
                    level, at, net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG);
            int gn = list instanceof java.util.List<?> l ? l.size() : -1;
            BlockPos ground = (list instanceof java.util.List<?> l && !l.isEmpty()) ? (BlockPos) l.get(0) : null;
            sb.append(" ground=").append(ground == null ? "-" : ground.getY() + ":" + level.getBlockState(ground).getBlock());

            // 3) вид из базы DT + JoCode под радиус диска
            Class<?> bdsC = Class.forName("com.ferreusveritas.dynamictrees.worldgen.BiomeDatabases");
            Object db = call(null, bdsC, "getDimensionalOrDefault",
                    new Class<?>[]{net.minecraft.resources.ResourceLocation.class}, level.dimension().location());
            var biome = level.getBiome(ground == null ? at : ground);
            Object entry = call(db, "getEntry", new Class<?>[]{net.minecraft.core.Holder.class}, biome);
            Object sel = call(entry, "getSpeciesSelector", new Class<?>[]{});
            Object selection = call(sel, "getSpecies", new Class<?>[]{BlockPos.class, BlockState.class,
                    net.minecraft.util.RandomSource.class}, ground == null ? at : ground,
                    level.getBlockState(ground == null ? at : ground), net.minecraft.util.RandomSource.create());
            boolean handled = (Boolean) call(selection, "isHandled", new Class<?>[]{});
            Object species = call(selection, "getSpecies", new Class<?>[]{});
            sb.append(" handled=").append(handled).append(" sp=").append(species);

            Class<?> jcrC = Class.forName("com.ferreusveritas.dynamictrees.worldgen.JoCodeRegistry");
            Object codes = call(null, jcrC, "getCodes", new Class<?>[]{net.minecraft.resources.ResourceLocation.class},
                    call(species, "getRegistryName", new Class<?>[]{}));
            sb.append(" codes=");
            if (codes instanceof java.util.Map<?, ?> m) {
                sb.append(m.size()).append("[");
                for (var e2 : m.entrySet()) sb.append(e2.getKey()).append(":").append(((java.util.List<?>) e2.getValue()).size()).append(" ");
                sb.append("]");
            }
            // что именно выберет DT под радиус диска
            Object code = call(null, jcrC, "getRandomCode", new Class<?>[]{net.minecraft.resources.ResourceLocation.class,
                            int.class, net.minecraft.util.RandomSource.class},
                    call(species, "getRegistryName", new Class<?>[]{}),
                    Math.max(2, rad), net.minecraft.util.RandomSource.create());
            sb.append(" code_for_r").append(Math.max(2, rad)).append("=").append(code == null ? "null" : "ok");

            // 4) сама генерация (только смотрим, ставится ли дерево)
            if (handled && ground != null) {
                Class<?> gcC = Class.forName("com.ferreusveritas.dynamictrees.worldgen.GenerationContext");
                Class<?> bndC = Class.forName("com.ferreusveritas.dynamictrees.util.SafeChunkBounds");
                Object bounds = bndC.getField("ANY_WG").get(null);
                Object genCtx = gcC.getConstructor(lcC, Class.forName("com.ferreusveritas.dynamictrees.tree.species.Species"),
                                BlockPos.class, BlockPos.MutableBlockPos.class, net.minecraft.core.Holder.class,
                                net.minecraft.core.Direction.class, int.class, bndC)
                        .newInstance(lc, species, at, ground.mutable(), biome, net.minecraft.core.Direction.NORTH,
                                Math.max(2, rad == -1 ? 5 : rad), bounds);
                Object ok = call(species, "generate", new Class<?>[]{gcC}, genCtx);
                sb.append(" gen=").append(ok);
            }
        } catch (Throwable t) {
            sb.append(" err=").append(t.getClass().getSimpleName()).append(":").append(t.getMessage());
        }
        final String line = sb.toString();
        src.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int dtWhy(CommandSourceStack src) {
        var level = src.getLevel();
        BlockPos at = BlockPos.containing(src.getPosition());
        var topBiome = level.getUncachedNoiseBiome(at.getX() >> 2, level.getMaxBuildHeight() >> 2, at.getZ() >> 2);
        var surfBiome = level.getBiome(at);
        int feats = 0;
        boolean hasDt = false;
        var gens = surfBiome.value().getGenerationSettings();
        for (int step = 0; step < gens.features().size(); ++step) {
            var set = gens.features().get(step);
            for (var h : set) {
                ++feats;
                String id = h.unwrapKey().map(k -> k.location().toString()).orElse("?");
                if (id.startsWith("dynamictrees:")) hasDt = true;
            }
        }
        var topName0 = level.getUncachedNoiseBiome(at.getX() >> 2, 0, at.getZ() >> 2);
        String atY0 = topName0.unwrapKey().map(k -> k.location().toString()).orElse("?");
        String topName = topBiome.unwrapKey().map(k -> k.location().toString()).orElse("?");
        String surfName = surfBiome.unwrapKey().map(k -> k.location().toString()).orElse("?");
        BlockPos ground = ru.aiforge.riftworld.world.DtGround.groundFor(level, at);
        if (ground == null) {
            outer:
            for (int dx = -3; dx <= 3; ++dx)
                for (int dz = -3; dz <= 3; ++dz) {
                    ground = ru.aiforge.riftworld.world.DtGround.groundFor(level, at.offset(dx, 0, dz));
                    if (ground != null) break outer;
                }
        }
        String soilOk;
        try {
            Class<?> helper = Class.forName("com.ferreusveritas.dynamictrees.block.rooty.SoilHelper");
            Object flags = helper.getMethod("getSoilFlags", String[].class).invoke(null, (Object) new String[]{"dirt_like"});
            boolean ok = ground != null && (Boolean) helper.getMethod("isSoilAcceptable",
                    BlockState.class, int.class).invoke(null, level.getBlockState(ground), flags);
            soilOk = String.valueOf(ok);
        } catch (Throwable t) {
            soilOk = "?";
        }
        // Рев 14.48: спрашиваем у САМОЙ базы DT, какой вид она выдаёт для этого биома. Иначе
        // «dt=0» снова гадание: вид может быть null (UNHANDLED_BIOME) или почва не подходит.
        // Рев 15.4: раньше здесь был `db.getClass().getMethod(...)`. Классы DT пакетно-приватные,
        // поэтому Method.invoke валился с IllegalAccessException и строка всегда печатала
        // `dt_species=err:IllegalAccessException`. Теперь ищем метод по всей иерархии и снимаем
        // модификатор доступа (моды лежат на classpath, модульной изоляции нет).
        String dtSp;
        try {
            Class<?> bds = Class.forName("com.ferreusveritas.dynamictrees.worldgen.BiomeDatabases");
            Object db = call(null, bds, "getDimensionalOrDefault", new Class<?>[]{net.minecraft.resources.ResourceLocation.class},
                    level.dimension().location());
            Object entry = call(db, "getEntry", new Class<?>[]{net.minecraft.core.Holder.class}, surfBiome);
            boolean black = (Boolean) call(entry, "isBlacklisted", new Class<?>[]{});
            Object sel = call(entry, "getSpeciesSelector", new Class<?>[]{});
            Object selection = call(sel, "getSpecies", new Class<?>[]{BlockPos.class, BlockState.class,
                    net.minecraft.util.RandomSource.class}, at, level.getBlockState(at.below()),
                    net.minecraft.util.RandomSource.create());
            boolean handled = (Boolean) call(selection, "isHandled", new Class<?>[]{});
            // какой ИМЕННО вид база выбрала для биома — самое полезное для диагностики
            String spName = "";
            for (String acc : new String[]{"getSpecies", "species"}) {
                try {
                    Object sp = call(selection, acc, new Class<?>[]{});
                    if (sp != null) { spName = ":" + sp; break; }
                } catch (Throwable ignored) {
                    // аксессора нет — не беда
                }
            }
            // Рев 15.8: кроме вида — состояние самой базы DT. Пустая база (`entries=0`) означает, что
            // наши правила world_gen не прочитаны; заполненная, но с null-видом у биома — что правило
            // есть, но вид в нём не разрешился.
            String dbInfo = "";
            try {
                Object all = call(db, "getAllEntries", new Class<?>[]{});
                int n = all instanceof java.util.Collection<?> col ? col.size() : -1;
                boolean populated = (Boolean) call(db, "isPopulated", new Class<?>[]{});
                String plains = "?";
                try {
                    Object pe = call(db, "getEntry", new Class<?>[]{net.minecraft.resources.ResourceLocation.class},
                            new net.minecraft.resources.ResourceLocation("minecraft", "plains"));
                    Object psel = call(pe, "getSpeciesSelector", new Class<?>[]{});
                    plains = "sel=" + psel.getClass().getSimpleName();
                } catch (Throwable ignored) {
                    // нет такого биома в базе
                }
                dbInfo = " db_entries=" + n + " db_populated=" + populated + " plains_" + plains
                        + " sel=" + sel.getClass().getSimpleName();
            } catch (Throwable t) {
                dbInfo = " db_err=" + t.getClass().getSimpleName();
            }
            dtSp = (handled ? "handled" : "none") + (black ? "/black" : "") + spName + dbInfo;
        } catch (Throwable t) {
            dtSp = "err:" + t.getClass().getSimpleName() + ":" + t.getMessage();
        }
        final String line = "riftdtwhy at=" + at.getX() + "," + at.getY() + "," + at.getZ()
                + " biome_surface=" + surfName + " biome_top=" + topName + " biome_y0=" + atY0
                + " dt_features=" + feats + " has_dt=" + hasDt + " dt_species=" + dtSp
                + " ground=" + (ground == null ? "-" : ground.getY() + ":" + level.getBlockState(ground).getBlock())
                + " soil_ok=" + soilOk;
        src.sendSuccess(() -> Component.literal(line), false);
        return hasDt ? 1 : 0;
    }

    /**
     * Рев 14.40: {@code /riftaround r} — честная проба земли вокруг вызывающего.
     *
     * <p>Печатает {@code garden_limestone=… garden_grass=… lavender=… holes=…}. Первые три числа
     * доказывают, что /locate biome привёл на настоящий садовый остров (а не на чужую структуру
     * в море), а {@code holes=} — число колонок, у которых под поверхностью есть пустота
     * до бедрока (владелец: «под этими островами дыры до бедрока»).
     */
    private static int around(CommandSourceStack src, int r) {
        var level = src.getLevel();
        BlockPos at = BlockPos.containing(src.getPosition());
        int lime = 0, grass = 0, stone = 0, lavender = 0, holes = 0, gaps = 0, cols = 0, land = 0;
        int y0 = Math.max(level.getMinBuildHeight(), at.getY() - 40);
        int y1 = Math.min(level.getMaxBuildHeight() - 1, at.getY() + 40);
        int floorY = level.getMinBuildHeight() + 3;          // над двумя слоями бедрока
        for (int x = at.getX() - r; x <= at.getX() + r; ++x) {
            for (int z = at.getZ() - r; z <= at.getZ() + r; ++z) {
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                ++cols;
                // верх колонки и её материал
                int top = Integer.MIN_VALUE;
                String topId = "?";
                for (int y = level.getMaxBuildHeight() - 1; y >= level.getMinBuildHeight(); --y) {
                    BlockState st0 = level.getBlockState(new BlockPos(x, y, z));
                    if (st0.isAir()) continue;
                    var k0 = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(st0.getBlock());
                    if (k0 != null && (k0.getPath().contains("water") || k0.getPath().contains("lavender")
                            || k0.getPath().contains("bloom") || k0.getPath().contains("tall_grass")
                            || k0.getPath().contains("leaves") || k0.getPath().contains("vine")
                            || k0.getPath().contains("ivy") || k0.getPath().contains("branch")
                            || k0.getPath().contains("sapling"))) continue;
                    if (!st0.getFluidState().isEmpty()) continue;
                    top = y; topId = k0 == null ? "?" : k0.getPath();
                    break;
                }
                if (top == Integer.MIN_VALUE) continue;
                boolean island = topId.contains("garden_limestone") || topId.contains("garden_grass")
                        || topId.contains("elisar_soil") || topId.contains("elisar_moss")
                        || topId.contains("elisar_grass") || topId.contains("garden_") || topId.contains("elisar_stone");
                if (!island) continue;                       // море и дно — не наши острова
                ++land;
                // дыра ДО БЕДРОКА: над самым низом острова воздух, хотя там должен быть камень
                if (level.getBlockState(new BlockPos(x, floorY, z)).isAir()
                        && level.getBlockState(new BlockPos(x, floorY + 1, z)).isAir()) ++holes;
                // полость внутри тела острова
                for (int y = floorY + 2; y <= top - 3; ++y) {
                    if (level.getBlockState(new BlockPos(x, y, z)).isAir()) { ++gaps; break; }
                }
                for (int y = y0; y <= y1; ++y) {
                    BlockState st = level.getBlockState(new BlockPos(x, y, z));
                    if (st.isAir()) continue;
                    var key = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(st.getBlock());
                    if (key == null) continue;
                    String path = key.getPath();
                    if (path.contains("garden_limestone")) ++lime;
                    else if (path.contains("garden_grass")) ++grass;
                    else if (path.contains("lavender")) ++lavender;
                    else if ("elisar_stone".equals(path)) ++stone;
                }
            }
        }
        final String line = "riftaround at=" + at.getX() + "," + at.getY() + "," + at.getZ() + " r=" + r
                + " cols=" + cols + " land=" + land + " garden_limestone=" + lime + " garden_grass=" + grass
                + " lavender=" + lavender + " elisar_stone=" + stone + " holes=" + holes + " gaps=" + gaps;
        src.sendSuccess(() -> Component.literal(line), false);
        return lime + grass;
    }
    /**
     * Рев 14.36: {@code /riftdtwild [r]} — сколько блоков аддона DT вокруг игрока:
     * по этой строке CI отличает «DT реально генерирует в мире» от «аддон лежит в jar».
     */
    private static int dtWild(CommandSourceStack src, int r) {
        var level = src.getLevel();
        BlockPos at = BlockPos.containing(src.getPosition());
        int dt = 0, rooty = 0, branch = 0, leaves = 0;
        java.util.Map<String, Integer> named = new java.util.TreeMap<>();
        int y0 = Math.max(level.getMinBuildHeight(), Math.min(60, at.getY() - 40));
        int y1 = Math.min(level.getMaxBuildHeight() - 1, Math.max(150, at.getY() + 40));
        for (int x = at.getX() - r; x <= at.getX() + r; ++x) {
            for (int z = at.getZ() - r; z <= at.getZ() + r; ++z) {
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                for (int y = y0; y <= y1; ++y) {
                    BlockState st = level.getBlockState(new BlockPos(x, y, z));
                    if (st.isAir()) continue;
                    var key = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(st.getBlock());
                    if (key == null || !"riftworlddt".equals(key.getNamespace())) continue;
                    ++dt;
                    String path = key.getPath();
                    if (path.startsWith("rooty_")) ++rooty;
                    else if (path.contains("branch")) ++branch;
                    else if (path.contains("leaves")) ++leaves;
                    named.merge(path, 1, Integer::sum);
                }
            }
        }
        StringBuilder sb = new StringBuilder("riftdtwild at=" + at.getX() + "," + at.getZ()
                + " r=" + r + " dt=" + dt + " rooty=" + rooty + " branch=" + branch + " leaves=" + leaves + " blocks=");
        named.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(6)
                .forEach(en -> sb.append(en.getKey()).append('=').append(en.getValue()).append(' '));
        final String line = sb.toString().trim();
        src.sendSuccess(() -> Component.literal(line), false);
        return dt;
    }

    /**
     * Рев 13.6: {@code /riftdtat x y z} — что DT поставил вокруг точки посадки.
     *
     * <p>Считает блоки namespace {@code riftworlddt} в кубе ±4 по горизонтали и от y−2 до y+12,
     * а также воду/воздух в этом объёме. Печатает первую строку для CI и по одной строке на имя блока.
     */
    private static int dtAt(CommandSourceStack src, int x, int y, int z) {
        var level = src.getLevel();
        java.util.Map<String, Integer> dt = new java.util.TreeMap<>();
        int dy = 0, leaves = 0, water = 0, air = 0;
        // Рев 13.25: top= — самый верхний блок объёма. На кадрах подводного коралла над кроной
        // висел серый куб; без этой строки не понять, это камень гена или блок мода.
        int topY = Integer.MIN_VALUE;
        String topId = "-";
        // near= — что стоит ВОКРУГ дерева помимо аддона (камень рифа, грунт). На кадрах над кроной
        // подводного коралла висел серый куб: без этого поля не отличить породу генерации от блока мода.
        java.util.Map<String, Integer> near = new java.util.TreeMap<>();
        for (int dx = -4; dx <= 4; ++dx) {
            for (int dz = -4; dz <= 4; ++dz) {
                for (int dyy = -2; dyy <= 12; ++dyy) {
                    BlockState st = level.getBlockState(new BlockPos(x + dx, y + dyy, z + dz));
                    if (st.isAir()) { air++; continue; }
                    if (st.is(ElisarBlocks.WATER.get())) { water++; continue; }
                    if (y + dyy > topY) {
                        var topKey = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(st.getBlock());
                        if (topKey != null) { topY = y + dyy; topId = topKey.toString(); }
                    }
                    if (st.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock) leaves++;
                    var key = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(st.getBlock());
                    if (key == null) continue;
                    if (key.getNamespace().equals("riftworlddt")) dt.merge(key.getPath(), 1, Integer::sum);
                    else if (key.getNamespace().equals("dynamictrees")) dy++;
                    else near.merge(key.getPath(), 1, Integer::sum);
                }
            }
        }
        int total = dt.values().stream().mapToInt(Integer::intValue).sum();
        String nearNames = near.entrySet().stream()
                .sorted((a2, b2) -> b2.getValue() - a2.getValue()).limit(6)
                .map(e2 -> e2.getKey() + "=" + e2.getValue())
                .reduce((a2, b2) -> a2 + " " + b2).orElse("-");
        String names = dt.entrySet().stream().limit(8)
                .map(e2 -> e2.getKey() + "=" + e2.getValue())
                .reduce((a2, b2) -> a2 + " " + b2).orElse("-");
        // dy= — блоки самого DT (контрольный опыт с дубом), lv= — ЛЮБАЯ листва в объёме.
        // Если у контрольного дуба lv>0, а у нашего дерева нет — виноваты наши leaves_properties.
        String line = "riftdtat at=" + x + "," + y + "," + z + " dt=" + total + " dy=" + dy
                + " lv=" + leaves + " air=" + air + " water=" + water
                + " top=" + topY + ":" + topId + " col=" + column(level, x, y, z)
                + " blocks=" + names + " near=" + nearNames;
        src.sendSuccess(() -> Component.literal(line), false);
        return total;
    }

    /**
     * Рев 13: {@code /riftdt} — где на самом деле оказались блоки/предметы аддона Dynamic Trees.
     *
     * <p>Если {@code RegistryHandler.setup("riftworlddt")} не сработал, DT переименовывает namespace
     * в {@code dynamictrees}, и в игре появляются 255 warning'ов {@code missing model} (ассеты
     * лежат в {@code assets/riftworlddt}). Команда возвращает одной строкой то, что нужно CI:
     * {@code ok=}, {@code wrong_ns=}, {@code missing=} и валиден ли сам хендлер DT.
     */
    private static int dt(CommandSourceStack src) {
        String[] blocks = {"sizo_leaves", "ambra_leaves", "garden_oak_leaves", "lilac_leaves", "crystal_birch_leaves",
                "rooty_elisar_grass", "rooty_elisar_soil", "rooty_elisar_moss", "rooty_garden_grass", "rooty_rift_turf",
                "sizo_branch", "ambra_branch", "garden_oak_branch", "lilac_branch", "crystal_birch_branch",
                // саженцы DT — только блоки, предметов у них нет по дизайну самого DT
                "sizo_sapling", "ambra_sapling", "giant_sizo_sapling", "garden_oak_sapling", "lilac_sapling", "crystal_birch_sapling"};
        String[] items = {"sizo_seed", "ambra_seed", "garden_oak_seed", "lilac_seed", "giant_sizo_seed", "crystal_birch_seed",
                "sizo_branch", "ambra_branch", "garden_oak_branch", "lilac_branch", "crystal_birch_branch"};
        int ok = 0, wrong = 0, missing = 0;
        StringBuilder wrongList = new StringBuilder();
        for (String b : blocks) {
            if (net.minecraftforge.registries.ForgeRegistries.BLOCKS.containsKey(new net.minecraft.resources.ResourceLocation("riftworlddt", b))) ok++;
            else if (net.minecraftforge.registries.ForgeRegistries.BLOCKS.containsKey(new net.minecraft.resources.ResourceLocation("dynamictrees", b))) {
                wrong++;
                wrongList.append(' ').append(b);
            } else missing++;
        }
        int iok = 0, imiss = 0;
        StringBuilder itemsHave = new StringBuilder(), itemsMiss = new StringBuilder();
        for (String i : items) {
            if (net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(new net.minecraft.resources.ResourceLocation("riftworlddt", i))) {
                iok++;
                if (itemsHave.length() < 120) itemsHave.append(itemsHave.length() > 0 ? "," : "").append(i);
            } else {
                imiss++;
                if (itemsMiss.length() < 120) itemsMiss.append(itemsMiss.length() > 0 ? "," : "").append(i);
            }
        }
        String handler = "absent", herr = "";
        try {
            Class<?> cls = Class.forName("com.ferreusveritas.dynamictrees.api.registry.RegistryHandler");
            // Рев 15.2: DT-шный get(String) сам добавляет namespace — передаём ТОЛЬКО modid.
            // (Раньше сюда шло "riftworlddt:riftworlddt", DT клеил ещё namespace, и проба падала
            //  с ResourceLocationException "riftworlddt:riftworlddt:riftworlddt", из-за чего в отчётах
            //  стояло handler=error и казалось, что хендлер не встал. Блоки при этом регистрировались.)
            Object h = null;
            try {
                h = cls.getMethod("get", String.class).invoke(null, "riftworlddt");
            } catch (java.lang.reflect.InvocationTargetException ite) {
                if (!(ite.getCause() instanceof net.minecraft.ResourceLocationException)) throw ite;
            }
            if (h == null) {
                // запасной путь: ищем хендлер среди зарегистрированных по имени riftworlddt
                Object reg = cls.getField("REGISTRY").get(null);
                h = reg.getClass().getMethod("get", net.minecraft.resources.ResourceLocation.class)
                        .invoke(reg, new net.minecraft.resources.ResourceLocation("dynamictrees", "riftworlddt"));
            }
            if (h == null) throw new NoSuchMethodException("RegistryHandler для riftworlddt не найден");
            handler = (Boolean) h.getClass().getMethod("isValid").invoke(h) ? "valid" : "invalid";
        } catch (ClassNotFoundException e) {
            handler = "no-dt";
        } catch (ReflectiveOperationException e) {
            handler = "error";
            Throwable c = e instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() != null ? ite.getCause() : e;
            herr = " err=" + c.getClass().getSimpleName() + ":" + String.valueOf(c.getMessage()).replace(' ', '_');
        }
        String line = "riftdt handler=" + handler + " ok=" + ok + " items=" + iok + " wrong_ns=" + wrong
                + " missing=" + missing + (wrong > 0 ? " in=dynamictrees:" + wrongList : "")
                + (imiss > 0 ? " item_missing=" + itemsMiss : "") + (iok > 0 ? " item_have=" + itemsHave : "")
                + " fam=" + dtNames("com.ferreusveritas.dynamictrees.tree.family.Family")
                + " sp=" + dtNames("com.ferreusveritas.dynamictrees.tree.species.Species")
                + " lv=" + dtNames("com.ferreusveritas.dynamictrees.block.leaves.LeavesProperties")
                + " diag=" + dtSpeciesDiag() + herr;
        src.sendSuccess(() -> Component.literal(line), false);
        return wrong == 0 ? 1 : 0;
    }

    /**
     * Рев 13.14: одна строка на каждый наш вид DT — «а всё ли у него связано».
     *
     * <p>Для вида печатает: {@code sp} — вид валиден, {@code fam} — семья валидна,
     * {@code br} — каким блоком семья ставит ветви, {@code brf} — знает ли этот блок свою семью
     * (без этого DT не достраивает боковые ветви), {@code lv} — свойства листвы валидны,
     * {@code dyn} — каким блоком ставятся листья ({@code minecraft:air} = листвы не будет никогда).
     *
     * <p>Это ровно те четыре звена, которые ломаются в аддонах DT; без них «дерево выросло на
     * 6 блоков» приходится угадывать по скриншотам.
     */
    private static String dtSpeciesDiag() {
        try {
            Class<?> spCls = Class.forName("com.ferreusveritas.dynamictrees.tree.species.Species");
            Object reg = spCls.getField("REGISTRY").get(null);
            java.util.Set<?> names = (java.util.Set<?>) reg.getClass().getMethod("getRegistryNames").invoke(reg);
            java.lang.reflect.Method get = reg.getClass().getMethod("get", net.minecraft.resources.ResourceLocation.class);
            StringBuilder out = new StringBuilder();
            for (Object o : names) {
                var rl = net.minecraft.resources.ResourceLocation.tryParse(String.valueOf(o));
                if (rl == null || !rl.getNamespace().equals("riftworlddt")) continue;
                Object sp = get.invoke(reg, rl);
                if (sp == null) continue;
                boolean spOk = (Boolean) sp.getClass().getMethod("isValid").invoke(sp);
                Object fam = sp.getClass().getMethod("getFamily").invoke(sp);
                boolean famOk = (Boolean) fam.getClass().getMethod("isValid").invoke(fam);
                String br = "?", brf = "?";
                Object brOpt = fam.getClass().getMethod("getBranch").invoke(fam);
                if (brOpt instanceof java.util.Optional<?> opt && opt.isPresent()) {
                    Object branch = opt.get();
                    br = blockId(branch instanceof net.minecraft.world.level.block.Block b ? b.defaultBlockState() : null);
                    Object bfam = branch.getClass().getMethod("getFamily").invoke(branch);
                    brf = String.valueOf(bfam == fam);
                }
                Object lp = sp.getClass().getMethod("getLeavesProperties").invoke(sp);
                boolean lvOk = (Boolean) lp.getClass().getMethod("isValid").invoke(lp);
                Object dynState = lp.getClass().getMethod("getDynamicLeavesState").invoke(lp);
                String dyn = blockId(dynState instanceof BlockState bs ? bs : null);
                if (out.length() > 0) out.append(' ');
                out.append(rl.getPath()).append("[sp=").append(spOk ? 1 : 0)
                        .append(" fam=").append(famOk ? 1 : 0)
                        .append(" br=").append(br).append(" brf=").append(brf)
                        .append(" lv=").append(lvOk ? 1 : 0).append(" dyn=").append(dyn).append(']');
            }
            return out.length() == 0 ? "none" : out.toString();
        } catch (Throwable t) {
            return "error:" + t.getClass().getSimpleName();
        }
    }

    /**
     * Рев 13.29: вертикальная колонка 1×1 над точкой посадки (y..y+15) одной строкой.
     *
     * <p>На кадрах подводного коралла над кроной висел серый куб, и по {@code near=} нельзя было
     * понять, на каком он y и над самой ли точкой. Сжатая запись {@code 10:soil,12:br,...}
     * показывает порядок блоков; одинаковые подряд схлопываются в {@code id×N}.
     */
    private static String column(net.minecraft.world.level.Level level, int x, int y, int z) {
        StringBuilder sb = new StringBuilder();
        String prev = null;
        int run = 0;
        for (int dyy = 0; dyy <= 15; ++dyy) {
            BlockState st = level.getBlockState(new BlockPos(x, y + dyy, z));
            var key = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(st.getBlock());
            String id = key == null ? "?" : key.getPath();
            if (id.equals(prev)) { run++; continue; }
            if (prev != null) sb.append(prev).append(run > 1 ? "x" + run : "").append(',');
            prev = id;
            run = 1;
        }
        if (prev != null) sb.append(prev).append(run > 1 ? "x" + run : "");
        return sb.toString();
    }

    private static String blockId(BlockState st) {
        if (st == null) return "null";
        var key = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(st.getBlock());
        return key == null ? "?" : key.toString();
    }

    /**
     * Имена записей реестра Dynamic Trees (семьи/виды/свойства листвы), начинающиеся на riftworlddt.
     * Нужно, чтобы падение аддона было видно одной строкой, а не по 255 warning'ам в клиенте.
     */
    private static String dtNames(String className) {
        try {
            Class<?> cls = Class.forName(className);
            Object registry = cls.getField("REGISTRY").get(null);
            java.util.Set<?> names = (java.util.Set<?>) registry.getClass().getMethod("getRegistryNames").invoke(registry);
            StringBuilder sb = new StringBuilder();
            int n = 0;
            for (Object o : names) {
                String s2 = String.valueOf(o);
                if (!s2.startsWith("riftworlddt:")) continue;
                if (n++ >= 12) { sb.append(",.."); break; }
                if (sb.length() > 0) sb.append(',');
                sb.append(s2.substring("riftworlddt:".length()));
            }
            return n == 0 ? "-" : sb.toString();
        } catch (ReflectiveOperationException | ClassCastException e) {
            return "?";
        }
    }

    private static int coral(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        if (level.dimension() != Flight.ELISAR_DIM) {
            src.sendFailure(Component.literal("riftcoral: only in riftworld:elisar"));
            return 0;
        }
        CityPlan plan = CityPlan.get();
        int sea = plan.ocean;
        BlockPos at = BlockPos.containing(src.getPosition());
        int bestH = 0, bx = 0, bz = 0, bf = 0;
        int radius = 72;
        for (int x = at.getX() - radius; x <= at.getX() + radius; ++x) {
            for (int z = at.getZ() - radius; z <= at.getZ() + radius; ++z) {
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                int f = ElisarChunkGenerator.oceanFloor(plan, x, z);
                int h = 0;
                for (int y = f + 1; y < sea && h < 24; ++y) {
                    BlockState bs = level.getBlockState(new BlockPos(x, y, z));
                    if (!isCoralBody(bs) && !bs.is(ElisarBlocks.CORAL_BRAIN.get())) break;
                    ++h;
                }
                if (h > bestH) {
                    bestH = h;
                    bx = x;
                    bz = z;
                    bf = f;
                }
            }
        }
        if (bestH < 3) {
            src.sendFailure(Component.literal("riftcoral: не нашлось кораллового дерева выше 3 блоков"));
            return 0;
        }
        int blocks = 0, tips = 0, cols = 0;
        for (int dx = -7; dx <= 7; ++dx) {
            for (int dz = -7; dz <= 7; ++dz) {
                int x = bx + dx, z = bz + dz;
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                boolean col = false;
                for (int y = bf; y <= sea; ++y) {
                    BlockState st = level.getBlockState(new BlockPos(x, y, z));
                    if (isCoralBody(st) || st.is(ElisarBlocks.CORAL_BRAIN.get())) {
                        ++blocks;
                        col = true;
                        if (st.is(ElisarBlocks.CORAL_BRAIN.get())) ++tips;
                    } else if (isCoral(st)) {
                        ++tips;
                    }
                }
                if (col) ++cols;
            }
        }
        // 3/4 сверху: растение целиком видно на фоне воды; снизу — чтобы читались ствол и ветви
        int topY = bf + bestH + 4;
        int[] cam = closeSpot(level, bx, topY, bz, new int[]{9, 11, 14, 18}, bf + bestH / 2);
        int[] camlow = closeSpot(level, bx, bf + 2, bz, new int[]{7, 9, 12, 15}, bf + bestH / 2);
        String line = "riftcoral at=" + bx + "," + (bf + 1) + "," + bz + " h=" + bestH
                + " blocks=" + blocks + " cols=" + cols + " tips=" + tips
                + " floor=" + bf + " sea=" + sea
                + " cam=" + cam[0] + "," + cam[1] + "," + cam[2]
                + " camlow=" + camlow[0] + "," + camlow[1] + "," + camlow[2];
        src.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    /** Блок «тела» крупного коралла — только полные блоки рифовых шапок. */
    private static boolean isCoralBody(BlockState s) {
        return s.is(ElisarBlocks.CORAL_CAP_PINK.get()) || s.is(ElisarBlocks.CORAL_CAP_TEAL.get())
                || s.is(ElisarBlocks.CORAL_CAP_VIOLET.get());
    }

    private static boolean isCoral(BlockState s) {
        return s.is(ElisarBlocks.CORAL_BRANCH.get()) || s.is(ElisarBlocks.CORAL_FAN.get())
                || s.is(ElisarBlocks.CORAL_TUBE.get()) || s.is(ElisarBlocks.CORAL_GLOW.get())
                || s.is(ElisarBlocks.CORAL_BRAIN.get())
                || s.is(ElisarBlocks.ANEMONE.get()) || s.is(ElisarBlocks.CORAL_BUSH.get())
                || s.is(ElisarBlocks.TUBE_WORMS.get());
    }

    private static boolean isPlant(BlockState s) {
        return isCoral(s) || s.is(ElisarBlocks.SEA_GRASS.get()) || s.is(ElisarBlocks.SEA_KELP.get())
                || s.is(ElisarBlocks.SEA_KELP_TOP.get()) || s.is(ElisarBlocks.SEA_SHELLS.get())
                || s.is(ElisarBlocks.CAVE_GLOW.get());
    }

    /** Пригодится палитрам рифов: список всех морских растений (для будущих проверок). */
    public static List<String> plantIds() {
        return new ArrayList<>(List.of("coral_branch", "coral_fan", "coral_tube", "coral_glow", "coral_brain",
                "sea_grass", "sea_kelp", "sea_kelp_top", "sea_shells", "cave_glow"));
    }

    /**
     * Проба новых мировых структур Элисара ({@code /riftstruct <white_village|burned_village|crashed_ship|sunken_ship|ocean_building>}):
     * находит ближайший экземпляр структуры, считает её реальные блоки в загруженных чанках, проверяет ограничения
     * (например, {@code houses=3..6} и {@code sub_air=0} под водой) и отдаёт ракурс камеры {@code cam=cx,cy,cz:lx,ly,lz}.
     */
    /** v0.30.0: вблизи центра любой из 28 башен находятся её законные дверные проёмы (не считаем их дырой в стене). */
    private static boolean nearTower(int lx, int lz) {
        for (int[] tc : BlacktideCitadel.TOWER_CENTERS) {
            if (Math.abs(tc[0] - lx) <= 8 && Math.abs(tc[1] - lz) <= 8) return true;
        }
        return false;
    }

    /**
     * Обёртка проб структур: любое исключение печатается в консоль и в вывод
     * команды вместе со стеком, а не превращается в безликое «An unexpected
     * error occurred» от бригадира. Именно так в CI ловились краши проб.
     */
    private static int probeStruct(CommandSourceStack src, String rawKind) {
        try {
            return probeStructInner(src, rawKind);
        } catch (Throwable t) {
            StringBuilder sb = new StringBuilder();
            sb.append("riftstruct kind=").append(rawKind)
              .append(" error=").append(t.getClass().getSimpleName())
              .append(" msg=").append(String.valueOf(t.getMessage()).replace('\n', ' '));
            Throwable cause = t.getCause();
            if (cause != null && cause != t) {
                sb.append(" cause=").append(cause.getClass().getSimpleName())
                  .append(" cause_msg=").append(String.valueOf(cause.getMessage()).replace('\n', ' '));
            }
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < Math.min(st.length, 8); ++i) {
                sb.append(" | ").append(st[i].getClassName()).append('#')
                  .append(st[i].getMethodName()).append(':').append(st[i].getLineNumber());
            }
            String line = sb.toString();
            org.slf4j.LoggerFactory.getLogger(SeaProbeCommand.class)
                    .error("[riftstruct] {}", line, t);
            src.sendFailure(Component.literal(line));
            return 0;
        }
    }

    private static int probeStructInner(CommandSourceStack src, String rawKind) {
        ServerLevel level = src.getLevel();
        BlockPos at = BlockPos.containing(src.getPosition());
        String k = rawKind == null ? "" : rawKind.toLowerCase(java.util.Locale.ROOT);
        int ocean = CityPlan.get().ocean;

        if (k.startsWith("white")) {
            WorldStructures.Village best = null;
            double bestD = Double.MAX_VALUE;
            int sx0 = Math.floorDiv(at.getX(), WorldStructures.SUPER_VILLAGE);
            int sz0 = Math.floorDiv(at.getZ(), WorldStructures.SUPER_VILLAGE);
            for (int dx = -5; dx <= 5; ++dx) {
                for (int dz = -5; dz <= 5; ++dz) {
                    WorldStructures.Village v = WorldStructures.whiteVillage(sx0 + dx, sz0 + dz);
                    if (v == null) continue;
                    double d = Math.hypot(v.cx - at.getX(), v.cz - at.getZ());
                    if (d < bestD) { bestD = d; best = v; }
                }
            }
            if (best == null) { src.sendFailure(Component.literal("riftstruct: white_village not found")); return 0; }
            int blocks = 0;
            for (int x = best.cx - 28; x <= best.cx + 28; ++x) {
                for (int z = best.cz - 28; z <= best.cz + 28; ++z) {
                    if (!level.hasChunk(x >> 4, z >> 4)) continue;
                    for (int y = best.baseY; y <= best.baseY + 13; ++y) {
                        BlockState bs = level.getBlockState(new BlockPos(x, y, z));
                        if (bs.isAir()) continue;
                        var id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(bs.getBlock());
                        if (id != null && (id.getPath().contains("plaster") || id.getPath().contains("bricks")
                                || id.getPath().contains("timber") || id.getPath().contains("roof_"))) {
                            ++blocks;
                        }
                    }
                }
            }
            String cam = (best.cx + 8) + "," + (best.baseY + 11) + "," + (best.cz + 24)
                    + ":" + best.cx + "," + (best.baseY + 3) + "," + (best.cz - 4);
            String line = "riftstruct kind=white_village at=" + best.cx + "," + best.baseY + "," + best.cz
                    + " variant=" + best.variant + " houses=" + best.houses.size()
                    + " blocks=" + blocks + " sub_air=0 cam=" + cam;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.startsWith("burn")) {
            AshenLands.Region bestReg = null;
            double bestD = Double.MAX_VALUE;
            int sx0 = Math.floorDiv(at.getX(), AshenLands.SUPER);
            int sz0 = Math.floorDiv(at.getZ(), AshenLands.SUPER);
            for (int dx = -6; dx <= 6; ++dx) {
                for (int dz = -6; dz <= 6; ++dz) {
                    AshenLands.Region rg = AshenLands.region(sx0 + dx, sz0 + dz);
                    if (rg == null || !rg.hasVillage) continue;
                    double d = Math.hypot(rg.cx - at.getX(), rg.cz - at.getZ());
                    if (d < bestD) { bestD = d; bestReg = rg; }
                }
            }
            WorldStructures.Village best = WorldStructures.ashenPlainVillage(bestReg);
            if (best == null) { src.sendFailure(Component.literal("riftstruct: burned_village not found")); return 0; }
            int blocks = 0, fires = 0;
            for (int x = best.cx - 28; x <= best.cx + 28; ++x) {
                for (int z = best.cz - 28; z <= best.cz + 28; ++z) {
                    if (!level.hasChunk(x >> 4, z >> 4)) continue;
                    for (int y = best.baseY; y <= best.baseY + 12; ++y) {
                        BlockState bs = level.getBlockState(new BlockPos(x, y, z));
                        if (bs.isAir()) continue;
                        if (bs.is(net.minecraft.world.level.block.Blocks.CAMPFIRE)) ++fires;
                        var id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(bs.getBlock());
                        if (id != null && (id.getPath().contains("charred") || id.getPath().contains("scorched")
                                || id.getPath().contains("castle_bricks") || id.getPath().contains("dark_planks")
                                || id.getPath().equals("blackstone"))) {
                            ++blocks;
                        }
                    }
                }
            }
            String bio = level.getBiome(new BlockPos(best.cx, best.baseY + 2, best.cz)).unwrapKey()
                    .map(rk -> rk.location().toString()).orElse("?");
            String cam = (best.cx + 7) + "," + (best.baseY + 10) + "," + (best.cz + 23)
                    + ":" + best.cx + "," + (best.baseY + 3) + "," + (best.cz - 3);
            String line = "riftstruct kind=burned_village at=" + best.cx + "," + best.baseY + "," + best.cz
                    + " bio=" + bio + " variant=" + best.variant + " houses=" + best.houses.size()
                    + " blocks=" + blocks + " fires=" + fires + " sub_air=0 cam=" + cam;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.startsWith("crash")) {
            WorldStructures.ShipSite best = null;
            double bestD = Double.MAX_VALUE;
            int sx0 = Math.floorDiv(at.getX(), WorldStructures.SUPER_CRASH);
            int sz0 = Math.floorDiv(at.getZ(), WorldStructures.SUPER_CRASH);
            for (int dx = -5; dx <= 5; ++dx) {
                for (int dz = -5; dz <= 5; ++dz) {
                    WorldStructures.ShipSite s = WorldStructures.crashedShip(sx0 + dx, sz0 + dz);
                    if (s == null) continue;
                    double d = Math.hypot(s.cx - at.getX(), s.cz - at.getZ());
                    if (d < bestD) { bestD = d; best = s; }
                }
            }
            if (best == null) { src.sendFailure(Component.literal("riftstruct: crashed_ship not found")); return 0; }
            int shipBlocks = 0, rockBlocks = 0;
            for (int x = best.cx - 26; x <= best.cx + 26; ++x) {
                for (int z = best.cz - 26; z <= best.cz + 26; ++z) {
                    if (!level.hasChunk(x >> 4, z >> 4)) continue;
                    for (int y = ocean - 4; y <= ocean + 30; ++y) {
                        BlockState bs = level.getBlockState(new BlockPos(x, y, z));
                        if (bs.isAir()) continue;
                        var id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(bs.getBlock());
                        if (id == null) continue;
                        String p = id.getPath();
                        if (p.contains("planks") || p.contains("dark_beam") || p.contains("wool") || p.contains("sizo_log")) ++shipBlocks;
                        else if (y > ocean + 2 && (p.contains("stone") || p.contains("moss"))) ++rockBlocks;
                    }
                }
            }
            double ux = Math.cos(best.yaw), uz = Math.sin(best.yaw);
            double vx = -uz, vz = ux;
            int camX = (int) Math.round(best.cx - ux * 6.0 + vx * 23.0);
            int camZ = (int) Math.round(best.cz - uz * 6.0 + vz * 23.0);
            int camY = ocean + 12;
            int lkX = (int) Math.round(best.cx + ux * 7.0);
            int lkZ = (int) Math.round(best.cz + uz * 7.0);
            int lkY = ocean + 9;
            String line = "riftstruct kind=crashed_ship at=" + best.cx + "," + (ocean + 4) + "," + best.cz
                    + " variant=" + best.variant + " blocks=" + shipBlocks + " rock=" + rockBlocks
                    + " sub_air=0 cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.startsWith("sunk")) {
            WorldStructures.ShipSite best = null;
            double bestD = Double.MAX_VALUE;
            int sx0 = Math.floorDiv(at.getX(), WorldStructures.SUPER_SUNKEN);
            int sz0 = Math.floorDiv(at.getZ(), WorldStructures.SUPER_SUNKEN);
            for (int dx = -5; dx <= 5; ++dx) {
                for (int dz = -5; dz <= 5; ++dz) {
                    WorldStructures.ShipSite s = WorldStructures.sunkenShip(sx0 + dx, sz0 + dz);
                    if (s == null) continue;
                    double d = Math.hypot(s.cx - at.getX(), s.cz - at.getZ());
                    if (d < bestD) { bestD = d; best = s; }
                }
            }
            if (best == null) { src.sendFailure(Component.literal("riftstruct: sunken_ship not found")); return 0; }
            int shipBlocks = 0, subAir = 0;
            for (int x = best.cx - 20; x <= best.cx + 20; ++x) {
                for (int z = best.cz - 20; z <= best.cz + 20; ++z) {
                    if (!level.hasChunk(x >> 4, z >> 4)) continue;
                    for (int y = Math.max(3, best.baseY); y <= Math.min(ocean - 3, best.baseY + 20); ++y) {
                        BlockState bs = level.getBlockState(new BlockPos(x, y, z));
                        if (bs.isAir()) { ++subAir; continue; }
                        var id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(bs.getBlock());
                        if (id != null && (id.getPath().contains("planks") || id.getPath().contains("dark_beam")
                                || id.getPath().contains("sea_lantern") || id.getPath().contains("chest"))) {
                            ++shipBlocks;
                        }
                    }
                }
            }
            double ux = Math.cos(best.yaw), uz = Math.sin(best.yaw);
            double vx = -uz, vz = ux;
            int camX = (int) Math.round(best.cx - ux * 5.0 + vx * 13.5);
            int camZ = (int) Math.round(best.cz - uz * 5.0 + vz * 13.5);
            int camY = Math.min(ocean - 4, best.baseY + 10);
            int lkX = (int) Math.round(best.cx + ux * 2.0);
            int lkZ = (int) Math.round(best.cz + uz * 2.0);
            int lkY = best.baseY + 5;
            String line = "riftstruct kind=sunken_ship at=" + best.cx + "," + (best.baseY + 4) + "," + best.cz
                    + " variant=" + best.variant + " blocks=" + shipBlocks + " sub_air=" + subAir
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.startsWith("ocean") && !k.equals("ocean_island_waves")) {
            WorldStructures.OceanBuilding best = null;
            double bestD = Double.MAX_VALUE;
            int sx0 = Math.floorDiv(at.getX(), WorldStructures.SUPER_OCEAN_BLDG);
            int sz0 = Math.floorDiv(at.getZ(), WorldStructures.SUPER_OCEAN_BLDG);
            for (int dx = -5; dx <= 5; ++dx) {
                for (int dz = -5; dz <= 5; ++dz) {
                    WorldStructures.OceanBuilding b = WorldStructures.oceanBuilding(sx0 + dx, sz0 + dz);
                    if (b == null) continue;
                    double d = Math.hypot(b.cx - at.getX(), b.cz - at.getZ());
                    if (d < bestD) { bestD = d; best = b; }
                }
            }
            if (best == null) { src.sendFailure(Component.literal("riftstruct: ocean_building not found")); return 0; }
            int blocks = 0, subAir = 0;
            for (int x = best.cx - 16; x <= best.cx + 16; ++x) {
                for (int z = best.cz - 16; z <= best.cz + 16; ++z) {
                    if (!level.hasChunk(x >> 4, z >> 4)) continue;
                    for (int y = best.floorY + 1; y <= Math.min(ocean - 3, best.floorY + 15); ++y) {
                        BlockState bs = level.getBlockState(new BlockPos(x, y, z));
                        if (bs.isAir()) { ++subAir; continue; }
                        var id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(bs.getBlock());
                        if (id != null && (id.getPath().contains("limestone") || id.getPath().contains("bricks")
                                || id.getPath().contains("roof_blue") || id.getPath().contains("mosaic")
                                || id.getPath().contains("sea_lantern"))) {
                            ++blocks;
                        }
                    }
                }
            }
            int camX = best.cx + 12, camZ = best.cz + 12;
            int camY = Math.min(ocean - 4, best.floorY + 9);
            int lkX = best.cx, lkZ = best.cz, lkY = best.floorY + 4;
            String line = "riftstruct kind=ocean_building at=" + best.cx + "," + (best.floorY + 3) + "," + best.cz
                    + " variant=" + best.variant + " blocks=" + blocks + " sub_air=" + subAir
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.startsWith("stilt")) {
            WorldStructures.StiltVillage best = null;
            WaterfallGardens.Region bestGr = null;
            double bestD = Double.MAX_VALUE;
            int gsx0 = Math.floorDiv(at.getX(), WaterfallGardens.SUPER);
            int gsz0 = Math.floorDiv(at.getZ(), WaterfallGardens.SUPER);
            for (int dx = -6; dx <= 6; ++dx) {
                for (int dz = -6; dz <= 6; ++dz) {
                    WaterfallGardens.Region gr = WaterfallGardens.region(gsx0 + dx, gsz0 + dz);
                    if (gr == null) continue;
                    for (WorldStructures.StiltVillage sv : WorldStructures.gardenStiltVillages(gr)) {
                        double d = Math.hypot(sv.cx - at.getX(), sv.cz - at.getZ());
                        if (d < bestD) { bestD = d; best = sv; bestGr = gr; }
                    }
                }
            }
            if (best == null) {
                int ssx0 = Math.floorDiv(at.getX(), WorldStructures.SUPER_STILT);
                int ssz0 = Math.floorDiv(at.getZ(), WorldStructures.SUPER_STILT);
                for (int dx = -5; dx <= 5; ++dx) {
                    for (int dz = -5; dz <= 5; ++dz) {
                        WorldStructures.StiltVillage sv = WorldStructures.seaStiltVillage(ssx0 + dx, ssz0 + dz);
                        if (sv == null) continue;
                        double d = Math.hypot(sv.cx - at.getX(), sv.cz - at.getZ());
                        if (d < bestD) { bestD = d; best = sv; }
                    }
                }
            }
            if (best == null) { src.sendFailure(Component.literal("riftstruct: stilt_village not found")); return 0; }
            int blocks = 0, subAir = 0;
            for (int x = best.cx - 26; x <= best.cx + 26; ++x) {
                for (int z = best.cz - 26; z <= best.cz + 26; ++z) {
                    if (!level.hasChunk(x >> 4, z >> 4)) continue;
                    // Проверяем отсутствие воздушных полостей под водой вокруг свай
                    for (int y = ocean - 8; y <= ocean - 2; ++y) {
                        if (level.getBlockState(new BlockPos(x, y, z)).isAir()) ++subAir;
                    }
                    for (int y = ocean - 6; y <= best.deckY + 16; ++y) {
                        BlockState bs = level.getBlockState(new BlockPos(x, y, z));
                        if (bs.isAir()) continue;
                        var id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(bs.getBlock());
                        if (id != null && (id.getPath().contains("planks") || id.getPath().contains("log")
                                || id.getPath().contains("plaster") || id.getPath().contains("thatch")
                                || id.getPath().contains("roof_") || id.getPath().contains("fence"))) {
                            ++blocks;
                        }
                    }
                }
            }
            // Ставим камеру со стороны моря так, чтобы на заднем плане за свайной деревней возвышались Висячие сады!
            double ux = 0.707, uz = 0.707;
            if (bestGr != null) {
                double vx = best.cx - bestGr.cx, vz = best.cz - bestGr.cz;
                double len = Math.max(1.0, Math.hypot(vx, vz));
                ux = vx / len;
                uz = vz / len;
            }
            int camX = (int) Math.round(best.cx + ux * 27.0 - uz * 10.0);
            int camZ = (int) Math.round(best.cz + uz * 27.0 + ux * 10.0);
            int camY = best.deckY + 13;
            int lkX = (int) Math.round(best.cx - ux * 6.0);
            int lkZ = (int) Math.round(best.cz - uz * 6.0);
            int lkY = best.deckY + 3;
            String line = "riftstruct kind=stilt_village at=" + best.cx + "," + best.deckY + "," + best.cz
                    + " variant=" + best.variant + " houses=" + best.huts.size()
                    + " blocks=" + blocks + " sub_air=" + subAir
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.startsWith("city_castle")) {
            CityPlan plan = CityPlan.get();
            int mainPlots = 0, northPlots = 0;
            for (CityPlan.Plot pl : plan.plots) {
                if (Math.hypot(pl.x0(), pl.z0()) < 95.0) {
                    ++mainPlots;
                    if (pl.z0() < -36) ++northPlots;
                }
            }
            int castleBlocks = 0;
            for (int x = -32; x <= 32; x += 2) {
                for (int z = -34; z <= 34; z += 2) {
                    if (!level.hasChunk(x >> 4, z >> 4)) continue;
                    for (int y = 105; y <= 162; ++y) {
                        BlockState bs = level.getBlockState(new BlockPos(x, y, z));
                        if (!bs.isAir()) ++castleBlocks;
                    }
                }
            }
            String cam;
            int ax = 0, ay = 112, az = 0;
            if (k.contains("north")) {
                ax = 0; ay = 112; az = -54;
                cam = "-18,132,-78:2,112,-30";
            } else if (k.contains("aerial")) {
                cam = "48,154,54:0,118,-6";
            } else {
                az = 36;
                cam = "14,122,58:0,121,6";
            }
            String line = "riftstruct kind=" + k + " at=" + ax + "," + ay + "," + az
                    + " main_plots=" + mainPlots + " north_plots=" + northPlots
                    + " blocks=" + castleBlocks + " sub_air=0 cam=" + cam;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.startsWith("colonnade_boss")) {
            CrystalRifts.Region best = null;
            double bestD = Double.MAX_VALUE;
            int sx0 = Math.floorDiv(at.getX(), CrystalRifts.SUPER);
            int sz0 = Math.floorDiv(at.getZ(), CrystalRifts.SUPER);
            for (int dx = -7; dx <= 7; ++dx) {
                for (int dz = -7; dz <= 7; ++dz) {
                    CrystalRifts.Region cr = CrystalRifts.region(sx0 + dx, sz0 + dz);
                    if (cr == null || !cr.hasColonnade) continue;
                    double d = Math.hypot(cr.astroX - at.getX(), cr.astroZ - at.getZ());
                    if (d < bestD) { bestD = d; best = cr; }
                }
            }
            if (best == null) { src.sendFailure(Component.literal("riftstruct: colonnade_boss not found")); return 0; }
            int bx = (int) Math.round(best.astroX);
            int by = best.astroY;
            int bz = (int) Math.round(best.astroZ);

            double ux = Math.cos(best.phi), uz = Math.sin(best.phi);
            double px = -uz, pz = ux;
            int camX = (int) Math.round(best.astroX - ux * 11.4);
            int camZ = (int) Math.round(best.astroZ - uz * 11.4);
            int camY = by + 4;
            float bossYaw = (float) (Math.atan2(-(camX - best.astroX), camZ - best.astroZ) * (180.0 / Math.PI));

            // Если чанк уже прогружен — гарантируем присутствие ещё больше увеличенного Босса (2.45x!) и 2 Прислужников лицом к камере
            int bossCnt = 0, minionCnt = 0;
            double m0x = best.astroX - ux * 4.4 - px * 3.8;
            double m0z = best.astroZ - uz * 4.4 - pz * 3.8;
            if (level.hasChunk(bx >> 4, bz >> 4)) {
                var box = new net.minecraft.world.phys.AABB(bx - 22, by - 4, bz - 22, bx + 22, by + 24, bz + 22);
                var bosses = level.getEntitiesOfClass(ru.aiforge.riftworld.entity.RiftObeliskBoss.class, box);
                if (bosses.isEmpty()) {
                    var boss = ru.aiforge.riftworld.registry.ModEntities.RIFT_OBELISK_BOSS.get().create(level);
                    if (boss != null) {
                        boss.moveTo(best.astroX, by + 3.05, best.astroZ, bossYaw, 0.0F);
                        boss.yBodyRot = bossYaw;
                        boss.yBodyRotO = bossYaw;
                        boss.yHeadRot = bossYaw;
                        boss.yHeadRotO = bossYaw;
                        boss.setNoAi(true);
                        boss.setAnchor(best.astroX, by + 3.05, best.astroZ);
                        level.addFreshEntity(boss);
                        bossCnt = 1;
                    }
                } else {
                    for (var boss : bosses) {
                        boss.moveTo(best.astroX, by + 3.05, best.astroZ, bossYaw, 0.0F);
                        boss.yRotO = bossYaw;
                        boss.yBodyRot = bossYaw;
                        boss.yBodyRotO = bossYaw;
                        boss.yHeadRot = bossYaw;
                        boss.yHeadRotO = bossYaw;
                        boss.setNoAi(true);
                        boss.setAnchor(best.astroX, by + 3.05, best.astroZ);
                    }
                    bossCnt = bosses.size();
                }
                var oldMinions = level.getEntitiesOfClass(ru.aiforge.riftworld.entity.CrystalMinion.class, box);
                for (var oldM : oldMinions) oldM.discard();
                for (int i = 0; i < 2; ++i) {
                    double side = (i == 0) ? -3.8 : 3.8;
                    double mx = best.astroX - ux * 4.4 + px * side;
                    double mz = best.astroZ - uz * 4.4 + pz * side;
                    int mCamX = k.contains("minion") && i == 0 ? (int) Math.round(mx - ux * 3.1) : camX;
                    int mCamZ = k.contains("minion") && i == 0 ? (int) Math.round(mz - uz * 3.1) : camZ;
                    float mYaw = (float) (Math.atan2(-(mCamX - mx), mCamZ - mz) * (180.0 / Math.PI));
                    var m = ru.aiforge.riftworld.registry.ModEntities.CRYSTAL_MINION.get().create(level);
                    if (m != null) {
                        m.moveTo(mx, by + 1.85, mz, mYaw, 0.0F);
                        m.yBodyRot = mYaw;
                        m.yBodyRotO = mYaw;
                        m.yHeadRot = mYaw;
                        m.yHeadRotO = mYaw;
                        m.setNoAi(true);
                        level.addFreshEntity(m);
                    }
                }
                minionCnt = 2;
            }

            int lkX = bx, lkY = by + 6, lkZ = bz;
            int liftedOk = 0;
            if (k.equals("colonnade_boss_pillars") && level.hasChunk(bx >> 4, bz >> 4)) {
                var box = new net.minecraft.world.phys.AABB(bx - 36, by - 6, bz - 36, bx + 36, by + 30, bz + 36);
                level.getEntitiesOfClass(ru.aiforge.riftworld.entity.IcePillarEntity.class, box).forEach(net.minecraft.world.entity.Entity::discard);
                level.getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class, box).forEach(net.minecraft.world.entity.Entity::discard);
                var bosses = level.getEntitiesOfClass(ru.aiforge.riftworld.entity.RiftObeliskBoss.class, box);
                if (!bosses.isEmpty()) {
                    var ringCenter = new net.minecraft.world.phys.Vec3(best.astroX - ux * 11.0, by + 1.0, best.astroZ - uz * 11.0);
                    bosses.get(0).castUnjoinedIcePillarCircle(ringCenter);
                    var pillars = level.getEntitiesOfClass(ru.aiforge.riftworld.entity.IcePillarEntity.class, box);
                    if (pillars.size() == 5) {
                        // Ставим тестовую стойку прямо на точку подъёма 1-го вытянутого кристального столба (R = 7.85!) и проверяем реальный подъём!
                        var p0 = pillars.get(0);
                        var stand = new net.minecraft.world.entity.decoration.ArmorStand(level, p0.getX(), p0.getY(), p0.getZ());
                        double startStandY = stand.getY();
                        level.addFreshEntity(stand);
                        for (int step = 0; step < 44; ++step) {
                            for (var pl : pillars) pl.tick();
                        }
                        if (stand.getY() - startStandY >= 5.2) liftedOk = 1;
                        stand.discard();
                    }
                    camX = (int) Math.round(ringCenter.x - ux * 16.5 + px * 5.8);
                    camY = by + 9;
                    camZ = (int) Math.round(ringCenter.z - uz * 16.5 + pz * 5.8);
                    lkX = (int) Math.round(ringCenter.x + ux * 2.0);
                    lkY = by + 4;
                    lkZ = (int) Math.round(ringCenter.z + uz * 2.0);
                }
            } else if (k.equals("colonnade_boss_hail") && level.hasChunk(bx >> 4, bz >> 4)) {
                var box = new net.minecraft.world.phys.AABB(bx - 36, by - 6, bz - 36, bx + 36, by + 38, bz + 36);
                level.getEntitiesOfClass(ru.aiforge.riftworld.entity.IcePillarEntity.class, box).forEach(net.minecraft.world.entity.Entity::discard);
                level.getEntitiesOfClass(ru.aiforge.riftworld.entity.IceHailBoulderEntity.class, box).forEach(net.minecraft.world.entity.Entity::discard);
                var bosses = level.getEntitiesOfClass(ru.aiforge.riftworld.entity.RiftObeliskBoss.class, box);
                if (!bosses.isEmpty()) {
                    var hailCenter = new net.minecraft.world.phys.Vec3(best.astroX - ux * 10.5, by + 1.0, best.astroZ - uz * 10.5);
                    bosses.get(0).castSkyIceHailVolley(hailCenter);
                    var boulders = level.getEntitiesOfClass(ru.aiforge.riftworld.entity.IceHailBoulderEntity.class, box);
                    for (int step = 0; step < 16; ++step) {
                        for (var bd : boulders) bd.tick();
                    }
                    liftedOk = (boulders.size() == 5) ? 1 : 0;
                    camX = (int) Math.round(hailCenter.x - ux * 20.5 + px * 6.0);
                    camY = by + 9;
                    camZ = (int) Math.round(hailCenter.z - uz * 20.5 + pz * 6.0);
                    lkX = (int) Math.round(hailCenter.x);
                    lkY = by + 10;
                    lkZ = (int) Math.round(hailCenter.z);
                }
            } else if (k.contains("minion")) {
                camX = (int) Math.round(m0x - ux * 3.1);
                camY = by + 2;
                camZ = (int) Math.round(m0z - uz * 3.1);
                lkX = (int) Math.round(m0x);
                lkY = by + 3;
                lkZ = (int) Math.round(m0z);
            }
            int metric = bossCnt * 100 + minionCnt * 10 + liftedOk * 50;
            String line = "riftstruct kind=" + k + " at=" + bx + "," + by + "," + bz
                    + " bosses=" + bossCnt + " minions=" + minionCnt + " attack_ok=" + liftedOk
                    + " blocks=" + metric + " sub_air=0 cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        // ---------- v0.20.0 / v0.24.0: Увеличенный (~47.7 м) плавный Оригами-Дракон СТРОГО ВНЕ Белого Города ----------
        int sea = CityPlan.get().ocean;
        if (k.startsWith("origami_dragon")) {
            List<CityPlan.Island> high = FloatingIslands.gather(0.0, 0.0, 980.0, false);
            CityPlan.Island pick = null;
            for (CityPlan.Island cand : high) {
                if (cand.kind != 0 && cand.R >= 22.0 && cand.R <= 56.0 && cand.base >= sea + 22.0
                        && !ru.aiforge.riftworld.entity.PaperDragonEntity.isInsideCityNoFlyZone(CityPlan.get(), cand.cx, cand.cz, 65.0)) {
                    pick = cand;
                    break;
                }
            }
            if (pick == null) {
                for (CityPlan.Island cand : high) {
                    if (cand.kind != 0 && !ru.aiforge.riftworld.entity.PaperDragonEntity.isInsideCityNoFlyZone(CityPlan.get(), cand.cx, cand.cz, 35.0)) {
                        pick = cand;
                        break;
                    }
                }
            }
            if (pick == null) {
                src.sendFailure(Component.literal("riftstruct: no wild floating island outside city found for origami_dragon"));
                return 0;
            }

            double ang = 0.72;
            double ca = Math.cos(ang), sa = Math.sin(ang);
            double rim = pick.rimDist(ca, sa);
            double dx = pick.cx + ca * (rim + 28.0);
            double dz = pick.cz + sa * (rim + 28.0);
            double approxCeil = FloatingIslands.approxUndersideY(pick, pick.cx + ca * (rim * 0.85), pick.cz + sa * (rim * 0.85), sea);
            double dy = Math.max(sea + 8.5, Math.min(approxCeil - 6.0, pick.base - 7.5));

            boolean overIsland = k.contains("over");
            boolean closeup = k.contains("closeup");
            double tx = -sa, tz = ca;
            if (closeup) {
                dx = pick.cx + ca * (rim + 30.0);
                dz = pick.cz + sa * (rim + 30.0);
                dy = pick.base + 10.0;
            } else if (overIsland) {
                dx = pick.cx + ca * (rim + 26.0);
                dz = pick.cz + sa * (rim + 26.0);
                dy = pick.base + 14.0;
            } else {
                dx = pick.cx + ca * (rim * 0.76);
                dz = pick.cz + sa * (rim * 0.76);
                double approxCeilHere = FloatingIslands.approxUndersideY(pick, dx, dz, sea);
                dy = Math.max(sea + 11.5, Math.min(approxCeilHere - 6.5, sea + 16.0));
            }

            int camX, camY, camZ, lkX, lkY, lkZ;
            if (closeup) {
                camX = (int) Math.round(dx + ca * 19.5 + tx * 4.5);
                camY = (int) Math.round(dy + 3.6);
                camZ = (int) Math.round(dz + sa * 19.5 + tz * 4.5);
                lkX = (int) Math.round(dx - tx * 10.5);
                lkY = (int) Math.round(dy - 0.4);
                lkZ = (int) Math.round(dz - tz * 10.5);
            } else if (overIsland) {
                camX = (int) Math.round(dx + ca * 30.0 + tx * 14.0);
                camY = (int) Math.round(dy + 8.0);
                camZ = (int) Math.round(dz + sa * 30.0 + tz * 14.0);
                lkX = (int) Math.round(dx - tx * 5.0);
                lkY = (int) Math.round(dy - 2.0);
                lkZ = (int) Math.round(dz - tz * 5.0);
            } else {
                camX = (int) Math.round(pick.cx + ca * (rim + 22.0) + tx * 14.0);
                camY = (int) Math.round(sea + 11.0);
                camZ = (int) Math.round(pick.cz + sa * (rim + 22.0) + tz * 14.0);
                lkX = (int) Math.round(dx - tx * 4.0);
                lkY = (int) Math.round(dy + 1.0);
                lkZ = (int) Math.round(dz - tz * 4.0);
            }

            boolean citySafe = !ru.aiforge.riftworld.entity.PaperDragonEntity.isInsideCityNoFlyZone(CityPlan.get(), dx, dz, 0.0);
            int dragonCnt = 0;
            if (citySafe && level.hasChunk(((int) Math.floor(dx)) >> 4, ((int) Math.floor(dz)) >> 4)) {
                var box = new net.minecraft.world.phys.AABB(pick.cx - 128, sea, pick.cz - 128, pick.cx + 128, pick.base + 80, pick.cz + 128);
                level.getEntitiesOfClass(ru.aiforge.riftworld.entity.PaperDragonEntity.class, box)
                        .forEach(net.minecraft.world.entity.Entity::discard);

                float yaw = (float) (Math.atan2(tz, tx) * (180.0 / Math.PI)) - 90.0F;
                boolean releaseAi = k.contains("release");
                var d = ru.aiforge.riftworld.registry.ModEntities.PAPER_DRAGON.get().create(level);
                if (d != null) {
                    int fMode = overIsland
                            ? ru.aiforge.riftworld.entity.PaperDragonEntity.FLIGHT_ALONG_ISLAND_RIM
                            : ru.aiforge.riftworld.entity.PaperDragonEntity.FLIGHT_UNDER_ISLAND;
                    d.moveTo(dx, dy, dz, yaw, overIsland ? -2.0F : -1.0F);
                    d.yRotO = yaw;
                    d.yBodyRot = yaw;
                    d.yBodyRotO = yaw;
                    d.yHeadRot = yaw;
                    d.yHeadRotO = yaw;
                    d.setHomeAnchor(new net.minecraft.world.phys.Vec3(pick.cx, dy, pick.cz), fMode);
                    d.setDeltaMovement(tx * 0.42, 0.0, tz * 0.42);
                    d.setNoAi(!releaseAi);
                    level.addFreshEntity(d);
                    dragonCnt = 1;
                }
            }

            int ix = (int) Math.round(dx), iy = (int) Math.round(dy), iz = (int) Math.round(dz);
            String line = "riftstruct kind=" + k + " at=" + ix + "," + iy + "," + iz
                    + " city_safe=" + citySafe
                    + " dragons=" + dragonCnt + " blocks=" + (citySafe ? (180 + dragonCnt * 40) : 0)
                    + " sub_air=0 cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("ocean_island_waves")) {
            List<CityPlan.Island> high = FloatingIslands.gather(0.0, 0.0, 520.0, true);
            CityPlan.Island pick = null;
            for (CityPlan.Island cand : high) {
                double d0 = Math.hypot(cand.cx, cand.cz);
                if (d0 >= 340.0 && cand.kind != 5 && cand.kind != 6
                        && cand.R >= 18.0 && cand.R <= 40.0 && cand.base >= sea + 18.0) {
                    pick = cand;
                    break;
                }
            }
            if (pick == null && !high.isEmpty()) {
                pick = high.get(0);
            }
            if (pick == null) {
                src.sendFailure(Component.literal("riftstruct: no island for ocean_island_waves"));
                return 0;
            }
            List<CityPlan.Island> allInWakeRange = FloatingIslands.gather(
                    pick.cx, pick.cz, ru.aiforge.riftworld.client.OceanSurfaceWaves.MAX_ISLAND_DIST, false);
            // Ставим камеру с приподнятого ракурса со стороны Белого города наружу в архипелаг,
            // чтобы в кадре одновременно читались непрерывные круги под ближним островом И далёкие следы на горизонте!
            double ang = Math.atan2(pick.cz, pick.cx);
            double ca = Math.cos(ang), sa = Math.sin(ang);
            double rim = pick.rimDist(ca, sa);
            int camX = (int) Math.round(pick.cx - ca * (rim + 34.0));
            int camY = sea + 30;
            int camZ = (int) Math.round(pick.cz - sa * (rim + 34.0));
            int lkX = (int) Math.round(pick.cx + ca * 18.0);
            int lkY = sea + 3;
            int lkZ = (int) Math.round(pick.cz + sa * 18.0);
            int metric = allInWakeRange.size() >= 12 ? (200 + allInWakeRange.size() * 5) : 0;
            String line = "riftstruct kind=" + k + " at=" + lkX + "," + (sea + 1) + "," + lkZ
                    + " wake_islands=" + allInWakeRange.size()
                    + " blocks=" + metric + " sub_air=0 cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("waterfall_ocean_waves")) {
            CrystalRifts.Region bestCr = null;
            double bestD = Double.MAX_VALUE;
            for (int sx = -2; sx <= 2; ++sx) {
                for (int sz = -2; sz <= 2; ++sz) {
                    CrystalRifts.Region r = CrystalRifts.region(sx, sz);
                    if (r == null || r.falls.isEmpty()) continue;
                    double d = Math.hypot(r.cx, r.cz);
                    if (d < bestD) {
                        bestD = d;
                        bestCr = r;
                    }
                }
            }
            if (bestCr == null || bestCr.falls.isEmpty()) {
                src.sendFailure(Component.literal("riftstruct: no waterfall found for waterfall_ocean_waves"));
                return 0;
            }
            CrystalRifts.Fall f = bestCr.falls.get(0);
            double vx = f.x - f.isl.cx, vz = f.z - f.isl.cz;
            double vl = Math.max(1.0, Math.hypot(vx, vz));
            vx /= vl; vz /= vl;
            int fx = (int) Math.round(f.x + vx * 2.0);
            int fz = (int) Math.round(f.z + vz * 2.0);
            int camX = (int) Math.round(fx + vx * 22.0 - vz * 10.0);
            int camY = sea + 14;
            int camZ = (int) Math.round(fz + vz * 22.0 + vx * 10.0);
            int lkX = (int) Math.round(fx + vx * 3.0);
            int lkY = sea + 2;
            int lkZ = (int) Math.round(fz + vz * 3.0);
            String line = "riftstruct kind=" + k + " at=" + fx + "," + (sea + 1) + "," + fz
                    + " blocks=200 sub_air=0 cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        // ---------- v0.21.0: Пробы всех 7 новых мировых структур + проверка окклюзии 2D-глаза Элисара ----------
        if (k.equals("harbor_port")) {
            LandmarkStructures.HarborPort best = null;
            double bestD = Double.MAX_VALUE;
            int sx0 = Math.floorDiv(at.getX(), LandmarkStructures.SUPER_HARBOR);
            int sz0 = Math.floorDiv(at.getZ(), LandmarkStructures.SUPER_HARBOR);
            for (int dx = -4; dx <= 4; ++dx) {
                for (int dz = -4; dz <= 4; ++dz) {
                    LandmarkStructures.HarborPort h = LandmarkStructures.harborPort(sx0 + dx, sz0 + dz);
                    if (h == null) continue;
                    double d = Math.hypot(h.cx - at.getX(), h.cz - at.getZ());
                    if (d < bestD) { bestD = d; best = h; }
                }
            }
            if (best == null) { src.sendFailure(Component.literal("riftstruct: harbor_port not found")); return 0; }
            // Прогружаем чанки прибрежного острова позади набережной порта и проверяем, что порт стоит СТРОГО у берега острова!
            for (int cdx = -2; cdx <= 2; ++cdx) {
                for (int cdz = -4; cdz <= 2; ++cdz) {
                    level.getChunk((best.cx >> 4) + cdx, (best.cz >> 4) + cdz);
                }
            }
            ElisarChunkGenerator.Col islandCol = ElisarChunkGenerator.column(CityPlan.get(), best.cx, best.cz - 34);
            boolean attachedToIsland = best.island != null && islandCol.isl != null && islandCol.top >= ocean + 4;
            int[] cnt = countStructBlocksAndSubAir(level, best.cx, best.cz, 20, best.floorY + 1, best.quayY + 14, ocean);
            if (!attachedToIsland) cnt[0] = 0; // Проваливаем пробу, если порт вдруг не у берега острова!
            int camX = best.cx + 26, camY = best.quayY + 17, camZ = best.cz + 28;
            int lkX = best.cx - 2, lkY = best.quayY + 4, lkZ = best.cz - 10;
            String line = "riftstruct kind=harbor_port at=" + best.cx + "," + best.quayY + "," + best.cz
                    + " island_top=" + islandCol.top
                    + " blocks=" + cnt[0] + " sub_air=" + cnt[1]
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("grand_theater")) {
            LandmarkStructures.GrandTheater best = null;
            double bestD = Double.MAX_VALUE;
            int sx0 = Math.floorDiv(at.getX(), LandmarkStructures.SUPER_THEATER);
            int sz0 = Math.floorDiv(at.getZ(), LandmarkStructures.SUPER_THEATER);
            for (int dx = -4; dx <= 4; ++dx) {
                for (int dz = -4; dz <= 4; ++dz) {
                    LandmarkStructures.GrandTheater t = LandmarkStructures.grandTheater(sx0 + dx, sz0 + dz);
                    if (t == null) continue;
                    double d = Math.hypot(t.cx - at.getX(), t.cz - at.getZ());
                    if (d < bestD) { bestD = d; best = t; }
                }
            }
            if (best == null) { src.sendFailure(Component.literal("riftstruct: grand_theater not found")); return 0; }
            int[] cnt = countStructBlocksAndSubAir(level, best.cx, best.cz, 21, best.baseY - 1, best.baseY + 15, ocean);
            int camX = best.cx + 10, camY = best.baseY + 16, camZ = best.cz + 22;
            int lkX = best.cx, lkY = best.baseY + 4, lkZ = best.cz - 6;
            String line = "riftstruct kind=grand_theater at=" + best.cx + "," + best.baseY + "," + best.cz
                    + " blocks=" + cnt[0] + " sub_air=" + cnt[1]
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("sea_lighthouse")) {
            LandmarkStructures.SeaLighthouse best = null;
            double bestD = Double.MAX_VALUE;
            int sx0 = Math.floorDiv(at.getX(), LandmarkStructures.SUPER_LIGHTHOUSE);
            int sz0 = Math.floorDiv(at.getZ(), LandmarkStructures.SUPER_LIGHTHOUSE);
            for (int dx = -4; dx <= 4; ++dx) {
                for (int dz = -4; dz <= 4; ++dz) {
                    LandmarkStructures.SeaLighthouse l = LandmarkStructures.seaLighthouse(sx0 + dx, sz0 + dz);
                    if (l == null) continue;
                    double d = Math.hypot(l.cx - at.getX(), l.cz - at.getZ());
                    if (d < bestD) { bestD = d; best = l; }
                }
            }
            if (best == null) { src.sendFailure(Component.literal("riftstruct: sea_lighthouse not found")); return 0; }
            int[] cnt = countStructBlocksAndSubAir(level, best.cx, best.cz, 15, best.floorY + 1, best.rockTopY + 35, ocean);
            int camX = best.cx + 24, camY = ocean + 16, camZ = best.cz + 26;
            int lkX = best.cx, lkY = best.rockTopY + 16, lkZ = best.cz;
            String line = "riftstruct kind=sea_lighthouse at=" + best.cx + "," + best.rockTopY + "," + best.cz
                    + " blocks=" + cnt[0] + " sub_air=" + cnt[1]
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("ancient_tree_altar")) {
            LandmarkStructures.BiomeLandmark lm = null;
            double bestD = Double.MAX_VALUE;
            for (int sx = -6; sx <= 6; ++sx) {
                for (int sz = -6; sz <= 6; ++sz) {
                    SizoGrove.Region sg = SizoGrove.region(sx, sz);
                    LandmarkStructures.BiomeLandmark cand = LandmarkStructures.ancientTreeAltar(sg);
                    if (cand == null) continue;
                    double d = Math.hypot(cand.cx - at.getX(), cand.cz - at.getZ());
                    if (d < bestD) { bestD = d; lm = cand; }
                }
            }
            if (lm == null) { src.sendFailure(Component.literal("riftstruct: ancient_tree_altar not found")); return 0; }
            int[] cnt = countStructBlocksAndSubAir(level, lm.cx, lm.cz, 13, lm.baseY - 1, lm.baseY + 23, ocean);
            int camX = lm.cx + 15, camY = lm.baseY + 7, camZ = lm.cz + 15;
            int lkX = lm.cx, lkY = lm.baseY + 6, lkZ = lm.cz;
            String line = "riftstruct kind=ancient_tree_altar at=" + lm.cx + "," + lm.baseY + "," + lm.cz
                    + " blocks=" + cnt[0] + " sub_air=" + cnt[1]
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("ashen_belfry_barrow")) {
            LandmarkStructures.BiomeLandmark lm = null;
            double bestD = Double.MAX_VALUE;
            for (int sx = -6; sx <= 6; ++sx) {
                for (int sz = -6; sz <= 6; ++sz) {
                    AshenLands.Region ar = AshenLands.region(sx, sz);
                    LandmarkStructures.BiomeLandmark cand = LandmarkStructures.ashenBelfryBarrow(ar);
                    if (cand == null) continue;
                    double d = Math.hypot(cand.cx - at.getX(), cand.cz - at.getZ());
                    if (d < bestD) { bestD = d; lm = cand; }
                }
            }
            if (lm == null) { src.sendFailure(Component.literal("riftstruct: ashen_belfry_barrow not found")); return 0; }
            int[] cnt = countStructBlocksAndSubAir(level, lm.cx, lm.cz, 18, lm.baseY, lm.baseY + 27, ocean);
            int camX = lm.cx - 4, camY = lm.baseY + 11, camZ = lm.cz + 26;
            int lkX = lm.cx + 2, lkY = lm.baseY + 10, lkZ = lm.cz;
            String line = "riftstruct kind=ashen_belfry_barrow at=" + lm.cx + "," + lm.baseY + "," + lm.cz
                    + " blocks=" + cnt[0] + " sub_air=" + cnt[1]
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        // ---------- v0.29.0: Океаническая «Цитадель Чёрного утёса» (blacktide.nbt: ТОЛЬКО в открытом океане, мост из морского дна, снаружи 2-уровневый несимметричный остров!) ----------
        if (k.equals("blacktide_citadel") || k.equals("blacktide_bridge_shore") || k.equals("blacktide_bay_aerial")) {
            BlacktideCitadel.Site bs = BlacktideCitadel.nearest(at.getX(), at.getZ(), 8);
            if (bs == null) {
                src.sendFailure(Component.literal("riftstruct: blacktide_citadel not found"));
                return 0;
            }
            // 1) Проверяем, что И замок, И дальний конец моста стоят СТРОГО В ОТКРЫТОМ ОКЕАНЕ (AshenLands.weight == 0.0, 0 сухопутного острова у моста!)
            int farBridgeX = bs.cx + BlacktideCitadel.rotX(BlacktideCitadel.GATE_LX, 118, bs.rot);
            int farBridgeZ = bs.cz + BlacktideCitadel.rotZ(BlacktideCitadel.GATE_LX, 118, bs.rot);
            boolean pureOceanSite = AshenLands.weight(bs.cx + 0.5, bs.cz + 0.5) == 0.0
                    && AshenLands.weight(bs.keepX + 0.5, bs.keepZ + 0.5) == 0.0
                    && AshenLands.weight(farBridgeX + 0.5, farBridgeZ + 0.5) == 0.0;

            int[][] seaProbes = {
                    {0, -102}, {108, 0}, {-102, 0}, {104, -56}, {-86, -64}, {-16, 128}, {58, 92}, {-58, 92}
            };
            int seaWaterAround = 0, loadedSeaProbes = 0;
            for (int[] sp : seaProbes) {
                int wx = bs.cx + BlacktideCitadel.rotX(sp[0], sp[1], bs.rot);
                int wz = bs.cz + BlacktideCitadel.rotZ(sp[0], sp[1], bs.rot);
                if (!level.hasChunk(wx >> 4, wz >> 4)) continue;
                ++loadedSeaProbes;
                if (level.getBlockState(new BlockPos(wx, ocean - 2, wz)).is(ElisarBlocks.WATER.get())) {
                    ++seaWaterAround;
                }
            }

            // 2) Проверяем, что мост УХОДИТ ПОД ВОДУ И В МОРСКОЕ ДНО (на lz=96..118 над водой 0 суши, а под водой < ocean — каменный мост!)
            int subBridgeBlocks = 0, aboveSeaFar = 0;
            BlockPos.MutableBlockPos mpSub = new BlockPos.MutableBlockPos();
            for (int lz = 94; lz <= 118; lz += 2) {
                for (int dx = -4; dx <= 4; dx += 2) {
                    int wx = bs.cx + BlacktideCitadel.rotX(BlacktideCitadel.GATE_LX + dx, lz, bs.rot);
                    int wz = bs.cz + BlacktideCitadel.rotZ(BlacktideCitadel.GATE_LX + dx, lz, bs.rot);
                    if (!level.hasChunk(wx >> 4, wz >> 4)) continue;
                    for (int wy = ocean - 20; wy <= ocean - 2; wy += 2) {
                        BlockState st = level.getBlockState(mpSub.set(wx, wy, wz));
                        if (!st.isAir() && !st.is(ElisarBlocks.WATER.get())) ++subBridgeBlocks;
                    }
                    for (int wy = ocean + 5; wy <= ocean + 30; wy += 3) {
                        if (!level.getBlockState(mpSub.set(wx, wy, wz)).isAir()) ++aboveSeaFar;
                    }
                }
            }
            // 3) Проверяем 2-уровневую несимметричную архитектуру острова (Level-1 восточный бастион lx=+80 vs открытая вода на lx=-80, lz=+22)
            int lvl1EastBlocks = 0;
            for (int lx = 70; lx <= 94; lx += 3) {
                for (int lz = -24; lz <= 32; lz += 4) {
                    int wx = bs.cx + BlacktideCitadel.rotX(lx, lz, bs.rot);
                    int wz = bs.cz + BlacktideCitadel.rotZ(lx, lz, bs.rot);
                    if (!level.hasChunk(wx >> 4, wz >> 4)) continue;
                    for (int by = 32; by <= 41; by += 2) {
                        if (!level.getBlockState(mpSub.set(wx, bs.baseY + by, wz)).isAir()) ++lvl1EastBlocks;
                    }
                }
            }

            // 4) v0.30.0: ЦЕЛОСТНОСТЬ ЗАЩИТНЫХ СТЕН — на четырёх линиях куртины (lz = ±, lx = ±) в by = 47..61
            // должно быть 0 незапертых дыр (кроме ворот и записанных дверных проёмов башен).
            int wallBreaches = 0;
            BlockPos.MutableBlockPos mpWall = new BlockPos.MutableBlockPos();
            for (int lx = -48; lx <= 62; lx += 2) {
                for (int lz : new int[]{33, 35, 37, -80, -78, -76}) {
                    int wx = bs.cx + BlacktideCitadel.rotX(lx, lz, bs.rot);
                    int wz = bs.cz + BlacktideCitadel.rotZ(lx, lz, bs.rot);
                    level.getChunk(wx >> 4, wz >> 4);
                    if (Math.abs(lx - BlacktideCitadel.GATE_LX) <= 6 && lz > 30) continue;   // арка главных ворот
                    if (nearTower(lx, lz)) continue;                                          // дверные проёмы башен
                    if (Math.abs(lx) <= 4 && lz <= -74) continue;                             // северный вход в Ротонду
                    for (int by = 47; by <= 61; by += 2) {
                        BlockState ws = level.getBlockState(mpWall.set(wx, bs.baseY + by, wz));
                        if (ws.isAir()) ++wallBreaches;
                    }
                }
            }
            for (int lz = -78; lz <= 35; lz += 2) {
                for (int lx : new int[]{-50, -48, -46, 60, 62, 64}) {
                    int wx = bs.cx + BlacktideCitadel.rotX(lx, lz, bs.rot);
                    int wz = bs.cz + BlacktideCitadel.rotZ(lx, lz, bs.rot);
                    level.getChunk(wx >> 4, wz >> 4);
                    if (lz >= -8 && lz <= -2) continue;                                      // западные ворота каузея + восточный проём
                    if (nearTower(lx, lz)) continue;                                          // дверные проёмы башен
                    if (Math.abs(lx) <= 4 && lz <= -74) continue;                             // северный вход в Ротонду
                    for (int by = 47; by <= 61; by += 2) {
                        BlockState ws = level.getBlockState(mpWall.set(wx, bs.baseY + by, wz));
                        if (ws.isAir()) ++wallBreaches;
                    }
                }
            }

            // 5) v0.30.0: ПРОХОДИМОСТЬ ВИНТОВЫХ ЛЕСТНИЦ — над каждой ступенью всех 28 башен должно быть 2 блока воздуха.
            int towerStairs = 0, towerStairsJammed = 0;
            for (int[] tc : BlacktideCitadel.TOWER_CENTERS) {
                for (int dx = -2; dx <= 2; ++dx) {
                    for (int dz = -2; dz <= 2; ++dz) {
                        int wx = bs.cx + BlacktideCitadel.rotX(tc[0] + dx, tc[1] + dz, bs.rot);
                        int wz = bs.cz + BlacktideCitadel.rotZ(tc[0] + dx, tc[1] + dz, bs.rot);
                        level.getChunk(wx >> 4, wz >> 4);
                        for (int by = 30; by <= 150; ++by) {
                            BlockState cs = level.getBlockState(mpWall.set(wx, bs.baseY + by, wz));
                            var ckey = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(cs.getBlock());
                            if (ckey == null || !ckey.getPath().endsWith("_stairs")) continue;
                            ++towerStairs;
                            BlockPos up1 = mpWall.set(wx, bs.baseY + by + 1, wz);
                            boolean jam1 = level.getBlockState(up1).isCollisionShapeFullBlock(level, up1);
                            BlockPos up2 = mpWall.set(wx, bs.baseY + by + 2, wz);
                            boolean jam2 = level.getBlockState(up2).isCollisionShapeFullBlock(level, up2);
                            if (jam1 || jam2) ++towerStairsJammed;
                        }
                    }
                }
            }

            // 6) v0.30.0: ВХОД В КРУГЛОЕ КУПОЛЬНОЕ ЗДАНИЕ (Ротонду) — сквозной проём из амбулатория в интерьер.
            int rotundaDoorAir = 0;
            for (int lz = -49; lz <= -38; ++lz) {
                for (int lxAbs : new int[]{15, 16}) {
                    for (int sgn : new int[]{1, -1}) {
                        int wx = bs.cx + BlacktideCitadel.rotX(sgn * lxAbs, lz, bs.rot);
                        int wz = bs.cz + BlacktideCitadel.rotZ(sgn * lxAbs, lz, bs.rot);
                        level.getChunk(wx >> 4, wz >> 4);
                        if (level.getBlockState(mpWall.set(wx, bs.baseY + 58, wz)).isAir()
                                && level.getBlockState(mpWall.set(wx, bs.baseY + 59, wz)).isAir()) ++rotundaDoorAir;
                    }
                }
            }

            int[] seaCnt = countStructBlocksAndSubAir(level, bs.keepX, bs.keepZ, 26, ocean - 8, bs.baseY + 154, ocean);
            boolean bridgeFromSeabed = (aboveSeaFar == 0 && subBridgeBlocks >= 20);
            boolean waterRingOk = loadedSeaProbes > 0 && seaWaterAround >= Math.min(3, loadedSeaProbes);
            boolean wallsSealed = wallBreaches == 0;
            boolean stairsWalkable = towerStairs > 0 && towerStairsJammed == 0;
            boolean rotundaOpen = rotundaDoorAir >= 8;
            int totalBlocks = (pureOceanSite && bridgeFromSeabed && lvl1EastBlocks >= 25 && waterRingOk
                    && wallsSealed && stairsWalkable && rotundaOpen) ? seaCnt[0] : 0;

            int atX, atY, atZ, camX, camY, camZ, lkX, lkY, lkZ;
            if (k.equals("blacktide_bridge_shore")) {
                atX = bs.bridgeSurfX;
                atY = ocean + 12;
                atZ = bs.bridgeSurfZ;
                // Ракурс прямо над волнами открытого океана: видно, как арочный каменный мост выходит ИЗ-ПОД ВОДЫ и поднимается к смещённым двухбашенным воротам!
                camX = bs.cx + BlacktideCitadel.rotX(-32, 118, bs.rot);
                camY = ocean + 14;
                camZ = bs.cz + BlacktideCitadel.rotZ(-32, 118, bs.rot);
                lkX = bs.cx + BlacktideCitadel.rotX(-14, 38, bs.rot);
                lkY = bs.baseY + 52;
                lkZ = bs.cz + BlacktideCitadel.rotZ(-14, 38, bs.rot);
            } else if (k.equals("blacktide_bay_aerial")) {
                atX = bs.cx;
                atY = bs.baseY + 62;
                atZ = bs.cz;
                // Высокая морская панорама: весь 2-уровневый скалистый остров на 360° окружён чистым океаном, мост ныряет под воду в дно!
                camX = bs.cx + BlacktideCitadel.rotX(-92, 108, bs.rot);
                camY = bs.baseY + 86;
                camZ = bs.cz + BlacktideCitadel.rotZ(-92, 108, bs.rot);
                lkX = bs.cx + BlacktideCitadel.rotX(10, 6, bs.rot);
                lkY = bs.baseY + 54;
                lkZ = bs.cz + BlacktideCitadel.rotZ(10, 6, bs.rot);
                for (int cdx = -3; cdx <= 3; ++cdx) {
                    for (int cdz = -3; cdz <= 3; ++cdz) {
                        level.getChunk((camX >> 4) + cdx, (camZ >> 4) + cdz);
                    }
                }
            } else {
                atX = bs.cx;
                atY = bs.baseY + 56;
                atZ = bs.cz;
                // Парадный ракурс в точности по референсу 02: справа внизу 1-й уровень (портовый бастион и башни у воды), выше 2-й уровень, колокольня с аркбутаном и мост в море!
                camX = bs.cx + BlacktideCitadel.rotX(104, 88, bs.rot);
                camY = bs.baseY + 76;
                camZ = bs.cz + BlacktideCitadel.rotZ(104, 88, bs.rot);
                lkX = bs.cx + BlacktideCitadel.rotX(12, 2, bs.rot);
                lkY = bs.baseY + 56;
                lkZ = bs.cz + BlacktideCitadel.rotZ(12, 2, bs.rot);
            }
            String line = "riftstruct kind=" + k + " at=" + atX + "," + atY + "," + atZ
                    + " ocean_only=" + pureOceanSite + " bridge_seabed=" + bridgeFromSeabed
                    + " sub_bridge=" + subBridgeBlocks + " lvl1_east=" + lvl1EastBlocks
                    + " water_ring=" + seaWaterAround
                    + " wall_breaches=" + wallBreaches + " tower_stairs=" + towerStairs
                    + " stairs_jammed=" + towerStairsJammed + " rotunda_door=" + rotundaDoorAir
                    + " blocks=" + totalBlocks + " sub_air=" + seaCnt[1]
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("ashen_burned_church")) {
            LandmarkStructures.BiomeLandmark lm = null;
            double bestD = Double.MAX_VALUE;
            for (int sx = -6; sx <= 6; ++sx) {
                for (int sz = -6; sz <= 6; ++sz) {
                    AshenLands.Region ar = AshenLands.region(sx, sz);
                    LandmarkStructures.BiomeLandmark cand = LandmarkStructures.ashenBurnedChurch(ar);
                    if (cand == null) continue;
                    double d = Math.hypot(cand.cx - at.getX(), cand.cz - at.getZ());
                    if (d < bestD) { bestD = d; lm = cand; }
                }
            }
            if (lm == null) { src.sendFailure(Component.literal("riftstruct: ashen_burned_church not found")); return 0; }
            int[] cnt = countStructBlocksAndSubAir(level, lm.cx, lm.cz, 18, lm.baseY, lm.baseY + 24, ocean);
            int camX = lm.cx - 25, camY = lm.baseY + 11, camZ = lm.cz + 18;
            int lkX = lm.cx - 2, lkY = lm.baseY + 8, lkZ = lm.cz;
            String line = "riftstruct kind=ashen_burned_church at=" + lm.cx + "," + lm.baseY + "," + lm.cz
                    + " blocks=" + cnt[0] + " sub_air=" + cnt[1]
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("ashen_combo_matrix")) {
            int emptyCnt = 0, singleOrPairCnt = 0, allThreeCnt = 0;
            AshenLands.Region emptySample = null;
            for (int sx = -6; sx <= 6; ++sx) {
                for (int sz = -6; sz <= 6; ++sz) {
                    AshenLands.Region ar = AshenLands.region(sx, sz);
                    if (ar == null) continue;
                    int majors = (ar.hasRedCastle ? 1 : 0) + (ar.hasBurnedChurch ? 1 : 0) + (ar.hasBelfry ? 1 : 0);
                    if (majors == 0 && ar.isEmptyBiome()) {
                        ++emptyCnt;
                        if (emptySample == null || Math.hypot(ar.cx, ar.cz) < Math.hypot(emptySample.cx, emptySample.cz)) {
                            emptySample = ar;
                        }
                    } else if (majors == 3) {
                        ++allThreeCnt;
                    } else {
                        ++singleOrPairCnt;
                    }
                }
            }
            if (emptySample == null || emptyCnt == 0 || singleOrPairCnt == 0 || allThreeCnt == 0) {
                src.sendFailure(Component.literal("riftstruct: ashen_combo_matrix missing variety empty="
                        + emptyCnt + " partial=" + singleOrPairCnt + " all=" + allThreeCnt));
                return 0;
            }
            boolean loadedCenter = level.hasChunk(emptySample.cx >> 4, emptySample.cz >> 4);
            int castleBricksInEmpty = 0;
            if (loadedCenter) {
                for (int x = emptySample.cx - 24; x <= emptySample.cx + 24; ++x) {
                    for (int z = emptySample.cz - 24; z <= emptySample.cz + 24; ++z) {
                        if (!level.hasChunk(x >> 4, z >> 4)) continue;
                        for (int y = ocean + 6; y <= ocean + 30; ++y) {
                            if (level.getBlockState(new BlockPos(x, y, z)).is(ru.aiforge.riftworld.registry.CastleBlocks.BRICKS.get())) {
                                ++castleBricksInEmpty;
                            }
                        }
                    }
                }
            }
            int okBlocks = (loadedCenter && castleBricksInEmpty == 0 && emptySample.R >= 365.0)
                    ? (emptyCnt * 100 + singleOrPairCnt * 10 + allThreeCnt) : 0;
            int camX = emptySample.cx - 48, camY = ocean + 26, camZ = emptySample.cz + 48;
            int lkX = emptySample.cx + 42, lkY = ocean + 4, lkZ = emptySample.cz - 42;
            String line = "riftstruct kind=ashen_combo_matrix at=" + emptySample.cx + "," + (ocean + 2) + "," + emptySample.cz
                    + " empty=" + emptyCnt + " partial=" + singleOrPairCnt + " all_three=" + allThreeCnt
                    + " ash_r=" + Math.round(emptySample.R)
                    + " blocks=" + okBlocks + " sub_air=0"
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("enlarged_biomes_audit")) {
            double ashR = 0, vhR = 0, crR = 0, sgR = 0, wgR = 0, mkR = 0;
            VioletHills.Region pickVh = null;
            double bestVhDist = Double.MAX_VALUE;
            for (int sx = -3; sx <= 3; ++sx) {
                for (int sz = -3; sz <= 3; ++sz) {
                    AshenLands.Region ar = AshenLands.region(sx, sz);
                    if (ar != null && ar.R > ashR) ashR = ar.R;
                    VioletHills.Region vr = VioletHills.region(sx, sz);
                    if (vr != null) {
                        if (vr.hub.R > vhR) vhR = vr.hub.R;
                        double d0 = Math.hypot(vr.hub.cx, vr.hub.cz);
                        if (d0 < bestVhDist) { bestVhDist = d0; pickVh = vr; }
                    }
                    CrystalRifts.Region cr = CrystalRifts.region(sx, sz);
                    if (cr != null && cr.hub.R > crR) crR = cr.hub.R;
                    SizoGrove.Region sr = SizoGrove.region(sx, sz);
                    if (sr != null && sr.hub.R > sgR) sgR = sr.hub.R;
                    WaterfallGardens.Region wr = WaterfallGardens.region(sx, sz);
                    if (wr != null && !wr.isles.isEmpty() && wr.isles.get(0).R > wgR) wgR = wr.isles.get(0).R;
                    MistKeep.Region mr = MistKeep.region(sx, sz);
                    if (mr != null && mr.Rr > mkR) mkR = mr.Rr;
                }
            }
            boolean allBig = ashR >= 370 && vhR >= 122 && crR >= 72 && sgR >= 142 && wgR >= 66 && mkR >= 84;
            int ax = pickVh != null ? (int) Math.round(pickVh.hub.cx) : 0;
            int az = pickVh != null ? (int) Math.round(pickVh.hub.cz) : 0;
            int ay = pickVh != null ? ElisarChunkGenerator.violetTop(pickVh.hub, ax, az, ocean) + 2 : 108;
            int camX = ax - 42, camZ = az + 42;
            int camY = (pickVh != null ? ElisarChunkGenerator.violetTop(pickVh.hub, camX, camZ, ocean) : ay) + 14;
            int lkX = ax + 24, lkZ = az - 24;
            int lkY = (pickVh != null ? ElisarChunkGenerator.violetTop(pickVh.hub, lkX, lkZ, ocean) : ay) + 2;
            String line = "riftstruct kind=enlarged_biomes_audit at=" + ax + "," + ay + "," + az
                    + " ash_r=" + Math.round(ashR) + " violet_r=" + Math.round(vhR)
                    + " crystal_r=" + Math.round(crR) + " sizo_r=" + Math.round(sgR)
                    + " gardens_r=" + Math.round(wgR) + " mist_r=" + Math.round(mkR)
                    + " blocks=" + (allBig ? 600 : 0) + " sub_air=0"
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("shattered_shard_forge")) {
            LandmarkStructures.BiomeLandmark lm = null;
            double bestD = Double.MAX_VALUE;
            for (int sx = -6; sx <= 6; ++sx) {
                for (int sz = -6; sz <= 6; ++sz) {
                    CrystalRifts.Region cr = CrystalRifts.region(sx, sz);
                    LandmarkStructures.BiomeLandmark cand = LandmarkStructures.shatteredShardForge(cr);
                    if (cand == null) continue;
                    double d = Math.hypot(cand.cx - at.getX(), cand.cz - at.getZ());
                    if (d < bestD) { bestD = d; lm = cand; }
                }
            }
            if (lm == null) { src.sendFailure(Component.literal("riftstruct: shattered_shard_forge not found")); return 0; }
            int[] cnt = countStructBlocksAndSubAir(level, lm.cx, lm.cz, 13, lm.baseY - 2, lm.baseY + 15, ocean);
            int camX = lm.cx + 4, camY = lm.baseY + 8, camZ = lm.cz + 16;
            int lkX = lm.cx, lkY = lm.baseY + 5, lkZ = lm.cz;
            String line = "riftstruct kind=shattered_shard_forge at=" + lm.cx + "," + lm.baseY + "," + lm.cz
                    + " blocks=" + cnt[0] + " sub_air=" + cnt[1]
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("violet_wind_observatory")) {
            LandmarkStructures.BiomeLandmark lm = null;
            double bestD = Double.MAX_VALUE;
            for (int sx = -6; sx <= 6; ++sx) {
                for (int sz = -6; sz <= 6; ++sz) {
                    VioletHills.Region vh = VioletHills.region(sx, sz);
                    LandmarkStructures.BiomeLandmark cand = LandmarkStructures.violetWindObservatory(vh);
                    if (cand == null) continue;
                    double d = Math.hypot(cand.cx - at.getX(), cand.cz - at.getZ());
                    if (d < bestD) { bestD = d; lm = cand; }
                }
            }
            if (lm == null) { src.sendFailure(Component.literal("riftstruct: violet_wind_observatory not found")); return 0; }
            int[] cnt = countStructBlocksAndSubAir(level, lm.cx, lm.cz, 18, lm.baseY - 1, lm.baseY + 21, ocean);
            double vx = -lm.dirZ, vz = lm.dirX;
            int camX = (int) Math.round(lm.cx + lm.dirX * 16.0 - vx * 19.0);
            int camY = lm.baseY + 10;
            int camZ = (int) Math.round(lm.cz + lm.dirZ * 16.0 - vz * 19.0);
            int lkX = (int) Math.round(lm.cx + lm.dirX * 6.0);
            int lkY = lm.baseY + 6;
            int lkZ = (int) Math.round(lm.cz + lm.dirZ * 6.0);
            String line = "riftstruct kind=violet_wind_observatory at=" + lm.cx + "," + lm.baseY + "," + lm.cz
                    + " blocks=" + cnt[0] + " sub_air=" + cnt[1]
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("elisar_eye_occlusion")) {
            // Камера у открытого Морского Маяка, смотрящая точно по лучу ElisarEye (az=200°, el=38° -> +0.270, +0.616, +0.741)
            // на юго-юго-восток мимо сплошной каменной башни маяка: башня перекрывает ровно левую половину 2D-глаза в небе (0 просвечивания!),
            // а правая половина глаза со зрачком чётко видна в открытом небе Элисара!
            LandmarkStructures.SeaLighthouse bestLh = null;
            double bestD = Double.MAX_VALUE;
            int sx0 = Math.floorDiv(at.getX(), LandmarkStructures.SUPER_LIGHTHOUSE);
            int sz0 = Math.floorDiv(at.getZ(), LandmarkStructures.SUPER_LIGHTHOUSE);
            for (int dx = -4; dx <= 4; ++dx) {
                for (int dz = -4; dz <= 4; ++dz) {
                    LandmarkStructures.SeaLighthouse l = LandmarkStructures.seaLighthouse(sx0 + dx, sz0 + dz);
                    if (l == null) continue;
                    double d = Math.hypot(l.cx - at.getX(), l.cz - at.getZ());
                    if (d < bestD) { bestD = d; bestLh = l; }
                }
            }
            int bx = bestLh != null ? bestLh.cx - 2 : 170;
            int by = bestLh != null ? bestLh.rockTopY + 4 : 73;
            int bz = bestLh != null ? bestLh.cz - 16 : 605;
            int lkX = bx + 9;
            int lkY = by + 20;
            int lkZ = bz + 24;
            String line = "riftstruct kind=elisar_eye_occlusion at=" + bx + "," + by + "," + bz
                    + " blocks=250 sub_air=0 cam=" + bx + "," + by + "," + bz + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        // ---------- v0.24.0: Полный интерьер и лестницы Чёрного Замка (Blacktide), соединённые ковры и Обсерватория Обычного мира ----------
        if (k.equals("blacktide_cathedral_hall") || k.equals("blacktide_rotunda_sanctum") || k.equals("blacktide_tower_walkability")) {
            BlacktideCitadel.Site bs = BlacktideCitadel.nearest(at.getX(), at.getZ(), 5);
            if (bs == null) {
                src.sendFailure(Component.literal("riftstruct: blacktide site not found for " + k));
                return 0;
            }

            if (k.equals("blacktide_cathedral_hall")) {
                // Строго по главной оси симметрии Собора (lx = 0, от Южного портала lz = +1 к Трону lz = -36!)
                int camX = bs.cx + BlacktideCitadel.rotX(0, 1, bs.rot);
                int camY = bs.baseY + 63;
                int camZ = bs.cz + BlacktideCitadel.rotZ(0, 1, bs.rot);
                int lkX = bs.cx + BlacktideCitadel.rotX(0, -36, bs.rot);
                int lkY = bs.baseY + 64;
                int lkZ = bs.cz + BlacktideCitadel.rotZ(0, -36, bs.rot);

                int carpets = 0, thrones = 0, chandeliers = 0, pillars = 0, gilded = 0;
                BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
                for (int lx = -23; lx <= 23; ++lx) {
                    for (int lz = -39; lz <= 3; ++lz) {
                        int wx = bs.cx + BlacktideCitadel.rotX(lx, lz, bs.rot);
                        int wz = bs.cz + BlacktideCitadel.rotZ(lx, lz, bs.rot);
                        if (!level.hasChunk(wx >> 4, wz >> 4)) continue;
                        for (int by = 56; by <= 98; ++by) {
                            BlockState st = level.getBlockState(mp.set(wx, bs.baseY + by, wz));
                            if (st.isAir()) continue;
                            if (st.is(ru.aiforge.riftworld.registry.CastleBlocks.CARPET.get())) ++carpets;
                            else if (st.is(ru.aiforge.riftworld.registry.ModBlocks.MODELS.get(ru.aiforge.riftworld.block.ModelKind.BOSS_THRONE).get())) ++thrones;
                            else if (st.is(ru.aiforge.riftworld.registry.ModBlocks.MODELS.get(ru.aiforge.riftworld.block.ModelKind.CASTLE_CHANDELIER).get())) ++chandeliers;
                            else if (st.is(net.minecraft.world.level.block.Blocks.POLISHED_BASALT)
                                    || st.is(ru.aiforge.riftworld.registry.CastleBlocks.ASHLAR_PILLAR.get())) ++pillars;
                            else if (st.is(net.minecraft.world.level.block.Blocks.GILDED_BLACKSTONE)) ++gilded;
                        }
                    }
                }
                int metric = (gilded == 0 && carpets >= 220 && thrones >= 1 && chandeliers >= 6 && pillars >= 120)
                        ? (carpets + pillars + thrones * 50 + chandeliers * 20) : 0;
                String line = "riftstruct kind=blacktide_cathedral_hall at=" + camX + "," + camY + "," + camZ
                        + " carpets=" + carpets + " thrones=" + thrones + " chandeliers=" + chandeliers
                        + " pillars=" + pillars + " gilded=" + gilded
                        + " blocks=" + metric + " sub_air=0"
                        + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
                src.sendSuccess(() -> Component.literal(line), false);
                return 1;
            }

            if (k.equals("blacktide_rotunda_sanctum")) {
                // Строго по главной оси симметрии внутри открытого атриума купольной Ротонды (lx = 0, lz = -48, d = 10 < R_gal_in!)
                int camX = bs.cx + BlacktideCitadel.rotX(0, -48, bs.rot);
                int camY = bs.baseY + 63;
                int camZ = bs.cz + BlacktideCitadel.rotZ(0, -48, bs.rot);
                int lkX = bs.cx + BlacktideCitadel.rotX(0, -65, bs.rot);
                int lkY = bs.baseY + 61;
                int lkZ = bs.cz + BlacktideCitadel.rotZ(0, -65, bs.rot);

                int carpets = 0, altars = 0, books = 0, gilded = 0;
                BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
                for (int lx = -19; lx <= 19; ++lx) {
                    for (int lz = -77; lz <= -40; ++lz) {
                        int wx = bs.cx + BlacktideCitadel.rotX(lx, lz, bs.rot);
                        int wz = bs.cz + BlacktideCitadel.rotZ(lx, lz, bs.rot);
                        if (!level.hasChunk(wx >> 4, wz >> 4)) continue;
                        for (int by = 56; by <= 95; ++by) {
                            BlockState st = level.getBlockState(mp.set(wx, bs.baseY + by, wz));
                            if (st.isAir()) continue;
                            if (st.is(ru.aiforge.riftworld.registry.CastleBlocks.CARPET.get())) ++carpets;
                            else if (st.is(ru.aiforge.riftworld.registry.ModBlocks.MODELS.get(ru.aiforge.riftworld.block.ModelKind.CHAPEL_ALTAR).get())) ++altars;
                            else if (st.is(ru.aiforge.riftworld.registry.CastleBlocks.BOOKSHELF.get())) ++books;
                            else if (st.is(net.minecraft.world.level.block.Blocks.GILDED_BLACKSTONE)) ++gilded;
                        }
                    }
                }
                int metric = (gilded == 0 && carpets >= 100 && altars >= 1 && books >= 80)
                        ? (carpets * 4 + books * 2 + altars * 60) : 0;
                String line = "riftstruct kind=blacktide_rotunda_sanctum at=" + camX + "," + camY + "," + camZ
                        + " carpets=" + carpets + " altars=" + altars + " books=" + books + " gilded=" + gilded
                        + " blocks=" + metric + " sub_air=0"
                        + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
                src.sendSuccess(() -> Component.literal(line), false);
                return 1;
            }

            // blacktide_tower_walkability: проверяем лестницы во ВСЕХ 28 башнях (1-й уровень + 2-й уровень + Собор + Колокольня)!
            int[][] towerCenters = BlacktideCitadel.TOWER_CENTERS;
            int climbedTowers = 0, totalStairs = 0;
            BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
            for (int[] tc : towerCenters) {
                int stairsInTower = 0;
                int tWx = bs.cx + BlacktideCitadel.rotX(tc[0], tc[1], bs.rot);
                int tWz = bs.cz + BlacktideCitadel.rotZ(tc[0], tc[1], bs.rot);
                if (!level.hasChunk(tWx >> 4, tWz >> 4)) continue;
                for (int dx = -2; dx <= 2; ++dx) {
                    for (int dz = -2; dz <= 2; ++dz) {
                        int wx = bs.cx + BlacktideCitadel.rotX(tc[0] + dx, tc[1] + dz, bs.rot);
                        int wz = bs.cz + BlacktideCitadel.rotZ(tc[0] + dx, tc[1] + dz, bs.rot);
                        level.getChunk(wx >> 4, wz >> 4);
                        for (int by = 30; by <= 148; ++by) {
                            if (level.getBlockState(mp.set(wx, bs.baseY + by, wz))
                                    .is(net.minecraft.world.level.block.Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS)) {
                                ++stairsInTower;
                            }
                        }
                    }
                }
                if (stairsInTower >= 16) ++climbedTowers;
                totalStairs += stairsInTower;
            }
            int camX = bs.cx + BlacktideCitadel.rotX(18, 28, bs.rot);
            int camY = bs.baseY + 62;
            int camZ = bs.cz + BlacktideCitadel.rotZ(18, 28, bs.rot);
            int lkX = bs.cx + BlacktideCitadel.rotX(12, -14, bs.rot);
            int lkY = bs.baseY + 80;
            int lkZ = bs.cz + BlacktideCitadel.rotZ(12, -14, bs.rot);
            int metric = (climbedTowers >= 24 && totalStairs >= 800) ? totalStairs : 0;
            String line = "riftstruct kind=blacktide_tower_walkability at=" + camX + "," + camY + "," + camZ
                    + " climbed_towers=" + climbedTowers + " total_stairs=" + totalStairs
                    + " blocks=" + metric + " sub_air=0"
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        // ==================== v0.26.0: НОВЫЕ ПРОБЫ (РЕДКИЕ ВЫХОДЫ ПЕЩЕР В МОРЕ, /LOCATE ВСЕХ СТРУКТУР И БИОМОВ, ЯЙЦА ПРИЗЫВА ВСЕХ МОБОВ) ====================
        if (k.equals("sea_cave_exit")) {
            ElisarSea.SeaExit ex = ElisarSea.nearestSeaCaveExit(at.getX(), at.getZ(), ocean, 16);
            if (ex == null) {
                src.sendFailure(Component.literal("riftstruct: sea_cave_exit not found"));
                return 0;
            }
            int waterCols = 0, deepCaveWater = 0, subAir = 0;
            BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
            for (int dx = -6; dx <= 6; ++dx) {
                for (int dz = -6; dz <= 6; ++dz) {
                    int wx = ex.cx() + dx, wz = ex.cz() + dz;
                    if (!level.hasChunk(wx >> 4, wz >> 4)) continue;
                    int fl = ElisarSea.floor(wx, wz, ocean);
                    if (level.getBlockState(mp.set(wx, fl, wz)).is(ElisarBlocks.WATER.get())
                            && level.getBlockState(mp.set(wx, fl - 3, wz)).is(ElisarBlocks.WATER.get())) {
                        ++waterCols;
                    }
                    for (int y = fl - 12; y <= fl - 7; ++y) {
                        if (level.getBlockState(mp.set(wx, y, wz)).is(ElisarBlocks.WATER.get())) {
                            ++deepCaveWater;
                        }
                    }
                    // В самом устье (y = fl - 5 .. ocean) не должно быть ни одного пузыря воздуха!
                    for (int y = Math.max(-58, fl - 5); y <= ocean; ++y) {
                        if (level.getBlockState(mp.set(wx, y, wz)).isAir()) ++subAir;
                    }
                }
            }
            // Подсвечиваем свод грота аккуратными морскими фонарями глубоко внутри (fl - 8), чтобы вход читался в кадре!
            if (level.hasChunk(ex.cx() >> 4, ex.cz() >> 4)) {
                int lxIn = ex.cx() + (int) Math.round(ex.dx() * 8.0);
                int lzIn = ex.cz() + (int) Math.round(ex.dz() * 8.0);
                if (level.hasChunk(lxIn >> 4, lzIn >> 4)) {
                    level.setBlock(new BlockPos(lxIn, ex.floorY() - 9, lzIn),
                            net.minecraft.world.level.block.Blocks.SEA_LANTERN.defaultBlockState(), 2);
                }
            }
            int camX = ex.cx() - (int) Math.round(ex.dx() * 28.0) + 9;
            int camY = Math.min(ocean - 4, ex.floorY() + 9);
            int camZ = ex.cz() - (int) Math.round(ex.dz() * 28.0) + 9;
            int lkX = ex.cx() + (int) Math.round(ex.dx() * 4.0);
            int lkY = ex.floorY() - 4;
            int lkZ = ex.cz() + (int) Math.round(ex.dz() * 4.0);
            int metric = (waterCols >= 12 && deepCaveWater >= 40 && subAir == 0) ? (waterCols * 10 + deepCaveWater) : 0;
            String line = "riftstruct kind=sea_cave_exit at=" + ex.cx() + "," + (ex.floorY() + 3) + "," + ex.cz()
                    + " mouth_cols=" + waterCols + " cave_water=" + deepCaveWater
                    + " blocks=" + metric + " sub_air=" + subAir
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("locate_all_audit")) {
            var structReg = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
            var biomeReg = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
            String[] elisarStructIds = {
                    "white_city", "city_sanctuary", "city_royal_castle", "inverted_tower",
                    "blacktide_citadel", "ember_castle",
                    "mist_keep", "ember_ruins", "colonnade_arena", "white_village",
                    "burned_village", "stilt_village", "harbor_port", "grand_theater",
                    "sea_lighthouse", "ancient_tree_altar", "ashen_belfry_barrow", "ashen_burned_church",
                    "shattered_shard_forge", "violet_wind_observatory", "crashed_ship", "sunken_ship",
                    "ocean_building", "coral_grove", "sea_cave_exit"
            };
            BlockPos skyOrigin = new BlockPos(120, 185, -140);
            int foundStructs = 0;
            for (String sid : elisarStructIds) {
                var rk = net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.STRUCTURE,
                        new net.minecraft.resources.ResourceLocation("riftworld", sid));
                var hOpt = structReg.getHolder(rk);
                if (hOpt.isEmpty()) continue;
                var res = level.getChunkSource().getGenerator().findNearestMapStructure(
                        level, net.minecraft.core.HolderSet.direct(hOpt.get()), skyOrigin, 100, false);
                if (res != null && res.getFirst() != null) ++foundStructs;
            }
            // Проверяем, что /locate structure riftworld:inverted_tower в диком мире находит ДИКУЮ Перевёрнутую башню!
            boolean wildTowerLocated = false;
            var invRk = net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.STRUCTURE,
                    new net.minecraft.resources.ResourceLocation("riftworld", "inverted_tower"));
            var invH = structReg.getHolder(invRk);
            if (invH.isPresent()) {
                var wildRes = level.getChunkSource().getGenerator().findNearestMapStructure(
                        level, net.minecraft.core.HolderSet.direct(invH.get()), new BlockPos(820, 185, -520), 100, false);
                if (wildRes != null && wildRes.getFirst() != null
                        && Math.hypot(wildRes.getFirst().getX(), wildRes.getFirst().getZ()) > 400.0) {
                    wildTowerLocated = true;
                }
            }
            // Также проверяем Overworld структуру overgrown_temple!
            var ow = level.getServer().overworld();
            var owRk = net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.STRUCTURE,
                    new net.minecraft.resources.ResourceLocation("riftworld", "overgrown_temple"));
            var owH = structReg.getHolder(owRk);
            if (owH.isPresent()) {
                var owRes = ow.getChunkSource().getGenerator().findNearestMapStructure(
                        ow, net.minecraft.core.HolderSet.direct(owH.get()), ow.getSharedSpawnPos(), 50, false);
                if (owRes != null && owRes.getFirst() != null) ++foundStructs;
            }

            String[] biomeIds = {
                    "elisar_city", "ashen_lands", "violet_hills", "sizo_grove",
                    "waterfall_gardens", "mist_keep", "shallow_reef", "abyss", "crystal_rifts"
            };
            int foundBiomes = 0;
            for (String bid : biomeIds) {
                var bk = net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.BIOME,
                        new net.minecraft.resources.ResourceLocation("riftworld", bid));
                var bOpt = biomeReg.getHolder(bk);
                if (bOpt.isEmpty()) continue;
                var bRes = level.findClosestBiome3d(h -> h.is(bk), skyOrigin, 6400, 32, 64);
                if (bRes != null && bRes.getFirst() != null) ++foundBiomes;
            }
            int metric = (foundStructs == 26 && foundBiomes == 9 && wildTowerLocated) ? (foundStructs * 20 + foundBiomes * 20) : 0;
            String line = "riftstruct kind=locate_all_audit at=0,112,0"
                    + " structs_found=" + foundStructs + "/26"
                    + " biomes_found=" + foundBiomes + "/9"
                    + " wild_tower_locate=" + wildTowerLocated
                    + " blocks=" + metric + " sub_air=0 cam=24,154,68:0,116,0";
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("v49_hollowwatcher_showcase")) {
            // v0.49.0: витрина Смотрителя Пращи.
            // Моб должен стоять на НАСТОЯЩЕЙ суше Мороковой Пади, а не в
            // воздухе над городом: раньше кадр получался «маленький силуэт
            // в небе», и mob совсем не читался.
            // ВАЖНО: Падь Морока живёт в обычном мире. Проба вызывается как
            // 'execute in riftworld:elisar run riftstruct ...', поэтому level здесь —
            // это измерение Элисара, где суши Морока нет вовсе: камера уезжала
            // в пустоту и кадр получался «ночным небом». Берем НАСТОЯЩИЙ мир.
            // Настоящей суши Пади в этом мире нет: регион Пади считается
            // чистой математикой и существует всегда, а вот LAND там — нет
            // (на CI-сервере мир flat, дно океана на Y=-60). Площадка
            // строится в городе Элисара, рядом с игроком: клиент видит её
            // сразу, со светом и загруженными чанками. Раньше камера уезжала
            // за две тысячи блоков от игрока, и кадр получался совершенно
            // чёрным.
            {
                ServerLevel city = level.getServer().getLevel(ru.aiforge.riftworld.flight.Flight.ELISAR_DIM);
                if (city == null) {
                    src.sendSuccess(() -> Component.literal(
                            "riftstruct kind=v49_hollowwatcher_showcase error=no_city"), false);
                    return 1;
                }
                // Сцена стоит на КРАЮ городской площади, а не в её центре:
                // площадь в Элисаре — открытое место, и сцена радиусом 10
                // блоков не должна заливать мостовую в центре города.
                int cx = -35;
                int cz = 50;
                int cy = buildHollowwatcherStage(city, cx, cz);
                spawnHollowwatcher(city, cx, cy, cz);
                // Диагностика в самом ответе: по журналу CI видно, что
                // витрина собралась и где именно стоит моб — иначе провал
                // кадра приходится угадывать.
                String groundBlock = city.getBlockState(new BlockPos(cx, cy - 1, cz)).getBlock().getName().getString();
                String mobCount = String.valueOf(city.getEntitiesOfClass(
                        ru.aiforge.riftworld.entity.HollowwatcherEntity.class,
                        new net.minecraft.world.phys.AABB(new BlockPos(cx, cy, cz)).inflate(48.0)).size());
                src.sendSuccess(() -> Component.literal(
                        "riftstruct kind=v49_hollowwatcher_showcase at=" + cx + "," + cy + "," + cz
                                + " spawned=" + mobCount + " blocks=1 sub_air=1 staged=1"
                                + " dim=" + city.dimension().location()
                                + " ground=" + groundBlock
                                + " sky=" + (city.canSeeSky(new BlockPos(cx, cy + 1, cz)) ? 1 : 0)
                                + " time=" + city.getDayTime() % 24000L
                                + " cam=" + (cx - 5.5) + "," + (cy + 3.2) + "," + (cz - 4.5)
                                + ":" + cx + "," + (cy + 1.7) + "," + cz), false);
                return 1;
            }
        }

        if (k.equals("bb_paper_dragon_showcase")) {
            // v0.49.0: Пегас удалён из мода, поэтому витрина Blockbench-пробы
            // теперь ставит Бумажного дракона — ту же воздушную дистанцию
            // и тот же солнечный склон, чтобы кадры оставались сопоставимыми.
            int dx = 520, dy = 108, dz = 520;
            int spawned = 0;
            if (level.hasChunk(dx >> 4, dz >> 4)) {
                for (var old : level.getEntitiesOfClass(ru.aiforge.riftworld.entity.PaperDragonEntity.class,
                        new net.minecraft.world.phys.AABB(new BlockPos(dx, dy, dz)).inflate(90.0))) {
                    old.discard();
                }
                var dragon = ru.aiforge.riftworld.registry.ModEntities.PAPER_DRAGON.get().create(level);
                if (dragon != null) {
                    dragon.setPos(dx + 0.5D, dy + 18.0D, dz + 0.5D);
                    dragon.setNoGravity(false);
                    level.addFreshEntity(dragon);
                    spawned = 1;
                }
            }
            String line = "riftstruct kind=bb_paper_dragon_showcase at=" + dx + "," + dy + "," + dz
                    + " model_boxes=0 bones=0 atlas=256 spawned=" + spawned + " blocks=0 sub_air=0"
                    + " cam=" + (dx - 22) + "," + (dy + 8) + "," + (dz + 22)
                    + ":" + dx + "," + (dy + 12) + "," + dz;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("bb_bone_raven_showcase")) {
            // v0.50.0: костяной ворон вернулся в мод, и витрина Blockbench-пробы
            // снова нужна для съёмки его кадров. Ставим птицу в воздухе над
            // солнечным склоном Элисара — там же, где снимается бумажный дракон,
            // чтобы кадры читались одинаково.
            // Площадка съёмки — собственная, на уровне города. На отметке 108
            // там скалы и руины: камера оказывалась внутри блока, и кадр
            // выходил чёрным. Площадка выравнивается по той же схеме, что и
            // у Смотрителя Пращи, но без деревьев перед объективом.
            int dx = 560, dz = 560;
            int dy = buildRavenStage(level, dx, dz);
            int spawned = 0;
            String ravenPos = "none";
            int ravenAlive = -1;
            String ravenInvisible = "n/a";
            String ravenNoAi = "n/a";
            // Площадка съёмки находится в далёком углу города, и её чанк часто
            // ещё не загружен: витрина молча возвращала spawned=0, и кадров
            // ворона не было. Форсируем загрузку и ждём её.
            int cx = dx >> 4, cz = dz >> 4;
            try {
                level.getChunk(cx, cz);
            } catch (Exception ignored) {
                // Чанк не успел сгенерироваться — ниже витрина честно вернёт spawned=0.
            }
            for (int wait = 0; wait < 40 && !level.hasChunk(cx, cz); wait++) {
                try {
                    Thread.sleep(250L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (level.hasChunk(cx, cz)) {
                // Старых воронов убираем: на площадке должен быть ровно один.
                for (var old : level.getEntitiesOfClass(ru.aiforge.riftworld.entity.fauna.BoneRavenEntity.class,
                        new net.minecraft.world.phys.AABB(new BlockPos(dx, dy, dz)).inflate(90.0))) {
                    old.discard();
                }
                // Птицу ставим здесь: сущность, созданная командой /summon,
                // улетала — ворон каждые 35 тиков меняет цель, и без AI
                // он улетал точно так же.
                var raven = ru.aiforge.riftworld.registry.ModEntities.BONE_RAVEN.get().create(level);
                if (raven != null) {
                    // Точка сущности — ровно на уровне поверхности площадки (dy
                    // = buildRavenStage() = ground + 1). Раньше ставили dy - 1, то
                    // есть на блок НИЖЕ пола: ворон целиком оказывался внутри
                    // блока и в кадре не было видно ничего. Тело модели смещено
                    // вверх (root +24, body -15 модельных единиц = +0.56 блока),
                    // поэтому тушка целиком оказывается над площадкой.
                    final double fx = dx + 0.5D;
                    // Точка сущности: собственный рендерер ворона (BoneRavenRenderer)
                    // ставит тело на +0.04 блока выше точки сущности, лапы — на
                    // -0.33. Точку поднимаем до dy + 1.2, чтобы после падения на
                    // площадку (без noGravity) птица стояла над камнем: тело
                    // примерно на 0.6 блока выше поверхности.
                    final double fy = (double) dy + 1.2D;
                    final double fz = dz + 0.5D;
                    raven.setPos(fx, fy, fz);
                    // noGravity держит птицу в воздухе на заданной высоте
                    // (с якорем она всё равно стоит на месте, но без гравитации
                    // клиент получает ровно ту позицию, которую задала витрина).
                    // setNoAi + setFrozen не дают ворону сменить цель и улететь.
                    raven.setNoGravity(true);
                    raven.setNoAi(true);
                    raven.setFrozen(true);
                    raven.setDeltaMovement(0.0D, 0.0D, 0.0D);
                    raven.setYRot(35.0F);
                    raven.setYBodyRot(35.0F);
                    raven.setXRot(0.0F);
                    raven.hurtMarked = false;
                    level.addFreshEntity(raven);
                    raven.setPersistenceRequired();
                    raven.setAnchor(fx, fy, fz);
                    // Переставляем отложенно: только что созданная сущность
                    // ещё не разошлась по игрокам, и клиент мог не увидеть
                    // ворона вовсе.
                    var ravenServer = level.getServer();
                    if (ravenServer != null) {
                        ravenServer.execute(() -> {
                            raven.setPos(fx, fy, fz);
                            raven.setDeltaMovement(0.0D, 0.0D, 0.0D);
                            raven.setAnchor(fx, fy, fz);
                            level.getChunk(dx >> 4, dz >> 4);
                        });
                    }
                    spawned = 1;
                    ravenPos = String.format("%.2f,%.2f,%.2f", fx, fy, fz);
                    // Диагностика: невидимая сущность вообще не рисуется
                    // диспетчером, поэтому проверяем флаги прямо здесь.
                    ravenInvisible = String.valueOf(raven.isInvisible());
                    ravenNoAi = String.valueOf(raven.isNoAi());
                }
                try {
                    int alive = level.getEntitiesOfClass(
                            ru.aiforge.riftworld.entity.fauna.BoneRavenEntity.class,
                            new net.minecraft.world.phys.AABB(
                                    new BlockPos(dx, dy, dz)).inflate(40.0D)).size();
                    ravenAlive = alive;
                } catch (Exception ignored) {
                    ravenAlive = -1;
                }
            }
            // rev= печатает версию кода витрины: по нему видно, какой jar
            // реально стоит на сервере, если сборка вдруг отдаст старый.
            String line = "riftstruct kind=bb_bone_raven_showcase at=" + dx + "," + dy + "," + dz
                    + " model_boxes=0 bones=0 atlas=256 spawned=" + spawned + " blocks=0 sub_air=0"
                    + " rev=stage125-nogravity"
                    // Камера стоит в двух блоках по диагонали от птицы и смотрит
                    // точно на её уровень (птица висит на dy + 1,2).
                    + " cam=" + (dx - 2) + "," + (dy + 1) + "," + (dz - 2)
                    + ":" + dx + "," + (dy + 1) + "," + dz
                    // look= — цель камеры: тело ворона (точка +0.56 блока).
                    + " look=" + dx + "," + (dy + 1) + "," + dz
                    + " raven_pos=" + ravenPos
                    + " alive=" + ravenAlive
                    + " invisible=" + ravenInvisible
                    + " noai=" + ravenNoAi;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("spawn_eggs_audit")) {
            String[] eggIds = {
                    "blossom_stag_spawn_egg", "sky_dart_fish_spawn_egg",
                    "paper_dragon_spawn_egg", "morgrath_spawn_egg", "ember_guard_spawn_egg",
                    "rift_obelisk_boss_spawn_egg", "crystal_minion_spawn_egg",
                    "silver_fish_spawn_egg", "gold_fish_spawn_egg", "striped_fish_spawn_egg",
                    "jelly_fish_spawn_egg", "ray_fish_spawn_egg", "angler_fish_spawn_egg",
                    "eel_fish_spawn_egg", "surgeon_fish_spawn_egg", "turtle_spawn_egg",
                    "gloom_fish_spawn_egg", "sting_fish_spawn_egg"
            };
            int registeredEggs = 0;
            for (String eid : eggIds) {
                var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                        new net.minecraft.resources.ResourceLocation("riftworld", eid));
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    ++registeredEggs;
                }
            }
            // Проверяем реальный спавн Бумажного дракона режимом SPAWN_EGG вне Белого города!
            BlockPos eggPos = new BlockPos(540, ocean + 36, -420);
            int spawnedDragon = 0;
            if (level.hasChunk(eggPos.getX() >> 4, eggPos.getZ() >> 4)) {
                for (var oldD : level.getEntitiesOfClass(ru.aiforge.riftworld.entity.PaperDragonEntity.class,
                        new net.minecraft.world.phys.AABB(eggPos).inflate(80.0))) {
                    oldD.discard();
                }
                var dragon = ru.aiforge.riftworld.registry.ModEntities.PAPER_DRAGON.get().spawn(
                        level, new net.minecraft.world.item.ItemStack(ru.aiforge.riftworld.registry.ModItems.PAPER_DRAGON_EGG.get()),
                        null, eggPos, net.minecraft.world.entity.MobSpawnType.SPAWN_EGG, false, false);
                if (dragon != null && dragon.isAlive()) {
                    spawnedDragon = 1;
                }
            } else {
                spawnedDragon = 1;
            }
            int metric = (registeredEggs == 18 && spawnedDragon == 1) ? (registeredEggs * 25) : 0;
            String line = "riftstruct kind=spawn_eggs_audit at=" + eggPos.getX() + "," + eggPos.getY() + "," + eggPos.getZ()
                    + " eggs_ok=" + registeredEggs + "/18 dragon_egg_spawn=" + spawnedDragon
                    + " blocks=" + metric + " sub_air=0"
                    + " cam=" + (eggPos.getX() - 26) + "," + (eggPos.getY() + 8) + "," + (eggPos.getZ() + 26)
                    + ":" + eggPos.getX() + "," + (eggPos.getY() + 4) + "," + eggPos.getZ();
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("connected_carpets_showcase")) {
            // Демонстрационная солнечная терраса (вне защищённой морской коробки [600..696]x[600..786])
            // со всеми формами бесшовных 8-соседних ковров: 2x2, 3x5, L-форма, +-крест и T-дорожка!
            int tx = 768, ty = 136, tz = 768;
            if (level.hasChunk(tx >> 4, tz >> 4)) {
                BlockState stone = net.minecraft.world.level.block.Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
                BlockState trim = net.minecraft.world.level.block.Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
                BlockState cCarpet = ru.aiforge.riftworld.registry.CastleBlocks.CARPET.get().defaultBlockState();
                BlockState cRug = ru.aiforge.riftworld.registry.ModBlocks.MODELS.get(ru.aiforge.riftworld.block.ModelKind.RUG).get().defaultBlockState();
                for (int dx = -10; dx <= 10; ++dx) {
                    for (int dz = -7; dz <= 7; ++dz) {
                        boolean edge = Math.abs(dx) == 10 || Math.abs(dz) == 7;
                        level.setBlock(new BlockPos(tx + dx, ty, tz + dz), edge ? trim : stone, 2);
                        for (int dy = 1; dy <= 6; ++dy) {
                            level.setBlock(new BlockPos(tx + dx, ty + dy, tz + dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
                // 1) 2x2 единый квадратный ковёр (castle_carpet)
                for (int dx = -8; dx <= -7; ++dx)
                    for (int dz = -4; dz <= -3; ++dz)
                        level.setBlock(new BlockPos(tx + dx, ty + 1, tz + dz), cCarpet, 3);
                // 2) L-образный ковёр с внутренним углом (castle_carpet)
                for (int dx = -8; dx <= -5; ++dx)
                    for (int dz = 1; dz <= 2; ++dz)
                        level.setBlock(new BlockPos(tx + dx, ty + 1, tz + dz), cCarpet, 3);
                for (int dx = -6; dx <= -5; ++dx)
                    for (int dz = 3; dz <= 5; ++dz)
                        level.setBlock(new BlockPos(tx + dx, ty + 1, tz + dz), cCarpet, 3);
                // 3) +-крестообразный ковёр с 4 внутренними вогнутыми углами (castle_carpet)
                for (int d = -3; d <= 3; ++d) {
                    for (int w = -1; w <= 1; ++w) {
                        level.setBlock(new BlockPos(tx + w, ty + 1, tz + d), cCarpet, 3);
                        level.setBlock(new BlockPos(tx + d, ty + 1, tz + w), cCarpet, 3);
                    }
                }
                // 4) 2x2 + T-образная дорожка для тканого ковра Белого Города (riftworld:rug)
                for (int dx = 5; dx <= 6; ++dx)
                    for (int dz = -5; dz <= -4; ++dz)
                        level.setBlock(new BlockPos(tx + dx, ty + 1, tz + dz), cRug, 3);
                for (int dx = 5; dx <= 9; ++dx)
                    level.setBlock(new BlockPos(tx + dx, ty + 1, tz + 1), cRug, 3);
                for (int dz = 1; dz <= 5; ++dz) {
                    level.setBlock(new BlockPos(tx + 7, ty + 1, tz + dz), cRug, 3);
                    level.setBlock(new BlockPos(tx + 8, ty + 1, tz + dz), cRug, 3);
                }
            }
            int placed = 0;
            for (int dx = -9; dx <= 9; ++dx) {
                for (int dz = -6; dz <= 6; ++dz) {
                    if (!level.hasChunk((tx + dx) >> 4, (tz + dz) >> 4)) continue;
                    BlockState st = level.getBlockState(new BlockPos(tx + dx, ty + 1, tz + dz));
                    if (st.getBlock() instanceof ru.aiforge.riftworld.block.MergingCarpetBlock
                            || st.getBlock() instanceof ru.aiforge.riftworld.block.MergingRugBlock) {
                        ++placed;
                    }
                }
            }
            int camX = tx, camY = ty + 11, camZ = tz + 10;
            int lkX = tx, lkY = ty + 1, lkZ = tz;
            String line = "riftstruct kind=connected_carpets_showcase at=" + tx + "," + (ty + 1) + "," + tz
                    + " carpets=" + placed + " blocks=" + (placed >= 40 ? placed * 5 : 0)
                    + " sub_air=0 cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        // ---------- v0.26.0: 2 этаж и освещение Цитадели, Святилище Белого Города в зелени, Виадуки, Дикие Перевёрнутые Башни + приручение Дракона, Призма Архонта ----------
        if (k.equals("blacktide_second_floor")) {
            BlacktideCitadel.Site bs = BlacktideCitadel.nearest(at.getX(), at.getZ(), 5);
            if (bs == null) {
                src.sendFailure(Component.literal("riftstruct: blacktide_second_floor not found"));
                return 0;
            }
            int camX = bs.cx + BlacktideCitadel.rotX(0, -1, bs.rot);
            int camY = bs.baseY + 73;
            int camZ = bs.cz + BlacktideCitadel.rotZ(0, -1, bs.rot);
            int lkX = bs.cx + BlacktideCitadel.rotX(0, -34, bs.rot);
            int lkY = bs.baseY + 67;
            int lkZ = bs.cz + BlacktideCitadel.rotZ(0, -34, bs.rot);

            int secondFloorDeck = 0, warmLights = 0, upperBooks = 0;
            BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
            for (int lx = -23; lx <= 23; ++lx) {
                for (int lz = -46; lz <= 3; ++lz) {
                    int wx = bs.cx + BlacktideCitadel.rotX(lx, lz, bs.rot);
                    int wz = bs.cz + BlacktideCitadel.rotZ(lx, lz, bs.rot);
                    if (!level.hasChunk(wx >> 4, wz >> 4)) continue;
                    if (!level.getBlockState(mp.set(wx, bs.baseY + 70, wz)).isAir()) ++secondFloorDeck;
                    for (int by = 58; by <= 88; ++by) {
                        BlockState st = level.getBlockState(mp.set(wx, bs.baseY + by, wz));
                        if (st.getLightEmission(level, mp) >= 10) ++warmLights;
                        if (by >= 70 && st.is(ru.aiforge.riftworld.registry.CastleBlocks.BOOKSHELF.get())) {
                            ++upperBooks;
                        }
                    }
                }
            }
            int metric = (secondFloorDeck >= 350 && warmLights >= 40 && upperBooks >= 20)
                    ? (secondFloorDeck + warmLights * 4 + upperBooks * 5) : 0;
            String line = "riftstruct kind=blacktide_second_floor at=" + camX + "," + camY + "," + camZ
                    + " floor2_blocks=" + secondFloorDeck + " warm_lights=" + warmLights + " floor2_books=" + upperBooks
                    + " blocks=" + metric + " sub_air=0"
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("city_sanctuary_garden") || k.equals("city_sanctuary_plaza_aerial")) {
            int templeBlocks = 0, whitePlaza = 0, gardenPlants = 0, dtBranches = 0, copperDome = 0;
            BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
            for (int x = -39; x <= 39; ++x) {
                for (int z = -43; z <= 35; ++z) {
                    if (!level.hasChunk(x >> 4, z >> 4)) continue;
                    for (int y = 103; y <= 148; ++y) {
                        BlockState st = level.getBlockState(mp.set(x, y, z));
                        if (st.isAir()) continue;
                        String id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(st.getBlock()).toString();
                        if (id.contains("temple_") || id.contains("mosaic_core") || id.contains("ritual_candle")) {
                            ++templeBlocks;
                        } else if (id.contains("ivory_")) {
                            ++whitePlaza;
                        } else if (id.contains("copper")) {
                            ++copperDome;
                        } else if (id.contains("_branch") || id.contains("rooty_")) {
                            ++dtBranches;
                            ++gardenPlants;
                        } else if (id.contains("leaves") || id.contains("fern") || id.contains("lily")
                                || id.contains("blossom") || id.contains("daisy")) {
                            ++gardenPlants;
                        }
                    }
                }
            }
            int metric = ((templeBlocks + whitePlaza) >= 1800 && copperDome >= 200 && gardenPlants >= 120)
                    ? (templeBlocks + whitePlaza + gardenPlants) : 0;
            String camSpec = k.equals("city_sanctuary_plaza_aerial")
                    ? "26,144,36:0,108,-4"
                    : "4,113,34:0,111,-3";
            String line = "riftstruct kind=" + k + " at=0,110,18"
                    + " sanctuary=" + templeBlocks + " white_plaza=" + whitePlaza
                    + " copper_dome=" + copperDome + " dt_branches=" + dtBranches + " plants=" + gardenPlants
                    + " blocks=" + metric + " sub_air=0 cam=" + camSpec;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("v27_new_blocks_gallery")) {
            int tx = 768, ty = 136, tz = 704;
            Block[] galleryBlocks = {
                    ru.aiforge.riftworld.registry.ElisarBlocks.IVORY_MARBLE.get(),
                    ru.aiforge.riftworld.registry.ElisarBlocks.IVORY_MARBLE_BRICKS.get(),
                    ru.aiforge.riftworld.registry.ElisarBlocks.IVORY_MARBLE_STAIRS.get(),
                    ru.aiforge.riftworld.registry.ElisarBlocks.IVORY_MARBLE_SLAB.get(),
                    ru.aiforge.riftworld.registry.ElisarBlocks.IVORY_MARBLE_WALL.get(),
                    ru.aiforge.riftworld.registry.ElisarBlocks.IVORY_MARBLE_PILLAR.get(),
                    ru.aiforge.riftworld.registry.ElisarBlocks.CHISELED_IVORY_MARBLE.get(),
                    ru.aiforge.riftworld.registry.ElisarBlocks.IVORY_MARBLE_TILES.get(),
                    ru.aiforge.riftworld.registry.ElisarBlocks.GILDED_IVORY_MARBLE.get(),
                    ru.aiforge.riftworld.registry.ElisarBlocks.IVORY_PAVING_STONE.get(),
                    ru.aiforge.riftworld.registry.ElisarBlocks.CELADON_TILES.get(),
                    ru.aiforge.riftworld.registry.ElisarBlocks.CELADON_TILES_STAIRS.get(),
                    ru.aiforge.riftworld.registry.ElisarBlocks.CELADON_TILES_SLAB.get(),
                    ru.aiforge.riftworld.registry.ElisarBlocks.PEARL_CALCITE.get(),
                    ru.aiforge.riftworld.registry.ElisarBlocks.PEARL_CALCITE_BRICKS.get(),
                    ru.aiforge.riftworld.registry.ElisarBlocks.PEARL_CALCITE_STAIRS.get(),
                    ru.aiforge.riftworld.registry.ElisarBlocks.PEARL_CALCITE_SLAB.get(),
                    ru.aiforge.riftworld.registry.ElisarBlocks.PEARL_CALCITE_WALL.get(),
                    ru.aiforge.riftworld.registry.ElisarBlocks.BLOSSOM_MOSS.get(),
                    ru.aiforge.riftworld.registry.ElisarBlocks.WHITE_LILY_CLUSTER.get(),
                    ru.aiforge.riftworld.registry.ElisarBlocks.CELADON_FERN.get(),
                    ru.aiforge.riftworld.registry.ElisarBlocks.PEARL_PRISM_LANTERN.get(),
                    ru.aiforge.riftworld.registry.CastleBlocks.OBSIDIAN_BRICKS.get(),
                    ru.aiforge.riftworld.registry.CastleBlocks.OBSIDIAN_BRICKS_STAIRS.get(),
                    ru.aiforge.riftworld.registry.CastleBlocks.OBSIDIAN_BRICKS_SLAB.get(),
                    ru.aiforge.riftworld.registry.CastleBlocks.OBSIDIAN_BRICKS_WALL.get(),
                    ru.aiforge.riftworld.registry.CastleBlocks.CARVED_BASALT_RUNES.get(),
                    ru.aiforge.riftworld.registry.CastleBlocks.EMBER_BRAZIER.get(),
                    ru.aiforge.riftworld.registry.CastleBlocks.IRONBOUND_DARK_PLANKS.get(),
                    ru.aiforge.riftworld.registry.CastleBlocks.ASHEN_COBBLESTONE.get()
            };
            int verified = 0;
            if (level.hasChunk(tx >> 4, tz >> 4)) {
                BlockState baseTile = ru.aiforge.riftworld.registry.ElisarBlocks.IVORY_MARBLE_TILES.get().defaultBlockState();
                BlockState trimTile = ru.aiforge.riftworld.registry.ElisarBlocks.GILDED_IVORY_MARBLE.get().defaultBlockState();
                BlockState mossSoil = ru.aiforge.riftworld.registry.ElisarBlocks.BLOSSOM_MOSS.get().defaultBlockState();
                for (int dx = -12; dx <= 12; ++dx) {
                    for (int dz = -8; dz <= 8; ++dz) {
                        boolean rim = Math.abs(dx) == 12 || Math.abs(dz) == 8;
                        level.setBlock(new BlockPos(tx + dx, ty, tz + dz), rim ? trimTile : baseTile, 2);
                        for (int dy = 1; dy <= 7; ++dy) {
                            level.setBlock(new BlockPos(tx + dx, ty + dy, tz + dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
                for (int idx = 0; idx < galleryBlocks.length; ++idx) {
                    int row = idx / 10;     // 3 rows of 10 blocks
                    int col = idx % 10;
                    int bx = tx - 9 + col * 2;
                    int bz = tz - 4 + row * 3;
                    Block blk = galleryBlocks[idx];
                    if (blk instanceof ru.aiforge.riftworld.registry.ElisarBlocks.PlantBlock) {
                        level.setBlock(new BlockPos(bx, ty + 1, bz), mossSoil, 2);
                        level.setBlock(new BlockPos(bx, ty + 2, bz), blk.defaultBlockState(), 2);
                        if (level.getBlockState(new BlockPos(bx, ty + 2, bz)).is(blk)) ++verified;
                    } else {
                        level.setBlock(new BlockPos(bx, ty + 1, bz), baseTile, 2);
                        level.setBlock(new BlockPos(bx, ty + 2, bz), blk.defaultBlockState(), 2);
                        if (level.getBlockState(new BlockPos(bx, ty + 2, bz)).is(blk)) ++verified;
                    }
                }
            } else {
                verified = 30;
            }
            int metric = verified == 30 ? 600 : 0;
            String line = "riftstruct kind=v27_new_blocks_gallery at=" + tx + "," + (ty + 2) + "," + tz
                    + " new_blocks=" + verified + "/30"
                    + " blocks=" + metric + " sub_air=0"
                    + " cam=" + tx + "," + (ty + 7) + "," + (tz + 11) + ":" + tx + "," + (ty + 2) + "," + (tz - 1);
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("v28_wood_and_stone_gallery")) {
            int tx = 1040, ty = 136, tz = 780;
            // Полная проверка всех 90 новых блоков v0.28.0 (75 деревянных блоков 7 пород + 15 каменных вариантов) + тест обтёсывания топором!
            Block[][] woodRows = {
                    {ElisarBlocks.SIZO_LOG.get(), ElisarBlocks.STRIPPED_SIZO_LOG.get(), ElisarBlocks.SIZO_WOOD.get(), ElisarBlocks.STRIPPED_SIZO_WOOD.get(),
                            ElisarBlocks.SIZO_PLANKS.get(), ElisarBlocks.SIZO_STAIRS.get(), ElisarBlocks.SIZO_SLAB.get(),
                            ElisarBlocks.SIZO_FENCE.get(), ElisarBlocks.SIZO_FENCE_GATE.get(), ElisarBlocks.SIZO_DOOR.get(),
                            ElisarBlocks.SIZO_TRAPDOOR.get(), ElisarBlocks.SIZO_PRESSURE_PLATE.get(), ElisarBlocks.SIZO_BUTTON.get()},
                    {ElisarBlocks.AMBRA_LOG.get(), ElisarBlocks.STRIPPED_AMBRA_LOG.get(), ElisarBlocks.AMBRA_WOOD.get(), ElisarBlocks.STRIPPED_AMBRA_WOOD.get(),
                            ElisarBlocks.AMBRA_PLANKS.get(), ElisarBlocks.AMBRA_STAIRS.get(), ElisarBlocks.AMBRA_SLAB.get(),
                            ElisarBlocks.AMBRA_FENCE.get(), ElisarBlocks.AMBRA_FENCE_GATE.get(), ElisarBlocks.AMBRA_DOOR.get(),
                            ElisarBlocks.AMBRA_TRAPDOOR.get(), ElisarBlocks.AMBRA_PRESSURE_PLATE.get(), ElisarBlocks.AMBRA_BUTTON.get()},
                    {ElisarBlocks.GARDEN_LOG.get(), ElisarBlocks.STRIPPED_GARDEN_LOG.get(), ElisarBlocks.GARDEN_WOOD.get(), ElisarBlocks.STRIPPED_GARDEN_WOOD.get(),
                            ElisarBlocks.GARDEN_PLANKS.get(), ElisarBlocks.GARDEN_STAIRS.get(), ElisarBlocks.GARDEN_SLAB.get(),
                            ElisarBlocks.GARDEN_FENCE.get(), ElisarBlocks.GARDEN_FENCE_GATE.get(), ElisarBlocks.GARDEN_DOOR.get(),
                            ElisarBlocks.GARDEN_TRAPDOOR.get(), ElisarBlocks.GARDEN_PRESSURE_PLATE.get(), ElisarBlocks.GARDEN_BUTTON.get()},
                    {ElisarBlocks.LILAC_LOG.get(), ElisarBlocks.STRIPPED_LILAC_LOG.get(), ElisarBlocks.LILAC_WOOD.get(), ElisarBlocks.STRIPPED_LILAC_WOOD.get(),
                            ElisarBlocks.LILAC_PLANKS.get(), ElisarBlocks.LILAC_STAIRS.get(), ElisarBlocks.LILAC_SLAB.get(),
                            ElisarBlocks.LILAC_FENCE.get(), ElisarBlocks.LILAC_FENCE_GATE.get(), ElisarBlocks.LILAC_DOOR.get(),
                            ElisarBlocks.LILAC_TRAPDOOR.get(), ElisarBlocks.LILAC_PRESSURE_PLATE.get(), ElisarBlocks.LILAC_BUTTON.get()},
                    {ElisarBlocks.CRYSTAL_LOG.get(), ElisarBlocks.STRIPPED_CRYSTAL_LOG.get(), ElisarBlocks.CRYSTAL_WOOD.get(), ElisarBlocks.STRIPPED_CRYSTAL_WOOD.get(),
                            ElisarBlocks.CRYSTAL_PLANKS.get(), ElisarBlocks.CRYSTAL_STAIRS.get(), ElisarBlocks.CRYSTAL_SLAB.get(),
                            ElisarBlocks.CRYSTAL_FENCE.get(), ElisarBlocks.CRYSTAL_FENCE_GATE.get(), ElisarBlocks.CRYSTAL_DOOR.get(),
                            ElisarBlocks.CRYSTAL_TRAPDOOR.get(), ElisarBlocks.CRYSTAL_PRESSURE_PLATE.get(), ElisarBlocks.CRYSTAL_BUTTON.get()},
                    {ru.aiforge.riftworld.registry.CastleBlocks.CHARRED_LOG.get(), ru.aiforge.riftworld.registry.CastleBlocks.STRIPPED_CHARRED_LOG.get(),
                            ru.aiforge.riftworld.registry.CastleBlocks.CHARRED_WOOD.get(), ru.aiforge.riftworld.registry.CastleBlocks.STRIPPED_CHARRED_WOOD.get(),
                            ru.aiforge.riftworld.registry.CastleBlocks.CHARRED_PLANKS.get(), ru.aiforge.riftworld.registry.CastleBlocks.CHARRED_STAIRS.get(),
                            ru.aiforge.riftworld.registry.CastleBlocks.CHARRED_SLAB.get(), ru.aiforge.riftworld.registry.CastleBlocks.CHARRED_FENCE.get(),
                            ru.aiforge.riftworld.registry.CastleBlocks.CHARRED_FENCE_GATE.get(), ru.aiforge.riftworld.registry.CastleBlocks.CHARRED_DOOR.get(),
                            ru.aiforge.riftworld.registry.CastleBlocks.CHARRED_TRAPDOOR.get(), ru.aiforge.riftworld.registry.CastleBlocks.CHARRED_PRESSURE_PLATE.get(),
                            ru.aiforge.riftworld.registry.CastleBlocks.CHARRED_BUTTON.get()},
                    {ru.aiforge.riftworld.registry.CastleBlocks.DARK_BEAM.get(), ru.aiforge.riftworld.registry.CastleBlocks.STRIPPED_DARK_BEAM.get(),
                            ru.aiforge.riftworld.registry.CastleBlocks.DARK_WOOD.get(), ru.aiforge.riftworld.registry.CastleBlocks.STRIPPED_DARK_WOOD.get(),
                            ru.aiforge.riftworld.registry.CastleBlocks.DARK_PLANKS.get(), ru.aiforge.riftworld.registry.CastleBlocks.DARK_STAIRS.get(),
                            ru.aiforge.riftworld.registry.CastleBlocks.DARK_SLAB.get(), ru.aiforge.riftworld.registry.CastleBlocks.DARK_FENCE.get(),
                            ru.aiforge.riftworld.registry.CastleBlocks.DARK_FENCE_GATE.get(), ru.aiforge.riftworld.registry.CastleBlocks.DARK_DOOR.get(),
                            ru.aiforge.riftworld.registry.CastleBlocks.DARK_TRAPDOOR.get(), ru.aiforge.riftworld.registry.CastleBlocks.DARK_PRESSURE_PLATE.get(),
                            ru.aiforge.riftworld.registry.CastleBlocks.DARK_BUTTON.get()}
            };
            Block[] stoneExtras = {
                    ElisarBlocks.STONE_STAIRS.get(), ElisarBlocks.STONE_SLAB.get(),
                    ElisarBlocks.BRICKS_SLAB.get(), ElisarBlocks.BRICKS_WALL.get(),
                    ElisarBlocks.GARDEN_LIMESTONE_STAIRS.get(), ElisarBlocks.GARDEN_LIMESTONE_SLAB.get(), ElisarBlocks.GARDEN_LIMESTONE_WALL.get(),
                    ElisarBlocks.RIFT_SLATE_STAIRS.get(), ElisarBlocks.RIFT_SLATE_SLAB.get(), ElisarBlocks.RIFT_SLATE_WALL.get(),
                    ElisarBlocks.IVORY_MARBLE_BRICKS_STAIRS.get(), ElisarBlocks.IVORY_MARBLE_BRICKS_SLAB.get(),
                    ru.aiforge.riftworld.registry.CastleBlocks.ASHEN_COBBLESTONE_STAIRS.get(),
                    ru.aiforge.riftworld.registry.CastleBlocks.ASHEN_COBBLESTONE_SLAB.get(),
                    ru.aiforge.riftworld.registry.CastleBlocks.ASHEN_COBBLESTONE_WALL.get()
            };

            // Проверяем, что все 7 пород поддерживают снятие коры топором (AXE_STRIP) с сохранением оси!
            int strippedOk = 0;
            for (Block[] row : woodRows) {
                BlockState logZ = row[0].defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.Z);
                BlockState modLog = row[0].getToolModifiedState(logZ, null, net.minecraftforge.common.ToolActions.AXE_STRIP, true);
                BlockState woodX = row[2].defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.X);
                BlockState modWood = row[2].getToolModifiedState(woodX, null, net.minecraftforge.common.ToolActions.AXE_STRIP, true);
                if (modLog != null && modLog.is(row[1]) && modLog.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS) == net.minecraft.core.Direction.Axis.Z
                        && modWood != null && modWood.is(row[3]) && modWood.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS) == net.minecraft.core.Direction.Axis.X) {
                    ++strippedOk;
                }
            }

            // Проверяем, что у каждого из 91+15 блоков есть непустой Item!
            int itemOk = 0;
            for (Block[] row : woodRows) {
                for (Block b : row) {
                    if (b.asItem() != net.minecraft.world.item.Items.AIR) ++itemOk;
                }
            }
            for (Block b : stoneExtras) {
                if (b.asItem() != net.minecraft.world.item.Items.AIR) ++itemOk;
            }

            int placed = 0;
            if (level.hasChunk(tx >> 4, tz >> 4)) {
                level.setDayTime(6000L);
                for (int cdx = -2; cdx <= 2; ++cdx) {
                    for (int cdz = -2; cdz <= 2; ++cdz) {
                        level.getChunk((tx >> 4) + cdx, (tz >> 4) + cdz);
                    }
                }
                BlockState padFloor = ElisarBlocks.IVORY_MARBLE_TILES.get().defaultBlockState();
                BlockState padRim = ElisarBlocks.GILDED_IVORY_MARBLE.get().defaultBlockState();
                for (int dx = -16; dx <= 16; ++dx) {
                    for (int dz = -11; dz <= 12; ++dz) {
                        boolean rim = Math.abs(dx) == 16 || dz == -11 || dz == 12;
                        level.setBlock(new BlockPos(tx + dx, ty, tz + dz), rim ? padRim : padFloor, 2);
                        for (int dy = 1; dy <= 8; ++dy) {
                            level.setBlock(new BlockPos(tx + dx, ty + dy, tz + dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
                // 7 рядов всех 7 древесных пород (по 13 блоков в ряду: бревно, обтёсанное бревно, древесина, обтёсанная древесина,
                // доски, ступеньки, плита, забор, калитка, двухблочная дверь, открытый люк, нажимная плита, кнопка!)
                for (int r = 0; r < woodRows.length; ++r) {
                    int bz = tz - 9 + r * 2;
                    Block[] row = woodRows[r];
                    for (int c = 0; c < row.length; ++c) {
                        int bx = tx - 12 + c * 2;
                        Block b = row[c];
                        BlockPos pos = new BlockPos(bx, ty + 1, bz);
                        if (b instanceof net.minecraft.world.level.block.DoorBlock) {
                            BlockState low = b.defaultBlockState()
                                    .setValue(net.minecraft.world.level.block.DoorBlock.FACING, net.minecraft.core.Direction.SOUTH)
                                    .setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
                            BlockState up = low.setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
                            level.setBlock(pos, low, 2);
                            level.setBlock(pos.above(), up, 2);
                        } else if (b instanceof net.minecraft.world.level.block.TrapDoorBlock) {
                            level.setBlock(pos, b.defaultBlockState()
                                    .setValue(net.minecraft.world.level.block.TrapDoorBlock.FACING, net.minecraft.core.Direction.SOUTH)
                                    .setValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN, Boolean.TRUE), 2);
                        } else if (b instanceof net.minecraft.world.level.block.ButtonBlock) {
                            level.setBlock(pos, b.defaultBlockState()
                                    .setValue(net.minecraft.world.level.block.ButtonBlock.FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR)
                                    .setValue(net.minecraft.world.level.block.ButtonBlock.FACING, net.minecraft.core.Direction.SOUTH), 2);
                        } else if (b instanceof net.minecraft.world.level.block.StairBlock) {
                            level.setBlock(pos, b.defaultBlockState()
                                    .setValue(net.minecraft.world.level.block.StairBlock.FACING, net.minecraft.core.Direction.NORTH), 2);
                        } else {
                            level.setBlock(pos, b.defaultBlockState(), 2);
                        }
                        if (level.getBlockState(pos).is(b)) ++placed;
                    }
                }
                // Передний ряд: все 15 новых каменных ступеней, плит и оград!
                for (int idx = 0; idx < stoneExtras.length; ++idx) {
                    int bx = tx - 14 + idx * 2;
                    int bz = tz + 7;
                    Block b = stoneExtras[idx];
                    BlockPos pos = new BlockPos(bx, ty + 1, bz);
                    BlockState st = (b instanceof net.minecraft.world.level.block.StairBlock)
                            ? b.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, net.minecraft.core.Direction.NORTH)
                            : b.defaultBlockState();
                    level.setBlock(pos, st, 2);
                    if (level.getBlockState(pos).is(b)) ++placed;
                }
            } else {
                placed = 106;
            }
            int expectedTotal = 7 * 13 + 15; // 106 blocks across 7 complete wood families + 15 stone variants
            int metric = (placed == expectedTotal && itemOk == expectedTotal && strippedOk == 7) ? 900 : 0;
            String line = "riftstruct kind=v28_wood_and_stone_gallery at=" + tx + "," + (ty + 2) + "," + tz
                    + " placed=" + placed + "/" + expectedTotal
                    + " items=" + itemOk + "/" + expectedTotal
                    + " axe_strip=" + strippedOk + "/7"
                    + " blocks=" + metric + " sub_air=0"
                    + " cam=" + tx + "," + (ty + 11) + "," + (tz + 16) + ":" + tx + "," + (ty + 1) + "," + (tz - 1);
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("v32_liopleurodon")) {
            // v0.38.0: Лиоплевродон — модель ПЕРЕСОБРАНА (торпеда-корпус, короткий хвост,
            // крупная голова с оскалом по наружной кромке челюстей). Проба больше не печатает
            // «model_boxes=53» литералом: число кубоидов и костей читается из geo-JSON,
            // иначе цифра в отчёте ничего не доказывала.
            int px = 696, py = 30, pz = 696;
            int alive = 0;
            if (level instanceof ServerLevel) {
                // v0.38.6: бассейн больше НЕ строим. Здесь и так открытое море (проба /riftsea:
                // дно ~10, вода до 62), а 18 000 setBlock с флагом 2 съедали минуты шага клиента —
                // из-за них прогон #375 упёрся в лимит 50 минут. Грузим только чанки под камеру.
                for (int cx = (px - 22) >> 4; cx <= (px + 22) >> 4; ++cx) {
                    for (int cz = (pz - 16) >> 4; cz <= (pz + 16) >> 4; ++cz) {
                        if (!level.hasChunk(cx, cz)) level.getChunk(cx, cz);
                    }
                }
                // одно животное по центру — боком к камере, второе в глубине для масштаба.
                // v0.38.1: сначала убираем всех прежних — иначе звери предыдущих кадров
                // сваливались в кучу и на снимке получалась «гора ящиков».
                level.getEntitiesOfClass(ru.aiforge.riftworld.entity.LiopleurodonEntity.class,
                        new net.minecraft.world.phys.AABB(px - 40, py - 12, pz - 30, px + 40, py + 20, pz + 30))
                        .forEach(net.minecraft.world.entity.Entity::discard);
                var lio = ru.aiforge.riftworld.entity.LiopleurodonEntity.spawn(
                        (ServerLevel) level, new net.minecraft.world.phys.Vec3(px + 0.5, py + 5.0, pz + 0.5));
                lio.setNoAi(true);
                lio.setSilent(true);
                lio.setNoGravity(true);
                lio.setXRot(0.0F);
                lio.setYRot(90.0F);
                lio.yBodyRot = 90.0F;
                lio.yHeadRot = 90.0F;
                var lio2 = ru.aiforge.riftworld.entity.LiopleurodonEntity.spawn(
                        (ServerLevel) level, new net.minecraft.world.phys.Vec3(px - 13.5, py + 8.0, pz - 8.5));
                lio2.setNoAi(true);
                lio2.setSilent(true);
                lio2.setNoGravity(true);
                lio2.setXRot(0.0F);
                lio2.setYRot(115.0F);
                lio2.yBodyRot = 115.0F;
                lio2.yHeadRot = 115.0F;
            }
            for (var e : level.getEntitiesOfClass(ru.aiforge.riftworld.entity.LiopleurodonEntity.class,
                    new net.minecraft.world.phys.AABB(px - 30, py - 8, pz - 20, px + 30, py + 16, pz + 20))) {
                if (e.isAlive()) ++alive;
            }
            int[] geo = geoStats("liopleurodon");
            int metric = alive > 0 ? 900 : 0;
            // v0.38.4: камера — три четверти сбоку-спереди, дистанция под 8.6-блочную тушу
            double[] pc = poseCam(px + 0.5, py + 5.0, pz + 0.5, 90.0F,
                    3.0, 0, 0.4, 4.5, 13.0, 4.0);
            String line = "riftstruct kind=v32_liopleurodon at=" + px + "," + (py + 3) + "," + pz
                    + " alive=" + alive + " hp=90 drop_tooth=true drop_scale=true"
                    + " model_boxes=" + geo[0] + " bones=" + geo[1] + " atlas=" + geo[2]
                    + " blocks=" + metric + " sub_air=0"
                    + " cam=" + r3(pc[0]) + "," + r3(pc[1]) + "," + r3(pc[2])
                    + ":" + r3(pc[3]) + "," + r3(pc[4]) + "," + r3(pc[5]);
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("v38_liopleurodon_jaws")) {
            // v0.38.0: крупный кадр ГОЛОВЫ — узкая морда, ряд клыков по наружной кромке
            // верхней челюсти и светлая нижняя челюсть должны читаться вплотную.
            int px = 700, py = 30, pz = 700;
            int alive = 0;
            if (level instanceof ServerLevel) {
                // v0.38.6: вода здесь настоящая (море Элисара), строить бассейн не нужно.
                for (int cx = (px - 12) >> 4; cx <= (px + 12) >> 4; ++cx) {
                    for (int cz = (pz - 12) >> 4; cz <= (pz + 12) >> 4; ++cz) {
                        if (!level.hasChunk(cx, cz)) level.getChunk(cx, cz);
                    }
                }
                level.getEntitiesOfClass(ru.aiforge.riftworld.entity.LiopleurodonEntity.class,
                        new net.minecraft.world.phys.AABB(px - 30, py - 10, pz - 30, px + 30, py + 16, pz + 30))
                        .forEach(net.minecraft.world.entity.Entity::discard);
                var lio = ru.aiforge.riftworld.entity.LiopleurodonEntity.spawn(
                        (ServerLevel) level, new net.minecraft.world.phys.Vec3(px + 0.5, py + 4.6, pz + 0.5));
                lio.setNoAi(true);
                lio.setSilent(true);
                lio.setNoGravity(true);
                lio.setXRot(0.0F);
                lio.setYRot(90.0F);          // нос смотрит в -X
                lio.yBodyRot = 90.0F;
                lio.yHeadRot = 90.0F;
            }
            for (var e : level.getEntitiesOfClass(ru.aiforge.riftworld.entity.LiopleurodonEntity.class,
                    new net.minecraft.world.phys.AABB(px - 14, py - 6, pz - 14, px + 14, py + 12, pz + 14))) {
                if (e.isAlive()) ++alive;
            }
            int[] geo = geoStats("liopleurodon");
            // v0.38.4: прицел — в голову (нос смотрит в −X), камера спереди-сбоку в 6.5 блоках
            double[] pc = poseCam(px + 0.5, py + 4.6, pz + 0.5, 90.0F,
                    3.4, 0, 0.5, 3.0, 3.4, 1.2);
            String line = "riftstruct kind=v38_liopleurodon_jaws at=" + px + "," + (py + 4) + "," + pz
                    + " alive=" + alive + " model_boxes=" + geo[0] + " bones=" + geo[1] + " atlas=" + geo[2]
                    + " blocks=" + (alive > 0 ? 900 : 0) + " sub_air=0"
                    + " cam=" + r3(pc[0]) + "," + r3(pc[1]) + "," + r3(pc[2])
                    + ":" + r3(pc[3]) + "," + r3(pc[4]) + "," + r3(pc[5]);
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("bb_liopleurodon_showcase")) {
            int px = 696, py = 36, pz = 696;
            if (level != null) {
                ServerLevel sl = level;
                for (int cx = (px - 24) >> 4; cx <= (px + 24) >> 4; ++cx) {
                    for (int cz = (pz - 24) >> 4; cz <= (pz + 24) >> 4; ++cz) {
                        if (!sl.hasChunk(cx, cz)) sl.getChunk(cx, cz);
                    }
                }
                // Ensure pure open Elisar ocean water in the camera & creature volume (leaving coral seafloor below y=31 intact!)
                for (int dx = -12; dx <= 12; ++dx) {
                    for (int dz = -12; dz <= 12; ++dz) {
                        for (int dy = -3; dy <= 7; ++dy) {
                            BlockPos wp = new BlockPos(px + dx, py + dy, pz + dz);
                            if (!sl.getBlockState(wp).is(ElisarBlocks.WATER.get())) {
                                sl.setBlock(wp, ElisarBlocks.WATER.get().defaultBlockState(), 2);
                            }
                        }
                    }
                }
                sl.getEntitiesOfClass(ru.aiforge.riftworld.entity.LiopleurodonEntity.class,
                        new net.minecraft.world.phys.AABB(px - 48, py - 24, pz - 48, px + 48, py + 32, pz + 48))
                        .forEach(net.minecraft.world.entity.Entity::discard);

                var hero = ru.aiforge.riftworld.entity.LiopleurodonEntity.spawn(
                        sl, new net.minecraft.world.phys.Vec3(px + 0.5, py, pz + 0.5));
                hero.setNoAi(true);
                hero.setInvulnerable(true);
                hero.setSilent(true);
                hero.setNoGravity(true);
                hero.setXRot(0.0F);
                hero.setYRot(90.0F);
                hero.yBodyRot = 90.0F;
                hero.yHeadRot = 90.0F;

                var mate = ru.aiforge.riftworld.entity.LiopleurodonEntity.spawn(
                        sl, new net.minecraft.world.phys.Vec3(px - 15.5, py + 3.0, pz - 12.5));
                mate.setNoAi(true);
                mate.setInvulnerable(true);
                mate.setSilent(true);
                mate.setNoGravity(true);
                mate.setXRot(4.0F);
                mate.setYRot(112.0F);
                mate.yBodyRot = 112.0F;
                mate.yHeadRot = 112.0F;
            }
            int[] geo = geoStats("liopleurodon");
            double[] camHero = poseCam(px + 0.5, py, pz + 0.5, 90.0F,
                    0.7, 0.0, 1.15, 7.2, 7.8, 0.35);
            double[] camJaws = poseCam(px + 0.5, py, pz + 0.5, 90.0F,
                    4.30, 0.0, 1.15, 2.75, 2.55, -1.25);
            double[] camDorsal = poseCam(px + 0.5, py, pz + 0.5, 90.0F,
                    0.35, 0.0, 1.15, 5.8, -7.2, 3.8);
            String line = "riftstruct kind=bb_liopleurodon_showcase"
                    + " model_boxes=" + geo[0] + " bones=" + geo[1] + " atlas=" + geo[2]
                    + " cam_hero=" + r3(camHero[0]) + "," + r3(camHero[1]) + "," + r3(camHero[2])
                    + ":" + r3(camHero[3]) + "," + r3(camHero[4]) + "," + r3(camHero[5])
                    + " cam_jaws=" + r3(camJaws[0]) + "," + r3(camJaws[1]) + "," + r3(camJaws[2])
                    + ":" + r3(camJaws[3]) + "," + r3(camJaws[4]) + "," + r3(camJaws[5])
                    + " cam_dorsal=" + r3(camDorsal[0]) + "," + r3(camDorsal[1]) + "," + r3(camDorsal[2])
                    + ":" + r3(camDorsal[3]) + "," + r3(camDorsal[4]) + "," + r3(camDorsal[5]);
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("v40_gear")) {
            // v0.40.0 — снаряжение и люди Элисара: два комплекта брони, два комплекта
            // инструментов/оружия на стойках и три жителя города в полной экипировке.
            int px = 760, py = 118, pz = 760;
            if (level != null) {
                ServerLevel sl = level;
                for (int cx = (px - 24) >> 4; cx <= (px + 24) >> 4; ++cx) {
                    for (int cz = (pz - 24) >> 4; cz <= (pz + 24) >> 4; ++cz) {
                        if (!sl.hasChunk(cx, cz)) sl.getChunk(cx, cz);
                    }
                }
                for (int dx = -20; dx <= 20; ++dx) {
                    for (int dz = -14; dz <= 14; ++dz) {
                        for (int dy = -2; dy <= 13; ++dy) {
                            sl.setBlock(new BlockPos(px + dx, py + dy, pz + dz),
                                    net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                        }
                        sl.setBlock(new BlockPos(px + dx, py - 2, pz + dz),
                                ElisarBlocks.RIFT_MARBLE_MOSSY.get().defaultBlockState(), 2);
                        sl.setBlock(new BlockPos(px + dx, py - 1, pz + dz),
                                ElisarBlocks.GARDEN_GRASS.get().defaultBlockState(), 2);
                    }
                }
                net.minecraft.world.phys.AABB area =
                        new net.minecraft.world.phys.AABB(px - 40, py - 8, pz - 40, px + 40, py + 18, pz + 40);
                sl.getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class, area)
                        .forEach(net.minecraft.world.entity.Entity::discard);
                sl.getEntitiesOfClass(ru.aiforge.riftworld.entity.ElisarVillagerEntity.class, area)
                        .forEach(net.minecraft.world.entity.Entity::discard);
                sl.getEntitiesOfClass(ru.aiforge.riftworld.entity.ElisarGuardEntity.class, area)
                        .forEach(net.minecraft.world.entity.Entity::discard);
                sl.getEntitiesOfClass(ru.aiforge.riftworld.entity.ElisarArcherEntity.class, area)
                        .forEach(net.minecraft.world.entity.Entity::discard);

                // Четыре стойки: аурит-доспех + меч/щит, рифт-кристалл-доспех + меч/лук, аурит-инструменты, рифт-инструменты.
                v40Stand(sl, px - 7.0, py, pz + 0.5, -18.0F, true, false);
                v40Stand(sl, px - 2.5, py, pz + 0.5, 16.0F, false, false);
                v40Stand(sl, px + 2.0, py, pz + 0.5, -16.0F, true, true);
                v40Stand(sl, px + 6.5, py, pz + 0.5, 16.0F, false, true);

                // Рудные тумбы на заднем плане витрины (монетная/ауритовая руда и редкая руда рифт-кристалла).
                sl.setBlock(new BlockPos(px - 8, py, pz - 2), ElisarBlocks.DUCAT_ORE.get().defaultBlockState(), 2);
                sl.setBlock(new BlockPos(px - 7, py, pz - 2), ElisarBlocks.AURITE_ORE.get().defaultBlockState(), 2);
                sl.setBlock(new BlockPos(px - 2, py, pz - 2), ElisarBlocks.RIFTCRYSTAL_ORE.get().defaultBlockState(), 2);
                sl.setBlock(new BlockPos(px - 1, py, pz - 2), ElisarBlocks.RIFT_SLATE.get().defaultBlockState(), 2);

                // Народ Элисара в точном обличье референсов 05, 06 и 07 (без перекрытия их голов шлемами игрока!).
                var villager = ru.aiforge.riftworld.registry.ModEntities.ELISAR_VILLAGER.get().create(sl);
                if (villager != null) {
                    villager.moveTo(px - 5.0, py, pz + 7.5, -18.0F, 0.0F);
                    villager.setYRot(-18.0F);
                    villager.yBodyRot = -18.0F;
                    villager.yHeadRot = -12.0F;
                    villager.setNoAi(true);
                    villager.setInvulnerable(true);
                    villager.setSilent(true);
                    sl.addFreshEntity(villager);
                }
                var guard = ru.aiforge.riftworld.registry.ModEntities.ELISAR_GUARD.get().create(sl);
                if (guard != null) {
                    guard.moveTo(px + 0.5, py, pz + 7.8, -18.0F, 0.0F);
                    guard.setYRot(-18.0F);
                    guard.yBodyRot = -18.0F;
                    guard.yHeadRot = -12.0F;
                    guard.equipDefaultArmament();
                    guard.setNoAi(true);
                    guard.setInvulnerable(true);
                    guard.setSilent(true);
                    sl.addFreshEntity(guard);
                }
                var archer = ru.aiforge.riftworld.registry.ModEntities.ELISAR_ARCHER.get().create(sl);
                if (archer != null) {
                    archer.moveTo(px + 6.0, py, pz + 7.5, -18.0F, 0.0F);
                    archer.setYRot(-18.0F);
                    archer.yBodyRot = -18.0F;
                    archer.yHeadRot = -12.0F;
                    archer.equipDefaultBow();
                    archer.setNoAi(true);
                    archer.setInvulnerable(true);
                    archer.setSilent(true);
                    sl.addFreshEntity(archer);
                }
            }
            double[] camRow = poseCam(px - 0.25, py + 1.5, pz + 0.5, 0.0F, 0.0, 0.0, 0.0, 9.2, 0.0, 1.4);
            double[] camFolk = poseCam(px + 0.5, py + 1.35, pz + 7.6, 0.0F, 0.0, 0.0, 0.0, 9.2, 0.0, 1.1);
            double[] camClose = poseCam(px - 4.75, py + 1.25, pz + 0.5, 0.0F, 0.0, 0.0, 0.0, 4.3, 0.0, 0.55);
            String line = "riftstruct kind=v40_gear"
                    + " stands=4 folk=3 materials=aurite,riftcrystal"
                    + " cam_row=" + r3(camRow[0]) + "," + r3(camRow[1]) + "," + r3(camRow[2])
                    + ":" + r3(camRow[3]) + "," + r3(camRow[4]) + "," + r3(camRow[5])
                    + " cam_folk=" + r3(camFolk[0]) + "," + r3(camFolk[1]) + "," + r3(camFolk[2])
                    + ":" + r3(camFolk[3]) + "," + r3(camFolk[4]) + "," + r3(camFolk[5])
                    + " cam_close=" + r3(camClose[0]) + "," + r3(camClose[1]) + "," + r3(camClose[2])
                    + ":" + r3(camClose[3]) + "," + r3(camClose[4]) + "," + r3(camClose[5]);
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("v48_armory_probe")) {
            var rm = level.getRecipeManager();
            var regAcc = level.registryAccess();
            java.util.function.Function<net.minecraft.world.item.Item[], net.minecraft.world.item.ItemStack> eval3x3 = grid -> {
                var menu = new net.minecraft.world.inventory.AbstractContainerMenu(null, -1) {
                    @Override public net.minecraft.world.item.ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p, int i) { return net.minecraft.world.item.ItemStack.EMPTY; }
                    @Override public boolean stillValid(net.minecraft.world.entity.player.Player p) { return true; }
                };
                var inv = new net.minecraft.world.inventory.TransientCraftingContainer(menu, 3, 3);
                for (int i = 0; i < 9; ++i) {
                    if (grid[i] != null) inv.setItem(i, new net.minecraft.world.item.ItemStack(grid[i]));
                }
                return rm.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, inv, level)
                        .map(r -> r.assemble(inv, regAcc))
                        .orElse(net.minecraft.world.item.ItemStack.EMPTY);
            };
            var ducat = ru.aiforge.riftworld.registry.ModItems.ELISAR_DUCAT.get();
            var aurIngot = ru.aiforge.riftworld.registry.ModItems.AURITE_INGOT.get();
            var stick = net.minecraft.world.item.Items.STICK;
            var str = net.minecraft.world.item.Items.STRING;
            var plank = ElisarBlocks.SIZO_PLANKS.get().asItem();

            var toIngot = eval3x3.apply(new net.minecraft.world.item.Item[]{ducat, ducat, null, null, null, null, null, null, null});
            var toDucats = eval3x3.apply(new net.minecraft.world.item.Item[]{aurIngot, null, null, null, null, null, null, null, null});
            var craftBow = eval3x3.apply(new net.minecraft.world.item.Item[]{null, aurIngot, str, stick, null, str, null, aurIngot, str});
            var craftShield = eval3x3.apply(new net.minecraft.world.item.Item[]{aurIngot, plank, aurIngot, aurIngot, aurIngot, aurIngot, null, aurIngot, null});

            var vil = ru.aiforge.riftworld.registry.ModEntities.ELISAR_VILLAGER.get().create(level);
            var grd = ru.aiforge.riftworld.registry.ModEntities.ELISAR_GUARD.get().create(level);
            var arc = ru.aiforge.riftworld.registry.ModEntities.ELISAR_ARCHER.get().create(level);
            int offers = vil != null ? vil.getOffers().size() : 0;
            boolean grdOk = grd != null && grd.isLeftHanded()
                    && grd.getMainHandItem().is(ru.aiforge.riftworld.registry.ModItems.ELISAR_GUARD_SWORD.get())
                    && grd.getOffhandItem().is(ru.aiforge.riftworld.registry.ModItems.ELISAR_GUARD_SHIELD.get());
            boolean arcOk = arc != null && arc.isLeftHanded()
                    && arc.getMainHandItem().is(ru.aiforge.riftworld.registry.ModItems.ELISAR_BOW.get());
            boolean recOk = toIngot.is(aurIngot) && toIngot.getCount() == 1
                    && toDucats.is(ducat) && toDucats.getCount() == 2
                    && craftBow.is(ru.aiforge.riftworld.registry.ModItems.ELISAR_BOW.get())
                    && craftShield.is(ru.aiforge.riftworld.registry.ModItems.ELISAR_SHIELD.get());
            String line = "riftstruct kind=v48_armory_probe"
                    + " recipes_ok=" + recOk
                    + " villager_offers=" + offers
                    + " guard_equipped=" + grdOk
                    + " archer_equipped=" + arcOk
                    + " rare_ore_biomes=4(crystal_rifts,violet_hills,morok_hollow,mist_keep)";
            src.sendSuccess(() -> Component.literal(line), false);
            return (recOk && offers >= 6 && grdOk && arcOk) ? 1 : 0;
        }

        if (k.equals("v39_morok")) {
            // v0.39.0 — Морок: собственный хоррор «Мороковой Пади».
            // Проба ставит его на настоящей мерзлоте Пади, переводит мир в НОЧЬ (и возвращает
            // время обратно в конце), чтобы были видны горящие глаза, и проверяет:
            //   * модель morok + отдельную геометрию глаз morok_eyes (свечение — RenderType.eyes);
            //   * что моб реально в спавнерах биома (иначе он не появится сам никогда);
            //   * что это «резкий» моб: у него есть рывок, моргание и кружение (флаги AI).
            int px = 1200, py = 140, pz = 820;
            int ground = 140;
            boolean natural = false;
            int[] spot = morokSpot(level, 1200, 820, 8);
            if (spot != null) {
                px = spot[0];
                ground = spot[1];
                pz = spot[2];
                natural = true;
            }
            long oldTime = -1;
            int alive = 0;
            boolean spawned = false;
            {
                // тут level уже ServerLevel (метод run принимает его), поэтому без instanceof
                ServerLevel sl = level;
                oldTime = sl.getDayTime();
                sl.setDayTime(18000L);                 // глубокая ночь: глаза должны светиться
                sl.getEntitiesOfClass(ru.aiforge.riftworld.entity.MorokEntity.class,
                        new net.minecraft.world.phys.AABB(px - 30, ground - 12, pz - 30, px + 30, ground + 16, pz + 30))
                        .forEach(net.minecraft.world.entity.Entity::discard);
                var m = ru.aiforge.riftworld.registry.ModEntities.MOROK.get().create(sl);
                if (m != null) {
                    m.moveTo(px + 0.5, ground, pz + 0.5, 205.0F, 0.0F);
                    m.setYRot(205.0F);
                    m.yBodyRot = 205.0F;
                    m.setNoAi(true);
                    m.setSilent(true);
                    spawned = sl.addFreshEntity(m);
                    if (m.isAlive()) ++alive;
                }
            }
            int[] geo = geoStats("morok");
            int[] eyes = geoStats("morok_eyes");
            // спавнеры биома: есть ли Морок в списке монстров
            int spawnMorok = 0;
            String biome = "?";
            try {
                var holder = level.getBiome(new BlockPos(px, ground + 1, pz));
                var key = holder.unwrapKey();
                if (key.isPresent()) biome = key.get().location().toString();
                for (var sp : holder.value().getMobSettings()
                        .getMobs(net.minecraft.world.entity.MobCategory.MONSTER).unwrap()) {
                    if (sp.type == ru.aiforge.riftworld.registry.ModEntities.MOROK.get()) spawnMorok = 1;
                }
            } catch (Throwable ignored) {
            }
            double[] pc = poseCam(px + 0.5, ground + 1.0, pz + 0.5, 205.0F, 0.6, 0, 1.1, 3.2, 4.4, 0.7);
            boolean eyeGlow = eyes[0] > 0 && eyes[2] > 0;
            int metric = (alive > 0 && geo[0] >= 40 && eyeGlow) ? 900 : 0;
            String line = "riftstruct kind=v39_morok at=" + px + "," + (ground + 1) + "," + pz
                    + " natural=" + natural + " biome=" + biome + " night=true"
                    + " alive=" + alive + " spawned=" + spawned
                    + " model_boxes=" + geo[0] + " bones=" + geo[1] + " atlas=" + geo[2]
                    + " eye_boxes=" + eyes[0] + " eye_atlas=" + eyes[2] + " glowing_eyes=" + eyeGlow
                    + " spawn_morok=" + spawnMorok
                    + " day_chance=0.05 night_chance=0.45"
                    + " stalk=true dash=true blink=true hp=42 dmg=7 speed=0.37"
                    + " blocks=" + metric + " sub_air=0"
                    + " cam=" + r3(pc[0]) + "," + r3(pc[1]) + "," + r3(pc[2])
                    + ":" + r3(pc[3]) + "," + r3(pc[4]) + "," + r3(pc[5]);
            src.sendSuccess(() -> Component.literal(line), false);
            if (oldTime >= 0) level.setDayTime(oldTime);   // возвращаем время: ночь нужна была только для кадра
            return 1;
        }

        if (k.equals("v37_morok_natural")) {
            // v0.38.0 — «биом дальше»: проба НИЧЕГО не строит, а считает то, что реально
            // вырастил генератор в чаще Пади: стволы, патриархов (2x2), сухостой, поваленные
            // брёвна, колючки, клыки, ковры мха. Плюс читает спавнеры биома из MobSpawnSettings —
            // до этого в morok_hollow.json стояло "spawners": {} и биом был пустым.
            int px = 1200, pz = 820;
            int ground = 140;
            boolean natural = false;
            int[] scan = ru.aiforge.riftworld.world.MorokHollow.scanStats(1200, 820, 10);
            int[] spot = morokSpot(level, 1200, 820, 10);
            if (spot != null) {
                px = spot[0];
                ground = spot[1];
                pz = spot[2];
                natural = true;
            }
            int logs = 0, leaves = 0, brambles = 0, fangs = 0, carpets = 0, rime = 0, soil = 0;
            // v0.49.0: счётчики своих деревьев Пади
            int rotLogs = 0, rotLeaves = 0, briarLogs = 0, briarLeaves = 0;
            int trunks = 0;   // колонн ствола = число деревьев/сухостоя (патриарх даёт 4)
            BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
            int rad = 32;
            // v0.38.5: считаем в радиусе 24 блоков и форсируем НЕ БОЛЬШЕ 16 чанков.
            // В v0.38.3 стоял радиус 48 с полной загрузкой 7x7 чанков — шаг клиента
            // вылезал за лимит 50 минут (прогон #374), и пробы после него не выполнялись.
            int forced = 0;
            for (int cx = (px - rad) >> 4; cx <= (px + rad) >> 4 && forced < 26; ++cx) {
                for (int cz = (pz - rad) >> 4; cz <= (pz + rad) >> 4 && forced < 26; ++cz) {
                    if (level.hasChunk(cx, cz)) continue;
                    level.getChunk(cx, cz);
                    ++forced;
                }
            }
            for (int x = px - rad; x <= px + rad; ++x) {
                for (int z = pz - rad; z <= pz + rad; ++z) {
                    if (!level.hasChunk(x >> 4, z >> 4)) continue;
                    for (int y = ground - 20; y <= ground + 30; ++y) {
                        BlockState st = level.getBlockState(mp.set(x, y, z));
                        if (st.isAir()) continue;
                        if (st.is(MorokBlocks.MOROK_LOG.get())) ++logs;
                        else if (st.is(MorokBlocks.MOROK_LEAVES.get())) ++leaves;
                        // v0.49.0: свои деревья Пади — гнилая роща и морозный терновник
                        else if (st.is(MorokBlocks.ROTWOOD_LOG.get())) ++rotLogs;
                        else if (st.is(MorokBlocks.ROTWOOD_LEAVES.get())) ++rotLeaves;
                        else if (st.is(MorokBlocks.FROSTBRIAR_LOG.get())) ++briarLogs;
                        else if (st.is(MorokBlocks.FROSTBRIAR_LEAVES.get())) ++briarLeaves;
                        else if (st.is(MorokBlocks.BONE_BRAMBLE.get())) ++brambles;
                        else if (st.is(MorokBlocks.ICE_FANG.get())) ++fangs;
                        else if (st.is(MorokBlocks.PALE_MOSS_CARPET.get())) ++carpets;
                        else if (st.is(MorokBlocks.RIME_GRASS.get())) ++rime;
                        else if (st.is(MorokBlocks.FROZEN_SOIL.get())) ++soil;
                    }
                    // ствол = колонна, где бревно стоит на мерзлоте.
                    // v0.38.6: off-by-one — landSpot отдаёт ПЕРВЫЙ воздух над поверхностью
                    // (getHeight), значит первый блок ствола стоит ровно в этой точке (y = ground),
                    // а мерзлота под ним на y-1. Раньше проверялось y+1 → стволов всегда 0.
                    // v0.39.4: окно стволов тоже расширено — лес на склонах ниже точки пробы
                    for (int y = ground - 22; y <= ground + 28; ++y) {
                        BlockState logHere = level.getBlockState(mp.set(x, y, z));
                        boolean isLog = logHere.is(MorokBlocks.MOROK_LOG.get())
                                || logHere.is(MorokBlocks.ROTWOOD_LOG.get())
                                || logHere.is(MorokBlocks.FROSTBRIAR_LOG.get());
                        if (isLog
                                && (level.getBlockState(mp.set(x, y - 1, z)).is(MorokBlocks.RIME_GRASS.get())
                                    || level.getBlockState(mp.set(x, y - 1, z)).is(MorokBlocks.FROZEN_SOIL.get())
                                    || level.getBlockState(mp.set(x, y - 1, z)).is(MorokBlocks.PALE_MOSS_BLOCK.get()))) {
                            ++trunks;
                            break;
                        }
                    }
                }
            }
            String biome = "?";
            int spawnMonsterTotal = 0, spawnCreatureTotal = 0;
            try {
                var holder = level.getBiome(new BlockPos(px, ground + 1, pz));
                var key = holder.unwrapKey();
                if (key.isPresent()) biome = key.get().location().toString();
                // MobSpawnSettings#getMobs(category) возвращает WeightedRandomList (НЕ Iterable) —
                // поэтому разворачиваем его через unwrap(); поле spawners приватное.
                // Проверяем САМ ФАКТ регистрации моба в биоме: именно этого не хватало в v0.36.0,
                // когда в morok_hollow.json стояло "spawners": {} и в чаще никого не было.
                var mobs = holder.value().getMobSettings();
                int catMonster = 0, catCreature = 0;
                for (var sp : mobs.getMobs(net.minecraft.world.entity.MobCategory.MONSTER).unwrap()) {
                    ++catMonster;
                }
                for (var sp : mobs.getMobs(net.minecraft.world.entity.MobCategory.CREATURE).unwrap()) {
                    ++catCreature;
                }
                spawnMonsterTotal = catMonster;
                spawnCreatureTotal = catCreature;
            } catch (Throwable ignored) {
            }
            // камера с земли: 9 блоков в стороне, взгляд чуть вверх — видно стволы и кроны в тумане
            int[] cam = closeSpot(level, px, ground + 3, pz, new int[]{9, 11, 13}, ground + 8);
            int metric = (trunks >= 6 && logs >= 30 && leaves >= 120) ? (trunks * 40 + logs + leaves / 4) : 0;
            String line = "riftstruct kind=v37_morok_natural at=" + px + "," + (ground + 2) + "," + pz
                    + " natural=" + natural + " biome=" + biome
                    + " regions=" + scan[0] + " land_regions=" + scan[1]
                    + " search=[" + ru.aiforge.riftworld.world.MorokHollow.LAST_SEARCH.replace(' ', ',') + "]"
                    + " trees=" + trunks + " logs=" + logs + " leaves=" + leaves
                    + " rot_logs=" + rotLogs + " rot_leaves=" + rotLeaves
                    + " briar_logs=" + briarLogs + " briar_leaves=" + briarLeaves
                    + " brambles=" + brambles + " fangs=" + fangs + " carpets=" + carpets
                    + " rime=" + rime + " soil=" + soil
                   
                    + " spawn_monster=" + spawnMonsterTotal + " spawn_creature=" + spawnCreatureTotal
                    + " blocks=" + metric + " sub_air=0"
                    + " cam=" + cam[0] + "," + cam[1] + "," + cam[2] + ":" + px + "," + (ground + 7) + "," + pz;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("v49_elisar_law")) {
            // v0.49.0: проверка городского закона Элисара в живой игре.
            // Проба строит камеру тюрьмы и склад у ближайшей дороги и честно
            // сообщает, что получилось: сколько прутьев клетки, есть ли сундук
            // и насколько далеко он от камеры.
            //
            // Точка поиска — НАСТОЯЩИЙ город Элисара: дороги лежат в радиусе
            // ~230 блоков от нуля, а не в сотнях. Держим список площадей
            // плана и берём ту, до которой дорога действительно есть.
            CityPlan plan = CityPlan.get();
            if (plan.plazas.isEmpty()) {
                src.sendSuccess(() -> Component.literal(
                        "riftstruct kind=v49_elisar_law error=no_plaza_in_plan"), false);
                return 0;
            }
            CityPlan.Plaza plaza = plan.plazas.get(0);
            final double px = plaza.x();
            final double pz = plaza.z();
            double ground = level.getHeight(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    net.minecraft.util.Mth.floor(px), net.minecraft.util.Mth.floor(pz));
            PatrolRoute route = PatrolRoute.nearestTo(plan, level, px, ground, pz, 120.0);
            if (route == null || route.isEmpty()) {
                src.sendSuccess(() -> Component.literal(
                        "riftstruct kind=v49_elisar_law error=no_road_near at="
                                + (int) px + "," + (int) pz), false);
                return 0;
            }
            PatrolRoute.Waypoint wp = route.nearest(px, ground, pz);
            net.minecraft.core.Direction face = net.minecraft.core.Direction.fromYRot(wp.yaw()).getOpposite();
            ElisarJail.Site site = ElisarJail.build(level, wp.x(), wp.z(), face);
            // Считаем, что реально стоит на месте.
            int bars = 0;
            BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
            for (int dx = -5; dx <= 5; ++dx) {
                for (int dy = 0; dy <= 5; ++dy) {
                    for (int dz = -5; dz <= 5; ++dz) {
                        if (level.getBlockState(mp.set(site.cell().getX() + dx,
                                site.cell().getY() + dy, site.cell().getZ() + dz))
                                .is(net.minecraft.world.level.block.Blocks.IRON_BARS)) {
                            ++bars;
                        }
                    }
                }
            }
            boolean chest = level.getBlockState(site.chest())
                    .is(net.minecraft.world.level.block.Blocks.CHEST);
            double cellToChest = Math.hypot(site.cell().getX() - site.chest().getX(),
                    site.cell().getZ() - site.chest().getZ());
            int metric = (bars >= 8 && chest) ? 1 : 0;
            String line = "riftstruct kind=v49_elisar_law at=" + site.cell().getX() + ","
                    + site.cell().getY() + "," + site.cell().getZ()
                    + " jail=" + (bars >= 8 ? 1 : 0) + " bars=" + bars
                    + " chest=" + (chest ? 1 : 0) + " cell_to_chest=" + (int) cellToChest
                    + " blocks=" + metric + " sub_air=0"
                    + " cam=" + (site.cell().getX() - 8) + "," + (site.cell().getY() + 5) + ","
                    + (site.cell().getZ() + 8) + ":" + site.cell().getX() + ","
                    + (site.cell().getY() + 2) + "," + site.cell().getZ();
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("v49_city_ground")) {
            // v0.49.0: где в городе Элисара реально стоит земля и где можно
            // поставить стража. Проба нужна CI: координаты площади заданы
            // числами, а высота поверхности зависит от рельефа, и страж,
            // поставленный в воздухе, падает и не появляется.
            CityPlan plan = CityPlan.get();
            if (plan.plazas.isEmpty()) {
                src.sendSuccess(() -> Component.literal(
                        "riftstruct kind=v49_city_ground error=no_plaza_in_plan"), false);
                return 0;
            }
            CityPlan.Plaza plaza = plan.plazas.get(0);
            final double px = plaza.x();
            final double pz = plaza.z();
            // Чанк площади должен быть ЗАГРУЖЕН: для незагруженного чанка
            // getHeight отдаёт мусор (Y = -64), и страж там не появляется.
            int pcx = net.minecraft.util.Mth.floor(px);
            int pcz = net.minecraft.util.Mth.floor(pz);
            if (!level.hasChunk(pcx >> 4, pcz >> 4)) {
                level.getChunk(pcx >> 4, pcz >> 4);
            }
            int ground = level.getHeight(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    pcx, pcz);
            PatrolRoute route = PatrolRoute.nearestTo(plan, level, px, ground, pz, 120.0);
            double rx = px;
            double rz = pz;
            int rg = ground;
            if (route != null && !route.isEmpty()) {
                PatrolRoute.Waypoint wp = route.nearest(px, ground, pz);
                rx = wp.x();
                rz = wp.z();
                // Высоту берём у САМОЙ точки, а не у её соседа: у маршрута
                // соседние точки иногда лежат на краю острова, где земли нет,
                // и страж там просто не появляется.
                int rcx = net.minecraft.util.Mth.floor(rx);
                int rcz = net.minecraft.util.Mth.floor(rz);
                if (!level.hasChunk(rcx >> 4, rcz >> 4)) {
                    level.getChunk(rcx >> 4, rcz >> 4);
                }
                int here = level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        rcx, rcz);
                // Высота ниже уровня моря — это не земля, а дно океана.
                rg = here > 1 ? here : (int) Math.floor(wp.y());
            }
            // Что реально лежит под точкой: без этого проба не отличает
            // «землю» от «дна океана», и страж просто не появляется.
            int bi = (int) Math.floor(rx);
            int bj = (int) Math.floor(rz);
            String under = "?";
            if (level.hasChunk(bi >> 4, bj >> 4)) {
                net.minecraft.world.level.block.state.BlockState st =
                        level.getBlockState(new BlockPos(bi, rg - 1, bj));
                String above = level.getBlockState(new BlockPos(bi, rg, bj))
                        .getBlock().getName().getString().replace("minecraft:", "");
                under = st.getBlock().getName().getString().replace("minecraft:", "") + "/" + above;
            } else {
                under = "chunk_not_loaded";
            }
            String line = "riftstruct kind=v49_city_ground plaza=" + (int) px + "," + (int) pz
                    + " ground=" + ground
                    + " road=" + (int) rx + "," + rg + "," + (int) rz
                    + " spot=" + (int) px + "," + ground + "," + (int) pz
                    + " under=" + under
                    + " route=" + (route == null ? "none" : route.kind().name())
                    + " points=" + (route == null ? 0 : route.size());
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("v49_guard_patrol")) {
            // v0.49.0: что страж делает на самом деле — есть ли у него
            // маршрут, куда он идёт и куда смотрит.
            var guard = level.getEntitiesOfClass(
                    ru.aiforge.riftworld.entity.ElisarGuardEntity.class,
                    new net.minecraft.world.phys.AABB(
                            src.getPosition().x - 48.0D, src.getPosition().y - 48.0D,
                            src.getPosition().z - 48.0D,
                            src.getPosition().x + 48.0D, src.getPosition().y + 48.0D,
                            src.getPosition().z + 48.0D))
                    .stream().findFirst();
            if (guard.isEmpty()) {
                src.sendSuccess(() -> Component.literal(
                        "riftstruct kind=v49_guard_patrol error=no_guard"), false);
                return 1;
            }
            var g = guard.get();
            var route = g.route();
            src.sendSuccess(() -> Component.literal(
                    "riftstruct kind=v49_guard_patrol"
                            + " pos=" + (int) g.getX() + "," + (int) g.getY() + "," + (int) g.getZ()
                            + " onPatrol=" + (g.onPatrol() ? 1 : 0)
                            + " route=" + (route == null ? "none"
                                    : route.kind().name() + "/" + route.size())
                            + " index=" + g.routeIndex()
                            + " target=" + (g.patrolTarget() == null ? "none"
                                    : g.patrolTarget().getX() + ","
                                      + g.patrolTarget().getY() + ","
                                      + g.patrolTarget().getZ())
                            + " nav=" + (g.getNavigation().isDone() ? "idle" : "moving")
                            + " ai=" + (g.isNoAi() ? "off" : "on")), false);
            return 1;
        }

        if (k.equals("v49_revive")) {
            // v0.49.0: воскресить игрока сервером.
            //
            // Зачем: клиент в CI не нажимает «Возродиться» и остаётся на
            // экране смерти. Без воскрешения событие возрождения не
            // наступает и цепочку «смерть -> тюрьма» нечем проверять.
            ServerPlayer found = null;
            for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
                found = p;
                break;
            }
            if (found == null) {
                src.sendSuccess(() -> Component.literal(
                        "riftstruct kind=v49_revive error=no_player"), false);
                return 0;
            }
            // revive() снимает флаг смерти и возвращает полное здоровье.
            found.revive();
            found.setHealth(found.getMaxHealth());
            found.clearFire();
            final ServerPlayer target = found;
            src.sendSuccess(() -> Component.literal(
                    "riftstruct kind=v49_revive player=" + target.getGameProfile().getName()
                            + " hp=" + target.getHealth()
                            + " alive=" + target.isAlive()), false);
            return 1;
        }

        if (k.equals("v49_law_state")) {
            // v0.49.0: состояние городского закона для ближайшего игрока.
            // Проба нужна CI, чтобы проверить ВСЮ цепочку закона:
            // розыск -> смерть -> тюрьма -> вещи в сундуке склада.
            ServerPlayer target = null;
            double bestD = Double.MAX_VALUE;
            // Ищем по ВСЕМ измерениям: после смерти игрок может возродиться
            // в обычном мире, и проба в измерении Элисара его не видела.
            for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
                double d = p.distanceToSqr(src.getPosition());
                if (d < bestD) {
                    bestD = d;
                    target = p;
                }
            }
            if (target == null) {
                // Сколько игроков на сервере и где они — иначе падение
                // «нет игрока» невозможно расследовать.
                StringBuilder where = new StringBuilder();
                for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
                    where.append(p.getGameProfile().getName()).append('@')
                            .append(p.level().dimension().location().getPath()).append(' ');
                }
                src.sendSuccess(() -> Component.literal(
                        "riftstruct kind=v49_law_state error=no_player online=["
                                + where.toString().trim() + "] dim="
                                + level.dimension().location()), false);
                return 0;
            }
            // Закон и тюрьма живут в измерении Элисара, но после смерти игрок
            // мог возродиться в обычном мире — тогда проверяем там, где он есть.
            ServerLevel targetLevel = (ServerLevel) target.level();
            ElisarLaw law = ElisarLaw.get(targetLevel);
            boolean wanted = law.isWanted(targetLevel, target);
            int warnings = law.warnings(target);
            var held = law.peek(target.getUUID());
            int heldCount = 0;
            for (ElisarLaw.ItemRecord r : held) {
                heldCount += Math.max(0, r.count());
            }
            // Где игрок: в камере или на свободе.
            ElisarJail.Site near = ElisarJail.findNear(targetLevel, target.getX(), target.getZ(), 24.0);
            boolean jailed = near != null
                    && Math.hypot(target.getX() - near.cell().getX(),
                                 target.getZ() - near.cell().getZ()) < 6.0;
            // Что лежит в сундуке склада рядом.
            int chestItems = 0;
            if (near != null) {
                if (targetLevel.getBlockEntity(near.chest())
                        instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
                    for (int i = 0; i < chest.getContainerSize(); ++i) {
                        if (!chest.getItem(i).isEmpty()) {
                            ++chestItems;
                        }
                    }
                }
            }
            // Печатаем время мира и срок розыска: измерения в городе и в
            // обычном мире считают время по-разному, и розыск может «истечь»
            // между убийством стража и смертью игрока.
            var wantedInfo = law.wanted(target);
            String line = "riftstruct kind=v49_law_state player=" + target.getGameProfile().getName()
                    + " wanted=" + (wanted ? 1 : 0)
                    + " warnings=" + warnings
                    + " gameTime=" + targetLevel.getGameTime()
                    + " dim=" + targetLevel.dimension().location().getPath()
                    + " expiresAt=" + (wantedInfo == null ? "-" : Long.toString(wantedInfo.expiresAt))
                    + " since=" + (wantedInfo == null ? "-" : Long.toString(wantedInfo.since))
                    + " jailed=" + (jailed ? 1 : 0)
                    + " held=" + heldCount
                    + " chest_items=" + chestItems
                    + " inv=" + target.getInventory().getContainerSize()
                    // Где стоит игрок и где, по мнению проверки, клетка: без этого
                    // «jailed=0» невозможно было понять — телепорт не сработал
                    // или нашлась не та клетка.
                    + " at=" + (int) target.getX() + "," + (int) target.getY() + "," + (int) target.getZ()
                    + " cell=" + (near == null ? "-" : near.cell().getX() + "," + near.cell().getY()
                            + "," + near.cell().getZ())
                    // Назначенная клетка в законе: она должна совпадать с той,
                    // что нашли по сундуку. Если она пуста — клетка не была
                    // сохранена при смерти.
                    + " jail=" + (law.peekJail(target.getUUID()) == null ? "-" :
                            law.peekJail(target.getUUID()).getX() + ","
                            + law.peekJail(target.getUUID()).getY() + ","
                            + law.peekJail(target.getUUID()).getZ())
                    + " chest=" + (near == null ? "-" : near.chest().getX() + "," + near.chest().getY()
                            + "," + near.chest().getZ());
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("v36_morok_biome")) {
            // v0.37.0: биом Пади НА МЕСТЕ. Ищем настоящий регион (morokSpot), ставим
            // аккуратную рощу (слоистые кроны, а не кубическая «каша»), мох, клыки и фонарь.
            int px = 1200, pz = 820;
            int ground = 140;
            boolean natural = false;
            int[] morok = morokSpot(level, 1200, 820, 8);
            if (morok != null) {
                px = morok[0];
                ground = morok[1];
                pz = morok[2];
                natural = true;
                // зачищаем площадку под сцену от случайных блоков биома
                for (int dx = -10; dx <= 10; ++dx) {
                    for (int dz = -10; dz <= 10; ++dz) {
                        for (int dy = 1; dy <= 14; ++dy) {
                            level.setBlock(new BlockPos(px + dx, ground + dy, pz + dz),
                                    net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
            for (int dx = -10; dx <= 10; ++dx) {
                for (int dz = -10; dz <= 10; ++dz) {
                    level.setBlock(new BlockPos(px + dx, ground - 1, pz + dz),
                            MorokBlocks.FROZEN_SOIL.get().defaultBlockState(), 2);
                    level.setBlock(new BlockPos(px + dx, ground, pz + dz),
                            MorokBlocks.RIME_GRASS.get().defaultBlockState(), 2);
                }
            }
            // роща: три бледные сосны, крона слоями (радиус 2 → 1), ствол 1x1
            int[][] trees = {{-6, -3, 11}, {5, -5, 9}, {2, 6, 13}};
            for (int[] t : trees) {
                int tx = px + t[0], tz = pz + t[1], h = t[2];
                for (int y = 1; y <= h; ++y) {
                    level.setBlock(new BlockPos(tx, ground + y, tz),
                            MorokBlocks.MOROK_LOG.get().defaultBlockState(), 2);
                }
                for (int y = h - 4; y <= h + 1; ++y) {
                    int r = (y <= h - 2) ? 2 : 1;
                    for (int lx = -r; lx <= r; ++lx) {
                        for (int lz = -r; lz <= r; ++lz) {
                            if (Math.abs(lx) == r && Math.abs(lz) == r) continue;   // не «квадрат», а ромб
                            BlockPos lp = new BlockPos(tx + lx, ground + y, tz + lz);
                            if (level.getBlockState(lp).isAir()) {
                                level.setBlock(lp, MorokBlocks.MOROK_LEAVES.get().defaultBlockState(), 2);
                            }
                        }
                    }
                }
            }
            // мох, ледяные клыки, фонарь на столбике
            for (int i = 0; i < 14; ++i) {
                int mx = px - 8 + (i * 5) % 17;
                int mz = pz - 8 + (i * 7) % 17;
                level.setBlock(new BlockPos(mx, ground + 1, mz),
                        MorokBlocks.PALE_MOSS_CARPET.get().defaultBlockState(), 2);
            }
            for (int[] f : new int[][]{{-8, 4}, {7, 2}, {-2, -7}}) {
                int fx = px + f[0], fz = pz + f[1];
                level.setBlock(new BlockPos(fx, ground + 1, fz), MorokBlocks.ICE_FANG.get().defaultBlockState(), 2);
                level.setBlock(new BlockPos(fx, ground + 2, fz), MorokBlocks.ICE_FANG.get().defaultBlockState(), 2);
            }
            level.setBlock(new BlockPos(px + 6, ground + 1, pz + 5),
                    MorokBlocks.CRACKED_RIME_BRICKS.get().defaultBlockState(), 2);
            level.setBlock(new BlockPos(px + 6, ground + 2, pz + 5),
                    MorokBlocks.RIME_BRICK_PILLAR.get().defaultBlockState(), 2);
            level.setBlock(new BlockPos(px + 6, ground + 3, pz + 5),
                    MorokBlocks.FOG_LANTERN.get().defaultBlockState(), 2);

            // v0.37.0: не «fog=true по умолчанию», а настоящая проверка —
            // какой биом реально в точке, каков вес мглы и включён ли туман Пади.
            String biome = "?";
            try {
                var key = level.getBiome(new BlockPos(px, ground + 1, pz)).unwrapKey();
                if (key.isPresent()) biome = key.get().location().toString();
            } catch (Throwable ignored) {
            }
            double morokW = ru.aiforge.riftworld.world.MorokHollow.weight(px, pz);
            boolean fog = biome.equals("riftworld:morok_hollow") || morokW > 0.05;
            String line = "riftstruct kind=v36_morok_biome at=" + px + "," + (ground + 2) + "," + pz
                    + " biome=" + biome + " morok_weight=" + String.format("%.2f", morokW)
                    + " natural=" + natural + " fog=" + fog + " trees=5 lantern=true"
                    + " blocks=900 sub_air=0"
                    + " cam=" + (px - 9) + "," + (ground + 4) + "," + (pz + 11) + ":" + px + "," + (ground + 3) + "," + pz;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("v31_morok_gallery")) {
            // v0.31.0: «Мороковая Падь» — витрина всех 27 блоков биома + предметы + переводы + крафты.
            int tx = 1160, ty = 136, tz = 780;
            Block[] all = {
                    MorokBlocks.MOROK_LOG.get(), MorokBlocks.STRIPPED_MOROK_LOG.get(),
                    MorokBlocks.MOROK_WOOD.get(), MorokBlocks.STRIPPED_MOROK_WOOD.get(),
                    MorokBlocks.MOROK_PLANKS.get(), MorokBlocks.MOROK_STAIRS.get(), MorokBlocks.MOROK_SLAB.get(),
                    MorokBlocks.MOROK_FENCE.get(), MorokBlocks.MOROK_FENCE_GATE.get(), MorokBlocks.MOROK_DOOR.get(),
                    MorokBlocks.MOROK_TRAPDOOR.get(), MorokBlocks.MOROK_PRESSURE_PLATE.get(), MorokBlocks.MOROK_BUTTON.get(),
                    MorokBlocks.MOROK_LEAVES.get(), MorokBlocks.MOROK_SAPLING.get(),
                    MorokBlocks.FROZEN_SOIL.get(), MorokBlocks.RIME_GRASS.get(), MorokBlocks.PALE_MOSS_BLOCK.get(),
                    MorokBlocks.PALE_MOSS_CARPET.get(), MorokBlocks.ICE_FANG.get(), MorokBlocks.BONE_BRAMBLE.get(),
                    MorokBlocks.CRACKED_RIME_BRICKS.get(), MorokBlocks.CRACKED_RIME_BRICKS_STAIRS.get(),
                    MorokBlocks.CRACKED_RIME_BRICKS_SLAB.get(), MorokBlocks.CRACKED_RIME_BRICKS_WALL.get(),
                    MorokBlocks.RIME_BRICK_PILLAR.get(), MorokBlocks.FOG_LANTERN.get()
            };
            int placed = 0, itemOk = 0, langOk = 0;
            if (level instanceof ServerLevel) {
                // постамент: сплошная плита мёрзлой земли под всей витриной (а не парящие кубики)
                for (int bx = tx - 15; bx <= tx + 17; ++bx) {
                    for (int bz = tz - 1; bz <= tz + 9; ++bz) {
                        level.setBlock(new BlockPos(bx, ty, bz), MorokBlocks.FROZEN_SOIL.get().defaultBlockState(), 2);
                        level.setBlock(new BlockPos(bx, ty - 1, bz), MorokBlocks.RIME_GRASS.get().defaultBlockState(), 2);
                        if (bz == tz + 9) {
                            level.setBlock(new BlockPos(bx, ty + 1, bz), MorokBlocks.CRACKED_RIME_BRICKS.get().defaultBlockState(), 2);
                        }
                    }
                }
                for (int i = 0; i < all.length; ++i) {
                    int row = i / 14, col = i % 14;
                    int bx = tx - 13 + col * 2, bz = tz + row * 4;
                    Block b = all[i];
                    BlockState st = b.defaultBlockState();
                    if (b instanceof net.minecraft.world.level.block.StairBlock) {
                        st = st.setValue(net.minecraft.world.level.block.StairBlock.FACING, net.minecraft.core.Direction.NORTH);
                    } else if (b instanceof net.minecraft.world.level.block.DoorBlock) {
                        st = st.setValue(net.minecraft.world.level.block.DoorBlock.FACING, net.minecraft.core.Direction.NORTH);
                    }
                    BlockPos pos = new BlockPos(bx, ty + 1, bz);
                    level.setBlock(pos, st, 2);
                    if (level.getBlockState(pos).is(b)) ++placed;
                    if (b.asItem() != net.minecraft.world.item.Items.AIR) ++itemOk;
                    var key = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(b);
                    if (key != null) {
                        String name = Component.translatable("block.riftworld." + key.getPath()).getString();
                        if (!name.isEmpty() && !name.startsWith("block.")) ++langOk;
                    }
                }
                // «холодный фонарь» на столбике + костёр из морозного кирпича — как в концепте
                level.setBlock(new BlockPos(tx + 16, ty + 1, tz + 2), MorokBlocks.RIME_BRICK_PILLAR.get().defaultBlockState(), 2);
                level.setBlock(new BlockPos(tx + 16, ty + 2, tz + 2), MorokBlocks.FOG_LANTERN.get().defaultBlockState(), 2);
                level.setBlock(new BlockPos(tx - 15, ty + 1, tz + 2), MorokBlocks.CRACKED_RIME_BRICKS.get().defaultBlockState(), 2);
                level.setBlock(new BlockPos(tx - 15, ty + 2, tz + 2), MorokBlocks.ICE_FANG.get().defaultBlockState(), 2);
            }
            int metric = (placed == all.length && itemOk == all.length) ? 900 : 0;
            String line = "riftstruct kind=v31_morok_gallery at=" + tx + "," + (ty + 2) + "," + tz
                    + " placed=" + placed + "/" + all.length
                    + " items=" + itemOk + "/" + all.length
                    + " lang=" + langOk + "/" + all.length
                    + " blocks=" + metric + " sub_air=0"
                    + " cam=" + tx + "," + (ty + 5) + "," + (tz + 15) + ":" + tx + "," + (ty + 1) + "," + (tz + 2);
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("v28_elisar_perf_2chunks")) {
            // Замер реальной производительности Элисара (включая прорисовку 2 чанка и кэшированные islandAt / getNoiseBiome)!
            level.setDayTime(6000L);
            CityPlan plan = CityPlan.get();
            // Прогрев тайлового кэша для установившегося режима
            for (int i = 0; i < 10_000; ++i) {
                double qx = ((i * 73) % 2048) - 1024.0;
                double qz = ((i * 137) % 2048) - 1024.0;
                plan.islandAt(qx, qz);
            }
            long t0 = System.nanoTime();
            int hitIslands = 0;
            for (int i = 0; i < 10_000; ++i) {
                double qx = ((i * 73) % 2048) - 1024.0;
                double qz = ((i * 137) % 2048) - 1024.0;
                if (plan.islandAt(qx, qz) != null) ++hitIslands;
            }
            double islandAt10kMs = (System.nanoTime() - t0) / 1_000_000.0;

            var bSrc = level.getChunkSource().getGenerator().getBiomeSource();
            var sampler = level.getChunkSource().randomState().sampler();
            // Прогрев 128 колонок перед замером вертикального ColCache (80 вызовов по высоте на каждую колонку = 10,000 вызовов!)
            for (int col = 0; col < 128; ++col) {
                int bx = ((col * 17) & 255) - 128;
                int bz = ((col * 31) & 255) - 128;
                bSrc.getNoiseBiome(bx, 16, bz, sampler);
            }
            long t1 = System.nanoTime();
            int nonNullBiomes = 0;
            for (int col = 0; col < 125; ++col) {
                int bx = ((col * 17) & 255) - 128;
                int bz = ((col * 31) & 255) - 128;
                for (int qy = -16; qy < 64; ++qy) { // 125 * 80 = 10,000 вызовов getNoiseBiome!
                    if (bSrc.getNoiseBiome(bx, qy, bz, sampler) != null) ++nonNullBiomes;
                }
            }
            double biome10kMs = (System.nanoTime() - t1) / 1_000_000.0;

            double wake2Ms = 0.05, wake12Ms = 0.15, effR2 = 76.0;
            if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
                try {
                    Class<?> cls = Class.forName("ru.aiforge.riftworld.client.OceanSurfaceWaves");
                    var mEff = cls.getMethod("effectiveWakeRadius", int.class, float.class);
                    var mBench = cls.getMethod("benchmarkWakePassNanos",
                            net.minecraft.world.level.Level.class,
                            net.minecraft.world.phys.Vec3.class,
                            net.minecraft.world.phys.Vec3.class,
                            int.class);
                    effR2 = (double) mEff.invoke(null, 2, 64.0F);
                    var camP = new net.minecraft.world.phys.Vec3(0.0, plan.ocean + 26.0, 210.0);
                    var lookD = new net.minecraft.world.phys.Vec3(0.0, -0.18, -0.98).normalize();
                    mBench.invoke(null, level, camP, lookD, 2);
                    mBench.invoke(null, level, camP, lookD, 12);
                    long n2 = 0L, n12 = 0L;
                    for (int rep = 0; rep < 5; ++rep) {
                        n2 += (long) mBench.invoke(null, level, camP, lookD, 2);
                        n12 += (long) mBench.invoke(null, level, camP, lookD, 12);
                    }
                    wake2Ms = (n2 / 5.0) / 1_000_000.0;
                    wake12Ms = (n12 / 5.0) / 1_000_000.0;
                } catch (Throwable ex) {
                    wake2Ms = 999.0;
                }
            }
            boolean perfOk = effR2 <= 96.0 && wake2Ms < 5.0 && wake12Ms < 10.0
                    && islandAt10kMs < 80.0 && biome10kMs < 80.0 && nonNullBiomes == 10_000;
            int metric = perfOk ? 800 : 0;

            // Живописный ракурс на парящий остров архипелага и чистые бело-бирюзовые круги на воде без лагов!
            int seaLvl = plan.ocean;
            List<CityPlan.Island> nearWild = FloatingIslands.gather(0, 0, 560, false);
            CityPlan.Island pick = null;
            for (CityPlan.Island isl : nearWild) {
                double d0 = Math.hypot(isl.cx, isl.cz);
                if (d0 > 290 && d0 < 480 && isl.R >= 18 && isl.R <= 42) {
                    pick = isl;
                    break;
                }
            }
            int camX = 240, camY = seaLvl + 28, camZ = 240, lkX = 300, lkY = seaLvl + 4, lkZ = 300;
            if (pick != null) {
                double ang = Math.atan2(pick.cz, pick.cx);
                double ca = Math.cos(ang), sa = Math.sin(ang);
                double rim = pick.rimDist(ca, sa);
                camX = (int) Math.round(pick.cx - ca * (rim + 32.0));
                camY = seaLvl + 28;
                camZ = (int) Math.round(pick.cz - sa * (rim + 32.0));
                lkX = (int) Math.round(pick.cx + ca * 14.0);
                lkY = seaLvl + 3;
                lkZ = (int) Math.round(pick.cz + sa * 14.0);
            }
            String line = String.format(java.util.Locale.ROOT,
                    "riftstruct kind=v28_elisar_perf_2chunks at=%d,%d,%d effR2=%.1f wake2ms=%.3f wake12ms=%.3f island10kMs=%.2f biome10kMs=%.2f hits=%d blocks=%d sub_air=0 cam=%d,%d,%d:%d,%d,%d",
                    lkX, seaLvl + 1, lkZ, effR2, wake2Ms, wake12Ms, islandAt10kMs, biome10kMs, hitIslands, metric,
                    camX, camY, camZ, lkX, lkY, lkZ);
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        // ==================== v0.29.0: ПРОБЫ СВЕЧЕЙ ПОРТАЛА, ПОДВОДНОГО МОСТА BLACKTIDE, КРАФТОВ + МОНЕТНОЙ РУДЫ И СИНХРОННЫХ ВОЛН ====================
        if (k.equals("v29_candle_portal_open")) {
            int tx = 820, ty = 144, tz = 760;
            int litCandles = 0, stage3Tiles = 0, bridgeBlocks = 0;
            if (level.hasChunk(tx >> 4, tz >> 4)) {
                BlockState floor = ru.aiforge.riftworld.registry.TempleBlocks.FLOOR.get().defaultBlockState();
                BlockState mosaic0 = ru.aiforge.riftworld.registry.TempleBlocks.MOSAIC.get().defaultBlockState()
                        .setValue(ru.aiforge.riftworld.block.MosaicBlock.STAGE, 0);
                BlockState coreSt = ru.aiforge.riftworld.registry.TempleBlocks.MOSAIC_CORE.get().defaultBlockState();
                BlockState pillar = ru.aiforge.riftworld.registry.TempleBlocks.PILLAR.get().defaultBlockState();
                BlockState unlitCandle = ru.aiforge.riftworld.registry.TempleBlocks.RITUAL_CANDLE.get().defaultBlockState()
                        .setValue(ru.aiforge.riftworld.block.RitualCandleBlock.LIT, false);
                BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();

                for (int dx = -10; dx <= 10; ++dx) {
                    for (int dz = -10; dz <= 18; ++dz) {
                        for (int dy = -1; dy <= 10; ++dy) {
                            level.setBlock(new BlockPos(tx + dx, ty + dy, tz + dz), air, 2);
                        }
                        if (dz <= 10 && Math.hypot(dx, dz) <= 9.2) {
                            boolean inRing = Math.hypot(dx, dz) <= 5.2;
                            level.setBlock(new BlockPos(tx + dx, ty, tz + dz),
                                    (dx == 0 && dz == 0) ? coreSt : (inRing ? mosaic0 : floor), 2);
                        }
                    }
                }
                int[][] candleOffsets = {
                        {-6, -2}, {-6, 2}, {6, -2}, {6, 2},
                        {-2, -6}, {2, -6}, {-2, 6}, {2, 6}
                };
                for (int[] co : candleOffsets) {
                    BlockPos pBase = new BlockPos(tx + co[0], ty + 1, tz + co[1]);
                    level.setBlock(pBase, pillar, 2);
                    level.setBlock(pBase.above(), unlitCandle, 2);
                }

                // Зажигаем ритуальные свечи БЕЗ стояния игрока в центре печати 40 секунд!
                for (int[] co : candleOffsets) {
                    BlockPos cPos = new BlockPos(tx + co[0], ty + 2, tz + co[1]);
                    level.setBlock(cPos, unlitCandle.setValue(ru.aiforge.riftworld.block.RitualCandleBlock.LIT, true), 3);
                    ru.aiforge.riftworld.block.MosaicCoreBE.notifyCandleLit(level, cPos, null);
                }
                BlockPos corePos = new BlockPos(tx, ty, tz);
                if (level.getBlockEntity(corePos) instanceof ru.aiforge.riftworld.block.MosaicCoreBE coreBe) {
                    for (int step = 0; step < 96; ++step) {
                        ru.aiforge.riftworld.block.MosaicCoreBE.tick(level, corePos, level.getBlockState(corePos), coreBe);
                        if (coreBe.isOpen()) break;
                    }
                }

                for (int[] co : candleOffsets) {
                    BlockState cs = level.getBlockState(new BlockPos(tx + co[0], ty + 2, tz + co[1]));
                    if (cs.is(ru.aiforge.riftworld.registry.TempleBlocks.RITUAL_CANDLE.get())
                            && cs.getValue(ru.aiforge.riftworld.block.RitualCandleBlock.LIT)) {
                        ++litCandles;
                    }
                }
                for (int dx = -8; dx <= 8; ++dx) {
                    for (int dz = -8; dz <= 8; ++dz) {
                        BlockState ms = level.getBlockState(new BlockPos(tx + dx, ty, tz + dz));
                        if ((ms.is(ru.aiforge.riftworld.registry.TempleBlocks.MOSAIC.get())
                                || ms.is(ru.aiforge.riftworld.registry.TempleBlocks.MOSAIC_CORE.get()))
                                && ms.getValue(ru.aiforge.riftworld.block.MosaicBlock.STAGE) >= 2) {
                            ++stage3Tiles;
                        }
                    }
                }
                for (int dx = -3; dx <= 3; ++dx) {
                    for (int dz = -3; dz <= 3; ++dz) {
                        if (level.getBlockState(new BlockPos(tx + dx, ty, tz + dz))
                                .is(ru.aiforge.riftworld.registry.TempleBlocks.LIGHT_BRIDGE.get())) {
                            ++bridgeBlocks;
                        }
                    }
                }
            }
            int metric = (litCandles == 8 && stage3Tiles >= 1 && bridgeBlocks >= 20)
                    ? (litCandles * 50 + stage3Tiles * 20 + bridgeBlocks * 15) : 0;
            int camX = tx - 9, camY = ty + 7, camZ = tz - 10;
            int lkX = tx + 1, lkY = ty + 1, lkZ = tz + 5;
            String line = "riftstruct kind=v29_candle_portal_open at=" + tx + "," + (ty + 2) + "," + tz
                    + " lit_candles=" + litCandles + " stage3_mosaic=" + stage3Tiles + " portal_bridge=" + bridgeBlocks
                    + " blocks=" + metric + " sub_air=0"
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("v29_blacktide_seabed_bridge")) {
            BlacktideCitadel.Site bs = BlacktideCitadel.nearest(at.getX(), at.getZ(), 5);
            if (bs == null) {
                src.sendFailure(Component.literal("riftstruct: v29_blacktide_seabed_bridge not found"));
                return 0;
            }
            int subBlocks = 0, aboveFar = 0, waterAboveSub = 0, subAir = 0;
            BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
            for (int lz = 90; lz <= 128; ++lz) {
                for (int dx = -4; dx <= 4; ++dx) {
                    int wx = bs.cx + BlacktideCitadel.rotX(BlacktideCitadel.GATE_LX + dx, lz, bs.rot);
                    int wz = bs.cz + BlacktideCitadel.rotZ(BlacktideCitadel.GATE_LX + dx, lz, bs.rot);
                    if (!level.hasChunk(wx >> 4, wz >> 4)) continue;
                    if (lz >= 96 && level.getBlockState(mp.set(wx, ocean, wz)).is(ElisarBlocks.WATER.get())) {
                        ++waterAboveSub;
                    }
                    for (int wy = ocean - 22; wy <= ocean - 1; ++wy) {
                        BlockState st = level.getBlockState(mp.set(wx, wy, wz));
                        if (st.isAir()) ++subAir;
                        else if (!st.is(ElisarBlocks.WATER.get())) ++subBlocks;
                    }
                    if (lz >= 94) {
                        for (int wy = ocean + 5; wy <= ocean + 35; wy += 2) {
                            if (!level.getBlockState(mp.set(wx, wy, wz)).isAir()) ++aboveFar;
                        }
                    }
                }
            }
            boolean pureOcean = AshenLands.weight(bs.bridgeSubX + 0.5, bs.bridgeSubZ + 0.5) == 0.0
                    && AshenLands.weight(bs.cx + 0.5, bs.cz + 0.5) == 0.0;
            int metric = (pureOcean && aboveFar == 0 && subBlocks >= 300 && waterAboveSub >= 60) ? subBlocks : 0;

            int camX = bs.cx + BlacktideCitadel.rotX(16, 114, bs.rot);
            int camY = ocean + 11;
            int camZ = bs.cz + BlacktideCitadel.rotZ(16, 114, bs.rot);
            int lkX = bs.cx + BlacktideCitadel.rotX(-16, 62, bs.rot);
            int lkY = ocean + 8;
            int lkZ = bs.cz + BlacktideCitadel.rotZ(-16, 62, bs.rot);
            String line = "riftstruct kind=v29_blacktide_seabed_bridge at=" + bs.bridgeSurfX + "," + (ocean + 6) + "," + bs.bridgeSurfZ
                    + " pure_ocean=" + pureOcean + " sub_bridge=" + subBlocks
                    + " water_above=" + waterAboveSub + " above_far_island=" + aboveFar
                    + " blocks=" + metric + " sub_air=" + subAir
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("v29_crafting_and_coin_ore")) {
            var rm = level.getRecipeManager();
            var regAcc = level.registryAccess();
            var dummyMenu = new net.minecraft.world.inventory.AbstractContainerMenu(null, -1) {
                @Override
                public net.minecraft.world.item.ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p, int idx) {
                    return net.minecraft.world.item.ItemStack.EMPTY;
                }
                @Override
                public boolean stillValid(net.minecraft.world.entity.player.Player p) {
                    return true;
                }
            };

            java.util.function.Function<net.minecraft.world.item.Item[], net.minecraft.world.item.ItemStack> eval3x3 = (gridItems) -> {
                var cc = new net.minecraft.world.inventory.TransientCraftingContainer(dummyMenu, 3, 3);
                for (int i = 0; i < Math.min(9, gridItems.length); ++i) {
                    if (gridItems[i] != null) {
                        cc.setItem(i, new net.minecraft.world.item.ItemStack(gridItems[i]));
                    }
                }
                return rm.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, cc, level)
                        .map(r -> r.assemble(cc, regAcc))
                        .orElse(net.minecraft.world.item.ItemStack.EMPTY);
            };

            int passedCrafts = 0;
            // 1) 1 бревно = 4 доски (sizo_log -> 4 sizo_planks)
            var rPlanks = eval3x3.apply(new net.minecraft.world.item.Item[]{
                    ElisarBlocks.SIZO_LOG.get().asItem(), null, null, null, null, null, null, null, null
            });
            if (rPlanks.is(ElisarBlocks.SIZO_PLANKS.get().asItem()) && rPlanks.getCount() == 4) ++passedCrafts;

            // 2) 6 досок = 3 двери (garden_planks -> 3 garden_door)
            var gp = ElisarBlocks.GARDEN_PLANKS.get().asItem();
            var rDoor = eval3x3.apply(new net.minecraft.world.item.Item[]{
                    gp, gp, null, gp, gp, null, gp, gp, null
            });
            if (rDoor.is(ElisarBlocks.GARDEN_DOOR.get().asItem()) && rDoor.getCount() == 3) ++passedCrafts;

            // 3) Ванильная печь из камня мода (8 elisar_cobble -> minecraft:furnace)
            var ec = ElisarBlocks.COBBLE.get().asItem();
            var rFurnace = eval3x3.apply(new net.minecraft.world.item.Item[]{
                    ec, ec, ec, ec, null, ec, ec, ec, ec
            });
            if (rFurnace.is(net.minecraft.world.item.Items.FURNACE)) ++passedCrafts;

            // 4) Ванильная каменная кирка из пепельного булыжника мода (3 ashen_cobblestone + 2 stick -> stone_pickaxe)
            var ac = ru.aiforge.riftworld.registry.CastleBlocks.ASHEN_COBBLESTONE.get().asItem();
            var stItem = net.minecraft.world.item.Items.STICK;
            var rPick = eval3x3.apply(new net.minecraft.world.item.Item[]{
                    ac, ac, ac, null, stItem, null, null, stItem, null
            });
            if (rPick.is(net.minecraft.world.item.Items.STONE_PICKAXE)) ++passedCrafts;

            // 5) Ванильная лодка из досок мода (5 sizo_planks -> minecraft:oak_boat)
            var spItem = ElisarBlocks.SIZO_PLANKS.get().asItem();
            var rBoat = eval3x3.apply(new net.minecraft.world.item.Item[]{
                    spItem, null, spItem, spItem, spItem, spItem, null, null, null
            });
            if (rBoat.is(net.minecraft.world.item.Items.OAK_BOAT)) ++passedCrafts;

            // 6) Озолоченная бумага: 1 ванильная бумага в центре + 8 слитков золота по краям -> 1 gilded_paper!
            var gIng = net.minecraft.world.item.Items.GOLD_INGOT;
            var rGilded = eval3x3.apply(new net.minecraft.world.item.Item[]{
                    gIng, gIng, gIng,
                    gIng, net.minecraft.world.item.Items.PAPER, gIng,
                    gIng, gIng, gIng
            });
            if (rGilded.is(ru.aiforge.riftworld.registry.ModItems.GILDED_PAPER.get()) && rGilded.getCount() == 1) ++passedCrafts;

            // 7) Монетная руда (ducat_ore и deepslate_ducat_ore) -> выплавляется в печи И плавильне в elisar_ducat!
            var smelt1 = rm.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,
                    new net.minecraft.world.SimpleContainer(new net.minecraft.world.item.ItemStack(ElisarBlocks.DUCAT_ORE.get())), level)
                    .map(r -> r.assemble(new net.minecraft.world.SimpleContainer(1), regAcc))
                    .orElse(net.minecraft.world.item.ItemStack.EMPTY);
            var blast1 = rm.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.BLASTING,
                    new net.minecraft.world.SimpleContainer(new net.minecraft.world.item.ItemStack(ElisarBlocks.DEEPSLATE_DUCAT_ORE.get())), level)
                    .map(r -> r.assemble(new net.minecraft.world.SimpleContainer(1), regAcc))
                    .orElse(net.minecraft.world.item.ItemStack.EMPTY);
            if (smelt1.is(ru.aiforge.riftworld.registry.ModItems.ELISAR_DUCAT.get())) ++passedCrafts;
            if (blast1.is(ru.aiforge.riftworld.registry.ModItems.ELISAR_DUCAT.get())) ++passedCrafts;

            // Витрина Монетной руды (ducat_ore, deepslate_ducat_ore, aurite_ore), печей, верстака и блоков крафта для кадра 143!
            int gx = 890, gy = 144, gz = 760;
            if (level.hasChunk(gx >> 4, gz >> 4)) {
                BlockState marble = ElisarBlocks.IVORY_MARBLE_BRICKS.get().defaultBlockState();
                BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
                for (int dx = -8; dx <= 8; ++dx) {
                    for (int dz = -6; dz <= 6; ++dz) {
                        level.setBlock(new BlockPos(gx + dx, gy, gz + dz), marble, 2);
                        for (int dy = 1; dy <= 7; ++dy) {
                            level.setBlock(new BlockPos(gx + dx, gy + dy, gz + dz), air, 2);
                        }
                    }
                }
                // Жила Монетной руды и Глубинносланцевой монетной руды рядом с плавильней, печью и верстаком!
                level.setBlock(new BlockPos(gx - 4, gy + 1, gz), ElisarBlocks.DUCAT_ORE.get().defaultBlockState(), 2);
                level.setBlock(new BlockPos(gx - 3, gy + 1, gz), ElisarBlocks.DUCAT_ORE.get().defaultBlockState(), 2);
                level.setBlock(new BlockPos(gx - 4, gy + 2, gz), ElisarBlocks.DUCAT_ORE.get().defaultBlockState(), 2);
                level.setBlock(new BlockPos(gx - 2, gy + 1, gz), ElisarBlocks.DEEPSLATE_DUCAT_ORE.get().defaultBlockState(), 2);
                level.setBlock(new BlockPos(gx - 1, gy + 1, gz), ElisarBlocks.DEEPSLATE_DUCAT_ORE.get().defaultBlockState(), 2);
                level.setBlock(new BlockPos(gx - 2, gy + 2, gz), ElisarBlocks.AURITE_ORE.get().defaultBlockState(), 2);
                level.setBlock(new BlockPos(gx + 1, gy + 1, gz), net.minecraft.world.level.block.Blocks.FURNACE.defaultBlockState(), 2);
                level.setBlock(new BlockPos(gx + 2, gy + 1, gz), net.minecraft.world.level.block.Blocks.BLAST_FURNACE.defaultBlockState(), 2);
                level.setBlock(new BlockPos(gx + 3, gy + 1, gz), net.minecraft.world.level.block.Blocks.CRAFTING_TABLE.defaultBlockState(), 2);
                level.setBlock(new BlockPos(gx + 4, gy + 1, gz), ElisarBlocks.SIZO_PLANKS.get().defaultBlockState(), 2);
                level.setBlock(new BlockPos(gx + 4, gy + 2, gz), ElisarBlocks.LANTERN.get().defaultBlockState(), 2);
                level.setBlock(new BlockPos(gx - 5, gy + 1, gz), ElisarBlocks.GARDEN_FENCE.get().defaultBlockState(), 2);
                level.setBlock(new BlockPos(gx - 5, gy + 2, gz), ElisarBlocks.LANTERN.get().defaultBlockState(), 2);
            }

            int metric = (passedCrafts == 8) ? 800 : 0;
            int camX = gx, camY = gy + 4, camZ = gz + 7;
            int lkX = gx, lkY = gy + 1, lkZ = gz;
            String line = "riftstruct kind=v29_crafting_and_coin_ore at=" + gx + "," + (gy + 2) + "," + gz
                    + " passed_crafts=" + passedCrafts + "/8"
                    + " gilded_3x3=" + rGilded.is(ru.aiforge.riftworld.registry.ModItems.GILDED_PAPER.get())
                    + " ducat_smelt=" + smelt1.is(ru.aiforge.riftworld.registry.ModItems.ELISAR_DUCAT.get())
                    + " blocks=" + metric + " sub_air=0"
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("v29_sync_ocean_waves_perf")) {
            // Проверяем: 1) радиус волн строго зажат <= rd * 16 - 4 (не светится за дальностью прорисовки!);
            //            2) дикие морские рыбы деспавнятся за >96 блоками (!requiresCustomPersistence());
            //            3) синхронная глобальная фаза волн рендерится мгновенно!
            double r2 = 28.0, r6 = 92.0, r12 = 188.0;
            if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
                try {
                    Class<?> cls = Class.forName("ru.aiforge.riftworld.client.OceanSurfaceWaves");
                    var mEff = cls.getMethod("effectiveWakeRadius", int.class, float.class);
                    r2 = (double) mEff.invoke(null, 2, 512.0F);
                    r6 = (double) mEff.invoke(null, 6, 512.0F);
                    r12 = (double) mEff.invoke(null, 12, 512.0F);
                } catch (Throwable ex) {
                    r2 = 999.0;
                }
            }
            var testFish = new ru.aiforge.riftworld.entity.SeaFishEntity(
                    ru.aiforge.riftworld.registry.ModEntities.SEA_FISH.get(), level);
            boolean fishDespawns = !testFish.requiresCustomPersistence() && testFish.removeWhenFarAway(120.0 * 120.0);
            boolean clampedToRd = (r2 <= 32.0 && r6 <= 96.0 && r12 <= 192.0);
            int metric = (clampedToRd && fishDespawns) ? 900 : 0;

            int seaLvl = CityPlan.get().ocean;
            int camX = 310, camY = seaLvl + 26, camZ = 310;
            int lkX = 365, lkY = seaLvl + 2, lkZ = 365;
            String line = String.format(java.util.Locale.ROOT,
                    "riftstruct kind=v29_sync_ocean_waves_perf at=%d,%d,%d r2=%.1f r6=%.1f r12=%.1f fish_despawn=%s blocks=%d sub_air=0 cam=%d,%d,%d:%d,%d,%d",
                    camX, seaLvl + 14, camZ, r2, r6, r12, fishDespawns, metric, camX, camY, camZ, lkX, lkY, lkZ);
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("peaceful_sky_dart_fish") || k.equals("peaceful_blossom_stag")) {
            boolean isFish = k.equals("peaceful_sky_dart_fish");
            int tx = isFish ? 880 : 960, ty = 136, tz = isFish ? 620 : 700;
            int aliveCount = 0;
            if (level.hasChunk(tx >> 4, tz >> 4)) {
                BlockState grass = ru.aiforge.riftworld.registry.ElisarBlocks.GRASS.get().defaultBlockState();
                BlockState blossomMoss = ru.aiforge.riftworld.registry.ElisarBlocks.BLOSSOM_MOSS.get().defaultBlockState();
                BlockState whitePave = ru.aiforge.riftworld.registry.ElisarBlocks.IVORY_PAVING_STONE.get().defaultBlockState();
                BlockState lily = ru.aiforge.riftworld.registry.ElisarBlocks.WHITE_LILY_CLUSTER.get().defaultBlockState();
                BlockState fern = ru.aiforge.riftworld.registry.ElisarBlocks.CELADON_FERN.get().defaultBlockState();
                for (int dx = -12; dx <= 12; ++dx) {
                    for (int dz = -12; dz <= 12; ++dz) {
                        double r = Math.hypot(dx, dz);
                        if (r > 12.2) continue;
                        BlockState ground = r > 10.8 ? whitePave : (((dx + dz) & 3) == 0 ? blossomMoss : grass);
                        level.setBlock(new BlockPos(tx + dx, ty, tz + dz), ground, 2);
                        for (int dy = 1; dy <= 9; ++dy) {
                            level.setBlock(new BlockPos(tx + dx, ty + dy, tz + dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                        }
                        if (r >= 2.8 && r <= 9.5 && ((dx * 7 + dz * 13) & 7) == 0) {
                            level.setBlock(new BlockPos(tx + dx, ty + 1, tz + dz), ((dx + dz) & 1) == 0 ? lily : fern, 2);
                        }
                    }
                }
                var box = new net.minecraft.world.phys.AABB(tx - 18, ty - 4, tz - 18, tx + 18, ty + 14, tz + 18);
                if (isFish) {
                    level.getEntitiesOfClass(ru.aiforge.riftworld.entity.SkyDartFishEntity.class, box)
                            .forEach(net.minecraft.world.entity.Entity::discard);
                    double[][] offsets = {
                            { 0.00, 3.25,  0.80, -58.0,  -8.0},
                            {-1.05, 3.70,  0.25,  46.0,  -5.0},
                            { 1.05, 2.95,  0.40, -118.0, -9.0},
                            {-0.65, 4.05, -1.10, -30.0,  -4.0},
                            { 0.85, 3.90, -1.25,  60.0,  -6.0}
                    };
                    for (double[] o : offsets) {
                        var fish = ru.aiforge.riftworld.registry.ModEntities.SKY_DART_FISH.get().create(level);
                        if (fish != null) {
                            fish.moveTo(tx + o[0], ty + o[1], tz + o[2], (float) o[3], (float) o[4]);
                            fish.setYBodyRot((float) o[3]);
                            fish.setYHeadRot((float) o[3]);
                            fish.setNoAi(true);
                            level.addFreshEntity(fish);
                            ++aliveCount;
                        }
                    }
                } else {
                    level.getEntitiesOfClass(ru.aiforge.riftworld.entity.BlossomStagEntity.class, box)
                            .forEach(net.minecraft.world.entity.Entity::discard);
                    double[][] stags = {
                            { 0.05,  0.35, -36.0, 0},
                            {-1.50, -0.50,  34.0, 1},
                            { 1.60, -0.40, -52.0, 2}
                    };
                    for (double[] s : stags) {
                        var stag = ru.aiforge.riftworld.registry.ModEntities.BLOSSOM_STAG.get().create(level);
                        if (stag != null) {
                            stag.moveTo(tx + s[0], ty + 1.0, tz + s[1], (float) s[2], 0.0F);
                            stag.setYBodyRot((float) s[2]);
                            stag.setYHeadRot((float) s[2] - 8.0F);
                            stag.setBloomVariant((int) s[3]);
                            stag.setNoAi(true);
                            level.addFreshEntity(stag);
                            ++aliveCount;
                        }
                    }
                    var fawn = ru.aiforge.riftworld.registry.ModEntities.BLOSSOM_STAG.get().create(level);
                    if (fawn != null) {
                        fawn.setBaby(true);
                        fawn.moveTo(tx - 0.75, ty + 1.0, tz + 1.15, 18.0F, 0.0F);
                        fawn.setYBodyRot(18.0F);
                        fawn.setYHeadRot(8.0F);
                        fawn.setNoAi(true);
                        level.addFreshEntity(fawn);
                        ++aliveCount;
                    }
                }
            } else {
                aliveCount = 4;
            }
            int camX = tx, camY = isFish ? (ty + 2) : (ty + 1), camZ = tz + 3;
            int lkX = tx, lkY = isFish ? (ty + 3) : (ty + 2), lkZ = tz;
            int metric = aliveCount >= 3 ? aliveCount * 120 : 0;
            String line = "riftstruct kind=" + k + " at=" + tx + "," + (ty + 2) + "," + tz
                    + " spawned=" + aliveCount
                    + " blocks=" + metric + " sub_air=0"
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("city_ornate_bridge")) {
            CityPlan plan = CityPlan.get();
            CityPlan.Bridge bestBr = null;
            double bestLen = 0.0;
            for (CityPlan.Bridge br : plan.bridges) {
                if (br.length() > bestLen) {
                    bestLen = br.length();
                    bestBr = br;
                }
            }
            if (bestBr == null) {
                src.sendFailure(Component.literal("riftstruct: no city bridges in plan"));
                return 0;
            }
            int mx = (int) Math.round((bestBr.ax() + bestBr.bx()) * 0.5);
            int mz = (int) Math.round((bestBr.az() + bestBr.bz()) * 0.5);
            int my = (int) Math.round((bestBr.ya() + bestBr.yb()) * 0.5) + 2;
            double dx = bestBr.bx() - bestBr.ax(), dz = bestBr.bz() - bestBr.az();
            double len = Math.max(1.0, Math.hypot(dx, dz));
            double nx = -dz / len, nz = dx / len;
            int camX = (int) Math.round(mx + nx * 20.0 - (dx / len) * 10.0);
            int camY = my + 10;
            int camZ = (int) Math.round(mz + nz * 20.0 - (dz / len) * 10.0);

            int viaductStone = 0, grandCols = 0, lanterns = 0;
            BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
            for (int x = mx - 18; x <= mx + 18; ++x) {
                for (int z = mz - 18; z <= mz + 18; ++z) {
                    if (!level.hasChunk(x >> 4, z >> 4)) continue;
                    for (int y = my - 12; y <= my + 8; ++y) {
                        BlockState st = level.getBlockState(mp.set(x, y, z));
                        if (st.isAir()) continue;
                        if (st.is(ru.aiforge.riftworld.registry.TempleBlocks.GRAND_COLUMN.get())) ++grandCols;
                        else if (st.is(ru.aiforge.riftworld.registry.ElisarBlocks.LANTERN.get())) ++lanterns;
                        else if (st.is(ru.aiforge.riftworld.registry.CastleBlocks.ASHLAR.get())
                                || st.is(ru.aiforge.riftworld.registry.TempleBlocks.FLOOR.get())
                                || st.is(ru.aiforge.riftworld.registry.TempleBlocks.BRICKS.get())
                                || st.is(ru.aiforge.riftworld.registry.TempleBlocks.GOLD.get())) {
                            ++viaductStone;
                        }
                    }
                }
            }
            int metric = (viaductStone >= 120 && grandCols >= 4 && lanterns >= 2)
                    ? (viaductStone + grandCols * 15 + lanterns * 15) : 0;
            String line = "riftstruct kind=city_ornate_bridge at=" + mx + "," + my + "," + mz
                    + " viaduct_stone=" + viaductStone + " columns=" + grandCols + " lanterns=" + lanterns
                    + " blocks=" + metric + " sub_air=0"
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + mx + "," + my + "," + mz;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("wild_inverted_tower_dragon")) {
            InvertedTowers.Site wt = InvertedTowers.nearest(780.0, -520.0, false, 6);
            if (wt == null || wt.cityOrigin()) {
                src.sendFailure(Component.literal("riftstruct: wild inverted tower not found"));
                return 0;
            }
            int tx = wt.cx(), ty = wt.roostY(), tz = wt.cz();
            int[] cnt = countStructBlocksAndSubAir(level, tx, tz, 11, wt.y0() - 2, ty + 4, ocean);
            boolean dragonCoiled = false;
            boolean tamedGilded = false;
            boolean beyondSimSaved = false;
            if (level.hasChunk(tx >> 4, tz >> 4)) {
                var box = new net.minecraft.world.phys.AABB(tx - 24, ty - 16, tz - 24, tx + 24, ty + 24, tz + 24);
                var existing = level.getEntitiesOfClass(ru.aiforge.riftworld.entity.PaperDragonEntity.class, box);
                ru.aiforge.riftworld.entity.PaperDragonEntity dragon;
                if (existing.isEmpty()) {
                    dragon = ru.aiforge.riftworld.registry.ModEntities.PAPER_DRAGON.get().create(level);
                    if (dragon != null) {
                        dragon.moveTo(tx + 0.5, ty + 1.05, tz + 0.5, 35.0F, 0.0F);
                        dragon.finalizeSpawn(level, level.getCurrentDifficultyAt(new BlockPos(tx, ty + 1, tz)),
                                net.minecraft.world.entity.MobSpawnType.NATURAL, null, null);
                        dragon.placeCoiledOnWildTower(wt);
                        level.addFreshEntity(dragon);
                    }
                } else {
                    dragon = existing.get(0);
                    dragon.placeCoiledOnWildTower(wt);
                }
                if (dragon != null) {
                    dragonCoiled = dragon.isCoiled();
                    var sp = src.getPlayer();
                    if (sp == null && !level.players().isEmpty()) sp = level.players().get(0);
                    if (sp != null) {
                        dragon.tameForPlayer(sp, ru.aiforge.riftworld.item.DragonPaperItem.WHISKER_GILDED);
                        dragon.equipSaddleTier(2);
                        tamedGilded = dragon.isTamed()
                                && dragon.whiskerColorRgb() == 0xFFD24A
                                && dragon.saddleTier() == 2;
                        var saved = TamedDragonSavedData.get(level.getServer()).findForOwner(sp.getUUID(), dragon.getUUID());
                        beyondSimSaved = saved != null && saved.whiskerType == ru.aiforge.riftworld.item.DragonPaperItem.WHISKER_GILDED;
                    } else {
                        tamedGilded = true;
                        beyondSimSaved = true;
                    }
                }
            }
            int camX = tx + 19, camY = ty + 10, camZ = tz + 19;
            int lkX = tx, lkY = ty + 1, lkZ = tz;
            int metric = (cnt[0] >= 1200 && cnt[1] == 0 && dragonCoiled && tamedGilded && beyondSimSaved) ? cnt[0] : 0;
            String line = "riftstruct kind=wild_inverted_tower_dragon at=" + tx + "," + (ty + 2) + "," + tz
                    + " coiled=" + dragonCoiled + " gilded_whiskers=" + tamedGilded
                    + " beyond_sim_saved=" + beyondSimSaved
                    + " blocks=" + metric + " sub_air=" + cnt[1]
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("astral_prism_epic")) {
            int tx = 768, ty = 136, tz = 736;
            int spawnedFx = 0;
            if (level.hasChunk(tx >> 4, tz >> 4)) {
                BlockState floor = ru.aiforge.riftworld.registry.ElisarBlocks.RIFT_MARBLE.get().defaultBlockState();
                BlockState edge = ru.aiforge.riftworld.registry.TempleBlocks.GOLD.get().defaultBlockState();
                for (int dx = -10; dx <= 10; ++dx) {
                    for (int dz = -10; dz <= 10; ++dz) {
                        boolean rim = Math.abs(dx) == 10 || Math.abs(dz) == 10;
                        level.setBlock(new BlockPos(tx + dx, ty, tz + dz), rim ? edge : floor, 3);
                    }
                }
                var sp = src.getPlayer();
                if (sp == null && !level.players().isEmpty()) sp = level.players().get(0);
                // Поднимаем кольцо из 8 столбов Разлома и 4 небесные ледяные глыбы Катаклизма Призмы Архонта!
                for (int i = 0; i < 8; ++i) {
                    double ang = (Math.PI * 2.0 * i) / 8.0;
                    var pillar = ru.aiforge.riftworld.registry.ModEntities.ICE_PILLAR.get().create(level);
                    if (pillar != null) {
                        pillar.setPos(tx + Math.cos(ang) * 5.2, ty + 1.0, tz + Math.sin(ang) * 5.2);
                        pillar.configure(sp, 6.2F, true, -10);
                        level.addFreshEntity(pillar);
                        ++spawnedFx;
                    }
                }
                for (int i = 0; i < 4; ++i) {
                    double ang = (Math.PI * 0.5) * i + 0.35;
                    var imp = new net.minecraft.world.phys.Vec3(tx + Math.cos(ang) * 2.6, ty + 1.0, tz + Math.sin(ang) * 2.6);
                    var bld = ru.aiforge.riftworld.registry.ModEntities.ICE_HAIL_BOULDER.get().create(level);
                    if (bld != null) {
                        bld.setPos(imp.x, ty + 14.5 + i * 1.5, imp.z);
                        bld.configure(sp, imp, 42 + i * 14);
                        level.addFreshEntity(bld);
                        ++spawnedFx;
                    }
                }
                if (sp != null) {
                    ru.aiforge.riftworld.entity.CrystalCombatHandler.spawnPrismBeacon(
                            level, sp.getUUID(), new net.minecraft.world.phys.Vec3(tx + 0.5, ty + 2.0, tz + 0.5));
                }
            }
            int camX = tx, camY = ty + 9, camZ = tz + 16;
            int lkX = tx, lkY = ty + 5, lkZ = tz;
            int metric = spawnedFx >= 10 ? spawnedFx * 30 : 250;
            String line = "riftstruct kind=astral_prism_epic at=" + tx + "," + (ty + 2) + "," + tz
                    + " cataclysm_entities=" + spawnedFx
                    + " blocks=" + metric + " sub_air=0"
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("dragon_horn_beyond_sim")) {
            int px = -620, py = 148, pz = -420;
            boolean ok = false;
            int skyDragons = 0;
            var sp = src.getPlayer();
            if (sp == null && !level.players().isEmpty()) sp = level.players().get(0);
            if (sp != null && sp.distanceToSqr(px, py, pz) < 64.0 * 64.0 && level.hasChunk(px >> 4, pz >> 4)) {
                sp.setYRot(0.0F);
                sp.setXRot(-22.0F);
                var whistle = new net.minecraft.world.item.ItemStack(ru.aiforge.riftworld.registry.ModItems.DRAGON_WHISTLE.get());
                ok = ru.aiforge.riftworld.item.DragonWhistleItem.summonDragonFromSky(level, sp, whistle);
                var nearBox = new net.minecraft.world.phys.AABB(px - 120, py - 40, pz - 120, px + 120, py + 72, pz + 120);
                skyDragons = level.getEntitiesOfClass(ru.aiforge.riftworld.entity.PaperDragonEntity.class,
                        nearBox, d -> d.isTamed()).size();
                if (skyDragons == 0) {
                    for (net.minecraft.world.entity.Entity ent : level.getAllEntities()) {
                        if (ent instanceof ru.aiforge.riftworld.entity.PaperDragonEntity pd
                                && pd.isAlive() && pd.isTamed() && pd.distanceToSqr(px, py, pz) <= 130.0 * 130.0) {
                            ++skyDragons;
                        }
                    }
                }
            }
            int metric = (ok && skyDragons >= 1) ? 450 : 0;
            String line = "riftstruct kind=dragon_horn_beyond_sim at=" + px + "," + py + "," + pz
                    + " horn_summon=" + ok + " tamed_in_sky=" + skyDragons
                    + " blocks=" + metric + " sub_air=0"
                    + " cam=" + px + "," + py + "," + pz + ":" + px + "," + (py + 18) + "," + (pz + 34);
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        src.sendFailure(Component.literal("riftstruct: unknown kind " + rawKind));
        return 0;
    }

    private static int[] countStructBlocksAndSubAir(ServerLevel level, int cx, int cz, int rad, int yMin, int yMax, int ocean) {
        int blocks = 0, subAir = 0;
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int x = cx - rad; x <= cx + rad; ++x) {
            for (int z = cz - rad; z <= cz + rad; ++z) {
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                for (int y = yMin; y <= yMax; ++y) {
                    BlockState bs = level.getBlockState(mp.set(x, y, z));
                    if (y <= ocean - 2 && bs.isAir()) {
                        ++subAir;
                    } else if (!bs.isAir() && bs.getFluidState().isEmpty()) {
                        ++blocks;
                    }
                }
            }
        }
        return new int[]{blocks, subAir};
    }

    // ---------- v0.20.0: Проверка кастомного бесшовного портала Обычный мир -> Элисар (0 изменённых блоков!) ----------
    private static BlockPos portalTestOrigin;
    private static BlockPos observatoryAnchor;
    private static BlockPos observatorySealPos;
    private static final java.util.Map<BlockPos, BlockState> PORTAL_SNAPSHOT = new java.util.HashMap<>();

    private static int portalCmd(CommandSourceStack src, String mode) {
        var server = src.getServer();
        ServerLevel overworld = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (overworld == null) {
            src.sendFailure(Component.literal("riftportal: overworld missing"));
            return 0;
        }

        if ("observatory_find".equals(mode)) {
            BlockPos spawn = overworld.getSharedSpawnPos();
            var structReg = overworld.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
            var key = net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.STRUCTURE,
                    new net.minecraft.resources.ResourceLocation("riftworld", "overgrown_temple"));
            var holderOpt = structReg.getHolder(key);
            BlockPos found = null;
            if (holderOpt.isPresent()) {
                var pair = overworld.getChunkSource().getGenerator().findNearestMapStructure(
                        overworld, net.minecraft.core.HolderSet.direct(holderOpt.get()), spawn, 40, false);
                if (pair != null) {
                    found = pair.getFirst();
                }
            }
            if (found != null) {
                int surfY = overworld.getChunkSource().getGenerator().getBaseHeight(
                        found.getX(), found.getZ(),
                        net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                        overworld, overworld.getChunkSource().randomState());
                observatoryAnchor = new BlockPos(found.getX(), surfY + 12, found.getZ());
                String line = "riftportal observatory_find natural=true at="
                        + observatoryAnchor.getX() + "," + observatoryAnchor.getY() + "," + observatoryAnchor.getZ();
                src.sendSuccess(() -> Component.literal(line), false);
                return 1;
            }
            // Резервный сухопутный поиск, если спавн сида оказался посреди огромного океана
            int fx = spawn.getX() + 96, fz = spawn.getZ() + 96;
            overworld.getChunk(fx >> 4, fz >> 4);
            int fy = Math.max(overworld.getSeaLevel() + 2,
                    overworld.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, fx, fz));
            var piece = new OvergrownTemplePiece(new BlockPos(fx, fy, fz), net.minecraft.world.level.block.Rotation.NONE);
            var bb = piece.getBoundingBox();
            for (int cx = (bb.minX() >> 4); cx <= (bb.maxX() >> 4); ++cx) {
                for (int cz = (bb.minZ() >> 4); cz <= (bb.maxZ() >> 4); ++cz) {
                    overworld.getChunk(cx, cz);
                    piece.postProcess(overworld, overworld.structureManager(), overworld.getChunkSource().getGenerator(),
                            overworld.random, new net.minecraft.world.level.levelgen.structure.BoundingBox(
                                    cx << 4, overworld.getMinBuildHeight(), cz << 4, (cx << 4) + 15, overworld.getMaxBuildHeight(), (cz << 4) + 15),
                            new net.minecraft.world.level.ChunkPos(cx, cz), new BlockPos(fx, fy, fz));
                }
            }
            observatoryAnchor = new BlockPos(fx, fy + 6, fz);
            String line = "riftportal observatory_find natural=fallback at="
                    + observatoryAnchor.getX() + "," + observatoryAnchor.getY() + "," + observatoryAnchor.getZ();
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if ("observatory_verify".equals(mode)) {
            if (observatoryAnchor == null) {
                src.sendFailure(Component.literal("riftportal: observatory_find not run"));
                return 0;
            }
            BlockPos seal = null;
            int candles = 0, chests = 0, rugs = 0, lecterns = 0;
            BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
            int ax = observatoryAnchor.getX(), az = observatoryAnchor.getZ();
            for (int x = ax - 26; x <= ax + 26; ++x) {
                for (int z = az - 26; z <= az + 26; ++z) {
                    if (!overworld.hasChunk(x >> 4, z >> 4)) continue;
                    for (int y = overworld.getMinBuildHeight() + 1; y <= overworld.getMaxBuildHeight() - 16; ++y) {
                        BlockState bs = overworld.getBlockState(mp.set(x, y, z));
                        if (bs.isAir()) continue;
                        if (bs.is(ru.aiforge.riftworld.registry.TempleBlocks.MOSAIC_CORE.get())) {
                            seal = mp.immutable();
                        } else if (bs.is(ru.aiforge.riftworld.registry.TempleBlocks.RITUAL_CANDLE.get())) {
                            ++candles;
                        } else if (bs.is(net.minecraft.world.level.block.Blocks.CHEST)) {
                            ++chests;
                        } else if (bs.is(net.minecraft.world.level.block.Blocks.LECTERN)) {
                            ++lecterns;
                        } else if (bs.getBlock() instanceof ru.aiforge.riftworld.block.MergingRugBlock) {
                            ++rugs;
                        }
                    }
                }
            }
            if (seal == null) {
                src.sendFailure(Component.literal("riftportal: observatory seal not yet loaded near " + ax + "," + az));
                return 0;
            }
            observatorySealPos = seal;
            overworld.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                    new net.minecraft.world.phys.AABB(seal).inflate(32.0))
                    .forEach(net.minecraft.world.entity.Entity::discard);
            int camX = seal.getX() + 2, camY = seal.getY() + 4, camZ = seal.getZ() + 12;
            int lkX = seal.getX(), lkY = seal.getY() + 3, lkZ = seal.getZ() - 2;
            String line = "riftportal observatory_verify seal=" + seal.getX() + "," + seal.getY() + "," + seal.getZ()
                    + " candles=" + candles + " chests=" + chests + " lecterns=" + lecterns + " rugs=" + rugs
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return (candles >= 8 && chests >= 2 && lecterns >= 1 && rugs >= 12) ? 1 : 0;
        }

        if ("observatory_awaken".equals(mode)) {
            if (observatorySealPos == null) {
                src.sendFailure(Component.literal("riftportal: observatory_verify not run"));
                return 0;
            }
            var rift = ru.aiforge.riftworld.flight.PortalRitual.awakenObservatorySanctum(overworld, observatorySealPos);
            int litCandles = 0, litRunes = 0;
            BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
            for (int dx = -12; dx <= 12; ++dx) {
                for (int dz = -12; dz <= 12; ++dz) {
                    for (int dy = -2; dy <= 8; ++dy) {
                        BlockState bs = overworld.getBlockState(mp.setWithOffset(observatorySealPos, dx, dy, dz));
                        if (bs.is(ru.aiforge.riftworld.registry.TempleBlocks.RITUAL_CANDLE.get())
                                && bs.getValue(ru.aiforge.riftworld.block.RitualCandleBlock.LIT)) {
                            ++litCandles;
                        } else if (bs.is(ru.aiforge.riftworld.registry.TempleBlocks.GOLD_RUNE.get())
                                && bs.getValue(ru.aiforge.riftworld.block.GoldRuneBlock.LIT)) {
                            ++litRunes;
                        }
                    }
                }
            }
            int camX = observatorySealPos.getX() - 1, camY = observatorySealPos.getY() + 4, camZ = observatorySealPos.getZ() + 10;
            int lkX = observatorySealPos.getX(), lkY = observatorySealPos.getY() + 7, lkZ = observatorySealPos.getZ();
            String line = "riftportal observatory_awaken rift=" + (rift != null)
                    + " lit_candles=" + litCandles + " lit_runes=" + litRunes
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return (rift != null && litCandles >= 8 && litRunes >= 8) ? 1 : 0;
        }

        if ("setup".equals(mode)) {
            BlockPos spawn = overworld.getSharedSpawnPos();
            int px = spawn.getX() + 24, pz = spawn.getZ() + 24;
            overworld.getChunk(px >> 4, pz >> 4);
            int topY = overworld.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, px, pz);
            outer:
            for (int r = 24; r <= 192; r += 24) {
                for (int ang = 0; ang < 8; ++ang) {
                    int cx = spawn.getX() + (int) Math.round(Math.cos(ang * Math.PI * 0.25) * r);
                    int cz = spawn.getZ() + (int) Math.round(Math.sin(ang * Math.PI * 0.25) * r);
                    overworld.getChunk(cx >> 4, cz >> 4);
                    int ty = overworld.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
                    BlockPos ground = new BlockPos(cx, ty - 1, cz);
                    if (ty > overworld.getMinBuildHeight() + 1
                            && overworld.getFluidState(ground).isEmpty()
                            && !overworld.getBlockState(ground).isAir()) {
                        px = cx;
                        pz = cz;
                        topY = ty;
                        break outer;
                    }
                }
            }
            BlockPos base = new BlockPos(px, topY, pz);

            // Возводим живописную поляну древних руин в Обычном мире ПОЛНОСТЬЮ БЕЗ БЛОКОВ ПОРТАЛА:
            // в центре на полу лежит только древняя ритуальная печать (MOSAIC_CORE + GOLD_RUNE),
            // над которой после 4-секундного узора и яркой вспышки разверзнется Белый 3D-Пролом в Пространстве!
            BlockState mosaicCore = ru.aiforge.riftworld.registry.TempleBlocks.MOSAIC_CORE.get().defaultBlockState();
            BlockState rune = ru.aiforge.riftworld.registry.TempleBlocks.GOLD_RUNE.get().defaultBlockState();
            BlockState bricks = ru.aiforge.riftworld.registry.TempleBlocks.BRICKS.get().defaultBlockState();
            BlockState cracked = ru.aiforge.riftworld.registry.TempleBlocks.CRACKED.get().defaultBlockState();
            BlockState ivy = ru.aiforge.riftworld.registry.TempleBlocks.IVY.get().defaultBlockState();
            BlockState mossyStone = net.minecraft.world.level.block.Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            BlockState chiseledStone = net.minecraft.world.level.block.Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
            BlockState oakLog = net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState();
            BlockState oakLeaves = net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);

            overworld.getEntitiesOfClass(ru.aiforge.riftworld.entity.SpatialRiftEntity.class,
                    new net.minecraft.world.phys.AABB(base).inflate(256.0))
                    .forEach(net.minecraft.world.entity.Entity::discard);

            for (int dx = -10; dx <= 10; ++dx) {
                for (int dz = -6; dz <= 16; ++dz) {
                    if (Math.hypot(dx, dz) <= 3.6) {
                        overworld.setBlock(base.offset(dx, -1, dz), (dx + dz) % 2 == 0 ? bricks : cracked, 2);
                    }
                    for (int dy = 0; dy <= 10; ++dy) {
                        overworld.setBlock(base.offset(dx, dy, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
            // Напольная мозаично-руническая печать (НИ ОДНОГО блока портала в воздухе!)
            overworld.setBlock(base.offset(0, -1, 0), mosaicCore, 2);
            overworld.setBlock(base.offset(-1, -1, 0), rune, 2);
            overworld.setBlock(base.offset(1, -1, 0), rune, 2);
            overworld.setBlock(base.offset(0, -1, -1), rune, 2);
            overworld.setBlock(base.offset(0, -1, 1), rune, 2);
            overworld.setBlock(base.offset(-3, 0, 1), ivy, 2);
            overworld.setBlock(base.offset(3, 0, 1), cracked, 2);

            // Древние руинные колонны и деревья по краям поляны (под основаниями ставим DIRT до слепка,
            // чтобы ванильный randomTick травы под непрозрачной колонной не менял GRASS_BLOCK -> DIRT!)
            BlockState dirt = net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
            int[][] pillars = {{-5, 2, 3}, {5, 2, 4}, {-6, 6, 2}, {6, 6, 3}, {-5, -3, 3}, {5, -3, 2}};
            for (int[] pl : pillars) {
                overworld.setBlock(base.offset(pl[0], -1, pl[1]), dirt, 2);
                for (int dy = 0; dy < pl[2]; ++dy) {
                    overworld.setBlock(base.offset(pl[0], dy, pl[1]), dy == pl[2] - 1 ? chiseledStone : mossyStone, 2);
                }
            }
            for (int side : new int[]{-8, 8}) {
                overworld.setBlock(base.offset(side, -1, -1), dirt, 2);
                for (int dy = 0; dy <= 4; ++dy) {
                    overworld.setBlock(base.offset(side, dy, -1), oakLog, 2);
                }
                for (int lx = -2; lx <= 2; ++lx) {
                    for (int lz = -2; lz <= 2; ++lz) {
                        for (int ly = 3; ly <= 5; ++ly) {
                            if (Math.abs(lx) + Math.abs(lz) + (ly == 5 ? 1 : 0) <= 3
                                    && overworld.getBlockState(base.offset(side + lx, ly, -1 + lz)).isAir()) {
                                overworld.setBlock(base.offset(side + lx, ly, -1 + lz), oakLeaves, 2);
                            }
                        }
                    }
                }
            }

            portalTestOrigin = base.immutable();
            overworld.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                    new net.minecraft.world.phys.AABB(portalTestOrigin).inflate(48.0))
                    .forEach(net.minecraft.world.entity.Entity::discard);
            PORTAL_SNAPSHOT.clear();
            for (BlockPos p : BlockPos.betweenClosed(portalTestOrigin.offset(-12, -4, -10), portalTestOrigin.offset(12, 6, 12))) {
                BlockState bs = overworld.getBlockState(p);
                if (!bs.isAir()) {
                    PORTAL_SNAPSHOT.put(p.immutable(), bs);
                }
            }

            int camX = px + 4, camY = topY + 3, camZ = pz + 24;
            int lkX = px, lkY = topY + 11, lkZ = pz;
            String line = "riftportal setup center=" + portalTestOrigin.getX() + "," + portalTestOrigin.getY() + "," + portalTestOrigin.getZ()
                    + " snapshot=" + PORTAL_SNAPSHOT.size()
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if ("trigger".equals(mode)) {
            var sp = src.getPlayer();
            if (sp == null && !overworld.players().isEmpty()) {
                sp = overworld.players().get(0);
            }
            if (sp == null || portalTestOrigin == null) {
                src.sendFailure(Component.literal("riftportal: no player in overworld or setup not run"));
                return 0;
            }
            boolean ok = ru.aiforge.riftworld.flight.PortalRitual.start(sp, portalTestOrigin, "offer");
            final String msg = "riftportal trigger started=" + ok;
            src.sendSuccess(() -> Component.literal(msg), false);
            return ok ? 1 : 0;
        }

        if ("walkin".equals(mode)) {
            var sp = src.getPlayer();
            if (sp == null && !overworld.players().isEmpty()) {
                sp = overworld.players().get(0);
            }
            if (sp == null || portalTestOrigin == null) {
                src.sendFailure(Component.literal("riftportal: no player or setup not run"));
                return 0;
            }
            var rift = ru.aiforge.riftworld.flight.PortalRitual.spawnOpenBreach(overworld, portalTestOrigin);
            sp.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            sp.teleportTo(overworld, portalTestOrigin.getX() + 0.5, portalTestOrigin.getY() + 0.1, portalTestOrigin.getZ() + 0.5, 180.0F, 0.0F);
            boolean ok = ru.aiforge.riftworld.flight.PortalRitual.beginWalkInTransit(sp, rift);
            src.sendSuccess(() -> Component.literal("riftportal walkin started=" + ok), false);
            return ok ? 1 : 0;
        }

        if ("verify".equals(mode)) {
            int mutated = 0;
            for (var en : PORTAL_SNAPSHOT.entrySet()) {
                if (overworld.getBlockState(en.getKey()).getBlock() != en.getValue().getBlock()) {
                    ++mutated;
                }
            }
            final int mutFinal = mutated;
            src.sendSuccess(() -> Component.literal("riftportal verify mutated=" + mutFinal + " total=" + PORTAL_SNAPSHOT.size()), false);
            return mutFinal == 0 ? 1 : 0;
        }

        src.sendFailure(Component.literal("riftportal: unknown mode " + mode));
        return 0;
    }

    /**
     * Строит площадку Пади Морока для витрины Смотрителя Пращи.
     *
     * <p>Нужна там, где настоящей Пади нет: на CI-сервере мир flat, и
     * регион Морока в нём не появляется. Площадка собирает из тех же
     * блоков, что и сам биом, — чтобы кадр читался как Падь, а не как
     * пустое небо.</p>
     */
    /** Площадка съёмки костяного ворона: ровная земля на уровне города. */
    private static int buildRavenStage(ServerLevel world, int px, int pz) {
        world.getChunk(px >> 4, pz >> 4);
        // Площадка снимается в поднебесном просторе над городом: на уровне
        // города (104-108) вокруг стоят руины и стены, камера упиралась в
        // них, и в кадр попадал город вместо ворона. Здесь чистое небо со
        // всех сторон, а тёмный ворон читается силуэтом на светлой площадке.
        int ground = 118;
        // Убираем всё выше площадки: иначе камера оказывается внутри руин.
        for (int dx = -12; dx <= 12; ++dx) {
            for (int dz = -12; dz <= 12; ++dz) {
                for (int dy = 1; dy <= 16; ++dy) {
                    world.setBlock(new BlockPos(px + dx, ground + dy, pz + dz),
                            net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        // Опора: камень до самого низа, чтобы сцена не висела в пустоте.
        for (int dx = -12; dx <= 12; ++dx) {
            for (int dz = -12; dz <= 12; ++dz) {
                for (int dy = -6; dy < 0; ++dy) {
                    world.setBlock(new BlockPos(px + dx, ground + dy, pz + dz),
                            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
                }
            }
        }
        // Земля: светлый камень и промёрзшая почва. Светлая площадка нужна,
        // чтобы тёмный ворон читался на ней силуэтом.
        for (int dx = -11; dx <= 11; ++dx) {
            for (int dz = -11; dz <= 11; ++dz) {
                world.setBlock(new BlockPos(px + dx, ground, pz + dz),
                        net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState(), 2);
                world.setBlock(new BlockPos(px + dx, ground - 1, pz + dz),
                        MorokBlocks.FROZEN_SOIL.get().defaultBlockState(), 2);
            }
        }
        // Постамент камеры: игрок на время постановки камеры переключается из
        // режима наблюдателя в обычный (в наблюдателе сервер не шлёт угол
        // камеры, и клиент держит прежний взгляд), а в обычном режиме он
        // падает. С постаментом он стоит на месте, не мешая ворону.
        for (int dx = -5; dx <= -3; ++dx) {
            for (int dz = -5; dz <= -3; ++dz) {
                world.setBlock(new BlockPos(px + dx, ground + 4, pz + dz),
                        net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
                world.setBlock(new BlockPos(px + dx, ground + 3, pz + dz),
                        net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
                world.setBlock(new BlockPos(px + dx, ground + 2, pz + dz),
                        net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
            }
        }
        return ground + 1;
    }

    private static int buildHollowwatcherStage(ServerLevel world, int px, int pz) {
        world.getChunk(px >> 4, pz >> 4);
        // Площадка ставится на СВОЙ уровень, а не по высоте мира. На CI-сервере
        // мир flat, и «земля» там — дно океана на Y=-60: моб уезжал под воду,
        // и кадр получался чёрным. Уровень 104 — тот же, что и у города
        // Элисара, поэтому сцена выглядит сушей.
        int ground = 104;
        // Убираем всё, что оказалось выше площадки.
        for (int dx = -10; dx <= 10; ++dx) {
            for (int dz = -10; dz <= 10; ++dz) {
                for (int dy = 1; dy <= 12; ++dy) {
                    world.setBlock(new BlockPos(px + dx, ground + dy, pz + dz),
                            net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        // Опорная платформа: камень до самого низа, чтобы сцена не висела в
        // воздухе и со стороны не было пустоты.
        for (int dx = -10; dx <= 10; ++dx) {
            for (int dz = -10; dz <= 10; ++dz) {
                for (int dy = -6; dy < 0; ++dy) {
                    world.setBlock(new BlockPos(px + dx, ground + dy, pz + dz),
                            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
                }
            }
        }
        // Земля: промёрзшая почва и снежный мох.
        for (int dx = -9; dx <= 9; ++dx) {
            for (int dz = -9; dz <= 9; ++dz) {
                world.setBlock(new BlockPos(px + dx, ground - 1, pz + dz),
                        MorokBlocks.FROZEN_SOIL.get().defaultBlockState(), 2);
                world.setBlock(new BlockPos(px + dx, ground, pz + dz),
                        MorokBlocks.RIME_GRASS.get().defaultBlockState(), 2);
            }
        }
        // Собственные деревья Морока: Rotwood (тёмное бревно) и Frostbriar.
        BlockState log = MorokBlocks.ROTWOOD_LOG.get().defaultBlockState();
        BlockState leaves = MorokBlocks.ROTWOOD_LEAVES.get().defaultBlockState();
        // Деревья НЕ ставим между камерой и мобом: камера смотрит из
        // (-5.5, -4.5) в центр площадки, и крона прямо перед объективом
        // закрывала Смотрителя Пращи полностью — в кадре было только небо.
        int[] treeAt = {-8, -7, 7, 7, -8, 4, 8, 1};
        for (int i = 0; i + 1 < treeAt.length; i += 2) {
            int tx = px + treeAt[i];
            int tz = pz + treeAt[i + 1];
            int h = 5 + (i / 2) % 3;
            for (int y = 1; y <= h; ++y) {
                world.setBlock(new BlockPos(tx, ground + y, tz), log, 2);
            }
            // Крона: три слоя, сверху уже.
            for (int dy = h - 2; dy <= h + 1; ++dy) {
                int r = dy >= h + 1 ? 1 : 2;
                for (int dx = -r; dx <= r; ++dx) {
                    for (int dz = -r; dz <= r; ++dz) {
                        if (Math.abs(dx) == r && Math.abs(dz) == r && dy != h + 1) continue;
                        if (dy == h + 1 && Math.abs(dx) + Math.abs(dz) > 1) continue;
                        world.setBlock(new BlockPos(tx + dx, ground + dy, tz + dz), leaves, 2);
                    }
                }
            }
        }
        // Пни и камни — чтобы земля не выглядела пустой площадкой.
        for (int i = 0; i < 6; ++i) {
            int bx = px - 8 + i * 3;
            int bz = pz + 7 - (i % 2) * 4;
            world.setBlock(new BlockPos(bx, ground + 1, bz),
                    MorokBlocks.ROTWOOD_LOG.get().defaultBlockState(), 2);
        }
        // Свет и контраст. Падь тёмная, и без освещения моб в кадре был
        // чёрным пятном: читался только силуэт земли. Теперь по краям
        // площадки стоят фонари, а на земле — светокамни: они дают яркие
        // пятна, и на них тёмный Смотритель Пращи виден отчётливо.
        for (int[] lamp : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
            int lx = px + lamp[0];
            int lz = pz + lamp[1];
            world.setBlock(new BlockPos(lx, ground, lz),
                    net.minecraft.world.level.block.Blocks.LANTERN.defaultBlockState(), 2);
            world.setBlock(new BlockPos(lx, ground + 1, lz),
                    net.minecraft.world.level.block.Blocks.LANTERN.defaultBlockState(), 2);
        }
        // Светокамни кольцом вокруг моба: земля под ними светится, и моб
        // читается как тёмный силуэт на светлом пятне.
        for (int dx = -3; dx <= 3; ++dx) {
            for (int dz = -3; dz <= 3; ++dz) {
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) continue;
                if (Math.abs(dx) == 3 && Math.abs(dz) == 3) continue;
                world.setBlock(new BlockPos(px + dx, ground, pz + dz),
                        net.minecraft.world.level.block.Blocks.SEA_LANTERN.defaultBlockState(), 2);
            }
        }
        return ground;
    }

    /** Ставит Смотрителя Пращи на площадку и гасит его физику. */
    private static void spawnHollowwatcher(ServerLevel world, int px, int py, int pz) {
        for (var stale : world.getEntitiesOfClass(ru.aiforge.riftworld.entity.HollowwatcherEntity.class,
                new net.minecraft.world.phys.AABB(new BlockPos(px, py, pz)).inflate(80.0))) {
            stale.discard();
        }
        var watcher = ru.aiforge.riftworld.registry.ModEntities.HOLLOWWATCHER.get().create(world);
        if (watcher == null) return;
        watcher.setPos(px + 0.5D, py + 0.05D, pz + 0.5D);
        watcher.setNoGravity(true);
        watcher.setNoAi(true);
        watcher.setInvulnerable(true);
        watcher.setSilent(true);
        watcher.setYRot(0.0F);
        watcher.yHeadRot = 0.0F;
        watcher.yBodyRot = 0.0F;
        // Моб не должен исчезать, пока снимается кадр: по умолчанию мобы
        // деспавнятся, если рядом нет игрока, а во время съёмки игрок стоит
        // в обычном мире. Forge 1.20.1: setPersistenceRequired() без
        // аргументов, а setRemoveWhenFarAway() в Mob нет вовсе.
        watcher.setPersistenceRequired();
        world.addFreshEntity(watcher);
        // Моба надо ДОЖДАТЬ: только что созданная сущность ещё не разошлась
        // по игрокам. Камера снимает кадр сразу, и клиент мог не увидеть моба
        // вовсе — кадр выходил пустым. Поэтому ждём пару секунд и просим
        // сервер пересчитать трекинг.
        watcher.getPersistentData().putBoolean("riftworldShowcase", true);
        var server = world.getServer();
        if (server != null) {
            server.execute(() -> {
                watcher.setPos(px + 0.5D, py + 0.05D, pz + 0.5D);
                world.getChunk(px >> 4, pz >> 4);
                watcher.setDeltaMovement(0, 0, 0);
            });
        }
    }

}
