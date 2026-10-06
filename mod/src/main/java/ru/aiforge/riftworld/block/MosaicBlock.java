package ru.aiforge.riftworld.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Rev 4 item 3: temple floor mosaic ring tiles. STAGE 0..3 = how strongly the sigils glow while the ritual runs. */
public class MosaicBlock extends Block {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);

    public MosaicBlock(Properties p) {
        super(p.lightLevel(s -> s.getValue(STAGE) * 5));
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(STAGE);
    }
}
