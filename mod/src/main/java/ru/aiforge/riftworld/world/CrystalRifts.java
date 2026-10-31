package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Биом «Хрустальные разломы» ({@code riftworld:crystal_rifts}, референс {@code 02_crystal_sanctum.png}):
 * <ul>
 *   <li>архипелаг из 4–6 огромных расколотых парящих островов из светлого мрамора и сланца с висячими сталактитами;</li>
 *   <li>каменные арочные мосты-переходы ({@link Span}) прямо между всеми островами архипелага, по которым можно пройти пешком;</li>
 *   <li>огромные наклонные блочные кристаллы ({@link Spire}) шестигранного сечения с градиентом от глубинного бирюзового у корня до яркого ледяного сияния на острие + боковые отростки и кристаллы в обрывах скал;</li>
 *   <li>зеркальные бирюзовые минеральные чаши ({@link Pool}) у подножия кристаллов с водопадами ({@link Fall}) через мраморный край;</li>
 *   <li>центральный остров-святилище с белой мраморной аллеей, хрустальными серебрицами ({@code riftworlddt:crystal_birch}) и разрушенной каменной колоннадой-астролябией.</li>
 * </ul>
 */
public final class CrystalRifts {
    public static final int SUPER = 1120;
    public static final int KIND = 10;
    private static final long FIXED = 0x4352595354414CL;
    private static final Map<Long, Optional<Region>> CACHE = new ConcurrentHashMap<>();

    /** Каменный арочный мост-проход между двумя островами архипелага. */
    public static final class Span {
        public final CityPlan.Island islA, islB;
        public final double ax, az, bx, bz;
        public final double perpX, perpZ, curveAmp, len;
        public final double archApexBonus;
        public final long seed;

        Span(CityPlan.Island islA, CityPlan.Island islB,
             double ax, double az, double bx, double bz,
             double curveAmp, double archApexBonus, long seed) {
            this.islA = islA;
            this.islB = islB;
            this.ax = ax;
            this.az = az;
            this.bx = bx;
            this.bz = bz;
            double dx = bx - ax, dz = bz - az;
            this.len = Math.max(1.0, Math.hypot(dx, dz));
            this.perpX = -dz / this.len;
            this.perpZ = dx / this.len;
            this.curveAmp = curveAmp;
            this.archApexBonus = archApexBonus;
            this.seed = seed;
        }

        /** Точка на центральной оси моста при u в [0..1]. */
        public double[] pointAt(double u) {
            double uc = Math.max(0.0, Math.min(1.0, u));
            double bend = curveAmp * Math.sin(Math.PI * uc);
            return new double[]{
                    ax + (bx - ax) * uc + perpX * bend,
                    az + (bz - az) * uc + perpZ * bend
            };
        }

        /** Полуширина моста: шире у береговых устоев и изящнее в середине пролёта. */
        public double halfWidth(double u) {
            double mid = 4.0 * u * (1.0 - u);
            return 3.25 - 0.85 * mid;
        }

        /** Расстояние от (x, z) до изогнутой оси моста; outU[0] получает параметр u в [0..1]. */
        public double dist(double x, double z, double[] outU) {
            int steps = 20;
            double bestD2 = 1e18;
            double bestU = 0.5;
            double[] p0 = pointAt(0.0);
            for (int i = 1; i <= steps; ++i) {
                double u1 = i / (double) steps;
                double[] p1 = pointAt(u1);
                double u0 = (i - 1) / (double) steps;
                double vx = p1[0] - p0[0], vz = p1[1] - p0[1];
                double seg2 = vx * vx + vz * vz;
                double t = seg2 < 1e-6 ? 0.0 : ((x - p0[0]) * vx + (z - p0[1]) * vz) / seg2;
                t = Math.max(0.0, Math.min(1.0, t));
                double qx = p0[0] + vx * t, qz = p0[1] + vz * t;
                double d2 = (x - qx) * (x - qx) + (z - qz) * (z - qz);
                if (d2 < bestD2) {
                    bestD2 = d2;
                    bestU = u0 + (u1 - u0) * t;
                }
                p0 = p1;
            }
            if (outU != null && outU.length > 0) outU[0] = bestU;
            return Math.sqrt(bestD2);
        }
    }

