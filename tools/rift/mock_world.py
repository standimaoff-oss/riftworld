"""Rev 7 offline photo renderer: builds block scenes from the mod's real textures and rasterises an isometric view.

Not a game screenshot - a geometry+texture mock shot (same numbers as the Java generator: tree_gen.py mirrors
tree/TreeGen.java, bridge math mirrors BridgeDeckGeom). Fast enough to iterate on the look without a game client.

Run:  python3 tools/rift/mock_world.py            -> reports/rev7_shots/*.png
"""
from __future__ import annotations

import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import tree_gen as TG
import bridge_preview as BP

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
TEX = os.path.join(ROOT, "mod", "src", "main", "resources", "assets", "riftworld", "textures", "block")
OUT = os.path.join(ROOT, "reports", "rev7_shots")
os.makedirs(OUT, exist_ok=True)

# ---------------------------------------------------------------- block palette (texture + tint)
TEXFILES = {
    "grass_top": "elisar_grass_top", "grass": "elisar_grass_side", "dirt": "elisar_crust",
    "stone": "elisar_stone", "moss": "elisar_bricks_mossy", "water": None,
    "leaf_lilac": "lilac_leaves", "leaf_sizo": "sizo_leaves", "leaf_fern": "fern_leaves",
    "leaf_mist": "mist_pine_leaves",
    "log_lilac": "lilac_log", "log_sizo": "sizo_log", "log_fern": "fern_log", "log_mist": "mist_pine_log",
    "blossom": "lilac_blossom", "planks": "sizo_planks", "dark_planks": "dark_planks",
    "charred": "charred_log", "scorched": "scorched_stone", "ash": "ash_block",
    "keep": "keep_stone", "cobble": "elisar_cobble", "bricks": "elisar_bricks",
    "path": "elisar_cobble", "flower_violet": "violet_bloom", "flower_lilac": "lilac_bloom",
    "flower_pale": "pale_bloom", "tall_grass": "elisar_tall_grass", "sand": "elisar_crust",
    "magma": "magma_meander", "iron": "bridge_post",
}
FALLBACK = (150, 150, 150)


def avg_color(name: str):
    f = TEXFILES.get(name)
    if not f:
        return None
    p = os.path.join(TEX, f + ".png")
    if not os.path.exists(p):
        return FALLBACK
    im = Image.open(p).convert("RGBA")
    a = np.asarray(im).astype(np.float32)
    m = a[..., 3] > 16
    if not m.any():
        return FALLBACK
    return tuple(int(v) for v in a[m][:, :3].mean(axis=0))


COLORS = {k: avg_color(k) for k in TEXFILES}
COLORS["water"] = (70, 104, 120)


def load_tex(name):
    f = TEXFILES.get(name)
    if not f:
        return None
    p = os.path.join(TEX, f + ".png")
    if not os.path.exists(p):
        return None
    im = Image.open(p).convert("RGBA")
    return np.asarray(im).astype(np.uint8)


TEXDATA = {k: load_tex(k) for k in TEXFILES}


def px(kind, x, y, z, salt=0):
    """One deterministic pixel of a block's texture - gives per-block colour variation like in game."""
    a = TEXDATA.get(kind)
    hh = (x * 73856093) ^ (y * 19349663) ^ (z * 83492791) ^ (salt * 2654435761)
    if a is None:
        return COLORS.get(kind) or FALLBACK
    u = (hh >> 3) & 15
    v = (hh >> 9) & 15
    r, g, b, al = a[v, u]
    if al < 24:
        r, g, b = COLORS.get(kind, FALLBACK)
    return (int(r), int(g), int(b))


LEAF_KINDS = {"leaf_lilac", "leaf_sizo", "leaf_fern", "leaf_mist"}
PLANT_KINDS = {"flower_violet", "flower_lilac", "flower_pale", "tall_grass", "blossom"}


TINT = {"grass_top": (1.0, 1.0, 1.0)}


def tint(c, key):
    t = TINT.get(key, (1, 1, 1))
    return (int(c[0] * t[0]), int(c[1] * t[1]), int(c[2] * t[2]))


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


