"""Revision 2 textures (reference village style) - runs AFTER textures_elisar.py and overrides the style-relevant ones.

  * elisar_grass  -> pale smooth crust top (the reference islands have a 1-block bleached top layer)
  * elisar_crust  -> the same pale stone one layer down (visible on cliff faces as the white band)
  * elisar_stone  -> mid-grey rough stone blocks (reference undersides are mid grey, not black)
  * elisar_cobble -> pebble gravel of the plazas / streets
  * elisar_moss, tower_bricks(_cracked), elisar_lantern, elisar_stone_wall, pale_wheat
  * environment: eye (open / iris / closed), earth, elisar planet, soft glow, moon
"""
import os, math
import numpy as np
from PIL import Image, ImageDraw

ROOT = "/home/user/mods/riftworld/src/main/resources/assets/riftworld/textures"
BLK = os.path.join(ROOT, "block")
ENV = os.path.join(ROOT, "environment")
os.makedirs(ENV, exist_ok=True)


def rgb(h):
    h = h.lstrip('#')
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], float)


def save(a, name, d=BLK):
    Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(d, name + ".png"))


def base(col, seed, amp=6, n=16):
    r = np.random.RandomState(seed)
    a = np.zeros((n, n, 4))
    a[..., :3] = rgb(col) + ((r.rand(n, n) - 0.5) * 2 * amp)[..., None]
    a[..., 3] = 255
    return a


