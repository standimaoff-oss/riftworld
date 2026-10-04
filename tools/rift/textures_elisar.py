#!/usr/bin/env python3
"""Elisar (city «Странники») textures: custom 16x16 block textures + 64/128 px model textures. Procedural only.

Palette: blue-green grass, rust-red soil, lilac-grey stone, two native trees
  sizo  - "сизоствол": blue-grey bark, teal-cyan leaves
  ambra - "янтарник": red-brown bark, amber-orange leaves
warm cream plaster, dark timber, terracotta and slate-blue roof tiles, clear turquoise water.
"""
import os, math
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = "/home/user/mods/riftworld/src/main/resources/assets/riftworld/textures"
BLK = os.path.join(ROOT, "block")
ITM = os.path.join(ROOT, "item")
MOD = "/home/user/tools/rift/tex_models"
for d in (BLK, ITM, MOD):
    os.makedirs(d, exist_ok=True)
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_SERIF = "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"
N = 16


def rgb(h):
    h = h.lstrip('#')
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], float)


def noise(seed, amp, n=N, m=None):
    r = np.random.RandomState(seed)
    return (r.rand(m or n, n) - 0.5) * 2 * amp


def base(col, seed, amp=6, n=N, m=None):
    a = np.zeros((m or n, n, 4))
    a[..., :3] = rgb(col)
    a[..., :3] += noise(seed, amp, n, m)[..., None]
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


def blotch(a, seed, cols, count, rmax=2):
    r = np.random.RandomState(seed)
    h, w = a.shape[:2]
    for _ in range(count):
        cx, cy, rr = r.randint(w), r.randint(h), r.randint(1, rmax + 1)
        c = rgb(cols[r.randint(len(cols))])
        for y in range(cy - rr, cy + rr + 1):
            for x in range(cx - rr, cx + rr + 1):
                if (x - cx) ** 2 + (y - cy) ** 2 <= rr * rr - r.randint(2):
                    a[y % h, x % w, :3] = c + r.randint(-8, 8)


# ------------------------------------------------------------------------------------------- terrain
GRASS = ['#4c9a83', '#3f8a78', '#5aa88c', '#468f88', '#64b096']
a = base('#4c9a83', 1, 7); blotch(a, 2, GRASS, 40, 1)
for k in range(10):
    setp(a, (k * 7) % 16, (k * 5 + 3) % 16, '#7cc4a6')
save(a, "elisar_grass_top")
soil = base('#7a4a3a', 3, 9); blotch(soil, 4, ['#6a3e31', '#8a5644', '#5e372c', '#94604c'], 30, 1)
for k in range(6):
    setp(soil, (k * 11 + 2) % 16, (k * 7 + 5) % 16, '#a9a092')        # pebbles
save(soil, "elisar_soil")
side = soil.copy()
r = np.random.RandomState(5)
for x in range(16):
    d = 3 + r.randint(0, 3)
    for y in range(d):
        side[y, x, :3] = rgb(GRASS[r.randint(len(GRASS))]) + r.randint(-6, 6)
save(side, "elisar_grass_side")
st = base('#8d8496', 6, 8); blotch(st, 7, ['#7d7487', '#9a91a3', '#857c90', '#a49bae'], 26, 2)
for (x0, y0, x1, y1) in ((2, 5, 7, 7), (9, 11, 14, 12), (11, 2, 14, 4)):
    for x in range(x0, x1 + 1):
        shade(st, x, y0 + (x % 2), 0.72)
save(st, "elisar_stone")
m = st.copy(); blotch(m, 8, ['#4f8f6c', '#5c9c76', '#3f7f5f'], 16, 2); save(m, "elisar_stone_mossy")
sb = base('#2f5f78', 8, 7); blotch(sb, 10, ['#28526a', '#3a6f88', '#24485e'], 30, 1); save(sb, "elisar_seabed")


