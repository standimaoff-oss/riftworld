# Rev 3 / C-2: furnishing of every castle room.  exec()'d inside gen_castle.py (shares its namespace:
# B, put, clear, fill, model, rot, KINDS, S, PL, ASH, FENCE, FY, brick, stairs, beam, rubble, h ...).
# Conventions: ground storey y1..5 (wing joists at y5 on rows z = z0+3+3k), upper floor y6, upper rooms y7..12.
# A prop "faces" the side its front looks at; wall props are placed in the air cell in front of the wall.

NBT = {}
LIT = {"castle_chandelier", "wall_sconce", "feast_table", "long_table", "chapel_altar", "hearth_cauldron", "alchemy_table",
       "candle_stand"}
SKIPPED = []
PLACED = []
BOOKS = S("castle_bookshelf", axis="y")
BARS = S("castle_bars")
CARPET = S("castle_carpet")
WEB = S("castle_cobweb")
SOFT = (CARPET[0], WEB[0])


def cells(kind, x, y, z, f):
    x0, x1, back, hh = KINDS[kind]
    return [(x + a, y + b, z + c) for (a, b, c) in (rot(i, j, -k, f) for i in range(x0, x1 + 1) for k in range(back + 1) for j in range(hh))]


def place(kind, x, y, z, f, lit=True, loot=None, topper=None):
    cs = cells(kind, x, y, z, f)
    bad = [c for c in cs if c in MODEL_CELLS or (c in B and B[c] != AIR and B[c][0] not in SOFT)]
    if bad:
        SKIPPED.append((kind, (x, y, z), bad[:2]))
        return False
    model(kind, x, y, z, f, powered=(lit and kind in LIT))
    if topper:
        model(topper, x, y, z, f, powered=(lit and topper in LIT), overlay=True)
        PLACED.append(topper)
    if loot:
        NBT[(x, y, z)] = {"LootTable": "riftworld:chests/castle_" + loot}
    PLACED.append(kind)
    return True


def chest(x, y, z, f, loot):
    return place("castle_chest", x, y, z, f, loot=loot)


def carpet(x0, z0, x1, z1, y):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if (x, y, z) not in B and (x, y - 1, z) in B:
                put(x, y, z, CARPET)


def web(*pts):
    for (x, y, z) in pts:
        if (x, y, z) not in B:
            put(x, y, z, WEB)


def shelves(x0, z0, x1, z1, y0, y1):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            for y in range(y0, y1 + 1):
                if (x, y, z) not in B:
                    put(x, y, z, BOOKS)


def tower_door(cx, cz, r, tx, tz, y0=1, y1=3):
    """Cut a 2-wide doorway through a tower ring along the ray centre -> (tx, tz)."""
    L = math.hypot(tx - cx, tz - cz)
    ux, uz = (tx - cx) / L, (tz - cz) / L
    for x in range(int(cx - r - 2), int(cx + r + 3)):
        for z in range(int(cz - r - 2), int(cz + r + 3)):
            dx, dz = x - cx, z - cz
            t = dx * ux + dz * uz
            p = abs(-dx * uz + dz * ux)
            if p <= 1.15 and r - 2.6 <= t <= r + 1.2:
                for y in range(y0, y1 + 1):
                    clear(x, y, z)
                if (x, y1 + 1, z) in B:
                    put(x, y1 + 1, z, ASH)


# ============================================================ connections / new stairs and doors
# E wing: the chapel is double height, so its upper doors (x58..59) opened into the void -> choir gallery along the hall wall
fill(53, FY, 29, 55, FY, 46, PL)
for z in range(29, 47):
    put(56, FY + 1, z, FENCE)
    put(56, FY, z, beam("z"))
for z in (30, 36, 42):
    for y in range(1, FY):
        put(55, y, z, beam("y"))
for zp in (28, 47):
    for x in (53, 54):
        for y in range(FY + 1, FY + 4):
            clear(x, y, zp)
        put(x, FY + 4, zp, ASH)
    for x in (58, 59):
        for y in range(FY + 1, FY + 4):
            put(x, y, zp, brick(x, y, zp))

# north range: stair along the north wall (x28..33, z59..60) rising east to the upper floor
for k in range(6):
    x = 28 + k
    for z in (59, 60):
        put(x, 1 + k, z, stairs("dark_planks", "east"))
        for y in range(1, 1 + k):
            put(x, y, z, PL)
        if k < 5:
            clear(x, FY, z)
