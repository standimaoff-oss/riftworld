#!/usr/bin/env python3
"""Rev 7 part 2: builds the Dynamic Trees add-on (data-only tree pack + seed sprites).

Dynamic Trees 1.20.1 loads tree packs from the ``trees/<pack>/`` folder of any mod jar
(see com.ferreusveritas.dynamictrees.resources.ModTreeResourcePack). A pack is pure JSON, so this add-on
compiles against nothing and cannot break when DT is absent: every family is guarded with "only_if_loaded".

Output:
  dtcompat/src/main/resources/trees/riftworld_dt/{families,species,leaves_properties,world_gen}/*.json
  dtcompat/src/main/resources/assets/riftworld_dt/{models/item,textures/item}/*_seed.png|json
  dtcompat/src/main/resources/assets/riftworld_dt/lang/{ru_ru,en_us}.json
  dtcompat/src/main/resources/{META-INF/mods.toml,pack.mcmeta}

Run: python3 tools/rift/gen_dt_addon.py
"""
from __future__ import annotations

import json
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
DT = os.path.join(ROOT, "dtcompat", "src", "main", "resources")
PACK = "riftworld_dt"

# name -> (log, leaves, cell kit, growth logic, tapering, signal energy, growth rate, lowest branch, max radius,
#          perfect biomes, russian name, english name, seed palette)
SPECIES = {
    "lilac": dict(log="lilac_log", leaves="lilac_leaves", kit="deciduous", logic=None,
                  tapering=0.34, energy=16.0, rate=0.72, branch=5, radius=7,
                  biomes=["riftworld:violet_hills"], ru="Сиреневое древо", en="Lilac Tree",
                  seed=((122, 112, 158), (168, 158, 196), (72, 66, 96))),
    "sizo_v2": dict(log="sizo_log", leaves="sizo_leaves", kit="deciduous", logic="dark_oak",
                    tapering=0.28, energy=20.0, rate=0.62, branch=6, radius=10,
                    biomes=["riftworld:sizo_grove"], ru="Сизо (второе поколение)", en="Sizo v2",
                    seed=((104, 116, 118), (150, 162, 162), (62, 70, 72))),
    "fern_tree": dict(log="fern_log", leaves="fern_leaves", kit="palm", logic="palm",
                      tapering=0.55, energy=8.0, rate=1.15, branch=3, radius=5,
                      biomes=["riftworld:waterfall_gardens"], ru="Древовидный папоротник", en="Fern Tree",
                      seed=((78, 100, 70), (118, 140, 104), (48, 66, 44))),
    "mist_pine": dict(log="mist_pine_log", leaves="mist_pine_leaves", kit="conifer", logic="conifer",
                      tapering=0.42, energy=13.0, rate=0.66, branch=4, radius=6,
                      biomes=["riftworld:mist_keep"], ru="Туманная сосна", en="Mist Pine",
                      seed=((92, 106, 100), (140, 154, 148), (56, 66, 64))),
}


def write(path: str, data) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        if isinstance(data, str):
            f.write(data)
        else:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write("\n")


