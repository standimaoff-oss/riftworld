"""Рев 15: текстуры биома «Фиолетовые холмы» (референс reports/refs_rev4/04_violet_hills.png).

Что генерирует:
  * lavender_bed  — КУСТ лаванды (крестик, 16x16 с прозрачностью): три колоса с плотными
    фиолетовыми цветками и зелёными стеблями. Раньше это был сплошной куб лилового цвета —
    из-за него поля читались «пластиковыми плитами».
  * violet_leaves — плотная сиреневая листва: два оттенка, тёмная нижняя часть, светлые блики,
    мелкие просветы только по краям (а не «дырки» по всей кроне).

Запуск: python3 tools/rift/gen_violet_textures.py
"""
import os
import random
from PIL import Image

A = os.path.join(os.path.dirname(__file__), "../../mod/src/main/resources/assets/riftworld/textures/block")

STEM = (86, 128, 74, 255)
STEM_D = (64, 104, 58, 255)
LEAF = (104, 150, 84, 255)
L0 = (120, 82, 176, 255)      # тень колоса
L1 = (146, 106, 204, 255)     # основной
L2 = (172, 136, 226, 255)     # блик
L3 = (198, 172, 240, 255)     # самый светлый


def lavender():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    rnd = random.Random(11)
    # три стебля: (x, высота колоса, высота стебля)
    stalks = [(4, 12, 15), (8, 14, 15), (12, 11, 15)]
    for i, (x, h, bottom) in enumerate(stalks):
        # стебель
        for y in range(15, bottom - h - 1, -1):
            if 0 <= y < 16:
                px[x, y] = STEM if y % 3 else STEM_D
        # листик у стебля
        if 0 <= 14 - i < 16:
            px[x + (1 if i != 2 else -1), 13] = LEAF
            px[x + (1 if i != 2 else -1), 12] = STEM_D
        # колос: плотные цветки, сужающиеся кверху
        top = 15 - h
        for y in range(top, top + 9):
            if not (0 <= y < 16):
                continue
            w = 1 if y <= top + 1 else 0
            for dx in range(-w, w + 1):
                if 0 <= x + dx < 16:
                    shade = L2 if (dx + y) % 3 == 0 else (L1 if (dx + y) % 3 == 1 else L0)
                    px[x + dx, y] = shade
            if y == top:
                px[x, y] = L3
            if rnd.random() < 0.5 and 0 <= x + 1 < 16 and y > top:
                px[x + 1, y] = L1
    # немного отдельных бутонов вокруг
    for _ in range(14):
        x = rnd.randrange(1, 15)
        y = rnd.randrange(9, 16)
        if px[x, y][3] == 0 and px[x, y - 1][3] == 0:
            px[x, y] = rnd.choice([L0, L1, L2])
    im.save(os.path.join(A, "lavender_bed.png"))


def leaves():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    rnd = random.Random(23)
    base = [(124, 96, 178), (140, 112, 196), (158, 130, 212), (110, 84, 162)]
    for y in range(16):
        for x in range(16):
            if 0.5 < 1.2 * ((x - 7.5) ** 2 + (y - 7.5) ** 2) / 64 + rnd.random() * 0.3 and (x in (0, 15) or y in (0, 15)) and rnd.random() < 0.55:
                continue                                   # рваные края только по контуру
            k = rnd.random()
            c = base[0] if k < 0.42 else base[1] if k < 0.70 else base[2] if k < 0.86 else base[3]
            if y > 10:
                c = (int(c[0] * 0.82), int(c[1] * 0.82), int(c[2] * 0.86))     # низ кроны темнее
            px[x, y] = (c[0], c[1], c[2], 255)
    im.save(os.path.join(A, "violet_leaves.png"))


if __name__ == "__main__":
    os.makedirs(A, exist_ok=True)
    lavender()
    leaves()
    print("VIOLET_TEXTURES_OK")
