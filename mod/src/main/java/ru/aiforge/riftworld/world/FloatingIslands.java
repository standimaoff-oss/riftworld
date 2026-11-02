package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * v0.26.0: Полный аналитический сборщик ВСЕХ островов Элисара в радиусе от заданной точки
 * (без пропусков ячеек — охватывает 100% крупных, средних, малых островов и островков-чаш,
 * а также острова всех биомов, деревень, театра и морских бухт).
 */
public final class FloatingIslands {
    private FloatingIslands() {
    }

    /**
     * Возвращает все уникальные острова в радиусе {@code range} от {@code (x, z)}.
     *
     * @param onlyHighFloating если {@code true}, возвращает только высоко парящие над морем острова
     *                         для полёта оригами-дракона под сводами; если {@code false} — абсолютно ВСЕ острова
     *                         для непрерывного водного следа {@link ru.aiforge.riftworld.client.OceanSurfaceWaves}.
     */
    public static List<CityPlan.Island> gather(double x, double z, double range, boolean onlyHighFloating) {
        CityPlan plan = CityPlan.get();
        int ocean = plan.ocean;
        Set<CityPlan.Island> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<CityPlan.Island> out = new ArrayList<>(64);

        // 1. Все острова главного архипелага Белого города
        for (CityPlan.Island isl : plan.islands) {
            addIfMatch(isl, x, z, range, ocean, onlyHighFloating, seen, out);
        }

        double pad = 128.0;
        int minX = (int) Math.floor(x - range - pad);
        int maxX = (int) Math.ceil(x + range + pad);
        int minZ = (int) Math.floor(z - range - pad);
        int maxZ = (int) Math.ceil(z + range + pad);

        // 2. Прямой обход ВСЕХ ячеек диких островов (160x160) — 0 пропущенных средних/малых островов и чаш!
        int wMinX = Math.floorDiv(minX, WildIslands.CELL), wMaxX = Math.floorDiv(maxX, WildIslands.CELL);
        int wMinZ = Math.floorDiv(minZ, WildIslands.CELL), wMaxZ = Math.floorDiv(maxZ, WildIslands.CELL);
        for (int cx = wMinX; cx <= wMaxX; ++cx) {
            for (int cz = wMinZ; cz <= wMaxZ; ++cz) {
                for (CityPlan.Island wi : WildIslands.cell(cx, cz).islands) {
                    addIfMatch(wi, x, z, range, ocean, onlyHighFloating, seen, out);
                }
            }
        }

        // 3. Архипелаги всех крупных биомов Элисара
        int cr0X = Math.floorDiv(minX, CrystalRifts.SUPER), cr1X = Math.floorDiv(maxX, CrystalRifts.SUPER);
        int cr0Z = Math.floorDiv(minZ, CrystalRifts.SUPER), cr1Z = Math.floorDiv(maxZ, CrystalRifts.SUPER);
        for (int sx = cr0X; sx <= cr1X; ++sx) {
            for (int sz = cr0Z; sz <= cr1Z; ++sz) {
                CrystalRifts.Region cr = CrystalRifts.region(sx, sz);
                if (cr != null) {
                    for (CityPlan.Island ci : cr.isles) {
                        addIfMatch(ci, x, z, range, ocean, onlyHighFloating, seen, out);
                    }
                }
            }
        }

        int vh0X = Math.floorDiv(minX, VioletHills.SUPER), vh1X = Math.floorDiv(maxX, VioletHills.SUPER);
        int vh0Z = Math.floorDiv(minZ, VioletHills.SUPER), vh1Z = Math.floorDiv(maxZ, VioletHills.SUPER);
        for (int sx = vh0X; sx <= vh1X; ++sx) {
            for (int sz = vh0Z; sz <= vh1Z; ++sz) {
                VioletHills.Region vh = VioletHills.region(sx, sz);
                if (vh != null) {
                    for (CityPlan.Island vi : vh.isles) {
                        addIfMatch(vi, x, z, range, ocean, onlyHighFloating, seen, out);
                    }
                }
            }
        }

        int sg0X = Math.floorDiv(minX, SizoGrove.SUPER), sg1X = Math.floorDiv(maxX, SizoGrove.SUPER);
        int sg0Z = Math.floorDiv(minZ, SizoGrove.SUPER), sg1Z = Math.floorDiv(maxZ, SizoGrove.SUPER);
        for (int sx = sg0X; sx <= sg1X; ++sx) {
            for (int sz = sg0Z; sz <= sg1Z; ++sz) {
                SizoGrove.Region sg = SizoGrove.region(sx, sz);
                if (sg != null && sg.hub != null) {
                    addIfMatch(sg.hub, x, z, range, ocean, onlyHighFloating, seen, out);
                }
            }
        }

        int al0X = Math.floorDiv(minX, AshenLands.SUPER), al1X = Math.floorDiv(maxX, AshenLands.SUPER);
        int al0Z = Math.floorDiv(minZ, AshenLands.SUPER), al1Z = Math.floorDiv(maxZ, AshenLands.SUPER);
        for (int sx = al0X; sx <= al1X; ++sx) {
            for (int sz = al0Z; sz <= al1Z; ++sz) {
                AshenLands.Region al = AshenLands.region(sx, sz);
                if (al != null && al.island != null) {
                    addIfMatch(al.island, x, z, range, ocean, onlyHighFloating, seen, out);
                }
            }
        }

        if (!onlyHighFloating) {
            int wg0X = Math.floorDiv(minX, WaterfallGardens.SUPER), wg1X = Math.floorDiv(maxX, WaterfallGardens.SUPER);
            int wg0Z = Math.floorDiv(minZ, WaterfallGardens.SUPER), wg1Z = Math.floorDiv(maxZ, WaterfallGardens.SUPER);
            for (int sx = wg0X; sx <= wg1X; ++sx) {
                for (int sz = wg0Z; sz <= wg1Z; ++sz) {
                    WaterfallGardens.Region wg = WaterfallGardens.region(sx, sz);
                    if (wg != null) {
                        for (CityPlan.Island gi : wg.isles) {
                            addIfMatch(gi, x, z, range, ocean, false, seen, out);
                        }
                    }
                }
            }
        }

        // 4. Пепельные руины, деревни на островах, заброшенный театр и прибрежные острова бухты
        int er0X = Math.floorDiv(minX, EmberRuins.SUPER), er1X = Math.floorDiv(maxX, EmberRuins.SUPER);
        int er0Z = Math.floorDiv(minZ, EmberRuins.SUPER), er1Z = Math.floorDiv(maxZ, EmberRuins.SUPER);
        for (int sx = er0X; sx <= er1X; ++sx) {
            for (int sz = er0Z; sz <= er1Z; ++sz) {
                EmberRuins.Site er = EmberRuins.site(sx, sz);
                if (er != null && er.island != null) {
                    addIfMatch(er.island, x, z, range, ocean, onlyHighFloating, seen, out);
                }
            }
        }

        int vv0X = Math.floorDiv(minX, WorldStructures.SUPER_VILLAGE), vv1X = Math.floorDiv(maxX, WorldStructures.SUPER_VILLAGE);
        int vv0Z = Math.floorDiv(minZ, WorldStructures.SUPER_VILLAGE), vv1Z = Math.floorDiv(maxZ, WorldStructures.SUPER_VILLAGE);
        for (int sx = vv0X; sx <= vv1X; ++sx) {
            for (int sz = vv0Z; sz <= vv1Z; ++sz) {
                WorldStructures.Village wv = WorldStructures.whiteVillage(sx, sz);
                if (wv != null && wv.island != null) {
                    addIfMatch(wv.island, x, z, range, ocean, onlyHighFloating, seen, out);
                }
            }
        }

        int bv0X = Math.floorDiv(minX, WorldStructures.SUPER_BURNED), bv1X = Math.floorDiv(maxX, WorldStructures.SUPER_BURNED);
        int bv0Z = Math.floorDiv(minZ, WorldStructures.SUPER_BURNED), bv1Z = Math.floorDiv(maxZ, WorldStructures.SUPER_BURNED);
        for (int sx = bv0X; sx <= bv1X; ++sx) {
            for (int sz = bv0Z; sz <= bv1Z; ++sz) {
                WorldStructures.Village bv = WorldStructures.burnedIslandVillage(sx, sz);
                if (bv != null && bv.island != null) {
                    addIfMatch(bv.island, x, z, range, ocean, onlyHighFloating, seen, out);
                }
            }
        }

        int gt0X = Math.floorDiv(minX, LandmarkStructures.SUPER_THEATER), gt1X = Math.floorDiv(maxX, LandmarkStructures.SUPER_THEATER);
        int gt0Z = Math.floorDiv(minZ, LandmarkStructures.SUPER_THEATER), gt1Z = Math.floorDiv(maxZ, LandmarkStructures.SUPER_THEATER);
        for (int sx = gt0X; sx <= gt1X; ++sx) {
            for (int sz = gt0Z; sz <= gt1Z; ++sz) {
                LandmarkStructures.GrandTheater gt = LandmarkStructures.grandTheater(sx, sz);
                if (gt != null && gt.island != null) {
                    addIfMatch(gt.island, x, z, range, ocean, onlyHighFloating, seen, out);
                }
            }
        }

        int hb0X = Math.floorDiv(minX, LandmarkStructures.SUPER_HARBOR), hb1X = Math.floorDiv(maxX, LandmarkStructures.SUPER_HARBOR);
        int hb0Z = Math.floorDiv(minZ, LandmarkStructures.SUPER_HARBOR), hb1Z = Math.floorDiv(maxZ, LandmarkStructures.SUPER_HARBOR);
        for (int sx = hb0X; sx <= hb1X; ++sx) {
            for (int sz = hb0Z; sz <= hb1Z; ++sz) {
                LandmarkStructures.HarborPort hp = LandmarkStructures.harborPort(sx, sz);
                if (hp != null && hp.island != null) {
                    addIfMatch(hp.island, x, z, range, ocean, onlyHighFloating, seen, out);
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
        double cone = isl.depth * Math.pow(t, 0.72);
        return Math.max(ocean + 6.0, isl.base - cone);
    }
}
