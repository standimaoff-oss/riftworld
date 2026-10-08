"""Rev 5 «Разлом»: 16x16 textures of the hellish hall and the 256x256 red hexagram floor plate.

Run:  python3 tools/rift/textures_rift.py     (writes into mod/src/main/resources/assets/riftworld/textures)
"""
from PIL import Image, ImageDraw
import math
import os
import random

A = "/home/user/riftworld/mod/src/main/resources/assets/riftworld/textures/"
BLOCK = A + "block/"
ITEM = A + "item/"
os.makedirs(BLOCK, exist_ok=True)
os.makedirs(ITEM, exist_ok=True)
rng = random.Random(59)

DARK = (26, 25, 33)
DARK2 = (34, 32, 41)
BLACK = (14, 13, 18)
CRIM = (150, 22, 30)
CRIM_HI = (226, 62, 48)
GOLD = (196, 150, 62)


def grain(d, base, amp=7, n=70):
    for _ in range(n):
        x, y = rng.randrange(16), rng.randrange(16)
        v = rng.randrange(-amp, amp + 1)
        d.point((x, y), fill=tuple(max(0, min(255, c + v)) for c in base))


def veins(d, count=5, colour=CRIM):
    """Thin crimson veins crawling through the stone."""
    for _ in range(count):
        x, y = rng.randrange(16), rng.randrange(16)
        for _ in range(rng.randrange(4, 11)):
            d.point((x, y), fill=colour)
            x += rng.choice((-1, 0, 1, 1, 0))
            y += rng.choice((-1, 0, 1, 1, 0))
            x %= 16
            y %= 16


def rift_stone():
    im = Image.new("RGB", (16, 16), DARK)
    d = ImageDraw.Draw(im)
    for k in range(0, 16, 8):
        d.line([(0, k), (15, k)], fill=DARK2)
        d.line([(k, 0), (k, 15)], fill=DARK2)
    grain(d, DARK)
    veins(d, 4)
    im.save(BLOCK + "rift_stone.png")


def rift_bricks(cracked):
    im = Image.new("RGB", (16, 16), DARK2)
    d = ImageDraw.Draw(im)
    for k in range(4):
        y = k * 4
        d.line([(0, y), (15, y)], fill=(88, 26, 34) if k % 2 == 0 else (58, 20, 26))
        off = 4 if k % 2 else 0
        for x in range(off, 16, 8):
            d.line([(x, y), (x, y + 3)], fill=(58, 20, 26))
    for k in range(4):
        y = k * 4 + 1
        for x in range(16):
            if rng.random() < 0.25:
                d.point((x, y + rng.randrange(0, 3)), fill=(44, 40, 50))
    if cracked:
        for _ in range(3):
            x, y = rng.randrange(16), rng.randrange(16)
            for _ in range(rng.randrange(5, 12)):
                d.point((x, y), fill=(10, 9, 12))
                x += rng.choice((-1, 0, 1))
                y += rng.choice((0, 1))
                if not (0 <= x < 16 and 0 <= y < 16):
                    break
    grain(d, DARK2, 5, 40)
    im.save(BLOCK + ("rift_stone_bricks_cracked.png" if cracked else "rift_stone_bricks.png"))


def rift_pillar():
    im = Image.new("RGB", (16, 16), DARK)
    d = ImageDraw.Draw(im)
    for x in range(16):
        shade = int(16 * math.sin(x * math.pi * 4 / 16))
        d.line([(x, 0), (x, 15)], fill=tuple(max(0, c - shade) for c in DARK))
    for y in (1, 2, 13, 14):
        d.line([(0, y), (15, y)], fill=(104, 24, 30))
    for y in (6, 7):
        d.line([(3, y), (12, y)], fill=CRIM)
    grain(d, DARK, 5, 40)
    im.save(BLOCK + "rift_pillar.png")

    top = Image.new("RGB", (16, 16), DARK2)
    dt = ImageDraw.Draw(top)
    dt.ellipse([2, 2, 13, 13], outline=CRIM)
    for i in range(6):                                     # tiny hexagram of the pillar cap
        a = i * math.pi / 3
        a2 = a + 2 * math.pi / 3
        dt.line([(8 + 5 * math.cos(a), 8 + 5 * math.sin(a)), (8 + 5 * math.cos(a2), 8 + 5 * math.sin(a2))], fill=CRIM_HI)
    grain(dt, DARK2, 5, 30)
    top.save(BLOCK + "rift_pillar_top.png")


