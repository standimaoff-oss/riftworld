package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.ForgeEventFactory;
import ru.aiforge.riftworld.MajaConfig;
import ru.aiforge.riftworld.entity.ElGranMajaEntity;
import ru.aiforge.riftworld.registry.ElisarBlocks;

/**
 * Incremental island feeding, deliberately restricted to recently generated,
 * journalled wild islands. Old chunks, structures, block entities, five player
 * blocks, any nearby player or unloaded chunks all stop the operation. Forge's
 * mobGriefing and individual LivingDestroyBlockEvent are both respected.
 */
public final class MajaIslandDevourer {
    private static final int SCAN_BUDGET = 2048;
    private static final int BREAK_BUDGET = 96;
    private record Bite(long pos, Block expectedBlock) {}
    private Job job;
    private int cooldown;
    private String status = "idle";
    private int blocksEaten;
    public String status() { return status; }
    public int blocksEaten() { return blocksEaten; }

    private static final class Job {
        final CityPlan.Island island;
        final int minX, maxX, minZ, maxZ;
        final List<Bite> blocks = new ArrayList<>();
        final Set<Long> protectedCells = new HashSet<>();
        int x, z, y = Integer.MIN_VALUE, top, surfaceTop, consumeIndex;
        boolean scanComplete;
        Job(CityPlan.Island island) {
            this.island = island;
            int r = (int) Math.ceil(island.maxReach() + 2.0);
            minX = (int) Math.floor(island.cx) - r; maxX = (int) Math.floor(island.cx) + r;
            minZ = (int) Math.floor(island.cz) - r; maxZ = (int) Math.floor(island.cz) + r;
            x = minX; z = minZ;
        }
    }

    /** @return true while preparing or eating, when the titan should hold position and open its jaw. */
    public boolean tick(ServerLevel level, ElGranMajaEntity maja) {
        if (!MajaConfig.islandEatingEnabled()) { job = null; status = "disabled"; return false; }
        if (cooldown > 0) --cooldown;
        if (job == null) {
            if (cooldown > 0 || maja.tickCount % 80 != 0) return false;
            CityPlan.Island island = findCandidate(level, maja);
            if (island == null) return false;
            job = new Job(island);
            blocksEaten = 0;
            status = "scanning";
            MajaWorldData.get(level).protectPlacedNear(island.cx, island.cz, island.maxReach() + 8, job.protectedCells);
        }
        if (!safe(level, maja, job.island)) return cancel("unsafe");
        if (!job.scanComplete) {
            if (!scan(level, job)) return cancel("scan_blocked");
            if (!job.scanComplete) return true;
            if (job.blocks.isEmpty()) return cancel("empty");
            status = "eating";
        }
        MajaWorldData data = MajaWorldData.get(level);
        // Refresh protection if someone placed blocks remotely between scan batches.
        data.protectPlacedNear(job.island.cx, job.island.cz, job.island.maxReach() + 8, job.protectedCells);
        int done = 0;
        while (done++ < BREAK_BUDGET && job.consumeIndex < job.blocks.size()) {
            Bite bite = job.blocks.get(job.consumeIndex++);
            BlockPos p = BlockPos.of(bite.pos());
            if (job.protectedCells.contains(p.asLong()) || data.wasPlaced(p)) continue;
            BlockState state = level.getBlockState(p);
            if (state.isAir()) continue;
            if (state.hasBlockEntity() || !natural(state) || state.getBlock() != bite.expectedBlock()) return cancel("changed_or_denied");
            if (!state.canEntityDestroy(level, p, maja) || !ForgeEventFactory.onEntityDestroyBlock(maja, p, state)) return cancel("changed_or_denied");
            // A protection/event handler may itself replace a block or unload a chunk during the callback.
            if (level.getChunkSource().getChunkNow(p.getX() >> 4, p.getZ() >> 4) == null
                    || level.getBlockState(p).getBlock() != bite.expectedBlock() || data.wasPlaced(p)) return cancel("changed_or_denied");
            if (!level.destroyBlock(p, false, maja)) return cancel("changed_or_denied");
            blocksEaten++;
        }
        if (job.consumeIndex >= job.blocks.size()) return cancel("finished");
        return true;
    }

    private boolean cancel(String why) {
        status = why;
        job = null;
        cooldown = 300;
        return false;
    }

