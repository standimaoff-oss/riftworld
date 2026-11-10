#!/usr/bin/env python3
"""
v0.49.0 — 2x HD (256x256) Box-UV worn armor generator for ElisarArmorModels.java.

Upgrades over v0.48.0 (128x128):
  * Every armor atlas is rebuilt at 256x256 with half-unit texels (0.5 px/texel),
    driven through a "hd" child bone that is uniformly scaled to 0.5F at runtime.
  * The geometry gains the sculpting of the target references:
      - 01_aurite_medium_armor.png: hollow T-visor helm with 3-louver cheek vents,
        golden crest, tilted gold-trimmed pauldrons over OPEN arms, 3D V-gorget and
        medallion, sculpted abdominal lames, octagonal golden knee rondels and
        golden sabaton toe-caps.
      - 02_riftcrystal_strong_armor.png: faceted dark slate-iron plate, swept 3D
        crystal horns, hollow cyan T-visor, 3D triple-chevron cyan/magenta ribcage,
        crystal-spiked pauldrons, full-sleeve gauntlets and a matched 256x256
        emissive glow layer.

The generator writes:
  mod/src/main/java/ru/aiforge/riftworld/client/model/ElisarArmorModels.java
  mod/src/main/resources/assets/riftworld/textures/models/armor/aurite_armor.png
  mod/src/main/resources/assets/riftworld/textures/models/armor/riftcrystal_armor.png
  mod/src/main/resources/assets/riftworld/textures/models/armor/riftcrystal_armor_glow.png

It also renders two software preview images into /tmp for visual inspection.
"""
from __future__ import annotations

import math
from pathlib import Path
from typing import Callable, Dict, List, Optional, Sequence, Tuple

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "mod/src/main/resources/assets/riftworld"
ARMOR_TEX = ASSETS / "textures/models/armor"
JAVA_OUT = ROOT / "mod/src/main/java/ru/aiforge/riftworld/client/model/ElisarArmorModels.java"

RGBA = Tuple[int, int, int, int]

# ==============================================================================
# Palettes
# ==============================================================================

def hex_rgba(h: str, a: int = 255) -> RGBA:
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


AUR = {
    "iron": hex_rgba("#2B2B2E"),     # dark forged iron underlayer
    "iron_l": hex_rgba("#45464B"),   # lit iron
    "bronze_d": hex_rgba("#3A2410"),  # shadowed bronze
    "bronze": hex_rgba("#5E3F1B"),    # weathered bronze cheek
    "bronze_l": hex_rgba("#8A6129"),  # lit bronze
    "gold_d": hex_rgba("#A6741C"),   # deep Aurite gold
    "gold": hex_rgba("#C68924"),     # Aurite gold
    "gold_l": hex_rgba("#ECB946"),   # bright Aurite gold
    "gold_h": hex_rgba("#FCE48E"),   # sunlit specular gold
    "teal_d": hex_rgba("#234946"),   # shaded patina deep teal
    "teal": hex_rgba("#326762"),     # muted oxidised teal gambeson
    "teal_l": hex_rgba("#488882"),   # lit patina teal
    "dark": hex_rgba("#15100C"),     # deep recess / visor void
    "crack": hex_rgba("#1B1611"),    # hairline crack
}

RC = {
    "slate_0": hex_rgba("#141A26"),  # deepest slate
    "slate_1": hex_rgba("#1E2735"),  # dark slate facet
    "slate_2": hex_rgba("#2A3548"),  # mid slate-iron
    "slate_3": hex_rgba("#3A4860"),  # lit slate facet
    "slate_hi": hex_rgba("#55688A"), # bright slate bevel
    "cyan_d": hex_rgba("#0C6E72"),   # deep cyan crystal bevel
    "cyan": hex_rgba("#19BDB8"),     # rift cyan
    "cyan_l": hex_rgba("#30ECE4"),   # glowing rift cyan
    "cyan_h": hex_rgba("#B8FFFC"),   # incandescent white-cyan
    "mag_d": hex_rgba("#6B2394"),    # deep rift magenta
    "mag": hex_rgba("#B047D6"),      # rift magenta vein
    "mag_h": hex_rgba("#DA6EFF"),    # bright rift magenta
    "void": hex_rgba("#080B12"),     # visor void
}


# ==============================================================================
# Small pixel-art helpers
# ==============================================================================

def new_img(w: int, h: int, fill: RGBA = (0, 0, 0, 0)) -> np.ndarray:
    arr = np.zeros((h, w, 4), dtype=np.uint8)
    arr[:, :] = fill
    return arr


def put_px(arr: np.ndarray, x: int, y: int, c: RGBA) -> None:
    if 0 <= x < arr.shape[1] and 0 <= y < arr.shape[0]:
        arr[y, x] = c


def fill_rect(arr: np.ndarray, x: int, y: int, w: int, h: int, c: RGBA) -> None:
    if w <= 0 or h <= 0:
        return
    x0 = max(0, x)
    y0 = max(0, y)
    x1 = min(arr.shape[1], x + w)
    y1 = min(arr.shape[0], y + h)
    if x1 > x0 and y1 > y0:
        arr[y0:y1, x0:x1] = c


def outline_rect(arr: np.ndarray, x: int, y: int, w: int, h: int, c: RGBA) -> None:
    fill_rect(arr, x, y, w, 1, c)
    fill_rect(arr, x, y + h - 1, w, 1, c)
    fill_rect(arr, x, y, 1, h, c)
    fill_rect(arr, x + w - 1, y, 1, h, c)


