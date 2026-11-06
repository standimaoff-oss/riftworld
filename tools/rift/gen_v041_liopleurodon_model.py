#!/usr/bin/env python3
"""
v0.41.0 — Лиоплевродон, полностью пересобранная «витринная» модель.

Идея: не набор коробок, а округлая торпеда с настоящей головой плиозавра.
  * туловище, шея, череп и хвост набраны фасеточными сечениями;
  * отдельная подвижная нижняя челюсть, тёмное нёбо, язык, десны, оскал
    из 40+ зубов разной длины и четыре крупных клыка;
  * удлинённая крокодилья морда, ноздри, надбровные гребни,
    трёхслойные глаза, скуловые дуги и шрамы;
  * четыре широких веслообразных ласта с лучами и ведущей кромкой;
  * трёхсегментный хвост с вертикальным серповидным плавником;
  * боковая линия, брюшные пластины, спинные скуты и бирюзовые фотоплямки;
  * атлас 1024² с чешуёй, лучами, пластинами, царапинами и фасками.

Проект для Blockbench: reports/hero_models/liopleurodon.bbmodel и
liopleurodon.blockbench.geo.json. Запуск: python3 tools/rift/gen_v041_liopleurodon_model.py
"""
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from hero_model_lib import HeroRig, barrel, box_x, pair  # noqa: E402

ATLAS = 1024
TARGET_BLOCKS = 8.6
Y_STRETCH = 1.55          # поднимаем корпус: плиозавр, а не камбала

BACK_DARK = "#223746"
BACK = "#38586B"
FLANK = "#5C7F90"
BELLY = "#AEC2C8"
BELLY_LIGHT = "#D9E3E2"
SCAR = "#D9C7A3"
SPOT = "#1C2C37"
FIN = "#406A7E"
FIN_DARK = "#294E60"
FIN_EDGE = "#78B8BE"
RIDGE = "#1F3A49"
TOOTH = "#F3EBD6"
TOOTH_SHADE = "#D8CDB2"
GUM = "#713E46"
TONGUE = "#A95D64"
MOUTH = "#241B22"
SOCKET = "#0B131A"
IRIS = "#D9A03C"
GLINT = "#FFF6D8"
GLOW = "#73D9CF"

MATS = {
    "back":        (BACK_DARK, BACK, BACK, BACK_DARK),
    "flank":       (BACK, FLANK, FLANK, BACK_DARK),
    "belly":       (FLANK, BELLY, BELLY, BELLY_LIGHT),
    "belly_plate": (BELLY, BELLY_LIGHT, BELLY_LIGHT, BELLY),
    "head_back":   (BACK_DARK, BACK, BACK, BACK_DARK),
    "head_side":   (BACK, FLANK, FLANK, BACK_DARK),
    "jaw":         (BELLY, BELLY_LIGHT, BELLY, BELLY),
    "mouth":       (MOUTH, MOUTH, MOUTH, MOUTH),
    "gum":         (GUM, GUM, "#552D34", GUM),
    "tongue":      (TONGUE, TONGUE, "#7A3E46", TONGUE),
    "tooth":       (TOOTH, TOOTH_SHADE, TOOTH, TOOTH_SHADE),
    "spot":        (SPOT, SPOT, SPOT, SPOT),
    "scar":        (SCAR, SCAR, "#B8A984", SCAR),
    "ridge":       (RIDGE, BACK_DARK, BACK_DARK, RIDGE),
    "fin":         (FIN, FIN_DARK, FIN, FIN_DARK),
    "fin_dark":    (FIN_DARK, BACK_DARK, FIN_DARK, BACK_DARK),
    "fin_edge":    (FIN_EDGE, FIN, FIN_EDGE, FIN),
    "glow":        (GLOW, "#44A9AA", GLOW, "#44A9AA"),
    "socket":      (SOCKET, SOCKET, SOCKET, SOCKET),
    "iris":        (IRIS, "#9E6B22", IRIS, IRIS),
    "glint":       (GLINT, GLINT, GLINT, GLINT),
}

PATTERNS = {
    "back": "scales", "flank": "scales", "belly": "plates",
    "belly_plate": "plates", "head_back": "scales", "head_side": "scales",
    "jaw": "plates", "mouth": "mottle", "gum": "mottle", "tongue": "mottle",
    "tooth": "mottle", "spot": "mottle", "scar": "strands", "ridge": "stripe",
    "fin": "rays", "fin_dark": "rays", "fin_edge": "rays", "glow": "gold",
    "socket": "eye", "iris": "eye", "glint": "gold",
}


