package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.registry.ElisarBlocks;

/**
 * Рев 13 — <b>кораллы-деревья</b>: настоящее подводное дерево, а не столбик с шапкой.
 *
 * <p>Замечание заказчика: «пусть кораллы будут прям как подводные деревья… у них есть красивые
 * извилины». В прошлой партии растение собиралось из вертикальных колонн: ствол строго по
 * одной линии, ветви — ступеньки по диагонали. На кадрах это читалось как «трубы с шапками».
 *
 * <p>Что теперь:
 * <ul>
 *   <li><b>Извилистый ствол.</b> Угол наклона медленно поворачивается от яруса к ярусу, поэтому
 *       ствол змеится (S-образно), а не стоит по линейке.</li>
 *   <li><b>Кривые ветви.</b> Каждая ветвь — дуга: направление всё время подкручивается, пока
 *       ветвь лезет вверх. Отсюда «извилины» и живой силуэт.</li>
 *   <li><b>Крона на концах ветвей, а не на макушке.</b> У столовых кораллов на ветвях
 *       раскрываются широкие <b>плиты</b> в 2 яруса, у рогов — развилки с полипами на кончиках,
 *       у вееров — решётчатая пластина, у хлыстов — сужающийся кончик, у губок — гроздь трубок.</li>
 *   <li><b>Несколько отрезков в одной колонке.</b> Ветвь проходит над ветвью, поэтому крона не
 *       «сплющивается» и дерево не превращается в сплошную колонну.</li>
 *   <li><b>Жёсткая привязка к полу своей посадки.</b> Высоты считаются от дна колонки-хозяина,
 *       а не от дна текущей колонки: иначе на неровном склоне дерево «сдвигалось» по ярусам и
 *       распадалось на плавающие куски.</li>
 * </ul>
 *
 * <p>Генератор мира Элисара заполняет блоки по колонкам, поэтому растение описано как чистая
 * функция от абсолютных координат ({@link #spansAt}, {@link #tipsAt}). Чертежи считаются один раз
 * и кешируются по ячейке сетки — генерация чанков не тормозит.
 */
public final class CoralGrove {
    private CoralGrove() {
    }

    /** Сетка посадок: одна ячейка 11×11 — максимум одно растение. */
    static final int CELL = 11;

    // силуэты
    public static final int STAGHORN = 0, TABLE = 1, WHIP = 2, FAN = 3, TUBES = 4, SPIRAL = 5;

    /** Типы блоков в чертеже: узкий ствол/ветвь и широкая плита/шапка. */
    private static final int STEM = 0, PLATE = 1;

    /** Насколько глубоко под поверхностью должен оставаться верх растения. */
    private static final int HEADROOM = 3;

    /** Участок посадки (уже с посчитанным дном). */
    private record Site(int x, int z, int floor, int kind, int h, int seed) {
    }

    /**
     * Отрезок колонки в <b>абсолютных</b> координатах Y: коралл занимает блоки {@code from..to}.
     * {@code stem} — узкая колонна (ствол/ветвь), иначе широкая плита/шапка.
     */
    public record Span(int from, int to, boolean stem) {
    }

    /** Украшение-полип на верхушке: абсолютный {@code y} и индекс блока. */
    public record Tip(int y, int block) {
    }

    /** Готовый чертёж растения: колонки (смещение от центра → отрезки) и украшения. */
    private record Blueprint(Site site, Map<Long, List<int[]>> columns, Map<Long, List<Tip>> tips) {
        static final Blueprint EMPTY = new Blueprint(null, Map.of(), Map.of());
    }

    private static final Map<Long, Blueprint> CACHE = new ConcurrentHashMap<>();

    private static long key(int gx, int gz) {
        return ((long) gx << 32) ^ (gz & 0xFFFFFFFFL);
    }

    private static long colKey(int dx, int dz) {
        return ((long) (dx + 16) << 8) | (dz + 16);
    }

    // ------------------------------------------------------------------ посадки
    private static double rnd(int seed, int a, int b) {
        return ElisarSea.hash(seed, a, b);
    }

