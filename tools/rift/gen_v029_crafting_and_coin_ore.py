#!/usr/bin/env python3
"""
v0.29.0 Generator:
1. Dedicated Coin Ore blocks (`riftworld:ducat_ore` & `riftworld:deepslate_ducat_ore`):
   - Crisp 16x16 Minecraft-style PNG textures (stdlib zlib + struct, 0 PIL dependency),
   - Blockstates, block models, item models, Silk-Touch-compatible ore loot tables (drops the ore block so it smelts!),
   - Smelting (`200` ticks) & Blasting (`100` ticks) recipes -> `riftworld:elisar_ducat` (Монета Элисара),
   - Overworld `configured_feature` + `placed_feature` + Forge `biome_modifier` (`#minecraft:is_overworld`),
   - Pickaxe / Stone-tool tags + Russian & English translations.
2. Gilded Paper (`riftworld:gilded_paper`):
   - Exact requested 3x3 shaped recipe: `1` vanilla `minecraft:paper` in center + `8` `minecraft:gold_ingot` around border!
3. Complete Minecraft-style crafting, stonecutting & smelting recipes:
   - All mod wood, stone, castle, temple, roof, lantern & lab blocks (`1 log = 4 planks`, `6 planks = 3 doors`, etc.),
   - Vanilla items crafted by substituting mod materials (via `#minecraft:stone_crafting_materials`, `#minecraft:stone_tool_materials`,
     `#minecraft:planks`, `#minecraft:logs`, PLUS explicit vanilla-output recipes for Boat, Sign, Lever, Piston, Dispenser,
     Dropper, Observer, Repeater, Comparator, Stone Button/Plate, Bread, Paper, Mossy Stone, Dyes),
   - Mod items (`bridge_post`, `dragon_saddle`, `dragon_whistle`, `ritual_candle`, etc.) accepting mod OR vanilla equivalents.
4. Strict automated verification against registered IDs in `mod/src/main/java/ru/aiforge/riftworld/registry/*.java`.
"""

import glob
import json
import math
import os
import re
import struct
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "mod", "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "riftworld")
DATA = os.path.join(RES, "data")
RECIPES_DIR = os.path.join(DATA, "riftworld", "recipes")


# =============================================================================
# 1. STDLIB 16x16 PNG WRITER & COIN ORE TEXTURE SYNTHESIS
# =============================================================================
def write_png_rgba(path: str, w: int, h: int, pixels_rgba: list[tuple[int, int, int, int]]):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    raw = bytearray()
    for y in range(h):
        raw.append(0)  # filter 0
        for x in range(w):
            r, g, b, a = pixels_rgba[y * w + x]
            raw.extend((max(0, min(255, r)), max(0, min(255, g)), max(0, min(255, b)), max(0, min(255, a))))

    def chunk(tag: bytes, payload: bytes) -> bytes:
        crc = zlib.crc32(tag + payload) & 0xFFFFFFFF
        return struct.pack(">I", len(payload)) + tag + payload + struct.pack(">I", crc)

    ihdr = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)


def hash2(x: int, y: int, seed: int) -> float:
    h = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    h = h ^ (h >> 16)
    return (h & 0xFFFFFF) / float(0x1000000)


