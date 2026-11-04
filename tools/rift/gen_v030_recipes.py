#!/usr/bin/env python3
"""
v0.30.0 Survival Crafting Recipe Generator & Collision Verifier:
- Removes ALL Laboratory block recipes (`lab_*`, `steel_floor*`, `hazard_block`, `metal_grate`,
  `emergency_lamp`, `pipe`, `wall_vent`, etc.) and Very Rare / Ritual Core recipes (`mosaic_core`,
  `temple_gold_rune`), ensuring 0 recipes target any Laboratory or Rare/Boss/Ore block.
- Generates intuitive Minecraft-style survival crafting, stonecutting & smelting recipes for
  practically ALL remaining mod blocks (all building stones, trims, beams, crystal blocks, ground
  surfaces, lanterns, signs, carpets, castle props, and all 64 non-rare Elisar 3D furniture/props).
- Runs a strict mathematical recipe collision detector (shaped trim + horizontal mirror + shapeless
  multiset + tag expansion + vanilla recipe shadow check) and asserts 0 collisions & 0 unknown IDs.
"""

import glob
import json
import os
import re

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "mod", "src", "main", "resources")
DATA = os.path.join(RES, "data")
RECIPES_DIR = os.path.join(DATA, "riftworld", "recipes")

LAB_BLOCKS = {
    "lab_panel", "lab_panel_dirty", "lab_panel_stripe", "lab_panel_stripe_dirty",
    "lab_tile", "lab_tile_cracked", "lab_tile_slab", "lab_tile_stairs",
    "lab_concrete", "lab_concrete_cracked", "lab_concrete_slab", "lab_concrete_stairs",
    "lab_facade", "lab_ceiling", "lab_ceiling_dirty", "lab_glass", "lab_glass_cracked",
    "lab_lamp", "lab_railing", "steel_floor", "steel_floor_rusty", "steel_floor_slab",
    "steel_floor_stairs", "hazard_block", "hazard_block_worn", "shaft_wall",
    "metal_grate", "rusty_grate", "emergency_lamp", "pipe", "wall_vent",
    "debris", "dirt_pile", "dead_moss", "dead_vines",
    # Non-Elisar 3D Lab Models (!ModelKind.isElisar())
    "portal_helm", "portal_lever", "control_console", "lathe", "milling_machine",
    "specimen_tank", "electrical_cabinet", "generator", "transformer", "server_rack",
    "security_desk", "crate_wood", "crate_metal", "lab_door", "lab_table", "shelf",
    "office_chair", "locker", "trash_bin_tipped", "chandelier", "siren", "shelf_fallen",
    "wall_notes", "desk_computer", "table_flipped", "barrels", "fire_extinguisher",
    "box_pile", "paper_pile", "chair_fallen",
}

RARE_OR_RAW_ORE_BLOCKS = {
    "mosaic_core", "temple_gold_rune", "light_bridge", "temple_spirit",
    "boss_throne", "gold_hoard", "boss_column", "arena_core",
    "aurite_ore", "ducat_ore", "deepslate_ducat_ore",
    "crystal_cluster", "magma_meander",
}


def write_json(path: str, obj: dict):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def ing(spec: str) -> dict:
    if spec.startswith("#"):
        return {"tag": spec[1:]}
    return {"item": spec}


def shaped(name: str, pattern: list[str], key: dict[str, str], result: str, count: int = 1):
    res_obj = {"item": result}
    if count > 1:
        res_obj["count"] = count
    write_json(
        os.path.join(RECIPES_DIR, f"{name}.json"),
        {
            "type": "minecraft:crafting_shaped",
            "pattern": pattern,
            "key": {k: ing(v) for k, v in key.items()},
            "result": res_obj,
        },
    )


def shapeless(name: str, ingredients: list[str], result: str, count: int = 1):
    res_obj = {"item": result}
    if count > 1:
        res_obj["count"] = count
    write_json(
        os.path.join(RECIPES_DIR, f"{name}.json"),
        {
            "type": "minecraft:crafting_shapeless",
            "ingredients": [ing(x) for x in ingredients],
            "result": res_obj,
        },
    )


def stonecutting(name: str, ingredient: str, result: str, count: int = 1):
    write_json(
        os.path.join(RECIPES_DIR, f"{name}.json"),
        {
            "type": "minecraft:stonecutting",
            "ingredient": ing(ingredient),
            "result": result,
            "count": count,
        },
    )


def smelting(name: str, ingredient: str, result: str, xp: float = 0.1, time: int = 200):
    write_json(
        os.path.join(RECIPES_DIR, f"{name}.json"),
        {
            "type": "minecraft:smelting",
            "ingredient": ing(ingredient),
            "result": result,
            "experience": xp,
            "cookingtime": time,
        },
    )


def remove_forbidden_recipes():
    removed = []
    forbidden_outputs = {f"riftworld:{b}" for b in (LAB_BLOCKS | RARE_OR_RAW_ORE_BLOCKS)}
    forbidden_lab_ings = {f"riftworld:{b}" for b in LAB_BLOCKS}
    for rf in sorted(glob.glob(os.path.join(RECIPES_DIR, "*.json"))):
        d = json.load(open(rf, encoding="utf-8"))
        res = d.get("result")
        rid = res.get("item", "") if isinstance(res, dict) else (res if isinstance(res, str) else "")
        uses_forbidden = rid in forbidden_outputs
        for k_field in ("ingredient",):
            obj = d.get(k_field)
            if isinstance(obj, dict) and obj.get("item") in forbidden_lab_ings:
                uses_forbidden = True
        for obj in d.get("ingredients", []):
            if isinstance(obj, dict) and obj.get("item") in forbidden_lab_ings:
                uses_forbidden = True
        for obj in d.get("key", {}).values():
            if isinstance(obj, dict) and obj.get("item") in forbidden_lab_ings:
                uses_forbidden = True
        if uses_forbidden:
            os.remove(rf)
            removed.append(os.path.basename(rf))
    return removed


