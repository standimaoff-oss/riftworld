#!/usr/bin/env python3
"""
v0.41.0 — Пегас, полностью пересобранная «витринная» модель.

Вместо «блина со сложенными крыльями» — анатомичный белый скакун:
  * бочкообразный корпус из фасеточных сечений, выраженные плечи и круп;
  * аркообразная шея, лошадиная голова с ушами, ноздрями, глазами и чёлкой;
  * четыре ноги с коленями, щётками, копытами и золотыми подковами;
  * развёрнутые вверх-назад крылья: leading edge, кроющие, второстепенные
    и длинные маховые перья с бородками и золотым кантом;
  * многослойная грива и хвост отдельными прядями, плюс золотые ленты;
  * седло с попоной, стёжкой, лукой, стременами, подпругой и уздечкой;
  * атлас 1024²: яблоки на шкуре, пряди, бородки перьев, стёжка и фаски.

Проект для Blockbench: reports/hero_models/pegasus.bbmodel и
pegasus.blockbench.geo.json. Запуск: python3 tools/rift/gen_v041_pegasus_model.py
"""
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from hero_model_lib import HeroRig, barrel, box_x, pair  # noqa: E402

ATLAS = 1024
RENDER_SCALE = 1.0

COAT = "#F6F3ED"
COAT_DARK = "#D9D4CA"
COAT_SHADE = "#BAB4A9"
MANE = "#EAE6DE"
MANE_DARK = "#B8B0A4"
MANE_GOLD = "#E5CB84"
FEATHER = "#FFFCF6"
FEATHER_PEARL = "#E7E2D9"
FEATHER_SHADE = "#C2BAAE"
FEATHER_TIP = "#9E9588"
FEATHER_GOLD = "#E8CC81"
SADDLE = "#5F3D34"
SADDLE_DARK = "#402A26"
GOLD = "#C9A24A"
BRIDLE = "#44302B"
HOOF = "#302D29"
HOOF_LIGHT = "#4B463F"
NOSE = "#C9A5A0"
EYE = "#15191E"
IRIS = "#8B5E3C"
GLINT = "#FFFFFF"

MATS = {
    "coat":          (COAT, COAT_DARK, COAT, COAT_SHADE),
    "coat_shade":    (COAT_DARK, COAT_SHADE, COAT_DARK, COAT_SHADE),
    "mane":          (MANE, MANE_DARK, MANE, MANE_DARK),
    "mane_dark":     (MANE_DARK, MANE, MANE_DARK, MANE),
    "mane_gold":     (MANE_GOLD, "#B99B55", MANE_GOLD, "#B99B55"),
    "feather":       (FEATHER, FEATHER_PEARL, FEATHER, FEATHER_PEARL),
    "feather_pearl": (FEATHER_PEARL, FEATHER_SHADE, FEATHER_PEARL, FEATHER_SHADE),
    "feather_shade": (FEATHER_SHADE, FEATHER_TIP, FEATHER_SHADE, FEATHER_TIP),
    "feather_tip":   (FEATHER_TIP, FEATHER_SHADE, FEATHER_TIP, FEATHER_SHADE),
    "feather_gold":  (FEATHER_GOLD, GOLD, FEATHER_GOLD, GOLD),
    "saddle":        (SADDLE, SADDLE_DARK, SADDLE, SADDLE_DARK),
    "saddle_dark":   (SADDLE_DARK, "#2F1F1C", SADDLE_DARK, "#2F1F1C"),
    "saddle_pad":    (SADDLE_DARK, SADDLE, SADDLE_DARK, SADDLE),
    "gold":          (GOLD, "#9B7832", GOLD, "#9B7832"),
    "bridle":        (BRIDLE, SADDLE_DARK, BRIDLE, SADDLE_DARK),
    "hoof":          (HOOF, HOOF_LIGHT, HOOF, HOOF_LIGHT),
    "nose":          (NOSE, "#A9817C", NOSE, "#A9817C"),
    "eye":           (EYE, EYE, EYE, EYE),
    "iris":          (IRIS, "#5D3D27", IRIS, IRIS),
    "glint":         (GLINT, GLINT, GLINT, GLINT),
    "star":          (FEATHER_GOLD, GOLD, FEATHER_GOLD, GOLD),
}