def lerp_points(points, t):
    """points: [(t, rx, ry, cy), ...] — кусочно-линейный профиль сечения."""
    t = max(0.0, min(1.0, t))
    for i in range(len(points) - 1):
        a, b = points[i], points[i + 1]
        if t <= b[0]:
            f = (t - a[0]) / max(1e-6, b[0] - a[0])
            return tuple(a[j] + (b[j] - a[j]) * f for j in (1, 2, 3))
    return tuple(points[-1][1:])


BODY_PROFILE = [
    (0.00, 14.0, 11.0, 0.0),
    (0.18, 18.0, 13.0, 0.0),
    (0.48, 19.0, 13.0, 0.0),
    (0.75, 16.0, 11.0, 0.0),
    (1.00, 10.0, 8.0, 0.0),
]
NECK_PROFILE = [
    (0.00, 13.0, 10.0, 0.0),
    (0.55, 15.0, 11.0, 0.0),
    (1.00, 15.0, 12.0, 0.0),
]
HEAD_PROFILE = [
    (0.00, 6.5, 3.8, 3.5),
    (0.28, 9.0, 5.5, 4.5),
    (0.65, 12.0, 8.0, 6.0),
    (1.00, 13.5, 10.0, 7.0),
]
JAW_PROFILE = [
    (0.00, 5.0, 2.8, -8.0),
    (0.35, 8.0, 3.2, -8.0),
    (1.00, 11.5, 3.5, -8.0),
]
TAIL1_PROFILE = [(0.0, 9.0, 7.5, 0.0), (1.0, 6.0, 5.5, 0.0)]
TAIL2_PROFILE = [(0.0, 6.0, 5.5, 0.0), (1.0, 3.5, 3.8, 0.0)]
TAIL3_PROFILE = [(0.0, 3.5, 3.8, 0.0), (1.0, 2.0, 2.6, 0.0)]

HEAD_Z0, HEAD_Z1 = -104, -58
HEAD_LEN = HEAD_Z1 - HEAD_Z0


def body_profile(t):
    return lerp_points(BODY_PROFILE, t)


def head_rx(z):
    return lerp_points(HEAD_PROFILE, (z - HEAD_Z0) / HEAD_LEN)[0]


def jaw_rx(z):
    return lerp_points(JAW_PROFILE, (z - HEAD_Z0) / HEAD_LEN)[0]


def body_material(norm, t, j):
    if norm > 0.48:
        return "back", "scales"
    if norm < -0.46:
        return "belly", "plates"
    return "flank", "scales"


def head_material(norm, t, j):
    if norm > 0.35:
        return "head_back", "scales"
    if norm < -0.78:
        return "gum", "mottle"
    return "head_side", "scales"


def jaw_material(norm, t, j):
    if norm > 0.55:
        return "gum", "mottle"
    return "jaw", "plates"


