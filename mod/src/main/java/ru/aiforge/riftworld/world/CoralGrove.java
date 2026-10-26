package ru.aiforge.riftworld.world;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.registry.ElisarBlocks;

/**
 * Рев 12 «Кораллы-деревья»: коралл — это <b>растение из десятков блоков</b>, а не одиночный крестик.
 *
 * <p>Замечание с прошлого рева: кораллы выглядели как отдельные 3D-кустики, воткнутые в дно по одному,
 * без силуэта. Здесь у каждого коралла есть <b>ствол</b>, <b>ветви</b>, развилки и своя форма — одно
 * растение высотой 3–13 блоков. Стволы и ветви сложены из специальных блоков-«колонн» 6×16×6
 * ({@code coral_stem_*}), поэтому ветвь читается как ветвь, а не как стопка кубов; широкие части
 * (плита стола, шапка) — из блоков-шапок.
 *
 * <p>Ключевая особенность реализации: генератор мира Элисара заполняет блоки <i>по колонкам</i>
 * (см. {@link ElisarSea#column}), поэтому растение нельзя «построить» из центра — оно описано как чистая
 * функция от абсолютных координат: {@link #spanAt(int, int, int)} отвечает, какие блоки коралла стоят
 * в этой колонке. Отсюда два важных следствия:
 * <ul>
 *   <li>ничего не зависит от порядка обхода чанков — на границе чанка растение не рвётся;</li>
 *   <li>растение не нужно «дорисовывать» в соседний чанк — каждый чанк сам знает свою часть.</li>
 * </ul>
 *
 * <p>Шесть силуэтов: оленьи рога (канделябр), стол, хлыст, веер, трубчатая губка и спираль.
 * Верхушки ветвей увенчаны прежними кораллами-крестиками ({@code STAGHORN_CORAL}, {@code SEA_WHIP}…).
 */
public final class CoralGrove {
    private CoralGrove() {
    }

    /** Сетка посадок: одна ячейка 11×11 — максимум одно растение. */
    static final int CELL = 11;

    // силуэты
    public static final int STAGHORN = 0, TABLE = 1, WHIP = 2, FAN = 3, TUBES = 4, SPIRAL = 5;

    /** Участок посадки. */
    private record Site(int x, int z, int kind, int h, int len, int seed) {
    }

    /**
     * Участок колонки: с какого по какой блок над дном стоит коралл (1 — первый блок над дном).
     * {@code stem} — это ствол/ветвь (узкая колонна), иначе широкая шапка/плита.
     */
    public record Span(int from, int to, boolean stem) {
    }

    /** Есть ли растение в этой ячейке сетки (и какое). */
    private static Site siteIn(int gx, int gz) {
        int cx = gx * CELL + CELL / 2, cz = gz * CELL + CELL / 2;
        double reef = ElisarSea.reefness(cx, cz);
        // густота зависит от «рифовости» места: в плотном саду растения почти в каждой второй ячейке
        if (ElisarSea.hash(gx, gz, 601) > 0.10 + reef * 0.42) return null;
        // на высоких холмах растений нет: там уже стоят шапки куполов
        if (ElisarSea.moundHeightAt(cx, cz) > 1) return null;
        int sx = gx * CELL + 2 + (int) (ElisarSea.hash(gx, gz, 602) * (CELL - 4));
        int sz = gz * CELL + 2 + (int) (ElisarSea.hash(gx, gz, 603) * (CELL - 4));
        int seed = (int) (ElisarSea.hash(gx, gz, 604) * 1_000_003);
        int kind = (int) (ElisarSea.hash(gx, gz, 605) * 6) % 6;
        int h = 4 + (int) (ElisarSea.hash(gx, gz, 606) * 7);        // 4..10
        int len = 2 + (int) (ElisarSea.hash(gx, gz, 607) * 4);      // 2..5
        return new Site(sx, sz, kind, h, len, seed);
    }

    /**
     * Какие блоки коралла стоят в этой колонке (соседние 3×3 ячейки — растения могут нависать из соседней).
     *
     * @param room сколько блоков свободно над дном
     * @return {@code null}, если в колонке ничего нет
     */
    public static Span spanAt(int x, int z, int room) {
        if (room < 3) return null;
        int gx = Math.floorDiv(x, CELL), gz = Math.floorDiv(z, CELL);
        Span best = null;
        for (int ax = -1; ax <= 1; ++ax) {
            for (int az = -1; az <= 1; ++az) {
                Site s = siteIn(gx + ax, gz + az);
                if (s == null) continue;
                Span sp = columnOf(s, x - s.x, z - s.z, room);
                if (sp == null) continue;
                if (best == null || sp.to() > best.to()) best = sp;
            }
        }
        return best;
    }

