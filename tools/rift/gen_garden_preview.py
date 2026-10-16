#!/usr/bin/env python3
"""Техническое превью биома 07 «Висячие сады»: зеркалит формулы генератора рев7
(Island.raw с hill=1.8, terraceTop STEP=5, пруды по шуму, водопады с карнизов, лианы-занавесы,
мини-деревья 4-6 блоков) и отдаёт воксельную сцену в shot_vox. Это превью ФОРМУЛ, не данные мира."""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import nbtio  # noqa: E402

R = 90
BASE = 120.0
HILL = 1.8
OCEAN = 62
STEP = 5.0
P1, P2, P3 = 1.1, 2.7, 4.2
S = int(R * 1.45)
OX, OZ = S, S                      # offset to positive coords
rnd = random.Random(777)
V = {}


def smooth(u):
    return u * u * (3 - 2 * u)


def shape_r(th):
    return R * (1 + 0.07 * math.sin(3 * th + P1) + 0.045 * math.sin(5 * th + P2) + 0.025 * math.sin(11 * th + P3))


def raw(x, z):
    h = BASE + HILL * (1.6 * math.sin(x * 0.043 + P1) * math.cos(z * 0.037 + P2) + 0.9 * math.sin((x - z) * 0.071 + P3))
    t = 1 - math.hypot(x, z) / shape_r(math.atan2(z, x))
    if t < 0.12:
        k = 1 - max(t, 0) / 0.12
        h -= k * k * 4
    return h, t


def terrace(top):
    h = top - (OCEAN + 1)
    if h <= 0:
        return top
    L = math.floor(h / STEP)
    band = h - L * STEP
    rise = smooth(band / 1.2) if band <= 1.2 else 1.0
    return OCEAN + 1 + (L + rise) * STEP


def vn(x, z, seed):
    return 0.5 + 0.5 * math.sin(x * 0.9 + seed) * math.sin(z * 0.7 + seed * 1.7)


tops = {}
for x in range(-S, S + 1):
    for z in range(-S, S + 1):
        rw, t = raw(x, z)
        if t <= 0.0:
            tops[(x, z)] = None
            continue
        dome_h = BASE + HILL * (1.6 * math.sin(x * 0.043 + P1) * math.cos(z * 0.037 + P2)
                                + 0.9 * math.sin((x - z) * 0.071 + P3))
        dome = min(1.0, max(0.0, t * 1.15)) ** 0.62
        hgt = OCEAN + 2 + max(0.0, dome_h - OCEAN - 2) * dome
        hgt += (math.sin(x * 0.35 + 141) * math.sin(z * 0.31 + 97)) * 1.5
        tops[(x, z)] = int(math.floor(terrace(hgt)))

# море и дно
for x in range(-S, S + 1):
    for z in range(-S, S + 1):
        for y in range(OCEAN - 4, OCEAN + 1):
            V[(OX + x, y, OZ + z)] = "elisar_water" if y <= OCEAN else None
        V[(OX + x, OCEAN - 5, OZ + z)] = "elisar_seabed"

# колонны острова: террасы, пруды, кромки
for (x, z), top in tops.items():
    if top is None:
        continue
    pond = vn(x * 0.09 * 10, z * 0.09 * 10, 131) > 0.62 and math.hypot(x, z) < R * 0.95 and OCEAN + 2 < top < OCEAN + 80
    if pond:
        for y in range(top - 2, top + 1):
            V[(OX + x, y, OZ + z)] = "elisar_water"
        for y in range(OCEAN - 5, top - 2):
            V[(OX + x, y, OZ + z)] = "elisar_stone"
        continue
    for y in range(OCEAN - 5, top + 1):
        V[(OX + x, y, OZ + z)] = "elisar_stone"
    V[(OX + x, top, OZ + z)] = "elisar_grass" if top > OCEAN + 1 else "elisar_seabed"
    # лианы-занавесы с губы террасы
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        nb = tops.get((x + dx, z + dz), 0)
        if nb is not None and top - nb >= 4:
            for k in range(1, min(5, top - nb)):
                V[(OX + x + dx, top - k, OZ + z + dz)] = "elisar_ivy"
            break
    # водопад: сосед снаружи острова ниже, а наша полка высокая
    if top > OCEAN + 8 and math.hypot(x, z) > R * 0.55:
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nt = tops.get((x + dx, z + dz))
            if nt is None and vn(x * 1.3, z * 1.3, 131) > 0.55:
                for y in range(OCEAN + 1, top + 1):
                    V[(OX + x + dx, y, OZ + z + dz)] = "elisar_water"
                break

# мини-деревья 4-6 блоков и цветы
for _ in range(34):
    a = rnd.random() * 6.283
    d = (0.12 + rnd.random() * 0.7) * R
    x, z = int(math.cos(a) * d), int(math.sin(a) * d)
    top = tops.get((x, z))
    if top is None or top <= OCEAN + 2:
        continue
    h = 4 + rnd.randint(0, 2)
    for k in range(1, h + 1):
        V[(OX + x, top + k, OZ + z)] = "sizo_log" if rnd.random() < 0.7 else "ambra_log"
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            for dy in range(0, 3):
                if abs(dx) + abs(dz) + dy <= 3 and (dx or dz or dy):
                    V[(OX + x + dx, top + h + dy, OZ + z + dz)] = "ambra_leaves" if rnd.random() < 0.8 else "sizo_leaves"
for _ in range(400):
    x = rnd.randint(-int(R * 0.9), int(R * 0.9))
    z = rnd.randint(-int(R * 0.9), int(R * 0.9))
    top = tops.get((x, z))
    if top is None or top <= OCEAN + 2:
        continue
    if rnd.random() < 0.5:
        V[(OX + x, top + 1, OZ + z)] = "elisar_flowers" if rnd.random() < 0.6 else "elisar_tall_grass"

B = {pos: ("riftworld:" + b, ()) for pos, b in V.items() if b}
out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../reports/shots_rev7/garden_scene.nbt")
n, np_ = nbtio.write_template(os.path.normpath(out), (2 * S + 1, 140, 2 * S + 1), B, {})
print("garden preview blocks", n, "palette", np_)
