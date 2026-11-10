package ru.aiforge.riftworld.world;

/**
 * Pure fail-closed decision shared by the island eater and its boundary tests.
 *
 * <p>v0.49.0: the gate is now maximally conservative — a SINGLE recorded player
 * placement anywhere in the island column aborts the feeding (previously up to four
 * placements were tolerated). Old/unknown chunks, any nearby player, block entities
 * and {@code mobGriefing=false} remain hard stops.
 */
public final class MajaSafetyPolicy {
    private MajaSafetyPolicy() {}

    /**
     * @param allChunksNewAndLoaded every chunk of the island column is journal-trusted and resident
     * @param placedBlocks         number of recorded player placements inside the island column
     * @param anyPlayerNearby      a player is close enough to witness the feeding
     * @param mobGriefingAllowed   the gamerule (and Forge event) permits block destruction
     */
    public static boolean mayDevour(boolean allChunksNewAndLoaded, int placedBlocks,
                                    boolean anyPlayerNearby, boolean mobGriefingAllowed) {
        return allChunksNewAndLoaded && placedBlocks == 0
                && !anyPlayerNearby && mobGriefingAllowed;
    }
}
