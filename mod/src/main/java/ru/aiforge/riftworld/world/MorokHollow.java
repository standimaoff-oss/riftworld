package ru.aiforge.riftworld.world;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import ru.aiforge.riftworld.registry.MorokBlocks;

/**
 * v0.36.0 — биом <b>«Мороковая Падь»</b> ({@code riftworld:morok_hollow}).
 *
 * <p>Страшный туманный бор из концепта {@code reports/biome_concept_morok_hollow/}: палевые
 * «костяные» сосны, изморозь, ледяные клыки, пепельный мох и жёсткий холодный туман.
 * Биом не перестраивает рельеф (никаких висячих островов над ним): он <b>перекрашивает
 * поверхность</b> диких островов Элисара в мерзлоту, а плотные деревья и туман делают
 * клиентские слои (см. {@code ElisarBiomeSource}, {@code morok_hollow.json}).
 *
 * <p>Сетка {@link #SUPER}: ~4 региона из 10 секторов, радиус 150–240 блоков,
 * детерминировано от семени — как у остальных биомов мода.
 */
public final class MorokHollow {
    public static final int SUPER = 1240;
    private static final long FIXED = 0x4D4F524F4B48L;
    private static final ConcurrentHashMap<Long, Region> CACHE = new ConcurrentHashMap<>();

    /** Один массив суши: центр, радиус, семя. */
    public static final class Region {
        public final int cx, cz, R;
        public final long seed;

        Region(int cx, int cz, int R, long seed) {
            this.cx = cx;
            this.cz = cz;
            this.R = R;
            this.seed = seed;
        }
    }

    private MorokHollow() {
    }

    public static Region region(int sx, int sz) {
        return CACHE.computeIfAbsent(((long) sx << 32) ^ (sz & 0xFFFFFFFFL), k -> gen(sx, sz));
    }

    private static Region gen(int sx, int sz) {
        Random r = new Random(FIXED ^ (sx * 42317861L + sz * 1103515245L));
        if (r.nextDouble() > 0.38) return null;
        for (int attempt = 0; attempt < 10; ++attempt) {
            int cx = sx * SUPER + 190 + r.nextInt(SUPER - 380);
            int cz = sz * SUPER + 190 + r.nextInt(SUPER - 380);
            int R = 150 + r.nextInt(90);
            if (Math.hypot(cx, cz) < 940) continue;                                  // не рядом с Белым городом
            if (AshenLands.blocks(cx, cz, R + 140)) continue;                        // пепельные угодья
            if (BlacktideCitadel.insideAnyFootprint(cx, cz, R + 200)) continue;      // цитадель в открытом океане
            if (CrystalRifts.nearAny(cx, cz, R, 90)) continue;                       // хрустальные разломы
            if (VioletHills.nearAny(cx, cz, R, 90)) continue;                        // фиолетовые холмы
            if (WaterfallGardens.nearAny(cx, cz, R, 90)) continue;                   // висячие сады
            SizoGrove.Region sg = SizoGrove.regionAt(cx, cz);
            if (sg != null && Math.hypot(cx - sg.hub.cx, cz - sg.hub.cz) < R + sg.hub.maxReach() + 60) continue;
            MistKeep.Region mk = MistKeep.regionAt(cx, cz);
            if (mk != null && Math.hypot(cx - mk.cx, cz - mk.cz) < R + mk.Rr + 80) continue;
            // v0.38.3: регион обязан задевать сушу, иначе биом физически не появляется —
            // ElisarBiomeSource красит в Падь только острова. Проверка идёт через
            // WildIslands.cell(...) — тот же примитив, что использует FloatingIslands:
            // это чистая математика по сетке 160x160 с кэшем, без CityPlan.islandAt и без
            // построения тайлов (именно оно в v0.38.1 залипло на /riftbiome crystal).
            if (!touchesLand(cx, cz, R)) continue;
            return new Region(cx, cz, R, FIXED ^ ((long) cx * 1000003L + (long) cz * 7867L));
        }
        return null;
    }

