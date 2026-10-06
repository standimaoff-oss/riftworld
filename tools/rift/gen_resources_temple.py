"""Rev 4 item 3: blockstates / models / lang / loot for the Great Temple blocks (run after the castle one)."""
import json, os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
MOD = os.environ.get("RIFT_MOD_DIR", os.path.join(ROOT, "mod"))
R = os.path.join(MOD, "src/main/resources")
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

for n in ("temple_bricks", "temple_gold", "temple_floor", "temple_understone", "temple_maze_floor", "temple_garden_soil"):
    cube(n)
pillar("temple_pillar")

# The damp undercroft uses its own soil, moss, cross-plants and luminous crystal (no vanilla flora blocks).
def carpet(n, texture=None):
    w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/carpet", "textures": {"wool": NS + (texture or n)}})
    w(f"{A}/blockstates/{n}.json", {"variants": {"": {"model": NS + n}}})
    w(f"{A}/models/item/{n}.json", {"parent": NS + n})
    drop(n)

def cross(n, texture=None):
    w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/cross", "textures": {"cross": NS + (texture or n)}, "render_type": "minecraft:cutout"})
    w(f"{A}/blockstates/{n}.json", {"variants": {"": {"model": NS + n}}})
    w(f"{A}/models/item/{n}.json", {"parent": NS + n})
    drop(n)

carpet("temple_moss")
for n in ("temple_fern", "temple_bloom", "temple_root_vine"):
    cross(n)
w(f"{A}/models/block/temple_lumen_crystal.json", {"parent": "minecraft:block/cube_all", "textures": {"all": NS + "temple_lumen_crystal"}, "render_type": "minecraft:translucent"})
w(f"{A}/blockstates/temple_lumen_crystal.json", {"variants": {"": {"model": NS + "temple_lumen_crystal"}}})
w(f"{A}/models/item/temple_lumen_crystal.json", {"parent": NS + "temple_lumen_crystal"})
drop("temple_lumen_crystal")

# Three visibly distinct, reusable rune traps; their armed state has a brighter inset.
trap_variants = {}
for kind, name in enumerate(("spike", "ember", "drain")):
    for armed in (False, True):
        suffix = "armed" if armed else "spent"
        model = f"temple_trap_{name}_{suffix}"
        w(f"{A}/models/block/{model}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": NS + model}})
        trap_variants[f"kind={kind},armed={str(armed).lower()}"] = {"model": NS + model}
w(f"{A}/blockstates/temple_trap_plate.json", {"variants": trap_variants})
w(f"{A}/models/item/temple_trap_plate.json", {"parent": "minecraft:block/cube_all", "textures": {"all": NS + "temple_trap_spike_armed"}})
drop("temple_trap_plate")


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
          "riftworld:temple_gold", "riftworld:temple_pillar", "riftworld:temple_floor", "riftworld:temple_understone",
          "riftworld:temple_maze_floor", "riftworld:temple_lumen_crystal", "riftworld:temple_trap_plate",
          "riftworld:temple_mosaic", "riftworld:mosaic_core"]:
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
 "block.riftworld.temple_understone": ("Vesper Understone", "Сумрачный храмовый камень"),
 "block.riftworld.temple_maze_floor": ("Runed Crypt Tile", "Рунная плита крипты"),
 "block.riftworld.temple_garden_soil": ("Sanctum Soil", "Почва святилища"),
 "block.riftworld.temple_moss": ("Temple Moss", "Храмовый мох"),
 "block.riftworld.temple_fern": ("Vesper Fern", "Сумрачный папоротник"),
 "block.riftworld.temple_bloom": ("Starbloom", "Звездоцвет"),
 "block.riftworld.temple_root_vine": ("Root Veil", "Корневая завеса"),
 "block.riftworld.temple_lumen_crystal": ("Lumen Crystal", "Люмен-кристалл"),
 "block.riftworld.temple_trap_plate": ("Runic Trap Plate", "Рунная ловушка"),
 "block.riftworld.temple_mosaic": ("Ritual Mosaic", "Ритуальная мозаика"),
 "block.riftworld.mosaic_core": ("Sigil of the Crossing", "Сигил Перехода"),
 "block.riftworld.ritual_candle": ("Ritual Candle", "Ритуальная свеча"),
 "block.riftworld.light_bridge": ("Liquid Light", "Жидкий свет"),
}
for lang, i in ((L_en, 0), (L_ru, 1)):
    merge(lang, {k: v[i] for k, v in add.items()})

