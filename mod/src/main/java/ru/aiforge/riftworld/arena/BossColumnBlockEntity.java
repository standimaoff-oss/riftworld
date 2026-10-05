package ru.aiforge.riftworld.arena;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import ru.aiforge.riftworld.registry.ArenaBlocks;
import ru.aiforge.riftworld.registry.CastleBlocks;

/** State of a healing column: crystal HP, rise / sink animation clock, healing beam target (synced to clients). */
public class BossColumnBlockEntity extends BlockEntity {
    public static final int RISE_T = 60, SINK_T = 50, MAX_HP = 3, HIT_COOLDOWN = 8;
    public static final double CRYSTAL_Y = 2.6;

    public int hp = MAX_HP;
    /** 0 = still, 1 = rising, 2 = sinking. */
    public int anim;
    public long animStart;
    public boolean beam;
    public double tx, ty, tz;
    public long flash = -100;
    private long lastHit = -100;

    public BossColumnBlockEntity(BlockPos pos, BlockState state) {
        super(ArenaBlocks.COLUMN_BE.get(), pos, state);
    }

    public int phase() {
        return getBlockState().getValue(BossColumnBlock.PHASE);
    }

    public boolean standing() {
        return phase() == 1 && anim == 0;
    }

    public boolean shattered() {
        return phase() == 2;
    }

    private long now() {
        return level == null ? 0 : level.getGameTime();
    }

    public void rise() {
        if (level == null || phase() != 0 || anim != 0) return;
        anim = 1;
        animStart = now();
        hp = MAX_HP;
        level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.4F, 0.45F);
        level.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0F, 0.5F);
        sync();
    }

    public void sink() {
        if (level == null || (phase() == 0 && anim == 0)) return;
        removeParts();
        anim = 2;
        animStart = now();
        beam = false;
        level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 1.0F, 0.5F);
        sync();
    }

    public void setBeam(boolean on, double x, double y, double z) {
        boolean b = on && standing();
        if (b == beam && Math.abs(x - tx) + Math.abs(y - ty) + Math.abs(z - tz) < 0.3) return;
        beam = b;
        tx = x;
        ty = y;
        tz = z;
        sync();
    }

    void serverTick() {
        if (!(level instanceof ServerLevel sl) || anim == 0) return;
        long t = now() - animStart;
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY(), z = worldPosition.getZ() + 0.5;
        if (anim == 1) {
            if (t % 4 == 0) {
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, CastleBlocks.ASHLAR.get().defaultBlockState()), x, y + 0.1, z,
                        10, 0.45, 0.05, 0.45, 0.05);
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.2, z, 2, 0.4, 0.05, 0.4, 0.01);
            }
            if (t >= RISE_T) {
                anim = 0;
                setPhase(1);
                placeParts();
                sl.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.2F, 0.6F);
                sl.sendParticles(ParticleTypes.FLAME, x, y + CRYSTAL_Y, z, 20, 0.2, 0.2, 0.2, 0.03);
                sync();
            }
        } else if (t >= SINK_T) {
            anim = 0;
            hp = MAX_HP;
            setPhase(0);
            sync();
        }
    }

    private void setPhase(int p) {
        if (level != null) level.setBlock(worldPosition, getBlockState().setValue(BossColumnBlock.PHASE, p), 3);
    }

    private void placeParts() {
        if (level == null) return;
        BlockState s = getBlockState();
        for (int i = 1; i <= 2; ++i) {
            BlockPos p = worldPosition.above(i);
            if (level.getBlockState(p).canBeReplaced()) {
                level.setBlock(p, s.setValue(BossColumnBlock.PART, i).setValue(BossColumnBlock.PHASE, 1), 3);
            }
        }
    }

    private void removeParts() {
        if (level == null) return;
        for (int i = 1; i <= 2; ++i) {
            BlockPos p = worldPosition.above(i);
            if (level.getBlockState(p).is(getBlockState().getBlock())) level.removeBlock(p, false);
        }
    }

    public static boolean isPickaxe(ItemStack s) {
        return s.getItem() instanceof PickaxeItem || s.is(ItemTags.PICKAXES);
    }

    /** A melee hit on one of the cells (top = the crystal cell) or an arrow in the crystal. */
    void hit(Player player, boolean top) {
        if (!(level instanceof ServerLevel sl) || !standing()) return;
        long t = now();
        if (t - lastHit < HIT_COOLDOWN) return;
        lastHit = t;
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY(), z = worldPosition.getZ() + 0.5;
        boolean pick = player != null && isPickaxe(player.getMainHandItem());
        if (top || pick) {
            --hp;
            flash = t;
            double hy = y + (top ? CRYSTAL_Y : 1.2);
            sl.playSound(null, x, hy, z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.6F, 0.55F);
            sl.playSound(null, x, hy, z, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 0.7F);
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, CastleBlocks.ASHLAR.get().defaultBlockState()), x, hy, z,
                    18, 0.25, 0.25, 0.25, 0.1);
            sl.sendParticles(ParticleTypes.LAVA, x, y + CRYSTAL_Y, z, 4, 0.15, 0.15, 0.15, 0);
            if (hp <= 0) {
                shatter(sl);
            } else if (player != null) {
                player.displayClientMessage(Component.translatable("riftworld.column.crack", MAX_HP - hp, MAX_HP).withStyle(ChatFormatting.GOLD), true);
            }
        } else {
            sl.playSound(null, x, y + 1.0, z, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5F, 1.6F);
            sl.sendParticles(ParticleTypes.CRIT, x, y + 1.0, z, 8, 0.3, 0.3, 0.3, 0.2);
            if (player != null) player.displayClientMessage(Component.translatable("riftworld.column.hint").withStyle(ChatFormatting.GRAY), true);
        }
        sync();
    }

    private void shatter(ServerLevel sl) {
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY(), z = worldPosition.getZ() + 0.5;
        removeParts();
        beam = false;
        setPhase(2);
        sl.playSound(null, x, y + 1.5, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0F, 1.3F);
        sl.playSound(null, x, y + CRYSTAL_Y, z, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.6F, 0.5F);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, CastleBlocks.ASHLAR.get().defaultBlockState()), x, y + 1.5, z,
                70, 0.35, 1.0, 0.35, 0.15);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, CastleBlocks.BRICKS.get().defaultBlockState()), x, y + 1.2, z,
                25, 0.3, 0.8, 0.3, 0.12);
        sl.sendParticles(ParticleTypes.FLAME, x, y + CRYSTAL_Y, z, 30, 0.3, 0.3, 0.3, 0.08);
        sl.sendParticles(ParticleTypes.LAVA, x, y + CRYSTAL_Y, z, 10, 0.3, 0.3, 0.3, 0);
        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 1.6, z, 8, 0.3, 0.6, 0.3, 0.02);
    }

    // ------------------------------------------------------------------------------------------ persistence / sync
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Hp", hp);
        tag.putInt("Anim", anim);
        tag.putLong("AnimStart", animStart);
        tag.putBoolean("Beam", beam);
        tag.putDouble("TX", tx);
        tag.putDouble("TY", ty);
        tag.putDouble("TZ", tz);
        tag.putLong("Flash", flash);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        hp = tag.contains("Hp") ? tag.getInt("Hp") : MAX_HP;
        anim = tag.getInt("Anim");
        animStart = tag.getLong("AnimStart");
        beam = tag.getBoolean("Beam");
        tx = tag.getDouble("TX");
        ty = tag.getDouble("TY");
        tz = tag.getDouble("TZ");
        flash = tag.contains("Flash") ? tag.getLong("Flash") : -100;
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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
