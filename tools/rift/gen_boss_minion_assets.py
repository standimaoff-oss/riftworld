#!/usr/bin/env python3
"""Генератор 3D GeoModel-моделей, текстур сущностей и иконок предметов для v0.19.0:
1) Босс Колоннады «Расколотый Обелиск Разлома» (rift_obelisk_boss) — БЕЗ малейшей человеческой тени!
   Стационарный левитирующий расколотый бело-мраморный обелиск с чистым геометрическим призматическим ядром
   света, кристальными наростами и 3 вращающимися руническими кольцами астролябии.
2) Призываемый Прислужник Разлома «Гранёный Прислужник» (crystal_minion) — парящий безногий конструкт:
   треснувшая мраморная маска с кристальной короной, призрачный бирюзовый шлейф и парящие клинки-катары на руках.
3) 4 уникальных предмета/оружия с 3D-рендером в руке:
   - crystal_katar («Длань Осколка» — многоразовый клинок-наруч точь-в-точь как на руке прислужника)
   - crystal_barrage («Веер Осколков» — стакающийся одноразовый кристальный залп ЛКМ / до 5 кристаллов ПКМ)
   - crystal_aegis («Бастион Граней» — стакающийся одноразовый таранный ЛКМ / защитный 6с ПКМ щит)
   - astral_prism («Призма Архонта Разлома» — сбалансированный трофей с босса: блинк + маяк резонанса)
"""
import json
import math
import os
import random
import struct
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "../.."))
ASSETS = os.path.join(ROOT, "mod/src/main/resources/assets/riftworld")
GEO_DIR = os.path.join(ASSETS, "geo")
TEX_ENT = os.path.join(ASSETS, "textures/entity")
TEX_ITEM = os.path.join(ASSETS, "textures/item")
MOD_ITEM = os.path.join(ASSETS, "models/item")


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