# ---------------------------------------------------------------- world container
class World:
    def __init__(self):
        self.b = {}

    def put(self, x, y, z, t):
        self.b[(x, y, z)] = t

    def put_soft(self, x, y, z, t):
        if (x, y, z) not in self.b:
            self.b[(x, y, z)] = t

    def get(self, x, y, z):
        return self.b.get((x, y, z))


def tree(world, kind, x, z, base_y, height, seed, leaf="leaf_lilac", log="log_lilac", accent="blossom"):
    for dx, dy, dz, k, ax in TG.build(kind, height, seed):
        t = log if k == TG.LOG else (leaf if k == TG.LEAF else accent)
        world.put_soft(x + dx, base_y + dy, z + dz, t)


# ---------------------------------------------------------------- terrain scenes
def hilly(seed, amp=2.6, freq=0.055, base=0.0):
    def f(x, z):
        return (base + amp * math.sin(x * freq + seed) * math.cos(z * freq * 0.9 + seed * 1.7)
                + amp * 0.45 * math.sin((x + z) * freq * 2.1 + seed * 0.6))
    return f


def build_violet_hills():
    """Biome 04: meadow, river crossing the view, four-post garden bridge over it, lilac groves."""
    w = World()
    surf = hilly(1.7, 2.4, 0.045, 7)
    river_z = 4.0
    for x in range(-22, 40):
        for z in range(-18, 24):
            h = int(round(surf(x, z)))
            river = abs(z - river_z + 2.4 * math.sin(x * 0.13)) < 2.0
            top = h - 3 if river else h
            w.put(x, top, z, "sand" if river else "grass_top")
            for y in range(top - 3, top):
                w.put(x, y, z, "dirt" if y > top - 3 else "stone")
            if river:
                for y in range(top + 1, h):
                    w.put(x, y, z, "water")
    # flowers / grass tufts
    rng = np.random.default_rng(7)
    for _ in range(2600):
        x = int(rng.integers(-22, 40)); z = int(rng.integers(-18, 24))
        t = w.get(x, int(round(surf(x, z))), z)
        if t != "grass_top":
            continue
        r = rng.random()
        if abs(z - river_z + 2.4 * math.sin(x * 0.13)) < 3.2:
            continue
        if r < 0.42:
            w.put(x, int(round(surf(x, z))) + 1, z, "flower_violet")
        elif r < 0.62:
            w.put(x, int(round(surf(x, z))) + 1, z, "flower_lilac")
        elif r < 0.74:
            w.put(x, int(round(surf(x, z))) + 1, z, "flower_pale")
        elif r < 0.92:
            w.put(x, int(round(surf(x, z))) + 1, z, "tall_grass")
    # groves of lilac trees
    rng = np.random.default_rng(11)
    spots = []
    for gx, gz in ((14, -10), (20, 12), (-6, 14), (26, 0), (4, -14), (30, 16), (10, 20)):
        for k in range(3):
            x = gx + int(rng.integers(-4, 5)); z = gz + int(rng.integers(-4, 5))
            if any((x - a) ** 2 + (z - b) ** 2 < 144 for a, b in spots):
                continue
            if abs(z - river_z + 2.4 * math.sin(x * 0.13)) < 4:
                continue
            spots.append((x, z))
            base = int(round(surf(x, z))) + 1
            tree(w, TG.LILAC, x, z, base, int(rng.integers(12, 17)), int(rng.integers(1, 1 << 30)))
    # the bridge: 4 posts on a perpendicular footprint, diagonal-friendly, spanning the river
    a = (-1, int(round(surf(-1, -1))), -1)
    b = (9, int(round(surf(9, 9))), 9)
    dx, dz = b[0] - a[0], b[2] - a[2]
    L = math.hypot(dx, dz)
    px, pz = -dz / L, dx / L                      # width direction = perpendicular to the span
    hw = 1.0
    a2 = (a[0] + px * 2 * hw, a[1], a[2] + pz * 2 * hw)
    b2 = (b[0] + px * 2 * hw, b[1], b[2] + pz * 2 * hw)
    posts = [a, b, a2, b2]
    # carve the channel under the span (the generator does the same via applyChannel(ford)), so the deck reads
    for t in range(0, 101):
        u = t / 100.0
        cx = a[0] + (b[0] - a[0]) * u
        cz = a[2] + (b[2] - a[2]) * u
        for ox in range(-3, 4):
            for oz in range(-3, 4):
                gx, gz = int(round(cx)) + ox, int(round(cz)) + oz
                top = int(round(surf(gx, gz)))
                for y in range(top - 4, top + 4):
                    w.put(gx, y, gz, "water" if y <= top - 2 else "air")
                # keep the deck line clear of grass tufts on the banks
                for y in range(top + 1, top + 6):
                    if w.get(gx, y, gz) in ("flower_violet", "flower_lilac", "flower_pale", "tall_grass"):
                        w.put(gx, y, gz, "air")
    deck = BP.V((a[0] + 0.5, a[1], a[2] + 0.5), (b[0] + 0.5, b[1], b[2] + 0.5),
                (a2[0] + 0.5, a2[1], a2[2] + 0.5), (b2[0] + 0.5, b2[1], b2[2] + 0.5),
                {"plank": COLORS["planks"], "edge": shade(COLORS["planks"], 0.83),
                 "rope": COLORS["iron"], "post": COLORS["cobble"], "net": (170, 170, 168),
                 "holes": 0.06, "sagmul": 1.08, "chain": False, "lantern": (255, 214, 150)})
    return w, dict(name="violet_hills", style="garden", deck=deck, posts=posts, half_w=hw,
                   post_blocks="cobble", sky=((168, 178, 214), (236, 226, 214)),
                   fog=(214, 214, 226), sun=(255, 236, 200))


