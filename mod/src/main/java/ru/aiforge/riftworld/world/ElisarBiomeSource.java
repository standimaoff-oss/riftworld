package ru.aiforge.riftworld.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/** Elisar biomes: the Ashen Lands around the castle, the bright island sky everywhere else. */
public class ElisarBiomeSource extends BiomeSource {
    public static final Codec<ElisarBiomeSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            Biome.CODEC.fieldOf("default").forGetter(s -> s.def),
            Biome.CODEC.fieldOf("ashen").forGetter(s -> s.ashen)).apply(i, ElisarBiomeSource::new));

    private final Holder<Biome> def, ashen;

    public ElisarBiomeSource(Holder<Biome> def, Holder<Biome> ashen) {
        this.def = def;
        this.ashen = ashen;
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(def, ashen);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
        return AshenLands.inside(qx * 4 + 2, qz * 4 + 2) ? ashen : def;
    }
}
