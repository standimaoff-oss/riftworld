"""Rev 3 / C-2 textures: castle rooms (all generated, 16x16 blocks; small model textures for the Blender props).

Blocks:  castle_bookshelf (+ _top), castle_bars (dark forged prison bars), castle_carpet (red runner with a gold-black border),
         castle_cobweb.
Models:  m_c_tapestry (32x64 red hanging with the horned-skull emblem), m_c_linen, m_c_blacksteel (guards' blades),
         m_c_food (bread/roast atlas strip is not needed - solid colours are used), m_c_parch_text (open book pages).
"""
import os, sys
import numpy as np
sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image

ROOT = "/home/user/riftworld/mod/src/main/resources/assets/riftworld/textures/block"
TM = "/home/user/tools/rift/tex_models"


def rgb(h):
    h = h.lstrip('#')
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], float)


def save(a, name, d=ROOT):
    Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(d, name + ".png"))


def base(col, seed, amp=6, h=16, w=16):
    r = np.random.RandomState(seed)
    a = np.zeros((h, w, 4))
    a[..., :3] = rgb(col) + ((r.rand(h, w) - 0.5) * 2 * amp)[..., None]
    a[..., 3] = 255
    return a


# ------------------------------------------------------------------ bookshelf: dark oak frame, two shelves of worn spines
bs = base('#2a1d15', 4101, 3)
r = np.random.RandomState(4102)
SPINES = ['#5e1418', '#3b2a1c', '#2c3a52', '#6b4a1e', '#24321f', '#4a1c2a', '#7a6a4a', '#1f1a17']
for row, (y0, y1) in enumerate(((1, 7), (9, 15))):
    x = 1
    while x < 15:
        w = r.randint(1, 3)
        top = y0 + r.randint(0, 2)
        col = rgb(SPINES[r.randint(len(SPINES))])
        lean = r.rand() < 0.12
        for xx in range(x, min(15, x + w)):
            for yy in range(top, y1):
                bs[yy, xx, :3] = col * (0.85 + 0.15 * ((xx - x) == 0)) + r.randint(-6, 6)
            if r.rand() < 0.5:
                bs[top + 1, xx, :3] = rgb('#b38a3a')          # gilt band
        if lean:
            x += 1
        x += w
for y in (0, 7, 8, 15):
    bs[y, :, :3] = rgb('#3a2a1f') + r.randint(-5, 5, (16, 1))
bs[:, 0, :3] = bs[:, 15, :3] = rgb('#33251b')
save(bs, "castle_bookshelf")
bt = base('#3d2c22', 4103, 4)
for y in (3, 7, 11, 15):
    bt[y, :, :3] = rgb('#1f1612')
save(bt, "castle_bookshelf_top")

# ------------------------------------------------------------------ forged bars (alpha)
bars = np.zeros((16, 16, 4))
for x in (1, 2, 7, 8, 13, 14):
    bars[:, x, :3] = rgb('#2b2724') + (12 if x in (1, 7, 13) else 0)
    bars[:, x, 3] = 255
for y in (2, 3, 12, 13):
    bars[y, :, :3] = rgb('#262220'); bars[y, :, 3] = 255
for (x, y) in ((1, 2), (7, 2), (13, 2), (1, 12), (7, 12), (13, 12)):
    bars[y, x, :3] = rgb('#5a524a')                           # rivets
r = np.random.RandomState(4104)
for _ in range(14):
    x, y = r.randint(0, 16), r.randint(0, 16)
    if bars[y, x, 3] > 0:
        bars[y, x, :3] = rgb('#5b3420')                       # rust
save(bars, "castle_bars")

# ------------------------------------------------------------------ carpet (tileable runner: field + border on all sides)
cp = base('#5a1216', 4105, 4)
yy, xx = np.mgrid[0:16, 0:16]
d = np.abs(xx - 7.5) + np.abs(yy - 7.5)
cp[(d > 3.5) & (d < 4.6), :3] = rgb('#b3862e')                 # gold lozenge
cp[d < 1.6, :3] = rgb('#1c0f0e')
for k in (0, 15):
    cp[k, :, :3] = rgb('#1c0f0e'); cp[:, k, :3] = rgb('#1c0f0e')
for k in (1, 14):
    cp[k, 1:15, :3] = rgb('#9a7428'); cp[1:15, k, :3] = rgb('#9a7428')
save(cp, "castle_carpet")

