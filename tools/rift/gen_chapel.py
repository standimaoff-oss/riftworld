"""«Тлеющая часовня» (Smouldering Chapel) - the lightweight, naturally generated version of the castle for normal play.
-> data/riftworld/structures/ruins/chapel.nbt (+ chapel_meta.json)

A small ruined gothic chapel of the same red brick / grey ashlar / rare brass palette as the castle: nave with stained-glass
lancets (E/W side windows, a triple lancet over the altar in the south wall, a round window over the door), a broken NW bell
tower, the NE corner and part of the roof collapsed, altar on a dais with candles, pews, tapestries, a loot chest, cobwebs,
bone piles and rubble; a small burnt graveyard by the door.  Ember guards perch on the walls (spawned by the chunk generator).

Template frame: x -> east, z -> south, y up; y = 0 = floor layer on the flattened island top; door at z = 3 facing north.
Reuses the primitives of gen_castle.py (exec of its primitive section) and the furnishing helpers of castle_rooms.py.
"""
import json, os, re, sys

HERE = os.path.dirname(os.path.abspath(__file__))
src = open(os.path.join(HERE, "gen_castle.py"), encoding="utf-8").read()
exec(src[:src.index("# ================================================================== foundation / floors")])
rooms = open(os.path.join(HERE, "castle_rooms.py"), encoding="utf-8").read()
exec(rooms[:rooms.index("def tower_door")])

OUT = MOD + "/src/main/resources/data/riftworld/structures/ruins"
os.makedirs(OUT, exist_ok=True)
W, D = 19, 28

# ------------------------------------------------------------------ shell
fill(1, 0, 2, 17, 0, 24, ASH)                               # plinth slab
walls(2, 3, 16, 23, 1, 9)                                   # nave walls (t = 2)
band(2, 3, 16, 23, 1, ASH)                                  # plinth course
band(2, 3, 16, 23, 6, ASH)                                  # string course
for (x, z) in ((2, 3), (16, 3), (2, 23), (16, 23), (2, 13), (16, 13)):  # buttresses (ashlar-capped)
    dx = -1 if x == 2 else 1
    dz = -1 if z == 3 else (1 if z == 23 else 0)
    for y in range(1, 7):
        put(x + dx, y, z, brick(x + dx, y, z))
        if dz:
            put(x, y, z + dz, brick(x, y, z + dz))
    put(x + dx, 7, z, slab("ashlar"))
    if dz:
        put(x, 7, z + dz, slab("ashlar"))
# floor: grey ashlar, mosaic aisle with pillar borders, dais at the south end
fill(4, 0, 5, 14, 0, 21, ASH)
fill(8, 0, 5, 10, 0, 18, MOS)
for z in range(5, 19):
    put(7, 0, z, pillar("z")); put(11, 0, z, pillar("z"))
fill(4, 1, 19, 14, 1, 21, ASH)
for x in range(5, 14):
    put(x, 1, 19, stairs("ashlar", "south"))
put(9, 1, 20, BRASS)                                        # brass inlay before the altar

# door (pointed, framed) and a round window above it
opening("x", 0, 8, 10, 1, 4, [3, 4])
for (x, y) in ((8, 7), (9, 7), (10, 7), (8, 8), (9, 8), (10, 8), (9, 9), (9, 6)):
    put(x, y, 3, GLASS); clear(x, y, 4)
for (x, y) in ((7, 7), (7, 8), (11, 7), (11, 8), (8, 6), (10, 6), (8, 9), (10, 9)):
    put(x, y, 3, ASH)

# stained-glass lancets: 3 per side wall, triple lancet over the altar
for z in (7, 11, 16):
    lancet("z", z, 2, 4, 2, [3])
    lancet("z", z, 2, 4, 16, [15])
for x in (7, 9, 11):
    lancet("x", x, 2 if x == 9 else 3, 6 if x == 9 else 5, 23, [22])

# gables + roof (ridge along z)
ry = gable_roof_z(1, 17, 2, 24, 10)
for i in range(8):                                          # brick gable fill between the wall tops and the roof
    for x in range(2 + i + 1, 16 - i):
        for z in (3, 23):
            if (x, 10 + i, z) not in B:
                put(x, 10 + i, z, brick(x, 10 + i, z))
fill(3, 10, 3, 15, 10, 3, brick)
fill(3, 10, 23, 15, 10, 23, brick)
put(9, 12, 3, GLASS); put(9, 13, 3, GLASS)                   # slit in the front gable
put(9, 12, 23, GLASS); put(9, 13, 23, GLASS)
put(9, ry + 1, 3, WALL); put(9, ry + 2, 3, WALL)             # gable cross stub
# open timber trusses under the roof
for z in (7, 11, 15, 19):
    for x in range(4, 15):
        put(x, 10, z, beam("x"))

# NW bell tower (square, broken top), opens into the nave
walls(0, 0, 5, 5, 1, 17, t=1)
fill(0, 0, 0, 5, 0, 5, ASH)
band(0, 0, 5, 5, 1, ASH)
band(0, 0, 5, 5, 9, ASH)
for y in (12, 13, 14):                                      # belfry openings
    for a in (2, 3):
        clear(a, y, 0); clear(0, y, a); clear(a, y, 5); clear(5, y, a)
for y in range(1, 4):
    clear(4, y, 4); clear(5, y, 4); clear(4, y, 5)