def rift_vein():
    im = Image.new("RGB", (16, 16), BLACK)
    d = ImageDraw.Draw(im)
    for _ in range(7):
        x, y = rng.randrange(16), rng.randrange(16)
        for _ in range(rng.randrange(6, 16)):
            d.point((x, y), fill=rng.choice((CRIM, CRIM_HI, (255, 150, 90))))
            x += rng.choice((-1, 0, 1, 1))
            y += rng.choice((-1, 0, 1))
            x %= 16
            y %= 16
    im.save(BLOCK + "rift_vein.png")


def rift_ash():
    im = Image.new("RGB", (16, 16), (24, 22, 26))
    d = ImageDraw.Draw(im)
    for _ in range(150):
        x, y = rng.randrange(16), rng.randrange(16)
        c = rng.choice(((34, 30, 34), (18, 17, 20), (46, 36, 36), (30, 24, 24)))
        d.point((x, y), fill=c)
    for _ in range(6):
        x, y = rng.randrange(16), rng.randrange(16)
        d.point((x, y), fill=(96, 40, 34))
    im.save(BLOCK + "rift_ash.png")


def rift_candle():
    im = Image.new("RGB", (16, 16), (18, 16, 20))
    d = ImageDraw.Draw(im)
    for x in range(5, 11):                                  # black wax column
        for y in range(2, 15):
            v = rng.randrange(-5, 6)
            d.point((x, y), fill=(28 + v, 24 + v, 30 + v))
    for y in (11, 12, 13, 14):                              # wax drips, crimson
        d.line([(5, y), (10, y)], fill=(120, 26, 30))
    d.line([(5, 2), (10, 2)], fill=(150, 40, 40))
    d.rectangle([3, 14, 12, 15], fill=(38, 34, 40))         # foot
    d.rectangle([3, 13, 12, 13], fill=(96, 24, 28))
    im.save(BLOCK + "rift_candle.png")

    fl = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    df = ImageDraw.Draw(fl)
    df.ellipse([6, 4, 10, 12], fill=(255, 90, 60, 255))
    df.ellipse([7, 2, 9, 8], fill=(255, 170, 90, 255))
    df.ellipse([7, 6, 9, 11], fill=(255, 240, 200, 255))
    fl.save(BLOCK + "rift_flame.png")


def rift_altar_tex():
    im = Image.new("RGB", (16, 16), DARK2)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 15, 15], outline=GOLD)
    d.rectangle([2, 2, 13, 13], outline=(72, 22, 28))
    for i in range(6):
        a = i * math.pi / 3
        a2 = a + 2 * math.pi / 3
        d.line([(8 + 5 * math.cos(a), 8 + 5 * math.sin(a)), (8 + 5 * math.cos(a2), 8 + 5 * math.sin(a2))], fill=CRIM)
    d.point((8, 8), fill=CRIM_HI)
    grain(d, DARK2, 5, 30)
    im.save(BLOCK + "rift_altar.png")

    top = Image.new("RGB", (16, 16), BLACK)
    dt = ImageDraw.Draw(top)
    dt.ellipse([1, 1, 14, 14], outline=GOLD)
    dt.ellipse([5, 5, 10, 10], fill=(44, 12, 16))
    dt.point((8, 8), fill=(255, 200, 150))
    grain(dt, BLACK, 4, 25)
    top.save(BLOCK + "rift_altar_top.png")


def crystal(stage):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    body = [(8, 1), (13, 7), (8, 15), (3, 7)]
    base = (60, 12, 18) if stage == 0 else (150 + 20 * stage, 30 + 15 * stage, 40)
    d.polygon(body, fill=base + (255,))
    hi = (120, 30, 40) if stage == 0 else (255, 170, 120)
    d.line([(8, 2), (8, 14)], fill=hi + (255,))
    if stage >= 1:
        for i in range(stage * 6):
            d.point((rng.randrange(16), rng.randrange(16)), fill=(255, 190, 150, 200))
    im.save(BLOCK + f"rift_crystal_{stage}.png")


# ------------------------------------------------------------------ the hexagram floor plate
PLATE = 256          # 16 px per block x 16 blocks of bezel
BLOCKS = 13          # the altar plate covers 13 x 13 blocks
PPB = PLATE // 16     # 16 px per block


def line(d, p, q, fill, w):
    d.line([p, q], fill=fill, width=w, joint="curve")