    /** Огромный наклонный блочный кристалл шестигранного сечения. */
    public static final class Spire {
        public final double ox, oy, oz;     // точка корня кристалла
        public final double dx, dy, dz;     // единичный вектор оси роста
        public final double length;         // длина кристалла в блоках (6..20)
        public final double baseRadius;     // радиус шестигранника у основания (1.4..3.5)
        public final double twist;          // поворот граней призмы вокруг оси
        public final int minX, maxX, minY, maxY, minZ, maxZ;

        Spire(double ox, double oy, double oz, double dx, double dy, double dz,
              double length, double baseRadius, double twist) {
            this.ox = ox;
            this.oy = oy;
            this.oz = oz;
            double l = Math.max(1e-6, Math.sqrt(dx * dx + dy * dy + dz * dz));
            this.dx = dx / l;
            this.dy = dy / l;
            this.dz = dz / l;
            this.length = length;
            this.baseRadius = baseRadius;
            this.twist = twist;
            double ex = ox + this.dx * length, ey = oy + this.dy * length, ez = oz + this.dz * length;
            double bx0 = ox - this.dx * 3.0, by0 = oy - this.dy * 3.0, bz0 = oz - this.dz * 3.0;
            int pad = (int) Math.ceil(baseRadius + 2.0);
            this.minX = (int) Math.floor(Math.min(bx0, ex)) - pad;
            this.maxX = (int) Math.ceil(Math.max(bx0, ex)) + pad;
            this.minY = (int) Math.floor(Math.min(by0, ey)) - pad;
            this.maxY = (int) Math.ceil(Math.max(by0, ey)) + pad;
            this.minZ = (int) Math.floor(Math.min(bz0, ez)) - pad;
            this.maxZ = (int) Math.ceil(Math.max(bz0, ez)) + pad;
        }
    }

    /** Зеркальная бирюзовая минеральная чаша у подножия главного кристалла. */
    public static final class Pool {
        public final CityPlan.Island isl;
        public final double cx, cz, r;
        public final int waterY;

        Pool(CityPlan.Island isl, double cx, double cz, double r, int waterY) {
            this.isl = isl;
            this.cx = cx;
            this.cz = cz;
            this.r = r;
            this.waterY = waterY;
        }
    }

    /** Водопад с мраморной кромки острова в море Элисара. */
    public static final class Fall {
        public final CityPlan.Island isl;
        public final double x, z, r;

        Fall(CityPlan.Island isl, double x, double z, double r) {
            this.isl = isl;
            this.x = x;
            this.z = z;
            this.r = r;
        }
    }

    /** Тропа из колотого белого мрамора на острове от моста к террасе кристалла. */
    public static final class IslandPath {
        public final CityPlan.Island isl;
        public final double ax, az, bx, bz, hw;

        IslandPath(CityPlan.Island isl, double ax, double az, double bx, double bz, double hw) {
            this.isl = isl;
            this.ax = ax;
            this.az = az;
            this.bx = bx;
            this.bz = bz;
            this.hw = hw;
        }
    }

    public static final class Region {
        public final int cx, cz, Rr;
        public final long seed;
        public final double phi;
        public final CityPlan.Island hub;
        public final List<CityPlan.Island> isles;
        public final List<Span> spans;
        public final List<Spire> spires;
        public final List<Pool> pools;
        public final List<Fall> falls;
        public final List<IslandPath> paths;

        // Центральная разрушенная каменная колоннада-астролябия и мраморная аллея на hub
        public final double astroX, astroZ;
        public final int astroY;
        public final double aveAx, aveAz, aveBx, aveBz;
        public static final double AVE_HW = 3.1;
        public static final double ASTRO_R = 8.2;

