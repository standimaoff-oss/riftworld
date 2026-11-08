#!/usr/bin/env python3
"""Превью объёмного оружия Элисара без запуска Minecraft.

Маленький программный растеризатор: ортографическая камера, z-буфер, плоское
затенение и выборка цвета прямо из атласа по UV. Если развёртка или обход
граней где-то перепутаны, это видно на листе сразу, а не только в игре.

Запуск:  python tools/rift/preview_v046_weapons.py
Выход:   reports/v040_refs/01_weapons_3d_preview.png
"""
from __future__ import annotations

import json
import struct
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parents[2]
GLB = HERE / "mod/src/main/resources/assets/riftworld/models/glb"
OUT = HERE / "reports/v040_refs/01_weapons_3d_preview.png"
CELL = 260
BG = 26.0 / 255.0   # тот же серый, что и фон листа


def read_glb(p: Path):
    d = p.read_bytes()
    off, js, bin_ = 12, None, None
    while off < len(d):
        clen, ctype = struct.unpack("<II", d[off:off + 8])
        body = d[off + 8:off + 8 + clen]
        if ctype == 0x4E4F534A:
            js = json.loads(body.decode("utf-8"))
        else:
            bin_ = body
        off += 8 + clen
    return js, bin_


def view(js, bin_, i):
    bv = js["bufferViews"][i]
    return bin_[bv["byteOffset"]:bv["byteOffset"] + bv["byteLength"]]


def load(p: Path):
    js, bin_ = read_glb(p)
    prim = js["meshes"][0]["primitives"][0]
    pos = np.frombuffer(view(js, bin_, js["accessors"][prim["attributes"]["POSITION"]]["bufferView"]),
                        dtype="<f4").reshape(-1, 3)
    nrm = np.frombuffer(view(js, bin_, js["accessors"][prim["attributes"]["NORMAL"]]["bufferView"]),
                        dtype="<f4").reshape(-1, 3)
    uv = np.frombuffer(view(js, bin_, js["accessors"][prim["attributes"]["TEXCOORD_0"]]["bufferView"]),
                       dtype="<f4").reshape(-1, 2)
    idx = np.frombuffer(view(js, bin_, js["accessors"][prim["indices"]]["bufferView"]),
                        dtype="<u4").reshape(-1, 3)
    im = js["images"][0]
    png = view(js, bin_, im["bufferView"])
    atlas = np.asarray(Image.open(__import__("io").BytesIO(png)).convert("RGB")).astype(np.float32) / 255.0
    return pos, nrm, uv, idx, atlas


