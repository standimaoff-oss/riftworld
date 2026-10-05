"""Rev 3 / C-1 castle blocks: blockstates / models / items / loot / tags / lang. Run AFTER gen_resources_elisar.py (merges)."""
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


def drop(n):
    w(f"{D}/loot_tables/blocks/{n}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "riftworld:" + n}],
                                                                              "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


CUBES = ["castle_bricks", "castle_bricks_cracked", "castle_bricks_mossy", "ashlar", "dark_planks", "brass_block", "castle_mosaic",
         "castle_roof", "ash_block", "burnt_soil", "scorched_stone"]
RANDROT = {"ash_block", "burnt_soil", "scorched_stone"}
for n in CUBES:
    rots = (0, 90, 180, 270) if n in RANDROT else (0,)
    w(f"{A}/blockstates/{n}.json", {"variants": {"": [{"model": NS + n, "y": r} for r in rots]}})
    w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": NS + n}})
    item(n); drop(n)

# pillars (axis)
for n, end in (("ashlar_pillar", "ashlar_pillar_top"), ("dark_beam", "dark_beam_top"), ("charred_log", "charred_log_top"), ("castle_bookshelf", "castle_bookshelf_top")):
    w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/cube_column", "textures": {"end": NS + end, "side": NS + n}})
    w(f"{A}/models/block/{n}_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal", "textures": {"end": NS + end, "side": NS + n}})
    w(f"{A}/blockstates/{n}.json", {"variants": {"axis=y": {"model": NS + n}, "axis=z": {"model": NS + n + "_horizontal", "x": 90},
                                                "axis=x": {"model": NS + n + "_horizontal", "x": 90, "y": 90}}})
    item(n); drop(n)

STAIRS = {"castle_bricks_stairs": "castle_bricks", "ashlar_stairs": "ashlar", "dark_planks_stairs": "dark_planks", "castle_roof_stairs": "castle_roof"}
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
    item(n); drop(n)

SLABS = {"castle_bricks_slab": "castle_bricks", "ashlar_slab": "ashlar", "dark_planks_slab": "dark_planks", "castle_roof_slab": "castle_roof"}
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

# wall (vanilla wall templates)
n = "castle_bricks_wall"
t = {"wall": NS + "castle_bricks"}
for sub, par in (("post", "template_wall_post"), ("side", "template_wall_side"), ("side_tall", "template_wall_side_tall"), ("inventory", "wall_inventory")):
    w(f"{A}/models/block/{n}_{sub}.json", {"parent": "minecraft:block/" + par, "textures": t})
mp = [{"when": {"up": "true"}, "apply": {"model": NS + n + "_post"}}]
for face, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
    for hgt, sub in (("low", "side"), ("tall", "side_tall")):
        e = {"model": NS + n + "_" + sub, "uvlock": True}
        if y: e["y"] = y
        mp.append({"when": {face: hgt}, "apply": e})
w(f"{A}/blockstates/{n}.json", {"multipart": mp})
item(n, NS + n + "_inventory"); drop(n)

# dark fence (balustrades of the galleries)
n = "dark_fence"
t = {"texture": NS + "dark_planks"}
for sub in ("post", "side", "inventory"):
    w(f"{A}/models/block/{n}_{sub}.json", {"parent": "minecraft:block/fence_" + sub, "textures": t})
mp = [{"apply": {"model": NS + n + "_post"}}]
for face, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
    e = {"model": NS + n + "_side", "uvlock": True}
    if y: e["y"] = y
    mp.append({"when": {face: "true"}, "apply": e})
w(f"{A}/blockstates/{n}.json", {"multipart": mp})
item(n, NS + n + "_inventory"); drop(n)

# stained glass pane
n = "stained_glass"
t = {"pane": NS + n, "edge": NS + n, "particle": NS + n}
for sub in ("post", "side", "side_alt", "noside", "noside_alt"):
    w(f"{A}/models/block/{n}_{sub}.json", {"parent": "minecraft:block/template_glass_pane_" + sub, "textures": t, "render_type": "minecraft:translucent"})
P = NS + n + "_"
w(f"{A}/blockstates/{n}.json", {"multipart": [
    {"apply": {"model": P + "post"}},
    {"when": {"north": "true"}, "apply": {"model": P + "side"}}, {"when": {"east": "true"}, "apply": {"model": P + "side", "y": 90}},
    {"when": {"south": "true"}, "apply": {"model": P + "side_alt"}}, {"when": {"west": "true"}, "apply": {"model": P + "side_alt", "y": 90}},
    {"when": {"north": "false"}, "apply": {"model": P + "noside"}}, {"when": {"east": "false"}, "apply": {"model": P + "noside_alt"}},
    {"when": {"south": "false"}, "apply": {"model": P + "noside_alt", "y": 90}}, {"when": {"west": "false"}, "apply": {"model": P + "noside", "y": 270}}]})
