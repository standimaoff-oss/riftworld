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
    static final int S_GRASS = 0, S_COBBLE = 1, S_PATH = 2, S_PLANK = 3, S_PLOT = 4, S_BARE = 5;

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
        /**
         * Рев 15: ГЛАДКАЯ (до квантования в ярусы) высота поверхности. Нужна Фиолетовым холмам:
         * гряды лаванды идут ПО КОНТУРУ яруса, а сам контур — это изолиния этой высоты, поэтому
         * полосы считаются прямо по ней (а не по шуму «в клетку», из-за которого поля читались
         * фиолетовой крупой).
         */
        double soft = Double.NaN;
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
            // Рев 14.30: вокруг островов Висячих садов летающий мусор не нужен — на референсе чистое
            // небо над водой, а в кадрах обломки читались как грязь.
            if (n.kind == WaterfallGardens.KIND) return;
            // Рев 15.4: над Фиолетовыми холмами и Сизо-рощей тоже чистое небо. На кадрах 49–51
            // итерации 46 вокруг луга висели серые кубы «обломков» — они читались как мусор
            // (на референсе 04 над полями ничего нет).
            if (n.kind == 7 || n.kind == SizoGrove.KIND) return;
            double t = n.t(cx, cz);
            if (t > -0.015 || t < -0.5) return;
            double p = 0.16 * (1 + t * 2.2);                 // denser right at the rim, thinning outwards
            if (pick > p || plan.islandAt(cx, cz) != null) return;
            y0 = (int) Math.floor(n.base - 22 + hash(gx, gz, 92) * 34);
        }
        double k = hash(gx, gz, 93);
        BlockState s = k < 0.34 ? st(ElisarBlocks.CRUST) : k < 0.62 ? st(ElisarBlocks.STONE) : k < 0.78 ? st(ElisarBlocks.COBBLE)
                : k < 0.92 ? st(ElisarBlocks.GRASS) : st(ElisarBlocks.STONE_MOSSY);
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
            } else if (isl.kind == 7) {                                // Фиолетовые холмы (референс 04)
                // Рев 15 — биом переделан с нуля по просьбе владельца («сейчас сделано ужасно»).
                // Что было не так на кадрах 49–51 итерации 44: серые «кучи» подпорных стенок на
                // каждом перепаде, лаванда разложена шумом в клетку (читалась фиолетовой крупой),
                // трава — белёсая элисарская (на фото луг сочный зелёный), ступени по 5 блоков.
                //
                // Что теперь:
                //   * рельеф — ШИРОКИЕ ВОЛНЫ (две крупные, одна средняя, одна мелкая рябь), а не
                //     конус: на склоне появляются настоящие холмы и ложбины, как на фото;
                //   * квантование в ярусы по 2 блока → длинные узкие террасы-ленты по изолиниям;
                //   * гладкая высота кладётся в {@code c.soft}: по ней потом режутся гряды лаванды
                //     ПО КОНТУРУ, и подпорные стенки больше не нужны (обрыв яруса и есть стенка).
                double dome = violetProfile(isl, px, pz, plan.ocean);
                c.soft = dome;
                top = violetTerrace(dome);
            }
            else if (isl.kind == SizoGrove.KIND) {                     // sizo grove: gentle forest floor, trail & ruined tower terrace
                SizoGrove.Region sr = SizoGrove.regionAt(px, pz);
                if (sr != null && sr.hub == isl) {
                    // Мягко сглаживаем центральную лесную долину вдоль тропы, чтобы мощёная дорожка шла плавно
                    double td = sr.trailDist(px, pz);
                    double flatY = sr.hub.base + 2.2 + 1.8 * (vnoise(px * 0.012, pz * 0.012, 189) - 0.5);
                    if (td < 9.0) {
                        double k = smooth(Math.max(0.0, (td - 2.4) / 6.6));
                        top = flatY + (top - flatY) * k;
                    }
                    if (sr.temple) {
                        double o = Math.max(Math.abs(px - sr.tx), Math.abs(pz - sr.tz)) - 6.5;
                        if (o < 8.0) top = o <= 0 ? sr.ty : sr.ty + (top - sr.ty) * smooth(o / 8.0);
                    }
                    double edgeJitter = 0.42 * (vnoise(px * 0.28, pz * 0.28, 191) - 0.5);
                    if (isl.t(px, pz) > 0.20 && td <= SizoGrove.Region.TRAIL_HW + edgeJitter
                            && (!sr.temple || Math.hypot(px - sr.tx, pz - sr.tz) > 4.2)) {
                        c.surface = S_PATH;
                    }
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
                    // Рев 14.26: у садов архипелаг — высоты соседей считаем по СВОЕМУ острову.
                    int low = Integer.MAX_VALUE;
                    for (int ddx = -2; ddx <= 2; ++ddx)
                        for (int ddz = -2; ddz <= 2; ++ddz) {
                            if (isl.t(px + ddx, pz + ddz) <= 0.05) continue;
                            low = Math.min(low, (int) Math.floor(terraceTop(
                                    gardenProfile(isl, px + ddx, pz + ddz, plan.ocean), plan.ocean)));
                        }
                    if (low < c.top - 1) {                    // край полки: рядом полка заметно ниже
                        // Рев 16: струя начинается В САМОЙ КРОМКЕ (в блоке дёрна, который станет
                        // водой), а не висит над травой. Иначе вода стояла «стаканом» на кромке
                        // и не лилась — владелец: «водопады должны стекать».
                        c.fallTop = c.top;
                        c.fallBottom = plan.ocean + 1;
                        c.waterTop = Integer.MIN_VALUE;
                        c.pond = false;
                    }
                }
            }
            // biome 07: terrace pools / hot springs carve back into the shelves
            WaterfallGardens.Region gr = WaterfallGardens.regionAt(px, pz);
            if (isl.kind == WaterfallGardens.KIND && gr != null && gr.owns(isl)) {
                c.garden = true;
                int ct = c.top;
                if (ct > plan.ocean + 2) {
                    double pn = vnoise(px * 0.09, pz * 0.09, 131);
                    if (pondAt(plan, gr, isl, px, pz, ct)) {
                        int pd = pn > 0.75 ? 3 : 2;
                        c.waterTop = ct;
                        c.waterBottom = ct - pd + 1;
                        c.top = ct - pd;
                        c.pond = true;
                        c.springy = hash(x, z, 132) < 0.30;       // geothermal core: warm glow under the water
                    } else {
                        c.pondRim = pondAt(plan, gr, isl, px + 1, pz, ct) || pondAt(plan, gr, isl, px - 1, pz, ct)
                                || pondAt(plan, gr, isl, px, pz + 1, ct) || pondAt(plan, gr, isl, px, pz - 1, ct);
                    }
                }
            }
            double t = isl.t(px, pz);
            double depth = 3 + isl.depth * Math.pow(Math.max(t, 0), 0.55) * (0.72 + 0.28 * vnoise(px * 0.07, pz * 0.07, 11));
            double stal = Math.max(0, vnoise(px * 0.21, pz * 0.21, 12) - 0.6) * 48 * Math.pow(Math.max(t, 0), 0.3);
            c.bottom = Math.max(plan.ocean + 6, (int) Math.floor(c.top - depth - stal));
            c.bottom = Math.min(c.bottom, c.top - 2);
            if (isl.kind == WaterfallGardens.KIND) {
                // Подводные ярусы (ocean - 9 и ocean - 4) — голый камень S_BARE под водой,
                // а основание острова — сплошной камень до самого бедрока без единой пустоты.
                if (c.top <= plan.ocean) {
                    c.surface = S_BARE;
                    c.waterTop = Integer.MIN_VALUE;
                    c.pond = false;
                    c.pondRim = false;
                    c.fallTop = Integer.MIN_VALUE;
                }
                c.bottom = Math.min(c.bottom, Math.min(plan.ocean - 2, c.top - 4));
            }
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
            applyVioletStream(c, plan, px, pz, vr, tt);
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
        // Ниже уровня моря: 2 пустых каменных подводных яруса (ocean - 9 и ocean - 4)
        double rimWave = 0.022 * (vnoise(px * 0.045, pz * 0.045, 60) - 0.5);
        if (t + rimWave < 0.08) return ocean - 9.0;
        if (t + rimWave < 0.16) return ocean - 4.0;

        // Нижний садовый ярус — строго на уровне моря (ocean + 1), выше — ступенчатые полки
        double tg = Math.max(0.0, (t - 0.16) / 0.84);
        double rim = ocean + 1.2;
        double dome = rim + (isl.base + 8 - rim) * Math.pow(tg, 0.88);
        double lobe = (4.6 * (vnoise(px * 0.028, pz * 0.028, 61) - 0.5)
                     + 1.1 * (vnoise(px * 0.055, pz * 0.055, 62) - 0.5)) * smooth(tg / 0.18);
        return Math.max(rim, dome + lobe);
    }

    /**
     * Фиолетовые холмы: поля-террасы с подпорными стенками (референс 04).
     * Рев 14.42: шаг 3 блока давал на склоне 10–12 ступеней-пятачков; на фото ступени крупные,
     * их 4–6 на холм. Шаг 5 блока — ровно столько.
     */
    /**
     * Рев 15: гладкий профиль луга Фиолетовых холмов (ДО квантования в ярусы) — одна формула на
     * рельеф, гряды лаванды, посадку церквей и камеры CI. Широкие волны холмов + наплыв от кромки
     * острова, как на референсе 04.
     */
    public static double violetProfile(CityPlan.Island isl, double px, double pz, int ocean) {
        double tm = Math.max(0.0, Math.min(1.0, isl.t(px, pz)));
        double rimY = ocean + 2.2;
        // Референс 04_violet_hills.png: дорога вьётся по пологой долине, а ПО ОБЕ СТОРОНЫ ВОКРУГ ДОРОГИ
        // поднимаются выразительные ярусные лавандовые холмы (не один пик по центру острова и не блин).
        VioletHills.Region vr = VioletHills.regionAt(px, pz);
        double rd = vr != null ? vr.mainRoadDist(px, pz) : 28.0;
        double brd = vr != null ? vr.branchRoadDist(px, pz) : 40.0;
        double valleyFloor = ocean + 11.8
                + 2.8 * (vnoise(px * 0.0046, pz * 0.0046, 70) - 0.5)
                + 1.0 * (vnoise(px * 0.014, pz * 0.014, 74) - 0.5);
        // Несколько отдельных мягких холмов слева и справа от дороги (+9..+17 блоков над дорожкой)
        double hillWave = 0.42
                + 0.78 * vnoise(px * 0.0086, pz * 0.0086, 71)
                + 0.34 * (vnoise(px * 0.019, pz * 0.019, 72) - 0.35);
        double flankMask = smooth(Math.max(0.0, Math.min(1.0, (rd - 3.8) / 22.0)));
        double branchCut = 1.0 - 0.42 * Math.exp(-(brd * brd) / (9.0 * 9.0));
        double hills = Math.max(0.0, 13.4 * hillWave) * flankMask * branchCut
                + 0.85 * (vnoise(px * 0.026, pz * 0.026, 73) - 0.5) * flankMask;
        double inland = smooth(tm / 0.17);
        return rimY + (valleyFloor + hills - rimY) * inland;
    }

    /** Верх луга в точке (та же формула, что в {@link #column}), для посадки церквей и камер. */
    public static int violetTop(CityPlan.Island isl, double px, double pz, int ocean) {
        return (int) Math.floor(violetTerrace(violetProfile(isl, px, pz, ocean)));
    }

    public static final int VIOLET_STEP = 1;

    private static double violetTerrace(double top) {
        return Math.floor(top / (double) VIOLET_STEP) * VIOLET_STEP + 1.0;
    }

    /** Terraced top estimate of a garden column (bridge anchor heights in WaterfallGardens). */
    public static int gardenTerraceTop(CityPlan.Island isl, double px, double pz, int ocean) {
        return (int) Math.floor(terraceTop(gardenProfile(isl, px, pz, ocean), ocean));
    }

    /** Biome 07 pond-mask test stateless over garden region: shelf cells the pond noise claims. */
    private static boolean pondAt(CityPlan plan, WaterfallGardens.Region rg, CityPlan.Island isl, double px, double pz, int maxTop) {
        if (vnoise(px * 0.09, pz * 0.09, 131) <= 0.84) return false;
        if (isl.t(px, pz) < 0.24) return false; // пруды только внутри надводных полок с учётом формы острова
        return maxTop > plan.ocean + 3 && maxTop < plan.ocean + 46;
    }

    /**
     * Рев 15: ручей Фиолетовых холмов идёт ПО ТЕРРАСАМ, а не одним уровнем.
     *
     * <p>Раньше ручей вырезался на единой высоте ({@code riverLevel = base-2}): на кадрах 49–51 это
     * была прямая бирюзовая канава с вертикальными стенками в 10+ блоков, перерезающая все ярусы
     * насквозь. Теперь уровень воды берётся от ПОЛКИ, на которой стоит колонка: ручей спускается
     * каскадом по террасам, оставаясь в одной-двух клетках от поверхности — как горный поток на
     * референсе.
     */
    private static void applyVioletStream(Col c, CityPlan plan, double px, double pz,
                                          VioletHills.Region vr, double[] tt) {
        CityPlan.Island isl = c.isl;
        if (isl == null || isl.kind != 7) return;
        if (isl.t(px, pz) < 0.13) return;
        double dm = vr.mainRoadDist(px, pz);
        double db = vr.branchRoadDist(px, pz);
        double hw = VioletHills.Region.RIVER_HW;
        double bhw = VioletHills.Region.BRANCH_HW;
        if (dm <= hw || (isl.t(px, pz) > 0.20 && db <= bhw)) {
            c.soft = Double.NaN;
            c.surface = S_COBBLE;
        } else if (dm <= hw + 1.95 || db <= bhw + 1.15) {
            // Аккуратная зелёная обочина под каменные стенки, деревянные заборы и столбы с факелами
            c.soft = Double.NaN;
        }
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
        BlockState gWater = st(ElisarBlocks.GARDEN_WATER);
        BlockState gFalling = gWater.setValue(LiquidBlock.LEVEL, 8);
        BlockState seabed = st(ElisarBlocks.SEABED), stone = st(ElisarBlocks.STONE), mossy = st(ElisarBlocks.STONE_MOSSY), soil = st(ElisarBlocks.SOIL);
        BlockState grass = st(ElisarBlocks.GRASS), cobble = st(ElisarBlocks.COBBLE), path = st(ElisarBlocks.PATH), plank = st(ElisarBlocks.SIZO_PLANKS);
        BlockState tall = st(ElisarBlocks.TALL_GRASS), flowers = st(ElisarBlocks.FLOWERS), log = st(ElisarBlocks.SIZO_LOG);
        BlockState moss = st(ElisarBlocks.MOSS), crust = st(ElisarBlocks.CRUST);
        BlockState aurite = st(ElisarBlocks.AURITE_ORE);
        // Рев 14: у биома 07 своя зелёная палитра (референс — яркая трава и дубы, а не кремовый Элисар)
        BlockState gardenGrass = st(ElisarBlocks.GARDEN_GRASS);
        // Рев 15.4: газон Фиолетовых холмов — ВАНИЛЬНАЯ короткая трава: она тинтуется цветом
        // биома (grass_color 0xB6C77A) и потому сочно-зелёная. Наша elisar_tall_grass почти белая,
        // и на кадрах 49–51 весь луг был усыпан белой «крупой».
        BlockState vGrass = Blocks.GRASS.defaultBlockState();
        // Рев 15.5: ПОЧВА луга. Раньше поверхность холмов была нашим `elisar_soil` (#A69F93 —
        // кремово-серый), из-за чего луг читался серым полем с белой крупой. Теперь дерн —
        // ванильный `grass_block` (его верх и бока тинтуются grass_color биома → шалфейная
        // зелень), а борозды между грядами — тёмная земля (`coarse_dirt`), как вспаханная
        // полоса на референсе 04.
        BlockState vTurf = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState vEarth = Blocks.COARSE_DIRT.defaultBlockState();
        // Рев 14.25: на обрывах садов — свой сочно-зелёный плющ, как на референсе
        BlockState gIvy = ElisarBlocks.GARDEN_IVY.get().defaultBlockState();
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
                                case S_COBBLE -> {
                                    if (c.isl.kind == 7) {
                                        double rh = hash(x, z, 181);
                                        yield rh < 0.62 ? lime
                                                : rh < 0.88 ? Blocks.DIORITE.defaultBlockState()
                                                : Blocks.CALCITE.defaultBlockState();
                                    }
                                    yield cobble;
                                }
                                case S_PATH -> {
                                    if (c.isl.kind == SizoGrove.KIND) {
                                        double patch = vnoise(x * 0.36, z * 0.36, 193) + 0.24 * (hash(x, z, 194) - 0.5);
                                        yield patch < 0.34 ? Blocks.COBBLESTONE.defaultBlockState()
                                                : patch < 0.52 ? (hash(x, z, 195) < 0.55
                                                    ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                                                    : Blocks.ANDESITE.defaultBlockState())
                                                : patch < 0.80 ? Blocks.DIRT_PATH.defaultBlockState()
                                                : Blocks.COARSE_DIRT.defaultBlockState();
                                    }
                                    yield path;
                                }
                                case S_PLANK -> plank;
                                case S_PLOT -> cobble;         // pebble apron around the houses (reference)
                                default -> {
                                    // Рев 15: луг Фиолетовых холмов — ЗЕЛЁНАЯ трава (на референсе сочный
                                    // зелёный газон), а не белёсая элисарская корка. Именно из-за неё
                                    // биом на кадрах читался «снежным» с фиолетовыми точками.
                                    if (c.isl.kind == 7) yield hash(x, z, 62) < 0.10 ? soil : gardenGrass;
                                    if (c.garden) {
                                        // Рев 14.28: подводная полка острова — голый камень (S_BARE)
                                        if (c.surface == S_BARE) yield hash(x, z, 65) < 0.3 ? limeMossy : lime;
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
                                            ? (vnoise(x * 0.16, z * 0.16, 66) > 0.82 ? moss : grass)
                                            : (c.isl.kind != 4 && vnoise(x * 0.11, z * 0.11, 21) > 0.74 || hash(x, z, 23) < 0.012) ? moss : grass;
                                }
                            };
                        } else if (y == c.top - 1) {
                            // Рев 14: под травой полки — не кремовая корка, а слой земли/камня,
                            // как на референсе: трава не «наклеена» на известняк.
                            s = c.garden || c.isl.kind == 7 ? soil : crust;
                        } else if (c.isl.kind == 7) {
                            // Рев 15: обрыв яруса Фиолетовых холмов — каменная стенка террасы
                            // (на фото каждая ступень подпёрта кладкой). Верхний блок — земля,
                            // ниже 3–4 блока светлого камня, у подножия — мшистый.
                            s = y >= c.top - 5
                                    ? (hash(x, y, z) < 0.30 ? limeMossy : lime)
                                    : (hash(x, y, z) < 0.10 ? mossy : stone);
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
                    if (c.isl.kind == WaterfallGardens.KIND) {
                        // Сплошное каменное основание острова до бедрока (0 дыр) + вода над подводными ярусами (0 воздуха под водой).
                        for (int y = MIN_Y + 2; y < c.bottom; ++y) {
                            BlockState base = hash(x, y, z) < 0.18 ? limeMossy : stone;
                            chunk.setBlockState(mp.set(x, y, z), base, false);
                        }
                        if (c.top < plan.ocean) {
                            for (int y = c.top + 1; y <= plan.ocean; ++y) {
                                chunk.setBlockState(mp.set(x, y, z), water, false);
                            }
                        }
                    }
                    // half-step slabs where a street or country lane climbs one block (smooth walking, no hard ledges)
                    if ((c.surface == S_COBBLE || (c.isl.kind == SizoGrove.KIND && c.surface == S_PATH))
                            && c.waterTop == Integer.MIN_VALUE && c.deckY == Integer.MIN_VALUE && c.fallTop == Integer.MIN_VALUE) {
                        for (Direction d : Direction.Plane.HORIZONTAL) {
                            Col n = cols[dx + 1 + d.getStepX()][dz + 1 + d.getStepZ()];
                            if (n.isl != null && n.top == c.top + 1 && n.deckY == Integer.MIN_VALUE
                                    && (n.surface == S_COBBLE || n.surface == S_PATH)) {
                                BlockState roadSlab = c.isl.kind == 7 ? Blocks.SMOOTH_STONE_SLAB.defaultBlockState()
                                        : c.isl.kind == SizoGrove.KIND ? Blocks.COBBLESTONE_SLAB.defaultBlockState()
                                        : st(ElisarBlocks.COBBLE_SLAB);
                                chunk.setBlockState(mp.set(x, c.top + 1, z), roadSlab, false);
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
                            if (violet) {
                                // Небольшая редкая примесь (~4 %) других цветов по референсу 04_violet_hills.png
                                double acc = hash(x, z, 99);
                                BlockState accentBloom = acc < 0.015 ? Blocks.PINK_TULIP.defaultBlockState()
                                        : acc < 0.026 ? Blocks.ALLIUM.defaultBlockState()
                                        : acc < 0.034 ? Blocks.ORANGE_TULIP.defaultBlockState()
                                        : acc < 0.041 ? Blocks.OXEYE_DAISY.defaultBlockState()
                                        : acc < 0.046 ? Blocks.CORNFLOWER.defaultBlockState()
                                        : acc < 0.050 ? Blocks.DANDELION.defaultBlockState()
                                        : null;
                                if (Double.isNaN(c.soft)) {
                                    // Чистая травяная обочина под придорожные каменные стенки, заборы и столбы с факелами
                                    chunk.setBlockState(mp.set(x, c.top, z), vTurf, false);
                                    VioletHills.Region vr0 = VioletHills.regionAt(x + 0.5, z + 0.5);
                                    double md = vr0 != null ? vr0.mainRoadDist(x + 0.5, z + 0.5) : 99.0;
                                    if (md > VioletHills.Region.RIVER_HW + 1.35) {
                                        if (accentBloom != null) {
                                            chunk.setBlockState(mp.set(x, c.top + 1, z), accentBloom, false);
                                        } else if (h < 0.55) {
                                            BlockState rimBloom = st(h < 0.24 ? ElisarBlocks.VIOLET_BLOOM
                                                    : h < 0.44 ? ElisarBlocks.LILAC_BLOOM : ElisarBlocks.PALE_BLOOM);
                                            chunk.setBlockState(mp.set(x, c.top + 1, z), rimBloom, false);
                                        } else if (h < 0.72) {
                                            chunk.setBlockState(mp.set(x, c.top + 1, z), vGrass, false);
                                        }
                                    }
                                } else {
                                    double soft = c.soft;
                                    boolean stepLip = false;
                                    for (Direction dd : Direction.Plane.HORIZONTAL) {
                                        Col nb = cols[dx + 1 + dd.getStepX()][dz + 1 + dd.getStepZ()];
                                        if (nb.isl != null && nb.top < c.top) { stepLip = true; break; }
                                    }
                                    double rp = soft * 0.55 + (x * 0.21 + z * 0.15) + 2.8 * vnoise(x * 0.015, z * 0.015, 91);
                                    double frac = rp - Math.floor(rp);
                                    boolean row = frac < 0.76 && !stepLip;
                                    double band = vnoise(x * 0.013, z * 0.013, 94) + (hash(x, z, 98) - 0.5) * 0.18;
                                    BlockState bloom = st(band < 0.44 ? ElisarBlocks.VIOLET_BLOOM
                                            : band < 0.84 ? ElisarBlocks.LILAC_BLOOM : ElisarBlocks.PALE_BLOOM);
                                    chunk.setBlockState(mp.set(x, c.top, z), vTurf, false);
                                    if (accentBloom != null) {
                                        chunk.setBlockState(mp.set(x, c.top + 1, z), accentBloom, false);
                                    } else if (row) {
                                        chunk.setBlockState(mp.set(x, c.top + 1, z), st(ElisarBlocks.LAVENDER_BED), false);
                                        chunk.setBlockState(mp.set(x, c.top + 2, z), bloom, false);
                                    } else if (stepLip && h < 0.42) {
                                        chunk.setBlockState(mp.set(x, c.top + 1, z), h < 0.18 ? Blocks.FERN.defaultBlockState() : vGrass, false);
                                    } else if (h < 0.88) {
                                        chunk.setBlockState(mp.set(x, c.top + 1, z), h < 0.34 ? st(ElisarBlocks.LAVENDER_BED) : bloom, false);
                                    } else {
                                        chunk.setBlockState(mp.set(x, c.top + 1, z), vGrass, false);
                                    }
                                }
                            } else if (c.garden) {
                                if (h < 0.10) chunk.setBlockState(mp.set(x, c.top + 1, z), tall, false);
                                else if (h < 0.26 && vnoise(x * 0.1, z * 0.1, 154) > 0.42) {
                                    int f = (int) (hash(x, z, 155) * 100);
                                    BlockState bloom = f < 42 ? Blocks.POPPY.defaultBlockState()
                                            : f < 72 ? Blocks.DANDELION.defaultBlockState()
                                            : f < 88 ? Blocks.CORNFLOWER.defaultBlockState()
                                            : f < 95 ? Blocks.OXEYE_DAISY.defaultBlockState()
                                            : st(ElisarBlocks.PALE_BLOOM);
                                    chunk.setBlockState(mp.set(x, c.top + 1, z), bloom, false);
                                }
                            } else if (c.isl.kind == SizoGrove.KIND || (c.isl.kind != 4 && SizoGrove.inside(x + 0.5, z + 0.5))) {
                                // biome 05 (05_biome_sizo_grove.png): серебристо-мятная трава и нежные васильково-фиолетовые соцветия
                                if (h < 0.25) {
                                    chunk.setBlockState(mp.set(x, c.top + 1, z), tall, false);
                                } else if (h < 0.38 && vnoise(x * 0.085, z * 0.085, 95) > 0.48) {
                                    int f = (int) (hash(x, z, 96) * 100);
                                    chunk.setBlockState(mp.set(x, c.top + 1, z),
                                            st(f < 78 ? ElisarBlocks.FLOWERS : ElisarBlocks.PALE_BLOOM), false);
                                }
                            } else if (h < 0.09) chunk.setBlockState(mp.set(x, c.top + 1, z), tall, false);
                            else if (h < 0.105) chunk.setBlockState(mp.set(x, c.top + 1, z), flowers, false);
                        }
                    }
                }
                // Рев 15: СТАРЫЕ «ПОДПОРНЫЕ СТЕНКИ» УДАЛЕНЫ. Владелец видел на кадрах 49–51 серые
                // кучи на каждом перепаде: проход ставил по 2 блока кладки в НАДземную часть у любой
                // колонки, у которой сосед ниже на террасу, — то есть прямо на поле. Теперь стенку
                // делает сам обрыв яруса (2 блока камня под почвой, см. цикл заполнения), а сверху
                // остаётся ровная травяная кромка. Надземных камней в полях больше нет.
                // biome 07: ПЛЮЩ ПО ОБРЫВАМ (референс). Раньше «шторы» собирались из белой листвы
                // сизо, а ванильный VineBlock проверяет соседний блок через chunk.getBlockState —
                // на границе чанка сосед ещё не сгенерирован, все стороны оставались false, и лоза
                // не ставилась вовсе (в /riftbiome ivy=0). Теперь сторону выбираем по сетке колонок
                // (она уже посчитана), а не по содержимому соседнего чанка.
                // Рев 14.3, по референсу: у кромки каждой полки — ВОДОПАДНЫЕ ШТОРЫ (широкие струи,
                // льющиеся через край) и длинные плети плюща по отвесной стене. И то и другое ставим
                // в СВОЮ колонку у подножия стены: писать в соседний чанк при генерации нельзя.
                if (c.garden && (c.surface == S_GRASS || c.surface == S_BARE) && c.deckY == Integer.MIN_VALUE) {
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        Col n = cols[dx + 1 + d.getStepX()][dz + 1 + d.getStepZ()];
                        if (n.isl == null || n.surface != S_GRASS) continue;
                        int foot = c.surface == S_BARE ? plan.ocean : (c.waterTop != Integer.MIN_VALUE ? c.waterTop : c.top);
                        int rise = n.top - foot;
                        if (rise < 4) continue;
                        int nxp = x + d.getStepX(), nzp = z + d.getStepZ();
                        WaterfallGardens.Region rgf = WaterfallGardens.regionAt(nxp, nzp);
                        boolean preselected = rgf != null && WaterfallGardens.fallNear(rgf, nxp + 0.5, nzp + 0.5) != null;
                        if (preselected || vnoise(x * 0.085, z * 0.085, 171) > 0.87) {
                            if (nxp >= x0 && nxp <= x0 + 15 && nzp >= z0 && nzp <= z0 + 15) {
                                chunk.setBlockState(mp.set(nxp, n.top, nzp), gWater, false);
                            }
                            for (int y = foot + 1; y <= n.top; ++y) {
                                BlockPos wp = mp.set(x, y, z);
                                if (chunk.getBlockState(wp).canBeReplaced())
                                    chunk.setBlockState(wp, y == n.top ? gWater : gFalling, false);
                            }
                        } else if (vnoise(x * 0.21, z * 0.21, 151) > 0.30) {       // плети плюща прижаты к стене обрыва
                            int len = Math.min(rise - 1, 6 + (int) (hash(x, z, 152) * 9.0));
                            int from = Math.max(foot + 1, n.top - 1 - len);
                            BlockState face = ivyFace(d, gIvy);
                            for (int y = from; y <= n.top - 1; ++y) {
                                BlockPos vp = mp.set(x, y, z);
                                BlockState cur = chunk.getBlockState(vp);
                                if (cur.isAir() || cur.canBeReplaced()) chunk.setBlockState(vp, face, false);
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
                    //
                    // Рев 16: ВОДОПАД ДОЛЖЕН ТЕЧЬ. В кромке полки (fallTop) теперь стоит настоящий
                    // ИСТОЧНИК воды: он вырезает в дёрне ложбину и льётся вниз, а ванильная физика
                    // поддерживает струю, пока чанк тикает. Ниже источника — падающая вода, чтобы
                    // струя читалась сразу после генерации, не дожидаясь первых тиков жидкости.
                    BlockPos lip = mp.set(x, c.fallTop, z);
                    chunk.setBlockState(lip, c.garden ? gWater : water, false);   // ложбина в кромке = исток
                    BlockState fallW = c.garden ? gFalling : falling;
                    for (int y = c.fallBottom; y < c.fallTop; ++y) {
                        BlockPos wp = mp.set(x, y, z);
                        if (chunk.getBlockState(wp).canBeReplaced()) chunk.setBlockState(wp, fallW, false);
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
        if (solidAt(chunk, x, y, z - 1)) out = out.setValue(VineBlock.NORTH, true);
        if (solidAt(chunk, x, y, z + 1)) out = out.setValue(VineBlock.SOUTH, true);
        if (solidAt(chunk, x - 1, y, z)) out = out.setValue(VineBlock.WEST, true);
        if (solidAt(chunk, x + 1, y, z)) out = out.setValue(VineBlock.EAST, true);
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
        // Рев 16 — ВСЕ ДЕРЕВЬЯ СТАВИТ АДДОН DYNAMIC TREES. Владелец: «янтарник, сизоствол, садовый
        // дуб и др. должны меняться аддоном DT, а сейчас в части биомов обычные деревья». Пока DT
        // загружен, обычные (stock) деревья генератора не ставятся вообще: их место занимает
        // DtGround.plantChunk (см. applyBiomeDecoration) с видом по биому — ambra / sizo /
        // giant_sizo / garden_oak / lilac. Без DT мод по-прежнему строит свои деревья, чтобы
        // биомы не оставались лысыми.
        // Исключение — сухие обгорелые стволы адских угодий (kind 2): это не дерево, а декорация.
        boolean dtTrees = DtGround.dtPresent();
        java.util.List<CityPlan.Tree> wild = WildIslands.treesNear(x, z);
        java.util.List<CityPlan.Tree> all = wild.isEmpty() ? plan.at(x, z).trees : new java.util.ArrayList<>(plan.at(x, z).trees);
        if (!wild.isEmpty()) all.addAll(wild);
        for (CityPlan.Tree t : all) {
            if (dtTrees && t.kind != 2) continue;              // деревья отданы аддону DT
            int dx = x - t.x, dz = z - t.z;
            // Рев 14.13: у садовых дубов широкая крона — иначе она обрезалась бы по квадрату в 4 блока
            int reach = t.kind == SizoGrove.TREE_GIANT ? 7
                    : t.kind == WaterfallGardens.TREE_OAK ? 10 : 4;
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
            boolean violetTree = false;   // Рев 16: обычных фиолетовых деревьев в холмах больше нет
            BlockState logS = (gardenTree ? ElisarBlocks.GARDEN_LOG
                    : sizo || violetTree ? ElisarBlocks.SIZO_LOG : ElisarBlocks.AMBRA_LOG).get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            BlockState leaf = (gardenTree ? ElisarBlocks.GARDEN_LEAVES
                    : violetTree ? ElisarBlocks.VIOLET_LEAVES
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
        // Рев 14.36: на референсе 04 кроны фиолетовых деревьев — широкие и пышные (в 2 раза шире
        // ствола), а не «шарик на палке». Даём им отдельный масштаб кроны.
        boolean violetCanopy = false;     // Рев 16: фиолетовые деревья ставит аддон DT (riftworlddt:lilac)
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
            double len = 2.6 + r.nextDouble() * 2.6 * (violetCanopy ? 1.45 : sizo ? 1.15 : garden ? 1.25 : 1.0);
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
            // крона на конце ветви: облако листвы.
            // Рев 15.4: у фиолетовых деревьев крону целиком рисует {@link #violetUmbrella()};
            // облака на концах ветвей давали рядом с зонтом «шишки» и рваные клочья.
            if (!violetCanopy) {
                crown(cols, put, bx, by, bz,
                        (2.2 + r.nextDouble() * (sizo ? 0.9 : garden ? 0.9 : 0.6)), r);
            }
        }
        // макушка
        if (violetCanopy) {
            // Рев 15, по референсу 04: крона Фиолетовых холмов — плотный ШИРОКИЙ зонт из наложенных
            // ярусов листвы (как сиреневое дерево), а не «шарик на палке» из отдельных облачков.
            violetUmbrella(cols, put, trunkTop, r);
        } else {
            crown(cols, put, 0, trunkTop + 1.4, 0,
                    (sizo ? 2.5 : garden ? 3.3 : 2.9) + r.nextDouble() * 0.5, r);
        }
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

    /**
     * Рев 15: крона-зонт для дерева Фиолетовых холмов (референс 04).
     *
     * <p>Складывается из четырёх-пяти горизонтальных ярусов листвы: самый широкий — в середине,
     * кверху и книзу ярусы уже. Получается пышная вытянутая крона, как у сирени, при этом нижняя
     * граница ровная и «читается» с земли (раньше это был один шар, и издалека крона выглядела
     * кубиком на палке).
     */
    private static void violetUmbrella(java.util.Map<Long, java.util.List<int[]>> cols,
                                       java.util.function.BiConsumer<int[], Integer> put,
                                       int trunkTop, java.util.Random r) {
        // Рев 15.6: крона — ПРИПЛЮСНУТЫЙ ЭЛЛИПСОИД (не стопка дисков). Радиус яруса считается по
        // формуле сферы rad = R*√(1-(dy/H)²), поэтому силуэт получается плавным куполом, как у
        // сирени на референсе 04, а не «капустой» из ровных слоёв (так было в итерациях 47–48).
        double baseR = 5.2 + r.nextDouble() * 1.3;
        double squash = 0.78 + r.nextDouble() * 0.10;       // крона шире, чем глубже
        double H = 2.6 + r.nextDouble() * 0.8;             // полувысота купола
        double cy = trunkTop + 0.4;                        // центр купола чуть выше ствола
        int y0 = (int) Math.floor(cy - H), y1 = (int) Math.ceil(cy + H);
        for (int y = y0; y <= y1; ++y) {
            double dy = (y - cy) / H;
            if (Math.abs(dy) > 1.0) continue;
            double rad = baseR * Math.sqrt(1.0 - dy * dy);
            if (rad < 0.7) continue;
            int R = (int) Math.ceil(rad) + 1;
            for (int dx = -R; dx <= R; ++dx) {
                for (int dz = -R; dz <= R; ++dz) {
                    double d = Math.hypot(dx, dz / squash);
                    if (d > rad + (r.nextDouble() - 0.5) * 0.7) continue;
                    if (d > rad - 0.9 && r.nextDouble() < 0.22) continue;      // рваный контур
                    put.accept(new int[]{dx, y, dz}, 0);
                }
            }
        }
    }

    /** Крона-облако: почти шар с рваным краем, чтобы листва не выглядела кубиком. */
    private static void crown(java.util.Map<Long, java.util.List<int[]>> cols,
                              java.util.function.BiConsumer<int[], Integer> put,
                              double cx, double cy, double cz, double radius, java.util.Random r) {
        int R = (int) Math.ceil(radius);
        for (int dx = -R; dx <= R; ++dx) {
            for (int dz = -R; dz <= R; ++dz) {
                for (int dy = -R; dy <= R; ++dy) {
                    // Рев 14.37: крона — ВЫТЯНУТЫЙ ВВЕРХ шар (на референсе 04 кроны пышные и высокие,
                    // а не блины). Плюс больше рваного контура по внешней оболочке, иначе издалека
                    // листва читается сплошным кубом.
                    double d = Math.sqrt(dx * dx + dy * dy * 0.86 + dz * dz);
                    if (d > radius) continue;
                    if (d > radius - 0.9 && r.nextDouble() < 0.34) continue;      // рваный контур
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
        AshenLands.Region ashReg = AshenLands.regionAt(x0 + 8, z0 + 8);
        if (ashReg != null && x0 + 15 >= ashReg.x0 && x0 <= ashReg.x0 + CastleSite.TW - 1
                && z0 + 15 >= ashReg.z0 && z0 <= ashReg.z0 + CastleSite.TD - 1) {
            stm.get(new ResourceLocation("riftworld", "castle/castle")).ifPresent(tpl -> {
                StructurePlaceSettings s = new StructurePlaceSettings().setBoundingBox(box).setIgnoreEntities(true).setKnownShape(true);
                BlockPos pos = new BlockPos(ashReg.x0, CastleSite.Y, ashReg.z0);
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
        // Референс 04_violet_hills.png: вдоль извилистой светлой дороги стоят каменные столбы с факелами,
        // низкие каменные стенки и деревянные заборы с проходами к лавандовым склонам (церквей нет).
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        VioletHills.Region vreg = VioletHills.regionAt(x0 + 8, z0 + 8);
        if (vreg != null) {
            buildVioletRoadside(chunk, mp, plan, vreg, x0, z0);
        }
        // biome 05 (05_biome_sizo_grove.png): ступенчатые замшелые серые валуны и парные путевые столбы с янтарным огнём
        SizoGrove.Region greg = SizoGrove.regionAt(x0 + 8, z0 + 8);
        if (greg != null) {
            BlockState rockAndesite = Blocks.ANDESITE.defaultBlockState();
            BlockState rockCobble = Blocks.COBBLESTONE.defaultBlockState();
            BlockState rockMossy = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            BlockState rockPale = st(ElisarBlocks.STONE);
            BlockState mossCap = st(ElisarBlocks.MOSS);
            for (SizoGrove.Boulder b : greg.boulders) {
                if (b.x + b.r < x0 - 1 || b.x - b.r > x0 + 16 || b.z + b.r < z0 - 1 || b.z - b.r > z0 + 16) continue;
                Col bc = column(plan, b.x, b.z);
                if (bc.isl == null || bc.top <= plan.ocean + 1) continue;
                double rx = b.r + 0.55, ry = b.r * 1.12 + 0.6;
                int cy = bc.top + (int) Math.round(ry * 0.22);
                int cx0 = Math.max(x0, (int) Math.floor(b.x - rx)), cx1 = Math.min(x0 + 15, (int) Math.ceil(b.x + rx));
                int cz0 = Math.max(z0, (int) Math.floor(b.z - rx)), cz1 = Math.min(z0 + 15, (int) Math.ceil(b.z + rx));
                int bseed = (int) (b.seed & 0x3FFFFFFF);
                for (int bx = cx0; bx <= cx1; ++bx)
                    for (int bz = cz0; bz <= cz1; ++bz) {
                        double dx2 = bx - b.x, dz2 = bz - b.z;
                        double warp = 0.82 + 0.34 * vnoise(bx * 0.32 + (bseed & 63), bz * 0.32, 211);
                        double hq = (dx2 * dx2 + dz2 * dz2) / (rx * rx * warp * warp);
                        if (hq >= 1.0) continue;
                        // Ступенчатый скальный уступ с толстой мшистой шапкой сверху (как на 05_biome_sizo_grove.png)
                        int maxDy = (int) Math.floor(ry * Math.pow(1.0 - hq, 0.62) + 0.45);
                        for (int dy = -maxDy - 1; dy <= maxDy; ++dy) {
                            int yy = cy + dy;
                            if (yy <= plan.ocean) continue;
                            boolean skyCap = dy == maxDy && hash(bx, bz, bseed) < 0.90;
                            boolean mossyRim = dy >= maxDy - 1 && hash(bx, yy, bz) < 0.55;
                            double rk = hash(bx, yy, bz ^ 0x55AA);
                            BlockState bs = skyCap ? mossCap
                                    : mossyRim ? rockMossy
                                    : rk < 0.44 ? rockAndesite
                                    : rk < 0.76 ? rockCobble : rockPale;
                            chunk.setBlockState(mp.set(bx, yy, bz), bs, false);
                        }
                    }
            }
            BlockState pillarBase = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
            BlockState pillarShaft = ru.aiforge.riftworld.registry.TempleBlocks.PILLAR.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            BlockState pillarAmber = ru.aiforge.riftworld.registry.TempleBlocks.GOLD_RUNE.get().defaultBlockState()
                    .setValue(ru.aiforge.riftworld.block.GoldRuneBlock.LIT, true);
            BlockState pillarCap = Blocks.STONE_BRICK_SLAB.defaultBlockState();
            for (int[] pp : SizoGrove.pillarsIn(greg, x0, z0)) {
                if (pp[0] < x0 || pp[0] > x0 + 15 || pp[1] < z0 || pp[1] > z0 + 15) continue;
                Col pc = column(plan, pp[0], pp[1]);
                if (pc.isl == null || pc.top <= plan.ocean + 1) continue;
                chunk.setBlockState(mp.set(pp[0], pc.top + 1, pp[1]), pillarBase, false);
                chunk.setBlockState(mp.set(pp[0], pc.top + 2, pp[1]), pillarShaft, false);
                chunk.setBlockState(mp.set(pp[0], pc.top + 3, pp[1]), pillarAmber, false);
                chunk.setBlockState(mp.set(pp[0], pc.top + 4, pp[1]), pillarBase, false);
                chunk.setBlockState(mp.set(pp[0], pc.top + 5, pp[1]), pillarCap, false);
            }
        }
        // biome 05 (05_biome_sizo_grove.png): деревянная тележка и резное придорожное святилище у тропы
        for (SizoGrove.Cart cart : SizoGrove.cartsIn(greg, x0, z0)) {
            Col cc = column(plan, cart.x, cart.z);
            if (cc.isl == null || cc.top <= plan.ocean + 1) continue;
            buildCart(chunk, mp, cart, cc.top + 1, x0, z0);
        }
        for (SizoGrove.Shrine sh : SizoGrove.shrinesIn(greg, x0, z0)) {
            Col sc = column(plan, sh.x, sh.z);
            if (sc.isl == null || sc.top <= plan.ocean + 1) continue;
            buildShrine(chunk, mp, sh, sc.top + 1, x0, z0);
        }
        // biome 05 (05_biome_sizo_grove.png): высокая замшелая полуразрушенная каменная башня в дымке в конце тропы
        SizoGrove.Region treg = SizoGrove.templeIn(x0, z0);
        if (treg != null) {
            buildSizoWatchtower(chunk, mp, treg, x0, z0);
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
            for (AshenLands.Placed pl : AshenLands.decorNear(x0, z0)) {
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
        // Рев 14.36 — ПОЧЕМУ АДДОН DYNAMIC TREES «НИЧЕГО НЕ МЕНЯЛ»: у нас СВОЙ applyBiomeDecoration,
        // и он никогда не запускал ванильный конвейер фич биома. DT добавляет свою фичу в биомы
        // модификатором (Phase.ADD), но она физически не выполнялась — динамические деревья и
        // кораллы не появлялись в мире вовсе (в CI они росли только через /dynamictrees settree).
        // Теперь после собственной расстановки мы честно прогоняем фичи биома. Своих фич у наших
        // биомов нет (features: [] во всех JSON), поэтому лишнего декора не появится — придут
        // только фичи модов (DT и т.п.).
        try {
            super.applyBiomeDecoration(level, chunk, sm);
        } catch (Throwable err) {
            if (FEATURE_FAILS.getAndIncrement() == 0)
                org.apache.logging.log4j.LogManager.getLogger("riftworld").warn(
                        "biome decoration features failed once (run continues): " + err);
        }
        // Рев 15.11: фича DT в нашем мире деревьев не даёт (её конвейер «диски → heightmap →
        // GroundFinder» на кастомном генераторе пуст, см. /riftdtdbg), поэтому аддон сажает
        // деревья и кораллы сам — тем же API DT (Species.generate). В биомах, которых нет в
        // списке видов аддона, ничего не происходит.
        try {
            DtGround.plantChunk(level, chunk.getPos());
        } catch (Throwable err) {
            if (DT_FAILS.getAndIncrement() == 0)
                org.apache.logging.log4j.LogManager.getLogger("riftworld").warn(
                        "DT planting failed once (run continues): " + err);
        }
        // Планируем тики жидкости у водопадов Висячих садов, чтобы струи активно стекали вниз и растекались у подножия
        if (WaterfallGardens.regionAt(x0 + 8, z0 + 8) != null) {
            var gBlock = ElisarBlocks.GARDEN_WATER.get();
            for (int dx = 0; dx < 16; ++dx)
                for (int dz = 0; dz < 16; ++dz) {
                    int wx = x0 + dx, wz = z0 + dz;
                    for (int wy = plan.ocean; wy <= plan.ocean + 52; ++wy) {
                        BlockState bs = chunk.getBlockState(mp.set(wx, wy, wz));
                        if (bs.getBlock() == gBlock) {
                            var fs = bs.getFluidState();
                            if (!fs.isEmpty()) level.scheduleTick(mp.immutable(), fs.getType(), 2);
                        }
                    }
                }
        }
    }

    /** Счётчик сбоев чужих фич: логируем только первый, чтобы не залить лог. */
    private static final java.util.concurrent.atomic.AtomicInteger FEATURE_FAILS = new java.util.concurrent.atomic.AtomicInteger();

    /** Счётчик сбоев собственной посадки DT: логируем только первый. */
    private static final java.util.concurrent.atomic.AtomicInteger DT_FAILS = new java.util.concurrent.atomic.AtomicInteger();

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
    private static void putChunk(ChunkAccess chunk, BlockPos.MutableBlockPos mp, int x0, int z0,
                                 int bx, int by, int bz, BlockState state) {
        if (bx >= x0 && bx <= x0 + 15 && bz >= z0 && bz <= z0 + 15) {
            chunk.setBlockState(mp.set(bx, by, bz), state, false);
        }
    }

    private static long packXZ(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xFFFFFFFFL);
    }

    /**
     * Референс 04_violet_hills.png: вдоль главной извилистой дороги идут чередующиеся пролёты
     * низких каменных стенок и тёплых деревянных заборов, каменные столбы-тумбы с факелами/фонарями
     * на концах пролётов и открытые проходы к лавандовым полям.
     */
    private static void buildVioletRoadside(ChunkAccess chunk, BlockPos.MutableBlockPos mp, CityPlan plan,
                                            VioletHills.Region vr, int x0, int z0) {
        // Проверяем, проходит ли дорога рядом с текущим чанком
        if (vr.mainRoadDist(x0 + 8, z0 + 8) > 22.0) return;
        double totalLen = vr.mainRoadLen();
        if (totalLen < 8.0) return;

        // kind: 1 = каменная стенка, 2 = деревянный забор, 3 = каменный столб с факелом/фонарём
        java.util.HashMap<Long, Integer> curb = new java.util.HashMap<>();
        double sideOff = VioletHills.Region.RIVER_HW + 1.05;
        for (int side = -1; side <= 1; side += 2) {
            int prevX = Integer.MIN_VALUE, prevZ = Integer.MIN_VALUE, prevKind = 0;
            int lastPierX = -999999, lastPierZ = -999999;
            for (double s = 2.0; s <= totalLen - 2.0; s += 0.35) {
                double[] pt = vr.roadSidePoint(s, side * sideOff);
                int bx = (int) Math.floor(pt[0]);
                int bz = (int) Math.floor(pt[1]);
                if (bx < x0 - 3 || bx > x0 + 18 || bz < z0 - 3 || bz > z0 + 18) {
                    prevX = Integer.MIN_VALUE;
                    continue;
                }
                if (vr.meadow.t(bx + 0.5, bz + 0.5) < 0.18) {
                    prevX = Integer.MIN_VALUE;
                    continue;
                }
                // Оставляем аккуратный открытый перекрёсток там, где отходит боковая тропинка
                if (vr.branchRoadDist(bx + 0.5, bz + 0.5) <= VioletHills.Region.BRANCH_HW + 1.15) {
                    prevX = Integer.MIN_VALUE;
                    continue;
                }
                int seg = (int) Math.floor(s / 7.0);
                int mod = Math.floorMod(seg + (side > 0 ? 2 : 0), 5);
                if (mod == 0) {
                    prevX = Integer.MIN_VALUE;
                    continue; // открытый проход в лавандовое поле
                }
                boolean nearBoundary = Math.abs(s - Math.round(s / 7.0) * 7.0) < 0.55;
                int kind = (mod == 1 || mod == 3) ? 1 : 2;
                if (nearBoundary && Math.hypot(bx - lastPierX, bz - lastPierZ) >= 4.2) {
                    kind = 3;
                    lastPierX = bx;
                    lastPierZ = bz;
                }
                if (prevX != Integer.MIN_VALUE && bx != prevX && bz != prevZ && prevKind != 0) {
                    // Замыкаем диагональный шаг 4-связным углом, чтобы забор и стенка не рвались
                    long elbowKey = packXZ(bx, prevZ);
                    curb.putIfAbsent(elbowKey, prevKind == 3 ? 1 : prevKind);
                }
                long key = packXZ(bx, bz);
                int old = curb.getOrDefault(key, 0);
                if (kind > old) curb.put(key, kind);
                prevX = bx;
                prevZ = bz;
                prevKind = kind;
            }
        }

        BlockState pierStone = st(ru.aiforge.riftworld.registry.CastleBlocks.ASHLAR);
        BlockState pierChiseled = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
        BlockState torch = Blocks.TORCH.defaultBlockState();
        BlockState lantern = st(ElisarBlocks.LANTERN);

        for (var entry : curb.entrySet()) {
            long key = entry.getKey();
            int bx = (int) (key >> 32);
            int bz = (int) key;
            if (bx < x0 || bx > x0 + 15 || bz < z0 || bz > z0 + 15) continue;
            Col c = column(plan, bx, bz);
            if (c.isl == null || c.isl.kind != 7 || c.surface == S_COBBLE || c.top <= plan.ocean + 1) continue;
            int kind = entry.getValue();
            // Очищаем цветы в точке установки придорожной ограды
            chunk.setBlockState(mp.set(bx, c.top + 2, bz), Blocks.AIR.defaultBlockState(), false);
            boolean cn = curb.containsKey(packXZ(bx, bz - 1));
            boolean cs = curb.containsKey(packXZ(bx, bz + 1));
            boolean cw = curb.containsKey(packXZ(bx - 1, bz));
            boolean ce = curb.containsKey(packXZ(bx + 1, bz));
            if (kind == 3) {
                boolean ornate = hash(bx, bz, 305) < 0.36;
                chunk.setBlockState(mp.set(bx, c.top + 1, bz), ornate ? pierChiseled : pierStone, false);
                chunk.setBlockState(mp.set(bx, c.top + 2, bz), hash(bx, bz, 306) < 0.25 ? lantern : torch, false);
            } else if (kind == 1) {
                var low = net.minecraft.world.level.block.state.properties.WallSide.LOW;
                var none = net.minecraft.world.level.block.state.properties.WallSide.NONE;
                boolean straightNS = cn && cs && !cw && !ce;
                boolean straightEW = cw && ce && !cn && !cs;
                BlockState ws = Blocks.STONE_BRICK_WALL.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.WallBlock.NORTH_WALL, cn ? low : none)
                        .setValue(net.minecraft.world.level.block.WallBlock.SOUTH_WALL, cs ? low : none)
                        .setValue(net.minecraft.world.level.block.WallBlock.WEST_WALL, cw ? low : none)
                        .setValue(net.minecraft.world.level.block.WallBlock.EAST_WALL, ce ? low : none)
                        .setValue(net.minecraft.world.level.block.WallBlock.UP, !(straightNS || straightEW));
                chunk.setBlockState(mp.set(bx, c.top + 1, bz), ws, false);
            } else {
                BlockState fs = st(ElisarBlocks.FENCE)
                        .setValue(FenceBlock.NORTH, cn)
                        .setValue(FenceBlock.SOUTH, cs)
                        .setValue(FenceBlock.WEST, cw)
                        .setValue(FenceBlock.EAST, ce);
                chunk.setBlockState(mp.set(bx, c.top + 1, bz), fs, false);
            }
        }
    }

    /**
     * Референс 05_biome_sizo_grove.png: тёплая деревянная тележка у лесной тропы —
     * дощатое днище, реечные откидные борта из еловых люков, колёса и парные оглобли вперёд.
     */
    private static void buildCart(ChunkAccess chunk, BlockPos.MutableBlockPos mp, SizoGrove.Cart cart, int base, int x0, int z0) {
        BlockState plank = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState slab = Blocks.SPRUCE_SLAB.defaultBlockState();
        BlockState wheelX = Blocks.SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState wheelZ = Blocks.SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        BlockState cargo = Blocks.BARREL.defaultBlockState();
        BlockState shaft = Blocks.SPRUCE_FENCE.defaultBlockState();
        int[] du = switch (cart.rot % 4) { case 0 -> new int[]{1, 0}; case 1 -> new int[]{0, 1}; case 2 -> new int[]{-1, 0}; default -> new int[]{0, -1}; };
        int[] dv = new int[]{-du[1], du[0]};
        for (int a = -1; a <= 1; ++a) {
            for (int b = -1; b <= 0; ++b) {
                int bx = cart.x + du[0] * a + dv[0] * b, bz = cart.z + du[1] * a + dv[1] * b;
                putChunk(chunk, mp, x0, z0, bx, base, bz, plank);
                if (a == -1 && b == 0) {
                    putChunk(chunk, mp, x0, z0, bx, base + 1, bz, cargo);
                } else if (a == 0 && b == -1) {
                    putChunk(chunk, mp, x0, z0, bx, base + 1, bz, slab);
                } else {
                    putChunk(chunk, mp, x0, z0, bx, base + 1, bz, Blocks.AIR.defaultBlockState());
                }
            }
        }
        // Реечные деревянные борта из вертикально открытых еловых люков
        Direction sideNeg = dv[0] < 0 ? Direction.WEST : dv[0] > 0 ? Direction.EAST : dv[1] < 0 ? Direction.NORTH : Direction.SOUTH;
        Direction sidePos = sideNeg.getOpposite();
        BlockState tdNeg = Blocks.SPRUCE_TRAPDOOR.defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, sidePos)
                .setValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN, true);
        BlockState tdPos = Blocks.SPRUCE_TRAPDOOR.defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, sideNeg)
                .setValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN, true);
        for (int a = -1; a <= 1; ++a) {
            putChunk(chunk, mp, x0, z0, cart.x + du[0] * a - dv[0] * 2, base + 1, cart.z + du[1] * a - dv[1] * 2, tdNeg);
            putChunk(chunk, mp, x0, z0, cart.x + du[0] * a + dv[0], base + 1, cart.z + du[1] * a + dv[1], tdPos);
        }
        // Колёса по бокам и две оглобли вперёд
        putChunk(chunk, mp, x0, z0, cart.x - dv[0] * 2, base, cart.z - dv[1] * 2, du[0] == 0 ? wheelX : wheelZ);
        putChunk(chunk, mp, x0, z0, cart.x + dv[0], base, cart.z + dv[1], du[0] == 0 ? wheelX : wheelZ);
        for (int b = -1; b <= 0; ++b) {
            BlockState sf = shaft
                    .setValue(FenceBlock.NORTH, du[1] != 0)
                    .setValue(FenceBlock.SOUTH, du[1] != 0)
                    .setValue(FenceBlock.EAST, du[0] != 0)
                    .setValue(FenceBlock.WEST, du[0] != 0);
            putChunk(chunk, mp, x0, z0, cart.x + du[0] * 2 + dv[0] * b, base, cart.z + du[1] * 2 + dv[1] * b, sf);
        }
    }

    /**
     * Референс 05_biome_sizo_grove.png: резное придорожное святилище на замшелом каменном цоколе
     * с деревянными стойками, двускатной ступенчатой крышей, цепью и тёплым янтарным камнем внутри.
     */
    private static void buildShrine(ChunkAccess chunk, BlockPos.MutableBlockPos mp, SizoGrove.Shrine sh, int base, int x0, int z0) {
        BlockState brick = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState brickMossy = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        BlockState chiseled = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
        BlockState plank = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState post = Blocks.SPRUCE_FENCE.defaultBlockState();
        BlockState slab = Blocks.SPRUCE_SLAB.defaultBlockState();
        BlockState gold = ru.aiforge.riftworld.registry.TempleBlocks.GOLD_RUNE.get().defaultBlockState()
                .setValue(ru.aiforge.riftworld.block.GoldRuneBlock.LIT, true);
        BlockState chain = st(ElisarBlocks.CHAIN).setValue(net.minecraft.world.level.block.ChainBlock.AXIS, Direction.Axis.Y);
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) {
                putChunk(chunk, mp, x0, z0, sh.x + dx, base, sh.z + dz, ((dx + dz) & 1) == 0 ? brickMossy : brick);
                putChunk(chunk, mp, x0, z0, sh.x + dx, base + 1, sh.z + dz, dx == 0 && dz == 0 ? gold : chiseled);
                putChunk(chunk, mp, x0, z0, sh.x + dx, base + 2, sh.z + dz, plank);
                if (Math.abs(dx) == 1 && Math.abs(dz) == 1) {
                    putChunk(chunk, mp, x0, z0, sh.x + dx, base + 3, sh.z + dz, post);
                } else {
                    putChunk(chunk, mp, x0, z0, sh.x + dx, base + 3, sh.z + dz, Blocks.AIR.defaultBlockState());
                }
                putChunk(chunk, mp, x0, z0, sh.x + dx, base + 4, sh.z + dz, slab);
            }
        putChunk(chunk, mp, x0, z0, sh.x, base + 3, sh.z, chain);
        for (int dx = -1; dx <= 1; ++dx) {
            putChunk(chunk, mp, x0, z0, sh.x + dx, base + 4, sh.z, plank);
        }
        putChunk(chunk, mp, x0, z0, sh.x, base + 5, sh.z, slab);
        int[] f = switch (sh.rot % 4) { case 0 -> new int[]{0, 1}; case 1 -> new int[]{1, 0}; case 2 -> new int[]{0, -1}; default -> new int[]{-1, 0}; };
        putChunk(chunk, mp, x0, z0, sh.x + f[0] * 2, base, sh.z + f[1] * 2, brickMossy);
    }

    /**
     * Референс 05_biome_sizo_grove.png: высокая замшелая полуразрушенная башня из светлого камня
     * в конце лесной тропы (7×7 в плане, высота 14 блоков, зубчатый обломанный верх, мшистые уступы и бойницы).
     */
    private static void buildSizoWatchtower(ChunkAccess chunk, BlockPos.MutableBlockPos mp,
                                            SizoGrove.Region sr, int x0, int z0) {
        if (sr.tx + 7 < x0 || sr.tx - 7 > x0 + 15 || sr.tz + 7 < z0 || sr.tz - 7 > z0 + 15) return;
        BlockState ashlar = st(ru.aiforge.riftworld.registry.CastleBlocks.ASHLAR);
        BlockState brick = st(ElisarBlocks.GARDEN_LIMESTONE);
        BlockState mossy = st(ElisarBlocks.GARDEN_LIMESTONE_MOSSY);
        BlockState cracked = st(ElisarBlocks.STONE_MOSSY);
        BlockState mossBlock = st(ElisarBlocks.MOSS);
        BlockState slab = st(ru.aiforge.riftworld.registry.TempleBlocks.SLAB);
        BlockState air = Blocks.AIR.defaultBlockState();

        // Ступенчатый замшелый цоколь из светлого известняка вокруг башни (радиус 5)
        for (int dx = -5; dx <= 5; ++dx)
            for (int dz = -5; dz <= 5; ++dz) {
                double d = Math.hypot(dx, dz);
                if (d > 5.3 || (Math.abs(dx) <= 3 && Math.abs(dz) <= 3)) continue;
                int hSkirt = d < 4.3 ? 2 : 1;
                for (int dy = 0; dy <= hSkirt; ++dy) {
                    boolean topCap = dy == hSkirt && hash(sr.tx + dx, sr.tz + dz, 401) < 0.72;
                    putChunk(chunk, mp, x0, z0, sr.tx + dx, sr.ty + dy, sr.tz + dz,
                            topCap ? mossBlock : (hash(sr.tx + dx, dy, sr.tz + dz) < 0.55 ? mossy : brick));
                }
            }

        // Направление входа — к началу лесной тропы
        double[] start = sr.trailPoint(0.25);
        double vx = start[0] - sr.tx, vz = start[1] - sr.tz;
        int doorX = Math.abs(vx) >= Math.abs(vz) ? (vx >= 0 ? 3 : -3) : 0;
        int doorZ = Math.abs(vx) < Math.abs(vz) ? (vz >= 0 ? 3 : -3) : 0;

        // Стены башни 7×7 с неровным обрушенным верхом (высота 9..14 блоков)
        for (int dx = -3; dx <= 3; ++dx)
            for (int dz = -3; dz <= 3; ++dz) {
                int bx = sr.tx + dx, bz = sr.tz + dz;
                boolean wall = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                boolean corner = Math.abs(dx) == 3 && Math.abs(dz) == 3;
                putChunk(chunk, mp, x0, z0, bx, sr.ty, bz, ((dx + dz) & 1) == 0 ? mossy : brick);
                if (!wall) {
                    for (int dy = 1; dy <= 14; ++dy) putChunk(chunk, mp, x0, z0, bx, sr.ty + dy, bz, air);
                    continue;
                }
                // Живописный зубчатый силуэт руины: один угол выше (до +14), противоположный ниже (+9..+11)
                double ruinProfile = 10.5 + 3.2 * vnoise(bx * 0.27, bz * 0.27, 409) + (corner ? 1.2 : 0.0);
                int maxH = Math.max(8, Math.min(14, (int) Math.round(ruinProfile)));
                for (int dy = 1; dy <= 14; ++dy) {
                    if (dy > maxH) {
                        putChunk(chunk, mp, x0, z0, bx, sr.ty + dy, bz, air);
                        continue;
                    }
                    // Арочный вход со стороны тропы и узкие бойницы на среднем ярусе
                    boolean doorway = (doorX != 0 && dx == doorX && Math.abs(dz) <= 1 && dy <= 4)
                            || (doorZ != 0 && dz == doorZ && Math.abs(dx) <= 1 && dy <= 4);
                    boolean slit = (dy >= 7 && dy <= 9) && ((Math.abs(dx) == 3 && dz == 0) || (Math.abs(dz) == 3 && dx == 0));
                    if (doorway || slit) {
                        putChunk(chunk, mp, x0, z0, bx, sr.ty + dy, bz, air);
                        continue;
                    }
                    double rk = hash(bx, sr.ty + dy, bz);
                    BlockState ws = (dy == maxH && rk < 0.48) ? mossBlock
                            : (dy <= 3 || rk < 0.34) ? mossy
                            : rk < 0.62 ? ashlar
                            : rk < 0.84 ? brick : cracked;
                    putChunk(chunk, mp, x0, z0, bx, sr.ty + dy, bz, ws);
                }
                if (maxH < 14 && hash(bx, bz, 412) < 0.42) {
                    putChunk(chunk, mp, x0, z0, bx, sr.ty + maxH + 1, bz, slab);
                }
            }
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
