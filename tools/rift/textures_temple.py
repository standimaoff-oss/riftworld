"""Rev 4 item 3: 16x16 vanilla-style textures of the Great Temple (ivory & gold, ritual mosaic)."""

raise SystemExit(
    "obsolete: this generator belongs to content that was deleted from the mod\n"
    "(the laboratory and the Rev 4 temple mosaic). It would rebuild assets that no longer\n"
    "exist - see README, Rev 5. The files are kept only for reference.")
from PIL import Image, ImageDraw
import math, random

A = "/home/user/riftworld/mod/src/main/resources/assets/riftworld/textures/block/"
rng = random.Random(41)

def grain(d, base, amp=8):
    for _ in range(60):
        x, y = rng.randrange(16), rng.randrange(16)
        v = rng.randrange(-amp, amp + 1)
        c = tuple(max(0, min(255, b + v)) for b in base)
        d.point((x, y), fill=c)

def bricks():
    im = Image.new("RGB", (16, 16), (226, 218, 200))
    d = ImageDraw.Draw(im)
    for k in range(4):
        y = k * 4
        d.line([(0, y), (15, y)], fill=(188, 178, 158))
        off = 4 if k % 2 else 0
        for x in range(off, 16, 8):
            d.line([(x, y), (x, y + 3)], fill=(196, 186, 166))
    grain(d, (226, 218, 200))
    im.save(A + "temple_bricks.png")

def gold():
    im = Image.new("RGB", (16, 16), (250, 210, 96))
    d = ImageDraw.Draw(im)
    for k in range(4):
        d.line([(0, k * 4), (15, k * 4)], fill=(214, 172, 60))
        d.line([(k * 4, 0), (k * 4, 15)], fill=(214, 172, 60))
    for k in range(4):
        x, y = 2 + (k % 2) * 8, 2 + (k // 2) * 8
        d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=(255, 236, 150))
    grain(d, (250, 210, 96), 10)
    im.save(A + "temple_gold.png")

def pillar():
    im = Image.new("RGB", (16, 16), (232, 224, 206))
    d = ImageDraw.Draw(im)
    for x in range(16):
        shade = int(22 * math.sin(x * math.pi * 4 / 16))
        c = tuple(max(0, b - shade) for b in (232, 224, 206))
        d.line([(x, 0), (x, 15)], fill=c)
    for y in (1, 2, 13, 14):
        d.line([(0, y), (15, y)], fill=(240, 210, 110))
    grain(d, (232, 224, 206), 6)
    im.save(A + "temple_pillar.png")
    im = Image.new("RGB", (16, 16), (238, 230, 212))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 15, 15], outline=(240, 210, 110))
    d.rectangle([3, 3, 12, 12], outline=(200, 190, 168))
    grain(d, (238, 230, 212), 6)
    im.save(A + "temple_pillar_top.png")

def floor():
    im = Image.new("RGB", (16, 16), (236, 232, 222))
    d = ImageDraw.Draw(im)
    for y in range(0, 16, 8):
        for x in range(0, 16, 8):
            if (x + y) % 16 == 8:
                d.rectangle([x, y, x + 7, y + 7], fill=(206, 200, 186))
    d.rectangle([7, 7, 8, 8], fill=(240, 210, 110))
    for y in range(0, 16, 8):
        d.line([(0, y), (15, y)], fill=(168, 162, 148)); d.line([(y, 0), (y, 15)], fill=(168, 162, 148))
    grain(d, (236, 232, 222), 6)
    im.save(A + "temple_floor.png")

def mosaic(core, stage):
    im = Image.new("RGB", (16, 16), (20, 18, 46))
    d = ImageDraw.Draw(im)
    for k in range(0, 16, 4):
        d.line([(0, k), (15, k)], fill=(30, 28, 60)); d.line([(k, 0), (k, 15)], fill=(30, 28, 60))
    if core:
        # centre sigil: horned disc in a circle
        d.ellipse([1, 1, 14, 14], outline=(212, 172, 80))
        d.ellipse([5, 5, 10, 10], outline=(212, 172, 80))
        d.line([(3, 2), (6, 6)], fill=(212, 172, 80)); d.line([(12, 2), (9, 6)], fill=(212, 172, 80))
        d.point((8, 8), fill=(230, 60, 60))
    else:
        # ring tiles: arc quadrant + little runes
        d.arc([4, 4, 11, 11], 30, 330, fill=(196, 158, 70))
        d.point((2, 2), fill=(196, 158, 70)); d.point((13, 13), fill=(196, 158, 70))
    if stage > 0:                                 # the glow bleeds through the seams
        glow = [(0, 0, 0), (90, 30, 40), (176, 52, 44), (255, 96, 48)][stage]
        for k in range(stage * 26):
            x, y = rng.randrange(16), rng.randrange(16)
            if (x + y) % 2 == 0:
                d.point((x, y), fill=glow)
        if stage >= 2:
            d.ellipse([1, 1, 14, 14], outline=glow) if core else d.arc([4, 4, 11, 11], 30, 330, fill=glow)
        if stage == 3:
            for x in range(8):
                d.line([(x, 15 - x), (x + 0, 15 - x)], fill=(255, 200, 120))
            d.point((8, 8), fill=(255, 220, 160))
    im.save(A + ("mosaic_core" if core else "temple_mosaic") + f"_{stage}.png")

def candle():
    im = Image.new("RGB", (16, 16), (238, 230, 214))
    d = ImageDraw.Draw(im)
    for y in range(16):
        v = rng.randrange(-6, 7)
        d.line([(0, y), (15, y)], fill=tuple(max(0, min(255, b + v)) for b in (238 - y * 2, 230 - y * 2, 214 - y * 2)))
    d.rectangle([0, 12, 15, 15], fill=(212, 172, 80))
    im.save(A + "ritual_candle.png")
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.ellipse([5, 4, 10, 11], fill=(255, 238, 170, 255))
    d.ellipse([6, 2, 9, 8], fill=(255, 214, 96, 255))
    im.save(A + "ritual_flame.png")

def bridge():
    im = Image.new("RGBA", (16, 16), (190, 230, 255, 200))
    d = ImageDraw.Draw(im)
    for _ in range(24):
        x, y = rng.randrange(16), rng.randrange(16)
        d.point((x, y), fill=(255, 255, 255, 220))
    for k in (3, 11):
        d.line([(0, k), (15, k)], fill=(150, 210, 250, 180))
    im.save(A + "light_bridge.png")

bricks(); gold(); pillar(); floor()
for s in range(4):
    mosaic(False, s); mosaic(True, s)
candle(); bridge()
print("TEMPLE_TEX_OK")
