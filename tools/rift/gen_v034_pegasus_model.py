#!/usr/bin/env python3
"""
v0.34.0 → v0.39.11 — модель Пегаса (`riftworld:pegasus`): крылатый конь «земля/воздух».

Что изменилось в v0.39.11 после разбора кадра 147:
  * корпус собран из бочки, груди и крупа (а не из трёх плит), 1.9 блока в длину;
  * шея поднимается вперёд-вверх, голова выше холки, уши торчком, чёлка и борода;
  * ноги четырёхзвенные: бедро → голень → копыто, копыта чёрные;
  * крылья: костяная основа (плечо → предплечье → кисть) + четыре яруса узких маховых,
    прижатых к боку и уходящих назад к крупу — размах 1.5 блока, а не 6;
  * текстуры рисует `tex_paint.PEGASUS`: глаза с бликом на боках черепа, розовый нос с ноздрями,
    яблоки на шкуре, ряды перьев, прошивка и золотая пряжка на седле.

Запуск: python3 tools/rift/gen_v034_pegasus_model.py
"""
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from geo_model_lib import Rig  # noqa: E402
from tex_paint import PEGASUS  # noqa: E402

ATLAS = 256

COAT = "#F4F2EE"
COAT_DARK = "#D9D5CD"
COAT_SHADE = "#BFBAB0"
MANE = "#EDEAE4"
MANE_DARK = "#B9B4AA"
HOOF = "#2E2B27"
EYE = "#101318"
FEATHER = "#FCFBF9"
FEATHER_EDGE = "#CFCAC1"
NOSE = "#C09C95"
SADDLE = "#6B4A38"
SADDLE_TRIM = "#C9A24A"

MATS = {
    "coat": (COAT, COAT_DARK, COAT, COAT_SHADE),
    "leg": (COAT, COAT_SHADE, COAT, COAT_SHADE),
    "skull": (COAT, COAT_DARK, COAT, COAT_SHADE),
    "mane": (MANE, MANE_DARK, MANE, MANE_DARK),
    "tail": (MANE, MANE_DARK, MANE, MANE_DARK),
    "hoof": (HOOF, HOOF, HOOF, HOOF),
    "eye": (EYE, EYE, EYE, EYE),
    "nose": (NOSE, NOSE, NOSE, NOSE),
    "feather": (FEATHER, FEATHER_EDGE, FEATHER, FEATHER_EDGE),
    "feather_edge": (FEATHER_EDGE, COAT_SHADE, FEATHER_EDGE, COAT_SHADE),
    "saddle": (SADDLE, SADDLE_TRIM, SADDLE, SADDLE_TRIM),
}

SPECK = [COAT_DARK, COAT_SHADE, "#CFCAC2"]


