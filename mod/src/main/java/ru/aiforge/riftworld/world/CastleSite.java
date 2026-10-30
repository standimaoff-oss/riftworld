package ru.aiforge.riftworld.world;

/**
 * Геометрия острова-крепости «Чертог Тлеющего Рога» ({@code data/riftworld/structures/castle/castle.nbt})
 * в каждом случайно сгенерированном регионе биома {@link AshenLands} ({@code riftworld:ashen_lands}).
 */
public final class CastleSite {
    public static final int TW = 76, TD = 76;   // template footprint
    public static final int Y = 104;            // island top = template floor layer
    public static final double R = 74;

    // Ближайший к началу координат регион — для обратной совместимости команд и проб
    public static final int CX = AshenLands.CX, CZ = AshenLands.CZ;
    public static final int X0 = AshenLands.CAPITAL.x0, Z0 = AshenLands.CAPITAL.z0;
    public static final CityPlan.Island ISLAND = AshenLands.CAPITAL.island;

    private CastleSite() {
    }

    /** Остров замка региона адских угодий рядом с (x, z), или null. */
    public static CityPlan.Island islandNear(double x, double z, double pad) {
        AshenLands.Region rg = AshenLands.regionAt(x, z);
        if (rg == null) return null;
        double reach = rg.island.maxReach() + pad;
        return Math.abs(x - rg.cx) < reach && Math.abs(z - rg.cz) < reach ? rg.island : null;
    }

    public static boolean inFootprint(double x, double z, double margin) {
        AshenLands.Region rg = AshenLands.regionAt(x, z);
        return rg != null && rg.inCastleFootprint(x, z, margin);
    }

    /** Distance outside the castle footprint rectangle of the enclosing AshenLands region (0 inside). */
    public static double outside(double x, double z) {
        AshenLands.Region rg = AshenLands.regionAt(x, z);
        return rg == null ? 1e9 : rg.castleOutside(x, z);
    }

    public static boolean near(double x, double z, double pad) {
        return islandNear(x, z, pad) != null;
    }
}
