package ru.aiforge.riftworld.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
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
public class SeaPlantBlock extends BushBlock implements SimpleWaterloggedBlock {
    /** Растение всегда в толще воды: без этого вода вокруг него рисует грани — «стеклянные кубики». */
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14, 14);
    private static final VoxelShape SHAPE_TALL = Block.box(2, 0, 2, 14, 16, 14);
    private final boolean tall;

    public SeaPlantBlock(BlockBehaviour.Properties p) {
        this(p, false);
    }

    public SeaPlantBlock(BlockBehaviour.Properties p, boolean tall) {
        super(p);
        this.tall = tall;
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(WATERLOGGED);
    }

    @Override
    public FluidState getFluidState(BlockState s) {
        return s.getValue(WATERLOGGED)
                ? ru.aiforge.riftworld.registry.ModFluids.SOURCE.get().defaultFluidState()
                : Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public BlockState updateShape(BlockState s, Direction dir, BlockState neighbor, LevelAccessor lvl, BlockPos pos, BlockPos npos) {
        if (s.getValue(WATERLOGGED)) {
            lvl.scheduleTick(pos, ru.aiforge.riftworld.registry.ModFluids.SOURCE.get(),
                    ru.aiforge.riftworld.registry.ModFluids.SOURCE.get().getTickDelay(lvl));
        }
        return super.updateShape(s, dir, neighbor, lvl, pos, npos);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
        FluidState f = ctx.getLevel().getFluidState(ctx.getClickedPos());
        return defaultBlockState().setValue(WATERLOGGED,
                f.getType() == ru.aiforge.riftworld.registry.ModFluids.SOURCE.get() || f.getType() == Fluids.WATER);
    }

    @Override
    protected boolean mayPlaceOn(BlockState below, BlockGetter level, BlockPos pos) {
        return below.is(ElisarBlocks.SILT.get()) || below.is(ElisarBlocks.SEABED.get()) || below.is(ElisarBlocks.REEF_STONE.get())
                || below.is(ElisarBlocks.STONE.get()) || below.is(ElisarBlocks.STONE_MOSSY.get()) || below.is(ElisarBlocks.CORAL_BRAIN.get())
                || below.is(ElisarBlocks.SEA_GRASS.get()) || below.is(ElisarBlocks.SEA_KELP.get()) || below.is(ElisarBlocks.SEA_KELP_TOP.get())
                || below.is(ElisarBlocks.CORAL_BRANCH.get()) || below.is(ElisarBlocks.CORAL_FAN.get())
                || below.is(ElisarBlocks.CORAL_TUBE.get()) || below.is(ElisarBlocks.CORAL_GLOW.get())
                // Рев 10: новая жизнь рифа растёт друг на друге и на рифовых шапках
                || below.is(ElisarBlocks.CORAL_CAP_PINK.get()) || below.is(ElisarBlocks.CORAL_CAP_TEAL.get())
                || below.is(ElisarBlocks.CORAL_CAP_VIOLET.get()) || below.is(ElisarBlocks.ANEMONE.get())
                || below.is(ElisarBlocks.CORAL_BUSH.get()) || below.is(ElisarBlocks.TUBE_WORMS.get())
                // Рев 11: новые кораллы-растения растут друг на друге и на шапках
                || below.is(ElisarBlocks.STAGHORN_CORAL.get()) || below.is(ElisarBlocks.FEATHER_CORAL.get())
                || below.is(ElisarBlocks.SEA_WHIP.get()) || below.is(ElisarBlocks.TABLE_CORAL.get())
                || below.is(ElisarBlocks.TUBE_SPONGE_CLUSTER.get()) || below.is(ElisarBlocks.RIBBON_CORAL.get())
                || below.is(ElisarBlocks.COBBLE.get());
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return (tall ? SHAPE_TALL : SHAPE).move(s.getOffset(level, pos).x, 0, s.getOffset(level, pos).z);
    }
}
