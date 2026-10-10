"""Biome 04: assets for the three violet bloom blocks (textures + blockstates + models + loot + lang patches).
Run once; deterministic. Usage: python3 tools/rift/gen_violet_flowers.py
"""
import json, os
from PIL import Image

A = "/home/user/mods/riftworld/src/main/resources/assets/riftworld"
D = "/home/user/mods/riftworld/src/main/resources/data/riftworld"

STEM = (74, 122, 74, 255)
STEM_D = (52, 92, 54, 255)

# (name, petal, petal_hi, core, style)
FLOWERS = [
    ("violet_bloom", (124, 68, 196, 255), (158, 106, 224, 255), (240, 214, 120, 255), "bells"),   # глубокий фиолет
    ("lilac_bloom",  (168, 124, 228, 255), (196, 160, 240, 255), (248, 226, 140, 255), "cluster"), # светлая сирень
    ("pale_bloom",   (224, 208, 248, 255), (246, 240, 255, 255), (250, 232, 150, 255), "stars"),   # почти белая лаванда
]

def img(name, petal, hi, core, style):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    def put(x, y, c):
        if 0 <= x < 16 and 0 <= y < 16: px[x, y] = c
    # stems (two stalks)
    for y in range(15, 4, -1):
        put(7, y, STEM if y % 2 else STEM_D)
        put(10, y, STEM if y % 2 else STEM_D)
        if style == "cluster": put(12, y, STEM_D if y % 2 else STEM)
    put(6, 12, STEM_D); put(6, 11, STEM)   # side leaf
    put(11, 10, STEM_D); put(12, 10, STEM)
    if style == "bells":                    # две колоколо-головки
        for (cx, cy) in ((7, 3), (10, 5)):
            put(cx, cy, hi); put(cx - 1, cy + 1, petal); put(cx, cy + 1, core); put(cx + 1, cy + 1, petal)
            put(cx - 1, cy + 2, petal); put(cx + 1, cy + 2, petal); put(cx, cy + 3, petal)
    elif style == "cluster":                # лиловый шарик-соцветие
        for (cx, cy) in ((7, 3), (10, 4), (12, 3)):
            for dx, dy in ((0, 0), (-1, 0), (1, 0), (0, -1), (0, 1)):
                put(cx + dx, cy + dy, petal if (dx + dy) % 2 else hi)
            put(cx, cy, core)
    else:                                   # pale: маленькие звёздочки
        for (cx, cy) in ((7, 3), (10, 5), (7, 6), (12, 2)):
            put(cx, cy, hi); put(cx - 1, cy, petal); put(cx + 1, cy, petal); put(cx, cy - 1, petal); put(cx, cy + 1, petal)
            put(cx, cy, core)
    im.save(f"{A}/textures/block/{name}.png")

for n, p1, p2, c, st in FLOWERS:
    img(n, p1, p2, c, st)
    json.dump({"variants": {"": {"model": f"riftworld:block/{n}"}}}, open(f"{A}/blockstates/{n}.json", "w"), indent=2)
    json.dump({"parent": "minecraft:block/cross", "textures": {"cross": f"riftworld:block/{n}"},
               "render_type": "minecraft:cutout"}, open(f"{A}/models/block/{n}.json", "w"), indent=2)
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": f"riftworld:block/{n}"}},
              open(f"{A}/models/item/{n}.json", "w"), indent=2)
    loot = {"type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"riftworld:{n}"}],
                       "conditions": [{"condition": "minecraft:match_tool", "predicate": {"items": ["minecraft:shears"]}}]}]}
    json.dump(loot, open(f"{D}/loot_tables/blocks/{n}.json", "w"), indent=2)

# lang (append in place, keep everything else)
RU = {"violet_bloom": "Фиалколокольник", "lilac_bloom": "Сиренец", "pale_bloom": "Бледная лаванда"}
EN = {"violet_bloom": "Violet Bells", "lilac_bloom": "Lilac Tuft", "pale_bloom": "Pale Lavender"}
for path, names in ((f"{A}/lang/ru_ru.json", RU), (f"{A}/lang/en_us.json", EN)):
    d = json.load(open(path))
    for k, v in names.items():
        d[f"block.riftworld.{k}"] = v
    json.dump(d, open(path, "w"), ensure_ascii=False, indent=2)
print("VIOLET_FLOWERS_OK", len(FLOWERS) * 5, "files")
