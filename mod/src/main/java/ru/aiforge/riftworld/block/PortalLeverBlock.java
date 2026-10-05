package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import ru.aiforge.riftworld.registry.ModBlocks;

/** Main launch lever. With fewer than five stones a note falls from the ceiling. */
public class PortalLeverBlock extends ModelEntityBlock {
    public static final int PULL_TICKS = 30;

    public PortalLeverBlock(ModelKind kind, Properties props) {
        super(kind, props);
    }

    static RiftBlockEntity findHelm(Level level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-14, -4, -14), pos.offset(14, 4, 14))) {
            if (level.getBlockState(p).is(ModBlocks.PORTAL_HELM.get()) && level.getBlockEntity(p) instanceof RiftBlockEntity be) {
                return be;
            }
        }
        return null;
    }

    public static ItemStack noteBook(int stones) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", "\u0417\u0430\u043f\u0438\u0441\u043a\u0430 / Note");
        tag.putString("author", "\u0414-\u0440 \u0412\u0435\u0440\u0435\u0441\u043e\u0432");
        tag.putBoolean("resolved", true);
        ListTag pages = new ListTag();
        pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable("book.riftworld.note.p1", stones))));
        pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable("book.riftworld.note.p2"))));
        tag.put("pages", pages);
        return book;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof RiftBlockEntity be)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        long now = level.getGameTime();
        if (now - be.animStart < PULL_TICKS) return InteractionResult.CONSUME;
        be.animStart = now;
        be.sync();
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.7F, 0.5F);
        RiftBlockEntity helm = findHelm(level, pos);
        int n = helm == null ? 0 : helm.stoneCount();
        if (n < 5) {
            level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 0.6F);
            player.displayClientMessage(Component.translatable("message.riftworld.not_enough", n), true);
            ItemEntity e = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 3.6, pos.getZ() + 0.5, noteBook(n));
            e.setDeltaMovement(0, -0.05, 0);
            e.setPickUpDelay(10);
            level.addFreshEntity(e);
        } else if (helm.stage != LabPower.POWERED) {
            level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 1.2F);
            player.displayClientMessage(Component.translatable("message.riftworld.booting"), true);
        } else {
            LaunchCoreBlockEntity core = LaunchCoreBlock.find(level, pos);
            if (core != null && core.startLaunch()) {
                level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5F, 0.5F);
                player.displayClientMessage(Component.translatable("message.riftworld.launch"), true);
            } else {
                player.displayClientMessage(Component.translatable(core == null ? "message.riftworld.ready" : "message.riftworld.launch_busy"), true);
            }
        }
        return InteractionResult.CONSUME;
    }
}
