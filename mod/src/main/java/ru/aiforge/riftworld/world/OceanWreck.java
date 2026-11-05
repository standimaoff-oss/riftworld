package ru.aiforge.riftworld.world;

/** Deterministic first-pass wreck plan, evaluated per block and clipped by the caller's chunk.
 * It never changes existing chunks and is outside Maja's reserved open-water core.
 */
public final class OceanWreck {
    public static final int NONE = 0, PLANK = 1, LOG = 2, MOSS = 3, RAIL = 4;
    public static final int OFFSET_X = 600;
    private OceanWreck() {}

    public static int materialAt(int x, int y, int z, int sea) {
        DeepOcean.Region r = DeepOcean.regionAt(x, z);
        int dx = x - (r.x() + OFFSET_X), dz = z - r.z(), dy = y - sea;
        int ax = Math.abs(dx), az = Math.abs(dz);
        if (ax > 28 || az > 9 || dy < -6 || dy > 23) return NONE;
        // Raised broken rails along both surviving halves. The straight fence
        // runs are aligned with the hull, with gaps at the splintered ends.
        if (dy == 5 && az >= 5 && az <= 8 && ax >= 9 && ax <= 23
                && az == Math.max(1, 8 - Math.max(0, ax - 14) / 2)
                && ax % 6 != 0) return RAIL;
        // Fragments in the breach, with space for swimming between them.
        if (dy == -1 && ax <= 4 && az >= 3 && az <= 4 && (dx + dz) % 3 != 0) return LOG;
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
        // Raised, damaged forecastle and sterncastle, with open decks inside.
        if (ax >= 18 && ax <= 25 && az <= width && dy >= 5 && dy <= 7
                && (az >= width - 1 || ax == 25)
                && (dx + 2 * dz + dy) % 7 != 0) return PLANK;
        // Cross beams on the open deck; no block entities or loot in this draft.
        if (dy == 3 && (dx == -18 || dx == -10 || dx == 16 || dx == 22)
                && az < width - 1 && az > 1) return LOG;
        // Broken central mast, a yardarm and a tilted remnant, without sails.
        if (dx == 9 && dz == 0 && dy >= 2 && dy <= 21) return LOG;
        if (dy == 17 && dx >= 4 && dx <= 15 && dz == 0) return LOG;
        if (dx >= -9 && dx <= -4 && dz == 1 && dy == 4 + (dx + 9)) return LOG;
        if (dy == 3 && ax >= 8 && ax <= 23 && az == width) return LOG;
        return NONE;
    }
}
