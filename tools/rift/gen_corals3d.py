#!/usr/bin/env python3
"""Рев 11 — новые КАСТОМНЫЕ КОРАЛЛЫ как настоящие растения (3D-модели из коробок, не плоский крестик).

Что делает:
  1. рисует 16x16 текстуры (PIL) — четыре оттенка в квадрантах, чтобы грани коробок играли светом;
  2. собирает `models/block/<name>.json` из настоящих элементов (ветки под углами, пластины, трубки);
  3. пишет blockstates / item-модели / loot / lang (ru+en) — только для новых блоков, старое не трогает.

Старые кораллы (`coral_branch`, `coral_fan`, `coral_tube`, `coral_glow`, `anemone`, `coral_bush`,
`tube_worms`) остаются как были: скрипт их не перезаписывает.
"""
import json
import math
import os
import random

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
R = os.path.join(ROOT, "mod/src/main/resources")
A = R + "/assets/riftworld"
D = R + "/data/riftworld"
NS = "riftworld:block/"

# ---------------------------------------------------------------- палитры
# base — тело ветки, lit — освещённая грань, dark — тень, tip — кончик/полипы
PALETTES = {
    "staghorn_coral": ((88, 150, 96), (146, 208, 132), (52, 104, 68), (247, 232, 168)),
    "feather_coral": ((178, 62, 132), (232, 118, 176), (118, 34, 92), (255, 214, 236)),
    "sea_whip": ((104, 74, 168), (158, 126, 224), (66, 44, 116), (232, 208, 255)),
    "table_coral": ((206, 116, 58), (244, 168, 96), (146, 74, 34), (255, 232, 186)),
    "tube_sponge_cluster": ((196, 168, 48), (246, 222, 104), (140, 116, 24), (255, 250, 204)),
    "ribbon_coral": ((52, 152, 168), (110, 208, 214), (28, 104, 120), (214, 250, 250)),
}


def texture(name, pal):
    """16x16: четыре квадранта-оттенка + прожилки, чтобы коробки не выглядели пластиком."""
    base, lit, dark, tip = pal
    rnd = random.Random(hash(name) & 0xFFFF)
    img = Image.new("RGBA", (16, 16), base + (255,))
    px = img.load()
    quads = {(0, 0): lit, (1, 0): base, (0, 1): base, (1, 1): dark}
    for qx in range(2):
        for qy in range(2):
            c = quads[(qx, qy)]
            for x in range(qx * 8, qx * 8 + 8):
                for y in range(qy * 8, qy * 8 + 8):
                    px[x, y] = c + (255,)
    # прожилки-полипы: короткие штрихи по всем квадрантам
    for _ in range(46):
        x, y = rnd.randrange(16), rnd.randrange(16)
        c = tip if rnd.random() < 0.35 else (lit if rnd.random() < 0.5 else dark)
        px[x, y] = c + (255,)
    for _ in range(10):                       # тёмные бороздки
        x = rnd.randrange(16)
        for y in range(rnd.randrange(4), 16, 2):
            px[x, y] = dark + (255,)
    return img


