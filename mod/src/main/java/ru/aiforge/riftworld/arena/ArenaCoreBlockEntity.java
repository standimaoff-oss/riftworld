package ru.aiforge.riftworld.arena;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.block.ModelBlock;
import ru.aiforge.riftworld.registry.ArenaBlocks;

/**
 * Throne-hall sequence (no cutscene - the player keeps full control):
 * DORMANT --player enters the hall--> ENTRY (dark; candles light pair by pair from the door to the throne)
 * --> WAITING (heartbeat from the throne) --player comes near the throne--> AWAKENING (roar, the six columns rise)
 * --> FIGHT (columns beam their healing into the boss; break them) --all broken--> CLEARED.
 * Any state resets to DORMANT after the hall has been empty for a minute (candles out, columns sink and re-form).
 * Positions come from the castle template NBT (offsets relative to this block).
 */
public class ArenaCoreBlockEntity extends BlockEntity {
    public enum State { DORMANT, ENTRY, WAITING, AWAKENING, FIGHT, CLEARED, DEFEATED }

    public static final Set<ArenaCoreBlockEntity> LOADED = Collections.newSetFromMap(new WeakHashMap<>());
    static final int CANDLE_DELAY = 30, CANDLE_STEP = 14, COL_STAGGER = 10, RESET_IDLE = 1200;
    static final float HEAL_PER_COLUMN = 1.0F;

    int[] candles = new int[0], sockets = new int[0];
    int[] throne = {0, 2, 0}, hallMin = {-13, -1, -31}, hallMax = {13, 18, 3};
    State state = State.DORMANT;
    long stateTime;
    int lit, idle, lastLeft = -1;
    UUID boss;