        Region(int cx, int cz, Random r) {
            this.cx = cx;
            this.cz = cz;
            this.seed = r.nextLong();
            Random rnd = new Random(this.seed);
            int ocean = 62;
            this.phi = rnd.nextDouble() * Math.PI * 2.0;
            double ux = Math.cos(phi), uz = Math.sin(phi);
            double px = -uz, pz = ux;

            // 4..6 огромных парящих островов (hub + 3..5 расколотых кристальных островов вокруг)
            int count = 4 + rnd.nextInt(3); // 4, 5 или 6
            List<CityPlan.Island> built = new ArrayList<>(count);

            double hubR = 47.0 + rnd.nextDouble() * 7.0; // 47..54
            int hubBase = ocean + 23 + rnd.nextInt(4);   // 85..88
            CityPlan.Island hubIsl = new CityPlan.Island(
                    cx, cz, hubR, hubBase, 28 + rnd.nextInt(6),
                    rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283,
                    KIND, 0.9);
            built.add(hubIsl);
            this.hub = hubIsl;

            // Колоннада-астролябия ближе к передней части hub (в сторону главных островов #1 и #2),
            // а мраморная аллея ведёт к ней от противоположного края hub — точь-в-точь кадр 02_crystal_sanctum.png!
            this.astroX = cx + ux * (hubR * 0.20);
            this.astroZ = cz + uz * (hubR * 0.20);
            this.astroY = hubBase + 1;
            this.aveAx = cx - ux * (hubR * 0.72);
            this.aveAz = cz - uz * (hubR * 0.72);
            this.aveBx = this.astroX - ux * 5.5;
            this.aveBz = this.astroZ - uz * 5.5;

            // Размещаем окружающие острова полумесяцем/архипелагом вокруг hub с проливами 18..25 блоков
            double[] relAngles = {
                    0.58 + (rnd.nextDouble() - 0.5) * 0.10,   // #1 левый главный остров с гигантским кристаллом, чашей и водопадами
                    -0.60 + (rnd.nextDouble() - 0.5) * 0.10,  // #2 правый остров с кристаллом и светящимися грибами
                    1.42 + (rnd.nextDouble() - 0.5) * 0.16,   // #3 дальний левый остров
                    -1.44 + (rnd.nextDouble() - 0.5) * 0.16,  // #4 дальний правый остров
                    0.02 + (rnd.nextDouble() - 0.5) * 0.14    // #5 дальний центральный остров за проливом
            };

            int[] parentIdx = new int[count];
            for (int i = 1; i < count; ++i) {
                double rIsl = i == 1 ? (58.0 + rnd.nextDouble() * 10.0)
                        : i == 2 ? (53.0 + rnd.nextDouble() * 9.0)
                        : (48.0 + rnd.nextDouble() * 12.0);
                int baseIsl = ocean + 25 + rnd.nextInt(6); // 87..92
                CityPlan.Island proto = new CityPlan.Island(
                        cx, cz, rIsl, baseIsl, 30 + rnd.nextInt(8),
                        rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283,
                        KIND, 1.5);

                CityPlan.Island anchor = (i == 5 && built.size() > 1) ? built.get(1) : hubIsl;
                parentIdx[i] = (anchor == hubIsl) ? 0 : 1;
                double ang = phi + relAngles[i - 1];
                double wantGap = 18.5 + rnd.nextDouble() * 6.5; // пролив 18.5..25 блоков

                CityPlan.Island placed = null;
                for (int attempt = 0; attempt < 28; ++attempt) {
                    double testAng = ang + (attempt == 0 ? 0.0 : (attempt % 2 == 0 ? 1 : -1) * (attempt * 0.08));
                    double dirX = Math.cos(testAng), dirZ = Math.sin(testAng);
                    double rA = rimDist(anchor, dirX, dirZ);
                    double rB = rimDist(proto, -dirX, -dirZ);
                    double dist = rA + rB + wantGap + (attempt >= 14 ? (attempt - 13) * 2.5 : 0.0);
                    double ix = anchor.cx + dirX * dist;
                    double iz = anchor.cz + dirZ * dist;
                    CityPlan.Island cand = moveIsland(proto, ix, iz);
                    if (minGapToAll(cand, built) >= 15.5) {
                        placed = cand;
                        break;
                    }
                }
                if (placed == null) {
                    double fallbackAng = phi + relAngles[i - 1];
                    double dirX = Math.cos(fallbackAng), dirZ = Math.sin(fallbackAng);
                    double dist = anchor.maxReach() + proto.maxReach() + 20.0;
                    while (minGapToAll(moveIsland(proto, anchor.cx + dirX * dist, anchor.cz + dirZ * dist), built) < 15.5) {
                        dist += 8.0;
                    }
                    placed = moveIsland(proto, anchor.cx + dirX * dist, anchor.cz + dirZ * dist);
                }
                built.add(placed);
            }
            this.isles = Collections.unmodifiableList(built);

            double maxReach = 0.0;
            for (CityPlan.Island vi : this.isles) {
                double d = Math.hypot(vi.cx - cx, vi.cz - cz) + vi.maxReach();
                if (d > maxReach) maxReach = d;
            }
            this.Rr = (int) Math.ceil(maxReach);

            // Строим каменные арочные мосты-переходы между островами (связный граф по всем островам + доп. связь)
            List<Span> spList = new ArrayList<>();
            List<IslandPath> pathList = new ArrayList<>();
            for (int i = 1; i < this.isles.size(); ++i) {
                // Выбираем ближайший уже подключённый остров, чтобы мост шёл через самый узкий живописный пролив
                int bestP = parentIdx[i];
                double bestRimGap = 1e9;
                for (int j = 0; j < i; ++j) {
                    CityPlan.Island candA = this.isles.get(j);
                    CityPlan.Island candB = this.isles.get(i);
                    double dx = candB.cx - candA.cx, dz = candB.cz - candA.cz;
                    double d = Math.max(1.0, Math.hypot(dx, dz));
                    double gap = d - rimDist(candA, dx / d, dz / d) - rimDist(candB, -dx / d, -dz / d);
                    if (gap < bestRimGap) {
                        bestRimGap = gap;
                        bestP = j;
                    }
                }
                CityPlan.Island a = this.isles.get(bestP);
                CityPlan.Island b = this.isles.get(i);
                Span span = makeSpan(a, b, rnd);
                spList.add(span);
                if (a == hubIsl) {
                    // Соединяем устой моста на hub с площадью вокруг колоннады-астролябии
                    pathList.add(new IslandPath(hubIsl, astroX, astroZ, span.ax, span.az, 2.3));
                }
            }
            // Если островов >= 4 и между #1 и #3 (или #2 и #4) узкий пролив, добавим ещё один высотный мост!
            if (this.isles.size() >= 4) {
                for (int i = 1; i < this.isles.size(); ++i) {
                    for (int j = i + 1; j < this.isles.size(); ++j) {
                        if (alreadyLinked(spList, this.isles.get(i), this.isles.get(j))) continue;
                        CityPlan.Island a = this.isles.get(i), b = this.isles.get(j);
                        double dx = b.cx - a.cx, dz = b.cz - a.cz;
                        double d = Math.max(1.0, Math.hypot(dx, dz));
                        double gap = d - rimDist(a, dx / d, dz / d) - rimDist(b, -dx / d, -dz / d);
                        if (gap >= 14.0 && gap <= 34.0) {
                            spList.add(makeSpan(a, b, rnd));
                            break;
                        }
                    }
                    if (spList.size() >= this.isles.size() + 1) break;
                }
            }
            this.spans = Collections.unmodifiableList(spList);

            // На каждом окружающем острове создаём гигантские блочные кристаллы, а на #1 и #2 — минеральные чаши и водопады
            List<Spire> spireList = new ArrayList<>();
            List<Pool> poolList = new ArrayList<>();
            List<Fall> fallList = new ArrayList<>();

            for (int i = 1; i < this.isles.size(); ++i) {
                CityPlan.Island isl = this.isles.get(i);
                // Направление от острова к центру святилища (чтобы кристаллы и водопады эффектно смотрели на святилище, как на референсе!)
                double toHubX = hubIsl.cx - isl.cx, toHubZ = hubIsl.cz - isl.cz;
                double toHubL = Math.max(1.0, Math.hypot(toHubX, toHubZ));
                toHubX /= toHubL;
                toHubZ /= toHubL;
                double sideX = -toHubZ, sideZ = toHubX;

                // Главный гигантский кристалл стоит чуть глубже центра террасы, наклоняясь вверх и в сторону пролива/святилища
                double spX = isl.cx + toHubX * (isl.R * 0.10) + sideX * ((rnd.nextDouble() - 0.5) * isl.R * 0.16);
                double spZ = isl.cz + toHubZ * (isl.R * 0.10) + sideZ * ((rnd.nextDouble() - 0.5) * isl.R * 0.16);
                int terraceY = (int) Math.round(isl.base + 2);

                if (i == 1 || (i == 2 && rnd.nextBoolean())) {
                    // Минеральная бирюзовая чаша перед главным кристаллом (как на левом острове 02_crystal_sanctum.png)
                    double poolX = spX + toHubX * 6.5;
                    double poolZ = spZ + toHubZ * 6.5;
                    double poolR = i == 1 ? 8.2 : 6.4;
                    poolList.add(new Pool(isl, poolX, poolZ, poolR, terraceY));

                    // Два водопада прямо на мраморной кромке обрыва перед чашей (льются в пролив/море)
                    for (int fk = -1; fk <= 1; fk += 2) {
                        double fdx = toHubX * 0.94 + sideX * 0.20 * fk;
                        double fdz = toHubZ * 0.94 + sideZ * 0.20 * fk;
                        double fl = Math.max(1e-6, Math.hypot(fdx, fdz));
                        fdx /= fl;
                        fdz /= fl;
                        double rim = rimDist(isl, fdx, fdz);
                        double fx = isl.cx + fdx * (rim - 1.6);
                        double fz = isl.cz + fdz * (rim - 1.6);
                        fallList.add(new Fall(isl, fx, fz, 3.2));
                    }
                }

                // Соединяем устой моста на этом острове мраморной тропой к террасе кристалла
                for (Span sp : spList) {
                    if (sp.islB == isl) {
                        pathList.add(new IslandPath(isl, sp.bx, sp.bz, spX + toHubX * 5.0, spZ + toHubZ * 5.0, 2.1));
                        break;
                    } else if (sp.islA == isl) {
                        pathList.add(new IslandPath(isl, sp.ax, sp.az, spX + toHubX * 5.0, spZ + toHubZ * 5.0, 2.1));
                        break;
                    }
                }

                // 1) Главный исполинский шестигранный кристалл (длина 15..19.5 блоков, радиус 2.7..3.4)
                double tilt = 0.42 + rnd.nextDouble() * 0.18; // наклон ~25°..35° от вертикали
                double tiltAng = Math.atan2(toHubZ, toHubX) + (i % 2 == 1 ? -0.35 : 0.35);
                double mainLen = (i == 1 ? 17.5 : 14.5) + rnd.nextDouble() * 2.5;
                double mainRad = (i == 1 ? 3.1 : 2.65) + rnd.nextDouble() * 0.35;
                spireList.add(new Spire(
                        spX, terraceY - 1.5, spZ,
                        Math.cos(tiltAng) * Math.sin(tilt), Math.cos(tilt), Math.sin(tiltAng) * Math.sin(tilt),
                        mainLen, mainRad, rnd.nextDouble() * Math.PI));

                // 2) 2–3 боковых кристалла-отростка вокруг корня главного кристалла (как пучок кварца на фото!)
                int prongs = 2 + rnd.nextInt(2);
                for (int p = 0; p < prongs; ++p) {
                    double pAng = tiltAng + 1.35 + p * 1.85 + (rnd.nextDouble() - 0.5) * 0.35;
                    double pTilt = 0.62 + rnd.nextDouble() * 0.24; // ~38°..52°
                    double pLen = 7.0 + rnd.nextDouble() * 4.2;
                    double pRad = 1.55 + rnd.nextDouble() * 0.55;
                    spireList.add(new Spire(
                            spX + Math.cos(pAng) * 2.2, terraceY - 1.2, spZ + Math.sin(pAng) * 2.2,
                            Math.cos(pAng) * Math.sin(pTilt), Math.cos(pTilt), Math.sin(pAng) * Math.sin(pTilt),
                            pLen, pRad, rnd.nextDouble() * Math.PI));
                }

                // 3) Диагональный кристалл, пробивающий снизу скальный обрыв над проливом (деталь левого острова на фото!)
                if (i <= 2) {
                    double cliffDist = rimDist(isl, toHubX, toHubZ) * 0.80;
                    double clX = isl.cx + toHubX * cliffDist + sideX * (i == 1 ? -3.5 : 3.5);
                    double clZ = isl.cz + toHubZ * cliffDist + sideZ * (i == 1 ? -3.5 : 3.5);
                    spireList.add(new Spire(
                            clX, terraceY - 8.5, clZ,
                            toHubX * 0.82 + sideX * 0.18, -0.54, toHubZ * 0.82 + sideZ * 0.18,
                            9.5 + rnd.nextDouble() * 2.5, 1.9 + rnd.nextDouble() * 0.3, rnd.nextDouble() * Math.PI));
                }
            }
            this.spires = Collections.unmodifiableList(spireList);
            this.pools = Collections.unmodifiableList(poolList);
            this.falls = Collections.unmodifiableList(fallList);
            this.paths = Collections.unmodifiableList(pathList);
        }