def uv(height_frac, face_top=False):
    """Квадрант текстуры: верх растения — светлее, низ — тень. Возвращает [u1,v1,u2,v2]."""
    if face_top:
        q = 0
    elif height_frac > 0.66:
        q = 0
    elif height_frac > 0.33:
        q = 1
    else:
        q = 3
    u = 8 * (q % 2)
    v = 8 * (q // 2)
    return [u, v, u + 8, v + 8]


def cube(frm, to, hfrac, rot=None, tex="#t", all_faces=True):
    """Один элемент модели. hfrac — доля высоты (для выбора оттенка квадранта)."""
    faces = {}
    for f in ("north", "south", "east", "west", "up", "down"):
        if not all_faces and f in ("up", "down"):
            continue
        faces[f] = {"uv": uv(hfrac, face_top=(f == "up")), "texture": tex}
    el = {"from": [round(v, 2) for v in frm], "to": [round(v, 2) for v in to], "faces": faces}
    if rot:
        el["rotation"] = {"origin": [round(v, 2) for v in rot[0]],
                          "axis": rot[1], "angle": round(rot[2], 1), "rescale": True}
    return el


def branch(elements, x, y, z, length, thick, tilt_deg, heading_deg, hfrac0=0.35):
    """Ветка: коробка, повёрнутая вокруг основания по двум осям — настоящий рог, а не кубик."""
    h = thick
    frm = [x - thick / 2, y, z - thick / 2]
    to = [x + thick / 2, y + length, z + thick / 2]
    # наклон по heading: сначала вокруг X (наклон в плоскости ZY), потом вокруг Y (поворот)
    elements.append(cube(frm, to, hfrac0, rot=([x, y, z], "x", tilt_deg)))
    # кончик — короткая светлая коробка сверху ветки (визуально «полип»)
    tx = x + math.sin(math.radians(heading_deg)) * math.sin(math.radians(tilt_deg)) * length * 0.5
    tz = z - math.cos(math.radians(heading_deg)) * math.sin(math.radians(tilt_deg)) * length * 0.5
    ty = y + math.cos(math.radians(tilt_deg)) * length
    elements.append(cube([tx - thick * 0.62, ty - thick * 0.2, tz - thick * 0.62],
                         [tx + thick * 0.62, ty + thick * 0.9, tz + thick * 0.62], 0.9,
                         rot=([tx, ty, tz], "y", heading_deg)))


def model(name, elements):
    return {"ambientocclusion": False, "credit": "Riftworld procedural corals",
            "textures": {"particle": NS + name, "t": NS + name},
            "elements": elements, "render_type": "minecraft:cutout"}


# ---------------------------------------------------------------- рецепты
def staghorn():
    rnd = random.Random(11)
    el = [cube([3, 0, 3], [13, 1.6, 13], 0.05)]                     # основание-диск
    for k in range(7):
        a = k * 360 / 7 + rnd.uniform(-12, 12)
        r = rnd.uniform(2.0, 4.6)
        bx, bz = 8 + math.cos(math.radians(a)) * r, 8 + math.sin(math.radians(a)) * r
        ln = rnd.uniform(6.5, 11.5)
        branch(el, bx, 1.2, bz, ln, 1.7, rnd.uniform(6, 26), a)
        # вторичная ветка-рог
        if ln > 8:
            branch(el, bx, 1.2 + ln * 0.55, bz, ln * 0.5, 1.3, rnd.uniform(28, 48), a + rnd.uniform(-50, 50))
    return el


def feather():
    el = [cube([4, 0, 4], [12, 1.4, 12], 0.05)]
    for k in range(6):
        a = k * 30 - 75
        hgt = 8 + (k % 3) * 2.6
        frm = [7.2, 1.0, 7.2]
        to = [8.8, 1.0 + hgt, 8.8]
        el.append(cube(frm, to, 0.4, rot=([8, 1, 8], "z", a)))
    for k in range(4):                                              # перья-пластины
        el.append(cube([2.4, 6 + k * 2.2, 7.4], [13.6, 7.0 + k * 2.2, 8.6], 0.5 + 0.12 * k,
                       rot=([8, 7, 8], "y", 22 * k - 30)))
    return el


def whip():
    el = [cube([5.5, 0, 5.5], [10.5, 2, 10.5], 0.05)]
    x, z = 8.0, 8.0
    for k in range(5):                                              # изогнутый хлыст из сегментов
        y0 = 1.8 + k * 2.9
        el.append(cube([x - 1.1, y0, z - 1.1], [x + 1.1, y0 + 3.1, z + 1.1], 0.2 + k * 0.16,
                       rot=([x, y0, z], "x", -6 + k * 5)))
        x += 0.5
        z += 0.3
    for k in range(4):                                              # кисточки-полипы по хлысту
        yy = 4.0 + k * 3.2
        a = 40 * k
        el.append(cube([x - 0.8, yy, z - 0.8], [x + 0.8, yy + 1.6, z + 0.8], 0.7,
                       rot=([x, yy, z], "y", a)))
    return el


def table():
    el = [cube([7.0, 0, 7.0], [9.0, 6.5, 9.0], 0.15)]               # ножка
    el.append(cube([1.2, 6.2, 1.2], [14.8, 8.0, 14.8], 0.75, rot=([8, 7, 8], "x", 3)))
    el.append(cube([3.0, 8.0, 3.0], [13.0, 9.0, 13.0], 0.9))        # второй ярус
    for k in range(5):
        a = k * 72
        el.append(cube([8 + math.cos(math.radians(a)) * 5.4 - 1.0, 7.4,
                        8 + math.sin(math.radians(a)) * 5.4 - 1.0],
                       [8 + math.cos(math.radians(a)) * 5.4 + 1.0, 8.6,
                        8 + math.sin(math.radians(a)) * 5.4 + 1.0], 0.8))
    return el


def tubes():
    rnd = random.Random(7)
    el = [cube([3.5, 0, 3.5], [12.5, 1.2, 12.5], 0.05)]
    spots = [(5.4, 5.4), (10.2, 5.8), (7.9, 9.6), (5.2, 10.0), (10.6, 10.4)]
    for i, (bx, bz) in enumerate(spots):
        h = rnd.uniform(4.5, 11.0)
        r = 1.5 if i % 2 == 0 else 1.9
        el.append(cube([bx - r, 1.0, bz - r], [bx + r, 1.0 + h, bz + r], min(0.95, h / 12),
                       rot=([bx, 1, bz], "x", rnd.uniform(-8, 8))))
        el.append(cube([bx - r * 0.75, 1.0 + h, bz - r * 0.75], [bx + r * 0.75, 1.0 + h + 0.45, bz + r * 0.75], 0.95))
    return el


def ribbon():
    el = [cube([4.0, 0, 4.0], [12.0, 1.3, 12.0], 0.05)]
    for k in range(7):                                              # спиральные ленты
        a = k * 51
        y0 = 1.1 + k * 1.5
        el.append(cube([3.4, y0, 7.0], [12.6, y0 + 1.5, 9.0], 0.2 + k * 0.1,
                       rot=([8, y0, 8], "y", a)))
    el.append(cube([7.0, 12.2, 7.0], [9.0, 13.6, 9.0], 0.95))       # верхушка
    return el


RECIPES = {
    "staghorn_coral": staghorn,
    "feather_coral": feather,
    "sea_whip": whip,
    "table_coral": table,
    "tube_sponge_cluster": tubes,
    "ribbon_coral": ribbon,
}

RU = {
    "staghorn_coral": "Оленьи рога (коралл)",
    "feather_coral": "Перистый коралл",
    "sea_whip": "Морской хлыст",
    "table_coral": "Столовидный коралл",
    "tube_sponge_cluster": "Гроздь трубчатых губок",
    "ribbon_coral": "Ленточный коралл",
}
EN = {
    "staghorn_coral": "Staghorn Coral",
    "feather_coral": "Feather Coral",
    "sea_whip": "Sea Whip",
    "table_coral": "Table Coral",
    "tube_sponge_cluster": "Tube Sponge Cluster",
    "ribbon_coral": "Ribbon Coral",
}


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)


