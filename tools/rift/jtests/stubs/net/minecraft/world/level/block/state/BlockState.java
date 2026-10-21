package net.minecraft.world.level.block.state;

import net.minecraft.world.level.block.Block;

/** Test-only stub of Minecraft's BlockState. */
public class BlockState {
    private final Block block;
    public BlockState(Block block) { this.block = block; }
    public Block getBlock() { return block; }
    public boolean isAir() { return false; }
}