    public ArenaCoreBlockEntity(BlockPos pos, BlockState s) {
        super(ArenaBlocks.CORE_BE.get(), pos, s);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) LOADED.add(this);
    }

    @Override
    public void setRemoved() {
        LOADED.remove(this);
        super.setRemoved();
    }

    public State state() {
        return state;
    }

    private BlockPos rel(int[] a, int i) {
        return worldPosition.offset(a[i], a[i + 1], a[i + 2]);
    }

    private Vec3 seat() {
        BlockPos t = rel(throne, 0);
        return new Vec3(t.getX() + 0.5, t.getY() + 1.1, t.getZ() + 0.5);
    }

    void go(State s, long now) {
        state = s;
        stateTime = now;
        setChanged();
    }

    // ------------------------------------------------------------------------------------------------ tick
    void tick(ServerLevel sl) {
        long now = sl.getGameTime(), t = now - stateTime;
        boolean check = now % 10 == 0;
        List<ServerPlayer> ps = check ? players(sl) : List.of();
        if (check) {
            if (state != State.DORMANT && state != State.DEFEATED && ps.isEmpty()) {
                idle += 10;
                if (idle >= RESET_IDLE) {
                    reset(sl);
                    return;
                }
            } else {
                idle = 0;
            }
        }
        switch (state) {
            case DORMANT -> {
                if (!ps.isEmpty()) start(sl, ps);
            }
            case ENTRY -> {
                int pairs = candles.length / 6;
                if (lit < pairs && t >= CANDLE_DELAY + (long) lit * CANDLE_STEP) lightPair(sl, lit++);
                if (lit >= pairs && t >= CANDLE_DELAY + (long) pairs * CANDLE_STEP + 10) {
                    go(State.WAITING, now);
                    BossHooks.stand(sl, this, boss);
                }
            }
            case WAITING -> {
                if (now % 40 == 0) sound(sl, seat(), SoundEvents.WARDEN_HEARTBEAT, 1.6F, 0.6F);
                if (check && nearThrone(ps)) trigger(sl);
            }
            case AWAKENING -> {
                List<BossColumnBlockEntity> cols = columns(sl);
                for (int i = 0; i < cols.size(); ++i) if (t == (long) i * COL_STAGGER + 2) cols.get(i).rise();
                if (t >= (long) cols.size() * COL_STAGGER + BossColumnBlockEntity.RISE_T + 8) {
                    go(State.FIGHT, now);
                    lastLeft = -1;
                }
            }
            case FIGHT -> {
                List<BossColumnBlockEntity> cols = columns(sl);
                int left = 0;
                for (BossColumnBlockEntity c : cols) if (c.standing()) ++left;
                if (check) {
                    Vec3 tg = target(sl);
                    for (BossColumnBlockEntity c : cols) c.setBeam(true, tg.x, tg.y, tg.z);
                    if (left != lastLeft) {
                        if (lastLeft >= 0 && left < lastLeft) {
                            if (left > 0) msg(ps, "riftworld.arena.columns_left", ChatFormatting.GOLD, left);
                            else msg(ps, "riftworld.arena.columns_down", ChatFormatting.GREEN);
                        }
                        lastLeft = left;
                    }
                    if (left == 0) {
                        go(State.CLEARED, now);
                        sound(sl, seat(), SoundEvents.BEACON_DEACTIVATE, 2.0F, 0.6F);
                    }
                }
                if (now % 20 == 0 && left > 0) heal(sl, left);
            }
            case CLEARED -> {
                if (check && boss(sl) == null && boss != null) go(State.DEFEATED, now);
            }
            case DEFEATED -> {
            }
        }
    }

    /** WAITING -> AWAKENING: a player reached the throne or struck the waiting boss. */
    public void trigger(ServerLevel sl) {
        if (state != State.WAITING && state != State.ENTRY) return;
        if (state == State.ENTRY) setLitAll(sl, true);
        long now = sl.getGameTime();
        go(State.AWAKENING, now);
        sound(sl, seat(), SoundEvents.WITHER_SPAWN, 0.6F, 0.5F);
        msg(players(sl), "riftworld.arena.awaken", ChatFormatting.RED);
        boss = BossHooks.awaken(sl, this, boss);
    }

    /** The boss died: columns go dark, the hall stays quiet until /riftarena reset. */
    public void bossDefeated(ServerLevel sl) {
        for (BossColumnBlockEntity c : columns(sl)) if (c.standing()) c.setBeam(false, 0, 0, 0);
        go(State.DEFEATED, sl.getGameTime());
        msg(players(sl), "riftworld.arena.defeated", ChatFormatting.GOLD);
        sound(sl, seat(), SoundEvents.BEACON_DEACTIVATE, 2.0F, 0.5F);
    }

    public AABB hallBox() {
        BlockPos a = rel(hallMin, 0), b = rel(hallMax, 0);
        return new AABB(a.getX() - 2, a.getY() - 2, a.getZ() - 2, b.getX() + 3, b.getY() + 3, b.getZ() + 3);
    }

    /** Begin the entry sequence (also used by /riftarena start). */
    public void start(ServerLevel sl, List<ServerPlayer> ps) {
        setLitAll(sl, false);
        boss = BossHooks.prepare(sl, this);
        lit = 0;
        go(State.ENTRY, sl.getGameTime());
        msg(ps, "riftworld.arena.enter", ChatFormatting.DARK_RED);
        sound(sl, seat(), SoundEvents.WARDEN_HEARTBEAT, 2.0F, 0.5F);
    }

    public void reset(ServerLevel sl) {
        setLitAll(sl, false);
        for (BossColumnBlockEntity c : columns(sl)) c.sink();
        BossHooks.reset(sl, this);
        boss = null;
        lit = 0;
        idle = 0;
        lastLeft = -1;
        go(State.DORMANT, sl.getGameTime());
    }

    // ------------------------------------------------------------------------------------------------ helpers
    public List<ServerPlayer> players(ServerLevel sl) {
        BlockPos a = rel(hallMin, 0), b = rel(hallMax, 0);
        AABB box = new AABB(a.getX(), a.getY(), a.getZ(), b.getX() + 1, b.getY() + 1, b.getZ() + 1);
        return sl.getEntitiesOfClass(ServerPlayer.class, box, p -> !p.isSpectator() && p.isAlive());
    }

    private boolean nearThrone(List<ServerPlayer> ps) {
        Vec3 s = seat();
        for (ServerPlayer p : ps) {
            double dx = p.getX() - s.x, dz = p.getZ() - s.z;
            if (dx * dx + dz * dz < 6.5 * 6.5 && Math.abs(p.getY() - s.y) < 5) return true;
        }
        return false;
    }

    public List<BossColumnBlockEntity> columns(ServerLevel sl) {
        List<BossColumnBlockEntity> out = new ArrayList<>();
        for (int i = 0; i + 2 < sockets.length; i += 3) {
            if (sl.getBlockEntity(rel(sockets, i)) instanceof BossColumnBlockEntity c) out.add(c);
        }
        return out;
    }

    private Vec3 target(ServerLevel sl) {
        LivingEntity b = boss(sl);
        return b != null ? b.position().add(0, b.getBbHeight() * 0.6, 0) : seat();
    }

    private LivingEntity boss(ServerLevel sl) {
        if (boss == null) return null;
        Entity e = sl.getEntity(boss);
        return e instanceof LivingEntity le && le.isAlive() ? le : null;
    }

    private void heal(ServerLevel sl, int columns) {
        LivingEntity b = boss(sl);
        if (b != null && b.getHealth() < b.getMaxHealth()) b.heal(columns * HEAL_PER_COLUMN);
    }

    private void lightPair(ServerLevel sl, int k) {
        for (int j = 0; j < 2; ++j) {
            int i = (2 * k + j) * 3;
            if (i + 2 >= candles.length) break;
            BlockPos p = rel(candles, i);
            BlockState s = sl.getBlockState(p);
            if (s.getBlock() instanceof ModelBlock && s.hasProperty(ModelBlock.POWERED)) {
                sl.setBlock(p, s.setValue(ModelBlock.POWERED, true), 3);
            }
            sl.sendParticles(ParticleTypes.FLAME, p.getX() + 0.5, p.getY() + 1.65, p.getZ() + 0.5, 14, 0.06, 0.06, 0.06, 0.03);
            sl.sendParticles(ParticleTypes.SMOKE, p.getX() + 0.5, p.getY() + 1.8, p.getZ() + 0.5, 4, 0.03, 0.05, 0.03, 0.01);
            sl.playSound(null, p, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.9F, 0.7F);
            sl.playSound(null, p, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.25F, 1.6F);
        }
    }

    private void setLitAll(ServerLevel sl, boolean on) {
        for (int i = 0; i + 2 < candles.length; i += 3) {
            BlockPos p = rel(candles, i);
            BlockState s = sl.getBlockState(p);
            if (s.getBlock() instanceof ModelBlock && s.hasProperty(ModelBlock.POWERED) && s.getValue(ModelBlock.POWERED) != on) {
                sl.setBlock(p, s.setValue(ModelBlock.POWERED, on), 3);
            }
        }
    }

    private static void sound(ServerLevel sl, Vec3 at, SoundEvent ev, float vol, float pitch) {
        sl.playSound(null, at.x, at.y, at.z, ev, SoundSource.HOSTILE, vol, pitch);
    }

    private static void msg(List<ServerPlayer> ps, String key, ChatFormatting col, Object... args) {
        for (ServerPlayer p : ps) p.displayClientMessage(Component.translatable(key, args).withStyle(col), true);
    }

    // ------------------------------------------------------------------------------------------------ persistence
    private static int[] ints(CompoundTag tag, String key, int[] def) {
        if (tag.contains(key, Tag.TAG_INT_ARRAY)) return tag.getIntArray(key);
        if (tag.contains(key, Tag.TAG_LIST)) {
            ListTag l = tag.getList(key, Tag.TAG_INT);
            int[] a = new int[l.size()];
            for (int i = 0; i < a.length; ++i) a[i] = l.getInt(i);
            return a;
        }
        return def;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putIntArray("Candles", candles);
        tag.putIntArray("Sockets", sockets);
        tag.putIntArray("Throne", throne);
        tag.putIntArray("HallMin", hallMin);
        tag.putIntArray("HallMax", hallMax);
        tag.putInt("State", state.ordinal());
        tag.putLong("StateTime", stateTime);
        tag.putInt("Lit", lit);
        if (boss != null) tag.putUUID("Boss", boss);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        candles = ints(tag, "Candles", candles);
        sockets = ints(tag, "Sockets", sockets);
        throne = ints(tag, "Throne", throne);
        hallMin = ints(tag, "HallMin", hallMin);
        hallMax = ints(tag, "HallMax", hallMax);
        int s = tag.getInt("State");
        state = State.values()[Math.max(0, Math.min(s, State.values().length - 1))];
        stateTime = tag.getLong("StateTime");
        lit = tag.getInt("Lit");
        boss = tag.hasUUID("Boss") ? tag.getUUID("Boss") : null;
    }
}
