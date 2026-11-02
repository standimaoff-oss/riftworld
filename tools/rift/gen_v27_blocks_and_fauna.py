#!/usr/bin/env python3
"""
v0.27.0 Procedural Asset & Data Generator:
  - 30 brand-new RiftWorld blocks (16x16 pixel-art PNG textures, blockstates, block/item models,
    loot tables, crafting & stonecutting recipes, mineable/walls/stairs/slabs/replaceable_by_trees tags)
  - 2 detailed 64x64 entity textures (sky_dart_fish.png & blossom_stag.png) + 2 spawn egg item models
  - Overworld peaceful biome modifier for normal biomes (Plains, Meadow, Forest, Flower Forest, Birch, Cherry)
  - Localization entries in ru_ru.json & en_us.json
"""
import json
import math
import os
import struct
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "mod", "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "riftworld")
DATA = os.path.join(RES, "data")


def write_png(path: str, w: int, h: int, rgba_rows: list[list[tuple[int, int, int, int]]]) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        for x in range(w):
            r, g, b, a = rgba_rows[y][x]
            raw.extend((max(0, min(255, int(r))), max(0, min(255, int(g))),
                        max(0, min(255, int(b))), max(0, min(255, int(a)))))

    def chunk(tag: bytes, payload: bytes) -> bytes:
        crc = zlib.crc32(tag + payload) & 0xFFFFFFFF
        return struct.pack(">I", len(payload)) + tag + payload + struct.pack(">I", crc)

    ihdr = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)


def write_json(path: str, obj) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")


def h2(x: int, y: int, seed: int) -> float:
    n = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def clamp_rgb(r: float, g: float, b: float, a: int = 255) -> tuple[int, int, int, int]:
    return (max(0, min(255, int(round(r)))),
            max(0, min(255, int(round(g)))),
            max(0, min(255, int(round(b)))), a)