for x in range(28, 33):
    put(x, FY + 1, 58, FENCE)

# gatehouse: its back wall (z12..13) sealed the gate passage off from the hall - open it (pointed arch like the front)
opening("x", None, 36, 40, 1, 7, [12, 13])
# stair in the east bay (x42..43) rising north from z11 (y1) to the guard room floor (y7) at z5
for k in range(6):
    z = 11 - k
    for x in (42, 43):
        put(x, 1 + k, z, stairs("dark_planks", "north"))
        for y in range(1, 1 + k):
            put(x, y, z, PL)
        for y in range(2 + k, 8):
            clear(x, y, z)
for x in (42, 43):
    put(x, 7, 5, PL)
    for y in (8, 9, 10):
        clear(x, y, 5)
        clear(x, y, 6)
for z in range(6, 12):
    put(41, 8, z, FENCE)
for x in range(33, 44):
    for z in range(5, 12):
        if (x, 7, z) not in B and not (x in (42, 43) and z >= 6):
            put(x, 7, z, PL)

# tower doors (ground level)
tower_door(5, 61, 6, 13, 55)           # NW prison <- kitchen
tower_door(5, 10, 5, 10, 14)           # SW watch tower <- armoury
for z in (56, 57):                      # NE keep <- treasury
    for x in (63, 64):
        for y in (1, 2, 3):
            clear(x, y, z)
        put(x, 4, z, ASH)
for z in (5, 6):                        # SE ruin: a breach in its west face
    for x in (66, 67):
        for y in (1, 2, 3):
            clear(x, y, z)

# ============================================================ MAIN HALL (kept clear for the fight; only wall hangings)
for (x, z, f) in ((26, 38, "east"), (26, 44, "east"), (50, 38, "west"), (50, 44, "west")):
    place("tapestry_horn", x, 1, z, f, lit=False)

# ============================================================ WEST WING - ground floor
# W-G1 armoury (z12..27): weapon racks, armour stands, a repair table, chests, two chandeliers
for zc in (17, 21, 26):
    place("guard_weapon_rack", 6, 1, zc, "east")
place("guard_weapon_rack", 23, 1, 21, "west")
for zc in (13, 17, 25):
    place("guard_armor", 23, 1, zc, "west")
place("long_table", 15, 1, 20, "east")
place("castle_bench", 13, 1, 20, "east")
place("castle_bench", 17, 1, 20, "west")
chest(11, 1, 12, "south", "armory")
chest(19, 1, 12, "south", "armory")
place("castle_chandelier", 15, 5, 17, "south")
place("castle_chandelier", 15, 5, 24, "south")
place("tapestry_horn", 16, 1, 27, "north")

# W-G2 refectory (z29..44): two rows of feast tables with benches, high chairs at the head, kegs, sideboard
for zc in (34, 37, 40):
    for xc in (11, 18):
        place("feast_table", xc, 1, zc, "east")
        place("castle_bench", xc - 1, 1, zc, "east")
        place("castle_bench", xc + 1, 1, zc, "west")
place("high_chair", 11, 1, 43, "north")
place("high_chair", 18, 1, 43, "north")
place("castle_chandelier", 14, 5, 33, "south")
place("castle_chandelier", 14, 5, 39, "south")
place("tapestry_horn", 6, 1, 37, "east")
place("tapestry_horn", 6, 1, 41, "east")
place("keg_rack", 23, 1, 37, "west")
place("keg_rack", 23, 1, 40, "west")
place("kitchen_counter", 23, 1, 43, "west", topper="kitchen_set")
place("dish_cupboard", 23, 1, 35, "west")

# W-G3 kitchen (z46..60): cauldron over the fire, pot, firewood, work table with hanging herbs, counters, sacks, kegs
place("hearth_cauldron", 11, 1, 52, "south")
place("cooking_pot", 8, 1, 48, "east")
for (x, z) in ((6, 47), (6, 48), (7, 47)):
    place("firewood_stack", x, 1, z, "east")
place("long_table", 17, 1, 52, "east")
place("herbs_hanging", 16, 4, 52, "east")
place("herbs_hanging", 18, 4, 52, "west")
for z in (48, 49, 50):
    place("kitchen_counter", 23, 1, z, "west", topper="kitchen_set")
