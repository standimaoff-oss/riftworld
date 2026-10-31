package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Biome 04 «Фиолетовые холмы» (референс {@code 04_violet_hills.png}).
 *
 * <ul>
 *   <li><b>Архипелаг из 3–6 огромных парящих островов</b> ({@code R = 52..102}, поперечник каждого
 *       острова ~110–215 блоков) с пропастями 20–32 блока открытого неба над морем между ними.</li>
 *   <li><b>Извилистая светло-каменная дорога с ответвлением</b> проходит по долинам между ярусными
 *       лавандовыми холмами и обрывается прямо у кромки скал над пропастью между островами
 *       (пропасть пока остаётся полностью пустой под будущие промежуточные островки/мосты),
 *       продолжаясь на соседнем огромном острове за обрывом.</li>
 *   <li>Вдоль дороги чередуются каменные стенки, деревянные заборы и каменные столбы с факелами/фонарями,
 *       на склонах растут деревья аддона DT ({@code riftworlddt:lilac}) и воздушные лавандовые цветы,
 *       а вход в атмосферу биома плавно интерполируется по {@link #skyWeight(double, double)}.</li>
 * </ul>
 */
public final class VioletHills {
    public static final int SUPER = 1160;
    private static final long SEED = 0x9B1C7L;

    public static final class Region {
        public final int cx, cz;
        public final double Rr;
        /** 3–6 огромных парящих островов архипелага; {@link #hub} ({@link #meadow}) — главный остров. */
        public final List<CityPlan.Island> isles = new ArrayList<>(6);
        public final CityPlan.Island hub;
        public final CityPlan.Island meadow;
        // главная извилистая дорога через долину с обрывом над пропастью между hub и вторым островом
        public final double rax, raz, rbx, rbz, curvePhase;
        // боковое ответвление дороги в сторону третьего огромного острова
        public final double bax, baz, bbx, bbz, branchPhase;
        // параметры u вдоль главной дороги для ракурса пропасти между первым и вторым островами
        public final double hubRimU, nextRimU;
        public final int riverLevel;
        public static final double RIVER_HW = 2.35;
        public static final double BRANCH_HW = 1.60;

        Region(int cx, int cz, Random rnd) {
            this.cx = cx;
            this.cz = cz;
            int ocean = CityPlan.get().ocean;
            int want = 3 + rnd.nextInt(4); // строго от 3 до 6 огромных островов (3, 4, 5 или 6)

            double[] rs = new double[want];
            rs[0] = 84.0 + rnd.nextDouble() * 16.0; // 84..100 (~180..215 блоков в поперечнике)
            rs[1] = 66.0 + rnd.nextDouble() * 14.0; // 66..80  (~140..175 блоков)
            rs[2] = 56.0 + rnd.nextDouble() * 14.0; // 56..70  (~120..150 блоков)
            for (int i = 3; i < want; ++i) {
                rs[i] = 52.0 + rnd.nextDouble() * 16.0; // 52..68 (~110..145 блоков)
            }

            double phi = rnd.nextDouble() * Math.PI;
            // Смещаем главный остров чуть назад вдоль -phi, чтобы весь архипелаг был сбалансирован вокруг (cx, cz)
            double hubShift = 38.0 + rnd.nextDouble() * 12.0;
            CityPlan.Island first = makeIsland(
                    cx - Math.cos(phi) * hubShift,
                    cz - Math.sin(phi) * hubShift,
                    rs[0], ocean, phi + (rnd.nextDouble() - 0.5) * 0.28, rnd);
            isles.add(first);

            // Второй огромный остров ставим вдоль направления дороги +phi через пустую пропасть 21..29 блоков
            double chasm1 = 21.0 + rnd.nextDouble() * 8.0;
            CityPlan.Island cand1 = makeIsland(0, 0, rs[1], ocean, phi + (rnd.nextDouble() - 0.5) * 0.55, rnd);
            double ang1 = phi + (rnd.nextDouble() - 0.5) * 0.16;
            double dist1 = outerRadius(first, ang1) + outerRadius(cand1, ang1 + Math.PI) + chasm1;
            CityPlan.Island second = copyAt(cand1, first.cx + Math.cos(ang1) * dist1, first.cz + Math.sin(ang1) * dist1);
            isles.add(second);

            // Третий огромный остров — сбоку от долины через пропасть 21..30 блоков (туда уходит боковая тропа)
            double bSide = rnd.nextBoolean() ? 1.0 : -1.0;
            CityPlan.Island placed2 = null;
            for (int attempt = 0; attempt < 40; ++attempt) {
                double ang2 = phi + bSide * (1.12 + (rnd.nextDouble() - 0.35) * 0.55) + attempt * 0.09 * bSide;
                double chasm2 = 21.0 + rnd.nextDouble() * 9.0;
                CityPlan.Island cand2 = makeIsland(0, 0, rs[2], ocean, rnd.nextDouble() * Math.PI, rnd);
                double d2 = outerRadius(first, ang2) + outerRadius(cand2, ang2 + Math.PI) + chasm2;
                double px = first.cx + Math.cos(ang2) * d2;
                double pz = first.cz + Math.sin(ang2) * d2;
                CityPlan.Island sh = copyAt(cand2, px, pz);
                if (minGapToAll(sh, isles) >= 17.5 && Math.hypot(px - cx, pz - cz) + rs[2] * 1.25 <= 360.0) {
                    placed2 = sh;
                    break;
                }
            }
            if (placed2 == null) {
                double ang2 = phi + bSide * 1.48;
                CityPlan.Island cand2 = makeIsland(0, 0, rs[2], ocean, rnd.nextDouble() * Math.PI, rnd);
                double d2 = outerRadius(first, ang2) + outerRadius(cand2, ang2 + Math.PI) + 24.0;
                placed2 = copyAt(cand2, first.cx + Math.cos(ang2) * d2, first.cz + Math.sin(ang2) * d2);
            }
            isles.add(placed2);

            // Остальные огромные острова (4-й, 5-й, 6-й) раскладываем вокруг ядра архипелага с пропастями 20..32 блока
            for (int i = 3; i < want; ++i) {
                double ri = rs[i];
                CityPlan.Island placed = null;
                for (int attempt = 0; attempt < 72; ++attempt) {
                    CityPlan.Island parent = isles.get(attempt % isles.size());
                    double ang = (attempt * 2.39996323 + rnd.nextDouble() * 0.55) % (Math.PI * 2.0);
                    double chasm = 20.0 + rnd.nextDouble() * 12.0;
                    CityPlan.Island cand = makeIsland(0, 0, ri, ocean, rnd.nextDouble() * Math.PI, rnd);
                    double dist = outerRadius(parent, ang) + outerRadius(cand, ang + Math.PI) + chasm;
                    double px = parent.cx + Math.cos(ang) * dist;
                    double pz = parent.cz + Math.sin(ang) * dist;
                    if (Math.hypot(px - cx, pz - cz) + ri * 1.25 > 375.0) continue;
                    CityPlan.Island sh = copyAt(cand, px, pz);
                    if (minGapToAll(sh, isles) >= 17.5) {
                        placed = sh;
                        break;
                    }
                }
                if (placed == null) {
                    // Гарантированный внешний пояс с проверкой зазора >= 17.5 блоков между всеми островами
                    for (int fb = 0; fb < 36; ++fb) {
                        double ang = phi + Math.PI + (fb * 2.39996323) % (Math.PI * 2.0);
                        double extra = 24.0 + (fb / 6) * 28.0;
                        CityPlan.Island cand = makeIsland(0, 0, ri, ocean, rnd.nextDouble() * Math.PI, rnd);
                        double dist = outerRadius(first, ang) + outerRadius(cand, ang + Math.PI) + extra;
                        CityPlan.Island sh = copyAt(cand, first.cx + Math.cos(ang) * dist, first.cz + Math.sin(ang) * dist);
                        if (minGapToAll(sh, isles) >= 17.5) {
                            placed = sh;
                            break;
                        }
                    }
                }
                if (placed != null) {
                    isles.add(placed);
                }
            }

            this.hub = first;
            this.meadow = first;

            // Главная дорога: смещена от (hub.cx, hub.cz) на 11..14 блоков, идёт через весь главный остров,
            // обрывается у пропасти (в пропасти пусто!) и продолжается на втором огромном острове
            double perpOff = (rnd.nextBoolean() ? 1.0 : -1.0) * (11.5 + 2.8 * rnd.nextDouble());
            double rcx = first.cx - Math.sin(ang1) * perpOff;
            double rcz = first.cz + Math.cos(ang1) * perpOff;
            double backLen = outerRadius(first, ang1 + Math.PI) * 0.84;
            double fwdLen = dist1 + outerRadius(second, ang1) * 0.80;
            this.rax = rcx - Math.cos(ang1) * backLen;
            this.raz = rcz - Math.sin(ang1) * backLen;
            this.rbx = rcx + Math.cos(ang1) * fwdLen;
            this.rbz = rcz + Math.sin(ang1) * fwdLen;
            this.curvePhase = rnd.nextDouble() * Math.PI * 2.0;
            this.branchPhase = rnd.nextDouble() * Math.PI * 2.0;

            // Находим точные параметры u кромки обрыва на hub и возобновления дороги на second
            double hRim = 0.48, nRim = 0.62;
            for (int step = 10; step <= 90; ++step) {
                double u = step / 100.0;
                double[] p = mainRoadPoint(u);
                if (first.t(p[0], p[1]) > 0.04) hRim = u;
            }
            for (int step = 90; step >= 10; --step) {
                double u = step / 100.0;
                double[] p = mainRoadPoint(u);
                if (second.t(p[0], p[1]) > 0.04) nRim = u;
            }
            this.hubRimU = hRim;
            this.nextRimU = Math.max(hRim + 0.06, nRim);

            // Боковая дорожка отходит на главном острове и идёт к третьему острову (через свой обрыв)
            double forkU = Math.max(0.18, Math.min(0.42, this.hubRimU * 0.62));
            double[] fork = mainRoadPoint(forkU);
            double bAng = Math.atan2(placed2.cz - fork[1], placed2.cx - fork[0]);
            double bLen = Math.hypot(placed2.cx - fork[0], placed2.cz - fork[1]) + placed2.R * 0.35;
            this.bax = fork[0];
            this.baz = fork[1];
            this.bbx = fork[0] + Math.cos(bAng) * bLen;
            this.bbz = fork[1] + Math.sin(bAng) * bLen;

            double extent = 0.0;
            for (CityPlan.Island isl : isles) {
                extent = Math.max(extent, Math.hypot(isl.cx - cx, isl.cz - cz) + isl.maxReach());
            }
            this.Rr = extent + 16.0;
            this.riverLevel = (int) Math.floor(first.base) - 2;
        }

        private static CityPlan.Island makeIsland(double x, double z, double R, int ocean, double elongAng, Random rnd) {
            double base = ocean + 18 + rnd.nextInt(5);
            CityPlan.Island isl = new CityPlan.Island(
                    x, z, R, base, R * 0.58 + 12,
                    rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283,
                    7, 0.9);
            isl.elong = 0.16 + rnd.nextDouble() * 0.18;
            isl.elongAng = elongAng;
            isl.bend = (rnd.nextDouble() - 0.5) * 0.26;
            isl.lobe2 = (rnd.nextDouble() - 0.5) * 0.18;
            isl.lobe3 = (rnd.nextDouble() - 0.5) * 0.14;
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

        /** Внешний радиус кромки (t = 0) острова в мировом направлении ang. */
        private static double outerRadius(CityPlan.Island isl, double ang) {
            double lo = isl.R * 0.35, hi = isl.maxReach();
            for (int it = 0; it < 16; ++it) {
                double mid = 0.5 * (lo + hi);
                double tx = isl.cx + Math.cos(ang) * mid;
                double tz = isl.cz + Math.sin(ang) * mid;
                if (isl.t(tx, tz) > 0.0) lo = mid;
                else hi = mid;
            }
            return 0.5 * (lo + hi);
        }

        private static double minGapToAll(CityPlan.Island cand, List<CityPlan.Island> others) {
            double minGap = Double.POSITIVE_INFINITY;
            for (CityPlan.Island o : others) {
                double a = Math.atan2(cand.cz - o.cz, cand.cx - o.cx);
                double gap = Math.hypot(cand.cx - o.cx, cand.cz - o.cz)
                        - (outerRadius(o, a) + outerRadius(cand, a + Math.PI));
                if (gap < minGap) minGap = gap;
            }
            return minGap;
        }

        /** Остров архипелага Фиолетовых холмов в точке (x, z) или {@code null} в пропасти между островами. */
        public CityPlan.Island islandAt(double x, double z) {
            for (CityPlan.Island isl : isles) {
                if (Math.abs(x - isl.cx) < isl.maxReach() && Math.abs(z - isl.cz) < isl.maxReach() && isl.t(x, z) > 0.0) {
                    return isl;
                }
            }
            return null;
        }

        public double mainRoadLen() {
            return Math.max(1.0, Math.hypot(rbx - rax, rbz - raz));
        }

        private double mainSway(double along, double bx, double bz) {
            return 12.5 * Math.sin(along * 0.038 + curvePhase)
                    + 3.8 * (ElisarChunkGenerator.vnoise(bx * 0.015, bz * 0.015, 173) - 0.5);
        }

        /** Расстояние до главной дороги через долину. */
        public double mainRoadDist(double px, double pz) {
            double dx = rbx - rax, dz = rbz - raz;
            double len = Math.max(1.0, Math.hypot(dx, dz));
            double ux = dx / len, uz = dz / len;
            double nx = -uz, nz = ux;
            double along = Math.max(0.0, Math.min(len, (px - rax) * ux + (pz - raz) * uz));
            double bx = rax + ux * along, bz = raz + uz * along;
            double sway = mainSway(along, bx, bz);
            return Math.hypot(px - (bx + nx * sway), pz - (bz + nz * sway));
        }

        /** Расстояние до боковой дорожки. */
        public double branchRoadDist(double px, double pz) {
            double dx = bbx - bax, dz = bbz - baz;
            double len = Math.max(1.0, Math.hypot(dx, dz));
            double ux = dx / len, uz = dz / len;
            double nx = -uz, nz = ux;
            double along = Math.max(0.0, Math.min(len, (px - bax) * ux + (pz - baz) * uz));
            double sway = 5.0 * Math.sin(along * 0.065 + branchPhase);
            double cx0 = bax + ux * along + nx * sway;
            double cz0 = baz + uz * along + nz * sway;
            return Math.hypot(px - cx0, pz - cz0);
        }

        /** Минимальное расстояние до дорожной сети (главная или боковая дорожка). */
        public double roadDist(double px, double pz) {
            return Math.min(mainRoadDist(px, pz), branchRoadDist(px, pz) + (RIVER_HW - BRANCH_HW));
        }

        /** Точка (x, z) на оси главной дороги по параметру u в [0..1]. */
        public double[] mainRoadPoint(double u) {
            double dx = rbx - rax, dz = rbz - raz;
            double len = Math.max(1.0, Math.hypot(dx, dz));
            double ux = dx / len, uz = dz / len;
            double nx = -uz, nz = ux;
            double along = Math.max(0.0, Math.min(1.0, u)) * len;
            double bx = rax + ux * along, bz = raz + uz * along;
            double sway = mainSway(along, bx, bz);
            return new double[]{bx + nx * sway, bz + nz * sway};
        }

        public double[] roadPoint(double u) {
            return mainRoadPoint(u);
        }

        /** Точка на обочине главной дороги на расстоянии {@code along} от начала и поперечном смещении {@code sideOffset}. */
        public double[] roadSidePoint(double along, double sideOffset) {
            double len = mainRoadLen();
            double u0 = Math.max(0.0, Math.min(1.0, (along - 0.75) / len));
            double u1 = Math.max(0.0, Math.min(1.0, (along + 0.75) / len));
            double u = Math.max(0.0, Math.min(1.0, along / len));
            double[] p = mainRoadPoint(u);
            double[] a = mainRoadPoint(u0);
            double[] b = mainRoadPoint(u1);
            double tx = b[0] - a[0], tz = b[1] - a[1];
            double tl = Math.max(1e-4, Math.hypot(tx, tz));
            double nx = -tz / tl, nz = tx / tl;
            return new double[]{p[0] + nx * sideOffset, p[1] + nz * sideOffset};
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
        if (r.nextDouble() > 0.48) return null;
        int margin = 250;
        int cx = sx * SUPER + margin + r.nextInt(SUPER - 2 * margin);
        int cz = sz * SUPER + margin + r.nextInt(SUPER - 2 * margin);
        if (Math.hypot(cx, cz) < 640) return null;
        if (AshenLands.blocks(cx, cz, 340)) return null;
        EmberRuins.Site es = EmberRuins.nearest(cx, cz);
        if (es != null && Math.hypot(cx - es.cx, cz - es.cz) < 460) return null;
        return new Region(cx, cz, r);
    }

    public static Region regionAt(double x, double z) {
        int sx = Math.floorDiv((int) Math.floor(x), SUPER), sz = Math.floorDiv((int) Math.floor(z), SUPER);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                Region rg = region(sx + dx, sz + dz);
                if (rg != null && Math.abs(x - rg.cx) < rg.Rr + 95 && Math.abs(z - rg.cz) < rg.Rr + 95) {
                    return rg;
                }
            }
        }
        return null;
    }

    /** Остров архипелага Фиолетовых холмов с запасом от кромки (для /locate biome и биом-сурса). */
    public static CityPlan.Island landAt(double x, double z) {
        Region rg = regionAt(x, z);
        if (rg == null) return null;
        for (CityPlan.Island isl : rg.isles) {
            if (Math.abs(x - isl.cx) < isl.maxReach() && Math.abs(z - isl.cz) < isl.maxReach()
                    && isl.t(x, z) > 0.06 + 4.0 / isl.R) {
                return isl;
            }
        }
        return null;
    }

    public static boolean inside(double x, double z) {
        return landAt(x, z) != null;
    }

    /**
     * Плавный пространственный вес атмосферы Фиолетовых холмов [0..1]:
     * 1.0 над всем архипелагом и пропастями между его островами, с мягким smoothstep-затуханием
     * на протяжении 95 блоков вокруг внешних берегов островов (никаких резких скачков неба/тумана).
     */
    public static float skyWeight(double x, double z) {
        int sx = Math.floorDiv((int) Math.floor(x), SUPER), sz = Math.floorDiv((int) Math.floor(z), SUPER);
        float best = 0.0F;
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                Region rg = region(sx + dx, sz + dz);
                if (rg == null) continue;
                double rc = Math.hypot(x - rg.cx, z - rg.cz);
                if (rc > rg.Rr + 115.0) continue;
                // Расстояние до ближайшего острова архипелага (внутри и между соседними островами -> 1.0)
                double minEdge = Double.POSITIVE_INFINITY;
                for (CityPlan.Island isl : rg.isles) {
                    double dCenter = Math.hypot(x - isl.cx, z - isl.cz);
                    double approxEdge = dCenter - isl.R * (1.0 + isl.elong * 0.5);
                    if (approxEdge < minEdge) minEdge = approxEdge;
                }
                double u = 1.0 - Math.max(0.0, Math.min(1.0, (minEdge - 18.0) / 95.0));
                float w = (float) (u * u * (3.0 - 2.0 * u));
                if (w > best) best = w;
            }
        }
        return best;
    }

    /** Проверка соседства со всеми островами архипелага, чтобы дикие скалы не спавнились в пустых пропастях между ними. */
    public static boolean nearAny(double x, double z, double r, double pad) {
        int sx = Math.floorDiv((int) Math.floor(x), SUPER), sz = Math.floorDiv((int) Math.floor(z), SUPER);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                Region rg = region(sx + dx, sz + dz);
                if (rg == null) continue;
                for (CityPlan.Island vi : rg.isles) {
                    if (Math.hypot(x - vi.cx, z - vi.cz) < r + vi.maxReach() + pad) return true;
                }
            }
        }
        return false;
    }

    public static List<CityPlan.Tree> treesNear(int x, int z) {
        return List.of();
    }
}
