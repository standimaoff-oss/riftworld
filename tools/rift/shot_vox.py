#!/usr/bin/env python3
"""Офлайн-рендер структурных NBT мода («скриншоты» без игры): перспектива, painter's algorithm,
цвета семплируются из реальных текстур мода (assets/riftworld/textures/block).

usage: shot_vox.py <nbt> <out.png> <yaw> <pitch> <dist> [scale] [--cut x|y|z=N] [--sky dusk|day|night]
"""
import gzip
import io
import math
import os
import struct
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.normpath(os.path.join(HERE, "../../mod/src/main/resources/assets/riftworld/textures/block"))

OPAQUE_FALLBACK = {
    "temple_bricks": (232, 226, 210), "temple_bricks_stairs": (228, 221, 204), "temple_bricks_slab": (228, 221, 204),
    "temple_bricks_cracked": (206, 198, 180), "temple_bricks_ivy": (176, 196, 158), "temple_gold": (238, 196, 74),
    "temple_pillar": (240, 234, 218), "temple_floor": (244, 240, 228), "temple_mosaic": (128, 44, 84),
    "mosaic_core": (196, 46, 66), "temple_gold_rune": (222, 182, 92), "temple_spirit": (228, 218, 194),
    "temple_grand_column": (242, 236, 220), "temple_rose_window": (150, 120, 210), "ritual_candle": (242, 226, 170),
    "light_bridge": (250, 240, 200), "demon_frame": (46, 34, 40), "demon_gate": (150, 30, 44),
    "keep_stone": (150, 150, 152), "keep_stone_cracked": (132, 132, 136), "keep_stone_mossy": (120, 138, 116),
    "keep_cage_lantern": (250, 202, 110), "elisar_chain": (70, 70, 78), "castle_carpet": (140, 40, 48),
    "elisar_lantern": (250, 214, 140), "bridge_walk": (122, 92, 58), "bridge_post": (96, 72, 46),
    "elisar_stone": (128, 132, 138), "dark_planks": (62, 44, 34),
}
ALPHA = {"elisar_water_still": 0.5, "light_blue_stained_glass": 0.42, "temple_rose_window": 0.75, "light_bridge": 0.5, "demon_gate": 0.8,
         "elisar_chain": 0.9, "ritual_candle": 0.95}
EMISSIVE = {"mosaic_core": 1.0, "temple_gold_rune": 0.55, "keep_cage_lantern": 0.85, "ritual_candle": 0.7,
            "temple_rose_window": 0.5, "light_bridge": 0.9, "demon_gate": 0.6, "elisar_lantern": 0.8}


def rd(f, t):
    if t == 1: return struct.unpack(">b", f.read(1))[0]
    if t == 2: return struct.unpack(">h", f.read(2))[0]
    if t == 3: return struct.unpack(">i", f.read(4))[0]
    if t == 4: return struct.unpack(">q", f.read(8))[0]
    if t == 5: return struct.unpack(">f", f.read(4))[0]
    if t == 6: return struct.unpack(">d", f.read(8))[0]
    if t == 8:
        n = struct.unpack(">H", f.read(2))[0]; return f.read(n).decode()
    if t == 9:
        et, n = struct.unpack(">bi", f.read(5)); return [rd(f, et) for _ in range(n)]
    if t == 10:
        o = {}
        while True:
            tt = struct.unpack(">b", f.read(1))[0]
            if tt == 0: return o
            n = struct.unpack(">H", f.read(2))[0]; k = f.read(n).decode(); o[k] = rd(f, tt)


def load(path):
    f = io.BytesIO(gzip.open(path).read()); f.read(1); f.read(struct.unpack(">H", f.read(2))[0])
    root = rd(f, 10)
    pal = [p["Name"].split(":")[-1] for p in root["palette"]]
    vox = {}
    for b in root["blocks"]:
        x, y, z = b["pos"]; vox[(x, y, z)] = pal[b["state"]]
    return root["size"], vox


def color_of(name):
    if name in OPAQUE_FALLBACK and not os.path.exists(os.path.join(TEX, name + ".png")):
        return OPAQUE_FALLBACK[name]
    p = os.path.join(TEX, name + ".png")
    if not os.path.exists(p):
        base = name.replace("_stairs", "").replace("_slab", "").replace("_wall", "")
        p = os.path.join(TEX, base + ".png")
    if os.path.exists(p):
        im = Image.open(p).convert("RGBA")
        a = np.asarray(im).reshape(-1, 4)
        m = a[:, 3] > 40
        if m.any():
            c = a[m][:, :3].mean(axis=0)
            return tuple(int(v) for v in c)
    return OPAQUE_FALLBACK.get(name, (200, 60, 200))


FACES = [((1, 0, 0), [(1, 0, 0), (1, 1, 0), (1, 1, 1), (1, 0, 1)]),
         ((-1, 0, 0), [(0, 0, 0), (0, 0, 1), (0, 1, 1), (0, 1, 0)]),
         ((0, 1, 0), [(0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)]),
         ((0, -1, 0), [(0, 0, 0), (0, 0, 1), (1, 0, 1), (1, 0, 0)]),
         ((0, 0, 1), [(0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)]),
         ((0, 0, -1), [(0, 0, 0), (1, 0, 0), (1, 1, 0), (0, 1, 0)])]