place("keg_rack", 23, 1, 55, "west")
for (x, z) in ((23, 58), (23, 59), (22, 59)):
    place("sacks", x, 1, z, "west")
chest(23, 1, 60, "west", "kitchen")
place("wall_sconce", 15, 3, 60, "north")

# ============================================================ WEST WING - upper floor
# W-U1 barracks (z12..19): cots along the walls, weapon rack, chest, candle stand
for x in (14, 16, 18):
    place("bed_wood", x, 7, 13, "south")
for z in (14, 16):
    place("bed_wood", 22, 7, z, "west")
for x in (15, 17):
    place("bed_wood", x, 7, 18, "north")
place("guard_weapon_rack", 21, 7, 19, "north")
chest(10, 7, 16, "east", "barracks")
place("candle_stand", 13, 7, 16, "south")
web((22, 11, 12), (23, 12, 12))

# W-U2 lord's bedchamber (z21..37): canopy bed, carpet, chests, tapestries, sconces, candle stand, bookcases
carpet(14, 25, 20, 32, 7)
place("canopy_bed", 21, 7, 28, "west")
chest(22, 7, 31, "west", "bedchamber")
chest(22, 7, 26, "west", "bedchamber")
place("high_chair", 15, 7, 22, "south")
place("table_wood", 17, 7, 22, "south")
place("tapestry_horn", 6, 7, 26, "east")
place("tapestry_horn", 6, 7, 36, "east")
place("wall_sconce", 23, 9, 25, "west")
place("wall_sconce", 23, 9, 33, "west")
place("candle_stand", 19, 7, 24, "south")
place("bookshelf_e", 16, 7, 37, "north")
place("bookshelf_e", 17, 7, 37, "north")

# W-U3 alchemist's laboratory (z39..51): benches with flasks, cauldron, book wall, lectern, bones, cobwebs
place("alchemy_table", 6, 7, 43, "east", topper="alchemy_set")
place("alchemy_table", 6, 7, 47, "east", topper="alchemy_set")
place("hearth_cauldron", 14, 7, 45, "south")
shelves(23, 40, 23, 50, 7, 9)
place("chained_lectern", 19, 7, 45, "west", topper="chained_books")
chest(7, 7, 50, "east", "alchemy")
place("bone_pile", 17, 7, 49, "south")
place("wall_sconce", 6, 9, 45, "east")
web((23, 10, 40), (23, 10, 41), (22, 11, 39), (6, 11, 50))

# W-U4 storage loft (z53..60): sacks, crates, firewood, kegs, chest
for (x, z) in ((20, 59), (21, 59), (22, 58)):
    place("sacks", x, 7, z, "north")
for (x, z) in ((22, 60), (23, 59), (23, 60)):
    place("crate_wood", x, 7, z, "west", loot="storeroom")
for (x, y, z) in ((14, 7, 60), (15, 7, 60), (14, 8, 60)):
    place("firewood_stack", x, y, z, "north")
place("keg_rack", 23, 7, 54, "west")
chest(18, 7, 60, "north", "storeroom")
web((23, 11, 53), (22, 12, 53), (15, 11, 60))

# ============================================================ EAST WING - ground floor
# E-G1 library (z12..27): book walls, two free-standing double shelves, chained lecterns, reading table, chandeliers
carpet(61, 12, 63, 27, 1)
shelves(53, 12, 53, 27, 1, 4)
shelves(54, 12, 59, 12, 1, 4)
shelves(55, 15, 59, 16, 1, 3)
shelves(55, 21, 59, 22, 1, 3)
shelves(64, 27, 68, 27, 1, 3)
place("chained_lectern", 65, 1, 15, "west", topper="chained_books")
place("chained_lectern", 65, 1, 21, "west", topper="chained_books")
place("long_table", 66, 1, 25, "north")
place("castle_bench", 66, 1, 24, "north")
chest(70, 1, 26, "west", "library")
place("castle_chandelier", 57, 5, 18, "south")
place("castle_chandelier", 62, 5, 24, "south")
web((53, 5, 12), (54, 5, 12))

# E-G2 chapel (z29..46, double height): pews, carpet aisle, altar with candles, tapestries, sconces
carpet(61, 29, 62, 43, 1)
for zc in (32, 34, 36, 38, 40):
    place("castle_bench", 58, 1, zc, "south")
    place("castle_bench", 65, 1, zc, "south")
