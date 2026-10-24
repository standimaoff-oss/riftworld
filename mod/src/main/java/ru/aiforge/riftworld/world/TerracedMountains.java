package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Биом «Многоуровневые горы» (rev 9): массив-«пирог» из горизонтальных ярусов, поднимающийся из моря
 * на 10-12 полок. Каждый ярус — ровная полка шириной 8-20 блоков, между ярусами отвесная стена
 * 7-9 блоков; со кромок полок срываются водопады, в нишах стен — жилы аурита, на полках — трава,
 * цветы, редкие маленькие деревья и пруды. Сверху массив венчает зелёное плато.
 *
 * <p>Всё детерминировано (фиксированный сид, без сохранённых данных). Сетка 1120 блоков, ~38 % ячеек.
 */
public final class TerracedMountains {
    public static final int SUPER = 1120;
    private static final long SEED = 0x7E44A19L;
    /** Тип острова в CityPlan (отдельный от остальных биомов, чтобы не путаться с «Висячими садами»). */
    public static final int KIND = 11;
    /** Высота одного яруса. */
    public static final double STEP = 9.0;
    /** Ширина «обрыва» внутри яруса: 2.2 блока высоты при уклоне ~1 — почти отвесная стена. */
    private static final double RISE = 2.2;
    /** Высота прибоя: ярусы считаются от уровня моря. */
    private static final int SEA_OFFSET = 2;

    public static final class Region {
        public final int cx, cz;
        public final double Rr;
        public final CityPlan.Island massif;
        public final List<CityPlan.Tree> trees = new ArrayList<>();

        Region(int cx, int cz, Random rnd) {
            this.cx = cx;
            this.cz = cz;
            double R = 104 + rnd.nextDouble() * 36;                       // широкий массив
            this.Rr = R * 1.75;
            double base = 132 + rnd.nextDouble() * 22;                    // вершина выше островов (base 98..112)
            this.massif = new CityPlan.Island(cx, cz, R, base, R * 1.1 + 12,
                    rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283, KIND, 1.0);
            // деревья только на полках: считаем уклон по нашей же функции высоты
            for (int k = 0, want = 26 + rnd.nextInt(14), tries = 0; k < want && tries < want * 26; ++tries) {
                double a = rnd.nextDouble() * Math.PI * 2, d = (0.12 + rnd.nextDouble() * 0.74) * R;
                int x = (int) Math.floor(cx + Math.cos(a) * d), z = (int) Math.floor(cz + Math.sin(a) * d);
                if (massif.t(x + 0.5, z + 0.5) < 0.22) continue;
                double h0 = top(this, x + 0.5, z + 0.5);
                double hx = top(this, x + 1.5, z + 0.5), hz = top(this, x + 0.5, z + 1.5);
                if (Math.abs(h0 - hx) > 1.0 || Math.abs(h0 - hz) > 1.0) continue;      // не на кромке обрыва
                if (pondAt(this, x + 0.5, z + 0.5, h0)) continue;
                boolean ok = true;
                for (CityPlan.Tree t : trees) if (Math.abs(t.x - x) < 6 && Math.abs(t.z - z) < 6) { ok = false; break; }
                if (!ok) continue;
                int kind = rnd.nextDouble() < 0.62 ? 0 : (rnd.nextDouble() < 0.75 ? 1 : 2);
                trees.add(new CityPlan.Tree(x, z, kind, kind == 2 ? 4 + rnd.nextInt(2) : 5 + rnd.nextInt(4), rnd.nextInt(1 << 30)));
                ++k;
            }
        }
    }

    private static final ConcurrentHashMap<Long, Optional<Region>> CACHE = new ConcurrentHashMap<>();

    private TerracedMountains() {
    }

    public static Region region(int sx, int sz) {
        return CACHE.computeIfAbsent(WildIslands.key(sx, sz), k -> Optional.ofNullable(gen(sx, sz))).orElse(null);
    }

