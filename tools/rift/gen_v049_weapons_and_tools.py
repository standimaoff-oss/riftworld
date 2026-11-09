#!/usr/bin/env python3
"""
v0.49.0 — Precision 3D Voxel Weapons, Dual-Ore Tool Sets, Recurve Bow, Kite Shields
and TRUE 3D VOLUMETRIC armour item models.

Upgrades over v0.48.0 (matched directly against 03_riftworld_weapon_tools_set.png,
01_aurite_medium_armor.png and 02_riftcrystal_strong_armor.png):
  1) No heavy dark outlines around the gleaming Aurite gold or radiant Riftcrystal
     blades: the cutting edges use sunlit gold and incandescent cyan crystal with a
     5-tier 3D depth relief (razor edge, primary blade, forged cheek, raised
     crossguard / collar, gem bezel, protruding Riftcrystal core).
  2) A 2-voxel shaft with a true (x + y) % 2 diagonal spiral grip wrap on the
     pickaxes, double-bit battleaxes, shovels and hoes.
  3) The Elisar Recurve Bow with multi-depth gold/bronze limbs and an outline-free
     2-voxel glowing cyan + violet energy bowstring (+ 3 pulling stages with a
     golden-bezeled Riftcrystal arrowhead).
  4) A 4-tier 3D relief Elisar Kite Shield and Elisar Guard Shield: faceted
     obsidian / lilac field, stepped golden frame, raised golden boss, protruding
     cyan Riftcrystal and a rear handle.
  5) TRUE 3D volumetric [0..16]^3 element models for all 8 armour items, so the
     dropped armours on the showcase pedestals, the held items and the GUI slots
     render sculpted 3D helmets, cuirasses, greaves and boots instead of flat
     2D cutouts.

The generator writes:
  mod/src/main/resources/assets/riftworld/models/item/*.json
  mod/src/main/resources/assets/riftworld/textures/item/*.png
and renders a software preview into /tmp/preview_v049_weapons_tools.png
"""
from __future__ import annotations

import json
import math
from pathlib import Path
from typing import Dict, List, Optional, Tuple

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "mod/src/main/resources/assets/riftworld"
ITEM_TEX = ASSETS / "textures/item"
ITEM_MODELS = ASSETS / "models/item"

RGBA = Tuple[int, int, int, int]


def hex_rgba(h: str, a: int = 255) -> RGBA:
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


# ==============================================================================
# Palettes
# ==============================================================================

AUR_PAL: Dict[str, RGBA] = {
    "k": hex_rgba("#24170B"),   # deep ironwood / dark recess
    "b": hex_rgba("#3E2813"),   # dark forged bronze / haft shadow
    "m": hex_rgba("#5C4224"),   # weathered olive-bronze cheek
    "w": hex_rgba("#87612B"),   # warm bronze bevel / leather wrap
    "g": hex_rgba("#C68924"),   # rich forged Aurite gold
    "G": hex_rgba("#ECB946"),   # bright Aurite gold blade
    "H": hex_rgba("#FCE48E"),   # sunlit specular gold cutting edge
    "t": hex_rgba("#2E645F"),   # oxidised patina teal shade
    "T": hex_rgba("#468780"),   # oxidised patina teal highlight
    "c": hex_rgba("#159E9E"),   # deep cyan crystal bevel
    "C": hex_rgba("#30ECE4"),   # glowing cyan Riftcrystal
    "W": hex_rgba("#B8FFFC"),   # incandescent white-cyan glint
    "p": hex_rgba("#8A38CC"),   # arcane violet spark shade (G = 56)
    "P": hex_rgba("#DA6EFF"),   # bright violet energy string spark (G = 110)
    "o": hex_rgba("#141822"),   # dark faceted obsidian shield field
    "O": hex_rgba("#222838"),   # lit obsidian shield facet
    "f": hex_rgba("#F2EFEA"),   # arrow fletching white
    "F": hex_rgba("#C5C0C8"),   # arrow fletching shade
}

RC_PAL: Dict[str, RGBA] = {
    "k": hex_rgba("#0D1119"),   # deepest slate recess
    "b": hex_rgba("#1A2230"),   # dark slate-iron haft/spine
    "m": hex_rgba("#2A3548"),   # mid faceted slate-iron cheek
    "w": hex_rgba("#41506A"),   # lit slate-iron bevel / spiral wrap
    "g": hex_rgba("#129A9C"),   # deep rift-cyan crystal bevel
    "G": hex_rgba("#2AE6DF"),   # vivid glowing cyan crystal blade
    "H": hex_rgba("#B8FFFC"),   # razor white-cyan crystal edge
    "c": hex_rgba("#8B34BC"),   # deep rift-magenta bevel (G = 52)
    "C": hex_rgba("#D656FA"),   # glowing rift-magenta crystal vein (G = 86)
    "W": hex_rgba("#F8A8FF"),   # incandescent magenta-white spark (G = 168)
}

GUARD_PAL: Dict[str, RGBA] = {
    "k": hex_rgba("#231A16"),
    "b": hex_rgba("#4A331C"),
    "w": hex_rgba("#76522C"),
    "g": hex_rgba("#AD6F2B"),
    "G": hex_rgba("#DC9842"),
    "H": hex_rgba("#F4C474"),
    "s": hex_rgba("#6E7C8E"),   # steel bevel
    "S": hex_rgba("#A8B7C8"),   # steel cheek
    "E": hex_rgba("#E2ECF6"),   # steel razor edge
    "l": hex_rgba("#8A6B8F"),   # Elisar lilac shade
    "L": hex_rgba("#AC8CAF"),   # Elisar lilac shield field
    "t": hex_rgba("#355E59"),   # seafoam-teal shade
    "T": hex_rgba("#528880"),   # seafoam-teal highlight
}


# ==============================================================================
# 5-tier depth map
# ==============================================================================
# '0': razor cutting edge / recessed fuller     [7.62 .. 8.38]
# '1': primary forged blade / haft               [7.38 .. 8.62]
# '2': raised crossguard, collar, rim            [7.02 .. 8.98]
# '3': 3D shield boss & gem bezel                [6.65 .. 9.35]
# '4': protruding glowing Riftcrystal core       [6.30 .. 9.70]

DEPTH_Z: Dict[str, Tuple[float, float]] = {
    "0": (7.62, 8.38),
    "1": (7.38, 8.62),
    "2": (7.02, 8.98),
    "3": (6.65, 9.35),
    "4": (6.30, 9.70),
}


def pad32(rows: List[str]) -> List[str]:
    """Pads a grid to exactly 32x32, centring it."""
    h = len(rows)
    widths = [len(r) for r in rows]
    assert len(set(widths)) == 1, f"rows have different widths: {widths}"
    w = widths[0]
    assert h <= 32 and w <= 32, f"Grid {w}x{h} exceeds 32x32"
    top = (32 - h) // 2
    left = (32 - w) // 2
    out = ["." * 32 for _ in range(top)]
    for r in rows:
        out.append("." * left + r + "." * (32 - left - w))
    while len(out) < 32:
        out.append("." * 32)
    return out