place("chapel_altar", 62, 2, 45, "north")
place("candle_stand", 59, 2, 45, "north")
place("candle_stand", 65, 2, 45, "north")
place("tapestry_horn", 60, 2, 46, "north")
place("tapestry_horn", 64, 2, 46, "north")
for (x, z, f) in ((57, 29, "south"), (66, 29, "south"), (56, 46, "north"), (67, 46, "north")):
    place("wall_sconce", x, 4, z, f)
web((70, 11, 29), (70, 12, 29), (53, 11, 46))

# E-G3 treasury (z48..60, L-shaped around the keep): gold, chests, armour stands at the door, carpet, sconces
carpet(61, 48, 62, 58, 1)
place("guard_armor", 60, 1, 48, "south")
place("guard_armor", 63, 1, 48, "south")
for (x, z) in ((55, 58), (57, 59), (54, 55), (66, 49), (59, 60)):
    place("gold_hoard", x, 1, z, "south")
chest(56, 1, 60, "north", "treasury")
chest(53, 1, 52, "east", "treasury")
chest(69, 1, 50, "west", "treasury")
place("tapestry_horn", 53, 1, 58, "east")
place("wall_sconce", 62, 3, 54, "west")
place("wall_sconce", 53, 3, 50, "east")

# ============================================================ EAST WING - upper floor
# E-U1 scriptorium (z12..27): writing tables with chairs, book wall, lectern, chest, sconces
for (xc, zc) in ((58, 17), (64, 17), (58, 22), (64, 22)):
    place("long_table", xc, 7, zc, "north")
    place("high_chair", xc, 7, zc + 1, "north")
shelves(53, 12, 53, 25, 7, 9)
place("chained_lectern", 67, 7, 25, "west", topper="chained_books")
chest(70, 7, 26, "west", "library")
place("wall_sconce", 62, 9, 12, "south")
place("wall_sconce", 70, 9, 22, "west")
web((53, 11, 12), (54, 12, 12))

# E-U3 guest chamber (z48..60): canopy bed, chest, chair + table, carpet, tapestry, bookcases, sconces
carpet(55, 54, 61, 57, 7)
place("canopy_bed", 59, 7, 58, "north")
chest(57, 7, 60, "north", "bedchamber")
place("high_chair", 55, 7, 53, "east")
place("table_wood", 56, 7, 53, "east")
place("tapestry_horn", 53, 7, 57, "east")
place("bookshelf_e", 66, 7, 51, "north")
place("bookshelf_e", 67, 7, 51, "north")
place("wall_sconce", 62, 9, 55, "west")
place("candle_stand", 61, 7, 49, "south")

# ============================================================ NORTH RANGE
# N-G1 storeroom (x26..37)
for (x, z) in ((36, 52), (37, 52), (37, 53)):
    place("sacks", x, 1, z, "south")
for (x, z) in ((26, 53), (26, 54)):
    place("crate_wood", x, 1, z, "east", loot="storeroom")
for (x, z) in ((26, 60), (27, 60), (26, 59)):
    place("firewood_stack", x, 1, z, "north")
chest(35, 1, 60, "north", "storeroom")
place("wall_sconce", 32, 3, 52, "south")
web((26, 5, 52), (37, 5, 60))

# N-G2 wine cellar (x39..50)
for x in (40, 43, 46):
    place("keg_rack", x, 1, 52, "south")
for x in (41, 45, 49):
    place("keg_rack", x, 1, 60, "north")
place("long_table", 45, 1, 56, "north")
place("sacks", 50, 1, 56, "west")
chest(50, 1, 53, "west", "cellar")
web((50, 5, 60), (39, 5, 52), (50, 5, 52))

# N-U trophy gallery / guards' dormitory (y7..12)
for x in (36, 41, 46, 50):
    place("guard_armor", x, 7, 60, "north")
for xc in (31, 45):
    place("guard_weapon_rack", xc, 7, 51, "south")
for x in (35, 38, 41):
    place("tapestry_horn", x, 7, 51, "south")
place("bone_pile", 42, 7, 56, "south")
for z in (53, 55, 57):
    place("bed_wood", 27, 7, z, "east")
place("candle_stand", 34, 7, 56, "south")
place("candle_stand", 46, 7, 56, "south")
chest(50, 7, 55, "west", "barracks")