def make_shared_entity_atlas(seed, shield_translucent=False):
    """Создаёт детализированный атлас 256x256 с 4 изолированными материальными квадрантами 128x128:
    [0..127, 0..127]     — Белый колотый мрамор с серебристыми прожилками и бирюзовыми трещинами
    [128..255, 0..127]   — Гранёный аквамариново-бирюзовый кристалл Разлома
    [0..127, 128..255]   — Чистое сияющее бело-голубое призматическое ядро / эфирный шлейф (0% чёрного/теней!)
    [128..255, 128..255] — Золочёная бронза рунических колец астролябии с бирюзовыми глифами
    """
    rng = random.Random(seed)
    px = [[(255, 255, 255, 255) for _ in range(256)] for _ in range(256)]

    for y in range(256):
        for x in range(256):
            if x < 128 and y < 128:
                v = math.sin(x * 0.23 + y * 0.17) + 0.5 * math.cos(x * 0.53 - y * 0.39)
                crack = abs(math.sin((x + y * 1.4) * 0.19 + math.sin(y * 0.31))) < 0.055
                glow_seam = abs(math.sin((x * 1.3 - y) * 0.17)) < 0.038
                if glow_seam:
                    col = (118, 246, 255, 255)
                elif crack:
                    col = (128, 140, 162, 255)
                elif v > 0.45:
                    col = (242, 245, 251, 255)
                elif v > -0.25:
                    col = (222, 228, 239, 255)
                else:
                    col = (196, 205, 222, 255)
                j = rng.randint(-4, 4)
                px[y][x] = (col[0] + j, col[1] + j, col[2] + j, 255)

            elif x >= 128 and y < 128:
                lx, ly = x - 128, y
                facet = ((lx // 4) * 7 + (ly // 4) * 13) % 5
                diag = (lx + ly) % 8 == 0 or (lx - ly) % 9 == 0
                alpha = 212 if shield_translucent else 255
                if diag:
                    col = (218, 254, 255, 255)
                elif facet == 0:
                    col = (34, 156, 194, alpha)
                elif facet == 1:
                    col = (56, 194, 224, alpha)
                elif facet == 2:
                    col = (88, 224, 244, alpha)
                elif facet == 3:
                    col = (134, 242, 254, alpha)
                else:
                    col = (46, 174, 210, alpha)
                j = rng.randint(-5, 5)
                px[y][x] = (col[0] + j, col[1] + j, col[2] + j, col[3])

            elif x < 128 and y >= 128:
                lx, ly = x, y - 128
                d = math.hypot(lx - 63.5, ly - 63.5) / 90.0
                ray = 0.5 + 0.5 * math.cos(math.atan2(ly - 63.5, lx - 63.5) * 6.0)
                r = int(155 + (1.0 - min(1.0, d)) * 95 + ray * 12)
                g = int(238 + (1.0 - min(1.0, d)) * 17)
                px[y][x] = (min(255, r), min(255, g), 255, 238 if shield_translucent else 255)

            else:
                lx, ly = x - 128, y - 128
                rune = (lx % 6 in (2, 3) and ly % 5 in (1, 3)) and ((lx + ly) % 3 == 0)
                edge = ly % 4 == 0 or lx % 8 == 0
                if rune:
                    col = (126, 250, 255, 255)
                elif edge:
                    col = (246, 214, 118, 255)
                elif (lx + ly) % 2 == 0:
                    col = (212, 168, 72, 255)
                else:
                    col = (172, 128, 46, 255)
                j = rng.randint(-5, 5)
                px[y][x] = (col[0] + j, col[1] + j, col[2] + j, 255)
    return px


def write_geo(name, scale, translucent, bones):
    os.makedirs(GEO_DIR, exist_ok=True)
    path = os.path.join(GEO_DIR, f"{name}.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(
            {
                "texture_width": 256,
                "texture_height": 256,
                "render_scale": scale,
                "translucent": translucent,
                "bones": bones,
            },
            f,
            indent=2,
        )


def cube(origin, size, uv, rot=None):
    return {"origin": origin, "size": size, "uv": uv}


def build_boss_geo():
    """Босс Колоннады: Расколотый Обелиск Разлома (ВЫСОТА ~3.8 блока, 0 человеческой тени!).
    Состоит из:
    - prism_core: чистое геометрическое октаэдрическое кристальное ядро в центре разлома
    - obelisk_lower / obelisk_upper_left / obelisk_upper_right / crown_spire: расколотые левитирующие
      половины мраморного обелиска с проросшими аквамариновыми кристаллами
    - ring_inner / ring_mid / ring_outer: 3 концентрических рунических кольца астролябии
    - orbit_shards: 4 парящих вокруг обелиска кристальных монолита
    """
    UV_MARBLE = [4, 4]
    UV_CRYSTAL = [132, 4]
    UV_CORE = [8, 136]
    UV_GOLD = [132, 136]

    bones = [
        {
            "name": "body",
            "pivot": [0, 28, 0],
            "cubes": [],
        },
        # Чистое геометрическое сияющее призматическое ядро (БЕЗ человеческого силуэта!)
        {
            "name": "prism_core",
            "parent": "body",
            "pivot": [0, 29, 0],
            "rotation": [0, 45, 0],
            "cubes": [
                cube([-4.5, 21, -4.5], [9, 16, 9], UV_CORE),
                cube([-3.0, 17, -3.0], [6, 24, 6], UV_CORE),
                cube([-6.0, 25, -2.0], [12, 8, 4], UV_CRYSTAL),
                cube([-2.0, 25, -6.0], [4, 8, 12], UV_CRYSTAL),
            ],
        },
        # Нижний парящий замковый камень обелиска с кристальным шипом вниз
        {
            "name": "lower_pedestal",
            "parent": "body",
            "pivot": [0, 10, 0],
            "cubes": [
                cube([-6, 7, -6], [12, 6, 12], UV_MARBLE),
                cube([-4, 3, -4], [8, 4, 8], UV_MARBLE),
                cube([-2, 0, -2], [4, 4, 4], UV_CRYSTAL),
                cube([-7, 9, -2], [2, 5, 4], UV_GOLD),
                cube([5, 9, -2], [2, 5, 4], UV_GOLD),
            ],
        },
        # Левая расколотая мраморная колонна обелиска с кристальными гранями
        {
            "name": "obelisk_left",
            "parent": "body",
            "pivot": [-8, 28, 0],
            "rotation": [0, 0, 5],
            "cubes": [
                cube([-12, 14, -5], [6, 26, 10], UV_MARBLE),
                cube([-11, 40, -4], [5, 9, 8], UV_MARBLE),
                cube([-14, 20, -2], [3, 11, 4], UV_CRYSTAL),
                cube([-13, 33, -3], [3, 9, 6], UV_CRYSTAL),
                cube([-12.5, 25, -5.5], [7, 2, 11], UV_GOLD),
            ],
        },
        # Правая расколотая мраморная колонна обелиска (асимметричный скол!)
        {
            "name": "obelisk_right",
            "parent": "body",
            "pivot": [8, 28, 0],
            "rotation": [0, 0, -5],
            "cubes": [
                cube([6, 15, -5], [6, 25, 10], UV_MARBLE),
                cube([6, 40, -4], [5, 10, 8], UV_MARBLE),
                cube([11, 18, -3], [3, 12, 6], UV_CRYSTAL),
                cube([10, 34, -2], [4, 10, 4], UV_CRYSTAL),
                cube([5.5, 27, -5.5], [7, 2, 11], UV_GOLD),
            ],
        },
        # Верхний парящий венец-пирамидион обелиска
        {
            "name": "apex_crown",
            "parent": "body",
            "pivot": [0, 53, 0],
            "cubes": [
                cube([-5, 49, -5], [10, 6, 10], UV_MARBLE),
                cube([-3.5, 55, -3.5], [7, 6, 7], UV_CRYSTAL),
                cube([-1.5, 61, -1.5], [3, 6, 3], UV_CORE),
                cube([-6, 50, -1], [12, 2, 2], UV_GOLD),
            ],
        },
        # Кольцо астролябии №1 (внутреннее быстрое)
        {
            "name": "ring_inner",
            "parent": "body",
            "pivot": [0, 29, 0],
            "rotation": [24, 0, 18],
            "cubes": [
                cube([-15, 28, -15], [30, 2, 2], UV_GOLD),
                cube([-15, 28, 13], [30, 2, 2], UV_GOLD),
                cube([-15, 28, -13], [2, 2, 26], UV_GOLD),
                cube([13, 28, -13], [2, 2, 26], UV_GOLD),
            ],
        },
        # Кольцо астролябии №2 (среднее наклонное с кристальными узлами)
        {
            "name": "ring_mid",
            "parent": "body",
            "pivot": [0, 29, 0],
            "rotation": [-32, 25, -14],
            "cubes": [
                cube([-20, 28, -20], [40, 2, 2], UV_GOLD),
                cube([-20, 28, 18], [40, 2, 2], UV_GOLD),
                cube([-20, 28, -18], [2, 2, 36], UV_GOLD),
                cube([18, 28, -18], [2, 2, 36], UV_GOLD),
                cube([-2, 27, -21], [4, 4, 4], UV_CRYSTAL),
                cube([-2, 27, 17], [4, 4, 4], UV_CRYSTAL),
            ],
        },
        # Кольцо астролябии №3 (большое внешнее королевское руническое кольцо)
        {
            "name": "ring_outer",
            "parent": "body",
            "pivot": [0, 29, 0],
            "rotation": [16, -20, -35],
            "cubes": [
                cube([-25, 28, -25], [50, 2, 2], UV_GOLD),
                cube([-25, 28, 23], [50, 2, 2], UV_GOLD),
                cube([-25, 28, -23], [2, 2, 46], UV_GOLD),
                cube([23, 28, -23], [2, 2, 46], UV_GOLD),
                cube([-26, 27, -2], [4, 4, 4], UV_CORE),
                cube([22, 27, -2], [4, 4, 4], UV_CORE),
            ],
        },
        # Парящие спутниковые кристаллы вокруг обелиска
        {
            "name": "orbit_shards",
            "parent": "body",
            "pivot": [0, 26, 0],
            "cubes": [
                cube([-22, 16, -2], [3, 10, 3], UV_CRYSTAL),
                cube([19, 18, -2], [3, 10, 3], UV_CRYSTAL),
                cube([-2, 15, -22], [3, 9, 3], UV_CRYSTAL),
                cube([-2, 19, 19], [3, 9, 3], UV_CRYSTAL),
            ],
        },
    ]
    write_geo("rift_obelisk_boss", 1.12, False, bones)


def build_minion_geo():
    """Прислужник Колоннады (02_crystal_minions_ref.png):
    - head: треснувшая бледно-мраморная маска со светящимися глазами + асимметричная корона из кристаллов
    - body: парящий мраморно-кристальный торс с сияющим бирюзовым ядром
    - wisp_tail: сужающийся эфирный бирюзовый шлейф вместо ног
    - arm_left / arm_right: парящие мраморные наручи с длинными гранёными кристальными клинками-катарами!
    """
    UV_MARBLE = [6, 6]
    UV_CRYSTAL = [134, 6]
    UV_CORE = [10, 138]
    UV_GOLD = [134, 138]

    bones = [
        {
            "name": "body",
            "pivot": [0, 18, 0],
            "cubes": [
                cube([-4.5, 14, -3], [9, 9, 6], UV_MARBLE),
                cube([-2.5, 15, -3.4], [5, 7, 7], UV_CORE),
                # Асимметричные кристальные наплечники
                cube([-8.5, 19, -2.5], [4, 6, 5], UV_CRYSTAL),
                cube([4.5, 19, -2.5], [4, 7, 5], UV_CRYSTAL),
                cube([-5.0, 13, -3.2], [10, 2, 6], UV_GOLD),
            ],
        },
        {
            "name": "head",
            "parent": "body",
            "pivot": [0, 24, 0],
            "cubes": [
                # Треснувшая мраморная маска
                cube([-4, 24, -4], [8, 8, 7], UV_MARBLE),
                # Светящиеся прорези глаз
                cube([-3.2, 27, -4.3], [2, 1, 1], UV_CORE),
                cube([1.2, 27, -4.3], [2, 1, 1], UV_CORE),
                # Острые кристальные рога-корона
                cube([-3.5, 31, -2], [2, 6, 3], UV_CRYSTAL),
                cube([1.5, 31, -2], [2, 7, 3], UV_CRYSTAL),
                cube([-1.0, 32, -1], [2, 5, 2], UV_CORE),
            ],
        },
        {
            "name": "wisp_tail",
            "parent": "body",
            "pivot": [0, 14, 0],
            "cubes": [
                cube([-3.0, 8, -2.0], [6, 6, 4], UV_CRYSTAL),
                cube([-2.0, 3, -1.5], [4, 5, 3], UV_CORE),
                cube([-1.0, 0, -1.0], [2, 3, 2], UV_CORE),
            ],
        },
        # Правая рука с КРИСТАЛЬНЫМ КАТАРОМ (модель совпадает с выпадающим оружием crystal_katar!)
        {
            "name": "arm_right",
            "parent": "body",
            "pivot": [-8, 20, 0],
            "rotation": [15, 0, 8],
            "cubes": [
                # Мраморно-золотой наруч
                cube([-10.5, 14, -2.5], [4, 6, 5], UV_MARBLE),
                cube([-11.0, 13, -3.0], [5, 2, 6], UV_GOLD),
                # Боковые кристальные зубья гарды + длинный центральный клинок в -Z!
                cube([-10.5, 13, -9.0], [1, 2, 7], UV_CRYSTAL),
                cube([-7.5, 13, -9.0], [1, 2, 7], UV_CRYSTAL),
                cube([-9.5, 12.5, -17.0], [2, 3, 15], UV_CRYSTAL),
                cube([-9.0, 13.0, -21.0], [1, 2, 5], UV_CORE),
            ],
        },
        # Левая рука с таким же КРИСТАЛЬНЫМ КАТАРОМ
        {
            "name": "arm_left",
            "parent": "body",
            "pivot": [8, 20, 0],
            "rotation": [15, 0, -8],
            "cubes": [
                cube([6.5, 14, -2.5], [4, 6, 5], UV_MARBLE),
                cube([6.0, 13, -3.0], [5, 2, 6], UV_GOLD),
                cube([6.5, 13, -9.0], [1, 2, 7], UV_CRYSTAL),
                cube([9.5, 13, -9.0], [1, 2, 7], UV_CRYSTAL),
                cube([7.5, 12.5, -17.0], [2, 3, 15], UV_CRYSTAL),
                cube([8.0, 13.0, -21.0], [1, 2, 5], UV_CORE),
            ],
        },
    ]
    write_geo("crystal_minion", 0.96, False, bones)


def build_weapon_and_projectile_geos():
    UV_MARBLE = [8, 8]
    UV_CRYSTAL = [136, 8]
    UV_CORE = [12, 140]
    UV_GOLD = [136, 140]

    # 1) crystal_katar («Длань Осколка») — точная копия наруча-клинка с руки прислужника!
    # Ориентация: рукоять в [0,0,0], лезвие смотрит вперёд вдоль -Z
    katar_bones = [
        {
            "name": "weapon",
            "pivot": [0, 0, 0],
            "cubes": [
                # Мраморный боевой наруч-гарда с золотой оковкой
                cube([-3.0, -2.5, 1.0], [6, 5, 6], UV_MARBLE),
                cube([-3.5, -3.0, -1.0], [7, 6, 2], UV_GOLD),
                # Два боковых кристальных клыка гарды
                cube([-3.5, -1.5, -8.0], [1, 3, 8], UV_CRYSTAL),
                cube([2.5, -1.5, -8.0], [1, 3, 8], UV_CRYSTAL),
                # Длинный центральный двухлезвийный кристальный клинок с сияющим сердечником
                cube([-1.5, -2.0, -17.0], [3, 4, 16], UV_CRYSTAL),
                cube([-1.0, -1.5, -23.0], [2, 3, 7], UV_CORE),
                cube([-0.5, -1.0, -26.0], [1, 2, 4], UV_CORE),
            ],
        }
    ]
    write_geo("crystal_katar", 1.0, False, katar_bones)

    # 2) crystal_shard (3D снаряд и 3D парящий кристалл вокруг прицела игрока!)
    shard_bones = [
        {
            "name": "shard",
            "pivot": [0, 0, 0],
            "cubes": [
                cube([-1.5, -1.5, -6.0], [3, 3, 12], UV_CRYSTAL),
                cube([-1.0, -1.0, -9.0], [2, 2, 16], UV_CORE),
                cube([-2.5, -0.5, -2.0], [5, 1, 5], UV_GOLD),
            ],
        }
    ]
    write_geo("crystal_shard", 1.0, False, shard_bones)

    # 3) crystal_barrage_item (веер из 3 парящих осколков в руке игрока)
    barrage_bones = [
        {
            "name": "center",
            "pivot": [0, 0, 0],
            "cubes": [
                cube([-1.5, -1.5, -8.0], [3, 3, 14], UV_CORE),
                cube([-2.0, -2.0, -2.0], [4, 4, 4], UV_GOLD),
            ],
        },
        {
            "name": "left",
            "parent": "center",
            "pivot": [-4, 1, 0],
            "rotation": [0, 18, -12],
            "cubes": [cube([-5.0, 0.0, -6.0], [2, 2, 11], UV_CRYSTAL)],
        },
        {
            "name": "right",
            "parent": "center",
            "pivot": [4, 1, 0],
            "rotation": [0, -18, 12],
            "cubes": [cube([3.0, 0.0, -6.0], [2, 2, 11], UV_CRYSTAL)],
        },
    ]
    write_geo("crystal_barrage_item", 1.0, False, barrage_bones)

    # 4) crystal_shield (3D щит Бастиона Граней — и как сущность перед игроком, и уменьшенный в руке!)
    shield_bones = [
        {
            "name": "shield",
            "pivot": [0, 17, 0],
            "cubes": [
                # Центральное полупрозрачное аквамариновое силовое полотно щита
                cube([-14, 3, -1], [28, 28, 2], UV_CRYSTAL),
                cube([-4, 11, -2], [8, 12, 4], UV_CORE),
                # Выгнутые боковые крылья щита под углом
                cube([-19, 5, 0], [5, 24, 2], UV_CRYSTAL),
                cube([14, 5, 0], [5, 24, 2], UV_CRYSTAL),
                # Мраморно-золотая оправа и верхние кристальные гребни
                cube([-16, 30, -1.5], [32, 3, 3], UV_MARBLE),
                cube([-16, 1, -1.5], [32, 3, 3], UV_MARBLE),
                cube([-19, 4, -1.5], [3, 26, 3], UV_GOLD),
                cube([16, 4, -1.5], [3, 26, 3], UV_GOLD),
                cube([-2, 32, -1], [4, 5, 2], UV_CORE),
                cube([-11, 32, -1], [3, 4, 2], UV_CRYSTAL),
                cube([8, 32, -1], [3, 4, 2], UV_CRYSTAL),
            ],
        }
    ]
    write_geo("crystal_shield", 1.0, True, shield_bones)

    # 5) astral_prism («Призма Архонта Разлома» — миниатюрная руническая астролябия с кристаллом босса!)
    prism_bones = [
        {
            "name": "core",
            "pivot": [0, 0, 0],
            "rotation": [0, 45, 0],
            "cubes": [
                cube([-2.5, -4.5, -2.5], [5, 9, 5], UV_CORE),
                cube([-1.5, -6.5, -1.5], [3, 13, 3], UV_CRYSTAL),
            ],
        },
        {
            "name": "ring_a",
            "pivot": [0, 0, 0],
            "rotation": [28, 0, 22],
            "cubes": [
                cube([-7, -0.5, -7], [14, 1, 1], UV_GOLD),
                cube([-7, -0.5, 6], [14, 1, 1], UV_GOLD),
                cube([-7, -0.5, -6], [1, 1, 12], UV_GOLD),
                cube([6, -0.5, -6], [1, 1, 12], UV_GOLD),
            ],
        },
        {
            "name": "ring_b",
            "pivot": [0, 0, 0],
            "rotation": [-28, 35, -20],
            "cubes": [
                cube([-9, -0.5, -9], [18, 1, 1], UV_GOLD),
                cube([-9, -0.5, 8], [18, 1, 1], UV_GOLD),
                cube([-9, -0.5, -8], [1, 1, 16], UV_GOLD),
                cube([8, -0.5, -8], [1, 1, 16], UV_GOLD),
            ],
        },
    ]
    write_geo("astral_prism", 1.0, False, prism_bones)


def make_item_icons_and_models():
    """Рисует чёткие 16x16 иконки на случай слотов/подсказок + JSON-модели builtin/entity для 3D-рендера."""
    os.makedirs(TEX_ITEM, exist_ok=True)
    os.makedirs(MOD_ITEM, exist_ok=True)

    def blank16():
        return [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]

    # Иконка Длани Осколка (катара)
    kat = blank16()
    for i in range(11):
        x, y = 4 + i, 11 - i
        if 0 <= x < 16 and 0 <= y < 16:
            kat[y][x] = (180, 252, 255, 255)
            if x - 1 >= 0:
                kat[y][x - 1] = (64, 212, 238, 255)
            if y + 1 < 16:
                kat[y + 1][x] = (36, 152, 192, 255)
    for dy in range(10, 15):
        for dx in range(1, 6):
            if dx + (15 - dy) <= 6:
                kat[dy][dx] = (232, 236, 245, 255) if (dx + dy) % 2 == 0 else (214, 174, 78, 255)
    save_png(os.path.join(TEX_ITEM, "crystal_katar.png"), kat)

    # Иконка Веера Осколков (5 кристаллов веером)
    bar = blank16()
    rays = [(3, 9), (5, 5), (8, 3), (11, 5), (13, 9)]
    for rx, ry in rays:
        for s in range(5):
            px_x = int(round(rx + (8 - rx) * s / 7.0))
            px_y = int(round(ry + (13 - ry) * s / 7.0))
            bar[px_y][px_x] = (190, 254, 255, 255) if s == 0 else (72, 220, 244, 255)
            if px_x + 1 < 16:
                bar[px_y][px_x + 1] = (38, 162, 200, 255)
    save_png(os.path.join(TEX_ITEM, "crystal_barrage.png"), bar)

    # Иконка Бастиона Граней (щит)
    sh = blank16()
    for y in range(2, 14):
        hw = 5 if y < 9 else max(1, 14 - y)
        for x in range(8 - hw, 8 + hw):
            border = x in (8 - hw, 8 + hw - 1) or y in (2, 13)
            core = abs(x - 7.5) < 1.6 and 5 <= y <= 10
            sh[y][x] = (224, 190, 96, 255) if border else ((200, 254, 255, 255) if core else (68, 208, 236, 255))
    save_png(os.path.join(TEX_ITEM, "crystal_aegis.png"), sh)

    # Иконка Призмы Архонта
    pr = blank16()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            md = abs(x - 7.5) + abs(y - 7.5)
            if 5.3 <= d <= 6.6:
                pr[y][x] = (232, 192, 88, 255)
            elif md <= 4.2:
                pr[y][x] = (214, 255, 255, 255) if md <= 2.0 else (76, 226, 248, 255)
    save_png(os.path.join(TEX_ITEM, "astral_prism.png"), pr)

    # 3D builtin/entity JSON модели для всех 4 предметов + 2 spawn eggs
    for item_id in ("crystal_katar", "crystal_barrage", "crystal_aegis", "astral_prism"):
        with open(os.path.join(MOD_ITEM, f"{item_id}.json"), "w", encoding="utf-8") as f:
            json.dump(
                {
                    "parent": "builtin/entity",
                    "gui_light": "front",
                    "textures": {"particle": f"riftworld:item/{item_id}"},
                },
                f,
                indent=2,
            )

    for egg_id in ("rift_obelisk_boss_spawn_egg", "crystal_minion_spawn_egg"):
        with open(os.path.join(MOD_ITEM, f"{egg_id}.json"), "w", encoding="utf-8") as f:
            json.dump({"parent": "minecraft:item/template_spawn_egg"}, f, indent=2)


def main():
    atlas_solid = make_shared_entity_atlas(9011, shield_translucent=False)
    atlas_shield = make_shared_entity_atlas(9012, shield_translucent=True)
    save_png(os.path.join(TEX_ENT, "rift_obelisk_boss.png"), atlas_solid)
    save_png(os.path.join(TEX_ENT, "crystal_minion.png"), atlas_solid)
    save_png(os.path.join(TEX_ENT, "crystal_shard.png"), atlas_solid)
    save_png(os.path.join(TEX_ENT, "crystal_shield.png"), atlas_shield)

    build_boss_geo()
    build_minion_geo()
    build_weapon_and_projectile_geos()
    make_item_icons_and_models()
    print("Generated all Boss, Minion, Shield, Shard & 4 Weapon GeoModels + 128x128 entity textures + item models.")


if __name__ == "__main__":
    main()
