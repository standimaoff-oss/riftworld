package ru.aiforge.riftworld.registry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.RiftWorld;

/** Кастомные атмосферные частицы биомов Элисара (кружащиеся в воздухе листья/лепестки). */
public final class ModParticles {
    private ModParticles() {
    }

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, RiftWorld.MODID);

    /** Фиолетово-сиреневые листья/лепестки в небе Фиолетовых холмов (референс 04_violet_hills.png). */
    public static final RegistryObject<SimpleParticleType> VIOLET_LEAF =
            PARTICLES.register("violet_leaf", () -> new SimpleParticleType(true));

    /** Шалфейно-сизые листья/пылинки света в воздухе Рощи Сизо (референс 05_biome_sizo_grove.png). */
    public static final RegistryObject<SimpleParticleType> SIZO_LEAF =
            PARTICLES.register("sizo_leaf", () -> new SimpleParticleType(true));
}
