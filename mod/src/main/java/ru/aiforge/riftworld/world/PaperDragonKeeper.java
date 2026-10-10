package ru.aiforge.riftworld.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.entity.PaperDragonEntity;
import ru.aiforge.riftworld.flight.Flight;

/**
 * Рев 5: бумажное море больше не биом - остаётся ОДИН дракон на всё измерение. Каждые ~5 с смотрим,
 * жив ли он в Элизаре; если нет (мир только запустился) - из пушинки тумана над водой у ближайшего
 * к игроку острова разворачивается новый и принимается кружить.
 * <p>Рев 6 (срочный): весь тик в failsafe-обёртке + снимки коллекций - реальный лог игрока показал
 * ArrayIndexOutOfBounds внутри fastutil-итераторов на тасании мира во время сохранения.
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID)
public final class PaperDragonKeeper {
    private PaperDragonKeeper() {
    }

    private static int failsafe = 0;

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.level.isClientSide()) return;
        if (!(e.level instanceof ServerLevel sl) || sl.dimension() != Flight.ELISAR_DIM) return;
        if (sl.getGameTime() % 100 != 0) return;
        try {
            tick(sl);
        } catch (Throwable t) {                                                     // тасания списков в момент сохранения мира
            if (++failsafe <= 8) System.out.println("[riftworld] keeper tick skipped: " + t);
        }
    }

    private static void tick(ServerLevel sl) {
        int have = 0;
        java.util.ArrayList<net.minecraft.world.entity.Entity> ents = new java.util.ArrayList<>();
        for (var ent : sl.getEntities().getAll()) ents.add(ent);                    // снимок - итераторы fastutil живут нестабильно
        for (var ent : ents) {
            if (ent instanceof PaperDragonEntity) {
                have++;
                if (have > 1) ent.discard();                                        // дубликатам не жить
            }
        }
        if (have > 0) return;
        java.util.List<ServerPlayer> ps = new java.util.ArrayList<>(sl.players());  // снимок списка игроков
        if (ps.isEmpty()) return;
        ServerPlayer sp = ps.get(0);
        // точка вылупления: край ближайшего острова, над поверхностью на 10..14
        CityPlan.Island isl = null;
        double bd = 1e9;
        for (CityPlan.Island i : CityPlan.get().islands) {
            if (i.kind == 4) continue;
            double d = Math.hypot(sp.getX() - i.cx, sp.getZ() - i.cz);
            if (d < bd) { bd = d; isl = i; }
        }
        if (isl == null) return;
        double a = Math.atan2(sp.getZ() - isl.cz, sp.getX() - isl.cx);
        int x = (int) Math.floor(isl.cx + Math.cos(a) * (isl.R + 26));
        int z = (int) Math.floor(isl.cz + Math.sin(a) * (isl.R + 26));
        int surf = sl.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        PaperDragonEntity.spawn(sl, new net.minecraft.core.BlockPos(x, surf + 12, z));
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.WHITE_ASH, x + 0.5, surf + 12.5, z + 0.5, 40, 2.6, 1.4, 2.6, 0.02); // бумажная пушинка в тумане
    }
}
