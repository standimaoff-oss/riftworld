package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Biome 04 «Фиолетовые холмы» (Rev 4 item 8): big meadows and smooth hills, rare trees, rare rivers with bridges, and
 * exactly TWO churches per biome region - one intact, one ruined - linked by a worn footpath. Colorful: 3 violet flower
 * types (placed by the chunk generator), lilac grass/foliage (biome json).
 *
 * Layout: the wild archipelago is divided into 880-block super-cells; ~45 % of suitable ones (beyond the city ring, away
 * from the Ashen Lands and the Ember Ruins) hold one region: a biome blob (R 130..170) around a single big smooth meadow
 * island (kind 7) that carries the two churches (+ their plateaus), the footpath between their doors, and one river that
 * cuts across the island and falls into the ocean at the rim; the footpath fords it on a plank bridge.
 * Deterministic from fixed seeds - no saved data.
 */
public final class VioletHills {
    public static final int SUPER = 880;
    private static final long SEED = 0x9B1C7L;

    /** Church template: 13 x 19, door centred in the +z (south) face, floor at local y 0 (data/riftworld/structures/ruins/church_violet*.nbt). */
    public static final int TW = 13, TD = 19;
    /**
     * Рев 14.29: вид дерева биома 04 — с СИРЕНЕВОЙ кроной, как на референсе 04_violet_hills.png.
     * Раньше здесь росли белые амбра/сизо, и фиолетовыми были только поля.
     */
    public static final int TREE_VIOLET = 10;

    public static final class Church {
        public final int cx, cz, y, x0, z0, wx, wz;
        public final String facing;      // door side in world (same convention as city plots)
        public final boolean ruined;
        public final int doorX, doorZ;   // outside step cell in front of the door (path endpoint)

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

        /** Distance outside the rotated footprint (0 inside). */
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
        public final Church churchA, churchB;                    // exactly two per biome (Rev 4): one intact, one ruined
        public final List<CityPlan.Tree> trees = new ArrayList<>();
        // footpath: churchA.door -> bulge -> churchB.door
        public final double pax, paz, pbx, pbz, pmx, pmz;
        // river: straight chord across the island
        public final double rax, raz, rbx, rbz;
        /**
         * Рев 14.59: ОСЬ ХРЕБТА луга. На референсе 04 террасы — это длинные параллельные
         * ленты, уходящие к горизонту, а не кольца вокруг вершины. Купол даёт изолинии-кольца,
         * поэтому рельеф луга строим как хребет вдоль этой оси: изолинии высоты вытягиваются
         * вдоль неё, и подпорные стенки идут длинными лентами, как на фото.
         */
        public final double axisX, axisZ;
        public final int riverLevel;
        public static final double RIVER_HW = 1.6;

