#!/usr/bin/env python3
"""v0.40.0 "Снаряжение и люди Элисара" — генератор текстур по палитрам референсов.

Все палитры выведены k-средними из приложенных референсов (reports/v040_refs):

* 01_aurite_medium_armor.png   -> тёмная сталь + ремни + золотистая (ауритовая) окантовка
* 02_riftcrystal_strong_armor.png -> почти чёрный кристалл + бирюзовое свечение + маджента
* 03_riftworld_weapon_tools_set.png -> тёмное дерево рукоятей + бронзово-золотые/бирюзовые головы
* 05_elisar_villager.png       -> сиренево-серое одеяние, тёплая кожа, каштановые волосы
* 06_elisar_guard.png          -> медная кираса, серые латы, светлый плюмаж
* 07_elisar_archer.png         -> коричневая кожа куртки, тёплая кожа, серые штаны

Запуск: python tools/rift/gen_v040_gear.py
"""
from __future__ import annotations

import pathlib

import numpy as np
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[2]
ITEMS = ROOT / "mod/src/main/resources/assets/riftworld/textures/item"
BLOCKS = ROOT / "mod/src/main/resources/assets/riftworld/textures/block"
ARMOR = ROOT / "mod/src/main/resources/assets/riftworld/textures/models/armor"
ENTITY = ROOT / "mod/src/main/resources/assets/riftworld/textures/entity"
REPORT = ROOT / "reports/v040_refs"

RNG = np.random.default_rng(20241107)

# --------------------------------------------------------------------------- palettes
AURITE = dict(  # 01_aurite_medium_armor.png
    plate=(91, 82, 71), plate_dark=(51, 45, 36), plate_lite=(117, 107, 94),
    leather=(141, 94, 56), trim=(194, 144, 72), trim_lite=(215, 186, 110),
    line=(21, 16, 11),
)
RIFT = dict(  # 02_riftcrystal_strong_armor.png
    plate=(53, 58, 67), plate_dark=(10, 18, 20), plate_lite=(209, 217, 224),
    leather=(16, 134, 134), trim=(82, 192, 189), trim_lite=(213, 113, 231),
    line=(10, 18, 20),
)
WOOD = dict(dark=(43, 27, 9), mid=(72, 48, 17), lite=(137, 94, 32))
WOOD_COLD = dict(dark=(17, 22, 26), mid=(53, 58, 67), lite=(120, 128, 138))

VILLAGER = dict(  # 05_elisar_villager.png
    robe=(112, 93, 104), robe_dark=(95, 70, 68), robe_lite=(143, 123, 132),
    cloth=(172, 138, 157), skin=(205, 157, 143), hair=(181, 123, 80),
    boot=(59, 46, 39), line=(40, 31, 27),
)
GUARD = dict(  # 06_elisar_guard.png
    robe=(194, 126, 63), robe_dark=(127, 82, 47), robe_lite=(177, 137, 141),
    cloth=(94, 97, 101), skin=(177, 137, 141), hair=(57, 45, 37),
    boot=(125, 129, 133), line=(41, 33, 28), plume=(237, 236, 243),
)
ARCHER = dict(  # 07_elisar_archer.png
    robe=(106, 73, 58), robe_dark=(69, 58, 50), robe_lite=(144, 105, 93),
    cloth=(97, 103, 109), skin=(208, 152, 115), hair=(48, 32, 22),
    boot=(179, 157, 169), line=(38, 27, 20),
)


def rgb(c):
    return np.array(c, dtype=np.float32)


# --------------------------------------------------------------------------- helpers
def new(w, h):
    return np.zeros((h, w, 4), np.uint8)


def shade(base, amount, noise=6.0, rng=RNG):
    """Слегка варьируем яркость, чтобы поверхность не была пластмассовой."""
    c = rgb(base) * amount
    c += rng.normal(0, noise, size=3).astype(np.float32)
    return tuple(int(max(0, min(255, v))) for v in c)


def fill(img, x, y, w, h, colour):
    if w <= 0 or h <= 0:
        return
    x0, y0 = max(0, x), max(0, y)
    x1, y1 = min(img.shape[1], x + w), min(img.shape[0], y + h)
    if x1 <= x0 or y1 <= y0:
        return
    img[y0:y1, x0:x1, :3] = colour
    img[y0:y1, x0:x1, 3] = 255


