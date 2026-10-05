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

## Part 3 revision 1 (v0.3.1): matching the user references

| Request | What was done |
|---|---|
| White/bleached look as in the references | All Elisar block and model textures re-coloured to the pale palette (grass #d6d0c2, plaster #e9e4d8, bricks #ddd6c6, roofs #b8ad98/#8e8d88, dark-grey undersides #4a4a4d). Custom `DimensionSpecialEffects riftworld:elisar`: no clouds, fog pulled to cream, fog starts at 45 % of render distance. Biome sky #d7e1e6, fog #ece8de. |
| Many more houses | 5 new templates: `house_e` (stone base + plank upper storey + balcony on posts), `house_f` (cottage), `house_g` (narrow two-storey), `house_h` (wide stone house), `house_i` (timber + amber planks + balcony). Fill pass along every street, plus a second row on lanes: **127 buildings** (was 37). Trees 523 → 137. |
| More things inside (paintings, kitchen…) | 15 new Blender models: 3 lanterns (hanging/post/wall), 3 paintings, kitchen stove, kitchen counter, dish cupboard, bookshelf, sacks, firewood stack, fire pit, flower pot, drying herbs. An auto-furnisher puts a kitchen corner, cupboards, books, sacks, flowers, 1–2 paintings and herbs into every storey of every house (walkways kept clear). Outside: a wall lantern by the door, a flower pot on the step, firewood against a side wall. |
| Everything as blocks, not entities | All 60 Elisar props are now **baked block models** (Forge OBJ loader, `RenderShape.MODEL`, no block entity). Exported from the Blender `.blend` by `tools/rift/obj_export.py`; UVs normalised per face into [0,1] (atlas sprites do not tile). Emissive lamp/flame materials through the MTL Ka. `_off` variants for powered=false. Lab machines keep their animated GLB renderers. |
| Waterfall hangs past the island | Falling water only in the first void column next to land; the channel banks are raised to river level up to the rim, so the water runs to the edge and drops straight down. The same rule applies to the 13 rim springs. |
| Water: only colour, like a reflection | Flat sky-reflection colour (164,212,232), no wave pattern. |
| Reference details | Chain railing (`elisar_chain`, vanilla ChainBlock class, own texture) between bridge posts every 4 blocks. Lantern posts along streets (124) and at bridge ends. Fire-pit plazas with 4 benches on each satellite island. Floating 2×2 debris cubes around the rims. |

Screenshots: `release/riftworld_screens/p3r_01…p3r_08_*.jpg`.

**Next (revision 2):** the giant eye + hand in the sky, the inverted floating tower, the rising water spiral with fish, and a check against `loc12_void`. **Then:** the flight (Part 3b).

## Revision 2 (v0.4.0): the village reference, wild islands, the tower, the eye, the flight

### Islands and ground
- Layering under the surface: the top block is the surface (grass/moss/cobble/path), then **one pale crust block** (`elisar_crust`), then grey stone straight away (mossy near the bottom). The old 3-block soil layer is gone, so the stone now starts right under the top.
- Moss patches (`elisar_moss`) from value noise, a pebble apron (cobble) around every house plot, tall grass about 9 %, fewer flowers.
- **Wild islands** (`WildIslands`, 160-block cells, kept 300+ blocks clear of the city): large (R 40–62), medium (17–30), small (7–13) and islets (3–6), each with its own depth and hill profile, plus grove trees.
- Debris: swarms of small cubes (1×1, some 2×2) in crust, stone, gravel, grass, mossy stone and tower bricks. They are densest right at the island rims and around the tower.

### Village details (from `struct1_village.png`)
- Pale wooden fences along the terrace edges of city islands near houses and streets.
- Bridges: a 3-wide crosswise plank deck with no middle posts and two chains on each side (at deck level and at hand height). At both ends stand pale stone pillars (`elisar_stone_wall` ×2) with an `elisar_lantern` on top.
- Street lamps are now block stacks (wall ×2 + lantern) instead of a model.
- Every house template has hanging lanterns on chains at its front eave corners (gen_city `eave_lanterns`).
- 30 pale wheat fields (`pale_wheat` on dark soil with a moss border) next to houses, many of them on the farm island. More crates, sacks, barrels and hide racks: 450 props in total.
- New blocks: `elisar_crust`, `elisar_moss`, `tower_bricks`, `tower_bricks_cracked`, `elisar_lantern` (standing and hanging), `elisar_stone_wall`, `pale_wheat`. All are custom 16×16 textures.

### Sky
- **Inverted tower** at (−104, 22), built from dark stone bricks:
  - a downward spike from y 148;
  - an overhanging battlement ring;
  - an octagonal hollow shaft with slit windows and string courses;
  - an upper ring and a spire up to about y 250.
- **The eye** (`ElisarEye`, client only, Elisar only): a faint pixel-line eye in the north-east sky, drawn behind everything so it can never be reached.
  - It blinks every 9 s and is closed with hanging lashes at night (13000–23000).
  - Its iris follows the player.
  - There is no hand.

### Flight (15 s, no cutscenes)
- Pressing the capsule console asks for confirmation. A second press within 5 s starts the flight. This works only in the overworld, with the capsule landed and the player inside it.
- **LIFT (2 s):** the capsule lifts 1.6 blocks on the laser.
  - A rainbow band wraps the bottom 20 % of the sphere.
  - Six twisting coloured strands plus a white core run down the shaft and up into the sky.
  - The camera rumbles, then flashes white.
- **TRANSIT (10 s):** a void dimension `riftworld:transit` with black fog and no sky. The capsule GLB floats there, and the space around it is drawn without depth:
  - 900 stars;
  - Earth (textured, sun-lit, with an atmosphere glow and the Moon) filling the view ahead and below, then shrinking and drifting away;
  - the beam still tying the capsule to Earth;
  - the jump, where stars stretch into colour streaks;
  - Elisar growing ahead from a dot until it fills the view.
- **ARRIVE (3 s):** the capsule rises up the shaft of the Elisar lab, lands, and the doors and the cleaning wave work as usual.
- While flying, the player cannot move, break, use, attack or take damage, and the first-person hand is hidden.
  - The server owns the timeline and resyncs the client every 5 ticks.
  - The client clock never runs ahead of the server.
  - The loading screens of the dimension change are replaced with plain white.
- The Elisar lab chunks are generated in the background (tickets) during the flight, so the server does not stall.
- Logging out mid-flight never leaves a player in the transit dimension.

### Fixes
- Baked Elisar props are no longer `EntityBlock`s. This removes about 2000 "Tried to load a block entity" warnings per city load.

### Verification
- Built (0.4.0) and run on the Forge server and client.
- Flight recorded from inside: lift, white flash, Earth and Moon, streaks, Elisar, arrival and the cleaning wave.
- City, bridges, undersides, tower and eye were checked on screenshots.
- Screenshots: `release/riftworld_screens/session1…session5`. Video: `release/riftworld_session5.mp4`.

## Rev 3 — stage C-1 (v0.5.0-c1): castle shell + main hall

The work follows the Rev 3 TZ (`reports/TZ_rev3.md`). The user picked variant C first. C-1 covers the castle shell, the main hall, the new blocks and a temporary site in Elisar.

### Castle «Чертог Тлеющего Рога» / "Hall of the Smouldering Horn"
- References: Malbork (red brick, stepped gables, red-tile roofs, grey stone trim), the Malbork Grand Refectory (rib vaults, tall lancets), and the Tantallon / Old Wardour / Scarborough ruins (jagged wall tops, roofless parts).
- The template is generated by `tools/rift/gen_castle.py`: 76×57×68 blocks, 33 932 blocks in total, saved as a vanilla NBT structure.
  - The front gatehouse with two round turrets faces north, towards the city.
  - The layout is asymmetric. There are four different corner towers:
    - NW: round;
    - NE: tall square keep with bartizans;
    - SW: ruined octagon;
    - SE: ruined square tower.
  - The west and east wings have steep stepped-gable roofs. The east chapel is double height, and its outer wall has collapsed.
  - A back range holds the rose window.
- Main hall (x24..52, z14..50, 2 floors):
  - wooden galleries/balconies at y10 on three sides, with stairs up;
  - gaps in the gallery rail where the guards will jump down;
  - clerestory lancets with stained glass and a rose window behind the throne;
  - a mosaic carpet and a raised dais with the «Трон Тлеющего Рога»;
  - 6 symmetric sockets for the healing columns (stage A/C-3);
  - 20 wrought-iron candle stands along the path to the throne and around the dais;
  - part of the roof has collapsed (x41..54, z36..44), with rubble on the floor.
- Colours: red brick (3 variants: plain, cracked, mossy), grey ashlar trim and pillars, rare brass accents, dark oak beams and red tile roofs.

### New blocks (all custom 16×16)
- `castle_bricks` (+ cracked, mossy, stairs, slab, wall)
- `ashlar` (+ stairs, slab, pillar)
- `dark_planks` (+ stairs, slab, beam, fence)
- `brass_block`, `castle_mosaic`, `stained_glass` (pane)
- `castle_roof` (+ stairs, slab)
- scorched ground: `ash_block`, `burnt_soil`, `scorched_stone`, `charred_log`
- 3D props (Blender `.blend` → OBJ):
  - `candle_stand`: tripod scroll feet, twisted stem, drip pan and candle. It is powered/unpowered and gives light 12 when lit.
  - `boss_throne`: pointed back with pinnacles, red velvet, beast-head armrests, 3×2×4.

### Temporary site in Elisar
- Island kind 5 is a scorched island at (40, 470), R 74, top y104: ash and burnt soil, scorched stone, boulders, and 34 leafless charred trees.
  - The branches are spread evenly, so the trees do not clump.
  - The approach to the gate is kept clear.
- `CastleSite` places the template in `applyBiomeDecoration`. The final home is the «Адские угодья» biome (stage B).
- Atmosphere preview: within about 170 blocks of the site, the Elisar cream haze blends into ash-brown fog.
  - The fog starts farther out, so the castle stays readable.
  - An ash pall veils the bright sky, and the sky eye fades out.

### Verification
- Built `riftworld-0.5.0-c1.jar` and ran it on the Forge server and client.
- Candles were checked dark and lit (via the `powered` state).
- Screenshots (13): `release/riftworld_screens/session6/`. Video (55 s): `release/riftworld_session6.mp4`.

### Next
- **C-2:** every room inside the castle:
  - armoury with the guards' weapons;
  - library;
  - chapel;
  - kitchen and dining room;
  - bedchambers;
  - treasury with resources;
  - chandeliers, tables, decor props.
- **C-3:** polish:
  - healing columns (break the top or use a pickaxe);
  - the entry candle sequence as a game mechanic;
  - tuning the collapse and ruins;
  - loot.

## Rev 3 / C-2a — v0.5.0-c2: castle rooms + launch without the cleaning strip

### Launch (both labs)
- The cleaning-wave strip is gone. After the capsule rises, the stage is now FLICKER (70 ticks): lab lamps blink in a broken
  pattern with a burnt-out torch sound, then all switch on; sirens stop; beacon sound. Core reaches DONE (checked: `Stage: 4`).

### 18 new Blender props (baked OBJ, `.blend` + `.glb`, 40–1250 faces each)
`castle_chandelier` (wrought-iron ring, 6 candles, chains; light 15), `wall_sconce` (12), `feast_table` (food, goblets,
candelabra; 8), `long_table` (books, papers; 6), `castle_bench`, `high_chair`, `guard_weapon_rack` (glaives, sickle, shield),
`guard_armor`, `canopy_bed`, `castle_chest` (container, 27 slots, loot table, chest sound), `chained_lectern`,
`chapel_altar` (10), `hearth_cauldron` (tripod over embers; 13), `alchemy_table` (coloured flasks, alembic; 7),
`wall_shackles`, `gold_hoard`, `tapestry_horn` (horned-skull arms), `bone_pile`.
Passable: chandelier, sconce, shackles, tapestry, bones. New 16×16 blocks: `castle_bookshelf`, `castle_bars`,
`castle_carpet`, `castle_cobweb`.

### Rooms (`tools/rift/castle_rooms.py`, exec'd by `gen_castle.py`; 335 props)
- **W wing, ground:** armoury (racks, armour stands, anvil, tool wall, 2 chandeliers), refectory (6 feast tables, benches,
  high chairs, kegs, sideboard), kitchen (cauldron, fireplace, stove, work tables, hanging herbs, sacks, kegs).
- **W wing, upper:** barracks (7 cots, table, chests), lord's bedchamber (canopy bed, fireplace with chairs, carpet,
  wardrobes, tapestries), alchemist's lab (3 benches, cauldron, book wall), storage loft.
