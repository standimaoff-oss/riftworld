#!/usr/bin/env python3
"""Рев 8 «Живое море»: процедурные 16x16 текстуры морского дна, рифов и подводной флоры.

Стиль — как у остальных текстур Элисара: плоские пастельные цвета + мелкий шум,
растения рисуются на прозрачном фоне с мягким тёмным контуром.

usage: python3 tools/rift/textures_sea.py            # пишет в assets/riftworld/textures/block
       python3 tools/rift/textures_sea.py --preview  # + увеличивает в /tmp/sea_tex_preview.png
"""
import math
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


def noise_fill(im, base, spread=7, seed=0):
    """Плоский цвет + мелкий детерминированный шум (как у остальных блоков мода)."""
    r = random.Random(seed)
    b = rgb(base)
    px = im.load()
    for y in range(16):
        for x in range(16):
            d = r.randint(-spread, spread)
            px[x, y] = (max(0, min(255, b[0] + d)), max(0, min(255, b[1] + d)), max(0, min(255, b[2] + d)), 255)


def speckle(im, color, n, seed, minc=0, maxc=16):
    r = random.Random(seed)
    c = rgb(color)
    px = im.load()
    for _ in range(n):
        x, y = r.randint(minc, maxc - 1), r.randint(minc, maxc - 1)
        px[x, y] = (*c, 255)


def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"))
    print("  ", name)


# ---------------------------------------------------------------- terrain cubes

def elisar_silt():
    im = img()
    noise_fill(im, "3e4a44", 6, 11)
    speckle(im, "4a5750", 34, 12)
    speckle(im, "35403b", 30, 13)
    speckle(im, "6c7266", 8, 14)       # ракушечная крошка
    speckle(im, "8a8f84", 4, 15)
    # мягкие горизонтальные полосы ила
    px = im.load()
    for y in (3, 4, 10, 11):
        for x in range(16):
            if random.Random(x * 31 + y).random() < 0.5:
                px[x, y] = (70, 80, 74, 255)
    return im


def reef_stone():
    im = img()
    noise_fill(im, "6e6480", 6, 21)
    speckle(im, "5d5470", 40, 22)
    speckle(im, "7e7392", 30, 23)
    speckle(im, "b08fa8", 7, 24)       # розовые вкрапления
    speckle(im, "4d4560", 6, 25)
    return im


def coral_brain():
    im = img()
    noise_fill(im, "d98295", 5, 31)
    px = im.load()
    # извилины мозгового коралла: две синусоиды + борозды
    dark, light = (184, 101, 122, 255), (239, 174, 189, 255)
    for y in range(16):
        for x in range(16):
            v = math.sin(x * 0.9 + math.sin(y * 0.7) * 2.1) + math.sin(y * 1.1 + math.cos(x * 0.6) * 1.7)
            if v > 1.25:
                px[x, y] = dark
            elif v < -1.35:
                px[x, y] = light
    speckle(im, "b8657a", 5, 32)
    return im


# ---------------------------------------------------------------- plants (cross)

def _stem(px, x, ys, color, shadow=None):
    for y in ys:
        px[x, y] = (*color, 255)
        if shadow and x + 1 < 16:
            px[x + 1, y] = (*shadow, 255)


def coral_branch():
    """Ветвистый коралл: рогатка с 3-4 отростками, розово-персиковый."""
    im = img()
    px = im.load()
    c, l, d = rgb("e98a73"), rgb("f6b39b"), rgb("c96b58")
    cols = {7: 0, 8: 0}
    # ножка
    for y in range(15, 9, -1):
        px[7, y] = (*d, 255)
        px[8, y] = (*c, 255)
    # ветви
    branches = [(8, 10, 1, 5), (7, 11, -1, 4), (8, 7, 1, 3)]
    for x0, y0, dx, steps in branches:
        x, y = x0, y0
        for i in range(steps):
            y -= 1
            if i % 2 == 0:
                x += dx
            if 0 <= x < 16 and 0 <= y < 16:
                px[x, y] = (*c, 255)
                if x + dx < 16 and 0 <= x + dx < 16 and y - 1 >= 0:
                    px[x + dx, y - 1] = (*l, 255)
                if 0 <= x < 16 and y - 1 >= 0:
                    px[x, y - 1] = (*c, 255)
    # кончики
    for (x, y) in ((11, 4), (5, 6), (12, 3), (4, 5)):
        if 0 <= x < 16 and 0 <= y < 16:
            px[x, y] = (*l, 255)
    return im


