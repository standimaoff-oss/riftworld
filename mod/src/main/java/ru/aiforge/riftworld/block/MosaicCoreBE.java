package ru.aiforge.riftworld.block;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import ru.aiforge.riftworld.registry.ModBlockEntities;
import ru.aiforge.riftworld.registry.TempleBlocks;

/**
 * Part 7 ritual: stand on the mosaic, light every ritual candle around it; the core charges (stages 0->3), flashes -
 * and then the RADIANT MOSAIC SPREADS: ring after ring the whole temple floor flips to glowing sigil mosaic until it
 * meets the sanctum walls. At the centre a pool of liquid light stays - the bridge back to the overworld.
 * (Part 3 shipped a broken pool: the core block deleted its own block entity, so the bridge never teleported - the
 * core is now kept as the rune island inside the light.)
 */
public class MosaicCoreBE extends BlockEntity {
    private int progress;               // ticks toward the next stage
    private int flash = -1;             // overcharge countdown before the wave starts
    private int wave = -1;              // current Chebyshev ring of the spread (-1 = not running)
    private int waveRest;               // consecutive empty rings (ends the wave)
    private boolean open;               // the bridge pool exists
    private static final int WAVE_MAX = 36;
    private static final Map<UUID, Long> TP_CD = new HashMap<>();

    public MosaicCoreBE(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOSAIC_CORE.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag t) {
        super.saveAdditional(t);
        t.putInt("progress", progress);
        t.putInt("flash", flash);
        t.putInt("wave", wave);
        t.putInt("waveRest", waveRest);
        t.putBoolean("open", open);
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        progress = t.getInt("progress");
        flash = t.contains("flash") ? t.getInt("flash") : -1;
        wave = t.contains("wave") ? t.getInt("wave") : -1;
        waveRest = t.getInt("waveRest");
        open = t.getBoolean("open");
    }