def stamp(img, art, x0, y0, colour_fn):
    """Рисуем ASCII-маску: '#' — тело, '.' — пусто."""
    for j, row in enumerate(art):
        for i, ch in enumerate(row):
            if ch == '#':
                fill(img, x0 + i, y0 + j, 1, 1, colour_fn(i, j))


def outline(img, art, x0, y0, colour):
    """Тёмный контур вокруг ASCII-маски — читаемость на любом фоне."""
    h = len(art)
    w = len(art[0])
    solid = {(i, j) for j in range(h) for i in range(w) if art[j][i] == '#'}
    for (i, j) in solid:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = i + dx, j + dy
            if (nx, ny) not in solid:
                fill(img, x0 + nx, y0 + ny, 1, 1, colour)


def box_net(img, u, v, w, h, d, pal, tint=1.0, rng=RNG):
    """Раскладываем развёртку куба (w,h,d) в UV-регион (u,v) как это делает MC."""
    faces = {
        'top': (u + d, v, w, d, 1.00),
        'bottom': (u + d + w, v, w, d, 0.55),
        'east': (u, v + d, d, h, 0.72),
        'north': (u + d, v + d, w, h, 0.90),
        'west': (u + d + w, v + d, d, h, 0.66),
        'south': (u + 2 * d + w, v + d, w, h, 0.80),
    }
    for name, (fx, fy, fw, fh, amt) in faces.items():
        if name in ('top', 'bottom'):
            base = pal['plate_lite'] if name == 'top' else pal['plate_dark']
        elif name == 'north':
            base = pal['plate']
        else:
            base = pal['plate_dark'] if name == 'east' else pal['plate']
        for j in range(fh):
            for i in range(fw):
                edge = (i == 0 or j == 0 or i == fw - 1 or j == fh - 1)
                fill(img, fx + i, fy + j, 1, 1,
                     shade(base, amt * tint * (0.86 if edge else 1.0), 5.0, rng))
    # окантовка: тонкая светлая линия по верху груди/головы
    return faces


# --------------------------------------------------------------------------- item art
HANDLE = [(1 + i, 14 - i) for i in range(10)] + [(2 + i, 14 - i) for i in range(10)]
HANDLE_SHORT = [(4 + i, 14 - i) for i in range(6)] + [(5 + i, 14 - i) for i in range(6)]

HEADS = {
    'pickaxe': [
        '...##...',
        '..####..',
        '.##..##.',
        '##....##',
        '##....##',
        '.#....#.',
        '........',
        '........',
    ],
    'axe': [
        '..####..',
        '.######.',
        '########',
        '########',
        '.######.',
        '..####..',
        '........',
        '........',
    ],
    'shovel': [
        '..####..',
        '.######.',
        '.######.',
        '..####..',
        '...##...',
        '........',
        '........',
        '........',
    ],
    'hoe': [
        '#######.',
        '#######.',
        '.....##.',
        '.....##.',
        '........',
        '........',
        '........',
        '........',
    ],
    'sword': [
        '.....##.',
        '....###.',
        '...###..',
        '..###...',
        '.###....',
        '#######.',
        '...##...',
        '...##...',
    ],
}

ARMOR_ART = {
    'helmet': [
        '................',
        '.....######.....',
        '...##########...',
        '..############..',
        '.##############.',
        '.###........###.',
        '.###........###.',
        '.##############.',
        '.##############.',
        '..############..',
        '...##########...',
        '....########....',
        '................',
        '................',
        '................',
        '................',
    ],
    'chestplate': [
        '................',
        '..####....####..',
        '.##############.',
        '################',
        '######....######',
        '######....######',
        '################',
        '################',
        '################',
        '.##############.',
        '.##############.',
        '..############..',
        '..############..',
        '..###......###..',
        '................',
        '................',
    ],
    'leggings': [
        '................',
        '.##############.',
        '.##############.',
        '.##############.',
        '.#####....#####.',
        '.#####....#####.',
        '.####......####.',
        '.####......####.',
        '.####......####.',
        '.####......####.',
        '.####......####.',
        '.###........###.',
        '.###........###.',
        '................',
        '................',
        '................',
    ],
    'boots': [
        '................',
        '................',
        '..#####..#####..',
        '..#####..#####..',
        '..#####..#####..',
        '..#####..#####..',
        '..#####..#####..',
        '..############..',
        '..############..',
        '.########..####.',
        '.########..####.',
        '.#######....###.',
        '................',
        '................',
        '................',
        '................',
    ],
}

