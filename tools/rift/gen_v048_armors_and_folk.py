#!/usr/bin/env python3
"""
v0.48.0 — From-scratch generator for:
  1) 3 Custom 3D Elisar Settlement Folk (Elisar Villager, Elisar Guard, Elisar Archer)
  2) Crisp 32x32 GUI Item Icons for Materials + 16x16 Riftcrystal Ore Block Texture

  v0.51.0: the worn-armour sets (Aurite / Riftcrystal) were removed from the mod,
  so this generator now produces only the settlement folk.

Every Mojmap ModelPart.Cube uses strictly integer (W, H, D) box dimensions with CubeDeformation
for sub-pixel layering, and shares a single deterministic Box-UV packer & zero-overlap verifier
that emits BOTH the 128x128 PNG atlases AND the Java LayerDefinition classes:
  - ru.aiforge.riftworld.client.model.ElisarFolkModels
"""
from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Callable, Dict, List, Optional, Tuple
import math
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "mod/src/main/resources/assets/riftworld"
JAVA_MODEL_DIR = ROOT / "mod/src/main/java/ru/aiforge/riftworld/client/model"

RGBA = Tuple[int, int, int, int]


def hex_rgba(h: str, a: int = 255) -> RGBA:
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def mix(c1: RGBA, c2: RGBA, t: float) -> RGBA:
    t = max(0.0, min(1.0, t))
    return (
        int(round(c1[0] * (1.0 - t) + c2[0] * t)),
        int(round(c1[1] * (1.0 - t) + c2[1] * t)),
        int(round(c1[2] * (1.0 - t) + c2[2] * t)),
        int(round(c1[3] * (1.0 - t) + c2[3] * t)),
    )


def brighten(c: RGBA, factor: float) -> RGBA:
    return (
        int(max(0, min(255, round(c[0] * factor)))),
        int(max(0, min(255, round(c[1] * factor)))),
        int(max(0, min(255, round(c[2] * factor)))),
        c[3],
    )


def det_noise(x: int, y: int, seed: int = 17) -> float:
    n = (x * 73856093) ^ (y * 19349663) ^ (seed * 83492791)
    n = (n ^ (n >> 13)) * 1274126177
    return (((n ^ (n >> 16)) & 0xFFFF) / 32767.5) - 1.0  # [-1, 1]


# ==============================================================================
# Shared Mojmap Box-UV Packer & Face Painter (0 UV overlaps guaranteed)
# ==============================================================================

@dataclass
class CubeSpec:
    name: str
    ox: float
    oy: float
    oz: float
    w: int
    h: int
    d: int
    inflate: float = 0.0
    mirror: bool = False
    # Filled by UV packer:
    u: int = 0
    v: int = 0
    # Face painters: fn(face_name, width, height) -> np.ndarray(H, W, 4) uint8
    painter: Optional[Callable[[str, int, int], np.ndarray]] = None


@dataclass
class BoneSpec:
    name: str
    px: float = 0.0
    py: float = 0.0
    pz: float = 0.0
    rx: float = 0.0  # radians
    ry: float = 0.0
    rz: float = 0.0
    cubes: List[CubeSpec] = field(default_factory=list)
    children: List["BoneSpec"] = field(default_factory=list)


class AtlasModelBuilder:
    """Packs integer-sized Mojmap cubes onto an atlasW x atlasH PNG with 1px safety padding."""

    def __init__(self, model_id: str, atlas_w: int = 128, atlas_h: int = 128):
        self.model_id = model_id
        self.atlas_w = atlas_w
        self.atlas_h = atlas_h
        self.roots: List[BoneSpec] = []

    def all_cubes(self) -> List[CubeSpec]:
        out: List[CubeSpec] = []

        def walk(b: BoneSpec):
            out.extend(b.cubes)
            for ch in b.children:
                walk(ch)

        for r in self.roots:
            walk(r)
        return out

    def pack_uvs(self) -> None:
        cur_x = 1
        cur_y = 1
        row_h = 0
        occupied = np.zeros((self.atlas_h, self.atlas_w), dtype=np.int32)

        for idx, c in enumerate(self.all_cubes(), start=1):
            assert isinstance(c.w, int) and isinstance(c.h, int) and isinstance(c.d, int), c
            bw = 2 * (c.w + c.d)
            bh = c.d + c.h
            if cur_x + bw + 1 > self.atlas_w:
                cur_x = 1
                cur_y += row_h + 1
                row_h = 0
            if cur_y + bh + 1 > self.atlas_h:
                raise RuntimeError(f"[{self.model_id}] Atlas {self.atlas_w}x{self.atlas_h} overflow at cube {c.name}")
            c.u = cur_x
            c.v = cur_y
            # Verify 0 overlap across the 6 actual face rectangles
            for fname, fx, fy, fw, fh in self.face_rects(c):
                if fw <= 0 or fh <= 0:
                    continue
                if np.any(occupied[fy:fy + fh, fx:fx + fw] != 0):
                    raise RuntimeError(f"[{self.model_id}] UV overlap on {c.name}:{fname}")
                occupied[fy:fy + fh, fx:fx + fw] = idx
            cur_x += bw + 2
            row_h = max(row_h, bh)

    @staticmethod
    def face_rects(c: CubeSpec) -> List[Tuple[str, int, int, int, int]]:
        """
        Mojmap 1.20.1 ModelPart.Cube standard Box UV layout at texOffs(u, v) for integer (W, H, D):
          - up    (top,    -Y): (u + D,         v,     W, D)
          - down  (bottom, +Y): (u + D + W,     v,     W, D)
          - right (west,   -X): (u,             v + D, D, H)
          - front (north,  -Z): (u + D,         v + D, W, H)
          - left  (east,   +X): (u + D + W,     v + D, D, H)
          - back  (south,  +Z): (u + 2*D + W,   v + D, W, H)
        """
        u, v, w, h, d = c.u, c.v, c.w, c.h, c.d
        return [
            ("up", u + d, v, w, d),
            ("down", u + d + w, v, w, d),
            ("right", u, v + d, d, h),
            ("front", u + d, v + d, w, h),
            ("left", u + d + w, v + d, d, h),
            ("back", u + 2 * d + w, v + d, w, h),
        ]

    def paint_atlas(self) -> np.ndarray:
        atlas = np.zeros((self.atlas_h, self.atlas_w, 4), dtype=np.uint8)
        for c in self.all_cubes():
            if c.painter is None:
                continue
            for fname, fx, fy, fw, fh in self.face_rects(c):
                if fw <= 0 or fh <= 0:
                    continue
                patch = c.painter(fname, fw, fh)
                assert patch.shape == (fh, fw, 4), f"{c.name}:{fname} expected {(fh, fw, 4)} got {patch.shape}"
                atlas[fy:fy + fh, fx:fx + fw] = patch
        return atlas


