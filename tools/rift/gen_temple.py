"""The Great Temple of the Crossing: a custom-block, ivory-and-gold cathedral replacing the Elisar laboratory.

Its nave, raised apse, twin spires, lantern tower, reliquary, archive and living chapel surround the candle ritual.
Three connected underground maze floors hold custom flora, traps, light-crystal galleries, treasure caches and a final
reliquary. No vanilla block is used for construction (minecraft:air is only used to carve the rooms).

Template frame: x -> east, z -> south, front porch at high z (face = south), ground floor at y = 56.
"""
import json, os, re, sys

HERE = os.path.dirname(os.path.abspath(__file__))
src = open(os.path.join(HERE, "gen_castle.py"), encoding="utf-8").read()
exec(src[:src.index("# ================================================================== foundation / floors")])
rooms = open(os.path.join(HERE, "castle_rooms.py"), encoding="utf-8").read()
exec(rooms[:rooms.index("def tower_door")])

OUT = MOD + "/src/main/resources/data/riftworld/structures/city"
os.makedirs(OUT, exist_ok=True)
W, D, H = 63, 70, 96
G = 56

TB = S("temple_bricks")
TG = S("temple_gold")
TF = S("temple_floor")
TP = lambda a: S("temple_pillar", axis=a)
MOS_T = S("temple_mosaic", stage=0)
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

# ------------------------------------------------------------------ side chapels: west living garden, east archive + reliquary
walls(50, 23, 61, 41, G + 1, G + 10, t=1, mat=lambda x, y, z: TB)
fill(51, G, 24, 60, G, 40, TF)
for z in range(26, 40, 4):
    lancet("z", z, G + 3, 3, 61, [])
walls(1, 23, 12, 41, G + 1, G + 10, t=1, mat=lambda x, y, z: TB)
fill(2, G, 24, 11, G, 40, TF)
for z in range(26, 40, 4):
    lancet("z", z, G + 3, 3, 1, [])
opening("z", 0, 31, 33, G + 1, G + 4, [48, 49, 50])                # nave -> east chapel
opening("z", 0, 31, 33, G + 1, G + 4, [12, 13, 14])                # nave -> west chapel
# Low cross-walls split each wing into a north archive / garden and a south reliquary / root shrine.
fill(51, G + 1, 32, 60, G + 6, 32, TB); fill(54, G + 1, 32, 57, G + 4, 32, AIR)
fill(2, G + 1, 32, 11, G + 6, 32, TB); fill(6, G + 1, 32, 7, G + 4, 32, AIR)
for x in (53, 58): put(x, G + 7, 32, TG)
for x in (4, 9): put(x, G + 7, 32, TG)
# East: chained archive in the north room, gilded reliquary and three caches in the south room.
model("chained_lectern", 55, G + 1, 28, "west")
model("chained_books", 55, G + 1, 28, "west", overlay=True)
model("candle_stand", 58, G + 1, 26, "east", powered=True)
model("gold_hoard", 53, G + 1, 36, "south", powered=False)
chest(57, G + 1, 36, "west", "temple_surface")
chest(55, G + 1, 38, "north", "temple_surface")
model("candle_stand", 58, G + 1, 38, "east", powered=True)
put(56, G + 2, 26, S("temple_lumen_crystal"))
# West: planted floor, a ring of custom ferns and blooms, and a small root shrine.
fill(3, G, 25, 10, G, 30, S("temple_garden_soil"))
fill(3, G, 34, 10, G, 39, S("temple_garden_soil"))
for x, z in ((4, 26), (6, 26), (8, 27), (10, 26), (4, 29), (9, 29), (5, 35), (9, 35), (4, 38), (10, 38)):
    put(x, G + 1, z, S("temple_fern"))
for x, z in ((6, 28), (8, 29), (4, 35), (7, 36), (10, 36), (6, 38)):
    put(x, G + 1, z, S("temple_bloom"))
for x, z in ((3, 27), (3, 28), (10, 28), (10, 29), (3, 36), (3, 37), (10, 37)):
    put(x, G + 1, z, S("temple_root_vine"))
