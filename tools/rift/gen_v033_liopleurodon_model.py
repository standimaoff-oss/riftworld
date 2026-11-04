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

ATLAS = 512
PAD = 1

# палитра (как в референсе и в концепте)
BACK_DARK = "#39485A"
BACK = "#4A5D6E"
SIDE = "#5A6E7D"
BELLY = "#9DABB4"
BELLY_LIGHT = "#BAC6CC"
SCAR = "#C6B79A"
SPOT = "#2B3B47"
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


# --- v0.38.4: НАСТОЯЩИЙ плиозавр ----------------------------------------------------------
#     Единицы: 1/16 блока. Нос смотрит в -Z, Y растёт ВНИЗ (спина — отрицательный Y).
#     Пропорции взяты по скелетам Liopleurodon: голова ≈ 28 % длины, короткая толстая шея,
#     бочкообразный торс, короткий хвост с вертикальным плавником и четыре веслообразных
#     ласта, СМЕЩЁННЫХ назад и вниз (прошлая версия растопыривала их в одной плоскости —
#     сверху животное читалось как самолёт).
#     Круглое сечение имитируется тремя ярусами коробок (узкая спина / широкий бок / узкое
#     брюхо): так силуэт не выглядит «доской».

# --- торс: три сегмента по длине, у каждого три яруса по высоте ---------------------------
bone("body", [0, 0, -20])
# грудь (z -44..-18) — самый широкий участок
cube([-9, -17, -44], [18, 8, 26], "back")           # спина
cube([-14, -9, -44], [28, 15, 26], "back")          # бока, максимальная ширина
cube([-11, 6, -44], [22, 10, 26], "belly")          # брюхо
# брюхо/середина (z -18..+8)
cube([-8, -16, -18], [16, 7, 26], "back")
cube([-13, -8, -18], [26, 14, 26], "back")
cube([-10, 6, -18], [20, 9, 26], "belly")
# поясница, сужение к хвосту (z +8..+24)
cube([-6, -13, 8], [12, 6, 16], "back")
cube([-10, -7, 8], [20, 12, 16], "back")
cube([-8, 5, 8], [16, 8, 16], "belly")
# низкий спинной киль
cube([-2, -19, -34], [4, 3, 18], "dorsal_ridge")

# --- шея: короткая и толстая (плиозавр — не плезиозавр) ----------------------------------
bone("neck", [0, 0, -46], {"axis": "y", "amp": 2.0, "speed": 0.9, "phase": 0.4})
cube([-11, -13, -60], [22, 26, 18], "back")         # шея-воротник
cube([-8, -16, -58], [16, 5, 14], "back")           # загривок

# --- голова: ЧЕТВЕРТЬ длины тела, длинные челюсти с полным оскалом -----------------------
#     +Y вниз, поэтому верхняя челюсть лежит при меньшем Y, чем нижняя;
#     между ними оставлен просвет 4 ед. и тёмное нёбо, чтобы пасть читалась сбоку.
bone("head", [0, 0, -60], {"axis": "y", "amp": 2.6, "speed": 1.1, "phase": 1.0})
cube([-13, -12, -76], [26, 23, 18], "back")         # череп
cube([-11, -12, -64], [22, 20, 8], "back")          # затылок
cube([-11, -6, -92], [22, 10, 18], "back")          # верхняя челюсть
cube([-9, -5, -99], [18, 9, 8], "back")             # кончик морды (розетка зубов)
cube([-10, 3, -93], [20, 2, 19], "gum")             # нёбо (тёмная полоса пасти)
cube([-9, 5, -95], [18, 9, 27], "belly")            # нижняя челюсть
cube([-7, 5, -101], [14, 8, 7], "belly")            # подбородок
# оскал: верхние клыки висят из челюсти, нижние торчат вверх — по НАРУЖНОЙ кромке
for zz in range(-97, -70, 4):
    for sx in (-1, 1):
        cube([sx * 11 - (1 if sx > 0 else 0), 3, zz], [1, 5, 2], "tooth")
for zz in range(-95, -70, 5):
    for sx in (-1, 1):
        cube([sx * 9 - (1 if sx > 0 else 0), 1, zz], [1, 4, 2], "tooth")
