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

Run:
  python3 tools/rift/gen_dt_addon.py           # rebuild the pack
  python3 tools/rift/gen_dt_addon.py --check   # only verify the pack matches the generator (exit 1 on drift)
"""
from __future__ import annotations

import json
import os
import random
import sys
import zlib

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
DT = os.path.join(ROOT, "dtcompat", "src", "main", "resources")
PACK = "riftworld_dt"

# name -> (log, leaves, cell kit, growth logic, tapering, signal energy, growth rate, lowest branch, max radius,
#          perfect biomes, russian name, english name, seed palette)
# Shape knobs are Dynamic Trees' own (see tools/rift/dt_schema.json):
#   signal_energy  -> overall size/height of the grown tree
#   up_probability -> how often a branch grows up instead of out: LOW = many branches, HIGH = tall bare trunk
#   lowest_branch_height -> trunk height before the first branch
#   tapering       -> how fast branches thin out;  max_branch_radius -> thickness of the main branches
#   cell_kit       -> shape of the leaf clusters (deciduous / conifer / palm / dark_oak / acacia / bare)
#
# Two values sit outside the band Dynamic Trees uses in its own pack, on purpose:
#   fern_tree tapering 0.55 and mist_pine tapering 0.42 (DT: 0.0..0.40). Both species must look thin and gnarled
#   rather than sturdy; a higher tapering makes the branches lose radius faster. Everything else stays inside DT's
#   own ranges, so a strange-looking tree is always explainable by a deliberate value and never by a typo.
SPECIES = {
    "lilac": dict(log="lilac_log", leaves="lilac_leaves", kit="deciduous", logic=None,
                  tapering=0.34, energy=16.0, rate=0.72, branch=5, radius=7, up=3,
                  smother=3, flamm=30, fire=20, leaf_map=8, longevity=10,
                  biomes=["riftworld:violet_hills"], ru="Сиреневое древо", en="Lilac Tree",
                  seed=((122, 112, 158), (168, 158, 196), (72, 66, 96))),
    "sizo_v2": dict(log="sizo_log", leaves="sizo_leaves", kit="dark_oak", logic="dark_oak",
                    tapering=0.26, energy=24.0, rate=0.60, branch=9, radius=12, up=5,
                    smother=4, flamm=30, fire=20, leaf_map=10, longevity=14,
                    biomes=["riftworld:sizo_grove"], ru="Сизо (второе поколение)", en="Sizo v2",
                    seed=((104, 116, 118), (150, 162, 162), (62, 70, 72))),
    "fern_tree": dict(log="fern_log", leaves="fern_leaves", kit="palm", logic="palm",
                      tapering=0.55, energy=7.0, rate=1.15, branch=2, radius=5, up=1,
                      smother=2, flamm=40, fire=30, leaf_map=6, longevity=8,
                      biomes=["riftworld:waterfall_gardens"], ru="Древовидный папоротник", en="Fern Tree",
                      seed=((78, 100, 70), (118, 140, 104), (48, 66, 44))),
    "mist_pine": dict(log="mist_pine_log", leaves="mist_pine_leaves", kit="conifer", logic="conifer",
                      tapering=0.42, energy=14.0, rate=0.66, branch=3, radius=6, up=4,
                      smother=4, flamm=20, fire=15, leaf_map=9, longevity=12,
                      biomes=["riftworld:mist_keep"], ru="Туманная сосна", en="Mist Pine",
                      seed=((92, 106, 100), (140, 154, 148), (56, 66, 64))),
}


# --check: compare what the generator WOULD write with what is on disk, without touching anything. A generated pack
# that silently drifted from its generator is a real hazard: the JSON on disk is what ships, the generator is what
# everyone believes ships.
CHECK = "--check" in sys.argv
DRIFT: list[str] = []


def _render(data) -> str:
    if isinstance(data, (dict, list)):
        return json.dumps(data, ensure_ascii=False, indent=2) + "\n"
    return data


def write(path: str, data) -> None:
    text = _render(data)
    if CHECK:
        try:
            current = open(path, encoding="utf-8").read()
        except OSError:
            current = None
        if current != text:
            DRIFT.append(os.path.relpath(path, ROOT))
        return
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


def family_tree(name: str, s: dict) -> None:
    family = {
        "common_leaves": f"{PACK}:{name}",
        "common_species": f"{PACK}:{name}",
        "primitive_log": f"riftworld:{s['log']}",
        "max_branch_radius": s["radius"],
        "primary_thickness": 3,
        "secondary_thickness": 2,
        "loot_volume_multiplier": 1.0,
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
        "smother": s["smother"],
        "light_requirement": 7,
        "fire_spread": s["fire"],
        "flammability": s["flamm"],
        "requires_shears": False,
        "only_if_loaded": "dynamictrees",
    }
    write(os.path.join(DT, "trees", PACK, "leaves_properties", name + ".json"), leaves)

    species = {
        "family": f"{PACK}:{name}",
        "tapering": s["tapering"],
        "signal_energy": s["energy"],
        "growth_rate": s["rate"],
        "up_probability": s["up"],
        "lowest_branch_height": s["branch"],
        "max_branch_radius": s["radius"],
        "soil_longevity": s["longevity"],
        "world_gen_leaf_map_height": s["leaf_map"],
        "drop_seeds": True,
        "seed_composter_chance": 0.3,
        "leaves_properties": f"{PACK}:{name}",
        # a regex over biome ids (DT: NameRegexMatchHolderSet), one pattern per species
        "perfect_biomes": {"name": s["biomes"][0]},
        "generate_seed": True,
        # seeds only: a sapling would need a generated model we do not ship, seeds are enough (and DT-spawned trees
        # drop them, so the player can grow the same species anywhere)
        "generate_sapling": False,
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
    # NOT Python's hash(): it is salted per process, which made every generator run produce a different sprite
    rng = random.Random(zlib.crc32(name.encode("utf-8")) & 0xFFFF)
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
    if CHECK:
        if not os.path.exists(path):
            DRIFT.append(os.path.relpath(path, ROOT))
        else:
            import io
            buf = io.BytesIO()
            img.save(buf, format="PNG")
            if open(path, "rb").read() != buf.getvalue():
                DRIFT.append(os.path.relpath(path, ROOT))
        return
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


def gradle_prop(path: str, key: str, default: str) -> str:
    """Reads `key=value` from a gradle.properties — the single source of truth for versions."""
    try:
        with open(path, encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line.startswith(key + "="):
                    return line.split("=", 1)[1].strip()
    except OSError:
        pass
    return default


# Versions must not be typed twice: a wrong versionRange makes Forge refuse to load the add-on before the game even
# starts (an add-on that asks for riftworld [1.0,) does not load against riftworld 0.6.1 — that bug existed here).
ADDON_VERSION = gradle_prop(os.path.join(ROOT, "dtcompat", "gradle.properties"), "dt_addon_version", "1.0.0")
MOD_VERSION = gradle_prop(os.path.join(ROOT, "mod", "gradle.properties"), "mod_version", "0.6.1")
# the tree pack was validated against this exact Dynamic Trees source tree (tools/rift/fetch_dt_schema.py)
DT_VALIDATED = "1.4.11"

MODS_TOML = f'''modLoader = "javafml"
loaderVersion = "[47,)"
license = "All Rights Reserved"
issueTrackerURL = "https://github.com/standimaoff-oss/riftworld/issues"

[[mods]]
modId = "riftworld_dt"
version = "{ADDON_VERSION}"
displayName = "Разлом: Dynamic Trees"
description = """Dynamic Trees add-on for Rift (Разлом): four species (lilac, sizo v2, fern tree, mist pine).
Tree pack validated against Dynamic Trees {DT_VALIDATED} for Minecraft 1.20.1."""

[[dependencies.riftworld_dt]]
    modId = "forge"
    type = "required"
    versionRange = "[47,)"
    ordering = "NONE"
    side = "BOTH"

[[dependencies.riftworld_dt]]
    modId = "riftworld"
    type = "required"
    versionRange = "[{MOD_VERSION},)"
    ordering = "AFTER"
    side = "BOTH"

[[dependencies.riftworld_dt]]
    modId = "dynamictrees"
    type = "optional"
    versionRange = "[1.4,)"
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

def readme_text() -> str:
    rows = []
    for n, d in SPECIES.items():
        rows.append(f"| `{PACK}:{n}` | {d['ru']} | `{d['biomes'][0]}` | {d['kit']} | {d['logic'] or 'по умолчанию'} | "
                    f"{d['energy']} / {d['up']} / {d['branch']} / {d['tapering']} / {d['radius']} / {d['rate']} |")
    return f"""# riftworld_dt — дополнение к Dynamic Trees

