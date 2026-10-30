package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Biome 05 «Роща Сизо» (референс 05_biome_sizo_grove.png):
 * высокие прямые стволы гигантских сизостволов с плоскими многоярусными шапками шалфейной листвы,
 * замшелые скальные выступы, васильково-фиолетовые лесные цветы в серебристо-мятной траве и
 * старинная мощёная булыжником и охристой землёй лесная тропа, проходящая между парными каменными
 * столбами с янтарными фонарями мимо деревянной тележки и лесной часовенки к замшелой руине башни.
 */
public final class SizoGrove {
    public static final int SUPER = 1040;
    private static final long FIXED = 0x51E0F00DL;

    public static final int TREE_GIANT = 3;
    public static final int KIND = 8;

    public static final class Cart {
        public final int x, z, rot;
        public final long seed;

        Cart(int x, int z, int rot, long seed) { this.x = x; this.z = z; this.rot = rot; this.seed = seed; }
    }

    public static final class Shrine {
        public final int x, z, rot;
        public final long seed;

        Shrine(int x, int z, int rot, long seed) { this.x = x; this.z = z; this.rot = rot; this.seed = seed; }
    }

    public static final class PillarPair {
        public final int ax, az, bx, bz;

        PillarPair(int ax, int az, int bx, int bz) { this.ax = ax; this.az = az; this.bx = bx; this.bz = bz; }
    }

    public static final class Boulder {
        public final int x, z;
        public final double r;
        public final long seed;

        Boulder(int x, int z, double r, long seed) { this.x = x; this.z = z; this.r = r; this.seed = seed; }
    }

    public static final class Region {
        public final int cx, cz;
        public final double Rr;
        public final CityPlan.Island hub;
        // извилистая лесная тропа (булыжник + замшелый камень + тёплая охра, как на 05_biome_sizo_grove.png)
        public final double tax, taz, tbx, tbz, trailPhase;
        public static final double TRAIL_HW = 2.05;

        public final List<CityPlan.Tree> trees = new ArrayList<>();
        public final List<Boulder> boulders = new ArrayList<>();
        public final List<int[]> pillars = new ArrayList<>();
        public final List<Cart> carts = new ArrayList<>();
        public final List<Shrine> shrines = new ArrayList<>();
        public final List<PillarPair> pillarPairs = new ArrayList<>();
        public final boolean temple;
        public final int tx, tz, ty, trot;

