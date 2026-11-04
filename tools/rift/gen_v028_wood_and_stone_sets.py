#!/usr/bin/env python3
"""
v0.28.0: Генератор полных ванильных наборов древесины для всех 7 древесных пород мода
(+75 блоков) и недостающих каменных ступеней/полублоков/оград (+15 блоков = 90 новых блоков).
Использует строго ванильные шаблоны JSON-моделей и перекрашенные 16x16 пиксельные текстуры!
"""
import json
import pathlib
import struct
import zlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
ASSETS = ROOT / "mod/src/main/resources/assets/riftworld"
DATA = ROOT / "mod/src/main/resources/data"

TEX_BLOCK = ASSETS / "textures/block"
TEX_ITEM = ASSETS / "textures/item"
BS_DIR = ASSETS / "blockstates"
M_BLOCK = ASSETS / "models/block"
M_ITEM = ASSETS / "models/item"
LANG_RU = ASSETS / "lang/ru_ru.json"
LANG_EN = ASSETS / "lang/en_us.json"
LOOT_DIR = DATA / "riftworld/loot_tables/blocks"
RECIPE_DIR = DATA / "riftworld/recipes"


# ---------------------------------------------------------------------------
# Pure-Python PNG reader / writer (RGBA 8-bit)
# ---------------------------------------------------------------------------
def read_png(path: pathlib.Path):
    raw_bytes = path.read_bytes()
    pos = 8
    w = h = ctype = 0
    idat = bytearray()
    while pos < len(raw_bytes):
        ln, typ = struct.unpack(">I4s", raw_bytes[pos:pos + 8])
        chunk = raw_bytes[pos + 8:pos + 8 + ln]
        pos += 12 + ln
        if typ == b"IHDR":
            w, h, _bd, ctype, *_ = struct.unpack(">IIBBBBB", chunk)
        elif typ == b"IDAT":
            idat.extend(chunk)
    decomp = zlib.decompress(bytes(idat))
    bpp = 4 if ctype == 6 else 3
    stride = 1 + w * bpp
    prev = bytearray(w * bpp)
    pixels = []
    for y in range(h):
        flt = decomp[y * stride]
        row = bytearray(decomp[y * stride + 1:(y + 1) * stride])
        for i in range(len(row)):
            a = row[i - bpp] if i >= bpp else 0
            b = prev[i]
            c = prev[i - bpp] if i >= bpp else 0
            if flt == 1:
                row[i] = (row[i] + a) & 255
            elif flt == 2:
                row[i] = (row[i] + b) & 255
            elif flt == 3:
                row[i] = (row[i] + ((a + b) >> 1)) & 255
            elif flt == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                row[i] = (row[i] + pr) & 255
        prev = row
        r_px = []
        for x in range(w):
            r = row[x * bpp]
            g = row[x * bpp + 1]
            b_ = row[x * bpp + 2]
            al = row[x * bpp + 3] if bpp == 4 else 255
            r_px.append((r, g, b_, al))
        pixels.append(r_px)
    return w, h, pixels


def write_png(path: pathlib.Path, pixels):
    path.parent.mkdir(parents=True, exist_ok=True)
    h = len(pixels)
    w = len(pixels[0])
    raw = bytearray()
    for row in pixels:
        raw.append(0)
        for r, g, b, a in row:
            raw.extend((max(0, min(255, int(r))),
                        max(0, min(255, int(g))),
                        max(0, min(255, int(b))),
                        max(0, min(255, int(a)))))

    def chunk(tag: bytes, payload: bytes) -> bytes:
        return (struct.pack(">I", len(payload)) + tag + payload +
                struct.pack(">I", zlib.crc32(tag + payload) & 0xFFFFFFFF))

    ihdr = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)
    idat = zlib.compress(bytes(raw), 9)
    out = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", idat) + chunk(b"IEND", b"")
    path.write_bytes(out)


def recolor_template(pixels, target_rgb, contrast=1.08, hinge_metal=False):
    """Перекрашивает шаблон с сохранением живой древесной фактуры, волокон и фурнитуры."""
    lum_vals = []
    for row in pixels:
        for r, g, b, a in row:
            if a > 32:
                lum_vals.append(0.299 * r + 0.587 * g + 0.114 * b)
    mean_lum = sum(lum_vals) / max(1, len(lum_vals))
    tr, tg, tb = target_rgb
    out = []
    for row in pixels:
        orow = []
        for r, g, b, a in row:
            if a == 0:
                orow.append((0, 0, 0, 0))
                continue
            lum = 0.299 * r + 0.587 * g + 0.114 * b
            # Сохраняем металлические петли/ручки двери серыми, если это нейтрально-тёмный металл
            sat = max(r, g, b) - min(r, g, b)
            if hinge_metal and sat <= 8 and lum < 96:
                orow.append((r, g, b, a))
                continue
            rel = ((lum - mean_lum) * contrast) / max(32.0, mean_lum)
            nr = tr * (1.0 + rel * 0.72) + (lum - mean_lum) * 0.28
            ng = tg * (1.0 + rel * 0.72) + (lum - mean_lum) * 0.28
            nb = tb * (1.0 + rel * 0.72) + (lum - mean_lum) * 0.28
            orow.append((int(round(nr)), int(round(ng)), int(round(nb)), a))
        out.append(orow)
    return out


def make_stripped_side(rgb):
    """Классическая ванильная текстура обтёсанного бревна (гладкие вертикальные волокна + мягкое затенение краёв)."""
    tr, tg, tb = rgb
    px = []
    for y in range(16):
        row = []
        for x in range(16):
            edge = -0.10 if x in (0, 15) else (-0.05 if x in (1, 14) else 0.0)
            grain = (((x * 37 + y * 13) ^ (x * 7)) % 11 - 5) * 0.014
            streak = 0.035 if x in (4, 5, 10, 11) else (-0.025 if x in (2, 8, 13) else 0.0)
            wave = (((y + x * 3) % 8) - 3.5) * 0.008
            k = 1.0 + edge + grain + streak + wave
            row.append((int(tr * k), int(tg * k), int(tb * k), 255))
        px.append(row)
    return px


def make_stripped_top(side_rgb, ring_rgb):
    """Классический торец обтёсанного бревна с годичными кольцами в стиле Minecraft."""
    sr, sg, sb = side_rgb
    rr, rg, rb = ring_rgb
    px = []
    for y in range(16):
        row = []
        for x in range(16):
            dist = max(abs(x - 7.5), abs(y - 7.5))
            if dist >= 6.5:
                k = 0.94 if dist >= 7.2 else 1.02
                row.append((int(sr * k), int(sg * k), int(sb * k), 255))
            else:
                ring = 0.92 if int(round(dist)) in (2, 4, 6) else 1.05
                n = (((x * 19 + y * 31) % 7) - 3) * 0.012
                k = ring + n
                row.append((int(rr * k), int(rg * k), int(rb * k), 255))
        px.append(row)
    return px