INGOT_ART = [
    '................',
    '................',
    '.....######.....',
    '....########....',
    '...##########...',
    '..############..',
    '..############..',
    '.##############.',
    '.##############.',
    '..############..',
    '..############..',
    '...##########...',
    '....########....',
    '.....######.....',
    '................',
    '................',
]

CRYSTAL_ART = [
    '................',
    '.......##.......',
    '......####......',
    '.....######.....',
    '....########....',
    '...##########...',
    '..############..',
    '..############..',
    '...##########...',
    '....########....',
    '.....######.....',
    '......####......',
    '.......##.......',
    '................',
    '................',
    '................',
]


def tool(name, pal, wood):
    img = new(16, 16)
    art = HEADS[name]
    # рукоять
    for (px, py) in (HANDLE_SHORT if name == 'sword' else HANDLE):
        t = (px + (16 - py)) / 26.0
        col = shade(wood['mid'], 0.75 + 0.45 * t, 5.0)
        fill(img, px, py, 1, 1, col)
    for (px, py) in (HANDLE_SHORT if name == 'sword' else HANDLE):
        fill(img, px, py + 1, 1, 1, shade(wood['dark'], 0.8, 4.0))
    outline(img, [''.join('#' if (x, y) in set(HANDLE) else '.' for x in range(16))
                  for y in range(16)], 0, 0, wood['dark'])

    def head_colour(i, j):
        t = (j / 7.0) * 0.5 + (i / 7.0) * 0.5
        base = pal['trim'] if t < 0.45 else pal['trim_lite']
        return shade(base, 0.82 + 0.36 * (1.0 - t), 7.0)

    stamp(img, art, 8, 1, head_colour)
    outline(img, art, 8, 1, pal['line'])
    # маджентовый акцент у рифт-кристалла — как на референсе 02
    if pal is RIFT:
        for (i, j) in ((0, 0), (1, 1), (6, 0), (5, 1)):
            fill(img, 8 + i, 1 + j, 1, 1, shade(pal['trim_lite'], 1.0, 4.0))
    return img


def armour_item(slot, pal):
    img = new(16, 16)
    art = ARMOR_ART[slot]

    def colour(i, j):
        t = (j / 15.0)
        base = pal['plate'] if t < 0.5 else pal['plate_dark']
        return shade(base, 0.80 + 0.40 * (1.0 - t), 6.0)

    stamp(img, art, 0, 0, colour)
    outline(img, art, 0, 0, pal['line'])
    h = len(art)
    w = len(art[0])
    solid = {(i, j) for j in range(h) for i in range(w) if art[j][i] == '#'}
    # блик: верхняя грань светлее
    for (i, j) in solid:
        if (i, j - 1) not in solid:
            fill(img, i, j, 1, 1, shade(pal['plate_lite'], 0.95, 4.0))
    # окантовка металлом
    for (i, j) in sorted(solid):
        if (i, j + 1) not in solid and j > 2:
            fill(img, i, j, 1, 1, shade(pal['trim'], 0.95, 5.0))
    if slot == 'helmet':
        for i in range(4, 12):
            fill(img, i, 5, 1, 1, shade(pal['line'], 1.0, 3.0))
            fill(img, i, 6, 1, 1, shade(pal['line'], 1.0, 3.0))
    if slot == 'chestplate':
        for j in range(3, 9):
            fill(img, 7, j, 1, 1, shade(pal['trim'], 1.0, 4.0))
            fill(img, 8, j, 1, 1, shade(pal['trim'], 1.0, 4.0))
    return img


