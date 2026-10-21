#!/usr/bin/env python3
"""Rev 7 part 2: textures + block resources for the new tree species.

Species (muted palettes, never bright):
  * lilac      - biome 04 Violet Hills: lavender-grey leaves, ash bark, soft lilac blossom accents
  * fern       - biome 07 Waterfall Gardens: deep damp-green fronds, pale fibre trunk
  * mist_pine  - Mist Keep: cold grey-green needles, dark bark
  * oak        - the ordinary world / city: calm green, warm bark

Writes textures (16x16), blockstates, block + item models, loot tables, tags and lang keys.
Run: python3 tools/rift/gen_tree_assets.py
"""
from __future__ import annotations

import json
import os
import random

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ASSETS = os.path.join(ROOT, "mod", "src", "main", "resources", "assets", "riftworld")
DATA = os.path.join(ROOT, "mod", "src", "main", "resources", "data", "riftworld")
MCB = os.path.join(ROOT, "mod", "src", "main", "resources", "data", "minecraft", "tags", "blocks")
MCI = os.path.join(ROOT, "mod", "src", "main", "resources", "data", "minecraft", "tags", "items")
TEX = os.path.join(ASSETS, "textures", "block")

SPECIES = {
    "lilac": dict(leaf=(134, 129, 156), leaf_light=(158, 153, 178), leaf_dark=(108, 102, 130),
                  bark=(104, 96, 98), bark_dark=(78, 72, 74), core=(162, 152, 142),
                  accent="lilac_blossom", accent_col=(178, 152, 198)),
    "fern": dict(leaf=(88, 112, 78), leaf_light=(108, 132, 92), leaf_dark=(64, 86, 58),
                 bark=(112, 96, 74), bark_dark=(84, 70, 54), core=(168, 152, 122),
                 accent=None, accent_col=None),
    "mist_pine": dict(leaf=(100, 114, 106), leaf_light=(122, 136, 126), leaf_dark=(76, 88, 82),
                      bark=(78, 72, 66), bark_dark=(58, 54, 50), core=(132, 124, 112),
                      accent=None, accent_col=None),
    "oak": dict(leaf=(94, 118, 72), leaf_light=(114, 138, 86), leaf_dark=(70, 92, 54),
                bark=(108, 86, 62), bark_dark=(82, 64, 46), core=(156, 128, 92),
                accent=None, accent_col=None),
}


def img_new():
    return [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]


def save(img, path):
    from PIL import Image
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im = Image.new("RGBA", (16, 16))
    im.putdata([px for row in img for px in row])
    im.save(path)


def leaves(spec, seed):
    rnd = random.Random(seed)
    img = img_new()
    for y in range(16):
        for x in range(16):
            r = rnd.random()
            if r < 0.10:
                c = spec["leaf_dark"]
            elif r < 0.22:
                c = spec["leaf_light"]
            else:
                c = spec["leaf"]
            k = 0.92 + 0.16 * rnd.random()
            # a few translucent holes so the canopy reads as foliage, not a solid box
            a = 255
            if rnd.random() < 0.06:
                a = 0
            img[y][x] = (int(c[0] * k), int(c[1] * k), int(c[2] * k), a)
    # leaf vein dots
    for _ in range(14):
        x, y = rnd.randrange(16), rnd.randrange(16)
        c = spec["leaf_dark"]
        img[y][x] = (c[0], c[1], c[2], 255)
    return img