w(f"{A}/models/item/{n}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": NS + n}, "render_type": "minecraft:translucent"})
w(f"{D}/loot_tables/blocks/{n}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "riftworld:" + n}],
    "conditions": [{"condition": "minecraft:match_tool", "predicate": {"enchantments": [{"enchantment": "minecraft:silk_touch", "levels": {"min": 1}}]}}]}]})

# C-2: iron bars (pane geometry, cutout)
n = "castle_bars"
t = {"bars": NS + n, "edge": NS + n, "particle": NS + n}
for sub in ("post_ends", "post", "cap", "cap_alt", "side", "side_alt"):
    w(f"{A}/models/block/{n}_{sub}.json", {"parent": "minecraft:block/iron_bars_" + sub, "textures": t, "render_type": "minecraft:cutout_mipped"})
P = NS + n + "_"
w(f"{A}/blockstates/{n}.json", {"multipart": [
    {"apply": {"model": P + "post_ends"}},
    {"when": {"north": "false", "west": "false", "south": "false", "east": "false"}, "apply": {"model": P + "post"}},
    {"when": {"north": "true", "west": "false", "south": "false", "east": "false"}, "apply": {"model": P + "cap"}},
    {"when": {"north": "false", "west": "false", "south": "false", "east": "true"}, "apply": {"model": P + "cap", "y": 90}},
    {"when": {"north": "false", "west": "false", "south": "true", "east": "false"}, "apply": {"model": P + "cap_alt"}},
    {"when": {"north": "false", "west": "true", "south": "false", "east": "false"}, "apply": {"model": P + "cap_alt", "y": 90}},
    {"when": {"north": "true"}, "apply": {"model": P + "side"}}, {"when": {"east": "true"}, "apply": {"model": P + "side", "y": 90}},
    {"when": {"south": "true"}, "apply": {"model": P + "side_alt"}}, {"when": {"west": "true"}, "apply": {"model": P + "side_alt", "y": 90}}]})
w(f"{A}/models/item/{n}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": NS + n}, "render_type": "minecraft:cutout"})
drop(n)
# carpet
n = "castle_carpet"
w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/carpet", "textures": {"wool": NS + n}})
w(f"{A}/blockstates/{n}.json", {"variants": {"": {"model": NS + n}}})
item(n); drop(n)
# cobweb (cross, cutout; drops nothing without shears/silk like vanilla - simplified: drops string)
n = "castle_cobweb"
w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/cross", "textures": {"cross": NS + n}, "render_type": "minecraft:cutout"})
w(f"{A}/blockstates/{n}.json", {"variants": {"": {"model": NS + n}}})
w(f"{A}/models/item/{n}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": NS + n}})
w(f"{D}/loot_tables/blocks/{n}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
    {"type": "minecraft:item", "name": "riftworld:" + n, "conditions": [{"condition": "minecraft:match_tool", "predicate": {"items": ["minecraft:shears"]}}]},
    {"type": "minecraft:item", "name": "minecraft:string", "conditions": [{"condition": "minecraft:survives_explosion"}]}]}]}]})

