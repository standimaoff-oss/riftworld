#!/usr/bin/env python3
"""
v0.48.0 — From-scratch 3D Voxel Armament & Dual-Ore Tool Set Generator.

Builds pixel-accurate 32x32 voxel matrices AND greedy-merged, interior-face-culled
3D JSON item models backed by mipmap-proof 64x64 solid-swatch palette PNGs:
  1) Aurite Set (from coin ore -> elisar_ducat <-> aurite_ingot):
     aurite_pickaxe, aurite_axe (double-bit), aurite_sword, aurite_shovel, aurite_hoe
  2) Riftcrystal Set (from rare 4-biome riftcrystal_ore -> riftcrystal):
     riftcrystal_pickaxe, riftcrystal_axe (double-bit), riftcrystal_sword, riftcrystal_shovel, riftcrystal_hoe
  3) Unified Riftworld Armament (Reference 03_riftworld_weapon_tools_set.png + 06_elisar_guard.png):
     elisar_bow (+ pulling_0/1/2), elisar_shield (+ blocking),
     elisar_guard_shield (+ blocking), elisar_guard_sword
  4) Crafting recipes + Minecraft tool/weapon item tags.
"""
from __future__ import annotations

import json
from pathlib import Path
from typing import Dict, List, Optional, Tuple
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "mod/src/main/resources/assets/riftworld"
DATA = ROOT / "mod/src/main/resources/data"

RGBA = Tuple[int, int, int, int]


def hex_rgba(h: str, a: int = 255) -> RGBA:
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


# ==============================================================================
# Palettes (sampled directly from 03_riftworld_weapon_tools_set.png, 02 & 06)
# ==============================================================================

AUR_PAL: Dict[str, RGBA] = {
    "k": hex_rgba("#241507"),  # darkest bronze/ironwood outline
    "b": hex_rgba("#3D250E"),  # dark bronze / haft border
    "m": hex_rgba("#5C3B16"),  # mid forged bronze cheek
    "w": hex_rgba("#8A5B1E"),  # warm bronze bevel
    "g": hex_rgba("#C48724"),  # rich aurite gold bevel
    "G": hex_rgba("#E6B33E"),  # bright aurite gold blade
    "H": hex_rgba("#F7DB74"),  # sunlit gold highlight
    "c": hex_rgba("#18A8A4"),  # deep cyan gem bevel
    "C": hex_rgba("#32EAE2"),  # glowing cyan crystal
    "W": hex_rgba("#9FFFFA"),  # white-cyan gem glint
    "p": hex_rgba("#9B3FE0"),  # violet rift spark (dark)
    "P": hex_rgba("#DF6BFF"),  # violet rift spark (bright)
    "o": hex_rgba("#11121A"),  # dark obsidian shield field
    "O": hex_rgba("#1C1E2B"),  # obsidian subtle highlight
    "f": hex_rgba("#EFECE8"),  # arrow feather white
    "F": hex_rgba("#C4BFC4"),  # arrow feather shade
}

RC_PAL: Dict[str, RGBA] = {
    "k": hex_rgba("#090D14"),  # deepest obsidian outline
    "b": hex_rgba("#151D2A"),  # dark obsidian haft/spine
    "m": hex_rgba("#243042"),  # slate-obsidian cheek
    "w": hex_rgba("#394860"),  # light slate bevel
    "g": hex_rgba("#129694"),  # deep cyan crystal bevel
    "G": hex_rgba("#28DBD4"),  # bright cyan crystal blade
    "H": hex_rgba("#8FFFF8"),  # radiant cyan highlight
    "c": hex_rgba("#9628C8"),  # deep magenta rift bevel
    "C": hex_rgba("#E454FF"),  # glowing magenta rift vein/gem
    "W": hex_rgba("#FFB5FF"),  # bright magenta-white glint
}

GUARD_PAL: Dict[str, RGBA] = {
    "k": hex_rgba("#241914"),
    "b": hex_rgba("#4A3219"),
    "g": hex_rgba("#A86828"),
    "G": hex_rgba("#D6913E"),
    "H": hex_rgba("#EEB968"),
    "s": hex_rgba("#6E7B8B"),  # steel bevel
    "S": hex_rgba("#A6B4C4"),  # steel mid
    "E": hex_rgba("#DCE6F2"),  # steel bright edge
    "l": hex_rgba("#8D6D91"),  # Elisar lilac shade
    "L": hex_rgba("#AA89AC"),  # Elisar lilac field
    "t": hex_rgba("#385F5A"),  # seafoam-teal dark
    "T": hex_rgba("#52857D"),  # seafoam-teal bright
}


# ==============================================================================
# 32x32 Voxel Art Grids (y=0 top .. y=31 bottom, x=0 left .. x=31 right)
# Optional depth map symbols:
#   default voxel: z in [7.5, 8.5] (1.0 unit thick)
#   '2' in depth_grid: z in [7.1, 8.9] (1.8 units thick — raised guards, rims, collars)
#   '3' in depth_grid: z in [6.75, 9.25] (2.5 units thick — protruding crystal gems & shield boss)
#   'B' in depth_grid: z in [6.0, 7.5] (rear shield handle)
# ==============================================================================

def pad32(rows: List[str]) -> List[str]:
    """Centers an art list of strings (width <= 32, height <= 32) onto a 32x32 grid."""
    h = len(rows)
    w = max(len(r) for r in rows)
    assert h <= 32 and w <= 32, f"Grid {w}x{h} exceeds 32x32"
    top = (32 - h) // 2
    left = (32 - w) // 2
    out = ["." * 32 for _ in range(top)]
    for r in rows:
        rp = r.ljust(w, ".")
        out.append("." * left + rp + "." * (32 - left - w))
    while len(out) < 32:
        out.append("." * 32)
    return out