# ==============================================================================
# Art grids (each cell = 1/32 of the item icon; the depth grid adds the 3rd axis)
# ==============================================================================

# --- PICKAXE: gull-wing golden arch, cyan socket gem, 2px diagonal-wrapped haft ---
PICKAXE_ART = pad32([
    ".........HH.........",
    ".......gGHHGg.......",
    ".....gGHccHGGg......",
    "....gGgmCWmgHGg.....",
    "...GHg.mbwm.gGHG....",
    "..GHG...gGGg...GHG..",
    ".GHg.....bw.....gHG.",
    "GHg......wb......gHG",
    "Hg.......bw.......gH",
    "g........wb........g",
    ".........wb.........",
    ".........wb.........",
    ".........wb.........",
    ".........wb.........",
    ".........wb.........",
    ".........wb.........",
    ".........wb.........",
    ".........wb.........",
    ".........wb.........",
    ".........wb.........",
    ".........wb.........",
    ".........wb.........",
    "........gGGg........",
    ".......gGccGg.......",
    ".......gGCWGg.......",
    "........gGGg........",
    ".........bb.........",
])

PICKAXE_DEPTH = pad32([
    ".........00.........",
    ".......011110.......",
    ".....0112332110.....",
    "....011224422110....",
    "...0110.2112.0110...",
    "..010...2222...010..",
    ".010.....11.....010.",
    "010......11......010",
    "00.......11.......00",
    "0........11........0",
    ".........11.........",
    ".........11.........",
    ".........11.........",
    ".........11.........",
    ".........11.........",
    ".........11.........",
    ".........11.........",
    ".........11.........",
    ".........11.........",
    ".........11.........",
    ".........11.........",
    ".........11.........",
    "........2222........",
    ".......223322.......",
    ".......224422.......",
    "........2222........",
    ".........11.........",
])

# --- DOUBLE-BIT BATTLEAXE: dark forged cheeks, golden crescent edges, 2x2 cyan gem ---
AXE_ART = pad32([
    "...HH............HH...",
    "..GHGg....GG....gGHG..",
    ".GHGGmg..gHHg..gmGGHG.",
    "GHGGmmbggGccGggbmmGGHG",
    "HGGmmmbbmGCWGmbbmmmGGH",
    "HGGmmmbbmGCWGmbbmmmGGH",
    "GHGGmmbggGccGggbmmGGHG",
    ".GHGGmg..gGGg..gmGGHG.",
    "..GHGg....bw....gGHG..",
    "...HH.....wb.....HH...",
    "....g.....wb.....g....",
    "..........wb..........",
    "..........wb..........",
    "..........wb..........",
    "..........wb..........",
    "..........wb..........",
    "..........wb..........",
    "..........wb..........",
    "..........wb..........",
    "..........wb..........",
    "..........wb..........",
    "..........wb..........",
    ".........gGGg.........",
    "........gGCWGg........",
    ".........gGGg.........",
    "..........bb..........",
])

AXE_DEPTH = pad32([
    "...00............00...",
    "..0110....22....0110..",
    ".011110..2222..011110.",
    "0111111223333221111110",
    "0111112223443222111110",
    "0111112223443222111110",
    "0111111223333221111110",
    ".011110..2222..011110.",
    "..0110....11....0110..",
    "...00.....11.....00...",
    "....0.....11.....0....",
    "..........11..........",
    "..........11..........",
    "..........11..........",
    "..........11..........",
    "..........11..........",
    "..........11..........",
    "..........11..........",
    "..........11..........",
    "..........11..........",
    "..........11..........",
    "..........11..........",
    ".........2222.........",
    "........234432........",
    ".........2222.........",
    "..........11..........",
])

# --- BROADSWORD: sculpted fuller, outline-free golden edges, gemmed crossguard ---
SWORD_ART = pad32([
    "......H......",
    ".....GHG.....",
    "....GHHHG....",
    "....GHgHG....",
    "....GHmHG....",
    "....GGwGG....",
    "....GHmHG....",
    "....GGgGG....",
    "....GHmHG....",
    "....GGwGG....",
    "....GHmHG....",
    "....GGgGG....",
    "....GHmHG....",
    "....GGwGG....",
    "....GHmHG....",
    "....GGgGG....",
    "....GHgHG....",
    ".HHgGGGGGgHH.",
    "GHGGGcCWGGGHC",
    ".ggbggCggbgg.",
    ".....bwb.....",
    ".....wbw.....",
    ".....bwb.....",
    ".....wbw.....",
    "....gGGGg....",
    "...gGCWCGg...",
    "....gGGGg....",
    ".....gGg.....",
])

SWORD_DEPTH = pad32([
    "......0......",
    ".....010.....",
    "....01110....",
    "....01010....",
    "....01010....",
    "....01010....",
    "....01010....",
    "....01010....",
    "....01010....",
    "....01010....",
    "....01010....",
    "....01010....",
    "....01010....",
    "....01010....",
    "....01010....",
    "....01010....",
    "....01110....",
    ".22222322222.",
    "2222344432222",
    ".22223332222.",
    ".....111.....",
    ".....111.....",
    ".....111.....",
    ".....111.....",
    "....22222....",
    "...2344432...",
    "....22222....",
    ".....222.....",
])

# --- SPADE SHOVEL: crowned spade blade, gemmed socket collar, 2px spiral shaft ---
SHOVEL_ART = pad32([
    ".....HH.....",
    "...gGHHGg...",
    "..GHGGGGHG..",
    ".GHGGmmGGHG.",
    ".GHGGwmGGHG.",
    ".GHGGmmGGHG.",
    "..GHGGGGHG..",
    "...gGccGg...",
    "...gGCWGg...",
    "....gGGg....",
    ".....bw.....",
    ".....wb.....",
    ".....bw.....",
    ".....wb.....",
    ".....bw.....",
    ".....wb.....",
    ".....bw.....",
    ".....wb.....",
    ".....bw.....",
    ".....wb.....",
    ".....bw.....",
    ".....wb.....",
    "....gGGg....",
    "...gGCWGg...",
    "....gGGg....",
    ".....bb.....",
])

SHOVEL_DEPTH = pad32([
    ".....00.....",
    "...011110...",
    "..01111110..",
    ".0111001110.",
    ".0111001110.",
    ".0111001110.",
    "..01111110..",
    "...223322...",
    "...234432...",
    "....2222....",
    ".....11.....",
    ".....11.....",
    ".....11.....",
    ".....11.....",
    ".....11.....",
    ".....11.....",
    ".....11.....",
    ".....11.....",
    ".....11.....",
    ".....11.....",
    ".....11.....",
    ".....11.....",
    "....2222....",
    "...234432...",
    "....2222....",
    ".....11.....",
])