        Region(int cx, int cz, Random rnd) {
            this.cx = cx; this.cz = cz;
            this.Rr = 130 + rnd.nextDouble() * 40;
            double R = 76 + rnd.nextDouble() * 18;
            double base = 98 + rnd.nextDouble() * 14;
            this.meadow = new CityPlan.Island(cx, cz, R, base, R * 0.6 + 12, rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283,
                    rnd.nextDouble() * 6.283, 7, 0.9);
            double aa = rnd.nextDouble() * Math.PI * 2;
            this.axisX = Math.cos(aa); this.axisZ = Math.sin(aa);
            // the two churches face the meadow centre from opposite sides
            double th = rnd.nextDouble() * Math.PI * 2, jj = (rnd.nextDouble() - 0.5) * 0.7;
            double rr = R * 0.52;
            int ax = (int) Math.floor(cx + Math.cos(th) * rr), az = (int) Math.floor(cz + Math.sin(th) * rr);
            int bx = (int) Math.floor(cx + Math.cos(th + Math.PI + jj) * rr), bz = (int) Math.floor(cz + Math.sin(th + Math.PI + jj) * rr);
            boolean aRuined = rnd.nextBoolean();
            this.churchA = new Church(ax, az, (int) Math.floor(meadow.raw(ax + 0.5, az + 0.5)), faceTo(ax - cx, az - cz), aRuined, meadow);
            this.churchB = new Church(bx, bz, (int) Math.floor(meadow.raw(bx + 0.5, bz + 0.5)), faceTo(bx - cx, bz - cz), !aRuined, meadow);
            // footpath with a soft bulge (never a ruler-straight line)
            double mx = (churchA.doorX + churchB.doorX) / 2.0, mz = (churchA.doorZ + churchB.doorZ) / 2.0;
            double pxn = -(churchB.doorZ - churchA.doorZ), pzn = churchB.doorX - churchA.doorX;
            double pl = Math.hypot(pxn, pzn) + 1e-6;
            double bulge = (rnd.nextDouble() - 0.5) * R * 0.45;
            this.pax = churchA.doorX; this.paz = churchA.doorZ; this.pbx = churchB.doorX; this.pbz = churchB.doorZ;
            this.pmx = mx + pxn / pl * bulge; this.pmz = mz + pzn / pl * bulge;
            // river: a chord across the island, roughly crosswise to the church line, spilling off both rims
            double phi = Math.atan2(churchB.doorZ - churchA.doorZ, churchB.doorX - churchA.doorX) + Math.PI / 2 + (rnd.nextDouble() - 0.5) * 0.9;
            double rl = R * 1.3;
            double rcx = cx + (rnd.nextDouble() - 0.5) * R * 0.3, rcz = cz + (rnd.nextDouble() - 0.5) * R * 0.3;
            this.rax = rcx - Math.cos(phi) * rl; this.raz = rcz - Math.sin(phi) * rl;
            this.rbx = rcx + Math.cos(phi) * rl; this.rbz = rcz + Math.sin(phi) * rl;
            this.riverLevel = (int) Math.floor(base) - 2;
            // rare trees (Rev 4): 4..7, well away from churches, path and river
            // Рев 14.31: на референсе деревья стоят по всему лугу (лиловые шапки среди полей) —
            // было 4..7 на весь регион, в кадре оказывалось одно.
            for (int k = 0, tries = 0; k < 16 && tries < 400; ++tries) {
                double a = rnd.nextDouble() * Math.PI * 2, d = (0.35 + rnd.nextDouble() * 0.55) * R;
                int x = (int) Math.floor(cx + Math.cos(a) * d), z = (int) Math.floor(cz + Math.sin(a) * d);
                if (meadow.t(x + 0.5, z + 0.5) < 0.3) continue;
                if (churchA.outside(x + 0.5, z + 0.5) < 10 || churchB.outside(x + 0.5, z + 0.5) < 10) continue;
                if (pathDist(x + 0.5, z + 0.5) < 3.0) continue;
                if (CityPlan.segDist(x + 0.5, z + 0.5, rax, raz, rbx, rbz, null) < 4.0) continue;
                boolean ok = true;
                for (CityPlan.Tree t : trees) if (Math.abs(t.x - x) < 11 && Math.abs(t.z - z) < 11) { ok = false; break; }
                if (!ok) continue;
                // на референсе все деревья поля — с лиловой кроной, разного размера.
                // Рев 14.78: стволы были 7–11 блоков, и зонт висел почти на уровне гряд — деревья
                // читались кустами (кадры 49/52 итерации 73). На фото ствол виден полностью,
                // а крона стоит на 11–17 блоках над полем.
                trees.add(new CityPlan.Tree(x, z, TREE_VIOLET, 11 + rnd.nextInt(7), rnd.nextInt(1 << 30)));
                ++k;
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

    private VioletHills() {
    }

    public static Region region(int sx, int sz) {
        return CACHE.computeIfAbsent(WildIslands.key(sx, sz), k -> Optional.ofNullable(gen(sx, sz))).orElse(null);
    }

    private static Region gen(int sx, int sz) {
        Random r = new Random(SEED ^ (sx * 53982894593057L + sz * 317074912223L) * 0x9E3779B97F4A7C15L);
        if (r.nextDouble() > 0.45) return null;
        int cx = sx * SUPER + 150 + r.nextInt(SUPER - 300), cz = sz * SUPER + 150 + r.nextInt(SUPER - 300);
        if (Math.hypot(cx, cz) < 560) return null;                                                        // city ring
        if (AshenLands.blocks(cx, cz, 260)) return null;                                                  // адские угодья (любой регион)
        EmberRuins.Site es = EmberRuins.nearest(cx, cz);
        if (es != null && Math.hypot(cx - es.cx, cz - es.cz) < 400) return null;                          // keep ruins regions apart
        return new Region(cx, cz, r);
    }

    /** The region whose influence can reach (x, z), or null. */
    public static Region regionAt(double x, double z) {
        Region rg = region(Math.floorDiv((int) Math.floor(x), SUPER), Math.floorDiv((int) Math.floor(z), SUPER));
        if (rg == null) return null;
        return Math.abs(x - rg.cx) < rg.Rr + 70 && Math.abs(z - rg.cz) < rg.Rr + 70 ? rg : null;
    }

    /**
     * Рев 14.34: биом 04 — это САМ ЛУГ, а не «блоб» вокруг него. Раньше проверка накрывала и море
     * вокруг острова, и {@code /locate biome riftworld:violet_hills} мог привести в воду.
     * Теперь берём луг с запасом 8 блоков от кромки — ровно там, где стоят поля.
     */
    public static boolean inside(double x, double z) {
        Region rg = regionAt(x, z);
        if (rg == null) return false;
        CityPlan.Island m = rg.meadow;
        return m.t(x, z) > 0.10 + 8.0 / m.R;
    }

    public static List<CityPlan.Tree> treesNear(int x, int z) {
        Region rg = regionAt(x + 0.5, z + 0.5);
        if (rg == null) return List.of();
        List<CityPlan.Tree> out = new ArrayList<>(2);
        for (CityPlan.Tree t : rg.trees) if (Math.abs(x - t.x) <= 4 && Math.abs(z - t.z) <= 4) out.add(t);
        return out;
    }
}
