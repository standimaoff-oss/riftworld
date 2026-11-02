package ru.aiforge.riftworld.item;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.entity.PaperDragonEntity;
import ru.aiforge.riftworld.registry.ModEntities;
import ru.aiforge.riftworld.world.TamedDragonSavedData;

/**
 * Горн Бумажного Дракона ({@code dragon_whistle}):
 * <ul>
 *   <li>Выдаётся при приручении Бумажного Дракона на вершине Перевёрнутой башни;</li>
 *   <li>Издаёт глубокий небесный зов рога и призывает приручённого дракона спуститься к игроку прямо в небе;</li>
 *   <li>Работает ДАЖЕ ВНЕ ЗОНЫ СИМУЛЯЦИИ (из выгруженных чанков за тысячи блоков!).</li>
 * </ul>
 */
public class DragonWhistleItem extends Item {
    public static final String TAG_DRAGON_UUID = "DragonUUID";
    private static final int COOLDOWN_TICKS = 100;

    public DragonWhistleItem(Properties props) {
        super(props);
    }

    public static void bindToDragon(ItemStack stack, PaperDragonEntity dragon) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(TAG_DRAGON_UUID, dragon.getUUID());
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.TOOT_HORN;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 40;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 2.6F, 1.18F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.6F, 0.85F);

        if (!level.isClientSide && level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
            summonDragonFromSky(sl, sp, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static boolean summonDragonFromSky(ServerLevel sl, ServerPlayer player, ItemStack whistleStack) {
        CompoundTag tag = whistleStack.getTag();
        UUID prefId = (tag != null && tag.hasUUID(TAG_DRAGON_UUID)) ? tag.getUUID(TAG_DRAGON_UUID) : null;

        TamedDragonSavedData saved = TamedDragonSavedData.get(sl.getServer());
        TamedDragonSavedData.Entry entry = saved.findForOwner(player.getUUID(), prefId);

        // 1) Проверяем, загружен ли уже дракон в текущем измерении
        PaperDragonEntity loaded = null;
        if (entry != null) {
            Entity ent = sl.getEntity(entry.dragonId);
            if (ent instanceof PaperDragonEntity pd && pd.isAlive() && pd.isOwnedBy(player)) {
                loaded = pd;
            }
        }
        if (loaded == null) {
            // Дополнительный поиск среди активных сущностей измерения (если запись ещё не успела сохраниться)
            for (Entity ent : sl.getAllEntities()) {
                if (ent instanceof PaperDragonEntity pd && pd.isAlive() && pd.isOwnedBy(player)) {
                    if (prefId == null || pd.getUUID().equals(prefId)) {
                        loaded = pd;
                        break;
                    }
                }
            }
        }

        // Вычисляем эффектную точку появления высоко в небе в поле зрения игрока (~48 м по взгляду и +22 м вверх —
        // гарантированно внутри ENTITY_TICKING чанков симуляции и перед стеной биомного тумана!),
        // чтобы дракон МГНОВЕННО ОТРИСОВАЛСЯ В НЕБЕ и плавно спустился прямо к хозяину!
        Vec3 lookHoriz = new Vec3(-Mth.sin(player.getYRot() * Mth.DEG_TO_RAD), 0.0, Mth.cos(player.getYRot() * Mth.DEG_TO_RAD));
        if (lookHoriz.lengthSqr() < 1.0E-4) lookHoriz = new Vec3(0.0, 0.0, 1.0);
        else lookHoriz = lookHoriz.normalize();

        double skyDist = 34.0;
        double skyX = player.getX() + lookHoriz.x * skyDist;
        double skyZ = player.getZ() + lookHoriz.z * skyDist;
        double skyY = Mth.clamp(player.getY() + 18.0, sl.getMinBuildHeight() + 48.0, sl.getMaxBuildHeight() - 36.0);
        float towardsPlayerYaw = (float) (Mth.atan2(player.getZ() - skyZ, player.getX() - skyX) * (180.0 / Math.PI)) - 90.0F;
        sl.getChunk(Mth.floor(skyX) >> 4, Mth.floor(skyZ) >> 4);

        if (loaded != null && entry != null && loaded.distanceToSqr(player) > 72.0 * 72.0) {
            // Если старый экземпляр далеко в пограничном/полувыгруженном чанке — переспавним свежего из сохранённых данных!
            loaded.stopRiding();
            loaded.ejectPassengers();
            loaded.discard();
            loaded = null;
        }

        if (loaded != null) {
            if (loaded.distanceToSqr(player) > 72.0 * 72.0) {
                loaded.stopRiding();
                loaded.ejectPassengers();
                loaded.moveTo(skyX, skyY, skyZ, towardsPlayerYaw, 14.0F);
            }
            loaded.beginHornSummonToOwner(player);
            bindToDragon(whistleStack, loaded);
            sl.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.8, player.getZ(), 18, 0.5, 0.5, 0.5, 0.06);
            player.displayClientMessage(Component.translatable("msg.riftworld.dragon_horn_called")
                    .withStyle(ChatFormatting.GOLD), true);
            return true;
        }

        // 2) Дракон находится ЗА ПРЕДЕЛАМИ СИМУЛЯЦИИ (в выгруженном чанке далеко в мире или другом измерении)!
        if (entry == null) {
            player.displayClientMessage(Component.translatable("msg.riftworld.dragon_horn_none")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }

        // Если дракон загружен в ДРУГОМ измерении — аккуратно выгрузим старый экземпляр оттуда
        for (ServerLevel otherLevel : sl.getServer().getAllLevels()) {
            if (otherLevel == sl) continue;
            Entity otherEnt = otherLevel.getEntity(entry.dragonId);
            if (otherEnt instanceof PaperDragonEntity pd) {
                pd.ejectPassengers();
                pd.discard();
            }
        }

        PaperDragonEntity recalled = ModEntities.PAPER_DRAGON.get().create(sl);
        if (recalled == null) return false;

        UUID newId = UUID.randomUUID();
        recalled.setUUID(newId);
        TamedDragonSavedData.Entry updated = saved.bumpRecallAndRebind(entry, newId);

        recalled.moveTo(skyX, skyY, skyZ, towardsPlayerYaw, 14.0F);
        recalled.restoreFromSavedEntry(updated);
        recalled.beginHornSummonToOwner(player);
        sl.addFreshEntity(recalled);

        bindToDragon(whistleStack, recalled);
        sl.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.8, player.getZ(), 24, 0.6, 0.6, 0.6, 0.08);
        player.displayClientMessage(Component.translatable("msg.riftworld.dragon_horn_beyond_sim")
                .withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.riftworld.dragon_whistle.desc1").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.riftworld.dragon_whistle.desc2").withStyle(ChatFormatting.GOLD));
    }
}