def hexagram(stage):
    """The sign on the floor: a red hexagram (Star of David) inside a ring, glowing hotter by stage."""
    im = Image.new("RGB", (PLATE, PLATE), (16, 14, 19))
    d = ImageDraw.Draw(im)
    c = PLATE / 2.0
    # plate bezel: gold frame + black marble floor
    d.rectangle([0, 0, PLATE - 1, PLATE - 1], outline=(150, 112, 44))
    d.rectangle([3, 3, PLATE - 4, PLATE - 4], outline=(60, 42, 26))
    for _ in range(2600):                                  # marble dust
        x, y = rng.randrange(PLATE), rng.randrange(PLATE)
        v = rng.randrange(-6, 7)
        d.point((x, y), fill=(max(0, 16 + v), max(0, 14 + v), max(0, 19 + v)))
    R = 0.40 * PLATE
    hot = [(84, 18, 24), (150, 30, 32), (214, 58, 40), (255, 132, 70)][stage]
    core = [(140, 40, 40), (210, 70, 50), (255, 120, 70), (255, 226, 190)][stage]
    w = 7 + stage                                        # the lines thicken as it burns
    ring = [0.47 * PLATE, 0.475 * PLATE, 0.48 * PLATE, 0.487 * PLATE][stage]
    d.ellipse([c - ring, c - ring, c + ring, c + ring], outline=hot, width=4 + stage)
    for turn in (0, math.pi / 3):                        # the two triangles of the sign
        pts = []
        for i in range(3):
            a = turn + i * 2 * math.pi / 3 - math.pi / 2
            pts.append((c + R * math.cos(a), c + R * math.sin(a)))
        for i in range(3):
            line(d, pts[i], pts[(i + 1) % 3], hot, w)
    if stage >= 1:                                        # a second, brighter core line
        for turn in (0, math.pi / 3):
            pts = []
            for i in range(3):
                a = turn + i * 2 * math.pi / 3 - math.pi / 2
                pts.append((c + R * math.cos(a), c + R * math.sin(a)))
            for i in range(3):
                line(d, pts[i], pts[(i + 1) % 3], core, max(1, w - 5))
    # the demon mark in the middle: a horned disc
    d.ellipse([c - 22, c - 22, c + 22, c + 22], outline=core, width=3)
    d.ellipse([c - 8, c - 8, c + 8, c + 8], fill=(hot if stage < 2 else core))
    d.line([(c - 20, c - 14), (c - 26, c - 34)], fill=core, width=4)
    d.line([(c + 20, c - 14), (c + 26, c - 34)], fill=core, width=4)
    for i in range(12):                                   # twelve candle marks at the ring
        a = i * math.pi / 6 + math.pi / 12
        x, y = c + ring * math.cos(a), c + ring * math.sin(a)
        d.ellipse([x - 4, y - 4, x + 4, y + 4], fill=hot)
    for _ in range(stage * 900):                          # embers bleeding out of the seams
        x, y = rng.randrange(PLATE), rng.randrange(PLATE)
        if rng.random() < 0.4:
            d.point((x, y), fill=core)
    if stage >= 2:                                        # the plate starts to break
        for _ in range(6 + stage * 4):
            a = rng.random() * math.tau
            x, y = c + R * math.cos(a), c + R * math.sin(a)
            for _ in range(rng.randrange(20, 70)):
                d.point((x, y), fill=(10, 8, 10))
                x += math.cos(a) * rng.choice((0, 1, 1, 2))
                y += math.sin(a) * rng.choice((0, 1, 1, 2))
    if stage == 3:                                        # molten centre, space torn open
        d.ellipse([c - 30, c - 30, c + 30, c + 30], outline=(255, 240, 210), width=3)
        for i in range(3):
            rr = 10 + i * 8
            d.ellipse([c - rr, c - rr, c + rr, c + rr], outline=(255, 120, 60), width=2)
    im.save(BLOCK + f"sigil_{stage}.png")


def item_icons():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.ellipse([0, 0, 15, 15], outline=(150, 112, 44))
    for turn in (0, math.pi / 3):
        pts = []
        for i in range(3):
            a = turn + i * 2 * math.pi / 3 - math.pi / 2
            pts.append((8 + 6.2 * math.cos(a), 8 + 6.2 * math.sin(a)))
        for i in range(3):
            d.line([pts[i], pts[(i + 1) % 3]], fill=(214, 58, 40), width=2)
    d.point((8, 8), fill=(255, 200, 150))
    im.save(ITEM + "rift_altar.png")


rift_stone()
rift_bricks(False)
rift_bricks(True)
rift_pillar()
rift_vein()
rift_ash()
rift_candle()
rift_altar_tex()
for s in range(4):
    crystal(s)
    hexagram(s)
item_icons()
print("RIFT_TEX_OK")
