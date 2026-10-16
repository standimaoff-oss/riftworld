#!/usr/bin/env python3
"""Великий Храм Перехода, V6 «Собор Разлома» — спроектирован и построен с нуля (rev 7, п. 1).

Слои (nbt-y): 0..29 — трёхъярусный подземный склеп (yo=-30 в plan.json), 30 — пол собора,
выше — аркада, клирстор, своды, купол средокрестия, башни разной высоты.
Палитра — только блоки мода (+ air). Выход: data/riftworld/structures/city/temple_great.nbt
"""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import nbtio  # noqa: E402

R = random.Random(60617)

W, D = 81, 95                 # footprint
GY = 30                       # соборный пол (nbt-y); ниже — склеп
H = 106
CX, CZ = 40, 47               # центр

V = {}                        # (x,y,z) -> blockname


def put(x, y, z, b):
    if 0 <= x < W and 0 <= y < H and 0 <= z < D:
        V[(x, y, z)] = b


def box(x0, y0, z0, x1, y1, z1, b, hollow=False):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            for z in range(z0, z1 + 1):
                if hollow and (x0 < x < x1 and y0 < y < y1 and z0 < z < z1):
                    continue
                put(x, y, z, b)


def floor_slab(x0, y, z0, x1, z1, b):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            put(x, y, z, b)


def wall_ring(x0, z0, x1, z1, y0, y1, b, thick=1):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x < x0 + thick or x > x1 - thick or z < z0 + thick or z > z1 - thick:
                    put(x, y, z, b)


def disc(cx, cz, r, y, b, ring=False, r_in=0):
    for x in range(int(cx - r), int(cx + r) + 1):
        for z in range(int(cz - r), int(cz + r) + 1):
            d = ((x - cx) ** 2 + (z - cz) ** 2) ** 0.5
            if d <= r and (not ring or d >= r_in):
                put(x, y, z, b)


def column(x, z, y0, y1, b="temple_grand_column"):
    for y in range(y0, y1 + 1):
        put(x, y, z, b)


def lancet(wx, wz_axis, z, y0, h, w, glass="light_blue_stained_glass"):
    """Проём стрельчатого окна в стене x=const (wz_axis='x') или z=const ('z')."""
    for i in range(w):
        for y in range(y0, y0 + h):
            top = y - (y0 + h - 3)
            off = i - (w - 1) / 2.0
            if top >= 0 and abs(off) > (w / 2.0 - 1 - top * 0.5):
                continue
            if wz_axis == "x":
                put(wx, y, z + i, "air")
            else:
                put(z + i, y, wx, "air")
    for i in range(w):
        for y in range(y0, y0 + h):
            top = y - (y0 + h - 3)
            off = i - (w - 1) / 2.0
            if top >= 0 and abs(off) > (w / 2.0 - 1 - top * 0.5):
                continue
            if wz_axis == "x":
                if y % 4 == 3:
                    put(wx, y, z + i, "temple_gold")
                else:
                    put(wx, y, z + i, glass)
            else:
                if y % 4 == 3:
                    put(z + i, y, wx, "temple_gold")
                else:
                    put(z + i, y, wx, glass)