    /** Есть ли суша ровно в этой точке (дикие острова: сетка {@code WildIslands.CELL}). */
    public static boolean landAt(double x, double z) {
        int cell = WildIslands.CELL;
        int cx = Math.floorDiv((int) Math.floor(x), cell);
        int cz = Math.floorDiv((int) Math.floor(z), cell);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                for (CityPlan.Island isl : WildIslands.cell(cx + dx, cz + dz).islands) {
                    if (isl.contains(x, z)) return true;
                }
            }
        }
        return false;
    }

    /** Задевает ли регион сушу: хотя бы четверть пробных колонок должны быть островом. */
    private static boolean touchesLand(int cx, int cz, int R) {
        int hits = 0, total = 0;
        for (double frac : new double[]{0.75, 0.55, 0.35}) {
            for (int i = 0; i < 8; ++i) {
                double a = i * Math.PI / 4.0;
                double x = cx + R * frac * Math.cos(a);
                double z = cz + R * frac * Math.sin(a);
                ++total;
                if (landAt(x, z)) ++hits;
            }
        }
        return total > 0 && hits * 3 >= total;   // >= 25 %
    }

    /**
     * Диагностика для пробы: сколько регионов Пади рядом и у скольких из них есть суша.
     *
     * @return {@code {регионов, регионов на суше}}
     */
    public static int[] scanStats(int nearX, int nearZ, int cellRadius) {
        int scx = Math.floorDiv(nearX, SUPER);
        int scz = Math.floorDiv(nearZ, SUPER);
        int all = 0, land = 0;
        for (int dx = -cellRadius; dx <= cellRadius; ++dx) {
            for (int dz = -cellRadius; dz <= cellRadius; ++dz) {
                Region rg = region(scx + dx, scz + dz);
                if (rg == null) continue;
                ++all;
                if (touchesLand(rg.cx, rg.cz, rg.R)) ++land;
            }
        }
        return new int[]{all, land};
    }

    /**
     * Регион Пади, который реально стоит на суше, вместе с точкой внутри него.
     *
     * <p>Проверка суши вынесена СЮДА (а не в {@link #gen}), потому что {@code islandAt}
     * строит тайлы островов: из пути генерации чанков это давало залипание сервера.
     *
     * @param cellRadius сколько ячеек {@link #SUPER} просмотреть вокруг точки
     * @return {@code {x, z, Region}} или {@code null}, если суши под Падью нет рядом
     */
    public static Object[] findLandRegion(int nearX, int nearZ, int cellRadius) {
        int scx = Math.floorDiv(nearX, SUPER);
        int scz = Math.floorDiv(nearZ, SUPER);
        java.util.List<Region> found = new java.util.ArrayList<>();
        for (int dx = -cellRadius; dx <= cellRadius; ++dx) {
            for (int dz = -cellRadius; dz <= cellRadius; ++dz) {
                Region rg = region(scx + dx, scz + dz);
                if (rg != null) found.add(rg);
            }
        }
        // ближние к запрошенной точке — первыми
        found.sort(java.util.Comparator.comparingDouble(
                r -> Math.hypot(r.cx - nearX, r.cz - nearZ)));
        int budget = 2400;  // страховка от долгого поиска
        for (Region rg : found) {
            // кольца внутри региона (вес >= 0.55 требует d <= R-25), 16 направлений
            for (double frac : new double[]{0.70, 0.50, 0.30}) {
                for (int i = 0; i < 16; ++i) {
                    double a = i * Math.PI / 8.0;
                    int x = rg.cx + (int) Math.round(rg.R * frac * Math.cos(a));
                    int z = rg.cz + (int) Math.round(rg.R * frac * Math.sin(a));
                    if (--budget <= 0) return null;
                    if (weight(x + 0.5, z + 0.5) < 0.55) continue;
                    if (!landAt(x + 0.5, z + 0.5)) continue;
                    return new Object[]{x, z, rg};
                }
            }
        }
        return null;
    }

    public static Region regionAt(double x, double z) {
        int sx = Math.floorDiv((int) Math.floor(x), SUPER), sz = Math.floorDiv((int) Math.floor(z), SUPER);
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) {
                Region rg = region(sx + dx, sz + dz);
                if (rg != null && Math.hypot(x - rg.cx, z - rg.cz) < rg.R + 80) return rg;
            }
        return null;
    }

    /** 0 вне Пади .. 1 в самой чаще (рваная кромка из шума, как у пепельных угодий). */
    public static double weight(double x, double z) {
        Region rg = regionAt(x, z);
        if (rg == null) return 0;
        double d = Math.hypot(x - rg.cx, z - rg.cz)
                + (ElisarChunkGenerator.vnoise(x * 0.014, z * 0.014, 907) - 0.5) * 56;
        return Math.max(0, Math.min(1, (rg.R - d) / 46.0));
    }

    public static boolean inside(double x, double z) {
        return weight(x, z) > 0.40;
    }

    /** Дальний (грубый) тест для расстановки других структур: занят ли прямоугольник. */
    public static boolean blocks(double x, double z, double margin) {
        Region rg = regionAt(x, z);
        return rg != null && Math.hypot(x - rg.cx, z - rg.cz) < rg.R + margin;
    }

    /**
     * Перекрашивает поверхность острова в мерзлоту Пади: дёрн/мох сверху, мёрзлая земля под ним,
     * редкие ледяные клыки и ковры пепельного мха.
     */
    public static void dressSurface(ChunkAccess chunk, BlockPos.MutableBlockPos mp, int x, int z, int top) {
        BlockState rime = MorokBlocks.RIME_GRASS.get().defaultBlockState();
        BlockState soil = MorokBlocks.FROZEN_SOIL.get().defaultBlockState();
        BlockState moss = MorokBlocks.PALE_MOSS_BLOCK.get().defaultBlockState();
        BlockState carpet = MorokBlocks.PALE_MOSS_CARPET.get().defaultBlockState();
        BlockState fang = MorokBlocks.ICE_FANG.get().defaultBlockState();
        double h = ElisarChunkGenerator.hash(x, z, 911);
        chunk.setBlockState(mp.set(x, top, z), h < 0.52 ? rime : h < 0.74 ? moss : soil, false);
        for (int y = top - 1; y >= top - 3; --y) chunk.setBlockState(mp.set(x, y, z), soil, false);
        double h2 = ElisarChunkGenerator.hash(x, z, 912);
        if (h2 < 0.055) chunk.setBlockState(mp.set(x, top + 1, z), carpet, false);
        else if (h2 > 0.988) chunk.setBlockState(mp.set(x, top + 1, z), fang, false);
    }

    /**
     * v0.38.0 — уровень «мерзлоты» в готовом чанке: верхний блок из набора Пади.
     *
     * <p>Декор (стволы, сухостой, поваленные бревна, колючки, клыки) ставится ПОСЛЕ рельефа,
     * поэтому высоту колонки нельзя взять из карты высот (там ещё трава/мох биома): ищем
     * первый сверху блок мерзлоты. {@code Integer.MIN_VALUE} — колонка не в Пади (вода/камень).
     */
    public static int groundOf(ChunkAccess chunk, BlockPos.MutableBlockPos mp, int x, int z) {
        for (int y = 200; y > 60; --y) {
            BlockState st = chunk.getBlockState(mp.set(x, y, z));
            if (st.is(MorokBlocks.RIME_GRASS.get()) || st.is(MorokBlocks.PALE_MOSS_BLOCK.get())
                    || st.is(MorokBlocks.FROZEN_SOIL.get())) {
                return y;
            }
            if (!st.isAir() && !st.canBeReplaced()) return Integer.MIN_VALUE;   // камень/вода/постройка
        }
        return Integer.MIN_VALUE;
    }

    /**
     * v0.38.0 — точка НАСТОЯЩЕЙ суши внутри Пади (для проб и камер CI).
     *
     * <p>Регион Пади задаётся расстоянием до центра и может лежать над открытым океаном: там
     * биом не включается ({@code ElisarBiomeSource} требует остров), деревья не растут, а
     * камера пробы оказывалась над водой. Перебираем сетку вокруг центра и берём колонку,
     * где вес максимален, есть остров и верхний блок — мерзлота Пади.
     *
     * @return {@code {x, y, z}} или {@code null}
     */
    public static int[] landSpot(ServerLevel level, int nearX, int nearZ, int cellRadius) {
        CityPlan plan = CityPlan.get();
        Object[] hit = findLandRegion(nearX, nearZ, cellRadius);
        if (hit == null) return null;
        int bx = (Integer) hit[0], bz = (Integer) hit[1];
        // вокруг найденной точки ищем уже «одетую» мерзлоту Пади: тогда кадр покажет живой биом
        for (int r = 0; r <= 32; r += 16) {
            for (int n = (r == 0 ? 1 : 8); n > 0; --n) {
                double a = n * Math.PI / 4.0;
                int x = bx + (int) Math.round(r * Math.cos(a));
                int z = bz + (int) Math.round(r * Math.sin(a));
                if (!landAt(x + 0.5, z + 0.5)) continue;
                level.getChunk(x >> 4, z >> 4);
                int top = level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (top <= plan.ocean + 1) continue;
                BlockState st = level.getBlockState(new BlockPos(x, top - 1, z));
                if (st.is(MorokBlocks.RIME_GRASS.get()) || st.is(MorokBlocks.PALE_MOSS_BLOCK.get())
                        || st.is(MorokBlocks.FROZEN_SOIL.get())) {
                    return new int[]{x, top, z};
                }
            }
        }
        return null;
    }

    /**
     * Нужно ли ставить бледную сосну в этой колонке: решётка 7x7 с плотностью 1 из 3 клеток
     * внутри Пади (лес разреженный, чтобы туман читался).
     */
    public static boolean treeHere(int x, int z) {
        if (!inside(x + 0.5, z + 0.5)) return false;
        if (Math.floorMod(x, 7) != 3 || Math.floorMod(z, 7) != 3) return false;
        return ElisarChunkGenerator.hash(x, z, 913) < 0.66;
    }

    /** Высота ствола бледной сосны (9–17 блоков). */
    public static int treeHeight(int x, int z) {
        return 9 + (int) Math.round(ElisarChunkGenerator.hash(x, z, 914) * 8);
    }
}
