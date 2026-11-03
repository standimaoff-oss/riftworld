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

COAT = "#F2F0EC"
COAT_DARK = "#D9D6D0"
MANE = "#E3E0DA"
MANE_DARK = "#C7C3BB"
HOOF = "#3A3733"
EYE = "#14161A"
FEATHER = "#FBF9F6"
FEATHER_EDGE = "#CFCBC4"
NOSE = "#C8A9A2"

MATS = {
    "coat": (COAT, COAT_DARK, COAT, COAT_DARK),
    "coat_dark": (COAT_DARK, COAT_DARK, COAT_DARK, COAT_DARK),
    "mane": (MANE, MANE_DARK, MANE, MANE_DARK),
    "hoof": (HOOF, HOOF, HOOF, HOOF),
    "eye": (EYE, EYE, EYE, EYE),
    "nose": (NOSE, NOSE, NOSE, NOSE),
    "feather": (FEATHER, FEATHER_EDGE, FEATHER, FEATHER_EDGE),
    "feather_edge": (FEATHER_EDGE, FEATHER, FEATHER_EDGE, FEATHER),
}

SPECK = [COAT_DARK, "#C9C5BE"]


def build():
    r = Rig(ATLAS)

    # --- корпус (стоит: y=0 — земля под копытами, ноги вниз-вверх от 24 до 0)
    r.bone("body", [0, 16, 0])
    r.cube([-6, 8, -11], [12, 12, 24], "coat", SPECK)       # грудь/живот
    r.cube([-5, 7, 9], [10, 10, 8], "coat", SPECK)          # круп
    r.cube([-6, 6, -11], [12, 2, 8], "coat_dark")           # грудина

    # --- шея и голова (смотрят в -Z)
    r.bone("neck", [0, 18, -10], {"axis": "x", "amp": 3.0, "speed": 0.7, "phase": 0.0})
    r.cube([-4, 10, -17], [8, 12, 8], "coat", SPECK)
    r.cube([-3, 9, -21], [6, 9, 5], "coat", SPECK)

    r.bone("head", [0, 18, -20], {"axis": "x", "amp": 2.5, "speed": 0.9, "phase": 0.6})
    r.cube([-3, 12, -27], [6, 7, 8], "coat", SPECK)         # череп
    r.cube([-2, 13, -31], [4, 4, 4], "coat")                # морда
    r.cube([-2, 13, -32], [4, 2, 1], "nose")                # нос
    r.cube([-4, 15, -25], [1, 2, 2], "eye")                 # глаза
    r.cube([3, 15, -25], [1, 2, 2], "eye")
    r.cube([-1, 10, -26], [2, 4, 3], "mane")                # борода-подвес
    # уши
    r.cube([-3, 18, -25], [1, 3, 2], "coat")
    r.cube([2, 18, -25], [1, 3, 2], "coat")
    # грива вдоль шеи
    for i, (yy, zz) in enumerate(((19, -18), (20, -16), (21, -14), (21, -12), (20, -10))):
        r.cube([-1, yy, zz], [2, 3, 3], "mane")

    # --- ноги: бедро + голень + копыто (передние/задние)
    legs = (("leg_fl", -4, -8), ("leg_fr", 4, -8), ("leg_bl", -4, 7), ("leg_br", 4, 7))
    for name, lx, lz in legs:
        r.bone(name, [lx, 16, lz], {"axis": "x", "amp": 14.0 if "f" in name[4] else -14.0,
                                    "speed": 2.2, "phase": 0.0 if lx < 0 else 1.6})
        r.cube([lx - 2, 8, lz - 2], [4, 10, 4], "coat", SPECK)     # бедро
        r.cube([lx - 1, 2, lz - 1], [2, 7, 2], "coat_dark")        # голень
        r.cube([lx - 2, 0, lz - 2], [4, 2, 4], "hoof")             # копыто

    # --- хвост: 2 сегмента + кисть
    r.bone("tail", [0, 16, 16], {"axis": "y", "amp": 6.0, "speed": 1.1, "phase": 0.4})
    r.cube([-2, 14, 15], [4, 6, 5], "mane")
    r.cube([-2, 8, 18], [4, 8, 4], "mane")
    r.cube([-1, 3, 20], [2, 6, 3], "mane")

    # --- крылья: плечо + предплечье + кисть + 4 маховых пера (широкие, «веером»)
    for side, sx in (("l", 1), ("r", -1)):
        r.bone(f"wing_{side}", [sx * 5, 15, -6], {"axis": "z", "amp": sx * -16.0, "speed": 1.8, "phase": 0.0})
        r.cube([min(sx * 5, sx * 13), 15, -9], [8, 3, 8], "feather")          # плечо
        r.cube([min(sx * 13, sx * 23), 16, -8], [10, 3, 7], "feather")        # предплечье
        r.cube([min(sx * 23, sx * 31), 17, -6], [8, 2, 5], "feather_edge")    # кисть
        for i in range(4):
            fx = min(sx * 31, sx * (31 + 3 + i * 3))
            r.cube([fx, 17 + i, 0 - i * 2], [7 + i, 2, 7], "feather_edge")     # маховые
        r.cube([min(sx * 5, sx * 15), 13, 4], [10, 2, 8], "feather")          # нижние кроющие
    return r


def main():
    r = build()
    info = r.write("pegasus", MATS, "textures/entity/pegasus.png", scale_to=1.0, length_axis=1,
                   length_blocks=1.75, scar_color=None)
    print(f"костей: {info['bones']}  кубоидов: {info['cubes']}  render_scale: {info['scale']}")
    print(f"габариты в блоках: {info['size_blocks']} (x, y, z)")
    print("geo:", info["geo"])
    print("tex:", info["tex"])


if __name__ == "__main__":
    main()