    /** Есть ли в этой ячейке растение. Детерминировано: одни и те же координаты — один и тот же сад. */
    private static Site siteIn(int gx, int gz) {
        int cx = gx * CELL + CELL / 2, cz = gz * CELL + CELL / 2;
        double reef = ElisarSea.reefness(cx, cz);
        // густота зависит от «рифовости» места: в плотном саду растение почти в каждой второй ячейке
        if (ElisarSea.hash(gx, gz, 601) > 0.10 + reef * 0.30) return null;
        // на высоких холмах растений нет: там уже стоят шапки куполов
        if (ElisarSea.moundHeightAt(cx, cz) > 1) return null;
        int sx = gx * CELL + 2 + (int) (ElisarSea.hash(gx, gz, 602) * (CELL - 4));
        int sz = gz * CELL + 2 + (int) (ElisarSea.hash(gx, gz, 603) * (CELL - 4));
        int floor = ElisarChunkGenerator.oceanFloor(CityPlan.get(), sx, sz);
        int sea = CityPlan.get().ocean;
        if (sea - HEADROOM - (floor + 1) < 5) return null;          // слишком мелко для дерева
        int seed = (int) (ElisarSea.hash(gx, gz, 604) * 1_000_003);
        int room = sea - HEADROOM - floor - 1;
        // распределение силуэтов: «рога» и веера — основа рощи, одиночные хлысты — редкость
        double k = ElisarSea.hash(gx, gz, 605);
        int kind = k < 0.30 ? STAGHORN : k < 0.44 ? TABLE : k < 0.62 ? FAN : k < 0.78 ? WHIP : k < 0.90 ? TUBES : SPIRAL;
        int h = 6 + (int) (ElisarSea.hash(gx, gz, 606) * 7);        // 6..12
        h = Math.min(h, Math.max(5, room - 2));
        return new Site(sx, sz, floor, kind, h, seed);
    }

    // ------------------------------------------------------------------ построение чертежа
    private static void put(Map<Long, List<int[]>> cols, int dx, int y, int dz, int type) {
        if (y <= 0) return;
        List<int[]> list = cols.computeIfAbsent(colKey(dx, dz), k -> new ArrayList<>());
        for (int[] e : list) {
            if (e[2] != type) continue;
            if (y >= e[0] && y <= e[1]) return;                 // уже есть
            if (y == e[1] + 1) { e[1] = y; return; }            // приклеиваем сверху
            if (y == e[0] - 1) { e[0] = y; return; }            // приклеиваем снизу
        }
        list.add(new int[]{y, y, type});
    }

    private static void tip(Map<Long, List<Tip>> tips, int dx, int y, int dz, int block) {
        if (y <= 0) return;
        tips.computeIfAbsent(colKey(dx, dz), k -> new ArrayList<>()).add(new Tip(y, block));
    }

    /**
     * Плита столового коралла: <b>тонкий</b> рваный диск с маленьким вторым ярусом сверху.
     *
     * <p>Рев 13.6: первые плиты были радиусом 2.6–4 блока и толщиной в 2–3 яруса — на кадрах живой игры
     * это читалось не как стол, а как розовая глыба, за которой не видно ствола. Теперь плита —
     * один ряд блоков (радиус 1.6–2.2) плюс «ступенька» радиусом ~1.2 сверху: силуэт плоский и лёгкий.
     */
    private static void plate(Map<Long, List<int[]>> cols, Map<Long, List<Tip>> tips,
                              int cx, int y, int cz, double radius, int seed) {
        int r = (int) Math.ceil(radius);
        for (int dx = -r; dx <= r; ++dx) {
            for (int dz = -r; dz <= r; ++dz) {
                double d = Math.hypot(dx, dz);
                double edge = radius + (rnd(seed, dx + 32, dz + 32) - 0.5) * 0.9;    // рваный край
                if (d > edge) continue;
                if (d > edge - 0.9 && rnd(seed + 7, dx + 32, dz + 32) < 0.30) continue;   // дырки по краю
                put(cols, cx + dx, y, cz + dz, PLATE);
                if (d < radius - 0.6) {
                    put(cols, cx + dx, y + 1, cz + dz, PLATE);                      // ступенька сверху
                } else if (rnd(seed + 13, dx + 64, dz + 64) < 0.45) {
                    // Рев 13.18: чистый край плиты читался на кадрах как розовый куб. По кромке
                    // ставим полипы (рога/анемоны/лента) — силуэт стола становится «живым», а не
                    // гранёным, и издалека видно, что это коралл, а не блок-заглушка.
                    tip(tips, cx + dx, y + 1, cz + dz, 0);
                }
            }
        }
    }

