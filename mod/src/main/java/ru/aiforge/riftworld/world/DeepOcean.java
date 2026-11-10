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

    /**
     * Reserve an open-water centre for this 322-block creature. This is used
     * only by world generation of new chunks; it never edits existing chunks.
     * Whole-chunk boundaries keep biome, heightmaps and decoration consistent.
     */
    public static boolean openCoreChunk(int blockX, int blockZ) {
        int x0 = Math.floorDiv(blockX, 16) * 16;
        int z0 = Math.floorDiv(blockZ, 16) * 16;
        Region r = regionAt(x0 + 8, z0 + 8);
        long dx = Math.max(Math.abs((long) x0 - r.x()), Math.abs((long) x0 + 15 - r.x()));
        long dz = Math.max(Math.abs((long) z0 - r.z()), Math.abs((long) z0 + 15 - r.z()));
        return dx * dx + dz * dz <= 384L * 384L;
    }

    /** A soft, 180-block coast prevents a cliff where the deeper seafloor begins. */
    public static double weight(int x, int z) {
        Region r = regionAt(x, z);
        double d = Math.hypot((double) x - r.x(), (double) z - r.z());
        double t = Math.max(0.0, Math.min(1.0, (r.radius() - d) / 180.0));
        return t * t * (3.0 - 2.0 * t);
    }

    /** Full 120-block water depth throughout the region, easing outward beyond its boundary. */
    public static double floorBlend(int x, int z) {
        Region r = regionAt(x, z);
        double d = Math.hypot((double) x - r.x(), (double) z - r.z());
        double t = Math.max(0.0, Math.min(1.0, (r.radius() + 180.0 - d) / 180.0));
        return t * t * (3.0 - 2.0 * t);
    }

    /** Floor under 120 blocks of water at sea level 62; leave rock above bedrock. */
    public static int deepFloor(int sea) { return Math.max(-60, sea - 120); }

    public static boolean contains(int x, int z) {
        Region r = regionAt(x, z);
        double dx = (double) x - r.x(), dz = (double) z - r.z();
        return dx * dx + dz * dz < (double) r.radius() * r.radius();
    }
}
