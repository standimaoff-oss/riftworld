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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import ru.aiforge.riftworld.registry.ModWorldgen;
import ru.aiforge.riftworld.registry.TempleBlocks;

/**
 * Руины храма в Обычном мире: устанавливают шаблон {@code ruins/temple_overgrown} (или {@code _b}),
 * после чего заменяют старую квадратную блочную рамку {@code demon_frame} + плоское поле {@code demon_gate}
 * на ЕДИНЫЙ невидимый напольный якорь {@code demon_gate}, из которого в небо вырастает гигантский
 * 3D-Пространственный Разлом ({@code DemonGateRenderer}).
 */
public class OvergrownTemplePiece extends StructurePiece {
    public static final int TW = 27, TD = 23, TH = 12;

    private final BlockPos anchor;
    private final Rotation rot;

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
        return new BoundingBox(center.getX() - r, center.getY() - 26, center.getZ() - r,
                center.getX() + r, center.getY() + TH + 4, center.getZ() + r);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.put("anchor", NbtUtils.writeBlockPos(anchor));
        tag.putString("rot", rot.name());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager sm, ChunkGenerator gen, RandomSource rnd,
                            BoundingBox box, ChunkPos cp, BlockPos pos) {
        StructureTemplateManager tm = level.getLevel().getServer().getStructureManager();
        long h = (anchor.getX() * 73428767L) ^ (anchor.getZ() * 912931L);
        boolean variantB = (h & 1) == 1;
        String template = variantB ? "ruins/temple_overgrown_b" : "ruins/temple_overgrown";
        Optional<StructureTemplate> t = tm.get(new ResourceLocation("riftworld", template));
        if (t.isEmpty()) return;

        StructurePlaceSettings s = new StructurePlaceSettings().setRotation(rot).setBoundingBox(box).setIgnoreEntities(true);
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, anchor.getX(), anchor.getZ()) - 1;
        BoundingBox bb = t.get().getBoundingBox(s, BlockPos.ZERO);
        BlockPos origin = new BlockPos(
                anchor.getX() - (bb.minX() + bb.maxX()) / 2,
                y - 1,
                anchor.getZ() - (bb.minZ() + bb.maxZ()) / 2);
        t.get().placeInWorld(level, origin, origin, s, rnd, 2);

        // В центре заросших руин закладываем безблочную древнюю ритуальную печать (MOSAIC_CORE + GOLD_RUNE)
        // и скромный сундук странника (никаких блоков портала в мире больше нет!).
        BlockPos sealCenter = new BlockPos(anchor.getX(), y, anchor.getZ());
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                BlockPos fp = sealCenter.offset(dx, 0, dz);
                if (!box.isInside(fp)) continue;
                BlockState floorSt = (dx == 0 && dz == 0)
                        ? TempleBlocks.MOSAIC_CORE.get().defaultBlockState()
                        : TempleBlocks.GOLD_RUNE.get().defaultBlockState();
                level.setBlock(fp, floorSt, 2);
                level.setBlock(fp.above(), Blocks.AIR.defaultBlockState(), 2);
                level.setBlock(fp.above(2), Blocks.AIR.defaultBlockState(), 2);
            }
        }
        BlockPos chestPos = sealCenter.offset(2, 1, -2);
        if (box.isInside(chestPos)) {
            LandmarkStructures.putLootChest(level.getChunk(chestPos),
                    chestPos.getX() & ~15, chestPos.getZ() & ~15,
                    chestPos.getX(), chestPos.getY(), chestPos.getZ(),
                    net.minecraft.core.Direction.WEST, LandmarkStructures.LOOT_MODEST_WAYFARER);
        }
    }
}
