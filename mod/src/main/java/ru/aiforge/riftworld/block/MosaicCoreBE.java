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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import ru.aiforge.riftworld.registry.ModBlockEntities;
import ru.aiforge.riftworld.registry.TempleBlocks;

/**
 * Rev 4 item 3 ritual: stand on the mosaic, light every ritual candle around it; the mosaic stages up (0->3, glow
 * rising), flashes, and the floor becomes a pool of liquid light - the bridge back to the overworld (walk in).
 */
public class MosaicCoreBE extends BlockEntity {
    private int progress;               // ticks toward the next stage
    private int flash = -1;             // overcharge countdown before the bridge opens
    private boolean open;               // the bridge pool exists
    private static final Map<UUID, Long> TP_CD = new HashMap<>();

    public MosaicCoreBE(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOSAIC_CORE.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag t) {
        super.saveAdditional(t);
        t.putInt("progress", progress);
        t.putInt("flash", flash);
        t.putBoolean("open", open);
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        progress = t.getInt("progress");
        flash = t.contains("flash") ? t.getInt("flash") : -1;
        open = t.getBoolean("open");
    }

    static void tick(Level level, BlockPos pos, BlockState state, MosaicCoreBE be) {
        if (!(level instanceof ServerLevel sl) || sl.getGameTime() % 5 != 0) return;
        if (be.open) {
            teleportStep(sl, pos);
            return;
        }
        if (be.flash >= 0) {                                  // overcharge: the mosaic boils with light
            be.flash -= 5;
            if (sl.getGameTime() % 10 == 0) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 24, 1.8, 0.4, 1.8, 0.02);
                sl.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 30, 1.6, 1.0, 1.6, 1.2);
            }
            if (be.flash <= 0) openBridge(sl, pos, be);
            return;
        }
        if (ritualActive(sl, pos)) {
            be.progress += 5;
            if (be.progress % 30 == 5)                        // rising sigils while it charges
                sl.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 1.4, 0.3, 1.4, 0.8);
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

    /** THE moment: the mosaic floor abruptly turns into liquid light - the bridge between the dimensions. */
    private static void openBridge(ServerLevel sl, BlockPos pos, MosaicCoreBE be) {
        BlockState bridge = TempleBlocks.LIGHT_BRIDGE.get().defaultBlockState();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, 0, -2), pos.offset(2, 0, 2)))
            if (sl.getBlockState(p).getBlock() instanceof MosaicBlock) sl.setBlock(p.immutable(), bridge, 3);
        sl.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 2.0F, 1.0F);
        sl.sendParticles(ParticleTypes.FLASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 3, 1.5, 0.5, 1.5, 0);
        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 120, 2.2, 1.2, 2.2, 0.35);
        be.open = true;
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
