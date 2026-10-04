package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import ru.aiforge.riftworld.registry.ModBlockEntities;

/** Shared block entity for all GLB machines: helm stones, lever/door animation clock. */
public class RiftBlockEntity extends BlockEntity {
    /** Bit mask of inserted resonance stones (helm only). */
    public int stones;
    /** Game time of the last activation (lever pull, book drop cooldown). */
    public long animStart = -1000;
    /** Helm power state ({@link LabPower}) and the game time it was entered. */
    public int stage;
    public long stageTime;
    /** Transient lamp / model lists of the building (helm only, rebuilt after load). */
    java.util.List<BlockPos> lamps, basementLamps, chandeliers, models;
    long[] flickerOn;
    /** Client-side smoothed door opening 0..1. */
    public float doorOpen, doorOpenPrev;

    public RiftBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MODEL.get(), pos, state);
    }

    public int stoneCount() {
        return Integer.bitCount(stones & 31);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Stones", stones);
        tag.putLong("AnimStart", animStart);
        tag.putInt("Stage", stage);
        tag.putLong("StageTime", stageTime);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        stones = tag.getInt("Stones");
        animStart = tag.contains("AnimStart") ? tag.getLong("AnimStart") : -1000;
        stage = tag.getInt("Stage");
        stageTime = tag.getLong("StageTime");
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
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
        return new AABB(worldPosition).inflate(3.0);
    }
}
