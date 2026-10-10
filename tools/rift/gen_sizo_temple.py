"""Биом 05 «Роща Сизо»: редкая руина бледного храма с референса (колоннада-аркада, обломки, мох).
-> data/riftworld/structures/ruins/temple_sizo_ruins.nbt   (терраса 17x13, осевая арка, упавшие блоки, мох/плющ)
"""
import os
HERE = os.path.dirname(os.path.abspath(__file__))
src = open(os.path.join(HERE, "gen_castle.py"), encoding="utf-8").read()
exec(src[:src.index("# ================================================================== foundation / floors")])
rooms = open(os.path.join(HERE, "castle_rooms.py"), encoding="utf-8").read()
exec(rooms[:rooms.index("def tower_door")])

OUT = MOD + "/src/main/resources/data/riftworld/structures/ruins"
os.makedirs(OUT, exist_ok=True)
W, D = 17, 13                                    # front (+z, local), y=0 = пол террасы

BR = S("temple_bricks")
SL = S("temple_bricks_slab")
ST = lambda f: S("temple_bricks_stairs", facing=f)
PL = lambda ax="y": S("temple_pillar", axis=ax)
GD = S("temple_gold")
MOSS = S("elisar_moss")

def rndh(x, y, z):
    return ((x * 73428767 + y * 912931 + z * 49993) & 0xFFFF) % 100

# terrace podium edge (frames the temple footprint)
for x in range(W):
    put(x, 0, 0, BR); put(x, 0, D - 1, BR)
for z in range(D):
    put(0, 0, z, BR); put(W - 1, 0, z, BR)
for x in range(2, W - 2):                        # worn slab walk inside the rim
    if rndh(x, 0, 2) > 18: put(x, 0, 2, SL)
    if rndh(x, 9, 0) > 30: put(x, 0, D - 3, SL)

# side colonnades: 5 pillars each side, some broken (2-4y) + fallen drums
for i, z in enumerate(range(2, D - 2, 2)):
    for x in (2, W - 3):
        h = 4 + (rndh(x, 1, z) < 60)
        if rndh(x, 7, z) < 22: h = 1 + (rndh(x, 3, z) // 30)   # broken stump 2-3
        for y in range(1, h + 1):
            put(x, y, z, PL())
        if h >= 4 and rndh(x, 5, z) < 55:
            put(x, h + 1, z, GD)                               # gilt capital remnant
        elif rndh(x, 11, z) < 40:
            put(x, 1, z + (1 if rndh(x, 13, z) < 50 else -1), PL("z"))   # fallen drum roll
# broken architrave spans across the tall pillars
for x in (2, W - 3):
    for z in range(2, D - 2):
        if B.get((x, 5, z)) is not None and B.get((x, 5, z + 1)) is None and rndh(x, 17, z) < 70:
            put(x, 5, z + 1, BR)

# rear sanctuary: broken arch — two stout piers + a cracked lintel with a hole
for y in range(1, 6):
    put(4, y, 2, PL()); put(W - 5, y, 2, PL())
for x in range(5, W - 5):
    if rndh(x, 21, 2) < 26: continue                             # collapsed mid-section
    put(x, 5, 2, BR)
put(7, 5, 2, AIR); put(10, 5, 2, AIR)
put(4, 6, 2, GD); put(W - 5, 6, 2, GD)                           # surviving apex gilt
# gold altar block, dusted
put(8, 1, 2, GD)

# fallen rubble + moss patches
RUBBLE = [(6, 5), (11, 4), (13, 7), (3, 9), (9, 8), (5, 10), (12, 10), (7, 7)]
for (x, z) in RUBBLE:
    m = BR if rndh(x, 3, z) < 55 else SL
    put(x, 0, z, m); 
    if rndh(x, 5, z) < 35: put(x, 1, z, m)
for x in range(W):
    for z in range(D):
        if (x, z) in B: continue
        if rndh(x, 31, z) < 30: put(x, 0, z, MOSS)
        elif rndh(x, 37, z) < 12: put(x, 0, z, SL)

# stair entry at the front, facing out (+z)
for x in range(6, 11):
    put(x, 0, D - 1, ST("south"))

B.clear() if False else None
k, pal = write_template(OUT + "/temple_sizo_ruins.nbt", (W, 8, D), B, {kk: vv for kk, vv in NBT.items() if kk in B})
print("temple_sizo_ruins ->", k, "blocks,", pal, "palette, skips", len(SKIPPED))
