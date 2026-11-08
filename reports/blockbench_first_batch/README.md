# Одиннадцать моделей Riftworld через Blockbench API

**Обновление: листокрыл, ворон и серебряный морской змей.** У листокрыла переработаны
многолопастные крылья, лапы и изогнутый хвост; у ворона — перья, каркас крыльев и рёбра.
Новый морской змей имеет S-образное тело, тёмную полосу на спине и светлый гребень.
Модели приближены к концептам, но **не являются их точной копией**: формы и позы отличаются.
Авторинг — 64 единицы на блок, `render_scale: 0.25`, атласы 2048×2048.
Светлые точки кальмара — обычные яркие текстуры, а не подтверждённое свечение или прозрачность.

Сгенерировано не вручную в JSON, а через публичный API Blockbench 5.2.2 в headless Chromium:
`newProject(Formats.bedrock_old)`, `Group`, `Cube`, `Texture`, `Animation`, `Codecs.project.compile()`,
`Codecs.bedrock_old.compile({raw:true})`. Исходный плагин-генератор: `tools/blockbench/riftworld_fauna_plugin.js`.

## Модели

| Модель | Биом | Кости | Кубы | Атлас | Габарит, блоков (Ш×В×Д) |
|---|---:|---:|---:|---:|---:|
| Фарфоровый журавль | `elisar_city` | 27 | 415 | 2048×2048 | 0.80 × 2.42 × 2.00 |
| Базальтовый жук-углеед | `ashen_lands` | 34 | 186 | 2048×2048 | 1.44 × 1.22 × 1.91 |
| Пепельная моль | `ashen_lands` | 10 | 66 | 2048×2048 | 2.77 × 1.23 × 1.09 |
| Аметистовый лис | `violet_hills` | 23 | 91 | 2048×2048 | 0.47 × 1.58 × 2.11 |
| Сизый листокрыл | `sizo_grove` | 44 | 206 | 2048×2048 | 4.50 × 2.58 × 2.38 |
| Лазурный древесный лягуш | `waterfall_gardens` | 12 | 159 | 2048×2048 | 1.27 × 0.98 × 1.16 |
| Костяной ворон | `morok_hollow` | 77 | 94 | 2048×2048 | 2.98 × 1.72 × 1.50 |
| Коралловый рак-отшельник | `shallow_reef` | 26 | 78 | 2048×2048 | 1.59 × 1.30 × 1.27 |
| Рифовый морской конёк | `shallow_reef` | 13 | 50 | 2048×2048 | 0.58 × 1.69 × 1.27 |
| Глубоководный кальмар-призрак | `abyss` | 20 | 85 | 2048×2048 | 0.75 × 0.41 × 3.09 |
| Серебряный морской змей | `maja_ocean` | 54 | 82 | 2048×2048 | 0.92 × 1.73 × 4.56 |

Габариты в таблице вычислены по исходным коробкам **до поворотов костей**; это не точный габарит позы.

**[Фотогалерея на GitHub](photos/README.md)** ·
[Сравнение концепт / модель](photos/05_reference_comparison.png) ·
[Все одиннадцать моделей — фото Blockbench](photos/00_all_models.png) ·
[ Журавль](photos/01_porcelain_crane.png) ·
[Жук](photos/02_basalt_beetle.png) ·
[Моль](photos/03_ash_moth.png) ·
[Лис](photos/04_amethyst_fox.png) ·
[Листокрыл](photos/05_sizo_leafwing.png) ·
[Лягуш](photos/06_azure_frog.png) ·
[Ворон](photos/07_bone_raven.png) ·
[Рак](photos/08_coral_hermit_crab.png) ·
[Конёк](photos/09_reef_seahorse.png) ·
[Кальмар](photos/10_ghost_squid.png) ·
[Морской змей](photos/11_silver_sea_serpent.png) ·
[Три модели и их концепты](photos/12_three_reference_comparison.png) ·
[Сравнение последних пяти с концептами](photos/11_latest_reference_comparison.png)

Фотографии сделаны через Blockbench `Screencam.advancedScreenshot`, разрешение 1600×1200, не в Minecraft.
Дополнительные проверочные ракурсы: [журавль спереди](photos/06_crane_front.png) / [сбоку](photos/07_crane_side.png), [жук спереди](photos/08_beetle_front.png) / [сбоку](photos/09_beetle_side.png).
Концепты не перегенерировались; использованы уже принятые
`reports/biome_mob_concepts/01_…06_….png` и `11_…15_….png`.

## Файлы

- Редактируемые Blockbench-проекты: `blockbench/riftworld_fauna/<id>.bbmodel`
  - текстура встроена data URL;
  - есть иерархия костей, box UV и idle-анимация ключевыми кадрами.
- Runtime-модели для текущего загрузчика мода: `mod/src/main/resources/assets/riftworld/geo/<id>.json`
  - формат `GeoModel.java`: Y вниз, нос в `-Z`, абсолютные pivot/origin, родители раньше детей,
    целочисленные размеры, box UV `[u,v]`, один синус на кость.
- Текстуры: `mod/src/main/resources/assets/riftworld/textures/entity/<id>.png`
- Промежуточный Bedrock export из Blockbench API: `<id>.bedrock.geo.json`
- Листы `<id>_model_sheet.png`: три фотографии Blockbench, не скриншоты Minecraft.
- Отчёт генерации и проверок: [`generation_report.json`](generation_report.json)

## Проверки

Для каждой модели выполнено:

1. Структурная проверка runtime geo: уникальные кости, родитель раньше ребёнка, положительные целые
   размеры, UV не выходит за атлас.
2. Round-trip через Blockbench: сохранённый `.bbmodel` заново распарсен `Codecs.project.parse`,
   текстура загрузилась, повторный `Codecs.bedrock_old.compile()` дал то же число костей/кубов и одну
   idle-анимацию.
3. `python3 tools/blockbench/check_geometry.py` проверяет преобразование статических поворотов
   всех вершин деталей из Blockbench в runtime geo и разделение усиков и рогов жука по Z
   при выборке колебаний усиков от −4° до +4° с шагом 0,25°.
4. Все одиннадцать моделей визуально проверены в Blockbench с трёх ракурсов. Проверка в Minecraft
   не проводилась: модели ещё не подключены к сущностям.

## Границы этой партии

Это именно модели и ассеты. Java-сущности, рендереры, яйца призыва, AI, естественный спавн и баланс
мобов не менялись. Следующий шаг — подключить эти geo/текстуры к новым entity-классам и `GeoMobRenderer`,
если нужно увидеть их в игре.
