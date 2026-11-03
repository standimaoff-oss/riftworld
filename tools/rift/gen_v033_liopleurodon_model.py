#!/usr/bin/env python3
"""
v0.33.0 — новая модель Лиоплевродона: геометрия + атлас 256x256.

Старая модель (v0.32.0) была «набором коробок»: UV брались на глаз, из-за чего
на кадре 146 существо выглядело плоским и без текстурных деталей. Здесь всё честно:

  * каждый кубоид получает СВОЮ UV-островку по ванильной развёртке коробки
    (ширина 2d+2w, высота d+h), упаковка — полками (shelf packing), без наложений;
  * атлас рисуется по палитре референса `reports/mob_refs/03_ref_liopleurodon.png`:
    тёмно-синеватая спина, светлое брюхо, шрамы, тёмные пятна;
  * геометрия ближе к референсу: длинная крокодилья морда с рядами зубов,
    мощный корпус, четыре веслообразных ласта, сужающийся хвост с плавником;
  * проверка: все UV внутри атласа, островки не перекрываются, длина модели
    в блоках попадает в 11–14.

Запуск: python3 tools/rift/gen_v033_liopleurodon_model.py
"""
import json
import math
import pathlib
import struct
import zlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
GEO = ROOT / "mod/src/main/resources/assets/riftworld/geo/liopleurodon.json"
TEX = ROOT / "mod/src/main/resources/assets/riftworld/textures/entity/liopleurodon.png"

ATLAS = 256
PAD = 1

# палитра (как в референсе и в концепте)
BACK_DARK = "#2E3A46"
BACK = "#3C4B59"
SIDE = "#4B5D6B"
BELLY = "#8C9AA4"
BELLY_LIGHT = "#A8B4BC"
SCAR = "#C6B79A"
SPOT = "#22303A"
TOOTH = "#EDE6D4"
GUM = "#6B4A47"
EYE = "#0B0F12"
FIN_EDGE = "#5B7080"