def generate_v030_block_recipes():
    # =========================================================================
    # 1. BUILDING STONES, BRICKS, CRYSTALS, CORAL CAPS, BEAMS & GROUND SURFACES
    # =========================================================================
    shaped(
        "tower_bricks",
        ["IS", "SI"],
        {"I": "riftworld:ivory_marble_bricks", "S": "riftworld:rift_slate"},
        "riftworld:tower_bricks",
        4,
    )
    stonecutting("tower_bricks_from_ivory_marble_stonecutting", "riftworld:ivory_marble", "riftworld:tower_bricks", 1)
    smelting("tower_bricks_cracked", "riftworld:tower_bricks", "riftworld:tower_bricks_cracked", xp=0.1, time=200)

    shaped(
        "garden_limestone",
        ["CS", "SC"],
        {"C": "minecraft:calcite", "S": "minecraft:cobblestone"},
        "riftworld:garden_limestone",
        4,
    )
    shaped(
        "rift_slate",
        ["DQ", "QD"],
        {"D": "minecraft:cobbled_deepslate", "Q": "minecraft:quartz"},
        "riftworld:rift_slate",
        4,
    )
    shaped(
        "reef_stone",
        ["CS", "SC"],
        {"C": "riftworld:sea_shells", "S": "minecraft:cobblestone"},
        "riftworld:reef_stone",
        4,
    )
    stonecutting("elisar_cobble_from_elisar_stone_stonecutting", "riftworld:elisar_stone", "riftworld:elisar_cobble", 1)
    shaped(
        "elisar_crust",
        ["SD", "DS"],
        {"S": "riftworld:elisar_stone", "D": "minecraft:dirt"},
        "riftworld:elisar_crust",
        4,
    )
    shaped(
        "elisar_soil",
        ["DG", "GD"],
        {"D": "minecraft:dirt", "G": "riftworld:elisar_silt"},
        "riftworld:elisar_soil",
        4,
    )
    shaped(
        "elisar_silt",
        ["SC", "CS"],
        {"S": "minecraft:sand", "C": "minecraft:clay_ball"},
        "riftworld:elisar_silt",
        4,
    )
    shaped(
        "elisar_seabed",
        ["SG", "GS"],
        {"S": "minecraft:sand", "G": "minecraft:gravel"},
        "riftworld:elisar_seabed",
        4,
    )
    shaped(
        "elisar_path",
        ["SG", "GS"],
        {"S": "riftworld:elisar_soil", "G": "minecraft:gravel"},
        "riftworld:elisar_path",
        4,
    )
    shapeless("rift_turf", ["riftworld:elisar_soil", "riftworld:rift_grass"], "riftworld:rift_turf", 1)
    shapeless("elisar_moss", ["minecraft:moss_block", "riftworld:elisar_ivy"], "riftworld:elisar_moss", 2)
    shapeless("blossom_moss_block", ["minecraft:moss_block", "riftworld:lilac_bloom"], "riftworld:blossom_moss_block", 2)

    shaped(
        "ash_block",
        ["BC", "CB"],
        {"B": "minecraft:basalt", "C": "minecraft:charcoal"},
        "riftworld:ash_block",
        4,
    )
    shaped(
        "burnt_soil",
        ["DC", "CD"],
        {"D": "minecraft:dirt", "C": "minecraft:charcoal"},
        "riftworld:burnt_soil",
        4,
    )
    shaped(
        "charred_log",
        ["LLL", "LCL", "LLL"],
        {"L": "riftworld:sizo_log", "C": "minecraft:charcoal"},
        "riftworld:charred_log",
        8,
    )

    shaped(
        "dark_beam",
        ["D", "N", "D"],
        {"D": "riftworld:dark_planks", "N": "minecraft:iron_nugget"},
        "riftworld:dark_beam",
        2,
    )
    shaped(
        "stripped_dark_beam",
        ["D", "S", "D"],
        {"D": "riftworld:stripped_dark_wood", "S": "minecraft:stick"},
        "riftworld:stripped_dark_beam",
        2,
    )
    for w in ("sizo", "ambra", "garden", "lilac", "crystal", "charred"):
        stonecutting(f"stripped_{w}_log_from_stonecutting", f"riftworld:{w}_log", f"riftworld:stripped_{w}_log", 1)
    stonecutting("stripped_dark_beam_from_stonecutting", "riftworld:dark_beam", "riftworld:stripped_dark_beam", 1)

    shaped(
        "crystal_block",
        ["AQ", "QA"],
        {"A": "minecraft:amethyst_shard", "Q": "minecraft:quartz"},
        "riftworld:crystal_block",
        2,
    )
    shapeless("crystal_block_bright", ["riftworld:crystal_block", "minecraft:glowstone_dust"], "riftworld:crystal_block_bright", 1)
    shapeless("crystal_block_deep", ["riftworld:crystal_block", "minecraft:lapis_lazuli"], "riftworld:crystal_block_deep", 1)

    shaped("coral_cap_pink", ["CC", "CC"], {"C": "riftworld:coral_branch"}, "riftworld:coral_cap_pink", 2)
    shaped("coral_cap_teal", ["CC", "CC"], {"C": "riftworld:coral_tube"}, "riftworld:coral_cap_teal", 2)
    shaped("coral_cap_violet", ["CC", "CC"], {"C": "riftworld:coral_fan"}, "riftworld:coral_cap_violet", 2)

    shaped("lavender_bed", ["VV", "LL"], {"V": "riftworld:violet_bloom", "L": "riftworld:lilac_bloom"}, "riftworld:lavender_bed", 4)
    shaped("shell_bed", ["SS", "SS"], {"S": "riftworld:sea_shells"}, "riftworld:shell_bed", 4)
    shaped("castle_cobweb", ["S S", " C ", "S S"], {"S": "minecraft:string", "C": "minecraft:charcoal"}, "riftworld:castle_cobweb", 2)
    shaped("castle_bars", ["III", "ICI"], {"I": "minecraft:iron_ingot", "C": "minecraft:charcoal"}, "riftworld:castle_bars", 16)
    shaped("elisar_chain", ["G", "I", "N"], {"G": "minecraft:gold_nugget", "I": "minecraft:iron_ingot", "N": "minecraft:iron_nugget"}, "riftworld:elisar_chain", 2)

    # =========================================================================
    # 2. LANTERNS, SCONCES, CHANDELIERS & HEARTHS (11 BLOCKS)
    # =========================================================================
    shaped(
        "street_lamp",
        [" L ", " I ", " B "],
        {"L": "riftworld:elisar_lantern", "I": "minecraft:iron_bars", "B": "riftworld:elisar_bricks"},
        "riftworld:street_lamp",
        1,
    )
    shaped(
        "lantern_hanging",
        ["C", "L"],
        {"C": "riftworld:elisar_chain", "L": "riftworld:elisar_lantern"},
        "riftworld:lantern_hanging",
        1,
    )
    shaped(
        "lantern_hang",
        ["N", "L"],
        {"N": "minecraft:iron_nugget", "L": "riftworld:elisar_lantern"},
        "riftworld:lantern_hang",
        1,
    )
    shaped(
        "lantern_post",
        ["L", "F"],
        {"L": "riftworld:elisar_lantern", "F": "riftworld:elisar_fence"},
        "riftworld:lantern_post",
        1,
    )
    shaped(
        "lantern_wall",
        ["NL"],
        {"N": "minecraft:iron_nugget", "L": "riftworld:elisar_lantern"},
        "riftworld:lantern_wall",
        1,
    )
    shaped(
        "candle_stand",
        ["CCC", " I ", " B "],
        {"C": "riftworld:ritual_candle", "I": "minecraft:iron_ingot", "B": "riftworld:brass_block"},
        "riftworld:candle_stand",
        1,
    )
    shaped(
        "castle_chandelier",
        [" H ", "CCC", "III"],
        {"H": "riftworld:elisar_chain", "C": "riftworld:ritual_candle", "I": "minecraft:iron_ingot"},
        "riftworld:castle_chandelier",
        1,
    )
    shaped(
        "wall_sconce",
        ["C", "B"],
        {"C": "riftworld:ritual_candle", "B": "minecraft:iron_bars"},
        "riftworld:wall_sconce",
        2,
    )
    shaped(
        "fire_bowl",
        ["I I", "ICI", " S "],
        {"I": "minecraft:iron_ingot", "C": "minecraft:coal", "S": "riftworld:elisar_stone"},
        "riftworld:fire_bowl",
        1,
    )
    shaped(
        "fire_pit",
        [" S ", "SCS", "SSS"],
        {"S": "riftworld:elisar_cobble", "C": "minecraft:charcoal"},
        "riftworld:fire_pit",
        1,
    )
    shaped(
        "fireplace",
        ["BBB", "B B", "BCB"],
        {"B": "riftworld:elisar_bricks", "C": "minecraft:campfire"},
        "riftworld:fireplace",
        1,
    )

    # =========================================================================
    # 3. TABLES, CHAIRS, BENCHES, BEDS, CARPETS & WARDROBES (13 BLOCKS)
    # =========================================================================
    shaped(
        "bench",
        ["SSS", "P P"],
        {"S": "riftworld:sizo_planks_slab", "P": "minecraft:stick"},
        "riftworld:bench",
        2,
    )
    shaped(
        "table_wood",
        ["PPP", "S S", "S S"],
        {"P": "riftworld:sizo_planks_slab", "S": "minecraft:stick"},
        "riftworld:table_wood",
        1,
    )
    shaped(
        "chair_wood",
        ["P  ", "PPP", "S S"],
        {"P": "riftworld:sizo_planks_slab", "S": "minecraft:stick"},
        "riftworld:chair_wood",
        2,
    )
    shaped(
        "stool",
        [" P ", "S S"],
        {"P": "riftworld:sizo_planks_slab", "S": "minecraft:stick"},
        "riftworld:stool",
        2,
    )
    shaped(
        "bed_wood",
        ["WWW", "PPP", "S S"],
        {"W": "#minecraft:wool", "P": "riftworld:sizo_planks", "S": "minecraft:stick"},
        "riftworld:bed_wood",
        1,
    )
    shaped(
        "canopy_bed",
        ["CCC", "WWW", "DDD"],
        {"C": "riftworld:castle_carpet", "W": "minecraft:red_wool", "D": "riftworld:dark_planks"},
        "riftworld:canopy_bed",
        1,
    )
    shaped(
        "wardrobe",
        ["PSP", "P P", "PPP"],
        {"P": "riftworld:sizo_planks", "S": "riftworld:sizo_planks_slab"},
        "riftworld:wardrobe",
        1,
    )
    shaped(
        "feast_table",
        ["CCC", "DDD", "B B"],
        {"C": "riftworld:castle_carpet", "D": "riftworld:dark_planks", "B": "riftworld:dark_beam"},
        "riftworld:feast_table",
        1,
    )
    shaped(
        "long_table",
        ["DDD", "B B"],
        {"D": "riftworld:dark_planks_slab", "B": "riftworld:dark_beam"},
        "riftworld:long_table",
        1,
    )
    shaped(
        "castle_bench",
        ["DDD", "S S"],
        {"D": "riftworld:dark_planks_slab", "S": "minecraft:stick"},
        "riftworld:castle_bench",
        2,
    )
    shaped(
        "high_chair",
        ["D  ", "DDD", "F F"],
        {"D": "riftworld:dark_planks_slab", "F": "riftworld:dark_fence"},
        "riftworld:high_chair",
        1,
    )
    shaped(
        "rug",
        ["WWW", "GGG"],
        {"W": "minecraft:white_wool", "G": "minecraft:gold_nugget"},
        "riftworld:rug",
        4,
    )
    shaped(
        "tapestry_horn",
        [" S ", "CCC", " G "],
        {"S": "minecraft:stick", "C": "riftworld:castle_carpet", "G": "minecraft:gold_ingot"},
        "riftworld:tapestry_horn",
        1,
    )

    # =========================================================================
    # 4. KITCHEN, TAVERN, SHOP & STORAGE FURNITURE (17 BLOCKS)
    # =========================================================================
    shaped(
        "bar_counter",
        ["SSS", "PPP", "PBP"],
        {"S": "riftworld:ambra_planks_slab", "P": "riftworld:ambra_planks", "B": "minecraft:barrel"},
        "riftworld:bar_counter",
        1,
    )
    shaped(
        "keg_rack",
        ["SSS", "B B", "SSS"],
        {"S": "riftworld:ambra_planks_slab", "B": "minecraft:barrel"},
        "riftworld:keg_rack",
        1,
    )
    shaped(
        "shop_counter",
        ["SSS", "PCP", "PPP"],
        {"S": "riftworld:sizo_planks_slab", "P": "riftworld:sizo_planks", "C": "minecraft:chest"},
        "riftworld:shop_counter",
        1,
    )
    shaped(
        "shop_shelf",
        ["SSS", "P P", "SSS"],
        {"S": "riftworld:sizo_planks_slab", "P": "minecraft:stick"},
        "riftworld:shop_shelf",
        2,
    )
    shaped(
        "bookshelf_e",
        ["SSS", "BBB", "SSS"],
        {"S": "riftworld:ambra_planks_slab", "B": "minecraft:book"},
        "riftworld:bookshelf_e",
        1,
    )
    shaped(
        "dish_cupboard",
        ["SSS", "GBG", "SSS"],
        {"S": "riftworld:sizo_planks_slab", "G": "minecraft:glass_pane", "B": "minecraft:bowl"},
        "riftworld:dish_cupboard",
        1,
    )
    shaped(
        "kitchen_counter",
        ["EEE", "P P", "PPP"],
        {"E": "riftworld:elisar_stone_slab", "P": "riftworld:sizo_planks"},
        "riftworld:kitchen_counter",
        2,
    )
    shaped(
        "kitchen_stove",
        ["III", "BFB", "BBB"],
        {"I": "minecraft:iron_ingot", "B": "riftworld:elisar_bricks", "F": "minecraft:furnace"},
        "riftworld:kitchen_stove",
        1,
    )
    shaped(
        "kitchen_set",
        ["B G", "NNN"],
        {"B": "minecraft:bowl", "G": "minecraft:glass_bottle", "N": "minecraft:iron_nugget"},
        "riftworld:kitchen_set",
        1,
    )
    shaped(
        "cooking_pot",
        ["S S", "ICI", " F "],
        {"S": "minecraft:stick", "I": "minecraft:iron_ingot", "C": "minecraft:cauldron", "F": "minecraft:campfire"},
        "riftworld:cooking_pot",
        1,
    )
    shaped(
        "hearth_cauldron",
        [" H ", "ACA", " F "],
        {"H": "riftworld:elisar_chain", "A": "riftworld:ashlar", "C": "minecraft:cauldron", "F": "minecraft:campfire"},
        "riftworld:hearth_cauldron",
        1,
    )
    shaped(
        "crate_stack",
        ["PSP", "SCS", "PSP"],
        {"P": "riftworld:sizo_planks", "S": "minecraft:stick", "C": "minecraft:chest"},
        "riftworld:crate_stack",
        2,
    )
    shaped(
        "castle_chest",
        ["DDD", "DCD", "DBD"],
        {"D": "riftworld:dark_planks", "C": "minecraft:chest", "B": "minecraft:iron_ingot"},
        "riftworld:castle_chest",
        1,
    )
    shaped(
        "sacks",
        [" S ", "WWW", "WWW"],
        {"S": "minecraft:string", "W": "riftworld:pale_wheat"},
        "riftworld:sacks",
        2,
    )
    shaped(
        "firewood_stack",
        ["LLL", "SSS"],
        {"L": "riftworld:sizo_log", "S": "minecraft:stick"},
        "riftworld:firewood_stack",
        3,
    )
    shaped(
        "flower_pot_e",
        ["R R", " R "],
        {"R": "riftworld:roof_red_slab"},
        "riftworld:flower_pot_e",
        2,
    )
    shaped(
        "herbs_hanging",
        ["SSS", "FPF"],
        {"S": "minecraft:stick", "F": "riftworld:elisar_flowers", "P": "riftworld:pale_bloom"},
        "riftworld:herbs_hanging",
        2,
    )

    # =========================================================================
    # 5. FORGE, GUARD, HARBOR, MARKET & ALCHEMY PROPS (19 BLOCKS)
    # =========================================================================
    shaped(
        "forge_hearth",
        ["BBB", "BFB", "BCB"],
        {"B": "riftworld:castle_bricks", "F": "minecraft:blast_furnace", "C": "minecraft:coal_block"},
        "riftworld:forge_hearth",
        1,
    )
    shaped(
        "anvil_stump",
        [" I ", "III", " L "],
        {"I": "minecraft:iron_ingot", "L": "riftworld:sizo_log"},
        "riftworld:anvil_stump",
        1,
    )
    shaped(
        "quench_barrel",
        ["PWP", "PBP"],
        {"P": "riftworld:sizo_planks", "W": "minecraft:water_bucket", "B": "minecraft:barrel"},
        "riftworld:quench_barrel",
        1,
    )
    shaped(
        "tool_wall",
        ["SSS", "IPI"],
        {"S": "minecraft:stick", "I": "minecraft:iron_ingot", "P": "riftworld:sizo_planks_slab"},
        "riftworld:tool_wall",
        1,
    )
    shaped(
        "weapon_rack",
        ["SPS", "SIS", "SPS"],
        {"S": "minecraft:stick", "P": "riftworld:sizo_planks", "I": "minecraft:iron_sword"},
        "riftworld:weapon_rack",
        1,
    )
    shaped(
        "guard_weapon_rack",
        ["FDF", "FIF", "FDF"],
        {"F": "riftworld:dark_fence", "D": "riftworld:dark_planks", "I": "minecraft:iron_sword"},
        "riftworld:guard_weapon_rack",
        1,
    )
    shaped(
        "guard_armor",
        [" H ", "CCC", " A "],
        {"H": "minecraft:iron_helmet", "C": "minecraft:iron_chestplate", "A": "minecraft:armor_stand"},
        "riftworld:guard_armor",
        1,
    )
    shaped(
        "hide_rack",
        ["SSS", "SLS", "S S"],
        {"S": "minecraft:stick", "L": "minecraft:leather"},
        "riftworld:hide_rack",
        1,
    )
    shaped(
        "trophy_antlers",
        ["B B", " B ", " P "],
        {"B": "minecraft:bone", "P": "riftworld:sizo_planks"},
        "riftworld:trophy_antlers",
        1,
    )
    shaped(
        "fish_barrel",
        ["FFF", "PBP"],
        {"F": "#minecraft:fishes", "P": "riftworld:sizo_planks_slab", "B": "minecraft:barrel"},
        "riftworld:fish_barrel",
        1,
    )
    shaped(
        "fish_rack",
        ["SSS", "FFF", "S S"],
        {"S": "minecraft:stick", "F": "#minecraft:fishes"},
        "riftworld:fish_rack",
        1,
    )
    shaped(
        "fishing_net",
        ["STS", "TST", "STS"],
        {"S": "minecraft:stick", "T": "minecraft:string"},
        "riftworld:fishing_net",
        2,
    )
    shaped(
        "rowboat",
        ["P P", "SPS", "PPP"],
        {"P": "riftworld:sizo_planks", "S": "minecraft:stick"},
        "riftworld:rowboat",
        1,
    )
    shaped(
        "market_stall",
        ["WWW", "S S", "PPP"],
        {"W": "minecraft:white_wool", "S": "minecraft:stick", "P": "riftworld:sizo_planks"},
        "riftworld:market_stall",
        1,
    )
    shaped(
        "market_stall_fish",
        ["BBB", "SFS", "PPP"],
        {"B": "minecraft:blue_wool", "S": "minecraft:stick", "F": "#minecraft:fishes", "P": "riftworld:sizo_planks"},
        "riftworld:market_stall_fish",
        1,
    )
    shaped(
        "wagon_overgrown",
        ["G G", "GGG", "S V"],
        {"G": "riftworld:garden_planks", "S": "minecraft:stick", "V": "#riftworld:mod_vines"},
        "riftworld:wagon_overgrown",
        1,
    )
    shaped(
        "well",
        ["RRR", "S S", "CCC"],
        {"R": "riftworld:roof_red_slab", "S": "minecraft:stick", "C": "riftworld:elisar_cobble"},
        "riftworld:well",
        1,
    )
    shaped(
        "alchemy_table",
        [" B ", "DDD", "M M"],
        {"B": "minecraft:brewing_stand", "D": "riftworld:dark_planks", "M": "riftworld:dark_beam"},
        "riftworld:alchemy_table",
        1,
    )
    shaped(
        "alchemy_set",
        ["G A", "DDD"],
        {"G": "minecraft:glass_bottle", "A": "minecraft:amethyst_shard", "D": "riftworld:dark_planks_slab"},
        "riftworld:alchemy_set",
        1,
    )

    # =========================================================================
    # 6. SIGNS, PAINTINGS, ALTARS, LECTERNS & DUNGEON DECOR (21 BLOCKS)
    # =========================================================================
    sign_emblems = {
        "sign_shop": "minecraft:gold_nugget",
        "sign_tavern": "minecraft:glass_bottle",
        "sign_forge": "minecraft:iron_ingot",
        "sign_fish": "minecraft:cod",
        "sign_police": "minecraft:iron_bars",
        "sign_hunter": "minecraft:bow",
        "sign_market": "minecraft:wheat",
        "sign_temple": "minecraft:quartz",
    }
    for sign_id, emblem in sign_emblems.items():
        shaped(
            sign_id,
            ["NN ", "PPX"],
            {"N": "minecraft:iron_nugget", "P": "riftworld:sizo_planks_slab", "X": emblem},
            f"riftworld:{sign_id}",
            2,
        )

    shaped(
        "bounty_board",
        ["PPP", "AAA", "S S"],
        {"P": "riftworld:sizo_planks", "A": "minecraft:paper", "S": "minecraft:stick"},
        "riftworld:bounty_board",
        1,
    )
    shaped(
        "wanted_wall",
        ["N N", "AAA"],
        {"N": "minecraft:iron_nugget", "A": "minecraft:paper"},
        "riftworld:wanted_wall",
        2,
    )
    shaped(
        "painting_islands",
        ["SSS", "SPS", "SDS"],
        {"S": "minecraft:stick", "P": "minecraft:paper", "D": "minecraft:light_blue_dye"},
        "riftworld:painting_islands",
        1,
    )
    shaped(
        "painting_portrait",
        ["SSS", "SPS", "SDS"],
        {"S": "minecraft:stick", "P": "minecraft:paper", "D": "minecraft:red_dye"},
        "riftworld:painting_portrait",
        1,
    )
    shaped(
        "painting_eye",
        ["SSS", "SPS", "SDS"],
        {"S": "minecraft:stick", "P": "minecraft:paper", "D": "minecraft:purple_dye"},
        "riftworld:painting_eye",
        1,
    )
    shaped(
        "idol",
        [" S ", " G ", " S "],
        {"S": "riftworld:elisar_stone", "G": "minecraft:gold_nugget"},
        "riftworld:idol",
        1,
    )
    shaped(
        "altar_stone",
        ["LLL", " G ", "SSS"],
        {"L": "riftworld:elisar_stone_slab", "G": "minecraft:gold_ingot", "S": "riftworld:elisar_stone"},
        "riftworld:altar_stone",
        1,
    )
    shaped(
        "sacred_poles",
        ["S W", "S W", "P P"],
        {"S": "minecraft:stick", "W": "minecraft:white_wool", "P": "riftworld:sizo_planks_slab"},
        "riftworld:sacred_poles",
        2,
    )
    shaped(
        "chapel_altar",
        ["CCC", "AAA", "A A"],
        {"C": "riftworld:castle_carpet", "A": "riftworld:ashlar"},
        "riftworld:chapel_altar",
        1,
    )
    shaped(
        "chained_lectern",
        [" B ", "HHH", " D "],
        {"B": "minecraft:book", "H": "riftworld:elisar_chain", "D": "riftworld:dark_planks"},
        "riftworld:chained_lectern",
        1,
    )
    shaped(
        "chained_books",
        ["HBH"],
        {"H": "riftworld:elisar_chain", "B": "minecraft:book"},
        "riftworld:chained_books",
        2,
    )
    shaped(
        "wall_shackles",
        ["H H", "I I"],
        {"H": "riftworld:elisar_chain", "I": "minecraft:iron_ingot"},
        "riftworld:wall_shackles",
        2,
    )
    shaped(
        "bone_pile",
        [" B ", "BBB"],
        {"B": "minecraft:bone"},
        "riftworld:bone_pile",
        2,
    )