# --- SCYTHE-HOE: sweeping curved golden blade, gemmed socket, 2px spiral shaft ---
HOE_ART = pad32([
    "......HHHHHH....",
    "....gGHHHHHHGg..",
    "..gGHGGGggGCWGg.",
    ".GHGGmg...gGccGg",
    "GHGGg......gGGg.",
    "GHGg........bw..",
    "GHg.........wb..",
    "Hg..........bw..",
    "............wb..",
    "............wb..",
    "............wb..",
    "............wb..",
    "............wb..",
    "............wb..",
    "............wb..",
    "............wb..",
    "............wb..",
    "............wb..",
    "............wb..",
    "............wb..",
    "............wb..",
    "...........gGGg.",
    "..........gGCWGg",
    "...........gGGg.",
    "............bb..",
])

HOE_DEPTH = pad32([
    "......000000....",
    "....0111111122..",
    "..0111112234432.",
    ".011110...233322",
    "01110......2222.",
    "0110........11..",
    "010.........11..",
    "00..........11..",
    "............11..",
    "............11..",
    "............11..",
    "............11..",
    "............11..",
    "............11..",
    "............11..",
    "............11..",
    "............11..",
    "............11..",
    "............11..",
    "............11..",
    "............11..",
    "...........2222.",
    "..........234432",
    "...........2222.",
    "............11..",
])

# --- ELISAR RECURVE BOW: gold/bronze limbs + 2-voxel cyan & violet energy string ---
BOW_IDLE_ART = pad32([
    ".....gHHg.......",
    "...CWgGGGgg.....",
    "...CC..bmGGGg...",
    "...CP....bmGGg..",
    "...WC.....bmGHg.",
    "...CC......bmGHg",
    "...CP.......bmGG",
    "...WC.......gGGG",
    "...CC......gGHGG",
    "...CP.....gGCWGG",
    "...WC.....gGCWGG",
    "...CC......gGHGG",
    "...CP.......gGGG",
    "...WC.......bmGG",
    "...CC......bmGHg",
    "...CP.....bmGHg.",
    "...WC....bmGGg..",
    "...CC..bmGGGg...",
    "...CWgGGGgg.....",
    ".....gHHg.......",
])

BOW_IDLE_DEPTH = pad32([
    ".....2222.......",
    "...33222211.....",
    "...00..112221...",
    "...00....11221..",
    "...00.....11221.",
    "...00......11221",
    "...00.......1122",
    "...00.......2222",
    "...00......22222",
    "...00.....234432",
    "...00.....234432",
    "...00......22222",
    "...00.......2222",
    "...00.......1122",
    "...00......11221",
    "...00.....11221.",
    "...00....11221..",
    "...00..112221...",
    "...33222211.....",
    ".....2222.......",
])


def make_bow_pull_art(stage: int) -> Tuple[List[str], List[str]]:
    """Stages 0/1/2 bend the 2-voxel cyan+violet energy string further -X and seat an arrow."""
    pull_shift = [2, 4, 6][stage]
    base_art = [list(r) for r in BOW_IDLE_ART]
    base_dep = [list(r) for r in BOW_IDLE_DEPTH]
    anchors = []
    for y in range(32):
        for x in range(32):
            if base_art[y][x] in ("C", "W", "P") and x < 15:
                anchors.append((x, y))
                base_art[y][x] = "."
                base_dep[y][x] = "."
    top_y = min(y for _, y in anchors)
    bot_y = max(y for _, y in anchors)
    mid_y = (top_y + bot_y) // 2
    base_x = min(x for x, y in anchors if y == top_y)

    for y in range(top_y, bot_y + 1):
        t = 1.0 - abs(y - mid_y) / max(1.0, (bot_y - top_y) * 0.5)
        sx = max(1, int(round(base_x - pull_shift * t)))
        c1 = "W" if (y % 3 == 0) else "C"
        c2 = "P" if (y % 2 == 1) else "C"
        base_art[y][sx] = c1
        base_art[y][sx + 1] = c2
        base_dep[y][sx] = "0"
        base_dep[y][sx + 1] = "0"

    nock_x = max(1, base_x - pull_shift)
    tip_x = min(30, nock_x + 19)
    for fy in (mid_y - 1, mid_y + 2):
        base_art[fy][nock_x + 1] = "f"
        base_art[fy][nock_x + 2] = "F"
        base_dep[fy][nock_x + 1] = "1"
        base_dep[fy][nock_x + 2] = "1"
    for ax in range(nock_x + 1, tip_x - 2):
        base_art[mid_y][ax] = "w" if (ax % 2 == 0) else "g"
        base_dep[mid_y][ax] = "2"
    for dy in (-1, 0, 1):
        base_art[mid_y + dy][tip_x - 2] = "G"
        base_dep[mid_y + dy][tip_x - 2] = "2"
    base_art[mid_y][tip_x - 1] = "C"
    base_dep[mid_y][tip_x - 1] = "3"
    base_art[mid_y][tip_x] = "W"
    base_dep[mid_y][tip_x] = "3"

    return ["".join(r) for r in base_art], ["".join(r) for r in base_dep]


# --- ELISAR KITE SHIELD: obsidian field, stepped golden frame, gold boss, cyan gem ---
SHIELD_ART = pad32([
    "..HHHHHHHH..HHHHHHHH..",
    ".HGGGGGGGGHHGGGGGGGGH.",
    "HGGggggggggggggggggGGH",
    "HGgooooOOooooooOOooGGH",
    "HGgooOOooooooooooOOgGH",
    "HGgooooooooooooooooGGH",
    "HGgoooogGGGGGGgooooGGH",
    "HGgooogGHHHHHHGgoooGGH",
    "HGgoogGHGGccGGHGgooGGH",
    "HGgoogGHGGCWGGHGgooGGH",
    "HGgoogGHGGWCGGHGgooGGH",
    "HGgoogGHGGccGGHGgooGGH",
    "HGgooogGHHHHHHGgoooGGH",
    "HGgoooogGGGGGGgooooGGH",
    ".HGgoooooOOoooooooGGH.",
    ".HGGooooooooooooooGGH.",
    "..HGgoooooooooooogGH..",
    "..HGGooooooooooooGGH..",
    "...HGgoooooooooogGH...",
    "...HGGooooooooooGGH...",
    "....HGgoooooooogGH....",
    ".....HGGooooooGGH.....",
    "......HGggggggGH......",
    ".......HGGGGGGH.......",
    "........HHHHHH........",
])

SHIELD_DEPTH = pad32([
    "..22222222..22222222..",
    ".22222222222222222222.",
    "2222222222222222222222",
    "2221111111111111111222",
    "2221111111111111111222",
    "2221111111111111111222",
    "2221111233333321111222",
    "2221112333333332111222",
    "2221123333443333211222",
    "2221123333443333211222",
    "2221123333443333211222",
    "2221123333443333211222",
    "2221112333333332111222",
    "2221111233333321111222",
    ".22211111111111111222.",
    ".22211111111111111222.",
    "..222111111111111222..",
    "..222111111111111222..",
    "...2221111111111222...",
    "...2221111111111222...",
    "....22211111111222....",
    ".....222111111222.....",
    "......2222222222......",
    ".......22222222.......",
    "........222222........",
])

