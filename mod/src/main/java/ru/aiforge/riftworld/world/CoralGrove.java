package ru.aiforge.riftworld.world;

import net.minecraft.world.level.block.state.BlockState;
import ru.aiforge.riftworld.registry.ElisarBlocks;

/**
 * Рев 12 «Кораллы-деревья»: коралл — это <b>растение из десятков блоков</b>, а не одиночный крестик.
 *
 * <p>Замечание с прошлого рева: кораллы выглядели как отдельные 3D-кустики, воткнутые в дно по одному,
 * без силуэта. Здесь у каждого коралла есть ствол, ветви, развилки и своя форма, и всё это —
 * <b>одно растение</b> высотой 3–14 блоков.
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
 * <p>Шесть силуэтов: оленьи рога (свечной канделябр), стол, хлыст, веер, трубчатая губка и спираль.
 * Верхушки ветвей увенчаны прежними кораллами-крестиками ({@code STAGHORN_CORAL}, {@code SEA_WHIP}…),
 * чтобы новые растения были «живыми» и узнаваемыми.
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

    /** Участок колонки: с какого по какой блок над дном стоит коралл (1 — первый блок над дном). */
    public record Span(int from, int to) {
    }

    /** Есть ли растение в этой ячейке сетки (и какое). */
    private static Site siteIn(int gx, int gz) {
        int cx = gx * CELL + CELL / 2, cz = gz * CELL + CELL / 2;
        double reef = ElisarSea.reefness(cx, cz);
        // густота зависит от «рифовости» места: в плотном саду растения почти в каждой ячейке
        if (ElisarSea.hash(gx, gz, 601) > 0.16 + reef * 0.72) return null;
        // на высоких холмах растений нет: там уже стоят шапки куполов
        if (ElisarSea.moundHeightAt(cx, cz) > 1) return null;
        int sx = gx * CELL + 2 + (int) (ElisarSea.hash(gx, gz, 602) * (CELL - 4));
        int sz = gz * CELL + 2 + (int) (ElisarSea.hash(gx, gz, 603) * (CELL - 4));
        int seed = (int) (ElisarSea.hash(gx, gz, 604) * 1_000_003);
        int kind = (int) (ElisarSea.hash(gx, gz, 605) * 6) % 6;
        int h = 4 + (int) (ElisarSea.hash(gx, gz, 606) * 8);        // 4..11
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
        int from = Integer.MAX_VALUE, to = -1;
        for (int ax = -1; ax <= 1; ++ax) {
            for (int az = -1; az <= 1; ++az) {
                Site s = siteIn(gx + ax, gz + az);
                if (s == null) continue;
                Span sp = columnOf(s, x - s.x, z - s.z, room);
                if (sp == null) continue;
                from = Math.min(from, sp.from());
                to = Math.max(to, sp.to());
            }
        }
        return to < 0 ? null : new Span(from, to);
    }

    /** Блок тела растения (шапка своей палитры). */
    public static BlockState body(int x, int z) {
        return ElisarSea.capState(ElisarSea.palette(x, z));
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
    /**
     * Геометрия одного растения. {@code dx,dz} — смещение от центра участка.
     * Возвращает участок блоков над дном либо {@code null}.
     */
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

    /** Оленьи рога: короткий ствол, четыре диагональные ветви, у каждой — отростки вбок. */
    private static Span staghorn(Site s, int dx, int dz, int room) {
        int trunk = Math.min(3 + (s.seed & 3), room - 2);
        if (dx == 0 && dz == 0) return new Span(1, Math.min(trunk, room));
        for (int a = 0; a < 4; ++a) {
            int ux = (a & 1) == 0 ? 1 : -1;
            int uz = (a & 2) == 0 ? 1 : -1;
            if (ElisarSea.hash(s.seed, a, 611) < 0.15) continue;                  // не все четыре ветви живы
            int len = Math.min(2 + (int) (ElisarSea.hash(s.seed, a, 612) * 4), Math.max(1, room - trunk - 1));
            for (int t = 1; t <= len; ++t) {
                int y = Math.min(trunk + t, room);
                if (dx == ux * t && dz == uz * t) return new Span(1, y);
                // отростки второго порядка: вбок от диагональной ветви
                if (dx == ux * t + ux && dz == uz * t) return new Span(y, y);
                if (dx == ux * t && dz == uz * t + uz) return new Span(y, y);
                // и «рожки» на самом конце
                if (t == len && ((dx == ux * t && dz == 0) || (dx == 0 && dz == uz * t))) {
                    return new Span(y, Math.min(y + 1, room));
                }
            }
        }
        return null;
    }

    /** Стол: тонкая ножка и широкая плита сверху с рваным краем. */
    private static Span table(Site s, int dx, int dz, int room) {
        int trunk = Math.max(2, Math.min(3 + (s.seed % 3), room - 1));
        int r = 3 + (int) (ElisarSea.hash(s.seed, 1, 621) * 3);                   // радиус плиты 3..5
        double d = Math.hypot(dx, dz);
        double rag = r + (ElisarSea.hash(s.x + dx, s.z + dz, 622) - 0.5) * 1.7;   // рваный край
        if (d > rag) return null;
        if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) return new Span(1, trunk);    // ножка
        if (d <= rag - 1.0) return new Span(trunk, Math.min(trunk + 1, room));    // плотная середина плиты
        return new Span(trunk, trunk);                                            // тонкий край
    }

    /** Хлыст: одна извилистая колонна (spiral — двойная спираль вокруг пустого центра). */
    private static Span whip(Site s, int dx, int dz, int room, boolean spiral) {
        int h = Math.min(s.h + 2, room);
        double phase = ElisarSea.hash(s.seed, 0, 631) * Math.PI * 2;
        double freq = 0.35 + ElisarSea.hash(s.seed, 1, 632) * 0.35;
        double amp = spiral ? 1.4 : 0.9 + ElisarSea.hash(s.seed, 2, 633) * 1.2;
        int best = -1;
        for (int t = 0; t < h; ++t) {
            double a = phase + t * freq * (spiral ? 1.6 : 1.0);
            int px = (int) Math.round(Math.cos(a) * amp * (spiral ? (0.5 + t * 0.10) : 1.0));
            int pz = (int) Math.round(Math.sin(a) * amp * (spiral ? (0.5 + t * 0.10) : 1.0));
            if (px == dx && pz == dz) best = Math.max(best, t + 1);
        }
        if (best < 0) return null;
        return spiral ? new Span(best, best) : new Span(1, best);
    }

    /** Веер: тонкая пластина в вертикальной плоскости, «растёт» от узкого основания к широкому краю. */
    private static Span fan(Site s, int dx, int dz, int room) {
        double angle = ElisarSea.hash(s.seed, 0, 641) * Math.PI;                 // куда смотрит плоскость
        double ca = Math.cos(angle), sa = Math.sin(angle);
        double u = dx * ca + dz * sa;                                            // вдоль веера
        double v = -dx * sa + dz * ca;                                           // поперёк (толщина)
        int w = Math.min(2 + s.len, 6);
        if (Math.abs(v) > 0.6 || Math.abs(u) > w) return null;
        int h = Math.min(s.h + 2, room);
        double top = h - Math.abs(u) * 0.85;                                     // край ниже середины
        int t = (int) Math.round(top);
        if (t < 1) return null;
        if (Math.abs(u) > w - 1.2 && ElisarSea.hash(s.x + (int) u, s.z + (int) v, 642) < 0.35) return null;
        return new Span(1, t);
    }

    /** Трубчатая губка: 4–6 трубок разной высоты, стоящих рядом. */
    private static Span tubes(Site s, int dx, int dz, int room) {
        int n = 4 + (int) (ElisarSea.hash(s.seed, 0, 651) * 3);
        for (int i = 0; i < n; ++i) {
            int ox = (int) Math.round((ElisarSea.hash(s.seed, i, 652) - 0.5) * 4.2);
            int oz = (int) Math.round((ElisarSea.hash(s.seed, i, 653) - 0.5) * 4.2);
            if (ox != dx || oz != dz) continue;
            int h = Math.min(2 + (int) (ElisarSea.hash(s.seed, i, 654) * 6) + (int) (ElisarSea.hash(s.seed, 0, 655) * 3), room);
            return new Span(1, h);
        }
        return null;
    }

    /** Украшение на самом верху растения. {@code top} — высота верхнего блока колонки. */
    private static BlockState tipOf(Site s, int dx, int dz, int top) {
        int room = 64;
        Span sp = columnOf(s, dx, dz, room);
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

    private static BlockState st(net.minecraftforge.registries.RegistryObject<? extends net.minecraft.world.level.block.Block> b) {
        return b.get().defaultBlockState().setValue(ru.aiforge.riftworld.block.SeaPlantBlock.WATERLOGGED, true);
    }
}
