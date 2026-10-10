package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import ru.aiforge.riftworld.registry.ModBlockEntities;

/** Anchor block entity of the demonic teleport (no data - the block does the work). */
public class DemonGateBE extends BlockEntity {
    public DemonGateBE(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEMON_GATE.get(), pos, state);
    }
}
