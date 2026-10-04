#!/usr/bin/env python3
"""Riftworld 16x16 block textures + model textures, drawn procedurally (numpy + PIL, no 3rd-party assets).

Lab palette: off-white panels, cool grey concrete, teal accent band, hazard yellow; "aged" variants get dust,
water stains, cracks and dead brown-grey moss (the complex has been abandoned for decades).
Output: mods/riftworld/src/main/resources/assets/riftworld/textures/{block,item}/ and tools/rift/tex_models/
"""
import os, math, random
import numpy as np
from PIL import Image

ROOT = "/home/user/mods/riftworld/src/main/resources/assets/riftworld/textures"
BLK = os.path.join(ROOT, "block")
ITM = os.path.join(ROOT, "item")
MOD = "/home/user/tools/rift/tex_models"
for d in (BLK, ITM, MOD):
    os.makedirs(d, exist_ok=True)
R = random.Random(27)
N = 16


def rgb(h):
    h = h.lstrip('#')
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], float)


def noise(seed, amp, n=N):
    r = np.random.RandomState(seed)
    return (r.rand(n, n) - 0.5) * 2 * amp


def base(col, seed, amp=6, n=N):
    a = np.zeros((n, n, 4))
    a[..., :3] = rgb(col)
    a[..., :3] += noise(seed, amp, n)[..., None]
    a[..., 3] = 255
    return a


def save(a, name, d=BLK):
    Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(d, name + ".png"))


def shade(a, x, y, k):
    if 0 <= x < a.shape[1] and 0 <= y < a.shape[0]:
        a[y, x, :3] *= k


def setp(a, x, y, col, alpha=255):
    if 0 <= x < a.shape[1] and 0 <= y < a.shape[0]:
        a[y, x, :3] = rgb(col) if isinstance(col, str) else col
        a[y, x, 3] = alpha


def bevel(a, x0, y0, x1, y1, hi=1.10, lo=0.84):
    for x in range(x0, x1 + 1):
        shade(a, x, y0, hi)
        shade(a, x, y1, lo)
    for y in range(y0 + 1, y1):
        shade(a, x0, y, hi)
        shade(a, x1, y, lo)


def age(a, seed, dust=0.25, stains=2, cracks=1, moss=0.0):
    """Dust gradient, vertical water stains, cracks, dead moss speckles."""
    r = np.random.RandomState(seed)
    n = a.shape[0]
    ys = np.linspace(0, 1, n)[:, None]
    dirt = rgb('#8a7f6e')
    m = dust * (0.4 + 0.6 * ys) * (0.6 + 0.8 * r.rand(n, n))
    a[..., :3] = a[..., :3] * (1 - m[..., None]) + dirt * m[..., None]
    for _ in range(stains):
        x = r.randint(1, n - 1)
        L = r.randint(4, n)
        for y in range(L):
            k = 0.86 + 0.08 * y / L
            shade(a, x, y, k)
            if r.rand() < 0.3:
                x = min(n - 1, max(0, x + r.choice((-1, 1))))
    for _ in range(cracks):
        x, y = r.randint(2, n - 2), r.randint(2, n - 2)
        for _ in range(r.randint(5, 10)):
            setp(a, x, y, a[y % n, x % n, :3] * 0.45)
            if r.rand() < 0.3 and 0 <= x + 1 < n:
                shade(a, x + 1, y, 1.12)
            x += r.choice((-1, 0, 1))
            y += r.choice((0, 1))
            if not (0 <= x < n and 0 <= y < n):
                break
    if moss > 0:
        mc = [rgb('#6b6448'), rgb('#57533d'), rgb('#7a6f52')]
        for _ in range(int(moss * n * n)):
            y = int(n - 1 - abs(r.randn()) * n * 0.3) % n
            x = r.randint(0, n)
            a[y, x, :3] = mc[r.randint(0, 3)] + r.randn() * 4
    return a


# ---------------------------------------------------------------- lab panels
def lab_panel(stripe=False):
    a = base('#d9dcdc', 1, 4)
    # two panels per block, recessed seam
    for y in range(N):
        for x in (0, 15):
            shade(a, x, y, 0.80)
    for x in range(N):
        shade(a, x, 0, 0.82)
        shade(a, x, 15, 0.78)
        shade(a, x, 7, 0.86)
        shade(a, x, 8, 1.06)
    bevel(a, 1, 1, 14, 6, 1.05, 0.93)
    bevel(a, 1, 9, 14, 14, 1.05, 0.93)
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        setp(a, x, y, '#a9aeb0')
    if stripe:
        for x in range(N):
            for y in (10, 11, 12):
                a[y, x, :3] = rgb('#2f8f8c') * (1.06 if y == 10 else (0.86 if y == 12 else 1)) + noise(3, 3)[y, x]
            setp(a, x, 9, '#c9cdcd')
    return a


