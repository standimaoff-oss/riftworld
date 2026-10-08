"""Rev 4 item 3: «Великий Храм Перехода» replaces the laboratory on the main island (city plot 63 x 70, ground y = 30).
-> data/riftworld/structures/city/temple_great.nbt

A huge ivory & gold temple in vanilla-block style: twin front towers with spires, deep porch with a pediment, tall nave
with stained-glass lancets, colonnades, crossing lantern tower, raised apse with the altar, two side chapels (east
reliquary + west quiet), the hellish ritual hall of the Rift (dark rift stone, the red hexagram inlaid into the floor
with glowing veins, the altar of the Rift and 8 unlit candles of the Rift) and a crypt below with the reliquary of the
first wound of the Rift. The laboratory and its launch core are gone: the rift is the only way out of the world.

Template frame: x -> east, z -> south, front porch at high z (face = south), ground floor at y = 30.
"""
import json, os, re, sys

HERE = os.path.dirname(os.path.abspath(__file__))
src = open(os.path.join(HERE, "gen_castle.py"), encoding="utf-8").read()
exec(src[:src.index("# ================================================================== foundation / floors")])
rooms = open(os.path.join(HERE, "castle_rooms.py"), encoding="utf-8").read()
exec(rooms[:rooms.index("def tower_door")])

OUT = MOD + "/src/main/resources/data/riftworld/structures/city"
os.makedirs(OUT, exist_ok=True)
W, D, H = 63, 70, 71
G = 30

TB = S("temple_bricks")
TG = S("temple_gold")
TF = S("temple_floor")
TP = lambda a: S("temple_pillar", axis=a)
TBS = lambda f, **kw: S("temple_bricks_stairs", facing=f, **kw)
TSL = lambda t="bottom": S("temple_bricks_slab", type=t)
# ---- the hellish masonry of the ritual hall (Rev 5)
RSB = S("rift_stone_bricks")
RST = S("rift_stone")
RSC = S("rift_stone_bricks_cracked")
RVEIN = S("rift_vein")
RASH = S("rift_ash")
RPIL = S("rift_pillar", axis="y")
RSLAB = S("rift_stone_slab", type="bottom")
RCANDLE = S("rift_candle", lit=False)
RALTAR = S("rift_altar", stage=0, open=False)


def line_cells(x0, z0, x1, z1):
    """Bresenham cells of a line of the sign (13x13 grid, the altar in the middle cell 6,6)."""
    cells, dx, dz = [], abs(x1 - x0), -abs(z1 - z0)
    sx, sz = (1 if x0 < x1 else -1), (1 if z0 < z1 else -1)
    err = dx + dz
    while True:
        cells.append((x0, z0))
        if x0 == x1 and z0 == z1:
            return cells
        e2 = 2 * err
        if e2 >= dz:
            err += dz; x0 += sx
        if e2 <= dx:
            err += dx; z0 += sz


SIGN = set()                                                     # the demon sign: two triangles + the outer ring
for (a, b) in (((6, 0), (0, 12)), ((6, 0), (12, 12)), ((0, 12), (12, 12)),
               ((6, 12), (0, 0)), ((6, 12), (12, 0)), ((0, 0), (12, 0))):
    SIGN.update(line_cells(a[0], a[1], b[0], b[1]))
for i in range(13):
    for j in range(13):
        if abs(((i - 6) ** 2 + (j - 6) ** 2) ** 0.5 - 6.0) < 0.5:
            SIGN.add((i, j))

# ------------------------------------------------------------------ platform & apron
fill(1, G - 1, 1, 61, G, 68, TB)                                  # plinth 2 thick
for x in range(6, 57):                                            # grand south apron step
    put(x, G, 69, TBS("south"))
band(1, 1, 61, 68, G, TG)                                         # gilded plinth lip