def family_tree(name: str, s: dict) -> None:
    family = {
        "common_leaves": f"{PACK}:{name}",
        "common_species": f"{PACK}:{name}",
        "primitive_log": f"riftworld:{s['log']}",
        "max_branch_radius": s["radius"],
        # the bark and the rings come straight from the mod's own log textures
        "texture_overrides": {
            "branch": f"riftworld:block/{s['log']}",
            "branch_top": f"riftworld:block/{s['log']}_top",
        },
        # if Dynamic Trees is not installed the whole pack simply does nothing
        "only_if_loaded": "dynamictrees",
    }
    write(os.path.join(DT, "trees", PACK, "families", name + ".json"), family)

    leaves = {
        "primitive_leaves": f"riftworld:{s['leaves']}",
        "cell_kit": f"dynamictrees:{s['kit']}",
        "smother": 3 if s["kit"] != "conifer" else 4,
        "only_if_loaded": "dynamictrees",
    }
    write(os.path.join(DT, "trees", PACK, "leaves_properties", name + ".json"), leaves)

    species = {
        "family": f"{PACK}:{name}",
        "tapering": s["tapering"],
        "signal_energy": s["energy"],
        "growth_rate": s["rate"],
        "lowest_branch_height": s["branch"],
        "max_branch_radius": s["radius"],
        "leaves_properties": f"{PACK}:{name}",
        # a regex over biome ids (DT: NameRegexMatchHolderSet), one pattern per species
        "perfect_biomes": {"name": s["biomes"][0]},
        "generate_seed": True,
        "up_probability": 3 if s["kit"] == "conifer" else 2,
        # the mod's own ground blocks have to be accepted, otherwise nothing can ever grow
        "acceptable_growth_blocks": ["riftworld:elisar_grass", "riftworld:elisar_soil", "minecraft:grass_block"],
        "acceptable_soils": ["dirt_like"],
        "world_gen_acceptable_soils": ["dirt_like"],
        "seed_name": name + "_seed",
        "texture_overrides": {"seed": f"{PACK}:item/{name}_seed"},
        "lang_overrides": {"seed": f"{s['en']} Seed"},
        "only_if_loaded": "dynamictrees",
    }
    if s["logic"]:
        species["growth_logic_kit"] = s["logic"]
    if s["kit"] == "conifer":
        species["environment_factors"] = {"#forge:is_cold": 1.1, "#forge:is_hot": 0.8}
    write(os.path.join(DT, "trees", PACK, "species", name + ".json"), species)


def soils() -> None:
    """The mod builds its own ground (riftworld:elisar_grass / elisar_soil): teach DT to root in it."""
    for block, name in (("elisar_grass", "elisar_grass"), ("elisar_soil", "elisar_soil"),
                        ("elisar_moss", "elisar_moss")):
        write(os.path.join(DT, "trees", PACK, "soil_properties", name + ".json"), {
            "primitive_soil": f"riftworld:{block}",
            "acceptable_soils": ["dirt_like"],
            "only_if_loaded": "dynamictrees",
        })


def world_gen() -> None:
    entries = []
    for name, s in SPECIES.items():
        for biome in s["biomes"]:
            entries.append({
                "select": biome,
                "apply": {"species": f"{PACK}:{name}", "density": [1.0], "chance": 1.0},
            })
    # the burnt lands keep the mod's own charred dead trees: DT adds nothing there
    entries.append({
        "_comment": "Ashen Lands stay bare - the mod renders its own charred dead trees there.",
        "select": "riftworld:ashen_lands",
        "apply": {"species": "null", "density": [1.0], "chance": 1.0},
    })
    entries.append({
        "_comment": "The city island is hand-built: no dynamic trees inside it.",
        "select": "riftworld:elisar_city",
        "apply": {"species": "null", "density": [1.0], "chance": 1.0},
    })
    write(os.path.join(DT, "trees", PACK, "world_gen", "riftworld.json"), entries)


def seed_sprite(name: str, palette) -> None:
    """A small 16x16 seed/nut sprite in the pack palette (muted, like the rest of Rev 7)."""
    dark, light, mid = palette
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rng = random.Random(hash(name) & 0xFFFF)
    body = [(7, 3), (8, 3), (6, 4), (7, 4), (8, 4), (9, 4),
            (5, 5), (6, 5), (7, 5), (8, 5), (9, 5), (10, 5),
            (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6),
            (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7),
            (6, 8), (7, 8), (8, 8), (9, 8),
            (6, 9), (7, 9), (8, 9), (9, 9),
            (7, 10), (8, 10)]
    for (x, y) in body:
        t = (y - 3) / 8.0
        base = tuple(int(light[i] + (dark[i] - light[i]) * t) for i in range(3))
        if rng.random() < 0.22:
            base = mid
        d.point((x, y), base + (255,))
    for (x, y) in ((7, 2), (8, 2), (8, 1)):
        d.point((x, y), mid + (255,))
    path = os.path.join(DT, "assets", PACK, "textures", "item", name + "_seed.png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def seed_model(name: str) -> None:
    write(os.path.join(DT, "assets", PACK, "models", "item", name + "_seed.json"), {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"{PACK}:item/{name}_seed"},
    })