def metal_panel(w: int, h: int, dark: RGBA, mid: RGBA, lit: RGBA, hi: RGBA,
                seed: int = 0, trim: Optional[Callable[[np.ndarray], None]] = None,
                rivets: bool = False) -> np.ndarray:
    """A brushed-metal panel with a vertical bevel gradient and subtle noise."""
    arr = new_img(w, h, mid)
    rng = np.random.default_rng(seed)
    for y in range(h):
        # top rows darker (recessed), lower-middle brighter (lit), bottom shaded.
        if y < max(1, h // 6):
            t = y / max(1, h // 6)
            col = blend(dark, mid, t)
        elif y > h - max(1, h // 7):
            t = (y - (h - max(1, h // 7))) / max(1, h // 7)
            col = blend(mid, dark, t * 0.85)
        else:
            col = mid
        fill_rect(arr, 0, y, w, 1, col)
    # Horizontal brushed streaks.
    for _ in range(max(2, w * h // 90)):
        x = int(rng.integers(0, max(1, w - 3)))
        y = int(rng.integers(0, max(1, h)))
        length = int(rng.integers(2, max(3, w // 3)))
        c = hi if rng.random() < 0.30 else dark
        a = 0.45 if rng.random() < 0.6 else 0.8
        for k in range(length):
            if 0 <= x + k < w:
                arr[y, x + k] = blend(arr[y, x + k], c, a * 0.35)
    # Left / right bevels.
    fill_rect(arr, 0, 0, 1, h, hi)
    fill_rect(arr, w - 1, 0, 1, h, dark)
    if trim:
        trim(arr)
    if rivets:
        for rx in (2, w - 3):
            for ry in range(2, h - 1, max(3, h // 4)):
                put_px(arr, rx, ry, hi)
                put_px(arr, rx, ry + 1, dark)
    return arr


def blend(a: RGBA, b: RGBA, t: float) -> RGBA:
    t = max(0.0, min(1.0, t))
    return (
        int(round(int(a[0]) + (int(b[0]) - int(a[0])) * t)),
        int(round(int(a[1]) + (int(b[1]) - int(a[1])) * t)),
        int(round(int(a[2]) + (int(b[2]) - int(a[2])) * t)),
        int(round(int(a[3]) + (int(b[3]) - int(a[3])) * t)),
    )


def scale_for_eyes(c: RGBA, factor: float) -> RGBA:
    """Dim a colour for the additive RenderType.eyes glow layer."""
    return (int(round(c[0] * factor)), int(round(c[1] * factor)), int(round(c[2] * factor)), 255)


# ==============================================================================
# Box-UV cube painting (a box of w x h x d uses 2w+2d wide by d+h tall)
# ==============================================================================

def box_faces(w: int, h: int, d: int, painter: Callable[[str, int, int], np.ndarray]) -> Dict[str, np.ndarray]:
    """Returns the six Box-UV face images keyed by their Minecraft face name."""
    fw = w + d
    return {
        "up": painter("up", w, d),
        "down": painter("down", w, d),
        "right": painter("right", d, h),
        "front": painter("front", w, h),
        "left": painter("left", d, h),
        "back": painter("back", w, h),
    }


def box_uv_size(w: int, h: int, d: int) -> Tuple[int, int]:
    return (2 * w + 2 * d, d + h)


# ==============================================================================
# Aurite palette painters
# ==============================================================================

def aur_gold_trim(arr: np.ndarray) -> None:
    h, w = arr.shape[:2]
    fill_rect(arr, 0, 0, w, 1, AUR["gold_l"])
    fill_rect(arr, 0, h - 1, w, 1, AUR["gold_d"])


def paint_aur_head(face: str, w: int, h: int) -> np.ndarray:
    if face in ("up", "down"):
        arr = metal_panel(w, h, AUR["iron"], AUR["iron_l"], AUR["bronze"], AUR["gold_l"], seed=11, trim=aur_gold_trim)
        if face == "up":
            # Golden crest band running over the crown.
            fill_rect(arr, w // 2 - 3, 0, 6, h, AUR["gold"])
            fill_rect(arr, w // 2 - 2, 0, 2, h, AUR["gold_h"])
        return arr
    if face == "right":
        arr = metal_panel(w, h, AUR["iron"], AUR["iron_l"], AUR["bronze"], AUR["gold_l"], seed=12, trim=aur_gold_trim)
        fill_rect(arr, 0, 0, w, 3, AUR["gold"])
        fill_rect(arr, 0, 3, w, 1, AUR["gold_d"])
        # Cheek louvre slots.
        for ly in (h // 3, h // 3 + 4, h // 3 + 8):
            if ly + 2 < h:
                fill_rect(arr, 1, ly, w - 2, 2, AUR["dark"])
        return arr
    if face == "left":
        arr = metal_panel(w, h, AUR["iron"], AUR["iron_l"], AUR["bronze"], AUR["gold_l"], seed=13, trim=aur_gold_trim)
        fill_rect(arr, 0, 0, w, 3, AUR["gold"])
        fill_rect(arr, 0, 3, w, 1, AUR["gold_d"])
        for ly in (h // 3, h // 3 + 4, h // 3 + 8):
            if ly + 2 < h:
                fill_rect(arr, 1, ly, w - 2, 2, AUR["dark"])
        return arr
    # front / back
    arr = metal_panel(w, h, AUR["iron"], AUR["iron_l"], AUR["bronze"], AUR["gold_l"], seed=14, trim=aur_gold_trim)
    if face == "front":
        # Hollow T-visor: a dark horizontal eye slit and a vertical nose stem.
        fill_rect(arr, w // 4, h // 3, w // 2, max(2, h // 10), AUR["dark"])
        fill_rect(arr, w // 2 - 1, h // 3 + max(2, h // 10), 2, h // 3, AUR["dark"])
        # Golden brow bar + central nose ridge.
        fill_rect(arr, 0, h // 3 - 2, w, 2, AUR["gold"])
        fill_rect(arr, w // 2 - 1, h // 3, 2, h // 2, AUR["gold_l"])
        # Golden crest running up the middle of the forehead.
        fill_rect(arr, w // 2 - 2, 0, 4, h // 3 - 2, AUR["gold"])
        fill_rect(arr, w // 2 - 1, 0, 2, h // 3 - 2, AUR["gold_h"])
        # Cheek louvres.
        for ly in (h // 3 + 5, h // 3 + 9):
            if ly + 2 < h:
                fill_rect(arr, 1, ly, w // 4 - 1, 2, AUR["dark"])
                fill_rect(arr, w - w // 4, ly, w // 4 - 1, 2, AUR["dark"])
    else:
        # Back: golden spine ridge + a small teal vent.
        fill_rect(arr, w // 2 - 2, 0, 4, h, AUR["gold_d"])
        fill_rect(arr, w // 2 - 1, 0, 2, h, AUR["gold_l"])
        fill_rect(arr, w // 4, h // 2, w // 2, max(2, h // 12), AUR["teal_d"])
    return arr


def paint_aur_body(face: str, w: int, h: int) -> np.ndarray:
    if face in ("up", "down"):
        arr = metal_panel(w, h, AUR["iron"], AUR["iron_l"], AUR["bronze"], AUR["gold_l"], seed=21, trim=aur_gold_trim)
        return arr
    if face in ("right", "left"):
        arr = metal_panel(w, h, AUR["bronze_d"], AUR["bronze"], AUR["bronze_l"], AUR["gold_l"], seed=22, trim=aur_gold_trim)
        # Side louvres.
        for ly in range(2, h - 2, 4):
            fill_rect(arr, 1, ly, w - 2, 2, AUR["iron"])
            fill_rect(arr, 1, ly + 2, w - 2, 1, AUR["bronze_l"])
        return arr
    if face == "front":
        arr = metal_panel(w, h, AUR["bronze_d"], AUR["bronze"], AUR["bronze_l"], AUR["gold_l"], seed=23, trim=aur_gold_trim)
        # 3D V-gorget: two diagonal golden bands meeting at a raised medallion.
        mid = w // 2
        for step in range(mid - 2):
            yy = 1 + step
            if yy < h:
                put_px(arr, 1 + step, yy, AUR["gold"])
                put_px(arr, 2 + step, yy, AUR["gold_l"])
                put_px(arr, w - 2 - step, yy, AUR["gold"])
                put_px(arr, w - 3 - step, yy, AUR["gold_l"])
        # Central golden medallion.
        fill_rect(arr, mid - 4, h // 3, 8, 8, AUR["gold"])
        fill_rect(arr, mid - 3, h // 3 + 1, 6, 6, AUR["gold_l"])
        fill_rect(arr, mid - 2, h // 3 + 2, 4, 4, AUR["gold_h"])
        # Oxidised teal gambeson showing between the abdominal lames.
        fill_rect(arr, 1, h - 8, w - 2, 6, AUR["teal"])
        fill_rect(arr, 1, h - 8, w - 2, 1, AUR["teal_l"])
        # Sculpted abdominal lames.
        for ly in (h - 14, h - 10, h - 6):
            fill_rect(arr, 2, ly, w - 4, 1, AUR["gold_d"])
            fill_rect(arr, 3, ly + 1, w - 6, 1, AUR["bronze_l"])
        # Neckline.
        fill_rect(arr, mid - 4, 0, 8, 2, AUR["dark"])
        return arr
    # back
    arr = metal_panel(w, h, AUR["bronze_d"], AUR["bronze"], AUR["bronze_l"], AUR["gold_l"], seed=24, trim=aur_gold_trim)
    fill_rect(arr, mid := w // 2 - 2, 0, 4, h, AUR["gold_d"])
    fill_rect(arr, mid + 1, 0, 2, h, AUR["gold_l"])
    for ly in (h // 4, h // 2, 3 * h // 4):
        fill_rect(arr, 1, ly, w // 3, 3, AUR["bronze_l"])
        fill_rect(arr, w - 1 - w // 3, ly, w // 3, 3, AUR["bronze_l"])
    return arr


def paint_aur_pauldron_main(face: str, w: int, h: int) -> np.ndarray:
    if face in ("up", "down"):
        arr = metal_panel(w, h, AUR["bronze_d"], AUR["bronze"], AUR["bronze_l"], AUR["gold_l"], seed=31, trim=aur_gold_trim)
        return arr
    arr = metal_panel(w, h, AUR["bronze_d"], AUR["bronze"], AUR["bronze_l"], AUR["gold_l"], seed=32, trim=aur_gold_trim)
    if face == "front":
        # Layered golden lames.
        for ly in range(1, h - 1, 4):
            fill_rect(arr, 0, ly, w, 2, AUR["gold_d"])
            fill_rect(arr, 0, ly + 2, w, 1, AUR["gold_l"])
    return arr


def paint_aur_pauldron_cap(face: str, w: int, h: int) -> np.ndarray:
    arr = metal_panel(w, h, AUR["gold_d"], AUR["gold"], AUR["gold_l"], AUR["gold_h"], seed=33, trim=aur_gold_trim)
    if face == "front":
        fill_rect(arr, 0, h // 2, w, 1, AUR["gold_h"])
    return arr


def paint_aur_arm(face: str, w: int, h: int) -> np.ndarray:
    """Open arm: only a bracer cuff at the bottom and a shoulder ring at the top."""
    if face in ("up", "down"):
        arr = metal_panel(w, h, AUR["iron"], AUR["iron_l"], AUR["bronze"], AUR["gold_l"], seed=41, trim=aur_gold_trim)
        return arr
    arr = metal_panel(w, h, AUR["bronze_d"], AUR["bronze"], AUR["bronze_l"], AUR["gold_l"], seed=42, trim=aur_gold_trim)
    fill_rect(arr, 0, 0, w, 2, AUR["gold_d"])
    fill_rect(arr, 0, h - 4, w, 4, AUR["gold"])
    fill_rect(arr, 0, h - 4, w, 1, AUR["gold_l"])
    return arr


def paint_aur_glove(face: str, w: int, h: int) -> np.ndarray:
    arr = metal_panel(w, h, AUR["bronze_d"], AUR["bronze"], AUR["bronze_l"], AUR["gold_l"], seed=43, trim=aur_gold_trim)
    if face == "front":
        for ly in (h // 3, 2 * h // 3):
            fill_rect(arr, 0, ly, w, 1, AUR["gold_d"])
    return arr


def paint_aur_leg(face: str, w: int, h: int) -> np.ndarray:
    if face in ("up", "down"):
        arr = metal_panel(w, h, AUR["iron"], AUR["iron_l"], AUR["bronze"], AUR["gold_l"], seed=51, trim=aur_gold_trim)
        return arr
    arr = metal_panel(w, h, AUR["bronze_d"], AUR["bronze"], AUR["bronze_l"], AUR["gold_l"], seed=52, trim=aur_gold_trim)
    if face == "front":
        # Teal greave panel in the middle of the shin.
        fill_rect(arr, 1, h // 4, w - 2, h // 2, AUR["teal"])
        fill_rect(arr, 1, h // 4, w - 2, 1, AUR["teal_l"])
        # Golden knee rondel.
        ky = h // 4 - 3
        fill_rect(arr, w // 2 - 3, ky, 6, 6, AUR["gold"])
        fill_rect(arr, w // 2 - 2, ky + 1, 4, 4, AUR["gold_l"])
        fill_rect(arr, w // 2 - 1, ky + 2, 2, 2, AUR["gold_h"])
    elif face in ("left", "right"):
        fill_rect(arr, 0, h // 4 - 3, w, 6, AUR["gold"])
        fill_rect(arr, 0, h // 4 - 3, w, 1, AUR["gold_l"])
    return arr


def paint_aur_boot(face: str, w: int, h: int) -> np.ndarray:
    if face in ("up", "down"):
        arr = metal_panel(w, h, AUR["iron"], AUR["iron_l"], AUR["bronze"], AUR["gold_l"], seed=61, trim=aur_gold_trim)
        return arr
    arr = metal_panel(w, h, AUR["bronze_d"], AUR["bronze"], AUR["bronze_l"], AUR["gold_l"], seed=62, trim=aur_gold_trim)
    if face == "front":
        # Golden sabaton toe cap.
        fill_rect(arr, 0, 0, w, h // 2, AUR["gold"])
        fill_rect(arr, 0, 0, w, 1, AUR["gold_l"])
        fill_rect(arr, w // 3, h // 3, w // 3, h // 6, AUR["gold_h"])
    elif face in ("left", "right"):
        fill_rect(arr, 0, 0, w, h // 2, AUR["gold"])
    return arr


def paint_aur_inner(face: str, w: int, h: int) -> np.ndarray:
    """The undersuit layer visible between the plates."""
    arr = metal_panel(w, h, AUR["teal_d"], AUR["teal"], AUR["teal_l"], AUR["bronze_l"], seed=71)
    if face == "front":
        fill_rect(arr, 0, 0, w, 2, AUR["dark"])
    return arr


# ==============================================================================
# Riftcrystal palette painters
# ==============================================================================

def rc_trim(arr: np.ndarray) -> None:
    h, w = arr.shape[:2]
    fill_rect(arr, 0, 0, w, 1, RC["slate_hi"])
    fill_rect(arr, 0, h - 1, w, 1, RC["slate_0"])


def paint_rc_head(face: str, w: int, h: int) -> np.ndarray:
    if face in ("up", "down"):
        arr = metal_panel(w, h, RC["slate_0"], RC["slate_1"], RC["slate_2"], RC["slate_3"], seed=111, trim=rc_trim)
        if face == "up":
            fill_rect(arr, w // 2 - 3, 0, 6, h, RC["slate_2"])
            fill_rect(arr, w // 2 - 2, 0, 2, h, RC["cyan_d"])
        return arr
    if face in ("right", "left"):
        arr = metal_panel(w, h, RC["slate_0"], RC["slate_1"], RC["slate_2"], RC["slate_3"], seed=112, trim=rc_trim)
        fill_rect(arr, 0, 0, w, 2, RC["cyan_d"])
        fill_rect(arr, 0, h - 5, w, 5, RC["slate_2"])
        fill_rect(arr, 0, h - 5, w, 1, RC["cyan"])
        return arr
    arr = metal_panel(w, h, RC["slate_0"], RC["slate_1"], RC["slate_2"], RC["slate_3"], seed=113, trim=rc_trim)
    if face == "front":
        # Hollow cyan T-visor.
        fill_rect(arr, w // 4, h // 3, w // 2, max(2, h // 10), RC["void"])
        fill_rect(arr, w // 2 - 1, h // 3 + max(2, h // 10), 2, h // 3, RC["void"])
        fill_rect(arr, 0, h // 3 - 2, w, 2, RC["cyan_d"])
        fill_rect(arr, w // 2 - 1, h // 3, 2, h // 2, RC["cyan"])
        # Crystalline crown ridge.
        fill_rect(arr, w // 2 - 2, 0, 4, h // 3 - 2, RC["slate_3"])
        fill_rect(arr, w // 2 - 1, 0, 2, h // 3 - 2, RC["cyan_l"])
        # Violet cheek sparks.
        for ly in (h // 3 + 5, h // 3 + 9):
            if ly + 2 < h:
                fill_rect(arr, 1, ly, w // 4 - 1, 2, RC["mag_d"])
                fill_rect(arr, w - w // 4, ly, w // 4 - 1, 2, RC["mag_d"])
    else:
        fill_rect(arr, w // 2 - 2, 0, 4, h, RC["slate_3"])
        fill_rect(arr, w // 2 - 1, 0, 2, h, RC["cyan_d"])
    return arr


def paint_rc_body(face: str, w: int, h: int) -> np.ndarray:
    if face in ("up", "down"):
        arr = metal_panel(w, h, RC["slate_0"], RC["slate_1"], RC["slate_2"], RC["slate_3"], seed=121, trim=rc_trim)
        return arr
    if face in ("right", "left"):
        arr = metal_panel(w, h, RC["slate_0"], RC["slate_1"], RC["slate_2"], RC["slate_3"], seed=122, trim=rc_trim)
        for ly in range(2, h - 2, 5):
            fill_rect(arr, 1, ly, w - 2, 2, RC["cyan_d"])
        return arr
    arr = metal_panel(w, h, RC["slate_0"], RC["slate_1"], RC["slate_2"], RC["slate_3"], seed=123, trim=rc_trim)
    mid = w // 2
    if face == "front":
        # Raised sternum spine.
        fill_rect(arr, mid - 1, 1, 2, h - 2, RC["cyan_d"])
        fill_rect(arr, mid - 1, 1, 1, h - 2, RC["cyan"])
        # Diagonal V-chevron ribcage relief.
        for ry in (2, 7, 12):
            if ry + 3 > h:
                continue
            for step in range(mid - 2):
                yy = ry + step // 2
                if yy + 1 >= h:
                    break
                c_top = RC["mag_h"] if step == 0 else (RC["cyan_h"] if step >= mid - 4 else RC["cyan_l"])
                put_px(arr, 1 + step, yy, c_top)
                put_px(arr, 1 + step, yy + 1, RC["cyan_d"])
                put_px(arr, w - 2 - step, yy, c_top)
                put_px(arr, w - 2 - step, yy + 1, RC["cyan_d"])
    else:
        fill_rect(arr, mid - 2, 0, 4, h, RC["slate_3"])
        fill_rect(arr, mid - 1, 0, 2, h, RC["cyan_d"])
    return arr


def paint_rc_pauldron(face: str, w: int, h: int) -> np.ndarray:
    if face in ("up", "down"):
        arr = metal_panel(w, h, RC["slate_0"], RC["slate_1"], RC["slate_2"], RC["slate_3"], seed=131, trim=rc_trim)
        return arr
    arr = metal_panel(w, h, RC["slate_0"], RC["slate_1"], RC["slate_2"], RC["slate_3"], seed=132, trim=rc_trim)
    if face == "front":
        # Faceted plates with a glowing cyan seam.
        for ly in range(1, h - 1, 4):
            fill_rect(arr, 0, ly, w, 1, RC["slate_0"])
            fill_rect(arr, 0, ly + 1, w, 1, RC["cyan_d"])
        fill_rect(arr, w // 2 - 1, 0, 2, h, RC["cyan_d"])
    return arr


def paint_rc_crystal_shard(face: str, w: int, h: int) -> np.ndarray:
    arr = metal_panel(w, h, RC["cyan_d"], RC["cyan"], RC["cyan_l"], RC["cyan_h"], seed=141)
    if face == "front":
        fill_rect(arr, w // 2 - 1, 0, 2, h, RC["cyan_h"])
        fill_rect(arr, 0, h // 2, w, 1, RC["cyan_d"])
    return arr


def paint_rc_vambrace(face: str, w: int, h: int) -> np.ndarray:
    if face in ("up", "down"):
        arr = metal_panel(w, h, RC["slate_0"], RC["slate_1"], RC["slate_2"], RC["slate_3"], seed=151, trim=rc_trim)
        return arr
    arr = metal_panel(w, h, RC["slate_0"], RC["slate_1"], RC["slate_2"], RC["slate_3"], seed=152, trim=rc_trim)
    if face == "front":
        for ly in range(1, h - 1, 4):
            fill_rect(arr, 0, ly, w, 2, RC["cyan_d"])
            fill_rect(arr, 0, ly + 2, w, 1, RC["cyan"])
    return arr


def paint_rc_leg(face: str, w: int, h: int) -> np.ndarray:
    if face in ("up", "down"):
        arr = metal_panel(w, h, RC["slate_0"], RC["slate_1"], RC["slate_2"], RC["slate_3"], seed=161, trim=rc_trim)
        return arr
    arr = metal_panel(w, h, RC["slate_0"], RC["slate_1"], RC["slate_2"], RC["slate_3"], seed=162, trim=rc_trim)
    if face == "front":
        # Glowing cyan shin channel + violet ankle spark.
        fill_rect(arr, w // 2 - 2, 2, 4, h - 6, RC["cyan_d"])
        fill_rect(arr, w // 2 - 1, 2, 2, h - 6, RC["cyan"])
        fill_rect(arr, w // 2 - 1, h - 4, 2, 2, RC["mag_h"])
    elif face in ("left", "right"):
        fill_rect(arr, 0, 1, w, h - 4, RC["cyan_d"])
    return arr


def paint_rc_boot(face: str, w: int, h: int) -> np.ndarray:
    if face in ("up", "down"):
        arr = metal_panel(w, h, RC["slate_0"], RC["slate_1"], RC["slate_2"], RC["slate_3"], seed=171, trim=rc_trim)
        return arr
    arr = metal_panel(w, h, RC["slate_0"], RC["slate_1"], RC["slate_2"], RC["slate_3"], seed=172, trim=rc_trim)
    if face == "front":
        fill_rect(arr, w // 2 - 2, 0, 4, h // 2, RC["cyan_d"])
        fill_rect(arr, w // 2 - 1, 0, 2, h // 2, RC["cyan"])
        fill_rect(arr, 1, h // 3, 2, 2, RC["mag_h"])
    return arr


def paint_rc_inner(face: str, w: int, h: int) -> np.ndarray:
    arr = metal_panel(w, h, RC["slate_0"], RC["slate_1"], RC["slate_2"], RC["slate_3"], seed=181, trim=rc_trim)
    return arr


# ==============================================================================
# Glow (emissive) painters
# ==============================================================================

def glow_rc_head(face: str, w: int, h: int) -> Optional[np.ndarray]:
    if face != "front":
        return None
    g = new_img(w, h)
    fill_rect(g, w // 4, h // 3, w // 2, max(2, h // 10), scale_for_eyes(RC["cyan_h"], 0.46))
    fill_rect(g, w // 2 - 1, h // 3 + max(2, h // 10), 2, h // 3, scale_for_eyes(RC["cyan_l"], 0.40))
    for ly in (h // 3 + 5, h // 3 + 9):
        if ly + 2 < h:
            fill_rect(g, 1, ly, w // 4 - 1, 2, scale_for_eyes(RC["mag_h"], 0.40))
            fill_rect(g, w - w // 4, ly, w // 4 - 1, 2, scale_for_eyes(RC["mag_h"], 0.40))
    fill_rect(g, w // 2 - 1, 0, 2, h // 3 - 2, scale_for_eyes(RC["cyan_l"], 0.34))
    return g


def glow_rc_body(face: str, w: int, h: int) -> Optional[np.ndarray]:
    if face != "front":
        return None
    g = new_img(w, h)
    mid = w // 2
    fill_rect(g, mid - 1, 1, 2, h - 2, scale_for_eyes(RC["cyan_h"], 0.44))
    for ry in (2, 7, 12):
        if ry + 3 > h:
            continue
        for step in range(mid - 2):
            yy = ry + step // 2
            if yy + 1 >= h:
                break
            c_top = RC["mag_h"] if step == 0 else RC["cyan_l"]
            put_px(g, 1 + step, yy, scale_for_eyes(c_top, 0.40))
            put_px(g, 1 + step, yy + 1, scale_for_eyes(RC["cyan"], 0.34))
            put_px(g, w - 2 - step, yy, scale_for_eyes(c_top, 0.40))
            put_px(g, w - 2 - step, yy + 1, scale_for_eyes(RC["cyan"], 0.34))
    return g


def glow_rc_pauldron(face: str, w: int, h: int) -> Optional[np.ndarray]:
    if face != "front":
        return None
    g = new_img(w, h)
    for ly in range(1, h - 1, 4):
        fill_rect(g, 0, ly + 1, w, 1, scale_for_eyes(RC["cyan"], 0.32))
    fill_rect(g, w // 2 - 1, 0, 2, h, scale_for_eyes(RC["cyan_l"], 0.30))
    return g


def glow_rc_crystal(face: str, w: int, h: int) -> Optional[np.ndarray]:
    if face != "front":
        return None
    g = new_img(w, h)
    fill_rect(g, w // 2 - 1, 0, 2, h, scale_for_eyes(RC["cyan_h"], 0.48))
    return g


def glow_rc_vambrace(face: str, w: int, h: int) -> Optional[np.ndarray]:
    if face != "front":
        return None
    g = new_img(w, h)
    for ly in range(1, h - 1, 4):
        fill_rect(g, 0, ly + 2, w, 1, scale_for_eyes(RC["cyan"], 0.30))
    return g


def glow_rc_leg(face: str, w: int, h: int) -> Optional[np.ndarray]:
    if face != "front":
        return None
    g = new_img(w, h)
    fill_rect(g, w // 2 - 1, 2, 2, h - 6, scale_for_eyes(RC["cyan"], 0.32))
    fill_rect(g, w // 2 - 1, h - 4, 2, 2, scale_for_eyes(RC["mag_h"], 0.40))
    return g


def glow_rc_boot(face: str, w: int, h: int) -> Optional[np.ndarray]:
    if face != "front":
        return None
    g = new_img(w, h)
    fill_rect(g, w // 2 - 1, 0, 2, h // 2, scale_for_eyes(RC["cyan"], 0.32))
    fill_rect(g, 1, h // 3, 2, 2, scale_for_eyes(RC["mag_h"], 0.40))
    return g


# ==============================================================================
# Cube / bone model
# ==============================================================================

class Cube:
    def __init__(self, name: str, x: float, y: float, z: float, w: float, h: float, d: float,
                 inflate: float, painter: Callable[[str, int, int], np.ndarray],
                 glow: Optional[Callable[[str, int, int], Optional[np.ndarray]]] = None):
        self.name = name
        self.x = x
        self.y = y
        self.z = z
        self.w = w
        self.h = h
        self.d = d
        self.inflate = inflate
        self.painter = painter
        self.glow = glow


class Bone:
    def __init__(self, name: str, px: float = 0.0, py: float = 0.0, pz: float = 0.0,
                 rx: float = 0.0, ry: float = 0.0, rz: float = 0.0, cubes: Optional[List[Cube]] = None):
        self.name = name
        self.px = px
        self.py = py
        self.pz = pz
        self.rx = rx
        self.ry = ry
        self.rz = rz
        self.cubes: List[Cube] = cubes or []
        self.children: List[Bone] = []


class Model:
    def __init__(self, atlas: int = 256):
        self.atlas = atlas
        self.roots: Dict[str, Bone] = {}
        self.uv_cursor: Dict[str, Tuple[int, int]] = {}

    def root(self, name: str, px: float = 0.0, py: float = 0.0, pz: float = 0.0,
             rx: float = 0.0, ry: float = 0.0, rz: float = 0.0) -> Bone:
        b = Bone(name, px, py, pz, rx, ry, rz)
        self.roots[name] = b
        return b


# ==============================================================================
# Atlas packing (no overlaps!)
# ==============================================================================

class AtlasPacker:
    """Simple shelf packer for Box-UV cube faces; guarantees no overlap."""

    def __init__(self, size: int):
        self.size = size
        self.occupied = np.zeros((size, size), dtype=bool)
        self.x = 0
        self.y = 0
        self.row_h = 0

    def alloc(self, w: int, h: int) -> Tuple[int, int]:
        if w > self.size or h > self.size:
            raise ValueError(f"face {w}x{h} does not fit in {self.size}x{self.size}")
        if self.x + w > self.size:
            self.x = 0
            self.y += self.row_h + 1
            self.row_h = 0
        if self.y + h > self.size:
            raise ValueError(f"atlas overflow while allocating {w}x{h}")
        if self.occupied[self.y:self.y + h, self.x:self.x + w].any():
            raise ValueError(f"overlap while allocating {w}x{h} at ({self.x},{self.y})")
        self.occupied[self.y:self.y + h, self.x:self.x + w] = True
        u = self.x
        self.x += w + 1
        self.row_h = max(self.row_h, h)
        return u, self.y


def paint_model(model: Model, atlas: int) -> Tuple[np.ndarray, np.ndarray]:
    """Renders every cube's six Box-UV faces into a fresh atlas + a matching glow atlas."""
    img = new_img(atlas, atlas)
    glow = new_img(atlas, atlas)
    packer = AtlasPacker(atlas)

    def walk(bone: Bone):
        for cube in bone.cubes:
            iw = int(round(cube.w + 2 * cube.inflate))
            ih = int(round(cube.h + 2 * cube.inflate))
            idp = int(round(cube.d + 2 * cube.inflate))
            faces = box_faces(iw, ih, idp, cube.painter)
            fw, fh = box_uv_size(iw, ih, idp)
            u, v = packer.alloc(fw, fh)
            # Minecraft Box-UV layout inside the allocated (fw x fh) region:
            #   up   (0,0)      (w x d)
            #   down (w,0)      (w x d)
            #   right(0,d)      (d x h)
            #   front(w,d)      (w x h)
            #   left (w+d,d)    (d x h)
            #   back (2d+w,d)   (w x h)
            placements = {
                "up": (u + idp, v),
                "down": (u + idp + iw, v),
                "right": (u, v + idp),
                "front": (u + idp, v + idp),
                "left": (u + idp + iw, v + idp),
                "back": (u + 2 * idp + iw, v + idp),
            }
            for face_name, (px, py) in placements.items():
                face_img = faces[face_name]
                fh2, fw2 = face_img.shape[:2]
                img[py:py + fh2, px:px + fw2] = face_img
                if cube.glow:
                    g = cube.glow(face_name, fw2, fh2)
                    if g is not None:
                        glow[py:py + fh2, px:px + fw2] = g
        for child in bone.children:
            walk(child)

    for root in model.roots.values():
        walk(root)
    return img, glow


# ==============================================================================
# Java emission
# ==============================================================================

def emit_java(aur_outer: Model, aur_inner: Model, rc_outer: Model, rc_inner: Model,
              aur_offsets_outer: Dict[int, Tuple[int, int]], aur_offsets_inner: Dict[int, Tuple[int, int]],
              rc_offsets_outer: Dict[int, Tuple[int, int]], rc_offsets_inner: Dict[int, Tuple[int, int]]) -> str:
    lines: List[str] = []
    a = lines.append
    a("package ru.aiforge.riftworld.client.model;")
    a("")
    a("import java.util.NoSuchElementException;")
    a("import net.minecraft.client.Minecraft;")
    a("import net.minecraft.client.model.HumanoidModel;")
    a("import net.minecraft.client.model.geom.ModelLayerLocation;")
    a("import net.minecraft.client.model.geom.ModelPart;")
    a("import net.minecraft.client.model.geom.PartPose;")
    a("import net.minecraft.client.model.geom.builders.CubeDeformation;")
    a("import net.minecraft.client.model.geom.builders.CubeListBuilder;")
    a("import net.minecraft.client.model.geom.builders.LayerDefinition;")
    a("import net.minecraft.client.model.geom.builders.MeshDefinition;")
    a("import net.minecraft.client.model.geom.builders.PartDefinition;")
    a("import net.minecraft.resources.ResourceLocation;")
    a("import net.minecraft.world.entity.EquipmentSlot;")
    a("import net.minecraft.world.entity.LivingEntity;")
    a("import net.minecraftforge.client.event.EntityRenderersEvent;")
    a("import ru.aiforge.riftworld.RiftWorld;")
    a("import ru.aiforge.riftworld.item.RiftArmorMaterials;")
    a("")
    a("/**")
    a(" * Generated by tools/rift/gen_v049_hd_armors.py — DO NOT HAND-EDIT UVs.")
    a(" * v0.49.0 2x HD (256x256 Box-UV, 0.5-unit/texel \"hd\" child bone) 3D armor geometry:")
    a(" *   - Aurite Medium Armor (01_aurite_medium_armor.png): hollow T-visor helm with 3-louver cheek")
    a(" *     vents, golden crest, tilted gold-trimmed pauldrons over open arms, 3D V-gorget and")
    a(" *     medallion, sculpted abdominal lames, octagonal golden knee rondels and golden sabatons.")
    a(" *   - Riftcrystal Strong Armor (02_riftcrystal_strong_armor.png): faceted dark slate-iron plate,")
    a(" *     swept 3D crystal horns, hollow cyan T-visor, 3D triple-chevron cyan/magenta ribcage,")
    a(" *     crystal-spiked pauldrons, full-sleeve gauntlets, and a matched 256x256 emissive glow layer.")
    a(" */")
    a("public final class ElisarArmorModels {")
    a("    public static final ModelLayerLocation AURITE_INNER = new ModelLayerLocation(")
    a("            new ResourceLocation(RiftWorld.MODID, \"aurite_armor\"), \"inner\");")
    a("    public static final ModelLayerLocation AURITE_OUTER = new ModelLayerLocation(")
    a("            new ResourceLocation(RiftWorld.MODID, \"aurite_armor\"), \"outer\");")
    a("    public static final ModelLayerLocation RIFTCRYSTAL_INNER = new ModelLayerLocation(")
    a("            new ResourceLocation(RiftWorld.MODID, \"riftcrystal_armor\"), \"inner\");")
    a("    public static final ModelLayerLocation RIFTCRYSTAL_OUTER = new ModelLayerLocation(")
    a("            new ResourceLocation(RiftWorld.MODID, \"riftcrystal_armor\"), \"outer\");")
    a("")
    a("    private static SlotArmorModel auriteHead, auriteChest, auriteLegs, auriteFeet;")
    a("    private static SlotArmorModel rcHead, rcChest, rcLegs, rcFeet;")
    a("")
    a("    private ElisarArmorModels() {")
    a("    }")
    a("")
    a("    public static void clearCache() {")
    a("        auriteHead = auriteChest = auriteLegs = auriteFeet = null;")
    a("        rcHead = rcChest = rcLegs = rcFeet = null;")
    a("    }")
    a("")
    a("    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {")
    a("        e.registerLayerDefinition(AURITE_INNER, ElisarArmorModels::createAuriteInnerLayer);")
    a("        e.registerLayerDefinition(AURITE_OUTER, ElisarArmorModels::createAuriteOuterLayer);")
    a("        e.registerLayerDefinition(RIFTCRYSTAL_INNER, ElisarArmorModels::createRiftcrystalInnerLayer);")
    a("        e.registerLayerDefinition(RIFTCRYSTAL_OUTER, ElisarArmorModels::createRiftcrystalOuterLayer);")
    a("    }")
    a("")

    counter = [0]

    def emit_layer(method: str, m: Model, offsets: Dict[int, Tuple[int, int]]):
        a(f"    public static LayerDefinition {method}() {{")
        a("        MeshDefinition mesh = new MeshDefinition();")
        a("        PartDefinition root = mesh.getRoot();")
        for root_bone in m.roots.values():
            emit_bone(root_bone, "root", offsets, counter, a)
        a(f"        return LayerDefinition.create(mesh, {m.atlas}, {m.atlas});")
        a("    }")
        a("")

    def emit_bone(bone: Bone, parent_var: str, offsets, counter, a):
        counter[0] += 1
        var = f"p{counter[0]}"
        if bone.cubes:
            a(f"        PartDefinition {var} = {parent_var}.addOrReplaceChild(\"{bone.name}\", CubeListBuilder.create()")
            for cube in bone.cubes:
                iw = int(round(cube.w + 2 * cube.inflate))
                ih = int(round(cube.h + 2 * cube.inflate))
                idp = int(round(cube.d + 2 * cube.inflate))
                u, v = offsets[id(cube)]
                deform = f", new CubeDeformation({cube.inflate:.2F})"
                a(f"                .texOffs({u}, {v}).addBox({cube.x:.2F}F, {cube.y:.2F}F, {cube.z:.2F}F, "
                  f"{iw}.0F, {ih}.0F, {idp}.0F{deform})")
            a(f"                , PartPose.offsetAndRotation({bone.px:.2F}F, {bone.py:.2F}F, {bone.pz:.2F}F, "
              f"{bone.rx:.4F}F, {bone.ry:.4F}F, {bone.rz:.4F}F));")
        else:
            a(f"        PartDefinition {var} = {parent_var}.addOrReplaceChild(\"{bone.name}\", CubeListBuilder.create(), "
              f"PartPose.offsetAndRotation({bone.px:.2F}F, {bone.py:.2F}F, {bone.pz:.2F}F, "
              f"{bone.rx:.4F}F, {bone.ry:.4F}F, {bone.rz:.4F}F));")
        for child in bone.children:
            emit_bone(child, var, offsets, counter, a)

    emit_layer("createAuriteInnerLayer", aur_inner, aur_offsets_inner)
    emit_layer("createAuriteOuterLayer", aur_outer, aur_offsets_outer)
    emit_layer("createRiftcrystalInnerLayer", rc_inner, rc_offsets_inner)
    emit_layer("createRiftcrystalOuterLayer", rc_outer, rc_offsets_outer)

    a("    private static void ensureBaked() {")
    a("        if (auriteHead != null && rcHead != null) return;")
    a("        var models = Minecraft.getInstance().getEntityModels();")
    a("        auriteHead = new SlotArmorModel(models.bakeLayer(AURITE_OUTER), EquipmentSlot.HEAD);")
    a("        auriteChest = new SlotArmorModel(models.bakeLayer(AURITE_OUTER), EquipmentSlot.CHEST);")
    a("        auriteLegs = new SlotArmorModel(models.bakeLayer(AURITE_INNER), EquipmentSlot.LEGS);")
    a("        auriteFeet = new SlotArmorModel(models.bakeLayer(AURITE_OUTER), EquipmentSlot.FEET);")
    a("")
    a("        rcHead = new SlotArmorModel(models.bakeLayer(RIFTCRYSTAL_OUTER), EquipmentSlot.HEAD);")
    a("        rcChest = new SlotArmorModel(models.bakeLayer(RIFTCRYSTAL_OUTER), EquipmentSlot.CHEST);")
    a("        rcLegs = new SlotArmorModel(models.bakeLayer(RIFTCRYSTAL_INNER), EquipmentSlot.LEGS);")
    a("        rcFeet = new SlotArmorModel(models.bakeLayer(RIFTCRYSTAL_OUTER), EquipmentSlot.FEET);")
    a("    }")
    a("")
    a("    @SuppressWarnings(\"unchecked\")")
    a("    public static HumanoidModel<LivingEntity> getForSlot(")
    a("            RiftArmorMaterials mat,")
    a("            EquipmentSlot slot,")
    a("            HumanoidModel<?> original) {")
    a("        ensureBaked();")
    a("        SlotArmorModel target;")
    a("        if (mat == RiftArmorMaterials.AURITE) {")
    a("            target = switch (slot) {")
    a("                case HEAD -> auriteHead;")
    a("                case CHEST -> auriteChest;")
    a("                case LEGS -> auriteLegs;")
    a("                default -> auriteFeet;")
    a("            };")
    a("        } else {")
    a("            target = switch (slot) {")
    a("                case HEAD -> rcHead;")
    a("                case CHEST -> rcChest;")
    a("                case LEGS -> rcLegs;")
    a("                default -> rcFeet;")
    a("            };")
    a("        }")
    a("        ((HumanoidModel<LivingEntity>) original).copyPropertiesTo(target);")
    a("        target.applySlotVisibility();")
    a("        return target;")
    a("    }")
    a("")
    a("    public static final class SlotArmorModel extends HumanoidModel<LivingEntity> {")
    a("        private final EquipmentSlot slot;")
    a("")
    a("        public SlotArmorModel(ModelPart root, EquipmentSlot slot) {")
    a("            super(root);")
    a("            this.slot = slot;")
    a("            enforceHdScales();")
    a("        }")
    a("")
    a("        private static void setHalfScaleIfPresent(ModelPart bone) {")
    a("            try {")
    a("                ModelPart hd = bone.getChild(\"hd\");")
    a("                hd.xScale = 0.5F;")
    a("                hd.yScale = 0.5F;")
    a("                hd.zScale = 0.5F;")
    a("            } catch (NoSuchElementException ignored) {")
    a("            }")
    a("        }")
    a("")
    a("        private void enforceHdScales() {")
    a("            setHalfScaleIfPresent(this.head);")
    a("            setHalfScaleIfPresent(this.body);")
    a("            setHalfScaleIfPresent(this.rightArm);")
    a("            setHalfScaleIfPresent(this.leftArm);")
    a("            setHalfScaleIfPresent(this.rightLeg);")
    a("            setHalfScaleIfPresent(this.leftLeg);")
    a("        }")
    a("")
    a("        public void applySlotVisibility() {")
    a("            enforceHdScales();")
    a("            this.setAllVisible(false);")
    a("            switch (this.slot) {")
    a("                case HEAD -> {")
    a("                    this.head.visible = true;")
    a("                    this.hat.visible = false;")
    a("                }")
    a("                case CHEST -> {")
    a("                    this.body.visible = true;")
    a("                    this.rightArm.visible = true;")
    a("                    this.leftArm.visible = true;")
    a("                }")
    a("                case LEGS -> {")
    a("                    this.body.visible = true;")
    a("                    this.rightLeg.visible = true;")
    a("                    this.leftLeg.visible = true;")
    a("                }")
    a("                case FEET -> {")
    a("                    this.rightLeg.visible = true;")
    a("                    this.leftLeg.visible = true;")
    a("                }")
    a("                default -> {}")
    a("            }")
    a("        }")
    a("    }")
    a("}")
    return "\n".join(lines) + "\n"


# ==============================================================================
# Model construction
# ==============================================================================

def build_aurite_outer() -> Model:
    """
    Geometry is authored in "HD units": every box is TWICE the vanilla Minecraft
    HumanoidModel size, and the "hd" child bone is scaled to 0.5F at runtime, so
    a 16x16x16 HD box fills exactly the 8x8x8 vanilla head.

    Vanilla reference box (humanoid):
        head   (-4,-8,-4)  8 x 8 x 8   (pivot at the neck)
        body   (-4, 0,-2)  8 x12 x 4   (pivot at the hips)
        arm    (-2,-2,-2)  4 x12 x 4   (pivot at the shoulder)
        leg    (-2, 0,-2)  4 x12 x 4   (pivot at the hips)
    """
    m = Model(256)

    # ---- head (HD: 16 x 16 x 16 centred on the neck pivot) ----
    head = m.root("head")
    head.cubes.append(Cube("aur_head", -8.0, -8.0, -8.0, 16, 16, 16, 0.26, paint_aur_head))
    head.cubes.append(Cube("aur_brow", -9.0, -8.0, -9.0, 18, 7, 18, 0.18, paint_aur_head))
    head.cubes.append(Cube("aur_jaw", -9.0, -4.0, -1.0, 18, 10, 10, 0.16, paint_aur_head))
    head.cubes.append(Cube("aur_crest", -4.0, -9.0, -9.0, 8, 8, 8, 0.16, paint_aur_head))
    head.cubes.append(Cube("aur_crest_r", 1.0, -4.0, -9.0, 7, 8, 8, 0.16, paint_aur_head))
    head.cubes.append(Cube("aur_horn_r", -5.0, -7.0, -3.0, 2, 4, 9, 0.05, paint_aur_head))
    head.cubes.append(Cube("aur_horn_l", 3.0, -7.0, -3.0, 2, 4, 9, 0.05, paint_aur_head))

    # ---- body (HD: 16 x 24 x 8, pivot at the hips) ----
    body = m.root("body")
    body.cubes.append(Cube("aur_cuirass", -8.0, 0.0, -4.0, 16, 24, 8, 0.20, paint_aur_body))
    body.cubes.append(Cube("aur_plastron", -7.0, 2.0, -6.0, 14, 12, 2, 0.10, paint_aur_body))
    body.cubes.append(Cube("aur_collar", -8.0, 22.0, -4.0, 16, 4, 8, 0.38, paint_aur_body))

    # ---- arms (HD: 8 x 24 x 8, pivot at the shoulder) ----
    right_arm = m.root("right_arm", -5.0, 2.0, 0.0)
    hd_r = Bone("hd")
    right_arm.children.append(hd_r)
    hd_r.cubes.append(Cube("aur_arm_r", -4.0, -24.0, -4.0, 8, 24, 8, 0.45, paint_aur_arm))
    pauldron_r = Bone("pauldron_r", -3.0, -2.0, 0.0, 0.0, 0.0, -0.14)
    hd_r.children.append(pauldron_r)
    pauldron_r.cubes.append(Cube("aur_pauldron_r", -6.0, -2.0, -6.0, 12, 9, 12, 0.22, paint_aur_pauldron_main))
    pauldron_r.cubes.append(Cube("aur_pauldron_cap_r", -5.0, -4.0, -5.0, 10, 3, 10, 0.14, paint_aur_pauldron_cap))

    left_arm = m.root("left_arm", 5.0, 2.0, 0.0)
    hd_l = Bone("hd")
    left_arm.children.append(hd_l)
    hd_l.cubes.append(Cube("aur_arm_l", -4.0, -24.0, -4.0, 8, 24, 8, 0.45, paint_aur_arm))
    pauldron_l = Bone("pauldron_l", 3.0, -2.0, 0.0, 0.0, 0.0, 0.14)
    hd_l.children.append(pauldron_l)
    pauldron_l.cubes.append(Cube("aur_pauldron_l", -6.0, -2.0, -6.0, 12, 9, 12, 0.22, paint_aur_pauldron_main))
    pauldron_l.cubes.append(Cube("aur_pauldron_cap_l", -5.0, -4.0, -5.0, 10, 3, 10, 0.14, paint_aur_pauldron_cap))

    # ---- legs (HD: 8 x 24 x 8, pivot at the hips) ----
    right_leg = m.root("right_leg", -1.9, 12.0, 0.0)
    hd_rl = Bone("hd")
    right_leg.children.append(hd_rl)
    hd_rl.cubes.append(Cube("aur_leg_r", -4.0, -24.0, -4.0, 8, 24, 8, 0.55, paint_aur_leg))
    hd_rl.cubes.append(Cube("aur_tasset_r", -7.0, -22.0, -5.0, 3, 11, 10, 0.14, paint_aur_leg))
    hd_rl.cubes.append(Cube("aur_rondel_r", -3.0, -13.0, -6.0, 6, 6, 3, 0.18, paint_aur_leg))
    hd_rl.cubes.append(Cube("aur_boot_r", -4.0, -12.0, -4.0, 8, 12, 8, 0.88, paint_aur_boot))

    left_leg = m.root("left_leg", 1.9, 12.0, 0.0)
    hd_ll = Bone("hd")
    left_leg.children.append(hd_ll)
    hd_ll.cubes.append(Cube("aur_leg_l", -4.0, -24.0, -4.0, 8, 24, 8, 0.55, paint_aur_leg))
    hd_ll.cubes.append(Cube("aur_tasset_l", 4.0, -22.0, -5.0, 3, 11, 10, 0.14, paint_aur_leg))
    hd_ll.cubes.append(Cube("aur_rondel_l", -3.0, -13.0, -6.0, 6, 6, 3, 0.18, paint_aur_leg))
    hd_ll.cubes.append(Cube("aur_boot_l", -4.0, -12.0, -4.0, 8, 12, 8, 0.88, paint_aur_boot))

    m.root("hat")
    return m


def build_aurite_inner() -> Model:
    m = Model(256)
    m.root("head")
    m.root("hat")
    body = m.root("body")
    hd = Bone("hd")
    body.children.append(hd)
    hd.cubes.append(Cube("aur_undersuit", -8.0, 14.0, -4.0, 16, 8, 8, 0.50, paint_aur_inner))
    m.root("right_arm", -5.0, 2.0, 0.0)
    m.root("left_arm", 5.0, 2.0, 0.0)
    for leg_name, sign in (("right_leg", -1), ("left_leg", 1)):
        leg = m.root(leg_name, 1.9 * sign, 12.0, 0.0)
        hd2 = Bone("hd")
        leg.children.append(hd2)
        hd2.cubes.append(Cube(f"aur_leg_inner_{leg_name}", -4.0, -24.0, -4.0, 8, 24, 8, 0.55, paint_aur_inner))
    return m


def build_rc_outer() -> Model:
    m = Model(256)
    head = m.root("head")
    head.cubes.append(Cube("rc_head", -8.0, -8.0, -8.0, 16, 16, 16, 0.26, paint_rc_head, glow_rc_head))
    head.cubes.append(Cube("rc_brow", -9.0, -8.0, -9.0, 18, 7, 18, 0.18, paint_rc_head, glow_rc_head))
    head.cubes.append(Cube("rc_jaw", -9.0, -4.0, -1.0, 18, 10, 10, 0.16, paint_rc_head, glow_rc_head))
    head.cubes.append(Cube("rc_crest", -4.0, -9.0, -9.0, 8, 8, 8, 0.16, paint_rc_head, glow_rc_head))
    head.cubes.append(Cube("rc_crest_r", 1.0, -4.0, -9.0, 7, 8, 8, 0.16, paint_rc_head, glow_rc_head))

    horn_r = Bone("rc_horn_r", -8.5, -14.5, -2.5, -0.42, 0.0, -0.28)
    head.children.append(horn_r)
    horn_r.cubes.append(Cube("rc_horn_r_spike", -1.5, -4.5, -1.5, 3, 9, 9, 0.06, paint_rc_crystal_shard, glow_rc_crystal))
    horn_l = Bone("rc_horn_l", 8.5, -14.5, -2.5, -0.42, 0.0, 0.28)
    head.children.append(horn_l)
    horn_l.cubes.append(Cube("rc_horn_l_spike", -1.5, -4.5, -1.5, 3, 9, 9, 0.06, paint_rc_crystal_shard, glow_rc_crystal))

    body = m.root("body")
    body.cubes.append(Cube("rc_cuirass", -8.0, 0.0, -4.0, 16, 24, 8, 0.20, paint_rc_body, glow_rc_body))
    body.cubes.append(Cube("rc_sternum", -7.0, 2.0, -6.0, 14, 12, 2, 0.10, paint_rc_body, glow_rc_body))
    body.cubes.append(Cube("rc_collar", -8.0, 22.0, -4.0, 16, 4, 8, 0.38, paint_rc_body, glow_rc_body))

    for arm_name, sign in (("right_arm", -1), ("left_arm", 1)):
        arm = m.root(arm_name, -5.0 * sign, 2.0, 0.0)
        hd = Bone("hd")
        arm.children.append(hd)
        hd.cubes.append(Cube(f"rc_arm_{arm_name}", -4.0, -24.0, -4.0, 8, 24, 8, 0.45, paint_rc_vambrace, glow_rc_vambrace))
        pauldron = Bone(f"rc_pauldron_{arm_name}", 3.0 * sign, -2.0, 0.0, 0.0, 0.0, 0.16 * sign)
        hd.children.append(pauldron)
        px0 = -6.0 if sign < 0 else -6.0
        pauldron.cubes.append(Cube(f"rc_pauldron_{arm_name}", px0, -2.0, -6.0, 12, 9, 12, 0.22, paint_rc_pauldron, glow_rc_pauldron))
        shard = Bone(f"rc_shard_{arm_name}", -5.0 if sign < 0 else 5.0, -1.0, -1.0, 0.0, 0.0, 0.36 * sign)
        pauldron.children.append(shard)
        shard.cubes.append(Cube(f"rc_shard_{arm_name}", -1.5, -7.0, -2.0, 3, 9, 4, 0.08, paint_rc_crystal_shard, glow_rc_crystal))
        hd.cubes.append(Cube(f"rc_vambrace_{arm_name}", -6.0 if sign < 0 else -2.0, -24.0, -4.0, 8, 24, 8, 0.62, paint_rc_vambrace, glow_rc_vambrace))
        fin_ox = -8.0 if sign < 0 else 4.0
        hd.cubes.append(Cube(f"rc_fin_{arm_name}", fin_ox, -20.0, -2.0, 2, 10, 3, 0.08, paint_rc_crystal_shard, glow_rc_crystal))

    for leg_name, sign in (("right_leg", -1), ("left_leg", 1)):
        leg = m.root(leg_name, 1.9 * sign, 12.0, 0.0)
        hd = Bone("hd")
        leg.children.append(hd)
        hd.cubes.append(Cube(f"rc_leg_{leg_name}", -4.0, -24.0, -4.0, 8, 24, 8, 0.55, paint_rc_leg, glow_rc_leg))
        tasset_x = -7.0 if sign < 0 else 4.0
        hd.cubes.append(Cube(f"rc_tasset_{leg_name}", tasset_x, -22.0, -5.0, 3, 11, 10, 0.14, paint_rc_leg, glow_rc_leg))
        hd.cubes.append(Cube(f"rc_knee_{leg_name}", -3.0, -13.0, -6.0, 6, 6, 3, 0.18, paint_rc_leg, glow_rc_leg))
        hd.cubes.append(Cube(f"rc_boot_{leg_name}", -4.0, -12.0, -4.0, 8, 12, 8, 0.88, paint_rc_boot, glow_rc_boot))

    m.root("hat")
    return m


def build_rc_inner() -> Model:
    m = Model(256)
    m.root("head")
    m.root("hat")
    body = m.root("body")
    hd = Bone("hd")
    body.children.append(hd)
    hd.cubes.append(Cube("rc_undersuit", -8.0, 14.0, -4.0, 16, 8, 8, 0.50, paint_rc_inner))
    m.root("right_arm", -5.0, 2.0, 0.0)
    m.root("left_arm", 5.0, 2.0, 0.0)
    for leg_name, sign in (("right_leg", -1), ("left_leg", 1)):
        leg = m.root(leg_name, 1.9 * sign, 12.0, 0.0)
        hd2 = Bone("hd")
        leg.children.append(hd2)
        hd2.cubes.append(Cube(f"rc_leg_inner_{leg_name}", -4.0, -24.0, -4.0, 8, 24, 8, 0.55, paint_rc_inner))
    return m


# ==============================================================================
# UV offset assignment (patch the .texOffs(0, 0) placeholders)
# ==============================================================================

def assign_offsets(model: Model, packer: AtlasPacker) -> Dict[int, Tuple[int, int]]:
    """Runs the same allocation order as paint_model and returns cube->(u,v)."""
    offsets: Dict[int, Tuple[int, int]] = {}

    def walk(bone: Bone):
        for cube in bone.cubes:
            iw = int(round(cube.w + 2 * cube.inflate))
            ih = int(round(cube.h + 2 * cube.inflate))
            idp = int(round(cube.d + 2 * cube.inflate))
            fw, fh = box_uv_size(iw, ih, idp)
            u, v = packer.alloc(fw, fh)
            offsets[id(cube)] = (u, v)
        for child in bone.children:
            walk(child)

    for root in model.roots.values():
        walk(root)
    return offsets


# ==============================================================================
# Software preview renderer
# ==============================================================================

def rot_xyz(p, rx, ry, rz):
    x, y, z = p
    if rx:
        c, s = math.cos(rx), math.sin(rx)
        y, z = y * c - z * s, y * s + z * c
    if ry:
        c, s = math.cos(ry), math.sin(ry)
        x, z = x * c + z * s, -x * s + z * c
    if rz:
        c, s = math.cos(rz), math.sin(rz)
        x, y = x * c - y * s, x * s + y * c
    return (x, y, z)


def render_preview(model: Model, atlas_img: np.ndarray, out_png: Path) -> None:
    """Renders the armour set (front + 3/4) with the runtime 0.5 'hd' bone scale applied."""
    W, H = 760, 500
    canvas = np.zeros((H, W, 4), dtype=np.uint8)
    canvas[:, :] = (22, 26, 33, 255)
    zbuf = np.full((H, W), 1e9, dtype=np.float32)

    def slot_of(bone_name: str) -> str:
        if bone_name in ("head", "hat"):
            return "head"
        if bone_name in ("body",) or "arm" in bone_name:
            return "chest"
        return "legs"

    views = [(-22.0, 10.0, 0.0, 210), (30.0, 10.0, 0.0, 480)]

    for vi, (yaw, pitch, roll, ox) in enumerate(views):
        cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
        cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
        cr, sr = math.cos(math.radians(roll)), math.sin(math.radians(roll))

        def world(p):
            x, y, z = p
            x, y = x * cr - y * sr, x * sr + y * cr
            x, z = x * cy + z * sy, -x * sy + z * cy
            y, z = y * cp - z * sp, y * sp + z * cp
            return (x, y, z)

        for bone_name, root in model.roots.items():
            if bone_name == "hat":
                continue
            if vi == 0 and slot_of(bone_name) == "legs":
                continue
            if vi == 1 and slot_of(bone_name) == "head":
                continue

            def walk(bone, parent_pos, parent_rot, hd):
                px, py, pz = parent_pos
                rx, ry, rz = parent_rot
                if bone.name == "hd":
                    hd = True
                rx += bone.rx
                ry += bone.ry
                rz += bone.rz
                for cube in bone.cubes:
                    draw_cube(canvas, zbuf, cube, (px, py, pz), (rx, ry, rz), world, ox, hd)
                for child in bone.children:
                    lx, ly, lz = rot_xyz((child.px, child.py, child.pz), rx, ry, rz)
                    walk(child, (px + lx, py + ly, pz + lz), (rx, ry, rz), hd)

            walk(root, (root.px, root.py, root.pz), (root.rx, root.ry, root.rz), False)

    Image.fromarray(canvas, "RGBA").save(out_png)


def draw_cube(canvas, zbuf, cube: Cube, parent_pos, parent_rot, world, ox, hd):
    """Projects one Box-UV cube into the preview canvas (HD cubes are scaled by 0.5)."""
    iw = int(round(cube.w + 2 * cube.inflate))
    ih = int(round(cube.h + 2 * cube.inflate))
    idp = int(round(cube.d + 2 * cube.inflate))
    faces = box_faces(iw, ih, idp, cube.painter)
    avg = {}
    for k, v in faces.items():
        arr = np.array(v, dtype=np.float32)
        op = arr[:, :, 3] > 0
        avg[k] = arr[op, :3].mean(axis=0) if op.any() else np.array([128.0, 128.0, 128.0], dtype=np.float32)
    cx, cy, cz = parent_pos
    rx, ry, rz = parent_rot
    k = 0.5 if hd else 1.0

    corners_local = [
        (cube.x, cube.y, cube.z),
        (cube.x + iw, cube.y, cube.z),
        (cube.x + iw, cube.y + ih, cube.z),
        (cube.x, cube.y + ih, cube.z),
        (cube.x, cube.y, cube.z + idp),
        (cube.x + iw, cube.y, cube.z + idp),
        (cube.x + iw, cube.y + ih, cube.z + idp),
        (cube.x, cube.y + ih, cube.z + idp),
    ]
    pts = []
    for (lx, ly, lz) in corners_local:
        lx, ly, lz = lx * k, ly * k, lz * k
        rx2, ry2, rz2 = rot_xyz((lx, ly, lz), rx, ry, rz)
        pts.append(world((rx2 + cx, ry2 + cy, rz2 + cz)))
    scale = 9.0
    oy = 250
    proj = [(ox + p[0] * scale, oy - p[1] * scale, -p[2]) for p in pts]

    quads = [
        (0, 1, 2, 3, "north"),
        (5, 4, 7, 6, "south"),
        (1, 5, 6, 2, "east"),
        (4, 0, 3, 7, "west"),
        (4, 5, 1, 0, "up"),
        (3, 2, 6, 7, "down"),
    ]
    for a, b, c, d, fname in quads:
        col = avg.get(fname, (120, 120, 120, 255))
        shade = 1.0
        if fname in ("north", "back"):
            shade = 0.72
        elif fname in ("east", "left"):
            shade = 0.85
        elif fname == "down":
            shade = 0.60
        elif fname == "up":
            shade = 1.12
        rgba = tuple(int(max(0, min(255, col[i] * shade))) for i in range(3)) + (255,)
        fill_tri(canvas, zbuf, proj[a], proj[b], proj[c], rgba)
        fill_tri(canvas, zbuf, proj[a], proj[c], proj[d], rgba)


def fill_tri(canvas, zbuf, p1, p2, p3, color):
    H, W = canvas.shape[:2]
    min_x = max(0, int(math.floor(min(p1[0], p2[0], p3[0]))))
    max_x = min(W - 1, int(math.ceil(max(p1[0], p2[0], p3[0]))))
    min_y = max(0, int(math.floor(min(p1[1], p2[1], p3[1]))))
    max_y = min(H - 1, int(math.ceil(max(p1[1], p2[1], p3[1]))))
    if min_x > max_x or min_y > max_y:
        return
    denom = (p2[1] - p3[1]) * (p1[0] - p3[0]) + (p3[0] - p2[0]) * (p1[1] - p3[1])
    if abs(denom) < 1e-6:
        return
    for py in range(min_y, max_y + 1):
        for px in range(min_x, max_x + 1):
            w0 = ((p2[1] - p3[1]) * (px + 0.5 - p3[0]) + (p3[0] - p2[0]) * (py + 0.5 - p3[1])) / denom
            w1 = ((p3[1] - p1[1]) * (px + 0.5 - p3[0]) + (p1[0] - p3[0]) * (py + 0.5 - p3[1])) / denom
            w2 = 1.0 - w0 - w1
            if w0 >= -1e-4 and w1 >= -1e-4 and w2 >= -1e-4:
                zz = w0 * p1[2] + w1 * p2[2] + w2 * p3[2]
                if zz < zbuf[py, px]:
                    zbuf[py, px] = zz
                    canvas[py, px] = color


# ==============================================================================
# Main
# ==============================================================================

def main() -> None:
    ARMOR_TEX.mkdir(parents=True, exist_ok=True)

    aur_outer = build_aurite_outer()
    aur_inner = build_aurite_inner()
    rc_outer = build_rc_outer()
    rc_inner = build_rc_inner()

    # One shared 256x256 atlas per material: the outer and inner layer pack into it.
    aur_packer = AtlasPacker(256)
    rc_packer = AtlasPacker(256)
    aur_img = np.zeros((256, 256, 4), dtype=np.uint8)
    rc_img = np.zeros((256, 256, 4), dtype=np.uint8)
    rc_glow = np.zeros((256, 256, 4), dtype=np.uint8)

    def paint_into(model: Model, packer: AtlasPacker, target: np.ndarray,
                   glow_target: Optional[np.ndarray] = None) -> Dict[int, Tuple[int, int]]:
        offsets: Dict[int, Tuple[int, int]] = {}

        def walk(bone: Bone):
            for cube in bone.cubes:
                iw = int(round(cube.w + 2 * cube.inflate))
                ih = int(round(cube.h + 2 * cube.inflate))
                idp = int(round(cube.d + 2 * cube.inflate))
                faces = box_faces(iw, ih, idp, cube.painter)
                fw, fh = box_uv_size(iw, ih, idp)
                u, v = packer.alloc(fw, fh)
                offsets[id(cube)] = (u, v)
                placements = {
                    "up": (u + idp, v),
                    "down": (u + idp + iw, v),
                    "right": (u, v + idp),
                    "front": (u + idp, v + idp),
                    "left": (u + idp + iw, v + idp),
                    "back": (u + 2 * idp + iw, v + idp),
                }
                for face_name, (px, py) in placements.items():
                    face_img = faces[face_name]
                    fh2, fw2 = face_img.shape[:2]
                    target[py:py + fh2, px:px + fw2] = face_img
                    if glow_target is not None and cube.glow:
                        g = cube.glow(face_name, fw2, fh2)
                        if g is not None:
                            glow_target[py:py + fh2, px:px + fw2] = g
            for child in bone.children:
                walk(child)

        for root in model.roots.values():
            walk(root)
        return offsets

    aur_offsets_outer = paint_into(aur_outer, aur_packer, aur_img)
    aur_offsets_inner = paint_into(aur_inner, aur_packer, aur_img)
    rc_offsets_outer = paint_into(rc_outer, rc_packer, rc_img, rc_glow)
    rc_offsets_inner = paint_into(rc_inner, rc_packer, rc_img, rc_glow)

    Image.fromarray(aur_img, "RGBA").save(ARMOR_TEX / "aurite_armor.png")
    Image.fromarray(rc_img, "RGBA").save(ARMOR_TEX / "riftcrystal_armor.png")
    Image.fromarray(rc_glow, "RGBA").save(ARMOR_TEX / "riftcrystal_armor_glow.png")

    java = emit_java(aur_outer, aur_inner, rc_outer, rc_inner,
                     aur_offsets_outer, aur_offsets_inner, rc_offsets_outer, rc_offsets_inner)
    JAVA_OUT.parent.mkdir(parents=True, exist_ok=True)
    JAVA_OUT.write_text(java, encoding="utf-8")

    render_preview(aur_outer, aur_img, Path("/tmp/preview_v049_aurite_armor.png"))
    render_preview(rc_outer, rc_img, Path("/tmp/preview_v049_riftcrystal_armor.png"))

    print(f"OK v0.49.0 2x HD armors generated: "
          f"aurite_cubes={sum(len(b.cubes) for b in aur_outer.roots.values())} "
          f"riftcrystal_cubes={sum(len(b.cubes) for b in rc_outer.roots.values())} "
          f"glow_px={int(np.count_nonzero(rc_glow[:, :, 3]))}")


if __name__ == "__main__":
    main()
