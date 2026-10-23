#!/usr/bin/env python3
"""Рев 8 «Живое море»: ресурсы новых блоков (blockstates / models / items / loot / lang / tags).

Запускать ПОСЛЕ tools/rift/textures_sea.py и после gen_resources_elisar.py
(этот скрипт только добавляет, ничего не перезаписывает у чужих блоков).
"""
import json
import os

R = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))),
                 "mod/src/main/resources")
A = R + "/assets/riftworld"
D = R + "/data/riftworld"
NS = "riftworld:block/"

CUBES = ["elisar_silt", "reef_stone", "coral_brain"]
CROSSES = ["coral_branch", "coral_fan", "coral_tube", "coral_glow",
           "sea_kelp", "sea_kelp_top", "sea_grass", "cave_glow"]

RU = {
    "elisar_silt": "Морской ил", "reef_stone": "Рифовый камень", "coral_brain": "Мозговой коралл",
    "coral_branch": "Ветвистый коралл", "coral_fan": "Веерный коралл", "coral_tube": "Трубчатая губка",
    "coral_glow": "Светящийся коралл", "sea_kelp": "Ламинария", "sea_kelp_top": "Верхушка ламинарии",
    "sea_grass": "Морская трава", "cave_glow": "Пещерный нарост", "sea_shells": "Ракушки и морские звёзды",
}
EN = {
    "elisar_silt": "Sea Silt", "reef_stone": "Reef Stone", "coral_brain": "Brain Coral",
    "coral_branch": "Branch Coral", "coral_fan": "Fan Coral", "coral_tube": "Tube Sponge",
    "coral_glow": "Glowing Coral", "sea_kelp": "Kelp", "sea_kelp_top": "Kelp Tip",
    "sea_grass": "Seagrass", "cave_glow": "Cave Growth", "sea_shells": "Shells & Starfish",
}


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)


def merge_json(path, obj):
    old = json.load(open(path, encoding="utf-8")) if os.path.exists(path) else {}
    old.update(obj)
    w(path, old)


def item_model(n):
    w(f"{A}/models/item/{n}.json", {"parent": NS + n})


def gen_item(n, tex):
    w(f"{A}/models/item/{n}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": tex}})


def loot_self(n):
    w(f"{D}/loot_tables/blocks/{n}.json", {"type": "minecraft:block", "pools": [{
        "rolls": 1,
        "entries": [{"type": "minecraft:item", "name": "riftworld:" + n}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


for n in CUBES:
    w(f"{A}/blockstates/{n}.json", {"variants": {"": [{"model": NS + n, "y": r} for r in (0, 90, 180, 270)]}})
    w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": NS + n}})
    item_model(n)
    loot_self(n)

for n in CROSSES:
    w(f"{A}/models/block/{n}.json", {"parent": "minecraft:block/cross",
                                     "textures": {"cross": NS + n}, "render_type": "minecraft:cutout"})
    w(f"{A}/blockstates/{n}.json", {"variants": {"waterlogged=true": {"model": NS + n},
                                                 "waterlogged=false": {"model": NS + n}}})
    gen_item(n, NS + n)
    loot_self(n)

# ракушки: плоский декаль на дне (тонкая пластина, текстура сверху)
w(f"{A}/models/block/sea_shells.json", {
    "ambientocclusion": False,
    "textures": {"particle": NS + "sea_shells", "t": NS + "sea_shells"},
    "elements": [{
        "from": [0, 0, 0], "to": [16, 0.6, 16],
        "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#t"}},
    }],
    "render_type": "minecraft:cutout",
})
w(f"{A}/blockstates/sea_shells.json", {"variants": {"waterlogged=true": {"model": NS + "sea_shells"},
                                                   "waterlogged=false": {"model": NS + "sea_shells"}}})
gen_item("sea_shells", NS + "sea_shells")
loot_self("sea_shells")

# lang
for lang, names in (("en_us", EN), ("ru_ru", RU)):
    merge_json(f"{A}/lang/{lang}.json", {f"block.riftworld.{n}": v for n, v in names.items()})

print("ресурсы моря: %d блоков" % (len(CUBES) + len(CROSSES) + 1))

# ---------------------------------------------------------------- яйца призыва рыбы
EGGS = {
    "silver_fish_spawn_egg": (0xC6D3DC, 0x2B3440),
    "gold_fish_spawn_egg": (0xE9C25A, 0xC29A38),
    "jelly_fish_spawn_egg": (0x9FE6FF, 0xD8F7FF),
    "ray_fish_spawn_egg": (0x68798C, 0xDCE6EC),
    "angler_fish_spawn_egg": (0x2C3947, 0xDFFFA8),
}
EGG_RU = {
    "silver_fish_spawn_egg": "Яйцо серебристой рыбы", "gold_fish_spawn_egg": "Яйцо золотого диска",
    "jelly_fish_spawn_egg": "Яйцо медузы", "ray_fish_spawn_egg": "Яйцо манты",
    "angler_fish_spawn_egg": "Яйцо пещерного удильщика",
}
EGG_EN = {
    "silver_fish_spawn_egg": "Silver Fish Spawn Egg", "gold_fish_spawn_egg": "Gold Disc Spawn Egg",
    "jelly_fish_spawn_egg": "Jellyfish Spawn Egg", "ray_fish_spawn_egg": "Ray Spawn Egg",
    "angler_fish_spawn_egg": "Cave Anglerfish Spawn Egg",
}
for n in EGGS:
    gen_item(n, "riftworld:item/" + n)
for lang, names in (("en_us", EGG_EN), ("ru_ru", EGG_RU)):
    merge_json(f"{A}/lang/{lang}.json", {f"item.riftworld.{n}": v for n, v in names.items()})

merge_json(f"{A}/lang/en_us.json", {"entity.riftworld.sea_fish": "Elisar Sea Fish"})
merge_json(f"{A}/lang/ru_ru.json", {"entity.riftworld.sea_fish": "Морская рыба Элисара"})
