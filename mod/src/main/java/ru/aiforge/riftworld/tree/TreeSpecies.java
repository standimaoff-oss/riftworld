package ru.aiforge.riftworld.tree;

/**
 * Rev 7 part 2: tree species of Elisar. Kind ids are persisted inside {@code CityPlan.Tree} (and in {@code plan.json}),
 * so they must never be renumbered: 0..3 are the old ones, 4..7 are new.
 *
 * <p>Muted palettes only (user rule «не делай яркие слишком»).</p>
 */
public final class TreeSpecies {
    /** Slender pale tree of the ordinary islands (old "sizo" look, now grown by {@link TreeGen}). */
    public static final int SIZO = 0;
    /** Round golden-leaf tree of the ordinary islands. */
    public static final int AMBRA = 1;
    /** Leafless charred tree of the Ashen Lands - untouched by the new engine (its own renderer). */
    public static final int DEAD = 2;
    /** Giant layered grove tree of biome 05 (2x2 trunk, real branches, cloud tiers). */
    public static final int SIZO_GIANT = 3;
    /** Biome 04 Violet Hills: lilac ash with soft lilac blossom patches. */
    public static final int LILAC = 4;
    /** Biome 07 Waterfall Gardens: low fern tree, umbrella crown, aerial roots. */
    public static final int FERN = 5;
    /** Mist Keep: cold grey-green pine. */
    public static final int MIST_PINE = 6;
    /** Ordinary world / city: calm green broadleaf. */
    public static final int OAK = 7;

    public static boolean usesEngine(int kind) {
        return kind == SIZO || kind == AMBRA || kind == SIZO_GIANT || kind == LILAC || kind == FERN || kind == MIST_PINE || kind == OAK;
    }

    /** Blocks of one species: log, leaves and the optional accent (blossom / moss / none). */
    public record Blocks(net.minecraft.world.level.block.Block log, net.minecraft.world.level.block.Block leaves,
                         net.minecraft.world.level.block.Block accent) {
    }

    private TreeSpecies() {
    }
}
