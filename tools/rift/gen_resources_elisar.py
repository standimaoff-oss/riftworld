"""Elisar blocks: blockstates / models / items / loot / tags / lang. Run AFTER gen_resources.py (merges lang + tags)."""
import os
import json, os
R = "/home/user/mods/riftworld/src/main/resources"
A = R + "/assets/riftworld"; D = R + "/data/riftworld"
NS = "riftworld:block/"


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)


def merge(path, obj):
    old = json.load(open(path, encoding="utf-8")) if os.path.exists(path) else {}
    if "values" in obj:
        old.setdefault("replace", False)
        old["values"] = list(dict.fromkeys(old.get("values", []) + obj["values"]))
    else:
        old.update(obj)
    w(path, old)


def item(n, parent=None):
    w(f"{A}/models/item/{n}.json", {"parent": parent or NS + n})


def gen_item(n, tex):
    w(f"{A}/models/item/{n}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": tex}})


DROPS = []
CUBES = ["elisar_soil", "elisar_stone", "elisar_stone_mossy", "elisar_seabed", "elisar_cobble", "elisar_path", "elisar_bricks",
         "elisar_bricks_mossy", "elisar_plaster", "timber_frame_cross", "timber_frame_post", "sizo_planks", "ambra_planks",
         "thatch", "roof_red", "roof_blue"]
for n in CUBES:
    rots = (0, 90, 180, 270) if n in ("elisar_soil", "elisar_seabed", "elisar_path", "elisar_stone") else (0,)
    w(f"{A}/blockstates/{n}.json", {"variants": {"": [{"model": NS + n, "y": r} for r in rots]}})
    w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": NS + n}})
    item(n); DROPS.append(n)
# grass
w(f"{A}/models/block/elisar_grass.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
    "top": NS + "elisar_grass_top", "side": NS + "elisar_grass_side", "bottom": NS + "elisar_soil", "particle": NS + "elisar_soil"}})
w(f"{A}/blockstates/elisar_grass.json", {"variants": {"": [{"model": NS + "elisar_grass", "y": r} for r in (0, 90, 180, 270)]}})
item("elisar_grass"); DROPS.append("elisar_grass")
# logs
for n in ("sizo_log", "ambra_log"):
    w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/cube_column", "textures": {"end": NS + n + "_top", "side": NS + n}})
    w(f"{A}/models/block/{n}_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal", "textures": {"end": NS + n + "_top", "side": NS + n}})
    w(f"{A}/blockstates/{n}.json", {"variants": {"axis=y": {"model": NS + n}, "axis=z": {"model": NS + n + "_horizontal", "x": 90},
                                                "axis=x": {"model": NS + n + "_horizontal", "x": 90, "y": 90}}})
    item(n); DROPS.append(n)
for n in ("sizo_leaves", "ambra_leaves"):
    w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/leaves", "textures": {"all": NS + n}, "render_type": "minecraft:cutout_mipped"})
    w(f"{A}/blockstates/{n}.json", {"variants": {"": {"model": NS + n}}})
    item(n)
# stairs / slabs
STAIRS = {"roof_red_stairs": "roof_red", "roof_blue_stairs": "roof_blue", "thatch_stairs": "thatch", "sizo_planks_stairs": "sizo_planks",
          "elisar_bricks_stairs": "elisar_bricks", "elisar_cobble_stairs": "elisar_cobble"}
SLABS = {"roof_red_slab": "roof_red", "roof_blue_slab": "roof_blue", "thatch_slab": "thatch", "sizo_planks_slab": "sizo_planks",
         "ambra_planks_slab": "ambra_planks", "elisar_cobble_slab": "elisar_cobble"}
for n, base in STAIRS.items():
    t = {"bottom": NS + base, "top": NS + base, "side": NS + base}
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
    item(n); DROPS.append(n)
