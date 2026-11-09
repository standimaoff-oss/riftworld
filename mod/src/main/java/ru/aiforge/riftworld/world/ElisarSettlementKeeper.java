package ru.aiforge.riftworld.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.entity.ElisarArcherEntity;
import ru.aiforge.riftworld.entity.ElisarGuardEntity;
import ru.aiforge.riftworld.entity.ElisarVillagerEntity;
import ru.aiforge.riftworld.flight.Flight;
import ru.aiforge.riftworld.registry.ModEntities;

/**
 * v0.48.0: Расселяет жителей Элисара (жителей, стражей и лучников) по площадям столицы «Странники»,
 * белым деревням на островах и надводным свайным деревням — как при первичной генерации чанка
 * ({@link #populateChunkRegion(WorldGenRegion)}), так и мягкой проверкой уже загруженных поселений.
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID)
public final class ElisarSettlementKeeper {
    private ElisarSettlementKeeper() {
    }

    public static void populateChunkRegion(WorldGenRegion region) {
        var center = region.getCenter();
        int cx = center.x;
        int cz = center.z;
        double midX = (cx << 4) + 8.0D;
        double midZ = (cz << 4) + 8.0D;

        // 1. Площади столицы «Странники»
        CityPlan plan = CityPlan.get();
        for (CityPlan.Plaza plaza : plan.plazas) {
            int pcx = ((int) Math.floor(plaza.x())) >> 4;
            int pcz = ((int) Math.floor(plaza.z())) >> 4;
            if (pcx == cx && pcz == cz) {
                spawnSquad(region, (int) Math.round(plaza.x()), (int) Math.round(plaza.z()), 2, 1, 1, MobSpawnType.CHUNK_GENERATION);
                return;
            }
        }

        // 2. Белые деревни на диких островах (кроме сгоревших в Пепельных землях)
        WorldStructures.Village wv = WorldStructures.whiteVillageAt(midX, midZ, 14.0D);
        if (wv != null && !wv.burned && (wv.cx >> 4) == cx && (wv.cz >> 4) == cz) {
            spawnSquad(region, wv.cx, wv.cz, 3, 1, 1, MobSpawnType.CHUNK_GENERATION);
            return;
        }

        // 3. Надводные свайные деревни
        WorldStructures.StiltVillage sv = WorldStructures.stiltVillageNear(midX, midZ, 14.0D);
        if (sv != null && (sv.cx >> 4) == cx && (sv.cz >> 4) == cz) {
            spawnSquad(region, sv.cx, sv.cz, 2, 1, 1, MobSpawnType.CHUNK_GENERATION);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel level)) return;
        if (!level.dimension().equals(Flight.ELISAR)) return;
        if ((level.getGameTime() % 80L) != 23L) return;

        CityPlan plan = CityPlan.get();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            double px = player.getX();
            double pz = player.getZ();

            int anchorX = Integer.MIN_VALUE;
            int anchorZ = Integer.MIN_VALUE;

            for (CityPlan.Plaza plaza : plan.plazas) {
                if (Math.hypot(px - plaza.x(), pz - plaza.z()) <= Math.max(22.0D, plaza.r() + 14.0D)) {
                    anchorX = (int) Math.round(plaza.x());
                    anchorZ = (int) Math.round(plaza.z());
                    break;
                }
            }
            if (anchorX == Integer.MIN_VALUE) {
                WorldStructures.Village wv = WorldStructures.whiteVillageAt(px, pz, 42.0D);
                if (wv != null && !wv.burned) {
                    anchorX = wv.cx;
                    anchorZ = wv.cz;
                }
            }
            if (anchorX == Integer.MIN_VALUE) {
                WorldStructures.StiltVillage sv = WorldStructures.stiltVillageNear(px, pz, 38.0D);
                if (sv != null) {
                    anchorX = sv.cx;
                    anchorZ = sv.cz;
                }
            }

            if (anchorX == Integer.MIN_VALUE || !level.hasChunk(anchorX >> 4, anchorZ >> 4)) {
                continue;
            }
            AABB checkBox = new AABB(anchorX - 44, level.getMinBuildHeight(), anchorZ - 44,
                    anchorX + 44, level.getMaxBuildHeight(), anchorZ + 44);
            int existing = level.getEntitiesOfClass(ElisarVillagerEntity.class, checkBox).size()
                    + level.getEntitiesOfClass(ElisarGuardEntity.class, checkBox).size()
                    + level.getEntitiesOfClass(ElisarArcherEntity.class, checkBox).size();
            if (existing == 0) {
                spawnSquad(level, anchorX, anchorZ, 2, 1, 1, MobSpawnType.STRUCTURE);
            }
        }
    }

    public static int spawnSquad(ServerLevelAccessor region, int cx, int cz,
                                 int villagers, int guards, int archers, MobSpawnType spawnType) {
        int spawned = 0;
        int[][] offsets = {
                {0, 0}, {3, 2}, {-3, 2}, {2, -3}, {-2, -3}, {5, 0}, {-5, 0}, {0, 5}, {0, -5}
        };
        int cursor = 0;
        for (int i = 0; i < villagers && cursor < offsets.length; ++i, ++cursor) {
            if (spawnOne(region, ModEntities.ELISAR_VILLAGER.get(), cx + offsets[cursor][0], cz + offsets[cursor][1], spawnType)) {
                ++spawned;
            }
        }
        for (int i = 0; i < guards && cursor < offsets.length; ++i, ++cursor) {
            if (spawnOne(region, ModEntities.ELISAR_GUARD.get(), cx + offsets[cursor][0], cz + offsets[cursor][1], spawnType)) {
                ++spawned;
            }
        }
        for (int i = 0; i < archers && cursor < offsets.length; ++i, ++cursor) {
            if (spawnOne(region, ModEntities.ELISAR_ARCHER.get(), cx + offsets[cursor][0], cz + offsets[cursor][1], spawnType)) {
                ++spawned;
            }
        }
        return spawned;
    }

    private static <T extends PathfinderMob> boolean spawnOne(
            ServerLevelAccessor region, EntityType<T> type, int x, int z, MobSpawnType spawnType) {
        if (!region.hasChunk(x >> 4, z >> 4)) return false;
        int y = region.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= region.getMinBuildHeight() + 2 || y >= region.getMaxBuildHeight() - 4) {
            return false;
        }
        BlockPos pos = new BlockPos(x, y, z);
        BlockState below = region.getBlockState(pos.below());
        if (!below.getFluidState().isEmpty() || below.isAir()) {
            return false;
        }
        if (!region.getFluidState(pos).isEmpty() || !region.getBlockState(pos).getCollisionShape(region, pos).isEmpty()) {
            return false;
        }
        ServerLevel sl = region.getLevel();
        T mob = type.create(sl);
        if (mob == null) return false;
        float yaw = sl.random.nextFloat() * 360.0F;
        mob.moveTo(x + 0.5D, y, z + 0.5D, yaw, 0.0F);
        mob.finalizeSpawn(region, region.getCurrentDifficultyAt(pos), spawnType, null, null);
        mob.restrictTo(new BlockPos(x, y, z), 32);
        return region.addFreshEntity(mob);
    }
}