Data-only мод: ни одного класса, только дерево-пак Dynamic Trees (`trees/{PACK}/`) и ресурсы семян.
Если Dynamic Trees не установлен, пак ничего не делает (`only_if_loaded`), а мод просто грузится пустым.
Собран и проверен против **Dynamic Trees {DT_VALIDATED}** для Minecraft 1.20.1 (ветка `release/1.20.1`).

## Породы

Деревья строит сам Dynamic Trees. Наш вклад — параметры силуэта, текстуры коры/спила и листвы, семена.
Если DT не установлен, мод выращивает деревья своим встроенным движком, так что биомы не пустеют.

| id | порода | биом | cell kit | growth logic | energy / up / ветки с / taper / радиус / rate |
|----|--------|------|----------|--------------|-----------------------------------------------|
{chr(10).join(rows)}

Породы босса/приспешников и «Адские угодья» не затрагиваются: в `ashen_lands` и `elisar_city` прописано
`species: null`, там растут только собственные обгоревшие деревья мода.

## Почему ключи именно такие

Ключи, типы и kit'ы сверены с исходниками Dynamic Trees: `python3 tools/rift/fetch_dt_schema.py` собирает
`tools/rift/dt_schema.json` (ключи лоадеров + собственный пак DT + типы значений + типы почв), а
`python3 tools/rift/check_dt_pack.py` проверяет пак против него — включая ссылки на блоки, биомы, текстуры,
модели семян, `world_gen` и диапазоны версий в `mods.toml`. У валидатора есть самопроверка мутациями.