    private static Blueprint build(Site s) {
        Map<Long, List<int[]>> cols = new HashMap<>();
        Map<Long, List<Tip>> tips = new HashMap<>();
        int seed = s.seed;
        int base = s.floor + 1;                                     // первый блок над дном

        // ---------------------------------------------------------------- извилистый ствол
        int h = Math.max(5, s.h);
        int[][] trunk = new int[h + 2][];
        double ang = rnd(seed, 1, 701) * Math.PI * 2;
        double sway = 0.35 + rnd(seed, 2, 702) * 0.50;
        double px = 0, pz = 0;
        for (int y = 1; y <= h; ++y) {
            // угол наклона поворачивается медленно и с разной скоростью — ствол змеится
            ang += (rnd(seed, y, 703) - 0.5) * 1.15;
            double pull = 1.0 - (double) y / (h + 3);            // у основания качает сильнее
            px += Math.cos(ang) * sway * pull;
            pz += Math.sin(ang) * sway * pull;
            int ix = (int) Math.round(px), iz = (int) Math.round(pz);
            trunk[y] = new int[]{ix, iz};
            put(cols, ix, base + y - 1, iz, STEM);
            // комель: первые 3 блока чуть толще (второй столбик рядом)
            if (y <= 2) {
                // комель: крест из трёх столбиков — низ читается как ствол, а не как палка
                int bx = (int) Math.round(Math.cos(ang + Math.PI / 2) * 1.0);
                int bz = (int) Math.round(Math.sin(ang + Math.PI / 2) * 1.0);
                if (bx != 0 || bz != 0) put(cols, ix + bx, base + y - 1, iz + bz, STEM);
                if (y == 1) put(cols, ix, base, iz, PLATE);
            }
        }

        // ---------------------------------------------------------------- ветви с развилками
        // Рев 13.1: одна короткая ветвь с шапкой на конце давала «палку с зонтиком».
        // Теперь каждая ветвь длинная и на её конце раскрываются 1–2 развилки следующего порядка,
        // поэтому крона получается широкой и многоярусной, как у настоящего дерева.
        int branches = s.kind == TABLE ? 4 : s.kind == TUBES ? 3 : 3;
        for (int b = 0; b < branches; ++b) {
            int y0 = 2 + (int) (h * (0.42 + 0.45 * rnd(seed, b, 705)));   // ветви ближе к кроне
            y0 = Math.min(y0, Math.max(1, h - 1));
            int[] p = trunk[y0];
            if (p == null) continue;
            double a = b * Math.PI * 2 / branches + rnd(seed, b, 706) * 1.1;
            double curve = (rnd(seed, b, 707) - 0.5) * 1.1;
            int len = 3 + (int) (rnd(seed, b, 708) * 3);          // 3..5
            int[] end = limb(cols, p[0], base + y0 - 1, p[1], a, curve, len, 0.95, 0.88);
            double endAng = end[3] / 1000.0;
            // развилки: 2 для широких силуэтов, 1 для хлыстов
            int forks = 1;                                       // одна развилка: ветвь читается, не «мочалка»
            for (int f = 0; f < forks; ++f) {
                double fa = endAng + (f == 0 ? -0.75 : 0.75) + (rnd(seed, b * 7 + f, 709) - 0.5) * 0.7;
                int flen = 2 + (int) (rnd(seed, b * 7 + f, 710) * 3);
                int[] fe = limb(cols, end[0], end[1], end[2], fa, (rnd(seed, f, 722) - 0.5) * 0.9,
                        flen, 0.95, 0.95);
                crown(cols, tips, s, fe[0], fe[1], fe[2], seed + b * 31 + f * 131);
            }
            crown(cols, tips, s, end[0], end[1], end[2], seed + b * 31 + 977);
        }
        // макушка ствола
        int[] top = trunk[h];
        if (top != null) crown(cols, tips, s, top[0], base + h - 1, top[1], seed + 991);
        return new Blueprint(s, cols, tips);
    }

