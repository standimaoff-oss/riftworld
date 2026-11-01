#!/usr/bin/env python3
"""
Pure-Python glTF 2.0 (.glb) high-detail model & procedural PNG texture generator for v0.19.1:
  - assets/riftworld/models/glb/rift_obelisk_boss.glb  (1:1 to 01_colonnade_boss_ref.png, ZERO human shadow!)
  - assets/riftworld/models/glb/crystal_minion.glb     (1:1 to 02_crystal_minions_ref.png, floating golem mask + crystal crown + wisp + floating claws!)
  - assets/riftworld/models/glb/crystal_katar.glb      (exact matching gauntlet-blade weapon)
  - assets/riftworld/models/glb/crystal_shield.glb     (octagonal marble-gold runic crystal shield)
  - assets/riftworld/models/glb/crystal_shard.glb      (6-faceted bipyramidal crystal spear)
  - assets/riftworld/models/glb/astral_prism.glb       (handheld celestial astrolabe relic)
"""
import json
import math
import os
import random
import struct
import zlib

OUT_DIR = "mod/src/main/resources/assets/riftworld/models/glb"


# ==============================================================================
# 1. PURE-PYTHON RGBA PNG ENCODER & PROCEDURAL TEXTURE PAINTERS (256x256)
# ==============================================================================
def encode_png(w: int, h: int, rgba: bytearray) -> bytes:
    raw = bytearray()
    stride = w * 4
    for y in range(h):
        raw.append(0)
        raw.extend(rgba[y * stride:(y + 1) * stride])

    def chunk(tag: bytes, payload: bytes) -> bytes:
        crc = zlib.crc32(tag + payload) & 0xFFFFFFFF
        return struct.pack(">I", len(payload)) + tag + payload + struct.pack(">I", crc)

    ihdr = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)
    idat = zlib.compress(bytes(raw), 9)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", idat) + chunk(b"IEND", b"")


def hash2(x: int, y: int, seed: int = 0) -> float:
    n = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def vnoise(x: float, y: float, seed: int = 0) -> float:
    ix, iy = math.floor(x), math.floor(y)
    fx, fy = x - ix, y - iy
    ux = fx * fx * (3.0 - 2.0 * fx)
    uy = fy * fy * (3.0 - 2.0 * fy)
    a = hash2(ix, iy, seed)
    b = hash2(ix + 1, iy, seed)
    c = hash2(ix, iy + 1, seed)
    d = hash2(ix + 1, iy + 1, seed)
    return (a * (1 - ux) + b * ux) * (1 - uy) + (c * (1 - ux) + d * ux) * uy


def fbm(x: float, y: float, seed: int = 0, octaves: int = 4) -> float:
    val = 0.0
    amp = 0.5
    freq = 1.0
    for i in range(octaves):
        val += amp * vnoise(x * freq, y * freq, seed + i * 31)
        freq *= 2.02
        amp *= 0.5
    return val


def clamp8(v: float) -> int:
    return 0 if v < 0 else (255 if v > 255 else int(v))


def make_marble_png(size: int = 256) -> bytes:
    """Monumental smooth pale Carrara marble (#EEF1F5..#CED5DF) with flowing slate veins & crisp chiseled bevels."""
    buf = bytearray(size * size * 4)
    for y in range(size):
        for x in range(size):
            n1 = fbm(x * 0.024, y * 0.024, 11, 4)
            n2 = fbm(x * 0.055, y * 0.055, 29, 3)
            # Smooth Carrara marble veins (low frequency, zero noisy pepper speckles!)
            vein_phase = (x * 0.026 + y * 0.019) + n1 * 4.2 + n2 * 1.1
            vein = abs(math.sin(vein_phase))
            vein_soft = max(0.0, 1.0 - vein * 3.2)
            vein_fine = max(0.0, 1.0 - abs(math.sin(vein_phase * 2.1 + 0.8)) * 9.0) * 0.55

            base_r = 240.0 - n1 * 18.0 - vein_soft * 28.0 - vein_fine * 34.0
            base_g = 243.0 - n1 * 16.0 - vein_soft * 25.0 - vein_fine * 30.0
            base_b = 247.0 - n1 * 14.0 - vein_soft * 20.0 - vein_fine * 24.0

            # Chiseled stone edge bevel & subtle dark outer seam
            edge = min(x, y, size - 1 - x, size - 1 - y)
            if edge < 3:
                k = (3 - edge) / 3.0
                base_r -= k * 34.0
                base_g -= k * 31.0
                base_b -= k * 27.0
            elif edge < 9:
                k = (9 - edge) / 6.0
                base_r += k * 8.0
                base_g += k * 8.0
                base_b += k * 8.0

            idx = (y * size + x) * 4
            buf[idx] = clamp8(base_r)
            buf[idx + 1] = clamp8(base_g)
            buf[idx + 2] = clamp8(base_b)
            buf[idx + 3] = 255
    return encode_png(size, size, buf)


def make_ashlar_stone_png(size: int = 256) -> bytes:
    """Carved ancient grey-ivory temple limestone blocks with bevels & mortar for circular rune rings & wheels."""
    buf = bytearray(size * size * 4)
    for y in range(size):
        for x in range(size):
            n = fbm(x * 0.045, y * 0.045, 53, 4)
            r = 208.0 - n * 26.0
            g = 214.0 - n * 24.0
            b = 220.0 - n * 20.0

            bx, by = x % 64, y % 64
            if bx < 2 or by < 2 or bx > 61 or by > 61:
                r -= 40.0
                g -= 38.0
                b -= 34.0
            elif bx < 5 or by < 5:
                r += 13.0
                g += 13.0
                b += 13.0
            elif bx > 58 or by > 58:
                r -= 15.0
                g -= 14.0
                b -= 13.0

            idx = (y * size + x) * 4
            buf[idx] = clamp8(r)
            buf[idx + 1] = clamp8(g)
            buf[idx + 2] = clamp8(b)
            buf[idx + 3] = 255
    return encode_png(size, size, buf)


def make_gold_relief_png(size: int = 256) -> bytes:
    """
    Quadrant [0..128, 0..128]: Square golden emblem panel with raised sun-cross circle relief (01_colonnade_boss_ref.png)!
    Remaining quadrants: Brushed antique temple bronze & burnished gold for wheel spokes, hubs & trim.
    """
    buf = bytearray(size * size * 4)
    for y in range(size):
        for x in range(size):
            n = fbm(x * 0.06, y * 0.06, 131, 3)
            r = 204.0 + (n - 0.5) * 30.0
            g = 164.0 + (n - 0.5) * 25.0
            b = 74.0 + (n - 0.5) * 16.0

            if x < 128 and y < 128:
                cx, cy = x - 64.0, y - 64.0
                adx, ady = abs(cx), abs(cy)
                dist = math.hypot(cx, cy)
                r, g, b = 148.0 + n * 18.0, 120.0 + n * 15.0, 54.0 + n * 10.0
                if max(adx, ady) > 54 or (44 <= max(adx, ady) <= 48):
                    r, g, b = 232.0, 194.0, 96.0
                elif 48 < max(adx, ady) <= 54:
                    r, g, b = 102.0, 80.0, 32.0
                if 31.0 <= dist <= 37.0 or dist <= 8.5:
                    r, g, b = 238.0, 200.0, 102.0
                elif dist < 31.0 and (adx <= 3.5 or ady <= 3.5):
                    r, g, b = 230.0, 192.0, 94.0
                elif 37.0 < dist <= 40.0:
                    r, g, b = 94.0, 72.0, 28.0
            else:
                band = math.sin(y * 0.20) * 12.0
                r += band
                g += band * 0.85
                b += band * 0.45

            idx = (y * size + x) * 4
            buf[idx] = clamp8(r)
            buf[idx + 1] = clamp8(g)
            buf[idx + 2] = clamp8(b)
            buf[idx + 3] = 255
    return encode_png(size, size, buf)


def make_crystal_png(size: int = 256, translucent: bool = False) -> bytes:
    """
    Clean, glass-smooth cut-gemstone aquamarine crystal texture (NO wavy stripes!):
      - Deep teal-cerulean base (v=0) -> rich luminous turquoise mid -> bright icy cyan-white tip (v=1),
      - Crisp straight facet edge highlights at UV facet seams (u*6 near integer) & subtle diagonal glass sheen!
    """
    buf = bytearray(size * size * 4)
    for y in range(size):
        v = y / (size - 1.0)  # v=0 base, v=1 tip
        for x in range(size):
            u = x / (size - 1.0)
            # Clean geometric facet UV cell (6 sides around crystal prism)
            uf = (u * 6.0) % 1.0
            side_id = int(u * 6.0) % 6
            # Crisp straight highlight right along the longitudinal crystal arris (uf near 0 or 1)
            edge_dist = min(uf, 1.0 - uf)
            arris = max(0.0, 1.0 - edge_dist * 11.0) ** 1.6
            # Subtle planar tilt shading across each flat crystal face (no wavy noise!)
            planar = (uf - 0.5) * (0.16 if side_id % 2 == 0 else -0.16)
            # Subtle straight internal cleavage reflection planes
            cleave1 = max(0.0, 1.0 - abs((v - uf * 0.28 - 0.52)) * 18.0) * 0.22
            cleave2 = max(0.0, 1.0 - abs((v + uf * 0.24 - 0.78)) * 22.0) * 0.18
            soft_cloud = (fbm(x * 0.022, y * 0.022, 211, 3) - 0.5) * 0.08

            # Rich teal -> aquamarine -> icy cyan-white progression from base to tip
            tip_glow = (v ** 1.55)
            lum = max(0.0, min(1.35, 0.22 + 0.62 * tip_glow + planar + arris * 0.42 + cleave1 + cleave2 + soft_cloud))

            r = 10.0 + lum * 138.0 + arris * 76.0 + (v ** 3.0) * 45.0
            g = 112.0 + lum * 136.0 + arris * 34.0
            b = 144.0 + lum * 108.0 + arris * 22.0

            if translucent:
                alpha = clamp8(122.0 + tip_glow * 74.0 + arris * 48.0)
            else:
                alpha = 255

            idx = (y * size + x) * 4
            buf[idx] = clamp8(r)
            buf[idx + 1] = clamp8(g)
            buf[idx + 2] = clamp8(b)
            buf[idx + 3] = alpha
    return encode_png(size, size, buf)


def make_wisp_png(size: int = 256) -> bytes:
    """Translucent spectral cyan energy ribbon / vortex texture with luminous core & feathery edges (alpha BLEND)."""
    buf = bytearray(size * size * 4)
    for y in range(size):
        v = y / (size - 1.0)  # 0 = top, 1 = bottom tip fade
        for x in range(size):
            u = x / (size - 1.0)
            n = fbm(x * 0.035, y * 0.035, 409, 3)
            streak = 0.58 + 0.42 * math.sin(u * math.pi * 4.0 + n * 3.0)
            edge_u = math.sin(u * math.pi) ** 0.75
            fade_v = math.sin(min(1.0, v * 1.02) * math.pi * 0.94)
            intensity = max(0.0, edge_u * fade_v * streak)
            r = 70.0 + intensity * 175.0
            g = 236.0 + intensity * 19.0
            b = 254.0
            a = clamp8(intensity * 225.0)
            idx = (y * size + x) * 4
            buf[idx] = clamp8(r)
            buf[idx + 1] = clamp8(g)
            buf[idx + 2] = clamp8(b)
            buf[idx + 3] = a
    return encode_png(size, size, buf)