        Region(int cx, int cz, Random rnd) {
            this.cx = cx; this.cz = cz;
            this.Rr = 148 + rnd.nextDouble() * 38;
            double R = 84 + rnd.nextDouble() * 24;
            double base = 98 + rnd.nextDouble() * 8;
            this.hub = new CityPlan.Island(cx, cz, R, base, R * 0.6 + 14, rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283,
                    rnd.nextDouble() * 6.283, KIND, 0.7);
            this.hub.elong = 0.16 + rnd.nextDouble() * 0.16;
            this.hub.elongAng = rnd.nextDouble() * Math.PI;

            double phi = this.hub.elongAng + (rnd.nextDouble() - 0.5) * 0.30;
            double tl = R * 0.72;
            this.tax = cx - Math.cos(phi) * tl;
            this.taz = cz - Math.sin(phi) * tl;
            this.tbx = cx + Math.cos(phi) * tl;
            this.tbz = cz + Math.sin(phi) * tl;
            this.trailPhase = rnd.nextDouble() * Math.PI * 2.0;
            int rotAlong = (Math.abs(Math.cos(phi)) > Math.abs(Math.sin(phi)))
                    ? (Math.cos(phi) >= 0 ? 0 : 2)
                    : (Math.sin(phi) >= 0 ? 1 : 3);

            // Разрушенная каменная башня во мху в конце тропы (как в глубине кадра 05_biome_sizo_grove.png)
            if (rnd.nextDouble() < 0.42) {
                this.temple = true;
                double[] tp = trailPoint(0.78);
                this.tx = (int) Math.round(tp[0]);
                this.tz = (int) Math.round(tp[1]);
                this.ty = (int) Math.floor(hub.raw(this.tx + 0.5, this.tz + 0.5));
                this.trot = rotAlong;
            } else {
                this.temple = false;
                this.tx = this.tz = this.ty = this.trot = 0;
            }

            // 1..2 пары резных каменных столбов с янтарными вставками по обе стороны лесной тропы
            double[] p1L = trailSidePoint(0.42, -3.4);
            double[] p1R = trailSidePoint(0.42, 3.4);
            addPillarPair((int) Math.round(p1L[0]), (int) Math.round(p1L[1]),
                          (int) Math.round(p1R[0]), (int) Math.round(p1R[1]));
            if (rnd.nextDouble() < 0.65) {
                double[] p2L = trailSidePoint(0.62, -3.5);
                double[] p2R = trailSidePoint(0.62, 3.5);
                if (!temple || Math.hypot(p2L[0] - tx, p2L[1] - tz) > 12) {
                    addPillarPair((int) Math.round(p2L[0]), (int) Math.round(p2L[1]),
                                  (int) Math.round(p2R[0]), (int) Math.round(p2R[1]));
                }
            }

            // Деревянная тележка у правой обочины тропы (референс 05_biome_sizo_grove.png)
            if (rnd.nextDouble() < 0.48) {
                double[] cp = trailSidePoint(0.31, 3.5);
                int cx0 = (int) Math.round(cp[0]), cz0 = (int) Math.round(cp[1]);
                if (hub.t(cx0 + 0.5, cz0 + 0.5) > 0.30) {
                    carts.add(new Cart(cx0, cz0, rotAlong, rnd.nextLong()));
                }
            }

            // Деревянная часовенка-памятник с цепью у обочины тропы за столбами (референс 05)
            if (rnd.nextDouble() < 0.36) {
                double[] sp = trailSidePoint(0.49, 4.7);
                int sx0 = (int) Math.round(sp[0]), sz0 = (int) Math.round(sp[1]);
                if (hub.t(sx0 + 0.5, sz0 + 0.5) > 0.30 && (!temple || Math.hypot(sx0 - tx, sz0 - tz) > 12)) {
                    shrines.add(new Shrine(sx0, sz0, (rotAlong + 3) & 3, rnd.nextLong()));
                }
            }

            // Замшелые скальные выступы вдоль обочин тропы (слева и справа, как на референсе 05) + в чаще рощи
            double[][] flankSpots = {{0.22, -6.4}, {0.25, 6.8}, {0.37, -7.2}, {0.53, 8.0}, {0.58, -6.8}};
            for (double[] fs : flankSpots) {
                if (rnd.nextDouble() > 0.82) continue;
                double[] bp = trailSidePoint(fs[0], fs[1] + (rnd.nextDouble() - 0.5) * 1.4);
                int bx = (int) Math.round(bp[0]), bz = (int) Math.round(bp[1]);
                if (hub.t(bx + 0.5, bz + 0.5) < 0.30) continue;
                if (nearLandmark(bx, bz, 4.5)) continue;
                boulders.add(new Boulder(bx, bz, 2.5 + rnd.nextDouble() * 1.4, rnd.nextLong()));
            }
            for (int k = 0, want = 8 + rnd.nextInt(5), tries = 0; k < want && tries < want * 18; ++tries) {
                double a = rnd.nextDouble() * Math.PI * 2, d = (0.16 + rnd.nextDouble() * 0.68) * R;
                int x = (int) Math.floor(cx + Math.cos(a) * d), z = (int) Math.floor(cz + Math.sin(a) * d);
                if (hub.t(x + 0.5, z + 0.5) < 0.34) continue;
                if (trailDist(x + 0.5, z + 0.5) < TRAIL_HW + 3.8) continue;
                if (nearLandmark(x, z, 6.0)) continue;
                boolean ok = true;
                for (Boulder b : boulders) if (Math.hypot(b.x - x, b.z - z) < b.r + 6.5) { ok = false; break; }
                if (!ok) continue;
                boulders.add(new Boulder(x, z, 2.3 + rnd.nextDouble() * 1.6, rnd.nextLong()));
                ++k;
            }

            // Стволы гигантских сизо (для fallback без DT и ориентиров вдоль аллеи)
            for (int k = 0, want = 14 + rnd.nextInt(5), tries = 0; k < want && tries < want * 24; ++tries) {
                double a = rnd.nextDouble() * Math.PI * 2, d = (0.16 + rnd.nextDouble() * 0.66) * R;
                int x = (int) Math.floor(cx + Math.cos(a) * d), z = (int) Math.floor(cz + Math.sin(a) * d);
                if (hub.t(x + 0.5, z + 0.5) < 0.32) continue;
                if (trailDist(x + 0.5, z + 0.5) < TRAIL_HW + 3.2) continue;
                if (nearLandmark(x, z, 6.5)) continue;
                boolean ok = true;
                for (CityPlan.Tree t : trees) if (Math.abs(t.x - x) < 9 && Math.abs(t.z - z) < 9) { ok = false; break; }
                for (Boulder b : boulders) if (Math.hypot(b.x - x, b.z - z) < b.r + 3.5) { ok = false; break; }
                if (!ok) continue;
                trees.add(new CityPlan.Tree(x, z, TREE_GIANT, 16 + rnd.nextInt(5), rnd.nextInt(1 << 30)));
                ++k;
            }
        }

