#!/usr/bin/env python3
"""
v0.34.0 — модель Пегаса (`riftworld:pegasus`): крылатый конь для «земля/воздух».

По референсу `reports/mob_refs/04_ref_pegasus.png`: белая шкура с серыми яблоками,
седая грива и хвост, широкие маховые крылья с рядами перьев, крепкие ноги.

Модель собрана через `geo_model_lib.Rig`: честные UV-островки, атлас 256x256.
"""
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from geo_model_lib import Rig  # noqa: E402

ATLAS = 256

COAT = "#F4F2EE"
COAT_DARK = "#D5D1C9"
COAT_SHADE = "#BEB9B0"
MANE = "#EDEAE4"
MANE_DARK = "#B9B4AA"
HOOF = "#2E2B27"
EYE = "#101318"
FEATHER = "#FCFBF9"
FEATHER_EDGE = "#C9C4BB"
FEATHER_DARK = "#A9A399"
NOSE = "#C09C95"
SADDLE = "#6B4A38"
SADDLE_TRIM = "#C9A24A"

MATS = {
    "coat": (COAT, COAT_DARK, COAT, COAT_SHADE),
    "coat_shade": (COAT_DARK, COAT_SHADE, COAT_DARK, COAT_SHADE),
    "mane": (MANE, MANE_DARK, MANE, MANE_DARK),
    "hoof": (HOOF, HOOF, HOOF, HOOF),
    "eye": (EYE, EYE, EYE, EYE),
    "nose": (NOSE, NOSE, NOSE, NOSE),
    "feather": (FEATHER, FEATHER_EDGE, FEATHER, FEATHER_EDGE),
    "feather_edge": (FEATHER_EDGE, FEATHER_DARK, FEATHER_EDGE, FEATHER_DARK),
    "saddle": (SADDLE, SADDLE_TRIM, SADDLE, SADDLE_TRIM),
}

SPECK = [COAT_DARK, COAT_SHADE, "#CFCAC2"]