def simple_item(art, pal, accent):
    img = new(16, 16)

    def colour(i, j):
        t = (i + j) / 30.0
        base = pal if isinstance(pal, tuple) else pal
        return shade(base, 0.78 + 0.44 * (1.0 - t), 7.0)

    stamp(img, art, 0, 0, colour)
    outline(img, art, 0, 0, accent)
    h = len(art)
    w = len(art[0])
    solid = {(i, j) for j in range(h) for i in range(w) if art[j][i] == '#'}
    for (i, j) in sorted(solid):
        if (i - 1, j - 1) not in solid:
            fill(img, i, j, 1, 1, shade(accent if not isinstance(accent, tuple) else accent, 1.15, 4.0))
    return img


# --------------------------------------------------------------------------- armour layers
def armour_layer(pal, kind):
    """kind='1' — шлем/кираса/ботинки (весь 64x32), kind='2' — только нагрудник и штаны."""
    img = new(64, 32)
    if kind == '1':
        box_net(img, 0, 0, 8, 8, 8, pal)        # голова
        box_net(img, 32, 0, 8, 8, 8, pal, 1.06)  # «шляпа» — внешний слой головы
    box_net(img, 16, 16, 8, 12, 4, pal)         # торс
    box_net(img, 40, 16, 4, 12, 4, pal)         # правая рука
    box_net(img, 0, 16, 4, 12, 4, pal)          # правая нога
    # светлая окантовка по низу торса и поясу
    for i in range(16, 40):
        fill(img, i, 27, 1, 1, shade(pal['trim'], 0.95, 4.0))
        fill(img, i, 16, 1, 1, shade(pal['trim'], 0.95, 4.0))
    for i in range(40, 56):
        fill(img, i, 27, 1, 1, shade(pal['trim'], 0.95, 4.0))
    # ремень/подкладка
    for i in range(16, 40):
        fill(img, i, 24, 1, 1, shade(pal['leather'], 0.9, 4.0))
    if kind == '1':
        # смотровая щель шлема
        for i in range(8, 24):
            fill(img, i, 8, 1, 1, shade(pal['line'], 1.0, 3.0))
        for i in range(40, 56):
            fill(img, i, 8, 1, 1, shade(pal['line'], 1.0, 3.0))
    if pal is RIFT:
        # бирюзовое свечение + маджентовые искры, как на референсе 02
        for (x, y) in ((20, 19), (21, 19), (34, 19), (35, 19), (6, 4), (44, 20)):
            fill(img, x, y, 1, 1, shade(pal['trim'], 1.25, 6.0))
        for (x, y) in ((26, 22), (29, 22), (12, 6)):
            fill(img, x, y, 1, 1, shade(pal['trim_lite'], 1.1, 6.0))
    return img


# --------------------------------------------------------------------------- mob skins
def skin(pal, *, hood=False, plume=False, quiver=False):
    """64x64 скин под ModelLayers.PLAYER."""
    img = new(64, 64)
    box_net(img, 0, 0, 8, 8, 8, {**pal, 'plate': pal['skin'], 'plate_lite': pal['skin'],
                                 'plate_dark': pal['robe_dark']})
    box_net(img, 32, 0, 8, 8, 8, {**pal, 'plate': pal['hair'], 'plate_lite': pal['hair'],
                                  'plate_dark': pal['hair']})
    box_net(img, 16, 32, 8, 12, 4, {**pal, 'plate': pal['robe'], 'plate_lite': pal['robe_lite'],
                                    'plate_dark': pal['robe_dark']})
    box_net(img, 16, 16, 8, 12, 4, {**pal, 'plate': pal['cloth'], 'plate_lite': pal['cloth'],
                                    'plate_dark': pal['robe_dark']})
    box_net(img, 40, 32, 4, 12, 4, {**pal, 'plate': pal['robe'], 'plate_lite': pal['robe_lite'],
                                    'plate_dark': pal['robe_dark']})
    box_net(img, 40, 16, 4, 12, 4, {**pal, 'plate': pal['cloth'], 'plate_lite': pal['cloth'],
                                    'plate_dark': pal['robe_dark']})
    box_net(img, 48, 48, 4, 12, 4, {**pal, 'plate': pal['robe'], 'plate_lite': pal['robe_lite'],
                                    'plate_dark': pal['robe_dark']})
    box_net(img, 0, 32, 4, 12, 4, {**pal, 'plate': pal['boot'], 'plate_lite': pal['boot'],
                                   'plate_dark': pal['line']})
    box_net(img, 0, 16, 4, 12, 4, {**pal, 'plate': pal['cloth'], 'plate_lite': pal['cloth'],
                                   'plate_dark': pal['line']})
    box_net(img, 0, 48, 4, 12, 4, {**pal, 'plate': pal['boot'], 'plate_lite': pal['boot'],
                                   'plate_dark': pal['line']})
    # лицо: глаза на передней грани головы (север = (d, d) .. )
    for (x, y) in ((2, 8), (5, 8)):
        fill(img, 8 + x, 8 + y - 8 + 8, 1, 1, shade(pal['line'], 1.0, 2.0))
    if hood:
        for i in range(32, 64):
            for j in range(0, 5):
                fill(img, i, j, 1, 1, shade(pal['robe'], 0.85, 4.0))
    if plume:
        for j in range(0, 4):
            fill(img, 40 + j, j, 1, 1, shade(pal['plume'], 1.0, 4.0))
            fill(img, 47 - j, j, 1, 1, shade(pal['plume'], 1.0, 4.0))
    if quiver:
        for j in range(20, 30):
            fill(img, 26, j, 1, 1, shade(pal['line'], 1.0, 3.0))
            fill(img, 27, j, 1, 1, shade(pal['hair'], 0.9, 3.0))
    return img


