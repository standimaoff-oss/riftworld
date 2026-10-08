"""Generates blockstates / block+item models / loot tables / tags / lang for riftworld."""

raise SystemExit(
    "obsolete: this generator belongs to content that was deleted from the mod\n"
    "(the laboratory and the Rev 4 temple mosaic). It would rebuild assets that no longer\n"
    "exist - see README, Rev 5. The files are kept only for reference.")
import json, os
R = "/home/user/riftworld/mod/src/main/resources"
A = R + "/assets/riftworld"; D = R + "/data/riftworld"
def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
NS = "riftworld:block/"
CUBES = ["lab_panel", "lab_panel_dirty", "lab_panel_stripe", "lab_panel_stripe_dirty", "lab_tile", "lab_tile_cracked",
         "steel_floor", "steel_floor_rusty", "lab_concrete", "lab_concrete_cracked", "lab_facade", "lab_ceiling",
         "lab_ceiling_dirty", "hazard_block", "hazard_block_worn", "shaft_wall", "wall_vent"]
CUTOUT = ["metal_grate", "rusty_grate"]
GLASS = ["lab_glass", "lab_glass_cracked"]
LAMPS = ["lab_lamp", "emergency_lamp"]
STAIRS = {"lab_concrete_stairs": "lab_concrete", "steel_floor_stairs": "steel_floor", "lab_tile_stairs": "lab_tile"}
STAIR_SIDE = {"lab_tile_stairs": "lab_stair_side"}
SLABS = {"lab_concrete_slab": "lab_concrete", "lab_tile_slab": "lab_tile", "steel_floor_slab": "steel_floor"}
CARPETS = ["debris", "dead_moss", "dirt_pile"]
MODELS = ["portal_helm", "portal_lever", "control_console", "lathe", "milling_machine", "specimen_tank", "electrical_cabinet",
          "generator", "transformer", "server_rack", "security_desk", "crate_wood", "crate_metal", "lab_door", "lab_table",
          "shelf", "office_chair", "locker", "trash_bin_tipped", "chandelier", "siren", "shelf_fallen", "wall_notes",
          "desk_computer", "table_flipped", "barrels", "fire_extinguisher", "box_pile", "paper_pile", "chair_fallen"]
UNBREAKABLE = {"portal_helm", "portal_lever"}
WOOD = {"crate_wood", "lab_table", "shelf"}

def simple_item(name, parent=None):
    w(f"{A}/models/item/{name}.json", {"parent": parent or NS + name})

for n in CUBES:
    w(f"{A}/blockstates/{n}.json", {"variants": {"": {"model": NS + n}}})
    w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": NS + n}})
    simple_item(n)
for n in CUTOUT + GLASS:
    w(f"{A}/blockstates/{n}.json", {"variants": {"": {"model": NS + n}}})
    w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": NS + n},
                                     "render_type": "minecraft:cutout" if n in CUTOUT else "minecraft:translucent"})
    simple_item(n)
for n in LAMPS:
    w(f"{A}/blockstates/{n}.json", {"variants": {"lit=false": {"model": NS + n}, "lit=true": {"model": NS + n + "_on"}}})
    w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": NS + n}})
    w(f"{A}/models/block/{n}_on.json", {"parent": "minecraft:block/cube_all", "textures": {"all": NS + n + "_on"}})
    simple_item(n)
for n, base in STAIRS.items():
    t = {"bottom": NS + base, "top": NS + base, "side": NS + STAIR_SIDE.get(n, base)}
    for suf, par in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        w(f"{A}/models/block/{n}{suf}.json", {"parent": "minecraft:block/" + par, "textures": t})
    v = {}
    for facing, yb in (("east", 0), ("south", 90), ("west", 180), ("north", 270)):
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                m = NS + n + ("_inner" if shape.startswith("inner") else "_outer" if shape.startswith("outer") else "")
                y = yb
                if shape in ("inner_left", "outer_left") and half == "bottom": y -= 90
                if shape in ("inner_right", "outer_right") and half == "top": y += 90
                e = {"model": m}
                if half == "top": e["x"] = 180
                y %= 360
                if y: e["y"] = y
                if y or half == "top": e["uvlock"] = True
                v[f"facing={facing},half={half},shape={shape}"] = e
    w(f"{A}/blockstates/{n}.json", {"variants": v})
    simple_item(n)
