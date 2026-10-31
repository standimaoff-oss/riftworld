#!/usr/bin/env python3
"""Генератор воздушных текстур цветов Фиолетовых холмов и плюща Висячих садов (без внешних зависимостей)."""
import json
import os
import random
import struct
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "../.."))
ASSETS = os.path.join(ROOT, "mod/src/main/resources/assets/riftworld")
DATA = os.path.join(ROOT, "mod/src/main/resources/data/riftworld")
TEX_BLOCK = os.path.join(ASSETS, "textures/block")
MOD_BLOCK = os.path.join(ASSETS, "models/block")
BS_DIR = os.path.join(ASSETS, "blockstates")


def save_png(path, px):
    w, h = 16, 16
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        for x in range(w):
            r, g, b, a = px[y][x]
            raw.extend((max(0, min(255, int(r))),
                        max(0, min(255, int(g))),
                        max(0, min(255, int(b))),
                        max(0, min(255, int(a)))))

    def chunk(tag, body):
        return struct.pack(">I4s", len(body), tag) + body + struct.pack(">I", zlib.crc32(tag + body) & 0xFFFFFFFF)

    ihdr = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)


def empty():
    return [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]


def put(px, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        px[y][x] = c


STEM = (78, 124, 66, 255)
STEM_D = (58, 98, 50, 255)
LEAF = (96, 144, 78, 255)


def make_lavender_bed():
    """Три тонких ажурных колоска лаванды на стройных зелёных стеблях (~27% заполнения вместо 70%)."""
    px = empty()
    l_dark = (112, 74, 170, 255)
    l_mid = (142, 102, 202, 255)
    l_hi = (172, 136, 226, 255)
    l_tip = (202, 176, 242, 255)

    # (x стебля, верх колоска, низ колоска)
    spikes = [(3, 4, 10), (8, 2, 9), (13, 5, 11)]
    for idx, (sx, y_top, y_bot) in enumerate(spikes):
        for y in range(y_bot - 1, 16):
            put(px, sx, y, STEM if (y + idx) % 2 == 0 else STEM_D)
        # прикорневые узкие листья
        put(px, sx - 1, 14, LEAF)
        put(px, sx + 1, 13, STEM_D)
        if idx == 1:
            put(px, sx - 1, 12, STEM)
            put(px, sx + 1, 15, LEAF)
        # воздушный колосок: 1px ось + чередующиеся боковые бутоны через ряд
        put(px, sx, y_top, l_tip)
        for y in range(y_top + 1, y_bot + 1):
            put(px, sx, y, l_hi if (y + idx) % 2 == 0 else l_mid)
            if (y - y_top) % 2 == 1:
                side = -1 if ((y + idx) // 2) % 2 == 0 else 1
                put(px, sx + side, y, l_mid if y < y_bot - 1 else l_dark)
            elif y in (y_top + 2, y_top + 4) and idx == 1:
                put(px, sx - 1, y, l_hi)
                put(px, sx + 1, y, l_dark)
    return px


def make_violet_bloom():
    """Два изящных стебля лесного колокольчика с небольшими фиолетовыми чашечками (~22% заполнения)."""
    px = empty()
    v_dark = (98, 60, 162, 255)
    v_mid = (130, 88, 194, 255)
    v_hi = (164, 126, 222, 255)
    v_core = (224, 206, 248, 255)

    # Левый стебель (x=5)
    for y in range(7, 16):
        put(px, 5 - (1 if y >= 13 else 0), y, STEM if y % 2 == 0 else STEM_D)
    put(px, 3, 14, LEAF)
    put(px, 5, 13, LEAF)
    # Цветки на левом стебле
    for bx, by in [(4, 5), (6, 8)]:
        put(px, bx, by, v_core)
        put(px, bx - 1, by, v_hi)
        put(px, bx + 1, by, v_mid)
        put(px, bx, by - 1, v_hi)
        put(px, bx, by + 1, v_dark)

    # Правый стебель (x=11)
    for y in range(5, 16):
        put(px, 11 + (1 if y >= 14 else 0), y, STEM if y % 2 == 1 else STEM_D)
    put(px, 10, 13, LEAF)
    put(px, 13, 14, STEM_D)
    for bx, by in [(11, 3), (10, 7), (12, 10)]:
        put(px, bx, by, v_core if by == 3 else v_hi)
        put(px, bx - 1, by, v_mid)
        put(px, bx + 1, by, v_dark)
        put(px, bx, by - 1, v_hi)
    return px


def make_lilac_bloom():
    """Ажурная разветвлённая кисть сиреневого лугового цветка с просветами (~23% заполнения)."""
    px = empty()
    c_dark = (132, 92, 184, 255)
    c_mid = (164, 124, 214, 255)
    c_hi = (192, 158, 234, 255)
    c_tip = (218, 194, 246, 255)

    # Центральный стебель с развилкой
    for y in range(10, 16):
        put(px, 8, y, STEM if y % 2 == 0 else STEM_D)
    put(px, 7, 14, LEAF)
    put(px, 9, 13, LEAF)
    put(px, 7, 10, STEM)
    put(px, 6, 9, STEM_D)
    put(px, 9, 10, STEM)
    put(px, 10, 9, STEM_D)

    # Левая, центральная и правая воздушные кисти (связные 3-4px соцветия с просветами между ними)
    florets = [
        (4, 5, c_tip), (3, 6, c_hi), (4, 6, c_mid), (5, 6, c_hi), (4, 7, c_dark), (5, 8, STEM_D),
        (8, 2, c_tip), (7, 3, c_hi), (8, 3, c_mid), (9, 3, c_hi), (8, 4, c_dark),
        (8, 5, STEM), (8, 6, STEM_D), (8, 7, STEM), (8, 8, STEM_D), (8, 9, STEM),
        (12, 4, c_tip), (11, 5, c_hi), (12, 5, c_mid), (13, 5, c_dark), (12, 6, c_hi), (11, 7, c_dark), (11, 8, STEM),
    ]
    for fx, fy, col in florets:
        put(px, fx, fy, col)
    return px


def make_pale_bloom():
    """Нежные бледно-лавандовые звёздочки на тонких травинках (~19% заполнения)."""
    px = empty()
    p_edge = (182, 164, 224, 255)
    p_pet = (210, 198, 242, 255)
    p_hi = (232, 224, 252, 255)
    p_center = (250, 236, 178, 255)

    stems = [(4, 8), (8, 5), (12, 9)]
    for idx, (sx, top_y) in enumerate(stems):
        for y in range(top_y + 1, 16):
            put(px, sx, y, STEM if (y + idx) % 2 == 0 else STEM_D)
        put(px, sx + (-1 if idx == 2 else 1), 14 - (idx % 2), LEAF)
        # 5-пиксельный цветок-звёздочка
        put(px, sx, top_y, p_center)
        put(px, sx, top_y - 1, p_hi)
        put(px, sx, top_y + 1, p_edge)
        put(px, sx - 1, top_y, p_pet)
        put(px, sx + 1, top_y, p_hi if idx == 1 else p_pet)
    return px


def make_garden_ivy():
    """Сочно-зелёные вертикальные плети плюща с резными листочками для обрывов Висячих садов."""
    px = empty()
    rnd = random.Random(714)
    stem = (58, 102, 40, 255)
    leaf_d = (68, 118, 46, 255)
    leaf_m = (88, 146, 58, 255)
    leaf_l = (114, 174, 74, 255)

    runners = [2, 7, 12]
    for idx, rx in enumerate(runners):
        x = rx
        max_y = 15 if idx != 2 else 14
        for y in range(0, max_y + 1):
            if y > 0 and y % 3 == 0:
                x = max(1, min(14, rx + ((y // 3 + idx) % 3 - 1)))
            put(px, x, y, stem)
            if (y + idx) % 2 == 0:
                side = -1 if ((y + idx) // 2) % 2 == 0 else 1
                put(px, x + side, y, leaf_l if y % 4 == 0 else leaf_m)
                if y + 1 <= max_y:
                    put(px, x + side, y + 1, leaf_d)
            if rnd.random() < 0.35:
                put(px, x, y, leaf_m)
    return px


def write_cross_model(name):
    model = {
        "render_type": "minecraft:cutout",
        "ambientocclusion": False,
        "textures": {
            "particle": f"riftworld:block/{name}",
            "cross": f"riftworld:block/{name}",
        },
        "elements": [
            {
                "from": [0.8, 0.0, 8.0],
                "to": [15.2, 16.0, 8.0],
                "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True},
                "shade": False,
                "faces": {
                    "north": {"uv": [0, 0, 16, 16], "texture": "#cross"},
                    "south": {"uv": [0, 0, 16, 16], "texture": "#cross"},
                },
            },
            {
                "from": [8.0, 0.0, 0.8],
                "to": [8.0, 16.0, 15.2],
                "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True},
                "shade": False,
                "faces": {
                    "west": {"uv": [0, 0, 16, 16], "texture": "#cross"},
                    "east": {"uv": [0, 0, 16, 16], "texture": "#cross"},
                },
            },
        ],
    }
    with open(os.path.join(MOD_BLOCK, f"{name}.json"), "w", encoding="utf-8") as f:
        json.dump(model, f, indent=2)
        f.write("\n")


def write_ivy_assets(name):
    """Явные неповёрнутые модели граней лозы (1:1 с AABB ванильного VineBlock, без x/y-вращений в blockstate)."""
    tex = f"riftworld:block/{name}"
    faces_spec = {
        # VineBlock.NORTH_AABB = [0, 0, 0]..[16, 16, 1]
        "north": ([0.0, 0.0, 0.8], [16.0, 16.0, 0.8], {
            "north": {"uv": [16, 0, 0, 16], "texture": "#vine"},
            "south": {"uv": [0, 0, 16, 16], "texture": "#vine"},
        }),
        # VineBlock.SOUTH_AABB = [0, 0, 15]..[16, 16, 16]
        "south": ([0.0, 0.0, 15.2], [16.0, 16.0, 15.2], {
            "north": {"uv": [0, 0, 16, 16], "texture": "#vine"},
            "south": {"uv": [16, 0, 0, 16], "texture": "#vine"},
        }),
        # VineBlock.WEST_AABB = [0, 0, 0]..[1, 16, 16]
        "west": ([0.8, 0.0, 0.0], [0.8, 16.0, 16.0], {
            "west": {"uv": [16, 0, 0, 16], "texture": "#vine"},
            "east": {"uv": [0, 0, 16, 16], "texture": "#vine"},
        }),
        # VineBlock.EAST_AABB = [15, 0, 0]..[16, 16, 16]
        "east": ([15.2, 0.0, 0.0], [15.2, 16.0, 16.0], {
            "west": {"uv": [0, 0, 16, 16], "texture": "#vine"},
            "east": {"uv": [16, 0, 0, 16], "texture": "#vine"},
        }),
        # VineBlock.UP_AABB = [0, 15, 0]..[16, 16, 16]
        "up": ([0.0, 15.2, 0.0], [16.0, 15.2, 16.0], {
            "up": {"uv": [0, 0, 16, 16], "texture": "#vine"},
            "down": {"uv": [0, 16, 16, 0], "texture": "#vine"},
        }),
    }
    for suffix, (frm, to, fcs) in faces_spec.items():
        m = {
            "ambientocclusion": False,
            "render_type": "minecraft:cutout",
            "textures": {"particle": tex, "vine": tex},
            "elements": [{"from": frm, "to": to, "shade": False, "faces": fcs}],
        }
        out_name = f"{name}_{suffix}.json"
        with open(os.path.join(MOD_BLOCK, out_name), "w", encoding="utf-8") as f:
            json.dump(m, f, indent=2)
            f.write("\n")

    # Оставляем и базовую модель <name>.json (= south) на случай внешних ссылок
    with open(os.path.join(MOD_BLOCK, f"{name}_south.json"), "r", encoding="utf-8") as src:
        base_m = json.load(src)
    with open(os.path.join(MOD_BLOCK, f"{name}.json"), "w", encoding="utf-8") as f:
        json.dump(base_m, f, indent=2)
        f.write("\n")

    bs = {
        "multipart": [
            {"when": {"north": "true"}, "apply": {"model": f"riftworld:block/{name}_north"}},
            {"when": {"south": "true"}, "apply": {"model": f"riftworld:block/{name}_south"}},
            {"when": {"west": "true"}, "apply": {"model": f"riftworld:block/{name}_west"}},
            {"when": {"east": "true"}, "apply": {"model": f"riftworld:block/{name}_east"}},
            {"when": {"up": "true"}, "apply": {"model": f"riftworld:block/{name}_up"}},
            {
                "when": {"north": "false", "south": "false", "west": "false", "east": "false", "up": "false"},
                "apply": {"model": f"riftworld:block/{name}_south"},
            },
        ]
    }
    with open(os.path.join(BS_DIR, f"{name}.json"), "w", encoding="utf-8") as f:
        json.dump(bs, f, indent=2)
        f.write("\n")


def main():
    flowers = {
        "lavender_bed": make_lavender_bed(),
        "violet_bloom": make_violet_bloom(),
        "lilac_bloom": make_lilac_bloom(),
        "pale_bloom": make_pale_bloom(),
    }
    for name, px in flowers.items():
        save_png(os.path.join(TEX_BLOCK, f"{name}.png"), px)
        write_cross_model(name)
        op = sum(1 for y in range(16) for x in range(16) if px[y][x][3] > 0)
        print(f"{name}: {op}/256 ({op / 2.56:.1f}% opaque)")

    ivy_px = make_garden_ivy()
    save_png(os.path.join(TEX_BLOCK, "garden_ivy.png"), ivy_px)
    op_ivy = sum(1 for y in range(16) for x in range(16) if ivy_px[y][x][3] > 0)
    print(f"garden_ivy: {op_ivy}/256 ({op_ivy / 2.56:.1f}% opaque)")

    for vine_name in ("garden_ivy", "elisar_ivy", "dead_vines"):
        write_ivy_assets(vine_name)

    loot_path = os.path.join(DATA, "loot_tables/blocks/garden_ivy.json")
    os.makedirs(os.path.dirname(loot_path), exist_ok=True)
    loot = {
        "type": "minecraft:block",
        "pools": [
            {
                "rolls": 1,
                "entries": [{"type": "minecraft:item", "name": "riftworld:garden_ivy"}],
                "conditions": [{"condition": "minecraft:match_tool", "predicate": {"items": ["minecraft:shears"]}}],
            }
        ],
    }
    with open(loot_path, "w", encoding="utf-8") as f:
        json.dump(loot, f, indent=2)
        f.write("\n")


if __name__ == "__main__":
    main()
