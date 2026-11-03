#!/usr/bin/env python3
"""
v0.32.0 — Лиоплевродон ({@code riftworld:liopleurodon}).

Делает по референсу `reports/mob_refs/03_ref_liopleurodon.png`:
  * блочная модель из кубоидов в ванильном стиле (geo-формат мода, как у рыб)
    — длинная зубастая морда, четыре ласта, хвост с плавником, изгиб тела;
  * текстура 256x256, нарисованная по РЕАЛЬНОЙ UV-раскладке кубоидов
    (тёмно-синяя спина, светлое брюхо, шрамы, пятна, зубы, глаза);
  * предметные текстуры: зуб, чешуя.
"""
import json
import pathlib
import struct
import zlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
ASSETS = ROOT / "mod/src/main/resources/assets/riftworld"
GEO = ASSETS / "geo/liopleurodon.json"
TEX_ENTITY = ASSETS / "textures/entity/liopleurodon.png"
TEX_ITEM = ASSETS / "textures/item"
MODELS_ITEM = ASSETS / "models/item"
LANG = (ASSETS / "lang/ru_ru.json", ASSETS / "lang/en_us.json")

TW = TH = 256

BACK = (38, 49, 60)
BACK_DARK = (24, 31, 39)
FLANK = (58, 72, 84)
BELLY = (196, 197, 190)
BELLY_DARK = (150, 154, 152)
SCAR = (208, 214, 212)
TOOTH = (238, 232, 214)
GUM = (120, 86, 84)
EYE = (18, 18, 20)


# ---------------------------------------------------------------- PNG
def write_png(path: pathlib.Path, px):
    h, w = len(px), len(px[0])
    raw = bytearray()
    for row in px:
        raw.append(0)
        for (r, g, b, a) in row:
            raw += bytes((r, g, b, a))

    def chunk(tag, payload):
        return struct.pack(">I", len(payload)) + tag + payload + struct.pack(">I", zlib.crc32(tag + payload) & 0xFFFFFFFF)

    out = b"\x89PNG\r\n\x1a\n"
    out += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    out += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    out += chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(out)


def rnd(seed):
    state = seed & 0xFFFFFFFF

    def nxt():
        nonlocal state
        state = (1103515245 * state + 12345) & 0x7FFFFFFF
        return state / 0x7FFFFFFF
    return nxt


# ---------------------------------------------------------------- модель
# Кость: (имя, родитель, pivot, кубы) — размеры в пикселях (1 px = 1/16 блока), нос смотрит в -Z.
# Кубы: (dx, dy, dz, w, h, d) — смещение от pivot кости и размер.
def build_model():
    bones = []

    def bone(name, parent, pivot, cubes, anim=None):
        b = {"name": name, "pivot": list(pivot)}
        if parent:
            b["parent"] = parent
        if anim:
            b["anim"] = anim
        b["cubes"] = []
        for (dx, dy, dz, w, h, d, tag) in cubes:
            b["cubes"].append({"origin": [pivot[0] + dx, pivot[1] + dy, pivot[2] + dz],
                               "size": [w, h, d], "_tag": tag})
        bones.append(b)

    # тело (торс): от -40 до +8 по Z, 20 в ширину, 18 в высоту
    bone("body", None, (0, 0, -16), [
        (-10, -9, -24, 20, 18, 48, "body"),
        (-10, 3, -24, 20, 2, 48, "body_top"),
    ])
    # шея: сужается к голове
    bone("neck", "body", (0, 0, -32), [
        (-8, -7, -14, 16, 14, 14, "neck"),
    ], {"axis": "y", "amp": 3.0, "speed": 1.1, "phase": 0.4})
    # голова: длинная крокодилья морда + нижняя челюсть + глаза
    bone("head", "neck", (0, 0, -46), [
        (-7, -6, -18, 14, 11, 18, "head"),
        (-6, -4, -34, 12, 7, 16, "snout"),
        (-6, -7, -20, 12, 2, 20, "skull_top"),
        (-5, -7, -33, 10, 2, 13, "teeth_upper"),
        (-11, -2, -17, 4, 5, 5, "jaw_hinge"),
        (7, -2, -17, 4, 5, 5, "jaw_hinge2"),
        (-6, -9, -32, 12, 3, 12, "jaw_lower"),
        (-5, -8, -32, 10, 1, 11, "teeth_lower"),
        (-8, -1, -14, 2, 3, 4, "eye"),
        (6, -1, -14, 2, 3, 4, "eye2"),
    ], {"axis": "y", "amp": 4.0, "speed": 1.3, "phase": 1.1})
    # четыре ласта
    bone("fin_fr", "body", (-10, 0, -18), [
        (-20, -3, -6, 20, 4, 10, "fin"),
        (-30, -3, -9, 10, 3, 7, "fin_tip"),
    ], {"axis": "z", "amp": 9.0, "speed": 1.6, "phase": 0.0})
    bone("fin_fl", "body", (10, 0, -18), [
        (-2, -3, -6, 20, 4, 10, "fin"),
        (18, -3, -9, 10, 3, 7, "fin_tip"),
    ], {"axis": "z", "amp": -9.0, "speed": 1.6, "phase": 0.0})
    bone("fin_br", "body", (-9, 0, 6), [
        (-16, -2, -5, 16, 3, 9, "fin"),
    ], {"axis": "z", "amp": 7.0, "speed": 1.5, "phase": 0.9})
    bone("fin_bl", "body", (9, 0, 6), [
        (0, -2, -5, 16, 3, 9, "fin"),
    ], {"axis": "z", "amp": -7.0, "speed": 1.5, "phase": 0.9})
    # хвост из трёх сегментов + вертикальный плавник
    bone("tail1", "body", (0, 0, 8), [
        (-9, -8, 0, 18, 16, 24, "tail"),
    ], {"axis": "y", "amp": 7.0, "speed": 1.5, "phase": 0.2})
    bone("tail2", "tail1", (0, 0, 24), [
        (-7, -6, 0, 14, 13, 22, "tail"),
    ], {"axis": "y", "amp": 9.0, "speed": 1.5, "phase": 1.4})
    bone("tail3", "tail2", (0, 0, 22), [
        (-5, -5, 0, 10, 10, 18, "tail"),
        (-1, 4, 6, 2, 9, 10, "fin_tail"),
    ], {"axis": "y", "amp": 12.0, "speed": 1.5, "phase": 2.4})

    return {"texture_width": TW, "texture_height": TH, "render_scale": 0.85, "translucent": False,
            "bones": bones}


