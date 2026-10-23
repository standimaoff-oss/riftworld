package ru.aiforge.riftworld.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.flight.Flight;

/**
 * Рев 8: Элисар — планета вечного дня. Небо тут всегда светлое (по нему и плывут острова),
 * поэтому время суток в измерении мягко держится у полудня: ночь не превращает море в чёрную дыру,
 * а рифы и пещеры читаются своим светом.
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID)
public final class ElisarDaylight {
    private static final long NOON = 6000L;

    private ElisarDaylight() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.level.isClientSide()) return;
        if (!(e.level instanceof ServerLevel sl) || sl.dimension() != Flight.ELISAR_DIM) return;
        if (sl.getGameTime() % 400 != 0) return;
        long day = sl.getDayTime() % 24000L;
        if (day < 1000 || day > 11000) {                        // ночь / сумерки -> полдень
            sl.setDayTime(sl.getDayTime() - day + NOON);
        }
    }
}
