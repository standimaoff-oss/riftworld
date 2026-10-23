package ru.aiforge.riftworld.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.registry.ElisarBlocks;

/**
 * Рев 8 «Живое море» — дно, ущелья, подводные пещеры и рифовые сады открытого океана Элисара.
 *
 * <p>Всё считается аналитически (как и остальной мир планеты), без сида и без фич-системы:
 * <ul>
 *   <li>порода дна — сплошной массив от y=0 до кромки, а не 5-блочная скорлупа (было: пещерам негде было жить);</li>
 *   <li>ущелья и желоба режут дно на глубину 6–14 блоков (каньоны идут пятнами по шуму);</li>
 *   <li>пещеры — двухуровневые «черви» (изолинии двух 3D-шумов) + залы, плюс «синие дыры»: вертикальные
 *       колодцы с дна в полость, у каждого — собственный зал на дне;</li>
 *   <li>рифовые сады: пятна рифового камня, мозговые кораллы, ветвистые/веерные/трубчатые и светящиеся
 *       кораллы, ламинария колоннами, морская трава, ракушки на иле.</li>
 * </ul>
 */
public final class ElisarSea {
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

    /** Насколько место «рифовое»: 0 — обычный ил/песок, 1 — плотный рифовый сад. */
    static double reefness(int x, int z) {
        double n = vnoise(x * 0.028, z * 0.028, 421) * 0.7 + vnoise(x * 0.075, z * 0.075, 422) * 0.3;
        return Math.max(0, Math.min(1, (n - 0.56) / 0.24));
    }

    /** Глубина ущелья под этой колонкой (0 — дно ровное). */
    static int trenchDepth(int x, int z) {
        double n = vnoise(x * 0.011, z * 0.011, 423) * 0.75 + vnoise(x * 0.035, z * 0.035, 424) * 0.25;
        if (n < 0.60) return 0;
        return (int) Math.round((n - 0.60) / 0.40 * 12.0);            // до 12 блоков вглубь
    }

    // ------------------------------------------------------------------ пещеры
    /** Пещера ли эта точка породы (ниже кромки дна). */
    static boolean cave(int x, int y, int z, int floor) {
        if (y < 1 || y > floor - 2) return false;
        // два уровня «червей»: верхний (y ~ floor-12) и нижний (y ~ 6..12)
        double w1 = vnoise3(x * 0.055, y * 0.085, z * 0.055, 431);
        double w2 = vnoise3(x * 0.055, y * 0.085, z * 0.055, 432);
        double d = Math.hypot(w1 - 0.5, w2 - 0.5);
        double width = 0.082 + 0.065 * vnoise3(x * 0.012, y * 0.02, z * 0.012, 433);
        if (d < width) return true;
        // залы-гроты
        double hall = vnoise3(x * 0.030, y * 0.055, z * 0.030, 434);
        return hall > 0.742 && y > 2;
    }

    /** «Синяя дыра»: колодец с морского дна вниз, с расширением-залом на дне. */
    static boolean blueHole(int x, int y, int z, int floor) {
        int gx = Math.floorDiv(x, 26), gz = Math.floorDiv(z, 26);
        if (hash(gx, gz, 441) < 0.42) return false;                       // не в каждой ячейке 26x26
        double cx = gx * 26 + 5 + hash(gx, gz, 442) * 16, cz = gz * 26 + 5 + hash(gx, gz, 443) * 16;
        double r = 2.0 + hash(gx, gz, 444) * 2.2;
        int depth = 9 + (int) (hash(gx, gz, 445) * 9);
        double d = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
        if (y > floor || y < floor - depth - 5) return false;
        if (d <= r) return true;
        // зал у основания колодца
        return y < floor - depth && d <= r + 3.5 && y > floor - depth - 5;
    }

    // ------------------------------------------------------------------ материалы
    private static BlockState st(RegistryObject<? extends Block> b) {
        return b.get().defaultBlockState();
    }

    /** Порода дна: сверху песок/ил/риф, ниже — камень с прослоями. */
    private static BlockState rock(int x, int y, int z, int floor, double reef) {
        int d = floor - y;
        if (d == 0) {
            if (reef > 0.62) return hash(x, z, 451) < 0.16 ? st(ElisarBlocks.CORAL_BRAIN) : st(ElisarBlocks.REEF_STONE);
            return reef > 0.30 ? st(ElisarBlocks.SEABED) : (vnoise(x * 0.06, z * 0.06, 452) > 0.52 ? st(ElisarBlocks.SILT) : st(ElisarBlocks.SEABED));
        }
        if (d <= 2) {
            if (reef > 0.55) return st(ElisarBlocks.REEF_STONE);
            return vnoise(x * 0.06, z * 0.06, 452) > 0.52 ? st(ElisarBlocks.SILT) : st(ElisarBlocks.SEABED);
        }
        if (d <= 5) return hash(x, y, z) < 0.35 ? st(ElisarBlocks.SEABED) : st(ElisarBlocks.STONE);
        double n = vnoise3(x * 0.05, y * 0.09, z * 0.05, 453);
        if (n > 0.70) return st(ElisarBlocks.REEF_STONE);
        if (n < 0.30) return st(ElisarBlocks.STONE_MOSSY);
        return hash(x, y, z) < 0.10 ? st(ElisarBlocks.COBBLE) : st(ElisarBlocks.STONE);
    }

