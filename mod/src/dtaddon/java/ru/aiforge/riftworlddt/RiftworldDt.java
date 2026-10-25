package ru.aiforge.riftworlddt;

import net.minecraftforge.fml.common.Mod;

/**
 * Рев 11 — АДДОН К DYNAMIC TREES для мода «Разлом».
 *
 * <p>Сам аддон почти целиком состоит из данных: в jar лежит папка {@code trees/riftworlddt/},
 * которую Dynamic Trees сканирует у всех модов (см. {@code Resources.addModResourcePack}).
 * Оттуда берутся:
 * <ul>
 *   <li>{@code families/*.json} — семейства (ветка + листва + примитивное бревно);</li>
 *   <li>{@code species/*.json} — виды деревьев Элисара: сизо, амбра и сизо-великан;</li>
 *   <li>{@code leaves_properties/*.json} и {@code soil_properties/*.json} — наши листья и грунт; </li>
 *   <li>{@code jo_codes/*.txt} — формы крон (тот же формат, что у ванильных дубов);</li>
 *   <li>{@code world_gen/elisar.json} — какие виды растут в биомах Элисара.</li>
 * </ul>
 * Java здесь нужна только как точка входа мода: класс ничего не регистрирует вручную,
 * поэтому аддон не ломается при обновлении Dynamic Trees.
 */
@Mod(RiftworldDt.MOD_ID)
public class RiftworldDt {
    public static final String MOD_ID = "riftworlddt";

    public RiftworldDt() {
        // всё содержимое — в ресурсах; DT подхватит их сам при загрузке мира
    }
}
