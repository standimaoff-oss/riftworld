package ru.aiforge.riftworld.bridge;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Shared math of the self-assembling rope bridge (Rev 4 item 13). A deck is defined by FOUR posts: two paired cable
 * lines (cables are tied lengthwise first - «в длину», then closed across - «в ширину»): endA/endB and endA2/endB2.
 * Diagonal builds are allowed: nothing here assumes a cardinal axis.
 */
public final class BridgeDeckGeom {
    public static final int MAX_LEN = 22;
    public static final int MAX_WID = 6;
    public static final double PLANK_STEP = 0.62;

    /** A built deck: the 4 corner posts. a -> b is the bridge DIRECTION; a -> a2 is the deck WIDTH. */
    public record Deck(BlockPos a, BlockPos b, BlockPos a2, BlockPos b2) {

        public Vec3 tipA() { return new Vec3(a.getX() + 0.5, a.getY() + 1.0, a.getZ() + 0.5); }
        public Vec3 tipB() { return new Vec3(b.getX() + 0.5, b.getY() + 1.0, b.getZ() + 0.5); }

        public double len() { return tipA().distanceTo(tipB()); }

        public double sag() { return Math.min(0.9, Math.max(0.28, len() * 0.05)); }

        /** Deck surface point at u (0..1) - sagging catwalk between the two cable lines. */
        public Vec3 pt(double u) {
            Vec3 p = tipA().lerp(tipB(), u);
            return new Vec3(p.x, p.y - sag() * 4 * u * (1 - u), p.z);
        }

        /** Unit-ish horizontal width direction (a -> a2 about its bridge partner line, flattened). */
        public Vec3 widthDir() {
            Vec3 w = tipB2().subtract(tipB());    // use the FAR line so the direction matches walking
            if (tipA2().distanceTo(tipA()) > tipB2().distanceTo(tipB())) w = tipA2().subtract(tipA());
            w = new Vec3(w.x, 0, w.z);
            return w.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : w.normalize();
        }

        public double width() { return tipA().distanceTo(tipA2()); }

        public Vec3 tipA2() { return new Vec3(a2.getX() + 0.5, a2.getY() + 1.0, a2.getZ() + 0.5); }
        public Vec3 tipB2() { return new Vec3(b2.getX() + 0.5, b2.getY() + 1.0, b2.getZ() + 0.5); }

        /** The across-deck curve of BOTH side lines at u, interpolated on the width (w in 0..1). */
        public Vec3 pt(double u, double w) {
            Vec3 p1 = tipA().lerp(tipB(), u);
            Vec3 p2 = tipA2().lerp(tipB2(), u);
            double s = sag() * 4 * u * (1 - u);
            return p1.lerp(p2, w).add(0, -s, 0);
        }

        /** Collision cells for the planks (deduped, exact - removal recomputes this same set). */
        public Set<BlockPos> plankCells() {
            Set<BlockPos> out = new LinkedHashSet<>();
            int n = Math.max(2, (int) Math.ceil(len() / 0.5));
            int nw = Math.max(0, (int) Math.round(width() - 1));
            for (int i = 0; i <= n; ++i) {
                double u = i / (double) n;
                for (int j = 0; j <= nw; ++j) {
                    Vec3 p = pt(u, nw == 0 ? 0.5 : j / (double) nw);
                    out.add(BlockPos.containing(p.x, p.y - 0.85, p.z));
                }
            }
            return out;
        }

        /** Axis bounds of the whole swinging volume (stir detection on the client). */
        public net.minecraft.world.phys.AABB stirBox() {
            Vec3 lo = new Vec3(Math.min(Math.min(a.getX(), b.getX()), Math.min(a2.getX(), b2.getX())),
                    Math.min(Math.min(a.getY(), b.getY()), Math.min(a2.getY(), b2.getY())) - 1.5,
                    Math.min(Math.min(a.getZ(), b.getZ()), Math.min(a2.getZ(), b2.getZ())));
            Vec3 hi = new Vec3(Math.max(Math.max(a.getX(), b.getX()), Math.max(a2.getX(), b2.getX())) + 1,
                    Math.max(Math.max(a.getY(), b.getY()), Math.max(a2.getY(), b2.getY())) + 2.6,
                    Math.max(Math.max(a.getZ(), b.getZ()), Math.max(a2.getZ(), b2.getZ())) + 1);
            return new net.minecraft.world.phys.AABB(lo.x, lo.y, lo.z, hi.x, hi.y, hi.z);
        }

        /** The deck-owner post (renders the mesh): the lexicographically smallest corner. */
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
}