def cobble(col, seed, mortar='#4e4a52'):
    a = base(mortar, seed, 4)
    r = np.random.RandomState(seed)
    stones = [(1, 1, 6, 5), (8, 0, 14, 4), (0, 7, 4, 11), (6, 6, 11, 10), (12, 6, 15, 11), (1, 13, 7, 15), (9, 12, 14, 15)]
    for (x0, y0, x1, y1) in stones:
        c = rgb(col) + r.randint(-14, 14)
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                if (x in (x0, x1) and y in (y0, y1)):
                    continue
                a[y % 16, x % 16, :3] = c + r.randint(-6, 6)
                if y == y0 or x == x0:
                    a[y % 16, x % 16, :3] *= 1.12
                if y == y1 or x == x1:
                    a[y % 16, x % 16, :3] *= 0.8
    return a


save(cobble('#9a8f88', 11), "elisar_cobble")
pa = base('#a58a66', 12, 7); blotch(pa, 13, ['#947a58', '#b49a74', '#8d7656', '#c0aa86'], 40, 1); save(pa, "elisar_path")


def bricks(col, seed, mortar='#6d6570', moss=0):
    a = base(mortar, seed, 3)
    r = np.random.RandomState(seed)
    for row in range(4):
        y0 = row * 4
        off = 4 if row % 2 else 0
        for bx in range(-1, 3):
            x0 = bx * 8 + off
            c = rgb(col) + r.randint(-12, 12)
            for y in range(y0, y0 + 3):
                for x in range(x0, x0 + 7):
                    if 0 <= x < 16:
                        a[y, x, :3] = c + r.randint(-5, 5)
                        if y == y0:
                            a[y, x, :3] *= 1.1
                        if y == y0 + 2:
                            a[y, x, :3] *= 0.86
    if moss:
        blotch(a, seed + 1, ['#4f8f6c', '#5c9c76'], moss, 1)
    return a


save(bricks('#a69aa6', 14), "elisar_bricks")
save(bricks('#a69aa6', 14, moss=14), "elisar_bricks_mossy")
pl = base('#e6d9bd', 15, 5); blotch(pl, 16, ['#dccfb2', '#efe3c8', '#d6c8aa'], 20, 1); save(pl, "elisar_plaster")
BEAM = '#4a3426'


def timber(kind):
    a = pl.copy()
    def beam(x, y):
        if 0 <= x < 16 and 0 <= y < 16:
            a[y, x, :3] = rgb(BEAM) + ((x * 7 + y * 3) % 5 - 2) * 4
    for k in range(16):
        beam(k, 0); beam(k, 15); beam(0, k); beam(15, k)
        beam(k, 1) if kind == "post" else None
        beam(1, k) if kind == "post" else None
    if kind == "cross":
        for k in range(16):
            beam(k, k); beam(k, 15 - k)
            beam(k + 1, k) if k < 15 else None
            beam(15 - k - 1, k) if k < 15 else None
    else:
        for k in range(16):
            beam(7, k); beam(8, k)
    return a


save(timber("cross"), "timber_frame_cross")
save(timber("post"), "timber_frame_post")


def planks(col, seed, dark):
    a = base(col, seed, 5)
    r = np.random.RandomState(seed)
    for row in range(4):
        y0 = row * 4
        for x in range(16):
            a[y0, x, :3] = rgb(dark)
        cut = r.randint(3, 13)
        a[y0 + 1:y0 + 4, cut, :3] = rgb(dark)
        for y in range(y0 + 1, y0 + 4):
            for x in range(16):
                a[y, x, :3] += 6 * math.sin((x + row * 5) * 0.9 + y)
    return a


def log_side(col, seed, dark):
    a = base(col, seed, 7)
    r = np.random.RandomState(seed)
    for x in range(16):
        if r.rand() < 0.35:
            for y in range(16):
                a[y, x, :3] = rgb(dark) + r.randint(-6, 6)
    for k in range(4):
        x, y = r.randint(16), r.randint(14)
        a[y:y + 2, x, :3] *= 0.7
    return a