# ------------------------------------------------------------------ склеп (3 яруса)
def crypt():
    box(16, 0, 6, 64, 1, 76, "temple_bricks")                      # подушка
    for lvl, fy in enumerate((2, 11, 20)):
        cy = fy + 8
        floor_slab(17, fy, 7, 63, 75, "keep_stone")
        wall_ring(16, 6, 64, 76, fy, cy, "keep_stone", 1)
        floor_slab(16, cy + 1, 6, 64, 76, "keep_stone_cracked")    # потолок яруса
        for x in range(20, 61, 8):
            for z in range(12, 72, 12):
                column(x, z, fy + 1, cy, "temple_pillar")
        for x in range(20, 61, 8):                                 # капители
            for z in range(12, 72, 12):
                put(x, cy, z, "keep_cage_lantern" if (x + z) % 16 == 4 else "temple_gold")
        # саркофаги и ниши
        for i, (sx, sz) in enumerate([(24, 16), (32, 16), (48, 16), (56, 16), (24, 66), (56, 66)]):
            if lvl == 0:
                box(sx, fy + 1, sz, sx + 2, fy + 2, sz + 4, "keep_stone_cracked")
                put(sx + 1, fy + 3, sz + 2, "temple_gold")
        if lvl == 1:                                               # реликварий: духи стен
            for z in range(14, 70, 10):
                put(16, fy + 4, z, "temple_spirit")
                put(64, fy + 4, z, "temple_spirit")
        if lvl == 2:                                               # ход к ритуальному ядру
            box(38, fy + 1, 40, 42, fy + 6, 44, "demon_frame", hollow=True)
            put(40, fy + 1, 42, "mosaic_core")
    # лестницы между ярусами
    for lvl, fy in enumerate((2, 11)):
        for i in range(9):
            box(58, fy + 1 + i, 68 - i, 60, fy + 1 + i, 68 - i, "temple_bricks_stairs")
    # выход наверх, в собор
    for i in range(10):
        box(38, 21 + i, 30 - i, 42, 21 + i, 30 - i, "temple_bricks_stairs")
    box(37, 26, 24, 43, 29, 29, "temple_bricks", hollow=True)


# ------------------------------------------------------------------ подиум и лестницы с 4 сторон
def podium():
    for t, (x0, z0, x1, z1) in enumerate([(8, 0, 72, 90), (10, 1, 70, 89), (12, 2, 68, 88)]):
        y = 27 + t
        box(x0, y, z0, x1, y, z1, "temple_floor" if t == 2 else "temple_bricks_slab")
    # лестницы на все 4 стороны
    for i in range(4):
        y = 26 + i
        box(34 - i, y, -0 + 0, 46 + i, y, 0, "temple_bricks_stairs")          # юг (вход)
        box(34 - i, y, 94, 46 + i, y, 94, "temple_bricks_stairs")             # север
        box(8, y, 41 - i, 8, y, 53 + i, "temple_bricks_stairs")               # запад
        box(72, y, 41 - i, 72, y, 53 + i, "temple_bricks_stairs")             # восток


# ------------------------------------------------------------------ корпус собора
NAVE_X0, NAVE_X1 = 14, 66          # внешние стены
NAVE_Z0, NAVE_Z1 = 4, 78
AISLE_W = 9                        # боковые нефы
ARC_X0, ARC_X1 = NAVE_X0 + AISLE_W + 1, NAVE_X1 - AISLE_W - 1   # 24..56 аркада
FLOOR_Y, ARC_TOP = GY, GY + 14
CLER_TOP = ARC_TOP + 12
VAULT_TOP = CLER_TOP + 8


