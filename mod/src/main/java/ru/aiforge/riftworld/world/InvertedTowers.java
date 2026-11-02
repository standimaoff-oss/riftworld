package ru.aiforge.riftworld.world;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

/**
 * v0.26.0: Летающие Перевёрнутые Башни Элисара ({@code riftworld:inverted_tower}):
 * <ul>
 *   <li>Сохраняет историческую городскую перевёрнутую башню у западного края Белого Города ({@code -104, 148, 22});</li>
 *   <li>Рандомно и детерминированно спавнит перевёрнутые летающие башни по всему миру Элисара в открытом небе над океаном;</li>
 *   <li>На вершине каждой дикой летающей башни ({@link Site#roostY()}) расположена древняя каменная площадка-гнездовье,
 *       куда дикие неприручённые Бумажные Драконы прилетают отдохнуть, свернувшись кольцом!</li>
 * </ul>
 */
public final class InvertedTowers {
    public static final int SUPER = 620;
    private static final long FIXED = 0x51A7E2D94C113B77L;

    public static final Site CITY_TOWER = new Site(-104, 148, 22, true);

    public record Site(int cx, int y0, int cz, boolean cityOrigin) {
        /** Высота плоской верхней короны-гнездовья, на которую садится отдыхать дикий Бумажный Дракон. */
        public int roostY() {
            return y0 + 87;
        }
    }

    private static final ConcurrentHashMap<Long, Optional<Site>> CACHE = new ConcurrentHashMap<>();

    private InvertedTowers() {
    }

    @Nullable
    public static Site siteInCell(int sx, int sz) {
        return CACHE.computeIfAbsent(WildIslands.key(sx, sz), k -> Optional.ofNullable(gen(sx, sz))).orElse(null);
    }

    @Nullable
    private static Site gen(int sx, int sz) {
        if (sx == 0 && sz == 0) {
            return null; // В центральной ячейке уже стоит историческая башня CITY_TOWER (-104, 148, 22)
        }
        long seed = FIXED ^ (sx * 0x9E3779B97F4A7C15L + sz * 0xC2B2AE3D27D4EB4FL);
        Random r = new Random(seed);

        boolean pinned = (sx == 1 && sz == -1) || (sx == -1 && sz == -1);
        if (!pinned && r.nextDouble() > 0.62) {
            return null;
        }

        CityPlan plan = CityPlan.get();
        int margin = 95;
        for (int attempt = 0; attempt < 18; ++attempt) {
            int cx = sx * SUPER + margin + r.nextInt(SUPER - 2 * margin);
            int cz = sz * SUPER + margin + r.nextInt(SUPER - 2 * margin);

            if (Math.hypot(cx, cz) < 420.0) continue;
            if (Math.hypot(cx - 648.0, cz - 693.0) < 220.0) continue; // не задеваем морские пробы spots.json

            if (AshenLands.blocks(cx, cz, 48.0)) continue;
            if (BlacktideCitadel.insideAnyFootprint(cx, cz, 96.0)) continue;
            if (VioletHills.nearAny(cx, cz, 12.0, 48.0)) continue;
            SizoGrove.Region sr = SizoGrove.regionAt(cx, cz);
            if (sr != null && Math.hypot(cx - sr.hub.cx, cz - sr.hub.cz) < sr.hub.maxReach() + 48.0) continue;
            if (WaterfallGardens.nearAny(cx, cz, 12.0, 56.0)) continue;
            if (CrystalRifts.nearAny(cx, cz, 12.0, 48.0)) continue;
            MistKeep.Region mk = MistKeep.regionAt(cx, cz);
            if (mk != null && Math.hypot(cx - mk.cx, cz - mk.cz) < mk.Rr + 48.0) continue;

            // Проверяем, что башня парит в чистом небе между дикими островами (не врезаясь в сушу!)
            boolean hitIsland = false;
            int icx0 = Math.floorDiv(cx, WildIslands.CELL), icz0 = Math.floorDiv(cz, WildIslands.CELL);
            outerIsl:
            for (int dx = -1; dx <= 1; ++dx) {
                for (int dz = -1; dz <= 1; ++dz) {
                    for (CityPlan.Island isl : WildIslands.cell(icx0 + dx, icz0 + dz).islands) {
                        if (Math.hypot(cx - isl.cx, cz - isl.cz) < isl.maxReach() + 26.0) {
                            hitIsland = true;
                            break outerIsl;
                        }
                    }
                }
            }
            if (hitIsland) continue;

            int y0 = 118 + r.nextInt(24); // остриё висит над морем (118..141), гнездовье дракона на вершине 205..228!
            return new Site(cx, y0, cz, false);
        }
        return null;
    }

    /** Возвращает башню (городскую или дикую), чья зона обломков/корпуса покрывает точку {@code (x, z)} в радиусе {@code maxDist}. */
    @Nullable
    public static Site nearAt(double x, double z, double maxDist) {
        if (Math.hypot(x - CITY_TOWER.cx, z - CITY_TOWER.cz) <= maxDist) {
            return CITY_TOWER;
        }
        int sx0 = Math.floorDiv((int) Math.floor(x), SUPER);
        int sz0 = Math.floorDiv((int) Math.floor(z), SUPER);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                Site s = siteInCell(sx0 + dx, sz0 + dz);
                if (s != null && Math.hypot(x - s.cx, z - s.cz) <= maxDist) {
                    return s;
                }
            }
        }
        return null;
    }

    /** Ближайшая Перевёрнутая башня (включая городскую, если {@code allowCity == true}, либо только дикая для посадки драконов!). */
    @Nullable
    public static Site nearest(double x, double z, boolean allowCity, int maxCellRadius) {
        Site best = null;
        double bestD2 = Double.MAX_VALUE;
        if (allowCity) {
            double dx = CITY_TOWER.cx - x, dz = CITY_TOWER.cz - z;
            bestD2 = dx * dx + dz * dz;
            best = CITY_TOWER;
        }
        int sx0 = Math.floorDiv((int) Math.floor(x), SUPER);
        int sz0 = Math.floorDiv((int) Math.floor(z), SUPER);
        for (int r = 0; r <= maxCellRadius; ++r) {
            for (int dx = -r; dx <= r; ++dx) {
                for (int dz = -r; dz <= r; ++dz) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    Site s = siteInCell(sx0 + dx, sz0 + dz);
                    if (s == null) continue;
                    double ddx = s.cx - x, ddz = s.cz - z;
                    double d2 = ddx * ddx + ddz * ddz;
                    if (d2 < bestD2) {
                        bestD2 = d2;
                        best = s;
                    }
                }
            }
            if (best != null && !best.cityOrigin && r >= 1) break;
        }
        return best;
    }
}