    /**
     * Кривая ветвь: угол всё время подкручивается ({@code curve}), пока ветвь лезет вверх.
     * Возвращает {@code {x, y, z, угол}} конца — чтобы от него отходили следующие развилки.
     */
    private static int[] limb(Map<Long, List<int[]>> cols, int x, int y, int z,
                              double ang, double curve, int len, double step, double rise) {
        double bx = x, bz = z;
        for (int t = 1; t <= len; ++t) {
            ang += curve;
            bx += Math.cos(ang) * step;
            bz += Math.sin(ang) * step;
            put(cols, (int) Math.round(bx), y + (int) Math.round(t * rise), (int) Math.round(bz), STEM);
        }
        return new int[]{(int) Math.round(bx), y + (int) Math.round(len * rise), (int) Math.round(bz),
                (int) Math.round(ang * 1000)};
    }

    /** Крона на конце ветви — своя у каждого силуэта. */
    private static void crown(Map<Long, List<int[]>> cols, Map<Long, List<Tip>> tips,
                              Site s, int x, int y, int z, int seed) {
        switch (s.kind) {
            case STAGHORN -> {
                // «оленьи рога»: 2–3 коротких отростка, расходящихся вилкой
                int n = 2 + (int) (rnd(seed, 1, 711) * 2);
                for (int i = 0; i < n; ++i) {
                    double a = rnd(seed, i, 712) * Math.PI * 2;
                    int dx = (int) Math.round(Math.cos(a) * 1.6);
                    int dz = (int) Math.round(Math.sin(a) * 1.6);
                    for (int t = 1; t <= 3; ++t) {
                        put(cols, x + dx * t / 2, y + t, z + dz * t / 2, STEM);
                    }
                    tip(tips, x + dx, y + 3, z + dz, 0);
                }
            }
            case TABLE -> {
                // стол: широкая плита над ветвью
                plate(cols, tips, x, y + 1, z, 1.6 + rnd(seed, 2, 713) * 0.6, seed);
                tip(tips, x, y + 3, z, 1);
            }
            case FAN -> {
                // веер: решётка в вертикальной плоскости, с округлым верхом
                double a = rnd(seed, 3, 714) * Math.PI;
                double ca = Math.cos(a), sa = Math.sin(a);
                for (int u = -3; u <= 3; ++u) {
                    if (rnd(seed, u + 8, 715) < 0.22) continue;   // живые просветы
                    int topY = 4 - Math.abs(u) / 2;
                    int dx = (int) Math.round(u * ca);
                    int dz = (int) Math.round(u * sa);
                    for (int v = 0; v <= topY; ++v) put(cols, x + dx, y + v, z + dz, STEM);
                    tip(tips, x + dx, y + topY + 1, z + dz, 2);
                }
            }
            case WHIP, SPIRAL -> {
                // хлыст: продолжает извиваться и сужается
                double a = rnd(seed, 4, 716) * Math.PI * 2;
                int px = x, pz = z;
                for (int t = 1; t <= 4; ++t) {
                    a += 0.7 + rnd(seed, t, 717) * 0.6;
                    px += (int) Math.round(Math.cos(a));
                    pz += (int) Math.round(Math.sin(a));
                    put(cols, px, y + t, pz, STEM);
                }
                tip(tips, px, y + 4, pz, 3);
            }
            default -> {
                // трубчатая губка: гроздь вертикальных трубок разной высоты
                int n = 3 + (int) (rnd(seed, 5, 718) * 3);
                for (int i = 0; i < n; ++i) {
                    int dx = (int) Math.round((rnd(seed, i, 719) - 0.5) * 4.0);
                    int dz = (int) Math.round((rnd(seed, i, 720) - 0.5) * 4.0);
                    int hh = 2 + (int) (rnd(seed, i, 721) * 4);
                    for (int t = 1; t <= hh; ++t) put(cols, x + dx, y + t, z + dz, STEM);
                    tip(tips, x + dx, y + hh, z + dz, 4);
                }
            }
        }
    }

