package ru.aiforge.riftworld.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import ru.aiforge.riftworld.registry.ModWorldgen;

/**
 * v0.26.0: Регистрационная запись аналитических структур измерения Элисар для полной поддержки
 * ванильной команды {@code /locate structure riftworld:<id>} и автодополнения в чате.
 * Сами блоки ставятся аналитически в {@link ElisarChunkGenerator}, поэтому {@link #findGenerationPoint}
 * возвращает {@link Optional#empty()}, исключая дублирование генерации.
 */
public final class ElisarAnalyticalStructure extends Structure {
    public static final Codec<ElisarAnalyticalStructure> CODEC = RecordCodecBuilder.create(i -> i.group(
            settingsCodec(i),
            Codec.STRING.fieldOf("kind").forGetter(ElisarAnalyticalStructure::kind)
    ).apply(i, ElisarAnalyticalStructure::new));

    private final String kind;

    public ElisarAnalyticalStructure(StructureSettings settings, String kind) {
        super(settings);
        this.kind = kind;
    }

    public String kind() {
        return kind;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        return Optional.empty();
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.ELISAR_ANALYTICAL.get();
    }
}
