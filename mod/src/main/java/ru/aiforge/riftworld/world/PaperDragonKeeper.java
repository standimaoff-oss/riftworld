package ru.aiforge.riftworld.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.entity.PaperDragonEntity;
import ru.aiforge.riftworld.flight.Flight;
import ru.aiforge.riftworld.registry.ModEntities;

/**
 * v0.24.0: Spawns and maintains majestic giant Origami Dragons ONLY outside the White City,
 * gliding along wild floating island cliffs, beneath high floating island vaults, and over open sea.
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID)
public final class PaperDragonKeeper {
    private static final int CHECK_EVERY = 90;
    private static final int RADIUS = 260;
    private static final int WANT = 2;

    private static final ResourceLocation VIOLET  = new ResourceLocation(RiftWorld.MODID, "violet_hills");
    private static final ResourceLocation GARDENS = new ResourceLocation(RiftWorld.MODID, "waterfall_gardens");
    private static final ResourceLocation SIZO    = new ResourceLocation(RiftWorld.MODID, "sizo_grove");
    private static final ResourceLocation CRYSTAL = new ResourceLocation(RiftWorld.MODID, "crystal_rifts");
    private static final ResourceLocation SEA     = new ResourceLocation(RiftWorld.MODID, "elisar_sea");
    private static final ResourceLocation REEF    = new ResourceLocation(RiftWorld.MODID, "shallow_reef");

    private PaperDragonKeeper() {
    }

    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        if (!level.dimension().equals(Flight.ELISAR_DIM)) return;
        if (level.getGameTime() % CHECK_EVERY != 0) return;

        CityPlan plan = CityPlan.get();
        int ocean = plan.ocean;

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            // Игрок внутри центра Белого Города — драконы в Город НЕ залетают и там не спавнятся!
            if (PaperDragonEntity.isInsideCityNoFlyZone(plan, player.getX(), player.getZ(), -40.0)) continue;

            // v0.26.0: Если поблизости есть Дикая Летающая Перевёрнутая Башня, заселяем её вершину спящим диким драконом!
            InvertedTowers.Site nearWildTower = InvertedTowers.nearest(player.getX(), player.getZ(), false, 1);
            if (nearWildTower != null && !nearWildTower.cityOrigin()
                    && Math.hypot(nearWildTower.cx() - player.getX(), nearWildTower.cz() - player.getZ()) <= 240.0
                    && level.hasChunk(nearWildTower.cx() >> 4, nearWildTower.cz() >> 4)) {
                AABB roostBox = new AABB(nearWildTower.cx() - 28, nearWildTower.roostY() - 24, nearWildTower.cz() - 28,
                        nearWildTower.cx() + 28, nearWildTower.roostY() + 32, nearWildTower.cz() + 28);
                if (level.getEntitiesOfClass(PaperDragonEntity.class, roostBox).isEmpty()) {
                    PaperDragonEntity sleeper = ModEntities.PAPER_DRAGON.get().create(level);
                    if (sleeper != null) {
                        BlockPos rp = new BlockPos(nearWildTower.cx(), nearWildTower.roostY() + 1, nearWildTower.cz());
                        sleeper.moveTo(nearWildTower.cx() + 0.5, nearWildTower.roostY() + 1.05, nearWildTower.cz() + 0.5,
                                level.random.nextFloat() * 360.0F, 0.0F);
                        sleeper.finalizeSpawn(level, level.getCurrentDifficultyAt(rp), MobSpawnType.NATURAL, null, null);
                        sleeper.placeCoiledOnWildTower(nearWildTower);
                        level.addFreshEntity(sleeper);
                        continue;
                    }
                }
            }

            AABB box = player.getBoundingBox().inflate(RADIUS, 160, RADIUS);
            int present = level.getEntitiesOfClass(PaperDragonEntity.class, box).size();
            if (present >= WANT) continue;

            var rng = level.random;
            for (int attempt = 0; attempt < 10; ++attempt) {
                double ang = rng.nextDouble() * Mth.TWO_PI;
                double dist = 85.0 + rng.nextDouble() * 115.0;
                int sx = Mth.floor(player.getX() + Math.cos(ang) * dist);
                int sz = Mth.floor(player.getZ() + Math.sin(ang) * dist);
                // Строгий запрет спавна рядом с Белым Городом!
                if (PaperDragonEntity.isInsideCityNoFlyZone(plan, sx + 0.5, sz + 0.5, 45.0)) continue;
                if (!level.hasChunk(sx >> 4, sz >> 4)) continue;

                ResourceLocation biomeKey = level.getBiome(new BlockPos(sx, ocean + 20, sz))
                        .unwrapKey().map(k -> k.location()).orElse(null);
                if (!allowedWildBiome(biomeKey)) continue;

                // Ищем живописный ДИКИЙ парящий остров поблизости, чтобы сразу запустить плавный облёт вдоль его скал
                CityPlan.Island wildIsl = null;
                double bestD2 = Double.MAX_VALUE;
                for (CityPlan.Island cand : FloatingIslands.gather(sx + 0.5, sz + 0.5, 190.0, false)) {
                    if (cand.kind == 0 || cand.R < 20.0) continue;
                    if (PaperDragonEntity.isInsideCityNoFlyZone(plan, cand.cx, cand.cz, 55.0)) continue;
                    double d2 = (cand.cx - sx) * (cand.cx - sx) + (cand.cz - sz) * (cand.cz - sz);
                    if (d2 < bestD2) {
                        bestD2 = d2;
                        wildIsl = cand;
                    }
                }

                int spawnX = sx;
                int spawnZ = sz;
                int spawnY;
                if (wildIsl != null) {
                    // Размещаем дракона СНАРУЖИ кромки острова (вдоль обрывов, а НЕ между постройками/деревьями!)
                    double rimAng = Math.atan2(sz - wildIsl.cz, sx - wildIsl.cx);
                    double perimeterR = wildIsl.maxReach() + 28.0 + rng.nextDouble() * 18.0;
                    spawnX = Mth.floor(wildIsl.cx + Math.cos(rimAng) * perimeterR);
                    spawnZ = Mth.floor(wildIsl.cz + Math.sin(rimAng) * perimeterR);
                    if (PaperDragonEntity.isInsideCityNoFlyZone(plan, spawnX + 0.5, spawnZ + 0.5, 40.0)) continue;
                    if (!level.hasChunk(spawnX >> 4, spawnZ >> 4)) continue;

                    double vaultBottom = wildIsl.base - wildIsl.depth;
                    if (vaultBottom - ocean >= 30.0 && rng.nextFloat() < 0.35F) {
                        spawnY = Mth.floor(Mth.lerp(0.5, ocean + 12.0, vaultBottom - 10.0));
                    } else {
                        spawnY = Mth.floor(wildIsl.base + 8.0 + rng.nextDouble() * 20.0);
                    }
                } else {
                    int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawnX, spawnZ);
                    spawnY = Math.max(top, ocean) + 28 + rng.nextInt(26);
                }

                spawnY = Mth.clamp(spawnY, ocean + 14, level.getMaxBuildHeight() - 36);
                BlockPos pos = new BlockPos(spawnX, spawnY, spawnZ);
                if (!isClearAirBox(level, pos, 5, 3)) continue;

                PaperDragonEntity dragon = ModEntities.PAPER_DRAGON.get().create(level);
                if (dragon == null) return;
                float initYaw = rng.nextFloat() * 360.0F;
                dragon.moveTo(spawnX + 0.5, spawnY + 0.5, spawnZ + 0.5, initYaw, 0.0F);
                dragon.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
                level.addFreshEntity(dragon);
                break;
            }
        }
    }

    private static boolean allowedWildBiome(ResourceLocation key) {
        return VIOLET.equals(key) || GARDENS.equals(key) || SIZO.equals(key)
                || CRYSTAL.equals(key) || SEA.equals(key) || REEF.equals(key);
    }

    private static boolean isClearAirBox(ServerLevel level, BlockPos center, int rHoriz, int rVert) {
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int dx = -rHoriz; dx <= rHoriz; dx += 2) {
            for (int dz = -rHoriz; dz <= rHoriz; dz += 2) {
                int wx = center.getX() + dx, wz = center.getZ() + dz;
                if (!level.hasChunk(wx >> 4, wz >> 4)) return false;
                for (int dy = -rVert; dy <= rVert; ++dy) {
                    mp.set(wx, center.getY() + dy, wz);
                    if (!level.getBlockState(mp).isAir() || !level.getFluidState(mp).isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