# 1. PICKAXE (03 item #1 — 19w x 28h with gull-wing twin golden arch & diagonal-wrapped haft)
PICKAXE_ART = pad32([
    "........kkk........",
    ".......kbwbk.......",
    ".....kkbbGbbkk.....",
    "....kbGHbwbHGbk....",
    "...kbGGHHgHHGGbk...",
    "..kbGGHgkwkgHGGbk..",
    ".kbGHgk.bGb.kgHGbk.",
    "kbGHgk..kbk..kgHGbk",
    "kbGgk...bGb...kgGbk",
    "kbgk....kbk....kgbk",
    ".kk.....bGb.....kk.",
    "........kbk........",
    "........bGb........",
    "........kbk........",
    "........bGb........",
    "........kbk........",
    "........bGb........",
    "........kbk........",
    "........bGb........",
    "........kbk........",
    "........bGb........",
    "........kbk........",
    ".......kcCck.......",
    ".......kbGbk.......",
    "......kbGCWbk......",
    ".......kbGbk.......",
    "........kbk........",
    ".........k.........",
])

PICKAXE_DEPTH = pad32([
    "........222........",
    ".......22222.......",
    ".....112222211.....",
    "....11112221111....",
    "...1111122211111...",
    "..111111222111111..",
    ".111111..1..111111.",
    "111111..111..111111",
    "11111...111...11111",
    "1111....111....1111",
    ".11.....111.....11.",
    "........111........",
    "........111........",
    "........111........",
    "........111........",
    "........111........",
    "........111........",
    "........111........",
    "........111........",
    "........111........",
    "........111........",
    "........111........",
    ".......22222.......",
    ".......22222.......",
    "......2223222......",
    ".......22222.......",
    "........222........",
    ".........1.........",
])

# 2. DOUBLE-BIT BATTLEAXE / AXE (03 item #2 — 19w x 28h)
AXE_ART = pad32([
    "................... ",
    "...kkk.......kkk...",
    "..kbGbk..k..kbGbk..",
    ".kbGHGbkbbbkGHGbk. ",
    "kbGHwmbbgGgbbmwHGbk",
    "kbGwmbbbwCwbbbmwGbk",
    "kbGmbmHkWCWkHmbmGbk",
    "kbGwmbbbwCwbbbmwGbk",
    "kbGHwmbbgGgbbmwHGbk",
    ".kbGHGbkbbbkGHGbk. ",
    "..kbGbk.bGb.kbGbk..",
    "...kkk..kbk..kkk...",
    "........bGb........",
    "........kbk........",
    "........bGb........",
    "........kbk........",
    "........bGb........",
    "........kbk........",
    "........bGb........",
    "........kbk........",
    "........bGb........",
    "........kbk........",
    "........bGb........",
    ".......kcCck.......",
    "......kbGCWbk......",
    ".......kbGbk.......",
    "........kbk........",
    ".........k.........",
])
# Clean trailing spaces in AXE_ART
AXE_ART = pad32([r.strip() if False else r[:19] for r in [
    "...................",
    "...kkk.......kkk...",
    "..kbGbk..k..kbGbk..",
    ".kbGHGbkkbbkkGHGbk.",
    "kbGHwmbbgGgbbmwHGbk",
    "kbGwmbbbwCwbbbmwGbk",
    "kbGmbmHkWCWkHmbmGbk",
    "kbGwmbbbwCwbbbmwGbk",
    "kbGHwmbbgGgbbmwHGbk",
    ".kbGHGbkkbbkkGHGbk.",
    "..kbGbk.bGb.kbGbk..",
    "...kkk..kbk..kkk...",
    "........bGb........",
    "........kbk........",
    "........bGb........",
    "........kbk........",
    "........bGb........",
    "........kbk........",
    "........bGb........",
    "........kbk........",
    "........bGb........",
    "........kbk........",
    "........bGb........",
    ".......kcCck.......",
    "......kbGCWbk......",
    ".......kbGbk.......",
    "........kbk........",
    ".........k.........",
]])

AXE_DEPTH = pad32([
    "...................",
    "...111.......111...",
    "..11111..2..11111..",
    ".11111112221111111.",
    "1111111222221111111",
    "1111111223221111111",
    "1111112233322111111",
    "1111111223221111111",
    "1111111222221111111",
    ".11111112221111111.",
    "..11111.111.11111..",
    "...111..111..111...",
    "........111........",
    "........111........",
    "........111........",
    "........111........",
    "........111........",
    "........111........",
    "........111........",
    "........111........",
    "........111........",
    "........111........",
    "........111........",
    ".......22222.......",
    "......2223222......",
    ".......22222.......",
    "........222........",
    ".........1.........",
])

# 3. BROADSWORD (03 item #3 — 13w x 30h)
SWORD_ART = pad32([
    "......k......",
    ".....kHk.....",
    "....kGHGk....",
    "...kgGHGgK...".replace("K", "k"),
    "...kgGHGgK...".replace("K", "k"),
    "...kgHGHHk...",
    "...kgGHGgK...".replace("K", "k"),
    "...kgHGHHk...",
    "...kgGHGgK...".replace("K", "k"),
    "...kgHGHHk...",
    "...kgGHGgK...".replace("K", "k"),
    "...kgHGHHk...",
    "...kgGHGgK...".replace("K", "k"),
    "...kgHGHHk...",
    "...kgGHGgK...".replace("K", "k"),
    "...kgGGGgk...",
    ".kkbbgGgbbkk.",
    "kbGmbcCcbmGbk",
    "kbHGkWCWkGHbk",
    ".kkbbcccbbkk.",
    ".....kbk.....",
    ".....bGb.....",
    ".....kbk.....",
    ".....bGb.....",
    ".....kbk.....",
    "....kbGbk....",
    "...kbGCWbk...",
    "....kbGbk....",
    ".....kbk.....",
    "......k......",
])

