package ru.aiforge.riftworld.block;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import ru.aiforge.riftworld.flight.Flight;
import ru.aiforge.riftworld.flight.PortalRitual;
import ru.aiforge.riftworld.registry.ModBlockEntities;
import ru.aiforge.riftworld.registry.TempleBlocks;

/**
 * v0.29.0: Portal Mosaic Core BlockEntity.
 * <ul>
 *   <li>Scans up to {@code ±10} blocks horizontally and {@code -2..+4} vertically for both {@link RitualCandleBlock}
 *       and vanilla {@link CandleBlock} (matching OvergrownTemplePiece, White City Sanctuary, Great Temple, and
 *       player-built rings at {@code ±6}!).</li>
 *   <li>Progressively illuminates the mosaic stages {@code 0 -> 1 -> 2 -> 3} as candles are lit and
 *       <b>IMMEDIATELY</b> opens the portal the moment the candle circle is lit — zero 40-second stand-still gate!</li>
 * </ul>
 */
public class MosaicCoreBE extends BlockEntity {
    private int progress;               // ticks / cooldown guard
    private int flash = -1;             // short dramatic flash before Elisar return wave starts
    private int wave = -1;              // current Chebyshev ring of the spread (-1 = not running)
    private int waveRest;               // consecutive empty rings (ends the wave)
    private boolean open;               // Elisar return pool open (or Overworld breach active)
    private int overworldCooldown;      // prevents duplicate triggers during the 4s AWAKEN sequence
    private static final int WAVE_MAX = 36;
    private static final int CANDLE_SCAN_XZ = 10;
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
        t.putInt("owCd", overworldCooldown);
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        progress = t.getInt("progress");
        flash = t.contains("flash") ? t.getInt("flash") : -1;
        wave = t.contains("wave") ? t.getInt("wave") : -1;
        waveRest = t.getInt("waveRest");
        open = t.getBoolean("open");
        overworldCooldown = t.getInt("owCd");
    }

    /** Called immediately when any {@link RitualCandleBlock} is lit so portal activation has zero tick lag! */
    public static void notifyCandleLit(ServerLevel sl, BlockPos candlePos, @Nullable ServerPlayer actor) {
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int dx = -CANDLE_SCAN_XZ; dx <= CANDLE_SCAN_XZ; ++dx) {
            for (int dz = -CANDLE_SCAN_XZ; dz <= CANDLE_SCAN_XZ; ++dz) {
                for (int dy = -4; dy <= 2; ++dy) {
                    mp.set(candlePos.getX() + dx, candlePos.getY() + dy, candlePos.getZ() + dz);
                    if (!sl.hasChunk(mp.getX() >> 4, mp.getZ() >> 4)) continue;
                    BlockEntity be = sl.getBlockEntity(mp);
                    if (be instanceof MosaicCoreBE core) {
                        core.evaluateCandlesAndTrigger(sl, mp.immutable(), sl.getBlockState(mp), actor);
                    }
                }
            }
        }
    }

    /** Counts `[totalCandles, litCandles]` around `pos` (supports both `RitualCandleBlock` and vanilla `CandleBlock`). */
    public static int[] countCandles(ServerLevel sl, BlockPos pos) {
        int total = 0, lit = 0;
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int dx = -CANDLE_SCAN_XZ; dx <= CANDLE_SCAN_XZ; ++dx) {
            for (int dz = -CANDLE_SCAN_XZ; dz <= CANDLE_SCAN_XZ; ++dz) {
                if (!sl.hasChunk((pos.getX() + dx) >> 4, (pos.getZ() + dz) >> 4)) continue;
                for (int dy = -2; dy <= 4; ++dy) {
                    mp.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    BlockState s = sl.getBlockState(mp);
                    if (s.getBlock() instanceof RitualCandleBlock) {
                        ++total;
                        if (s.getValue(RitualCandleBlock.LIT)) ++lit;
                    } else if (s.getBlock() instanceof CandleBlock && s.hasProperty(BlockStateProperties.LIT)) {
                        ++total;
                        if (s.getValue(BlockStateProperties.LIT)) ++lit;
                    }
                }
            }
        }
        return new int[]{total, lit};
    }

    private static boolean areCandlesSatisfied(int total, int lit) {
        if (total <= 0 || lit <= 0) return false;
        int required = total >= 8 ? 8 : (total >= 4 ? total : Math.max(1, total));
        return lit >= required;
    }

    public void evaluateCandlesAndTrigger(ServerLevel sl, BlockPos pos, BlockState state, @Nullable ServerPlayer preferredActor) {
        if (!(state.getBlock() instanceof MosaicBlock)) return;
        boolean inElisar = sl.dimension().equals(Flight.ELISAR_DIM);

        if (!inElisar) {
            if (PortalRitual.findNearbyOpenRift(sl, pos.above(), 6.0) != null || overworldCooldown > 0) {
                return;
            }
        } else if (open || wave >= 0 || flash >= 0) {
            return;
        }

        int[] counts = countCandles(sl, pos);
        int total = counts[0], lit = counts[1];
        if (total <= 0) return;

        int curStage = state.getValue(MosaicBlock.STAGE);
        if (areCandlesSatisfied(total, lit)) {
            if (curStage < 3) {
                setStage(sl, pos, 3);
            }
            if (!inElisar) {
                overworldCooldown = 120;
                open = true;
                setChanged();
                PortalRitual.pulseSanctumRunesAndCandles(sl, pos, 10);
                ServerPlayer target = preferredActor;
                if (target == null) {
                    List<ServerPlayer> nearSp = sl.getEntitiesOfClass(ServerPlayer.class,
                            new AABB(pos).inflate(24.0), p -> p.isAlive() && !p.isSpectator());
                    if (!nearSp.isEmpty()) target = nearSp.get(0);
                }
                if (target != null) {
                    if (!PortalRitual.start(target, pos.above(), "observatory_candles")) {
                        PortalRitual.awakenObservatorySanctum(sl, pos);
                    }
                } else {
                    PortalRitual.awakenObservatorySanctum(sl, pos);
                }
            } else {
                flash = 15;
                setChanged();
                sl.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 1.8F, 1.35F);
                sl.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.0F, 0.85F);
            }
        } else {
            int wantStage = lit == 0 ? 0 : (lit * 2 >= total ? 2 : 1);
            if (wantStage != curStage) {
                setStage(sl, pos, wantStage);
                if (wantStage > curStage) {
                    sl.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS,
                            1.2F, 0.85F + 0.18F * wantStage);
                    sl.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.15, pos.getZ() + 0.5,
                            8, 1.1, 0.25, 1.1, 0.02);
                }
            }
        }
    }

    static void tick(Level level, BlockPos pos, BlockState state, MosaicCoreBE be) {
        if (!(level instanceof ServerLevel sl)) return;
        boolean inElisar = sl.dimension().equals(Flight.ELISAR_DIM);

        if (!inElisar) {
            if (be.overworldCooldown > 0) --be.overworldCooldown;
            if (sl.getGameTime() % 10 == 0) {
                boolean hasOpenRift = PortalRitual.findNearbyOpenRift(sl, pos.above(), 6.0) != null;
                if (hasOpenRift) {
                    be.open = true;
                    if (state.getValue(MosaicBlock.STAGE) < 3) {
                        setStage(sl, pos, 3);
                    }
                    return;
                }
                if (be.open && be.overworldCooldown <= 0) {
                    // Rift was closed or despawned; allow re-evaluating if candles are re-lit
                    be.open = false;
                }
                if (!be.open && be.overworldCooldown <= 0) {
                    be.evaluateCandlesAndTrigger(sl, pos, state, null);
                }
            }
            return;
        }

        if (be.open) {
            if (sl.getGameTime() % 5 == 0) teleportStep(sl, pos);
            long gt = sl.getGameTime();
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
        if (be.wave >= 0) {
            if (sl.getGameTime() % 2 == 0) waveStep(sl, pos, be);
            return;
        }
        if (sl.getGameTime() % 5 != 0) return;
        if (be.flash >= 0) {
            be.flash -= 5;
            if (sl.getGameTime() % 10 == 0) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 24, 1.8, 0.4, 1.8, 0.02);
                sl.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 30, 1.6, 1.0, 1.6, 1.2);
            }
            if (be.flash <= 0) {
                be.flash = -1;
                be.wave = 0;
                be.waveRest = 0;
                be.setChanged();
                sl.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.6F, 1.3F);
            }
            return;
        }
        be.evaluateCandlesAndTrigger(sl, pos, state, null);
    }

    private static void setStage(ServerLevel sl, BlockPos pos, int stage) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, 0, -2), pos.offset(2, 0, 2))) {
            BlockState s = sl.getBlockState(p);
            if (s.getBlock() instanceof MosaicBlock && s.getValue(MosaicBlock.STAGE) != stage) {
                sl.setBlock(p.immutable(), s.setValue(MosaicBlock.STAGE, stage), 3);
            }
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
                    if (dy == 0 && s.is(TempleBlocks.LIGHT_BRIDGE.get())) {
                        sl.setBlock(p, front, 3);
                        ++touched;
                    } else if (s.is(TempleBlocks.FLOOR.get())) {
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
            int pts = 10;
            for (int i = 0; i < pts; ++i) {
                double a = i * Math.PI * 2 / pts;
                sl.sendParticles(ParticleTypes.CRIMSON_SPORE, pos.getX() + 0.5 + Math.cos(a) * d, pos.getY() + 1.2, pos.getZ() + 0.5 + Math.sin(a) * d, 4, 0.2, 0.4, 0.2, 0.4);
                sl.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5 + Math.cos(a) * d, pos.getY() + 0.6, pos.getZ() + 0.5 + Math.sin(a) * d, 2, 0.25, 0.1, 0.25, 0.05);
            }
        }
        if (touched == 0) ++be.waveRest;
        else be.waveRest = 0;
        if (be.waveRest >= 4 || d >= WAVE_MAX) {
            openBridge(sl, pos, be);
            return;
        }
        be.wave = d + 1;
        be.setChanged();
    }

    private static void openBridge(ServerLevel sl, BlockPos pos, MosaicCoreBE be) {
        BlockState bridge = TempleBlocks.LIGHT_BRIDGE.get().defaultBlockState();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, 0, -2), pos.offset(2, 0, 2))) {
            if (p.equals(pos)) continue;
            BlockState s = sl.getBlockState(p);
            if (s.getBlock() instanceof MosaicBlock) sl.setBlock(p.immutable(), bridge, 3);
        }
        sl.setBlock(pos, sl.getBlockState(pos).setValue(MosaicBlock.STAGE, 3), 3);
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