        private static boolean alreadyLinked(List<Span> spans, CityPlan.Island a, CityPlan.Island b) {
            for (Span s : spans) {
                if ((s.islA == a && s.islB == b) || (s.islA == b && s.islB == a)) return true;
            }
            return false;
        }

        private static Span makeSpan(CityPlan.Island a, CityPlan.Island b, Random rnd) {
            double dx = b.cx - a.cx, dz = b.cz - a.cz;
            double len = Math.max(1.0, Math.hypot(dx, dz));
            double ux = dx / len, uz = dz / len;
            double rA = rimDist(a, ux, uz);
            double rB = rimDist(b, -ux, -uz);
            // Анкеры ставятся уверенно на суше каждого острова (~7..9 блоков внутрь от кромки обрыва)
            double inA = Math.max(8.0, rA - 8.5);
            double inB = Math.max(8.0, rB - 8.5);
            double ax = a.cx + ux * inA, az = a.cz + uz * inA;
            double bx = b.cx - ux * inB, bz = b.cz - uz * inB;
            double curve = (rnd.nextDouble() - 0.5) * 3.4;
            double apex = 1.8 + rnd.nextDouble() * 1.1;
            return new Span(a, b, ax, az, bx, bz, curve, apex, rnd.nextLong());
        }

        public boolean owns(CityPlan.Island isl) {
            if (isl == null || isl.kind != KIND) return false;
            for (CityPlan.Island ci : isles) if (ci == isl) return true;
            return false;
        }

