package ru.aiforge.riftworld.world;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import ru.aiforge.riftworld.registry.ModWorldgen;

/** Single-piece ruin: places ruins/temple_overgrown around its anchor (piece bbox is a flat envelope of the ruin). */
public class OvergrownTemplePiece extends StructurePiece {
    public static final int TW = 27, TD = 23, TH = 12;

    private final BlockPos anchor;
    private final Rotation rot;

    /** Envelope covering the 27x23 footprint in ANY rotation, anchored at the template centre. */
    public OvergrownTemplePiece(BlockPos anchor, Rotation rot) {
        super(ModWorldgen.OVERGROWN_TEMPLE_PIECE.get(), 0, envelope(anchor));
        this.anchor = anchor;
        this.rot = rot;
    }

    public OvergrownTemplePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModWorldgen.OVERGROWN_TEMPLE_PIECE.get(), tag);
        this.anchor = NbtUtils.readBlockPos(tag.getCompound("anchor"));
        this.rot = Rotation.valueOf(tag.getString("rot"));
    }

    private static BoundingBox envelope(BlockPos center) {
        int r = (Math.max(TW, TD) - 1) / 2 + 2;
        return new BoundingBox(center.getX() - r, center.getY() - 26, center.getZ() - r, center.getX() + r, center.getY() + TH + 4, center.getZ() + r);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.put("anchor", NbtUtils.writeBlockPos(anchor));
        tag.putString("rot", rot.name());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager sm, ChunkGenerator gen, RandomSource rnd, BoundingBox box, ChunkPos cp, BlockPos pos) {
        StructureTemplateManager tm = level.getLevel().getServer().getStructureManager();
        // Part 10: TWO ruins in the overworld - the overgrown temple and its east-burst twin, picked by anchor hash
        long h = (anchor.getX() * 73428767L) ^ (anchor.getZ() * 912931L);
        boolean variantB = (h & 1) == 1;
        String template = variantB ? "ruins/temple_overgrown_b" : "ruins/temple_overgrown";
        Optional<StructureTemplate> t = tm.get(new ResourceLocation("riftworld", template));
        if (t.isEmpty()) return;
        StructurePlaceSettings s = new StructurePlaceSettings().setRotation(rot).setBoundingBox(box).setIgnoreEntities(true);
        // sink the ruined podium half a metre into the turf
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, anchor.getX(), anchor.getZ()) - 1;
        BoundingBox bb = t.get().getBoundingBox(s, BlockPos.ZERO);
        BlockPos origin = new BlockPos(anchor.getX() - (bb.minX() + bb.maxX()) / 2, y - 1, anchor.getZ() - (bb.minZ() + bb.maxZ()) / 2);
        t.get().placeInWorld(level, origin, origin, s, rnd, 2);
        BlockPos portalPos = StructureTemplate.calculateRelativePosition(s, new BlockPos(13, 2, 4)).offset(origin);
        RiftTravel.setOverworldAltar(level.getLevel(), portalPos);
    }
}
