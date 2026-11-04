package ru.aiforge.riftworld.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Conservative, animation-aware collision envelopes for the full 322-block model.
 * Boxes are generated from every bone and cover the entire range of tail and jaw
 * animation, not just the small vanilla entity hitbox. Unloaded chunks, missing
 * bounds and the world border are treated as solid; this never force-loads land.
 */
public final class MajaCollision {
    private record Box(double x0, double y0, double z0, double x1, double y1, double z1) {}
    private static final List<Box> BOXES = readBounds();
    private static final double MOTION_MARGIN = 1.25;

    private MajaCollision() {}

    private static List<Box> readBounds() {
        List<Box> boxes = new ArrayList<>();
        try (var stream = MajaCollision.class.getResourceAsStream("/assets/riftworld/geo/el_gran_maja_bounds.json")) {
            if (stream == null) throw new IllegalStateException("missing collision bounds");
            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : json.getAsJsonArray("boxes")) {
                JsonObject o = entry.getAsJsonObject();
                var lo = o.getAsJsonArray("min");
                var hi = o.getAsJsonArray("max");
                Box box = new Box(lo.get(0).getAsDouble(), lo.get(1).getAsDouble(), lo.get(2).getAsDouble(),
                        hi.get(0).getAsDouble(), hi.get(1).getAsDouble(), hi.get(2).getAsDouble());
                if (box.x0() >= box.x1() || box.y0() >= box.y1() || box.z0() >= box.z1())
                    throw new IllegalStateException("invalid Maja collision box");
                boxes.add(box);
            }
            if (boxes.size() != 13) throw new IllegalStateException("wrong Maja bone count");
            return List.copyOf(boxes);
        } catch (Exception ex) {
            System.err.println("[riftworld] Maja collision data unavailable; creature disabled: " + ex);
            return List.of(); // fail closed
        }
    }

    /** Server-side, conservative test; the margin covers movement and yaw interpolation between ticks. */
    public static boolean clear(ServerLevel level, Vec3 center, float yaw) {
        if (BOXES.isEmpty()) return false;
        double angle = Math.toRadians(180.0 - yaw);
        double cos = Math.cos(angle), sin = Math.sin(angle);
        for (Box b : BOXES) {
            double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
            for (double x : new double[]{b.x0(), b.x1()}) for (double z : new double[]{b.z0(), b.z1()}) {
                double wx = center.x + cos * x + sin * z;
                double wz = center.z - sin * x + cos * z;
                minX = Math.min(minX, wx); maxX = Math.max(maxX, wx);
                minZ = Math.min(minZ, wz); maxZ = Math.max(maxZ, wz);
            }
            AABB volume = new AABB(minX - MOTION_MARGIN, center.y + b.y0() - MOTION_MARGIN, minZ - MOTION_MARGIN,
                    maxX + MOTION_MARGIN, center.y + b.y1() + MOTION_MARGIN, maxZ + MOTION_MARGIN);
            var border = level.getWorldBorder();
            if (volume.minX < border.getMinX() || volume.maxX > border.getMaxX()
                    || volume.minZ < border.getMinZ() || volume.maxZ > border.getMaxZ()
                    || !clearBox(level, volume)) return false;
        }
        return true;
    }

    private static boolean mayObstruct(BlockState state) {
        // Do not treat a waterlogged solid as water: only actual liquid blocks can be skipped.
        return !state.isAir() && !(state.getBlock() instanceof LiquidBlock);
    }

    private static boolean clearBox(ServerLevel level, AABB box) {
        int minX = (int) Math.floor(box.minX), maxX = (int) Math.ceil(box.maxX) - 1;
        int minY = (int) Math.floor(box.minY), maxY = (int) Math.ceil(box.maxY) - 1;
        int minZ = (int) Math.floor(box.minZ), maxZ = (int) Math.ceil(box.maxZ) - 1;
        if (minY < level.getMinBuildHeight() || maxY >= level.getMaxBuildHeight()) return false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int cx = minX >> 4; cx <= (maxX >> 4); ++cx) {
            for (int cz = minZ >> 4; cz <= (maxZ >> 4); ++cz) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) return false;
                int x0 = Math.max(minX, cx << 4), x1 = Math.min(maxX, (cx << 4) + 15);
                int z0 = Math.max(minZ, cz << 4), z1 = Math.min(maxZ, (cz << 4) + 15);
                for (int sy = minY >> 4; sy <= (maxY >> 4); ++sy) {
                    int index = level.getSectionIndexFromSectionY(sy);
                    LevelChunkSection section = chunk.getSection(index);
                    if (section.hasOnlyAir() || !section.maybeHas(MajaCollision::mayObstruct)) continue;
                    int y0 = Math.max(minY, sy << 4), y1 = Math.min(maxY, (sy << 4) + 15);
                    for (int x = x0; x <= x1; ++x) for (int z = z0; z <= z1; ++z) {
                        for (int y = y0; y <= y1; ++y) {
                            BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                            if (mayObstruct(state) && !state.getCollisionShape(level, p.set(x, y, z)).isEmpty()) return false;
                        }
                    }
                }
            }
        }
        return true;
    }
}
