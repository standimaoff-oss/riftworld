package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Invisible collision cell of the suspension-bridge deck (Rev 7). Unlike the old flat 13/16 plate, every cell carries
 * its own top height in 1/16 steps ({@code top=1..16}) - the generator/assembly writes the value sampled from the
 * sagging curve, so a bridge at 25 degrees walks like a continuous ramp instead of a staircase.
 */
public class BridgeWalkBlock extends Block {
    public static final IntegerProperty TOP = IntegerProperty.create("top", 1, 16);
    private static final VoxelShape[] SHAPES = new VoxelShape[17];

    static {
        for (int i = 1; i <= 16; ++i) SHAPES[i] = Block.box(0, 0, 0, 16, i, 16);
    }

    public BridgeWalkBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(TOP, 16));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(TOP);
    }

    /** The plate that matches the deck surface inside this cell. */
    public static VoxelShape shapeFor(int top) {
        return SHAPES[Math.max(1, Math.min(16, top))];
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return shapeFor(s.getValue(TOP));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return shapeFor(s.getValue(TOP));
    }

    @Override
    public VoxelShape getVisualShape(BlockState s, BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return Shapes.empty();
    }
}
