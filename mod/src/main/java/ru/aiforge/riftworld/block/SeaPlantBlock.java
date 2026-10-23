package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import ru.aiforge.riftworld.registry.ElisarBlocks;

/**
 * Рев 8 «Живое море»: подводное растение (коралл / ламинария / морская трава / пещерный нарост).
 * Растёт только на морском дне — ил, донный песок, рифовый камень, камень и на собственной породе
 * (колонны ламинарии стоят друг на друге). Хитбокс — крестик, без коллизии, ломается мгновенно.
 */
public class SeaPlantBlock extends BushBlock {
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14, 14);
    private static final VoxelShape SHAPE_TALL = Block.box(2, 0, 2, 14, 16, 14);
    private final boolean tall;

    public SeaPlantBlock(BlockBehaviour.Properties p) {
        this(p, false);
    }

    public SeaPlantBlock(BlockBehaviour.Properties p, boolean tall) {
        super(p);
        this.tall = tall;
    }

    @Override
    protected boolean mayPlaceOn(BlockState below, BlockGetter level, BlockPos pos) {
        return below.is(ElisarBlocks.SILT.get()) || below.is(ElisarBlocks.SEABED.get()) || below.is(ElisarBlocks.REEF_STONE.get())
                || below.is(ElisarBlocks.STONE.get()) || below.is(ElisarBlocks.STONE_MOSSY.get()) || below.is(ElisarBlocks.CORAL_BRAIN.get())
                || below.is(ElisarBlocks.SEA_GRASS.get()) || below.is(ElisarBlocks.SEA_KELP.get()) || below.is(ElisarBlocks.SEA_KELP_TOP.get())
                || below.is(ElisarBlocks.CORAL_BRANCH.get()) || below.is(ElisarBlocks.CORAL_FAN.get())
                || below.is(ElisarBlocks.CORAL_TUBE.get()) || below.is(ElisarBlocks.CORAL_GLOW.get());
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return (tall ? SHAPE_TALL : SHAPE).move(s.getOffset(level, pos).x, 0, s.getOffset(level, pos).z);
    }
}