fill(6, G + 1, 34, 8, G + 1, 36, TB)
put(7, G + 2, 35, S("temple_lumen_crystal"))
model("castle_bench", 4, G + 1, 25, "east")
model("castle_bench", 4, G + 1, 39, "east")
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

# ------------------------------------------------------------------ the three-level undercroft: 8x8 connected mazes, traps and relic vaults
MAZE_N = 8
MAZE_PITCH = 7
MAZE_FLOORS = (G - 8, G - 20, G - 32)
MAZE_X0, MAZE_Z0 = 6, 7
MAZE_MIN_X, MAZE_MAX_X = 4, 58
MAZE_MIN_Z, MAZE_MAX_Z = 5, 64
UNDER = S("temple_understone")
MAZE_TILE = S("temple_maze_floor")
TRAP_K = S("temple_trap_plate")
SOIL = S("temple_garden_soil")
MOSS = S("temple_moss")
FERN = S("temple_fern")
BLOOM = S("temple_bloom")
ROOT = S("temple_root_vine")
CRYSTAL = S("temple_lumen_crystal")


def cell_center(cell):
    return MAZE_X0 + cell[0] * MAZE_PITCH, MAZE_Z0 + cell[1] * MAZE_PITCH


def maze_edges(seed, start):
    rng = random.Random(seed)
    seen, stack, edges = {start}, [start], set()
    while stack:
        x, z = stack[-1]
        opts = [(x + dx, z + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))
                if 0 <= x + dx < MAZE_N and 0 <= z + dz < MAZE_N and (x + dx, z + dz) not in seen]
        if not opts:
            stack.pop()
            continue
        nxt = rng.choice(opts)
        seen.add(nxt); stack.append(nxt)
        edges.add(tuple(sorted(((x, z), nxt))))
    # A few extra doors make the maze explore like a place, not a single forced corridor.
    for z in range(MAZE_N):
        for x in range(MAZE_N):
            for q in ((x + 1, z), (x, z + 1)):
                if q[0] < MAZE_N and q[1] < MAZE_N and rng.random() < 0.16:
                    edges.add(tuple(sorted(((x, z), q))))
    return edges


def carve_room(floor_y, cell, radius=1):
    cx, cz = cell_center(cell)
    fill(cx - radius, floor_y + 1, cz - radius, cx + radius, floor_y + 5, cz + radius, AIR)
    fill(cx - radius, floor_y, cz - radius, cx + radius, floor_y, cz + radius, MAZE_TILE)


def carve_passage(floor_y, a, b):
    ax, az = cell_center(a); bx, bz = cell_center(b)
    if ax != bx:
        fill(min(ax, bx) + 2, floor_y + 1, az - 1, max(ax, bx) - 2, floor_y + 5, az + 1, AIR)
    else:
        fill(ax - 1, floor_y + 1, min(az, bz) + 2, ax + 1, floor_y + 5, max(az, bz) - 2, AIR)


