package ru.aiforge.riftworld.world;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

/** Per-world spawn and block-placement journal. Unknown/old chunks are never trusted. */
public final class MajaWorldData extends SavedData {
    private final Set<Long> newChunks = new HashSet<>();
    private final Set<Long> unsafeChunks = new HashSet<>();
    private long trustAfter = -1;
    private final Set<Long> placed = new HashSet<>();
    private final Map<Long, Set<Long>> placedByChunk = new HashMap<>();
    private final Set<Long> spawnedSites = new HashSet<>();

    public static MajaWorldData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(MajaWorldData::load, MajaWorldData::new, "riftworld_maja_safety");
    }

    public static MajaWorldData load(CompoundTag tag) {
        MajaWorldData data = new MajaWorldData();
        for (long x : tag.getLongArray("NewChunks")) data.newChunks.add(x);
        for (long x : tag.getLongArray("UnsafeChunks")) data.unsafeChunks.add(x);
        if (tag.contains("TrustAfter")) data.trustAfter = tag.getLong("TrustAfter");
        for (long x : tag.getLongArray("Placed")) data.indexPlacement(BlockPos.of(x));
        for (long x : tag.getLongArray("SpawnedSites")) data.spawnedSites.add(x);
        return data;
    }

    private static long[] array(Set<Long> values) { return values.stream().mapToLong(Long::longValue).toArray(); }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("NewChunks", array(newChunks));
        tag.putLongArray("UnsafeChunks", array(unsafeChunks));
        tag.putLong("TrustAfter", trustAfter);
        tag.putLongArray("Placed", array(placed));
        tag.putLongArray("SpawnedSites", array(spawnedSites));
        return tag;
    }

    public void markNewChunk(int cx, int cz, long time) {
        long key = ChunkPos.asLong(cx, cz);
        if (time > trustAfter && !unsafeChunks.contains(key) && newChunks.add(key)) setDirty();
    }
    public boolean isNewChunk(int cx, int cz) {
        long key = ChunkPos.asLong(cx, cz);
        return !unsafeChunks.contains(key) && newChunks.contains(key);
    }
    /** Piston motion may move a recorded block without a normal placement event. */
    public void invalidateAround(BlockPos pos) {
        int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
        boolean changed = false;
        for (int x = cx - 2; x <= cx + 2; ++x) for (int z = cz - 2; z <= cz + 2; ++z) {
            long key = ChunkPos.asLong(x, z);
            changed |= unsafeChunks.add(key);
            changed |= newChunks.remove(key);
        }
        if (changed) setDirty();
    }
    /** Remote world-edit commands cannot be attributed to old generated blocks. */
    public void invalidateAfterCommand(long time) {
        newChunks.clear();
        trustAfter = Math.max(trustAfter, time + 40);
        setDirty();
    }
    private void indexPlacement(BlockPos pos) {
        long packed = pos.asLong();
        if (placed.add(packed)) {
            long chunk = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
            placedByChunk.computeIfAbsent(chunk, unused -> new HashSet<>()).add(packed);
        }
    }
    public void placed(BlockPos pos) {
        if (!placed.contains(pos.asLong())) { indexPlacement(pos); setDirty(); }
    }
    public void broken(BlockPos pos) {
        long packed = pos.asLong();
        if (placed.remove(packed)) {
            long chunk = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
            Set<Long> entries = placedByChunk.get(chunk);
            if (entries != null && entries.remove(packed) && entries.isEmpty()) placedByChunk.remove(chunk);
            setDirty();
        }
    }
    public boolean wasPlaced(BlockPos pos) { return placed.contains(pos.asLong()); }
    public boolean hasSpawned(long site) { return spawnedSites.contains(site); }
    public void recordSpawn(long site) { if (spawnedSites.add(site)) setDirty(); }

    private Iterable<Long> placementsNear(double x, double z, double radius) {
        Set<Long> nearby = new HashSet<>();
        int minX = (int) Math.floor(x - radius) >> 4, maxX = (int) Math.floor(x + radius) >> 4;
        int minZ = (int) Math.floor(z - radius) >> 4, maxZ = (int) Math.floor(z + radius) >> 4;
        for (int cx = minX; cx <= maxX; ++cx) for (int cz = minZ; cz <= maxZ; ++cz) {
            Set<Long> chunk = placedByChunk.get(ChunkPos.asLong(cx, cz));
            if (chunk != null) nearby.addAll(chunk);
        }
        return nearby;
    }

    /** Conservative count in the island's entire vertical column, not just at its surface. */
    public int placedNear(double x, double z, double radius) {
        double radius2 = radius * radius;
        int count = 0;
        for (long packed : placementsNear(x, z, radius)) {
            BlockPos pos = BlockPos.of(packed);
            double dx = pos.getX() + 0.5 - x, dz = pos.getZ() + 0.5 - z;
            // v0.49.0: the exact count matters now — a SINGLE player placement aborts the meal.
            if (dx * dx + dz * dz <= radius2) ++count;
        }
        return count;
    }

    /** Preserve recorded player blocks and their immediate support during feeding. */
    public void protectPlacedNear(double x, double z, double radius, Set<Long> output) {
        output.clear();
        double radius2 = radius * radius;
        for (long packed : placementsNear(x, z, radius)) {
            BlockPos p = BlockPos.of(packed);
            double dx = p.getX() + 0.5 - x, dz = p.getZ() + 0.5 - z;
            if (dx * dx + dz * dz > radius2) continue;
            for (int ox = -1; ox <= 1; ++ox) for (int oy = -1; oy <= 1; ++oy)
                for (int oz = -1; oz <= 1; ++oz) output.add(p.offset(ox, oy, oz).asLong());
        }
    }

}