for n, base in SLABS.items():
    t = {"bottom": NS + base, "top": NS + base, "side": NS + base}
    w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/slab", "textures": t})
    w(f"{A}/models/block/{n}_top.json", {"parent": "minecraft:block/slab_top", "textures": t})
    w(f"{A}/blockstates/{n}.json", {"variants": {"type=bottom": {"model": NS + n}, "type=top": {"model": NS + n + "_top"},
                                                 "type=double": {"model": NS + base}}})
    simple_item(n)
for n in CARPETS:
    w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/carpet", "textures": {"wool": NS + n}, "render_type": "minecraft:cutout"})
    w(f"{A}/blockstates/{n}.json", {"variants": {"": [{"model": NS + n, "y": r} for r in (0, 90, 180, 270)]}})
    simple_item(n)
# pipe
w(f"{A}/models/block/pipe.json", {"parent": "minecraft:block/block", "textures": {"side": NS + "pipe_side", "end": NS + "pipe_end", "particle": NS + "pipe_side"},
    "elements": [{"from": [4, 0, 4], "to": [12, 16, 12], "faces": {
        "north": {"uv": [4, 0, 12, 16], "texture": "#side"}, "south": {"uv": [4, 0, 12, 16], "texture": "#side"},
        "east": {"uv": [4, 0, 12, 16], "texture": "#side"}, "west": {"uv": [4, 0, 12, 16], "texture": "#side"},
        "up": {"uv": [4, 4, 12, 12], "texture": "#end", "cullface": "up"}, "down": {"uv": [4, 4, 12, 12], "texture": "#end", "cullface": "down"}}},
        {"from": [3, 0, 3], "to": [13, 2, 13], "faces": {f: {"uv": [3, 0, 13, 2], "texture": "#end"} for f in ("north", "south", "east", "west")}
         | {"up": {"uv": [3, 3, 13, 13], "texture": "#end"}, "down": {"uv": [3, 3, 13, 13], "texture": "#end", "cullface": "down"}}}]})
w(f"{A}/blockstates/pipe.json", {"variants": {"axis=y": {"model": NS + "pipe"}, "axis=z": {"model": NS + "pipe", "x": 90},
                                             "axis=x": {"model": NS + "pipe", "x": 90, "y": 90}}})
simple_item("pipe")
# dead vines: vanilla vine template
w(f"{A}/models/block/dead_vines.json", {"parent": "minecraft:block/vine", "textures": {"vine": NS + "dead_vines", "particle": NS + "dead_vines"}, "render_type": "minecraft:cutout"})
mp = [{"when": {"up": "true"}, "apply": {"model": NS + "dead_vines", "x": 270, "uvlock": True}}]
for face, y in (("south", 0), ("west", 90), ("north", 180), ("east", 270)):
    e = {"model": NS + "dead_vines", "uvlock": True}
    if y: e["y"] = y
    mp.append({"when": {face: "true"}, "apply": e})
mp.append({"when": {"OR": [{d: "false" for d in ("east", "north", "south", "up", "west")}]}, "apply": [
    {"model": NS + "dead_vines", "y": y, "uvlock": True} if y else {"model": NS + "dead_vines"} for y in (0, 90, 180, 270)]})
mp = mp[:-1]  # (the all-false state is impossible for placed vines)
w(f"{A}/blockstates/dead_vines.json", {"multipart": mp})
w(f"{A}/models/item/dead_vines.json", {"parent": "minecraft:item/generated", "textures": {"layer0": NS + "dead_vines"}})
# railing: vanilla iron-bars templates
bars = {"bars": NS + "lab_railing", "edge": NS + "lab_railing", "particle": NS + "lab_railing"}
for sub in ("post_ends", "post", "cap", "cap_alt", "side", "side_alt"):
    w(f"{A}/models/block/lab_railing_{sub}.json", {"parent": "minecraft:block/iron_bars_" + sub, "textures": bars, "render_type": "minecraft:cutout"})