def log_top(col, ring, bark):
    a = base(col, 40, 4)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 7.0:
                a[y, x, :3] = rgb(bark)
            elif int(d) % 3 == 0:
                a[y, x, :3] = rgb(ring)
    return a


def leaves(cols, seed):
    a = np.zeros((16, 16, 4))
    r = np.random.RandomState(seed)
    for y in range(16):
        for x in range(16):
            if r.rand() < 0.82:
                a[y, x, :3] = rgb(cols[r.randint(len(cols))]) + r.randint(-10, 10)
                a[y, x, 3] = 255
    return a


save(log_side('#5d6b78', 20, '#46525e'), "sizo_log")
save(log_top('#a9b7a8', '#7d8c80', '#4d5a66'), "sizo_log_top")
save(planks('#7f8f96', 21, '#56636a'), "sizo_planks")
save(leaves(['#2f9a96', '#3fb3a6', '#25807f', '#56c2b2', '#1f6e74'], 22), "sizo_leaves")
save(log_side('#6e3f2e', 23, '#542e22'), "ambra_log")
save(log_top('#c99566', '#a67248', '#5a3426'), "ambra_log_top")
save(planks('#a2673f', 24, '#6e4128'), "ambra_planks")
save(leaves(['#e09a32', '#f0b443', '#c97b25', '#f5c862', '#b8661e'], 25), "ambra_leaves")


def roof(col, dark, seed):
    a = base(col, seed, 6)
    r = np.random.RandomState(seed)
    for row in range(4):
        y0 = row * 4
        off = 2 if row % 2 else 0
        for x in range(16):
            a[y0 + 3, x, :3] = rgb(dark)
            if (x + off) % 4 == 0:
                a[y0:y0 + 3, x, :3] *= 0.8
        for x in range(16):
            a[y0, x, :3] *= 1.12
        if r.rand() < 0.5:
            a[y0 + 1, r.randint(16), :3] *= 0.75
    return a


