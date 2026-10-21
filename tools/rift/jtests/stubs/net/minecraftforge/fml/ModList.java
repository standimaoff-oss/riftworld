package net.minecraftforge.fml;

/**
 * Test stub of Forge's mod list. The real class asks the loading mod list; here a static switch plays the role of
 * "Dynamic Trees is installed", so the soft-dependency can be tested both ways in one JVM.
 */
public final class ModList {

    /** id -> loaded. Tests fill this before touching the mod's compat class. */
    public static final java.util.Map<String, Boolean> LOADED = new java.util.HashMap<>();

    /** When set, {@link #get()} throws — emulates running outside Forge (data generation, plain unit tests). */
    public static boolean explode = false;

    public static ModList get() {
        if (explode) throw new IllegalStateException("no mod list outside Forge");
        return INSTANCE;
    }

    private static final ModList INSTANCE = new ModList();

    public boolean isLoaded(String modId) {
        return LOADED.getOrDefault(modId, Boolean.FALSE);
    }
}
