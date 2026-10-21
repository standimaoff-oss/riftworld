# riftworld_dt — дополнение к Dynamic Trees

Data-only мод: ни одного класса, только дерево-пак Dynamic Trees (`trees/riftworld_dt/`) и ресурсы семян.
Если Dynamic Trees не установлен, пак ничего не делает (`only_if_loaded`), а мод просто грузится пустым.

## Породы

| id | порода | биом | cell kit | growth logic |
|----|--------|------|----------|--------------|
| `riftworld_dt:lilac` | сиреневое древо | `riftworld:violet_hills` | deciduous | по умолчанию |
| `riftworld_dt:sizo_v2` | сизо второго поколения (толстое) | `riftworld:sizo_grove` | deciduous | dark_oak |
| `riftworld_dt:fern_tree` | древовидный папоротник | `riftworld:waterfall_gardens` | palm | palm |
| `riftworld_dt:mist_pine` | туманная сосна | `riftworld:mist_keep` | conifer | conifer |

Породы босса/приспешников и «Адские угодья» не затрагиваются: в `ashen_lands` и `elisar_city` прописано
`species: null`, там растут только собственные обгоревшие деревья мода.

## Сборка

Пак генерируется скриптом: `python3 tools/rift/gen_dt_addon.py`.
Дальше это обычный Forge-проект: `cd dtcompat && ./gradlew build` (нужен интернет для Forge/Mojang).
Файл `libs/DynamicTrees-1.20.1-*.jar` для компиляции **не нужен** — в моде нет кода, обращающегося к API
Dynamic Trees, только ресурсы, поэтому несовпадение версий API пак не ломает.
