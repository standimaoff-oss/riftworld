package ru.aiforge.riftworld.arena;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;

/**
 * Seam between the arena sequence (C-3) and the boss itself (part A: model, animations, AI). The arena calls these at the
 * awakening and on reset; the columns heal whatever living entity {@link #awaken} returns.
 */
public final class BossHooks {
    private BossHooks() {
    }

    /** Called when a player approaches the throne after the candles are lit. Returns the boss UUID (null = no boss yet). */
    @Nullable
    public static UUID awaken(ServerLevel level, ArenaCoreBlockEntity core) {
        return null;
    }

    /** Called when the arena resets (hall empty for a minute): remove the boss, guards etc. */
    public static void reset(ServerLevel level, ArenaCoreBlockEntity core) {
    }
}
