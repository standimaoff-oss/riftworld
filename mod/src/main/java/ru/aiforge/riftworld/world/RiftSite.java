package ru.aiforge.riftworld.world;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.block.RiftAltarBlock;
import ru.aiforge.riftworld.registry.RiftBlocks;

/**
 * Rev 5: the Great Temple of the Rift in the OVERWORLD - the gate the player starts the journey from.
 * <p>
 * The temple is the very same building the city «Странники» carries in Elisar (template
 * {@code data/riftworld/structures/city/temple_great.nbt}): it is raised once per world on a flat, dry spot at the
 * edge of the spawn area, on its own dark plinth, and the position of its altar of the Rift is remembered in
 * {@link RiftTravel.Links}, so the ritual of the rift works exactly the same on both sides of the passage.
 * <p>
 * The spot is searched in rings around the world spawn (a plateau of low relief above the sea), the temple is placed
 * with its plinth (template layer 29) on the terrain top, the volume above the plinth is cleared of whatever grew
 * there before, and the dips under the plinth are filled with the dark stone of the rift: no floating edges, no trees
 * inside the nave. The library stone of the crypt stays where it was - the template only carves it open.
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RiftSite {
    /** Footprint of the template, in blocks. */
    public static final int TW = 63, TD = 70;
    /** The altar cell of the template and its floor layer: the same numbers {@link RiftZone} counts from. */
    public static final int ALTAR_X = 31, ALTAR_Z = 34, GROUND = 30;
    /** Rings of the spot search around the world spawn. */
    private static final int R0 = 176, R1 = 368, STEP = 24;
    /** How many layers above the plinth are cleared of terrain and plants. */
    private static final int LAYERS = 42;
    private static final ResourceLocation TEMPLATE = new ResourceLocation(RiftWorld.MODID, "city/temple_great");

    /** The site is raised on one of the first ticks of a fresh world (three tries, then the world is left alone). */
    private static int tries;

    private RiftSite() {
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        MinecraftServer server = e.getServer();
        if (server == null || server.getTickCount() < 60 || tries >= 3) return;
        ServerLevel ow = server.overworld();
        if (ow == null) return;
        if (RiftTravel.overworldAltar(server) != null) {
            tries = 3;                                            // the world already knows its temple
            return;
        }
        tries++;
        BlockPos altar = build(ow);
        if (altar == null) return;
        RiftTravel.setOverworldAltar(ow, altar);
        announce(server, altar, null);
    }

    /** Everybody who joins the overworld is told where the gate stands - otherwise the temple is a needle in a haystack. */
    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        MinecraftServer server = p.getServer();
        if (server == null || !p.level().dimension().equals(Level.OVERWORLD)) return;
        BlockPos altar = RiftTravel.overworldAltar(server);
        if (altar != null) announce(server, altar, p);
    }

    // ------------------------------------------------------------------ the site
    /** Raises the temple and returns the world cell of its altar (null when there is no room or no template). */
    public static BlockPos build(ServerLevel ow) {
        StructureTemplateManager stm = ow.getServer().getStructureManager();
        Optional<StructureTemplate> tpl = stm.get(TEMPLATE);
        if (tpl.isEmpty()) return null;
        int[] spot = findSpot(ow);
        if (spot == null) return null;
        int cx = spot[0], cz = spot[1], top = spot[2];
        int y0 = top - GROUND;                                     // plinth bottom (template 29) lands on the terrain top
        BlockPos origin = new BlockPos(cx - ALTAR_X, y0, cz - ALTAR_Z);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

        // 1) clear whatever grew above the terrain inside the footprint (trunks, leaves, water, snow)
        for (int dx = 0; dx < TW; dx++) {
            for (int dz = 0; dz < TD; dz++) {
                for (int dy = 1; dy <= LAYERS; dy++) {
                    m.set(origin.getX() + dx, y0 + GROUND + dy, origin.getZ() + dz);
                    if (!ow.getBlockState(m).isAir()) ow.setBlock(m, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        // 2) dark skirt under the plinth: the dips of the plateau are filled, nothing hangs in the air
        BlockState skirt = RiftBlocks.RIFT_STONE_BRICKS.get().defaultBlockState();
        for (int dx = 0; dx < TW; dx++) {
            for (int dz = 0; dz < TD; dz++) {
                for (int dy = 0; dy < 10; dy++) {
                    m.set(origin.getX() + dx, y0 + GROUND - 2 - dy, origin.getZ() + dz);
                    BlockState s = ow.getBlockState(m);
                    if (s.isAir() || !s.getFluidState().isEmpty()) ow.setBlock(m, skirt, 2);
                }
            }
        }
        // 3) the temple itself (the air of the template opens the crypt under the nave)
        StructurePlaceSettings set = new StructurePlaceSettings().setRotation(Rotation.NONE).setIgnoreEntities(true);
        BoundingBox bb = tpl.get().getBoundingBox(set, BlockPos.ZERO);
        BlockPos pos = origin.offset(-bb.minX(), 0, -bb.minZ());
        tpl.get().placeInWorld(ow, pos, pos, set, ow.getRandom(), 2);
        // 4) the altar of the Rift: even a world without the plot must get its gate
        BlockPos altar = pos.offset(ALTAR_X, GROUND, ALTAR_Z);
        if (!(ow.getBlockState(altar).getBlock() instanceof RiftAltarBlock)) {
            ow.setBlock(altar, RiftBlocks.ALTAR.get().defaultBlockState(), 3);
        }
        return altar;
    }

    /** A flat, dry spot for the temple: rings of candidates around the world spawn. */
    private static int[] findSpot(ServerLevel ow) {
        BlockPos sp = ow.getSharedSpawnPos();
        for (int r = R0; r <= R1; r += STEP) {
            for (int a = 0; a < 16; a++) {
                double t = a * (Math.PI / 8.0);
                int cx = sp.getX() + (int) Math.round(Math.cos(t) * r);
                int cz = sp.getZ() + (int) Math.round(Math.sin(t) * r);
                int[] h = heights(ow, cx, cz);
                if (h == null) continue;
                if (h[1] - h[0] > 6) continue;                     // too hilly for a plinth
                if (h[0] < 64) continue;                            // ocean, river or a beach at the water line
                if (Math.abs(h[0] - sp.getY()) > 32) continue;      // a completely different level of the world
                return new int[]{cx, cz, h[1]};
            }
        }
        return null;
    }

    /** {lowest, highest} terrain top over the temple footprint, or null where the chunks refuse to load. */
    private static int[] heights(ServerLevel ow, int cx, int cz) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int dx = -ALTAR_X; dx <= TW - ALTAR_X; dx += 3) {
            for (int dz = -ALTAR_Z; dz <= TD - ALTAR_Z; dz += 3) {
                int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx + dx, cz + dz);
                if (y <= ow.getMinBuildHeight() + 1) return null;
                if (y < min) min = y;
                if (y > max) max = y;
            }
        }
        return new int[]{min, max};
    }

    private static void announce(MinecraftServer server, BlockPos altar, ServerPlayer only) {
        Component msg = Component.translatable("riftworld.rift.temple", altar.getX(), altar.getZ());
        if (only != null) {
            only.sendSystemMessage(msg);
            return;
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.level().dimension().equals(Level.OVERWORLD)) p.sendSystemMessage(msg);
        }
    }
}
