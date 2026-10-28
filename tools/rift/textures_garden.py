#!/usr/bin/env python3
"""Текстуры биома 07 «Висячие сады» (референс reports/refs_rev4/07_waterfall_gardens.png).

Рев 14, итерация 1: на референсе полки — ярко-зелёная трава, обрывы увиты плющом, деревья —
обычные дубы с зелёной кроной и бурой корой. В Элисаре трава бледно-кремовая, а деревья белые
(сизо/амбра), поэтому для садов заводим СВОЙ комплект: зелёную траву, зелёную листву и бурое
дерево. Всё рисуется процедурно 16×16, в том же «плоском» стиле, что и остальные блоки мода.

usage: python3 tools/rift/textures_garden.py [--preview]
"""
import os
import random
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "mod/src/main/resources/assets/riftworld/textures/block")


def rgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def img():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def shade(c, k, d=0):
    return tuple(max(0, min(255, int(v * k) + d)) for v in c)


def garden_grass_top():
    """Ярко-зелёная трава полок: пятна трёх тонов + редкие светлые травинки."""
    im = img()
    r = random.Random(701)
    px = im.load()
    base, dark, light = rgb("6d9b45"), rgb("547c33"), rgb("84b558")
    for y in range(16):
        for x in range(16):
            n = r.random()
            c = light if n > 0.82 else (dark if n < 0.28 else base)
            px[x, y] = (*shade(c, 1.0, r.randint(-7, 7)), 255)
    # травинки: короткие вертикальные штрихи светлее фона
    for _ in range(26):
        x, y = r.randint(0, 15), r.randint(0, 13)
        ln = r.randint(1, 2)
        for k in range(ln):
            if y + k < 16:
                px[x, y + k] = (*shade(light, 1.06, r.randint(-4, 10)), 255)
    return im


def garden_grass_side():
    """Бок травы: земля снизу, зелёная кромка с рваным краем сверху (как у ванильного grass_block)."""
    im = img()
    r = random.Random(702)
    px = im.load()
    dirt, dirt_d, dirt_l = rgb("7a6142"), rgb("5f4a31"), rgb("94794f")
    green, green_d = rgb("6d9b45"), rgb("547c33")
    for y in range(16):
        for x in range(16):
            if y < 3:
                px[x, y] = (*shade(green if r.random() > 0.3 else green_d, 1.0, r.randint(-6, 6)), 255)
            elif y < 5 and r.random() < 0.55:
                px[x, y] = (*shade(green_d, 1.0, r.randint(-6, 6)), 255)
            else:
                n = r.random()
                c = dirt_l if n > 0.78 else (dirt_d if n < 0.3 else dirt)
                px[x, y] = (*shade(c, 1.0, r.randint(-6, 6)), 255)
    return im


def garden_leaves():
    """Листва садового дуба: плотные «кустики» из зелёных тонов и просветы ~9 %."""
    im = img()
    r = random.Random(703)
    px = im.load()
    tones = [rgb("3f7030"), rgb("4d8538"), rgb("5b9a42"), rgb("6fae52")]
    clusters = [(r.randint(1, 14), r.randint(1, 14)) for _ in range(5)]
    for y in range(16):
        for x in range(16):
            near = min(abs(x - cx) + abs(y - cy) for cx, cy in clusters)
            if r.random() < (0.03 if near <= 2 else 0.11):
                continue                                   # просвет — сквозь крону видно небо
            c = tones[min(3, max(0, int((4 - near) * 0.7) + r.randint(0, 1)))]
            px[x, y] = (*shade(c, 1.0, r.randint(-8, 8)), 255)
    return im