        public CityPlan.Island islandAt(double x, double z) {
            for (CityPlan.Island ci : isles) {
                if (Math.abs(x - ci.cx) > ci.maxReach() || Math.abs(z - ci.cz) > ci.maxReach()) continue;
                if (ci.t(x, z) > 0.0) return ci;
            }
            return null;
        }

        public double avenueDist(double x, double z) {
            return CityPlan.segDist(x, z, aveAx, aveAz, aveBx, aveBz, null);
        }

        public double pathDist(CityPlan.Island isl, double x, double z) {
            double best = 1e9;
            if (isl == hub) {
                best = Math.min(best, avenueDist(x, z) - AVE_HW);
                best = Math.min(best, Math.hypot(x - astroX, z - astroZ) - (ASTRO_R + 2.2));
            }
            for (IslandPath p : paths) {
                if (p.isl != isl) continue;
                double d = CityPlan.segDist(x, z, p.ax, p.az, p.bx, p.bz, null) - p.hw;
                if (d < best) best = d;
            }
            return best;
        }

        public Pool poolAt(CityPlan.Island isl, double x, double z) {
            for (Pool p : pools) {
                if (p.isl != isl) continue;
                if (Math.hypot(x - p.cx, z - p.cz) <= p.r + 3.5) return p;
            }
            return null;
        }