save(lab_panel(), "lab_panel")
save(age(lab_panel(), 11, 0.35, 3, 2, 0.06), "lab_panel_dirty")
save(lab_panel(True), "lab_panel_stripe")
save(age(lab_panel(True), 12, 0.35, 3, 1, 0.05), "lab_panel_stripe_dirty")


# ---------------------------------------------------------------- floor tiles
def lab_tile():
    a = base('#c4c8c6', 4, 3)
    for k in range(N):
        for g in (0, 8):
            shade(a, g, k, 0.78)
            shade(a, k, g, 0.78)
    for (x0, y0) in ((1, 1), (9, 1), (1, 9), (9, 9)):
        bevel(a, x0, y0, x0 + 6, y0 + 6, 1.04, 0.95)
    # faint speckle (terrazzo)
    r = np.random.RandomState(5)
    for _ in range(14):
        x, y = r.randint(0, N), r.randint(0, N)
        if x % 8 and y % 8:
            shade(a, x, y, 0.9 if r.rand() < .5 else 1.08)
    return a


save(lab_tile(), "lab_tile")
t = age(lab_tile(), 21, 0.30, 0, 3, 0.08)
# a broken tile corner
for (x, y) in ((10, 10), (11, 10), (10, 11), (12, 10), (10, 12), (11, 11)):
    setp(t, x, y, '#6f6a5f')
save(t, "lab_tile_cracked")