def garden_log():
    """Кора дуба: бурые вертикальные волокна с тёмными бороздками и парой сучков."""
    im = img()
    r = random.Random(704)
    px = im.load()
    base, dark, light = rgb("6b4b34"), rgb("4a3222"), rgb("8a6446")
    for x in range(16):
        tone = 1.0 + ((x * 7) % 5 - 2) * 0.045             # вертикальные полосы волокон
        for y in range(16):
            n = r.random()
            c = light if n > 0.85 else (dark if n < 0.22 else base)
            px[x, y] = (*shade(c, tone, r.randint(-6, 6)), 255)
    for _ in range(3):                                      # сучки
        cx, cy = r.randint(1, 14), r.randint(1, 14)
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            x, y = cx + dx, cy + dy
            if 0 <= x < 16 and 0 <= y < 16:
                px[x, y] = (*shade(dark, 0.9), 255)
    return im


def garden_log_top():
    """Спил: годовые кольца на светлой древесине."""
    im = img()
    r = random.Random(705)
    px = im.load()
    wood, ring, bark = rgb("a4835c"), rgb("7b5c3c"), rgb("4a3222")
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            c = bark if d > 6.6 else (ring if int(d) % 2 == 0 else wood)
            px[x, y] = (*shade(c, 1.0, r.randint(-6, 6)), 255)
    return im


def garden_limestone():
    """Светлый известняк обрывов: горизонтальные пласты, как на референсе (полосы через 3 px)."""
    im = img()
    r = random.Random(706)
    px = im.load()
    rows = [rgb("cfc9bc"), rgb("c2bcae"), rgb("d8d3c6"), rgb("b8b2a4")]
    for y in range(16):
        band = rows[(y // 3) % len(rows)]          # горизонтальная слоистость
        for x in range(16):
            # тонкая тёмная линия на стыке пластов
            seam = y % 3 == 2
            c = shade(band, 0.90 if seam else 1.0, r.randint(-7, 7))
            px[x, y] = (*c, 255)
    for _ in range(14):                            # ракушечные вкрапления
        x, y = r.randint(0, 15), r.randint(0, 15)
        px[x, y] = (*shade(rows[r.randint(0, 3)], 1.08), 255)
    return im


def garden_limestone_mossy():
    """Тот же известняк, но с зелёным мхом по швам — низ обрывов на референсе."""
    im = garden_limestone()
    r = random.Random(707)
    px = im.load()
    green, green_l = rgb("5c8b3c"), rgb("79a94f")
    for _ in range(9):
        cx, cy = r.randint(0, 15), r.randint(0, 15)
        rr = r.randint(1, 3)
        for dx in range(-rr, rr + 1):
            for dy in range(-rr, rr + 1):
                x, y = cx + dx, cy + dy
                if 0 <= x < 16 and 0 <= y < 16 and dx * dx + dy * dy <= rr * rr and r.random() > 0.25:
                    px[x, y] = (*shade(green if r.random() > 0.4 else green_l, 1.0, r.randint(-8, 8)), 255)
    return im


TEXTURES = {
    "garden_grass_top": garden_grass_top,
    "garden_grass_side": garden_grass_side,
    "garden_leaves": garden_leaves,
    "garden_log": garden_log,
    "garden_log_top": garden_log_top,
    "garden_limestone": garden_limestone,
    "garden_limestone_mossy": garden_limestone_mossy,
}


def main():
    os.makedirs(OUT, exist_ok=True)
    print("текстуры садов ->", OUT)
    ims = {}
    for name, fn in TEXTURES.items():
        im = fn()
        ims[name] = im
        im.save(os.path.join(OUT, name + ".png"))
        print("  ", name)
    if "--preview" in sys.argv:
        sheet = Image.new("RGBA", (len(ims) * 96, 96), (26, 28, 34, 255))
        for i, (name, im) in enumerate(ims.items()):
            sheet.alpha_composite(im.resize((80, 80), Image.NEAREST), (i * 96 + 8, 8))
        sheet.save("/tmp/garden_tex_preview.png")
        print("preview: /tmp/garden_tex_preview.png")


if __name__ == "__main__":
    main()
