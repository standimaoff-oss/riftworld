package ru.aiforge.riftworld.block;

import net.minecraft.world.level.block.EntityBlock;

/** Lab models drawn by the GLB block-entity renderer. Elisar props stay plain {@link ModelBlock}s (baked, no block entity). */
public class ModelEntityBlock extends ModelBlock implements EntityBlock {
    public ModelEntityBlock(ModelKind kind, Properties props) {
        super(kind, props);
    }
}
