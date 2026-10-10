"""Biome 04 «Фиолетовые холмы»: small field churches in TWO variants (Rev 4 item 8, max 2 per biome, on the meadow island).
-> data/riftworld/structures/ruins/church_violet.nbt        (действующая: intact, lit, carpet, gold bowl, bell cote)
-> data/riftworld/structures/ruins/church_violet_ruins.nbt  (заброшенная: collapsed roof half, broken wall, moss, cobwebs, dark)

Palette: pale elisar bricks + cobble plinth + blue-shingle roof (fits the lilac hills); both variants share one layout so the
pair reads as «та же церковь, но...». Template 13 x 19; y = 0 is the floor layer on the flattened meadow top; door faces +z (south).
"""
import json, os, re, sys

HERE = os.path.dirname(os.path.abspath(__file__))
src = open(os.path.join(HERE, "gen_castle.py"), encoding="utf-8").read()
exec(src[:src.index("# ================================================================== foundation / floors")])
rooms = open(os.path.join(HERE, "castle_rooms.py"), encoding="utf-8").read()
exec(rooms[:rooms.index("def tower_door")])

OUT = MOD + "/src/main/resources/data/riftworld/structures/ruins"
os.makedirs(OUT, exist_ok=True)
W, D, HW = 13, 19, 8                                    # width, depth, wall height

BR = S("elisar_bricks")
BR_M = S("elisar_bricks_mossy")
CB = S("elisar_cobble")
CB_S = lambda f: S("elisar_cobble_stairs", facing=f)
ROOF = lambda f, **kw: S("thatch_stairs", facing=f, **kw)
ROOF_T = S("thatch_slab", type="top")
WIN = S("elisar_window")
GOLD = S("temple_gold")
MOSS = S("elisar_moss")
WEB = S("castle_cobweb")


