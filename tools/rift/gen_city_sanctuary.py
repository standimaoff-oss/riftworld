#!/usr/bin/env python3
"""
v0.26.0: Replaces the central White Castle in the White City of Elisar (`city_castle.nbt` / `temple_great.nbt`,
`69 x 74 x 73`, `G = 12` -> `worldY = 104`) with the **Portal Sanctuary Rotunda from the Overworld**
(`OvergrownTemplePiece`) surrounded on all sides by **dense, lush Botanical Sanctuary Gardens**
(blossoming trees, arched vine-draped pergolas, lotus fountains, manicured flowering hedges, and rich
botanical flowerbeds) so there is virtually zero empty bare plaza space around the Sanctuary!
"""

import math
import os
import random
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "../.."))
sys.path.insert(0, HERE)
from nbtio import write_template

OUT_CITY = os.path.join(ROOT, "mod/src/main/resources/data/riftworld/structures/city")


def S(name, **p):
    if ":" not in name:
        name = "riftworld:" + name
    return (name, tuple(sorted((k, str(v).lower()) for k, v in p.items())))


def verify_all_blocks(blocks_dict):
    rw_ids = set()
    for jf in (
        "mod/src/main/java/ru/aiforge/riftworld/registry/CastleBlocks.java",
        "mod/src/main/java/ru/aiforge/riftworld/registry/ElisarBlocks.java",
        "mod/src/main/java/ru/aiforge/riftworld/registry/TempleBlocks.java",
        "mod/src/main/java/ru/aiforge/riftworld/registry/ModBlocks.java",
    ):
        txt = open(os.path.join(ROOT, jf), encoding="utf-8").read()
        for m in re.finditer(r'(?:reg|cube|stairs|slab|plant|leaves|log|wood|pillar|wall|fence|door|trapdoor)\(\s*"([a-z0-9_]+)"', txt):
            rw_ids.add("riftworld:" + m.group(1))
        for m in re.finditer(r'BLOCKS\.register\(\s*"([a-z0-9_]+)"', txt):
            rw_ids.add("riftworld:" + m.group(1))
    model_txt = open(os.path.join(ROOT, "mod/src/main/java/ru/aiforge/riftworld/block/ModelKind.java"), encoding="utf-8").read()
    for m in re.finditer(r'^\s*([A-Z0-9_]+)\(\s*"[a-z0-9_]+"', model_txt, re.M):
        rw_ids.add("riftworld:" + m.group(1).lower())

    used_names = {st[0] for st in blocks_dict.values()}
    missing = [nm for nm in sorted(used_names) if nm.startswith("riftworld:") and nm not in rw_ids]
    if missing:
        raise AssertionError(f"Unregistered riftworld block IDs: {missing}")