def make_rune_cutout_png(size: int = 256) -> bytes:
    """
    Crisp alpha-cutout fullbright cyan arcane runes (alpha=0 outside strokes, alpha=255 bright cyan-white inside!).
    - Rows y=0..191: 8x3 grid of 32x64 rune cells WITH top & bottom glowing track lines + carved arcane glyphs!
    - Rows y=192..255: continuous glowing cyan double-track border + solid fullbright cyan-white swatch!
    """
    buf = bytearray(size * size * 4)

    def put_px(px: int, py: int, core: bool = True):
        if 0 <= px < size and 0 <= py < size:
            i = (py * size + px) * 4
            if core:
                buf[i], buf[i + 1], buf[i + 2], buf[i + 3] = 185, 255, 255, 255
            elif buf[i + 3] == 0:
                buf[i], buf[i + 1], buf[i + 2], buf[i + 3] = 36, 228, 244, 255

    def stroke(x0: float, y0: float, x1: float, y1: float, rad: float = 1.65):
        steps = max(4, int(math.hypot(x1 - x0, y1 - y0) * 2.0))
        ir = int(math.ceil(rad + 1))
        for s in range(steps + 1):
            t = s / steps
            cx = x0 + (x1 - x0) * t
            cy = y0 + (y1 - y0) * t
            for dy in range(-ir, ir + 1):
                for dx in range(-ir, ir + 1):
                    d = math.hypot(dx, dy)
                    if d <= rad * 0.65:
                        put_px(int(round(cx + dx)), int(round(cy + dy)), True)
                    elif d <= rad:
                        put_px(int(round(cx + dx)), int(round(cy + dy)), False)

    def circle_stroke(cx: float, cy: float, r: float, rad: float = 1.55, a0: float = 0.0, a1: float = math.tau):
        steps = max(24, int(r * 6))
        for s in range(steps + 1):
            a = a0 + (a1 - a0) * (s / steps)
            px = cx + math.cos(a) * r
            py = cy + math.sin(a) * r
            stroke(px, py, px, py, rad)

    for row in range(3):
        oy = row * 64
        # Subtle glowing cyan top & bottom framing border lines along every rune band (matching 01_colonnade_boss_ref.png!)
        if row == 0:
            stroke(0, oy + 5, size - 1, oy + 5, 1.35)
            stroke(0, oy + 58, size - 1, oy + 58, 1.35)
        for col in range(8):
            ox = col * 32
            cx, cy = ox + 16.0, oy + 32.0
            g = (col + row * 3) % 8
            if g == 0:
                circle_stroke(cx, cy, 9.0, 1.6)
                stroke(cx - 11, cy, cx + 11, cy, 1.5)
                stroke(cx, cy - 13, cx, cy + 13, 1.5)
            elif g == 1:
                circle_stroke(cx, cy - 2, 8.5, 1.6, -0.4, math.pi * 1.45)
                stroke(cx, cy - 2, cx + 8, cy - 2, 1.6)
                stroke(cx, cy - 2, cx, cy + 13, 1.6)
                stroke(cx - 7, cy + 13, cx + 7, cy + 13, 1.5)
            elif g == 2:
                stroke(cx - 9, cy - 13, cx + 9, cy - 13, 1.6)
                stroke(cx, cy - 13, cx, cy + 13, 1.6)
                stroke(cx - 7, cy, cx + 7, cy, 1.5)
                stroke(cx - 9, cy + 13, cx + 9, cy + 13, 1.6)
            elif g == 3:
                stroke(cx - 9, cy - 12, cx + 9, cy - 12, 1.6)
                stroke(cx + 8, cy - 12, cx - 8, cy + 12, 1.6)
                stroke(cx - 9, cy + 12, cx + 9, cy + 12, 1.6)
                stroke(cx - 6, cy, cx + 6, cy, 1.4)
            elif g == 4:
                stroke(cx, cy - 14, cx + 9, cy, 1.6)
                stroke(cx + 9, cy, cx, cy + 14, 1.6)
                stroke(cx, cy + 14, cx - 9, cy, 1.6)
                stroke(cx - 9, cy, cx, cy - 14, 1.6)
                circle_stroke(cx, cy, 3.0, 1.5)
            elif g == 5:
                stroke(cx, cy - 13, cx, cy + 14, 1.6)
                stroke(cx - 9, cy - 4, cx + 9, cy - 4, 1.5)
                stroke(cx - 9, cy - 13, cx - 9, cy - 4, 1.5)
                stroke(cx + 9, cy - 13, cx + 9, cy - 4, 1.5)
                stroke(cx - 6, cy + 8, cx + 6, cy + 8, 1.5)
            elif g == 6:
                circle_stroke(cx, cy - 3, 8.5, 1.6, math.pi * 0.85, math.pi * 2.15)
                stroke(cx - 7, cy + 4, cx - 10, cy + 13, 1.6)
                stroke(cx + 7, cy + 4, cx + 10, cy + 13, 1.6)
                stroke(cx, cy - 7, cx, cy + 9, 1.5)
            else:
                stroke(cx - 9, cy - 13, cx + 9, cy - 13, 1.6)
                stroke(cx - 9, cy - 13, cx + 9, cy + 13, 1.5)
                stroke(cx + 9, cy - 13, cx - 9, cy + 13, 1.5)
                stroke(cx - 9, cy + 13, cx + 9, cy + 13, 1.6)

    for x in range(size):
        stroke(x, 200, x, 200, 1.8)
        stroke(x, 212, x, 212, 1.8)
    for y in range(220, size):
        for x in range(size):
            i = (y * size + x) * 4
            buf[i], buf[i + 1], buf[i + 2], buf[i + 3] = 165, 255, 255, 255

    return encode_png(size, size, buf)


# ==============================================================================
# 2. 3D VECTOR / MATRIX MATH & POLYHEDRAL MESH BUILDER
# ==============================================================================
def v_add(a, b):
    return (a[0] + b[0], a[1] + b[1], a[2] + b[2])


def v_sub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def v_mul(a, s):
    return (a[0] * s, a[1] * s, a[2] * s)


def v_lerp(a, b, t):
    return (a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t)


def v_cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def v_norm(a):
    l = math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2])
    return (a[0] / l, a[1] / l, a[2] / l) if l > 1e-8 else (0.0, 1.0, 0.0)


def rot_xyz(pt, rx_deg=0.0, ry_deg=0.0, rz_deg=0.0):
    x, y, z = pt
    if rx_deg:
        r = math.radians(rx_deg)
        c, s = math.cos(r), math.sin(r)
        y, z = y * c - z * s, y * s + z * c
    if ry_deg:
        r = math.radians(ry_deg)
        c, s = math.cos(r), math.sin(r)
        x, z = x * c + z * s, -x * s + z * c
    if rz_deg:
        r = math.radians(rz_deg)
        c, s = math.cos(r), math.sin(r)
        x, y = x * c - y * s, x * s + y * c
    return (x, y, z)


def euler_to_quat(rx_deg=0.0, ry_deg=0.0, rz_deg=0.0):
    cx, sx = math.cos(math.radians(rx_deg) * 0.5), math.sin(math.radians(rx_deg) * 0.5)
    cy, sy = math.cos(math.radians(ry_deg) * 0.5), math.sin(math.radians(ry_deg) * 0.5)
    cz, sz = math.cos(math.radians(rz_deg) * 0.5), math.sin(math.radians(rz_deg) * 0.5)
    qx = sx * cy * cz + cx * sy * sz
    qy = cx * sy * cz - sx * cy * sz
    qz = cx * cy * sz + sx * sy * cz
    qw = cx * cy * cz - sx * sy * sz
    l = math.sqrt(qx * qx + qy * qy + qz * qz + qw * qw)
    return [qx / l, qy / l, qz / l, qw / l]


def basis_from_dir(d):
    dn = v_norm(d)
    up = (0.0, 1.0, 0.0) if abs(dn[1]) < 0.92 else (1.0, 0.0, 0.0)
    u = v_norm(v_cross(up, dn))
    v = v_norm(v_cross(dn, u))
    return u, v, dn


class PrimBuilder:
    def __init__(self, material_idx: int):
        self.material = material_idx
        self.pos = []
        self.nrm = []
        self.uv = []
        self.col = []
        self.idx = []

    def add_vert(self, p, n, t, c=(1.0, 1.0, 1.0)) -> int:
        vi = len(self.pos) // 3
        self.pos.extend(p)
        self.nrm.extend(n)
        self.uv.extend(t)
        self.col.extend(c)
        return vi

    def add_tri(self, p0, p1, p2, uv0=(0.0, 0.0), uv1=(1.0, 0.0), uv2=(0.5, 1.0),
                c0=(1.0, 1.0, 1.0), c1=None, c2=None, two_sided=False):
        if c1 is None:
            c1 = c0
        if c2 is None:
            c2 = c0
        n = v_norm(v_cross(v_sub(p1, p0), v_sub(p2, p0)))
        i0 = self.add_vert(p0, n, uv0, c0)
        i1 = self.add_vert(p1, n, uv1, c1)
        i2 = self.add_vert(p2, n, uv2, c2)
        self.idx.extend([i0, i1, i2])
        if two_sided:
            nn = (-n[0], -n[1], -n[2])
            j0 = self.add_vert(p0, nn, uv0, c0)
            j2 = self.add_vert(p2, nn, uv2, c2)
            j1 = self.add_vert(p1, nn, uv1, c1)
            self.idx.extend([j0, j2, j1])

    def add_quad(self, p0, p1, p2, p3,
                 uv0=(0.0, 0.0), uv1=(1.0, 0.0), uv2=(1.0, 1.0), uv3=(0.0, 1.0),
                 c0=(1.0, 1.0, 1.0), c1=None, c2=None, c3=None, two_sided=False):
        if c1 is None:
            c1 = c0
        if c2 is None:
            c2 = c1
        if c3 is None:
            c3 = c0
        n = v_norm(v_cross(v_sub(p1, p0), v_sub(p2, p0)))
        i0 = self.add_vert(p0, n, uv0, c0)
        i1 = self.add_vert(p1, n, uv1, c1)
        i2 = self.add_vert(p2, n, uv2, c2)
        i3 = self.add_vert(p3, n, uv3, c3)
        self.idx.extend([i0, i1, i2, i2, i3, i0])
        if two_sided:
            nn = (-n[0], -n[1], -n[2])
            j0 = self.add_vert(p0, nn, uv0, c0)
            j3 = self.add_vert(p3, nn, uv3, c3)
            j2 = self.add_vert(p2, nn, uv2, c2)
            j1 = self.add_vert(p1, nn, uv1, c1)
            self.idx.extend([j0, j3, j2, j2, j1, j0])