def build_sizo_grove():
    w = World()
    surf = hilly(3.1, 1.6, 0.05, 8)
    for x in range(-20, 34):
        for z in range(-18, 22):
            h = int(round(surf(x, z)))
            w.put(x, h, z, "grass_top")
            for y in range(h - 4, h):
                w.put(x, y, z, "dirt" if y > h - 3 else "stone")
            if (x * 7 + z * 13) % 37 == 0:
                w.put(x, h + 1, z, "moss")
    rng = np.random.default_rng(3)
    for _ in range(1200):
        x = int(rng.integers(-20, 34)); z = int(rng.integers(-18, 22))
        r = rng.random()
        if r < 0.4:
            w.put(x, int(round(surf(x, z))) + 1, z, "tall_grass")
        elif r < 0.55:
            w.put(x, int(round(surf(x, z))) + 1, z, "flower_violet")
    for gx, gz in ((-10, -8), (2, 0), (14, -10), (-6, 12), (20, 10), (8, 14), (-14, 4)):
        x = gx + int(rng.integers(-2, 3)); z = gz + int(rng.integers(-2, 3))
        tree(w, TG.SIZO_GIANT, x, z, int(round(surf(x, z))) + 1, int(rng.integers(17, 22)),
             int(rng.integers(1, 1 << 30)), leaf="leaf_sizo", log="log_sizo", accent="moss")
    return w, dict(name="sizo_grove", style="grove", deck=None, posts=[],
                   sky=((186, 210, 200), (244, 240, 226)), fog=(228, 234, 228), sun=(255, 246, 220))


