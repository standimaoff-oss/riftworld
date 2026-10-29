package ru.aiforge.riftworld.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.registry.ElisarBlocks;

/**
 * Рев 10 «Живое море 2» — дно, впадины, затопленные пещеры и рифовые сады открытого океана Элисара.
 *
 * <p>Что изменилось относительно Рев 8 (по разбору в {@code reports/PLAN_sea_rev10.md}):
 * <ul>
 *   <li><b>Мир стал глубоким:</b> низ Элисара опущен на {@code min_y = -64} (см. {@code dimension_type/elisar.json}),
 *       поэтому под кромкой дна теперь 45–70 блоков породы, а не 10–18. Пещерам есть где жить.</li>
 *   <li><b>Пещеры затоплены по-настоящему.</b> Раньше вода решалась «по колонке» (первая твёрдая клетка навсегда
 *       выключала протечку) — 93% объёма пещер было сухим воздухом, а вода стояла вертикальными столбами.
 *       Теперь вода заполняет всё вырезанное пространство, а <b>воздушные карманы</b> задаются отдельным шумом
 *       у сводов (как ловушки воздуха в настоящих подводных гротах). Решение чисто аналитическое, поэтому
 *       одинаково с обеих сторон границы чанка — никаких «водяных стен».</li>
 *   <li><b>Рельеф дна</b> получил дюны, гребни, желоба-ущелья и редкие <b>абиссальные впадины</b> вместо
 *       девяти плоских плит.</li>
 * </ul>
 */
public final class ElisarSea {
    /** Нижняя граница породы: два блока бедрока у самого низа мира (min_y = -64). */
    public static final int BOTTOM = -62;
    /** Глубже этого кромка дна не опускается (даже во впадинах). */
    public static final int DEEPEST_FLOOR = -34;

    private ElisarSea() {
    }

