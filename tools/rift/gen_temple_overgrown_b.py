"""Part 10 'Храм полностью': the SECOND overworld ruin variant - a different story of collapse.\nThis one fell from the EAST side: his flank is burst open, the pillar drums rolled along the east aisle,\na wave of ivy took HALF the hall, moss drinks deeper. Same bones, same scale, same demon gate - another age.\n-> data/riftworld/structures/ruins/temple_overgrown_b.nbt   (27 x 23, local y 0 = ground podium, front at +z)\n"""
import os, random

HERE = os.path.dirname(os.path.abspath(__file__))
src = open(os.path.join(HERE, "gen_castle.py"), encoding="utf-8").read()
exec(src[:src.index("# ================================================================== foundation / floors")])
rooms = open(os.path.join(HERE, "castle_rooms.py"), encoding="utf-8").read()
exec(rooms[:rooms.index("def tower_door")])

OUT = MOD + "/src/main/resources/data/riftworld/structures/ruins"
os.makedirs(OUT, exist_ok=True)
W, D, H = 27, 23, 12

TB = S("temple_bricks")
CRK = S("temple_bricks_cracked")
IVY = S("temple_bricks_ivy")
RUNE = S("temple_gold_rune")
MOSS = S("elisar_moss")
WEB = S("castle_cobweb")
PIL = lambda ax="y": S("temple_pillar", axis=ax)
D_FR = S("demon_frame")
D_GT = S("demon_gate")
CANDLE = S("ritual_candle", lit=False)
CANDLE_LIT = S("ritual_candle", lit=True)
TSL = lambda t="bottom": S("temple_bricks_slab", type=t)
TBS = lambda f: S("temple_bricks_stairs", facing=f)

rnd = random.Random(0xBEE77)


def weathered(x, y, z):
    """Heavier decay than ruin A: ~44% cracked, ~32% ivy (this ruin is ancient-er)."""
    h = (x * 97542937 + y * 632101 + z * 86117) & 0xFFFF
    m = h % 25
    if m < 11: return CRK
    if m < 19: return IVY
    return TB


# ------------------------------------------------------------------ podium
fill(0, 0, 0, W - 1, 0, D - 1, TB)
for x in range(W):
    for z in range(D):
        h = (x * 982451653 + z * 79109) & 0xFFFF
        if h % 11 < 3:
            put(x, 0, z, MOSS if h % 2 else CRK)
        if (x in (0, W - 1)) or (z in (0, D - 1)):
            if h % 6 < 2:
                put(x, 0, z, IVY)
# the crumbling apron, heaved out of true
for x in range(9, 18):
    put(x, 0, D - 1, TBS("south"))
put(9, 0, D - 1, CRK); put(17, 0, D - 1, CRK)

# ------------------------------------------------------------------ hall shell: jagged tops, EAST side burst open
for x in range(2, W - 2):
    for z in (2, D - 2):
        h = ((x * 79109 + z * 61037) >> 3) & 0xF
        if h < 3: continue
        hh = 2 + h % 5
        for y in range(1, hh + 1):
            put(x, y, z, weathered(x, y, z))
for z in range(2, D - 2):
    for x in (2, W - 3):
        h = ((z * 79109 + x * 61037) >> 3) & 0xF
        if x == W - 3 and 8 < z < 17:                     # THE BREACH: east flank torn wide
            continue
        if h < 4: continue
        hh = 2 + h % 6
        for y in range(1, hh + 1):
            put(x, y, z, weathered(x, y, z))
# breach rim: jagged teeth left of the wound
for y in range(1, 3):
    put(W - 3, y, 8, weathered(W - 3, y, 8)); put(W - 3, y, 17, weathered(W - 3, y, 17))
put(W - 3, 1, 9, IVY); put(W - 3, 1, 16, IVY)

# the door: breach it clean, 3 wide (slightly off centre - the ruin has twisted)
for x in range(11, 14):
    for y in range(1, 5):
        put(x, y, D - 2, AIR)