# ------------------------------------------------------------------ nave shell (x 13..49, z 9..57, walls to G+16)
walls(13, 9, 49, 57, G + 1, G + 16, t=2, mat=lambda x, y, z: TB)
fill(14, G, 10, 48, G, 56, TF)                                    # inner floor
for z in range(12, 56, 7):                                        # buttresses with slab caps
    fill(11, G + 1, z, 12, G + 6, z, TB)
    fill(50, G + 1, z, 51, G + 6, z, TB)
    put(11, G + 7, z, TSL("top")); put(12, G + 7, z, TSL("top"))
    put(50, G + 7, z, TSL("top")); put(51, G + 7, z, TSL("top"))
band(13, 9, 49, 57, G + 16, TSL())                                # eaves course

# colonnade: two pillar rows with gold capitals
for z in range(14, 53, 6):
    for x in (20, 42):
        fill(x, G + 1, z, x, G + 11, z, TP("y"))
        put(x, G + 12, z, TG)

# lancet windows along the nave (two rows: arcade + clerestory)
for z in range(14, 54, 5):
    for outer, inner in ((13, [14]), (49, [48])):
        lancet("z", z, G + 9, 3, outer, inner)
        lancet("z", z, G + 3, 3, outer, inner)

# ------------------------------------------------------------------ apse (raised, z 4..9) + altar
fill(15, G + 1, 4, 47, G + 1, 9, TF)
for x in range(16, 47):
    put(x, G + 1, 10, TBS("north"))
for x in (24, 31, 38):                                            # tall lancets over the altar
    lancet("x", x, G + 6, 6, 10, [11])
# rose window in the nave back wall
for (x, y) in ((29, G + 12), (31, G + 11), (33, G + 12), (30, G + 14), (32, G + 14), (31, G + 15)):
    put(x, y, 10, GLASS)
model("chapel_altar", 30, G + 2, 5, "south", powered=True)
model("wall_sconce", 22, G + 3, 5, "west", powered=True)
model("wall_sconce", 40, G + 3, 5, "east", powered=True)

# ------------------------------------------------------------------ the hellish ritual hall of the Rift
SX0, SZ0 = 25, 28                                                # the 13x13 sign around the altar (31, G, 34)
fill(23, G, 26, 39, G, 42, RSB)                                  # 17x17 dark plate of the hall
fill(24, G, 27, 38, G, 41, RST)                                  # plain rift stone inside it
for (i, j) in SIGN:                                              # the red hexagram, inlaid with glowing veins
    put(SX0 + i, G, SZ0 + j, RVEIN)
for (vx, vz) in ((24, 30), (38, 30), (24, 38), (38, 38), (31, 27), (31, 41)):
    put(vx, G, vz, RVEIN)                                        # veins bleeding out of the sign
for (ax, az) in ((30, 27), (32, 41)):
    put(ax, G, az, RSC)                                          # cracked slab in the plate
put(31, G, 34, RALTAR)                                           # the altar of the Rift rises out of the floor layer
for (cx, cz) in ((27, 30), (35, 30), (27, 38), (35, 38),         # the 8 unlit candles of the Rift
                 (31, 29), (31, 39), (26, 34), (36, 34)):
    put(cx, G + 1, cz, RCANDLE)
for (px, pz) in ((24, 27), (38, 27), (24, 41), (38, 41)):
    fill(px, G + 1, pz, px, G + 3, pz, RPIL)                     # pillars of the rift at the corners of the plate
for x in range(23, 40):                                          # low dark enclosure with four gates
    if not 29 <= x <= 33:
        put(x, G + 1, 26, RSB); put(x, G + 1, 42, RSB)
        put(x, G + 2, 26, RSLAB); put(x, G + 2, 42, RSLAB)
for z in range(27, 42):
    if not 31 <= z <= 35:
        put(23, G + 1, z, RSB); put(39, G + 1, z, RSB)
        put(23, G + 2, z, RSLAB); put(39, G + 2, z, RSLAB)
for (ax, az) in ((23, 26), (39, 26), (23, 42), (39, 42), (25, 33), (37, 35)):
    put(ax, G, az, RASH)                                         # ash of the burnt world