SWORD_DEPTH = pad32([
    "......1......",
    ".....111.....",
    "....11111....",
    "...1111111...",
    "...1111111...",
    "...1111111...",
    "...1111111...",
    "...1111111...",
    "...1111111...",
    "...1111111...",
    "...1111111...",
    "...1111111...",
    "...1111111...",
    "...1111111...",
    "...1111111...",
    "...1122211...",
    ".22222222222.",
    "2222233322222",
    "2222233322222",
    ".22222222222.",
    ".....111.....",
    ".....111.....",
    ".....111.....",
    ".....111.....",
    ".....111.....",
    "....22222....",
    "...2223222...",
    "....22222....",
    ".....222.....",
    "......1......",
])

# 4. SHOVEL (Matching 03 voxel craftsmanship — 11w x 28h)
SHOVEL_ART = pad32([
    "....kkk....",
    "..kkGHGkK..".replace("K", "k"),
    ".kbGHHHGbk.",
    "kbGHHwHHGbk",
    "kbGHwmmHGbk",
    "kbGwmGmwGbk",
    "kbGwmbmwGbk",
    ".kbGmbmGbk.",
    ".kkbcCcbkk.",
    "...kWCWk...",
    "...kbGbk...",
    "....kbk....",
    "....bGb....",
    "....kbk....",
    "....bGb....",
    "....kbk....",
    "....bGb....",
    "....kbk....",
    "....bGb....",
    "....kbk....",
    "....bGb....",
    "....kbk....",
    "...kcCck...",
    "...kbGbk...",
    "..kbGCWbk..",
    "...kbGbk...",
    "....kbk....",
    ".....k.....",
])

SHOVEL_DEPTH = pad32([
    "....111....",
    "..1111111..",
    ".111111111.",
    "11111111111",
    "11111111111",
    "11111211111",
    "11111211111",
    ".111222111.",
    ".222333222.",
    "...23332...",
    "...22222...",
    "....111....",
    "....111....",
    "....111....",
    "....111....",
    "....111....",
    "....111....",
    "....111....",
    "....111....",
    "....111....",
    "....111....",
    "....111....",
    "...22222...",
    "...22222...",
    "..2223222..",
    "...22222...",
    "....222....",
    ".....1.....",
])

# 5. HOE / REAPER (Matching 03 voxel craftsmanship — 17w x 28h)
HOE_ART = pad32([
    "......kkkkkk.....",
    "....kkbGGGGbkk...",
    "...kbGGHHHHGGbk..",
    "..kbGGHggggcCcbk.",
    ".kbGHgk...kWCWbk.",
    "kbGHgk....kbGbbk.",
    "kbGgk......kbk...",
    "kbgk.......bGb...",
    ".kk........kbk...",
    "...........bGb...",
    "...........kbk...",
    "...........bGb...",
    "...........kbk...",
    "...........bGb...",
    "...........kbk...",
    "...........bGb...",
    "...........kbk...",
    "...........bGb...",
    "...........kbk...",
    "...........bGb...",
    "...........kbk...",
    "...........bGb...",
    "..........kcCck..",
    "..........kbGbk..",
    ".........kbGCWbk.",
    "..........kbGbk..",
    "...........kbk...",
    "............k....",
])

HOE_DEPTH = pad32([
    "......111111.....",
    "....1111111111...",
    "...111111112222..",
    "..11111111233322.",
    ".111111...233322.",
    "111111....222222.",
    "11111......111...",
    "1111.......111...",
    ".11........111...",
    "...........111...",
    "...........111...",
    "...........111...",
    "...........111...",
    "...........111...",
    "...........111...",
    "...........111...",
    "...........111...",
    "...........111...",
    "...........111...",
    "...........111...",
    "...........111...",
    "...........111...",
    "..........22222..",
    "..........22222..",
    ".........2223222.",
    "..........22222..",
    "...........222...",
    "............1....",
])

# 6. RECURVE RIFT-STRING BOW (03 item #4 — idle + 3 pulling stages with cyan crystal arrow!)
BOW_IDLE_ART = pad32([
    "....kk..........",
    "...kGHkk........",
    "...kcbbGkk......",
    "....C.kbbGk.....",
    "....C..kbHGk....",
    "....P...kbHGk...",
    "....C....kbGbk..",
    "....C....kbHGk..",
    "....C.....kbGbk.",
    "....P.....kbGbk.",
    "....C.....kcCck.",
    "....C.....kcWck.",
    "....C.....kbHbk.",
    "....C.....kbGbk.",
    "....P.....kbHbk.",
    "....C.....kbGbk.",
    "....C....kbHGk..",
    "....C....kbGbk..",
    "....P...kbHGk...",
    "....C..kbHGk....",
    "....C.kbbGk.....",
    "...kcbbGkk......",
    "...kGHkk........",
    "....kk..........",
])


