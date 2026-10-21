package ru.aiforge.riftworld.bridge;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import ru.aiforge.riftworld.block.BridgePostBE;
import ru.aiforge.riftworld.registry.BridgeBlocks;
import ru.aiforge.riftworld.world.AshenLands;
import ru.aiforge.riftworld.world.CityPlan;
import ru.aiforge.riftworld.world.VioletHills;
import ru.aiforge.riftworld.world.WaterfallGardens;

/**
 * Rev 7: the world-generated suspension bridges. Deterministic - no saved data: every chunk asks for the bridges whose
 * volume touches it and places only ITS OWN posts and collision cells, so a span may cross any number of chunks without
 * deferred writes.
 *
 * <ul>
 *   <li><b>04 Фиолетовые холмы</b> - the footpath between the two churches crosses the river: a four-post footbridge
 *       (garden style) replaces the old plank ford.</li>
 *   <li><b>07 Висячие сады</b> - the rare bridges over the terrace gullies, now four-post with netting.</li>
 *   <li><b>Адские угодья / Тлеющие руины</b> - burnt bridges: charred posts, chains instead of cables, a collapsed
 *       span in the middle and dangling ends.</li>
 * </ul>
 */
public final class WorldBridges {
    private WorldBridges() {
    }

    /** 4-post deck between two opposite corners: width is given in blocks (2 = narrow footbridge). */
    public static BridgeDeckGeom.Deck deckOf(BlockPos a, BlockPos b, int halfWidth, int style, float breakFrom, float breakTo) {
        int[] off = BridgeDeckGeom.perpOffset(b.getX() - a.getX(), b.getZ() - a.getZ(), halfWidth);
        int px = off[0], pz = off[1];
        if (px == 0 && pz == 0) px = halfWidth;
        BlockPos a2 = a.offset(px, 0, pz);
        BlockPos b2 = b.offset(px, 0, pz);
        return new BridgeDeckGeom.Deck(a, b, a2, b2, style, breakFrom, breakTo);
    }

    /** All decks whose volume can touch the chunk [x0..x0+15] x [z0..z0+15]. */
    public static List<BridgeDeckGeom.Deck> decksNear(int x0, int z0) {
        List<BridgeDeckGeom.Deck> out = new ArrayList<>(3);
        CityPlan plan = CityPlan.get();
        int ox = x0 + 8, oz = z0 + 8;

        //----- biome 04: footpath x river crossing
        for (int sx = -1; sx <= 1; ++sx)
            for (int sz = -1; sz <= 1; ++sz) {
                VioletHills.Region vr = VioletHills.region(
                        Math.floorDiv(ox, VioletHills.SUPER) + sx, Math.floorDiv(oz, VioletHills.SUPER) + sz);
                if (vr == null) continue;
                double[] cross = crossing(vr);
                if (cross == null) continue;
                if (Math.abs(cross[0] - ox) > 26 || Math.abs(cross[1] - oz) > 26) continue;
                double[] dir = pathDir(vr, cross[0], cross[1]);
                double half = VioletHills.Region.RIVER_HW + 3.4;
                double ax = cross[0] - dir[0] * half, az = cross[1] - dir[1] * half;
                double bx = cross[0] + dir[0] * half, bz = cross[1] + dir[1] * half;
                BlockPos pa = bank(vr.meadow, (int) Math.floor(ax), (int) Math.floor(az));
                BlockPos pb = bank(vr.meadow, (int) Math.floor(bx), (int) Math.floor(bz));
                if (pa == null || pb == null) continue;
                if (Math.abs(pa.getY() - pb.getY()) > 5) continue;
                out.add(deckOf(pa, pb, 1, BridgeStyle.GARDEN.id, 0F, 0F));
            }

        //----- biome 07: the rare gullies bridged on the terraces
        for (int sx = -1; sx <= 1; ++sx)
            for (int sz = -1; sz <= 1; ++sz) {
                WaterfallGardens.Region gr = WaterfallGardens.region(
                        Math.floorDiv(ox, WaterfallGardens.SUPER) + sx, Math.floorDiv(oz, WaterfallGardens.SUPER) + sz);
                if (gr == null) continue;
                for (CityPlan.Bridge b : gr.bridges) {
                    double mx = (b.ax() + b.bx()) / 2, mz = (b.az() + b.bz()) / 2;
                    if (Math.abs(mx - ox) > 30 || Math.abs(mz - oz) > 30) continue;
                    double dx = b.bx() - b.ax(), dz = b.bz() - b.az();
                    double l = Math.hypot(dx, dz);
                    if (l < 3) continue;
                    double ux = dx / l, uz = dz / l;
                    int half = (int) Math.ceil(l / 2) + 1;
                    BlockPos pa = new BlockPos((int) Math.round(mx - ux * half), b.ya(), (int) Math.round(mz - uz * half));
                    BlockPos pb = new BlockPos((int) Math.round(mx + ux * half), b.yb(), (int) Math.round(mz + uz * half));
                    out.add(deckOf(pa, pb, 1, BridgeStyle.GARDEN.id, 0F, 0F));
                }
            }

        //----- Ashen Lands: burnt, collapsed bridges on two charred stone piers over the ash plain
        int ocean = plan.ocean;
        for (AshenLands.Mound m : AshenLands.MOUNDS) {
            if (m.seed() != 2 && m.seed() != 3) continue;                 // one ruined span per big fallen island
            int cx = (int) m.x() + 44, cz = (int) m.z() + 6;
            if (Math.abs(cx - ox) > 30 || Math.abs(cz - oz) > 30) continue;
            int half = 7;
            if (AshenLands.moundTop(cx - half, cz) != Integer.MIN_VALUE
                    || AshenLands.moundTop(cx + half, cz) != Integer.MIN_VALUE) continue;      // not over the mound itself
            int y = ocean + 6;
            out.add(deckOf(new BlockPos(cx - half, y, cz), new BlockPos(cx + half, y, cz),
                    2, BridgeStyle.BURNT.id, 0.44F, 0.63F));
        }
        // trim to those that really can reach this chunk
        out.removeIf(d -> !touches(d, x0, z0));
        return out;
    }

