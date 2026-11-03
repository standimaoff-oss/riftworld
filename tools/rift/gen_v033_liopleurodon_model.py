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


# --- v0.38.0: корпус-«торпеда» плиозавра -------------------------------------------------
#     Единицы: 1/16 блока. Нос смотрит в -Z, y растёт вниз (спина отрицательная).
#     Ключевое отличие от v0.37.0: торс толще (24x30 против 18x24), хвост вдвое короче,
#     голова крупнее — силуэт читается как плиозавр, а не как «бревно с ластами».
bone("body", [0, 0, -20])
cube([-12, -15, -34], [24, 30, 14], "back")           # грудь
cube([-12, -16, -20], [24, 32, 14], "back")           # самая глубокая часть
cube([-10, -13, -6], [20, 27, 10], "back")            # поясница, сужение к хвосту
cube([-4, -18, -30], [8, 3, 10], "dorsal_ridge")      # низкий спинной киль
cube([-3, -19, -18], [6, 3, 9], "dorsal_ridge")
cube([-13, -6, -32], [2, 12, 16], "side_fin")         # прижатые боковые складки
cube([11, -6, -32], [2, 12, 16], "side_fin")
cube([-13, -4, -12], [2, 10, 12], "side_fin")
cube([11, -4, -12], [2, 10, 12], "side_fin")

# --- шея: короткая и толстая (плиозавр — не плезиозавр с лебединой шеей)
bone("neck", [0, 0, -34], {"axis": "y", "amp": 2.0, "speed": 0.9, "phase": 0.4})
cube([-9, -11, -48], [18, 22, 14], "back")

# --- голова: крупный череп + длинные челюсти с полным оскалом --------------------
#     ВАЖНО про оси: +Y вниз, поэтому ВЕРХНЯЯ челюсть лежит при меньшем Y, чем нижняя.
#     В v0.37.0 челюсти были перепутаны — «нижняя» оказывалась на темечке, а зубы
#     торчали из носа вверх (на листе это была белая полоса сверху морды).
bone("head", [0, 0, -48], {"axis": "y", "amp": 2.6, "speed": 1.1, "phase": 1.0})
cube([-10, -10, -64], [20, 22, 16], "back")           # череп
cube([-7, -2, -80], [14, 9, 16], "back")              # верхняя челюсть
cube([-5, -1, -88], [10, 8, 9], "back")               # кончик морды (розетка зубов)
cube([-6, 6, -82], [12, 8, 18], "belly")              # нижняя челюсть
cube([-5, 6, -88], [10, 7, 8], "belly")               # подбородок
cube([-5, 4, -78], [10, 2, 12], "gum")                # тёмное нёбо между челюстями
# Оскал: зубы идут по НАРУЖНОЙ кромке верхней челюсти (x = ±7..8) и по кромке нижней,
# поэтому белый ряд читается с любого ракурса и не «прорезает» морду сверху.
for zz in range(-86, -62, 4):
    for sx in (-1, 1):
        cube([sx * 8 - (1 if sx > 0 else 0), 3, zz], [1, 4, 2], "tooth")     # верхние клыки
for zz in range(-84, -62, 5):
    for sx in (-1, 1):
        cube([sx * 6 - (1 if sx > 0 else 0), 5, zz], [1, 3, 2], "tooth")     # нижние
cube([-11, -4, -64], [2, 3, 9], "side_fin")           # надбровные гребни
cube([9, -4, -64], [2, 3, 9], "side_fin")
cube([-10, -2, -62], [1, 3, 3], "eye")                # глаза высоко на черепе
cube([9, -2, -62], [1, 3, 3], "eye")
cube([-4, 0, -87], [2, 1, 3], "spot")                 # ноздри на кончике
cube([2, 0, -87], [2, 1, 3], "spot")

# --- передние ласты: длинные вёсла из трёх сужающихся сегментов
for name, sx, ph in (("fin_fr", -1, 0.0), ("fin_fl", 1, 0.0)):
    bone(name, [sx * 12, -2, -24], {"axis": "z", "amp": sx * 12.0, "speed": 1.3, "phase": ph})
    cube([min(sx * 12, sx * 30), -8, -32], [18, 10, 16], "fin")
    cube([min(sx * 30, sx * 50), -9, -28], [20, 7, 13], "fin_edge")
    cube([min(sx * 50, sx * 64), -8, -22], [14, 5, 10], "fin_edge")

# --- задние ласты: короче передних (у плиозавров гребут в основном передние)
for name, sx, ph in (("fin_br", -1, 0.9), ("fin_bl", 1, 0.9)):
    bone(name, [sx * 10, -3, 0], {"axis": "z", "amp": sx * 9.0, "speed": 1.15, "phase": ph})
    cube([min(sx * 10, sx * 26), -8, -6], [16, 8, 13], "fin")
    cube([min(sx * 26, sx * 42), -9, -3], [16, 6, 10], "fin_edge")
    cube([min(sx * 42, sx * 53), -8, 2], [11, 4, 8], "fin_edge")

# --- хвост: короткий, с вертикальным плавником
bone("tail1", [0, 0, 4], {"axis": "y", "amp": 5.0, "speed": 1.2, "phase": 0.2})
cube([-9, -12, 6], [18, 25, 16], "back")
bone("tail2", [0, 0, 20], {"axis": "y", "amp": 8.0, "speed": 1.2, "phase": 1.4})
cube([-7, -10, 22], [14, 20, 14], "back")
bone("tail3", [0, 0, 34], {"axis": "y", "amp": 11.0, "speed": 1.2, "phase": 2.4})
cube([-5, -8, 36], [10, 15, 12], "back")
cube([-3, -22, 46], [6, 18, 10], "fin_edge")          # верхняя лопасть хвоста
cube([-3, 2, 46], [6, 10, 8], "fin_edge")             # нижняя лопасть


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
