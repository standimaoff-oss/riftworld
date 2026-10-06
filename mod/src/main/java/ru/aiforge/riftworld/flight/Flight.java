package ru.aiforge.riftworld.flight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.block.LaunchCoreBlockEntity;
import ru.aiforge.riftworld.net.FlightMsg;
import ru.aiforge.riftworld.net.Net;
import ru.aiforge.riftworld.world.CityPlan;

/**
 * The 15-second ride from the surviving overworld launch lab to the Great Temple of the Crossing on Elisar:
 * <ol>
 * <li>LIFT (2 s): the source capsule lifts off its floor on the multicolour laser;</li>
 * <li>TRANSIT (10 s): the capsule hangs in the void transit dimension while the client draws the journey;</li>
 * <li>ARRIVE (3 s): the player descends into a brief temple arrival sigil on the front apron; the destination has no capsule.</li>
 * </ol>
 * While flying the player cannot move, break, use, attack or be hurt. The server owns the timeline, the client mirrors it.
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID)
public final class Flight {
    public static final int NONE = 0, LIFT = 1, TRANSIT = 2, ARRIVE = 3;
    public static final int LIFT_T = LaunchCoreBlockEntity.LIFT_T, TRANSIT_T = 200, ARRIVE_T = 60;
    public static final int TOTAL = LIFT_T + TRANSIT_T + ARRIVE_T;          // 300 ticks = 15 s
    public static final ResourceKey<Level> TRANSIT_DIM = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(RiftWorld.MODID, "transit"));
    public static final ResourceKey<Level> ELISAR_DIM = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(RiftWorld.MODID, "elisar"));
    /** Where the capsule floats in the transit dimension. */
    public static final BlockPos TRANSIT_CORE = new BlockPos(0, 100, 0);
    /** Set by the client while its own player is flying (events fire on both sides). */
    public static volatile boolean clientLocked;

    private static final Map<UUID, State> ACTIVE = new HashMap<>();

    private static final class State {
        int tick;
        BlockPos from, to;
        final List<ChunkPos> scan = new ArrayList<>();
        int scanI;
    }

    private Flight() {}

    public static boolean locked(Player p) {
        return p.level().isClientSide ? clientLocked : ACTIVE.containsKey(p.getUUID());
    }

    public static int phaseOf(int tick) {
        return tick < LIFT_T ? LIFT : tick < LIFT_T + TRANSIT_T ? TRANSIT : ARRIVE;
    }

    public static int phaseStart(int phase) {
        return phase == LIFT ? 0 : phase == TRANSIT ? LIFT_T : LIFT_T + TRANSIT_T;
    }

    /** Capsule height above its landed position for the overworld lift (t in ticks since phase start). */
    public static float liftOffset(float t) {
        return LaunchCoreBlockEntity.LIFT_H * LaunchCoreBlockEntity.ease(t / LIFT_T);
    }

    /** The temple has no arrival capsule or shaft: its sigil is ground-level and the player stays on the apron. */
    public static float arriveOffset(float t) {
        return 0;
    }

    // ------------------------------------------------------------------------------------------------ start
    /** Safe center of the temple's south apron: template-local (31, 68), one block above its custom floor. */
    private static BlockPos templeLanding(CityPlan.Plot temple) {
        return new BlockPos(temple.x0() + 31, temple.h(), temple.z0() + 68);
    }

    private static BlockPos templeLanding() {
        for (CityPlan.Plot pl : CityPlan.get().plots)
            if (pl.template().equals("temple_great")) return templeLanding(pl);
        return new BlockPos(0, 104, 33);
    }

    public static boolean start(ServerPlayer p, LaunchCoreBlockEntity core) {
        if (ACTIVE.containsKey(p.getUUID())) return false;
        CityPlan.Plot temple = null;
        for (CityPlan.Plot pl : CityPlan.get().plots)
            if (pl.template().equals("temple_great")) { temple = pl; break; }
        if (temple == null || !core.startFlight()) return false;

        State s = new State();
        s.from = core.getBlockPos();
        s.to = templeLanding(temple);
        int cx0 = (temple.x0() - 8) >> 4, cz0 = (temple.z0() - 8) >> 4;
        int cx1 = (temple.x0() + temple.w() + 8) >> 4, cz1 = (temple.z0() + temple.d() + 8) >> 4;
        for (int cx = cx0; cx <= cx1; ++cx)
            for (int cz = cz0; cz <= cz1; ++cz) s.scan.add(new ChunkPos(cx, cz));
        double mx = temple.x0() + temple.w() / 2.0, mz = temple.z0() + temple.d() / 2.0;
        s.scan.sort(Comparator.comparingDouble(c -> Math.hypot(c.getMiddleBlockX() - mx, c.getMiddleBlockZ() - mz)));
        ACTIVE.put(p.getUUID(), s);
        lock(p);
        p.level().playSound(null, s.from, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 3.0F, 0.7F);
        p.level().playSound(null, s.from, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 2.0F, 0.6F);
        send(p, s);
        return true;
    }

    private static void send(ServerPlayer p, State s) {
        int ph = phaseOf(s.tick);
        BlockPos pos = ph == LIFT ? s.from : ph == TRANSIT ? TRANSIT_CORE : (s.to != null ? s.to : BlockPos.ZERO);
        Net.send(p, new FlightMsg(ph, pos, s.tick - phaseStart(ph)));
    }

    private static void lock(ServerPlayer p) {
        p.getAbilities().mayfly = true;
        p.getAbilities().flying = true;
        p.getAbilities().invulnerable = true;
        p.onUpdateAbilities();
        p.setNoGravity(true);
        p.setDeltaMovement(Vec3.ZERO);
        p.fallDistance = 0;
    }

    private static void unlock(ServerPlayer p) {
        p.gameMode.getGameModeForPlayer().updatePlayerAbilities(p.getAbilities());
        p.onUpdateAbilities();
        p.setNoGravity(false);
        p.fallDistance = 0;
    }

    // ------------------------------------------------------------------------------------------------ timeline
    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) return;
        MinecraftServer server = e.getServer();
        Iterator<Map.Entry<UUID, State>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, State> en = it.next();
            State s = en.getValue();
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            if (p == null) {
                it.remove();
                continue;
            }
            ++s.tick;
            ServerLevel elisar = server.getLevel(ELISAR_DIM), transit = server.getLevel(TRANSIT_DIM);
            if (elisar == null || transit == null) {
                unlock(p);
                it.remove();
                Net.send(p, new FlightMsg(NONE, BlockPos.ZERO, 0));
                continue;
            }
            // Pre-generate the temple footprint behind the transition flash; add a few region tickets per tick so
            // the sizeable undercroft never forces one synchronous world-generation spike at arrival.
            if (s.tick == LIFT_T + 1) {
                int initial = Math.min(4, s.scan.size());
                for (int k = 0; k < initial; ++k)
                    elisar.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.PORTAL, s.scan.get(k), 2, s.scan.get(k).getMiddleBlockPosition(s.to.getY()));
                s.scanI = initial;
            } else if (s.tick > LIFT_T + 1 && s.scanI < s.scan.size()) {
                int end = Math.min(s.scanI + 2, s.scan.size());
                while (s.scanI < end) {
                    ChunkPos cp = s.scan.get(s.scanI++);
                    elisar.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.PORTAL, cp, 2, cp.getMiddleBlockPosition(s.to.getY()));
                }
            }
            if (s.tick == LIFT_T + TRANSIT_T - 30)
                elisar.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.PORTAL, new ChunkPos(s.to), 3, s.to);
            if (s.tick == LIFT_T) {
                p.teleportTo(transit, TRANSIT_CORE.getX() + 0.5, TRANSIT_CORE.getY() + 1, TRANSIT_CORE.getZ() + 0.5, 180F, 0F);
                lock(p);
                send(p, s);
            } else if (s.tick == LIFT_T + TRANSIT_T) {
                // The destination is a fixed, generated landing point on the Great Temple's front apron; it has no capsule.
                p.teleportTo(elisar, s.to.getX() + 0.5, s.to.getY() + 1, s.to.getZ() + 0.5, 180F, 0F);
                lock(p);
                send(p, s);
            } else if (s.tick >= TOTAL + 1) {
                unlock(p);
                it.remove();
                Net.send(p, new FlightMsg(NONE, BlockPos.ZERO, 0));
                p.displayClientMessage(Component.translatable("message.riftworld.terminal_arrived"), true);
                continue;
            } else if (s.tick % 5 == 0) {
                send(p, s);                                           // resync the client clock
            }
            // Keep the player on the source lift, in transit, or anchored to the temple apron sigil.
            int ph = phaseOf(s.tick);
            float t = s.tick - phaseStart(ph);
            Vec3 want = ph == LIFT ? Vec3.atBottomCenterOf(s.from).add(0, 1 + liftOffset(t), 0)
                    : ph == TRANSIT ? Vec3.atBottomCenterOf(TRANSIT_CORE).add(0, 1, 0)
                    : Vec3.atBottomCenterOf(s.to).add(0, 1 + arriveOffset(t), 0);
            p.setDeltaMovement(Vec3.ZERO);
            p.fallDistance = 0;
            if (p.position().distanceTo(want) > (ph == ARRIVE ? 3.0 : 1.5))
                p.connection.teleport(want.x, want.y, want.z, p.getYRot(), p.getXRot());
        }
    }

    // ------------------------------------------------------------------------------------------------ locks
    private static void deny(Event e, Player p) {
        if (p != null && locked(p) && e.isCancelable()) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void breakBlock(BlockEvent.BreakEvent e) {
        deny(e, e.getPlayer());
    }

    @SubscribeEvent
    public static void leftClick(PlayerInteractEvent.LeftClickBlock e) {
        deny(e, e.getEntity());
    }

    @SubscribeEvent
    public static void rightClick(PlayerInteractEvent.RightClickBlock e) {
        deny(e, e.getEntity());
    }

    @SubscribeEvent
    public static void useItem(PlayerInteractEvent.RightClickItem e) {
        deny(e, e.getEntity());
    }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract e) {
        deny(e, e.getEntity());
    }

    @SubscribeEvent
    public static void interactAt(PlayerInteractEvent.EntityInteractSpecific e) {
        deny(e, e.getEntity());
    }

    @SubscribeEvent
    public static void attack(AttackEntityEvent e) {
        deny(e, e.getEntity());
    }

    @SubscribeEvent
    public static void hurt(LivingAttackEvent e) {
        if (e.getEntity() instanceof Player p) deny(e, p);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && ACTIVE.remove(p.getUUID()) != null) unlock(p);
    }

    /** Somebody left the game mid-flight: never leave them in the empty transit dimension. */
    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.level().dimension().equals(TRANSIT_DIM)) {
            ServerLevel elisar = p.server.getLevel(ELISAR_DIM);
            if (elisar == null) return;
            BlockPos landing = templeLanding();
            unlock(p);
            p.teleportTo(elisar, landing.getX() + 0.5, landing.getY() + 1, landing.getZ() + 0.5, 180F, 0F);
        }
    }
}
