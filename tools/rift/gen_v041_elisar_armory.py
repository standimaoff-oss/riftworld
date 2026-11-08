#!/usr/bin/env python3
"""v0.41.0: Aurite/Riftcrystal equipment and three Elisar settlement residents.

Deterministic, dependency-free pixel art + JSON resources. Run from any directory:
    python3 tools/rift/gen_v041_elisar_armory.py
"""
from __future__ import annotations
import json
import math
import os
import random
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "mod/src/main/resources"
ASSETS = RES / "assets/riftworld"
DATA = RES / "data"
ITEM_TEX = ASSETS / "textures/item"
BLOCK_TEX = ASSETS / "textures/block"
ENTITY_TEX = ASSETS / "textures/entity"
ARMOR_TEX = ASSETS / "textures/models/armor"
REPORT = ROOT / "reports/elisar_armory"

TRANSPARENT = (0, 0, 0, 0)
AURITE = {
    "shadow": (83, 53, 22, 255), "dark": (139, 91, 29, 255),
    "base": (218, 157, 47, 255), "light": (255, 215, 105, 255),
    "glint": (255, 241, 170, 255), "accent": (65, 151, 139, 255),
    "accent_hi": (115, 207, 185, 255), "wood": (66, 45, 28, 255),
}
RIFT = {
    "shadow": (17, 20, 29, 255), "dark": (35, 43, 56, 255),
    "base": (45, 184, 183, 255), "light": (93, 239, 222, 255),
    "glint": (192, 255, 241, 255), "accent": (155, 76, 205, 255),
    "accent_hi": (218, 131, 255, 255), "wood": (31, 29, 39, 255),
}


def write_json(path: Path, value) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def merge_json(path: Path, value) -> None:
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    if isinstance(value, dict) and "values" in value:
        old.setdefault("replace", False)
        old["values"] = list(dict.fromkeys(old.get("values", []) + value["values"]))
    else:
        old.update(value)
    write_json(path, old)


class Canvas:
    def __init__(self, w: int, h: int, color=TRANSPARENT):
        self.w, self.h = w, h
        self.p = [list(color) for _ in range(w * h)]

    def set(self, x: int, y: int, c) -> None:
        if 0 <= x < self.w and 0 <= y < self.h:
            self.p[y * self.w + x] = list(c)

    def get(self, x: int, y: int):
        return self.p[y * self.w + x] if 0 <= x < self.w and 0 <= y < self.h else list(TRANSPARENT)

    def rect(self, x0: int, y0: int, x1: int, y1: int, c) -> None:
        for y in range(max(0, y0), min(self.h, y1 + 1)):
            for x in range(max(0, x0), min(self.w, x1 + 1)):
                self.set(x, y, c)

    def line(self, x0: int, y0: int, x1: int, y1: int, c, width: int = 1) -> None:
        dx, sx = abs(x1 - x0), 1 if x0 < x1 else -1
        dy, sy = -abs(y1 - y0), 1 if y0 < y1 else -1
        err = dx + dy
        while True:
            r = width // 2
            self.rect(x0 - r, y0 - r, x0 + r, y0 + r, c)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 >= dy:
                err += dy
                x0 += sx
            if e2 <= dx:
                err += dx
                y0 += sy

    def poly(self, points, c) -> None:
        minx = max(0, math.floor(min(x for x, _ in points)))
        maxx = min(self.w - 1, math.ceil(max(x for x, _ in points)))
        miny = max(0, math.floor(min(y for _, y in points)))
        maxy = min(self.h - 1, math.ceil(max(y for _, y in points)))
        for y in range(miny, maxy + 1):
            for x in range(minx, maxx + 1):
                inside = False
                j = len(points) - 1
                for i in range(len(points)):
                    xi, yi = points[i]
                    xj, yj = points[j]
                    if ((yi > y) != (yj > y)) and x < (xj - xi) * (y - yi) / ((yj - yi) or 1e-9) + xi:
                        inside = not inside
                    j = i
                if inside:
                    self.set(x, y, c)

    def outline(self, points, c, width: int = 1) -> None:
        for i, a in enumerate(points):
            self.line(*a, *points[(i + 1) % len(points)], c, width)

    def noise(self, seed: int, amount: int = 12, opaque_only: bool = True) -> None:
        rng = random.Random(seed)
        for p in self.p:
            if opaque_only and p[3] == 0:
                continue
            d = rng.randint(-amount, amount)
            p[0] = max(0, min(255, p[0] + d))
            p[1] = max(0, min(255, p[1] + d))
            p[2] = max(0, min(255, p[2] + d))

    def save(self, path: Path) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        raw = bytearray()
        for y in range(self.h):
            raw.append(0)
            for x in range(self.w):
                raw.extend(self.p[y * self.w + x])

        def chunk(tag: bytes, data: bytes) -> bytes:
            return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

        png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", self.w, self.h, 8, 6, 0, 0, 0))
        png += chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b"")
        path.write_bytes(png)


def gem(im: Canvas, x: int, y: int, p: dict) -> None:
    im.poly([(x, y - 2), (x + 2, y), (x, y + 2), (x - 2, y)], p["accent"])
    im.line(x, y - 1, x + 1, y, p["accent_hi"])
    im.set(x, y, p["glint"])