    static void tick(Level level, BlockPos pos, BlockState state, MosaicCoreBE be) {
        if (!(level instanceof ServerLevel sl)) return;
        if (be.open) {
            if (sl.getGameTime() % 5 == 0) teleportStep(sl, pos);
            long gt = sl.getGameTime();                         // living sanctum: shimmer, wisps, a chime, a shaft of light
            if (gt % 27 == 13) {
                double rx = pos.getX() + 0.5 + sl.random.nextGaussian() * 6.0, rz = pos.getZ() + 0.5 + sl.random.nextGaussian() * 6.0;
                sl.sendParticles(ParticleTypes.END_ROD, rx, pos.getY() + 0.4, rz, 3, 0.5, 0.25, 0.5, 0.01);
                if (sl.random.nextInt(3) == 0)
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, rx, pos.getY() + 0.9, rz, 2, 0.3, 0.15, 0.3, 0.005);
            }
            if (gt % 140 == 70)
                sl.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 6.5, pos.getZ() + 0.5, 30, 0.35, 4.5, 0.35, 0.01);
            if (gt % 740 == 370)
                sl.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.4F, 0.9F + sl.random.nextFloat() * 0.2F);
            return;
        }
        if (be.wave >= 0) {                                   // THE SPREAD: one glow ring every 2 ticks
            if (sl.getGameTime() % 2 == 0) waveStep(sl, pos, be);
            return;
        }
        if (sl.getGameTime() % 5 != 0) return;
        if (be.flash >= 0) {                                  // overcharge: the mosaic boils with light
            be.flash -= 5;
            if (sl.getGameTime() % 10 == 0) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 24, 1.8, 0.4, 1.8, 0.02);
                sl.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 30, 1.6, 1.0, 1.6, 1.2);
            }
            if (be.flash <= 0) {                              // GO: the floor starts to crawl with light
                be.flash = -1;
                be.wave = 0;
                be.waveRest = 0;
                be.setChanged();
                sl.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.6F, 1.3F);
            }
            return;
        }
        if (ritualActive(sl, pos)) {
            be.progress += 5;
            if (be.progress % 30 == 5)                        // rising sigils while it charges
                sl.sendParticles(ParticleTypes.CRIMSON_SPORE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 1.4, 0.3, 1.4, 0.8);
            if (be.progress >= 200) {
                be.progress = 0;
                int stage = state.getValue(MosaicBlock.STAGE);
                if (stage < 3) {
                    setStage(sl, pos, stage + 1);
                    sl.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.6F, 0.8F + 0.2F * stage);
                } else {                                      // fully charged -> the flash
                    be.flash = 60;
                    sl.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 2.0F, 1.4F);
                    sl.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.0F, 0.6F);
                }
            }
        } else if (be.progress > 0 || state.getValue(MosaicBlock.STAGE) > 0) {
            be.progress = 0;                                  // ritual broken: the glow dies down
            setStage(sl, pos, 0);
            sl.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
    }

    /** A player stands on the mosaic ring and every ritual candle in the sanctum is lit. */
    private static boolean ritualActive(ServerLevel sl, BlockPos pos) {
        List<Player> on = sl.getEntitiesOfClass(Player.class, new AABB(pos.getX() - 2, pos.getY() + 0.2, pos.getZ() - 2,
                pos.getX() + 3, pos.getY() + 2.5, pos.getZ() + 3), p -> p.isAlive() && !p.isSpectator());
        if (on.isEmpty()) return false;
        int total = 0, lit = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-8, -1, -8), pos.offset(8, 3, 8))) {
            BlockState s = sl.getBlockState(p);
            if (s.getBlock() instanceof RitualCandleBlock) {
                total++;
                if (s.getValue(RitualCandleBlock.LIT)) lit++;
            }
        }
        return total >= 6 && lit == total && anyOnGround(on);
    }

    private static boolean anyOnGround(List<Player> ps) {
        for (Player p : ps)
            if (p.onGround()) return true;
        return false;
    }

    private static void setStage(ServerLevel sl, BlockPos pos, int stage) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, 0, -2), pos.offset(2, 0, 2))) {
            BlockState s = sl.getBlockState(p);
            if (s.getBlock() instanceof MosaicBlock) sl.setBlock(p, s.setValue(MosaicBlock.STAGE, stage), 3);
        }
    }

    /**
     * One ring of the spread at Chebyshev distance d: replaces temple floor (and gold inlay is left - it is the
     * sanctum's goldwork) with glowing mosaic. The pulse front burns at stage 3, one ring behind it settles to stage 2.
     * Floor height allows < y core..core+2 > so the raised apse glows too.
     */
    private static void waveStep(ServerLevel sl, BlockPos pos, MosaicCoreBE be) {
        BlockState front = TempleBlocks.MOSAIC.get().defaultBlockState().setValue(MosaicBlock.STAGE, 3);
        BlockState settled = TempleBlocks.MOSAIC.get().defaultBlockState().setValue(MosaicBlock.STAGE, 2);
        BlockState bridge = TempleBlocks.LIGHT_BRIDGE.get().defaultBlockState();
        int d = be.wave;
        int touched = 0;
        for (int dx = -d; dx <= d; ++dx) {
            for (int dz = -d; dz <= d; ++dz) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != d) continue;
                for (int dy = 0; dy <= 2; ++dy) {
                    BlockPos p = pos.offset(dx, dy, dz);
                    BlockState s = sl.getBlockState(p);
                    if (dy == 0 && s.is(TempleBlocks.LIGHT_BRIDGE.get())) {
                        sl.setBlock(p, front, 3);             // re-ignite the old bridge once the ring passes it
                        ++touched;
                    } else if (s.is(TempleBlocks.FLOOR.get())) {
                        sl.setBlock(p, front, 3);
                        ++touched;
                    } else if (s.is(TempleBlocks.GOLD_RUNE.get()) && !s.getValue(GoldRuneBlock.LIT)) {
                        sl.setBlock(p, s.setValue(GoldRuneBlock.LIT, true), 3);      // the wave ignites the goldwork
                        sl.sendParticles(ParticleTypes.END_ROD, p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.05);
                    }
                }
            }
        }
        // one ring behind the front: the glow settles to its steady stage
        if (d >= 4) {
            int b = d - 3;
            for (int dx = -b; dx <= b; ++dx)
                for (int dz = -b; dz <= b; ++dz) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != b) continue;
                    for (int dy = 0; dy <= 2; ++dy) {                  // Part 10: the raised apse floor stops flashing too
                        BlockPos p = pos.offset(dx, dy, dz);
                        BlockState s = sl.getBlockState(p);
                        if (s.is(TempleBlocks.MOSAIC.get()) && s.getValue(MosaicBlock.STAGE) == 3) sl.setBlock(p, settled, 3);
                    }
                }
        }
        if ((d & 3) == 0) {                                   // the wave hums as it crawls
            sl.playSound(null, pos.offset(Math.max(-8, -d), 0, 0), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.2F, 0.8F + d * 0.02F);
            int pts = 10;
            for (int i = 0; i < pts; ++i) {
                double a = i * Math.PI * 2 / pts;
                sl.sendParticles(ParticleTypes.CRIMSON_SPORE, pos.getX() + 0.5 + Math.cos(a) * d, pos.getY() + 1.2, pos.getZ() + 0.5 + Math.sin(a) * d, 4, 0.2, 0.4, 0.2, 0.4);
                sl.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5 + Math.cos(a) * d, pos.getY() + 0.6, pos.getZ() + 0.5 + Math.sin(a) * d, 2, 0.25, 0.1, 0.25, 0.05);
            }
        }
        // state machine: count quiet rings; after the walls stop it, crown the pool of light
        if (touched == 0) ++be.waveRest;
        else be.waveRest = 0;
        if (be.waveRest >= 4 || d >= WAVE_MAX) {
            openBridge(sl, pos, be);
            return;
        }
        be.wave = d + 1;
        be.setChanged();
    }

    /** The crown: the sanctum's heart becomes a pool of liquid light around the rune island (the core STAYS). */
    private static void openBridge(ServerLevel sl, BlockPos pos, MosaicCoreBE be) {
        BlockState bridge = TempleBlocks.LIGHT_BRIDGE.get().defaultBlockState();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, 0, -2), pos.offset(2, 0, 2))) {
            if (p.equals(pos)) continue;                      // keep the core alive - this is what drives the teleport
            BlockState s = sl.getBlockState(p);
            if (s.getBlock() instanceof MosaicBlock) sl.setBlock(p.immutable(), bridge, 3);
        }
        sl.setBlock(pos, sl.getBlockState(pos).setValue(MosaicBlock.STAGE, 3), 3);
        // final calm: EVERY flashing square left by the wave (apse step, the last rings near the walls) settles down
        BlockState calm = TempleBlocks.MOSAIC.get().defaultBlockState().setValue(MosaicBlock.STAGE, 2);
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-WAVE_MAX, 0, -WAVE_MAX), pos.offset(WAVE_MAX, 2, WAVE_MAX))) {
            BlockState s = sl.getBlockState(p);
            if (s.is(TempleBlocks.MOSAIC.get()) && s.getValue(MosaicBlock.STAGE) == 3) sl.setBlock(p.immutable(), calm, 3);
        }
        sl.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 2.0F, 1.0F);
        sl.sendParticles(ParticleTypes.FLASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 3, 1.5, 0.5, 1.5, 0);
        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 120, 2.2, 1.2, 2.2, 0.35);
        be.open = true;
        be.wave = -1;
        be.flash = -1;
        be.setChanged();
    }

    private static void teleportStep(ServerLevel sl, BlockPos pos) {
        AABB pool = new AABB(pos.getX() - 2, pos.getY() + 0.05, pos.getZ() - 2, pos.getX() + 3, pos.getY() + 0.75, pos.getZ() + 3);
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, pool.inflate(0.2))) {
            if (p.isSpectator() || !p.onGround()) continue;
            Long last = TP_CD.get(p.getUUID());
            if (last != null && sl.getGameTime() - last < 100) continue;
            TP_CD.put(p.getUUID(), sl.getGameTime());
            ServerLevel ow = sl.getServer().overworld();
            BlockPos spawn = ow.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                    ow.getSharedSpawnPos());
            sl.playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.8F, 1.2F);
            p.teleportTo(ow, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, p.getYRot(), p.getXRot());
        }
    }
}
