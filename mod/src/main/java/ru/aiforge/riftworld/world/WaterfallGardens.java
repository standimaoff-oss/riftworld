package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Biome 07 «Висячие сады» — полностью переработанный (Rev 7):
 * - БОЛЬШОЙ остров-гора 70-110 блоков радиуса, верх на высоте диких островов (110-130), низ касается океана
 * - Многоуровневые известняковые террасы STEP=5 с мягким 1-блочным переходом, 5-блочные скалы
 * - Водопады с каждого карниза в общее море и с полки на полку, пруды с травертиновой кромкой, горячие источники с тёплым светом
 * - Почти джунгли: густая трава 55%, цветы всех сортов, моховые кляксы, свисающие лианы-занавесы с каждой губы
 * - Деревья очень маленькие 4-6 блоков, 28-38 штук: круглые амбра-пушистики и стройные сизо
 * - Из не-природы только редкие мосты 1-2 через ущелья, с провисом и опорами
 * - Своё небо аква, кремовый туман, бирюзовая вода, споры-пыльца
 * - Интеграция с Dynamic Trees: если мод есть, деревья заменятся на динамические (пока обычные, но структура готова)
 */
public final class WaterfallGardens {
    public static final int SUPER = 960;
    private static final long FIXED = 0x7A7EFA11L;
    public static final double STEP = 5.0;
    public static final int KIND = 9;

    public static final class Region {
        public final int cx, cz;
        public final double Rr;
        public final CityPlan.Island hub;
        public final List<CityPlan.Tree> trees = new ArrayList<>();
        public final List<CityPlan.Bridge> bridges = new ArrayList<>();
        // pools for extra decoration (center positions)
        public final List<int[]> pools = new ArrayList<>();
        public final List<int[]> springs = new ArrayList<>();

        Region(int cx, int cz, Random rnd) {
            this.cx = cx; this.cz = cz;
            // УВЕЛИЧЕНО: 70-110 радиус, было 46-66
            double R = 70 + rnd.nextDouble() * 40;
            this.Rr = R * 2.2;
            // верх выше: 110-130, было 96-112
            double base = 110 + rnd.nextDouble() * 20;
            this.hub = new CityPlan.Island(cx, cz, R, base, R * 1.3 + 12, rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283,
                    rnd.nextDouble() * 6.283, KIND, 1.8);

            // мосты: 1-2 через ущелья, было 0-1
            int bridgeWant = 1 + (rnd.nextDouble() < 0.55 ? 1 : 0);
            for (int bIdx = 0; bIdx < bridgeWant; bIdx++) {
                for (int t = 0; t < 16; ++t) {
                    double a = rnd.nextDouble() * Math.PI * 2;
                    double mx = cx + (rnd.nextDouble() - 0.5) * R * 0.65, mz = cz + (rnd.nextDouble() - 0.5) * R * 0.65;
                    double hl = (0.30 + rnd.nextDouble() * 0.28) * R;
                    double ax = mx - Math.cos(a) * hl, az = mz - Math.sin(a) * hl;
                    double bx = mx + Math.cos(a) * hl, bz = mz + Math.sin(a) * hl;
                    if (hub.t(ax + 0.5, az + 0.5) < 0.32 || hub.t(bx + 0.5, bz + 0.5) < 0.32) continue;
                    int ya = ElisarChunkGenerator.gardenTerraceTop(hub, ax + 0.5, az + 0.5, CityPlan.get().ocean) + 1;
                    int yb = ElisarChunkGenerator.gardenTerraceTop(hub, bx + 0.5, bz + 0.5, CityPlan.get().ocean) + 1;
                    if (Math.abs(ya - yb) > 4) continue;
                    // avoid overlapping bridges
                    boolean overlap = false;
                    for (CityPlan.Bridge ob : bridges) {
                        if (CityPlan.segDist((ax+bx)/2, (az+bz)/2, ob.ax(), ob.az(), ob.bx(), ob.bz(), null) < 12) { overlap = true; break; }
                    }
                    if (overlap) continue;
                    bridges.add(new CityPlan.Bridge(ax, az, bx, bz, ya, yb, 1.8 + rnd.nextDouble() * 1.2, 0.95));
                    break;
                }
            }

            // пруды и горячие источники — больше
            int poolWant = 8 + rnd.nextInt(7);
            for (int i = 0; i < poolWant; i++) {
                double ang = rnd.nextDouble() * Math.PI * 2, d = (0.2 + rnd.nextDouble() * 0.65) * R;
                int x = (int) Math.floor(cx + Math.cos(ang) * d), z = (int) Math.floor(cz + Math.sin(ang) * d);
                if (hub.t(x + 0.5, z + 0.5) < 0.3) continue;
                pools.add(new int[]{x, z, rnd.nextDouble() < 0.35 ? 1 : 0}); // 1 = spring
                if (pools.get(pools.size()-1)[2]==1) springs.add(new int[]{x,z});
            }

            // деревья очень маленькие 4-6 блоков, 28-38 штук, было 18-26
            for (int k = 0, want = 28 + rnd.nextInt(11), tries = 0; k < want && tries < want * 30; ++tries) {
                double a = rnd.nextDouble() * Math.PI * 2, d = (0.12 + rnd.nextDouble() * 0.75) * R;
                int x = (int) Math.floor(cx + Math.cos(a) * d), z = (int) Math.floor(cz + Math.sin(a) * d);
                if (hub.t(x + 0.5, z + 0.5) < 0.28) continue;
                boolean ok = true;
                for (CityPlan.Tree t : trees) if (Math.hypot(t.x - x, t.z - z) < 4.5) { ok = false; break; }
                for (CityPlan.Bridge b : bridges)
                    if (CityPlan.segDist(x + 0.5, z + 0.5, b.ax(), b.az(), b.bx(), b.bz(), null) < 4.0) { ok = false; break; }
                for (int[] p : pools) if (Math.hypot(p[0]-x, p[1]-z) < 5) { ok = false; break; }
                if (!ok) continue;
                int kind = rnd.nextDouble() < 0.62 ? 1 : 0; // 0=sizo slim, 1=ambra round
                int h = 4 + rnd.nextInt(3); // 4-6
                // Dynamic Trees integration point: if mod "dynamictrees" loaded, we could spawn DT trees here
                // For now vanilla mini-trees, but keep seed for future DT
                trees.add(new CityPlan.Tree(x, z, kind, h, rnd.nextInt(1 << 30)));
                ++k;
            }
        }
    }

