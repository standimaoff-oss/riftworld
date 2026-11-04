#!/usr/bin/env python3
"""
v0.39.3 — <b>Вендиго</b> пересобран заново (`riftworld:wendigo`).

Прошлая модель была почти чёрной (палитра #191614…#3A332C) и в игре стояла НА ГОЛОВЕ:
ось Y модели в Minecraft смотрит вниз, а геометрия писалась «вверх» (см. `flip_y` в Rig.write).

Что стало:
  * костяной цвет вместо угля: шкура из бурой палитры, кости светлые — силуэт читается в тумане;
  * неестественно вытянутые руки (до колен) и «птичьи» ноги, торс с выступающими рёбрами;
  * лосиный череп с рогами и ЧЁРНЫМИ глазницами; сами глаза — отдельная геометрия
    `geo/wendigo_eyes.json`, светится в темноте (рисует `client/WendigoRenderer`);
  * рваные лоскуты шкуры по плечам, узоры `bare` (просветы кожи) и `vein` (прожилки на костях).

Запуск: python3 tools/rift/gen_v039_wendigo_model.py
"""
import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from geo_model_lib import Rig, ASSETS, write_png, hx  # noqa: E402

ATLAS = 256

HIDE = "#4A3B2E"
HIDE_LIGHT = "#5E4C3A"
HIDE_DARK = "#33281F"
BONE = "#D9D0BA"
BONE_DARK = "#A79C84"
ANTLER = "#CBBFA2"
CLAW = "#14110E"
SOCKET = "#0B0908"
EYE = "#7CF0D8"          # холодный болотный огонь — вендиго светит иначе, чем Морок

MATS = {
    "hide": (HIDE_LIGHT, HIDE, HIDE, HIDE_DARK),
    "hide_dark": (HIDE, HIDE_DARK, HIDE_DARK, "#241B14"),
    "bone": (BONE, BONE_DARK, BONE, BONE_DARK),
    "antler": (ANTLER, BONE_DARK, ANTLER, BONE_DARK),
    "socket": (SOCKET, SOCKET, SOCKET, SOCKET),
    "claw": (CLAW, CLAW, CLAW, CLAW),
}

SPECK = ["#2A2118", "#6B5844", "#8C7A62"]

HEAD_PIVOT_UP = [0, 44, -9]      # в «человеческой» системе (до flip_y)