def build_waterfall_gardens():
    w = World()
    base = 9
    # terraced mound with a waterfall on the right
    for x in range(-18, 40):
        for z in range(-18, 24):
            d = math.hypot(x - 8, z - 2)
            step = int(max(0, 5 - d / 7.0))
            h = base + step * 2 + int(round(1.2 * math.sin(x * 0.2) + 1.2 * math.cos(z * 0.17)))
            w.put(x, h, z, "grass_top")
            for y in range(h - 3, h):
                w.put(x, y, z, "stone")
            if step == 0:
                for y in range(h + 1, base + 4):
                    w.put(x, y, z, "water")
    # waterfall column
    for x in range(16, 20):
        for z in range(-2, 2):
            for y in range(base + 2, base + 10):
                w.put(x, y, z, "water")
    rng = np.random.default_rng(5)
    for _ in range(3000):
        x = int(rng.integers(-18, 40)); z = int(rng.integers(-18, 24))
        y = max((yy for (xx, yy, zz) in w.b if (xx, zz) == (x, z)), default=None)
        if y is None:
            continue
        top = w.get(x, y, z)
        if top not in ("grass_top",):
            continue
        r = rng.random()
        if r < 0.42:
            w.put(x, y + 1, z, "tall_grass")
        elif r < 0.58:
            w.put(x, y + 1, z, "flower_pale")
        elif r < 0.70:
            w.put(x, y + 1, z, "flower_violet")
    for gx, gz in ((-8, 6), (2, -8), (12, 8), (-4, -6), (6, 4), (18, -4), (-12, -2), (4, 12)):
        x = gx + int(rng.integers(-2, 3)); z = gz + int(rng.integers(-2, 3))
        y = max((yy for (xx, yy, zz) in w.b if (xx, zz) == (x, z)), default=None)
        if y is None:
            continue
        tree(w, TG.FERN, x, z, y + 1, int(rng.integers(7, 10)), int(rng.integers(1, 1 << 30)),
             leaf="leaf_fern", log="log_fern", accent="tall_grass")
    return w, dict(name="waterfall_gardens", style="garden", deck=None, posts=[],
                   sky=((150, 186, 214), (232, 242, 236)), fog=(226, 238, 238), sun=(255, 250, 226))


def build_burnt_bridge():
    w = World()
    base = 8
    for x in range(-20, 42):
        for z in range(-16, 22):
            h = base + int(round(1.2 * math.sin(x * 0.15) + 1.0 * math.cos(z * 0.12)))
            if abs(x) > 2 or True:
                w.put(x, h, z, "ash")
                for y in range(h - 3, h):
                    w.put(x, y, z, "scorched")
            if (x * 5 + z * 11) % 29 == 0:
                w.put(x, h, z, "magma")
    # two charred piers carrying a burnt span with a collapsed middle
    pa, pb = (-6, 0), (14, 0)
    deck = None
    return w, dict(name="burnt_bridge", style="burnt", deck=None, posts=[],
                   sky=((92, 74, 68), (188, 142, 108)), fog=(150, 122, 104), sun=(255, 176, 120),
                   pier_bottom=6,
                   burnt=dict(a=(-6, base + 2, 0), b=(14, base + 2, 0), half_w=2,
                              break_from=0.44, break_to=0.63, piers=True))


# ---------------------------------------------------------------- rasteriser
FACE_DEFS = [
    ((0, 1, 0), [(0, 1, 0), (0, 1, 1), (1, 1, 1), (1, 1, 0)], 1.00),
    ((0, -1, 0), [(0, 0, 0), (1, 0, 0), (1, 0, 1), (0, 0, 1)], 0.52),
    ((0, 0, -1), [(0, 0, 0), (0, 1, 0), (1, 1, 0), (1, 0, 0)], 0.74),
    ((0, 0, 1), [(1, 0, 1), (1, 1, 1), (0, 1, 1), (0, 0, 1)], 0.82),
    ((-1, 0, 0), [(0, 0, 1), (0, 1, 1), (0, 1, 0), (0, 0, 0)], 0.88),
    ((1, 0, 0), [(1, 0, 0), (1, 1, 0), (1, 1, 1), (1, 0, 1)], 0.68),
]


