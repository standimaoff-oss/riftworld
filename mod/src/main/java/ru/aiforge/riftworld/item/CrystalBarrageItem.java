package ru.aiforge.riftworld.item;

import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import ru.aiforge.riftworld.entity.CrystalCombatHandler;
import ru.aiforge.riftworld.entity.CrystalShardEntity;
import ru.aiforge.riftworld.registry.ModEntities;

/**
 * «Веер Осколков» ({@code crystal_barrage}) — одноразовый стакающийся кристальный заряд:
 * <ul>
 *   <li><b>ЛКМ:</b> мгновенно выпускает 1 стремительный кристалл по прицелу (тратит 1 шт.).</li>
 *   <li><b>Удержание ПКМ:</b> выстраивает вокруг игрока прямо в поле его зрения до 5 парящих
 *       кристаллов (по 1 каждые 8 тиков, максимум 5).</li>
 *   <li><b>Отпускание ПКМ:</b> построенные кристаллы по очереди один за другим срываются
 *       строго туда, куда в момент выстрела смотрит прицел игрока (тратит 1 шт.).</li>
 * </ul>
 */
public class CrystalBarrageItem extends Item implements CrystalGeoItem {
    public static final int TICKS_PER_CRYSTAL = 8;
    public static final int MAX_CRYSTALS = 5;

    /** 5 точек полудуги в локальных координатах камеры {right, up, forward} — всегда в поле зрения игрока! */
    public static final double[][] FOV_OFFSETS = {
            {-0.52, -0.04, 1.15},
            {-0.30,  0.24, 1.18},
            { 0.00,  0.34, 1.20},
            { 0.30,  0.24, 1.18},
            { 0.52, -0.04, 1.15}
    };

    public CrystalBarrageItem(Properties props) {
        super(props);
    }

    @Override
    public String geoModel() {
        return "crystal_barrage_item";
    }

    public static int builtCrystals(int usedTicks) {
        if (usedTicks < 4) return 0;
        if (usedTicks < TICKS_PER_CRYSTAL) return 1;
        return Mth.clamp(usedTicks / TICKS_PER_CRYSTAL, 1, MAX_CRYSTALS);
    }

    /** Мгновенный выстрел 1 кристаллом по ЛКМ (вызывается пакетом {@code CrystalActionMsg}). */
    public void fireSingleLeftClick(ServerPlayer player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(this) || stack.isEmpty()) return;
        ServerLevel sl = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        CrystalShardEntity shard = new CrystalShardEntity(ModEntities.CRYSTAL_SHARD.get(), sl, player, 7.5F);
        shard.setPos(eye.x + look.x * 0.75, eye.y - 0.1 + look.y * 0.75, eye.z + look.z * 0.75);
        shard.setDeltaMovement(look.scale(1.65));
        sl.addFreshEntity(shard);

        sl.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.35F);
        sl.sendParticles(ParticleTypes.END_ROD, shard.getX(), shard.getY(), shard.getZ(), 5, 0.08, 0.08, 0.08, 0.03);
        player.getCooldowns().addCooldown(this, 10);
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
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingTicks) {
        int used = getUseDuration(stack) - remainingTicks;
        if (used > 0 && used <= MAX_CRYSTALS * TICKS_PER_CRYSTAL && used % TICKS_PER_CRYSTAL == 0) {
            int idx = used / TICKS_PER_CRYSTAL;
            float pitch = 0.9F + idx * 0.14F;
            level.playSound(null, living.getX(), living.getY(), living.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, pitch);
            if (!level.isClientSide && level instanceof ServerLevel sl) {
                Vec3 pos = slotWorldPos(living, idx - 1);
                sl.sendParticles(ParticleTypes.GLOW, pos.x, pos.y, pos.z, 5, 0.06, 0.06, 0.06, 0.01);
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!(living instanceof ServerPlayer player)) return;
        int used = getUseDuration(stack) - timeLeft;
        int count = builtCrystals(used);
        if (count <= 0) return;

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.getCooldowns().addCooldown(this, 8 + count * 4);
        CrystalCombatHandler.enqueueBarrage(player, count);
    }

    public static Vec3 slotWorldPos(LivingEntity viewer, int slotIndex) {
        int idx = Mth.clamp(slotIndex, 0, FOV_OFFSETS.length - 1);
        double[] off = FOV_OFFSETS[idx];
        Vec3 fwd = viewer.getLookAngle().normalize();
        Vec3 worldUp = new Vec3(0, 1, 0);
        Vec3 right = fwd.cross(worldUp);
        if (right.lengthSqr() < 1.0E-4) {
            right = Vec3.directionFromRotation(0.0F, viewer.getYRot() + 90.0F);
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(fwd).normalize();
        Vec3 eye = viewer.getEyePosition();
        return eye.add(right.scale(off[0])).add(up.scale(off[1])).add(fwd.scale(off[2]));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.riftworld.crystal_barrage.desc1").withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatable("item.riftworld.crystal_barrage.desc2").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.riftworld.crystal_barrage.desc3").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ru.aiforge.riftworld.client.ClientHooks.geoItemExt());
    }
}
