package ru.aiforge.riftworld.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.entity.PaperDragonEntity;
import ru.aiforge.riftworld.registry.ModEntities;

/**
 * Поддерживает естественное появление супер-детализированных модульных оригами-драконов в Элисаре:
 * часть драконов начинает полёт ПОД сводами парящих островов ({@link PaperDragonEntity#FLIGHT_UNDER_ISLAND}),
 * а часть — высоко НАД кронами и башнями островов ({@link PaperDragonEntity#FLIGHT_OVER_ISLAND}),
 * после чего каждый дракон плавно циклирует между полётом над и под островами!
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID)
public final class PaperDragonKeeper {
    private static final int CAP_NEAR_PLAYER = 3;
    private static final int SCAN_RADIUS = 300;
    private static int tick;

    private PaperDragonKeeper() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.level.isClientSide) return;
        if (e.level.dimension() != RiftWorld.RIFT_DIM) return;
        if ((++tick % 140) != 0) return;

        ServerLevel sl = (ServerLevel) e.level;
        CityPlan plan = CityPlan.get();
        RandomSource rnd = sl.random;

        for (ServerPlayer sp : sl.players()) {
            if (sp.isSpectator()) continue;

            AABB nearBox = new AABB(sp.blockPosition()).inflate(SCAN_RADIUS, 160, SCAN_RADIUS);
            int nearby = sl.getEntitiesOfClass(PaperDragonEntity.class, nearBox).size();
            if (nearby >= CAP_NEAR_PLAYER) continue;

            if (nearby >= 1 && rnd.nextFloat() > 0.45F) continue;

            // Чередуем начальный режим: чётный дракон появляется ПОД островом, нечётный — НАД островом!
            boolean spawnOver = (nearby & 1) == 1;

            for (int attempt = 0; attempt < 16; ++attempt) {
                double ang = rnd.nextDouble() * Math.PI * 2.0;
                double dist = 70.0 + rnd.nextDouble() * 115.0;
                int sx = Mth.floor(sp.getX() + Math.cos(ang) * dist);
                int sz = Mth.floor(sp.getZ() + Math.sin(ang) * dist);

                if (AshenLands.blocks(sx, sz, 40) || EmberRuins.siteAt(sx, sz) != null) continue;
                if (!sl.isLoaded(new BlockPos(sx, plan.ocean + 20, sz))) continue;

                ElisarChunkGenerator.Col col = ElisarChunkGenerator.column(plan, sx, sz);
                if (col.isl == null || col.isl.base < plan.ocean + 20) continue;

                int sy;
                int initMode;
                if (spawnOver) {
                    int surfY = (col.top > plan.ocean) ? col.top : col.isl.base + 8;
                    sy = surfY + 18 + rnd.nextInt(12);
                    initMode = PaperDragonEntity.FLIGHT_OVER_ISLAND;
                } else {
                    if (col.bottom < plan.ocean + 16) continue;
                    sy = Math.max(plan.ocean + 10, col.bottom - (8 + rnd.nextInt(7)));
                    initMode = PaperDragonEntity.FLIGHT_UNDER_ISLAND;
                }

                BlockPos pos = new BlockPos(sx, sy, sz);
                if (!sl.getBlockState(pos).isAir() || !sl.getBlockState(pos.above(2)).isAir()) continue;

                PaperDragonEntity dragon = ModEntities.PAPER_DRAGON.get().create(sl);
                if (dragon == null) break;
                dragon.moveTo(sx + 0.5, sy + 0.5, sz + 0.5, rnd.nextFloat() * 360.0F, 0.0F);
                dragon.setHomeAnchor(new Vec3(col.isl.cx, col.isl.base, col.isl.cz), initMode);
                sl.addFreshEntity(dragon);
                break;
            }
        }
    }
}
