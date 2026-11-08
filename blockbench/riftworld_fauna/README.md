# Riftworld Fauna — Blockbench MCP 3D Models (Hytale / Articulated Style)

Серия детализированных шарнирных 3D-мобов для **Riftworld**, собранных и провалидированных через официальный **Blockbench MCP** (`blockbench-mcp-plugin`, формат `.bbmodel` v5.0 + Bedrock `.geo.json` / `.animation.json` + движковый `GeoModel` JSON + текстурные атласы `512×512` 32 px/block).

Вместо ступенчатых единичных вокселей модели построены в стиле **Hytale / Blockbench**: из крупных прямоугольных брусков, копланарных пластин, наклонных анатомических сегментов с чистыми шарнирами (`pivot` + Euler `ZYX`) и детализированной пиксель-арт росписью по граням.

---

## Выпуск 1 (Модели 01–03)

![Витрина моделей 01–03 (тёмный фон)](../../reports/blockbench_mcp/batch_01/00_batch_01_showcase_dark.png)

| № | ID | Название | Биом | Кости | Прямоугольные элементы | Blockbench (`.bbmodel`) | Bedrock Geo & Anim | Riftworld Geo & Texture | MCP Валидация |
| :-: | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- | :-: |
| **01** | `porcelain_crane` | **Фарфоровый журавль** | `riftworld:elisar_city` | 9 | 71 | [`porcelain_crane.bbmodel`](./porcelain_crane.bbmodel) | [`geo`](./bedrock/porcelain_crane.geo.json) · [`anim`](./bedrock/porcelain_crane.animation.json) | [`geo`](../../mod/src/main/resources/assets/riftworld/geo/porcelain_crane.json) · [`png`](../../mod/src/main/resources/assets/riftworld/textures/entity/porcelain_crane.png) | `0 errors / 0 warnings` |
| **02** | `basalt_beetle` | **Базальтовый жук-углеед** | `riftworld:ashen_lands` | 13 | 99 | [`basalt_beetle.bbmodel`](./basalt_beetle.bbmodel) | [`geo`](./bedrock/basalt_beetle.geo.json) · [`anim`](./bedrock/basalt_beetle.animation.json) | [`geo`](../../mod/src/main/resources/assets/riftworld/geo/basalt_beetle.json) · [`png`](../../mod/src/main/resources/assets/riftworld/textures/entity/basalt_beetle.png) | `0 errors / 0 warnings` |
| **03** | `ash_moth` | **Пепельная моль** | `riftworld:ashen_lands` | 12 | 69 | [`ash_moth.bbmodel`](./ash_moth.bbmodel) | [`geo`](./bedrock/ash_moth.geo.json) · [`anim`](./bedrock/ash_moth.animation.json) | [`geo`](../../mod/src/main/resources/assets/riftworld/geo/ash_moth.json) · [`png`](../../mod/src/main/resources/assets/riftworld/textures/entity/ash_moth.png) | `0 errors / 0 warnings` |

Подробный отчёт Партии 1: **[`reports/blockbench_mcp/batch_01/README.md`](../../reports/blockbench_mcp/batch_01/README.md)**.

---

## Выпуск 2 (Модели 04–06)

![Витрина моделей 04–06 (тёмный фон)](../../reports/blockbench_mcp/batch_02/00_batch_02_showcase_dark.png)

| № | ID | Название | Биом | Кости | Прямоугольные элементы | Blockbench (`.bbmodel`) | Bedrock Geo & Anim | Riftworld Geo & Texture | MCP Валидация |
| :-: | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- | :-: |
| **04** | `amethyst_fox` | **Аметистовый лис** | `riftworld:violet_hills` | 10 | 52 | [`amethyst_fox.bbmodel`](./amethyst_fox.bbmodel) | [`geo`](./bedrock/amethyst_fox.geo.json) · [`anim`](./bedrock/amethyst_fox.animation.json) | [`geo`](../../mod/src/main/resources/assets/riftworld/geo/amethyst_fox.json) · [`png`](../../mod/src/main/resources/assets/riftworld/textures/entity/amethyst_fox.png) | `0 errors / 0 warnings` |
| **05** | `sizo_leafwing` | **Сизый листокрыл** | `riftworld:sizo_grove` | 12 | 89 | [`sizo_leafwing.bbmodel`](./sizo_leafwing.bbmodel) | [`geo`](./bedrock/sizo_leafwing.geo.json) · [`anim`](./bedrock/sizo_leafwing.animation.json) | [`geo`](../../mod/src/main/resources/assets/riftworld/geo/sizo_leafwing.json) · [`png`](../../mod/src/main/resources/assets/riftworld/textures/entity/sizo_leafwing.png) | `0 errors / 0 warnings` |
| **06** | `azure_frog` | **Лазурный древесный лягуш** | `riftworld:waterfall_gardens` | 8 | 68 | [`azure_frog.bbmodel`](./azure_frog.bbmodel) | [`geo`](./bedrock/azure_frog.geo.json) · [`anim`](./bedrock/azure_frog.animation.json) | [`geo`](../../mod/src/main/resources/assets/riftworld/geo/azure_frog.json) · [`png`](../../mod/src/main/resources/assets/riftworld/textures/entity/azure_frog.png) | `0 errors / 0 warnings` |

Подробный отчёт Партии 2: **[`reports/blockbench_mcp/batch_02/README.md`](../../reports/blockbench_mcp/batch_02/README.md)**.
