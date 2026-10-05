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
 * Planet Elisar: floating sky islands over a flat 1-block ocean. Everything is analytic and deterministic
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

    // ------------------------------------------------------------------ the inverted tower (reference): hangs high over the gap west of the main island
    static final int TOWER_X = -104, TOWER_Z = 22, TOWER_Y0 = 148;

    private static double oct(double dx, double dz) {
        double ax = Math.abs(dx), az = Math.abs(dz);
        return Math.max(Math.max(ax, az), (ax + az) * 0.7071);
    }

    private static void tower(ChunkAccess chunk, BlockPos.MutableBlockPos mp, int x, int z) {
        double dx = x + 0.5 - (TOWER_X + 0.5), dz = z + 0.5 - (TOWER_Z + 0.5);
        if (Math.abs(dx) > 10 || Math.abs(dz) > 10) return;
        double d = oct(dx, dz);
        BlockState br = st(ElisarBlocks.TOWER_BRICKS), cr = st(ElisarBlocks.TOWER_BRICKS_CRACKED);
        int y0 = TOWER_Y0;
        for (int y = y0; y <= y0 + 100 && y < MIN_Y + HEIGHT; ++y) {
            int ly = y - y0;
            boolean solid = false;
            if (ly < 22) {                                           // downward spike
                double r = 6.2 * ly / 22.0;
                solid = d <= r + 0.3 && (d > r - 1.6 || ly < 4);
            } else if (ly < 27) {                                    // overhanging battlement ring (upside-down crenellation)
                double r = 7.6;
                solid = d <= r && d > 3.4 && !(ly == 22 && ((int) Math.floor(Math.atan2(dz, dx) / (Math.PI / 8)) & 1) == 0 && d > 6.4);
            } else if (ly < 32) {                                    // corbels stepping back to the shaft
                double r = 7.2 - (ly - 27) * 0.4;
                solid = d <= r && d > 3.4;
            } else if (ly < 82) {                                    // hollow shaft with slit windows
                solid = d <= 5.3 && d > 3.4;
                boolean slitRow = (ly - 36) % 13 < 3;
                boolean slit = Math.abs(dx) < 0.6 || Math.abs(dz) < 0.6;
                if (solid && slitRow && slit && d > 4.4) solid = false;
                if (ly % 13 == 8 && d <= 5.9 && d > 4.6) solid = true; // string course
            } else if (ly < 86) {                                    // upper ring
                solid = d <= 6.6 && d > 3.4;
            } else {                                                 // spire
                double r = 6.6 * (1 - (ly - 86) / 15.0);
                solid = d <= r && d > r - 1.7;
            }
            if (solid) chunk.setBlockState(mp.set(x, y, z), hash(x * 7 + y, z, 31) < 0.18 ? cr : br, false);
        }
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
            }
            c.top = (int) Math.floor(top);
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
        // water channels (river, pond, springs)
        for (CityPlan.Channel ch : bk.channels) {
            double d = CityPlan.segDist(px, pz, ch.ax(), ch.az(), ch.bx(), ch.bz(), tt);
            int L = ch.level();
            if (d > ch.hw()) {
                // banks: keep the channel walls up to the very rim (the lowered rim lip would otherwise leave water hanging)
                if (isl != null && d <= ch.hw() + (ch.depth() > 1 ? 3.5 : 1.5) && c.top < L + 1) { c.top = L + 1; c.bottom = Math.min(c.bottom, c.top - 3); }
                continue;
            }
            if (isl != null && c.top < L + 1) { c.top = L + 1; c.bottom = Math.min(c.bottom, c.top - 3); }
            boolean onLand = isl != null;
            if (onLand) {
                int depth = ch.depth() == 1 ? 1 : (d < ch.hw() - 1 ? ch.depth() : Math.max(1, ch.depth() - 1));
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
                if (!overLand && !(land(plan, px + 1, pz) || land(plan, px - 1, pz) || land(plan, px, pz + 1) || land(plan, px, pz - 1))) continue;
                c.fallTop = L;
                c.fallBottom = overLand ? c.top + 1 : plan.ocean + 1;
            }
        }
        return c;
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
                    chunk.setBlockState(mp.set(x, plan.ocean - 1, z), seabed, false);
                    chunk.setBlockState(mp.set(x, plan.ocean, z), water, false);
                }
                if (!ashen && c.isl == null && c.fallTop == Integer.MIN_VALUE && c.deckY == Integer.MIN_VALUE) debris(plan, chunk, mp, x, z);
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
                                default -> (c.isl.kind != 4 && vnoise(x * 0.11, z * 0.11, 21) > 0.74 || hash(x, z, 23) < 0.012) ? moss : grass;
                            };
                        } else if (y == c.top - 1) {
                            s = crust;
                        } else if (y <= c.bottom + 1 && hash(x, z, y) < 0.5 || hash(x, y, z) < 0.07) {
                            s = mossy;
                        } else {
                            s = stone;
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
                        } else if (h < 0.09) chunk.setBlockState(mp.set(x, c.top + 1, z), tall, false);
                        else if (h < 0.105) chunk.setBlockState(mp.set(x, c.top + 1, z), flowers, false);
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
            if (Math.abs(dx) > 4 || Math.abs(dz) > 4) continue;
            int base = t.baseY;
            if (base == Integer.MIN_VALUE) {
                Col c = column(plan, t.x, t.z);
                base = c.isl == null ? -999 : c.top + 1;
                t.baseY = base;
            }
            if (base < 0) continue;
            if (t.kind == 2) { deadTree(chunk, mp, t, base, x, z); continue; }
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
            ResourceLocation id = p.template().equals("laboratory") ? new ResourceLocation("riftworld", "laboratory")
                    : new ResourceLocation("riftworld", "city/" + p.template());
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
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
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
        arr[plan.ocean - 1] = ElisarBlocks.SEABED.get().defaultBlockState();
        arr[plan.ocean] = ElisarBlocks.WATER.get().defaultBlockState();
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