def render(pos, nrm, uv, idx, atlas, size=CELL):
    """Ортографическая камера спереди-сверху-справа, чтобы читалась и форма, и объём."""
    ang = np.deg2rad(-28.0)
    # Сначала поворот вокруг Y, потом наклон вокруг X.
    cy, sy = np.cos(ang), np.sin(ang)
    ct, st = np.cos(np.deg2rad(18.0)), np.sin(np.deg2rad(18.0))
    p = pos.copy()
    p = p @ np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]], dtype=np.float32).T
    p = p @ np.array([[1, 0, 0], [0, ct, -st], [0, st, ct]], dtype=np.float32).T
    n = nrm @ np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]], dtype=np.float32).T
    n = n @ np.array([[1, 0, 0], [0, ct, -st], [0, st, ct]], dtype=np.float32).T

    lo, hi = p.min(axis=0), p.max(axis=0)
    span = max(hi[0] - lo[0], hi[1] - lo[1]) * 1.16
    cx, cyy = (lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2
    # В кадре вниз — это «вверх» модели: переводим в экранные пиксели.
    sx = (p[:, 0] - cx) / span * size + size / 2
    sy_ = size / 2 - (p[:, 1] - cyy) / span * size

    img = np.full((size, size, 3), BG, dtype=np.float32)
    zbuf = np.full((size, size), np.inf, dtype=np.float32)
    light = np.array([0.42, 0.80, -0.43], dtype=np.float32)
    light /= np.linalg.norm(light)
    fill = np.array([-0.55, 0.25, 0.70], dtype=np.float32)
    fill /= np.linalg.norm(fill)

    for tri in idx:
        a, b, c = tri
        x = np.array([sx[a], sx[b], sx[c]])
        y = np.array([sy_[a], sy_[b], sy_[c]])
        z = np.array([p[a, 2], p[b, 2], p[c, 2]])
        area = (x[1] - x[0]) * (y[2] - y[0]) - (x[2] - x[0]) * (y[1] - y[0])
        if abs(area) < 1e-9:
            continue
        if area > 0:  # задняя грань
            continue
        minx, maxx = max(0, int(np.floor(x.min()))), min(size - 1, int(np.ceil(x.max())))
        miny, maxy = max(0, int(np.floor(y.min()))), min(size - 1, int(np.ceil(y.max())))
        if minx > maxx or miny > maxy:
            continue
        gx, gy = np.meshgrid(np.arange(minx, maxx + 1) + 0.5, np.arange(miny, maxy + 1) + 0.5)
        w0 = ((x[1] - x[0]) * (gy - y[0]) - (y[1] - y[0]) * (gx - x[0])) / area
        w1 = ((x[2] - x[1]) * (gy - y[1]) - (y[2] - y[1]) * (gx - x[1])) / area
        w2 = ((x[0] - x[2]) * (gy - y[2]) - (y[0] - y[2]) * (gx - x[2])) / area
        inside = (w1 >= -0.02) & (w2 >= -0.02) & (w0 >= -0.02)
        if not inside.any():
            continue
        zz = w1 * z[0] + w2 * z[1] + w0 * z[2]
        closer = inside & (zz < zbuf[miny:maxy + 1, minx:maxx + 1])
        if not closer.any():
            continue
        uua = uv[[a, b, c], 0]
        vva = uv[[a, b, c], 1]
        uu = np.clip((w1 * uua[0] + w2 * uua[1] + w0 * uua[2]) * atlas.shape[1], 0, atlas.shape[1] - 1)
        vv = np.clip((w1 * vva[0] + w2 * vva[1] + w0 * vva[2]) * atlas.shape[0], 0, atlas.shape[0] - 1)
        col = atlas[vv.astype(int)[closer], uu.astype(int)[closer]]
        # Основной свет плюс заполняющий: иначе почти чёрный рифт-кристалл
        # на превью вообще не читается.
        lam = max(0.0, float(np.dot(n[a], light)))
        lam2 = max(0.0, float(np.dot(n[a], fill)))
        shade = 0.62 + 0.62 * lam + 0.22 * lam2
        sub = zbuf[miny:maxy + 1, minx:maxx + 1]
        sub[closer] = zz[closer]
        img[miny:maxy + 1, minx:maxx + 1][closer] = col * shade
    return (np.clip(img, 0, 1) * 255).astype(np.uint8)


def main():
    tiers = ["aurite", "riftcrystal"]
    names = ["sword", "pickaxe", "axe", "shovel", "hoe"]
    sheet = np.full((CELL * len(tiers), CELL * len(names), 3), 26, dtype=np.uint8)
    for r, tier in enumerate(tiers):
        for c, name in enumerate(names):
            p = GLB / f"{tier}_{name}.glb"
            sheet[r * CELL:(r + 1) * CELL, c * CELL:(c + 1) * CELL] = render(*load(p))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(sheet).save(OUT)
    # Численная самопроверка: оружие должно занимать разумную долю кадра.
    for r, tier in enumerate(tiers):
        row = []
        for c, name in enumerate(names):
            cell = sheet[r * CELL:(r + 1) * CELL, c * CELL:(c + 1) * CELL]
            # Фон листа — ровно 26; сравниваем с ним, а не с абсолютной яркостью:
            # рифт-кристалл почти чёрный, порог по яркости счёл бы его пустым.
            filled = float((np.abs(cell.astype(int) - 26).max(axis=2) > 6).mean())
            bright = float(cell[np.abs(cell.astype(int) - 26).max(axis=2) > 6].mean()) if filled else 0.0
            row.append(f"{name}:{filled * 100:4.1f}%/ярк{bright:3.0f}")
            assert 0.02 < filled < 0.60, f"{tier}_{name} занимает {filled:.1%} кадра — подозрительно"
        print(f"{tier:12s} " + "  ".join(row))
    print("wrote", OUT, sheet.shape)


if __name__ == "__main__":
    main()