def make_vanilla_trapdoor(rgb):
    """Классический ванильный деревянный люк 16x16 с рамой, перемычкой и 4 сквозными окошками (alpha=0) + железными петлями."""
    tr, tg, tb = rgb
    px = []
    windows = {
        (x, y)
        for bx in (3, 9)
        for by in (3, 9)
        for x in range(bx, bx + 4)
        for y in range(by, by + 4)
    }
    for y in range(16):
        row = []
        for x in range(16):
            if (x, y) in windows:
                row.append((0, 0, 0, 0))
                continue
            # Металлические петли/ручка как у ванильного дубового люка
            if (y in (0, 1) and x in (3, 4, 11, 12)) or (y in (14, 15) and x in (7, 8)):
                m = 82 if (x + y) % 2 == 0 else 104
                row.append((m, m, m + 4, 255))
                continue
            border = (x in (0, 15) or y in (0, 15) or
                      ((x in (2, 7, 8, 13)) and 2 <= y <= 13) or
                      ((y in (2, 7, 8, 13)) and 2 <= x <= 13))
            grain = (((x * 23 + y * 7) % 9) - 4) * 0.016
            k = (0.86 if border else 1.03) + grain
            row.append((int(tr * k), int(tg * k), int(tb * k), 255))
        px.append(row)
    return px


def write_json(path: pathlib.Path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


# ---------------------------------------------------------------------------
# Blockstate + Block Model + Item Model + Loot Generators (100% Vanilla JSON)
# ---------------------------------------------------------------------------
def gen_cube(block_id: str, tex: str):
    write_json(BS_DIR / f"{block_id}.json", {
        "variants": {"": {"model": f"riftworld:block/{block_id}"}}
    })
    write_json(M_BLOCK / f"{block_id}.json", {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": f"riftworld:block/{tex}"}
    })
    write_json(M_ITEM / f"{block_id}.json", {"parent": f"riftworld:block/{block_id}"})
    gen_loot_self(block_id)


def gen_pillar(block_id: str, side_tex: str, end_tex: str):
    write_json(BS_DIR / f"{block_id}.json", {
        "variants": {
            "axis=x": {"model": f"riftworld:block/{block_id}_horizontal", "x": 90, "y": 90},
            "axis=y": {"model": f"riftworld:block/{block_id}"},
            "axis=z": {"model": f"riftworld:block/{block_id}_horizontal", "x": 90}
        }
    })
    write_json(M_BLOCK / f"{block_id}.json", {
        "parent": "minecraft:block/cube_column",
        "textures": {"end": f"riftworld:block/{end_tex}", "side": f"riftworld:block/{side_tex}"}
    })
    write_json(M_BLOCK / f"{block_id}_horizontal.json", {
        "parent": "minecraft:block/cube_column_horizontal",
        "textures": {"end": f"riftworld:block/{end_tex}", "side": f"riftworld:block/{side_tex}"}
    })
    write_json(M_ITEM / f"{block_id}.json", {"parent": f"riftworld:block/{block_id}"})
    gen_loot_self(block_id)


def gen_stairs(block_id: str, tex: str):
    t = f"riftworld:block/{tex}"
    write_json(M_BLOCK / f"{block_id}.json", {
        "parent": "minecraft:block/stairs",
        "textures": {"bottom": t, "top": t, "side": t}
    })
    write_json(M_BLOCK / f"{block_id}_inner.json", {
        "parent": "minecraft:block/inner_stairs",
        "textures": {"bottom": t, "top": t, "side": t}
    })
    write_json(M_BLOCK / f"{block_id}_outer.json", {
        "parent": "minecraft:block/outer_stairs",
        "textures": {"bottom": t, "top": t, "side": t}
    })
    # Exact vanilla 40-variant stairs blockstate copied structurally from existing stairs
    ref_bs = json.loads((BS_DIR / "sizo_planks_stairs.json").read_text(encoding="utf-8"))
    new_bs = json.loads(json.dumps(ref_bs).replace("sizo_planks_stairs", block_id))
    write_json(BS_DIR / f"{block_id}.json", new_bs)
    write_json(M_ITEM / f"{block_id}.json", {"parent": f"riftworld:block/{block_id}"})
    gen_loot_self(block_id)


def gen_slab(block_id: str, full_block_model: str, tex: str):
    t = f"riftworld:block/{tex}"
    write_json(BS_DIR / f"{block_id}.json", {
        "variants": {
            "type=bottom": {"model": f"riftworld:block/{block_id}"},
            "type=double": {"model": f"riftworld:block/{full_block_model}"},
            "type=top": {"model": f"riftworld:block/{block_id}_top"}
        }
    })
    write_json(M_BLOCK / f"{block_id}.json", {
        "parent": "minecraft:block/slab",
        "textures": {"bottom": t, "top": t, "side": t}
    })
    write_json(M_BLOCK / f"{block_id}_top.json", {
        "parent": "minecraft:block/slab_top",
        "textures": {"bottom": t, "top": t, "side": t}
    })
    write_json(M_ITEM / f"{block_id}.json", {"parent": f"riftworld:block/{block_id}"})
    write_json(LOOT_DIR / f"{block_id}.json", {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1.0,
            "bonus_rolls": 0.0,
            "entries": [{
                "type": "minecraft:item",
                "name": f"riftworld:{block_id}",
                "functions": [
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
            }]
        }]
    })


def gen_fence(block_id: str, tex: str):
    t = f"riftworld:block/{tex}"
    write_json(BS_DIR / f"{block_id}.json", {
        "multipart": [
            {"apply": {"model": f"riftworld:block/{block_id}_post"}},
            {"when": {"north": "true"}, "apply": {"model": f"riftworld:block/{block_id}_side", "uvlock": True}},
            {"when": {"east": "true"}, "apply": {"model": f"riftworld:block/{block_id}_side", "y": 90, "uvlock": True}},
            {"when": {"south": "true"}, "apply": {"model": f"riftworld:block/{block_id}_side", "y": 180, "uvlock": True}},
            {"when": {"west": "true"}, "apply": {"model": f"riftworld:block/{block_id}_side", "y": 270, "uvlock": True}}
        ]
    })
    write_json(M_BLOCK / f"{block_id}_post.json", {"parent": "minecraft:block/fence_post", "textures": {"texture": t}})
    write_json(M_BLOCK / f"{block_id}_side.json", {"parent": "minecraft:block/fence_side", "textures": {"texture": t}})
    write_json(M_BLOCK / f"{block_id}_inventory.json", {"parent": "minecraft:block/fence_inventory", "textures": {"texture": t}})
    write_json(M_ITEM / f"{block_id}.json", {"parent": f"riftworld:block/{block_id}_inventory"})
    gen_loot_self(block_id)


