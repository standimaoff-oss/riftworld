package ru.aiforge.riftworld.world;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.flight.Flight;
import ru.aiforge.riftworld.registry.ModSounds;

/**
 * v0.55.0 — звуки Элисара в игре.
 *
 * <p>Звуков всего четыре, и они привязаны к понятным моментам:</p>
 * <ul>
 *   <li>переплавка в мире Элисара закончилась — гудит {@link ModSounds#SMELTING};</li>
 *   <li>сделка с торговцем — звонит {@link ModSounds#COIN} (это делает сам
 *       житель через {@code getNotifyTradeSound});</li>
 *   <li>появление рынка — бьёт {@link ModSounds#MARKET_BELL}.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID)
public final class ElisarSoundEvents {

    private ElisarSoundEvents() {
    }

    /**
     * Игрок забрал что-то из печи — переплавка закончилась.
     *
     * <p>Гудим только в Элисаре: в обычном мире у печей свой звук, и наш
     * горн там был бы лишним.</p>
     */
    @SubscribeEvent
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (!level.dimension().equals(Flight.ELISAR_DIM)) return;
        BlockPos pos = player.blockPosition();
        play(level, pos, ModSounds.SMELTING.get(), 0.5F, 0.9F);
    }

    /** Короткий звук в указанной точке. */
    public static void play(Level level, BlockPos pos, SoundEvent sound,
                            float volume, float pitch) {
        level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                sound, SoundSource.BLOCKS, volume, pitch);
    }
}
