package ru.aiforge.riftworlddt;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Рев 13 — АДДОН К DYNAMIC TREES для мода «Разлом».
 *
 * <p>Почти весь аддон — данные: в jar лежит папка {@code trees/riftworlddt/}, которую Dynamic Trees
 * сканирует у всех модов. Оттуда берутся семейства, виды, свойства листьев/грунта, формы крон
 * и правила, где деревья растут.
 *
 * <p><b>Что было сломано и что теперь сделано.</b>
 * <ol>
 *   <li><b>Краш NPE в {@code SoilProperties#getBlock()}.</b> DT читает JSON в момент регистрации
 *       блоков и требует, чтобы «примитивные» блоки мода (<code>riftworld:sizo_log</code>,
 *       <code>riftworld:elisar_grass</code>) уже были в реестре. Мы регистрируем их на приоритете
 *       HIGHEST (см. {@code ru.aiforge.riftworld.registry.EarlyRegistrations}).</li>
 *   <li><b>«Не найдены LeavesProperties для riftworlddt:sizo / riftworlddt:ambra».</b> Причина та же:
 *       {@code primitive_leaves} указывали на ещё не зарегистрированные блоки, DT подставлял
 *       {@code minecraft:air}, свойства листьев считались недействительными, и виды оставались без листвы.</li>
 *   <li><b>Блоки DT регистрировались в namespace {@code dynamictrees}.</b> Без вызова
 *       {@code RegistryHandler.setup(modid)} DT принудительно переименовывает namespace любого
 *       своего блока в {@code dynamictrees} ({@code correctRegistryName}). Из-за этого не находились
 *       наши blockstate-файлы (они лежат в {@code assets/riftworlddt}) и не работал
 *       {@code give riftworlddt:sizo_seed}. Теперь setup вызывается рефлексией (чтобы аддон
 *       собирался и без DT в classpath) и <b>проверяется</b>: если хендлер не зарегистрировался,
 *       аддон падает с понятным сообщением, а не заливает лог 255 «missing model».</li>
 * </ol>
 */
@Mod(RiftworldDt.MOD_ID)
public class RiftworldDt {
    public static final String MOD_ID = "riftworlddt";
    private static final Logger LOG = LoggerFactory.getLogger("riftworlddt");

    public RiftworldDt() {
        if (!ModList.get().isLoaded("dynamictrees")) {
            LOG.warn("[riftworlddt] Dynamic Trees не найден — аддон простаивает (данные в trees/ никому не нужны)");
            return;                     // аддон без DT просто ничего не делает
        }
        try {
            // Точка входа DT для аддонов: свой RegistryHandler → наши блоки/предметы
            // регистрируются под namespace riftworlddt (а не dynamictrees).
            Class<?> handlerCls = Class.forName("com.ferreusveritas.dynamictrees.api.registry.RegistryHandler");
            handlerCls.getMethod("setup", String.class).invoke(null, MOD_ID);
            // Проверяем, что хендлер реально встал в реестр DT: иначе correctRegistryName()
            // переименует ВСЕ наши блоки в dynamictrees:* и ассеты не найдутся.
            Object handler = handlerCls.getMethod("get", String.class).invoke(null, MOD_ID);
            boolean valid = handler != null && (Boolean) handler.getClass().getMethod("isValid").invoke(handler);
            if (!valid) {
                throw new IllegalStateException("RegistryHandler для '" + MOD_ID + "' не зарегистрировался");
            }
            LOG.info("[riftworlddt] RegistryHandler setup OK: блоки/предметы аддона идут в namespace '{}'", MOD_ID);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Dynamic Trees найден, но API RegistryHandler недоступен "
                    + "(версия DT несовместима с аддоном " + MOD_ID + ")", e);
        }
    }
}