def gen_fence_gate(block_id: str, tex: str):
    t = f"riftworld:block/{tex}"
    variants = {}
    rot_map = {"south": 0, "west": 90, "north": 180, "east": 270}
    for facing, y in rot_map.items():
        for in_wall in ("false", "true"):
            for open_s in ("false", "true"):
                suffix = ("_wall" if in_wall == "true" else "") + ("_open" if open_s == "true" else "")
                entry = {"model": f"riftworld:block/{block_id}{suffix}", "uvlock": True}
                if y:
                    entry["y"] = y
                variants[f"facing={facing},in_wall={in_wall},open={open_s}"] = entry
    write_json(BS_DIR / f"{block_id}.json", {"variants": variants})
    write_json(M_BLOCK / f"{block_id}.json", {"parent": "minecraft:block/template_fence_gate", "textures": {"texture": t}})
    write_json(M_BLOCK / f"{block_id}_open.json", {"parent": "minecraft:block/template_fence_gate_open", "textures": {"texture": t}})
    write_json(M_BLOCK / f"{block_id}_wall.json", {"parent": "minecraft:block/template_fence_gate_wall", "textures": {"texture": t}})
    write_json(M_BLOCK / f"{block_id}_wall_open.json", {"parent": "minecraft:block/template_fence_gate_wall_open", "textures": {"texture": t}})
    write_json(M_ITEM / f"{block_id}.json", {"parent": f"riftworld:block/{block_id}"})
    gen_loot_self(block_id)


def gen_door(block_id: str):
    ref_bs = json.loads((BS_DIR / "elisar_door.json").read_text(encoding="utf-8"))
    new_bs = json.loads(json.dumps(ref_bs).replace("elisar_door", block_id))
    write_json(BS_DIR / f"{block_id}.json", new_bs)
    for sub in (
        "bottom_left", "bottom_left_open", "bottom_right", "bottom_right_open",
        "top_left", "top_left_open", "top_right", "top_right_open"
    ):
        write_json(M_BLOCK / f"{block_id}_{sub}.json", {
            "parent": f"minecraft:block/door_{sub}",
            "render_type": "minecraft:cutout",
            "textures": {
                "bottom": f"riftworld:block/{block_id}_bottom",
                "top": f"riftworld:block/{block_id}_top"
            }
        })
    write_json(M_ITEM / f"{block_id}.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"riftworld:item/{block_id}"}
    })
    write_json(LOOT_DIR / f"{block_id}.json", {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1.0,
            "bonus_rolls": 0.0,
            "conditions": [{"condition": "minecraft:survives_explosion"}],
            "entries": [{
                "type": "minecraft:item",
                "name": f"riftworld:{block_id}",
                "conditions": [{
                    "condition": "minecraft:block_state_property",
                    "block": f"riftworld:{block_id}",
                    "properties": {"half": "lower"}
                }]
            }]
        }]
    })


def gen_trapdoor(block_id: str):
    t = f"riftworld:block/{block_id}"
    variants = {}
    rot_map = {"north": 0, "east": 90, "south": 180, "west": 270}
    for facing, y in rot_map.items():
        for half in ("bottom", "top"):
            for open_s in ("false", "true"):
                sub = "open" if open_s == "true" else half
                entry = {"model": f"riftworld:block/{block_id}_{sub}"}
                if y:
                    entry["y"] = y
                variants[f"facing={facing},half={half},open={open_s}"] = entry
    write_json(BS_DIR / f"{block_id}.json", {"variants": variants})
    for sub in ("bottom", "top", "open"):
        write_json(M_BLOCK / f"{block_id}_{sub}.json", {
            "parent": f"minecraft:block/template_orientable_trapdoor_{sub}",
            "render_type": "minecraft:cutout",
            "textures": {"texture": t}
        })
    write_json(M_ITEM / f"{block_id}.json", {"parent": f"riftworld:block/{block_id}_bottom"})
    gen_loot_self(block_id)


def gen_pressure_plate(block_id: str, tex: str):
    t = f"riftworld:block/{tex}"
    write_json(BS_DIR / f"{block_id}.json", {
        "variants": {
            "powered=false": {"model": f"riftworld:block/{block_id}"},
            "powered=true": {"model": f"riftworld:block/{block_id}_down"}
        }
    })
    write_json(M_BLOCK / f"{block_id}.json", {"parent": "minecraft:block/pressure_plate_up", "textures": {"texture": t}})
    write_json(M_BLOCK / f"{block_id}_down.json", {"parent": "minecraft:block/pressure_plate_down", "textures": {"texture": t}})
    write_json(M_ITEM / f"{block_id}.json", {"parent": f"riftworld:block/{block_id}"})
    gen_loot_self(block_id)


def gen_button(block_id: str, tex: str):
    t = f"riftworld:block/{tex}"
    variants = {}
    rot_face = {"north": 0, "east": 90, "south": 180, "west": 270}
    for face in ("floor", "wall", "ceiling"):
        for facing, y in rot_face.items():
            for powered in ("false", "true"):
                m = f"riftworld:block/{block_id}" + ("_pressed" if powered == "true" else "")
                entry = {"model": m}
                if face == "wall":
                    entry["uvlock"] = True
                    entry["x"] = 90
                    if y:
                        entry["y"] = y
                elif face == "ceiling":
                    entry["x"] = 180
                    ry = (y + 180) % 360
                    if ry:
                        entry["y"] = ry
                else:
                    if y:
                        entry["y"] = y
                variants[f"face={face},facing={facing},powered={powered}"] = entry
    write_json(BS_DIR / f"{block_id}.json", {"variants": variants})
    write_json(M_BLOCK / f"{block_id}.json", {"parent": "minecraft:block/button", "textures": {"texture": t}})
    write_json(M_BLOCK / f"{block_id}_pressed.json", {"parent": "minecraft:block/button_pressed", "textures": {"texture": t}})
    write_json(M_BLOCK / f"{block_id}_inventory.json", {"parent": "minecraft:block/button_inventory", "textures": {"texture": t}})
    write_json(M_ITEM / f"{block_id}.json", {"parent": f"riftworld:block/{block_id}_inventory"})
    gen_loot_self(block_id)


