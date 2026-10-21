package ru.aiforge.riftworld.tree;

import java.util.HashMap;
import java.util.Map;

/**
 * Rev 7 part 2: the tree engine. Replaces the old «ball on a stick» with real trees:
 *
 * <ul>
 *   <li>a conical trunk that leans and wobbles, thicker at the roots (2x2 for the giant);</li>
 *   <li>real branches reaching into every crown tier;</li>
 *   <li>crown tiers built as squashed clusters with a ragged, noise-broken rim, drooping skirts and hanging vines;</li>
 *   <li>species silhouettes: layered giant grove tree, lilac ash, fern tree with fronds, conifer, broadleaf.</li>
 * </ul>
 *
 * <p>Everything is derived from (kind, height, seed), so generation across chunks is deterministic and identical.
 * The reference implementation used for the offline photo mock is {@code tools/rift/tree_gen.py} - the formulas here
 * match it 1:1.</p>
 */
public final class TreeGen {
    public static final int LOG = 0, LEAF = 1, ACCENT = 2;
    public static final int AXIS_Y = 0, AXIS_X = 1, AXIS_Z = 2;

    /** Receives one block of a tree; coordinates are relative to the trunk base (dy = 0 is the block above ground). */
    public interface Placer {
        void place(int dx, int dy, int dz, int kind, int axis);
    }

    private static final class Spec {
        final int hmin, hmax;
        final double trunk, lean, squash, droop, accent, skirt, vines;
        final int tiers, branches, fronds;
        final double[] crown;
        final boolean cone;

        Spec(int hmin, int hmax, double trunk, double lean, int tiers, double[] crown, double squash, double droop,
             int branches, double accent, double skirt, double vines, int fronds, boolean cone) {
            this.hmin = hmin;
            this.hmax = hmax;
            this.trunk = trunk;
            this.lean = lean;
            this.tiers = tiers;
            this.crown = crown;
            this.squash = squash;
            this.droop = droop;
            this.branches = branches;
            this.accent = accent;
            this.skirt = skirt;
            this.vines = vines;
            this.fronds = fronds;
            this.cone = cone;
        }
    }

    private static final Map<Integer, Spec> SPECS = new HashMap<>();

    static {
        SPECS.put(TreeSpecies.SIZO, new Spec(10, 13, 0.0, 1.0, 3, new double[] {3.4, 2.7, 1.8}, 0.30, 1.2, 3, 0.06, 0.34, 0.05, 0, false));
        SPECS.put(TreeSpecies.SIZO_GIANT, new Spec(17, 22, 1.5, 1.4, 4, new double[] {5.6, 4.8, 3.8, 2.2}, 0.26, 2.0, 4, 0.05, 0.42, 0.10, 0, false));
        SPECS.put(TreeSpecies.LILAC, new Spec(12, 17, 0.0, 1.6, 4, new double[] {4.6, 4.2, 3.4, 2.2}, 0.30, 1.6, 4, 0.20, 0.34, 0.03, 0, false));
        SPECS.put(TreeSpecies.AMBRA, new Spec(7, 10, 0.0, 1.0, 3, new double[] {3.4, 2.6, 1.8}, 0.36, 0.5, 2, 0.02, 0.26, 0.0, 0, false));
        SPECS.put(TreeSpecies.OAK, new Spec(8, 12, 0.0, 1.2, 3, new double[] {3.9, 3.3, 2.3}, 0.34, 1.0, 3, 0.03, 0.32, 0.02, 0, false));
        SPECS.put(TreeSpecies.FERN, new Spec(7, 10, 0.0, 1.6, 0, new double[] {3.4}, 0.55, 0.8, 0, 0.10, 0.22, 0.22, 7, false));
        SPECS.put(TreeSpecies.MIST_PINE, new Spec(14, 20, 0.0, 0.8, 0, new double[] {3.8}, 0.60, 0.0, 0, 0.03, 0.14, 0.0, 0, true));
    }

