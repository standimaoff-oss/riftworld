package ru.aiforge.riftworld.block;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import ru.aiforge.riftworld.registry.ModBlockEntities;
import ru.aiforge.riftworld.registry.TempleBlocks;
import ru.aiforge.riftworld.world.RiftTravel;

/**
 * РИТУАЛ ВЕЛИКОГО ХРАМА (Great Temple Ritual of the Rift):
 * 1. Player stands on the ritual circle and lights all 8 ritual candles around it.
 * 2. The ritual charges through stages (0 -> 1 -> 2 -> 3) with rising soul flames, dark void spores and beacon resonance.
 * 3. BREAK phase: Space fractures with the sound of breaking glass and thunder, camera shakes, reality tears open.
 * 4. OPEN: ЧЕРНЫЙ ПРОЛОМ ПРОСТРАНСТВА tears open inside the ceremonial portal arch behind the altar!
 * 5. Stepping into the black rift (or the ritual circle in front of it) teleports the player across dimensions
 *    to the ancient ruined temple in the Overworld!
 */
public class MosaicCoreBE extends BlockEntity {
    private int progress;               // ticks toward the next stage
    private int flash = -1;             // overcharge countdown before space breaks
    private int wave = -1;              // spread wave
    private int waveRest;
    private boolean open;               // the black rift portal is open
    private static final int WAVE_MAX = 36;
    private static final Map<UUID, Long> TP_CD = new HashMap<>();

