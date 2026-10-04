"""Generates data/riftworld/structures/laboratory.nbt (abandoned research complex) + worldgen JSON.

Template 63 x 46 x 63.  Block layers:
  y0  shaft bottom            y24 basement floor   y25-28 basement   y29 basement ceiling
  y30 ground floor (stand 31) y31-36 ground floor  y37 ceiling       y38 2nd floor (stand 39)
  y39-42 2nd floor            y43 ceiling          y44 roof          y45 parapet
"""
import gzip, io, json, os, random, struct

OUT = "/home/user/mods/riftworld/src/main/resources/data/riftworld"
W, H, D = 63, 47, 70
rng = random.Random(1987)
B = {}          # (x,y,z) -> (name, props-tuple)
NBT = {}        # (x,y,z) -> dict (block entity)
MODEL_CELLS = set()


def S(name, **p):
    if ":" not in name:
        name = "riftworld:" + name
    return (name, tuple(sorted((k, str(v).lower()) for k, v in p.items())))


AIR = S("minecraft:air")


def put(x, y, z, st, nbt=None):
    if not (0 <= x < W and 0 <= y < H and 0 <= z < D):
        return
    if (x, y, z) in MODEL_CELLS:
        return
    B[(x, y, z)] = st
    if nbt is not None:
        NBT[(x, y, z)] = nbt
    else:
        NBT.pop((x, y, z), None)


def get(x, y, z):
    return B.get((x, y, z), AIR)