def build():
    r = HeroRig("liopleurodon", ATLAS, y_stretch=Y_STRETCH)

    # ---------------------------------------------------------------- торс
    r.bone("body", [0, 0, -20])
    barrel(r, "body", -42, 48, 9, 7, body_profile, body_material)
    # спинной киль и парные скуты
    for z in range(-36, 43, 6):
        rx, ry, _ = body_profile((z + 42) / 90.0)
        r.cube([-2.0, ry - 0.5, z], [4, 2, 5], "ridge", "stripe", bone="body")
        for sx in (-1, 1):
            r.cube([min(sx * (rx - 3), sx * (rx + 1)), ry - 3.0, z + 1],
                   [4, 1, 3], "ridge", "stripe", bone="body")
    # брюшные пластины и светлая контюрная полоса
    for z in range(-38, 43, 6):
        r.cube([-8, -11.5, z], [16, 1, 5], "belly_plate", "plates", bone="body")
        for sx in (-1, 1):
            rx, _, _ = body_profile((z + 42) / 90.0)
            o, _ = box_x(sx, rx - 1.2, -3.0, z, 1, 1, 4)
            r.cube(o, [1, 1, 4], "belly", "plates", bone="body")
    # боковая линия: редкие фотофоры, между ними тёмные пятна
    for i, z in enumerate(range(-36, 43, 9)):
        rx, _, _ = body_profile((z + 42) / 90.0)
        for sx in (-1, 1):
            o, _ = box_x(sx, rx - 0.8, 1.0, z + 1, 1, 2, 3)
            r.cube(o, [1, 2, 3], "glow", "gold", bone="body")
            if i % 2 == 0:
                o2, _ = box_x(sx, rx - 0.9, 4.0, z + 2, 1, 2, 2)
                r.cube(o2, [1, 2, 2], "spot", "mottle", bone="body")
    # шрамы-царапины на боках
    for z, y in ((-28, 6), (-12, 3), (4, 7), (20, 4), (34, 6)):
        rx, _, _ = body_profile((z + 42) / 90.0)
        for sx in (-1, 1):
            o, _ = box_x(sx, rx - 0.6, y, z, 1, 1, 7)
            r.cube(o, [1, 1, 7], "scar", "strands", bone="body")

    # ---------------------------------------------------------------- шея
    r.bone("neck", [0, 0, -42], {"axis": "y", "amp": 1.6, "speed": 0.8, "phase": 0.3},
           parent="body")
    barrel(r, "neck", -60, -42, 6, 6,
           lambda t: lerp_points(NECK_PROFILE, t), body_material)
    for z in (-58, -52, -46):
        r.cube([-7, -10.5, z], [14, 1, 5], "belly_plate", "plates", bone="neck")

    # ---------------------------------------------------------------- голова
    r.bone("head", [0, 0, -60], {"axis": "y", "amp": 2.0, "speed": 1.0, "phase": 0.8},
           parent="neck")
    barrel(r, "head", HEAD_Z0, HEAD_Z1, 5, 6,
           lambda t: lerp_points(HEAD_PROFILE, t), head_material)
    # носовой гребень и шишки на черепе
    for z in range(-100, -73, 5):
        r.cube([-1.5, 7.0, z], [3, 1, 4], "ridge", "stripe", bone="head")
    # тёмная пасть, язык и боковая линия рта
    for z in range(-102, -59, 5):
        w = int(2 * head_rx(z) - 3)
        r.cube([-w / 2.0, -4.0, z], [w, 4, 5], "mouth", "mottle", bone="head")
        for sx in (-1, 1):
            o, _ = box_x(sx, head_rx(z) - 0.8, -3.0, z, 1, 2, 4)
            r.cube(o, [1, 2, 4], "mouth", "mottle", bone="head")
            o2, _ = box_x(sx, head_rx(z) - 0.8, -1.0, z, 1, 1, 4)
            r.cube(o2, [1, 1, 4], "gum", "mottle", bone="head")
    for z in range(-98, -69, 6):
        w = int(head_rx(z) * 1.1)
        r.cube([-w / 2.0, -5.0, z], [w, 1, 5], "tongue", "mottle", bone="head")
    # надбровные гребни, скуловые дуги, ноздри
    pair(r, "head", 11.0, 11.0, -82, [3, 2, 14], "ridge", "stripe")
    pair(r, "head", 9.5, 8.0, -75, [2, 2, 18], "ridge", "stripe")
    pair(r, "head", 2.2, 6.5, -101, [2, 2, 3], "spot", "mottle")
    # трёхслойные глаза: глазница, янтарная радужка, блик
    for sx in (-1, 1):
        for inner, y, z, size, mat in (
            (11.5, 8.0, -78, [2, 4, 4], "socket"),
            (12.5, 8.5, -77, [1, 3, 3], "iris"),
            (13.2, 9.5, -76, [1, 1, 1], "glint"),
        ):
            o, _ = box_x(sx, inner, y, z, *size)
            r.cube(o, size, mat, bone="head")
    # верхние зубы: наружный ряд, внутренний ряд и четыре клыка
    for z in range(-100, -62, 4):
        rx = head_rx(z)
        for sx in (-1, 1):
            o, _ = box_x(sx, rx - 1.2, -5.0, z, 2, 5, 2)
            r.cube(o, [2, 5, 2], "tooth", "mottle", bone="head")
        if z % 8 == 0:
            for sx in (-1, 1):
                o, _ = box_x(sx, rx - 4.0, -4.0, z + 1, 1, 3, 2)
                r.cube(o, [1, 3, 2], "tooth", "mottle", bone="head")
    for z in (-99, -95):
        rx = head_rx(z)
        for sx in (-1, 1):
            o, _ = box_x(sx, rx - 1.4, -8.0, z, 2, 8, 2)
            r.cube(o, [2, 8, 2], "tooth", "mottle", bone="head")

    # ---------------------------------------------------------------- нижняя челюсть
    r.bone("jaw", [0, -5, -58], {"axis": "x", "amp": 3.2, "speed": 1.1, "phase": 0.2},
           parent="head")
    barrel(r, "jaw", HEAD_Z0, HEAD_Z1, 5, 3,
           lambda t: lerp_points(JAW_PROFILE, t), jaw_material)
    for z in range(-100, -62, 4):
        rx = jaw_rx(z)
        for sx in (-1, 1):
            o, _ = box_x(sx, rx - 1.2, -4.5, z, 2, 1, 3)
            r.cube(o, [2, 1, 3], "gum", "mottle", bone="jaw")
            o2, _ = box_x(sx, rx - 1.0, -4.0, z + 1, 1, 5, 2)
            r.cube(o2, [1, 5, 2], "tooth", "mottle", bone="jaw")
    for z in range(-100, -63, 7):
        rx = jaw_rx(z)
        r.cube([-rx + 2, -11.5, z], [int(2 * rx - 4), 1, 5],
               "belly_plate", "plates", bone="jaw")

    # ---------------------------------------------------------------- ласты
    def flipper(name, sx, front, pivot_x, pivot_z, phase):
        amp = 9.0 * sx if front else 7.0 * sx
        r.bone(name, [sx * pivot_x, -2, pivot_z],
               {"axis": "z", "amp": amp, "speed": 1.25 if front else 1.05,
                "phase": phase}, parent="body")
        z0 = -36 if front else 14
        root = (12, 10, 16) if front else (9, 8, 13)
        segs = ([(22, 10, 15), (32, 9, 12), (41, 8, 9)] if front
                else [(17, 8, 12), (25, 7, 9), (32, 6, 7)])
        tip_inner = 49 if front else 38
        # корень: три яруса (ведущая кромка / лопасть / тень)
        for k, (y, h, mat) in enumerate(((2, 2, "fin_edge"), (-1, 4, "fin"),
                                         (-4, 3, "fin_dark"))):
            o, size = box_x(sx, root[0], y, z0 + k, root[1], h, root[2])
            r.cube(o, size, mat, "rays", bone=name)
        for k, (inner, w, d) in enumerate(segs):
            zz = z0 + 2 + k * 3
            yy = -1 - k
            h = max(2, (4 if front else 3) - k // 2)
            o, size = box_x(sx, inner, yy, zz, w, h, d)
            r.cube(o, size, "fin" if k % 2 == 0 else "fin_dark", "rays", bone=name)
            o2, size2 = box_x(sx, inner, yy + h - 1, zz, w, 1, 2)
            r.cube(o2, size2, "fin_edge", "rays", bone=name)
        # лучи на конце весла
        for ray in range(3):
            o, size = box_x(sx, tip_inner - 1, -3 - ray,
                            z0 + 2 + len(segs) * 3 + ray * 2, 4 - ray, 1, 8)
            r.cube(o, size, "fin_edge", "rays", bone=name)

    flipper("fin_fl", -1, True, 16, -28, 0.0)
    flipper("fin_fr", 1, True, 16, -28, 0.0)
    flipper("fin_bl", -1, False, 12, 24, 0.9)
    flipper("fin_br", 1, False, 12, 24, 0.9)

    # ---------------------------------------------------------------- хвост
    r.bone("tail1", [0, 0, 48], {"axis": "y", "amp": 3.5, "speed": 1.15, "phase": 0.2},
           parent="body")
    barrel(r, "tail1", 48, 60, 4, 5,
           lambda t: lerp_points(TAIL1_PROFILE, t), body_material)
    r.bone("tail2", [0, 0, 60], {"axis": "y", "amp": 6.0, "speed": 1.15, "phase": 1.0},
           parent="tail1")
    barrel(r, "tail2", 60, 72, 4, 4,
           lambda t: lerp_points(TAIL2_PROFILE, t), body_material)
    r.bone("tail3", [0, 0, 72], {"axis": "y", "amp": 8.5, "speed": 1.15, "phase": 1.9},
           parent="tail2")
    barrel(r, "tail3", 72, 82, 3, 3,
           lambda t: lerp_points(TAIL3_PROFILE, t), body_material)
    r.bone("tail_fin", [0, 0, 82], {"axis": "y", "amp": 5.0, "speed": 1.15, "phase": 2.7},
           parent="tail3")
    # верхняя и нижняя лопасти серповидного плавника
    for i in range(5):
        w = max(2, 5 - i)
        r.cube([-w / 2.0, 1 + i * 3, 80 + i * 2], [w, 3, 4],
               "fin_edge" if i < 3 else "ridge", "rays", bone="tail_fin")
    for i in range(4):
        w = max(2, 4 - i)
        r.cube([-w / 2.0, -5 - i * 2, 81 + i * 2], [w, 3, 4],
               "fin", "rays", bone="tail_fin")
    for i in range(4):
        r.cube([-1, 3 + i * 3, 82 + i], [2, 1, 5], "fin_edge", "rays", bone="tail_fin")
    for z in (54, 60, 66):
        r.cube([-1, 6, z], [2, 3, 4], "ridge", "stripe", bone="tail1")

    return r


def main():
    r = build()
    lo, hi, size = r.extents()
    r.render_scale = round(TARGET_BLOCKS / (size[2] / 16.0), 4)
    stats = r.write_game_geo(MATS, "textures/entity/liopleurodon.png", PATTERNS, flip_y=True)
    print(f"Лиоплевродон: костей {stats['bones']}, кубоидов {stats['cubes']}, "
          f"атлас {stats['atlas']}², UV {stats['occupied_uv_px']} px")
    print(f"габариты: {size} ед. → {stats['size_blocks']} блоков, "
          f"render_scale={stats['render_scale']}")
    print("game:", stats["game_geo"], "| tex:", stats["texture"])


if __name__ == "__main__":
    main()
