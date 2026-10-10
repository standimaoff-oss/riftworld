package ru.aiforge.riftworld.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Part 10: gold sigil inlay of the temple - dormant metal until the radiant-mosaic wave WASHES over it, then it catches
 * fire with the runes and stays lit (warm steady glow, like the rest of the sanctum floor).
 */
public class GoldRuneBlock extends Block {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public GoldRuneBlock(Properties p) {
        super(p.lightLevel(s -> s.getValue(LIT) ? 13 : 0));
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(LIT);
    }
}
