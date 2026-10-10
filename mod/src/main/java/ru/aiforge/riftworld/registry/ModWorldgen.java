package ru.aiforge.riftworld.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.world.ElisarChunkGenerator;

public final class ModWorldgen {
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> GENERATORS = DeferredRegister.create(Registries.CHUNK_GENERATOR, RiftWorld.MODID);
    public static final RegistryObject<Codec<? extends ChunkGenerator>> ELISAR = GENERATORS.register("elisar", () -> ElisarChunkGenerator.CODEC);

    public static final DeferredRegister<Codec<? extends net.minecraft.world.level.biome.BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, RiftWorld.MODID);
    public static final RegistryObject<Codec<? extends net.minecraft.world.level.biome.BiomeSource>> ELISAR_BIOMES =
            BIOME_SOURCES.register("elisar", () -> ru.aiforge.riftworld.world.ElisarBiomeSource.CODEC);

    // Part 7 «Храм полностью»: the overgrown temple of the common overworld
    public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, RiftWorld.MODID);
    private static final net.minecraft.world.level.levelgen.structure.StructureType<ru.aiforge.riftworld.world.OvergrownTempleStructure> OGT_TYPE =
            () -> ru.aiforge.riftworld.world.OvergrownTempleStructure.CODEC.codec();
    public static final RegistryObject<net.minecraft.world.level.levelgen.structure.StructureType<ru.aiforge.riftworld.world.OvergrownTempleStructure>> OVERGROWN_TEMPLE =
            STRUCTURE_TYPES.register("overgrown_temple", () -> OGT_TYPE);

    public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType> PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, RiftWorld.MODID);
    public static final RegistryObject<net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType> OVERGROWN_TEMPLE_PIECE =
            PIECES.register("overgrown_temple", () -> ru.aiforge.riftworld.world.OvergrownTemplePiece::new);

    private ModWorldgen() {
    }
}
