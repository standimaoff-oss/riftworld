package ru.aiforge.riftworld.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/** Elisar biomes: Ashen Lands, Violet Hills, Sizo Grove, Waterfall Gardens, Crystal Rifts, Mist Keep, Sea, and City. */
public class ElisarBiomeSource extends BiomeSource {
    // ВНИМАНИЕ: порядок полей в RecordCodecBuilder обязан строго 1:1 совпадать с порядком аргументов конструктора!
    public static final Codec<ElisarBiomeSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            Biome.CODEC.fieldOf("default").forGetter(s -> s.def),
            Biome.CODEC.fieldOf("ashen").forGetter(s -> s.ashen),
            Biome.CODEC.fieldOf("violet").forGetter(s -> s.violet),
            Biome.CODEC.fieldOf("sizo").forGetter(s -> s.sizo),
            Biome.CODEC.fieldOf("gardens").forGetter(s -> s.gardens),
            Biome.CODEC.fieldOf("mist").forGetter(s -> s.mist),
            Biome.CODEC.fieldOf("reef").forGetter(s -> s.reef),
            Biome.CODEC.fieldOf("abyss").forGetter(s -> s.abyss),
            Biome.CODEC.fieldOf("crystal").forGetter(s -> s.crystal)).apply(i, ElisarBiomeSource::new));

    private final Holder<Biome> def, ashen, violet, sizo, gardens, mist, reef, abyss, crystal;

    public ElisarBiomeSource(Holder<Biome> def, Holder<Biome> ashen, Holder<Biome> violet, Holder<Biome> sizo, Holder<Biome> gardens,
                             Holder<Biome> mist, Holder<Biome> reef, Holder<Biome> abyss, Holder<Biome> crystal) {
        this.def = def;
        this.ashen = ashen;
        this.violet = prosec(violet);
        this.sizo = sizo;
        this.gardens = gardens;
        this.mist = mist;
        this.reef = reef;
        this.abyss = abyss;
        this.crystal = crystal;
    }

    private static Holder<Biome> prosec(Holder<Biome> h) {
        return h;
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(def, ashen, violet, sizo, gardens, mist, reef, abyss, crystal);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
        double x = qx * 4 + 2, z = qz * 4 + 2;
        CityPlan plan = CityPlan.get();
        CityPlan.Island at = plan.islandAt(x, z);
        if (at != null) {
            if (at.kind == WaterfallGardens.KIND && WaterfallGardens.landAt(x, z) != null) return gardens;
            if (at.kind == SizoGrove.KIND && SizoGrove.inside(x, z)) return sizo;
            if (at.kind == 7 && VioletHills.inside(x, z)) return violet;
            if (at.kind == CrystalRifts.KIND && CrystalRifts.landAt(x, z) != null) return crystal;
            if (at.kind == 5) return ashen;
        }
        boolean dry = at != null && qy * 4 > plan.ocean;
        if (dry) {
            if (at.kind == WaterfallGardens.KIND)
                return WaterfallGardens.landAt(x, z) != null ? gardens : reef;
            if (at.kind == SizoGrove.KIND) return sizo;
            if (at.kind == 7) return violet;
            if (at.kind == CrystalRifts.KIND)
                return CrystalRifts.landAt(x, z) != null ? crystal : def;
            if (at.kind == 5) return ashen;
        }
        if (qy * 4 > plan.ocean - 24 && AshenLands.inside(x, z)) return ashen;
        if (dry) return def;
        if (MistKeep.inside(x, z)) return mist;
        if (qy * 4 <= plan.ocean + 8) {
            int floor = ElisarChunkGenerator.oceanFloor(plan, (int) Math.floor(x), (int) Math.floor(z));
            if (ElisarSea.reefness((int) Math.floor(x), (int) Math.floor(x) * 0 + (int) Math.floor(z)) > 0.45) return reef;
            if (plan.ocean - floor > 46) return abyss;
        }
        return def;
    }
}
