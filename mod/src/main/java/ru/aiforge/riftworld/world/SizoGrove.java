package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Biome 05 «Роща Сизо» (Rev 4 item 8): sacred groves of GIANT layered-canopy sizo trees on one big hub island,
 * mossy boulders, rare amber-ringed way pillars, violet bloom drifts on silvery mint grass, and (in ~3 out of 5
 * regions) a lone RUINED pale-stone temple - the only structure, the grove keeps its silence otherwise (no paths,
 * no villages, no farms: TZ «без тропинок»).
 *
 * Layout mirrors VioletHills: 1040-block super-cells (offset half a cell against the violet grid), ~half of the
 * suitable ones hold one region (blob R 140..180) wrapped around a single big smooth hub island (kind 8). Both the
 * hub and the decorations are deterministic from one seed - no saved data.
 */
public final class SizoGrove {
    public static final int SUPER = 1040;
    private static final long FIXED = 0x51E0F00DL;       // region generator seed (kept stable across builds)

    // ---- the published constants (compile-time) -----------------------------------------------------------
    /** Giant sizo tree kind used by ElisarChunkGenerator.trees(). */
    public static final int TREE_GIANT = 3;
    /** Island kind of the grove hub. */
    public static final int KIND = 8;

    public static final class Boulder {
        public final int x, z;
        public final double r;        // horizontal radius (y squashed to 0.72*r)
        public final long seed;

        Boulder(int x, int z, double r, long seed) { this.x = x; this.z = z; this.r = r; this.seed = seed; }
    }

    public static final class Region {
        public final int cx, cz;
        public final double Rr;
        public final CityPlan.Island hub;
        public final List<CityPlan.Tree> trees = new ArrayList<>();
        public final List<Boulder> boulders = new ArrayList<>();
        public final List<int[]> pillars = new ArrayList<>();        // {x, z} amber-band way pillars
        // the rare ruined temple: cx/cz, flattened plateau y, rotation 0..3 (0 = +z front)
        public final boolean temple;
        public final int tx, tz, ty, trot;

        Region(int cx, int cz, Random rnd) {
            this.cx = cx; this.cz = cz;
            this.Rr = 140 + rnd.nextDouble() * 40;
            double R = 52 + rnd.nextDouble() * 20;
            double base = 100 + rnd.nextDouble() * 10;
            this.hub = new CityPlan.Island(cx, cz, R, base, R * 0.6 + 14, rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283,
                    rnd.nextDouble() * 6.283, KIND, 0.7);
            // giant sizos: 12..15, well spaced (canopies want ~9 blocks of elbow room)
            for (int k = 0, want = 12 + rnd.nextInt(4), tries = 0; k < want && tries < want * 22; ++tries) {
                double a = rnd.nextDouble() * Math.PI * 2, d = (0.18 + rnd.nextDouble() * 0.62) * R;
                int x = (int) Math.floor(cx + Math.cos(a) * d), z = (int) Math.floor(cz + Math.sin(a) * d);
                if (hub.t(x + 0.5, z + 0.5) < 0.32) continue;
                boolean ok = true;
                for (CityPlan.Tree t : trees) if (Math.abs(t.x - x) < 9 && Math.abs(t.z - z) < 9) { ok = false; break; }
                if (!ok) continue;
                trees.add(new CityPlan.Tree(x, z, TREE_GIANT, 13 + rnd.nextInt(5), rnd.nextInt(1 << 30)));
                ++k;
            }
            // mossy boulders between the trunks
            for (int k = 0, want = 8 + rnd.nextInt(5), tries = 0; k < want && tries < want * 14; ++tries) {
                double a = rnd.nextDouble() * Math.PI * 2, d = (0.15 + rnd.nextDouble() * 0.7) * R;
                int x = (int) Math.floor(cx + Math.cos(a) * d), z = (int) Math.floor(cz + Math.sin(a) * d);
                if (hub.t(x + 0.5, z + 0.5) < 0.34) continue;
                boolean ok = true;
                for (CityPlan.Tree t : trees) if (Math.abs(t.x - x) < 6 && Math.abs(t.z - z) < 6) { ok = false; break; }
                for (Boulder b : boulders) if (Math.hypot(b.x - x, b.z - z) < b.r + 7) { ok = false; break; }
                if (!ok) continue;
                boulders.add(new Boulder(x, z, 2.1 + rnd.nextDouble() * 1.5, rnd.nextLong()));
                ++k;
            }
            // amber-ringed way pillars: 0..3, always with sight of a boulder or a giant
            for (int k = 0, want = rnd.nextInt(4), tries = 0; k < want && tries < 40; ++tries) {
                double a = rnd.nextDouble() * Math.PI * 2, d = (0.25 + rnd.nextDouble() * 0.5) * R;
                int x = (int) Math.floor(cx + Math.cos(a) * d), z = (int) Math.floor(cz + Math.sin(a) * d);
                if (hub.t(x + 0.5, z + 0.5) < 0.3) continue;
                pillars.add(new int[] {x, z});
                ++k;
            }
            // the rare temple: a quiet terrace off-centre, rotated at will
            if (rnd.nextDouble() < 0.62) {
                this.temple = true;
                double a = rnd.nextDouble() * Math.PI * 2, d = (0.3 + rnd.nextDouble() * 0.25) * R;
                int x = (int) Math.floor(cx + Math.cos(a) * d), z = (int) Math.floor(cz + Math.sin(a) * d);
                this.tx = x; this.tz = z;
                this.ty = (int) Math.floor(hub.raw(x + 0.5, z + 0.5));
                this.trot = rnd.nextInt(4);
                // keep giants and boulders away from the terrace
                trees.removeIf(t -> Math.abs(t.x - tx) < 13 && Math.abs(t.z - tz) < 13);
                boulders.removeIf(b -> Math.abs(b.x - tx) < 14 && Math.abs(b.z - tz) < 14);
            } else {
                this.temple = false;
                this.tx = this.tz = this.ty = this.trot = 0;
            }
        }
    }