def shell():
    wall_ring(NAVE_X0, NAVE_Z0, NAVE_X1, NAVE_Z1, GY, CLER_TOP, "temple_bricks", 2)
    floor_slab(NAVE_X0, GY - 1, NAVE_Z0, NAVE_X1, NAVE_Z1, "temple_floor")
    floor_slab(NAVE_X0 + 2, GY, NAVE_Z0 + 2, NAVE_X1 - 2, NAVE_Z1 - 2, "temple_floor")
    # пилястры-контрфорсы снаружи + золотые тяги
    for z in range(NAVE_Z0 + 4, NAVE_Z1 - 3, 6):
        for x in (NAVE_X0 - 1, NAVE_X1 + 1):
            box(x, GY, z, x, CLER_TOP - 2, z + 1, "temple_bricks")
            put(x, ARC_TOP, z, "temple_gold")
            put(x, ARC_TOP, z + 1, "temple_gold")
    for x in range(NAVE_X0 + 4, NAVE_X1 - 3, 6):
        for z in (NAVE_Z0 - 1, NAVE_Z1 + 1):
            box(x, GY, z, x + 1, CLER_TOP - 2, z, "temple_bricks")
            put(x, ARC_TOP, z, "temple_gold")
            put(x + 1, ARC_TOP, z, "temple_gold")
    # золотая струна по всему периметру
    wall_ring(NAVE_X0, NAVE_Z0, NAVE_X1, NAVE_Z1, ARC_TOP, ARC_TOP, "temple_gold", 2)
    # клирстор: стрельчатые окна по бокам
    for z in range(NAVE_Z0 + 5, NAVE_Z1 - 4, 6):
        lancet(NAVE_X0 + 1, "x", z, ARC_TOP + 3, 8, 3)
        lancet(NAVE_X1 - 1, "x", z, ARC_TOP + 3, 8, 3)
        put(NAVE_X0 + 1, ARC_TOP + 1, z + 1, "temple_rose_window")
        put(NAVE_X1 - 1, ARC_TOP + 1, z + 1, "temple_rose_window")
    # розы на боковых стенах ниже
    for z in range(NAVE_Z0 + 8, NAVE_Z1 - 6, 12):
        put(NAVE_X0 + 1, GY + 8, z, "temple_rose_window")
        put(NAVE_X1 - 1, GY + 8, z, "temple_rose_window")


def arcade():
    for x in (ARC_X0, ARC_X1):
        for z in range(NAVE_Z0 + 4, NAVE_Z1 - 6, 7):
            column(x, z, GY, ARC_TOP - 2)
            put(x, ARC_TOP - 1, z, "temple_gold")
            put(x, ARC_TOP, z, "temple_grand_column")
            # арки между колоннами
            for zz in range(z + 1, z + 7):
                put(x, ARC_TOP - 1 - (3 if zz in (z + 3, z + 4) else 1 if zz in (z + 2, z + 5) else 0), zz, "temple_bricks_stairs")
    # своды: центральный неф — полуциркульный череп, боковые — коробовые
    for z in range(NAVE_Z0 + 2, NAVE_Z1 - 1):
        for x in range(ARC_X0, ARC_X1 + 1):
            off = abs(x - CX) / float(CX - ARC_X0 + 2)
            y = int(VAULT_TOP - off * off * 6)
            put(x, y, z, "temple_bricks_slab")
            put(x, y + 1, z, "temple_bricks")
            if off > 0.75:
                put(x, y - 1, z, "temple_bricks_stairs")
        for x in list(range(NAVE_X0 + 2, ARC_X0)) + list(range(ARC_X1 + 1, NAVE_X1 - 1)):
            off = 0.0 if x in (ARC_X0 - 1, ARC_X1 + 1) else 1.0
            y = ARC_TOP + 4 - int(off * 2)
            put(x, y, z, "temple_bricks_slab")
    # нервюры каждые 7 блоков
    for z in range(NAVE_Z0 + 4, NAVE_Z1 - 6, 7):
        for x in range(ARC_X0, ARC_X1 + 1):
            off = abs(x - CX) / float(CX - ARC_X0 + 2)
            y = int(VAULT_TOP - off * off * 6)
            put(x, y - 1, z, "temple_gold")
    # висячие фонари
    for z in range(NAVE_Z0 + 7, NAVE_Z1 - 6, 10):
        for x in (CX - 5, CX + 5):
            put(x, VAULT_TOP - 4, z, "elisar_chain")
            put(x, VAULT_TOP - 5, z, "keep_cage_lantern")


