#!/usr/bin/env python3
"""Оффлайн-превью КОРАЛЛОВЫХ ДЕРЕВЬЕВ (Rev 13).

Порт `CoralGrove.java` (та же математика: hash, извилистый ствол, кривые ветви, кроны) плюс
изометрический рендер блоков. Нужен, чтобы глазами проверить силуэт растения ДО сборки jar:
заказчик ругался, что кораллы выглядели «трубами с шапками».

Запуск:  python3 tools/rift/preview_coral.py --out /home/user/preview_coral
"""
from __future__ import annotations

import argparse
import math
import os

from PIL import Image, ImageDraw

M64 = (1 << 64) - 1

STAGHORN, TABLE, WHIP, FAN, TUBES, SPIRAL = range(6)
NAMES = {STAGHORN: "staghorn", TABLE: "table", WHIP: "whip", FAN: "fan", TUBES: "tubes", SPIRAL: "spiral"}
STEM, PLATE = 0, 1


def _hash(x: int, z: int, s: int) -> float:
    h = (x * 0x9E3779B97F4A7C15 ^ z * 0xC2B2AE3D27D4EB4F ^ s * 0x165667B19E3779F9) & M64
    h ^= h >> 33
    h = (h * 0xFF51AFD7ED558CCD) & M64
    h ^= h >> 33
    h = (h * 0xC4CEB9FE1A85EC53) & M64
    h ^= h >> 33
    return (h >> 11) * (2.0 ** -53)


def rnd(seed: int, a: int, b: int) -> float:
    return _hash(seed, a, b)


def col_key(dx: int, dz: int) -> int:
    return ((dx + 16) << 8) | (dz + 16)


def put(cols: dict, dx: int, y: int, dz: int, typ: int) -> None:
    if y <= 0:
        return
    lst = cols.setdefault(col_key(dx, dz), [])
    for e in lst:
        if e[2] != typ:
            continue
        if e[0] <= y <= e[1]:
            return
        if y == e[1] + 1:
            e[1] = y
            return
        if y == e[0] - 1:
            e[0] = y
            return
    lst.append([y, y, typ])


def tip(tips: dict, dx: int, y: int, dz: int, block: int) -> None:
    if y <= 0:
        return
    tips.setdefault(col_key(dx, dz), []).append((y, block))


def plate(cols: dict, tips: dict, cx: int, y: int, cz: int, radius: float, seed: int) -> None:
    r = int(math.ceil(radius))
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            d = math.hypot(dx, dz)
            edge = radius + (rnd(seed, dx + 32, dz + 32) - 0.5) * 0.9
            if d > edge:
                continue
            if d > edge - 0.9 and rnd(seed + 7, dx + 32, dz + 32) < 0.30:
                continue
            put(cols, cx + dx, y, cz + dz, PLATE)
            if d < radius - 0.6:
                put(cols, cx + dx, y + 1, cz + dz, PLATE)
            elif rnd(seed + 13, dx + 64, dz + 64) < 0.45:
                tip(tips, cx + dx, y + 1, cz + dz, 0)   # полипы по кромке, как в моде


def limb(cols, x, y, z, ang, curve, ln, step, rise):
    bx, bz = float(x), float(z)
    for t in range(1, ln + 1):
        ang += curve
        bx += math.cos(ang) * step
        bz += math.sin(ang) * step
        put(cols, int(round(bx)), y + int(round(t * rise)), int(round(bz)), STEM)
    return int(round(bx)), y + int(round(ln * rise)), int(round(bz)), ang