def solid_face(base: RGBA, seed: int = 1, noise_amt: float = 0.04, border_dark: float = 0.90) -> Callable[[str, int, int], np.ndarray]:
    def fn(face: str, w: int, h: int) -> np.ndarray:
        shade_mul = {
            "up": 1.10,
            "front": 1.00,
            "left": 0.92,
            "right": 0.92,
            "back": 0.86,
            "down": 0.74,
        }.get(face, 1.0)
        arr = np.zeros((h, w, 4), dtype=np.uint8)
        for y in range(h):
            for x in range(w):
                m = shade_mul + det_noise(x, y, seed + hash(face) % 997) * noise_amt
                if (w >= 3 and (x == 0 or x == w - 1)) or (h >= 3 and (y == 0 or y == h - 1)):
                    m *= border_dark
                arr[y, x] = brighten(base, m)
        return arr

    return fn


def put_px(arr: np.ndarray, x: int, y: int, c: RGBA) -> None:
    if 0 <= y < arr.shape[0] and 0 <= x < arr.shape[1]:
        arr[y, x] = c


def fill_rect(arr: np.ndarray, x0: int, y0: int, w: int, h: int, c: RGBA, seed: int = 0, jitter: float = 0.0) -> None:
    for y in range(max(0, y0), min(arr.shape[0], y0 + h)):
        for x in range(max(0, x0), min(arr.shape[1], x0 + w)):
            if jitter > 0.0:
                arr[y, x] = brighten(c, 1.0 + det_noise(x, y, seed) * jitter)
            else:
                arr[y, x] = c


# ==============================================================================
# 1. AURITE MEDIUM ARMOR (Reference 01_aurite_medium_armor.png)
# ==============================================================================
AUR_DARK = hex_rgba("#2B241E")
AUR_PLATE_D = hex_rgba("#3D332B")
AUR_PLATE = hex_rgba("#52443A")
AUR_PLATE_L = hex_rgba("#6A5647")
AUR_LEATHER = hex_rgba("#7A5334")
AUR_GOLD_D = hex_rgba("#A66F28")
AUR_GOLD = hex_rgba("#D4963A")
AUR_GOLD_L = hex_rgba("#ECC265")
AUR_GOLD_HI = hex_rgba("#FAE08E")
AUR_TEAL_D = hex_rgba("#294D46")
AUR_TEAL = hex_rgba("#3B6C63")
AUR_TEAL_L = hex_rgba("#528A7F")





SKIN_HI = hex_rgba("#D8A690")
SKIN = hex_rgba("#CB957E")
SKIN_SH = hex_rgba("#B57E67")
SKIN_DARK = hex_rgba("#9B6651")
BROW_DARK = hex_rgba("#3B241A")
EYE_WHITE = hex_rgba("#F6F3EF")
EYE_GREEN = hex_rgba("#24721F")

LILAC_HI = hex_rgba("#C29BB3")
LILAC = hex_rgba("#AD839D")
LILAC_SH = hex_rgba("#916A84")
PLUM_DARK = hex_rgba("#62415B")
CREAM_HI = hex_rgba("#F4E8D8")
CREAM = hex_rgba("#E6D7C3")
CREAM_SH = hex_rgba("#CEBEA9")
SASH_TEAL = hex_rgba("#4A7A73")
SASH_TEAL_D = hex_rgba("#345B56")
SASH_TEAL_L = hex_rgba("#64968D")
ROBE_GOLD = hex_rgba("#DF9F42")
ROBE_GOLD_HI = hex_rgba("#F2C266")
ROBE_GOLD_D = hex_rgba("#AE7226")
PANTS_BROWN = hex_rgba("#5C3E33")
PANTS_DARK = hex_rgba("#452E25")
SOLE_GREY = hex_rgba("#7A7370")
SOLE_DARK = hex_rgba("#5D5755")

GUARD_GOLD = hex_rgba("#CE863B")
GUARD_GOLD_L = hex_rgba("#E8A954")
GUARD_GOLD_D = hex_rgba("#9E5E24")

