# riftworld_dt — дополнение к Dynamic Trees

Data-only мод: ни одного класса, только дерево-пак Dynamic Trees (`trees/riftworld_dt/`) и ресурсы семян.
Если Dynamic Trees не установлен, пак ничего не делает (`only_if_loaded`), а мод просто грузится пустым.
Собран и проверен против **Dynamic Trees 1.4.11** для Minecraft 1.20.1 (ветка `release/1.20.1`).

## Породы

Деревья строит сам Dynamic Trees. Наш вклад — параметры силуэта, текстуры коры/спила и листвы, семена.
Если DT не установлен, мод выращивает деревья своим встроенным движком, так что биомы не пустеют.

| id | порода | биом | cell kit | growth logic | energy / up / ветки с / taper / радиус / rate |
|----|--------|------|----------|--------------|-----------------------------------------------|
| `riftworld_dt:lilac` | Сиреневое древо | `riftworld:violet_hills` | deciduous | по умолчанию | 16.0 / 3 / 5 / 0.34 / 7 / 0.72 |
| `riftworld_dt:sizo_v2` | Сизо (второе поколение) | `riftworld:sizo_grove` | dark_oak | dark_oak | 24.0 / 5 / 9 / 0.26 / 24 / 0.6 |
| `riftworld_dt:fern_tree` | Древовидный папоротник | `riftworld:waterfall_gardens` | palm | palm | 7.0 / 1 / 2 / 0.4 / 6 / 1.15 |
| `riftworld_dt:mist_pine` | Туманная сосна | `riftworld:mist_keep` | conifer | conifer | 14.0 / 4 / 3 / 0.3 / 12 / 0.66 |

Породы босса/приспешников и «Адские угодья» не затрагиваются: в `ashen_lands` и `elisar_city` прописано
`species: null`, там растут только собственные обгоревшие деревья мода.

## Почему ключи именно такие

Ключи, типы и kit'ы сверены с исходниками Dynamic Trees: `python3 tools/rift/fetch_dt_schema.py` собирает
`tools/rift/dt_schema.json` (ключи лоадеров + собственный пак DT + типы значений + типы почв), а
`python3 tools/rift/check_dt_pack.py` проверяет пак против него — включая ссылки на блоки, биомы, текстуры,
модели семян, `world_gen` и диапазоны версий в `mods.toml`. У валидатора есть самопроверка мутациями.

## Сборка

Пак генерируется скриптом: `python3 tools/rift/gen_dt_addon.py` (идемпотентно; спрайты семян детерминированы).
Дальше это обычный Forge-проект: `cd dtcompat && ./gradlew build` (нужен интернет для Forge/Mojang).
Файл `libs/DynamicTrees-1.20.1-*.jar` для компиляции **не нужен** — в моде нет кода, обращающегося к API
Dynamic Trees, только ресурсы, поэтому несовпадение версий API пак не ломает.

Версии в `mods.toml` берутся из `gradle.properties`: `dt_addon_version` — версия аддона, `mod_version` мода —
нижняя граница зависимости `riftworld`. Руками их править не надо (иначе Forge откажется грузить аддон).
