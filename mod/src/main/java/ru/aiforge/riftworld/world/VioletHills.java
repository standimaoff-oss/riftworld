package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Biome 04 «Фиолетовые холмы» Rev 7: большие луга и плавные холмы, редкие деревья,
 * редкие речки с мостами, и ровно ДВЕ церкви на регион — одна целая, одна руины,
 * связаны утоптанной тропинкой. Цветное: 3 типа фиолетовых цветов ковром, сиреневая трава/листва.
 * Rev7 улучшения: регион больше (160-220), луга больше (90-120), деревьев 8-12 с Dynamic Trees интеграцией,
 * цветочные поля гуще, речка шире, тропинка извилистее.
 */
public final class VioletHills {
    public static final int SUPER = 880;
    private static final long SEED = 0x9B1C7L;

    public static final int TW = 13, TD = 19;

    public static final class Church {
        public final int cx, cz, y, x0, z0, wx, wz;
        public final String facing;
        public final boolean ruined;
        public final int doorX, doorZ;

        Church(int cx, int cz, int y, String facing, boolean ruined, CityPlan.Island meadow) {
            this.cx = cx; this.cz = cz; this.y = y; this.facing = facing; this.ruined = ruined;
            boolean ew = facing.equals("east") || facing.equals("west");
            this.wx = ew ? TD : TW;
            this.wz = ew ? TW : TD;
            this.x0 = cx - wx / 2;
            this.z0 = cz - wz / 2;
            int off = TD / 2 + 1;
            switch (facing) {
                case "north" -> { this.doorX = cx; this.doorZ = cz - off; }
                case "east" -> { this.doorX = cx + off; this.doorZ = cz; }
                case "west" -> { this.doorX = cx - off; this.doorZ = cz; }
                default -> { this.doorX = cx; this.doorZ = cz + off; }
            }
        }

        public double outside(double x, double z) {
            double dx = Math.max(Math.max(x0 - x, 0), x - (x0 + wx - 1));
            double dz = Math.max(Math.max(z0 - z, 0), z - (z0 + wz - 1));
            return Math.hypot(dx, dz);
        }
    }

    public static final class Region {
        public final int cx, cz;
        public final double Rr;
        public final CityPlan.Island meadow;
        public final Church churchA, churchB;
        public final List<CityPlan.Tree> trees = new ArrayList<>();
        public final double pax, paz, pbx, pbz, pmx, pmz;
        public final double rax, raz, rbx, rbz;
        public final int riverLevel;
        public static final double RIVER_HW = 2.2; // было 1.6, теперь шире

        Region(int cx, int cz, Random rnd) {
            this.cx = cx; this.cz = cz;
            this.Rr = 160 + rnd.nextDouble() * 60; // было 130-170, теперь 160-220
            double R = 90 + rnd.nextDouble() * 30; // было 76-94
            double base = 100 + rnd.nextDouble() * 16;
            this.meadow = new CityPlan.Island(cx, cz, R, base, R * 0.7 + 16, rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283,
                    rnd.nextDouble() * 6.283, 7, 0.85);
            double th = rnd.nextDouble() * Math.PI * 2, jj = (rnd.nextDouble() - 0.5) * 0.9;
            double rr = R * 0.58; // церкви дальше от центра
            int ax = (int) Math.floor(cx + Math.cos(th) * rr), az = (int) Math.floor(cz + Math.sin(th) * rr);
            int bx = (int) Math.floor(cx + Math.cos(th + Math.PI + jj) * rr), bz = (int) Math.floor(cz + Math.sin(th + Math.PI + jj) * rr);
            boolean aRuined = rnd.nextBoolean();
            this.churchA = new Church(ax, az, (int) Math.floor(meadow.raw(ax + 0.5, az + 0.5)), faceTo(ax - cx, az - cz), aRuined, meadow);
            this.churchB = new Church(bx, bz, (int) Math.floor(meadow.raw(bx + 0.5, bz + 0.5)), faceTo(bx - cx, bz - cz), !aRuined, meadow);
            // тропинка с более сильным изгибом
            double mx = (churchA.doorX + churchB.doorX) / 2.0, mz = (churchA.doorZ + churchB.doorZ) / 2.0;
            double pxn = -(churchB.doorZ - churchA.doorZ), pzn = churchB.doorX - churchA.doorX;
            double pl = Math.hypot(pxn, pzn) + 1e-6;
            double bulge = (rnd.nextDouble() - 0.5) * R * 0.65; // было 0.45
            this.pax = churchA.doorX; this.paz = churchA.doorZ; this.pbx = churchB.doorX; this.pbz = churchB.doorZ;
            this.pmx = mx + pxn / pl * bulge; this.pmz = mz + pzn / pl * bulge;
            // речка поперёк
            double phi = Math.atan2(churchB.doorZ - churchA.doorZ, churchB.doorX - churchA.doorX) + Math.PI / 2 + (rnd.nextDouble() - 0.5) * 1.1;
            double rl = R * 1.45;
            double rcx = cx + (rnd.nextDouble() - 0.5) * R * 0.35, rcz = cz + (rnd.nextDouble() - 0.5) * R * 0.35;
            this.rax = rcx - Math.cos(phi) * rl; this.raz = rcz - Math.sin(phi) * rl;
            this.rbx = rcx + Math.cos(phi) * rl; this.rbz = rcz + Math.sin(phi) * rl;
            this.riverLevel = (int) Math.floor(base) - 2;

            // редкие деревья: 8-12, было 4-7, с интеграцией Dynamic Trees
            for (int k = 0, tries = 0; k < 12 && tries < 220; ++tries) {
                double a = rnd.nextDouble() * Math.PI * 2, d = (0.30 + rnd.nextDouble() * 0.60) * R;
                int x = (int) Math.floor(cx + Math.cos(a) * d), z = (int) Math.floor(cz + Math.sin(a) * d);
                if (meadow.t(x + 0.5, z + 0.5) < 0.28) continue;
                if (churchA.outside(x + 0.5, z + 0.5) < 12 || churchB.outside(x + 0.5, z + 0.5) < 12) continue;
                if (pathDist(x + 0.5, z + 0.5) < 3.5) continue;
                if (CityPlan.segDist(x + 0.5, z + 0.5, rax, raz, rbx, rbz, null) < 5.0) continue;
                boolean ok = true;
                for (CityPlan.Tree t : trees) if (Math.hypot(t.x - x, t.z - z) < 7) { ok = false; break; }
                if (!ok) continue;
                int kind = rnd.nextDouble() < 0.50 ? 0 : 1;
                // Dynamic Trees: если мод есть, kind 0/1 заменится на DT виды
                int h = kind == 0 ? 9 + rnd.nextInt(6) : 6 + rnd.nextInt(4);
                trees.add(new CityPlan.Tree(x, z, kind, h, rnd.nextInt(1 << 30)));
                ++k;
                if (k >= 8 && rnd.nextDouble() < 0.35) break; // 8-12
            }
        }

