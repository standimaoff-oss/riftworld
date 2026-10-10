"""Биом улучшение: ауритовая руда (прожилки в ярусах Висячих садов) + городской червонец (валюта)."""
from PIL import Image, ImageDraw
import random

B = "/home/user/mods/riftworld/src/main/resources/assets/riftworld/textures/block/"
I = "/home/user/mods/riftworld/src/main/resources/assets/riftworld/textures/item/"

GOLD = (238, 200, 74)
GOLD_HI = (255, 230, 130)
GOLD_LO = (178, 134, 40)


def ore():
    """Golden-coin ore speckles over the plain elisar stone (same bg => seamless shelves)."""
    base = Image.open(B + "elisar_stone.png").convert("RGB")
    im = base.copy()
    d = ImageDraw.Draw(im)
    rng = random.Random(31337)
    # 7 clusters of gold specs: 2-3 px clumps, vanilla-ore feel, not on a strict grid
    seeds = [(3, 3), (11, 2), (6, 7), (13, 9), (2, 11), (9, 13), (7, 10)]
    for cx, cy in seeds:
        n = rng.choice((2, 3))
        pts = [(cx + rng.randrange(-1, 2), cy + rng.randrange(-1, 2)) for _ in range(n)]
        for j, (x, y) in enumerate(pts):
            if 0 <= x <= 15 and 0 <= y <= 15:
                d.point((x, y), fill=GOLD)
        # glint on one pixel of the cluster
        d.point((cx % 16, cy % 16), fill=GOLD_HI)
        # and a shadow pit under a random clump
        sx, sy = pts[-1]
        if sy + 1 <= 15 and rng.random() < 0.8:
            d.point((sx, sy + 1), fill=GOLD_LO)
    # two solo glints (silver sheen of exposed coin metal)
    d.point((14, 14), fill=GOLD_HI)
    d.point((0, 6), fill=GOLD_LO)
    im.save(B + "aurite_ore.png")


def ducat():
    """City ducat: gold disc, milled rim, violet city-crest (romb + dot) like the temple sigils."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    # disc (slightly soft square coin, 12x12)
    d.ellipse((2, 2, 13, 13), fill=GOLD)
    # rim: milled dark edge + light edge (arc ticks)
    d.ellipse((2, 2, 13, 13), outline=GOLD_LO)
    d.ellipse((3, 3, 12, 12), outline=(226, 180, 56))
    for k in range(8):
        import math
        a = k * math.pi / 4 + math.pi / 8
        x, y = 7.5 + 6.0 * math.cos(a), 7.5 + 6.0 * math.sin(a)
        d.point((round(x), round(y)), fill=GOLD_LO)
    # highlight arc (top-left sheen)
    d.arc((4, 4, 11, 11), 150, 250, fill=GOLD_HI)
    # violet crest: rhombus with a centre eye
    V = (104, 76, 148)
    cx = cy = 7.5
    for r in range(0, 4):
        dy = 3 - r
        d.line([(cx - r, cy - r), (cx + r, cy - r)], fill=V)
        d.line([(cx - r, cy + r), (cx + r, cy + r)], fill=V)
    d.point((8, 7), fill=GOLD_HI)
    d.point((7, 8), fill=(60, 40, 96))
    im.save(I + "elisar_ducat.png")


ore()
ducat()
print("aurite ok")