# --- ELISAR CITY GUARD SHIELD & SWORD (06_elisar_guard.png) ---
GUARD_SHIELD_ART = pad32([
    "..EEEEEEEE..EEEEEEEE..",
    ".ESSSSSSSSEESSSSSSSSE.",
    "ESSssssssssssssssssSSE",
    "ESSllllLLllllllLLllSSE",
    "ESSllLLllllllllllLLsSE",
    "ESSllllllllllllllllSSE",
    "ESSllllgGGGGGGgllllSSE",
    "ESSlllgGHHHHHHGglllSSE",
    "ESSllgGHGGttGGHGgllSSE",
    "ESSllgGHGGTTGGHGgllSSE",
    "ESSllgGHGGTTGGHGgllSSE",
    "ESSllgGHGGttGGHGgllSSE",
    "ESSlllgGHHHHHHGglllSSE",
    "ESSllllgGGGGGGgllllSSE",
    ".ESSllllllllllllllSSE.",
    ".ESSllllllllllllllSSE.",
    "..ESSllllllllllllSSE..",
    "..ESSllllllllllllSSE..",
    "...ESSllllllllllSSE...",
    "...ESSllllllllllSSE...",
    "....ESSllllllllSSE....",
    ".....ESSllllllSSE.....",
    "......ESSssssSSE......",
    ".......ESSSSSSE.......",
    "........EEEEEE........",
])

GUARD_SWORD_ART = pad32([
    "......E......",
    ".....ESE.....",
    "....ESSSE....",
    "....ESsSE....",
    "....ESsSE....",
    "....ESSSE....",
    "....ESsSE....",
    "....ESSSE....",
    "....ESsSE....",
    "....ESSSE....",
    "....ESsSE....",
    "....ESSSE....",
    "....ESsSE....",
    "....ESsSE....",
    "....ESsSE....",
    "....ESsSE....",
    "....ESsSE....",
    ".HHgGGGGGgHH.",
    "GHGGGtTTGGGHG",
    ".ggbggTggbgg.",
    ".....bwb.....",
    ".....wbw.....",
    ".....bwb.....",
    ".....wbw.....",
    "....gGGGg....",
    "...gGHTHGg...",
    "....gGGGg....",
    ".....gGg.....",
])

GUARD_SWORD_DEPTH = SWORD_DEPTH


# ==============================================================================
# Palette atlas + UV map
# ==============================================================================

