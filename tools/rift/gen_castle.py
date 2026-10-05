"""Rev 3 / C-1: the castle «Чертог Тлеющего Рога» (Hall of the Smouldering Horn) -> data/riftworld/structures/castle/castle.nbt

References (checked by eye): Malbork (red brick gothic: steep tiled roofs, stepped gables, square + round towers, grey stone
bands/window frames), Malbork Grand Refectory (tall hall, grey column bases), Tantallon / Old Wardour (red ruined curtain
walls, roofless rooms, jagged broken tops).

Template frame: x -> east, z -> south, y up; y = 0 is the floor layer laid on the flattened island top.
The front (gatehouse) is at z = 0 and faces north - towards the city.

  main hall   outer x24..52 z14..50, 2 floors: ground y1..9, galleries (y10) on W / E / front, clerestory, open timber roof
              (partly collapsed), throne dais at the far end, 6 column sockets, 20 candle stands (unlit) leading to the throne
  W wing      x4..25 z10..62: armory, dining hall, kitchen / upper bedrooms          (rooms are furnished in C-2)
  E wing      x51..72 z10..62: library, chapel (double height), treasury; outer wall partly collapsed
  N range     x26..50 z51..62: storerooms / upper bedrooms
  gatehouse   x31..45 z3..13 with two round turrets
  towers      NW round (tall, cone roof), NE square keep (tallest, bartizans), SW octagon (broken top), SE square (ruined)
"""
import math, os, random, re, json, sys
sys.path.insert(0, os.path.dirname(__file__))
from nbtio import write_template

MOD = "/home/user/mods/riftworld"
OUT = MOD + "/src/main/resources/data/riftworld/structures/castle"
os.makedirs(OUT, exist_ok=True)
KINDS = {m.group(1): tuple(int(m.group(i)) for i in (2, 3, 4, 5))
         for m in re.finditer(r'[A-Z_]+\("([a-z_]+)", (-?\d+), (-?\d+), (\d+), (\d+),', open(MOD + "/src/main/java/ru/aiforge/riftworld/block/ModelKind.java").read())}
rng = random.Random(1337)


def S(name, **p):
    if ":" not in name:
        name = "riftworld:" + name
    return (name, tuple(sorted((k, str(v).lower()) for k, v in p.items())))


AIR = S("minecraft:air")
B = {}
MODEL_CELLS = set()
MODELS = []           # (master, cells) - needed to shift whole models when the hall is lengthened
CANDLES = []          # candle masters in lighting order (entrance -> throne)


def put(x, y, z, st):
    if (x, y, z) in MODEL_CELLS:
        return
    B[(x, y, z)] = st


def clear(x, y, z):
    if (x, y, z) in MODEL_CELLS:
        return
    B.pop((x, y, z), None)


