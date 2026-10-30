package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.entity.SeaFishEntity;
import ru.aiforge.riftworld.flight.Flight;

/**
 * Рев 8 «Живое море»: смотритель ихтиофауны Элисара.
 *
 * <p>Раз в 1.5 с (и только для загруженных чанков) держит население вокруг игроков:
 * <ul>
 *   <li><b>море</b> — косяки серебристых, золотые диски, полосатые; редко медузы и манты;</li>
 *   <li><b>затопленные пещеры</b> — удильщики и угри, только глубоко под дном и только в воде;</li>
 *   <li>общий потолок на измерение — 320 рыб, чтобы мир не «кишел» и не лагал.</li>
 * </ul>
 * Никаких ванильных спавнеров и фич: население детерминированно считается этим кодом, как и весь Элисар.
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID)
public final class SeaLifeKeeper {
    private static final int PEACEFUL_TARGET = 14;
    private static final int JELLY_TARGET = 2;
    private static final int RAY_TARGET = 1;
    private static final int TURTLE_TARGET = 1;
    /** Рев 10: хищники пещер — РЕДКИЕ. Не больше двух рядом и только в затопленных полостях. */
    private static final int HOSTILE_TARGET = 2;
    private static final int WORLD_CAP = 320;
    private static int failsafe = 0;

    private SeaLifeKeeper() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.level.isClientSide()) return;
        if (!(e.level instanceof ServerLevel sl) || sl.dimension() != Flight.ELISAR_DIM) return;
        if (sl.getGameTime() % 30 != 0) return;
        try {
            tick(sl);
        } catch (Throwable t) {
            if (++failsafe <= 6) System.out.println("[riftworld] sea-life tick skipped: " + t);
        }
    }

    private static void tick(ServerLevel sl) {
        List<net.minecraft.world.entity.Entity> ents = new ArrayList<>();
        for (var ent : sl.getEntities().getAll()) ents.add(ent);
        int total = 0;
        for (var ent : ents) if (ent instanceof SeaFishEntity) ++total;
        if (total >= WORLD_CAP) return;

        for (ServerPlayer sp : new ArrayList<>(sl.players())) {
            int peaceful = 0, jelly = 0, ray = 0, turtle = 0, hostile = 0;
            for (var ent : ents) {
                if (!(ent instanceof SeaFishEntity f)) continue;
                if (f.distanceToSqr(sp) > 80 * 80) continue;
                int v = f.variant();
                if (v == SeaFishEntity.JELLY) ++jelly;
                else if (v == SeaFishEntity.RAY) ++ray;
                else if (v == SeaFishEntity.TURTLE) ++turtle;
                else if (f.hostile()) ++hostile;
                else ++peaceful;
            }
            if (peaceful < PEACEFUL_TARGET) school(sl, sp, peaceful);
            if (jelly < JELLY_TARGET) single(sl, sp, SeaFishEntity.JELLY, false);
            if (ray < RAY_TARGET) single(sl, sp, SeaFishEntity.RAY, false);
            if (turtle < TURTLE_TARGET) single(sl, sp, SeaFishEntity.TURTLE, false);
            // «злые и редкие»: только в затопленных пещерах, редко и по одному
            if (hostile < HOSTILE_TARGET && rnd().nextDouble() < 0.55) single(sl, sp, cavePredator(), true);
        }
    }

    private static ThreadLocalRandom rnd() {
        return ThreadLocalRandom.current();
    }

    /** Кто именно выйдет из темноты: удильщик и угорь обычны, глот и жгучая медуза — редкая находка. */
    private static int cavePredator() {
        double r = rnd().nextDouble();
        if (r < 0.34) return SeaFishEntity.ANGLER;
        if (r < 0.64) return SeaFishEntity.EEL;
        if (r < 0.86) return SeaFishEntity.GLOOM;
        return SeaFishEntity.STING;
    }

    /** Косяк мирных рыб: 6..9 штук одной породы, в открытой воде недалеко от игрока. */
    private static void school(ServerLevel sl, ServerPlayer sp, int havePeaceful) {
        int variant = switch (rnd().nextInt(14)) {
            case 0, 1 -> SeaFishEntity.GOLD;
            case 2, 3, 4 -> SeaFishEntity.STRIPED;
            case 5, 6 -> SeaFishEntity.SURGEON;      // тропический хирург — стайками у рифов
            default -> SeaFishEntity.SILVER;
        };
        Vec3 at = findWater(sl, sp, false);
        if (at == null) return;
        int n = Math.min(9, Math.max(4, PEACEFUL_TARGET - havePeaceful));
        for (int i = 0; i < n; i++) {
            Vec3 p = at.add((rnd().nextDouble() - 0.5) * 4.0, (rnd().nextDouble() - 0.5) * 2.0, (rnd().nextDouble() - 0.5) * 4.0);
            if (SeaFishEntity.elisarWater(sl, BlockPos.containing(p))) SeaFishEntity.spawn(sl, p, variant);
        }
    }

    private static void single(ServerLevel sl, ServerPlayer sp, int variant, boolean cave) {
        Vec3 at = findWater(sl, sp, cave);
        if (at != null) SeaFishEntity.spawn(sl, at, variant);
    }

    /** Ищет подходящую воду: в открытом море или (cave=true) в затопленной пещере под дном. */
    private static Vec3 findWater(ServerLevel sl, ServerPlayer sp, boolean cave) {
        int sea = CityPlan.get().ocean;
        for (int attempt = 0; attempt < 26; attempt++) {
            double a = rnd().nextDouble() * Math.PI * 2;
            double d = cave ? 10 + rnd().nextDouble() * 46 : 14 + rnd().nextDouble() * 40;
            int x = (int) Math.floor(sp.getX() + Math.cos(a) * d);
            int z = (int) Math.floor(sp.getZ() + Math.sin(a) * d);
            if (!sl.hasChunk(x >> 4, z >> 4)) continue;
            int floor = ElisarChunkGenerator.oceanFloor(CityPlan.get(), x, z);
            int y;
            if (cave) {
                // глубоко под дном: 6..20 блоков вниз от кромки
                y = floor - 6 - rnd().nextInt(14);
                if (y < 3) continue;
            } else {
                // в толще воды: не у самой поверхности и не у дна
                int lo = Math.min(sea - 8, floor + 4);
                int hi = floor + 4;
                y = lo + rnd().nextInt(Math.max(1, sea - 8 - lo));
                if (y <= hi) continue;
            }
            BlockPos p = new BlockPos(x, y, z);
            if (!SeaFishEntity.elisarWater(sl, p) || !SeaFishEntity.elisarWater(sl, p.above())) continue;
            if (cave) {
                // хищник выходит только в темноте: над полостью не должно быть открытого света
                if (sl.getBrightness(net.minecraft.world.level.LightLayer.SKY, p) > 4) continue;
            }
            if (cave && !(sl.getBlockState(p.below()).isSolidRender(sl, p.below()))) {
                // хотим «с потолком над головой» — проверяем, что над рыбой не сразу открытое море
                boolean roofed = false;
                for (int dy = 3; dy <= 8; dy++) {
                    if (sl.getBlockState(p.above(dy)).isSolidRender(sl, p.above(dy))) { roofed = true; break; }
                }
                if (!roofed) continue;
            }
            if (!cave) {
                // никаких рыб в камне: над точкой должна быть вода до самого верха
                boolean ok = true;
                for (int dy = 1; dy <= 6; dy++) {
                    var st = sl.getBlockState(p.above(dy));
                    if (st.isSolidRender(sl, p.above(dy)) || st.is(Blocks.AIR)) { ok = false; break; }
                }
                if (!ok) continue;
            }
            return new Vec3(x + 0.5, y + 0.4, z + 0.5);
        }
        return null;
    }
}