for n, base in SLABS.items():
    t = {"bottom": NS + base, "top": NS + base, "side": NS + base}
    w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/slab", "textures": t})
    w(f"{A}/models/block/{n}_top.json", {"parent": "minecraft:block/slab_top", "textures": t})
    w(f"{A}/blockstates/{n}.json", {"variants": {"type=bottom": {"model": NS + n}, "type=top": {"model": NS + n + "_top"},
                                                "type=double": {"model": NS + base}}})
    item(n)
    w(f"{D}/loot_tables/blocks/{n}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "riftworld:" + n,
        "functions": [{"function": "minecraft:set_count", "count": 2, "add": False, "conditions": [{"condition": "minecraft:block_state_property",
                       "block": "riftworld:" + n, "properties": {"type": "double"}}]}]}]}]})
# fences (vanilla fence templates)
for n in ("elisar_fence", "rope_post"):
    t = {"texture": NS + n}
    for sub in ("post", "side", "inventory"):
        w(f"{A}/models/block/{n}_{sub}.json", {"parent": "minecraft:block/fence_" + sub, "textures": t})
    mp = [{"apply": {"model": NS + n + "_post"}}]
    for face, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        e = {"model": NS + n + "_side", "uvlock": True}
        if y: e["y"] = y
        mp.append({"when": {face: "true"}, "apply": e})
    w(f"{A}/blockstates/{n}.json", {"multipart": mp})
    item(n, NS + n + "_inventory"); DROPS.append(n)
# window pane (glass-pane templates)
t = {"pane": NS + "elisar_window", "edge": NS + "elisar_window", "particle": NS + "elisar_window"}
for sub in ("post", "side", "side_alt", "noside", "noside_alt"):
    w(f"{A}/models/block/elisar_window_{sub}.json", {"parent": "minecraft:block/template_glass_pane_" + sub, "textures": t, "render_type": "minecraft:translucent"})
P = NS + "elisar_window_"
w(f"{A}/blockstates/elisar_window.json", {"multipart": [
    {"apply": {"model": P + "post"}},
    {"when": {"north": "true"}, "apply": {"model": P + "side"}}, {"when": {"east": "true"}, "apply": {"model": P + "side", "y": 90}},
    {"when": {"south": "true"}, "apply": {"model": P + "side_alt"}}, {"when": {"west": "true"}, "apply": {"model": P + "side_alt", "y": 90}},
    {"when": {"north": "false"}, "apply": {"model": P + "noside"}}, {"when": {"east": "false"}, "apply": {"model": P + "noside_alt"}},
    {"when": {"south": "false"}, "apply": {"model": P + "noside_alt", "y": 90}}, {"when": {"west": "false"}, "apply": {"model": P + "noside", "y": 270}}]})
gen_item("elisar_window", NS + "elisar_window")
w(f"{D}/loot_tables/blocks/elisar_window.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "riftworld:elisar_window"}],
    "conditions": [{"condition": "minecraft:match_tool", "predicate": {"enchantments": [{"enchantment": "minecraft:silk_touch", "levels": {"min": 1}}]}}]}]})
# door
dt = {"bottom": NS + "elisar_door_bottom", "top": NS + "elisar_door_top"}
for half in ("bottom", "top"):
    for hinge in ("left", "right"):
        for op in ("", "_open"):
            w(f"{A}/models/block/elisar_door_{half}_{hinge}{op}.json", {"parent": f"minecraft:block/door_{half}_{hinge}{op}", "textures": dt, "render_type": "minecraft:cutout"})
v = {}
ROT = {"east": 0, "south": 90, "west": 180, "north": 270}
for facing, y0 in ROT.items():
    for half, hm in (("lower", "bottom"), ("upper", "top")):
        for hinge in ("left", "right"):
            for op in (False, True):
                y = y0
                if op:
                    y += 90 if hinge == "left" else 270
                model = f"{hm}_{hinge}" + ("_open" if op else "")
                e = {"model": NS + "elisar_door_" + model}
                if y % 360: e["y"] = y % 360
                v[f"facing={facing},half={half},hinge={hinge},open={str(op).lower()}"] = e
w(f"{A}/blockstates/elisar_door.json", {"variants": v})
gen_item("elisar_door", "riftworld:item/elisar_door")
w(f"{D}/loot_tables/blocks/elisar_door.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "riftworld:elisar_door",
    "conditions": [{"condition": "minecraft:block_state_property", "block": "riftworld:elisar_door", "properties": {"half": "lower"}}]}]}]})
