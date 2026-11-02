#!/usr/bin/env python3
"""
v0.27.0: Builds ONLY the circular White-Marble & Patinated-Copper Portal Sanctuary Rotunda
(`city_castle.nbt` & `temple_great.nbt`, `35 x 40 x 39`, `ROT_CX = 17, ROT_CZ = 17, G = 12, SY = 15`).

CRITICAL NON-DESTRUCTIVE GUARANTEE:
  - `write_template` ONLY writes `(x, y, z)` entries present in `B`.
  - Every coordinate outside the circular Rotunda cylinder (`r <= 16.5`) and South Portico steps
    is omitted from `B` (Minecraft Structure Void -> 0 blocks deleted or overwritten around it!).
  - ZERO static `.nbt` trees or square garden walls: the surrounding Circular White Marble Plaza,
    8 Radiating Walkways (`дорожки`), and Generative Dynamic Trees (`riftworlddt:ambra` / `sizo`)
    are generated procedurally on the terrain by `ElisarChunkGenerator` + `DtGround`!
"""

import math
import os
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


def build_circular_sanctuary_rotunda():
    W, H, D = 35, 40, 39
    ROT_CX, ROT_CZ = 17, 17   # placed at (-17, 92, -21) -> Rotunda center at world (0, -4)!
    G = 12                    # worldY = 104 (base island level; circular white plaza sits at 105)
    SY = G + 3                # Sanctuary rotunda floor at worldY = 107 (2 graceful steps above white plaza!)

    B = {}
    NBT = {}

    def put(x, y, z, st):
        if 0 <= x < W and 0 <= y < H and 0 <= z < D:
            B[(x, y, z)] = st

    AIR = S("minecraft:air")
    IVORY = S("ivory_marble")
    IVORY_BRICK = S("ivory_marble_bricks")
    IVORY_CHIS = S("chiseled_ivory_marble")
    IVORY_TILES = S("ivory_marble_tiles")
    IVORY_GILD = S("gilded_ivory_marble")
    IVORY_PILLAR = S("ivory_marble_pillar", axis="y")
    IVORY_STAIR = lambda f, half="bottom": S("ivory_marble_stairs", facing=f, half=half, shape="straight", waterlogged=False)
    IVORY_SLAB = lambda t="bottom": S("ivory_marble_slab", type=t, waterlogged=False)

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

    OX_COP = S("minecraft:oxidized_cut_copper")
    WE_COP = S("minecraft:weathered_cut_copper")
    OX_SLAB_BOT = S("minecraft:oxidized_cut_copper_slab", type="bottom", waterlogged=False)
    SPORE = S("minecraft:spore_blossom")
    LANT = S("minecraft:lantern", hanging=False, waterlogged=False)
    LANT_H = S("minecraft:lantern", hanging=True, waterlogged=False)
    CHAIN_Y = S("minecraft:chain", axis="y", waterlogged=False)
    CHEST_E = S("minecraft:chest", facing="east", type="single", waterlogged=False)
    LECTERN_W = S("minecraft:lectern", facing="west", has_book=True, powered=False)

    # 1. Strictly Circular White-Marble Stylobate & Interior Floor (`r <= 16.4` + South Portico `lz = 12..20`)
    for lx in range(-17, 18):
        for lz in range(-17, 22):
            r = math.hypot(lx, lz)
            in_south_portico = (abs(lx) <= 4 and 12 <= lz <= 20)
            in_cardinal_step = ((abs(lx) <= 2 and -17 <= lz <= -14) or (abs(lz) <= 2 and 14 <= abs(lx) <= 17))
            if r > 16.4 and not in_south_portico and not in_cardinal_step:
                continue
            wx, wz = ROT_CX + lx, ROT_CZ + lz

            # Clear interior air ONLY inside the Rotunda drum (never outside!)
            if r <= 13.8:
                for dy in range(1, 13):
                    put(wx, SY + dy, wz, AIR)

            top_stylobate = 0 if (r <= 14.6 or (abs(lx) <= 4 and lz <= 15)) else (-1 if r <= 15.6 else -2)
            if in_south_portico and lz >= 16:
                top_stylobate = max(-2, 16 - lz)

            # Solid circular foundation down to `y = 8` (`worldY = 100`) so zero gaps on terrain
            for fy in range(8, SY + top_stylobate):
                put(wx, fy, wz, IVORY_BRICK if fy >= SY + top_stylobate - 3 else TB)

            if in_south_portico and lz >= 16:
                if abs(lx) == 4 and lz <= 19:
                    for wy in range(top_stylobate, 2):
                        put(wx, SY + wy, wz, IVORY_CHIS if wy == 1 else IVORY_BRICK)
                    if lz in (16, 19):
                        put(wx, SY + 2, wz, LANT)
                elif lz in (16, 17):
                    put(wx, SY + top_stylobate, wz, IVORY_STAIR("north", "bottom"))
                else:
                    put(wx, SY + top_stylobate, wz, IVORY_TILES)
                continue

            if r > 14.6:
                in_dir = ("west" if lx > 0 else "east") if abs(lx) >= abs(lz) else ("north" if lz > 0 else "south")
                put(wx, SY + top_stylobate, wz, IVORY_STAIR(in_dir, "bottom") if r <= 15.6 else IVORY_TILES)
                continue

            # Gleaming White-Marble & Temple-Floor radial interior pavement at `SY`
            cardinal_rib = (abs(lx) <= 1 or abs(lz) <= 1 or abs(abs(lx) - abs(lz)) == 0)
            if r <= 6.2:
                floor_st = TF
            elif r >= 12.6:
                floor_st = IVORY_TILES
            elif cardinal_rib:
                floor_st = IVORY_GILD
            elif abs(r - 9.4) < 0.7:
                floor_st = TGR
            else:
                floor_st = TF
            put(wx, SY, wz, floor_st)

    # 2. Raised Central Ritual Seal Dais (`SY + 1`)
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

    # Connected Woven Entrance Rug
    for lz in range(6, 13):
        for lx in range(-1, 2):
            put(ROT_CX + lx, SY + 1, ROT_CZ + lz, RUG)
    for lz in (11, 12):
        put(ROT_CX - 2, SY + 1, ROT_CZ + lz, RUG)
        put(ROT_CX + 2, SY + 1, ROT_CZ + lz, RUG)

    # 8 Carved Ivory Candle Pedestals
    for cx_s, cz_s in ((-6, -2), (-6, 2), (6, -2), (6, 2), (-2, -6), (2, -6), (-2, 6), (2, 6)):
        put(ROT_CX + cx_s, SY + 1, ROT_CZ + cz_s, IVORY_CHIS)
        put(ROT_CX + cx_s, SY + 2, ROT_CZ + cz_s, CANDLE)

    # 3. 8 Solid 2x2 Buttressed Classical White-Marble Piers
    for px0, pz0 in ((-12, -5), (-12, 4), (11, -5), (11, 4), (-5, -12), (4, -12), (-5, 11), (4, 11)):
        for dx in (0, 1):
            for dz in (0, 1):
                px, pz = px0 + dx, pz0 + dz
                wx, wz = ROT_CX + px, ROT_CZ + pz
                put(wx, SY + 1, wz, IVORY_CHIS)
                put(wx, SY + 2, wz, IVORY_BRICK)
                inner_flute = (abs(px) <= 11 and abs(pz) <= 11)
                for dy in range(3, 10):
                    put(wx, SY + dy, wz, GCOL if inner_flute else (IVORY_CHIS if dy == 6 else IVORY_PILLAR))
                put(wx, SY + 10, wz, IVORY_CHIS)

    # 4. Perimeter White-Marble Drum & 4 Open Cardinal Portals (tying into the 8 radial walkways!)
    for lx in range(-14, 15):
        for lz in range(-14, 15):
            oct_r = max(abs(lx), abs(lz)) + 0.414 * min(abs(lx), abs(lz))
            if oct_r < 13.0 or oct_r > 14.3:
                continue
            wx, wz = ROT_CX + lx, ROT_CZ + lz
            south_arch = (lz > 10 and abs(lx) <= 3)
            north_arch = (lz < -10 and abs(lx) <= 2)
            west_arch = (lx < -10 and abs(lz) <= 2)
            east_arch = (lx > 10 and abs(lz) <= 2)

            for dy in range(1, 12):
                if south_arch:
                    arch_clear = 8 - (lx * lx) // 3
                    if dy <= arch_clear:
                        if dy == arch_clear and abs(lx) >= 2:
                            put(wx, SY + dy, wz, IVORY_STAIR("east" if lx < 0 else "west", "top"))
                        continue
                if north_arch or west_arch or east_arch:
                    span_coord = lx if north_arch else lz
                    if dy <= 5 - (span_coord * span_coord) // 2:
                        continue
                    if 6 <= dy <= 8:
                        if dy == 8 and span_coord == 0:
                            put(wx, SY + dy, wz, ROSE_WIN)
                        elif abs(span_coord) <= 1 and dy <= 7:
                            continue
                course = (
                    IVORY_BRICK if dy == 1
                    else IVORY_CHIS if dy in (2, 10)
                    else IVORY_GILD if dy == 11
                    else (TB_IVY if ((lx + lz + dy) % 13 == 0) else TB)
                )
                put(wx, SY + dy, wz, course)

    # 5. Classical White-Marble Triangular Pediment crowning South Entrance Portico
    for lz in range(13, 16):
        for lx in range(-5, 6):
            wx, wz = ROT_CX + lx, ROT_CZ + lz
            put(wx, SY + 10, wz, IVORY_CHIS)
            ped_top = 13 - abs(lx) // 2
            for dy in range(11, ped_top + 1):
                put(wx, SY + dy, wz, IVORY_GILD if (lz == 15 and dy == 11) else IVORY_BRICK)
            if abs(lx) >= 1 and abs(lx) % 2 == 1:
                put(wx, SY + ped_top + 1, wz, IVORY_STAIR("east" if lx < 0 else "west", "bottom"))
        put(ROT_CX, SY + 14, ROT_CZ + lz, IVORY_SLAB("bottom"))

    # 6. Ribbed Patinated-Copper & White-Marble Pantheon Dome (`dy = 11..18`) with Open Oculus!
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
                    put(wx, SY + fy, wz, IVORY_CHIS)
                elif meridian_rib:
                    put(wx, SY + fy, wz, IVORY)
                else:
                    put(wx, SY + fy, wz, WE_COP if fy <= 13 else OX_COP)
            if oculus_collar:
                put(wx, SY + dome_y + 1, wz, IVORY_SLAB("bottom"))
            elif meridian_rib and upper_half:
                put(wx, SY + dome_y + 1, wz, IVORY_SLAB("bottom"))
            elif not oculus_collar and not meridian_rib:
                put(wx, SY + min(low_y, dome_y) - 1, wz, IVORY_SLAB("top"))
                if upper_half:
                    put(wx, SY + dome_y + 1, wz, OX_SLAB_BOT)

    # 7. Interior Reliquary & Scriptorium + Hanging Lanterns
    for lx_l, lz_l in ((-9, -9), (9, -9), (-9, 9), (9, 9), (-10, 0), (10, 0), (0, -10)):
        put(ROT_CX + lx_l, SY + 10, ROT_CZ + lz_l, CHAIN_Y)
        put(ROT_CX + lx_l, SY + 9, ROT_CZ + lz_l, LANT_H)
    for lx_s, lz_s in ((-7, -7), (7, -7), (-7, 7), (7, 7)):
        put(ROT_CX + lx_s, SY + 12, ROT_CZ + lz_s, SPORE)

    put(ROT_CX - 11, SY + 1, ROT_CZ - 3, IVORY_CHIS)
    put(ROT_CX - 11, SY + 2, ROT_CZ - 3, CHEST_E)
    NBT[(ROT_CX - 11, SY + 2, ROT_CZ - 3)] = {"id": "minecraft:chest", "LootTable": "riftworld:chests/elisar_temple"}
    put(ROT_CX + 11, SY + 1, ROT_CZ - 3, IVORY_CHIS)
    put(ROT_CX + 11, SY + 2, ROT_CZ - 3, LECTERN_W)

    verify_all_blocks(B)
    for fname in ("city_castle.nbt", "temple_great.nbt"):
        out_path = os.path.join(OUT_CITY, fname)
        write_template(out_path, (W, H, D), B, NBT)
        print(f"Wrote {out_path}: size=({W},{H},{D}), blocks={len(B)}")


if __name__ == "__main__":
    build_circular_sanctuary_rotunda()
