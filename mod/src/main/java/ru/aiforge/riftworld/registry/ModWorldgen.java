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

    private ModWorldgen() {
    }
}
