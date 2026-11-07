# Первая партия моделей Riftworld через Blockbench API

**Ревизия 2 — детализация по концептам.** Силуэты и материалы пересобраны ближе к исходным изображениям:
изогнутая шея и веер перьев журавля, купольный панцирь с лавовыми трещинами жука, широкие крылья
с глазками моли, высокие уши и кристаллический хвост лиса. Геометрия теперь строится с шагом 64 единицы
на блок и экспортируется с `render_scale: 0.25`, поэтому видимый воксельный шаг — 1/32 блока, а загрузчик
`GeoModel.java` по-прежнему получает целочисленные кубы. Атласы увеличены до 2048×2048.

Сгенерировано не вручную в JSON, а через публичный API Blockbench 5.2.2 в headless Chromium:
`newProject(Formats.bedrock_old)`, `Group`, `Cube`, `Texture`, `Animation`, `Codecs.project.compile()`,
`Codecs.bedrock_old.compile({raw:true})`. Исходный плагин-генератор: `tools/blockbench/riftworld_fauna_plugin.js`.

## Модели

| Модель | Биом | Кости | Кубы | Атлас | Габарит, блоков (Ш×В×Д) |
|---|---:|---:|---:|---:|---:|
| Фарфоровый журавль | `elisar_city` | 12 | 1231 | 2048×2048 | 0.78 × 2.47 × 2.12 |
| Базальтовый жук-углеед | `ashen_lands` | 22 | 1086 | 2048×2048 | 1.69 × 1.19 × 2.06 |
| Пепельная моль | `ashen_lands` | 10 | 477 | 2048×2048 | 2.31 × 0.98 × 1.19 |
| Аметистовый лис | `violet_hills` | 9 | 309 | 2048×2048 | 0.59 × 1.61 × 2.30 |

**[Фотогалерея на GitHub](photos/README.md)** ·
[Сравнение концепт / модель](photos/05_reference_comparison.png) ·
[Все четыре модели — фото Blockbench](photos/00_all_models.png) ·
[ Журавль](photos/01_porcelain_crane.png) ·
[Жук](photos/02_basalt_beetle.png) ·
[Моль](photos/03_ash_moth.png) ·
[Лис](photos/04_amethyst_fox.png)

Фотографии сделаны через Blockbench `Screencam.advancedScreenshot`, разрешение 1600×1200, не в Minecraft.
Дополнительные проверочные ракурсы: [`00_overview.png`](00_overview.png) — лист оффлайн-рендеров ревизии 2.
Концепты не перегенерировались; использованы уже принятые
`reports/biome_mob_concepts/01_…04_….png`.

## Файлы

- Редактируемые Blockbench-проекты: `blockbench/riftworld_fauna/<id>.bbmodel`
  - текстура встроена data URL;
  - есть иерархия костей, box UV и idle-анимация ключевыми кадрами.
- Runtime-модели для текущего загрузчика мода: `mod/src/main/resources/assets/riftworld/geo/<id>.json`
  - формат `GeoModel.java`: Y вниз, нос в `-Z`, абсолютные pivot/origin, родители раньше детей,
    целочисленные размеры, box UV `[u,v]`, один синус на кость.
- Текстуры: `mod/src/main/resources/assets/riftworld/textures/entity/<id>.png`
- Промежуточный Bedrock export из Blockbench API: `<id>.bedrock.geo.json`
- Оффлайн-ракурсы: `<id>_model_sheet.png` — это программный рендер `tools/rift/preview_mob_model.py`,
  не скриншот Minecraft.
- Отчёт генерации и проверок: [`generation_report.json`](generation_report.json)

## Проверки

Для каждой модели выполнено:

1. Структурная проверка runtime geo: уникальные кости, родитель раньше ребёнка, положительные целые
   размеры, UV не выходит за атлас.
2. Round-trip через Blockbench: сохранённый `.bbmodel` заново распарсен `Codecs.project.parse`,
   текстура загрузилась, повторный `Codecs.bedrock_old.compile()` дал то же число костей/кубов и одну
   idle-анимацию.
3. Визуальная проверка оффлайн-рендером: все четыре модели отрисованы с четырьмя ракурсами.

## Границы этой партии

Это именно модели и ассеты. Java-сущности, рендереры, яйца призыва, AI, естественный спавн и баланс
мобов не менялись. Следующий шаг — подключить эти geo/текстуры к новым entity-классам и `GeoMobRenderer`,
если нужно увидеть их в игре.
