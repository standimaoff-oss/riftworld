# Rev 8. Настоящий запуск Minecraft: сборка, сервер, «игра» через RCON

Дата: 2026-10-22. Ветка `arena/c5212c7f-riftworld`.

Раньше всё проверялось офлайн: парсеры, JSON-валидаторы, Java-стенды на стабах и рендеры. Этот прогон
добавляет то, чего не было: **мод собирается настоящим компилятором Java и грузится настоящим сервером
Forge 1.20.1, а проверки выполняются командами в живом мире**.

## 1. Что получилось в итоге

| Шаг | Команда | Результат |
|---|---|---|
| Сборка мода | `python3 tools/rift/realbuild/build_jar.py` | 109 исходников → 165 классов, **0 ошибок**; ECJ + реобфускация ART; jar 6.4 МБ / 2007 файлов |
| Подготовка сервера | `python3 tools/rift/realbuild/run_server.py --prepare` | Forge 1.20.1-47.4.3, RCON включён |
| Запуск + игра | `python3 tools/rift/realbuild/run_live.py` | `Done (13.558s)!`, затем **19/19** проверок, штатная остановка |
| Всё одной командой | `python3 tools/rift/check_all.py --live` | офлайн-набор + живой сервер |

Логи и машинный отчёт: `reports/rev8_playtest/server_run.log`, `reports/rev8_playtest/playtest.json`.
В логе сервера ровно одна `ERROR` — `Failed to request yggdrasil public key` (в песочнице нет сети до Mojang).
Ошибок мода нет.

## 2. Почему это было непросто (и как решено)

В песочнице нет ни JDK, ни доступа к Mojang/Forge maven, поэтому «просто запустить Gradle» невозможно.
Рабочая схема:

1. **JRE 17** — колесо `jdk4py==17.0.9.2` (Temurin 17.0.9). Java 25 из свежих колёс Forge 47 не подходит.
2. **Компилятор** — ECJ 3.46 из колеса `karellen-jdtls` (полный javac-совместимый фронтенд).
3. **Сервер** — `fetch_server.py` тянет `libraries/` реальной серверной установки Forge из git-репозитория
   `chisaato/BACraft-Server` (158.7 МБ, ~13 с).
4. **Классы Minecraft для компиляции** — dev-артефакт `forge-...-mapped_official_1.20.1.jar` (клиент+сервер,
   официальные имена, access transformers уже применены, 8612 классов) из git-кэша ForgeGradle:
   `fetch_devjar.py`.
5. **Имена** — `make_mappings.py` строит official↔srg из двух файлов серверной установки;
   **65 439 членов, 0 пропусков**.
6. **Реобфускация** — ForgeAutoRenamingTool в два прохода (см. ниже про «класс в себя»).

Три «засады», на которых всё вставало, и их причины:

* **ART не переименовывает члены, если класс маппится «сам в себя»** — поэтому маппинги применяются в два
  прохода через промежуточные srg-имена классов (`compile_pass1/2`, `reobf_pass1/2`).
* **ART нужна иерархия (`--lib`)** — без srg-jar/официального артефакта в `--lib` падает
  `NPE` на `Transformation.class` и молча оставляет члены непереименованными.
* **Дескрипторы в TSRG2 живут в официальном пространстве имён**, а `mcp_config` пишет их в обфусцированном.
  Пока это не учтено, терялись перегрузки: `Commands.literal` «не находился» у 5460 классов.

## 3. Как запускается сервер и что пришлось починить

`java -Xms512M -Xmx1400M @libraries/net/minecraftforge/forge/1.20.1-47.4.3/unix_args.txt nogui`

Правки, без которых старт невозможен именно в этой песочнице (код мода тут не виноват):

* `META-INF/coremods.json` удаляется из `forge-...-universal.jar` — JS-coremods Forge требуют `nashorn`;
* в nashorn-core снимается `requires jdk.dynalink` (модуля нет в урезанном JRE): `ModuleRequiresFix.java`
  (правка `module-info.class` через ASM). Просто подложить свой `jdk.dynalink` нельзя — jlink-образ
  проверяет хеш модуля и падает с `Hash of jdk.dynalink ... differs`;
