#!/usr/bin/env python3
"""
v0.49.0 — <b>Смотритель Пращи</b> (`riftworld:hollowwatcher`): новый хоррор-моб Пади.

Морок — бродячий охотник. Смотритель — «дайз-джам»: он ВЫСОКИЙ (3.4 блока),
худой, как шест, и стоит неподвижно, пока на него смотрят. Стоит отвести взгляд —
он оказывается в трёх шагах.

Модель:
  * очень высокая, узкая фигура ~3.4 блока: длинные ноги, длинный торс, вытянутая шея;
  * вместо головы — высокий «фонарь» из кости с двумя узкими щелями-глазами;
  * рваная «паутина» из тряпья между плечами и по бёдрам;
  * длинные руки-«веслы» с крючьями вместо пальцев;
  * бледно-серый ковр с тёмными прожилками — в тумане Пади силуэт виден как
    тёмная свеча между стволами Гнилой рощи;
  * отдельная модель глаз `hollowwatcher_eyes` светится через RenderType.eyes.

Запуск: python3 tools/rift/gen_v049_hollowwatcher_model.py
"""
import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from geo_model_lib import Rig, ASSETS, write_png, hx  # noqa: E402

ATLAS = 256

BONE = "#C8C4B6"
BONE_DARK = "#8C897E"
SKIN = "#7C8288"
SKIN_DARK = "#4A5056"
MOTH = "#2A2E33"
MOTH_LIGHT = "#3E444A"
RIM = "#A8AEB2"
EYE = "#7FE8F0"          # ледяной голубой — «холодный свет» Пращи
EYE_CORE = "#EAFEFF"
CLAW = "#141618"

MATS = {
    "bone": (BONE, BONE_DARK, BONE, BONE_DARK),
    "skin": (SKIN, SKIN_DARK, SKIN, SKIN_DARK),
    "skin_dark": (SKIN_DARK, MOTH, SKIN_DARK, MOTH),
    "moth": (MOTH_LIGHT, MOTH, MOTH_LIGHT, MOTH),
    "rim": (RIM, BONE_DARK, RIM, BONE_DARK),
    "claw": (CLAW, CLAW, CLAW, CLAW),
}

SPECK = ["#1A1D20", "#6E747A", "#2B3034"]

EYE_Z = -12
HEAD_PIVOT = [0, 58, -6]


