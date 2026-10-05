# «Разлом» (riftworld) 0.1.0, часть 1: лаборатория

Minecraft 1.20.1 · Forge 47.4.23 · Java 17 · без миксинов и внешних зависимостей.

## Что в этой части
* **Блоки лаборатории (35, все текстуры 16×16, собственные):** панели (чистые, грязные, с полосой), плитка (целая и треснувшая), стальной и ржавый настил, бетон (целый и треснувший), фасад, потолок (чистый и грязный), предупредительные полосы (новые и стёртые), стена шахты, решётки (металл и ржавчина, cutout), стекло (целое и треснувшее), потолочная и аварийная лампы (`lit`), труба (по 3 осям), обломки, мёртвый мох, мёртвые лианы (по ним можно лазить), ступени и плиты.
* **3D-модели (18 GLB из Blender, `.blend` и `.glb`, текстуры упакованы):** руль запуска 3×2×3 (5 гнёзд, колесо, экраны), рычаг запуска, пульт, токарный и фрезерный станки, колба с мёртвым растением (целая и разбитая), электрощит, генератор, трансформатор, серверная стойка, пост охраны (6 мониторов), деревянный и металлический ящики (по 27 слотов, лут-таблица), раздвижная дверь 2×3 (анимация, открывается правым кликом или редстоуном), лабораторный стол, стеллаж, офисное кресло, шкафчик.
* **Мультиблоки:** невидимые блоки-части с коллизией. Клик и ломание любой части передаются мастер-блоку. Перед установкой проверяется свободное место. Повороты в структуре работают (проверено при повороте на 180°).
* **Сюжетная механика:**
  * В руле уже стоит 1 камень из 5.
  * Камень резонанса вставляется правым кликом, сообщение «Камень установлен (n/5)».
  * Рычаг с неполным набором камней: анимация рукояти, звук, сообщение и падающая сверху записка «не хватает камней, найдите и вставьте недостающие» с подсказкой о четырёх местах.
  * При 5/5 колесо руля начинает вращаться, сообщение «Лаборатория просыпается…».
* **Структура `riftworld:laboratory`** (63×47×70, генерируется в равнинах, лесах, тайге, саванне, пустыне и на лугах; spacing 48):
  * **Подвал-склад:** стеллажи, ящики, генератор, трубы. Камень 1 лежит в металлическом ящике в юго-западном углу.
  * **Первый этаж:**
    * Зал 27×27 высотой до крыши, со световым фонарём.
    * Шахта 11×11 глубиной 30 блоков под решёткой, с аварийными лампами по стенам.
    * Помост с рулём, рычагом и пультами.
    * Техотдел (станки, колбы с растениями; камень 2), электроотдел (щиты, трансформаторы, генератор; камень 3), северные лаборатории, вестибюль с постом охраны.
  * **Второй этаж:** галерея над залом, комната охраны (камень 4), офисы, серверная, архив.
  * **Две лестницы** (северо-западная и северо-восточная), от подвала до 2-го этажа.
  * **Снаружи:** входная группа с козырьком, оборудование на крыше.
  * **Следы запустения:** грязные и треснувшие варианты блоков, обломки, мох, лианы, мёртвые лампы, отдельные красные аварийные.

## Проверено в игре
Сервер и клиент Forge, `/place structure riftworld:laboratory`, плюс естественная генерация (`/locate` находит). Скриншоты лежат в `release/riftworld_screens/` (для читаемости тёмных помещений включено ночное зрение).
* Рычаг: сообщение и выпадение записки работают.
* Записка с русским текстом открывается.
* Вставка камней: 4/5 → 5/5, кристаллы появляются в гнёздах.
* Ящик с камнем открывается.

## Сборка
`MODID=riftworld bash tools/build.sh` → `release/riftworld-0.1.0.jar`.

Генераторы (Python и Blender):
* `tools/rift/textures.py` — пиксельные текстуры;
* `tools/rift/models.py` — модели, запускается в Blender с `--factory-startup`;
* `tools/rift/gen_resources.py` — blockstates, модели, лут, теги, языки;
* `tools/rift/gen_lab.py` — NBT структуры и JSON генерации мира.

## Следующие части (план)
2. Мерцание и включение света при 5/5, сфера из шахты с автоматическими дверями, «очищающая полоса».
3. Консоль в сфере, луч, кат-сцена полёта, измерение «Элисар»: город «Странники» на 5–7 островах, жители, валюта «кенли», глаз в небе.
4. Биомы: Звонкая роща, Разлом, Гремучая роща.

---

## Part 2 (v0.2.0) — decor, capsule, launch, cleaning wave, video

