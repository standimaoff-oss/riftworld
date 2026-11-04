#!/usr/bin/env python3
"""
v0.39.0 — <b>Морок</b> (`riftworld:morok`): собственный хоррор-моб «Мороковой Пади».

Пользователь попросил НЕ вендиго, а свою тварь: резкую, которая не идёт прямо на игрока,
редко показывается днём, часто ночью, с ГОРЯЩИМИ глазами и очень страшную.

Модель:
  * высокая измождённая фигура ~2.4 блока, вытянутые руки до колен, «птичьи» ноги назад;
  * вместо лица — чёрная впадина черепа и два уголька глаз (отдельная модель `morok_eyes`,
    рисуется ванильным `RenderType.eyes` — светится в полной темноте);
  * рваная «вуаль» из лоскутов вокруг плеч и рёбра наружу — силуэт не читается как человек;
  * атлас 256x256, честные UV через `geo_model_lib.Rig`.

Запуск: python3 tools/rift/gen_v039_morok_model.py
"""
import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from geo_model_lib import Rig, ASSETS, write_png, hx  # noqa: E402
from tex_paint import MOROK as MOROK_PAINTERS  # noqa: E402

ATLAS = 256

VEIL = "#2F3841"
VEIL_LIGHT = "#46525C"
SKIN = "#6B7681"
SKIN_DARK = "#48525B"
BONE = "#CFC7B2"
BONE_DARK = "#9A917C"
EYE = "#FF7A18"          # раскалённый уголь — основа «горящих глаз»
EYE_CORE = "#FFE9A8"
CLAW = "#0E1012"

MATS = {
    "veil": (VEIL_LIGHT, VEIL, VEIL, VEIL),
    "skin": (SKIN, SKIN_DARK, SKIN, SKIN_DARK),
    "skin_dark": (SKIN_DARK, VEIL, SKIN_DARK, VEIL),
    "skull": (SKIN, SKIN_DARK, SKIN, SKIN_DARK),
    "bone": (BONE, BONE_DARK, BONE, BONE_DARK),
    "rib": ("#9C937E", "#7A7261", "#9C937E", "#7A7261"),
    "eye": (EYE, EYE, EYE, EYE),
    "claw": (CLAW, CLAW, CLAW, CLAW),
}

SPECK = ["#151A1F", "#6B7783", "#2E353C"]

# v0.39.11: «маска» кончается на z=-19, поэтому глазницы ставим ПЕРЕД ней (было -17 —
# угли утопали внутри черепа и в кадре #404 светился один пиксель).
EYE_Z = -19.6        # глубина глазниц в системе модели (нос смотрит в -Z)
HEAD_PIVOT = [0, 44, -9]        # до flip_y; в geo пишется как -44