# ---------------------------------------------------------------- concrete
def concrete():
    a = base('#9b9e9f', 7, 5)
    a[..., :3] += noise(8, 3)[..., None]
    for (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        setp(a, x, y, '#6e7172')
        shade(a, x + 1, y + 1, 1.08)
    for x in range(N):
        shade(a, x, 15, 0.9)
    return a


save(concrete(), "lab_concrete")
save(age(concrete(), 31, 0.25, 2, 3, 0.12), "lab_concrete_cracked")


def facade():
    a = base('#7d8487', 9, 4)
    for x in range(N):
        shade(a, x, 0, 0.8)
        shade(a, x, 1, 1.1)
    for y in range(N):
        shade(a, 0, y, 0.84)
        shade(a, 8, y, 0.84)
        shade(a, 9, y, 1.08)
    for y in (4, 11):
        setp(a, 4, y, '#5a6063')
        setp(a, 12, y, '#5a6063')
    return a


save(age(facade(), 33, 0.2, 3, 1, 0.05), "lab_facade")

# ceiling
a = base('#e3e5e4', 41, 3)
for y in range(1, N, 3):
    for x in range(1 + (y // 3) % 2, N, 3):
        setp(a, x, y, '#a7abab')
for k in range(N):
    shade(a, k, 0, 0.8)
    shade(a, 0, k, 0.8)
save(a, "lab_ceiling")
save(age(a.copy(), 42, 0.3, 4, 1, 0.0), "lab_ceiling_dirty")


# ---------------------------------------------------------------- steel floor / diamond plate
def steel(rust=0.0):
    a = base('#8e9497', 51, 5)
    for y in range(N):
        for x in range(N):
            if (x + 2 * y) % 8 == 0 and y % 4 == 1:
                setp(a, x, y, '#b5bcbf')
                shade(a, x + 1, y, 0.85)
            if (x - 2 * y) % 8 == 4 and y % 4 == 3:
                setp(a, x, y, '#b5bcbf')
                shade(a, x + 1, y, 0.85)
    bevel(a, 0, 0, 15, 15, 1.06, 0.82)
    if rust:
        r = np.random.RandomState(52)
        rc = [rgb('#8a4b2a'), rgb('#a35e32'), rgb('#6e3b22')]
        for _ in range(int(rust * 256)):
            x, y = r.randint(0, N), r.randint(0, N)
            a[y, x, :3] = rc[r.randint(0, 3)]
    return a


save(steel(), "steel_floor")
save(steel(0.3), "steel_floor_rusty")

# hazard stripes
a = base('#e0b020', 61, 5)
for y in range(N):
    for x in range(N):
        if ((x + y) // 4) % 2:
            a[y, x, :3] = rgb('#262626') + noise(62, 4)[y, x]
bevel(a, 0, 0, 15, 15, 1.05, 0.8)
save(a, "hazard_block")
save(age(a.copy(), 63, 0.35, 2, 1, 0.0), "hazard_block_worn")


# ---------------------------------------------------------------- grates (cutout)
def grate(rust=0.0):
    a = np.zeros((N, N, 4))
    col = rgb('#6c7276')
    for y in range(N):
        for x in range(N):
            if x % 4 in (0, 1) or y % 4 in (0, 1) or x in (0, 15) or y in (0, 15):
                a[y, x, :3] = col * (1.12 if (x % 4 == 0 or y % 4 == 0) else 0.9)
                a[y, x, 3] = 255
    a[..., :3] += noise(71, 5)[..., None]
    if rust:
        r = np.random.RandomState(72)
        for _ in range(int(rust * 256)):
            x, y = r.randint(0, N), r.randint(0, N)
            if a[y, x, 3] > 0:
                a[y, x, :3] = [rgb('#8a4b2a'), rgb('#9c5a30'), rgb('#5e3420')][r.randint(0, 3)]
    return a


save(grate(), "metal_grate")
save(grate(0.35), "rusty_grate")

# framed lab glass (translucent)
a = np.zeros((N, N, 4))
a[..., :3] = rgb('#bfe3e6')
a[..., 3] = 70
for k in range(N):
    for (x, y) in ((k, 0), (k, 15), (0, k), (15, k)):
        setp(a, x, y, '#8d9699')
for k in range(3, 7):
    setp(a, k + 6, k, '#ffffff', 120)
    setp(a, k + 7, k, '#ffffff', 90)
save(a, "lab_glass")
b = a.copy()
for (x, y) in ((7, 9), (8, 8), (9, 9), (8, 10), (6, 11), (10, 7), (11, 6), (5, 12)):
    setp(b, x, y, '#e8f4f4', 200)
save(age(b, 81, 0.15, 0, 0, 0.0), "lab_glass_cracked")


# ---------------------------------------------------------------- lamps
def lamp(on, red=False):
    a = base('#cfd2d2', 91, 3)
    bevel(a, 0, 0, 15, 15, 1.05, 0.8)
    for y in range(3, 13):
        for x in range(2, 14):
            if on:
                c = rgb('#ff5a4a') if red else rgb('#f4fbff')
                a[y, x, :3] = c * (0.92 + 0.08 * math.sin((x - 2) / 11 * math.pi))
            else:
                a[y, x, :3] = rgb('#7b7f80') if not red else rgb('#5a3532')
                a[y, x, :3] += noise(92, 4)[y, x]
    for x in range(2, 14):
        shade(a, x, 3, 0.85)
        shade(a, x, 12, 0.85)
    return a


save(lamp(False), "lab_lamp")
save(lamp(True), "lab_lamp_on")
save(lamp(False, True), "emergency_lamp")
save(lamp(True, True), "emergency_lamp_on")

# pipes
a = base('#7f8a8c', 101, 4)
for y in range(N):
    for x in range(N):
        a[y, x, :3] *= 0.8 + 0.35 * math.sin((x + 0.5) / 16 * math.pi)
for x in range(N):
    for y in (6, 7):
        a[y, x, :3] = rgb('#2f8f8c') * (0.8 + 0.35 * math.sin((x + .5) / 16 * math.pi))
save(a, "pipe_side")
a = base('#6d7678', 102, 4)
for y in range(N):
    for x in range(N):
        d = math.hypot(x - 7.5, y - 7.5)
        if d < 4:
            a[y, x, :3] = rgb('#2a2e30')
        elif d < 5.2:
            a[y, x, :3] *= 1.15
save(a, "pipe_end")

# shaft wall (ribbed dark steel)
a = base('#4b5154', 111, 5)
for y in range(N):
    for x in range(N):
        if x % 4 == 0:
            a[y, x, :3] *= 1.25
        elif x % 4 == 3:
            a[y, x, :3] *= 0.75
for (x, y) in ((2, 2), (2, 13), (10, 2), (10, 13)):
    setp(a, x, y, '#7c8487')
save(age(a, 112, 0.15, 3, 0, 0.0), "shaft_wall")

# debris / rubble layer
a = base('#7c776d', 121, 10)
r = np.random.RandomState(122)
for _ in range(40):
    x, y = r.randint(0, N), r.randint(0, N)
    a[y, x, :3] = [rgb('#b9b6ae'), rgb('#57534c'), rgb('#9a8f7c'), rgb('#d1d4d2')][r.randint(0, 4)]
save(a, "debris")

# dead vines (cutout)
a = np.zeros((N, N, 4))
vc = [rgb('#6d6450'), rgb('#57503f'), rgb('#857a5f'), rgb('#4a4436')]
for vx in (2, 7, 12):
    x = vx
    for y in range(N):
        if r.rand() < 0.9:
            setp(a, x, y, vc[r.randint(0, 4)])
            if r.rand() < 0.35:
                setp(a, x + r.choice((-1, 1)), y, vc[r.randint(0, 4)])
        if r.rand() < 0.25:
            x = max(0, min(15, x + r.choice((-1, 1))))
save(a, "dead_vines")
# dead moss carpet
a = base('#6a6449', 131, 9)
for _ in range(60):
    x, y = r.randint(0, N), r.randint(0, N)
    a[y, x, :3] = vc[r.randint(0, 4)]
for _ in range(10):
    x, y = r.randint(0, N), r.randint(0, N)
    a[y, x, 3] = 0
save(a, "dead_moss")

# part block (invisible helper) - fully transparent
save(np.zeros((N, N, 4)), "part")


# ---------------------------------------------------------------- items
def stone_icon(col, glow, name):
    a = np.zeros((N, N, 4))
    pts = [(8, 1), (12, 4), (13, 9), (10, 14), (6, 14), (3, 10), (4, 4)]
    from PIL import ImageDraw
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.polygon(pts, fill=tuple(int(v) for v in rgb(col)) + (255,), outline=(40, 44, 48, 255))
    a = np.array(im).astype(float)
    for y in range(N):
        for x in range(N):
            if a[y, x, 3] > 0 and (x, y) not in pts:
                k = 1.0 + 0.25 * ((8 - x) + (8 - y)) / 14
                a[y, x, :3] *= k
    for (x, y) in ((7, 5), (8, 6), (8, 7), (9, 8), (8, 9), (7, 10)):
        setp(a, x, y, glow)
    setp(a, 6, 4, '#ffffff')
    save(a, name, ITM)


stone_icon('#3f4a57', '#8ff3ff', "resonance_stone")

# ---------------------------------------------------------------- model textures (Blender, 64x64 / 128x128)
def mtex(name, col, seed, amp=6, n=64, f=None):
    a = base(col, seed, amp, n)
    if f:
        f(a, n)
    Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA").convert("RGB").save(os.path.join(MOD, name + ".png"))


def panel_lines(a, n):
    for k in range(n):
        for g in range(0, n, 32):
            shade(a, g, k, 0.82)
            shade(a, k, g, 0.82)
    age(a, 201, 0.18, 6, 2, 0.0)


def wood(a, n):
    for y in range(n):
        for x in range(n):
            a[y, x, :3] *= 0.92 + 0.10 * math.sin(x * 0.9 + 2 * math.sin(y * 0.15))
    for y in range(0, n, 16):
        for x in range(n):
            shade(a, x, y, 0.6)
            shade(a, x, y + 1, 0.85)
    age(a, 202, 0.2, 4, 1, 0.0)


def metal(a, n):
    for y in range(n):
        a[y, :, :3] *= 0.95 + 0.06 * math.sin(y * 1.7)
    age(a, 203, 0.2, 5, 0, 0.0)


def rust(a, n):
    r = np.random.RandomState(204)
    for _ in range(n * n // 5):
        x, y = r.randint(0, n), r.randint(0, n)
        a[y, x, :3] = [rgb('#8a4b2a'), rgb('#a35e32'), rgb('#6e3b22')][r.randint(0, 3)]


def screen(a, n):
    a[..., :3] = rgb('#0b2a2c')
    for y in range(n):
        if y % 2 == 0:
            a[y, :, :3] *= 0.85
    # UI: header bar, graph, text lines, circular gauge
    for x in range(4, n - 4):
        a[5, x, :3] = rgb('#3ff0e0')
        a[6, x, :3] = rgb('#20a89c')
    for x in range(6, n // 2):
        yv = int(n * 0.55 + 8 * math.sin(x * 0.45))
        a[yv, x, :3] = rgb('#7dffb0')
    for row in range(3):
        for x in range(n // 2 + 4, n - 6):
            if (x * 7 + row * 3) % 5:
                a[14 + row * 6, x, :3] = rgb('#3ff0e0') * 0.8
    for k in range(60):
        t = k / 60 * 2 * math.pi
        x, y = int(n * 0.72 + 9 * math.cos(t)), int(n * 0.72 + 9 * math.sin(t))
        a[y, x, :3] = rgb('#ffb347')


def screen_dead(a, n):
    a[..., :3] = rgb('#151a1b')
    a[..., :3] += noise(205, 6, n)[..., None]
    for k in range(n):
        x = int(n * 0.3 + k * 0.4)
        if x < n:
            a[k, x, :3] = rgb('#5a6466')


def hazard(a, n):
    for y in range(n):
        for x in range(n):
            if ((x + y) // 8) % 2:
                a[y, x, :3] = rgb('#262626')
    age(a, 206, 0.25, 3, 0, 0.0)


def deadplant(a, n):
    r = np.random.RandomState(207)
    for _ in range(n * 3):
        x, y = r.randint(0, n), r.randint(0, n)
        a[y, x, :3] = [rgb('#6d6450'), rgb('#4e4838'), rgb('#8b7d5e'), rgb('#3a352a')][r.randint(0, 4)]


def labels(a, n):
    # crate stencil band + number
    for y in range(n // 2 - 6, n // 2 + 6):
        for x in range(8, n - 8):
            if y in (n // 2 - 6, n // 2 + 5) or x in (8, n - 9):
                a[y, x, :3] = rgb('#e8e1c8')
    for x in range(14, n - 14, 3):
        for y in range(n // 2 - 2, n // 2 + 2):
            a[y, x, :3] = rgb('#e8e1c8')


mtex("m_labwhite", '#d8dbdb', 210, 4, 64, panel_lines)
mtex("m_darkmetal", '#3d4346', 211, 5, 64, metal)
mtex("m_steel", '#9aa1a4', 212, 6, 64, metal)
mtex("m_rust", '#6f5040', 213, 8, 64, rust)
mtex("m_wood", '#9a7a50', 214, 6, 64, wood)
mtex("m_teal", '#2f8f8c', 215, 4, 64, metal)
mtex("m_screen", '#0b2a2c', 216, 2, 64, screen)
mtex("m_screen_dead", '#151a1b', 217, 2, 64, screen_dead)
mtex("m_hazard", '#e0b020', 218, 5, 64, hazard)
mtex("m_deadplant", '#5f5946', 219, 8, 64, deadplant)
mtex("m_crate_olive", '#58603f', 220, 6, 64, labels)
mtex("m_rubber", '#222426', 221, 4, 64)
mtex("m_copper", '#b06a3c', 222, 8, 64, metal)

# ================================================================ r2: decor / capsule textures
# dust & grime patch (carpet, cutout) - removed by the cleaning strip
a = np.zeros((N, N, 4))
r2 = np.random.RandomState(301)
for y in range(N):
    for x in range(N):
        d = math.hypot(x - 7.5, y - 7.5) / 8.0 + r2.rand() * 0.45
        if d < 1.0:
            c = rgb('#5d4a35') * (0.85 + 0.3 * r2.rand())
            if r2.rand() < 0.15:
                c = rgb('#7b6a50')
            a[y, x, :3] = c
            a[y, x, 3] = 255 if d < 0.8 else 170
save(a, "dirt_pile")
# handrail / railing (iron-bars style, cutout)
a = np.zeros((N, N, 4))
for x in range(N):
    for y in (1, 2):
        setp(a, x, y, '#2f8f8c' if y == 1 else '#256f6c')
    setp(a, x, 14, '#5d6366')
for x in (1, 5, 9, 13):
    for y in range(3, 14):
        setp(a, x, y, '#8d9497')
        setp(a, x + 1, y, '#6c7276')
a[..., :3] += noise(302, 4)[..., None] * (a[..., 3:4] > 0)
save(a, "lab_railing")
# stair riser with hazard nosing
a = base('#d9dcd8', 303, 4)
for y in range(0, 4):
    for x in range(N):
        a[y, x, :3] = rgb('#e0b020') if ((x + y) // 3) % 2 == 0 else rgb('#262626')
for x in range(N):
    shade(a, x, 4, 0.7)
bevel(a, 0, 5, 15, 15)
save(age(a, 304, 0.18, 1, 0, 0.0), "lab_stair_side")
# wall vent
a = base('#9aa1a4', 305, 4)
bevel(a, 0, 0, 15, 15, 1.15, 0.7)
for y in range(3, 13, 2):
    for x in range(2, 14):
        a[y, x, :3] = rgb('#1e2224')
        shade(a, x, y + 1, 1.1)
save(age(a, 306, 0.25, 2, 0, 0.0), "wall_vent")


def mtex_rgba(name, a):
    Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(MOD, name + ".png"))


def notes(a, n):
    r = np.random.RandomState(310)
    for y in range(6, n - 4, 4):
        x = 4
        while x < n - 6:
            L = r.randint(2, 7)
            for k in range(L):
                if r.rand() < 0.85:
                    a[y, x + k, :3] = rgb('#3a3f6a') if r.rand() < 0.7 else rgb('#7a2a2a')
            x += L + r.randint(1, 3)
    for k in range(n):
        shade(a, k, 2, 0.85)
    age(a, 311, 0.25, 3, 0, 0.0)


def cork(a, n):
    r = np.random.RandomState(312)
    for _ in range(n * n // 3):
        x, y = r.randint(0, n), r.randint(0, n)
        a[y, x, :3] *= 0.75 + r.rand() * 0.5


def trash(a, n):
    r = np.random.RandomState(313)
    cols = ['#d8d2c0', '#7b6a50', '#3b4a3a', '#aa3333', '#5a6670', '#c9b35a', '#2c2c2c', '#e8e8e8']
    for _ in range(n * n // 2):
        x, y = r.randint(0, n - 2), r.randint(0, n - 2)
        c = rgb(cols[r.randint(0, len(cols))])
        a[y:y + 2, x:x + 2, :3] = c


def barrel(a, n):
    for y in range(n):
        if y % 21 in (0, 1):
            a[y, :, :3] *= 0.65
    rust(a, n)
    age(a, 314, 0.3, 6, 0, 0.0)


def siren(a, n):
    r = np.random.RandomState(315)
    for _ in range(n * n // 6):
        x, y = r.randint(0, n), r.randint(0, n)
        a[y, x, :3] = [rgb('#8a4b2a'), rgb('#6e3b22'), rgb('#9a2b22')][r.randint(0, 3)]
    age(a, 316, 0.3, 5, 1, 0.0)


def ext(a, n):
    age(a, 317, 0.15, 3, 0, 0.0)
    for y in range(n // 3, n // 3 + 10):
        for x in range(n // 4, 3 * n // 4):
            a[y, x, :3] = rgb('#e8e1c8')


mtex("m_notes", '#e9e4d2', 320, 3, 64, notes)
mtex("m_cork", '#a57c4f', 321, 10, 64, cork)
mtex("m_trash", '#6a6052', 322, 10, 64, trash)
mtex("m_barrel", '#2f5b8a', 323, 6, 64, barrel)
mtex("m_siren", '#a3271f', 324, 6, 64, siren)
mtex("m_extinguisher", '#c0261c', 325, 5, 64, ext)
mtex("m_cardboard", '#a07f55', 326, 7, 64, lambda a, n: age(a, 327, 0.2, 3, 0, 0.0))
mtex("m_crt", '#cfc8b4', 328, 4, 64, lambda a, n: age(a, 329, 0.35, 4, 0, 0.0))
mtex("m_capdeck", '#7d8588', 330, 5, 64, metal)
# hatch leaf: 5.5 x 11 blocks of grate (88 x 176 px, cutout) with rust patches
g1 = np.array(Image.open(os.path.join(BLK, "metal_grate.png")).convert("RGBA"), float)
g2 = np.array(Image.open(os.path.join(BLK, "rusty_grate.png")).convert("RGBA"), float)
leaf = np.zeros((176, 88, 4))
rr = np.random.RandomState(331)
for ty in range(11):
    for tx in range(6):
        t = g2 if rr.rand() < 0.35 else g1
        y0, x0 = ty * 16, tx * 16
        h, w = min(16, 176 - y0), min(16, 88 - x0)
        leaf[y0:y0 + h, x0:x0 + w] = t[:h, :w]
for k in range(88):
    for j in (0, 1, 174, 175):
        leaf[j, k] = [70, 74, 77, 255]
for j in range(176):
    for k in (0, 1, 86, 87):
        leaf[j, k] = [70, 74, 77, 255]
mtex_rgba("m_hatch", leaf)

print("textures OK", len(os.listdir(BLK)), "block,", len(os.listdir(MOD)), "model")
