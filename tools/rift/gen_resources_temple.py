"""Rev 4 item 3: blockstates / models / lang / loot for the Great Temple blocks (run after the castle one)."""
import json, os

R = "/home/user/mods/riftworld/src/main/resources"
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

def pillar(n):
    w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/cube_column", "textures": {"side": NS + n, "end": NS + n + "_top"}})
    w(f"{A}/blockstates/{n}.json", {"variants": {"axis=x": {"model": NS + n, "x": 90, "y": 90},
        "axis=y": {"model": NS + n}, "axis=z": {"model": NS + n, "x": 90}}})
    w(f"{A}/models/item/{n}.json", {"parent": NS + n})
    drop(n)

for n in ("temple_bricks", "temple_gold", "temple_floor"):
    cube(n)
pillar("temple_pillar")

# stairs / slab
w(f"{A}/models/block/temple_bricks_stairs.json", {"parent": "minecraft:block/stairs", "textures": {"bottom": NS + "temple_bricks", "top": NS + "temple_bricks", "side": NS + "temple_bricks"}})
w(f"{A}/blockstates/temple_bricks_stairs.json", {"multipart": [
    {"when": {"facing": "east", "half": "bottom", "shape": "straight"}, "apply": {"model": NS + "temple_bricks_stairs"}},
    {"when": {"facing": "west", "half": "bottom", "shape": "straight"}, "apply": {"model": NS + "temple_bricks_stairs", "y": 180, "uvlock": True}},
    {"when": {"facing": "south", "half": "bottom", "shape": "straight"}, "apply": {"model": NS + "temple_bricks_stairs", "y": 90, "uvlock": True}},
    {"when": {"facing": "north", "half": "bottom", "shape": "straight"}, "apply": {"model": NS + "temple_bricks_stairs", "y": 270, "uvlock": True}}]})
w(f"{A}/models/item/temple_bricks_stairs.json", {"parent": NS + "temple_bricks_stairs"})
drop("temple_bricks_stairs")
w(f"{A}/models/block/temple_bricks_slab.json", {"parent": "minecraft:block/slab", "textures": {"bottom": NS + "temple_bricks", "top": NS + "temple_bricks", "side": NS + "temple_bricks"}})
w(f"{A}/models/block/temple_bricks_slab_top.json", {"parent": "minecraft:block/slab_top", "textures": {"bottom": NS + "temple_bricks", "top": NS + "temple_bricks", "side": NS + "temple_bricks"}})
w(f"{A}/blockstates/temple_bricks_slab.json", {"variants": {"type=bottom": {"model": NS + "temple_bricks_slab"},
    "type=top": {"model": NS + "temple_bricks_slab_top"}, "type=double": {"model": NS + "temple_bricks"}}})
w(f"{A}/models/item/temple_bricks_slab.json", {"parent": NS + "temple_bricks_slab"})
drop("temple_bricks_slab")

# mosaic ring + core (4 glow stages)
for name, tex in (("temple_mosaic", "temple_mosaic"), ("mosaic_core", "mosaic_core")):
    v = {}
    for s in range(4):
        w(f"{A}/models/block/{name}_{s}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": NS + f"{tex}_{s}"}})
        v[f"stage={s}"] = {"model": NS + f"{name}_{s}"}
    w(f"{A}/blockstates/{name}.json", {"variants": v})
    w(f"{A}/models/item/{name}.json", {"parent": NS + f"{name}_0"})
    drop(name)

# ritual candle: brass-footed ivory pillar; lit = with flame cross
base_el = {"from": [5, 2, 5], "to": [11, 15, 11], "faces": {f: {"texture": "#side", "uv": [4, 0, 10, 13]} for f in ("north", "east", "south", "west")} | {"up": {"texture": "#side", "uv": [4, 0, 10, 6]}, "down": {"texture": "#side", "uv": [4, 0, 10, 6]}}}
foot_el = {"from": [3, 0, 3], "to": [13, 2, 13], "faces": {f: {"texture": "#side", "uv": [0, 13, 16, 15]} for f in ("north", "east", "south", "west")} | {"up": {"texture": "#side", "uv": [0, 13, 16, 15]}, "down": {"texture": "#side", "uv": [0, 13, 16, 15]}}}
flame_el = lambda rot: {"from": [8, 15, 4], "to": [8, 16, 12], "rotation": {"origin": [8, 15, 8], "axis": "y", "angle": rot},
                        "faces": {"north": {"texture": "#flame", "uv": [0, 0, 16, 16]}, "south": {"texture": "#flame", "uv": [0, 0, 16, 16]}}}
