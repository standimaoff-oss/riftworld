#!/usr/bin/env python3
"""
Generates all custom 16x16 PNG textures, blockstates, models, loot tables, tags,
biome JSON, and Dynamic Trees addon assets (including strictly 6-connected
miniature bonsai JoCodes) for the Crystal Rifts biome (riftworld:crystal_rifts).
"""
import json
import math
import os
import random
import struct
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
MOD_RES = os.path.join(ROOT, "mod", "src", "main", "resources")
DT_RES = os.path.join(ROOT, "mod", "src", "dtaddon", "resources")


def write_png(path, w, h, pixels):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        for x in range(w):
            r, g, b, a = pixels[y][x]
            raw.extend((max(0, min(255, int(r))),
                        max(0, min(255, int(g))),
                        max(0, min(255, int(b))),
                        max(0, min(255, int(a)))))

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    ihdr = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)
    data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(data)


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")


def h2(x, y, seed):
    n = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def lerp(a, b, t):
    return a + (b - a) * t


def lerp_rgb(c0, c1, t):
    t = max(0.0, min(1.0, t))
    return (lerp(c0[0], c1[0], t), lerp(c0[1], c1[1], t), lerp(c0[2], c1[2], t))


def make_crystal_tex(c_shadow, c_mid, c_light, c_spec, seed):
    px = [[(0, 0, 0, 255) for _ in range(16)] for _ in range(16)]
    for y in range(16):
        for x in range(16):
            d1 = ((x * 0.85 - y * 0.55) % 8.0) / 8.0
            d2 = ((x * 0.45 + y * 0.90) % 7.0) / 7.0
            facet = 0.55 * abs(d1 - 0.5) * 2.0 + 0.45 * (1.0 - abs(d2 - 0.35) * 1.8)
            edge = 1.0 if (x in (0, 15) or y in (0, 15)) else 0.0
            ridge = 1.0 if abs((x + y) - 14) <= 1 or abs((x - y) + 2) <= 0 else 0.0
            n = (h2(x, y, seed) - 0.5) * 0.14
            v = max(0.0, min(1.0, facet + n + 0.22 * ridge - 0.14 * edge))
            if v < 0.38:
                rgb = lerp_rgb(c_shadow, c_mid, v / 0.38)
            elif v < 0.78:
                rgb = lerp_rgb(c_mid, c_light, (v - 0.38) / 0.40)
            else:
                rgb = lerp_rgb(c_light, c_spec, (v - 0.78) / 0.22)
            if (x, y) in ((4, 3), (5, 3), (11, 6), (10, 7), (6, 11)):
                rgb = lerp_rgb(rgb, c_spec, 0.72)
            px[y][x] = (rgb[0], rgb[1], rgb[2], 255)
    return px


