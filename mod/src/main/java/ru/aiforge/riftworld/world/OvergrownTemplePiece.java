package ru.aiforge.riftworld.world;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import ru.aiforge.riftworld.registry.ModStructures;
import ru.aiforge.riftworld.registry.TempleBlocks;

/**
 * Заросшие руины в Обычном мире: вместо старой квадратной рамки портала {@code demon_frame}
 * оставляет лишь один невидимый наземный якорь {@code demon_gate} посреди живописных мшистых руин,
 * из которого прямо в небо уходит огромный 3D-Прорыв Пространства!
 */
public class OvergrownTemplePiece extends TemplateStructurePiece {
    public OvergrownTemplePiece(StructureTemplateManager stm, ResourceLocation id, BlockPos pos, Rotation rot) {
        super(ModStructures.OVERGROWN_TEMPLE_PIECE.get(), 0, stm, id, id.toString(), settings(rot), pos);
    }

    public OvergrownTemplePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.OVERGROWN_TEMPLE_PIECE.get(), tag, ctx.structureTemplateManager(),
                id -> settings(Rotation.valueOf(tag.getString("Rot"))));
    }

    private static StructurePlaceSettings settings(Rotation rot) {
        return new StructurePlaceSettings().setRotation(rot).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super.addAdditionalSaveData(ctx, tag);
        tag.putString("Rot", this.placeSettings.getRotation().name());
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource rand, BoundingBox box) {
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager sm, ChunkGenerator gen, RandomSource rand,
                            BoundingBox box, ChunkPos cp, BlockPos pivot) {
        BoundingBox b = this.template.getBoundingBox(this.placeSettings, this.templatePosition);
        int cx = (b.minX() + b.maxX()) / 2, cz = (b.minZ() + b.maxZ()) / 2;
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx, cz) - 1;
        this.templatePosition = new BlockPos(this.templatePosition.getX(), y, this.templatePosition.getZ());
        super.postProcess(level, sm, gen, rand, box, cp, pivot);

        // Находим единую нижне-центральную точку якоря Разлома по всему шаблону (детерминированно между чанками)
        List<StructureTemplate.StructureBlockInfo> gates =
                this.template.filterBlocks(this.templatePosition, this.placeSettings, TempleBlocks.DEMON_GATE.get());
        BlockPos anchor = null;
        if (!gates.isEmpty()) {
            int minY = Integer.MAX_VALUE;
            double sumX = 0.0, sumZ = 0.0;
            for (StructureTemplate.StructureBlockInfo info : gates) {
                minY = Math.min(minY, info.pos().getY());
                sumX += info.pos().getX();
                sumZ += info.pos().getZ();
            }
            double avgX = sumX / gates.size();
            double avgZ = sumZ / gates.size();
            double bestD2 = Double.MAX_VALUE;
            for (StructureTemplate.StructureBlockInfo info : gates) {
                if (info.pos().getY() != minY) continue;
                double dx = info.pos().getX() - avgX, dz = info.pos().getZ() - avgZ;
                double d2 = dx * dx + dz * dz;
                if (d2 < bestD2) {
                    bestD2 = d2;
                    anchor = info.pos().immutable();
                }
            }
        }

        // Убираем квадратную каменную рамку demon_frame и лишние блоки полотна demon_gate,
        // оставляя только 1 точку якоря 3D-Прорыва Пространства у земли!
        BoundingBox placedBox = this.template.getBoundingBox(this.placeSettings, this.templatePosition);
        int x0 = Math.max(box.minX(), placedBox.minX()), x1 = Math.min(box.maxX(), placedBox.maxX());
        int y0 = Math.max(box.minY(), placedBox.minY()), y1 = Math.min(box.maxY(), placedBox.maxY());
        int z0 = Math.max(box.minZ(), placedBox.minZ()), z1 = Math.min(box.maxZ(), placedBox.maxZ());
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int py = y0; py <= y1; ++py) {
            for (int px = x0; px <= x1; ++px) {
                for (int pz = z0; pz <= z1; ++pz) {
                    mp.set(px, py, pz);
                    BlockState st = level.getBlockState(mp);
                    if (st.is(TempleBlocks.DEMON_FRAME.get())) {
                        level.setBlock(mp, air, 2);
                    } else if (st.is(TempleBlocks.DEMON_GATE.get()) && (anchor == null || !anchor.equals(mp))) {
                        level.setBlock(mp, air, 2);
                    }
                }
            }
        }
    }
}
