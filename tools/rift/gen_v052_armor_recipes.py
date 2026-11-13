#!/usr/bin/env python3
"""v0.52.0 — рецепты брони мода.

Раскладки — РОВНО ванильные (как у leather/diamond/netherite брони Minecraft),
подменён только материал. Ничего нового не изобретаем:

  шлем      "MMM" / "MMM"            (для кожи — "LLL" / " L ")
  нагрудник "MMM" / "MMM" / "MMM"    (для кожи — "L L" / "LLL" / "LLL")
  поножи    "MMM" / "MMM" / " L "     (для кожи — "LLL" / " L " / " L ")
  ботинки   "L L" / "L L"

Запуск: python3 tools/rift/gen_v052_armor_recipes.py
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RECIPES = ROOT / "mod/src/main/resources/data/riftworld/recipes"

# материал -> (id предмета, ключ в рецепте, item-тег для «кожи»)
SETS = {
    "leather": ("minecraft:leather", "L"),
    "aurite": ("riftworld:aurite_ingot", "M"),
    "riftcrystal": ("riftworld:riftcrystal", "M"),
}

# раскладки по частям (ванильные). Кожа и «полные» комплекты различаются:
# у кожи в шлеме/нагруднике/поножах есть характерные вырезы.
# Формат — список строк, каждая строка = ряд раскладки (как в ванильных JSON).
LAYOUT_METAL = {
    "helmet": [
        "MMM",
        "MMM",
    ],
    "chestplate": [
        "MMM",
        "MMM",
        "MMM",
    ],
    "leggings": [
        "MMM",
        "MMM",
        " L ",
    ],
    "boots": [
        "L L",
        "L L",
    ],
}

LAYOUT_LEATHER = {
    "helmet": [
        "LLL",
        "LLL",
        " L ",
    ],
    "chestplate": [
        "L L",
        "LLL",
        "LLL",
    ],
    "leggings": [
        "LLL",
        "LLL",
        " L ",
    ],
    "boots": [
        "L L",
        "L L",
    ],
}


def compact(rows):
    """Убирает полностью пустые строки — так же, как это делает ванильный JSON."""
    return [r for r in rows if r.strip()]


def main() -> None:
    RECIPES.mkdir(parents=True, exist_ok=True)
    made = 0
    for set_name, (material, key) in SETS.items():
        layout = LAYOUT_LEATHER if set_name == "leather" else LAYOUT_METAL
        for part, rows in layout.items():
            pat = compact(rows)
            data = {
                "type": "minecraft:crafting_shaped",
                "pattern": pat,
                "key": {key: {"item": material}},
                "result": {"item": f"riftworld:{set_name}_{part}", "count": 1},
            }
            path = RECIPES / f"{set_name}_{part}.json"
            path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
            made += 1
            print("  %s" % path.name)
    print("OK v0.52.0 armor recipes: %d" % made)


if __name__ == "__main__":
    main()