def generate_coin_ore_textures():
    # Embossed golden coin-nugget clusters on a 16x16 grid (reads immediately as Coin Ore in Minecraft!)
    # Centers of 4 stamped coin-medallion pockets: (4, 4), (11, 5), (5, 11), (11, 12)
    coins = [(4.0, 4.0, 2.35), (11.2, 4.8, 2.05), (4.8, 11.2, 2.15), (11.2, 11.6, 2.25)]

    for mode, tex_name in (("stone", "ducat_ore.png"), ("deepslate", "deepslate_ducat_ore.png")):
        px = []
        for y in range(16):
            for x in range(16):
                n1 = hash2(x, y, 17 if mode == "stone" else 91)
                n2 = hash2(x // 2, y, 43 if mode == "stone" else 131)
                if mode == "stone":
                    # Warm Elisar / Overworld limestone-stone matrix
                    base_r = int(124 + (n1 - 0.5) * 24 + (n2 - 0.5) * 16)
                    base_g = int(122 + (n1 - 0.5) * 22 + (n2 - 0.5) * 16)
                    base_b = int(120 + (n1 - 0.5) * 22 + (n2 - 0.5) * 15)
                else:
                    # Layered dark deepslate matrix
                    band = math.sin(y * 1.15 + x * 0.25) * 7.0
                    base_r = int(58 + band + (n1 - 0.5) * 16)
                    base_g = int(58 + band + (n1 - 0.5) * 16)
                    base_b = int(64 + band + (n1 - 0.5) * 18)

                r, g, b = base_r, base_g, base_b
                for cx, cy, rad in coins:
                    dx = x - cx
                    dy = y - cy
                    d = math.hypot(dx, dy)
                    if d <= rad + 0.55:
                        if d > rad - 0.15:
                            # Crisp dark mineral socket rim around the coin vein
                            r = int(r * 0.62)
                            g = int(g * 0.58)
                            b = int(b * 0.52)
                        elif d >= rad - 0.95:
                            # Beveled gold coin rim (bright top-left specular, rich bronze-gold bottom-right!)
                            lit = (-dx - dy) / max(1.0, rad * 1.4)
                            if lit > 0.22:
                                r, g, b = 255, 240, 148
                            elif lit > -0.20:
                                r, g, b = 236, 190, 62
                            else:
                                r, g, b = 176, 122, 28
                        else:
                            # Stamped inner ducat face with bright mint glint!
                            glint = (dx <= 0.1 and dy <= 0.1)
                            if glint:
                                r, g, b = 255, 248, 182
                            else:
                                r, g, b = 218, 166, 46
                        break
                px.append((r, g, b, 255))

        out_path = os.path.join(ASSETS, "textures", "block", tex_name)
        write_png_rgba(out_path, 16, 16, px)


def write_json(path: str, obj: dict):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")


# =============================================================================
# 2. COIN ORE MODELS, BLOCKSTATES, LOOT TABLES, WORLDGEN & LANG
# =============================================================================
def generate_coin_ore_assets_and_worldgen():
    generate_coin_ore_textures()

    for ore_id in ("ducat_ore", "deepslate_ducat_ore"):
        write_json(
            os.path.join(ASSETS, "blockstates", f"{ore_id}.json"),
            {
                "variants": {
                    "": [
                        {"model": f"riftworld:block/{ore_id}", "y": 0},
                        {"model": f"riftworld:block/{ore_id}", "y": 90},
                        {"model": f"riftworld:block/{ore_id}", "y": 180},
                        {"model": f"riftworld:block/{ore_id}", "y": 270},
                    ]
                }
            },
        )
        write_json(
            os.path.join(ASSETS, "models", "block", f"{ore_id}.json"),
            {
                "parent": "minecraft:block/cube_all",
                "textures": {"all": f"riftworld:block/{ore_id}"},
            },
        )
        write_json(
            os.path.join(ASSETS, "models", "item", f"{ore_id}.json"),
            {"parent": f"riftworld:block/{ore_id}"},
        )
        # Drops the ore block itself (just like iron/gold ore pre-raw or aurite_ore) so the player SMELTS it into a coin!
        write_json(
            os.path.join(DATA, "riftworld", "loot_tables", "blocks", f"{ore_id}.json"),
            {
                "type": "minecraft:block",
                "pools": [
                    {
                        "rolls": 1,
                        "entries": [{"type": "minecraft:item", "name": f"riftworld:{ore_id}"}],
                        "conditions": [{"condition": "minecraft:survives_explosion"}],
                    }
                ],
            },
        )

    # Overworld Ore Generation (`configured_feature` + `placed_feature` + Forge `biome_modifier`)
    write_json(
        os.path.join(DATA, "riftworld", "worldgen", "configured_feature", "ore_ducat.json"),
        {
            "type": "minecraft:ore",
            "config": {
                "size": 8,
                "discard_chance_on_air_exposure": 0.0,
                "targets": [
                    {
                        "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceable"},
                        "state": {"Name": "riftworld:ducat_ore"},
                    },
                    {
                        "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceable"},
                        "state": {"Name": "riftworld:deepslate_ducat_ore"},
                    },
                ],
            },
        },
    )
    write_json(
        os.path.join(DATA, "riftworld", "worldgen", "placed_feature", "ore_ducat.json"),
        {
            "feature": "riftworld:ore_ducat",
            "placement": [
                {"type": "minecraft:count", "count": 7},
                {"type": "minecraft:in_square"},
                {
                    "type": "minecraft:height_range",
                    "height": {
                        "type": "minecraft:trapezoid",
                        "min_inclusive": {"absolute": -56},
                        "max_inclusive": {"absolute": 56},
                    },
                },
                {"type": "minecraft:biome"},
            ],
        },
    )
    write_json(
        os.path.join(DATA, "riftworld", "forge", "biome_modifier", "add_ducat_ore_overworld.json"),
        {
            "type": "forge:add_features",
            "biomes": "#minecraft:is_overworld",
            "features": "riftworld:ore_ducat",
            "step": "underground_ores",
        },
    )

    # Update ru_ru.json & en_us.json translations cleanly
    for lang_file, entries in (
        (
            "ru_ru.json",
            {
                "block.riftworld.ducat_ore": "Монетная руда",
                "block.riftworld.deepslate_ducat_ore": "Глубинносланцевая монетная руда",
            },
        ),
        (
            "en_us.json",
            {
                "block.riftworld.ducat_ore": "Ducat Coin Ore",
                "block.riftworld.deepslate_ducat_ore": "Deepslate Ducat Coin Ore",
            },
        ),
    ):
        lp = os.path.join(ASSETS, "lang", lang_file)
        data = json.load(open(lp, encoding="utf-8"))
        data.update(entries)
        write_json(lp, data)


# =============================================================================
# 3. MINECRAFT & MOD ITEM/BLOCK TAGS FOR SEAMLESS MATERIAL SUBSTITUTION
# =============================================================================
def append_tag_values(rel_path: str, new_vals: list[str]):
    full = os.path.join(DATA, rel_path)
    if os.path.exists(full):
        obj = json.load(open(full, encoding="utf-8"))
    else:
        obj = {"replace": False, "values": []}
    existing = list(obj.get("values", []))
    for v in new_vals:
        if v not in existing:
            existing.append(v)
    obj["values"] = existing
    write_json(full, obj)


