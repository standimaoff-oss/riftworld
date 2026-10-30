package ru.aiforge.riftworld.world;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Biome 04 «Фиолетовые холмы» (референс 04_violet_hills.png).
 *
 * <p>Извилистая светло-каменная дорога с боковым ответвлением идёт по ложбине между ярусными
 * лавандовыми холмами, вдоль обочин чередуются каменные стенки, деревянные заборы и каменные
 * столбы с факелами/фонарями, а над полями растут деревья аддона DT ({@code riftworlddt:lilac})
 * и кружатся кастомные фиолетовые листья ({@code riftworld:violet_leaf}).
 */
public final class VioletHills {
    public static final int SUPER = 880;
    private static final long SEED = 0x9B1C7L;

    public static final class Region {
        public final int cx, cz;
        public final double Rr;
        public final CityPlan.Island meadow;
        // главная извилистая дорога через ложбину между холмами
        public final double rax, raz, rbx, rbz, curvePhase;
        // боковое ответвление дороги на один из склонов
        public final double bax, baz, bbx, bbz, branchPhase;
        public final int riverLevel;
        public static final double RIVER_HW = 2.35;
        public static final double BRANCH_HW = 1.60;

        Region(int cx, int cz, Random rnd) {
            this.cx = cx; this.cz = cz;
            this.Rr = 155 + rnd.nextDouble() * 40;
            double R = 100 + rnd.nextDouble() * 20;
            double base = CityPlan.get().ocean + 14 + rnd.nextInt(4);
            this.meadow = new CityPlan.Island(cx, cz, R, base, R * 0.6 + 12, rnd.nextDouble() * 6.283, rnd.nextDouble() * 6.283,
                    rnd.nextDouble() * 6.283, 7, 0.9);
            this.meadow.elong = 0.24 + rnd.nextDouble() * 0.18;
            this.meadow.elongAng = rnd.nextDouble() * Math.PI;
            double phi = this.meadow.elongAng + (rnd.nextDouble() - 0.5) * 0.35;
            double rl = R * 1.12;
            double perpOff = (rnd.nextBoolean() ? 1.0 : -1.0) * (10.5 + 3.0 * rnd.nextDouble());
            double rcx = cx - Math.sin(phi) * perpOff, rcz = cz + Math.cos(phi) * perpOff;
            this.rax = rcx - Math.cos(phi) * rl; this.raz = rcz - Math.sin(phi) * rl;
            this.rbx = rcx + Math.cos(phi) * rl; this.rbz = rcz + Math.sin(phi) * rl;
            this.curvePhase = rnd.nextDouble() * Math.PI * 2.0;
            this.branchPhase = rnd.nextDouble() * Math.PI * 2.0;

            double[] fork = mainRoadPoint(0.48);
            double bSide = rnd.nextBoolean() ? 1.0 : -1.0;
            double bAng = phi + bSide * (1.05 + rnd.nextDouble() * 0.30);
            double bl = R * 0.56;
            this.bax = fork[0];
            this.baz = fork[1];
            this.bbx = fork[0] + Math.cos(bAng) * bl;
            this.bbz = fork[1] + Math.sin(bAng) * bl;
            this.riverLevel = (int) Math.floor(base) - 2;
        }

        public double mainRoadLen() {
            return Math.max(1.0, Math.hypot(rbx - rax, rbz - raz));
        }

        private double mainSway(double along, double bx, double bz) {
            return 14.5 * Math.sin(along * 0.041 + curvePhase)
                    + 4.2 * (ElisarChunkGenerator.vnoise(bx * 0.015, bz * 0.015, 173) - 0.5);
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

        /** Расстояние до боковой дорожки на склон холма. */
        public double branchRoadDist(double px, double pz) {
            double dx = bbx - bax, dz = bbz - baz;
            double len = Math.max(1.0, Math.hypot(dx, dz));
            double ux = dx / len, uz = dz / len;
            double nx = -uz, nz = ux;
            double along = Math.max(0.0, Math.min(len, (px - bax) * ux + (pz - baz) * uz));
            double sway = 5.5 * Math.sin(along * 0.07 + branchPhase);
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

        /**
         * Точка на обочине главной дороги на расстоянии {@code along} от начала и поперечном смещении
         * {@code sideOffset} (по нормали к локальной касательной кривой).
         */
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
        if (r.nextDouble() > 0.45) return null;
        int cx = sx * SUPER + 150 + r.nextInt(SUPER - 300), cz = sz * SUPER + 150 + r.nextInt(SUPER - 300);
        if (Math.hypot(cx, cz) < 560) return null;
        if (AshenLands.blocks(cx, cz, 260)) return null;
        EmberRuins.Site es = EmberRuins.nearest(cx, cz);
        if (es != null && Math.hypot(cx - es.cx, cz - es.cz) < 400) return null;
        return new Region(cx, cz, r);
    }

    public static Region regionAt(double x, double z) {
        Region rg = region(Math.floorDiv((int) Math.floor(x), SUPER), Math.floorDiv((int) Math.floor(z), SUPER));
        if (rg == null) return null;
        return Math.abs(x - rg.cx) < rg.Rr + 70 && Math.abs(z - rg.cz) < rg.Rr + 70 ? rg : null;
    }

    public static boolean inside(double x, double z) {
        Region rg = regionAt(x, z);
        if (rg == null) return false;
        CityPlan.Island m = rg.meadow;
        return m.t(x, z) > 0.10 + 8.0 / m.R;
    }

    public static List<CityPlan.Tree> treesNear(int x, int z) {
        return List.of();
    }
}
