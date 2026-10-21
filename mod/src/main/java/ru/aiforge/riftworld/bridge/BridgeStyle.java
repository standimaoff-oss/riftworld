package ru.aiforge.riftworld.bridge;

/**
 * Rev 7 part 1: bridge LOOK per dimension/biome. Muted palettes only (по правке «не делай яркие слишком»).
 *
 * <p>Every style is a real suspension footbridge (as on the user's photo `reports/refs_bridges/user_photo_bridge.jpg`):
 * transverse planks, two longitudinal stringers, a main cable on each side that rises to the post caps, vertical
 * hangers every ~1.2 blocks and a diamond safety net between the hand cable and the deck. Only the materials, the
 * railing (rope / steel cable / chain) and the wear change.</p>
 */
public enum BridgeStyle {
    /** Ordinary world / city: grey stone posts, oak planks, tan rope + light net. */
    PLAINS(0, "plains", "planks_plains", "cable_rope", 0.03F, 1.0F, false, 12),
    /** Biome 04 «Фиолетовые холмы»: mossy stone + sizo wood, rope, a touch of wear. */
    GARDEN(1, "garden", "planks_garden", "cable_rope", 0.06F, 1.08F, false, 12),
    /** Biome 05 «Роща Сизо»: pale stone + sizo, rope, moss on the caps. */
    GROVE(2, "grove", "planks_grove", "cable_rope", 0.09F, 1.14F, false, 12),
    /** Ashen Lands / Ember Ruins: charred wood, chains instead of rope, half the planks burnt out. */
    BURNT(3, "burnt", "planks_burnt", "cable_chain", 0.34F, 1.28F, true, 7),
    /** Mist Keep: keep stone, cold planks, chains, drizzle-worn. */
    MIST(4, "mist", "planks_mist", "cable_chain", 0.14F, 1.18F, false, 10);

    public final int id;
    public final String key;
    /** Base name of the entity plank texture (textures/entity/bridge/&lt;name&gt;.png). */
    public final String plankTex;
    /** Base name of the cable texture (textures/entity/bridge/&lt;name&gt;.png). */
    public final String cableTex;
    /** Fraction of planks that are missing (the rest are drawn). */
    public final float holes;
    /** Sag multiplier (the more worn, the saggier the deck). */
    public final float sagMul;
    /** True for chain railing (no diamond netting panel - just sparse hanging chains). */
    public final boolean chainRail;
    /** Light level of the post lantern. */
    public final int light;

    BridgeStyle(int id, String key, String plankTex, String cableTex, float holes, float sagMul, boolean chainRail, int light) {
        this.id = id;
        this.key = key;
        this.plankTex = plankTex;
        this.cableTex = cableTex;
        this.holes = holes;
        this.sagMul = sagMul;
        this.chainRail = chainRail;
        this.light = light;
    }

    public static BridgeStyle of(int id) {
        for (BridgeStyle s : values()) if (s.id == id) return s;
        return PLAINS;
    }
}
