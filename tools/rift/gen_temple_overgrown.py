"""Part 7 'Храм полностью': the OVERWORLD twin of the Great Temple - a beautiful overgrown ruin with the
DEMONIC TELEPORT roaring in its apse (the way into the Rift; the Great Temple's light bridge is the way back).
-> data/riftworld/structures/ruins/temple_overgrown.nbt   (27 x 23, local y 0 = ground podium, front at +z)

Look: same ivory & gold bones, but every third brick is cracked or ivy-clad, the roof is gone to stubs,
moss drinks the floor, two surviving candle trees flank a demonstone arch filled with crimson gate panes.
"""
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

rnd = random.Random(0xDEAD0)


def weathered(x, y, z):
    """Ivory brick wall decay mix: ~30% cracked, ~25% ivy, occasional moss footing."""
    h = (x * 73428767 + y * 912931 + z * 49993) & 0xFFFF
    m = h % 25
    if m < 7: return CRK
    if m < 13: return IVY
    return TB


# ------------------------------------------------------------------ podium (already half-sunken: moss fringe)
fill(0, 0, 0, W - 1, 0, D - 1, TB)
for x in range(W):
    for z in range(D):
        h = (x * 2654435761 + z * 40503) & 0xFFFF
        if h % 13 < 3:
            put(x, 0, z, MOSS if h % 2 else CRK)
        if (x in (0, W - 1)) or (z in (0, D - 1)):
            if h % 7 < 2:
                put(x, 0, z, IVY)
# crumbling entry apron
for x in range(10, 17):
    put(x, 0, D - 1, TBS("south"))

# ------------------------------------------------------------------ hall shell: jagged-top ruins, door at +z
for x in range(2, W - 2):
    for z in (2, D - 2):
        h = ((x * 40503 + z * 99991) >> 3) & 0xF
        if h < 3: continue                              # solid breaches
        hh = 2 + h % 5
        for y in range(1, hh + 1):
            put(x, y, z, weathered(x, y, z))
for z in range(2, D - 2):
    for x in (2, W - 3):
        h = ((z * 40503 + x * 99991) >> 3) & 0xF
        if h < 4: continue
        hh = 2 + h % 6
        for y in range(1, hh + 1):
            put(x, y, z, weathered(x, y, z))
# the door: breach it clean, 3 wide
for x in range(12, 15):
    for y in range(1, 5):
        put(x, y, D - 2, AIR)
# lintel stump above the door
put(12, 5, D - 2, TB); put(13, 5, D - 2, CRK); put(14, 5, D - 2, TB)

# portico stubs: two surviving columns flanking the door (one ivy-choked)
for x, ivy in ((9, True), (17, False)):
    for y in range(1, 7 if not ivy else 5):
        put(x, y, D - 2, IVY if ivy and y > 2 else TB)
    put(x, 7 if not ivy else 5, D - 2, TSL("top"))

# rear wall of the apse is the strongest part (it holds the arch)
for x in range(2, W - 2):
    for y in range(1, 8):
        put(x, y, 2, weathered(x, y, z=2))

# ------------------------------------------------------------------ floor dressing: rune path to the gate
for z in range(6, D - 3):
    put(13, 0, z, RUNE if z % 2 else TB)
for x in range(11, 16):
    put(x, 0, 6, RUNE)
for (x, z) in [(11, 7), (15, 7), (11, 8), (15, 8), (12, 9), (14, 9), (13, 10)]:
    put(x, 0, z, RUNE)

# rubble and silence inside
for (x, z, m) in ((5, 9, CRK), (8, 12, TSL()), (20, 10, CRK), (18, 6, MOSS), (6, 15, MOSS), (21, 14, CRK), (10, 17, MOSS)):
    put(x, 0, z, m)
    if rnd.random() < 0.5:
        put(x, 1, z, m if m is CRK else CRK)
put(4, 1, 2, WEB); put(W - 5, 1, 2, WEB); put(2, 4, D - 6, WEB); put(W - 3, 3, 5, WEB)

# fallen column drums along the west aisle
for z in range(9, 14):
    put(5, 1, z, PIL("z"))
put(6, 1, 7, PIL("x")); put(7, 1, 7, PIL("x"))

# ------------------------------------------------------------------ the DEMON GATE arch in the apse
# demonstone arch 5 wide x 6 high at z = 4, portal panes 3x3 inside
for y in range(1, 7):
    put(11, y, 4, D_FR); put(15, y, 4, D_FR)
for x in range(11, 16):
    put(x, 6, 4, D_FR)
for x in range(12, 15):
    for y in range(2, 5):
        put(x, y, 4, D_GT)
# altar teeth at the gate foot
for x in (11, 15):
    put(x, 1, 5, D_FR)
put(12, 1, 5, D_FR); put(14, 1, 5, D_FR)
# the gate's candle guard: one dead, one still burning
put(10, 1, 6, CANDLE)
put(16, 1, 6, CANDLE_LIT)
# scorched hearth ring before the gate
for (x, z) in [(12, 5), (13, 5), (14, 5), (12, 6), (14, 6), (13, 4)]:
    if B.get((x, 0, z)) not in (RUNE,):
        continue
    put(x, 0, z, D_FR)

# surviving roof stubs straddling the walls
for x in range(3, W - 3):
    if rnd.random() < 0.55:
        put(x, 6, 2, TSL())
    if rnd.random() < 0.35:
        put(x, 5, D - 2, TSL())

# Part 11: the dead faithful are carved here too - two reliefs keep their eyes on the gate
SPIR = S("temple_spirit")
put(8, 2, 2, SPIR); put(18, 2, 2, SPIR)
put(9, 2, D - 2, SPIR)

n, pal = write_template(OUT + "/temple_overgrown.nbt", (W, H, D), dict(B), {k: v for k, v in NBT.items() if k in B})
print("temple_overgrown ->", n, "blocks,", pal, "palette, skips", len(SKIPPED))