def make_rift_marble(mossy=False):
    px = [[(0, 0, 0, 255) for _ in range(16)] for _ in range(16)]
    base0 = (212, 224, 230)
    base1 = (241, 247, 250)
    vein = (175, 194, 205)
    moss0 = (82, 134, 114)
    moss1 = (116, 168, 145)
    for y in range(16):
        for x in range(16):
            w = 0.5 + 0.5 * math.sin((x * 0.48 + y * 0.31) + 1.6 * h2(x // 2, y // 2, 17))
            n = h2(x, y, 31)
            rgb = lerp_rgb(base0, base1, 0.65 * w + 0.35 * n)
            vdist = abs(((x * 0.7 + y * 1.1) % 9.0) - 4.5)
            if vdist < 0.65:
                rgb = lerp_rgb(rgb, vein, 0.68)
            if y in (0, 8) and h2(x, y, 43) < 0.45:
                rgb = lerp_rgb(rgb, vein, 0.35)
            if mossy:
                m = 0.55 * (1.0 - y / 15.0) + 0.55 * h2(x // 2, y // 2, 77) + 0.25 * h2(x, y, 79)
                if m > 0.74 or (y <= 3 and h2(x, y, 81) > 0.32):
                    rgb = lerp_rgb(moss0, moss1, h2(x, y, 83))
            px[y][x] = (rgb[0], rgb[1], rgb[2], 255)
    return px


def make_rift_turf_top():
    # Custom non-vanilla cool sage-moss turf (02_crystal_sanctum.png foreground right: soft sage-emerald moss carpet)
    px = [[(0, 0, 0, 255) for _ in range(16)] for _ in range(16)]
    dark = (74, 116, 98)
    mid = (98, 144, 123)
    light = (128, 174, 151)
    glint = (156, 202, 182)
    for y in range(16):
        for x in range(16):
            cl = 0.5 + 0.5 * math.sin(x * 0.9 + 0.7 * h2(x // 2, y // 2, 601)) * math.cos(y * 0.9)
            n = h2(x, y, 607)
            v = 0.48 * cl + 0.52 * n
            if v < 0.32:
                rgb = lerp_rgb(dark, mid, v / 0.32)
            elif v < 0.80:
                rgb = lerp_rgb(mid, light, (v - 0.32) / 0.48)
            else:
                rgb = lerp_rgb(light, glint, (v - 0.80) / 0.20)
            px[y][x] = (rgb[0], rgb[1], rgb[2], 255)
    return px


def make_rift_turf_side():
    # Sage-moss turf draping over pale silver-grey rift marble stone — ZERO brown vanilla dirt!
    marble = make_rift_marble(False)
    top = make_rift_turf_top()
    px = [[(0, 0, 0, 255) for _ in range(16)] for _ in range(16)]
    shadow = (66, 102, 88)
    for x in range(16):
        overhang = 4 + int(3.8 * (0.5 + 0.5 * math.sin(x * 0.95 + 1.3)) + 1.6 * h2(x, 0, 613))
        for y in range(16):
            if y < overhang:
                px[y][x] = top[y][x]
            elif y == overhang:
                px[y][x] = (shadow[0], shadow[1], shadow[2], 255)
            else:
                # Occasional tiny moss fleck on pale stone below overhang
                base = marble[y][x]
                if y <= overhang + 3 and h2(x, y, 619) > 0.74:
                    rgb = lerp_rgb((base[0], base[1], base[2]), (94, 140, 120), 0.65)
                    px[y][x] = (rgb[0], rgb[1], rgb[2], 255)
                else:
                    px[y][x] = base
    return px


def make_rift_grass():
    # Custom short crystalline sage-moss tufts (NOT vanilla grass!) with tiny frost-cyan tips (~22% opaque)
    px = [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]
    c_root = (64, 106, 90, 255)
    c_mid = (92, 146, 126, 255)
    c_upper = (126, 184, 164, 255)
    c_frost = (168, 242, 244, 255)
    blades = [
        # (x_base, height, lean)
        (2, 6, -1),
        (4, 9, 0),
        (6, 7, 1),
        (8, 10, 0),
        (10, 6, -1),
        (12, 9, 1),
        (14, 5, 0),
    ]
    for xb, bh, lean in blades:
        for step in range(bh):
            y = 15 - step
            xx = xb + (lean if step >= bh - 2 else 0)
            if 0 <= xx < 16 and 0 <= y < 16:
                if step == bh - 1:
                    col = c_frost if (xb in (4, 8, 12)) else c_upper
                elif step >= bh - 3:
                    col = c_upper
                elif step >= 2:
                    col = c_mid
                else:
                    col = c_root
                px[y][xx] = col
    return px


def make_rift_slate():
    px = [[(0, 0, 0, 255) for _ in range(16)] for _ in range(16)]
    c0 = (72, 90, 104)
    c1 = (104, 124, 138)
    c2 = (132, 152, 166)
    for y in range(16):
        for x in range(16):
            strata = 0.5 + 0.5 * math.sin(y * 1.15 + 0.55 * math.sin(x * 0.7))
            n = h2(x, y, 97)
            v = 0.62 * strata + 0.38 * n
            rgb = lerp_rgb(c0, c1, v / 0.6) if v < 0.6 else lerp_rgb(c1, c2, (v - 0.6) / 0.4)
            px[y][x] = (rgb[0], rgb[1], rgb[2], 255)
    return px


def make_crystal_bark():
    px = [[(0, 0, 0, 255) for _ in range(16)] for _ in range(16)]
    c0 = (176, 188, 198)
    c1 = (224, 234, 240)
    c2 = (246, 250, 253)
    knot = (122, 140, 154)
    for y in range(16):
        for x in range(16):
            grain = 0.5 + 0.5 * math.sin(x * 0.95 + 0.35 * h2(x, y // 2, 109))
            n = h2(x, y, 113)
            rgb = lerp_rgb(c0, c2, 0.55 * grain + 0.45 * n)
            if y in (3, 9, 14) and abs((x + y * 3) % 7 - 3) <= 1 and h2(x, y, 127) > 0.38:
                rgb = lerp_rgb(rgb, knot, 0.72)
            elif h2(x, y, 119) < 0.14:
                rgb = lerp_rgb(rgb, c1, 0.5)
            px[y][x] = (rgb[0], rgb[1], rgb[2], 255)
    return px


def make_crystal_rings():
    px = [[(0, 0, 0, 255) for _ in range(16)] for _ in range(16)]
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 6.6:
                rgb = (182, 194, 204)
            else:
                ring = 0.5 + 0.5 * math.sin(d * 2.4)
                rgb = lerp_rgb((198, 214, 224), (242, 248, 252), ring)
            px[y][x] = (rgb[0], rgb[1], rgb[2], 255)
    return px


def make_crystal_canopy():
    # Crisp 100% opaque (alpha=255) bright snow-ice & frost-cyan bonsai leaf tufts (02_crystal_sanctum.png)
    px = [[(0, 0, 0, 255) for _ in range(16)] for _ in range(16)]
    deep = (128, 184, 214)
    mid = (184, 224, 244)
    bright = (228, 247, 255)
    tip = (250, 254, 255)
    for y in range(16):
        for x in range(16):
            cl = 0.5 + 0.5 * math.sin(x * 1.25) * math.cos(y * 1.25)
            n = h2(x, y, 131)
            v = 0.50 * cl + 0.50 * n
            if v < 0.25:
                rgb = lerp_rgb(deep, mid, v / 0.25)
            elif v < 0.65:
                rgb = lerp_rgb(mid, bright, (v - 0.25) / 0.40)
            else:
                rgb = lerp_rgb(bright, tip, (v - 0.65) / 0.35)
            px[y][x] = (rgb[0], rgb[1], rgb[2], 255)
    return px


def make_crystal_cluster():
    px = [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]
    c_base = (28, 148, 162, 255)
    c_mid = (62, 218, 222, 255)
    c_tip = (196, 255, 252, 255)
    shards = [(8, 2, 1), (5, 5, 1), (11, 4, 1), (3, 9, 0), (13, 8, 0)]
    for xb, yt, hw in shards:
        for y in range(yt, 16):
            t = (y - yt) / max(1.0, 15.0 - yt)
            w = hw if y >= yt + 2 else 0
            for dx in range(-w, w + 1):
                xx = xb + dx
                if 0 <= xx < 16:
                    if y <= yt + 1:
                        col = c_tip
                    elif t < 0.62:
                        col = c_mid if dx <= 0 else (45, 192, 200, 255)
                    else:
                        col = c_base
                    px[y][xx] = col
    return px


def make_crystal_shroom():
    px = [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]
    stem = (196, 232, 236, 255)
    cap_top = (142, 250, 248, 255)
    cap_mid = (52, 212, 220, 255)
    cap_rim = (28, 156, 172, 255)
    shrooms = [(5, 7, 2), (11, 9, 2), (8, 11, 1)]
    for cx, cy, r in shrooms:
        for y in range(cy + 1, 16):
            px[y][cx] = stem
        for dy in range(-r, 1):
            w = r if dy == 0 else max(1, r - 1)
            for dx in range(-w, w + 1):
                xx, yy = cx + dx, cy + dy
                if 0 <= xx < 16 and 0 <= yy < 16:
                    px[yy][xx] = cap_top if dy < 0 and abs(dx) <= 1 else (cap_mid if dy < 0 else cap_rim)
    return px


def make_seed_tex():
    px = [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]
    for y in range(4, 13):
        for x in range(5, 11):
            d = abs(x - 7.5) + abs(y - 8.5) * 0.75
            if d <= 3.3:
                col = (224, 246, 255, 255) if d < 1.6 else (130, 198, 224, 255)
                px[y][x] = col
    px[3][8] = (176, 228, 245, 255)
    px[13][7] = (110, 152, 172, 255)
    return px


def cross_model(tex):
    return {
        "ambientocclusion": False,
        "render_type": "minecraft:cutout",
        "textures": {"cross": tex, "particle": tex},
        "elements": [
            {
                "from": [0.8, 0.0, 8.0],
                "to": [15.2, 16.0, 8.0],
                "rotation": {"origin": [8.0, 8.0, 8.0], "axis": "y", "angle": 45.0, "rescale": True},
                "shade": False,
                "faces": {
                    "north": {"uv": [0, 0, 16, 16], "texture": "#cross"},
                    "south": {"uv": [0, 0, 16, 16], "texture": "#cross"},
                },
            },
            {
                "from": [8.0, 0.0, 0.8],
                "to": [8.0, 16.0, 15.2],
                "rotation": {"origin": [8.0, 8.0, 8.0], "axis": "y", "angle": 45.0, "rescale": True},
                "shade": False,
                "faces": {
                    "west": {"uv": [0, 0, 16, 16], "texture": "#cross"},
                    "east": {"uv": [0, 0, 16, 16], "texture": "#cross"},
                },
            },
        ],
    }


def self_loot(block_id):
    return {
        "type": "minecraft:block",
        "pools": [
            {
                "rolls": 1,
                "entries": [{"type": "minecraft:item", "name": f"riftworld:{block_id}"}],
            }
        ],
    }


B64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
DIRS = {0: (0, -1, 0), 1: (0, 1, 0), 2: (0, 0, -1), 3: (0, 0, 1), 4: (-1, 0, 0), 5: (1, 0, 0)}


def encode_ops(ops):
    out = list(ops)
    if len(out) % 2 == 1:
        out.append(7)  # CRITICAL: pad odd opcode count with 7 (RETURN on empty stack), NEVER 0 (DOWN)!
    chars = []
    for i in range(0, len(out), 2):
        chars.append(B64[(out[i] << 3) | out[i + 1]])
    return "".join(chars)


def verify_jocode(code):
    ops = []
    for ch in code:
        v = B64.index(ch)
        ops.append((v >> 3) & 7)
        ops.append(v & 7)
    pos = (0, 0, 0)
    vis = {pos}
    stk = []
    for op in ops:
        if op in DIRS:
            dx, dy, dz = DIRS[op]
            pos = (pos[0] + dx, pos[1] + dy, pos[2] + dz)
            if pos in vis:
                return False, "revisit"
            vis.add(pos)
        elif op == 6:
            stk.append(pos)
        elif op == 7:
            if stk:
                pos = stk.pop()
    if stk:
        return False, f"non-empty stack {len(stk)}"
    vl = list(vis)
    adj6 = 0
    for i in range(len(vl)):
        x1, y1, z1 = vl[i]
        for j in range(i + 1, len(vl)):
            x2, y2, z2 = vl[j]
            if abs(x1 - x2) + abs(y1 - y2) + abs(z1 - z2) == 1:
                adj6 += 1
    if adj6 != len(vl) - 1:
        return False, f"6-cycle: vox={len(vl)} adj6={adj6}"
    max_y = max(y for _, y, _ in vl)
    return True, f"ok vox={len(vl)} maxY={max_y}"


def build_bonsai_crystal_tree(rng, radius):
    """
    Builds a VERY SMALL, delicate gnarly bonsai silver tree (height 3..4 blocks, 6..11 branch voxels)
    matching the tiny sparse avenue trees in 02_crystal_sanctum.png.
    Strictly maintains induced tree in 6-connected Z^3 (adj6 == len(voxels) - 1).
    """
    vis = {(0, 0, 0)}
    children = {(0, 0, 0): []}

    def can_add(p):
        if p in vis or p[1] < 1 or p[1] > 4:
            return False
        nbrs = 0
        for dx, dy, dz in DIRS.values():
            if (p[0] + dx, p[1] + dy, p[2] + dz) in vis:
                nbrs += 1
        return nbrs == 1

    def add_edge(u, d_op):
        dx, dy, dz = DIRS[d_op]
        v = (u[0] + dx, u[1] + dy, u[2] + dz)
        if not can_add(v):
            return None
        vis.add(v)
        children[u].append((d_op, v))
        children[v] = []
        return v

    # Short gnarled bonsai stem: 2 blocks up, then ALWAYS splits into 2..3 spreading silver twigs!
    trunk_h = 2
    cur = (0, 0, 0)
    for _ in range(trunk_h):
        nxt = add_edge(cur, 1)
        if nxt is None:
            break
        cur = nxt

    top_trunk = cur
    horiz_dirs = [2, 3, 4, 5]
    rng.shuffle(horiz_dirs)
    n_limbs = 2 if radius <= 3 else (2 + rng.randint(0, 1))
    limb_len = 1 if radius <= 4 else 2

    for idx in range(n_limbs):
        hd = horiz_dirs[idx]
        p1 = add_edge(top_trunk, hd)
        if p1 is None:
            continue
        p = p1
        for step in range(limb_len):
            cand_ops = [1, hd] if step == 0 else [hd, 1]
            moved = None
            for op in cand_ops:
                moved = add_edge(p, op)
                if moved is not None:
                    break
            if moved is None:
                break
            p = moved

    ops = []

    def dfs(u):
        chs = children[u]
        if not chs:
            return
        for i, (op, v) in enumerate(chs):
            is_last = (i == len(chs) - 1)
            if not is_last:
                ops.append(6)
            ops.append(op)
            dfs(v)
            if not is_last:
                ops.append(7)

    dfs((0, 0, 0))
    return encode_ops(ops)


def append_tag_values(path, new_vals):
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    vals = data.get("values", [])
    for v in new_vals:
        if v not in vals:
            vals.append(v)
    data["values"] = vals
    write_json(path, data)


def main():
    tex_dir = os.path.join(MOD_RES, "assets", "riftworld", "textures", "block")
    write_png(os.path.join(tex_dir, "crystal_block_deep.png"), 16, 16,
              make_crystal_tex((18, 98, 116), (34, 158, 174), (66, 206, 214), (154, 246, 246), 501))
    write_png(os.path.join(tex_dir, "crystal_block.png"), 16, 16,
              make_crystal_tex((34, 156, 170), (62, 214, 220), (118, 242, 240), (218, 255, 254), 502))
    write_png(os.path.join(tex_dir, "crystal_block_bright.png"), 16, 16,
              make_crystal_tex((88, 214, 220), (144, 244, 244), (204, 254, 253), (246, 255, 255), 503))
    write_png(os.path.join(tex_dir, "rift_marble.png"), 16, 16, make_rift_marble(False))
    write_png(os.path.join(tex_dir, "rift_marble_mossy.png"), 16, 16, make_rift_marble(True))
    write_png(os.path.join(tex_dir, "rift_slate.png"), 16, 16, make_rift_slate())
    write_png(os.path.join(tex_dir, "rift_turf_top.png"), 16, 16, make_rift_turf_top())
    write_png(os.path.join(tex_dir, "rift_turf_side.png"), 16, 16, make_rift_turf_side())
    write_png(os.path.join(tex_dir, "rift_grass.png"), 16, 16, make_rift_grass())
    write_png(os.path.join(tex_dir, "crystal_bark.png"), 16, 16, make_crystal_bark())
    write_png(os.path.join(tex_dir, "crystal_rings.png"), 16, 16, make_crystal_rings())
    write_png(os.path.join(tex_dir, "crystal_canopy.png"), 16, 16, make_crystal_canopy())
    write_png(os.path.join(tex_dir, "crystal_cluster.png"), 16, 16, make_crystal_cluster())
    write_png(os.path.join(tex_dir, "crystal_shroom.png"), 16, 16, make_crystal_shroom())

    bs_dir = os.path.join(MOD_RES, "assets", "riftworld", "blockstates")
    bm_dir = os.path.join(MOD_RES, "assets", "riftworld", "models", "block")
    im_dir = os.path.join(MOD_RES, "assets", "riftworld", "models", "item")
    lt_dir = os.path.join(MOD_RES, "data", "riftworld", "loot_tables", "blocks")

    cubes = [
        "crystal_block_deep",
        "crystal_block",
        "crystal_block_bright",
        "rift_marble",
        "rift_marble_mossy",
        "rift_slate",
    ]
    for cid in cubes:
        write_json(os.path.join(bs_dir, f"{cid}.json"), {
            "variants": {"": {"model": f"riftworld:block/{cid}"}}
        })
        write_json(os.path.join(bm_dir, f"{cid}.json"), {
            "parent": "minecraft:block/cube_all",
            "textures": {"all": f"riftworld:block/{cid}"},
        })
        write_json(os.path.join(im_dir, f"{cid}.json"), {
            "parent": f"riftworld:block/{cid}"
        })
        write_json(os.path.join(lt_dir, f"{cid}.json"), self_loot(cid))

    # Custom non-vanilla rift_turf (sage-moss top + mossy silver-marble side, ZERO brown dirt!)
    write_json(os.path.join(bs_dir, "rift_turf.json"), {
        "variants": {"": {"model": "riftworld:block/rift_turf"}}
    })
    write_json(os.path.join(bm_dir, "rift_turf.json"), {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {
            "bottom": "riftworld:block/rift_marble",
            "side": "riftworld:block/rift_turf_side",
            "top": "riftworld:block/rift_turf_top",
        },
    })
    write_json(os.path.join(im_dir, "rift_turf.json"), {"parent": "riftworld:block/rift_turf"})
    write_json(os.path.join(lt_dir, "rift_turf.json"), self_loot("rift_turf"))

    # crystal_log
    write_json(os.path.join(bs_dir, "crystal_log.json"), {
        "variants": {
            "axis=y": {"model": "riftworld:block/crystal_log"},
            "axis=z": {"model": "riftworld:block/crystal_log_horizontal", "x": 90},
            "axis=x": {"model": "riftworld:block/crystal_log_horizontal", "x": 90, "y": 90},
        }
    })
    write_json(os.path.join(bm_dir, "crystal_log.json"), {
        "parent": "minecraft:block/cube_column",
        "textures": {"end": "riftworld:block/crystal_rings", "side": "riftworld:block/crystal_bark"},
    })
    write_json(os.path.join(bm_dir, "crystal_log_horizontal.json"), {
        "parent": "minecraft:block/cube_column_horizontal",
        "textures": {"end": "riftworld:block/crystal_rings", "side": "riftworld:block/crystal_bark"},
    })
    write_json(os.path.join(im_dir, "crystal_log.json"), {"parent": "riftworld:block/crystal_log"})
    write_json(os.path.join(lt_dir, "crystal_log.json"), self_loot("crystal_log"))

    # crystal_leaves
    write_json(os.path.join(bs_dir, "crystal_leaves.json"), {
        "variants": {"": {"model": "riftworld:block/crystal_leaves"}}
    })
    write_json(os.path.join(bm_dir, "crystal_leaves.json"), {
        "parent": "minecraft:block/leaves",
        "textures": {"all": "riftworld:block/crystal_canopy"},
        "render_type": "minecraft:cutout_mipped",
    })
    write_json(os.path.join(im_dir, "crystal_leaves.json"), {"parent": "riftworld:block/crystal_leaves"})
    write_json(os.path.join(lt_dir, "crystal_leaves.json"), self_loot("crystal_leaves"))

    # rift_marble_slab
    write_json(os.path.join(bs_dir, "rift_marble_slab.json"), {
        "variants": {
            "type=bottom": {"model": "riftworld:block/rift_marble_slab"},
            "type=top": {"model": "riftworld:block/rift_marble_slab_top"},
            "type=double": {"model": "riftworld:block/rift_marble"},
        }
    })
    for m_name, parent in [("rift_marble_slab", "slab"), ("rift_marble_slab_top", "slab_top")]:
        write_json(os.path.join(bm_dir, f"{m_name}.json"), {
            "parent": f"minecraft:block/{parent}",
            "textures": {
                "bottom": "riftworld:block/rift_marble",
                "top": "riftworld:block/rift_marble",
                "side": "riftworld:block/rift_marble",
            },
        })
    write_json(os.path.join(im_dir, "rift_marble_slab.json"), {"parent": "riftworld:block/rift_marble_slab"})
    write_json(os.path.join(lt_dir, "rift_marble_slab.json"), {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{
                "type": "minecraft:item",
                "name": "riftworld:rift_marble_slab",
                "functions": [{
                    "function": "minecraft:set_count",
                    "count": 2,
                    "add": False,
                    "conditions": [{
                        "condition": "minecraft:block_state_property",
                        "block": "riftworld:rift_marble_slab",
                        "properties": {"type": "double"},
                    }],
                }],
            }],
        }],
    })

    # rift_marble_stairs
    with open(os.path.join(bs_dir, "elisar_cobble_stairs.json"), "r", encoding="utf-8") as f:
        stairs_bs = f.read().replace("elisar_cobble", "rift_marble")
    with open(os.path.join(bs_dir, "rift_marble_stairs.json"), "w", encoding="utf-8") as f:
        f.write(stairs_bs)
    for suf, parent in [("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")]:
        write_json(os.path.join(bm_dir, f"rift_marble_stairs{suf}.json"), {
            "parent": f"minecraft:block/{parent}",
            "textures": {
                "bottom": "riftworld:block/rift_marble",
                "top": "riftworld:block/rift_marble",
                "side": "riftworld:block/rift_marble",
            },
        })
    write_json(os.path.join(im_dir, "rift_marble_stairs.json"), {"parent": "riftworld:block/rift_marble_stairs"})
    write_json(os.path.join(lt_dir, "rift_marble_stairs.json"), self_loot("rift_marble_stairs"))

    # Custom cross flora: crystal_cluster, crystal_shroom, AND custom rift_grass (replaces vanilla grass!)
    for pid in ("crystal_cluster", "crystal_shroom", "rift_grass"):
        write_json(os.path.join(bs_dir, f"{pid}.json"), {
            "variants": {"": {"model": f"riftworld:block/{pid}"}}
        })
        write_json(os.path.join(bm_dir, f"{pid}.json"), cross_model(f"riftworld:block/{pid}"))
        write_json(os.path.join(im_dir, f"{pid}.json"), {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"riftworld:block/{pid}"},
        })
        write_json(os.path.join(lt_dir, f"{pid}.json"), self_loot(pid))

    # Biome JSON: riftworld:crystal_rifts
    write_json(os.path.join(MOD_RES, "data", "riftworld", "worldgen", "biome", "crystal_rifts.json"), {
        "has_precipitation": False,
        "temperature": 0.65,
        "downfall": 0.6,
        "effects": {
            "sky_color": 12446448,
            "fog_color": 14874104,
            "water_color": 3723990,
            "water_fog_color": 2005658,
            "grass_color": 6791305,
            "foliage_color": 11524846,
            "particle": {
                "options": {"type": "minecraft:end_rod"},
                "probability": 0.00035,
            },
        },
        "spawners": {},
        "spawn_costs": {},
        "carvers": {},
        "features": [],
    })

    write_json(os.path.join(MOD_RES, "data", "riftworld", "dimension", "elisar.json"), {
        "type": "riftworld:elisar",
        "generator": {
            "type": "riftworld:elisar",
            "biome_source": {
                "type": "riftworld:elisar",
                "default": "riftworld:elisar_city",
                "ashen": "riftworld:ashen_lands",
                "violet": "riftworld:violet_hills",
                "sizo": "riftworld:sizo_grove",
                "gardens": "riftworld:waterfall_gardens",
                "mist": "riftworld:mist_keep",
                "reef": "riftworld:shallow_reef",
                "abyss": "riftworld:abyss",
                "crystal": "riftworld:crystal_rifts",
            },
        },
    })

    bt_dir = os.path.join(MOD_RES, "data", "minecraft", "tags", "blocks")
    it_dir = os.path.join(MOD_RES, "data", "minecraft", "tags", "items")
    append_tag_values(os.path.join(bt_dir, "replaceable_by_trees.json"), [
        "riftworld:crystal_cluster",
        "riftworld:crystal_shroom",
        "riftworld:rift_grass",
    ])
    append_tag_values(os.path.join(bt_dir, "logs.json"), ["riftworld:crystal_log"])
    append_tag_values(os.path.join(it_dir, "logs.json"), ["riftworld:crystal_log"])
    append_tag_values(os.path.join(bt_dir, "leaves.json"), ["riftworld:crystal_leaves"])
    append_tag_values(os.path.join(bt_dir, "slabs.json"), ["riftworld:rift_marble_slab"])
    append_tag_values(os.path.join(it_dir, "slabs.json"), ["riftworld:rift_marble_slab"])
    append_tag_values(os.path.join(bt_dir, "stairs.json"), ["riftworld:rift_marble_stairs"])
    append_tag_values(os.path.join(it_dir, "stairs.json"), ["riftworld:rift_marble_stairs"])

    en_path = os.path.join(MOD_RES, "assets", "riftworld", "lang", "en_us.json")
    ru_path = os.path.join(MOD_RES, "assets", "riftworld", "lang", "ru_ru.json")
    with open(en_path, "r", encoding="utf-8") as f:
        en = json.load(f)
    en.update({
        "block.riftworld.crystal_block_deep": "Deep Rift Crystal Block",
        "block.riftworld.crystal_block": "Rift Crystal Block",
        "block.riftworld.crystal_block_bright": "Luminous Rift Crystal Core",
        "block.riftworld.rift_marble": "Rift Marble",
        "block.riftworld.rift_marble_mossy": "Mossy Rift Marble",
        "block.riftworld.rift_marble_slab": "Rift Marble Slab",
        "block.riftworld.rift_marble_stairs": "Rift Marble Stairs",
        "block.riftworld.rift_slate": "Rift Slate",
        "block.riftworld.rift_turf": "Rift Moss Turf",
        "block.riftworld.rift_grass": "Crystalline Rift Grass",
        "block.riftworld.crystal_log": "Crystal Silverwood Log",
        "block.riftworld.crystal_leaves": "Crystal Silverwood Leaves",
        "block.riftworld.crystal_cluster": "Rift Crystal Cluster",
        "block.riftworld.crystal_shroom": "Luminous Rift Mushroom",
        "biome.riftworld.crystal_rifts": "Crystal Rifts",
    })
    write_json(en_path, en)

    with open(ru_path, "r", encoding="utf-8") as f:
        ru = json.load(f)
    ru.update({
        "block.riftworld.crystal_block_deep": "Глубинный кристалл разлома",
        "block.riftworld.crystal_block": "Кристалл разлома",
        "block.riftworld.crystal_block_bright": "Сияющее ядро кристалла разлома",
        "block.riftworld.rift_marble": "Мрамор разломов",
        "block.riftworld.rift_marble_mossy": "Замшелый мрамор разломов",
        "block.riftworld.rift_marble_slab": "Плита из мрамора разломов",
        "block.riftworld.rift_marble_stairs": "Ступени из мрамора разломов",
        "block.riftworld.rift_slate": "Сланец разломов",
        "block.riftworld.rift_turf": "Дёрн разломов",
        "block.riftworld.rift_grass": "Хрустальная трава",
        "block.riftworld.crystal_log": "Бревно хрустальной серебрицы",
        "block.riftworld.crystal_leaves": "Листва хрустальной серебрицы",
        "block.riftworld.crystal_cluster": "Друза кристалла разлома",
        "block.riftworld.crystal_shroom": "Светящийся гриб разломов",
        "biome.riftworld.crystal_rifts": "Хрустальные разломы",
    })
    write_json(ru_path, ru)

    # -----------------------------------------------------------------------
    # Dynamic Trees Addon: miniature sparse bonsai riftworlddt:crystal_birch + rooty_rift_turf
    # -----------------------------------------------------------------------
    write_png(os.path.join(DT_RES, "assets", "riftworlddt", "textures", "item", "crystal_birch_seed.png"),
              16, 16, make_seed_tex())

    write_json(os.path.join(DT_RES, "assets", "riftworlddt", "blockstates", "crystal_birch_branch.json"), {
        "variants": {"": {"model": "riftworlddt:block/crystal_birch_branch"}}
    })
    write_json(os.path.join(DT_RES, "assets", "riftworlddt", "blockstates", "crystal_birch_leaves.json"), {
        "variants": {"": {"model": "riftworlddt:block/crystal_birch_leaves"}}
    })
    write_json(os.path.join(DT_RES, "assets", "riftworlddt", "blockstates", "crystal_birch_sapling.json"), {
        "variants": {"": {"model": "riftworlddt:block/saplings/crystal_birch"}}
    })
    write_json(os.path.join(DT_RES, "assets", "riftworlddt", "blockstates", "rooty_rift_turf.json"), {
        "multipart": [
            {"apply": {"model": "riftworld:block/rift_turf"}},
            {"apply": {"model": "dynamictrees:block/roots"}},
        ]
    })
    write_json(os.path.join(DT_RES, "trees", "riftworlddt", "soil_properties", "rift_turf.json"), {
        "primitive_soil": "riftworld:rift_turf",
        "acceptable_soils": ["dirt_like"],
        "block_registry_name": "riftworlddt:rooty_rift_turf",
        "only_if_loaded": "riftworld",
    })
    write_json(os.path.join(DT_RES, "assets", "riftworlddt", "models", "block", "crystal_birch_branch.json"), {
        "loader": "dynamictrees:branch",
        "textures": {
            "bark": "riftworld:block/crystal_bark",
            "rings": "riftworld:block/crystal_rings",
        },
    })
    write_json(os.path.join(DT_RES, "assets", "riftworlddt", "models", "block", "crystal_birch_leaves.json"), {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": "riftworld:block/crystal_canopy"},
        "render_type": "minecraft:cutout_mipped",
    })
    write_json(os.path.join(DT_RES, "assets", "riftworlddt", "models", "block", "saplings", "crystal_birch.json"), {
        "parent": "dynamictrees:block/smartmodel/sapling",
        "render_type": "minecraft:cutout_mipped",
        "textures": {
            "leaves": "riftworld:block/crystal_canopy",
            "log": "riftworld:block/crystal_bark",
        },
    })
    write_json(os.path.join(DT_RES, "assets", "riftworlddt", "models", "item", "crystal_birch_branch.json"), {
        "parent": "dynamictrees:item/branch",
        "textures": {
            "bark": "riftworld:block/crystal_bark",
            "rings": "riftworld:block/crystal_rings",
        },
    })
    write_json(os.path.join(DT_RES, "assets", "riftworlddt", "models", "item", "crystal_birch_sapling.json"), {
        "parent": "riftworlddt:block/saplings/crystal_birch"
    })
    write_json(os.path.join(DT_RES, "assets", "riftworlddt", "models", "item", "crystal_birch_seed.json"), {
        "parent": "dynamictrees:item/standard_seed",
        "textures": {"layer0": "riftworlddt:item/crystal_birch_seed"},
    })

    # Slender bonsai twigs (max_branch_radius 8 for DT compatibility, planted at radius 2..3!)
    write_json(os.path.join(DT_RES, "trees", "riftworlddt", "families", "crystal_birch.json"), {
        "common_leaves": "riftworlddt:crystal_birch",
        "common_species": "riftworlddt:crystal_birch",
        "primitive_log": "riftworld:crystal_log",
        "generate_stripped_branch": False,
        "generate_surface_root": False,
        "max_branch_radius": 8,
        "primary_thickness": 1,
        "secondary_thickness": 1,
        "branch_properties": {
            "sound": "wood",
            "map_color": "QUARTZ",
        },
        "only_if_loaded": "riftworld",
    })
    # Acacia thin 1-layer tufts + smother: 1 = minimal sparse foliage at twig tips!
    write_json(os.path.join(DT_RES, "trees", "riftworlddt", "leaves_properties", "crystal_birch.json"), {
        "primitive_leaves": "riftworld:crystal_leaves",
        "color": "0xFFFFFF",
        "cell_kit": "dynamictrees:acacia",
        "smother": 1,
        "light_requirement": 13,
        "block_registry_name": "riftworlddt:crystal_birch_leaves",
        "only_if_loaded": "riftworld",
    })
    write_json(os.path.join(DT_RES, "trees", "riftworlddt", "species", "crystal_birch.json"), {
        "family": "riftworlddt:crystal_birch",
        "tapering": 0.38,
        "signal_energy": 8.5,
        "up_probability": 1,
        "lowest_branch_height": 2,
        "growth_rate": 0.85,
        "leaves_properties": "riftworlddt:crystal_birch",
        "generate_seed": True,
        "generate_sapling": True,
        "acceptable_soils": ["dirt_like"],
        "perfect_biomes": {"name": "riftworld:crystal_rifts"},
        "lang_overrides": {
            "seed": "Crystal Silverwood Seed",
            "sapling": "Crystal Silverwood Sapling",
        },
        "only_if_loaded": "riftworld",
        "sapling_shape": "slim_sapling",
        "world_gen_acceptable_soils": ["dirt_like"],
        "transformable": False,
        "does_rot": False,
    })

    # Generate & strictly verify miniature bonsai JoCodes for radii 2..8
    rng = random.Random(20261031)
    jo_lines = []
    for rad in range(2, 9):
        for _ in range(10):
            code = build_bonsai_crystal_tree(rng, rad)
            ok, msg = verify_jocode(code)
            if not ok:
                raise RuntimeError(f"Invalid JoCode for r={rad}: {msg} ({code})")
            jo_lines.append(f"{rad}:{code}")
    jo_path = os.path.join(DT_RES, "trees", "riftworlddt", "jo_codes", "crystal_birch.txt")
    os.makedirs(os.path.dirname(jo_path), exist_ok=True)
    with open(jo_path, "w", encoding="utf-8") as f:
        f.write("\n".join(jo_lines) + "\n")

    # CRITICAL: chance = 0.0 in Poisson world_gen so islands are crowned with CRYSTALS, NOT tree forests!
    # Only the 6-8 miniature avenue bonsai trees are planted by DtGround.plantChunk!
    wg_path = os.path.join(DT_RES, "trees", "riftworlddt", "world_gen", "default.json")
    with open(wg_path, "r", encoding="utf-8") as f:
        wg = json.load(f)
    wg = [entry for entry in wg if entry.get("select", {}).get("name") != "riftworld:crystal_rifts"]
    wg.append({
        "_comment": "Хрустальные разломы: на островах растут КРИСТАЛЛЫ, а не леса (chance: 0.0); редкие миниатюрные деревца высаживаются только вдоль мраморной аллеи.",
        "select": {"name": "riftworld:crystal_rifts"},
        "apply": {
            "species": "riftworlddt:crystal_birch",
            "density": [0.02],
            "chance": 0.0,
        },
    })
    write_json(wg_path, wg)

    dt_en_path = os.path.join(DT_RES, "assets", "riftworlddt", "lang", "en_us.json")
    dt_ru_path = os.path.join(DT_RES, "assets", "riftworlddt", "lang", "ru_ru.json")
    with open(dt_en_path, "r", encoding="utf-8") as f:
        dt_en = json.load(f)
    dt_en.update({
        "block.riftworlddt.crystal_birch_branch": "Crystal Silverwood Branch",
        "block.riftworlddt.crystal_birch_leaves": "Crystal Silverwood Leaves",
        "block.riftworlddt.crystal_birch_sapling": "Crystal Silverwood Sapling",
        "block.riftworlddt.rooty_rift_turf": "Rooty Rift Moss Turf",
        "item.riftworlddt.crystal_birch_seed": "Crystal Silverwood Seed",
        "item.riftworlddt.crystal_birch_branch": "Crystal Silverwood Branch",
        "species.riftworlddt.crystal_birch": "Crystal Silverwood",
        "family.riftworlddt.crystal_birch": "Crystal Silverwood",
    })
    write_json(dt_en_path, dt_en)

    with open(dt_ru_path, "r", encoding="utf-8") as f:
        dt_ru = json.load(f)
    dt_ru.update({
        "block.riftworlddt.crystal_birch_branch": "Ствол хрустальной серебрицы",
        "block.riftworlddt.crystal_birch_leaves": "Листва хрустальной серебрицы",
        "block.riftworlddt.crystal_birch_sapling": "Саженец хрустальной серебрицы",
        "block.riftworlddt.rooty_rift_turf": "Корневищный дёрн разломов",
        "item.riftworlddt.crystal_birch_seed": "Семя хрустальной серебрицы",
        "item.riftworlddt.crystal_birch_branch": "Ствол хрустальной серебрицы",
        "species.riftworlddt.crystal_birch": "Хрустальная серебрица",
        "family.riftworlddt.crystal_birch": "Хрустальная серебрица",
    })
    write_json(dt_ru_path, dt_ru)

    print(f"Generated all Crystal Rifts v0.18 assets & verified {len(jo_lines)} miniature bonsai JoCodes!")


if __name__ == "__main__":
    main()