ARCHER_LEATHER = hex_rgba("#6B4537")
ARCHER_LEATHER_D = hex_rgba("#4D3026")
ARCHER_LEATHER_L = hex_rgba("#875948")
FLETCH_WHITE = hex_rgba("#EEEAE8")
FLETCH_SH = hex_rgba("#C8C3C6")



def paint_villager_cranium(face: str, w: int, h: int) -> np.ndarray:
    arr = solid_face(SKIN, 101, 0.025, 0.96)(face, w, h)
    if face == "front":  # 8x10
        fill_rect(arr, 1, 0, 6, 4, SKIN_HI, 102, 0.02)
        # Continuous dark brown unibrow at y=4 (x=1..6)
        fill_rect(arr, 1, 4, 6, 1, BROW_DARK)
        # Crisp emerald-green eyes with outer white sclera at y=5
        put_px(arr, 1, 5, EYE_WHITE)
        put_px(arr, 2, 5, EYE_GREEN)
        put_px(arr, 3, 5, SKIN_SH)
        put_px(arr, 4, 5, SKIN_SH)
        put_px(arr, 5, 5, EYE_GREEN)
        put_px(arr, 6, 5, EYE_WHITE)
        # Cheek & mouth shading around the 3D nose
        fill_rect(arr, 1, 6, 6, 3, SKIN, 103, 0.02)
        put_px(arr, 2, 7, SKIN_DARK)
        put_px(arr, 5, 7, SKIN_DARK)
        fill_rect(arr, 1, 9, 6, 1, SKIN_SH)
    elif face == "up":
        fill_rect(arr, 1, 1, w - 2, h - 2, SKIN_HI, 104, 0.02)
    return arr


def paint_villager_nose(face: str, w: int, h: int) -> np.ndarray:
    arr = solid_face(SKIN_SH, 105, 0.02, 0.96)(face, w, h)
    if face == "front":
        fill_rect(arr, 0, 0, w, max(1, h - 1), SKIN)
        fill_rect(arr, 0, h - 1, w, 1, SKIN_DARK)
    return arr


def paint_robe_skirt(face: str, w: int, h: int) -> np.ndarray:
    """Long split-front lilac Elisar coat with dark plum center fold & gold+cream hem."""
    arr = solid_face(LILAC, 107, 0.03, 0.95)(face, w, h)
    # Bottom cream + gold hem across all 4 vertical sides
    if face in ("front", "back", "left", "right") and h >= 5:
        fill_rect(arr, 0, h - 2, w, 1, ROBE_GOLD)
        fill_rect(arr, 0, h - 1, w, 1, CREAM)
        if face == "front" and w == 8:
            # Split V-front opening showing dark plum inner fold & stepped gold border
            fill_rect(arr, 3, 0, 2, h - 2, PLUM_DARK)
            for y in range(0, h - 2):
                put_px(arr, 2 if y >= h - 3 else 3, y, PLUM_DARK)
            put_px(arr, 3, h - 3, ROBE_GOLD_HI)
            put_px(arr, 4, h - 3, ROBE_GOLD_HI)
            put_px(arr, 2, h - 2, ROBE_GOLD_HI)
            put_px(arr, 5, h - 2, ROBE_GOLD_HI)
            # Subtle central opening notch at bottom
            put_px(arr, 3, h - 1, PANTS_DARK)
            put_px(arr, 4, h - 1, PANTS_DARK)
    return arr


def paint_folk_leg(face: str, w: int, h: int) -> np.ndarray:
    arr = solid_face(PANTS_BROWN, 109, 0.03, 0.94)(face, w, h)
    if face in ("front", "back", "left", "right") and h == 12:
        fill_rect(arr, 0, 9, w, 1, PANTS_DARK)
        fill_rect(arr, 0, 10, w, 2, SOLE_GREY)
        fill_rect(arr, 0, 11, w, 1, SOLE_DARK)
    elif face == "down":
        fill_rect(arr, 0, 0, w, h, SOLE_DARK)
    return arr


