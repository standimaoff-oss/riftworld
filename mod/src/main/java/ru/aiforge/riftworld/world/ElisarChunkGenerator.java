package ru.aiforge.riftworld.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import ru.aiforge.riftworld.block.ModelBlock;
import ru.aiforge.riftworld.block.ModelKind;
import ru.aiforge.riftworld.registry.ElisarBlocks;
import ru.aiforge.riftworld.registry.ModBlocks;

/**
 * Planet Elisar: floating sky islands over a deep open ocean (40-50 blocks, currents, sea mist). Everything is analytic and deterministic
 * (independent of the world seed) so the city «Странники» from plan.json is always identical.
 */
public class ElisarChunkGenerator extends ChunkGenerator {
    public static final Codec<ElisarChunkGenerator> CODEC = RecordCodecBuilder.create(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)).apply(i, i.stable(ElisarChunkGenerator::new)));

    static final int MIN_Y = 0, HEIGHT = 256;

    public ElisarChunkGenerator(BiomeSource src) {
        super(src);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    // ================================================================== column model
    /** Surface kinds. */
    static final int S_GRASS = 0, S_COBBLE = 1, S_PATH = 2, S_PLANK = 3, S_PLOT = 4;

    static final class Col {
        CityPlan.Island isl;
        int top = Integer.MIN_VALUE, bottom;
        int surface = S_GRASS;
        boolean plot;
        // water
        int waterTop = Integer.MIN_VALUE, waterBottom;      // still water (river / pond / spring mouth)
        int fallTop = Integer.MIN_VALUE, fallBottom;        // falling water column
        // bridge
        int deckY = Integer.MIN_VALUE;
        boolean rail, post, railX;
        boolean fence;            // rim fence of the city terraces
        boolean garden;           // biome 07 zone (terraced hub or its sea fringe)
        boolean mtn;              // rev 9: многоуровневые горы (свои ярусы, пруды и водопады)
        TerracedMountains.Region mtnRegion;
        boolean pond, pondRim, springy;   // biome 07 terrace pools and hot-spring cores
    }

    static double hash(int x, int z, int s) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ s * 0x165667B19E3779F9L;
        h ^= h >>> 33; h *= 0xFF51AFD7ED558CCDL; h ^= h >>> 33; h *= 0xC4CEB9FE1A85EC53L; h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }

    static double vnoise(double x, double z, int s) {
        int xi = (int) Math.floor(x), zi = (int) Math.floor(z);
        double fx = x - xi, fz = z - zi;
        fx = fx * fx * (3 - 2 * fx); fz = fz * fz * (3 - 2 * fz);
        double a = hash(xi, zi, s), b = hash(xi + 1, zi, s), c = hash(xi, zi + 1, s), d = hash(xi + 1, zi + 1, s);
        return (a + (b - a) * fx) + ((c + (d - c) * fx) - (a + (b - a) * fx)) * fz;
    }

    static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    /** Floating debris (reference): swarms of small cubes - mostly single blocks, some 2x2 - around every rim and the tower. */
    private static void debris(CityPlan plan, ChunkAccess chunk, BlockPos.MutableBlockPos mp, int x, int z) {
        int gx = Math.floorDiv(x, 3), gz = Math.floorDiv(z, 3);
        double pick = hash(gx, gz, 91);
        boolean big = hash(gx, gz, 97) < 0.22;
        int lx = Math.floorMod(x, 3) - (int) (hash(gx, gz, 94) * (big ? 2 : 3)), lz = Math.floorMod(z, 3) - (int) (hash(gx, gz, 95) * (big ? 2 : 3));
        if (lx < 0 || lz < 0 || lx > (big ? 1 : 0) || lz > (big ? 1 : 0)) return;
        double cx = gx * 3 + 1.5, cz = gz * 3 + 1.5;
        double towerD = Math.hypot(cx - TOWER_X, cz - TOWER_Z);
        int y0;
        if (towerD < 34 && towerD > 8 && pick < 0.10) {
            y0 = (int) (TOWER_Y0 + hash(gx, gz, 92) * 95);
        } else {
            CityPlan.Island n = plan.nearest(cx, cz);
            if (n == null) return;
            double t = n.t(cx, cz);
            if (t > -0.015 || t < -0.5) return;
            double p = 0.16 * (1 + t * 2.2);                 // denser right at the rim, thinning outwards
            if (pick > p || plan.islandAt(cx, cz) != null) return;
            y0 = (int) Math.floor(n.base - 22 + hash(gx, gz, 92) * 34);
        }
        double k = hash(gx, gz, 93);
        BlockState s = k < 0.3 ? st(ElisarBlocks.CRUST) : k < 0.55 ? st(ElisarBlocks.STONE) : k < 0.7 ? st(ElisarBlocks.COBBLE)
                : k < 0.82 ? st(ElisarBlocks.GRASS) : k < 0.92 ? st(ElisarBlocks.STONE_MOSSY) : st(ElisarBlocks.TOWER_BRICKS);
        int h = big ? 2 : 1;
        for (int y = y0; y < y0 + h; ++y) chunk.setBlockState(mp.set(x, y, z), s, false);
    }

    // ------------------------------------------------------------------ the Inverted Tower (Rev 4 items 5+6): hangs upside-down high over the
    // deep ocean gap west of the main island - the spike and the upside-down battlement deck point at the sea, the torn
    // root crown is on top. A WHOLE enterable structure: spike reliquary with the treasure, round deck hall, shaft with
    // three landings, roof hatch. Lit by lanterns; ladders link every level. EXISTS ONLY HERE (riftworld:elisar).
    static final int TOWER_X = -104, TOWER_Z = 22, TOWER_Y0 = 148;

    private static double oct(double dx, double dz) {
        double ax = Math.abs(dx), az = Math.abs(dz);
        return Math.max(Math.max(ax, az), (ax + az) * 0.7071);
    }

    /** Decor cells of the tower interior (world coords are TOWER_X + entry[0], TOWER_Y0 + entry[1], TOWER_Z + entry[2]). */
    static final String[][] TOWER_DECOR = {
        {"gold_hoard",     "0",  "10", "0",  "south", "false"},   // spike reliquary (tapered: only the core cells are open at floor level)
        {"castle_chest",   "0",  "10", "-1", "south", "false"},
        {"candle_stand",   "-1", "10", "0",  "east",  "true"},
        {"castle_chest",   "-4", "23", "-4", "east",  "false"},   // deck hall
        {"castle_bench",   "4",  "23", "-3", "west",  "false"},
        {"chained_lectern","0",  "23", "5",  "north", "false"},
        {"wall_sconce",    "-5", "24", "0",  "east",  "true"},
        {"wall_sconce",    "5",  "24", "0",  "west",  "true"},
        {"wall_sconce",    "0",  "24", "-5", "south", "true"},
        {"castle_chest",   "-2", "41", "0",  "east",  "false"},   // shaft landing 1
        {"wall_sconce",    "0",  "42", "-3", "south", "true"},
        {"gold_hoard",     "0",  "57", "-1", "south", "false"},   // shaft landing 2
        {"candle_stand",   "-2", "57", "1",  "east",  "true"},
        {"wall_sconce",    "0",  "58", "-3", "south", "true"},
        {"castle_chest",   "-2", "73", "-1", "east",  "false"},   // shaft landing 3
        {"wall_sconce",    "0",  "74", "-3", "south", "true"},
    };

    private static void tower(ChunkAccess chunk, BlockPos.MutableBlockPos mp, int x, int z) {
        int ix = x - TOWER_X, iz = z - TOWER_Z;
        if (Math.abs(ix) > 10 || Math.abs(iz) > 10) return;
        double dx = ix + 0.5, dz = iz + 0.5;
        double d = oct(dx, dz);
        BlockState br = st(ElisarBlocks.TOWER_BRICKS), cr = st(ElisarBlocks.TOWER_BRICKS_CRACKED);
        BlockState lant = st(ElisarBlocks.LANTERN);
        int y0 = TOWER_Y0;
        // ladder wells: EAST well (cell ix 2, iz 0) pierces every landing and the roof and climbs the shaft on the spine
        // rib (ix 3); SOUTH drop links the deck hall with the spike reliquary (cells ix 0, iz 1..3, ladder on the rib ix 0, iz 4).
        boolean eastOpen = ix == 2 && iz == 0;
        boolean southOpen = ix == 0 && (iz == 2 || iz == 3);
        boolean southChute = ix == 0 && iz >= 1 && iz <= 3;
        boolean ribE = ix == 3 && iz == 0, ribS = ix == 0 && iz == 4;
        for (int y = y0; y <= y0 + 101 && y < MIN_Y + HEIGHT; ++y) {
            int ly = y - y0;
            boolean solid = false;
            if (ly <= 5) {                                         // spike tip
                double r = 6.2 * ly / 22.0;
                solid = d <= r + 0.3;
            } else if (ly < 22) {                                  // spike shell, hollow inside -> the reliquary
                double r = 6.2 * ly / 22.0;
                solid = d <= r + 0.3 && d > Math.max(r - 1.7, 0);
                if (ly == 9) solid = d <= r + 0.3 || southOpen;    // reliquary floor (plugs the drop chute)
                if (ribS && ly >= 10) solid = true;                // ladder rib
                if (solid && southChute && ly >= 10 && ly != 9) solid = false;  // the drop chute through the shell
            } else if (ly == 22) {                                 // upside-down battlement deck floor (full disc)
                solid = d <= 7.6;
                if (southOpen) solid = false;                      // reliquary hatch in the floor
            } else if (ly < 27) {                                  // deck hall wall ring with merlons on the top row
                solid = d <= 7.6 && d > 6.4;
                if (ly == 26 && solid && ((int) Math.floor((Math.atan2(dz, dx) + Math.PI) / (Math.PI / 10)) & 1) == 0) solid = false;
                if (ribE) solid = true;   // shaft ladder spine rib continues through the hall (the shaft wall owns it higher up)
            } else if (ly == 27) {                                 // hall ceiling under the corbels
                solid = d <= 7.3;
                if (eastOpen) solid = false;
            } else if (ly < 32) {                                  // corbels stepping back to the shaft
                double r = 7.2 - (ly - 28) * 0.4;
                solid = d <= r && d > 3.4;
            } else if (ly < 86) {                                  // hollow shaft
                solid = d <= 5.3 && d > 3.4;
                boolean slitRow = (ly - 36) % 13 < 3;
                boolean slit = Math.abs(ix) == 0 || Math.abs(iz) == 0;
                if (solid && slitRow && slit && d > 4.4) solid = false;
                if (ly % 13 == 8 && d <= 5.9 && d > 4.6) solid = true;   // string course
                if ((ly == 40 || ly == 56 || ly == 72) && d <= 3.4) solid = true;   // the three landings
                if ((ly == 40 || ly == 56 || ly == 72) && eastOpen) solid = false;  // hatch in every landing
                if (ly >= 82) solid = d <= 6.6 && d > 3.4;         // upper ring
            } else if (ly == 86) {                                 // roof disc with the entry hatch
                solid = d <= 6.6;
                if (eastOpen) solid = false;
            } else if (ly == 87) {                                 // torn root crown: jagged ring
                solid = d <= 6.6 && d > 5.4 && ((ix + 3 * iz) % 5 != 0);
            } else {                                               // root spire shell
                double r = 6.6 * (1 - (ly - 87) / 15.0);
                if (r > 0.2) solid = d <= r && d > r - 1.7;
            }
            if (!solid) {
                // interior dressing that lives INSIDE the air cells: the east ladder well and the south drop ladder
                if ((ix == 2 && iz == 0 && ly >= 23 && ly <= 85) || (ix == 0 && iz == 3 && ly >= 10 && ly <= 21)) {
                    chunk.setBlockState(mp.set(x, y, z), net.minecraft.world.level.block.Blocks.LADDER.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.LadderBlock.FACING, ix == 2 ? net.minecraft.core.Direction.WEST : net.minecraft.core.Direction.NORTH), false);
                } else if (ix == 0 && iz == 0 && (ly == 41 || ly == 57 || ly == 73)) {
                    chunk.setBlockState(mp.set(x, y, z), lant, false);   // sitting lanterns on each landing
                } else if (ix == 0 && iz == 0 && ly == 26 || ix == -1 && iz == 1 && ly == 21) {
                    chunk.setBlockState(mp.set(x, y, z), lant.setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), false);
                }
                continue;
            }
            chunk.setBlockState(mp.set(x, y, z), hash(x * 7 + y, z, 31) < 0.18 ? cr : br, false);
        }
    }

    /** Seabed height: gentle dunes around ocean-45 so the water column is 40-50 blocks deep everywhere (Rev 4 item 6).
     *  Рев 8: плюс ущелья — дно местами прорезано на 6–12 блоков вглубь (каньоны и желоба). */
    static int oceanFloor(CityPlan plan, int x, int z) {
        double n = vnoise(x * 0.016, z * 0.016, 77) * 0.72 + vnoise(x * 0.052, z * 0.052, 78) * 0.28;
        return Math.max(9, plan.ocean - 1 - 41 - (int) Math.round(n * 8) - ElisarSea.trenchDepth(x, z));
    }

    private static boolean land(CityPlan plan, double px, double pz) {
        CityPlan.Island i = plan.islandAt(px, pz);
        return i != null;
    }

    static Col column(CityPlan plan, int x, int z) {
        Col c = new Col();
        double px = x + 0.5, pz = z + 0.5;
        CityPlan.Bucket bk = plan.at(x, z);
        CityPlan.Island isl = plan.islandAt(px, pz);
        c.isl = isl;
        double[] tt = new double[1];
        double top = 0;
        if (isl != null) {
            double raw = isl.raw(px, pz);
            top = raw;
            // plots: flatten (nearest plot wins), blend over the margin
            double best = 1e9;
            for (CityPlan.Plot p : bk.plots) {
                double dx = Math.max(Math.max(p.x0() - px + 0.5, 0), px - 0.5 - (p.x0() + p.w() - 1));
                double dz = Math.max(Math.max(p.z0() - pz + 0.5, 0), pz - 0.5 - (p.z0() + p.d() - 1));
                double d = Math.hypot(dx, dz);
                if (d <= p.margin() + 0.5 && d < best) {
                    best = d;
                    if (d <= 0) {
                        top = p.h();
                        c.plot = true;
                    } else {
                        top = p.h() + (raw - p.h()) * smooth(d / (p.margin() + 1.0));
                    }
                }
            }
            if (isl.kind == 7) {                                       // Фиолетовые холмы: настоящий рельеф (rev 9)
                VioletHills.Region vh = VioletHills.regionAt(px, pz);
                if (vh != null && vh.meadow == isl) top = vh.hills(px, pz);
            } else if (isl.kind == TerracedMountains.KIND) {           // Многоуровневые горы: ярусы-полки (rev 9)
                TerracedMountains.Region tm = TerracedMountains.regionAt(px, pz);
                if (tm != null && tm.massif == isl) {
                    top = TerracedMountains.top(tm, px, pz);
                    c.mtn = true;
                    c.mtnRegion = tm;
                }
            }
            if (isl.kind == 5) {                                       // castle plateau
                double o = CastleSite.outside(px, pz);
                top = o <= 0 ? CastleSite.Y : CastleSite.Y + (top - CastleSite.Y) * smooth(o / 9.0);
            } else if (isl.kind == 6) {                                // ruined chapel plateau
                EmberRuins.Site es = EmberRuins.siteAt(px, pz);
                if (es != null) {
                    double o = es.outside(px, pz);
                    top = o <= 0 ? es.y : es.y + (top - es.y) * smooth(o / 6.0);
                }
            } else if (isl.kind == 7) {                                // violet meadow: plateaus under the two churches
                VioletHills.Region vr = VioletHills.regionAt(px, pz);
                if (vr != null && vr.meadow == isl) {
                    double oA = vr.churchA.outside(px, pz), oB = vr.churchB.outside(px, pz);
                    double o = Math.min(oA, oB);
                    int fy = oA <= oB ? vr.churchA.y : vr.churchB.y;
                    if (o < 7) top = o <= 0 ? fy : fy + (top - fy) * smooth(o / 7.0);
                }
            }
            else if (isl.kind == SizoGrove.KIND) {                     // sizo grove: terrace under the rare ruined temple
                SizoGrove.Region sr = SizoGrove.regionAt(px, pz);
                if (sr != null && sr.temple && sr.hub == isl) {
                    double o = Math.max(Math.abs(px - sr.tx), Math.abs(pz - sr.tz)) - 8.0;
                    if (o < 7) top = o <= 0 ? sr.ty : sr.ty + (top - sr.ty) * smooth(o / 7.0);
                }
            }
            else if (isl.kind == WaterfallGardens.KIND) {              // biome 07: limestone terraces stepping to the sea
                top = terraceTop(top, plan.ocean);
            }
            c.top = (int) Math.floor(top);
            // rev 9: пруды на полках и водопады с кромок ярусов
            if (c.mtn && c.mtnRegion != null) {
                if (TerracedMountains.pondAt(c.mtnRegion, px, pz, top) && c.top > plan.ocean + 6) {
                    int ct = c.top;
                    c.waterTop = ct;
                    c.waterBottom = ct - 2;
                    c.top = ct - 3;
                    c.pond = true;
                } else if (c.waterTop == Integer.MIN_VALUE) {
                    double hi = 0;
                    for (int dk = 0; dk < 4; ++dk) {
                        double nx = px + (dk == 0 ? 1 : dk == 1 ? -1 : 0), nz = pz + (dk == 2 ? 1 : dk == 3 ? -1 : 0);
                        double nt = TerracedMountains.top(c.mtnRegion, nx, nz);
                        if (nt > hi) hi = nt;
                    }
                    if (hi > c.top + 4.5 && vnoise(px * 0.31, pz * 0.31, 709) > 0.58) {
                        c.fallTop = (int) Math.floor(hi);
                        c.fallBottom = c.top + 1;
                    }
                }
            }
            // biome 07: terrace pools / hot springs carve back into the shelves
            WaterfallGardens.Region gr = WaterfallGardens.regionAt(px, pz);
            if (isl.kind == WaterfallGardens.KIND && gr != null && gr.hub == isl) {
                c.garden = true;
                int ct = c.top;
                double pn = vnoise(px * 0.09, pz * 0.09, 131);
                if (pondAt(plan, gr, px, pz, ct)) {
                    int pd = pn > 0.75 ? 3 : 2;
                    c.waterTop = ct;
                    c.waterBottom = ct - pd + 1;
                    c.top = ct - pd;
                    c.pond = true;
                    c.springy = hash(x, z, 132) < 0.30;       // geothermal core: warm glow under the water
                } else {
                    c.pondRim = pondAt(plan, gr, px + 1, pz, ct) || pondAt(plan, gr, px - 1, pz, ct)
                            || pondAt(plan, gr, px, pz + 1, ct) || pondAt(plan, gr, px, pz - 1, ct);
                }
            } else if (isl.kind != 4 && WaterfallGardens.inside(px, pz)) {
                c.garden = true;                                            // islet fringe inside the biome blob
            }
            double t = isl.t(px, pz);
            double depth = 3 + isl.depth * Math.pow(Math.max(t, 0), 0.55) * (0.72 + 0.28 * vnoise(px * 0.07, pz * 0.07, 11));
            double stal = Math.max(0, vnoise(px * 0.21, pz * 0.21, 12) - 0.6) * 48 * Math.pow(Math.max(t, 0), 0.3);
            c.bottom = Math.max(plan.ocean + 6, (int) Math.floor(c.top - depth - stal));
            c.bottom = Math.min(c.bottom, c.top - 2);
            // roads & plazas
            double rd = 1e9;
            int rk = -1;
            double rw = 0;
            for (CityPlan.Road r : bk.roads) {
                double d = CityPlan.segDist(px, pz, r.ax(), r.az(), r.bx(), r.bz(), null) - r.w() / 2;
                if (d < rd) { rd = d; rk = r.kind(); rw = r.w(); }
            }
            for (CityPlan.Plaza p : bk.plazas) {
                double d = Math.hypot(px - p.x(), pz - p.z()) - p.r();
                if (d < rd) { rd = d; rk = p.kind(); rw = 6; }
            }
            if (rk >= 0 && rd <= 0.15) {
                double edge = -rd;            // distance inside the road edge
                if (rk == 0) c.surface = edge < 0.7 && hash(x, z, 3) < 0.45 ? S_PATH : S_COBBLE;
                else if (rk == 1) c.surface = edge < 0.6 && hash(x, z, 4) < 0.35 ? S_GRASS : S_PATH;
                else c.surface = S_PLANK;
            }
            if (isl.kind == 7 && c.surface == S_GRASS) {               // violet meadow: footpath church <-> church
                VioletHills.Region vr = VioletHills.regionAt(px, pz);
                if (vr != null && vr.meadow == isl) {
                    double pd = vr.pathDist(px, pz);
                    if (pd <= 1.05 && !(pd < 0.5 && hash(x, z, 54) < 0.16)) c.surface = S_PATH;   // grass creeps back onto the centre line
                }
            }
            if (c.plot) c.surface = S_PLOT;
            if (isl.kind == 0 && !c.plot && c.surface == S_GRASS && t >= 0.05 && (!bk.plots.isEmpty() || rd < 9)) {
                double tm = Math.min(Math.min(isl.t(px + 1, pz), isl.t(px - 1, pz)), Math.min(isl.t(px, pz + 1), isl.t(px, pz - 1)));
                c.fence = tm < 0.05;
            }
        }
        // bridges
        for (CityPlan.Bridge b : bk.bridges) {
            double d = CityPlan.segDist(px, pz, b.ax(), b.az(), b.bx(), b.bz(), tt);
            if (d > b.hw() + 1.05 || tt[0] <= 0 && d > b.hw() || tt[0] >= 1 && d > b.hw()) continue;
            int y = b.deckY(tt[0]);
            if (d <= b.hw()) {
                c.deckY = y;
                c.rail = false;
            } else if (c.deckY == Integer.MIN_VALUE) {
                c.deckY = y;
                c.rail = true;
                int step = (int) Math.floor(tt[0] * b.length());
                c.post = step <= 0 || step >= (int) Math.floor(b.length()) - 1;
                c.railX = Math.abs(b.bx() - b.ax()) >= Math.abs(b.bz() - b.az());
            }
            if (isl != null && c.top >= y) {
                c.top = y;
                c.surface = S_PLANK;
                c.bottom = Math.min(c.bottom, y - 2);
            }
        }
        // biome 07: the rare sagging plank bridges of the waterfall gardens
        WaterfallGardens.Region grb = c.isl != null && c.isl.kind == WaterfallGardens.KIND ? WaterfallGardens.regionAt(px, pz) : null;
        if (grb != null)
            for (CityPlan.Bridge b : grb.bridges) {
                double d = CityPlan.segDist(px, pz, b.ax(), b.az(), b.bx(), b.bz(), tt);
                if (d > b.hw() + 1.05 || tt[0] <= 0 && d > b.hw() || tt[0] >= 1 && d > b.hw()) continue;
                int y = b.deckY(tt[0]);
                if (d <= b.hw()) {
                    c.deckY = y;
                    c.rail = false;
                } else if (c.deckY == Integer.MIN_VALUE) {
                    c.deckY = y;
                    c.rail = true;
                    int step = (int) Math.floor(tt[0] * b.length());
                    c.post = step <= 0 || step >= (int) Math.floor(b.length()) - 1;
                    c.railX = Math.abs(b.bx() - b.ax()) >= Math.abs(b.bz() - b.az());
                }
                if (c.isl != null && c.top >= y - 1) {
                    c.top = y;
                    c.surface = S_PLANK;
                    c.bottom = Math.min(c.bottom, y - 2);
                }
            }
        // water channels (river, pond, springs)
        for (CityPlan.Channel ch : bk.channels)
            applyChannel(c, plan, px, pz, ch.ax(), ch.az(), ch.bx(), ch.bz(), ch.hw(), ch.level(), ch.depth(), tt);
        // biome 04: the violet meadow river - a chord across the island that spills off both rims
        VioletHills.Region vr = VioletHills.regionAt(px, pz);
        if (vr != null)
            applyChannel(c, plan, px, pz, vr.rax, vr.raz, vr.rbx, vr.rbz, VioletHills.Region.RIVER_HW, vr.riverLevel, 2, tt);
        // biome 07: waterfalls - this column is low while a garden shelf with a pool/spring looms beside it
        if (c.fallTop == Integer.MIN_VALUE && c.waterTop == Integer.MIN_VALUE && c.deckY == Integer.MIN_VALUE) {
            WaterfallGardens.Region grf = WaterfallGardens.regionAt(px, pz);
            if (grf != null) {
                boolean rim = grf.hub.t(px, pz) < 0.1;
                for (int gdk = 0; gdk < 4; ++gdk) {
                    double gx = px + (gdk == 0 ? 1 : gdk == 1 ? -1 : 0), gz = pz + (gdk == 2 ? 1 : gdk == 3 ? -1 : 0);
                    if (grf.hub.t(gx, gz) < 0.12) continue;
                    double gnt = terraceTop(grf.hub.raw(gx, gz), plan.ocean);
                    if (gnt <= c.top + (rim ? 2.0 : 3.5)) continue;
                    // rim takes wide pond spills and broad springs; interior only real pond lips and sharp springs
                    boolean feed = vnoise(gx * 0.09, gz * 0.09, 131) > (rim ? 0.66 : 0.685)
                            || vnoise(gx * 0.13, gz * 0.13, 141) > (rim ? 0.72 : 0.765);
                    if (!feed) continue;
                    c.fallTop = (int) Math.floor(gnt);
                    c.fallBottom = c.isl != null ? c.top + 1 : plan.ocean + 1;
                    break;
                }
            }
        }
        return c;
    }

    /** Biome 07 terrace transform: horizontal limestone shelves with ~1-block steep rises, lowest shelf hugging the sea. */
    private static double terraceTop(double top, int ocean) {
        double h = top - (ocean + 1);
        if (h <= 0) return top;
        double L = Math.floor(h / WaterfallGardens.STEP), band = h - L * WaterfallGardens.STEP;
        double rise = band <= 1.2 ? smooth(band / 1.2) : 1.0;
        return ocean + 1 + (L + rise) * WaterfallGardens.STEP;
    }

    /** Terraced top estimate of a garden column (bridge anchor heights in WaterfallGardens). */
    public static int gardenTerraceTop(CityPlan.Island isl, double px, double pz, int ocean) {
        return (int) Math.floor(terraceTop(isl.raw(px, pz), ocean));
    }

    /** Biome 07 pond-mask test stateless over garden region: shelf cells the pond noise claims. */
    private static boolean pondAt(CityPlan plan, WaterfallGardens.Region rg, double px, double pz, int maxTop) {
        if (vnoise(px * 0.09, pz * 0.09, 131) <= 0.685) return false;
        if (Math.hypot(px - rg.cx, pz - rg.cz) > rg.hub.R * 0.95) return false;
        return maxTop > plan.ocean + 3 && maxTop < plan.ocean + 46;
    }

    /** Carves one straight water channel into the column (shared by city rivers and the violet meadow river). */
    private static void applyChannel(Col c, CityPlan plan, double px, double pz, double ax, double az, double bx, double bz,
                                     double hw, int L, int chDepth, double[] tt) {
        CityPlan.Island isl = c.isl;
        double d = CityPlan.segDist(px, pz, ax, az, bx, bz, tt);
        if (d > hw) {
            // banks: keep the channel walls up to the very rim (the lowered rim lip would otherwise leave water hanging)
            if (isl != null && d <= hw + (chDepth > 1 ? 3.5 : 1.5) && c.top < L + 1) { c.top = L + 1; c.bottom = Math.min(c.bottom, c.top - 3); }
            return;
        }
        if (isl != null && c.top < L + 1) { c.top = L + 1; c.bottom = Math.min(c.bottom, c.top - 3); }
        if (isl != null) {
            int depth = chDepth == 1 ? 1 : (d < hw - 1 ? chDepth : Math.max(1, chDepth - 1));
            c.waterTop = L;
            c.waterBottom = L - depth + 1;
            c.top = Math.min(c.top, c.waterBottom - 1);       // bed under the water; air above
            if (c.surface == S_COBBLE || c.surface == S_PATH || c.surface == S_PLANK) c.deckY = Math.max(c.deckY, L + 1);  // ford -> plank bridge
            c.surface = S_GRASS;
            c.bottom = Math.min(c.bottom, c.top - 2);
        } else {
            // the fall starts right at the island edge: only the first void column next to land carries falling water,
            // the channel never "hangs" over the void beyond the rim
            boolean overLand = isl != null;
            if (!overLand && !(land(plan, px + 1, pz) || land(plan, px - 1, pz) || land(plan, px, pz + 1) || land(plan, px, pz - 1))) return;
            c.fallTop = L;
            c.fallBottom = overLand ? c.top + 1 : plan.ocean + 1;
        }
    }

    // ================================================================== blocks
    private static BlockState st(net.minecraftforge.registries.RegistryObject<? extends net.minecraft.world.level.block.Block> b) {
        return b.get().defaultBlockState();
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor exec, Blender blender, RandomState rs, StructureManager sm, ChunkAccess chunk) {
        CityPlan plan = CityPlan.get();
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        BlockState water = st(ElisarBlocks.WATER);
        BlockState falling = water.setValue(LiquidBlock.LEVEL, 8);
        BlockState seabed = st(ElisarBlocks.SEABED), stone = st(ElisarBlocks.STONE), mossy = st(ElisarBlocks.STONE_MOSSY), soil = st(ElisarBlocks.SOIL);
        BlockState grass = st(ElisarBlocks.GRASS), cobble = st(ElisarBlocks.COBBLE), path = st(ElisarBlocks.PATH), plank = st(ElisarBlocks.SIZO_PLANKS);
        BlockState tall = st(ElisarBlocks.TALL_GRASS), flowers = st(ElisarBlocks.FLOWERS), log = st(ElisarBlocks.SIZO_LOG);
        BlockState moss = st(ElisarBlocks.MOSS), crust = st(ElisarBlocks.CRUST);
        BlockState ashlar = st(ru.aiforge.riftworld.registry.CastleBlocks.ASHLAR);
        BlockState aurite = st(ElisarBlocks.AURITE_ORE);
        Col[][] cols = new Col[18][18];
        for (int dx = -1; dx <= 16; ++dx)
            for (int dz = -1; dz <= 16; ++dz)
                cols[dx + 1][dz + 1] = column(plan, x0 + dx, z0 + dz);
        for (int dx = 0; dx < 16; ++dx) {
            for (int dz = 0; dz < 16; ++dz) {
                int x = x0 + dx, z = z0 + dz;
                Col c = cols[dx + 1][dz + 1];
                // ocean plane
                double aw = AshenLands.weight(x + 0.5, z + 0.5);
                boolean ashen = aw > 0 && aw > hash(x, z, 88);
                if (ashen) {
                    if (c.isl == null || c.bottom > plan.ocean + 1) AshenLands.column(chunk, mp, x, z, c.isl != null);
                } else if (c.isl == null || c.bottom > plan.ocean + 1) {
                    // Рев 8 «Живое море»: сплошной массив дна от y=0, ущелья, двухуровневые пещеры,
                    // «синие дыры» и рифовые сады (было: 5-блочная скорлупа и лысое дно).
                    int floor = oceanFloor(plan, x, z);
                    ElisarSea.fill(chunk, mp, x, z, floor, plan.ocean);
                    // мини-биом «Туманный данж»: серые каменные столбы из воды + центральный утёс под руину
                    MistKeep.Region mr = MistKeep.regionAt(x + 0.5, z + 0.5);
                    if (mr != null) {
                        int top = MistKeep.rockTop(plan.ocean, x + 0.5, z + 0.5, mr);
                        if (top > floor) {
                            BlockState rock = st(ElisarBlocks.KEEP_STONE), cracked = st(ElisarBlocks.KEEP_STONE_CRACKED), mossRock = st(ElisarBlocks.KEEP_STONE_MOSSY);
                            for (int y = floor + 1; y <= top; ++y) {
                                BlockState s = y == top
                                        ? (hash(x, z, 887) < 0.30 ? mossRock : rock)
                                        : (hash(x, y, z) < 0.10 ? cracked : (hash(x, y, z) < 0.14 ? mossRock : rock));
                                chunk.setBlockState(mp.set(x, y, z), s, false);
                            }
                        }
                    }
                }
                if (!ashen && c.isl == null && c.fallTop == Integer.MIN_VALUE && c.deckY == Integer.MIN_VALUE) debris(plan, chunk, mp, x, z);                if (dx == 15 && dz == 15) mistKeepDressing(plan, chunk, mp, x0, z0);

                tower(chunk, mp, x, z);
                if (c.isl != null) {
                    if (c.isl.kind == 5 || c.isl.kind == 6 || ashen && c.isl.kind != 0) {
                        scorched(chunk, mp, c, x, z);
                    } else for (int y = c.bottom; y <= c.top; ++y) {
                        BlockState s;
                        if (y == c.top) {
                            s = switch (c.surface) {
                                case S_COBBLE -> cobble;
                                case S_PATH -> path;
                                case S_PLANK -> plank;
                                case S_PLOT -> cobble;         // pebble apron around the houses (reference)
                                default -> {
                                    if (c.mtn) {
                                        yield vnoise(x * 0.12, z * 0.12, 68) > 0.38 ? moss : grass;   // зелёные полки
                                    }
                                    if (c.garden) {
                                        if (c.springy) yield ElisarBlocks.LANTERN.get().defaultBlockState();   // hot spring: warm glow in the pool bed
                                        if (c.pondRim || c.pond) yield st(ru.aiforge.riftworld.registry.CastleBlocks.ASHLAR);            // travertine
                                        yield vnoise(x * 0.14, z * 0.14, 67) > 0.60 ? moss : grass;
                                    }
                                    if (c.isl.kind == 7) {
                                        yield hash(x, z, 24) < 0.05 ? moss : grass;      // ровный луг, мох редкими пятнами
                                    }
                                    boolean groveTan = c.isl.kind == SizoGrove.KIND || (c.isl.kind != 4 && SizoGrove.inside(x + 0.5, z + 0.5));
                                    yield groveTan
                                            ? (vnoise(x * 0.15, z * 0.15, 66) > 0.58 ? moss : grass)
                                            : (c.isl.kind != 4 && vnoise(x * 0.11, z * 0.11, 21) > 0.74 || hash(x, z, 23) < 0.012) ? moss : grass;
                                }
                            };
                        } else if (c.mtn) {
                            // слоистые ярусы: у поверхности известняковые полосы с мхом, в глубине — камень
                            int band = Math.floorMod(y + (int) (hash(x, z, 712) * 3), 7);
                            s = y > c.top - 12 ? (band == 0 ? mossy : (band <= 2 ? ashlar : stone))
                                    : (hash(x, y, z) < 0.08 ? mossy : stone);
                            if (y <= c.top - 2 && y >= c.top - 6
                                    && vnoise(x * 0.33, z * 0.33, 313) > 0.80 && hash(x, y, z) < 0.88) s = aurite;
                        } else if (y == c.top - 1) {
                            // под травой — почва, а не белёсая корка: иначе склоны читаются как каменные ступени
                            s = c.surface == S_GRASS || c.surface == S_PATH ? soil : crust;
                        } else if (y <= c.bottom + 1 && hash(x, z, y) < 0.5 || hash(x, y, z) < 0.07) {
                            s = mossy;
                        } else {
                            s = stone;
                            // улучшение биомов: ауритовая руда (монетоносная) змеится вдоль ярусов Висячих садов,
                            // прячась на глубине 2..6 под поверхностью полки - волно-лентовые прожилки с промывками
                            if ((c.garden || c.mtn) && y <= c.top - 2 && y >= c.top - 6
                                    && vnoise(x * 0.33, z * 0.33, 313) > 0.80 && hash(x, y, z) < 0.88) s = aurite;
                        }
                        chunk.setBlockState(mp.set(x, y, z), s, false);
                    }
                    // half-step slabs where a street climbs one block (smooth walking, no hard ledges)
                    if (c.surface == S_COBBLE && c.waterTop == Integer.MIN_VALUE && c.deckY == Integer.MIN_VALUE && c.fallTop == Integer.MIN_VALUE) {
                        for (Direction d : Direction.Plane.HORIZONTAL) {
                            Col n = cols[dx + 1 + d.getStepX()][dz + 1 + d.getStepZ()];
                            if (n.isl != null && n.top == c.top + 1 && n.deckY == Integer.MIN_VALUE
                                    && (n.surface == S_COBBLE || n.surface == S_PATH)) {
                                chunk.setBlockState(mp.set(x, c.top + 1, z), st(ElisarBlocks.COBBLE_SLAB), false);
                                break;
                            }
                        }
                    }
                    if (c.surface == S_GRASS && c.isl.kind != 5 && !ashen && c.waterTop == Integer.MIN_VALUE && c.deckY == Integer.MIN_VALUE) {
                        double h = hash(x, z, 77);
                        if (c.fence) {
                            BlockState fs = st(ElisarBlocks.FENCE);
                            for (Direction d : Direction.Plane.HORIZONTAL) {
                                Col n = cols[dx + 1 + d.getStepX()][dz + 1 + d.getStepZ()];
                                fs = fs.setValue(prop(d), n.fence && n.top == c.top);
                            }
                            chunk.setBlockState(mp.set(x, c.top + 1, z), fs, false);
                        } else {
                            boolean violet = c.isl.kind == 7 || (c.isl.kind != 4 && VioletHills.inside(x + 0.5, z + 0.5));
                            if (violet) {                                  // biome 04: drift FIELDS of lavender rows (the reference carpet)
                                double field = vnoise(x * 0.021, z * 0.021, 92);
                                double edge = vnoise(x * 0.09, z * 0.09, 90);
                                if (field > 0.62) {                        // planted drift: wavy stripes, ~90% cover on rows
                                    double ph = x * 0.866 + z * 0.5 + vnoise(x * 0.03, z * 0.03, 93) * 4.2;
                                    int per = (int) Math.floor(ph) & 7;
                                    boolean planted = per <= 2 || per == 5;
                                    if (planted && h < 0.93) {
                                        int f = (int) (hash(x, z, 91) * 100);
                                        chunk.setBlockState(mp.set(x, c.top + 1, z), st(f < 58 ? ElisarBlocks.VIOLET_BLOOM : f < 86 ? ElisarBlocks.LILAC_BLOOM : ElisarBlocks.PALE_BLOOM), false);
                                    } else if (h < 0.09) chunk.setBlockState(mp.set(x, c.top + 1, z), tall, false);
                                } else if (field > 0.55) {                 // field fringe: mixed drifts
                                    if (h < 0.30 && edge > 0.52) {
                                        int f = (int) (hash(x, z, 91) * 100);
                                        chunk.setBlockState(mp.set(x, c.top + 1, z), st(f < 50 ? ElisarBlocks.VIOLET_BLOOM : f < 80 ? ElisarBlocks.LILAC_BLOOM : ElisarBlocks.PALE_BLOOM), false);
                                    } else if (h < 0.16) chunk.setBlockState(mp.set(x, c.top + 1, z), tall, false);
                                } else {                                   // wild lilac meadows between fields
                                    if (h < 0.14) chunk.setBlockState(mp.set(x, c.top + 1, z), tall, false);
                                    else if (h < 0.17 && edge > 0.58) {
                                        int f = (int) (hash(x, z, 91) * 3);
                                        chunk.setBlockState(mp.set(x, c.top + 1, z), st(f == 0 ? ElisarBlocks.VIOLET_BLOOM : f == 1 ? ElisarBlocks.LILAC_BLOOM : ElisarBlocks.PALE_BLOOM), false);
                                    }
                                }
                            } else if (c.mtn) {
                                // rev 9: полки многоуровневых гор - трава, цветы, низкие кусты
                                if (h < 0.11) chunk.setBlockState(mp.set(x, c.top + 1, z), tall, false);
                                else if (h < 0.30 && vnoise(x * 0.1, z * 0.1, 158) > 0.50) {
                                    int f = (int) (hash(x, z, 159) * 100);
                                    chunk.setBlockState(mp.set(x, c.top + 1, z), st(f < 34 ? ElisarBlocks.FLOWERS : f < 62 ? ElisarBlocks.VIOLET_BLOOM
                                            : f < 84 ? ElisarBlocks.LILAC_BLOOM : ElisarBlocks.PALE_BLOOM), false);
                                } else if (h < 0.36 && vnoise(x * 0.13, z * 0.13, 160) > 0.62
                                        && chunk.getBlockState(mp.set(x, c.top + 1, z)).isAir()) {
                                    chunk.setBlockState(mp.set(x, c.top + 1, z), ElisarBlocks.SIZO_LEAVES.get().defaultBlockState()
                                            .setValue(LeavesBlock.PERSISTENT, true).setValue(LeavesBlock.DISTANCE, 1), false);   // куст на полке
                                }
                            } else if (c.garden) {
                                // biome 07: nearly-jungle - thick grass, colour splashes, creeping shrubs (tiny trees handled at region)
                                if (h < 0.40) chunk.setBlockState(mp.set(x, c.top + 1, z), tall, false);
                                else if (h < 0.50 && vnoise(x * 0.1, z * 0.1, 154) > 0.55) {
                                    int f = (int) (hash(x, z, 155) * 100);
                                    chunk.setBlockState(mp.set(x, c.top + 1, z), st(f < 46 ? ElisarBlocks.FLOWERS : f < 66 ? ElisarBlocks.VIOLET_BLOOM : f < 84 ? ElisarBlocks.LILAC_BLOOM : ElisarBlocks.PALE_BLOOM), false);
                                } else if (h < 0.55 && chunk.getBlockState(mp.set(x, c.top + 1, z)).isAir()) {
                                    chunk.setBlockState(mp.set(x, c.top + 1, z), ElisarBlocks.SIZO_LEAVES.get().defaultBlockState()
                                            .setValue(LeavesBlock.PERSISTENT, true).setValue(LeavesBlock.DISTANCE, 1), false);   // low creeping shrub
                                }
                            } else if (c.isl.kind == SizoGrove.KIND || (c.isl.kind != 4 && SizoGrove.inside(x + 0.5, z + 0.5))) {
                                // biome 05: mint floor with violet bloom drifts among the giants (no paths - TZ)
                                if (h < 0.20) chunk.setBlockState(mp.set(x, c.top + 1, z), tall, false);
                                else if (h < 0.42 && vnoise(x * 0.075, z * 0.075, 95) > 0.60) {
                                    int f = (int) (hash(x, z, 96) * 100);
                                    chunk.setBlockState(mp.set(x, c.top + 1, z), st(f < 40 ? ElisarBlocks.VIOLET_BLOOM : f < 72 ? ElisarBlocks.LILAC_BLOOM : f < 88 ? ElisarBlocks.PALE_BLOOM : ElisarBlocks.FLOWERS), false);
                                }
                            } else if (h < 0.09) chunk.setBlockState(mp.set(x, c.top + 1, z), tall, false);
                            else if (h < 0.105) chunk.setBlockState(mp.set(x, c.top + 1, z), flowers, false);
                        }
                    }
                }
                // biome 07: hanging vine curtains draped from the cliff lip above this column (the reference look)
                if ((c.garden || c.mtn) && c.surface == S_GRASS && c.waterTop == Integer.MIN_VALUE && c.deckY == Integer.MIN_VALUE) {
                    int maxN = Integer.MIN_VALUE;
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        Col n = cols[dx + 1 + d.getStepX()][dz + 1 + d.getStepZ()];
                        if (n.isl != null) maxN = Math.max(maxN, n.top);
                    }
                    if (maxN > c.top + 2.5 && vnoise(x * 0.21, z * 0.21, 151) > 0.40) {
                        int len = 2 + (int) (hash(x, z, 152) * 3.0);
                        for (int y = Math.max(c.top + 1, maxN - len); y < maxN; ++y) {
                            BlockPos vp = mp.set(x, y, z);
                            if (chunk.getBlockState(vp).isAir() || chunk.getBlockState(vp).canBeReplaced())
                                chunk.setBlockState(vp, ElisarBlocks.SIZO_LEAVES.get().defaultBlockState()
                                        .setValue(LeavesBlock.PERSISTENT, true).setValue(LeavesBlock.DISTANCE, 2), false);
                        }
                    }
                }
                if (c.waterTop != Integer.MIN_VALUE) {
                    for (int y = c.waterBottom; y <= c.waterTop; ++y) chunk.setBlockState(mp.set(x, y, z), water, false);
                }
                if (c.fallTop != Integer.MIN_VALUE) {
                    for (int y = c.fallBottom; y <= c.fallTop; ++y) chunk.setBlockState(mp.set(x, y, z), falling, false);
                }
                if (c.deckY != Integer.MIN_VALUE) {
                    if (!c.rail) {
                        chunk.setBlockState(mp.set(x, c.deckY, z), plank, false);
                        BlockState above = Blocks.AIR.defaultBlockState();
                        for (Direction d : Direction.Plane.HORIZONTAL) {     // half step where the sagging deck rises
                            Col n = cols[dx + 1 + d.getStepX()][dz + 1 + d.getStepZ()];
                            if (!n.rail && n.deckY == c.deckY + 1) { above = st(ElisarBlocks.SIZO_SLAB); break; }
                        }
                        chunk.setBlockState(mp.set(x, c.deckY + 1, z), above, false);
                    } else if (c.isl == null || c.top < c.deckY + 1) {
                        chunk.setBlockState(mp.set(x, c.deckY, z), plank, false);
                        if (c.post) {
                            BlockState wall = st(ElisarBlocks.STONE_WALL);
                            chunk.setBlockState(mp.set(x, c.deckY + 1, z), wall, false);
                            chunk.setBlockState(mp.set(x, c.deckY + 2, z), wall, false);
                            chunk.setBlockState(mp.set(x, c.deckY + 3, z), st(ElisarBlocks.LANTERN), false);
                        } else {
                            // chain railing strung between the posts (as on the reference bridges)
                            BlockState ch = st(ElisarBlocks.CHAIN).setValue(net.minecraft.world.level.block.ChainBlock.AXIS, c.railX ? Direction.Axis.X : Direction.Axis.Z);
                            chunk.setBlockState(mp.set(x, c.deckY, z), ch, false);       // lower chain along the deck edge
                            chunk.setBlockState(mp.set(x, c.deckY + 1, z), ch, false);   // hand chain
                        }
                    }
                }
                trees(plan, chunk, mp, x, z);
            }
        }
        Heightmap.primeHeightmaps(chunk, EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG));
        return CompletableFuture.completedFuture(chunk);
    }

    /** Scorched castle island: ash / burnt soil top with ember-dark patches, scorched stone body, a few boulders. */
    private static void scorched(ChunkAccess chunk, BlockPos.MutableBlockPos mp, Col c, int x, int z) {
        BlockState ash = st(ru.aiforge.riftworld.registry.CastleBlocks.ASH), soil = st(ru.aiforge.riftworld.registry.CastleBlocks.BURNT_SOIL);
        BlockState sst = st(ru.aiforge.riftworld.registry.CastleBlocks.SCORCHED_STONE), stone = st(ElisarBlocks.STONE);
        double n = vnoise(x * 0.09, z * 0.09, 51);
        for (int y = c.bottom; y <= c.top; ++y) {
            BlockState s;
            if (y == c.top) s = n > 0.62 ? soil : (n < 0.3 && hash(x, z, 52) < 0.5) ? sst : ash;
            else if (y >= c.top - 2) s = n > 0.5 ? soil : sst;
            else s = hash(x, y, z) < 0.25 ? stone : sst;
            chunk.setBlockState(mp.set(x, y, z), s, false);
        }
        // boulders of scorched stone (outside the castle)
        if (!CastleSite.inFootprint(x, z, 3) && !EmberRuins.inFootprint(x, z, 3) && c.top - c.bottom > 3) {
            int gx = Math.floorDiv(x, 9), gz = Math.floorDiv(z, 9);
            if (hash(gx, gz, 53) < 0.22) {
                double bx = gx * 9 + 2 + hash(gx, gz, 54) * 5, bz = gz * 9 + 2 + hash(gx, gz, 55) * 5, br = 1.2 + hash(gx, gz, 56) * 1.6;
                double d = Math.hypot(x + 0.5 - bx, z + 0.5 - bz);
                if (d < br) {
                    int hgt = (int) Math.round((br - d) * 1.3 + 0.4);
                    for (int y = 1; y <= hgt; ++y) chunk.setBlockState(mp.set(x, c.top + y, z), hash(x, y, z) < 0.3 ? stone : sst, false);
                }
            }
        }
    }

    /** Leafless charred tree: crooked trunk and a few bare branches that fork upwards (no foliage). */
    private static void deadTree(ChunkAccess chunk, BlockPos.MutableBlockPos mp, CityPlan.Tree t, int base, int x, int z) {
        BlockState logY = st(ru.aiforge.riftworld.registry.CastleBlocks.CHARRED_LOG);
        int dx = x - t.x, dz = z - t.z;
        if (dx == 0 && dz == 0) for (int y = base; y < base + t.h; ++y) chunk.setBlockState(mp.set(x, y, z), logY, false);
        int nb = 2 + (t.seed & 3);
        for (int b = 0; b < nb; ++b) {
            double a = (b + hash(t.seed, b, 61) * 0.5) * Math.PI * 2 / nb + hash(t.seed, 7, 60) * Math.PI * 2;   // spread evenly, no clumping
            int len = 2 + (int) (hash(t.seed, b, 62) * 3);
            int y0 = base + t.h - 1 - (int) (hash(t.seed, b, 63) * Math.min(4, t.h - 2));
            for (int k = 1; k <= len; ++k) {
                int bx = (int) Math.round(Math.cos(a) * k), bz = (int) Math.round(Math.sin(a) * k);
                int by = y0 + k / 2;
                if (bx == dx && bz == dz) {
                    Direction.Axis ax = k % 2 == 0 ? Direction.Axis.Y : (Math.abs(Math.cos(a)) > Math.abs(Math.sin(a)) ? Direction.Axis.X : Direction.Axis.Z);
                    chunk.setBlockState(mp.set(x, by, z), logY.setValue(RotatedPillarBlock.AXIS, ax), false);
                    if (k == len) chunk.setBlockState(mp.set(x, by + 1, z), logY, false);
                }
            }
        }
    }

    private static net.minecraft.world.level.block.state.properties.BooleanProperty prop(Direction d) {
        return switch (d) {
            case NORTH -> FenceBlock.NORTH;
            case SOUTH -> FenceBlock.SOUTH;
            case WEST -> FenceBlock.WEST;
            default -> FenceBlock.EAST;
        };
    }

    // ------------------------------------------------------------------ native trees
    private static void trees(CityPlan plan, ChunkAccess chunk, BlockPos.MutableBlockPos mp, int x, int z) {
        java.util.List<CityPlan.Tree> wild = WildIslands.treesNear(x, z);
        java.util.List<CityPlan.Tree> all = wild.isEmpty() ? plan.at(x, z).trees : new java.util.ArrayList<>(plan.at(x, z).trees);
        if (!wild.isEmpty()) all.addAll(wild);
        for (CityPlan.Tree t : all) {
            int dx = x - t.x, dz = z - t.z;
            int reach = t.kind == SizoGrove.TREE_GIANT ? 7 : 4;
            if (Math.abs(dx) > reach || Math.abs(dz) > reach) continue;
            int base = t.baseY;
            if (base == Integer.MIN_VALUE) {
                Col c = column(plan, t.x, t.z);
                base = c.isl == null ? -999 : c.top + 1;
                t.baseY = base;
            }
            if (base < 0) continue;
            if (t.kind == 2) { deadTree(chunk, mp, t, base, x, z); continue; }
            if (t.kind == SizoGrove.TREE_GIANT) { giantSizo(chunk, mp, t, base, x, z); continue; }
            boolean sizo = t.kind == 0;
            BlockState logS = (sizo ? ElisarBlocks.SIZO_LOG : ElisarBlocks.AMBRA_LOG).get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            BlockState leaf = (sizo ? ElisarBlocks.SIZO_LEAVES : ElisarBlocks.AMBRA_LEAVES).get().defaultBlockState()
                    .setValue(LeavesBlock.PERSISTENT, true).setValue(LeavesBlock.DISTANCE, 1);
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dx == 0 && dz == 0) {
                for (int y = base; y < base + t.h; ++y) chunk.setBlockState(mp.set(x, y, z), logS, false);
            }
            if (sizo) {
                for (int j = 3; j <= t.h + 1; ++j) {
                    double r = (1 - (j - 3) / (double) (t.h - 1)) * 3.3 + 0.55;
                    if ((j & 1) == 1) r *= 0.72;
                    if (dist > r || dx == 0 && dz == 0 && j < t.h) continue;
                    if (dist > r - 1 && hash(x * 3 + j, z, t.seed) < 0.3) continue;
                    BlockPos p = mp.set(x, base + j, z);
                    if (chunk.getBlockState(p).isAir() || chunk.getBlockState(p).canBeReplaced()) chunk.setBlockState(p, leaf, false);
                }
            } else {
                double R = 2.7 + (t.seed % 3) * 0.3;
                int cy = base + t.h - 1;
                for (int y = cy - 3; y <= cy + 3; ++y) {
                    double dy = (y - cy) * 1.25;
                    double q = dx * dx + dz * dz + dy * dy;
                    if (q > R * R || dx == 0 && dz == 0 && y < base + t.h) continue;
                    if (q > (R - 1) * (R - 1) && hash(x, z * 5 + y, t.seed) < 0.28) continue;
                    BlockPos p = mp.set(x, y, z);
                    if (chunk.getBlockState(p).isAir() || chunk.getBlockState(p).canBeReplaced()) chunk.setBlockState(p, leaf, false);
                }
                // two short branches into the crown
                if (dist > 0 && dist < 1.5 && Math.abs(dx) + Math.abs(dz) == 1 && hash(dx, dz, t.seed) < 0.5) {
                    chunk.setBlockState(mp.set(x, base + t.h - 2, z), logS.setValue(RotatedPillarBlock.AXIS, dx != 0 ? Direction.Axis.X : Direction.Axis.Z), false);
                }
            }
        }
    }

    /** Biome 05 «Роща Сизо» giant: 2x2 trunk, branch cross, 3 stacked flat disc canopies + small crown (the reference silhouette). */
    private static void giantSizo(ChunkAccess chunk, BlockPos.MutableBlockPos mp, CityPlan.Tree t, int base, int x, int z) {
        int dx = x - t.x, dz = z - t.z;
        BlockState logS = ElisarBlocks.SIZO_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState leaf = ElisarBlocks.SIZO_LEAVES.get().defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, true).setValue(LeavesBlock.DISTANCE, 1);
        boolean inTrunk = dx >= 0 && dx <= 1 && dz >= 0 && dz <= 1;
        int topY = base + t.h + 2;                       // first disc altitude (trunk tops out here)
        if (inTrunk) {
            for (int y = base; y <= topY; ++y) chunk.setBlockState(mp.set(x, y, z), logS, false);
            return;
        }
        // branch cross under the discs (trunk centre sits between the 4 trunk cells)
        if ((dz == 0 || dz == 1) && ((dx >= 2 && dx <= 4) || (dx <= -1 && dx >= -3))
                 && !(dx == 0 || dx == 1)) {
            chunk.setBlockState(mp.set(x, base + t.h + 1, z), logS.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X), false);
        } else if ((dx == 0 || dx == 1) && ((dz >= 2 && dz <= 4) || (dz <= -1 && dz >= -3))) {
            chunk.setBlockState(mp.set(x, base + t.h + 1, z), logS.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z), false);
        }
        // stacked discs: centre (0.5, 0.5); tiers {radius, dy}
        double dc = Math.hypot(dx - 0.5, dz - 0.5);
        double[][] tiers = new double[][] {{1.7, 1}, {2.6, 0}, {3.6, -2}, {2.8, -4}};
        long seed = t.seed;
        for (double[] ti : tiers) {
            double r = ti[0]; int yy = topY + (int) ti[1];
            if (dc > r) continue;
            boolean rim = dc > r - 0.85;
            if (rim && hash(x * 7 + yy, z * 7, (int) (seed & 0x3FFFFFFF)) < 0.18) continue;          // ragged edge
            BlockPos pp = mp.set(x, yy, z);
            if (chunk.getBlockState(pp).isAir() || chunk.getBlockState(pp).canBeReplaced()) chunk.setBlockState(pp, leaf, false);
            if (rim && hash(x * 13 - yy, z * 11, (int) ((seed >>> 20) & 0x3FFFFFFF)) < 0.4) {         // drooping skirt
                BlockPos lo = mp.set(x, yy - 1, z);
                if (chunk.getBlockState(lo).isAir()) chunk.setBlockState(lo, leaf, false);
            }
        }
    }

    // ================================================================== decoration: buildings + props
    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager sm) {
        CityPlan plan = CityPlan.get();
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        BoundingBox box = new BoundingBox(x0, MIN_Y, z0, x0 + 15, MIN_Y + HEIGHT - 1, z0 + 15);
        StructureTemplateManager stm = level.getLevel().getServer().getStructureManager();
        RandomSource rnd = RandomSource.create(chunk.getPos().toLong());
        java.util.Set<CityPlan.Plot> plots = new java.util.LinkedHashSet<>();
        java.util.Set<CityPlan.Prop> props = new java.util.LinkedHashSet<>();
        for (int cx = x0; cx <= x0 + 16; cx += 16)
            for (int cz = z0; cz <= z0 + 16; cz += 16) {
                plots.addAll(plan.at(cx, cz).plots);
                props.addAll(plan.at(cx, cz).props);
            }
        plots.addAll(plan.at(x0 + 15, z0 + 15).plots);
        for (CityPlan.Plot p : plots) {
            if (p.x0() > x0 + 15 || p.x0() + p.w() - 1 < x0 || p.z0() > z0 + 15 || p.z0() + p.d() - 1 < z0) continue;
            ResourceLocation id = new ResourceLocation("riftworld", "city/" + p.template());
            Optional<StructureTemplate> tpl = stm.get(id);
            if (tpl.isEmpty()) continue;
            Rotation rot = switch (p.facing()) {
                case "west" -> Rotation.CLOCKWISE_90;
                case "north" -> Rotation.CLOCKWISE_180;
                case "east" -> Rotation.COUNTERCLOCKWISE_90;
                default -> Rotation.NONE;
            };
            StructurePlaceSettings s = new StructurePlaceSettings().setRotation(rot).setBoundingBox(box).setIgnoreEntities(true);
            BoundingBox bb = tpl.get().getBoundingBox(s, BlockPos.ZERO);
            BlockPos pos = new BlockPos(p.x0() - bb.minX(), p.h() + p.yOff(), p.z0() - bb.minZ());
            tpl.get().placeInWorld(level, pos, pos, s, rnd, 2);
        }
        if (x0 + 15 >= CastleSite.X0 && x0 <= CastleSite.X0 + CastleSite.TW - 1 && z0 + 15 >= CastleSite.Z0 && z0 <= CastleSite.Z0 + CastleSite.TD - 1) {
            stm.get(new ResourceLocation("riftworld", "castle/castle")).ifPresent(tpl -> {
                StructurePlaceSettings s = new StructurePlaceSettings().setBoundingBox(box).setIgnoreEntities(true).setKnownShape(true);
                BlockPos pos = new BlockPos(CastleSite.X0, CastleSite.Y, CastleSite.Z0);
                tpl.placeInWorld(level, pos, pos, s, rnd, 2);
            });
        }
        // Inverted Tower interior dressing (reliquary, deck hall, landings)
        if (x0 + 15 >= TOWER_X - 12 && x0 <= TOWER_X + 12 && z0 + 15 >= TOWER_Z - 12 && z0 <= TOWER_Z + 12) {
            for (String[] dec : TOWER_DECOR) {
                ModelKind k = kind(dec[0]);
                if (k == null) continue;
                BlockPos at = new BlockPos(TOWER_X + Integer.parseInt(dec[1]), TOWER_Y0 + Integer.parseInt(dec[2]), TOWER_Z + Integer.parseInt(dec[3]));
                if (!box.isInside(at)) continue;
                Direction f = switch (dec[4]) {
                    case "north" -> Direction.NORTH; case "east" -> Direction.EAST; case "west" -> Direction.WEST; default -> Direction.SOUTH;
                };
                placeModel(level, box, k, at, f, Boolean.parseBoolean(dec[5]));
            }
        }
        // biome 04: the two Violet Hills churches (one intact, one ruined), max 2 per biome region
        VioletHills.Region vreg = VioletHills.regionAt(x0 + 8, z0 + 8);
        if (vreg != null && (vreg.churchA.outside(x0 + 8, z0 + 8) < 48 || vreg.churchB.outside(x0 + 8, z0 + 8) < 48)) {
            for (VioletHills.Church ch : new VioletHills.Church[] { vreg.churchA, vreg.churchB }) {
                if (x0 + 15 < ch.x0 || x0 > ch.x0 + ch.wx - 1 || z0 + 15 < ch.z0 || z0 > ch.z0 + ch.wz - 1) continue;
                Optional<StructureTemplate> t = stm.get(new ResourceLocation("riftworld", ch.ruined ? "ruins/church_violet_ruins" : "ruins/church_violet"));
                if (t.isEmpty()) continue;
                Rotation rot = switch (ch.facing) {
                    case "west" -> Rotation.CLOCKWISE_90;
                    case "north" -> Rotation.CLOCKWISE_180;
                    case "east" -> Rotation.COUNTERCLOCKWISE_90;
                    default -> Rotation.NONE;
                };
                StructurePlaceSettings sp = new StructurePlaceSettings().setRotation(rot).setBoundingBox(box).setIgnoreEntities(true);
                BoundingBox bb = t.get().getBoundingBox(sp, BlockPos.ZERO);
                BlockPos pos = new BlockPos(ch.x0 - bb.minX(), ch.y, ch.z0 - bb.minZ());
                t.get().placeInWorld(level, pos, pos, sp, rnd, 2);
            }
        }
        // biome 05: grove dressing - mossy boulders & amber-band way pillars (also on islets inside the grove sea-ring)
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        SizoGrove.Region greg = SizoGrove.regionAt(x0 + 8, z0 + 8);
        if (greg != null) {
            for (SizoGrove.Boulder b : greg.boulders) {
                if (b.x + b.r < x0 - 1 || b.x - b.r > x0 + 16 || b.z + b.r < z0 - 1 || b.z - b.r > z0 + 16) continue;
                Col bc = column(plan, b.x, b.z);
                if (bc.isl == null || bc.top <= plan.ocean + 1) continue;
                double rx = b.r, ry = b.r * 0.72;
                int cy = bc.top + (int) Math.round(ry * 0.28);
                int cx0 = Math.max(x0, (int) Math.floor(b.x - rx)), cx1 = Math.min(x0 + 15, (int) Math.ceil(b.x + rx));
                int cz0 = Math.max(z0, (int) Math.floor(b.z - rx)), cz1 = Math.min(z0 + 15, (int) Math.ceil(b.z + rx));
                for (int bx = cx0; bx <= cx1; ++bx)
                    for (int bz = cz0; bz <= cz1; ++bz) {
                        double dx2 = bx - b.x, dz2 = bz - b.z;
                        double hq = (dx2 * dx2 + dz2 * dz2) / (rx * rx);
                        if (hq >= 1) continue;
                        int maxDy = (int) Math.floor(ry * Math.sqrt(1 - hq) + 0.4);
                        for (int dy = -maxDy; dy <= maxDy; ++dy) {
                            int yy = cy + dy;
                            if (yy <= plan.ocean) continue;
                            // moss cap wherever sky faces the boulder, yellowed stone core beneath (the reference look)
                            boolean cap = dy >= maxDy - 1 && hash(bx, bz, (int) (b.seed & 0x3FFFFFFF)) < (dy == maxDy ? 0.85 : 0.3);
                            chunk.setBlockState(mp.set(bx, yy, bz), st(cap ? ElisarBlocks.MOSS : ElisarBlocks.STONE), false);
                        }
                    }
            }
            for (int[] pp : SizoGrove.pillarsIn(greg, x0, z0)) {
                if (pp[0] < x0 || pp[0] > x0 + 15 || pp[1] < z0 || pp[1] > z0 + 15) continue;
                Col pc = column(plan, pp[0], pp[1]);
                if (pc.isl == null || pc.top <= plan.ocean + 1) continue;
                BlockState pl = ru.aiforge.riftworld.registry.TempleBlocks.PILLAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
                for (int dy = 1; dy <= 3; ++dy) chunk.setBlockState(mp.set(pp[0], pc.top + dy, pp[1]), pl, false);
                chunk.setBlockState(mp.set(pp[0], pc.top + 4, pp[1]), st(ru.aiforge.riftworld.registry.TempleBlocks.GOLD), false);
                chunk.setBlockState(mp.set(pp[0], pc.top + 5, pp[1]), pl, false);
                chunk.setBlockState(mp.set(pp[0], pc.top + 6, pp[1]), st(ru.aiforge.riftworld.registry.TempleBlocks.SLAB), false);
            }
        }
        // biome 05: the rare RUINED pale temple, alone on its terrace
        SizoGrove.Region treg = SizoGrove.templeIn(x0, z0);
        if (treg != null) {
            Optional<StructureTemplate> t = stm.get(new ResourceLocation("riftworld", "ruins/temple_sizo_ruins"));
            if (!t.isEmpty()) {
                Rotation rot = switch (treg.trot) {
                    case 1 -> Rotation.CLOCKWISE_90;
                    case 2 -> Rotation.CLOCKWISE_180;
                    case 3 -> Rotation.COUNTERCLOCKWISE_90;
                    default -> Rotation.NONE;
                };
                StructurePlaceSettings sp = new StructurePlaceSettings().setRotation(rot).setBoundingBox(box).setIgnoreEntities(true);
                BoundingBox bb = t.get().getBoundingBox(sp, BlockPos.ZERO);
                int cwx = (int) Math.floor(treg.tx - (bb.minX() + bb.maxX()) / 2.0);
                int cwz = (int) Math.floor(treg.tz - (bb.minZ() + bb.maxZ()) / 2.0);
                BlockPos pos = new BlockPos(cwx, treg.ty, cwz);      // floor layer replaces the flattened terrace top
                t.get().placeInWorld(level, pos, pos, sp, rnd, 2);
            }
        }
        EmberRuins.Site es = EmberRuins.siteAt(x0 + 8, z0 + 8);
        if (es != null && x0 + 15 >= es.x0 && x0 <= es.x0 + EmberRuins.TW - 1 && z0 + 15 >= es.z0 && z0 <= es.z0 + EmberRuins.TD - 1) {
            stm.get(new ResourceLocation("riftworld", "ruins/chapel")).ifPresent(tpl -> {
                StructurePlaceSettings s = new StructurePlaceSettings().setBoundingBox(box).setIgnoreEntities(true).setKnownShape(true);
                BlockPos pos = new BlockPos(es.x0, es.y, es.z0);
                tpl.placeInWorld(level, pos, pos, s, rnd, 2);
            });
            for (int g = 0; g < es.guards && g < EmberRuins.PERCHES.length; ++g) {
                int[] pc = EmberRuins.PERCHES[g];
                int gx = es.x0 + pc[0], gy = es.y + pc[1], gz = es.z0 + pc[2];
                if (gx < x0 || gx > x0 + 15 || gz < z0 || gz > z0 + 15) continue;
                ru.aiforge.riftworld.entity.EmberGuard guard = ru.aiforge.riftworld.registry.ModEntities.GUARD.get().create(level.getLevel());
                if (guard == null) continue;
                guard.moveTo(gx + 0.5, gy, gz + 0.5, 180.0F, 0.0F);
                guard.perch();
                guard.setPersistenceRequired();
                level.addFreshEntity(guard);
            }
        }
        if (AshenLands.weight(x0 + 8, z0 + 8) > 0) {
            for (AshenLands.Placed pl : AshenLands.decor()) {
                BlockPos bp = pl.pos();
                if (bp.getX() >= x0 && bp.getX() <= x0 + 15 && bp.getZ() >= z0 && bp.getZ() <= z0 + 15) level.setBlock(bp, pl.state(), 2);
            }
        }
        for (CityPlan.Prop p : props) {
            Direction f = Direction.byName(p.facing());
            if (f == null || f.getAxis() == Direction.Axis.Y) f = Direction.SOUTH;
            Col c = column(plan, p.x(), p.z());
            if (c.isl == null) continue;
            int y = c.top + 1;
            if (p.kind().equals("signpost")) {
                BlockState log = ElisarBlocks.SIZO_LOG.get().defaultBlockState();
                for (int k = 0; k < 4; ++k) setIn(level, box, mp.set(p.x(), y + k, p.z()), log);
                ModelKind sk = kind(p.sign());
                if (sk != null) placeModel(level, box, sk, new BlockPos(p.x() + f.getStepX(), y + 2, p.z() + f.getStepZ()), f, false);
                continue;
            }
            if (p.kind().equals("lantern_post")) {                  // pale stone pillar with a lantern on top (reference)
                BlockState wall = st(ElisarBlocks.STONE_WALL);
                setIn(level, box, mp.set(p.x(), y, p.z()), wall);
                setIn(level, box, mp.set(p.x(), y + 1, p.z()), wall);
                setIn(level, box, mp.set(p.x(), y + 2, p.z()), st(ElisarBlocks.LANTERN));
                continue;
            }
            if (p.kind().equals("wheat_field") && p.sign() != null) {  // pale wheat on dark soil, moss border
                String[] wd = p.sign().split("x");
                int w = Integer.parseInt(wd[0]), dd = Integer.parseInt(wd[1]);
                for (int i = -1; i <= w; ++i)
                    for (int j = -1; j <= dd; ++j) {
                        int fx = p.x() + i, fz = p.z() + j;
                        if (!box.isInside(fx, c.top, fz)) continue;
                        Col fc = column(plan, fx, fz);
                        if (fc.isl == null || fc.surface != S_GRASS || fc.waterTop != Integer.MIN_VALUE || fc.deckY != Integer.MIN_VALUE) continue;
                        boolean edge = i < 0 || j < 0 || i >= w || j >= dd;
                        setIn(level, box, mp.set(fx, fc.top, fz), edge ? st(ElisarBlocks.MOSS) : st(ElisarBlocks.SOIL));
                        setIn(level, box, mp.set(fx, fc.top + 1, fz), edge ? Blocks.AIR.defaultBlockState() : st(ElisarBlocks.PALE_WHEAT));
                    }
                continue;
            }
            ModelKind k = kind(p.kind());
            if (k != null) placeModel(level, box, k, new BlockPos(p.x(), y, p.z()), f, p.lit());
        }
    }

    private static ModelKind kind(String name) {
        try {
            return ModelKind.valueOf(name.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void setIn(WorldGenLevel level, BoundingBox box, BlockPos p, BlockState s) {
        if (box.isInside(p)) level.setBlock(p, s, 2);
    }

    private static void placeModel(WorldGenLevel level, BoundingBox box, ModelKind k, BlockPos master, Direction f, boolean lit) {
        ModelBlock mb = ModBlocks.MODELS.get(k).get();
        BlockState ms = mb.defaultBlockState().setValue(ModelBlock.FACING, f).setValue(ModelBlock.POWERED, lit);
        setIn(level, box, master, ms);
        BlockState part = ModBlocks.PART.get().defaultBlockState();
        if (k.passable()) part = part.setValue(ru.aiforge.riftworld.block.PartBlock.OPEN, true);
        for (BlockPos off : k.partOffsets(f)) setIn(level, box, master.offset(off), part);
    }


    /** Мини-биом «Туманный данж» (обходом за чанк): почти сломанная ротонда на центральном утёсе + обрушенная винтовая лестница. */
    private static void mistKeepDressing(CityPlan plan, ChunkAccess chunk, BlockPos.MutableBlockPos mp, int x0, int z0) {
        MistKeep.Region rg = MistKeep.regionAt(x0 + 8, z0 + 8);
        if (rg == null) return;
        double cx = rg.cx + 0.5, cz = rg.cz + 0.5;
        // bbox руины (r~13 + лестница ~4) должен пересекать чанк; пишем только собственные клетки чанка
        if (cx + 16 < x0 || cx - 16 > x0 + 15 || cz + 16 < z0 || cz - 16 > z0 + 15) return;
        BlockState rock = st(ElisarBlocks.KEEP_STONE), cracked = st(ElisarBlocks.KEEP_STONE_CRACKED), mossyK = st(ElisarBlocks.KEEP_STONE_MOSSY);
        BlockState slabB = st(ElisarBlocks.KEEP_SLAB), cage = st(ElisarBlocks.CAGE_LANTERN), chain = st(ElisarBlocks.CHAIN);
        int floorY = plan.ocean + 4;                                    // пол над массивом утёса (ocean+4)
        java.util.function.BiPredicate<Integer, Integer> own = (bx, bz) -> bx >= x0 && bx <= x0 + 15 && bz >= z0 && bz <= z0 + 15;
        // --- пол: диск r<=8 с кольцевым узором (центр остаётся под постамент)
        for (int ox = -9; ox <= 9; ++ox) for (int oz = -9; oz <= 9; ++oz) {
            double dd = Math.hypot(ox, oz);
            if (dd > 8.4 || dd < 1) continue;
            int bx = (int) Math.floor(cx + ox), bz = (int) Math.floor(cz + oz);
            if (!own.test(bx, bz)) continue;
            BlockState f = dd > 6.8 ? cracked : (hash(bx, bz, 891) < 0.18 ? mossyK : (hash(bx, bz, 892) < 0.10 ? cracked : rock));
            chunk.setBlockState(mp.set(bx, floorY - 1, bz), f, false);
        }
        // --- 12 пролётов ротонды: столпы до острий, обломки рёбер арок, зазубренные макушки
        for (int i = 0; i < 12; ++i) {
            double a = i * Math.PI / 6 + (rg.seed & 63) * 0.004;
            int px = (int) Math.floor(cx + Math.cos(a) * 7.0), pz = (int) Math.floor(cz + Math.sin(a) * 7.0);
            int ph = 3 + (int) (hash(px, pz, 893) * 4);                  // высота уцелевшего столпа
            if (hash(px, pz, 894) < 0.16) ph = Math.min(1, ph);          // некоторые почти снесены
            if (own.test(px, pz))
                for (int y = 0; y < ph; ++y)
                    chunk.setBlockState(mp.set(px, floorY + y, pz), y == ph - 1 ? cracked : rock, false);
            // арка из столпа i в i+1: остриё по двум дугам, ~половина пролётов упала
            double survive = hash(px, pz, 895);
            double a2 = (i + 1) * Math.PI / 6 + (rg.seed & 63) * 0.004;
            for (int k = 1; k < 12; ++k) {
                double t = k / 12.0;
                double spike = t < 0.5 ? Math.sqrt(t * 2) : Math.sqrt((1 - t) * 2);
                if (survive < 0.45 || hash((int) (t * 100), px ^ pz, 896) < 0.28) continue;    // пролёт или его кусок отвалился
                int ax = (int) Math.floor(cx + (Math.cos(a) * (1 - t) + Math.cos(a2) * t) * 7.0);
                int az = (int) Math.floor(cz + (Math.sin(a) * (1 - t) + Math.sin(a2) * t) * 7.0);
                int ay = floorY + 3 + (int) Math.round(spike * 3.4);
                if (own.test(ax, az)) chunk.setBlockState(mp.set(ax, ay, az), slabB, false);
                // свисающая цепь с уцелевшего острия
                if (t > 0.44 && t < 0.56 && survive >= 0.45 && hash(px, pz, 897) < 0.5 && own.test(ax, az))
                    for (int chl = 0; chl < 4 && hash(ax, az - chl, 898) > 0.2; ++chl)
                        chunk.setBlockState(mp.set(ax, ay - 1 - chl, az), chain, false);
            }
        }
        // --- центр: постамент + клетка-фонарь (сердце данжа)
        int hx = (int) Math.floor(cx), hz = (int) Math.floor(cz);
        if (own.test(hx, hz)) {
            chunk.setBlockState(mp.set(hx, floorY, hz), rock, false);
            chunk.setBlockState(mp.set(hx, floorY + 1, hz), cage, false);
        }
        // --- обрушенная винтовая лестница: с края пола спиралью вниз в воду
        double dirA = ((rg.seed >> 8) & 255) * (Math.PI * 2 / 256.0);
        for (int k = 0; k < 16; ++k) {
            double sa = dirA + k * 0.42;
            int sx = (int) Math.floor(cx + Math.cos(sa) * (9.6 + k * 0.18));
            int sz = (int) Math.floor(cz + Math.sin(sa) * (9.6 + k * 0.18));
            int sy = floorY - 1 - k;                                     // уходит под воду
            if (!own.test(sx, sz)) continue;
            double br = hash(sx, sz - k, 899);
            if (br < 0.30) continue;                                     // выпавшая ступень
            BlockState step;
            if (br > 0.62) {
                int card = Math.abs(Math.cos(sa)) > Math.abs(Math.sin(sa)) ? (Math.cos(sa) < 0 ? 1 : 3) : (Math.sin(sa) < 0 ? 2 : 0);
                step = st(ElisarBlocks.KEEP_STAIRS)
                        .setValue(net.minecraft.world.level.block.StairBlock.FACING, switch (card) {
                            case 0 -> net.minecraft.core.Direction.NORTH;
                            case 1 -> net.minecraft.core.Direction.WEST;
                            case 2 -> net.minecraft.core.Direction.SOUTH;
                            default -> net.minecraft.core.Direction.EAST;
                        }).setValue(net.minecraft.world.level.block.StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.BOTTOM);
            } else step = br > 0.45 ? cracked : rock;
            chunk.setBlockState(mp.set(sx, sy, sz), step, false);
        }
        // --- парочка обломков-колодцев внутри (был хребет крыши)
        for (int s1 = 0; s1 < 3; ++s1) {
            double ra = ((rg.seed >> (12 + s1 * 4)) & 15) * (Math.PI * 2 / 16.0);
            int rx = (int) Math.floor(cx + Math.cos(ra) * 3), rz = (int) Math.floor(cz + Math.sin(ra) * 3);
            int rh = 1 + (int) (hash(rx, rz, 900) * 3);
            if (own.test(rx, rz))
                for (int y = 0; y < rh; ++y)
                    chunk.setBlockState(mp.set(rx, floorY + y, rz), y == rh - 1 ? cracked : rock, false);
        }
    }

    // ================================================================== misc ChunkGenerator plumbing
    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState rs, BiomeManager bm, StructureManager sm, ChunkAccess chunk, GenerationStep.Carving step) {
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager sm, RandomState rs, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public void createStructures(RegistryAccess ra, ChunkGeneratorStructureState state, StructureManager sm, ChunkAccess chunk, StructureTemplateManager stm) {
    }

    @Override
    public int getGenDepth() {
        return HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return CityPlan.get().ocean;
    }

    @Override
    public int getMinY() {
        return MIN_Y;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState rs) {
        Col c = column(CityPlan.get(), x, z);
        int mt = AshenLands.weight(x, z) > 0 ? AshenLands.moundTop(x, z) : Integer.MIN_VALUE;
        if (c.isl == null && mt != Integer.MIN_VALUE) return mt + 1;
        return c.isl == null ? CityPlan.get().ocean + 1 : c.top + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState rs) {
        CityPlan plan = CityPlan.get();
        Col c = column(plan, x, z);
        BlockState[] arr = new BlockState[HEIGHT];
        java.util.Arrays.fill(arr, Blocks.AIR.defaultBlockState());
        int of = oceanFloor(plan, x, z);
        arr[of] = ElisarBlocks.SEABED.get().defaultBlockState();
        arr[of - 1] = ElisarBlocks.SEABED.get().defaultBlockState();
        // Рев 8: дно — сплошной массив породы (а не 5 блоков), для расстановки структур/высот
        for (int y = 0; y <= of - 2; ++y) arr[y] = ElisarBlocks.STONE.get().defaultBlockState();
        for (int y = of + 1; y <= plan.ocean; ++y) arr[y] = ElisarBlocks.WATER.get().defaultBlockState();
        if (c.isl != null) {
            for (int y = Math.max(0, c.bottom); y <= Math.min(HEIGHT - 1, c.top); ++y) arr[y] = ElisarBlocks.STONE.get().defaultBlockState();
        }
        return new NoiseColumn(MIN_Y, arr);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState rs, BlockPos pos) {
        CityPlan.Island i = CityPlan.get().islandAt(pos.getX() + 0.5, pos.getZ() + 0.5);
        info.add("Elisar: " + (i == null ? "open sky" : "island t=" + String.format("%.2f", i.t(pos.getX() + 0.5, pos.getZ() + 0.5))));
    }
}