def smooth_noise(n, seed, scale=4):
    r = np.random.RandomState(seed)
    g = r.rand(n // scale + 2, n // scale + 2)
    out = np.zeros((n, n))
    for y in range(n):
        for x in range(n):
            gx, gy = x / scale, y / scale
            x0, y0 = int(gx), int(gy)
            fx, fy = gx - x0, gy - y0
            out[y, x] = (g[y0, x0] * (1 - fx) * (1 - fy) + g[y0, x0 + 1] * fx * (1 - fy) + g[y0 + 1, x0] * (1 - fx) * fy + g[y0 + 1, x0 + 1] * fx * fy)
    return out - 0.5


# ---- pale crust top: smooth bleached stone with very soft mottling and a few hairline cracks
def crust(seed, col='#dcd7ca'):
    a = base(col, seed, 3)
    a[..., :3] += (smooth_noise(16, seed + 1, 4) * 10)[..., None]
    r = np.random.RandomState(seed + 2)
    for _ in range(2):
        x, y = r.randint(0, 16), r.randint(0, 16)
        for k in range(r.randint(2, 5)):
            if 0 <= x < 16 and 0 <= y < 16:
                a[y, x, :3] *= 0.9
            x += r.choice((-1, 0, 1)); y += 1
    return a


save(crust(101), "elisar_grass")
c2 = crust(202, '#d3cdbf')
for x in range(16):                      # faint bedding line
    c2[11, x, :3] *= 0.95
save(c2, "elisar_crust")

# ---- mid-grey rough stone blocks (large irregular blocks with darker joints)
st = base('#7b7c7f', 303, 7)
st[..., :3] += (smooth_noise(16, 304, 4) * 14)[..., None]
for y in (0, 7, 8, 15):
    pass
joints = [(0, 0, 9, 7), (9, 0, 16, 7), (0, 8, 5, 16), (5, 8, 12, 16), (12, 8, 16, 16)]
for x0, y0, x1, y1 in joints:
    for x in range(x0, x1):
        st[y0, x, :3] = st[y0, x, :3] * 0.72
    for y in range(y0, y1):
        st[y, x0, :3] = st[y, x0, :3] * 0.72
    for x in range(x0 + 1, x1):          # light top edge of every block
        if y0 + 1 < 16:
            st[y0 + 1, x, :3] = st[y0 + 1, x, :3] * 1.08
save(st, "elisar_stone")
stm = st.copy()
r = np.random.RandomState(305)
for _ in range(22):
    x, y = r.randint(0, 16), r.randint(0, 16)
    stm[y, x, :3] = rgb('#7f8160') + r.randint(-8, 8)
save(stm, "elisar_stone_mossy")

# ---- pebble gravel (plazas, streets, aprons around houses)
gv = base('#bdb9b1', 404, 5)
r = np.random.RandomState(405)
for _ in range(34):
    cx, cy = r.randint(0, 16), r.randint(0, 16)
    rad = r.choice((0.8, 1.0, 1.3))
    tone = r.choice((0.84, 0.9, 1.06, 1.1))
    for y in range(cy - 2, cy + 3):
        for x in range(cx - 2, cx + 3):
            d = math.hypot(x - cx, y - cy)
            if d <= rad:
                xx, yy = x % 16, y % 16
                gv[yy, xx, :3] = rgb('#bdb9b1') * tone
                if y < cy:
                    gv[yy, xx, :3] *= 1.05
            elif d <= rad + 0.7:
                xx, yy = x % 16, y % 16
                gv[yy, xx, :3] *= 0.9
save(gv, "elisar_cobble")

# ---- moss (olive strips on the pale top, as in the reference)
ms = base('#9c9c6c', 505, 8)
ms[..., :3] += (smooth_noise(16, 506, 3) * 22)[..., None]
r = np.random.RandomState(507)
for _ in range(26):
    x, y = r.randint(0, 16), r.randint(0, 16)
    ms[y, x, :3] = rgb('#b9b884')
save(ms, "elisar_moss")


# ---- dark tower bricks (+ cracked)
def tower(seed, cracked):
    a = base('#56575b', seed, 6)
    for row in range(4):
        y0 = row * 4
        for x in range(16):
            a[y0, x, :3] = rgb('#3c3d40')
        off = 0 if row % 2 == 0 else 4
        for x in (off, off + 8):
            for y in range(y0, y0 + 4):
                a[y, x % 16, :3] = rgb('#3c3d40')
        for x in range(16):
            if y0 + 1 < 16:
                a[y0 + 1, x, :3] *= 1.07
    if cracked:
        r = np.random.RandomState(seed + 1)
        x, y = 3, 0
        while y < 16:
            a[y, x % 16, :3] = rgb('#2c2d30'); x += r.choice((-1, 0, 1)); y += 1
        for _ in range(10):
            a[r.randint(0, 16), r.randint(0, 16), :3] = rgb('#6c6e5c')
    return a


save(tower(606, False), "tower_bricks")
save(tower(607, True), "tower_bricks_cracked")

# ---- stone wall (pillar posts of the lantern stands): pale bricks
wl = base('#d2ccbf', 708, 4)
for y in (0, 5, 10, 15):
    wl[y, :, :3] = rgb('#aaa496')
for y0, offs in ((0, (3, 11)), (5, (7, 15)), (10, (3, 11))):
    for x in offs:
        wl[y0:y0 + 5, x, :3] = rgb('#aaa496')
save(wl, "elisar_stone_wall")

# ---- lantern: own layout (side 0..6 x 0..7, top 0..6 x 9..15, cap 8..12 x 0..2, handle 11..14 x 10..16)
ln = np.zeros((16, 16, 4))
iron, iron_d, glow, glow2 = rgb('#3b3a38'), rgb('#2a2928'), rgb('#ffb35c'), rgb('#ffe2a6')
for y in range(7):
    for x in range(6):
        frame = x in (0, 5) or y in (0, 6)
        ln[y, x, :3] = iron if frame else (glow2 if 2 <= x <= 3 and 2 <= y <= 4 else glow)
        ln[y, x, 3] = 255
for y in range(9, 15):
    for x in range(6):
        ln[y, x, :3] = iron_d if (x in (0, 5) or y in (9, 14)) else iron
        ln[y, x, 3] = 255
for y in range(0, 2):
    for x in range(8, 12):
        ln[y, x, :3] = iron; ln[y, x, 3] = 255
for y in range(10, 16):
    for x in range(11, 14):
        if x == 12 or y in (10, 15):
            ln[y, x, :3] = iron_d; ln[y, x, 3] = 255
save(ln, "elisar_lantern")

# ---- pale wheat (cross plant): bleached stalks with heavy cream heads
wh = np.zeros((16, 16, 4))
r = np.random.RandomState(809)
for k in range(6):
    x = 1 + k * 2 + r.randint(0, 2)
    top = r.randint(1, 4)
    for y in range(top + 4, 16):
        wh[y, x, :3] = rgb('#c9bf9f') * (0.9 + 0.1 * (y % 2)); wh[y, x, 3] = 255
    for y in range(top, top + 5):
        for dx in (0, 1 if k % 2 else -1):
            xx = min(15, max(0, x + dx))
            if (y + dx) % 2 == 0 or dx == 0:
                wh[y, xx, :3] = rgb('#efe6cc') * (0.92 + 0.08 * ((y + k) % 2)); wh[y, xx, 3] = 255
save(wh, "pale_wheat")


# ================================================================ environment (sky / space)
def eye_layers(W=128, H=64):
    """Pixel-line eye drawn as in the reference: faint grey outlines, several lash/lid contour lines."""
    def lid_curve(t, amp, open_k):
        return H / 2 - amp * open_k * math.sin(math.pi * t)

    imgs = {}
    for name, open_k in (("eye_open", 1.0), ("eye_half", 0.45), ("eye_closed", 0.0)):
        im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(im)
        line = (120, 116, 108, 255)
        soft = (150, 146, 138, 200)
        x0, x1 = 10, W - 10
        for k, (amp, col) in enumerate(((22, line), (27, soft), (31, soft))):
            pts_u = [(x, H / 2 - (amp * (open_k if k == 0 else max(open_k, 0.25))) * math.sin(math.pi * (x - x0) / (x1 - x0))) for x in range(x0, x1 + 1, 2)]
            if k == 0 or open_k > 0:
                d.line(pts_u, fill=col, width=1)
        for k, (amp, col) in enumerate(((20, line), (25, soft))):
            pts_l = [(x, H / 2 + amp * max(open_k, 0.15 if k else 0.0) * math.sin(math.pi * (x - x0) / (x1 - x0))) for x in range(x0, x1 + 1, 2)]
            d.line(pts_l, fill=col, width=1)
        if open_k == 0:                                   # sleeping: lashes hang from the closed lid
            for k in range(9):
                x = x0 + 14 + k * 11
                d.line([(x, H / 2 + 1), (x - 2, H / 2 + 6)], fill=line, width=1)
        imgs[name] = im
    iris = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(iris)
    cx, cy = W / 2, H / 2
    d.ellipse([cx - 17, cy - 17, cx + 17, cy + 17], outline=(118, 114, 106, 255), width=1)
    d.ellipse([cx - 13, cy - 13, cx + 13, cy + 13], outline=(140, 136, 128, 220), width=1)
    d.ellipse([cx - 7, cy - 7, cx + 7, cy + 7], fill=(150, 146, 138, 255), outline=(110, 106, 98, 255))
    d.ellipse([cx - 3, cy - 6, cx + 1, cy - 2], fill=(236, 232, 224, 255))         # highlight
    imgs["eye_iris"] = iris
    for k, im in imgs.items():
        im.save(os.path.join(ENV, k + ".png"))


eye_layers()


def planet(name, W, H, seed, kind):
    r = np.random.RandomState(seed)
    # fractal value noise on the sphere (equirect, wrap in x)
    acc = np.zeros((H, W))
    for octave, amp in ((8, 1.0), (16, 0.5), (32, 0.25), (64, 0.12)):
        g = r.rand(octave // 2 + 2, octave + 1)
        g[:, -1] = g[:, 0]
        ys = np.linspace(0, octave // 2, H)
        xs = np.linspace(0, octave, W)
        y0 = np.floor(ys).astype(int); x0 = np.floor(xs).astype(int)
        fy = (ys - y0)[:, None]; fx = (xs - x0)[None, :]
        fy = fy * fy * (3 - 2 * fy); fx = fx * fx * (3 - 2 * fx)
        a = g[y0][:, x0]; b = g[y0][:, np.minimum(x0 + 1, octave)]
        c = g[np.minimum(y0 + 1, octave // 2 + 1)][:, x0]; dd = g[np.minimum(y0 + 1, octave // 2 + 1)][:, np.minimum(x0 + 1, octave)]
        acc += amp * (a * (1 - fx) * (1 - fy) + b * fx * (1 - fy) + c * (1 - fx) * fy + dd * fx * fy)
    acc /= 1.87
    lat = np.abs(np.linspace(-1, 1, H))[:, None] * np.ones((1, W))
    out = np.zeros((H, W, 4)); out[..., 3] = 255
    if kind == "earth":
        land = acc > 0.53
        ocean = np.array([22, 62, 128]) + (acc[..., None] - 0.4) * np.array([20, 60, 90])
        green = np.array([62, 108, 52]) + (acc[..., None] - 0.55) * np.array([160, 120, 60])
        desert = np.array([176, 150, 100])
        lnd = np.where((lat[..., None] < 0.35) & (acc[..., None] > 0.62), desert, green)
        out[..., :3] = np.where(land[..., None], lnd, ocean)
        ice = lat > 0.82
        out[ice, :3] = [236, 240, 245]
        # clouds: second noise field
        cl = np.roll(acc, W // 3, axis=1) * 0.6 + np.roll(acc[::-1], W // 5, axis=1) * 0.4
        cm = np.clip((cl - 0.5) * 4.0, 0, 1)[..., None]
        out[..., :3] = out[..., :3] * (1 - cm * 0.85) + np.array([248, 248, 250]) * cm * 0.85
    elif kind == "elisar":
        # bleached cream world: pale haze bands, grey island specks, a faint mirror ocean
        out[..., :3] = np.array([226, 220, 206]) + (acc[..., None] - 0.5) * 40
        isl = acc > 0.6
        out[isl, :3] = out[isl, :3] * 0.78
        dark = acc > 0.7
        out[dark, :3] = [118, 118, 122]
        band = (np.sin(np.linspace(0, 9 * math.pi, H)) * 0.5 + 0.5)[:, None]
        out[..., :3] = out[..., :3] * (0.94 + 0.06 * band[..., None])
        sea = acc < 0.36
        out[sea, :3] = [176, 210, 222]
    else:   # moon
        out[..., :3] = np.array([150, 150, 152]) + (acc[..., None] - 0.5) * 90
        for _ in range(60):
            cx, cy, rr = r.randint(0, W), r.randint(4, H - 4), r.randint(2, 7)
            yy, xx = np.ogrid[:H, :W]
            m = (yy - cy) ** 2 + (xx - cx) ** 2 <= rr * rr
            out[m, :3] *= 0.85
    Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(ENV, name + ".png"))


planet("earth", 512, 256, 11, "earth")
planet("elisar_planet", 512, 256, 12, "elisar")
planet("moon", 128, 64, 13, "moon")

# soft radial glow (atmosphere halo / laser flare / stars)
G = 64
g = np.zeros((G, G, 4))
for y in range(G):
    for x in range(G):
        d = math.hypot(x - G / 2 + 0.5, y - G / 2 + 0.5) / (G / 2)
        v = max(0.0, 1 - d) ** 2.2
        g[y, x] = [255, 255, 255, 255 * v]
Image.fromarray(g.astype(np.uint8), "RGBA").save(os.path.join(ENV, "glow.png"))
print("rev2 textures OK")