# plants
for n in ("elisar_tall_grass", "elisar_flowers"):
    w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/cross", "textures": {"cross": NS + n}, "render_type": "minecraft:cutout"})
    w(f"{A}/blockstates/{n}.json", {"variants": {"": {"model": NS + n}}})
    gen_item(n, NS + n)
# ivy (vanilla vine template)
w(f"{A}/models/block/elisar_ivy.json", {"parent": "minecraft:block/vine", "textures": {"vine": NS + "elisar_ivy", "particle": NS + "elisar_ivy"}, "render_type": "minecraft:cutout"})
mp = [{"when": {"up": "true"}, "apply": {"model": NS + "elisar_ivy", "x": 270, "uvlock": True}}]
for face, y in (("south", 0), ("west", 90), ("north", 180), ("east", 270)):
    e = {"model": NS + "elisar_ivy", "uvlock": True}
    if y: e["y"] = y
    mp.append({"when": {face: "true"}, "apply": e})
w(f"{A}/blockstates/elisar_ivy.json", {"multipart": mp})
gen_item("elisar_ivy", NS + "elisar_ivy")
# water
w(f"{A}/models/block/elisar_water.json", {"textures": {"particle": NS + "elisar_water_still"}})
w(f"{A}/blockstates/elisar_water.json", {"variants": {"": {"model": NS + "elisar_water"}}})
gen_item("elisar_water_bucket", "riftworld:item/elisar_water_bucket")
# Elisar model props: particle + builtin item model
PROPS = open("/home/user/notes/elisar_models.txt").read().strip().split(",")
STONEY = {"well", "idol", "altar_stone", "forge_hearth", "fireplace"}
ROT = {"south": 0, "west": 90, "north": 180, "east": 270}
for n in PROPS:
    tex = "riftworld:block/obj/" + ("m_e_stone" if n in STONEY else "m_e_wood")
    has_off = os.path.exists(f"{A}/models/block/obj/{n}_off.obj")
    for suffix in ("", "_off") if has_off else ("",):
        w(f"{A}/models/block/{n}{suffix}.json", {"loader": "forge:obj", "model": f"riftworld:models/block/obj/{n}{suffix}.obj",
                                                 "flip_v": False, "emissive_ambient": True, "automatic_culling": False, "shade_quads": True,
                                                 "render_type": "minecraft:cutout", "ambientocclusion": False, "textures": {"particle": tex}})
    v = {}
    for f, y in ROT.items():
        for pw in ("false", "true"):
            m = NS + n + ("_off" if has_off and pw == "false" else "")
            v[f"facing={f},powered={pw}"] = {"model": m, "y": y} if y else {"model": m}
    w(f"{A}/blockstates/{n}.json", {"variants": v})
    big = n in ("market_stall", "market_stall_fish", "wagon_overgrown", "forge_hearth", "fireplace", "bar_counter", "rowboat", "well", "idol", "painting_eye")
    tall = n in ("lantern_post", "kitchen_stove", "dish_cupboard", "bookshelf_e", "street_lamp", "painting_islands", "painting_portrait")
    s_ = 0.3 if big else (0.4 if tall else 0.6)
    w(f"{A}/models/item/{n}.json", {"parent": NS + n, "gui_light": "front", "display": {
        "gui": {"rotation": [30, 225, 0], "translation": [0, -3 if tall else 0, 0], "scale": [s_, s_, s_]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [s_, s_, s_]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]}}})
    DROPS.append(n)
