#!/usr/bin/env python3
"""
v0.49.0 — «перестройка» Мороковой Пади: два СВОИХ дерева вместо перекрашенных ванильных.

Раньше бор Пади состоял из бледной сосны (перекрашенной ванильной «сизой»),
поэтому лес читался как обычный берёзовый лес с серой землёй. Теперь у биома
есть собственная древесина:

  * Гнилая роща (Rotwood) — чёрное, узловатое дерево с плотной тёмной кроной.
    На нём стоит весь ночной лес Пади.
  * Морозный терновник (Frostbriar) — скрученное, колючее дерево с ледяной
    листвой; растёт по краям болот и на склонах.

Всё делается по-ванильному: 16x16 текстуры (свой PNG-writer), блок-стейты,
модели блоков/предметов, лут-таблицы, рецепты, lang ru/en.

Запуск:  python3 tools/rift/gen_v049_morok_woods.py
"""
import json
import pathlib
import struct
import zlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
ASSETS = ROOT / "mod/src/main/resources/assets/riftworld"
DATA = ROOT / "mod/src/main/resources/data"
TEX = ASSETS / "textures/block"
BS = ASSETS / "blockstates"
MB = ASSETS / "models/block"
MI = ASSETS / "models/item"
LOOT = DATA / "riftworld/loot_tables/blocks"
RECIPE = DATA / "riftworld/recipes"

# ---------------------------------------------------------------- PNG (RGBA)
def read_png(path: pathlib.Path):
    raw = path.read_bytes()
    pos, w, h, ctype, idat = 8, 0, 0, 6, bytearray()
    while pos < len(raw):
        ln, typ = struct.unpack(">I4s", raw[pos:pos + 8])
        chunk = raw[pos + 8:pos + 8 + ln]
        pos += 12 + ln
        if typ == b"IHDR":
            w, h, _bd, ctype, *_ = struct.unpack(">IIBBBBB", chunk)
        elif typ == b"IDAT":
            idat.extend(chunk)
    data = zlib.decompress(bytes(idat))
    bpp = 4 if ctype == 6 else 3
    stride = 1 + w * bpp
    out, prev = [], bytearray(w * bpp)
    for y in range(h):
        flt = data[y * stride]
        row = bytearray(data[y * stride + 1:(y + 1) * stride])
        if flt == 1:
            for i in range(bpp, len(row)):
                row[i] = (row[i] + row[i - bpp]) & 0xFF
        elif flt == 2:
            for i in range(len(row)):
                row[i] = (row[i] + prev[i]) & 0xFF
        elif flt == 3:
            for i in range(len(row)):
                left = row[i - bpp] if i >= bpp else 0
                row[i] = (row[i] + ((left + prev[i]) >> 1)) & 0xFF
        elif flt == 4:
            for i in range(len(row)):
                a = row[i - bpp] if i >= bpp else 0
                b = prev[i]
                c = row[i - bpp - 1] if i >= bpp + 1 else 0
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                row[i] = (row[i] + pr) & 0xFF
        prev = row
        px = []
        for x in range(w):
            r, g, b = row[x * bpp], row[x * bpp + 1], row[x * bpp + 2]
            a = row[x * bpp + 3] if bpp == 4 else 255
            px.append((r, g, b, a))
        out.append(px)
    return out


def write_png(path: pathlib.Path, pixels):
    h, w = len(pixels), len(pixels[0])
    raw = bytearray()
    for row in pixels:
        raw.append(0)
        for (r, g, b, a) in row:
            raw += bytes((r, g, b, a))

    def chunk(tag: bytes, payload: bytes) -> bytes:
        return struct.pack(">I", len(payload)) + tag + payload + struct.pack(">I", zlib.crc32(tag + payload) & 0xFFFFFFFF)

    out = b"\x89PNG\r\n\x1a\n"
    out += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    out += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    out += chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(out)


