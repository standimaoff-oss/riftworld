package ru.aiforge.riftworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import ru.aiforge.riftworld.bridge.BridgeDeckGeom;
import ru.aiforge.riftworld.registry.ModBlockEntities;

/** One rope-bridge post: its cable partner («в длину») and, once the rectangle closes, the built deck. */
public class BridgePostBE extends BlockEntity {
    private BlockPos partner;                    // the post the lengthwise cable is tied to
    private BridgeDeckGeom.Deck deck;            // set on all four corners when the bridge stands

    public BridgePostBE(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BRIDGE_POST.get(), pos, state);
    }

    public BlockPos partner() {
        return partner;
    }

    public BridgeDeckGeom.Deck deck() {
        return deck;
    }

    public void pair(BlockPos partner) {
        this.partner = partner;
        setChanged();
        sync();
    }

    public void stand(BridgeDeckGeom.Deck deck) {
        this.deck = deck;
        setChanged();
        sync();
    }

    public void clearDeck() {
        this.deck = null;
        setChanged();
        sync();
    }

    public void unpair() {
        this.partner = null;
        setChanged();
        sync();
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag t = new CompoundTag();
        saveAdditional(t);
        return t;
    }

    @Override
    protected void saveAdditional(CompoundTag t) {
        super.saveAdditional(t);
        if (partner != null) t.put("partner", NbtUtils.writeBlockPos(partner));
        if (deck != null) {
            CompoundTag d = new CompoundTag();
            d.put("a", NbtUtils.writeBlockPos(deck.a()));
            d.put("b", NbtUtils.writeBlockPos(deck.b()));
            d.put("a2", NbtUtils.writeBlockPos(deck.a2()));
            d.put("b2", NbtUtils.writeBlockPos(deck.b2()));
            t.put("deck", d);
        }
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        partner = t.contains("partner") ? NbtUtils.readBlockPos(t.getCompound("partner")) : null;
        if (t.contains("deck")) {
            CompoundTag d = t.getCompound("deck");
            deck = new BridgeDeckGeom.Deck(NbtUtils.readBlockPos(d.getCompound("a")), NbtUtils.readBlockPos(d.getCompound("b")),
                    NbtUtils.readBlockPos(d.getCompound("a2")), NbtUtils.readBlockPos(d.getCompound("b2")));
        } else deck = null;
    }
}