def lang() -> None:
    ru, en = {}, {}
    for name, s in SPECIES.items():
        ru[f"item.{PACK}.{name}_seed"] = f"Семя: {s['ru']}"
        en[f"item.{PACK}.{name}_seed"] = f"{s['en']} Seed"
    write(os.path.join(DT, "assets", PACK, "lang", "ru_ru.json"), ru)
    write(os.path.join(DT, "assets", PACK, "lang", "en_us.json"), en)


MODS_TOML = '''modLoader = "javafml"
loaderVersion = "[47,)"
license = "All Rights Reserved"
issueTrackerURL = "https://github.com/standimaoff-oss/riftworld/issues"

[[mods]]
modId = "riftworld_dt"
version = "1.0"
displayName = "Разлом: Dynamic Trees"
description = """Dynamic Trees add-on for Rift (Разлом): four species (lilac, sizo v2, fern tree, mist pine)."""

[[dependencies.riftworld_dt]]
    modId = "forge"
    type = "required"
    versionRange = "[47,)"
    ordering = "NONE"
    side = "BOTH"

[[dependencies.riftworld_dt]]
    modId = "riftworld"
    type = "required"
    versionRange = "[1.0,)"
    ordering = "AFTER"
    side = "BOTH"

[[dependencies.riftworld_dt]]
    modId = "dynamictrees"
    type = "optional"
    versionRange = "[1.3,)"
    ordering = "AFTER"
    side = "BOTH"
'''

PACK_MC_META = '''{
  "pack": {
    "description": "Rift x Dynamic Trees",
    "pack_format": 15
  }
}
'''

README = '''# riftworld_dt — дополнение к Dynamic Trees

Data-only мод: ни одного класса, только дерево-пак Dynamic Trees (`trees/riftworld_dt/`) и ресурсы семян.
Если Dynamic Trees не установлен, пак ничего не делает (`only_if_loaded`), а мод просто грузится пустым.

## Породы

| id | порода | биом | cell kit | growth logic |
|----|--------|------|----------|--------------|
| `riftworld_dt:lilac` | сиреневое древо | `riftworld:violet_hills` | deciduous | по умолчанию |
| `riftworld_dt:sizo_v2` | сизо второго поколения (толстое) | `riftworld:sizo_grove` | deciduous | dark_oak |
| `riftworld_dt:fern_tree` | древовидный папоротник | `riftworld:waterfall_gardens` | palm | palm |
| `riftworld_dt:mist_pine` | туманная сосна | `riftworld:mist_keep` | conifer | conifer |

Породы босса/приспешников и «Адские угодья» не затрагиваются: в `ashen_lands` и `elisar_city` прописано
`species: null`, там растут только собственные обгоревшие деревья мода.

## Сборка

Пак генерируется скриптом: `python3 tools/rift/gen_dt_addon.py`.
Дальше это обычный Forge-проект: `cd dtcompat && ./gradlew build` (нужен интернет для Forge/Mojang).
Файл `libs/DynamicTrees-1.20.1-*.jar` для компиляции **не нужен** — в моде нет кода, обращающегося к API
Dynamic Trees, только ресурсы, поэтому несовпадение версий API пак не ломает.
'''


def main() -> None:
    for name, s in SPECIES.items():
        family_tree(name, s)
        seed_model(name)
        seed_sprite(name, s["seed"])
    soils()
    world_gen()
    lang()
    write(os.path.join(DT, "META-INF", "mods.toml"), MODS_TOML)
    write(os.path.join(DT, "pack.mcmeta"), PACK_MC_META)
    write(os.path.join(ROOT, "dtcompat", "README.md"), README)
    n = 0
    for _, _, files in os.walk(DT):
        n += len(files)
    print(f"riftworld_dt: {len(SPECIES)} пород, {n} файлов в dtcompat/src/main/resources")


if __name__ == "__main__":
    main()
