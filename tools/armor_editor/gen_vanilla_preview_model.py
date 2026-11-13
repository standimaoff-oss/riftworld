#!/usr/bin/env python3
"""v0.52.0 — 3D-модель ВАНИЛЬНОЙ брони для превью в редакторе.

Мод мода теперь ванильный по геометрии: рендерится штатный HumanoidArmorLayer.
Значит, и превью в редакторе должно показывать РОВНО ту же геометрию, что в игре,
и накладывать на неё нарисованную карту брони 64×32.

Геометрия взята из ванильного PlayerModel.createMesh (Java) — она же в Bedrock
виде player_armor.json. Координаты в системе модели (1/16 блока), origin — левый
нижний угол куба:

    head       origin [-4, 24, -4] size [8, 8, 8]  uv [0, 0]
    hat        origin [-4, 24, -4] size [8, 8, 8]  uv [32, 0]  (inflate 0.5)
    body       origin [-4, 12, -2] size [8, 12, 4] uv [16, 16]
    right_arm  origin [-8, 12, -2] size [4, 12, 4] uv [40, 16]
    left_arm   origin [ 4, 12, -2] size [4, 12, 4] uv [40, 16] (mirror)
    right_leg  origin [-3.9, 0, -2] size [4, 12, 4] uv [0, 16]
    left_leg   origin [-0.1, 0, -2] size [4, 12, 4] uv [0, 16] (mirror)

Для превью нам нужен формат element-модели Minecraft (from/to + faces.uv),
поэтому пересчитываем origin/size в from/to и раскладываем UV граней по
Box-UV правилу Minecraft (up, down, west, north, east, south).

Запуск: python3 tools/armor_editor/gen_vanilla_preview_model.py
"""
from __future__ import annotations

import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
DATA = HERE / "data"

# части брони -> какие кости показывать (как в ванильном armor-моделі)
OUTER_PARTS = {
    # внешний слой: helmet/chestplate/boots — дилатация 1.0, показываем нужные кости
    "helmet":    {"show": ["head", "hat"], "inflate": 1.0},
    "chestplate": {"show": ["body", "right_arm", "left_arm"], "inflate": 1.0},
    "boots":     {"show": ["right_leg", "left_leg"], "inflate": 1.0},
}
# внутренний слой: поножи — дилатация 0.5
INNER_PARTS = {
    "leggings": {"show": ["right_leg", "left_leg"], "inflate": 0.5},
}

BONES = {
    "head":      {"origin": [-4.0, 24.0, -4.0], "size": [8.0, 8.0, 8.0], "uv": [0, 0],   "mirror": False},
    "hat":       {"origin": [-4.0, 24.0, -4.0], "size": [8.0, 8.0, 8.0], "uv": [32, 0],  "mirror": False},
    "body":      {"origin": [-4.0, 12.0, -2.0], "size": [8.0, 12.0, 4.0], "uv": [16, 16], "mirror": False},
    "right_arm": {"origin": [-8.0, 12.0, -2.0], "size": [4.0, 12.0, 4.0], "uv": [40, 16], "mirror": False},
    "left_arm":  {"origin": [4.0, 12.0, -2.0], "size": [4.0, 12.0, 4.0], "uv": [40, 16], "mirror": True},
    "right_leg": {"origin": [-3.9, 0.0, -2.0], "size": [4.0, 12.0, 4.0], "uv": [0, 16], "mirror": False},
    "left_leg":  {"origin": [-0.1, 0.0, -2.0], "size": [4.0, 12.0, 4.0], "uv": [0, 16], "mirror": True},
}

# Порядок граней в Box-UV Minecraft:
# up, down, west, north, east, south
FACE_ORDER = ["up", "down", "west", "north", "east", "south"]


def box_uv(u, v, w, h, d):
    """Раскладывает UV шести граней коробки по правилу Minecraft Box-UV."""
    return {
        "up":    [u + d, v, u + d + w, v + d],
        "down":  [u + d + w, v, u + d + w * 2, v + d],
        "west":  [u, v + d, u + d, v + d + h],
        "north": [u + d, v + d, u + d + w, v + d + h],
        "east":  [u + d + w, v + d, u + d + w + d, v + d + h],
        "south": [u + d + w + d, v + d, u + d + w * 2 + d, v + d + h],
    }


def make_element(bone, inflate):
    origin = bone["origin"]
    size = bone["size"]
    x0 = origin[0] - inflate
    y0 = origin[1] - inflate
    z0 = origin[2] - inflate
    x1 = origin[0] + size[0] + inflate
    y1 = origin[1] + size[1] + inflate
    z1 = origin[2] + size[2] + inflate
    u, v = bone["uv"]
    w, h, d = (int(size[0]), int(size[1]), int(size[2]))
    # зеркалим левые конечности: UV-регион зеркальный
    uv = box_uv(u, v, w, h, d)
    if bone["mirror"]:
        uv = {k: list(reversed(val)) for k, val in uv.items()}
    return {
        "name": bone.get("name", ""),
        "from": [round(x0, 3), round(y0, 3), round(z0, 3)],
        "to": [round(x1, 3), round(y1, 3), round(z1, 3)],
        "faces": {f: {"uv": uv[f], "texture": None} for f in FACE_ORDER},
    }


def build(part, spec):
    bones = BONES
    elements = []
    for bone_name in spec["show"]:
        b = dict(bones[bone_name])
        b["name"] = bone_name
        elements.append(make_element(b, spec["inflate"]))
    return {
        "texture_width": 64,
        "texture_height": 32,
        "elements": elements,
    }


def main() -> None:
    DATA.mkdir(parents=True, exist_ok=True)
    made = 0
    for part, spec in OUTER_PARTS.items():
        m = build(part, spec)
        path = DATA / f"vanilla_{part}.json"
        path.write_text(json.dumps(m, indent=1) + "\n", encoding="utf-8")
        print(f"  {path.name}: {len(m['elements'])} кубов")
        made += 1
    for part, spec in INNER_PARTS.items():
        m = build(part, spec)
        path = DATA / f"vanilla_{part}.json"
        path.write_text(json.dumps(m, indent=1) + "\n", encoding="utf-8")
        print(f"  {path.name}: {len(m['elements'])} кубов")
        made += 1
    print(f"OK: {made} ванильных моделей брони для превью")


if __name__ == "__main__":
    main()
