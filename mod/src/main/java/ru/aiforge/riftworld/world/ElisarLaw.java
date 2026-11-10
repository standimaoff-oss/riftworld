package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import ru.aiforge.riftworld.RiftWorld;

/**
 * v0.49.0 — «Закон Элисара»: розыск, предупреждения и тюрьма.
 *
 * <p>Правило простое и читаемое:</p>
 * <ul>
 *   <li>Удар по стражу или жителю — предупреждение. Страж отвечает
 *       предупредительным криком, но <b>не убивает</b>: он лишь отступает и
 *       зовёт подкрепление.</li>
 *   <li>Если смерть жителя или стража видел кто-то из стражей или горожан —
 *       убийца получает <b>розыск</b>: стражей переводят в погоню.</li>
 *   <li>Пойманного отправляют в <b>тюрьму</b> (клетку у города). Всё, что он
 *       держал, переносится в <b>сундук склада</b> рядом с тюрьмой, чтобы
 *       вещи не пропали и их можно было забрать позже.</li>
 * </ul>
 *
 * <p>Состояние хранится в мире, поэтому розыск и содержимое склада переживают
 * перезапуск.</p>
 */
public final class ElisarLaw extends net.minecraft.world.level.saveddata.SavedData {

    public static final String ID = RiftWorld.MODID + "_law";

    /** Состояние розыска одного игрока. */
    public static final class Wanted {
        /** Сколько горожан видели убийство. */
        public int witnesses;
        /** Метка времени начала розыска (game time). */
        public long since;
        /** Предупреждений за последнее время — до трёх предупреждений. */
        public int warnings;
        /** Срок розыска в тиках (5 минут). */
        public long expiresAt;
        /** Клетка (тюрьма), в которую отправят пойманного. */
        @Nullable
        public BlockPos jail;

        public boolean active(long now) {
            return now < expiresAt;
        }
    }

    /** Один отложенный при аресте предмет. */
    public record ItemRecord(String itemId, int count) {
    }

    private final Map<UUID, Wanted> wanted = new HashMap<>();
    /** Вещи, отложенные в складе при аресте: игрок -> предметы. */
    private final Map<UUID, List<ItemRecord>> confiscated = new HashMap<>();

    /** Имя файла сохранения в мире. */
    private static final String DATA_NAME = RiftWorld.MODID + "_law";

