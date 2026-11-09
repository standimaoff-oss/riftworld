#!/usr/bin/env python3
"""
v0.48.0 — From-scratch generator for:
  1) 2 Custom 3D Worn Armor Sets (Aurite Medium Armor + Riftcrystal Strong Armor + Emissive Glow)
  2) 3 Custom 3D Elisar Settlement Folk (Elisar Villager, Elisar Guard, Elisar Archer)
  3) Crisp 32x32 GUI Item Icons for Armors & Materials + 16x16 Riftcrystal Ore Block Texture

Every Mojmap ModelPart.Cube uses strictly integer (W, H, D) box dimensions with CubeDeformation
for sub-pixel layering, and shares a single deterministic Box-UV packer & zero-overlap verifier
that emits BOTH the 128x128 PNG atlases AND the Java LayerDefinition classes:
  - ru.aiforge.riftworld.client.model.ElisarArmorModels
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


def build_aurite_armor() -> AtlasModelBuilder:
    m = AtlasModelBuilder("aurite_armor", 128, 128)

    # --- HEAD (Open T-visor bronze/aurite barbute helm) ---
    def paint_crown(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(AUR_PLATE, 11, 0.035)(face, w, h)
        if face == "front":  # 8x3
            # Broad golden T-crown brow (x=1..6 at y=0, x=2..5 at y=1, x=3..4 at y=2)
            for x in range(1, 7):
                put_px(arr, x, 0, AUR_GOLD_L if 2 <= x <= 5 else AUR_GOLD)
            put_px(arr, 3, 0, AUR_GOLD_HI)
            put_px(arr, 4, 0, AUR_GOLD_HI)
            for x in range(2, 6):
                put_px(arr, x, 1, AUR_GOLD_L if 3 <= x <= 4 else AUR_GOLD)
            put_px(arr, 3, 2, AUR_GOLD_L)
            put_px(arr, 4, 2, AUR_GOLD_L)
            # Oxidized seafoam-teal corner rivets
            put_px(arr, 0, 1, AUR_TEAL)
            put_px(arr, 7, 1, AUR_TEAL)
            put_px(arr, 0, 2, AUR_DARK)
            put_px(arr, 7, 2, AUR_DARK)
        elif face in ("left", "right"):  # 8x3
            fill_rect(arr, 0, 1, w, 1, AUR_GOLD, 12, 0.03)
            put_px(arr, 1, 1, AUR_GOLD_L)
            put_px(arr, 5, 2, AUR_TEAL)
        elif face == "back":  # 8x3
            fill_rect(arr, 0, 1, w, 1, AUR_GOLD_D)
            fill_rect(arr, 3, 0, 2, 3, AUR_GOLD)
        elif face == "up":  # 8x8
            fill_rect(arr, 3, 0, 2, 8, AUR_GOLD, 13, 0.03)
            fill_rect(arr, 1, 0, 6, 2, AUR_GOLD_L)
        return arr

    def paint_helm_cheek(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(AUR_PLATE, 19, 0.035)(face, w, h)
        if face == "front":  # 2x5
            fill_rect(arr, 0, 0, w, 1, AUR_DARK)
            put_px(arr, 0, 3, AUR_GOLD)
            put_px(arr, 1, 3, AUR_GOLD_D)
            fill_rect(arr, 0, 4, w, 1, AUR_PLATE_D)
        elif face in ("left", "right"):  # 4x5
            put_px(arr, 2, 0, AUR_TEAL)
            put_px(arr, 1, 3, AUR_GOLD)
        return arr

    def paint_nasal(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(AUR_GOLD, 23, 0.02, 0.95)(face, w, h)
        if face == "front":
            fill_rect(arr, 0, 0, w, max(1, h - 1), AUR_GOLD_L)
            put_px(arr, 0, 0, AUR_GOLD_HI)
            if w > 1:
                put_px(arr, 1, 0, AUR_GOLD_HI)
            fill_rect(arr, 0, h - 1, w, 1, AUR_GOLD_D)
        elif face == "up":
            fill_rect(arr, 0, 0, w, h, AUR_GOLD_HI)
        return arr

    head = BoneSpec("head", cubes=[
        CubeSpec("helm_visor_shadow", -4.0, -8.0, -4.0, 8, 8, 8, 0.14, painter=solid_face(hex_rgba("#120E0D"), 14, 0.02)),
        CubeSpec("helm_crown", -4.0, -8.0, -4.0, 8, 3, 8, 0.55, painter=paint_crown),
        CubeSpec("helm_back", -4.0, -5.0, 0.0, 8, 5, 4, 0.52, painter=solid_face(AUR_PLATE_D, 15, 0.04)),
        CubeSpec("helm_cheek_r", -4.0, -5.0, -4.0, 2, 5, 4, 0.52, painter=paint_helm_cheek),
        CubeSpec("helm_cheek_l", 2.0, -5.0, -4.0, 2, 5, 4, 0.52, painter=paint_helm_cheek),
        CubeSpec("helm_chin_r", -3.0, -2.0, -4.0, 1, 2, 2, 0.46, painter=solid_face(AUR_PLATE_D, 16)),
        CubeSpec("helm_chin_l", 2.0, -2.0, -4.0, 1, 2, 2, 0.46, painter=solid_face(AUR_PLATE_D, 17)),
        CubeSpec("helm_nasal", -1.0, -8.0, -5.0, 2, 4, 5, 0.14, painter=paint_nasal),
    ])
    hat = BoneSpec("hat")

    # --- CHEST + LEGS waist on body ---
    def paint_cuirass(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(AUR_PLATE, 31, 0.04)(face, w, h)
        if face == "front":  # 8x10
            # Teal & gold V-gorget collar + golden chest medallion
            put_px(arr, 1, 0, AUR_GOLD)
            put_px(arr, 6, 0, AUR_GOLD)
            put_px(arr, 2, 0, AUR_TEAL)
            put_px(arr, 5, 0, AUR_TEAL)
            put_px(arr, 3, 0, AUR_DARK)
            put_px(arr, 4, 0, AUR_DARK)
            put_px(arr, 2, 1, AUR_GOLD)
            put_px(arr, 5, 1, AUR_GOLD)
            put_px(arr, 3, 1, AUR_TEAL_L)
            put_px(arr, 4, 1, AUR_TEAL_L)
            # Central golden medallion (2x2 at y=2..3)
            fill_rect(arr, 3, 2, 2, 2, AUR_GOLD_L)
            put_px(arr, 3, 2, AUR_GOLD_HI)
            # Sculpted bronze pectoral & rib lines
            fill_rect(arr, 1, 3, 2, 3, AUR_PLATE_L, 32, 0.03)
            fill_rect(arr, 5, 3, 2, 3, AUR_PLATE_L, 33, 0.03)
            fill_rect(arr, 1, 6, 6, 1, AUR_PLATE_D)
            # Belt with two orange-gold rivets & central golden buckle (y=7)
            fill_rect(arr, 0, 7, 8, 1, AUR_DARK)
            put_px(arr, 1, 7, AUR_GOLD)
            put_px(arr, 6, 7, AUR_GOLD)
            put_px(arr, 3, 7, AUR_GOLD_HI)
            put_px(arr, 4, 7, AUR_GOLD_L)
            # Lower abdominal plate + teal trim
            fill_rect(arr, 1, 8, 6, 1, AUR_PLATE)
            fill_rect(arr, 2, 9, 4, 1, AUR_TEAL)
        elif face == "back":  # 8x10
            fill_rect(arr, 3, 0, 2, 7, AUR_PLATE_D)
            fill_rect(arr, 1, 0, 6, 1, AUR_TEAL)
            fill_rect(arr, 0, 7, 8, 1, AUR_DARK)
            fill_rect(arr, 3, 7, 2, 1, AUR_GOLD)
        return arr

    def paint_plastron(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(AUR_PLATE_L, 35, 0.03)(face, w, h)
        if face == "front":  # 6x5
            put_px(arr, 1, 0, AUR_GOLD)
            put_px(arr, 4, 0, AUR_GOLD)
            put_px(arr, 2, 0, AUR_TEAL)
            put_px(arr, 3, 0, AUR_TEAL)
            fill_rect(arr, 2, 1, 2, 2, AUR_GOLD_L)
            put_px(arr, 2, 1, AUR_GOLD_HI)
            fill_rect(arr, 0, 4, 6, 1, AUR_PLATE_D)
        return arr

    def paint_fauld(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(AUR_PLATE_D, 37, 0.03)(face, w, h)
        if face in ("front", "back", "left", "right"):
            fill_rect(arr, 0, h - 1, w, 1, AUR_TEAL_D)
            if face == "front" and w == 8:
                fill_rect(arr, 2, h - 1, 4, 1, AUR_TEAL)
                put_px(arr, 3, 0, AUR_GOLD)
                put_px(arr, 4, 0, AUR_GOLD)
        return arr

    body = BoneSpec("body", cubes=[
        CubeSpec("chest_cuirass", -4.0, 0.0, -2.0, 8, 10, 4, 0.55, painter=paint_cuirass),
        CubeSpec("chest_plastron", -3.0, 1.0, -3.0, 6, 5, 1, 0.16, painter=paint_plastron),
        CubeSpec("chest_fauld", -4.0, 9.0, -2.0, 8, 3, 4, 0.64, painter=paint_fauld),
        CubeSpec("legs_belt", -4.0, 9.0, -2.0, 8, 3, 4, 0.36, painter=solid_face(AUR_LEATHER, 39, 0.03)),
    ])

    # --- SHORT OPEN SHOULDER PAULDRONS (lower arms uncovered!) ---
    def paint_pauldron(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(AUR_PLATE, 41, 0.035)(face, w, h)
        if face in ("front", "back", "left", "right") and h == 5:
            fill_rect(arr, 1, 1, max(1, w - 2), 1, AUR_PLATE_L)
            put_px(arr, w // 2, 1, AUR_GOLD_D)
            # Bright golden-aurite band at y=3 + dark seafoam-teal lower border at y=4
            fill_rect(arr, 0, 3, w, 1, AUR_GOLD_L)
            put_px(arr, 0, 3, AUR_GOLD)
            put_px(arr, w - 1, 3, AUR_GOLD)
            fill_rect(arr, 0, 4, w, 1, AUR_TEAL_D)
            if w >= 3:
                fill_rect(arr, 1, 4, w - 2, 1, AUR_TEAL)
        elif face == "up":
            fill_rect(arr, 1, 1, max(1, w - 2), max(1, h - 2), AUR_PLATE_L, 42, 0.03)
            put_px(arr, w // 2, h // 2, AUR_GOLD)
        return arr

    right_arm = BoneSpec("right_arm", px=-5.0, py=2.0, pz=0.0, cubes=[
        CubeSpec("chest_pauldron_r", -4.0, -3.0, -3.0, 5, 5, 6, 0.36, painter=paint_pauldron),
        CubeSpec("chest_pauldron_cap_r", -4.0, -4.0, -2.0, 4, 2, 4, 0.22, painter=solid_face(AUR_PLATE_L, 43, 0.03)),
    ])
    left_arm = BoneSpec("left_arm", px=5.0, py=2.0, pz=0.0, cubes=[
        CubeSpec("chest_pauldron_l", -1.0, -3.0, -3.0, 5, 5, 6, 0.36, painter=paint_pauldron),
        CubeSpec("chest_pauldron_cap_l", 0.0, -4.0, -2.0, 4, 2, 4, 0.22, painter=solid_face(AUR_PLATE_L, 44, 0.03)),
    ])

    # --- LEGS (Thigh cuisses with teal band + 3D golden knee cops) & FEET (Bronze boots + 3D golden toes) ---
    def paint_cuisse(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(AUR_PLATE, 51, 0.035)(face, w, h)
        if face in ("front", "back", "left", "right") and h == 8:
            fill_rect(arr, 0, 0, w, 3, AUR_LEATHER, 52, 0.04)
            fill_rect(arr, 0, 3, w, 1, AUR_TEAL)
            fill_rect(arr, 0, 4, w, 1, AUR_TEAL_D)
            fill_rect(arr, 0, 5, w, 3, AUR_PLATE_D, 53, 0.03)
            if face in ("left", "right"):
                fill_rect(arr, 0, 6, w, 1, AUR_GOLD_D)
        return arr

    def paint_knee_cop(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(AUR_GOLD, 55, 0.02)(face, w, h)
        if face == "front":  # 4x3
            fill_rect(arr, 1, 0, 2, 2, AUR_GOLD_HI)
            put_px(arr, 1, 1, AUR_GOLD_L)
            fill_rect(arr, 0, 2, w, 1, AUR_GOLD_D)
        return arr

    def paint_boot_greave(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(AUR_PLATE_D, 57, 0.035)(face, w, h)
        if face in ("front", "back", "left", "right") and h == 4:
            fill_rect(arr, 0, 0, w, 1, AUR_LEATHER)
            if face in ("left", "right"):
                put_px(arr, w - 1, 1, AUR_GOLD)
            fill_rect(arr, 0, 3, w, 1, AUR_DARK)
        return arr

    def paint_boot_toe(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(AUR_GOLD, 59, 0.02)(face, w, h)
        if face in ("front", "up"):
            fill_rect(arr, 1, 0, max(1, w - 2), max(1, h - 1), AUR_GOLD_HI)
        elif face == "down":
            fill_rect(arr, 0, 0, w, h, AUR_DARK)
        return arr

    right_leg = BoneSpec("right_leg", px=-1.9, py=12.0, pz=0.0, cubes=[
        CubeSpec("legs_cuisse_r", -2.0, 0.0, -2.0, 4, 8, 4, 0.36, painter=paint_cuisse),
        CubeSpec("legs_knee_r", -2.0, 5.0, -3.0, 4, 3, 2, 0.24, painter=paint_knee_cop),
        CubeSpec("feet_greave_r", -2.0, 8.0, -2.0, 4, 4, 4, 0.52, painter=paint_boot_greave),
        CubeSpec("feet_toe_r", -2.0, 10.0, -4.0, 4, 2, 2, 0.34, painter=paint_boot_toe),
    ])
    left_leg = BoneSpec("left_leg", px=1.9, py=12.0, pz=0.0, cubes=[
        CubeSpec("legs_cuisse_l", -2.0, 0.0, -2.0, 4, 8, 4, 0.36, painter=paint_cuisse),
        CubeSpec("legs_knee_l", -2.0, 5.0, -3.0, 4, 3, 2, 0.24, painter=paint_knee_cop),
        CubeSpec("feet_greave_l", -2.0, 8.0, -2.0, 4, 4, 4, 0.52, painter=paint_boot_greave),
        CubeSpec("feet_toe_l", -2.0, 10.0, -4.0, 4, 2, 2, 0.34, painter=paint_boot_toe),
    ])

    m.roots = [head, hat, body, right_arm, left_arm, right_leg, left_leg]
    m.pack_uvs()
    return m


# ==============================================================================
# 2. RIFTCRYSTAL STRONG ARMOR (Reference 02_riftcrystal_strong_armor.png)
# ==============================================================================
RC_BLACK = hex_rgba("#090E14")
RC_OBS_D = hex_rgba("#121A24")
RC_OBS = hex_rgba("#1D2735")
RC_SLATE = hex_rgba("#303B4C")
RC_SLATE_L = hex_rgba("#465368")
RC_CYAN_D = hex_rgba("#0E8785")
RC_CYAN = hex_rgba("#1FD1CA")
RC_CYAN_L = hex_rgba("#5CF7F0")
RC_CYAN_HI = hex_rgba("#B4FFFB")
RC_MAG_D = hex_rgba("#8E28BA")
RC_MAG = hex_rgba("#DA4CFA")
RC_MAG_HI = hex_rgba("#FA9EFF")


def build_riftcrystal_armor() -> AtlasModelBuilder:
    m = AtlasModelBuilder("riftcrystal_armor", 128, 128)

    def paint_rc_crown(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(RC_OBS, 71, 0.03)(face, w, h)
        if face == "front":  # 8x3
            # Cyan diamond crest + cyan side brow ridges
            put_px(arr, 0, 1, RC_CYAN_D)
            put_px(arr, 1, 0, RC_CYAN)
            put_px(arr, 6, 0, RC_CYAN)
            put_px(arr, 7, 1, RC_CYAN_D)
            fill_rect(arr, 3, 0, 2, 3, RC_CYAN_L)
            put_px(arr, 3, 0, RC_CYAN_HI)
            put_px(arr, 2, 1, RC_CYAN)
            put_px(arr, 5, 1, RC_CYAN)
        elif face in ("left", "right"):  # 8x3
            put_px(arr, 1, 0, RC_CYAN)
            put_px(arr, 2, 1, RC_MAG_HI)
            put_px(arr, 3, 2, RC_MAG)
            put_px(arr, 5, 0, RC_MAG)
            put_px(arr, 6, 1, RC_CYAN_D)
        elif face == "up":  # 8x8
            fill_rect(arr, 3, 0, 2, 7, RC_CYAN, 72, 0.02)
            fill_rect(arr, 3, 0, 2, 3, RC_CYAN_L)
            put_px(arr, 1, 3, RC_MAG)
            put_px(arr, 6, 3, RC_MAG)
        elif face == "back":
            fill_rect(arr, 3, 0, 2, 3, RC_CYAN_D)
            put_px(arr, 1, 1, RC_MAG)
            put_px(arr, 6, 1, RC_MAG)
        return arr

    def paint_rc_cheek(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(RC_OBS_D, 75, 0.03)(face, w, h)
        if face == "front":  # 2x5
            put_px(arr, 0, 0, RC_BLACK)
            put_px(arr, 1, 0, RC_BLACK)
            put_px(arr, 0, 2, RC_CYAN_D)
            put_px(arr, 0, 4, RC_SLATE)
        elif face in ("left", "right"):  # 4x5
            # Diagonal glowing violet rift vein down side of helm
            put_px(arr, 1, 0, RC_MAG_HI)
            put_px(arr, 2, 1, RC_MAG)
            put_px(arr, 2, 2, RC_MAG_HI)
            put_px(arr, 3, 3, RC_MAG_D)
        return arr

    def paint_rc_crest(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(RC_CYAN, 77, 0.01, 0.96)(face, w, h)
        if face == "front":
            fill_rect(arr, 0, 0, w, max(1, h - 1), RC_CYAN_L)
            put_px(arr, 0, 0, RC_CYAN_HI)
            fill_rect(arr, 0, h - 1, w, 1, RC_CYAN_D)
        elif face == "up":
            fill_rect(arr, 0, 0, w, h, RC_CYAN_HI)
        return arr

    head = BoneSpec("head", cubes=[
        CubeSpec("helm_visor_shadow", -4.0, -8.0, -4.0, 8, 8, 8, 0.14, painter=solid_face(hex_rgba("#06080E"), 72, 0.01)),
        CubeSpec("helm_crown", -4.0, -8.0, -4.0, 8, 3, 8, 0.60, painter=paint_rc_crown),
        CubeSpec("helm_back", -4.0, -5.0, 0.0, 8, 5, 4, 0.56, painter=solid_face(RC_OBS_D, 73, 0.03)),
        CubeSpec("helm_cheek_r", -4.0, -5.0, -4.0, 2, 5, 4, 0.56, painter=paint_rc_cheek),
        CubeSpec("helm_cheek_l", 2.0, -5.0, -4.0, 2, 5, 4, 0.56, painter=paint_rc_cheek),
        CubeSpec("helm_chin_r", -3.0, -2.0, -4.0, 1, 2, 2, 0.48, painter=solid_face(RC_OBS_D, 78)),
        CubeSpec("helm_chin_l", 2.0, -2.0, -4.0, 1, 2, 2, 0.48, painter=solid_face(RC_OBS_D, 79)),
        CubeSpec("helm_crest", -1.0, -8.0, -5.0, 2, 4, 5, 0.18, painter=paint_rc_crest),
    ])
    hat = BoneSpec("hat")

    def paint_rc_cuirass(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(RC_OBS, 81, 0.03)(face, w, h)
        if face == "front":  # 8x11
            # Glowing cyan V-heart chest core (y=0..4) + symmetric magenta rift veins
            put_px(arr, 1, 0, RC_CYAN)
            put_px(arr, 6, 0, RC_CYAN)
            fill_rect(arr, 2, 1, 4, 2, RC_CYAN_L)
            put_px(arr, 3, 1, RC_CYAN_HI)
            put_px(arr, 4, 1, RC_CYAN_HI)
            fill_rect(arr, 3, 3, 2, 2, RC_CYAN)
            # Magenta crystal veins framing the chest V
            for dx, dy in [(0, 1), (1, 2), (2, 3), (7, 1), (6, 2), (5, 3)]:
                put_px(arr, dx, dy, RC_MAG_HI if dy == 2 else RC_MAG)
            # Abdominal cyan chevrons + magenta rift sparks (y=5..8)
            fill_rect(arr, 3, 5, 2, 1, RC_CYAN_L)
            put_px(arr, 1, 6, RC_MAG)
            put_px(arr, 6, 6, RC_MAG)
            fill_rect(arr, 3, 7, 2, 1, RC_CYAN)
            put_px(arr, 2, 8, RC_MAG_HI)
            put_px(arr, 5, 8, RC_MAG_HI)
            # Slate waist guard + cyan hip crystals (y=9..10)
            fill_rect(arr, 2, 9, 4, 2, RC_SLATE)
            put_px(arr, 0, 9, RC_CYAN)
            put_px(arr, 7, 9, RC_CYAN)
        elif face == "back":  # 8x11
            fill_rect(arr, 3, 1, 2, 6, RC_CYAN_D)
            put_px(arr, 3, 2, RC_CYAN_L)
            put_px(arr, 4, 2, RC_CYAN_L)
            for y in (2, 4, 6):
                put_px(arr, 2, y, RC_MAG)
                put_px(arr, 5, y, RC_MAG)
        elif face in ("left", "right"):  # 4x11
            for y in range(2, 9, 2):
                put_px(arr, w // 2, y, RC_MAG)
            put_px(arr, w // 2, 9, RC_CYAN)
        return arr

    def paint_rc_core(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(RC_OBS_D, 83, 0.02)(face, w, h)
        if face == "front":  # 6x5
            fill_rect(arr, 1, 0, 4, 2, RC_CYAN_L)
            fill_rect(arr, 2, 0, 2, 1, RC_CYAN_HI)
            fill_rect(arr, 2, 2, 2, 2, RC_CYAN)
            put_px(arr, 0, 1, RC_MAG_HI)
            put_px(arr, 5, 1, RC_MAG_HI)
            put_px(arr, 1, 3, RC_MAG)
            put_px(arr, 4, 3, RC_MAG)
        return arr

    body = BoneSpec("body", cubes=[
        CubeSpec("chest_cuirass", -4.0, 0.0, -2.0, 8, 11, 4, 0.60, painter=paint_rc_cuirass),
        CubeSpec("chest_core", -3.0, 1.0, -3.0, 6, 5, 1, 0.22, painter=paint_rc_core),
        CubeSpec("legs_waist", -4.0, 9.0, -2.0, 8, 3, 4, 0.40, painter=solid_face(RC_OBS_D, 85, 0.03)),
    ])

    # --- FULL-ARM PAULDRONS + FOREARM VAMBRACES ---
    def paint_rc_pauldron(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(RC_OBS, 87, 0.03)(face, w, h)
        if face in ("front", "back", "left", "right") and h == 5:
            put_px(arr, 1, 0, RC_CYAN)
            put_px(arr, max(1, w - 2), 0, RC_CYAN)
            # Diagonal violet-magenta crystal vein + cyan spark
            put_px(arr, 1, 1, RC_MAG_HI)
            put_px(arr, 2, 2, RC_MAG)
            put_px(arr, min(w - 1, 3), 3, RC_CYAN)
            fill_rect(arr, 0, 4, w, 1, RC_BLACK)
        elif face == "up":
            fill_rect(arr, 1, 1, max(1, w - 2), 1, RC_CYAN)
            put_px(arr, w // 2, h // 2, RC_MAG)
        return arr

    def paint_rc_vambrace(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(RC_OBS_D, 89, 0.03)(face, w, h)
        if face in ("front", "left", "right") and h == 8:
            # Diagonal stepped violet-magenta rift vein + slate wrist cuff
            for step, (vx, vy) in enumerate([(1, 1), (2, 2), (2, 3), (1, 4), (2, 5)]):
                if vx < w:
                    put_px(arr, vx, vy, RC_MAG_HI if step == 1 else RC_MAG)
            put_px(arr, 0, 3, RC_CYAN_D)
            fill_rect(arr, 0, 6, w, 2, RC_SLATE)
            fill_rect(arr, 1, 7, max(1, w - 2), 1, RC_SLATE_L)
        return arr

    right_arm = BoneSpec("right_arm", px=-5.0, py=2.0, pz=0.0, cubes=[
        CubeSpec("chest_pauldron_r", -4.0, -3.0, -3.0, 5, 5, 6, 0.44, painter=paint_rc_pauldron),
        CubeSpec("chest_ridge_r", -4.0, -4.0, -2.0, 4, 2, 4, 0.24, painter=solid_face(RC_OBS, 90, 0.02)),
        CubeSpec("chest_vambrace_r", -3.0, 2.0, -2.0, 4, 8, 4, 0.40, painter=paint_rc_vambrace),
    ])
    left_arm = BoneSpec("left_arm", px=5.0, py=2.0, pz=0.0, cubes=[
        CubeSpec("chest_pauldron_l", -1.0, -3.0, -3.0, 5, 5, 6, 0.44, painter=paint_rc_pauldron),
        CubeSpec("chest_ridge_l", 0.0, -4.0, -2.0, 4, 2, 4, 0.24, painter=solid_face(RC_OBS, 91, 0.02)),
        CubeSpec("chest_vambrace_l", -1.0, 2.0, -2.0, 4, 8, 4, 0.40, painter=paint_rc_vambrace),
    ])

    def paint_rc_cuisse(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(RC_OBS_D, 93, 0.03)(face, w, h)
        if face == "front" and h == 8:
            put_px(arr, 0, 1, RC_MAG)
            put_px(arr, 3, 1, RC_MAG)
            put_px(arr, 1, 2, RC_CYAN_L)
            put_px(arr, 2, 3, RC_CYAN)
            put_px(arr, 1, 4, RC_MAG)
        elif face in ("left", "right") and h == 8:
            for vx, vy in [(1, 1), (2, 2), (2, 4), (1, 5)]:
                if vx < w:
                    put_px(arr, vx, vy, RC_MAG)
        return arr

    def paint_rc_knee(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(RC_OBS, 95, 0.02)(face, w, h)
        if face == "front":  # 4x3
            fill_rect(arr, 1, 0, 2, 2, RC_CYAN_L)
            put_px(arr, 1, 0, RC_CYAN_HI)
            fill_rect(arr, 0, 2, w, 1, RC_BLACK)
        return arr

    def paint_rc_greave(face: str, w: int, h: int) -> np.ndarray:
        arr = solid_face(RC_OBS_D, 97, 0.03)(face, w, h)
        if face == "front" and h == 4:
            fill_rect(arr, 1, 0, 2, 3, RC_SLATE_L)
            put_px(arr, 0, 2, RC_MAG)
            put_px(arr, 3, 2, RC_MAG)
        elif face in ("left", "right") and h == 4:
            put_px(arr, 1, 0, RC_MAG)
            put_px(arr, 2, 1, RC_MAG_HI)
            put_px(arr, 2, 2, RC_MAG)
        return arr

    right_leg = BoneSpec("right_leg", px=-1.9, py=12.0, pz=0.0, cubes=[
        CubeSpec("legs_cuisse_r", -2.0, 0.0, -2.0, 4, 8, 4, 0.40, painter=paint_rc_cuisse),
        CubeSpec("legs_knee_r", -2.0, 5.0, -3.0, 4, 3, 2, 0.26, painter=paint_rc_knee),
        CubeSpec("feet_greave_r", -2.0, 8.0, -2.0, 4, 4, 4, 0.54, painter=paint_rc_greave),
        CubeSpec("feet_toe_r", -2.0, 10.0, -3.0, 4, 2, 2, 0.36, painter=solid_face(RC_SLATE, 98, 0.02)),
    ])
    left_leg = BoneSpec("left_leg", px=1.9, py=12.0, pz=0.0, cubes=[
        CubeSpec("legs_cuisse_l", -2.0, 0.0, -2.0, 4, 8, 4, 0.40, painter=paint_rc_cuisse),
        CubeSpec("legs_knee_l", -2.0, 5.0, -3.0, 4, 3, 2, 0.26, painter=paint_rc_knee),
        CubeSpec("feet_greave_l", -2.0, 8.0, -2.0, 4, 4, 4, 0.54, painter=paint_rc_greave),
        CubeSpec("feet_toe_l", -2.0, 10.0, -3.0, 4, 2, 2, 0.36, painter=solid_face(RC_SLATE, 99, 0.02)),
    ])

    m.roots = [head, hat, body, right_arm, left_arm, right_leg, left_leg]
    m.pack_uvs()
    return m


def extract_riftcrystal_glow(atlas: np.ndarray) -> np.ndarray:
    """Keep only high-chroma cyan and magenta crystal pixels for additive RenderType.eyes (scaled so daylight doesn't clip white)."""
    glow = np.zeros_like(atlas)
    r = atlas[:, :, 0].astype(np.int32)
    g = atlas[:, :, 1].astype(np.int32)
    b = atlas[:, :, 2].astype(np.int32)
    a = atlas[:, :, 3]
    is_cyan = (a > 0) & (g > 125) & (b > 120) & (g > r + 40)
    is_mag = (a > 0) & (r > 125) & (b > 155) & (b > g + 55)
    mask = is_cyan | is_mag
    glow[mask, :3] = (atlas[mask, :3].astype(np.float32) * 0.36).astype(np.uint8)
    glow[mask, 3] = 230
    return glow


# ==============================================================================
# 3. THREE ELISAR SETTLEMENT FOLK (References 05, 06, 07)
# ==============================================================================
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


def emit_armor_layer_lines(model: AtlasModelBuilder, inner: bool, indent: str = "        ") -> List[str]:
    lines: List[str] = []
    for b in model.roots:
        filtered = [c for c in b.cubes if (c.name.startswith("legs_") if inner else not c.name.startswith("legs_"))]
        fb = BoneSpec(b.name, b.px, b.py, b.pz, b.rx, b.ry, b.rz, cubes=filtered)
        lines.extend(emit_bone_java(fb, "root", indent))
    return lines


def write_armor_models_java(aurite: AtlasModelBuilder, riftcrystal: AtlasModelBuilder) -> None:
    aur_inner = emit_armor_layer_lines(aurite, True)
    aur_outer = emit_armor_layer_lines(aurite, False)
    rc_inner = emit_armor_layer_lines(riftcrystal, True)
    rc_outer = emit_armor_layer_lines(riftcrystal, False)

    code = f"""package ru.aiforge.riftworld.client.model;

import net.minecraft.client.Minecraft;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.EntityRenderersEvent;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.item.RiftArmorMaterials;

/**
 * Generated by tools/rift/gen_v048_armors_and_folk.py — DO NOT HAND-EDIT UVS.
 * Custom 128x128 3D armor geometry for Aurite (open-arm medium armor, T-visor helm, 3D knee & toe plates)
 * and Riftcrystal (full-sleeve obsidian crystal plate with 3D cyan V-core & crystal crest).
 */
public final class ElisarArmorModels {{
    public static final ModelLayerLocation AURITE_INNER = new ModelLayerLocation(
            new ResourceLocation(RiftWorld.MODID, "aurite_armor"), "inner");
    public static final ModelLayerLocation AURITE_OUTER = new ModelLayerLocation(
            new ResourceLocation(RiftWorld.MODID, "aurite_armor"), "outer");
    public static final ModelLayerLocation RIFTCRYSTAL_INNER = new ModelLayerLocation(
            new ResourceLocation(RiftWorld.MODID, "riftcrystal_armor"), "inner");
    public static final ModelLayerLocation RIFTCRYSTAL_OUTER = new ModelLayerLocation(
            new ResourceLocation(RiftWorld.MODID, "riftcrystal_armor"), "outer");

    private static SlotArmorModel auriteHead, auriteChest, auriteLegs, auriteFeet;
    private static SlotArmorModel rcHead, rcChest, rcLegs, rcFeet;

    private ElisarArmorModels() {{
    }}

    public static void clearCache() {{
        auriteHead = auriteChest = auriteLegs = auriteFeet = null;
        rcHead = rcChest = rcLegs = rcFeet = null;
    }}

    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {{
        e.registerLayerDefinition(AURITE_INNER, ElisarArmorModels::createAuriteInnerLayer);
        e.registerLayerDefinition(AURITE_OUTER, ElisarArmorModels::createAuriteOuterLayer);
        e.registerLayerDefinition(RIFTCRYSTAL_INNER, ElisarArmorModels::createRiftcrystalInnerLayer);
        e.registerLayerDefinition(RIFTCRYSTAL_OUTER, ElisarArmorModels::createRiftcrystalOuterLayer);
    }}

    public static LayerDefinition createAuriteInnerLayer() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
{chr(10).join(aur_inner)}
        return LayerDefinition.create(mesh, 128, 128);
    }}

    public static LayerDefinition createAuriteOuterLayer() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
{chr(10).join(aur_outer)}
        return LayerDefinition.create(mesh, 128, 128);
    }}

    public static LayerDefinition createRiftcrystalInnerLayer() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
{chr(10).join(rc_inner)}
        return LayerDefinition.create(mesh, 128, 128);
    }}

    public static LayerDefinition createRiftcrystalOuterLayer() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
{chr(10).join(rc_outer)}
        return LayerDefinition.create(mesh, 128, 128);
    }}

    private static void ensureBaked() {{
        if (auriteHead != null && rcHead != null) return;
        var models = Minecraft.getInstance().getEntityModels();
        auriteHead = new SlotArmorModel(models.bakeLayer(AURITE_OUTER), EquipmentSlot.HEAD);
        auriteChest = new SlotArmorModel(models.bakeLayer(AURITE_OUTER), EquipmentSlot.CHEST);
        auriteLegs = new SlotArmorModel(models.bakeLayer(AURITE_INNER), EquipmentSlot.LEGS);
        auriteFeet = new SlotArmorModel(models.bakeLayer(AURITE_OUTER), EquipmentSlot.FEET);

        rcHead = new SlotArmorModel(models.bakeLayer(RIFTCRYSTAL_OUTER), EquipmentSlot.HEAD);
        rcChest = new SlotArmorModel(models.bakeLayer(RIFTCRYSTAL_OUTER), EquipmentSlot.CHEST);
        rcLegs = new SlotArmorModel(models.bakeLayer(RIFTCRYSTAL_INNER), EquipmentSlot.LEGS);
        rcFeet = new SlotArmorModel(models.bakeLayer(RIFTCRYSTAL_OUTER), EquipmentSlot.FEET);
    }}

    @SuppressWarnings("unchecked")
    public static HumanoidModel<LivingEntity> getForSlot(
            RiftArmorMaterials mat,
            EquipmentSlot slot,
            HumanoidModel<?> original) {{
        ensureBaked();
        SlotArmorModel target;
        if (mat == RiftArmorMaterials.AURITE) {{
            target = switch (slot) {{
                case HEAD -> auriteHead;
                case CHEST -> auriteChest;
                case LEGS -> auriteLegs;
                default -> auriteFeet;
            }};
        }} else {{
            target = switch (slot) {{
                case HEAD -> rcHead;
                case CHEST -> rcChest;
                case LEGS -> rcLegs;
                default -> rcFeet;
            }};
        }}
        ((HumanoidModel<LivingEntity>) original).copyPropertiesTo(target);
        target.applySlotVisibility();
        return target;
    }}

    public static final class SlotArmorModel extends HumanoidModel<LivingEntity> {{
        private final EquipmentSlot slot;

        public SlotArmorModel(ModelPart root, EquipmentSlot slot) {{
            super(root);
            this.slot = slot;
        }}

        public void applySlotVisibility() {{
            this.setAllVisible(false);
            switch (this.slot) {{
                case HEAD -> {{
                    this.head.visible = true;
                    this.hat.visible = false;
                }}
                case CHEST -> {{
                    this.body.visible = true;
                    this.rightArm.visible = true;
                    this.leftArm.visible = true;
                }}
                case LEGS -> {{
                    this.body.visible = true;
                    this.rightLeg.visible = true;
                    this.leftLeg.visible = true;
                }}
                case FEET -> {{
                    this.rightLeg.visible = true;
                    this.leftLeg.visible = true;
                }}
                default -> {{}}
            }}
        }}
    }}
}}
"""
    JAVA_MODEL_DIR.mkdir(parents=True, exist_ok=True)
    (JAVA_MODEL_DIR / "ElisarArmorModels.java").write_text(code, encoding="utf-8")


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


def generate_armor_and_material_icons() -> None:
    item_tex = ASSETS / "textures/item"
    block_tex = ASSETS / "textures/block"
    item_tex.mkdir(parents=True, exist_ok=True)
    block_tex.mkdir(parents=True, exist_ok=True)

    aur_pal = {
        "k": AUR_DARK,
        "d": AUR_PLATE_D,
        "p": AUR_PLATE,
        "l": AUR_PLATE_L,
        "b": AUR_LEATHER,
        "g": AUR_GOLD,
        "G": AUR_GOLD_L,
        "H": AUR_GOLD_HI,
        "t": AUR_TEAL,
        "T": AUR_TEAL_D,
    }
    rc_pal = {
        "k": RC_BLACK,
        "d": RC_OBS_D,
        "p": RC_OBS,
        "s": RC_SLATE,
        "S": RC_SLATE_L,
        "c": RC_CYAN,
        "C": RC_CYAN_L,
        "H": RC_CYAN_HI,
        "m": RC_MAG,
        "M": RC_MAG_HI,
    }

    # Aurite armor 16x16 -> 32x32 icons matching 01_aurite_medium_armor.png
    aur_helm = [
        "................",
        "....kkkkkkkk....",
        "...kggGHHGggk...",
        "..kptggGHggtpk..",
        "..kppdGGGGdppk..",
        "..kpldkHHkdlpk..",
        "..kpl..GH..lpk..",
        "..kpd..gg..dpk..",
        "..kpgd....dgpk..",
        "..kppk....kppk..",
        "...kdk....kdk...",
        "....kk....kk....",
        "................",
        "................",
        "................",
        "................",
    ]
    aur_chest = [
        "................",
        ".kkkk......kkkk.",
        "kplppk....kpplpk",
        "kppgppkkkkppgppk",
        "kGGGGGgtTGgGGGGk",
        "kTTTTkdkHHkdkTTk",
        ".kkkk.kpGGpk.kkk",
        ".....kplpplpk...",
        ".....kppddppk...",
        ".....kkgGHgkk...",
        ".....kpddddpk...",
        ".....kTTTTTTk...",
        "......kkkkkk....",
        "................",
        "................",
        "................",
    ]
    aur_legs = [
        "................",
        "...kkkkkkkkkk...",
        "..kbbbgGGgbbbk..",
        "..kbbppddppbbk..",
        "..kbbpk..kpbbk..",
        "..ktttk..ktttk..",
        "..kTTTk..kTTTk..",
        "..kgGHk..kHGgk..",
        "..kgGGk..kGGgk..",
        "..kppdk..kdppk..",
        "..kdddk..kdddk..",
        "...kkkk..kkkk...",
        "................",
        "................",
        "................",
        "................",
    ]
    aur_boots = [
        "................",
        "................",
        "................",
        "................",
        "...kkkk..kkkk...",
        "...kbbk..kbbk...",
        "...kpgk..kgpk...",
        "...kpdk..kdpk...",
        "..kkpdk..kdpkk..",
        ".kgGHpk..kpHGgk.",
        ".kGGGGk..kGGGGk.",
        ".kkkkkk..kkkkkk.",
        "................",
        "................",
        "................",
        "................",
    ]

    # Riftcrystal armor 16x16 -> 32x32 icons matching 02_riftcrystal_strong_armor.png
    rc_helm = [
        "................",
        "....kkkkkkkk....",
        "...kccdCHdcck...",
        "..kcmppCHppmck..",
        "..kmppdCCdppmk..",
        "..kpsdkHHkdspk..",
        "..kpd..CH..dpk..",
        "..kmd..cc..dmk..",
        "..kpsd....dspk..",
        "..kddk....kddk..",
        "...kkk....kkk...",
        "................",
        "................",
        "................",
        "................",
        "................",
    ]
    rc_chest = [
        "................",
        ".kccck....kccck.",
        "kcCpppk..kpppcCk",
        "kpmMppkkkkppMmpk",
        "kppmppCCHCppmppk",
        "kdppdkmcCCmkdppk",
        "kpMdk.kpCCpk.kMk",
        "kpmdk.kmccmk.kmk",
        "kssdk.kpcCpk.ksk",
        ".kkk..kmpcmk.kkk",
        "......kcssck....",
        "......kkkkkk....",
        "................",
        "................",
        "................",
        "................",
    ]
    rc_legs = [
        "................",
        "...kkkkkkkkkk...",
        "..kdmpcsspcmdk..",
        "..kpdppkkppdpk..",
        "..kmpck..kcpmk..",
        "..kpCCk..kCCpk..",
        "..kmdpk..kpdmk..",
        "..kcCHk..kHCck..",
        "..kccck..kccck..",
        "..kpddk..kddpk..",
        "..kssdk..kdssk..",
        "...kkkk..kkkk...",
        "................",
        "................",
        "................",
        "................",
    ]
    rc_boots = [
        "................",
        "................",
        "................",
        "................",
        "...kkkk..kkkk...",
        "...kdmk..kmdk...",
        "...ksSk..kSsk...",
        "...kpMk..kMpk...",
        "..kkssK..Ksskk..".replace("K", "k"),
        ".kdSSpk..kpSSdk.",
        ".kddddk..kddddk.",
        ".kkkkkk..kkkkkk.",
        "................",
        "................",
        "................",
        "................",
    ]

    aur_ingot = [
        "................",
        "................",
        "................",
        "......kkkkkk....",
        "....kkGGGGHHkk..",
        "...kGGHHHHHHGGk.",
        "..kGHHgtGGtgHHgk",
        ".kGGHgGGGGGGgddk",
        ".kggggggggggddk.",
        "..kddddddddddk..",
        "...kkkkkkkkkk...",
        "................",
        "................",
        "................",
        "................",
        "................",
    ]
    rc_gem = [
        "................",
        ".......kk.......",
        "......kCHk......",
        ".....kcCHck.....",
        "....kMcCHcMk....",
        "...kmpcHHcpmk...",
        "..kpdccHHccdpk..",
        "..kdcCCHHCCcdk..",
        "..kpdccccccdpk..",
        "...kmpccccpmk...",
        "....kMccccMk....",
        ".....kpccpk.....",
        "......kddk......",
        ".......kk.......",
        "................",
        "................",
    ]

    icons = {
        "aurite_helmet": (aur_helm, aur_pal),
        "aurite_chestplate": (aur_chest, aur_pal),
        "aurite_leggings": (aur_legs, aur_pal),
        "aurite_boots": (aur_boots, aur_pal),
        "riftcrystal_helmet": (rc_helm, rc_pal),
        "riftcrystal_chestplate": (rc_chest, rc_pal),
        "riftcrystal_leggings": (rc_legs, rc_pal),
        "riftcrystal_boots": (rc_boots, rc_pal),
        "aurite_ingot": (aur_ingot, aur_pal),
        "riftcrystal": (rc_gem, rc_pal),
    }
    for name, (grid, pal) in icons.items():
        arr32 = upscale_2x_bevel(draw_pattern(grid, pal), RC_BLACK)
        Image.fromarray(arr32, "RGBA").save(item_tex / f"{name}.png")

    # 16x16 Riftcrystal Ore block texture: dark rift-slate matrix studded with glowing cyan & magenta crystal clusters
    ore = np.zeros((16, 16, 4), dtype=np.uint8)
    for y in range(16):
        for x in range(16):
            n = det_noise(x, y, 505)
            base = RC_SLATE if n > 0.25 else (RC_OBS if n > -0.35 else RC_OBS_D)
            ore[y, x] = brighten(base, 1.0 + n * 0.08)
    # Faceted cyan crystal pockets + magenta rift veins
    cyan_clusters = [
        (3, 3, RC_CYAN_HI), (4, 3, RC_CYAN_L), (3, 4, RC_CYAN_L), (4, 4, RC_CYAN), (5, 4, RC_CYAN_D),
        (10, 5, RC_CYAN_HI), (11, 5, RC_CYAN_L), (10, 6, RC_CYAN), (11, 6, RC_CYAN_D),
        (5, 11, RC_CYAN_HI), (6, 11, RC_CYAN_L), (6, 12, RC_CYAN), (7, 12, RC_CYAN_D),
        (12, 11, RC_CYAN_L), (13, 11, RC_CYAN),
    ]
    mag_veins = [
        (2, 5, RC_MAG_HI), (5, 3, RC_MAG), (9, 4, RC_MAG), (12, 6, RC_MAG_HI),
        (4, 10, RC_MAG), (8, 11, RC_MAG_HI), (11, 12, RC_MAG), (7, 7, RC_MAG_D), (8, 8, RC_MAG),
    ]
    for x, y, c in cyan_clusters + mag_veins:
        put_px(ore, x, y, c)
    Image.fromarray(ore, "RGBA").save(block_tex / "riftcrystal_ore.png")


def build_fallback_vanilla_armor_layers() -> None:
    """Keep valid 64x32 layer_1 / layer_2 PNGs for any third-party mod inspecting standard paths."""
    armor_dir = ASSETS / "textures/models/armor"
    armor_dir.mkdir(parents=True, exist_ok=True)
    for prefix, c_main, c_trim in [
        ("aurite", AUR_PLATE, AUR_GOLD_L),
        ("riftcrystal", RC_OBS, RC_CYAN_L),
    ]:
        for layer in (1, 2):
            img = np.zeros((32, 64, 4), dtype=np.uint8)
            fill_rect(img, 0, 0, 64, 32, c_main)
            fill_rect(img, 0, 15, 64, 2, c_trim)
            Image.fromarray(img, "RGBA").save(armor_dir / f"{prefix}_layer_{layer}.png")


def main() -> None:
    armor_dir = ASSETS / "textures/models/armor"
    entity_dir = ASSETS / "textures/entity"
    armor_dir.mkdir(parents=True, exist_ok=True)
    entity_dir.mkdir(parents=True, exist_ok=True)

    aurite = build_aurite_armor()
    riftcrystal = build_riftcrystal_armor()
    aur_png = aurite.paint_atlas()
    rc_png = riftcrystal.paint_atlas()
    rc_glow = extract_riftcrystal_glow(rc_png)

    Image.fromarray(aur_png, "RGBA").save(armor_dir / "aurite_armor.png")
    Image.fromarray(rc_png, "RGBA").save(armor_dir / "riftcrystal_armor.png")
    Image.fromarray(rc_glow, "RGBA").save(armor_dir / "riftcrystal_armor_glow.png")
    build_fallback_vanilla_armor_layers()
    write_armor_models_java(aurite, riftcrystal)

    vil = build_elisar_villager()
    grd = build_elisar_guard()
    arc = build_elisar_archer()
    Image.fromarray(vil.paint_atlas(), "RGBA").save(entity_dir / "elisar_villager.png")
    Image.fromarray(grd.paint_atlas(), "RGBA").save(entity_dir / "elisar_guard.png")
    Image.fromarray(arc.paint_atlas(), "RGBA").save(entity_dir / "elisar_archer.png")
    write_folk_models_java(vil, grd, arc)

    generate_armor_and_material_icons()
    print(
        f"OK v0.48.0 armors & folk generated: "
        f"aurite_cubes={len(aurite.all_cubes())} riftcrystal_cubes={len(riftcrystal.all_cubes())} "
        f"villager={len(vil.all_cubes())} guard={len(grd.all_cubes())} archer={len(arc.all_cubes())}"
    )


if __name__ == "__main__":
    main()
