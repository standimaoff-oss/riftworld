#!/usr/bin/env python3
"""
v0.40.0 — <b>Морок</b> (`riftworld:morok`): собственный хоррор «Мороковой Пади» собран заново.

Пользователь трижды повторил: Вендиго не нужен, нужна СВОЯ тварь — резкая, которая не идёт
напролом, редко показывается днём, часто ночью, с ГОРЯЩИМИ глазами и очень страшная.

Почему модель переписана. В кадре 152 прогонов #404–#423 Морок выглядел серым «мешком»:
  * торс, «вуаль» и рёбра-полоски сливались в один блок, силуэт читался как человек;
  * голова была кубом 8×9×9 без маски, а угольки глаз — 2×3 пикселя, их не было видно;
  * руки прятались внутри «вуали», ноги — короткие и толстые.

Что теперь:
  * высокая (2.85 блока) измождённая фигура: длинные тонкие ноги с птичьим изгибом,
    узкая талия, костяное «коромысло» плеч, руки до колен с длинными когтями;
  * вместо лица — костяная маска-череп: надбровный валик, скулы, длинная челюсть,
    провалы глазниц и два раскалённых угля в них (отдельная геометрия `morok_eyes`);
  * рваный саван за спиной и по бокам: лоскуты разной длины, низ изорван;
  * рёбра нарисованы на коже (а не торчат светлыми полосками-конфетти, как в #423).

Запуск: python3 tools/rift/gen_v040_morok_model.py
"""
import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from geo_model_lib import Rig, ASSETS, write_png, hx  # noqa: E402
from tex_paint import MOROK as MOROK_PAINTERS  # noqa: E402

ATLAS = 256

SKIN = "#8A929B"          # пепельная кожа
SKIN_DARK = "#5A626C"
BONE = "#D8D2C0"          # костяная маска
BONE_DARK = "#A29A86"
SOCKET = "#070809"        # провал глазницы
CLOAK = "#1B2129"         # саван
CLOAK_LIGHT = "#2E3843"
CLAW = "#0A0B0D"
EYE = "#FF7A18"           # раскалённый уголь
EYE_CORE = "#FFE9A8"

MATS = {
    "skin": (SKIN, SKIN_DARK, SKIN, SKIN_DARK),
    "chest": (SKIN, SKIN_DARK, SKIN, SKIN_DARK),
    "skin_dark": (SKIN_DARK, "#3E4650", SKIN_DARK, "#3E4650"),
    "bone": (BONE, BONE_DARK, BONE, BONE_DARK),
    "bone_dark": (BONE_DARK, "#7C7466", BONE_DARK, "#7C7466"),
    "socket": (SOCKET, SOCKET, SOCKET, SOCKET),
    "cloak": (CLOAK_LIGHT, CLOAK, CLOAK, CLOAK),
    "claw": (CLAW, CLAW, CLAW, CLAW),
    "eye": (EYE, EYE, EYE, EYE),
}

SPECK = ["#6F7781", "#9BA3AC", "#4C545E"]

# Глазницы: череп занимает x=-3..3, поэтому угли стоят РОВНО в провалах: -3..-1 и 1..3.
EYE_Y = 45
EYE_Z = -10.6
HEAD_PIVOT = [0, 44, -4]