def build_palette_png_and_uv_map(pal: Dict[str, RGBA], out_png: Path) -> Dict[str, List[float]]:
    img = np.zeros((64, 64, 4), dtype=np.uint8)
    img[:, :] = pal[list(pal.keys())[0]]
    uv_map: Dict[str, List[float]] = {}
    for idx, k in enumerate(pal.keys()):
        gx = (idx % 8) * 8
        gy = (idx // 8) * 8
        img[gy:gy + 8, gx:gx + 8] = pal[k]
        uv_map[k] = [(gx + 2), (gy + 2), (gx + 6), (gy + 6)]
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


# ==============================================================================
# Voxel element compilers
# ==============================================================================

def compile_voxel_elements(
    art32: List[str],
    depth32: Optional[List[str]],
    uv_map: Dict[str, List[float]],
    add_shield_grip: bool = False,
    grip_key: str = "b",
) -> List[dict]:
    """Greedy 2D merge of the art grid, extruded along Z using the 5-tier depth map."""
    if depth32 is None:
        depth32 = ["".join("1" if c != "." else "." for c in row) for row in art32]

    zspan: Dict[Tuple[int, int], Tuple[float, float]] = {}
    for py in range(32):
        for px in range(32):
            if art32[py][px] != ".":
                zspan[(px, py)] = DEPTH_Z.get(depth32[py][px], (7.38, 8.62))

    visited = [[False] * 32 for _ in range(32)]
    elements: List[dict] = []

    for py in range(32):
        for px in range(32):
            if visited[py][px] or art32[py][px] == ".":
                continue
            sym = art32[py][px]
            dk = depth32[py][px]
            z0, z1 = DEPTH_Z.get(dk, (7.38, 8.62))

            w = 1
            while px + w < 32 and not visited[py][px + w] and art32[py][px + w] == sym and depth32[py][px + w] == dk:
                w += 1
            h = 1
            while py + h < 32:
                if any(
                    visited[py + h][px + dx] or art32[py + h][px + dx] != sym or depth32[py + h][px + dx] != dk
                    for dx in range(w)
                ):
                    break
                h += 1

            for dy in range(h):
                for dx in range(w):
                    visited[py + dy][px + dx] = True

            x0 = round(px * 0.5, 3)
            x1 = round((px + w) * 0.5, 3)
            y0 = round((32 - (py + h)) * 0.5, 3)
            y1 = round((32 - py) * 0.5, 3)

            uv = uv_map[sym]
            faces: Dict[str, dict] = {"north": {"uv": uv, "texture": "#pal"}, "south": {"uv": uv, "texture": "#pal"}}
            if not all((px - 1, py + dy) in zspan and zspan[(px - 1, py + dy)][0] <= z0 and zspan[(px - 1, py + dy)][1] >= z1 for dy in range(h)):
                faces["west"] = {"uv": uv, "texture": "#pal"}
            if not all((px + w, py + dy) in zspan and zspan[(px + w, py + dy)][0] <= z0 and zspan[(px + w, py + dy)][1] >= z1 for dy in range(h)):
                faces["east"] = {"uv": uv, "texture": "#pal"}
            if not all((px + dx, py - 1) in zspan and zspan[(px + dx, py - 1)][0] <= z0 and zspan[(px + dx, py - 1)][1] >= z1 for dx in range(w)):
                faces["up"] = {"uv": uv, "texture": "#pal"}
            if not all((px + dx, py + h) in zspan and zspan[(px + dx, py + h)][0] <= z0 and zspan[(px + dx, py + h)][1] >= z1 for dx in range(w)):
                faces["down"] = {"uv": uv, "texture": "#pal"}

            elements.append({"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": faces})

    if add_shield_grip:
        uv_g = uv_map[grip_key]
        elements.append({
            "from": [7.25, 6.0, 5.2],
            "to": [8.75, 10.5, 7.38],
            "faces": {d: {"uv": uv_g, "texture": "#pal"} for d in ("north", "south", "east", "west", "up", "down")},
        })

    return elements


def compile_3d_grid_elements(grid: Dict[Tuple[int, int, int], str], uv_map: Dict[str, List[float]],
                             step: float = 0.5) -> List[dict]:
    """Greedy 3D box merger for true volumetric armour items in the [0..16]^3 model space."""
    if not grid:
        return []
    visited = set()
    elements: List[dict] = []

    min_x = min(p[0] for p in grid)
    max_x = max(p[0] for p in grid)
    min_y = min(p[1] for p in grid)
    max_y = max(p[1] for p in grid)
    min_z = min(p[2] for p in grid)
    max_z = max(p[2] for p in grid)

    for z in range(min_z, max_z + 1):
        for y in range(min_y, max_y + 1):
            for x in range(min_x, max_x + 1):
                if (x, y, z) not in grid or (x, y, z) in visited:
                    continue
                sym = grid[(x, y, z)]
                w = 1
                while (x + w, y, z) in grid and (x + w, y, z) not in visited and grid[(x + w, y, z)] == sym:
                    w += 1
                h = 1
                while all((x + dx, y + h, z) in grid and (x + dx, y + h, z) not in visited and grid[(x + dx, y + h, z)] == sym for dx in range(w)):
                    h += 1
                d = 1
                while all(
                    (x + dx, y + dy, z + d) in grid and (x + dx, y + dy, z + d) not in visited and grid[(x + dx, y + dy, z + d)] == sym
                    for dy in range(h) for dx in range(w)
                ):
                    d += 1
                for dz in range(d):
                    for dy in range(h):
                        for dx in range(w):
                            visited.add((x + dx, y + dy, z + dz))

                uv = uv_map[sym]
                faces: Dict[str, dict] = {}
                if not all((x - 1, y + dy, z + dz) in grid for dy in range(h) for dz in range(d)):
                    faces["west"] = {"uv": uv, "texture": "#pal"}
                if not all((x + w, y + dy, z + dz) in grid for dy in range(h) for dz in range(d)):
                    faces["east"] = {"uv": uv, "texture": "#pal"}
                if not all((x + dx, y - 1, z + dz) in grid for dx in range(w) for dz in range(d)):
                    faces["down"] = {"uv": uv, "texture": "#pal"}
                if not all((x + dx, y + h, z + dz) in grid for dx in range(w) for dz in range(d)):
                    faces["up"] = {"uv": uv, "texture": "#pal"}
                if not all((x + dx, y + dy, z - 1) in grid for dx in range(w) for dy in range(h)):
                    faces["north"] = {"uv": uv, "texture": "#pal"}
                if not all((x + dx, y + dy, z + d) in grid for dx in range(w) for dy in range(h)):
                    faces["south"] = {"uv": uv, "texture": "#pal"}

                if faces:
                    x0 = round(x * step, 3)
                    y0 = round(y * step, 3)
                    z0 = round(z * step, 3)
                    x1 = round((x + w) * step, 3)
                    y1 = round((y + h) * step, 3)
                    z1 = round((z + d) * step, 3)
                    assert 0.0 <= x0 and x1 <= 16.0 and 0.0 <= y0 and y1 <= 16.0 and 0.0 <= z0 and z1 <= 16.0, (
                        f"3D box out of [0..16]: {[x0, y0, z0]}..{[x1, y1, z1]}")
                    elements.append({"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": faces})

    return elements


# ==============================================================================
# Volumetric 3D armour builders (voxel coordinates 0..31 = model units 0..16)
# Front of each piece faces +Z (south) so the GUI and the ground show the front.
# ==============================================================================

def set_box(g, x0, y0, z0, x1, y1, z1, sym):
    for z in range(z0, z1):
        for y in range(y0, y1):
            for x in range(x0, x1):
                g[(x, y, z)] = sym


def del_box(g, x0, y0, z0, x1, y1, z1):
    for z in range(z0, z1):
        for y in range(y0, y1):
            for x in range(x0, x1):
                g.pop((x, y, z), None)


def build_3d_helmet_grid(is_rc: bool) -> Dict[Tuple[int, int, int], str]:
    g: Dict[Tuple[int, int, int], str] = {}
    body = "m"
    trim = "G" if not is_rc else "w"
    # Outer helmet shell: x 8..24 (16w), y 6..22 (16h), z 8..24 (16d)
    set_box(g, 8, 6, 8, 24, 22, 24, body)
    del_box(g, 10, 6, 10, 22, 20, 22)
    set_box(g, 10, 7, 20, 22, 18, 22, "k")
    # Carve the front T-visor opening through z 22..24
    del_box(g, 10, 14, 22, 22, 16, 24)
    del_box(g, 14, 6, 22, 18, 15, 24)
    # Brow band + cheek borders
    set_box(g, 8, 16, 8, 24, 18, 25, trim)
    set_box(g, 13, 6, 23, 14, 16, 24, trim)
    set_box(g, 18, 6, 23, 19, 16, 24, trim)
    if not is_rc:
        set_box(g, 14, 17, 7, 18, 24, 25, "G")
        set_box(g, 15, 18, 8, 17, 25, 25, "H")
        set_box(g, 7, 15, 11, 8, 19, 20, "G")
        set_box(g, 24, 15, 11, 25, 19, 20, "G")
        for ly in (8, 10, 12):
            set_box(g, 9, ly, 23, 13, ly + 1, 24, "k")
            set_box(g, 19, ly, 23, 23, ly + 1, 24, "k")
    else:
        set_box(g, 11, 14, 21, 21, 16, 22, "G")
        set_box(g, 12, 15, 21, 20, 16, 22, "H")
        set_box(g, 15, 8, 21, 17, 15, 22, "G")
        set_box(g, 15, 22, 9, 17, 25, 23, "G")
        set_box(g, 15, 24, 11, 17, 26, 21, "H")
        for step in range(5):
            set_box(g, 6, 18 + step, 17 - step * 2, 8, 21 + step, 20 - step * 2, "H" if step >= 3 else ("G" if step >= 1 else "C"))
            set_box(g, 24, 18 + step, 17 - step * 2, 26, 21 + step, 20 - step * 2, "H" if step >= 3 else ("G" if step >= 1 else "C"))
        g[(11, 11, 23)] = "C"
        g[(20, 11, 23)] = "C"
        g[(15, 18, 24)] = "H"
        g[(16, 18, 24)] = "H"
    return g


def build_3d_chestplate_grid(is_rc: bool) -> Dict[Tuple[int, int, int], str]:
    g: Dict[Tuple[int, int, int], str] = {}
    body = "m"
    set_box(g, 9, 4, 11, 23, 22, 21, body)
    del_box(g, 11, 4, 13, 21, 22, 19)
    del_box(g, 13, 19, 19, 19, 22, 21)
    set_box(g, 3, 14, 10, 10, 22, 22, body)
    del_box(g, 4, 14, 12, 9, 19, 20)
    set_box(g, 22, 14, 10, 29, 22, 22, body)
    del_box(g, 23, 14, 12, 28, 19, 20)

    if not is_rc:
        set_box(g, 3, 14, 10, 10, 16, 22, "G")
        set_box(g, 22, 14, 10, 29, 16, 22, "G")
        set_box(g, 4, 22, 11, 9, 24, 21, "H")
        set_box(g, 23, 22, 11, 28, 24, 21, "H")
        set_box(g, 11, 11, 21, 21, 20, 22, "m")
        for s in range(4):
            set_box(g, 11 + s, 18 - s, 22, 13 + s, 20 - s, 23, "H" if s == 0 else "G")
            set_box(g, 19 - s, 18 - s, 22, 21 - s, 20 - s, 23, "H" if s == 0 else "G")
        set_box(g, 14, 13, 22, 18, 17, 23, "H")
        set_box(g, 15, 14, 22, 17, 16, 24, "T")
        set_box(g, 9, 7, 11, 23, 10, 21, "T")
        set_box(g, 9, 4, 10, 23, 7, 22, "b")
        set_box(g, 14, 4, 21, 18, 7, 23, "G")
        set_box(g, 15, 5, 22, 17, 6, 23, "H")
    else:
        set_box(g, 4, 22, 14, 7, 27, 18, "G")
        set_box(g, 4, 25, 15, 6, 28, 17, "H")
        set_box(g, 25, 22, 14, 28, 27, 18, "G")
        set_box(g, 26, 25, 15, 28, 28, 17, "H")
        set_box(g, 5, 16, 21, 7, 21, 23, "G")
        set_box(g, 25, 16, 21, 27, 21, 23, "G")
        set_box(g, 15, 7, 21, 17, 19, 23, "H")
        for ry in (9, 13, 17):
            set_box(g, 11, ry, 21, 21, ry + 2, 22, "G")
            set_box(g, 11, ry + 1, 21, 13, ry + 2, 23, "C")
            set_box(g, 19, ry + 1, 21, 21, ry + 2, 23, "C")
        set_box(g, 9, 4, 10, 23, 6, 22, "w")
    return g


def build_3d_leggings_grid(is_rc: bool) -> Dict[Tuple[int, int, int], str]:
    g: Dict[Tuple[int, int, int], str] = {}
    set_box(g, 9, 19, 12, 23, 24, 20, "b" if not is_rc else "w")
    del_box(g, 11, 19, 14, 21, 24, 18)
    set_box(g, 14, 20, 19, 18, 23, 21, "G")

    for lx0, tasset_x in [(9, 8), (17, 23)]:
        set_box(g, lx0, 4, 12, lx0 + 6, 19, 20, "m")
        del_box(g, lx0 + 1, 4, 13, lx0 + 5, 19, 19)
        if not is_rc:
            set_box(g, lx0, 12, 12, lx0 + 6, 18, 20, "T")
            del_box(g, lx0 + 1, 12, 13, lx0 + 5, 18, 19)
            set_box(g, tasset_x, 12, 12, tasset_x + 1, 19, 20, "G")
            set_box(g, lx0 + 1, 7, 20, lx0 + 5, 12, 22, "G")
            set_box(g, lx0 + 2, 8, 21, lx0 + 4, 11, 23, "H")
        else:
            set_box(g, tasset_x, 12, 12, tasset_x + 1, 19, 20, "w")
            set_box(g, lx0 + 2, 5, 19, lx0 + 4, 18, 21, "G")
            set_box(g, lx0 + 1, 7, 20, lx0 + 5, 12, 22, "w")
            set_box(g, lx0 + 2, 8, 21, lx0 + 4, 11, 23, "H")
            g[(lx0 + 1, 15, 20)] = "C"
    return g


def build_3d_boots_grid(is_rc: bool) -> Dict[Tuple[int, int, int], str]:
    g: Dict[Tuple[int, int, int], str] = {}
    for lx0 in (8, 17):
        set_box(g, lx0, 4, 11, lx0 + 7, 15, 20, "m")
        del_box(g, lx0 + 1, 6, 12, lx0 + 6, 15, 19)
        if not is_rc:
            set_box(g, lx0, 13, 11, lx0 + 7, 15, 20, "G")
            del_box(g, lx0 + 1, 13, 12, lx0 + 6, 15, 19)
            set_box(g, lx0, 4, 11, lx0 + 7, 5, 24, "b")
            set_box(g, lx0, 5, 19, lx0 + 7, 9, 24, "G")
            set_box(g, lx0 + 1, 6, 20, lx0 + 6, 9, 24, "H")
        else:
            set_box(g, lx0, 13, 11, lx0 + 7, 15, 20, "w")
            del_box(g, lx0 + 1, 13, 12, lx0 + 6, 15, 19)
            set_box(g, lx0, 4, 11, lx0 + 7, 9, 24, "b")
            set_box(g, lx0 + 1, 5, 19, lx0 + 6, 9, 24, "w")
            set_box(g, lx0 + 3, 5, 20, lx0 + 4, 14, 21, "G")
            set_box(g, lx0 + 3, 6, 23, lx0 + 4, 9, 25, "H")
            g[(lx0 + 1, 11, 20)] = "C"
    return g


def project_3d_grid_to_sprite32(grid: Dict[Tuple[int, int, int], str]) -> List[str]:
    rows = [["." for _ in range(32)] for _ in range(32)]
    for (x, y, z), sym in sorted(grid.items(), key=lambda kv: kv[0][2]):
        py = 31 - y
        if 0 <= x < 32 and 0 <= py < 32:
            rows[py][x] = sym
    return ["".join(r) for r in rows]


# ==============================================================================
# Display transforms (Mojmap 1.20.1 verified)
# ==============================================================================

WEAPON_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, -90, -42], "translation": [0, 3.2, 1.8], "scale": [0.92, 0.92, 0.92]},
    "thirdperson_lefthand":  {"rotation": [0, 90, 42],   "translation": [0, 3.2, 1.8], "scale": [0.92, 0.92, 0.92]},
    "firstperson_righthand": {"rotation": [0, -90, 20],  "translation": [1.13, 3.2, 1.13], "scale": [0.72, 0.72, 0.72]},
    "firstperson_lefthand":  {"rotation": [0, 90, -20],  "translation": [1.13, 3.2, 1.13], "scale": [0.72, 0.72, 0.72]},
    "ground": {"rotation": [0, 0, -35], "translation": [0, 3.0, 0], "scale": [0.68, 0.68, 0.68]},
    "gui":    {"rotation": [14, -22, -38], "translation": [0.0, 0.0, 0], "scale": [1.00, 1.00, 1.00]},
    "fixed":  {"rotation": [0, 0, 0], "translation": [0, 0, -0.5], "scale": [1.05, 1.05, 1.05]},
}