def make_bow_pull_art(pull_stage: int) -> Tuple[List[str], List[str]]:
    """
    pull_stage 0, 1, 2 bends the cyan+violet rift string further left
    and nocks the cyan-crystal-tipped arrow right across the grip!
    """
    shift = [1, 3, 5][pull_stage]
    grid = [["." for _ in range(32)] for _ in range(32)]
    depth = [["." for _ in range(32)] for _ in range(32)]
    base = BOW_IDLE_ART
    # Copy curved bow limbs (everything with x >= 11 in padded 32x32)
    # In BOW_IDLE_ART (width 16 -> left pad = 8), string is at x = 8 + 4 = 12!
    str_x = 12
    top_y = 4
    bot_y = 27
    mid_y = 15

    for y in range(32):
        for x in range(32):
            ch = base[y][x]
            if ch != "." and x > str_x:
                grid[y][x] = ch
                depth[y][x] = "2" if ch in ("C", "W", "c") and 14 <= y <= 17 else "1"
            elif ch != "." and y in (4, 5, 6, 25, 26, 27):
                grid[y][x] = ch
                depth[y][x] = "1"

    # Draw bent cyan+violet string from (str_x, 6) -> (str_x - shift, mid_y) -> (str_x, 25)
    for y in range(7, 25):
        t = 1.0 - abs(y - mid_y) / 9.0
        sx = int(round(str_x - shift * max(0.0, t)))
        grid[y][sx] = "P" if y in (9, 13, 18, 22) else "C"
        depth[y][sx] = "1"

    # Draw nocked arrow along y = mid_y..mid_y+1 from pulled string (str_x - shift) to tip!
    ax0 = str_x - shift
    tip_x = 24 - pull_stage
    # White feather fletching at tail
    for fy in (mid_y - 1, mid_y + 2):
        grid[fy][ax0 + 1] = "f"
        grid[fy][ax0 + 2] = "F"
        depth[fy][ax0 + 1] = "1"
        depth[fy][ax0 + 2] = "1"
    # Golden-wood arrow shaft
    for x in range(ax0 + 1, tip_x - 2):
        grid[mid_y][x] = "G" if x % 2 == 0 else "m"
        grid[mid_y + 1][x] = "b"
        depth[mid_y][x] = "2"
        depth[mid_y + 1][x] = "2"
    # Faceted glowing cyan crystal arrowhead (exact match to 03 item #4!)
    for dy, row_c in [(-1, ".cC."), (0, "cCWH"), (1, "cCWC"), (2, ".cc.")]:
        for dx, ch in enumerate(row_c):
            if ch != ".":
                px = tip_x - 2 + dx
                py = mid_y + dy
                grid[py][px] = ch
                depth[py][px] = "3" if ch in ("W", "C") else "2"

    return ["".join(r) for r in grid], ["".join(r) for r in depth]


# 7. HEATER RIFT SHIELD (03 item #5 — 17w x 24h, true 3D rim + recessed plate + raised golden eye boss)
SHIELD_ART = pad32([
    ".kkkkkkkkkkkkkkk.",
    "kbGGGGHHHHHGGGGbk",
    "kGCkkkkkkkkkkkCGk",
    "kGkoooooooooookGk",
    "kGkooCCoooCCookGk",
    "kGkooCWoooWCookGk",
    "kGkoooooooooookGk",
    "kGkoookgGgkookGk.".rstrip(".") + "",
    "kGkoogGHcHGgookGk",
    "kGkooGHcWCWHookGk",
    "kGkooGHcWCWHookGk",
    "kGkoogGHcHGgookGk",
    "kGkoPoookookPookGk"[:17],
    "kGkoPookCkookPokGk"[:17],
    ".kGkooooooooookGk",
    ".kbGkoPoooPookGbk",
    "..kGkoooooooookGk",
    "..kbGkoPoPookGbk.",
    "...kbGkoCookGbk..",
    "....kbGkokGbk....",
    ".....kbGCGbk.....",
    "......kbGbk......",
    ".......kbk.......",
    "........k........",
])
# Make every row of SHIELD_ART cleanly symmetric 17 chars wide:
SHIELD_ROWS = [
    ".kkkkkkkkkkkkkkk.",
    "kbGGGGHHHHHGGGGbk",
    "kGCkkkkkkkkkkkCGk",
    "kGkoooooooooookGk",
    "kGkooCCoooCCookGk",
    "kGkooCWoooWCookGk",
    "kGkoooooooooookGk",
    "kGkoookgGgkoookGk",
    "kGkoogGHcHGgookGk",
    "kGkooGHcWCWHookGk",
    "kGkooGHcWCWHookGk",
    "kGkoogGHcHGgookGk",
    "kGkoPookCkookPokGk"[:17],  # fix to exact 17 symmetry below!
]
SHIELD_CLEAN = [
    ".kkkkkkkkkkkkkkk.",
    "kbGGGGHHHHHGGGGbk",
    "kGCkkkkkkkkkkkCGk",
    "kGkoooooooooookGk",
    "kGkooCCoooCCookGk",
    "kGkooCWoooWCookGk",
    "kGkoooooooooookGk",
    "kGkoookgGgkoookGk",
    "kGkoogGHcHGgookGk",
    "kGkooGHcWCWHookGk",
    "kGkooGHcWCWHookGk",
    "kGkoogGHcHGgookGk",
    "kGkoPookCkookPokk"[:17],
]
SHIELD_EXACT_ROWS = [
    ".kkkkkkkkkkkkkkk.",
    "kbGGGGHHHHHGGGGbk",
    "kGCkkkkkkkkkkkCGk",
    "kGkoooooooooookGk",
    "kGkooCCoooCCookGk",
    "kGkooCWoooWCookGk",
    "kGkoooooooooookGk",
    "kGkoookgGgkoookGk",
    "kGkoogGHcHGgookGk",
    "kGkooGHcWCWHookGk",
    "kGkooGHcWCWHookGk",
    "kGkoogGHcHGgookGk",
    "kGkoPookCkookPok.",  # wait: keep right border kGk!
]
# Let's write exact 17-char symmetric rows cleanly:
SHIELD_SYM_17 = [
    ".kkkkkkkkkkkkkkk.",  # 17
    "kbGGGGHHHHHGGGGbk",  # 17
    "kGCkkkkkkkkkkkCGk",  # 17
    "kGkoooooooooookGk",  # 17
    "kGkooCCoooCCookGk",  # 17
    "kGkooCWoooWCookGk",  # 17
    "kGkoooooooooookGk",  # 17
    "kGkoookgGgkoookGk",  # 17
    "kGkoogGHcHGgookGk",  # 17
    "kGkooGHcWCWHookGk",  # 17
    "kGkooGHcWCWHookGk",  # 17
    "kGkoogGHcHGgookGk",  # 17
    "kGkoPokgGgkoPokGk",  # 17
    "kGkoPoookoooPokGk",  # 17
    ".kGkooooooooookGk"[:17],
]
# Even better: 17-wide symmetric helper!
SHIELD_17 = [
    ".kkkkkkkkkkkkkkk.",
    "kbGGGGHHHHHGGGGbk",
    "kGCkkkkkkkkkkkCGk",
    "kGkoooooooooookGk",
    "kGkooCCoooCCookGk",
    "kGkooCWoooWCookGk",
    "kGkoooooooooookGk",
    "kGkoookgGgkoookGk",
    "kGkoogGHcHGgookGk",
    "kGkooGHcWCWHookGk",
    "kGkooGHcWCWHookGk",
    "kGkoogGHcHGgookGk",
    "kGkoPokgGgkoPokGk",
    "kGkoPoookoooPokGk",
    ".kbGkoooooooookbk",
    ".kbGkoPoooPokGbk.",  # 17! check: 1+4+7+4+1=17
    "..kbGkooooookGbk.",
    "..kbGkoPoPokGbk..",
    "...kbGkoCokGbk...",
    "....kbGkokGbk....",
    ".....kbGCGbk.....",
    "......kbGbk......",
    ".......kbk.......",
    "........k........",
]
# Fix rows 14 and 16 to exact 17 symmetry:
SHIELD_17[14] = ".kbGkooooooookGbk"
SHIELD_17[16] = "..kbGkooooookGbk."  # 2 + 13 + 2 -> wait: 2+4+5+4+2 = 17: "..kbGkoooookGbk.."
SHIELD_17[16] = "..kbGkoooookGbk.."
for r in SHIELD_17:
    assert len(r) == 17, (len(r), r)