# ==============================================================================
# 1. PROCEDURAL 16x16 PIXEL-ART BLOCK TEXTURES
# ==============================================================================
def make_marble_base(base_rgb, vein_rgb, seed: int):
    rows = []
    for y in range(16):
        row = []
        for x in range(16):
            wave = math.sin((x + y * 0.7) * 0.55 + h2(x // 2, y // 2, seed) * 2.2)
            vein = max(0.0, 1.0 - abs(wave) * 3.2)
            jitter = (h2(x, y, seed + 7) - 0.5) * 6.0
            r = base_rgb[0] * (1.0 - vein * 0.35) + vein_rgb[0] * (vein * 0.35) + jitter
            g = base_rgb[1] * (1.0 - vein * 0.35) + vein_rgb[1] * (vein * 0.35) + jitter
            b = base_rgb[2] * (1.0 - vein * 0.35) + vein_rgb[2] * (vein * 0.35) + jitter
            row.append(clamp_rgb(r, g, b))
        rows.append(row)
    return rows


def make_bricks(base_rgb, mortar_rgb, seed: int, course_h: int = 4):
    rows = []
    for y in range(16):
        row = []
        course = y // course_h
        offset = (course % 2) * 4
        is_h_mortar = (y % course_h) == 0
        for x in range(16):
            is_v_mortar = ((x + offset) % 8) == 0
            if is_h_mortar or is_v_mortar:
                j = (h2(x, y, seed + 3) - 0.5) * 7.0
                row.append(clamp_rgb(mortar_rgb[0] + j, mortar_rgb[1] + j, mortar_rgb[2] + j))
            else:
                brick_tint = (h2((x + offset) // 8, course, seed + 11) - 0.5) * 9.0
                hi = 5.0 if (y % course_h == 1 or (x + offset) % 8 == 1) else (-3.0 if (y % course_h == course_h - 1) else 0.0)
                j = (h2(x, y, seed + 19) - 0.5) * 5.0
                row.append(clamp_rgb(base_rgb[0] + brick_tint + hi + j,
                                     base_rgb[1] + brick_tint + hi + j,
                                     base_rgb[2] + brick_tint + hi + j))
        rows.append(row)
    return rows


def generate_block_textures():
    tex_dir = os.path.join(ASSETS, "textures", "block")

    # 1. Ivory Marble family (luminous warm snow-white)
    write_png(os.path.join(tex_dir, "ivory_marble.png"), 16, 16,
              make_marble_base((246, 244, 238), (218, 214, 204), 101))
    write_png(os.path.join(tex_dir, "ivory_marble_bricks.png"), 16, 16,
              make_bricks((244, 241, 234), (212, 207, 196), 102, 4))

    # Ivory marble pillar (side + top)
    p_side, p_top = [], []
    for y in range(16):
        rs, rt = [], []
        for x in range(16):
            flute = math.cos((x - 0.5) * math.pi / 2.0) * 9.0
            cap = -8.0 if y in (0, 15) else (5.0 if y in (1, 14) else 0.0)
            j = (h2(x, y, 103) - 0.5) * 4.0
            rs.append(clamp_rgb(242 + flute + cap + j, 239 + flute + cap + j, 232 + flute + cap + j))
            d = max(abs(x - 7.5), abs(y - 7.5))
            ring = -12.0 if abs(d - 6.5) < 0.6 or abs(d - 3.5) < 0.6 else 4.0
            rt.append(clamp_rgb(241 + ring + j, 238 + ring + j, 230 + ring + j))
        p_side.append(rs)
        p_top.append(rt)
    write_png(os.path.join(tex_dir, "ivory_marble_pillar.png"), 16, 16, p_side)
    write_png(os.path.join(tex_dir, "ivory_marble_pillar_top.png"), 16, 16, p_top)

    # Chiseled ivory marble (carved relief rosette)
    chis = []
    for y in range(16):
        row = []
        for x in range(16):
            dx, dy = abs(x - 7.5), abs(y - 7.5)
            box = max(dx, dy)
            rad = math.hypot(x - 7.5, y - 7.5)
            j = (h2(x, y, 104) - 0.5) * 4.0
            if 5.8 <= box <= 6.8 or 2.8 <= box <= 3.6:
                v = -16.0
            elif rad <= 2.1:
                v = 7.0
            else:
                v = 2.0
            row.append(clamp_rgb(242 + v + j, 239 + v + j, 231 + v + j))
        chis.append(row)
    write_png(os.path.join(tex_dir, "chiseled_ivory_marble.png"), 16, 16, chis)

    # Ivory marble tiles (8x8 diagonal-chamfered white plaza tiles!)
    tiles = []
    for y in range(16):
        row = []
        for x in range(16):
            tx, ty = x % 8, y % 8
            seam = (tx == 0 or ty == 0)
            diamond = (tx + ty <= 1) or (tx >= 7 and ty >= 7)
            j = (h2(x, y, 105) - 0.5) * 4.5
            if diamond:
                row.append(clamp_rgb(208 + j, 204 + j, 195 + j))
            elif seam:
                row.append(clamp_rgb(220 + j, 216 + j, 207 + j))
            else:
                hi = 4.0 if (tx == 1 or ty == 1) else 0.0
                row.append(clamp_rgb(246 + hi + j, 244 + hi + j, 238 + hi + j))
        tiles.append(row)
    write_png(os.path.join(tex_dir, "ivory_marble_tiles.png"), 16, 16, tiles)

    # Gilded ivory marble (delicate warm champagne-gold border inlay, NOT noisy!)
    gilded = []
    for y in range(16):
        row = []
        for x in range(16):
            border = (x in (1, 14) and 1 <= y <= 14) or (y in (1, 14) and 1 <= x <= 14)
            inner_dot = (x in (7, 8) and y in (7, 8))
            j = (h2(x, y, 106) - 0.5) * 4.0
            if border or inner_dot:
                row.append(clamp_rgb(224 + j, 196 + j, 126 + j))
            elif x in (0, 15) or y in (0, 15):
                row.append(clamp_rgb(226 + j, 222 + j, 212 + j))
            else:
                row.append(clamp_rgb(246 + j, 244 + j, 237 + j))
        gilded.append(row)
    write_png(os.path.join(tex_dir, "gilded_ivory_marble.png"), 16, 16, gilded)

    # Ivory paving stone (scalloped white cobblestone walkway fan pattern!)
    pave = []
    for y in range(16):
        row = []
        for x in range(16):
            cx = 8.0 if (y // 8) % 2 == 0 else (0.0 if x < 8 else 16.0)
            cy = 8.0 if (y // 8) % 2 == 0 else 16.0
            rad = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            ang = math.atan2(y + 0.5 - cy, x + 0.5 - cx)
            seam = abs((rad % 3.2) - 0.35) < 0.38 or abs(((ang * 5.0) % 1.0) - 0.5) > 0.43
            j = (h2(x, y, 107) - 0.5) * 6.0
            if seam:
                row.append(clamp_rgb(206 + j, 203 + j, 194 + j))
            else:
                row.append(clamp_rgb(241 + j, 239 + j, 232 + j))
        pave.append(row)
    write_png(os.path.join(tex_dir, "ivory_paving_stone.png"), 16, 16, pave)

    # 2. Celadon Roof Tiles & Pearl Calcite family
    cel = []
    for y in range(16):
        row = []
        course = y // 4
        off = (course % 2) * 4
        for x in range(16):
            ux = (x + off) % 8
            uy = y % 4
            arch = (uy == 3 and ux in (0, 7)) or (uy == 0 and ux in (3, 4))
            seam = (uy == 3 and 1 <= ux <= 6) or (ux == 0 and uy >= 1)
            j = (h2(x, y, 108) - 0.5) * 6.0
            if seam or arch:
                row.append(clamp_rgb(108 + j, 152 + j, 146 + j))
            else:
                glaze = 12.0 if (uy == 1 and 2 <= ux <= 5) else 0.0
                row.append(clamp_rgb(146 + glaze + j, 196 + glaze + j, 188 + glaze + j))
        cel.append(row)
    write_png(os.path.join(tex_dir, "celadon_tiles.png"), 16, 16, cel)

    write_png(os.path.join(tex_dir, "pearl_calcite.png"), 16, 16,
              make_marble_base((235, 240, 244), (196, 212, 224), 109))
    write_png(os.path.join(tex_dir, "pearl_calcite_bricks.png"), 16, 16,
              make_bricks((234, 239, 243), (195, 207, 218), 110, 4))

    # 3. Luminous & Peaceful Flora family
    moss = []
    for y in range(16):
        row = []
        for x in range(16):
            j = (h2(x, y, 111) - 0.5) * 14.0
            petal = h2(x, y, 711) > 0.90
            center = h2(x + 1, y, 711) > 0.90 and h2(x, y, 712) > 0.55
            if petal:
                row.append(clamp_rgb(248, 246, 242))
            elif center:
                row.append(clamp_rgb(242, 214, 132))
            else:
                row.append(clamp_rgb(112 + j, 168 + j, 118 + j))
        moss.append(row)
    write_png(os.path.join(tex_dir, "blossom_moss_block.png"), 16, 16, moss)

    # White lily cluster (transparent 16x16 cross plant)
    lily = [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]
    stems = [(4, 15, 5, 7), (8, 15, 8, 5), (12, 15, 11, 8), (6, 15, 3, 10), (10, 15, 13, 10)]
    for sx, sy, ex, ey in stems:
        for s in range(12):
            t = s / 11.0
            px, py = int(round(sx + (ex - sx) * t)), int(round(sy + (ey - sy) * t))
            if 0 <= px < 16 and 0 <= py < 16:
                lily[py][px] = (86, 146, 96, 255)
                if py >= 11 and px + 1 < 16:
                    lily[py][px + 1] = (72, 130, 84, 255)
    blossoms = [(5, 6), (8, 4), (11, 7), (3, 9), (13, 9)]
    for bx, by in blossoms:
        for dy in range(-2, 2):
            for dx in range(-2, 3):
                px, py = bx + dx, by + dy
                if 0 <= px < 16 and 0 <= py < 16 and abs(dx) + abs(dy) <= 2:
                    if dx == 0 and dy == 0:
                        lily[py][px] = (248, 222, 136, 255)
                    elif dy == 1 and dx == 0:
                        lily[py][px] = (224, 232, 224, 255)
                    else:
                        lily[py][px] = (252, 250, 247, 255)
    write_png(os.path.join(tex_dir, "white_lily_cluster.png"), 16, 16, lily)

    # Celadon fern (transparent 16x16 lush jade-seafoam fern fronds)
    fern = [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]
    fronds = [(8, 15, 8, 2), (7, 15, 2, 5), (9, 15, 13, 5), (6, 15, 1, 10), (10, 15, 14, 10)]
    for sx, sy, ex, ey in fronds:
        for s in range(16):
            t = s / 15.0
            px = int(round(sx + (ex - sx) * t))
            py = int(round(sy + (ey - sy) * t))
            if 0 <= px < 16 and 0 <= py < 16:
                fern[py][px] = clamp_rgb(92 + 54 * t, 158 + 46 * t, 138 + 42 * t)
                for leaf_dx in (-1, 1):
                    lx = px + leaf_dx
                    if 0 <= lx < 16 and s % 2 == 1 and 0.18 < t < 0.92:
                        fern[py][lx] = clamp_rgb(118 + 52 * t, 186 + 38 * t, 166 + 36 * t)
    write_png(os.path.join(tex_dir, "celadon_fern.png"), 16, 16, fern)

    # Pearl prism lantern (ivory frame + glowing celadon-pearl core)
    plantern = []
    for y in range(16):
        row = []
        for x in range(16):
            edge = x in (0, 1, 14, 15) or y in (0, 1, 14, 15)
            lattice = (x + y) % 7 == 0 and not edge
            d = math.hypot(x - 7.5, y - 7.5)
            if edge:
                row.append(clamp_rgb(224, 218, 204))
            elif lattice:
                row.append(clamp_rgb(212, 198, 154))
            else:
                glow = max(0.0, 1.0 - d / 8.5)
                row.append(clamp_rgb(216 + 39 * glow, 242 + 13 * glow, 238 + 17 * glow))
        plantern.append(row)
    write_png(os.path.join(tex_dir, "pearl_prism_lantern.png"), 16, 16, plantern)

    # 4. Blacktide Obsidian-Basalt & Fortress family
    write_png(os.path.join(tex_dir, "obsidian_bricks.png"), 16, 16,
              make_bricks((36, 33, 44), (20, 18, 26), 201, 4))

    # Carved basalt runes (dark basalt + warm amber glowing carved sigils)
    runes = []
    rune_pts = {
        (4, 3), (5, 3), (6, 4), (6, 5), (5, 6), (4, 7), (4, 8), (5, 9), (6, 9),
        (10, 4), (11, 3), (12, 4), (11, 5), (11, 6), (11, 7), (10, 8), (12, 8),
        (7, 12), (8, 11), (9, 12), (8, 13)
    }
    for y in range(16):
        row = []
        for x in range(16):
            j = (h2(x, y, 202) - 0.5) * 7.0
            if (x, y) in rune_pts:
                row.append(clamp_rgb(245, 162, 68))
            elif any(abs(x - rx) + abs(y - ry) == 1 for rx, ry in rune_pts):
                row.append(clamp_rgb(92 + j, 58 + j, 42 + j))
            elif y in (0, 15):
                row.append(clamp_rgb(28 + j, 27 + j, 32 + j))
            else:
                row.append(clamp_rgb(48 + j, 46 + j, 54 + j))
        runes.append(row)
    write_png(os.path.join(tex_dir, "carved_basalt_runes.png"), 16, 16, runes)

    # Ember brazier block (wrought-iron cage + glowing forge coals)
    braz_side, braz_top = [], []
    for y in range(16):
        rs, rt = [], []
        for x in range(16):
            bar = x in (0, 1, 7, 8, 14, 15) or y in (0, 1, 2, 13, 14, 15)
            j = (h2(x, y, 203) - 0.5) * 16.0
            if bar:
                rs.append(clamp_rgb(42 + j * 0.3, 40 + j * 0.3, 46 + j * 0.3))
            else:
                heat = max(0.0, 1.0 - abs(y - 9.0) / 7.0)
                rs.append(clamp_rgb(195 + 58 * heat + j, 82 + 88 * heat + j * 0.5, 28 + 25 * heat))
            rim = x in (0, 1, 14, 15) or y in (0, 1, 14, 15)
            if rim:
                rt.append(clamp_rgb(44 + j * 0.3, 42 + j * 0.3, 48 + j * 0.3))
            else:
                d = math.hypot(x - 7.5, y - 7.5)
                heat = max(0.0, 1.0 - d / 8.0)
                rt.append(clamp_rgb(205 + 48 * heat + j, 92 + 95 * heat + j * 0.5, 32 + 30 * heat))
        braz_side.append(rs)
        braz_top.append(rt)
    write_png(os.path.join(tex_dir, "ember_brazier_side.png"), 16, 16, braz_side)
    write_png(os.path.join(tex_dir, "ember_brazier_top.png"), 16, 16, braz_top)

    # Ironbound dark planks (dark smoked oak + riveted wrought-iron straps)
    ib_planks = []
    for y in range(16):
        row = []
        strap = y in (2, 3, 12, 13)
        seam = y in (0, 8)
        for x in range(16):
            rivet = (y in (2, 12)) and (x in (2, 7, 12))
            j = (h2(x, y, 204) - 0.5) * 6.0
            if rivet:
                row.append(clamp_rgb(132 + j, 134 + j, 142 + j))
            elif strap:
                row.append(clamp_rgb(58 + j, 60 + j, 66 + j))
            elif seam:
                row.append(clamp_rgb(38 + j, 28 + j, 22 + j))
            else:
                grain = math.sin(x * 0.6 + (y // 4) * 1.9) * 4.0
                row.append(clamp_rgb(70 + grain + j, 50 + grain + j, 36 + grain + j))
        ib_planks.append(row)
    write_png(os.path.join(tex_dir, "ironbound_dark_planks.png"), 16, 16, ib_planks)

    # Ashen cobblestone (weathered dark basalt + pale volcanic ash mortar)
    ash_cob = []
    centers = [(3, 3), (10, 2), (14, 6), (5, 9), (11, 10), (3, 14), (10, 14), (15, 13)]
    for y in range(16):
        row = []
        for x in range(16):
            dists = sorted(math.hypot(((x - cx + 8) % 16) - 8, ((y - cy + 8) % 16) - 8) for cx, cy in centers)
            edge = (dists[1] - dists[0]) < 0.85
            j = (h2(x, y, 205) - 0.5) * 9.0
            if edge:
                row.append(clamp_rgb(104 + j, 100 + j, 98 + j))
            else:
                row.append(clamp_rgb(62 + j, 60 + j, 65 + j))
        ash_cob.append(row)
    write_png(os.path.join(tex_dir, "ashen_cobblestone.png"), 16, 16, ash_cob)


# ==============================================================================
# 2. BLOCKSTATES, MODELS, LOOT TABLES, RECIPES & MINECRAFT TAGS (30 BLOCKS)
# ==============================================================================
CUBE_BLOCKS = [
    ("ivory_marble", "riftworld:block/ivory_marble"),
    ("ivory_marble_bricks", "riftworld:block/ivory_marble_bricks"),
    ("chiseled_ivory_marble", "riftworld:block/chiseled_ivory_marble"),
    ("ivory_marble_tiles", "riftworld:block/ivory_marble_tiles"),
    ("gilded_ivory_marble", "riftworld:block/gilded_ivory_marble"),
    ("ivory_paving_stone", "riftworld:block/ivory_paving_stone"),
    ("celadon_tiles", "riftworld:block/celadon_tiles"),
    ("pearl_calcite", "riftworld:block/pearl_calcite"),
    ("pearl_calcite_bricks", "riftworld:block/pearl_calcite_bricks"),
    ("blossom_moss_block", "riftworld:block/blossom_moss_block"),
    ("pearl_prism_lantern", "riftworld:block/pearl_prism_lantern"),
    ("obsidian_bricks", "riftworld:block/obsidian_bricks"),
    ("carved_basalt_runes", "riftworld:block/carved_basalt_runes"),
    ("ironbound_dark_planks", "riftworld:block/ironbound_dark_planks"),
    ("ashen_cobblestone", "riftworld:block/ashen_cobblestone"),
]

STAIR_FAMILIES = [
    ("ivory_marble_stairs", "ivory_marble", "riftworld:block/ivory_marble"),
    ("celadon_tiles_stairs", "celadon_tiles", "riftworld:block/celadon_tiles"),
    ("pearl_calcite_stairs", "pearl_calcite_bricks", "riftworld:block/pearl_calcite_bricks"),
    ("obsidian_bricks_stairs", "obsidian_bricks", "riftworld:block/obsidian_bricks"),
]

SLAB_FAMILIES = [
    ("ivory_marble_slab", "ivory_marble", "riftworld:block/ivory_marble"),
    ("celadon_tiles_slab", "celadon_tiles", "riftworld:block/celadon_tiles"),
    ("pearl_calcite_slab", "pearl_calcite_bricks", "riftworld:block/pearl_calcite_bricks"),
    ("obsidian_bricks_slab", "obsidian_bricks", "riftworld:block/obsidian_bricks"),
]

WALL_FAMILIES = [
    ("ivory_marble_wall", "ivory_marble_bricks", "riftworld:block/ivory_marble_bricks"),
    ("pearl_calcite_wall", "pearl_calcite_bricks", "riftworld:block/pearl_calcite_bricks"),
    ("obsidian_bricks_wall", "obsidian_bricks", "riftworld:block/obsidian_bricks"),
]

PLANT_BLOCKS = ["white_lily_cluster", "celadon_fern"]


def write_drop_self_loot(block_id: str, is_slab: bool = False):
    path = os.path.join(DATA, "riftworld", "loot_tables", "blocks", f"{block_id}.json")
    entry = {"type": "minecraft:item", "name": f"riftworld:{block_id}"}
    if is_slab:
        entry["functions"] = [
            {
                "function": "minecraft:set_count",
                "count": 2.0,
                "conditions": [{
                    "condition": "minecraft:block_state_property",
                    "block": f"riftworld:{block_id}",
                    "properties": {"type": "double"}
                }]
            },
            {"function": "minecraft:explosion_decay"}
        ]
        pool = {"rolls": 1.0, "entries": [entry]}
    else:
        pool = {
            "rolls": 1.0,
            "entries": [entry],
            "conditions": [{"condition": "minecraft:survives_explosion"}]
        }
    write_json(path, {"type": "minecraft:block", "pools": [pool]})


def generate_block_jsons():
    bs_dir = os.path.join(ASSETS, "blockstates")
    bm_dir = os.path.join(ASSETS, "models", "block")
    im_dir = os.path.join(ASSETS, "models", "item")
    rc_dir = os.path.join(DATA, "riftworld", "recipes")

    # A) Cube blocks
    for bid, tex in CUBE_BLOCKS:
        write_json(os.path.join(bs_dir, f"{bid}.json"),
                   {"variants": {"": {"model": f"riftworld:block/{bid}"}}})
        write_json(os.path.join(bm_dir, f"{bid}.json"),
                   {"parent": "minecraft:block/cube_all", "textures": {"all": tex}})
        write_json(os.path.join(im_dir, f"{bid}.json"),
                   {"parent": f"riftworld:block/{bid}"})
        write_drop_self_loot(bid)

    # B) Pillar block: ivory_marble_pillar
    write_json(os.path.join(bs_dir, "ivory_marble_pillar.json"), {
        "variants": {
            "axis=x": {"model": "riftworld:block/ivory_marble_pillar_horizontal", "x": 90, "y": 90},
            "axis=y": {"model": "riftworld:block/ivory_marble_pillar"},
            "axis=z": {"model": "riftworld:block/ivory_marble_pillar_horizontal", "x": 90}
        }
    })
    write_json(os.path.join(bm_dir, "ivory_marble_pillar.json"), {
        "parent": "minecraft:block/cube_column",
        "textures": {
            "end": "riftworld:block/ivory_marble_pillar_top",
            "side": "riftworld:block/ivory_marble_pillar"
        }
    })
    write_json(os.path.join(bm_dir, "ivory_marble_pillar_horizontal.json"), {
        "parent": "minecraft:block/cube_column_horizontal",
        "textures": {
            "end": "riftworld:block/ivory_marble_pillar_top",
            "side": "riftworld:block/ivory_marble_pillar"
        }
    })
    write_json(os.path.join(im_dir, "ivory_marble_pillar.json"),
               {"parent": "riftworld:block/ivory_marble_pillar"})
    write_drop_self_loot("ivory_marble_pillar")

    # C) Ember brazier block (cube_bottom_top)
    write_json(os.path.join(bs_dir, "ember_brazier_block.json"),
               {"variants": {"": {"model": "riftworld:block/ember_brazier_block"}}})
    write_json(os.path.join(bm_dir, "ember_brazier_block.json"), {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {
            "top": "riftworld:block/ember_brazier_top",
            "bottom": "riftworld:block/obsidian_bricks",
            "side": "riftworld:block/ember_brazier_side"
        }
    })
    write_json(os.path.join(im_dir, "ember_brazier_block.json"),
               {"parent": "riftworld:block/ember_brazier_block"})
    write_drop_self_loot("ember_brazier_block")

    # D) Stairs
    for sid, base_id, tex in STAIR_FAMILIES:
        write_json(os.path.join(bm_dir, f"{sid}.json"),
                   {"parent": "minecraft:block/stairs", "textures": {"bottom": tex, "top": tex, "side": tex}})
        write_json(os.path.join(bm_dir, f"{sid}_inner.json"),
                   {"parent": "minecraft:block/inner_stairs", "textures": {"bottom": tex, "top": tex, "side": tex}})
        write_json(os.path.join(bm_dir, f"{sid}_outer.json"),
                   {"parent": "minecraft:block/outer_stairs", "textures": {"bottom": tex, "top": tex, "side": tex}})
        write_json(os.path.join(im_dir, f"{sid}.json"), {"parent": f"riftworld:block/{sid}"})
        write_drop_self_loot(sid)

        # Standard vanilla 40-variant stair blockstate
        variants = {}
        facings = [("east", 0), ("south", 90), ("west", 180), ("north", 270)]
        for facing, fy in facings:
            for half in ("bottom", "top"):
                xrot = 0 if half == "bottom" else 180
                for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                    m = f"riftworld:block/{sid}" + ("" if shape == "straight" else ("_inner" if "inner" in shape else "_outer"))
                    yrot = fy
                    if shape in ("inner_left", "outer_left"):
                        yrot = (fy + (270 if half == "bottom" else 0)) % 360
                    elif shape in ("inner_right", "outer_right"):
                        yrot = (fy + (0 if half == "bottom" else 90)) % 360
                    entry = {"model": m}
                    if xrot:
                        entry["x"] = xrot
                    if yrot:
                        entry["y"] = yrot
                    if xrot or yrot:
                        entry["uvlock"] = True
                    variants[f"facing={facing},half={half},shape={shape}"] = entry
        write_json(os.path.join(bs_dir, f"{sid}.json"), {"variants": variants})

        write_json(os.path.join(rc_dir, f"{sid}.json"), {
            "type": "minecraft:crafting_shaped",
            "pattern": ["#  ", "## ", "###"],
            "key": {"#": {"item": f"riftworld:{base_id}"}},
            "result": {"item": f"riftworld:{sid}", "count": 4}
        })

    # E) Slabs
    for slid, base_id, tex in SLAB_FAMILIES:
        write_json(os.path.join(bm_dir, f"{slid}.json"),
                   {"parent": "minecraft:block/slab", "textures": {"bottom": tex, "top": tex, "side": tex}})
        write_json(os.path.join(bm_dir, f"{slid}_top.json"),
                   {"parent": "minecraft:block/slab_top", "textures": {"bottom": tex, "top": tex, "side": tex}})
        write_json(os.path.join(bs_dir, f"{slid}.json"), {
            "variants": {
                "type=bottom": {"model": f"riftworld:block/{slid}"},
                "type=top": {"model": f"riftworld:block/{slid}_top"},
                "type=double": {"model": f"riftworld:block/{base_id}"}
            }
        })
        write_json(os.path.join(im_dir, f"{slid}.json"), {"parent": f"riftworld:block/{slid}"})
        write_drop_self_loot(slid, is_slab=True)
        write_json(os.path.join(rc_dir, f"{slid}.json"), {
            "type": "minecraft:crafting_shaped",
            "pattern": ["###"],
            "key": {"#": {"item": f"riftworld:{base_id}"}},
            "result": {"item": f"riftworld:{slid}", "count": 6}
        })

    # F) Walls
    for wid, base_id, tex in WALL_FAMILIES:
        write_json(os.path.join(bm_dir, f"{wid}_post.json"),
                   {"parent": "minecraft:block/template_wall_post", "textures": {"wall": tex}})
        write_json(os.path.join(bm_dir, f"{wid}_side.json"),
                   {"parent": "minecraft:block/template_wall_side", "textures": {"wall": tex}})
        write_json(os.path.join(bm_dir, f"{wid}_side_tall.json"),
                   {"parent": "minecraft:block/template_wall_side_tall", "textures": {"wall": tex}})
        write_json(os.path.join(bm_dir, f"{wid}_inventory.json"),
                   {"parent": "minecraft:block/wall_inventory", "textures": {"wall": tex}})
        write_json(os.path.join(im_dir, f"{wid}.json"), {"parent": f"riftworld:block/{wid}_inventory"})
        write_drop_self_loot(wid)

        multipart = [{"when": {"up": "true"}, "apply": {"model": f"riftworld:block/{wid}_post"}}]
        for dname, yrot in [("north", 0), ("east", 90), ("south", 180), ("west", 270)]:
            apply_low = {"model": f"riftworld:block/{wid}_side", "uvlock": True}
            apply_tall = {"model": f"riftworld:block/{wid}_side_tall", "uvlock": True}
            if yrot:
                apply_low["y"] = yrot
                apply_tall["y"] = yrot
            multipart.append({"when": {dname: "low"}, "apply": apply_low})
            multipart.append({"when": {dname: "tall"}, "apply": apply_tall})
        write_json(os.path.join(bs_dir, f"{wid}.json"), {"multipart": multipart})
        write_json(os.path.join(rc_dir, f"{wid}.json"), {
            "type": "minecraft:crafting_shaped",
            "pattern": ["###", "###"],
            "key": {"#": {"item": f"riftworld:{base_id}"}},
            "result": {"item": f"riftworld:{wid}", "count": 6}
        })

    # G)Lush Cross Plants (white_lily_cluster & celadon_fern)
    for pid in PLANT_BLOCKS:
        write_json(os.path.join(bs_dir, f"{pid}.json"),
                   {"variants": {"": {"model": f"riftworld:block/{pid}"}}})
        write_json(os.path.join(bm_dir, f"{pid}.json"), {
            "ambientocclusion": false_val(),
            "render_type": "minecraft:cutout",
            "parent": "minecraft:block/cross",
            "textures": {"cross": f"riftworld:block/{pid}"}
        })
        write_json(os.path.join(im_dir, f"{pid}.json"), {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"riftworld:block/{pid}"}
        })
        write_drop_self_loot(pid)

    # H) Additional Crafting Recipes for Core Blocks
    extra_recipes = [
        ("ivory_marble", ["QC", "CQ"], {"Q": "minecraft:quartz_block", "C": "minecraft:calcite"}, 4),
        ("ivory_marble_bricks", ["##", "##"], {"#": "riftworld:ivory_marble"}, 4),
        ("ivory_marble_tiles", ["##", "##"], {"#": "riftworld:ivory_marble_bricks"}, 4),
        ("ivory_marble_pillar", ["#", "#"], {"#": "riftworld:ivory_marble"}, 2),
        ("chiseled_ivory_marble", ["#", "#"], {"#": "riftworld:ivory_marble_slab"}, 1),
        ("gilded_ivory_marble", [" N ", "N#N", " N "], {"#": "riftworld:ivory_marble", "N": "minecraft:gold_nugget"}, 1),
        ("ivory_paving_stone", ["#S", "S#"], {"#": "riftworld:ivory_marble", "S": "riftworld:elisar_stone"}, 4),
        ("celadon_tiles", ["##", "##"], {"#": "minecraft:prismarine_shard"}, 2),
        ("pearl_calcite", ["CC", "CC"], {"C": "minecraft:calcite"}, 4),
        ("pearl_calcite_bricks", ["##", "##"], {"#": "riftworld:pearl_calcite"}, 4),
        ("pearl_prism_lantern", [" I ", "I#I", " I "], {"I": "riftworld:ivory_marble", "#": "minecraft:sea_lantern"}, 2),
        ("obsidian_bricks", ["OB", "BO"], {"O": "minecraft:obsidian", "B": "minecraft:polished_basalt"}, 4),
        ("carved_basalt_runes", [" B ", "BMB", " B "], {"B": "riftworld:obsidian_bricks", "M": "minecraft:blaze_powder"}, 4),
        ("ember_brazier_block", ["I I", "ICI", "BBB"], {"I": "minecraft:iron_bars", "C": "minecraft:campfire", "B": "riftworld:obsidian_bricks"}, 1),
        ("ironbound_dark_planks", [" N ", "P#P", " N "], {"N": "minecraft:iron_nugget", "P": "riftworld:dark_planks", "#": "minecraft:iron_ingot"}, 4),
        ("ashen_cobblestone", ["SA", "AS"], {"S": "riftworld:scorched_stone", "A": "riftworld:ash_block"}, 4),
    ]
    for rid, pat, keys, cnt in extra_recipes:
        write_json(os.path.join(rc_dir, f"{rid}.json"), {
            "type": "minecraft:crafting_shaped",
            "pattern": pat,
            "key": {k: {"item": v} for k, v in keys.items()},
            "result": {"item": f"riftworld:{rid}", "count": cnt}
        })

    # I) Update Minecraft Block Tags
    def append_tag(rel_path: str, additions: list[str]):
        full = os.path.join(DATA, "minecraft", "tags", "blocks", rel_path)
        if os.path.exists(full):
            with open(full, "r", encoding="utf-8") as f:
                data = json.load(f)
        else:
            data = {"replace": False, "values": []}
        vals = list(data.get("values", []))
        for a in additions:
            if a not in vals:
                vals.append(a)
        data["values"] = vals
        write_json(full, data)

    append_tag("walls.json", [f"riftworld:{w[0]}" for w in WALL_FAMILIES])
    append_tag("stairs.json", [f"riftworld:{s[0]}" for s in STAIR_FAMILIES])
    append_tag("slabs.json", [f"riftworld:{s[0]}" for s in SLAB_FAMILIES])
    append_tag("replaceable_by_trees.json", [f"riftworld:{p}" for p in PLANT_BLOCKS])
    append_tag("mineable/pickaxe.json", [
        "riftworld:ivory_marble", "riftworld:ivory_marble_bricks", "riftworld:ivory_marble_stairs",
        "riftworld:ivory_marble_slab", "riftworld:ivory_marble_wall", "riftworld:ivory_marble_pillar",
        "riftworld:chiseled_ivory_marble", "riftworld:ivory_marble_tiles", "riftworld:gilded_ivory_marble",
        "riftworld:ivory_paving_stone", "riftworld:celadon_tiles", "riftworld:celadon_tiles_stairs",
        "riftworld:celadon_tiles_slab", "riftworld:pearl_calcite", "riftworld:pearl_calcite_bricks",
        "riftworld:pearl_calcite_stairs", "riftworld:pearl_calcite_slab", "riftworld:pearl_calcite_wall",
        "riftworld:pearl_prism_lantern", "riftworld:obsidian_bricks", "riftworld:obsidian_bricks_stairs",
        "riftworld:obsidian_bricks_slab", "riftworld:obsidian_bricks_wall", "riftworld:carved_basalt_runes",
        "riftworld:ember_brazier_block", "riftworld:ashen_cobblestone"
    ])
    append_tag("mineable/axe.json", ["riftworld:ironbound_dark_planks"])
    append_tag("mineable/hoe.json", ["riftworld:blossom_moss_block"])


def false_val():
    return False


# ==============================================================================
# 3. 64x64 ENTITY TEXTURES + SPAWN EGG MODELS + OVERWORLD BIOME MODIFIER
# ==============================================================================
def generate_entity_textures_and_eggs():
    ent_dir = os.path.join(ASSETS, "textures", "entity")

    # 3A) sky_dart_fish.png (64x64):
    #   UV regions:
    #     (0..31, 0..31)   -> creamy porcelain-ivory body with subtle celadon-aqua lateral sheen
    #     (32..63, 0..31)  -> pleated seafoam-celadon pectoral & tail fins (matching Photo 1!)
    #     (0..63, 32..55)  -> translucent iridescent dragonfly wing membrane with fine dark-celadon veins!
    #     (0..15, 56..63)  -> glossy dark almond eye with crisp white specular glint
    fish_tex = [[(244, 241, 234, 255) for _ in range(64)] for _ in range(64)]
    for y in range(64):
        for x in range(64):
            j = (h2(x, y, 501) - 0.5) * 4.0
            if y < 32 and x < 32:
                # Porcelain ivory body + celadon belly/flank gradient
                u, v = x / 31.0, y / 31.0
                cel_mix = max(0.0, math.sin(v * math.pi) * 0.28)
                r = 247 * (1.0 - cel_mix) + 172 * cel_mix + j
                g = 244 * (1.0 - cel_mix) + 216 * cel_mix + j
                b = 236 * (1.0 - cel_mix) + 206 * cel_mix + j
                fish_tex[y][x] = clamp_rgb(r, g, b, 255)
            elif y < 32 and x >= 32:
                # Pleated seafoam-celadon fins (Photo 1!)
                pleat = math.sin((x - 32) * 1.35) * 14.0
                tip = (y / 31.0)
                r = 152 + 68 * tip + pleat + j
                g = 204 + 34 * tip + pleat * 0.8 + j
                b = 194 + 36 * tip + pleat * 0.8 + j
                fish_tex[y][x] = clamp_rgb(r, g, b, 242)
            elif 32 <= y < 56:
                # Translucent dragonfly wing with costa vein + hexagonal dragonfly cell venation!
                wx, wy = x / 63.0, (y - 32) / 23.0
                costa = wy < 0.11
                main_vein = abs(wy - 0.28 - wx * 0.14) < 0.045 or abs(wy - 0.54 - wx * 0.18) < 0.04
                cell_x = (x * 0.42 + (y % 4) * 0.35) % 3.0
                cell_y = ((y - 32) * 0.55 + (x % 5) * 0.25) % 3.0
                net_vein = cell_x < 0.36 or cell_y < 0.36
                if costa:
                    fish_tex[y][x] = (92, 138, 136, 245)
                elif main_vein:
                    fish_tex[y][x] = (116, 168, 166, 228)
                elif net_vein:
                    fish_tex[y][x] = (164, 212, 208, 196)
                else:
                    irid = math.sin(wx * 5.0 + wy * 3.0) * 12.0
                    fish_tex[y][x] = clamp_rgb(220 + irid, 246 + irid * 0.4, 244 + irid * 0.5, 136)
            else:
                # Eye & warm nose details
                if x < 16:
                    d = math.hypot(x - 7.5, (y - 56) - 3.5)
                    if x in (5, 6) and y in (58, 59):
                        fish_tex[y][x] = (255, 255, 255, 255)
                    elif d < 3.6:
                        fish_tex[y][x] = (24, 28, 36, 255)
                    else:
                        fish_tex[y][x] = (232, 226, 216, 255)
                else:
                    fish_tex[y][x] = (236, 212, 198, 255)
    write_png(os.path.join(ent_dir, "sky_dart_fish.png"), 64, 64, fish_tex)

    # 3B) blossom_stag.png (64x64):
    #   UV regions:
    #     (0..31, 0..31)   -> pure snow-white / alabaster coat with subtle silky shading
    #     (32..47, 0..15)  -> warm peach-rose inner ear & soft blush muzzle (matching Photo 2!)
    #     (48..63, 0..15)  -> pale ivory-champagne antlers & dainty hooves
    #     (32..63, 16..31) -> emerald calyxes & green antler shoots
    #     (0..31, 32..63)  -> snow-white & sakura-blush flower petals with warm golden stamens
    #     (32..47, 32..47) -> pale lilac-pearl flower petals
    #     (48..63, 48..63) -> serene dark-espresso almond eye with white highlight
    stag_tex = [[(250, 249, 246, 255) for _ in range(64)] for _ in range(64)]
    for y in range(64):
        for x in range(64):
            j = (h2(x, y, 601) - 0.5) * 3.5
            if y < 32 and x < 32:
                shade = math.sin((y / 31.0) * math.pi) * 5.0
                stag_tex[y][x] = clamp_rgb(246 + shade + j, 245 + shade + j, 242 + shade + j)
            elif y < 16 and 32 <= x < 48:
                # Warm peach-blush inner ear (Photo 2!)
                u = (x - 32) / 15.0
                v = y / 15.0
                warm = max(0.0, 1.0 - math.hypot(u - 0.5, v - 0.55) * 1.35)
                stag_tex[y][x] = clamp_rgb(245 - 6 * warm + j, 224 - 28 * warm + j, 214 - 36 * warm + j)
            elif y < 16 and x >= 48:
                # Pale ivory antler & hoof keratin
                grain = math.sin(y * 0.9) * 5.0
                stag_tex[y][x] = clamp_rgb(236 + grain + j, 228 + grain + j, 212 + grain + j)
            elif 16 <= y < 32 and x >= 32:
                # Fresh spring leaf calyx
                stag_tex[y][x] = clamp_rgb(106 + j * 1.5, 168 + j * 1.5, 112 + j * 1.5)
            elif y >= 32 and x < 32:
                # Sakura-blush & snow-white flower petal gradient + golden center
                d = math.hypot((x - 15.5) / 15.5, (y - 47.5) / 15.5)
                if d < 0.22:
                    stag_tex[y][x] = clamp_rgb(248, 214, 112)
                elif d < 0.58:
                    stag_tex[y][x] = clamp_rgb(252 + j, 218 + j, 226 + j)
                else:
                    stag_tex[y][x] = clamp_rgb(253 + j, 248 + j, 250 + j)
            elif 32 <= y < 48 and 32 <= x < 48:
                # Pearl-lilac blossom variant
                d = math.hypot((x - 39.5) / 7.5, (y - 39.5) / 7.5)
                if d < 0.25:
                    stag_tex[y][x] = clamp_rgb(250, 222, 126)
                else:
                    stag_tex[y][x] = clamp_rgb(242 + j, 232 + j, 252 + j)
            else:
                # Expressive dark eye + highlight
                d = math.hypot(x - 55.5, y - 55.5)
                if x in (53, 54) and y in (53, 54):
                    stag_tex[y][x] = (255, 255, 255, 255)
                elif d < 5.0:
                    stag_tex[y][x] = (32, 26, 28, 255)
                else:
                    stag_tex[y][x] = (242, 206, 202, 255)
    write_png(os.path.join(ent_dir, "blossom_stag.png"), 64, 64, stag_tex)

    # 3C) Spawn Egg item models
    im_dir = os.path.join(ASSETS, "models", "item")
    for egg_id in ("sky_dart_fish_spawn_egg", "blossom_stag_spawn_egg"):
        write_json(os.path.join(im_dir, f"{egg_id}.json"),
                   {"parent": "minecraft:item/template_spawn_egg"})

    # 3D) Overworld peaceful biome modifier (Plains, Meadow, Forest, Flower Forest, Birch, Cherry)
    write_json(os.path.join(DATA, "riftworld", "tags", "worldgen", "biome", "peaceful_normal_biomes.json"), {
        "replace": False,
        "values": [
            "minecraft:plains",
            "minecraft:sunflower_plains",
            "minecraft:meadow",
            "minecraft:forest",
            "minecraft:flower_forest",
            "minecraft:birch_forest",
            "minecraft:old_growth_birch_forest",
            "minecraft:cherry_grove"
        ]
    })
    write_json(os.path.join(DATA, "riftworld", "forge", "biome_modifier", "peaceful_fauna_overworld.json"), {
        "type": "forge:add_spawns",
        "biomes": "#riftworld:peaceful_normal_biomes",
        "spawners": [
            {"type": "riftworld:blossom_stag", "weight": 10, "minCount": 2, "maxCount": 4},
            {"type": "riftworld:sky_dart_fish", "weight": 12, "minCount": 3, "maxCount": 5}
        ]
    })


# ==============================================================================
# 4. LOCALIZATION (ru_ru.json & en_us.json)
# ==============================================================================
RU_ENTRIES = {
    "block.riftworld.ivory_marble": "Белоснежный мрамор",
    "block.riftworld.ivory_marble_bricks": "Кирпичи из белоснежного мрамора",
    "block.riftworld.ivory_marble_stairs": "Ступени из белоснежного мрамора",
    "block.riftworld.ivory_marble_slab": "Плита из белоснежного мрамора",
    "block.riftworld.ivory_marble_wall": "Ограда из белоснежного мрамора",
    "block.riftworld.ivory_marble_pillar": "Колонна из белоснежного мрамора",
    "block.riftworld.chiseled_ivory_marble": "Резной белоснежный мрамор",
    "block.riftworld.ivory_marble_tiles": "Плитка из белоснежного мрамора",
    "block.riftworld.gilded_ivory_marble": "Златотканый белоснежный мрамор",
    "block.riftworld.ivory_paving_stone": "Белая брусчатка Элисара",
    "block.riftworld.celadon_tiles": "Селадоновая черепица",
    "block.riftworld.celadon_tiles_stairs": "Ступени из селадоновой черепицы",
    "block.riftworld.celadon_tiles_slab": "Плита из селадоновой черепицы",
    "block.riftworld.pearl_calcite": "Жемчужный кальцит",
    "block.riftworld.pearl_calcite_bricks": "Кирпичи из жемчужного кальцита",
    "block.riftworld.pearl_calcite_stairs": "Ступени из жемчужного кальцита",
    "block.riftworld.pearl_calcite_slab": "Плита из жемчужного кальцита",
    "block.riftworld.pearl_calcite_wall": "Ограда из жемчужного кальцита",
    "block.riftworld.blossom_moss_block": "Цветущий мох Элисара",
    "block.riftworld.white_lily_cluster": "Белоснежные лилии",
    "block.riftworld.celadon_fern": "Селадоновый папоротник",
    "block.riftworld.pearl_prism_lantern": "Жемчужный призменный фонарь",
    "block.riftworld.obsidian_bricks": "Обсидианово-базальтовые кирпичи",
    "block.riftworld.obsidian_bricks_stairs": "Ступени из обсидиановых кирпичей",
    "block.riftworld.obsidian_bricks_slab": "Плита из обсидиановых кирпичей",
    "block.riftworld.obsidian_bricks_wall": "Ограда из обсидиановых кирпичей",
    "block.riftworld.carved_basalt_runes": "Базальт с тлеющими рунами",
    "block.riftworld.ember_brazier_block": "Кованая жаровня Черноводья",
    "block.riftworld.ironbound_dark_planks": "Окованные тёмные доски",
    "block.riftworld.ashen_cobblestone": "Пепельный булыжник",
    "entity.riftworld.sky_dart_fish": "Крылатая рыбка-стрекоза",
    "entity.riftworld.blossom_stag": "Белоснежный цветущий олень",
    "item.riftworld.sky_dart_fish_spawn_egg": "Яйцо призыва крылатой рыбки-стрекозы",
    "item.riftworld.blossom_stag_spawn_egg": "Яйцо призыва белоснежного цветущего оленя",
}

EN_ENTRIES = {
    "block.riftworld.ivory_marble": "Ivory Marble",
    "block.riftworld.ivory_marble_bricks": "Ivory Marble Bricks",
    "block.riftworld.ivory_marble_stairs": "Ivory Marble Stairs",
    "block.riftworld.ivory_marble_slab": "Ivory Marble Slab",
    "block.riftworld.ivory_marble_wall": "Ivory Marble Wall",
    "block.riftworld.ivory_marble_pillar": "Ivory Marble Pillar",
    "block.riftworld.chiseled_ivory_marble": "Chiseled Ivory Marble",
    "block.riftworld.ivory_marble_tiles": "Ivory Marble Tiles",
    "block.riftworld.gilded_ivory_marble": "Gilded Ivory Marble",
    "block.riftworld.ivory_paving_stone": "Ivory Paving Stone",
    "block.riftworld.celadon_tiles": "Celadon Roof Tiles",
    "block.riftworld.celadon_tiles_stairs": "Celadon Tile Stairs",
    "block.riftworld.celadon_tiles_slab": "Celadon Tile Slab",
    "block.riftworld.pearl_calcite": "Pearl Calcite",
    "block.riftworld.pearl_calcite_bricks": "Pearl Calcite Bricks",
    "block.riftworld.pearl_calcite_stairs": "Pearl Calcite Stairs",
    "block.riftworld.pearl_calcite_slab": "Pearl Calcite Slab",
    "block.riftworld.pearl_calcite_wall": "Pearl Calcite Wall",
    "block.riftworld.blossom_moss_block": "Blossom Moss Block",
    "block.riftworld.white_lily_cluster": "White Lily Cluster",
    "block.riftworld.celadon_fern": "Celadon Fern",
    "block.riftworld.pearl_prism_lantern": "Pearl Prism Lantern",
    "block.riftworld.obsidian_bricks": "Obsidian Basalt Bricks",
    "block.riftworld.obsidian_bricks_stairs": "Obsidian Brick Stairs",
    "block.riftworld.obsidian_bricks_slab": "Obsidian Brick Slab",
    "block.riftworld.obsidian_bricks_wall": "Obsidian Brick Wall",
    "block.riftworld.carved_basalt_runes": "Carved Basalt Runes",
    "block.riftworld.ember_brazier_block": "Blacktide Ember Brazier",
    "block.riftworld.ironbound_dark_planks": "Ironbound Dark Planks",
    "block.riftworld.ashen_cobblestone": "Ashen Cobblestone",
    "entity.riftworld.sky_dart_fish": "Sky-Dart Dragonfly Fish",
    "entity.riftworld.blossom_stag": "Snow-White Blossom Stag",
    "item.riftworld.sky_dart_fish_spawn_egg": "Sky-Dart Dragonfly Fish Spawn Egg",
    "item.riftworld.blossom_stag_spawn_egg": "Snow-White Blossom Stag Spawn Egg",
}


def update_lang(fname: str, entries: dict[str, str]):
    path = os.path.join(ASSETS, "lang", fname)
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    data.update(entries)
    write_json(path, data)


def main():
    print("Generating v0.27.0 30 block textures, models, recipes, tags & 2 friendly mob assets...")
    generate_block_textures()
    generate_block_jsons()
    generate_entity_textures_and_eggs()
    update_lang("ru_ru.json", RU_ENTRIES)
    update_lang("en_us.json", EN_ENTRIES)
    print("Done! Generated 30 blocks + 2 entity textures + spawn eggs + recipes + tags.")


if __name__ == "__main__":
    main()