def gen_wall(block_id: str, tex: str):
    t = f"riftworld:block/{tex}"
    ref_bs = json.loads((BS_DIR / "elisar_stone_wall.json").read_text(encoding="utf-8"))
    new_bs = json.loads(json.dumps(ref_bs).replace("elisar_stone_wall", block_id))
    write_json(BS_DIR / f"{block_id}.json", new_bs)
    write_json(M_BLOCK / f"{block_id}_post.json", {"parent": "minecraft:block/template_wall_post", "textures": {"wall": t}})
    write_json(M_BLOCK / f"{block_id}_side.json", {"parent": "minecraft:block/template_wall_side", "textures": {"wall": t}})
    write_json(M_BLOCK / f"{block_id}_side_tall.json", {"parent": "minecraft:block/template_wall_side_tall", "textures": {"wall": t}})
    write_json(M_BLOCK / f"{block_id}_inventory.json", {"parent": "minecraft:block/wall_inventory", "textures": {"wall": t}})
    write_json(M_ITEM / f"{block_id}.json", {"parent": f"riftworld:block/{block_id}_inventory"})
    gen_loot_self(block_id)


def gen_loot_self(block_id: str):
    write_json(LOOT_DIR / f"{block_id}.json", {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1.0,
            "bonus_rolls": 0.0,
            "conditions": [{"condition": "minecraft:survives_explosion"}],
            "entries": [{"type": "minecraft:item", "name": f"riftworld:{block_id}"}]
        }]
    })


# ---------------------------------------------------------------------------
# Crafting & Stonecutting Recipes
# ---------------------------------------------------------------------------
def add_wood_recipes(spec):
    p = spec["planks"]
    # 1) Logs / Wood -> 4 Planks
    for src in (spec["log"], spec["stripped_log"], spec["wood"], spec["stripped_wood"]):
        r_name = f"{p}_from_{src}"
        write_json(RECIPE_DIR / f"{r_name}.json", {
            "type": "minecraft:crafting_shapeless",
            "category": "building",
            "group": "planks",
            "ingredients": [{"item": f"riftworld:{src}"}],
            "result": {"item": f"riftworld:{p}", "count": 4}
        })
    # 2) 4 Logs -> 3 Wood & 4 Stripped Logs -> 3 Stripped Wood
    for log_id, wood_id in ((spec["log"], spec["wood"]), (spec["stripped_log"], spec["stripped_wood"])):
        write_json(RECIPE_DIR / f"{wood_id}.json", {
            "type": "minecraft:crafting_shaped",
            "category": "building",
            "group": "bark",
            "pattern": ["##", "##"],
            "key": {"#": {"item": f"riftworld:{log_id}"}},
            "result": {"item": f"riftworld:{wood_id}", "count": 3}
        })
    # 3) Planks -> Stairs, Slab, Fence, Gate, Door, Trapdoor, Plate, Button
    write_json(RECIPE_DIR / f"{spec['stairs']}.json", {
        "type": "minecraft:crafting_shaped",
        "category": "building",
        "group": "wooden_stairs",
        "pattern": ["#  ", "## ", "###"],
        "key": {"#": {"item": f"riftworld:{p}"}},
        "result": {"item": f"riftworld:{spec['stairs']}", "count": 4}
    })
    write_json(RECIPE_DIR / f"{spec['slab']}.json", {
        "type": "minecraft:crafting_shaped",
        "category": "building",
        "group": "wooden_slab",
        "pattern": ["###"],
        "key": {"#": {"item": f"riftworld:{p}"}},
        "result": {"item": f"riftworld:{spec['slab']}", "count": 6}
    })
    write_json(RECIPE_DIR / f"{spec['fence']}.json", {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "group": "wooden_fence",
        "pattern": ["W#W", "W#W"],
        "key": {"W": {"item": f"riftworld:{p}"}, "#": {"item": "minecraft:stick"}},
        "result": {"item": f"riftworld:{spec['fence']}", "count": 3}
    })
    write_json(RECIPE_DIR / f"{spec['gate']}.json", {
        "type": "minecraft:crafting_shaped",
        "category": "redstone",
        "group": "wooden_fence_gate",
        "pattern": ["#W#", "#W#"],
        "key": {"W": {"item": f"riftworld:{p}"}, "#": {"item": "minecraft:stick"}},
        "result": {"item": f"riftworld:{spec['gate']}", "count": 1}
    })
    write_json(RECIPE_DIR / f"{spec['door']}.json", {
        "type": "minecraft:crafting_shaped",
        "category": "redstone",
        "group": "wooden_door",
        "pattern": ["##", "##", "##"],
        "key": {"#": {"item": f"riftworld:{p}"}},
        "result": {"item": f"riftworld:{spec['door']}", "count": 3}
    })
    write_json(RECIPE_DIR / f"{spec['trapdoor']}.json", {
        "type": "minecraft:crafting_shaped",
        "category": "redstone",
        "group": "wooden_trapdoor",
        "pattern": ["###", "###"],
        "key": {"#": {"item": f"riftworld:{p}"}},
        "result": {"item": f"riftworld:{spec['trapdoor']}", "count": 2}
    })
    write_json(RECIPE_DIR / f"{spec['plate']}.json", {
        "type": "minecraft:crafting_shaped",
        "category": "redstone",
        "group": "wooden_pressure_plate",
        "pattern": ["##"],
        "key": {"#": {"item": f"riftworld:{p}"}},
        "result": {"item": f"riftworld:{spec['plate']}", "count": 1}
    })
    write_json(RECIPE_DIR / f"{spec['button']}.json", {
        "type": "minecraft:crafting_shapeless",
        "category": "redstone",
        "group": "wooden_button",
        "ingredients": [{"item": f"riftworld:{p}"}],
        "result": {"item": f"riftworld:{spec['button']}", "count": 1}
    })


def add_stone_recipes(base_id: str, stairs_id=None, slab_id=None, wall_id=None):
    if stairs_id:
        write_json(RECIPE_DIR / f"{stairs_id}.json", {
            "type": "minecraft:crafting_shaped",
            "category": "building",
            "pattern": ["#  ", "## ", "###"],
            "key": {"#": {"item": f"riftworld:{base_id}"}},
            "result": {"item": f"riftworld:{stairs_id}", "count": 4}
        })
        write_json(RECIPE_DIR / f"{stairs_id}_from_stonecutting.json", {
            "type": "minecraft:stonecutting",
            "ingredient": {"item": f"riftworld:{base_id}"},
            "result": f"riftworld:{stairs_id}",
            "count": 1
        })
    if slab_id:
        write_json(RECIPE_DIR / f"{slab_id}.json", {
            "type": "minecraft:crafting_shaped",
            "category": "building",
            "pattern": ["###"],
            "key": {"#": {"item": f"riftworld:{base_id}"}},
            "result": {"item": f"riftworld:{slab_id}", "count": 6}
        })
        write_json(RECIPE_DIR / f"{slab_id}_from_stonecutting.json", {
            "type": "minecraft:stonecutting",
            "ingredient": {"item": f"riftworld:{base_id}"},
            "result": f"riftworld:{slab_id}",
            "count": 2
        })
    if wall_id:
        write_json(RECIPE_DIR / f"{wall_id}.json", {
            "type": "minecraft:crafting_shaped",
            "category": "misc",
            "pattern": ["###", "###"],
            "key": {"#": {"item": f"riftworld:{base_id}"}},
            "result": {"item": f"riftworld:{wall_id}", "count": 6}
        })
        write_json(RECIPE_DIR / f"{wall_id}_from_stonecutting.json", {
            "type": "minecraft:stonecutting",
            "ingredient": {"item": f"riftworld:{base_id}"},
            "result": f"riftworld:{wall_id}",
            "count": 1
        })


