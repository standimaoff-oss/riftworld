package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 8-pixel thick industrial pipe along an axis. */
public class PipeBlock extends RotatedPillarBlock {
    private static final VoxelShape Y = box(4, 0, 4, 12, 16, 12);
    private static final VoxelShape X = box(0, 4, 4, 16, 12, 12);
    private static final VoxelShape Z = box(4, 4, 0, 12, 12, 16);

    public PipeBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(AXIS)) {
            case X -> X;
            case Z -> Z;
            default -> Y;
        };
    }
}
