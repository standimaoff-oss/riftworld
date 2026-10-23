package ru.aiforge.riftworld.bridge;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Rev 7: math of the REAL suspension footbridge. A deck is defined by FOUR posts (Rev 4 item 13: cables are tied
 * lengthwise first «в длину», then the rectangle closes «в ширину»). Nothing here assumes a cardinal axis - the bridge
 * may stand at ANY angle and the two ends may sit on different heights.
 *
 * <p>Geometry of one span (as on the user's photo):</p>
 * <ul>
 *   <li>the DECK is anchored low on the posts ({@link #DECK_ANCHOR}) and sags between them;</li>
 *   <li>the MAIN CABLE on each side is anchored on the post caps ({@link #CABLE_ANCHOR}), sags a bit less and carries
 *       vertical hangers down to the deck edge;</li>
 *   <li>a diamond safety net fills the band between the hand cable and the deck edge.</li>
 * </ul>
 *
 * <p>Collision is not "a plate per block any more": {@link #collisionCells()} samples the sagging surface every quarter
 * of a block and returns a per-cell top height in 1/16 steps, so walking feels continuous even at 20-30 degrees.</p>
 */
public final class BridgeDeckGeom {
    public static final int MAX_LEN = 26;
    public static final int MAX_WID = 6;
    /** Distance between the two plank rows (render only, in blocks). */
    public static final double PLANK_STEP = 0.5;
    /** Deck surface height above the post base (the anchor of the deck ends). */
    public static final double DECK_ANCHOR = 0.22;
    /** Main cable height above the post base (the post cap). */
    public static final double CABLE_ANCHOR = 1.62;
    /** Plank thickness (blocks). */
    public static final double PLANK_TH = 0.11;

    /** A built deck: the 4 corner posts. a -> b is the bridge DIRECTION; a -> a2 is the deck WIDTH. */
    public record Deck(BlockPos a, BlockPos b, BlockPos a2, BlockPos b2, int styleId, float breakFrom, float breakTo) {

        public BridgeStyle style() {
            return BridgeStyle.of(styleId);
        }

        // ----- anchors -------------------------------------------------------------------------------------
        /** Deck-end point on a post (low, where the walking surface is tied). */
        private static Vec3 post(BlockPos p, double dy) {
            return new Vec3(p.getX() + 0.5, p.getY() + dy, p.getZ() + 0.5);
        }

        public Vec3 tipA() { return post(a, DECK_ANCHOR); }
        public Vec3 tipB() { return post(b, DECK_ANCHOR); }
        public Vec3 tipA2() { return post(a2, DECK_ANCHOR); }
        public Vec3 tipB2() { return post(b2, DECK_ANCHOR); }

        public Vec3 cableA() { return post(a, CABLE_ANCHOR); }
        public Vec3 cableB() { return post(b, CABLE_ANCHOR); }
        public Vec3 cableA2() { return post(a2, CABLE_ANCHOR); }
        public Vec3 cableB2() { return post(b2, CABLE_ANCHOR); }

        public double len() { return tipA().distanceTo(tipB()); }

        public double width() { return tipA().distanceTo(tipA2()); }

        /** Deck sag (blocks) - grows with length, wear and a broken span. */
        public double sag() {
            // a shallow catenary as on the reference photo: ~2 % of the span, a bit more on worn/burnt decks
            double s = len() * 0.0205 * style().sagMul;
            if (isBroken()) s *= 1.30;
            return Math.min(1.25, Math.max(0.16, s));
        }

        /** Main-cable sag: the cable is tighter than the deck (it carries it). */
        public double cableSag() {
            return sag() * 0.78;
        }

        public boolean isBroken() {
            return breakTo > breakFrom;
        }

        /** True inside the missing span of a collapsed bridge. */
        public boolean open(double u) {
            return isBroken() && u > breakFrom && u < breakTo;
        }

        /** Width direction (horizontal, a -> a2 flattened). */
        public Vec3 widthDir() {
            Vec3 w = tipA2().subtract(tipA());
            if (w.lengthSqr() < 1e-6) w = tipB2().subtract(tipB());
            w = new Vec3(w.x, 0, w.z);
            return w.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : w.normalize();
        }

        // ----- curves --------------------------------------------------------------------------------------
        /** Deck centre point at u (0..1) - the sagging catenary. */
        public Vec3 pt(double u) {
            Vec3 p = tipA().lerp(tipB(), u);
            return new Vec3(p.x, p.y - sag() * 4 * u * (1 - u), p.z);
        }

        /** Deck surface point at (u, w): w = 0 is the a-side edge, w = 1 the a2-side edge. */
        public Vec3 pt(double u, double w) {
            Vec3 p1 = pt(u);
            Vec3 p2 = tipA2().lerp(tipB2(), u);
            p2 = new Vec3(p2.x, p2.y - sag() * 4 * u * (1 - u), p2.z);
            return p1.lerp(p2, w);
        }

        /** Hand-cable point at u on side 0 (a-side) or 1 (a2-side). */
        public Vec3 railTop(double u, int side) {
            Vec3 from = side == 0 ? cableA() : cableA2();
            Vec3 to = side == 0 ? cableB() : cableB2();
            Vec3 p = from.lerp(to, u);
            // the cable hangs from the caps: shallow catenary, never below the handrail line
            double sag = cableSag() * 4 * u * (1 - u);
            return new Vec3(p.x, p.y - sag, p.z);
        }

        /** Mid strand (the second cable of the safety net) - follows the deck a bit closer. */
        public Vec3 railMid(double u, int side) {
            Vec3 deckP = pt(u, side);
            Vec3 top = railTop(u, side);
            return new Vec3((deckP.x + top.x) / 2, deckP.y + (top.y - deckP.y) * 0.55, (deckP.z + top.z) / 2);
        }

        /** Deck edge point (where hangers land). */
        public Vec3 edge(double u, int side) {
            return pt(u, side);
        }

        /** u samples for the vertical hangers. */
        public double[] hangerUs() {
            int n = Math.max(2, (int) Math.round(len() / 1.25));
            double[] out = new double[n + 1];
            for (int i = 0; i <= n; ++i) out[i] = i / (double) n;
            return out;
        }

        /** Plank index / count of the planks along the span. */
        public int plankCount() {
            return Math.max(2, (int) Math.ceil(len() / PLANK_STEP));
        }

        /** A plank is really there (wear + the broken gap). */
        public boolean plankAt(double u) {
            if (open(u)) return false;
            int h = (int) Math.floor(u * plankCount());
            int r = mix(h, a.getX() * 31 + a.getZ() * 17, styleId);
            return (r & 0xFF) / 255.0 >= style().holes;
        }

        private static int mix(int a, int b, int c) {
            int h = a * 374761393 + b * 668265263 + c * 2147483647;
            h = (h ^ (h >>> 13)) * 1274126177;
            return h ^ (h >>> 16);
        }

        // ----- collision -----------------------------------------------------------------------------------
        /**
         * Invisible collision cells: block pos -> top height in 1/16 of a block (1..16). Quarter-block sampling keeps
         * the walking surface continuous on steep and diagonal spans.
         */
        public Map<BlockPos, Integer> collisionCells() {
            Map<BlockPos, Integer> out = new LinkedHashMap<>();
            int steps = Math.max(4, (int) Math.ceil(len() / 0.25));
            for (int i = 0; i <= steps; ++i) {
                double u = i / (double) steps;
                if (open(u)) continue;
                for (int j = 0; j <= 4; ++j) {
                    Vec3 p = pt(u, j / 4.0);
                    double top = p.y;
                    int cellY = (int) Math.floor(top - 1.0E-3);
                    int frac = (int) Math.round((top - cellY) * 16.0);
                    if (frac < 2) { cellY -= 1; frac = 16; }
                    frac = Math.max(2, Math.min(16, frac));
                    BlockPos bp = new BlockPos((int) Math.floor(p.x), cellY, (int) Math.floor(p.z));
                    out.merge(bp, frac, Math::max);
                }
            }
            return out;
        }

        /** Axis bounds of the whole swinging volume (client-side stir detection). */
        public net.minecraft.world.phys.AABB stirBox() {
            double minX = Math.min(Math.min(a.getX(), b.getX()), Math.min(a2.getX(), b2.getX())) - 1;
            double minZ = Math.min(Math.min(a.getZ(), b.getZ()), Math.min(a2.getZ(), b2.getZ())) - 1;
            double maxX = Math.max(Math.max(a.getX(), b.getX()), Math.max(a2.getX(), b2.getX())) + 2;
            double maxZ = Math.max(Math.max(a.getZ(), b.getZ()), Math.max(a2.getZ(), b2.getZ())) + 2;
            return new net.minecraft.world.phys.AABB(minX, Math.min(a.getY(), Math.min(b.getY(), Math.min(a2.getY(), b2.getY()))) - 1.5,
                    minZ, maxX, Math.max(a.getY(), Math.max(b.getY(), Math.max(a2.getY(), b2.getY()))) + CABLE_ANCHOR + 1.2, maxZ);
        }

        /** The deck-owner post (renders the whole mesh): the lexicographically smallest corner. */
        public BlockPos master() {
            BlockPos m = a;
            if (b.compareTo(m) < 0) m = b;
            if (a2.compareTo(m) < 0) m = a2;
            if (b2.compareTo(m) < 0) m = b2;
            return m;
        }

        public BlockPos[] corners() { return new BlockPos[] {a, b, a2, b2}; }
    }

    private BridgeDeckGeom() {
    }

    /**
     * Whole-block offset of the second post pair: perpendicular to the span and {@code width} blocks long.
     * Plain rounding of the ideal vector skews short diagonal spans by up to ~14 degrees (the deck becomes a
     * rhombus), so every candidate offset is scored and the squarest one wins.
     *
     * @return {dx, dz} of the offset from a to a2
     */
    /**
     * Offset from the first post to its partner across the deck: as close to `width` blocks as whole blocks allow,
     * and as square to the span as possible.
     *
     * <p>Rev 9.1: раньше побеждал чисто «самый ровный» сдвиг, и при ширине 1 и 2 выбирался один и тот же диагональный
     * (1,-1): настил выходил 1.4 блока — «бревно» вместо моста. Теперь скос и ошибка ширины сравниваются вместе, причём
     * недостаток ширины штрафуется квадратично, поэтому для широкого настила выбирается длинный перпендикуляр
     * (например (1,-2) при пролёте под 30°), а не короткая диагональ. Перебор ограничен ±(w+1) клеток — этого
     * достаточно, чтобы найти почти перпендикуляр при любом угле.
     */
    public static int[] perpOffset(double dx, double dz, int width) {
        double l = Math.hypot(dx, dz);
        if (l < 1.0) l = 1.0;
        double ux = dx / l, uz = dz / l;
        int w = Math.max(1, width);
        int px = 0, pz = 0;
        double best = Double.MAX_VALUE;
        for (int ax = -w - 1; ax <= w + 1; ++ax)
            for (int az = -w - 1; az <= w + 1; ++az) {
                if (ax == 0 && az == 0) continue;
                double alen = Math.hypot(ax, az);
                if (alen < Math.max(1.0, w * 0.8)) continue;              // заведомо уже, чем нужно
                double skew = Math.abs(ax * ux + az * uz) / alen;          // 0 = ровно поперёк пролёта
                double err = alen - w;
                double score = skew * 2.0 + err * err * 0.5 + Math.abs(err) * 0.05;
                if (score < best - 1e-9) { best = score; px = ax; pz = az; }
            }
        if (px == 0 && pz == 0) return new int[]{0, w};
        return new int[]{px, pz};
    }

    public static int styleFor(String biomePath) {
        if (biomePath == null) return BridgeStyle.PLAINS.id;
        return switch (biomePath) {
            case "violet_hills" -> BridgeStyle.GARDEN.id;
            case "sizo_grove" -> BridgeStyle.GROVE.id;
            case "waterfall_gardens" -> BridgeStyle.GARDEN.id;
            case "ashen_lands" -> BridgeStyle.BURNT.id;
            case "mist_keep" -> BridgeStyle.MIST.id;
            // vanilla Nether: charred planks and chains, the deck half burnt out (same as the Ashen Lands)
            case "nether_wastes", "soul_sand_valley", "crimson_forest", "warped_forest", "basalt_deltas" ->
                    BridgeStyle.BURNT.id;
            default -> BridgeStyle.PLAINS.id;
        };
    }
}