save(roof('#b5543a', '#6e2f22', 26), "roof_red")
save(roof('#566c8a', '#2f3d52', 27), "roof_blue")
th = base('#c2a25c', 28, 10)
for x in range(16):
    for y in range(16):
        th[y, x, :3] += 14 * math.sin(x * 2.3 + (y // 4) * 1.7)
    for row in range(4):
        th[row * 4 + 3, x, :3] *= 0.75
save(th, "thatch")
w = np.zeros((16, 16, 4))
for y in range(16):
    for x in range(16):
        if x in (0, 15, 7, 8) or y in (0, 15, 7, 8):
            w[y, x] = list(rgb('#3b2b20')) + [255]
        else:
            w[y, x] = [170, 205, 215, 90 + (15 if (x + y) % 7 == 0 else 0)]
save(w, "elisar_window")
fen = planks('#7f8f96', 29, '#56636a'); save(fen, "elisar_fence")
rope = base('#b39a6a', 30, 8)
for y in range(16):
    for x in range(16):
        if (x + y) % 4 == 0:
            rope[y, x, :3] *= 0.78
save(rope, "rope_post")
# plants
tg = np.zeros((16, 16, 4))
r = np.random.RandomState(31)
for k in range(9):
    x = 1 + k * 1.6 + r.rand()
    h = r.randint(8, 16)
    lean = r.uniform(-0.25, 0.25)
    for t in range(h):
        xx = int(x + lean * t)
        if 0 <= xx < 16:
            tg[15 - t, xx] = list(rgb(GRASS[r.randint(len(GRASS))]) * (0.8 + 0.3 * t / h)) + [255]
save(tg, "elisar_tall_grass")
fl = np.zeros((16, 16, 4))
for (x, top, col) in ((3, 6, '#c86cf0'), (8, 3, '#f0e66c'), (12, 7, '#6ce8f0')):
    for t in range(15, top, -1):
        fl[t, x] = list(rgb('#3f7f5f')) + [255]
    for dx, dy in ((0, 0), (-1, 0), (1, 0), (0, -1), (0, 1)):
        fl[top + dy, x + dx] = list(rgb(col)) + [255]
    fl[top, x] = list(rgb('#fff6d0')) + [255]
    fl[11, x - 1] = list(rgb('#4c9a5f')) + [255]; fl[12, x + 1] = list(rgb('#4c9a5f')) + [255]
save(fl, "elisar_flowers")
iv = np.zeros((16, 16, 4))
r = np.random.RandomState(32)
for k in range(5):
    x = r.randint(16)
    for y in range(16):
        x = (x + r.randint(-1, 2)) % 16
        iv[y, x] = list(rgb(['#3f8a78', '#2f7a68', '#5aa88c'][r.randint(3)])) + [255]
        if r.rand() < 0.3:
            iv[y, (x + 1) % 16] = list(rgb('#64b096')) + [255]
save(iv, "elisar_ivy")
# door (2 halves)
for half in ("bottom", "top"):
    d = planks('#7a5038', 33 if half == "bottom" else 34, '#4f3424')
    for y in range(16):
        d[y, 0, :3] = d[y, 15, :3] = rgb('#3b2a1e')
    for x in range(16):
        d[0 if half == "top" else 15, x, :3] = rgb('#3b2a1e')
    if half == "top":
        for y in range(4, 10):
            for x in range(4, 12):
                d[y, x] = [150, 190, 200, 255] if not (x in (7, 8) or y == 7) else list(rgb('#3b2a1e')) + [255]
    else:
        for x in range(2, 14):
            d[6, x, :3] = rgb('#2a2a2e')
        d[8, 11, :3] = d[9, 11, :3] = rgb('#c9a24a')
    save(d, "elisar_door_" + half)
save(np.array(Image.open(os.path.join(BLK, "elisar_door_top.png")).convert("RGBA"), float), "elisar_door", ITM)


# water: animated still (16x16 x 16 frames) + flowing (32x32 x 16 frames)
def water(size, frames, seed):
    out = np.zeros((size * frames, size, 4))
    for f in range(frames):
        ph = 2 * math.pi * f / frames
        for y in range(size):
            for x in range(size):
                v = math.sin(x * 2 * math.pi / size * 2 + ph) + math.sin((y + x * 0.5) * 2 * math.pi / size * 2 - ph * 2)
                c = np.array([34, 112, 192]) + v * 13
                if v > 1.55:
                    c = np.array([120, 196, 240])          # sun glints on the wave crests
                out[f * size + y, x] = list(np.clip(c, 0, 255)) + [178]
    return out


save(water(16, 16, 1), "elisar_water_still")
save(water(32, 16, 2), "elisar_water_flow")
for n, t in (("elisar_water_still", 3), ("elisar_water_flow", 2)):
    with open(os.path.join(BLK, n + ".png.mcmeta"), "w") as f:
        f.write('{"animation": {"frametime": %d, "interpolate": true}}' % t)
_bk = np.zeros((16, 16, 4))
for _y in range(3, 15):
    _w = 5 + (14 - _y) * 0.25
    for _x in range(16):
        if abs(_x - 7.5) <= _w:
            edge = abs(_x - 7.5) > _w - 1 or _y == 14
            _bk[_y, _x] = [96, 100, 108, 255] if edge else [150, 154, 162, 255]
for _x in range(16):
    if abs(_x - 7.5) <= 6.2:
        _bk[3, _x] = [48, 128, 205, 255]; _bk[4, _x] = [70, 150, 220, 255] if abs(_x - 7.5) <= 5.2 else _bk[4, _x]
for _x in range(3, 13):
    _y = int(round(2 - 2 * math.sin(math.pi * (_x - 3) / 9)))
    _bk[max(_y, 0), _x] = [80, 84, 92, 255]
save(_bk, "elisar_water_bucket", ITM)


# ------------------------------------------------------------------------------------------- model textures (64 px)
def mtex(name, arr):
    Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(MOD, name + ".png"))


def up(a16, n=64):
    return np.kron(a16, np.ones((n // a16.shape[0], n // a16.shape[1], 1)))


def wood64(col, dark, seed):
    a = base(col, seed, 6, 64)
    r = np.random.RandomState(seed)
    for y in range(64):
        for x in range(64):
            a[y, x, :3] += 9 * math.sin(y * 0.35 + 3 * math.sin(x * 0.07 + r.rand() * 0.2))
    for x in range(0, 64, 16):
        a[:, x, :3] = rgb(dark)
    return a


mtex("m_e_wood", wood64('#8a6244', '#5a3e2a', 50))
mtex("m_e_wood_dark", wood64('#5a3e2c', '#3a271b', 51))
mtex("m_e_sizo", wood64('#7f8f96', '#56636a', 52))
mtex("m_e_bark", up(log_side('#6e3f2e', 53, '#542e22')))
mtex("m_e_stone", up(bricks('#a69aa6', 54)))
mtex("m_e_rock", up(st))
ir = base('#2f2f34', 55, 6, 64); mtex("m_e_iron", ir)
cv = base('#d8ccb0', 56, 6, 64)
for y in range(64):
    for x in range(64):
        if (x + y) % 3 == 0:
            cv[y, x, :3] *= 0.95
blotch(cv, 57, ['#b8a888', '#a89878', '#8a8a6a'], 14, 3)
mtex("m_e_canvas", cv)
sp = base('#c84a3a', 58, 4, 64)
for x in range(64):
    if (x // 8) % 2:
        sp[:, x, :3] = rgb('#efe4cc') + noise(59, 4, 64)[:, x:x + 1]
mtex("m_e_stripe", sp)
sp2 = sp.copy()
for x in range(64):
    if not (x // 8) % 2:
        sp2[:, x, :3] = rgb('#3a7aa8')
mtex("m_e_stripe_blue", sp2)
pa = base('#e3cf9f', 60, 5, 64); blotch(pa, 61, ['#d2bb88', '#c9ad78'], 18, 4)
for k in range(64):
    for j in (0, 1, 62, 63):
        pa[j, k, :3] *= 0.8; pa[k, j, :3] *= 0.8
mtex("m_e_parchment", pa)
hide = base('#9a6a44', 62, 8, 64); blotch(hide, 63, ['#8a5a38', '#b07c52', '#6a4a30'], 30, 4); mtex("m_e_hide", hide)
cloth = base('#8a2a2a', 64, 6, 64)
for y in range(0, 64, 8):
    cloth[y, :, :3] = rgb('#d8b040')
mtex("m_e_cloth", cloth)
coal = base('#2a1a14', 65, 10, 64); blotch(coal, 66, ['#ff7a1a', '#ffb040', '#e04a10', '#401a10'], 70, 2); mtex("m_e_embers", coal)
fire = base('#ff9a2a', 67, 20, 64); mtex("m_e_fire", fire)
glass = base('#fff1b0', 68, 8, 64); mtex("m_e_lampglow", glass)
rug = base('#7a2a3a', 69, 4, 64)
for y in range(64):
    for x in range(64):
        d = max(abs(x - 31.5), abs(y - 31.5))
        if int(d) % 8 in (0, 1):
            rug[y, x, :3] = rgb('#d8b040')
        elif abs(abs(x - 31.5) - abs(y - 31.5)) < 2 and d < 20:
            rug[y, x, :3] = rgb('#2a4a7a')
mtex("m_e_rug", rug)
fish = base('#2a6ab0', 70, 8, 64)
for y in range(64):
    for x in range(64):
        if (x // 4 + y // 4) % 2 == 0:
            fish[y, x, :3] *= 0.86
        if y > 40:
            fish[y, x, :3] = rgb('#d8e8f0')
blotch(fish, 71, ['#5af0e0', '#a0fff0'], 10, 1)
mtex("m_e_fish", fish)
fish2 = base('#c84aa0', 72, 8, 64)
for y in range(64):
    for x in range(64):
        if (x // 3) % 3 == 0:
            fish2[y, x, :3] = rgb('#f0c040')
mtex("m_e_fish2", fish2)
fur = base('#7a6248', 73, 12, 64); mtex("m_e_fur", fur)
gold = base('#c9a24a', 74, 10, 64); mtex("m_e_gold", gold)
leafm = np.zeros((64, 64, 4))
r = np.random.RandomState(75)
for y in range(64):
    for x in range(64):
        if r.rand() < 0.7:
            leafm[y, x] = list(rgb(GRASS[r.randint(len(GRASS))]) * (0.75 + 0.4 * r.rand())) + [255]
mtex("m_e_foliage", leafm)
flw = leafm.copy(); blotch(flw, 76, ['#c86cf0', '#f0e66c', '#6ce8f0', '#ffffff'], 26, 1); mtex("m_e_flowers", flw)
food = base('#c04030', 77, 20, 64); blotch(food, 78, ['#e0a020', '#60a030', '#a03070', '#f0e0a0'], 60, 3); mtex("m_e_produce", food)


def sign_tex(name, word, emblem, board='#6a4a32', paint='#e8d8a8'):
    W, H = 128, 64
    a = wood64('#7a5236', '#4f3424', 80 + len(word))
    img = Image.fromarray(np.clip(np.kron(a, np.ones((1, 2, 1))), 0, 255).astype(np.uint8), "RGBA").resize((W, H))
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, W - 3, H - 3], outline=(58, 40, 28, 255), width=3)
    d.rectangle([6, 6, W - 7, H - 7], outline=(201, 162, 74, 255), width=1)
    sz = 18
    while True:
        f = ImageFont.truetype(FONT_SERIF, sz)
        tw = d.textlength(word, font=f)
        if tw <= W - 50 or sz <= 8:
            break
        sz -= 1
    d.text((38 + (W - 46 - tw) / 2, 32 - sz * 0.6), word, font=f, fill=paint)
    emblem(d)
    img.save(os.path.join(MOD, name + ".png"))


def em_coin(d):
    d.ellipse([10, 20, 30, 40], fill=(201, 162, 74), outline=(120, 90, 40), width=2); d.text((15, 21), "К", font=ImageFont.truetype(FONT, 14), fill=(90, 60, 20))


def em_mug(d):
    d.rectangle([12, 22, 26, 42], fill=(160, 110, 60), outline=(60, 40, 20), width=2); d.arc([22, 26, 32, 38], 270, 90, fill=(60, 40, 20), width=3)
    d.ellipse([11, 18, 27, 25], fill=(240, 230, 210))


def em_anvil(d):
    d.polygon([(8, 26), (32, 26), (28, 31), (24, 31), (24, 38), (30, 42), (10, 42), (16, 38), (16, 31), (12, 31)], fill=(70, 70, 76))


def em_fish(d):
    d.ellipse([8, 25, 28, 37], fill=(60, 140, 200)); d.polygon([(27, 31), (34, 25), (34, 37)], fill=(60, 140, 200)); d.ellipse([11, 28, 14, 31], fill=(255, 255, 255))


def em_shield(d):
    d.polygon([(10, 20), (30, 20), (30, 32), (20, 44), (10, 32)], fill=(40, 70, 130), outline=(201, 162, 74)); d.line([(20, 22), (20, 40)], fill=(201, 162, 74), width=2)


def em_antlers(d):
    for s in (-1, 1):
        d.line([(20, 42), (20 + 8 * s, 22)], fill=(220, 200, 160), width=3); d.line([(20 + 4 * s, 32), (20 + 11 * s, 30)], fill=(220, 200, 160), width=2)
        d.line([(20 + 6 * s, 26), (20 + 12 * s, 21)], fill=(220, 200, 160), width=2)


def em_scales(d):
    d.line([(20, 18), (20, 42)], fill=(201, 162, 74), width=2); d.line([(9, 24), (31, 24)], fill=(201, 162, 74), width=2)
    d.arc([6, 26, 16, 36], 0, 180, fill=(201, 162, 74), width=2); d.arc([24, 26, 34, 36], 0, 180, fill=(201, 162, 74), width=2)


def em_sun(d):
    d.ellipse([13, 23, 27, 37], fill=(240, 170, 50))
    for k in range(8):
        t = k * math.pi / 4
        d.line([(20 + 9 * math.cos(t), 30 + 9 * math.sin(t)), (20 + 13 * math.cos(t), 30 + 13 * math.sin(t))], fill=(240, 170, 50), width=2)


for (n, w, e) in (("m_sign_shop", "ЛАВКА", em_coin), ("m_sign_tavern", "ТРАКТИР", em_mug), ("m_sign_forge", "КУЗНИЦА", em_anvil),
                  ("m_sign_fish", "РЫБА", em_fish), ("m_sign_police", "СТРАЖА", em_shield), ("m_sign_hunter", "ОХОТНИК", em_antlers),
                  ("m_sign_market", "РЫНОК", em_scales), ("m_sign_temple", "ХРАМ", em_sun)):
    sign_tex(n, w, e)


def wanted(name, seed, reward):
    W, H = 64, 88
    img = Image.new("RGBA", (W, H), (227, 207, 159, 255))
    d = ImageDraw.Draw(img)
    r = np.random.RandomState(seed)
    for k in range(40):
        x, y = r.randint(W), r.randint(H)
        d.point((x, y), fill=(200, 175, 120))
    d.rectangle([0, 0, W - 1, H - 1], outline=(150, 120, 80), width=2)
    f = ImageFont.truetype(FONT_SERIF, 6)
    d.text(((W - d.textlength("РАЗЫСКИВАЕТСЯ", font=f)) / 2, 5), "РАЗЫСКИВАЕТСЯ", font=f, fill=(90, 30, 20))
    d.rectangle([12, 15, 52, 55], fill=(210, 190, 145), outline=(90, 60, 40))
    hc = (60, 45, 35)
    d.ellipse([22, 20, 42, 42], fill=(170, 140, 110), outline=hc)       # face
    d.chord([20, 16, 44, 34], 180, 360, fill=hc)                         # hair
    if r.rand() < 0.5:
        d.polygon([(24, 36), (40, 36), (36, 44), (28, 44)], fill=hc)     # beard
    d.ellipse([26, 28, 29, 31], fill=hc); d.ellipse([35, 28, 38, 31], fill=hc)
    d.rectangle([16, 44, 48, 55], fill=(120, 90, 70))                     # shoulders
    f2 = ImageFont.truetype(FONT, 8)
    t = "%d КЕНЛИ" % reward
    d.text(((W - d.textlength(t, font=f2)) / 2, 60), t, font=f2, fill=(90, 30, 20))
    for y in (74, 79):
        d.line([(6, y), (58, y)], fill=(120, 100, 80))
    img.save(os.path.join(MOD, name + ".png"))


wanted("m_wanted1", 1, 500); wanted("m_wanted2", 2, 1200); wanted("m_wanted3", 3, 250)
print("elisar textures OK", len(os.listdir(BLK)), "block,", len(os.listdir(MOD)), "model")

# block roof texture reused by Blender models (well roof)
import shutil as _sh
_sh.copy(os.path.join(BLK, "roof_red.png"), os.path.join(MOD, "m_e_roofred.png"))