    // ------------------------------------------------------------------ шум
    static double hash(int x, int z, int s) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ s * 0x165667B19E3779F9L;
        h ^= h >>> 33; h *= 0xFF51AFD7ED558CCDL; h ^= h >>> 33; h *= 0xC4CEB9FE1A85EC53L; h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }

    static double hash3(int x, int y, int z, int s) {
        long h = x * 0x9E3779B97F4A7C15L ^ y * 0xD6E8FEB86659FD93L ^ z * 0xC2B2AE3D27D4EB4FL ^ s * 0x165667B19E3779F9L;
        h ^= h >>> 33; h *= 0xFF51AFD7ED558CCDL; h ^= h >>> 33; h *= 0xC4CEB9FE1A85EC53L; h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }

    static double vnoise(double x, double z, int s) {
        int xi = (int) Math.floor(x), zi = (int) Math.floor(z);
        double fx = x - xi, fz = z - zi;
        fx = fx * fx * (3 - 2 * fx); fz = fz * fz * (3 - 2 * fz);
        double a = hash(xi, zi, s), b = hash(xi + 1, zi, s), c = hash(xi, zi + 1, s), d = hash(xi + 1, zi + 1, s);
        double ab = a + (b - a) * fx, cd = c + (d - c) * fx;
        return ab + (cd - ab) * fz;
    }

    static double vnoise3(double x, double y, double z, int s) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y), zi = (int) Math.floor(z);
        double fx = x - xi, fy = y - yi, fz = z - zi;
        fx = fx * fx * (3 - 2 * fx); fy = fy * fy * (3 - 2 * fy); fz = fz * fz * (3 - 2 * fz);
        double c000 = hash3(xi, yi, zi, s), c100 = hash3(xi + 1, yi, zi, s);
        double c010 = hash3(xi, yi + 1, zi, s), c110 = hash3(xi + 1, yi + 1, zi, s);
        double c001 = hash3(xi, yi, zi + 1, s), c101 = hash3(xi + 1, yi, zi + 1, s);
        double c011 = hash3(xi, yi + 1, zi + 1, s), c111 = hash3(xi + 1, yi + 1, zi + 1, s);
        double x00 = c000 + (c100 - c000) * fx, x10 = c010 + (c110 - c010) * fx;
        double x01 = c001 + (c101 - c001) * fx, x11 = c011 + (c111 - c011) * fx;
        double y0 = x00 + (x10 - x00) * fy, y1 = x01 + (x11 - x01) * fy;
        return y0 + (y1 - y0) * fz;
    }

    // ------------------------------------------------------------------ рельеф дна
    /** Насколько место «рифовое»: 0 — обычный ил/песок, 1 — плотный рифовый сад. */
    static double reefness(int x, int z) {
        double n = vnoise(x * 0.028, z * 0.028, 421) * 0.7 + vnoise(x * 0.075, z * 0.075, 422) * 0.3;
        return Math.max(0, Math.min(1, (n - 0.54) / 0.24));
    }

    /** Глубина желоба-ущелья под этой колонкой (0 — дно ровное). Рев 10: до 16 блоков. */
    static int trenchDepth(int x, int z) {
        double n = vnoise(x * 0.011, z * 0.011, 423) * 0.72 + vnoise(x * 0.035, z * 0.035, 424) * 0.28;
        if (n < 0.58) return 0;
        double t = (n - 0.58) / 0.42;
        return (int) Math.round(t * t * 16.0);
    }

    /** Абиссальная впадина: редкий широкий провал в дне до 22 блоков вглубь (синяя бездна, видно издалека). */
    static int pitDepth(int x, int z) {
        double n = vnoise(x * 0.0055, z * 0.0055, 481) * 0.8 + vnoise(x * 0.019, z * 0.019, 482) * 0.2;
        if (n < 0.70) return 0;
        double t = (n - 0.70) / 0.30;
        return (int) Math.round(t * t * 22.0);
    }

    /**
     * Кромка дна: 46–58 блоков воды в норме, желоба до −16 и впадины до −22 от неё.
     *
     * @param sea уровень моря ({@code plan.ocean})
     */
    public static int floor(int x, int z, int sea) {
        double n = vnoise(x * 0.016, z * 0.016, 77) * 0.52 + vnoise(x * 0.045, z * 0.045, 78) * 0.24
                + vnoise(x * 0.095, z * 0.095, 79) * 0.14 + vnoise(x * 0.21, z * 0.21, 80) * 0.10;
        int f = sea - 45 - (int) Math.round(n * 13);
        f += duneRipple(x, z);                     // песчаная рябь: волны по 8-14 блоков
        f += boulder(x, z);                        // редкие валуны на дне
        f -= trenchDepth(x, z);
        f -= pitDepth(x, z);
        return Math.max(DEEPEST_FLOOR, f);
    }

    /**
     * Песчаные дюны: вытянутые волны высотой ±1..2 блока. Без них ровные участки дна читаются
     * как бетонные плиты (замечание по скриншотам Рев 10, партия 1).
     */
    static int duneRipple(int x, int z) {
        double warp = vnoise(x * 0.018, z * 0.018, 483) * 7.0;
        double phase = (x * 0.62 + z * 0.42) * 0.30 + warp;
        double amp = 0.55 + 0.85 * vnoise(x * 0.010, z * 0.010, 484);       // где-то дюны выше
        return (int) Math.round(Math.sin(phase) * amp);
    }

    /** Валун: редкий каменный бугор 1–3 блока на сетке 19×19 (щебень, замшелый камень). */
    static int boulder(int x, int z) {
        int gx = Math.floorDiv(x, 19), gz = Math.floorDiv(z, 19);
        if (hash(gx, gz, 485) > 0.34) return 0;
        double cx = gx * 19 + 3 + hash(gx, gz, 486) * 13, cz = gz * 19 + 3 + hash(gx, gz, 487) * 13;
        double r = 1.6 + hash(gx, gz, 488) * 2.6;
        double d = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
        if (d >= r) return 0;
        return (int) Math.round((1.0 - d / r) * (1.0 + hash(gx, gz, 489) * 2.2));
    }

    // ------------------------------------------------------------------ пещеры
    /**
     * «Пещерность» точки: > 0 — внутри полости. Два уровня туннелей-червей (мелкий у дна и глубокий лабиринт)
     * плюс залы-гроты. Чем глубже, тем шире ходы — так глубина читается силуэтом.
     */
    static double caveValue(int x, int y, int z, int floor) {
        double deep = Math.max(0, Math.min(1, (floor - y) / 58.0));
        // ходы: изолиния двух 3D-шумов (пересечение «трубы»), масштаб реже с глубиной
        double s = 0.028 + 0.012 * deep;
        double w1 = vnoise3(x * s, y * 0.048, z * s, 431);
        double w2 = vnoise3(x * s, y * 0.048, z * s, 432);
        double d = Math.hypot(w1 - 0.5, w2 - 0.5);
        double width = 0.086 + 0.070 * deep + 0.034 * vnoise3(x * 0.008, y * 0.011, z * 0.008, 433);
        double tunnel = width - d;
        if (tunnel > 0) return tunnel;
        // залы: отдельный шум, чем глубже — тем охотнее раскрывается
        double hall = vnoise3(x * 0.022, y * 0.038, z * 0.022, 434);
        return (hall - (0.745 - 0.045 * deep)) * 0.5;
    }

    /**
     * Рев 11 — ВОДА ПО НАПРАВЛЕНИЮ ХОДА, а не «случайная заливка».
     *
     * <p>Раньше воздух в пещерах ставился одним 3D-шумом: полости были заполнены как попало,
     * и в нисходящем ходе мог висеть сухой пузырь, а в восходящем тупике — вода. Теперь решает геометрия:
     * <ul>
     *   <li><b>Ход уходит вниз</b> — вода заливает его целиком: воздуху неоткуда взяться, он всплывает;</li>
     *   <li><b>Ход горизонтальный</b> — вода стоит, но у сводов остаются карманы: заливка неполная;</li>
     *   <li><b>Ход уходит вверх и не связан с морем</b> — это ловушка: вода туда не поднимется, воздух остаётся;</li>
     *   <li><b>Ход уходит вверх, но сверху открытая вода</b> (пробитое дно или «синяя дыра») — залит доверху.</li>
     * </ul>
     * Всё считается по аналитическому полю пещер, поэтому на границах чанков решение не рвётся.
     */
    static boolean dryPocket(int x, int y, int z, int floor) {
        // 1) СВЯЗЬ С МОРЕМ. Воздух держится только в полости, которую сверху закрывает порода:
        //    если над клеткой идёт непрерывный ход до самого дна/колодца, вода туда опустится.
        if (openAbove(x, y, z, floor)) return false;
        // 2) НАПРАВЛЕНИЕ ХОДА. Нисходящий ход всегда полон воды (воздух всплывает),
        //    восходящий тупик — наоборот, воздушная пробка.
        boolean down = cave(x, y - 4, z, floor);
        boolean up = cave(x, y + 4, z, floor);
        if (down && !up) return false;                     // нисходящий ход — всегда в воде
        // 3) КАРМАН У СВОДА. Воздух стоит только в верхних 3 блоках полости — там, где до потолка
        //    рукой подать. Раньше шум давал «пузыри» в середине затопленного хода, и это читалось
        //    как дырка в воде; теперь воздух всегда прижат к камню сверху.
        if (ceilingGap(x, y, z, floor) > 3) return false;
        if (up && !down) return true;                      // запечатанный восходящий тупик: пробка воздуха
        // наклонный/горизонтальный ход — редкие карманы у самого свода
        return up ? ceilingPocket(x, y, z) : airPocket(x, y, z);
    }

    /**
     * Есть ли над клеткой сплошной проход до открытой воды (дно пробито, колодец «синей дыры»
     * или ход выходит к кромке дна). Идём вверх до первого камня; если камень встретился раньше,
     * чем ход дошёл до верха зоны пещер, — полость запечатана.
     */
    static boolean openAbove(int x, int y, int z, int floor) {
        int top = floor - 6;                       // выше этой линии полостей не бывает
        for (int yy = y + 1; yy <= top; ++yy) {
            if (blueHole(x, yy, z, floor)) return true;
            if (!cave(x, yy, z, floor)) return false;   // камень над головой — полость запечатана
        }
        // ход непрерывен до самого верха зоны пещер: вода придёт, только если дно здесь пробито
        return roofOpen(x, z, floor) || blueHole(x, top + 1, z, floor);
    }

    /** Пробито ли дно этой колонки (тот же хеш, что и у заливки свода в {@link #column}). */
    static boolean roofOpen(int x, int z, int floor) {
        return hash(x, z, 454) < 0.18;
    }

    /** Сколько блоков до каменного свода полости (0 — клетка сама у свода). */
    static int ceilingGap(int x, int y, int z, int floor) {
        for (int d = 1; d <= 3; ++d) {
            if (!cave(x, y + d, z, floor)) return d - 1;
        }
        return 99;
    }

    /** Воздушный карман: у свода полости, задаётся отдельным шумом (там можно вдохнуть). */
    static boolean airPocket(int x, int y, int z) {
        return vnoise3(x * 0.043, y * 0.062, z * 0.043, 435) > 0.70;
    }

    /** Второй сорт кармана: у свода полости достаточно и более слабого шума (воздух всплывает к потолку). */
    static boolean ceilingPocket(int x, int y, int z) {
        return vnoise3(x * 0.043, y * 0.062, z * 0.043, 435) > 0.585;
    }

    /** Пещера ли эта точка породы (ниже кромки дна). */
    static boolean cave(int x, int y, int z, int floor) {
        // полости держим не ближе 6 блоков к кромке: иначе дно становится решетом
        if (y <= BOTTOM + 1 || y > floor - 6) return false;
        return caveValue(x, y, z, floor) > 0;
    }

    /** «Синяя дыра»: колодец с морского дна вниз, с расширением-залом на дне. */
    static boolean blueHole(int x, int y, int z, int floor) {
        int gx = Math.floorDiv(x, 26), gz = Math.floorDiv(z, 26);
        if (hash(gx, gz, 441) < 0.40) return false;                       // не в каждой ячейке 26x26
        double cx = gx * 26 + 5 + hash(gx, gz, 442) * 16, cz = gz * 26 + 5 + hash(gx, gz, 443) * 16;
        double r = 2.0 + hash(gx, gz, 444) * 2.4;
        int depth = 12 + (int) (hash(gx, gz, 445) * 14);
        double d = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
        if (y > floor || y < floor - depth - 6) return false;
        if (d <= r) return true;
        // зал у основания колодца
        return y < floor - depth && d <= r + 3.8 && y > floor - depth - 6;
    }

    // ------------------------------------------------------------------ материалы
    private static BlockState st(RegistryObject<? extends Block> b) {
        return b.get().defaultBlockState();
    }

    /** Порода дна: сверху песок/ил/риф, ниже — камень с прослоями, у самого низа — тёмный камень. */
    private static BlockState rock(int x, int y, int z, int floor, double reef) {
        int d = floor - y;
        if (d == 0 && boulder(x, z) > 0 && reef < 0.5) {
            return hash(x, z, 490) < 0.55 ? st(ElisarBlocks.COBBLE) : st(ElisarBlocks.STONE_MOSSY);
        }
        if (d == 0) {
            if (reef > 0.62) return hash(x, z, 451) < 0.14 ? st(ElisarBlocks.CORAL_BRAIN) : st(ElisarBlocks.REEF_STONE);
            if (reef > 0.30) return st(ElisarBlocks.REEF_STONE);
            double sand = vnoise(x * 0.035, z * 0.035, 452);
            if (hash(x, z, 455) < 0.035) return st(ElisarBlocks.COBBLE);           // одиночная галька
            if (sand < 0.42) return st(ElisarBlocks.SILT);                          // тёмный ил
            return st(ElisarBlocks.SEABED);                                        // тёплый песок
        }
        if (d <= 2) {
            if (reef > 0.55) return st(ElisarBlocks.REEF_STONE);
            return vnoise(x * 0.05, z * 0.05, 452) > 0.50 ? st(ElisarBlocks.SILT) : st(ElisarBlocks.SEABED);
        }
        if (d <= 5) return hash(x, y, z) < 0.35 ? st(ElisarBlocks.SEABED) : st(ElisarBlocks.STONE);
        double n = vnoise3(x * 0.05, y * 0.09, z * 0.05, 453);
        if (d < 30) {
            if (n > 0.70) return st(ElisarBlocks.REEF_STONE);
            if (n < 0.30) return st(ElisarBlocks.STONE_MOSSY);
            return hash(x, y, z) < 0.10 ? st(ElisarBlocks.COBBLE) : st(ElisarBlocks.STONE);
        }
        // глубокая порода: тёмные слои с прожилками и редкими карманами ила
        if (n > 0.78) return st(ElisarBlocks.STONE_MOSSY);
        return hash(x, y, z) < 0.08 ? st(ElisarBlocks.COBBLE) : st(ElisarBlocks.STONE);
    }

    // ------------------------------------------------------------------ заливка колонки
    /**
     * Порода, пещеры и вода в точке (x,z). Декорации (сад на дне, убранство пещер) ставятся отдельным проходом
     * {@link #decorate} — уже после того, как вся колонка залита.
     *
     * @param floor кромка дна (см. {@link #floor(int, int, int)})
     * @param sea   уровень моря
     */
    public static void column(ChunkAccess chunk, BlockPos.MutableBlockPos mp, int x, int z, int floor, int sea) {
        double reef = reefness(x, z);
        for (int y = BOTTOM; y <= floor; ++y) chunk.setBlockState(mp.set(x, y, z), rock(x, y, z, floor, reef), false);
        // бедрок: два блока у самого низа мира
        for (int y = BOTTOM - 2; y < BOTTOM; ++y) chunk.setBlockState(mp.set(x, y, z), Blocks.BEDROCK.defaultBlockState(), false);

        // пещеры: вырезаем и затопляем. Воздушные карманы — только у сводов, отдельным шумом,
        // поэтому решение не зависит от соседних колонок и на границах чанков не рвётся.
        BlockState water = st(ElisarBlocks.WATER);
        boolean holeColumn = false;
        for (int y = BOTTOM; y <= floor - 6; ++y) {
            boolean shaft = blueHole(x, y, z, floor);
            if (!shaft && !cave(x, y, z, floor)) continue;
            if (shaft) holeColumn = true;
            boolean pocket = !shaft && dryPocket(x, y, z, floor);
            chunk.setBlockState(mp.set(x, y, z), pocket ? Blocks.AIR.defaultBlockState() : water, false);
        }

        // свод над полостью: «синяя дыра» всегда пробивает дно, обычная пещера — примерно в половине мест
        if (floor > BOTTOM + 4) {
            boolean belowCarved = blueHole(x, floor - 1, z, floor) || cave(x, floor - 1, z, floor);
            if (belowCarved && (holeColumn || hash(x, z, 454) < 0.18)) chunk.setBlockState(mp.set(x, floor, z), water, false);
        }

        for (int y = floor + 1; y <= sea; ++y) chunk.setBlockState(mp.set(x, y, z), water, false);
    }

    // ------------------------------------------------------------------ рифовый сад на дне
    /** Сад и убранство: ставится после воды, чтобы видеть итоговый блок под растением. */
    public static void decorate(ChunkAccess chunk, BlockPos.MutableBlockPos mp, int x, int z, int floor, int sea) {
        garden(chunk, mp, x, z, floor, sea, reefness(x, z));
        caveDressing(chunk, mp, x, z, floor);
    }

    /** Палитра рифовой зоны: 0 — розовая, 1 — бирюзовая, 2 — фиолетовая (пятнами по 48 блоков). */
    static int palette(int x, int z) {
        double h = hash(Math.floorDiv(x, 48), Math.floorDiv(z, 48), 476);
        return h < 0.38 ? 0 : h < 0.72 ? 1 : 2;
    }

    static BlockState capState(int p) {
        return p == 0 ? st(ElisarBlocks.CORAL_CAP_PINK) : p == 1 ? st(ElisarBlocks.CORAL_CAP_TEAL) : st(ElisarBlocks.CORAL_CAP_VIOLET);
    }

    /**
     * Нарост на вершине рифового холма. Рев 14.26: владелец потребовал убрать трёхмерные модели
     * кораллов размером в один блок — вершина собирается ТОЛЬКО из полноценных кубов: мозговой
     * коралл и шапки трёх палитр. Цветные пятна остаются, «крестиков» больше нет.
     */
    private static BlockState crown(int p, double r) {
        if (r < 0.22) return st(ElisarBlocks.CORAL_BRAIN);
        int q = r < 0.48 ? p : r < 0.66 ? (p + 1) % 3 : r < 0.86 ? (p + 2) % 3 : p;
        return capState(q);
    }

    /** Высота рифового холма в точке (0 — между холмами). Нужна посадкам коралловых деревьев. */
    static int moundHeightAt(int x, int z) {
        return moundHeight(x, z, reefness(x, z));
    }

    /**
     * Высота рифового купола в точке: 0 — между холмами. Пики живут на сетке 13×13 со случайным сдвигом,
     * высота спадает по параболе от пика, поэтому сад читается округлыми буграми, а не ступенчатыми плато.
     */
    static int moundHeight(int x, int z, double reef) {
        int gx = Math.floorDiv(x, 13), gz = Math.floorDiv(z, 13);
        double best = 0;
        for (int ax = -1; ax <= 1; ++ax) {
            for (int az = -1; az <= 1; ++az) {
                int cx = gx + ax, cz = gz + az;
                // Рев 14.28: купола-рифы пореже (владелец: меньше больших кораллов в океане)
                if (hash(cx, cz, 477) > 0.20 + reef * 0.35) continue;          // не в каждой ячейке есть холм
                double px = cx * 13 + 2.5 + hash(cx, cz, 478) * 8.0;
                double pz = cz * 13 + 2.5 + hash(cx, cz, 479) * 8.0;
                double r = 3.2 + hash(cx, cz, 480) * 4.4;                       // радиус купола
                double d = Math.hypot(x + 0.5 - px, z + 0.5 - pz);
                if (d >= r) continue;
                double hMax = 1.4 + hash(cx, cz, 481) * 2.4;                    // высота пика (рев 14.28: ниже)
                double t = 1.0 - d / r;
                double h = hMax * t * t * (1.0 + 0.35 * t);                     // крутая вершина, пологий склон
                if (h > best) best = h;
            }
        }
        // мелкая рябь по склону: не идеальный конус
        if (best > 0) best += (vnoise(x * 0.4, z * 0.4, 482) - 0.5) * 0.8;
        return best < 0.75 ? 0 : (int) Math.min(5, Math.round(best));
    }

    private static void garden(ChunkAccess chunk, BlockPos.MutableBlockPos mp, int x, int z, int floor, int sea, double reef) {
        int y = floor + 1;
        if (y > 60) return;
        // Рев 12: КОРАЛЛОВЫЕ ДЕРЕВЬЯ. Растение занимает несколько колонок (см. CoralGrove) и ставится
        // раньше всей прочей декорации: если в этой колонке есть ствол/ветвь/плита, она забирает место.
        // Рев 13: колонка может содержать НЕСКОЛЬКО отрезков (ветвь проходит над ветвью),
        // а на концах ветвей стоят полипы — раньше был один непрерывный столбик с шапкой.
        // Высоты в CoralGrove АБСОЛЮТНЫЕ и отсчитаны от дна колонки-хозяина, поэтому дерево
        // не «съезжает» на неровном склоне.
        java.util.List<CoralGrove.Span> spans = CoralGrove.spansAt(x, z, sea);
        if (!spans.isEmpty()) {
            boolean placed = false;
            for (CoralGrove.Span span : spans) {
                boolean free = true;
                for (int y2 = span.from(); y2 <= span.to(); ++y2) {
                    if (!chunk.getBlockState(mp.set(x, y2, z)).is(ElisarBlocks.WATER.get())) { free = false; break; }
                }
                if (!free) continue;
                BlockState bodyState = span.stem() ? CoralGrove.stem(x, z) : CoralGrove.body(x, z);
                for (int y2 = span.from(); y2 <= span.to(); ++y2) chunk.setBlockState(mp.set(x, y2, z), bodyState, false);
                placed = true;
            }
            if (placed) {
                for (CoralGrove.Tip t : CoralGrove.tipsAt(x, z, sea)) {
                    BlockPos tp = mp.set(x, t.y(), z);
                    if (chunk.getBlockState(tp).is(ElisarBlocks.WATER.get())) {
                        chunk.setBlockState(tp, CoralGrove.tipBlock(t.block()), false);
                    }
                }
                return;
            }
        }
        BlockState below = chunk.getBlockState(mp.set(x, floor, z));
        if (!below.is(ElisarBlocks.SEABED.get()) && !below.is(ElisarBlocks.SILT.get()) && !below.is(ElisarBlocks.REEF_STONE.get())
                && !below.is(ElisarBlocks.CORAL_BRAIN.get()) && !below.is(ElisarBlocks.COBBLE.get())) return;
        double r = hash(x, z, 461);
        int pal = palette(x, z);

        // ---- Рев 10: РИФОВЫЕ ХОЛМЫ. Пятна «холмовости» × рифовость дают бугры 1-4 блоков высотой,
        // шапка — цельный тропический коралл своей палитры, на вершине — актиния/куст/веер.
        // Раньше верх дна был шахматкой кубов; теперь это силуэт сада, читаемый издалека.
        if (reef > 0.42) {
            // Рев 10: КУПОЛА, а не плато. Холм растёт к своему центру-пику (сетка 13x13 со сдвигом),
            // высота падает по параболе от пика, поэтому силуэт — округлый бугор со ступенями только на склоне.
            int h = moundHeight(x, z, reef);
            if (h > 0) {
                boolean stacked = true;
                for (int i = 0; i < h; ++i) {
                    if (chunk.getBlockState(mp.set(x, y + i, z)).is(ElisarBlocks.WATER.get())) chunk.setBlockState(mp.set(x, y + i, z), capState(pal), false);
                    else { stacked = false; break; }
                }
                if (stacked) {
                    BlockState topS = crown(pal, hash(x, z, 470));
                    if (chunk.getBlockState(mp.set(x, y + h, z)).is(ElisarBlocks.WATER.get())) chunk.setBlockState(mp.set(x, y + h, z), topS, false);
                }
                return;
            }
            // между холмами — ковёр из НАСТОЯЩИХ блоков коралла (рев 14.26: без 1-блочных 3D-моделей)
            if (r < reef * 0.45) {
                double k = hash(x, z, 462);
                BlockState s = k < 0.30 ? crown(pal, hash(x, z, 470))
                        : k < 0.55 ? st(ElisarBlocks.CORAL_BRAIN) : capState((pal + 1) % 3);
                chunk.setBlockState(mp.set(x, y, z), s, false);
                return;
            }
            // «роща» высоких кораллов: гнездо 3x3, где все колонки заняты оленьими рогами/хлыстами
            int gx = Math.floorDiv(x, 9), gz = Math.floorDiv(z, 9);
            if (hash(gx, gz, 474) > 0.72 && hash(x, z, 475) < 0.40) {     // рев 14.28: рощи реже
                // рев 14.26: «роща» теперь из полных кубов, а не из крестиков-растений
                BlockState thick = hash(x, z, 476) < 0.5 ? st(ElisarBlocks.CORAL_BRAIN) : capState(pal);
                chunk.setBlockState(mp.set(x, y, z), thick, false);
                return;
            }
        }

        // ---- декали на песке и иле: ракушки, морские звёзды, ежи
        if ((below.is(ElisarBlocks.SILT.get()) || below.is(ElisarBlocks.SEABED.get())) && r < 0.14) {
            double k = hash(x, z, 473);
            chunk.setBlockState(mp.set(x, y, z), k < 0.42 ? st(ElisarBlocks.SHELL_BED)
                    : k < 0.74 ? st(ElisarBlocks.SEA_STAR) : st(ElisarBlocks.SEA_URCHIN), false);
            return;
        }

        // морская трава — пятнами по всему дну
        if (vnoise(x * 0.07, z * 0.07, 464) > 0.48 && r < 0.40) {
            chunk.setBlockState(mp.set(x, y, z), st(ElisarBlocks.SEA_GRASS), false);
            return;
        }
        // ламинария — колоннами, до самого света. Рев 12: колонна растёт ТОЛЬКО в воде и только пока
        // над ней вода: раньше она перезаписывала камень и висела в пещерах бирюзовыми штырями.
        if (vnoise(x * 0.05, z * 0.05, 465) > 0.66 && reef < 0.35) {
            int h = 4 + (int) (hash(x, z, 466) * 8);
            int placed = 0;
            for (int i = 0; i < h; ++i) {
                if (!chunk.getBlockState(mp.set(x, y + i, z)).is(ElisarBlocks.WATER.get())) break;
                ++placed;
            }
            if (placed >= 2) {
                for (int i = 0; i < placed; ++i) {
                    BlockState kelp = i == placed - 1 ? st(ElisarBlocks.SEA_KELP_TOP) : st(ElisarBlocks.SEA_KELP);
                    chunk.setBlockState(mp.set(x, y + i, z), kelp, false);
                }
            }
        }
    }

    /** Нарост в сухой пещере (воздушный карман): не waterlogged, иначе блок стал бы источником воды. */
    private static BlockState dryCaveGlow() {
        return ElisarBlocks.CAVE_GLOW.get().defaultBlockState()
                .setValue(ru.aiforge.riftworld.block.SeaPlantBlock.WATERLOGGED, false);
    }

    /** Нарост в затопленной пещере — вода вокруг него остаётся прозрачной. */
    private static BlockState wetCaveGlow() {
        return ElisarBlocks.CAVE_GLOW.get().defaultBlockState()
                .setValue(ru.aiforge.riftworld.block.SeaPlantBlock.WATERLOGGED, true);
    }

    // ------------------------------------------------------------------ убранство пещер
    /** Светящиеся наросты на полу пещер, «сосульки» с потолка, ракушки в нишах. */
    private static void caveDressing(ChunkAccess chunk, BlockPos.MutableBlockPos mp, int x, int z, int floor) {
        // собственный «курсор»: раньше down/up делили один MutableBlockPos, поэтому у состояния «под»
        // в isSolidRender уходила позиция «над» — проверка пола/свода врала.
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        for (int y = BOTTOM + 2; y < floor - 1; ++y) {
            BlockState here = chunk.getBlockState(mp.set(x, y, z));
            if (!here.isAir() && !here.is(ElisarBlocks.WATER.get())) continue;
            boolean wet = here.is(ElisarBlocks.WATER.get());
            BlockState down = chunk.getBlockState(q.set(x, y - 1, z));
            boolean floorSolid = down.isSolidRender(chunk, q);
            BlockState up = chunk.getBlockState(q.set(x, y + 1, z));
            boolean roofSolid = up.isSolidRender(chunk, q);
            double h = hash3(x, y, z, 471);
            // Рев 13: пещеры были практически чёрными — наростов стало больше (13% пола, 7% свода,
            // 1.5% стен), а сам нарост светит на 15. Полость читается целиком, а не «пятном лампы».
            if (floorSolid && h < 0.13) {
                chunk.setBlockState(mp.set(x, y, z), wet ? wetCaveGlow() : dryCaveGlow(), false);
                continue;
            }
            if (roofSolid && h > 0.93) {
                chunk.setBlockState(mp.set(x, y, z), wet ? wetCaveGlow() : dryCaveGlow(), false);
                continue;
            }
            if (h > 0.60 && h < 0.615) {
                // нарост на стене: полость подсвечена по бокам, а не только снизу
                boolean wall = chunk.getBlockState(q.set(x + 1, y, z)).isSolidRender(chunk, q)
                        || chunk.getBlockState(q.set(x - 1, y, z)).isSolidRender(chunk, q)
                        || chunk.getBlockState(q.set(x, y, z + 1)).isSolidRender(chunk, q)
                        || chunk.getBlockState(q.set(x, y, z - 1)).isSolidRender(chunk, q);
                if (wall) chunk.setBlockState(mp.set(x, y, z), wet ? wetCaveGlow() : dryCaveGlow(), false);
            }
        }
    }
}