def make_sword(p: dict) -> Canvas:
    im = Canvas(32, 32)
    blade = [(25, 1), (30, 1), (30, 7), (13, 23), (9, 19)]
    im.poly(blade, p["shadow"])
    im.poly([(25, 3), (28, 2), (28, 7), (13, 21), (11, 19)], p["base"])
    im.poly([(26, 3), (28, 3), (27, 7), (14, 19), (17, 14)], p["light"])
    im.line(27, 3, 14, 17, p["glint"])
    im.line(10, 19, 14, 23, p["dark"], 3)
    im.line(6, 19, 13, 26, p["shadow"], 3)
    im.line(6, 18, 14, 25, p["base"], 1)
    gem(im, 11, 22, p)
    im.line(9, 24, 4, 29, p["wood"], 3)
    im.line(8, 24, 4, 28, p["dark"], 1)
    im.rect(2, 28, 5, 30, p["shadow"])
    im.rect(3, 28, 5, 29, p["accent"])
    return im


def make_pickaxe(p: dict) -> Canvas:
    im = Canvas(32, 32)
    im.line(8, 27, 21, 8, p["shadow"], 4)
    im.line(8, 26, 20, 8, p["wood"], 2)
    for x, y in [(10, 23), (13, 19), (16, 15)]:
        im.line(x, y, x + 2, y + 2, p["base"])
    head = [(2, 10), (4, 6), (10, 2), (18, 1), (25, 4), (30, 10), (28, 13), (23, 8), (17, 5), (11, 5), (6, 8), (4, 13)]
    im.poly(head, p["shadow"])
    inner = [(4, 9), (7, 5), (12, 3), (18, 3), (24, 6), (28, 10), (27, 11), (22, 7), (17, 4), (11, 4), (7, 7), (5, 11)]
    im.poly(inner, p["base"])
    im.line(7, 5, 17, 2, p["light"], 2)
    im.line(18, 3, 25, 6, p["glint"])
    im.line(5, 9, 4, 11, p["dark"], 2)
    im.line(26, 8, 28, 11, p["dark"], 2)
    gem(im, 19, 7, p)
    gem(im, 8, 25, p)
    return im


def make_axe(p: dict) -> Canvas:
    im = Canvas(32, 32)
    im.line(8, 28, 19, 7, p["shadow"], 4)
    im.line(8, 27, 19, 7, p["wood"], 2)
    for x, y in [(9, 24), (12, 20), (15, 16), (17, 12)]:
        im.line(x, y, x + 2, y + 2, p["base"])
    left = [(17, 3), (10, 1), (4, 3), (1, 8), (2, 15), (7, 18), (14, 13), (16, 8)]
    right = [(20, 3), (27, 2), (31, 6), (30, 14), (25, 18), (19, 13), (18, 8)]
    im.poly(left, p["shadow"])
    im.poly(right, p["shadow"])
    im.poly([(15, 5), (10, 3), (6, 4), (3, 8), (4, 14), (7, 15), (12, 12), (14, 8)], p["base"])
    im.poly([(22, 5), (27, 4), (29, 7), (28, 13), (25, 15), (21, 12), (20, 8)], p["base"])
    im.line(5, 5, 3, 9, p["light"], 2)
    im.line(4, 12, 7, 14, p["glint"])
    im.line(27, 5, 29, 8, p["light"], 2)
    im.line(28, 11, 25, 14, p["glint"])
    im.rect(15, 4, 21, 14, p["dark"])
    im.rect(17, 5, 20, 13, p["base"])
    gem(im, 18, 9, p)
    gem(im, 8, 26, p)
    return im


def make_bow(p: dict, stage: int = -1) -> Canvas:
    im = Canvas(32, 32)
    # The pull variants really change both string geometry and the arrow position.
    pull = 0 if stage < 0 else [3, 7, 11][stage]
    pts = [(22, 1), (26, 4), (28, 9), (28, 15), (26, 22), (22, 28), (18, 30)]
    for a, b in zip(pts, pts[1:]):
        im.line(*a, *b, p["shadow"], 4)
    inner = [(22, 2), (25, 5), (26, 10), (26, 15), (24, 21), (21, 27), (18, 29)]
    for a, b in zip(inner, inner[1:]):
        im.line(*a, *b, p["base"], 2)
    im.line(23, 2, 25, 5, p["light"])
    im.line(25, 20, 22, 26, p["light"])
    im.line(22, 2, 12 - pull, 15, p["glint"])
    im.line(12 - pull, 15, 18, 29, p["glint"])
    im.rect(24, 12, 28, 18, p["dark"])
    im.rect(25, 13, 27, 17, p["accent"])
    if stage >= 0:
        im.line(3, 15, 25, 15, p["wood"])
        im.poly([(2, 15), (6, 12), (6, 18)], p["light"])
        im.line(23, 13, 27, 15, p["accent"])
        im.line(23, 17, 27, 15, p["accent_hi"])
    return im


def make_shield(p: dict) -> Canvas:
    im = Canvas(32, 32)
    shape = [(4, 3), (27, 3), (29, 6), (28, 21), (24, 26), (16, 31), (7, 26), (3, 20), (2, 6)]
    im.poly(shape, p["shadow"])
    inner = [(5, 5), (26, 5), (27, 7), (26, 20), (22, 24), (16, 28), (9, 24), (5, 19), (4, 7)]
    im.poly(inner, p["dark"])
    im.line(6, 5, 25, 5, p["light"], 2)
    im.line(5, 7, 5, 18, p["base"], 2)
    im.line(27, 8, 26, 20, p["base"], 2)
    im.line(8, 24, 16, 28, p["base"], 2)
    im.line(24, 23, 16, 28, p["dark"], 2)
    # Heraldic central crest + four embedded crystals.
    im.poly([(16, 10), (21, 13), (20, 19), (16, 23), (12, 19), (11, 13)], p["base"])
    im.poly([(16, 12), (19, 14), (18, 18), (16, 20), (14, 18), (13, 14)], p["shadow"])
    gem(im, 16, 15, p)
    for x, y in [(8, 8), (23, 8), (8, 19), (23, 19)]:
        gem(im, x, y, p)
    return im