def coral_fan():
    """Веерный коралл: широкий веер из тонких спиц, фиолетовый (растёт вверх от основания)."""
    im = img()
    px = im.load()
    deep, c, l = rgb("6f52a8"), rgb("a386d8"), rgb("cbb6f5")
    for y in range(12, 16):                    # стволик
        px[8, y] = (*deep, 255)
        px[7, y] = (*c, 255)
    cx, cy = 7.5, 15.0
    for a in range(15, 166, 9):                # веер вверх ~150 градусов
        rad = math.radians(a)
        for r in range(1, 10):
            x = int(round(cx + math.cos(rad) * r))
            y = int(round(cy - math.sin(rad) * r * 0.72))
            if 0 <= x < 16 and 0 <= y < 16:
                px[x, y] = (*(l if r >= 8 else (c if (a // 9) % 2 else deep)), 255)
    return im


def coral_tube():
    """Трубчатые губки: три вертикальные трубки с тёмным зевом."""
    im = img()
    px = im.load()
    c, d, l = rgb("2fa39a"), rgb("1e7a73"), rgb("6fd3c8")
    tubes = [(4, 15, 7), (8, 15, 5), (12, 15, 9)]      # x, низ, верх
    for (x, y0, y1) in tubes:
        for y in range(y0, y1 - 1, -1):
            px[x, y] = (*c, 255)
            if x + 1 < 16:
                px[x + 1, y] = (*d, 255)
        px[x, y1] = (*l, 255)
        if x + 1 < 16:
            px[x + 1, y1] = (*l, 255)
    return im


def coral_glow():
    """Светящийся коралл: бирюзовые «свечи» с белым ядром (блок светит)."""
    im = img()
    px = im.load()
    c, l, w = rgb("3fc9bc"), rgb("7ff2e4"), (232, 255, 251)
    for x, top in ((5, 6), (8, 3), (11, 7)):
        for y in range(15, top - 1, -1):
            px[x, y] = (*c, 255)
            px[x - 1 if x > 0 else x, y] = (*rgb("2a9c92"), 255)
        px[x, top] = (*w, 255)
        px[x, top + 1] = (*l, 255)
    return im


def sea_kelp():
    im = img()
    px = im.load()
    c, d, l = rgb("6e9a4e"), rgb("4f7437"), rgb("9ac472")
    # S-образная лента ламинарии
    for y in range(16):
        x = 7 + int(round(math.sin(y * 0.55) * 2.0))
        x = max(1, min(14, x))
        px[x, y] = (*c, 255)
        px[x + 1, y] = (*d, 255)
        if y % 3 == 0:
            px[x - 1, y] = (*l, 255)
    return im


def sea_kelp_top():
    im = img()
    px = im.load()
    c, l = rgb("6e9a4e"), rgb("9ac472")
    for y in range(6, 16):
        x = 7 + int(round(math.sin(y * 0.55) * 2.0))
        x = max(1, min(14, x))
        px[x, y] = (*c, 255)
    # широкий лист на верхушке
    for x in range(5, 12):
        for y in range(3, 7):
            if abs(x - 8) + abs(y - 5) < 4:
                px[x, y] = (*(l if (x + y) % 3 else c), 255)
    return im


def sea_grass():
    im = img()
    px = im.load()
    c, l = rgb("8fbf6a"), rgb("b6d98f")
    for x, top in ((3, 9), (6, 6), (9, 8), (12, 10)):
        for y in range(15, top - 1, -1):
            px[x, y] = (*(l if y < top + 3 else c), 255)
            if y == top:
                px[x, y - 1] = (*l, 255)
    return im


def cave_glow():
    """Кристалл-нарост пещер: бледно-бирюзовые друзы (свет 10)."""
    im = img()
    px = im.load()
    c, l, w = rgb("4e8a82"), rgb("9fe8dc"), rgb("e8fff9")
    for x, top in ((5, 5), (7, 2), (10, 6), (12, 8)):
        for y in range(15, top - 1, -1):
            px[x, y] = (*c, 255)
        px[x, top] = (*w, 255)
        px[x, top + 1] = (*l, 255)
        px[x, top + 2] = (*l, 255)
    return im


def sea_shells():
    """Ракушки/морские звёзды — плоский коврик на дне."""
    im = img()
    px = im.load()
    shell, sh = rgb("efe6d2"), rgb("d8c9ae")
    star, star_d = rgb("cf8fa6"), rgb("a86a83")
    # три ракушки
    for (cx, cy) in ((4, 5), (11, 9), (7, 12)):
        for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1), (1, 1)):
            x, y = cx + dx, cy + dy
            if 0 <= x < 16 and 0 <= y < 16:
                px[x, y] = (*(shell if (dx + dy) % 2 else sh), 255)
    # морская звезда
    sx, sy = 12, 4
    for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1), (2, -1), (-2, 1)):
        x, y = sx + dx, sy + dy
        if 0 <= x < 16 and 0 <= y < 16:
            px[x, y] = (*(star if (dx, dy) != (0, 0) else star_d), 255)
    return im