        public Fall fallNear(CityPlan.Island isl, double x, double z) {
            for (Fall f : falls) {
                if (f.isl != isl) continue;
                if (Math.hypot(x - f.x, z - f.z) <= f.r) return f;
            }
            return null;
        }

        public boolean nearCrystalOrStructure(double x, double z, double pad) {
            if (Math.hypot(x - astroX, z - astroZ) < ASTRO_R + pad + 2.5) return true;
            if (avenueDist(x, z) < AVE_HW + pad) return true;
            for (IslandPath p : paths) {
                if (CityPlan.segDist(x, z, p.ax, p.az, p.bx, p.bz, null) < p.hw + pad) return true;
            }
            for (Span s : spans) {
                if (s.dist(x, z, null) < 3.6 + pad) return true;
            }
            for (Pool pl : pools) {
                if (Math.hypot(x - pl.cx, z - pl.cz) < pl.r + pad + 1.5) return true;
            }
            for (Spire sp : spires) {
                if (x >= sp.minX - pad && x <= sp.maxX + pad && z >= sp.minZ - pad && z <= sp.maxZ + pad) {
                    double d = CityPlan.segDist(x, z, sp.ox, sp.oz,
                            sp.ox + sp.dx * sp.length, sp.oz + sp.dz * sp.length, null);
                    if (d < sp.baseRadius + pad) return true;
                }
            }
            return false;
        }
    }

