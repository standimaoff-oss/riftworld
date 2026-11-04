package ru.aiforge.riftworlddt;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Рев 15 — АДДОН К DYNAMIC TREES для мода «Разлом».
 *
 * <p>Почти весь аддон — данные: в jar лежит папка {@code trees/riftworlddt/}, которую Dynamic Trees
 * сканирует у всех модов (семейства, виды, листва, грунты, jo_codes и правила генерации
 * {@code world_gen/default.json}).
 *
 * <p><b>Что было не так (жалоба владельца: «аддон не меняет ничего» и «вылет»):</b>
 * <ol>
 *   <li><b>Вылет.</b> В {@code mods.toml} Dynamic Trees и «Разлом» были объявлены
 *       {@code mandatory=true}: если DT не установлен (или стоит другой версии), Forge
 *       останавливал запуск с экраном «Missing dependencies». Теперь оба — {@code mandatory=false},
 *       аддон просто тихо спит.</li>
 *   <li><b>Вылет №2.</b> Любая проблема с API DT приводила к {@link IllegalStateException}
 *       в конструкторе мода — то есть к крашу на загрузке. Теперь всё внутри try/catch:
 *       не получилось — пишем предупреждение и живём дальше (деревья просто не появятся).</li>
 *   <li><b>«Ничего не меняет».</b> Правила генерации были написаны полями, которых в DT нет
 *       ({@code "force"}, а {@code density} — массивом). DT такие файлы не применяет. Теперь
 *       формат строго по вики: {@code select} → {@code apply: {species, density (число), chance}}.</li>
 * </ol>
 *
 * <p>Регистрация блоков аддона (ветки/листва/саженцы) идёт через {@code RegistryHandler.setup(id)}:
 * без него DT переименовывает namespace всех своих блоков в {@code dynamictrees}, и наши
 * blockstate-файлы не находятся.
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
            Object handler = handlerCls.getMethod("get", String.class).invoke(null, MOD_ID);
            boolean valid = handler != null && (Boolean) handler.getClass().getMethod("isValid").invoke(handler);
            if (valid) {
                LOG.info("[riftworlddt] RegistryHandler setup OK: блоки/предметы аддона идут в namespace '{}'", MOD_ID);
            } else {
                // Рев 15: это НЕ повод падать. Блоки уйдут в namespace dynamictrees — будет некрасиво,
                // но мир загрузится; в логе останется понятная причина.
                LOG.warn("[riftworlddt] RegistryHandler не зарегистрировался — блоки DT будут в namespace "
                        + "'dynamictrees' (наши blockstate не найдутся). Аддон работает в щадящем режиме.");
            }
        } catch (Throwable t) {
            LOG.warn("[riftworlddt] API Dynamic Trees недоступен ({}): аддон работает в щадящем режиме, "
                    + "деревья DT в Элисаре могут не появиться", t.toString());
        }
    }

    // Рев 14.41: поиск грунта для нашего измерения регистрирует ОСНОВНОЙ мод
    // (ru.aiforge.riftworld.world.DtGround). В аддоне этого делать нельзя: его jar
    // собирается задачей dtJar без реобфускации, и обращения к полям Minecraft
    // падают с NoSuchFieldError (проверено прогоном 192).
}