    /**
     * Закон города живёт в данных мира: и розыск, и вещи на складе должны
     * пережить перезапуск сервера, иначе игрок может обойти наказание,
     * просто перезайдя в игру.
     */
    public static ElisarLaw get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                ElisarLaw::load, ElisarLaw::new, DATA_NAME);
    }

    // ------------------------------------------------------------------
    // Розыск
    // ------------------------------------------------------------------

    /** Сообщить о преступлении: зафиксировать убийцу и начать розыск. */
    public void reportCrime(ServerLevel level, ServerPlayer criminal, int witnessCount, @Nullable BlockPos jail) {
        Wanted w = wanted.computeIfAbsent(criminal.getUUID(), k -> new Wanted());
        w.witnesses += Math.max(1, witnessCount);
        w.since = level.getGameTime();
        w.expiresAt = level.getGameTime() + 5L * 60L * 20L;
        w.jail = jail;
        setDirty();
    }

    /** Розыск активен? */
    public boolean isWanted(ServerLevel level, ServerPlayer player) {
        Wanted w = wanted.get(player.getUUID());
        if (w == null) {
            return false;
        }
        if (!w.active(level.getGameTime())) {
            wanted.remove(player.getUUID());
            setDirty();
            return false;
        }
        return true;
    }

    /** Заметки розыска игрока (для UI/сообщений). */
    @Nullable
    public Wanted wanted(ServerPlayer player) {
        return wanted.get(player.getUUID());
    }

    /** Снять розыск. */
    public void clear(ServerPlayer player) {
        if (wanted.remove(player.getUUID()) != null) {
            setDirty();
        }
    }

    /** Выдать предупреждение. Возвращает номер предупреждения. */
    public int warn(ServerPlayer player) {
        Wanted w = wanted.computeIfAbsent(player.getUUID(), k -> new Wanted());
        w.warnings++;
        setDirty();
        return w.warnings;
    }

    public int warnings(ServerPlayer player) {
        Wanted w = wanted.get(player.getUUID());
        return w == null ? 0 : w.warnings;
    }

    // ------------------------------------------------------------------
    // Конфискация при аресте
    // ------------------------------------------------------------------

    /** Отложить предметы арестованного в складе у тюрьмы. */
    public void confiscate(UUID player, List<ItemRecord> items) {
        if (items.isEmpty()) {
            return;
        }
        confiscated.computeIfAbsent(player, k -> new ArrayList<>()).addAll(items);
        setDirty();
    }

    /** Забрать отложенные предметы (когда игрок выходит из тюрьмы). */
    public List<ItemRecord> release(UUID player) {
        List<ItemRecord> out = confiscated.remove(player);
        if (out != null) {
            setDirty();
        }
        return out == null ? List.of() : out;
    }

    /** Что отложено у игрока (для подсказок в тюрьме). */
    public List<ItemRecord> peek(UUID player) {
        return confiscated.getOrDefault(player, List.of());
    }

    // ------------------------------------------------------------------
    // Сохранение
    // ------------------------------------------------------------------

    @Override
    public CompoundTag save(CompoundTag tag) {
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (Map.Entry<UUID, Wanted> e : wanted.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", e.getKey());
            t.putInt("witnesses", e.getValue().witnesses);
            t.putLong("since", e.getValue().since);
            t.putInt("warnings", e.getValue().warnings);
            t.putLong("expiresAt", e.getValue().expiresAt);
            if (e.getValue().jail != null) {
                t.putInt("jx", e.getValue().jail.getX());
                t.putInt("jy", e.getValue().jail.getY());
                t.putInt("jz", e.getValue().jail.getZ());
            }
            list.add(t);
        }
        tag.put("wanted", list);

        net.minecraft.nbt.ListTag conf = new net.minecraft.nbt.ListTag();
        for (Map.Entry<UUID, List<ItemRecord>> e : confiscated.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", e.getKey());
            net.minecraft.nbt.ListTag items = new net.minecraft.nbt.ListTag();
            for (ItemRecord r : e.getValue()) {
                CompoundTag it = new CompoundTag();
                it.putString("i", r.itemId());
                it.putInt("c", r.count());
                items.add(it);
            }
            t.put("items", items);
            conf.add(t);
        }
        tag.put("confiscated", conf);
        return tag;
    }

    public static ElisarLaw load(CompoundTag tag) {
        ElisarLaw law = new ElisarLaw();
        law.readFrom(tag);
        return law;
    }

    private void readFrom(CompoundTag tag) {
        wanted.clear();
        confiscated.clear();
        net.minecraft.nbt.ListTag list = tag.getList("wanted", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            Wanted w = new Wanted();
            w.witnesses = t.getInt("witnesses");
            w.since = t.getLong("since");
            w.warnings = t.getInt("warnings");
            w.expiresAt = t.getLong("expiresAt");
            if (t.contains("jx")) {
                w.jail = new BlockPos(t.getInt("jx"), t.getInt("jy"), t.getInt("jz"));
            }
            wanted.put(t.getUUID("id"), w);
        }
        net.minecraft.nbt.ListTag conf = tag.getList("confiscated", 10);
        for (int i = 0; i < conf.size(); i++) {
            CompoundTag t = conf.getCompound(i);
            List<ItemRecord> items = new ArrayList<>();
            net.minecraft.nbt.ListTag il = t.getList("items", 10);
            for (int j = 0; j < il.size(); j++) {
                CompoundTag it = il.getCompound(j);
                items.add(new ItemRecord(it.getString("i"), it.getInt("c")));
            }
            confiscated.put(t.getUUID("id"), items);
        }
    }
}
