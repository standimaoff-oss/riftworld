#!/usr/bin/env python3
"""
Generates 32x32 8-neighbor CTM sprites for Riftworld connected carpets:
  - castle_carpet_center.png (32x32 seamless 2x2-block royal crimson & gold gothic damask)
  - castle_carpet_trim.png   (32x32 4-quadrant border/outer-corner/horiz/vert/inner-corner gold trim)
  - castle_carpet.png        (16x16 standalone 1x1 icon / particle texture)
  - rug_center.png           (32x32 seamless 2x2-block Elisar warm saffron-indigo-terracotta kilim weave)
  - rug_trim.png             (32x32 4-quadrant woven border & inner/outer corner trim)
  - rug_item.png             (16x16 standalone 1x1 icon / particle texture)
And updates the 7 city/*.nbt templates so former 2x2 multi-block rug footprints use 4 connected 1x1 riftworld:rug blocks.
"""
import gzip
import json
import math
import os
import struct
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
TEX_DIR = os.path.join(ROOT, "mod/src/main/resources/assets/riftworld/textures/block")
BS_DIR = os.path.join(ROOT, "mod/src/main/resources/assets/riftworld/blockstates")
BM_DIR = os.path.join(ROOT, "mod/src/main/resources/assets/riftworld/models/block")
IM_DIR = os.path.join(ROOT, "mod/src/main/resources/assets/riftworld/models/item")
CITY_NBT_DIR = os.path.join(ROOT, "mod/src/main/resources/data/riftworld/structures/city")


def write_png(path: str, w: int, h: int, pixels):
    raw_rows = bytearray()
    for y in range(h):
        raw_rows.append(0)  # filter type 0
        for x in range(w):
            r, g, b, a = pixels[y][x]
            raw_rows.extend((max(0, min(255, int(r))),
                             max(0, min(255, int(g))),
                             max(0, min(255, int(b))),
                             max(0, min(255, int(a)))))

    def chunk(tag: bytes, data: bytes) -> bytes:
        crc = zlib.crc32(tag + data) & 0xFFFFFFFF
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", crc)

    ihdr = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(bytes(raw_rows), 9)) + chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)


def hnoise(x: int, y: int, seed: int = 0) -> float:
    n = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def castle_weave_pixel(x: int, y: int):
    """32x32 seamless royal crimson & dark burgundy gothic damask with subtle gold thread."""
    # Warp/weft wool micro-grain
    grain = (hnoise(x % 32, y % 32, 17) - 0.5) * 9.0
    twill = 3.5 if ((x + y) & 1) == 0 else -3.5

    # Seamless 16-px diamond lattice + 32-px gothic rosette medallion
    dx16 = abs((x % 16) - 7.5)
    dy16 = abs((y % 16) - 7.5)
    lattice = abs((dx16 + dy16) - 7.0)

    dx32 = abs((x % 32) - 15.5)
    dy32 = abs((y % 32) - 15.5)
    manh32 = dx32 + dy32

    r, g, b = 132.0, 22.0, 30.0  # Rich royal crimson velvet base
    if lattice < 0.85:
        # Subtle dark burgundy + warm gold brocade thread along diamond lattice
        r, g, b = 158.0, 48.0, 36.0
    elif lattice < 1.65:
        r, g, b = 104.0, 15.0, 23.0

    # Delicate gold-crimson fleur-de-lis / gothic quatrefoil accents at 16px & 32px centers
    if (dx16 < 1.6 and dy16 < 1.6) or (abs(manh32 - 4.0) < 0.75 and max(dx32, dy32) < 3.6):
        r, g, b = 188.0, 132.0, 52.0
    elif dx16 < 2.7 and dy16 < 2.7 and (dx16 + dy16) < 3.5:
        r, g, b = 154.0, 36.0, 34.0

    return (r + grain + twill, g + grain * 0.45 + twill * 0.4, b + grain * 0.45 + twill * 0.4, 255)