cube([-14, -8, -72], [2, 3, 12], "side_fin")        # надбровные гребни
cube([12, -8, -72], [2, 3, 12], "side_fin")
cube([-13, -6, -70], [1, 4, 4], "eye")              # глаза высоко на черепе
cube([12, -6, -70], [1, 4, 4], "eye")
cube([-4, -7, -98], [2, 2, 3], "spot")              # ноздри на кончике морды
cube([2, -7, -98], [2, 2, 3], "spot")

# --- передние ласты: длинные вёсла, СМЕЩЁННЫЕ назад и вниз --------------------------------
for name, sx, ph in (("fin_fr", -1, 0.0), ("fin_fl", 1, 0.0)):
    bone(name, [sx * 13, -2, -30], {"axis": "z", "amp": sx * 12.0, "speed": 1.3, "phase": ph})
    cube([min(sx * 12, sx * 30), -4, -38], [18, 11, 20], "fin")
    cube([min(sx * 30, sx * 44), -2, -30], [14, 9, 18], "fin_edge")
    cube([min(sx * 44, sx * 56), 0, -22], [12, 7, 14], "fin_edge")

# --- задние ласты: короче передних (у плиозавров гребут в основном передние) --------------
for name, sx, ph in (("fin_br", -1, 0.9), ("fin_bl", 1, 0.9)):
    bone(name, [sx * 11, -2, 14], {"axis": "z", "amp": sx * 9.0, "speed": 1.15, "phase": ph})
    cube([min(sx * 10, sx * 26), -3, 8], [16, 9, 16], "fin")
    cube([min(sx * 26, sx * 38), -1, 14], [12, 7, 14], "fin_edge")
    cube([min(sx * 38, sx * 46), 1, 20], [8, 5, 10], "fin_edge")

# --- хвост: короткий, с вертикальным плавником -------------------------------------------
bone("tail1", [0, 0, 24], {"axis": "y", "amp": 5.0, "speed": 1.2, "phase": 0.2})
cube([-7, -11, 24], [14, 20, 14], "back")
bone("tail2", [0, 0, 38], {"axis": "y", "amp": 8.0, "speed": 1.2, "phase": 1.4})
cube([-5, -10, 38], [10, 18, 12], "back")
bone("tail3", [0, 0, 50], {"axis": "y", "amp": 11.0, "speed": 1.2, "phase": 2.4})
cube([-3, -8, 50], [6, 15, 10], "back")
cube([-2, -22, 54], [4, 15, 8], "fin_edge")         # верхняя лопасть
cube([-2, 4, 54], [4, 9, 7], "fin_edge")            # нижняя лопасть


MATS = {
    "back": (BACK, SIDE, BACK_DARK, BELY_PLACEHOLDER := BELLY),
    "dorsal_ridge": (SIDE, BACK, BACK_DARK, BELLY),
    "side_fin": (SIDE, BACK_DARK, BACK_DARK, BELLY),
    # v0.38.4: бока брюшных коробок больше не белые — светлым остаётся только низ
    "belly": (BELLY, "#7C8B96", "#7C8B96", BELLY_LIGHT),
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
    """Упаковка UV-островков «небесной линией» (skyline): ищем самое низкое место.

    Полковая упаковка из v0.33.0 теряла до 40% атласа на высоких коробках и
    после утолщения торса перестала влезать в 256x256. Skyline кладёт каждую
    островку в первую подходящую низину — атлас заполняется плотно, без наложений.
    """
    items = []
    for cb in cubes:
        w, h, d = cb["size"]
        fw, fh = footprint(w, h, d)
        items.append((cb, fw + 2 * PAD, fh + 2 * PAD))
    items.sort(key=lambda t: (-t[2], -t[1]))
    sky = [0] * ATLAS
    placed = []
    for cb, fw, fh in items:
        best_x, best_y = -1, 1 << 30
        for x in range(0, ATLAS - fw + 1):
            y = max(sky[x:x + fw])
            if y + fh <= ATLAS and y < best_y:
                best_x, best_y = x, y
                if y == 0:
                    break
        if best_x < 0:
            raise SystemExit("атлас переполнен: уменьшите детализацию или увеличьте атлас")
        for x in range(best_x, best_x + fw):
            sky[x] = best_y + fh
        placed.append((cb, best_x + PAD, best_y + PAD))
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
    render_scale = round(8.6 / (length_units / 16.0), 3)       # ~8.6 блока в длину: левиафан, но влезает в кадр

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
