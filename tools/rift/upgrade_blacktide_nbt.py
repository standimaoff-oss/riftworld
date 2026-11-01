#!/usr/bin/env python3
"""
v0.24.0 Architectural Interior & Exterior Overhaul for Blacktide Citadel (blacktide.nbt):
  1. Grand Black Cathedral Throne Hall (lx=-38..+50, lz=-47..-5, by=63..108, matching 03_citadel_hall_cathedral_throne.png):
     - Sunken mosaic & connected +-cross Royal Crimson-and-Gold carpet nave (riftworld:castle_carpet)
     - 16 tall clustered basalt-and-stone gothic columns with burnished gold capitals
     - Soaring dark-slate & gilded cross-ribbed gothic vaults (by=88..97) + wrought-iron chandeliers
     - Two-story side arcades with walkable upper triforium galleries (by=76), banquet/trophy halls & west choir loft
     - Tall pointed-arch gothic stained-glass lancet windows (glowing riftworld:stained_glass + rose accents)
     - Monumental stepped Royal Throne Apse (by=63->69) with 4 golden fire braziers, royal banners,
       gothic obsidian-and-gold baldachin canopy, and BOSS_THRONE facing down the 80m nave!
     - Twin West Narthex staircases (by=63->76->90) + Grand Spiral Belfry Staircase up the Central Crossing
       Lantern Spire (by=76->92->108->122->134 panoramic lookout!)
     - Vaulted Undercroft Wine-Cellar, Castle Kitchen & Dungeon Brig beneath the nave (by=53..60)
  2. South Astral Rotunda / Chapel Sanctum & Twin Spires ((-25,+22), matching 04_citadel_hall_rotunda_sanctum.png):
     - Concentric dark-basalt & gold-inlaid floor + connected +-cross carpet + CHAPEL_ALTAR + scriptorium
     - Continuous spiral stone stairs up BOTH Twin Spires to by=92
  3. SW Sea-Bridge Gatehouse Barbican & Twin Towers ((-65,+35)):
     - Interior guard-tower stairs (by=48->60->72) into the Upper Barbican War-Room (connected L-shaped rug,
       LONG_TABLE, GUARD_ARMOR, GUARD_WEAPON_RACK) + stairs up both Gatehouse Towers to by=88
  4. ALL 12 Outer Perimeter Watchtowers:
     - Ground courtyard doorways + rampart doorways + intermediate floors (by=60, by=68) + continuous
       square-spiral stone staircases all the way through roof hatches onto upgraded crenellated blackstone
       watch-decks with gilded beacon lantern finials!
  5. Monumental Courtyard & Curtain-Wall Rampart Staircases connecting Lower Courtyard (by=48),
     Curtain-Wall Ramparts (by=60), and Upper Cathedral Plaza (by=63).
  6. Preserves by <= 24 and the shore sea-bridge 100% untouched (sub_air == 0, bridge_land == true).
"""
import gzip
import math
import os
import struct

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
NBT_PATH = os.path.join(ROOT, "mod/src/main/resources/data/riftworld/structures/blacktide.nbt")

W, H, D = 248, 176, 216
OX, OZ = -124, -100


def load_template(path: str):
    raw = gzip.open(path, "rb").read()
    pos = 0

    def u8():
        nonlocal pos; v = raw[pos]; pos += 1; return v

    def i32():
        nonlocal pos; v = struct.unpack_from(">i", raw, pos)[0]; pos += 4; return v

    def utf():
        nonlocal pos
        ln = struct.unpack_from(">H", raw, pos)[0]; pos += 2
        s = raw[pos:pos + ln].decode("utf-8")
        pos += ln
        return s

    def skip(t):
        nonlocal pos
        if t == 1: pos += 1
        elif t == 2: pos += 2
        elif t in (3, 5): pos += 4
        elif t in (4, 6): pos += 8
        elif t == 7: pos += i32()
        elif t == 8: utf()
        elif t == 9:
            et = u8(); cnt = i32()
            for _ in range(cnt): skip(et)
        elif t == 10:
            while True:
                ft = u8()
                if ft == 0: break
                utf(); skip(ft)
        elif t == 11: pos += 4 * i32()
        elif t == 12: pos += 8 * i32()

    assert u8() == 10
    utf()
    vox = bytearray(W * H * D)
    palette = []
    size_vec = [W, H, D]
    data_version = 3465

    while True:
        t = u8()
        if t == 0: break
        nm = utf()
        if nm == "DataVersion" and t == 3:
            data_version = i32()
        elif nm == "size" and t == 9:
            assert u8() == 3 and i32() == 3
            size_vec = [i32(), i32(), i32()]
        elif nm == "palette" and t == 9:
            assert u8() == 10
            cnt = i32()
            for _ in range(cnt):
                bname = "minecraft:air"
                props = {}
                while True:
                    ft = u8()
                    if ft == 0: break
                    fn = utf()
                    if fn == "Name" and ft == 8:
                        bname = utf()
                    elif fn == "Properties" and ft == 10:
                        while True:
                            pt = u8()
                            if pt == 0: break
                            pk = utf()
                            if pt == 8: props[pk] = utf()
                            else: skip(pt)
                    else:
                        skip(ft)
                palette.append((bname, tuple(sorted(props.items()))))
        elif nm == "blocks" and t == 9:
            assert u8() == 10
            cnt = i32()
            for _ in range(cnt):
                bx = by = bz = st = 0
                while True:
                    ft = u8()
                    if ft == 0: break
                    fn = utf()
                    if fn == "pos" and ft == 9:
                        assert u8() == 3 and i32() == 3
                        bx, by, bz = i32(), i32(), i32()
                    elif fn == "state" and ft == 3:
                        st = i32()
                    else:
                        skip(ft)
                if 0 <= bx < W and 0 <= by < H and 0 <= bz < D:
                    vox[(by * D + bz) * W + bx] = st + 1
        else:
            skip(t)

    return vox, palette, size_vec, data_version


