package ru.aiforge.riftworld.world;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
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
import ru.aiforge.riftworld.entity.ElGranMajaEntity;
import ru.aiforge.riftworld.entity.fauna.AmethystFoxEntity;
import ru.aiforge.riftworld.entity.fauna.AshMothEntity;
import ru.aiforge.riftworld.entity.fauna.AzureFrogEntity;
import ru.aiforge.riftworld.entity.fauna.BasaltBeetleEntity;
import ru.aiforge.riftworld.entity.fauna.BlossomSnailEntity;
import ru.aiforge.riftworld.entity.fauna.CoralHermitCrabEntity;
import ru.aiforge.riftworld.entity.fauna.CrystalGeckoEntity;
import ru.aiforge.riftworld.entity.fauna.ElisarFaunaUtil;
import ru.aiforge.riftworld.entity.fauna.FacetedOwlEntity;
import ru.aiforge.riftworld.entity.fauna.GhostSquidEntity;
import ru.aiforge.riftworld.entity.fauna.PhantomWolfEntity;
import ru.aiforge.riftworld.entity.fauna.PorcelainCraneEntity;
import ru.aiforge.riftworld.entity.fauna.ReefSeahorseEntity;
import ru.aiforge.riftworld.entity.fauna.SilverSeaSerpentEntity;
import ru.aiforge.riftworld.entity.fauna.SizoLeafwingEntity;
import ru.aiforge.riftworld.flight.Flight;
import ru.aiforge.riftworld.registry.ModEntities;

