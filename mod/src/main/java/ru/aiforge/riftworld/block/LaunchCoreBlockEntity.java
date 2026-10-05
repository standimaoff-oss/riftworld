package ru.aiforge.riftworld.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;
import ru.aiforge.riftworld.registry.ModBlockEntities;
import ru.aiforge.riftworld.registry.ModBlocks;

/**
 * Launch sequence of the shaft: the lid leaves swing down (OPEN_T), the capsule rides up 29 blocks (RISE_T), lands
 * flush with the hall floor and the lab lights flicker back to life (FLICKER_T). Nothing in the lab is changed or cleaned -
 * only the ceiling and emergency lamps blink and then stay on.
 */
public class LaunchCoreBlockEntity extends BlockEntity {
    public static final int IDLE = 0, OPENING = 1, RISING = 2, FLICKER = 3, DONE = 4, FLIGHT = 5, ARRIVE = 6;
    /** Lights flicker after the capsule lands (ticks). */
    public static final int FLICKER_T = 70;
    /** Flight: lift-off on the laser (overworld) and the arrival rise in the Elisar shaft (ticks). */
    public static final int LIFT_T = 40, ARRIVE_T = 60;
    public static final float LIFT_H = 1.6F;
    public static final int OPEN_T = 60, RISE_T = 200, DEPTH = 29, HALF = 5;

    public int stage;
    public long start;
    public boolean door;
    /** Client-side smoothed door opening. */
    public float doorAnim, doorAnimPrev;
    private List<BlockPos> sirens, lamps;

