package ru.aiforge.riftworld.world;

import java.util.Random;

/** Compare the cached generator against the original analytical rules, including signed coords. */
public final class SeaWorldgenParityProbe {
    private static void check(boolean value, String label, int x, int y, int z) {
        if (!value) throw new AssertionError(label + " at " + x + "," + y + "," + z);
    }
    public static void main(String[] args) {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        Random random = new Random(0x5ea40L);
        int sea = CityPlan.get().ocean, caves = 0, shafts = 0, pockets = 0;
        for (int sample = 0; sample < 420; ++sample) {
            int x = random.nextInt(10001) - 5000, z = random.nextInt(10001) - 5000;
            if (sample < 140) { x = 1664 + random.nextInt(112); z = 2064 + random.nextInt(112); }
            int floor = ElisarSea.floor(x, z, sea);
            ElisarSea.CaveColumn cached = new ElisarSea.CaveColumn(x, z, floor);
            for (int y = ElisarSea.BOTTOM; y <= floor + 1; ++y) {
                boolean originalCave = ElisarSea.cave(x, y, z, floor);
                boolean originalBlue = ElisarSea.blueHole(x, y, z, floor);
                boolean originalExit = ElisarSea.seaCaveExit(x, y, z, floor);
                check(cached.caveAt(y) == originalCave, "cave", x, y, z);
                check(cached.blueAt(y) == originalBlue, "blue hole", x, y, z);
                check(cached.exitAt(y) == originalExit, "sea exit", x, y, z);
                if (y <= floor - 6 && originalCave && !originalBlue && !originalExit) {
                    boolean p = ElisarSea.dryPocket(x, y, z, floor);
                    check(cached.pocketAt(y) == p, "air pocket", x, y, z);
                    ++caves;
                    if (p) ++pockets;
                }
                if (originalBlue || originalExit) ++shafts;
            }
        }
        if (caves == 0 || shafts == 0 || pockets == 0) throw new AssertionError("test did not exercise all features");
        System.out.println("OK: 420 columns match old cave/shaft/pocket rules; caves=" + caves
                + " shafts=" + shafts + " pockets=" + pockets);
    }
}