def build():
    r = Rig(ATLAS)

    # --- торс: вытянутый, узкий, «деревянный»; рост модели ~55 юнитов (3.4 блока)
    r.bone("root", [0, 36, 0])
    r.cube([-4, 24, -3], [8, 8, 6], "moth", SPECK)             # таз
    r.cube([-5, 31, -4], [10, 8, 7], "skin", SPECK)            # грудь
    r.cube([-6, 38, -3], [12, 3, 6], "skin_dark")              # плечевой пояс
    # рёбра: видны сквозь кожу
    for yy in (32, 35, 38):
        r.cube([-5, yy, -5], [10, 1, 1], "bone")

    # --- «паутина»: лоскуты тряпья от плеч и вдоль бёдер
    r.bone("shroud", [0, 39, -1], {"axis": "y", "amp": 2.4, "speed": 1.2, "phase": 0.0})
    for sx in (-1, 1):
        r.cube([min(sx * 5, sx * 10), 33, -6], [5, 7, 11], "moth", SPECK)
        r.cube([min(sx * 6, sx * 11), 22, -5], [5, 12, 9], "moth", SPECK)
        r.cube([min(sx * 4, sx * 9), 14, -3], [5, 9, 6], "moth")

    # --- шея: длинная, тонкая, втянута
    r.bone("neck", [0, 40, -3], {"axis": "x", "amp": 1.6, "speed": 0.9, "phase": 0.4})
    r.cube([-2, 40, -5], [4, 12, 4], "skin_dark")

    # --- голова: «фонарь» из кости, две узкие щели глаз
    r.bone("head", HEAD_PIVOT, {"axis": "x", "amp": 3.5, "speed": 0.8, "phase": 1.0})
    r.cube([-4, 46, -13], [8, 12, 8], "bone", SPECK)           # высокий череп-фонарь
    r.cube([-5, 58, -12], [10, 2, 7], "rim")                   # ободок фонаря
    r.cube([-3, 49, -16], [6, 6, 3], "bone", SPECK)            # «нос»-клин вперёд
    r.cube([-4, 47, -15], [8, 2, 2], "skin_dark")              # щель пасти
    # глазницы-щели
    r.cube([-4, 52, EYE_Z], [3, 4, 1], "claw")
    r.cube([1, 52, EYE_Z], [3, 4, 1], "claw")
    # рёбра-зубы вдоль нижней кромки «фонаря»
    for sx in (-3, -1, 1):
        r.cube([sx, 45, -14], [1, 3, 1], "bone")

    # --- руки: очень длинные, «весло» с крючьями
    for side, sx in (("l", 1), ("r", -1)):
        r.bone(f"arm_{side}", [sx * 8, 37, -2], {"axis": "x", "amp": sx * 5.0, "speed": 1.3, "phase": 0.0})
        r.cube([min(sx * 7, sx * 11), 24, -4], [4, 14, 4], "skin", SPECK)      # плечо
        r.cube([min(sx * 7, sx * 10), 10, -4], [3, 15, 3], "skin_dark")        # предплечье
        r.cube([min(sx * 6, sx * 10), 4, -5], [4, 7, 4], "skin", SPECK)        # кисть
        for k in range(4):
            r.cube([min(sx * 6, sx * 11) + k, 0, -6 + k], [1, 6, 1], "claw")   # крючья

    # --- ноги: длинные, тонкие, чуть согнуты
    for side, sx in (("l", 1), ("r", -1)):
        r.bone(f"leg_{side}", [sx * 4, 24, 1], {"axis": "x", "amp": sx * 7.0, "speed": 1.6, "phase": 0.0})
        r.cube([min(sx * 1, sx * 5), 12, -3], [4, 13, 5], "skin", SPECK)       # бедро
        r.cube([min(sx * 1, sx * 5), 4, 0], [3, 9, 3], "skin_dark")            # голень
        r.cube([min(sx * 0, sx * 5), 0, -4], [5, 4, 8], "skin")                # ступня
        for k in range(3):
            r.cube([min(sx * 0, sx * 5) + k * 2, 0, -7], [1, 2, 3], "claw")
    return r


def build_eyes():
    """Отдельная геометрия глаз: светится в темноте через RenderType.eyes."""
    cubes = []
    for x in (-4, 1):
        cubes.append({"origin": [x, -58, EYE_Z - 0.2], "size": [3, 4, 1], "uv": [0, 0]})
    return {
        "texture_width": 16,
        "texture_height": 16,
        "render_scale": 1.0,
        "translucent": True,
        "bones": [{
            "name": "head",
            "pivot": [0, -58, -6],
            "anim": {"axis": "x", "amp": 3.5, "speed": 0.8, "phase": 1.0},
            "cubes": cubes,
        }],
    }


def eyes_texture():
    """16x16: ледяная щель — белое ядро, голубой обод, тёмная кромка."""
    img = []
    for y in range(16):
        row = []
        for x in range(16):
            dx = abs(x - 7.5)
            dy = abs(y - 7.5)
            if dx < 2.0 and dy < 4.5:
                c = hx(EYE_CORE)
            elif dx < 3.6 and dy < 6.2:
                c = hx(EYE)
            elif dx < 5.0 and dy < 7.0:
                c = (30, 92, 104)
            else:
                c = (12, 26, 30)
            row.append((c[0], c[1], c[2], 255))
        img.append(row)
    return img


def main():
    r = build()
    info = r.write("hollowwatcher", MATS, "textures/entity/hollowwatcher.png", scale_to=1.0, length_axis=1,
                   length_blocks=3.40, scar_color=None, flip_y=True,
                   patterns={"skin": "felt", "moth": "bare", "bone": "vein"})
    eyes = build_eyes()
    (ASSETS / "geo" / "hollowwatcher_eyes.json").write_text(
        json.dumps(eyes, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    write_png(ASSETS / "textures" / "entity" / "hollowwatcher_eyes.png", eyes_texture())
    print(f"костей: {info['bones']}  кубоидов: {info['cubes']}  render_scale: {info['scale']}")
    print(f"габариты в блоках: {info['size_blocks']} (x, y, z)")
    print("geo:", info["geo"])
    print("tex:", info["tex"])
    print("geo: assets/riftworld/geo/hollowwatcher_eyes.json  tex: assets/riftworld/textures/entity/hollowwatcher_eyes.png")


if __name__ == "__main__":
    main()