def crown(cols, tips, kind, x, y, z, seed):
    if kind == STAGHORN:
        n = 2 + int(rnd(seed, 1, 711) * 2)
        for i in range(n):
            a = rnd(seed, i, 712) * math.pi * 2
            dx = int(round(math.cos(a) * 1.6))
            dz = int(round(math.sin(a) * 1.6))
            for t in range(1, 4):
                put(cols, x + dx * t // 2, y + t, z + dz * t // 2, STEM)
            tip(tips, x + dx, y + 3, z + dz, 0)
    elif kind == TABLE:
        plate(cols, tips, x, y + 1, z, 1.6 + rnd(seed, 2, 713) * 0.6, seed)
        tip(tips, x, y + 3, z, 1)
    elif kind == FAN:
        a = rnd(seed, 3, 714) * math.pi
        ca, sa = math.cos(a), math.sin(a)
        for u in range(-3, 4):
            if rnd(seed, u + 8, 715) < 0.22:
                continue
            top_y = 4 - abs(u) // 2
            dx = int(round(u * ca))
            dz = int(round(u * sa))
            for v in range(0, top_y + 1):
                put(cols, x + dx, y + v, z + dz, STEM)
            tip(tips, x + dx, y + top_y + 1, z + dz, 2)
    elif kind in (WHIP, SPIRAL):
        a = rnd(seed, 4, 716) * math.pi * 2
        px, pz = x, z
        for t in range(1, 5):
            a += 0.7 + rnd(seed, t, 717) * 0.6
            px += int(round(math.cos(a)))
            pz += int(round(math.sin(a)))
            put(cols, px, y + t, pz, STEM)
        tip(tips, px, y + 4, pz, 3)
    else:  # TUBES
        n = 3 + int(rnd(seed, 5, 718) * 3)
        for i in range(n):
            dx = int(round((rnd(seed, i, 719) - 0.5) * 4.0))
            dz = int(round((rnd(seed, i, 720) - 0.5) * 4.0))
            hh = 2 + int(rnd(seed, i, 721) * 4)
            for t in range(1, hh + 1):
                put(cols, x + dx, y + t, z + dz, STEM)
            tip(tips, x + dx, y + hh, z + dz, 4)


def build(kind: int, h: int, seed: int):
    cols, tips = {}, {}
    base = 1
    trunk = {}
    ang = rnd(seed, 1, 701) * math.pi * 2
    sway = 0.35 + rnd(seed, 2, 702) * 0.50
    px = pz = 0.0
    for y in range(1, h + 1):
        ang += (rnd(seed, y, 703) - 0.5) * 1.15
        pull = 1.0 - y / (h + 3)
        px += math.cos(ang) * sway * pull
        pz += math.sin(ang) * sway * pull
        ix, iz = int(round(px)), int(round(pz))
        trunk[y] = (ix, iz)
        put(cols, ix, base + y - 1, iz, STEM)
        if y <= 2:
            bx = int(round(math.cos(ang + math.pi / 2) * 1.0))
            bz = int(round(math.sin(ang + math.pi / 2) * 1.0))
            if bx or bz:
                put(cols, ix + bx, base + y - 1, iz + bz, STEM)
            if y == 1:
                put(cols, ix, base, iz, PLATE)

    branches = 4 if kind == TABLE else 3
    for b in range(branches):
        y0 = 2 + int(h * (0.42 + 0.45 * rnd(seed, b, 705)))
        y0 = min(y0, max(1, h - 1))
        p = trunk.get(y0)
        if p is None:
            continue
        a = b * math.pi * 2 / branches + rnd(seed, b, 706) * 1.1
        curve = (rnd(seed, b, 707) - 0.5) * 1.1
        ln = 3 + int(rnd(seed, b, 708) * 3)
        ex, ey, ez, ea = limb(cols, p[0], base + y0 - 1, p[1], a, curve, ln, 0.95, 0.88)
        forks = 1
        for f in range(forks):
            fa = ea + (-0.75 if f == 0 else 0.75) + (rnd(seed, b * 7 + f, 709) - 0.5) * 0.7
            flen = 2 + int(rnd(seed, b * 7 + f, 710) * 3)
            fx, fy, fz, _ = limb(cols, ex, ey, ez, fa, (rnd(seed, f, 722) - 0.5) * 0.9, flen, 0.95, 0.95)
            crown(cols, tips, kind, fx, fy, fz, seed + b * 31 + f * 131)
        crown(cols, tips, kind, ex, ey, ez, seed + b * 31 + 977)
    if h in trunk:
        crown(cols, tips, kind, trunk[h][0], base + h - 1, trunk[h][1], seed + 991)
    return cols, tips


# ---------------------------------------------------------------- изометрический рендер
S = 9          # полуширина ромба
HH = S // 2    # полувысота ромба
COLS_STEM = (232, 120, 168)
COLS_PLATE = (255, 214, 232)
COLS_TIP = (150, 245, 210)


def _shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


def render(cols, tips, out_png, size=(520, 560)):
    blocks = {}
    for key, lst in cols.items():
        dx = (key >> 8) - 16
        dz = (key & 0xFF) - 16
        for a, b, typ in lst:
            for y in range(a, b + 1):
                blocks[(dx, y, dz)] = COLS_STEM if typ == STEM else COLS_PLATE
    for key, lst in tips.items():
        dx = (key >> 8) - 16
        dz = (key & 0xFF) - 16
        for y, _blk in lst:
            blocks[(dx, y, dz)] = COLS_TIP

    xs = [k[0] for k in blocks]
    zs = [k[2] for k in blocks]
    ys = [k[1] for k in blocks]
    if not blocks:
        return
    cx = (max(xs) + min(xs)) / 2
    cz = (max(zs) + min(zs)) / 2
    # масштаб подгоняем под габариты: иначе растение занимает 1/10 кадра и ничего не видно
    span_x = (max(xs) - min(xs) + max(zs) - min(zs) + 2) * S * 2
    span_y = (max(ys) - min(ys) + 2) * (S - 1) + (max(xs) - min(xs) + max(zs) - min(zs) + 2) * HH
    scale = min(3.4, min(size[0] / max(1, span_x), size[1] / max(1, span_y)))
    S2 = max(3, int(S * scale))
    H2 = max(2, S2 // 2)
    ox, oy = size[0] // 2, size[1] - 40
    top = max(ys)

    img = Image.new("RGB", size, (16, 20, 30))
    d = ImageDraw.Draw(img)
    # рисуем снизу вверх и от дальних к ближним
    for (x, y, z) in sorted(blocks, key=lambda p: (p[1], p[0] + p[2])):
        rx, rz = x - cx, z - cz
        sx = ox + (rx - rz) * S2
        sy = oy + (rx + rz) * H2 - (y - 1) * (S2 - 1)
        col = blocks[(x, y, z)]
        top_c = col
        left_c = _shade(col, 0.62)
        right_c = _shade(col, 0.42)
        d.polygon([(sx, sy - H2), (sx + S2, sy), (sx, sy + H2), (sx - S2, sy)], fill=top_c)
        d.polygon([(sx - S2, sy), (sx, sy + H2), (sx, sy + H2 + (S2 - 1)), (sx - S2, sy + (S2 - 1))], fill=left_c)
        d.polygon([(sx + S2, sy), (sx, sy + H2), (sx, sy + H2 + (S2 - 1)), (sx + S2, sy + (S2 - 1))], fill=right_c)
    _ = top
    img.save(out_png)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="/home/user/preview_coral")
    args = ap.parse_args()
    os.makedirs(args.out, exist_ok=True)
    tiles = []
    for kind in (STAGHORN, TABLE, FAN, WHIP, TUBES, SPIRAL):
        seed = 424242 + kind * 7919
        h = 8 + (kind % 4)
        cols, tips = build(kind, h, seed)
        blocks = sum(b - a + 1 for lst in cols.values() for a, b, _t in lst)
        ncols = len(cols)
        print(f"{NAMES[kind]:9s} h={h} blocks={blocks} cols={ncols} tips={sum(len(v) for v in tips.values())}")
        png = f"{args.out}/{NAMES[kind]}.png"
        render(cols, tips, png)
        tiles.append(Image.open(png))
    w = max(t.width for t in tiles)
    hh = max(t.height for t in tiles)
    sheet = Image.new("RGB", (w * 3, hh * 2), (10, 12, 18))
    for i, t in enumerate(tiles):
        sheet.paste(t, ((i % 3) * w, (i // 3) * hh))
    sheet.save(f"{args.out}/sheet.png")
    print("sheet:", f"{args.out}/sheet.png")


if __name__ == "__main__":
    main()
