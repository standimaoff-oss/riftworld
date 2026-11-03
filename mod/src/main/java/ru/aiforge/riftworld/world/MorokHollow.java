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
            return new Region(cx, cz, R, FIXED ^ ((long) cx * 1000003L + (long) cz * 7867L));
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
        int bestX = 0, bestY = 0, bestZ = 0;
        int fbX = Integer.MIN_VALUE, fbY = 0, fbZ = 0;   // запасная точка на кромке биома
        double bestW = -1.0;
        for (int cx = -cellRadius; cx <= cellRadius; ++cx) {
            for (int cz = -cellRadius; cz <= cellRadius; ++cz) {
                int x = nearX + cx * 24, z = nearZ + cz * 24;
                double w = weight(x + 0.5, z + 0.5);
                if (w < 0.55 || w <= bestW) continue;
                if (plan.islandAt(x + 0.5, z + 0.5) == null) continue;
                level.getChunk(x >> 4, z >> 4);
                int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (top <= plan.ocean + 1) continue;
                BlockState st = level.getBlockState(new BlockPos(x, top - 1, z));
                boolean dressed = st.is(MorokBlocks.RIME_GRASS.get()) || st.is(MorokBlocks.PALE_MOSS_BLOCK.get())
                        || st.is(MorokBlocks.FROZEN_SOIL.get());
                if (!dressed) {
                    // кромка биома: мерзлота ещё не легла (её красит только при morok=true).
                    // Держим такую точку как запасную, но продолжаем искать «настоящую» чащу.
                    if (fbX == Integer.MIN_VALUE) {
                        fbX = x;
                        fbY = top;
                        fbZ = z;
                    }
                    continue;
                }
                bestW = w;
                bestX = x;
                bestY = top;
                bestZ = z;
            }
        }
        if (bestW >= 0) return new int[]{bestX, bestY, bestZ};
        return fbX == Integer.MIN_VALUE ? null : new int[]{fbX, fbY, fbZ};
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