    private static Region gen(int sx, int sz) {
        Random r = new Random(SEED ^ (sx * 53982894593057L + sz * 317074912223L) * 0x9E3779B97F4A7C15L);
        if (r.nextDouble() > 0.38) return null;
        int cx = sx * SUPER + 200 + r.nextInt(SUPER - 400), cz = sz * SUPER + 200 + r.nextInt(SUPER - 400);
        if (Math.hypot(cx, cz) < 640) return null;                                              // кольцо города
        if (Math.hypot(cx - AshenLands.CX, cz - AshenLands.CZ) < AshenLands.R + 300) return null;
        EmberRuins.Site es = EmberRuins.nearest(cx, cz);
        if (es != null && Math.hypot(cx - es.cx, cz - es.cz) < 460) return null;
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) {
                SizoGrove.Region sr = SizoGrove.region(Math.floorDiv(cx, SizoGrove.SUPER) + dx, Math.floorDiv(cz, SizoGrove.SUPER) + dz);
                if (sr != null && Math.hypot(cx - sr.cx, cz - sr.cz) < sr.Rr + 240) return null;
                VioletHills.Region vr = VioletHills.region(Math.floorDiv(cx, VioletHills.SUPER) + dx, Math.floorDiv(cz, VioletHills.SUPER) + dz);
                if (vr != null && Math.hypot(cx - vr.cx, cz - vr.cz) < vr.Rr + 240) return null;
                WaterfallGardens.Region wr = WaterfallGardens.region(Math.floorDiv(cx, WaterfallGardens.SUPER) + dx, Math.floorDiv(cz, WaterfallGardens.SUPER) + dz);
                if (wr != null && Math.hypot(cx - wr.cx, cz - wr.cz) < wr.Rr + 240) return null;
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

    /** Пятно биома (неровная кромка) — для эха в ElisarBiomeSource. */
    public static boolean inside(double x, double z) {
        Region rg = regionAt(x, z);
        if (rg == null) return false;
        double d = Math.hypot(x - rg.cx, z - rg.cz)
                + (ElisarChunkGenerator.vnoise(x * 0.026, z * 0.026, 705) - 0.5) * 52;
        return d < rg.Rr;
    }

    /** Высота массива: конус с крутыми боками, изрезанный ступенями ярусов. */
    public static double top(Region rg, double x, double z) {
        CityPlan.Island m = rg.massif;
        int ocean = CityPlan.get().ocean;
        double t = m.t(x, z);
        if (t <= 0) return ocean;
        double rise = Math.max(24.0, m.base - (ocean + SEA_OFFSET));
        double s = ElisarChunkGenerator.smooth(Math.min(1.0, t * 1.14));
        double h = ocean + SEA_OFFSET + Math.pow(s, 0.62) * rise;
        // неровный верх: чем выше, тем заметнее гребни и ложбины
        double n = ElisarChunkGenerator.vnoise(x * 0.016, z * 0.016, 701) * 0.62
                + ElisarChunkGenerator.vnoise(x * 0.043, z * 0.043, 702) * 0.38;
        h += (n - 0.5) * 15.0 * (0.30 + 0.70 * s);
        // ярусы: полка -> отвесный подъём -> следующая полка
        double rel = h - (ocean + SEA_OFFSET);
        double lvl = Math.floor(rel / STEP);
        double band = rel - lvl * STEP;
        double f = ElisarChunkGenerator.smooth((band - (STEP - RISE)) / RISE);
        return ocean + SEA_OFFSET + (lvl + f) * STEP;
    }

    /** Пруд на полке: плоские места, где шум перевалил порог. */
    public static boolean pondAt(Region rg, double x, double z, double top) {
        if (top <= CityPlan.get().ocean + STEP * 1.5) return false;
        return ElisarChunkGenerator.vnoise(x * 0.055, z * 0.055, 707) > 0.735;
    }

    public static List<CityPlan.Tree> treesNear(int x, int z) {
        Region rg = regionAt(x + 0.5, z + 0.5);
        if (rg == null) return List.of();
        List<CityPlan.Tree> out = new ArrayList<>(2);
        for (CityPlan.Tree t : rg.trees) if (Math.abs(x - t.x) <= 4 && Math.abs(z - t.z) <= 4) out.add(t);
        return out;
    }
}