def build():
    """v0.39.3: конь, а не «блин». В прошлой модели высота была 24 юнита, а размах крыльев 84 —
    получалось 1.75 блока в высоту против 6.1 в ширину, и на кадре это читалось как куча плит.

    Теперь: холка 2.15 блока, корпус длиной 2.5, крылья сложены вдоль корпуса (размах ~3.6),
    ноги с копытами, грива, хвост и седло. Масштаб 1.0 — юнит равен 1/16 блока.
    """
    r = Rig(ATLAS)

    # ---------------- корпус: бочка из трёх ярусов (спина / бока / брюхо)
    r.bone("body", [0, 22, 0])
    r.cube([-5, 22, -13], [10, 4, 26], "coat", SPECK)        # спина (узкая)
    r.cube([-7, 15, -13], [14, 8, 26], "coat", SPECK)        # бока (широкие)
    r.cube([-5, 12, -11], [10, 4, 22], "coat_shade", SPECK)  # брюхо (узкое)
    r.cube([-6, 24, -12], [12, 3, 10], "saddle")             # седло на холке
    r.cube([-6, 27, -12], [12, 1, 10], "saddle")             # лука седла

    # ---------------- шея: поднимается вперёд-вверх, толстая у плеча
    r.bone("neck", [0, 24, -12], {"axis": "x", "amp": 4.0, "speed": 0.8, "phase": 0.0})
    r.cube([-4, 22, -18], [8, 10, 9], "coat", SPECK)
    r.cube([-4, 26, -22], [8, 9, 8], "coat", SPECK)
    # грива по верху шеи
    for i in range(5):
        r.cube([-1, 30 + i, -21 + i * 2], [2, 3, 3], "mane")

    # ---------------- голова: вытянутый череп, морда, уши, глаза
    r.bone("head", [0, 32, -22], {"axis": "x", "amp": 3.0, "speed": 1.0, "phase": 0.6})
    r.cube([-3, 28, -28], [7, 8, 7], "coat", SPECK)          # череп
    r.cube([-3, 26, -33], [6, 6, 6], "coat", SPECK)          # морда
    r.cube([-2, 26, -35], [4, 4, 2], "nose")                 # нос
    r.cube([-4, 30, -26], [1, 3, 3], "eye")                  # глаза
    r.cube([4, 30, -26], [1, 3, 3], "eye")
    r.cube([-3, 33, -27], [1, 4, 2], "coat")                 # уши
    r.cube([3, 33, -27], [1, 4, 2], "coat")
    r.cube([-1, 31, -24], [2, 3, 6], "mane")                 # чёлка
    r.cube([-1, 24, -30], [2, 3, 4], "mane")                 # борода

    # ---------------- ноги: бедро, голень, копыто (передние чуть прямее задних)
    for name, lx, lz, top, knee in (("leg_fl", -5, -9, 16, 8), ("leg_fr", 5, -9, 16, 8),
                                    ("leg_bl", -5, 9, 16, 9), ("leg_br", 5, 9, 16, 9)):
        r.bone(name, [lx, top, lz], {"axis": "x", "amp": 13.0, "speed": 2.2,
                                     "phase": 0.0 if name.endswith("l") else 1.6})
        r.cube([lx - 2, knee, lz - 2], [5, top - knee + 1, 5], "coat", SPECK)   # бедро
        r.cube([lx - 1, 2, lz - 1], [3, knee - 1, 3], "coat_shade")             # голень
        r.cube([lx - 2, 0, lz - 2], [5, 2, 5], "hoof")                          # копыто

    # ---------------- хвост: три сегмента из гривы
    r.bone("tail", [0, 22, 12], {"axis": "y", "amp": 7.0, "speed": 1.1, "phase": 0.4})
    r.cube([-2, 18, 12], [5, 6, 5], "mane")
    r.cube([-2, 12, 14], [5, 7, 4], "mane")
    r.cube([-2, 6, 16], [4, 7, 3], "mane")

    # ---------------- крылья: СЛОЖЕНЫ вдоль корпуса, три яруса перьев
    #     (в прошлой версии они торчали в стороны на 42 юнита — отсюда «блин» шириной 6 блоков)
    for side, sx in (("l", 1), ("r", -1)):
        r.bone(f"wing_{side}", [sx * 6, 24, -8], {"axis": "z", "amp": sx * -10.0,
                                                  "speed": 1.8, "phase": 0.0})
        # костяная основа крыла: плечо → предплечье → кисть, всё прижато к боку
        r.cube([min(sx * 6, sx * 11), 23, -10], [6, 5, 10], "feather")          # плечо
        r.cube([min(sx * 8, sx * 13), 25, -2], [6, 4, 12], "feather")           # предплечье назад
        r.cube([min(sx * 9, sx * 14), 27, 8], [6, 3, 8], "feather_edge")        # кисть у крупа
        # три яруса маховых: короткие сверху, длинные снизу и сзади
        for i in range(3):
            r.cube([min(sx * 10, sx * 15), 21 - i * 3, -6 + i * 5], [5, 3, 16 - i * 2], "feather")
            r.cube([min(sx * 11, sx * 16), 19 - i * 3, 2 + i * 4], [4, 2, 12 - i * 2], "feather_edge")
    return r


def main():
    r = build()
    # v0.39.3: масштаб привязан к ВЫСОТЕ в холке (2.15 блока) — раньше считался по высоте
    # 24 юнита и растягивал коня в ширину до 6 блоков.
    info = r.write("pegasus", MATS, "textures/entity/pegasus.png", scale_to=1.0, length_axis=1,
                   length_blocks=2.5, scar_color=None, flip_y=False,
                   patterns={"coat": "dapple", "coat_shade": "dapple", "mane": "strands",
                             "feather": "bars", "feather_edge": "bars"})
    print(f"костей: {info['bones']}  кубоидов: {info['cubes']}  render_scale: {info['scale']}")
    print(f"габариты в блоках: {info['size_blocks']} (x, y, z)")
    print("geo:", info["geo"])
    print("tex:", info["tex"])


if __name__ == "__main__":
    main()
