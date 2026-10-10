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

    private WildIslands() {
    }

    static long key(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xffffffffL);
    }

    public static Cell cell(int cx, int cz) {
        return CACHE.computeIfAbsent(key(cx, cz), k -> gen(cx, cz));
    }

    /** Islands whose shape can reach (x, z): own cell + the 8 neighbours. */
    public static List<CityPlan.Island> near(double x, double z) {
        int cx = Math.floorDiv((int) Math.floor(x), CELL), cz = Math.floorDiv((int) Math.floor(z), CELL);
        if (Math.hypot(x, z) < CITY_CLEAR - 120) return Collections.emptyList();
        List<CityPlan.Island> out = new ArrayList<>(8);
        if (CastleSite.near(x, z, 2)) out.add(CastleSite.ISLAND);
        EmberRuins.Site es = EmberRuins.siteAt(x, z);
        if (es != null && Math.abs(x - es.cx) < es.island.R * 1.2 + 2 && Math.abs(z - es.cz) < es.island.R * 1.2 + 2) out.add(es.island);
        VioletHills.Region vr = VioletHills.regionAt(x, z);
        if (vr != null && Math.abs(x - vr.meadow.cx) < vr.meadow.R * 1.2 + 2 && Math.abs(z - vr.meadow.cz) < vr.meadow.R * 1.2 + 2) out.add(vr.meadow);
        SizoGrove.Region sr = SizoGrove.regionAt(x, z);
        if (sr != null && Math.abs(x - sr.hub.cx) < sr.hub.R * 1.2 + 2 && Math.abs(z - sr.hub.cz) < sr.hub.R * 1.2 + 2) out.add(sr.hub);
        WaterfallGardens.Region gr = WaterfallGardens.regionAt(x, z);
        if (gr != null && Math.abs(x - gr.hub.cx) < gr.hub.R * 1.2 + 2 && Math.abs(z - gr.hub.cz) < gr.hub.R * 1.2 + 2) out.add(gr.hub);
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) {
                for (CityPlan.Island i : cell(cx + dx, cz + dz).islands) {
                    if (Math.abs(x - i.cx) < i.R * 1.2 + 2 && Math.abs(z - i.cz) < i.R * 1.2 + 2) out.add(i);
                }
            }
        return out;
    }

    public static List<CityPlan.Tree> treesNear(int x, int z) {
        if (Math.hypot(x, z) < CITY_CLEAR - 120) return Collections.emptyList();
        int cx = Math.floorDiv(x, CELL), cz = Math.floorDiv(z, CELL);
        List<CityPlan.Tree> out = new ArrayList<>();
        if (CastleSite.near(x, z, 6))
            for (CityPlan.Tree t : CastleSite.TREES) if (Math.abs(x - t.x) <= 4 && Math.abs(z - t.z) <= 4) out.add(t);
        if (AshenLands.weight(x, z) > 0) {
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
        for (int k = 0; k < med; ++k) want.add(new double[] {2, 17 + r.nextDouble() * 13});
        int small = 2 + r.nextInt(3);
        for (int k = 0; k < small; ++k) want.add(new double[] {3, 7 + r.nextDouble() * 6});
        int islets = 3 + r.nextInt(5);
        for (int k = 0; k < islets; ++k) want.add(new double[] {4, 3 + r.nextDouble() * 3});
        for (double[] w : want) {
            int kind = (int) w[0];
            double R = w[1];
            for (int tries = 0; tries < 30; ++tries) {
                double m = R * 1.15 + 4;
                if (m * 2 >= CELL) break;
                double x = ox + m + r.nextDouble() * (CELL - 2 * m), z = oz + m + r.nextDouble() * (CELL - 2 * m);
                if (Math.hypot(x, z) - R * 1.15 < CITY_CLEAR) continue;
                if (Math.hypot(x - CastleSite.CX, z - CastleSite.CZ) < R * 1.15 + CastleSite.R * 1.15 + 14) continue;   // castle site
                EmberRuins.Site es = EmberRuins.siteAt(x, z);
                if (es != null && Math.hypot(x - es.cx, z - es.cz) < R * 1.15 + es.island.R * 1.15 + 10) continue;     // ruins island
                VioletHills.Region vr = VioletHills.regionAt(x, z);                                                 // violet meadow island
                if (vr != null && Math.hypot(x - vr.meadow.cx, z - vr.meadow.cz) < R * 1.15 + vr.meadow.R * 1.15 + 12) continue;
                SizoGrove.Region sr = SizoGrove.regionAt(x, z);                                                     // sizo grove hub
                if (sr != null && Math.hypot(x - sr.hub.cx, z - sr.hub.cz) < R * 1.15 + sr.hub.R * 1.15 + 12) continue;
                WaterfallGardens.Region gr = WaterfallGardens.regionAt(x, z);                                       // waterfall gardens hub
                if (gr != null && Math.hypot(x - gr.hub.cx, z - gr.hub.cz) < R * 1.15 + gr.hub.R * 1.15 + 12) continue;
                MistKeep.Region mk = MistKeep.regionAt(x, z);                                                          // туманный данж: вода между столбами остаётся морем
                if (mk != null && Math.hypot(x - mk.cx, z - mk.cz) < R * 1.15 + mk.Rr + 40) continue;
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
            int kind = r.nextDouble() < 0.55 ? 0 : 1;
            int h = kind == 0 ? 8 + r.nextInt(6) : 5 + r.nextInt(4);
            if (AshenLands.weight(x, z) > 0.3) {             // burnt: bare dead trees only
                kind = 2;
                h = 5 + r.nextInt(6);
            }
            c.trees.add(new CityPlan.Tree((int) Math.floor(x), (int) Math.floor(z), kind, h, r.nextInt(1 << 30)));
            ++k;
        }
    }
}