def build_city_sanctuary_and_gardens():
    W, H, D = 69, 74, 73
    G = 12  # worldY = 104
    CX, CZ = 34, 36  # world (0, 0)
    # Center the Overworld Sanctuary Rotunda at (lx=0, lz=-4) -> South Portico steps end at lz=+14,
    # leaving an inviting floral forecourt around arrival (0, +22) facing straight into the Sanctuary!
    ROT_CX, ROT_CZ = CX, CZ - 4
    SY = G + 5  # Sanctuary floor at G+5 (worldY = 109), raised gracefully above garden terraces (G+2)!

    B = {}
    rng = random.Random(20261102)

    def put(x, y, z, st):
        if 0 <= x < W and 0 <= y < H and 0 <= z < D:
            B[(x, y, z)] = st

    def get(x, y, z):
        return B.get((x, y, z))

    AIR = S("minecraft:air")
    SB = S("minecraft:stone_bricks")
    MSB = S("minecraft:mossy_stone_bricks")
    CSB = S("minecraft:chiseled_stone_bricks")
    SMOOTH = S("minecraft:smooth_stone")
    COBBLE = S("minecraft:cobblestone")
    SB_STAIR = lambda f, half="bottom": S("minecraft:stone_brick_stairs", facing=f, half=half, shape="straight", waterlogged=False)
    SB_SLAB = lambda t="bottom": S("minecraft:stone_brick_slab", type=t, waterlogged=False)
    MSB_WALL = S("minecraft:mossy_stone_brick_wall", east="none", north="none", south="none", up="true", waterlogged=False, west="none")

    TF = S("temple_floor")
    TB = S("temple_bricks")
    TB_IVY = S("temple_bricks_ivy")
    TB_STAIR = lambda f, half="bottom": S("temple_bricks_stairs", facing=f, half=half, shape="straight", waterlogged=False)
    GCOL = S("temple_grand_column", axis="y")
    TM = S("temple_mosaic")
    TMC = S("mosaic_core")
    TGR = S("temple_gold_rune", lit=True)
    CANDLE = S("ritual_candle", lit=True)
    ROSE_WIN = S("temple_rose_window")
    RUG = S("rug", facing="south", powered=False, broken=False)

    ASH = S("ashlar")
    ASHP = S("ashlar_pillar", axis="y")
    ASH_STAIR = lambda f, half="bottom": S("ashlar_stairs", facing=f, half=half, shape="straight", waterlogged=False)
    ASH_SLAB = lambda t="bottom": S("ashlar_slab", type=t, waterlogged=False)

    OX_COP = S("minecraft:oxidized_cut_copper")
    WE_COP = S("minecraft:weathered_cut_copper")
    OX_SLAB_BOT = S("minecraft:oxidized_cut_copper_slab", type="bottom", waterlogged=False)
    OX_SLAB_TOP = S("minecraft:oxidized_cut_copper_slab", type="top", waterlogged=False)

    EGRASS = S("elisar_grass")
    ESOIL = S("elisar_soil")
    ESTONE = S("elisar_stone")
    EPATH = S("elisar_path")
    EFLOW = S("elisar_flowers")
    MOSS = S("minecraft:moss_block")
    MOSS_CARP = S("minecraft:moss_carpet")

    AMBRA_LOG = S("ambra_log", axis="y")
    AMBRA_LOG_X = S("ambra_log", axis="x")
    AMBRA_LOG_Z = S("ambra_log", axis="z")
    SIZO_SLAB_TOP = S("sizo_planks_slab", type="top", waterlogged=False)
    AMBRA_LEAVES = S("ambra_leaves", distance=1, persistent=True, waterlogged=False)
    SIZO_LEAVES = S("sizo_leaves", distance=1, persistent=True, waterlogged=False)
    AZ_LEAVES = S("minecraft:azalea_leaves", distance=1, persistent=True, waterlogged=False)
    FAZ_LEAVES = S("minecraft:flowering_azalea_leaves", distance=1, persistent=True, waterlogged=False)
    AZ_BUSH = S("minecraft:azalea")
    FAZ_BUSH = S("minecraft:flowering_azalea")
    FERN = S("minecraft:fern")
    CORNFLOWER = S("minecraft:cornflower")
    LILY_VAL = S("minecraft:lily_of_the_valley")
    BLUE_ORCHID = S("minecraft:blue_orchid")
    OXEYE = S("minecraft:oxeye_daisy")
    ALLIUM = S("minecraft:allium")
    SPORE = S("minecraft:spore_blossom")
    LILYPAD = S("minecraft:lily_pad")
    PEONY_L = S("minecraft:peony", half="lower")
    PEONY_U = S("minecraft:peony", half="upper")
    LILAC_L = S("minecraft:lilac", half="lower")
    LILAC_U = S("minecraft:lilac", half="upper")
    ROSE_L = S("minecraft:rose_bush", half="lower")
    ROSE_U = S("minecraft:rose_bush", half="upper")

    GWATER = S("garden_water", level=0)
    SEA_L = S("minecraft:sea_lantern")
    LANT = S("minecraft:lantern", hanging=False, waterlogged=False)
    LANT_H = S("minecraft:lantern", hanging=True, waterlogged=False)
    CHAIN_Y = S("minecraft:chain", axis="y", waterlogged=False)
    CHEST_E = S("minecraft:chest", facing="east", type="single", waterlogged=False)
    CHEST_W = S("minecraft:chest", facing="west", type="single", waterlogged=False)
    LECTERN_W = S("minecraft:lectern", facing="west", has_book=True, powered=False)
    BENCH_N = S("bench", facing="north", powered=False, broken=False)
    BENCH_S = S("bench", facing="south", powered=False, broken=False)
    BENCH_E = S("bench", facing="east", powered=False, broken=False)
    BENCH_W = S("bench", facing="west", powered=False, broken=False)

    # =========================================================================
    # 1. FULL PRECINCT TERRACED GARDEN BASE (`x = 0..68, z = 0..72`, `y = 0..G+2`)
    # =========================================================================
    for x in range(W):
        for z in range(D):
            lx = x - CX
            lz = z - CZ
            # Clear old volume
            for y in range(G + 1, H):
                # Leave nothing stale
                pass
            # Deep solid foundation skirt (`0..gy`) so zero corners float on sloped island terrain!
            edge_dist = min(x, W - 1 - x, z, D - 1 - z)
            gy = G + 2 if edge_dist >= 2 else (G + 1 if edge_dist == 1 else G)
            for y in range(0, gy):
                put(x, y, z, ESOIL if y >= gy - 2 else ESTONE)

            # Winding stone walkways:
            # - Cardinal avenues (`|lx| <= 2` South/North, `|lz + 4| <= 2` East/West)
            # - Circular botanical ring promenade (`r_rot` in `19.2 .. 22.0`)
            # - Arrival oval plaza around `(lx = 0, lz = +22)` (`r_arr <= 4.6`)
            r_rot = math.hypot(x - ROT_CX, z - ROT_CZ)
            r_arr = math.hypot(lx, lz - 22)
            on_cardinal_path = (abs(lx) <= 2) or (abs(z - ROT_CZ) <= 2)
            on_ring_path = (19.0 <= r_rot <= 21.8)
            on_arrival = (r_arr <= 4.6)

            if edge_dist == 0:
                # Low ornamental mossy stone & hedge border around precinct with wide cardinal gates!
                if on_cardinal_path:
                    put(x, G, z, TF if abs(lx) <= 1 or abs(z - ROT_CZ) <= 1 else EPATH)
                else:
                    put(x, G, z, MSB)
                    put(x, G + 1, z, FAZ_LEAVES if (x + z) % 3 == 0 else AMBRA_LEAVES)
                    if (x + z) % 8 == 0:
                        put(x, G + 1, z, CSB)
                        put(x, G + 2, z, LANT)
            elif on_arrival or on_cardinal_path or on_ring_path:
                is_trim = (abs(r_rot - 19.0) < 0.65 or abs(r_rot - 21.8) < 0.65) and not on_cardinal_path
                put(x, gy, z, TF if (r_arr <= 2.8 or is_trim or (x + z) % 5 == 0) else EPATH)
            else:
                # Rich living botanical soil/grass/moss bed!
                h = ((x * 734287 + z * 912931) & 0xFFFF) / 65535.0
                put(x, gy, z, MOSS if h < 0.36 else EGRASS)

    # =========================================================================
    # 2. OVERWORLD PORTAL SANCTUARY ROTUNDA (`1:1` with `OvergrownTemplePiece`!)
    #    Centered at `(ROT_CX, SY, ROT_CZ)` (`lx = -17..+17, lz = -17..+20`)
    # =========================================================================
    for lx in range(-17, 18):
        for lz in range(-17, 21):
            r = math.hypot(lx, lz)
            in_south_portico = (abs(lx) <= 4 and 12 <= lz <= 20)
            if r > 16.6 and not in_south_portico:
                continue
            wx, wz = ROT_CX + lx, ROT_CZ + lz

            # Clear air above Sanctuary footprint
            for dy in range(0, 25):
                if (wx, SY + dy, wz) in B:
                    del B[(wx, SY + dy, wz)]

            top_stylobate = 0 if (r <= 14.6 or (abs(lx) <= 4 and lz <= 15)) else (-1 if r <= 15.6 else -2)
            if in_south_portico and lz >= 16:
                top_stylobate = max(-3, 15 - lz)  # steps down G+4 -> G+3 -> G+2 -> G+1 smoothly onto South Avenue!

            for fy in range(G, SY + top_stylobate):
                put(wx, fy, wz, MSB if fy >= SY + top_stylobate - 2 else COBBLE)

            if in_south_portico and lz >= 16:
                if abs(lx) == 4:
                    for wy in range(top_stylobate, 2):
                        put(wx, SY + wy, wz, CSB if wy == 1 else SB)
                    if lz in (16, 19):
                        put(wx, SY + 2, wz, LANT)
                elif lz <= 18:
                    put(wx, SY + top_stylobate, wz, SB_STAIR("north", "bottom"))
                else:
                    put(wx, SY + top_stylobate, wz, TF)
                continue

            if r > 14.6:
                in_dir = ("west" if lx > 0 else "east") if abs(lx) >= abs(lz) else ("north" if lz > 0 else "south")
                put(wx, SY + top_stylobate, wz, SB_STAIR(in_dir, "bottom") if r <= 15.6 else MSB)
                continue

            # Clean radial pavement at `SY`
            cardinal_rib = (abs(lx) <= 1 or abs(lz) <= 1 or abs(abs(lx) - abs(lz)) == 0)
            if r <= 6.2:
                floor_st = TF
            elif r >= 12.6:
                floor_st = MSB if ((lx + lz) & 3) == 0 else SB
            elif cardinal_rib:
                floor_st = SB
            elif abs(r - 9.4) < 0.7:
                floor_st = TGR
            else:
                floor_st = TF
            put(wx, SY, wz, floor_st)

    # Raised Central Ritual Seal Dais (`SY + 1`)
    for lx in range(-6, 7):
        for lz in range(-6, 7):
            r = math.hypot(lx, lz)
            wx, wz = ROT_CX + lx, ROT_CZ + lz
            if r <= 4.5:
                if lx == 0 and lz == 0:
                    st = TMC
                elif abs(lx) <= 1 and abs(lz) <= 1:
                    st = TGR
                elif abs(lx) <= 2 and abs(lz) <= 2:
                    st = TM
                elif 3.2 <= r <= 4.3 and ((abs(lx) + abs(lz)) % 2 == 0):
                    st = TGR
                else:
                    st = TF
                put(wx, SY + 1, wz, st)
            elif r <= 5.5:
                in_dir = ("west" if lx > 0 else "east") if abs(lx) >= abs(lz) else ("north" if lz > 0 else "south")
                put(wx, SY + 1, wz, TB_STAIR(in_dir, "bottom"))

    # Connected Woven Entrance Rug (`T`-shaped runner)
    for lz in range(6, 13):
        for lx in range(-1, 2):
            put(ROT_CX + lx, SY + 1, ROT_CZ + lz, RUG)
    for lz in (11, 12):
        put(ROT_CX - 2, SY + 1, ROT_CZ + lz, RUG)
        put(ROT_CX + 2, SY + 1, ROT_CZ + lz, RUG)

    # 8 Carved Stone Candle Pedestals
    for cx_s, cz_s in ((-6, -2), (-6, 2), (6, -2), (6, 2), (-2, -6), (2, -6), (-2, 6), (2, 6)):
        put(ROT_CX + cx_s, SY + 1, ROT_CZ + cz_s, CSB)
        put(ROT_CX + cx_s, SY + 2, ROT_CZ + cz_s, CANDLE)

    # 8 Solid 2x2 Buttressed Classical Piers
    for px0, pz0 in ((-12, -5), (-12, 4), (11, -5), (11, 4), (-5, -12), (4, -12), (-5, 11), (4, 11)):
        for dx in (0, 1):
            for dz in (0, 1):
                px, pz = px0 + dx, pz0 + dz
                wx, wz = ROT_CX + px, ROT_CZ + pz
                put(wx, SY + 1, wz, CSB)
                put(wx, SY + 2, wz, SB)
                inner_flute = (abs(px) <= 11 and abs(pz) <= 11)
                for dy in range(3, 10):
                    put(wx, SY + dy, wz, GCOL if inner_flute else (CSB if dy == 6 else TB))
                put(wx, SY + 10, wz, CSB)
                # Warm lantern sconces on inner pier faces!
                if inner_flute and abs(px) in (4, 11) and abs(pz) in (4, 11):
                    pass

    # Perimeter Ashlar Drum & Arched Portals (South, North, East, West open garden archways & alcoves!)
    for lx in range(-14, 15):
        for lz in range(-14, 15):
            oct_r = max(abs(lx), abs(lz)) + 0.414 * min(abs(lx), abs(lz))
            if oct_r < 13.0 or oct_r > 14.3:
                continue
            wx, wz = ROT_CX + lx, ROT_CZ + lz
            south_arch = (lz > 10 and abs(lx) <= 3)
            north_arch = (lz < -10 and abs(lx) <= 2)
            west_alcove = (lx < -11 and abs(lz) <= 2)
            east_alcove = (lx > 11 and abs(lz) <= 2)

            for dy in range(1, 12):
                if south_arch:
                    arch_clear = 8 - (lx * lx) // 3
                    if dy <= arch_clear:
                        if dy == arch_clear and abs(lx) >= 2:
                            put(wx, SY + dy, wz, SB_STAIR("east" if lx < 0 else "west", "top"))
                        continue
                if north_arch:
                    # Open arched garden portal on lower levels (dy <= 5) + Rose Window above (dy = 8)!
                    if dy <= 5 - (lx * lx) // 2:
                        continue
                    if 6 <= dy <= 8:
                        if dy == 8 and lx == 0:
                            put(wx, SY + dy, wz, ROSE_WIN)
                        elif abs(lx) <= 1 and dy <= 7:
                            continue
                course = (
                    MSB if dy == 1
                    else CSB if dy in (2, 10)
                    else SMOOTH if dy == 11
                    else (TB_IVY if ((lx + lz + dy) % 9 == 0) else (TB if (west_alcove or east_alcove) and dy <= 7 else SB))
                )
                put(wx, SY + dy, wz, course)

    # Classical Triangular Pediment crowning South Entrance Portico
    for lz in range(13, 16):
        for lx in range(-5, 6):
            wx, wz = ROT_CX + lx, ROT_CZ + lz
            put(wx, SY + 10, wz, CSB)
            ped_top = 13 - abs(lx) // 2
            for dy in range(11, ped_top + 1):
                put(wx, SY + dy, wz, TB if (lz == 15 and dy < ped_top) else SB)
            if abs(lx) >= 1 and abs(lx) % 2 == 1:
                put(wx, SY + ped_top + 1, wz, SB_STAIR("east" if lx < 0 else "west", "bottom"))
        put(ROT_CX, SY + 14, ROT_CZ + lz, SB_SLAB("bottom"))

    # Ribbed Patinated-Copper & Stone Pantheon Dome (`dy = 11..18`) with Open Oculus Collar!
    for lx in range(-14, 15):
        for lz in range(-14, 15):
            r = math.hypot(lx, lz)
            if r < 4.5 or r > 14.2:
                continue
            wx, wz = ROT_CX + lx, ROT_CZ + lz
            u = (14.2 - r) / (14.2 - 4.5)
            u_next = max(0.0, (14.2 - (r + 0.95)) / (14.2 - 4.5))
            exact_h = 11.2 + math.sin(u * math.pi * 0.5) * 6.4
            prev_h = 11.2 + math.sin(u_next * math.pi * 0.5) * 6.4
            dome_y = int(math.floor(exact_h))
            low_y = int(math.floor(prev_h))
            upper_half = (exact_h - dome_y) >= 0.52

            oculus_collar = (r <= 5.3)
            meridian_rib = (abs(lx) <= 1 or abs(lz) <= 1 or abs(abs(lx) - abs(lz)) <= 1)

            for fy in range(min(low_y, dome_y), dome_y + 1):
                if oculus_collar:
                    put(wx, SY + fy, wz, CSB)
                elif meridian_rib:
                    put(wx, SY + fy, wz, SB)
                else:
                    put(wx, SY + fy, wz, WE_COP if fy <= 13 else OX_COP)
            if oculus_collar:
                put(wx, SY + dome_y + 1, wz, SB_SLAB("bottom"))
            elif meridian_rib and upper_half:
                put(wx, SY + dome_y + 1, wz, SB_SLAB("bottom"))
            elif not oculus_collar and not meridian_rib:
                put(wx, SY + min(low_y, dome_y) - 1, wz, SB_SLAB("top"))
                if upper_half:
                    put(wx, SY + dome_y + 1, wz, OX_SLAB_BOT)

    # Interior West Reliquary & East Scriptorium Furnishings + Hanging Lanterns & Spore Blossoms!
    for lx_l, lz_l in ((-9, -9), (9, -9), (-9, 9), (9, 9), (-10, 0), (10, 0), (0, -10)):
        put(ROT_CX + lx_l, SY + 10, ROT_CZ + lz_l, CHAIN_Y)
        put(ROT_CX + lx_l, SY + 9, ROT_CZ + lz_l, LANT_H)
    for lx_s, lz_s in ((-7, -7), (7, -7), (-7, 7), (7, 7)):
        put(ROT_CX + lx_s, SY + 12, ROT_CZ + lz_s, SPORE)

    for dz in range(-2, 3):
        put(ROT_CX - 11, SY + 1, ROT_CZ + dz, SB_STAIR("east") if abs(dz) == 2 else CSB)
        put(ROT_CX + 11, SY + 1, ROT_CZ + dz, SB_STAIR("west") if abs(dz) == 2 else CSB)
    put(ROT_CX - 11, SY + 2, ROT_CZ - 1, CHEST_E)
    put(ROT_CX - 11, SY + 2, ROT_CZ + 1, CHEST_E)
    put(ROT_CX - 11, SY + 2, ROT_CZ, LANT)
    put(ROT_CX + 11, SY + 2, ROT_CZ, LECTERN_W)
    put(ROT_CX + 11, SY + 2, ROT_CZ - 1, CANDLE)
    put(ROT_CX + 11, SY + 2, ROT_CZ + 1, CANDLE)

    # =========================================================================
    # 3. DENSE BOTANICAL GARDENS FILLING ALL SURROUNDING COURTYARDS (`r > 16.6`)
    # =========================================================================
    # A) 4 Tranquil Carved-Stone Lotus Fountains in the 4 Diagonal Quadrants!
    fountain_spots = [
        (CX - 23, CZ - 23),
        (CX + 23, CZ - 23),
        (CX - 23, CZ + 18),
        (CX + 23, CZ + 18),
    ]
    for fx, fz in fountain_spots:
        by = G + 2
        for dx in range(-3, 4):
            for dz in range(-3, 4):
                d = math.hypot(dx, dz)
                if d > 3.4:
                    continue
                if d <= 2.1:
                    put(fx + dx, by - 1, fz + dz, SEA_L if (dx == 0 and dz == 0) else ASH)
                    put(fx + dx, by, fz + dz, GWATER)
                    if abs(dx) == 1 and abs(dz) == 1:
                        put(fx + dx, by + 1, fz + dz, LILYPAD)
                else:
                    put(fx + dx, by, fz + dz, CSB if (dx == 0 or dz == 0) else ASH)
        # Central sculpted water pillar with lantern!
        put(fx, by, fz, ASHP)
        put(fx, by + 1, fz, ASHP)
        put(fx, by + 2, fz, CSB)
        put(fx, by + 3, fz, LANT)

    # B) 4 Vine- & Blossom-Draped Marble Botanical Pergolas (`East`, `West`, `SE`, `SW`)!
    pergola_boxes = [
        (CX - 28, CX - 20, ROT_CZ - 7, ROT_CZ + 7),
        (CX + 20, CX + 28, ROT_CZ - 7, ROT_CZ + 7),
        (CX - 18, CX - 9, CZ + 18, CZ + 28),
        (CX + 9, CX + 18, CZ + 18, CZ + 28),
    ]
    for x0, x1, z0, z1 in pergola_boxes:
        by = G + 2
        # Stone columns along corners & midpoints
        pxs = [x0, (x0 + x1) // 2, x1]
        pzs = [z0, (z0 + z1) // 2, z1]
        for px in pxs:
            for pz in pzs:
                if px not in (x0, x1) and pz not in (z0, z1):
                    continue
                put(px, by, pz, CSB)
                for dy in range(1, 5):
                    put(px, by + dy, pz, ASHP)
                put(px, by + 5, pz, CSB)
        # Overhead trellis beams & flowering leaf canopy (`by + 5..6`)
        for x in range(x0 - 1, x1 + 2):
            for z in range(z0 - 1, z1 + 2):
                on_beam = ((x - x0) % 2 == 0) or ((z - z0) % 2 == 0)
                if x0 <= x <= x1 and z0 <= z <= z1 and on_beam:
                    put(x, by + 5, z, SIZO_SLAB_TOP)
                if ((x * 5 + z * 3) % 3 != 0):
                    leaf = FAZ_LEAVES if ((x + z) % 2 == 0) else AMBRA_LEAVES
                    put(x, by + 6, z, leaf)
                    # Hanging leaf fringes & spore blossoms!
                    if x in (x0 - 1, x1 + 1) or z in (z0 - 1, z1 + 1):
                        if (x + z) % 3 == 0:
                            put(x, by + 5, z, FAZ_LEAVES)
                    elif (x + z) % 7 == 0:
                        put(x, by + 4, z, SPORE)
        # Hanging lantern & garden benches inside pergola
        mx, mz = (x0 + x1) // 2, (z0 + z1) // 2
        put(mx, by + 4, mz, LANT_H)
        put(mx, by, mz, TF)
        put(mx - 2, by + 1, mz, BENCH_E)
        put(mx + 2, by + 1, mz, BENCH_W)

    # C) 14 Blossoming Sanctuary Trees (`Ambra` + `Flowering Azalea` canopies) placed around all sides!
    def build_blossom_tree(tx, tz, height, seed):
        trng = random.Random(seed)
        by = G + 2
        # Organic curved trunk + 4 spreading branches
        lean_x = trng.choice([-1, 0, 1])
        lean_z = trng.choice([-1, 0, 1])
        top_x, top_z = tx, tz
        for dy in range(0, height):
            if dy == height // 2:
                top_x += lean_x
                top_z += lean_z
            put(top_x, by + dy, top_z, AMBRA_LOG)
        # Root collar
        for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            if trng.random() < 0.65:
                put(tx + dx, by, tz + dz, AMBRA_LOG)

        # 4 diagonal/cardinal branches + lush layered blossom crown
        crown_centers = [(top_x, by + height, top_z, 3.6)]
        for b_ang in (0.4, 1.95, 3.55, 5.1):
            bx = top_x + int(round(math.cos(b_ang) * 2.4))
            bz = top_z + int(round(math.sin(b_ang) * 2.4))
            by_b = by + height - 1 - (1 if trng.random() < 0.5 else 0)
            mx_b = (top_x + bx) // 2
            mz_b = (top_z + bz) // 2
            put(mx_b, by_b, mz_b, AMBRA_LOG_X if abs(bx - top_x) >= abs(bz - top_z) else AMBRA_LOG_Z)
            put(bx, by_b, bz, AMBRA_LOG)
            crown_centers.append((bx, by_b + 1, bz, 2.7))

        for cx_c, cy_c, cz_c, rad in crown_centers:
            ir = int(math.ceil(rad))
            for dx in range(-ir, ir + 1):
                for dy in range(-2, 3):
                    for dz in range(-ir, ir + 1):
                        dist = math.sqrt(dx * dx + (dy * 1.35) ** 2 + dz * dz)
                        if dist <= rad + (trng.random() - 0.5) * 0.55:
                            lx_p, ly_p, lz_p = cx_c + dx, cy_c + dy, cz_c + dz
                            if get(lx_p, ly_p, lz_p) is None:
                                rpick = trng.random()
                                leaf = FAZ_LEAVES if rpick < 0.42 else (AMBRA_LEAVES if rpick < 0.82 else SIZO_LEAVES)
                                put(lx_p, ly_p, lz_p, leaf)
        # Hanging lantern from one bough!
        lx_hang, hy_hang, lz_hang = crown_centers[1][0], crown_centers[1][1] - 2, crown_centers[1][2]
        if get(lx_hang, hy_hang, lz_hang) in (None, AMBRA_LEAVES, FAZ_LEAVES, SIZO_LEAVES):
            put(lx_hang, hy_hang, lz_hang, LANT_H)

    tree_spots = [
        (CX - 28, CZ - 30, 8), (CX + 28, CZ - 30, 8),
        (CX - 13, CZ - 29, 7), (CX + 13, CZ - 29, 7),
        (CX - 29, CZ - 16, 8), (CX + 29, CZ - 16, 8),
        (CX - 29, CZ + 8, 7),  (CX + 29, CZ + 8, 7),
        (CX - 29, CZ + 29, 8), (CX + 29, CZ + 29, 8),
        (CX - 15, CZ + 31, 7), (CX + 15, CZ + 31, 7),
        (CX - 7,  CZ + 29, 6), (CX + 7,  CZ + 29, 6),
    ]
    for idx, (tx, tz, th) in enumerate(tree_spots):
        build_blossom_tree(tx, tz, th, 9001 + idx * 37)

    # D) Dense Botanical Groundcover, Manicured Hedges, Two-Block Tall Rose/Peony/Lilac Clusters & Garden Lanterns!
    #    Fills ~78% of all open grass/moss tiles so the Sanctuary sits in an abundant living paradise!
    tall_flowers = [(PEONY_L, PEONY_U), (LILAC_L, LILAC_U), (ROSE_L, ROSE_U)]
    low_flora = [EFLOW, EFLOW, FAZ_BUSH, AZ_BUSH, CORNFLOWER, LILY_VAL, BLUE_ORCHID, OXEYE, ALLIUM, FERN, MOSS_CARP]

    # Symmetrical lampposts along main avenues & ring promenade
    for lx_l, lz_l in (
        (-4, 16), (4, 16), (-4, 24), (4, 24), (-4, 31), (4, 31),
        (-4, -24), (4, -24), (-4, -32), (4, -32),
        (-22, -4), (22, -4), (-31, -4), (31, -4),
    ):
        px, pz = CX + lx_l, CZ + lz_l
        if get(px, G + 3, pz) is None:
            put(px, G + 2, pz, TF)
            put(px, G + 3, pz, MSB_WALL)
            put(px, G + 4, pz, MSB_WALL)
            put(px, G + 5, pz, LANT)

    for x in range(2, W - 2):
        for z in range(2, D - 2):
            r_rot = math.hypot(x - ROT_CX, z - ROT_CZ)
            in_portico = (abs(x - ROT_CX) <= 5 and ROT_CZ + 12 <= z <= ROT_CZ + 20)
            if r_rot <= 16.9 or in_portico:
                continue
            ground = get(x, G + 2, z)
            if ground not in (EGRASS, MOSS):
                continue
            if get(x, G + 3, z) is not None:
                continue

            # Sculpted flowering hedges bordering the circular ring path (`r_rot` in `17.2..18.4` and `22.3..23.2`)
            on_hedge_ring = (17.2 <= r_rot <= 18.2) or (22.3 <= r_rot <= 23.1)
            lx = x - CX
            near_avenue = (abs(lx) <= 4) or (abs(z - ROT_CZ) <= 4)
            if on_hedge_ring and not near_avenue:
                put(x, G + 3, z, FAZ_LEAVES if (x + z) % 2 == 0 else AMBRA_LEAVES)
                if (x + z) % 5 == 0 and get(x, G + 4, z) is None:
                    put(x, G + 4, z, FAZ_LEAVES)
                continue

            roll = rng.random()
            if roll < 0.18 and get(x, G + 4, z) is None:
                low_st, up_st = tall_flowers[(x * 3 + z * 5) % len(tall_flowers)]
                put(x, G + 3, z, low_st)
                put(x, G + 4, z, up_st)
            elif roll < 0.30:
                # Lush low ornamental shrub!
                put(x, G + 3, z, FAZ_BUSH if (x + z) % 2 == 0 else AZ_BUSH)
            elif roll < 0.82:
                put(x, G + 3, z, low_flora[(x * 7 + z * 13 + rng.randint(0, 3)) % len(low_flora)])

    verify_all_blocks(B)

    n1, pal1 = write_template(os.path.join(OUT_CITY, "city_castle.nbt"), (W, H, D), B, {})
    write_template(os.path.join(OUT_CITY, "temple_great.nbt"), (W, H, D), B, {})
    print(f"Generated Sanctuary + Botanical Gardens -> city_castle.nbt & temple_great.nbt: {W}x{H}x{D}, blocks={n1}, palette={pal1}")


if __name__ == "__main__":
    build_city_sanctuary_and_gardens()
