package ru.aiforge.riftworld.item;

/** Item drawn with a GLB model by ModelItemRenderer (scythe, guard sickle). */
public interface GlbItem {
    /** Model name under assets/riftworld/models/glb/. */
    String glb();

    /** Grip-to-tip length in blocks (used to fit the inventory icon). */
    float length();
}
