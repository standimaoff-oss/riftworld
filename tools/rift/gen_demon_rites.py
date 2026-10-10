"""Part 7 'Храм полностью': assets for the new temple blocks (textures are 16x16, in the temple palette).
-> assets/riftworld/textures/block/{temple_bricks_cracked,temple_bricks_ivy,temple_gold_rune,demon_frame,demon_gate}.png
-> blockstates + block/item models + ru/en lang + self-drop loot tables.
"""
import json, os, random

ROOT = "/home/user/mods/riftworld/src/main/resources"
A = ROOT + "/assets/riftworld"
D = ROOT + "/data/riftworld"

BLOCKS = [
    ("temple_bricks_cracked", "Храмовый кирпич с трещинами", "Cracked Temple Bricks"),
    ("temple_bricks_ivy", "Заросший плющом храмовый кирпич", "Ivy Temple Bricks"),
    ("temple_gold_rune", "Рунная позолота", "Gilded Rune Inlay"),
    ("demon_frame", "Демоническая каменная рама", "Demonstone Frame"),
    ("demon_gate", "Демонический телепорт", "Demonic Teleport"),
]


def brick_rows(draw, base, mortar, wobble=1):
    for y in range(16):
        for x in range(16):
            row = y // 4
            off = 8 if row % 2 else 0
            border = (y % 4 == 3) or ((x + off) % 8 == 7)
            n = random.randint(-wobble, wobble)
            c = mortar if border else (base[0] + n, base[1] + n, base[2] + n)
            draw(x, y, c)


def write_png(name, px):
    from PIL import Image
    im = Image.new("RGBA", (16, 16))
    for (x, y), c in px.items():
        im.putpixel((x, y), (*c, 255))
    im.save(A + f"/textures/block/{name}.png")
    print("  tex", name)


# ------------------------------------------------------------------ cracked ivory bricks
random.seed(77)
px = {}
brick_rows(lambda x, y, c: px.__setitem__((x, y), c), (217, 208, 190), (188, 178, 158), 6)
for crack in ([(3, 1), (4, 2), (4, 3), (5, 4), (5, 5), (6, 6), (5, 7), (6, 8), (7, 9), (7, 10), (8, 11)],
              [(12, 4), (11, 5), (11, 6), (12, 7), (11, 8), (10, 9), (11, 10), (10, 11), (10, 12), (9, 13)],
              [(1, 12), (2, 12), (3, 13), (4, 13), (5, 14)]):
    for (x, y) in crack:
        px[(x, y)] = (64, 58, 50)
        if random.random() < 0.4 and (x + 1, y) in px:
            px[(x + 1, y)] = (96, 88, 76)
write_png("temple_bricks_cracked", px)

# ------------------------------------------------------------------ ivy-clad bricks
random.seed(88)
px = {}
brick_rows(lambda x, y, c: px.__setitem__((x, y), c), (214, 206, 188), (186, 176, 156), 5)
leaf = [(86, 122, 52), (104, 142, 62), (72, 104, 44), (118, 156, 70)]
for _ in range(46):                                       # creeping leaf clusters, denser at the edges
    cx, cy = random.randint(0, 15), random.randint(0, 15)
    bias = random.random()
    if bias < 0.65 and not (cx < 3 or cx > 12 or cy < 3 or cy > 12):
        continue
    for (dx, dy) in ((0, 0), (1, 0), (0, 1), (1, 1), (-1, 0), (0, -1)):
        if random.random() < 0.66 and 0 <= cx + dx < 16 and 0 <= cy + dy < 16:
            px[(cx + dx, cy + dy)] = random.choice(leaf)
for _ in range(5):                                        # thin vine runners
    x, y = random.randint(0, 15), 0
    for yy in range(0, 16, 1):
        x = max(0, min(15, x + random.randint(-1, 1)))
        px[(x, yy)] = (60, 88, 38)