fill(1, 1, 1, 4, 16, 4, None)
fill(1, 10, 1, 4, 10, 4, PL)                                 # belfry floor (broken)
for (x, z) in ((3, 3), (4, 3), (3, 4)):
    clear(x, 10, z)
put(2, 11, 2, S("chain", axis="y")) if False else None
ruin(0, 0, 5, 5, 14, 17, 3.1)

# ------------------------------------------------------------------ collapse: NE corner, part of the roof
ruin(10, 2, 18, 11, 3, 19, 7.7)
ruin(12, 12, 17, 18, 8, 19, 4.2)
for x in range(4, 15):                                      # roof hole over the middle of the nave
    for z in range(9, 14):
        for y in range(10, 19):
            if (x, y, z) in B and "roof" in B[(x, y, z)][0] and (x + z) % 5 != 0:
                del B[(x, y, z)]

# ------------------------------------------------------------------ furnishing
for (x, y, z) in list(B):                                   # nothing left floating after the collapse
    pass
carpet(9, 6, 9, 17, 1)
for zc in (8, 10, 12, 14, 16):
    if zc != 12:
        place("castle_bench", 5, 1, zc, "north")
    if zc not in (8, 10):
        place("castle_bench", 13, 1, zc, "north")
place("chapel_altar", 9, 2, 20, "north")
place("candle_stand", 6, 2, 20, "north")
place("candle_stand", 12, 2, 20, "north")
chest(9, 2, 21, "north", "chapel_ruin")
place("tapestry_horn", 5, 2, 21, "north")
place("tapestry_horn", 13, 2, 21, "north")
for (x, z, f) in ((4, 9, "east"), (4, 18, "east"), (14, 18, "west")):
    place("wall_sconce", x, 4, z, f)
place("bone_pile", 12, 1, 9, "south")
place("bone_pile", 6, 1, 6, "west")
place("gold_hoard", 4, 2, 21, "south")
web((4, 9, 5), (4, 9, 6), (14, 9, 21), (4, 9, 21), (5, 9, 5), (1, 9, 1), (4, 8, 4))
rubble(13, 6, 3.2, 1, 41)
rubble(14, 15, 2.2, 1, 42, n=10)
rubble(9, 11, 2.0, 1, 43, n=7)
rubble(17, 6, 2.5, 1, 44, n=10)

# ------------------------------------------------------------------ small burnt graveyard in front of the door (west side)
for (x, z) in ((2, 25), (4, 26), (14, 25), (16, 26), (12, 26)):
    put(x, 1, z, WALL); put(x, 2, z, slab("castle_bricks"))
fill(1, 0, 25, 17, 0, 27, None)
for (x, z) in ((5, 1), (13, 1), (15, 0)):                   # broken piers by the approach
    for y in range(1, 2 + (x % 3)):
        put(x, y, z, BRC if y > 1 else ASH)

# ------------------------------------------------------------------ guard perches (spawned by ElisarChunkGenerator)
PERCH_CAND = [(3, 10, 6), (15, 10, 17), (3, 10, 19), (9, 16, 3), (2, 15, 2), (15, 10, 21), (3, 10, 14)]
perches = []
for (x, y, z) in PERCH_CAND:
    yy = y + 6
    while yy > 1 and (x, yy - 1, z) not in B:
        yy -= 1
    if (x, yy - 1, z) in B and all((x, yy + k, z) not in B for k in range(3)):
        perches.append([x, yy, z])

# ================================================================== connections (panes / fences / walls)
SOLID_SKIP = ("stairs", "slab", "part", "candle_stand", "fence", "_wall", "stained_glass")


def solid(st):
    return (st is not None and st != AIR and not any(k in st[0] for k in SOLID_SKIP)
            and st[0].split(":")[1] not in KINDS and st[0].split(":")[1] not in ("castle_carpet", "castle_cobweb", "castle_bars"))


for (x, y, z), st in list(B.items()):
    name = st[0].split(":")[1]
    if name in ("stained_glass", "dark_fence", "castle_bricks_wall", "castle_bars"):
        con = {}
        for d, dx, dz in (("north", 0, -1), ("south", 0, 1), ("west", -1, 0), ("east", 1, 0)):
            nb = B.get((x + dx, y, z + dz))
            same = nb is not None and nb[0] == st[0]
            con[d] = same or solid(nb)
        if name == "castle_bricks_wall":
            up = not ((con["north"] and con["south"] and not con["east"] and not con["west"]) or
                      (con["east"] and con["west"] and not con["north"] and not con["south"])) or (x, y + 1, z) in B
            B[(x, y, z)] = S("castle_bricks_wall", up=up, waterlogged=False, **{k: ("low" if v else "none") for k, v in con.items()})
        else:
            B[(x, y, z)] = S(name, waterlogged=False, **con)

B = {k: v for k, v in B.items() if v != AIR and 0 <= k[0] < W and 0 <= k[2] < D and k[1] >= 0}
H = max(p[1] for p in B) + 1
n, pal = write_template(OUT + "/chapel.nbt", (W, H, D), B, {k: v for k, v in NBT.items() if k in B})
json.dump({"size": [W, H, D], "blocks": n, "perches": perches, "skipped": [str(s) for s in SKIPPED]},
          open(OUT + "/chapel_meta.json", "w"), indent=1)
print("chapel", W, H, D, "blocks", n, "palette", pal, "perches", perches, "skipped", SKIPPED)