def fill(x0, y0, z0, x1, y1, z1, st):
    for x in range(min(x0, x1), max(x0, x1) + 1):
        for y in range(min(y0, y1), max(y0, y1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                if st is None:
                    clear(x, y, z)
                else:
                    put(x, y, z, st(x, y, z) if callable(st) else st)


def h(x, y, z, s=0):
    v = (int(x) * 73856093 ^ int(y) * 19349663 ^ int(z) * 83492791 ^ int(s * 7) * 2654435761) & 0xffffffff
    v ^= v >> 13; v = (v * 0x5bd1e995) & 0xffffffff; v ^= v >> 15
    return v / 4294967296.0


# ------------------------------------------------------------------ palette
BR = S("castle_bricks"); BRC = S("castle_bricks_cracked"); BRM = S("castle_bricks_mossy")
ASH = S("ashlar"); BRASS = S("brass_block"); MOS = S("castle_mosaic"); PL = S("dark_planks"); ROOF = S("castle_roof")


def brick(x, y, z):
    k = h(x, y, z, 1)
    if y <= 2 and k < 0.16:
        return BRM
    if k < 0.10:
        return BRC
    if k < 0.13:
        return BRM
    return BR


def pillar(axis="y"):
    return S("ashlar_pillar", axis=axis)


def beam(axis):
    return S("dark_beam", axis=axis)


def stairs(mat, facing, half="bottom", shape="straight"):
    return S(mat + "_stairs", facing=facing, half=half, shape=shape, waterlogged=False)


def slab(mat, t="bottom"):
    return S(mat + "_slab", type=t, waterlogged=False)


GLASS = S("stained_glass")             # connections fixed at the end
FENCE = S("dark_fence")
WALL = S("castle_bricks_wall")


def rot(x, y, z, f):
    return {"south": (x, y, z), "north": (-x, y, -z), "west": (-z, y, x), "east": (z, y, -x)}[f]


def model(kind, x, y, z, f, powered=False):
    x0, x1, back, hh = KINDS[kind]
    cs = [(x + a, y + b, z + c) for (a, b, c) in (rot(i, j, -k, f) for i in range(x0, x1 + 1) for k in range(back + 1) for j in range(hh))]
    for c in cs:
        assert c not in MODEL_CELLS, (kind, c)
        B[c] = S("part", open=False)
    B[(x, y, z)] = S(kind, facing=f, powered=powered, broken=False)
    MODEL_CELLS.update(cs)
    MODELS.append(((x, y, z), cs))


# ------------------------------------------------------------------ primitives
def walls(x0, z0, x1, z1, y0, y1, t=2, mat=brick):
    """Hollow rectangle of thickness t (outer bounds)."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x < x0 + t or x > x1 - t or z < z0 + t or z > z1 - t:
                for y in range(y0, y1 + 1):
                    put(x, y, z, mat(x, y, z) if callable(mat) else mat)


def band(x0, z0, x1, z1, y, mat=ASH):
    """Stone band (plinth / string course) on the outer face of a rectangle."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                put(x, y, z, mat)


def opening(axis, c, a0, a1, y0, y1, depth, pointed=True, frame=True):
    """Arched opening through a wall.  axis 'x': wall plane is z in depth (list), spans x a0..a1; axis 'z': plane x in depth."""
    w = a1 - a0 + 1
    for a in range(a0, a1 + 1):
        top = y1
        if pointed and w >= 3:
            edge = min(a - a0, a1 - a)
            top = y1 - max(0, (w // 2) - edge - 1) if w > 3 else (y1 if edge >= 1 else y1 - 1)
        for d in depth:
            for y in range(y0, top + 1):
                (clear(a, y, d) if axis == "x" else clear(d, y, a))
            if frame:
                (put(a, top + 1, d, ASH) if axis == "x" else put(d, top + 1, a, ASH))
    if frame:
        for d in depth:
            for y in range(y0, y1 + 1):
                if axis == "x":
                    put(a0 - 1, y, d, ASH if y % 2 == 0 else brick(a0 - 1, y, d)); put(a1 + 1, y, d, ASH if y % 2 == 0 else brick(a1 + 1, y, d))
                else:
                    put(d, y, a0 - 1, ASH if y % 2 == 0 else brick(d, y, a0 - 1)); put(d, y, a1 + 1, ASH if y % 2 == 0 else brick(d, y, a1 + 1))


def lancet(axis, a, y0, hgt, outer, inner, w=1, sill=True):
    """Gothic lancet window: glass in the outer layer, deep reveal inside, ashlar sill + pointed head."""
    xs = range(a, a + w)
    for k in xs:
        for y in range(y0, y0 + hgt):
            if axis == "x":
                put(k, y, outer, GLASS)
                for d in inner: clear(k, y, d)
            else:
                put(outer, y, k, GLASS)
                for d in inner: clear(d, y, k)
    if axis == "x":
        for d in [outer] + list(inner):
            if sill:
                for k in xs: put(k, y0 - 1, d, ASH)
            for k in xs: put(k, y0 + hgt, d, ASH)
        if w == 1:
            put(a, y0 + hgt, outer, slab("ashlar", "top"))
    else:
        for d in [outer] + list(inner):
            if sill:
                for k in xs: put(d, y0 - 1, k, ASH)
            for k in xs: put(d, y0 + hgt, k, ASH)
        if w == 1:
            put(outer, y0 + hgt, a, slab("ashlar", "top"))


def crenel(x0, z0, x1, z1, y, t=2):
    """Battlement on top of a rectangle of walls: walkway at y, merlons at y+1 (alternating), ashlar coping."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                put(x, y, z, ASH)
                if (x + z) % 3 != 0:
                    put(x, y + 1, z, brick(x, y + 1, z))
                    put(x, y + 2, z, slab("castle_bricks"))


def gable_roof_z(x0, x1, z0, z1, y0, mat="castle_roof", gable=True):
    """Roof with the ridge along z over x0..x1 (incl. overhang), eaves at y0. Returns ridge y."""
    w = x1 - x0 + 1
    for i in range(w // 2):
        y = y0 + i
        for z in range(z0, z1 + 1):
            put(x0 + i, y, z, stairs(mat, "east"))
            put(x1 - i, y, z, stairs(mat, "west"))
        if gable:
            for xx in range(x0 + i + 1, x1 - i):
                for z in (z0, z1):
                    put(xx, y, z, brick(xx, y, z))
    ry = y0 + w // 2
    if w % 2 == 1:
        for z in range(z0, z1 + 1):
            put(x0 + w // 2, ry, z, slab(mat))
            put(x0 + w // 2, ry - 1, z, S(mat))
    return ry


def shed_roof_x(xlow, xhigh, z0, z1, y0, mat="castle_roof"):
    """Lean-to roof rising from xlow (eave, y0) towards xhigh (one block per two columns, slab/stair steps)."""
    step = 1 if xhigh > xlow else -1
    n = abs(xhigh - xlow) + 1
    face = "east" if step == 1 else "west"
    for k in range(n):
        x = xlow + k * step
        y = y0 + k // 2
        for z in range(z0, z1 + 1):
            put(x, y, z, slab(mat) if k % 2 == 0 else slab(mat, "top"))
            if k % 2 == 1:
                pass
    return y0 + (n - 1) // 2


def cylinder(cx, cz, r, y0, y1, t=2, octagon=False, mat=brick):
    for x in range(int(cx - r - 1), int(cx + r + 2)):
        for z in range(int(cz - r - 1), int(cz + r + 2)):
            dx, dz = x - cx, z - cz
            d = max(abs(dx), abs(dz), (abs(dx) + abs(dz)) * 0.7071) if octagon else math.hypot(dx, dz)
            if d <= r + 0.3:
                inner = d <= r - t + 0.3
                for y in range(y0, y1 + 1):
                    if inner:
                        clear(x, y, z)
                    else:
                        put(x, y, z, mat(x, y, z) if callable(mat) else mat)


def disc(cx, cz, r, y, st, octagon=False):
    for x in range(int(cx - r - 1), int(cx + r + 2)):
        for z in range(int(cz - r - 1), int(cz + r + 2)):
            dx, dz = x - cx, z - cz
            d = max(abs(dx), abs(dz), (abs(dx) + abs(dz)) * 0.7071) if octagon else math.hypot(dx, dz)
            if d <= r + 0.3:
                put(x, y, z, st)


def cone(cx, cz, r, y0, mat=ROOF):
    """Conical tower roof (rings of tiles shrinking 1 block every 1.4 rows) with a brass finial."""
    rr = r + 1.0
    y = y0
    while rr > 0.6:
        for x in range(int(cx - rr - 1), int(cx + rr + 2)):
            for z in range(int(cz - rr - 1), int(cz + rr + 2)):
                d = math.hypot(x - cx, z - cz)
                if rr - 1.3 < d <= rr + 0.3:
                    put(x, y, z, mat)
        rr -= 0.72
        y += 1
    put(int(cx), y, int(cz), BRASS)
    put(int(cx), y + 1, int(cz), WALL)
    return y + 1


def ruin(x0, z0, x1, z1, ylo, yhi, seed, rubble_y=None):
    """Break the masonry above a jagged line (value noise per column) - Tantallon-style broken tops."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            k = (math.sin(x * 0.9 + seed) * 0.5 + math.sin(z * 1.3 + seed * 2) * 0.5) * 0.5 + 0.5
            cut = int(ylo + (yhi - ylo) * (0.65 * k + 0.35 * h(x, 0, z, seed)))
            for y in range(cut, yhi + 12):
                if (x, y, z) in B and (x, y, z) not in MODEL_CELLS:
                    del B[(x, y, z)]
            if (x, cut - 1, z) in B and B[(x, cut - 1, z)][0] == "riftworld:castle_bricks" and h(x, cut, z, 9) < 0.5:
                B[(x, cut - 1, z)] = BRC


def rubble(cx, cz, r, y, seed, n=None):
    rr = random.Random(seed)
    n = n or int(r * r * 2.2)
    for _ in range(n):
        a = rr.random() * 6.283
        d = math.sqrt(rr.random()) * r
        x, z = int(round(cx + math.cos(a) * d)), int(round(cz + math.sin(a) * d))
        yy = y
        while (x, yy, z) in B and yy < y + 4:
            yy += 1
        if (x, yy - 1, z) not in B and yy > 1:
            continue
        k = rr.random()
        st = BRC if k < 0.35 else BR if k < 0.5 else slab("castle_bricks") if k < 0.75 else \
            stairs("castle_bricks", rr.choice(("north", "south", "east", "west"))) if k < 0.9 else ASH
        put(x, yy, z, st)


# ================================================================== foundation / floors
W, D = 76, 68
fill(4, 0, 10, 72, 0, 62, ASH)                       # ground floor slab of all ranges
fill(31, 0, 3, 45, 0, 13, ASH)                       # gatehouse

# ================================================================== MAIN HALL
HX0, HX1, HZ0, HZ1 = 24, 52, 14, 50
HTOP = 24
walls(HX0, HZ0, HX1, HZ1, 1, HTOP)
# floor: grey ashlar, mosaic aisle, dark borders along the walls
fill(26, 0, 16, 50, 0, 48, ASH)
fill(35, 0, 16, 41, 0, 41, MOS)
for z in range(16, 42):
    put(34, 0, z, pillar("z")); put(42, 0, z, pillar("z"))
# throne dais (3 steps) + niche
fill(31, 1, 41, 45, 1, 48, ASH)
fill(32, 2, 42, 44, 2, 48, ASH)
fill(33, 3, 43, 43, 3, 48, MOS)
for x in range(31, 46):
    put(x, 1, 41, stairs("ashlar", "south") if 33 <= x <= 43 else ASH)
for x in range(32, 45):
    put(x, 2, 42, stairs("ashlar", "south") if 33 <= x <= 43 else ASH)
for x in range(33, 44):
    put(x, 3, 43, stairs("ashlar", "south"))
for z in range(43, 49):
    put(32, 3, z, ASH); put(44, 3, z, ASH)
model("boss_throne", 38, 4, 46, "north")
# niche behind the throne: ashlar frame, brass ribs, pointed head
for y in range(4, 15):
    put(34, y, 48, pillar()); put(42, y, 48, pillar())
    for x in range(35, 42):
        put(x, y, 48, brick(x, y, 48) if y < 13 else ASH)
for x in range(35, 42):
    put(x, 4, 48, BRASS if x % 2 == 0 else ASH)
for x, y in ((35, 13), (36, 14), (37, 15), (38, 16), (39, 15), (40, 14), (41, 13)):
    put(x, y, 48, ASH); put(x, y, 47, stairs("ashlar", "south", "top"))
put(38, 15, 48, BRASS)
# 6 column sockets (symmetric): chamfered ashlar ring around a brass core
SOCKETS = [(31, z) for z in (23, 30, 37)] + [(45, z) for z in (23, 30, 37)]
for sx, sz in SOCKETS:
    put(sx, 0, sz, BRASS); put(sx, 1, sz, BRASS)
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if dx == 0 and dz == 0:
                continue
            if dx != 0 and dz != 0:
                put(sx + dx, 1, sz + dz, slab("ashlar"))
            else:
                f = {(1, 0): "west", (-1, 0): "east", (0, 1): "north", (0, -1): "south"}[(dx, dz)]
                put(sx + dx, 1, sz + dz, stairs("ashlar", f))
    for dx, dz in ((-2, 0), (2, 0), (0, -2), (0, 2)):
        put(sx + dx, 0, sz + dz, BRASS)
# candle path: pairs from the entrance to the dais (unlit; the entry scene lights them one by one)
for z in (19, 22, 25, 28, 31, 34, 37, 40):
    yb = 1 if z < 41 else 2
    for x in (35, 41):
        model("candle_stand", x, 1, z, "north")
        CANDLES.append((x, 1, z))
for z in (44, 47):
    for x in (34, 42):
        model("candle_stand", x, 4, z, "north")
        CANDLES.append((x, 4, z))

# C-3: the six healing columns (sunk in their sockets until the boss awakens) and the hidden arena heart in the dais
for sx, sz in SOCKETS:
    put(sx, 2, sz, S("boss_column", part=0, phase=0))
CORE = (38, 2, 46)
put(*CORE, S("arena_core"))


def _rel(pts):
    return [v for (x, y, z) in pts for v in (x - CORE[0], y - CORE[1], z - CORE[2])]


ARENA_NBT = {"Candles": _rel(CANDLES), "Sockets": _rel([(sx, 2, sz) for sx, sz in SOCKETS]), "Throne": _rel([(38, 4, 46)]),
             "HallMin": _rel([(25, 1, 15)]), "HallMax": _rel([(51, 20, 49)])}

# ---- galleries (y10) on the west, east and front sides
GY = 10
fill(26, GY, 16, 28, GY, 48, PL)
fill(48, GY, 16, 50, GY, 48, PL)
fill(29, GY, 16, 47, GY, 18, PL)
# corbels + beams carrying the galleries
for z in range(17, 49, 4):
    for x0, x1, f in ((26, 28, "west"), (48, 50, "east")):
        for x in range(x0, x1 + 1):
            put(x, GY - 1, z, beam("x"))
        xin = x1 if f == "west" else x0
        xw = x0 if f == "west" else x1
        put(xw, GY - 2, z, stairs("ashlar", "east" if f == "west" else "west", "top"))
for x in range(30, 47, 4):
    for z in (16, 17, 18):
        put(x, GY - 1, z, beam("z"))
    put(x, GY - 2, 16, stairs("ashlar", "north", "top"))
# balustrade with gaps = the four guards' jump points
JUMPS = {(29, 26), (29, 34), (47, 26), (47, 34)}
for z in range(19, 49):
    for x in (29, 47):
        if (x, z) in JUMPS:
            put(x, GY, z, BRASS)            # worn brass sill marks the drop
            continue
        put(x, GY, z, PL)
        put(x, GY + 1, z, FENCE if (z - 19) % 5 else beam("y"))
        if (z - 19) % 5 == 0:
            put(x, GY + 2, z, S("candle_stand_placeholder")) if False else None
for x in range(29, 48):
    put(x, GY, 19, PL)
    put(x, GY + 1, 19, FENCE if (x - 29) % 6 else beam("y"))
# gallery stairs: straight flights along both side walls (under the galleries), hole in the gallery floor above
for side, x0 in (("w", 26), ("e", 49)):
    for k in range(10):
        z = 20 + k
        for x in (x0, x0 + 1):
            put(x, 1 + k, z, stairs("dark_planks", "south"))
            for y in range(1, 1 + k):
                put(x, y, z, PL if y == k else brick(x, y, z))
            for y in range(2 + k, 2 + k + 3):
                if y >= GY:
                    clear(x, y, z) if y == GY else None
    for x in (x0, x0 + 1):                           # railing at the end of the stair hole
        put(x, GY + 1, 24, FENCE)
# ---- windows: clerestory lancets (both long walls), lower lancets above the wing roofs are the clerestory too
for z in range(19, 47, 5):
    lancet("z", z, 15, 6, HX0, [HX0 + 1], w=2)
    lancet("z", z, 15, 6, HX1, [HX1 - 1], w=2)
# front wall: entrance + gallery-level triple window
opening("x", None, 36, 40, 1, 7, [HZ0, HZ0 + 1])
put(38, 9, HZ0, BRASS)
for x0 in (31, 37, 43):
    lancet("x", x0, 16, 6, HZ0, [HZ0 + 1], w=3 if x0 == 37 else 2)
# side doors to the wings (under the galleries) and to the north range (behind the dais, west of it)
opening("z", None, 31, 33, 1, 5, [HX0, HX0 + 1])
opening("z", None, 31, 33, 1, 5, [HX1 - 1, HX1])
opening("x", None, 27, 29, 1, 4, [HZ1 - 1, HZ1])
# far wall: great rose window above the niche
RCX, RCY = 38, 21
for x in range(RCX - 5, RCX + 6):
    for y in range(RCY - 5, RCY + 6):
        d = math.hypot(x - RCX, y - RCY)
        if d <= 4.6:
            put(x, y, HZ1, GLASS); clear(x, y, HZ1 - 1)
        elif d <= 5.6:
            put(x, y, HZ1, ASH); put(x, y, HZ1 - 1, ASH)
put(RCX, RCY, HZ1, BRASS)
for a in range(8):
    an = a * math.pi / 4
    for t in (1.5, 2.5, 3.5):
        put(int(round(RCX + math.cos(an) * t)), int(round(RCY + math.sin(an) * t)), HZ1, ASH)
# string courses / parapet of the hall
band(HX0, HZ0, HX1, HZ1, 13)
band(HX0, HZ0, HX1, HZ1, HTOP)
# ---- open timber roof: tie beams, king posts, struts
RY0 = HTOP + 1
for z in range(19, 48, 6):
    for x in range(26, 51):
        put(x, HTOP, z, beam("x"))
    for y in range(HTOP + 1, HTOP + 14):
        put(38, y, z, beam("y"))
    for k in range(3, 8):
        put(38 - k, HTOP + k, z, beam("x"))
        put(38 + k, HTOP + k, z, beam("x"))
    put(26, HTOP - 1, z, stairs("ashlar", "east", "top")); put(50, HTOP - 1, z, stairs("ashlar", "west", "top"))
ridge = gable_roof_z(HX0 - 1, HX1 + 1, HZ0, HZ1, RY0, gable=False)
for z in range(HZ0, HZ1 + 1):                        # purlin under the ridge
    put(38, ridge - 2, z, beam("z"))
# stepped gables (front + back) rising above the roof with pinnacles and blind lancets
for zg in (HZ0, HZ1):
    for x in range(HX0 - 1, HX1 + 2):
        dxr = abs(x - 38)
        top = RY0 + ((16 - dxr) // 2) * 2 + (2 if dxr <= 1 else 0)
        for y in range(HTOP, top + 1):
            for zz in (zg, zg + (1 if zg == HZ0 else -1)):
                if zg == HZ1 and math.hypot(x - RCX, y - RCY) <= 5.6:
                    continue
                if zz == zg or y <= top - 1:
                    put(x, y, zz, brick(x, y, zz))
        put(x, top + 1, zg, slab("ashlar"))
    for x in (30, 34, 42, 46):                       # blind lancets (recessed ashlar panels)
        for y in range(RY0 + 2, RY0 + 2 + (11 - abs(x - 38))):
            put(x, y, zg, ASH)
    for x in (HX0, HX1):                             # corner pinnacles
        for y in range(HTOP, HTOP + 6):
            put(x, y, zg, ASH if y % 3 == 0 else brick(x, y, zg))
        put(x, HTOP + 6, zg, WALL); put(x, HTOP + 7, zg, BRASS) if zg == HZ1 else put(x, HTOP + 7, zg, WALL)
# hall pilaster strips between the clerestory windows
for z in range(17, 49, 5):
    for y in range(14, HTOP):
        put(HX0 - 1, y, z, ASH if y % 4 == 0 else brick(HX0 - 1, y, z))
        put(HX1 + 1, y, z, ASH if y % 4 == 0 else brick(HX1 + 1, y, z))
# bell turret on the ridge (small, brass bell = brass block)
for y in range(ridge, ridge + 5):
    for dx in (-1, 1):
        for dz in (-1, 1):
            put(38 + dx, y, 22 + dz, pillar())
put(38, ridge + 1, 22, BRASS)
for dx in (-1, 0, 1):
    for dz in (-1, 0, 1):
        put(38 + dx, ridge + 5, 22 + dz, ROOF)
put(38, ridge + 6, 22, ROOF); put(38, ridge + 7, 22, WALL)

# ================================================================== WINGS (shells; rooms furnished in C-2)
FY = 6        # upper storey floor
WT = 12       # wing wall top


def wing(x0, x1, z0, z1, outer_x, parts_low, parts_up, stair_x):
    walls(x0, z0, x1, z1, 1, WT)
    band(x0, z0, x1, z1, 1)
    band(x0, z0, x1, z1, FY)
    xi0, xi1 = x0 + 2, x1 - 2
    fill(xi0, FY, z0 + 2, xi1, FY, z1 - 2, PL)
    # joists under the upper floor
    for z in range(z0 + 3, z1 - 1, 3):
        for x in range(xi0, xi1 + 1):
            put(x, FY - 1, z, beam("x")) if (x, FY - 1, z) not in B else None
    for zp in parts_low:                              # ground-floor partitions with a door
        for x in range(xi0, xi1 + 1):
            for y in range(1, FY):
                put(x, y, zp, brick(x, y, zp))
        dx = (xi0 + xi1) // 2
        for x in (dx, dx + 1):
            for y in range(1, 4):
                clear(x, y, zp)
        put(dx, 4, zp, ASH); put(dx + 1, 4, zp, ASH)
    for zp in parts_up:
        for x in range(xi0, xi1 + 1):
            for y in range(FY + 1, WT + 1):
                put(x, y, zp, brick(x, y, zp))
        dx = (xi0 + xi1) // 2 - 3
        for x in (dx, dx + 1):
            for y in range(FY + 1, FY + 4):
                clear(x, y, zp)
    # internal stair (straight flight along the outer wall) with a hole in the upper floor
    sz0 = stair_x[1]
    for k in range(FY):
        z = sz0 + k
        for x in stair_x[0]:
            put(x, 1 + k, z, stairs("dark_planks", "south"))
            for y in range(1, 1 + k):
                put(x, y, z, PL)
            clear(x, FY, z) if k < FY - 1 else None
            for y in range(2 + k, FY + 1):
                if y >= FY and k < FY - 1:
                    clear(x, y, z)
    # windows: ground slits, upper lancets (outer long wall + end walls)
    for z in range(z0 + 4, z1 - 3, 5):
        lancet("z", z, 2, 2, outer_x, [outer_x + (1 if outer_x == x0 else -1)])
        lancet("z", z, FY + 2, 4, outer_x, [outer_x + (1 if outer_x == x0 else -1)])
    # buttresses on the outer wall
    bx = outer_x - 1 if outer_x == x0 else outer_x + 1
    for z in range(z0 + 2, z1, 5):
        for y in range(1, WT - 1):
            put(bx, y, z, ASH if y in (1, FY) else brick(bx, y, z))
        put(bx, WT - 1, z, stairs("castle_bricks", "east" if outer_x == x0 else "west"))
    # steep gable roof (Malbork-like) leaving the hall clerestory free above the valley
    if outer_x == x0:
        return gable_roof_z(x0 - 1, x1 - 2, z0, z1, WT + 1)
    return gable_roof_z(x0 + 2, x1 + 1, z0, z1, WT + 1)


wing(4, 25, 10, 62, 4, parts_low=(28, 45), parts_up=(20, 38, 52), stair_x=((6, 7), 30))
wing(51, 72, 10, 62, 72, parts_low=(28, 47), parts_up=(28, 47), stair_x=((69, 70), 14))
# front walls of the wings: entrance doors + windows facing the city
opening("x", None, 13, 15, 1, 4, [10, 11])
opening("x", None, 61, 63, 1, 4, [10, 11])
for x in (7, 19):
    lancet("x", x, FY + 2, 4, 10, [11])
for x in (56, 68):
    lancet("x", x, FY + 2, 4, 10, [11])
# chapel: no upper floor between z29..46 (double height), tall lancets
for x in range(53, 71):
    for z in range(29, 47):
        clear(x, FY, z); clear(x, FY - 1, z)
for z in (32, 37, 42):
    for y in range(2, 11):
        put(72, y, z, GLASS); clear(71, y, z)
    put(72, 11, z, ASH); put(71, 11, z, ASH); put(72, 1, z, ASH)
fill(58, 1, 44, 65, 1, 46, ASH)                    # chapel altar platform
put(61, 2, 46, BRASS); put(62, 2, 46, BRASS)
# treasury: brass-framed door
for y in range(1, 5):
    put(60, y, 47, BRASS) if y < 4 else None
    put(63, y, 47, BRASS) if y < 4 else None

# ================================================================== NORTH RANGE (behind the hall)
walls(24, 49, 52, 62, 1, WT)
band(24, 49, 52, 62, 1)
band(24, 49, 52, 62, FY)
fill(26, FY, 51, 50, FY, 60, PL)
for x in range(26, 51):
    for y in range(1, WT + 1):
        put(x, y, 51, brick(x, y, 51)) if y <= FY else None
for y in range(1, FY):
    for z in range(52, 61):
        put(38, y, z, brick(38, y, z))
for z in (55, 56):
    for y in range(1, 4):
        clear(38, y, z)
for x in range(28, 49, 5):
    lancet("x", x, 2, 2, 62, [61])
    lancet("x", x, FY + 2, 4, 62, [61])
for i in range(6):                                  # gable roof with the ridge along x (keeps the rose window free)
    for x in range(24, 53):
        put(x, WT + 1 + i, 51 + i, stairs("castle_roof", "south"))
        put(x, WT + 1 + i, 63 - i, stairs("castle_roof", "north"))
for x in range(24, 53):
    put(x, WT + 7, 57, slab("castle_roof")); put(x, WT + 6, 57, ROOF)
# connect the hall's back door into the north range
for x in range(27, 30):
    for y in range(1, 4):
        clear(x, y, 51)

# ================================================================== GATEHOUSE
walls(31, 3, 45, 13, 1, 12)
band(31, 3, 45, 13, 1)
fill(33, 7, 5, 43, 7, 11, PL)
crenel(31, 3, 45, 13, 13)
opening("x", None, 36, 40, 1, 7, [3, 4])
for x in range(36, 41):                             # raised portcullis
    put(x, 7, 5, FENCE); put(x, 6, 5, FENCE) if x % 2 == 0 else None
for z in range(5, 12):                              # passage vault ribs
    if z % 2 == 1:
        for x in range(36, 41):
            put(x, 7, z, ASH)
put(38, 9, 3, BRASS)
for x in (34, 42):
    lancet("x", x, 9, 2, 3, [4])
for cx in (32, 44):                                 # round turrets flanking the gate
    cylinder(cx, 4, 3, 1, 19, t=1)
    disc(cx, 4, 3, 20, ASH)
    cone(cx, 4, 3, 21)
    for y in (6, 12):
        put(cx, y, 1, GLASS)

# ================================================================== TOWERS
# NW: round, tall, cone roof (prison levels inside - C-2)
cylinder(5, 61, 6, 1, 34)
for y in (8, 16, 24):
    disc(5, 61, 4, y, PL)
disc(5, 61, 6, 35, ASH)
for a in range(0, 360, 30):
    x, z = int(round(5 + math.cos(math.radians(a)) * 6)), int(round(61 + math.sin(math.radians(a)) * 6))
    put(x, 36, z, brick(x, 36, z)) if a % 60 == 0 else None
cone(5, 61, 6, 37)
for y in (4, 12, 20, 28):
    for (x, z) in ((5, 55) if y % 8 == 4 else (0, 61), (11, 61) if y % 8 == 4 else (5, 67)):
        if 0 <= x < W and 0 <= z < D:
            put(x, y, z, GLASS); put(x, y + 1, z, GLASS)
# NE: square keep 11x11, tallest; crenellated with corner bartizans
fill(65, 1, 54, 71, 50, 60, None)
walls(63, 52, 73, 62, 1, 44)
fill(65, 44, 54, 71, 44, 60, PL)
for y in (8, 16, 24, 32, 40):
    fill(65, y, 54, 71, y, 60, PL)
    band(63, 52, 73, 62, y, ASH)
crenel(63, 52, 73, 62, 45)
for (bx, bz) in ((63, 52), (73, 62), (73, 52)):
    cylinder(bx, bz, 2, 41, 49, t=1)
    disc(bx, bz, 2, 50, ASH)
    cone(bx, bz, 2, 51)
for y in range(4, 44, 8):
    lancet("x", 68, y, 3, 52, [53]); lancet("z", 57, y, 3, 73, [72])
    lancet("x", 68, y, 3, 62, [61]) if y > 12 else None
# SW: octagon with a broken top
cylinder(5, 10, 5, 1, 30, octagon=True)
for y in (8, 16):
    disc(5, 10, 3, y, PL, octagon=True)
for y in (5, 13, 21):
    put(0, y, 10, GLASS); put(5, y, 5, GLASS)
ruin(0, 5, 10, 15, 22, 30, 3.0)
# SE: square, heavily ruined (one face collapsed low)
walls(66, 2, 74, 10, 1, 26)
fill(68, 8, 4, 72, 8, 8, PL)
ruin(66, 2, 74, 10, 13, 26, 7.0)
ruin(73, 2, 74, 10, 4, 9, 11.0)
rubble(76 - 1, 6, 3, 1, 21)

# ================================================================== RUINS of the main body
# collapsed part of the hall roof (NE) with broken beams; east gallery partly fallen
for x in range(41, 55):
    for z in range(36, 45):
        k = h(x, 0, z, 5)
        edge = min(x - 41, 54 - x, z - 36, 44 - z)
        if edge >= 1 or k < 0.6:
            for y in range(RY0, ridge + 2):
                if (x, y, z) in B and B[(x, y, z)][0] in ("riftworld:castle_roof_stairs", "riftworld:castle_roof", "riftworld:castle_roof_slab"):
                    del B[(x, y, z)]
for x in range(39, 51):
    clear(x, HTOP, 37) if x > 44 else None
for z in range(38, 43):
    for x in (48, 49, 50):
        if h(x, 1, z, 2) < 0.7:
            clear(x, GY, z)
put(47, GY, 40, AIR) if False else None
for z in range(38, 43):
    clear(47, GY, z); clear(47, GY + 1, z)
rubble(46, 40, 4, 1, 31, 40)
rubble(49, 39, 2, GY + 1, 32, 6)
for (x, y, z, a) in ((45, 1, 39, "x"), (44, 2, 40, "z"), (47, 1, 41, "x")):
    put(x, y, z, beam(a))
# hall clerestory: east upper wall broken around the collapse
ruin(HX1 - 1, 37, HX1, 43, 19, HTOP, 4.0)
# east wing outer wall collapsed (z31..40) with rubble spilling out and in
ruin(71, 31, 72, 40, 2, WT + 8, 13.0)
for x in range(53, 74):
    for z in range(31, 41):
        for y in range(WT + 1, WT + 12):
            if (x, y, z) in B and "castle_roof" in B[(x, y, z)][0] and h(x, y, z, 3) < 0.75 and x > 60:
                del B[(x, y, z)]
rubble(73, 35, 3, 1, 41, 30)
rubble(68, 35, 3, 1, 42, 22)
# west wing roof: a smaller hole over the kitchen
for x in range(8, 16):
    for z in range(50, 56):
        for y in range(WT + 1, WT + 12):
            if (x, y, z) in B and h(x, y, z, 7) < 0.8 and min(x - 8, 15 - x, z - 50, 55 - z) >= 1:
                del B[(x, y, z)]
rubble(12, 53, 2, FY + 1, 51, 8)
# weathering: random cracked/mossy patches near the ground on all outer faces handled by brick(); a few missing merlons
for (x, y, z), st in list(B.items()):
    if st == ASH and y >= 16 and h(x, y, z, 77) < 0.04:
        B[(x, y, z)] = BRC

# ================================================================== C-2: rooms
exec(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "castle_rooms.py"), encoding="utf-8").read())

# ================================================================== Rev 4: lengthen the throne hall
# Insert DZ rows (copies of row ZC-1: side galleries, balustrades, walls, floor, roof) between the 3rd and the 4th candle
# pair rows' sockets: everything at z >= ZC (back half of the hall, dais, throne, arena heart, north range) moves by DZ,
# models move as a whole with their master; glass / brass marks in the copied rows become plain masonry / floor.
ZC, DZ = 36, 8


def zmap(z):
    return z + DZ if z >= ZC else z


def _remap():
    global B, MODEL_CELLS, NBT, MODELS
    shift = {}
    for (m, cs) in MODELS:
        d = DZ if m[2] >= ZC else 0
        for c in cs:
            shift[c] = d
    nb = {}
    for (x, y, z), st in B.items():
        nb[(x, y, z + shift[(x, y, z)] if (x, y, z) in shift else zmap(z))] = st
    for (x, y, z), st in B.items():
        if z != ZC - 1 or (x, y, z) in MODEL_CELLS or (x, y, z) in NBT:
            continue
        n = st[0].split(":")[1]
        if "stained_glass" in n or n in ("castle_bars",):
            st = brick(x, y, z)
        elif n == "brass_block":
            st = ASH
        for k in range(DZ):
            nb.setdefault((x, y, ZC + k), st)
    B = nb
    MODEL_CELLS = {(x, y, z + shift[(x, y, z)]) for (x, y, z) in MODEL_CELLS}
    NBT = {(x, y, z + shift.get((x, y, z), DZ if z >= ZC else 0)): v for (x, y, z), v in NBT.items()}
    MODELS = [((m[0], m[1], m[2] + (DZ if m[2] >= ZC else 0)), [(c[0], c[1], c[2] + (DZ if m[2] >= ZC else 0)) for c in cs]) for m, cs in MODELS]


_remap()
D += DZ
CANDLES[:] = [(x, y, zmap(z)) for (x, y, z) in CANDLES]
SOCKETS = [(x, zmap(z)) for (x, z) in SOCKETS]
JUMPS = {(x, zmap(z)) for (x, z) in JUMPS}
CORE = (CORE[0], CORE[1], zmap(CORE[2]))
THRONE = (38, 4, zmap(46))
# two more candle pairs along the new stretch of the aisle (lit in order with the others)
_extra = []
for z in (ZC + 2, ZC + 5):
    for x in (35, 41):
        model("candle_stand", x, 1, z, "north")
        _extra.append((x, 1, z))
_i = max(i for i, c in enumerate(CANDLES) if c[2] < ZC) + 1
CANDLES[_i:_i] = _extra
ARENA_NBT = {"Candles": _rel(CANDLES), "Sockets": _rel([(sx, 2, sz) for sx, sz in SOCKETS]), "Throne": _rel([THRONE]),
             "HallMin": _rel([(25, 1, 15)]), "HallMax": _rel([(51, 20, zmap(49))])}
NW_STEPS = [[a, b, zmap(c)] + list(r) for (a, b, c, *r) in NW_STEPS] if NW_STEPS and len(NW_STEPS[0]) >= 3 else NW_STEPS
SW_STEPS = [[a, b, zmap(c)] + list(r) for (a, b, c, *r) in SW_STEPS] if SW_STEPS and len(SW_STEPS[0]) >= 3 else SW_STEPS
KEEP_STEPS = [[a, b, zmap(c)] + list(r) for (a, b, c, *r) in KEEP_STEPS] if KEEP_STEPS and len(KEEP_STEPS[0]) >= 3 else KEEP_STEPS

# ================================================================== connections (panes / fences / walls)
SOLID_SKIP = ("stairs", "slab", "part", "candle_stand", "boss_throne", "fence", "_wall", "stained_glass")


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
            if name == "dark_fence":
                ok = same or solid(nb)
            elif name in ("stained_glass", "castle_bars"):
                ok = same or solid(nb)
            else:
                ok = same or solid(nb) or (nb is not None and "fence" in nb[0])
            con[d] = ok
        if name == "castle_bricks_wall":
            up = not ((con["north"] and con["south"] and not con["east"] and not con["west"]) or
                      (con["east"] and con["west"] and not con["north"] and not con["south"])) or (x, y + 1, z) in B
            B[(x, y, z)] = S("castle_bricks_wall", up=up, waterlogged=False,
                             **{k: ("low" if v else "none") for k, v in con.items()})
        else:
            B[(x, y, z)] = S(name, waterlogged=False, **con)

# ================================================================== save
B = {k: v for k, v in B.items() if v != AIR and 0 <= k[0] < W and 0 <= k[2] < D and k[1] >= 0}
H = max(p[1] for p in B) + 1
for sx, sz in SOCKETS:              # keep the rise path of the columns clear of later rubble
    for yy in (3, 4, 5):
        B.pop((sx, yy, sz), None)
NBT[CORE] = ARENA_NBT
n, pal = write_template(OUT + "/castle.nbt", (W, H, D), B, {k: v for k, v in NBT.items() if k in B})
meta = {"size": [W, H, D], "blocks": n, "palette": pal, "candles": CANDLES, "sockets": SOCKETS, "jumps": sorted(JUMPS),
        "throne": list(THRONE), "entrance": [38, 1, 2], "core": list(CORE)}
json.dump(meta, open(OUT + "/castle_meta.json", "w"), indent=1)
json.dump({"NW": NW_STEPS, "SW": SW_STEPS, "KEEP": KEEP_STEPS},
          open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "tower_steps.json"), "w"))
print("castle", W, H, D, "blocks", n, "palette", pal, "candles", len(CANDLES))
