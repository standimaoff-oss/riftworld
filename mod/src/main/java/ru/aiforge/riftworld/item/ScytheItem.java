package ru.aiforge.riftworld.item;

import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import ru.aiforge.riftworld.entity.ScytheItemEntity;

/**
 * «Коса Морграта» - the boss's war scythe. Heavy melee weapon (13 damage, slow) with four super attacks on right click:
 * plain - «Жатва» (360° reaping spin); while sneaking - «Огненная борозда» (a furrow of fire erupting along the ground);
 * while sprinting - «Рывок жнеца» (dash through enemies); in the air - «Падение кометы» (slam with a shock wave).
 * Looks like an ordinary weapon in the hand; dropped, it lies as a large glowing scythe (ScytheItemEntity).
 */
public class ScytheItem extends SwordItem implements GlbItem {
    public ScytheItem(Properties p) {
        super(RiftTiers.MORGRATH, 12, -3.0F, p);
    }

    @Override
    public String glb() {
        return "scythe";
    }

    @Override
    public float length() {
        return 2.8F;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            int cd;
            if (player.isShiftKeyDown()) cd = ScytheFx.furrow(sp);
            else if (!player.onGround() && !player.isInWater() && player.getDeltaMovement().y < 0.25) cd = ScytheFx.slam(sp);
            else if (player.isSprinting()) cd = ScytheFx.dash(sp);
            else cd = ScytheFx.spin(sp);
            player.getCooldowns().addCooldown(this, cd);
            stack.hurtAndBreak(2, player, p -> p.broadcastBreakEvent(hand));
        }
        player.swing(hand, true);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.riftworld.morgrath_scythe.tip1").withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable("item.riftworld.morgrath_scythe.tip2").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.riftworld.morgrath_scythe.tip3").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.riftworld.morgrath_scythe.tip4").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.riftworld.morgrath_scythe.tip5").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.riftworld.morgrath_scythe.combo").withStyle(ChatFormatting.RED));
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    @Override
    public Entity createEntity(Level level, Entity location, ItemStack stack) {
        ScytheItemEntity e = new ScytheItemEntity(level, location.getX(), location.getY(), location.getZ(), stack);
        e.setDeltaMovement(location.getDeltaMovement());
        e.setDefaultPickUpDelay();
        return e;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ru.aiforge.riftworld.client.ComboClient.ext(true));
    }

}