def render(world, cfg, size=(1000, 620), eye=(-24, 26, -30), target=(8, 8, 2), fov=46, sun_dir=None):
    W, H = size
    cam = BP.Cam(eye, target, fov=fov, w=W, h=H)
    sky_top, sky_bot = cfg["sky"]
    img = Image.new("RGB", (W, H), sky_top)
    d = ImageDraw.Draw(img)
    for y in range(H):                                    # sky gradient
        t = y / float(H - 1)
        c = tuple(int(sky_top[i] * (1 - t) + sky_bot[i] * t) for i in range(3))
        d.line([(0, y), (W, y)], fill=c)
    # sun glow
    sx, sy = W * 0.78, H * 0.14
    for r in range(120, 0, -4):
        a = (1 - r / 120.0) ** 2 * 0.45
        col = tuple(int(sky_top[i] + (cfg["sun"][i] - sky_top[i]) * a) for i in range(3))
        d.ellipse([sx - r, sy - r * 0.7, sx + r, sy + r * 0.7], fill=col)

    # ---- bridge geometry (deck/cables/net) drawn as its own pass, sorted with the blocks
    prims = []

    def add_poly(pts3, color, depth_bias=0.0, alpha=255):
        p = [cam.p(q) for q in pts3]
        if any(q[2] <= 0.06 for q in p):
            return
        depth = sum(q[2] for q in p) / len(p) + depth_bias
        prims.append((depth, [(q[0], q[1]) for q in p], color, alpha))

    def add_tube(a, b, r, color):
        ax = tuple(b[i] - a[i] for i in range(3))
        L = math.sqrt(sum(v * v for v in ax)) or 1e-6
        ax = tuple(v / L for v in ax)
        ref = (0, 1, 0) if abs(ax[1]) < 0.9 else (1, 0, 0)
        s1 = BP.norm(BP.cross(ax, ref))
        s2 = BP.norm(BP.cross(ax, s1))
        for i in range(4):
            k1 = (s1[i % 3] * r, s2[i % 3] * r) if False else None
        corners = []
        for sx_, sy_ in ((1, 0), (0, 1), (-1, 0), (0, -1)):
            corners.append(tuple(a[i] + s1[i] * r * sx_ + s2[i] * r * sy_ for i in range(3)))
        for i in range(4):
            j = (i + 1) % 4
            add_poly([corners[i], corners[j],
                      tuple(b[k] + corners[j][k] - a[k] for k in range(3)),
                      tuple(b[k] + corners[i][k] - a[k] for k in range(3))], color)

    def draw_deck(a, b, half_w, holes, sagmul, chain, broken=None, sunk=0.0):
        ax, ay, az = a
        bx, by, bz = b
        dx, dz = bx - ax, bz - az
        L = math.hypot(dx, dz) or 1.0
        px, pz = -dz / L * half_w, dx / L * half_w
        v = BP.V((ax, ay - sunk, az), (bx, by - sunk, bz), (ax + px, ay - sunk, az + pz), (bx + px, by - sunk, bz + pz),
                 {"plank": None, "edge": None, "rope": None, "post": None, "net": None,
                  "holes": holes, "sagmul": sagmul, "chain": chain, "lantern": None})
        n = max(3, int(math.ceil(v.len() / 0.5)))
        plank_c = shade(COLORS["planks"] if cfg["style"] != "burnt" else COLORS["dark_planks"], 0.95)
        edge_c = shade(plank_c, 0.78)
        rope_c = COLORS["iron"] if not chain else (150, 148, 142)
        net_c = (176, 176, 172) if not chain else (138, 136, 132)
        for i in range(n):
            u = (i + 0.5) / n
            if broken and broken[0] < u < broken[1]:
                continue
            if not v.plank(u):
                continue
            u0, u1 = max(0.0, (i + 0.02) / n), min(1.0, (i + 0.98) / n)
            p00, p01 = v.pt(u0, 0), v.pt(u0, 1)
            p10, p11 = v.pt(u1, 0), v.pt(u1, 1)
            add_poly([p00, p10, p11, p01], plank_c)
            for q0, q1 in ((p00, p10), (p01, p11)):
                add_poly([q0, q1, (q1[0], q1[1] - 0.1, q1[2]), (q0[0], q0[1] - 0.1, q0[2])], edge_c)
        for side in (0, 1):
            prev = None
            for i in range(0, 49):
                u = i / 48.0
                if broken and broken[0] < u < broken[1]:
                    prev = None
                    continue
                c = v.cable(u, side)
                if prev:
                    add_tube(prev, c, 0.045, rope_c)
                prev = c
            hn = max(2, int(round(v.len() / 1.25)))
            for i in range(hn + 1):
                u = i / hn
                if broken and broken[0] < u < broken[1]:
                    continue
                top, bot = v.cable(u, side), v.edge(u, side)
                add_tube(top, bot, 0.02, rope_c)
                if not chain and i < hn:
                    u2 = (i + 1) / hn
                    if broken and broken[0] < u2 < broken[1]:
                        continue
                    t2, b2 = v.cable(u2, side), v.edge(u2, side)
                    # chain-link mesh: horizontal strands + diagonals (the photo's safety netting)
                    for h in (0.16, 0.34, 0.52, 0.70, 0.86):
                        add_tube(BP.lerp(bot, top, h), BP.lerp(b2, t2, h), 0.012, net_c)
                    add_tube(BP.lerp(bot, top, 0.02), BP.lerp(b2, t2, 0.98), 0.012, net_c)
                    add_tube(BP.lerp(bot, top, 0.98), BP.lerp(b2, t2, 0.02), 0.012, net_c)
        if broken:
            for k in range(3):
                u = broken[0] + 0.004 * k
                p = v.edge(min(1.0, u), k % 2)
                add_tube(p, (p[0], p[1] - 1.0 - k * 0.6, p[2]), 0.03, rope_c)
            mid = BP.lerp(v.pt(broken[0]), v.pt(broken[1]), 0.5)
            add_tube(v.pt(broken[0]), (mid[0], mid[1] - 1.3, mid[2]), 0.05, plank_c)
        for p in (a, (a[0] + px, a[1], a[2] + pz), (b[0] + px, b[1], b[2] + pz), b):
            burnt = cfg["style"] == "burnt"
            post_color = COLORS["cobble"] if not burnt else shade(COLORS["scorched"], 0.55)
            shaft = COLORS["dark_planks"] if not burnt else shade(COLORS["charred"], 0.9)
            lantern = (255, 208, 140) if not burnt else (255, 138, 74)
            wood = COLORS["planks"] if not burnt else shade(COLORS["charred"], 1.1)

            def box(x0, y0, z0, x1, y1, z1, col):
                for (nx, ny, nz), quad, sh in FACE_DEFS:
                    pts = [(x0 + (x1 - x0) * q[0], y0 + (y1 - y0) * q[1], z0 + (z1 - z0) * q[2]) for q in quad]
                    add_poly(pts, shade(col, sh * 0.96))
            box(p[0] + 0.12, p[1], p[2] + 0.12, p[0] + 0.88, p[1] + 0.32, p[2] + 0.88, post_color)   # plinth
            box(p[0] + 0.38, p[1] + 0.32, p[2] + 0.38, p[0] + 0.62, p[1] + 1.55, p[2] + 0.62, shaft)   # shaft
            box(p[0] + 0.31, p[1] + 0.82, p[2] + 0.31, p[0] + 0.69, p[1] + 1.0, p[2] + 0.69, wood)     # rope knot
            box(p[0] + 0.19, p[1] + 1.55, p[2] + 0.19, p[0] + 0.81, p[1] + 1.69, p[2] + 0.81, post_color)  # cap
            box(p[0] + 0.38, p[1] + 1.69, p[2] + 0.38, p[0] + 0.62, p[1] + 1.88, p[2] + 0.62, lantern)   # lantern
            box(p[0] + 0.31, p[1] + 1.88, p[2] + 0.31, p[0] + 0.69, p[1] + 1.97, p[2] + 0.69, post_color)

    if cfg.get("burnt"):
        bc = cfg["burnt"]
        draw_deck(bc["a"], bc["b"], bc["half_w"], 0.34, 1.28, True,
                  broken=(bc["break_from"], bc["break_to"]))
        for px_, pz_ in ((bc["a"][0], bc["a"][2]), (bc["b"][0], bc["b"][2])):
            for y in range(cfg.get("pier_bottom", 5), bc["a"][1]):
                for (nx, ny, nz), quad, sh in FACE_DEFS:
                    pts = [(px_ - 1 + 2 * q[0], y + q[1], pz_ - 1 + 2 * q[2]) for q in quad]
                    add_poly(pts, shade(COLORS["scorched"], sh * 0.48))
            add_poly([(px_ - 1.1, bc["a"][1], pz_ - 1.1), (px_ + 1.1, bc["a"][1], pz_ - 1.1),
                      (px_ + 1.1, bc["a"][1] + 0.35, pz_ + 1.1), (px_ - 1.1, bc["a"][1] + 0.35, pz_ + 1.1)],
                     shade(COLORS["ash"], 0.7))
    if cfg.get("bridge"):
        br = cfg["bridge"]
        draw_deck(br["a"], br["b"], br["half_w"], br["holes"], br["sag"], br["chain"])

    # ---- blocks
    def ao(x, y, z, nx, ny, nz):
        # darken faces that have neighbours around them (cheap ambient occlusion)
        cnt = 0
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            if d == (nx, ny, nz) or d == (-nx, -ny, -nz):
                continue
            if (x + nx + d[0], y + ny + d[1], z + nz + d[2]) in world.b:
                cnt += 1
        return 1.0 - 0.055 * cnt

    for (x, y, z), t in world.b.items():
        if t in LEAF_KINDS:
            c = px(t, x, y, z, 7)
            for (a0, a1) in (((0.08, 0.08), (0.92, 0.92)), ((0.92, 0.08), (0.08, 0.92))):
                add_poly([(x + a0[0], y, z + a0[1]), (x + a1[0], y, z + a1[1]),
                          (x + a1[0], y + 1, z + a1[1]), (x + a0[0], y + 1, z + a0[1])],
                         shade(c, 0.92), alpha=238)
                add_poly([(x + a0[0], y + 1, z + a0[1]), (x + a1[0], y + 1, z + a1[1]),
                          (x + a1[0], y, z + a1[1]), (x + a0[0], y, z + a0[1])],
                         shade(c, 0.78), alpha=238)
            add_poly([(x, y + 0.97, z), (x + 1, y + 0.97, z), (x + 1, y + 0.97, z + 1), (x, y + 0.97, z + 1)],
                     shade(c, 1.06), alpha=230)
            continue
        if t in PLANT_KINDS:
            c = tint(px(t, x, y, z, 11), "grass_top")
            for (a0, a1) in (((0.5, 0.05), (0.5, 0.95)), ((0.05, 0.5), (0.95, 0.5))):
                add_poly([(x + a0[0], y, z + a0[1]), (x + a1[0], y, z + a1[1]),
                          (x + a1[0], y + 0.85, z + a1[1]), (x + a0[0], y + 0.85, z + a0[1])], c, alpha=245)
            continue
        for (nx, ny, nz), quad, sh in FACE_DEFS:
            if (x + nx, y + ny, z + nz) in world.b:
                continue
            pts = [(x + q[0], y + q[1], z + q[2]) for q in quad]
            c = px(t, x, y, z, (nx + 2) * 7 + (ny + 2) * 3 + (nz + 2))
            col = shade(tint(c, t), sh * ao(x, y, z, nx, ny, nz))
            add_poly(pts, col, alpha=255 if t != "water" else 216)

    prims.sort(key=lambda p: -p[0])
    fog = cfg["fog"]
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ld = ImageDraw.Draw(layer)
    for depth, pts, color, alpha in prims:
        f = min(1.0, max(0.0, (depth - 30) / 110.0)) * 0.62
        c = tuple(int(color[i] * (1 - f) + fog[i] * f) for i in range(3))
        if alpha >= 255:
            d.polygon(pts, fill=c)
        else:
            ld.polygon(pts, fill=c + (alpha,))
    img = Image.alpha_composite(img.convert("RGBA"), layer).convert("RGB")
    d = ImageDraw.Draw(img)
    # soft vignette
    vig = Image.new("L", (W, H), 0)
    dv = ImageDraw.Draw(vig)
    dv.ellipse([-W * 0.25, -H * 0.35, W * 1.25, H * 1.35], fill=255)
    vig = vig.filter(ImageFilter.GaussianBlur(70))
    dark = Image.new("RGB", (W, H), (26, 24, 30))
    img = Image.composite(img, dark, vig)
    return img


