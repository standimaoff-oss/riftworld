"""Rev 5 «Разлом»: blockstates / models / items / loot / tags / lang of the hellish ritual hall and the rift altar.

The altar block stands in the floor layer of the ritual hall, so its model lives above y = 16 (local units): a plinth,
a column, a capital that carries the small hexagram of the 256x256 sigil texture and the crystal of the rift. Four
stages: 0 dark, 1 lit, 2 burning with cracks, 3 torn open.

Run:  python3 tools/rift/gen_resources_rift.py
"""
import json
import os

R = "/home/user/riftworld/mod/src/main/resources"
A = R + "/assets/riftworld"
D = R + "/data/riftworld"
NS = "riftworld:block/"


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)


def merge(path, add):
    d = {}
    if os.path.exists(path):
        with open(path, encoding="utf-8") as f:
            d = json.load(f)
    d.update(add)
    w(path, d)


def drop(n):
    w(f"{D}/loot_tables/blocks/{n}.json", {"type": "minecraft:block",
      "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "riftworld:" + n}],
                 "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def cube(n, tex=None):
    w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": NS + (tex or n)}})
    w(f"{A}/blockstates/{n}.json", {"variants": {"": {"model": NS + n}}})
    w(f"{A}/models/item/{n}.json", {"parent": NS + n})
    drop(n)


def slab(n, base):
    t = {"bottom": NS + base, "top": NS + base, "side": NS + base}
    w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/slab", "textures": t})
    w(f"{A}/models/block/{n}_top.json", {"parent": "minecraft:block/slab_top", "textures": t})
    w(f"{A}/blockstates/{n}.json", {"variants": {"type=bottom": {"model": NS + n}, "type=top": {"model": NS + n + "_top"},
                                                 "type=double": {"model": NS + base}}})
    w(f"{A}/models/item/{n}.json", {"parent": NS + n})
    w(f"{D}/loot_tables/blocks/{n}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "riftworld:" + n, "functions": [
            {"function": "minecraft:set_count", "count": 2, "add": False, "conditions": [
                {"condition": "minecraft:block_state_property", "block": "riftworld:" + n, "properties": {"type": "double"}}]},
            {"function": "minecraft:explosion_decay"}]}]}]})


def stairs(n, base):
    t = {"bottom": NS + base, "top": NS + base, "side": NS + base}
    for suf, par in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        w(f"{A}/models/block/{n}{suf}.json", {"parent": "minecraft:block/" + par, "textures": t})
    w(f"{A}/blockstates/{n}.json", {"multipart": [
        {"when": {"facing": "east", "half": "bottom", "shape": "straight"}, "apply": {"model": NS + n}},
        {"when": {"facing": "west", "half": "bottom", "shape": "straight"}, "apply": {"model": NS + n, "y": 180, "uvlock": True}},
        {"when": {"facing": "south", "half": "bottom", "shape": "straight"}, "apply": {"model": NS + n, "y": 90, "uvlock": True}},
        {"when": {"facing": "north", "half": "bottom", "shape": "straight"}, "apply": {"model": NS + n, "y": 270, "uvlock": True}}]})
    w(f"{A}/models/item/{n}.json", {"parent": NS + n})
    drop(n)


def wall(n, base):
    t = {"wall": NS + base, "particle": NS + base}
    for sub in ("post", "side", "side_tall"):
        w(f"{A}/models/block/{n}_{sub}.json", {"parent": "minecraft:block/template_wall_" + sub, "textures": t})
    B = NS + n + "_"
    mp = [{"when": {"up": "true"}, "apply": {"model": B + "post"}}]
    for face, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for kind in ("low", "tall"):
            e = {"model": B + ("side" if kind == "low" else "side_tall"), "uvlock": True}
            if y:
                e["y"] = y
            mp.append({"when": {face: kind}, "apply": e})
    w(f"{A}/blockstates/{n}.json", {"multipart": mp})
    w(f"{A}/models/item/{n}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": NS + base}})
    drop(n)


# ------------------------------------------------------------------ the hellish masonry
for n in ("rift_stone", "rift_stone_bricks", "rift_stone_bricks_cracked", "rift_vein"):
    cube(n)
slab("rift_stone_slab", "rift_stone_bricks")
stairs("rift_stone_stairs", "rift_stone_bricks")
wall("rift_stone_wall", "rift_stone_bricks")

# pillar (two textures: runes on the side, hexagram cap on top)
w(f"{A}/models/block/rift_pillar.json", {"parent": "minecraft:block/cube_column",
    "textures": {"side": NS + "rift_pillar", "end": NS + "rift_pillar_top"}})
