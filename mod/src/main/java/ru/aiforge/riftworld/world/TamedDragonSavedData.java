package ru.aiforge.riftworld.world;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Глобальный реестр приручённых Бумажных Драконов сервера.
 * Позволяет Горну Дракона ({@code dragon_whistle}) призывать приручённого дракона с небес к хозяину
 * ДАЖЕ ЗА ПРЕДЕЛАМИ СИМУЛЯЦИИ (из выгруженных чанков за тысячи блоков или другого измерения),
 * а также предотвращает дублирование при последующей прогрузке старого чанка ({@code recallGen}).
 */
public class TamedDragonSavedData extends SavedData {
    private static final String DATA_NAME = "riftworld_tamed_dragons";

    public static final class Entry {
        public UUID dragonId;
        public UUID ownerId;
        public int whiskerType;
        public int saddleTier;
        public float health;
        public String customNameJson = "";
        public int recallGen;

        public Entry(UUID dragonId, UUID ownerId, int whiskerType, int saddleTier, float health, String customNameJson, int recallGen) {
            this.dragonId = dragonId;
            this.ownerId = ownerId;
            this.whiskerType = whiskerType;
            this.saddleTier = saddleTier;
            this.health = health;
            this.customNameJson = customNameJson == null ? "" : customNameJson;
            this.recallGen = recallGen;
        }
    }

    private final Map<UUID, Entry> entries = new HashMap<>();

    public static TamedDragonSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                TamedDragonSavedData::load, TamedDragonSavedData::new, DATA_NAME);
    }

    public static TamedDragonSavedData load(CompoundTag tag) {
        TamedDragonSavedData data = new TamedDragonSavedData();
        ListTag list = tag.getList("Dragons", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); ++i) {
            CompoundTag c = list.getCompound(i);
            if (!c.hasUUID("DragonId") || !c.hasUUID("OwnerId")) continue;
            UUID dId = c.getUUID("DragonId");
            UUID oId = c.getUUID("OwnerId");
            Entry e = new Entry(
                    dId, oId,
                    c.getInt("Whisker"),
                    c.getInt("Saddle"),
                    c.contains("Health") ? c.getFloat("Health") : 60.0F,
                    c.getString("CustomName"),
                    c.getInt("RecallGen"));
            data.entries.put(dId, e);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Entry e : entries.values()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("DragonId", e.dragonId);
            c.putUUID("OwnerId", e.ownerId);
            c.putInt("Whisker", e.whiskerType);
            c.putInt("Saddle", e.saddleTier);
            c.putFloat("Health", e.health);
            if (!e.customNameJson.isEmpty()) c.putString("CustomName", e.customNameJson);
            c.putInt("RecallGen", e.recallGen);
            list.add(c);
        }
        tag.put("Dragons", list);
        return tag;
    }

    public void update(UUID dragonId, UUID ownerId, int whiskerType, int saddleTier, float health, String customNameJson, int recallGen) {
        Entry prev = entries.get(dragonId);
        int gen = prev != null ? Math.max(prev.recallGen, recallGen) : recallGen;
        entries.put(dragonId, new Entry(dragonId, ownerId, whiskerType, saddleTier, health, customNameJson, gen));
        setDirty();
    }

    public void remove(UUID dragonId) {
        if (entries.remove(dragonId) != null) {
            setDirty();
        }
    }

    @Nullable
    public Entry getEntry(UUID dragonId) {
        return entries.get(dragonId);
    }

    @Nullable
    public Entry findForOwner(UUID ownerId, @Nullable UUID preferredDragonId) {
        if (preferredDragonId != null) {
            Entry pref = entries.get(preferredDragonId);
            if (pref != null && ownerId.equals(pref.ownerId)) {
                return pref;
            }
        }
        Entry fallback = null;
        for (Entry e : entries.values()) {
            if (ownerId.equals(e.ownerId)) {
                fallback = e;
            }
        }
        return fallback;
    }

    public Entry bumpRecallAndRebind(Entry oldEntry, UUID newDragonId) {
        int nextGen = oldEntry.recallGen + 1;
        oldEntry.recallGen = nextGen;
        if (!oldEntry.dragonId.equals(newDragonId)) {
            Entry fresh = new Entry(newDragonId, oldEntry.ownerId, oldEntry.whiskerType,
                    oldEntry.saddleTier, oldEntry.health, oldEntry.customNameJson, nextGen);
            entries.put(newDragonId, fresh);
            setDirty();
            return fresh;
        }
        setDirty();
        return oldEntry;
    }
}