    // ------------------------------------------------------------------ deterministic noise
    public static int hash(int a, int b, int c) {
        int h = a * 374761393 + b * 668265263 + c * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    private static double rnd(int a, int b, int c) {
        return (hash(a, b, c) & 0xFFFFFF) / (double) 0xFFFFFF;
    }

    /** Clamped crown height H of a species; the trunk itself reaches H + 1. */
    public static int trunkHeight(int kind, int h) {
        Spec sp = SPECS.get(kind);
        if (sp == null) return h;
        return Math.max(sp.hmin, Math.min(sp.hmax, h));
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFF) << 40) | ((long) (y & 0xFFF) << 28) | ((long) (z & 0x3FFFF) << 6);
    }

    /** Builds one tree and reports each block. */
    public static void build(int kind, int height, int seed, Placer out) {
        Spec sp = SPECS.get(kind);
        if (sp == null) return;
        java.util.LinkedHashMap<Long, int[]> nodes = new java.util.LinkedHashMap<>(512);

        final int H = Math.max(sp.hmin, Math.min(sp.hmax, height));
        final int trunkTop = H + 1;
        final double[][] centers = new double[trunkTop + 2][2];

        double leanX = (rnd(seed, 1, 977) - 0.5) * 2 * sp.lean;
        double leanZ = (rnd(seed, 2, 977) - 0.5) * 2 * sp.lean;
        double wobPhaseX = rnd(seed, 3, 977) * 6.283;
        double wobPhaseZ = rnd(seed, 4, 977) * 6.283;

        // ----- trunk: cone with a lean and a wobble
        for (int y = 0; y <= trunkTop; ++y) {
            double t = y / (double) Math.max(1, trunkTop);
            double cx = leanX * t + 0.45 * Math.sin(t * 3.4 + wobPhaseX) * (1 - t * 0.35);
            double cz = leanZ * t + 0.45 * Math.cos(t * 3.0 + wobPhaseZ) * (1 - t * 0.35);
            centers[y][0] = cx;
            centers[y][1] = cz;
            double baseRad = sp.trunk >= 1.0 ? sp.trunk : (H >= 12 ? 0.75 : 0.55);
            double rad = baseRad * (1.0 - 0.52 * t) + 0.08;
            int cells = rad > 0.2 ? (int) Math.ceil(rad) : 0;
            int bx = (int) Math.round(cx), bz = (int) Math.round(cz);
            if (cells == 0) {
                nodes.put(key(bx, y, bz), new int[] {LOG, AXIS_Y});          // 1x1 trunk core always exists
            } else {
                for (int ox = -cells; ox <= cells; ++ox)
                    for (int oz = -cells; oz <= cells; ++oz)
                        if ((ox + 0.5) * (ox + 0.5) + (oz + 0.5) * (oz + 0.5) <= Math.max(0.50, rad * rad + 0.35))   // 0.50 keeps the centre cell: no trunk hole on taper
                            nodes.put(key(bx + ox, y, bz + oz), new int[] {LOG, AXIS_Y});
            }
        }
        int topX = (int) Math.round(centers[trunkTop][0]), topZ = (int) Math.round(centers[trunkTop][1]);
        // the crowns above the trunk top reuse the last filled centre; without this they sit at (0,0)
        centers[trunkTop + 1][0] = centers[trunkTop][0];
        centers[trunkTop + 1][1] = centers[trunkTop][1];

        // ----- crown: limbs (broadleaf). A limb is a real log reaching out of the trunk; its tip carries a
        //       ragged leaf cloud, so the silhouette is a union of clouds - the Dynamic Trees look.
        if (sp.tiers > 0) {
            int n = sp.tiers;
            for (int i = 0; i < n; ++i) {
                double frac = n > 1 ? 0.40 + 0.52 * (i / (double) (n - 1)) : 0.75;
                int y0 = (int) Math.round(trunkTop * frac);
                double rad = sp.crown[Math.min(i, sp.crown.length - 1)];
                int count = sp.branches > 0 ? sp.branches : 3;
                int ci0 = Math.max(0, Math.min(centers.length - 1, y0));
                for (int b = 0; b < count; ++b) {
                    double a = (b * 2 * Math.PI / count) + (i % 2 == 0 ? 0.0 : Math.PI / count) + rnd(seed, i, 70 + b) * 0.55;
                    int len = Math.max(2, (int) Math.round(rad * (0.72 + 0.30 * rnd(seed, i, 71 + b))));
                    double rise = 0.50 + 0.35 * rnd(seed, i, 75 + b);
                    double ax0 = centers[ci0][0], az0 = centers[ci0][1];
                    for (int k = 1; k <= len; ++k) {
                        int bx = (int) Math.round(ax0 + Math.cos(a) * k);
                        int bz = (int) Math.round(az0 + Math.sin(a) * k);
                        int by = y0 + (int) Math.round(k * rise);
                        int axis = Math.abs(Math.cos(a)) > Math.abs(Math.sin(a)) ? AXIS_X : AXIS_Z;
                        nodes.put(key(bx, by, bz), new int[] {LOG, axis});
                    }
                    double tipX = ax0 + Math.cos(a) * len;
                    double tipZ = az0 + Math.sin(a) * len;
                    double tipY = y0 + len * rise;
                    double br = 1.9 + 0.85 * rnd(seed, i, 76 + b) * (rad / 4.0);
                    blob(nodes, seed, tipX, tipY, tipZ, br, Math.max(1.4, br * (0.65 + sp.squash)),
                            br * (0.85 + 0.3 * rnd(seed, i, 77 + b)), i * 31 + b, sp.accent);
                    // a fork: a shorter side limb with its own smaller cloud
                    if (rnd(seed, i, 78 + b) < 0.55) {
                        double a2 = a + (rnd(seed, i, 79 + b) > 0.5 ? 0.7 : -0.7);
                        int k0 = Math.max(1, len / 2);
                        for (int k = 1; k <= len - k0; ++k) {
                            int bx = (int) Math.round(ax0 + Math.cos(a) * k0 + Math.cos(a2) * k);
                            int bz = (int) Math.round(az0 + Math.sin(a) * k0 + Math.sin(a2) * k);
                            int by = y0 + (int) Math.round((k0 + k) * rise);
                            int axis = Math.abs(Math.cos(a2)) > Math.abs(Math.sin(a2)) ? AXIS_X : AXIS_Z;
                            nodes.put(key(bx, by, bz), new int[] {LOG, axis});
                        }
                        double fx = ax0 + Math.cos(a) * k0 + Math.cos(a2) * (len - k0);
                        double fz = az0 + Math.sin(a) * k0 + Math.sin(a2) * (len - k0);
                        double fy = y0 + len * rise;
                        double fr = br * 0.72;
                        blob(nodes, seed, fx, fy, fz, fr, Math.max(1.2, fr * 0.8), fr, i * 31 + b + 500, sp.accent);
                    }
                }
            }
            // the leader: a cloud right on top of the trunk
            blob(nodes, seed, centers[trunkTop][0], trunkTop + 1, centers[trunkTop][1],
                    sp.crown[sp.crown.length - 1] * 0.95, Math.max(1.6, sp.crown[sp.crown.length - 1] * 0.8),
                    sp.crown[sp.crown.length - 1] * 0.95, 999, sp.accent);
        }

        // ----- fern: umbrella fronds
        if (sp.fronds > 0) {
            for (int i = 0; i < sp.fronds; ++i) {
                double a = i * 2 * Math.PI / sp.fronds + rnd(seed, i, 901) * 0.5;
                int len = (int) Math.round(2.8 + rnd(seed, i, 902) * 1.8);
                int axis = Math.abs(Math.cos(a)) > Math.abs(Math.sin(a)) ? AXIS_X : AXIS_Z;
                int px0 = topX, py0 = trunkTop - 1, pz0 = topZ;      // the rachis starts at the trunk apex
                for (int k = 1; k <= len; ++k) {
                    int bx = (int) Math.round(topX + Math.cos(a) * k);
                    int bz = (int) Math.round(topZ + Math.sin(a) * k);
                    int by = trunkTop - 1 + (int) Math.round(Math.sin(Math.PI * k / (len + 1.0)) * 2.0) - (k * 2) / 3;
                    // walk one block at a time, so rounding can never tear the frond apart
                    int steps = Math.max(Math.abs(bx - px0), Math.max(Math.abs(by - py0), Math.abs(bz - pz0)));
                    for (int j = 1; j <= steps; ++j) {
                        double t = j / (double) steps;
                        nodes.put(key((int) Math.round(px0 + (bx - px0) * t),
                                        (int) Math.round(py0 + (by - py0) * t),
                                        (int) Math.round(pz0 + (bz - pz0) * t)),
                                new int[] {LOG, axis});
                    }
                    px0 = bx; py0 = by; pz0 = bz;
                    for (int s = -1; s <= 1; s += 2) {
                        int lx = (int) Math.round(topX + Math.cos(a) * k + Math.cos(a + Math.PI / 2) * s * 0.9);
                        int lz = (int) Math.round(topZ + Math.sin(a) * k + Math.sin(a + Math.PI / 2) * s * 0.9);
                        putSoft(nodes, lx, k > len - 2 ? by - 1 : by, lz, rnd(seed, i * 7 + k, s + 3) > 0.12 ? LEAF : ACCENT);
                    }
                    putSoft(nodes, bx, k < len - 1 ? by + 1 : by, bz, LEAF);
                }
            }
            crown(nodes, seed, centers, trunkTop, 1.6, 1.6, 0.6, 0.3, 98, sp.accent, sp.droop, sp.vines, 0, 0);
        }

        // ----- conifer: stacked rings shrinking to the tip
        if (sp.cone) {
            int baseY = (int) (trunkTop * 0.30);
            int tier = 0;
            for (int y = baseY; y <= trunkTop + 1; y += 2, ++tier) {
                double t = (y - baseY) / (double) Math.max(1, trunkTop + 1 - baseY);
                double rad = sp.crown[0] * (1.0 - t) + 0.9;
                crown(nodes, seed, centers, y, rad, rad, sp.squash, sp.skirt, 200 + tier, sp.accent, 0, sp.vines, 0, 0);
            }
            crown(nodes, seed, centers, trunkTop + 2, 0.9, 0.9, 0.8, 0.1, 299, sp.accent, 0, 0, 0, 0);
            // the tip must touch the ring below, otherwise a single leaf block floats in the air
            int tipX = (int) Math.round(centers[trunkTop][0]), tipZ = (int) Math.round(centers[trunkTop][1]);
            for (int y = trunkTop - 1; y <= trunkTop + 2; ++y) putSoft(nodes, tipX, y, tipZ, LEAF);
        }

        for (Map.Entry<Long, int[]> e : nodes.entrySet()) {
            long k = e.getKey();
            int x = (int) ((k >> 40) & 0x3FFFF);
            int y = (int) ((k >> 28) & 0xFFF);
            int z = (int) ((k >> 6) & 0x3FFFF);
            if (x >= 0x20000) x -= 0x40000;
            if (z >= 0x20000) z -= 0x40000;
            out.place(x, y, z, e.getValue()[0], e.getValue()[1]);
        }
    }

    /** One crown tier: a squashed ragged cluster + drooping skirt + hanging vines. */
    /** A ragged leaf cloud (the crown is a union of these); mirrors blob() in tools/rift/tree_gen.py. */
    private static void blob(Map<Long, int[]> nodes, int seed, double px, double py, double pz,
                             double rx, double ry, double rz, int tag, double accent) {
        int ix = (int) Math.round(px), iy = (int) Math.round(py), iz = (int) Math.round(pz);
        for (int oy = -(int) Math.ceil(ry); oy <= (int) Math.ceil(ry); ++oy)
            for (int ox = -(int) Math.ceil(rx); ox <= (int) Math.ceil(rx); ++ox)
                for (int oz = -(int) Math.ceil(rz); oz <= (int) Math.ceil(rz); ++oz) {
                    double q = (ox * ox) / (rx * rx) + (oy * oy) / (ry * ry) + (oz * oz) / (rz * rz);
                    if (q > 1.0) continue;
                    if (q > 0.45 && rnd(seed, ix + ox * 17 + tag, (iz + oz * 13) * 7919 + iy + oy * 29) < 0.40) continue;
                    if (q < 0.22 && rnd(seed, ix + ox * 7, (iz + oz * 11) * 7919 + iy + oy * 5) < 0.30) continue;
                    int k = (accent > 0 && q > 0.45 && rnd(seed, ox * 13 + tag, oz * 29 + oy) < accent) ? ACCENT : LEAF;
                    putSoft(nodes, ix + ox, iy + oy, iz + oz, k);
                }
    }

    /** A small ragged leaf cloud - one per branch tip. Mirrors puff() in tools/rift/tree_gen.py. */
    private static void puff(Map<Long, int[]> nodes, int seed, int px, int py, int pz,
                             double rx, double ry, double rz, int tag) {
        for (int oy = -(int) ry; oy <= (int) ry; ++oy)
            for (int ox = -(int) rx; ox <= (int) rx; ++ox)
                for (int oz = -(int) rz; oz <= (int) rz; ++oz) {
                    if ((ox * ox) / (rx * rx) + (oy * oy) / (ry * ry) + (oz * oz) / (rz * rz) > 1.0) continue;
                    if (rnd(seed, px + ox * 23 + tag, (py + oy * 31) * 7919 + pz + oz * 19) < 0.42) continue;
                    putSoft(nodes, px + ox, py + oy, pz + oz, LEAF);
                }
    }

    private static void crown(Map<Long, int[]> nodes, int seed, double[][] centers, int cy, double rx, double rz,
                              double squash, double ragged, int tier, double accent, double droop, double vines,
                              double jx, double jz) {
        double ry = Math.max(1.0, rx * squash);
        int R = (int) Math.ceil(Math.max(rx, rz)) + 1;
        int ci = Math.max(0, Math.min(centers.length - 1, cy));
        double cx = centers[ci][0] + jx, cz = centers[ci][1] + jz;
        int bx0 = (int) Math.round(cx), bz0 = (int) Math.round(cz);
        for (int oy = -(int) ry - 1; oy <= (int) ry + 1; ++oy)
            for (int ox = -R; ox <= R; ++ox)
                for (int oz = -R; oz <= R; ++oz) {
                    double q = (ox * ox) / (rx * rx) + (oy * oy) / (ry * ry) + (oz * oz) / (rz * rz);
                    if (q > 1.0) continue;
                    boolean edge = q > 0.64;
                    // canopy = a THIN SHELL (like Dynamic Trees' cloud layers): the hollow core keeps the trunk visible
                    if (q < 0.30) continue;
                    if (edge && rnd(seed, ox * 31 + cy * 7 + tier, oz * 17 + oy) < ragged) continue;
                    if (!edge && rnd(seed, ox * 7 + cy * 3 + tier, oz * 11 + oy * 5) < 0.32) continue;
                    int k = LEAF;
                    if (edge && accent > 0 && rnd(seed, ox * 13 + tier, oz * 29 + oy) < accent) k = ACCENT;
                    putSoft(nodes, bx0 + ox, cy + oy, bz0 + oz, k);
                }
        if (droop > 0) {
            int n = 12 + (int) (rnd(seed, tier, 41) * 8);
            for (int i = 0; i < n; ++i) {
                double a = i * 2 * Math.PI / n + rnd(seed, tier, 42) * 0.4;
                double rr = rx * (0.72 + 0.22 * rnd(seed, tier, 43 + i));
                int bx = (int) Math.round(cx + Math.cos(a) * rr);
                int bz = (int) Math.round(cz + Math.sin(a) * rr);
                int ln = 1 + (int) (rnd(seed, tier, 44 + i) * droop);
                for (int k2 = 1; k2 <= ln; ++k2) putSoft(nodes, bx, cy - k2, bz, LEAF);
            }
        }
        if (vines > 0) {
            for (int i = 0; i < 8; ++i) {
                if (rnd(seed, tier, 51 + i) > vines) continue;
                double a = i * 2 * Math.PI / 8;
                int bx = (int) Math.round(cx + Math.cos(a) * rx * 0.85);
                int bz = (int) Math.round(cz + Math.sin(a) * rz * 0.85);
                int ln = 1 + (int) (rnd(seed, tier, 61 + i) * 3);
                for (int k2 = 1; k2 <= ln; ++k2) putSoft(nodes, bx, cy - (int) droop - k2, bz, ACCENT);
            }
        }
    }

    private static void putSoft(Map<Long, int[]> nodes, int x, int y, int z, int kind) {
        long k = key(x, y, z);
        int[] cur = nodes.get(k);
        if (cur != null && cur[0] == LOG) return;         // logs always win over leaves
        nodes.put(k, new int[] {kind, AXIS_Y});
    }

    private TreeGen() {
    }
}
