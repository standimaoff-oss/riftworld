#!/usr/bin/env python3
"""v0.40.0: JSON-ресурсы и крафты снаряжения Элисара.

Генерирует модели предметов, blockstate/модель руды, таблицу добычи и все рецепты.
Раскладки — ванильные, с подменой материала (2 палки + 3 руды сверху = кирка и т.д.).

Запуск: python tools/rift/gen_v040_assets.py
"""
from __future__ import annotations

import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
ASSETS = ROOT / "mod/src/main/resources/assets/riftworld"
DATA = ROOT / "mod/src/main/resources/data/riftworld"
VANILLA = ROOT / "mod/src/main/resources/data/minecraft/tags/blocks"

MATERIALS = ("aurite", "riftcrystal")
GEM = {"aurite": "riftworld:aurite_ingot", "riftcrystal": "riftworld:riftcrystal"}

TOOLS = ("pickaxe", "axe", "shovel", "hoe", "sword")
ARMOUR = ("helmet", "chestplate", "leggings", "boots")

# Ванильные раскладки: M — материал, S — палка.
PATTERNS = {
    "pickaxe": (["MMM", " S ", " S "], 1),
    "axe": (["MM", "MS", " S"], 1),
    "shovel": (["M", "S", "S"], 1),
    "hoe": (["MM", " S", " S"], 1),
    "sword": (["M", "M", "S"], 1),
    "helmet": (["MMM", "M M"], 1),
    "chestplate": (["M M", "MMM", "MMM"], 1),
    "leggings": (["MMM", "M M", "M M"], 1),
    "boots": (["M M", "M M"], 1),
}


def write(path: pathlib.Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def j(obj) -> str:
    return json.dumps(obj, ensure_ascii=False, indent=2) + "\n"


def main() -> None:
    made = 0

    # ---------------------------------------------------------------- models
    for mat in MATERIALS:
        for tool in TOOLS:
            write(ASSETS / f"models/item/{mat}_{tool}.json", j({
                "parent": "minecraft:item/handheld",
                "textures": {"layer0": f"riftworld:item/{mat}_{tool}"},
            }))
            made += 1
        for slot in ARMOUR:
            write(ASSETS / f"models/item/{mat}_{slot}.json", j({
                "parent": "minecraft:item/generated",
                "textures": {"layer0": f"riftworld:item/{mat}_{slot}"},
            }))
            made += 1

    write(ASSETS / "models/item/aurite_ingot.json", j({
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "riftworld:item/aurite_ingot"},
    }))
    write(ASSETS / "models/item/riftcrystal.json", j({
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "riftworld:item/riftcrystal"},
    }))
    made += 2

    for egg in ("elisar_villager_spawn_egg", "elisar_guard_spawn_egg", "elisar_archer_spawn_egg"):
        write(ASSETS / f"models/item/{egg}.json", j({"parent": "minecraft:item/template_spawn_egg"}))
        made += 1

    # ------------------------------------------------------------ руда рифт-кристалла
    write(ASSETS / "blockstates/riftcrystal_ore.json", j({
        "variants": {"": [{"model": "riftworld:block/riftcrystal_ore", "y": y} for y in (0, 90, 180, 270)]}
    }))
    write(ASSETS / "models/block/riftcrystal_ore.json", j({
        "parent": "minecraft:block/cube_all",
        "textures": {"all": "riftworld:block/riftcrystal_ore"},
    }))
    write(ASSETS / "models/item/riftcrystal_ore.json", j({"parent": "riftworld:block/riftcrystal_ore"}))
    made += 3

    write(DATA / "loot_tables/blocks/riftcrystal_ore.json", j({
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": "riftworld:riftcrystal_ore"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    }))
    made += 1

    # ---------------------------------------------------------------- рецепты
    for mat in MATERIALS:
        gem = GEM[mat]
        for name, (pattern, count) in PATTERNS.items():
            key = {"M": {"item": gem}, "S": {"item": "minecraft:stick"}}
            write(DATA / f"recipes/{mat}_{name}.json", j({
                "type": "minecraft:crafting_shaped",
                "pattern": pattern,
                "key": key,
                "result": {"item": f"riftworld:{mat}_{name}", "count": count},
            }))
            made += 1

    # Выплавка: ауритовая руда уже идёт в монеты — добавляем из неё же слиток,
    # а руда рифт-кристалла плавится в сам кристалл.
    write(DATA / "recipes/aurite_ingot_from_ore.json", j({
        "type": "minecraft:smelting",
        "ingredient": {"item": "riftworld:aurite_ore"},
        "result": "riftworld:aurite_ingot",
        "experience": 0.7,
        "cookingtime": 200,
    }))
    write(DATA / "recipes/aurite_ingot_from_ore_blasting.json", j({
        "type": "minecraft:blasting",
        "ingredient": {"item": "riftworld:aurite_ore"},
        "result": "riftworld:aurite_ingot",
        "experience": 0.7,
        "cookingtime": 100,
    }))
    write(DATA / "recipes/riftcrystal_from_ore.json", j({
        "type": "minecraft:smelting",
        "ingredient": {"item": "riftworld:riftcrystal_ore"},
        "result": "riftworld:riftcrystal",
        "experience": 1.5,
        "cookingtime": 200,
    }))
    write(DATA / "recipes/riftcrystal_from_ore_blasting.json", j({
        "type": "minecraft:blasting",
        "ingredient": {"item": "riftworld:riftcrystal_ore"},
        "result": "riftworld:riftcrystal",
        "experience": 1.5,
        "cookingtime": 100,
    }))
    # Излишек слитков можно превратить в ходячую валюту города (обратного хода нет).
    write(DATA / "recipes/elisar_ducat_from_aurite_ingot.json", j({
        "type": "minecraft:crafting_shapeless",
        "ingredients": [{"item": "riftworld:aurite_ingot"}],
        "result": {"item": "riftworld:elisar_ducat", "count": 2},
    }))
    made += 5

    # ---------------------------------------------------------------- теги
    pick = json.loads((VANILLA / "mineable/pickaxe.json").read_text(encoding="utf-8"))
    if "riftworld:riftcrystal_ore" not in pick["values"]:
        pick["values"].append("riftworld:riftcrystal_ore")
        write(VANILLA / "mineable/pickaxe.json", j(pick))
        made += 1
    write(VANILLA / "needs_diamond_tool.json", j({
        "replace": False,
        "values": ["riftworld:riftcrystal_ore"],
    }))
    made += 1

    print(f"Wrote {made} JSON assets.")


if __name__ == "__main__":
    main()
