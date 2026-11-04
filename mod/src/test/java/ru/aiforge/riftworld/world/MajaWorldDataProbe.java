package ru.aiforge.riftworld.world;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/** A small JVM integration test for placement indexing, protection and SavedData persistence. */
public final class MajaWorldDataProbe {
    private static void check(boolean condition, String what) {
        if (!condition) throw new AssertionError(what);
    }
    public static void main(String[] args) {
        MajaWorldData data = new MajaWorldData();
        data.markNewChunk(-1, -1, 0);
        data.markNewChunk(0, 0, 0);
        check(data.isNewChunk(-1, -1) && data.isNewChunk(0, 0), "new chunk including negative coordinates");
        check(!data.isNewChunk(5, 5), "unknown chunk must fail closed");
        for (int i = 0; i < 5; i++) data.placed(new BlockPos(-2 + i, 100 + i, -1));
        check(data.placedNear(0, 0, 20) == 5, "five counted across chunk boundaries");
        data.broken(new BlockPos(2, 104, -1));
        check(data.placedNear(0, 0, 20) == 4, "break updates index");
        Set<Long> protectedCells = new HashSet<>();
        data.protectPlacedNear(0, 0, 20, protectedCells);
        check(protectedCells.contains(new BlockPos(-2, 100, -1).asLong()), "placed block protected");
        check(protectedCells.contains(new BlockPos(-2, 99, -1).asLong()), "support protected");
        check(!protectedCells.contains(new BlockPos(50, 99, -1).asLong()), "protection is local");
        data.recordSpawn(42);
        MajaWorldData loaded = MajaWorldData.load(data.save(new CompoundTag()));
        check(loaded.hasSpawned(42), "spawn site persists");
        check(loaded.placedNear(0, 0, 20) == 4 && loaded.wasPlaced(new BlockPos(-2, 100, -1)), "placements persist");
        loaded.invalidateAround(new BlockPos(0, 70, 0));
        check(!loaded.isNewChunk(0, 0) && !loaded.isNewChunk(-1, -1), "piston invalidates nearby chunks");
        loaded.markNewChunk(0, 0, 500);
        check(!loaded.isNewChunk(0, 0), "unsafe piston area cannot become trusted again");
        loaded.markNewChunk(10, 10, 500);
        loaded.invalidateAfterCommand(500);
        check(!loaded.isNewChunk(10, 10), "remote edit invalidates previously trusted chunks");
        loaded.markNewChunk(11, 11, 540);
        check(!loaded.isNewChunk(11, 11), "command-generated chunks remain untrusted during grace period");
        loaded.markNewChunk(11, 11, 541);
        check(loaded.isNewChunk(11, 11), "later new chunks can be trusted");
        MajaWorldData reloaded = MajaWorldData.load(loaded.save(new CompoundTag()));
        check(!reloaded.isNewChunk(0, 0) && reloaded.isNewChunk(11, 11), "invalidation persists");
        System.out.println("OK: Maja saved journal, player block protection, spawn record and invalidation");
    }
}