flame_el2 = lambda rot: {"from": [4, 15, 8], "to": [12, 16, 8], "rotation": {"origin": [8, 15, 8], "axis": "y", "angle": rot},
                         "faces": {"east": {"texture": "#flame", "uv": [0, 0, 16, 16]}, "west": {"texture": "#flame", "uv": [0, 0, 16, 16]}}}
w(f"{A}/models/block/ritual_candle.json", {"textures": {"side": NS + "ritual_candle", "particle": NS + "ritual_candle"}, "elements": [foot_el, base_el]})
w(f"{A}/models/block/ritual_candle_lit.json", {"textures": {"side": NS + "ritual_candle", "flame": NS + "ritual_flame", "particle": NS + "ritual_candle"},
   "elements": [foot_el, base_el, flame_el(0), flame_el(90), flame_el2(0), flame_el2(90)]})
w(f"{A}/blockstates/ritual_candle.json", {"variants": {"lit=false": {"model": NS + "ritual_candle"}, "lit=true": {"model": NS + "ritual_candle_lit"}}})
w(f"{A}/models/item/ritual_candle.json", {"parent": NS + "ritual_candle"})
drop("ritual_candle")

# light bridge (translucent)
w(f"{A}/models/block/light_bridge.json", {"parent": "minecraft:block/cube_all", "textures": {"all": NS + "light_bridge"}, "render_type": "minecraft:translucent"})
w(f"{A}/blockstates/light_bridge.json", {"variants": {"": {"model": NS + "light_bridge"}}})
w(f"{A}/models/item/light_bridge.json", {"parent": NS + "light_bridge"})

T = R + "/data/minecraft/tags/"
pt = T + "blocks/mineable/pickaxe.json"
cur = {"values": []}
if os.path.exists(pt):
    with open(pt, encoding="utf-8") as f:
        cur = json.load(f)
for v in ["riftworld:temple_bricks", "riftworld:temple_bricks_stairs", "riftworld:temple_bricks_slab",
          "riftworld:temple_gold", "riftworld:temple_pillar", "riftworld:temple_floor", "riftworld:temple_mosaic", "riftworld:mosaic_core"]:
    if v not in cur["values"]:
        cur["values"].append(v)
w(pt, cur)

L_en, L_ru = A + "/lang/en_us.json", A + "/lang/ru_ru.json"
add = {
 "block.riftworld.temple_bricks": ("Temple Bricks", "Храмовый кирпич"),
 "block.riftworld.temple_bricks_stairs": ("Temple Brick Stairs", "Храмовая лестница"),
 "block.riftworld.temple_bricks_slab": ("Temple Brick Slab", "Храмовая плита"),
 "block.riftworld.temple_gold": ("Gilded Temple Block", "Золочёный храмовый блок"),
 "block.riftworld.temple_pillar": ("Temple Pillar", "Храмовая колонна"),
 "block.riftworld.temple_floor": ("Temple Floor", "Храмовый пол"),
 "block.riftworld.temple_mosaic": ("Ritual Mosaic", "Ритуальная мозаика"),
 "block.riftworld.mosaic_core": ("Sigil of the Crossing", "Сигил Перехода"),
 "block.riftworld.ritual_candle": ("Ritual Candle", "Ритуальная свеча"),
 "block.riftworld.light_bridge": ("Liquid Light", "Жидкий свет"),
}
for lang, i in ((L_en, 0), (L_ru, 1)):
    merge(lang, {k: v[i] for k, v in add.items()})
print("TEMPLE_RES_OK")
