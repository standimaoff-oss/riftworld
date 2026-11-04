package ru.aiforge.riftworld.block;

import java.util.function.Supplier;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import org.jetbrains.annotations.Nullable;

/**
 * Ванильное бревно / древесина с поддержкой снятия коры топором (ПКМ топором -> обтёсанное бревно / древесина).
 */
public class StrippablePillarBlock extends RotatedPillarBlock {
    private final Supplier<? extends Block> stripped;

    public StrippablePillarBlock(Properties props, Supplier<? extends Block> stripped) {
        super(props);
        this.stripped = stripped;
    }

    @Override
    public @Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context,
                                                     ToolAction toolAction, boolean simulate) {
        if (ToolActions.AXE_STRIP.equals(toolAction) && stripped != null) {
            Block target = stripped.get();
            if (target != null) {
                return target.defaultBlockState().setValue(AXIS, state.getValue(AXIS));
            }
        }
        return super.getToolModifiedState(state, context, toolAction, simulate);
    }
}
