package ru.aiforge.riftworld.world;

/**
 * Large, deterministic ocean basins in Elisar.  Centres are roughly 4096 blocks
 * apart, independent of chunk generation order and the client's render distance.
 * This class is deliberately pure so the region rules can be tested offline.
 */
public final class DeepOcean {
    public static final int CELL = 4096;
    /** Unique biome colour, also used to select the very subtle custom-fluid tint. */
    public static final int WATER_COLOR = 0x142532;

    private DeepOcean() {}

    public record Region(int cellX, int cellZ, int x, int z, int radius) {
        public long key() {
            return ((long) cellX << 32) ^ (cellZ & 0xffffffffL);
        }
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    public static Region region(int cellX, int cellZ) {
        long h = mix((long) cellX * 0x9e3779b97f4a7c15L ^ (long) cellZ * 0xc2b2ae3d27d4eb4fL ^ 0x4d414a414f434541L);
        int dx = (int) Math.floorMod(h, 701L) - 350;
        int dz = (int) Math.floorMod(h >>> 20, 701L) - 350;
        int radius = 900 + (int) Math.floorMod(h >>> 40, 201L);
        return new Region(cellX, cellZ, cellX * CELL + CELL / 2 + dx,
                cellZ * CELL + CELL / 2 + dz, radius);
    }

    public static Region regionAt(int x, int z) {
        return region(Math.floorDiv(x, CELL), Math.floorDiv(z, CELL));
    }

    /** A soft, 180-block coast prevents a cliff where the deeper seafloor begins. */
    public static double weight(int x, int z) {
        Region r = regionAt(x, z);
        double d = Math.hypot((double) x - r.x(), (double) z - r.z());
        double t = Math.max(0.0, Math.min(1.0, (r.radius() - d) / 180.0));
        return t * t * (3.0 - 2.0 * t);
    }

    public static boolean contains(int x, int z) {
        Region r = regionAt(x, z);
        double dx = (double) x - r.x(), dz = (double) z - r.z();
        return dx * dx + dz * dz < (double) r.radius() * r.radius();
    }
}
