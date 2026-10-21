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
 * «Адские угодья» (Ashen Lands, lightweight first version): a scorched region of ~235 blocks around the castle island.
 * Below the islands the ocean is replaced by an ash plain cut by glowing lava meanders; three huge islands lie fallen on
 * the plain (one carries a ruined house, one a broken bridge); floating islands inside the region are burnt, with bare
 * dead trees; the air is ash-brown (ElisarSky fog + biome particles).
 */
public final class AshenLands {
    public static final double CX = CastleSite.CX, CZ = CastleSite.CZ, R = 235;

    /** A huge island that fell onto the plain. */
    public record Mound(double x, double z, double r, double h, double tx, double tz, int seed) {
    }

    public static final List<Mound> MOUNDS = List.of(
            new Mound(-120, 585, 46, 30, 0.45, -0.25, 1),       // ruined house on top
            new Mound(185, 590, 40, 26, -0.35, 0.4, 2),         // broken bridge
            new Mound(-70, 318, 34, 22, 0.2, 0.5, 3));
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

    /** 0 outside .. 1 inside, with a wobbly edge. */
    public static double weight(double x, double z) {
        double dx = x - CX, dz = z - CZ;
        if (Math.abs(dx) > R + 60 || Math.abs(dz) > R + 60) return EmberRuins.weight(x, z);
        double d = Math.hypot(dx, dz) + (ElisarChunkGenerator.vnoise(x * 0.015, z * 0.015, 81) - 0.5) * 40;
        return Math.max(0, Math.min(1, (R - d) / 30.0));
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
            // Rev 7: the old block-stair bridge was replaced by a four-post burnt suspension bridge
            // (bridge/WorldBridges.java, style BURNT with a collapsed span) - only the fallen debris stays here.
            bridgeDebris(out, MOUNDS.get(1));
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

    /** Fallen debris of the old bridge on the plain: planks, a snapped beam and a dangling chain under the new span. */
    private static void bridgeDebris(List<Placed> out, Mound m) {
        int bz = (int) m.z + 6;
        int xa = (int) m.x + 44;
        BlockState plank = s(CastleBlocks.DARK_PLANKS);
        BlockState slab = s(CastleBlocks.DARK_SLAB);
        BlockState chainY = s(ElisarBlocks.CHAIN).setValue(ChainBlock.AXIS, Direction.Axis.Y);
        // fallen pieces on the plain below the gap
        int o = ocean();
        for (int k = 0; k < 7; ++k) {
            int x = xa - 4 - k, z = bz - 2 + (k * 5) % 5;
            if (!meander(x, z)) out.add(new Placed(new BlockPos(x, o + 1, z), k % 2 == 0 ? plank : slab));
        }
        out.add(new Placed(new BlockPos(xa - 9, o + 1, bz + 2), s(CastleBlocks.DARK_BEAM).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X)));
        out.add(new Placed(new BlockPos(xa - 10, o + 1, bz + 2), s(CastleBlocks.DARK_BEAM).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X)));
    }
}