# baked OBJ props
ROT = {"south": 0, "west": 90, "north": 180, "east": 270}
for n, tex, s_ in (("candle_stand", "m_e_iron", 0.4), ("boss_throne", "m_c_darkwood", 0.22),
                   ("castle_chandelier", "m_e_iron", 0.4), ("wall_sconce", "m_e_iron", 0.5), ("feast_table", "m_c_darkwood", 0.25),
                   ("long_table", "m_c_darkwood", 0.25), ("castle_bench", "m_c_darkwood", 0.25), ("high_chair", "m_c_darkwood", 0.3),
                   ("guard_weapon_rack", "m_c_darkwood", 0.2), ("guard_armor", "m_c_blacksteel", 0.3), ("canopy_bed", "m_c_velvet", 0.2),
                   ("castle_chest", "m_c_darkwood", 0.5), ("chained_lectern", "m_c_darkwood", 0.3), ("chapel_altar", "m_c_ashlar", 0.25),
                   ("hearth_cauldron", "m_e_iron", 0.3), ("alchemy_table", "m_c_darkwood", 0.22), ("alchemy_set", "m_c_darkwood", 0.22), ("wall_shackles", "m_e_iron", 0.3),
                   ("chained_books", "m_c_darkwood", 0.3), ("kitchen_set", "e_wood", 0.5),
                   ("gold_hoard", "m_c_coins", 0.5), ("tapestry_horn", "m_c_tapestry", 0.22), ("bone_pile", "m_e_bone", 0.5)):
    has_off = os.path.exists(f"{A}/models/block/obj/{n}_off.obj")
    for suffix in ("", "_off") if has_off else ("",):
        w(f"{A}/models/block/{n}{suffix}.json", {"loader": "forge:obj", "model": f"riftworld:models/block/obj/{n}{suffix}.obj",
                                                 "flip_v": False, "emissive_ambient": True, "automatic_culling": False, "shade_quads": True,
                                                 "render_type": "minecraft:cutout", "ambientocclusion": False, "textures": {"particle": "riftworld:block/obj/" + tex}})
    v = {}
    for f, y in ROT.items():
        for pw in ("false", "true"):
            m = NS + n + ("_off" if has_off and pw == "false" else "")
            v[f"facing={f},powered={pw}"] = {"model": m, "y": y} if y else {"model": m}
    w(f"{A}/blockstates/{n}.json", {"variants": v})
    w(f"{A}/models/item/{n}.json", {"parent": NS + n, "gui_light": "front", "display": {
        "gui": {"rotation": [30, 225, 0], "translation": [0, -3, 0], "scale": [s_, s_, s_]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.2, 0.2, 0.2]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [s_, s_, s_]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.25, 0.25, 0.25]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.25, 0.25, 0.25]}}})
    drop(n)

# tags
T = R + "/data/minecraft/tags/"
ID = lambda l: ["riftworld:" + x for x in l]
STONES = ["castle_bricks", "castle_bricks_cracked", "castle_bricks_mossy", "castle_bricks_stairs", "castle_bricks_slab", "castle_bricks_wall",
          "ashlar", "ashlar_stairs", "ashlar_slab", "ashlar_pillar", "brass_block", "castle_mosaic", "castle_roof", "castle_roof_stairs",
          "castle_roof_slab", "scorched_stone", "candle_stand", "castle_bars", "chapel_altar", "castle_chandelier", "wall_sconce",
          "wall_shackles", "guard_armor", "gold_hoard", "hearth_cauldron"]
WOODY = ["dark_planks", "dark_planks_stairs", "dark_planks_slab", "dark_beam", "dark_fence", "charred_log", "boss_throne", "castle_bookshelf",
         "feast_table", "long_table", "castle_bench", "high_chair", "guard_weapon_rack", "canopy_bed", "castle_chest", "chained_lectern",
         "alchemy_table", "alchemy_set", "chained_books", "kitchen_set", "tapestry_horn"]
merge(T + "blocks/mineable/pickaxe.json", {"values": ID(STONES)})
merge(T + "blocks/mineable/axe.json", {"values": ID(WOODY)})
merge(T + "blocks/mineable/shovel.json", {"values": ID(["ash_block", "burnt_soil"])})
merge(T + "blocks/walls.json", {"values": ID(["castle_bricks_wall"])})
merge(T + "blocks/wooden_fences.json", {"values": ID(["dark_fence"])})
merge(T + "blocks/stairs.json", {"values": ID(list(STAIRS))})
merge(T + "blocks/slabs.json", {"values": ID(list(SLABS))})
merge(T + "items/walls.json", {"values": ID(["castle_bricks_wall"])})
merge(T + "items/wooden_fences.json", {"values": ID(["dark_fence"])})

