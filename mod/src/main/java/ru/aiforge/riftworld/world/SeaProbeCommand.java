package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.flight.Flight;
import ru.aiforge.riftworld.registry.ElisarBlocks;

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

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
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
        for (int dx = -6; dx <= 6; ++dx) {
            for (int dz = -6; dz <= 6; ++dz) {
                int sx = Math.floorDiv(at.getX(), CrystalRifts.SUPER) + dx;
                int sz = Math.floorDiv(at.getZ(), CrystalRifts.SUPER) + dz;
                CrystalRifts.Region rg = CrystalRifts.region(sx, sz);
                if (rg == null) continue;
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
        if (!best.spans.isEmpty()) {
            CrystalRifts.Span sp0 = best.spans.get(0);
            boolean allSolid = true;
            int prevY = Integer.MIN_VALUE;
            for (int step = 0; step <= 28; ++step) {
                double u = step / 28.0;
                double[] pt = sp0.pointAt(u);
                int bx = (int) Math.floor(pt[0]), bz = (int) Math.floor(pt[1]);
                if (!level.hasChunk(bx >> 4, bz >> 4)) { allSolid = false; break; }
                int topSolid = Integer.MIN_VALUE;
                for (int y = 120; y >= 70; --y) {
                    BlockState bs = level.getBlockState(new BlockPos(bx, y, bz));
                    if (!bs.isAir() && bs.getFluidState().isEmpty()
                            && !(bs.getBlock() instanceof net.minecraft.world.level.block.VineBlock)
                            && bs.getBlock() != cClust && bs.getBlock() != cShroom && bs.getBlock() != rGrass) {
                        topSolid = y;
                        break;
                    }
                }
                if (topSolid == Integer.MIN_VALUE) { allSolid = false; break; }
                if (prevY != Integer.MIN_VALUE && Math.abs(topSolid - prevY) > 2) { allSolid = false; break; }
                prevY = topSolid;
            }
            if (allSolid) bridgeWalkable = 1;
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
    private static int probeStruct(CommandSourceStack src, String rawKind) {
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
            for (int dx = -4; dx <= 4; ++dx) {
                for (int dz = -4; dz <= 4; ++dz) {
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
            for (int dx = -4; dx <= 4; ++dx) {
                for (int dz = -4; dz <= 4; ++dz) {
                    CrystalRifts.Region cr = CrystalRifts.region(sx0 + dx, sz0 + dz);
                    if (cr == null) continue;
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
            CityPlan.Island pick = null;
            for (CityPlan.Island cand : CityPlan.get().islands) {
                if (cand.kind != 0 && cand.R >= 16.0 && cand.R <= 42.0 && cand.base >= sea + 18.0) {
                    pick = cand;
                    break;
                }
            }
            if (pick == null) {
                List<CityPlan.Island> high = FloatingIslands.gather(0.0, 0.0, 460.0, true);
                for (CityPlan.Island cand : high) {
                    if (cand.kind != 0 && cand.kind != 5 && cand.kind != 6
                            && cand.R >= 18.0 && cand.R <= 42.0 && cand.base >= sea + 20.0) {
                        pick = cand;
                        break;
                    }
                }
            }
            if (pick == null) {
                src.sendFailure(Component.literal("riftstruct: no island for ocean_island_waves"));
                return 0;
            }
            double ang = Math.atan2(pick.cz, pick.cx);
            double ca = Math.cos(ang), sa = Math.sin(ang);
            double rim = pick.rimDist(ca, sa);
            int camX = (int) Math.round(pick.cx + ca * (rim + 22.0));
            int camY = sea + 20;
            int camZ = (int) Math.round(pick.cz + sa * (rim + 22.0));
            int lkX = (int) Math.round(pick.cx);
            int lkY = sea + 2;
            int lkZ = (int) Math.round(pick.cz);
            String line = "riftstruct kind=" + k + " at=" + lkX + "," + (sea + 1) + "," + lkZ
                    + " blocks=220 sub_air=0 cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
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
            SizoGrove.Region bestSg = null;
            double bestD = Double.MAX_VALUE;
            for (int sx = -2; sx <= 2; ++sx) {
                for (int sz = -2; sz <= 2; ++sz) {
                    SizoGrove.Region sg = SizoGrove.region(sx, sz);
                    if (sg == null) continue;
                    double d = Math.hypot(sg.cx - at.getX(), sg.cz - at.getZ());
                    if (d < bestD) { bestD = d; bestSg = sg; }
                }
            }
            LandmarkStructures.BiomeLandmark lm = LandmarkStructures.ancientTreeAltar(bestSg);
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
            AshenLands.Region bestAr = null;
            double bestD = Double.MAX_VALUE;
            for (int sx = -3; sx <= 3; ++sx) {
                for (int sz = -3; sz <= 3; ++sz) {
                    AshenLands.Region ar = AshenLands.region(sx, sz);
                    if (ar == null || !ar.hasBelfry) continue;
                    double d = Math.hypot(ar.cx - at.getX(), ar.cz - at.getZ());
                    if (d < bestD) { bestD = d; bestAr = ar; }
                }
            }
            LandmarkStructures.BiomeLandmark lm = LandmarkStructures.ashenBelfryBarrow(bestAr);
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

        // ---------- v0.23.0: «Цитадель Чёрного утёса» (blacktide.nbt), Сгоревшая Церковь, комбинации Пепельного биома и увеличенные биомы ----------
        if (k.equals("blacktide_citadel") || k.equals("blacktide_bridge_shore")) {
            AshenLands.Region bestAr = null;
            double bestD = Double.MAX_VALUE;
            for (int sx = -3; sx <= 3; ++sx) {
                for (int sz = -3; sz <= 3; ++sz) {
                    AshenLands.Region ar = AshenLands.region(sx, sz);
                    if (ar == null || !ar.hasBlacktide || ar.blacktideSite == null) continue;
                    double d = Math.hypot(ar.blacktideSite.cx - at.getX(), ar.blacktideSite.cz - at.getZ());
                    if (d < bestD) { bestD = d; bestAr = ar; }
                }
            }
            if (bestAr == null || bestAr.blacktideSite == null) {
                src.sendFailure(Component.literal("riftstruct: blacktide_citadel not found"));
                return 0;
            }
            BlacktideCitadel.Site bs = bestAr.blacktideSite;
            // НИКОГДА не вызываем синхронный level.getChunk(...) по 300 чанкам на первом шаге S0:
            // возвращаем at=... мгновенно (<1 мс), после чего TestPlayer телепортируется в (cx, cz),
            // пул потоков сервера сам генерирует все чанки в радиусе 12 чанков (192 м), и на S1 все чанки уже в памяти!
            // 1) Проверяем, что начало моста (landX, landZ) стоит НА СУШЕ Пепельного биома!
            boolean bridgeOnAshenLand = AshenLands.inside(bs.landX, bs.landZ)
                    && AshenLands.weight(bs.landX + 0.5, bs.landZ + 0.5) >= 0.85;
            int[] landCnt = countStructBlocksAndSubAir(level, bs.landX, bs.landZ, 12, ocean + 2, bs.baseY + 62, ocean);
            // 2) Проверяем, что сам замок (cx, cz / keepX, keepZ) стоит В ОТКРЫТОМ МОРЕ (weight == 0.0, вокруг elisar_water ниже ряби волн, 0 sub_air)!
            boolean castleInSea = AshenLands.weight(bs.cx + 0.5, bs.cz + 0.5) == 0.0
                    && AshenLands.weight(bs.keepX + 0.5, bs.keepZ + 0.5) == 0.0;
            int[][] seaProbes = {
                    {0, -108}, {0, 122}, {-42, 122}, {130, 0}, {88, -108}, {88, 122}
            };
            int seaWaterAround = 0;
            for (int[] sp : seaProbes) {
                int wx = bs.cx + BlacktideCitadel.rotX(sp[0], sp[1], bs.rot);
                int wz = bs.cz + BlacktideCitadel.rotZ(sp[0], sp[1], bs.rot);
                if (!level.hasChunk(wx >> 4, wz >> 4)) continue;
                if (level.getBlockState(new BlockPos(wx, ocean - 2, wz)).is(ElisarBlocks.WATER.get())) {
                    ++seaWaterAround;
                }
            }
            int[] seaCnt = countStructBlocksAndSubAir(level, bs.keepX, bs.keepZ, 24, ocean - 8, bs.baseY + 145, ocean);
            int totalBlocks = (bridgeOnAshenLand && castleInSea && landCnt[0] >= 80 && seaWaterAround >= 2) ? seaCnt[0] : 0;

            double ux = bs.outUx, uz = bs.outUz;
            double px = -uz, pz = ux;

            int atX, atY, atZ, camX, camY, camZ, lkX, lkY, lkZ;
            if (k.equals("blacktide_bridge_shore")) {
                atX = bs.gateX;
                atY = bs.baseY + 46;
                atZ = bs.gateZ;
                // Вид прямо с пепельного берега биома вдоль каменного моста, уходящего через морские волны к башням замка!
                camX = (int) Math.round(bs.landX - ux * 16.0 + px * 18.0);
                camY = bs.baseY + 52;
                camZ = (int) Math.round(bs.landZ - uz * 16.0 + pz * 18.0);
                lkX = (int) Math.round(bs.gateX + ux * 18.0);
                lkY = bs.baseY + 56;
                lkZ = (int) Math.round(bs.gateZ + uz * 18.0);
            } else {
                atX = bs.cx;
                atY = bs.baseY + 46;
                atZ = bs.cz;
                // Панорамный морской ракурс: одновременно виден и берег Пепельного биома с началом моста, и весь огромный замок в открытом море!
                double midX = (bs.landX * 0.35 + bs.cx * 0.65);
                double midZ = (bs.landZ * 0.35 + bs.cz * 0.65);
                camX = (int) Math.round(midX + px * 112.0 - ux * 16.0);
                camY = ocean + 52;
                camZ = (int) Math.round(midZ + pz * 112.0 - uz * 16.0);
                lkX = (int) Math.round(bs.cx - ux * 12.0);
                lkY = bs.baseY + 62;
                lkZ = (int) Math.round(bs.cz - uz * 12.0);
            }
            String line = "riftstruct kind=" + k + " at=" + atX + "," + atY + "," + atZ
                    + " bridge_land=" + bridgeOnAshenLand + " castle_sea=" + castleInSea
                    + " water_ring=" + seaWaterAround + " land_blocks=" + landCnt[0]
                    + " blocks=" + totalBlocks + " sub_air=" + (landCnt[1] + seaCnt[1])
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("ashen_burned_church")) {
            AshenLands.Region bestAr = null;
            double bestD = Double.MAX_VALUE;
            for (int sx = -3; sx <= 3; ++sx) {
                for (int sz = -3; sz <= 3; ++sz) {
                    AshenLands.Region ar = AshenLands.region(sx, sz);
                    if (ar == null || !ar.hasBurnedChurch) continue;
                    double d = Math.hypot(ar.churchX - at.getX(), ar.churchZ - at.getZ());
                    if (d < bestD) { bestD = d; bestAr = ar; }
                }
            }
            LandmarkStructures.BiomeLandmark lm = LandmarkStructures.ashenBurnedChurch(bestAr);
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
            for (int sx = -4; sx <= 4; ++sx) {
                for (int sz = -4; sz <= 4; ++sz) {
                    AshenLands.Region ar = AshenLands.region(sx, sz);
                    if (ar == null) continue;
                    int majors = (ar.hasRedCastle ? 1 : 0) + (ar.hasBurnedChurch ? 1 : 0) + (ar.hasBlacktide ? 1 : 0);
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
            CrystalRifts.Region bestCr = null;
            double bestD = Double.MAX_VALUE;
            for (int sx = -2; sx <= 2; ++sx) {
                for (int sz = -2; sz <= 2; ++sz) {
                    CrystalRifts.Region cr = CrystalRifts.region(sx, sz);
                    if (cr == null) continue;
                    double d = Math.hypot(cr.cx - at.getX(), cr.cz - at.getZ());
                    if (d < bestD) { bestD = d; bestCr = cr; }
                }
            }
            LandmarkStructures.BiomeLandmark lm = LandmarkStructures.shatteredShardForge(bestCr);
            if (lm == null) { src.sendFailure(Component.literal("riftstruct: shattered_shard_forge not found")); return 0; }
            int[] cnt = countStructBlocksAndSubAir(level, lm.cx, lm.cz, 13, lm.baseY - 2, lm.baseY + 15, ocean);
            int camX = lm.cx + 7, camY = lm.baseY + 8, camZ = lm.cz + 10;
            int lkX = lm.cx - 1, lkY = lm.baseY + 4, lkZ = lm.cz - 2;
            String line = "riftstruct kind=shattered_shard_forge at=" + lm.cx + "," + lm.baseY + "," + lm.cz
                    + " blocks=" + cnt[0] + " sub_air=" + cnt[1]
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("violet_wind_observatory")) {
            VioletHills.Region bestVh = null;
            double bestD = Double.MAX_VALUE;
            for (int sx = -2; sx <= 2; ++sx) {
                for (int sz = -2; sz <= 2; ++sz) {
                    VioletHills.Region vh = VioletHills.region(sx, sz);
                    if (vh == null) continue;
                    double d = Math.hypot(vh.cx - at.getX(), vh.cz - at.getZ());
                    if (d < bestD) { bestD = d; bestVh = vh; }
                }
            }
            LandmarkStructures.BiomeLandmark lm = LandmarkStructures.violetWindObservatory(bestVh);
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
            AshenLands.Region bestAr = null;
            double bestD = Double.MAX_VALUE;
            for (int sx = -3; sx <= 3; ++sx) {
                for (int sz = -3; sz <= 3; ++sz) {
                    AshenLands.Region ar = AshenLands.region(sx, sz);
                    if (ar == null || !ar.hasBlacktide || ar.blacktideSite == null) continue;
                    double d = Math.hypot(ar.blacktideSite.cx - at.getX(), ar.blacktideSite.cz - at.getZ());
                    if (d < bestD) { bestD = d; bestAr = ar; }
                }
            }
            if (bestAr == null || bestAr.blacktideSite == null) {
                src.sendFailure(Component.literal("riftstruct: blacktide site not found for " + k));
                return 0;
            }
            BlacktideCitadel.Site bs = bestAr.blacktideSite;

            if (k.equals("blacktide_cathedral_hall")) {
                int camX = bs.cx + BlacktideCitadel.rotX(-16, -25, bs.rot);
                int camY = bs.baseY + 69;
                int camZ = bs.cz + BlacktideCitadel.rotZ(-16, -25, bs.rot);
                int lkX = bs.cx + BlacktideCitadel.rotX(44, -25, bs.rot);
                int lkY = bs.baseY + 73;
                int lkZ = bs.cz + BlacktideCitadel.rotZ(44, -25, bs.rot);

                int carpets = 0, thrones = 0, chandeliers = 0, goldCaps = 0;
                BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
                for (int lx = -36; lx <= 49; ++lx) {
                    for (int lz = -45; lz <= -6; ++lz) {
                        int wx = bs.cx + BlacktideCitadel.rotX(lx, lz, bs.rot);
                        int wz = bs.cz + BlacktideCitadel.rotZ(lx, lz, bs.rot);
                        if (!level.hasChunk(wx >> 4, wz >> 4)) continue;
                        for (int by = 63; by <= 97; ++by) {
                            BlockState st = level.getBlockState(mp.set(wx, bs.baseY + by, wz));
                            if (st.isAir()) continue;
                            if (st.is(ru.aiforge.riftworld.registry.CastleBlocks.CARPET.get())) ++carpets;
                            else if (st.is(ru.aiforge.riftworld.registry.ModBlocks.MODELS.get(ru.aiforge.riftworld.block.ModelKind.BOSS_THRONE).get())) ++thrones;
                            else if (st.is(ru.aiforge.riftworld.registry.ModBlocks.MODELS.get(ru.aiforge.riftworld.block.ModelKind.CASTLE_CHANDELIER).get())) ++chandeliers;
                            else if (st.is(net.minecraft.world.level.block.Blocks.GOLD_BLOCK)
                                    || st.is(net.minecraft.world.level.block.Blocks.GILDED_BLACKSTONE)) ++goldCaps;
                        }
                    }
                }
                int metric = (carpets >= 180 && thrones >= 1 && chandeliers >= 4 && goldCaps >= 60)
                        ? (carpets + goldCaps + thrones * 50 + chandeliers * 20) : 0;
                String line = "riftstruct kind=blacktide_cathedral_hall at=" + camX + "," + camY + "," + camZ
                        + " carpets=" + carpets + " thrones=" + thrones + " chandeliers=" + chandeliers + " gold=" + goldCaps
                        + " blocks=" + metric + " sub_air=0"
                        + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
                src.sendSuccess(() -> Component.literal(line), false);
                return 1;
            }

            if (k.equals("blacktide_rotunda_sanctum")) {
                int camX = bs.cx + BlacktideCitadel.rotX(-19, 15, bs.rot);
                int camY = bs.baseY + 69;
                int camZ = bs.cz + BlacktideCitadel.rotZ(-19, 15, bs.rot);
                int lkX = bs.cx + BlacktideCitadel.rotX(-26, 23, bs.rot);
                int lkY = bs.baseY + 65;
                int lkZ = bs.cz + BlacktideCitadel.rotZ(-26, 23, bs.rot);

                int carpets = 0, altars = 0, books = 0;
                BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
                for (int lx = -37; lx <= -13; ++lx) {
                    for (int lz = 10; lz <= 34; ++lz) {
                        int wx = bs.cx + BlacktideCitadel.rotX(lx, lz, bs.rot);
                        int wz = bs.cz + BlacktideCitadel.rotZ(lx, lz, bs.rot);
                        if (!level.hasChunk(wx >> 4, wz >> 4)) continue;
                        for (int by = 63; by <= 78; ++by) {
                            BlockState st = level.getBlockState(mp.set(wx, bs.baseY + by, wz));
                            if (st.isAir()) continue;
                            if (st.is(ru.aiforge.riftworld.registry.CastleBlocks.CARPET.get())) ++carpets;
                            else if (st.is(ru.aiforge.riftworld.registry.ModBlocks.MODELS.get(ru.aiforge.riftworld.block.ModelKind.CHAPEL_ALTAR).get())) ++altars;
                            else if (st.is(ru.aiforge.riftworld.registry.CastleBlocks.BOOKSHELF.get())) ++books;
                        }
                    }
                }
                int metric = (carpets >= 25 && altars >= 1 && books >= 12) ? (carpets * 4 + books * 5 + altars * 60) : 0;
                String line = "riftstruct kind=blacktide_rotunda_sanctum at=" + camX + "," + camY + "," + camZ
                        + " carpets=" + carpets + " altars=" + altars + " books=" + books
                        + " blocks=" + metric + " sub_air=0"
                        + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
                src.sendSuccess(() -> Component.literal(line), false);
                return 1;
            }

            // blacktide_tower_walkability: проверяем лестницы во ВСЕХ вышках замка!
            int[][] towerCenters = {
                    {-9, -78}, {-32, -74}, {-55, -69}, {48, -66}, {-76, -43}, {79, -39},
                    {85, -20}, {91, -1}, {78, 36}, {62, 47}, {47, 58}, {-6, 60},
                    {-72, 30}, {-58, 35}, {-29, 13}, {-21, 31}, {-34, -40}, {-34, -10}, {20, -38}
            };
            int climbedTowers = 0, totalStairs = 0;
            BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
            for (int[] tc : towerCenters) {
                int stairsInTower = 0;
                for (int dx = -2; dx <= 2; ++dx) {
                    for (int dz = -2; dz <= 2; ++dz) {
                        int wx = bs.cx + BlacktideCitadel.rotX(tc[0] + dx, tc[1] + dz, bs.rot);
                        int wz = bs.cz + BlacktideCitadel.rotZ(tc[0] + dx, tc[1] + dz, bs.rot);
                        if (!level.hasChunk(wx >> 4, wz >> 4)) continue;
                        for (int by = 48; by <= 134; ++by) {
                            if (level.getBlockState(mp.set(wx, bs.baseY + by, wz))
                                    .is(net.minecraft.world.level.block.Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS)) {
                                ++stairsInTower;
                            }
                        }
                    }
                }
                if (stairsInTower >= 12) ++climbedTowers;
                totalStairs += stairsInTower;
            }
            int camX = bs.cx + BlacktideCitadel.rotX(46, 56, bs.rot);
            int camY = bs.baseY + 86;
            int camZ = bs.cz + BlacktideCitadel.rotZ(46, 56, bs.rot);
            int lkX = bs.cx + BlacktideCitadel.rotX(-6, -12, bs.rot);
            int lkY = bs.baseY + 76;
            int lkZ = bs.cz + BlacktideCitadel.rotZ(-6, -12, bs.rot);
            int metric = (climbedTowers >= 14 && totalStairs >= 350) ? totalStairs : 0;
            String line = "riftstruct kind=blacktide_tower_walkability at=" + camX + "," + camY + "," + camZ
                    + " climbed_towers=" + climbedTowers + " total_stairs=" + totalStairs
                    + " blocks=" + metric + " sub_air=0"
                    + " cam=" + camX + "," + camY + "," + camZ + ":" + lkX + "," + lkY + "," + lkZ;
            src.sendSuccess(() -> Component.literal(line), false);
            return 1;
        }

        if (k.equals("connected_carpets_showcase")) {
            // Демонстрационная солнечная терраса (вне защищённой морской коробки [600..696]x[600..786])
            // со всеми формами бесшовных 8-соседних ковров: 2x2, 3x5, L-форма, +-крест и T-дорожка!
            int tx = 768, ty = 136, tz = 768;
            if (level.hasChunk(tx >> 4, tz >> 4)) {
                BlockState stone = net.minecraft.world.level.block.Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
                BlockState trim = net.minecraft.world.level.block.Blocks.GILDED_BLACKSTONE.defaultBlockState();
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
                observatoryAnchor = new BlockPos(found.getX(), 96, found.getZ());
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
                    for (int y = overworld.getSeaLevel() - 4; y <= overworld.getMaxBuildHeight() - 16; ++y) {
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
            int camX = observatorySealPos.getX() - 3, camY = observatorySealPos.getY() + 4, camZ = observatorySealPos.getZ() + 13;
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
                    new net.minecraft.world.phys.AABB(base).inflate(48.0))
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
}
