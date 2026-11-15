package ru.aiforge.riftworld.world;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * v0.55.0 — журнал построенных рынков Элисара.
 *
 * <p>Рынок строится один раз на площадь. Чтобы прилавки и торговцы не
 * копились при каждом посещении, список уже обстроенных площадей хранится
 * в мире и переживает перезагрузку.</p>
 */
public final class ElisarMarketData extends SavedData {

    private final Set<Long> builtPlazas = new HashSet<>();

    public static ElisarMarketData get(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(ElisarMarketData::load, ElisarMarketData::new, "riftworld_elisar_market");
    }

    public static ElisarMarketData load(CompoundTag tag) {
        ElisarMarketData data = new ElisarMarketData();
        for (long key : tag.getLongArray("BuiltPlazas")) {
            data.builtPlazas.add(key);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("BuiltPlazas", builtPlazas.stream().mapToLong(Long::longValue).toArray());
        return tag;
    }

    /** Рынок уже построен на этой площади? */
    public boolean isBuilt(int plazaX, int plazaZ) {
        return builtPlazas.contains(ChunkPos.asLong(plazaX >> 4, plazaZ >> 4));
    }

    /** Пометить площадь как обстроенную. */
    public void markBuilt(int plazaX, int plazaZ) {
        if (builtPlazas.add(ChunkPos.asLong(plazaX >> 4, plazaZ >> 4))) {
            setDirty();
        }
    }
}