w(f"{A}/blockstates/rift_pillar.json", {"variants": {"axis=x": {"model": NS + "rift_pillar", "x": 90, "y": 90},
    "axis=y": {"model": NS + "rift_pillar"}, "axis=z": {"model": NS + "rift_pillar", "x": 90}}})
w(f"{A}/models/item/rift_pillar.json", {"parent": NS + "rift_pillar"})
drop("rift_pillar")

# ash carpet
w(f"{A}/models/block/rift_ash.json", {"parent": "minecraft:block/carpet", "textures": {"wool": NS + "rift_ash"},
    "render_type": "minecraft:cutout"})
w(f"{A}/blockstates/rift_ash.json", {"variants": {"": [{"model": NS + "rift_ash", "y": r} for r in (0, 90, 180, 270)]}})
w(f"{A}/models/item/rift_ash.json", {"parent": NS + "rift_ash"})
drop("rift_ash")

# ------------------------------------------------------------------ the candle of the rift
foot = {"from": [3, 0, 3], "to": [13, 2, 13], "faces": {f: {"texture": "#side", "uv": [0, 13, 16, 15]}
        for f in ("north", "east", "south", "west")} | {"up": {"texture": "#side", "uv": [0, 13, 16, 15]},
                                                        "down": {"texture": "#side", "uv": [0, 13, 16, 15]}}}
body = {"from": [5, 2, 5], "to": [11, 14, 11], "faces": {f: {"texture": "#side", "uv": [5, 0, 11, 12]}
        for f in ("north", "east", "south", "west")} | {"up": {"texture": "#side", "uv": [5, 0, 11, 6]},
                                                        "down": {"texture": "#side", "uv": [5, 0, 11, 6]}}}
flame = lambda rot, axis="y": {"from": [8, 13, 4] if axis == "y" else [4, 13, 8], "to": [8, 15, 12] if axis == "y" else [12, 15, 8],
    "rotation": {"origin": [8, 13, 8], "axis": axis, "angle": rot},
    "faces": {("north" if axis == "y" else "east"): {"texture": "#flame", "uv": [0, 0, 16, 16]},
              ("south" if axis == "y" else "west"): {"texture": "#flame", "uv": [0, 0, 16, 16]}}}
w(f"{A}/models/block/rift_candle.json", {"textures": {"side": NS + "rift_candle", "particle": NS + "rift_candle"},
    "elements": [foot, body]})
w(f"{A}/models/block/rift_candle_lit.json", {"textures": {"side": NS + "rift_candle", "flame": NS + "rift_flame",
    "particle": NS + "rift_candle"}, "elements": [foot, body, flame(0), flame(90), flame(0, "x"), flame(90, "x")]})
w(f"{A}/blockstates/rift_candle.json", {"variants": {"lit=false": {"model": NS + "rift_candle"},
                                                     "lit=true": {"model": NS + "rift_candle_lit"}}})
w(f"{A}/models/item/rift_candle.json", {"parent": NS + "rift_candle"})
drop("rift_candle")

# ------------------------------------------------------------------ the altar: the 13x13 hexagram plate
# The altar stands in the FLOOR layer of the hall (template cell 31, G, 34 - the same cell the /riftrift geometry
# counts from), so every element has to live above y = 16 to rise out of the floor: the plinth starts on the floor
# surface (y = 16 = one block up) and the crystal ends exactly at the model ceiling (y = 32). The hexagram itself is
# not part of this model - it is inlaid into the hall floor with the glowing veins (tools/rift/gen_temple.py).
for s in range(4):
    elements = [
        {"from": [2, 16.5, 2], "to": [14, 19, 14], "faces": {f: {"texture": "#altar", "uv": [0, 10, 16, 16]}
            for f in ("north", "east", "south", "west")} | {"up": {"texture": "#altar_top", "uv": [2, 2, 14, 14]}}},
        {"from": [5, 19, 5], "to": [11, 27, 11], "faces": {f: {"texture": "#altar", "uv": [0, 0, 16, 16]}
            for f in ("north", "east", "south", "west")} | {"up": {"texture": "#altar_top", "uv": [5, 5, 11, 11]}}},
        {"from": [3, 27, 3], "to": [13, 29, 13], "faces": {f: {"texture": "#altar_top", "uv": [0, 12, 16, 16]}
            for f in ("north", "east", "south", "west")} | {"up": {"texture": "#sigil", "uv": [0, 0, 16, 16]}}},
        {"from": [6.5, 29, 6.5], "to": [9.5, 32, 9.5], "rotation": {"origin": [8, 30.5, 8], "axis": "y", "angle": 45},
         "faces": {f: {"texture": "#crystal", "uv": [0, 0, 16, 16]} for f in ("north", "east", "south", "west", "up", "down")}},
    ]
    w(f"{A}/models/block/rift_altar_{s}.json", {"render_type": "minecraft:cutout", "ambientocclusion": False,
        "textures": {"sigil": NS + f"sigil_{s}", "altar": NS + "rift_altar", "altar_top": NS + "rift_altar_top",
                     "crystal": NS + f"rift_crystal_{s}", "particle": NS + "rift_altar"},
        "elements": elements})