- **E wing:** library (book walls, shelf rows, chained lecterns, reading table), chapel (pews, altar, candles; choir gallery
  along the hall wall fixes the upper doors that opened into the void), treasury (gold, chests, armour at the door);
  upper scriptorium and guest chamber.
- **North range:** storeroom, wine cellar, upper trophy gallery / dormitory with a new stair.
- **Gatehouse:** the back wall sealed the gate passage — now open; new stair to the guard room.
- **Towers:** doorways into the NW prison (bars, two cells, shackles, bones), SW watch tower, NE keep; breach into the SE ruin.
- 12 loot tables `chests/castle_*` (vanilla items) — all verified with `/loot spawn`.

### Verification
- Built `riftworld-0.5.0-c2.jar`, regenerated Элисар on the Forge server, client walk-through.
- Screenshots (20): `release/riftworld_screens/session7/`. Video (57 s): `release/riftworld_session7.mp4`.

### Next (C-2b)
- Spiral stairs inside the towers to their upper floors (prison levels, keep lookout), tower upper rooms.
- Then C-3: column mechanics, the entry candle sequence as a game mechanic.

## Rev 3 / C-2b — tower spiral stairs, every storey furnished (v0.5.0-c3)

### Spiral stairs (`castle_rooms.py`: `ring_path`, `spiral`, `tower_lights`)
- A closed 4-connected ring of cells along the inner wall of each tower; one `ashlar_stairs` tread per cell, rising 1 per
  cell (facing = towards the next cell); grey ashlar masonry fills under each flight down to the floor below (contrasts
  with the red brick); the last tread of each flight lies in the floor plane; headroom y+1..y+3 is cut through the floors.
