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

### Next (Part 3)
Beam + flight cinematic, dimension «Элисар» (city «Странники», islands, villagers/кенли, sky eye, biomes).