def castle_border_pixel(dist_edge: int, along: int, is_corner_jewel: bool = False):
    """dist_edge = 0 (outermost), 1 (gold Greek-key braid), 2 (inner dark-crimson/gold piping)."""
    grain = (hnoise(along, dist_edge, 91) - 0.5) * 8.0
    if is_corner_jewel:
        return (232 + grain, 188 + grain, 78 + grain, 255)
    if dist_edge == 0:
        # Outer dark burgundy-gold hem with fringe rhythm
        fringe = 14.0 if (along & 1) == 0 else -6.0
        return (92 + fringe + grain, 24 + fringe * 0.5 + grain * 0.4, 24 + grain * 0.4, 255)
    elif dist_edge == 1:
        # Bright burnished imperial gold braid
        braid = 16.0 if ((along // 2) & 1) == 0 else -10.0
        return (218 + braid + grain, 170 + braid * 0.8 + grain, 64 + braid * 0.4 + grain * 0.5, 255)
    else:
        # Inner dark velvet & gold beading line separating border from center field
        bead = 38.0 if (along % 3 == 1) else 0.0
        return (84 + bead + grain, 16 + bead * 0.7 + grain * 0.3, 22 + grain * 0.3, 255)


def rug_weave_pixel(x: int, y: int):
    """32x32 seamless Elisar woven tapestry rug (warm terracotta, cream-gold, deep indigo)."""
    grain = (hnoise(x % 32, y % 32, 53) - 0.5) * 10.0
    weft = 4.5 if (y & 1) == 0 else -3.5  # Horizontal kilim weave ribs

    dx16 = abs((x % 16) - 7.5)
    dy16 = abs((y % 16) - 7.5)
    step_d = round(dx16) + round(dy16)

    # Warm terracotta-ochre woven wool field
    r, g, b = 158.0, 74.0, 48.0
    if step_d in (5, 6):
        # Deep Elisar twilight-indigo geometric kilim lozenge
        r, g, b = 54.0, 66.0, 96.0
    elif step_d == 4:
        # Warm parchment-cream outline
        r, g, b = 216.0, 190.0, 138.0
    elif step_d <= 2:
        # Saffron-gold diamond heart
        r, g, b = 204.0, 148.0, 66.0

    return (r + grain + weft, g + grain * 0.7 + weft * 0.6, b + grain * 0.6 + weft * 0.5, 255)


def rug_border_pixel(dist_edge: int, along: int, is_corner_jewel: bool = False):
    grain = (hnoise(along, dist_edge, 131) - 0.5) * 8.0
    if is_corner_jewel:
        return (230 + grain, 198 + grain, 128 + grain, 255)
    if dist_edge == 0:
        # Woven cream-ochre tassel fringe along outer edge
        tassel = 22.0 if (along & 1) == 0 else -14.0
        return (188 + tassel + grain, 158 + tassel * 0.85 + grain, 108 + tassel * 0.7 + grain, 255)
    elif dist_edge == 1:
        # Deep indigo & gold geometric kilim border band
        zig = ((along // 2) & 1) == 0
        if zig:
            return (214 + grain, 168 + grain, 82 + grain, 255)
        return (48 + grain * 0.5, 58 + grain * 0.5, 88 + grain * 0.5, 255)
    else:
        # Warm cream inner guard stripe
        return (206 + grain, 174 + grain, 120 + grain, 255)


def build_trim_atlas(weave_fn, border_fn):
    """
    Builds the 32x32 4-quadrant CTM trim sprite:
      TL (0..15, 0..15):  Full 16x16 outer frame (borders on N, S, W, E + 4 outer corner miters)
      TR (16..31, 0..15): Continuous horizontal borders along N (y=0..2) and S (y=13..15)
      BL (0..15, 16..31): Continuous vertical borders along W (x=0..2) and E (x=13..15)
      BR (16..31, 16..31): 4 inner concave corner miters in [0..2 / 13..15] x [0..2 / 13..15]
    """
    img = [[(0, 0, 0, 255) for _ in range(32)] for _ in range(32)]

    # 1) TL Quadrant (ox=0, oy=0): Full 16x16 frame with 3px mitered outer border
    for y in range(16):
        for x in range(16):
            d = min(x, 15 - x, y, 15 - y)
            if d < 3:
                corner_jewel = (x in (1, 14) and y in (1, 14))
                along = (x + y) if (x < 3 or x > 12) and (y < 3 or y > 12) else (x if min(y, 15 - y) <= min(x, 15 - x) else y)
                img[y][x] = border_fn(d, along, corner_jewel)
            else:
                img[y][x] = weave_fn(x, y)

    # 2) TR Quadrant (ox=16, oy=0): Horizontal continuous N/S borders across full x=0..15
    for y in range(16):
        for x in range(16):
            dy = min(y, 15 - y)
            if dy < 3:
                img[y][16 + x] = border_fn(dy, x, False)
            else:
                img[y][16 + x] = weave_fn(x, y)

    # 3) BL Quadrant (ox=0, oy=16): Vertical continuous W/E borders across full y=0..15
    for y in range(16):
        for x in range(16):
            dx = min(x, 15 - x)
            if dx < 3:
                img[16 + y][x] = border_fn(dx, y, False)
            else:
                img[16 + y][x] = weave_fn(x, y)

    # 4) BR Quadrant (ox=16, oy=16): Inner concave corners in 3x3 corners!
    # At NW sub-tile (x=0..2, y=0..2), neighbor N and W are carpet, NW is NOT carpet:
    # distance from the missing NW exterior block (-1,-1) is max(x, y) in 0..2!
    for y in range(16):
        for x in range(16):
            cx = x if x < 3 else (15 - x if x >= 13 else 99)
            cy = y if y < 3 else (15 - y if y >= 13 else 99)
            if cx < 3 and cy < 3:
                d_conc = max(cx, cy)  # 0 at corner tip (0,0), 1 for L-braid, 2 for inner piping!
                jewel = (cx == 1 and cy == 1)
                img[16 + y][16 + x] = border_fn(d_conc, cx + cy, jewel)
            else:
                img[16 + y][16 + x] = weave_fn(x, y)

    return img


def generate_all_textures():
    castle_center = [[castle_weave_pixel(x, y) for x in range(32)] for y in range(32)]
    castle_trim = build_trim_atlas(castle_weave_pixel, castle_border_pixel)
    castle_icon = [[castle_trim[y][x] for x in range(16)] for y in range(16)]

    rug_center = [[rug_weave_pixel(x, y) for x in range(32)] for y in range(32)]
    rug_trim = build_trim_atlas(rug_weave_pixel, rug_border_pixel)
    rug_icon = [[rug_trim[y][x] for x in range(16)] for y in range(16)]

    write_png(os.path.join(TEX_DIR, "castle_carpet_center.png"), 32, 32, castle_center)
    write_png(os.path.join(TEX_DIR, "castle_carpet_trim.png"), 32, 32, castle_trim)
    write_png(os.path.join(TEX_DIR, "castle_carpet.png"), 16, 16, castle_icon)

    write_png(os.path.join(TEX_DIR, "rug_center.png"), 32, 32, rug_center)
    write_png(os.path.join(TEX_DIR, "rug_trim.png"), 32, 32, rug_trim)
    write_png(os.path.join(TEX_DIR, "rug_item.png"), 16, 16, rug_icon)
    print("Generated 32x32 + 16x16 CTM carpet textures for castle_carpet and rug.")


def write_models_and_blockstates():
    # Single clean blockstate for castle_carpet (wrapped by ConnectedCarpetBakedModel on client)
    with open(os.path.join(BS_DIR, "castle_carpet.json"), "w") as f:
        json.dump({"variants": {"": {"model": "riftworld:block/castle_carpet"}}}, f, indent=2)

    # Single clean blockstate for rug (all ModelBlock states map to riftworld:block/rug, wrapped by ConnectedCarpetBakedModel)
    with open(os.path.join(BS_DIR, "rug.json"), "w") as f:
        json.dump({"variants": {"": {"model": "riftworld:block/rug"}}}, f, indent=2)

    # Fix castle_carpet_border_n.json invalid UV [16,0,19,3] -> [13,0,16,1] so zero model loader warnings ever occur
    with open(os.path.join(BM_DIR, "castle_carpet_border_n.json"), "w") as f:
        json.dump({
            "textures": {"t": "riftworld:block/castle_carpet_trim", "particle": "riftworld:block/castle_carpet"},
            "elements": [{
                "from": [0, 0, 0],
                "to": [16, 1, 3],
                "faces": {
                    "up": {"uv": [0, 0, 8, 1.5], "texture": "#t"},
                    "north": {"uv": [0, 0, 8, 0.5], "texture": "#t"},
                    "south": {"uv": [0, 1, 8, 1.5], "texture": "#t"},
                    "west": {"uv": [0, 0, 1.5, 0.5], "texture": "#t"},
                    "east": {"uv": [6.5, 0, 8, 0.5], "texture": "#t"}
                }
            }]
        }, f, indent=2)

    # Block model for castle_carpet (explicitly references #wool, #center, #trim so ModelBakery always stitches all 3!)
    with open(os.path.join(BM_DIR, "castle_carpet.json"), "w") as f:
        json.dump({
            "textures": {
                "particle": "riftworld:block/castle_carpet",
                "wool": "riftworld:block/castle_carpet",
                "center": "riftworld:block/castle_carpet_center",
                "trim": "riftworld:block/castle_carpet_trim"
            },
            "elements": [{
                "from": [0, 0, 0],
                "to": [16, 1, 16],
                "faces": {
                    "up": {"uv": [0, 0, 16, 16], "texture": "#wool"},
                    "down": {"uv": [0, 0, 16, 16], "texture": "#center", "cullface": "down"},
                    "north": {"uv": [0, 0, 8, 0.5], "texture": "#trim", "cullface": "north"},
                    "south": {"uv": [0, 0, 8, 0.5], "texture": "#trim", "cullface": "south"},
                    "west": {"uv": [0, 0, 8, 0.5], "texture": "#trim", "cullface": "west"},
                    "east": {"uv": [0, 0, 8, 0.5], "texture": "#trim", "cullface": "east"}
                }
            }]
        }, f, indent=2)

    # Block model for rug (same carpet geometry + explicit #wool, #center, #trim atlas references)
    with open(os.path.join(BM_DIR, "rug.json"), "w") as f:
        json.dump({
            "textures": {
                "particle": "riftworld:block/rug_item",
                "wool": "riftworld:block/rug_item",
                "center": "riftworld:block/rug_center",
                "trim": "riftworld:block/rug_trim"
            },
            "elements": [{
                "from": [0, 0, 0],
                "to": [16, 1, 16],
                "faces": {
                    "up": {"uv": [0, 0, 16, 16], "texture": "#wool"},
                    "down": {"uv": [0, 0, 16, 16], "texture": "#center", "cullface": "down"},
                    "north": {"uv": [0, 0, 8, 0.5], "texture": "#trim", "cullface": "north"},
                    "south": {"uv": [0, 0, 8, 0.5], "texture": "#trim", "cullface": "south"},
                    "west": {"uv": [0, 0, 8, 0.5], "texture": "#trim", "cullface": "west"},
                    "east": {"uv": [0, 0, 8, 0.5], "texture": "#trim", "cullface": "east"}
                }
            }]
        }, f, indent=2)

    # Item model for rug so inventory icon renders as a crisp 3D carpet tile (like castle_carpet)
    with open(os.path.join(IM_DIR, "rug.json"), "w") as f:
        json.dump({"parent": "riftworld:block/rug"}, f, indent=2)
    print("Updated blockstates and block/item models for castle_carpet and rug.")


# Minimal lossless NBT reader/writer for migrating the 7 city/*.nbt rug footprints
def read_nbt(data: bytes):
    pos = 0

    def u8():
        nonlocal pos; v = data[pos]; pos += 1; return v

    def i16():
        nonlocal pos; v = struct.unpack_from(">h", data, pos)[0]; pos += 2; return v

    def i32():
        nonlocal pos; v = struct.unpack_from(">i", data, pos)[0]; pos += 4; return v

    def i64():
        nonlocal pos; v = struct.unpack_from(">q", data, pos)[0]; pos += 8; return v

    def f32():
        nonlocal pos; v = struct.unpack_from(">f", data, pos)[0]; pos += 4; return v

    def f64():
        nonlocal pos; v = struct.unpack_from(">d", data, pos)[0]; pos += 8; return v

    def utf():
        nonlocal pos
        ln = struct.unpack_from(">H", data, pos)[0]; pos += 2
        s = data[pos:pos + ln].decode("utf-8")
        pos += ln
        return s

    def payload(t):
        nonlocal pos
        if t == 1:
            v = struct.unpack_from(">b", data, pos)[0]; pos += 1; return (1, v)
        if t == 2: return (2, i16())
        if t == 3: return (3, i32())
        if t == 4: return (4, i64())
        if t == 5: return (5, f32())
        if t == 6: return (6, f64())
        if t == 7:
            ln = i32(); b = data[pos:pos + ln]; pos += ln; return (7, b)
        if t == 8: return (8, utf())
        if t == 9:
            et = u8(); cnt = i32()
            return (9, (et, [payload(et)[1] for _ in range(cnt)]))
        if t == 10:
            entries = []
            while True:
                ft = u8()
                if ft == 0: break
                fn = utf()
                entries.append((fn, payload(ft)))
            return (10, entries)
        if t == 11:
            cnt = i32(); return (11, [i32() for _ in range(cnt)])
        if t == 12:
            cnt = i32(); return (12, [i64() for _ in range(cnt)])
        raise ValueError(f"Unknown tag {t}")

    rt = u8(); rn = utf()
    return (rn, payload(rt))


def write_nbt(root) -> bytes:
    out = bytearray()

    def u8(v): out.append(v & 0xFF)
    def utf(s):
        b = s.encode("utf-8")
        out.extend(struct.pack(">H", len(b)))
        out.extend(b)

    def emit(t, val):
        if t == 1: out.extend(struct.pack(">b", val))
        elif t == 2: out.extend(struct.pack(">h", val))
        elif t == 3: out.extend(struct.pack(">i", val))
        elif t == 4: out.extend(struct.pack(">q", val))
        elif t == 5: out.extend(struct.pack(">f", val))
        elif t == 6: out.extend(struct.pack(">d", val))
        elif t == 7:
            out.extend(struct.pack(">i", len(val))); out.extend(val)
        elif t == 8: utf(val)
        elif t == 9:
            et, items = val
            u8(et); out.extend(struct.pack(">i", len(items)))
            for it in items: emit(et, it)
        elif t == 10:
            for fn, (ft, fv) in val:
                u8(ft); utf(fn); emit(ft, fv)
            u8(0)
        elif t == 11:
            out.extend(struct.pack(">i", len(val)))
            for v in val: out.extend(struct.pack(">i", v))
        elif t == 12:
            out.extend(struct.pack(">i", len(val)))
            for v in val: out.extend(struct.pack(">q", v))

    rn, (rt, rv) = root
    u8(rt); utf(rn); emit(rt, rv)
    return bytes(out)


def migrate_city_rugs():
    targets = ["house_a", "house_b", "house_c", "house_d", "hunter_house", "tavern", "temple"]
    for name in targets:
        p = os.path.join(CITY_NBT_DIR, f"{name}.nbt")
        root = read_nbt(gzip.open(p, "rb").read())
        entries = dict(root[1][1])
        pal_items = entries["palette"][1][1]
        blk_items = entries["blocks"][1][1]

        rug_states = {}
        part_states = set()
        for idx, comp in enumerate(pal_items):
            cd = dict(comp)
            bn = cd["Name"][1]
            if bn == "riftworld:rug":
                props = {k: v[1] for k, v in cd.get("Properties", (10, []))[1]}
                rug_states[idx] = props.get("facing", "south")
            elif bn == "riftworld:part":
                part_states.add(idx)

        if not rug_states:
            continue

        # Collect the 3 partner cells of every former 2x2 rug footprint
        partner_to_rug_state = {}
        for comp in blk_items:
            cd = dict(comp)
            st = cd["state"][1]
            if st in rug_states:
                bx, by, bz = cd["pos"][1][1]
                facing = rug_states[st]
                # ModelKind.RUG had x0=0, x1=1, back=1
                for x_off in (0, 1):
                    for b_off in (0, 1):
                        if x_off == 0 and b_off == 0:
                            continue
                        z_rel = -b_off
                        if facing == "north":
                            dx, dz = -x_off, -z_rel
                        elif facing == "west":
                            dx, dz = -z_rel, x_off
                        elif facing == "east":
                            dx, dz = z_rel, -x_off
                        else:  # south
                            dx, dz = x_off, z_rel
                        partner_to_rug_state[(bx + dx, by, bz + dz)] = st

        converted = 0
        for comp in blk_items:
            for i, (fn, (ft, fv)) in enumerate(comp):
                if fn == "pos":
                    pos_tuple = tuple(fv[1])
            for i, (fn, (ft, fv)) in enumerate(comp):
                if fn == "state" and fv in part_states and pos_tuple in partner_to_rug_state:
                    comp[i] = ("state", (3, partner_to_rug_state[pos_tuple]))
                    converted += 1

        if converted > 0:
            with gzip.open(p, "wb", compresslevel=9) as gz:
                gz.write(write_nbt(root))
            print(f"  Migrated {converted} rug part-cells -> connected riftworld:rug in {name}.nbt")


if __name__ == "__main__":
    generate_all_textures()
    write_models_and_blockstates()
    migrate_city_rugs()
