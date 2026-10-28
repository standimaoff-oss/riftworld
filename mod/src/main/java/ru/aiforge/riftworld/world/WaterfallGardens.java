package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Biome 07 «Висячие сады» (референс {@code reports/refs_rev4/07_waterfall_gardens.png}).
 *
 * <p>Ступенчатые известняковые острова: широкие плоские полки через каждые 9 блоков, почти
 * вертикальные стены между ними, на каждой полке — пруд, льющаяся через край вода, сплошная зелень
 * и длинные плети плюща по обрывам. Деревья — обычные дубы с зелёной кроной: на референсе только они.
 *
 * <p>Рев 14 (итерация 1): убрано ВСЁ рукотворное — ни лестниц, ни мостов, ни настилов.
 *
 * <p>Рев 14.26 (по замечаниям владельца):
 * <ul>
 *   <li><b>Не один остров, а архипелаг 3–6 островов.</b> Иногда это пять-шесть КРУПНЫХ островов
 *       (большой биом), иногда три-четыре маленьких.</li>
 *   <li><b>Биом = суша.</b> {@link #inside} возвращает true только там, где реально стоит остров
 *       садов: раньше биом накрывал область в 2× больше острова и висел над открытым морем,
 *       поэтому {@code /locate biome riftworld:waterfall_gardens} приводил в воду и камни.</li>
 *   <li><b>Острова расставлены кольцом с протокой 7–14 блоков</b> — между ними течёт река.</li>
 *   <li><b>Нижний ярус — на уровне моря</b> (см. {@code ElisarChunkGenerator.gardenProfile}),
 *       ниже него только каменное основание острова.</li>
 * </ul>
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
        /** Габарит региона: дальше него ни один остров архипелага не выходит. */
        public final double Rr;
        /** Большой биом (5–6 крупных островов) или маленький (3–4 мелких). */
        public final boolean big;
        /** Острова архипелага; {@link #hub} — первый из них (совместимость и камеры). */
        public final List<CityPlan.Island> isles = new ArrayList<>(6);
        public final CityPlan.Island hub;
        public final List<CityPlan.Tree> trees = new ArrayList<>();
        public final List<Fall> falls = new ArrayList<>();

        Region(int cx, int cz, Random rnd) {
            this.cx = cx;
            this.cz = cz;
            int ocean = CityPlan.get().ocean;
            this.big = rnd.nextDouble() < 0.5;
            // Рев 14.26: «3–6 островов, рандомно — иногда 5 крупных, иногда 3 маленьких».
            int n = big ? 4 + rnd.nextInt(3) : 3 + rnd.nextInt(2);
            double rMin = big ? 24 : 12, rMax = big ? 34 : 19;
            double[] rs = new double[n];
            double sum = 0;
            for (int i = 0; i < n; ++i) {
                rs[i] = rMin + rnd.nextDouble() * (rMax - rMin);
                sum += rs[i];
            }
            // Кольцо островов: радиус подбираем так, чтобы хорда между соседями была ровно
            // «два радиуса + протока». Тогда между кромками остаётся 7–14 блоков воды — это и есть
            // река между островами архипелага (владелец: «острова расставлены так, что между ними
            // образуется река»).
            double gap = 7 + rnd.nextDouble() * 6;
            double rbar = sum / n;
            double spread = (2 * rbar + gap) / (2 * Math.sin(Math.PI / n));
            double a0 = rnd.nextDouble() * Math.PI * 2;
            double extent = 0;
            for (int i = 0; i < n; ++i) {
                double a = a0 + i * Math.PI * 2 / n + (rnd.nextDouble() - 0.5) * 0.9 / n * Math.PI * 2;
                double d = spread * (0.92 + rnd.nextDouble() * 0.16);
                double x = cx + Math.cos(a) * d, z = cz + Math.sin(a) * d;
                double R = rs[i];
                // вершина: 3–5 ярусов у крупных островов, 2–4 у мелких
                int peak = ocean + (int) Math.round((big ? 3.4 : 2.4) * STEP + rnd.nextDouble() * STEP);
                CityPlan.Island isl = new CityPlan.Island(x, z, R, peak - 8, R * 1.15 + 10,
                        rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283, KIND, 1.6);
                isles.add(isl);
                extent = Math.max(extent, Math.hypot(x - cx, z - cz) + R * 1.2 + 4);
                // на референсе полки заросли обычными ДУБАМИ с зелёной кроной — по 5–16 на остров,
                // и только там, где полка широкая (t > 0.30), иначе ствол повисает на обрыве.
                // Рев 14.27: на референсе полки — ОТКРЫТЫЕ лужайки, а дубы стоят небольшой рощей
                // в одном месте. В итерации 26 деревьев было 9–16 на остров: они закрывали и полки,
                // и водопады (кадры 40–42 — сплошная крона). Теперь 3–7 дубов, собранных в рощу.
                int want = big ? 4 + rnd.nextInt(4) : 3 + rnd.nextInt(3);
                double ga = rnd.nextDouble() * Math.PI * 2, gd = (0.15 + rnd.nextDouble() * 0.35) * R;
                double gx = x + Math.cos(ga) * gd, gz = z + Math.sin(ga) * gd;
                for (int k = 0, tries = 0; k < want && tries < want * 30; ++tries) {
                    double ta = rnd.nextDouble() * Math.PI * 2, td = Math.sqrt(rnd.nextDouble()) * (big ? 13 : 8);
                    int tx = (int) Math.floor(gx + Math.cos(ta) * td), tz = (int) Math.floor(gz + Math.sin(ta) * td);
                    if (isl.t(tx + 0.5, tz + 0.5) < 0.32) continue;
                    boolean ok = true;
                    for (CityPlan.Tree t : trees) if (Math.abs(t.x - tx) < 6 && Math.abs(t.z - tz) < 6) { ok = false; break; }
                    if (!ok) continue;
                    trees.add(new CityPlan.Tree(tx, tz, TREE_OAK, 6 + rnd.nextInt(4), rnd.nextInt(1 << 30)));
                    ++k;
                }
                // водопады: по 1–2 струи на остров
                int fallWant = 1 + rnd.nextInt(2);
                for (int k = 0, tries = 0; k < fallWant && tries < fallWant * 30; ++tries) {
                    double fa = rnd.nextDouble() * Math.PI * 2, fd = (0.25 + rnd.nextDouble() * 0.6) * R;
                    double fx = x + Math.cos(fa) * fd, fz = z + Math.sin(fa) * fd;
                    if (isl.t(fx, fz) < 0.12) continue;
                    boolean ok = true;
                    for (Fall f : falls) if (Math.hypot(f.x - fx, f.z - fz) < 9) { ok = false; break; }
                    if (!ok) continue;
                    falls.add(new Fall((int) Math.floor(fx), (int) Math.floor(fz), 2.0 + rnd.nextDouble() * 2.5, rnd.nextLong()));
                    ++k;
                }
            }
            this.hub = isles.get(0);
            this.Rr = extent + 8;
        }

        /** Принадлежит ли остров этому архипелагу (замена старого сравнения {@code gr.hub == isl}). */
        public boolean owns(CityPlan.Island isl) {
            for (CityPlan.Island a : isles) if (a == isl) return true;
            return false;
        }

        /** Остров архипелага под точкой (или null, если точка над водой). */
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
        if (r.nextDouble() > 0.42) return null;
        int cx = sx * SUPER + 480 + 120 + r.nextInt(SUPER - 720), cz = sz * SUPER + 480 + 120 + r.nextInt(SUPER - 720);
        if (Math.hypot(cx, cz) < 640) return null;
        if (Math.hypot(cx - AshenLands.CX, cz - AshenLands.CZ) < AshenLands.R + 320) return null;
        EmberRuins.Site es = EmberRuins.nearest(cx, cz);
        if (es != null && Math.hypot(cx - es.cx, cz - es.cz) < 460) return null;
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
                if (rg != null && Math.abs(x - rg.cx) < rg.Rr && Math.abs(z - rg.cz) < rg.Rr) return rg;
            }
        return null;
    }

    /**
     * Рев 14.26: биом садов — это СУША. Раньше здесь был только большой круг вокруг центра
     * региона (в 2× больше самого острова), поэтому биом висел над открытым морем, а
     * {@code /locate biome riftworld:waterfall_gardens} приводил в воду и камни. Теперь проверка
     * идёт по форме островов архипелага: биом там, где реально стоят сады.
     */
    public static boolean inside(double x, double z) {
        Region rg = regionAt(x, z);
        return rg != null && rg.islandAt(x, z) != null;
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

    /** Не подходит ли точка (x, z) слишком близко к какому-нибудь острову архипелага (для соседних биомов). */
    public static boolean near(Region rg, double x, double z, double r, double pad) {
        for (CityPlan.Island gi : rg.isles)
            if (Math.hypot(x - gi.cx, z - gi.cz) < r + gi.R * 1.15 + pad) return true;
        return false;
    }

    public static List<CityPlan.Tree> treesNear(int x, int z) {
        Region rg = regionAt(x + 0.5, z + 0.5);
        if (rg == null) return List.of();
        List<CityPlan.Tree> out = new ArrayList<>(2);
        for (CityPlan.Tree t : rg.trees) if (Math.abs(x - t.x) <= 5 && Math.abs(z - t.z) <= 5) out.add(t);
        return out;
    }
}