/**
 * v0.47.0: Смотритель биомной фауны Элисара — расселяет все 15 существ строго по их родным биомам
 * только в уже загруженных чанках вокруг игроков, с малыми локальными лимитами и нулевой нагрузкой на TPS.
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID)
public final class ElisarBiomeFaunaKeeper {
    private static int failsafe = 0;

    private ElisarBiomeFaunaKeeper() {
    }

    @Mod.EventBusSubscriber(modid = RiftWorld.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent
        public static void registerSpawnPlacements(SpawnPlacementRegisterEvent e) {
            regGround(e, ModEntities.PORCELAIN_CRANE.get());
            regGround(e, ModEntities.BASALT_BEETLE.get());
            regAir(e, ModEntities.ASH_MOTH.get());
            regGround(e, ModEntities.AMETHYST_FOX.get());
            regGround(e, ModEntities.SIZO_LEAFWING.get());
            regGround(e, ModEntities.AZURE_FROG.get());
            regGround(e, ModEntities.BLOSSOM_SNAIL.get());
            regGround(e, ModEntities.PHANTOM_WOLF.get());
            regGround(e, ModEntities.CRYSTAL_GECKO.get());
            regAir(e, ModEntities.FACETED_OWL.get());
            regAny(e, ModEntities.CORAL_HERMIT_CRAB.get());
            regAny(e, ModEntities.REEF_SEAHORSE.get());
            regAny(e, ModEntities.GHOST_SQUID.get());
            regAny(e, ModEntities.SILVER_SEA_SERPENT.get());
        }

        private static <T extends Mob> void regGround(SpawnPlacementRegisterEvent e, EntityType<T> type) {
            e.register(type, SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (t, lvl, st, pos, rnd) -> st == MobSpawnType.SPAWN_EGG || st == MobSpawnType.COMMAND
                            || lvl.getBlockState(pos.below()).isSolid(),
                    SpawnPlacementRegisterEvent.Operation.REPLACE);
        }

        private static <T extends Mob> void regAir(SpawnPlacementRegisterEvent e, EntityType<T> type) {
            e.register(type, SpawnPlacements.Type.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (t, lvl, st, pos, rnd) -> true,
                    SpawnPlacementRegisterEvent.Operation.REPLACE);
        }

        private static <T extends Mob> void regAny(SpawnPlacementRegisterEvent e, EntityType<T> type) {
            e.register(type, SpawnPlacements.Type.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (t, lvl, st, pos, rnd) -> true,
                    SpawnPlacementRegisterEvent.Operation.REPLACE);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) return;
        if (!(event.level instanceof ServerLevel level)) return;
        if (!level.dimension().equals(Flight.ELISAR_DIM)) return;
        if ((level.getGameTime() % 80L) != 53L) return; // раз в 4 секунды в противофазе с другими киперами
        try {
            tick(level);
        } catch (Throwable t) {
            if (++failsafe <= 6) {
                System.out.println("[riftworld] v0.47 fauna keeper skipped: " + t);
            }
        }
    }

    private static void tick(ServerLevel level) {
        boolean peaceful = level.getDifficulty() == Difficulty.PEACEFUL;
        boolean night = !level.isDay();

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            AABB localBox = player.getBoundingBox().inflate(72.0D, 40.0D, 72.0D);

            for (int attempt = 0; attempt < 6; ++attempt) {
                double ang = level.random.nextDouble() * Math.PI * 2.0D;
                double dist = 18.0D + level.random.nextDouble() * 38.0D;
                int x = (int) Math.floor(player.getX() + Math.cos(ang) * dist);
                int z = (int) Math.floor(player.getZ() + Math.sin(ang) * dist);
                if (!level.hasChunk(x >> 4, z >> 4)) continue;

                BlockPos probe = new BlockPos(x, MthClampY(level, (int) player.getY()), z);
                String biome = level.getBiome(probe).unwrapKey().map(k -> k.location().getPath()).orElse("");

                if (trySpawnMarineFauna(level, player, localBox, x, z, biome)) {
                    break;
                }
                if (trySpawnLandOrSkyFauna(level, localBox, x, z, (int) player.getY(), biome, night, peaceful)) {
                    break;
                }
            }
        }
    }

    private static int MthClampY(ServerLevel level, int y) {
        return Math.max(level.getMinBuildHeight() + 8, Math.min(level.getMaxBuildHeight() - 8, y));
    }

    private static boolean trySpawnMarineFauna(ServerLevel level, ServerPlayer player, AABB box,
                                               int x, int z, String biome) {
        int sea = CityPlan.get().ocean;
        if ("shallow_reef".equals(biome)) {
            BlockPos seabed = findWaterFloor(level, x, z, sea);
            if (seabed == null) return false;
            if (level.random.nextBoolean()
                    && level.getEntitiesOfClass(ReefSeahorseEntity.class, box).size() < 5) {
                int school = 2 + level.random.nextInt(3); // 2..4
                int spawned = 0;
                for (int i = 0; i < school; ++i) {
                    BlockPos wp = seabed.above(1 + level.random.nextInt(3)).offset(
                            level.random.nextIntBetweenInclusive(-2, 2), 0,
                            level.random.nextIntBetweenInclusive(-2, 2));
                    if (!level.hasChunk(wp.getX() >> 4, wp.getZ() >> 4)) continue;
                    if (!ElisarFaunaUtil.isAnyWater(level, wp)) continue;
                    if (spawnOne(level, ModEntities.REEF_SEAHORSE.get(), wp.getX() + 0.5D, wp.getY() + 0.1D, wp.getZ() + 0.5D)) {
                        ++spawned;
                    }
                }
                return spawned > 0;
            }
            if (level.getEntitiesOfClass(CoralHermitCrabEntity.class, box).size() < 3) {
                return spawnOne(level, ModEntities.CORAL_HERMIT_CRAB.get(),
                        seabed.getX() + 0.5D, seabed.getY() + 1.0D, seabed.getZ() + 0.5D);
            }
            return false;
        }

        if ("abyss".equals(biome)) {
            if (level.getEntitiesOfClass(GhostSquidEntity.class, box).size() >= 2) return false;
            BlockPos seabed = findWaterFloor(level, x, z, sea);
            if (seabed == null || (sea - seabed.getY()) < 18) return false;
            BlockPos deepPos = seabed.above(4 + level.random.nextInt(6));
            if (ElisarFaunaUtil.isAnyWater(level, deepPos) && level.getMaxLocalRawBrightness(deepPos) < 10) {
                return spawnOne(level, ModEntities.GHOST_SQUID.get(),
                        deepPos.getX() + 0.5D, deepPos.getY() + 0.2D, deepPos.getZ() + 0.5D);
            }
            return false;
        }

        if ("maja_ocean".equals(biome) || DeepOcean.contains(x, z)) {
            if (level.random.nextFloat() > 0.20F) return false;
            if (level.getEntitiesOfClass(SilverSeaSerpentEntity.class, player.getBoundingBox().inflate(128.0D)).size() >= 1) {
                return false;
            }
            if (!level.getEntitiesOfClass(ElGranMajaEntity.class, player.getBoundingBox().inflate(80.0D)).isEmpty()) {
                return false;
            }
            if (countLoaded(level, SilverSeaSerpentEntity.class, 4) >= 4) return false;
            BlockPos wp = new BlockPos(x, sea - 10 - level.random.nextInt(14), z);
            if (ElisarFaunaUtil.isAnyWater(level, wp) && ElisarFaunaUtil.isAnyWater(level, wp.above(2))) {
                return spawnOne(level, ModEntities.SILVER_SEA_SERPENT.get(),
                        wp.getX() + 0.5D, wp.getY() + 0.5D, wp.getZ() + 0.5D);
            }
        }
        return false;
    }

    private static boolean trySpawnLandOrSkyFauna(ServerLevel level, AABB box, int x, int z, int refY,
                                                  String biome, boolean night, boolean peaceful) {
        BlockPos ground = findSurfaceGround(level, x, z, refY);
        if (ground == null) return false;
        String surfBiome = level.getBiome(ground.above()).unwrapKey().map(k -> k.location().getPath()).orElse(biome);

        return switch (surfBiome) {
            case "elisar_city" -> level.getEntitiesOfClass(PorcelainCraneEntity.class, box).size() < 2
                    && spawnOne(level, ModEntities.PORCELAIN_CRANE.get(), ground.getX() + 0.5D, ground.getY() + 1.0D, ground.getZ() + 0.5D);

            case "ashen_lands" -> {
                if (!peaceful && (night || level.getMaxLocalRawBrightness(ground.above(2)) <= 8)
                        && level.random.nextBoolean()
                        && level.getEntitiesOfClass(AshMothEntity.class, box).size() < 3) {
                    yield spawnOne(level, ModEntities.ASH_MOTH.get(),
                            ground.getX() + 0.5D, ground.getY() + 2.6D, ground.getZ() + 0.5D);
                }
                yield level.getEntitiesOfClass(BasaltBeetleEntity.class, box).size() < 2
                        && spawnOne(level, ModEntities.BASALT_BEETLE.get(),
                        ground.getX() + 0.5D, ground.getY() + 1.0D, ground.getZ() + 0.5D);
            }

            case "violet_hills" -> level.getEntitiesOfClass(AmethystFoxEntity.class, box).size() < 2
                    && spawnOne(level, ModEntities.AMETHYST_FOX.get(),
                    ground.getX() + 0.5D, ground.getY() + 1.0D, ground.getZ() + 0.5D);

            case "sizo_grove" -> level.getEntitiesOfClass(SizoLeafwingEntity.class, box).size() < 2
                    && spawnOne(level, ModEntities.SIZO_LEAFWING.get(),
                    ground.getX() + 0.5D, ground.getY() + 1.0D, ground.getZ() + 0.5D);

            case "waterfall_gardens" -> {
                if (level.random.nextBoolean() && level.getEntitiesOfClass(AzureFrogEntity.class, box).size() < 3) {
                    yield spawnOne(level, ModEntities.AZURE_FROG.get(),
                            ground.getX() + 0.5D, ground.getY() + 1.0D, ground.getZ() + 0.5D);
                }
                yield level.getEntitiesOfClass(BlossomSnailEntity.class, box).size() < 2
                        && spawnOne(level, ModEntities.BLOSSOM_SNAIL.get(),
                        ground.getX() + 0.5D, ground.getY() + 1.0D, ground.getZ() + 0.5D);
            }

            case "mist_keep" -> !peaceful
                    && level.getMaxLocalRawBrightness(ground.above()) <= 9
                    && level.getEntitiesOfClass(PhantomWolfEntity.class, box).size() < 2
                    && spawnOne(level, ModEntities.PHANTOM_WOLF.get(),
                    ground.getX() + 0.5D, ground.getY() + 1.0D, ground.getZ() + 0.5D);

            case "crystal_rifts" -> {
                if (level.random.nextBoolean() && level.getEntitiesOfClass(FacetedOwlEntity.class, box).size() < 2) {
                    yield spawnOne(level, ModEntities.FACETED_OWL.get(),
                            ground.getX() + 0.5D, ground.getY() + 2.5D, ground.getZ() + 0.5D);
                }
                yield level.getEntitiesOfClass(CrystalGeckoEntity.class, box).size() < 3
                        && spawnOne(level, ModEntities.CRYSTAL_GECKO.get(),
                        ground.getX() + 0.5D, ground.getY() + 1.0D, ground.getZ() + 0.5D);
            }

            default -> false;
        };
    }

    private static <T extends Mob> boolean spawnOne(ServerLevel level, EntityType<T> type, double x, double y, double z) {
        if (type.getCategory() == MobCategory.MONSTER && level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        T mob = type.create(level);
        if (mob == null) return false;
        mob.moveTo(x, y, z, level.random.nextFloat() * 360.0F, 0.0F);
        if (!level.noCollision(mob)) return false;
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(x, y, z)),
                MobSpawnType.NATURAL, null, null);
        return level.addFreshEntity(mob);
    }

    private static int countLoaded(ServerLevel level, Class<?> cls, int cap) {
        int c = 0;
        for (var e : level.getAllEntities()) {
            if (cls.isInstance(e) && ++c >= cap) break;
        }
        return c;
    }

    @Nullable
    private static BlockPos findSurfaceGround(ServerLevel level, int x, int z, int refY) {
        int top = Math.min(level.getMaxBuildHeight() - 4, refY + 28);
        int bot = Math.max(level.getMinBuildHeight() + 4, refY - 28);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = top; y >= bot; --y) {
            p.set(x, y, z);
            BlockState ground = level.getBlockState(p);
            if (!ground.isSolid() || ElisarFaunaUtil.isAnyWater(level, p)) continue;
            BlockState a1 = level.getBlockState(p.above());
            BlockState a2 = level.getBlockState(p.above(2));
            if (!a1.isSolid() && !a2.isSolid() && !ElisarFaunaUtil.isAnyWater(level, p.above())) {
                return p.immutable();
            }
        }
        return null;
    }

    @Nullable
    private static BlockPos findWaterFloor(ServerLevel level, int x, int z, int sea) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, sea, z);
        if (!ElisarFaunaUtil.isAnyWater(level, p)) {
            boolean found = false;
            for (int y = sea + 2; y >= sea - 24; --y) {
                p.set(x, y, z);
                if (ElisarFaunaUtil.isAnyWater(level, p)) {
                    found = true;
                    break;
                }
            }
            if (!found) return null;
        }
        int depth = 0;
        while (depth < 68 && p.getY() > level.getMinBuildHeight() + 2 && ElisarFaunaUtil.isAnyWater(level, p.below())) {
            p.move(0, -1, 0);
            ++depth;
        }
        BlockPos floor = p.below();
        return level.getBlockState(floor).isSolid() ? floor : null;
    }
}