def make_ingot(p: dict, crystal: bool = False) -> Canvas:
    im = Canvas(32, 32)
    if crystal:
        im.poly([(7, 26), (4, 16), (9, 4), (16, 1), (22, 7), (25, 22), (18, 29), (11, 28)], p["shadow"])
        im.poly([(9, 24), (7, 16), (11, 6), (16, 3), (19, 8), (18, 25), (13, 27)], p["base"])
        im.poly([(16, 3), (22, 8), (23, 21), (18, 26), (19, 8)], p["accent"])
        im.line(11, 7, 9, 16, p["glint"], 2)
        im.line(16, 4, 18, 9, p["light"])
        im.line(12, 24, 17, 26, p["light"])
    else:
        im.poly([(3, 20), (9, 9), (24, 9), (30, 20), (25, 27), (8, 27)], p["shadow"])
        im.poly([(9, 10), (23, 10), (27, 18), (7, 18)], p["light"])
        im.poly([(7, 19), (27, 19), (23, 25), (9, 25)], p["base"])
        im.line(10, 11, 21, 11, p["glint"], 2)
        im.line(9, 22, 22, 22, p["dark"], 2)
        gem(im, 17, 17, p)
    return im


def make_armor_icon(part: str, p: dict) -> Canvas:
    im = Canvas(32, 32)
    if part == "helmet":
        im.poly([(7, 25), (5, 11), (9, 4), (16, 2), (24, 5), (27, 12), (25, 25), (20, 27), (19, 17), (12, 17), (11, 27)], p["shadow"])
        im.poly([(8, 22), (7, 11), (11, 6), (16, 4), (22, 7), (25, 12), (23, 22), (21, 23), (21, 14), (10, 14), (10, 23)], p["base"])
        im.line(11, 6, 17, 4, p["light"], 2)
        im.rect(9, 10, 23, 13, p["dark"])
        im.line(11, 10, 21, 10, p["light"])
        gem(im, 16, 8, p)
    elif part == "chestplate":
        shape = [(10, 3), (5, 5), (1, 13), (6, 17), (9, 12), (9, 28), (23, 28), (23, 12), (26, 17), (31, 13), (27, 5), (22, 3), (20, 8), (12, 8)]
        im.poly(shape, p["shadow"])
        im.poly([(11, 5), (7, 6), (4, 12), (6, 14), (8, 9), (11, 7), (11, 25), (21, 25), (21, 7), (24, 9), (26, 14), (28, 12), (25, 6), (21, 5), (19, 10), (13, 10)], p["base"])
        im.line(11, 18, 21, 18, p["dark"], 2)
        im.line(11, 24, 21, 24, p["accent"], 2)
        im.poly([(16, 10), (21, 14), (16, 20), (11, 14)], p["dark"])
        gem(im, 16, 14, p)
        im.line(7, 6, 4, 11, p["light"], 2)
        im.line(25, 6, 28, 11, p["light"], 2)
    elif part == "leggings":
        im.poly([(6, 3), (26, 3), (25, 16), (23, 29), (16, 29), (15, 17), (13, 29), (6, 29), (5, 15)], p["shadow"])
        im.poly([(8, 5), (24, 5), (23, 15), (21, 26), (18, 26), (16, 12), (14, 26), (8, 26), (7, 14)], p["base"])
        im.rect(7, 5, 24, 9, p["accent"])
        im.line(8, 11, 23, 11, p["light"])
        im.line(9, 23, 13, 23, p["dark"], 2)
        im.line(18, 23, 22, 23, p["dark"], 2)
        gem(im, 16, 7, p)
    else:
        im.poly([(5, 4), (13, 4), (14, 18), (18, 21), (18, 28), (2, 28), (2, 20), (5, 16)], p["shadow"])
        im.poly([(19, 4), (27, 4), (27, 16), (30, 20), (30, 28), (14, 28), (14, 21), (18, 18)], p["shadow"])
        im.poly([(7, 6), (11, 6), (12, 19), (16, 22), (16, 25), (4, 25), (4, 21), (7, 17)], p["base"])
        im.poly([(21, 6), (25, 6), (25, 17), (28, 21), (28, 25), (16, 25), (16, 22), (20, 19)], p["base"])
        im.line(4, 24, 16, 24, p["accent"], 2)
        im.line(16, 24, 28, 24, p["accent"], 2)
        im.line(7, 7, 11, 7, p["light"], 2)
        im.line(21, 7, 25, 7, p["light"], 2)
    return im


