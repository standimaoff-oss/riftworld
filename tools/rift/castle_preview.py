"""Quick orthographic previews of castle.nbt (top / front / side / hall section) for fast iteration before the game."""
import gzip, io, struct, sys
from PIL import Image, ImageDraw
P = sys.argv[1] if len(sys.argv) > 1 else "/home/user/mods/riftworld/src/main/resources/data/riftworld/structures/castle/castle.nbt"

def rd(f, t):
    if t == 1: return struct.unpack(">b", f.read(1))[0]
    if t == 2: return struct.unpack(">h", f.read(2))[0]
    if t == 3: return struct.unpack(">i", f.read(4))[0]
    if t == 4: return struct.unpack(">q", f.read(8))[0]
    if t == 5: return struct.unpack(">f", f.read(4))[0]
    if t == 6: return struct.unpack(">d", f.read(8))[0]
    if t == 8: n = struct.unpack(">H", f.read(2))[0]; return f.read(n).decode()
    if t == 9:
        et, n = struct.unpack(">bi", f.read(5)); return [rd(f, et) for _ in range(n)]
    if t == 10:
        o = {}
        while True:
            tt = struct.unpack(">b", f.read(1))[0]
            if tt == 0: return o
            n = struct.unpack(">H", f.read(2))[0]; k = f.read(n).decode(); o[k] = rd(f, tt)
f = io.BytesIO(gzip.open(P).read()); f.read(1); f.read(struct.unpack(">H", f.read(2))[0])
root = rd(f, 10)
W, H, D = root["size"]
pal = [p["Name"].split(":")[1] for p in root["palette"]]
COL = {"castle_bricks": (150, 62, 42), "castle_bricks_cracked": (110, 45, 32), "castle_bricks_mossy": (90, 100, 55), "ashlar": (140, 138, 132),
       "ashlar_stairs": (150, 148, 142), "ashlar_slab": (150, 148, 142), "ashlar_pillar": (160, 158, 150), "dark_planks": (62, 44, 34),
       "dark_beam": (55, 40, 30), "dark_fence": (70, 50, 38), "brass_block": (200, 160, 60), "castle_mosaic": (160, 90, 50),
       "stained_glass": (60, 90, 170), "castle_roof": (92, 42, 34), "castle_roof_stairs": (100, 46, 36), "castle_roof_slab": (100, 46, 36),
       "castle_bricks_stairs": (150, 62, 42), "castle_bricks_slab": (150, 62, 42), "castle_bricks_wall": (150, 62, 42),
       "dark_planks_stairs": (62, 44, 34), "candle_stand": (255, 220, 120), "boss_throne": (180, 20, 30), "part": (120, 20, 30)}
vox = {}
for b in root["blocks"]:
    x, y, z = b["pos"]; vox[(x, y, z)] = pal[b["state"]]
S = 6
def col(n, shade=1.0):
    c = COL.get(n, (255, 0, 255)); return tuple(int(v * shade) for v in c)
# top view
top = Image.new("RGB", (W * S, D * S), (30, 30, 30)); d = ImageDraw.Draw(top)
for x in range(W):
    for z in range(D):
        for y in range(H - 1, -1, -1):
            if (x, y, z) in vox:
                d.rectangle([x * S, z * S, x * S + S - 1, z * S + S - 1], fill=col(vox[(x, y, z)], 0.5 + 0.5 * y / H)); break
# front elevation (from -z: x right, y up)
fr = Image.new("RGB", (W * S, H * S), (200, 210, 220)); d = ImageDraw.Draw(fr)
for x in range(W):
    for y in range(H):
        for z in range(D):
            if (x, y, z) in vox:
                d.rectangle([x * S, (H - 1 - y) * S, x * S + S - 1, (H - 1 - y) * S + S - 1], fill=col(vox[(x, y, z)], 1.0 - 0.5 * z / D)); break
# side elevation (from +x: z right->left)
sd = Image.new("RGB", (D * S, H * S), (200, 210, 220)); d = ImageDraw.Draw(sd)
for z in range(D):
    for y in range(H):
        for x in range(W - 1, -1, -1):
            if (x, y, z) in vox:
                d.rectangle([(D - 1 - z) * S, (H - 1 - y) * S, (D - 1 - z) * S + S - 1, (H - 1 - y) * S + S - 1], fill=col(vox[(x, y, z)], 0.5 + 0.5 * x / W)); break
# hall section: cut at z = int(arg) looking towards +z (the throne)
zc = 30
sc = Image.new("RGB", (W * S, H * S), (20, 20, 25)); d = ImageDraw.Draw(sc)
for x in range(W):
    for y in range(H):
        for z in range(zc, D):
            if (x, y, z) in vox:
                d.rectangle([x * S, (H - 1 - y) * S, x * S + S - 1, (H - 1 - y) * S + S - 1], fill=col(vox[(x, y, z)], 1.0 - 0.8 * (z - zc) / (D - zc))); break
# floor plan at y=2 and y=11
def plan(yy):
    im = Image.new("RGB", (W * S, D * S), (235, 235, 230)); dd = ImageDraw.Draw(im)
    for (x, y, z), n in vox.items():
        if y == yy:
            dd.rectangle([x * S, z * S, x * S + S - 1, z * S + S - 1], fill=col(n))
    return im
p2, p11 = plan(2), plan(11)
out = Image.new("RGB", (W * S * 2 + 10, D * S * 2 + H * S * 2 + 30), (255, 255, 255))
out.paste(top, (0, 0)); out.paste(p2, (W * S + 10, 0))
out.paste(fr, (0, D * S + 10)); out.paste(sd, (W * S + 10, D * S + 10))
out.paste(sc, (0, D * S + H * S + 20)); out.paste(p11, (W * S + 10, D * S + H * S + 20))
out.save("/tmp/castle_pv.png")
print("ok", W, H, D, len(vox))
