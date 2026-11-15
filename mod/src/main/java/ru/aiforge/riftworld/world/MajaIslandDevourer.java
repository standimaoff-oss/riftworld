package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.registries.ForgeRegistries;
import ru.aiforge.riftworld.MajaConfig;
import ru.aiforge.riftworld.entity.ElGranMajaEntity;
import ru.aiforge.riftworld.entity.MajaCollision;
import ru.aiforge.riftworld.flight.Flight;
import ru.aiforge.riftworld.registry.ElisarBlocks;

/**
 * Incremental island feeding for El Gran Maja, deliberately restricted to recently
 * generated, journalled wild islands of the Elisar dimension.
 *
 * <p>v0.49.0 — the titan became a rare but ALWAYS-ENABLED event:
 * <ul>
 *   <li>whole small islands (kind 2/3/4 with {@code R <= 28}) are devoured completely,
 *       including their bare stone rims ({@code S_BARE}) and the natural trees that
 *       the Dynamic Trees addon grows on them;</li>
 *   <li>larger organic islands (kind 1/2 with {@code R > 28}) are bitten instead: the
 *       titan chews an organic jagged chunk out of the rim that borders the deep ocean;</li>
 *   <li>after every meal the titan waits for a long, rare cooldown before hunting again;</li>
 *   <li>while feeding it breaches upward out of the water with a wide jaw chomp and a
 *       splash / cloud / block particle burst, but only through poses that the
 *       conservative collision envelope has validated.</li>
 * </ul>
 *
 * <p>Old chunks, structures, block entities, ANY player-placed block, any nearby
 * player or unloaded chunks all stop the operation. Forge's mobGriefing and the
 * individual LivingDestroyBlockEvent are both respected.
 */
public final class MajaIslandDevourer {
    private static final int SCAN_BUDGET = 2048;
    private static final int BREAK_BUDGET = 96;
    /** Maximum radius of a whole island that may be swallowed in one meal. */
    public static final double MAX_WHOLE_ISLAND_RADIUS = 28.0;
    /** Radius of the organic chunk bitten out of a large island rim. */
    private static final double BITE_RADIUS = 18.0;
    /** Rare: how many feeding attempts may be skipped before the next hunt. */
    private static final int MEAL_COOLDOWN = 2400;
    /** Minimum number of quiet ticks between two candidate searches. */
    private static final int SEARCH_INTERVAL = 80;

    private record Bite(long pos, Block expectedBlock) {}

    private Job job;
    private int cooldown;
    private String status = "idle";
    private int blocksEaten;
    /** Ticks spent actively rising out of the water during the current meal. */
    private int breachTicks;
    private boolean breached;

    public String status() { return status; }
    public int blocksEaten() { return blocksEaten; }
    /** True while the titan performs its dramatic upward water-breach surge. */
    public boolean isBreaching() { return job != null && breachTicks > 0; }

    private static final class Job {
        final CityPlan.Island island;
        final boolean wholeIsland;
        final int minX, maxX, minZ, maxZ;
        final List<Bite> blocks = new ArrayList<>();
        final Set<Long> protectedCells = new HashSet<>();
        int x, z, y = Integer.MIN_VALUE, top, surfaceTop, consumeIndex;
        boolean scanComplete;
        Job(CityPlan.Island island, boolean wholeIsland) {
            this.island = island;
            this.wholeIsland = wholeIsland;
            int r = (int) Math.ceil((wholeIsland ? island.maxReach() : BITE_RADIUS) + 2.0);
            minX = (int) Math.floor(island.cx) - r; maxX = (int) Math.floor(island.cx) + r;
            minZ = (int) Math.floor(island.cz) - r; maxZ = (int) Math.floor(island.cz) + r;
            x = minX; z = minZ;
        }
    }

    /**
     * @return true while preparing or eating, when the titan should hold position and open its jaw.
     */
    public boolean tick(ServerLevel level, ElGranMajaEntity maja) {
        if (!MajaConfig.islandEatingEnabled()) { job = null; status = "disabled"; return false; }
        // The titan only ever hunts inside the Elisar ocean dimension.
        if (!level.dimension().equals(Flight.ELISAR_DIM)) {
            job = null; status = "idle"; return false;
        }
        if (cooldown > 0) --cooldown;
        if (job == null) {
            if (cooldown > 0 || maja.tickCount % SEARCH_INTERVAL != 0) return false;
            CityPlan.Island island = findCandidate(level, maja);
            if (island == null) return false;
            boolean whole = island.R <= MAX_WHOLE_ISLAND_RADIUS && island.kind >= 2 && island.kind <= 4;
            job = new Job(island, whole);
            blocksEaten = 0;
            breachTicks = 0;
            breached = false;
            status = "scanning";
            MajaWorldData.get(level).protectPlacedNear(island.cx, island.cz, island.maxReach() + 8, job.protectedCells);
        }
        if (!safe(level, maja, job.island, job.wholeIsland)) return cancel("unsafe");
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
        if (job.consumeIndex >= job.blocks.size()) {
            if (!breached) {
                // The meal is done — the titan completes its dramatic breach before it lets go.
                status = "breaching";
                return true;
            }
            return cancel("finished");
        }
        return true;
    }