def carve_spiral_stair(upper_y, lower_y, cell):
    """An enclosed 5x5 spiral stair connects adjacent floors without a fall shaft."""
    cx, cz = cell_center(cell)
    lo, hi = min(lower_y, upper_y), max(lower_y, upper_y)
    fill(cx - 2, lo + 1, cz - 2, cx + 2, hi + 5, cz + 2, AIR)
    # The solid central pier leaves a safe inner rail and a glowing way-point.
    fill(cx - 1, lo + 6, cz - 1, cx + 1, hi - 1, cz + 1, UNDER)
    put(cx, lo + 6, cz, CRYSTAL)
    ring = []
    ring.extend((cx + dx, cz - 2) for dx in range(-2, 3))
    ring.extend((cx + 2, cz + dz) for dz in range(-1, 3))
    ring.extend((cx + dx, cz + 2) for dx in range(1, -3, -1))
    ring.extend((cx - 2, cz + dz) for dz in range(1, -2, -1))
    assert len(ring) == 16
    drop = hi - lo
    for i, (x, z) in enumerate(ring):
        y = hi - (i * drop // len(ring))
        nxt = ring[(i + 1) % len(ring)]
        dx, dz = nxt[0] - x, nxt[1] - z
        facing = "east" if dx > 0 else "west" if dx < 0 else "south" if dz > 0 else "north"
        put(x, y, z, stairs("temple_bricks", facing))
        for yy in range(y + 1, min(hi + 5, y + 5) + 1):
            clear(x, yy, z)


maze_specs = [
    {"start": (2, 4), "stairs": (6, 6), "treasure": (5, 1), "seed": 0x51A1},
    {"start": (6, 6), "stairs": (1, 6), "garden": (4, 3), "cache": (1, 1), "seed": 0x51A2},
    {"start": (1, 6), "vault": (6, 1), "seed": 0x51A3},
]
maze_maps = []
# A single custom-stone shell encloses every branch, room and ceiling on all three floors.
fill(MAZE_MIN_X, MAZE_FLOORS[-1] - 2, MAZE_MIN_Z, MAZE_MAX_X, MAZE_FLOORS[0] + 6, MAZE_MAX_Z, UNDER)
for fi, (floor_y, spec) in enumerate(zip(MAZE_FLOORS, maze_specs)):
    edges = maze_edges(spec["seed"], spec["start"])
    maze_maps.append(edges)
    fill(MAZE_MIN_X, floor_y, MAZE_MIN_Z, MAZE_MAX_X, floor_y, MAZE_MAX_Z, MAZE_TILE)
    for z in range(MAZE_N):
        for x in range(MAZE_N):
            carve_room(floor_y, (x, z))
    for a, b in edges:
        carve_passage(floor_y, a, b)

    # Luminous custom crystal studs every few chambers keep the route legible but still atmospheric.
    for idx, cell in enumerate(sorted((x, z) for z in range(MAZE_N) for x in range(MAZE_N))):
        if (idx + fi * 3) % 5 == 0 and cell not in (spec["start"], spec.get("stairs"), spec.get("treasure"), spec.get("garden"), spec.get("cache"), spec.get("vault")):
            cx, cz = cell_center(cell)
            wallx = cx - 2 if (idx & 1) == 0 else cx + 2
            put(wallx, floor_y + 2, cz, CRYSTAL)
            put(cx, floor_y + 1, cz + 1, MOSS)

    # Traps sit in the open runs between rooms; each has one of three readable rune faces and rearms after use.
    rng = random.Random(spec["seed"] ^ 0x7A9)
    avoid = {spec["start"], spec.get("stairs"), spec.get("treasure"), spec.get("garden"), spec.get("cache"), spec.get("vault")}
    trap_edges = [edge for edge in sorted(edges) if edge[0] not in avoid and edge[1] not in avoid]
    rng.shuffle(trap_edges)
    for j, (a, b) in enumerate(trap_edges[:11 if fi < 2 else 13]):
        ax, az = cell_center(a); bx, bz = cell_center(b)
        if ax != bx:
            tx, tz = (ax + bx) // 2, az
        else:
            tx, tz = ax, (az + bz) // 2
        put(tx, floor_y, tz, S("temple_trap_plate", kind=(j + fi) % 3, armed=True))

    # Reliquaries have wider chambers and curated dressing, still entirely in the temple palette.
    if spec.get("treasure"):
        cell = spec["treasure"]; cx, cz = cell_center(cell)
        carve_room(floor_y, cell, 2)
        model("gold_hoard", cx, floor_y + 1, cz, "south", powered=False)
        chest(cx - 1, floor_y + 1, cz, "east", "temple_maze_cache")
        chest(cx + 1, floor_y + 1, cz, "west", "temple_maze_cache")
        put(cx, floor_y + 1, cz - 2, CRYSTAL)
    if spec.get("garden"):
        cell = spec["garden"]; cx, cz = cell_center(cell)
        carve_room(floor_y, cell, 2)
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 2, cz + 3):
                if (x + z) % 2 == 0:
                    put(x, floor_y, z, SOIL)
                    put(x, floor_y + 1, z, MOSS)
        for x, z, plant in ((cx - 1, cz - 1, FERN), (cx + 1, cz - 1, BLOOM), (cx - 1, cz + 1, ROOT), (cx + 1, cz + 1, FERN), (cx, cz, BLOOM)):
            put(x, floor_y + 1, z, plant)
        for x, z in ((cx - 2, cz), (cx + 2, cz), (cx, cz - 2), (cx, cz + 2)):
            put(x, floor_y + 2, z, CRYSTAL)
        clear(cx, floor_y + 1, cz - 2)
        chest(cx, floor_y + 1, cz - 2, "south", "temple_garden_cache")
    if spec.get("cache"):
        cell = spec["cache"]; cx, cz = cell_center(cell)
        model("chapel_altar", cx, floor_y + 1, cz, "south", powered=True)
        chest(cx - 1, floor_y + 1, cz + 1, "north", "temple_maze_cache")
        chest(cx + 1, floor_y + 1, cz + 1, "north", "temple_maze_cache")
    if spec.get("vault"):
        cell = spec["vault"]; cx, cz = cell_center(cell)
        carve_room(floor_y, cell, 2)
        fill(cx - 2, floor_y, cz - 2, cx + 2, floor_y, cz + 2, S("temple_gold"))
        for x, z in ((cx - 1, cz - 1), (cx + 1, cz - 1), (cx - 1, cz + 1), (cx + 1, cz + 1)):
            put(x, floor_y, z, MAZE_TILE)
        put(cx, floor_y, cz, MAZE_TILE)
        model("chapel_altar", cx, floor_y + 1, cz, "south", powered=True)
        chest(cx - 1, floor_y + 1, cz - 1, "east", "temple_final_vault")
        chest(cx + 1, floor_y + 1, cz - 1, "west", "temple_final_vault")
        chest(cx, floor_y + 1, cz + 1, "north", "temple_final_vault")
        for x, z in ((cx - 2, cz), (cx + 2, cz), (cx, cz - 2), (cx, cz + 2)):
            put(x, floor_y + 2, z, CRYSTAL)

