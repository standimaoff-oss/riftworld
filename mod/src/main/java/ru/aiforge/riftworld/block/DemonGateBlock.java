package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import ru.aiforge.riftworld.flight.PortalRitual;

/**
 * Невидимый наземный якорь уходящего в небо 3D-Прорыва Пространства (БЕЗ квадратной рамки и портальных блоков):
 * <ul>
 *   <li>Рендерится исключительно 3D-геометрией {@code DemonGateRenderer} от узкого лезвия у земли до огромного разлома в небе;</li>
 *   <li>Поддерживает интерактивное пробуждение ритуала: поднести кристалл/дар ПКМ, нажать ПКМ рукой, ударить ЛКМ или шагнуть в разлом.</li>
 * </ul>
 */
public class DemonGateBlock extends BaseEntityBlock {
    private static final VoxelShape HIT_SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    public DemonGateBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.QUARTZ)
                .strength(-1.0F, 3600000.0F)
                .sound(SoundType.AMETHYST)
                .lightLevel(s -> 15)
                .noCollission()
                .noLootTable());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return HIT_SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState s) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DemonGateBE(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp) {
            ItemStack held = player.getItemInHand(hand);
            boolean offered = PortalRitual.isOfferingItem(held);
            if (PortalRitual.start(sp, pos, offered ? "offer" : "press")) {
                if (offered && !sp.getAbilities().instabuild) {
                    held.shrink(1);
                }
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            PortalRitual.start(sp, pos, "break");
        }
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer sp) {
            PortalRitual.start(sp, pos, "step");
        }
    }
}
