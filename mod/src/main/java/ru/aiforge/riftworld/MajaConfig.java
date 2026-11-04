package ru.aiforge.riftworld;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/** Destructive behaviour is opt-in until it has been tested against the user's mod setup. */
public final class MajaConfig {
    private static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue ISLAND_EATING;
    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("maja");
        ISLAND_EATING = b.comment(
                "EXPERIMENTAL: allow El Gran Maja to consume eligible wild islands.",
                "Make a world backup first. Other mods can change blocks without Forge player-placement events.",
                "Unjournalled chunks, nearby players, five placements, block entities and mobGriefing=false still stop eating.")
                .define("allowIslandEating", false);
        b.pop();
        SPEC = b.build();
    }
    private MajaConfig() {}

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "riftworld-maja.toml");
    }

    public static boolean islandEatingEnabled() {
        try { return ISLAND_EATING.get(); }
        catch (IllegalStateException ignored) { return false; }
    }
}