def make_spawn_egg(back: str, spots: str) -> Canvas:
    im = Canvas(32, 32)
    def rgb(h):
        h = h.lstrip("#")
        return tuple(int(h[i:i+2], 16) for i in (0, 2, 4)) + (255,)
    base, spot = rgb(back), rgb(spots)
    im.poly([(16, 2), (23, 4), (28, 11), (28, 21), (23, 28), (16, 30), (8, 28), (3, 21), (3, 11), (8, 4)], base)
    im.poly([(10, 5), (18, 3), (23, 5), (17, 8), (9, 14), (5, 18), (5, 11)], tuple(min(255, c + 25) for c in base[:3]) + (255,))
    for x, y, r in [(10, 10, 3), (22, 9, 2), (18, 18, 3), (8, 22, 2), (24, 23, 3), (14, 27, 2)]:
        im.rect(x-r//2, y-r//2, x+r//2, y+r//2, spot)
    return im


# ---- worn armour: vanilla UV in 0..63 + custom geometric plates in a 128x128 atlas ----
def box_faces(im: Canvas, x: int, y: int, w: int, h: int, d: int, p: dict, base="base") -> None:
    shades = [("dark", (x, y + d, d, h)), (base, (x + d, y + d, w, h)),
              ("shadow", (x + d + w, y + d, d, h)), ("dark", (x + d + w + d, y + d, w, h)),
              ("light", (x + d, y, w, d)), ("shadow", (x + d + w, y, w, d))]
    for key, (rx, ry, rw, rh) in shades:
        im.rect(rx, ry, rx + rw - 1, ry + rh - 1, p[key])


def armor_detail_box(im: Canvas, x: int, y: int, w: int, h: int, d: int, p: dict) -> None:
    """Shaded cuboid UV with an inset face, metal edging, rivets and a small central gem."""
    box_faces(im, x, y, w, h, d, p)
    fx, fy = x + d, y + d
    if w >= 3 and h >= 3:
        im.line(fx, fy, fx + w - 1, fy, p["light"])
        im.line(fx, fy, fx, fy + h - 1, p["light"])
        im.line(fx, fy + h - 1, fx + w - 1, fy + h - 1, p["shadow"])
        im.line(fx + w - 1, fy, fx + w - 1, fy + h - 1, p["shadow"])
        if w >= 5 and h >= 4:
            im.rect(fx + 1, fy + 2, fx + w - 2, fy + h - 2, p["dark"])
            im.line(fx + 1, fy + 1, fx + w - 2, fy + 1, p["base"])
            gem(im, fx + w // 2, fy + h // 2, p)
        for rx, ry in ((fx + 1, fy + 1), (fx + w - 2, fy + 1),
                       (fx + 1, fy + h - 2), (fx + w - 2, fy + h - 2)):
            im.set(rx, ry, p["glint"])


def make_armor_layer(material: str, layer: int) -> Canvas:
    p = AURITE if material == "aurite" else RIFT
    # The reference armour has dark under-mail with bright plates, not a solid cyan/gold body.
    under = dict(p)
    if material == "riftcrystal":
        under.update(base=(39, 49, 62, 255), dark=(25, 31, 43, 255), light=p["base"])
    else:
        under.update(base=(105, 76, 47, 255), dark=(58, 46, 38, 255), light=p["base"])
    plate = p if material == "aurite" else under
    im = Canvas(128, 128)
    if layer == 1:
        box_faces(im, 0, 0, 8, 8, 8, under)       # helmet
        box_faces(im, 32, 0, 8, 8, 8, under)      # helmet overlay
        box_faces(im, 16, 16, 8, 12, 4, under)    # chest
        box_faces(im, 40, 16, 4, 12, 4, under)    # right arm
        box_faces(im, 32, 48, 4, 12, 4, under)    # left arm
        box_faces(im, 0, 16, 4, 12, 4, under)     # right boot
        box_faces(im, 16, 48, 4, 12, 4, under)    # left boot
        im.rect(8, 11, 15, 12, p["dark"])
        im.line(9, 9, 14, 9, p["light"])
        gem(im, 12, 9, p)
        im.rect(20, 20, 27, 21, p["accent"])
        im.line(20, 29, 27, 29, p["accent"], 2)
        gem(im, 24, 25, p)
        for x in (44, 36):
            im.rect(x, 20, x + 3, 23, p["light"])
            im.line(x, 27, x + 3, 27, p["accent"])
        for x in (4, 20):
            im.rect(x, 26, x + 3, 31, p["dark"])
            im.line(x, 29, x + 3, 29, p["accent"])
    else:
        box_faces(im, 16, 16, 8, 12, 4, under)    # waist
        box_faces(im, 0, 16, 4, 12, 4, under)     # right legging
        box_faces(im, 16, 48, 4, 12, 4, under)    # left legging
        im.rect(20, 20, 27, 23, p["accent"])
        gem(im, 24, 22, p)
        for x in (4, 20):
            im.line(x, 20, x + 3, 20, p["light"])
            im.rect(x, 25, x + 3, 26, p["dark"])
            im.line(x, 28, x + 3, 28, p["accent"])

    # Exact UV regions used by ElisarArmorModel for raised geometry.
    details = [
        (64, 0, 9, 2, 1), (88, 0, 1, 5, 1), (96, 0, 2, 4, 2),
        (64, 16, 9, 7, 1), (88, 16, 9, 7, 1), (64, 32, 9, 2, 1),
        (64, 48, 6, 5, 7), (96, 48, 1, 5, 5), (64, 64, 5, 4, 5),
        (88, 64, 5, 5, 1), (104, 64, 5, 3, 1), (64, 80, 5, 4, 5),
        (88, 80, 5, 2, 2), (112, 80, 2, 4, 2),
    ]
    for args in details:
        armor_detail_box(im, *args, plate)
    im.noise(4100 + layer + (0 if material == "aurite" else 50), 4)
    return im


# ---- resident skins -----------------------------------------------------------------
def skin_base(im: Canvas, part_box, color: str, palettes: dict) -> None:
    box_faces(im, *part_box, palettes, base=color)


def make_resident(kind: str) -> Canvas:
    skin = {"shadow": (116, 76, 56, 255), "dark": (155, 105, 76, 255), "base": (190, 139, 102, 255),
            "light": (215, 169, 130, 255), "accent": (57, 116, 54, 255), "accent_hi": (107, 175, 78, 255),
            "glint": (245, 235, 211, 255), "wood": (67, 45, 30, 255)}
    robe = {"shadow": (61, 43, 65, 255), "dark": (100, 70, 103, 255), "base": (151, 112, 145, 255),
            "light": (205, 178, 183, 255), "accent": (223, 175, 71, 255), "accent_hi": (250, 215, 125, 255),
            "glint": (241, 227, 210, 255), "wood": (76, 51, 43, 255)}
    leather = {"shadow": (49, 36, 34, 255), "dark": (79, 54, 45, 255), "base": (112, 76, 59, 255),
               "light": (160, 116, 80, 255), "accent": (64, 132, 119, 255), "accent_hi": (111, 185, 157, 255),
               "glint": (237, 218, 174, 255), "wood": (60, 42, 31, 255)}
    metal = AURITE if kind == "guard" else leather
    im = Canvas(64, 64)
    skin_base(im, (0, 0, 8, 8, 8), "base", skin)
    skin_base(im, (32, 0, 8, 8, 8), "base" if kind == "villager" else "base", metal)
    skin_base(im, (16, 16, 8, 12, 4), "base", robe if kind == "villager" else metal)
    skin_base(im, (40, 16, 4, 12, 4), "base", robe if kind == "villager" else metal)
    skin_base(im, (32, 48, 4, 12, 4), "base", robe if kind == "villager" else metal)
    skin_base(im, (0, 16, 4, 12, 4), "wood", robe if kind == "villager" else leather)
    skin_base(im, (16, 48, 4, 12, 4), "wood", robe if kind == "villager" else leather)
    # Brows, white/green eyes and shaded cheek pixels on the actual head-front UV.
    im.rect(9, 10, 14, 10, (54, 39, 24, 255))
    im.rect(9, 11, 10, 12, (240, 235, 214, 255))
    im.rect(13, 11, 14, 12, (240, 235, 214, 255))
    im.rect(10, 12, 10, 12, (50, 135, 62, 255))
    im.rect(13, 12, 13, 12, (50, 135, 62, 255))
    im.set(8, 14, skin["dark"]); im.set(15, 14, skin["dark"])
    # Nose UV (52,0) is a 2x4x2 cuboid.
    box_faces(im, 52, 0, 2, 4, 2, skin)
    # Restore face after nose call (separate UV, but the helper does not overlap it).
    if kind == "villager":
        im.rect(20, 20, 27, 23, robe["light"])
        im.rect(20, 24, 21, 31, robe["dark"])
        im.rect(26, 24, 27, 31, robe["dark"])
        im.rect(23, 20, 24, 31, robe["base"])
        im.line(20, 30, 27, 30, robe["accent"])
        for x in (44, 36):
            im.rect(x, 20, x + 3, 23, robe["light"])
            im.line(x, 29, x + 3, 29, robe["accent"])
        im.rect(23, 25, 24, 26, robe["accent_hi"])
    elif kind == "guard":
        # Gold helmet with celadon crest, heavy shoulder plates and laminated chest.
        im.rect(40, 10, 47, 12, AURITE["accent"])
        im.line(41, 5, 46, 4, AURITE["light"], 2)
        gem(im, 44, 8, AURITE)
        im.rect(20, 20, 27, 23, AURITE["accent"])
        im.rect(20, 28, 27, 30, AURITE["accent"])
        im.poly([(24, 23), (27, 25), (24, 28), (21, 25)], AURITE["base"])
        for x in (44, 36):
            im.rect(x, 20, x + 3, 23, AURITE["light"])
            im.line(x, 25, x + 3, 25, AURITE["accent"])
        for x in (4, 20):
            im.rect(x, 26, x + 3, 31, AURITE["base"])
            im.line(x, 29, x + 3, 29, AURITE["light"])
    else:
        # Archer hood, teal short cape, diagonal leather strap and belt buckle.
        im.rect(40, 5, 47, 9, leather["accent"])
        im.line(41, 4, 46, 4, leather["accent_hi"], 2)
        im.line(20, 20, 27, 27, leather["wood"], 2)
        im.rect(20, 27, 27, 29, leather["accent"])
        im.rect(23, 27, 24, 29, AURITE["base"])
        im.rect(20, 20, 22, 23, leather["accent"])
        for x in (44, 36):
            im.rect(x, 20, x + 3, 23, leather["accent"])
            im.line(x, 28, x + 3, 28, AURITE["base"])
        # Quiver UV at 48,32 and arrows at 48,46.
        im.rect(48, 32, 63, 45, leather["wood"])
        im.line(49, 33, 61, 33, AURITE["base"], 2)
        im.rect(48, 46, 63, 63, TRANSPARENT)
        for x in (49, 54, 59):
            im.line(x, 51, x, 62, (106, 73, 45, 255))
            im.line(x - 1, 51, x + 1, 51, (226, 222, 202, 255), 2)
            im.set(x, 50, AURITE["light"])
    im.noise(5150 + {"villager": 0, "guard": 10, "archer": 20}[kind], 4)
    return im


def make_ore(deepslate: bool) -> Canvas:
    im = Canvas(16, 16)
    rng = random.Random(9081 + int(deepslate))
    for y in range(16):
        for x in range(16):
            n = rng.randint(-14, 14)
            if deepslate:
                band = int(7 * math.sin(y * 1.4 + x * .25))
                c = (54 + n + band, 57 + n + band, 65 + n + band, 255)
            else:
                c = (112 + n, 113 + n, 120 + n, 255)
            im.set(x, y, c)
    clusters = [(3, 3), (11, 2), (7, 7), (13, 10), (4, 12), (9, 14)]
    for i, (x, y) in enumerate(clusters):
        p = RIFT if i % 3 else {**RIFT, "base": RIFT["accent"], "light": RIFT["accent_hi"]}
        im.poly([(x, y - 2), (x + 2, y), (x + 1, y + 3), (x - 1, y + 2), (x - 2, y)], p["shadow"])
        im.poly([(x, y - 1), (x + 1, y), (x, y + 2), (x - 1, y)], p["base"])
        im.set(x, y - 1, p["light"])
        if i % 2 == 0:
            im.set(x + 1, y, p["glint"])
    return im


def item_model(name: str, handheld: bool = False) -> None:
    write_json(ASSETS / f"models/item/{name}.json", {
        "parent": "minecraft:item/handheld" if handheld else "minecraft:item/generated",
        "textures": {"layer0": f"riftworld:item/{name}"},
    })


def shaped(name: str, pattern: list[str], key: dict, result: str, count: int = 1) -> None:
    write_json(DATA / f"riftworld/recipes/{name}.json", {
        "type": "minecraft:crafting_shaped", "pattern": pattern,
        "key": {k: ({"tag": v[1:]} if v.startswith("#") else {"item": v}) for k, v in key.items()},
        "result": {"item": result, "count": count}, "show_notification": True,
    })


def shapeless(name: str, ingredients: list[str], result: str, count: int = 1) -> None:
    write_json(DATA / f"riftworld/recipes/{name}.json", {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [{"item": item} for item in ingredients],
        "result": {"item": result, "count": count},
    })


def ore_loot(ore: str) -> None:
    write_json(DATA / f"riftworld/loot_tables/blocks/{ore}.json", {
        "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
            "type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": f"riftworld:{ore}", "conditions": [{
                    "condition": "minecraft:match_tool", "predicate": {"enchantments": [{
                        "enchantment": "minecraft:silk_touch", "levels": {"min": 1}}]}}]},
                {"type": "minecraft:item", "name": "riftworld:riftcrystal_shard", "functions": [
                    {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}},
                    {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                    {"function": "minecraft:explosion_decay"},
                ]},
            ]}]}],
    })