### New 3D models (Blender → .blend + .glb, `tools/rift/models.py`)
| model | cells | notes |
|---|---|---|
| trash_bin_tipped | 1×1 | bin on its side, lid, spilled trash/papers/cans |
| chandelier | 1×1 (ceiling) | ring with 6 shades; `lamp_on` node + light 15 when powered |
| siren | wall | spinning `rotor` + blinking red lamp while the launch runs |
| shelf_fallen | 3×1 | storage shelf lying on its back, boxes spilled |
| wall_notes | wall | cork board, notes, a photo, red string; no collision |
| desk_computer | 2×1×2 | desk with drawers, CRT (live/dead screen), keyboard, phone, mug |
| table_flipped | 2×1 | lab table on its side, beakers and papers |
| barrels | 1×1 | upright + lying drum, puddle |
| fire_extinguisher | wall | hose, gauge, wall bracket |
| box_pile / paper_pile / chair_fallen | 1×1 | edge clutter |
| capsule | 11×11×10 | platform 10.4 m, glass sphere R 4.5 with 5 steel rings, vestibule + 2 sliding iron doors, console (monitor + PC + stool), `lights_on` |
| shaft_hatch | 11×11 | 2 leaves with grate texture (alpha MASK), ribs, hinges; leaves rotate down to 90° |
| scan_ring | — | emissive band, scaled by the renderer for the cleaning wave |

### New blocks
dirt_pile (carpet, cleaned by the wave), lab_railing (iron-bars type, gallery), lab_tile_stairs (separate side texture, stairwells), wall_vent, launch_core (shaft-centre anchor, BE + renderer), capsule_terminal (invisible console hitbox).

### Logic
- **Helm 5/5** → `LabPower`: scans the building (±42, −9..+15), lamps + chandeliers flash at ticks 8/11/20/23/25/28 and stay on from 44; then all machines become POWERED (screens, LEDs). In the POWERED state basement lamps randomly go out for 2–10 ticks. State is saved in the BE NBT (`Stage`, `StageTime`).
- **Lever**: <5 stones → note book (as before); powering → "wait for the light"; powered → `LaunchCoreBlockEntity.startLaunch()`.
- **Launch** (`LaunchCoreBlockEntity`):
  - 0–60 ticks: hatch leaves open; at 12 the lid's invisible blocks are removed.
  - 60–260: capsule rises 29 blocks (smoothstep); clouds at the shaft walls; RAID_HORN.
  - At 260 it lands: invisible collision cells for the platform, sphere and vestibule; door cells open automatically when a player is within 3.2 blocks; the terminal is placed.
  - The cleaning wave runs at 0.3 block/tick out to 46 blocks, y −8..+16, from a precomputed offset list sorted by distance (only about 90 columns per tick).
  - It removes only dead_moss, dead_vines, dirt_pile, cobweb and vine, and washes the `*_dirty` panels and ceilings. Debris, cracked tiles, fallen shelves and flipped tables are not touched.
- Sirens spin during the launch and turn off after the wave.

### Lab v2
- The shaft lid is now invisible part blocks plus launch_core in the centre; the renderer draws the lid.
- Added 25 chandeliers, 7 sirens, 7 notice boards, 8 extinguishers, 4 fallen shelves, 4 flipped tables, 7 desks, 6 barrel groups, 7 fallen chairs and 11 bins.
- Edge clutter pass: about 45 paper piles and 30 box piles placed against walls only, and only where 2 free cells remain for the walkway.
- Also: about 300 dirt piles, 38 wall vents, a railing around the gallery, and tile stairs in the stairwells.

### Verification
- Build OK. Server + client test in a flat world at `place template riftworld:laboratory 0 -63 0`.
- Checked: power-up flicker, launch, hatch, rise, landing, wave (Wave index 6625 = all columns done, Stage 4), auto doors, terminal message.
- Video `riftworld_part2.mp4` (2:14, 800×480, 15 fps, no sound): camera path by `tools/rift/director.py` (RCON tp at 20 Hz + xdotool clicks), captured with ffmpeg x11grab, Russian captions via drawtext.


## Part 3 — dimension «Элисар», city «Странники» (v0.3.0)

### Dimension
- `riftworld:elisar`: `dimension_type/elisar.json` (min_y 0, height 256, fixed sky light, no ceiling) and `dimension/elisar.json` with the custom chunk generator `riftworld:elisar_city` (`ElisarChunkGenerator`, codec registered in `ModWorldgen`). Biome `elisar_city` sets blue-green foliage and a pale sky.
- **Ocean**: a flat 1-block plane of `elisar_water` at y 62 on an `elisar_seabed` floor. It lies 35–50 blocks under the islands, everywhere, including under the islands. The texture is translucent, animated, deep blue with sun glints on wave crests. It uses its own fluid (`ModFluids`) on the translucent render layer.
- **Islands** (`CityPlan`, loaded from `data/riftworld/city/plan.json`): 6 floating islands, together 0.2–0.28 km across.