    private static CityPlan.Island findCandidate(ServerLevel level, ElGranMajaEntity maja) {
        double yaw = Math.toRadians(maja.getYRot());
        double mouthX = maja.getX() - Math.sin(yaw) * 156.0;
        double mouthZ = maja.getZ() + Math.cos(yaw) * 156.0;
        int cx = Math.floorDiv((int) Math.floor(mouthX), WildIslands.CELL);
        int cz = Math.floorDiv((int) Math.floor(mouthZ), WildIslands.CELL);
        CityPlan.Island best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int dx = -1; dx <= 1; ++dx) for (int dz = -1; dz <= 1; ++dz) {
            for (CityPlan.Island island : WildIslands.cell(cx + dx, cz + dz).islands) {
                if ((island.kind != 2 && island.kind != 3) || island.R > 36.0) continue;
                if (!DeepOcean.contains((int) Math.floor(island.cx), (int) Math.floor(island.cz))) continue;
                if (island.base < maja.getY() - 8 || island.base > maja.getY() + 75) continue;
                double d = Math.hypot(island.cx - mouthX, island.cz - mouthZ);
                if (d > island.maxReach() + 30 || d >= bestDistance) continue;
                if (safe(level, maja, island)) { best = island; bestDistance = d; }
            }
        }
        return best;
    }

    private static boolean safe(ServerLevel level, ElGranMajaEntity maja, CityPlan.Island island) {
        MajaWorldData data = MajaWorldData.get(level);
        double radius = island.maxReach() + 8;
        int minX = (int) Math.floor(island.cx - radius) >> 4, maxX = (int) Math.floor(island.cx + radius) >> 4;
        int minZ = (int) Math.floor(island.cz - radius) >> 4, maxZ = (int) Math.floor(island.cz + radius) >> 4;
        boolean trusted = true;
        for (int x = minX; x <= maxX && trusted; ++x) for (int z = minZ; z <= maxZ; ++z) {
            if (!data.isNewChunk(x, z) || level.getChunkSource().getChunkNow(x, z) == null) { trusted = false; break; }
        }
        boolean playerNearby = false;
        double playerRadius = radius + 64.0;
        for (ServerPlayer player : level.players()) {
            double dx = player.getX() - island.cx, dz = player.getZ() - island.cz;
            if (dx * dx + dz * dz <= playerRadius * playerRadius) { playerNearby = true; break; }
        }
        return MajaSafetyPolicy.mayDevour(trusted, data.placedNear(island.cx, island.cz, radius),
                playerNearby, ForgeEventFactory.getMobGriefingEvent(level, maja));
    }

    /** Scan a bounded slice each tick; abort before any destruction if an unfamiliar block appears. */
    private static boolean scan(ServerLevel level, Job j) {
        CityPlan plan = CityPlan.get();
        MajaWorldData data = MajaWorldData.get(level);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int budget = SCAN_BUDGET;
        while (budget-- > 0 && j.x <= j.maxX) {
            if (j.y == Integer.MIN_VALUE) {
                if (!j.island.contains(j.x + .5, j.z + .5)) { nextColumn(j); continue; }
                ElisarChunkGenerator.Col col = ElisarChunkGenerator.column(plan, j.x, j.z);
                // Never consume a road, plot, bridge or a different island.
                if (col.isl != j.island || col.plot || col.surface != ElisarChunkGenerator.S_GRASS
                        || col.deckY != Integer.MIN_VALUE || col.waterTop != Integer.MIN_VALUE) return false;
                j.y = Math.max(level.getMinBuildHeight(), col.bottom);
                j.surfaceTop = col.top;
                j.top = Math.min(level.getMaxBuildHeight() - 1, col.top + 80);
            }
            pos.set(j.x, j.y, j.z);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !(state.getBlock() instanceof LiquidBlock)) {
                if (state.hasBlockEntity()) return false;
                if (!data.wasPlaced(pos) && !natural(state)) return false;
                // Natural foliage is usually below top+24; do not delete higher, unrelated terrain.
                if (j.y <= j.surfaceTop + 24 && !data.wasPlaced(pos) && natural(state)) j.blocks.add(new Bite(pos.asLong(), state.getBlock()));
            }
            if (++j.y > j.top) nextColumn(j);
        }
        if (j.x > j.maxX) j.scanComplete = true;
        return true;
    }

    private static void nextColumn(Job j) {
        j.y = Integer.MIN_VALUE;
        if (++j.z > j.maxZ) { j.z = j.minZ; ++j.x; }
    }

    private static boolean natural(BlockState s) {
        // v0.46.0: рифт-кристалл генерируется гнёздами прямо в коренном камне диких
        // островов, поэтому пожиратель обязан считать его природной породой — иначе
        // сканирование прерывается на первом же гнезде и остров остаётся несъеденным.
        return s.is(ElisarBlocks.STONE.get()) || s.is(ElisarBlocks.STONE_MOSSY.get())
                || s.is(ElisarBlocks.GRASS.get()) || s.is(ElisarBlocks.SOIL.get())
                || s.is(ElisarBlocks.CRUST.get()) || s.is(ElisarBlocks.MOSS.get())
                || s.is(ElisarBlocks.DUCAT_ORE.get()) || s.is(ElisarBlocks.RIFTCRYSTAL_ORE.get())
                || s.is(ElisarBlocks.TALL_GRASS.get())
                || s.is(ElisarBlocks.FLOWERS.get()) || s.is(ElisarBlocks.AMBRA_LOG.get())
                || s.is(ElisarBlocks.AMBRA_LEAVES.get());
    }
}
