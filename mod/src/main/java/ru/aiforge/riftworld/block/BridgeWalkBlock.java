package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Invisible collision cell of the rope-bridge deck (Rev 4 item 13). A 13/16 tall plate so the player stands ON the
 * deck with feet slightly above the cell floor. Punch holes in the bridge by breaking it!
 * Fixed: sturdy for carpets and other placements.
 */
public class BridgeWalkBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 13, 16);

    public BridgeWalkBlock(Properties p) {
        super(p);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getVisualShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public boolean isFaceSturdy(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
        return dir == Direction.UP;
    }
}