def build():
    r = Rig(ATLAS)

    # --- таз и торс: высокий, костлявый, рост ~44 юнита = 2.6 блока
    r.bone("root", [0, 28, 0])
    r.cube([-5, 22, -4], [10, 8, 7], "hide", SPECK)          # таз
    r.cube([-6, 31, -5], [12, 12, 8], "hide", SPECK)         # грудь
    r.cube([-7, 33, -4], [14, 3, 8], "hide_dark")            # плечевой горб
    r.cube([-1, 26, -6], [2, 9, 1], "bone")                  # грудина наружу
    # рёбра наружу: три пары светлых дуг по груди
    for yy in (28, 31, 34):
        r.cube([-6, yy, -6], [12, 1, 1], "bone")
        r.cube([-7, yy, -5], [1, 1, 2], "bone")
        r.cube([6, yy, -5], [1, 1, 2], "bone")
    # рваные лоскуты шкуры по бокам
    for sx in (-1, 1):
        r.cube([min(sx * 6, sx * 8), 24, -6], [3, 8, 9], "hide_dark", SPECK)
        r.cube([min(sx * 6, sx * 9), 19, -5], [3, 5, 6], "hide_dark")

    # --- шея: длинная, наклонена вперёд
    r.bone("neck", [0, 42, -3], {"axis": "x", "amp": 4.0, "speed": 0.8, "phase": 0.0})
    r.cube([-3, 38, -9], [6, 9, 6], "hide", SPECK)
    r.cube([-2, 44, -11], [4, 5, 4], "hide_dark")

    # --- голова: лосиный череп, чёрные глазницы, длинная морда
    r.bone("head", HEAD_PIVOT_UP, {"axis": "x", "amp": 6.0, "speed": 0.9, "phase": 0.7})
    r.cube([-4, 43, -17], [8, 7, 8], "bone", ["#C4BAA2", "#8D8371"])   # череп
    r.cube([-3, 42, -23], [6, 5, 6], "bone", ["#C4BAA2", "#8D8371"])   # морда
    r.cube([-2, 41, -25], [4, 2, 2], "hide_dark")                      # нос
    r.cube([-4, 45, -16], [2, 3, 3], "socket")                         # глазницы (свет рисует рендер)
    r.cube([2, 45, -16], [2, 3, 3], "socket")
    r.cube([-3, 40, -20], [6, 1, 3], "hide_dark")                      # пасть
    r.cube([-3, 40, -20], [1, 2, 1], "bone")                            # клыки
    r.cube([1, 40, -20], [1, 2, 1], "bone")
    r.cube([-3, 38, -18], [2, 2, 2], "hide_dark")                       # отвисшая челюсть

    # --- рога: два ствола и по три отростка
    r.bone("antlers", [0, 50, -13])
    for sx in (-1, 1):
        r.cube([min(sx * 2, sx * 6), 49, -14], [4, 2, 3], "antler")
        r.cube([min(sx * 6, sx * 9), 51, -15], [3, 2, 2], "antler")
        r.cube([min(sx * 9, sx * 12), 54, -16], [3, 2, 2], "antler")
        r.cube([min(sx * 5, sx * 7), 53, -18], [2, 5, 2], "antler")
        r.cube([min(sx * 9, sx * 11), 56, -13], [2, 4, 2], "antler")
        r.cube([min(sx * 7, sx * 9), 52, -10], [2, 3, 2], "antler")
        r.cube([min(sx * 11, sx * 13), 53, -14], [2, 3, 2], "antler")    # четвёртый отросток

    # --- руки: до колен, с когтями
    for side, sx in (("l", 1), ("r", -1)):
        r.bone(f"arm_{side}", [sx * 6, 40, -2], {"axis": "x", "amp": sx * 9.0, "speed": 1.1, "phase": 0.0})
        r.cube([min(sx * 6, sx * 10), 26, -4], [4, 15, 4], "hide", SPECK)    # плечо
        r.cube([min(sx * 7, sx * 10), 14, -4], [3, 13, 3], "hide_dark")      # предплечье
        r.cube([min(sx * 7, sx * 11), 9, -5], [4, 6, 4], "hide", SPECK)      # кисть
        for k in range(3):
            r.cube([min(sx * 7, sx * 12) + k, 6, -5 + k], [1, 4, 1], "claw")

    # --- ноги: «птичьи» — бедро, назад выгнутая голень, широкая лапа
    for side, sx in (("l", 1), ("r", -1)):
        r.bone(f"leg_{side}", [sx * 4, 22, 1], {"axis": "x", "amp": sx * 12.0, "speed": 1.6, "phase": 0.0})
        r.cube([min(sx * 1, sx * 5), 12, -2], [4, 11, 5], "hide", SPECK)     # бедро
        r.cube([min(sx * 1, sx * 4), 3, 1], [3, 10, 3], "hide_dark")         # голень назад
        r.cube([min(sx * 0, sx * 5), 0, -4], [5, 3, 8], "hide")              # лапа
        for k in range(3):
            r.cube([min(sx * 0, sx * 5) + k * 2, 0, -7], [1, 2, 3], "claw")
    return r


def build_eyes():
    """Светящиеся глаза вендиго: отдельная геометрия под RenderType.eyes."""
    cubes = []
    for x in (-4, 2):
        # v0.39.8: тело пишется «вверх», значит глаза — на высоте 45..48
        cubes.append({"origin": [x, 44, -16.4], "size": [3, 4, 1], "uv": [0, 0]})
    return {
        "texture_width": 16,
        "texture_height": 16,
        "render_scale": 0.723,
        "translucent": True,
        "bones": [{
            "name": "head",
            "pivot": [0, 44, -9],
            "anim": {"axis": "x", "amp": 6.0, "speed": 0.9, "phase": 0.7},
            "cubes": cubes,
        }],
    }


def eyes_texture():
    """16x16: болотный огонь — белое ядро, бирюзовый обод, тёмная кромка."""
    img = []
    for y in range(16):
        row = []
        for x in range(16):
            # v0.39.9: UV глаза = [0,0] (3x4 пикселя), поэтому жар должен быть В ЛЕВОМ ВЕРХНЕМ углу,
            # а не в центре 16x16 — иначе глаз рисовался тёмным пятном.
            if x < 4 and y < 5:
                c = hx("#F4FFF9") if (x < 2 and y < 2) else hx(EYE)
            elif x < 8 and y < 8:
                c = (28, 92, 80)
            else:
                c = (8, 22, 20)
            row.append((c[0], c[1], c[2], 255))
        img.append(row)
    return img


def main():
    r = build()
    info = r.write("wendigo", MATS, "textures/entity/wendigo.png", scale_to=1.0, length_axis=1,
                   length_blocks=2.62, scar_color=None, flip_y=False,
                   patterns={"hide": "bare", "hide_dark": "bare", "bone": "vein", "antler": "vein"})
    (ASSETS / "geo" / "wendigo_eyes.json").write_text(
        json.dumps(build_eyes(), ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    write_png(ASSETS / "textures" / "entity" / "wendigo_eyes.png", eyes_texture())
    print(f"костей: {info['bones']}  кубоидов: {info['cubes']}  render_scale: {info['scale']}")
    print(f"габариты в блоках: {info['size_blocks']} (x, y, z)")
    print("geo:", info["geo"])
    print("tex:", info["tex"])


if __name__ == "__main__":
    main()
