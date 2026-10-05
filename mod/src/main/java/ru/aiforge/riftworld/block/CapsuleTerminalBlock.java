package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Invisible hit box over the capsule console (the console itself is part of the capsule GLB). */
public class CapsuleTerminalBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public CapsuleTerminalBlock(Properties props) {
        super(props);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    private static final java.util.Map<java.util.UUID, Long> CONFIRM = new java.util.HashMap<>();

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof net.minecraft.server.level.ServerPlayer sp) || ru.aiforge.riftworld.flight.Flight.locked(player)) return InteractionResult.CONSUME;
        level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.6F, 1.4F);
        LaunchCoreBlockEntity core = null;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-3, -2, -3), pos.offset(3, 0, 3)))
            if (level.getBlockEntity(p) instanceof LaunchCoreBlockEntity c) {
                core = c;
                break;
            }
        if (level.dimension() != Level.OVERWORLD) {
            player.displayClientMessage(Component.translatable("message.riftworld.terminal_arrived"), false);
            return InteractionResult.CONSUME;
        }
        if (core == null || core.stage != LaunchCoreBlockEntity.DONE) {
            player.displayClientMessage(Component.translatable("message.riftworld.terminal_wait"), true);
            return InteractionResult.CONSUME;
        }
        BlockPos c = core.getBlockPos();
        if (player.position().distanceToSqr(c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5) > 16) {
            player.displayClientMessage(Component.translatable("message.riftworld.terminal_center"), true);
            return InteractionResult.CONSUME;
        }
        long now = level.getGameTime();
        Long last = CONFIRM.get(player.getUUID());
        if (last != null && now - last <= 100) {
            CONFIRM.remove(player.getUUID());
            if (ru.aiforge.riftworld.flight.Flight.start(sp, core))
                player.displayClientMessage(Component.translatable("message.riftworld.terminal_launch"), true);
            return InteractionResult.CONSUME;
        }
        CONFIRM.put(player.getUUID(), now);
        level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.8F, 1.6F);
        player.displayClientMessage(Component.translatable("message.riftworld.terminal_route"), false);
        player.displayClientMessage(Component.translatable("message.riftworld.terminal_confirm"), true);
        return InteractionResult.CONSUME;
    }
}