* `online-mode=false`, версия-чекер и Yggdrasil просто пишут WARN;
* RCON (`enable-rcon=true`, порт 25575) — им и «играем»;
* сервер запускать только с **открытым stdin**: закрытие stdin Minecraft трактует как `stop`
  (из-за этого первый прогон оборвался посреди теста).

## 4. Что проверяет живой прогон (`playtest.py`)

19 проверок на настоящем сервере (`reports/rev8_playtest/playtest.json`):

* сервер отвечает, измерение `riftworld:elisar` загружено, команда мода `/riftarena` работает;
* **все 271 блок** из `blockstates/` мода реально регистрируются и ставятся в мире
  (ставим на каменную опору — лианам и растениям ванильный `setblock` иначе честно отвечает «Could not set»);
* опора моста создаёт блок-энтити (`data get block` возвращает NBT);
* все 4 породы деревьев (lilac / sizo / fern / mist_pine): ствол + листва ставятся;
* бревно мода попадает в `#minecraft:logs`;
* биом `riftworld:sizo_grove` находится `/locate biome`;
* **деревья действительно растут**: поперёк биома найдена листва, например `(752, 99, -2144)`, `(768, 102, -2144)`;
* генерация выдаёт непустые колонки, TPS 20.

## 5. Найденные и исправленные настоящие баги

Живой прогон нашёл то, что не видел ни один офлайн-стенд:

1. **`riftworld:elisar_water` ронял сервер.** `new LiquidBlock(ModFluids.SOURCE, ...)` выбирает Forge-конструктор
   `LiquidBlock(Supplier, Properties)`, который оставляет поле `fluid = null`; восемь методов
   (`neighborChanged`, `onPlace`, `updateShape`, `skipRendering`, `isPathfindable`, `pickupBlock`,
   `getPickupSound`, `shouldSpreadLiquid`) читают это поле напрямую. Первый же тик воды →
   `NullPointerException ... this.f_54689_ is null` → `Exception ticking world` → crash-report.
   Исправлено передачей готовой жидкости: `new LiquidBlock(ModFluids.SOURCE.get(), ...)`.
2. **`MistKeep.treesNear`** строил `CityPlan.Tree` из `double` (результат `hash()`), а конструктор ждёт `int` —
   не компилировалось вообще. Исправлено приведением типов.
3. **`BridgePostBlock.findRect`** возвращал `Deck` с 4 аргументами, а record требует 7 (стиль и обрыв
   добавляются выше) — не компилировалось. Теперь возвращается `BlockPos[4]`.
4. **`BridgePostRenderer`**: аргументы `tube(...)` в старом порядке, `VertexConsumer.overlay` вместо
   `overlayCoords`, `level.getLightColor(...)` вместо `LevelRenderer.getLightColor(...)`.

Пункты 2–4 — просто ошибки компиляции: без настоящего компилятора они жили в коде незамеченными.

## 6. Ограничения этого прогона

* **Клиента и графики нет** — сервер выделенный, без окна, поэтому «фотографий экрана» из него не получить.
  Визуальная часть по-прежнему подтверждается офлайн-рендерами (`reports/rev7_shots/`) и вашими фото.
* **Dynamic Trees в песочницу не поставить** — jar DT взять неоткуда (Mojang/CurseForge/Modrinth недоступны),
  поэтому аддон проверяется валидатором схемы и Java-стендом, а не живой загрузкой DT.
* Мир создаётся заново (`level-seed=424242`), тестовая зона — `riftworld:elisar` вокруг `(0..120, 90..110, 0..60)`.

## 7. Повторить

```bash
pip install --break-system-packages jdk4py==17.0.9.2 karellen-jdtls pillow
python3 tools/rift/realbuild/fetch_server.py            # 158.7 МБ libraries/
python3 tools/rift/realbuild/fetch_devjar.py            # 19.3 МБ dev-артефакт Forge (клиент+сервер)
python3 tools/rift/realbuild/run_server.py --prepare    # eula, server.properties, RCON, coremods.json
python3 tools/rift/realbuild/run_live.py --log-dir reports/rev8_playtest
# либо всё вместе:
python3 tools/rift/check_all.py --live
```
