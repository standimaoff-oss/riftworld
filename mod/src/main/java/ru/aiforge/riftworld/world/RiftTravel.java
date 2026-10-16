package ru.aiforge.riftworld.world;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import ru.aiforge.riftworld.block.DemonGateBlock;
import ru.aiforge.riftworld.flight.Flight;

/**
 * Two-way dimensional travel across the Rift:
 * Overworld (ruined abandoned temple) &lt;-&gt; Elisar (intact Great Temple in the city).
 * Remembers the Overworld temple location so the return journey always lands right at the ruin.
 */
public final class RiftTravel {
    private static volatile BlockPos overworldAltar = null;

    private RiftTravel() {
    }

    /** Overworld portal used: remember it so travel from Elisar returns here. */
    public static void remember(ServerLevel from, BlockPos altar) {
        if (!from.dimension().equals(Level.OVERWORLD)) return;
        overworldAltar = altar.immutable();
    }

    public static void setOverworldAltar(ServerLevel overworld, BlockPos altar) {
        overworldAltar = altar.immutable();
    }

    @Nullable
    public static BlockPos overworldAltar(MinecraftServer server) {
        return overworldAltar;
    }

    /** Teleports player from Overworld ruined temple to Elisar Great Temple. */
    public static void travelToElisar(ServerPlayer p, ServerLevel sl) {
        MinecraftServer server = sl.getServer();
        ServerLevel elisar = server.getLevel(Flight.ELISAR_DIM);
        if (elisar == null) return;

        BlockPos target = DemonGateBlock.templeApron(elisar);

        sl.playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.0F, 0.6F);
        sl.playSound(null, p.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 1.1F, 0.5F);
        sl.sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1.0, p.getZ(), 45, 0.5, 0.8, 0.5, 0.04);
        sl.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1.0, p.getZ(), 30, 0.5, 0.5, 0.5, 0.1);

        p.teleportTo(elisar, target.getX() + 0.5, target.getY(), target.getZ() + 0.5, p.getYRot(), p.getXRot());

        elisar.playSound(null, target, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.9F, 1.2F);
        elisar.playSound(null, target, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.2F, 0.8F);
        elisar.sendParticles(ParticleTypes.SQUID_INK, target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5, 50, 1.0, 1.0, 1.0, 0.03);
        elisar.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX() + 0.5, target.getY() + 0.6, target.getZ() + 0.5, 25, 0.8, 0.5, 0.8, 0.02);
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0, false, false));
    }

    /** Teleports player from Elisar Great Temple back to Overworld ruined temple. */
    public static void travelToOverworld(ServerPlayer p, ServerLevel sl) {
        MinecraftServer server = sl.getServer();
        ServerLevel ow = server.overworld();
        if (ow == null) return;

        BlockPos target = overworldAltar(server);
        if (target == null) {
            target = ow.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, ow.getSharedSpawnPos());
        }

        sl.playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.0F, 0.8F);
        sl.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.2F, 0.6F);
        sl.sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1.0, p.getZ(), 50, 0.6, 1.0, 0.6, 0.05);

        // Arrive just in front of the altar/portal
        BlockPos arr = target.offset(0, 0, 2);
        p.teleportTo(ow, arr.getX() + 0.5, arr.getY() + 0.5, arr.getZ() + 0.5, p.getYRot(), p.getXRot());

        ow.playSound(null, arr, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.0F, 1.2F);
        ow.playSound(null, arr, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.2F, 0.8F);
        ow.sendParticles(ParticleTypes.SQUID_INK, arr.getX() + 0.5, arr.getY() + 1.0, arr.getZ() + 0.5, 60, 1.0, 1.0, 1.0, 0.03);
        ow.sendParticles(ParticleTypes.FLASH, arr.getX() + 0.5, arr.getY() + 1.2, arr.getZ() + 0.5, 2, 0.4, 0.4, 0.4, 0);
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false));
    }
}