for z in range(12, 56, 7):                                       # ash at the feet of the colonnade
    put(19, G, z, RASH); put(43, G, z, RASH)

# crimson runner from the doors to the gate of the hall
fill(30, G, 43, 32, G, 55, S("castle_carpet"))

# ------------------------------------------------------------------ side chapels (east reliquary, west quiet)
walls(50, 23, 61, 41, G + 1, G + 10, t=1, mat=lambda x, y, z: TB)  # east
fill(51, G, 24, 60, G, 40, TF)
for z in range(26, 40, 4):
    lancet("z", z, G + 3, 3, 61, [])
walls(1, 23, 12, 41, G + 1, G + 10, t=1, mat=lambda x, y, z: TB)   # west
fill(2, G, 24, 11, G, 40, TF)
for z in range(26, 40, 4):
    lancet("z", z, G + 3, 3, 1, [])
opening("z", 0, 31, 33, G + 1, G + 4, [48, 49, 50])                # nave -> east chapel
opening("z", 0, 31, 33, G + 1, G + 4, [12, 13, 14])                # nave -> west chapel
model("chained_lectern", 55, G + 1, 30, "west")
model("chained_books", 55, G + 1, 30, "west", overlay=True)
model("gold_hoard", 53, G + 1, 36, "south", powered=False)
model("candle_stand", 58, G + 1, 26, "east", powered=True)
model("candle_stand", 58, G + 1, 38, "east", powered=True)
model("castle_bench", 4, G + 1, 29, "east")
model("castle_bench", 4, G + 1, 33, "east")
model("candle_stand", 3, G + 1, 25, "east", powered=True)
model("tapestry_horn", 2, G + 3, 37, "east")

# ------------------------------------------------------------------ porch & pediment (south face)
for x in (26, 36):
    for z in (60, 66):
        fill(x, G + 1, z, x, G + 8, z, TP("y"))
        put(x, G + 9, z, TG)
opening("x", 0, 29, 33, G + 1, G + 7, [56, 57], pointed=True)
fill(24, G + 9, 58, 38, G + 9, 68, TSL())                        # porch ceiling
for k in range(7):                                                # pediment triangle over the door
    fill(22 + k, G + 10 + k, 56, 40 - k, G + 10 + k, 58, TB)
for k in range(19):                                               # gold cornice + apex
    put(22 + k, G + 10, 55, TG)
put(31, G + 17, 57, TG)
model("wall_sconce", 27, G + 4, 56, "west", powered=True)
model("wall_sconce", 35, G + 4, 56, "east", powered=True)

# ------------------------------------------------------------------ twin front towers (x 2..10 / 52..60, z 56..68)
for xa, xb in ((2, 10), (52, 60)):
    walls(xa, 56, xb, 68, G + 1, G + 28, t=1, mat=lambda x, y, z: TB)
    fill(xa + 1, G, 57, xb - 1, G, 67, TF)
    for x in range(xa + 2, xb - 1, 3):                            # bell lancets
        lancet("x", x, G + 22, 5, 68, [])
        lancet("x", x, G + 22, 5, 56, [])
    for z in range(60, 66, 3):
        lancet("z", z, G + 22, 5, xa, [xa + 1])
        lancet("z", z, G + 22, 5, xb, [xb - 1])
    fill(xa, G + 29, 56, xb, G + 29, 68, TB)                      # spire base
    for k in range(1, 5):                                         # pyramid spire + gold tip
        band(xa + k, 56 + k, xb - k, 68 - k, G + 29 + k, TB)
    xm = (xa + xb) // 2
    fill(xm, G + 34, 62, xm, G + 37, 62, TB)
    put(xm, G + 38, 62, TG)
model("high_chair", 6, G + 1, 60, "east")
model("high_chair", 56, G + 1, 60, "west")

# ------------------------------------------------------------------ crossing lantern tower (x 23..39, z 29..43)
walls(23, 29, 39, 43, G + 17, G + 26, t=1, mat=lambda x, y, z: TB)
for x in range(26, 37, 3):
    lancet("x", x, G + 19, 5, 29, [30])
    lancet("x", x, G + 19, 5, 43, [42])
