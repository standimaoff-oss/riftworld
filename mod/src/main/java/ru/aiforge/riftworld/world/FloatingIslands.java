package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.Collections;

/**
 * Единый сборщик всех парящих островов Элисара в радиусе от заданной точки (после генерации чанков:
 * для полёта оригами-драконов вдоль/под островами и для 2D белых волн на воде под островами).
 */
public final class FloatingIslands {
    private FloatingIslands() {
    }

    /**
     * Возвращает все уникальные острова в радиусе {@code range} от {@code (x, z)} (включая главный архипелаг
     * города, дикие острова и архипелаги всех биомов).
     *
     * @param onlyHighFloating если {@code true}, возвращает только высоко парящие над морем острова
     *                         (исключая посаженные в воду острова Висячих садов и совсем крошечные чаши),
     *                         под каменным конусом которых есть просторный свод для полёта оригами-дракона.
     */
    public static List<CityPlan.Island> gather(double x, double z, double range, boolean onlyHighFloating) {
        CityPlan plan = CityPlan.get();
        int ocean = plan.ocean;
        Set<CityPlan.Island> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<CityPlan.Island> out = new ArrayList<>(24);

        for (CityPlan.Island isl : plan.islands) {
            addIfMatch(isl, x, z, range, ocean, onlyHighFloating, seen, out);
        }

        // Сетка точек вокруг (x, z) с шагом 140 блоков покрывает все соседние ячейки диких островов и биомов
        int steps = Math.max(1, (int) Math.ceil(range / 140.0));
        for (int gx = -steps; gx <= steps; ++gx) {
            for (int gz = -steps; gz <= steps; ++gz) {
                double sx = x + gx * 140.0;
                double sz = z + gz * 140.0;
                for (CityPlan.Island wi : WildIslands.near(sx, sz)) {
                    addIfMatch(wi, x, z, range, ocean, onlyHighFloating, seen, out);
                }
                CrystalRifts.Region cr = CrystalRifts.regionAt(sx, sz);
                if (cr != null) {
                    for (CityPlan.Island ci : cr.isles) {
                        addIfMatch(ci, x, z, range, ocean, onlyHighFloating, seen, out);
                    }
                }
                VioletHills.Region vh = VioletHills.regionAt(sx, sz);
                if (vh != null) {
                    for (CityPlan.Island vi : vh.isles) {
                        addIfMatch(vi, x, z, range, ocean, onlyHighFloating, seen, out);
                    }
                }
                SizoGrove.Region sg = SizoGrove.regionAt(sx, sz);
                if (sg != null && sg.hub != null) {
                    addIfMatch(sg.hub, x, z, range, ocean, onlyHighFloating, seen, out);
                }
                AshenLands.Region al = AshenLands.regionAt(sx, sz);
                if (al != null && al.island != null) {
                    addIfMatch(al.island, x, z, range, ocean, onlyHighFloating, seen, out);
                }
                if (!onlyHighFloating) {
                    WaterfallGardens.Region wg = WaterfallGardens.regionAt(sx, sz);
                    if (wg != null) {
                        for (CityPlan.Island gi : wg.isles) {
                            addIfMatch(gi, x, z, range, ocean, false, seen, out);
                        }
                    }
                }
                EmberRuins.Site er = EmberRuins.siteAt(sx, sz);
                if (er != null && er.island != null) {
                    addIfMatch(er.island, x, z, range, ocean, onlyHighFloating, seen, out);
                }
                WorldStructures.Village vil = WorldStructures.anyVillageAt(sx, sz, 90.0);
                if (vil != null && vil.island != null) {
                    addIfMatch(vil.island, x, z, range, ocean, onlyHighFloating, seen, out);
                }
            }
        }
        return out;
    }

    private static void addIfMatch(CityPlan.Island isl, double x, double z, double range, int ocean,
                                   boolean onlyHighFloating, Set<CityPlan.Island> seen, List<CityPlan.Island> out) {
        if (isl == null || !seen.add(isl)) return;
        double reach = isl.maxReach();
        if (Math.hypot(isl.cx - x, isl.cz - z) > range + reach) return;
        if (onlyHighFloating) {
            if (isl.kind == WaterfallGardens.KIND) return;
            if (isl.base < ocean + 15.0 || isl.R < 15.0) return;
        }
        out.add(isl);
    }

    /** Оценочная высота каменного свода под островом в точке (x, z) (для плавного полёта под островом). */
    public static double approxUndersideY(CityPlan.Island isl, double x, double z, int ocean) {
        double t = isl.t(x, z);
        if (t <= 0.0) {
            return isl.base;
        }
        double cone = 3.5 + isl.depth * Math.pow(t, 0.54) * 0.92;
        return Math.max(ocean + 6.0, isl.base - cone);
    }
}