    private static final ConcurrentHashMap<Long, Optional<Region>> CACHE = new ConcurrentHashMap<>();

    private WaterfallGardens() {}

    public static Region region(int sx, int sz) {
        return CACHE.computeIfAbsent(WildIslands.key(sx, sz), k -> Optional.ofNullable(gen(sx, sz))).orElse(null);
    }

    private static Region gen(int sx, int sz) {
        Random r = new Random(FIXED ^ (sx * 53982894593057L + sz * 317074912223L) * 0x9E3779B97F4A7C15L);
        if (r.nextDouble() > 0.78) return null; // REV7b: сады должны встречаться (78% секторов)
        int cx = sx * SUPER + 480 + 120 + r.nextInt(SUPER - 720), cz = sz * SUPER + 480 + 120 + r.nextInt(SUPER - 720);
        if (Math.hypot(cx, cz) < 620) return null; // дальше от города, было 560
        if (Math.hypot(cx - AshenLands.CX, cz - AshenLands.CZ) < AshenLands.R + 300) return null;
        EmberRuins.Site es = EmberRuins.nearest(cx, cz);
        if (es != null && Math.hypot(cx - es.cx, cz - es.cz) < 480) return null;
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) {
                SizoGrove.Region sr = SizoGrove.region(Math.floorDiv(cx, SizoGrove.SUPER) + dx, Math.floorDiv(cz, SizoGrove.SUPER) + dz);
                if (sr != null && Math.hypot(cx - sr.cx, cz - sr.cz) < sr.Rr + 240) return null;
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
                if (rg != null && Math.abs(x - rg.cx) < rg.Rr + 90 && Math.abs(z - rg.cz) < rg.Rr + 90) return rg;
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
        List<CityPlan.Tree> out = new ArrayList<>(3);
        for (CityPlan.Tree t : rg.trees) if (Math.abs(x - t.x) <= 6 && Math.abs(z - t.z) <= 6) out.add(t);
        return out;
    }
}