def roofs():
    # двускатная кровля центрального нефа с золотым коньком
    for z in range(NAVE_Z0 - 1, NAVE_Z1 + 2):
        for x in range(ARC_X0 - 1, CX + 1):
            t = (CX - x) / float(CX - ARC_X0 + 1)
            y = int(VAULT_TOP + 7 - t * 6)
            put(x, y, z, "temple_bricks_stairs")
            put(2 * CX - x, y, z, "temple_bricks_stairs")
        put(CX, VAULT_TOP + 7, z, "temple_gold")
        put(CX, VAULT_TOP + 8, z, "temple_gold")
    # односкатные кровли боковых нефов
    for z in range(NAVE_Z0 - 1, NAVE_Z1 + 2):
        for i, x in enumerate(range(NAVE_X0 - 1, ARC_X0)):
            y = ARC_TOP + 6 - int(i * 0.55)
            put(x, y, z, "temple_bricks_slab")
            put(NAVE_X1 + 1 - (x - (NAVE_X0 - 1)), y, z, "temple_bricks_slab")
    # кровля трансепта
    mz = 63
    for x in range(5, 76):
        for z in range(mz - 7, mz + 8):
            t = abs(z - mz) / 7.0
            y = int(ARC_TOP + 8 - t * 4)
            if 6 <= x <= 74:
                put(x, y, z, "temple_bricks_slab")
        put(x, ARC_TOP + 8, mz, "temple_gold")
    # пинакли на контрфорсах
    for z in range(NAVE_Z0 + 4, NAVE_Z1 - 3, 12):
        for x in (NAVE_X0 - 1, NAVE_X1 + 1):
            box(x, CLER_TOP - 1, z, x, CLER_TOP + 2, z + 1, "temple_bricks")
            put(x, CLER_TOP + 3, z, "temple_bricks_stairs")
            put(x, CLER_TOP + 4, z, "temple_gold")


def ritual():
    """Мозаичный круг ритуала в средокрестии + 8 свечей + руны дорожкой."""
    mz = 63
    disc(CX, mz, 6, GY, "temple_mosaic")
    disc(CX, mz, 6, GY, "temple_mosaic", ring=True, r_in=5)
    put(CX, GY, mz, "mosaic_core")
    for i in range(8):
        import math
        a = math.pi * 2 * i / 8 + math.pi / 8
        x = int(round(CX + 7.5 * math.cos(a)))
        z = int(round(mz + 7.5 * math.sin(a)))
        put(x, GY, z, "ritual_candle")
        put(x, GY + 1, z, "air")
    for z in range(NAVE_Z0 + 3, mz - 7):
        put(CX, GY, z, "temple_gold_rune")
        put(CX - 1, GY, z, "castle_carpet")
        put(CX + 1, GY, z, "castle_carpet")
    # четыре столпа средокрестия
    for x in (ARC_X0 - 1, ARC_X1 + 1):
        for z in (mz - 8, mz + 8):
            column(x, z, GY, VAULT_TOP + 2)
            put(x, VAULT_TOP + 3, z, "temple_gold")


def transept_dome():
    mz = 63
    # поперечный неф пробивает стены
    for y in range(GY, ARC_TOP + 2):
        for x in range(6, NAVE_X0):
            for z in range(mz - 6, mz + 7):
                put(x, y, z, "temple_bricks" if (x in (6, 7) or z in (mz - 6, mz + 6)) else "air")
        for x in range(NAVE_X1 + 1, 75):
            for z in range(mz - 6, mz + 7):
                put(x, y, z, "temple_bricks" if (x in (73, 74) or z in (mz - 6, mz + 6)) else "air")
    for x in (NAVE_X0, NAVE_X0 + 1, NAVE_X1 - 1, NAVE_X1):
        for z in range(mz - 5, mz + 6):
            for y in range(GY, ARC_TOP + 2):
                put(x, y, z, "air")
    for x in (6, 7, 73, 74):
        for z in (mz - 6, mz + 6):
            column(x, z, GY, ARC_TOP + 6, "temple_pillar")
    # розы в торцах трансепта
    for y in range(ARC_TOP + 3, ARC_TOP + 9):
        for x in (6, 74):
            put(x, y, mz, "temple_rose_window")
    # барабан и купол средокрестия
    for y in range(VAULT_TOP + 2, VAULT_TOP + 12):
        disc(CX, mz, 9, y, "temple_bricks", ring=True, r_in=7.4)
    for i in range(8):
        import math
        a = math.pi * 2 * i / 8
        x = int(round(CX + 8 * math.cos(a)))
        z = int(round(mz + 8 * math.sin(a)))
        for y in range(VAULT_TOP + 2, VAULT_TOP + 12):
            put(x, y, z, "temple_pillar")
        lancet_x = int(round(CX + 8 * math.cos(a + math.pi / 8)))
        lancet_z = int(round(mz + 8 * math.sin(a + math.pi / 8)))
        put(lancet_x, VAULT_TOP + 6, lancet_z, "temple_rose_window")
    r = 9
    y = VAULT_TOP + 12
    while r > 1:
        disc(CX, mz, r, y, "temple_bricks_slab", ring=True, r_in=r - 1.6)
        r -= 1.7
        y += 1
    disc(CX, mz, 2, y, "temple_gold")
    for i in range(1, 7):
        put(CX, y + i, mz, "temple_gold" if i < 5 else "temple_rose_window")
    put(CX, y + 7, mz, "temple_gold")