write_png("temple_bricks_ivy", px)

# ------------------------------------------------------------------ gilded rune inlay (pale floor + gold sigil)
random.seed(99)
px = {}
GOLD_A, GOLD_B = (237, 197, 86), (188, 152, 58)
for y in range(16):
    for x in range(16):
        n = random.randint(-4, 4)
        px[(x, y)] = (213 + n, 208 + n, 196 + n)
ring = [(3, 3), (8, 2), (12, 3), (13, 8), (12, 12), (8, 13), (3, 12), (2, 8)]
for (x, y) in ring:
    px[(x, y)] = GOLD_A
    px[(min(x + 1, 15), y)] = GOLD_B if random.random() < 0.5 else GOLD_A
px[(8, 7)] = px[(8, 8)] = px[(7, 8)] = px[(9, 8)] = GOLD_A     # the sigil core
for (x, y) in ring[::2]:
    px[(x, y)] = GOLD_A
write_png("temple_gold_rune", px)

# ------------------------------------------------------------------ demonstone frame (black brick, ember veins)
random.seed(66)
px = {}
brick_rows(lambda x, y, c: px.__setitem__((x, y), c), (38, 30, 40), (22, 17, 26), 4)
EMBER = [(196, 44, 18), (232, 84, 30), (255, 128, 60)]
for vein in ([(2, 1), (2, 2), (3, 2), (3, 3), (4, 3), (4, 4), (5, 5), (5, 6), (6, 6), (6, 7)],
             [(12, 1), (12, 2), (11, 2), (11, 3), (10, 4), (10, 5), (9, 5), (9, 6), (8, 7), (8, 8)],
             [(2, 12), (3, 12), (3, 13), (4, 13), (5, 14), (13, 13), (12, 13), (12, 14), (11, 14)]):
    for (x, y) in vein:
        px[(x, y)] = random.choice(EMBER)
write_png("demon_frame", px)

# ------------------------------------------------------------------ demon gate body (crimson churn)
random.seed(55)
px = {}
for y in range(16):
    for x in range(16):
        d = abs(x - 7.5) + abs(y - 7.5)
        if d < 3:
            c = (255, 110, 70)
        elif d < 6:
            c = (200, 48, 26)
        elif d < 9:
            c = (140, 22, 14)
        else:
            c = (70, 8, 8)
        n = random.randint(-14, 14)
        px[(x, y)] = (max(0, min(255, c[0] + n)), max(0, min(255, c[1] + n // 2)), max(0, min(255, c[2] + n // 3)))
write_png("demon_gate", px)

# ------------------------------------------------------------------ blockstates / models / loot / lang
for bid, _, _ in BLOCKS:
    json.dump({"variants": {"": {"model": f"riftworld:block/{bid}"}}}, open(A + f"/blockstates/{bid}.json", "w"), indent=2)
    json.dump({"parent": "minecraft:block/cube_all", "textures": {"all": f"riftworld:block/{bid}"}},
              open(A + f"/models/block/{bid}.json", "w"), indent=2)
    json.dump({"parent": f"riftworld:block/{bid}"}, open(A + f"/models/item/{bid}.json", "w"), indent=2)
    json.dump({"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
               "entries": [{"type": "minecraft:item", "name": f"riftworld:{bid}"}],
               "conditions": [{"condition": "minecraft:survives_explosion"}]}]},
              open(D + f"/loot_tables/blocks/{bid}.json", "w"), indent=2)

for lang, col in (("ru_ru", 1), ("en_us", 2)):
    p = A + f"/lang/{lang}.json"
    j = json.load(open(p, encoding="utf-8"))
    for bid, ru, en in BLOCKS:
        j[f"block.riftworld.{bid}"] = ru if col == 1 else en
    json.dump(j, open(p, "w", encoding="utf-8"), ensure_ascii=False, indent=2)

print("DEMON_RITES_OK", len(BLOCKS), "blocks")
