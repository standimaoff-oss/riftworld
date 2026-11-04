package ru.aiforge.riftworld.item;

import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import ru.aiforge.riftworld.entity.CrystalShieldEntity;
import ru.aiforge.riftworld.registry.ModEntities;

/**
 * «Бастион Граней» ({@code crystal_aegis}) — одноразовый стакающийся кристальный щит:
 * <ul>
 *   <li><b>ЛКМ:</b> спавнит перед игроком таранный щит, который несётся вперёд, нанося урон
 *       и мощно отбрасывая всё живое на пути (тратит 1 шт.).</li>
 *   <li><b>ПКМ:</b> спавнит перед игроком защитный барьер на максимум 6 секунд (120 тиков),
 *       поглощающий удары и перехватывающий снаряды (тратит 1 шт.).</li>
 * </ul>
 */
public class CrystalAegisItem extends Item implements CrystalGeoItem {
    public CrystalAegisItem(Properties props) {
        super(props);
    }

    @Override
    public String geoModel() {
        return "crystal_shield";
    }

    @Override
    public String geoTexture() {
        return "crystal_shield";
    }

    /** Таранный щит по ЛКМ (вызывается пакетом {@code CrystalActionMsg}). */
    public void launchRamLeftClick(ServerPlayer player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(this) || stack.isEmpty()) return;
        ServerLevel sl = player.serverLevel();
        CrystalShieldEntity shield = ModEntities.CRYSTAL_SHIELD.get().create(sl);
        if (shield == null) return;
        shield.initRam(player);
        sl.addFreshEntity(shield);
        sl.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.35F);
        sl.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.1F, 1.1F);

        player.getCooldowns().addCooldown(this, 18);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide && level instanceof ServerLevel sl) {
            // Заменяем предыдущий защитный щит этого игрока, если он ещё стоит
            for (CrystalShieldEntity old : sl.getEntitiesOfClass(CrystalShieldEntity.class,
                    player.getBoundingBox().inflate(8.0),
                    s -> s.mode() == CrystalShieldEntity.MODE_GUARD && player.getUUID().equals(s.ownerUuid()))) {
                old.discard();
            }
            CrystalShieldEntity shield = ModEntities.CRYSTAL_SHIELD.get().create(sl);
            if (shield != null) {
                shield.initGuard(player);
                sl.addFreshEntity(shield);
                sl.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 0.9F, 1.4F);
                sl.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, 1.0F);
            }
            player.getCooldowns().addCooldown(this, 24);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.riftworld.crystal_aegis.desc1").withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatable("item.riftworld.crystal_aegis.desc2").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.riftworld.crystal_aegis.desc3").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ru.aiforge.riftworld.client.ClientHooks.geoItemExt());
    }
}