variants = {}
for s in range(4):
    for op in ("false", "true"):
        variants[f"stage={s},open={op}"] = {"model": NS + f"rift_altar_{s}"}
w(f"{A}/blockstates/rift_altar.json", {"variants": variants})
w(f"{A}/models/item/rift_altar.json", {"parent": "minecraft:item/generated",
    "textures": {"layer0": "riftworld:item/rift_altar"}})

# ------------------------------------------------------------------ tags
pt = R + "/data/minecraft/tags/blocks/mineable/pickaxe.json"
cur = {"values": []}
if os.path.exists(pt):
    with open(pt, encoding="utf-8") as f:
        cur = json.load(f)
for v in ("riftworld:rift_stone", "riftworld:rift_stone_bricks", "riftworld:rift_stone_bricks_cracked",
          "riftworld:rift_stone_stairs", "riftworld:rift_stone_slab", "riftworld:rift_stone_wall",
          "riftworld:rift_pillar", "riftworld:rift_vein"):
    if v not in cur["values"]:
        cur["values"].append(v)
w(pt, cur)

# ------------------------------------------------------------------ lang
add = {
    "block.riftworld.rift_stone": ("Riftstone", "Разломный камень"),
    "block.riftworld.rift_stone_bricks": ("Riftstone Bricks", "Разломный кирпич"),
    "block.riftworld.rift_stone_bricks_cracked": ("Cracked Riftstone Bricks", "Треснувший разломный кирпич"),
    "block.riftworld.rift_stone_stairs": ("Riftstone Stairs", "Разломная лестница"),
    "block.riftworld.rift_stone_slab": ("Riftstone Slab", "Разломная плита"),
    "block.riftworld.rift_stone_wall": ("Riftstone Wall", "Разломная стена"),
    "block.riftworld.rift_pillar": ("Rift Pillar", "Разломная колонна"),
    "block.riftworld.rift_vein": ("Rift Vein", "Раскалённая жила"),
    "block.riftworld.rift_ash": ("Rift Ash", "Пепел разлома"),
    "block.riftworld.rift_candle": ("Candle of the Rift", "Свеча Разлома"),
    "block.riftworld.rift_altar": ("Altar of the Rift", "Алтарь Разлома"),
    "riftworld.rift.open": ("The rift is already open - step in", "Разлом уже открыт — войди в него"),
    "riftworld.rift.candles": ("Candles of the Rift: %s of %s lit", "Свечи Разлома: горят %s из %s"),
    "riftworld.rift.stand": ("All candles burn - stand on the sign of the hexagram", "Все свечи горят — встань на знак гексаграммы"),
    "riftworld.rift.nocandles": ("No candles of the Rift around the altar", "Вокруг алтаря нет свечей Разлома"),
    "riftworld.rift.enter": ("The rift pulls you in…", "Разлом втягивает тебя…"),
    "riftworld.rift.nowhere": ("There is nowhere to go: the way is not open yet", "Идти некуда: путь ещё не открыт"),
    "riftworld.rift.temple": ("The Great Temple of the Rift stands at %s, %s", "Великий Храм Разлома стоит на %s, %s"),
}
for lang, i in ((A + "/lang/en_us.json", 0), (A + "/lang/ru_ru.json", 1)):
    merge(lang, {k: v[i] for k, v in add.items()})

# the language keys of the deleted mosaic
for lang in (A + "/lang/en_us.json", A + "/lang/ru_ru.json"):
    if os.path.exists(lang):
        with open(lang, encoding="utf-8") as f:
            d = json.load(f)
        for k in [k for k in d if "temple_mosaic" in k or "mosaic_core" in k or "light_bridge" in k]:
            d.pop(k)
        w(lang, d)
print("RIFT_RES_OK")