def fill(x0, y0, z0, x1, y1, z1, st):
    for x in range(min(x0, x1), max(x0, x1) + 1):
        for y in range(min(y0, y1), max(y0, y1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                put(x, y, z, st() if callable(st) else st)


def pick(*opts):
    """Weighted random block: pick(("lab_panel", 6), ("lab_panel_dirty", 3))."""
    tot = sum(w for _, w in opts)
    r = rng.random() * tot
    for n, w in opts:
        r -= w
        if r <= 0:
            return S(n)
    return S(opts[-1][0])


panel = lambda: pick(("lab_panel", 7), ("lab_panel_dirty", 3))
stripe = lambda: pick(("lab_panel_stripe", 6), ("lab_panel_stripe_dirty", 3))
tile = lambda: pick(("lab_tile", 6), ("lab_tile_cracked", 3))
concrete = lambda: pick(("lab_concrete", 6), ("lab_concrete_cracked", 3))
ceiling = lambda: pick(("lab_ceiling", 6), ("lab_ceiling_dirty", 3))
steel = lambda: pick(("steel_floor", 5), ("steel_floor_rusty", 3))
facade = lambda: S("lab_facade")

# ---------------------------------------------------------------- models
KINDS = {  # name: (x0, x1, back, h)
    "portal_helm": (-1, 1, 1, 3), "portal_lever": (0, 0, 0, 2), "control_console": (0, 1, 0, 2), "lathe": (0, 1, 0, 1),
    "milling_machine": (0, 0, 0, 2), "specimen_tank": (0, 0, 0, 3), "electrical_cabinet": (0, 0, 0, 2),
    "generator": (-1, 1, 1, 2), "transformer": (0, 1, 0, 2), "server_rack": (0, 0, 0, 2), "security_desk": (-1, 1, 0, 2),
    "crate_wood": (0, 0, 0, 1), "crate_metal": (0, 0, 0, 1), "lab_door": (0, 1, 0, 3), "lab_table": (0, 1, 0, 1),
    "shelf": (0, 1, 0, 3), "office_chair": (0, 0, 0, 1), "locker": (0, 0, 0, 2)}


def rot(x, y, z, f):
    return {"south": (x, y, z), "north": (-x, y, -z), "west": (-z, y, x), "east": (z, y, -x)}[f]


def cells(kind, f):
    x0, x1, back, h = KINDS[kind]
    out = []
    for x in range(x0, x1 + 1):
        for b in range(back + 1):
            for y in range(h):
                out.append(rot(x, y, -b, f))
    return out


def model(kind, x, y, z, f, nbt=None, **props):
    """Place a model master at (x,y,z) facing f plus its part cells; asserts free space."""
    cs = [(x + dx, y + dy, z + dz) for dx, dy, dz in cells(kind, f)]
    for c in cs:
        assert c not in MODEL_CELLS, (kind, (x, y, z), c, "overlaps another model")
        assert 0 <= c[0] < W and 0 <= c[1] < H and 0 <= c[2] < D, (kind, c)
    st = dict(facing=f, powered=False, broken=False)
    st.update(props)
    if kind == "lab_door":
        st.setdefault("open", False)
    be = {"id": "riftworld:crate" if kind.startswith("crate") else "riftworld:model"}
    if nbt:
        be.update(nbt)
    for c in cs:
        if c == (x, y, z):
            B[c] = S(kind, **st)
            NBT[c] = be
        else:
            B[c] = S("part", open=st.get("open", False))
            NBT.pop(c, None)
    MODEL_CELLS.update(cs)


def crate(x, y, z, f="south", metal=False, items=None):
    nbt = {"LootTable": "riftworld:chests/lab_storage", "LootTableSeed": rng.randrange(1 << 40)} if items is None else {"Items": items}
    model("crate_metal" if metal else "crate_wood", x, y, z, f, nbt)


STONE = [{"Slot": ("b", 13), "id": "riftworld:resonance_stone", "Count": ("b", 1)}]

# ---------------------------------------------------------------- shell
G, F2, ROOF = 30, 38, 44     # floor block layers
BAS = 24
# clear the whole building volume first (air carves terrain)
fill(0, BAS, 0, W - 1, H - 1, D - 1, AIR)
D0 = D
D = 63
# foundations / basement shell
fill(0, BAS - 1, 0, W - 1, BAS - 1, D - 1, S("lab_concrete"))
fill(0, BAS, 0, W - 1, BAS, D - 1, steel)                         # basement floor
for y in range(BAS + 1, BAS + 5):
    for x in range(W):
        for z in (0, D - 1):
            put(x, y, z, concrete()); put(z, y, x, concrete())
fill(0, 29, 0, W - 1, 29, D - 1, ceiling)                          # basement ceiling
fill(0, G, 0, W - 1, G, D - 1, tile)                               # ground floor
fill(0, 37, 0, W - 1, 37, D - 1, ceiling)
fill(0, F2, 0, W - 1, F2, D - 1, tile)
fill(0, 43, 0, W - 1, 43, D - 1, ceiling)
fill(0, ROOF, 0, W - 1, ROOF, D - 1, concrete)
for x in range(W):                                                 # parapet
    for z in (0, D - 1):
        put(x, 45, z, S("lab_concrete")); put(z, 45, x, S("lab_concrete"))
        put(x, 46, z, S("lab_concrete_slab", type="bottom")); put(z, 46, x, S("lab_concrete_slab", type="bottom"))

# exterior walls with window bands
for y in range(G + 1, ROOF):
    for i in range(W):
        for (x, z, along) in ((i, 0, "x"), (i, D - 1, "x"), (0, i, "z"), (W - 1, i, "z")):
            win = (y in (32, 33, 34) or y in (40, 41)) and 2 <= i <= W - 3 and i % 4 != 0
            if y in (G + 1, 37, F2, 43) or i in (0, W - 1) or i % 8 == 0:
                st = S("lab_concrete")
            elif win:
                st = S("lab_glass_cracked") if rng.random() < 0.3 else (AIR if rng.random() < 0.08 else S("lab_glass"))
            elif y == 36 or y == 42:
                st = S("lab_panel_stripe_dirty")
            else:
                st = facade()
            put(x, y, z, st)

D = D0
# ---------------------------------------------------------------- shaft
for y in range(0, G):
    for i in range(25, 38):
        for (x, z) in ((i, 25), (i, 37), (25, i), (37, i)):
            put(x, y, z, S("shaft_wall"))
fill(26, 0, 26, 36, 0, 36, S("steel_floor_rusty"))
fill(26, 1, 26, 36, G - 1, 36, AIR)
for y in range(2, G - 1, 6):                                       # rings of red emergency light down the shaft
    for i in (28, 31, 34):
        for (x, z) in ((i, 25), (i, 37), (25, i), (37, i)):
            put(x, y, z, S("emergency_lamp", lit=(y < 12)))
fill(26, G, 26, 36, G, 36, lambda: S("rusty_grate") if rng.random() < 0.35 else S("metal_grate"))
for i in range(24, 39):                                            # hazard border
    for (x, z) in ((i, 24), (i, 38), (24, i), (38, i), (i, 25), (i, 37), (25, i), (37, i)):
        if 25 <= x <= 37 and 25 <= z <= 37 and not (x in (25, 37) or z in (25, 37)):
            continue
        put(x, G, z, pick(("hazard_block_worn", 6), ("hazard_block", 3)))

# ---------------------------------------------------------------- interior walls (ground + 2nd floor)


def wall_x(x, z0, z1, y0, y1, stripe_y=None):
    for z in range(z0, z1 + 1):
        for y in range(y0, y1 + 1):
            put(x, y, z, stripe() if y == stripe_y else panel())


def wall_z(z, x0, x1, y0, y1, stripe_y=None):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            put(x, y, z, stripe() if y == stripe_y else panel())


def opening(x0, z0, x1, z1, y0, h=3):
    fill(x0, y0, z0, x1, y0 + h - 1, z1, AIR)


for (y0, y1, sy) in ((G + 1, 36, 32), (F2 + 1, 42, 40)):
    wall_x(12, 1, 61, y0, y1, sy); wall_x(50, 1, 61, y0, y1, sy)          # wing walls
    wall_z(12, 13, 49, y0, y1, sy); wall_z(50, 13, 49, y0, y1, sy)        # north labs / south lobby walls
    wall_z(12, 1, 11, y0, y1, sy); wall_z(12, 51, 61, y0, y1, sy)         # stairwell walls (NW / NE)
for (y0, y1, sy) in ((BAS + 1, 28, None),):
    wall_x(12, 1, 11, y0, y1); wall_z(12, 1, 11, y0, y1)
    wall_x(50, 1, 11, y0, y1); wall_z(12, 51, 61, y0, y1)
# hall walls x/z 17 & 45, ground floor only (hall is open to the roof)
for y in range(G + 1, 37):
    for i in range(17, 46):
        for (x, z) in ((i, 17), (i, 45), (17, i), (45, i)):
            put(x, y, z, stripe() if y == 32 else panel())
# corridor ring x/z 13..16 and 46..49 is open space between wing walls and hall walls
# hall: open from ground to roof; 2nd floor becomes a 2-wide gallery
fill(19, 29 + 0, 19, 43, 29, 43, ceiling)  # (basement ceiling under hall stays)
fill(17, 37, 17, 45, F2, 45, AIR)
for i in range(17, 46):
    for k in (17, 18, 44, 45):
        for (x, z) in ((i, k), (k, i)):
            put(x, 37, z, ceiling()); put(x, F2, z, steel())
for i in range(19, 44):                                            # gallery railing (glass)
    for (x, z) in ((i, 19), (i, 43), (19, i), (43, i)):
        if 19 <= x <= 43 and 19 <= z <= 43:
            put(x, F2, z, S("steel_floor_slab", type="top"))
            put(x, F2 + 1, z, S("lab_glass") if rng.random() > 0.12 else S("lab_glass_cracked"))
fill(20, 39, 20, 42, 42, 42, AIR)
# skylight
for x in range(20, 43):
    for z in range(20, 43):
        put(x, 43, z, AIR); put(x, ROOF, z, AIR)
        put(x, ROOF + 2, z, S("lab_concrete") if (x - 20) % 6 == 0 or (z - 20) % 6 == 0 else
            (S("lab_glass_cracked") if rng.random() < 0.25 else (AIR if rng.random() < 0.05 else S("lab_glass"))))

for i in range(19, 44):
    for (x, z) in ((i, 19), (i, 43), (19, i), (43, i)):
        put(x, ROOF, z, S("lab_concrete")); put(x, ROOF + 1, z, S("lab_concrete") if i in (19, 43) or (i - 19) % 6 == 0 else S("lab_glass"))
        put(x, ROOF + 2, z, S("lab_concrete"))
# doorways
opening(17, 30, 17, 32, G + 1); opening(45, 30, 45, 32, G + 1)           # hall <-> corridor west/east
opening(30, 17, 32, 17, G + 1)                                          # hall north
opening(12, 26, 12, 28, G + 1); opening(50, 26, 50, 28, G + 1)           # wings
opening(12, 44, 12, 45, G + 1); opening(50, 44, 50, 45, G + 1)
opening(24, 12, 25, 12, G + 1); opening(37, 12, 38, 12, G + 1)           # north labs
opening(12, 26, 12, 28, F2 + 1); opening(50, 26, 50, 28, F2 + 1)         # 2nd floor
opening(24, 12, 25, 12, F2 + 1); opening(37, 12, 38, 12, F2 + 1)
opening(30, 50, 32, 50, F2 + 1)
opening(17, 30, 17, 32, F2 + 1); opening(45, 30, 45, 32, F2 + 1)         # gallery access
opening(30, 17, 32, 17, F2 + 1); opening(30, 45, 32, 45, F2 + 1)
for (x0, x1) in ((17, 17), (45, 45)):
    pass
# hall wall above the ground floor on 2nd level: low glass partition
for i in range(17, 46):
    for (x, z) in ((i, 16), (i, 46), (16, i), (46, i)):
        pass

# sliding doors (models)
model("lab_door", 31, G + 1, 62, "south")                                # main entrance (outer wall)
fill(31, G + 1, 62, 32, G + 3, 62, AIR) if False else None
model("lab_door", 31, G + 1, 45, "south")                                # hall south
model("lab_door", 31, G + 1, 50, "south")                                # lobby -> corridor
model("lab_door", 11, G + 1, 50, "east") if False else None
for (x, y, z, f) in ((31, F2 + 1, 50, "south"),):
    pass

# ---------------------------------------------------------------- stairwells (NW and NE corners)


def stairwell(mirror):
    X = (lambda x: x) if not mirror else (lambda x: W - 1 - x)
    fm = (lambda f: f) if not mirror else (lambda f: {"east": "west", "west": "east"}.get(f, f))
    # open the shaft of the stairwell through all floors, keep the core
    for y in range(BAS + 1, 43):
        for x in range(1, 12):
            for z in range(1, 12):
                put(X(x), y, z, AIR)
    for y in range(BAS + 1, 44):
        for x in range(4, 10):
            for z in range(4, 10):
                put(X(x), y, z, S("lab_concrete") if (x in (4, 9) or z in (4, 9)) else S("lab_concrete"))
    path = []
    path += [((10, 11), (z, z), "south") for z in range(4, 10)]      # east band, going south  L26..31
    path += [((10, 11), (10, 11), None)]                              # SE landing  L31 (ground)
    path += [((x, x), (10, 11), "west") for x in range(9, 3, -1)]     # south band, going west  L32..37
    path += [((2, 3), (10, 11), None)]                                # SW landing  L37
    path += [((2, 3), (z, z), "north") for z in range(9, 7, -1)]      # west band L38, 39
    L = 25
    for (xs, zs, f) in path:
        if f is not None:
            L += 1
        for x in range(xs[0], xs[1] + 1):
            for z in range(zs[0], zs[1] + 1):
                for y in range(BAS + 1, L - 1):
                    put(X(x), y, z, S("lab_concrete"))
                put(X(x), L - 1, z, S("lab_concrete_stairs", facing=fm(f), half="bottom", shape="straight", waterlogged=False)
                    if f else tile())
    # basement start landing (NE corner of the well) and 2nd-floor landing (west band north end, NW corner, north band)
    fill(X(10), BAS, 2, X(11), BAS, 3, steel)
    for (x0, x1, z0, z1) in ((2, 3, 2, 7), (4, 11, 2, 3)):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                put(X(x), F2, z, tile()); put(X(x), 37, z, ceiling())
    # railings along the open edges of the upper landing
    for z in range(4, 8):
        put(X(4), F2 + 1, z, S("lab_glass")) if False else None
    # exits: ground -> through z=12 at x 10-11 ; 2nd floor -> through x=12 at z 2-3 ; basement -> through x=12 at z 2-3
    for x in (10, 11):
        for y in range(G + 1, G + 4):
            put(X(x), y, 12, AIR)
    for z in (2, 3):
        for y in range(F2 + 1, F2 + 4):
            put(X(12), y, z, AIR)
        for y in range(BAS + 1, BAS + 4):
            put(X(12), y, z, AIR)
    # lamps
    put(X(6), 43, 2, S("emergency_lamp", lit=True)); put(X(2), 29, 6, S("emergency_lamp", lit=not mirror))


stairwell(False)
stairwell(True)

# ---------------------------------------------------------------- basement: warehouse (stone 1)
# pillars
for x in range(6, 60, 8):
    for z in range(6, 60, 8):
        if 23 <= x <= 39 and 23 <= z <= 39:
            continue
        if (x < 13 and z < 13) or (x > 49 and z < 13):
            continue
        fill(x, BAS + 1, z, x, 28, z, S("lab_concrete"))
        put(x, 28, z, S("hazard_block_worn")) if rng.random() < 0.3 else None
# shelving rows with crates
for row_z in (16, 20, 42, 46, 52, 56):
    for x in range(15, 48, 3):
        if 23 <= x <= 39 and 22 <= row_z <= 40:
            continue
        if rng.random() < 0.85:
            model("shelf", x, BAS + 1, row_z, "south" if row_z % 8 == 0 else "north") if False else None
for (x, z, f) in ((15, 16, "south"), (18, 16, "south"), (21, 16, "south"), (40, 16, "south"), (43, 16, "south"), (46, 16, "south"),
                  (15, 47, "north"), (18, 47, "north"), (21, 47, "north"), (40, 47, "north"), (43, 47, "north"), (46, 47, "north"),
                  (15, 56, "north"), (18, 56, "north"), (40, 56, "north"), (43, 56, "north")):
    model("shelf", x, BAS + 1, z, f)
# crate stacks
crate_spots = [(14, 20), (15, 20), (14, 21), (20, 21), (21, 21), (41, 20), (42, 20), (47, 21), (16, 43), (17, 43), (44, 42), (45, 43),
               (20, 52), (21, 52), (21, 53), (34, 54), (35, 54), (35, 55), (28, 56), (29, 56), (52, 30), (53, 30), (52, 31),
               (56, 40), (57, 40), (8, 30), (9, 30), (8, 31), (5, 42), (6, 42), (5, 50), (5, 51), (6, 51), (57, 52), (58, 52)]
for i, (x, z) in enumerate(crate_spots):
    metal = i % 3 == 1
    crate(x, BAS + 1, z, rng.choice(["north", "south", "east", "west"]), metal)
    if i % 4 == 0 and (x, BAS + 2, z) not in MODEL_CELLS:
        crate(x, BAS + 2, z, "south", not metal)
# THE stone #1: metal crate in the far SW corner behind the generator
crate(3, BAS + 1, 59, "north", True, STONE)
model("generator", 8, BAS + 1, 58, "north")
model("transformer", 54, BAS + 1, 58, "north")
model("transformer", 54, BAS + 1, 18, "south")
model("electrical_cabinet", 60, BAS + 1, 24, "west"); model("electrical_cabinet", 60, BAS + 1, 25, "west")
for x in range(2, 61):                                             # pipes along the basement ceiling
    if not (23 <= x <= 39):
        put(x, 28, 2 if x > 12 and x < 50 else 13, S("pipe", axis="x"))
for z in range(14, 61):
    if not (24 <= z <= 38):
        put(2, 28, z, S("pipe", axis="z")); put(60, 28, z, S("pipe", axis="z"))
# basement lamps: mostly dead, a couple of red emergency
for x in range(10, 56, 8):
    for z in range(10, 56, 8):
        if 23 <= x <= 39 and 23 <= z <= 39:
            continue
        if get(x, 29, z)[0].endswith("ceiling") or get(x, 29, z)[0].endswith("ceiling_dirty"):
            put(x, 29, z, S("emergency_lamp", lit=True) if rng.random() < 0.15 else S("lab_lamp", lit=False))

# ---------------------------------------------------------------- ground floor: hall with helm
fill(26, G + 1, 39, 36, G + 1, 43, S("steel_floor"))               # control dais
for x in range(26, 37):
    put(x, G + 1, 39, S("hazard_block"))
for x in range(29, 34):
    put(x, G + 1, 44, S("steel_floor_stairs", facing="north", half="bottom", shape="straight", waterlogged=False))
for z in (40, 42):
    for x in (26, 36):
        pass
model("portal_helm", 31, G + 2, 41, "north", {"Stones": 1})
model("portal_lever", 31, G + 2, 39, "south")
model("control_console", 27, G + 2, 41, "north")                   # x 27..26 for north facing (mirrored)
model("control_console", 36, G + 2, 41, "north")
model("generator", 20, G + 1, 27, "east"); model("generator", 42, G + 1, 27, "west")
model("transformer", 19, G + 1, 40, "east") if False else None
for i in range(19, 44):
    put(18, 36, i, S("pipe", axis="z")); put(44, 36, i, S("pipe", axis="z"))
    put(i, 36, 18, S("pipe", axis="x")); put(i, 36, 44, S("pipe", axis="x"))
for (x, z) in ((18, 25), (18, 37), (44, 25), (44, 37), (25, 18), (37, 18)):
    for y in range(G + 1, 36):
        put(x, y, z, S("pipe", axis="y"))
model("server_rack", 19, G + 1, 44, "north"); model("server_rack", 20, G + 1, 44, "north")
model("server_rack", 42, G + 1, 44, "north"); model("server_rack", 43, G + 1, 44, "north")
for (x, z) in ((18, 18), (44, 18), (18, 22), (44, 22)):
    model("specimen_tank", x, G + 1, z, "east" if x < 30 else "west", broken=rng.random() < 0.5)
model("control_console", 23, G + 1, 18, "south"); model("control_console", 40, G + 1, 18, "south")
model("transformer", 18, G + 1, 30, "east") if False else None
model("electrical_cabinet", 18, G + 1, 35, "east"); model("electrical_cabinet", 18, G + 1, 36, "east")
model("electrical_cabinet", 44, G + 1, 35, "west"); model("electrical_cabinet", 44, G + 1, 36, "west")
model("office_chair", 31, G + 1, 39, "north") if False else None   # operator stands here
# hall lamps on the gallery ceiling + hanging over the shaft (dead)
for i in range(20, 43, 4):
    for (x, z) in ((i, 18), (i, 44), (18, i), (44, i)):
        put(x, 37, z, S("lab_lamp", lit=False))

# ---------------------------------------------------------------- west wing: electrical dept (stone 3)
for z in range(14, 49, 2):
    model("electrical_cabinet", 1, G + 1, z, "east")
    if z % 4 == 0 and z < 46:
        model("electrical_cabinet", 1, G + 1, z + 1, "east")
model("transformer", 6, G + 1, 20, "south"); model("transformer", 6, G + 1, 26, "south"); model("transformer", 6, G + 1, 32, "south")
model("generator", 6, G + 1, 41, "south")
model("control_console", 9, G + 1, 52, "north"); model("lab_table", 4, G + 1, 56, "south")
model("office_chair", 5, G + 1, 55, "north"); model("locker", 1, G + 1, 60, "east"); model("locker", 1, G + 1, 59, "east")
crate(10, G + 1, 60, "north", True, STONE)                          # stone 3
crate(11, G + 1, 60, "north", True)
for z in range(14, 62):
    put(11, 36, z, S("pipe", axis="z"))
for z in range(16, 60, 6):
    put(6, 37, z, S("lab_lamp", lit=False))

# ---------------------------------------------------------------- east wing: tech dept (stone 2)
for z in (15, 19, 23, 27):
    model("lathe", 52, G + 1, z, "south")
    model("milling_machine", 58, G + 1, z, "west")
for z in range(32, 47, 2):
    model("specimen_tank", 61, G + 1, z, "west", broken=rng.random() < 0.4)
    model("specimen_tank", 55, G + 1, z, "east", broken=rng.random() < 0.4) if z % 4 == 0 else None
model("lab_table", 52, G + 1, 52, "south"); model("lab_table", 56, G + 1, 52, "south")
model("shelf", 59, G + 1, 60, "north"); model("shelf", 52, G + 1, 60, "north")
model("control_console", 55, G + 1, 56, "north")
crate(51, G + 1, 47, "east", True, STONE)                           # stone 2
for z in range(14, 62):
    put(51, 36, z, S("pipe", axis="z"))
for z in range(16, 60, 6):
    put(56, 37, z, S("lab_lamp", lit=False))

# ---------------------------------------------------------------- north labs (ground)
for x in range(15, 48, 5):
    if x in (25, 35):
        continue
    model("lab_table", x, G + 1, 6, "south")
    model("office_chair", x, G + 1, 7, "north")
for x in range(14, 49, 3):
    if x in (23, 26, 35, 38):
        continue
    model("shelf", x, G + 1, 1, "south") if x % 2 == 0 else model("specimen_tank", x, G + 1, 1, "south", broken=rng.random() < 0.5)
model("control_console", 29, G + 1, 10, "north"); model("control_console", 33, G + 1, 10, "north")
for x in range(16, 48, 6):
    put(x, 37, 6, S("lab_lamp", lit=False))

# ---------------------------------------------------------------- south lobby (ground)
model("security_desk", 31, G + 1, 55, "south")
model("office_chair", 31, G + 1, 54, "south") if False else None
for x in (15, 47):
    model("locker", x, G + 1, 51, "south"); model("locker", x + (1 if x < 30 else -1), G + 1, 51, "south")
for x in (18, 22, 40, 44):
    model("lab_table", x, G + 1, 59, "north")
for x in range(16, 48, 6):
    put(x, 37, 56, S("lab_lamp", lit=False))
put(31, 37, 58, S("emergency_lamp", lit=True))

# ---------------------------------------------------------------- 2nd floor
L2 = F2 + 1
# security room above the lobby (stone 4)
wall_x(24, 51, 61, L2, 42, 40); wall_x(38, 51, 61, L2, 42, 40)
opening(24, 54, 24, 55, L2)
model("security_desk", 31, L2, 53, "south")
for x in (26, 36):
    model("server_rack", x, L2, 61, "north"); model("server_rack", x + 1, L2, 61, "north")
model("locker", 37, L2, 58, "west"); model("locker", 37, L2, 57, "west")
model("office_chair", 31, L2, 54, "north")
crate(25, L2, 61, "north", True, STONE)                             # stone 4
put(31, 43, 56, S("emergency_lamp", lit=True))
# offices (north)
for x in range(15, 48, 6):
    model("lab_table", x, L2, 3, "south"); model("office_chair", x, L2, 4, "north")
for x in (20, 26, 32, 38, 44):
    wall_x(x, 1, 8, L2, 42) if x not in (26, 38) else None
for x in (21, 33, 45):
    model("locker", x, L2, 1, "south")
# server room (east wing)
for z in range(15, 47, 2):
    model("server_rack", 52, L2, z, "east"); model("server_rack", 57, L2, z, "west")
model("control_console", 54, L2, 55, "north")
# archive (west wing)
for z in range(15, 50, 4):
    model("shelf", 2, L2, z, "east") if False else None
for z in (15, 21, 27, 33, 39, 45):
    model("shelf", 4, L2, z, "south"); model("shelf", 8, L2, z, "south")
crate(2, L2, 55, "south"); crate(3, L2, 55, "south", True)
for (x0, x1) in ((14, 48),):
    for x in range(x0, x1, 6):
        put(x, 43, 14, S("lab_lamp", lit=False)); put(x, 43, 48, S("lab_lamp", lit=False))

# ---------------------------------------------------------------- forecourt + canopy (z 63..69)
fill(0, G - 1, 63, W - 1, G - 1, D - 1, S("lab_concrete"))
fill(0, G, 63, W - 1, G, D - 1, concrete)
for x in range(22, 41):
    for z in range(63, D):
        put(x, G, z, tile() if 24 <= x <= 38 else S("hazard_block_worn") if z % 2 else concrete())
for x in range(24, 39):
    for z in range(63, 68):
        put(x, 35, z, S("lab_concrete"))
    put(x, 36, 67, S("lab_concrete_slab", type="bottom")); put(x, 34, 67, S("lab_panel_stripe_dirty"))
for z in range(63, 68):
    put(24, 36, z, S("lab_concrete_slab", type="bottom")); put(38, 36, z, S("lab_concrete_slab", type="bottom"))
for (x, z) in ((24, 67), (38, 67)):
    for y in range(G + 1, 35):
        put(x, y, z, S("lab_concrete"))
for x in (27, 31, 35):
    put(x, 35, 65, S("lab_lamp", lit=False))
for x in (28, 34):
    put(x, G + 1, 68, S("emergency_lamp", lit=False)); put(x, G + 2, 68, S("lab_concrete_slab", type="bottom"))
# roof machinery
for (k, x, z, f) in (("generator", 8, 9, "south"), ("generator", 54, 9, "south"), ("transformer", 8, 52, "north"),
                     ("transformer", 53, 52, "north"), ("server_rack", 30, 8, "south"), ("server_rack", 32, 8, "south")):
    model(k, x, ROOF + 1, z, f)
for (x, z) in ((14, 30), (48, 30), (31, 52)):
    fill(x, ROOF + 1, z, x + 1, ROOF + 2, z + 1, S("lab_panel_stripe_dirty"))
    put(x, ROOF + 3, z, S("metal_grate")); put(x + 1, ROOF + 3, z, S("metal_grate")); put(x, ROOF + 3, z + 1, S("metal_grate")); put(x + 1, ROOF + 3, z + 1, S("metal_grate"))
for z in range(10, 53):
    put(12, ROOF + 1, z, S("pipe", axis="z")); put(50, ROOF + 1, z, S("pipe", axis="z"))

# ---------------------------------------------------------------- ruin dressing


def solid(st):
    n = st[0]
    return n != "minecraft:air" and not any(k in n for k in ("glass", "grate", "debris", "moss", "vines", "part", "pipe", "stairs", "slab", "lamp")) \
        and not n.split(":")[1] in KINDS


for (x, y, z), st in list(B.items()):
    if st != AIR or (x, y, z) in MODEL_CELLS:
        continue
    below = get(x, y - 1, z)
    if solid(below) and y in (BAS + 1, G + 1, F2 + 1):
        r = rng.random()
        if r < 0.05:
            put(x, y, z, S("debris"))
        elif r < 0.085 and y != F2 + 1:
            put(x, y, z, S("dead_moss"))
    elif y in range(BAS + 2, 44) and rng.random() < 0.03:
        faces = {d: solid(get(x + dx, y, z + dz)) for d, dx, dz in (("north", 0, -1), ("south", 0, 1), ("west", -1, 0), ("east", 1, 0))}
        if any(faces.values()) and get(x, y - 1, z)[0] == "minecraft:air":
            up = solid(get(x, y + 1, z))
            put(x, y, z, S("dead_vines", up=up, **faces))
            yy = y - 1
            while yy > y - 4 and get(x, yy, z) == AIR and rng.random() < 0.7:
                put(x, yy, z, S("dead_vines", up=False, **faces)); yy -= 1

# ---------------------------------------------------------------- NBT writer
TAG = {"b": 1, "s": 2, "i": 3, "l": 4, "f": 5, "d": 6, "str": 8, "list": 9, "comp": 10}


def tag_type(v):
    if isinstance(v, tuple):
        return TAG[v[0]]
    if isinstance(v, bool):
        return 1
    if isinstance(v, int):
        return 3
    if isinstance(v, float):
        return 6
    if isinstance(v, str):
        return 8
    if isinstance(v, list):
        return 9
    if isinstance(v, dict):
        return 10
    raise TypeError(v)


def wpayload(o, v):
    t = tag_type(v)
    if isinstance(v, tuple) and v[0] in "bsilfd":
        v = v[1]
    if t == 1:
        o.write(struct.pack(">b", int(v)))
    elif t == 2:
        o.write(struct.pack(">h", v))
    elif t == 3:
        o.write(struct.pack(">i", v))
    elif t == 4:
        o.write(struct.pack(">q", v))
    elif t == 5:
        o.write(struct.pack(">f", v))
    elif t == 6:
        o.write(struct.pack(">d", v))
    elif t == 8:
        b = v.encode("utf-8"); o.write(struct.pack(">H", len(b))); o.write(b)
    elif t == 9:
        et = tag_type(v[0]) if v else 0
        o.write(struct.pack(">bi", et, len(v)))
        for e in v:
            wpayload(o, e)
    elif t == 10:
        for k, e in v.items():
            o.write(struct.pack(">b", tag_type(e))); wstr(o, k); wpayload(o, e)
        o.write(b"\x00")


def wstr(o, s):
    b = s.encode("utf-8"); o.write(struct.pack(">H", len(b))); o.write(b)


palette, pidx = [], {}
blocks = []
for pos in sorted(B, key=lambda p: (p[1], p[2], p[0])):
    st = B[pos]
    if st not in pidx:
        pidx[st] = len(palette)
        e = {"Name": st[0]}
        if st[1]:
            e["Properties"] = {k: v for k, v in st[1]}
        palette.append(e)
    be = {"pos": list(pos), "state": pidx[st]}
    if pos in NBT:
        nb = dict(NBT[pos])
        if "LootTableSeed" in nb:
            nb["LootTableSeed"] = ("l", nb["LootTableSeed"])
        be["nbt"] = nb
    blocks.append(be)
root = {"DataVersion": 3465, "size": [W, H, D], "palette": palette, "blocks": blocks, "entities": []}
buf = io.BytesIO()
buf.write(struct.pack(">b", 10)); wstr(buf, ""); wpayload(buf, root)
os.makedirs(OUT + "/structures", exist_ok=True)
with gzip.open(OUT + "/structures/laboratory.nbt", "wb") as f:
    f.write(buf.getvalue())


# ---------------------------------------------------------------- worldgen JSON
def wj(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)


wj(OUT + "/worldgen/template_pool/laboratory/start.json", {"name": "riftworld:laboratory/start", "fallback": "minecraft:empty", "elements": [
    {"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": "riftworld:laboratory",
                              "processors": "minecraft:empty", "projection": "rigid"}}]})
wj(OUT + "/worldgen/structure/laboratory.json", {"type": "minecraft:jigsaw", "biomes": "#riftworld:has_laboratory", "step": "surface_structures",
    "spawn_overrides": {}, "terrain_adaptation": "beard_box", "start_pool": "riftworld:laboratory/start", "size": 1,
    "start_height": {"absolute": -G}, "project_start_to_heightmap": "WORLD_SURFACE_WG", "max_distance_from_center": 80, "use_expansion_hack": False})
wj(OUT + "/worldgen/structure_set/laboratory.json", {"structures": [{"structure": "riftworld:laboratory", "weight": 1}],
    "placement": {"type": "minecraft:random_spread", "spacing": 48, "separation": 24, "salt": 718204551}})
wj(OUT + "/tags/worldgen/biome/has_laboratory.json", {"replace": False, "values": [
    "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:forest", "minecraft:birch_forest",
    "minecraft:taiga", "minecraft:snowy_plains", "minecraft:savanna", "minecraft:desert"]})
cnt = {}
for st in B.values():
    cnt[st[0]] = cnt.get(st[0], 0) + 1
print("lab OK blocks=%d palette=%d nbt=%d bytes=%d" % (len(B), len(palette), len(NBT), os.path.getsize(OUT + "/structures/laboratory.nbt")))
print(sorted(((v, k) for k, v in cnt.items() if "air" not in k), reverse=True)[:12])
