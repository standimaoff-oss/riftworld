package ru.aiforge.riftworld.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.entity.ElisarArcherEntity;
import ru.aiforge.riftworld.entity.ElisarCitizenEntity;
import ru.aiforge.riftworld.entity.ElisarGuardEntity;
import ru.aiforge.riftworld.entity.ElisarVillagerEntity;
import ru.aiforge.riftworld.registry.ModEntities;

/** Deterministic population of Elisar cities/villages and companions for vanilla village NPCs. */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID)
public final class ElisarSettlementResidents {
    private static final String POPULATED = "RiftworldSettlementAttendants";
    private static final int[][] OFFSETS = {{-2, 0}, {2, 0}, {0, 2}, {0, -2}};

    /** Called once by ElisarChunkGenerator after blocks and structures have been generated. */
    public static void spawnOriginal(WorldGenRegion level) {
        int cx = level.getCenter().x, cz = level.getCenter().z;
        CityPlan plan = CityPlan.get();
        for (CityPlan.Plaza plaza : plan.plazas) {
            if (((int) Math.floor(plaza.x()) >> 4) != cx || ((int) Math.floor(plaza.z()) >> 4) != cz) continue;
            ElisarChunkGenerator.Col column = ElisarChunkGenerator.column(plan, (int) plaza.x(), (int) plaza.z());
            int y = (column.deckY != Integer.MIN_VALUE ? column.deckY : column.top) + 1;
            if (y > plan.ocean + 1) spawnGroup(level, new BlockPos((int) plaza.x(), y, (int) plaza.z()), 26);
        }

        WorldStructures.Village village = WorldStructures.whiteVillageAt(level.getCenter().getMinBlockX() + 8.0D,
                level.getCenter().getMinBlockZ() + 8.0D, 56.0D);
        if (village != null && village.cx >> 4 == cx && village.cz >> 4 == cz) {
            spawnGroup(level, new BlockPos(village.cx, village.baseY + 1, village.cz), 24);
        }
        WorldStructures.StiltVillage stilt = WorldStructures.stiltVillageNear(level.getCenter().getMinBlockX() + 8.0D,
                level.getCenter().getMinBlockZ() + 8.0D, 42.0D);
        if (stilt != null && stilt.cx >> 4 == cx && stilt.cz >> 4 == cz) {
            spawnGroup(level, new BlockPos(stilt.cx, stilt.deckY + 1, stilt.cz), 20);
        }
    }

    /** Vanilla villages get the same three roles, once per original villager and with nearby caps. */
    @SubscribeEvent
    public static void onVillagerJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Villager villager)
                || !(event.getLevel() instanceof ServerLevel level)) return;
        if (villager.getPersistentData().getBoolean(POPULATED)) return;
        villager.getPersistentData().putBoolean(POPULATED, true);
        BlockPos home = villager.blockPosition();
        if (!level.structureManager().getStructureWithPieceAt(home, StructureTags.VILLAGE).isValid()) return;
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, home);
        if (level.getEntitiesOfClass(ElisarVillagerEntity.class, villager.getBoundingBox().inflate(32.0D)).size() < 3) {
            spawn(level, ModEntities.ELISAR_VILLAGER.get(), surface.offset(-2, 0, 0), 28);
        }
        if (level.getEntitiesOfClass(ElisarGuardEntity.class, villager.getBoundingBox().inflate(32.0D)).size() < 2) {
            spawn(level, ModEntities.ELISAR_GUARD.get(), surface.offset(2, 0, 0), 28);
        }
        if (level.getEntitiesOfClass(ElisarArcherEntity.class, villager.getBoundingBox().inflate(32.0D)).size() < 2) {
            spawn(level, ModEntities.ELISAR_ARCHER.get(), surface.offset(0, 0, 2), 28);
        }
    }

    private static void spawnGroup(WorldGenLevel level, BlockPos home, int radius) {
        spawn(level, ModEntities.ELISAR_VILLAGER.get(), home.offset(OFFSETS[0][0], 0, OFFSETS[0][1]), radius);
        spawn(level, ModEntities.ELISAR_VILLAGER.get(), home.offset(OFFSETS[1][0], 0, OFFSETS[1][1]), radius);
        spawn(level, ModEntities.ELISAR_GUARD.get(), home.offset(OFFSETS[2][0], 0, OFFSETS[2][1]), radius);
        spawn(level, ModEntities.ELISAR_ARCHER.get(), home.offset(OFFSETS[3][0], 0, OFFSETS[3][1]), radius);
    }

    private static <T extends ElisarCitizenEntity> void spawn(WorldGenLevel level, EntityType<T> type, BlockPos requested, int radius) {
        ServerLevel server = level.getLevel();
        for (int attempt = 0; attempt < 5; ++attempt) {
            BlockPos pos = requested.above(attempt > 2 ? 1 : 0);
            T entity = type.create(server);
            if (entity == null) return;
            entity.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, attempt * 72.0F, 0.0F);
            if (!level.noCollision(entity)) continue;
            entity.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null, null);
            entity.setPersistenceRequired();
            entity.restrictTo(pos, radius);
            level.addFreshEntity(entity);
            return;
        }
    }

    private ElisarSettlementResidents() {
    }
}