# lang
L = {
    "castle_bricks": ("Castle Bricks", "Замковый кирпич"),
    "castle_bricks_cracked": ("Cracked Castle Bricks", "Потрескавшийся замковый кирпич"),
    "castle_bricks_mossy": ("Mossy Castle Bricks", "Замшелый замковый кирпич"),
    "castle_bricks_stairs": ("Castle Brick Stairs", "Ступени из замкового кирпича"),
    "castle_bricks_slab": ("Castle Brick Slab", "Плита из замкового кирпича"),
    "castle_bricks_wall": ("Castle Brick Wall", "Ограда из замкового кирпича"),
    "ashlar": ("Grey Ashlar", "Серый тёсаный камень"),
    "ashlar_stairs": ("Ashlar Stairs", "Ступени из тёсаного камня"),
    "ashlar_slab": ("Ashlar Slab", "Плита из тёсаного камня"),
    "ashlar_pillar": ("Ashlar Pillar", "Колонна из тёсаного камня"),
    "dark_planks": ("Dark Oak Boards", "Тёмные доски"),
    "dark_planks_stairs": ("Dark Board Stairs", "Ступени из тёмных досок"),
    "dark_planks_slab": ("Dark Board Slab", "Плита из тёмных досок"),
    "dark_beam": ("Dark Beam", "Тёмная балка"),
    "dark_fence": ("Dark Balustrade", "Тёмная балюстрада"),
    "brass_block": ("Brass Ornament", "Латунный орнамент"),
    "castle_mosaic": ("Castle Mosaic", "Замковая мозаика"),
    "stained_glass": ("Stained Glass", "Витраж"),
    "castle_roof": ("Castle Roof Tiles", "Замковая черепица"),
    "castle_roof_stairs": ("Castle Roof Stairs", "Скат замковой черепицы"),
    "castle_roof_slab": ("Castle Roof Slab", "Плита замковой черепицы"),
    "ash_block": ("Ash", "Пепел"),
    "burnt_soil": ("Burnt Soil", "Выжженная земля"),
    "scorched_stone": ("Scorched Stone", "Опалённый камень"),
    "charred_log": ("Charred Log", "Обугленное бревно"),
    "candle_stand": ("Iron Candle Stand", "Железный подсвечник"),
    "boss_throne": ("Throne of the Smouldering Horn", "Трон Тлеющего Рога"),
    "castle_bookshelf": ("Castle Bookshelf", "Замковый книжный шкаф"),
    "castle_bars": ("Castle Bars", "Решётка темницы"),
    "castle_carpet": ("Crimson Carpet", "Багровая дорожка"),
    "castle_cobweb": ("Old Cobweb", "Старая паутина"),
    "castle_chandelier": ("Wrought-Iron Chandelier", "Кованая люстра"),
    "wall_sconce": ("Wall Sconce", "Настенный светец"),
    "feast_table": ("Feast Table", "Пиршественный стол"),
    "long_table": ("Long Oak Table", "Длинный дубовый стол"),
    "castle_bench": ("Castle Bench", "Замковая скамья"),
    "high_chair": ("High-Backed Chair", "Кресло с высокой спинкой"),
    "guard_weapon_rack": ("Guards' Weapon Rack", "Стойка с оружием стражи"),
    "guard_armor": ("Guard Armour Stand", "Доспех стражника"),
    "canopy_bed": ("Canopy Bed", "Кровать с балдахином"),
    "castle_chest": ("Iron-Bound Chest", "Окованный сундук"),
    "chained_lectern": ("Chained Lectern", "Аналой с цепью"),
    "chapel_altar": ("Chapel Altar", "Алтарь часовни"),
    "hearth_cauldron": ("Hearth Cauldron", "Котёл над очагом"),
    "alchemy_table": ("Alchemist's Bench", "Алхимический стол"),
    "alchemy_set": ("Alchemist's Glassware", "Алхимическая посуда"),
    "chained_books": ("Chained Folio", "Фолиант на цепи"),
    "kitchen_set": ("Kitchen Dishes", "Кухонная утварь"),
    "wall_shackles": ("Wall Shackles", "Кандалы"),
    "gold_hoard": ("Gold Hoard", "Груда золота"),
    "tapestry_horn": ("Horned Tapestry", "Гобелен с рогатым черепом"),
    "bone_pile": ("Bone Pile", "Груда костей"),
}
en = {"block.riftworld." + k: v[0] for k, v in L.items()}
ru = {"block.riftworld." + k: v[1] for k, v in L.items()}
merge(A + "/lang/en_us.json", en); merge(A + "/lang/ru_ru.json", ru)
print("castle resources ok", len(L))

# ---- C-2: castle chest loot tables (vanilla items only)
def _e(name, wt, lo=1, hi=1, ench=False):
    e = {"type": "minecraft:item", "name": "minecraft:" + name, "weight": wt, "functions": []}
    if hi > 1:
        e["functions"].append({"function": "minecraft:set_count", "count": {"min": lo, "max": hi}})
    if ench:
        e["functions"].append({"function": "minecraft:enchant_randomly"})
    if not e["functions"]:
        del e["functions"]
    return e