def apse():
    az = NAVE_Z1
    for y in range(GY, CLER_TOP - 2):
        disc(CX, az, 13, y, "temple_bricks", ring=True, r_in=11)
    disc(CX, az, 12, GY, "temple_floor")
    disc(CX, az, 11, GY, "temple_gold_rune", ring=True, r_in=9)
    # алтарь
    box(CX - 2, GY, az - 6, CX + 2, GY + 1, az - 2, "temple_gold")
    put(CX, GY + 2, az - 4, "temple_rose_window")
    for x in (CX - 4, CX + 4):
        put(x, GY, az - 4, "ritual_candle")
    # полукупол апсиды
    r = 12
    y = CLER_TOP - 2
    while r > 1:
        for x in range(int(CX - r), int(CX + r) + 1):
            for z in range(int(az - r), az + 1):
                d = ((x - CX) ** 2 + (z - az) ** 2) ** 0.5
                if r - 1.6 <= d <= r:
                    put(x, y, z, "temple_bricks_slab")
        r -= 1.7
        y += 1
    # окна апсиды
    import math
    for i in range(7):
        t = -1.15 + 2.3 * i / 6.0
        x = int(round(CX + 12 * math.sin(t)))
        z = int(round(az - 12 * math.cos(t)))
        for y in range(GY + 5, GY + 12):
            put(x, y, z, "light_blue_stained_glass" if y % 4 else "temple_gold")


def towers():
    """8 башен разной высоты: 2 фасадных звонницы + 4 угловых + 2 над трансептом."""
    spec = [((10, 8), 96, 5), ((70, 8), 88, 5), ((10, 74), 80, 4), ((70, 74), 74, 4),
            ((6, 60), 66, 3), ((74, 60), 70, 3), ((10, 41), 60, 3), ((70, 41), 64, 3)]
    for (tx, tz), top, rad in spec:
        for y in range(27, top):
            for x in range(tx - rad, tx + rad + 1):
                for z in range(tz - rad, tz + rad + 1):
                    on_edge = abs(x - tx) == rad or abs(z - tz) == rad
                    corner = abs(x - tx) == rad and abs(z - tz) == rad
                    if corner:
                        put(x, y, z, "temple_bricks")
                    elif on_edge:
                        slit = (top - 10 <= y <= top - 4) and ((x - tx) == 0 or (z - tz) == 0)
                        band = (y in (46, 47) or y in (58, 59)) and ((x + z) % 2 == 0)
                        if slit:
                            put(x, y, z, "light_blue_stained_glass")
                        elif band:
                            put(x, y, z, "temple_rose_window")
                        else:
                            put(x, y, z, "temple_bricks")
        # ярусные пояса
        for by in range(40, top - 6, 12):
            for x in range(tx - rad - 1, tx + rad + 2):
                for z in range(tz - rad - 1, tz + rad + 2):
                    if abs(x - tx) == rad + 1 or abs(z - tz) == rad + 1:
                        put(x, by, z, "temple_gold")
        # шатёр
        r = rad
        y = top
        while r > 0:
            for x in range(tx - r, tx + r + 1):
                for z in range(tz - r, tz + r + 1):
                    if max(abs(x - tx), abs(z - tz)) == r:
                        put(x, y, z, "temple_bricks_stairs")
            r -= 1
            y += 1
        put(tx, y, tz, "temple_gold")
        put(tx, y + 1, tz, "temple_gold")
        # дух-страж на фасадных башнях
        if tz == 8:
            put(tx, 44, tz - rad - 1 if tz < 40 else tz + rad + 1, "temple_spirit")


