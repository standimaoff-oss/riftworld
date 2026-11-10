package ru.aiforge.riftworld;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * El Gran Maja's feeding behaviour.
 *
 * <p>v0.49.0: island eating is now ENABLED BY DEFAULT — the titan is a rare event that
 * breaches out of the ocean and devours small wild islands. It is still fail-closed:
 * unjournalled chunks, any nearby player, a single player-placed block, block entities
 * and {@code mobGriefing=false} all stop the feeding immediately.
 */
public final class MajaConfig {
    private static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue ISLAND_EATING;
    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("maja");
        ISLAND_EATING = b.comment(
                "Allow El Gran Maja to consume eligible wild islands (always enabled by default since v0.49.0).",
                "Make a world backup first. Other mods can change blocks without Forge player-placement events.",
                "Unjournalled chunks, nearby players, ANY player-placed block, block entities and",
                "mobGriefing=false all stop eating. Set to false to disable the titan entirely.")
                .define("allowIslandEating", true);
        b.pop();
        SPEC = b.build();
    }
    private MajaConfig() {}

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "riftworld-maja.toml");
    }

    public static boolean islandEatingEnabled() {
        try { return ISLAND_EATING.get(); }
        catch (IllegalStateException ignored) { return true; }
    }
}