    public LaunchCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCH_CORE.get(), pos, state);
    }

    public Direction facing() {
        return getBlockState().getValue(LaunchCoreBlock.FACING);
    }

    public static float ease(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    /** Capsule vertical offset (blocks) at time t since launch. */
    public float capsuleOffset(float t) {
        if (stage == FLIGHT) return LIFT_H * ease(t / LIFT_T);
        if (stage == ARRIVE) return -DEPTH + DEPTH * ease(t / ARRIVE_T);
        if (stage == FLICKER || stage == DONE) return 0;
        if (stage == IDLE || t < OPEN_T) return -DEPTH;
        return -DEPTH + DEPTH * ease((t - OPEN_T) / RISE_T);
    }

    // ------------------------------------------------------------------------------------------------ server
    public boolean startLaunch() {
        if (level == null || stage != IDLE) return false;
        stage = OPENING;
        start = level.getGameTime();
        sync();
        level.playSound(null, worldPosition, SoundEvents.RAID_HORN.value(), SoundSource.BLOCKS, 6.0F, 0.8F);
        level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 3.0F, 0.5F);
        setSirens(true);
        return true;
    }

    /** Flight start: the landed capsule lifts off on the laser (the player is carried by {@code Flight}). */
    public boolean startFlight() {
        if (level == null || stage != DONE) return false;
        stage = FLIGHT;
        start = level.getGameTime();
        door = false;
        for (int y = 1; y <= 3; ++y)
            for (int x = -1; x <= 1; ++x) {
                BlockPos p = worldPosition.offset(ModelKind.rotate(x, y, 4, facing()));
                BlockState s = level.getBlockState(p);
                if (s.is(ModBlocks.PART.get())) level.setBlock(p, s.setValue(PartBlock.OPEN, false), 2);
            }
        sync();
        return true;
    }

    /** Arrival at the Elisar lab: the shaft lid is gone at once and the capsule rises from the bottom. */
    public void startArrive() {
        if (level == null) return;
        for (int dx = -HALF; dx <= HALF; ++dx)
            for (int dz = -HALF; dz <= HALF; ++dz) {
                if (dx == 0 && dz == 0) continue;
                BlockPos p = worldPosition.offset(dx, 0, dz);
                BlockState s = level.getBlockState(p);
                if (s.is(ModBlocks.PART.get()) || s.getBlock() instanceof GrateBlock) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
        stage = ARRIVE;
        start = level.getGameTime();
        door = false;
        sync();
    }

    private void setSirens(boolean on) {
        if (sirens == null) {
            sirens = new ArrayList<>();
            for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-40, -8, -40), worldPosition.offset(40, 16, 40))) {
                BlockState s = level.getBlockState(p);
                if (s.getBlock() instanceof ModelBlock mb && mb.kind == ModelKind.SIREN) sirens.add(p.immutable());
            }
        }
        for (BlockPos p : sirens) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof ModelBlock) level.setBlock(p, s.setValue(ModelBlock.POWERED, on), 2);
        }
    }

    public void serverTick(ServerLevel sl) {
        if (stage == IDLE) return;
        long now = sl.getGameTime();
        long t = now - start;
        Direction f = facing();
        if (stage == FLIGHT) {
            if (t % 5 == 0) sl.playSound(null, worldPosition, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 3.0F, 1.4F);
            if (t >= LIFT_T + 4) {                          // the capsule is far away now; the shaft shows the landed state again
                stage = DONE;
                sync();
            }
            return;
        }
        if (stage == ARRIVE) {
            float off = capsuleOffset(t);
            if (t % 6 == 0) sl.playSound(null, worldPosition.offset(0, Mth.floor(off) + 2, 0), SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 3.0F, 0.9F);
            if (t >= ARRIVE_T) {
                land(sl, f);
                stage = FLICKER;
                start = now;
                sync();
                sl.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 2.0F, 0.4F);
                sl.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 4.0F, 0.6F);
            }
            return;
        }
        if (stage == OPENING) {
            if (t == 12) {
                for (int dx = -HALF; dx <= HALF; ++dx)
                    for (int dz = -HALF; dz <= HALF; ++dz) {
                        if (dx == 0 && dz == 0) continue;
                        BlockPos p = worldPosition.offset(dx, 0, dz);
                        BlockState s = sl.getBlockState(p);
                        if (s.is(ModBlocks.PART.get()) || s.getBlock() instanceof GrateBlock) sl.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    }
                sync();
            }
            if (t % 14 == 0) sl.playSound(null, worldPosition, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 2.5F, 0.45F);
            if (t == OPEN_T - 6) sl.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.5F, 0.5F);
            if (t >= OPEN_T) {
                stage = RISING;
                sync();
            }
        } else if (stage == RISING) {
            float off = capsuleOffset(t);
            BlockPos at = worldPosition.offset(0, Mth.floor(off) + 2, 0);
            if (t % 30 == 0) sl.playSound(null, at, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 3.0F, 0.6F);
            if (t % 8 == 0) sl.playSound(null, at, SoundEvents.MINECART_INSIDE, SoundSource.BLOCKS, 0.25F, 0.4F);
            if (t == OPEN_T + 100) sl.playSound(null, worldPosition, SoundEvents.RAID_HORN.value(), SoundSource.BLOCKS, 6.0F, 0.8F);
            if (t % 3 == 0) {
                for (int k = 0; k < 4; ++k) {
                    double a = sl.random.nextDouble() * Math.PI * 2;
                    sl.sendParticles(ParticleTypes.CLOUD, worldPosition.getX() + 0.5 + Math.cos(a) * 5.2, worldPosition.getY() + off + 0.3,
                            worldPosition.getZ() + 0.5 + Math.sin(a) * 5.2, 1, 0.1, 0.05, 0.1, 0.01);
                }
            }
            if (t >= OPEN_T + RISE_T) {
                land(sl, f);
                stage = FLICKER;
                start = now;
                sync();
                sl.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 2.0F, 0.4F);
                sl.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 4.0F, 0.6F);
            }
        } else if (stage == FLICKER) {
            Boolean on = flickerState(t);
            if (on != null) {
                setLamps(sl, on);
                if (on) sl.playSound(null, worldPosition.above(6), SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 0.6F, 1.6F + sl.random.nextFloat() * 0.3F);
            }
            if (t >= FLICKER_T) {
                setLamps(sl, true);
                stage = DONE;
                setSirens(false);
                sync();
                sl.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 3.0F, 1.2F);
            }
        }
        if ((stage == FLICKER || stage == DONE) && t % 4 == 0) doors(sl, f);
    }

    /** Capsule cells (model frame: x right, z toward the front) that receive invisible collision parts. */
    public static List<int[]> capsuleCells() {
        List<int[]> out = new ArrayList<>();
        for (int x = -HALF; x <= HALF; ++x)
            for (int z = -HALF; z <= HALF; ++z)
                if (x != 0 || z != 0) out.add(new int[] {x, 0, z, 0});
        for (int y = 1; y <= 8; ++y) {
            double zc = y + 0.5 - 4.6;
            double rr = Math.sqrt(Math.max(0, 4.5 * 4.5 - zc * zc));
            for (int x = -HALF; x <= HALF; ++x)
                for (int z = -HALF; z <= HALF; ++z) {
                    double d = Math.sqrt(x * x + z * z);
                    boolean wall = y == 8 ? d <= rr + 0.5 : Math.abs(d - rr) < 0.6;
                    if (!wall) continue;
                    if (Math.abs(x) <= 1 && z >= 2 && y <= 3) continue; // vestibule passage
                    out.add(new int[] {x, y, z, 0});
                }
        }
        for (int y = 1; y <= 3; ++y) {
            for (int z = 3; z <= 4; ++z) {
                out.add(new int[] {-2, y, z, 0});
                out.add(new int[] {2, y, z, 0});
            }
            for (int x = -1; x <= 1; ++x) out.add(new int[] {x, y, 4, 1}); // door
        }
        for (int x = -1; x <= 1; ++x) {
            out.add(new int[] {x, 4, 3, 0});
            out.add(new int[] {x, 4, 4, 0});
        }
        out.add(new int[] {-1, 1, -2, 0});
        out.add(new int[] {1, 1, -2, 0});
        out.add(new int[] {0, 1, -2, 2}); // terminal
        return out;
    }

    private void land(ServerLevel sl, Direction f) {
        BlockState part = ModBlocks.PART.get().defaultBlockState();
        for (int[] c : capsuleCells()) {
            BlockPos p = worldPosition.offset(ModelKind.rotate(c[0], c[1], c[2], f));
            BlockState cur = sl.getBlockState(p);
            if (!cur.isAir() && !cur.canBeReplaced() && !cur.is(ModBlocks.PART.get())) continue;
            BlockState s = c[3] == 2 ? ModBlocks.CAPSULE_TERMINAL.get().defaultBlockState() : part;
            sl.setBlock(p, s, 3);
        }
    }

    private void doors(ServerLevel sl, Direction f) {
        BlockPos c = worldPosition.offset(ModelKind.rotate(0, 2, 4, f));
        AABB box = new AABB(c).inflate(3.2, 2.0, 3.2);
        boolean near = !sl.getEntitiesOfClass(Player.class, box, p -> !p.isSpectator()).isEmpty();
        if (near == door) return;
        door = near;
        for (int y = 1; y <= 3; ++y)
            for (int x = -1; x <= 1; ++x) {
                BlockPos p = worldPosition.offset(ModelKind.rotate(x, y, 4, f));
                BlockState s = sl.getBlockState(p);
                if (s.is(ModBlocks.PART.get())) sl.setBlock(p, s.setValue(PartBlock.OPEN, near), 2);
            }
        sl.playSound(null, c, near ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.8F, 1.5F);
        sl.playSound(null, c, near ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 0.8F);
        sync();
    }

    /** Irregular power-up blink: short stutters that get longer until the lamps hold. null = no change this tick. */
    private static final int[] BLINK = {2, 5, 8, 9, 14, 16, 22, 23, 26, 33, 41, 44, 52, 54, 58};

    static Boolean flickerState(long t) {
        for (int k = 0; k < BLINK.length; ++k) if (BLINK[k] == t) return k % 2 == 0;
        return null;
    }

    /** Lamp state at time t of the flicker (used by the capsule renderer for its own lights). */
    public static boolean lightsOn(float t) {
        boolean on = false;
        for (int k = 0; k < BLINK.length && BLINK[k] <= t; ++k) on = k % 2 == 0;
        return on || t >= FLICKER_T;
    }

    private void setLamps(ServerLevel sl, boolean on) {
        if (lamps == null) {
            lamps = new ArrayList<>();
            for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-40, -8, -40), worldPosition.offset(40, 16, 40))) {
                if (sl.getBlockState(p).getBlock() instanceof LabLampBlock) lamps.add(p.immutable());
            }
        }
        for (BlockPos p : lamps) {
            BlockState s = sl.getBlockState(p);
            if (s.getBlock() instanceof LabLampBlock && s.getValue(LabLampBlock.LIT) != on) sl.setBlock(p, s.setValue(LabLampBlock.LIT, on), 2);
        }
    }

    // ------------------------------------------------------------------------------------------------ client
    public static void clientTick(LaunchCoreBlockEntity be) {
        be.doorAnimPrev = be.doorAnim;
        be.doorAnim = Mth.clamp(be.doorAnim + (be.door ? 0.1F : -0.1F), 0, 1);
    }

    // ------------------------------------------------------------------------------------------------ sync
    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Stage", stage);
        tag.putLong("Start", start);
        tag.putBoolean("Door", door);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        stage = tag.getInt("Stage");
        start = tag.getLong("Start");
        door = tag.getBoolean("Door");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }
}