def save_template(path: str, vox: bytearray, palette: list, size_vec: list, data_version: int):
    # Compact unused palette entries first so palette stays small & fast
    used = [False] * (len(palette) + 1)
    for v in vox:
        if v:
            used[v] = True
    remap = [0] * (len(palette) + 1)
    new_palette = []
    for old_v in range(1, len(palette) + 1):
        if used[old_v]:
            new_palette.append(palette[old_v - 1])
            remap[old_v] = len(new_palette)  # 1-based

    assert len(new_palette) <= 250, f"Palette exceeded byte limit: {len(new_palette)}"

    nonzero_count = 0
    for i in range(len(vox)):
        v = vox[i]
        if v:
            vox[i] = remap[v]
            nonzero_count += 1

    out = bytearray()

    def u8(v): out.append(v & 0xFF)
    def i32(v): out.extend(struct.pack(">i", v))
    def utf(s):
        b = s.encode("utf-8")
        out.extend(struct.pack(">H", len(b)))
        out.extend(b)

    # Root Compound ""
    u8(10); utf("")

    # DataVersion
    u8(3); utf("DataVersion"); i32(data_version)

    # size
    u8(9); utf("size"); u8(3); i32(3)
    for sv in size_vec: i32(sv)

    # entities (empty list)
    u8(9); utf("entities"); u8(10); i32(0)

    # palette
    u8(9); utf("palette"); u8(10); i32(len(new_palette))
    for bname, props in new_palette:
        u8(8); utf("Name"); utf(bname)
        if props:
            u8(10); utf("Properties")
            for pk, pv in props:
                u8(8); utf(pk); utf(pv)
            u8(0)
        u8(0)

    # blocks
    u8(9); utf("blocks"); u8(10); i32(nonzero_count)
    pos_bytes = b"\x09\x00\x03pos\x03\x00\x00\x00\x03"
    state_bytes = b"\x03\x00\x05state"
    pack_pos_st = struct.Struct(">iii").pack
    pack_i32 = struct.Struct(">i").pack

    idx = 0
    for by in range(H):
        for bz in range(D):
            for bx in range(W):
                v = vox[idx]
                idx += 1
                if v:
                    out.extend(pos_bytes)
                    out.extend(pack_pos_st(bx, by, bz))
                    out.extend(state_bytes)
                    out.extend(pack_i32(v - 1))
                    u8(0)

    # End root
    u8(0)

    with gzip.open(path, "wb", compresslevel=6) as gz:
        gz.write(out)
    print(f"Saved upgraded {path}: {nonzero_count:,} blocks, {len(new_palette)} palette states.")