PATTERNS = {
    "coat": "dapple", "coat_shade": "dapple",
    "mane": "strands", "mane_dark": "strands", "mane_gold": "gold",
    "feather": "feather", "feather_pearl": "feather",
    "feather_shade": "feather", "feather_tip": "feather",
    "feather_gold": "gold", "saddle": "quilt", "saddle_dark": "quilt",
    "saddle_pad": "quilt",
    "gold": "gold", "star": "gold", "bridle": "quilt", "hoof": "horn",
    "nose": "mottle", "eye": "eye", "iris": "eye", "glint": "gold",
}


def lerp_points(points, t):
    t = max(0.0, min(1.0, t))
    for i in range(len(points) - 1):
        a, b = points[i], points[i + 1]
        if t <= b[0]:
            f = (t - a[0]) / max(1e-6, b[0] - a[0])
            return tuple(a[j] + (b[j] - a[j]) * f for j in (1, 2, 3))
    return tuple(points[-1][1:])


BODY_PROFILE = [
    (0.00, 8.5, 8.0, 23.0),
    (0.30, 9.0, 8.5, 23.0),
    (0.70, 8.0, 8.0, 23.0),
    (1.00, 7.0, 7.0, 23.0),
]
NECK_PROFILE = [
    (0.00, 3.8, 4.0, 38.0),
    (0.45, 5.2, 5.5, 34.5),
    (1.00, 7.5, 7.5, 28.0),
]
HEAD_PROFILE = [
    (0.00, 3.0, 3.0, 34.0),
    (0.35, 3.8, 4.0, 35.0),
    (1.00, 5.0, 5.5, 38.0),
]
NECK_Z0, NECK_Z1 = -30, -12
HEAD_Z0, HEAD_Z1 = -42, -28


def body_material(norm, t, j):
    if norm < -0.55:
        return "coat_shade", "dapple"
    return "coat", "dapple"


def head_material(norm, t, j):
    if norm < -0.65:
        return "coat_shade", "dapple"
    return "coat", "dapple"


def neck_material(norm, t, j):
    return "coat" if norm > -0.45 else "coat_shade", "dapple"


def neck_top(z):
    rx, ry, cy = lerp_points(NECK_PROFILE, (z - NECK_Z0) / (NECK_Z1 - NECK_Z0))
    return cy + ry


