package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Temporary site of the castle «Чертог Тлеющего Рога» in Elisar (until the Hell lands biome exists): one scorched island
 * south of the city with the castle template (data/riftworld/structures/castle/castle.nbt) on a flattened plateau,
 * ash / burnt soil ground and leafless charred trees.  The castle front (gatehouse, template z = 0) faces north - the city.
 */
public final class CastleSite {
    public static final int TW = 76, TD = 76;                   // template footprint
    public static final int CX = 40, CZ = 470;                  // island centre
    public static final int Y = 104;                            // island top = template floor layer
    public static final int X0 = CX - 38, Z0 = CZ - 34;         // template origin (min corner)
    public static final double R = 74;
    public static final CityPlan.Island ISLAND = new CityPlan.Island(CX, CZ, R, Y, R * 0.6 + 14, 1.3, 4.1, 2.2, 5, 0.35);
    public static final List<CityPlan.Tree> TREES = new ArrayList<>();

    static {
        Random r = new Random(0xA5E5L);
        for (int k = 0, tries = 0; k < 34 && tries < 600; ++tries) {
            double a = r.nextDouble() * Math.PI * 2, d = 20 + r.nextDouble() * (R - 26);
            int x = (int) Math.floor(CX + Math.cos(a) * d), z = (int) Math.floor(CZ + Math.sin(a) * d);
            if (ISLAND.t(x, z) < 0.12 || inFootprint(x, z, 7)) continue;
            if (Math.abs(x - CX) < 12 && z < Z0) continue;          // keep the approach to the gatehouse clear
            boolean ok = true;
            for (CityPlan.Tree t : TREES) if (Math.abs(t.x - x) < 6 && Math.abs(t.z - z) < 6) { ok = false; break; }
            if (!ok) continue;
            TREES.add(new CityPlan.Tree(x, z, 2, 5 + r.nextInt(6), r.nextInt(1 << 30)));
            ++k;
        }
    }

    private CastleSite() {
    }

    public static boolean inFootprint(double x, double z, double margin) {
        return x >= X0 - margin && x < X0 + TW + margin && z >= Z0 - margin && z < Z0 + TD + margin;
    }

    /** Distance outside the footprint rectangle (0 inside). */
    public static double outside(double x, double z) {
        double dx = Math.max(Math.max(X0 - x, 0), x - (X0 + TW - 1));
        double dz = Math.max(Math.max(Z0 - z, 0), z - (Z0 + TD - 1));
        return Math.hypot(dx, dz);
    }

    public static boolean near(double x, double z, double pad) {
        return Math.abs(x - CX) < R * 1.2 + pad && Math.abs(z - CZ) < R * 1.2 + pad;
    }
}