- Automatic check in the generator: every tread has 2 free blocks above it (no "HEADROOM" warnings), no tower prop skipped.
- **NW round tower** (24 treads, floors y8/16/24): prison (ground; the first flight stays in the corridor outside the
  bars) → jailers' room (table, stools, weapon rack, chest, bones) → torture chamber (bar cage, shackles, brazier with
  cauldron, bones) → watch room under the roof (armour, chest, bench, candle stand, webs).
- **SW octagonal ruin** (21 treads, floors y8/16, landing y21): watch room (ground) → store (sacks, crates, gatehouse
  loot chest) → archers' room (rack, bench, chest) → open lookout platform among the broken walls.
- **NE keep** (44 treads along the perimeter, floors y8/16/24/32/40/44; the doorway stays free): store with chandelier →
  guards' hall (2 beds, long table, bench, chest) → armoury (2 racks, 2 armour stands, chest) → commander's room (bed,
  table, high chair, chained lectern, chest) → loft (crates, sacks, chest, webs) → upper hall (armour, candle stand,
  chest) → crenellated roof (candle stand, rack).
- Wall sconces (light 12) above head height on every storey: 31 added, placed clear of the stair headroom.
- Total 416 props in the castle.

### Verification
- Built `riftworld-0.5.0-c3.jar`, regenerated Элисар on the Forge server, walked every tower in spectator.
- Screenshots (15): `release/riftworld_screens/session8/`. Video (54 s, climbing all three towers): `release/riftworld_session8.mp4`.

