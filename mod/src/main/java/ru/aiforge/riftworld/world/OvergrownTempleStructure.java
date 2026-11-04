package ru.aiforge.riftworld.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import ru.aiforge.riftworld.registry.ModWorldgen;

/**
 * v0.24.0: Abandoned Portal Observatory of the Overworld ({@code riftworld:overgrown_temple}).
 * Spawns strictly on dry, gently sloped Overworld land (rejects oceans, rivers, and steep cliffs)
 * and contains the complete ritual sanctum + supplies to awaken the White 3D Spatial Breach into Elisar.
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
        boolean isFlat = ctx.chunkGenerator() instanceof net.minecraft.world.level.levelgen.FlatLevelSource;
        int sea = ctx.chunkGenerator().getSeaLevel();
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;

        for (int dx = -15; dx <= 15; dx += 15) {
            for (int dz = -15; dz <= 15; dz += 15) {
                int ws = ctx.chunkGenerator().getBaseHeight(x + dx, z + dz, Heightmap.Types.WORLD_SURFACE_WG,
                        ctx.heightAccessor(), ctx.randomState());
                int of = ctx.chunkGenerator().getBaseHeight(x + dx, z + dz, Heightmap.Types.OCEAN_FLOOR_WG,
                        ctx.heightAccessor(), ctx.randomState());
                if (!isFlat && (ws <= sea + 1 || of < ws)) {
                    return Optional.empty();
                }
                min = Math.min(min, ws);
                max = Math.max(max, ws);
            }
        }
        if (max - min > 13) return Optional.empty();

        int centerH = ctx.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                ctx.heightAccessor(), ctx.randomState());
        int y0 = Math.max(min, centerH - 1);
        Rotation rot = Rotation.getRandom(ctx.random());
        BlockPos anchor = new BlockPos(x, y0, z);
        return Optional.of(new GenerationStub(anchor, builder -> builder.addPiece(
                new OvergrownTemplePiece(anchor, rot))));
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.OVERGROWN_TEMPLE.get();
    }
}