def facade():
    """Южный фасад: гексастиль в два ряда, фронтон, роза, золотые врата."""
    z = 2
    for x in range(20, 61, 8):
        column(x, z, 30, 48)
        column(x, z + 3, 30, 48)
        put(x, 49, z, "temple_gold")
        put(x, 49, z + 3, "temple_gold")
    box(18, 50, 1, 62, 52, 5, "temple_bricks")
    box(18, 51, 1, 62, 51, 5, "temple_gold")
    # фронтон
    for i in range(12):
        box(20 + i, 53 + (i if i < 6 else 11 - i), 2, 60 - i, 53 + (i if i < 6 else 11 - i), 4, "temple_bricks_stairs")
    put(CX, 57, 3, "temple_rose_window")
    # портал
    for y in range(30, 42):
        for x in range(CX - 4, CX + 5):
            arch = (y > 38) and abs(x - CX) > (4 - (y - 38))
            if not arch:
                put(x, y, 4, "air")
                put(x, y, 5, "air")
    for y in range(30, 43):
        put(CX - 5, y, 4, "temple_gold")
        put(CX + 5, y, 4, "temple_gold")
    put(CX - 5, 43, 4, "temple_gold")
    put(CX + 5, 43, 4, "temple_gold")
    for x in range(CX - 4, CX + 5):
        put(x, 42 - (0 if abs(x - CX) < 3 else 1), 4, "temple_gold")
    # роза фасада
    for y in range(44, 50):
        for x in range(CX - 3, CX + 4):
            if (x - CX) ** 2 + (y - 46.5) ** 2 <= 9.5:
                put(x, y, 4, "temple_rose_window")


def dressing():
    for x in range(38, 43):
        for z in range(19, 23):
            for y in range(27, 32):
                put(x, y, z, "air")
        put(x, 28, 21, "temple_bricks_stairs")
        put(x, 29, 20, "temple_bricks_stairs")
    for (x, y, z), b in list(V.items()):
        if b == "temple_bricks" and y > 28:
            r = R.random()
            if r < 0.035:
                V[(x, y, z)] = "temple_bricks_cracked"
            elif r < 0.055 and (x in (NAVE_X0, NAVE_X1) or z in (NAVE_Z0, NAVE_Z1)):
                V[(x, y, z)] = "temple_bricks_ivy"
    # скамьи в боковых нефах
    for z in range(NAVE_Z0 + 8, 56, 5):
        for x0 in (NAVE_X0 + 4, ARC_X1 + 4):
            box(x0, GY, z, x0 + 2, GY, z, "temple_bricks_stairs")
            box(x0, GY + 1, z, x0 + 2, GY + 1, z, "temple_bricks_slab")


def main():
    crypt()
    podium()
    shell()
    arcade()
    ritual()
    transept_dome()
    apse()
    towers()
    roofs()
    facade()
    dressing()
    B = {pos: ("riftworld:" + b, ()) for pos, b in V.items() if b != "air"}
    out = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                       "../../mod/src/main/resources/data/riftworld/structures/city/temple_great.nbt")
    nb, np_ = nbtio.write_template(os.path.normpath(out), (W, H, D), B, {})
    from collections import Counter
    c = Counter(V.values())
    print("temple V6: %dx%dx%d, blocks=%d, palette=%d" % (W, H, D, nb, np_))
    for k, v in c.most_common(14):
        print("   %-26s %d" % (k, v))


if __name__ == "__main__":
    main()
