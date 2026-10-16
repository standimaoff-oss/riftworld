#!/usr/bin/env python3
"""Превью морской арены дракона (зеркало dragonArenaDressing из ElisarChunkGenerator)."""
import math, os, random, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import nbtio
SEA, DECK = 62, 76
OX, OZ = 40, 40
V = {}
def put(x, y, z, b): V[(x, y, z)] = b
r = random.Random(0xD4A60)
for x in range(-30, 61):
    for z in range(-30, 61):
        V[(OX + x, SEA, OZ + z)] = "elisar_water"
        V[(OX + x, SEA - 1, OZ + z)] = "elisar_seabed"
for y in range(SEA - 12, DECK + 1):
    rad = max(2, round(11 - (y - (SEA - 12)) * 0.34))
    for dx in range(-rad, rad + 1):
        for dz in range(-rad, rad + 1):
            if dx * dx + dz * dz <= rad * rad: put(OX + dx, y, OZ + dz, "keep_stone")
for dx in range(-9, 10):
    for dz in range(-9, 10):
        if dx * dx + dz * dz <= 81: put(OX + dx, DECK, OZ + dz, "keep_stone")
        if dx * dx + dz * dz <= 64: put(OX + dx, DECK + 1, OZ + dz, "keep_stone_cracked")
ph = [13, 6, 12, 4, 13, 8, 5, 11]; pp = []
for i in range(8):
    a = i * math.pi / 4 + 0.2
    px, pz = OX + round(math.cos(a) * 7), OZ + round(math.sin(a) * 7)
    pp.append((px, pz, ph[i]))
    for y in range(DECK + 1, DECK + ph[i] + 1): put(px, y, pz, "keep_stone")
    if ph[i] > 10: put(px, DECK + ph[i] + 1, pz, "keep_stone_stairs")
for i in range(8):
    a1, a2 = pp[i], pp[(i + 1) % 8]
    if a1[2] < 10 or a2[2] < 10: continue
    top = DECK + min(a1[2], a2[2])
    for t in range(11):
        u = t / 10
        x = round(a1[0] + (a2[0] - a1[0]) * u); z = round(a1[1] + (a2[1] - a1[1]) * u)
        y = round(top + math.sin(u * math.pi) * 3.5)
        put(x, y, z, "keep_stone")
        if t == 5:
            for k in range(1, 6): put(x, y - k, z, "elisar_chain")
for dx in (-1, 0, 1):
    for dz in (-1, 0, 1): put(OX + dx, DECK + 2, OZ + dz, "keep_stone_stairs")
put(OX, DECK + 3, OZ, "keep_stone"); put(OX, DECK + 4, OZ, "keep_cage_lantern")
for t in range(47):
    a = t * 0.42; rad = 10.5 - t * 0.06
    y = DECK + 1 - t // 3
    if y < SEA - 2: break
    put(OX + round(math.cos(a) * rad), y, OZ + round(math.sin(a) * rad), "keep_stone_stairs")
for i in range(12):
    a = i * math.pi / 6 + r.random() * 0.4; d = 16 + r.random() * 10
    x, z = OX + round(math.cos(a) * d), OZ + round(math.sin(a) * d)
    h = 2 + r.randint(0, 4)
    for y in range(SEA - 3, SEA + h + 1): put(x, y, z, "keep_stone_mossy")
    if r.random() < 0.5: put(x, SEA + h + 1, z, "elisar_ivy")
B = {p: ("riftworld:" + b, ()) for p, b in V.items()}
out = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../reports/shots_rev7/arena_scene.nbt"))
n, np_ = nbtio.write_template(out, (101, 100, 101), B, {})
print("arena preview blocks", n)