# ---------------------------------------------------------------- UV-раскладка
def uv_blocks(model):
    """Раскладывает кубоиды по атласу полками и возвращает список (cube, (u,v), (w,h,d))."""
    x = y = 0
    shelf = 0
    out = []
    for b in model["bones"]:
        for c in b["cubes"]:
            w, h, d = c["size"]
            need_w, need_h = 2 * (w + d), d + h
            if x + need_w > TW:
                x = 0
                y += shelf + 1
                shelf = 0
            if y + need_h > TH:
                raise SystemExit(f"атлас переполнен на кубе {c}")
            c["uv"] = [x, y]
            out.append((c, (x, y), (w, h, d)))
            x += need_w + 1
            shelf = max(shelf, need_h)
    return out


def face_rects(u, v, w, h, d):
    """Прямоугольники граней в ванильной раскладке кубоида."""
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "east": (u, v + d, d, h),
        "north": (u + d, v + d, w, h),
        "west": (u + d + w, v + d, d, h),
        "south": (u + d + w + d, v + d, w, h),
    }


def paint_texture(model):
    px = [[(0, 0, 0, 0) for _ in range(TW)] for _ in range(TH)]

    def rect(x, y, w, h, col, jitter=0, seed=1):
        r = rnd(seed)
        for yy in range(y, min(TH, y + h)):
            for xx in range(x, min(TW, x + w)):
                j = int((r() - 0.5) * 2 * jitter)
                px[yy][xx] = (max(0, min(255, col[0] + j)), max(0, min(255, col[1] + j)),
                              max(0, min(255, col[2] + j)), 255)

    seed = 7
    for c, (u, v), (w, h, d) in uv_blocks(model):
        fr = face_rects(u, v, w, h, d)
        tag = c.get("_tag", "body")
        seed += 13
        # ВАЖНО: у моделей Minecraft ось Y направлена ВНИЗ, поэтому визуальный ВЕРХ кубоида
        # берёт UV-прямоугольник "bottom". Спина — тёмная, брюхо — светлое.
        rect(*fr["top"], BELLY, jitter=5, seed=seed)
        rect(*fr["bottom"], BACK_DARK if tag != "fin" else FLANK, jitter=6, seed=seed + 1)
        # бока: градиент от спины к брюху
        for name in ("east", "north", "west", "south"):
            fx, fy, fw, fh = fr[name]
            if tag == "eye":
                rect(fx, fy, fw, fh, EYE, jitter=3, seed=seed + 2)
                continue
            if tag == "eye2":
                rect(fx, fy, fw, fh, EYE, jitter=3, seed=seed + 3)
                continue
            if tag.startswith("teeth") or tag == "jaw_lower":
                rect(fx, fy, fw, fh, GUM, jitter=4, seed=seed + 4)
                for i in range(0, fw, 2):
                    rect(fx + i, fy, 1, max(1, fh // 2), TOOTH, jitter=3, seed=seed + i + 5)
                continue
            for row in range(fh):
                k = 1.0 - row / max(1, fh - 1)     # rect-строка 0 = визуальный низ
                if tag == "fin" or tag == "fin_tip" or tag == "fin_tail":
                    col = FLANK if k < 0.5 else BELLY_DARK
                else:
                    col = (int(BACK[0] + (FLANK[0] - BACK[0]) * k * 1.6),
                           int(BACK[1] + (FLANK[1] - BACK[1]) * k * 1.6),
                           int(BACK[2] + (FLANK[2] - BACK[2]) * k * 1.6))
                    if k > 0.62:
                        t = (k - 0.62) / 0.38
                        col = (int(col[0] + (BELLY[0] - col[0]) * t),
                               int(col[1] + (BELLY[1] - col[1]) * t),
                               int(col[2] + (BELLY[2] - col[2]) * t))
                rect(fx, fy + row, fw, 1, col, jitter=5, seed=seed + row * 7 + len(name))
        # шрамы на спине/боках
        r = rnd(seed * 3)
        for _ in range(max(1, (w * h) // 90)):
            sx = fr["north"][0] + int(r() * max(1, fr["north"][2]))
            sy = fr["north"][1] + int(r() * max(1, fr["north"][3]))
            ln = 2 + int(r() * 4)
            for i in range(ln):
                if 0 <= sx + i < TW and 0 <= sy < TH:
                    px[sy][sx + i] = (SCAR[0], SCAR[1], SCAR[2], 255)
    return px


# ---------------------------------------------------------------- предметы
def item_texture(path, draw):
    px = [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]
    draw(px)
    write_png(path, px)


def draw_tooth(px):
    body = (232, 226, 208)
    shade = (198, 191, 172)
    root = (150, 138, 120)
    # изогнутый клык: толстый корень слева-сверху, острие справа-снизу
    for i, (x, y, w, h) in enumerate([(3, 3, 5, 5), (6, 5, 5, 5), (9, 8, 4, 4), (12, 11, 2, 2)]):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                c = shade if (xx + yy) % 5 == 0 else body
                px[yy][xx] = (c[0], c[1], c[2], 255)
    for yy in range(3, 8):
        for xx in range(3, 5):
            px[yy][xx] = (root[0], root[1], root[2], 255)
    px[4][4] = (250, 247, 238, 255)


def draw_scale(px):
    base = (86, 116, 124)
    edge = (58, 84, 92)
    light = (128, 158, 164)
    for yy in range(3, 13):
        for xx in range(3, 13):
            c = base
            if xx in (3, 12) or yy in (3, 12):
                c = edge
            if (xx + yy) % 6 == 0:
                c = light
            px[yy][xx] = (c[0], c[1], c[2], 255)
    px[4][4] = (170, 196, 200, 255)


NAMES = {
    "entity.riftworld.liopleurodon": ("Лиоплевродон", "Liopleurodon"),
    "item.riftworld.liopleurodon_spawn_egg": ("Яйцо призыва: Лиоплевродон", "Liopleurodon Spawn Egg"),
    "item.riftworld.liopleurodon_tooth": ("Зуб лиоплевродона", "Liopleurodon Tooth"),
    "item.riftworld.sea_scale": ("Морская чешуя", "Sea Scale"),
}


def main():
    model = build_model()
    uv_blocks(model)
    for b in model["bones"]:
        for c in b["cubes"]:
            c.pop("_tag", None)
    GEO.parent.mkdir(parents=True, exist_ok=True)
    GEO.write_text(json.dumps(model, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")

    paint = build_model()
    uv_blocks(paint)
    write_png(TEX_ENTITY, paint_texture(paint))

    item_texture(TEX_ITEM / "liopleurodon_tooth.png", draw_tooth)
    item_texture(TEX_ITEM / "sea_scale.png", draw_scale)
    for item in ("liopleurodon_tooth", "sea_scale"):
        (MODELS_ITEM / f"{item}.json").write_text(json.dumps(
            {"parent": "minecraft:item/generated",
             "textures": {"layer0": f"riftworld:item/{item}"}}, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8")

    for path, idx in zip(LANG, (0, 1)):
        data = json.loads(path.read_text(encoding="utf-8"))
        for k, pair in NAMES.items():
            data[k] = pair[idx]
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")

    cubes = sum(len(b["cubes"]) for b in model["bones"])
    print(f"geo: костей {len(model['bones'])}, кубоидов {cubes} -> {GEO.relative_to(ROOT)}")
    print(f"текстура: {TEX_ENTITY.relative_to(ROOT)} {TW}x{TH}")


if __name__ == "__main__":
    main()