    private static final ConcurrentHashMap<Long, Optional<Region>> CACHE = new ConcurrentHashMap<>();

    private SizoGrove() {
    }

    public static Region region(int sx, int sz) {
        return CACHE.computeIfAbsent(WildIslands.key(sx, sz), k -> Optional.ofNullable(gen(sx, sz))).orElse(null);
    }

    /** The region of one super-cell, or null. The grid is shifted half a cell so it never aligns with VioletHills. */
    private static Region gen(int sx, int sz) {
        Random r = new Random(FIXED ^ (sx * 53982894593057L + sz * 317074912223L) * 0x9E3779B97F4A7C15L);
        if (r.nextDouble() > 0.5) return null;
        int cx = sx * SUPER + 520 + 160 + r.nextInt(SUPER - 840), cz = sz * SUPER + 520 + 160 + r.nextInt(SUPER - 840);
        if (Math.hypot(cx, cz) < 600) return null;                                                        // city ring
        if (Math.hypot(cx - AshenLands.CX, cz - AshenLands.CZ) < AshenLands.R + 300) return null;         // real Ashen Lands
        EmberRuins.Site es = EmberRuins.nearest(cx, cz);
        if (es != null && Math.hypot(cx - es.cx, cz - es.cz) < 460) return null;                          // keep ruins regions apart
        // never on top of a violet meadow region (their cell = 880)
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) {
                VioletHills.Region vr = VioletHills.region(Math.floorDiv(cx, VioletHills.SUPER) + dx, Math.floorDiv(cz, VioletHills.SUPER) + dz);
                if (vr != null && Math.hypot(cx - vr.cx, cz - vr.cz) < vr.Rr + 240) return null;
            }
        return new Region(cx, cz, r);
    }

    /** The region whose influence can reach (x, z), or null. */
    public static Region regionAt(double x, double z) {
        int sx = Math.floorDiv((int) Math.floor(x), SUPER), sz = Math.floorDiv((int) Math.floor(z), SUPER);
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) {
                Region rg = region(sx + dx, sz + dz);
                if (rg != null && Math.abs(x - rg.cx) < rg.Rr + 80 && Math.abs(z - rg.cz) < rg.Rr + 80) return rg;
            }
        return null;
    }

    /** Biome blob test (wobbly edge): hub island + surrounding islets and a stretch of sea. */
    public static boolean inside(double x, double z) {
        Region rg = regionAt(x, z);
        if (rg == null) return false;
        double d = Math.hypot(x - rg.cx, z - rg.cz) + (ElisarChunkGenerator.vnoise(x * 0.026, z * 0.026, 155) - 0.5) * 48;
        return d < rg.Rr;
    }

    public static List<CityPlan.Tree> treesNear(int x, int z) {
        Region rg = regionAt(x + 0.5, z + 0.5);
        if (rg == null) return List.of();
        List<CityPlan.Tree> out = new ArrayList<>(2);
        for (CityPlan.Tree t : rg.trees) if (Math.abs(x - t.x) <= 7 && Math.abs(z - t.z) <= 7) out.add(t);
        return out;
    }

    /** Boulders whose 3D reach can touch cell (x, z) (chunk callers pad by their own margin). */
    public static List<Boulder> bouldersNear(int x, int z) {
        Region rg = regionAt(x + 0.5, z + 0.5);
        if (rg == null) return List.of();
        List<Boulder> out = new ArrayList<>(2);
        for (Boulder b : rg.boulders) if (Math.abs(x - b.x) < b.r + 8 && Math.abs(z - b.z) < b.r + 8) out.add(b);
        return out;
    }

    /** Way pillars that could stand in/around chunk square [x0, x0+15] x [z0, z0+15]. */
    public static List<int[]> pillarsIn(Region rg, int x0, int z0) {
        List<int[]> out = new ArrayList<>(2);
        for (int[] p : rg.pillars)
            if (p[0] >= x0 - 2 && p[0] <= x0 + 17 && p[1] >= z0 - 2 && p[1] <= z0 + 17) out.add(p);
        return out;
    }

    public static Region templeIn(int x0, int z0) {
        Region rg = regionAt(x0 + 8.5, z0 + 8.5);
        if (rg == null || !rg.temple) return null;
        return Math.abs(rg.tx - x0) <= 40 && Math.abs(rg.tz - z0) <= 40 ? rg : null;
    }
}
