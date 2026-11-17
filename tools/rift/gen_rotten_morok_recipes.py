#!/usr/bin/env python3
"""v0.55.0 — рецепты гнилого дерева Мороковой Пади.

Полный набор — как у бледного дерева, но из гнилых досок. Терновая дверь
не крафтится: её не рубят, а находят в пади (см. MorokHollow).

Запуск:
    python tools/rift/gen_rotten_morok_recipes.py
"""

from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
REC = ROOT / "mod/src/main/resources/data/riftworld/recipes"


def write(name: str, data: dict) -> None:
    path = REC / f"{name}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def shapeless_planks(log: str) -> dict:
    return {
        "type": "minecraft:crafting_shapeless",
        "category": "building",
        "group": "planks",
        "ingredients": [{"item": f"riftworld:{log}"}],
        "result": {"item": "riftworld:rotten_morok_planks", "count": 4},
    }


def main() -> int:
    # Доски из бревна и древесины — четыре варианта, как у бледного.
    for src in ("rotten_morok_log", "stripped_rotten_morok_log",
                "rotten_morok_wood", "stripped_rotten_morok_wood"):
        write(f"rotten_morok_planks_from_{src}", shapeless_planks(src))

    P = "riftworld:rotten_morok_planks"

    def key_of(item: str) -> dict:
        return {"item": item}

    # Ступени.
    write("rotten_morok_planks_stairs", {
        "type": "minecraft:crafting_shaped", "category": "building", "group": "building",
        "pattern": ["P  ", "PP ", "PPP"],
        "key": {"P": key_of(P)},
        "result": {"item": "riftworld:rotten_morok_planks_stairs", "count": 4},
    })
    # Плита.
    write("rotten_morok_planks_slab", {
        "type": "minecraft:crafting_shaped", "category": "building", "group": "building",
        "pattern": ["PPP"],
        "key": {"P": key_of(P)},
        "result": {"item": "riftworld:rotten_morok_planks_slab", "count": 6},
    })
    # Забор.
    write("rotten_morok_fence", {
        "type": "minecraft:crafting_shaped", "category": "building", "group": "building",
        "pattern": ["PSP", "PSP"],
        "key": {"P": key_of(P), "S": key_of("minecraft:stick")},
        "result": {"item": "riftworld:rotten_morok_fence", "count": 3},
    })
    # Калитка.
    write("rotten_morok_fence_gate", {
        "type": "minecraft:crafting_shaped", "category": "building", "group": "building",
        "pattern": ["SPS", "SPS"],
        "key": {"P": key_of(P), "S": key_of("minecraft:stick")},
        "result": {"item": "riftworld:rotten_morok_fence_gate", "count": 1},
    })
    # Дверь.
    write("rotten_morok_door", {
        "type": "minecraft:crafting_shaped", "category": "building", "group": "building",
        "pattern": ["PP", "PP", "PP"],
        "key": {"P": key_of(P)},
        "result": {"item": "riftworld:rotten_morok_door", "count": 3},
    })
    # Люк.
    write("rotten_morok_trapdoor", {
        "type": "minecraft:crafting_shaped", "category": "building", "group": "building",
        "pattern": ["PP", "PP"],
        "key": {"P": key_of(P)},
        "result": {"item": "riftworld:rotten_morok_trapdoor", "count": 2},
    })
    # Нажимная плита.
    write("rotten_morok_pressure_plate", {
        "type": "minecraft:crafting_shaped", "category": "building", "group": "building",
        "pattern": ["PP"],
        "key": {"P": key_of(P)},
        "result": {"item": "riftworld:rotten_morok_pressure_plate", "count": 1},
    })
    # Кнопка.
    write("rotten_morok_button", {
        "type": "minecraft:crafting_shaped", "category": "building", "group": "building",
        "pattern": ["P"],
        "key": {"P": key_of(P)},
        "result": {"item": "riftworld:rotten_morok_button", "count": 1},
    })
    print("OK: рецепты гнилого дерева — 13")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