# ---------------------------------------------------------------- Рев 10: тропический риф

def anemone():
    """Актиния: венчик щупалец на короткой ножке, розово-коралловая, кончики светятся."""
    im = img()
    px = im.load()
    foot, c, l, tip = rgb("b8567a"), rgb("e585a8"), rgb("f7bcd0"), (255, 242, 246)
    for y in range(15, 10, -1):
        px[7, y] = (*foot, 255)
        px[8, y] = (*foot, 255)
    # щупальца: редкий веер из центра (7.5, 10.5) — между лучами видно фон, силуэт «цветка»
    cx, cy = 7.5, 10.5
    for a in range(15, 166, 15):
        rad = math.radians(a)
        for r in range(2, 6):
            x = int(round(cx + math.cos(rad) * r * 1.15))
            y = int(round(cy - math.sin(rad) * r * 0.95))
            if 0 <= x < 16 and 0 <= y < 16:
                px[x, y] = (*(tip if r >= 5 else (l if r >= 3 else c)), 255)
    for x, y in ((7, 10), (8, 10), (7, 11), (8, 11)):
        px[x, y] = (*c, 255)
    return im


def coral_bush():
    """Куст коралла: плотный пучок коротких веточек бирюзово-зелёного тона."""
    im = img()
    px = im.load()
    d, c, l = rgb("1f7f74"), rgb("35b3a2"), rgb("7fe3d4")
    for x, top in ((4, 9), (6, 6), (8, 4), (10, 7), (12, 10)):
        for y in range(15, top - 1, -1):
            px[x, y] = (*(c if y > top + 2 else l), 255)
            if x + 1 < 16 and y % 2 == 0:
                px[x + 1, y] = (*d, 255)
    for x, y in ((5, 8), (9, 5), (11, 9), (3, 10), (13, 11)):
        if 0 <= x < 16 and 0 <= y < 16:
            px[x, y] = (*l, 255)
    return im


