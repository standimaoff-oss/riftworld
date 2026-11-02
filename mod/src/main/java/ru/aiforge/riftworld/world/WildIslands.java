package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The main Elisar biome around the city: an endless archipelago of floating islands of four kinds, generated per 160-block cell
 * from a fixed seed (deterministic, no plan file).
 *
 *   1 large  (R 40..62)   wide rolling meadow islands with knolls and groves
 *   2 medium (R 17..30)   grassy islands with a few trees
 *   3 small  (R 7..13)    rocks with a tuft of grass, sometimes one tree
 *   4 islet  (R 3..6)     bowl-shaped bleached islets with a deep pointed underside (as in the reference)
 *
 * Nothing is generated within the city ring (the planned islands of plan.json live there).
 */
public final class WildIslands {
    public static final int CELL = 160;
    private static final double CITY_CLEAR = 300;
    private static final long SEED = 0x51A1E5L;

    public static final class Cell {
        public final List<CityPlan.Island> islands = new ArrayList<>(8);
        public final List<CityPlan.Tree> trees = new ArrayList<>(32);
    }

    private static final ConcurrentHashMap<Long, Cell> CACHE = new ConcurrentHashMap<>();
    private static final int TILE = 64;
    private static final CityPlan.Island[] EMPTY_ISLANDS = new CityPlan.Island[0];
    private static final ConcurrentHashMap<Long, CityPlan.Island[]> TILE_CACHE = new ConcurrentHashMap<>(512);
    private static final ThreadLocal<Boolean> BUILDING_TILE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private WildIslands() {
    }