SHIELD_ART = pad32(SHIELD_17)
SHIELD_DEPTH_17 = []
for y, r in enumerate(SHIELD_17):
    dr = []
    for x, ch in enumerate(r):
        if ch == ".":
            dr.append(".")
        elif 7 <= y <= 12 and 5 <= x <= 11 and ch in ("g", "G", "H", "c", "C", "W"):
            dr.append("3")  # Raised 3D golden eye medallion & cyan crystal core
        elif ch in ("b", "G", "H", "C", "W") and (y <= 2 or x <= 3 or x >= 13 or y >= 17):
            dr.append("2")  # Raised 3D golden rim & corner gems
        else:
            dr.append("1")  # Recessed obsidian plate
    SHIELD_DEPTH_17.append("".join(dr))
SHIELD_DEPTH = pad32(SHIELD_DEPTH_17)

# 8. ELISAR GUARD SHIELD & SWORD (Reference 06_elisar_guard.png)
GUARD_SHIELD_15 = [
    "kkkkkkkkkkkkkkk",
    "kGGGGHHHHHGGGGk",
    "kGTTLLLLLLLTTGk",
    "kGTLLLLLLLLLTGk",
    "kGLLLtTTTtLLLGk",
    "kGLLtTGGGTtLLGk",
    "kGLLTGGHGGTLlGk",
    "kGLLTGHGHGTLlGk",
    "kGLLTGHGHGTLlGk",
    "kGLLTGGHGGTLlGk",
    "kGLLtTGGGTtLlGk",
    "kGLLLtTTTtLLlGk",
    "kGTlllllllllTGk",
    "kGTTlllllllTTGk",
    "kbGGGGGGGGGGGbk",
    ".kkkkkkkkkkkkk.",
]
for r in GUARD_SHIELD_15:
    assert len(r) == 15, r
GUARD_SHIELD_ART = pad32(GUARD_SHIELD_15)
GUARD_SHIELD_DEPTH = pad32([
    "".join(
        "." if ch == "." else (
            "3" if (4 <= y <= 11 and 4 <= x <= 10 and ch in ("t", "T", "G", "H")) else (
                "2" if ch in ("k", "b", "G", "H") else "1"
            )
        )
        for x, ch in enumerate(r)
    )
    for y, r in enumerate(GUARD_SHIELD_15)
])

GUARD_SWORD_ART = pad32([
    "......k......",
    ".....kEk.....",
    "....kSEEk....",
    "...ksSEESk...",
    "...ksSEESk...",
    "...ksSEESk...",
    "...ksSEESk...",
    "...ksSEESk...",
    "...ksSEESk...",
    "...ksSEESk...",
    "...ksSEESk...",
    "...ksSEESk...",
    "...ksSEESk...",
    "...ksSEESk...",
    "...ksSSSSk...",
    ".kkggGGGggkk.",
    "kbGHGGGGGHGbk",
    ".kkbbbbbbbkk.",
    ".....kbk.....",
    ".....bGb.....",
    ".....kbk.....",
    ".....bGb.....",
    "....kbGbk....",
    "...kbGHGbK...".replace("K", "k"),
    "....kbGbk....",
    ".....kkk.....",
])
GUARD_SWORD_DEPTH = pad32([
    "......1......",
    ".....111.....",
    "....11111....",
    "...1111111...",
    "...1111111...",
    "...1111111...",
    "...1111111...",
    "...1111111...",
    "...1111111...",
    "...1111111...",
    "...1111111...",
    "...1111111...",
    "...1111111...",
    "...1111111...",
    "...1122211...",
    ".22222222222.",
    "2222233322222",
    ".22222222222.",
    ".....111.....",
    ".....111.....",
    ".....111.....",
    ".....111.....",
    "....22222....",
    "...2223222...",
    "....22222....",
    ".....111.....",
])


# ==============================================================================
# Greedy Voxel-to-Minecraft-JSON-Elements Compiler (Mipmap-proof 64x64 Swatches)
# ==============================================================================

DEPTH_Z: Dict[str, Tuple[float, float]] = {
    "1": (7.50, 8.50),
    "2": (7.10, 8.90),
    "3": (6.70, 9.30),
}