### Next (C-3)
- Column mechanics (6 healing columns: hit the top or use a pickaxe), the candle entry sequence as a game mechanic,
  boss chair trigger zone.

## Rev 3 / C-3 — healing columns, entry candle sequence, boss-chair trigger (v0.5.0-c4)

### Arena controller (`arena/ArenaCoreBlockEntity`, hidden block `arena_core` under the throne)
- States DORMANT → ENTRY → WAITING → AWAKENING → FIGHT → CLEARED; persisted in block-entity NBT.
- **ENTRY** (a survival/adventure player enters the hall box): the hall candles go out, then after 1.5 s light pair by pair
  from the door to the throne (14 ticks per pair, flame burst + sound); not a cutscene — the player moves freely.
- **WAITING**: a low heartbeat sound every 2 s; trigger zone = within 6.5 blocks of the seat on the dais (|dy| < 5).
- **AWAKENING**: message, the 6 columns rise from their sockets one after another (10-tick stagger, 3 s rise animation with
  dust), ember beams connect each crystal to the throne.
- **FIGHT**: each standing column heals the boss 1 HP/s (hook `BossHooks` — the boss entity is Part A).
- **CLEARED**: all 6 broken → «Все колонны разрушены — его больше ничто не лечит».
- **Reset**: hall empty of non-spectator players for 60 s → candles out, columns sink back and are restored.
- Command `/riftarena status|start|reset` (op level 2).