# --------------------------------------------------------------------------- block
def ore_block(host, gem):
    img = new(16, 16)
    hp = np.array([[[0.0]]])
    for y in range(16):
        for x in range(16):
            n = RNG.normal(0, 12)
            base = rgb(host) * (0.82 + 0.30 * ((x * 7 + y * 13) % 5) / 5.0) + n
            img[y, x, :3] = tuple(int(max(0, min(255, v))) for v in base)
            img[y, x, 3] = 255
    # прожилки самоцвета
    for (cx, cy, r) in ((4, 4, 2), (11, 6, 2), (7, 11, 2), (13, 12, 1)):
        for y in range(max(0, cy - r), min(16, cy + r + 1)):
            for x in range(max(0, cx - r), min(16, cx + r + 1)):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                    t = ((x + y) % 4) / 4.0
                    col = rgb(gem) * (0.78 + 0.44 * t)
                    img[y, x, :3] = tuple(int(max(0, min(255, v))) for v in col)
    return img


def main() -> None:
    for d in (ITEMS, BLOCKS, ARMOR, ENTITY, REPORT):
        d.mkdir(parents=True, exist_ok=True)

    written = []

    def save(path, img):
        Image.fromarray(img, "RGBA").save(path)
        written.append(path)

    # --- материалы
    save(ITEMS / "aurite_ingot.png", simple_item(INGOT_ART, AURITE['trim'], AURITE['line']))
    save(ITEMS / "riftcrystal.png", simple_item(CRYSTAL_ART, RIFT['trim'], RIFT['trim_lite']))

    # --- инструменты и оружие: два полных комплекта
    for mat, pal, wood in (("aurite", AURITE, WOOD), ("riftcrystal", RIFT, WOOD_COLD)):
        for tool_name in ("pickaxe", "axe", "shovel", "hoe", "sword"):
            save(ITEMS / f"{mat}_{tool_name}.png", tool(tool_name, pal, wood))
        for slot in ("helmet", "chestplate", "leggings", "boots"):
            save(ITEMS / f"{mat}_{slot}.png", armour_item(slot, pal))
        save(ARMOR / f"{mat}_layer_1.png", armour_layer(pal, '1'))
        save(ARMOR / f"{mat}_layer_2.png", armour_layer(pal, '2'))

    # --- руда рифт-кристалла
    save(BLOCKS / "riftcrystal_ore.png", ore_block((58, 62, 70), RIFT['trim']))

    # --- жители Элисара
    save(ENTITY / "elisar_villager.png", skin(VILLAGER, hood=True))
    save(ENTITY / "elisar_guard.png", skin(GUARD, plume=True))
    save(ENTITY / "elisar_archer.png", skin(ARCHER, quiver=True))

    print(f"Wrote {len(written)} textures:")
    for p in written:
        print("  ", p.relative_to(ROOT))


if __name__ == "__main__":
    main()
