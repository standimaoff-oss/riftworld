package ru.aiforge.riftworld.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import ru.aiforge.riftworld.registry.ModWorldgen;

/**
 * Part 7 «Храм полностью»: the overgrown temple of the COMMON world. A rare ruined ivory hall overgrown with ivy
 * and moss, holding the roaring demonic teleport - the way into the Rift. Quiet open terrain only (no water, no
 * cliffs, no mountainsides): the template is centred on the candidate chunk with a random rotation.
 */
public class OvergrownTempleStructure extends Structure {
    public static final MapCodec<OvergrownTempleStructure> CODEC =
            RecordCodecBuilder.mapCodec(i -> i.group(settingsCodec(i)).apply(i, OvergrownTempleStructure::new));

    public OvergrownTempleStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        int x = cp.getMiddleBlockX(), z = cp.getMiddleBlockZ();
        int[] hs = new int[9];
        int i = 0, sum = 0;
        for (int dx = -12; dx <= 12; dx += 12)
            for (int dz = -12; dz <= 12; dz += 12) {
                int h = ctx.chunkGenerator().getBaseHeight(x + dx, z + dz, Heightmap.Types.WORLD_SURFACE_WG,
                        ctx.heightAccessor(), ctx.randomState());
                hs[i++] = h;
                sum += h;
            }
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int h : hs) { min = Math.min(min, h); max = Math.max(max, h); }
        if (min < ctx.chunkGenerator().getSeaLevel()) return Optional.empty();      // no oceans/swamp ankles
        if (max - min > 10) return Optional.empty();                                // no cliff sides: it must stand pretty
        int y0 = min - 1;
        net.minecraft.world.level.block.Rotation rot = net.minecraft.world.level.block.Rotation.getRandom(ctx.random());
        BlockPos anchor = new BlockPos(x, y0, z);
        return Optional.of(new GenerationStub(anchor, builder -> builder.addPiece(
                new OvergrownTemplePiece(anchor, rot))));
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.OVERGROWN_TEMPLE.get();
    }
}