class MeshBuilder:
    def __init__(self, name: str):
        self.name = name
        self.prims = {}

    def prim(self, mat_idx: int) -> PrimBuilder:
        if mat_idx not in self.prims:
            self.prims[mat_idx] = PrimBuilder(mat_idx)
        return self.prims[mat_idx]

    def add_bevel_box(self, mat: int, center, size, bevel: float = 0.02,
                      rot=(0.0, 0.0, 0.0), taper_top=(1.0, 1.0),
                      uv_rect=(0.0, 0.0, 1.0, 1.0), col=(1.0, 1.0, 1.0)):
        pb = self.prim(mat)
        hx, hy, hz = size[0] * 0.5, size[1] * 0.5, size[2] * 0.5
        bv = min(bevel, hx * 0.38, hy * 0.38, hz * 0.38)
        tx, tz = taper_top

        def tf(p):
            return v_add(center, rot_xyz(p, rot[0], rot[1], rot[2]))

        u0, v0, u1, v1 = uv_rect
        rings = [
            (-hy, max(0.002, hx - bv), max(0.002, hz - bv), 0.88),
            (-hy + bv, hx, hz, 0.96),
            (hy - bv, hx * tx, hz * tz, 1.00),
            (hy, max(0.002, hx * tx - bv), max(0.002, hz * tz - bv), 1.04),
        ]

        def ring_pts(y, rx, rz):
            return [
                tf((-rx, y, +rz)),
                tf((+rx, y, +rz)),
                tf((+rx, y, -rz)),
                tf((-rx, y, -rz)),
            ]

        r_pts = [ring_pts(y, rx, rz) for (y, rx, rz, _) in rings]
        v_bands = [v1, v0 + (v1 - v0) * 0.85, v0 + (v1 - v0) * 0.15, v0]
        for bi in range(3):
            pa, pb_ring = r_pts[bi], r_pts[bi + 1]
            ca = tuple(min(1.0, c * rings[bi][3]) for c in col)
            cb = tuple(min(1.0, c * rings[bi + 1][3]) for c in col)
            va, vb = v_bands[bi], v_bands[bi + 1]
            for s in range(4):
                sn = (s + 1) % 4
                pb.add_quad(
                    pa[s], pa[sn], pb_ring[sn], pb_ring[s],
                    (u0, va), (u1, va), (u1, vb), (u0, vb),
                    ca, ca, cb, cb
                )
        ctop = tuple(min(1.0, c * 1.05) for c in col)
        t = r_pts[3]
        pb.add_quad(t[0], t[1], t[2], t[3], (u0, v1), (u1, v1), (u1, v0), (u0, v0), ctop)
        cbot = tuple(c * 0.80 for c in col)
        b = r_pts[0]
        pb.add_quad(b[3], b[2], b[1], b[0], (u0, v0), (u1, v0), (u1, v1), (u0, v1), cbot)

    def add_crystal_spire(self, mat: int, base, tip, r_base: float, r_mid: float = None,
                          sides: int = 6, split_u: float = 0.66, twist_deg: float = 0.0,
                          double_ended: bool = False, two_sided: bool = False,
                          col_base=(0.74, 0.93, 0.98), col_tip=(1.0, 1.0, 1.0)):
        """True cut-gemstone hexagonal crystal prism + pyramid apex with crisp per-facet contrast!"""
        pb = self.prim(mat)
        if r_mid is None:
            r_mid = r_base * 0.82
        axis = v_sub(tip, base)
        ux, vx, _ = basis_from_dir(axis)
        mid = v_lerp(base, tip, split_u)
        tw = math.radians(twist_deg)

        ring0 = []
        ring1 = []
        for i in range(sides):
            asym = 1.0 + 0.07 * math.cos(i * 2.0 + 0.4)
            a0 = (math.tau * i) / sides
            a1 = a0 + tw
            p0 = v_add(base, v_add(v_mul(ux, math.cos(a0) * r_base * asym), v_mul(vx, math.sin(a0) * r_base * asym)))
            p1 = v_add(mid, v_add(v_mul(ux, math.cos(a1) * r_mid * asym), v_mul(vx, math.sin(a1) * r_mid * asym)))
            ring0.append(p0)
            ring1.append(p1)

        # Distinct per-facet lighting tint so even fullbright/emissive crystals show crisp 3D gem facets!
        facet_shades = [1.04, 0.84, 0.96, 0.78, 0.98, 0.86, 1.02, 0.82]
        for i in range(sides):
            j = (i + 1) % sides
            u0, u1 = i / sides, (i + 1) / sides
            fs = facet_shades[i % len(facet_shades)]
            cb = tuple(min(1.0, c * fs) for c in col_base)
            cm = tuple(min(1.0, c * fs) for c in v_lerp(col_base, col_tip, 0.64))
            ct = tuple(min(1.0, c * (0.90 + 0.10 * fs)) for c in col_tip)

            pb.add_quad(
                ring0[i], ring0[j], ring1[j], ring1[i],
                (u0, 0.12), (u1, 0.12), (u1, 0.70), (u0, 0.70),
                cb, cb, cm, cm, two_sided=two_sided
            )
            pb.add_tri(
                ring1[i], ring1[j], tip,
                (u0, 0.70), (u1, 0.70), ((u0 + u1) * 0.5, 0.99),
                cm, cm, ct, two_sided=two_sided
            )
            if double_ended:
                back_tip = v_sub(base, v_mul(axis, 0.30))
                pb.add_tri(
                    ring0[j], ring0[i], back_tip,
                    (u1, 0.12), (u0, 0.12), ((u0 + u1) * 0.5, 0.98),
                    cb, cb, ct, two_sided=two_sided
                )
            else:
                pb.add_tri(
                    ring0[j], ring0[i], base,
                    (u1, 0.12), (u0, 0.12), (0.5, 0.02),
                    cb, cb, cb, two_sided=two_sided
                )

    def add_octahedron(self, mat_face: int, mat_wire: int, center, radii, rot=(0.0, 0.0, 0.0),
                       wire_thick: float = 0.026, two_sided: bool = True):
        rx, ry, rz = radii

        def tf(p):
            return v_add(center, rot_xyz(p, rot[0], rot[1], rot[2]))

        top = tf((0.0, +ry, 0.0))
        bot = tf((0.0, -ry, 0.0))
        eq = [
            tf((0.0, 0.0, +rz)),
            tf((+rx, 0.0, 0.0)),
            tf((0.0, 0.0, -rz)),
            tf((-rx, 0.0, 0.0)),
        ]
        pf = self.prim(mat_face)
        fsh = [1.0, 0.84, 0.96, 0.80]
        for i in range(4):
            j = (i + 1) % 4
            s = fsh[i]
            pf.add_tri(eq[i], eq[j], top, (0.05, 0.45), (0.30, 0.45), (0.18, 0.96),
                       (0.78 * s, 0.96 * s, 1.0), (0.78 * s, 0.96 * s, 1.0), (1.0, 1.0, 1.0), two_sided=two_sided)
            pf.add_tri(eq[j], eq[i], bot, (0.30, 0.45), (0.05, 0.45), (0.18, 0.96),
                       (0.74 * s, 0.93 * s, 0.98), (0.74 * s, 0.93 * s, 0.98), (0.95, 1.0, 1.0), two_sided=two_sided)

        if mat_wire is not None and wire_thick > 0:
            edges = []
            for i in range(4):
                edges.append((top, eq[i]))
                edges.append((bot, eq[i]))
                edges.append((eq[i], eq[(i + 1) % 4]))
            for a, b in edges:
                self.add_rod(mat_wire, a, b, wire_thick, sides=4, uv_rect=(0.1, 0.88, 0.9, 0.98))

    def add_rod(self, mat: int, a, b, radius: float, sides: int = 6,
                r_end: float = None, uv_rect=(0.55, 0.55, 0.95, 0.95), col=(1.0, 1.0, 1.0)):
        if r_end is None:
            r_end = radius
        pb = self.prim(mat)
        axis = v_sub(b, a)
        ux, vx, _ = basis_from_dir(axis)
        u0, v0, u1, v1 = uv_rect
        ra = []
        rb = []
        for i in range(sides):
            ang = (math.tau * i) / sides
            ca, sa = math.cos(ang), math.sin(ang)
            ra.append(v_add(a, v_add(v_mul(ux, ca * radius), v_mul(vx, sa * radius))))
            rb.append(v_add(b, v_add(v_mul(ux, ca * r_end), v_mul(vx, sa * r_end))))
        for i in range(sides):
            j = (i + 1) % sides
            pb.add_quad(ra[i], ra[j], rb[j], rb[i], (u0, v0), (u1, v0), (u1, v1), (u0, v1), col)
            pb.add_tri(ra[j], ra[i], a, (u1, v0), (u0, v0), ((u0 + u1) * 0.5, v0), col)
            pb.add_tri(rb[i], rb[j], b, (u0, v1), (u1, v1), ((u0 + u1) * 0.5, v1), col)

    def add_carved_rune_ring(self, mat_stone: int, mat_rune: int, mat_gold: int,
                             r_in: float, r_out: float, height: float, segments: int = 24,
                             axis: str = "Y", outer_cogs: bool = False, cog_every: int = 1,
                             rune_outer: bool = True, rune_inner: bool = False, rune_sides: bool = False,
                             framed_band: bool = False):
        ps = self.prim(mat_stone)
        pr = self.prim(mat_rune)
        hh = height * 0.5
        gap = 0.011

        def map_ax(x, y, z):
            if axis == "Y":
                return (x, y, z)
            return (x, z, y)

        def emit_oriented_quad(pb, q0, q1, q2, q3, t0, t1, t2, t3, outward_ref, col=(1.0, 1.0, 1.0)):
            n = v_cross(v_sub(q1, q0), v_sub(q2, q0))
            if n[0] * outward_ref[0] + n[1] * outward_ref[1] + n[2] * outward_ref[2] >= 0:
                pb.add_quad(q0, q1, q2, q3, t0, t1, t2, t3, col)
            else:
                pb.add_quad(q0, q3, q2, q1, t0, t3, t2, t1, col)

        for i in range(segments):
            a0 = (math.tau * i) / segments + gap
            a1 = (math.tau * (i + 1)) / segments - gap
            c0, s0 = math.cos(a0), math.sin(a0)
            c1, s1 = math.cos(a1), math.sin(a1)

            p_in_b0 = map_ax(c0 * r_in, -hh, s0 * r_in)
            p_in_b1 = map_ax(c1 * r_in, -hh, s1 * r_in)
            p_in_t1 = map_ax(c1 * r_in, +hh, s1 * r_in)
            p_in_t0 = map_ax(c0 * r_in, +hh, s0 * r_in)

            p_out_b0 = map_ax(c0 * r_out, -hh, s0 * r_out)
            p_out_b1 = map_ax(c1 * r_out, -hh, s1 * r_out)
            p_out_t1 = map_ax(c1 * r_out, +hh, s1 * r_out)
            p_out_t0 = map_ax(c0 * r_out, +hh, s0 * r_out)

            am = (a0 + a1) * 0.5
            out_dir = map_ax(math.cos(am), 0.0, math.sin(am))
            in_dir = (-out_dir[0], -out_dir[1], -out_dir[2])
            up_dir = map_ax(0.0, 1.0, 0.0)
            dn_dir = map_ax(0.0, -1.0, 0.0)
            tan_dir = map_ax(-math.sin(am), 0.0, math.cos(am))
            ntan_dir = (-tan_dir[0], -tan_dir[1], -tan_dir[2])

            shade = 0.94 + 0.05 * ((i * 7) % 3 - 1)
            scol = (shade, shade, shade)
            emit_oriented_quad(ps, p_out_b0, p_out_b1, p_out_t1, p_out_t0, (0, 0), (1, 0), (1, 1), (0, 1), out_dir, scol)
            emit_oriented_quad(ps, p_in_b0, p_in_b1, p_in_t1, p_in_t0, (0, 0), (1, 0), (1, 1), (0, 1), in_dir, scol)
            emit_oriented_quad(ps, p_in_t0, p_out_t0, p_out_t1, p_in_t1, (0, 0), (1, 0), (1, 1), (0, 1), up_dir, scol)
            emit_oriented_quad(ps, p_in_b0, p_out_b0, p_out_b1, p_in_b1, (0, 0), (1, 0), (1, 1), (0, 1), dn_dir, scol)
            emit_oriented_quad(ps, p_in_b1, p_out_b1, p_out_t1, p_in_t1, (0, 0), (0.5, 0), (0.5, 1), (0, 1), tan_dir, (0.82, 0.84, 0.86))
            emit_oriented_quad(ps, p_in_b0, p_out_b0, p_out_t0, p_in_t0, (0, 0), (0.5, 0), (0.5, 1), (0, 1), ntan_dir, (0.82, 0.84, 0.86))

            if outer_cogs and (i % cog_every == 0):
                ac0 = am - (math.tau / segments) * 0.25
                ac1 = am + (math.tau / segments) * 0.25
                rc = r_out + 0.11
                hc = hh * 0.74
                cc0, sc0 = math.cos(ac0), math.sin(ac0)
                cc1, sc1 = math.cos(ac1), math.sin(ac1)
                cb0 = map_ax(cc0 * (r_out - 0.02), -hc, sc0 * (r_out - 0.02))
                cb1 = map_ax(cc1 * (r_out - 0.02), -hc, sc1 * (r_out - 0.02))
                ct1 = map_ax(cc1 * (r_out - 0.02), +hc, sc1 * (r_out - 0.02))
                ct0 = map_ax(cc0 * (r_out - 0.02), +hc, sc0 * (r_out - 0.02))
                ob0 = map_ax(cc0 * rc, -hc * 0.82, sc0 * rc)
                ob1 = map_ax(cc1 * rc, -hc * 0.82, sc1 * rc)
                ot1 = map_ax(cc1 * rc, +hc * 0.82, sc1 * rc)
                ot0 = map_ax(cc0 * rc, +hc * 0.82, sc0 * rc)
                emit_oriented_quad(ps, ob0, ob1, ot1, ot0, (0, 0), (0.5, 0), (0.5, 0.5), (0, 0.5), out_dir, scol)
                emit_oriented_quad(ps, ct0, ot0, ot1, ct1, (0, 0), (0.5, 0), (0.5, 0.5), (0, 0.5), up_dir, scol)
                emit_oriented_quad(ps, cb0, ob0, ob1, cb1, (0, 0), (0.5, 0), (0.5, 0.5), (0, 0.5), dn_dir, scol)
                emit_oriented_quad(ps, cb1, ob1, ot1, ct1, (0, 0), (0.5, 0), (0.5, 0.5), (0, 0.5), tan_dir, scol)
                emit_oriented_quad(ps, cb0, ob0, ot0, ct0, (0, 0), (0.5, 0), (0.5, 0.5), (0, 0.5), ntan_dir, scol)

            gcol = i % 8
            grow = 0 if framed_band else (1 + (i // 8) % 2)
            gu0, gu1 = gcol / 8.0, (gcol + 1) / 8.0
            gv0, gv1 = (grow * 64) / 256.0, ((grow + 1) * 64) / 256.0

            if rune_outer:
                ro = r_out + 0.006
                hr = hh * 0.92
                q0 = map_ax(c0 * ro, -hr, s0 * ro)
                q1 = map_ax(c1 * ro, -hr, s1 * ro)
                q2 = map_ax(c1 * ro, +hr, s1 * ro)
                q3 = map_ax(c0 * ro, +hr, s0 * ro)
                emit_oriented_quad(pr, q0, q1, q2, q3, (gu0, gv1), (gu1, gv1), (gu1, gv0), (gu0, gv0), out_dir)

            if rune_inner:
                ri = r_in - 0.006
                hr = hh * 0.92
                q0 = map_ax(c0 * ri, -hr, s0 * ri)
                q1 = map_ax(c1 * ri, -hr, s1 * ri)
                q2 = map_ax(c1 * ri, +hr, s1 * ri)
                q3 = map_ax(c0 * ri, +hr, s0 * ri)
                emit_oriented_quad(pr, q0, q1, q2, q3, (gu0, gv1), (gu1, gv1), (gu1, gv0), (gu0, gv0), in_dir)

            if rune_sides:
                r_lo = r_in + (r_out - r_in) * 0.14
                r_hi = r_out - (r_out - r_in) * 0.10
                for sgn, ref_d in ((+1.0, up_dir), (-1.0, dn_dir)):
                    zf = sgn * (hh + 0.006)
                    q0 = map_ax(c0 * r_lo, zf, s0 * r_lo)
                    q1 = map_ax(c1 * r_lo, zf, s1 * r_lo)
                    q2 = map_ax(c1 * r_hi, zf, s1 * r_hi)
                    q3 = map_ax(c0 * r_hi, zf, s0 * r_hi)
                    emit_oriented_quad(pr, q0, q1, q2, q3, (gu0, gv1), (gu1, gv1), (gu1, gv0), (gu0, gv0), ref_d)
                    rt0, rt1 = r_in + 0.01, r_in + 0.055
                    t0 = map_ax(c0 * rt0, zf, s0 * rt0)
                    t1 = map_ax(c1 * rt0, zf, s1 * rt0)
                    t2 = map_ax(c1 * rt1, zf, s1 * rt1)
                    t3 = map_ax(c0 * rt1, zf, s0 * rt1)
                    emit_oriented_quad(pr, t0, t1, t2, t3, (0.0, 0.96), (1.0, 0.96), (1.0, 0.90), (0.0, 0.90), ref_d)


# ==============================================================================
# 3. BINARY glTF 2.0 (.GLB) COMPILER (WITH UNUSED MATERIAL/IMAGE PRUNING!)
# ==============================================================================
class GlbScene:
    def __init__(self):
        self.images = []
        self.materials = []
        self.meshes = []
        self.nodes = []
        self.roots = []

    def add_image(self, png_bytes: bytes) -> int:
        idx = len(self.images)
        self.images.append(png_bytes)
        return idx

    def add_material(self, name: str, image_idx: int = None, color=(1.0, 1.0, 1.0, 1.0),
                     emissive: bool = False, alpha_mode: str = None, double_sided: bool = True) -> int:
        idx = len(self.materials)
        pbr = {
            "baseColorFactor": [float(c) for c in color],
            "metallicFactor": 0.1,
            "roughnessFactor": 0.8,
        }
        if image_idx is not None:
            pbr["baseColorTexture"] = {"index": image_idx}
        mat = {
            "name": name,
            "doubleSided": double_sided,
            "pbrMetallicRoughness": pbr,
        }
        if emissive:
            mat["emissiveFactor"] = [1.0, 1.0, 1.0]
        if alpha_mode:
            mat["alphaMode"] = alpha_mode
            if alpha_mode == "MASK":
                mat["alphaCutoff"] = 0.25
        self.materials.append(mat)
        return idx

    def add_mesh(self, mb: MeshBuilder) -> int:
        idx = len(self.meshes)
        self.meshes.append(mb)
        return idx

    def add_node(self, name: str, mesh_idx: int = None, translation=(0.0, 0.0, 0.0),
                 rotation_euler=(0.0, 0.0, 0.0), scale=(1.0, 1.0, 1.0), parent: int = None) -> int:
        idx = len(self.nodes)
        n = {"name": name}
        if mesh_idx is not None:
            n["mesh"] = mesh_idx
        if any(abs(v) > 1e-6 for v in translation):
            n["translation"] = [float(v) for v in translation]
        if any(abs(v) > 1e-6 for v in rotation_euler):
            n["rotation"] = euler_to_quat(*rotation_euler)
        if any(abs(v - 1.0) > 1e-6 for v in scale):
            n["scale"] = [float(v) for v in scale]
        self.nodes.append(n)
        if parent is None:
            self.roots.append(idx)
        else:
            self.nodes[parent].setdefault("children", []).append(idx)
        return idx

    def to_glb_bytes(self) -> bytes:
        # Prune unused materials & images so each .glb is super compact!
        used_mats = sorted({
            mi for mb in self.meshes for mi, p in mb.prims.items() if p.idx
        })
        mat_remap = {old_mi: new_mi for new_mi, old_mi in enumerate(used_mats)}
        used_imgs = []
        img_remap = {}
        out_materials = []
        for old_mi in used_mats:
            m = json.loads(json.dumps(self.materials[old_mi]))
            pbr = m["pbrMetallicRoughness"]
            if "baseColorTexture" in pbr:
                old_ii = pbr["baseColorTexture"]["index"]
                if old_ii not in img_remap:
                    img_remap[old_ii] = len(used_imgs)
                    used_imgs.append(self.images[old_ii])
                pbr["baseColorTexture"]["index"] = img_remap[old_ii]
            out_materials.append(m)

        bin_buf = bytearray()
        buffer_views = []
        accessors = []

        def append_bin(raw: bytes, target: int = None) -> int:
            while len(bin_buf) % 4 != 0:
                bin_buf.append(0)
            off = len(bin_buf)
            bin_buf.extend(raw)
            bv = {"buffer": 0, "byteOffset": off, "byteLength": len(raw)}
            if target is not None:
                bv["target"] = target
            bvi = len(buffer_views)
            buffer_views.append(bv)
            return bvi

        def add_float_acc(vals, comp_count: int, acc_type: str, target: int = 34962) -> int:
            raw = struct.pack(f"<{len(vals)}f", *vals)
            bvi = append_bin(raw, target)
            cnt = len(vals) // comp_count
            acc = {
                "bufferView": bvi,
                "componentType": 5126,
                "count": cnt,
                "type": acc_type,
            }
            if acc_type == "VEC3" and cnt > 0:
                xs, ys, zs = vals[0::3], vals[1::3], vals[2::3]
                acc["min"] = [min(xs), min(ys), min(zs)]
                acc["max"] = [max(xs), max(ys), max(zs)]
            ai = len(accessors)
            accessors.append(acc)
            return ai

        def add_idx_acc(indices) -> int:
            max_i = max(indices) if indices else 0
            if max_i < 65535:
                raw = struct.pack(f"<{len(indices)}H", *indices)
                ct = 5123
            else:
                raw = struct.pack(f"<{len(indices)}I", *indices)
                ct = 5125
            bvi = append_bin(raw, 34963)
            ai = len(accessors)
            accessors.append({
                "bufferView": bvi,
                "componentType": ct,
                "count": len(indices),
                "type": "SCALAR",
            })
            return ai

        gltf_images = []
        gltf_textures = []
        for i, png in enumerate(used_imgs):
            bvi = append_bin(png)
            gltf_images.append({"bufferView": bvi, "mimeType": "image/png"})
            gltf_textures.append({"source": i})

        gltf_meshes = []
        for mb in self.meshes:
            prims_json = []
            for mat_idx in sorted(mb.prims.keys()):
                p = mb.prims[mat_idx]
                if not p.idx:
                    continue
                pos_ai = add_float_acc(p.pos, 3, "VEC3")
                nrm_ai = add_float_acc(p.nrm, 3, "VEC3")
                uv_ai = add_float_acc(p.uv, 2, "VEC2")
                col_ai = add_float_acc(p.col, 3, "VEC3")
                idx_ai = add_idx_acc(p.idx)
                prims_json.append({
                    "attributes": {
                        "POSITION": pos_ai,
                        "NORMAL": nrm_ai,
                        "TEXCOORD_0": uv_ai,
                        "COLOR_0": col_ai,
                    },
                    "indices": idx_ai,
                    "material": mat_remap[mat_idx],
                    "mode": 4,
                })
            gltf_meshes.append({"name": mb.name, "primitives": prims_json})

        while len(bin_buf) % 4 != 0:
            bin_buf.append(0)

        doc = {
            "asset": {"version": "2.0", "generator": "riftworld-colonnade-glb-v2"},
            "scene": 0,
            "scenes": [{"nodes": self.roots}],
            "nodes": self.nodes,
            "meshes": gltf_meshes,
            "materials": out_materials,
            "textures": gltf_textures,
            "images": gltf_images,
            "accessors": accessors,
            "bufferViews": buffer_views,
            "buffers": [{"byteLength": len(bin_buf)}],
        }
        json_bytes = json.dumps(doc, separators=(",", ":")).encode("utf-8")
        while len(json_bytes) % 4 != 0:
            json_bytes += b" "

        total_len = 12 + 8 + len(json_bytes) + 8 + len(bin_buf)
        header = struct.pack("<III", 0x46546C67, 2, total_len)
        j_chunk = struct.pack("<II", len(json_bytes), 0x4E4F534A) + json_bytes
        b_chunk = struct.pack("<II", len(bin_buf), 0x004E4942) + bytes(bin_buf)
        return header + j_chunk + b_chunk


def setup_palette(sc: GlbScene, shared_pngs: dict) -> dict:
    im_marble = sc.add_image(shared_pngs["marble"])
    im_ashlar = sc.add_image(shared_pngs["ashlar"])
    im_gold = sc.add_image(shared_pngs["gold"])
    im_crys_op = sc.add_image(shared_pngs["crystal_op"])
    im_crys_tr = sc.add_image(shared_pngs["crystal_tr"])
    im_rune = sc.add_image(shared_pngs["rune"])
    im_wisp = sc.add_image(shared_pngs["wisp"])

    return {
        "marble": sc.add_material("marble", im_marble, (1.0, 1.0, 1.0, 1.0)),
        "ashlar": sc.add_material("ashlar", im_ashlar, (1.0, 1.0, 1.0, 1.0)),
        "fissure": sc.add_material("fissure", None, (0.10, 0.12, 0.15, 1.0)),
        "gold": sc.add_material("gold", im_gold, (1.0, 1.0, 1.0, 1.0)),
        "crystal_solid": sc.add_material("crystal_solid", im_crys_op, (1.0, 1.0, 1.0, 1.0)),
        "crystal_bright": sc.add_material("crystal_bright", im_crys_op, (0.90, 1.0, 1.0, 1.0), emissive=True),
        "rune_glow": sc.add_material("rune_glow", im_rune, (1.0, 1.0, 1.0, 1.0), emissive=True, alpha_mode="MASK"),
        "eye_glow": sc.add_material("eye_glow", None, (0.44, 1.0, 1.0, 1.0), emissive=True),
        "core_white": sc.add_material("core_white", None, (0.86, 1.0, 1.0, 1.0), emissive=True),
        "crystal_glass": sc.add_material("crystal_glass", im_crys_tr, (0.82, 0.99, 1.0, 0.78), emissive=True, alpha_mode="BLEND"),
        "wisp_blend": sc.add_material("wisp_blend", im_wisp, (0.68, 0.99, 1.0, 0.80), emissive=True, alpha_mode="BLEND"),
    }


# ==============================================================================
# 4. SHARED KATAR GAUNTLET-BLADE BUILDER (Minion Claws & Katar Weapon!)
# ==============================================================================
def build_katar_claw_mesh(mb: MeshBuilder, mats: dict, scale: float = 1.0):
    s = scale

    def pt(x, y, z):
        return (x * s, y * s, z * s)

    # 1. Sculpted multi-plate cracked white-marble gauntlet cuff around origin
    mb.add_bevel_box(mats["fissure"], pt(0, 0, 0.01), pt(0.26, 0.24, 0.27), bevel=0.02)
    mb.add_bevel_box(mats["marble"], pt(-0.075, +0.095, 0.02), pt(0.135, 0.10, 0.30), bevel=0.024, rot=(4, 0, 10))
    mb.add_bevel_box(mats["marble"], pt(+0.075, +0.095, 0.02), pt(0.135, 0.10, 0.30), bevel=0.024, rot=(4, 0, -10))
    mb.add_bevel_box(mats["marble"], pt(-0.145, -0.005, 0.01), pt(0.08, 0.18, 0.28), bevel=0.022, rot=(0, -5, -14))
    mb.add_bevel_box(mats["marble"], pt(+0.145, -0.005, 0.01), pt(0.08, 0.18, 0.28), bevel=0.022, rot=(0, +5, +14))
    mb.add_bevel_box(mats["marble"], pt(0.0, -0.10, 0.01), pt(0.24, 0.08, 0.26), bevel=0.020)
    mb.add_bevel_box(mats["marble"], pt(-0.07, +0.04, 0.16), pt(0.12, 0.13, 0.09), bevel=0.018, rot=(-12, -8, 0))
    mb.add_bevel_box(mats["marble"], pt(+0.07, +0.04, 0.16), pt(0.12, 0.13, 0.09), bevel=0.018, rot=(-12, +8, 0))
    mb.add_bevel_box(mats["eye_glow"], pt(0.0, +0.105, 0.03), pt(0.018, 0.04, 0.24), bevel=0.004)
    mb.add_bevel_box(mats["eye_glow"], pt(0.0, 0.0, 0.165), pt(0.20, 0.15, 0.02), bevel=0.004)

    # 2. Rear faceted aquamarine crystal counterweight spurs
    mb.add_crystal_spire(mats["crystal_solid"], pt(0.0, +0.05, -0.10), pt(0.0, +0.19, -0.35), 0.088 * s, 0.056 * s, sides=6)
    mb.add_crystal_spire(mats["crystal_solid"], pt(-0.09, +0.02, -0.09), pt(-0.16, +0.12, -0.29), 0.066 * s, 0.042 * s, sides=6)
    mb.add_crystal_spire(mats["crystal_solid"], pt(+0.09, +0.02, -0.09), pt(+0.16, +0.12, -0.29), 0.066 * s, 0.042 * s, sides=6)

    # 3. Wide-shouldered razor-sharp diamond crystal katar blade (+Z = +0.11 .. +1.00)
    pb_solid = mb.prim(mats["crystal_solid"])
    sec = [
        (0.11, 0.135, 0.078, (0.72, 0.92, 0.97)),
        (0.30, 0.168, 0.088, (0.86, 0.97, 1.00)),
        (0.65, 0.106, 0.054, (0.95, 1.00, 1.00)),
    ]
    tip = pt(0.0, 0.0, 1.00)
    rings = []
    for z, w, th, col in sec:
        rings.append(([
            pt(+w, 0.0, z),
            pt(0.0, +th, z),
            pt(-w, 0.0, z),
            pt(0.0, -th, z),
        ], col))

    fsh = [1.04, 0.84, 0.96, 0.80]
    for ri in range(len(rings) - 1):
        r0, c0 = rings[ri]
        r1, c1 = rings[ri + 1]
        v0 = 0.16 + 0.54 * (ri / len(rings))
        v1 = 0.16 + 0.54 * ((ri + 1) / len(rings))
        for k in range(4):
            kn = (k + 1) % 4
            u0, u1 = k / 6.0, (k + 1) / 6.0
            ca = tuple(min(1.0, c * fsh[k]) for c in c0)
            cb = tuple(min(1.0, c * fsh[k]) for c in c1)
            pb_solid.add_quad(r0[k], r0[kn], r1[kn], r1[k], (u0, v0), (u1, v0), (u1, v1), (u0, v1), ca, ca, cb, cb)

    rlast, clast = rings[-1]
    for k in range(4):
        kn = (k + 1) % 4
        u0, u1 = k / 6.0, (k + 1) / 6.0
        ca = tuple(min(1.0, c * fsh[k]) for c in clast)
        pb_solid.add_tri(rlast[k], rlast[kn], tip, (u0, 0.72), (u1, 0.72), ((u0 + u1) * 0.5, 0.99), ca, ca, (1.0, 1.0, 1.0))

    mb.add_crystal_spire(mats["crystal_bright"], pt(0.0, 0.0, 0.14), pt(0.0, 0.0, 0.90), 0.046 * s, 0.026 * s, sides=4)
    mb.add_crystal_spire(mats["crystal_solid"], pt(-0.11, 0.0, 0.13), pt(-0.21, +0.02, 0.44), 0.048 * s, 0.028 * s, sides=5)
    mb.add_crystal_spire(mats["crystal_solid"], pt(+0.11, 0.0, 0.13), pt(+0.21, +0.02, 0.44), 0.048 * s, 0.028 * s, sides=5)


# ==============================================================================
# 5. CRYSTAL MINION (.GLB) — 1:1 TO 02_crystal_minions_ref.png!
# ==============================================================================
def generate_crystal_minion_glb(pngs: dict) -> bytes:
    sc = GlbScene()
    mats = setup_palette(sc, pngs)
    root = sc.add_node("root")
    body = sc.add_node("body", parent=root)

    # ---- A. SCULPTED 3D CRACKED PALE-MARBLE GOLEM VISAGE ----
    mb_visage = MeshBuilder("visage_mesh")
    mb_visage.add_bevel_box(mats["fissure"], (0.0, 1.30, 0.10), (0.64, 0.70, 0.34), bevel=0.03)
    # Glowing cyan rift core shining out from behind the cracked mask & collar!
    mb_visage.add_bevel_box(mats["eye_glow"], (0.0, 1.14, -0.01), (0.42, 0.52, 0.24), bevel=0.03)
    # Subtle glowing cyan lightning fissure inside the central forehead & jaw gap
    mb_visage.add_bevel_box(mats["eye_glow"], (0.0, 1.54, 0.20), (0.018, 0.24, 0.06), bevel=0.004)

    # Fierce slanted triangular/almond glowing cyan eyes blazing clearly under the stone scowl!
    peye = mb_visage.prim(mats["core_white"])
    peye_rim = mb_visage.prim(mats["eye_glow"])
    for sgn in (-1.0, +1.0):
        e0 = (sgn * 0.080, 1.262, 0.318)
        e1 = (sgn * 0.262, 1.308, 0.294)
        e2 = (sgn * 0.250, 1.362, 0.294)
        e3 = (sgn * 0.090, 1.314, 0.318)
        if sgn > 0:
            peye_rim.add_quad(e0, e1, e2, e3, two_sided=True)
        else:
            peye_rim.add_quad(e1, e0, e3, e2, two_sided=True)
        i0 = (sgn * 0.100, 1.274, 0.322)
        i1 = (sgn * 0.236, 1.312, 0.300)
        i2 = (sgn * 0.226, 1.346, 0.300)
        i3 = (sgn * 0.108, 1.304, 0.322)
        if sgn > 0:
            peye.add_quad(i0, i1, i2, i3, two_sided=True)
        else:
            peye.add_quad(i1, i0, i3, i2, two_sided=True)

    # Forehead plates split down the middle by a jagged fissure + central keystone wedge
    mb_visage.add_bevel_box(mats["marble"], (-0.105, 1.56, 0.255), (0.18, 0.28, 0.14), bevel=0.026, rot=(-6, +7, +4))
    mb_visage.add_bevel_box(mats["marble"], (+0.105, 1.56, 0.255), (0.18, 0.28, 0.14), bevel=0.026, rot=(-6, -7, -4))
    mb_visage.add_bevel_box(mats["marble"], (-0.255, 1.53, 0.215), (0.15, 0.26, 0.15), bevel=0.024, rot=(-4, +18, +8))
    mb_visage.add_bevel_box(mats["marble"], (+0.255, 1.53, 0.215), (0.15, 0.26, 0.15), bevel=0.024, rot=(-4, -18, -8))
    mb_visage.add_bevel_box(mats["marble"], (0.0, 1.67, 0.25), (0.11, 0.13, 0.13), bevel=0.020, taper_top=(0.72, 0.82))

    # Swept-up chiseled pale-marble corner horn crests
    mb_visage.add_bevel_box(mats["marble"], (-0.355, 1.66, 0.19), (0.13, 0.27, 0.16), bevel=0.024,
                            rot=(-10, +22, +18), taper_top=(0.52, 0.62))
    mb_visage.add_bevel_box(mats["marble"], (+0.355, 1.66, 0.19), (0.13, 0.27, 0.16), bevel=0.024,
                            rot=(-10, -22, -18), taper_top=(0.52, 0.62))

    # Heavy scowling V-angled stone brow ridges overhanging the glowing cyan eyes
    mb_visage.add_bevel_box(mats["marble"], (-0.175, 1.385, 0.308), (0.25, 0.086, 0.14), bevel=0.020, rot=(+10, +10, -17))
    mb_visage.add_bevel_box(mats["marble"], (+0.175, 1.385, 0.308), (0.25, 0.086, 0.14), bevel=0.020, rot=(+10, -10, +17))

    # Sculpted central stone nose bridge & nasal tip
    mb_visage.add_bevel_box(mats["marble"], (0.0, 1.24, 0.328), (0.095, 0.24, 0.12), bevel=0.020, rot=(-5, 0, 0), taper_top=(0.76, 0.85))
    mb_visage.add_bevel_box(mats["marble"], (0.0, 1.13, 0.348), (0.125, 0.09, 0.10), bevel=0.018)

    # Multi-layered cracked cheekbone & temple armor plates
    mb_visage.add_bevel_box(mats["marble"], (-0.20, 1.15, 0.278), (0.19, 0.20, 0.14), bevel=0.022, rot=(+4, +14, -8))
    mb_visage.add_bevel_box(mats["marble"], (+0.20, 1.15, 0.278), (0.19, 0.20, 0.14), bevel=0.022, rot=(+4, -14, +8))
    mb_visage.add_bevel_box(mats["marble"], (-0.345, 1.22, 0.185), (0.14, 0.30, 0.18), bevel=0.024, rot=(0, +28, -10))
    mb_visage.add_bevel_box(mats["marble"], (+0.345, 1.22, 0.185), (0.14, 0.30, 0.18), bevel=0.024, rot=(0, -28, +10))
    mb_visage.add_bevel_box(mats["marble"], (-0.28, 1.01, 0.215), (0.14, 0.16, 0.14), bevel=0.020, rot=(+8, +20, -18))
    mb_visage.add_bevel_box(mats["marble"], (+0.28, 1.01, 0.215), (0.14, 0.16, 0.14), bevel=0.020, rot=(+8, -20, +18))

    visage_node = sc.add_node("visage", sc.add_mesh(mb_visage), parent=body)

    # Segmented vertical stone teeth / jaw grille
    mb_jaw = MeshBuilder("jaw_mesh")
    for tx, th in zip([-0.17, -0.085, 0.0, +0.085, +0.17], [0.16, 0.20, 0.22, 0.20, 0.16]):
        zs = 0.295 - abs(tx) * 0.22
        mb_jaw.add_bevel_box(mats["marble"], (tx, 0.96 - (0.22 - th) * 0.35, zs), (0.068, th, 0.11), bevel=0.015)
    sc.add_node("jaw", sc.add_mesh(mb_jaw), parent=visage_node)

    # ---- B. RADIATING 23-SPIRE CUT-GEMSTONE AQUAMARINE CRYSTAL CROWN & HALO ----
    mb_crown = MeshBuilder("crystal_crown_mesh")
    crown_spires = [
        ((0.00, 1.48, -0.02), (0.00, 2.38, -0.14), 0.170, "crystal_solid"),
        ((-0.14, 1.46, -0.01), (-0.30, 2.28, -0.10), 0.150, "crystal_solid"),
        ((+0.14, 1.46, -0.01), (+0.30, 2.28, -0.10), 0.150, "crystal_solid"),
        ((-0.26, 1.40, -0.02), (-0.58, 2.10, -0.08), 0.138, "crystal_solid"),
        ((+0.26, 1.40, -0.02), (+0.58, 2.10, -0.08), 0.138, "crystal_solid"),
        ((-0.32, 1.30, -0.04), (-0.78, 1.84, -0.05), 0.125, "crystal_solid"),
        ((+0.32, 1.30, -0.04), (+0.78, 1.84, -0.05), 0.125, "crystal_solid"),
        ((-0.32, 1.18, -0.05), (-0.76, 1.50, -0.10), 0.112, "crystal_solid"),
        ((+0.32, 1.18, -0.05), (+0.76, 1.50, -0.10), 0.112, "crystal_solid"),
        ((-0.30, 1.06, -0.04), (-0.64, 1.20, -0.10), 0.096, "crystal_solid"),
        ((+0.30, 1.06, -0.04), (+0.64, 1.20, -0.10), 0.096, "crystal_solid"),
        # Second staggered dorsal row & glowing inner crystal highlights
        ((-0.08, 1.50, -0.12), (-0.15, 2.20, -0.35), 0.138, "crystal_bright"),
        ((+0.08, 1.50, -0.12), (+0.15, 2.20, -0.35), 0.138, "crystal_bright"),
        ((0.00, 1.34, -0.12), (0.00, 1.88, -0.54), 0.148, "crystal_solid"),
        ((-0.20, 1.28, -0.12), (-0.44, 1.70, -0.45), 0.122, "crystal_solid"),
        ((+0.20, 1.28, -0.12), (+0.44, 1.70, -0.45), 0.122, "crystal_solid"),
        ((-0.18, 1.06, -0.10), (-0.38, 1.24, -0.44), 0.108, "crystal_bright"),
        ((+0.18, 1.06, -0.10), (+0.38, 1.24, -0.44), 0.108, "crystal_bright"),
        # Downward crystal stalactites around the lower energy core
        ((0.00, 0.96, -0.04), (0.00, 0.40, -0.10), 0.140, "crystal_bright"),
        ((-0.18, 0.98, 0.02), (-0.32, 0.52, 0.04), 0.108, "crystal_solid"),
        ((+0.18, 0.98, 0.02), (+0.32, 0.52, 0.04), 0.108, "crystal_solid"),
        ((-0.12, 0.92, -0.08), (-0.22, 0.46, -0.22), 0.096, "crystal_solid"),
        ((+0.12, 0.92, -0.08), (+0.22, 0.46, -0.22), 0.096, "crystal_solid"),
    ]
    for b_pt, t_pt, rb, mk in crown_spires:
        mb_crown.add_crystal_spire(mats[mk], b_pt, t_pt, rb, rb * 0.80, sides=6, split_u=0.64)
        if rb >= 0.138:
            t_outer = v_add(t_pt, v_mul(v_norm(v_sub(t_pt, b_pt)), 0.028))
            mb_crown.add_crystal_spire(mats["crystal_glass"], b_pt, t_outer, rb * 1.07, rb * 0.85, sides=6, split_u=0.64)

    sc.add_node("crystal_crown", sc.add_mesh(mb_crown), parent=body)

    # ---- C. FLOATING MARBLE SHOULDER WING-PLATES & COLLARBONE RIB SHARDS ----
    for sgn, nm in ((-1.0, "shoulder_right"), (+1.0, "shoulder_left")):
        mb_sh = MeshBuilder(nm + "_mesh")
        mb_sh.add_bevel_box(mats["marble"], (sgn * 0.54, 1.44, 0.08), (0.14, 0.36, 0.22),
                            bevel=0.024, rot=(-8, -sgn * 18, -sgn * 34), taper_top=(0.56, 0.72))
        mb_sh.add_bevel_box(mats["marble"], (sgn * 0.46, 1.30, 0.15), (0.11, 0.22, 0.16),
                            bevel=0.020, rot=(+6, -sgn * 14, -sgn * 26), taper_top=(0.65, 0.75))
        mb_sh.add_bevel_box(mats["marble"], (sgn * 0.28, 0.79, 0.16), (0.13, 0.30, 0.14),
                            bevel=0.020, rot=(+12, -sgn * 16, +sgn * 38), taper_top=(0.62, 0.75))
        sc.add_node(nm, sc.add_mesh(mb_sh), parent=body)

    # ---- D. SWIRLING TRANSLUCENT CYAN WISP VORTEX TAIL (y = 0.06 .. 0.98) ----
    for nm, r_top, n_ribbons, twist_tot in [
        ("wisp_outer", 0.32, 5, 165.0),
        ("wisp_inner", 0.18, 4, -220.0),
    ]:
        mb_w = MeshBuilder(nm + "_mesh")
        pw = mb_w.prim(mats["wisp_blend"])
        steps = 9
        for rb_i in range(n_ribbons):
            base_a = (math.tau * rb_i) / n_ribbons
            for st in range(steps):
                u0, u1 = st / steps, (st + 1) / steps
                y0 = 0.98 - u0 * 0.92
                y1 = 0.98 - u1 * 0.92
                rad0 = r_top * (1.0 - u0 * 0.92)
                rad1 = r_top * (1.0 - u1 * 0.92)
                w0 = 0.15 * (1.0 - u0 * 0.78)
                w1 = 0.15 * (1.0 - u1 * 0.78)
                a0 = base_a + math.radians(twist_tot * u0)
                a1 = base_a + math.radians(twist_tot * u1)

                p00 = (math.cos(a0 - 0.38) * rad0, y0, math.sin(a0 - 0.38) * rad0)
                p01 = (math.cos(a0 + 0.38) * (rad0 + w0), y0, math.sin(a0 + 0.38) * (rad0 + w0))
                p11 = (math.cos(a1 + 0.38) * (rad1 + w1), y1, math.sin(a1 + 0.38) * (rad1 + w1))
                p10 = (math.cos(a1 - 0.38) * rad1, y1, math.sin(a1 - 0.38) * rad1)
                pw.add_quad(p00, p01, p11, p10,
                            (0.12, u0), (0.88, u0), (0.88, u1), (0.12, u1),
                            two_sided=True)
        sc.add_node(nm, sc.add_mesh(mb_w), parent=body)

    # ---- E. DISCONNECTED FLOATING GAUNTLET-BLADE CLAWS (LEFT & RIGHT) ----
    for sgn, nm in ((-1.0, "arm_right"), (+1.0, "arm_left")):
        mb_claw = MeshBuilder(nm + "_mesh")
        build_katar_claw_mesh(mb_claw, mats, scale=0.88)
        sc.add_node(
            nm,
            sc.add_mesh(mb_claw),
            translation=(sgn * 0.75, 1.00, 0.14),
            rotation_euler=(+34.0, sgn * 18.0, -sgn * 22.0),
            parent=body,
        )

    return sc.to_glb_bytes()


# ==============================================================================
# 6. COLONNADE BOSS (.GLB) — 1:1 TO 01_colonnade_boss_ref.png (ZERO HUMAN SHADOW!)
# ==============================================================================
def generate_rift_obelisk_boss_glb(pngs: dict) -> bytes:
    sc = GlbScene()
    mats = setup_palette(sc, pngs)
    root = sc.add_node("root")
    body = sc.add_node("body", parent=root)

    # ---- A. UPPER FRACTURED WHITE-MARBLE OBELISK PYRAMIDION & ERUPTING CRYSTALS (y = 2.30 .. 4.65) ----
    # Matches 01_colonnade_boss_ref.png: monumental tapered white-marble obelisk spire split by a glowing cyan rift seam,
    # crowned by a sharp white-marble pyramidion apex, with faceted aquamarine crystals erupting out of its fractured base!
    mb_up = MeshBuilder("obelisk_upper_mesh")
    mb_up.add_bevel_box(mats["gold"], (0.0, 2.38, 0.0), (0.94, 0.12, 0.94), bevel=0.025, uv_rect=(0.52, 0.52, 0.98, 0.98))
    # Split lower-upper marble obelisk halves with glowing cyan inner rift seam
    mb_up.add_bevel_box(mats["eye_glow"], (0.0, 3.25, 0.0), (0.14, 1.55, 0.56), bevel=0.02)
    mb_up.add_bevel_box(mats["marble"], (-0.23, 3.04, 0.0), (0.38, 1.24, 0.74), bevel=0.03, taper_top=(0.76, 0.80))
    mb_up.add_bevel_box(mats["marble"], (+0.23, 3.04, 0.0), (0.38, 1.24, 0.74), bevel=0.03, taper_top=(0.76, 0.80))
    # Upper tapered white-marble obelisk shaft + sharp 4-sided white-marble pyramidion cap!
    mb_up.add_bevel_box(mats["marble"], (0.0, 3.88, 0.0), (0.60, 0.62, 0.60), bevel=0.028, taper_top=(0.64, 0.64))
    mb_up.add_crystal_spire(mats["marble"], (0.0, 4.16, 0.0), (0.0, 4.72, 0.0), 0.27, 0.22, sides=4,
                            split_u=0.18, twist_deg=0.0, col_base=(0.94, 0.95, 0.97), col_tip=(1.0, 1.0, 1.0))
    # Glowing cyan rift veins along the marble pyramidion & erupting teal crystal shards around the fractured base!
    mb_up.add_bevel_box(mats["eye_glow"], (0.0, 3.92, 0.0), (0.05, 0.78, 0.62), bevel=0.008)
    for ang_d in (25, 75, 135, 205, 255, 315):
        ar = math.radians(ang_d)
        bx, bz = math.cos(ar) * 0.26, math.sin(ar) * 0.26
        tx, tz = math.cos(ar) * 0.52, math.sin(ar) * 0.52
        mb_up.add_crystal_spire(mats["crystal_solid"], (bx, 2.38, bz), (tx, 3.48 + (ang_d % 2) * 0.28, tz), 0.135, 0.09, sides=6)
    sc.add_node("obelisk_upper", sc.add_mesh(mb_up), parent=body)

    # ---- B. SPLIT LEFT & RIGHT MARBLE MONOLITH PILLARS (y = 0.88 .. 2.34) ----
    for sgn, nm in ((-1.0, "obelisk_right"), (+1.0, "obelisk_left")):
        mb_side = MeshBuilder(nm + "_mesh")
        mb_side.add_bevel_box(mats["marble"], (sgn * 0.45, 1.62, 0.0), (0.36, 1.42, 0.76), bevel=0.03)
        mb_side.add_bevel_box(mats["marble"], (sgn * 0.27, 2.12, 0.0), (0.16, 0.28, 0.58), bevel=0.022, rot=(0, 0, sgn * 14))
        mb_side.add_bevel_box(mats["marble"], (sgn * 0.28, 1.14, 0.0), (0.16, 0.28, 0.58), bevel=0.022, rot=(0, 0, -sgn * 14))
        mb_side.add_bevel_box(mats["gold"], (sgn * 0.45, 2.30, 0.0), (0.40, 0.08, 0.80), bevel=0.018, uv_rect=(0.52, 0.52, 0.98, 0.98))
        mb_side.add_bevel_box(mats["gold"], (sgn * 0.45, 0.92, 0.0), (0.40, 0.08, 0.80), bevel=0.018, uv_rect=(0.52, 0.52, 0.98, 0.98))
        sc.add_node(nm, sc.add_mesh(mb_side), parent=body)

    # ---- C. GOLD RELIEF EMBLEM BLOCKS & INVERTED CRYSTAL PYRAMID BASE (y = -0.54 .. +0.90) ----
    mb_base = MeshBuilder("obelisk_base_mesh")
    mb_base.add_bevel_box(mats["marble"], (-0.36, 0.62, 0.0), (0.38, 0.52, 0.68), bevel=0.024)
    mb_base.add_bevel_box(mats["marble"], (+0.36, 0.62, 0.0), (0.38, 0.52, 0.68), bevel=0.024)
    mb_base.add_bevel_box(mats["gold"], (-0.36, 0.62, +0.345), (0.32, 0.42, 0.05), bevel=0.012, uv_rect=(0.0, 0.0, 0.5, 0.5))
    mb_base.add_bevel_box(mats["gold"], (+0.36, 0.62, +0.345), (0.32, 0.42, 0.05), bevel=0.012, uv_rect=(0.0, 0.0, 0.5, 0.5))
    mb_base.add_bevel_box(mats["gold"], (-0.36, 0.62, -0.345), (0.32, 0.42, 0.05), bevel=0.012, uv_rect=(0.0, 0.0, 0.5, 0.5))
    mb_base.add_bevel_box(mats["gold"], (+0.36, 0.62, -0.345), (0.32, 0.42, 0.05), bevel=0.012, uv_rect=(0.0, 0.0, 0.5, 0.5))

    mb_base.add_bevel_box(mats["marble"], (0.0, 0.28, 0.0), (0.76, 0.18, 0.64), bevel=0.022)
    mb_base.add_crystal_spire(mats["crystal_solid"], (0.0, 0.42, 0.0), (0.0, -0.54, 0.0), 0.44, 0.26, sides=6, split_u=0.55)
    mb_base.add_crystal_spire(mats["crystal_bright"], (0.0, 0.90, 0.0), (0.0, -0.48, 0.0), 0.24, 0.14, sides=6, split_u=0.55)
    for ang_d in (30, 90, 150, 210, 270, 330):
        ar = math.radians(ang_d)
        mb_base.add_crystal_spire(
            mats["crystal_solid"],
            (math.cos(ar) * 0.30, 0.34, math.sin(ar) * 0.28),
            (math.cos(ar) * 0.20, -0.24, math.sin(ar) * 0.18),
            0.12, 0.07, sides=5,
        )
    sc.add_node("obelisk_base", sc.add_mesh(mb_base), parent=body)

    # ---- D. BLAZING STACKED OCTAHEDRAL CRYSTAL CLUSTER CORE (STRICTLY ZERO HUMAN SHADOW!) ----
    mb_core_local = MeshBuilder("prism_core_local")
    mb_core_local.add_crystal_spire(mats["core_white"], (0.0, -0.74, 0.0), (0.0, +0.74, 0.0), 0.15, 0.14, sides=6, double_ended=True)
    mb_core_local.add_octahedron(mats["crystal_glass"], mats["eye_glow"], (0.0, 0.0, 0.0), (0.37, 0.43, 0.37), rot=(0, 45, 0), wire_thick=0.025)
    mb_core_local.add_octahedron(mats["crystal_glass"], mats["eye_glow"], (0.0, +0.39, 0.0), (0.29, 0.33, 0.29), rot=(0, 12, 0), wire_thick=0.021)
    mb_core_local.add_octahedron(mats["crystal_glass"], mats["eye_glow"], (0.0, -0.39, 0.0), (0.29, 0.33, 0.29), rot=(0, 78, 0), wire_thick=0.021)
    for i in range(8):
        ar = (math.tau * i) / 8.0 + 0.39
        elev = 0.24 if i % 2 == 0 else -0.24
        mb_core_local.add_crystal_spire(
            mats["crystal_bright"],
            (0.0, 0.0, 0.0),
            (math.cos(ar) * 0.50, elev, math.sin(ar) * 0.50),
            0.095, 0.058, sides=6,
        )
    sc.add_node("prism_core", sc.add_mesh(mb_core_local), translation=(0.0, 1.62, 0.0), parent=body)

    # ---- E. MASSIVE 24-SEGMENT HORIZONTAL CARVED STONE RUNIC RING OVERHEAD (y = 2.52) ----
    mb_ring_top = MeshBuilder("ring_top_mesh")
    mb_ring_top.add_carved_rune_ring(
        mats["ashlar"], mats["rune_glow"], mats["gold"],
        r_in=1.24, r_out=1.56, height=0.38, segments=24, axis="Y",
        outer_cogs=False, rune_outer=True, rune_inner=True, rune_sides=False, framed_band=True,
    )
    mb_ring_top.add_carved_rune_ring(
        mats["ashlar"], mats["rune_glow"], mats["gold"],
        r_in=0.84, r_out=0.98, height=0.18, segments=20, axis="Y",
        outer_cogs=False, rune_outer=True, rune_inner=True, rune_sides=False, framed_band=True,
    )
    for i in range(8):
        ar = (math.tau * i) / 8.0
        mb_ring_top.add_rod(
            mats["gold"],
            (math.cos(ar) * 0.96, 0.0, math.sin(ar) * 0.96),
            (math.cos(ar) * 1.26, 0.0, math.sin(ar) * 1.26),
            0.036, sides=6,
        )
    sc.add_node("ring_top", sc.add_mesh(mb_ring_top), translation=(0.0, 2.52, 0.0), parent=body)

    # ---- F. TWO SYMMETRICAL A-FRAME TILTED 24-SEGMENT SPOKED ASTROLABE WHEELS ----
    def make_spoked_wheel_mesh(mesh_name: str) -> int:
        mw = MeshBuilder(mesh_name)
        mw.add_carved_rune_ring(
            mats["ashlar"], mats["rune_glow"], mats["gold"],
            r_in=0.80, r_out=1.06, height=0.20, segments=24, axis="Z",
            outer_cogs=True, cog_every=1, rune_outer=False, rune_inner=False, rune_sides=True,
        )
        mw.add_carved_rune_ring(
            mats["gold"], mats["rune_glow"], mats["gold"],
            r_in=0.30, r_out=0.39, height=0.14, segments=16, axis="Z",
            outer_cogs=False, rune_outer=False, rune_inner=False, rune_sides=False,
        )
        mw.add_rod(mats["gold"], (0.0, 0.0, -0.14), (0.0, 0.0, +0.14), 0.18, sides=12)
        mw.add_crystal_spire(mats["crystal_bright"], (0.0, 0.0, -0.24), (0.0, 0.0, +0.24), 0.11, 0.10, sides=8, double_ended=True)
        for si in range(8):
            ang = (math.tau * si) / 8.0
            ca, sa = math.cos(ang), math.sin(ang)
            mw.add_rod(mats["gold"], (ca * 0.15, sa * 0.15, 0.0), (ca * 0.82, sa * 0.82, 0.0), 0.042, sides=6)
            mw.add_rod(mats["gold"], (ca * 0.56, sa * 0.56, -0.05), (ca * 0.56, sa * 0.56, +0.05), 0.068, sides=8)
        return sc.add_mesh(mw)

    wheel_mesh_idx = make_spoked_wheel_mesh("astrolabe_wheel_mesh")
    # Exact A-frame tilt matching 01_colonnade_boss_ref.png:
    # Wheel starts in XY plane (facing +Z). Rotating around Y by +-42 deg faces it semi-front/semi-outward,
    # and rotating around X by -36 deg tilts the TOP of the wheel INWARD/BACK toward the top stone ring while
    # the BOTTOM flares outward! Let's verify in preview!
    mount_l = sc.add_node("mount_left", translation=(+1.58, 1.56, 0.16), rotation_euler=(-34.0, +42.0, +18.0), parent=body)
    sc.add_node("wheel_left", wheel_mesh_idx, parent=mount_l)

    mount_r = sc.add_node("mount_right", translation=(-1.58, 1.56, 0.16), rotation_euler=(-34.0, -42.0, -18.0), parent=body)
    sc.add_node("wheel_right", wheel_mesh_idx, parent=mount_r)

    # ---- G. 14 FLOATING ORBITAL DARK-TEAL CRYSTAL SHARDS ----
    mb_orb = MeshBuilder("orbit_shards_mesh")
    rng = random.Random(1901)
    for i in range(14):
        ang = (math.tau * i) / 14.0 + rng.uniform(-0.12, 0.12)
        rad = rng.uniform(1.25, 2.30) if i < 10 else rng.uniform(0.55, 1.05)
        yy = rng.uniform(0.45, 2.95) if i < 10 else rng.uniform(-0.35, 0.35)
        cx, cz = math.cos(ang) * rad, math.sin(ang) * rad
        tilt_x = rng.uniform(-0.10, 0.10)
        tilt_z = rng.uniform(-0.10, 0.10)
        h = rng.uniform(0.18, 0.30)
        mb_orb.add_crystal_spire(
            mats["crystal_solid" if i % 3 != 0 else "crystal_bright"],
            (cx - tilt_x, yy - h * 0.5, cz - tilt_z),
            (cx + tilt_x, yy + h * 0.5, cz + tilt_z),
            rng.uniform(0.058, 0.094),
            rng.uniform(0.042, 0.068),
            sides=6,
            double_ended=True,
        )
    sc.add_node("orbit_shards", sc.add_mesh(mb_orb), parent=body)

    return sc.to_glb_bytes()


# ==============================================================================
# 7. MATCHING 3D WEAPONS & PROJECTILES (.GLB)
# ==============================================================================
def generate_crystal_katar_glb(pngs: dict) -> bytes:
    sc = GlbScene()
    mats = setup_palette(sc, pngs)
    mb = MeshBuilder("crystal_katar_mesh")
    build_katar_claw_mesh(mb, mats, scale=1.0)
    sc.add_node("katar", sc.add_mesh(mb))
    return sc.to_glb_bytes()


def generate_crystal_shield_glb(pngs: dict) -> bytes:
    sc = GlbScene()
    mats = setup_palette(sc, pngs)
    mb = MeshBuilder("crystal_shield_mesh")
    mb.add_carved_rune_ring(
        mats["marble"], mats["rune_glow"], mats["gold"],
        r_in=0.66, r_out=0.88, height=0.12, segments=8, axis="Z",
        outer_cogs=True, rune_sides=True,
    )
    pg = mb.prim(mats["crystal_glass"])
    fsh = [1.02, 0.86, 0.98, 0.82, 1.00, 0.85, 0.96, 0.84]
    for i in range(8):
        a0, a1 = (math.tau * i) / 8.0, (math.tau * (i + 1)) / 8.0
        p0 = (math.cos(a0) * 0.68, math.sin(a0) * 0.68, 0.02)
        p1 = (math.cos(a1) * 0.68, math.sin(a1) * 0.68, 0.02)
        apex_f = (0.0, 0.0, +0.16)
        apex_b = (0.0, 0.0, -0.06)
        s = fsh[i]
        c_edge = (0.74 * s, 0.94 * s, 0.99 * s)
        pg.add_tri(p0, p1, apex_f, (0.05, 0.2), (0.28, 0.2), (0.16, 0.96), c_edge, c_edge, (1.0, 1.0, 1.0), two_sided=True)
        pg.add_tri(p1, p0, apex_b, (0.28, 0.2), (0.05, 0.2), (0.16, 0.96), c_edge, c_edge, (1.0, 1.0, 1.0), two_sided=True)
        mb.add_rod(mats["gold"], (0.0, 0.0, 0.12), (math.cos(a0) * 0.70, math.sin(a0) * 0.70, 0.02), 0.024, sides=5)
    mb.add_octahedron(mats["crystal_bright"], mats["core_white"], (0.0, 0.0, 0.09), (0.20, 0.20, 0.16), wire_thick=0.014)
    sc.add_node("shield", sc.add_mesh(mb))
    return sc.to_glb_bytes()


def generate_crystal_shard_glb(pngs: dict) -> bytes:
    sc = GlbScene()
    mats = setup_palette(sc, pngs)
    mb = MeshBuilder("crystal_shard_mesh")
    mb.add_crystal_spire(mats["crystal_bright"], (0.0, 0.0, -0.18), (0.0, 0.0, +0.44), 0.095, 0.062, sides=6, double_ended=True)
    mb.add_crystal_spire(mats["crystal_glass"], (0.0, 0.0, -0.22), (0.0, 0.0, +0.48), 0.115, 0.076, sides=6, double_ended=True)
    sc.add_node("shard", sc.add_mesh(mb))
    return sc.to_glb_bytes()


def generate_crystal_barrage_item_glb(pngs: dict) -> bytes:
    """3-shard radiant fan of faceted aquamarine crystal spears for the 'crystal_barrage' item."""
    sc = GlbScene()
    mats = setup_palette(sc, pngs)
    mb = MeshBuilder("crystal_barrage_item_mesh")
    for sgn, elev, ln in [(0.0, 0.04, 0.48), (-1.0, -0.02, 0.40), (+1.0, -0.02, 0.40)]:
        bx = sgn * 0.08
        tx = sgn * 0.24
        mb.add_crystal_spire(mats["crystal_bright"], (bx, elev * 0.5, -0.16), (tx, elev, ln), 0.078, 0.052, sides=6, double_ended=True)
        mb.add_crystal_spire(mats["crystal_glass"], (bx, elev * 0.5, -0.19), (tx * 1.04, elev, ln + 0.03), 0.094, 0.062, sides=6, double_ended=True)
    sc.add_node("barrage_fan", sc.add_mesh(mb))
    return sc.to_glb_bytes()


def generate_astral_prism_glb(pngs: dict) -> bytes:
    sc = GlbScene()
    mats = setup_palette(sc, pngs)
    root = sc.add_node("root")

    mb_outer = MeshBuilder("ring_outer_mesh")
    mb_outer.add_carved_rune_ring(
        mats["ashlar"], mats["rune_glow"], mats["gold"],
        r_in=0.38, r_out=0.49, height=0.10, segments=16, axis="Y",
        rune_outer=True, rune_inner=True, framed_band=True,
    )
    sc.add_node("ring_a", sc.add_mesh(mb_outer), parent=root)

    mb_inner = MeshBuilder("ring_inner_mesh")
    mb_inner.add_carved_rune_ring(
        mats["gold"], mats["rune_glow"], mats["gold"],
        r_in=0.26, r_out=0.34, height=0.08, segments=16, axis="Z",
        outer_cogs=True, rune_sides=True,
    )
    for si in range(4):
        a = (math.tau * si) / 4.0
        mb_inner.add_rod(mats["gold"], (math.cos(a) * 0.10, math.sin(a) * 0.10, 0.0),
                         (math.cos(a) * 0.27, sa := math.sin(a) * 0.27, 0.0), 0.016, sides=5)
    sc.add_node("ring_b", sc.add_mesh(mb_inner), rotation_euler=(35.0, 20.0, 0.0), parent=root)

    mb_core = MeshBuilder("core_mesh")
    mb_core.add_octahedron(mats["crystal_glass"], mats["eye_glow"], (0.0, 0.0, 0.0), (0.14, 0.20, 0.14), wire_thick=0.012)
    mb_core.add_crystal_spire(mats["core_white"], (0.0, -0.14, 0.0), (0.0, +0.14, 0.0), 0.06, 0.05, sides=6, double_ended=True)
    sc.add_node("core", sc.add_mesh(mb_core), parent=root)
    return sc.to_glb_bytes()


def verify_glb(name: str, data: bytes):
    magic, ver, total = struct.unpack_from("<III", data, 0)
    assert magic == 0x46546C67 and ver == 2 and total == len(data), f"{name}: bad GLB header"
    jlen, jtype = struct.unpack_from("<II", data, 12)
    assert jtype == 0x4E4F534A
    doc = json.loads(data[20:20 + jlen].decode("utf-8"))
    b_off = 20 + jlen
    blen, btype = struct.unpack_from("<II", data, b_off)
    assert btype == 0x004E4942 and b_off + 8 + blen == len(data)
    bin_chunk = data[b_off + 8:]

    verts = 0
    tris = 0
    for m in doc["meshes"]:
        for p in m["primitives"]:
            pa = doc["accessors"][p["attributes"]["POSITION"]]
            ia = doc["accessors"][p["indices"]]
            verts += pa["count"]
            tris += ia["count"] // 3
            for c in pa["min"] + pa["max"]:
                assert math.isfinite(c), f"{name}: non-finite vertex"
    for bv in doc["bufferViews"]:
        assert 0 <= bv["byteOffset"] and bv["byteOffset"] + bv["byteLength"] <= len(bin_chunk)
    print(f"  OK  {name:26s} size={len(data):7d} B  imgs={len(doc['images'])}  nodes={len(doc['nodes']):2d}  verts={verts:5d}  tris={tris:5d}")


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    print("Generating shared 256x256 procedural crystalline & temple stone PNG textures...")
    pngs = {
        "marble": make_marble_png(256),
        "ashlar": make_ashlar_stone_png(256),
        "gold": make_gold_relief_png(256),
        "crystal_op": make_crystal_png(256, translucent=False),
        "crystal_tr": make_crystal_png(256, translucent=True),
        "rune": make_rune_cutout_png(256),
        "wisp": make_wisp_png(256),
    }

    targets = [
        ("rift_obelisk_boss.glb", generate_rift_obelisk_boss_glb(pngs)),
        ("crystal_minion.glb", generate_crystal_minion_glb(pngs)),
        ("crystal_katar.glb", generate_crystal_katar_glb(pngs)),
        ("crystal_shield.glb", generate_crystal_shield_glb(pngs)),
        ("crystal_shard.glb", generate_crystal_shard_glb(pngs)),
        ("crystal_barrage_item.glb", generate_crystal_barrage_item_glb(pngs)),
        ("astral_prism.glb", generate_astral_prism_glb(pngs)),
    ]
    for fn, blob in targets:
        verify_glb(fn, blob)
        with open(os.path.join(OUT_DIR, fn), "wb") as f:
            f.write(blob)


if __name__ == "__main__":
    main()