# ---------------------------------------------------------------- helpers
def hx(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def lum(px):
    return 0.299 * px[0] + 0.587 * px[1] + 0.114 * px[2]


def recolor(pixels, stops):
    stops = sorted(((p, hx(c)) for p, c in stops), key=lambda t: t[0])
    cache = {}
    out = []
    for row in pixels:
        nrow = []
        for (r, g, b, a) in row:
            L = lum((r, g, b))
            key = int(L) // 4
            if key not in cache:
                t = (key * 4) / 255.0
                if t <= stops[0][0]:
                    col = stops[0][1]
                elif t >= stops[-1][0]:
                    col = stops[-1][1]
                else:
                    col = None
                    for i in range(len(stops) - 1):
                        p0, c0 = stops[i]
                        p1, c1 = stops[i + 1]
                        if p0 <= t <= p1:
                            f = 0.0 if p1 == p0 else (t - p0) / (p1 - p0)
                            col = (int(c0[0] + (c1[0] - c0[0]) * f),
                                   int(c0[1] + (c1[1] - c0[1]) * f),
                                   int(c0[2] + (c1[2] - c0[2]) * f))
                            break
                    if col is None:
                        col = stops[-1][1]
                cache[key] = col
            c = cache[key]
            nrow.append((c[0], c[1], c[2], a))
        out.append(nrow)
    return out


def add_cracks(pixels, seed, density=0.45):
    import random
    rnd = random.Random(seed)
    h, w = len(pixels), len(pixels[0])
    out = [list(row) for row in pixels]
    n = int(w * h * density)
    for _ in range(n):
        x = rnd.randrange(w)
        y = rnd.randrange(h)
        length = rnd.randint(2, 7)
        dx = rnd.choice((-1, 0, 1))
        dy = rnd.choice((-1, 0, 1))
        if dx == 0 and dy == 0:
            dx = 1
        for i in range(length):
            xx = (x + dx * i) % w
            yy = (y + dy * i) % h
            r, g, b, a = out[yy][xx]
            out[yy][xx] = (max(0, r - 52), max(0, g - 50), max(0, b - 46), a)
    return out


def rime_specks(pixels, density=0.10, seed=0):
    import random
    rnd = random.Random(seed)
    h, w = len(pixels), len(pixels[0])
    out = [list(row) for row in pixels]
    n = int(w * h * density)
    for _ in range(n):
        x = rnd.randrange(w)
        y = rnd.randrange(h)
        r, g, b, a = out[y][x]
        k = rnd.randint(60, 120)
        out[y][x] = (min(255, r + k), min(255, g + k), min(255, b + k), a)
    return out


def darken(pixels, factor=0.55):
    return [[(int(r * factor), int(g * factor), int(b * factor), a) for (r, g, b, a) in row] for row in pixels]


def solid(pixels):
    return [[(r, g, b, 255) for (r, g, b, a) in row] for row in pixels]


# ---------------------------------------------------------------- palettes
# Гнилая роща: почти чёрная кора с зелёным подтеньем.
ROT_BARK = [(0.0, "#0B0D0C"), (0.40, "#1A2019"), (0.75, "#2B3326"), (1.0, "#414B36")]
ROT_CORE = [(0.0, "#241A18"), (0.45, "#4A342A"), (0.80, "#6B4C36"), (1.0, "#8A6444")]
ROT_PLANKS = [(0.0, "#171A14"), (0.45, "#2A3024"), (0.78, "#3C4432"), (1.0, "#525C43")]
ROT_LEAVES = [(0.0, "#050806"), (0.40, "#0C140E"), (0.75, "#162418"), (1.0, "#263A26")]
# Морозный терновник: серая кора с ледяной листвой.
BRIAR_BARK = [(0.0, "#22252B"), (0.42, "#414852"), (0.78, "#5E6875"), (1.0, "#7E8895")]
BRIAR_CORE = [(0.0, "#5B6470"), (0.45, "#8794A2"), (0.80, "#A8B6C4"), (1.0, "#CAD7E2")]
BRIAR_PLANKS = [(0.0, "#2B2F36"), (0.45, "#4A5058"), (0.78, "#646C76"), (1.0, "#828B95")]
BRIAR_LEAVES = [(0.0, "#1A2630"), (0.40, "#31454F"), (0.75, "#4E6B76"), (1.0, "#7A9BAA")]

TEXTURE_JOBS = [
    # Гнилая роща
    ("morok_log.png", "rotwood_log.png", ROT_BARK, dict(cracks=True)),
    ("morok_log_top.png", "rotwood_log_top.png", ROT_CORE, dict()),
    ("stripped_morok_log.png", "stripped_rotwood_log.png", ROT_CORE, dict()),
    ("stripped_morok_log_top.png", "stripped_rotwood_log_top.png", ROT_CORE, dict()),
    ("morok_planks.png", "rotwood_planks.png", ROT_PLANKS, dict()),
    ("morok_leaves.png", "rotwood_leaves.png", ROT_LEAVES, dict(solid=True)),
    ("morok_sapling.png", "rotwood_sapling.png", ROT_LEAVES, dict()),
    ("morok_door_bottom.png", "rotwood_door_bottom.png", ROT_PLANKS, dict()),
    ("morok_door_top.png", "rotwood_door_top.png", ROT_PLANKS, dict()),
    ("morok_trapdoor.png", "rotwood_trapdoor.png", ROT_PLANKS, dict()),
    # Морозный терновник
    ("morok_log.png", "frostbriar_log.png", BRIAR_BARK, dict(cracks=True)),
    ("morok_log_top.png", "frostbriar_log_top.png", BRIAR_CORE, dict(rime=0.10)),
    ("stripped_morok_log.png", "stripped_frostbriar_log.png", BRIAR_CORE, dict(rime=0.10)),
    ("stripped_morok_log_top.png", "stripped_frostbriar_log_top.png", BRIAR_CORE, dict(rime=0.10)),
    ("morok_planks.png", "frostbriar_planks.png", BRIAR_PLANKS, dict()),
    ("morok_leaves.png", "frostbriar_leaves.png", BRIAR_LEAVES, dict(solid=True, rime=0.12)),
    ("morok_sapling.png", "frostbriar_sapling.png", BRIAR_LEAVES, dict()),
    ("morok_door_bottom.png", "frostbriar_door_bottom.png", BRIAR_PLANKS, dict()),
    ("morok_door_top.png", "frostbriar_door_top.png", BRIAR_PLANKS, dict()),
    ("morok_trapdoor.png", "frostbriar_trapdoor.png", BRIAR_PLANKS, dict()),
]


def gen_textures():
    made = []
    for src_name, dst_name, stops, opts in TEXTURE_JOBS:
        src = TEX / src_name
        if not src.exists():
            print("  ! нет источника текстуры:", src_name)
            continue
        px = recolor(read_png(src), stops)
        if opts.get("cracks"):
            px = add_cracks(px, seed=abs(hash(dst_name)) & 0x7FFF, density=0.5)
        if opts.get("solid"):
            px = solid(px)
        if opts.get("rime"):
            px = rime_specks(px, density=opts.get("rime", 0.10), seed=(abs(hash(dst_name)) >> 3) & 0x7FFF)
        write_png(TEX / dst_name, px)
        made.append(dst_name)
    return made


# ---------------------------------------------------------------- JSON cloning
def write_json(path: pathlib.Path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def clone_json(src_prefix, dst_prefix, subdir):
    base = {"blockstates": BS, "models_block": MB, "models_item": MI, "loot": LOOT}[subdir]
    made = 0
    for f in sorted(base.glob(f"{src_prefix}*.json")):
        target = base / f"{f.stem.replace(src_prefix, dst_prefix, 1)}.json"
        text = f.read_text(encoding="utf-8").replace(src_prefix, dst_prefix)
        target.write_text(text, encoding="utf-8")
        made += 1
    return made


def clone_set(src, dst):
    n = 0
    n += clone_json(src, dst, "blockstates")
    n += clone_json(src, dst, "models_block")
    n += clone_json(src, dst, "models_item")
    n += clone_json(src, dst, "loot")
    return n


# ---------------------------------------------------------------- recipes
def r_shapeless(name, ingredients, result, count=1, group="building"):
    write_json(RECIPE / f"{name}.json", {
        "type": "minecraft:crafting_shapeless",
        "category": "building",
        "group": group,
        "ingredients": [{"item": i} for i in ingredients],
        "result": {"item": result, "count": count},
    })


def r_shaped(name, pattern, key, result, count=1, group="building"):
    write_json(RECIPE / f"{name}.json", {
        "type": "minecraft:crafting_shaped",
        "category": "building",
        "group": group,
        "pattern": pattern,
        "key": {k: {"item": v} for k, v in key.items()},
        "result": {"item": result, "count": count},
    })


def gen_recipes():
    R = "riftworld:"
    n = 0
    for wood in ("rotwood", "frostbriar"):
        for src in (f"{wood}_log", f"{wood}_wood", f"stripped_{wood}_log", f"stripped_{wood}_wood"):
            r_shapeless(f"{wood}_planks_from_{src}", [R + src], R + f"{wood}_planks", 4, group="planks")
            n += 1
        r_shaped(f"{wood}_planks_stairs", ["P  ", "PP ", "PPP"], {"P": R + f"{wood}_planks"}, R + f"{wood}_planks_stairs", 4)
        r_shaped(f"{wood}_planks_slab", ["PPP"], {"P": R + f"{wood}_planks"}, R + f"{wood}_planks_slab", 6)
        r_shaped(f"{wood}_fence", ["PSP", "PSP"], {"P": R + f"{wood}_planks", "S": "minecraft:stick"}, R + f"{wood}_fence", 3)
        r_shaped(f"{wood}_fence_gate", ["SPS", "SPS"], {"P": R + f"{wood}_planks", "S": "minecraft:stick"}, R + f"{wood}_fence_gate")
        r_shaped(f"{wood}_door", ["PP", "PP", "PP"], {"P": R + f"{wood}_planks"}, R + f"{wood}_door", 3)
        r_shaped(f"{wood}_trapdoor", ["PPP", "PPP"], {"P": R + f"{wood}_planks"}, R + f"{wood}_trapdoor", 2)
        r_shaped(f"{wood}_pressure_plate", ["PP"], {"P": R + f"{wood}_planks"}, R + f"{wood}_pressure_plate")
        r_shapeless(f"{wood}_button", [R + f"{wood}_planks"], R + f"{wood}_button", group="buttons")
        n += 9
    # Колючие ветки терновника — из ледяных клыков Пади.
    r_shaped("frostbriar_sapling_from_ice_fang", ["FF", "FF"], {"F": R + "ice_fang"}, R + "frostbriar_sapling", 2, group="plants")
    n += 1
    return n


# ---------------------------------------------------------------- lang
def gen_lang():
    names = {
        "rotwood_log": ("Гнилая роща (бревно)", "Rotwood Log"),
        "stripped_rotwood_log": ("Обтёсанная гнилая роща", "Stripped Rotwood Log"),
        "rotwood_wood": ("Гнилая роща (дерево)", "Rotwood Wood"),
        "stripped_rotwood_wood": ("Обтёсанная гнилая роща (дерево)", "Stripped Rotwood Wood"),
        "rotwood_planks": ("Гнилые доски", "Rotwood Planks"),
        "rotwood_planks_stairs": ("Гнилые ступени", "Rotwood Stairs"),
        "rotwood_planks_slab": ("Гнилая плита", "Rotwood Slab"),
        "rotwood_fence": ("Гнилый забор", "Rotwood Fence"),
        "rotwood_fence_gate": ("Гнилые ворота", "Rotwood Fence Gate"),
        "rotwood_door": ("Гнилая дверь", "Rotwood Door"),
        "rotwood_trapdoor": ("Гнилый люк", "Rotwood Trapdoor"),
        "rotwood_pressure_plate": ("Гнилая нажимная плита", "Rotwood Pressure Plate"),
        "rotwood_button": ("Гнилая кнопка", "Rotwood Button"),
        "rotwood_leaves": ("Гнилая листва", "Rotwood Leaves"),
        "rotwood_sapling": ("Росток гнилой рощи", "Rotwood Sapling"),
        "frostbriar_log": ("Морозный терновник (бревно)", "Frostbriar Log"),
        "stripped_frostbriar_log": ("Обтёсанный морозный терновник", "Stripped Frostbriar Log"),
        "frostbriar_wood": ("Морозный терновник (дерево)", "Frostbriar Wood"),
        "stripped_frostbriar_wood": ("Обтёсанный морозный терновник (дерево)", "Stripped Frostbriar Wood"),
        "frostbriar_planks": ("Терновые доски", "Frostbriar Planks"),
        "frostbriar_planks_stairs": ("Терновые ступени", "Frostbriar Stairs"),
        "frostbriar_planks_slab": ("Терновая плита", "Frostbriar Slab"),
        "frostbriar_fence": ("Терновый забор", "Frostbriar Fence"),
        "frostbriar_fence_gate": ("Терновые ворота", "Frostbriar Fence Gate"),
        "frostbriar_door": ("Терновая дверь", "Frostbriar Door"),
        "frostbriar_trapdoor": ("Терновый люк", "Frostbriar Trapdoor"),
        "frostbriar_pressure_plate": ("Терновая нажимная плита", "Frostbriar Pressure Plate"),
        "frostbriar_button": ("Терновая кнопка", "Frostbriar Button"),
        "frostbriar_leaves": ("Ледяная листва", "Frostbriar Leaves"),
        "frostbriar_sapling": ("Росток терновника", "Frostbriar Sapling"),
    }
    for path, idx in ((ASSETS / "lang/ru_ru.json", 0), (ASSETS / "lang/en_us.json", 1)):
        data = json.loads(path.read_text(encoding="utf-8"))
        for key, pair in names.items():
            data[f"block.riftworld.{key}"] = pair[idx]
            data[f"item.riftworld.{key}"] = pair[idx]
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return len(names)


def main():
    print("== v0.49.0: свои деревья Мороковой Пади ==")
    tex = gen_textures()
    print(f"  текстур: {len(tex)}")
    n = 0
    n += clone_set("morok_", "rotwood_")
    n += clone_set("morok_", "frostbriar_")
    print(f"  json-файлов клонировано: {n}")
    # полные наборы бло��-стейтов для брёвен/досок
    for wood in ("rotwood", "frostbriar"):
        for extra in ("log", "wood"):
            for stripped in (False, True):
                key = f"{'stripped_' if stripped else ''}{wood}_{extra}"
                src = "stripped_morok_log" if stripped else "morok_log"
                base = {"blockstates": BS, "models_block": MB, "models_item": MI, "loot": LOOT}
                for sub, folder in base.items():
                    for f in sorted(folder.glob(f"{src}*.json")):
                        target = folder / f"{f.stem.replace(src, key, 1)}.json"
                        target.write_text(f.read_text(encoding="utf-8").replace(src, key), encoding="utf-8")
    r = gen_recipes()
    print(f"  рецептов: {r}")
    l = gen_lang()
    print(f"  записей lang: {l}")
    print("OK.")


if __name__ == "__main__":
    main()
