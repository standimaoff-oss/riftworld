#!/usr/bin/env python3
"""v0.52.0 — рецепты брони мода.

Раскладки — РОВНО ванильные 1.20.1 (leather/diamond/netherite брони),
подменён только материал. Ванильные раскладки (data/minecraft/recipes):

    шлем       "XXX" / "X X"
    нагрудник  "X X" / "XXX" / "XXX"
    поножи     "XXX" / "X X" / "X X"
    ботинки   "X X" / "X X"

Ключ в раскладке — один символ "X" (в ванилле он тоже "X"), и в key он
обязан присутствовать: Minecraft ругается "Pattern references symbol 'L'
but it's not defined in the key", если символ раскладки не объявлен.

Запуск: python3 tools/rift/gen_v052_armor_recipes.py
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RECIPES = ROOT / "mod/src/main/resources/data/riftworld/recipes"

# материал -> item-идентификатор ингредиента
SETS = {
    "leather": "minecraft:leather",
    "aurite": "riftworld:aurite_ingot",
    "riftcrystal": "riftworld:riftcrystal",
}

# Ванильная раскладка 1.20.1 (одинакова для кожи и металлов).
# Символ ключа — "X", как в ванильных рецептах.
LAYOUT = {
    "helmet": [
        "XXX",
        "X X",
    ],
    "chestplate": [
        "X X",
        "XXX",
        "XXX",
    ],
    "leggings": [
        "XXX",
        "X X",
        "X X",
    ],
    "boots": [
        "X X",
        "X X",
    ],
}

KEY = "X"


def main() -> None:
    RECIPES.mkdir(parents=True, exist_ok=True)
    made = 0
    for set_name, material in SETS.items():
        for part, rows in LAYOUT.items():
            # страховка: каждый символ раскладки обязан быть объявлен в key
            used = set("".join(rows))
            assert used <= {KEY, " "}, f"{set_name}_{part}: символы {used - {KEY, ' '}} без ключа"
            data = {
                "type": "minecraft:crafting_shaped",
                "category": "equipment",
                "pattern": rows,
                "key": {KEY: {"item": material}},
                "result": {"item": f"riftworld:{set_name}_{part}", "count": 1},
            }
            path = RECIPES / f"{set_name}_{part}.json"
            path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
            made += 1
            print("  %s" % path.name)
    print("OK v0.52.0 armor recipes: %d" % made)


if __name__ == "__main__":
    main()
