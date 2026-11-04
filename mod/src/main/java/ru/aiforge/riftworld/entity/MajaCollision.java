package ru.aiforge.riftworld.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Blocks;
import ru.aiforge.riftworld.registry.ElisarBlocks;
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
    // Entity yaw is sent as a byte (1.40625° steps): at 166 blocks even a
    // one-step discrepancy moves a tail tip over four blocks. Include that,
    // per-tick turning and movement, without unnecessarily shrinking water depth.
    private static final double HORIZONTAL_MARGIN = 6.0;
    private static final double VERTICAL_MARGIN = 1.25;

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
        AABB[] volumes = new AABB[BOXES.size()];
        double allMinX = Double.POSITIVE_INFINITY, allMaxX = Double.NEGATIVE_INFINITY;
        double allMinZ = Double.POSITIVE_INFINITY, allMaxZ = Double.NEGATIVE_INFINITY;
        int minSectionY = Integer.MAX_VALUE, maxSectionY = Integer.MIN_VALUE;
        var border = level.getWorldBorder();
        for (int i = 0; i < volumes.length; ++i) {
            Box b = BOXES.get(i);
            // Linear extrema give the same four-corner bounds without creating
            // two temporary double arrays for each of the thirteen bones.
            double ax = cos * b.x0(), bx = cos * b.x1();
            double az = sin * b.z0(), bz = sin * b.z1();
            double cx = -sin * b.x0(), dx = -sin * b.x1();
            double cz = cos * b.z0(), dz = cos * b.z1();
            AABB v = new AABB(center.x + Math.min(ax, bx) + Math.min(az, bz) - HORIZONTAL_MARGIN,
                    center.y + b.y0() - VERTICAL_MARGIN,
                    center.z + Math.min(cx, dx) + Math.min(cz, dz) - HORIZONTAL_MARGIN,
                    center.x + Math.max(ax, bx) + Math.max(az, bz) + HORIZONTAL_MARGIN,
                    center.y + b.y1() + VERTICAL_MARGIN,
                    center.z + Math.max(cx, dx) + Math.max(cz, dz) + HORIZONTAL_MARGIN);
            if (v.minX < border.getMinX() || v.maxX > border.getMaxX()
                    || v.minZ < border.getMinZ() || v.maxZ > border.getMaxZ()
                    || Math.floor(v.minY) < level.getMinBuildHeight()
                    || Math.ceil(v.maxY) > level.getMaxBuildHeight()) return false;
            volumes[i] = v;
            allMinX = Math.min(allMinX, v.minX); allMaxX = Math.max(allMaxX, v.maxX);
            allMinZ = Math.min(allMinZ, v.minZ); allMaxZ = Math.max(allMaxZ, v.maxZ);
            minSectionY = Math.min(minSectionY, (int) Math.floor(v.minY) >> 4);
            maxSectionY = Math.max(maxSectionY, ((int) Math.ceil(v.maxY) - 1) >> 4);
        }
        int minChunkX = (int) Math.floor(allMinX) >> 4, minChunkZ = (int) Math.floor(allMinZ) >> 4;
        int maxChunkX = ((int) Math.ceil(allMaxX) - 1) >> 4, maxChunkZ = ((int) Math.ceil(allMaxZ) - 1) >> 4;
        int width = maxChunkX - minChunkX + 1, depth = maxChunkZ - minChunkZ + 1;
        int sectionCount = maxSectionY - minSectionY + 1;
        LevelChunk[] chunks = new LevelChunk[width * depth];
        byte[] palettes = new byte[width * depth * sectionCount];

        // First verify that every touched chunk is already FULL. Never load a
        // chunk here, and avoid scanning many blocks only to discover an
        // unloaded chunk near the far end of the animal.
        for (AABB v : volumes) {
            int minX = (int) Math.floor(v.minX) >> 4, maxX = ((int) Math.ceil(v.maxX) - 1) >> 4;
            int minZ = (int) Math.floor(v.minZ) >> 4, maxZ = ((int) Math.ceil(v.maxZ) - 1) >> 4;
            for (int x = minX; x <= maxX; ++x) for (int z = minZ; z <= maxZ; ++z) {
                int ci = (x - minChunkX) * depth + z - minChunkZ;
                if (chunks[ci] == null) {
                    chunks[ci] = level.getChunkSource().getChunkNow(x, z);
                    if (chunks[ci] == null) return false;
                }
            }
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (AABB v : volumes) {
            int minX = (int) Math.floor(v.minX), maxX = (int) Math.ceil(v.maxX) - 1;
            int minY = (int) Math.floor(v.minY), maxY = (int) Math.ceil(v.maxY) - 1;
            int minZ = (int) Math.floor(v.minZ), maxZ = (int) Math.ceil(v.maxZ) - 1;
            for (int cx = minX >> 4; cx <= (maxX >> 4); ++cx) for (int cz = minZ >> 4; cz <= (maxZ >> 4); ++cz) {
                int ci = (cx - minChunkX) * depth + cz - minChunkZ;
                LevelChunk chunk = chunks[ci];
                int x0 = Math.max(minX, cx << 4), x1 = Math.min(maxX, (cx << 4) + 15);
                int z0 = Math.max(minZ, cz << 4), z1 = Math.min(maxZ, (cz << 4) + 15);
                for (int sy = minY >> 4; sy <= (maxY >> 4); ++sy) {
                    LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(sy));
                    int pi = ci * sectionCount + sy - minSectionY;
                    if (palettes[pi] == 0) {
                        palettes[pi] = (byte) (section.hasOnlyAir() || !section.maybeHas(MajaCollision::mayObstruct) ? 1 : 2);
                    }
                    if (palettes[pi] == 1) continue;
                    int y0 = Math.max(minY, sy << 4), y1 = Math.min(maxY, (sy << 4) + 15);
                    for (int x = x0; x <= x1; ++x) for (int z = z0; z <= z1; ++z) for (int y = y0; y <= y1; ++y) {
                        BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                        if (mayObstruct(state) && !state.getCollisionShape(level, p.set(x, y, z)).isEmpty()) return false;
                    }
                }
            }
        }
        return true;
    }

    /** Only blocks whose no-collision behavior is guaranteed by our own registrations. */
    private static final class PassableSeaBlocks {
        private static final java.util.Set<Block> BLOCKS = java.util.Set.of(
                ElisarBlocks.CORAL_BRANCH.get(), ElisarBlocks.CORAL_FAN.get(), ElisarBlocks.CORAL_TUBE.get(),
                ElisarBlocks.CORAL_GLOW.get(), ElisarBlocks.SEA_GRASS.get(), ElisarBlocks.SEA_KELP.get(),
                ElisarBlocks.SEA_KELP_TOP.get(), ElisarBlocks.CAVE_GLOW.get(), ElisarBlocks.SEA_SHELLS.get(),
                ElisarBlocks.ANEMONE.get(), ElisarBlocks.CORAL_BUSH.get(), ElisarBlocks.TUBE_WORMS.get(),
                ElisarBlocks.SEA_STAR.get(), ElisarBlocks.SEA_URCHIN.get(), ElisarBlocks.SHELL_BED.get(),
                Blocks.KELP, Blocks.KELP_PLANT, Blocks.SEAGRASS, Blocks.TALL_SEAGRASS);
    }

    private static boolean mayObstruct(BlockState state) {
        // Waterlogged *solid* blocks still obstruct; never skip them merely
        // because their fluid state contains water. Other mods remain checked.
        return !state.isAir() && !(state.getBlock() instanceof LiquidBlock)
                && !PassableSeaBlocks.BLOCKS.contains(state.getBlock());
    }

}
