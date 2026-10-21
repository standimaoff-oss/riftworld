package ru.aiforge.riftworld.compat;

/**
 * Rev 7 part 2: soft dependency on Dynamic Trees.
 *
 * <p>The trees of Elisar are built by <b>Dynamic Trees</b> itself: our mod only supplies the species parameters
 * (height, branchiness, tapering, canopy cell kit) and the bark/leaves textures, which live in the separate
 * {@code riftworld_dt} tree pack (folder {@code dtcompat/}). When DT is installed, the built-in generator below must
 * stay silent, otherwise two different trees would grow in the same spot.</p>
 *
 * <p>Nothing here references a Dynamic Trees class, so the mod still compiles and runs without DT; the check is just
 * a mod-list lookup and is cached.</p>
 */
public final class DynamicTreesCompat {
    public static final String MOD_ID = "dynamictrees";

    private static Boolean loaded;
    private static Boolean forced;

    private DynamicTreesCompat() {
    }

    /** True when Dynamic Trees is on the classpath/mod list (cached; safe on both sides). */
    public static synchronized boolean loaded() {
        if (forced != null) return forced;
        if (loaded == null) {
            boolean found = false;
            try {
                found = net.minecraftforge.fml.ModList.get().isLoaded(MOD_ID);
            } catch (Throwable ignored) {
                // no Forge mod list (unit tests, data generation): assume DT is absent
            }
            loaded = found;
        }
        return loaded;
    }

    /**
     * True when the built-in tree engine of this mod has to grow trees. Exactly the opposite of {@link #loaded()}:
     * one place decides who owns the trees, so the generator cannot accidentally plant twice.
     */
    public static boolean ownTreesEnabled() {
        return !loaded();
    }

    /**
     * Test hook: forces the state so the generator can be checked both ways without launching two games.
     * Pass {@code null} to go back to the real mod list (the cached answer is dropped as well).
     */
    public static synchronized void force(Boolean state) {
        forced = state;
        loaded = null;
    }
}