def block_resources(ore: str) -> None:
    write_json(ASSETS / f"blockstates/{ore}.json", {"variants": {"": [
        {"model": f"riftworld:block/{ore}", "y": r} for r in (0, 90, 180, 270)]}})
    write_json(ASSETS / f"models/block/{ore}.json", {
        "parent": "minecraft:block/cube_all", "textures": {"all": f"riftworld:block/{ore}"}})
    write_json(ASSETS / f"models/item/{ore}.json", {"parent": f"riftworld:block/{ore}"})


def generate() -> None:
    for path in (ITEM_TEX, BLOCK_TEX, ENTITY_TEX, ARMOR_TEX, REPORT):
        path.mkdir(parents=True, exist_ok=True)

    # Ores and material items.
    make_ore(False).save(BLOCK_TEX / "riftcrystal_ore.png")
    make_ore(True).save(BLOCK_TEX / "deepslate_riftcrystal_ore.png")
    for ore in ("riftcrystal_ore", "deepslate_riftcrystal_ore"):
        block_resources(ore)
        ore_loot(ore)
    make_ingot(AURITE).save(ITEM_TEX / "aurite_ingot.png")
    make_ingot(RIFT, True).save(ITEM_TEX / "riftcrystal_shard.png")
    make_ingot(RIFT).save(ITEM_TEX / "riftcrystal_ingot.png")
    for item in ("aurite_ingot", "riftcrystal_shard", "riftcrystal_ingot"):
        item_model(item)

    # All 18 equipment icons and models.
    for material, p in (("aurite", AURITE), ("riftcrystal", RIFT)):
        make_sword(p).save(ITEM_TEX / f"{material}_sword.png")
        make_pickaxe(p).save(ITEM_TEX / f"{material}_pickaxe.png")
        make_axe(p).save(ITEM_TEX / f"{material}_war_axe.png")
        make_shield(p).save(ITEM_TEX / f"{material}_shield.png")
        for tool in ("sword", "pickaxe", "war_axe", "shield"):
            item_model(f"{material}_{tool}", handheld=True)
        # Separate blocking model keeps the vanilla blocking predicate functional.
        write_json(ASSETS / f"models/item/{material}_shield.json", {
            "parent": "minecraft:item/handheld", "textures": {"layer0": f"riftworld:item/{material}_shield"},
            "overrides": [{"predicate": {"blocking": 1}, "model": f"riftworld:item/{material}_shield_blocking"}],
        })
        item_model(f"{material}_shield_blocking", handheld=True)
        make_bow(p).save(ITEM_TEX / f"{material}_bow.png")
        write_json(ASSETS / f"models/item/{material}_bow.json", {
            "parent": "minecraft:item/handheld", "textures": {"layer0": f"riftworld:item/{material}_bow"},
            "overrides": [
                {"predicate": {"pulling": 1}, "model": f"riftworld:item/{material}_bow_pulling_0"},
                {"predicate": {"pulling": 1, "pull": 0.65}, "model": f"riftworld:item/{material}_bow_pulling_1"},
                {"predicate": {"pulling": 1, "pull": 0.9}, "model": f"riftworld:item/{material}_bow_pulling_2"},
            ],
        })
        for stage in range(3):
            make_bow(p, stage).save(ITEM_TEX / f"{material}_bow_pulling_{stage}.png")
            item_model(f"{material}_bow_pulling_{stage}", handheld=True)
        for part in ("helmet", "chestplate", "leggings", "boots"):
            make_armor_icon(part, p).save(ITEM_TEX / f"{material}_{part}.png")
            item_model(f"{material}_{part}")
        make_armor_layer(material, 1).save(ARMOR_TEX / f"{material}_layer_1.png")
        make_armor_layer(material, 2).save(ARMOR_TEX / f"{material}_layer_2.png")

    for kind, back, spots in (("villager", "8f718e", "d8ad4c"), ("guard", "4e877f", "e0a83e"), ("archer", "477e78", "795347")):
        make_resident(kind).save(ENTITY_TEX / f"elisar_{kind}.png")
        make_spawn_egg(back, spots).save(ITEM_TEX / f"elisar_{kind}_spawn_egg.png")
        item_model(f"elisar_{kind}_spawn_egg")

    # Currency <-> equipment metal. Four ducats remain fully reversible.
    shaped("aurite_ingot_from_ducats", ["DD", "DD"], {"D": "riftworld:elisar_ducat"}, "riftworld:aurite_ingot")
    shapeless("ducats_from_aurite_ingot", ["riftworld:aurite_ingot"], "riftworld:elisar_ducat", 4)
    shaped("riftcrystal_ingot", ["SGS", "GDG", "SGS"], {
        "S": "riftworld:riftcrystal_shard", "G": "minecraft:gold_ingot", "D": "minecraft:diamond",
    }, "riftworld:riftcrystal_ingot")
    for ore in ("riftcrystal_ore", "deepslate_riftcrystal_ore"):
        for kind, time in (("smelting", 200), ("blasting", 100)):
            write_json(DATA / f"riftworld/recipes/{ore}_{kind}.json", {
                "type": f"minecraft:{kind}", "ingredient": {"item": f"riftworld:{ore}"},
                "result": "riftworld:riftcrystal_shard", "experience": 1.2, "cookingtime": time,
            })

    for material, ingot in (("aurite", "riftworld:aurite_ingot"), ("riftcrystal", "riftworld:riftcrystal_ingot")):
        shaped(f"{material}_sword", ["I", "I", "S"], {"I": ingot, "S": "minecraft:stick"}, f"riftworld:{material}_sword")
        shaped(f"{material}_pickaxe", ["III", " S ", " S "], {"I": ingot, "S": "minecraft:stick"}, f"riftworld:{material}_pickaxe")
        shaped(f"{material}_war_axe", ["III", "ISI", " S "], {"I": ingot, "S": "minecraft:stick"}, f"riftworld:{material}_war_axe")
        shaped(f"{material}_bow", [" SI", "S I", " SI"], {"S": "minecraft:stick", "I": "minecraft:string"}, f"riftworld:{material}_bow")
        shaped(f"{material}_shield", ["P P", "PIP", " P "], {"P": "#minecraft:planks", "I": ingot}, f"riftworld:{material}_shield")
        shaped(f"{material}_helmet", ["III", "I I"], {"I": ingot}, f"riftworld:{material}_helmet")
        shaped(f"{material}_chestplate", ["I I", "III", "III"], {"I": ingot}, f"riftworld:{material}_chestplate")
        shaped(f"{material}_leggings", ["III", "I I", "I I"], {"I": ingot}, f"riftworld:{material}_leggings")
        shaped(f"{material}_boots", ["I I", "I I"], {"I": ingot}, f"riftworld:{material}_boots")

    # Worldgen: three explicit biomes, small veins, low Y and strong air-exposure discard.
    write_json(DATA / "riftworld/worldgen/configured_feature/ore_riftcrystal.json", {
        "type": "minecraft:ore", "config": {"size": 3, "discard_chance_on_air_exposure": 0.65, "targets": [
            {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceable"},
             "state": {"Name": "riftworld:riftcrystal_ore"}},
            {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceable"},
             "state": {"Name": "riftworld:deepslate_riftcrystal_ore"}},
        ]},
    })
    write_json(DATA / "riftworld/worldgen/placed_feature/ore_riftcrystal.json", {
        "feature": "riftworld:ore_riftcrystal", "placement": [
            {"type": "minecraft:rarity_filter", "chance": 3}, {"type": "minecraft:count", "count": 1},
            {"type": "minecraft:in_square"}, {"type": "minecraft:height_range", "height": {
                "type": "minecraft:trapezoid", "min_inclusive": {"absolute": -56}, "max_inclusive": {"absolute": 16}}},
            {"type": "minecraft:biome"},
        ],
    })
    biomes = ["minecraft:lush_caves", "minecraft:dripstone_caves", "minecraft:deep_dark"]
    write_json(DATA / "riftworld/forge/biome_modifier/add_riftcrystal_ore.json", {
        "type": "forge:add_features", "biomes": biomes, "features": "riftworld:ore_riftcrystal", "step": "underground_ores",
    })
    write_json(DATA / "riftworld/tags/worldgen/biome/has_riftcrystal_ore.json", {"replace": False, "values": biomes})

    # Mining and gameplay tags (merge; never erase the other mod blocks).
    merge_json(DATA / "minecraft/tags/blocks/mineable/pickaxe.json", {"values": [
        "riftworld:riftcrystal_ore", "riftworld:deepslate_riftcrystal_ore"]})
    merge_json(DATA / "minecraft/tags/blocks/needs_diamond_tool.json", {"values": [
        "riftworld:riftcrystal_ore", "riftworld:deepslate_riftcrystal_ore"]})
    merge_json(DATA / "minecraft/tags/blocks/needs_iron_tool.json", {"values": [
        "riftworld:ducat_ore", "riftworld:deepslate_ducat_ore"]})
    for tag, tools in (("swords", ["sword"]), ("pickaxes", ["pickaxe"]), ("axes", ["war_axe"])):
        merge_json(DATA / f"minecraft/tags/items/{tag}.json", {"values": [f"riftworld:{m}_{t}" for m in ("aurite", "riftcrystal") for t in tools]})
    merge_json(DATA / "minecraft/tags/items/piglin_loved.json", {"values": ["riftworld:aurite_ingot"] + [
        f"riftworld:aurite_{x}" for x in ("helmet", "chestplate", "leggings", "boots", "sword", "pickaxe", "war_axe", "bow", "shield")]})

    en = {
        "block.riftworld.riftcrystal_ore": "Riftcrystal Ore",
        "block.riftworld.deepslate_riftcrystal_ore": "Deepslate Riftcrystal Ore",
        "item.riftworld.aurite_ingot": "Aurite Ingot",
        "item.riftworld.riftcrystal_shard": "Riftcrystal Shard",
        "item.riftworld.riftcrystal_ingot": "Riftcrystal Ingot",
        "item.riftworld.aurite_armor.set_bonus": "Full set: Haste I + Luck I",
        "item.riftworld.riftcrystal_armor.set_bonus": "Full set: Resistance I + Fire Resistance",
        "entity.riftworld.elisar_villager": "Elisar Citizen",
        "entity.riftworld.elisar_guard": "Elisar Guard",
        "entity.riftworld.elisar_archer": "Elisar Archer",
        "item.riftworld.elisar_villager_spawn_egg": "Elisar Citizen Spawn Egg",
        "item.riftworld.elisar_guard_spawn_egg": "Elisar Guard Spawn Egg",
        "item.riftworld.elisar_archer_spawn_egg": "Elisar Archer Spawn Egg",
    }
    ru = {
        "block.riftworld.riftcrystal_ore": "Рифткристаллическая руда",
        "block.riftworld.deepslate_riftcrystal_ore": "Глубинносланцевая рифткристаллическая руда",
        "item.riftworld.aurite_ingot": "Ауритовый слиток",
        "item.riftworld.riftcrystal_shard": "Осколок рифткристалла",
        "item.riftworld.riftcrystal_ingot": "Рифткристаллический слиток",
        "item.riftworld.aurite_armor.set_bonus": "Полный комплект: Спешка I + Удача I",
        "item.riftworld.riftcrystal_armor.set_bonus": "Полный комплект: Сопротивление I + Огнестойкость",
        "entity.riftworld.elisar_villager": "Житель Элисара",
        "entity.riftworld.elisar_guard": "Стражник Элисара",
        "entity.riftworld.elisar_archer": "Лучник Элисара",
        "item.riftworld.elisar_villager_spawn_egg": "Яйцо призыва: житель Элисара",
        "item.riftworld.elisar_guard_spawn_egg": "Яйцо призыва: стражник Элисара",
        "item.riftworld.elisar_archer_spawn_egg": "Яйцо призыва: лучник Элисара",
    }
    names = {
        "helmet": ("Aurite Helmet", "Ауритовый шлем"), "chestplate": ("Aurite Chestplate", "Ауритовый нагрудник"),
        "leggings": ("Aurite Leggings", "Ауритовые поножи"), "boots": ("Aurite Boots", "Ауритовые ботинки"),
        "sword": ("Aurite Sword", "Ауритовый меч"), "pickaxe": ("Aurite Pickaxe", "Ауритовая кирка"),
        "war_axe": ("Aurite War Axe", "Ауритовый боевой топор"), "bow": ("Aurite Bow", "Ауритовый лук"),
        "shield": ("Aurite Shield", "Ауритовый щит"),
    }
    for material, prefix_en, prefix_ru in (("aurite", "Aurite", "Ауритовый"), ("riftcrystal", "Riftcrystal", "Рифткристаллический")):
        for part in ("helmet", "chestplate", "leggings", "boots", "sword", "pickaxe", "war_axe", "bow", "shield"):
            en_name, ru_name = names[part]
            if material == "riftcrystal":
                en_name = en_name.replace("Aurite", "Riftcrystal")
                ru_name = ru_name.replace("Ауритовый", "Рифткристаллический").replace("Ауритовая", "Рифткристаллическая").replace("Ауритовые", "Рифткристаллические")
            en[f"item.riftworld.{material}_{part}"] = en_name
            ru[f"item.riftworld.{material}_{part}"] = ru_name
    merge_json(ASSETS / "lang/en_us.json", en)
    merge_json(ASSETS / "lang/ru_ru.json", ru)
    print(f"v0.41 resources generated: {RES}")


if __name__ == "__main__":
    generate()