for n in DROPS + ["sizo_leaves", "ambra_leaves", "elisar_tall_grass", "elisar_flowers", "elisar_ivy"]:
    if n in ("sizo_leaves", "ambra_leaves", "elisar_tall_grass", "elisar_flowers", "elisar_ivy"):
        w(f"{D}/loot_tables/blocks/{n}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "riftworld:" + n}],
            "conditions": [{"condition": "minecraft:match_tool", "predicate": {"items": ["minecraft:shears"]}}]}]})
    else:
        w(f"{D}/loot_tables/blocks/{n}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "riftworld:" + n}],
                                                                                  "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
# chain (vanilla ChainBlock): parented to the vanilla chain geometry, own texture
w(f"{A}/models/block/elisar_chain.json", {"parent": "minecraft:block/chain", "render_type": "minecraft:cutout", "textures": {"particle": "riftworld:block/elisar_chain", "all": "riftworld:block/elisar_chain"}})
w(f"{A}/blockstates/elisar_chain.json", {"variants": {"axis=x": {"model": NS + "elisar_chain", "x": 90, "y": 90}, "axis=y": {"model": NS + "elisar_chain"},
                                                      "axis=z": {"model": NS + "elisar_chain", "x": 90}}})
gen_item("elisar_chain", "riftworld:block/elisar_chain")
w(f"{D}/loot_tables/blocks/elisar_chain.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "riftworld:elisar_chain"}],
                                                                                  "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
# tags
T = R + "/data/minecraft/tags/"
ID = lambda l: ["riftworld:" + x for x in l]
WOODY = ["sizo_log", "ambra_log", "sizo_planks", "ambra_planks", "sizo_planks_stairs", "sizo_planks_slab", "ambra_planks_slab", "elisar_fence",
         "rope_post", "elisar_door", "timber_frame_cross", "timber_frame_post"] + [p for p in PROPS if p not in STONEY]
STONES = ["elisar_stone", "elisar_stone_mossy", "elisar_cobble", "elisar_bricks", "elisar_bricks_mossy", "elisar_plaster", "roof_red", "roof_blue",
          "roof_red_stairs", "roof_blue_stairs", "elisar_chain", "roof_red_slab", "roof_blue_slab", "elisar_bricks_stairs", "elisar_cobble_stairs", "elisar_cobble_slab"] + sorted(STONEY)
merge(T + "blocks/mineable/axe.json", {"values": ID(WOODY)})
merge(T + "blocks/mineable/pickaxe.json", {"values": ID(STONES)})
merge(T + "blocks/mineable/shovel.json", {"values": ID(["elisar_grass", "elisar_soil", "elisar_seabed", "elisar_path"])})
merge(T + "blocks/mineable/hoe.json", {"values": ID(["sizo_leaves", "ambra_leaves", "thatch", "thatch_stairs", "thatch_slab"])})
merge(T + "blocks/logs.json", {"values": ID(["sizo_log", "ambra_log"])})
merge(T + "blocks/leaves.json", {"values": ID(["sizo_leaves", "ambra_leaves"])})
merge(T + "blocks/planks.json", {"values": ID(["sizo_planks", "ambra_planks"])})
merge(T + "blocks/wooden_fences.json", {"values": ID(["elisar_fence", "rope_post"])})
merge(T + "blocks/fences.json", {"values": ID(["elisar_fence", "rope_post"])})
merge(T + "blocks/wooden_doors.json", {"values": ID(["elisar_door"])})
merge(T + "blocks/doors.json", {"values": ID(["elisar_door"])})
merge(T + "blocks/stairs.json", {"values": ID(list(STAIRS))})
merge(T + "blocks/slabs.json", {"values": ID(list(SLABS))})
merge(T + "blocks/climbable.json", {"values": ID(["elisar_ivy"])})
merge(T + "blocks/replaceable_by_trees.json", {"values": ID(["elisar_tall_grass", "elisar_flowers"])})
merge(T + "items/stairs.json", {"values": ID(list(STAIRS))})
merge(T + "items/slabs.json", {"values": ID(list(SLABS))})
merge(T + "items/logs.json", {"values": ID(["sizo_log", "ambra_log"])})
merge(T + "items/planks.json", {"values": ID(["sizo_planks", "ambra_planks"])})
merge(R + "/data/riftworld/tags/fluids/elisar_water.json", {"values": ["riftworld:elisar_water", "riftworld:flowing_elisar_water"]})

NAMES = {
    "elisar_grass": ("Elisar Turf", "Дёрн Элисара"), "elisar_soil": ("Rust Soil", "Ржавая почва"), "elisar_stone": ("Lilac Stone", "Сиреневый камень"),
    "elisar_stone_mossy": ("Mossy Lilac Stone", "Замшелый сиреневый камень"), "elisar_seabed": ("Seabed Sand", "Донный песок"),
    "elisar_cobble": ("Road Cobblestone", "Мостовая"), "elisar_path": ("Trodden Path", "Утоптанная тропа"),
    "elisar_bricks": ("Elisar Bricks", "Кирпич Элисара"), "elisar_bricks_mossy": ("Mossy Elisar Bricks", "Замшелый кирпич Элисара"),
    "elisar_plaster": ("Plaster", "Штукатурка"), "timber_frame_cross": ("Timber Frame (Cross)", "Фахверк (крест)"),
    "timber_frame_post": ("Timber Frame (Posts)", "Фахверк (стойки)"), "sizo_planks": ("Sizotrunk Planks", "Доски сизоствола"),
    "ambra_planks": ("Ambertree Planks", "Доски янтарника"), "thatch": ("Thatch", "Солома"), "roof_red": ("Red Roof Tiles", "Красная черепица"),
    "roof_blue": ("Blue Roof Tiles", "Синяя черепица"), "sizo_log": ("Sizotrunk Log", "Бревно сизоствола"), "ambra_log": ("Ambertree Log", "Бревно янтарника"),
    "sizo_leaves": ("Sizotrunk Leaves", "Листва сизоствола"), "ambra_leaves": ("Ambertree Leaves", "Листва янтарника"),
    "roof_red_stairs": ("Red Roof Stairs", "Красная черепица (скат)"), "roof_blue_stairs": ("Blue Roof Stairs", "Синяя черепица (скат)"),
    "thatch_stairs": ("Thatch Stairs", "Соломенный скат"), "sizo_planks_stairs": ("Sizotrunk Stairs", "Ступени из сизоствола"),
    "elisar_bricks_stairs": ("Elisar Brick Stairs", "Кирпичные ступени Элисара"), "elisar_cobble_stairs": ("Cobblestone Stairs", "Ступени мостовой"),
    "roof_red_slab": ("Red Roof Slab", "Плита красной черепицы"), "roof_blue_slab": ("Blue Roof Slab", "Плита синей черепицы"),
    "thatch_slab": ("Thatch Slab", "Соломенная плита"), "sizo_planks_slab": ("Sizotrunk Slab", "Плита из сизоствола"),
    "ambra_planks_slab": ("Ambertree Slab", "Плита из янтарника"), "elisar_cobble_slab": ("Cobblestone Slab", "Плита мостовой"),
    "elisar_fence": ("Elisar Fence", "Забор Элисара"), "rope_post": ("Rope Post", "Столбик с канатом"), "elisar_chain": ("Elisar Chain", "Цепь Элисара"), "elisar_window": ("Leaded Window", "Окно с переплётом"),
    "elisar_door": ("Plank Door", "Дощатая дверь"), "elisar_tall_grass": ("Blue Grass", "Синяя трава"), "elisar_flowers": ("Starbells", "Звездоцветы"),
    "elisar_ivy": ("Elisar Ivy", "Плющ Элисара"), "elisar_water": ("Elisar Water", "Вода Элисара"),
    "street_lamp": ("Street Lamp", "Уличный фонарь"), "lantern_hanging": ("Hanging Candle Chandelier", "Подвесная люстра со свечами"),
    "lantern_hang": ("Hanging Lantern", "Подвесной фонарь"), "lantern_post": ("Lantern Post", "Фонарный столб"), "lantern_wall": ("Wall Lantern", "Настенный фонарь"),
    "painting_islands": ("Painting «Islands»", "Картина «Острова»"), "painting_portrait": ("Portrait", "Портрет"), "painting_eye": ("Painting «The Eye»", "Картина «Око»"),
    "kitchen_stove": ("Kitchen Stove", "Кухонная печь"), "kitchen_counter": ("Kitchen Counter", "Кухонный стол"), "dish_cupboard": ("Dish Cupboard", "Буфет с посудой"),
    "bookshelf_e": ("Bookshelf", "Книжный шкаф"), "sacks": ("Grain Sacks", "Мешки с зерном"), "firewood_stack": ("Firewood Stack", "Поленница"),
    "fire_pit": ("Fire Pit", "Кострище"), "flower_pot_e": ("Flower Pot", "Цветочный горшок"), "herbs_hanging": ("Drying Herbs", "Сушёные травы"), "sign_shop": ("Shop Sign", "Вывеска «Лавка»"), "sign_tavern": ("Tavern Sign", "Вывеска «Трактир»"),
    "sign_forge": ("Forge Sign", "Вывеска «Кузница»"), "sign_fish": ("Fishmonger Sign", "Вывеска «Рыба»"), "sign_police": ("Guard Post Sign", "Вывеска «Стража»"),
    "sign_hunter": ("Hunter Sign", "Вывеска «Охотник»"), "sign_market": ("Market Sign", "Вывеска «Рынок»"), "sign_temple": ("Temple Sign", "Вывеска «Храм»"),
    "bounty_board": ("Bounty Board", "Доска розыска"), "market_stall": ("Produce Stall", "Прилавок с овощами"),
    "market_stall_fish": ("Fish Stall", "Рыбный прилавок"), "wagon_overgrown": ("Abandoned Wagon", "Брошенная повозка"), "bench": ("Bench", "Скамья"),
    "well": ("Well", "Колодец"), "fish_barrel": ("Barrel of Glowfin", "Бочка светопёрок"), "fish_rack": ("Fish Drying Rack", "Сушилка для рыбы"),
    "fishing_net": ("Fishing Net", "Рыболовная сеть"), "rowboat": ("Rowboat on Trestles", "Лодка на козлах"), "trophy_antlers": ("Antler Trophy", "Охотничий трофей"),
    "hide_rack": ("Hide Stretcher", "Рама со шкурой"), "weapon_rack": ("Weapon Rack", "Стойка с оружием"), "forge_hearth": ("Forge Hearth", "Кузнечный горн"),
    "anvil_stump": ("Anvil on a Stump", "Наковальня на пне"), "quench_barrel": ("Quench Barrel", "Бочка для закалки"), "tool_wall": ("Tool Board", "Доска с инструментами"),
    "idol": ("Carved Idol", "Резной идол"), "altar_stone": ("Stone Altar", "Каменный алтарь"), "fire_bowl": ("Fire Bowl", "Жаровня"),
    "sacred_poles": ("Sacred Poles", "Священные шесты"), "table_wood": ("Wooden Table", "Деревянный стол"), "chair_wood": ("Wooden Chair", "Деревянный стул"),
    "stool": ("Stool", "Табурет"), "bed_wood": ("Wooden Bed", "Деревянная кровать"), "wardrobe": ("Wardrobe", "Шкаф"), "bar_counter": ("Bar Counter", "Барная стойка"),
    "keg_rack": ("Keg Rack", "Стеллаж с бочонками"), "fireplace": ("Fireplace", "Камин"), "shop_counter": ("Shop Counter", "Прилавок лавки"),
    "shop_shelf": ("Goods Shelf", "Полка с товаром"), "rug": ("Woven Rug", "Тканый ковёр"), "cooking_pot": ("Cooking Pot", "Котелок"),
    "wanted_wall": ("Wanted Posters", "Листовки «Разыскивается»"), "crate_stack": ("Crates and Sack", "Ящики и мешок"),
}
assert all(p in NAMES for p in PROPS), [p for p in PROPS if p not in NAMES]
en = {"biome.riftworld.elisar_city": "Wanderers (Elisar)", "item.riftworld.elisar_water_bucket": "Bucket of Elisar Water", "fluid_type.riftworld.elisar_water": "Elisar Water"}
ru = {"biome.riftworld.elisar_city": "Странники (Элисар)", "item.riftworld.elisar_water_bucket": "Ведро воды Элисара", "fluid_type.riftworld.elisar_water": "Вода Элисара"}
for k, (e, r) in NAMES.items():
    en["block.riftworld." + k] = e; ru["block.riftworld." + k] = r
merge(A + "/lang/en_us.json", en); merge(A + "/lang/ru_ru.json", ru)
print("elisar resources OK", len(NAMES))
