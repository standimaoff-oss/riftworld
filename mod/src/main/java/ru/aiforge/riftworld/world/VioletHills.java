package ru.aiforge.riftworld.world;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Biome 04 «Фиолетовые холмы» (референс 04_violet_hills.png).
 *
 * <p>Рев 16: луг — ЧИСТОЕ ПОЛЕ. Церкви и дорожка церковь→церковь убраны по просьбе владельца,
 * собственные «фиолетовые кусты»-деревья тоже: единственное дерево биома — динамическое
 * ({@code riftworlddt:lilac}, «лиловый хлыст») из аддона Dynamic Trees. Рельеф — ровное плато с
 * холмами, которые ставит шум, а не купол к центру. Остались река (хорда через остров) и поля.
 *
 * <p>Раскладка: дикий архипелаг делится на супер-ячейки 880 блоков; ~45 % подходящих (вне города,
 * адских земель и руин) держат один регион — остров-луг (kind 7) с вытянутым эллипсом и рекой.
 * Всё детерминировано сидом, без сохранённых данных.
 */
public final class VioletHills {
    public static final int SUPER = 880;
    private static final long SEED = 0x9B1C7L;

    /**
     * Рев 16: ЦЕРКВИ УБРАНЫ по просьбе владельца («убери церкви пока в фиолетовых холмах»).
     * Вместе с ними ушли и дорожка церковь→церковь, и ступени-площадки под храмами: луг стал
     * чистым полем, как на референсе 04.
     */
    public static final class Region {
        public final int cx, cz;
        public final double Rr;
        public final CityPlan.Island meadow;
        // winding country road: a gently S-curving chord across the valley between rolling hills
        public final double rax, raz, rbx, rbz, curvePhase;
        public final int riverLevel;
        public static final double RIVER_HW = 1.65;

        Region(int cx, int cz, Random rnd) {
            this.cx = cx; this.cz = cz;
            this.Rr = 150 + rnd.nextDouble() * 40;
            double R = 96 + rnd.nextDouble() * 20;
            double base = CityPlan.get().ocean + 16 + rnd.nextInt(5);
            this.meadow = new CityPlan.Island(cx, cz, R, base, R * 0.6 + 12, rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283,
                    rnd.nextDouble() * 6.283, 7, 0.9);
            this.meadow.elong = 0.22 + rnd.nextDouble() * 0.20;
            this.meadow.elongAng = rnd.nextDouble() * Math.PI;
            double phi = this.meadow.elongAng + (rnd.nextDouble() - 0.5) * 0.45;
            double rl = R * 1.15;
            double rcx = cx + (rnd.nextDouble() - 0.5) * R * 0.16, rcz = cz + (rnd.nextDouble() - 0.5) * R * 0.16;
            this.rax = rcx - Math.cos(phi) * rl; this.raz = rcz - Math.sin(phi) * rl;
            this.rbx = rcx + Math.cos(phi) * rl; this.rbz = rcz + Math.sin(phi) * rl;
            this.curvePhase = rnd.nextDouble() * Math.PI * 2.0;
            this.riverLevel = (int) Math.floor(base) - 2;
        }

        /** Signed/unsigned distance to the gently winding country lane across the meadow. */
        public double roadDist(double px, double pz) {
            double dx = rbx - rax, dz = rbz - raz;
            double len = Math.max(1.0, Math.hypot(dx, dz));
            double ux = dx / len, uz = dz / len;
            double nx = -uz, nz = ux;
            double relX = px - rax, relZ = pz - raz;
            double along = Math.max(0.0, Math.min(len, relX * ux + relZ * uz));
            double sway = 13.0 * Math.sin(along * 0.045 + curvePhase)
                    + 5.0 * (ElisarChunkGenerator.vnoise(px * 0.016, pz * 0.016, 173) - 0.5);
            double cx0 = rax + ux * along + nx * sway;
            double cz0 = raz + uz * along + nz * sway;
            return Math.hypot(px - cx0, pz - cz0);
        }

        /** Point (x, z) directly on the winding lane at normalized parameter u in [0..1]. */
        public double[] roadPoint(double u) {
            double dx = rbx - rax, dz = rbz - raz;
            double len = Math.max(1.0, Math.hypot(dx, dz));
            double ux = dx / len, uz = dz / len;
            double nx = -uz, nz = ux;
            double along = Math.max(0.0, Math.min(1.0, u)) * len;
            double bx = rax + ux * along, bz = raz + uz * along;
            double sway = 13.0 * Math.sin(along * 0.045 + curvePhase)
                    + 5.0 * (ElisarChunkGenerator.vnoise(bx * 0.016, bz * 0.016, 173) - 0.5);
            return new double[]{bx + nx * sway, bz + nz * sway};
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

    /**
     * Рев 16: собственных деревьев у биома больше НЕТ — обычное «дерево фиолетовых кустов»
     * удалено по просьбе владельца. Единственное дерево холмов — динамическое (аддон DT,
     * вид {@code riftworlddt:lilac}, «лиловый хлыст»).
     */
    public static List<CityPlan.Tree> treesNear(int x, int z) {
        return List.of();
    }
}