# ============================================================ GATEHOUSE guard room (y8..12)
place("long_table", 37, 8, 8, "north")
place("castle_bench", 37, 8, 7, "north")
place("castle_bench", 37, 8, 9, "south")
place("guard_weapon_rack", 33, 8, 9, "east")
chest(35, 8, 11, "north", "gatehouse")
place("guard_armor", 39, 8, 11, "north")
place("wall_sconce", 38, 10, 11, "north")
web((33, 12, 11), (43, 12, 11))

# ============================================================ TOWERS: spiral stairs + every storey furnished (C-2b)
def ring_path(interior, cx, cz, a_start, sign):
    """Ordered, closed, 4-connected ring of interior cells along the wall, from angle a_start (deg) in direction sign."""
    I = set(interior)
    R = [c for c in I if any((c[0] + dx, c[1] + dz) not in I for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))]
    ang = lambda c: math.degrees(math.atan2(c[1] - cz, c[0] - cx))
    R.sort(key=lambda c: ((ang(c) - a_start) * sign) % 360)
    out = []
    for c in R + [R[0]]:
        if out:
            p_ = out[-1]
            if abs(c[0] - p_[0]) == 1 and abs(c[1] - p_[1]) == 1:
                for m in ((c[0], p_[1]), (p_[0], c[1])):
                    if m in I and m not in out:
                        out.append(m)
                        break
        if c not in out:
            out.append(c)
    return out


DIRS = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}


def spiral(path, floors, ytop, mat="ashlar"):
    """Spiral stair along the tower wall: one riser per ring cell; solid masonry under each flight (built into the wall);
    the last tread of each flight lies in the floor plane above; headroom cut through the floors."""
    n = len(path)
    steps = []
    for k in range(ytop):
        y = 1 + k
        x, z = path[k % n]
        nx, nz = path[(k + 1) % n]
        base = max([f for f in [0] + floors if f < y])
        for yy in range(base + 1, y):
            put(x, yy, z, ASH)
        put(x, y, z, stairs(mat, DIRS[(nx - x, nz - z)]))
        steps.append((x, y, z))
    for (x, y, z) in steps:
        for yy in (y + 1, y + 2, y + 3):
            if (x, yy, z) not in steps and yy <= ytop:
                clear(x, yy, z)
    return steps


def interior_round(cx, cz, r):
    return [(x, z) for x in range(cx - 6, cx + 7) for z in range(cz - 6, cz + 7) if math.hypot(x - cx, z - cz) <= r]


def interior_oct(cx, cz, r):
    return [(x, z) for x in range(cx - 6, cx + 7) for z in range(cz - 6, cz + 7)
            if max(abs(x - cx), abs(z - cz), (abs(x - cx) + abs(z - cz)) * 0.7071) <= r]