put(11, 5, D - 2, CRK); put(12, 5, D - 2, TB); put(13, 5, D - 2, CRK)

# portico stubs: here BOTH columns stand, but the east one has lost its cap
for x, tall in ((8, 5), (16, 4)):
    for y in range(1, tall + 1):
        put(x, y, D - 2, IVY if x == 16 and y > 1 else TB)
    if tall == 5:
        put(x, tall + 1, D - 2, TSL("top"))

# rear apse wall: strongest (the arch still holds)
for x in range(2, W - 2):
    for y in range(1, 8):
        put(x, y, 2, weathered(x, y, 2))

# ------------------------------------------------------------------ floor dressing: zigzag rune march to the gate
for i, z in enumerate(range(6, D - 3)):
    put(13 if z % 3 else (12 if (z // 3) % 2 else 14), 0, z, RUNE if z % 2 else TB)
for x in range(11, 16):
    put(x, 0, 6, RUNE)
for (x, z) in [(11, 8), (15, 7), (12, 7), (14, 8), (11, 9), (15, 9), (13, 11)]:
    put(x, 0, z, RUNE)

# rubble: the east wound spilled its guts inside
for (x, z, m) in ((21, 9, CRK), (19, 11, TSL()), (18, 13, CRK), (17, 8, IVY), (22, 12, CRK), (20, 14, MOSS),
                  (6, 10, MOSS), (7, 13, CRK), (24, 11, CRK)):
    put(x, 0, z, m)
    if rnd.random() < 0.7 and 16 <= x <= 24:
        put(x, 1, z, CRK)
put(3, 1, 2, WEB); put(W - 4, 1, 3, WEB); put(2, 5, D - 5, WEB); put(W - 5, 2, D - 4, WEB); put(20, 1, 8, WEB)

# fallen drums along the EAST aisle (they rolled out of the burst flank)
for z in range(7, 12):
    put(21, 1, z, PIL("z"))
put(20, 1, 13, PIL("x")); put(19, 1, 13, PIL("x")); put(22, 1, 6, PIL("x"))

# moss seeds
for x in range(W):
    for z in range(D):
        if B.get((x, 0, z)) in (TB,) and ((x * 131071 + z * 524287) & 0xFF) < 26:
            put(x, 0, z, MOSS)

# ------------------------------------------------------------------ the DEMON GATE arch (same sport as ruin A)
for y in range(1, 7):
    put(11, y, 4, D_FR); put(15, y, 4, D_FR)
for x in range(11, 16):
    put(x, 6, 4, D_FR)
for x in range(12, 15):
    for y in range(2, 5):
        put(x, y, 4, D_GT)
for x in (11, 15):
    put(x, 1, 5, D_FR)
put(12, 1, 5, D_FR); put(14, 1, 5, D_FR)
# candle guard mirrored: here the WEST one still burns
put(10, 1, 6, CANDLE_LIT)
put(16, 1, 6, CANDLE)
# scorched ring, thickened with ash
for (x, z) in [(12, 5), (13, 5), (14, 5), (12, 6), (14, 6), (13, 4), (12, 4), (14, 4)]:
    if B.get((x, 0, z)) not in (RUNE,):
        continue
    put(x, 0, z, D_FR)

# roof stubs - fewer have survived, and none over the breach
for x in range(3, W - 3):
    if rnd.random() < 0.40:
        put(x, 6, 2, TSL())
    if rnd.random() < 0.25:
        put(x, 5, D - 2, TSL())

# Part 11: the dead faithful are carved here too - two reliefs keep their eyes on the gate
SPIR = S("temple_spirit")
put(8, 2, 2, SPIR); put(18, 2, 2, SPIR)
put(8, 2, D - 2, SPIR)

n, pal = write_template(OUT + "/temple_overgrown_b.nbt", (W, H, D), dict(B), {k: v for k, v in NBT.items() if k in B})
print("temple_overgrown_b ->", n, "blocks,", pal, "palette, skips", len(SKIPPED))