    public MosaicCoreBE(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOSAIC_CORE.get(), pos, state);
    }

    public boolean isOpen() {
        return open;
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

        // When the rift is open: living sanctum atmosphere & teleportation
        if (be.open) {
            if (sl.getGameTime() % 5 == 0) teleportStep(sl, pos);
            long gt = sl.getGameTime();
            if (gt % 20 == 0) {
                // Billows of black void ink and dimensional distortion rising around the opened rift
                sl.sendParticles(ParticleTypes.SQUID_INK, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() - 5.0, 8, 1.2, 1.0, 0.4, 0.02);
                sl.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() - 5.0, 6, 0.8, 0.8, 0.3, 0.05);
            }
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

        // The wave spread when charging
        if (be.wave >= 0) {
            if (sl.getGameTime() % 2 == 0) waveStep(sl, pos, be);
            return;
        }

        if (sl.getGameTime() % 5 != 0) return;

        // Space shatter / BREAK phase: reality fractures before the black rift tears open
        if (be.flash >= 0) {
            be.flash -= 5;
            if (sl.getGameTime() % 10 == 0) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 24, 1.8, 0.4, 1.8, 0.02);
                sl.sendParticles(ParticleTypes.SQUID_INK, pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() - 5.0, 30, 1.5, 1.5, 0.5, 0.05);
                sl.sendParticles(ParticleTypes.FLASH, pos.getX() + 0.5, pos.getY() + 2.5, pos.getZ() - 5.0, 2, 0.5, 0.5, 0.5, 0);
            }
            if (be.flash <= 0) {
                // THE RIFT TEARS OPEN!
                be.flash = -1;
                be.wave = 0;
                be.waveRest = 0;
                be.setChanged();
                sl.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 2.5F, 0.4F);
                sl.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 2.0F, 1.4F);
                sl.playSound(null, pos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.BLOCKS, 1.8F, 0.7F);
                openRift(sl, pos, be);
            }
            return;
        }

        // Charging the ritual
        if (ritualActive(sl, pos)) {
            be.progress += 5;
            if (be.progress % 25 == 5) {
                sl.sendParticles(ParticleTypes.CRIMSON_SPORE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 1.4, 0.3, 1.4, 0.8);
                sl.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 1.2, 0.4, 1.2, 0.1);
            }
            if (be.progress >= 120) {
                be.progress = 0;
                int stage = state.getValue(MosaicBlock.STAGE);
                if (stage < 3) {
                    setStage(sl, pos, stage + 1);
                    sl.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.6F, 0.8F + 0.2F * stage);
                    sl.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.8F, 0.7F + 0.15F * stage);
                } else {
                    // Fully charged -> triggers the space rupture!
                    be.flash = 60;
                    sl.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.4F, 0.6F);
                    sl.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 2.0F, 0.5F);
                }
            }
        } else if (be.progress > 0 || state.getValue(MosaicBlock.STAGE) > 0) {
            be.progress = 0;
            setStage(sl, pos, 0);
            sl.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
    }

    /** A player stands on the mosaic circle and every ritual candle in the sanctum is lit. */
    public static boolean ritualActive(ServerLevel sl, BlockPos pos) {
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

    private static void waveStep(ServerLevel sl, BlockPos pos, MosaicCoreBE be) {
        BlockState front = TempleBlocks.MOSAIC.get().defaultBlockState().setValue(MosaicBlock.STAGE, 3);
        BlockState settled = TempleBlocks.MOSAIC.get().defaultBlockState().setValue(MosaicBlock.STAGE, 2);
        int d = be.wave;
        int touched = 0;
        for (int dx = -d; dx <= d; ++dx) {
            for (int dz = -d; dz <= d; ++dz) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != d) continue;
                for (int dy = 0; dy <= 2; ++dy) {
                    BlockPos p = pos.offset(dx, dy, dz);
                    BlockState s = sl.getBlockState(p);
                    if (s.is(TempleBlocks.FLOOR.get())) {
                        sl.setBlock(p, front, 3);
                        ++touched;
                    } else if (s.is(TempleBlocks.GOLD_RUNE.get()) && !s.getValue(GoldRuneBlock.LIT)) {
                        sl.setBlock(p, s.setValue(GoldRuneBlock.LIT, true), 3);
                        sl.sendParticles(ParticleTypes.END_ROD, p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.05);
                    }
                }
            }
        }
        if (d >= 4) {
            int b = d - 3;
            for (int dx = -b; dx <= b; ++dx)
                for (int dz = -b; dz <= b; ++dz) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != b) continue;
                    for (int dy = 0; dy <= 2; ++dy) {
                        BlockPos p = pos.offset(dx, dy, dz);
                        BlockState s = sl.getBlockState(p);
                        if (s.is(TempleBlocks.MOSAIC.get()) && s.getValue(MosaicBlock.STAGE) == 3) sl.setBlock(p, settled, 3);
                    }
                }
        }
        if ((d & 3) == 0) {
            sl.playSound(null, pos.offset(Math.max(-8, -d), 0, 0), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.2F, 0.8F + d * 0.02F);
        }
        if (touched == 0) ++be.waveRest;
        else be.waveRest = 0;
        if (be.waveRest >= 4 || d >= WAVE_MAX) {
            be.wave = -1;
            be.setChanged();
            return;
        }
        be.wave = d + 1;
        be.setChanged();
    }

    /** The culmination: the Black Rift tears open in the ceremonial archway behind the altar! */
    private static void openRift(ServerLevel sl, BlockPos pos, MosaicCoreBE be) {
        // 1. Fill the 3x3 ceremonial portal opening in the arch behind the altar with the Black Rift (DemonGateBlock)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 1; dy <= 3; dy++) {
                BlockPos archPos = pos.offset(dx, dy + 1, -5);
                sl.setBlock(archPos, TempleBlocks.DEMON_GATE.get().defaultBlockState(), 3);
            }
        }

        // 2. The core and ritual floor settle into radiant state
        sl.setBlock(pos, sl.getBlockState(pos).setValue(MosaicBlock.STAGE, 3), 3);
        BlockState calm = TempleBlocks.MOSAIC.get().defaultBlockState().setValue(MosaicBlock.STAGE, 2);
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-WAVE_MAX, 0, -WAVE_MAX), pos.offset(WAVE_MAX, 2, WAVE_MAX))) {
            BlockState s = sl.getBlockState(p);
            if (s.is(TempleBlocks.MOSAIC.get()) && s.getValue(MosaicBlock.STAGE) == 3) sl.setBlock(p.immutable(), calm, 3);
        }

        // 3. Dimensional shockwave
        sl.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 2.0F, 0.8F);
        sl.playSound(null, pos, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS, 1.5F, 0.7F);
        sl.sendParticles(ParticleTypes.FLASH, pos.getX() + 0.5, pos.getY() + 3.0, pos.getZ() - 5.0, 4, 1.2, 1.2, 0.5, 0);
        sl.sendParticles(ParticleTypes.SQUID_INK, pos.getX() + 0.5, pos.getY() + 3.0, pos.getZ() - 5.0, 120, 2.0, 2.0, 0.8, 0.04);
        sl.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 3.0, pos.getZ() - 5.0, 60, 1.5, 1.5, 0.5, 0.1);

        // Darkness effect for nearby players as light is swallowed
        AABB hall = new AABB(pos).inflate(35, 20, 35);
        for (Player p : sl.getEntitiesOfClass(Player.class, hall)) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 70, 0, false, false));
        }

        be.open = true;
        be.wave = -1;
        be.flash = -1;
        be.setChanged();
    }

    private static void teleportStep(ServerLevel sl, BlockPos pos) {
        // Pool area covering the ritual platform in front of the altar & archway
        AABB pool = new AABB(pos.getX() - 2, pos.getY() + 0.05, pos.getZ() - 5, pos.getX() + 3, pos.getY() + 3.5, pos.getZ() + 3);
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, pool.inflate(0.3))) {
            if (p.isSpectator()) continue;
            Long last = TP_CD.get(p.getUUID());
            if (last != null && sl.getGameTime() - last < 100) continue;
            TP_CD.put(p.getUUID(), sl.getGameTime());
            RiftTravel.travelToOverworld(p, sl);
        }
    }

    /** Right click feedback for players. */
    public void report(Player player) {
        if (!(level instanceof ServerLevel sl)) return;
        if (open) {
            player.displayClientMessage(Component.translatable("riftworld.ritual.open"), true);
            return;
        }
        BlockPos pos = getBlockPos();
        int total = 0, lit = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-8, -1, -8), pos.offset(8, 3, 8))) {
            BlockState s = sl.getBlockState(p);
            if (s.getBlock() instanceof RitualCandleBlock) {
                total++;
                if (s.getValue(RitualCandleBlock.LIT)) lit++;
            }
        }
        if (lit < total) {
            player.displayClientMessage(Component.translatable("riftworld.ritual.candles", lit, total), true);
        } else {
            player.displayClientMessage(Component.translatable("riftworld.ritual.stand"), true);
        }
    }
}
