"""Rev 4 item 3: 16x16 vanilla-style textures of the Great Temple (ivory & gold, ritual mosaic)."""
from pixel16 import Image, ImageDraw
import math, random, os

from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
MOD = Path(os.environ.get("RIFT_MOD_DIR", ROOT / "mod"))
A = str(MOD / "src/main/resources/assets/riftworld/textures/block") + "/"
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

def understone():
    im = Image.new("RGB", (16, 16), (52, 47, 66))
    d = ImageDraw.Draw(im)
    for y in (0, 8):
        d.line([(0, y), (15, y)], fill=(31, 29, 43))
    for row in range(2):
        y = row * 8
        off = 4 if row else 0
        for x in range(off, 16, 8):
            d.line([(x, y), (x, y + 7)], fill=(35, 32, 49))
    for _ in range(18):
        x, y = rng.randrange(16), rng.randrange(16)
        d.point((x, y), fill=rng.choice(((82, 70, 94), (39, 71, 78), (114, 78, 66))))
    im.save(A + "temple_understone.png")

def maze_floor():
    im = Image.new("RGB", (16, 16), (83, 77, 95))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 15, 15], outline=(42, 40, 57))
    d.rectangle([2, 2, 13, 13], outline=(113, 100, 123))
    d.line([(0, 8), (3, 8)], fill=(205, 171, 97)); d.line([(12, 8), (15, 8)], fill=(205, 171, 97))
    d.line([(8, 0), (8, 3)], fill=(205, 171, 97)); d.line([(8, 12), (8, 15)], fill=(205, 171, 97))
    d.point((8, 8), fill=(189, 129, 78))
    grain(d, (83, 77, 95), 8)
    im.save(A + "temple_maze_floor.png")

def garden_soil():
    im = Image.new("RGB", (16, 16), (64, 51, 48))
    d = ImageDraw.Draw(im)
    for _ in range(45):
        x, y = rng.randrange(16), rng.randrange(16)
        d.point((x, y), fill=rng.choice(((81, 62, 53), (50, 75, 48), (112, 83, 59))))
    for x in (3, 12):
        d.point((x, 2), fill=(137, 101, 62))
        d.point((x - 1, 3), fill=(45, 97, 56))
    im.save(A + "temple_garden_soil.png")

def moss():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for _ in range(36):
        x, y = rng.randrange(16), rng.randrange(16)
        d.point((x, y), fill=rng.choice(((59, 114, 76, 255), (86, 139, 91, 255), (43, 87, 72, 230))))
    for x, y in ((3, 4), (7, 8), (12, 3), (12, 12)):
        d.rectangle([x, y, x + 1, y + 1], fill=(138, 171, 104, 255))
    im.save(A + "temple_moss.png")

def plants():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    d.line([(8, 15), (8, 4)], fill=(94, 139, 89, 255), width=1)
    for pts in ([(8, 12), (2, 9), (5, 8)], [(8, 10), (14, 7), (11, 6)], [(8, 7), (3, 4), (6, 3)], [(8, 6), (12, 2), (10, 2)]):
        d.polygon(pts, fill=(92, 153, 101, 255), outline=(44, 103, 72, 255))
    im.save(A + "temple_fern.png")

    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    d.line([(8, 15), (8, 7)], fill=(66, 113, 73, 255), width=1)
    for x, y, c in ((8, 4, (169, 112, 222, 255)), (5, 5, (194, 144, 237, 255)), (11, 5, (133, 114, 229, 255)),
                    (6, 8, (121, 178, 115, 255)), (10, 9, (99, 156, 99, 255))):
        d.rectangle([x - 1, y - 1, x + 1, y + 1], fill=c)
    d.point((8, 5), fill=(255, 221, 142, 255))
    im.save(A + "temple_bloom.png")

    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    d.line([(4, 0), (5, 6), (3, 10), (5, 15)], fill=(88, 120, 73, 255), width=2)
    d.line([(11, 0), (10, 5), (13, 9), (11, 15)], fill=(73, 108, 70, 255), width=2)
    for x, y in ((5, 4), (10, 3), (4, 9), (12, 8), (6, 12), (10, 13)):
        d.rectangle([x - 1, y, x + 1, y + 1], fill=(121, 164, 98, 255))
    im.save(A + "temple_root_vine.png")

def lumen_crystal():
    im = Image.new("RGBA", (16, 16), (18, 20, 45, 190)); d = ImageDraw.Draw(im)
    d.polygon([(7, 1), (11, 5), (10, 13), (7, 15), (4, 12), (5, 5)], fill=(116, 211, 222, 255), outline=(222, 250, 255, 255))
    d.polygon([(7, 2), (9, 6), (8, 12), (6, 11)], fill=(222, 250, 255, 255))
    d.point((12, 3), fill=(191, 112, 233, 255)); d.point((2, 13), fill=(241, 197, 124, 255))
    im.save(A + "temple_lumen_crystal.png")

def trap_texture(name, accent, spent=False):
    im = Image.new("RGB", (16, 16), (45, 42, 57) if spent else (68, 57, 76))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 15, 15], outline=(28, 27, 38))
    d.rectangle([2, 2, 13, 13], outline=(96, 83, 105) if spent else accent)
    d.polygon([(8, 3), (12, 8), (8, 13), (4, 8)], outline=(82, 74, 96) if spent else accent)
    if name == "spike":
        d.polygon([(8, 5), (10, 10), (8, 9), (6, 10)], fill=(112, 107, 137) if spent else (223, 227, 239), outline=accent)
    elif name == "ember":
        d.polygon([(8, 4), (10, 8), (9, 7), (9, 11), (7, 10), (6, 7)], fill=(111, 73, 55) if spent else (255, 147, 61), outline=accent)
    else:
        d.line([(4, 5), (7, 8), (5, 11), (9, 13)], fill=(108, 93, 119) if spent else accent, width=1)
        d.line([(11, 3), (9, 7), (12, 9), (10, 12)], fill=(108, 93, 119) if spent else accent, width=1)
    grain(d, (45, 42, 57) if spent else (68, 57, 76), 4)
    im.save(A + f"temple_trap_{name}_{'spent' if spent else 'armed'}.png")

def relic():
    item_dir = str(MOD / "src/main/resources/assets/riftworld/textures/item")
    os.makedirs(item_dir, exist_ok=True)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    d.ellipse([2, 2, 13, 13], fill=(205, 163, 79, 255), outline=(255, 227, 145, 255))
    d.ellipse([4, 4, 11, 11], fill=(72, 55, 116, 255), outline=(249, 212, 121, 255))
    d.polygon([(8, 4), (10, 8), (8, 12), (6, 8)], fill=(141, 225, 231, 255), outline=(238, 255, 255, 255))
    d.point((8, 8), fill=(255, 248, 196, 255))
    im.save(item_dir + "/temple_relic.png")

bricks(); gold(); pillar(); floor(); understone(); maze_floor(); garden_soil(); moss(); plants(); lumen_crystal()
for trap_name, accent in (("spike", (213, 206, 232)), ("ember", (235, 117, 51)), ("drain", (155, 101, 197))):
    trap_texture(trap_name, accent, False); trap_texture(trap_name, accent, True)
for s in range(4):
    mosaic(False, s); mosaic(True, s)
candle(); bridge(); relic()
print("TEMPLE_TEX_OK")