def main():
    a = sys.argv[1:]
    nbt, out = a[0], a[1]
    yaw, pitch, dist = float(a[2]), float(a[3]), float(a[4])
    scale = float(a[5]) if len(a) > 5 and not a[5].startswith("--") else 7.0
    cut = None
    sky = "dusk"
    i = 0
    while i < len(a):
        t = a[i]
        if t == "--cut":
            k, v = a[i + 1].split("="); cut = (k, int(v)); i += 2; continue
        if t == "--sky":
            sky = a[i + 1]; i += 2; continue
        i += 1
    size, vox = load(nbt)
    W, H, D = size
    ks = np.array(list(vox.keys()), np.float64)
    mn = ks.min(axis=0); mx = ks.max(axis=0)
    cx, cy, cz = (mn[0] + mx[0] + 1) / 2, mn[1] + (mx[1] - mn[1]) * 0.45, (mn[2] + mx[2] + 1) / 2
    cols = {n: color_of(n) for n in set(vox.values())}
    opaque = {n: (n not in ALPHA) for n in cols}

    quads = []
    for (x, y, z), n in vox.items():
        if cut:
            if cut[0] == "x" and x > cut[1]: continue
            if cut[0] == "y" and y > cut[1]: continue
            if cut[0] == "z" and z > cut[1]: continue
        for nrm, corners in FACES:
            nb = vox.get((x + nrm[0], y + nrm[1], z + nrm[2]))
            if nb is not None and opaque.get(nb, True) and not (n in ALPHA):
                continue
            if nb == n and opaque.get(n, True):
                continue
            quads.append((x, y, z, n, nrm, corners))

    yr, pr = math.radians(yaw), math.radians(pitch)
    fwd = np.array([math.sin(yr) * math.cos(pr), math.sin(pr), math.cos(yr) * math.cos(pr)])
    eye = np.array([cx, cy, cz]) + fwd * dist
    fcam = -fwd
    up = np.array([0.0, 1.0, 0.0])
    right = np.cross(fcam, up); right /= np.linalg.norm(right)
    upv = np.cross(right, fcam)
    fl = 1.6
    IW, IH = 1500, 1050
    L = np.array([0.45, 0.75, 0.35]); L /= np.linalg.norm(L)

    SKY = {"dusk": ((38, 30, 58), (232, 148, 96)), "day": ((120, 168, 226), (214, 232, 246)),
           "night": ((8, 10, 24), (30, 36, 66))}[sky]
    img = np.zeros((IH, IW, 3), np.float64)
    gy = np.linspace(0, 1, IH)[:, None]
    img[:, :, :] = (np.array(SKY[0])[None, None, :] * (1 - gy[..., None]) + np.array(SKY[1])[None, None, :] * gy[..., None])

    def proj(p):
        d = p - eye
        dz = d @ fcam
        if dz < 0.2:
            return None, dz
        dx = d @ right
        dy = d @ upv
        return (IW / 2 + dx / dz * fl * IW / 2, IH / 2 - dy / dz * fl * IW / 2), dz

    drawn = []
    for (x, y, z, n, nrm, corners) in quads:
        pts = []
        ok = True
        for c in corners:
            q, dz = proj(np.array([x + c[0], y + c[1], z + c[2]], np.float64))
            if q is None:
                ok = False
                break
            pts.append(q)
        if not ok:
            continue
        ctr = np.array([x + 0.5 + nrm[0] * 0.5, y + 0.5 + nrm[1] * 0.5, z + 0.5 + nrm[2] * 0.5])
        depth = np.linalg.norm(ctr - eye)
        drawn.append((depth, pts, n, nrm))
    drawn.sort(key=lambda t: -t[0])

    light_base = {"dusk": 0.62, "day": 1.0, "night": 0.34}[sky]
    for depth, pts, n, nrm in drawn:
        col = np.array(cols[n], np.float64)
        em = EMISSIVE.get(n, 0.0)
        sh = light_base * (0.42 + 0.58 * max(0.0, np.dot(np.array(nrm, np.float64), L)))
        col = col * (sh + em * 1.15)
        al = ALPHA.get(n, 1.0)
        xs = [p[0] for p in pts]; ys = [p[1] for p in pts]
        x0, x1 = int(max(0, min(xs))), int(min(IW - 1, max(xs) + 1))
        y0, y1 = int(max(0, min(ys))), int(min(IH - 1, max(ys) + 1))
        if x1 <= x0 or y1 <= y0:
            continue
        gx, gyy = np.meshgrid(np.arange(x0, x1 + 1), np.arange(y0, y1 + 1))
        inside = np.ones(gx.shape, bool)
        for i in range(4):
            ax, ay = pts[i]; bx, by = pts[(i + 1) % 4]
            cross = (bx - ax) * (gyy - ay) - (by - ay) * (gx - ax)
            inside &= cross >= -0.5
        if not inside.any():
            continue
        fog = min(1.0, max(0.0, (depth - dist * 0.55) / (dist * 1.5)))
        skyc = np.array(SKY[1], np.float64)
        c = col * (1 - fog * 0.75) + skyc * (fog * 0.75)
        reg = img[y0:y1 + 1, x0:x1 + 1]
        reg[inside] = reg[inside] * (1 - al) + c * al
    Image.fromarray(np.clip(img, 0, 255).astype(np.uint8)).save(out)
    print("rendered", out, "quads:", len(drawn), "vox:", len(vox))


if __name__ == "__main__":
    main()
