package ru.aiforge.riftworld.item;

import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import ru.aiforge.riftworld.entity.CrystalCombatHandler;

/**
 * «Призма Архонта Разлома» ({@code astral_prism}) — уникальный сбалансированный трофей с Босса Колоннады:
 * миниатюрная руническая астролябия с кристаллом Разлома (многоразовая, перезарядка 14 секунд).
 *
 * <p><b>ПКМ — «Шаг сквозь Грань»:</b> мгновенно переносит владельца на 9 блоков вперёд по взгляду
 * в россыпи бирюзовых искр, даёт Сопротивление I и Скорость I на 4 с и оставляет на 5 секунд
 * резонирующий маяк Разлома, пульсирующий лёгким уроном и замедлением по врагам рядом.
 */
public class AstralPrismItem extends Item implements CrystalGeoItem {
    public AstralPrismItem(Properties props) {
        super(props);
    }

    @Override
    public String geoModel() {
        return "astral_prism";
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            ServerLevel sl = sp.serverLevel();
            Vec3 start = sp.getEyePosition();
            Vec3 look = sp.getLookAngle().normalize();
            Vec3 end = start.add(look.scale(9.0));
            BlockHitResult hit = sl.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp));
            Vec3 dest = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation().subtract(look.scale(0.85));

            // Безопасная коррекция высоты, чтобы не застрять в полу
            BlockPos bp = BlockPos.containing(dest.x, dest.y - 1.0, dest.z);
            double targetY = dest.y - 1.0;
            if (sl.getBlockState(bp).isSolid() && sl.getBlockState(bp.above()).isAir() && sl.getBlockState(bp.above(2)).isAir()) {
                targetY = bp.getY() + 1.05;
            }

            Vec3 origin = sp.position();
            sl.sendParticles(ParticleTypes.END_ROD, origin.x, origin.y + 1.0, origin.z, 22, 0.35, 0.65, 0.35, 0.06);
            sl.playSound(null, sp.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.9F, 1.35F);

            sp.teleportTo(dest.x, targetY, dest.z);
            sp.fallDistance = 0.0F;
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, false, true));
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 0, false, true));

            sl.sendParticles(ParticleTypes.END_ROD, dest.x, targetY + 1.0, dest.z, 24, 0.35, 0.65, 0.35, 0.06);
            sl.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.1F, 1.25F);

            // Оставляем 5-секундный пульсирующий маяк резонанса в точке прибытия
            CrystalCombatHandler.spawnPrismBeacon(sl, sp.getUUID(), new Vec3(dest.x, targetY + 0.9, dest.z));
            sp.getCooldowns().addCooldown(this, 280); // сбалансированный КД 14 секунд
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.riftworld.astral_prism.desc1").withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatable("item.riftworld.astral_prism.desc2").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.riftworld.astral_prism.desc3").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ru.aiforge.riftworld.client.ClientHooks.geoItemExt());
    }
}
