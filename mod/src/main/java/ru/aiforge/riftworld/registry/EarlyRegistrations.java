package ru.aiforge.riftworld.registry;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Рев 13 — <b>ранняя регистрация блоков</b> (лечение падения аддона Dynamic Trees).
 *
 * <p>Dynamic Trees читает свои {@code trees/*.json} прямо в обработчике {@link RegisterEvent}
 * для реестра блоков (см. {@code DTTrees#loadResources}). Обработчик висит на обычном приоритете,
 * а мы подключаемся к тому же событию — значит, к моменту чтения JSON у DT уже должны быть
 * <b>наши</b> блоки ({@code riftworld:sizo_log}, {@code riftworld:elisar_grass}, …).
 *
 * <p>Раньше это было не так: {@link DeferredRegister} тоже регистрирует на обычном приоритете,
 * но DT загружается как зависимость <i>до</i> нас. В итоге аддон видел вместо наших блоков
 * {@code minecraft:air}:
 * <ul>
 *   <li>{@code leaves_properties} с «пустыми» листьями → «не найдены LeavesProperties
 *       для {@code riftworlddt:sizo}/{@code riftworlddt:ambra}»;</li>
 *   <li>исключение на {@code primitive_soil} → {@code SoilProperties.block} остаётся {@code null}
 *       → {@link NullPointerException} в {@code SoilProperties#getBlock()} при регистрации
 *       TileEntities, то есть краш, о котором писал заказчик.</li>
 * </ul>
 *
 * <p>Решение: регистрируем блоки на приоритете {@link EventPriority#HIGHEST}. Вызов
 * {@code DeferredRegister#addEntries} приватный (так же поступает и сам DT в своём
 * {@code RegistryHandler}), поэтому берём его рефлексией — один раз в статическом блоке.
 */
public final class EarlyRegistrations {
    private static final Method ADD_ENTRIES;

    static {
        try {
            ADD_ENTRIES = DeferredRegister.class.getDeclaredMethod("addEntries", RegisterEvent.class);
            ADD_ENTRIES.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Не удалось получить DeferredRegister#addEntries", e);
        }
    }

    private EarlyRegistrations() {
    }

    /**
     * Подключает {@code registers} к шине так, чтобы записи попадали в реестр раньше всех
     * (раньше Dynamic Trees, раньше остальных модов).
     */
    @SafeVarargs
    public static void registerFirst(IEventBus bus, DeferredRegister<?>... registers) {
        bus.addListener(EventPriority.HIGHEST, false, (RegisterEvent event) -> {
            for (DeferredRegister<?> register : registers) {
                if (event.getRegistryKey() != register.getRegistryKey()) continue;
                try {
                    ADD_ENTRIES.invoke(register, event);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    throw new RuntimeException("Ранняя регистрация " + register.getRegistryKey().location() + " провалилась", cause);
                }
            }
        });
    }
}
