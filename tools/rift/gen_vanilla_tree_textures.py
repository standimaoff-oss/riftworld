#!/usr/bin/env python3
"""Генератор ванильных (Jappa-стиль) текстур листвы с настоящими сквозными просветами и коры
для деревьев Фиолетовых холмов (violet_leaves / lilac_canopy / lilac_bark / lilac_rings)
и Висячих садов (garden_leaves / garden_log / garden_log_top).

Без внешних зависимостей (чистый Python stdlib: struct + zlib).
Бесшовная тороидальная развёртка 16x16 + ~28-30% крупных 2-4px просветов (alpha=0),
чтобы сквозь кроны деревьев реально просвечивали небо и ветви, как в ванильном Minecraft.
"""
import math
import os
import random
import struct
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "../.."))
TEX_BLOCK = os.path.join(ROOT, "mod/src/main/resources/assets/riftworld/textures/block")


def save_png(path, px):
    h = len(px)
    w = len(px[0])
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        for x in range(w):
            r, g, b, a = px[y][x]
            raw.extend((
                max(0, min(255, int(r))),
                max(0, min(255, int(g))),
                max(0, min(255, int(b))),
                max(0, min(255, int(a))),
            ))

    def chunk(tag, body):
        return struct.pack(">I4s", len(body), tag) + body + struct.pack(">I", zlib.crc32(tag + body) & 0xFFFFFFFF)

    ihdr = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)


def make_vanilla_cutout_leaves(palette, seed, target_hole_Windows):
    """Рисует бесшовную 16x16 текстуру листвы в духе ванильных oak/azalea/cherry leaves:
    перекрывающиеся диагональные листовые пластинки 3x2 / 4x2 с тёмной нижней кромкой,
    средним тоном, солнечным бликом сверху и чёткими фигурными просветами 2x2..3x2 (~28-31% alpha=0).
    """
    rng = random.Random(seed)
    c_shadow, c_dark, c_mid, c_light, c_spark = palette
    px = [[None for _ in range(16)] for _ in range(16)]

    # Шаг 1: базовый ковёр из перекрывающихся диагональных листочков (бесшовно по mod 16)
    blades = [
        # (cx, cy, orient) — равномерная сетка с живым сдвигом, дающая настоящие листья, а не шум
        (1, 1, 1), (6, 0, -1), (11, 2, 1), (15, 1, -1),
        (3, 5, -1), (8, 4, 1), (13, 6, -1),
        (0, 9, 1), (5, 8, -1), (10, 9, 1), (14, 10, -1),
        (2, 13, -1), (7, 13, 1), (12, 14, -1),
    ]
    rng.shuffle(blades)
    for bx, by, ori in blades:
        # Каждый листок — диагональный ромбик ~4x3 пикселя со светотенью сверху-слева вниз-вправо
        shape = [
            (0, -1, c_light), (ori, -1, c_spark if rng.random() < 0.42 else c_light),
            (-ori, 0, c_mid), (0, 0, c_light), (ori, 0, c_mid), (2 * ori, 0, c_dark),
            (-ori, 1, c_dark), (0, 1, c_mid), (ori, 1, c_shadow),
        ]
        for dx, dy, col in shape:
            x = (bx + dx) % 16
            y = (by + dy) % 16
            j = rng.randint(-5, 5)
            px[y][x] = (col[0] + j, col[1] + j, col[2] + j, 255)

    # Заполняем оставшиеся одиночные клетки тёмно-средними листьями перед вырезанием просветов
    for y in range(16):
        for x in range(16):
            if px[y][x] is None:
                col = c_mid if (x + y) % 2 == 0 else c_dark
                j = rng.randint(-6, 6)
                px[y][x] = (col[0] + j, col[1] + j, col[2] + j, 255)

    # Шаг 2: вырезаем настоящие ванильные фигурные «окошки» просветов (2..5 пикселей каждое),
    # равномерно распределённые по 16x16 и окаймлённые тенью сверху/слева, чтобы листва имела толщину!
    for hx, hy, cells in target_hole_Windows:
        for dx, dy in cells:
            x = (hx + dx) % 16
            y = (hy + dy) % 16
            px[y][x] = (0, 0, 0, 0)

    # Шаг 3: ванильная обводка-глубина вокруг просветов:
    # пиксель над просветом получает тёмный нижний край листа, пиксель под просветом — верхний блик нижнего листа!
    out = [row[:] for row in px]
    for y in range(16):
        for x in range(16):
            if px[y][x][3] == 0:
                continue
            below_hole = px[(y + 1) % 16][x][3] == 0
            above_hole = px[(y - 1) % 16][x][3] == 0
            right_hole = px[y][(x + 1) % 16][3] == 0
            if below_hole or right_hole:
                j = rng.randint(-4, 4)
                col = c_shadow if (below_hole and right_hole) else c_dark
                out[y][x] = (col[0] + j, col[1] + j, col[2] + j, 255)
            elif above_hole and rng.random() < 0.72:
                j = rng.randint(-4, 4)
                col = c_spark if rng.random() < 0.30 else c_light
                out[y][x] = (col[0] + j, col[1] + j, col[2] + j, 255)
    return out


