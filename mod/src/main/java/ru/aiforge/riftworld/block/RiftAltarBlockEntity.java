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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import ru.aiforge.riftworld.net.Net;
import ru.aiforge.riftworld.net.RiftFxMsg;
import ru.aiforge.riftworld.registry.ModBlockEntities;
import ru.aiforge.riftworld.world.RiftStasis;
import ru.aiforge.riftworld.world.RiftTravel;
import ru.aiforge.riftworld.world.RiftZone;

/**
 * Rev 5 ritual of the Rift.
 * <ol>
 * <li><b>IDLE</b> - the hexagram is dark. Stand on the sign, light every rift candle around it;</li>
 * <li><b>CHARGE</b> - while a player stands on the sign and all candles burn, the hexagram flares up stage by stage
 * (0 -> 3), the hall darkens, the air slows down (everything except walking) and the space starts to break;</li>
 * <li><b>BREAK</b> - the space shatters: the plates of the sign crack, dust and shards fly, the camera rumbles;</li>
 * <li><b>OPEN</b> - the black rift tears open above the altar, swallows the light of the hall and carries whoever
 * walks into it to the other side (the pale world of Elisar, and back).</li>
 * </ol>
 */
public class RiftAltarBlockEntity extends BlockEntity {
    public static final int IDLE = 0, CHARGE = 1, BREAK = 2, OPEN = 3;

    private static final int STAGE_TICKS = 140;      // 7 s per glow stage
    private static final int BREAK_TICKS = 90;       // the space-shatter moment
    private static final int RIFT_UP = 3;            // the rift floats this many blocks above the altar

    private int state = IDLE;
    private int stage;
    private int progress;
    private int breakTicks;
    private long lastFx;
    private static final Map<UUID, Long> TP_CD = new HashMap<>();

