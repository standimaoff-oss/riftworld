package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import ru.aiforge.riftworld.registry.CastleBlocks;
import ru.aiforge.riftworld.registry.ElisarBlocks;

/**
 * «Адские угодья» ({@code riftworld:ashen_lands}): полноценный биом со случайным спавном по миру.
 *
 * <p>В каждом сгенерированном регионе биома посреди пепельной равнины с лавовыми меандрами стоит
 * выжженный остров с замком «Чертог Тлеющего Рога» ({@code riftworld:castle/castle}) и боссом
 * Моргратом ({@code ArenaCoreBlockEntity} → {@code MorgrathBoss}), а вокруг разбросаны обрушившиеся
 * курганы с обугленными деревьями, разрушенным домом и обломанным мостом.
 */
public final class AshenLands {
    public static final int SUPER = 1600;
    private static final long FIXED = 0x0A5E71A9L;

    /** A huge island that fell onto the plain. */
    public record Mound(double x, double z, double r, double h, double tx, double tz, int seed) {
    }

    public record Placed(BlockPos pos, BlockState state) {
    }

    /** Один регион адских угодий: свой остров-крепость с замком и боссом + курганы и руины вокруг. */
    public static final class Region {
        public final int cx, cz;
        public final double R;
        public final boolean capital;
        public final int x0, z0;
        public final CityPlan.Island island;
        public final List<CityPlan.Tree> castleTrees = new ArrayList<>();
        public final List<Mound> mounds;
        public final List<CityPlan.Tree> moundTrees = new ArrayList<>();
        private List<Placed> decor;

        Region(int cx, int cz, double R, long rSeed) {
            this.cx = cx;
            this.cz = cz;
            this.R = R;
            this.capital = true;
            this.x0 = cx - 38;
            this.z0 = cz - 34;
            Random rnd = new Random(rSeed ^ 0xA5E5L);
            this.island = new CityPlan.Island(cx, cz, CastleSite.R, CastleSite.Y, CastleSite.R * 0.6 + 14,
                    rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283, 5, 0.35);
            for (int k = 0, tries = 0; k < 34 && tries < 600; ++tries) {
                double a = rnd.nextDouble() * Math.PI * 2, d = 20 + rnd.nextDouble() * (CastleSite.R - 26);
                int x = (int) Math.floor(cx + Math.cos(a) * d), z = (int) Math.floor(cz + Math.sin(a) * d);
                if (island.t(x, z) < 0.12 || inCastleFootprint(x, z, 7)) continue;
                if (Math.abs(x - cx) < 12 && z < z0) continue; // keep gatehouse approach clear
                boolean ok = true;
                for (CityPlan.Tree t : castleTrees) {
                    if (Math.abs(t.x - x) < 6 && Math.abs(t.z - z) < 6) { ok = false; break; }
                }
                if (!ok) continue;
                castleTrees.add(new CityPlan.Tree(x, z, 2, 5 + rnd.nextInt(6), rnd.nextInt(1 << 30)));
                ++k;
            }
            this.mounds = List.of(
                    new Mound(cx - 155, cz + 112, 46, 30, 0.45, -0.25, 1),
                    new Mound(cx + 142, cz + 118, 40, 26, -0.35, 0.40, 2),
                    new Mound(cx - 108, cz - 148, 34, 22, 0.20, 0.50, 3));
            Random mr = new Random(rSeed ^ 0xA5E0L);
            for (Mound m : mounds) {
                int n = (int) (m.r * m.r / 140);
                for (int k = 0, tries = 0; k < n && tries < 200; ++tries) {
                    double a = mr.nextDouble() * Math.PI * 2, d = Math.sqrt(mr.nextDouble()) * m.r * 0.8;
                    int x = (int) Math.floor(m.x + Math.cos(a) * d), z = (int) Math.floor(m.z + Math.sin(a) * d);
                    if (m.seed == 1 && Math.abs(x - m.x) < 9 && Math.abs(z - m.z) < 9) continue;
                    if (m.seed == 2 && Math.abs(z - m.z) < 5) continue;
                    boolean ok = true;
                    for (CityPlan.Tree t : moundTrees) {
                        if (Math.abs(t.x - x) < 6 && Math.abs(t.z - z) < 6) { ok = false; break; }
                    }
                    if (!ok) continue;
                    CityPlan.Tree t = new CityPlan.Tree(x, z, 2, 5 + mr.nextInt(6), mr.nextInt(1 << 30));
                    t.baseY = moundTopIn(mounds, x, z) + 1;
                    if (t.baseY <= ocean() + 2) continue;
                    moundTrees.add(t);
                    ++k;
                }
            }
        }