        private static String faceTo(double dx, double dz) {
            return Math.abs(dx) >= Math.abs(dz) ? (dx > 0 ? "east" : "west") : (dz > 0 ? "south" : "north");
        }

        public double pathDist(double x, double z) {
            return Math.min(CityPlan.segDist(x, z, pax, paz, pmx, pmz, null), CityPlan.segDist(x, z, pmx, pmz, pbx, pbz, null));
        }
    }

    private static final ConcurrentHashMap<Long, Optional<Region>> CACHE = new ConcurrentHashMap<>();

    private VioletHills() {}

    public static Region region(int sx, int sz) {
        return CACHE.computeIfAbsent(WildIslands.key(sx, sz), k -> Optional.ofNullable(gen(sx, sz))).orElse(null);
    }

    private static Region gen(int sx, int sz) {
        Random r = new Random(SEED ^ (sx * 53982894593057L + sz * 317074912223L) * 0x9E3779B97F4A7C15L);
        if (r.nextDouble() > 0.58) return null; // было 0.45, теперь чаще
        int cx = sx * SUPER + 150 + r.nextInt(SUPER - 300), cz = sz * SUPER + 150 + r.nextInt(SUPER - 300);
        if (Math.hypot(cx, cz) < 600) return null;
        if (Math.hypot(cx - AshenLands.CX, cz - AshenLands.CZ) < AshenLands.R + 280) return null;
        EmberRuins.Site es = EmberRuins.nearest(cx, cz);
        if (es != null && Math.hypot(cx - es.cx, cz - es.cz) < 440) return null;
        return new Region(cx, cz, r);
    }

    public static Region regionAt(double x, double z) {
        Region rg = region(Math.floorDiv((int) Math.floor(x), SUPER), Math.floorDiv((int) Math.floor(z), SUPER));
        if (rg == null) return null;
        return Math.abs(x - rg.cx) < rg.Rr + 80 && Math.abs(z - rg.cz) < rg.Rr + 80 ? rg : null;
    }

    public static boolean inside(double x, double z) {
        Region rg = regionAt(x, z);
        if (rg == null) return false;
        double d = Math.hypot(x - rg.cx, z - rg.cz) + (ElisarChunkGenerator.vnoise(x * 0.028, z * 0.028, 55) - 0.5) * 44;
        return d < rg.Rr;
    }

    public static List<CityPlan.Tree> treesNear(int x, int z) {
        Region rg = regionAt(x + 0.5, z + 0.5);
        if (rg == null) return List.of();
        List<CityPlan.Tree> out = new ArrayList<>(3);
        for (CityPlan.Tree t : rg.trees) if (Math.abs(x - t.x) <= 5 && Math.abs(z - t.z) <= 5) out.add(t);
        return out;
    }
}