# Spiral stairs between the maze floors. These link the marked rooms, not arbitrary maze cells.
carve_spiral_stair(MAZE_FLOORS[0], MAZE_FLOORS[1], maze_specs[0]["stairs"])
carve_spiral_stair(MAZE_FLOORS[1], MAZE_FLOORS[2], maze_specs[1]["stairs"])

# Custom stairwell from the nave down to the first maze level; it lands in the west branch of the maze.
ENTRY_X = (22, 23)
for k in range(1, 9):
    yy, zz = G - k, 44 - k
    for x in ENTRY_X:
        put(x, yy, zz, stairs("temple_bricks", "north"))
        for clear_y in range(yy + 1, G + 3):
            clear(x, clear_y, zz)
fill(22, G, 44, 23, G + 2, 44, AIR)
put(21, G + 1, 44, TP("y")); put(24, G + 1, 44, TP("y"))
# The last two steps meet the entry rotunda; widen the throat, not the rest of the maze.
fill(21, MAZE_FLOORS[0] + 1, 34, 23, MAZE_FLOORS[0] + 5, 36, AIR)

# ------------------------------------------------------------------ write
assert all(0 <= x < W and 0 <= y < H and 0 <= z < D for x, y, z in B), "template block outside declared bounds"
vanilla = sorted({state[0] for state in B.values() if state[0].startswith("minecraft:") and state[0] != "minecraft:air"})
trap_count = sum(st[0] == "riftworld:temple_trap_plate" for st in B.values())
chest_count = sum("LootTable" in tag for tag in NBT.values())
assert not vanilla, "temple must not build with vanilla blocks: " + ", ".join(vanilla)
assert not any(st[0] == "riftworld:launch_core" for st in B.values()), "the temple must not contain an arrival capsule"
assert len(maze_maps) == 3 and all(len(m) >= 63 for m in maze_maps), "undercroft mazes must be fully connected"
assert sum(st[0] == "riftworld:ritual_candle" for st in B.values()) == 8
assert sum(st[0] == "riftworld:mosaic_core" for st in B.values()) == 1
assert trap_count >= 30 and chest_count >= 10, (trap_count, chest_count)
n, pal = write_template(OUT + "/temple_great.nbt", (W, H, D), B, {k: v for k, v in NBT.items() if k in B})
print("temple_great", W, H, D, "blocks", n, "palette", pal, "maze_floors", len(maze_maps), "traps", trap_count, "loot_chests", chest_count, "vanilla", vanilla, "skips", len(SKIPPED))
for k in list(SKIPPED)[:6]:
    print("  SKIP", k)
