"""Мини-биом «Туманный данж»: серо-зелёный обломчатый камень утёсов + клетка-фонарь. 16x16."""
from PIL import Image, ImageDraw
import random

B = "/home/user/mods/riftworld/src/main/resources/assets/riftworld/textures/block/"
rng = random.Random(721)

BASE = (148, 152, 148)      # misty green-gray
DARK = (112, 116, 112)
LIGHT = (172, 176, 170)
MOSS = (96, 124, 78)


def grain(d, base, amp=10, n=70):
    for _ in range(n):
        x, y = rng.randrange(16), rng.randrange(16)
        v = rng.randrange(-amp, amp + 1)
        d.point((x, y), fill=tuple(max(0, min(255, b + v)) for b in base))


def cube(name, cracks=0, moss=0):
    im = Image.new("RGB", (16, 16), BASE)
    d = ImageDraw.Draw(im)
    # blocky ashlar seams (2 courses, offset)
    d.line([(0, 7), (15, 7)], fill=DARK)
    d.line([(6, 0), (6, 7)], fill=DARK)
    d.line([(11, 8), (11, 15)], fill=DARK)
    for _ in range(cracks):
        x, y = rng.randrange(2, 13), rng.randrange(2, 13)
        for k in range(3):
            d.point((x + k, y + (k if rng.random() < 0.5 else -k)), fill=(84, 88, 84))
    for _ in range(moss):
        x, y = rng.randrange(0, 14), rng.randrange(0, 14)
        for k in range(rng.randrange(2, 5)):
            d.point((x + rng.randrange(0, 2), y + rng.randrange(0, 2)), fill=MOSS)
    grain(d, BASE)
    # corner shading
    for i in range(16):
        d.point((i, 15), fill=tuple(max(0, c - 14) for c in im.getpixel((i, 15))))
        d.point((15, i), fill=tuple(max(0, c - 10) for c in im.getpixel((15, i))))
        d.point((i, 0), fill=tuple(min(255, c + 10) for c in im.getpixel((i, 0))))
        d.point((0, i), fill=tuple(min(255, c + 6) for c in im.getpixel((0, i))))
    im.save(B + name + ".png")


def cage():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    IRON = (52, 54, 60)
    IRON_HI = (86, 88, 96)
    GLOW = (255, 214, 140)
    # cage silhouette: dome top + 4 bars + base ring
    d.arc((3, 1, 12, 11), 180, 360, fill=IRON)         # dome
    for x in (3, 6, 9, 12):
        d.line([(x, 5), (x, 13)], fill=IRON)
    d.line([(3, 13), (12, 13)], fill=IRON)
    d.line([(4, 12), (11, 12)], fill=IRON_HI)
    # warm core glow
    d.rectangle((6, 7, 9, 12), fill=GLOW)
    d.point((7, 9), fill=(255, 240, 190))
    d.point((8, 10), fill=(255, 240, 190))
    d.point((7, 4), fill=(255, 128, 60))
    im.save(B + "keep_cage_lantern.png")


cube("keep_stone")
cube("keep_stone_cracked", cracks=9)
cube("keep_stone_mossy", moss=26)
cage()
print("mistkeep ok")
