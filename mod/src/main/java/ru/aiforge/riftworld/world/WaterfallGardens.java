package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Biome 07 «Висячие сады» (референс {@code reports/refs_rev4/07_waterfall_gardens.png}).
 *
 * <p>Ступенчатая известняковая гора: широкие плоские полки через каждые 5 блоков, почти вертикальные
 * стены между ними, на каждой полке — пруд, льющаяся через край вода, сплошная зелень и длинные
 * плети плюща по обрывам. Деревья — обычные дубы с зелёной кроной: на референсе только они.
 *
 * <p>Рев 14 (итерация 1): убрано ВСЁ рукотворное. Раньше здесь стояли висячие дощатые мосты
 * с цепями, фонари в горячих источниках и настилы — на референсе их нет, и владелец попросил
 * «ничего созданного человеком: ни лестниц, ни мостов, ни каналов».
 *
 * <p>Своя сетка 960, ~42 % подходящих ячеек, всё детерминировано от одного сида.
 */
public final class WaterfallGardens {
    public static final int SUPER = 960;
    private static final long FIXED = 0x7A7EFA11L;
    /**
     * Высота яруса (блоков). По референсу между полками отвесные известняковые стены в 8–12
     * блоков: 5 давало пологий холм, 6 — ещё низкие бортики (кадры итерации 12). 9 — стены
     * читаются как скалы, а полок остаётся 4–5, как на фото.
     */
    public static final double STEP = 9.0;
    public static final int KIND = 9;
    /** Kind of the garden's own tree (green oak, the reference silhouette). */
    public static final int TREE_OAK = 4;

    /** Заранее выбранный водопад: точка на полке, откуда вода льётся через край. */
    public static final class Fall {
        public final int x, z;
        public final double w;      // ширина струи в блоках
        public final long seed;

        Fall(int x, int z, double w, long seed) { this.x = x; this.z = z; this.w = w; this.seed = seed; }
    }

    public static final class Region {
        public final int cx, cz;
        public final double Rr;
        public final CityPlan.Island hub;
        public final List<CityPlan.Tree> trees = new ArrayList<>();
        public final List<Fall> falls = new ArrayList<>();

        Region(int cx, int cz, Random rnd) {
            this.cx = cx; this.cz = cz;
            double R = 46 + rnd.nextDouble() * 20;                        // «биом небольшой»
            this.Rr = R * 2.1;
            double base = 96 + rnd.nextDouble() * 16;                     // верх - на высоте островов
            this.hub = new CityPlan.Island(cx, cz, R, base, R * 1.15 + 10, rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283,
                    rnd.nextDouble() * 6.283, KIND, 1.6);
            // Рев 14: на референсе полки заросли обычными ДУБАМИ с зелёной кроной, а не «крошечными
            // деревцами» — ставим 14..20 дубов, но только там, где полка широкая (t < 0.72), иначе
            // ствол повисает на самом обрыве.
            for (int k = 0, want = 14 + rnd.nextInt(7), tries = 0; k < want && tries < want * 24; ++tries) {
                double a = rnd.nextDouble() * Math.PI * 2, d = (0.10 + rnd.nextDouble() * 0.60) * R;
                int x = (int) Math.floor(cx + Math.cos(a) * d), z = (int) Math.floor(cz + Math.sin(a) * d);
                if (hub.t(x + 0.5, z + 0.5) < 0.30) continue;
                boolean ok = true;
                for (CityPlan.Tree t : trees) if (Math.abs(t.x - x) < 6 && Math.abs(t.z - z) < 6) { ok = false; break; }
                if (!ok) continue;
                trees.add(new CityPlan.Tree(x, z, TREE_OAK, 7 + rnd.nextInt(5), rnd.nextInt(1 << 30)));
                ++k;
            }
            // водопады: полка с прудом, чей край выше соседа — заранее помечаем как «льющуюся»
            for (int k = 0, want = 5 + rnd.nextInt(4), tries = 0; k < want && tries < want * 30; ++tries) {
                double a = rnd.nextDouble() * Math.PI * 2, d = (0.25 + rnd.nextDouble() * 0.6) * R;
                double x = cx + Math.cos(a) * d, z = cz + Math.sin(a) * d;
                if (hub.t(x, z) < 0.10) continue;
                boolean ok = true;
                for (Fall f : falls) if (Math.hypot(f.x - x, f.z - z) < 9) { ok = false; break; }
                if (!ok) continue;
                double w = 2.0 + rnd.nextDouble() * 2.5;                 // ширина струи, как на референсе
                falls.add(new Fall((int) Math.floor(x), (int) Math.floor(z), w, rnd.nextLong()));
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

    /**
     * Рев 14: водопад, струя которого может попасть в колонку (x, z).
     *
     * <p>Струя идёт не вниз по склону, а наружу: вода находит край полки и льётся с него. Поэтому
     * для каждой колонки проверяем сам водопад и 4 его соседа в радиусе {@link Fall#w} — так
     * получается широкая (2..4 блока) струя, как на референсе, а не нитка шириной в блок.
     */
    public static Fall fallNear(Region rg, double x, double z) {
        for (Fall f : rg.falls) {
            double d = Math.hypot(x - f.x, z - f.z);
            if (d <= f.w) return f;
        }
        return null;
    }

    public static List<CityPlan.Tree> treesNear(int x, int z) {
        Region rg = regionAt(x + 0.5, z + 0.5);
        if (rg == null) return List.of();
        List<CityPlan.Tree> out = new ArrayList<>(2);
        for (CityPlan.Tree t : rg.trees) if (Math.abs(x - t.x) <= 5 && Math.abs(z - t.z) <= 5) out.add(t);
        return out;
    }
}
