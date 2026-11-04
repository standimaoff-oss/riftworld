package ru.aiforge.riftworld.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.entity.ElGranMajaEntity;
import ru.aiforge.riftworld.entity.MajaCollision;
import ru.aiforge.riftworld.flight.Flight;
import ru.aiforge.riftworld.registry.ModEntities;

/** At most one persistent natural Maja per 4096-block ocean cell, across reloads. */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID)
public final class MajaOceanKeeper {
    public static final ResourceKey<Biome> BIOME = ResourceKey.create(Registries.BIOME,
            new ResourceLocation(RiftWorld.MODID, "maja_ocean"));
    private MajaOceanKeeper() {}

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || !level.dimension().equals(Flight.ELISAR_DIM) || level.getGameTime() % 40 != 0) return;
        MajaWorldData data = MajaWorldData.get(level);
        int sea = CityPlan.get().ocean;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            int px = floor(player.getX()), pz = floor(player.getZ());
            DeepOcean.Region region = DeepOcean.regionAt(px, pz);
            if (!DeepOcean.contains(px, pz) || data.hasSpawned(region.key())) continue;
            if (!level.getBiome(new BlockPos(px, sea - 8, pz)).is(BIOME)) continue;
            // The 322-block animal has to fit in *already loaded* chunks.
            // Centre it within the player's chunk and try diagonals first:
            // these reduce its maximum X/Z reach, whereas three random angles
            // and offsets often had no chance of fitting at normal view distance.
            double x = ((px >> 4) << 4) + 8.0;
            double z = ((pz >> 4) << 4) + 8.0;
            int first = level.random.nextInt(4);
            boolean spawned = false;
            // A fixed sea-13 anchor put the animated jaw through the seabed.
            // Try progressively shallower positions, keeping the same full
            // loaded-chunk, border and solid-block checks for each candidate.
            for (int rise = 0; rise <= 40 && !spawned; rise += 8) {
                Vec3 at = new Vec3(x, sea - 13.0 + rise, z);
                for (int attempt = 0; attempt < 4; ++attempt) {
                    float yaw = 45.0F + ((first + attempt) & 3) * 90.0F;
                    if (!MajaCollision.clear(level, at, yaw)) continue;
                    ElGranMajaEntity maja = new ElGranMajaEntity(ModEntities.EL_GRAN_MAJA.get(), level);
                    maja.moveTo(at.x, at.y, at.z, yaw, 0.0F);
                    if (level.addFreshEntity(maja)) {
                        data.recordSpawn(region.key());
                        spawned = true;
                    }
                    break;
                }
            }
        }
    }

    private static int floor(double value) { return (int) Math.floor(value); }
}