| island | centre | R | top y |
|---|---|---|---|
| main (lab, market, guard post, tavern, shop, houses) | 0,0 | 90 | 104 |
| temple | 142,−111 | ~60 | 112 |
| fishers | 171,43 | ~55 | 99 |
| homes (river + pond) | 19,181 | ~60 | 106 |
| forge | −171,56 | ~55 | 108 |
| farms | −96,−143 | ~55 | 103 |

- **Island shape**:
  - noisy rim with gentle hills;
  - plots flattened with a smoothstep blend;
  - grass over 3 rows of soil over stone;
  - a cone underside (depth·t^0.55 plus noise) with stalactites and moss near the bottom.
- **Streets**:
  - cobble main ring and radials, dirt paths with ragged grass edges, plank decks;
  - plazas;
  - **half-slabs** wherever a street climbs one block;
  - trimmed turf around the houses.
- **Bridges**: 6 rope suspension bridges with 26–34-block spans and a sagging plank deck (half-slabs on steps). Rope railings on both sides, log posts every 6 blocks.
- **Water**:
  - 1 river across the homes island into a pond; it leaves over the rim as a **waterfall** down to the ocean;
  - 13 rim springs, each with its own small waterfall;
  - roads cross water on plank fords.
- **Native trees** (523):
  - сизо: blue-grey bark, teal tiered cone;
  - амбра: red-brown bark, amber round crown.
- **Ground cover**: tall grass and flowers. Every block in the dimension is a custom 16×16 block.

### City «Странники»
- **37 plots, 12 building templates** (`structures/city/*.nbt`, generated by `tools/rift/gen_city.py`). Placement is varied: rotated to face the street, mixed over the islands.

| template | content |
|---|---|
| laboratory | twin of the lab in the island centre (ring road around it); for now the same template, the cleaned "intact" variant comes with the flight in Part 3b |
| police (guard post) | 2 floors; jail cell with bars and door; weapon racks; wanted posters; **bounty board** outside with reward posters on named people |
| tavern | 2 floors; bar counter, keg rack, stools, tables, fireplace; guest rooms upstairs |
| shop ×2 | counter, shelves, crates, hanging sign |
| fisher_house ×3 | **barrels of Elisar fish**, fish drying rack, net, rowboat |
| hunter_house | antler trophy, hide rack, fireplace |
| forge | open front, hearth (coals glow), anvil stump, quench barrel, tool wall |
| temple | pagan temple; idol ring + fire bowls on the plaza; altar stone, sacred poles |
| house_a…d | 1–2-floor homes: beds, wardrobes, tables, rugs, cooking pots, fireplaces |

- **Street props** (222):
  - 118 street lamps (light 15);
  - market: 8 stalls (incl. fish stall), well, benches, signposts;
  - **7 horseless overgrown wagons** standing still;
  - crates, barrels, benches;
  - hanging shop signs (tavern, shop, forge, fish, guard, hunter, temple, market).
- **NEW `lantern_hanging`**: iron ring chandelier with 4 crowned candle cups (modelled after reference photos of wrought-iron tavern chandeliers). Blender model, light 14. It is auto-hung in every room of every house (1–6 per building) below the first solid block over a clear column.
- 45 Elisar 3D models in total (Blender, `.blend` + `.glb`). Each is one block-entity model with invisible part blocks for multi-cell footprints.
- No people yet (villagers and кенли come later, as requested).

### Generation pipeline
- `gen_city.py` → templates + `plan.json`.
- `ElisarChunkGenerator.fillFromNoise`:
  - builds an 18×18 column cache per chunk (island height, plot flattening, roads, bridges, water);
  - fills the blocks;
  - grows the trees.
- `applyBiomeDecoration`:
  - pastes the templates clipped to the current chunk (`StructureTemplate.placeInWorld`);
  - places props through the 32-block spatial grid of `CityPlan`.
- Deterministic and seed-independent: the city is the same in every world.

### Verification
- Build OK. Server + client: `/execute in riftworld:elisar run tp @s -34 110 58` (market).
- Checked: islands, ocean plane, waterfalls, bridges, slab steps, all templates and interiors (chandeliers lit), props, signs, wagons, temple, forge, fishers, hunter, night lighting.
- No errors in the server log.
- Video `riftworld_part3.mp4` (`tools/rift/city_tour.py`: per-segment warm-up, RCON camera, x11grab, captions) and screenshots `p3_*.jpg`.

### Next (Part 3b)
Flight: confirm on the capsule console → the sphere rises in real time along a multicolour laser that wraps the bottom 20% of the sphere. Earth from orbit → deep space → approach to Элисар; no breaking or exiting; 15 s; landing in the intact lab twin in «Странники».