## Сборка

Пак генерируется скриптом: `python3 tools/rift/gen_dt_addon.py` (идемпотентно; спрайты семян детерминированы).
Дальше это обычный Forge-проект: `cd dtcompat && ./gradlew build` (нужен интернет для Forge/Mojang).
Файл `libs/DynamicTrees-1.20.1-*.jar` для компиляции **не нужен** — в моде нет кода, обращающегося к API
Dynamic Trees, только ресурсы, поэтому несовпадение версий API пак не ломает.

Версии в `mods.toml` берутся из `gradle.properties`: `dt_addon_version` — версия аддона, `mod_version` мода —
нижняя граница зависимости `riftworld`. Руками их править не надо (иначе Forge откажется грузить аддон).
"""


README = readme_text()

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
    if CHECK:
        if DRIFT:
            print(f"РАСХОЖДЕНИЕ: {len(DRIFT)} файлов пака не совпадают с генератором")
            for d in DRIFT:
                print("  -", d)
            print("исправляется прогоном: python3 tools/rift/gen_dt_addon.py")
            raise SystemExit(1)
        print(f"пак соответствует генератору: {len(SPECIES)} пород, {n} файлов, расхождений 0")
        return
    print(f"riftworld_dt: {len(SPECIES)} пород, {n} файлов в dtcompat/src/main/resources")


if __name__ == "__main__":
    main()
