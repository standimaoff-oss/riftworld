package ru.aiforge.riftworld.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.entity.SeaFishEntity;

/** Рев 8 + v0.26.0: яйцо призыва морской живности — работает как по твёрдому блоку, так и прямо по воде! */
public class SeaEggItem extends Item {
    private final int variant;

    public SeaEggItem(int variant, Properties props) {
        super(props);
        this.variant = variant;
    }

    public int variant() {
        return variant;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel sl)) return InteractionResult.PASS;
        SeaFishEntity.spawn(sl, Vec3.atCenterOf(pos), variant);
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_FISH, SoundSource.NEUTRAL, 0.9F, 1.1F);
        ItemStack st = ctx.getItemInHand();
        if (ctx.getPlayer() != null) {
            ctx.getPlayer().awardStat(Stats.ITEM_USED.get(this));
            if (!ctx.getPlayer().isCreative()) st.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        BlockPos pos = hit.getBlockPos();
        if (level.getFluidState(pos).isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide && level instanceof ServerLevel sl) {
            SeaFishEntity.spawn(sl, Vec3.atCenterOf(pos), variant);
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_FISH, SoundSource.NEUTRAL, 0.9F, 1.1F);
            player.awardStat(Stats.ITEM_USED.get(this));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