B = NS + "lab_railing_"
F = lambda **k: {kk: str(v).lower() for kk, v in k.items()}
w(f"{A}/blockstates/lab_railing.json", {"multipart": [
    {"apply": {"model": B + "post_ends"}},
    {"when": F(north=False, east=False, south=False, west=False), "apply": {"model": B + "post"}},
    {"when": F(north=True, east=False, south=False, west=False), "apply": {"model": B + "cap"}},
    {"when": F(north=False, east=True, south=False, west=False), "apply": {"model": B + "cap", "y": 90}},
    {"when": F(north=False, east=False, south=True, west=False), "apply": {"model": B + "cap_alt"}},
    {"when": F(north=False, east=False, south=False, west=True), "apply": {"model": B + "cap_alt", "y": 90}},
    {"when": F(north=True), "apply": {"model": B + "side"}},
    {"when": F(east=True), "apply": {"model": B + "side", "y": 90}},
    {"when": F(south=True), "apply": {"model": B + "side_alt"}},
    {"when": F(west=True), "apply": {"model": B + "side_alt", "y": 90}}]})
w(f"{A}/models/item/lab_railing.json", {"parent": "minecraft:item/generated", "textures": {"layer0": NS + "lab_railing"}})
for n in ("launch_core", "capsule_terminal"):
    w(f"{A}/models/block/{n}.json", {"textures": {"particle": NS + "steel_floor"}, "elements": []})
w(f"{A}/blockstates/launch_core.json", {"variants": {f"facing={d}": {"model": NS + "launch_core"} for d in ("north", "south", "east", "west")}})
w(f"{A}/blockstates/capsule_terminal.json", {"variants": {"": {"model": NS + "capsule_terminal"}}})
# part (invisible)
w(f"{A}/models/block/part.json", {"textures": {"particle": "minecraft:block/iron_block"}, "elements": []})
w(f"{A}/blockstates/part.json", {"variants": {"open=false": {"model": NS + "part"}, "open=true": {"model": NS + "part"}}})
# model blocks: particle-only block model; builtin/entity item model
PART = {"crate_wood": "minecraft:block/spruce_planks", "lab_table": "minecraft:block/birch_planks", "shelf": "minecraft:block/iron_block",
        "office_chair": "minecraft:block/gray_wool"}
for n in MODELS:
    w(f"{A}/models/block/{n}.json", {"textures": {"particle": PART.get(n, NS + "lab_panel")}, "elements": []})
    w(f"{A}/blockstates/{n}.json", {"variants": {"": {"model": NS + n}}})
    w(f"{A}/models/item/{n}.json", {"parent": "minecraft:builtin/entity", "gui_light": "front", "display": {
        "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
        "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]}}})
w(f"{A}/models/item/resonance_stone.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "riftworld:item/resonance_stone"}})

# loot tables
def drop_self(n):
    return {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "riftworld:" + n}],
                                                  "conditions": [{"condition": "minecraft:survives_explosion"}]}]}
ALL = CUBES + CUTOUT + LAMPS + list(STAIRS) + CARPETS + ["pipe", "lab_railing"] + [m for m in MODELS if m not in UNBREAKABLE]
for n in ALL:
    w(f"{D}/loot_tables/blocks/{n}.json", drop_self(n))
for n in SLABS:
    w(f"{D}/loot_tables/blocks/{n}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "riftworld:" + n,
        "functions": [{"function": "minecraft:set_count", "count": 2, "add": False, "conditions": [{"condition": "minecraft:block_state_property",
        "block": "riftworld:" + n, "properties": {"type": "double"}}]}, {"function": "minecraft:explosion_decay"}]}]}]})
silk = lambda n: {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "riftworld:" + n}],
                                                       "conditions": [{"condition": "minecraft:match_tool", "predicate": {"enchantments": [{"enchantment": "minecraft:silk_touch", "levels": {"min": 1}}]}}]}]}
for n in GLASS + ["dead_vines"]:
    w(f"{D}/loot_tables/blocks/{n}.json", silk(n))
