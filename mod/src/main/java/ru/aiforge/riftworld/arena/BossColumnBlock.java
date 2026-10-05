package ru.aiforge.riftworld.arena;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;
import ru.aiforge.riftworld.registry.ArenaBlocks;

/**
 * Rev 3 / C-3: one of the six healing columns of the throne hall. Three cells: base (block entity, renderer), shaft, top
 * (the ember crystal = weak spot). Sunk into its brass socket until the boss awakens, then rises. A hit on the TOP cell
 * (any weapon, fist or arrow) or a PICKAXE on any cell cracks the crystal; three cracks shatter the column.
 */
public class BossColumnBlock extends Block implements EntityBlock {
    /** 0 = base, 1 = shaft, 2 = top (crystal). */
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);
    /** 0 = sunk in the socket, 1 = standing, 2 = shattered stump. */
    public static final IntegerProperty PHASE = IntegerProperty.create("phase", 0, 2);

    private static final VoxelShape PLINTH = Block.box(1, 0, 1, 15, 5, 15);
    private static final VoxelShape SHAFT = Block.box(3.5, 0, 3.5, 12.5, 16, 12.5);
    private static final VoxelShape BASE = Shapes.or(PLINTH, SHAFT);
    private static final VoxelShape TOP = Shapes.or(Block.box(3.5, 0, 3.5, 12.5, 4, 12.5), Block.box(1, 4, 1, 15, 6, 15),
            Block.box(3, 6, 3, 13, 16, 13));
    private static final DustParticleOptions EMBER = new DustParticleOptions(new Vector3f(1.0F, 0.35F, 0.08F), 0.8F);

    public BossColumnBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(PART, 0).setValue(PHASE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(PART, PHASE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (s.getValue(PART)) {
            case 0 -> s.getValue(PHASE) == 0 ? Shapes.empty() : BASE;
            case 1 -> SHAFT;
            default -> TOP;
        };
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? new BossColumnBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ArenaBlocks.COLUMN_BE.get()) return null;
        return (l, p, s, be) -> ((BossColumnBlockEntity) be).serverTick();
    }

    @Nullable
    static BossColumnBlockEntity base(Level level, BlockPos pos, BlockState state) {
        BlockEntity be = level.getBlockEntity(pos.below(state.getValue(PART)));
        return be instanceof BossColumnBlockEntity c ? c : null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return;
        BossColumnBlockEntity be = base(level, pos, state);
        if (be != null) be.hit(player, state.getValue(PART) == 2);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level.isClientSide || state.getValue(PART) != 2) return;
        BossColumnBlockEntity be = base(level, hit.getBlockPos(), state);
        if (be != null) be.hit(projectile.getOwner() instanceof Player p ? p : null, true);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && state.getValue(PART) == 0) {
            for (int i = 1; i <= 2; ++i) {
                BlockPos p = pos.above(i);
                if (level.getBlockState(p).is(this)) level.removeBlock(p, false);
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    /** Embers drifting off the crystal while the column stands. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (state.getValue(PART) != 0 || state.getValue(PHASE) != 1) return;
        double x = pos.getX() + 0.5, y = pos.getY() + 2.6, z = pos.getZ() + 0.5;
        level.addParticle(EMBER, x + (r.nextDouble() - 0.5) * 0.4, y + (r.nextDouble() - 0.5) * 0.4, z + (r.nextDouble() - 0.5) * 0.4, 0, 0.02, 0);
        if (r.nextInt(4) == 0) level.addParticle(ParticleTypes.SMALL_FLAME, x, y + 0.2, z, 0, 0.01, 0);
    }
}