LOOT = {
    "armory": (3, 6, [_e("iron_sword", 6), _e("iron_axe", 5), _e("shield", 5), _e("arrow", 10, 4, 16), _e("bow", 4),
                      _e("iron_helmet", 3), _e("chainmail_chestplate", 3), _e("iron_ingot", 8, 1, 4), _e("crossbow", 2),
                      _e("iron_sword", 1, ench=True)]),
    "barracks": (2, 5, [_e("bread", 10, 1, 3), _e("leather_chestplate", 4), _e("arrow", 8, 2, 8),
                        _e("stone_sword", 5), _e("candle", 6, 1, 3), _e("string", 6, 1, 4), _e("bone", 6, 1, 3)]),
    "kitchen": (3, 7, [_e("bread", 12, 1, 4), _e("cooked_beef", 8, 1, 3), _e("baked_potato", 8, 1, 4), _e("apple", 8, 1, 3),
                       _e("pumpkin_pie", 3), _e("bowl", 6, 1, 3), _e("sugar", 5, 1, 4), _e("wheat", 6, 2, 6), _e("honey_bottle", 2)]),
    "library": (2, 5, [_e("book", 12, 1, 4), _e("paper", 10, 2, 8), _e("writable_book", 4), _e("ink_sac", 6, 1, 3),
                       _e("feather", 6, 1, 4), _e("enchanted_book", 2, ench=True), _e("candle", 5, 1, 2), _e("map", 2)]),
    "alchemy": (3, 6, [_e("glass_bottle", 10, 1, 4), _e("redstone", 8, 2, 6), _e("glowstone_dust", 6, 1, 4), _e("blaze_powder", 4, 1, 2),
                       _e("nether_wart", 6, 1, 4), _e("spider_eye", 6, 1, 2), _e("gunpowder", 5, 1, 3), _e("fermented_spider_eye", 3),
                       _e("brewing_stand", 1), _e("magma_cream", 3, 1, 2)]),
    "treasury": (4, 8, [_e("gold_ingot", 12, 2, 7), _e("gold_nugget", 12, 4, 16), _e("emerald", 6, 1, 4), _e("diamond", 3, 1, 2),
                        _e("golden_apple", 3), _e("golden_helmet", 3, ench=True), _e("clock", 3), _e("amethyst_shard", 5, 1, 4),
                        _e("enchanted_golden_apple", 1)]),
    "bedchamber": (2, 5, [_e("gold_nugget", 8, 2, 8), _e("book", 6), _e("candle", 6, 1, 3), _e("red_wool", 6, 1, 3), _e("clock", 2),
                          _e("golden_apple", 2), _e("name_tag", 3), _e("music_disc_13", 1)]),
    "storeroom": (3, 7, [_e("coal", 10, 2, 8), _e("torch", 8, 2, 8), _e("wheat", 8, 2, 8), _e("leather", 6, 1, 3), _e("string", 6, 1, 4),
                         _e("iron_nugget", 8, 2, 9), _e("oak_planks", 6, 4, 12), _e("shears", 2), _e("lead", 3)]),
    "cellar": (2, 5, [_e("potion", 6), _e("glass_bottle", 8, 1, 4), _e("sweet_berries", 8, 2, 6), _e("apple", 6, 1, 3),
                      _e("honey_bottle", 4), _e("sugar", 5, 1, 4)]),
    "prison": (1, 3, [_e("bone", 12, 1, 4), _e("rotten_flesh", 10, 1, 3), _e("chain", 6, 1, 2), _e("iron_nugget", 6, 1, 4),
                      _e("string", 5, 1, 2), _e("skeleton_skull", 1)]),
    "keep": (3, 6, [_e("arrow", 10, 4, 16), _e("spyglass", 3), _e("iron_ingot", 6, 1, 4), _e("gold_ingot", 4, 1, 3), _e("bow", 3, ench=True),
                    _e("map", 3), _e("compass", 3), _e("diamond", 1)]),
    "gatehouse": (2, 5, [_e("arrow", 12, 4, 12), _e("iron_nugget", 8, 2, 8), _e("torch", 8, 2, 6), _e("bread", 6, 1, 2), _e("crossbow", 2),
                         _e("chain", 4, 1, 3), _e("tripwire_hook", 3, 1, 2)]),
}
for k, (lo, hi, ents) in LOOT.items():
    w(f"{D}/loot_tables/chests/castle_{k}.json", {"type": "minecraft:chest", "pools": [{"rolls": {"min": lo, "max": hi}, "entries": ents}]})
print("castle loot tables", len(LOOT))