def tube_worms():
    """Трубчатые черви: пучок белых трубок с алыми венчиками."""
    im = img()
    px = im.load()
    white, shade, red, hot = rgb("e8e2d4"), rgb("b9b2a2"), rgb("c9425c"), rgb("f2798f")
    for x, top in ((4, 7), (7, 4), (10, 6), (12, 9)):
        for y in range(15, top - 1, -1):
            px[x, y] = (*white, 255)
            if x + 1 < 16:
                px[x + 1, y] = (*shade, 255)
        for dx in (-1, 0, 1):
            if 0 <= x + dx < 16 and top - 1 >= 0:
                px[x + dx, top - 1] = (*(hot if dx == 0 else red), 255)
    return im


def sea_star():
    """Морская звезда — плоский декаль на дне (оранжевая, пятилучевая)."""
    im = img()
    px = im.load()
    c, d, l = rgb("e0813f"), rgb("b45f2b"), rgb("f4b273")
    cx, cy = 8, 8
    for a in range(0, 360, 72):
        rad = math.radians(a - 90)
        for r in range(0, 6):
            x = int(round(cx + math.cos(rad) * r))
            y = int(round(cy + math.sin(rad) * r))
            if 0 <= x < 16 and 0 <= y < 16:
                px[x, y] = (*(l if r >= 4 else (c if r >= 2 else d)), 255)
                if r < 3:
                    for dx, dy in ((1, 0), (0, 1)):
                        if 0 <= x + dx < 16 and 0 <= y + dy < 16:
                            px[x + dx, y + dy] = (*c, 255)
    return im


def sea_urchin():
    """Морской ёж — плоский декаль: тёмный купол с фиолетовыми иглами."""
    im = img()
    px = im.load()
    body, spike, tip = rgb("3a2b46"), rgb("6d4f86"), rgb("b79ad4")
    cx, cy = 8, 8
    for y in range(16):
        for x in range(16):
            dd = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if dd < 2.6:
                px[x, y] = (*body, 255)
            elif dd < 3.3:
                px[x, y] = (*spike, 255)
    for a in range(0, 360, 18):
        rad = math.radians(a)
        for r in range(3, 7):
            x = int(round(cx + math.cos(rad) * r))
            y = int(round(cy + math.sin(rad) * r))
            if 0 <= x < 16 and 0 <= y < 16:
                px[x, y] = (*(tip if r >= 6 else spike), 255)
    return im


def shell_bed():
    """Ракушечная грядка — плоский декаль: плотная россыпь ракушек и камешков."""
    im = img()
    px = im.load()
    a, b, c2 = rgb("f0e7d3"), rgb("d5c7ab"), rgb("c2b6a0")
    rnd = random.Random(41)
    for _ in range(26):
        x, y = rnd.randint(1, 13), rnd.randint(1, 13)
        w = rnd.choice((1, 2, 2, 3))
        col = rnd.choice((a, b, c2))
        for dx in range(w):
            for dy in range(max(1, w - 1)):
                if 0 <= x + dx < 16 and 0 <= y + dy < 16:
                    px[x + dx, y + dy] = (*col, 255)
    return im


def _coral_cap(base, light, dark, seed):
    """Рифовая шапка: бугристый цельный блок тропического коралла (ядро рифового холма)."""
    im = img()
    rnd = random.Random(seed)
    b, l, d = rgb(base), rgb(light), rgb(dark)
    px = im.load()
    for y in range(16):
        for x in range(16):
            n = rnd.random()
            col = l if n > 0.78 else (d if n < 0.22 else b)
            px[x, y] = (*col, 255)
    # мелкие «поры» — кружки другого тона
    for _ in range(9):
        cx, cy = rnd.randint(2, 13), rnd.randint(2, 13)
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1), (-1, 0), (0, -1)):
            x, y = cx + dx, cy + dy
            if 0 <= x < 16 and 0 <= y < 16:
                px[x, y] = (*(d if rnd.random() > 0.4 else l), 255)
    return im


def coral_cap_pink():
    return _coral_cap("d9678a", "f0a2ba", "a83f62", 11)


def coral_cap_teal():
    return _coral_cap("2f9f95", "6fd6c9", "1c6f68", 12)


def coral_cap_violet():
    return _coral_cap("7a5cc0", "ab8fe6", "513a8c", 13)