for z in range(32, 41, 3):
    lancet("z", z, G + 19, 5, 39, [38])
    lancet("z", z, G + 19, 5, 23, [24])
for k in range(5):                                                # pyramid cap + gold spire
    fill(23 + k, G + 27 + k, 29 + k, 39 - k, G + 27 + k, 43 - k, TB)
fill(31, G + 32, 36, 31, G + 35, 36, TB)
put(31, G + 36, 36, TG)

# ------------------------------------------------------------------ nave roof (gable over x 13..49, ridge at x 31)
for s in range(18):
    y = G + 17 + s // 2
    xi = 13 + s
    for z in range(8, 59):
        put(xi, y, z, TBS("east"))
        put(49 - s, y, z, TBS("west"))
for z in range(8, 59):                                            # ridge caps (gold every 6th)
    put(31, G + 26, z, TG if z % 6 == 2 else TSL())
    put(30, G + 26, z, TSL())
fill(50, G + 11, 23, 61, G + 11, 41, TSL())                       # chapel sheds
fill(1, G + 11, 23, 12, G + 11, 41, TSL())

# ------------------------------------------------------------------ hanging light & benches
model("castle_chandelier", 31, G + 13, 22, "south", powered=True)
model("castle_chandelier", 31, G + 13, 46, "south", powered=True)
for z in (18, 24):
    model("castle_bench", 24, G + 1, z, "east")
    model("castle_bench", 38, G + 1, z, "west")

# ------------------------------------------------------------------ crypt (interior y G-6..G-1) + launch-core chamber
fill(22, G - 6, 26, 40, G - 1, 44, AIR)                           # carve the hall
fill(22, G - 7, 26, 40, G - 7, 44, S("ashlar"))                   # crypt floor
for x in (26, 36):                                                # sarcophagi
    for z in (30, 34):
        put(x, G - 7, z, ASH); put(x, G - 6, z, TSL("top"))
model("candle_stand", 24, G - 6, 28, "east", powered=True)
model("candle_stand", 38, G - 6, 28, "west", powered=True)
# stairs down from the nave beside the mosaic (entry hole at z 44, going north)
for k in range(1, 7):
    zz = 44 - k
    put(22, G - k, zz, TBS("north")); put(23, G - k, zz, TBS("north"))
    fill(22, G - k + 1, zz, 23, G + 2, zz, AIR)
fill(22, G + 1, 44, 23, G + 2, 44, AIR)                           # entry hole in the nave floor
for p in ((22, 44), (23, 44)):
    pass
fill(21, G - 7, 43, 24, G - 7, 45, S("ashlar"))                   # landing floor at the entry bottom
# crypt reliquary of the Rift (the room that once held the launch core of the laboratory)
fill(28, G - 6, 40, 34, G - 3, 44, AIR)
fill(28, G - 7, 40, 34, G - 7, 44, RSB)                           # dark floor of the reliquary
put(31, G - 7, 42, RSC)                                           # cracked slab under the first wound
put(31, G - 6, 42, RVEIN)                                         # the glowing wound the rift left in the world
for (px, pz) in ((28, 40), (34, 40), (28, 44), (34, 44)):
    fill(px, G - 6, pz, px, G - 5, pz, RPIL)                      # black pillars around it
put(30, G - 6, 41, RCANDLE); put(32, G - 6, 41, RCANDLE)          # two candles nobody has lit yet
model("wall_sconce", 27, G - 5, 41, "east", powered=True)
model("wall_sconce", 35, G - 5, 41, "west", powered=True)

# ------------------------------------------------------------------ write
n, pal = write_template(OUT + "/temple_great.nbt", (W, H, D), B, {k: v for k, v in NBT.items() if k in B})
print("temple_great", W, H, D, "blocks", n, "palette", pal, "skips", len(SKIPPED))
for k in list(SKIPPED)[:6]:
    print("  SKIP", k)