# warehouse loot
w(f"{D}/loot_tables/chests/lab_storage.json", {"type": "minecraft:chest", "pools": [
    {"rolls": {"min": 3, "max": 7}, "entries": [
        {"type": "minecraft:item", "name": "minecraft:iron_ingot", "weight": 10, "functions": [{"function": "minecraft:set_count", "count": {"min": 1, "max": 5}}]},
        {"type": "minecraft:item", "name": "minecraft:copper_ingot", "weight": 12, "functions": [{"function": "minecraft:set_count", "count": {"min": 2, "max": 8}}]},
        {"type": "minecraft:item", "name": "minecraft:redstone", "weight": 10, "functions": [{"function": "minecraft:set_count", "count": {"min": 2, "max": 9}}]},
        {"type": "minecraft:item", "name": "minecraft:glass_bottle", "weight": 8, "functions": [{"function": "minecraft:set_count", "count": {"min": 1, "max": 4}}]},
        {"type": "minecraft:item", "name": "minecraft:paper", "weight": 8, "functions": [{"function": "minecraft:set_count", "count": {"min": 1, "max": 6}}]},
        {"type": "minecraft:item", "name": "minecraft:bread", "weight": 4, "functions": [{"function": "minecraft:set_count", "count": {"min": 1, "max": 3}}]},
        {"type": "minecraft:item", "name": "minecraft:quartz", "weight": 6, "functions": [{"function": "minecraft:set_count", "count": {"min": 1, "max": 6}}]},
        {"type": "minecraft:item", "name": "minecraft:amethyst_shard", "weight": 3, "functions": [{"function": "minecraft:set_count", "count": {"min": 1, "max": 3}}]},
        {"type": "minecraft:item", "name": "minecraft:compass", "weight": 1},
        {"type": "minecraft:item", "name": "minecraft:diamond", "weight": 1}]}]})
# tags
pick = [n for n in ALL + list(SLABS) if n not in CARPETS and n not in WOOD]
w(R + "/data/minecraft/tags/blocks/mineable/pickaxe.json", {"replace": False, "values": ["riftworld:" + n for n in pick + ["part"]]})
w(R + "/data/minecraft/tags/blocks/mineable/axe.json", {"replace": False, "values": ["riftworld:" + n for n in WOOD]})
w(R + "/data/minecraft/tags/blocks/climbable.json", {"replace": False, "values": ["riftworld:dead_vines"]})
w(R + "/data/minecraft/tags/blocks/stairs.json", {"replace": False, "values": ["riftworld:" + n for n in STAIRS]})
w(R + "/data/minecraft/tags/blocks/slabs.json", {"replace": False, "values": ["riftworld:" + n for n in SLABS]})
w(R + "/data/minecraft/tags/items/stairs.json", {"replace": False, "values": ["riftworld:" + n for n in STAIRS]})
w(R + "/data/minecraft/tags/items/slabs.json", {"replace": False, "values": ["riftworld:" + n for n in SLABS]})

