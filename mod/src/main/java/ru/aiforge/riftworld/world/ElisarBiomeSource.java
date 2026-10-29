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
    // Рев 14.35 — НАСТОЯЩАЯ ПРИЧИНА «/locate приводит в воду». Поля кодека связываются с
    // аргументами конструктора ПО ПОЗИЦИИ, а не по имени. Здесь «mist» стоял пятым, а в конструкторе
    // пятым идёт gardens: JSON отдавал mist_keep в поле gardens и waterfall_gardens в поле mist.
    // Итог: биом садов физически стоял над островами Туманного данжа (это и есть море!), а на самих
    // садах числился mist_keep. /locate biome waterfall_gardens честно находил ближайший «садовый»
    // биом — а это был островок данжа в открытом океане. Порядок теперь 1:1 с конструктором.
    public static final Codec<ElisarBiomeSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            Biome.CODEC.fieldOf("default").forGetter(s -> s.def),
            Biome.CODEC.fieldOf("ashen").forGetter(s -> s.ashen),
            Biome.CODEC.fieldOf("violet").forGetter(s -> s.violet),
            Biome.CODEC.fieldOf("sizo").forGetter(s -> s.sizo),
            Biome.CODEC.fieldOf("gardens").forGetter(s -> s.gardens),
            Biome.CODEC.fieldOf("mist").forGetter(s -> s.mist),
            Biome.CODEC.fieldOf("reef").forGetter(s -> s.reef),
            Biome.CODEC.fieldOf("abyss").forGetter(s -> s.abyss)).apply(i, ElisarBiomeSource::new));

    private final Holder<Biome> def, ashen, violet, sizo, gardens, mist, reef, abyss;

    public ElisarBiomeSource(Holder<Biome> def, Holder<Biome> ashen, Holder<Biome> violet, Holder<Biome> sizo, Holder<Biome> gardens,
                             Holder<Biome> mist, Holder<Biome> reef, Holder<Biome> abyss) {
        this.def = def;
        this.ashen = ashen;
        this.violet = violet;
        this.sizo = sizo;
        this.gardens = gardens;
        this.mist = mist;
        this.reef = reef;
        this.abyss = abyss;
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(def, ashen, violet, sizo, gardens, mist, reef, abyss);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
        double x = qx * 4 + 2, z = qz * 4 + 2;
        // Рев 14.28 — ПРЯМО ПО ЖАЛОБЕ ВЛАДЕЛЬЦА: «/locate biome waterfall_gardens находит не биом,
        // а структуру вообще другую». Причина была в том, что биом ставился по «региону» (круг/блоб),
        // а рельеф — по островам из CityPlan, и это два разных источника правды: биом повисал над
        // водой и над чужими островами, и /locate приводил в море. Теперь биом выводится ИЗ ТОГО ЖЕ
        // острова, что строит рельеф (plan.islandAt): нет острова — нет биома, есть остров — его биом.
        CityPlan plan = CityPlan.get();
        CityPlan.Island at = plan.islandAt(x, z);
        // Рев 14.46 — ПОЧЕМУ DYNAMIC TREES НЕ САЖАЛ ДЕРЕВЬЯ, даже когда вид зарегистрирован.
        // DynamicTreeFeature берёт биом для выбора вида на `new BlockPos(disc.x, 0, disc.z)` — то есть
        // на y=0, а не на поверхности! У нас ниже уровня моря море, поэтому DT видел «морской» биом,
        // для которого в базе нет ни вида, ни плотности, и молча возвращал UNHANDLED_BIOME:
        // ни деревьев в холмах/роще, ни диких кораллов на рифе.
        // Теперь на ГАРАНТИРОВАННОЙ суше острова (landAt — та же функция, что строит рельеф)
        // биом острова отдаётся на любой высоте: и DT получает нужный вид, и /locate остаётся на суше.
        if (at != null && WaterfallGardens.landAt(x, z) != null) {
            if (at.kind == WaterfallGardens.KIND) return gardens;
            if (at.kind == SizoGrove.KIND) return sizo;
            if (at.kind == 7) return violet;
        }
        // Биомы суши живут ТОЛЬКО выше уровня моря: у садов и рощи остров продолжается под водой
        // голыми каменными ярусами, и без этой проверки /locate biome отдавал точку в море —
        // владелец видел «находит не биом, а воду». Теперь ниже моря — морские биомы.
        boolean dry = at != null && qy * 4 > plan.ocean;
        if (dry) {
            // У садов внешние ярусы острова уходят ПОД воду (это те самые «1–2 пустых каменных
            // яруса»). Биом ставим только там, где рельеф ГАРАНТИРОВАННО выше моря — по той же
            // функции, которой генератор строит остров, с запасом 3 блока: /locate возвращает
            // блок на пару блоков в стороне от точки выборки биома, и без запаса он попадал
            // на кромку, где уже вода (владелец: «находит не биом, а воду»).
            if (at.kind == WaterfallGardens.KIND)
                return WaterfallGardens.landAt(x, z) != null ? gardens : def;
            if (at.kind == SizoGrove.KIND) return sizo;
            if (at.kind == 7) return violet;                       // луг Фиолетовых холмов
        }
        if (qy * 4 > plan.ocean - 24 && AshenLands.inside(x, z)) return ashen;
        if (dry) return def;                                       // острова города, замка, руин и мелкие островки
        if (MistKeep.inside(x, z)) return mist;                    // мини-биом «Туманный данж»: дымка над столбами-останцами
        // Рев 8 «Живое море»: открытый океан получает собственные биомы по глубине и «рифовости» дна.
        // Считаем только там, где есть смысл (у воды и ниже): небо над морем остаётся обычным.
        if (qy * 4 <= plan.ocean + 8) {
            int floor = ElisarChunkGenerator.oceanFloor(plan, (int) Math.floor(x), (int) Math.floor(z));
            if (ElisarSea.reefness((int) Math.floor(x), (int) Math.floor(z)) > 0.45) return reef;   // коралловые отмели
            if (plan.ocean - floor > 46) return abyss;                                              // тёмные глубины
        }
        return def;
    }
}