def log_side(spec, seed):
    rnd = random.Random(seed)
    img = img_new()
    for y in range(16):
        for x in range(16):
            stripe = (x * 5 + (y // 3) * 3) % 7
            base = spec["bark_dark"] if stripe == 0 else spec["bark"]
            k = 0.88 + 0.22 * rnd.random()
            if rnd.random() < 0.05:
                k *= 0.75
            img[y][x] = (int(base[0] * k), int(base[1] * k), int(base[2] * k), 255)
    # a pale scar from a broken branch
    for i in range(3):
        y = 5 + i
        img[y][7] = (*spec["core"], 255)
        img[y][8] = (*spec["core"], 255)
    return img


def log_top(spec, seed):
    rnd = random.Random(seed)
    img = img_new()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d > 7.4:
                c, k = spec["bark_dark"], 0.9 + 0.2 * rnd.random()
            else:
                ring = int(d) % 2
                c = spec["core"] if ring == 0 else spec["bark"]
                k = 0.86 + 0.22 * rnd.random()
            img[y][x] = (int(c[0] * k), int(c[1] * k), int(c[2] * k), 255)
    return img


def blossom(seed, col):
    rnd = random.Random(seed)
    img = img_new()
    for y in range(16):
        for x in range(16):
            r = rnd.random()
            if r < 0.35:
                img[y][x] = (0, 0, 0, 0)
                continue
            k = 0.85 + 0.3 * rnd.random()
            c = col if r > 0.25 else (int(col[0] * 1.12), int(col[1] * 1.12), int(col[2] * 1.12))
            img[y][x] = (min(255, int(c[0] * k)), min(255, int(c[1] * k)), min(255, int(c[2] * k)), 255)
    return img


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def block_resources(name):
    mid = f"riftworld:{name}"
    # log
    write_json(os.path.join(ASSETS, "blockstates", name + "_log.json"), {"variants": {
        "axis=y": {"model": f"riftworld:block/{name}_log"},
        "axis=z": {"model": f"riftworld:block/{name}_log_horizontal", "x": 90},
        "axis=x": {"model": f"riftworld:block/{name}_log_horizontal", "x": 90, "y": 90}}})
    write_json(os.path.join(ASSETS, "models", "block", name + "_log.json"), {
        "parent": "minecraft:block/cube_column",
        "textures": {"end": mid + "_log_top", "side": mid + "_log"}})
    write_json(os.path.join(ASSETS, "models", "block", name + "_log_horizontal.json"), {
        "parent": "minecraft:block/cube_column_horizontal",
        "textures": {"end": mid + "_log_top", "side": mid + "_log"}})
    write_json(os.path.join(ASSETS, "models", "item", name + "_log.json"), {"parent": f"riftworld:block/{name}_log"})
    # leaves
    write_json(os.path.join(ASSETS, "blockstates", name + "_leaves.json"), {"variants": {
        "": {"model": f"riftworld:block/{name}_leaves"}}})
    write_json(os.path.join(ASSETS, "models", "block", name + "_leaves.json"), {
        "parent": "minecraft:block/leaves",
        "textures": {"all": mid + "_leaves"},
        "render_type": "minecraft:cutout_mipped"})
    write_json(os.path.join(ASSETS, "models", "item", name + "_leaves.json"), {"parent": f"riftworld:block/{name}_leaves"})
    # loot
    write_json(os.path.join(DATA, "loot_tables", "blocks", name + "_log.json"), {
        "type": "minecraft:block", "pools": [{"rolls": 1,
            "entries": [{"type": "minecraft:item", "name": f"riftworld:{name}_log"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write_json(os.path.join(DATA, "loot_tables", "blocks", name + "_leaves.json"), {
        "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item",
            "name": f"riftworld:{name}_leaves"}], "conditions": [{"condition": "minecraft:match_tool",
            "predicate": {"items": ["minecraft:shears"]}}]}]})


def main():
    lang_ru = {"lilac": "Лиловый ясень", "fern": "Папоротниковое дерево", "mist_pine": "Туманная сосна", "oak": "Элисарский дуб"}
    lang_en = {"lilac": "Lilac Ash", "fern": "Fern Tree", "mist_pine": "Mist Pine", "oak": "Elisar Oak"}
    ru = json.load(open(os.path.join(ASSETS, "lang", "ru_ru.json"), encoding="utf-8"))
    en = json.load(open(os.path.join(ASSETS, "lang", "en_us.json"), encoding="utf-8"))
    for i, (name, spec) in enumerate(SPECIES.items()):
        save(leaves(spec, 100 + i), os.path.join(TEX, name + "_leaves.png"))
        save(log_side(spec, 200 + i), os.path.join(TEX, name + "_log.png"))
        save(log_top(spec, 300 + i), os.path.join(TEX, name + "_log_top.png"))
        block_resources(name)
        if spec.get("accent"):
            acc = spec["accent"]
            save(blossom(400 + i, spec["accent_col"]), os.path.join(TEX, acc + ".png"))
            write_json(os.path.join(ASSETS, "blockstates", acc + ".json"), {"variants": {
                "": {"model": f"riftworld:block/{acc}"}}})
            write_json(os.path.join(ASSETS, "models", "block", acc + ".json"), {
                "parent": "minecraft:block/cube_all", "textures": {"all": f"riftworld:block/{acc}"},
                "render_type": "minecraft:cutout"})
            write_json(os.path.join(ASSETS, "models", "item", acc + ".json"), {"parent": f"riftworld:block/{acc}"})
            write_json(os.path.join(DATA, "loot_tables", "blocks", acc + ".json"), {
                "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item",
                    "name": f"riftworld:{acc}"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
            ru[f"block.riftworld.{acc}"] = "Лиловый цвет"
            en[f"block.riftworld.{acc}"] = "Lilac Blossom"
        ru[f"block.riftworld.{name}_log"] = f"Бревно: {lang_ru[name].lower()}"
        en[f"block.riftworld.{name}_log"] = f"{lang_en[name]} Log"
        ru[f"block.riftworld.{name}_leaves"] = f"Листва: {lang_ru[name].lower()}"
        en[f"block.riftworld.{name}_leaves"] = f"{lang_en[name]} Leaves"
    # tags: logs / leaves / mineable
    for path in (os.path.join(MCB, "logs.json"), os.path.join(MCI, "logs.json")):
        d = json.load(open(path, encoding="utf-8"))
        for name in SPECIES:
            v = f"riftworld:{name}_log"
            if v not in d["values"]:
                d["values"].append(v)
        write_json(path, d)
    lp = os.path.join(MCB, "leaves.json")
    d = json.load(open(lp, encoding="utf-8")) if os.path.exists(lp) else {"replace": False, "values": []}
    for name in SPECIES:
        v = f"riftworld:{name}_leaves"
        if v not in d["values"]:
            d["values"].append(v)
    write_json(lp, d)
    for tag, names in (("axe", [f"{n}_log" for n in SPECIES]), ("hoe", [f"{n}_leaves" for n in SPECIES])):
        p = os.path.join(MCB, "mineable", tag + ".json")
        d = json.load(open(p, encoding="utf-8"))
        for n in names:
            v = f"riftworld:{n}"
            if v not in d["values"]:
                d["values"].append(v)
        write_json(p, d)
    write_json(os.path.join(ASSETS, "lang", "ru_ru.json"), ru)
    write_json(os.path.join(ASSETS, "lang", "en_us.json"), en)
    print("tree assets written:", ", ".join(SPECIES))


if __name__ == "__main__":
    main()
