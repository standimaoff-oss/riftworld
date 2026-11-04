#!/usr/bin/env python3
"""
v0.35.0 — модель Вендиго (`riftworld:wendigo`), хозяина «Мороковой Пади».

По референсу `reports/biome_concept_morok_hollow/02_night_horror_wendigo.jpg`:
высокая измождённая фигура на длинных ногах, непропорционально длинные руки,
лосиный череп с ветвистыми рогами, рёбра наружу, тёмная шкура с сединой.

Модель: 13 костей, атлас 256x256, честные UV (см. `geo_model_lib.Rig`).
"""
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from geo_model_lib import Rig  # noqa: E402

ATLAS = 256

HIDE = "#2B2622"
HIDE_LIGHT = "#3A332C"
HIDE_DARK = "#191614"
BONE = "#D8CFB8"
BONE_DARK = "#AFA68F"
ANTLER = "#C6BBA0"
EYE = "#8FF0F0"
CLAW = "#1A1613"

MATS = {
    "hide": (HIDE_LIGHT, HIDE, HIDE, HIDE_DARK),
    "hide_dark": (HIDE, HIDE_DARK, HIDE_DARK, HIDE_DARK),
    "bone": (BONE, BONE_DARK, BONE, BONE_DARK),
    "antler": (ANTLER, BONE_DARK, ANTLER, BONE_DARK),
    "eye": (EYE, EYE, EYE, EYE),
    "claw": (CLAW, CLAW, CLAW, CLAW),
}

SPECK = ["#4A423A", "#15120F"]


def build():
    r = Rig(ATLAS)

    # --- таз/торс: стоит на земле (y=0), рост ~42 юнита = 2.6 блока
    r.bone("root", [0, 30, 0])
    r.cube([-5, 22, -4], [10, 10, 8], "hide", SPECK)        # таз
    r.cube([-6, 30, -5], [12, 12, 9], "hide", SPECK)        # грудь
    r.cube([-7, 32, -4], [14, 2, 7], "hide_dark")           # плечевой горб
    r.cube([-1, 24, -6], [2, 9, 1], "bone")                 # грудина наружу

    # --- рёбра (торчат из кожи)
    for i, yy in enumerate((29, 32, 35)):
        r.cube([-6, yy, -6], [12, 1, 1], "bone")

    # --- шея: длинная, наклонена вперёд
    r.bone("neck", [0, 40, -3], {"axis": "x", "amp": 4.0, "speed": 0.8, "phase": 0.0})
    r.cube([-3, 38, -8], [6, 8, 6], "hide", SPECK)
    r.cube([-2, 43, -10], [4, 5, 4], "hide_dark")

    # --- голова: лосиный череп с длинной мордой и пустыми глазницами
    r.bone("head", [0, 44, -9], {"axis": "x", "amp": 6.0, "speed": 0.9, "phase": 0.7})
    r.cube([-3, 43, -16], [6, 6, 8], "bone")                 # череп
    r.cube([-2, 42, -21], [4, 4, 5], "bone")                 # морда
    r.cube([-2, 41, -22], [4, 2, 1], "hide_dark")            # нос
    r.cube([-4, 46, -15], [1, 2, 2], "eye")                  # пустые светящиеся глазницы
    r.cube([3, 46, -15], [1, 2, 2], "eye")
    r.cube([-3, 41, -18], [6, 1, 2], "hide_dark")            # пасть

    # --- рога: 2 ствола + 3 отростка на каждом
    r.bone("antlers", [0, 49, -13])
    for sx in (-1, 1):
        r.cube([min(sx * 2, sx * 6), 49, -14], [4, 2, 3], "antler")       # основание
        r.cube([min(sx * 6, sx * 9), 50, -15], [3, 2, 2], "antler")       # изгиб
        r.cube([min(sx * 9, sx * 12), 52, -16], [3, 2, 2], "antler")      # вверх
        r.cube([min(sx * 5, sx * 7), 52, -18], [2, 5, 2], "antler")       # отросток вперёд
        r.cube([min(sx * 9, sx * 11), 54, -13], [2, 4, 2], "antler")      # отросток вверх
        r.cube([min(sx * 7, sx * 9), 51, -10], [2, 3, 2], "antler")       # отросток назад

    # --- руки: плечо, предплечье, кисть с когтями (до колен — как у Вендиго)
    for side, sx in (("l", 1), ("r", -1)):
        r.bone(f"arm_{side}", [sx * 6, 40, -2], {"axis": "x", "amp": sx * 9.0, "speed": 1.1, "phase": 0.0})
        r.cube([min(sx * 6, sx * 10), 26, -4], [4, 15, 4], "hide", SPECK)      # плечо
        r.cube([min(sx * 7, sx * 10), 14, -4], [3, 13, 3], "hide_dark")        # предплечье
        r.cube([min(sx * 7, sx * 11), 9, -5], [4, 6, 4], "hide", SPECK)        # кисть
        for k in range(3):
            r.cube([min(sx * 7, sx * 12) + k, 6, -5 + k], [1, 4, 1], "claw")   # когти

    # --- ноги: бедро, голень, стопа (длинные, «звериные»)
    for side, sx in (("l", 1), ("r", -1)):
        r.bone(f"leg_{side}", [sx * 4, 22, 1], {"axis": "x", "amp": sx * 12.0, "speed": 1.6, "phase": 0.0})
        r.cube([min(sx * 1, sx * 5), 12, -2], [4, 11, 5], "hide", SPECK)       # бедро
        r.cube([min(sx * 2, sx * 5), 3, -1], [3, 10, 3], "hide_dark")          # голень
        r.cube([min(sx * 1, sx * 6), 0, -4], [5, 3, 7], "hide")                # стопа
        for k in range(3):
            r.cube([min(sx * 1, sx * 6) + k * 2, 0, -6], [1, 2, 2], "claw")    # когти на лапе
    return r


def main():
    r = build()
    info = r.write("wendigo", MATS, "textures/entity/wendigo.png", scale_to=1.0, length_axis=1,
                   length_blocks=2.62, scar_color=None)
    print(f"костей: {info['bones']}  кубоидов: {info['cubes']}  render_scale: {info['scale']}")
    print(f"габариты в блоках: {info['size_blocks']} (x, y, z)")
    print("geo:", info["geo"])
    print("tex:", info["tex"])


if __name__ == "__main__":
    main()