    /** Широкая часть растения (шапка/плита) — блок палитры. */
    public static BlockState body(int x, int z) {
        return ElisarSea.capState(ElisarSea.palette(x, z));
    }

    /** Ствол и ветви — узкая колонна 6×16×6, а не полный куб. */
    public static BlockState stem(int x, int z) {
        int p = ElisarSea.palette(x, z);
        return st(p == 0 ? ElisarBlocks.CORAL_STEM_PINK : p == 1 ? ElisarBlocks.CORAL_STEM_TEAL : ElisarBlocks.CORAL_STEM_VIOLET);
    }

    /**
     * Украшение на верхушке: 0 — нет, иначе блок-крестик предыдущих ревов. Ставится только там,
     * где растение действительно заканчивается, — на концах ветвей, хлыстах и на плите стола.
     */
    public static BlockState tipAt(int x, int z, int top) {
        int gx = Math.floorDiv(x, CELL), gz = Math.floorDiv(z, CELL);
        for (int ax = -1; ax <= 1; ++ax) {
            for (int az = -1; az <= 1; ++az) {
                Site s = siteIn(gx + ax, gz + az);
                if (s == null) continue;
                BlockState tip = tipOf(s, x - s.x, z - s.z, top);
                if (tip != null) return tip;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ силуэты
    /** Геометрия одного растения. {@code dx,dz} — смещение от центра участка. */
    private static Span columnOf(Site s, int dx, int dz, int room) {
        return switch (s.kind) {
            case STAGHORN -> staghorn(s, dx, dz, room);
            case TABLE -> table(s, dx, dz, room);
            case WHIP -> whip(s, dx, dz, room, false);
            case FAN -> fan(s, dx, dz, room);
            case TUBES -> tubes(s, dx, dz, room);
            default -> whip(s, dx, dz, room, true);
        };
    }

    /** Оленьи рога: ствол, 3–4 диагональные ветви и отростки второго порядка на них. */
    private static Span staghorn(Site s, int dx, int dz, int room) {
        int trunk = Math.max(2, Math.min(2 + (s.seed & 3), room - 2));           // 2..5
        if (dx == 0 && dz == 0) return new Span(1, trunk, true);
        for (int a = 0; a < 4; ++a) {
            int ux = (a & 1) == 0 ? 1 : -1;
            int uz = (a & 2) == 0 ? 1 : -1;
            if (ElisarSea.hash(s.seed, a, 611) < 0.18) continue;                 // не все четыре ветви живы
            int len = Math.min(2 + (int) (ElisarSea.hash(s.seed, a, 612) * 4), Math.max(1, room - trunk - 1));
            for (int t = 1; t <= len; ++t) {
                int y = Math.min(trunk + t, room);
                if (dx == ux * t && dz == uz * t) return new Span(y, y, true);
                // отростки второго порядка
                if (dx == ux * t + ux && dz == uz * t) return new Span(y, y, true);
                if (dx == ux * t && dz == uz * t + uz) return new Span(y, y, true);
                // «рожки» на самом конце ветви
                if (t == len && ((dx == ux * t && dz == 0) || (dx == 0 && dz == uz * t))) {
                    return new Span(Math.min(y + 1, room), Math.min(y + 1, room), true);
                }
            }
        }
        return null;
    }

    /** Стол: тонкая ножка и широкая плита сверху с рваным краем. */
    private static Span table(Site s, int dx, int dz, int room) {
        int trunk = Math.max(2, Math.min(3 + (s.seed % 3), room - 1));
        int r = 2 + (int) (ElisarSea.hash(s.seed, 1, 621) * 3);                  // радиус плиты 2..4
        double d = Math.hypot(dx, dz);
        double rag = r + (ElisarSea.hash(s.x + dx, s.z + dz, 622) - 0.5) * 1.5;  // рваный край
        if (d > rag) return null;
        if (Math.abs(dx) <= 0 && Math.abs(dz) <= 0) return new Span(1, trunk, true);      // ножка
        if (d <= rag - 0.8) return new Span(trunk, Math.min(trunk + 1, room), false);     // плотная середина
        return new Span(trunk, trunk, false);                                             // тонкий край
    }

    /** Хлыст: одна извилистая колонна (spiral — двойная спираль вокруг пустого центра). */
    private static Span whip(Site s, int dx, int dz, int room, boolean spiral) {
        int h = Math.min(s.h + 2, Math.min(room, 13));
        double phase = ElisarSea.hash(s.seed, 0, 631) * Math.PI * 2;
        double freq = 0.35 + ElisarSea.hash(s.seed, 1, 632) * 0.30;
        double amp = spiral ? 1.3 : 0.8 + ElisarSea.hash(s.seed, 2, 633) * 1.0;
        int best = -1;
        for (int t = 0; t < h; ++t) {
            double a = phase + t * freq * (spiral ? 1.5 : 1.0);
            double grow = spiral ? 0.5 + t * 0.12 : 1.0;
            int px = (int) Math.round(Math.cos(a) * amp * grow);
            int pz = (int) Math.round(Math.sin(a) * amp * grow);
            if (px == dx && pz == dz) best = Math.max(best, t + 1);
        }
        if (best < 0) return null;
        return spiral ? new Span(best, best, true) : new Span(1, best, true);
    }

    /**
     * Веер: решётчатая пластина в вертикальной плоскости. Раньше здесь был сплошной «парус» на всю
     * ширину — на скриншотах он читался как розовая стена. Теперь веер собран из отдельных «рёбер»
     * (каждое второе ребро пропущено + дырки), с округлым верхним краем.
     */
    private static Span fan(Site s, int dx, int dz, int room) {
        double angle = ElisarSea.hash(s.seed, 0, 641) * Math.PI;                 // куда смотрит плоскость
        double ca = Math.cos(angle), sa = Math.sin(angle);
        double u = dx * ca + dz * sa;                                            // вдоль веера
        double v = -dx * sa + dz * ca;                                           // поперёк (толщина)
        int w = Math.min(2 + s.len, 5);                                          // полуширина 4..5
        int h = Math.min(s.h, Math.min(room, 8));
        if (Math.abs(v) > 0.6 || Math.abs(u) > w) return null;
        int rib = (int) Math.round(Math.abs(u) * 2);
        if (rib % 2 == 1) return null;                                            // рёбра через клетку
        if (ElisarSea.hash(s.x + dx, s.z + dz, 642) < 0.22) return null;          // дырки в пластине
        int top = h - (int) Math.round(u * u / (1.4 * Math.max(2, w)));           // округлый край
        if (top < 2) return null;
        return new Span(1, Math.min(top, room), true);
    }

    /** Трубчатая губка: 3–5 трубок разной высоты, стоящих рядом. */
    private static Span tubes(Site s, int dx, int dz, int room) {
        int n = 3 + (int) (ElisarSea.hash(s.seed, 0, 651) * 3);
        for (int i = 0; i < n; ++i) {
            int ox = (int) Math.round((ElisarSea.hash(s.seed, i, 652) - 0.5) * 4.2);
            int oz = (int) Math.round((ElisarSea.hash(s.seed, i, 653) - 0.5) * 4.2);
            if (ox != dx || oz != dz) continue;
            int h = Math.min(2 + (int) (ElisarSea.hash(s.seed, i, 654) * 5) + (int) (ElisarSea.hash(s.seed, 0, 655) * 2), room);
            return new Span(1, h, true);
        }
        return null;
    }

    /** Украшение на самом верху растения. {@code top} — высота верхнего блока колонки. */
    private static BlockState tipOf(Site s, int dx, int dz, int top) {
        Span sp = columnOf(s, dx, dz, 64);
        if (sp == null || sp.to() != top) return null;
        return switch (s.kind) {
            case STAGHORN -> (dx != 0 || dz != 0) ? st(ElisarBlocks.STAGHORN_CORAL) : null;
            case TABLE -> (dx == 0 && dz == 0) ? st(ElisarBlocks.ANEMONE) : null;
            case WHIP -> st(ElisarBlocks.SEA_WHIP);
            case FAN -> st(ElisarBlocks.RIBBON_CORAL);
            case TUBES -> st(ElisarBlocks.TUBE_WORMS);
            default -> st(ElisarBlocks.FEATHER_CORAL);
        };
    }

    private static BlockState st(RegistryObject<? extends Block> b) {
        return b.get().defaultBlockState().setValue(ru.aiforge.riftworld.block.SeaPlantBlock.WATERLOGGED, true);
    }
}