    /**
     * Raises the titan out of the water while it chews. The horizontal position and yaw
     * are held; only the vertical pose changes, and every candidate pose is validated
     * with the conservative collision envelope before it is accepted.
     */
    public void breach(ServerLevel level, ElGranMajaEntity maja) {
        if (job == null) return;
        if (breachTicks == 0) {
            // Lift the titan to a clear pose first; if that is impossible, stay hidden.
            if (!riseSafely(level, maja, 0.35)) return;
            breachTicks = 1;
        }
        breachTicks++;
        double sea = CityPlan.get().ocean;
        // The head of the 322-block model sits roughly 36..24 blocks BELOW the entity
        // anchor, so the anchor must climb to sea+42 for the jaws to break the surface.
        double target = sea + 42.0;
        double step = Math.max(-0.85, Math.min(0.85, (target - maja.getY()) * 0.10));
        if (Math.abs(step) < 0.05) step = 0.05;
        if (!riseSafely(level, maja, step)) {
            // Collision blocked the surge: fall back to the previous validated pose.
            riseSafely(level, maja, -step);
            breachTicks = 1;
            return;
        }
        if (maja.tickCount % 2 == 0) {
            double yaw = Math.toRadians(maja.getYRot());
            double jx = maja.getX() - Math.sin(yaw) * 140.0;
            double jz = maja.getZ() + Math.cos(yaw) * 140.0;
            level.sendParticles(ParticleTypes.SPLASH, jx, sea + 0.4, jz, 24, 14.0, 0.6, 6.0, 0.28);
            level.sendParticles(ParticleTypes.CLOUD, jx, sea + 1.6, jz, 10, 10.0, 0.8, 3.0, 0.06);
            level.sendParticles(ParticleTypes.BLOCK, jx, sea + 0.2, jz, 18, 12.0, 0.8, 4.0, 0.04);
        }
        if (Math.abs(maja.getY() - target) <= 1.0 || breachTicks > 400) {
            breached = true;
            // Settle back down to the validated cruising depth.
            riseSafely(level, maja, -Math.min(1.2, Math.max(0.25, (maja.getY() - sea + 32.0) * 0.12)));
        }
    }

    /** Moves the titan vertically only; the horizontal position and yaw are preserved. */
    private static boolean riseSafely(ServerLevel level, ElGranMajaEntity maja, double delta) {
        double oldY = maja.getY();
        Vec3 next = maja.position().add(0.0, delta, 0.0);
        // The conservative envelope must be clear at the TARGET pose, not only at the current one.
        if (!MajaCollision.clear(level, next, maja.getYRot())) return false;
        maja.setDeltaMovement(0.0, delta, 0.0);
        maja.move(MoverType.SELF, new Vec3(0.0, delta, 0.0));
        // move() resolves against the tiny vanilla hitbox; make sure the validated
        // vertical pose was actually reached (x and z must never change).
        if (Math.abs(maja.getY() - next.y) > 1e-4) maja.setPos(maja.getX(), next.y, maja.getZ());
        return Math.abs(maja.getY() - oldY) > 1e-6 || Math.abs(delta) < 1e-6;
    }