### Healing column (`arena/BossColumnBlock` + `BossColumnBlockEntity` + `BossColumnRenderer`)
- Blender model `boss_column.glb` (2.6k faces): socket base, carved shaft with grotesque masks, talon crown cupping a
  large ember crystal; 3 crack stages and a broken stump as separate nodes.
- Hit the crystal (top part) 3 times — or an arrow into it — cracks grow, then it shatters (fire, smoke, sound);
  any hit with a pickaxe on any part breaks it immediately. Hitting the shaft with anything else shows a hint.
- The column cannot be mined; it is indestructible apart from this mechanic.

### Verification
- Live survival run on the Forge server (first person): entry candles, awakening, beams, wrong hit, 3 crystal hits,
  pickaxe, all six down, reset. Fixed during testing: first column never rose (tick 0 skipped), trigger height,
  beam width, bigger crystal, smoke instead of the explosion sprite on shatter.
- Screenshots (11): `release/riftworld_screens/session9/`. Video (69 s): `release/riftworld_session9.mp4`.

### Next (Part A)
- Boss model + animations (hunched biped, dog-like skull, bison horns, scythe), 200 HP, AI with telegraphed swings,
  scythe stuck in the floor on some swings, wall climbing only against a bow user on the 2nd floor; spawn via
  `BossHooks.awaken` (lies as if dead, jerks, stands and waits by the chair).
- 4 guards jumping from the balconies (spider-like wall running, pause before a dash).
- Death: lies 15–20 s, vanishes; the scythe drops (glowing large item, super attacks).

## Rev 3 / Part A — boss, guards, weapons, animations, Ashen Lands, glowing glass (v0.6.0-a1)

### Models (Blender, `tools/rift/creatures.py` → `boss.glb`, `guard.glb`, `scythe.glb`, `guard_sickle.glb` + `.blend`)
- Rigid detailed parts parented to armatures; every clip is an armature action exported as a glTF animation, sampled
  in the mod by `glb/Pose.sample/sampleBlend` with cross-fades (`RiftMob` syncs clip id + start tick).
- **Morgrath, Master of the Hall** (~3 m, strongly hunched biped, long canine skull, bison horns, bone plates, rags,
  war scythe): clips idle, walk, awaken (jerk + rise), swing_over, pull, swing_side, swing_diag, pull_diag, roar,
  climb, death, dead.
- **Smouldering Warden** (gaunt hunched guard, bone mask, spiked back, hooked sickle): idle, run, crawl, leap,
  windup, dash, slash, death.

### Boss (`entity/MorgrathBoss`), 200 HP, boss bar
- Entry: during the candle sequence he lies as if dead in front of the chair, jerks and rises, then waits by the chair.
- Throne trigger: roar → four guards appear on the balconies and leap down into the hall.
- Swings are telegraphed (wind-up, flare + sound 0.45 s before the blow, aim locks before the hit):
  overhead (line, buries the scythe → he tugs it out, vulnerable, +50% damage), diagonal (also sticks),
  horizontal sweep (never sticks). Below 50% HP he roars again and calls new guards if fewer than 2 remain.
- Healed by the standing columns (1 HP/s each) — beams follow him.
- Climbs walls only when shot with an arrow by a player on the 2nd-floor gallery (path clear of the column sockets);
  otherwise a player on the gallery is not followed up the stairs — he waits below. Never leaves the hall.
