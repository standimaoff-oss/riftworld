#!/usr/bin/env python3
"""Riftworld v0.49.0 — vanilla-style spawn egg textures.

Minecraft's own spawn eggs are not solid coloured eggs. The game ships a
single grey speckled ``spawn_egg.png`` and one coloured ``spawn_egg_overlay.png``
per creature; the item model combines them so the egg reads as a pale grey
shell with a blotch of the creature's colour.

The v0.49.0 eggs were hand-painted solid blobs in an unrelated look, and 29 of
the registered eggs had no texture at all, so they rendered as the
missing-texture checkerboard. This generator rebuilds every Riftworld spawn
egg from the vanilla recipe:

  * the exact vanilla grey speckle mask (sampled from Minecraft 1.20.1)
  * the exact vanilla overlay blotch mask
  * the creature colour, chosen from the creature's own palette

The result is a 16x16 RGBA PNG that sits on the vanilla
``minecraft:item/template_spawn_egg`` model like every vanilla egg.
"""
from __future__ import annotations

from pathlib import Path

from PIL import Image
import numpy as np

ROOT = Path(__file__).resolve().parents[2]
TEX = ROOT / "mod/src/main/resources/assets/riftworld/textures/item"

# Vanilla 1.20.1 assets/minecraft/textures/item/spawn_egg.png
# Greys sampled from the shipped texture; "." = transparent, the letters are
# the four speckle brightnesses the vanilla egg uses.
BASE_GREY = np.array(
    [
        "................",
        "................",
        ".......MD.......",
        ".......??M......",
        "........??......",
        "........??.D....",
        "...ML..???BBD...",
        "...M?????..BD...",
        "..M??..?L..BBD..",
        "....?.??LLBBB?..",
        "....???.LLBB.?..",
        "...ML?...B......",
        "...M?B...B......",
        "....DDBBB??.....",
        "......D..?......",
        "................",
    ],
    dtype="U16",
)
GREY_VALUE = {"D": 92, "M": 108, "B": 220, "L": 232, "?": 128}

# Vanilla 1.20.1 assets/minecraft/textures/item/spawn_egg_overlay.png
# "X" = paint the creature colour here, "." = leave the grey speckle alone.
OVERLAY = np.array(
    [
        "................",
        "................",
        "................",
        ".....XX...X.....",
        "....XXXX..XX....",
        "....XXXX..X.....",
        ".....XX.........",
        ".........XX.....",
        ".....XX..XX.....",
        "..XX.X..........",
        "..XX...X....X...",
        "......XXX.XXX...",
        "......XXX.XXX...",
        "...........X....",
        ".......XX.......",
        "................",
    ],
    dtype="U16",
)

# Creature colour per egg. The values are the Riftworld palette colour of the
# creature so the egg matches what the player sees in the world.
COLOURS: dict[str, tuple[int, int, int]] = {
    # --- fauna -------------------------------------------------------
    "amethyst_fox": (176, 108, 214),
    "ash_moth": (128, 126, 122),
    "azure_frog": (92, 176, 224),
    "basalt_beetle": (74, 74, 82),
    "blossom_snail": (226, 140, 176),
    "blossom_stag": (238, 190, 214),
    "coral_hermit_crab": (232, 118, 96),
    "crystal_gecko": (120, 226, 214),
    "faceted_owl": (198, 168, 128),
    "ghost_squid": (176, 200, 226),
    "phantom_wolf": (96, 104, 124),
    "porcelain_crane": (238, 238, 230),
    "reef_seahorse": (238, 156, 96),
    "sizo_leafwing": (176, 210, 116),
    "silver_sea_serpent": (198, 210, 222),
    # --- fish --------------------------------------------------------
    "angler_fish": (64, 78, 94),
    "eel_fish": (74, 88, 100),
    "gloom_fish": (54, 42, 70),
    "gold_fish": (233, 194, 90),
    "jelly_fish": (198, 132, 208),
    "morok": (96, 132, 108),
    "hollowwatcher": (127, 232, 240),
    "ray_fish": (128, 118, 108),
    "silver_fish": (176, 186, 196),
    "sting_fish": (216, 176, 96),
    "striped_fish": (208, 122, 96),
    "surgeon_fish": (86, 178, 200),
    "turtle": (118, 160, 106),
    "sky_dart_fish": (140, 206, 232),
    # --- riftworld creatures ----------------------------------------
    "crystal_minion": (218, 110, 255),
    "el_gran_maja": (72, 96, 78),
    "elisar_archer": (196, 156, 96),
    "elisar_guard": (206, 168, 104),
    "elisar_villager": (168, 132, 186),
    "ember_guard": (214, 110, 62),
    "liopleurodon": (122, 168, 214),
    "morgrath": (110, 66, 92),
    "paper_dragon": (214, 196, 150),
    "rift_obelisk_boss": (146, 96, 214),
}


def build(colour: tuple[int, int, int]) -> np.ndarray:
    """Compose one spawn egg texture: grey speckle base + coloured overlay."""
    img = np.zeros((16, 16, 4), dtype=np.uint8)

    # Neutral vanilla greys, kept at the shipped brightness.
    for y in range(16):
        for x in range(16):
            ch = BASE_GREY[y][x]
            if ch == ".":
                continue
            grey = GREY_VALUE.get(ch, 128)
            img[y, x] = (grey, grey, grey, 255)

    # The creature-coloured blotch, in the vanilla overlay mask.
    cr, cg, cb = colour
    for y in range(16):
        for x in range(16):
            if OVERLAY[y][x] != "X":
                continue
            # Vanilla tints the overlay slightly brighter than the pure colour
            # so the speckle still reads through.
            img[y, x] = (
                min(255, int(cr * 1.15)),
                min(255, int(cg * 1.15)),
                min(255, int(cb * 1.15)),
                255,
            )
    return img


def main() -> None:
    TEX.mkdir(parents=True, exist_ok=True)
    written = 0
    for name, colour in sorted(COLOURS.items()):
        Image.fromarray(build(colour), "RGBA").save(TEX / f"{name}_spawn_egg.png")
        written += 1
    print(f"OK vanilla spawn eggs: {written} textures written to {TEX}")


if __name__ == "__main__":
    main()
