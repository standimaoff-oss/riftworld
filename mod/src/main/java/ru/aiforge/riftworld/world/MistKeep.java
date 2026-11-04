package ru.aiforge.riftworld.world;

import java.util.Random;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Мини-биом «Туманный данж» (рев 5, по референсу): редкие одинокие скопы серо-зелёных каменных столбов,
 * торчащих из моря, в плотной дымке, а посередине - почти сломанная готическая руина-ротонда с обрушенной
 * винтовой лестницей. Без собственных островов: ландшафт - открытое море, скалы стоят на океанском ложе.
 * Сетка 720, ~3 из 10 секторов, детерминировано; дикие острова держатся подальше.
 */
public final class MistKeep {
    public static final int SUPER = 980;
    private static final long FIXED = 0x5F0A11EDL;

    /** Одна скопа: центр, радиус, семя её руины. */
    public static final class Region {
        public final int cx, cz, Rr;
        public final long seed;

        Region(int cx, int cz, int Rr, long seed) {
            this.cx = cx; this.cz = cz; this.Rr = Rr; this.seed = seed;
        }
    }

    private static final ConcurrentHashMap<Long, Optional<Region>> CACHE = new ConcurrentHashMap<>();

    private MistKeep() {
    }

    public static Region region(int sx, int sz) {
        return CACHE.computeIfAbsent(WildIslands.key(sx, sz), k -> Optional.ofNullable(gen(sx, sz))).orElse(null);
    }

    private static Region gen(int sx, int sz) {
        Random r = new Random(FIXED ^ (sx * 53982894593057L + sz * 317074912223L) * 0x9E3779B97F4A7C15L);
        if (r.nextDouble() > 0.42) return null;
        for (int attempt = 0; attempt < 8; ++attempt) {
            int cx = sx * SUPER + 210 + r.nextInt(SUPER - 420), cz = sz * SUPER + 210 + r.nextInt(SUPER - 420);
            int Rr = 84 + r.nextInt(32); // v0.23.0: увеличен в ~2 раза по радиусу (~4x по площади)!
            if (Math.hypot(cx, cz) < 760) continue;
            if (Math.hypot(cx - 648, cz - 693) < Rr + 280) continue;
            if (AshenLands.blocks(cx, cz, Rr + 220)) continue;
            EmberRuins.Site es = EmberRuins.nearest(cx, cz);
            if (es != null && Math.hypot(cx - es.cx, cz - es.cz) < es.island.R + Rr + 260) continue;
            boolean ok = true;
            for (int dx = -1; dx <= 1 && ok; ++dx)
                for (int dz = -1; dz <= 1 && ok; ++dz) {
                    WaterfallGardens.Region gr = WaterfallGardens.region(Math.floorDiv(cx, WaterfallGardens.SUPER) + dx, Math.floorDiv(cz, WaterfallGardens.SUPER) + dz);
                    if (gr != null && Math.hypot(cx - gr.cx, cz - gr.cz) < gr.Rr + Rr + 180) ok = false;
                    SizoGrove.Region sr = SizoGrove.region(Math.floorDiv(cx, SizoGrove.SUPER) + dx, Math.floorDiv(cz, SizoGrove.SUPER) + dz);
                    if (sr != null && Math.hypot(cx - sr.cx, cz - sr.cz) < sr.Rr + Rr + 180) ok = false;
                    VioletHills.Region vr = VioletHills.region(Math.floorDiv(cx, VioletHills.SUPER) + dx, Math.floorDiv(cz, VioletHills.SUPER) + dz);
                    if (vr != null && Math.hypot(cx - vr.cx, cz - vr.cz) < vr.Rr + Rr + 180) ok = false;
                    CrystalRifts.Region cr = CrystalRifts.region(Math.floorDiv(cx, CrystalRifts.SUPER) + dx, Math.floorDiv(cz, CrystalRifts.SUPER) + dz);
                    if (cr != null && Math.hypot(cx - cr.cx, cz - cr.cz) < cr.Rr + Rr + 180) ok = false;
                }
            if (!ok) continue;
            return new Region(cx, cz, Rr, FIXED ^ ((long) cx * 1000003L + (long) cz * 7867L));
        }
        return null;
    }

    public static Region regionAt(double x, double z) {
        int sx = Math.floorDiv((int) Math.floor(x), SUPER), sz = Math.floorDiv((int) Math.floor(z), SUPER);
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) {
                Region rg = region(sx + dx, sz + dz);
                if (rg != null && Math.abs(x - rg.cx) < rg.Rr + 110 && Math.abs(z - rg.cz) < rg.Rr + 110) return rg;
            }
        return null;
    }

    public static boolean inside(double x, double z) {
        Region rg = regionAt(x, z);
        return rg != null && Math.hypot(x - rg.cx, z - rg.cz) < rg.Rr + 14;
    }

    /** Высота скалы в столбце (центральный утёс + кольцо столбов), либо -1 если скалы нет. */
    public static int rockTop(int ocean, double x, double z, Region rg) {
        double dx = x - rg.cx, dz = z - rg.cz;
        double d = Math.hypot(dx, dz);
        // центральный утёс: плоская скала r~11 пол НАД водой (на нём стоит руина; верх = ocean+3)
        if (d <= 11.5) {
            double j = ElisarChunkGenerator.vnoise(x * 0.3, z * 0.3, 881) - 0.5;          // лёгкий скол края
            if (d > 10.2 + j) return -1;
            return ocean + 3;
        }
        // два концентрических кольца высоких зубчатых скальных столбов в густом морском тумане (v0.23.0)!
        double wob = (ElisarChunkGenerator.vnoise(x * 0.05, z * 0.05, 882) - 0.5) * 12.0;
        double ring1 = Math.abs(d - (rg.Rr * 0.42 + wob));
        double ring2 = Math.abs(d - (rg.Rr * 0.78 - wob * 0.8));
        double ring = Math.min(ring1, ring2);
        if (d < 16 || d > rg.Rr + 10 || ring > 8.5) return -1;
        double cell = ElisarChunkGenerator.vnoise(x * 0.14, z * 0.14, 883);
        if (cell < 0.57) return -1;
        double hgt = ElisarChunkGenerator.vnoise(x * 0.21, z * 0.21, 884);
        int rise = 2 + (int) Math.round((cell - 0.57) * (8 + hgt * 18));
        return Math.min(ocean + 18, ocean - 1 + rise);
    }
}