def build():
    r = HeroRig("pegasus", ATLAS, RENDER_SCALE)

    # ---------------------------------------------------------------- корпус
    r.bone("body", [0, 23, 0])
    barrel(r, "body", -16, 18, 5, 7,
           lambda t: lerp_points(BODY_PROFILE, t), body_material)
    # плечи и круп: дополнительные выпуклые пластины
    for sx in (-1, 1):
        for z, y, w, h, d in ((-12, 18, 4, 10, 8), (11, 18, 4, 10, 8)):
            o, size = box_x(sx, 5.0, y, z, w, h, d)
            r.cube(o, size, "coat", "dapple", bone="body")
        # мягкая линия along spine
        r.cube([min(sx * 2, sx * 5), 31, -13], [3, 1, 24],
               "coat_shade", "dapple", bone="body")
    # светлая подпруга и дымчатые яблоки на боку
    for z in range(-12, 16, 6):
        r.cube([-7, 22, z], [14, 1, 3], "coat_shade", "dapple", bone="body")

    # ---------------------------------------------------------------- шея и грива
    r.bone("neck", [0, 28, -14],
           {"axis": "x", "amp": 3.2, "speed": 0.8, "phase": 0.0}, parent="body")
    barrel(r, "neck", NECK_Z0, NECK_Z1, 3, 5,
           lambda t: lerp_points(NECK_PROFILE, t), neck_material)
    for i, z in enumerate(range(-29, -13, 3)):
        top = neck_top(z)
        for j, (dx, mat) in enumerate(((-1.5, "mane_dark"), (0.0, "mane"),
                                       (1.5, "mane_dark"))):
            r.cube([dx - 1.0, top - 1.0, z], [2, 5 + (i % 2), 3],
                   mat, "strands", bone="neck")
        if i % 2 == 0:
            r.cube([-0.5, top + 2.0, z + 1], [1, 3, 2],
                   "mane_gold", "gold", bone="neck")

    # ---------------------------------------------------------------- голова
    r.bone("head", [0, 38, -28],
           {"axis": "x", "amp": 2.8, "speed": 1.0, "phase": 0.5}, parent="neck")
    barrel(r, "head", HEAD_Z0, HEAD_Z1, 3, 5,
           lambda t: lerp_points(HEAD_PROFILE, t), head_material)
    # морда, губы, ноздри и подбородок
    r.cube([-3, 32, -45], [6, 5, 3], "coat", "dapple", bone="head")
    r.cube([-2.5, 31, -44], [5, 1, 4], "nose", "mottle", bone="head")
    r.cube([-2.0, 30, -42], [4, 2, 5], "coat_shade", "dapple", bone="head")
    pair(r, "head", 1.6, 34.0, -45, [1, 1, 1], "nose", "mottle")
    # уши с тёплой внутренней частью
    for sx in (-1, 1):
        o, size = box_x(sx, 3.2, 41.0, -32, 2, 5, 2)
        r.cube(o, size, "coat", "dapple", bone="head")
        o2, size2 = box_x(sx, 3.7, 42.0, -33, 1, 3, 1)
        r.cube(o2, size2, "nose", "mottle", bone="head")
    # глаза: глазница, карий зрачок, белый блик
    for sx in (-1, 1):
        for inner, y, z, size, mat in (
            (4.2, 37.0, -36, [2, 3, 3], "eye"),
            (5.1, 37.5, -35, [1, 2, 2], "iris"),
            (5.8, 38.5, -34, [1, 1, 1], "glint"),
        ):
            o, _ = box_x(sx, inner, y, z, *size)
            r.cube(o, size, mat, bone="head")
    # чёлка, золотая звёздочка во лбу, скулы
    for i, z in enumerate(range(-40, -34, 2)):
        r.cube([-1.5 + (i % 2) * 0.8, 40 - i, z], [2, 4, 3],
               "mane" if i % 2 else "mane_dark", "strands", bone="head")
    r.cube([-1, 41, -40], [2, 2, 2], "star", "gold", bone="head")
    r.cube([-0.5, 43, -39], [1, 2, 1], "star", "gold", bone="head")
    pair(r, "head", 4.0, 34.0, -39, [1, 3, 5], "coat_shade", "dapple")
    # уздечка: нахрапник, налобник, щёчные ремни, пряжки, поводья
    r.cube([-3.5, 35, -44], [7, 1, 2], "bridle", "quilt", bone="head")
    r.cube([-4.5, 40, -34], [9, 1, 2], "gold", "gold", bone="head")
    pair(r, "head", 4.4, 36.0, -37, [1, 7, 2], "bridle", "quilt")
    pair(r, "head", 5.0, 36.0, -37, [1, 1, 1], "gold", "gold")
    for z in (-34, -30, -26, -22):
        for sx in (-1, 1):
            o, size = box_x(sx, 4.0, 33 - (z + 34) * 0.15, z, 1, 1, 5)
            r.cube(o, size, "bridle", "quilt", bone="head")

    # ---------------------------------------------------------------- ноги
    def leg(name, sx, front):
        lz = -10 if front else 11
        pivot = [sx * 5, 16, lz]
        phase = 0.0 if (sx < 0) == front else 1.6
        r.bone(name, pivot,
               {"axis": "x", "amp": 5.0, "speed": 1.7, "phase": phase}, parent="body")
        knee_z = lz + (0 if front else 1)
        parts = [
            (4.0, 11, lz - 2.5, 4, 7, 5, "coat"),
            (4.5, 8, knee_z - 1.8, 3, 4, 4, "coat_shade"),
            (5.0, 3, lz - 1.2, 2, 6, 3, "coat"),
            (4.0, 2, lz - 2.0, 4, 2, 4, "mane"),
            (4.0, 0, lz - 2.0, 4, 2, 4, "hoof"),
            (4.0, 2, lz - 2.0, 4, 1, 4, "gold"),
        ]
        for inner, y, z, w, h, d, mat in parts:
            o, size = box_x(sx, inner, y, z, w, h, d)
            r.cube(o, size, mat, None, bone=name)
        # щётки по бокам бабки
        for dz in (-1, 2):
            o, size = box_x(sx, 3.5, 3, lz + dz, 1, 4, 2)
            r.cube(o, size, "mane_dark", "strands", bone=name)

    leg("leg_fl", -1, True)
    leg("leg_fr", 1, True)
    leg("leg_bl", -1, False)
    leg("leg_br", 1, False)

    # ---------------------------------------------------------------- хвост
    r.bone("tail", [0, 27, 17],
           {"axis": "y", "amp": 7.0, "speed": 1.0, "phase": 0.4}, parent="body")
    for i in range(5):
        z = 17 + i * 2
        y = 25 - i * 2.5
        for j, x in enumerate((-2.0, 0.0, 2.0)):
            r.cube([x - 1.0, y - 1.0, z + (j % 2)], [2, 7 - i // 2, 3],
                   "mane" if j == 1 else "mane_dark", "strands", bone="tail")
    r.cube([-1, 19, 20], [2, 2, 3], "mane_gold", "gold", bone="tail")
    r.cube([-1, 15, 24], [2, 2, 3], "mane_gold", "gold", bone="tail")

    # ---------------------------------------------------------------- крылья
    def wing(side, label):
        root = f"wing_{label}"
        mid = f"wing2_{label}"
        tip = f"wing3_{label}"
        s = side
        r.bone(root, [s * 7, 28, -8],
               {"axis": "z", "amp": 4.0 * s, "speed": 1.15, "phase": 0.0},
               parent="body")
        r.bone(mid, [s * 15, 33, 0],
               {"axis": "z", "amp": 3.0 * s, "speed": 1.15, "phase": 0.45},
               parent=root)
        r.bone(tip, [s * 23, 38, 8],
               {"axis": "z", "amp": 2.5 * s, "speed": 1.15, "phase": 0.9},
               parent=mid)

        # плечо и предплечье: три яруса белого пера
        for y, h, z, w, d, mat in (
            (27, 3, -10, 8, 10, "feather_shade"),
            (29, 3, -9, 8, 10, "feather"),
            (32, 2, 0, 9, 9, "feather_pearl"),
        ):
            o, size = box_x(s, 7, y, z, w, h, d)
            r.cube(o, size, mat, "feather", bone=root)
        for y, h, z, w, d, mat in (
            (32, 2, 1, 8, 8, "feather_pearl"),
            (34, 2, 3, 8, 8, "feather"),
            (36, 2, 8, 7, 7, "feather_shade"),
        ):
            o, size = box_x(s, 15, y, z, w, h, d)
            r.cube(o, size, mat, "feather", bone=mid)
        # золотая ведущая кромка и кроющие перья
        for i in range(8):
            span = i / 7.0
            inner = 7 + span * 21
            y = 29 + span * 12
            z = -9 + span * 9
            o, size = box_x(s, inner, y, z, 3, 1, 4)
            r.cube(o, size, "feather_gold", "gold", bone=root if i < 3 else mid)
            o2, size2 = box_x(s, inner, y - 2, z + 2, 3, 2, 7)
            r.cube(o2, size2, "feather_pearl" if i % 2 else "feather_shade",
                   "feather", bone=root if i < 3 else mid)
        # маховые: три ряда у каждого пера + тонкий стержень
        for i in range(10):
            span = i / 9.0
            inner = 9 + span * 20
            y0 = 30 + span * 12
            z0 = -4 + span * 8
            length = 18 + span * 8
            bone = root if span < 0.34 else (mid if span < 0.72 else tip)
            for row in range(3):
                yy = y0 - (row + 1) * (2.2 + span * 1.3)
                zz = z0 + row * 3
                w = max(2, 4 - row)
                d = max(6, int(length - row * 4))
                mat = ("feather", "feather_pearl", "feather_shade")[row]
                o, size = box_x(s, inner + row * 1.2, yy, zz, w, 1, d)
                r.cube(o, size, mat, "feather", bone=bone)
            o, size = box_x(s, inner + 1.0, y0 - 1.0, z0, 1, 1, int(length + 2))
            r.cube(o, size, "feather_tip", "feather", bone=bone)
        # кисть крыла: три длинных крайних пера и золотой кончик
        for i in range(3):
            o, size = box_x(s, 27 + i, 40 - i * 2, 10 + i * 3, 3, 1, 18 - i * 3)
            r.cube(o, size, "feather" if i == 0 else "feather_pearl",
                   "feather", bone=tip)
        o, size = box_x(s, 29, 43, 18, 3, 1, 6)
        r.cube(o, size, "feather_gold", "gold", bone=tip)

    wing(-1, "l")
    wing(1, "r")

    # ---------------------------------------------------------------- седло и сбруя
    r.cube([-6, 31, -7], [12, 1, 15], "saddle_pad", "quilt", bone="body")
    r.cube([-6, 32, -7], [12, 1, 1], "gold", "gold", bone="body")
    r.cube([-6, 32, 7], [12, 1, 1], "gold", "gold", bone="body")
    r.cube([-6, 32, -7], [1, 1, 15], "gold", "gold", bone="body")
    r.cube([5, 32, -7], [1, 1, 15], "gold", "gold", bone="body")
    r.cube([-5, 32, -5], [10, 2, 10], "saddle", "quilt", bone="body")
    r.cube([-4, 34, -7], [8, 2, 2], "saddle_dark", "quilt", bone="body")
    r.cube([-4, 34, 5], [8, 2, 2], "saddle_dark", "quilt", bone="body")
    r.cube([-5, 34, -5], [10, 1, 1], "gold", "gold", bone="body")
    r.cube([-5, 34, 4], [10, 1, 1], "gold", "gold", bone="body")
    # подпруга, стремена и медальоны на попоне
    r.cube([-6, 23, 0], [12, 1, 2], "bridle", "quilt", bone="body")
    for sx in (-1, 1):
        o, size = box_x(sx, 7, 25, 1, 1, 6, 2)
        r.cube(o, size, "gold", "gold", bone="body")
        o2, size2 = box_x(sx, 7, 24, 0, 2, 1, 4)
        r.cube(o2, size2, "saddle_dark", "quilt", bone="body")
        o3, size3 = box_x(sx, 6.2, 31, -1, 1, 3, 3)
        r.cube(o3, size3, "star", "gold", bone="body")

    return r


def main():
    r = build()
    stats = r.write_game_geo(MATS, "textures/entity/pegasus.png", PATTERNS, flip_y=True)
    print(f"Пегас: костей {stats['bones']}, кубоидов {stats['cubes']}, "
          f"атлас {stats['atlas']}², UV {stats['occupied_uv_px']} px")
    print(f"габариты: {stats['design_extents_units']['size']} ед. → "
          f"{stats['size_blocks']} блоков, render_scale={stats['render_scale']}")
    print("game:", stats["game_geo"], "| tex:", stats["texture"])


if __name__ == "__main__":
    main()
