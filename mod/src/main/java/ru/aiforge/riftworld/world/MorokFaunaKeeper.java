package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.entity.HollowwatcherEntity;
import ru.aiforge.riftworld.entity.MorokEntity;
import ru.aiforge.riftworld.flight.Flight;
import ru.aiforge.riftworld.registry.ModEntities;

/**
 * v0.36.0 — расселение новых мобов Элисара.
 *
 * <ul>
 *   <li><b>Морок</b> (v0.39.0) — главный хоррор «Мороковой Пади»: <b>днём 5 %, ночью 45 %</b>,
 *       не больше 3 на игрока. Появляется в тумане на 12–40 блоках, а не под носом.</li>
 *   <li><b>Вендиго</b> — теперь редкий гость чащи (1 на игрока): его место занял Морок.</li>
 *   <li><b>Пегас</b> — только в живых биомах (Аметистовые холмы, Висячие сады, Роща Сизо),
 *       редко и по одному: скакун, а не «моб на каждом шагу».</li>
 * </ul>
 * Никаких надписей в чате — только спавн и звуки.
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID)
public final class MorokFaunaKeeper {
    // v0.39.0: Морок — главный хоррор Пади. Ночью часто, днём редко.
    private static final int MOROK_PER_PLAYER = 3;
    private static final int MOROK_WORLD_CAP = 14;
    // v0.49.0: Смотритель Пращи — РЕДКИЙ, но самый страшный. Днём почти не появляется
    // (2 %), ночью — 18 %, и не больше одного на игрока: одного такого хватает.
    private static final int WATCHER_PER_PLAYER = 1;
    private static final int WATCHER_WORLD_CAP = 5;
    private static int failsafe = 0;

    private MorokFaunaKeeper() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.level.isClientSide()) return;
        if (!(e.level instanceof ServerLevel sl) || sl.dimension() != Flight.ELISAR_DIM) return;
        if (sl.getGameTime() % 60 != 0) return;           // раз в 3 секунды
        try {
            tick(sl);
        } catch (Throwable t) {
            if (++failsafe <= 6) System.out.println("[riftworld] morok-fauna tick skipped: " + t);
        }
    }

    private static void tick(ServerLevel sl) {
        List<ServerPlayer> players = sl.players();
        if (players.isEmpty()) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        boolean night = !sl.isDay();
        for (ServerPlayer sp : new ArrayList<>(players)) {
            if (sp.isSpectator()) continue;

            // ---------------- Морок: свой хоррор Пади (днём редко, ночью часто)
            if (MorokHollow.inside(sp.getX(), sp.getZ())) {
                List<MorokEntity> nearMorok = sl.getEntitiesOfClass(MorokEntity.class, sp.getBoundingBox().inflate(96.0));
                // день 0.05, ночь 0.45 — ровно то, что просили: «редко днём, часто ночью»
                double morokChance = night ? 0.45 : 0.05;
                if (nearMorok.size() < MOROK_PER_PLAYER && r.nextDouble() < morokChance
                        && countMorok(sl) < MOROK_WORLD_CAP) {
                    spawnMorok(sl, sp);
                }

                // ---------------- Смотритель Пращи: страшный «стоитель», ждущий, пока отвернёшься
                List<HollowwatcherEntity> nearWatchers =
                        sl.getEntitiesOfClass(HollowwatcherEntity.class, sp.getBoundingBox().inflate(120.0));
                double watcherChance = night ? 0.18 : 0.02;
                if (nearWatchers.size() < WATCHER_PER_PLAYER && r.nextDouble() < watcherChance
                        && countWatchers(sl) < WATCHER_WORLD_CAP) {
                    spawnWatcher(sl, sp);
                }
            }


        }
    }

    private static boolean liveBiomeAt(double x, double z) {
        return VioletHills.inside(x, z) || SizoGrove.inside(x, z) || WaterfallGardens.inside(x, z);
    }

    private static int countWatchers(ServerLevel sl) {
        return sl.getEntitiesOfClass(HollowwatcherEntity.class,
                new net.minecraft.world.phys.AABB(-30000, -64, -30000, 30000, 320, 30000)).size();
    }

    private static int countMorok(ServerLevel sl) {
        return sl.getEntitiesOfClass(MorokEntity.class, new net.minecraft.world.phys.AABB(-30000, -64, -30000, 30000, 320, 30000)).size();
    }



    private static void spawnMorok(ServerLevel sl, ServerPlayer sp) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 14; ++attempt) {
            // спавн не вплотную: Морок появляется в тумане на 12–40 блоках и уже сам выходит на охоту
            int px = (int) sp.getX() + r.nextInt(64) - 32;
            int pz = (int) sp.getZ() + r.nextInt(64) - 32;
            if (!sl.hasChunk(px >> 4, pz >> 4)) continue;
            if (!MorokHollow.inside(px + 0.5, pz + 0.5)) continue;
            double d = Math.hypot(px + 0.5 - sp.getX(), pz + 0.5 - sp.getZ());
            if (d < 12.0) continue;
            BlockPos ground = groundAt(sl, px, pz);
            if (ground == null) continue;
            MorokEntity m = ModEntities.MOROK.get().create(sl);
            if (m == null) return;
            m.moveTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, r.nextFloat() * 360.0F, 0.0F);
            m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(ground), MobSpawnType.NATURAL, null, null);
            sl.addFreshEntity(m);
            if (r.nextDouble() < 0.5) {
                sl.playSound(null, ground, net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT,
                        net.minecraft.sounds.SoundSource.HOSTILE, 1.2F, 0.7F);
            }
            return;
        }
    }



    /**
     * v0.49.0: Смотритель появляется ВЫШЕ — между стволами Гнилой рощи, а не на земле.
     * Он стоит в полусотне блоков от игрока и ждёт, пока на него посмотрят.
     */
    private static void spawnWatcher(ServerLevel sl, ServerPlayer sp) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 14; ++attempt) {
            int px = (int) sp.getX() + r.nextInt(80) - 40;
            int pz = (int) sp.getZ() + r.nextInt(80) - 40;
            if (!sl.hasChunk(px >> 4, pz >> 4)) continue;
            if (!MorokHollow.inside(px + 0.5, pz + 0.5)) continue;
            double d = Math.hypot(px + 0.5 - sp.getX(), pz + 0.5 - sp.getZ());
            if (d < 18.0) continue;
            BlockPos ground = groundAt(sl, px, pz);
            if (ground == null) continue;
            HollowwatcherEntity w = ModEntities.HOLLOWWATCHER.get().create(sl);
            if (w == null) return;
            w.moveTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, r.nextFloat() * 360.0F, 0.0F);
            w.finalizeSpawn(sl, sl.getCurrentDifficultyAt(ground), MobSpawnType.NATURAL, null, null);
            sl.addFreshEntity(w);
            // Тишина — часть страха: слышно только сердцебиение вдалеке.
            sl.playSound(null, ground, net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT,
                    net.minecraft.sounds.SoundSource.HOSTILE, 0.7F, 0.5F);
            return;
        }
    }


    /** Ищет твёрдую землю сверху вниз (первый непроходимый блок, над которым воздух). */
    private static BlockPos groundAt(ServerLevel sl, int x, int z) {
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos(x, 0, z);
        for (int y = 190; y > 40; --y) {
            mp.set(x, y, z);
            var st = sl.getBlockState(mp);
            if (st.isAir()) continue;
            if (!st.getFluidState().isEmpty()) continue;
            if (!st.getCollisionShape(sl, mp).isEmpty()) {
                return new BlockPos(x, y + 1, z);
            }
        }
        return null;
    }
}
