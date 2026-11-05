package ru.aiforge.riftworld.world;

/** Deterministic first-pass wreck plan, evaluated per block and clipped by the caller's chunk.
 * It never changes existing chunks and is outside Maja's reserved open-water core.
 */
public final class OceanWreck {
    public static final int NONE = 0, PLANK = 1, LOG = 2, MOSS = 3;
    public static final int OFFSET_X = 600;
    private OceanWreck() {}

    public static int materialAt(int x, int y, int z, int sea) {
        DeepOcean.Region r = DeepOcean.regionAt(x, z);
        int dx = x - (r.x() + OFFSET_X), dz = z - r.z(), dy = y - sea;
        int ax = Math.abs(dx), az = Math.abs(dz);
        if (ax > 28 || az > 9 || dy < -6 || dy > 23) return NONE;
        // Ragged halves of a long, tapering ship; the gap is intentionally open.
        int width = Math.max(1, 8 - Math.max(0, ax - 14) / 2);
        int top = 4 + Math.max(0, ax - 14) / 5;
        int bottom = -6 + Math.max(0, ax - 17) / 4;
        boolean split = ax <= 3 + (Math.abs(dz) % 2);
        if (!split && ax <= 27 && az <= width && dy >= bottom && dy <= top) {
            int widthAtY = Math.max(1, width - Math.max(0, -3 - dy));
            if (az <= widthAtY && (az >= widthAtY - 1 || dy == bottom || dy == 2)) {
                if (dy <= 0 && (dx * dx + dz * dz + dy * dy) % 17 == 0) return MOSS;
                return dy == bottom || (ax % 7 == 0 && az == widthAtY) ? LOG : PLANK;
            }
        }
        // Broken central mast and a single yardarm, without sails.
        if (dx == 9 && dz == 0 && dy >= 2 && dy <= 21) return LOG;
        if (dy == 17 && dx >= 4 && dx <= 15 && dz == 0) return LOG;
        if (dy == 3 && ax >= 8 && ax <= 23 && az == width) return LOG;
        return NONE;
    }
}