def build_castle_interiors():
    vox, palette, size_vec, data_version = load_template(NBT_PATH)
    pal_map = {entry: idx + 1 for idx, entry in enumerate(palette)}

    def sid(bname: str, **props) -> int:
        key = (bname, tuple(sorted((k, str(v)) for k, v in props.items())))
        if key not in pal_map:
            palette.append(key)
            pal_map[key] = len(palette)
        return pal_map[key]

    AIR = 0
    PBB = sid("minecraft:polished_blackstone_bricks")
    CPBB = sid("minecraft:chiseled_polished_blackstone")
    CRK_PBB = sid("minecraft:cracked_polished_blackstone_bricks")
    GILDED = sid("minecraft:gilded_blackstone")
    GOLD = sid("minecraft:gold_block")
    DEEP_TILES = sid("minecraft:deepslate_tiles")
    DEEP_BRICKS = sid("minecraft:deepslate_bricks")
    BASALT_Y = sid("minecraft:polished_basalt", axis="y")
    STONE_BRK = sid("minecraft:stone_bricks")
    CHIS_STONE = sid("minecraft:chiseled_stone_bricks")
    ASHLAR_PIL = sid("riftworld:ashlar_pillar", axis="y")
    DARK_PLK = sid("riftworld:dark_planks")
    DARK_BEAM_X = sid("riftworld:dark_beam", axis="x")
    DARK_BEAM_Z = sid("riftworld:dark_beam", axis="z")
    BOOKSHELF = sid("riftworld:castle_bookshelf", axis="y")
    BARS = sid("minecraft:iron_bars", north="false", east="false", south="false", west="false", waterlogged="false")
    BARS_NS = sid("minecraft:iron_bars", north="true", east="false", south="true", west="false", waterlogged="false")
    BARS_EW = sid("minecraft:iron_bars", north="false", east="true", south="false", west="true", waterlogged="false")
    PBB_WALL = sid("minecraft:polished_blackstone_brick_wall", up="true", north="none", east="none", south="none", west="none", waterlogged="false")
    CHAIN_Y = sid("minecraft:chain", axis="y", waterlogged="false")
    LANTERN_HANG = sid("minecraft:lantern", hanging="true", waterlogged="false")
    LANTERN_FLOOR = sid("minecraft:lantern", hanging="false", waterlogged="false")
    SOUL_FIRE = sid("minecraft:campfire", facing="north", lit="true", signal_fire="false", waterlogged="false")

    # Glowing stained glass for upgraded lancet windows!
    GLOW_GLASS_NS = sid("riftworld:stained_glass", north="true", south="true", east="false", west="false", waterlogged="false")
    GLOW_GLASS_EW = sid("riftworld:stained_glass", north="false", south="false", east="true", west="true", waterlogged="false")
    RED_GLASS = sid("minecraft:red_stained_glass")
    GOLD_GLASS = sid("minecraft:yellow_stained_glass")
    GRAY_GLASS = sid("minecraft:gray_stained_glass")

    # Connected Carpets & Rugs!
    CARPET = sid("riftworld:castle_carpet", north="false", east="false", south="false", west="false")
    RUG = sid("riftworld:rug", facing="south", powered="false", broken="false")

    # Banners / Wool accents
    RED_WOOL = sid("minecraft:red_wool")
    YELLOW_WOOL = sid("minecraft:yellow_wool")
    BARREL_UP = sid("minecraft:barrel", facing="up", open="false")

    def pbb_stair(facing: str, half: str = "bottom") -> int:
        return sid("minecraft:polished_blackstone_brick_stairs", facing=facing, half=half, shape="straight", waterlogged="false")

    def deep_stair(facing: str, half: str = "bottom") -> int:
        return sid("minecraft:deepslate_tile_stairs", facing=facing, half=half, shape="straight", waterlogged="false")

    def pbb_slab(slab_type: str = "bottom") -> int:
        return sid("minecraft:polished_blackstone_brick_slab", type=slab_type, waterlogged="false")

    # Castle Props (static baked OBJ ModelBlocks with exact multi-cell part placement!)
    PART_SOLID = sid("riftworld:part", open="false")
    PART_OPEN = sid("riftworld:part", open="true")

    def get(lx: int, by: int, lz: int) -> int:
        bx, bz = lx - OX, lz - OZ
        if not (0 <= bx < W and 0 <= by < H and 0 <= bz < D):
            return 0
        return vox[(by * D + bz) * W + bx]

    def put(lx: int, by: int, lz: int, state_id: int):
        # Strictly protect sea/seabed (by <= 24) and Ashen shore bridge (lx <= -80 and lz >= 60)
        if by <= 24:
            return
        if lx <= -80 and lz >= 60:
            return
        bx, bz = lx - OX, lz - OZ
        if 0 <= bx < W and 0 <= by < H and 0 <= bz < D:
            vox[(by * D + bz) * W + bx] = state_id

    def fill(x0: int, y0: int, z0: int, x1: int, y1: int, z1: int, state_id: int):
        for by in range(min(y0, y1), max(y0, y1) + 1):
            for lz in range(min(z0, z1), max(z0, z1) + 1):
                for lx in range(min(x0, x1), max(x0, x1) + 1):
                    put(lx, by, lz, state_id)

    def place_prop(name: str, lx: int, by: int, lz: int, facing: str = "south", powered: bool = True,
                   x0: int = 0, x1: int = 0, back: int = 0, h: int = 1, passable: bool = False):
        st = sid(f"riftworld:{name}", facing=facing, powered="true" if powered else "false", broken="false")
        put(lx, by, lz, st)
        part = PART_OPEN if passable else PART_SOLID
        for xo in range(x0, x1 + 1):
            for bo in range(0, back + 1):
                for yo in range(0, h):
                    if xo == 0 and bo == 0 and yo == 0:
                        continue
                    zr = -bo
                    if facing == "north":
                        dx, dz = -xo, -zr
                    elif facing == "west":
                        dx, dz = -zr, xo
                    elif facing == "east":
                        dx, dz = zr, -xo
                    else:
                        dx, dz = xo, zr
                    put(lx + dx, by + yo, lz + dz, part)

    # =========================================================================
    # 0. UPGRADE EXTERIOR WOODEN TOWER ROOFS -> BLACKSTONE FLAGSTONES
    # =========================================================================
    spruce_ids = {i + 1 for i, (bn, _) in enumerate(palette) if bn == "minecraft:spruce_planks"}
    for by in range(66, 96):
        for lz in range(-95, 100):
            for lx in range(-105, 105):
                if get(lx, by, lz) in spruce_ids:
                    # Replace exposed rooftop spruce discs with dark blackstone/deepslate tiles
                    if get(lx, by + 1, lz) == AIR and get(lx, by + 2, lz) == AIR:
                        put(lx, by, lz, DEEP_TILES if ((lx + lz) & 3) == 0 else PBB)

    # =========================================================================
    # 1. GRAND COURTYARD & CURTAIN-WALL RAMPART STAIRCASES (100% WALKABLE!)
    # =========================================================================
    def build_straight_staircase(x0: int, x1: int, start_y: int, end_y: int, start_pos: int, axis: str, step_dir: int):
        """
        Builds a monumental stone staircase with balustrades & 5-block clear headroom!
        axis='z': steps advance along z = start_pos + k * step_dir
        axis='x': steps advance along x = start_pos + k * step_dir
        """
        steps = abs(end_y - start_y)
        up = 1 if end_y >= start_y else -1
        if axis == "z":
            stair_face = ("south" if step_dir > 0 else "north") if up > 0 else ("north" if step_dir > 0 else "south")
            for k in range(steps + 1):
                cy = start_y + k * up
                cz = start_pos + k * step_dir
                for lx in range(x0 - 1, x1 + 2):
                    for fy in range(min(46, cy - 4), cy):
                        put(lx, fy, cz, PBB)
                    if lx == x0 - 1 or lx == x1 + 1:
                        put(lx, cy, cz, PBB)
                        put(lx, cy + 1, cz, PBB_WALL)
                        for ay in range(cy + 2, cy + 6):
                            put(lx, ay, cz, AIR)
                        if k % 5 == 0:
                            put(lx, cy + 2, cz, LANTERN_FLOOR)
                    else:
                        put(lx, cy, cz, pbb_stair(stair_face) if k < steps else PBB)
                        for ay in range(cy + 1, cy + 6):
                            put(lx, ay, cz, AIR)
        else:
            stair_face = ("east" if step_dir > 0 else "west") if up > 0 else ("west" if step_dir > 0 else "east")
            for k in range(steps + 1):
                cy = start_y + k * up
                cx = start_pos + k * step_dir
                for lz in range(x0 - 1, x1 + 2):
                    for fy in range(min(46, cy - 4), cy):
                        put(cx, fy, lz, PBB)
                    if lz == x0 - 1 or lz == x1 + 1:
                        put(cx, cy, lz, PBB)
                        put(cx, cy + 1, lz, PBB_WALL)
                        for ay in range(cy + 2, cy + 6):
                            put(cx, ay, lz, AIR)
                        if k % 5 == 0:
                            put(cx, cy + 2, lz, LANTERN_FLOOR)
                    else:
                        put(cx, cy, lz, pbb_stair(stair_face) if k < steps else PBB)
                        for ay in range(cy + 1, cy + 6):
                            put(cx, ay, lz, AIR)

    # Grand South Plaza Staircase: Lower Courtyard (lz=43, by=48) -> Upper Cathedral Plaza (lz=28, by=63)
    build_straight_staircase(4, 10, 48, 63, 43, axis="z", step_dir=-1)
    # Pave smooth plaza promenade from top of South Staircase (lz=28) straight to Cathedral South Portal (lz=-4)
    for lz in range(-3, 29):
        for lx in range(4, 11):
            put(lx, 63, lz, GILDED if (lx in (4, 10) and lz % 4 == 0) else PBB)
            for ay in range(64, 70):
                put(lx, ay, lz, AIR)

    # Grand West Narthex Staircase: West Lower Forecourt (lx=-55, by=48) -> West Cathedral Portal (lx=-40, by=63)
    build_straight_staircase(-28, -22, 48, 63, -55, axis="x", step_dir=1)

    # 4 Curtain-Wall Rampart Access Stairways (Lower Courtyard by=48 -> Outer Rampart Walk by=60)
    build_straight_staircase(-47, -45, 48, 60, -52, axis="z", step_dir=-1)  # North Ramparts
    build_straight_staircase(22, 24, 48, 60, 44, axis="z", step_dir=1)      # South Ramparts
    build_straight_staircase(-24, -22, 48, 60, -56, axis="x", step_dir=-1)  # West Ramparts
    build_straight_staircase(18, 20, 48, 60, 48, axis="x", step_dir=1)      # East Ramparts

    # =========================================================================
    # 2. SPIRAL STAIRCASE BUILDER FOR ALL 12 OUTER PERIMETER WATCHTOWERS!
    # =========================================================================
    def build_spiral_tower(cx: int, cz: int, base_y: int, top_y: int, r_in: int = 2,
                           ground_door_dir: str = None, rampart_y: int = 60):
        """
        Carves a clean square-spiral stone staircase around a central stone newel column (cx, cz)
        from base_y up to top_y + 1 (opening onto the top battlement deck!), with intermediate landings,
        lanterns, arrow-slits, and upgraded battlement merlons + beacon finial!
        """
        # 1) Clear inner shaft & build central newel pillar with warm lanterns
        for by in range(base_y, top_y):
            fill(cx - r_in, by, cz - r_in, cx + r_in, by, cz + r_in, AIR)
            put(cx, by, cz, GILDED if by % 6 == 0 else PBB)

        # Solid base floor
        fill(cx - r_in - 1, base_y - 1, cz - r_in - 1, cx + r_in + 1, base_y - 1, cz + r_in + 1, PBB)

        # Intermediate Rampart-level landing ring (at rampart_y) if within shaft
        if base_y + 5 <= rampart_y <= top_y - 4:
            fill(cx - r_in, rampart_y, cz - r_in, cx + r_in, rampart_y, cz + r_in, DARK_PLK)
            put(cx, rampart_y, cz, PBB)

        # Build perimeter clockwise spiral path around radius r_s = min(r_in, 2)
        rs = min(r_in, 2)
        # Perimeter ring of 16 cells (for rs=2) or 8 cells (for rs=1)
        ring = []
        for x in range(-rs, rs):
            ring.append((x, -rs, "east"))
        for z in range(-rs, rs):
            ring.append((rs, z, "south"))
        for x in range(rs, -rs, -1):
            ring.append((x, rs, "west"))
        for z in range(rs, -rs, -1):
            ring.append((-rs, z, "north"))

        idx = 0
        cur_y = base_y
        while cur_y <= top_y:
            rx, rz, face = ring[idx % len(ring)]
            wx, wz = cx + rx, cz + rz
            is_corner = (abs(rx) == rs and abs(rz) == rs)
            # Clear 4 blocks of headroom above every stair step (punches clean hatch through roof!)
            for hy in range(1, 5):
                if cur_y + hy <= top_y + 4:
                    put(wx, cur_y + hy, wz, AIR)
            if is_corner:
                put(wx, cur_y, wz, PBB)
                idx += 1  # flat corner landing before rising on next straight run
            else:
                put(wx, cur_y, wz, pbb_stair(face))
                cur_y += 1
                idx += 1
            if cur_y % 8 == 0:
                put(cx + (1 if rx >= 0 else -1), min(cur_y + 2, top_y - 1), cz, LANTERN_HANG)

        # Ensure roof deck around stair hatch is paved & crenellated with gothic beacon spirelet!
        for dx in range(-r_in - 2, r_in + 3):
            for dz in range(-r_in - 2, r_in + 3):
                if get(cx + dx, top_y, cz + dz) != AIR:
                    put(cx + dx, top_y, cz + dz, DEEP_TILES if ((dx + dz) & 1) == 0 else PBB)
                # Perimeter battlement parapet on outer ring of tower roof
                if max(abs(dx), abs(dz)) == r_in + 2 and abs(dx) + abs(dz) <= 2 * r_in + 3:
                    if get(cx + dx, top_y - 1, cz + dz) != AIR:
                        put(cx + dx, top_y, cz + dz, PBB)
                        put(cx + dx, top_y + 1, cz + dz, CPBB if ((dx + dz) & 1) == 0 else PBB_WALL)
                        if dx == 0 or dz == 0:
                            put(cx + dx, top_y + 2, cz + dz, PBB_WALL)

        # Central Gothic Beacon Finial atop the tower newel (rises 5 blocks above roof deck without blocking walk!)
        for fy in range(top_y, top_y + 4):
            put(cx, fy, cz, GILDED if fy == top_y + 2 else PBB_WALL)
        put(cx, top_y + 4, cz, LANTERN_FLOOR)

        # Carve arched Ground Courtyard Doorway facing inward toward castle courtyard!
        if ground_door_dir:
            ddx, ddz = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}[ground_door_dir]
            for step in range(1, r_in + 6):
                px, pz = cx + ddx * step, cz + ddz * step
                # Paving under doorway + 3-wide x 4-tall arched opening
                for w in (-1, 0, 1):
                    ox = px + (w if ddx == 0 else 0)
                    oz = pz + (w if ddz == 0 else 0)
                    put(ox, base_y - 1, oz, PBB)
                    for dy in range(0, 4 if w == 0 else 3):
                        put(ox, base_y + dy, oz, AIR)

    outer_towers = [
        (-9, -78, 48, 83, 2, "south"),
        (-32, -74, 48, 74, 2, "south"),
        (-55, -69, 48, 80, 2, "south"),
        (48, -66, 48, 77, 2, "south"),
        (-76, -43, 48, 77, 2, "east"),
        (79, -39, 48, 80, 2, "west"),
        (85, -20, 48, 74, 2, "west"),
        (91, -1, 48, 83, 2, "west"),
        (78, 36, 48, 77, 2, "west"),
        (62, 47, 48, 74, 2, "north"),
        (47, 58, 48, 80, 2, "north"),
        (-6, 60, 48, 83, 2, "north"),
    ]
    for cx, cz, by0, top_y, rin, ddir in outer_towers:
        build_spiral_tower(cx, cz, by0, top_y, rin, ground_door_dir=ddir, rampart_y=60)

    # =========================================================================
    # 3. SW SEA-BRIDGE GATEHOUSE BARBICAN & TWIN TOWERS ((-65, +35), up to by=88)
    # =========================================================================
    # Carve Upper Barbican War-Room at by=72..78 (lx=-73..-57, lz=27..43)
    fill(-73, 71, 27, -57, 71, 43, PBB)
    fill(-72, 72, 28, -58, 77, 42, AIR)
    # Gothic arched windows overlooking the sea-bridge & courtyard
    for wz in (31, 35, 39):
        for wy in range(73, 77):
            put(-73, wy, wz, GLOW_GLASS_NS)
            put(-57, wy, wz, GLOW_GLASS_NS)
    # Furnish Barbican War-Room with connected L-shaped woven rug, war table, armor & weapons!
    for rx in range(-69, -62):
        for rz in range(32, 36):
            put(rx, 72, rz, RUG)
    for rx in range(-65, -62):
        for rz in range(36, 40):
            put(rx, 72, rz, RUG)  # L-wing showcasing connected L-shape rug!
    place_prop("long_table", -67, 72, 34, facing="south", powered=True, x0=-1, x1=1, back=0, h=1)
    place_prop("high_chair", -67, 72, 32, facing="south", powered=False, h=2)
    place_prop("guard_armor", -71, 72, 30, facing="east", powered=False, h=2)
    place_prop("guard_armor", -71, 72, 40, facing="east", powered=False, h=2)
    place_prop("guard_weapon_rack", -60, 72, 30, facing="south", powered=False, x0=-1, x1=1, h=3)
    put(-65, 77, 35, CHAIN_Y)
    place_prop("castle_chandelier", -65, 76, 35, facing="south", powered=True, passable=True)

    # Spiral stairs up BOTH Gatehouse Towers (-72, +35) from by=48 -> 72 -> 87 AND (-58, +35) from by=72 -> 87!
    build_spiral_tower(-72, 30, 48, 87, r_in=2, ground_door_dir="east", rampart_y=60)
    # Connect (-72, 30) shaft at by=72 directly into Barbican War-Room
    fill(-71, 72, 31, -69, 75, 33, AIR)
    build_spiral_tower(-58, 35, 72, 87, r_in=2, ground_door_dir="west", rampart_y=72)

    # =========================================================================
    # 4. SOUTH ASTRAL ROTUNDA / CHAPEL SANCTUM & TWIN SPIRES ((-25, +22), ref 04!)
    # =========================================================================
    # Clear & sculpt vaulted Rotunda interior (lx=-37..-13, lz=+10..+34, floor by=63, dome to by=81)
    for lz in range(10, 35):
        for lx in range(-37, -12):
            dist = math.hypot(lx - (-25), lz - 22)
            if dist <= 11.8:
                # Concentric dark-slate & gilded floor ring pattern (04_citadel_hall_rotunda_sanctum.png)
                ring_d = round(dist)
                if ring_d in (4, 8):
                    put(lx, 63, lz, GILDED if ((lx + lz) & 1) == 0 else CPBB)
                else:
                    put(lx, 63, lz, DEEP_TILES if ((lx + lz) & 1) == 0 else PBB)
                dome_ceil = int(81 - max(0.0, (dist - 4.0) * 0.65))
                for by in range(64, dome_ceil):
                    put(lx, by, lz, AIR)
                # Gilded vault ribs overhead
                if abs(lx - (-25)) <= 1 or abs(lz - 22) <= 1:
                    put(lx, dome_ceil, lz, GILDED)

    # North Entrance Archway from Upper Keep Plaza (lx=-27..-23, lz=7..11, by=64..70)
    fill(-27, 63, 7, -23, 63, 11, PBB)
    for lz in range(7, 12):
        for lx in range(-27, -22):
            max_ay = 70 if abs(lx - (-25)) <= 1 else 68
            for by in range(64, max_ay):
                put(lx, by, lz, AIR)

    # Seamless Connected +-Cross Royal Carpet inside the Rotunda + Raised Sanctum Altar!
    for d in range(-8, 9):
        for w in (-1, 0, 1):
            put(-25 + w, 64, 22 + d, CARPET)
            put(-25 + d, 64, 22 + w, CARPET)
    fill(-26, 64, 21, -24, 64, 23, CPBB)
    place_prop("chapel_altar", -25, 65, 22, facing="north", powered=True, x0=-1, x1=1, h=2)
    for sx, sz in [(-29, 18), (-21, 18), (-29, 26), (-21, 26)]:
        place_prop("candle_stand", sx, 64, sz, facing="north", powered=True, h=2)
    place_prop("chained_lectern", -25, 64, 17, facing="north", powered=False, h=2)

    # Rotunda Scriptorium Bookshelf Arcades & Chandelier
    for bz in range(18, 27, 2):
        for by in range(64, 68):
            put(-35, by, bz, BOOKSHELF)
            put(-15, by, bz, BOOKSHELF)
    for cy in range(75, 81):
        put(-25, cy, 22, CHAIN_Y)
    place_prop("castle_chandelier", -25, 74, 22, facing="south", powered=True, passable=True)

    # Spiral stairs up BOTH Rotunda Twin Spires (-29, +13) and (-21, +31) inside their natural stone shafts!
    build_spiral_tower(-29, 13, 64, 90, r_in=2, ground_door_dir="south", rampart_y=76)
    build_spiral_tower(-21, 31, 64, 90, r_in=2, ground_door_dir="north", rampart_y=76)

    # =========================================================================
    # 5. THE GRAND BLACK CATHEDRAL THRONE HALL (03_citadel_hall_cathedral_throne.png!)
    #    Spanning lx = -37..+49, lz = -46..-5 (East Chancel lx>=37 spans lz=-33..-9), vaults to by = 98!
    # =========================================================================

    # 5A) Clear hollow Cathedral cleanly while preserving sculpted East Chancel exterior walls (lx>=37)!
    for lz in range(-46, -4):
        in_nave = (-32 <= lz <= -18)
        for lx in range(-37, 50):
            if lx >= 37 and not (-33 <= lz <= -9):
                continue
            # Keep solid foundation below floor (except Undercroft stairwell carved later)
            put(lx, 62, lz, PBB)
            if in_nave:
                # Rich dark-slate & gilded geometric nave inlay
                is_rib = ((lx % 8 == 0) or lz in (-32, -18))
                put(lx, 63, lz, GILDED if (is_rib and (lx + lz) % 4 == 0) else (DEEP_TILES if ((lx + lz) & 1) == 0 else PBB))
                ceil_y = 97 if lx < 37 else 93
                for by in range(64, ceil_y):
                    put(lx, by, lz, AIR)
            else:
                # Side aisles raised +1 block (by=64) with dark-stone steps down into the sunken nave!
                put(lx, 63, lz, PBB)
                if lz == -33:
                    put(lx, 64, lz, pbb_stair("north"))
                elif lz == -17:
                    put(lx, 64, lz, pbb_stair("south"))
                else:
                    put(lx, 64, lz, DEEP_BRICKS if ((lx + lz) & 1) == 0 else PBB)
                for by in range(65, 92 if lx < 37 else 88):
                    put(lx, by, lz, AIR)

    # 5B) Grand Portals: South Courtyard Portal (lx=+4..+10, lz=-4) & West Ceremonial Portal (lx=-38, lz=-28..-22)
    for lx in range(4, 11):
        max_py = 74 - abs(lx - 7)
        for lz in range(-6, -2):
            put(lx, 63, lz, PBB)
            for by in range(64, max_py):
                put(lx, by, lz, AIR)
    for lz in range(-28, -21):
        max_py = 75 - abs(lz - (-25))
        for lx in range(-41, -36):
            put(lx, 63, lz, PBB)
            for by in range(64, max_py):
                put(lx, by, lz, AIR)

    # 5C) Walkable Upper Triforium Galleries (by=76) along North Aisle, South Aisle & West Choir Loft!
    for lx in range(-37, 36):
        # North Gallery (lz=-46..-34)
        for lz in range(-46, -33):
            put(lx, 75, lz, DEEP_TILES)
            put(lx, 76, lz, DARK_PLK if -44 <= lz <= -36 else PBB)
            if lz == -34:
                # Ornate gothic stone & iron-bar balustrade overlooking the Nave!
                put(lx, 77, lz, CPBB if lx % 4 == 0 else BARS_EW)
                if lx % 8 == 4:
                    put(lx, 78, lz, LANTERN_FLOOR)
        # South Gallery (lz=-16..-5)
        for lz in range(-16, -4):
            put(lx, 75, lz, DEEP_TILES)
            put(lx, 76, lz, DARK_PLK if -14 <= lz <= -7 else PBB)
            if lz == -16:
                put(lx, 77, lz, CPBB if lx % 4 == 0 else BARS_EW)
                if lx % 8 == 4:
                    put(lx, 78, lz, LANTERN_FLOOR)

    # West Choir / Organ Loft bridge connecting North & South Galleries at lx=-37..-30
    for lx in range(-37, -29):
        for lz in range(-33, -16):
            put(lx, 75, lz, DEEP_TILES)
            put(lx, 76, lz, RUG if (-35 <= lx <= -32 and -30 <= lz <= -20) else PBB)
            if lx == -30:
                put(lx, 77, lz, CPBB if lz % 4 == 0 else BARS_NS)

    # 5D) 16 Towering Clustered Gothic Columns with Gold Capitals (03_citadel_hall_cathedral_throne.png!)
    col_xs = [-28, -20, -12, -4, 4, 12, 20, 28]
    for cx in col_xs:
        for cz in (-33, -17):
            # Stepped dark-basalt plinth (by=64..66)
            for dx in range(-1, 2):
                for dz in range(-1, 2):
                    put(cx + dx, 64, cz + dz, PBB)
                    if abs(dx) + abs(dz) <= 1:
                        put(cx + dx, 65, cz + dz, CPBB)
            # Fluted clustered shaft (dark core + pale ashlar colonnettes, by=66..86)
            for by in range(66, 87):
                put(cx, by, cz, DEEP_TILES)
                if by == 75:
                    # Mid-shaft gilded ring band at gallery level
                    for dx, dz in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
                        put(cx + dx, by, cz + dz, GILDED)
                else:
                    for dx, dz in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
                        put(cx + dx, by, cz + dz, ASHLAR_PIL)
            # Burnished Gold Gothic Capital (by=87..88)
            for dx in range(-1, 2):
                for dz in range(-1, 2):
                    if abs(dx) + abs(dz) <= 1:
                        put(cx + dx, 87, cz + dz, GOLD)
                    put(cx + dx, 88, cz + dz, GILDED if abs(dx) + abs(dz) <= 1 else PBB)

            # Black-iron Guard Armor sentinel + lit Candle Stand facing the Central Nave!
            nave_dz = 2 if cz == -33 else -2
            nave_face = "south" if cz == -33 else "north"
            if cx in (-20, -4, 12, 28):
                place_prop("guard_armor", cx, 64, cz + nave_dz, facing=nave_face, powered=False, h=2)
            else:
                place_prop("candle_stand", cx, 64, cz + nave_dz, facing=nave_face, powered=True, h=2)

    # Lower arcade pointed arches between columns (by=72..75)
    for i in range(len(col_xs) - 1):
        x_a, x_b = col_xs[i], col_xs[i + 1]
        mid_x = (x_a + x_b) // 2
        for lx in range(x_a + 1, x_b):
            arch_h = 74 - max(0, 2 - abs(lx - mid_x))
            for cz in (-33, -17):
                for by in range(arch_h, 76):
                    put(lx, by, cz, GILDED if (lx == mid_x and by == arch_h) else PBB)

    # 5E) Soaring Dark-Slate Ribbed Gothic Cross-Vaults with Gold Ribs (by=89..97)
    #     (Leaves Crossing Oculus lx=6..18, lz=-31..-19 open into the towering Lantern Spire!)
    for lx in range(-36, 48):
        for lz in range(-33, -16):
            if 6 <= lx <= 18 and -31 <= lz <= -19:
                continue  # Central Crossing Lantern Oculus!
            dist_wall = min(lz - (-33), (-17) - lz)  # 0 at column line -> 8 at nave centerline (lz=-25)
            vault_y = 89 + int(round(math.sin((dist_wall / 8.0) * (math.pi / 2.0)) * 8.0))
            put(lx, vault_y, lz, DEEP_TILES)
            put(lx, vault_y + 1, lz, PBB)
            # Transverse & diagonal X-ribs in Gilded Blackstone / Gold!
            bay_x = (lx + 36) % 8  # 0..7
            diag1 = abs(bay_x - (dist_wall % 8)) <= 0
            diag2 = abs((7 - bay_x) - (dist_wall % 8)) <= 0
            if lx in col_xs or diag1 or diag2 or lz == -25:
                put(lx, vault_y - 1, lz, GOLD if (lz == -25 and lx in col_xs) else GILDED)

    # Grand Multi-Tiered Wrought-Iron Chandeliers down the Cathedral Nave!
    for ch_x in (-24, -16, -8, 0, 24, 32):
        apex_y = 95
        for cy in range(82, apex_y + 1):
            put(ch_x, cy, -25, CHAIN_Y)
        # Iron wheel chandelier frame + glowing CASTLE_CHANDELIER + 4 outer lanterns
        for dx, dz in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            put(ch_x + dx, 82, -25 + dz, CPBB)
            put(ch_x + dx, 81, -25 + dz, LANTERN_HANG)
        place_prop("castle_chandelier", ch_x, 81, -25, facing="south", powered=True, passable=True)

    # 5F) Upgraded Pointed-Arch Gothic Stained-Glass Lancet Windows (North, South & East Apse!)
    for bay_x in (-24, -16, -8, 0, 16, 24, 32):
        for dx in range(-2, 3):
            max_wy = 90 - abs(dx) * 2
            # Clerestory lancets (by=79..max_wy) & Ground aisle lancets (by=66..73)
            for wy in list(range(66, 73 - abs(dx))) + list(range(79, max_wy)):
                glass = GOLD_GLASS if (dx == 0 and wy % 4 == 0) else (RED_GLASS if abs(dx) == 2 else GLOW_GLASS_EW)
                put(bay_x + dx, wy, -47, glass)
                put(bay_x + dx, wy, -48, AIR if abs(dx) <= 1 else PBB)
                put(bay_x + dx, wy, -4, glass)
    # Towering East Apse Trio of Lancet Windows behind the Royal Throne (lx=49..51, lz=-31..-19, by=72..92)
    for center_z in (-29, -25, -21):
        for dz in range(-1, 2):
            top_w = (93 if center_z == -25 else 89) - abs(dz) * 2
            for wy in range(72, top_w):
                glass = GOLD_GLASS if (dz == 0 and wy % 3 == 0) else (RED_GLASS if abs(dz) == 1 else GLOW_GLASS_NS)
                put(50, wy, center_z + dz, glass)
                put(51, wy, center_z + dz, AIR)
    # Matching pointed stained-glass lancets on North & South Chancel walls (lx=41..45 at lz=-34 and lz=-8)
    for dx in range(-2, 3):
        for wy in range(73, 87 - abs(dx) * 2):
            glass = GOLD_GLASS if (dx == 0 and wy % 4 == 0) else (RED_GLASS if abs(dx) == 2 else GLOW_GLASS_EW)
            put(43 + dx, wy, -34, glass)
            put(43 + dx, wy, -8, glass)

    # 5G) Seamless Connected +-Cross Royal Carpet Runner + Monumental Stepped Throne Sanctuary!
    # Main Nave Royal Runner (7 blocks wide: lz=-28..-22, lx=-35..+33)
    for lx in range(-35, 34):
        for lz in range(-28, -21):
            put(lx, 64, lz, CARPET)
    # South Transept Branch to Courtyard Portal (lx=+5..+9, lz=-21..-4) -> Seamless T/+ inner corners!
    for lz in range(-21, -3):
        for lx in range(5, 10):
            put(lx, 65 if lz <= -17 and lz != -4 else 64, lz, CARPET)
    # North Transept Branch (lx=+5..+9, lz=-44..-28)
    for lz in range(-44, -28):
        for lx in range(5, 10):
            put(lx, 65 if lz <= -33 else 64, lz, CARPET)

    # Monumental 6-Step Royal Throne Apse Dais (lx=34..49, lz=-32..-18, rising by=64 -> 69)
    for lx in range(34, 50):
        step_h = min(6, (lx - 32) // 2)  # 1..6 -> top surface at 63 + step_h (up to 69)
        top_s = 63 + step_h
        is_riser = ((lx - 32) % 2 == 0) and step_h <= 6 and lx <= 44
        for lz in range(-32, -17):
            for fy in range(63, top_s + 1):
                put(lx, fy, lz, GILDED if (lz in (-32, -18) and fy == top_s) else PBB)
            if is_riser and -31 <= lz <= -19:
                put(lx, top_s, lz, pbb_stair("east"))
            elif -28 <= lz <= -22 and lx <= 47:
                # Continuous connected Royal Carpet climbing every terrace of the Throne Dais!
                put(lx, top_s + 1, lz, CARPET)

    # 4 Towering Golden Fire Braziers flanking the Throne Steps (03_citadel_hall_cathedral_throne.png!)
    for bx_p, by_p in [(36, 65), (42, 68)]:
        for bz_p in (-31, -19):
            put(bx_p, by_p, bz_p, CPBB)
            put(bx_p, by_p + 1, bz_p, GILDED)
            put(bx_p, by_p + 2, bz_p, GOLD)
            put(bx_p, by_p + 3, bz_p, SOUL_FIRE)

    # Tall Crimson & Gold Royal Heraldic Standards flanking the Throne Apse
    for bz_b in (-32, -18):
        for by_b in range(72, 85):
            put(47, by_b, bz_b, YELLOW_WOOL if by_b in (72, 78, 84) else RED_WOOL)

    # Sculpted Gothic Obsidian-and-Gold Baldachin Canopy + BOSS_THRONE at (45, 70, -25) facing WEST!
    for dz in (-3, 3):
        for by_c in range(70, 81):
            put(44, by_c, -25 + dz, GOLD if by_c in (70, 79) else DEEP_TILES)
            put(48, by_c, -25 + dz, GOLD if by_c in (70, 79) else DEEP_TILES)
    for lx_c in range(44, 49):
        for lz_c in range(-28, -21):
            spire_y = 81 + (3 - max(abs(lx_c - 46), abs(lz_c - (-25))))
            put(lx_c, 80, lz_c, GILDED if (lx_c in (44, 48) or lz_c in (-28, -22)) else DEEP_TILES)
            if spire_y >= 81:
                put(lx_c, spire_y, lz_c, GOLD if (lx_c == 46 and lz_c == -25) else PBB)
    put(46, 85, -25, GOLD)
    put(46, 86, -25, LANTERN_FLOOR)
    place_prop("boss_throne", 46, 70, -25, facing="west", powered=True, x0=-1, x1=1, back=1, h=4)

    # Furnish Ground Side Aisles (Royal Banquet Gallery on South Aisle + Trophy Gallery on North Aisle!)
    for tx in (-22, -10, 18):
        place_prop("feast_table", tx, 65, -11, facing="south", powered=True, x0=-1, x1=1, h=1)
        place_prop("feast_table", tx, 65, -40, facing="north", powered=True, x0=-1, x1=1, h=1)

    # 5H) Twin West Portal Tower Staircases (by=64 -> 76 Triforium Gallery -> 90 West Belfries)
    build_spiral_tower(-34, -40, 64, 90, r_in=2, ground_door_dir="east", rampart_y=76)
    build_spiral_tower(-34, -10, 64, 90, r_in=2, ground_door_dir="east", rampart_y=76)
    # Open gallery doorways from both West Towers onto the by=76 Triforium Galleries!
    fill(-32, 77, -41, -30, 80, -39, AIR)
    fill(-32, 77, -11, -30, 80, -9, AIR)

    # 5I) SOARING CENTRAL CROSSING LANTERN SPIRE STAIRCASE (by=76 -> 92 -> 108 -> 122 -> 134 CROWN LOOKOUT!)
    # Inside NE Crossing Pier (cx=20, cz=-38 -> joins Crossing Tower lx=0..24, lz=-42..-20)
    build_spiral_tower(20, -38, 76, 108, r_in=2, ground_door_dir="south", rampart_y=92)
    # Crossing Bell & Astral Lantern Gallery at by=108 (around open central oculus lx=8..16, lz=-30..-22)
    for lx in range(1, 24):
        for lz in range(-41, -20):
            if 8 <= lx <= 16 and -33 <= lz <= -25:
                # Open central oculus with balustrade looking 45m straight down into the Throne Hall!
                if lx in (8, 16) or lz in (-33, -25):
                    put(lx, 108, lz, PBB)
                    put(lx, 109, lz, BARS)
            else:
                put(lx, 108, lz, DEEP_TILES if ((lx + lz) & 1) == 0 else PBB)
                for ay in range(109, 116):
                    if 2 <= lx <= 22 and -40 <= lz <= -21:
                        put(lx, ay, lz, AIR)
    # Upper Lantern Spiral Staircase from by=108 -> by=134 Panoramic Crown Balcony!
    build_spiral_tower(12, -37, 108, 134, r_in=2, ground_door_dir="south", rampart_y=122)
    # Carve 4 Cardinal Panoramic Arched Lookout Windows at by=134..139 in the Crown Spire!
    fill(8, 134, -35, 16, 134, -26, DEEP_TILES)
    fill(9, 135, -34, 15, 139, -27, AIR)
    for wx in range(10, 15):
        for wy in range(135, 139):
            put(wx, wy, -42, AIR); put(wx, wy, -41, AIR)
            put(wx, wy, -20, AIR); put(wx, wy, -21, AIR)
        put(wx, 135, -42, BARS_EW); put(wx, 135, -20, BARS_EW)
    for wz in range(-33, -28):
        for wy in range(135, 139):
            put(0, wy, wz, AIR); put(1, wy, wz, AIR)
            put(24, wy, wz, AIR); put(23, wy, wz, AIR)
        put(0, 135, wz, BARS_NS); put(24, 135, wz, BARS_NS)
    # Giant Golden Spire Bell suspended inside the Crossing Oculus (by=118..122 at lx=12, lz=-29)
    for cy in range(123, 133):
        put(12, cy, -29, CHAIN_Y)
    fill(11, 120, -30, 13, 122, -28, GOLD)
    put(12, 119, -29, LANTERN_HANG)

    # 5J) Vaulted Undercroft Wine-Cellar, Kitchen & Brig beneath the Nave (by=53..60)
    fill(-18, 52, -33, 14, 52, -17, PBB)
    fill(-17, 53, -32, 13, 59, -18, AIR)
    fill(-18, 60, -33, 14, 61, -17, DEEP_TILES)
    # Vaulted cellar pillars & lanterns
    for ux in (-12, -4, 4, 10):
        for uz in (-29, -21):
            for uy in range(53, 60):
                put(ux, uy, uz, PBB)
        put(ux, 59, -25, LANTERN_HANG)
    # Connected carpet + cauldron hearth + wine barrel racks + iron-barred dungeon cell
    for ux in range(-10, 8):
        put(ux, 53, -25, RUG)
        put(ux, 53, -24, RUG)
    for bx_w in range(-15, -6, 2):
        put(bx_w, 53, -31, BARREL_UP)
        put(bx_w, 54, -31, BARREL_UP)
    place_prop("hearth_cauldron", 11, 53, -25, facing="west", powered=True, h=2)
    # Iron-barred brig cell on SE corner of Undercroft
    for bz_c in range(-21, -17):
        for by_c in range(53, 58):
            put(6, by_c, bz_c, BARS_NS)
    for bx_c in range(6, 14):
        for by_c in range(53, 58):
            if bx_c != 9 or by_c >= 56:
                put(bx_c, by_c, -21, BARS_EW)
    # Grand Descending Staircase from North Aisle (lx=-14..-3, lz=-42..-40, by=64 -> 53) into Undercroft!
    build_straight_staircase(-42, -40, 64, 53, -15, axis="x", step_dir=1)
    # Passageway from bottom of Undercroft stairs (lx=-4, lz=-41, by=53) South into Undercroft (lz=-32)
    fill(-5, 52, -43, -1, 52, -31, PBB)
    fill(-4, 53, -42, -2, 57, -31, AIR)
    put(-3, 57, -36, LANTERN_HANG)

    save_template(NBT_PATH, vox, palette, size_vec, data_version)


if __name__ == "__main__":
    build_castle_interiors()