def build_palette_png_and_uv_map(pal: Dict[str, RGBA], out_png: Path) -> Dict[str, List[float]]:
    """
    Packs each palette symbol into an 8x8 solid opaque tile on a 64x64 PNG (8x8 grid of tiles).
    Returns UV rect [u0, v0, u1, v1] in [0..16] normalized coordinates pointing to the safe
    interior 4x4 core of each 8x8 tile -> ZERO mipmap bleeding at all mipmap levels!
    """
    img = np.zeros((64, 64, 4), dtype=np.uint8)
    uv_map: Dict[str, List[float]] = {}
    keys = list(pal.keys())
    assert len(keys) <= 64
    # Fill entire background with first opaque color so even lvl-4 mipmaps have 255 alpha everywhere
    img[:, :] = pal[keys[0]]
    for idx, k in enumerate(keys):
        gx = (idx % 8) * 8
        gy = (idx // 8) * 8
        img[gy:gy + 8, gx:gx + 8] = pal[k]
        # 64px corresponds to 16.0 UV units -> 1px = 0.25 UV units.
        # Safe center [gx+2 .. gx+6, gy+2 .. gy+6]:
        uv_map[k] = [(gx + 2) * 0.25, (gy + 2) * 0.25, (gx + 6) * 0.25, (gy + 6) * 0.25]
    out_png.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(img, "RGBA").save(out_png)
    return uv_map


def save_sprite_png(art32: List[str], pal: Dict[str, RGBA], out_png: Path) -> None:
    arr = np.zeros((32, 32, 4), dtype=np.uint8)
    for y in range(32):
        for x in range(32):
            ch = art32[y][x]
            if ch != ".":
                arr[y, x] = pal[ch]
    out_png.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(arr, "RGBA").save(out_png)


def compile_voxel_elements(
    art32: List[str],
    depth32: Optional[List[str]],
    uv_map: Dict[str, List[float]],
    add_shield_grip: bool = False,
    grip_key: str = "b",
) -> List[dict]:
    """
    Greedy 2D rectangle merger over the 32x32 grid (grouped by (color_symbol, depth_symbol))
    with exact per-side face culling where neighboring voxels completely cover a shared side!
    """
    if depth32 is None:
        depth32 = ["".join("1" if c != "." else "." for c in row) for row in art32]

    # Track z-span per (x, y) for side-face occlusion culling
    zspan: Dict[Tuple[int, int], Tuple[float, float]] = {}
    for py in range(32):
        for px in range(32):
            ch = art32[py][px]
            if ch != ".":
                dk = depth32[py][px]
                zspan[(px, py)] = DEPTH_Z.get(dk, (7.50, 8.50))

    visited = [[False] * 32 for _ in range(32)]
    elements: List[dict] = []

    for py in range(32):
        for px in range(32):
            if visited[py][px] or art32[py][px] == ".":
                continue
            sym = art32[py][px]
            dk = depth32[py][px]
            z0, z1 = DEPTH_Z.get(dk, (7.50, 8.50))

            # Expand width
            w = 1
            while px + w < 32 and not visited[py][px + w] and art32[py][px + w] == sym and depth32[py][px + w] == dk:
                w += 1
            # Expand height
            h = 1
            while py + h < 32:
                ok = True
                for dx in range(w):
                    if visited[py + h][px + dx] or art32[py + h][px + dx] != sym or depth32[py + h][px + dx] != dk:
                        ok = False
                        break
                if not ok:
                    break
                h += 1

            for dy in range(h):
                for dx in range(w):
                    visited[py + dy][px + dx] = True

            # Model coordinates in [0.0, 16.0]:
            x0 = round(px * 0.5, 3)
            x1 = round((px + w) * 0.5, 3)
            y0 = round((32 - (py + h)) * 0.5, 3)
            y1 = round((32 - py) * 0.5, 3)

            uv = uv_map[sym]
            faces: Dict[str, dict] = {
                "north": {"uv": uv, "texture": "#pal"},
                "south": {"uv": uv, "texture": "#pal"},
            }
            # Check if west (px - 1) is completely covered for all dy in 0..h-1
            if not all((px - 1, py + dy) in zspan and zspan[(px - 1, py + dy)][0] <= z0 and zspan[(px - 1, py + dy)][1] >= z1 for dy in range(h)):
                faces["west"] = {"uv": uv, "texture": "#pal"}
            if not all((px + w, py + dy) in zspan and zspan[(px + w, py + dy)][0] <= z0 and zspan[(px + w, py + dy)][1] >= z1 for dy in range(h)):
                faces["east"] = {"uv": uv, "texture": "#pal"}
            # Up corresponds to py - 1; Down corresponds to py + h
            if not all((px + dx, py - 1) in zspan and zspan[(px + dx, py - 1)][0] <= z0 and zspan[(px + dx, py - 1)][1] >= z1 for dx in range(w)):
                faces["up"] = {"uv": uv, "texture": "#pal"}
            if not all((px + dx, py + h) in zspan and zspan[(px + dx, py + h)][0] <= z0 and zspan[(px + dx, py + h)][1] >= z1 for dx in range(w)):
                faces["down"] = {"uv": uv, "texture": "#pal"}

            elements.append({
                "from": [x0, y0, z0],
                "to": [x1, y1, z1],
                "faces": faces,
            })

    if add_shield_grip:
        uv_g = uv_map[grip_key]
        elements.append({
            "from": [7.25, 6.0, 5.2],
            "to": [8.75, 10.5, 7.5],
            "faces": {d: {"uv": uv_g, "texture": "#pal"} for d in ("north", "south", "east", "west", "up", "down")},
        })

    return elements


# ==============================================================================
# Mathematically Verified Display Transforms (Mojmap 1.20.1 ItemInHandLayer)
# ==============================================================================

WEAPON_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, -90, 10], "translation": [0, 3.6, 1.0], "scale": [0.92, 0.92, 0.92]},
    "thirdperson_lefthand":  {"rotation": [0, 90, -10], "translation": [0, 3.6, 1.0], "scale": [0.92, 0.92, 0.92]},
    "firstperson_righthand": {"rotation": [0, -90, -20], "translation": [1.13, 3.2, 1.13], "scale": [0.72, 0.72, 0.72]},
    "firstperson_lefthand":  {"rotation": [0, 90, 20],  "translation": [1.13, 3.2, 1.13], "scale": [0.72, 0.72, 0.72]},
    "ground": {"rotation": [0, 0, -35], "translation": [0, 3.0, 0], "scale": [0.65, 0.65, 0.65]},
    "gui":    {"rotation": [14, -22, -38], "translation": [0.0, 0.0, 0], "scale": [0.98, 0.98, 0.98]},
    "fixed":  {"rotation": [0, 0, 0], "translation": [0, 0, -0.5], "scale": [1.05, 1.05, 1.05]},
}