def make_lilac_bark():
    """Ванильная рельефная кора фиолетового дерева (тёмная сливово-пепельная с продольными пластинами)."""
    rng = random.Random(4401)
    c_crevice = (52, 42, 66)
    c_dark = (72, 59, 90)
    c_mid = (92, 77, 112)
    c_light = (114, 97, 136)
    px = [[(0, 0, 0, 255) for _ in range(16)] for _ in range(16)]
    # Продольные пластины коры с перевязкой по высоте (бесшовно 16x16)
    fissures = {0, 4, 8, 11, 15}
    for y in range(16):
        for x in range(16):
            shift = 1 if (y // 5 + x // 4) % 2 == 1 and x in (4, 11) else 0
            xe = (x + shift) % 16
            if xe in fissures:
                col = c_crevice if (y + x) % 3 != 0 else c_dark
            elif (xe - 1) % 16 in fissures:
                col = c_light if y % 4 != 0 else c_mid
            elif (xe + 1) % 16 in fissures:
                col = c_dark
            else:
                col = c_mid if rng.random() < 0.72 else c_light
            j = rng.randint(-5, 5)
            px[y][x] = (col[0] + j, col[1] + j, col[2] + j, 255)
    return px


def make_lilac_rings():
    rng = random.Random(4402)
    bark = (62, 50, 78)
    wood_l = (182, 162, 198)
    wood_d = (144, 122, 166)
    px = [[(0, 0, 0, 255) for _ in range(16)] for _ in range(16)]
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d > 6.5:
                col = bark
            else:
                col = wood_d if int(round(d)) % 2 == 0 else wood_l
            j = rng.randint(-5, 5)
            px[y][x] = (col[0] + j, col[1] + j, col[2] + j, 255)
    return px


def make_garden_bark():
    """Ванильная кора дуба Висячих садов в стиле Jappa oak_log."""
    rng = random.Random(7701)
    c_crevice = (62, 42, 26)
    c_dark = (84, 58, 36)
    c_mid = (108, 77, 49)
    c_light = (130, 96, 63)
    px = [[(0, 0, 0, 255) for _ in range(16)] for _ in range(16)]
    fissures = {1, 5, 9, 13}
    for y in range(16):
        for x in range(16):
            wobble = 1 if (y >= 8 and x in (5, 13)) else 0
            xe = (x + wobble) % 16
            if xe in fissures:
                col = c_crevice if y % 5 != 2 else c_dark
            elif (xe - 1) % 16 in fissures:
                col = c_light if y % 3 != 0 else c_mid
            elif (xe + 1) % 16 in fissures:
                col = c_dark
            else:
                col = c_mid
            j = rng.randint(-5, 5)
            px[y][x] = (col[0] + j, col[1] + j, col[2] + j, 255)
    return px


def main():
    # Фигурные просветы для сиренево-фиолетовой листвы (~82 прозрачных пикселя из 256 = ~32%, точно как ванильный oak_leaves!)
    violet_holes = [
        (2, 2, [(0, 0), (1, 0), (0, 1), (1, 1), (2, 1)]),
        (9, 1, [(0, 0), (1, 0), (0, 1), (1, 1), (2, 1)]),
        (14, 3, [(0, 0), (1, 0), (0, 1), (1, 1), (1, 2)]),
        (5, 5, [(0, 0), (1, 0), (2, 0), (1, 1), (2, 1), (0, 1)]),
        (12, 7, [(0, 0), (1, 0), (0, 1), (1, 1), (1, 2)]),
        (1, 8, [(0, 0), (1, 0), (0, 1), (1, 1), (2, 1)]),
        (7, 10, [(0, 0), (1, 0), (0, 1), (1, 1), (2, 0), (2, 1)]),
        (3, 13, [(0, 0), (1, 0), (1, 1), (2, 1), (0, 1)]),
        (10, 14, [(0, 0), (1, 0), (0, 1), (1, 1), (2, 0)]),
        (15, 12, [(0, 0), (0, 1), (1, 1), (1, 2), (1, 0)]),
        (6, 0, [(0, 0), (0, 1), (1, 0)]),
        (0, 5, [(0, 0), (1, 0), (0, 1)]),
        (13, 10, [(0, 0), (1, 0), (0, 1)]),
        (8, 6, [(0, 0), (0, 1), (1, 1)]),
        (4, 9, [(0, 0), (1, 0), (1, 1)]),
        (11, 4, [(0, 0), (1, 0), (0, 1)]),
        (2, 11, [(0, 0), (0, 1)]),
        (7, 14, [(0, 0), (1, 0)]),
        (14, 0, [(0, 0), (0, 1)]),
        (5, 2, [(0, 0), (0, 1)]),
        (11, 11, [(0, 0), (1, 0)]),
    ]
    violet_palette = (
        (82, 54, 132),    # глубокая тень под листом
        (108, 76, 164),   # тёмный фиолетовый лист
        (138, 102, 194),  # основной сиреневый тон
        (168, 134, 220),  # солнечный лавандовый лист
        (206, 180, 244),  # нежные цветущие соцветия на верхушках
    )
    lilac_px = make_vanilla_cutout_leaves(violet_palette, 5201, violet_holes)
    violet_px = make_vanilla_cutout_leaves(violet_palette, 5202, violet_holes)

    # Фигурные просветы для дубовой листвы Висячих садов (~81 прозрачный пиксель из 256 = ~31.6%, точно как ванильный oak_leaves!)
    garden_holes = [
        (1, 1, [(0, 0), (1, 0), (0, 1), (1, 1), (2, 1)]),
        (7, 2, [(0, 0), (1, 0), (2, 0), (1, 1), (2, 1), (0, 1)]),
        (13, 2, [(0, 0), (1, 0), (0, 1), (1, 1), (1, 2)]),
        (4, 6, [(0, 0), (1, 0), (0, 1), (1, 1), (1, 2)]),
        (10, 6, [(0, 0), (1, 0), (0, 1), (1, 1), (2, 1)]),
        (15, 7, [(0, 0), (1, 0), (0, 1), (1, 1), (1, 2)]),
        (2, 10, [(0, 0), (1, 0), (2, 0), (0, 1), (1, 1)]),
        (8, 11, [(0, 0), (1, 0), (0, 1), (1, 1), (2, 0)]),
        (12, 12, [(0, 0), (1, 0), (1, 1), (2, 1), (0, 1)]),
        (5, 14, [(0, 0), (1, 0), (0, 1), (1, 1), (2, 0)]),
        (0, 5, [(0, 0), (0, 1), (1, 0)]),
        (6, 8, [(0, 0), (1, 0), (0, 1)]),
        (11, 0, [(0, 0), (1, 0), (0, 1)]),
        (3, 3, [(0, 0), (0, 1), (1, 1)]),
        (9, 4, [(0, 0), (1, 0), (1, 1)]),
        (14, 10, [(0, 0), (0, 1), (1, 0)]),
        (1, 14, [(0, 0), (1, 0)]),
        (10, 15, [(0, 0), (1, 0)]),
        (6, 12, [(0, 0), (0, 1)]),
        (4, 0, [(0, 0), (1, 0)]),
        (12, 4, [(0, 0), (0, 1)]),
    ]
    garden_palette = (
        (38, 74, 28),     # глубокая лесная тень (ванильный дуб/азалия)
        (56, 102, 40),    # нижний лист
        (76, 130, 52),    # сочный средний лист
        (100, 158, 66),   # освещённый солнцем лист
        (128, 184, 82),   # тёплый солнечный блик молодых побегов
    )
    garden_px = make_vanilla_cutout_leaves(garden_palette, 7301, garden_holes)

    save_png(os.path.join(TEX_BLOCK, "lilac_canopy.png"), lilac_px)
    save_png(os.path.join(TEX_BLOCK, "violet_leaves.png"), violet_px)
    save_png(os.path.join(TEX_BLOCK, "lilac_bark.png"), make_lilac_bark())
    save_png(os.path.join(TEX_BLOCK, "lilac_rings.png"), make_lilac_rings())
    save_png(os.path.join(TEX_BLOCK, "garden_leaves.png"), garden_px)
    save_png(os.path.join(TEX_BLOCK, "garden_log.png"), make_garden_bark())

    for nm, grid in [("lilac_canopy", lilac_px), ("violet_leaves", violet_px), ("garden_leaves", garden_px)]:
        tr = sum(1 for row in grid for p in row if p[3] == 0)
        print(f"{nm:16s}: transparent={tr}/256 ({tr * 100.0 / 256:.1f}%)")


if __name__ == "__main__":
    main()