def append_tag(rel_path: str, entries):
    p = DATA / rel_path
    if p.exists():
        obj = json.loads(p.read_text(encoding="utf-8"))
    else:
        obj = {"replace": False, "values": []}
    vals = obj.setdefault("values", [])
    for e in entries:
        full = e if ":" in e else f"riftworld:{e}"
        if full not in vals:
            vals.append(full)
    write_json(p, obj)


def main():
    _, _, planks_tmpl = read_png(TEX_BLOCK / "sizo_planks.png")
    _, _, door_top_tmpl = read_png(TEX_BLOCK / "elisar_door_top.png")
    _, _, door_bot_tmpl = read_png(TEX_BLOCK / "elisar_door_bottom.png")
    _, _, door_item_tmpl = read_png(TEX_ITEM / "elisar_door.png")

    wood_families = [
        {
            "key": "sizo",
            "rgb": (190, 180, 159),
            "strip_rgb": (176, 165, 146),
            "ring_rgb": (167, 154, 137),
            "bark_tex": "sizo_log",
            "log": "sizo_log",
            "stripped_log": "stripped_sizo_log",
            "wood": "sizo_wood",
            "stripped_wood": "stripped_sizo_wood",
            "planks": "sizo_planks",
            "new_planks": False,
            "stairs": "sizo_planks_stairs",
            "new_stairs": False,
            "slab": "sizo_planks_slab",
            "new_slab": False,
            "fence": "sizo_fence",
            "new_fence": True,
            "gate": "sizo_fence_gate",
            "door": "sizo_door",
            "trapdoor": "sizo_trapdoor",
            "plate": "sizo_pressure_plate",
            "button": "sizo_button",
            "ru": {
                "stripped_sizo_log": "Обтёсанное бревно сизоствола",
                "sizo_wood": "Древесина сизоствола",
                "stripped_sizo_wood": "Обтёсанная древесина сизоствола",
                "sizo_fence": "Забор из сизоствола",
                "sizo_fence_gate": "Калитка из сизоствола",
                "sizo_door": "Дверь из сизоствола",
                "sizo_trapdoor": "Люк из сизоствола",
                "sizo_pressure_plate": "Нажимная плита из сизоствола",
                "sizo_button": "Кнопка из сизоствола",
            },
            "en": {
                "stripped_sizo_log": "Stripped Sizo Log",
                "sizo_wood": "Sizo Wood",
                "stripped_sizo_wood": "Stripped Sizo Wood",
                "sizo_fence": "Sizo Fence",
                "sizo_fence_gate": "Sizo Fence Gate",
                "sizo_door": "Sizo Door",
                "sizo_trapdoor": "Sizo Trapdoor",
                "sizo_pressure_plate": "Sizo Pressure Plate",
                "sizo_button": "Sizo Button",
            },
        },
        {
            "key": "ambra",
            "rgb": (168, 152, 124),
            "strip_rgb": (176, 162, 136),
            "ring_rgb": (183, 176, 161),
            "bark_tex": "ambra_log",
            "log": "ambra_log",
            "stripped_log": "stripped_ambra_log",
            "wood": "ambra_wood",
            "stripped_wood": "stripped_ambra_wood",
            "planks": "ambra_planks",
            "new_planks": False,
            "stairs": "ambra_planks_stairs",
            "new_stairs": True,
            "slab": "ambra_planks_slab",
            "new_slab": False,
            "fence": "ambra_fence",
            "new_fence": True,
            "gate": "ambra_fence_gate",
            "door": "ambra_door",
            "trapdoor": "ambra_trapdoor",
            "plate": "ambra_pressure_plate",
            "button": "ambra_button",
            "ru": {
                "stripped_ambra_log": "Обтёсанное бревно янтарника",
                "ambra_wood": "Древесина янтарника",
                "stripped_ambra_wood": "Обтёсанная древесина янтарника",
                "ambra_planks_stairs": "Ступеньки из янтарника",
                "ambra_fence": "Забор из янтарника",
                "ambra_fence_gate": "Калитка из янтарника",
                "ambra_door": "Дверь из янтарника",
                "ambra_trapdoor": "Люк из янтарника",
                "ambra_pressure_plate": "Нажимная плита из янтарника",
                "ambra_button": "Кнопка из янтарника",
            },
            "en": {
                "stripped_ambra_log": "Stripped Ambra Log",
                "ambra_wood": "Ambra Wood",
                "stripped_ambra_wood": "Stripped Ambra Wood",
                "ambra_planks_stairs": "Ambra Stairs",
                "ambra_fence": "Ambra Fence",
                "ambra_fence_gate": "Ambra Fence Gate",
                "ambra_door": "Ambra Door",
                "ambra_trapdoor": "Ambra Trapdoor",
                "ambra_pressure_plate": "Ambra Pressure Plate",
                "ambra_button": "Ambra Button",
            },
        },
        {
            "key": "garden",
            "rgb": (146, 110, 72),
            "strip_rgb": (154, 118, 79),
            "ring_rgb": (132, 98, 66),
            "bark_tex": "garden_log",
            "log": "garden_log",
            "stripped_log": "stripped_garden_log",
            "wood": "garden_wood",
            "stripped_wood": "stripped_garden_wood",
            "planks": "garden_planks",
            "new_planks": True,
            "stairs": "garden_stairs",
            "new_stairs": True,
            "slab": "garden_slab",
            "new_slab": True,
            "fence": "garden_fence",
            "new_fence": True,
            "gate": "garden_fence_gate",
            "door": "garden_door",
            "trapdoor": "garden_trapdoor",
            "plate": "garden_pressure_plate",
            "button": "garden_button",
            "ru": {
                "stripped_garden_log": "Обтёсанное бревно садового дуба",
                "garden_wood": "Древесина садового дуба",
                "stripped_garden_wood": "Обтёсанная древесина садового дуба",
                "garden_planks": "Доски из садового дуба",
                "garden_stairs": "Ступеньки из садового дуба",
                "garden_slab": "Плита из садового дуба",
                "garden_fence": "Забор из садового дуба",
                "garden_fence_gate": "Калитка из садового дуба",
                "garden_door": "Дверь из садового дуба",
                "garden_trapdoor": "Люк из садового дуба",
                "garden_pressure_plate": "Нажимная плита из садового дуба",
                "garden_button": "Кнопка из садового дуба",
            },
            "en": {
                "stripped_garden_log": "Stripped Garden Oak Log",
                "garden_wood": "Garden Oak Wood",
                "stripped_garden_wood": "Stripped Garden Oak Wood",
                "garden_planks": "Garden Oak Planks",
                "garden_stairs": "Garden Oak Stairs",
                "garden_slab": "Garden Oak Slab",
                "garden_fence": "Garden Oak Fence",
                "garden_fence_gate": "Garden Oak Fence Gate",
                "garden_door": "Garden Oak Door",
                "garden_trapdoor": "Garden Oak Trapdoor",
                "garden_pressure_plate": "Garden Oak Pressure Plate",
                "garden_button": "Garden Oak Button",
            },
        },
        {
            "key": "lilac",
            "rgb": (146, 120, 170),
            "strip_rgb": (156, 130, 178),
            "ring_rgb": (132, 108, 154),
            "bark_tex": "lilac_bark",
            "log": "lilac_log",
            "stripped_log": "stripped_lilac_log",
            "wood": "lilac_wood",
            "stripped_wood": "stripped_lilac_wood",
            "planks": "lilac_planks",
            "new_planks": True,
            "stairs": "lilac_stairs",
            "new_stairs": True,
            "slab": "lilac_slab",
            "new_slab": True,
            "fence": "lilac_fence",
            "new_fence": True,
            "gate": "lilac_fence_gate",
            "door": "lilac_door",
            "trapdoor": "lilac_trapdoor",
            "plate": "lilac_pressure_plate",
            "button": "lilac_button",
            "ru": {
                "stripped_lilac_log": "Обтёсанное сиреневое бревно",
                "lilac_wood": "Сиреневая древесина",
                "stripped_lilac_wood": "Обтёсанная сиреневая древесина",
                "lilac_planks": "Сиреневые доски",
                "lilac_stairs": "Сиреневые ступеньки",
                "lilac_slab": "Сиреневая плита",
                "lilac_fence": "Сиреневый забор",
                "lilac_fence_gate": "Сиреневая калитка",
                "lilac_door": "Сиреневая дверь",
                "lilac_trapdoor": "Сиреневый люк",
                "lilac_pressure_plate": "Сиреневая нажимная плита",
                "lilac_button": "Сиреневая кнопка",
            },
            "en": {
                "stripped_lilac_log": "Stripped Lilac Log",
                "lilac_wood": "Lilac Wood",
                "stripped_lilac_wood": "Stripped Lilac Wood",
                "lilac_planks": "Lilac Planks",
                "lilac_stairs": "Lilac Stairs",
                "lilac_slab": "Lilac Slab",
                "lilac_fence": "Lilac Fence",
                "lilac_fence_gate": "Lilac Fence Gate",
                "lilac_door": "Lilac Door",
                "lilac_trapdoor": "Lilac Trapdoor",
                "lilac_pressure_plate": "Lilac Pressure Plate",
                "lilac_button": "Lilac Button",
            },
        },
        {
            "key": "crystal",
            "rgb": (196, 218, 232),
            "strip_rgb": (206, 224, 236),
            "ring_rgb": (184, 208, 224),
            "bark_tex": "crystal_bark",
            "log": "crystal_log",
            "stripped_log": "stripped_crystal_log",
            "wood": "crystal_wood",
            "stripped_wood": "stripped_crystal_wood",
            "planks": "crystal_planks",
            "new_planks": True,
            "stairs": "crystal_stairs",
            "new_stairs": True,
            "slab": "crystal_slab",
            "new_slab": True,
            "fence": "crystal_fence",
            "new_fence": True,
            "gate": "crystal_fence_gate",
            "door": "crystal_door",
            "trapdoor": "crystal_trapdoor",
            "plate": "crystal_pressure_plate",
            "button": "crystal_button",
            "ru": {
                "stripped_crystal_log": "Обтёсанное бревно хрустальной берёзы",
                "crystal_wood": "Древесина хрустальной берёзы",
                "stripped_crystal_wood": "Обтёсанная древесина хрустальной берёзы",
                "crystal_planks": "Доски из хрустальной берёзы",
                "crystal_stairs": "Ступеньки из хрустальной берёзы",
                "crystal_slab": "Плита из хрустальной берёзы",
                "crystal_fence": "Забор из хрустальной берёзы",
                "crystal_fence_gate": "Калитка из хрустальной берёзы",
                "crystal_door": "Дверь из хрустальной берёзы",
                "crystal_trapdoor": "Люк из хрустальной берёзы",
                "crystal_pressure_plate": "Нажимная плита из хрустальной берёзы",
                "crystal_button": "Кнопка из хрустальной берёзы",
            },
            "en": {
                "stripped_crystal_log": "Stripped Crystal Birch Log",
                "crystal_wood": "Crystal Birch Wood",
                "stripped_crystal_wood": "Stripped Crystal Birch Wood",
                "crystal_planks": "Crystal Birch Planks",
                "crystal_stairs": "Crystal Birch Stairs",
                "crystal_slab": "Crystal Birch Slab",
                "crystal_fence": "Crystal Birch Fence",
                "crystal_fence_gate": "Crystal Birch Fence Gate",
                "crystal_door": "Crystal Birch Door",
                "crystal_trapdoor": "Crystal Birch Trapdoor",
                "crystal_pressure_plate": "Crystal Birch Pressure Plate",
                "crystal_button": "Crystal Birch Button",
            },
        },
        {
            "key": "charred",
            "rgb": (54, 47, 44),
            "strip_rgb": (62, 53, 49),
            "ring_rgb": (45, 38, 35),
            "bark_tex": "charred_log",
            "log": "charred_log",
            "stripped_log": "stripped_charred_log",
            "wood": "charred_wood",
            "stripped_wood": "stripped_charred_wood",
            "planks": "charred_planks",
            "new_planks": True,
            "stairs": "charred_stairs",
            "new_stairs": True,
            "slab": "charred_slab",
            "new_slab": True,
            "fence": "charred_fence",
            "new_fence": True,
            "gate": "charred_fence_gate",
            "door": "charred_door",
            "trapdoor": "charred_trapdoor",
            "plate": "charred_pressure_plate",
            "button": "charred_button",
            "ru": {
                "stripped_charred_log": "Обтёсанное обугленное бревно",
                "charred_wood": "Обугленная древесина",
                "stripped_charred_wood": "Обтёсанная обугленная древесина",
                "charred_planks": "Обугленные доски",
                "charred_stairs": "Обугленные ступеньки",
                "charred_slab": "Обугленная плита",
                "charred_fence": "Обугленный забор",
                "charred_fence_gate": "Обугленная калитка",
                "charred_door": "Обугленная дверь",
                "charred_trapdoor": "Обугленный люк",
                "charred_pressure_plate": "Обугленная нажимная плита",
                "charred_button": "Обугленная кнопка",
            },
            "en": {
                "stripped_charred_log": "Stripped Charred Log",
                "charred_wood": "Charred Wood",
                "stripped_charred_wood": "Stripped Charred Wood",
                "charred_planks": "Charred Planks",
                "charred_stairs": "Charred Stairs",
                "charred_slab": "Charred Slab",
                "charred_fence": "Charred Fence",
                "charred_fence_gate": "Charred Fence Gate",
                "charred_door": "Charred Door",
                "charred_trapdoor": "Charred Trapdoor",
                "charred_pressure_plate": "Charred Pressure Plate",
                "charred_button": "Charred Button",
            },
        },
        {
            "key": "dark",
            "rgb": (64, 46, 35),
            "strip_rgb": (74, 54, 40),
            "ring_rgb": (58, 42, 31),
            "bark_tex": "dark_beam",
            "log": "dark_beam",
            "stripped_log": "stripped_dark_beam",
            "wood": "dark_wood",
            "stripped_wood": "stripped_dark_wood",
            "planks": "dark_planks",
            "new_planks": False,
            "stairs": "dark_planks_stairs",
            "new_stairs": False,
            "slab": "dark_planks_slab",
            "new_slab": False,
            "fence": "dark_fence",
            "new_fence": False,
            "gate": "dark_fence_gate",
            "door": "dark_door",
            "trapdoor": "dark_trapdoor",
            "plate": "dark_pressure_plate",
            "button": "dark_button",
            "ru": {
                "stripped_dark_beam": "Обтёсанный брус тёмного дуба",
                "dark_wood": "Древесина тёмного дуба замка",
                "stripped_dark_wood": "Обтёсанная древесина тёмного дуба замка",
                "dark_fence_gate": "Калитка из тёмного дуба замка",
                "dark_door": "Дверь из тёмного дуба замка",
                "dark_trapdoor": "Люк из тёмного дуба замка",
                "dark_pressure_plate": "Нажимная плита из тёмного дуба замка",
                "dark_button": "Кнопка из тёмного дуба замка",
            },
            "en": {
                "stripped_dark_beam": "Stripped Dark Timber Beam",
                "dark_wood": "Castle Dark Wood",
                "stripped_dark_wood": "Stripped Castle Dark Wood",
                "dark_fence_gate": "Castle Dark Fence Gate",
                "dark_door": "Castle Dark Door",
                "dark_trapdoor": "Castle Dark Trapdoor",
                "dark_pressure_plate": "Castle Dark Pressure Plate",
                "dark_button": "Castle Dark Button",
            },
        },
    ]

    stone_families = [
        {
            "base": "elisar_stone",
            "tex": "elisar_stone",
            "stairs": ("elisar_stone_stairs", "Ступеньки из камня Элисара", "Elisar Stone Stairs"),
            "slab": ("elisar_stone_slab", "Плита из камня Элисара", "Elisar Stone Slab"),
            "wall": None,
        },
        {
            "base": "elisar_bricks",
            "tex": "elisar_bricks",
            "stairs": None,
            "slab": ("elisar_bricks_slab", "Плита из кирпича Элисара", "Elisar Brick Slab"),
            "wall": ("elisar_bricks_wall", "Ограда из кирпича Элисара", "Elisar Brick Wall"),
        },
        {
            "base": "garden_limestone",
            "tex": "garden_limestone",
            "stairs": ("garden_limestone_stairs", "Ступеньки из садового известняка", "Garden Limestone Stairs"),
            "slab": ("garden_limestone_slab", "Плита из садового известняка", "Garden Limestone Slab"),
            "wall": ("garden_limestone_wall", "Ограда из садового известняка", "Garden Limestone Wall"),
        },
        {
            "base": "rift_slate",
            "tex": "rift_slate",
            "stairs": ("rift_slate_stairs", "Ступеньки из разломного сланца", "Rift Slate Stairs"),
            "slab": ("rift_slate_slab", "Плита из разломного сланца", "Rift Slate Slab"),
            "wall": ("rift_slate_wall", "Ограда из разломного сланца", "Rift Slate Wall"),
        },
        {
            "base": "ivory_marble_bricks",
            "tex": "ivory_marble_bricks",
            "stairs": ("ivory_marble_bricks_stairs", "Ступеньки из белокаменного кирпича", "Ivory Marble Brick Stairs"),
            "slab": ("ivory_marble_bricks_slab", "Плита из белокаменного кирпича", "Ivory Marble Brick Slab"),
            "wall": None,
        },
        {
            "base": "ashen_cobblestone",
            "tex": "ashen_cobblestone",
            "stairs": ("ashen_cobblestone_stairs", "Ступеньки из пепельного булыжника", "Ashen Cobblestone Stairs"),
            "slab": ("ashen_cobblestone_slab", "Плита из пепельного булыжника", "Ashen Cobblestone Slab"),
            "wall": ("ashen_cobblestone_wall", "Ограда из пепельного булыжника", "Ashen Cobblestone Wall"),
        },
    ]

    ru_map = json.loads(LANG_RU.read_text(encoding="utf-8"))
    en_map = json.loads(LANG_EN.read_text(encoding="utf-8"))

    axe_mineable = []
    pickaxe_mineable = []
    logs_tag = []
    planks_tag = []
    w_stairs_tag = []
    w_slabs_tag = []
    w_fences_tag = []
    gates_tag = []
    w_doors_tag = []
    w_trapdoors_tag = []
    w_plates_tag = []
    w_buttons_tag = []
    s_stairs_tag = []
    s_slabs_tag = []
    s_walls_tag = []

    for fam in wood_families:
        rgb = fam["rgb"]
        s_rgb = fam["strip_rgb"]
        r_rgb = fam["ring_rgb"]
        s_log = fam["stripped_log"]
        s_top = f"{s_log}_top"

        # 1. Generate textures (16x16 PNGs)
        write_png(TEX_BLOCK / f"{s_log}.png", make_stripped_side(s_rgb))
        write_png(TEX_BLOCK / f"{s_top}.png", make_stripped_top(s_rgb, r_rgb))
        if fam["new_planks"]:
            write_png(TEX_BLOCK / f"{fam['planks']}.png", recolor_template(planks_tmpl, rgb, contrast=1.12))
        write_png(TEX_BLOCK / f"{fam['door']}_top.png", recolor_template(door_top_tmpl, rgb, contrast=1.10, hinge_metal=True))
        write_png(TEX_BLOCK / f"{fam['door']}_bottom.png", recolor_template(door_bot_tmpl, rgb, contrast=1.10, hinge_metal=True))
        write_png(TEX_ITEM / f"{fam['door']}.png", recolor_template(door_item_tmpl, rgb, contrast=1.10, hinge_metal=True))
        write_png(TEX_BLOCK / f"{fam['trapdoor']}.png", make_vanilla_trapdoor(rgb))

        # 2. Generate blockstates + models + loot
        gen_pillar(s_log, s_log, s_top)
        gen_pillar(fam["wood"], fam["bark_tex"], fam["bark_tex"])
        gen_pillar(fam["stripped_wood"], s_log, s_log)
        if fam["new_planks"]:
            gen_cube(fam["planks"], fam["planks"])
        if fam["new_stairs"]:
            gen_stairs(fam["stairs"], fam["planks"])
        if fam["new_slab"]:
            gen_slab(fam["slab"], fam["planks"], fam["planks"])
        if fam["new_fence"]:
            gen_fence(fam["fence"], fam["planks"])
        gen_fence_gate(fam["gate"], fam["planks"])
        gen_door(fam["door"])
        gen_trapdoor(fam["trapdoor"])
        gen_pressure_plate(fam["plate"], fam["planks"])
        gen_button(fam["button"], fam["planks"])

        # 3. Recipes
        add_wood_recipes(fam)

        # 4. Tags & Lang
        logs_tag.extend([fam["log"], s_log, fam["wood"], fam["stripped_wood"]])
        planks_tag.append(fam["planks"])
        w_stairs_tag.append(fam["stairs"])
        w_slabs_tag.append(fam["slab"])
        w_fences_tag.append(fam["fence"])
        gates_tag.append(fam["gate"])
        w_doors_tag.append(fam["door"])
        w_trapdoors_tag.append(fam["trapdoor"])
        w_plates_tag.append(fam["plate"])
        w_buttons_tag.append(fam["button"])
        axe_mineable.extend([
            fam["log"], s_log, fam["wood"], fam["stripped_wood"],
            fam["planks"], fam["stairs"], fam["slab"], fam["fence"],
            fam["gate"], fam["door"], fam["trapdoor"], fam["plate"], fam["button"]
        ])
        for bid, txt in fam["ru"].items():
            ru_map[f"block.riftworld.{bid}"] = txt
        for bid, txt in fam["en"].items():
            en_map[f"block.riftworld.{bid}"] = txt

    for sf in stone_families:
        st_id = sf["stairs"][0] if sf["stairs"] else None
        sl_id = sf["slab"][0] if sf["slab"] else None
        wl_id = sf["wall"][0] if sf["wall"] else None
        if sf["stairs"]:
            bid, ru_t, en_t = sf["stairs"]
            gen_stairs(bid, sf["tex"])
            s_stairs_tag.append(bid)
            pickaxe_mineable.append(bid)
            ru_map[f"block.riftworld.{bid}"] = ru_t
            en_map[f"block.riftworld.{bid}"] = en_t
        if sf["slab"]:
            bid, ru_t, en_t = sf["slab"]
            gen_slab(bid, sf["base"], sf["tex"])
            s_slabs_tag.append(bid)
            pickaxe_mineable.append(bid)
            ru_map[f"block.riftworld.{bid}"] = ru_t
            en_map[f"block.riftworld.{bid}"] = en_t
        if sf["wall"]:
            bid, ru_t, en_t = sf["wall"]
            gen_wall(bid, sf["tex"])
            s_walls_tag.append(bid)
            pickaxe_mineable.append(bid)
            ru_map[f"block.riftworld.{bid}"] = ru_t
            en_map[f"block.riftworld.{bid}"] = en_t
        add_stone_recipes(sf["base"], st_id, sl_id, wl_id)

    # Update Tags (Blocks + Items)
    append_tag("minecraft/tags/blocks/mineable/axe.json", axe_mineable)
    append_tag("minecraft/tags/blocks/mineable/pickaxe.json", pickaxe_mineable)
    for scope in ("blocks", "items"):
        append_tag(f"minecraft/tags/{scope}/logs.json", logs_tag)
        append_tag(f"minecraft/tags/{scope}/logs_that_burn.json", logs_tag)
        append_tag(f"minecraft/tags/{scope}/planks.json", planks_tag)
        append_tag(f"minecraft/tags/{scope}/wooden_stairs.json", w_stairs_tag)
        append_tag(f"minecraft/tags/{scope}/stairs.json", w_stairs_tag + s_stairs_tag)
        append_tag(f"minecraft/tags/{scope}/wooden_slabs.json", w_slabs_tag)
        append_tag(f"minecraft/tags/{scope}/slabs.json", w_slabs_tag + s_slabs_tag)
        append_tag(f"minecraft/tags/{scope}/wooden_fences.json", w_fences_tag)
        append_tag(f"minecraft/tags/{scope}/fences.json", w_fences_tag)
        append_tag(f"minecraft/tags/{scope}/fence_gates.json", gates_tag)
        append_tag(f"minecraft/tags/{scope}/wooden_doors.json", w_doors_tag)
        append_tag(f"minecraft/tags/{scope}/doors.json", w_doors_tag)
        append_tag(f"minecraft/tags/{scope}/wooden_trapdoors.json", w_trapdoors_tag)
        append_tag(f"minecraft/tags/{scope}/trapdoors.json", w_trapdoors_tag)
        append_tag(f"minecraft/tags/{scope}/wooden_pressure_plates.json", w_plates_tag)
        append_tag(f"minecraft/tags/{scope}/pressure_plates.json", w_plates_tag)
        append_tag(f"minecraft/tags/{scope}/wooden_buttons.json", w_buttons_tag)
        append_tag(f"minecraft/tags/{scope}/buttons.json", w_buttons_tag)
        append_tag(f"minecraft/tags/{scope}/walls.json", s_walls_tag)

    write_json(LANG_RU, ru_map)
    write_json(LANG_EN, en_map)
    print(f"Generated v0.28.0 assets/data cleanly: {sum(len(f['ru']) for f in wood_families)} wood blocks + {len(pickaxe_mineable)} stone blocks = {sum(len(f['ru']) for f in wood_families) + len(pickaxe_mineable)} blocks!")


if __name__ == "__main__":
    main()
