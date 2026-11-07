package ru.aiforge.riftworld.world;

/** A deterministic chunk-clipped plan for an ocean wreck. Only newly generated
 * chunks are touched, outside Maja's reserved open-water core. */
public final class OceanWreck {
    public static final int NONE = 0, PLANK = 1, LOG = 2, MOSS = 3, RAIL = 4, DARK_PLANK = 5;
    public static final int OFFSET_X = 600;
    private OceanWreck() {}

    public static int materialAt(int x, int y, int z, int sea) {
        DeepOcean.Region r = DeepOcean.regionAt(x, z);
        int dx = x - (r.x() + OFFSET_X), dz = z - r.z(), dy = y - sea;
        int ax = Math.abs(dx), az = Math.abs(dz);
        if (ax > 28 || az > 9 || dy < -6 || dy > 23) return NONE;

        // The broken mast leans towards the bow, with block-connected steps.
        int mx = 9 + Math.max(0, dy - 4) / 4;
        if (dz == 0 && dy >= 2 && dy <= 21
                && (dx == mx || (dy > 4 && (dy - 4) % 4 == 0 && dx == mx - 1))) return LOG;
        if (dy == 17 && dx >= 7 && dx <= 19 && dz == 0) return LOG;
        if (dx >= -9 && dx <= -4 && dz == 1 && dy == 4 + (dx + 9)) return LOG;

        // Fore and aft are deliberately different. The narrow forward end
        // tapers more sharply; the upper planks descend into a jagged breach.
        int width = 8 - Math.max(0, ax - (dx < 0 ? 13 : 17)) / (dx < 0 ? 2 : 3);
        width = Math.max(1, width);
        int bottom = -6 + Math.max(0, ax - 16) / 4;
        int top = Math.min(7, 1 + Math.max(0, ax - 4) / 4);
        int damage = Math.floorMod(dx * 31 + dz * 17, 7);
        if (az >= width - 1 && ax < 13 && damage < 3) top--;
        // The breach meanders sideways, opens towards the waterline, and
        // preserves a few ribs and remnants below rather than a clean cut.
        int gap = dy < -3 ? 1 : (dy < 1 ? 2 : 3 + Math.floorMod(dz, 3));
        boolean broken = ax <= gap;
        if (dy == -1 && ax <= 4 && az >= 3 && az <= 4 && (dx + dz) % 3 != 0) return LOG;
        if (broken) return NONE;

        int widthAtY = Math.max(1, width - Math.max(0, (1 - dy) / 2));
        if (ax <= 27 && az <= widthAtY && dy >= bottom && dy <= top) {
            boolean outside = az >= widthAtY - 1;
            boolean hole = outside && ax < 16 && dy >= 0 && damage == 0 && dy != 2;
            if (!hole && (outside || dy == bottom || dy == 2)) {
                if (dy <= 0 && Math.floorMod(dx * dx + dz * dz + dy * dy, 17) == 0) return MOSS;
                if (dy == bottom || (ax % 7 == 0 && az == widthAtY)) return LOG;
                return dy <= 0 ? DARK_PLANK : PLANK;
            }
        }
        // Damaged rails and raised aft/forecastle planks.
        if (dy == top + 1 && az == width && ax >= 12 && ax <= 25
                && ax % 6 != 0 && damage != 0) return RAIL;
        if (ax >= 18 && ax <= 25 && az <= width && dy >= 5 && dy <= 7
                && (az >= width - 1 || ax == 25)
                && Math.floorMod(dx + 2 * dz + dy, 7) != 0) return PLANK;
        if (dy == 3 && (dx == -18 || dx == -10 || dx == 16 || dx == 22)
                && az < width - 1 && az > 1) return LOG;
        return NONE;
    }
}