def build():
    r = Rig(ATLAS)

    # ------------------------------------------------------------------ торс
    r.bone("root", [0, 30, 0])
    r.cube([-4, 24, -3], [8, 5, 7], "skin", SPECK)             # таз
    r.cube([-3, 28, -4], [6, 5, 6], "skin", SPECK)             # талия (узкая)
    r.cube([-5, 32, -5], [10, 7, 8], "chest", SPECK)           # грудь (рёбра нарисованы)
    r.cube([-7, 37, -5], [14, 4, 7], "bone_dark")              # костяное «коромысло» плеч
    r.cube([-2, 40, -4], [4, 4, 4], "skin_dark")               # шея

    # ------------------------------------------------------------------ саван (рваный)
    r.bone("cloak", [0, 34, 2], {"axis": "y", "amp": 4.0, "speed": 1.2, "phase": 0.0})
    r.cube([-6, 18, 3], [12, 19, 3], "cloak")                  # спина
    for sx in (-1, 1):
        x = -9 if sx < 0 else 6
        r.cube([x, 20, -1], [3, 15, 6], "cloak")               # боковой лоскут
        r.cube([x, 14, 0], [3, 7, 5], "cloak")                 # свисающая полоса
        r.cube([x + (1 if sx < 0 else 0), 9, 1], [2, 6, 3], "cloak")   # рваная кромка
        r.cube([-8 if sx < 0 else 5, 30, -4], [3, 8, 4], "cloak")      # плечевой лоскут
    r.cube([-5, 16, 3], [3, 5, 2], "cloak")                    # низ изорван
    r.cube([1, 14, 3], [4, 6, 2], "cloak")

    # ------------------------------------------------------------------ руки (до колен, с когтями)
    for side, sx in (("l", 1), ("r", -1)):
        def cb(x, y, z, w, h, d, mat):
            r.cube([x if sx > 0 else -x - w, y, z], [w, h, d], mat)

        r.bone(f"arm_{side}", [sx * 8, 37, -2],
               {"axis": "x", "amp": sx * 9.0, "speed": 1.6, "phase": 0.0 if sx > 0 else 1.4})
        cb(6, 24, -4, 4, 14, 4, "skin")                        # плечо
        cb(7, 22, -4, 3, 4, 4, "skin_dark")                    # локоть
        cb(7, 13, -4, 3, 10, 3, "skin")                        # предплечье
        cb(6, 10, -5, 5, 4, 6, "skin_dark")                    # кисть
        for k in range(4):                                     # пальцы-когти
            cb(6 + k, 5, -7, 1, 6, 2, "claw")
        cb(6, 8, -8, 1, 3, 1, "claw")                          # коготь большого пальца

    # ------------------------------------------------------------------ ноги (длинные, «птичьи»)
    for side, sx in (("l", 1), ("r", -1)):
        def cb(x, y, z, w, h, d, mat):
            r.cube([x if sx > 0 else -x - w, y, z], [w, h, d], mat)

        r.bone(f"leg_{side}", [sx * 3, 24, 1],
               {"axis": "x", "amp": sx * 12.0, "speed": 1.9, "phase": 0.0 if sx > 0 else 1.5})
        cb(1, 12, -2, 4, 13, 4, "skin")                        # бедро
        cb(1, 10, -3, 4, 3, 5, "skin_dark")                    # колено вперёд
        cb(2, 3, 0, 3, 8, 3, "skin")                           # голень назад
        cb(1, 0, -4, 4, 3, 8, "skin_dark")                     # лапа
        for k in range(3):                                     # когти на лапе
            cb(1 + k * 2, 0, -7, 1, 2, 3, "claw")

    # ------------------------------------------------------------------ голова: костяная маска
    r.bone("head", HEAD_PIVOT, {"axis": "x", "amp": 6.0, "speed": 1.0, "phase": 0.9})
    r.cube([-3, 42, -9], [6, 7, 6], "bone")                    # череп
    r.cube([-4, 48, -10], [8, 2, 3], "bone")                   # надбровный валик
    r.cube([-4, 44, -10], [8, 2, 2], "bone")                   # скулы
    r.cube([-2, 40, -10], [4, 4, 6], "bone")                   # длинная челюсть
    r.cube([-2, 39, -11], [4, 1, 2], "bone_dark")              # подбородок
    r.cube([-1, 41, -11], [2, 2, 1], "socket")                 # провал между челюстями
    # провалы глазниц (в них светятся угли отдельной моделью)
    r.cube([-3, EYE_Y - 1, -10], [2, 3, 1], "socket")
    r.cube([1, EYE_Y - 1, -10], [2, 3, 1], "socket")
    # костяные «рожки» — силуэт перестаёт быть человеческим
    r.cube([-3, 49, -8], [1, 3, 3], "bone")
    r.cube([2, 49, -8], [1, 3, 3], "bone")
    r.cube([-1, 49, -7], [2, 2, 2], "bone_dark")
    # редкие зубы по краю челюсти
    for i in (-2, 0, 1):
        r.cube([i, 38, -10], [1, 2, 1], "bone")
    return r


def build_eyes():
    """Отдельная геометрия глаз: уголь + ореол, светится в полной темноте.

    Куб глаза крупнее провала на 1 юнит — в кадре видно ДВА горящих глаза, а не пиксель
    (в #423 светился один уголёк сбоку).
    """
    cubes = []
    for sx in (-1, 1):
        x = -3 if sx < 0 else 1
        cubes.append({"origin": [x, EYE_Y, EYE_Z - 0.6], "size": [2, 4, 1], "uv": [0, 0]})
    return {
        "texture_width": 16,
        "texture_height": 16,
        "render_scale": 1.0,
        "translucent": True,
        "bones": [{
            "name": "head",
            "pivot": HEAD_PIVOT,
            "anim": {"axis": "x", "amp": 6.0, "speed": 1.0, "phase": 0.9},
            "cubes": cubes,
        }],
    }


def eyes_texture():
    """16x16: раскалённый уголь. UV глаза = [0,0] — жар в левом верхнем углу."""
    img = []
    for y in range(16):
        row = []
        for x in range(16):
            if x < 5 and y < 6:
                c = hx(EYE_CORE) if (x < 2 and y < 2) else hx(EYE)
            elif x < 9 and y < 10:
                c = (140, 54, 12)
            else:
                c = (20, 10, 6)
            row.append((c[0], c[1], c[2], 255))
        img.append(row)
    return img


def main():
    r = build()
    info = r.write("morok", MATS, "textures/entity/morok.png", scale_to=1.0, length_axis=1,
                   length_blocks=2.85, scar_color=None, flip_y=False,
                   patterns=None, painters=MOROK_PAINTERS)
    eyes = build_eyes()
    # масштаб глаз ВСЕГДА как у тела — иначе угли съезжают с черепа
    eyes["render_scale"] = info["scale"]
    (ASSETS / "geo" / "morok_eyes.json").write_text(
        json.dumps(eyes, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    write_png(ASSETS / "textures" / "entity" / "morok_eyes.png", eyes_texture())
    print(f"костей: {info['bones']}  кубоидов: {info['cubes']}  render_scale: {info['scale']}")
    print(f"габариты в блоках: {info['size_blocks']} (x, y, z)")
    print("geo:", info["geo"])
    print("tex:", info["tex"])
    print("geo: assets/riftworld/geo/morok_eyes.json  tex: assets/riftworld/textures/entity/morok_eyes.png")


if __name__ == "__main__":
    main()