    private static CityPlan.Island moveIsland(CityPlan.Island src, double x, double z) {
        return new CityPlan.Island(x, z, src.R, src.base, src.depth, src.p1, src.p2, src.p3, src.kind, src.hill);
    }

    private static double rimDist(CityPlan.Island isl, double ux, double uz) {
        double d = isl.R * 0.45;
        double limit = isl.maxReach() + 6.0;
        while (d < limit && isl.t(isl.cx + ux * d, isl.cz + uz * d) > 0.0) {
            d += 1.5;
        }
        return d;
    }

    private static double minGapToAll(CityPlan.Island cand, List<CityPlan.Island> others) {
        double minGap = 1e9;
        for (CityPlan.Island o : others) {
            double dx = cand.cx - o.cx, dz = cand.cz - o.cz;
            double centerDist = Math.hypot(dx, dz);
            if (centerDist < 1.0) return -999.0;
            double ux = dx / centerDist, uz = dz / centerDist;
            double rO = rimDist(o, ux, uz);
            double rC = rimDist(cand, -ux, -uz);
            double gap = centerDist - rO - rC;
            if (gap < minGap) minGap = gap;
        }
        return minGap;
    }

    private CrystalRifts() {
    }

    public static Region region(int sx, int sz) {
        long key = ((long) sx << 32) ^ (sz & 0xFFFFFFFFL);
        Optional<Region> cached = CACHE.get(key);
        if (cached != null) return cached.orElse(null);

        Random baseRnd = new Random(FIXED ^ (sx * 53982894593057L + sz * 317074912223L) * 0x9E3779B97F4A7C15L);
        int margin = 230;
        Region built = null;
        for (int attempt = 0; attempt < 8; ++attempt) {
            int cx = sx * SUPER + margin + baseRnd.nextInt(SUPER - 2 * margin);
            int cz = sz * SUPER + margin + baseRnd.nextInt(SUPER - 2 * margin);
            if (!spotClear(cx, cz)) continue;
            built = new Region(cx, cz, new Random(baseRnd.nextLong()));
            break;
        }
        CACHE.put(key, Optional.ofNullable(built));
        return built;
    }