- Death: falls, lies 17 s (no red tint), crumbles; the scythe drops at once.

### Guards (`entity/EmberGuard`)
- Zombie-strength (20 HP, 3 dmg), weave from side to side while closing in, run up walls like spiders, vault over
  overhangs (gallery floor) onto the target's floor, crouch-tremble ~1.5–2.3 s before a dash, slash up close.

### Weapons
- **Morgrath's Scythe** (13 dmg, slow; normal look in hand): right click — «Жатва» 360° spin; sneak — «Огненная
  борозда» fire furrow; sprint — «Рывок жнеца» dash; in the air — «Падение кометы» shock-wave slam. Dropped, it lies
  as a large glowing blade with embers and a real light block under it (picks up normally).
- **Warden's hook-sickle** (guard weapon item).

### Ashen Lands (biome `riftworld:ashen_lands`, R 235 around the castle) + finding it
- Ash-brown/red fog, white-ash particles, basalt ambience; burnt islands with leafless trees; the ocean plane turns
  into an ash plain with winding lava meanders (`magma_meander`, harms), boulders, three huge fallen islands
  (a ruined house, a broken bridge). The castle stands in the middle.
- The first arrival on Elisar gives a compass bound to the castle (40, 470) — «Компас к Чертогу Тлеющего Рога».

### Glass
- Castle stained glass: light level 10, emissive (full-bright) faces, translucent render type → in shader packs the
  sun passes through and is tinted, and the glass itself glows.

### Verification (live server + client, survival, first/third person)
- Entry/rise/wait, trigger + roar + guard leap (positions logged from balcony y115 to floor y105), swings and knockback,
  bow from the gallery → wall climb → landing on the gallery, guard vault onto the gallery, death 17 s → corpse gone,
  scythe drop + light + pickup, all four super attacks on husks, biome views, compass on dimension change.
- Fixed while testing: client crash on scythe pickup (renderer cast), boss following out of the hall / up the stairs,
  red corpse tint, scythe not visible enough (light), guards stuck under the gallery, climb blocked by sockets.
- Screenshots: `release/riftworld_screens/session10/`.

## Part A-2 (v0.6.0-a2) — fight depth, guard leap, sharp head turns

### Boss — new attacks (new Blender clips `lunge`, `scrape` in `boss.glb`)
- **«Выпад жнеца»** (lunge, 1.7 s): at 5–10 blocks with a clear line, a deep spring crouch with the scythe drawn back
  (direction tracks for 0.35 s, then locks; flame flash + smoke under the feet), then a ~5-block low leap and a forward
  cut (14 dmg along a 1.1-wide line). The leap cannot climb column sockets (step height lowered during the leap) and
  stops short of the target/walls. Cooldown 5.5–8.5 s. Never sticks.
- **«Огненный разрез»** (scrape, 2.0 s): if the player keeps away (> 7 blocks for 2.5 s), the blade grinds through the
  floor on his right (sparks, embers, grinding sound — 0.75 s warning), then rips across: close arc 9 dmg, and a narrow
  fire furrow runs straight along the floor at the locked direction (0.8 block/tick, 18 blocks, stops at walls),
  7 dmg + 4 s fire, hits each victim once — dodge sideways. Cooldown 8.5–12.5 s.
- **Combo**: a horizontal sweep can flow straight into an overhead chop (40 %, 60 % in phase 2), wind-up shortened by
  0.4 s but still telegraphed (flame flash + locked direction 0.45 s before the blow); the chop sticks as usual.
- **Climb trigger is no longer lost**: an arrow from the gallery during the roar/swing/tug is remembered and he goes to
  the wall as soon as he is free (tested: arrow at roar st≈30 → CLIMB_GO right after the roar → on the gallery).
- **Unstick**: if he tries to reach the target but has not moved for 2.5 s (wedged at a column socket / rubble),
  he hops to the nearest free floor spot towards the target inside the hall.
- `/riftarena status` also prints the boss state/clip/HP/position (testing aid).

