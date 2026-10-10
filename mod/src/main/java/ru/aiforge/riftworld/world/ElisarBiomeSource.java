package ru.aiforge.riftworld.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/** Elisar biomes: the Ashen Lands around the castle, Violet Hills meadows, Sizo groves, the bright island sky everywhere else. */
public class ElisarBiomeSource extends BiomeSource {
    public static final Codec<ElisarBiomeSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            Biome.CODEC.fieldOf("default").forGetter(s -> s.def),
            Biome.CODEC.fieldOf("ashen").forGetter(s -> s.ashen),
            Biome.CODEC.fieldOf("violet").forGetter(s -> s.violet),
            Biome.CODEC.fieldOf("sizo").forGetter(s -> s.sizo),
            Biome.CODEC.fieldOf("mist").forGetter(s -> s.mist),
            Biome.CODEC.fieldOf("gardens").forGetter(s -> s.gardens)).apply(i, ElisarBiomeSource::new));

    private final Holder<Biome> def, ashen, violet, sizo, gardens, mist;

    public ElisarBiomeSource(Holder<Biome> def, Holder<Biome> ashen, Holder<Biome> violet, Holder<Biome> sizo, Holder<Biome> gardens, Holder<Biome> mist) {
        this.def = def;
        this.ashen = ashen;
        this.violet = violet;
        this.sizo = sizo;
        this.gardens = gardens;
        this.mist = mist;
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(def, ashen, violet, sizo, gardens, mist);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
        double x = qx * 4 + 2, z = qz * 4 + 2;
        if (AshenLands.inside(qx * 4 + 2, qz * 4 + 2)) return ashen;
        if (WaterfallGardens.inside(x, z)) return gardens;
        if (SizoGrove.inside(x, z)) return sizo;
        if (VioletHills.inside(x, z)) return violet;
        if (MistKeep.inside(x, z)) return mist;                    // мини-биом «Туманный данж»: дымка над столбами-останцами
        return def;
    }
}
