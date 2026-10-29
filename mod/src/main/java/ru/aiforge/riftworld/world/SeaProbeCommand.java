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
        e.getDispatcher().register(Commands.literal("riftdtwhy").requires(s2 -> s2.hasPermission(2))
                .executes(c -> dtWhy(c.getSource())));
        e.getDispatcher().register(Commands.literal("riftaround").requires(s2 -> s2.hasPermission(2))
                .then(Commands.argument("r", com.mojang.brigadier.arguments.IntegerArgumentType.integer(4, 48))
                        .executes(c -> around(c.getSource(),
                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "r")))));
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
                    if (d < bestD) { bestD = d; bestX = rg.cx; bestZ = rg.cz; bestR = (int) rg.hub.R; bestSizo = rg; }
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
        int R = Math.max(22, Math.min(40, bestR));
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
        int violetBlooms = 0, violetLeaves = 0, fences = 0, lamps = 0, lavender = 0;
        if (violet) {
            var bloom = new java.util.HashSet<net.minecraft.world.level.block.Block>();
            bloom.add(ru.aiforge.riftworld.registry.ElisarBlocks.VIOLET_BLOOM.get());
            bloom.add(ru.aiforge.riftworld.registry.ElisarBlocks.LILAC_BLOOM.get());
            bloom.add(ru.aiforge.riftworld.registry.ElisarBlocks.PALE_BLOOM.get());
            var vl = ru.aiforge.riftworld.registry.ElisarBlocks.VIOLET_LEAVES.get();
            var fenceBlk = ru.aiforge.riftworld.registry.ElisarBlocks.FENCE.get();
            var lavenderBlk = ru.aiforge.riftworld.registry.ElisarBlocks.LAVENDER_BED.get();
            for (int x = bestX - R; x <= bestX + R; x += 1) {
                for (int z = bestZ - R; z <= bestZ + R; z += 1) {
                    if (!level.isLoaded(new BlockPos(x, at.getY(), z))) continue;
                    for (int y = 60; y <= 150; ++y) {
                        BlockState bs = level.getBlockState(new BlockPos(x, y, z));
                        if (bs.isAir()) continue;
                        if (bloom.contains(bs.getBlock())) { violetBlooms++; continue; }
                        if (bs.getBlock() == vl) { violetLeaves++; continue; }
                        if (bs.getBlock() == lavenderBlk) { lavender++; continue; }
                        if (bs.getBlock() == fenceBlk) { fences++; continue; }
                        if (bs.getBlock() instanceof net.minecraft.world.level.block.LanternBlock) lamps++;
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
        if (bestGarden != null && !bestGarden.falls.isEmpty()) {
            WaterfallGardens.Fall f0 = bestGarden.falls.get(0);
            fallAt = f0.x + "," + f0.z;
        }
        // Рев 14.18: точка обзора обрыва — кадр с полки, как на референсе
        String wall = "-";
        if (gardens) {
            int[] wv = ElisarChunkGenerator.gardenWallView(bestX, bestZ, Math.max(24, bestR));
            if (wv != null) wall = wv[0] + "," + wv[1] + "," + wv[2] + ":" + wv[3] + "," + wv[4] + "," + wv[5];
        }
        String vio = violet
                ? " violet_blooms=" + violetBlooms + " violet_leaves=" + violetLeaves + " lavender=" + lavender
                  + " fences=" + fences + " lamps=" + lamps
                : "";
        // Рев 14.35: каким биомом НА САМОМ ДЕЛЕ числится земля региона. Это ловит перепутанные
        // поля биом-сурса (именно так «сады» оказались на островках данжа в море).
        String hubBio = level.getBiome(new BlockPos(bestX, CityPlan.get().ocean + 6, bestZ)).unwrapKey()
                .map(k -> k.location().toString()).orElse("?");
        String line = "riftbiome name=" + (violet ? "violet" : gardens ? "gardens" : "sizo") + " at=" + bestX + "," + bestZ
                + " bio=" + hubBio
                + " r=" + bestR + " top=" + top + " water=" + water + " garden_grass=" + gardenGrass
                + " garden_leaves=" + leaves + " garden_logs=" + logs + " ivy=" + ivy
                + " planks=" + planks + " lantern=" + lanterns + " struct=" + struct
                + " fall=" + fallAt + " wall=" + wall + vio + " blocks=" + top6;
        src.sendSuccess(() -> Component.literal(line), false);
        found = water;
        return found;
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
        String topName = topBiome.unwrapKey().map(k -> k.location().toString()).orElse("?");
        String surfName = surfBiome.unwrapKey().map(k -> k.location().toString()).orElse("?");
        BlockPos ground = ru.aiforge.riftworld.world.DtGround.groundFor(level, at);
        final String line = "riftdtwhy at=" + at.getX() + "," + at.getY() + "," + at.getZ()
                + " biome_surface=" + surfName + " biome_top=" + topName
                + " dt_features=" + feats + " has_dt=" + hasDt
                + " ground=" + (ground == null ? "-" : ground.getY() + ":" + level.getBlockState(ground).getBlock());
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
                            || k0.getPath().contains("leaves") || k0.getPath().contains("vine"))) continue;
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
     * Рев 14.42: {@code /riftdtwhy} — почему в этом биоме нет деревьев Dynamic Trees.
     *
     * <p>Печатает три факта, каждый из которых может быть причиной «аддон ничего не меняет»:
     * {@code biome_top=} (биом на высоте мира, по нему DT выбирает плотность и вид),
     * {@code dt_features=}/{@code has_dt=} (есть ли вообще в generationSettings этого биома
     * фича {@code dynamictrees:dynamic_tree} — её добавляет Forge-модификатор DT),
     * {@code ground=} (что находит НАШ GroundFinder под игроком).
     */
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
        String topName = topBiome.unwrapKey().map(k -> k.location().toString()).orElse("?");
        String surfName = surfBiome.unwrapKey().map(k -> k.location().toString()).orElse("?");
        BlockPos ground = ru.aiforge.riftworld.world.DtGround.groundFor(level, at);
        final String line = "riftdtwhy at=" + at.getX() + "," + at.getY() + "," + at.getZ()
                + " biome_surface=" + surfName + " biome_top=" + topName
                + " dt_features=" + feats + " has_dt=" + hasDt
                + " ground=" + (ground == null ? "-" : ground.getY() + ":" + level.getBlockState(ground).getBlock());
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
        int lime = 0, grass = 0, stone = 0, lavender = 0, holes = 0, cols = 0;
        int y0 = Math.max(level.getMinBuildHeight(), at.getY() - 40);
        int y1 = Math.min(level.getMaxBuildHeight() - 1, at.getY() + 40);
        for (int x = at.getX() - r; x <= at.getX() + r; ++x) {
            for (int z = at.getZ() - r; z <= at.getZ() + r; ++z) {
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                ++cols;
                int top = Integer.MIN_VALUE;
                for (int y = level.getMaxBuildHeight() - 1; y >= level.getMinBuildHeight(); --y) {
                    if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) { top = y; break; }
                }
                if (top == Integer.MIN_VALUE) continue;
                for (int y = level.getMinBuildHeight() + 2; y <= top - 4; ++y) {
                    if (level.getBlockState(new BlockPos(x, y, z)).isAir()) { ++holes; break; }
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
                + " cols=" + cols + " garden_limestone=" + lime + " garden_grass=" + grass
                + " lavender=" + lavender + " elisar_stone=" + stone + " holes=" + holes;
        src.sendSuccess(() -> Component.literal(line), false);
        return lime + grass;
    }

    private static int dtWild(CommandSourceStack src, int r) {
        var level = src.getLevel();
        BlockPos at = BlockPos.containing(src.getPosition());
        int dt = 0, rooty = 0, branch = 0, leaves = 0;
        java.util.Map<String, Integer> named = new java.util.TreeMap<>();
        int y0 = Math.max(level.getMinBuildHeight(), at.getY() - 40);
        int y1 = Math.min(level.getMaxBuildHeight() - 1, at.getY() + 40);
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
        String[] blocks = {"sizo_leaves", "ambra_leaves", "coral_staghorn_leaves", "coral_table_leaves",
                "coral_whip_leaves", "rooty_elisar_grass", "rooty_elisar_soil",
                "sizo_branch", "ambra_branch", "coral_branch",
                // саженцы DT — только блоки, предметов у них нет по дизайну самого DT
                "sizo_sapling", "ambra_sapling", "giant_sizo_sapling", "coral_staghorn_sapling",
                "coral_table_sapling", "coral_whip_sapling"};
        String[] items = {"sizo_seed", "ambra_seed", "coral_staghorn_seed", "coral_table_seed",
                "coral_whip_seed", "giant_sizo_seed",
                "sizo_branch", "ambra_branch", "coral_branch"};
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
            // NB: static get() у DT только со String, и внутри он делает ResourceLocation.tryParse —
            // поэтому "riftworlddt" превратился бы в minecraft:riftworlddt. Передаём полное имя.
            Object h = cls.getMethod("get", String.class).invoke(null, "riftworlddt:riftworlddt");
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
                if (n++ >= 6) { sb.append(",.."); break; }
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
                    if (!isCoralBody(level.getBlockState(new BlockPos(x, y, z)))) break;
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
                    if (isCoralBody(st)) {
                        ++blocks;
                        col = true;
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

    /** Блок «тела» кораллового дерева (шапка палитры) — то, из чего сложены ствол и ветви. */
    private static boolean isCoralBody(BlockState s) {
        return s.is(ElisarBlocks.CORAL_CAP_PINK.get()) || s.is(ElisarBlocks.CORAL_CAP_TEAL.get())
                || s.is(ElisarBlocks.CORAL_CAP_VIOLET.get())
                || s.is(ElisarBlocks.CORAL_FOLIAGE_PINK.get()) || s.is(ElisarBlocks.CORAL_FOLIAGE_TEAL.get())
                || s.is(ElisarBlocks.CORAL_FOLIAGE_VIOLET.get())
                // Рев 12: стволы/ветви коралловых деревьев (узкие колонны)
                || s.is(ElisarBlocks.CORAL_STEM_PINK.get()) || s.is(ElisarBlocks.CORAL_STEM_TEAL.get())
                || s.is(ElisarBlocks.CORAL_STEM_VIOLET.get());
    }

    private static boolean isCoral(BlockState s) {
        return s.is(ElisarBlocks.CORAL_BRANCH.get()) || s.is(ElisarBlocks.CORAL_FAN.get())
                || s.is(ElisarBlocks.CORAL_TUBE.get()) || s.is(ElisarBlocks.CORAL_GLOW.get())
                || s.is(ElisarBlocks.CORAL_BRAIN.get())
                || s.is(ElisarBlocks.ANEMONE.get()) || s.is(ElisarBlocks.CORAL_BUSH.get())
                || s.is(ElisarBlocks.TUBE_WORMS.get())
                // Рев 11: новые кораллы-растения с 3D-моделью
                || s.is(ElisarBlocks.STAGHORN_CORAL.get()) || s.is(ElisarBlocks.FEATHER_CORAL.get())
                || s.is(ElisarBlocks.SEA_WHIP.get()) || s.is(ElisarBlocks.TABLE_CORAL.get())
                || s.is(ElisarBlocks.TUBE_SPONGE_CLUSTER.get()) || s.is(ElisarBlocks.RIBBON_CORAL.get());
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
}
