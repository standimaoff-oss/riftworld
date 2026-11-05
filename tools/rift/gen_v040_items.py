#!/usr/bin/env python3
"""
v0.40.3 — предметы Пегаса, Вендиго и Лиоплевродона: яйца призыва, перо и рог.

Почему появился файл. В клиентском логе прогонов #424–#426 висели строки
``Unable to load model: 'riftworld:pegasus_spawn_egg#inventory'``, ``wendigo_spawn_egg``,
``liopleurodon_spawn_egg``, ``pegasus_feather``, ``wendigo_antler``: у этих предметов не было ни
модели, ни текстуры. В мире они рисовались классической **магентой с шахматкой** — ровно то,
что попало в кадры 148/152 (Пегас умирал при постройке сцены и ронял перо, Вендиго — рог).

Скрипт рисует текстуры 16x16 и простые ``item/generated``-модели.

Запуск: python3 tools/rift/gen_v040_items.py
"""
import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from geo_model_lib import write_png, hx, shade, mixc  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parents[2]
ASSETS = ROOT / "mod/src/main/resources/assets/riftworld"
TEX = ASSETS / "textures/item"
MODELS = ASSETS / "models/item"

S = 16


def blank():
    return [[(0, 0, 0, 0)] * S for _ in range(S)]


def put(img, x, y, c):
    if 0 <= x < S and 0 <= y < S:
        img[y][x] = (c[0], c[1], c[2], 255)


def egg(base, spot, spot2, name):
    """Яйцо призыва: овал 8x11 по явной таблице строк — так же ровно, как ванильные яйца."""
    img = blank()
    rows = {3: (6, 9), 4: (5, 10), 5: (5, 10), 6: (4, 11), 7: (4, 11), 8: (4, 11),
            9: (4, 11), 10: (4, 11), 11: (5, 10), 12: (5, 10), 13: (6, 9)}
    b = hx(base)
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            c = b
            if y <= 6 and x <= x0 + 1:
                c = shade(c, 1.18)               # блик сверху-слева
            elif y >= 11:
                c = shade(c, 0.84)               # тень снизу
            elif x == x1:
                c = shade(c, 0.92)               # затенённый правый край
            put(img, x, y, c)
    for (sx, sy, col) in ((6, 6, spot), (9, 8, spot), (6, 10, spot2), (10, 11, spot2),
                          (9, 5, spot2), (7, 12, spot)):
        for (dx, dy) in ((0, 0), (1, 0), (0, 1)):
            x, y = sx + dx, sy + dy
            if 0 <= y < S and 0 <= x < S and img[y][x][3]:
                put(img, x, y, hx(col))
    write_png(TEX / f"{name}.png", img)
    model(name)


def feather(base, tip, shaft, name):
    """Перо: опахало 4 пикселя в ширину вдоль диагонали, тёмный кончик, светлый стержень."""
    img = blank()
    b, t, s = hx(base), hx(tip), hx(shaft)
    for i in range(12):
        x0 = 3 + i
        y0 = 12 - i
        w = 4 if 2 <= i <= 8 else (3 if i < 2 else 2)
        for k in range(w):
            c = b if i <= 9 else t
            if k == 0:
                c = shade(c, 1.10)               # верхняя кромка опахала
            elif k == w - 1:
                c = shade(c, 0.86)               # нижняя кромка
            put(img, x0, y0 + k, c)
        put(img, x0 + 1, y0 + w // 2, s if i % 2 else shade(s, 1.06))   # стержень
    put(img, 3, 12, s)
    put(img, 15, 0, shade(b, 0.92))
    write_png(TEX / f"{name}.png", img)
    model(name)


def antler(base, dark, name):
    """Рог: ствол с тремя отростками, костяной цвет с тёмными кольцами."""
    img = blank()
    b, d = hx(base), hx(dark)
    stem = [(6, 13), (6, 12), (6, 11), (7, 10), (7, 9), (8, 8), (8, 7), (9, 6), (9, 5), (10, 4)]
    for i, (x, y) in enumerate(stem):
        c = b if i % 3 else d                            # кольца роста
        put(img, x, y, c)
        put(img, x + 1, y, shade(c, 0.88))
    for (x0, y0, dx, dy, n) in ((7, 10, -1, 0, 3), (8, 7, 1, -1, 3), (9, 6, -1, -1, 2)):
        for k in range(1, n + 1):
            put(img, x0 + dx * k, y0 + dy * k, shade(b, 0.94 if k < n else 1.06))
    put(img, 6, 14, d)
    write_png(TEX / f"{name}.png", img)
    model(name)


def model(name):
    MODELS.mkdir(parents=True, exist_ok=True)
    (MODELS / f"{name}.json").write_text(json.dumps({
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"riftworld:item/{name}"},
    }, indent=2) + "\n", encoding="utf-8")


def main():
    TEX.mkdir(parents=True, exist_ok=True)
    egg("#E8E2D6", "#6B6357", "#9C9384", "pegasus_spawn_egg")     # белый конь, серые пятна
    egg("#5C4C3B", "#D9D0BA", "#40342A", "wendigo_spawn_egg")     # бурая шкура, костяные пятна
    egg("#2E4A5A", "#B9D8E6", "#17303C", "liopleurodon_spawn_egg")  # глубинная синева
    feather("#EFE9DE", "#57514A", "#B9B1A2", "pegasus_feather")
    antler("#D8D2C0", "#8B8271", "wendigo_antler")
    print("готово: 5 текстур + 5 моделей в", TEX.relative_to(ROOT), "и", MODELS.relative_to(ROOT))


if __name__ == "__main__":
    main()
