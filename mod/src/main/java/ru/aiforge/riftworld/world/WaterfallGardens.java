package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Biome 07 «Висячие сады» (референс {@code reports/refs_rev4/07_waterfall_gardens.png}).
 *
 * <ul>
 *   <li><b>Архипелаг из 3–6 крупных островов</b> разных некруглых форм (изгибы, лопасти, вытянутость),
 *       с разбросом радиусов от 22–34 до 43–55 блоков и извилистыми речными протоками шириной
 *       13–22 блока между островами (никаких мелких скал в море).</li>
 *   <li><b>Нижний садовый ярус — на уровне моря</b> ({@code ocean + 1}), ниже уровня моря —
 *       1–2 пустых каменных подводных яруса ({@code ocean - 4} и {@code ocean - 9}) под толщей воды
 *       и сплошное каменное основание острова до самого бедрока без единой пустоты.</li>
 *   <li><b>{@code /locate biome riftworld:waterfall_gardens}</b> опирается на {@link #landAt},
 *       совпадающий с надводной сушей садовых островов, а небо и дымка архипелага опираются на
 *       {@link #skyInside}, накрывая и острова, и реки между ними.</li>
 * </ul>
 */
public final class WaterfallGardens {
    public static final int SUPER = 1040;
    private static final long FIXED = 0x7A7EFA11L;
    /** Высота надводного яруса известняковых полок (блоков). */
    public static final double STEP = 7.0;
    public static final int KIND = 9;
    /** Kind of the garden's own tree (green oak, the reference silhouette). */
    public static final int TREE_OAK = 4;

    /** Заранее выбранный водопад: точка на кромке полки, откуда вода стекает вниз. */
    public static final class Fall {
        public final int x, z;
        public final double w;
        public final long seed;

        Fall(int x, int z, double w, long seed) {
            this.x = x;
            this.z = z;
            this.w = w;
            this.seed = seed;
        }
    }

    public static final class Region {
        public final int cx, cz;
        /** Габарит региона: дальше него ни один остров архипелага не выходит. */
        public final double Rr;
        public final boolean big = true;
        /** 3–6 крупных островов архипелага; {@link #hub} — самый крупный якорный остров. */
        public final List<CityPlan.Island> isles = new ArrayList<>(6);
        public final CityPlan.Island hub;
        public final List<CityPlan.Tree> trees = new ArrayList<>();
        public final List<Fall> falls = new ArrayList<>();

        Region(int cx, int cz, Random rnd) {
            this.cx = cx;
            this.cz = cz;
            int ocean = CityPlan.get().ocean;
            int want = 3 + rnd.nextInt(4); // строго от 3 до 6 крупных островов

            // Разброс радиусов: от 22..33 у меньших крупных островов до 43..55 у главного массива
            double[] rs = new double[want];
            rs[0] = 43.0 + rnd.nextDouble() * 12.0;                 // 43..55
            rs[1] = 31.0 + rnd.nextDouble() * 11.0;                 // 31..42
            rs[2] = 22.0 + rnd.nextDouble() * 11.0;                 // 22..33
            for (int i = 3; i < want; ++i) {
                rs[i] = 23.0 + rnd.nextDouble() * 27.0;             // 23..50
            }

            double a0 = rnd.nextDouble() * Math.PI * 2;
            CityPlan.Island first = makeIsland(
                    cx + Math.cos(a0) * 14.0,
                    cz + Math.sin(a0) * 14.0,
                    rs[0], ocean, rnd);
            isles.add(first);

            for (int i = 1; i < want; ++i) {
                double ri = rs[i];
                CityPlan.Island placed = null;
                for (int attempt = 0; attempt < 48; ++attempt) {
                    CityPlan.Island parent = isles.get((i - 1 + attempt) % isles.size());
                    double ang = rnd.nextDouble() * Math.PI * 2;
                    double riverWidth = 13.0 + rnd.nextDouble() * 9.0; // река шириной 13..22 блока между берегами
                    CityPlan.Island cand = makeIsland(0, 0, ri, ocean, rnd);
                    double dist = shoreRadius(parent, ang) + shoreRadius(cand, ang + Math.PI) + riverWidth;
                    double px = parent.cx + Math.cos(ang) * dist;
                    double pz = parent.cz + Math.sin(ang) * dist;
                    if (Math.hypot(px - cx, pz - cz) + ri * 1.45 > 215.0) continue;

                    CityPlan.Island shifted = copyAt(cand, px, pz);
                    boolean ok = true;
                    for (CityPlan.Island other : isles) {
                        double a2 = Math.atan2(pz - other.cz, px - other.cx);
                        double gap = Math.hypot(px - other.cx, pz - other.cz)
                                - (shoreRadius(other, a2) + shoreRadius(shifted, a2 + Math.PI));
                        if (gap < 11.5) {
                            ok = false;
                            break;
                        }
                    }
                    if (ok) {
                        placed = shifted;
                        break;
                    }
                }
                if (placed != null) {
                    isles.add(placed);
                }
            }

            double extent = 0;
            for (CityPlan.Island isl : isles) {
                extent = Math.max(extent, Math.hypot(isl.cx - cx, isl.cz - cz) + isl.maxReach());
                int fallWant = isl.R >= 38 ? 3 : 2;
                for (int k = 0, tries = 0; k < fallWant && tries < fallWant * 45; ++tries) {
                    double fa = rnd.nextDouble() * Math.PI * 2;
                    double fx = 0, fz = 0;
                    boolean edge = false;
                    int prevTop = Integer.MIN_VALUE;
                    for (double rd = 0.22 * isl.R; rd < isl.maxReach(); rd += 1.0) {
                        double tx = isl.cx + Math.cos(fa) * rd, tz = isl.cz + Math.sin(fa) * rd;
                        if (isl.t(tx, tz) < 0.17) break; // дошли до уровня моря
                        int topNow = ElisarChunkGenerator.gardenTerraceTop(isl, tx, tz, ocean);
                        if (prevTop != Integer.MIN_VALUE && prevTop - topNow >= 4 && topNow >= ocean + 1) {
                            fx = tx - Math.cos(fa);
                            fz = tz - Math.sin(fa);
                            edge = true;
                            break;
                        }
                        prevTop = topNow;
                    }
                    if (!edge || isl.t(fx, fz) < 0.19) continue;
                    boolean ok = true;
                    for (Fall f : falls) {
                        if (Math.hypot(f.x - fx, f.z - fz) < 10) {
                            ok = false;
                            break;
                        }
                    }
                    if (!ok) continue;
                    falls.add(new Fall((int) Math.floor(fx), (int) Math.floor(fz), 2.2 + rnd.nextDouble() * 2.2, rnd.nextLong()));
                    ++k;
                }
            }
            this.hub = isles.get(0);
            this.Rr = extent + 12.0;
        }

        private static CityPlan.Island makeIsland(double x, double z, double R, int ocean, Random rnd) {
            double tiers = R >= 42 ? (3.8 + rnd.nextDouble() * 1.1)
                    : R >= 31 ? (3.0 + rnd.nextDouble() * 1.0)
                    : (2.3 + rnd.nextDouble() * 0.9);
            int peak = ocean + 1 + (int) Math.round(tiers * STEP);
            CityPlan.Island isl = new CityPlan.Island(
                    x, z, R, peak - 6, R * 1.15 + 10,
                    rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283,
                    KIND, 1.5);
            isl.elong = 0.08 + rnd.nextDouble() * 0.30;
            isl.elongAng = rnd.nextDouble() * Math.PI;
            isl.bend = (rnd.nextDouble() - 0.5) * 0.42;
            isl.lobe2 = (rnd.nextDouble() - 0.5) * 0.24;
            isl.lobe3 = (rnd.nextDouble() - 0.5) * 0.18;
            return isl;
        }

        private static CityPlan.Island copyAt(CityPlan.Island src, double x, double z) {
            CityPlan.Island dst = new CityPlan.Island(x, z, src.R, src.base, src.depth, src.p1, src.p2, src.p3, src.kind, src.hill);
            dst.elong = src.elong;
            dst.elongAng = src.elongAng;
            dst.bend = src.bend;
            dst.lobe2 = src.lobe2;
            dst.lobe3 = src.lobe3;
            return dst;
        }

        /** Оценка радиуса надводного берега (t = 0.16) острова в мировом направлении ang. */
        private static double shoreRadius(CityPlan.Island isl, double ang) {
            double dx = Math.cos(ang), dz = Math.sin(ang);
            double lo = 4.0, hi = isl.maxReach();
            for (int it = 0; it < 14; ++it) {
                double mid = 0.5 * (lo + hi);
                if (isl.t(isl.cx + dx * mid, isl.cz + dz * mid) >= 0.16) lo = mid;
                else hi = mid;
            }
            return 0.5 * (lo + hi);
        }

        public boolean owns(CityPlan.Island isl) {
            for (CityPlan.Island a : isles) if (a == isl) return true;
            return false;
        }

        public CityPlan.Island islandAt(double x, double z) {
            for (CityPlan.Island a : isles) if (a.t(x, z) > 0) return a;
            return null;
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
        if (r.nextDouble() > 0.44) return null;
        int cx = sx * SUPER + 240 + r.nextInt(SUPER - 480);
        int cz = sz * SUPER + 240 + r.nextInt(SUPER - 480);
        if (Math.hypot(cx, cz) < 680) return null;
        if (Math.hypot(cx - 640, cz - 690) < 380) return null; // не перекрываем морские пробы spots.json
        if (AshenLands.blocks(cx, cz, 340)) return null;
        EmberRuins.Site es = EmberRuins.nearest(cx, cz);
        if (es != null && Math.hypot(cx - es.cx, cz - es.cz) < 460) return null;
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) {
                SizoGrove.Region sr = SizoGrove.region(Math.floorDiv(cx, SizoGrove.SUPER) + dx, Math.floorDiv(cz, SizoGrove.SUPER) + dz);
                if (sr != null && Math.hypot(cx - sr.cx, cz - sr.cz) < sr.Rr + 260) return null;
                VioletHills.Region vr = VioletHills.region(Math.floorDiv(cx, VioletHills.SUPER) + dx, Math.floorDiv(cz, VioletHills.SUPER) + dz);
                if (vr != null && Math.hypot(cx - vr.cx, cz - vr.cz) < vr.Rr + 260) return null;
            }
        return new Region(cx, cz, r);
    }

    public static Region regionAt(double x, double z) {
        int sx = Math.floorDiv((int) Math.floor(x), SUPER), sz = Math.floorDiv((int) Math.floor(z), SUPER);
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) {
                Region rg = region(sx + dx, sz + dz);
                if (rg != null && Math.abs(x - rg.cx) < rg.Rr && Math.abs(z - rg.cz) < rg.Rr) return rg;
            }
        return null;
    }

    /**
     * Надводная суша острова садов в точке (x, z), гарантированно выше уровня моря (t &gt;= 0.18).
     * Используется {@link ElisarBiomeSource} и {@code /locate biome riftworld:waterfall_gardens}:
     * локатор никогда не попадает в реку, море или на подводный каменный уступ.
     */
    public static CityPlan.Island landAt(double x, double z) {
        Region rg = regionAt(x, z);
        if (rg == null) return null;
        for (CityPlan.Island a : rg.isles) {
            if (a.t(x, z) >= 0.18) return a;
        }
        return null;
    }

    /** Биом садов = надводная суша архипелага. */
    public static boolean inside(double x, double z) {
        return landAt(x, z) != null;
    }

    /**
     * Атмосферная область архипелага (острова + речные протоки между ними + точки обзора у скал)
     * для {@code GardenSky} и {@code ElisarSky}: голубое небо, облака и солнечная дымка не гаснут над реками.
     */
    public static boolean skyInside(double x, double z) {
        return skyWeight(x, z) > 0.01F;
    }

    /** Плавный вес атмосферы Висячих садов [0..1] с smoothstep-поясом 90 блоков вокруг архипелага. */
    public static float skyWeight(double x, double z) {
        int sx = Math.floorDiv((int) Math.floor(x), SUPER), sz = Math.floorDiv((int) Math.floor(z), SUPER);
        float best = 0.0F;
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                Region rg = region(sx + dx, sz + dz);
                if (rg == null) continue;
                double d = Math.hypot(x - rg.cx, z - rg.cz);
                if (d >= rg.Rr + 102.0) continue;
                double u = 1.0 - Math.max(0.0, Math.min(1.0, (d - (rg.Rr + 12.0)) / 90.0));
                float w = (float) (u * u * (3.0 - 2.0 * u));
                if (w > best) best = w;
            }
        }
        return best;
    }

    public static Fall fallNear(Region rg, double x, double z) {
        for (Fall f : rg.falls) {
            if (Math.hypot(x - f.x, z - f.z) <= f.w) return f;
        }
        return null;
    }

    public static boolean near(Region rg, double x, double z, double r, double pad) {
        for (CityPlan.Island gi : rg.isles) {
            if (Math.hypot(x - gi.cx, z - gi.cz) < r + gi.maxReach() + pad) return true;
        }
        return false;
    }

    /** Проверка по всем соседним регионам, чтобы дикие скалы не спавнились рядом с архипелагом садов. */
    public static boolean nearAny(double x, double z, double r, double pad) {
        int sx = Math.floorDiv((int) Math.floor(x), SUPER), sz = Math.floorDiv((int) Math.floor(z), SUPER);
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) {
                Region rg = region(sx + dx, sz + dz);
                if (rg != null && near(rg, x, z, r, pad)) return true;
            }
        return false;
    }

    public static List<CityPlan.Tree> treesNear(int x, int z) {
        // Все деревья садов ставит DtGround (riftworlddt:garden_oak); при отсутствии DT есть лёгкий запас ниже
        if (DtGround.dtPresent()) return List.of();
        Region rg = regionAt(x + 0.5, z + 0.5);
        if (rg == null) return List.of();
        CityPlan.Island isl = landAt(x + 0.5, z + 0.5);
        if (isl == null) return List.of();
        List<CityPlan.Tree> out = new ArrayList<>(2);
        int gx = Math.floorDiv(x, 12), gz = Math.floorDiv(z, 12);
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) {
                int cx2 = (gx + dx) * 12 + 6, cz2 = (gz + dz) * 12 + 6;
                if (landAt(cx2 + 0.5, cz2 + 0.5) == null) continue;
                if (ElisarChunkGenerator.hash(gx + dx, gz + dz, 911) > 0.35) continue;
                if (Math.abs(x - cx2) <= 10 && Math.abs(z - cz2) <= 10) {
                    out.add(new CityPlan.Tree(cx2, cz2, TREE_OAK, 7, (gx + dx) * 734287 + (gz + dz) * 912931));
                }
            }
        return out;
    }
}