# ------------------------------------------------------------------ cobweb (alpha)
cw = np.zeros((16, 16, 4))
c = (7.5, 7.5)
for a in range(8):
    t = np.linspace(0, 9, 30)
    ang = a * np.pi / 4 + 0.15
    for s in t:
        x, y = int(round(c[0] + np.cos(ang) * s)), int(round(c[1] + np.sin(ang) * s))
        if 0 <= x < 16 and 0 <= y < 16:
            cw[y, x] = (222, 222, 216, 200)
for rr in (2.5, 4.6, 6.8):
    for k in range(80):
        ang = k / 80 * 2 * np.pi
        x, y = int(round(c[0] + np.cos(ang) * rr * (1 + 0.12 * np.sin(ang * 8)))), int(round(c[1] + np.sin(ang) * rr * (1 + 0.12 * np.sin(ang * 8))))
        if 0 <= x < 16 and 0 <= y < 16:
            cw[y, x] = (210, 210, 205, 170)
save(cw, "castle_cobweb")

# ------------------------------------------------------------------ model textures
# tapestry 32x64: deep red field, black-gold border, horned beast skull, flames below, fringe
tp = base('#5c1216', 4201, 5, 64, 32)
for y in range(64):
    for x in range(32):
        if x < 2 or x > 29 or y < 2 or y > 59:
            tp[y, x, :3] = rgb('#1a0f0d')
        elif x < 4 or x > 27 or y < 4 or y > 57:
            tp[y, x, :3] = rgb('#a57a2a') if (x + y) % 4 < 2 else rgb('#6e4f1a')
for y in range(60, 64):                                        # fringe
    for x in range(32):
        tp[y, x, :3] = rgb('#a57a2a') if x % 2 == 0 else rgb('#1a0f0d')
        tp[y, x, 3] = 255 if x % 2 == 0 else 0
BONE, DARK, GOLD = rgb('#d8ccb0'), rgb('#120a09'), rgb('#c69a3a')
sk = ["....HH..........HH....",
      "...HH............HH...",
      "..HH..............HH..",
      "..HH...BBBBBBBB...HH..",
      "...HHBBBBBBBBBBBBHH...",
      "....BBBBBBBBBBBBBB....",
      "....BBDDBBBBBBDDBB....",
      "....BBDDDBBBBDDDBB....",
      ".....BBBBBBBBBBBB.....",
      "......BBBBBBBBBB......",
      ".......BBBBBBBB.......",
      ".......BBDBBDBB.......",
      "........BBBBBB........",
      "........BDBBDB........",
      ".........BBBB........."]
for j, row in enumerate(sk):
    for i, ch in enumerate(row):
        x, y = 5 + i, 14 + j
        if ch == 'B': tp[y, x, :3] = BONE
        elif ch == 'D': tp[y, x, :3] = DARK
        elif ch == 'H': tp[y, x, :3] = GOLD
r = np.random.RandomState(4202)
for x in range(6, 26):                                         # embers / flames under the skull
    hgt = int(6 + 5 * abs(np.sin(x * 0.9)) + r.randint(0, 3))
    for y in range(52 - hgt, 52):
        f = (52 - y) / hgt
        tp[y, x, :3] = rgb('#e0a040') * (1 - f) + rgb('#7a1a10') * f if r.rand() < 0.85 else tp[y, x, :3]
for x in range(8, 24, 3):
    tp[8, x, :3] = GOLD; tp[9, x, :3] = GOLD
save(tp, "m_c_tapestry", TM)

ln = base('#d9d0bc', 4203, 5)
for y in range(0, 16, 2):
    ln[y, :, :3] *= 0.95
save(ln, "m_c_linen", TM)

bk = base('#26231f', 4204, 6)
for x in range(16):
    bk[(x * 3) % 16, x, :3] = rgb('#4c4741')                   # forge marks
for _ in range(10):
    bk[r.randint(0, 16), r.randint(0, 16), :3] = rgb('#5b2e1a')
bk[0:2, :, :3] = rgb('#8a8580')                                # honed edge
save(bk, "m_c_blacksteel", TM)

pg = base('#d6c9a4', 4205, 4)
for y in range(2, 15, 2):
    for x in list(range(1, 7)) + list(range(9, 15)):
        if r.rand() < 0.8:
            pg[y, x, :3] = rgb('#5a4a38')
pg[:, 7:9, :3] = rgb('#a89a78')
save(pg, "m_c_pages", TM)

gd = base('#c9a13c', 4206, 18)
for _ in range(30):
    x, y = r.randint(0, 16), r.randint(0, 16)
    gd[y, x, :3] = rgb('#f2d77a')
    gd[min(15, y + 1), x, :3] = rgb('#7a5a18')
save(gd, "m_c_coins", TM)
print("castle C-2 textures ok")