    private static Blueprint blueprint(int gx, int gz) {
        long k = key(gx, gz);
        Blueprint bp = CACHE.get(k);
        if (bp != null) return bp;
        Site s = siteIn(gx, gz);
        bp = s == null ? Blueprint.EMPTY : build(s);
        if (CACHE.size() > 60_000) CACHE.clear();                   // долгая игра не течёт памятью
        CACHE.putIfAbsent(k, bp);
        return bp;
    }

    private static List<int[]> columnOf(Blueprint bp, int x, int z) {
        if (bp.site() == null) return null;
        return bp.columns().get(colKey(x - bp.site().x, z - bp.site().z));
    }

    /** Отрезки коралла в этой колонке (соседние 3×3 ячейки — растение может нависать из соседней). */
    public static List<Span> spansAt(int x, int z, int sea) {
        int maxY = sea - HEADROOM;
        int gx = Math.floorDiv(x, CELL), gz = Math.floorDiv(z, CELL);
        List<Span> out = null;
        for (int ax = -1; ax <= 1; ++ax) {
            for (int az = -1; az <= 1; ++az) {
                List<int[]> list = columnOf(blueprint(gx + ax, gz + az), x, z);
                if (list == null) continue;
                for (int[] e : list) {
                    int from = Math.max(e[0], -60), to = Math.min(e[1], maxY);
                    if (from > to) continue;
                    if (out == null) out = new ArrayList<>(2);
                    out.add(new Span(from, to, e[2] == STEM));
                }
            }
        }
        return out == null ? List.of() : out;
    }

    /** Украшения на верхушках в этой колонке. */
    public static List<Tip> tipsAt(int x, int z, int sea) {
        int maxY = sea - HEADROOM;
        int gx = Math.floorDiv(x, CELL), gz = Math.floorDiv(z, CELL);
        List<Tip> out = null;
        for (int ax = -1; ax <= 1; ++ax) {
            for (int az = -1; az <= 1; ++az) {
                Blueprint bp = blueprint(gx + ax, gz + az);
                if (bp.site() == null) continue;
                List<Tip> list = bp.tips().get(colKey(x - bp.site().x, z - bp.site().z));
                if (list == null) continue;
                for (Tip t : list) {
                    if (t.y() > maxY) continue;
                    if (out == null) out = new ArrayList<>(2);
                    out.add(t);
                }
            }
        }
        return out == null ? List.of() : out;
    }

    /** Широкая часть растения (плита/шапка) — блок палитры. */
    public static BlockState body(int x, int z) {
        return ElisarSea.capState(ElisarSea.palette(x, z));
    }

    /**
     * Ствол и ветви — узкая колонна 6×16×6, а не полный куб. У этих блоков НЕТ свойства
     * {@code waterlogged} (это обычный Block), поэтому состояние берём как есть: попытка выставить
     * waterlogged здесь валила генерацию чанка (IllegalArgumentException «Cannot set property …»).
     */
    public static BlockState stem(int x, int z) {
        int p = ElisarSea.palette(x, z);
        RegistryObject<? extends Block> b = p == 0 ? ElisarBlocks.CORAL_STEM_PINK
                : p == 1 ? ElisarBlocks.CORAL_STEM_TEAL : ElisarBlocks.CORAL_STEM_VIOLET;
        return b.get().defaultBlockState();
    }

    /** Блок-полип по индексу из {@link Tip}. */
    public static BlockState tipBlock(int block) {
        RegistryObject<? extends Block> b = switch (block) {
            case 0 -> ElisarBlocks.STAGHORN_CORAL;
            case 1 -> ElisarBlocks.ANEMONE;
            case 2 -> ElisarBlocks.RIBBON_CORAL;
            case 3 -> ElisarBlocks.SEA_WHIP;
            default -> ElisarBlocks.TUBE_WORMS;
        };
        BlockState st = b.get().defaultBlockState();
        return st.hasProperty(ru.aiforge.riftworld.block.SeaPlantBlock.WATERLOGGED)
                ? st.setValue(ru.aiforge.riftworld.block.SeaPlantBlock.WATERLOGGED, true) : st;
    }
}
