package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.List;
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
 * «Адские угодья» (Ashen Lands): a scorched biome — the ocean below the islands is replaced by an ash plain cut by
 * glowing lava meanders, with scorched mounds, burnt dead trees and an ash-brown sky (ElisarSky fog + particles).
 *
 * <p>Рев 14.26 по замечанию владельца: «адский остров в море с замком и боссом сделать биомом, спавнящимся
 * рандомно по миру как любой другой биом». Раньше это был ОДИН жёстко прибитый к координатам (40, 470)
 * регион у города — теперь у адских угодий своя сетка {@link #SUPER}: как и у прочих биомов Элисара,
 * регион появляется в случайных ячейках мира. В одном из них (ячейка 1,1 — «столица») стоит замок
 * «Чертог Тлеющего Рога» с боссом, в остальных — только выжженная равнина с лавовыми меандрами.
 */
public final class AshenLands {
    /** Сетка регионов адских угодий (как у садов и рощи — биом спавнится случайно по миру). */
    public static final int SUPER = 1600;
    private static final long FIXED = 0x0A5E71A9L;

    /** Один регион адских угодий. {@link #capital} — в нём стоит замок с боссом. */
    public static final class Region {
        public final int cx, cz;
        public final double R;
        public final boolean capital;

        Region(int cx, int cz, double R, boolean capital) {
            this.cx = cx;
            this.cz = cz;
            this.R = R;
            this.capital = capital;
        }
    }

    private static final java.util.concurrent.ConcurrentHashMap<Long, java.util.Optional<Region>> CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

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

    /** Регион адских угодий в ячейке сетки (или null, если ячейка пустая). */
    public static Region region(int sx, int sz) {
        return CACHE.computeIfAbsent(WildIslands.key(sx, sz), k -> java.util.Optional.ofNullable(gen(sx, sz))).orElse(null);
    }

    private static Region gen(int sx, int sz) {
        java.util.Random r = new java.util.Random(FIXED ^ (sx * 53982894593057L + sz * 317074912223L) * 0x9E3779B97F4A7C15L);
        boolean capital = sx == 1 && sz == 1;                        // столица — всегда (замок и босс)
        if (!capital && r.nextDouble() > 0.30) return null;
        int cx = sx * SUPER + 320 + r.nextInt(SUPER - 640);
        int cz = sz * SUPER + 320 + r.nextInt(SUPER - 640);
        double R = 205 + r.nextInt(55);
        return new Region(cx, cz, R, capital);
    }

    /** Столица (в ней замок и босс) и её координаты — их использует {@link CastleSite}. */
    public static final Region CAPITAL = region(1, 1);
    public static final int CX = CAPITAL.cx, CZ = CAPITAL.cz;
    public static final double R = CAPITAL.R;

    /** A huge island that fell onto the plain. */
    public record Mound(double x, double z, double r, double h, double tx, double tz, int seed) {
    }

    // смещения — от центра столицы (замок стоит в её середине)
    public static final List<Mound> MOUNDS = List.of(
            new Mound(CX - 160, CZ + 115, 46, 30, 0.45, -0.25, 1),       // ruined house on top
            new Mound(CX + 145, CZ + 120, 40, 26, -0.35, 0.4, 2),        // broken bridge
            new Mound(CX - 110, CZ - 152, 34, 22, 0.2, 0.5, 3));
    public static final List<CityPlan.Tree> TREES = new ArrayList<>();

    static {
        java.util.Random r = new java.util.Random(0xA5E0L);
        for (Mound m : MOUNDS) {
            int n = (int) (m.r * m.r / 140);
            for (int k = 0, tries = 0; k < n && tries < 200; ++tries) {
                double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * m.r * 0.8;
                int x = (int) Math.floor(m.x + Math.cos(a) * d), z = (int) Math.floor(m.z + Math.sin(a) * d);
                if (m.seed == 1 && Math.abs(x - m.x) < 9 && Math.abs(z - m.z) < 9) continue;
                if (m.seed == 2 && Math.abs(z - m.z) < 5) continue;
                boolean ok = true;
                for (CityPlan.Tree t : TREES) if (Math.abs(t.x - x) < 6 && Math.abs(t.z - z) < 6) { ok = false; break; }
                if (!ok) continue;
                CityPlan.Tree t = new CityPlan.Tree(x, z, 2, 5 + r.nextInt(6), r.nextInt(1 << 30));
                t.baseY = moundTop(x, z) + 1;
                if (t.baseY <= ocean() + 2) continue;
                TREES.add(t);
                ++k;
            }
        }
    }

    private AshenLands() {
    }

    static int ocean() {
        return CityPlan.get().ocean;
    }


    public static boolean inside(double x, double z) {
        return weight(x, z) > 0.5;
    }

    // ------------------------------------------------------------------ fallen islands
    /** Top Y of the fallen islands at (x, z), or Integer.MIN_VALUE where there is none. */
    public static int moundTop(int x, int z) {
        double best = -1;
        for (Mound m : MOUNDS) {
            double dx = x + 0.5 - m.x, dz = z + 0.5 - m.z;
            double wob = (ElisarChunkGenerator.vnoise(x * 0.06, z * 0.06, 90 + m.seed) - 0.5) * 0.35;
            double d = Math.hypot(dx, dz) / m.r + wob;
            if (d >= 1) continue;
            double h = m.h * Math.pow(1 - d * d, 0.55);
            h += (dx * m.tx + dz * m.tz) / m.r * m.h * 0.35 * (1 - d);             // tilted - it came down on one side
            h += (ElisarChunkGenerator.vnoise(x * 0.12, z * 0.12, 95 + m.seed) - 0.5) * 5 * (1 - d);
            if (m.seed == 1 && Math.abs(dx) < 9 && Math.abs(dz) < 9) h = m.h * 0.92; // flat yard for the house
            best = Math.max(best, Math.min(h, m.h * 1.1));
        }
        return best < 1.5 ? Integer.MIN_VALUE : ocean() + (int) Math.floor(best);
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
        // boulders
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
        // scattered ash heaps
        if (ElisarChunkGenerator.hash(x, z, 82) < 0.02) chunk.setBlockState(mp.set(x, o + 1, z), ash, false);
    }

    public static List<CityPlan.Tree> treesNear(int x, int z) {
        List<CityPlan.Tree> out = new ArrayList<>();
        for (CityPlan.Tree t : TREES) if (Math.abs(x - t.x) <= 4 && Math.abs(z - t.z) <= 4) out.add(t);
        return out;
    }

    // ------------------------------------------------------------------ ruined house + broken bridge (decoration pass)
    public record Placed(BlockPos pos, BlockState state) {
    }

    private static List<Placed> decor;

    public static synchronized List<Placed> decor() {
        if (decor == null) {
            List<Placed> out = new ArrayList<>();
            house(out, MOUNDS.get(0));
            bridge(out, MOUNDS.get(1));
            decor = out;
        }
        return decor;
    }

    private static BlockState s(net.minecraftforge.registries.RegistryObject<? extends net.minecraft.world.level.block.Block> b) {
        return b.get().defaultBlockState();
    }

    /** Former house: 9 x 7 shell of castle bricks with crumbled walls, an empty door and window, fallen roof beams. */
    private static void house(List<Placed> out, Mound m) {
        int x0 = (int) m.x - 4, z0 = (int) m.z - 3, y0 = moundTop((int) m.x, (int) m.z) + 1;
        java.util.Random r = new java.util.Random(0xB0B5E);
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
                if (z == z0 && (x == x0 + 4)) h = 0;                                   // doorway (north)
                for (int y = 0; y < h; ++y) {
                    if (z == z0 + 6 && x == x0 + 4 && y == 1) continue;                 // window
                    if (x == x0 && z == z0 + 3 && y == 1) continue;
                    BlockState b = corner ? ash : r.nextInt(3) == 0 ? cr : br;
                    out.add(new Placed(new BlockPos(x, y0 + y, z), b));
                }
                if (z == z0 && x == x0 + 4) {
                    out.add(new Placed(new BlockPos(x, y0 + 3, z), ash));                 // lintel survived
                    out.add(new Placed(new BlockPos(x - 1, y0, z), br));
                }
            }
        // fallen roof beams lying across the room, a slab of roof, rubble outside
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
            out.add(new Placed(new BlockPos(x, moundTop(x, z) + 1, z), r.nextBoolean() ? cr : s(CastleBlocks.BRICKS_SLAB)));
        }
        // chimney stub
        for (int y = 0; y < 6; ++y) out.add(new Placed(new BlockPos(x0 + 8, y0 + y, z0 + 5), y > 3 ? cr : br));
        out.add(new Placed(new BlockPos(x0 + 7, y0, z0 + 5), Blocks.AIR.defaultBlockState()));
    }

    /** Broken bridge: a stone abutment on the fallen island and a plank deck on posts that ends in mid-air; the rest lies below. */
    private static void bridge(List<Placed> out, Mound m) {
        int bz = (int) m.z;
        int xa = (int) (m.x - m.r * 0.45);
        int ya = moundTop(xa, bz) + 1;
        BlockState ashl = s(CastleBlocks.ASHLAR), plank = s(CastleBlocks.DARK_PLANKS), fence = s(CastleBlocks.DARK_FENCE);
        BlockState slab = s(CastleBlocks.DARK_SLAB);
        BlockState chainY = s(ElisarBlocks.CHAIN).setValue(ChainBlock.AXIS, Direction.Axis.Y);
        // abutment
        for (int dz = -2; dz <= 2; ++dz)
            for (int y = ya - 3; y <= ya; ++y) out.add(new Placed(new BlockPos(xa, y, bz + dz), ashl));
        out.add(new Placed(new BlockPos(xa, ya + 1, bz - 2), ashl));
        out.add(new Placed(new BlockPos(xa, ya + 1, bz + 2), ashl));
        out.add(new Placed(new BlockPos(xa, ya + 2, bz - 2), s(ElisarBlocks.LANTERN)));
        // deck westwards, sagging, with gaps and a ragged end
        int len = 16;
        for (int k = 1; k <= len; ++k) {
            int x = xa - k;
            int y = ya - (int) Math.round(Math.sin(Math.PI * k / 30.0) * 2.2);
            for (int dz = -1; dz <= 1; ++dz) {
                if (k > len - 3 && (dz + k) % 3 == 0) continue;                  // ragged end
                if (k == 9 && dz == 0) continue;                                  // missing plank
                out.add(new Placed(new BlockPos(x, y, bz + dz), k > len - 2 ? slab.setValue(SlabBlock.TYPE, SlabType.TOP) : plank));
            }
            if (k % 5 == 0 && k < len - 2) {
                for (int dz : new int[] {-2, 2}) {
                    out.add(new Placed(new BlockPos(x, y, bz + dz), plank));
                    out.add(new Placed(new BlockPos(x, y + 1, bz + dz), fence));
                    out.add(new Placed(new BlockPos(x, y + 2, bz + dz), fence));
                }
                // pier down to the plain
                for (int y2 = ocean() + 1; y2 < y; ++y2) out.add(new Placed(new BlockPos(x, y2, bz), k == 10 && y2 > y - 4 ? Blocks.AIR.defaultBlockState() : s(CastleBlocks.DARK_BEAM)));
            }
            if (k == len - 1) for (int y2 = y - 1; y2 > y - 5; --y2) out.add(new Placed(new BlockPos(x, y2, bz + 1), chainY));   // dangling chain
        }
        // fallen pieces on the plain below the gap
        int o = ocean();
        for (int k = 0; k < 7; ++k) {
            int x = xa - len - 2 - k, z = bz - 2 + (k * 5) % 5;
            if (!meander(x, z)) out.add(new Placed(new BlockPos(x, o + 1, z), k % 2 == 0 ? plank : slab));
        }
        out.add(new Placed(new BlockPos(xa - len - 4, o + 1, bz + 2), s(CastleBlocks.DARK_BEAM).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X)));
        out.add(new Placed(new BlockPos(xa - len - 5, o + 1, bz + 2), s(CastleBlocks.DARK_BEAM).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X)));
    }
}
