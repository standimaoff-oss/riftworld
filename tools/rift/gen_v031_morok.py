#!/usr/bin/env python3
"""
v0.31.0 «Мороковая Падь» / morok_hollow — набор блоков биома.

Делает полностью по-ванильному (формы как у Minecraft, без супер-моделей):
  * 16x16 текстуры (чистый Python: zlib + собственный PNG writer, PIL недоступен);
  * blockstates / модели блоков / модели предметов / лут-таблицы — клонированием
    ванильных шаблонов уже существующих блоков мода с заменой префикса;
  * практические крафты (по правилу мода: кроме лабораторных и очень редких);
  * lang ru/en.

Палитра из концепта: кость #D9D2C4 + трещины #1B1B1F, хвоя #4A5F58, земля #22262B,
мох #9FAFA6, лёд #BBD6E4, свет фонаря #8FF0F0, кирпич #C9C3B4.
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
                c = prev[i - bpp] if i >= bpp else 0
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


# ---------------------------------------------------------------- palettes
def hx(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def lum(px):
    return 0.299 * px[0] + 0.587 * px[1] + 0.114 * px[2]


def recolor(pixels, stops, keep_alpha=True):
    """Перекрашивает по яркости: stops = [(0.0, '#000'), (0.5, '#888'), (1.0, '#fff')]."""
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
                    for i in range(len(stops) - 1):
                        p0, c0 = stops[i]
                        p1, c1 = stops[i + 1]
                        if p0 <= t <= p1:
                            k = (t - p0) / max(1e-6, p1 - p0)
                            col = tuple(int(c0[j] + (c1[j] - c0[j]) * k) for j in range(3))
                            break
                cache[key] = col
            c = cache[key]
            nrow.append((c[0], c[1], c[2], a if keep_alpha else 255))
        out.append(nrow)
    return out


def rnd(seed):
    state = seed & 0xFFFFFFFF

    def nxt():
        nonlocal state
        state = (1103515245 * state + 12345) & 0x7FFFFFFF
        return state / 0x7FFFFFFF
    return nxt


def add_cracks(pixels, seed=7, density=0.16, color="#1B1B1F"):
    """Вертикальные «трещины» в коре/кирпиче (как на референсе 03)."""
    h, w = len(pixels), len(pixels[0])
    r = rnd(seed)
    c = hx(color)
    for x in range(w):
        if r() > density:
            continue
        y = int(r() * h)
        ln = 2 + int(r() * 5)
        for k in range(ln):
            yy = (y + k) % h
            pixels[yy][x] = (c[0], c[1], c[2], pixels[yy][x][3])
            if r() > 0.72:
                xx = (x + 1) % w
                pixels[yy][xx] = (c[0], c[1], c[2], pixels[yy][xx][3])
    return pixels


def rime_specks(pixels, seed=11, density=0.10, color="#F2F7F7"):
    h, w = len(pixels), len(pixels[0])
    r = rnd(seed)
    c = hx(color)
    for y in range(h):
        for x in range(w):
            if r() < density and pixels[y][x][3] > 0:
                pixels[y][x] = (c[0], c[1], c[2], pixels[y][x][3])
    return pixels


BARK = [(0.0, "#14151A"), (0.25, "#3A3B3E"), (0.55, "#B9B2A4"), (0.80, "#D9D2C4"), (1.0, "#EFE9DC")]
BARK_STRIPPED = [(0.0, "#8C8579"), (0.5, "#CFC7B7"), (1.0, "#E8E1D2")]
PLANKS = [(0.0, "#8E887A"), (0.45, "#BDB5A5"), (0.75, "#D3CBBA"), (1.0, "#E4DDCE")]
NEEDLES = [(0.0, "#1E2A26"), (0.4, "#3A4E47"), (0.7, "#4A5F58"), (1.0, "#7E9198")]
SOIL = [(0.0, "#14161A"), (0.5, "#22262B"), (0.85, "#333941"), (1.0, "#4A5058")]
GRASS_TOP = [(0.0, "#4E5A54"), (0.4, "#7E8F86"), (0.8, "#9FAFA6"), (1.0, "#B9C4BC")]
MOSS = [(0.0, "#5C6A62"), (0.5, "#9FAFA6"), (0.85, "#B7C4BB"), (1.0, "#CBD5CC")]
BRICK = [(0.0, "#6E6A5F"), (0.35, "#A8A395"), (0.7, "#C9C3B4"), (1.0, "#DED8C9")]
ICE = [(0.0, "#6E8B9C"), (0.4, "#A8C8DA"), (0.75, "#BBD6E4"), (1.0, "#E2F1F7")]
LAMP = [(0.0, "#123033"), (0.35, "#2C6E72"), (0.65, "#8FF0F0"), (1.0, "#EAFFFF")]
BONE = [(0.0, "#8A8172"), (0.5, "#CFC5B0"), (1.0, "#EFE7D6")]

TEXTURE_JOBS = [
    ("sizo_log.png", "morok_log.png", BARK, dict(cracks=True)),
    ("sizo_log_top.png", "morok_log_top.png", PLANKS, dict()),
    ("stripped_sizo_log.png", "stripped_morok_log.png", BARK_STRIPPED, dict(cracks=False)),
    ("stripped_sizo_log_top.png", "stripped_morok_log_top.png", PLANKS, dict()),
    ("sizo_planks.png", "morok_planks.png", PLANKS, dict()),
    ("sizo_leaves.png", "morok_leaves.png", NEEDLES, dict(rime=True)),
    ("celadon_fern.png", "morok_sapling.png", PLANKS, dict()),
    ("sizo_door_bottom.png", "morok_door_bottom.png", PLANKS, dict()),
    ("sizo_door_top.png", "morok_door_top.png", PLANKS, dict()),
    ("sizo_trapdoor.png", "morok_trapdoor.png", PLANKS, dict()),
    ("elisar_soil.png", "frozen_soil.png", SOIL, dict(rime=True)),
    ("elisar_grass_top.png", "rime_grass_top.png", GRASS_TOP, dict(rime=True)),
    ("elisar_grass_side.png", "rime_grass_side.png", GRASS_TOP, dict(rime=True)),
    ("elisar_grass.png", "rime_grass.png", GRASS_TOP, dict()),
    ("elisar_moss.png", "pale_moss_block.png", MOSS, dict()),
    ("castle_carpet.png", "pale_moss_carpet.png", MOSS, dict()),
    ("castle_carpet_border.png", "pale_moss_carpet_border.png", MOSS, dict()),
    ("castle_carpet_center.png", "pale_moss_carpet_center.png", MOSS, dict()),
    ("castle_carpet_trim.png", "pale_moss_carpet_trim.png", MOSS, dict()),
    ("celadon_fern.png", "bone_bramble.png", BONE, dict()),
    ("celadon_fern.png", "ice_fang.png", ICE, dict(rime=True)),
    ("elisar_lantern.png", "fog_lantern.png", LAMP, dict()),
    ("pearl_calcite_bricks.png", "cracked_rime_bricks.png", BRICK, dict(cracks=True)),
    ("ivory_marble_pillar.png", "rime_brick_pillar.png", BRICK, dict()),
    ("ivory_marble_pillar_top.png", "rime_brick_pillar_top.png", BRICK, dict()),
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
            px = add_cracks(px, seed=hash(dst_name) & 0x7FFF)
        if opts.get("rime"):
            px = rime_specks(px, seed=(hash(dst_name) >> 3) & 0x7FFF)
        write_png(TEX / dst_name, px)
        made.append(dst_name)
    return made


# ---------------------------------------------------------------- JSON cloning
def clone_json(src_prefix, dst_prefix, subdir, exact=False):
    """Копирует blockstates/models/loot с заменой префикса в имени и внутри файла."""
    base = {"blockstates": BS, "models_block": MB, "models_item": MI, "loot": LOOT}[subdir]
    made = 0
    for f in base.glob(f"{src_prefix}*.json"):
        if exact and f.stem != src_prefix:
            continue
        target = base / f"{f.stem.replace(src_prefix, dst_prefix, 1)}.json"
        text = f.read_text(encoding="utf-8").replace(src_prefix, dst_prefix)
        target.write_text(text, encoding="utf-8")
        made += 1
    return made


def write_json(path: pathlib.Path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


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
    # бревно → 4 доски (жёсткое правило мода), любой вариант бревна
    for src in ("morok_log", "morok_wood", "stripped_morok_log", "stripped_morok_wood"):
        r_shapeless(f"morok_planks_from_{src}", [R + src], R + "morok_planks", 4, group="planks")
        n += 1
    r_shaped("morok_planks_stairs", ["P  ", "PP ", "PPP"], {"P": R + "morok_planks"}, R + "morok_planks_stairs", 4)
    r_shaped("morok_planks_slab", ["PPP"], {"P": R + "morok_planks"}, R + "morok_planks_slab", 6)
    r_shaped("morok_fence", ["PSP", "PSP"], {"P": R + "morok_planks", "S": "minecraft:stick"}, R + "morok_fence", 3)
    r_shaped("morok_fence_gate", ["SPS", "SPS"], {"P": R + "morok_planks", "S": "minecraft:stick"}, R + "morok_fence_gate")
    r_shaped("morok_door", ["PP", "PP", "PP"], {"P": R + "morok_planks"}, R + "morok_door", 3)
    r_shaped("morok_trapdoor", ["PPP", "PPP"], {"P": R + "morok_planks"}, R + "morok_trapdoor", 2)
    r_shaped("morok_pressure_plate", ["PP"], {"P": R + "morok_planks"}, R + "morok_pressure_plate")
    r_shapeless("morok_button", [R + "morok_planks"], R + "morok_button", group="buttons")
    n += 8
    r_shaped("frozen_soil", ["IS", "SI"], {"I": "minecraft:ice", "S": R + "elisar_soil"}, R + "frozen_soil", 4)
    r_shapeless("rime_grass", [R + "frozen_soil", R + "pale_moss_block"], R + "rime_grass")
    r_shapeless("pale_moss_block", [R + "rime_grass", "minecraft:vine"], R + "pale_moss_block", 2)
    r_shaped("pale_moss_carpet", ["MM"], {"M": R + "pale_moss_block"}, R + "pale_moss_carpet", 3)
    r_shaped("cracked_rime_bricks", ["BI", "IB"], {"B": R + "frozen_soil", "I": "minecraft:packed_ice"}, R + "cracked_rime_bricks", 4)
    r_shaped("cracked_rime_bricks_stairs", ["B  ", "BB ", "BBB"], {"B": R + "cracked_rime_bricks"}, R + "cracked_rime_bricks_stairs", 4)
    r_shaped("cracked_rime_bricks_slab", ["BBB"], {"B": R + "cracked_rime_bricks"}, R + "cracked_rime_bricks_slab", 6)
    r_shaped("cracked_rime_bricks_wall", ["BBB", "BBB"], {"B": R + "cracked_rime_bricks"}, R + "cracked_rime_bricks_wall", 6)
    r_shaped("rime_brick_pillar", ["B", "B"], {"B": R + "cracked_rime_bricks"}, R + "rime_brick_pillar", 2)
    r_shaped("fog_lantern", ["III", "ITI", "III"],
             {"I": "minecraft:iron_ingot", "T": "minecraft:torch"}, R + "fog_lantern")
    n += 10
    # «ледяной клык» и «колючник» — из своих же материалов (практические крафты)
    r_shaped("ice_fang", ["P", "P"], {"P": "minecraft:packed_ice"}, R + "ice_fang", 2)
    r_shapeless("bone_bramble", ["minecraft:bone_meal", "minecraft:stick"], R + "bone_bramble", 2)
    n += 2
    return n


# ---------------------------------------------------------------- lang
def add_lang():
    names = {
        "morok_log": ("Бледное бревно", "Pale Log"),
        "stripped_morok_log": ("Обтёсанное бледное бревно", "Stripped Pale Log"),
        "morok_wood": ("Бледная древесина", "Pale Wood"),
        "stripped_morok_wood": ("Обтёсанная бледная древесина", "Stripped Pale Wood"),
        "morok_planks": ("Бледные доски", "Pale Planks"),
        "morok_planks_stairs": ("Бледные ступени", "Pale Stairs"),
        "morok_planks_slab": ("Бледная плита", "Pale Slab"),
        "morok_fence": ("Бледный забор", "Pale Fence"),
        "morok_fence_gate": ("Бледная калитка", "Pale Fence Gate"),
        "morok_door": ("Бледная дверь", "Pale Door"),
        "morok_trapdoor": ("Бледный люк", "Pale Trapdoor"),
        "morok_pressure_plate": ("Бледная нажимная плита", "Pale Pressure Plate"),
        "morok_button": ("Бледная кнопка", "Pale Button"),
        "morok_leaves": ("Хвоя в изморози", "Rime Needles"),
        "morok_sapling": ("Бледный росток", "Pale Sapling"),
        "frozen_soil": ("Мёрзлая земля", "Frozen Soil"),
        "rime_grass": ("Замшелая мерзлота", "Rime Turf"),
        "pale_moss_block": ("Пепельный мох", "Pale Moss Block"),
        "pale_moss_carpet": ("Пепельный ковёр", "Pale Moss Carpet"),
        "ice_fang": ("Ледяной клык", "Ice Fang"),
        "bone_bramble": ("Костяной колючник", "Bone Bramble"),
        "cracked_rime_bricks": ("Треснувший морозный кирпич", "Cracked Rime Bricks"),
        "cracked_rime_bricks_stairs": ("Морозные ступени", "Rime Brick Stairs"),
        "cracked_rime_bricks_slab": ("Морозная плита", "Rime Brick Slab"),
        "cracked_rime_bricks_wall": ("Морозная ограда", "Rime Brick Wall"),
        "rime_brick_pillar": ("Морозная колонна", "Rime Brick Pillar"),
        "fog_lantern": ("Туманный фонарь", "Fog Lantern"),
    }
    for path, idx in ((ASSETS / "lang/ru_ru.json", 0), (ASSETS / "lang/en_us.json", 1)):
        data = json.loads(path.read_text(encoding="utf-8"))
        for bid, pair in names.items():
            data[f"block.riftworld.{bid}"] = pair[idx]
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return len(names)


BLOCK_IDS = [
    "morok_log", "stripped_morok_log", "morok_wood", "stripped_morok_wood",
    "morok_planks", "morok_planks_stairs", "morok_planks_slab", "morok_fence", "morok_fence_gate",
    "morok_door", "morok_trapdoor", "morok_pressure_plate", "morok_button",
    "morok_leaves", "morok_sapling",
    "frozen_soil", "rime_grass", "pale_moss_block", "pale_moss_carpet", "ice_fang", "bone_bramble",
    "cracked_rime_bricks", "cracked_rime_bricks_stairs", "cracked_rime_bricks_slab",
    "cracked_rime_bricks_wall", "rime_brick_pillar", "fog_lantern",
]

PREFIXES = ("morok_", "stripped_morok_", "frozen_soil", "rime_grass", "rime_brick", "pale_moss_",
            "ice_fang", "bone_bramble", "cracked_rime_", "fog_lantern")


def cleanup():
    """Удаляет файлы клонов, чей id не входит в список блоков биома."""
    removed = 0
    for base in (BS, MB, MI, LOOT):
        for f in base.glob("*.json"):
            stem = f.stem
            if not stem.startswith(PREFIXES):
                continue
            # модель принадлежит блоку, если её имя начинается с одного из id
            if not any(stem == bid or stem.startswith(bid + "_") or stem.startswith(bid) for bid in BLOCK_IDS):
                f.unlink()
                removed += 1
            elif not any(stem == bid or stem.startswith(bid) for bid in BLOCK_IDS):
                f.unlink()
                removed += 1
    # отдельно: *mossy варианты морозного кирпича не нужны
    for base in (BS, MB, MI, LOOT):
        for f in base.glob("cracked_rime_bricks_mossy*.json"):
            f.unlink()
            removed += 1
    return removed


TAGS = {
    "logs": ["morok_log", "morok_wood", "stripped_morok_log", "stripped_morok_wood"],
    "logs_that_burn": ["morok_log", "morok_wood", "stripped_morok_log", "stripped_morok_wood"],
    "planks": ["morok_planks"],
    "leaves": ["morok_leaves"],
    "saplings": ["morok_sapling"],
    "stairs": ["morok_planks_stairs", "cracked_rime_bricks_stairs"],
    "slabs": ["morok_planks_slab", "cracked_rime_bricks_slab"],
    "walls": ["cracked_rime_bricks_wall"],
    "fences": ["morok_fence"],
    "fence_gates": ["morok_fence_gate"],
    "doors": ["morok_door"],
    "trapdoors": ["morok_trapdoor"],
    "pressure_plates": ["morok_pressure_plate"],
    "buttons": ["morok_button"],
    "wooden_stairs": ["morok_planks_stairs"],
    "wooden_slabs": ["morok_planks_slab"],
    "wooden_fences": ["morok_fence"],
    "wooden_doors": ["morok_door"],
    "wooden_trapdoors": ["morok_trapdoor"],
    "wooden_pressure_plates": ["morok_pressure_plate"],
    "wooden_buttons": ["morok_button"],
    "mineable/axe": ["morok_log", "morok_wood", "stripped_morok_log", "stripped_morok_wood",
                     "morok_planks", "morok_planks_stairs", "morok_planks_slab", "morok_fence",
                     "morok_fence_gate", "morok_door", "morok_trapdoor", "morok_pressure_plate",
                     "morok_button", "pale_moss_carpet", "fog_lantern"],
    "mineable/pickaxe": ["cracked_rime_bricks", "cracked_rime_bricks_stairs", "cracked_rime_bricks_slab",
                         "cracked_rime_bricks_wall", "rime_brick_pillar", "ice_fang"],
    "mineable/shovel": ["frozen_soil", "rime_grass", "pale_moss_block", "pale_moss_carpet"],
}


def gen_tags():
    """Дописывает блоки биома в ванильные теги (скорость копания, звук, крафты)."""
    added = 0
    for tag, ids in TAGS.items():
        path = DATA / f"minecraft/tags/blocks/{tag}.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        if path.exists():
            data = json.loads(path.read_text(encoding="utf-8"))
            values = data.setdefault("values", [])
        else:
            data, values = {"replace": False, "values": []}, []
            data["values"] = values
        for bid in ids:
            entry = f"riftworld:{bid}"
            if entry not in values:
                values.append(entry)
                added += 1
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return added


def fix_grass_bottom():
    """У клона rime_grass нижняя грань должна быть мёрзлой землёй, а не элисарской почвой."""
    f = MB / "rime_grass.json"
    if f.exists():
        f.write_text(f.read_text(encoding="utf-8").replace("elisar_soil", "frozen_soil"), encoding="utf-8")


def main():
    print("== textures ==")
    tex = gen_textures()
    print(f"  текстур: {len(tex)}")

    print("== blockstates / модели / лут ==")
    sets = [
        ("sizo", "morok"),
        ("stripped_sizo_log", "stripped_morok_log"),
        ("stripped_sizo_wood", "stripped_morok_wood"),
        ("elisar_soil", "frozen_soil"),
        ("elisar_grass", "rime_grass"),
        ("elisar_moss", "pale_moss_block"),
        ("castle_carpet", "pale_moss_carpet"),
        ("celadon_fern", "bone_bramble"),
        ("celadon_fern", "ice_fang"),
        ("celadon_fern", "morok_sapling"),
        ("elisar_lantern", "fog_lantern"),
        ("pearl_calcite_bricks", "cracked_rime_bricks"),
        ("ivory_marble_pillar", "rime_brick_pillar"),
        ("elisar_bricks", "cracked_rime_bricks"),
    ]
    total = 0
    for src, dst in sets:
        n = clone_set(src, dst)
        total += n
        print(f"  {src} -> {dst}: {n}")
    print(f"  всего файлов: {total}")

    print("== очистка лишних клонов ==")
    print(f"  удалено: {cleanup()}")

    print("== теги ==")
    print(f"  записей добавлено: {gen_tags()}")
    fix_grass_bottom()

    print("== крафты ==")
    print(f"  рецептов: {gen_recipes()}")
    print("== lang ==")
    print(f"  записей: {add_lang()}")


if __name__ == "__main__":
    main()
