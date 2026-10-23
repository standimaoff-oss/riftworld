package ru.aiforge.riftworld.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.entity.SeaFishEntity;

/** Рев 8: яйцо призыва морской живности — кладёт в воду конкретный вид рыбы. */
public class SeaEggItem extends Item {
    private final int variant;

    public SeaEggItem(int variant, Properties props) {
        super(props);
        this.variant = variant;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) return InteractionResult.PASS;
        SeaFishEntity.spawn(sl, Vec3.atCenterOf(pos), variant);
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_FISH, SoundSource.NEUTRAL, 0.9F, 1.1F);
        ItemStack st = ctx.getItemInHand();
        if (ctx.getPlayer() != null && !ctx.getPlayer().isCreative()) st.shrink(1);
        return InteractionResult.CONSUME;
    }
}