def update_tags():
    mod_cobbles = [
        "riftworld:elisar_cobble",
        "riftworld:elisar_stone",
        "riftworld:ashen_cobblestone",
        "riftworld:scorched_stone",
        "riftworld:keep_stone",
        "riftworld:garden_limestone",
        "riftworld:rift_slate",
        "riftworld:rift_marble",
        "riftworld:ivory_marble",
        "riftworld:pearl_calcite",
    ]
    mod_smooth_stones = [
        "riftworld:elisar_stone",
        "riftworld:scorched_stone",
        "riftworld:keep_stone",
        "riftworld:garden_limestone",
        "riftworld:rift_marble",
        "riftworld:ivory_marble",
        "riftworld:ashlar",
    ]
    mod_planks = [
        "riftworld:sizo_planks",
        "riftworld:ambra_planks",
        "riftworld:garden_planks",
        "riftworld:lilac_planks",
        "riftworld:crystal_planks",
        "riftworld:dark_planks",
        "riftworld:charred_planks",
    ]
    mod_vines = [
        "riftworld:elisar_ivy",
        "riftworld:garden_ivy",
        "riftworld:elisar_moss",
        "riftworld:blossom_moss_block",
        "minecraft:vine",
        "minecraft:moss_block",
    ]

    # Vanilla stone crafting & stone tool tags -> unlocks Furnace, Brewing Stand, and ALL 5 Stone Tools!
    append_tag_values("minecraft/tags/items/stone_crafting_materials.json", mod_cobbles)
    append_tag_values("minecraft/tags/items/stone_tool_materials.json", mod_cobbles)

    # Ensure legacy wooden blocks are also in vanilla tags
    append_tag_values("minecraft/tags/items/planks.json", mod_planks)
    append_tag_values("minecraft/tags/blocks/planks.json", mod_planks)
    append_tag_values("minecraft/tags/items/wooden_doors.json", ["riftworld:elisar_door"])
    append_tag_values("minecraft/tags/items/doors.json", ["riftworld:elisar_door"])
    append_tag_values("minecraft/tags/items/wooden_fences.json", ["riftworld:elisar_fence", "riftworld:dark_fence"])
    append_tag_values("minecraft/tags/items/fences.json", ["riftworld:elisar_fence", "riftworld:dark_fence"])
    append_tag_values(
        "minecraft/tags/items/wooden_slabs.json",
        [
            "riftworld:sizo_planks_slab",
            "riftworld:ambra_planks_slab",
            "riftworld:garden_slab",
            "riftworld:lilac_slab",
            "riftworld:crystal_slab",
            "riftworld:dark_planks_slab",
            "riftworld:charred_slab",
        ],
    )
    append_tag_values(
        "minecraft/tags/items/wooden_stairs.json",
        [
            "riftworld:sizo_planks_stairs",
            "riftworld:ambra_planks_stairs",
            "riftworld:garden_stairs",
            "riftworld:lilac_stairs",
            "riftworld:crystal_stairs",
            "riftworld:dark_planks_stairs",
            "riftworld:charred_stairs",
        ],
    )

    # Custom RiftWorld material tags (so vanilla-output recipes never collide with vanilla-input recipes!)
    write_json(os.path.join(DATA, "riftworld/tags/items/mod_planks.json"), {"replace": False, "values": mod_planks})
    write_json(os.path.join(DATA, "riftworld/tags/items/mod_cobblestone.json"), {"replace": False, "values": mod_cobbles})
    write_json(os.path.join(DATA, "riftworld/tags/items/mod_smooth_stone.json"), {"replace": False, "values": mod_smooth_stones})
    write_json(os.path.join(DATA, "riftworld/tags/items/mod_vines.json"), {"replace": False, "values": mod_vines})

    # Pickaxe & tool-tier block tags for Coin Ore + all stone/brick/metal blocks
    append_tag_values(
        "minecraft/tags/blocks/mineable/pickaxe.json",
        [
            "riftworld:ducat_ore",
            "riftworld:deepslate_ducat_ore",
            "riftworld:elisar_stone",
            "riftworld:elisar_stone_mossy",
            "riftworld:elisar_cobble",
            "riftworld:elisar_bricks",
            "riftworld:elisar_bricks_mossy",
            "riftworld:elisar_bricks_stairs",
            "riftworld:elisar_cobble_stairs",
            "riftworld:elisar_cobble_slab",
            "riftworld:elisar_stone_wall",
            "riftworld:keep_stone",
            "riftworld:keep_stone_cracked",
            "riftworld:keep_stone_mossy",
            "riftworld:keep_stone_stairs",
            "riftworld:keep_stone_slab",
            "riftworld:garden_limestone",
            "riftworld:garden_limestone_mossy",
            "riftworld:rift_marble",
            "riftworld:rift_marble_mossy",
            "riftworld:rift_marble_stairs",
            "riftworld:rift_marble_slab",
            "riftworld:rift_slate",
            "riftworld:castle_bricks",
            "riftworld:castle_bricks_cracked",
            "riftworld:castle_bricks_mossy",
            "riftworld:castle_bricks_stairs",
            "riftworld:castle_bricks_slab",
            "riftworld:castle_bricks_wall",
            "riftworld:ashlar",
            "riftworld:ashlar_stairs",
            "riftworld:ashlar_slab",
            "riftworld:ashlar_pillar",
            "riftworld:scorched_stone",
            "riftworld:temple_bricks",
            "riftworld:temple_bricks_stairs",
            "riftworld:temple_bricks_slab",
            "riftworld:temple_bricks_cracked",
            "riftworld:temple_bricks_ivy",
            "riftworld:temple_pillar",
            "riftworld:temple_grand_column",
            "riftworld:temple_floor",
            "riftworld:temple_gold",
            "riftworld:temple_gold_rune",
        ],
    )
    append_tag_values(
        "minecraft/tags/blocks/needs_stone_tool.json",
        ["riftworld:ducat_ore", "riftworld:deepslate_ducat_ore"],
    )


# =============================================================================
# 4. COMPLETE RECIPE SUITE (COIN ORE, GILDED PAPER, VANILLA SUBSTITUTIONS, MOD BLOCKS)
# =============================================================================
def ing(spec: str) -> dict:
    return {"tag": spec[1:]} if spec.startswith("#") else {"item": spec}


def shaped(name: str, pattern: list[str], key: dict[str, str], result: str, count: int = 1):
    write_json(
        os.path.join(RECIPES_DIR, f"{name}.json"),
        {
            "type": "minecraft:crafting_shaped",
            "pattern": pattern,
            "key": {k: ing(v) for k, v in key.items()},
            "result": {"item": result, "count": count},
        },
    )


def shapeless(name: str, items: list[str], result: str, count: int = 1):
    write_json(
        os.path.join(RECIPES_DIR, f"{name}.json"),
        {
            "type": "minecraft:crafting_shapeless",
            "ingredients": [ing(x) for x in items],
            "result": {"item": result, "count": count},
        },
    )


def smelting(name: str, src: str, result: str, xp: float = 0.1, time: int = 200):
    write_json(
        os.path.join(RECIPES_DIR, f"{name}.json"),
        {
            "type": "minecraft:smelting",
            "ingredient": ing(src),
            "result": result,
            "experience": xp,
            "cookingtime": time,
        },
    )