        private void addPillarPair(int ax, int az, int bx, int bz) {
            if (hub.t(ax + 0.5, az + 0.5) < 0.26 || hub.t(bx + 0.5, bz + 0.5) < 0.26) return;
            pillarPairs.add(new PillarPair(ax, az, bx, bz));
            pillars.add(new int[]{ax, az});
            pillars.add(new int[]{bx, bz});
        }

        public boolean nearLandmark(double x, double z, double pad) {
            if (temple && Math.hypot(x - tx, z - tz) < pad + 7.5) return true;
            for (int[] p : pillars) if (Math.hypot(x - p[0], z - p[1]) < pad) return true;
            for (Cart c : carts) if (Math.hypot(x - c.x, z - c.z) < pad + 1.5) return true;
            for (Shrine s : shrines) if (Math.hypot(x - s.x, z - s.z) < pad + 2.0) return true;
            return false;
        }

        public double trailLen() {
            return Math.max(1.0, Math.hypot(tbx - tax, tbz - taz));
        }

        private double trailSway(double along) {
            return 8.5 * Math.sin(along * 0.046 + trailPhase);
        }

        public double trailDist(double px, double pz) {
            double dx = tbx - tax, dz = tbz - taz;
            double len = Math.max(1.0, Math.hypot(dx, dz));
            double ux = dx / len, uz = dz / len;
            double nx = -uz, nz = ux;
            double along = Math.max(0.0, Math.min(len, (px - tax) * ux + (pz - taz) * uz));
            double sway = trailSway(along);
            double cx0 = tax + ux * along + nx * sway;
            double cz0 = taz + uz * along + nz * sway;
            return Math.hypot(px - cx0, pz - cz0);
        }

        public double[] trailPoint(double u) {
            double dx = tbx - tax, dz = tbz - taz;
            double len = Math.max(1.0, Math.hypot(dx, dz));
            double ux = dx / len, uz = dz / len;
            double nx = -uz, nz = ux;
            double along = Math.max(0.0, Math.min(1.0, u)) * len;
            double sway = trailSway(along);
            return new double[]{tax + ux * along + nx * sway, taz + uz * along + nz * sway};
        }

        public double[] trailSidePoint(double u, double sideOffset) {
            double u0 = Math.max(0.0, Math.min(1.0, u - 0.02));
            double u1 = Math.max(0.0, Math.min(1.0, u + 0.02));
            double[] p = trailPoint(u);
            double[] a = trailPoint(u0);
            double[] b = trailPoint(u1);
            double tx0 = b[0] - a[0], tz0 = b[1] - a[1];
            double tl = Math.max(1e-4, Math.hypot(tx0, tz0));
            double nx = -tz0 / tl, nz = tx0 / tl;
            return new double[]{p[0] + nx * sideOffset, p[1] + nz * sideOffset};
        }
    }

    private static final ConcurrentHashMap<Long, Optional<Region>> CACHE = new ConcurrentHashMap<>();

    private SizoGrove() {
    }

