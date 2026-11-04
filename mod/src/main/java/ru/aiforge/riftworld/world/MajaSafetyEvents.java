package ru.aiforge.riftworld.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.TickEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.flight.Flight;

/** Records only newly generated chunks and ordinary Forge player placements. */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID)
public final class MajaSafetyEvents {
    private static final Map<ServerLevel, Set<BlockPos>> PENDING_BREAKS = new HashMap<>();
    private static final Pattern WORLD_EDIT = Pattern.compile("(^|[\\s/])(setblock|fill|clone|place)(\\s|$)|//(set|paste|replace|undo)(\\s|$)");
    private MajaSafetyEvents() {}

    @SubscribeEvent
    public static void newChunk(ChunkEvent.Load event) {
        if (!event.isNewChunk() || !(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(Flight.ELISAR_DIM)) return;
        int cx = event.getChunk().getPos().x, cz = event.getChunk().getPos().z;
        // The event may fire before FULL; touch no chunks here and marshal the journal to the server thread.
        level.getServer().execute(() -> MajaWorldData.get(level).markNewChunk(cx, cz, level.getGameTime()));
    }

    @SubscribeEvent(priority = EventPriority.MONITOR)
    public static void place(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(Flight.ELISAR_DIM)) return;
        MajaWorldData data = MajaWorldData.get(level);
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            for (var snapshot : multi.getReplacedBlockSnapshots()) recordPlacement(level, data, snapshot.getPos());
        } else recordPlacement(level, data, event.getPos());
    }

    private static void recordPlacement(ServerLevel level, MajaWorldData data, BlockPos pos) {
        Set<BlockPos> pending = PENDING_BREAKS.get(level);
        if (pending != null) pending.remove(pos);
        data.placed(pos);
    }

    @SubscribeEvent(priority = EventPriority.MONITOR)
    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension().equals(Flight.ELISAR_DIM))
            PENDING_BREAKS.computeIfAbsent(level, unused -> new HashSet<>()).add(event.getPos().immutable());
    }

    /** The break event happens before the block actually disappears; a cancelled break must stay protected. */
    @SubscribeEvent
    public static void finishBreaks(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        Set<BlockPos> pending = PENDING_BREAKS.remove(level);
        if (pending == null) return;
        MajaWorldData data = MajaWorldData.get(level);
        for (BlockPos pos : pending) {
            if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null
                    && level.isEmptyBlock(pos)) data.broken(pos);
        }
    }
    @SubscribeEvent(priority = EventPriority.MONITOR)
    public static void piston(PistonEvent.Pre event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension().equals(Flight.ELISAR_DIM))
            MajaWorldData.get(level).invalidateAround(event.getPos());
    }

    @SubscribeEvent(priority = EventPriority.MONITOR)
    public static void command(CommandEvent event) {
        String raw = event.getParseResults().getReader().getString().toLowerCase(Locale.ROOT);
        if (!WORLD_EDIT.matcher(raw).find()) return;
        var source = event.getParseResults().getContext().getSource();
        if (!source.getLevel().dimension().equals(Flight.ELISAR_DIM) && !raw.contains("riftworld:elisar")) return;
        ServerLevel level = source.getServer().getLevel(Flight.ELISAR_DIM);
        if (level != null) MajaWorldData.get(level).invalidateAfterCommand(level.getGameTime());
    }

}