def tower_lights(interior, cx, cz, floors, steps, n):
    """Wall sconces above head height (floor+3) on every storey, spread around the wall, clear of the stair headroom."""
    ins = set(interior)
    head = {(x, yy, z) for (x, y, z) in steps for yy in range(y - 1, y + 4)}
    for fy in floors:
        y = fy + 3
        cand = []
        for (x, z) in interior:
            if (x, y, z) in head or (x, y, z) in MODEL_CELLS or (B.get((x, y, z), AIR) != AIR):
                continue
            ax, az = cx - x, cz - z
            d = (1 if ax > 0 else -1, 0) if abs(ax) >= abs(az) else (0, 1 if az > 0 else -1)
            bx, bz = x - d[0], z - d[1]
            wb = B.get((bx, y, bz))
            if (bx, bz) in ins or wb is None or wb == AIR or "stairs" in wb[0]:
                continue
            cand.append((math.atan2(z - cz, x - cx), x, z, DIRS[d]))
        cand.sort()
        have = [c for c in MODEL_CELLS if c[1] == y and (c[0], c[2]) in ins]
        for i in range(n):
            if not cand:
                break
            a, x, z, f = cand[(i * len(cand)) // n]
            if any(abs(h[0] - x) + abs(h[2] - z) < 3 for h in have):
                continue
            if place("wall_sconce", x, y, z, f):
                have.append((x, y, z))


# ---- NW tower: prison (ground), jailers' room (y9), torture chamber (y17), watch room under the roof (y25)
NW_IN = interior_round(5, 61, 4.3)
NW_STEPS = spiral(ring_path(NW_IN, 5, 61, -60, -1), [8, 16, 24], 24)
for x in range(0, 11):                      # bars split the far half of the prison into two cells
    for y in range(1, 8):
        if (x, y, 61) not in B and (x, 61) in NW_IN:
            put(x, y, 61, BARS)
for z in range(62, 67):
    for y in range(1, 8):
        if (5, y, z) not in B and (5, z) in NW_IN:
            put(5, y, z, BARS)
place("wall_shackles", 2, 1, 64, "east")
place("wall_shackles", 8, 1, 64, "west")
place("bone_pile", 3, 1, 63, "south")
place("bone_pile", 7, 1, 62, "south")
chest(7, 1, 60, "west", "prison")
place("wall_sconce", 9, 3, 60, "west")
web((4, 7, 65), (6, 7, 65), (2, 7, 62), (8, 7, 63), (6, 7, 59))
# y9 jailers' room
place("table_wood", 5, 9, 60, "south")
place("stool", 4, 9, 61, "south")
place("stool", 7, 9, 60, "south")
place("guard_weapon_rack", 9, 9, 61, "west")
chest(6, 9, 64, "north", "prison")
place("wall_sconce", 5, 11, 65, "north")
place("bone_pile", 3, 9, 62, "south")
# y17 torture chamber: shackles, a cage of bars, bones, a hearth cauldron (glowing coals), cobwebs
for (x, z) in ((3, 60), (3, 61)):
    for y in range(17, 20):
        put(x, y, z, BARS)
place("wall_shackles", 2, 17, 61, "east")
place("hearth_cauldron", 5, 17, 61, "south")
place("bone_pile", 6, 17, 59, "south")
place("wall_sconce", 9, 19, 61, "west")
web((5, 23, 57), (2, 23, 63), (7, 22, 58))
# y25 watch room under the roof: armour, chest, bench, candles, tapestry
place("guard_armor", 5, 25, 64, "north")
chest(2, 25, 61, "east", "keep")
place("castle_bench", 5, 25, 58, "south")
place("candle_stand", 7, 25, 61, "south")
web((5, 33, 57), (8, 33, 64), (2, 34, 62))

# ---- SW watch tower (octagon, broken top): guard post (ground), store (y9), archers' room (y17), lookout landing y21
SW_IN = interior_oct(5, 10, 3.3)
fill(4, 21, 9, 6, 21, 11, PL)                # landing at the broken top
SW_STEPS = spiral(ring_path(SW_IN, 5, 10, 20, -1), [8, 16], 21)
for (x, z) in ((7, 11), (7, 12), (6, 12), (6, 13), (7, 10)):
    if (x, 21, z) not in B:
        put(x, 21, z, PL)
put(4, 22, 9, FENCE) if (4, 22, 9) not in B else None
place("guard_weapon_rack", 5, 1, 9, "south")
chest(2, 1, 10, "east", "keep")
place("bone_pile", 5, 1, 12, "south")
web((3, 7, 8), (4, 7, 13), (2, 6, 9))
place("sacks", 4, 9, 10, "south")
place("crate_stack", 5, 9, 11, "south")
place("crate_wood", 6, 9, 10, "south", loot="gatehouse")
place("wall_sconce", 5, 11, 13, "north")
place("guard_weapon_rack", 5, 17, 11, "north")
place("castle_bench", 5, 17, 9, "south")
chest(3, 17, 10, "east", "gatehouse")
web((5, 20, 13), (2, 19, 11))

# ---- NE keep: garrison store (ground), guards' hall (y9), keep armoury (y17), commander (y25), store loft (y33),
#      upper hall (y41) and the open crenellated roof (y44)
KEEP_IN = [(x, z) for x in range(65, 72) for z in range(54, 61)]
KEEP_STEPS = spiral(ring_path(KEEP_IN, 68, 57, 165, -1), [8, 16, 24, 32, 40, 44], 44)
# ground
chest(67, 1, 56, "south", "keep")
place("table_wood", 68, 1, 57, "south")
place("stool", 68, 1, 58, "south")
place("stool", 70, 1, 57, "south")
for (x, z) in ((66, 55), (66, 56)):
    place("sacks", x, 1, z, "south")
place("crate_stack", 70, 1, 55, "south")
place("crate_stack", 70, 2, 55, "south")
place("castle_chandelier", 68, 7, 57, "south")
# y9 guards' hall
for (x, z) in ((66, 55), (66, 57)):
    place("bed_wood", x + 1, 9, z, "east")
place("long_table", 69, 9, 57, "north")
place("castle_bench", 69, 9, 58, "north")
place("castle_chandelier", 68, 15, 57, "south")
chest(70, 9, 55, "west", "barracks")
# y17 keep armoury
place("guard_weapon_rack", 68, 17, 55, "south")
place("guard_weapon_rack", 68, 17, 59, "north")
place("guard_armor", 66, 17, 57, "east")
place("guard_armor", 70, 17, 57, "west")
chest(68, 17, 57, "south", "armory")
place("castle_chandelier", 68, 23, 56, "south")
# y25 commander's chamber
place("bed_wood", 67, 25, 56, "east")
place("table_wood", 69, 25, 57, "south")
place("high_chair", 69, 25, 58, "north")
place("chained_lectern", 67, 25, 59, "north", topper="chained_books")
chest(70, 25, 55, "west", "treasury")
place("castle_chandelier", 68, 31, 57, "south")
# y33 store loft
for (x, y, z) in ((66, 33, 55), (67, 33, 55), (66, 34, 55), (70, 33, 59), (70, 33, 58)):
    place("crate_stack", x, y, z, "south")
for (x, z) in ((66, 58), (67, 59)):
    place("sacks", x, 33, z, "south")
chest(70, 33, 55, "west", "storeroom")
web((66, 39, 55), (70, 39, 59), (67, 39, 59))
# y41 upper hall (low, under the roof platform)
place("guard_armor", 67, 41, 57, "east")
place("candle_stand", 70, 41, 57, "south")
chest(68, 41, 59, "north", "keep")
# roof: watch post
place("candle_stand", 69, 45, 56, "south")
place("guard_weapon_rack", 68, 45, 59, "north")

# ---- SE ruin: bones, a forgotten chest, cobwebs, rubble
place("bone_pile", 69, 1, 5, "south")
chest(72, 1, 7, "west", "prison")
web((68, 7, 4), (72, 7, 8), (68, 6, 8), (70, 7, 4))
rubble(71, 5, 2, 1, 61, 7)


# ============================================================ dressing pass (rooms are large - fill them more densely)
# armoury: armour row, smithing corner, tool wall, crate stacks
for z in (16, 20, 24):
    place("guard_armor", 10, 1, z, "east")
place("anvil_stump", 19, 1, 17, "south")
place("quench_barrel", 20, 1, 18, "south")
place("weapon_rack", 16, 1, 12, "south")
place("weapon_rack", 17, 1, 12, "south")
place("tool_wall", 21, 1, 12, "south")
for (x, y, z) in ((21, 1, 27), (22, 1, 27), (22, 2, 27)):
    place("crate_stack", x, y, z, "north")
# kitchen: big fireplace, stove, table with stools, more herbs, cupboards, crates
place("fireplace", 6, 1, 53, "east")
place("kitchen_stove", 6, 1, 50, "east")
place("table_wood", 14, 1, 57, "south")
for (x, z) in ((13, 57), (16, 57), (14, 58), (15, 56)):
    place("stool", x, 1, z, "south")
for (x, z) in ((23, 49), (22, 49), (11, 55), (12, 55), (20, 58)):
    place("herbs_hanging", x, 4, z, "west")
place("dish_cupboard", 16, 1, 60, "north")
place("dish_cupboard", 17, 1, 60, "north")
for (x, z) in ((21, 60), (22, 60)):
    place("sacks", x, 1, z, "north")
place("fish_barrel", 20, 1, 60, "north")
for (x, z) in ((23, 46), (23, 47)):
    place("crate_stack", x, 1, z, "west")
# barracks: table with stools, chests at the bed feet, wall rack, sconce
place("table_wood", 17, 7, 16, "south")
for (x, z) in ((16, 16), (19, 16), (17, 15), (18, 17)):
    place("stool", x, 7, z, "south")
chest(14, 7, 14, "south", "barracks")
chest(18, 7, 14, "south", "barracks")
place("weapon_rack", 10, 7, 12, "south")
place("wall_sconce", 20, 9, 12, "south")
# bedchamber: fireplace with two chairs, wardrobes, candles by the bed, bench at its foot, lectern, more hangings
place("fireplace", 6, 7, 23, "east")
place("high_chair", 9, 7, 22, "west")
place("high_chair", 9, 7, 25, "west")
place("stool", 9, 7, 23, "south")
place("wardrobe", 13, 7, 37, "north")
place("wardrobe", 14, 7, 37, "north")
place("candle_stand", 23, 7, 27, "west")
place("candle_stand", 23, 7, 30, "west")
place("castle_bench", 20, 7, 28, "west")
place("chained_lectern", 10, 7, 32, "east", topper="chained_books")
place("tapestry_horn", 17, 7, 21, "south")
place("tapestry_horn", 20, 7, 37, "north")
# alchemy: shelf row, third bench, work table, crates, candles, bones in the corner
shelves(15, 41, 20, 41, 7, 8)
place("alchemy_table", 15, 7, 39, "south", topper="alchemy_set")
place("long_table", 10, 7, 48, "east")
place("crate_wood", 23, 7, 51, "west", loot="alchemy")
place("sacks", 22, 7, 51, "north")
place("candle_stand", 9, 7, 41, "south")
place("candle_stand", 21, 7, 47, "south")
place("bone_pile", 8, 7, 40, "south")
# storage loft: crate stacks, sacks, kegs, firewood
for (x, y, z) in ((16, 7, 57), (17, 7, 57), (16, 8, 57), (17, 7, 58), (16, 7, 58)):
    place("crate_stack", x, y, z, "north")
for (x, y, z) in ((19, 7, 55), (20, 7, 55), (19, 8, 55), (20, 7, 56)):
    place("sacks", x, y, z, "north")
place("keg_rack", 21, 7, 53, "south")
for (x, z) in ((16, 60), (17, 60)):
    place("firewood_stack", x, 7, z, "north")
# treasury: more gold, hangings, candles
for (x, z) in ((53, 56), (54, 59), (56, 58), (58, 59), (67, 48), (68, 49), (70, 48), (55, 60), (57, 57)):
    place("gold_hoard", x, 1, z, "south")
place("tapestry_horn", 57, 1, 48, "south")
place("tapestry_horn", 65, 1, 48, "south")
place("candle_stand", 58, 1, 52, "south")
place("candle_stand", 58, 1, 55, "south")

# storeroom: crate stacks, more sacks, keg rack, barrel, wall shelf, herbs drying under the ceiling
for (x, y, z) in ((31, 1, 56), (32, 1, 56), (33, 1, 56), (31, 1, 57), (32, 1, 57), (31, 2, 56), (32, 2, 56)):
    place("crate_stack", x, y, z, "north")
for x in (34, 35):
    place("sacks", x, 1, 52, "south")
place("keg_rack", 26, 1, 56, "east")
place("fish_barrel", 27, 1, 57, "east")
place("shop_shelf", 31, 1, 52, "south")
for (x, z) in ((30, 53), (33, 53), (35, 58), (29, 56)):
    place("herbs_hanging", x, 5, z, "south")
# guest chamber: fireplace, wardrobes, candle and bench by the bed, a small table in the side bay
place("fireplace", 53, 7, 51, "east")
place("wardrobe", 61, 7, 60, "north")
place("wardrobe", 62, 7, 60, "north")
place("candle_stand", 57, 7, 58, "north")
place("castle_bench", 58, 7, 57, "north")
place("table_wood", 68, 7, 49, "south")
place("stool", 67, 7, 49, "south")
place("stool", 70, 7, 49, "south")
place("crate_wood", 70, 7, 51, "west", loot="storeroom")

tower_lights(NW_IN, 5, 61, [0, 8, 16, 24], NW_STEPS, 3)
tower_lights(SW_IN, 5, 10, [0, 8, 16], SW_STEPS, 2)
tower_lights(KEEP_IN, 68, 57, [0, 8, 16, 24, 32, 40], KEEP_STEPS, 3)
# headroom check on all spiral treads
for name_, st_ in (("NW", NW_STEPS), ("SW", SW_STEPS), ("KEEP", KEEP_STEPS)):
    for (x, y, z) in st_:
        for yy in (y + 1, y + 2):
            b_ = B.get((x, yy, z))
            if b_ is not None and b_ != AIR and "stairs" not in b_[0] and b_[0] not in SOFT:
                print("  HEADROOM", name_, (x, y, z), yy, b_[0])
print("rooms: placed", len(PLACED), "skipped", len(SKIPPED))
for s_ in SKIPPED:
    print("  SKIP", s_)
