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

        // Превращаем старый квадратный портал шаблона (demon_frame + demon_gate) в безрамочный 3D-Разлом:
        // убираем все блоки demon_frame / demon_gate в пересечении чанка и ставим один якорь в центре основания!
        BoundingBox fullBb = t.get().getBoundingBox(new StructurePlaceSettings().setRotation(rot), origin);
        long sumX = 0, sumZ = 0;
        int count = 0;
        int minGateY = Integer.MAX_VALUE;
        for (StructureTemplate.StructureBlockInfo info : t.get().filterBlocks(origin,
                new StructurePlaceSettings().setRotation(rot), TempleBlocks.DEMON_GATE.get())) {
            BlockPos gp = info.pos();
            sumX += gp.getX();
            sumZ += gp.getZ();
            ++count;
            if (gp.getY() < minGateY) minGateY = gp.getY();
        }

        int x0 = Math.max(box.minX(), fullBb.minX());
        int x1 = Math.min(box.maxX(), fullBb.maxX());
        int y0 = Math.max(box.minY(), fullBb.minY());
        int y1 = Math.min(box.maxY(), fullBb.maxY());
        int z0 = Math.max(box.minZ(), fullBb.minZ());
        int z1 = Math.min(box.maxZ(), fullBb.maxZ());
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int bx = x0; bx <= x1; ++bx) {
            for (int bz = z0; bz <= z1; ++bz) {
                for (int by = y0; by <= y1; ++by) {
                    mp.set(bx, by, bz);
                    BlockState st = level.getBlockState(mp);
                    if (st.is(TempleBlocks.DEMON_FRAME.get()) || st.is(TempleBlocks.DEMON_GATE.get())) {
                        level.setBlock(mp, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        BlockPos riftAnchor = count > 0
                ? new BlockPos((int) Math.round((double) sumX / count), minGateY, (int) Math.round((double) sumZ / count))
                : new BlockPos(anchor.getX(), y + 1, anchor.getZ());
        if (box.isInside(riftAnchor)) {
            level.setBlock(riftAnchor, TempleBlocks.DEMON_GATE.get().defaultBlockState(), 2);
        }
    }
}
