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
        // river: a chord across the island that spills off both rims
        public final double rax, raz, rbx, rbz;
        public final int riverLevel;
        public static final double RIVER_HW = 1.6;

        Region(int cx, int cz, Random rnd) {
            this.cx = cx; this.cz = cz;
            this.Rr = 150 + rnd.nextDouble() * 40;
            double R = 92 + rnd.nextDouble() * 20;
            // Рев 16: луг НЕ КУПОЛ. Раньше высота росла к центру (`rimY + (base-rimY)*t`), из-за
            // чего любой кадр смотрел в «горку». Теперь база — ровное плато чуть выше моря, а
            // «холмистость» даёт шум рельефа (см. violetProfile): холмы стоят ГДЕ ХОЧЕТ ШУМ, а не
            // в центре острова.
            double base = CityPlan.get().ocean + 17 + rnd.nextInt(6);
            this.meadow = new CityPlan.Island(cx, cz, R, base, R * 0.6 + 12, rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283,
                    rnd.nextDouble() * 6.283, 7, 0.9);
            // луг ВЫТЯНУТЫЙ — гряды лаванды идут длинными лентами через склон, как на референсе 04
            this.meadow.elong = 0.22 + rnd.nextDouble() * 0.22;
            this.meadow.elongAng = rnd.nextDouble() * Math.PI;
            // river: a chord across the island, spilling off both rims
            double phi = rnd.nextDouble() * Math.PI;
            double rl = R * 1.3;
            double rcx = cx + (rnd.nextDouble() - 0.5) * R * 0.3, rcz = cz + (rnd.nextDouble() - 0.5) * R * 0.3;
            this.rax = rcx - Math.cos(phi) * rl; this.raz = rcz - Math.sin(phi) * rl;
            this.rbx = rcx + Math.cos(phi) * rl; this.rbz = rcz + Math.sin(phi) * rl;
            this.riverLevel = (int) Math.floor(base) - 3;
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