BOW_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, -90, 20], "translation": [-1.0, -1.5, 2.0], "scale": [0.92, 0.92, 0.92]},
    "thirdperson_lefthand":  {"rotation": [0, 90, -20], "translation": [-1.0, -1.5, 2.0], "scale": [0.92, 0.92, 0.92]},
    "firstperson_righthand": {"rotation": [0, -90, 15], "translation": [1.13, 2.8, 1.13], "scale": [0.72, 0.72, 0.72]},
    "firstperson_lefthand":  {"rotation": [0, 90, -15], "translation": [1.13, 2.8, 1.13], "scale": [0.72, 0.72, 0.72]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2.5, 0], "scale": [0.65, 0.65, 0.65]},
    "gui":    {"rotation": [12, -20, -28], "translation": [0.0, 0.0, 0], "scale": [1.02, 1.02, 1.02]},
    "fixed":  {"rotation": [0, 0, 0], "translation": [0, 0, -0.5], "scale": [1.05, 1.05, 1.05]},
}

SHIELD_IDLE_DISPLAY = {
    # Remember: ItemTransform.apply negates ry & rz when leftHand=true, so [90, 125, 0] in BOTH
    # thirdperson_righthand and thirdperson_lefthand angles the shield outward-and-forward symmetrically!
    "thirdperson_righthand": {"rotation": [90, 125, 0], "translation": [2.2, -1.5, -1.0], "scale": [1.0, 1.0, 1.0]},
    "thirdperson_lefthand":  {"rotation": [90, 125, 0], "translation": [2.2, -1.5, -1.0], "scale": [1.0, 1.0, 1.0]},
    "firstperson_righthand": {"rotation": [0, 165, 5],  "translation": [-1.5, -2.2, 1.5], "scale": [0.85, 0.85, 0.85]},
    "firstperson_lefthand":  {"rotation": [0, 165, 5],  "translation": [-1.5, -2.2, 1.5], "scale": [0.85, 0.85, 0.85]},
    "ground": {"rotation": [15, 0, 0], "translation": [0, 3.0, 0], "scale": [0.68, 0.68, 0.68]},
    "gui":    {"rotation": [12, -24, 0], "translation": [0, 0.2, 0], "scale": [0.96, 0.96, 0.96]},
    "fixed":  {"rotation": [0, 0, 0], "translation": [0, 0, -0.5], "scale": [1.05, 1.05, 1.05]},
}

SHIELD_BLOCKING_DISPLAY = {
    "thirdperson_righthand": {"rotation": [90, 170, 0], "translation": [-2.5, 3.0, 2.0], "scale": [1.0, 1.0, 1.0]},
    "thirdperson_lefthand":  {"rotation": [90, 170, 0], "translation": [-2.5, 3.0, 2.0], "scale": [1.0, 1.0, 1.0]},
    "firstperson_righthand": {"rotation": [0, 178, -5], "translation": [-4.5, 1.0, 2.0], "scale": [0.92, 0.92, 0.92]},
    "firstperson_lefthand":  {"rotation": [0, 178, -5], "translation": [-4.5, 1.0, 2.0], "scale": [0.92, 0.92, 0.92]},
    "ground": SHIELD_IDLE_DISPLAY["ground"],
    "gui":    SHIELD_IDLE_DISPLAY["gui"],
    "fixed":  SHIELD_IDLE_DISPLAY["fixed"],
}