        public boolean inCastleFootprint(double x, double z, double margin) {
            return x >= x0 - margin && x < x0 + CastleSite.TW + margin
                    && z >= z0 - margin && z < z0 + CastleSite.TD + margin;
        }

        public double castleOutside(double x, double z) {
            double dx = Math.max(Math.max(x0 - x, 0), x - (x0 + CastleSite.TW - 1));
            double dz = Math.max(Math.max(z0 - z, 0), z - (z0 + CastleSite.TD - 1));
            return Math.hypot(dx, dz);
        }

        public synchronized List<Placed> decor() {
            if (decor == null) {
                List<Placed> out = new ArrayList<>();
                house(out, mounds, mounds.get(0));
                bridge(out, mounds, mounds.get(1));
                decor = out;
            }
            return decor;
        }
    }

    private static final ConcurrentHashMap<Long, Optional<Region>> CACHE = new ConcurrentHashMap<>();

    public static Region region(int sx, int sz) {
        return CACHE.computeIfAbsent(WildIslands.key(sx, sz), k -> Optional.ofNullable(gen(sx, sz))).orElse(null);
    }

    private static Region gen(int sx, int sz) {
        if (sx == 0 && sz == 0) return null; // hub city cell
        long cellSeed = FIXED ^ (sx * 53982894593057L + sz * 317074912223L) * 0x9E3779B97F4A7C15L;
        Random r = new Random(cellSeed);
        if (r.nextDouble() > 0.36) return null; // случайно спавнится в ~36% супер-ячеек мира
        int cx = sx * SUPER + 320 + r.nextInt(SUPER - 640);
        int cz = sz * SUPER + 320 + r.nextInt(SUPER - 640);
        if (Math.hypot(cx, cz) < 860) return null;                       // вдали от города Элисар
        if (Math.hypot(cx - 640, cz - 690) < 420) return null;           // не перекрывает морские пробы spots.json
        double R = 215 + r.nextInt(50);
        return new Region(cx, cz, R, cellSeed);
    }