    public static Region region(int sx, int sz) {
        return CACHE.computeIfAbsent(WildIslands.key(sx, sz), k -> Optional.ofNullable(gen(sx, sz))).orElse(null);
    }

    private static Region gen(int sx, int sz) {
        Random r = new Random(FIXED ^ (sx * 53982894593057L + sz * 317074912223L) * 0x9E3779B97F4A7C15L);
        if (r.nextDouble() > 0.5) return null;
        int cx = sx * SUPER + 520 + 160 + r.nextInt(SUPER - 840), cz = sz * SUPER + 520 + 160 + r.nextInt(SUPER - 840);
        if (Math.hypot(cx, cz) < 600) return null;
        if (AshenLands.blocks(cx, cz, 300)) return null;
        EmberRuins.Site es = EmberRuins.nearest(cx, cz);
        if (es != null && Math.hypot(cx - es.cx, cz - es.cz) < 460) return null;
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) {
                VioletHills.Region vr = VioletHills.region(Math.floorDiv(cx, VioletHills.SUPER) + dx, Math.floorDiv(cz, VioletHills.SUPER) + dz);
                if (vr != null && Math.hypot(cx - vr.cx, cz - vr.cz) < vr.Rr + 240) return null;
            }
        return new Region(cx, cz, r);
    }

    public static Region regionAt(double x, double z) {
        int sx = Math.floorDiv((int) Math.floor(x), SUPER), sz = Math.floorDiv((int) Math.floor(z), SUPER);
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) {
                Region rg = region(sx + dx, sz + dz);
                if (rg != null && Math.abs(x - rg.cx) < rg.Rr + 80 && Math.abs(z - rg.cz) < rg.Rr + 80) return rg;
            }
        return null;
    }

    public static boolean inside(double x, double z) {
        Region rg = regionAt(x, z);
        if (rg == null) return false;
        return rg.hub.t(x, z) > 0.10 + 8.0 / rg.hub.R;
    }

    public static List<CityPlan.Tree> treesNear(int x, int z) {
        Region rg = regionAt(x + 0.5, z + 0.5);
        if (rg == null) return List.of();
        List<CityPlan.Tree> out = new ArrayList<>(2);
        for (CityPlan.Tree t : rg.trees) if (Math.abs(x - t.x) <= 7 && Math.abs(z - t.z) <= 7) out.add(t);
        return out;
    }

    public static List<Boulder> bouldersNear(int x, int z) {
        Region rg = regionAt(x + 0.5, z + 0.5);
        if (rg == null) return List.of();
        List<Boulder> out = new ArrayList<>(2);
        for (Boulder b : rg.boulders) if (Math.abs(x - b.x) < b.r + 8 && Math.abs(z - b.z) < b.r + 8) out.add(b);
        return out;
    }

    public static List<int[]> pillarsIn(Region rg, int x0, int z0) {
        List<int[]> out = new ArrayList<>(2);
        for (int[] p : rg.pillars)
            if (p[0] >= x0 - 2 && p[0] <= x0 + 17 && p[1] >= z0 - 2 && p[1] <= z0 + 17) out.add(p);
        return out;
    }

    public static List<Cart> cartsIn(Region rg, int x0, int z0) {
        List<Cart> out = new ArrayList<>(1);
        if (rg == null) return out;
        for (Cart c : rg.carts) if (c.x >= x0 - 4 && c.x <= x0 + 19 && c.z >= z0 - 4 && c.z <= z0 + 19) out.add(c);
        return out;
    }

    public static List<Shrine> shrinesIn(Region rg, int x0, int z0) {
        List<Shrine> out = new ArrayList<>(1);
        if (rg == null) return out;
        for (Shrine s : rg.shrines) if (s.x >= x0 - 5 && s.x <= x0 + 20 && s.z >= z0 - 5 && s.z <= z0 + 20) out.add(s);
        return out;
    }

    public static Region templeIn(int x0, int z0) {
        Region rg = regionAt(x0 + 8.5, z0 + 8.5);
        if (rg == null || !rg.temple) return null;
        return Math.abs(rg.tx - x0) <= 32 && Math.abs(rg.tz - z0) <= 32 ? rg : null;
    }
}