TEXTURES = {
    "elisar_silt": elisar_silt,
    "reef_stone": reef_stone,
    "coral_brain": coral_brain,
    "coral_branch": coral_branch,
    "coral_fan": coral_fan,
    "coral_tube": coral_tube,
    "coral_glow": coral_glow,
    "sea_kelp": sea_kelp,
    "sea_kelp_top": sea_kelp_top,
    "sea_grass": sea_grass,
    "cave_glow": cave_glow,
    "sea_shells": sea_shells,
    # Рев 10: тропический риф
    "anemone": anemone,
    "coral_bush": coral_bush,
    "tube_worms": tube_worms,
    "sea_star": sea_star,
    "sea_urchin": sea_urchin,
    "shell_bed": shell_bed,
    "coral_cap_pink": coral_cap_pink,
    "coral_cap_teal": coral_cap_teal,
    "coral_cap_violet": coral_cap_violet,
}


def main():
    print("текстуры моря ->", OUT)
    ims = {}
    for name, fn in TEXTURES.items():
        im = fn()
        ims[name] = im
        save(im, name)
    if "--preview" in sys.argv:
        cols, cell = 6, 96
        rows = (len(ims) + cols - 1) // cols
        sheet = Image.new("RGBA", (cols * cell, rows * cell), (26, 28, 34, 255))
        for i, (name, im) in enumerate(ims.items()):
            big = im.resize((80, 80), Image.NEAREST)
            sheet.alpha_composite(big, ((i % cols) * cell + 8, (i // cols) * cell + 8))
        out = "/tmp/sea_tex_preview.png"
        sheet.save(out)
        print("preview:", out)


if __name__ == "__main__":
    main()


def _egg(name, back, spots):
    """Иконка яйца призыва: овальная скорлупа + пятна вторым цветом (16x16, как ванильные яйца)."""
    im = img()
    px = im.load()
    b = rgb(back)
    s = rgb(spots)
    # силуэт яйца: уже сверху, шире в середине, узкий низ
    for y in range(2, 15):
        f = (y - 2) / 12.0
        half = 2.6 + 2.6 * math.sin(math.pi * min(1.0, f * 1.15)) - 1.1 * max(0.0, f - 0.55) * 2
        half = max(1.6, min(5.4, half))
        for x in range(16):
            if abs(x - 7.5) <= half:
                shade = -10 if x < 6 else (8 if x > 9 else 0)
                px[x, y] = (max(0, b[0] + shade), max(0, b[1] + shade), max(0, b[2] + shade), 255)
    for (x, y) in ((5, 6), (6, 6), (5, 7), (9, 9), (10, 9), (9, 10), (7, 12), (8, 12), (10, 5), (4, 9), (8, 4)):
        if 0 <= x < 16 and 0 <= y < 16 and px[x, y][3] > 0:
            px[x, y] = (*s, 255)
    return im


EGGS = {
    "silver_fish_spawn_egg": ("C6D3DC", "2B3440"),
    "gold_fish_spawn_egg": ("E9C25A", "C29A38"),
    "jelly_fish_spawn_egg": ("9FE6FF", "D8F7FF"),
    "ray_fish_spawn_egg": ("68798C", "DCE6EC"),
    "angler_fish_spawn_egg": ("2C3947", "DFFFA8"),
    # Рев 10
    "surgeon_fish_spawn_egg": ("2F6FD0", "F2D24C"),
    "turtle_spawn_egg": ("4E7A4A", "E3DCC0"),
    "gloom_fish_spawn_egg": ("2A1F36", "C46BFF"),
    "sting_fish_spawn_egg": ("B05AD6", "FFE9FF"),
}
for _n, (_b, _s) in EGGS.items():
    _d = os.path.join(OUT, "..", "item")
    os.makedirs(_d, exist_ok=True)
    _egg(_n, _b, _s).save(os.path.join(_d, _n + ".png"))
    print("   item/" + _n)