def write_json(path: Path, obj: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def emit_3d_item(
    item_id: str,
    art32: List[str],
    depth32: Optional[List[str]],
    pal_name: str,
    uv_map: Dict[str, List[float]],
    pal: Dict[str, RGBA],
    display: dict,
    overrides: Optional[List[dict]] = None,
    add_shield_grip: bool = False,
) -> int:
    item_tex_dir = ASSETS / "textures/item"
    model_dir = ASSETS / "models/item"
    save_sprite_png(art32, pal, item_tex_dir / f"{item_id}.png")
    elems = compile_voxel_elements(art32, depth32, uv_map, add_shield_grip=add_shield_grip)
    model = {
        "gui_light": "front",
        "textures": {
            "pal": f"riftworld:item/{pal_name}",
            "particle": f"riftworld:item/{item_id}",
        },
        "elements": elems,
        "display": display,
    }
    if overrides:
        model["overrides"] = overrides
    write_json(model_dir / f"{item_id}.json", model)
    return len(elems)


def write_recipes_and_tags() -> None:
    rec = DATA / "riftworld/recipes"
    rec.mkdir(parents=True, exist_ok=True)

    # Lossless coin <-> aurite_ingot forging (2 coins <-> 1 ingot)
    write_json(rec / "aurite_ingot_from_ducats.json", {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [
            {"item": "riftworld:elisar_ducat"},
            {"item": "riftworld:elisar_ducat"},
        ],
        "result": {"item": "riftworld:aurite_ingot", "count": 1},
    })
    write_json(rec / "elisar_ducat_from_aurite_ingot.json", {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [
            {"item": "riftworld:aurite_ingot"},
        ],
        "result": {"item": "riftworld:elisar_ducat", "count": 2},
    })

    # Elisar Bow & Elisar Shield crafting recipes
    write_json(rec / "elisar_bow.json", {
        "type": "minecraft:crafting_shaped",
        "pattern": [
            " AS",
            "K S",
            " AS",
        ],
        "key": {
            "A": {"item": "riftworld:aurite_ingot"},
            "K": {"item": "minecraft:stick"},
            "S": {"item": "minecraft:string"},
        },
        "result": {"item": "riftworld:elisar_bow", "count": 1},
    })
    write_json(rec / "elisar_shield.json", {
        "type": "minecraft:crafting_shaped",
        "pattern": [
            "APA",
            "AAA",
            " A ",
        ],
        "key": {
            "A": {"item": "riftworld:aurite_ingot"},
            "P": {"tag": "minecraft:planks"},
        },
        "result": {"item": "riftworld:elisar_shield", "count": 1},
    })

    # Standard Minecraft tool/weapon item tags so enchantments & combat mechanics apply natively
    mc_tags = DATA / "minecraft/tags/items"
    mc_tags.mkdir(parents=True, exist_ok=True)
    tag_defs = {
        "swords.json": ["riftworld:aurite_sword", "riftworld:riftcrystal_sword", "riftworld:elisar_guard_sword"],
        "pickaxes.json": ["riftworld:aurite_pickaxe", "riftworld:riftcrystal_pickaxe"],
        "axes.json": ["riftworld:aurite_axe", "riftworld:riftcrystal_axe"],
        "shovels.json": ["riftworld:aurite_shovel", "riftworld:riftcrystal_shovel"],
        "hoes.json": ["riftworld:aurite_hoe", "riftworld:riftcrystal_hoe"],
    }
    for fn, vals in tag_defs.items():
        p = mc_tags / fn
        existing = []
        if p.exists():
            existing = json.loads(p.read_text(encoding="utf-8")).get("values", [])
        merged = list(dict.fromkeys(existing + vals))
        write_json(p, {"replace": False, "values": merged})


def main() -> None:
    item_tex = ASSETS / "textures/item"
    aur_uv = build_palette_png_and_uv_map(AUR_PAL, item_tex / "v048_aurite_pal.png")
    rc_uv = build_palette_png_and_uv_map(RC_PAL, item_tex / "v048_riftcrystal_pal.png")
    grd_uv = build_palette_png_and_uv_map(GUARD_PAL, item_tex / "v048_guard_pal.png")

    counts = {}
    # 1. Aurite & Riftcrystal 2x5 Weapons & Tools
    specs = [
        ("pickaxe", PICKAXE_ART, PICKAXE_DEPTH),
        ("axe", AXE_ART, AXE_DEPTH),
        ("sword", SWORD_ART, SWORD_DEPTH),
        ("shovel", SHOVEL_ART, SHOVEL_DEPTH),
        ("hoe", HOE_ART, HOE_DEPTH),
    ]
    for kind, art, dep in specs:
        counts[f"aurite_{kind}"] = emit_3d_item(
            f"aurite_{kind}", art, dep, "v048_aurite_pal", aur_uv, AUR_PAL, WEAPON_DISPLAY
        )
        counts[f"riftcrystal_{kind}"] = emit_3d_item(
            f"riftcrystal_{kind}", art, dep, "v048_riftcrystal_pal", rc_uv, RC_PAL, WEAPON_DISPLAY
        )

    # 2. Recurve Rift-String Bow + 3 Pulling Stages
    bow_overrides = [
        {"predicate": {"pulling": 1}, "model": "riftworld:item/elisar_bow_pulling_0"},
        {"predicate": {"pulling": 1, "pull": 0.65}, "model": "riftworld:item/elisar_bow_pulling_1"},
        {"predicate": {"pulling": 1, "pull": 0.9}, "model": "riftworld:item/elisar_bow_pulling_2"},
    ]
    counts["elisar_bow"] = emit_3d_item(
        "elisar_bow", BOW_IDLE_ART, None, "v048_aurite_pal", aur_uv, AUR_PAL, BOW_DISPLAY, overrides=bow_overrides
    )
    for stg in (0, 1, 2):
        p_art, p_dep = make_bow_pull_art(stg)
        counts[f"elisar_bow_pulling_{stg}"] = emit_3d_item(
            f"elisar_bow_pulling_{stg}", p_art, p_dep, "v048_aurite_pal", aur_uv, AUR_PAL, BOW_DISPLAY
        )

    # 3. Obsidian-Gold Rift Heater Shield + Blocking
    counts["elisar_shield"] = emit_3d_item(
        "elisar_shield", SHIELD_ART, SHIELD_DEPTH, "v048_aurite_pal", aur_uv, AUR_PAL, SHIELD_IDLE_DISPLAY,
        overrides=[{"predicate": {"blocking": 1}, "model": "riftworld:item/elisar_shield_blocking"}],
        add_shield_grip=True,
    )
    write_json(ASSETS / "models/item/elisar_shield_blocking.json", {
        "parent": "riftworld:item/elisar_shield",
        "display": SHIELD_BLOCKING_DISPLAY,
    })

    # 4. Elisar City Guard Shield + Guard Sword (06_elisar_guard.png)
    counts["elisar_guard_shield"] = emit_3d_item(
        "elisar_guard_shield", GUARD_SHIELD_ART, GUARD_SHIELD_DEPTH, "v048_guard_pal", grd_uv, GUARD_PAL,
        SHIELD_IDLE_DISPLAY,
        overrides=[{"predicate": {"blocking": 1}, "model": "riftworld:item/elisar_guard_shield_blocking"}],
        add_shield_grip=True,
    )
    write_json(ASSETS / "models/item/elisar_guard_shield_blocking.json", {
        "parent": "riftworld:item/elisar_guard_shield",
        "display": SHIELD_BLOCKING_DISPLAY,
    })
    counts["elisar_guard_sword"] = emit_3d_item(
        "elisar_guard_sword", GUARD_SWORD_ART, GUARD_SWORD_DEPTH, "v048_guard_pal", grd_uv, GUARD_PAL, WEAPON_DISPLAY
    )

    write_recipes_and_tags()
    print("OK v0.48.0 3D voxel weapons, tools, bow & shields generated:", counts)


if __name__ == "__main__":
    main()
