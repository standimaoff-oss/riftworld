package ru.aiforge.riftworld.world;

/** Pure fail-closed decision shared by the island eater and its boundary tests. */
public final class MajaSafetyPolicy {
    private MajaSafetyPolicy() {}

    public static boolean mayDevour(boolean allChunksNewAndLoaded, int placedBlocks,
                                    boolean anyPlayerNearby, boolean mobGriefingAllowed) {
        return allChunksNewAndLoaded && placedBlocks >= 0 && placedBlocks < 5
                && !anyPlayerNearby && mobGriefingAllowed;
    }
}
