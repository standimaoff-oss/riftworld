package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.entity.PaperDragonEntity;
import ru.aiforge.riftworld.flight.Flight;

/**
 * v0.20.0: Поддерживает разреженную популяцию (2–3 особи в радиусе видимости) безвредных
 * модульных оригами-драконов, парящих вдоль и под летающими островами Элисара.
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID)
public final class PaperDragonKeeper {
    private static final int MAX_NEAR_PLAYER = 2;
    private static final double MIN_SPACING = 88.0;
    private static final double KEEP_RANGE = 270.0;

    private PaperDragonKeeper() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!(e.level instanceof ServerLevel sl) || sl.dimension() != Flight.ELISAR_DIM) return;
        if (sl.getGameTime() % 60 != 20) return;

        List<ServerPlayer> players = sl.players();
        List<PaperDragonEntity> live = new ArrayList<>();
        for (Entity ent : sl.getAllEntities()) {
            if (ent instanceof PaperDragonEntity d && d.isAlive()) {
                live.add(d);
            }
        }

        if (players.isEmpty()) return;

        // Мягко убираем драконов, ушедших слишком далеко от всех игроков (> 270 блоков)
        for (int i = live.size() - 1; i >= 0; --i) {
            PaperDragonEntity d = live.get(i);
            boolean nearAny = false;
            for (ServerPlayer sp : players) {
                if (sp.distanceToSqr(d) <= KEEP_RANGE * KEEP_RANGE) {
                    nearAny = true;
                    break;
                }
            }
            if (!nearAny && live.size() > MAX_NEAR_PLAYER) {
                d.discard();
                live.remove(i);
            }
        }

        for (ServerPlayer sp : players) {
            if (sp.isSpectator()) continue;
            int around = 0;
            for (PaperDragonEntity d : live) {
                if (sp.distanceToSqr(d) <= 235.0 * 235.0) {
                    ++around;
                }
            }
            if (around >= MAX_NEAR_PLAYER) continue;

            List<CityPlan.Island> islands = FloatingIslands.gather(sp.getX(), sp.getZ(), 210.0, true);
            if (islands.isEmpty()) continue;

            CityPlan.Island pick = null;
            double bestDist = Double.MAX_VALUE;
            for (CityPlan.Island cand : islands) {
                double distPlayer = Math.hypot(cand.cx - sp.getX(), cand.cz - sp.getZ());
                if (distPlayer > 210.0) continue;
                boolean occupied = false;
                for (PaperDragonEntity d : live) {
                    if (Math.hypot(d.getX() - cand.cx, d.getZ() - cand.cz) < Math.max(MIN_SPACING, cand.R * 0.9)) {
                        occupied = true;
                        break;
                    }
                }
                if (occupied) continue;
                if (distPlayer < bestDist) {
                    bestDist = distPlayer;
                    pick = cand;
                }
            }
            if (pick == null) continue;

            double ang = Math.atan2(sp.getZ() - pick.cz, sp.getX() - pick.cx) + (sl.random.nextDouble() - 0.5) * 0.9;
            double checkX = pick.cx + Math.cos(ang) * pick.R * 0.8;
            double checkZ = pick.cz + Math.sin(ang) * pick.R * 0.8;
            if (!sl.isLoaded(BlockPos.containing(checkX, CityPlan.get().ocean + 8, checkZ))) continue;

            PaperDragonEntity spawned = PaperDragonEntity.spawnForIsland(sl, pick, ang);
            live.add(spawned);
        }
    }
}
