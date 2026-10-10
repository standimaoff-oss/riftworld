package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Biome 07 «Висячие сады» (Rev 4 item 8): a SMALL paradise stack - one limestone-stepped mound whose lowest shelf hugs
 * the ocean and whose top reaches the wild-island cruising height. Shelf edges break into waterfalls (terraced pools +
 * springs + hot springs with travertine rims and warm lantern cores), nearly-jungle vegetation, TINY trees only, and
 * the single man-made thing: one rare sagging plank bridge across the gorge (TZ: «из не-природы только редкие мосты»).
 *
 * Own grid 960 (mid-shifted against the others), ~42 % of fitting cells; deterministic from one seed.
 */
public final class WaterfallGardens {
    public static final int SUPER = 960;
    private static final long FIXED = 0x7A7EFA11L;
    /** Terrace step height (blocks). */
    public static final double STEP = 5.0;
    public static final int KIND = 9;

    public static final class Region {
        public final int cx, cz;
        public final double Rr;
        public final CityPlan.Island hub;
        public final List<CityPlan.Tree> trees = new ArrayList<>();
        public final List<CityPlan.Bridge> bridges = new ArrayList<>();

        Region(int cx, int cz, Random rnd) {
            this.cx = cx; this.cz = cz;
            double R = 30 + rnd.nextDouble() * 14;                        // «биом небольшой»
            this.Rr = R * 2.1;
            double base = 96 + rnd.nextDouble() * 16;                     // верх - на высоте островов
            this.hub = new CityPlan.Island(cx, cz, R, base, R * 1.15 + 10, rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283,
                    rnd.nextDouble() * 6.283, KIND, 1.6);
            // rare bridges: one sagging span over a mid-terrace dip (tries to keep both feet on solid shelves)
            if (rnd.nextDouble() < 0.6) {
                for (int t = 0; t < 8; ++t) {
                    double a = rnd.nextDouble() * Math.PI * 2;
                    double mx = cx + (rnd.nextDouble() - 0.5) * R * 0.5, mz = cz + (rnd.nextDouble() - 0.5) * R * 0.5;
                    double hl = (0.28 + rnd.nextDouble() * 0.2) * R;
                    double ax = mx - Math.cos(a) * hl, az = mz - Math.sin(a) * hl;
                    double bx = mx + Math.cos(a) * hl, bz = mz + Math.sin(a) * hl;
                    if (hub.t(ax + 0.5, az + 0.5) < 0.3 || hub.t(bx + 0.5, bz + 0.5) < 0.3) continue;
                    int ya = ElisarChunkGenerator.gardenTerraceTop(hub, ax + 0.5, az + 0.5, CityPlan.get().ocean) + 1;
                    int yb = ElisarChunkGenerator.gardenTerraceTop(hub, bx + 0.5, bz + 0.5, CityPlan.get().ocean) + 1;
                    if (Math.abs(ya - yb) > 3) continue;
                    bridges.add(new CityPlan.Bridge(ax, az, bx, bz, ya, yb, 1.5 + rnd.nextDouble() * 0.8, 0.9));
                    break;
                }
            }
            // tiny trees only (TZ): mostly round ambra puffballs, sparse slim sizo - elbow room 5
            for (int k = 0, want = 18 + rnd.nextInt(9), tries = 0; k < want && tries < want * 20; ++tries) {
                double a = rnd.nextDouble() * Math.PI * 2, d = (0.15 + rnd.nextDouble() * 0.7) * R;
                int x = (int) Math.floor(cx + Math.cos(a) * d), z = (int) Math.floor(cz + Math.sin(a) * d);
                if (hub.t(x + 0.5, z + 0.5) < 0.34) continue;
                boolean ok = true;
                for (CityPlan.Tree t : trees) if (Math.abs(t.x - x) < 5 && Math.abs(t.z - z) < 5) { ok = false; break; }
                for (CityPlan.Bridge b : bridges)
                    if (CityPlan.segDist(x + 0.5, z + 0.5, b.ax(), b.az(), b.bx(), b.bz(), null) < 3.0) { ok = false; break; }
                if (!ok) continue;
                int kind = rnd.nextDouble() < 0.6 ? 1 : 0;
                trees.add(new CityPlan.Tree(x, z, kind, 4 + rnd.nextInt(3), rnd.nextInt(1 << 30)));
                ++k;
            }
        }
    }

    private static final ConcurrentHashMap<Long, Optional<Region>> CACHE = new ConcurrentHashMap<>();

    private WaterfallGardens() {
    }

    public static Region region(int sx, int sz) {
        return CACHE.computeIfAbsent(WildIslands.key(sx, sz), k -> Optional.ofNullable(gen(sx, sz))).orElse(null);
    }

    private static Region gen(int sx, int sz) {
        Random r = new Random(FIXED ^ (sx * 53982894593057L + sz * 317074912223L) * 0x9E3779B97F4A7C15L);
        if (r.nextDouble() > 0.42) return null;
        int cx = sx * SUPER + 480 + 120 + r.nextInt(SUPER - 720), cz = sz * SUPER + 480 + 120 + r.nextInt(SUPER - 720);
        if (Math.hypot(cx, cz) < 560) return null;
        if (Math.hypot(cx - AshenLands.CX, cz - AshenLands.CZ) < AshenLands.R + 260) return null;
        EmberRuins.Site es = EmberRuins.nearest(cx, cz);
        if (es != null && Math.hypot(cx - es.cx, cz - es.cz) < 420) return null;
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) {
                SizoGrove.Region sr = SizoGrove.region(Math.floorDiv(cx, SizoGrove.SUPER) + dx, Math.floorDiv(cz, SizoGrove.SUPER) + dz);
                if (sr != null && Math.hypot(cx - sr.cx, cz - sr.cz) < sr.Rr + 200) return null;
                VioletHills.Region vr = VioletHills.region(Math.floorDiv(cx, VioletHills.SUPER) + dx, Math.floorDiv(cz, VioletHills.SUPER) + dz);
                if (vr != null && Math.hypot(cx - vr.cx, cz - vr.cz) < vr.Rr + 200) return null;
            }
        return new Region(cx, cz, r);
    }

    public static Region regionAt(double x, double z) {
        int sx = Math.floorDiv((int) Math.floor(x), SUPER), sz = Math.floorDiv((int) Math.floor(z), SUPER);
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) {
                Region rg = region(sx + dx, sz + dz);
                if (rg != null && Math.abs(x - rg.cx) < rg.Rr + 70 && Math.abs(z - rg.cz) < rg.Rr + 70) return rg;
            }
        return null;
    }

    public static boolean inside(double x, double z) {
        Region rg = regionAt(x, z);
        if (rg == null) return false;
        double d = Math.hypot(x - rg.cx, z - rg.cz) + (ElisarChunkGenerator.vnoise(x * 0.03, z * 0.03, 255) - 0.5) * 40;
        return d < rg.Rr;
    }

    public static List<CityPlan.Tree> treesNear(int x, int z) {
        Region rg = regionAt(x + 0.5, z + 0.5);
        if (rg == null) return List.of();
        List<CityPlan.Tree> out = new ArrayList<>(2);
        for (CityPlan.Tree t : rg.trees) if (Math.abs(x - t.x) <= 5 && Math.abs(z - t.z) <= 5) out.add(t);
        return out;
    }
}
