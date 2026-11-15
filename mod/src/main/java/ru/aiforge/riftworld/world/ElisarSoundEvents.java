package ru.aiforge.riftworld.world;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.event.level.BlockEvent;
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
 *   <li>печь в мире Элисара, доведённая до готовности, — гудит
 *       {@link ModSounds#SMELTING};</li>
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
     * Переплавка в Элисаре.
     *
     * <p>Ловим момент, когда печь переходит из состояния «горит» в
     * «не горит»: в этот момент в выходном слоте появляется готовый
     * результат — значит, пора дать звук горна.</p>
     */
    @SubscribeEvent
    public static void onBlockStateChange(BlockEvent.BlockStateEvent event) {
        Level level = event.getLevel();
        if (level == null || level.isClientSide) return;
        if (!level.dimension().equals(Flight.ELISAR_DIM)) return;
        if (!(event.getBlock() instanceof net.minecraft.world.level.block.FurnaceBlock)) return;

        boolean wasLit = event.getOldState().hasProperty(BlockStateProperties.LIT)
                && event.getOldState().getValue(BlockStateProperties.LIT);
        boolean nowLit = event.getNewState().hasProperty(BlockStateProperties.LIT)
                && event.getNewState().getValue(BlockStateProperties.LIT);
        if (!wasLit || nowLit) return;

        BlockEntity be = level.getBlockEntity(event.getPos());
        if (!(be instanceof FurnaceBlockEntity)) return;
        play(level, event.getPos(), ModSounds.SMELTING.get(), 0.5F, 0.9F);
    }

    /** Короткий звук в указанной точке. */
    public static void play(Level level, BlockPos pos, SoundEvent sound,
                            float volume, float pitch) {
        level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                sound, SoundSource.BLOCKS, volume, pitch);
    }
}
