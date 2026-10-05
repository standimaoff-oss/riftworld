# Textures for the boss «Морграт», his guards and their weapons (32x32 = 16 px per block at uvbox scale 0.5).
# python3 tools/rift/textures_creatures.py  -> tools/rift/tex_models/m_b_*.png
import os, random, math
from PIL import Image

OUT = "/home/user/tools/rift/tex_models"
N = 32


def clamp(v):
    return max(0, min(255, int(v)))


def noise_img(seed, base, var, fn=None):
    r = random.Random(seed)
    im = Image.new("RGBA", (N, N))
    px = im.load()
    # value noise on a 4x4 lattice (tileable)
    g = [[r.random() for _ in range(8)] for _ in range(8)]

    def vn(x, y):
        x0, y0 = int(x) % 8, int(y) % 8
        fx, fy = x - int(x), y - int(y)
        a, b = g[y0][x0], g[y0][(x0 + 1) % 8]
        c, d = g[(y0 + 1) % 8][x0], g[(y0 + 1) % 8][(x0 + 1) % 8]
        sx, sy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
        return (a + (b - a) * sx) * (1 - sy) + (c + (d - c) * sx) * sy

    for y in range(N):
        for x in range(N):
            n = vn(x / 4.0, y / 4.0) * 0.65 + r.random() * 0.35
            col = [base[i] + (n - 0.5) * var * 2 for i in range(3)]
            if fn:
                col = fn(x, y, n, col, r)
            px[x, y] = (clamp(col[0]), clamp(col[1]), clamp(col[2]), 255)
    return im


def save(im, name):
    im.save(os.path.join(OUT, name + ".png"))
    print("TEX", name)


# boss hide: charcoal-brown leathery skin, dark cracks with a faint ember glow deep inside
def hide_fn(x, y, n, col, r):
    if (x * 7 + y * 3) % 11 == 0 and r.random() < 0.55:
        return [col[0] * 0.45 + 40, col[1] * 0.4 + 6, col[2] * 0.4]
    if (x + y * 5) % 13 == 0:
        return [c * 0.6 for c in col]
    return col


save(noise_img(11, (52, 38, 34), 16, hide_fn), "m_b_hide")


# guards: ash-grey dried skin with darker sinew stripes running down
def ash_fn(x, y, n, col, r):
    if x % 6 in (0, 1) and r.random() < 0.7:
        return [c * 0.72 for c in col]
    return col


save(noise_img(12, (92, 84, 78), 14, ash_fn), "m_b_skin_ash")


# porous bone (yellowed)
def bone_fn(x, y, n, col, r):
    if r.random() < 0.07:
        return [c * 0.62 for c in col]
    return col


save(noise_img(13, (196, 182, 150), 14, bone_fn), "m_b_bone")


# rotten dark-red rag with weave and holes darkened
def rag_fn(x, y, n, col, r):
    if (x % 2 == 0) != (y % 2 == 0):
        col = [c * 0.88 for c in col]
    if n < 0.25:
        col = [c * 0.55 for c in col]
    return col


save(noise_img(14, (92, 22, 18), 14, rag_fn), "m_b_rag")


# black shaggy mane (vertical strands)
def fur_fn(x, y, n, col, r):
    k = ((x * 37) % 5) / 5.0
    return [c * (0.75 + 0.4 * k) for c in col]


save(noise_img(15, (30, 25, 24), 8, fur_fn), "m_b_fur")


# blade steel: dark blued steel with long scratches
def blade_fn(x, y, n, col, r):
    if (y * 3 + x) % 17 == 0:
        return [c * 1.35 for c in col]
    return col


save(noise_img(16, (56, 58, 64), 10, blade_fn), "m_b_blade")


# old dark wood of the scythe snath with grain
def snath_fn(x, y, n, col, r):
    k = math.sin(x * 0.9 + n * 3) * 0.12
    return [c * (1 + k) for c in col]


save(noise_img(17, (64, 42, 28), 10, snath_fn), "m_b_snath")
