package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import ru.aiforge.riftworld.registry.ModItems;

/** The big control wheel with five resonance-stone sockets. */
public class PortalHelmBlock extends ModelBlock {
    public PortalHelmBlock(ModelKind kind, Properties props) {
        super(kind, props);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof RiftBlockEntity be)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (held.is(ModItems.RESONANCE_STONE.get()) && be.stoneCount() < 5) {
            for (int i = 0; i < 5; ++i) {
                if ((be.stones & (1 << i)) == 0) {
                    be.stones |= 1 << i;
                    break;
                }
            }
            if (!player.getAbilities().instabuild) held.shrink(1);
            be.sync();
            int n = be.stoneCount();
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.2F, 0.7F + 0.1F * n);
            level.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 0.8F, 1.0F);
            player.displayClientMessage(Component.translatable(n >= 5 ? "message.riftworld.stones_all" : "message.riftworld.stone_set", n), true);
            if (n >= 5) LabPower.start((net.minecraft.server.level.ServerLevel) level, pos, be);
        } else if (be.stoneCount() >= 5 && be.stage == LabPower.IDLE) {
            LabPower.start((net.minecraft.server.level.ServerLevel) level, pos, be);
            player.displayClientMessage(Component.translatable("message.riftworld.stones_all", 5), true);
        } else {
            player.displayClientMessage(Component.translatable("message.riftworld.stones_status", be.stoneCount()), true);
        }
        return InteractionResult.CONSUME;
    }
}