def church(ruined):
    global B, NBT
    B.clear(); NBT.clear() if isinstance(NBT, dict) else None
    # ------------------------------------------------------------------ plinth + nave shell
    fill(0, 0, 0, 12, 0, 18, CB)                                    # plinth slab
    walls(1, 1, 11, 17, 1, HW, t=1, mat=lambda x, y, z: BR_M if hash_wall(x, y, z) else BR)
    band(1, 1, 11, 17, 1, CB)                                       # plinth course
    # buttresses at the corners + middle
    for (bx, bz) in ((0, 0), (12, 0), (0, 18), (12, 18), (0, 9), (12, 9)):
        fill(bx, 1, bz, bx, 4, bz, CB)
    # door arch at the south face (x 5..7, y 1..4)
    opening("x", 0, 5, 7, 1, 4, [17], pointed=True)
    # lancet windows on the long walls
    for z in (5, 13):
        lancet("z", z, 3, 3, 1, [])
        lancet("z", z, 3, 3, 11, [])
    # round window over the door (light trick: gold centre)
    put(6, HW - 2, 17, WIN); put(5, HW - 1, 17, WIN); put(7, HW - 1, 17, WIN); put(6, HW - 1, 17, GOLD if not ruined else WIN)
    if ruined:
        put(6, HW - 2, 17, AIR)
    # gable roofs (ridge N-S): steps up from y HW+1 at x 0/12 to ridge x 6
    for s in range(7):
        y = HW + 1 + s
        put(0 + s, y, 0, ROOF("east"))
        for z in range(0, 19):
            put(0 + s, y, z, ROOF("east"))
            put(12 - s, y, z, ROOF("west"))
    for z in range(0, 19):
        put(6, HW + 7, z, ROOF_T)
    # gable end walls (north/south triangles)
    for s in range(7):
        y = HW + 1 + s
        fill(1 + s, y, 1, 11 - s, y, 1, BR)
        fill(1 + s, y, 17, 11 - s, y, 17, BR)
    # golden onion dome over mid-nave (the reference silhouette: pale nave + gold dome + cross)
    drum_y0 = HW + 1
    for y in range(drum_y0, HW + 5):
        fill(5, y, 8, 7, y, 10, BR)                       # 3x3 drum pierces the ridge
    put(6, HW + 2, 7, WIN); put(6, HW + 2, 11, WIN)       # drum glow-slits
    for xx in (5, 7):
        put(xx, HW + 2, 9, WIN) if xx in (5, 7) else None
    oy = HW + 5                                           # onion base
    for (dx, dz) in [(dx, dz) for dx in range(-2, 3) for dz in range(-2, 3) if max(abs(dx), abs(dz)) == 2 and abs(dx) == 2 and abs(dz) == 2]:
        continue
    dome_cells = []
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if abs(dx) == 2 and abs(dz) == 2: continue    # cornerless 5x5
            dome_cells.append((dx, dz, 0))
            if abs(dx) <= 1 and abs(dz) <= 1:
                dome_cells.append((dx, dz, 1))
                dome_cells.append((dx, dz, 2))
    for dx in (-1, 0, 1):
        dome_cells.append((dx, 0, 3)); dome_cells.append((0, dx, 3))
    dome_cells.append((0, 0, 4))
    for (dx, dz, dy) in dome_cells:
        put(6 + dx, oy + dy, 9 + dz, GOLD)
    # the cross
    put(6, oy + 5, 9, GOLD); put(6, oy + 6, 9, GOLD); put(6, oy + 7, 9, GOLD)
    put(5, oy + 6, 9, GOLD); put(7, oy + 6, 9, GOLD)
    put(6, oy + 6, 8, GOLD); put(6, oy + 6, 10, GOLD)
    # gold finials on both gable ends
    put(6, HW + 8, 0, GOLD); put(6, HW + 8, 18, GOLD)
    # floor: cobble with a mossy checker, crimson runner to the dais
    for x in range(2, 11):
        for z in range(2, 17):
            if (x + z) % 7 == 0:
                put(x, 1, z, BR_M)
    fill(5, 1, 9, 7, 1, 16, S("castle_carpet"))
    # dais + altar at the north end
    fill(2, 1, 2, 10, 1, 4, CB)
    for x in range(3, 10):
        put(x, 1, 5, CB_S("north"))
    model("chapel_altar", 5, 2, 2, "south", powered=not ruined)
    # pews
    for z in (9, 13):
        model("castle_bench", 3, 1, z, "east")
        model("castle_bench", 9, 1, z, "west")
    # corner dressing
    model("candle_stand", 2, 1, 6, "east", powered=not ruined)
    model("candle_stand", 10, 1, 6, "west", powered=not ruined)
    model("wall_sconce", 2, 3, 12, "east", powered=not ruined)
    model("wall_sconce", 10, 3, 12, "west", powered=not ruined)
    if not ruined:
        model("gold_hoard", 9, 1, 3, "south", powered=False)
        model("chained_lectern", 3, 1, 4, "east")
        model("chained_books", 3, 1, 5, "east")        # item 14: books RРЯДОМ, not on top of the lectern
    else:
        # the ruin: half the roof gone, wall broken low, moss & cobwebs inside
        import random
        rnd = random.Random(4422)
        for z in range(5, 15):
            for x in range(3, 10):
                for y in range(1, HW + 8):
                    st_ = B.get((x, y, z))
                    if st_ is None: continue
                    n = st_[0]
                    if (n.startswith("thatch") and rnd.random() < 0.55) or (n in ("elisar_bricks", "elisar_bricks_mossy") and y > HW - 2 and rnd.random() < 0.4):
                        put(x, y, z, AIR)
        for (x, y, z) in ((2, 1, 8), (7, 1, 10), (4, 2, 6), (9, 1, 14), (6, 1, 12)):
            put(x, y, z, MOSS)
        # onion + cross collapsed: gold above the lower rim is gone
        for (x, y, z) in list(B.keys()):
            st_ = B[(x, y, z)]
            if st_[0] == "temple_gold" and y >= HW + 6 and 4 <= x <= 8 and 7 <= z <= 11:
                if rnd.random() < (0.85 if y >= HW + 7 else 0.45):
                    put(x, y, z, AIR)
        for z in range(4, 14, 3):
            put(4, 1, z, WEB); put(8, 2, z + 1, WEB) if z + 1 < 18 else None
        # rubble inside the breach
        for (x, z) in ((5, 8), (6, 8), (6, 9), (5, 11)):
            put(x, 1, z, CB)
        put(6, 2, 8, CB)
    return B, NBT


def hash_wall(x, y, z):
    h = (x * 73428767 + y * 912931 + z * 49993) & 0xFFFF
    return h % 9 < 2


for _name, _r in (("church_violet", False), ("church_violet_ruins", True)):
    B.clear(); NBT.clear(); MODEL_CELLS.clear(); MODELS.clear(); SKIPPED.clear()
    church(_r)
    n, pal = write_template(OUT + "/" + _name + ".nbt", (W, 21, D), dict(B), {k: v for k, v in NBT.items() if k in B})
    print(_name, "->", n, "blocks,", pal, "palette, skips", len(SKIPPED))
