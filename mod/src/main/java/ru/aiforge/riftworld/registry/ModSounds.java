package ru.aiforge.riftworld.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.RiftWorld;

/**
 * v0.55.0 — звуки Элисара.
 *
 * <p>Звуков немного, всего четыре, но каждое место теперь звучит по-своему:</p>
 * <ul>
 *   <li>{@link #SMELTING} — гудение горна, когда плавится руда;</li>
 *   <li>{@link #COIN} — звон монет в момент сделки;</li>
 *   <li>{@link #MARKET_BELL} — колокол рынка, когда ряд открывается;</li>
 *   <li>{@link #HAMMER} — удар молота кузнеца.</li>
 * </ul>
 */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RiftWorld.MODID);

    /** Гудение горна и переплавки. */
    public static final RegistryObject<SoundEvent> SMELTING =
            SOUNDS.register("elisar_smelting",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(RiftWorld.MODID, "elisar_smelting")));

    /** Звон монет при сделке с торговцем. */
    public static final RegistryObject<SoundEvent> COIN =
            SOUNDS.register("elisar_coin",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(RiftWorld.MODID, "elisar_coin")));

    /** Колокол рынка. */
    public static final RegistryObject<SoundEvent> MARKET_BELL =
            SOUNDS.register("elisar_market_bell",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(RiftWorld.MODID, "elisar_market_bell")));

    /** Удар молота кузнеца. */
    public static final RegistryObject<SoundEvent> HAMMER =
            SOUNDS.register("elisar_hammer",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(RiftWorld.MODID, "elisar_hammer")));

    private ModSounds() {
    }
}
