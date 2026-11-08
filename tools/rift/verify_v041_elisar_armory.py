#!/usr/bin/env python3
"""Strict static acceptance checks for the v0.41.0 equipment/settlement delivery."""
from pathlib import Path
import json
import struct

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "mod/src/main/resources"
A = RES / "assets/riftworld"
D = RES / "data"
REPORT = ROOT / "reports/elisar_armory"
checks = []


def check(condition, message):
    if not condition:
        raise AssertionError(message)
    checks.append(message)


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def png_size(path):
    data = path.read_bytes()
    check(data[:8] == b"\x89PNG\r\n\x1a\n", f"PNG signature: {path.name}")
    return struct.unpack(">II", data[16:24])


MATERIALS = ("aurite", "riftcrystal")
PARTS = ("helmet", "chestplate", "leggings", "boots")
TOOLS = ("sword", "pickaxe", "war_axe", "bow", "shield")
GEAR = PARTS + TOOLS
EXPECTED_BIOMES = {"minecraft:lush_caves", "minecraft:dripstone_caves", "minecraft:deep_dark"}

for material in MATERIALS:
    for part in GEAR:
        item = f"{material}_{part}"
        model = load(A / f"models/item/{item}.json")
        texture = A / f"textures/item/{item}.png"
        check(texture.exists(), f"texture: {item}")
        check(png_size(texture) == (32, 32), f"32x32 item art: {item}")
        check(model["textures"]["layer0"] == f"riftworld:item/{item}", f"model texture link: {item}")
        check((D / f"riftworld/recipes/{item}.json").exists(), f"crafting recipe: {item}")
        for lang in ("en_us", "ru_ru"):
            check(f"item.riftworld.{item}" in load(A / f"lang/{lang}.json"), f"{lang} translation: {item}")
    for layer in (1, 2):
        path = A / f"textures/models/armor/{material}_layer_{layer}.png"
        check(png_size(path) == (128, 128), f"128x128 geometric worn armour layer: {material} {layer}")
    bow = load(A / f"models/item/{material}_bow.json")
    check(len(bow.get("overrides", [])) == 3, f"three bow pull predicates: {material}")
    for stage in range(3):
        check((A / f"models/item/{material}_bow_pulling_{stage}.json").exists(), f"bow stage model: {material} {stage}")
        check(png_size(A / f"textures/item/{material}_bow_pulling_{stage}.png") == (32, 32), f"bow stage art: {material} {stage}")
    shield = load(A / f"models/item/{material}_shield.json")
    check(shield["overrides"][0]["predicate"] == {"blocking": 1}, f"shield blocking predicate: {material}")

for item in ("aurite_ingot", "riftcrystal_shard", "riftcrystal_ingot"):
    check(png_size(A / f"textures/item/{item}.png") == (32, 32), f"material art: {item}")
    check((A / f"models/item/{item}.json").exists(), f"material model: {item}")
check(load(D / "riftworld/recipes/ducats_from_aurite_ingot.json")["result"]["count"] == 4,
      "aurite ingot reverses to exactly four ducats")

for ore in ("riftcrystal_ore", "deepslate_riftcrystal_ore"):
    check(png_size(A / f"textures/block/{ore}.png") == (16, 16), f"16x16 ore art: {ore}")
    check((A / f"blockstates/{ore}.json").exists(), f"ore blockstate: {ore}")
    loot = json.dumps(load(D / f"riftworld/loot_tables/blocks/{ore}.json"))
    check("minecraft:silk_touch" in loot, f"silk touch ore alternative: {ore}")
    check("minecraft:fortune" in loot and "minecraft:ore_drops" in loot, f"fortune shard bonus: {ore}")
    check((D / f"riftworld/recipes/{ore}_smelting.json").exists(), f"ore smelting fallback: {ore}")

modifier = load(D / "riftworld/forge/biome_modifier/add_riftcrystal_ore.json")
check(set(modifier["biomes"]) == EXPECTED_BIOMES and len(modifier["biomes"]) == 3,
      "riftcrystal biome modifier contains exactly three intended biomes")
check(set(load(D / "riftworld/tags/worldgen/biome/has_riftcrystal_ore.json")["values"]) == EXPECTED_BIOMES,
      "documented biome tag matches the modifier")
placed = load(D / "riftworld/worldgen/placed_feature/ore_riftcrystal.json")
check(placed["placement"][0] == {"type": "minecraft:rarity_filter", "chance": 3}, "rare 1/3 chunk placement")
configured = load(D / "riftworld/worldgen/configured_feature/ore_riftcrystal.json")
check(configured["config"]["size"] <= 3, "small rare ore veins")
check(configured["config"]["discard_chance_on_air_exposure"] >= 0.65, "hidden veins rather than surface spam")
for path in D.rglob("*.json"):
    if path.name == "add_riftcrystal_ore.json":
        continue
    if "riftcrystal_ore" in path.name and "biome_modifier" in str(path):
        raise AssertionError(f"unexpected extra riftcrystal biome modifier: {path}")

for role in ("villager", "guard", "archer"):
    check(png_size(A / f"textures/entity/elisar_{role}.png") == (64, 64), f"64x64 resident skin: {role}")
    check(png_size(A / f"textures/item/elisar_{role}_spawn_egg.png") == (32, 32), f"spawn egg art: {role}")
    for lang in ("en_us", "ru_ru"):
        data = load(A / f"lang/{lang}.json")
        check(f"entity.riftworld.elisar_{role}" in data, f"{lang} entity name: {role}")
        check(f"item.riftworld.elisar_{role}_spawn_egg" in data, f"{lang} egg name: {role}")

entities = (ROOT / "mod/src/main/java/ru/aiforge/riftworld/registry/ModEntities.java").read_text()
residents = (ROOT / "mod/src/main/java/ru/aiforge/riftworld/world/ElisarSettlementResidents.java").read_text()
generator = (ROOT / "mod/src/main/java/ru/aiforge/riftworld/world/ElisarChunkGenerator.java").read_text()
for role in ("ELISAR_VILLAGER", "ELISAR_GUARD", "ELISAR_ARCHER"):
    check(role in entities and role in residents, f"registered and settlement-spawned role: {role}")
check("ElisarSettlementResidents.spawnOriginal(region)" in generator, "custom generator populates residents after structures")
check("StructureTags.VILLAGE" in residents, "vanilla village structure guard")
check("getPersistentData().getBoolean(POPULATED)" in residents, "no duplicate vanilla companions after reload")

armor_item = (ROOT / "mod/src/main/java/ru/aiforge/riftworld/item/RiftArmorItem.java").read_text()
armor_model = (ROOT / "mod/src/main/java/ru/aiforge/riftworld/client/ElisarArmorModel.java").read_text()
check("getHumanoidArmorModel" in (ROOT / "mod/src/main/java/ru/aiforge/riftworld/client/ElisarArmorClientExtensions.java").read_text()
      and "initializeClient" in armor_item, "Forge client extension selects the worn armour model")
check("LayerDefinition.create(mesh, 128, 128)" in armor_model
      and all(part in armor_model for part in ("shoulder_shell", "breastplate", "knee_guard", "shoulder_crystal")),
      "real 128x128 geometry includes shoulders, breastplate, knees and crystals")

REPORT.mkdir(parents=True, exist_ok=True)
result = {"status": "PASS", "checks": len(checks), "equipment_per_material": len(GEAR),
          "armour_pieces_total": 8, "weapons_tools_total": 10, "resident_types": 3,
          "riftcrystal_biomes": sorted(EXPECTED_BIOMES)}
(REPORT / "verification.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
print(json.dumps(result, ensure_ascii=False, indent=2))