def set_tint(hex_color, k=1.0):
    """Biome grass tint from data/riftworld/worldgen/biome/*.json (grass_color), like the game applies it."""
    r = ((hex_color >> 16) & 255) / 255.0
    g = ((hex_color >> 8) & 255) / 255.0
    b = (hex_color & 255) / 255.0
    # Minecraft's tint is applied to a grey texture; blend towards it so the ivory base does not blow out
    TINT["grass_top"] = (min(1.35, (r * 1.30) * k), min(1.35, (g * 1.14) * k), min(1.35, (b * 1.06) * k))


def main():
    scenes = []
    set_tint(10153932)                     # violet_hills grass_color
    w, cfg = build_violet_hills()
    br_a = (-1, int(round(7 + 2.4 * math.sin(-1 * 0.045 + 1.7))) + 1, -1)
    br_b = (9, int(round(7 + 2.4 * math.sin(9 * 0.045 + 1.7))) + 1, 9)
    cfg["bridge"] = dict(a=br_a, b=br_b, half_w=1.0, holes=0.06, sag=1.08, chain=False)
    scenes.append(("04_violet_hills", render(w, cfg, eye=(-34, 26, -42), target=(10, 10, 6), fov=32)))
    # the reference angle: standing on the deck in mid-span, looking along it (as on the user's photo)
    br = cfg["bridge"]
    ax, ay, az = br["a"]; bx, by, bz = br["b"]
    hw = br["half_w"]
    v = BP.V((ax, ay, az), (bx, by, bz), (ax, ay, az + 2 * hw), (bx, by, bz + 2 * hw),
             {"plank": None, "edge": None, "rope": None, "post": None, "net": None,
              "holes": br["holes"], "sagmul": br["sag"], "chain": False, "lantern": None})
    # standing on the deck, looking along the span (the user's photo angle)
    mid = v.pt(0.28, 0.5)
    scenes.append(("04_bridge_deck_view", render(w, cfg, eye=(mid[0], mid[1] + 1.75, mid[2]),
                   target=(bx + 0.5, by + 1.2, bz + 0.5), fov=66)))

    # close-up from the side: planks, stringers, cables, hangers, net, posts
    mid = (0.42 * (bx - ax) + ax, 0.42 * (by - ay) + ay, 0.42 * (bz - az) + az)
    scenes.append(("04_bridge_closeup", render(w, cfg,
                   eye=(mid[0] - 9.5, mid[1] + 3.6, mid[2] - 8.0),
                   target=(mid[0] + 0.4, mid[1] + 1.0, mid[2] + 0.4), fov=44)))

    set_tint(10273458)                     # sizo_grove grass_color
    w, cfg = build_sizo_grove()
    scenes.append(("05_sizo_grove", render(w, cfg, eye=(-30, 34, -44), target=(6, 17, 2), fov=40)))

    set_tint(7321674, 0.74)                # waterfall_gardens grass_color (deeper, muted - not neon)
    w, cfg = build_waterfall_gardens()
    scenes.append(("07_waterfall_gardens", render(w, cfg, eye=(-33, 30, -41), target=(8, 11, 2), fov=42)))

    set_tint(5917748)                      # ashen_lands grass_color
    w, cfg = build_burnt_bridge()
    cfg["burnt"]["a"] = (-6, 10, 0)
    cfg["burnt"]["b"] = (14, 10, 0)
    cfg["pier_bottom"] = 6
    scenes.append(("ashen_burnt_bridge", render(w, cfg, eye=(-20, 14, -28), target=(4, 10, 0), fov=40)))

    for name, img in scenes:
        p = os.path.join(OUT, name + ".png")
        img.save(p)
        print("written", p)


if __name__ == "__main__":
    main()