# The Reliquary Token is a mod item and a repeatable reward throughout the maze.
w(f"{A}/models/item/temple_relic.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "riftworld:item/temple_relic"}})
merge(L_en, {"item.riftworld.temple_relic": "Reliquary Token"})
merge(L_ru, {"item.riftworld.temple_relic": "Токен реликвария"})

# Soil is shovel-mineable; the remaining structural set is in the pickaxe tag above.
sp = T + "blocks/mineable/shovel.json"
shovels = {"values": []}
if os.path.exists(sp):
    with open(sp, encoding="utf-8") as f:
        shovels = json.load(f)
shovels.setdefault("values", [])
if "riftworld:temple_garden_soil" not in shovels["values"]:
    shovels["values"].append("riftworld:temple_garden_soil")
w(sp, shovels)

# Curated treasure pools: ordinary supplies in the entry, rare mod rewards in the final vault.
def loot_entry(name, weight, count=1, count_max=None, enchanted=False):
    entry = {"type": "minecraft:item", "name": name, "weight": weight}
    functions = []
    if count_max is not None:
        functions.append({"function": "minecraft:set_count", "count": {"min": count, "max": count_max}})
    if enchanted:
        functions.append({"function": "minecraft:enchant_randomly"})
    if functions:
        entry["functions"] = functions
    return entry

def write_chest(name, rolls_min, rolls_max, entries):
    w(f"{D}/loot_tables/chests/{name}.json", {"type": "minecraft:chest", "pools": [{
        "rolls": {"min": rolls_min, "max": rolls_max}, "entries": entries}]})

write_chest("temple_surface", 3, 6, [
    loot_entry("minecraft:gold_nugget", 12, 2, 9), loot_entry("minecraft:iron_ingot", 8, 1, 4),
    loot_entry("minecraft:amethyst_shard", 7, 1, 4), loot_entry("minecraft:map", 3),
    loot_entry("minecraft:golden_apple", 2), loot_entry("riftworld:temple_relic", 4),
    loot_entry("minecraft:enchanted_book", 2, enchanted=True)])
write_chest("temple_maze_cache", 3, 6, [
    loot_entry("minecraft:torch", 10, 2, 8), loot_entry("minecraft:iron_ingot", 8, 1, 4),
    loot_entry("minecraft:emerald", 5, 1, 3), loot_entry("minecraft:gold_ingot", 6, 1, 3),
    loot_entry("minecraft:ender_pearl", 3, 1, 2), loot_entry("riftworld:resonance_stone", 3),
    loot_entry("riftworld:temple_relic", 5), loot_entry("riftworld:guard_sickle", 1)])
write_chest("temple_garden_cache", 3, 5, [
    loot_entry("minecraft:glow_berries", 10, 2, 8), loot_entry("minecraft:honey_bottle", 6),
    loot_entry("minecraft:golden_carrot", 5, 1, 3), loot_entry("minecraft:amethyst_shard", 8, 1, 5),
    loot_entry("riftworld:temple_relic", 4), loot_entry("riftworld:resonance_stone", 2)])
write_chest("temple_final_vault", 5, 8, [
    loot_entry("minecraft:diamond", 6, 1, 3), loot_entry("minecraft:gold_ingot", 12, 2, 8),
    loot_entry("minecraft:enchanted_book", 5, enchanted=True), loot_entry("minecraft:golden_apple", 4),
    loot_entry("minecraft:netherite_scrap", 2), loot_entry("riftworld:temple_relic", 10, 1, 2),
    loot_entry("riftworld:resonance_stone", 5), loot_entry("riftworld:guard_sickle", 2)])
print("TEMPLE_RES_OK")
