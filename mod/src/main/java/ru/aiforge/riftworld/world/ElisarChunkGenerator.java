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
import net.minecraft.world.level.block.VineBlock;
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

    static final int MIN_Y = -64, HEIGHT = 384;

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
        // Рев 10: дюны, желоба и абиссальные впадины считает сам ElisarSea (мир стал глубже: min_y = -64)
        return ElisarSea.floor(x, z, plan.ocean);
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
                // Рев 14.5: остров-плато давал почти ровную высоту (разброс ~5 блоков), квантовать
                // было нечего — полок-обрывов не получалось вовсе (проверено расчётом: STEP 5 давал
                // 299 стен, STEP 8 — пять). Теперь у садов свой горный профиль: купол от кромки у воды
                // к центру, и уже он квантуется в полки.
                top = terraceTop(gardenProfile(isl, px, pz, plan.ocean), plan.ocean);
            }
            c.top = (int) Math.floor(top);
            // Рев 14: ВОДОПАДЫ. На референсе с каждой полки через край льётся широкая струя и уходит
            // вниз до самой воды. Раньше струя появлялась только там, где рядом оказывалась
            // выпуклость рельефа (редкие «feed»-точки) — на кадрах биом стоял сухой. Теперь водопад
            // выбирается заранее (см. WaterfallGardens.Fall) и льётся там, где его край выше соседа.
            WaterfallGardens.Region grf0 = WaterfallGardens.regionAt(px, pz);
            if (grf0 != null) {
                WaterfallGardens.Fall fall = WaterfallGardens.fallNear(grf0, px, pz);
                if (fall != null) {
                    int low = Integer.MAX_VALUE;
                    for (int ddx = -2; ddx <= 2; ++ddx)
                        for (int ddz = -2; ddz <= 2; ++ddz) {
                            if (grf0.hub.t(px + ddx, pz + ddz) <= 0.05) continue;
                            low = Math.min(low, (int) Math.floor(terraceTop(
                                    gardenProfile(grf0.hub, px + ddx, pz + ddz, plan.ocean), plan.ocean)));
                        }
                    if (low < c.top - 1) {                    // край полки: рядом полка заметно ниже
                        c.fallTop = c.top + 1;                // струя начинается на уровне полки
                        c.fallBottom = plan.ocean + 1;
                        c.waterTop = Integer.MIN_VALUE;
                        c.pond = false;
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
        // Рев 14: на референсе Висячих садов НЕТ ни мостов, ни настилов, ни лестниц — убрано
        // полностью (дощатые мосты с цепями жили здесь с рев 4).
        // water channels (river, pond, springs)
        for (CityPlan.Channel ch : bk.channels)
            applyChannel(c, plan, px, pz, ch.ax(), ch.az(), ch.bx(), ch.bz(), ch.hw(), ch.level(), ch.depth(), tt);
        // biome 04: the violet meadow river - a chord across the island that spills off both rims
        VioletHills.Region vr = VioletHills.regionAt(px, pz);
        if (vr != null)
            applyChannel(c, plan, px, pz, vr.rax, vr.raz, vr.rbx, vr.rbz, VioletHills.Region.RIVER_HW, vr.riverLevel, 2, tt);
        // Рев 14.8: старый «feed»-механизм водопадов УДАЛЁН. Он искал более высокую полку в
        // радиусе 1 и ставил струю почти на каждой колонке (шумовые пороги 0.50/0.58/0.72 в
        // объединении давали ~65 %); пока рельеф был плоским, это давало одну клетку воды, но с
        // настоящими полками биом залило водяной лестницей (29503 блоков воды в кубе ±36).
        // Водопады теперь дают: предвыбранные WaterfallGardens.Fall (ниже) и шторы у подножия
        // стен (см. блок про плющ/шторы в fillFromNoise).
        return c;
    }

    /** Biome 07 terrace transform: horizontal limestone shelves with ~1-block steep rises, lowest shelf hugging the sea. */
    private static double terraceTop(double top, int ocean) {
        double h = top - (ocean + 1);
        if (h <= 0) return top;
        double L = Math.floor(h / WaterfallGardens.STEP), band = h - L * WaterfallGardens.STEP;
        // Рев 14.2: полки с НАСТОЯЩИМИ обрывами. Раньше подъём на STEP размазывался по 1.2 блока
        // высоты, а при пологом рельефе это давало пандус шириной в десятки блоков — вертикальных
        // стен не было вовсе (на кадрах ровный зелёный холм, плющ физически некуда вешать).
        // Теперь переход жёсткий: ровное плато + отвесная известняковая стена как на референсе.
        double rise = band < 0.55 ? 0.0 : 1.0;
        return ocean + 1 + (L + rise) * WaterfallGardens.STEP;
    }

    /**
     * Рев 14.18: точка обзора для кадра «как на референсе» — стоим на полке вплотную к обрыву,
     * смотрим на его стену с плющом и водопадом. Возвращает {camX, camY, camZ, lookX, lookY, lookZ}.
     *
     * <p>Все высоты считаются тем же кодом, что и генерация ({@link #column}), поэтому камера
     * гарантированно оказывается на полке, а не внутри камня или листвы.
     */
    public static int[] gardenWallView(int cx, int cz, int radius) {
        CityPlan plan = CityPlan.get();
        for (int rr = 6; rr <= radius; rr += 2) {
            for (int a = 0; a < 360; a += 7) {
                double th = Math.toRadians(a);
                int x = (int) Math.round(cx + Math.cos(th) * rr);
                int z = (int) Math.round(cz + Math.sin(th) * rr);
                Col c = column(plan, x, z);
                if (c.isl == null || !c.garden || c.surface != S_GRASS
                        || c.waterTop != Integer.MIN_VALUE || c.deckY != Integer.MIN_VALUE) continue;
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    Col n = column(plan, x + d.getStepX(), z + d.getStepZ());
                    int rise = n.top - c.top;
                    if (rise < 5) continue;                        // перед нами стена нужной высоты
                    // Рев 14.22: для кадра ищем стену, по которой реально льётся струя
                    // (тот же шум, что и в генерации штор), иначе кадр «сухой».
                    boolean falling = vnoise(x * 0.085, z * 0.085, 171) > 0.70;
                    if (!falling && rr < radius / 2) continue;
                    // отходим назад по ТОЙ ЖЕ полке: высота площадки должна совпасть с подошвой
                    int vx = x - d.getStepX() * 11, vz = z - d.getStepZ() * 11;
                    Col vc = column(plan, vx, vz);
                    if (vc.isl == null || Math.abs(vc.top - c.top) > 1) continue;
                    if (vc.waterTop != Integer.MIN_VALUE) continue; // не встаём в пруд
                    return new int[]{vx, c.top + 2, vz, x, c.top + Math.max(2, rise / 2), z};
                }
            }
        }
        return null;
    }

    /**
     * Рев 14.5: рельеф Висячих садов. Острова-плато в Elisar почти плоские (у hub разброс высоты
     * ~5 блоков), поэтому ступени не образовывались: терраса квантует высоту, а квантовать было
     * нечего. Здесь сады получают настоящую гору — купол от кромки у самой воды к центру плюс
     * пологий шум, чтобы полки шли неровными языками, как на референсе.
     */
    public static double gardenProfile(CityPlan.Island isl, double px, double pz, int ocean) {
        double t = Math.max(0.0, Math.min(1.0, isl.t(px, pz)));
        double rim = ocean + 6;                                   // у кромки полка почти уходит в воду
        // Рев 14.11: гора выше и круче: на фото от воды до верхней полки 4–5 ярусов по 8–12 блоков.
        double dome = rim + (isl.base + 8 - rim) * Math.pow(t, 0.85);
        // Рев 14.12: только КРУПНЫЙ шум. Мелкий (шаг 0.09) рвал кромки полок на зубцы — на фото
        // полки это широкие ровные платформы с почти прямыми краями.
        dome += 4.6 * (vnoise(px * 0.028, pz * 0.028, 61) - 0.5);  // языки полок
        dome += 1.1 * (vnoise(px * 0.055, pz * 0.055, 62) - 0.5);  // лёгкая волна кромки
        return dome;
    }

    /** Terraced top estimate of a garden column (bridge anchor heights in WaterfallGardens). */
    public static int gardenTerraceTop(CityPlan.Island isl, double px, double pz, int ocean) {
        return (int) Math.floor(terraceTop(gardenProfile(isl, px, pz, ocean), ocean));
    }

    /** Biome 07 pond-mask test stateless over garden region: shelf cells the pond noise claims. */
    private static boolean pondAt(CityPlan plan, WaterfallGardens.Region rg, double px, double pz, int maxTop) {
        // Рев 14.6: на референсе вода стоит почти на каждой полке, но НЕ заливает её целиком.
        // Порог 0.55 после появления настоящих полок залил гору водяной лестницей (кадр 42 прогона
        // 40696891803: 23849 блоков воды в кубе ±36). 0.72 — пруды на полках, как на фото.
        if (vnoise(px * 0.09, pz * 0.09, 131) <= 0.72) return false;
        if (Math.hypot(px - rg.cx, pz - rg.cz) > rg.hub.R * 0.97) return false;
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
        // Рев 14.23: вода Висячих садов — ВАНИЛЬНАЯ. Её цвет берётся из biome water_color,
        // и на референсе вода ярко-бирюзовая; у нашего флюида своя тёмно-синяя текстура, которую
        // тинтом не осветлить. Море Элисара остаётся на своём флюиде.
        BlockState gWater = Blocks.WATER.defaultBlockState();
        BlockState gFalling = gWater.setValue(LiquidBlock.LEVEL, 8);
        BlockState seabed = st(ElisarBlocks.SEABED), stone = st(ElisarBlocks.STONE), mossy = st(ElisarBlocks.STONE_MOSSY), soil = st(ElisarBlocks.SOIL);
        BlockState grass = st(ElisarBlocks.GRASS), cobble = st(ElisarBlocks.COBBLE), path = st(ElisarBlocks.PATH), plank = st(ElisarBlocks.SIZO_PLANKS);
        BlockState tall = st(ElisarBlocks.TALL_GRASS), flowers = st(ElisarBlocks.FLOWERS), log = st(ElisarBlocks.SIZO_LOG);
        BlockState moss = st(ElisarBlocks.MOSS), crust = st(ElisarBlocks.CRUST);
        BlockState aurite = st(ElisarBlocks.AURITE_ORE);
        // Рев 14: у биома 07 своя зелёная палитра (референс — яркая трава и дубы, а не кремовый Элисар)
        BlockState gardenGrass = st(ElisarBlocks.GARDEN_GRASS);
        BlockState gardenLeaf = st(ElisarBlocks.GARDEN_LEAVES).setValue(LeavesBlock.PERSISTENT, true).setValue(LeavesBlock.DISTANCE, 1);
        BlockState ivy = ElisarBlocks.IVY.get().defaultBlockState();
        BlockState lime = st(ElisarBlocks.GARDEN_LIMESTONE), limeMossy = st(ElisarBlocks.GARDEN_LIMESTONE_MOSSY);
        Col[][] cols = new Col[18][18];
        // Рев 10: кромки дна по колонкам — декор моря ставится вторым проходом, после воды
        int[][] seaFloor = new int[16][16];
        for (int[] row : seaFloor) java.util.Arrays.fill(row, Integer.MIN_VALUE);
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
                    ElisarSea.column(chunk, mp, x, z, floor, plan.ocean);
                    seaFloor[dx][dz] = floor;
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
                // Рев 11: СПЛОШНОЙ ПОЛ МИРА. Раньше в адских угодьях под пепельной равниной и на стыках
                // с морем оставалась пустота — можно было копнуть вниз и улететь в бездну. Теперь под любой
                // колонкой лежит камень/бедрок до самого низа измерения.
                bedrockFloor(plan, chunk, mp, x, z, ashen, c);
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
                                    if (c.garden) {
                                        // Рев 14: фонарь в источнике был рукотворным предметом — на референсе
                                        // только природная вода и камень. Убран.
                                        if (c.pondRim || c.pond) yield st(ru.aiforge.riftworld.registry.CastleBlocks.ASHLAR);            // travertine
                                        // Рев 14.12: кромка полки — светлый известняк (как на фото), трава
                                        // начинается отступя от обрыва.
                                        boolean rimEdge = false;
                                        for (Direction dd : Direction.Plane.HORIZONTAL) {
                                            Col nb = cols[dx + 1 + dd.getStepX()][dz + 1 + dd.getStepZ()];
                                            if (nb.isl != null && nb.top <= c.top - 3) { rimEdge = true; break; }
                                        }
                                        if (rimEdge && (c.waterTop != Integer.MIN_VALUE || hash(x, z, 63) < 0.82))
                                            yield hash(x, z, 64) < 0.38 ? limeMossy : lime;
                                        yield vnoise(x * 0.14, z * 0.14, 67) > 0.72 ? moss : gardenGrass;
                                    }
                                    boolean groveTan = c.isl.kind == SizoGrove.KIND || (c.isl.kind != 4 && SizoGrove.inside(x + 0.5, z + 0.5));
                                    yield groveTan
                                            ? (vnoise(x * 0.15, z * 0.15, 66) > 0.58 ? moss : grass)
                                            : (c.isl.kind != 4 && vnoise(x * 0.11, z * 0.11, 21) > 0.74 || hash(x, z, 23) < 0.012) ? moss : grass;
                                }
                            };
                        } else if (y == c.top - 1) {
                            // Рев 14: под травой полки — не кремовая корка, а слой земли/камня,
                            // как на референсе: трава не «наклеена» на известняк.
                            s = c.garden ? soil : crust;
                        } else if (c.garden && y >= c.top - 14) {
                            // стены полок — светлый известняк пластами (на фото обрыв цельно светлый),
                            // у подножия — мшистый
                            s = (y <= c.top - 10 && hash(x, y, z) < 0.45) ? limeMossy : lime;
                        } else if (y <= c.bottom + 1 && hash(x, z, y) < 0.5 || hash(x, y, z) < 0.07) {
                            s = mossy;
                        } else {
                            s = stone;
                            // улучшение биомов: ауритовая руда (монетоносная) змеится вдоль ярусов Висячих садов,
                            // прячась на глубине 2..6 под поверхностью полки - волно-лентовые прожилки с промывками
                            if (c.garden && y <= c.top - 2 && y >= c.top - 6
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
                            } else if (c.garden) {
                                // Рев 14.3, по референсу: газон садов — ЯРКО-ЗЕЛЁНЫЙ (это трава полки),
                                // а не белое поле. Раньше 52 % колонок забивалось elisar_tall_grass,
                                // у которой текстура почти белая (200,195,183) — кадры выглядели снежными.
                                // Теперь: редкие светлые былинки, островки мелких цветов (красный/жёлтый/
                                // синий — как на фото) и низкие стелющиеся кусты.
                                if (h < 0.10) chunk.setBlockState(mp.set(x, c.top + 1, z), tall, false);
                                else if (h < 0.26 && vnoise(x * 0.1, z * 0.1, 154) > 0.42) {
                                    int f = (int) (hash(x, z, 155) * 100);
                                    BlockState bloom = f < 42 ? Blocks.POPPY.defaultBlockState()
                                            : f < 72 ? Blocks.DANDELION.defaultBlockState()
                                            : f < 88 ? Blocks.CORNFLOWER.defaultBlockState()
                                            : f < 95 ? Blocks.OXEYE_DAISY.defaultBlockState()
                                            : st(ElisarBlocks.PALE_BLOOM);
                                    chunk.setBlockState(mp.set(x, c.top + 1, z), bloom, false);
                                } else if (h < 0.30 && chunk.getBlockState(mp.set(x, c.top + 1, z)).isAir()) {
                                    chunk.setBlockState(mp.set(x, c.top + 1, z), gardenLeaf, false);   // низкий стелющийся куст
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
                // biome 07: ПЛЮЩ ПО ОБРЫВАМ (референс). Раньше «шторы» собирались из белой листвы
                // сизо, а ванильный VineBlock проверяет соседний блок через chunk.getBlockState —
                // на границе чанка сосед ещё не сгенерирован, все стороны оставались false, и лоза
                // не ставилась вовсе (в /riftbiome ivy=0). Теперь сторону выбираем по сетке колонок
                // (она уже посчитана), а не по содержимому соседнего чанка.
                // Рев 14.3, по референсу: у кромки каждой полки — ВОДОПАДНЫЕ ШТОРЫ (широкие струи,
                // льющиеся через край) и длинные плети плюща по отвесной стене. И то и другое ставим
                // в СВОЮ колонку у подножия стены: писать в соседний чанк при генерации нельзя.
                if (c.garden && c.surface == S_GRASS && c.deckY == Integer.MIN_VALUE) {
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        Col n = cols[dx + 1 + d.getStepX()][dz + 1 + d.getStepZ()];
                        if (n.isl == null) continue;
                        int rise = n.top - c.top;
                        if (rise < 3) continue;                                   // стены нет — ни воды, ни плюща
                        int foot = c.waterTop != Integer.MIN_VALUE ? c.waterTop : c.top;   // вода встаёт на пруд
                        if (vnoise(x * 0.085, z * 0.085, 171) > 0.80) {            // ~1 струя на ярус, шириной 5-8
                            for (int y = foot + 1; y <= n.top; ++y) {
                                BlockPos wp = mp.set(x, y, z);
                                if (chunk.getBlockState(wp).canBeReplaced()) chunk.setBlockState(wp, gFalling, false);
                            }
                        } else if (vnoise(x * 0.21, z * 0.21, 151) > 0.32) {       // плети свисают с кромки вниз
                            int len = Math.min(rise - 1, 6 + (int) (hash(x, z, 152) * 9.0));
                            int from = Math.max(foot + 1, n.top - 1 - len);
                            BlockState face = ivyFace(d, ivy);
                            for (int y = from; y <= n.top - 1; ++y) {
                                BlockPos vp = mp.set(x, y, z);
                                BlockState cur = chunk.getBlockState(vp);
                                if (cur.isAir() || cur.canBeReplaced()) chunk.setBlockState(vp, face, false);
                            }
                            // Рев 14.14: куст, перевалившийся через кромку — на референсе край полки
                            // всегда «оброс» зеленью и свисает вниз зелёной бахромой.
                            if (hash(x, z, 153) < 0.34) {
                                BlockPos lp = mp.set(x, n.top - 1, z);
                                if (chunk.getBlockState(lp).canBeReplaced()) chunk.setBlockState(lp, gardenLeaf, false);
                            }
                        }
                        break;
                    }
                }
                if (c.waterTop != Integer.MIN_VALUE) {
                    BlockState wb = c.garden ? gWater : water;
                    for (int y = c.waterBottom; y <= c.waterTop; ++y) chunk.setBlockState(mp.set(x, y, z), wb, false);
                }
                if (c.fallTop != Integer.MIN_VALUE) {
                    // Рев 14.5: вода льётся ТОЛЬКО по воздуху. Раньше струя прописывалась на всю высоту
                    // до океана без проверки — она прорезала полки насквозь, и биом оказывался залит
                    // вертикальными водяными стенами (прогон 40693993378, кадр 42).
                    for (int y = c.fallBottom; y <= c.fallTop; ++y) {
                        BlockPos wp = mp.set(x, y, z);
                        if (chunk.getBlockState(wp).canBeReplaced()) chunk.setBlockState(wp, falling, false);
                    }
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
        // Рев 10 «Живое море 2»: сад на дне и убранство затопленных пещер — вторым проходом,
        // чтобы растения ставились на уже готовую воду и породу.
        for (int dx = 0; dx < 16; ++dx)
            for (int dz = 0; dz < 16; ++dz)
                if (seaFloor[dx][dz] != Integer.MIN_VALUE)
                    ElisarSea.decorate(chunk, mp, x0 + dx, z0 + dz, seaFloor[dx][dz], plan.ocean);
        Heightmap.primeHeightmaps(chunk, EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG));
        return CompletableFuture.completedFuture(chunk);
    }

    /**
     * Пол мира (Рев 11): две плиты бедрока у самого низа измерения плюс каменная подушка под пепельной
     * равниной адских угодьев. Адские угодья заливали только y >= ocean-2, а всё, что ниже, оставалось
     * воздухом — отсюда и «провал в пустоту» под замком. Теперь там монолит из обожжённого камня до бедрока.
     */
    private static void bedrockFloor(CityPlan plan, ChunkAccess chunk, BlockPos.MutableBlockPos mp, int x, int z,
                                     boolean ashen, Col c) {
        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        BlockState stone = st(ElisarBlocks.STONE);
        BlockState sst = st(ru.aiforge.riftworld.registry.CastleBlocks.SCORCHED_STONE);
        chunk.setBlockState(mp.set(x, MIN_Y, z), bedrock, false);
        chunk.setBlockState(mp.set(x, MIN_Y + 1, z), bedrock, false);
        if (ashen) {
            // монолит под пепельной равниной (и под замком в ней): от бедрока до подошвы равнины
            for (int y = MIN_Y + 2; y <= plan.ocean - 3; ++y) {
                boolean deep = y > MIN_Y + 4;
                chunk.setBlockState(mp.set(x, y, z), deep && hash(x, y, z) < 0.22 ? stone : sst, false);
            }
        }
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

    /** Плющ, прижатый к стене в направлении {@code d} (стена стоит с этой стороны). */
    private static BlockState ivyFace(Direction d, BlockState ivy) {
        // в ваниле «north=true» значит «опора с севера», поэтому сторона совпадает с направлением стены
        return switch (d) {
            case NORTH -> ivy.setValue(VineBlock.NORTH, true);
            case SOUTH -> ivy.setValue(VineBlock.SOUTH, true);
            case EAST -> ivy.setValue(VineBlock.EAST, true);
            default -> ivy.setValue(VineBlock.WEST, true);
        };
    }

    /**
     * Рев 14: плющ на стене обрыва. {@link net.minecraft.world.level.block.VineBlock} держится за
     * соседний твёрдый блок, поэтому выставляем только те стороны, где действительно стена — иначе
     * ванильная проверка снимет лозу в первом же тике.
     */
    private static BlockState ivyFor(ChunkAccess chunk, int x, int y, int z, BlockState ivy) {
        BlockState out = ivy;
        if (solidAt(chunk, x, y, z + 1)) out = out.setValue(VineBlock.NORTH, true);
        if (solidAt(chunk, x, y, z - 1)) out = out.setValue(VineBlock.SOUTH, true);
        if (solidAt(chunk, x + 1, y, z)) out = out.setValue(VineBlock.WEST, true);
        if (solidAt(chunk, x - 1, y, z)) out = out.setValue(VineBlock.EAST, true);
        if (solidAt(chunk, x, y + 1, z)) out = out.setValue(VineBlock.UP, true);
        return out;
    }

    private static boolean solidAt(ChunkAccess chunk, int x, int y, int z) {
        BlockState st = chunk.getBlockState(new BlockPos(x, y, z));
        return st.isSolidRender(chunk, new BlockPos(x, y, z)) && !st.canBeReplaced();
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
            // Рев 14.13: у садовых дубов широкая крона — иначе она обрезалась бы по квадрату в 4 блока
            int reach = t.kind == SizoGrove.TREE_GIANT ? 7 : t.kind == WaterfallGardens.TREE_OAK ? 10 : 4;
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
            // Рев 11: дерево строится по «чертежу» — ствол, настоящие наклонные ВЕТКИ и крона-облако
            // на их концах. Раньше сизо был набором колец, а амбра — шаром: издалека это читалось
            // сплошным квадратом. Теперь у каждого дерева свой скелет из ветвей.
            TileBlueprint bp = blueprintOf(t);
            java.util.List<int[]> cells = bp.at(x - t.x, z - t.z);
            if (cells == null) continue;
            // Рев 14: у садов свой вид дерева (kind 4) — дуб с бурой корой и зелёной кроной,
            // как на референсе; белые сизо/амбра здесь больше не растут.
            boolean gardenTree = t.kind == WaterfallGardens.TREE_OAK;
            BlockState logS = (gardenTree ? ElisarBlocks.GARDEN_LOG
                    : sizo ? ElisarBlocks.SIZO_LOG : ElisarBlocks.AMBRA_LOG).get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            BlockState leaf = (gardenTree ? ElisarBlocks.GARDEN_LEAVES
                    : sizo ? ElisarBlocks.SIZO_LEAVES : ElisarBlocks.AMBRA_LEAVES).get().defaultBlockState()
                    .setValue(LeavesBlock.PERSISTENT, true).setValue(LeavesBlock.DISTANCE, 1);
            // сначала листва, потом брёвна — ветки прорезают крону, а не тонут в ней
            for (int[] cell : cells) {
                if (cell[0] != 0) continue;
                BlockPos p = mp.set(x, base + cell[1], z);
                if (chunk.getBlockState(p).isAir() || chunk.getBlockState(p).canBeReplaced()) chunk.setBlockState(p, leaf, false);
            }
            for (int[] cell : cells) {
                if (cell[0] == 0) continue;
                BlockPos p = mp.set(x, base + cell[1], z);
                Direction.Axis ax = cell[0] == 1 ? Direction.Axis.X : cell[0] == 2 ? Direction.Axis.Z : Direction.Axis.Y;
                chunk.setBlockState(p, logS.setValue(RotatedPillarBlock.AXIS, ax), false);
            }
        }
    }

    // ------------------------------------------------------------------ чертёж дерева (ствол + ветки + крона)
    /** Ячейка чертежа: [тип (0 — листва, 1 — бревно по X, 2 — по Z, 3 — по Y), смещение по Y]. */
    private record TileBlueprint(java.util.Map<Long, java.util.List<int[]>> columns) {
        java.util.List<int[]> at(int dx, int dz) {
            if (Math.abs(dx) > 12 || Math.abs(dz) > 12) return null;
            return columns.get(((long) (dx + 16) << 8) | (dz + 16));
        }
    }

    private static final java.util.Map<Long, TileBlueprint> BLUEPRINTS = new java.util.concurrent.ConcurrentHashMap<>();

    private static TileBlueprint blueprintOf(CityPlan.Tree t) {
        long key = ((long) t.x << 42) ^ ((long) t.z << 12) ^ (t.seed & 0xFFFL) ^ ((long) t.baseY << 1);
        return BLUEPRINTS.computeIfAbsent(key, k -> buildBlueprint(t));
    }

    /**
     * Скелет дерева: вертикальный ствол, 4–6 ветвей под углом вверх (как у настоящего дерева),
     * крона-облако на каждой ветви и на макушке. Всё считается один раз и кешируется, а колонки
     * чанка просто спрашивают «что тут стоит».
     */
    private static TileBlueprint buildBlueprint(CityPlan.Tree t) {
        java.util.Map<Long, java.util.List<int[]>> cols = new java.util.HashMap<>();
        java.util.Random r = new java.util.Random(t.seed * 0x9E3779B1L);
        boolean sizo = t.kind == 0;
        // Рев 14.13: дубы Висячих садов — крупные, с широкой кроной, как на референсе
        boolean garden = t.kind == WaterfallGardens.TREE_OAK;
        int h = Math.max(5, t.h);
        java.util.function.BiConsumer<int[], Integer> put = (pos, kind) -> {
            int dx = pos[0], dy = pos[1], dz = pos[2];
            if (dy < 0) return;
            long ck = ((long) (dx + 16) << 8) | (dz + 16);
            java.util.List<int[]> list = cols.computeIfAbsent(ck, q -> new java.util.ArrayList<>());
            for (int[] e : list) if (e[0] == kind && e[1] == dy) return;      // без дублей
            list.add(new int[]{kind, dy});
        };

        // ствол
        int trunkTop = h - 1;
        for (int dy = 0; dy <= trunkTop; ++dy) put.accept(new int[]{0, dy, 0}, 3);

        // ветви: расходятся вверх от ствола
        int n = 4 + r.nextInt(3);
        java.util.List<double[]> tips = new java.util.ArrayList<>();
        for (int i = 0; i < n; ++i) {
            double a = i * Math.PI * 2 / n + r.nextDouble() * 0.55;
            double y0 = trunkTop * (0.42 + 0.34 * r.nextDouble());
            double len = 2.6 + r.nextDouble() * 2.6 * (sizo ? 1.15 : garden ? 1.25 : 1.0);
            double up = 0.5 + r.nextDouble() * 0.65;
            double bx = Math.cos(a) * len, bz = Math.sin(a) * len, by = y0 + len * up;
            int steps = (int) Math.ceil(len * up + len);
            for (int k = 0; k <= steps; ++k) {
                double f = k / (double) Math.max(1, steps);
                int px = (int) Math.round(bx * f), pz = (int) Math.round(bz * f), py = (int) Math.round(y0 + (by - y0) * f);
                double ddx = bx * (f + 0.5), ddz = bz * (f + 0.5);
                int kind = Math.abs(ddx) > Math.abs(ddz) ? 1 : 2;
                put.accept(new int[]{px, py, pz}, k == 0 || k == steps ? 3 : kind);
            }
            tips.add(new double[]{bx, by, bz});
            // крона на конце ветви: облако листвы
            crown(cols, put, bx, by, bz, 2.2 + r.nextDouble() * (sizo ? 0.9 : garden ? 0.9 : 0.6), r);
        }
        // макушка
        crown(cols, put, 0, trunkTop + 1.4, 0, (sizo ? 2.5 : garden ? 3.3 : 2.9) + r.nextDouble() * 0.5, r);
        // Рев 11.19: рваный контур кроны оставлял одиночные блоки листвы, висящие в воздухе.
        // Убираем листву, у которой меньше двух соседей (брёвна соседями считаются).
        java.util.Set<Long> solid = new java.util.HashSet<>();
        for (java.util.Map.Entry<Long, java.util.List<int[]>> en : cols.entrySet()) {
            int cx = (int) (en.getKey() >> 8) - 16, cz = (int) (en.getKey() & 0xFFL) - 16;
            for (int[] e2 : en.getValue()) solid.add(cellKey(cx, e2[1], cz));
        }
        java.util.Set<Long> lonely = new java.util.HashSet<>();
        for (java.util.Map.Entry<Long, java.util.List<int[]>> en : cols.entrySet()) {
            int cx = (int) (en.getKey() >> 8) - 16, cz = (int) (en.getKey() & 0xFFL) - 16;
            for (int[] e2 : en.getValue()) {
                if (e2[0] != 0) continue;
                if (neighbours(solid, cx, e2[1], cz) < 2) lonely.add(cellKey(cx, e2[1], cz));
            }
        }
        if (!lonely.isEmpty()) {
            for (java.util.Map.Entry<Long, java.util.List<int[]>> en : cols.entrySet()) {
                int cx = (int) (en.getKey() >> 8) - 16, cz = (int) (en.getKey() & 0xFFL) - 16;
                en.getValue().removeIf(e2 -> e2[0] == 0 && lonely.contains(cellKey(cx, e2[1], cz)));
            }
        }
        return new TileBlueprint(cols);
    }

    private static long cellKey(int x, int y, int z) {
        return ((long) (x + 32) << 40) | ((long) (z + 32) << 20) | y;
    }

    private static int neighbours(java.util.Set<Long> solid, int x, int y, int z) {
        int n = 0;
        if (solid.contains(cellKey(x + 1, y, z))) ++n;
        if (solid.contains(cellKey(x - 1, y, z))) ++n;
        if (solid.contains(cellKey(x, y + 1, z))) ++n;
        if (solid.contains(cellKey(x, y - 1, z))) ++n;
        if (solid.contains(cellKey(x, y, z + 1))) ++n;
        if (solid.contains(cellKey(x, y, z - 1))) ++n;
        return n;
    }

    /** Крона-облако: почти шар с рваным краем, чтобы листва не выглядела кубиком. */
    private static void crown(java.util.Map<Long, java.util.List<int[]>> cols,
                              java.util.function.BiConsumer<int[], Integer> put,
                              double cx, double cy, double cz, double radius, java.util.Random r) {
        int R = (int) Math.ceil(radius);
        for (int dx = -R; dx <= R; ++dx) {
            for (int dz = -R; dz <= R; ++dz) {
                for (int dy = -R; dy <= R; ++dy) {
                    double d = Math.sqrt(dx * dx + dy * dy * 1.35 + dz * dz);
                    if (d > radius) continue;
                    if (d > radius - 0.7 && r.nextDouble() < 0.22) continue;      // рваный контур, но крона плотная
                    put.accept(new int[]{(int) Math.round(cx) + dx, (int) Math.round(cy) + dy, (int) Math.round(cz) + dz}, 0);
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
        // biome 05, референс: ТЕЛЕЖКА (1/3 биомов) и ПАМЯТНИК (1/7 биомов).
        // Обе постройки собираются по блокам, как на фото: доски, брёвна-колёса, каменный
        // постамент, деревянная надстройка с цепью и янтарной вставкой.
        for (SizoGrove.Cart cart : SizoGrove.cartsIn(greg, x0, z0)) {
            Col cc = column(plan, cart.x, cart.z);
            if (cc.isl == null || cc.top <= plan.ocean + 1) continue;
            buildCart(chunk, mp, cart, cc.top + 1);
        }
        for (SizoGrove.Shrine sh : SizoGrove.shrinesIn(greg, x0, z0)) {
            Col sc = column(plan, sh.x, sh.z);
            if (sc.isl == null || sc.top <= plan.ocean + 1) continue;
            buildShrine(chunk, mp, sh, sc.top + 1);
        }
        // biome 05: the rare RUINED pale temple, alone on its terrace        // biome 05: the rare RUINED pale temple, alone on its terrace
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
    /**
     * Рев 14: тележка из референса рощи. Корпус 3×2 из досок с бортами, два бревна-колеса,
     * оглобля вперёд и горка груза внутри. Поворот {@code rot} 0..3.
     */
    private static void buildCart(ChunkAccess chunk, BlockPos.MutableBlockPos mp, SizoGrove.Cart cart, int base) {
        // на референсе тележка из тёмного дерева, а сизо-доски почти белые — берём тёмные
        BlockState plank = st(ru.aiforge.riftworld.registry.CastleBlocks.DARK_PLANKS);
        BlockState logX = st(ElisarBlocks.SIZO_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState logZ = st(ElisarBlocks.SIZO_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        // локальные оси: u — вдоль корпуса (3 блока), v — поперёк (2 блока)
        int[] du = switch (cart.rot % 4) { case 0 -> new int[]{1, 0}; case 1 -> new int[]{0, 1}; case 2 -> new int[]{-1, 0}; default -> new int[]{0, -1}; };
        int[] dv = new int[]{-du[1], du[0]};
        for (int a = -1; a <= 1; ++a) {
            for (int b = -1; b <= 0; ++b) {
                int x = cart.x + du[0] * a + dv[0] * b, z = cart.z + du[1] * a + dv[1] * b;
                chunk.setBlockState(mp.set(x, base, z), plank, false);                    // днище
                chunk.setBlockState(mp.set(x, base + 1, z), plank, false);                // борт
                if (a == 0 && b == 0) chunk.setBlockState(mp.set(x, base + 2, z), logX, false);  // горка груза
            }
        }
        // колёса: бревно вдоль оси, по одному с каждой стороны у переднего края
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            int wx = cart.x + dv[0] * sgn, wz = cart.z + dv[1] * sgn;
            chunk.setBlockState(mp.set(wx, base, wz), du[0] == 0 ? logX : logZ, false);
        }
        // оглобля: бревно вперёд от корпуса
        int hx = cart.x + du[0] * 2, hz = cart.z + du[1] * 2;
        chunk.setBlockState(mp.set(hx, base, hz), du[0] == 0 ? logZ : logX, false);
    }

    /**
     * Рев 14: памятник из референса рощи — каменный постамент 3×3, на нём деревянная
     * «пагода» с уступами, под навесом висит цепь, в постамент вставлен янтарь.
     */
    private static void buildShrine(ChunkAccess chunk, BlockPos.MutableBlockPos mp, SizoGrove.Shrine sh, int base) {
        BlockState stone = st(ru.aiforge.riftworld.registry.CastleBlocks.ASHLAR);
        BlockState stoneMossy = st(ElisarBlocks.STONE_MOSSY);
        BlockState plank = st(ru.aiforge.riftworld.registry.CastleBlocks.DARK_PLANKS);
        BlockState logY = st(ru.aiforge.riftworld.registry.CastleBlocks.DARK_BEAM);
        BlockState gold = st(ru.aiforge.riftworld.registry.TempleBlocks.GOLD);
        BlockState chain = st(ElisarBlocks.CHAIN).setValue(net.minecraft.world.level.block.ChainBlock.AXIS, Direction.Axis.Y);
        // постамент: 3×3, верхний слой — замшелый камень (как на фото)
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) {
                chunk.setBlockState(mp.set(sh.x + dx, base, sh.z + dz), (dx + dz) % 2 == 0 ? stone : stoneMossy, false);
                chunk.setBlockState(mp.set(sh.x + dx, base + 1, sh.z + dz), stone, false);
            }
        // янтарная вставка в передней грани постамента
        int[] f = switch (sh.rot % 4) { case 0 -> new int[]{0, 1}; case 1 -> new int[]{1, 0}; case 2 -> new int[]{0, -1}; default -> new int[]{-1, 0}; };
        chunk.setBlockState(mp.set(sh.x + f[0] * 2, base + 1, sh.z + f[1] * 2), gold, false);
        // деревянная надстройка с уступами
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) {
                chunk.setBlockState(mp.set(sh.x + dx, base + 2, sh.z + dz), plank, false);            // пол капища
                boolean edge = Math.abs(dx) == 1 || Math.abs(dz) == 1;
                if (edge) chunk.setBlockState(mp.set(sh.x + dx, base + 3, sh.z + dz), logY, false);   // столбики
            }
        chunk.setBlockState(mp.set(sh.x, base + 3, sh.z), chain, false);                              // висящая цепь
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) chunk.setBlockState(mp.set(sh.x + dx, base + 4, sh.z + dz), plank, false);   // крыша-уступ
        chunk.setBlockState(mp.set(sh.x, base + 5, sh.z), plank, false);                              // гребень
        // каменная ступенька перед постаментом
        chunk.setBlockState(mp.set(sh.x + f[0] * 2, base, sh.z + f[1] * 2), stoneMossy, false);
    }

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
        // Рев 10: индексы массива отсчитываются от MIN_Y (мир стал глубже: min_y = -64, height = 384)
        arr[0] = Blocks.BEDROCK.defaultBlockState();          // Рев 11: пол мира и в базовой колонке
        arr[1] = Blocks.BEDROCK.defaultBlockState();
        arr[of - MIN_Y] = ElisarBlocks.SEABED.get().defaultBlockState();
        arr[of - 1 - MIN_Y] = ElisarBlocks.SEABED.get().defaultBlockState();
        for (int y = ElisarSea.BOTTOM; y <= of - 2; ++y) arr[y - MIN_Y] = ElisarBlocks.STONE.get().defaultBlockState();
        for (int y = of + 1; y <= plan.ocean; ++y) arr[y - MIN_Y] = ElisarBlocks.WATER.get().defaultBlockState();
        if (c.isl != null) {
            for (int y = Math.max(MIN_Y, c.bottom); y <= Math.min(MIN_Y + HEIGHT - 1, c.top); ++y) arr[y - MIN_Y] = ElisarBlocks.STONE.get().defaultBlockState();
        }
        return new NoiseColumn(MIN_Y, arr);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState rs, BlockPos pos) {
        CityPlan.Island i = CityPlan.get().islandAt(pos.getX() + 0.5, pos.getZ() + 0.5);
        info.add("Elisar: " + (i == null ? "open sky" : "island t=" + String.format("%.2f", i.t(pos.getX() + 0.5, pos.getZ() + 0.5))));
    }
}