def build():
    """Юнит = 1/16 блока, y=0 — земля под копытами, нос смотрит в −Z."""
    r = Rig(ATLAS)

    # ---------------- корпус: бочка + грудь + круп + спинной хребет
    r.bone("body", [0, 22, 0])
    r.cube([-5, 20, -14], [10, 9, 28], "coat", SPECK)        # бочка
    r.cube([-4, 28, -12], [8, 3, 24], "coat", SPECK)         # хребет
    r.cube([-6, 18, -15], [12, 9, 8], "coat", SPECK)         # грудь
    r.cube([-6, 19, 6], [12, 10, 9], "coat", SPECK)          # круп
    r.cube([-4, 16, -11], [8, 3, 22], "coat", SPECK)         # брюхо
    r.cube([-5, 29, -6], [10, 2, 12], "saddle")              # седло
    r.cube([-5, 31, -7], [10, 2, 2], "saddle")               # лука
    r.cube([-5, 31, 4], [10, 2, 2], "saddle")                # задняя лука
    for sx in (-1, 1):                                       # стремена
        r.cube([min(sx * 5, sx * 6), 24, -2], [1, 6, 2], "saddle")

    # ---------------- шея: вперёд-вверх, сужается к голове
    r.bone("neck", [0, 28, -14], {"axis": "x", "amp": 3.0, "speed": 0.8, "phase": 0.0})
    # v0.39.11: шея ВСТАЁТ (а не идёт вперёд): три сегмента вверх-вперёд, голова выше холки
    r.cube([-3, 28, -19], [7, 9, 7], "coat", SPECK)          # основание
    r.cube([-3, 33, -23], [6, 8, 6], "coat", SPECK)          # середина
    r.cube([-3, 38, -26], [6, 7, 6], "coat", SPECK)          # верх шеи
    for i in range(5):                                       # грива по верху шеи
        r.cube([-1, 37 + i, -27 + i * 2], [2, 3, 3], "mane")

    # ---------------- голова: череп, надбровье, морда, уши, чёлка, борода
    r.bone("head", [0, 41, -28], {"axis": "x", "amp": 3.0, "speed": 1.0, "phase": 0.6})
    r.cube([-3, 40, -33], [7, 8, 8], "skull", SPECK)         # череп (выше холки)
    r.cube([-4, 46, -32], [9, 2, 5], "skull")                # надбровье
    r.cube([-2, 36, -37], [6, 7, 6], "skull", SPECK)         # морда вниз-вперёд
    r.cube([-3, 47, -33], [2, 3, 2], "coat")                 # уши торчком
    r.cube([2, 47, -33], [2, 3, 2], "coat")
    r.cube([-1, 45, -31], [2, 3, 6], "mane")                 # чёлка
    r.cube([-1, 33, -36], [2, 4, 4], "mane")                 # борода

    # ---------------- ноги: бедро, голень, копыто
    for name, lx, lz, ph in (("leg_fl", -4, -9, 0.0), ("leg_fr", 4, -9, 1.6),
                             ("leg_bl", -4, 8, 0.4), ("leg_br", 4, 8, 2.0)):
        r.bone(name, [lx, 16, lz], {"axis": "x", "amp": 12.0, "speed": 2.2, "phase": ph})
        r.cube([lx - 3, 10, lz - 3], [6, 7, 6], "leg", SPECK)     # бедро
        r.cube([lx - 2, 3, lz - 2], [3, 8, 3], "leg")             # голень
        r.cube([lx - 2, 0, lz - 2], [4, 3, 5], "hoof")            # копыто

    # ---------------- хвост из трёх сегментов гривы
    r.bone("tail", [0, 24, 15], {"axis": "y", "amp": 7.0, "speed": 1.1, "phase": 0.4})
    r.cube([-2, 20, 15], [5, 6, 4], "tail")
    r.cube([-2, 14, 16], [5, 7, 4], "tail")
    r.cube([-2, 8, 17], [4, 7, 3], "tail")

    # ---------------- крылья: костяная основа вдоль бока + четыре яруса маховых
    for side, sx in (("l", 1), ("r", -1)):
        r.bone(f"wing_{side}", [sx * 5, 28, -4], {"axis": "z", "amp": sx * -6.0,
                                                  "speed": 1.4, "phase": 0.0})
        r.cube([min(sx * 5, sx * 7), 27, -7], [2, 5, 7], "feather_edge")   # плечо
        r.cube([min(sx * 5, sx * 7), 29, 0], [2, 4, 9], "feather_edge")    # предплечье
        r.cube([min(sx * 5, sx * 6), 30, 9], [2, 3, 6], "feather_edge")    # кисть
        for i in range(4):
            # маховые: узкие пластины, растут назад и вверх, каждая следующая длиннее
            r.cube([min(sx * 6 + i, sx * 7 + i), 23 + i, -3 + i * 4],
                   [1, 7 + i, 13 - i * 2], "feather")
            r.cube([min(sx * 7 + i, sx * 8 + i), 21 + i, 4 + i * 4],
                   [1, 6 + i, 10 - i], "feather_edge")
    return r


def main():
    r = build()
    info = r.write("pegasus", MATS, "textures/entity/pegasus.png", scale_to=1.0, length_axis=1,
                   length_blocks=2.5, scar_color=None, flip_y=False, painters=PEGASUS)
    print(f"костей: {info['bones']}  кубоидов: {info['cubes']}  render_scale: {info['scale']}")
    print(f"габариты в блоках: {info['size_blocks']} (x, y, z)")
    print("geo:", info["geo"])
    print("tex:", info["tex"])


if __name__ == "__main__":
    main()