    static long key(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xffffffffL);
    }

    public static Cell cell(int cx, int cz) {
        Cell c = CACHE.get(key(cx, cz));
        if (c == null) {
            c = gen(cx, cz);
            CACHE.putIfAbsent(key(cx, cz), c);
        }
        return c;
    }

    /** Возвращает кэшированный массив всех диких/региональных островов, способных затронуть тайл 64x64 вокруг (x, z). */
    public static CityPlan.Island[] tileCandidates(double x, double z) {
        if (x * x + z * z < (CITY_CLEAR - 120.0) * (CITY_CLEAR - 120.0)) return EMPTY_ISLANDS;
        int tx = Math.floorDiv((int) Math.floor(x), TILE);
        int tz = Math.floorDiv((int) Math.floor(z), TILE);
        long tk = key(tx, tz);
        CityPlan.Island[] cached = TILE_CACHE.get(tk);
        if (cached != null) return cached;
        if (Boolean.TRUE.equals(BUILDING_TILE.get())) {
            return scanDirect(x, z);
        }
        BUILDING_TILE.set(Boolean.TRUE);
        try {
            CityPlan.Island[] built = buildTile(tx, tz);
            if (TILE_CACHE.size() > 8192) {
                TILE_CACHE.clear();
            }
            TILE_CACHE.put(tk, built);
            return built;
        } finally {
            BUILDING_TILE.set(Boolean.FALSE);
        }
    }

    public static CityPlan.Island islandAt(double x, double z) {
        if (x * x + z * z < (CITY_CLEAR - 120.0) * (CITY_CLEAR - 120.0)) return null;
        CityPlan.Island[] cand = tileCandidates(x, z);
        for (int k = 0, n = cand.length; k < n; ++k) {
            CityPlan.Island isl = cand[k];
            if (isl.contains(x, z)) return isl;
        }
        return null;
    }

    private static void addTileCandidate(List<CityPlan.Island> list, CityPlan.Island isl,
                                         double mx, double mz) {
        if (isl == null) return;
        double lim = (TILE * 0.5) + isl.maxReach() * 1.65 + 12.0;
        if (Math.abs(mx - isl.cx) <= lim && Math.abs(mz - isl.cz) <= lim) {
            for (int i = 0, n = list.size(); i < n; ++i) {
                if (list.get(i) == isl) return;
            }
            list.add(isl);
        }
    }

    private static CityPlan.Island[] buildTile(int tx, int tz) {
        int x0 = tx * TILE, x1 = x0 + TILE - 1;
        int z0 = tz * TILE, z1 = z0 + TILE - 1;
        double mx = x0 + TILE * 0.5;
        double mz = z0 + TILE * 0.5;
        List<CityPlan.Island> out = new ArrayList<>(8);

        for (int sx = Math.floorDiv(x0, AshenLands.SUPER) - 1; sx <= Math.floorDiv(x1, AshenLands.SUPER) + 1; ++sx)
            for (int sz = Math.floorDiv(z0, AshenLands.SUPER) - 1; sz <= Math.floorDiv(z1, AshenLands.SUPER) + 1; ++sz) {
                AshenLands.Region ar = AshenLands.region(sx, sz);
                if (ar != null && ar.island != null) addTileCandidate(out, ar.island, mx, mz);
            }
        for (int sx = Math.floorDiv(x0, EmberRuins.SUPER) - 1; sx <= Math.floorDiv(x1, EmberRuins.SUPER) + 1; ++sx)
            for (int sz = Math.floorDiv(z0, EmberRuins.SUPER) - 1; sz <= Math.floorDiv(z1, EmberRuins.SUPER) + 1; ++sz) {
                EmberRuins.Site es = EmberRuins.site(sx, sz);
                if (es != null) addTileCandidate(out, es.island, mx, mz);
            }
        for (int sx = Math.floorDiv(x0, VioletHills.SUPER) - 1; sx <= Math.floorDiv(x1, VioletHills.SUPER) + 1; ++sx)
            for (int sz = Math.floorDiv(z0, VioletHills.SUPER) - 1; sz <= Math.floorDiv(z1, VioletHills.SUPER) + 1; ++sz) {
                VioletHills.Region vr = VioletHills.region(sx, sz);
                if (vr != null) for (CityPlan.Island vi : vr.isles) addTileCandidate(out, vi, mx, mz);
            }
        for (int sx = Math.floorDiv(x0, CrystalRifts.SUPER) - 1; sx <= Math.floorDiv(x1, CrystalRifts.SUPER) + 1; ++sx)
            for (int sz = Math.floorDiv(z0, CrystalRifts.SUPER) - 1; sz <= Math.floorDiv(z1, CrystalRifts.SUPER) + 1; ++sz) {
                CrystalRifts.Region cr = CrystalRifts.region(sx, sz);
                if (cr != null) for (CityPlan.Island ci : cr.isles) addTileCandidate(out, ci, mx, mz);
            }
        for (int sx = Math.floorDiv(x0, SizoGrove.SUPER) - 1; sx <= Math.floorDiv(x1, SizoGrove.SUPER) + 1; ++sx)
            for (int sz = Math.floorDiv(z0, SizoGrove.SUPER) - 1; sz <= Math.floorDiv(z1, SizoGrove.SUPER) + 1; ++sz) {
                SizoGrove.Region sr = SizoGrove.region(sx, sz);
                if (sr != null) addTileCandidate(out, sr.hub, mx, mz);
            }
        for (int sx = Math.floorDiv(x0, WaterfallGardens.SUPER) - 1; sx <= Math.floorDiv(x1, WaterfallGardens.SUPER) + 1; ++sx)
            for (int sz = Math.floorDiv(z0, WaterfallGardens.SUPER) - 1; sz <= Math.floorDiv(z1, WaterfallGardens.SUPER) + 1; ++sz) {
                WaterfallGardens.Region gr = WaterfallGardens.region(sx, sz);
                if (gr != null) for (CityPlan.Island gi : gr.isles) addTileCandidate(out, gi, mx, mz);
            }
        for (int sx = Math.floorDiv(x0, WorldStructures.SUPER_VILLAGE) - 1; sx <= Math.floorDiv(x1, WorldStructures.SUPER_VILLAGE) + 1; ++sx)
            for (int sz = Math.floorDiv(z0, WorldStructures.SUPER_VILLAGE) - 1; sz <= Math.floorDiv(z1, WorldStructures.SUPER_VILLAGE) + 1; ++sz) {
                WorldStructures.Village wv = WorldStructures.whiteVillage(sx, sz);
                if (wv != null) addTileCandidate(out, wv.island, mx, mz);
            }
        for (int sx = Math.floorDiv(x0, WorldStructures.SUPER_BURNED) - 1; sx <= Math.floorDiv(x1, WorldStructures.SUPER_BURNED) + 1; ++sx)
            for (int sz = Math.floorDiv(z0, WorldStructures.SUPER_BURNED) - 1; sz <= Math.floorDiv(z1, WorldStructures.SUPER_BURNED) + 1; ++sz) {
                WorldStructures.Village bv = WorldStructures.burnedIslandVillage(sx, sz);
                if (bv != null) addTileCandidate(out, bv.island, mx, mz);
            }
        for (int sx = Math.floorDiv(x0, LandmarkStructures.SUPER_THEATER) - 1; sx <= Math.floorDiv(x1, LandmarkStructures.SUPER_THEATER) + 1; ++sx)
            for (int sz = Math.floorDiv(z0, LandmarkStructures.SUPER_THEATER) - 1; sz <= Math.floorDiv(z1, LandmarkStructures.SUPER_THEATER) + 1; ++sz) {
                LandmarkStructures.GrandTheater gt = LandmarkStructures.grandTheater(sx, sz);
                if (gt != null) addTileCandidate(out, gt.island, mx, mz);
            }
        for (int sx = Math.floorDiv(x0, LandmarkStructures.SUPER_HARBOR) - 1; sx <= Math.floorDiv(x1, LandmarkStructures.SUPER_HARBOR) + 1; ++sx)
            for (int sz = Math.floorDiv(z0, LandmarkStructures.SUPER_HARBOR) - 1; sz <= Math.floorDiv(z1, LandmarkStructures.SUPER_HARBOR) + 1; ++sz) {
                LandmarkStructures.HarborPort hp = LandmarkStructures.harborPort(sx, sz);
                if (hp != null) addTileCandidate(out, hp.island, mx, mz);
            }
        for (int cx = Math.floorDiv(x0, CELL) - 1; cx <= Math.floorDiv(x1, CELL) + 1; ++cx)
            for (int cz = Math.floorDiv(z0, CELL) - 1; cz <= Math.floorDiv(z1, CELL) + 1; ++cz) {
                for (CityPlan.Island wi : cell(cx, cz).islands) addTileCandidate(out, wi, mx, mz);
            }
        return out.isEmpty() ? EMPTY_ISLANDS : out.toArray(EMPTY_ISLANDS);
    }

    private static CityPlan.Island[] scanDirect(double x, double z) {
        List<CityPlan.Island> list = scanNearUncached(x, z);
        return list.isEmpty() ? EMPTY_ISLANDS : list.toArray(EMPTY_ISLANDS);
    }

    /** Islands whose shape can reach (x, z): own cell + the 8 neighbours. */
    public static List<CityPlan.Island> near(double x, double z) {
        CityPlan.Island[] cand = tileCandidates(x, z);
        if (cand.length == 0) return Collections.emptyList();
        List<CityPlan.Island> out = new ArrayList<>(cand.length);
        for (CityPlan.Island isl : cand) {
            double r = isl.maxReach();
            if (Math.abs(x - isl.cx) < r && Math.abs(z - isl.cz) < r) {
                out.add(isl);
            }
        }
        return out;
    }

    private static List<CityPlan.Island> scanNearUncached(double x, double z) {
        int cx = Math.floorDiv((int) Math.floor(x), CELL), cz = Math.floorDiv((int) Math.floor(z), CELL);
        if (Math.hypot(x, z) < CITY_CLEAR - 120) return Collections.emptyList();
        List<CityPlan.Island> out = new ArrayList<>(8);
        CityPlan.Island castleIsl = CastleSite.islandNear(x, z, 4);
        if (castleIsl != null) out.add(castleIsl);
        EmberRuins.Site es = EmberRuins.siteAt(x, z);
        if (es != null && Math.abs(x - es.cx) < es.island.maxReach() && Math.abs(z - es.cz) < es.island.maxReach()) out.add(es.island);
        VioletHills.Region vr = VioletHills.regionAt(x, z);
        if (vr != null)
            for (CityPlan.Island vi : vr.isles)
                if (Math.abs(x - vi.cx) < vi.maxReach() && Math.abs(z - vi.cz) < vi.maxReach()) out.add(vi);
        CrystalRifts.Region cr = CrystalRifts.regionAt(x, z);
        if (cr != null)
            for (CityPlan.Island ci : cr.isles)
                if (Math.abs(x - ci.cx) < ci.maxReach() && Math.abs(z - ci.cz) < ci.maxReach()) out.add(ci);
        SizoGrove.Region sr = SizoGrove.regionAt(x, z);
        if (sr != null && Math.abs(x - sr.hub.cx) < sr.hub.maxReach() && Math.abs(z - sr.hub.cz) < sr.hub.maxReach()) out.add(sr.hub);
        // Рев 14.26: у садов теперь архипелаг из 3–6 островов — в выборку попадает каждый,
        // чья форма может дотянуться до точки (раньше добавлялся только один hub).
        WaterfallGardens.Region gr = WaterfallGardens.regionAt(x, z);
        if (gr != null)
            for (CityPlan.Island gi : gr.isles)
                if (Math.abs(x - gi.cx) < gi.maxReach() && Math.abs(z - gi.cz) < gi.maxReach()) out.add(gi);
        out.addAll(WorldStructures.islandsNear(x, z));
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) {
                for (CityPlan.Island i : cell(cx + dx, cz + dz).islands) {
                    if (Math.abs(x - i.cx) < i.maxReach() && Math.abs(z - i.cz) < i.maxReach()) out.add(i);
                }
            }
        return out;
    }

    public static List<CityPlan.Tree> treesNear(int x, int z) {
        if (Math.hypot(x, z) < CITY_CLEAR - 120) return Collections.emptyList();
        int cx = Math.floorDiv(x, CELL), cz = Math.floorDiv(z, CELL);
        List<CityPlan.Tree> out = new ArrayList<>();
        if (AshenLands.weight(x, z) > 0 || CastleSite.near(x, z, 6)) {
            out.addAll(AshenLands.treesNear(x, z));
            out.addAll(EmberRuins.treesNear(x, z));
        }
        out.addAll(VioletHills.treesNear(x, z));
        out.addAll(SizoGrove.treesNear(x, z));
        out.addAll(WaterfallGardens.treesNear(x, z));
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz)
                for (CityPlan.Tree t : cell(cx + dx, cz + dz).trees)
                    if (Math.abs(x - t.x) <= 4 && Math.abs(z - t.z) <= 4) out.add(t);
        return out;
    }

    private static Cell gen(int cx, int cz) {
        Cell c = new Cell();
        Random r = new Random(SEED ^ (cx * 341873128712L + cz * 132897987541L));
        double ox = cx * CELL, oz = cz * CELL;
        boolean large = r.nextDouble() < 0.5;
        List<double[]> want = new ArrayList<>();          // {kind, R}
        if (large) want.add(new double[] {1, 40 + r.nextDouble() * 22});
        int med = large ? r.nextInt(2) : 1 + r.nextInt(2);
        for (int k = 0; k < med; ++k) want.add(new double[] {2, 20 + r.nextDouble() * 16});
        int small = 1 + r.nextInt(2);
        for (int k = 0; k < small; ++k) want.add(new double[] {3, 7 + r.nextDouble() * 6});
        int islets = 2 + r.nextInt(3);
        for (int k = 0; k < islets; ++k) want.add(new double[] {4, 3 + r.nextDouble() * 3});
        for (double[] w : want) {
            int kind = (int) w[0];
            double R = w[1];
            for (int tries = 0; tries < 30; ++tries) {
                double m = R * 1.15 + 4;
                if (m * 2 >= CELL) break;
                double x = ox + m + r.nextDouble() * (CELL - 2 * m), z = oz + m + r.nextDouble() * (CELL - 2 * m);
                if (Math.hypot(x, z) - R * 1.15 < CITY_CLEAR) continue;
                if (AshenLands.blocks(x, z, R * 1.15 + 28)) continue;                                               // any AshenLands castle region
                EmberRuins.Site es = EmberRuins.siteAt(x, z);
                if (es != null && Math.hypot(x - es.cx, z - es.cz) < R * 1.15 + es.island.maxReach() + 10) continue; // ruins island
                if (VioletHills.nearAny(x, z, R * 1.15, 36)) continue;                                              // violet hills archipelago + chasms
                if (CrystalRifts.nearAny(x, z, R * 1.15, 36)) continue;                                             // crystal rifts archipelago + stone bridges
                SizoGrove.Region sr = SizoGrove.regionAt(x, z);                                                     // sizo grove hub
                if (sr != null && Math.hypot(x - sr.hub.cx, z - sr.hub.cz) < R * 1.15 + sr.hub.maxReach() + 16) continue;
                if (WaterfallGardens.nearAny(x, z, R * 1.15, 76)) continue;                                         // waterfall gardens archipelago + turquoise lagoon
                MistKeep.Region mk = MistKeep.regionAt(x, z);                                                          // туманный данж: вода между столбами остаётся морем
                if (mk != null && Math.hypot(x - mk.cx, z - mk.cz) < R * 1.15 + mk.Rr + 40) continue;
                if (WorldStructures.anyVillageAt(x, z, R * 1.15 + 68) != null) continue;                              // белые и сгоревшие деревни на собственных островах
                if (LandmarkStructures.theaterAt(x, z, R * 1.15 + 68) != null) continue;                              // античный театр на собственном острове
                boolean ok = true;
                for (CityPlan.Island o : c.islands) {
                    if (Math.hypot(x - o.cx, z - o.cz) < (R + o.R) * 1.15 + 6) { ok = false; break; }
                }
                if (!ok) continue;
                double base, depth, hill;
                switch (kind) {
                    case 1 -> { base = 96 + r.nextDouble() * 26; depth = R * 0.55 + 12; hill = 2.0; }
                    case 2 -> { base = 90 + r.nextDouble() * 42; depth = R * 0.65 + 8; hill = 1.3; }
                    case 3 -> { base = 86 + r.nextDouble() * 56; depth = R * 0.95 + 5; hill = 0.6; }
                    default -> { base = 95 + r.nextDouble() * 55; depth = R * 1.7 + 3; hill = 0.2; }
                }
                CityPlan.Island isl = new CityPlan.Island(x, z, R, base, depth, r.nextDouble() * 6.283, r.nextDouble() * 6.283, r.nextDouble() * 6.283, kind, hill);
                c.islands.add(isl);
                trees(c, isl, r);
                break;
            }
        }
        return c;
    }

    private static void trees(Cell c, CityPlan.Island isl, Random r) {
        int n = switch (isl.kind) {
            case 1 -> (int) (isl.R * isl.R / 90);
            case 2 -> (int) (isl.R * isl.R / 110);
            case 3 -> r.nextDouble() < 0.5 ? 1 : 0;
            default -> 0;
        };
        // groves: trees cluster around a few seeds instead of an even sprinkle
        double gx = isl.cx + (r.nextDouble() - 0.5) * isl.R, gz = isl.cz + (r.nextDouble() - 0.5) * isl.R;
        for (int k = 0, tries = 0; k < n && tries < n * 8; ++tries) {
            double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * isl.R * (r.nextDouble() < 0.6 ? 0.35 : 0.85);
            double x = (r.nextDouble() < 0.6 ? gx : isl.cx) + Math.cos(a) * d, z = (r.nextDouble() < 0.6 ? gz : isl.cz) + Math.sin(a) * d;
            if (isl.t(x, z) < 0.28) continue;
            boolean ok = true;
            for (CityPlan.Tree t : c.trees) if (Math.abs(t.x - x) < 4 && Math.abs(t.z - z) < 4) { ok = false; break; }
            if (!ok) continue;
            int kind = 1; // только янтарник (сизоствол растёт исключительно в Роще Сизо)
            int h = 5 + r.nextInt(4);
            if (AshenLands.weight(x, z) > 0.3) {             // burnt: bare dead trees only
                kind = 2;
                h = 5 + r.nextInt(6);
            }
            c.trees.add(new CityPlan.Tree((int) Math.floor(x), (int) Math.floor(z), kind, h, r.nextInt(1 << 30)));
            ++k;
        }
    }
}