BOW_DISPLAY = {
    "thirdperson_righthand": {"rotation": [80, 90, 0],   "translation": [-0.5, -2.0, 0.8], "scale": [0.95, 0.95, 0.95]},
    "thirdperson_lefthand":  {"rotation": [80, -90, 0],  "translation": [-0.5, -2.0, 0.8], "scale": [0.95, 0.95, 0.95]},
    "firstperson_righthand": {"rotation": [0, 90, -15],  "translation": [1.13, 2.8, 1.13], "scale": [0.72, 0.72, 0.72]},
    "firstperson_lefthand":  {"rotation": [0, -90, 15],  "translation": [1.13, 2.8, 1.13], "scale": [0.72, 0.72, 0.72]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2.5, 0], "scale": [0.68, 0.68, 0.68]},
    "gui":    {"rotation": [12, -20, -28], "translation": [0.0, 0.0, 0], "scale": [1.02, 1.02, 1.02]},
    "fixed":  {"rotation": [0, 0, 0], "translation": [0, 0, -0.5], "scale": [1.05, 1.05, 1.05]},
}

SHIELD_IDLE_DISPLAY = {
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

ARMOR_3D_DISPLAY = {
    "thirdperson_righthand": {"rotation": [65, 25, 0], "translation": [0, 2.2, 0.5], "scale": [0.58, 0.58, 0.58]},
    "thirdperson_lefthand":  {"rotation": [65, 25, 0], "translation": [0, 2.2, 0.5], "scale": [0.58, 0.58, 0.58]},
    "firstperson_righthand": {"rotation": [8, -28, 0], "translation": [1.0, 2.4, 0.5], "scale": [0.56, 0.56, 0.56]},
    "firstperson_lefthand":  {"rotation": [8, -28, 0], "translation": [1.0, 2.4, 0.5], "scale": [0.56, 0.56, 0.56]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2.8, 0], "scale": [0.68, 0.68, 0.68]},
    "gui":    {"rotation": [20, -28, 0], "translation": [0, 0.5, 0], "scale": [0.95, 0.95, 0.95]},
    "fixed":  {"rotation": [0, 0, 0], "translation": [0, 0, -0.5], "scale": [0.95, 0.95, 0.95]},
}