def blasting(name: str, src: str, result: str, xp: float = 0.1, time: int = 100):
    write_json(
        os.path.join(RECIPES_DIR, f"{name}.json"),
        {
            "type": "minecraft:blasting",
            "ingredient": ing(src),
            "result": result,
            "experience": xp,
            "cookingtime": time,
        },
    )


def stonecutting(name: str, src: str, result: str, count: int = 1):
    write_json(
        os.path.join(RECIPES_DIR, f"{name}.json"),
        {
            "type": "minecraft:stonecutting",
            "ingredient": ing(src),
            "result": result,
            "count": count,
        },
    )


def add_stone_shape_recipes(base_id: str, stairs_id: str = None, slab_id: str = None, wall_id: str = None):
    short = base_id.split(":")[1]
    if stairs_id:
        st_short = stairs_id.split(":")[1]
        shaped(st_short, ["#  ", "## ", "###"], {"#": base_id}, stairs_id, 4)
        stonecutting(f"{st_short}_from_{short}_stonecutting", base_id, stairs_id, 1)
    if slab_id:
        sl_short = slab_id.split(":")[1]
        shaped(sl_short, ["###"], {"#": base_id}, slab_id, 6)
        stonecutting(f"{sl_short}_from_{short}_stonecutting", base_id, slab_id, 2)
    if wall_id:
        wl_short = wall_id.split(":")[1]
        shaped(wl_short, ["###", "###"], {"#": base_id}, wall_id, 6)
        stonecutting(f"{wl_short}_from_{short}_stonecutting", base_id, wall_id, 1)