def build_elisar_villager() -> AtlasModelBuilder:
    """Reference 05_elisar_villager.png — Peaceful lilac-robed Elisar Villager with crossed arms."""
    m = AtlasModelBuilder("elisar_villager", 128, 128)

    head = BoneSpec("head", cubes=[
        CubeSpec("cranium", -4.0, -10.0, -4.0, 8, 10, 8, 0.0, painter=paint_villager_cranium),
        CubeSpec("nose", -1.0, -3.0, -6.0, 2, 4, 2, 0.0, painter=paint_villager_nose),
    ])

    def paint_vil_torso(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(LILAC, 111, 0.03, 0.95)(face, w, h)
        if face == "front":  # 8x12
            # Gold V-collar around neck + plum lapel
            put_px(arr, 2, 0, ROBE_GOLD_HI)
            put_px(arr, 5, 0, ROBE_GOLD_HI)
            put_px(arr, 3, 0, SKIN_SH)
            put_px(arr, 4, 0, SKIN_SH)
            put_px(arr, 2, 1, ROBE_GOLD)
            put_px(arr, 5, 1, ROBE_GOLD)
            fill_rect(arr, 3, 1, 2, 8, PLUM_DARK)
            # Woven seafoam-teal sash at y=9..11
            fill_rect(arr, 0, 9, 8, 3, SASH_TEAL)
            for x in range(0, 8, 2):
                put_px(arr, x, 10, SASH_TEAL_L)
                put_px(arr, x + 1, 11, SASH_TEAL_D)
        elif face in ("back", "left", "right"):
            fill_rect(arr, 0, 9, w, 3, SASH_TEAL)
            fill_rect(arr, 0, 11, w, 1, SASH_TEAL_D)
        return arr

    def paint_vil_arm_side(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(CREAM, 113, 0.025, 0.96)(face, w, h)
        if face in ("front", "back", "left", "right") and h == 8:
            # Lilac upper shoulder transitioning into cream wide sleeve
            fill_rect(arr, 0, 0, w, 3, LILAC_HI, 114, 0.02)
            fill_rect(arr, 0, 3, w, 1, LILAC)
            fill_rect(arr, 0, 4, w, 4, CREAM_HI, 115, 0.02)
        elif face == "up":
            fill_rect(arr, 0, 0, w, h, LILAC_HI)
        return arr

    def paint_vil_arm_cross(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(CREAM_HI, 117, 0.02, 0.96)(face, w, h)
        if face in ("front", "up", "down", "back") and w == 8:
            # Central vertical lilac/plum sleeve cuff band right in the middle
            fill_rect(arr, 3, 0, 2, h, PLUM_DARK)
            put_px(arr, 2, 0, LILAC)
            put_px(arr, 5, 0, LILAC)
            if h >= 3:
                fill_rect(arr, 3, 1, 2, h - 2, LILAC_SH)
        return arr

    body = BoneSpec("body", cubes=[
        CubeSpec("torso", -4.0, 0.0, -3.0, 8, 12, 6, 0.0, painter=paint_vil_torso),
        CubeSpec("robe_skirt", -4.0, 12.0, -3.0, 8, 7, 6, 0.25, painter=paint_robe_skirt),
    ])
    arms = BoneSpec("arms", px=0.0, py=3.0, pz=-1.0, rx=-0.75, cubes=[
        CubeSpec("arm_r", -8.0, -2.0, -2.0, 4, 8, 4, 0.0, painter=paint_vil_arm_side),
        CubeSpec("arm_l", 4.0, -2.0, -2.0, 4, 8, 4, 0.0, painter=paint_vil_arm_side),
        CubeSpec("arm_cross", -4.0, 2.0, -2.0, 8, 4, 4, 0.0, painter=paint_vil_arm_cross),
    ])
    right_leg = BoneSpec("right_leg", px=-2.0, py=12.0, pz=0.0, cubes=[
        CubeSpec("leg_r", -2.0, 0.0, -2.0, 4, 12, 4, 0.0, painter=paint_folk_leg),
    ])
    left_leg = BoneSpec("left_leg", px=2.0, py=12.0, pz=0.0, cubes=[
        CubeSpec("leg_l", -2.0, 0.0, -2.0, 4, 12, 4, 0.0, painter=paint_folk_leg),
    ])

    m.roots = [head, body, arms, right_leg, left_leg]
    m.pack_uvs()
    return m


def build_elisar_guard() -> AtlasModelBuilder:
    """Reference 06_elisar_guard.png — Domed aurite-celadon helmet, cuirass, pauldrons & cuffed boots."""
    m = AtlasModelBuilder("elisar_guard", 128, 128)

    def paint_guard_dome(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(GUARD_GOLD, 121, 0.03, 0.94)(face, w, h)
        if face in ("front", "back", "left", "right"):
            fill_rect(arr, 1, 0, max(1, w - 2), 1, GUARD_GOLD_L)
            fill_rect(arr, 0, h - 1, w, 1, GUARD_GOLD_D)
        elif face == "up":
            fill_rect(arr, 1, 1, w - 2, h - 2, GUARD_GOLD_L)
        return arr

    head = BoneSpec("head", cubes=[
        CubeSpec("cranium", -4.0, -10.0, -4.0, 8, 10, 8, 0.0, painter=paint_villager_cranium),
        CubeSpec("nose", -1.0, -3.0, -6.0, 2, 4, 2, 0.0, painter=paint_villager_nose),
        CubeSpec("helm_brim", -4.0, -8.0, -4.0, 8, 1, 8, 0.42, painter=solid_face(SASH_TEAL, 122, 0.02)),
        CubeSpec("helm_ear_r", -4.0, -7.0, -2.0, 1, 2, 4, 0.38, painter=solid_face(SASH_TEAL_D, 120, 0.02)),
        CubeSpec("helm_ear_l", 3.0, -7.0, -2.0, 1, 2, 4, 0.38, painter=solid_face(SASH_TEAL_D, 124, 0.02)),
        CubeSpec("helm_dome", -4.0, -10.0, -4.0, 8, 2, 8, 0.32, painter=paint_guard_dome),
        CubeSpec("helm_crest", -1.0, -11.0, -4.0, 2, 3, 8, 0.38, painter=solid_face(SASH_TEAL_L, 123, 0.02)),
    ])
    hat = BoneSpec("hat")

    def paint_guard_torso(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(LILAC, 125, 0.03)(face, w, h)
        if face == "front":  # 8x12
            # Seafoam-teal shoulder straps (x=0..1 and x=6..7) + golden breastplate backing
            fill_rect(arr, 0, 0, 2, 8, SASH_TEAL)
            fill_rect(arr, 6, 0, 2, 8, SASH_TEAL)
            fill_rect(arr, 2, 1, 4, 8, GUARD_GOLD)
            fill_rect(arr, 0, 9, 8, 3, SASH_TEAL)
            fill_rect(arr, 2, 10, 4, 1, SASH_TEAL_L)
        elif face == "back":
            fill_rect(arr, 0, 0, 2, 8, SASH_TEAL)
            fill_rect(arr, 6, 0, 2, 8, SASH_TEAL)
            fill_rect(arr, 1, 1, 6, 7, GUARD_GOLD_D)
            fill_rect(arr, 0, 9, 8, 3, SASH_TEAL)
        elif face in ("left", "right"):
            fill_rect(arr, 0, 2, w, 6, GUARD_GOLD_D)
            fill_rect(arr, 0, 9, w, 3, SASH_TEAL)
        return arr

    def paint_guard_cuirass(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(GUARD_GOLD, 127, 0.025, 0.93)(face, w, h)
        if face == "front":  # 6x8
            fill_rect(arr, 1, 1, 4, 5, GUARD_GOLD_L, 128, 0.02)
            fill_rect(arr, 0, 7, 6, 1, GUARD_GOLD_D)
        return arr

    body = BoneSpec("body", cubes=[
        CubeSpec("torso", -4.0, 0.0, -3.0, 8, 12, 6, 0.0, painter=paint_guard_torso),
        CubeSpec("cuirass_front", -3.0, 1.0, -4.0, 6, 8, 1, 0.12, painter=paint_guard_cuirass),
        CubeSpec("sash_tails", -3.0, 10.0, -4.0, 6, 4, 1, 0.05, painter=solid_face(SASH_TEAL_L, 129, 0.02)),
        CubeSpec("robe_skirt", -4.0, 12.0, -3.0, 8, 6, 6, 0.24, painter=paint_robe_skirt),
    ])

    def paint_guard_arm(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(CREAM, 131, 0.025)(face, w, h)
        if face in ("front", "back", "left", "right") and h == 12:
            fill_rect(arr, 0, 0, w, 4, LILAC)
            fill_rect(arr, 0, 4, w, 6, CREAM_HI, 132, 0.02)
            fill_rect(arr, 0, 10, w, 2, SKIN)
        elif face == "down":
            fill_rect(arr, 0, 0, w, h, SKIN_SH)
        return arr

    def paint_guard_pauldron(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(GUARD_GOLD, 133, 0.025)(face, w, h)
        if face in ("front", "back", "left", "right") and h == 4:
            fill_rect(arr, 1, 0, max(1, w - 2), 2, GUARD_GOLD_L)
            fill_rect(arr, 0, 3, w, 1, SASH_TEAL)
        elif face == "up":
            fill_rect(arr, 1, 1, max(1, w - 2), max(1, h - 2), GUARD_GOLD_L)
        return arr

    right_arm = BoneSpec("right_arm", px=-5.0, py=2.0, pz=0.0, cubes=[
        CubeSpec("arm_r", -3.0, -2.0, -2.0, 4, 12, 4, 0.0, painter=paint_guard_arm),
        CubeSpec("pauldron_r", -4.0, -3.0, -3.0, 5, 4, 6, 0.25, painter=paint_guard_pauldron),
    ])
    left_arm = BoneSpec("left_arm", px=5.0, py=2.0, pz=0.0, cubes=[
        CubeSpec("arm_l", -1.0, -2.0, -2.0, 4, 12, 4, 0.0, painter=paint_guard_arm),
        CubeSpec("pauldron_l", -1.0, -3.0, -3.0, 5, 4, 6, 0.25, painter=paint_guard_pauldron),
    ])

    def paint_guard_boot(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(GUARD_GOLD, 135, 0.03)(face, w, h)
        if face in ("front", "back", "left", "right") and h == 5:
            fill_rect(arr, 0, 0, w, 1, GUARD_GOLD_L)
            fill_rect(arr, 0, 3, w, 1, GUARD_GOLD_D)
            fill_rect(arr, 0, 4, w, 1, SOLE_GREY)
            if face == "front":
                fill_rect(arr, 1, 3, 2, 2, SOLE_GREY)
        elif face == "down":
            fill_rect(arr, 0, 0, w, h, SOLE_DARK)
        return arr

    right_leg = BoneSpec("right_leg", px=-2.0, py=12.0, pz=0.0, cubes=[
        CubeSpec("leg_r", -2.0, 0.0, -2.0, 4, 12, 4, 0.0, painter=paint_folk_leg),
        CubeSpec("boot_r", -2.0, 7.0, -2.0, 4, 5, 4, 0.34, painter=paint_guard_boot),
    ])
    left_leg = BoneSpec("left_leg", px=2.0, py=12.0, pz=0.0, cubes=[
        CubeSpec("leg_l", -2.0, 0.0, -2.0, 4, 12, 4, 0.0, painter=paint_folk_leg),
        CubeSpec("boot_l", -2.0, 7.0, -2.0, 4, 5, 4, 0.34, painter=paint_guard_boot),
    ])

    m.roots = [head, hat, body, right_arm, left_arm, right_leg, left_leg]
    m.pack_uvs()
    return m


def build_elisar_archer() -> AtlasModelBuilder:
    """Reference 07_elisar_archer.png — Bare villager head, asymmetric teal right pauldron, studded vest & 3D back quiver."""
    m = AtlasModelBuilder("elisar_archer", 128, 128)

    head = BoneSpec("head", cubes=[
        CubeSpec("cranium", -4.0, -10.0, -4.0, 8, 10, 8, 0.0, painter=paint_villager_cranium),
        CubeSpec("nose", -1.0, -3.0, -6.0, 2, 4, 2, 0.0, painter=paint_villager_nose),
    ])
    hat = BoneSpec("hat")

    def paint_archer_torso(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(ARCHER_LEATHER, 141, 0.035)(face, w, h)
        if face == "front":  # 8x12
            # Lilac & gold collar at y=0..1
            fill_rect(arr, 1, 0, 6, 2, LILAC_HI)
            put_px(arr, 2, 1, ROBE_GOLD_HI)
            put_px(arr, 5, 1, ROBE_GOLD_HI)
            put_px(arr, 3, 0, SKIN_SH)
            put_px(arr, 4, 0, SKIN_SH)
            # Studded leather vest (y=2..8) + diagonal quiver strap from right shoulder (x=1,y=1) to left hip (x=6,y=8)
            for idx, x in enumerate(range(1, 7)):
                put_px(arr, x, 2 + idx, ARCHER_LEATHER_D)
                if 2 + idx + 1 < 9:
                    put_px(arr, x, 2 + idx + 1, ARCHER_LEATHER_L)
            # Gold rivets on leather vest
            for gx, gy in [(2, 4), (5, 4), (2, 6), (5, 6)]:
                put_px(arr, gx, gy, ROBE_GOLD_HI)
            # Seafoam-teal sash (y=9..11) + golden center belt buckle
            fill_rect(arr, 0, 9, 8, 3, SASH_TEAL)
            fill_rect(arr, 3, 9, 2, 2, ROBE_GOLD_HI)
            put_px(arr, 3, 11, SASH_TEAL_L)
            put_px(arr, 4, 11, SASH_TEAL_L)
        elif face in ("back", "left", "right"):
            fill_rect(arr, 0, 9, w, 3, SASH_TEAL)
        return arr

    def paint_quiver_fletch(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(FLETCH_WHITE, 145, 0.02, 0.96)(face, w, h)
        for x in range(0, w, 2):
            fill_rect(arr, x, 0, 1, h, FLETCH_SH)
        return arr

    quiver = BoneSpec("quiver", px=-3.8, py=-0.5, pz=2.4, rz=-0.42, cubes=[
        CubeSpec("quiver_tube", -2.0, -2.0, 0.0, 4, 11, 3, 0.0, painter=solid_face(ARCHER_LEATHER_D, 143, 0.04)),
        CubeSpec("quiver_shafts", -2.0, -5.0, 1.0, 3, 3, 1, 0.0, painter=solid_face(ROBE_GOLD_D, 144, 0.02)),
        CubeSpec("quiver_fletch", -3.0, -8.0, 0.0, 5, 3, 3, 0.0, painter=paint_quiver_fletch),
    ])

    body = BoneSpec("body", cubes=[
        CubeSpec("torso", -4.0, 0.0, -3.0, 8, 12, 6, 0.0, painter=paint_archer_torso),
        CubeSpec("belt_buckle", -1.0, 9.0, -4.0, 2, 2, 1, 0.08, painter=solid_face(ROBE_GOLD_HI, 146, 0.01)),
        CubeSpec("robe_skirt", -4.0, 12.0, -3.0, 8, 7, 6, 0.24, painter=paint_robe_skirt),
    ], children=[quiver])

    def paint_archer_arm(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(CREAM, 147, 0.025)(face, w, h)
        if face in ("front", "back", "left", "right") and h == 12:
            fill_rect(arr, 0, 0, w, 3, LILAC)
            fill_rect(arr, 0, 3, w, 3, CREAM_HI)
            # Studded brown leather bracer at y=6..9 + tan hand at y=10..11
            fill_rect(arr, 0, 6, w, 4, ARCHER_LEATHER_D)
            put_px(arr, w // 2, 7, ROBE_GOLD_HI)
            fill_rect(arr, 0, 10, w, 2, SKIN)
        elif face == "down":
            fill_rect(arr, 0, 0, w, h, SKIN_SH)
        return arr

    def paint_archer_pauldron(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(SASH_TEAL, 149, 0.03)(face, w, h)
        if face == "front":
            put_px(arr, w - 2, h - 2, ROBE_GOLD_HI)
            fill_rect(arr, 0, 0, w, 1, SASH_TEAL_L)
        elif face == "up":
            fill_rect(arr, 1, 1, max(1, w - 2), max(1, h - 2), SASH_TEAL_L)
        return arr

    right_arm = BoneSpec("right_arm", px=-5.0, py=2.0, pz=0.0, cubes=[
        CubeSpec("arm_r", -3.0, -2.0, -2.0, 4, 12, 4, 0.0, painter=paint_archer_arm),
        # Asymmetric right shoulder seafoam-teal pauldron ONLY on right_arm!
        CubeSpec("pauldron_r", -4.0, -3.0, -3.0, 5, 4, 6, 0.28, painter=paint_archer_pauldron),
    ])
    left_arm = BoneSpec("left_arm", px=5.0, py=2.0, pz=0.0, cubes=[
        CubeSpec("arm_l", -1.0, -2.0, -2.0, 4, 12, 4, 0.0, painter=paint_archer_arm),
    ])

    right_leg = BoneSpec("right_leg", px=-2.0, py=12.0, pz=0.0, cubes=[
        CubeSpec("leg_r", -2.0, 0.0, -2.0, 4, 12, 4, 0.0, painter=paint_folk_leg),
    ])
    left_leg = BoneSpec("left_leg", px=2.0, py=12.0, pz=0.0, cubes=[
        CubeSpec("leg_l", -2.0, 0.0, -2.0, 4, 12, 4, 0.0, painter=paint_folk_leg),
    ])

    m.roots = [head, hat, body, right_arm, left_arm, right_leg, left_leg]
    m.pack_uvs()
    return m


# ==============================================================================
# 4. JAVA CODEGEN FOR ElisarArmorModels.java & ElisarFolkModels.java
# ==============================================================================

def emit_bone_java(b: BoneSpec, parent_var: str, indent: str = "        ") -> List[str]:
    lines: List[str] = []
    var_name = f"p_{b.name}"
    if not b.cubes:
        cube_expr = "CubeListBuilder.create()"
    else:
        parts = ["CubeListBuilder.create()"]
        for c in b.cubes:
            parts.append(
                f".texOffs({c.u}, {c.v}).addBox({c.ox:.2f}F, {c.oy:.2f}F, {c.oz:.2f}F, "
                f"{c.w}.0F, {c.h}.0F, {c.d}.0F, new CubeDeformation({c.inflate:.2f}F))"
            )
        cube_expr = ("\n" + indent + "        ").join(parts)

    if abs(b.rx) > 1e-5 or abs(b.ry) > 1e-5 or abs(b.rz) > 1e-5:
        pose_expr = f"PartPose.offsetAndRotation({b.px:.2f}F, {b.py:.2f}F, {b.pz:.2f}F, {b.rx:.4f}F, {b.ry:.4f}F, {b.rz:.4f}F)"
    else:
        pose_expr = f"PartPose.offset({b.px:.2f}F, {b.py:.2f}F, {b.pz:.2f}F)"

    if b.children:
        lines.append(f'{indent}PartDefinition {var_name} = {parent_var}.addOrReplaceChild("{b.name}", {cube_expr}, {pose_expr});')
        for ch in b.children:
            lines.extend(emit_bone_java(ch, var_name, indent))
    else:
        lines.append(f'{indent}{parent_var}.addOrReplaceChild("{b.name}", {cube_expr}, {pose_expr});')
    return lines



def write_folk_models_java(vil: AtlasModelBuilder, grd: AtlasModelBuilder, arc: AtlasModelBuilder) -> None:
    vil_lines, grd_lines, arc_lines = [], [], []
    for b in vil.roots:
        vil_lines.extend(emit_bone_java(b, "root", "            "))
    for b in grd.roots:
        grd_lines.extend(emit_bone_java(b, "root", "            "))
    for b in arc.roots:
        arc_lines.extend(emit_bone_java(b, "root", "            "))

    code = f"""package ru.aiforge.riftworld.client.model;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.EntityRenderersEvent;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.entity.ElisarArcherEntity;
import ru.aiforge.riftworld.entity.ElisarGuardEntity;
import ru.aiforge.riftworld.entity.ElisarVillagerEntity;

/**
 * Generated by tools/rift/gen_v048_armors_and_folk.py — DO NOT HAND-EDIT UVS.
 * Custom 128x128 3D Villager-proportioned models matching references 05, 06, 07.
 */
public final class ElisarFolkModels {{
    public static final ModelLayerLocation VILLAGER_LAYER = new ModelLayerLocation(
            new ResourceLocation(RiftWorld.MODID, "elisar_villager"), "main");
    public static final ModelLayerLocation GUARD_LAYER = new ModelLayerLocation(
            new ResourceLocation(RiftWorld.MODID, "elisar_guard"), "main");
    public static final ModelLayerLocation ARCHER_LAYER = new ModelLayerLocation(
            new ResourceLocation(RiftWorld.MODID, "elisar_archer"), "main");

    private ElisarFolkModels() {{
    }}

    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {{
        e.registerLayerDefinition(VILLAGER_LAYER, Villager::createBodyLayer);
        e.registerLayerDefinition(GUARD_LAYER, Guard::createBodyLayer);
        e.registerLayerDefinition(ARCHER_LAYER, Archer::createBodyLayer);
    }}

    public static final class Villager extends HierarchicalModel<ElisarVillagerEntity> {{
        private final ModelPart root;
        private final ModelPart head;
        private final ModelPart arms;
        private final ModelPart rightLeg;
        private final ModelPart leftLeg;

        public Villager(ModelPart root) {{
            this.root = root;
            this.head = root.getChild("head");
            this.arms = root.getChild("arms");
            this.rightLeg = root.getChild("right_leg");
            this.leftLeg = root.getChild("left_leg");
        }}

        public static LayerDefinition createBodyLayer() {{
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
{chr(10).join(vil_lines)}
            return LayerDefinition.create(mesh, 128, 128);
        }}

        @Override
        public ModelPart root() {{
            return this.root;
        }}

        @Override
        public void setupAnim(ElisarVillagerEntity entity, float limbSwing, float limbSwingAmount,
                              float ageInTicks, float netHeadYaw, float headPitch) {{
            this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
            this.head.xRot = headPitch * Mth.DEG_TO_RAD;
            if (entity.getUnhappyCounter() > 0) {{
                this.head.zRot = 0.22F * Mth.sin(0.45F * ageInTicks);
            }} else {{
                this.head.zRot = 0.0F;
            }}
            this.arms.xRot = -0.75F + 0.03F * Mth.sin(ageInTicks * 0.06F);
            this.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.2F * limbSwingAmount * 0.5F;
            this.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.2F * limbSwingAmount * 0.5F;
        }}
    }}

    public static final class Guard extends HumanoidModel<ElisarGuardEntity> {{
        public Guard(ModelPart root) {{
            super(root);
        }}

        public static LayerDefinition createBodyLayer() {{
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
{chr(10).join(grd_lines)}
            return LayerDefinition.create(mesh, 128, 128);
        }}

        @Override
        public void prepareMobModel(ElisarGuardEntity entity, float limbSwing, float limbSwingAmount, float partialTick) {{
            this.rightArmPose = ArmPose.ITEM;
            this.leftArmPose = ArmPose.ITEM;
            if (entity.isBlocking()) {{
                if (entity.getMainArm() == HumanoidArm.LEFT) {{
                    this.rightArmPose = ArmPose.BLOCK;
                }} else {{
                    this.leftArmPose = ArmPose.BLOCK;
                }}
            }}
            super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
        }}

        @Override
        public void setupAnim(ElisarGuardEntity entity, float limbSwing, float limbSwingAmount,
                              float ageInTicks, float netHeadYaw, float headPitch) {{
            super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            // Slight ready stance matching reference 06_elisar_guard.png
            if (!entity.isAggressive() && limbSwingAmount < 0.05F) {{
                this.leftArm.xRot = -0.22F;
                this.rightArm.xRot = -0.18F;
            }}
        }}
    }}

    public static final class Archer extends HumanoidModel<ElisarArcherEntity> {{
        public Archer(ModelPart root) {{
            super(root);
        }}

        public static LayerDefinition createBodyLayer() {{
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
{chr(10).join(arc_lines)}
            return LayerDefinition.create(mesh, 128, 128);
        }}

        @Override
        public void prepareMobModel(ElisarArcherEntity entity, float limbSwing, float limbSwingAmount, float partialTick) {{
            this.rightArmPose = ArmPose.EMPTY;
            this.leftArmPose = ArmPose.EMPTY;
            ItemStack main = entity.getItemInHand(InteractionHand.MAIN_HAND);
            if (main.getItem() instanceof BowItem && entity.isAggressive()) {{
                if (entity.getMainArm() == HumanoidArm.RIGHT) {{
                    this.rightArmPose = ArmPose.BOW_AND_ARROW;
                }} else {{
                    this.leftArmPose = ArmPose.BOW_AND_ARROW;
                }}
            }}
            super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
        }}

        @Override
        public void setupAnim(ElisarArcherEntity entity, float limbSwing, float limbSwingAmount,
                              float ageInTicks, float netHeadYaw, float headPitch) {{
            super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            if (!entity.isAggressive() && limbSwingAmount < 0.05F) {{
                // Relaxed bow-in-left-hand pose matching reference 07_elisar_archer.png
                this.leftArm.xRot = -0.45F;
                this.leftArm.yRot = -0.12F;
                this.rightArm.xRot = -0.08F;
            }}
        }}
    }}
}}
"""
    (JAVA_MODEL_DIR / "ElisarFolkModels.java").write_text(code, encoding="utf-8")


# ==============================================================================
# 5. CRISP 32x32 GUI ARMOR/MATERIAL ICONS + 16x16 RIFTCRYSTAL ORE BLOCK TEXTURE
# ==============================================================================

def draw_pattern(grid: List[str], pal: Dict[str, RGBA]) -> np.ndarray:
    h = len(grid)
    w = len(grid[0])
    arr = np.zeros((h, w, 4), dtype=np.uint8)
    for y, row in enumerate(grid):
        assert len(row) == w, f"Row {y} len {len(row)} != {w}"
        for x, ch in enumerate(row):
            if ch != ".":
                arr[y, x] = pal[ch]
    return arr


def upscale_2x_bevel(arr16: np.ndarray, outline_col: RGBA) -> np.ndarray:
    """Crisp 16x16 -> 32x32 pixel-art expansion with clean sub-pixel corner smoothing & bevel."""
    h, w, _ = arr16.shape
    out = np.zeros((h * 2, w * 2, 4), dtype=np.uint8)
    for y in range(h):
        for x in range(w):
            c = tuple(int(v) for v in arr16[y, x])
            if c[3] == 0:
                continue
            out[2 * y, 2 * x] = brighten(c, 1.05)
            out[2 * y, 2 * x + 1] = c
            out[2 * y + 1, 2 * x] = c
            out[2 * y + 1, 2 * x + 1] = brighten(c, 0.93)
    return out



def main() -> None:
    # v0.51.0: броня мода удалена целиком — генератор теперь делает только
    # жителей Элисара (житель, страж, лучник).
    entity_dir = ASSETS / "textures/entity"
    entity_dir.mkdir(parents=True, exist_ok=True)

    vil = build_elisar_villager()
    grd = build_elisar_guard()
    arc = build_elisar_archer()
    Image.fromarray(vil.paint_atlas(), "RGBA").save(entity_dir / "elisar_villager.png")
    Image.fromarray(grd.paint_atlas(), "RGBA").save(entity_dir / "elisar_guard.png")
    Image.fromarray(arc.paint_atlas(), "RGBA").save(entity_dir / "elisar_archer.png")
    write_folk_models_java(vil, grd, arc)

    print(
        f"OK v0.51.0 folk generated: "
        f"villager={len(vil.all_cubes())} guard={len(grd.all_cubes())} archer={len(arc.all_cubes())}"
    )


if __name__ == "__main__":
    main()