def write_json(path: Path, obj: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def emit_3d_item(item_id, art32, depth32, pal_name, uv_map, pal, display,
                 overrides=None, add_shield_grip=False) -> int:
    save_sprite_png(art32, pal, ITEM_TEX / f"{item_id}.png")
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
    write_json(ITEM_MODELS / f"{item_id}.json", model)
    return len(elems)


def emit_3d_armor_item(item_id, grid3d, pal_name, uv_map, pal) -> int:
    sprite32 = project_3d_grid_to_sprite32(grid3d)
    save_sprite_png(sprite32, pal, ITEM_TEX / f"{item_id}.png")
    elems = compile_3d_grid_elements(grid3d, uv_map, step=0.5)
    model = {
        "gui_light": "side",
        "textures": {
            "pal": f"riftworld:item/{pal_name}",
            "particle": f"riftworld:item/{item_id}",
        },
        "elements": elems,
        "display": ARMOR_3D_DISPLAY,
    }
    write_json(ITEM_MODELS / f"{item_id}.json", model)
    return len(elems)


# ==============================================================================
# Software showcase preview
# ==============================================================================

def render_items_showcase(out_png: Path) -> None:
    items = [
        "aurite_pickaxe", "aurite_axe", "aurite_sword", "aurite_shovel", "aurite_hoe", "elisar_bow", "elisar_shield",
        "riftcrystal_pickaxe", "riftcrystal_axe", "riftcrystal_sword", "riftcrystal_shovel", "riftcrystal_hoe",
        "elisar_bow_pulling_2", "elisar_guard_shield",
        "aurite_helmet", "aurite_chestplate", "aurite_leggings", "aurite_boots",
        "riftcrystal_helmet", "riftcrystal_chestplate", "riftcrystal_boots",
    ]
    cols, rows_n = 7, 3
    cell_w, cell_h = 150, 155
    W, H = cols * cell_w, rows_n * cell_h
    canvas = np.zeros((H, W, 4), dtype=np.uint8)
    canvas[:, :] = (24, 28, 36, 255)
    zbuf = np.full((H, W), 1e9, dtype=np.float32)
    pal_cache: Dict[str, np.ndarray] = {}

    for idx, iid in enumerate(items):
        gx, gy = idx % cols, idx // cols
        cx = gx * cell_w + cell_w * 0.5
        cy_s = gy * cell_h + cell_h * 0.54
        mjson = json.loads((ITEM_MODELS / f"{iid}.json").read_text(encoding="utf-8"))
        pal_ref = mjson["textures"]["pal"].split("/")[-1]
        if pal_ref not in pal_cache:
            pal_cache[pal_ref] = np.array(Image.open(ITEM_TEX / f"{pal_ref}.png").convert("RGBA"))
        pal_img = pal_cache[pal_ref]

        is_armor = gy == 2
        yaw = -28.0 if is_armor else -26.0
        pitch = 20.0 if is_armor else 14.0
        roll = 0.0 if is_armor else (-28.0 if "shield" not in iid else 0.0)
        scale = 8.2 if is_armor else 7.6

        cy_r, sy_r = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
        cp_r, sp_r = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
        cr_r, sr_r = math.cos(math.radians(roll)), math.sin(math.radians(roll))

        def tf(pt, cy_r=cy_r, sy_r=sy_r, cp_r=cp_r, sp_r=sp_r, cr_r=cr_r, sr_r=sr_r):
            x, y, z = pt[0] - 8.0, pt[1] - 8.0, pt[2] - 8.0
            x, y = x * cr_r - y * sr_r, x * sr_r + y * cr_r
            x, z = x * cy_r + z * sy_r, -x * sy_r + z * cy_r
            y, z = y * cp_r - z * sp_r, y * sp_r + z * cp_r
            return (x, y, z)

        face_defs = [
            ("south", lambda x0, y0, z0, x1, y1, z1: [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)], 1.02),
            ("north", lambda x0, y0, z0, x1, y1, z1: [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)], 0.78),
            ("east",  lambda x0, y0, z0, x1, y1, z1: [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)], 0.86),
            ("west",  lambda x0, y0, z0, x1, y1, z1: [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)], 0.88),
            ("up",    lambda x0, y0, z0, x1, y1, z1: [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)], 1.10),
            ("down",  lambda x0, y0, z0, x1, y1, z1: [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)], 0.65),
        ]

        for el in mjson["elements"]:
            x0, y0, z0 = el["from"]
            x1, y1, z1 = el["to"]
            for fname, cfn, shade in face_defs:
                if fname not in el["faces"]:
                    continue
                uv = el["faces"][fname]["uv"]
                tx = min(63, int((uv[0] + uv[2]) * 0.5))
                ty = min(63, int((uv[1] + uv[3]) * 0.5))
                base_c = pal_img[ty, tx]
                col = (
                    int(max(0, min(255, round(int(base_c[0]) * shade)))),
                    int(max(0, min(255, round(int(base_c[1]) * shade)))),
                    int(max(0, min(255, round(int(base_c[2]) * shade)))),
                    255,
                )
                wc = [tf(p) for p in cfn(x0, y0, z0, x1, y1, z1)]
                pts2 = [(cx + p[0] * scale, cy_s - p[1] * scale, -p[2]) for p in wc]
                min_px = max(gx * cell_w, int(math.floor(min(p[0] for p in pts2))))
                max_px = min((gx + 1) * cell_w - 1, int(math.ceil(max(p[0] for p in pts2))))
                min_py = max(gy * cell_h, int(math.floor(min(p[1] for p in pts2))))
                max_py = min((gy + 1) * cell_h - 1, int(math.ceil(max(p[1] for p in pts2))))
                for pa, pb, pc in [(pts2[0], pts2[1], pts2[2]), (pts2[0], pts2[2], pts2[3])]:
                    denom = (pb[1] - pc[1]) * (pa[0] - pc[0]) + (pc[0] - pb[0]) * (pa[1] - pc[1])
                    if abs(denom) < 1e-5:
                        continue
                    for py in range(min_py, max_py + 1):
                        for px in range(min_px, max_px + 1):
                            w0 = ((pb[1] - pc[1]) * (px + 0.5 - pc[0]) + (pc[0] - pb[0]) * (py + 0.5 - pc[1])) / denom
                            w1 = ((pc[1] - pa[1]) * (px + 0.5 - pc[0]) + (pa[0] - pc[0]) * (py + 0.5 - pc[1])) / denom
                            w2 = 1.0 - w0 - w1
                            if w0 >= -1e-4 and w1 >= -1e-4 and w2 >= -1e-4:
                                zz = w0 * pa[2] + w1 * pb[2] + w2 * pc[2]
                                if zz < zbuf[py, px]:
                                    zbuf[py, px] = zz
                                    canvas[py, px] = col

    out_png.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(canvas, "RGBA").save(out_png)


