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
    """v0.39.9: конь, собранный «как конь».

    Разбор кадра 147 (прогон #402): крылья лежали на спине плоскими плитами в 11 юнитов
    шириной, шея шла горизонтально, и всё это читалось как куча палок. Теперь:
    корпус 30 юнитов (1.9 блока) в длину, шея поднимается вперёд-вверх, голова выше холки,
    а крылья — три узких яруса перьев, прижатых к боку и уходящих назад к крупу.
    """
    r = Rig(ATLAS)

    # ---------------- корпус
    r.bone("body", [0, 22, 0])
    r.cube([-5, 22, -15], [10, 4, 30], "coat", SPECK)        # спина
    r.cube([-7, 14, -15], [14, 9, 30], "coat", SPECK)        # бока
    r.cube([-5, 11, -13], [10, 4, 26], "coat_shade", SPECK)  # брюхо
    r.cube([-6, 25, -10], [12, 2, 12], "saddle")             # седло
    r.cube([-6, 27, -10], [12, 1, 3], "saddle")              # лука
    r.cube([-6, 27, 1], [12, 1, 3], "saddle")                # задняя лука

    # ---------------- шея: вперёд-вверх (а не вперёд)
    r.bone("neck", [0, 24, -14], {"axis": "x", "amp": 3.0, "speed": 0.8, "phase": 0.0})
    r.cube([-4, 22, -19], [8, 10, 6], "coat", SPECK)         # основание шеи
    r.cube([-3, 27, -23], [7, 9, 6], "coat", SPECK)          # верх шеи
    # грива по верху шеи
    for i in range(4):
        r.cube([-1, 31 + i, -24 + i * 2], [2, 3, 3], "mane")

    # ---------------- голова: череп, морда, уши, глаза
    r.bone("head", [0, 33, -24], {"axis": "x", "amp": 3.0, "speed": 1.0, "phase": 0.6})
    r.cube([-3, 30, -29], [7, 8, 7], "coat", SPECK)          # череп
    r.cube([-2, 27, -34], [6, 6, 6], "coat", SPECK)          # морда
    r.cube([-2, 27, -36], [4, 4, 2], "nose")                 # нос
    r.cube([-4, 32, -27], [1, 3, 3], "eye")                  # глаза
    r.cube([4, 32, -27], [1, 3, 3], "eye")
    r.cube([-3, 37, -28], [1, 4, 2], "coat")                 # уши
    r.cube([3, 37, -28], [1, 4, 2], "coat")
    r.cube([-1, 34, -26], [2, 3, 5], "mane")                 # чёлка
    r.cube([-1, 25, -32], [2, 3, 4], "mane")                 # борода

    # ---------------- ноги: бедро, голень, копыто
    for name, lx, lz, top, knee in (("leg_fl", -5, -10, 16, 7), ("leg_fr", 5, -10, 16, 7),
                                    ("leg_bl", -5, 10, 16, 8), ("leg_br", 5, 10, 16, 8)):
        r.bone(name, [lx, top, lz], {"axis": "x", "amp": 12.0, "speed": 2.2,
                                     "phase": 0.0 if name.endswith("l") else 1.6})
        r.cube([lx - 2, knee, lz - 2], [5, top - knee + 1, 5], "coat", SPECK)   # бедро
        r.cube([lx - 1, 2, lz - 1], [3, knee - 1, 3], "coat_shade")             # голень
        r.cube([lx - 2, 0, lz - 2], [5, 2, 5], "hoof")                          # копыто

    # ---------------- хвост
    r.bone("tail", [0, 22, 15], {"axis": "y", "amp": 7.0, "speed": 1.1, "phase": 0.4})
    r.cube([-2, 18, 15], [5, 6, 4], "mane")
    r.cube([-2, 12, 16], [5, 7, 4], "mane")
    r.cube([-2, 6, 17], [4, 7, 3], "mane")

    # ---------------- крылья: СЛОЖЕНЫ вдоль бока, три яруса перьев уходят к крупу
    for side, sx in (("l", 1), ("r", -1)):
        r.bone(f"wing_{side}", [sx * 6, 26, -6], {"axis": "z", "amp": sx * -6.0,
                                                  "speed": 1.4, "phase": 0.0})
        # костяная основа: плечо у холки, предплечье вдоль спины, кисть у крупа
        r.cube([min(sx * 6, sx * 8), 24, -8], [2, 6, 8], "feather")             # плечо
        r.cube([min(sx * 6, sx * 8), 26, 0], [2, 5, 9], "feather_edge")         # предплечье
        r.cube([min(sx * 6, sx * 7), 27, 9], [2, 4, 6], "feather_edge")         # кисть
        # три яруса маховых: узкие пластины, растущие назад и вверх
        for i in range(3):
            r.cube([min(sx * 7 + i, sx * 8 + i), 22 + i * 2, -4 + i * 4],
                   [1, 6 + i * 2, 14 - i * 2], "feather")
            r.cube([min(sx * 8 + i, sx * 9 + i), 20 + i * 2, 3 + i * 4],
                   [1, 5 + i * 2, 12 - i * 2], "feather_edge")
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