    /** Регион адских угодий, чья область покрывает точку (x, z), или null. */
    public static Region regionAt(double x, double z) {
        int sx = Math.floorDiv((int) Math.floor(x), SUPER), sz = Math.floorDiv((int) Math.floor(z), SUPER);
        Region best = null;
        double bestD = 1e18;
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) {
                Region rg = region(sx + dx, sz + dz);
                if (rg == null) continue;
                double d = Math.hypot(x - rg.cx, z - rg.cz);
                if (d < rg.R + 90 && d < bestD) {
                    bestD = d;
                    best = rg;
                }
            }
        return best;
    }

    /** Ближайший регион адских угодий с замком и боссом вокруг точки (x, z). */
    public static Region nearest(double x, double z) {
        int sx0 = Math.floorDiv((int) Math.floor(x), SUPER), sz0 = Math.floorDiv((int) Math.floor(z), SUPER);
        Region best = null;
        double bestD = 1e18;
        for (int rad = 0; rad <= 6; ++rad) {
            for (int dx = -rad; dx <= rad; ++dx)
                for (int dz = -rad; dz <= rad; ++dz) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != rad) continue;
                    Region rg = region(sx0 + dx, sz0 + dz);
                    if (rg == null) continue;
                    double d = Math.hypot(x - rg.cx, z - rg.cz);
                    if (d < bestD) {
                        bestD = d;
                        best = rg;
                    }
                }
            if (best != null && rad >= 1) break;
        }
        return best;
    }

    /** 0 вне региона .. 1 в центре; край «рваный» — как у остальных биомов. */
    public static double weight(double x, double z) {
        int sx = Math.floorDiv((int) Math.floor(x), SUPER), sz = Math.floorDiv((int) Math.floor(z), SUPER);
        double best = EmberRuins.weight(x, z);
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) {
                Region rg = region(sx + dx, sz + dz);
                if (rg == null) continue;
                double ox = x - rg.cx, oz = z - rg.cz;
                if (Math.abs(ox) > rg.R + 60 || Math.abs(oz) > rg.R + 60) continue;
                double d = Math.hypot(ox, oz) + (ElisarChunkGenerator.vnoise(x * 0.015, z * 0.015, 81) - 0.5) * 40;
                double w = Math.max(0, Math.min(1, (rg.R - d) / 30.0));
                if (w > best) best = w;
            }
        return best;
    }

    public static boolean blocks(double x, double z, double margin) {
        int sx = Math.floorDiv((int) Math.floor(x), SUPER), sz = Math.floorDiv((int) Math.floor(z), SUPER);
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) {
                Region rg = region(sx + dx, sz + dz);
                if (rg != null && Math.hypot(x - rg.cx, z - rg.cz) < rg.R + margin) return true;
            }
        return false;
    }

    /** Совместимость со старыми константами: ближайший к городу случайно сгенерированный замок. */
    public static final Region CAPITAL = nearest(0, 0);
    public static final int CX = CAPITAL.cx, CZ = CAPITAL.cz;
    public static final double R = CAPITAL.R;

    private AshenLands() {
    }

    static int ocean() {
        return CityPlan.get().ocean;
    }

    public static boolean inside(double x, double z) {
        Region rg = regionAt(x, z);
        return rg != null && Math.hypot(x - rg.cx, z - rg.cz) < CastleSite.R + 46;
    }

    private static int moundTopIn(List<Mound> mounds, int x, int z) {
        double best = -1;
        for (Mound m : mounds) {
            double dx = x + 0.5 - m.x, dz = z + 0.5 - m.z;
            double wob = (ElisarChunkGenerator.vnoise(x * 0.06, z * 0.06, 90 + m.seed) - 0.5) * 0.35;
            double d = Math.hypot(dx, dz) / m.r + wob;
            if (d >= 1) continue;
            double h = m.h * Math.pow(1 - d * d, 0.55);
            h += (dx * m.tx + dz * m.tz) / m.r * m.h * 0.35 * (1 - d);
            h += (ElisarChunkGenerator.vnoise(x * 0.12, z * 0.12, 95 + m.seed) - 0.5) * 5 * (1 - d);
            if (m.seed == 1 && Math.abs(dx) < 9 && Math.abs(dz) < 9) h = m.h * 0.92;
            best = Math.max(best, Math.min(h, m.h * 1.1));
        }
        return best < 1.5 ? Integer.MIN_VALUE : ocean() + (int) Math.floor(best);
    }

    /** Top Y of the fallen islands at (x, z), or Integer.MIN_VALUE where there is none. */
    public static int moundTop(int x, int z) {
        Region rg = regionAt(x + 0.5, z + 0.5);
        return rg == null ? Integer.MIN_VALUE : moundTopIn(rg.mounds, x, z);
    }

    /** Lava meander under (x, z)? Contour lines of a warped noise field = winding channels ~3 blocks wide. */
    public static boolean meander(int x, int z) {
        double wx = (ElisarChunkGenerator.vnoise(x * 0.02, z * 0.02, 71) - 0.5) * 40;
        double wz = (ElisarChunkGenerator.vnoise(x * 0.02 + 17, z * 0.02 + 9, 72) - 0.5) * 40;
        double m = ElisarChunkGenerator.vnoise((x + wx) * 0.011, (z + wz) * 0.011, 73);
        double w = 0.014 + ElisarChunkGenerator.vnoise(x * 0.03, z * 0.03, 74) * 0.012;
        return Math.abs(m - 0.5) < w || Math.abs(m - 0.22) < w * 0.7;
    }

    /** Plain + meanders + boulders + fallen islands for one column (replaces seabed/water). */
    static void column(net.minecraft.world.level.chunk.ChunkAccess chunk, BlockPos.MutableBlockPos mp, int x, int z, boolean islandAbove) {
        int o = ocean();
        BlockState ash = CastleBlocks.ASH.get().defaultBlockState(), soil = CastleBlocks.BURNT_SOIL.get().defaultBlockState();
        BlockState sst = CastleBlocks.SCORCHED_STONE.get().defaultBlockState(), stone = ElisarBlocks.STONE.get().defaultBlockState();
        BlockState lava = ru.aiforge.riftworld.registry.ModBlocks.MAGMA_MEANDER.get().defaultBlockState();
        int top = moundTop(x, z);
        chunk.setBlockState(mp.set(x, o - 2, z), sst, false);
        if (top != Integer.MIN_VALUE) {
            double n = ElisarChunkGenerator.vnoise(x * 0.09, z * 0.09, 51);
            for (int y = o - 1; y <= top; ++y) {
                BlockState s;
                if (y == top) s = n > 0.62 ? soil : (n < 0.3 && ElisarChunkGenerator.hash(x, z, 52) < 0.5) ? sst : ash;
                else if (y >= top - 2) s = n > 0.5 ? soil : sst;
                else s = ElisarChunkGenerator.hash(x, y, z) < 0.22 ? stone : sst;
                chunk.setBlockState(mp.set(x, y, z), s, false);
            }
            return;
        }
        if (meander(x, z)) {
            chunk.setBlockState(mp.set(x, o - 1, z), lava, false);
            return;
        }
        double n = ElisarChunkGenerator.vnoise(x * 0.07, z * 0.07, 76);
        boolean bank = meander(x + 1, z) || meander(x - 1, z) || meander(x, z + 1) || meander(x, z - 1);
        chunk.setBlockState(mp.set(x, o - 1, z), bank || n < 0.35 ? sst : soil, false);
        chunk.setBlockState(mp.set(x, o, z), bank ? sst : n > 0.55 ? ash : n > 0.4 ? soil : sst, false);
        int gx = Math.floorDiv(x, 11), gz = Math.floorDiv(z, 11);
        if (!bank && ElisarChunkGenerator.hash(gx, gz, 77) < 0.3) {
            double bx = gx * 11 + 3 + ElisarChunkGenerator.hash(gx, gz, 78) * 5, bz = gz * 11 + 3 + ElisarChunkGenerator.hash(gx, gz, 79) * 5;
            double br = 1.3 + ElisarChunkGenerator.hash(gx, gz, 80) * 2.2;
            double d = Math.hypot(x + 0.5 - bx, z + 0.5 - bz);
            if (d < br && !meander((int) bx, (int) bz)) {
                int h = (int) Math.round((br - d) * 1.4 + 0.3);
                for (int y = 1; y <= h; ++y) chunk.setBlockState(mp.set(x, o + y, z), ElisarChunkGenerator.hash(x, y, z) < 0.3 ? stone : sst, false);
            }
        }
        if (ElisarChunkGenerator.hash(x, z, 82) < 0.02) chunk.setBlockState(mp.set(x, o + 1, z), ash, false);
    }

    public static List<CityPlan.Tree> treesNear(int x, int z) {
        Region rg = regionAt(x + 0.5, z + 0.5);
        if (rg == null) return List.of();
        List<CityPlan.Tree> out = new ArrayList<>();
        for (CityPlan.Tree t : rg.moundTrees) {
            if (Math.abs(x - t.x) <= 4 && Math.abs(z - t.z) <= 4) out.add(t);
        }
        for (CityPlan.Tree t : rg.castleTrees) {
            if (Math.abs(x - t.x) <= 4 && Math.abs(z - t.z) <= 4) out.add(t);
        }
        return out;
    }

    public static List<Placed> decorNear(int x0, int z0) {
        Region rg = regionAt(x0 + 8, z0 + 8);
        return rg == null ? List.of() : rg.decor();
    }

    private static BlockState s(net.minecraftforge.registries.RegistryObject<? extends net.minecraft.world.level.block.Block> b) {
        return b.get().defaultBlockState();
    }

    private static void house(List<Placed> out, List<Mound> mounds, Mound m) {
        int x0 = (int) m.x - 4, z0 = (int) m.z - 3, y0 = moundTopIn(mounds, (int) m.x, (int) m.z) + 1;
        Random r = new Random(0xB0B5E);
        BlockState br = s(CastleBlocks.BRICKS), cr = s(CastleBlocks.BRICKS_CRACKED), ash = s(CastleBlocks.ASHLAR);
        for (int x = x0; x < x0 + 9; ++x)
            for (int z = z0; z < z0 + 7; ++z) {
                boolean wall = x == x0 || x == x0 + 8 || z == z0 || z == z0 + 6;
                out.add(new Placed(new BlockPos(x, y0 - 1, z), r.nextInt(4) == 0 ? s(CastleBlocks.BURNT_SOIL) : s(CastleBlocks.DARK_PLANKS)));
                if (!wall) {
                    if (r.nextInt(9) == 0) out.add(new Placed(new BlockPos(x, y0, z), s(CastleBlocks.ASH)));
                    continue;
                }
                boolean corner = (x == x0 || x == x0 + 8) && (z == z0 || z == z0 + 6);
                int h = corner ? 4 + r.nextInt(2) : (int) Math.round(1 + 3.2 * Math.abs(Math.sin((x * 0.9 + z * 1.3) * 0.7)) * r.nextDouble() + (x < x0 + 4 ? 1 : 0));
                if (z == z0 && x == x0 + 4) h = 0;
                for (int y = 0; y < h; ++y) {
                    if (z == z0 + 6 && x == x0 + 4 && y == 1) continue;
                    if (x == x0 && z == z0 + 3 && y == 1) continue;
                    BlockState b = corner ? ash : r.nextInt(3) == 0 ? cr : br;
                    out.add(new Placed(new BlockPos(x, y0 + y, z), b));
                }
                if (z == z0 && x == x0 + 4) {
                    out.add(new Placed(new BlockPos(x, y0 + 3, z), ash));
                    out.add(new Placed(new BlockPos(x - 1, y0, z), br));
                }
            }
        for (int k = 0; k < 6; ++k) {
            out.add(new Placed(new BlockPos(x0 + 1 + k, y0, z0 + 2 + (k > 3 ? 1 : 0)), s(CastleBlocks.CHARRED_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X)));
        }
        out.add(new Placed(new BlockPos(x0 + 6, y0 + 1, z0 + 5), s(CastleBlocks.DARK_BEAM).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z)));
        out.add(new Placed(new BlockPos(x0 + 7, y0, z0 + 4), s(CastleBlocks.ROOF_SLAB)));
        out.add(new Placed(new BlockPos(x0 + 2, y0, z0 + 5), s(CastleBlocks.ROOF_STAIRS).setValue(StairBlock.FACING, Direction.EAST)));
        out.add(new Placed(new BlockPos(x0 + 7, y0, z0 + 1), s(CastleBlocks.BRICKS_STAIRS).setValue(StairBlock.FACING, Direction.WEST)));
        for (int k = 0; k < 8; ++k) {
            int x = x0 - 2 + r.nextInt(13), z = z0 - 2 + r.nextInt(11);
            if (x > x0 && x < x0 + 8 && z > z0 && z < z0 + 6) continue;
            out.add(new Placed(new BlockPos(x, moundTopIn(mounds, x, z) + 1, z), r.nextBoolean() ? cr : s(CastleBlocks.BRICKS_SLAB)));
        }
        for (int y = 0; y < 6; ++y) out.add(new Placed(new BlockPos(x0 + 8, y0 + y, z0 + 5), y > 3 ? cr : br));
        out.add(new Placed(new BlockPos(x0 + 7, y0, z0 + 5), Blocks.AIR.defaultBlockState()));
    }

    private static void bridge(List<Placed> out, List<Mound> mounds, Mound m) {
        int bz = (int) m.z;
        int xa = (int) (m.x - m.r * 0.45);
        int ya = moundTopIn(mounds, xa, bz) + 1;
        BlockState ashl = s(CastleBlocks.ASHLAR), plank = s(CastleBlocks.DARK_PLANKS), fence = s(CastleBlocks.DARK_FENCE);
        BlockState slab = s(CastleBlocks.DARK_SLAB);
        BlockState chainY = s(ElisarBlocks.CHAIN).setValue(ChainBlock.AXIS, Direction.Axis.Y);
        for (int dz = -2; dz <= 2; ++dz)
            for (int y = ya - 3; y <= ya; ++y) out.add(new Placed(new BlockPos(xa, y, bz + dz), ashl));
        out.add(new Placed(new BlockPos(xa, ya + 1, bz - 2), ashl));
        out.add(new Placed(new BlockPos(xa, ya + 1, bz + 2), ashl));
        out.add(new Placed(new BlockPos(xa, ya + 2, bz - 2), s(ElisarBlocks.LANTERN)));
        int len = 16;
        for (int k = 1; k <= len; ++k) {
            int x = xa - k;
            int y = ya - (int) Math.round(Math.sin(Math.PI * k / 30.0) * 2.2);
            for (int dz = -1; dz <= 1; ++dz) {
                if (k > len - 3 && (dz + k) % 3 == 0) continue;
                if (k == 9 && dz == 0) continue;
                out.add(new Placed(new BlockPos(x, y, bz + dz), k > len - 2 ? slab.setValue(SlabBlock.TYPE, SlabType.TOP) : plank));
            }
            if (k % 5 == 0 && k < len - 2) {
                for (int dz : new int[] {-2, 2}) {
                    out.add(new Placed(new BlockPos(x, y, bz + dz), plank));
                    out.add(new Placed(new BlockPos(x, y + 1, bz + dz), fence));
                    out.add(new Placed(new BlockPos(x, y + 2, bz + dz), fence));
                }
                for (int y2 = ocean() + 1; y2 < y; ++y2) out.add(new Placed(new BlockPos(x, y2, bz), k == 10 && y2 > y - 4 ? Blocks.AIR.defaultBlockState() : s(CastleBlocks.DARK_BEAM)));
            }
            if (k == len - 1) for (int y2 = y - 1; y2 > y - 5; --y2) out.add(new Placed(new BlockPos(x, y2, bz + 1), chainY));
        }
        int o = ocean();
        for (int k = 0; k < 7; ++k) {
            int x = xa - len - 2 - k, z = bz - 2 + (k * 5) % 5;
            if (!meander(x, z)) out.add(new Placed(new BlockPos(x, o + 1, z), k % 2 == 0 ? plank : slab));
        }
        out.add(new Placed(new BlockPos(xa - len - 4, o + 1, bz + 2), s(CastleBlocks.DARK_BEAM).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X)));
        out.add(new Placed(new BlockPos(xa - len - 5, o + 1, bz + 2), s(CastleBlocks.DARK_BEAM).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X)));
    }
}