def build():
    r = Rig(ATLAS)

    # --- торс: узкий, вытянутый, «голодный»; стоит на земле (y=0), рост ~38 юнитов
    r.bone("root", [0, 26, 0])
    r.cube([-5, 18, -4], [10, 8, 7], "veil", SPECK)            # таз
    r.cube([-6, 25, -5], [12, 12, 8], "veil", SPECK)           # грудь
    r.cube([-7, 30, -4], [14, 3, 7], "skin_dark")              # плечевой горб
    # рёбра: две пары коротких дуг по бокам груди, БЕЗ перемычки посередине
    # (v0.39.11: сплошные полосы через всю грудь читались как белый крест/буква «Т» на торсе)
    for yy in (26, 29, 32, 35):
        r.cube([-5, yy, -6], [4, 1, 1], "rib")
        r.cube([1, yy, -6], [4, 1, 1], "rib")

    # --- «вуаль»: лоскуты, свисающие с плеч (рваный силуэт, не человек)
    r.bone("veil", [0, 32, -2], {"axis": "y", "amp": 3.0, "speed": 1.4, "phase": 0.0})
    for i, sx in enumerate((-1, 1)):
        r.cube([min(sx * 6, sx * 11), 26, -7], [5, 7, 12], "veil", SPECK)   # плечи
        r.cube([min(sx * 7, sx * 12), 16, -6], [5, 10, 10], "veil", SPECK)  # свисающий лоскут
        r.cube([min(sx * 5, sx * 9), 10, -4], [4, 6, 6], "veil")            # рваная кромка
    r.cube([-9, 30, -9], [18, 2, 4], "skin_dark")                          # воротник

    # --- шея: короткая, втянута в плечи
    r.bone("neck", [0, 34, -3], {"axis": "x", "amp": 2.0, "speed": 1.1, "phase": 0.3})
    r.cube([-3, 33, -8], [6, 6, 6], "skin_dark")

    # --- голова: гладкая маска без лица, вместо глаз — угольки
    r.bone("head", HEAD_PIVOT, {"axis": "x", "amp": 5.0, "speed": 1.0, "phase": 0.9})
    r.cube([-4, 37, -16], [8, 9, 9], "skull")                  # череп: глаза и пасть рисует текстура
    r.cube([-3, 39, -19], [6, 5, 3], "skull")                  # «маска» вперёд
    r.cube([-3, 37, -19], [6, 2, 2], "skin_dark")              # чёрная пасть-щель
    r.cube([-4, 40, -18], [8, 1, 2], "rib")                    # костяная кромка надбровья
    # глазницы: тёмные провалы, угольки глаз светятся отдельной моделью morok_eyes
    r.cube([-3, 42, EYE_Z], [2, 3, 1], "claw")
    r.cube([1, 42, EYE_Z], [2, 3, 1], "claw")
    # оскал: редкие длинные зубы по краю пасти
    for i, sx in enumerate((-3, -1, 1)):
        r.cube([sx, 36, -20], [1, 2, 1], "rib")

    # --- руки: до колен, с длинными пальцами
    for side, sx in (("l", 1), ("r", -1)):
        r.bone(f"arm_{side}", [sx * 7, 31, -2], {"axis": "x", "amp": sx * 8.0, "speed": 1.6, "phase": 0.0})
        r.cube([min(sx * 6, sx * 10), 18, -4], [4, 14, 4], "skin", SPECK)    # плечо
        r.cube([min(sx * 6, sx * 9), 8, -4], [3, 11, 3], "skin_dark")        # предплечье
        r.cube([min(sx * 6, sx * 10), 5, -5], [4, 4, 4], "skin", SPECK)      # кисть
        for k in range(4):
            r.cube([min(sx * 6, sx * 11) + k, 0, -6 + k], [1, 6, 1], "claw")  # пальцы

    # --- ноги: «птичьи», колено выгнуто назад (силуэт, который не спутать)
    for side, sx in (("l", 1), ("r", -1)):
        r.bone(f"leg_{side}", [sx * 4, 18, 1], {"axis": "x", "amp": sx * 11.0, "speed": 1.9, "phase": 0.0})
        r.cube([min(sx * 1, sx * 5), 9, -3], [4, 10, 5], "skin", SPECK)      # бедро
        r.cube([min(sx * 1, sx * 5), 4, 1], [3, 8, 3], "skin_dark")          # голень (назад)
        r.cube([min(sx * 0, sx * 5), 0, -4], [5, 4, 8], "skin")              # лапа
        for k in range(3):
            r.cube([min(sx * 0, sx * 5) + k * 2, 0, -7], [1, 2, 3], "claw")
    return r


def build_eyes():
    """Отдельная геометрия глаз: светится в темноте через RenderType.eyes."""
    cubes = []
    # v0.39.13: череп занимает x=-4..4, значит угли стоят ровно в глазницах: -3..-1 и 1..3.
    # Раньше правый угол был на 2..6 и торчал за череп — в кадре светился «один глаз сбоку».
    for sx in (-1, 1):
        x = -3 if sx < 0 else 1
        # уголь: маленький куб + чуть больший ореол вокруг него
        # v0.39.8: модель пишется «вверх» (Y вверх), как и тело — глаза на высоте 42
        cubes.append({"origin": [x, 42, EYE_Z - 0.35], "size": [2, 3, 1], "uv": [0, 0]})
    return {
        "texture_width": 16,
        "texture_height": 16,
        "render_scale": 1.0,
        "translucent": True,
        "bones": [{
            "name": "head",
            "pivot": [0, 44, -9],
            "anim": {"axis": "x", "amp": 5.0, "speed": 1.0, "phase": 0.9},
            "cubes": cubes,
        }],
    }


def eyes_texture():
    """16x16: раскалённый уголь. UV глаза = [0,0], поэтому жар кладём в левый верхний угол."""
    img = []
    for y in range(16):
        row = []
        for x in range(16):
            if x < 5 and y < 5:
                c = hx(EYE_CORE) if (x < 2 and y < 2) else hx(EYE)
            elif x < 9 and y < 9:
                c = (120, 48, 10)
            else:
                c = (18, 10, 6)
            row.append((c[0], c[1], c[2], 255))
        img.append(row)
    return img


def main():
    r = build()
    info = r.write("morok", MATS, "textures/entity/morok.png", scale_to=1.0, length_axis=1,
                   length_blocks=2.42, scar_color=None, flip_y=False,
                   patterns=None, painters=MOROK_PAINTERS)
    eyes = build_eyes()
    # v0.39.11: масштаб глаз ВСЕГДА как у тела — иначе угли съезжают с черепа
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