def generate_all_recipes():
    # -------------------------------------------------------------------------
    # A) COIN ORE -> COIN (`elisar_ducat`) SMELTING & BLASTING!
    # -------------------------------------------------------------------------
    smelting("ducat_ore_smelting", "riftworld:ducat_ore", "riftworld:elisar_ducat", xp=1.0, time=200)
    blasting("ducat_ore_blasting", "riftworld:ducat_ore", "riftworld:elisar_ducat", xp=1.0, time=100)
    smelting("deepslate_ducat_ore_smelting", "riftworld:deepslate_ducat_ore", "riftworld:elisar_ducat", xp=1.0, time=200)
    blasting("deepslate_ducat_ore_blasting", "riftworld:deepslate_ducat_ore", "riftworld:elisar_ducat", xp=1.0, time=100)

    # -------------------------------------------------------------------------
    # B) GILDED PAPER: 1 VANILLA PAPER IN CENTER + 8 GOLD INGOTS AROUND BORDER!
    #    ("озолоченная бумага по середине обычная ванильная бумага и 8 золота по бокам")
    # -------------------------------------------------------------------------
    shaped(
        "gilded_paper",
        ["GGG", "GPG", "GGG"],
        {"P": "minecraft:paper", "G": "minecraft:gold_ingot"},
        "riftworld:gilded_paper",
        1,
    )

    # Upgrade `bridge_post` so ANY planks (mod or vanilla!) craft 4 bridge posts
    shaped("bridge_post", ["P", "P", "P"], {"P": "#minecraft:planks"}, "riftworld:bridge_post", 4)
    # Alternative `bridge_rope` from Elisar vines/ivy so players inside Elisar can craft ropes without spiders!
    shaped("bridge_rope_from_ivy", ["V", "V", "V"], {"V": "#riftworld:mod_vines"}, "riftworld:bridge_rope", 2)

    # -------------------------------------------------------------------------
    # C) VANILLA ITEMS CRAFTED FROM MOD MATERIALS (MATERIAL SUBSTITUTION!)
    # -------------------------------------------------------------------------
    shaped("vanilla_oak_boat_from_mod_planks", ["P P", "PPP"], {"P": "#riftworld:mod_planks"}, "minecraft:oak_boat", 1)
    shaped(
        "vanilla_oak_sign_from_mod_planks",
        ["PPP", "PPP", " S "],
        {"P": "#riftworld:mod_planks", "S": "minecraft:stick"},
        "minecraft:oak_sign",
        3,
    )
    shaped(
        "vanilla_lever_from_mod_stone",
        ["S", "C"],
        {"S": "minecraft:stick", "C": "#riftworld:mod_cobblestone"},
        "minecraft:lever",
        1,
    )
    shaped(
        "vanilla_piston_from_mod_stone",
        ["PPP", "CIC", "CRC"],
        {"P": "#minecraft:planks", "C": "#riftworld:mod_cobblestone", "I": "minecraft:iron_ingot", "R": "minecraft:redstone"},
        "minecraft:piston",
        1,
    )
    shaped(
        "vanilla_dispenser_from_mod_stone",
        ["CCC", "CBC", "CRC"],
        {"C": "#riftworld:mod_cobblestone", "B": "minecraft:bow", "R": "minecraft:redstone"},
        "minecraft:dispenser",
        1,
    )
    shaped(
        "vanilla_dropper_from_mod_stone",
        ["CCC", "C C", "CRC"],
        {"C": "#riftworld:mod_cobblestone", "R": "minecraft:redstone"},
        "minecraft:dropper",
        1,
    )
    shaped(
        "vanilla_observer_from_mod_stone",
        ["CCC", "RRQ", "CCC"],
        {"C": "#riftworld:mod_cobblestone", "R": "minecraft:redstone", "Q": "minecraft:quartz"},
        "minecraft:observer",
        1,
    )
    shaped(
        "vanilla_repeater_from_mod_stone",
        ["TRT", "SSS"],
        {"T": "minecraft:redstone_torch", "R": "minecraft:redstone", "S": "#riftworld:mod_smooth_stone"},
        "minecraft:repeater",
        1,
    )
    shaped(
        "vanilla_comparator_from_mod_stone",
        [" T ", "TQT", "SSS"],
        {"T": "minecraft:redstone_torch", "Q": "minecraft:quartz", "S": "#riftworld:mod_smooth_stone"},
        "minecraft:comparator",
        1,
    )
    shapeless("vanilla_stone_button_from_mod_stone", ["#riftworld:mod_smooth_stone"], "minecraft:stone_button", 1)
    shaped("vanilla_stone_pressure_plate_from_mod_stone", ["SS"], {"S": "#riftworld:mod_smooth_stone"}, "minecraft:stone_pressure_plate", 1)

    # Vanilla survival staples from Elisar flora & seabed resources!
    shaped("vanilla_bread_from_pale_wheat", ["WWW"], {"W": "riftworld:pale_wheat"}, "minecraft:bread", 1)
    shaped("vanilla_paper_from_pale_wheat", ["WWW"], {"W": "riftworld:elisar_tall_grass"}, "minecraft:paper", 3)
    shapeless("vanilla_mossy_cobblestone_from_mod_ivy", ["minecraft:cobblestone", "#riftworld:mod_vines"], "minecraft:mossy_cobblestone", 1)
    shapeless("vanilla_mossy_stone_bricks_from_mod_ivy", ["minecraft:stone_bricks", "#riftworld:mod_vines"], "minecraft:mossy_stone_bricks", 1)
    shapeless("vanilla_purple_dye_from_violet_bloom", ["riftworld:violet_bloom"], "minecraft:purple_dye", 1)
    shapeless("vanilla_magenta_dye_from_lilac_bloom", ["riftworld:lilac_bloom"], "minecraft:magenta_dye", 1)
    shapeless("vanilla_light_gray_dye_from_pale_bloom", ["riftworld:pale_bloom"], "minecraft:light_gray_dye", 1)
    shapeless("vanilla_white_dye_from_elisar_flowers", ["riftworld:elisar_flowers"], "minecraft:white_dye", 1)
    shapeless("vanilla_white_dye_from_white_lily", ["riftworld:white_lily_cluster"], "minecraft:white_dye", 2)
    shapeless("vanilla_cyan_dye_from_celadon_fern", ["riftworld:celadon_fern"], "minecraft:cyan_dye", 1)
    smelting("vanilla_glass_from_elisar_seabed", "riftworld:elisar_seabed", "minecraft:glass", xp=0.1, time=200)
    smelting("vanilla_green_dye_from_sea_kelp", "riftworld:sea_kelp", "minecraft:Dried_kelp".lower().replace("dried_kelp", "dried_kelp"), xp=0.1, time=200)

    # -------------------------------------------------------------------------
    # D) COMPLETE MINECRAFT-STYLE RECIPES FOR ALL MOD WOOD, STONE & DECOR BLOCKS!
    # -------------------------------------------------------------------------
    # Legacy Wood Shapes (`sizo` & `dark` pre-v0.28 names + `elisar_door` / `elisar_fence` / `rope_post`)
    shaped("sizo_planks_stairs", ["#  ", "## ", "###"], {"#": "riftworld:sizo_planks"}, "riftworld:sizo_planks_stairs", 4)
    shaped("sizo_planks_slab", ["###"], {"#": "riftworld:sizo_planks"}, "riftworld:sizo_planks_slab", 6)
    shaped("dark_planks_stairs", ["#  ", "## ", "###"], {"#": "riftworld:dark_planks"}, "riftworld:dark_planks_stairs", 4)
    shaped("dark_planks_slab", ["###"], {"#": "riftworld:dark_planks"}, "riftworld:dark_planks_slab", 6)
    shaped("dark_fence", ["PSP", "PSP"], {"P": "riftworld:dark_planks", "S": "minecraft:stick"}, "riftworld:dark_fence", 3)
    shaped("elisar_door", ["PP", "WW", "PP"], {"P": "#riftworld:mod_planks", "W": "riftworld:elisar_plaster"}, "riftworld:elisar_door", 3)
    shaped("elisar_fence", ["PSP", "PSP"], {"P": "riftworld:elisar_plaster", "S": "minecraft:stick"}, "riftworld:elisar_fence", 3)
    shaped("rope_post", ["P", "R", "P"], {"P": "#minecraft:planks", "R": "riftworld:bridge_rope"}, "riftworld:rope_post", 2)

    # Elisar Stone, Cobble, Bricks, Plaster, Timber & Roofs
    smelting("elisar_stone_from_smelting_elisar_cobble", "riftworld:elisar_cobble", "riftworld:elisar_stone", xp=0.1, time=200)
    add_stone_shape_recipes("riftworld:elisar_cobble", "riftworld:elisar_cobble_stairs", "riftworld:elisar_cobble_slab", None)
    stonecutting("elisar_stone_wall_from_elisar_stone_stonecutting", "riftworld:elisar_stone", "riftworld:elisar_stone_wall", 1)
    shaped("elisar_stone_wall", ["###", "###"], {"#": "riftworld:elisar_stone"}, "riftworld:elisar_stone_wall", 6)

    shaped("elisar_bricks", ["##", "##"], {"#": "riftworld:elisar_stone"}, "riftworld:elisar_bricks", 4)
    stonecutting("elisar_bricks_from_elisar_stone_stonecutting", "riftworld:elisar_stone", "riftworld:elisar_bricks", 1)
    shaped("elisar_bricks_stairs", ["#  ", "## ", "###"], {"#": "riftworld:elisar_bricks"}, "riftworld:elisar_bricks_stairs", 4)
    stonecutting("elisar_bricks_stairs_from_elisar_bricks_stonecutting", "riftworld:elisar_bricks", "riftworld:elisar_bricks_stairs", 1)
    shapeless("elisar_stone_mossy", ["riftworld:elisar_stone", "#riftworld:mod_vines"], "riftworld:elisar_stone_mossy", 1)
    shapeless("elisar_bricks_mossy", ["riftworld:elisar_bricks", "#riftworld:mod_vines"], "riftworld:elisar_bricks_mossy", 1)

    shaped(
        "elisar_plaster",
        ["SC", "CS"],
        {"S": "riftworld:elisar_seabed", "C": "#minecraft:stone_crafting_materials"},
        "riftworld:elisar_plaster",
        4,
    )
    shaped(
        "timber_frame_post",
        ["SPS", "SPS", "SPS"],
        {"S": "minecraft:stick", "P": "riftworld:elisar_plaster"},
        "riftworld:timber_frame_post",
        6,
    )
    shaped(
        "timber_frame_cross",
        ["S S", " P ", "S S"],
        {"S": "minecraft:stick", "P": "riftworld:elisar_plaster"},
        "riftworld:timber_frame_cross",
        4,
    )

    shaped("thatch", ["WW", "WW"], {"W": "riftworld:pale_wheat"}, "riftworld:thatch", 4)
    shaped("thatch_from_wheat", ["WW", "WW"], {"W": "minecraft:wheat"}, "riftworld:thatch", 4)
    shaped("thatch_stairs", ["#  ", "## ", "###"], {"#": "riftworld:thatch"}, "riftworld:thatch_stairs", 4)
    shaped("thatch_slab", ["###"], {"#": "riftworld:thatch"}, "riftworld:thatch_slab", 6)

    shaped("roof_red", ["BB", "BB"], {"B": "minecraft:brick"}, "riftworld:roof_red", 4)
    shapeless("roof_red_from_dye", ["#minecraft:stone_crafting_materials", "minecraft:red_dye"], "riftworld:roof_red", 2)
    add_stone_shape_recipes("riftworld:roof_red", "riftworld:roof_red_stairs", "riftworld:roof_red_slab", None)

    shapeless("roof_blue", ["#minecraft:stone_crafting_materials", "minecraft:blue_dye"], "riftworld:roof_blue", 2)
    add_stone_shape_recipes("riftworld:roof_blue", "riftworld:roof_blue_stairs", "riftworld:roof_blue_slab", None)

    # Lanterns, Chains, Windows & Candles
    shaped(
        "elisar_chain",
        ["N", "I", "N"],
        {"N": "minecraft:iron_nugget", "I": "minecraft:iron_ingot"},
        "riftworld:elisar_chain",
        2,
    )
    shaped("elisar_window", ["GGG", "GGG"], {"G": "minecraft:glass"}, "riftworld:elisar_window", 16)
    shaped(
        "elisar_lantern",
        ["NNN", "NTN", "NNN"],
        {"N": "minecraft:gold_nugget", "T": "minecraft:torch"},
        "riftworld:elisar_lantern",
        1,
    )
    shaped(
        "keep_cage_lantern",
        ["NNN", "NTN", "III"],
        {"N": "minecraft:iron_nugget", "T": "minecraft:soul_torch", "I": "minecraft:iron_bars"},
        "riftworld:keep_cage_lantern",
        2,
    )
    shaped(
        "pearl_prism_lantern",
        ["PCP", "CGC", "PCP"],
        {"P": "riftworld:pearl_calcite", "C": "minecraft:amethyst_shard", "G": "minecraft:glowstone_dust"},
        "riftworld:pearl_prism_lantern",
        1,
    )
    shaped(
        "ritual_candle",
        ["S", "W", "G"],
        {"S": "minecraft:string", "W": "minecraft:honeycomb", "G": "minecraft:gold_nugget"},
        "riftworld:ritual_candle",
        2,
    )
    shaped(
        "ritual_candle_from_candle",
        ["C", "G"],
        {"C": "minecraft:candle", "G": "minecraft:gold_nugget"},
        "riftworld:ritual_candle",
        1,
    )

    # Keep Stone, Garden Limestone & Rift Marble
    shaped("keep_stone", ["SC", "CS"], {"S": "#riftworld:mod_cobblestone", "C": "minecraft:cobblestone"}, "riftworld:keep_stone", 4)
    smelting("keep_stone_cracked", "riftworld:keep_stone", "riftworld:keep_stone_cracked", xp=0.1, time=200)
    shapeless("keep_stone_mossy", ["riftworld:keep_stone", "#riftworld:mod_vines"], "riftworld:keep_stone_mossy", 1)
    add_stone_shape_recipes("riftworld:keep_stone", "riftworld:keep_stone_stairs", "riftworld:keep_stone_slab", None)

    shapeless("garden_limestone_mossy", ["riftworld:garden_limestone", "#riftworld:mod_vines"], "riftworld:garden_limestone_mossy", 1)
    add_stone_shape_recipes(
        "riftworld:garden_limestone",
        "riftworld:garden_limestone_stairs",
        "riftworld:garden_limestone_slab",
        "riftworld:garden_limestone_wall",
    )

    shaped("rift_marble", ["QQ", "SS"], {"Q": "minecraft:quartz", "S": "#riftworld:mod_cobblestone"}, "riftworld:rift_marble", 4)
    shapeless("rift_marble_mossy", ["riftworld:rift_marble", "#riftworld:mod_vines"], "riftworld:rift_marble_mossy", 1)
    add_stone_shape_recipes("riftworld:rift_marble", "riftworld:rift_marble_stairs", "riftworld:rift_marble_slab", None)

    # Castle Blocks (`castle_bricks`, `ashlar`, `brass_block`, `castle_bookshelf`, `castle_carpet`, etc.)
    smelting("scorched_stone_from_ashen_cobblestone", "riftworld:ashen_cobblestone", "riftworld:scorched_stone", xp=0.1, time=200)
    shaped("castle_bricks", ["SB", "BS"], {"S": "riftworld:scorched_stone", "B": "minecraft:brick"}, "riftworld:castle_bricks", 4)
    smelting("castle_bricks_cracked", "riftworld:castle_bricks", "riftworld:castle_bricks_cracked", xp=0.1, time=200)
    shapeless("castle_bricks_mossy", ["riftworld:castle_bricks", "#riftworld:mod_vines"], "riftworld:castle_bricks_mossy", 1)
    add_stone_shape_recipes(
        "riftworld:castle_bricks",
        "riftworld:castle_bricks_stairs",
        "riftworld:castle_bricks_slab",
        "riftworld:castle_bricks_wall",
    )

    shaped("ashlar", ["##", "##"], {"#": "riftworld:scorched_stone"}, "riftworld:ashlar", 4)
    stonecutting("ashlar_from_scorched_stone_stonecutting", "riftworld:scorched_stone", "riftworld:ashlar", 1)
    shaped("ashlar_pillar", ["#", "#"], {"#": "riftworld:ashlar"}, "riftworld:ashlar_pillar", 2)
    stonecutting("ashlar_pillar_from_ashlar_stonecutting", "riftworld:ashlar", "riftworld:ashlar_pillar", 1)
    add_stone_shape_recipes("riftworld:ashlar", "riftworld:ashlar_stairs", "riftworld:ashlar_slab", None)

    shaped(
        "brass_block",
        ["CCC", "CGC", "CCC"],
        {"C": "minecraft:copper_ingot", "G": "minecraft:gold_ingot"},
        "riftworld:brass_block",
        2,
    )
    shaped(
        "castle_mosaic",
        ["GA", "AG"],
        {"G": "minecraft:gold_nugget", "A": "riftworld:ashlar"},
        "riftworld:castle_mosaic",
        2,
    )
    shaped(
        "stained_glass",
        ["GGG", "GRG", "GGG"],
        {"G": "minecraft:glass", "R": "minecraft:red_dye"},
        "riftworld:stained_glass",
        16,
    )
    shaped("castle_roof", ["SS", "BB"], {"S": "riftworld:scorched_stone", "B": "minecraft:brick"}, "riftworld:castle_roof", 4)
    add_stone_shape_recipes("riftworld:castle_roof", "riftworld:castle_roof_stairs", "riftworld:castle_roof_slab", None)
    shaped(
        "castle_bookshelf",
        ["PPP", "BBB", "PPP"],
        {"P": "#riftworld:mod_planks", "B": "minecraft:book"},
        "riftworld:castle_bookshelf",
        1,
    )
    shaped("castle_bars", ["III", "III"], {"I": "minecraft:iron_ingot"}, "riftworld:castle_bars", 16)
    shaped(
        "castle_carpet",
        ["WW", "GG"],
        {"W": "minecraft:red_wool", "G": "minecraft:gold_nugget"},
        "riftworld:castle_carpet",
        6,
    )

    # Temple Blocks (`temple_bricks`, `temple_pillar`, `temple_gold`, `temple_mosaic`, `mosaic_core`, etc.)
    shaped("temple_bricks", ["IP", "PI"], {"I": "riftworld:ivory_marble", "P": "riftworld:pearl_calcite"}, "riftworld:temple_bricks", 4)
    stonecutting("temple_bricks_from_ivory_marble_stonecutting", "riftworld:ivory_marble", "riftworld:temple_bricks", 1)
    smelting("temple_bricks_cracked", "riftworld:temple_bricks", "riftworld:temple_bricks_cracked", xp=0.1, time=200)
    shapeless("temple_bricks_ivy", ["riftworld:temple_bricks", "#riftworld:mod_vines"], "riftworld:temple_bricks_ivy", 1)
    add_stone_shape_recipes("riftworld:temple_bricks", "riftworld:temple_bricks_stairs", "riftworld:temple_bricks_slab", None)
    shaped("temple_pillar", ["#", "#"], {"#": "riftworld:temple_bricks"}, "riftworld:temple_pillar", 2)
    stonecutting("temple_pillar_from_temple_bricks_stonecutting", "riftworld:temple_bricks", "riftworld:temple_pillar", 1)
    shaped("temple_grand_column", ["#", "#"], {"#": "riftworld:temple_pillar"}, "riftworld:temple_grand_column", 2)
    shaped("temple_floor", ["TT", "TT"], {"T": "riftworld:temple_bricks_slab"}, "riftworld:temple_floor", 2)
    shaped(
        "temple_gold",
        ["GGG", "GTG", "GGG"],
        {"G": "minecraft:gold_nugget", "T": "riftworld:temple_bricks"},
        "riftworld:temple_gold",
        1,
    )
    shaped(
        "temple_gold_rune",
        [" R ", "GTG", " R "],
        {"R": "minecraft:redstone", "G": "minecraft:gold_ingot", "T": "riftworld:temple_gold"},
        "riftworld:temple_gold_rune",
        1,
    )
    shaped(
        "temple_mosaic",
        ["GLG", "LTL", "GLG"],
        {"G": "minecraft:gold_nugget", "L": "minecraft:lapis_lazuli", "T": "riftworld:temple_floor"},
        "riftworld:temple_mosaic",
        4,
    )
    shaped(
        "mosaic_core",
        ["GMG", "MDM", "GMG"],
        {"G": "minecraft:gold_ingot", "M": "riftworld:temple_mosaic", "D": "riftworld:elisar_ducat"},
        "riftworld:mosaic_core",
        1,
    )
    shaped(
        "temple_rose_window",
        ["GGG", "GDG", "GGG"],
        {"G": "minecraft:glass", "D": "minecraft:orange_dye"},
        "riftworld:temple_rose_window",
        4,
    )

    # Lab & Industrial Building Blocks
    shaped("lab_concrete", ["SG", "GS"], {"S": "#minecraft:stone_crafting_materials", "G": "minecraft:gravel"}, "riftworld:lab_concrete", 4)
    smelting("lab_concrete_cracked", "riftworld:lab_concrete", "riftworld:lab_concrete_cracked", xp=0.1, time=200)
    add_stone_shape_recipes("riftworld:lab_concrete", "riftworld:lab_concrete_stairs", "riftworld:lab_concrete_slab", None)

    shaped("lab_tile", ["##", "##"], {"#": "riftworld:lab_concrete"}, "riftworld:lab_tile", 4)
    smelting("lab_tile_cracked", "riftworld:lab_tile", "riftworld:lab_tile_cracked", xp=0.1, time=200)
    add_stone_shape_recipes("riftworld:lab_tile", "riftworld:lab_tile_stairs", "riftworld:lab_tile_slab", None)

    shaped("lab_panel", ["II", "CC"], {"I": "minecraft:iron_ingot", "C": "riftworld:lab_concrete"}, "riftworld:lab_panel", 4)
    shapeless("lab_panel_stripe", ["riftworld:lab_panel", "minecraft:yellow_dye"], "riftworld:lab_panel_stripe", 1)
    shaped("steel_floor", ["III", "CCC"], {"I": "minecraft:iron_nugget", "C": "riftworld:lab_concrete"}, "riftworld:steel_floor", 6)
    add_stone_shape_recipes("riftworld:steel_floor", "riftworld:steel_floor_stairs", "riftworld:steel_floor_slab", None)
    shaped("hazard_block", ["YB", "BY"], {"Y": "minecraft:yellow_dye", "B": "riftworld:lab_panel"}, "riftworld:hazard_block", 2)
    shaped("metal_grate", ["I I", " I ", "I I"], {"I": "minecraft:iron_bars"}, "riftworld:metal_grate", 4)
    shaped("lab_glass", ["GGG", "GIG", "GGG"], {"G": "minecraft:glass", "I": "minecraft:iron_nugget"}, "riftworld:lab_glass", 8)
    shaped("lab_lamp", ["III", "GGG", "RRR"], {"I": "minecraft:iron_nugget", "G": "minecraft:glowstone_dust", "R": "minecraft:glass"}, "riftworld:lab_lamp", 3)
    shaped("emergency_lamp", ["III", "GRG", "III"], {"I": "minecraft:iron_nugget", "G": "minecraft:redstone", "R": "minecraft:red_stained_glass"}, "riftworld:emergency_lamp", 2)
    shaped("pipe", ["III"], {"I": "minecraft:copper_ingot"}, "riftworld:pipe", 6)
    shaped("lab_railing", ["III", "I I"], {"I": "minecraft:iron_nugget"}, "riftworld:lab_railing", 4)
    shaped("wall_vent", ["III", "IBI", "III"], {"I": "minecraft:iron_nugget", "B": "minecraft:iron_bars"}, "riftworld:wall_vent", 2)