    private static boolean spotClear(int cx, int cz) {
        if (Math.hypot(cx, cz) < 550) return false;
        if (Math.hypot(cx - 640, cz - 690) < 380) return false; // не перекрываем морские пробы spots.json
        if (AshenLands.blocks(cx, cz, 300)) return false;
        EmberRuins.Site es = EmberRuins.nearest(cx, cz);
        if (es != null && Math.hypot(cx - es.cx, cz - es.cz) < 440) return false;
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                VioletHills.Region vr = VioletHills.region(Math.floorDiv(cx, VioletHills.SUPER) + dx, Math.floorDiv(cz, VioletHills.SUPER) + dz);
                if (vr != null && Math.hypot(cx - vr.cx, cz - vr.cz) < vr.Rr + 230) return false;
                SizoGrove.Region sr = SizoGrove.region(Math.floorDiv(cx, SizoGrove.SUPER) + dx, Math.floorDiv(cz, SizoGrove.SUPER) + dz);
                if (sr != null && Math.hypot(cx - sr.cx, cz - sr.cz) < sr.Rr + 230) return false;
                WaterfallGardens.Region gr = WaterfallGardens.region(Math.floorDiv(cx, WaterfallGardens.SUPER) + dx, Math.floorDiv(cz, WaterfallGardens.SUPER) + dz);
                if (gr != null && Math.hypot(cx - gr.cx, cz - gr.cz) < gr.Rr + 230) return false;
            }
        }
        return true;
    }

    public static Region regionAt(double x, double z) {
        int sx = Math.floorDiv((int) Math.floor(x), SUPER), sz = Math.floorDiv((int) Math.floor(z), SUPER);
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) {
                Region rg = region(sx + dx, sz + dz);
                if (rg != null && Math.hypot(x - rg.cx, z - rg.cz) < rg.Rr + 96) return rg;
            }
        return null;
    }

    public static CityPlan.Island landAt(double x, double z) {
        Region rg = regionAt(x, z);
        if (rg == null) return null;
        for (CityPlan.Island ci : rg.isles) {
            if (Math.abs(x - ci.cx) > ci.maxReach() || Math.abs(z - ci.cz) > ci.maxReach()) continue;
            if (ci.t(x, z) >= 0.10) return ci;
        }
        return null;
    }

    public static boolean inside(double x, double z) {
        Region rg = regionAt(x, z);
        if (rg == null) return false;
        if (rg.islandAt(x, z) != null) return true;
        for (Span sp : rg.spans) {
            double[] u = new double[1];
            double d = sp.dist(x, z, u);
            if (d <= sp.halfWidth(u[0]) + 1.0) return true;
        }
        return false;
    }

    /** Блокирует генерацию посторонних диких островков внутри архипелага и его проливов с мостами. */
    public static boolean nearAny(double x, double z, double extraReach, double pad) {
        int sx = Math.floorDiv((int) Math.floor(x), SUPER), sz = Math.floorDiv((int) Math.floor(z), SUPER);
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) {
                Region rg = region(sx + dx, sz + dz);
                if (rg == null) continue;
                if (Math.hypot(x - rg.cx, z - rg.cz) < rg.Rr + extraReach + pad) return true;
            }
        return false;
    }

    /** Плавный вес атмосферы биома [0..1] с мягким затуханием ~92 блока от кромки островов и мостов. */
    public static float skyWeight(double x, double z) {
        int sx = Math.floorDiv((int) Math.floor(x), SUPER), sz = Math.floorDiv((int) Math.floor(z), SUPER);
        double best = 0.0;
        for (int dx = -1; dx <= 1; ++dx)
            for (int dz = -1; dz <= 1; ++dz) {
                Region rg = region(sx + dx, sz + dz);
                if (rg == null) continue;
                if (Math.hypot(x - rg.cx, z - rg.cz) > rg.Rr + 150) continue;
                for (CityPlan.Island ci : rg.isles) {
                    double d = Math.hypot(x - ci.cx, z - ci.cz) - ci.R * 0.92;
                    double u = Math.max(0.0, Math.min(1.0, 1.0 - d / 92.0));
                    double w = u * u * (3.0 - 2.0 * u);
                    if (w > best) best = w;
                }
            }
        return (float) best;
    }
}