# ==============================================================================
# Main
# ==============================================================================

def main() -> None:
    ITEM_TEX.mkdir(parents=True, exist_ok=True)
    aur_uv = build_palette_png_and_uv_map(AUR_PAL, ITEM_TEX / "v048_aurite_pal.png")
    rc_uv = build_palette_png_and_uv_map(RC_PAL, ITEM_TEX / "v048_riftcrystal_pal.png")
    grd_uv = build_palette_png_and_uv_map(GUARD_PAL, ITEM_TEX / "v048_guard_pal.png")

    counts: Dict[str, int] = {}
    specs = [
        ("pickaxe", PICKAXE_ART, PICKAXE_DEPTH),
        ("axe", AXE_ART, AXE_DEPTH),
        ("sword", SWORD_ART, SWORD_DEPTH),
        ("shovel", SHOVEL_ART, SHOVEL_DEPTH),
        ("hoe", HOE_ART, HOE_DEPTH),
    ]
    for kind, art, dep in specs:
        counts[f"aurite_{kind}"] = emit_3d_item(
            f"aurite_{kind}", art, dep, "v048_aurite_pal", aur_uv, AUR_PAL, WEAPON_DISPLAY)
        counts[f"riftcrystal_{kind}"] = emit_3d_item(
            f"riftcrystal_{kind}", art, dep, "v048_riftcrystal_pal", rc_uv, RC_PAL, WEAPON_DISPLAY)

    bow_overrides = [
        {"predicate": {"pulling": 1}, "model": "riftworld:item/elisar_bow_pulling_0"},
        {"predicate": {"pulling": 1, "pull": 0.65}, "model": "riftworld:item/elisar_bow_pulling_1"},
        {"predicate": {"pulling": 1, "pull": 0.9}, "model": "riftworld:item/elisar_bow_pulling_2"},
    ]
    counts["elisar_bow"] = emit_3d_item(
        "elisar_bow", BOW_IDLE_ART, BOW_IDLE_DEPTH, "v048_aurite_pal", aur_uv, AUR_PAL, BOW_DISPLAY,
        overrides=bow_overrides)
    for stg in (0, 1, 2):
        p_art, p_dep = make_bow_pull_art(stg)
        counts[f"elisar_bow_pulling_{stg}"] = emit_3d_item(
            f"elisar_bow_pulling_{stg}", p_art, p_dep, "v048_aurite_pal", aur_uv, AUR_PAL, BOW_DISPLAY)

    counts["elisar_shield"] = emit_3d_item(
        "elisar_shield", SHIELD_ART, SHIELD_DEPTH, "v048_aurite_pal", aur_uv, AUR_PAL, SHIELD_IDLE_DISPLAY,
        overrides=[{"predicate": {"blocking": 1}, "model": "riftworld:item/elisar_shield_blocking"}],
        add_shield_grip=True)
    write_json(ITEM_MODELS / "elisar_shield_blocking.json", {
        "parent": "riftworld:item/elisar_shield",
        "display": SHIELD_BLOCKING_DISPLAY,
    })

    counts["elisar_guard_shield"] = emit_3d_item(
        "elisar_guard_shield", GUARD_SHIELD_ART, SHIELD_DEPTH, "v048_guard_pal", grd_uv, GUARD_PAL,
        SHIELD_IDLE_DISPLAY,
        overrides=[{"predicate": {"blocking": 1}, "model": "riftworld:item/elisar_guard_shield_blocking"}],
        add_shield_grip=True)
    write_json(ITEM_MODELS / "elisar_guard_shield_blocking.json", {
        "parent": "riftworld:item/elisar_guard_shield",
        "display": SHIELD_BLOCKING_DISPLAY,
    })
    counts["elisar_guard_sword"] = emit_3d_item(
        "elisar_guard_sword", GUARD_SWORD_ART, GUARD_SWORD_DEPTH, "v048_guard_pal", grd_uv, GUARD_PAL, WEAPON_DISPLAY)

    armor_3d_specs = [
        ("aurite_helmet",           build_3d_helmet_grid(False),     "v048_aurite_pal",      aur_uv, AUR_PAL),
        ("aurite_chestplate",       build_3d_chestplate_grid(False), "v048_aurite_pal",      aur_uv, AUR_PAL),
        ("aurite_leggings",         build_3d_leggings_grid(False),   "v048_aurite_pal",      aur_uv, AUR_PAL),
        ("aurite_boots",            build_3d_boots_grid(False),      "v048_aurite_pal",      aur_uv, AUR_PAL),
        ("riftcrystal_helmet",      build_3d_helmet_grid(True),      "v048_riftcrystal_pal", rc_uv,  RC_PAL),
        ("riftcrystal_chestplate",  build_3d_chestplate_grid(True),  "v048_riftcrystal_pal", rc_uv,  RC_PAL),
        ("riftcrystal_leggings",    build_3d_leggings_grid(True),    "v048_riftcrystal_pal", rc_uv,  RC_PAL),
        ("riftcrystal_boots",       build_3d_boots_grid(True),       "v048_riftcrystal_pal", rc_uv,  RC_PAL),
    ]
    for aid, agrid, apal_name, auv, apal in armor_3d_specs:
        counts[aid] = emit_3d_armor_item(aid, agrid, apal_name, auv, apal)

    render_items_showcase(Path("/tmp/preview_v049_weapons_tools.png"))
    print("OK v0.49.0 3D weapons, tools, bow, shields & 3D armor items generated:", counts)


if __name__ == "__main__":
    main()