NAMES = {  # id: (en, ru)
    "lab_panel": ("Lab Wall Panel", "Лабораторная панель"), "lab_panel_dirty": ("Dirty Lab Wall Panel", "Грязная лабораторная панель"),
    "lab_panel_stripe": ("Striped Lab Panel", "Панель с полосой"), "lab_panel_stripe_dirty": ("Dirty Striped Lab Panel", "Грязная панель с полосой"),
    "lab_tile": ("Lab Floor Tile", "Лабораторная плитка"), "lab_tile_cracked": ("Cracked Lab Tile", "Треснувшая плитка"),
    "steel_floor": ("Steel Floor Plate", "Стальной настил"), "steel_floor_rusty": ("Rusty Steel Floor", "Ржавый настил"),
    "lab_concrete": ("Lab Concrete", "Бетон лаборатории"), "lab_concrete_cracked": ("Cracked Lab Concrete", "Треснувший бетон"),
    "lab_facade": ("Facade Panel", "Фасадная панель"), "lab_ceiling": ("Ceiling Panel", "Потолочная панель"),
    "lab_ceiling_dirty": ("Dirty Ceiling Panel", "Грязная потолочная панель"), "hazard_block": ("Hazard Stripes", "Предупредительные полосы"),
    "hazard_block_worn": ("Worn Hazard Stripes", "Стёртые предупредительные полосы"), "shaft_wall": ("Shaft Wall", "Стена шахты"),
    "lab_concrete_stairs": ("Lab Concrete Stairs", "Бетонные ступени"), "lab_concrete_slab": ("Lab Concrete Slab", "Бетонная плита"),
    "lab_tile_slab": ("Lab Tile Slab", "Плита из плитки"), "steel_floor_stairs": ("Steel Stairs", "Стальные ступени"),
    "steel_floor_slab": ("Steel Floor Slab", "Стальная плита"), "metal_grate": ("Metal Grate", "Металлическая решётка"),
    "rusty_grate": ("Rusty Grate", "Ржавая решётка"), "lab_glass": ("Lab Glass", "Лабораторное стекло"),
    "lab_glass_cracked": ("Cracked Lab Glass", "Треснувшее стекло"), "lab_lamp": ("Ceiling Lamp", "Потолочная лампа"),
    "emergency_lamp": ("Emergency Lamp", "Аварийная лампа"), "pipe": ("Industrial Pipe", "Промышленная труба"),
    "debris": ("Debris", "Обломки"), "dead_moss": ("Dead Moss", "Мёртвый мох"), "dead_vines": ("Dead Vines", "Мёртвые лианы"),
    "part": ("Machine Part", "Часть машины"),
    "portal_helm": ("Launch Helm", "Руль управления запуском"), "portal_lever": ("Launch Lever", "Рычаг запуска"),
    "control_console": ("Control Console", "Пульт управления"), "lathe": ("Lathe", "Токарный станок"),
    "milling_machine": ("Milling Machine", "Фрезерный станок"), "specimen_tank": ("Specimen Tank", "Колба с образцом"),
    "electrical_cabinet": ("Electrical Cabinet", "Электрощит"), "generator": ("Generator", "Генератор"),
    "transformer": ("Transformer", "Трансформатор"), "server_rack": ("Server Rack", "Серверная стойка"),
    "security_desk": ("Security Desk", "Пост охраны"), "crate_wood": ("Wooden Crate", "Деревянный ящик"),
    "crate_metal": ("Metal Crate", "Металлический ящик"), "lab_door": ("Sliding Lab Door", "Раздвижная дверь"),
    "lab_table": ("Lab Table", "Лабораторный стол"), "shelf": ("Storage Shelf", "Стеллаж"),
    "office_chair": ("Office Chair", "Офисное кресло"), "locker": ("Locker", "Шкафчик"),
    "wall_vent": ("Wall Vent", "Вентиляционная решётка"), "dirt_pile": ("Dirt Pile", "Куча грязи"),
    "lab_railing": ("Lab Railing", "Перила"), "lab_tile_stairs": ("Lab Tile Stairs", "Ступени из плитки"),
    "launch_core": ("Launch Shaft Core", "Ядро шахты запуска"), "capsule_terminal": ("Capsule Terminal", "Терминал капсулы"),
    "trash_bin_tipped": ("Tipped Trash Bin", "Опрокинутая урна"), "chandelier": ("Chandelier", "Люстра"),
    "siren": ("Old Siren", "Старая сирена"), "shelf_fallen": ("Fallen Shelf", "Упавший стеллаж"),
    "wall_notes": ("Notice Board", "Доска с записками"), "desk_computer": ("Office Desk", "Рабочий стол с компьютером"),
    "table_flipped": ("Flipped Table", "Перевёрнутый стол"), "barrels": ("Barrels", "Бочки"),
    "fire_extinguisher": ("Fire Extinguisher", "Огнетушитель"), "box_pile": ("Box Pile", "Груда коробок"),
    "paper_pile": ("Scattered Papers", "Разбросанные бумаги"), "chair_fallen": ("Fallen Chair", "Упавшее кресло"),
}
en = {"itemGroup.riftworld": "Rift", "item.riftworld.resonance_stone": "Resonance Stone",
      "item.riftworld.resonance_stone.desc": "Hums softly. Fits a socket of the launch helm.",
      "message.riftworld.stone_set": "Resonance stone installed (%s/5)",
      "message.riftworld.stones_all": "All five stones are in place (5/5). The lab is waking up...",
      "message.riftworld.stones_status": "Resonance stones: %s/5",
      "message.riftworld.not_enough": "Not enough stones (%s/5). Something fell from above...",
      "message.riftworld.ready": "The helm hums. Launch systems answer...",
      "message.riftworld.booting": "Power is coming back... wait for the lights.",
      "message.riftworld.launch": "LAUNCH! The shaft is opening...",
      "message.riftworld.launch_busy": "The launch sequence is already running.",
      "message.riftworld.terminal_route": "[Capsule] Route: ELISAR. Coordinates locked. Beam emitter: charging...",
      "message.riftworld.terminal_wait": "The beam emitter is not ready yet",
      "message.riftworld.terminal_confirm": "[Capsule] Beam emitter ready. Press the console again within 5 s to launch",
      "message.riftworld.terminal_launch": "[Capsule] LAUNCH. Hold on.",
      "message.riftworld.terminal_center": "Stand inside the capsule to launch",
      "message.riftworld.terminal_arrived": "[Capsule] Arrived: ELISAR. Return route not calculated.",
      "message.riftworld.flight_locked": "The capsule is in flight",
      "book.riftworld.note.p1": "Not enough stones: %s of 5.\n\nFind the missing resonance stones and insert them into the helm sockets.",
      "book.riftworld.note.p2": "We hid them when the evacuation started:\n- basement warehouse\n- technical department\n- electrical department\n- security room, 2nd floor\n\n— Dr. Veresov"}
