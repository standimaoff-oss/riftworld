#!/usr/bin/env python3
"""
v0.49.0 — «ванильные» текстуры предметов.

Пользователь попросил:
  * большинство текстур предметов — в ванильном стиле;
  * ведро элисарской воды — обычная ванильная текстура ведра, но в ДРУГОМ цвете;
  * Riftcrystal — изумрудная текстура.

Подход: берём ОРИГИНАЛЬНЫЕ ванильные текстуры 1.20.1 (Minecraft-assets) и
перекрашиваем их по яркости в фирменные цвета мода. Так форма, свет, блики и
растр остаются ванильными — предметы перестают выглядеть «нарисованными».

Всё, что перекрашивается, читается из локального кэша ванильных ассетов:
  ~/.cache/riftworld/vanilla-assets/  (см. fetch_vanilla_assets.py)

Запуск: python3 tools/rift/gen_v049_item_textures.py
"""
import json
import pathlib
import shutil
import struct
import sys
import zlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
ASSETS = ROOT / "mod/src/main/resources/assets/riftworld"
TEX = ASSETS / "textures/item"
CACHE = pathlib.Path.home() / ".cache/riftworld/vanilla-assets"

# ---------------------------------------------------------------- PNG helpers
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


def hx(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def lum(px):
    return 0.299 * px[0] + 0.587 * px[1] + 0.114 * px[2]


def recolor(pixels, stops):
    """Перекраска по яркости: stops = [(0.0, '#000'), (0.5, '#888'), (1.0, '#fff')]."""
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


def tint(pixels, colour, amount=0.65):
    """Лёгкий цветной тон поверх (для «другого цвета» ведра)."""
    r0, g0, b0 = hx(colour)
    out = []
    for row in pixels:
        nrow = []
        for (r, g, b, a) in row:
            nrow.append((int(r + (r0 - r) * amount),
                         int(g + (g0 - g) * amount),
                         int(b + (b0 - b) * amount),
                         a))
        out.append(nrow)
    return out


def upscale(pixels, factor):
    """Nearest-neighbour увеличение (для модельных текстур 16x16 -> 32x32 и т.п.)."""
    out = []
    for row in pixels:
        big = []
        for px in row:
            big.extend([px] * factor)
        for _ in range(factor):
            out.append(list(big))
    return out


def vanilla(name):
    # Кэш: ~/.cache/riftworld/vanilla-assets/assets/minecraft/textures/item/<name>.png
    p = CACHE / "assets/minecraft/textures/item" / (name if name.endswith(".png") else name + ".png")
    if not p.exists():
        return None
    # Часть ванильных текстур — индексированные PNG (палитра) или серые; PIL
    # приводит их к RGBA одинаково, а наш собственный reader — нет.
    try:
        from PIL import Image
        im = Image.open(p).convert("RGBA")
        w, h = im.size
        return [[im.getpixel((x, y)) for x in range(w)] for y in range(h)]
    except Exception:  # noqa: BLE001
        return read_png(p)


# ---------------------------------------------------------------- palettes
AURITE = [(0.0, "#3A2C14"), (0.40, "#7A5C24"), (0.75, "#C9A24A"), (1.0, "#F2D98A")]
RIFT = [(0.0, "#0B2418"), (0.40, "#145438"), (0.75, "#2C9A62"), (1.0, "#8CE8B4")]
ELISAR = [(0.0, "#16304A"), (0.40, "#2E6795"), (0.75, "#5FA8D8"), (1.0, "#BEE6FA")]
SHADOW = [(0.0, "#101216"), (0.42, "#262A32"), (0.78, "#3E4552"), (1.0, "#626C7E")]

# ---------------------------------------------------------------- jobs
# (имя нашей текстуры, ванильный источник, палитра, доп. обработка)
JOBS = [
    # --- инструменты и оружие: берём ВАНИЛЬНЫЙ инструмент и красим в цвет мода
    ("aurite_axe", "iron_axe.png", AURITE, None),
    ("aurite_pickaxe", "iron_pickaxe.png", AURITE, None),
    ("aurite_shovel", "iron_shovel.png", AURITE, None),
    ("aurite_hoe", "iron_hoe.png", AURITE, None),
    ("aurite_sword", "iron_sword.png", AURITE, None),
    ("aurite_hammer", "iron_axe.png", AURITE, None),
    ("riftcrystal_axe", "diamond_axe.png", RIFT, None),
    ("riftcrystal_pickaxe", "diamond_pickaxe.png", RIFT, None),
    ("riftcrystal_shovel", "diamond_shovel.png", RIFT, None),
    ("riftcrystal_hoe", "diamond_hoe.png", RIFT, None),
    ("riftcrystal_sword", "diamond_sword.png", RIFT, None),
    ("shadowsteel_axe", "netherite_axe.png", SHADOW, None),
    ("shadowsteel_pickaxe", "netherite_pickaxe.png", SHADOW, None),
    ("shadowsteel_shovel", "netherite_shovel.png", SHADOW, None),
    ("shadowsteel_hoe", "netherite_hoe.png", SHADOW, None),
    ("shadowsteel_sword", "netherite_sword.png", SHADOW, None),
    # --- слитки/порошки
    ("aurite_ingot", "iron_ingot.png", AURITE, None),
    ("riftcrystal_shard", "diamond.png", RIFT, None),
    ("riftcrystal_dust", "gunpowder.png", RIFT, None),
    ("shadowsteel_ingot", "netherite_ingot.png", SHADOW, None),
    # --- еда/материалы
    ("morok_ember", "blaze_powder.png", [(0.0, "#2A0C04"), (0.45, "#8C2A08"), (0.8, "#E05A18"), (1.0, "#FFC46A")], None),
    ("sea_scale", "prismarine_shard.png", [(0.0, "#16303A"), (0.45, "#2E6070"), (0.8, "#6FB2BE"), (1.0, "#CFF2F6")], None),
    ("abyssal_ink_sac", "ink_sac.png", [(0.0, "#080A12"), (0.45, "#161A2A"), (0.8, "#2A3048"), (1.0, "#5A6490")], None),
    ("basalt_scute", "netherite_scrap.png", [(0.0, "#1A1A1E"), (0.45, "#3A3A42"), (0.8, "#5E5E68"), (1.0, "#8E8E9A")], None),
    ("ash_wing_dust", "glowstone_dust.png", [(0.0, "#3A3418"), (0.45, "#7E7326"), (0.8, "#C8B84A"), (1.0, "#FFF0A0")], None),
    ("porcelain_feather", "feather.png", [(0.0, "#8A8C86"), (0.45, "#C8CAC2"), (0.8, "#EDEFE8"), (1.0, "#FFFFFF")], None),
    ("phantom_fang", "bone.png", [(0.0, "#1A1424"), (0.45, "#3E3458"), (0.8, "#6A5C8E"), (1.0, "#B0A4D4")], None),
    ("liopleurodon_tooth", "prismarine_crystals.png", [(0.0, "#143244"), (0.45, "#2E6C8A"), (0.8, "#62AEC8"), (1.0, "#C8EEF8")], None),
    # --- зелья/эликсиры
    ("elisar_tonic", "potion.png", ELISAR, None),
]

# Ведро элисарской воды: обычная ванильная ТЕКСТУРА ведра, но в другом цвете.
BUCKET_TINT = "#3E8FD0"

# Riftcrystal: изумрудная текстура — берём ванильный изумруд и красим в его тон.
EMERALD_TINT = [(0.0, "#0A3A22"), (0.40, "#187A48"), (0.75, "#3FC77A"), (1.0, "#9CFFC0")]


def is_voxel_item(name: str) -> bool:
    """3D-модели (инструменты, броня) используют палитру; PNG у них — только
    particle-ссылка и должен оставаться 32x32. Перекрашивать их нельзя."""
    mp = ASSETS / "models/item" / f"{name}.json"
    if not mp.exists():
        return False
    try:
        d = json.loads(mp.read_text(encoding="utf-8"))
    except Exception:
        return False
    return "elements" in d or d.get("parent") == "minecraft:item/generated" and False


def main():
    if not CACHE.exists():
        print("!! нет кэша ванильных ассетов:", CACHE)
        print("   скачай ассеты Minecraft 1.20.1 в", CACHE)
        return 1
    made, skipped = [], []
    for name, src, stops, extra in JOBS:
        if is_voxel_item(name):
            # 3D-модель: PNG — только particle-ссылка, трогать нельзя
            skipped.append((name, "3d voxel model"))
            continue
        px = vanilla(src)
        if px is None:
            skipped.append((name, src))
            continue
        px = recolor(px, stops)
        write_png(TEX / f"{name}.png", px)
        made.append(name)

    # --- ведро элисарской воды
    bucket = vanilla("bucket.png")
    if bucket is not None:
        px = tint(bucket, BUCKET_TINT, 0.72)
        write_png(TEX / "elisar_water_bucket.png", px)
        made.append("elisar_water_bucket")
    else:
        skipped.append(("elisar_water_bucket", "bucket.png"))

    # --- ведро морского конька (тоже обычная форма ведра, другой цвет)
    if bucket is not None:
        px = tint(bucket, "#E0763C", 0.70)
        write_png(TEX / "reef_seahorse_bucket.png", px)
        made.append("reef_seahorse_bucket")

    # --- Riftcrystal: изумрудная текстура
    em = vanilla("emerald.png")
    if em is not None and not is_voxel_item("riftcrystal"):
        px = recolor(em, EMERALD_TINT)
        write_png(TEX / "riftcrystal.png", px)
        made.append("riftcrystal")
    else:
        skipped.append(("riftcrystal", "emerald.png"))

    print(f"OK: перекрашено {len(made)} текстур предметов")
    if skipped:
        print("   нет ванильного источника:", skipped)
    return 0


if __name__ == "__main__":
    sys.exit(main())