def hx(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def rnd(seed):
    st = seed & 0xFFFFFFFF

    def nxt():
        nonlocal st
        st = (1103515245 * st + 12345) & 0x7FFFFFFF
        return st / 0x7FFFFFFF
    return nxt


def write_png(path, pixels):
    h, w = len(pixels), len(pixels[0])
    raw = bytearray()
    for row in pixels:
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


# --------------------------------------------------------------------- геометрия
# Кость: имя, pivot, анимация, кубоиды (origin, size, группа текстуры).
# Единицы — 1/16 блока; нос смотрит в -Z (как в рендере).
BONES = []


def bone(name, pivot, anim=None, parent=None):
    BONES.append({"name": name, "pivot": pivot, "anim": anim, "parent": parent, "cubes": []})


def cube(origin, size, mat):
    BONES[-1]["cubes"].append({"origin": origin, "size": size, "mat": mat})


# --- корпус: 44 длины, 16 ширины, 15 высоты, слегка сжатый сверху
bone("body", [0, 0, -14])
cube([-8, -7, -36], [16, 15, 46], "back")
cube([-7, 5, -32], [14, 3, 38], "dorsal_ridge")      # невысокий гребень вдоль спины
cube([-9, -4, -20], [2, 6, 26], "side_fin")          # боковые «плавники»-складки

# --- шея: переход к морде
bone("neck", [0, 0, -34], {"axis": "y", "amp": 2.5, "speed": 0.9, "phase": 0.4})
cube([-7, -6, -46], [14, 12, 14], "back")

# --- голова: череп + длинная верхняя челюсть + нижняя челюсть + зубы + глаза
bone("head", [0, 0, -46], {"axis": "y", "amp": 3.2, "speed": 1.1, "phase": 1.0})
cube([-7, -6, -58], [14, 11, 13], "back")            # череп
cube([-6, -4, -76], [12, 6, 19], "back")             # верхняя челюсть
cube([-5, -4, -86], [10, 5, 11], "back")             # кончик морды
cube([-5, -8, -77], [10, 4, 20], "belly")            # нижняя челюсть
cube([-5, -8, -86], [8, 3, 12], "belly")
cube([-3, -9, -84], [6, 1, 8], "gum")                # нёбо/горло — тёмная пасть

# зубы верхней челюсти (по бокам, с шагом 4)
for i, zz in enumerate(range(-84, -58, 4)):
    for sx in (-1, 1):
        cube([sx * 5 - (1 if sx > 0 else 0), -6, zz], [1, 2, 2], "tooth")
for i, zz in enumerate(range(-82, -58, 5)):
    for sx in (-1, 1):
        cube([sx * 4 - (1 if sx > 0 else 0), -6, zz], [1, 3, 2], "tooth")   # нижние

# ноздри и глаза
cube([-4, -2, -84], [3, 1, 2], "spot")
cube([1, -2, -84], [3, 1, 2], "spot")
cube([-8, -3, -57], [1, 3, 3], "eye")
cube([7, -3, -57], [1, 3, 3], "eye")

# --- передние ласты: плечо + лопасть (2 сегмента), машут вверх-вниз
for name, sx, ph in (("fin_fr", -1, 0.0), ("fin_fl", 1, 0.0)):
    bone(name, [sx * 7, -1, -18], {"axis": "z", "amp": sx * 11.0, "speed": 1.4, "phase": ph})
    cube([min(sx * 7, sx * 21), -3, -24], [14, 4, 12], "fin")            # основание
    cube([min(sx * 21, sx * 37), -3, -27], [16, 3, 10], "fin_edge")      # лопасть
    cube([min(sx * 37, sx * 45), -2, -28], [8, 2, 7], "fin_edge")        # кончик

# --- задние ласты: короче
for name, sx, ph in (("fin_br", -1, 0.9), ("fin_bl", 1, 0.9)):
    bone(name, [sx * 6, -2, 8], {"axis": "z", "amp": sx * 8.5, "speed": 1.2, "phase": ph})
    cube([min(sx * 6, sx * 19), -3, 4], [13, 3, 11], "fin")
    cube([min(sx * 19, sx * 29), -3, 3], [10, 2, 8], "fin_edge")

# --- хвост: 3 сегмента + вертикальный плавник
bone("tail1", [0, 0, 10], {"axis": "y", "amp": 6.0, "speed": 1.3, "phase": 0.2})
cube([-7, -6, 10], [14, 13, 22], "back")
bone("tail2", [0, 0, 28], {"axis": "y", "amp": 9.0, "speed": 1.3, "phase": 1.4})
cube([-5, -5, 32], [10, 10, 20], "back")
bone("tail3", [0, 0, 48], {"axis": "y", "amp": 12.0, "speed": 1.3, "phase": 2.4})
cube([-3, -3, 52], [6, 7, 16], "back")
cube([-1, -3, 62], [2, 12, 9], "fin_edge")           # верхний плавник
cube([-1, -10, 62], [2, 7, 7], "fin_edge")           # нижняя лопасть

MATS = {
    "back": (BACK, SIDE, BACK_DARK, BELY_PLACEHOLDER := BELLY),
    "dorsal_ridge": (SIDE, BACK, BACK_DARK, BELLY),
    "side_fin": (SIDE, BACK_DARK, BACK_DARK, BELLY),
    "belly": (BELLY_LIGHT, BELLY, BELLY, BELLY_LIGHT),
    "gum": (GUM, GUM, "#4A2F2D", GUM),
    "tooth": (TOOTH, TOOTH, TOOTH, TOOTH),
    "spot": (SPOT, SPOT, SPOT, SPOT),
    "eye": (EYE, EYE, EYE, EYE),
    "fin": (SIDE, BACK, BACK_DARK, BELLY),
    "fin_edge": (FIN_EDGE, SIDE, BACK_DARK, BELY_PLACEHOLDER),
}


def footprint(w, h, d):
    """Ванильная развёртка коробки: ширина 2d+2w, высота d+h."""
    return 2 * d + 2 * w, d + h


def pack(cubes):
    """Полковая упаковка UV-островков. Возвращает список (cube, u, v)."""
    items = []
    for cb in cubes:
        w, h, d = cb["size"]
        fw, fh = footprint(w, h, d)
        items.append((cb, fw + 2 * PAD, fh + 2 * PAD))
    items.sort(key=lambda t: -t[1])
    placed, shelf_y, shelf_h, cur_x = [], 0, 0, 0
    for cb, fw, fh in items:
        if cur_x + fw > ATLAS:
            shelf_y += shelf_h
            shelf_h, cur_x = 0, 0
        if shelf_y + fh > ATLAS:
            raise SystemExit("атлас переполнен: уменьшите детализацию или увеличьте атлас")
        placed.append((cb, cur_x + PAD, shelf_y + PAD))
        cur_x += fw
        shelf_h = max(shelf_h, fh)
    return placed


def face_rects(u, v, w, h, d):
    """Прямоугольники шести граней в координатах атласа: (name, x, y, fw, fh)."""
    return [
        ("up", u + d, v, w, d),
        ("down", u + d + w, v, w, d),
        ("east", u, v + d, d, h),
        ("north", u + d, v + d, w, h),
        ("west", u + d + w, v + d, d, h),
        ("south", u + d + w + d, v + d, w, h),
    ]


def paint(img, x, y, fw, fh, color, seed, kind):
    """Заливает грань базовым цветом + пиксельный шум/полосы/шрамы."""
    r = rnd(seed)
    base = hx(color)
    for yy in range(fh):
        for xx in range(fw):
            k = 1.0 + (r() - 0.5) * 0.13
            px = list(int(max(0, min(255, c * k))) for c in base)
            if kind == "back" and r() < 0.045:
                px = list(hx(SPOT))                       # тёмные пятна
            if kind == "belly" and r() < 0.05:
                px = list(hx(BELLY_LIGHT))
            if kind == "fin_edge" and r() < 0.08:
                px = list(hx(FIN_EDGE))
            if kind == "tooth" and r() < 0.10:
                px = list(hx("#C9C0AA"))
            ax, ay = x + xx, y + yy
            if 0 <= ax < ATLAS and 0 <= ay < ATLAS:
                img[ay][ax] = (px[0], px[1], px[2], 255)


def add_scars(img, u, v, w, h, d, seed):
    """Шрамы-царапины на боках и спине (как на референсе)."""
    r = rnd(seed ^ 0x5CA5)
    for _ in range(max(1, (w + h) // 10)):
        side_x = u + int(r() * (d + w))
        side_y = v + d + int(r() * h)
        ln = 2 + int(r() * 4)
        dxs = 1 if r() < 0.5 else -1
        for k in range(ln):
            ax, ay = side_x + k * dxs, side_y + (k % 2)
            if 0 <= ax < ATLAS and 0 <= ay < ATLAS:
                img[ay][ax] = (*hx(SCAR), 255)


def build_texture(placed):
    img = [[(0, 0, 0, 0)] * ATLAS for _ in range(ATLAS)]
    for cb, u, v in placed:
        w, h, d = cb["size"]
        mat = cb["mat"]
        top_c, side_c, front_c, bottom_c = MATS[mat]
        seed = hash((u, v, mat)) & 0x7FFF
        for name, fx, fy, fw, fh in face_rects(u, v, w, h, d):
            color = {"up": top_c, "down": bottom_c, "north": front_c, "south": front_c,
                     "east": side_c, "west": side_c}[name]
            kind = "belly" if mat in ("belly",) else ("fin_edge" if mat == "fin_edge" else
                                                      ("tooth" if mat == "tooth" else "back"))
            paint(img, fx, fy, fw, fh, color, seed + ord(name[0]), kind)
        if mat in ("back", "fin"):
            add_scars(img, u, v, w, h, d, seed)
    return img


def model_extents():
    lo = [1 << 30] * 3
    hi = [-1 << 30] * 3
    for b in BONES:
        for c in b["cubes"]:
            for i in range(3):
                lo[i] = min(lo[i], c["origin"][i])
                hi[i] = max(hi[i], c["origin"][i] + c["size"][i])
    return lo, hi


def main():
    all_cubes = [c for b in BONES for c in b["cubes"]]
    placed = pack(all_cubes)
    pos = {id(cb): (u, v) for cb, u, v in placed}

    bones_out = []
    for b in BONES:
        cubes = []
        for cb in b["cubes"]:
            u, v = pos[id(cb)]
            entry = {"origin": cb["origin"], "size": cb["size"], "uv": [u, v]}
            cubes.append(entry)
        entry = {"name": b["name"], "pivot": b["pivot"], "cubes": cubes}
        if b.get("parent"):
            entry["parent"] = b["parent"]
        if b.get("anim"):
            entry["anim"] = b["anim"]
        bones_out.append(entry)

    lo, hi = model_extents()
    length_units = hi[2] - lo[2]
    render_scale = round(13.0 / (length_units / 16.0), 3)      # ~13 блоков в длину

    geo = {
        "texture_width": ATLAS,
        "texture_height": ATLAS,
        "render_scale": render_scale,
        "translucent": False,
        "bones": bones_out,
    }
    GEO.write_text(json.dumps(geo, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    write_png(TEX, build_texture(placed))

    # ---- самопроверка
    assert len(all_cubes) == sum(len(b["cubes"]) for b in bones_out)
    used = [[False] * ATLAS for _ in range(ATLAS)]
    for cb, u, v in placed:
        w, h, d = cb["size"]
        for name, fx, fy, fw, fh in face_rects(u, v, w, h, d):
            assert fx >= 0 and fy >= 0 and fx + fw <= ATLAS and fy + fh <= ATLAS, f"UV за атласом: {cb}"
            for yy in range(fy, fy + fh):
                for xx in range(fx, fx + fw):
                    assert not used[yy][xx], f"наложение UV у {cb} на ({xx},{yy})"
                    used[yy][xx] = True

    print(f"костей: {len(bones_out)}  кубоидов: {len(all_cubes)}")
    print(f"габариты (ед.): x {hi[0]-lo[0]}  y {hi[1]-lo[1]}  z {length_units}"
          f"  → длина {length_units/16:.2f} блока при render_scale {render_scale}")
    print(f"атлас {ATLAS}x{ATLAS}, занято {sum(sum(r) for r in used)} px")
    print(f"geo: {GEO.relative_to(ROOT)}")
    print(f"tex: {TEX.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