    // ------------------------------------------------------------------ заливка колонки
    /**
     * Полная заливка открытого океана в точке (x,z): порода, пещеры, вода и сад на дне.
     *
     * @param floor кромка дна (обычно ocean-42..ocean-50), уже с учётом ущелья
     * @param sea   уровень моря
     */
    public static void fill(ChunkAccess chunk, BlockPos.MutableBlockPos mp, int x, int z, int floor, int sea) {
        double reef = reefness(x, z);
        for (int y = 0; y <= floor; ++y) chunk.setBlockState(mp.set(x, y, z), rock(x, y, z, floor, reef), false);

        // Пещеры: вырезаем полости и затопляем то, что связано с морем. Сверху вниз по колонке:
        //   open=true (море сверху) -> полость заливается водой; упёрлись в породу -> полость закрыта,
        //   дальше вниз это уже воздушный карман (там дышат рыбы и игрок).
        boolean open = true;
        boolean[] carved = new boolean[Math.max(floor, 1)];
        for (int y = 1; y < floor; ++y) carved[y] = cave(x, y, z, floor) || blueHole(x, y, z, floor);
        for (int y = floor - 1; y >= 1; --y) {
            if (!carved[y]) { open = false; continue; }
            if (open) chunk.setBlockState(mp.set(x, y, z), st(ElisarBlocks.WATER), false);
            else chunk.setBlockState(mp.set(x, y, z), Blocks.AIR.defaultBlockState(), false);
        }
        // кромка дна не проваливается сквозь пещеры чаще, чем в 35% мест (свод держит)
        if (floor > 2 && carved[floor - 1]) {
            boolean shaft = blueHole(x, floor - 1, z, floor);
            if (shaft || hash(x, z, 454) < 0.35) chunk.setBlockState(mp.set(x, floor, z), st(ElisarBlocks.WATER), false);
        }

        for (int y = floor + 1; y <= sea; ++y) chunk.setBlockState(mp.set(x, y, z), st(ElisarBlocks.WATER), false);

        garden(chunk, mp, x, z, floor, reef);
        caveDressing(chunk, mp, x, z, floor);
    }

    // ------------------------------------------------------------------ рифовый сад на дне
    private static void garden(ChunkAccess chunk, BlockPos.MutableBlockPos mp, int x, int z, int floor, double reef) {
        int y = floor + 1;
        BlockState below = chunk.getBlockState(mp.set(x, floor, z));
        if (!below.is(ElisarBlocks.SEABED.get()) && !below.is(ElisarBlocks.SILT.get()) && !below.is(ElisarBlocks.REEF_STONE.get())
                && !below.is(ElisarBlocks.CORAL_BRAIN.get())) return;
        double r = hash(x, z, 461);
        // ракушки на иле — ковриком
        if (below.is(ElisarBlocks.SILT.get()) && r < 0.10) {
            chunk.setBlockState(mp.set(x, y, z), st(ElisarBlocks.SEA_SHELLS), false);
            return;
        }
        if (reef > 0.5) {
            // плотный риф: ветвистые, веерные, трубчатые, светящиеся + мозговые шапки
            double p = reef * 0.55;
            if (r < p) {
                double k = hash(x, z, 462);
                BlockState s = k < 0.30 ? st(ElisarBlocks.CORAL_BRANCH)
                        : k < 0.54 ? st(ElisarBlocks.CORAL_FAN)
                        : k < 0.72 ? st(ElisarBlocks.CORAL_TUBE)
                        : k < 0.86 ? st(ElisarBlocks.CORAL_GLOW)
                        : st(ElisarBlocks.CORAL_BRAIN);
                chunk.setBlockState(mp.set(x, y, z), s, false);
                // ярусы: шапки и «деревца» в 2-3 блока высотой — силуэт рифа виден издалека
                double up = hash(x, z, 463);
                if (up < 0.42) chunk.setBlockState(mp.set(x, y + 1, z), s, false);
                if (up < 0.12) chunk.setBlockState(mp.set(x, y + 2, z), st(ElisarBlocks.CORAL_GLOW), false);
                return;
            }
        }
        // морская трава — пятнами по всему дну
        if (vnoise(x * 0.07, z * 0.07, 464) > 0.48 && r < 0.40) {
            chunk.setBlockState(mp.set(x, y, z), st(ElisarBlocks.SEA_GRASS), false);
            return;
        }
        // ламинария — колоннами, до самого света
        if (vnoise(x * 0.05, z * 0.05, 465) > 0.66 && reef < 0.35) {
            int h = 3 + (int) (hash(x, z, 466) * 6);
            for (int i = 0; i < h; ++i) {
                BlockState s = i == h - 1 ? st(ElisarBlocks.SEA_KELP_TOP) : st(ElisarBlocks.SEA_KELP);
                chunk.setBlockState(mp.set(x, y + i, z), s, false);
            }
        }
    }

    // ------------------------------------------------------------------ убранство пещер
    /** Светящиеся наросты на полу пещер, «сосульки» с потолка, ракушки в нишах. */
    private static void caveDressing(ChunkAccess chunk, BlockPos.MutableBlockPos mp, int x, int z, int floor) {
        for (int y = 2; y < floor - 1; ++y) {
            BlockPos p = mp.set(x, y, z);
            BlockState here = chunk.getBlockState(p);
            if (!here.isAir()) continue;
            BlockState up = chunk.getBlockState(mp.set(x, y + 1, z));
            BlockState down = chunk.getBlockState(mp.set(x, y - 1, z));
            double h = hash3(x, y, z, 471);
            if (down.isSolidRender(chunk, mp.set(x, y - 1, z)) && h < 0.10) {
                chunk.setBlockState(mp.set(x, y, z), st(ElisarBlocks.CAVE_GLOW), false);        // нарост на полу
            } else if (up.isSolidRender(chunk, mp.set(x, y + 1, z)) && h > 0.955) {
                chunk.setBlockState(mp.set(x, y, z), st(ElisarBlocks.CAVE_GLOW), false);        // друза на потолке
            }
        }
    }
}
