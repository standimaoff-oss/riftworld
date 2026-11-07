# Headless Blockbench API runner

Генератор первых моделей Riftworld fauna через настоящий Blockbench API, а не через ручную сборку
`.bbmodel`/geo JSON.

## Состав

- `riftworld_fauna_plugin.js` — плагин/библиотека Blockbench. Строит модели через `Group`, `Cube`,
  `Texture`, `Animation`, экспортирует `.bbmodel` через `Codecs.project.compile()` и Bedrock geometry
  через `Codecs.bedrock_old.compile({raw:true})`, затем конвертирует Bedrock Y-up в Riftworld Y-down.
  Ревизия 2 строит геометрию с шагом 64 единицы на блок и ставит `render_scale: 0.25`, сохраняя
  целочисленные кубы для `GeoModel.java`; атласы 2048×2048.
- `run_headless.mjs` — поднимает локальный HTTP-сервер для собранного Blockbench web, запускает
  headless Chromium, инжектит плагин, пишет артефакты и делает round-trip проверку `.bbmodel`.
- `take_photos.mjs` — открывает сохранённые проекты и снимает их через
  `Screencam.advancedScreenshot`; создаёт четыре PNG 1600 × 1200 и общий лист.
- `package.json` / `package-lock.json` — зависимости runner: `puppeteer-core`, `@sparticuz/chromium`.

## Требования

- Node.js >= 22.17.
- Собранный Blockbench web 5.2.2: каталог с `index.html` и `dist/bundle.js`.
- Для фотографий: ImageMagick 6 (`convert` и `montage`) и шрифт DejaVu Sans.

Сборка Blockbench из официального исходника:

```bash
# распаковать/склонировать JannisX11/blockbench v5.2.2
cd blockbench-5.2.2
npm ci --ignore-scripts --no-audit --no-fund
node build.js --target=web
```

## Запуск

```bash
cd tools/blockbench
npm ci --ignore-scripts --no-audit --no-fund
BLOCKBENCH_ROOT=/path/to/blockbench-5.2.2 node run_headless.mjs
# только часть моделей:
BLOCKBENCH_ROOT=/path/to/blockbench-5.2.2 node run_headless.mjs --ids=porcelain_crane,ash_moth
```

По умолчанию runner ищет Blockbench в `../.cache/riftworld-blockbench/blockbench-5.2.2` относительно
корня репозитория и пишет результаты в корень репозитория:

- `blockbench/riftworld_fauna/*.bbmodel`
- `mod/src/main/resources/assets/riftworld/geo/*.json`
- `mod/src/main/resources/assets/riftworld/textures/entity/*.png`
- `reports/blockbench_first_batch/*`

### Фотографии опубликованных моделей

После генерации запустите из корня репозитория:

```bash
BLOCKBENCH_ROOT=/path/to/blockbench-5.2.2 node tools/blockbench/take_photos.mjs
```

Снимки сохраняются в `reports/blockbench_first_batch/photos/`. Галерея с прямыми
ссылками на PNG: [`reports/blockbench_first_batch/photos/README.md`](../../reports/blockbench_first_batch/photos/README.md).
Фотографии делаются в Blockbench, а не в Minecraft.

## Ручное использование плагина

`riftworld_fauna_plugin.js` можно загрузить в Blockbench как plugin from file. После загрузки доступен
`window.RiftworldFauna`:

```js
await RiftworldFauna.generateAll();
await RiftworldFauna.generateOne('amethyst_fox');
```

Возвращает объекты с `bbmodel`, `geo`, `texture` data URL, `bedrock` и `stats`.

## Примечания

- Runner блокирует все внешние запросы страницы; безобидная ошибка старта `Object: Object` от probing
  plugin CDN пишется в `benign_startup_errors`, а не в `page_errors`.
- Runner явно распаковывает NSS/NSPR-библиотеки из `al2023.tar.br` комплекта
  `@sparticuz/chromium`, чтобы Chromium запускался не только на Amazon Linux.
- Runtime-анимация в `geo/*.json` намеренно ограничена одним синусом на кость, как требует
  `GeoModel.java`; более богатые ключевые кадры сохраняются в `.bbmodel`.
