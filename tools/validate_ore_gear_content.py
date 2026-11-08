#!/usr/bin/env python3
"""Static regression checks for the Aurite and Riftcrystal content pack."""
import json
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "mod/src/main/resources"
ASSETS = RES / "assets/riftworld"

def load(path):
    with path.open(encoding="utf-8") as stream:
        return json.load(stream)

def png_size(path):
    with path.open("rb") as stream:
        header = stream.read(24)
    assert header[:8] == b"\x89PNG\r\n\x1a\n", f"not a PNG: {path}"
    return struct.unpack(">II", header[16:24])

# Ensure recipes and loot are syntactically valid and every newly registered item has an icon.
new_items = [
    "aurite_ingot", "riftcrystal", "aurite_sword", "aurite_pickaxe", "aurite_axe", "aurite_shovel",
    "riftcrystal_sword", "riftcrystal_pickaxe", "riftcrystal_axe", "riftcrystal_shovel",
    *[f"aurite_{slot}" for slot in ("helmet", "chestplate", "leggings", "boots")],
    *[f"riftcrystal_{slot}" for slot in ("helmet", "chestplate", "leggings", "boots")],
]
for item in new_items:
    model = load(ASSETS / "models/item" / f"{item}.json")
    icon = ASSETS / "textures/item" / f"{item}.png"
    assert model["textures"]["layer0"] == f"riftworld:item/{item}"
    assert png_size(icon) == (16, 16), f"wrong item texture size: {item}"

for material in ("aurite", "riftcrystal"):
    assert png_size(ASSETS / f"textures/models/armor/{material}_layer_1.png") == (64, 32)
    assert png_size(ASSETS / f"textures/models/armor/{material}_layer_2.png") == (64, 32)

for path in (RES / "data/riftworld/recipes").glob("*.json"):
    load(path)
load(RES / "data/riftworld/loot_tables/blocks/riftcrystal_ore.json")
load(ASSETS / "blockstates/riftcrystal_ore.json")
for locale in ("ru_ru", "en_us"):
    entries = load(ASSETS / f"lang/{locale}.json")
    for item in new_items:
        assert f"item.riftworld.{item}" in entries, f"missing {locale} name for {item}"
    assert "block.riftworld.riftcrystal_ore" in entries

# World generation must be guarded by exactly the three intended custom regions.
generator = (ROOT / "mod/src/main/java/ru/aiforge/riftworld/world/ElisarChunkGenerator.java").read_text()
needle = "boolean rareCrystalBiome = (c.isl != null && c.isl.kind == CrystalRifts.KIND)"
assert needle in generator and "|| MorokHollow.inside(x + 0.5, z + 0.5) || MistKeep.inside(x + 0.5, z + 0.5)" in generator
assert "hash(x, z, y * 31 + 791) < 0.00055" in generator
print(f"Validated {len(new_items)} new gear icons, armor atlases, recipes, localization and restricted ore generation.")