    public RiftAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RIFT_ALTAR.get(), pos, state);
    }

    public int state() {
        return state;
    }

    public int stage() {
        return stage;
    }

    public boolean isOpen() {
        return state == OPEN;
    }

    // ------------------------------------------------------------------ nbt
    @Override
    protected void saveAdditional(CompoundTag t) {
        super.saveAdditional(t);
        t.putInt("state", state);
        t.putInt("stage", stage);
        t.putInt("progress", progress);
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        state = t.getInt("state");
        stage = t.getInt("stage");
        progress = t.getInt("progress");
    }

    /** Placing/removing the altar must not leave a half-open ritual behind. */
    public void shutdown() {
        state = IDLE;
        stage = 0;
        progress = 0;
        setChanged();
    }

    // ------------------------------------------------------------------ tick
    static void tick(Level level, BlockPos pos, BlockState bs, RiftAltarBlockEntity be) {
        if (!(level instanceof ServerLevel sl)) return;
        switch (be.state) {
            case BREAK -> be.tickBreak(sl, pos);
            case OPEN -> be.tickOpen(sl, pos);
            default -> be.tickRitual(sl, pos);
        }
    }

    private void tickRitual(ServerLevel sl, BlockPos pos) {
        boolean ok = ritualActive(sl, pos);
        if (ok) {
            state = CHARGE;
            progress++;
            if (progress % 25 == 0) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                        6 + stage * 5, 6.0, 0.4, 6.0, 0.01);
            }
            if (stage == 0 && progress == 1) {
                sl.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.6F, 0.7F);
            }
            if (progress >= STAGE_TICKS) {
                progress = 0;
                if (stage < 3) {
                    stage++;
                    applyStage(sl, pos);
                    sl.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.8F, 0.6F + 0.15F * stage);
                    sl.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.2F, 0.5F);
                } else if (state != BREAK) {
                    state = BREAK;
                    breakTicks = BREAK_TICKS;
                    sl.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 2.6F, 0.35F);
                    sl.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 2.0F, 1.6F);
                    sl.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.4F, 0.6F);
                    broadcast(sl, pos, 12, 0);
                }
            }
        } else if (state != IDLE || stage > 0 || progress > 0) {
            state = IDLE;
            progress = 0;
            if (stage > 0) {
                stage = 0;
                applyStage(sl, pos);
                sl.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.2F, 0.7F);
            }
        }
        // the hall itself slows down and darkens from the very first spark
        if (state != IDLE) RiftStasis.apply(sl, pos);
        fx(sl, pos, 0);
    }

    private void tickBreak(ServerLevel sl, BlockPos pos) {
        breakTicks--;
        AABB hall = RiftZone.box(pos);
        if (breakTicks % 6 == 0) {
            for (int i = 0; i < 22; ++i) {
                double x = pos.getX() + 0.5 + (sl.random.nextDouble() - 0.5) * 16.0;
                double z = pos.getZ() + 0.5 + (sl.random.nextDouble() - 0.5) * 18.0;
                sl.sendParticles(ParticleTypes.CLOUD, x, pos.getY() + 0.4 + sl.random.nextDouble() * 3.0, z, 1, 0, 0.05, 0, 0.06);
                sl.sendParticles(ParticleTypes.FLASH, x, pos.getY() + 1.0, z, 1, 0, 0, 0, 0);
            }
        }
        if (breakTicks % 10 == 0) {
            broadcast(sl, pos, 10, 2);
            sl.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.6F, 0.4F + 0.02F * (BREAK_TICKS - breakTicks));
        }
        RiftStasis.apply(sl, pos);
        fx(sl, pos, 0);
        for (Player p : sl.getEntitiesOfClass(Player.class, hall, p -> p.isAlive())) {
            p.displayClientMessage(RiftAltarBlock.hint(true, 0, 0, false), true);
        }
        if (breakTicks <= 0) open(sl, pos);
    }

    private void tickOpen(ServerLevel sl, BlockPos pos) {
        RiftStasis.apply(sl, pos);
        fx(sl, pos, 0);
        travelStep(sl, pos);
    }

    /** THE moment: the sign is torn apart and the rift hangs in the air. */
    private void open(ServerLevel sl, BlockPos pos) {
        state = OPEN;
        stage = 3;
        setBlock(sl, pos, true, 3);
        sl.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 2.0F, 0.8F);
        sl.playSound(null, pos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.BLOCKS, 1.6F, 0.7F);
        sl.sendParticles(ParticleTypes.FLASH, pos.getX() + 0.5, pos.getY() + RIFT_UP, pos.getZ() + 0.5, 4, 1.0, 0.6, 1.0, 0);
        sl.sendParticles(ParticleTypes.SQUID_INK, pos.getX() + 0.5, pos.getY() + RIFT_UP, pos.getZ() + 0.5, 160, 2.4, 2.4, 2.4, 0.02);
        broadcast(sl, pos, 14, 1);
        RiftTravel.remember(sl, pos);
        setChanged();
    }

    /** Opens the altar on the other side (the twin temple) so the traveller can walk back. */
    public void forceOpen(ServerLevel sl, BlockPos pos) {
        state = OPEN;
        stage = 3;
        setBlock(sl, pos, true, 3);
        setChanged();
    }

    // ------------------------------------------------------------------ ritual helpers
    /** A player stands on the sign and every rift candle around it is lit. */
    private static boolean ritualActive(ServerLevel sl, BlockPos pos) {
        List<Player> on = sl.getEntitiesOfClass(Player.class, RiftZone.signBox(pos), p -> p.isAlive() && !p.isSpectator());
        if (on.isEmpty()) return false;
        int total = 0, lit = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-RiftZone.CANDLE_R, -3, -RiftZone.CANDLE_R),
                pos.offset(RiftZone.CANDLE_R, 3, RiftZone.CANDLE_R))) {
            BlockState s = sl.getBlockState(p);
            if (s.getBlock() instanceof RitualCandleBlock) {
                total++;
                if (s.getValue(RitualCandleBlock.LIT)) lit++;
            }
        }
        return total >= 6 && lit == total;
    }

    private static int[] candleCount(ServerLevel sl, BlockPos pos) {
        int total = 0, lit = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-RiftZone.CANDLE_R, -3, -RiftZone.CANDLE_R),
                pos.offset(RiftZone.CANDLE_R, 3, RiftZone.CANDLE_R))) {
            BlockState s = sl.getBlockState(p);
            if (s.getBlock() instanceof RitualCandleBlock) {
                total++;
                if (s.getValue(RitualCandleBlock.LIT)) lit++;
            }
        }
        return new int[]{lit, total};
    }

    private void applyStage(ServerLevel sl, BlockPos pos) {
        setBlock(sl, pos, false, stage);
        setChanged();
    }

    private void setBlock(ServerLevel sl, BlockPos pos, boolean open, int st) {
        BlockState s = sl.getBlockState(pos);
        if (s.getBlock() instanceof RiftAltarBlock) {
            sl.setBlock(pos, s.setValue(RiftAltarBlock.STAGE, Math.min(3, st)).setValue(RiftAltarBlock.OPEN, open), 3);
        }
    }

    // ------------------------------------------------------------------ travel
    private static void travelStep(ServerLevel sl, BlockPos pos) {
        AABB rift = RiftZone.riftBox(pos);
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, rift.inflate(0.6))) {
            if (p.isSpectator()) continue;
            Long last = TP_CD.get(p.getUUID());
            if (last != null && sl.getGameTime() - last < 120) continue;
            TP_CD.put(p.getUUID(), sl.getGameTime());
            RiftTravel.travel(p, sl, pos);
        }
    }

    // ------------------------------------------------------------------ fx
    private void fx(ServerLevel sl, BlockPos pos, int extra) {
        long now = sl.getGameTime();
        if (now - lastFx < 20) return;
        lastFx = now;
        broadcast(sl, pos, 0, extra);
    }

    private void broadcast(ServerLevel sl, BlockPos pos, int shake, int flash) {
        int size = state == OPEN ? 100 : state == BREAK ? (int) (100 * (1.0F - breakTicks / (float) BREAK_TICKS)) : 0;
        RiftFxMsg msg = new RiftFxMsg(pos, state, stage, shake, flash, size);
        for (ServerPlayer p : sl.players()) {
            if (p.level() == sl && p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5) < 128.0 * 128.0) {
                Net.send(p, msg);
            }
        }
    }

    /** Right click on the altar: how far is the ritual? */
    public void report(Player player) {
        if (!(level instanceof ServerLevel sl)) return;
        int[] c = candleCount(sl, pos);
        boolean all = c[1] > 0 && c[0] == c[1];
        player.displayClientMessage(RiftAltarBlock.hint(all, c[0], c[1], state == OPEN), false);
    }
}
