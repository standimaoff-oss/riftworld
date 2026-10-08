package ru.aiforge.riftworld.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import ru.aiforge.riftworld.RiftWorld;

/**
 * Geometry of the ritual hall of the Great Temple, measured from the altar (template cell 31, G, 34).
 * <p>
 * The hall is the volume where the world darkens, where everything except the player's walk slows down and where the
 * rift hangs. It is the same building in the overworld and in Elisar (the twin temple of the city «Странники»), so the
 * altar position is all we need on both sides.
 */
public final class RiftZone {
    /** Radius of the candle ring: every rift candle inside it must burn. */
    public static final int CANDLE_R = 10;
    /** Half sizes of the hall (the nave of the temple) around the altar. */
    public static final int HALF_X = 17, HALF_Z = 24, DOWN = 4, UP = 26;
    /** The rift floats above the altar. */
    public static final int RIFT_UP = 3;
    /** Cells of the temple template between its origin and the altar. */
    public static final int TEMPLATE_ALTAR_X = 31, TEMPLATE_ALTAR_Z = 34, TEMPLATE_GROUND = 30;

    public static final ResourceKey<Level> ELISAR = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(RiftWorld.MODID, "elisar"));

    private RiftZone() {
    }

    /** The hall itself: darkness and slow motion apply here and not outside the building. */
    public static AABB box(BlockPos altar) {
        return new AABB(altar.getX() - HALF_X, altar.getY() - DOWN, altar.getZ() - HALF_Z,
                altar.getX() + HALF_X + 1, altar.getY() + UP, altar.getZ() + HALF_Z + 1);
    }

    /** Where the player has to stand: on the hexagram, close to the altar. */
    public static AABB signBox(BlockPos altar) {
        return new AABB(altar.getX() - 6, altar.getY() + 0.2, altar.getZ() - 6, altar.getX() + 7, altar.getY() + 3.0, altar.getZ() + 7);
    }

    /** The rift itself: walk into this and you cross over. */
    public static AABB riftBox(BlockPos altar) {
        return new AABB(altar.getX() - 1.4, altar.getY() + RIFT_UP - 2.2, altar.getZ() - 1.4,
                altar.getX() + 2.4, altar.getY() + RIFT_UP + 2.2, altar.getZ() + 2.4);
    }

    public static double centerX(BlockPos altar) {
        return altar.getX() + 0.5;
    }

    public static double centerY(BlockPos altar) {
        return altar.getY() + RIFT_UP;
    }

    public static double centerZ(BlockPos altar) {
        return altar.getZ() + 0.5;
    }

    public static boolean inside(BlockPos altar, double x, double y, double z) {
        return box(altar).contains(x, y, z);
    }

    /** 0 outside the hall, 1 deep inside - used by the darkness and the slow motion. */
    public static float weight(BlockPos altar, double x, double y, double z) {
        AABB b = box(altar);
        if (!b.contains(x, y, z)) return 0.0F;
        double dx = Math.min(x - b.minX, b.maxX - x), dz = Math.min(z - b.minZ, b.maxZ - z);
        double t = Math.min(1.0, Math.min(dx, dz) / 6.0);
        return (float) (t * t * (3 - 2 * t));
    }

    /**
     * Where the twin altar stands in Elisar: the temple is a fixed plot of the deterministic city plan, so the position
     * is computed from data/riftworld/city/plan.json without any saved state.
     */
    public static BlockPos elisarAltar() {
        for (CityPlan.Plot p : CityPlan.get().plots) {
            if (!"temple_great".equals(p.template())) continue;
            int[] off = rotateToWorld(p.facing(), TEMPLATE_ALTAR_X, TEMPLATE_ALTAR_Z, p.w(), p.d());
            return new BlockPos(p.x0() + off[0], p.h() + p.yOff() + TEMPLATE_GROUND, p.z0() + off[1]);
        }
        return new BlockPos(0, 104, 0);
    }

    /** Template cells are placed with the city rotation; the altar offset follows it. */
    private static int[] rotateToWorld(String facing, int x, int z, int w, int d) {
        return switch (facing) {
            case "west" -> new int[]{d - 1 - z, x};
            case "east" -> new int[]{z, w - 1 - x};
            case "north" -> new int[]{w - 1 - x, d - 1 - z};
            default -> new int[]{x, z};
        };
    }

}
