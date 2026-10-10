"""Rev 4 item 3: «Великий Храм Перехода» replaces the laboratory on the main island (city plot 63 x 70, ground y = 30).
-> data/riftworld/structures/city/temple_great.nbt

A huge ivory & gold temple in vanilla-block style: twin front towers with spires, deep porch with a pediment, tall nave
with stained-glass lancets, colonnades, crossing lantern tower, raised apse with the altar, two side chapels (east
reliquary + west quiet), the ritual sanctum (5x5 mosaic + Sigil core ringed by 8 unlit ritual candles) and a crypt
below holding the salvaged launch core, so the flight mechanic survives the lab.

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
MOS_T = S("temple_mosaic", stage=0)
RUNE = S("temple_gold_rune")
CORE = S("mosaic_core", stage=0)
CANDLE = S("ritual_candle", lit=False)
TBS = lambda f, **kw: S("temple_bricks_stairs", facing=f, **kw)
TSL = lambda t="bottom": S("temple_bricks_slab", type=t)

# ------------------------------------------------------------------ platform & apron
fill(1, G - 1, 1, 61, G, 68, TB)                                  # plinth 2 thick
for x in range(6, 57):                                            # grand south apron step
    put(x, G, 69, TBS("south"))
band(1, 1, 61, 68, G, TG)                                         # gilded plinth lip

# ------------------------------------------------------------------ nave shell (x 13..49, z 9..57, walls to G+16)
walls(13, 9, 49, 57, G + 1, G + 16, t=2, mat=lambda x, y, z: TB)
fill(14, G, 10, 48, G, 56, TF)                                    # inner floor
# Part 7: gilded rune inlay borders - the central aisle rails + the sanctum frame (glow among the spread mosaic)
for z in range(12, 56):
    put(26, G, z, RUNE); put(36, G, z, RUNE)
for x in range(26, 37):
    put(x, G, 28, RUNE); put(x, G, 40, RUNE)
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
for x in range(15, 48):                                           # runic rail across the apse front
    put(x, G + 1, 9, RUNE)
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

# ------------------------------------------------------------------ ritual sanctum: 5x5 mosaic + core + 8 candles
MX0, MZ0 = 29, 32
fill(MX0, G, MZ0, MX0 + 4, G, MZ0 + 4, MOS_T)
put(MX0 + 2, G, MZ0 + 2, CORE)
for (cx, cz) in ((27, 30), (35, 30), (27, 38), (35, 38),
                 (31, 29), (31, 39), (26, 34), (36, 34)):
    put(cx, G + 1, cz, CANDLE)
for k in range(MX0 - 3, MX0 + 8, 2):                              # gold studs outlining the square
    put(k, G, MZ0 - 3, TG); put(k, G, MZ0 + 7, TG)
for k in range(MZ0 - 1, MZ0 + 7, 2):
    put(MX0 - 3, G, k, TG); put(MX0 + 7, G, k, TG)

# crimson runner from the doors to the sanctum
fill(30, G, 38, 32, G, 55, S("castle_carpet"))

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
# launch-core chamber (relic of the Rift), crypt south room z 39..44
fill(28, G - 6, 40, 34, G - 3, 44, AIR)
put(31, G - 7, 42, TG)
put(31, G - 6, 42, S("launch_core"))
model("wall_sconce", 27, G - 5, 41, "east", powered=True)
model("wall_sconce", 35, G - 5, 41, "west", powered=True)

# ----------------------------------- Part 11: the sleeping SPIRIT faces of the walls + a deeper, darker crypt
SPIR = S("temple_spirit")
for z in (17, 27, 37, 47):                                        # nave side walls, below the lower lancets
    put(14, G + 2, z, SPIR); put(48, G + 2, z, SPIR)
for z in (17, 37):                                                # between the upper lancets
    put(14, G + 8, z, SPIR); put(48, G + 8, z, SPIR)
for x in (22, 40):                                                # north wall flanking the aisle to the apse
    put(x, G + 3, 10, SPIR); put(x, G + 7, 10, SPIR)
put(61, G + 2, 28, SPIR); put(61, G + 2, 36, SPIR)                # reliquary chapel
put(1, G + 2, 30, SPIR); put(1, G + 2, 39, SPIR)                  # quiet chapel
# crypt: two more tombs (six in total), spirit faces watching from the north wall, webs in the dead corners
for x in (29, 33):
    for z in (30, 34):
        put(x, G - 7, z, ASH); put(x, G - 6, z, TSL("top"))
for x in (26, 31, 36):
    put(x, G - 5, 25, SPIR)
model("candle_stand", 24, G - 6, 41, "east", powered=True)
model("candle_stand", 38, G - 6, 41, "west", powered=True)
for (wx, wy, wz) in ((23, G - 1, 26), (39, G - 1, 26), (23, G - 1, 38), (39, G - 1, 38)):
    put(wx, wy, wz, S("castle_cobweb"))
for (cx, cz) in ((27, 35), (35, 31), (24, 32), (37, 29)):
    put(cx, G - 7, cz, TSL())


# ================================================================== REV 6 PART 2 «храм красивее»: второй дизайн-проход
# 1) Удвоенный портик: вторая колонна пара + золотая антаблюдэт поп-полоса
for x in (29, 33):
    for z in (60, 66):
        fill(x, G + 1, z, x, G + 8, z, TP("y"))
        put(x, G + 9, z, TG)
fill(24, G + 10, 59, 38, G + 10, 59, TG)                        # золотой пояс антаблемента спереди
fill(24, G + 10, 67, 38, G + 10, 67, TG)                        # ... и сзади для второй ноги колонн

# 2) Пинакли по углам обеих башен (кирпичные свечи с золотыми искрами)
for xa, xb in ((2, 10), (52, 60)):
    for xx in (xa, xb):
        for zz in (56, 68):
            fill(xx, G + 29, zz, xx, G + 33, zz, TB)
            put(xx, G + 34, zz, TG)

# 3) Ребристый купол-воротник вокруг фонаря средокрестия
for ring, rad in enumerate((9, 8, 7, 6)):
    yy = G + 31 + ring
    for xx in range(31 - rad, 32 + rad):
        for zz in range(36 - rad, 37 + rad):
            dx, dz = abs(xx - 31), abs(zz - 36)
            if max(dx, dz) == rad and (dx == rad or dz == rad) and dx + dz >= rad:
                face = "east" if dx >= dz else "south"
                put(xx, yy, zz, TBS(face))

# 4) Золотые раскреповки между витражами (vertikальные муллионы, южный и северный неф)
for z in range(11, 56, 5):
    for xx in (13, 49):
        fill(xx, G + 6, z, xx, G + 7, z, TG)

# 5) Висячие фонари под карнизом нефа (по внешним стенам, пятно тёплого света через ребра)
for z in range(14, 56, 8):
    put(13, G + 15, z, S("elisar_lantern", hanging=True))
    put(49, G + 15, z, S("elisar_lantern", hanging=True))

# 6) Трёхполосная мозаичная дорожка центрального придела
for z in range(12, 56):
    if z % 2 == 0:
        put(30, G, z, MOS_T); put(32, G, z, MOS_T)

# ------------------------------------------------------------------ write
n, pal = write_template(OUT + "/temple_great.nbt", (W, H, D), B, {k: v for k, v in NBT.items() if k in B})
print("temple_great", W, H, D, "blocks", n, "palette", pal, "skips", len(SKIPPED))
for k in list(SKIPPED)[:6]:
    print("  SKIP", k)