def merge_json(path, obj):
    old = json.load(open(path, encoding="utf-8")) if os.path.exists(path) else {}
    old.update(obj)
    w(path, old)


def main():
    total_elements = 0
    for name, recipe in RECIPES.items():
        pal = PALETTES[name]
        tmp = "/tmp/rift_corals"
        os.makedirs(tmp, exist_ok=True)
        tex_path = f"{A}/textures/block/{name}.png"
        os.makedirs(os.path.dirname(tex_path), exist_ok=True)
        texture(name, pal).save(tex_path)

        elements = recipe()
        total_elements += len(elements)
        w(f"{A}/models/block/{name}.json", model(name, elements))
        # два поворота по Y: сад не выглядит тиражированным
        w(f"{A}/blockstates/{name}.json", {"variants": {
            "waterlogged=true": [{"model": NS + name, "y": 0}, {"model": NS + name, "y": 90},
                                 {"model": NS + name, "y": 180}, {"model": NS + name, "y": 270}],
            "waterlogged=false": [{"model": NS + name, "y": 0}, {"model": NS + name, "y": 90},
                                  {"model": NS + name, "y": 180}, {"model": NS + name, "y": 270}],
        }})
        w(f"{A}/models/item/{name}.json", {"parent": NS + name})
        w(f"{D}/loot_tables/blocks/{name}.json", {"type": "minecraft:block", "pools": [{
            "rolls": 1, "entries": [{"type": "minecraft:item", "name": "riftworld:" + name}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
        print(f"  {name}: {len(elements)} элементов, текстура 16x16")

    for lang, names in (("en_us", EN), ("ru_ru", RU)):
        merge_json(f"{A}/lang/{lang}.json", {f"block.riftworld.{n}": v for n, v in names.items()})
    print("новых кораллов: %d, элементов модели всего: %d" % (len(RECIPES), total_elements))


if __name__ == "__main__":
    main()
