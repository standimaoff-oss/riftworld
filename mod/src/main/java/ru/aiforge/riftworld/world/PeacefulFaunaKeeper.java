package ru.aiforge.riftworld.world;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.entity.BlossomStagEntity;
import ru.aiforge.riftworld.entity.SkyDartFishEntity;
import ru.aiforge.riftworld.flight.Flight;
import ru.aiforge.riftworld.registry.ElisarBlocks;
import ru.aiforge.riftworld.registry.ModEntities;

/**
 * v0.27.0: Поддержка естественного появления двух мирных существ обычного биома:
 *  - в Обычном мире (Overworld) — через ванильные SpawnPlacements + Forge biome_modifier
 *    ({@code data/riftworld/forge/biome_modifier/peaceful_fauna_overworld.json});
 *  - на мирных травянистых островах Элисара ({@code riftworld:elisar_city}) — через лёгкий фоновый кипер.
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID)
public final class PeacefulFaunaKeeper {
    private PeacefulFaunaKeeper() {
    }

    @Mod.EventBusSubscriber(modid = RiftWorld.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent
        public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
            event.register(ModEntities.BLOSSOM_STAG.get(),
                    SpawnPlacements.Type.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    BlossomStagEntity::checkStagSpawnRules,
                    SpawnPlacementRegisterEvent.Operation.REPLACE);
            event.register(ModEntities.SKY_DART_FISH.get(),
                    SpawnPlacements.Type.NO_RESTRICTIONS,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    SkyDartFishEntity::checkSkyDartSpawnRules,
                    SpawnPlacementRegisterEvent.Operation.REPLACE);
            // v0.38.0: без регистрации плейсмента ванильный NaturalSpawner НИКОГДА не берёт
            // моба из spawners биома — поэтому «Мороковая Падь» до этого была пустой: в JSON
            // не было ни списка спавна, ни правил постановки на блок.
            // v0.39.0: Морок — хозяин чащи. Ставится только на мерзлоту Пади и только в темноте,
            // поэтому днём его почти не видно, а ночью он появляется сам, без всяких скриптов.
            event.register(ModEntities.HOLLOWWATCHER.get(),
                    SpawnPlacementRegisterEvent.Operation.REPLACE);
            event.register(ModEntities.MOROK.get(),
                    SpawnPlacements.Type.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (type, level, spawnType, pos, random) -> {
                        if (spawnType == MobSpawnType.SPAWN_EGG || spawnType == MobSpawnType.COMMAND) return true;
                        BlockState below = level.getBlockState(pos.below());
                        return (below.is(ru.aiforge.riftworld.registry.MorokBlocks.RIME_GRASS.get())
                                || below.is(ru.aiforge.riftworld.registry.MorokBlocks.PALE_MOSS_BLOCK.get())
                                || below.is(ru.aiforge.riftworld.registry.MorokBlocks.FROZEN_SOIL.get()))
                                && level.getRawBrightness(pos, 0) <= 6;
                    },
                    SpawnPlacementRegisterEvent.Operation.REPLACE);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;
        if (!(event.level instanceof ServerLevel level)) return;
        if (!level.dimension().equals(Flight.ELISAR_DIM)) return;
        if ((level.getGameTime() % 100L) != 37L) return;

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            AABB box = player.getBoundingBox().inflate(72.0D, 36.0D, 72.0D);
            List<BlossomStagEntity> stags = level.getEntitiesOfClass(BlossomStagEntity.class, box);
            List<SkyDartFishEntity> fishes = level.getEntitiesOfClass(SkyDartFishEntity.class, box);
            if (stags.size() >= 4 && fishes.size() >= 6) continue;

            for (int tries = 0; tries < 8; ++tries) {
                double ang = level.random.nextDouble() * Math.PI * 2.0D;
                double dist = 18.0D + level.random.nextDouble() * 36.0D;
                int x = (int) Math.floor(player.getX() + Math.cos(ang) * dist);
                int z = (int) Math.floor(player.getZ() + Math.sin(ang) * dist);
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                if (AshenLands.weight(x + 0.5D, z + 0.5D) > 0.1D || MistKeep.inside(x + 0.5D, z + 0.5D)) continue;

                // Проверяем, что биом — именно обычный мирный биом островов (elisar_city)
                BlockPos topScan = findPeacefulGrass(level, x, z, (int) player.getY());
                if (topScan == null) continue;
                String biomeId = level.getBiome(topScan.above()).unwrapKey()
                        .map(k -> k.location().toString()).orElse("");
                if (!biomeId.endsWith(":elisar_city")) continue;

                if (stags.size() < 3 && level.random.nextBoolean()) {
                    int group = 1 + level.random.nextInt(2);
                    for (int g = 0; g < group; ++g) {
                        BlockPos gp = g == 0 ? topScan : findPeacefulGrass(level, x + level.random.nextIntBetweenInclusive(-3, 3),
                                z + level.random.nextIntBetweenInclusive(-3, 3), topScan.getY());
                        if (gp == null) continue;
                        BlossomStagEntity stag = ModEntities.BLOSSOM_STAG.get().create(level);
                        if (stag != null) {
                            stag.moveTo(gp.getX() + 0.5D, gp.getY() + 1.0D, gp.getZ() + 0.5D,
                                    level.random.nextFloat() * 360.0F, 0.0F);
                            stag.setBloomVariant(level.random.nextInt(3));
                            if (level.noCollision(stag)) {
                                level.addFreshEntity(stag);
                            }
                        }
                    }
                    break;
                } else if (fishes.size() < 5) {
                    int school = 2 + level.random.nextInt(3);
                    for (int s = 0; s < school; ++s) {
                        SkyDartFishEntity fish = ModEntities.SKY_DART_FISH.get().create(level);
                        if (fish != null) {
                            double fx = topScan.getX() + 0.5D + (level.random.nextDouble() - 0.5D) * 3.5D;
                            double fy = topScan.getY() + 2.2D + level.random.nextDouble() * 2.2D;
                            double fz = topScan.getZ() + 0.5D + (level.random.nextDouble() - 0.5D) * 3.5D;
                            fish.moveTo(fx, fy, fz, level.random.nextFloat() * 360.0F, 0.0F);
                            fish.finalizeSpawn(level, level.getCurrentDifficultyAt(topScan.above(2)),
                                    MobSpawnType.NATURAL, null, null);
                            if (level.noCollision(fish)) {
                                level.addFreshEntity(fish);
                            }
                        }
                    }
                    break;
                }
            }
        }
    }

    private static BlockPos findPeacefulGrass(ServerLevel level, int x, int z, int refY) {
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos(x, 0, z);
        int topY = Math.min(level.getMaxBuildHeight() - 4, refY + 28);
        int botY = Math.max(CityPlan.get().ocean + 2, refY - 28);
        for (int y = topY; y >= botY; --y) {
            mp.setY(y);
            BlockState st = level.getBlockState(mp);
            if (st.is(ElisarBlocks.GRASS.get()) || st.is(ElisarBlocks.BLOSSOM_MOSS.get())) {
                if (level.isEmptyBlock(mp.above()) && level.isEmptyBlock(mp.above(2))) {
                    return mp.immutable();
                }
            } else if (!st.isAir() && st.blocksMotion()) {
                return null; // наткнулись на крышу/дорогу
            }
        }
        return null;
    }
}