def verify_all_recipes():
    # 1) Gather all registered RiftWorld IDs
    reg_ids = set()
    reg_pat = re.compile(
        r'(?:reg|BLOCKS\.register|ITEMS\.register|cube|stairs|slab|woodPillar|strippableWood|woodFence|woodGate|woodDoor|woodTrapdoor|woodPlate|woodButton|stoneWall|egg)\(\s*"([a-z0-9_]+)"'
    )
    for jf in glob.glob(os.path.join(ROOT, "mod/src/main/java/ru/aiforge/riftworld/**/*.java"), recursive=True):
        txt = open(jf, encoding="utf-8").read()
        for m in reg_pat.finditer(txt):
            reg_ids.add("riftworld:" + m.group(1))
    mk_file = os.path.join(ROOT, "mod/src/main/java/ru/aiforge/riftworld/block/ModelKind.java")
    for m in re.finditer(r"^\s*([A-Z0-9_]+)\s*\(", open(mk_file, encoding="utf-8").read(), re.M):
        reg_ids.add("riftworld:" + m.group(1).lower())

    # 2) Build tag -> representative concrete items map from data/*/tags/items/*.json + vanilla tags
    tag_map: dict[str, set[str]] = {
        "minecraft:wool": {"minecraft:white_wool", "minecraft:red_wool", "minecraft:blue_wool"},
        "minecraft:fishes": {"minecraft:cod", "minecraft:salmon"},
        "minecraft:planks": {"minecraft:oak_planks"},
        "minecraft:logs": {"minecraft:oak_log"},
        "minecraft:stone_crafting_materials": {"minecraft:cobblestone", "minecraft:blackstone", "minecraft:cobbled_deepslate"},
        "minecraft:stone_tool_materials": {"minecraft:cobblestone", "minecraft:blackstone", "minecraft:cobbled_deepslate"},
        "minecraft:coals": {"minecraft:coal", "minecraft:charcoal"},
    }
    for tf in glob.glob(os.path.join(DATA, "*/*/items/*.json")) + glob.glob(os.path.join(DATA, "*/tags/items/*.json")):
        parts = tf.split(os.sep)
        if "tags" not in parts:
            continue
        ti = parts.index("tags")
        ns = parts[ti - 1]
        tname = os.path.basename(tf)[:-5]
        vals = json.load(open(tf, encoding="utf-8")).get("values", [])
        s = tag_map.setdefault(f"{ns}:{tname}", set())
        for v in vals:
            if isinstance(v, str) and not v.startswith("#"):
                s.add(v)
    # Propagate mod tags into vanilla tags where merged
    for _ in range(3):
        for tf in glob.glob(os.path.join(DATA, "*/tags/items/*.json")):
            parts = tf.split(os.sep)
            ti = parts.index("tags")
            ns = parts[ti - 1]
            tname = os.path.basename(tf)[:-5]
            vals = json.load(open(tf, encoding="utf-8")).get("values", [])
            for v in vals:
                if isinstance(v, str) and v.startswith("#"):
                    tag_map.setdefault(f"{ns}:{tname}", set()).update(tag_map.get(v[1:], set()))

    def expand_ing(obj) -> set[str]:
        if isinstance(obj, list):
            out = set()
            for x in obj:
                out.update(expand_ing(x))
            return out
        if isinstance(obj, dict):
            if "item" in obj:
                return {obj["item"]}
            if "tag" in obj:
                t = obj["tag"]
                return set(tag_map.get(t, {f"#{t}"}))
        return set()

    def trim_grid(rows: list[str]) -> tuple[str, ...]:
        grid = [list(r) for r in rows]
        while grid and all(c == " " for c in grid[0]):
            grid.pop(0)
        while grid and all(c == " " for c in grid[-1]):
            grid.pop()
        if not grid:
            return ()
        w = max(len(r) for r in grid)
        for r in grid:
            while len(r) < w:
                r.append(" ")
        while grid and all(r[0] == " " for r in grid):
            for r in grid:
                r.pop(0)
        while grid and grid[0] and all(r[-1] == " " for r in grid):
            for r in grid:
                r.pop()
        return tuple("".join(r) for r in grid)

    forbidden_outputs = {f"riftworld:{b}" for b in (LAB_BLOCKS | RARE_OR_RAW_ORE_BLOCKS)}
    forbidden_lab_ings = {f"riftworld:{b}" for b in LAB_BLOCKS}
    recipe_files = sorted(glob.glob(os.path.join(RECIPES_DIR, "*.json")))

    shaped_list = []
    shapeless_list = []
    smelt_map = {}

    for rf in recipe_files:
        rname = os.path.basename(rf)[:-5]
        d = json.load(open(rf, encoding="utf-8"))
        rtype = d["type"]

        # Validate IDs & forbidden exclusions
        res = d.get("result")
        rid = res["item"] if isinstance(res, dict) else res
        assert rid not in forbidden_outputs, f"FORBIDDEN lab/rare recipe output {rid} in {rname}"
        if rid.startswith("riftworld:"):
            assert rid in reg_ids, f"Unknown result ID {rid} in {rname}"

        for obj in (
            ([d["ingredient"]] if "ingredient" in d else [])
            + d.get("ingredients", [])
            + list(d.get("key", {}).values())
        ):
            items_in = expand_ing(obj)
            for it in items_in:
                if it.startswith("riftworld:"):
                    assert it in reg_ids, f"Unknown ingredient ID {it} in {rname}"
                assert it not in forbidden_lab_ings, f"Forbidden lab ingredient {it} in {rname}"

        if rtype == "minecraft:crafting_shaped":
            pat = trim_grid(d["pattern"])
            mir = tuple(r[::-1] for r in pat)
            kmap = {k: expand_ing(v) for k, v in d["key"].items()}
            shaped_list.append((rname, rid, pat, mir, kmap))
        elif rtype == "minecraft:crafting_shapeless":
            ings = [expand_ing(x) for x in d["ingredients"]]
            shapeless_list.append((rname, rid, ings))
        elif rtype in ("minecraft:smelting", "minecraft:blasting"):
            for it in expand_ing(d["ingredient"]):
                key = (rtype, it)
                if key in smelt_map and smelt_map[key][1] != rid:
                    raise AssertionError(f"Smelting collision on {key}: {smelt_map[key]} vs {(rname, rid)}")
                smelt_map[key] = (rname, rid)

    # Check shaped-vs-shaped collisions (including horizontal mirror!)
    def grids_can_collide(p1, k1, p2, k2) -> bool:
        if len(p1) != len(p2) or (len(p1) > 0 and len(p1[0]) != len(p2[0])):
            return False
        for y in range(len(p1)):
            for x in range(len(p1[0])):
                c1, c2 = p1[y][x], p2[y][x]
                if (c1 == " ") != (c2 == " "):
                    return False
                if c1 != " " and not (k1[c1] & k2[c2]):
                    return False
        return True

    for i in range(len(shaped_list)):
        n1, r1, p1, m1, k1 = shaped_list[i]
        for j in range(i + 1, len(shaped_list)):
            n2, r2, p2, m2, k2 = shaped_list[j]
            if r1 == r2:
                continue
            if grids_can_collide(p1, k1, p2, k2) or grids_can_collide(p1, k1, m2, k2):
                raise AssertionError(f"SHAPED COLLISION: {n1} ({r1}) vs {n2} ({r2})")

    # Check shapeless-vs-shapeless & 1-slot/multi-slot shapeless-vs-shaped collisions
    def multisets_can_collide(ings1: list[set[str]], ings2: list[set[str]]) -> bool:
        if len(ings1) != len(ings2):
            return False
        # Backtracking bipartite matching of ingredient slots
        used = [False] * len(ings2)

        def dfs(idx: int) -> bool:
            if idx == len(ings1):
                return True
            for j in range(len(ings2)):
                if not used[j] and (ings1[idx] & ings2[j]):
                    used[j] = True
                    if dfs(idx + 1):
                        return True
                    used[j] = False
            return False

        return dfs(0)

    for i in range(len(shapeless_list)):
        n1, r1, ings1 = shapeless_list[i]
        for j in range(i + 1, len(shapeless_list)):
            n2, r2, ings2 = shapeless_list[j]
            if r1 == r2:
                continue
            if multisets_can_collide(ings1, ings2):
                raise AssertionError(f"SHAPELESS COLLISION: {n1} ({r1}) vs {n2} ({r2})")
        for n2, r2, p2, _, k2 in shaped_list:
            if r1 == r2:
                continue
            non_empty = [k2[c] for row in p2 for c in row if c != " "]
            if multisets_can_collide(ings1, non_empty):
                raise AssertionError(f"SHAPELESS-SHAPED COLLISION: {n1} ({r1}) vs {n2} ({r2})")

    # Check against core vanilla shaped recipes that use sticks/planks/iron/cobble/bones/string/paper
    vanilla_shaped = [
        ("minecraft:stick", ("#", "#"), {"#": tag_map["minecraft:planks"]}),
        ("minecraft:crafting_table", ("##", "##"), {"#": tag_map["minecraft:planks"]}),
        ("minecraft:chest", ("###", "# #", "###"), {"#": tag_map["minecraft:planks"]}),
        ("minecraft:bowl", ("# #", " # "), {"#": tag_map["minecraft:planks"]}),
        ("minecraft:ladder", ("# #", "###", "# #"), {"#": {"minecraft:stick"}}),
        ("minecraft:painting", ("###", "#X#", "###"), {"#": {"minecraft:stick"}, "X": tag_map["minecraft:wool"]}),
        ("minecraft:item_frame", ("###", "#X#", "###"), {"#": {"minecraft:stick"}, "X": {"minecraft:leather"}}),
        ("minecraft:armor_stand", ("###", " # ", "#S#"), {"#": {"minecraft:stick"}, "S": {"minecraft:smooth_stone_slab"}}),
        ("minecraft:white_wool", ("##", "##"), {"#": {"minecraft:string"}}),
        ("minecraft:paper", ("###",), {"#": {"minecraft:sugar_cane"}}),
        ("minecraft:cauldron", ("# #", "# #", "###"), {"#": {"minecraft:iron_ingot"}}),
        ("minecraft:bucket", ("# #", " # "), {"#": {"minecraft:iron_ingot"}}),
        ("minecraft:iron_bars", ("###", "###"), {"#": {"minecraft:iron_ingot"}}),
        ("minecraft:chain", ("N", "I", "N"), {"N": {"minecraft:iron_nugget"}, "I": {"minecraft:iron_ingot"}}),
        ("minecraft:bone_block", ("###", "###", "###"), {"#": {"minecraft:bone_meal"}}),
    ]
    for n1, r1, p1, _, k1 in shaped_list:
        for vn, vp, vk in vanilla_shaped:
            if r1 == vn:
                continue
            if grids_can_collide(p1, k1, vp, vk) or grids_can_collide(p1, k1, tuple(x[::-1] for x in vp), vk):
                raise AssertionError(f"VANILLA COLLISION: {n1} ({r1}) shadows {vn}")

    # Verify coin ores still smelt & blast into elisar_ducat!
    for ore in ("riftworld:ducat_ore", "riftworld:deepslate_ducat_ore", "riftworld:aurite_ore"):
        assert smelt_map.get(("minecraft:smelting", ore), (None, None))[1] == "riftworld:elisar_ducat"
        assert smelt_map.get(("minecraft:blasting", ore), (None, None))[1] == "riftworld:elisar_ducat"

    # Coverage summary over all 419 WITH_ITEM blocks
    blockstates = {os.path.basename(p)[:-5] for p in glob.glob(os.path.join(RES, "assets/riftworld/blockstates/*.json"))}
    item_models = {os.path.basename(p)[:-5] for p in glob.glob(os.path.join(RES, "assets/riftworld/models/item/*.json"))}
    block_items = sorted(blockstates & item_models)

    crafted_blocks = set()
    for rf in recipe_files:
        d = json.load(open(rf, encoding="utf-8"))
        res = d.get("result")
        rid = res["item"] if isinstance(res, dict) else res
        if rid.startswith("riftworld:"):
            b = rid.split(":", 1)[1]
            if b in block_items:
                crafted_blocks.add(b)

    lab_crafted = crafted_blocks & LAB_BLOCKS
    rare_crafted = crafted_blocks & RARE_OR_RAW_ORE_BLOCKS
    assert not lab_crafted, f"Lab blocks have recipes: {lab_crafted}"
    assert not rare_crafted, f"Rare blocks have recipes: {rare_crafted}"

    uncrafted_non_lab_non_rare = sorted(set(block_items) - LAB_BLOCKS - RARE_OR_RAW_ORE_BLOCKS - crafted_blocks)
    print(
        f"VERIFIED v0.30.0 RECIPES: total_recipe_files={len(recipe_files)} | "
        f"craftable_mod_blocks={len(crafted_blocks)}/{len(set(block_items) - LAB_BLOCKS - RARE_OR_RAW_ORE_BLOCKS)} "
        f"| lab_recipes={len(lab_crafted)} | rare_recipes={len(rare_crafted)} | collisions=0"
    )
    print(f"Remaining natural-only flora/logs/leaves ({len(uncrafted_non_lab_non_rare)}): {uncrafted_non_lab_non_rare}")


if __name__ == "__main__":
    removed = remove_forbidden_recipes()
    print(f"Removed {len(removed)} forbidden lab/rare recipe files: {removed}")
    generate_v030_block_recipes()
    verify_all_recipes()
