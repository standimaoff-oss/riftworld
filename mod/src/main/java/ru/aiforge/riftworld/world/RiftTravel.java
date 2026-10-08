package ru.aiforge.riftworld.world;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import ru.aiforge.riftworld.block.RiftAltarBlock;
import ru.aiforge.riftworld.block.RiftAltarBlockEntity;
import ru.aiforge.riftworld.net.Net;
import ru.aiforge.riftworld.net.RiftFxMsg;
import ru.aiforge.riftworld.registry.RiftBlocks;

/**
 * Crossing the rift: overworld temple &lt;-&gt; the pale world of Elisar.
 * <p>
 * The ritual hall of the Great Temple exists in both worlds (the twin temple of the city «Странники» is a fixed plot of
 * the deterministic city plan), so the destination is known: the overworld leads to the Elisar altar and back the
 * traveller lands at the altar remembered by the overworld's saved data.
 */
public final class RiftTravel {
    private RiftTravel() {
    }

    /** The overworld altar was opened: remember it, so the way back has a target. */
    public static void remember(ServerLevel from, BlockPos altar) {
        if (!from.dimension().equals(Level.OVERWORLD)) return;
        setOverworldAltar(from, altar);
    }

    /** The temple of the overworld was raised (or its rift was opened): this altar is the gate to Elisar. */
    public static void setOverworldAltar(ServerLevel overworld, BlockPos altar) {
        Links l = Links.get(overworld);
        if (l.set && l.x == altar.getX() && l.y == altar.getY() && l.z == altar.getZ()) return;
        l.x = altar.getX();
        l.y = altar.getY();
        l.z = altar.getZ();
        l.set = true;
        l.setDirty();
    }

    /** Walked into the rift. */
    public static void travel(ServerPlayer p, ServerLevel from, BlockPos altar) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        ServerLevel to;
        BlockPos target;
        if (from.dimension().equals(Level.OVERWORLD)) {
            to = server.getLevel(RiftZone.ELISAR);
            target = to == null ? null : RiftZone.elisarAltar();
        } else {
            to = server.overworld();
            Links l = Links.get(to);
            target = l.set ? new BlockPos(l.x, l.y, l.z) : null;
            if (target == null) {
                target = to.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, to.getSharedSpawnPos());
            }
        }
        if (to == null || target == null) {
            p.displayClientMessage(Component.translatable("riftworld.rift.nowhere"), true);
            return;
        }
        p.displayClientMessage(Component.translatable("riftworld.rift.enter"), true);
        cross(p, to, target);
    }

    private static void cross(ServerPlayer p, ServerLevel to, BlockPos altar) {
        ensureLanding(to, altar);
        if (to.getBlockEntity(altar) instanceof RiftAltarBlockEntity be) be.forceOpen(to, altar);
        BlockPos arr = arrival(altar);
        p.level().playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.0F, 0.7F);
        p.level().sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1.0, p.getZ(), 60, 0.6, 1.0, 0.6, 0.05);
        p.teleportTo(to, arr.getX() + 0.5, arr.getY(), arr.getZ() + 0.5, p.getYRot(), p.getXRot());
        to.playSound(null, arr, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.0F, 1.3F);
        to.sendParticles(ParticleTypes.FLASH, arr.getX() + 0.5, arr.getY() + 1.2, arr.getZ() + 0.5, 3, 0.4, 0.4, 0.4, 0);
        Net.send(p, new RiftFxMsg(altar, RiftAltarBlockEntity.OPEN, 3, 0, 1, 100));
    }

    /** Three blocks next to the altar: on the hexagram, never inside the rift volume. */
    public static BlockPos arrival(BlockPos altar) {
        return altar.offset(3, 0, 0);
    }

    /** Makes sure the traveller lands on solid ground in a hall, even if the twin temple was never generated. */
    private static void ensureLanding(ServerLevel to, BlockPos altar) {
        BlockPos pad = arrival(altar);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos f = pad.offset(dx, -1, dz);
                if (!to.getBlockState(f).isFaceSturdy(to, f, Direction.UP)) {
                    to.setBlockAndUpdate(f, RiftBlocks.RIFT_STONE.get().defaultBlockState());
                }
                for (int dy = 0; dy <= 3; dy++) {
                    BlockPos a = pad.offset(dx, dy, dz);
                    if (!to.isEmptyBlock(a)) to.setBlockAndUpdate(a, Blocks.AIR.defaultBlockState());
                }
            }
        }
        if (to.getBlockState(altar).getBlock() instanceof RiftAltarBlock) return;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos f = altar.offset(dx, -1, dz);
                if (!to.getBlockState(f).isFaceSturdy(to, f, Direction.UP)) {
                    to.setBlockAndUpdate(f, RiftBlocks.RIFT_STONE.get().defaultBlockState());
                }
            }
        }
        if (to.isEmptyBlock(altar)) to.setBlockAndUpdate(altar, RiftBlocks.ALTAR.get().defaultBlockState());
    }

    /** Where the way back leads (the overworld altar, if the ritual ever ran there). */
    @Nullable
    public static BlockPos overworldAltar(MinecraftServer server) {
        ServerLevel ow = server.overworld();
        if (ow == null) return null;
        Links l = Links.get(ow);
        return l.set ? new BlockPos(l.x, l.y, l.z) : null;
    }

    /** Saved data of the overworld: the altar that leads to Elisar. */
    public static final class Links extends SavedData {
        private static final String ID = "riftworld_links";
        private int x, y, z;
        private boolean set;

        public static Links get(ServerLevel overworld) {
            return overworld.getDataStorage().computeIfAbsent(Links::load, Links::new, ID);
        }

        private static Links load(CompoundTag t) {
            Links l = new Links();
            l.x = t.getInt("x");
            l.y = t.getInt("y");
            l.z = t.getInt("z");
            l.set = t.contains("set") && t.getBoolean("set");
            return l;
        }

        @Override
        public CompoundTag save(CompoundTag t) {
            t.putInt("x", x);
            t.putInt("y", y);
            t.putInt("z", z);
            t.putBoolean("set", set);
            return t;
        }
    }
}