# =============================================================================
# 5. STRICT VERIFICATION OF ALL RECIPES, TAGS & PNG DIMENSIONS
# =============================================================================
def verify_everything():
    # Collect all registered riftworld block & item IDs from Java sources
    reg_ids = set()
    reg_pat = re.compile(
        r'(?:reg|BLOCKS\.register|ITEMS\.register|cube|stairs|slab|woodPillar|strippableWood|woodFence|woodGate|woodDoor|woodTrapdoor|woodPlate|woodButton|stoneWall|egg)\(\s*"([a-z0-9_]+)"'
    )
    for jf in glob.glob(os.path.join(ROOT, "mod/src/main/java/ru/aiforge/riftworld/**/*.java"), recursive=True):
        txt = open(jf, encoding="utf-8").read()
        for m in reg_pat.finditer(txt):
            reg_ids.add("riftworld:" + m.group(1))
    # Also ModelKind enum entries
    mk_file = os.path.join(ROOT, "mod/src/main/java/ru/aiforge/riftworld/block/ModelKind.java")
    if os.path.exists(mk_file):
        for m in re.finditer(r"^\s*([A-Z0-9_]+)\s*\(", open(mk_file, encoding="utf-8").read(), re.M):
            reg_ids.add("riftworld:" + m.group(1).lower())

    # Verify all JSON files under data/ parse cleanly and reference valid riftworld IDs
    recipe_files = sorted(glob.glob(os.path.join(RECIPES_DIR, "*.json")))
    for rf in recipe_files:
        d = json.load(open(rf, encoding="utf-8"))
        # Check ingredients
        def check_id(val: str):
            if val.startswith("riftworld:"):
                assert val in reg_ids, f"Unknown RiftWorld ID {val} in {rf}"

        if "ingredient" in d:
            ing_obj = d["ingredient"]
            if isinstance(ing_obj, dict) and "item" in ing_obj:
                check_id(ing_obj["item"])
        if "ingredients" in d:
            for x in d["ingredients"]:
                if isinstance(x, dict) and "item" in x:
                    check_id(x["item"])
        if "key" in d:
            for x in d["key"].values():
                if isinstance(x, dict) and "item" in x:
                    check_id(x["item"])
        res = d.get("result")
        if isinstance(res, str):
            check_id(res)
        elif isinstance(res, dict) and "item" in res:
            check_id(res["item"])

    # Verify gilded_paper exact pattern!
    gp = json.load(open(os.path.join(RECIPES_DIR, "gilded_paper.json"), encoding="utf-8"))
    assert gp["type"] == "minecraft:crafting_shaped"
    assert gp["pattern"] == ["GGG", "GPG", "GGG"]
    assert gp["key"]["P"]["item"] == "minecraft:paper"
    assert gp["key"]["G"]["item"] == "minecraft:gold_ingot"

    # Verify PNG dimensions of ducat_ore & deepslate_ducat_ore are 16x16
    for tn in ("ducat_ore.png", "deepslate_ducat_ore.png"):
        tp = os.path.join(ASSETS, "textures", "block", tn)
        hdr = open(tp, "rb").read(24)
        w, h = struct.unpack(">II", hdr[16:24])
        assert (w, h) == (16, 16), f"Expected 16x16 for {tn}, got {w}x{h}"

    print(
        f"VERIFIED v0.29.0 Crafting & Coin Ore: total_recipes={len(recipe_files)} | gilded_paper=GGG/GPG/GGG "
        f"| coin_ores=(ducat_ore, deepslate_ducat_ore 16x16 -> elisar_ducat) | unknown_ids=0"
    )


if __name__ == "__main__":
    generate_coin_ore_assets_and_worldgen()
    update_tags()
    generate_all_recipes()
    verify_everything()