    private static boolean touches(BridgeDeckGeom.Deck d, int x0, int z0) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : d.corners()) {
            minX = Math.min(minX, p.getX());
            maxX = Math.max(maxX, p.getX());
            minZ = Math.min(minZ, p.getZ());
            maxZ = Math.max(maxZ, p.getZ());
        }
        return maxX + 2 >= x0 && minX - 2 <= x0 + 15 && maxZ + 2 >= z0 && minZ - 2 <= z0 + 15;
    }

    /**
     * Places this chunk's share of the world bridges: the corner posts that stand inside it (with the deck bound to
     * their block entities) and the invisible collision plates of the deck (with their 1/16 top heights).
     */
    public static void place(LevelAccessor level, int x0, int z0) {
        List<BridgeDeckGeom.Deck> decks = decksNear(x0, z0);
        if (decks.isEmpty()) return;
        BlockState post = BridgeBlocks.POST.get().defaultBlockState();
        int ocean = CityPlan.get().ocean;
        for (BridgeDeckGeom.Deck d : decks) {
            if (d.style() == BridgeStyle.BURNT) piers(level, d, ocean, x0, z0);
            for (BlockPos p : d.corners()) {
                if (p.getX() < x0 || p.getX() > x0 + 15 || p.getZ() < z0 || p.getZ() > z0 + 15) continue;
                BlockPos ground = p.below();
                boolean supported = !level.getBlockState(ground).isAir();
                BlockPos at = supported ? p : p.below(2);
                level.setBlock(at, post.setValue(ru.aiforge.riftworld.block.BridgePostBlock.STYLE, d.styleId()), 2);
                if (level.getBlockEntity(at) instanceof BridgePostBE be) {
                    be.stand(shift(d, at.getY() - p.getY()));
                    be.setChanged();
                }
            }
            // collision plates: only the cells that belong to this chunk
            for (var e : d.collisionCells().entrySet()) {
                BlockPos c = e.getKey();
                if (c.getX() < x0 || c.getX() > x0 + 15 || c.getZ() < z0 || c.getZ() > z0 + 15) continue;
                BlockState cur = level.getBlockState(c);
                if (!cur.isAir() && !cur.canBeReplaced()) continue;
                level.setBlock(c, BridgeBlocks.WALK.get().defaultBlockState()
                        .setValue(ru.aiforge.riftworld.block.BridgeWalkBlock.TOP, e.getValue()), 2);
            }
        }
    }

    /** Charred stone piers under a burnt span (two pillar groups = the four posts stand on masonry, as on a ruin). */
    private static void piers(LevelAccessor level, BridgeDeckGeom.Deck d, int ocean, int x0, int z0) {
        BlockState stone = ru.aiforge.riftworld.registry.CastleBlocks.SCORCHED_STONE.get().defaultBlockState();
        BlockState cracked = ru.aiforge.riftworld.registry.CastleBlocks.BRICKS_CRACKED.get().defaultBlockState();
        BlockPos[][] pairs = {{d.a(), d.a2()}, {d.b(), d.b2()}};
        for (BlockPos[] pair : pairs) {
            BlockPos p0 = pair[0], p1 = pair[1];
            int steps = Math.max(Math.abs(p1.getX() - p0.getX()), Math.abs(p1.getZ() - p0.getZ()));
            for (int i = 0; i <= steps; ++i) {
                int cx = (int) Math.round(p0.getX() + (p1.getX() - p0.getX()) * (steps == 0 ? 0 : i / (double) steps));
                int cz = (int) Math.round(p0.getZ() + (p1.getZ() - p0.getZ()) * (steps == 0 ? 0 : i / (double) steps));
                if (cx < x0 || cx > x0 + 15 || cz < z0 || cz > z0 + 15) continue;
                for (int y = ocean + 1; y < p0.getY() - 1; ++y) {
                    BlockPos at = new BlockPos(cx, y, cz);
                    if (!level.getBlockState(at).isAir()) continue;
                    boolean crumbly = Math.floorMod(cx * 31 + cz * 17 + y * 7, 11) < 3;
                    level.setBlock(at, crumbly ? cracked : stone, 2);
                }
                // one burnt bracket stone just under the cap so the post does not float
                BlockPos cap = new BlockPos(cx, p0.getY() - 1, cz);
                if (level.getBlockState(cap).isAir()) level.setBlock(cap, stone, 2);
            }
        }
    }

    /** A deck copy whose posts all moved down by dy (so the deck really starts at the placed post). */
    private static BridgeDeckGeom.Deck shift(BridgeDeckGeom.Deck d, int dy) {
        if (dy == 0) return d;
        return new BridgeDeckGeom.Deck(d.a().offset(0, dy, 0), d.b().offset(0, dy, 0), d.a2().offset(0, dy, 0),
                d.b2().offset(0, dy, 0), d.styleId(), d.breakFrom(), d.breakTo());
    }

    // ------------------------------------------------------------------ helpers

    /** Bank post position for the violet meadow: one block sunk into the ground, never under the water line. */
    private static BlockPos bank(CityPlan.Island meadow, int x, int z) {
        double raw = meadow.raw(x + 0.5, z + 0.5);
        if (raw < 1) return null;
        return new BlockPos(x, (int) Math.floor(raw), z);
    }

    /** Where the church footpath crosses the river (x, z), or null. */
    private static double[] crossing(VioletHills.Region vr) {
        double[] c1 = segX(vr.pax, vr.paz, vr.pmx, vr.pmz, vr.rax, vr.raz, vr.rbx, vr.rbz);
        if (c1 != null) return c1;
        return segX(vr.pmx, vr.pmz, vr.pbx, vr.pbz, vr.rax, vr.raz, vr.rbx, vr.rbz);
    }

    /** Unit direction of the footpath at (x, z). */
    private static double[] pathDir(VioletHills.Region vr, double x, double z) {
        double d1 = Math.hypot(x - vr.pax, z - vr.paz) + Math.hypot(x - vr.pmx, z - vr.pmz);
        double d2 = Math.hypot(x - vr.pmx, z - vr.pmz) + Math.hypot(x - vr.pbx, z - vr.pbz);
        double ax, az, bx, bz;
        if (Math.abs(d1 - Math.hypot(vr.pax - vr.pmx, vr.paz - vr.pmz)) < Math.abs(d2 - Math.hypot(vr.pmx - vr.pbx, vr.pmz - vr.pbz))) {
            ax = vr.pax; az = vr.paz; bx = vr.pmx; bz = vr.pmz;
        } else {
            ax = vr.pmx; az = vr.pmz; bx = vr.pbx; bz = vr.pbz;
        }
        double l = Math.hypot(bx - ax, bz - az);
        if (l < 1e-6) return new double[] {1, 0};
        return new double[] {(bx - ax) / l, (bz - az) / l};
    }

    /** Segment intersection point (x, z) or null. */
    private static double[] segX(double ax, double az, double bx, double bz, double cx, double cz, double dx, double dz) {
        double r1 = bx - ax, r2 = bz - az, s1 = dx - cx, s2 = dz - cz;
        double den = r1 * s2 - r2 * s1;
        if (Math.abs(den) < 1e-9) return null;
        double t = ((cx - ax) * s2 - (cz - az) * s1) / den;
        double u = ((cx - ax) * r2 - (cz - az) * r1) / den;
        if (t < 0 || t > 1 || u < 0 || u > 1) return null;
        return new double[] {ax + r1 * t, az + r2 * t};
    }
}