### Sharp head turns (TZ «резкие повороты головы»)
- Client renderer: the head no longer glides after the gaze. It holds a direction and, when the gaze drifts past a
  threshold (13° boss / 9° guards), snaps to the new one in ~2 ticks with a small overshoot. On top, a seeded
  involuntary twitch every few seconds (14–30° yaw + roll, 0.3 s), identical for all viewers. Server-side head jerks
  (35–60°) during waiting/fight remain.

### Guards — balcony leap
- New `leap` clip: a spring crouch (claw on the railing) → stretched launch → tucked fall; new `land` clip: absorbs the
  drop in a deep crouch with the claw on the floor, head snapping up. Physics: up over the railing first (0.6), then out
  into the hall (0.60–0.68 + small side spread), body turned along the flight. Logged: from x30.5 / x50.5 they land at
  x≈36.9–37.1 / x≈43.7 (6–7 blocks out, was ~2), dust + embers on landing.

### Verification (live server + client, survival + resistance)
- Status polling: lunge (moves 4–5 blocks), scrape (player burned at 11 blocks ~0.7 s after the rip), combo,
  pending climb during roar → gallery, unstick from a socket (≈3 s), guard leap positions; burst screenshots of the
  leap arc and both new attacks; 0 exceptions in the server log.

## Part A-3 (v0.6.0-a3) — coloured light through the stained glass (vanilla, shader-safe)

- `client/WindowLights.java` (client only, no template change, works on already generated castles and on any window
  a player builds from the castle stained glass). Every vertical run of panes is found when its chunk arrives.
- The light direction follows the real sun path (tilted 28° to the south like shader packs): mornings light the east
  windows, noon fills the great rose window over the throne, evenings the west side; at night a faint cold moonlight.
  Rain dims it. The room side is detected by sky light (open ruins get no shaft).
- Per window column: a soft additive shaft in the glass colours (amber, ruby, cobalt, gold, wine, green — sampled from
  the texture) from the pane to whatever it hits (ray-marched against real collision shapes, the window reveal and sill
  are skipped), fading with distance; where it lands, the leaded glass pattern itself is projected as a coloured
  additive patch (black lead lines stay dark); coloured dust motes drift in the beams.
- Rendered after translucent blocks, depth-tested, no depth write; with shader packs it draws as an emissive overlay on
  top of their volumetric light. The glass itself keeps light 10 + emissive faces.
- Geometry refresh spread over frames (≤ 48 columns/frame, every 5 in-game seconds), range 72 blocks.
- Verified live at 2500 / 3000 / 5200 / 6000 / 9800 / 16000 ticks; screenshots `release/riftworld_screens/session12/`.

## Part A-4 — final polish (v0.6.0, Part A complete)

- **Boss stagger** (new Blender clip `stagger`, 1.0 s): 24+ damage within 1.5 s while he is idle/walking or pulling a stuck
  scythe (scythe super attacks, crits into the stuck scythe) knocks him off balance — reels back with arms and scythe flung
  out, jaw open, head snapping aside, then a low crouch and recovery. 20 ticks without attacks = a punish window;
  cooldown 12 s so it can't be chain-locked. Sounds: warden hurt + metal clang + heavy step.
- **Hit reactions on top of any clip** (`RiftMobRenderer`): on every hit the torso jerks back and the head snaps sideways,
  decaying over ~0.5 s (guards 22°, boss 11°) — reactions now visible even mid-swing.
- **Heavy footfalls**: client-side dust puffs + an ember spark and a low step sound exactly at the walk clip's foot contacts.
- **Window light patch follows the geometry**: the coloured leaded patch is now projected on a half-pane grid (each grid
  point marched separately), so it lies on the dais steps, kerbs and wall bases instead of stretching over vertical faces;
  cells torn across very different surfaces are skipped. Refresh budget lowered to 12 columns/frame.
- Verified live (survival + resistance): stagger triggers from FIGHT (not during attack swings; PULL path same code, not separately filmed); recovers into
  normal combat; patches on stairs at noon and on the floor in the morning; no exceptions. Screenshots: `session13/`.
- Lightweight biome + structure for normal play = the Ashen Lands (R≈235, burnt islands, fallen islands with ruined house and
  broken bridge, lava meanders) + the castle, located with the castle compass — already shipped in A-1..A-3.