    private boolean cancel(String why) {
        status = why;
        job = null;
        breachTicks = 0;
        breached = false;
        // Rare event: a long cooldown keeps the titan from hoovering up every island.
        cooldown = MEAL_COOLDOWN + (int) (Math.random() * MEAL_COOLDOWN);
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
                // Whole islands: small / medium / bowl islets. Bites: large and medium rims.
                boolean whole = island.kind >= 2 && island.kind <= 4 && island.R <= MAX_WHOLE_ISLAND_RADIUS;
                boolean bite = (island.kind == 1 || island.kind == 2) && island.R > MAX_WHOLE_ISLAND_RADIUS;
                if (!whole && !bite) continue;
                if (!DeepOcean.contains((int) Math.floor(island.cx), (int) Math.floor(island.cz))) continue;
                if (island.base < maja.getY() - 8 || island.base > maja.getY() + 75) continue;
                double d = Math.hypot(island.cx - mouthX, island.cz - mouthZ);
                if (d > island.maxReach() + 30 || d >= bestDistance) continue;
                if (safe(level, maja, island, whole)) { best = island; bestDistance = d; }
            }
        }
        return best;
    }

    private static boolean safe(ServerLevel level, ElGranMajaEntity maja, CityPlan.Island island, boolean wholeIsland) {
        MajaWorldData data = MajaWorldData.get(level);
        double radius = (wholeIsland ? island.maxReach() : BITE_RADIUS) + 8.0;
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

    /**
     * Scan a bounded slice each tick; abort before any destruction if an unfamiliar block appears.
     *
     * <p>Whole islands accept their bare stone rims ({@code S_BARE}) and the natural
     * Dynamic Trees foliage; bites only ever remove the organic, jagged outer rim.
     */
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
                if (col.isl != j.island || col.plot || col.deckY != Integer.MIN_VALUE || col.waterTop != Integer.MIN_VALUE) {
                    nextColumn(j); continue;
                }
                if (j.wholeIsland) {
                    // A whole island may be grass, bare bleached stone or a cobbled rim.
                    if (col.surface != ElisarChunkGenerator.S_GRASS && col.surface != ElisarChunkGenerator.S_BARE
                            && col.surface != ElisarChunkGenerator.S_COBBLE) { nextColumn(j); continue; }
                } else {
                    // A bite only chews the organic outer rim of a large island.
                    if (col.surface != ElisarChunkGenerator.S_GRASS) { nextColumn(j); continue; }
                }
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

    /**
     * Natural blocks of the wild islands, including the Dynamic Trees addon foliage
     * (namespace {@code riftworlddt}) that grows on them.
     */
    private static boolean natural(BlockState s) {
        // v0.46.0: рифт-кристалл генерируется гнёздами прямо в коренном камне диких
        // островов, поэтому пожиратель обязан считать его природной породой — иначе
        // сканирование прерывается на первом же гнезде и остров остаётся несъеденным.
        if (s.is(ElisarBlocks.STONE.get()) || s.is(ElisarBlocks.STONE_MOSSY.get())
                || s.is(ElisarBlocks.GRASS.get()) || s.is(ElisarBlocks.SOIL.get())
                || s.is(ElisarBlocks.CRUST.get()) || s.is(ElisarBlocks.MOSS.get())
                || s.is(ElisarBlocks.DUCAT_ORE.get()) || s.is(ElisarBlocks.RIFTCRYSTAL_ORE.get())
                || s.is(ElisarBlocks.TALL_GRASS.get())
                || s.is(ElisarBlocks.FLOWERS.get()) || s.is(ElisarBlocks.AMBRA_LOG.get())
                || s.is(ElisarBlocks.AMBRA_LEAVES.get())) return true;
        // v0.49.0: the wild-island surface is not only Elisar grass — it also carries
        // bleached cobble rims, path slabs, vanilla turf and wild flowers. All of them
        // are natural terrain of the island and must be edible, otherwise the titan
        // aborts on the first rim block and the island survives untouched.
        if (s.is(ElisarBlocks.COBBLE.get()) || s.is(ElisarBlocks.COBBLE_SLAB.get())
                || s.is(ElisarBlocks.PATH.get())) return true;
        if (s.is(net.minecraft.world.level.block.Blocks.GRASS)
                || s.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                || s.is(net.minecraft.world.level.block.Blocks.FERN)
                || s.is(net.minecraft.world.level.block.Blocks.LARGE_FERN)
                || s.is(net.minecraft.world.level.block.Blocks.DIRT)
                || s.is(net.minecraft.world.level.block.Blocks.DIRT_PATH)
                || s.is(net.minecraft.world.level.block.Blocks.COARSE_DIRT)
                || s.is(net.minecraft.world.level.block.Blocks.COBBLESTONE)
                || s.is(net.minecraft.world.level.block.Blocks.MOSSY_COBBLESTONE)
                || s.is(net.minecraft.world.level.block.Blocks.ANDESITE)
                || s.is(net.minecraft.world.level.block.Blocks.CALCITE)
                || s.is(net.minecraft.world.level.block.Blocks.DIORITE)
                || s.is(net.minecraft.world.level.block.Blocks.OXEYE_DAISY)
                || s.is(net.minecraft.world.level.block.Blocks.LILY_OF_THE_VALLEY)
                || s.is(net.minecraft.world.level.block.Blocks.PINK_TULIP)
                || s.is(net.minecraft.world.level.block.Blocks.ALLIUM)
                || s.is(net.minecraft.world.level.block.Blocks.ORANGE_TULIP)
                || s.is(net.minecraft.world.level.block.Blocks.CORNFLOWER)
                || s.is(net.minecraft.world.level.block.Blocks.DANDELION)) return true;
        // v0.49.0: Dynamic Trees grows real roots, trunks, branches and leaves on the
        // wild islands. Those blocks are natural terrain and must be edible, otherwise
        // the titan aborts on the first tree and the island survives.
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(s.getBlock());
        if (key == null || !key.getNamespace().equals("riftworlddt")) return false;
        String path = key.getPath();
        return path.startsWith("rooty_") || path.contains("branch") || path.contains("leaves")
                || path.contains("log") || path.contains("trunk");
    }
}
