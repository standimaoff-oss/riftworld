package net.minecraft.world.level.block;

import net.minecraft.world.level.block.state.BlockState;

/** Test-only stub of Minecraft's Block (only what TreeSpecies.Blocks needs). */
public class Block {
    public BlockState defaultBlockState() { return new BlockState(this); }
    public String getDescriptionId() { return "block.stub"; }
}