ru = {"itemGroup.riftworld": "Разлом", "item.riftworld.resonance_stone": "Камень резонанса",
      "item.riftworld.resonance_stone.desc": "Тихо гудит. Подходит к гнезду руля запуска.",
      "message.riftworld.stone_set": "Камень установлен (%s/5)",
      "message.riftworld.stones_all": "Все пять камней на месте (5/5). Лаборатория просыпается...",
      "message.riftworld.stones_status": "Камни резонанса: %s/5",
      "message.riftworld.not_enough": "Не хватает камней (%s/5). Сверху что-то упало...",
      "message.riftworld.ready": "Руль гудит. Системы запуска отвечают...",
      "message.riftworld.booting": "Питание возвращается... дождитесь света.",
      "message.riftworld.launch": "ЗАПУСК! Шахта открывается...",
      "message.riftworld.launch_busy": "Последовательность запуска уже идёт.",
      "message.riftworld.terminal_route": "[Капсула] Маршрут: ЭЛИСАР. Координаты зафиксированы. Излучатель луча: зарядка...",
      "message.riftworld.terminal_wait": "Излучатель луча ещё не готов",
      "message.riftworld.terminal_confirm": "[Капсула] Излучатель готов. Нажмите на пульт ещё раз в течение 5 с для старта",
      "message.riftworld.terminal_launch": "[Капсула] СТАРТ. Держитесь.",
      "message.riftworld.terminal_center": "Встаньте внутрь капсулы, чтобы стартовать",
      "message.riftworld.terminal_arrived": "[Капсула] Прибытие: ЭЛИСАР. Обратный маршрут не рассчитан.",
      "message.riftworld.flight_locked": "Капсула в полёте",
      "book.riftworld.note.p1": "Не хватает камней: %s из 5.\n\nНайдите недостающие камни резонанса и вставьте их в гнёзда руля.",
      "book.riftworld.note.p2": "Мы спрятали их, когда началась эвакуация:\n- склад в подвале\n- технический отдел\n- электротехнический отдел\n- комната охраны, 2 этаж\n\n— Д-р Вересов"}
for k, (e, r) in NAMES.items():
    en["block.riftworld." + k] = e; ru["block.riftworld." + k] = r
w(A + "/lang/en_us.json", en); w(A + "/lang/ru_ru.json", ru)
print("resources OK", len(NAMES), "blocks")
