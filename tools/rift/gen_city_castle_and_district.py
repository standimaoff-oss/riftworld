#!/usr/bin/env python3
"""Строит:
1) Красивый неквадратный Королевский Замок в центре Белого Города (data/riftworld/structures/city/city_castle.nbt)
   вместо старого квадратного храма temple_great: полигональные крепостные стены с зубцами и галереями,
   6 круглых и восьмигранных башен разной высоты под синими остроконечными шпилями с золотыми навершиями,
   парадный двухбашенный барбакан на юге, северные ворота в задний посад, многообъёмный асимметричный
   донжон с контрфорсами, витражами и тронным залом, фонтаны и внутренние постройки во дворах.
2) 4 новых городских здания (guildhall, manor, bakery, guardhouse) в structures/city/*.nbt.
3) Обновлённый data/riftworld/city/plan.json: сохраняет все спутниковые острова и мосты 1:1, ставит
   city_castle в центр и плотно застраивает пространство ЗА замком (северный посад, площадь с колодцем и
   лавками, ремесленные улочки, особняки, пекарни, сады и фонари) и по бокам, чтобы вокруг замка не было пустот.
"""
import json
import math
import os
import random
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "../.."))
sys.path.insert(0, HERE)
from nbtio import write_template

OUT_DATA = os.path.join(ROOT, "mod/src/main/resources/data/riftworld")
OUT_CITY = os.path.join(OUT_DATA, "structures/city")
JAVA_KINDS = os.path.join(ROOT, "mod/src/main/java/ru/aiforge/riftworld/block/ModelKind.java")

KINDS = {
    m.group(1): tuple(int(m.group(i)) for i in (2, 3, 4, 5))
    for m in re.finditer(r'[A-Z_]+\("([a-z_]+)", (-?\d+), (-?\d+), (\d+), (\d+),', open(JAVA_KINDS, encoding="utf-8").read())
}
LIT = {
    "lantern_hanging", "street_lamp", "fire_bowl", "fireplace", "cooking_pot",
    "altar_stone", "table_wood", "lantern_hang", "lantern_post", "lantern_wall", "fire_pit", "kitchen_stove",
}


def S(name, **p):
    if ":" not in name:
        name = "riftworld:" + name
    return (name, tuple(sorted((k, str(v).lower()) for k, v in p.items())))


AIR = S("minecraft:air")


# ==============================================================================
# 1. КОРОЛЕВСКИЙ ЗАМОК В ЦЕНТРЕ ГОРОДА (city_castle.nbt: 69 x 74 x 73, G = 12)
# ==============================================================================
def build_city_castle():
    W, H, D = 69, 74, 73
    G = 12  # уровень земли (h = 104, yo = -12 -> G ложится ровно на y = 104)
    CX, CZ = W // 2, D // 2  # (34, 36)
    B = {}
    rng = random.Random(20261031)

    TB = S("temple_bricks")
    TBM = S("temple_bricks_ivy")
    TBC = S("temple_bricks_cracked")
    TBS = lambda f, half="bottom": S("temple_bricks_stairs", facing=f, half=half, shape="straight", waterlogged=False)
    TBSl = lambda t="bottom": S("temple_bricks_slab", type=t, waterlogged=False)
    ASH = S("ashlar")
    ASHP = S("ashlar_pillar", axis="y")
    ASHS = lambda f, half="bottom": S("ashlar_stairs", facing=f, half=half, shape="straight", waterlogged=False)
    ASHSl = lambda t="bottom": S("ashlar_slab", type=t, waterlogged=False)
    EB = S("elisar_bricks")
    EBM = S("elisar_bricks_mossy")
    EC = S("elisar_cobble")
    EP = S("elisar_path")
    KS = S("keep_stone")
    TF = S("temple_floor")
    TM = S("temple_mosaic")
    TG = S("temple_gold")
    TGR = S("temple_gold_rune")
    GCOL = S("temple_grand_column", axis="y")
    RB = S("roof_blue")
    RBS = lambda f, half="bottom": S("roof_blue_stairs", facing=f, half=half, shape="straight", waterlogged=False)
    RBSl = lambda t="bottom": S("roof_blue_slab", type=t, waterlogged=False)
    RR = S("roof_red")
    DP = S("dark_planks")
    DBY = S("dark_beam", axis="y")
    DBX = S("dark_beam", axis="x")
    DBZ = S("dark_beam", axis="z")
    SP = S("sizo_planks")
    SPSl = lambda t="bottom": S("sizo_planks_slab", type=t, waterlogged=False)
    GL = S("minecraft:light_blue_stained_glass")
    BARS = S("minecraft:iron_bars", east=False, north=False, south=False, west=False, waterlogged=False)
    BARS_EW = S("minecraft:iron_bars", east=True, north=False, south=False, west=True, waterlogged=False)
    LANT = S("minecraft:lantern", hanging=False, waterlogged=False)
    LANT_H = S("minecraft:lantern", hanging=True, waterlogged=False)
    SEA_L = S("minecraft:sea_lantern")
    WATER = S("garden_water")
    GRASS = S("elisar_grass")
    FLOW = S("elisar_flowers")
    LEAVES = S("sizo_leaves", distance=1, persistent=True, waterlogged=False)
    LOG_S = S("sizo_log", axis="y")
    CARPET = S("minecraft:red_carpet")
    GOLD_CARP = S("minecraft:yellow_carpet")

    def put(x, y, z, st):
        if 0 <= x < W and 0 <= y < H and 0 <= z < D:
            B[(x, y, z)] = st

    def fill(x0, y0, z0, x1, y1, z1, st):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    put(x, y, z, st)

    def wall_stone(x, y, z):
        h = ((x * 734287 + y * 912931 + z * 19349663) & 0xFFFF) / 65535.0
        if y <= G + 1:
            return EBM if h < 0.22 else (KS if h < 0.55 else EB)
        if h < 0.08:
            return TBM
        if h < 0.24:
            return ASH
        if h < 0.42:
            return EB
        return TB

    # --- 1) Полигональный (восьмигранный со скошенными углами и выступами) контур крепости!
    # Не квадрат: срезанные диагональные куртины + расширенный южный двор и сужающийся северный бастион
    def inside_fortress(x, z, pad=0.0):
        dx = abs(x - CX)
        dz = z - CZ
        # Асимметрия север/юг: южный парадный двор шире, северная цитадель гранёная
        max_x = (28.5 if dz >= -4 else 25.5) + pad
        max_z_south = 29.5 + pad
        max_z_north = 30.5 + pad
        if dx > max_x or dz > max_z_south or dz < -max_z_north:
            return False
        # Диагональные скосы углов (восьмигранная цитадель, а не коробка!)
        if dx + max(0.0, dz) > 45.5 + pad * 1.4:
            return False
        if dx + max(0.0, -dz) > 43.5 + pad * 1.4:
            return False
        return True

    # Монолитный каменный фундамент от G-10 до G под всей крепостью + мощение дворов
    for x in range(1, W - 1):
        for z in range(1, D - 1):
            if inside_fortress(x, z, pad=2.0):
                for y in range(2, G):
                    put(x, y, z, KS if y < G - 3 else EB)
                if inside_fortress(x, z, pad=0.0):
                    # Внутреннее мощение дворов: каменные дорожки, мозаика и зелёные садовые клумбы
                    dx, dz = x - CX, z - CZ
                    if abs(dx) <= 3 or abs(dz - 12) <= 2 or abs(dz + 16) <= 2:
                        put(x, G, z, TF if (x + z) % 5 != 0 else TM)
                    elif (x + z) % 7 == 0:
                        put(x, G, z, EC)
                    else:
                        put(x, G, z, EB if (x * 3 + z) % 4 != 0 else ASH)
                    # Воздух над двором, чтобы не осталось холмов рельефа
                    for y in range(G + 1, G + 18):
                        put(x, y, z, AIR)
                else:
                    # Внешняя каменная отмостка вокруг стен замка
                    put(x, G, z, EC if (x + z) % 3 == 0 else EP)
                    for y in range(G + 1, G + 10):
                        put(x, y, z, AIR)

    # --- 2) Полигональные крепостные стены (куртины) высотой 10 блоков с боевым ходом и зубцами
    WALL_TOP = G + 10
    for x in range(2, W - 2):
        for z in range(2, D - 2):
            in0 = inside_fortress(x, z, 0.0)
            in_inner = inside_fortress(x, z, -2.6)
            if in0 and not in_inner:
                is_outer_skin = not inside_fortress(x, z, -1.1)
                is_inner_skin = not inside_fortress(x, z, -1.7) and not is_outer_skin
                for y in range(G + 1, WALL_TOP + 1):
                    if is_outer_skin or y == WALL_TOP or y <= G + 2:
                        put(x, y, z, wall_stone(x, y, z))
                    elif is_inner_skin and y <= WALL_TOP - 1:
                        # Внутренняя аркада под боевым ходом
                        if ((x + z) % 4 in (1, 2)) and G + 2 <= y <= G + 6:
                            put(x, y, z, AIR)
                        else:
                            put(x, y, z, ASH if y == G + 7 else wall_stone(x, y, z))
                    else:
                        put(x, y, z, SP if y == WALL_TOP - 1 else wall_stone(x, y, z))
                # Бойницы во внешней стене
                if is_outer_skin and (x + z) % 6 == 0 and abs(x - CX) > 6 and abs(z - CZ) > 5:
                    put(x, G + 5, z, BARS)
                    put(x, G + 6, z, BARS)
                # Зубцы (мерлоны) и фонари на боевом ходе
                if is_outer_skin:
                    if (x + z) % 2 == 0:
                        put(x, WALL_TOP + 1, z, TB)
                        put(x, WALL_TOP + 2, z, TBSl("bottom"))
                    else:
                        put(x, WALL_TOP + 1, z, ASHSl("bottom"))
                elif not inside_fortress(x, z, -2.5) and (x + z) % 2 == 0:
                    put(x, WALL_TOP + 1, z, ASHSl("bottom"))

    # --- 3) Круглые и гранёные башни разной высоты с коническими синими шпилями и золотыми иглами!
    def build_round_tower(tx, tz, rad, shaft_h, spire_h, roof_mat=RB, has_belfry=False):
        r_int = int(math.ceil(rad + 2))
        top_y = G + shaft_h
        for dx in range(-r_int, r_int + 1):
            for dz in range(-r_int, r_int + 1):
                d = math.hypot(dx, dz)
                x, z = tx + dx, tz + dz
                if d <= rad + 0.8:
                    for y in range(2, G + 1):
                        put(x, y, z, KS if y < G else TF)
                if d <= rad + 0.35:
                    for y in range(G + 1, top_y + 1):
                        if d >= rad - 1.05:
                            # Каменная кладка башни с поясками и узкими стрельчатыми окнами
                            if y in (G + shaft_h // 2, top_y - 1):
                                put(x, y, z, ASH)
                            elif (abs(dx) <= 0 and abs(dz) >= rad - 1.0 or abs(dz) <= 0 and abs(dx) >= rad - 1.0) and (
                                y in (G + 6, G + 7, top_y - 6, top_y - 5)
                            ):
                                put(x, y, z, GL)
                            else:
                                put(x, y, z, wall_stone(x, y, z))
                        else:
                            # Внутренние перекрытия башни и люстры
                            if (y - G) % 7 == 0 and y < top_y - 2:
                                put(x, y, z, DP)
                            else:
                                put(x, y, z, AIR)
                    if dx == 0 and dz == 0:
                        put(x, top_y - 2, z, SEA_L)
        # Выступающий машикуль-карниз наверху башни
        for dx in range(-r_int, r_int + 1):
            for dz in range(-r_int, r_int + 1):
                d = math.hypot(dx, dz)
                if rad - 0.5 <= d <= rad + 1.05:
                    put(tx + dx, top_y, tz + dz, TB)
                    if (dx + dz) % 2 == 0 and d >= rad + 0.15:
                        put(tx + dx, top_y + 1, tz + dz, TB)
        # Если это колокольня — открытый арочный ярус перед шпилем
        spire_base = top_y + 1
        if has_belfry:
            for y in range(top_y + 1, top_y + 6):
                for dx in range(-r_int, r_int + 1):
                    for dz in range(-r_int, r_int + 1):
                        d = math.hypot(dx, dz)
                        if d <= rad - 0.3:
                            put(tx + dx, y, tz + dz, AIR)
                        if rad - 1.1 <= d <= rad - 0.2 and abs(dx) >= 2 and abs(dz) >= 2:
                            put(tx + dx, y, tz + dz, ASHP)
            put(tx, top_y + 4, tz, TG)
            put(tx, top_y + 3, tz, LANT_H)
            spire_base = top_y + 6

        # Остроконечный конический шпиль с синей черепицей и золотыми рёбрами
        for sy in range(spire_h):
            cur_r = (rad + 1.15) * (1.0 - sy / float(max(1, spire_h)))
            y = spire_base + sy
            for dx in range(-r_int, r_int + 1):
                for dz in range(-r_int, r_int + 1):
                    d = math.hypot(dx, dz)
                    if d <= cur_r + 0.35:
                        if d >= cur_r - 0.95 or sy >= spire_h - 3:
                            gold_rib = (dx == 0 or dz == 0) and sy >= spire_h - 4
                            put(tx + dx, y, tz + dz, TG if gold_rib else roof_mat)
                        else:
                            put(tx + dx, y, tz + dz, AIR)
        # Золотой шпиль-флюгер на вершине
        for fy in range(spire_base + spire_h, spire_base + spire_h + 4):
            put(tx, fy, tz, TG)

    # 6 угловых/фланговых и воротных башен РАЗНОЙ ВЫСОТЫ (живой силуэт!)
    # Парадные южные башни барбакана у ворот (z = CZ + 27 = 63)
    build_round_tower(CX - 9, CZ + 26, 4.3, 19, 10, RB, has_belfry=False)
    build_round_tower(CX + 9, CZ + 26, 4.3, 22, 11, RB, has_belfry=False)
    # Западный и восточный бастионы
    build_round_tower(CX - 26, CZ + 4, 4.2, 17, 9, RB, has_belfry=False)
    build_round_tower(CX + 26, CZ + 5, 4.5, 21, 10, RB, has_belfry=True)
    # Высокие северные башни, смотрящие на задний посад (чтобы вид СЗАДИ замка тоже был королевским!)
    build_round_tower(CX - 20, CZ - 23, 4.8, 26, 12, RB, has_belfry=False)
    build_round_tower(CX + 20, CZ - 22, 4.6, 23, 11, RB, has_belfry=True)
    # Две малые угловые дозорные башенки на юго-западном и юго-восточном изломах стен
    build_round_tower(CX - 21, CZ + 18, 3.4, 15, 8, RB, has_belfry=False)
    build_round_tower(CX + 21, CZ + 18, 3.4, 16, 8, RB, has_belfry=False)

    # Надвратная перемычка южного барбакана (между башнями CX-9 и CX+9) + арочные ворота с решёткой
    for x in range(CX - 6, CX + 7):
        for z in range(CZ + 24, CZ + 29):
            for y in range(G + 1, G + 15):
                if abs(x - CX) <= 3 and y <= G + 7:
                    if z == CZ + 26 and y >= G + 6:
                        put(x, y, z, BARS_EW)  # поднятая кованая решётка (порткулис)!
                    else:
                        put(x, y, z, AIR)
                elif y == G + 14:
                    put(x, y, z, TB if (x + z) % 2 == 0 else ASHSl("bottom"))
                elif z in (CZ + 24, CZ + 28) or abs(x - CX) == 6:
                    if y in (G + 10, G + 11) and abs(x - CX) in (0, 3) and z == CZ + 28:
                        put(x, y, z, GL)
                    else:
                        put(x, y, z, TG if (y == G + 9 and abs(x - CX) <= 2 and z == CZ + 28) else wall_stone(x, y, z))
                else:
                    put(x, y, z, DP if y == G + 8 else AIR)
    # Мощёный выезд из южных ворот до края шаблона
    for x in range(CX - 4, CX + 5):
        for z in range(CZ + 24, D):
            put(x, G, z, TF if abs(x - CX) <= 2 else EC)
            for y in range(G + 1, G + 6):
                if z > CZ + 28:
                    put(x, y, z, AIR)

    # Северные задние ворота (Потерна) из замка прямо на Северную замковую площадь!
    for x in range(CX - 4, CX + 5):
        for z in range(0, CZ - 24):
            put(x, G, z, TF if abs(x - CX) <= 2 else EC)
    for x in range(CX - 3, CX + 4):
        for z in range(CZ - 31, CZ - 25):
            for y in range(G + 1, G + 7):
                if abs(x - CX) <= 2 and y <= G + 5:
                    put(x, y, z, BARS_EW if (y == G + 5 and z == CZ - 29) else AIR)
                elif abs(x - CX) == 3 or y == G + 6:
                    put(x, y, z, ASH)

    # --- 4) Многообъёмный НЕКВАДРАТНЫЙ Королевский Донжон и крылья дворца в центре замка!
    # Композиция:
    #  - Главный Тронный Неф (CX-10..CX+10, CZ-12..CZ+10, высота стен 20, двускатная синяя крыша с золотым коньком)
    #  - Возвышающаяся Северная Главная Башня-Цитадель (CX-7..CX+7, CZ-18..CZ-4, высота 34 + королевский шпиль до G+55!)
    #  - Западное Рыцарское Крыло под прямым углом (CX-19..CX-9, CZ-6..CZ+6, высота 13, синяя крыша)
    #  - Восточная Королевская Капелла с полукруглой апсидой (CX+9..CX+18, CZ-8..CZ+4, высота 15)
    #  - Аркбутаны (летающие контрфорсы) и парадный портик с колоннами на южном входе!

    def build_gable_wing(x0, x1, z0, z1, wall_h, axis="z", roof_mat=RB):
        top = G + wall_h
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                put(x, G, z, TM if (abs(x - CX) <= 3 and abs(z - CZ) <= 8) else TF)
                border = x in (x0, x1) or z in (z0, z1)
                corner = (x in (x0, x1)) and (z in (z0, z1))
                for y in range(G + 1, top + 1):
                    if corner:
                        put(x, y, z, ASHP)
                    elif border:
                        # Высокие стрельчатые витражные окна с переплётами
                        win_pos = ((z - z0) % 5 == 2) if x in (x0, x1) else ((x - x0) % 5 == 2)
                        if win_pos and G + 5 <= y <= top - 4:
                            put(x, y, z, GL)
                        elif y in (G + 4, top - 1, top):
                            put(x, y, z, ASH)
                        else:
                            put(x, y, z, wall_stone(x, y, z))
                    else:
                        put(x, y, z, AIR)
        # Крутая вальмово-щипцовая крыша с золотым коньком
        span = (x1 - x0) // 2 + 1 if axis == "z" else (z1 - z0) // 2 + 1
        for k in range(span + 1):
            ry = top + 1 + k
            if axis == "z":
                lx, rx = x0 - 1 + k, x1 + 1 - k
                if lx > rx:
                    break
                for z in range(z0 - 1, z1 + 2):
                    if lx == rx:
                        put(lx, ry, z, TG)
                    else:
                        put(lx, ry, z, RBS("east"))
                        put(rx, ry, z, RBS("west"))
                        if z in (z0, z1):
                            for gx in range(lx + 1, rx):
                                put(gx, ry, z, TB if gx != (x0 + x1) // 2 else GL)
                        else:
                            for gx in range(lx + 1, rx):
                                put(gx, ry, z, AIR)
            else:
                lz, rz = z0 - 1 + k, z1 + 1 - k
                if lz > rz:
                    break
                for x in range(x0 - 1, x1 + 2):
                    if lz == rz:
                        put(x, ry, lz, TG)
                    else:
                        put(x, ry, lz, RBS("south"))
                        put(x, ry, rz, RBS("north"))
                        if x in (x0, x1):
                            for gz in range(lz + 1, rz):
                                put(x, ry, gz, TB)
                        else:
                            for gz in range(lz + 1, rz):
                                put(x, ry, gz, AIR)

    # Крылья дворца (сначала боковые, затем главный неф, затем высокий донжон!)
    build_gable_wing(CX - 19, CX - 9, CZ - 6, CZ + 6, 13, axis="x", roof_mat=RB)
    build_gable_wing(CX + 9, CX + 18, CZ - 8, CZ + 4, 14, axis="x", roof_mat=RB)
    build_gable_wing(CX - 10, CX + 10, CZ - 11, CZ + 11, 20, axis="z", roof_mat=RB)

    # Возвышающаяся Северная Главная Башня-Донжон (восьмигранно-ступенчатая до G + 56!)
    DON_TOP = G + 35
    for x in range(CX - 7, CX + 8):
        for z in range(CZ - 18, CZ - 3):
            dx, dz = abs(x - CX), abs(z - (CZ - 11))
            if dx + dz > 12:
                continue
            border = (dx == 7 or dz == 7 or dx + dz >= 11)
            for y in range(G + 1, DON_TOP + 1):
                if border:
                    if (dx == 0 or dz == 0) and (G + 14 <= y <= G + 21 or G + 25 <= y <= G + 31):
                        put(x, y, z, GL)
                    elif y in (G + 12, G + 23, DON_TOP - 1, DON_TOP):
                        put(x, y, z, TG if (y == DON_TOP and (dx + dz) % 3 == 0) else ASH)
                    else:
                        put(x, y, z, wall_stone(x, y, z))
                elif y > G + 21:
                    put(x, y, z, DP if y == G + 24 else AIR)
    # Четыре угловых пинакля донжона + высокий королевский сине-золотой шпиль (до G + 56)
    for px2, pz2 in ((CX - 6, CZ - 17), (CX + 6, CZ - 17), (CX - 6, CZ - 5), (CX + 6, CZ - 5)):
        for py in range(DON_TOP + 1, DON_TOP + 6):
            put(px2, py, pz2, ASHP if py < DON_TOP + 4 else TG)
    for sy in range(18):
        y = DON_TOP + 1 + sy
        rad = max(0.0, 6.8 * (1.0 - sy / 17.5))
        for dx in range(-7, 8):
            for dz in range(-7, 8):
                d = math.hypot(dx, dz)
                if d <= rad + 0.35:
                    if d >= rad - 0.95 or sy >= 14:
                        put(CX + dx, y, CZ - 11 + dz, TG if (dx == 0 or dz == 0) and sy >= 11 else RB)
                    else:
                        put(CX + dx, y, CZ - 11 + dz, AIR)
    for fy in range(DON_TOP + 19, DON_TOP + 24):
        put(CX, fy, CZ - 11, TG)

    # Летающие аркбутаны (контрфорсы) по бокам главного нефа
    for bz in (CZ - 7, CZ - 1, CZ + 5):
        for side in (-1, 1):
            bx_wall = CX + side * 10
            bx_pier = CX + side * 14
            if abs(bz) > 6 or side > 0:
                for y in range(G + 1, G + 14):
                    put(bx_pier, y, bz, ASHP)
                put(bx_pier, G + 14, bz, TG)
                for step in range(1, 4):
                    put(bx_pier - side * step, G + 12 + step, bz, ASH)

    # Интерьер Тронного Зала: колоннада, ковровая дорожка, люстры, королевский помост и проходы в крылья!
    fill(CX - 3, G + 1, CZ + 11, CX + 3, G + 7, CZ + 11, AIR)  # парадный портал
    fill(CX - 2, G + 1, CZ - 18, CX + 2, G + 6, CZ - 18, AIR)  # северный выход в задний двор
    fill(CX - 10, G + 1, CZ - 2, CX - 9, G + 5, CZ + 2, AIR)   # проход в западное крыло
    fill(CX + 9, G + 1, CZ - 3, CX + 10, G + 5, CZ + 1, AIR)   # проход в восточную капеллу
    for kz in range(CZ - 8, CZ + 9, 4):
        for side in (-1, 1):
            for y in range(G + 1, G + 16):
                put(CX + side * 6, y, kz, GCOL)
            put(CX + side * 6, G + 10, kz - side * 0, LANT_H)
    # Красная с золотом ковровая дорожка к тронному помосту
    for z in range(CZ - 13, CZ + 11):
        for dx in range(-2, 3):
            put(CX + dx, G + 1, z, GOLD_CARP if abs(dx) == 2 else CARPET)
    # Тронный помост на севере зала
    fill(CX - 4, G + 1, CZ - 15, CX + 4, G + 1, CZ - 12, ASH)
    fill(CX - 2, G + 2, CZ - 15, CX + 2, G + 2, CZ - 13, TG)
    put(CX, G + 3, CZ - 14, TGR)
    put(CX, G + 4, CZ - 14, TG)
    # Большие подвесные люстры под сводом зала
    for cz_l in (CZ - 6, CZ + 2):
        for y in range(G + 14, G + 19):
            put(CX, y, cz_l, BARS)
        for dx in range(-1, 2):
            for dz in range(-1, 2):
                if abs(dx) + abs(dz) <= 1:
                    put(CX + dx, G + 13, cz_l + dz, TG if (dx == 0 and dz == 0) else SEA_L)

    # Южный парадный портик перед входом в главный зал + парные фонтаны и сады в южном дворе!
    for side in (-1, 1):
        for y in range(G + 1, G + 9):
            put(CX + side * 4, y, CZ + 14, GCOL)
    fill(CX - 5, G + 9, CZ + 11, CX + 5, G + 9, CZ + 15, ASH)
    for k in range(5):
        if CX - 5 + k <= CX + 5 - k:
            fill(CX - 5 + k, G + 10 + k, CZ + 11, CX + 5 - k, G + 10 + k, CZ + 15, TG if CX - 5 + k == CX + 5 - k else RB)

    # Два мраморных фонтана и подстриженные белые деревца в парадном южном и северном дворах
    for fx, fz in ((CX - 11, CZ + 16), (CX + 11, CZ + 16), (CX - 11, CZ - 22), (CX + 11, CZ - 22)):
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                if abs(dx) == 2 or abs(dz) == 2:
                    put(fx + dx, G + 1, fz + dz, TBSl("bottom"))
                else:
                    put(fx + dx, G, fz + dz, TM)
                    put(fx + dx, G + 1, fz + dz, WATER)
        put(fx, G + 1, fz, ASHP)
        put(fx, G + 2, fz, ASHP)
        put(fx, G + 3, fz, SEA_L)

    # Внутренние замковые постройки в заднем и боковых дворах (чтобы двор замка тоже был обжитым!)
    for tx, tz in ((CX - 17, CZ + 14), (CX + 17, CZ + 14), (CX - 14, CZ - 15), (CX + 14, CZ - 15)):
        put(tx, G, tz, GRASS)
        for y in range(G + 1, G + 4):
            put(tx, y, tz, LOG_S)
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                for dy in (4, 5):
                    if abs(dx) + abs(dz) <= (3 if dy == 4 else 2):
                        put(tx + dx, G + dy, tz + dz, LEAVES)

    os.makedirs(OUT_CITY, exist_ok=True)
    n, pal = write_template(os.path.join(OUT_CITY, "city_castle.nbt"), (W, H, D), B, {})
    # Также записываем копию в temple_great.nbt для полной обратной совместимости любых ссылок
    write_template(os.path.join(OUT_CITY, "temple_great.nbt"), (W, H, D), B, {})
    print(f"city_castle.nbt: {W}x{H}x{D}, blocks={n}, palette={pal}")
    return {"W": W, "H": H, "D": D}


# ==============================================================================
# 2. НОВЫЕ ГОРОДСКИЕ ЗДАНИЯ (guildhall, manor, bakery, guardhouse)
# ==============================================================================
def build_extra_city_buildings():
    # Загружаем класс Tpl из gen_city.py без запуска его генератора
    src = open(os.path.join(HERE, "gen_city.py"), encoding="utf-8").read()
    cut = src.index('TPL = {}')
    ns = {"__file__": os.path.join(HERE, "gen_city.py")}
    src_patched = src[:cut].replace('MOD = "/home/user/mods/riftworld"', f'MOD = "{ROOT}/mod"')
    exec(compile(src_patched, "gen_city_tpl", "exec"), ns)
    Tpl = ns["Tpl"]
    S_fn = ns["S"]
    AIR_s = ns["AIR"]

    extra_meta = {}

    def finish_tpl(tp, door_x):
        tp.outside()
        tp.eave_lanterns()
        ns["fix_fences"](tp)
        tp.nlights = tp.lights()
        H = max(p[1] for p in tp.B) + 1
        n, _ = write_template(os.path.join(OUT_CITY, f"{tp.name}.nbt"), (tp.W, H, tp.D), tp.B, tp.NBT)
        return ({"name": tp.name, "W": tp.W, "H": H, "D": tp.D, "door_x": door_x, "blocks": n, "lights": tp.nlights}, door_x)

    # 1) Гильдейский дом (guildhall: 2 этажа, камень + фахверк, синяя крыша с башенкой-звонницей)
    t = Tpl("guildhall", 11, 9, floors=2)
    t.shell(["brick", "timber"], floor="sizo_planks", corner="ashlar_pillar")
    t.door(6)
    for x in (3, 9):
        t.window(x, 2, 9)
        t.window(x, 7, 9)
        t.window(x, 2, 1)
        t.window(x, 7, 1)
    for z in (3, 6):
        t.window(1, 2, z)
        t.window(11, 2, z)
        t.window(1, 7, z)
        t.window(11, 7, z)
    t.stairs_up(10, 2, 1, rail_dx=-1)
    t.model("fireplace", 2, 1, 5, "east")
    t.model("table_wood", 5, 1, 4, "south")
    t.model("chair_wood", 5, 1, 5, "north")
    t.model("chair_wood", 6, 1, 5, "north")
    t.model("bookshelf_e", 4, 6, 2, "south")
    t.model("bed_wood", 2, 6, 4, "south")
    t.roof("roof_blue", "x", gable="timber_frame_post")
    t.chimney(1, 5, 5)
    ry = t.ridge_y
    for dy in range(1, 4):
        for gx in (5, 6):
            t.put(gx, ry + dy, 5, S_fn("ashlar_pillar", axis="y"), force=True)
    t.put(5, ry + 4, 5, S_fn("temple_gold"), force=True)
    t.put(6, ry + 4, 5, S_fn("temple_gold"), force=True)
    extra_meta["guildhall"] = finish_tpl(t, 6)

    # 2) Двухэтажный городской особняк (manor: камень + штукатурка, красная черепица)
    m = Tpl("manor", 10, 8, floors=2)
    m.shell(["stone", "plaster"], floor="ambra_planks", corner="sizo_log")
    m.door(5)
    for x in (3, 8):
        m.window(x, 2, 8)
        m.window(x, 7, 8)
        m.window(x, 7, 1)
    for z in (3, 6):
        m.window(1, 2, z)
        m.window(10, 2, z)
        m.window(1, 7, z)
        m.window(10, 7, z)
    m.stairs_up(9, 2, 1, rail_dx=-1)
    m.model("fireplace", 2, 1, 4, "east")
    m.model("table_wood", 5, 1, 4, "south")
    m.model("chair_wood", 5, 1, 5, "north")
    m.model("bed_wood", 3, 6, 3, "south")
    m.model("wardrobe", 7, 6, 2, "south")
    m.roof("roof_red", "x", gable="timber_frame_post")
    m.chimney(1, 4, 5)
    extra_meta["manor"] = finish_tpl(m, 5)

    # 3) Городская пекарня с высокой кирпичной печной трубой (bakery)
    b = Tpl("bakery", 9, 7, floors=1)
    b.shell(["brick"], floor="ambra_planks", corner="sizo_log")
    b.door(5)
    b.window(3, 2, 7)
    b.window(7, 2, 7)
    b.window(1, 2, 4)
    b.window(9, 2, 4)
    b.model("fireplace", 2, 1, 3, "east")
    b.model("table_wood", 5, 1, 3, "south")
    b.model("sacks", 7, 1, 2, "south")
    b.roof("roof_red", "x", gable="timber_frame_post")
    b.chimney(1, 3, 6)
    extra_meta["bakery"] = finish_tpl(b, 5)

    # 4) Караульня замковой стражи с синей крышей (guardhouse)
    g = Tpl("guardhouse", 8, 7, floors=2)
    g.shell(["stone", "brick"], floor="sizo_planks", corner="ashlar_pillar")
    g.door(4)
    for x in (2, 6):
        g.window(x, 2, 7)
        g.window(x, 7, 7)
    g.window(1, 7, 4)
    g.window(8, 7, 4)
    g.stairs_up(7, 2, 1, rail_dx=-1)
    g.model("table_wood", 4, 1, 3, "south")
    g.model("bed_wood", 2, 6, 3, "south")
    g.roof("roof_blue", "z", gable="elisar_bricks")
    extra_meta["guardhouse"] = finish_tpl(g, 4)

    for k, (meta, dx) in extra_meta.items():
        meta["door_x"] = dx
        print(f"extra tpl {k}: {meta['W']}x{meta['D']}x{meta['H']} blocks={meta['blocks']}")
    return {k: v[0] for k, v in extra_meta.items()}


# ==============================================================================
# 3. ОБНОВЛЕНИЕ plan.json: ЗАМОК В ЦЕНТРЕ + ПЛОТНАЯ ЗАСТРОЙКА ЗА ЗАМКОМ И ВОКРУГ!
# ==============================================================================
def update_city_plan(castle_meta, extra_meta):
    plan_path = os.path.join(OUT_DATA, "city/plan.json")
    plan = json.load(open(plan_path, encoding="utf-8"))

    # Увеличиваем радиус главного острова с 76.5 до 86.0 (в точном соответствии с точками мостов ~82..88!),
    # чтобы за замком и у мостов было полноценное плато под северный посад без обрывов под дорогами!
    i0 = plan["islands"][0]
    i0["R"] = 86.0
    p1, p2, p3 = i0["p"]
    base0 = i0["base"]

    def shapeR(th):
        return i0["R"] * (1 + 0.07 * math.sin(3 * th + p1) + 0.045 * math.sin(5 * th + p2) + 0.025 * math.sin(11 * th + p3))

    def t_main(x, z):
        return 1.0 - math.hypot(x, z) / shapeR(math.atan2(z, x))

    def raw_main(x, z):
        h = base0 + 1.6 * math.sin(x * 0.043 + p1) * math.cos(z * 0.037 + p2) + 0.9 * math.sin((x - z) * 0.071 + p3)
        tm = t_main(x, z)
        if tm < 0.12:
            h -= ((1.0 - max(tm, 0.0) / 0.12) ** 2) * 4.0
        return h

    # Регистрируем новые шаблоны в plan["templates"]
    plan["templates"]["city_castle"] = [castle_meta["W"], castle_meta["H"], castle_meta["D"]]
    plan["templates"]["temple_great"] = [castle_meta["W"], castle_meta["H"], castle_meta["D"]]
    for k, m in extra_meta.items():
        plan["templates"][k] = [m["W"], m["H"], m["D"]]

    # Заменяем центральный plot (temple_great -> city_castle, 69x73 в центре острова (-34, -36), h=104, yo=-12)
    # И расчищаем старые разрозненные дома/дороги/декор строго в северном секторе главного острова (z < -35),
    # чтобы выстроить там полноценный плотный Северный Посад с площадью, гильдией, особняками, пекарнями и улочками!
    castle_plot = {
        "t": "city_castle",
        "x0": -34,
        "z0": -36,
        "w": castle_meta["W"],
        "d": castle_meta["D"],
        "f": "south",
        "h": 104,
        "yo": -12,
        "m": 3,
    }
    new_plots = [castle_plot]
    for p in plan["plots"]:
        if p["t"] == "temple_great":
            continue
        px0, pz0, pw, pd = p["x0"], p["z0"], p["w"], p["d"]
        cxp, czp = px0 + pw / 2.0, pz0 + pd / 2.0
        if math.hypot(cxp, czp) < 92.0:
            # На главном острове убираем постройки, пересекающие замок или северный сектор за замком (czp < -35)
            if not (px0 + pw < -35 or px0 > 35 or pz0 + pd < -37 or pz0 > 37):
                continue
            if czp < -35.0:
                continue
        new_plots.append(p)
    plan["plots"] = new_plots

    # Убираем старые тупиковые дорожки И старую северную перемычку кольца z=-40 строго за замком,
    # чтобы дома Северного Посада подходили вплотную к северным башням и стенам замка без пустой полосы!
    cleaned_roads = []
    for r in plan["roads"]:
        ax, az = r["a"]
        bx, bz = r["b"]
        if math.hypot(ax, az) < 88 and math.hypot(bx, bz) < 88:
            if r["w"] <= 2 and min(az, bz) < -36:
                continue
            if -33 < ax < 33 and -35 < az < 35 and -33 < bx < 33 and -35 < bz < 35:
                continue
            if az < -37 and bz < -37 and max(abs(ax), abs(bx)) < 40:
                continue
        cleaned_roads.append(r)
    plan["roads"] = cleaned_roads

    # Добавляем Северную Замковую Площадь прямо за северной потерной замка (0, -52)
    # и продуманную сеть улиц, оставляющую глубокие строительные кварталы с обеих сторон!
    NORTH_PLAZA = (0.0, -52.0)
    EAST_PLAZA = (57.0, -4.0)
    SOUTH_EAST_PLAZA = (35.0, 52.0)
    plan["plazas"].extend([
        {"x": NORTH_PLAZA[0], "z": NORTH_PLAZA[1], "r": 6.5, "k": 0},
        {"x": EAST_PLAZA[0], "z": EAST_PLAZA[1], "r": 5.5, "k": 0},
        {"x": SOUTH_EAST_PLAZA[0], "z": SOUTH_EAST_PLAZA[1], "r": 6.0, "k": 0},
    ])

    extra_roads = [
        # Северный проспект от задних ворот замка через площадь к северной смотровой террасе
        ([0.0, -36.5], [0.0, -52.0], 4, 0),
        ([0.0, -52.0], [0.0, -70.0], 3, 0),
        # Главная Северная Дуга Посада (между двумя рядами плотной застройки: внутренним у стен замка и внешним!)
        ([-47.0, -68.0], [-36.0, -56.0], 4, 0),
        ([-36.0, -56.0], [-18.0, -53.0], 4, 0),
        ([-18.0, -53.0], [0.0, -52.0], 4, 0),
        ([0.0, -52.0], [20.0, -52.0], 4, 0),
        ([20.0, -52.0], [42.0, -47.0], 4, 0),
        ([42.0, -47.0], [62.0, -49.0], 4, 0),
        # Боковые связки с западным и восточным кольцом вокруг замка
        ([-37.4, -26.0], [-36.0, -56.0], 3, 0),
        ([37.4, -26.0], [42.0, -47.0], 3, 0),
        # Восточные, западные и юго-восточные переулки для плотной застройки флангов
        ([37.4, -4.0], [57.0, -4.0], 4, 0),
        ([57.0, -4.0], [64.0, 18.0], 3, 1),
        ([26.2, 37.9], [35.0, 52.0], 4, 0),
        ([35.0, 52.0], [54.0, 44.0], 3, 1),
        ([35.0, 52.0], [16.0, 66.0], 3, 1),
        ([-28.9, 50.1], [-10.0, 67.0], 3, 1),
    ]
    for a, b, w, k in extra_roads:
        plan["roads"].append({"a": a, "b": b, "w": w, "k": k})

    # Строим карту занятости главного острова, чтобы плотно поставить здания за замком и вокруг него!
    N0 = 120
    SZ = 2 * N0
    FREE, ROAD, PLOT, PROP, KEEP = 0, 1, 2, 3, 4
    occ = bytearray(SZ * SZ)

    def oget(x, z):
        ix, iz = int(math.floor(x)) + N0, int(math.floor(z)) + N0
        if 0 <= ix < SZ and 0 <= iz < SZ:
            return occ[iz * SZ + ix]
        return KEEP

    def oset(x, z, v):
        ix, iz = int(math.floor(x)) + N0, int(math.floor(z)) + N0
        if 0 <= ix < SZ and 0 <= iz < SZ:
            occ[iz * SZ + ix] = v

    def seg_dist(px, pz, ax, az, bx, bz):
        vx, vz = bx - ax, bz - az
        L2 = vx * vx + vz * vz
        t = 0.0 if L2 == 0 else max(0.0, min(1.0, ((px - ax) * vx + (pz - az) * vz) / L2))
        return math.hypot(px - ax - vx * t, pz - az - vz * t)

    # Помечаем дороги, площади, мосты, родники, существующие дома и бутафорию
    for r in plan["roads"]:
        ax, az = r["a"]
        bx, bz = r["b"]
        if math.hypot(ax, az) > 105 and math.hypot(bx, bz) > 105:
            continue
        w = r["w"]
        for x in range(int(min(ax, bx) - w - 1), int(max(ax, bx) + w + 2)):
            for z in range(int(min(az, bz) - w - 1), int(max(az, bz) + w + 2)):
                if seg_dist(x + 0.5, z + 0.5, ax, az, bx, bz) <= w / 2.0 + 0.6:
                    oset(x, z, ROAD)

    for pl in plan["plazas"]:
        if math.hypot(pl["x"], pl["z"]) > 105:
            continue
        for x in range(int(pl["x"] - pl["r"] - 1), int(pl["x"] + pl["r"] + 2)):
            for z in range(int(pl["z"] - pl["r"] - 1), int(pl["z"] + pl["r"] + 2)):
                if math.hypot(x + 0.5 - pl["x"], z + 0.5 - pl["z"]) <= pl["r"] + 0.5:
                    oset(x, z, ROAD)

    for b in plan["bridges"]:
        ax, az = b["a"]
        bx, bz = b["b"]
        L = max(1, int(math.hypot(bx - ax, bz - az)))
        for k in range(L + 1):
            x, z = ax + (bx - ax) * k / L, az + (bz - az) * k / L
            for dx in range(-4, 5):
                for dz in range(-4, 5):
                    oset(x + dx, z + dz, KEEP)

    for sp in plan["springs"]:
        sx, sz = sp["s"]
        if math.hypot(sx, sz) < 105:
            for dx in range(-4, 5):
                for dz in range(-4, 5):
                    oset(sx + dx, sz + dz, KEEP)

    for p in plan["plots"]:
        if math.hypot(p["x0"], p["z0"]) > 110:
            continue
        pad = 2 if p["t"] == "city_castle" else 1
        for x in range(int(math.floor(p["x0"])) - pad, int(math.ceil(p["x0"] + p["w"])) + pad):
            for z in range(int(math.floor(p["z0"])) - pad, int(math.ceil(p["z0"] + p["d"])) + pad):
                oset(x, z, PLOT)

    # Удаляем старые деревья главного острова, попавшие под замок или новые дороги
    kept_trees = []
    for tr in plan["trees"]:
        tx, tz = tr[0], tr[1]
        if math.hypot(tx, tz) < 98 and oget(tx, tz) in (ROAD, PLOT, KEEP):
            continue
        kept_trees.append(tr)
    plan["trees"] = kept_trees

    # Удаляем старые props главного острова, попавшие на замок, северную перестройку или новые улицы
    kept_props = []
    for pr in plan["props"]:
        px, pz = pr["x"], pr["z"]
        if math.hypot(px, pz) < 92:
            if (-36 <= px <= 36 and -38 <= pz <= 38) or pz < -35 or oget(px, pz) in (ROAD, PLOT, KEEP):
                continue
        kept_props.append(pr)
    plan["props"] = kept_props
    for pr in plan["props"]:
        if math.hypot(pr["x"], pr["z"]) < 98:
            ext = 7 if pr["k"] == "wheat_field" else 1
            for dx in range(-1, ext + 1):
                for dz in range(-1, ext + 1):
                    if oget(pr["x"] + dx, pr["z"] + dz) == FREE:
                        oset(pr["x"] + dx, pr["z"] + dz, PROP)

    # Размеры и двери всех доступных городских шаблонов
    DOOR_X = {
        "guildhall": 6, "manor": 6, "bakery": 5, "guardhouse": 5,
        "tavern": 7, "shop": 5, "shop2": 5, "forge": 5, "police": 6,
        "house_a": 4, "house_b": 5, "house_c": 5, "house_d": 4, "house_e": 5,
        "house_f": 3, "house_g": 3, "house_h": 5, "house_i": 4,
    }

    def tpl_dims(name, f):
        W0, _, D0 = plan["templates"][name]
        return (W0, D0) if f in ("south", "north") else (D0, W0)

    def rect_ok(x0, z0, w, d, margin=1):
        for x in range(x0 - margin, x0 + w + margin):
            for z in range(z0 - margin, z0 + d + margin):
                if oget(x, z) != FREE:
                    return False
        for x in range(x0, x0 + w, 2):
            for z in range(z0, z0 + d, 2):
                if t_main(x + 0.5, z + 0.5) < 0.075:
                    return False
        for cx2, cz2 in ((x0, z0), (x0 + w - 1, z0), (x0, z0 + d - 1), (x0 + w - 1, z0 + d - 1)):
            if t_main(cx2 + 0.5, cz2 + 0.5) < 0.075:
                return False
        hs = [math.floor(raw_main(x + 0.5, z + 0.5)) for x in range(x0, x0 + w, 2) for z in range(z0, z0 + d, 2)]
        return max(hs) - min(hs) <= 5

    rng = random.Random(20261101)
    main_roads = [r for r in plan["roads"] if math.hypot(r["a"][0], r["a"][1]) < 92 and r["w"] >= 3]

    def try_place_on_main(name, target_zone=None, tries=600, margin=1):
        best = None
        for _ in range(tries):
            r = rng.choice(main_roads)
            ax, az = r["a"]
            bx, bz = r["b"]
            u = rng.uniform(0.08, 0.92)
            px, pz = ax + (bx - ax) * u, az + (bz - az) * u
            if target_zone == "north" and pz > -34:
                continue
            if target_zone == "flank" and pz <= -34:
                continue
            vx, vz = bx - ax, bz - az
            ln = math.hypot(vx, vz) or 1.0
            side = rng.choice((-1, 1))
            nx, nz = -vz / ln * side, vx / ln * side
            fx, fz = -nx, -nz
            f = ("east" if fx > 0 else "west") if abs(fx) > abs(fz) else ("south" if fz > 0 else "north")
            w, d = tpl_dims(name, f)
            depth = d if f in ("south", "north") else w
            second_row = rng.random() < 0.32
            off = r["w"] / 2.0 + 1.5 + depth / 2.0 + (rng.uniform(8.5, 14.0) if second_row else rng.uniform(0.0, 2.2))
            cx, cz = px + nx * off, pz + nz * off
            x0, z0 = int(round(cx - w / 2.0)), int(round(cz - d / 2.0))
            if not rect_ok(x0, z0, w, d, margin=margin):
                continue
            hs = sorted(math.floor(raw_main(x + 0.5, z + 0.5)) for x in range(x0, x0 + w) for z in range(z0, z0 + d))
            h = hs[len(hs) // 2]
            best = (x0, z0, w, d, f, h)
            break
        if best is None:
            return None
        x0, z0, w, d, f, h = best
        p = {"t": name, "x0": x0, "z0": z0, "w": w, "d": d, "f": f, "h": h, "yo": 0, "m": 3}
        plan["plots"].append(p)
        for x in range(x0 - 1, x0 + w + 1):
            for z in range(z0 - 1, z0 + d + 1):
                oset(x, z, PLOT)
        # Дорожка от двери к улице
        W0, _, D0 = plan["templates"][name]
        tx, tz = DOOR_X.get(name, W0 // 2), D0 - 1
        if f == "south":
            dx, dz = x0 + tx, z0 + tz
        elif f == "north":
            dx, dz = x0 + W0 - 1 - tx, z0 + D0 - 1 - tz
        elif f == "west":
            dx, dz = x0 + D0 - 1 - tz, z0 + tx
        else:
            dx, dz = x0 + tz, z0 + W0 - 1 - tx
        step = {"south": (0, 1), "north": (0, -1), "east": (1, 0), "west": (-1, 0)}[f]
        sx, sz = dx + step[0], dz + step[1]
        ex, ez = sx, sz
        for k in range(1, 20):
            ex, ez = sx + step[0] * k, sz + step[1] * k
            if oget(ex, ez) == ROAD:
                break
        plan["roads"].append({"a": [sx + 0.5, sz + 0.5], "b": [ex + 0.5, ez + 0.5], "w": 2, "k": 1})
        return p

    # Целенаправленно застраиваем Северный Посад ЗА ЗАМКОМ (z < -36) новыми крупными и малыми зданиями!
    north_priority = [
        "guildhall", "manor", "bakery", "guardhouse", "tavern", "forge", "manor", "bakery",
        "shop", "shop2", "house_b", "house_c", "house_e", "house_i", "house_d", "house_h",
        "house_a", "house_f", "house_g", "house_b", "house_e", "house_c", "manor", "bakery",
    ]
    placed_north = 0
    for nm in north_priority:
        if try_place_on_main(nm, target_zone="north", tries=700, margin=1):
            placed_north += 1

    # И застраиваем восточные, западные и юго-восточные кварталы вокруг замка!
    flank_priority = [
        "guildhall", "manor", "bakery", "guardhouse", "manor", "shop", "shop2",
        "house_b", "house_c", "house_e", "house_i", "house_d", "house_h", "house_a",
        "house_g", "house_f", "house_e", "house_c", "house_b", "bakery", "guardhouse",
        "house_i", "house_d", "house_a", "house_h", "house_f",
    ]
    placed_flank = 0
    for nm in flank_priority:
        if try_place_on_main(nm, target_zone="flank", tries=700, margin=1):
            placed_flank += 1

    # Дополнительное плотное сеточное заполнение ВСЕХ свободных карманов вокруг и позади замка!
    grid_pool = [
        "guildhall", "manor", "bakery", "guardhouse", "house_b", "house_c",
        "house_e", "house_i", "house_d", "house_h", "house_a", "house_g", "house_f",
    ]
    extra_fill = 0
    candidates = []
    for gz in range(-78, 72, 3):
        for gx in range(-76, 72, 3):
            # Приоритет точкам вплотную к замку и в северном посаде за замком
            dist_castle = max(0.0, abs(gx) - 35.0, abs(gz) - 37.0)
            north_bonus = -18.0 if gz < -36 else 0.0
            candidates.append((dist_castle + north_bonus + rng.random() * 2.0, gx, gz))
    candidates.sort()

    for _, gx, gz in candidates:
        if t_main(gx + 4.5, gz + 4.5) < 0.085:
            continue
        # Выбираем ориентацию двери к ближайшей дороге
        best_f = None
        best_rd = 999
        for f, sx, sz in (("north", 0, -1), ("south", 0, 1), ("west", -1, 0), ("east", 1, 0)):
            for step in range(1, 16):
                if oget(gx + 4 + sx * step, gz + 4 + sz * step) == ROAD:
                    if step < best_rd:
                        best_rd = step
                        best_f = f
                    break
        if best_f is None:
            continue
        rng.shuffle(grid_pool)
        for nm in grid_pool:
            w, d = tpl_dims(nm, best_f)
            if not rect_ok(gx, gz, w, d, margin=1):
                continue
            hs = sorted(math.floor(raw_main(x + 0.5, z + 0.5)) for x in range(gx, gx + w) for z in range(gz, gz + d))
            h = hs[len(hs) // 2]
            plan["plots"].append({"t": nm, "x0": gx, "z0": gz, "w": w, "d": d, "f": best_f, "h": h, "yo": 0, "m": 3})
            for x in range(gx - 1, gx + w + 1):
                for z in range(gz - 1, gz + d + 1):
                    oset(x, z, PLOT)
            W0, _, D0 = plan["templates"][nm]
            tx, tz = DOOR_X.get(nm, W0 // 2), D0 - 1
            if best_f == "south":
                dx, dz = gx + tx, gz + tz
            elif best_f == "north":
                dx, dz = gx + W0 - 1 - tx, gz + D0 - 1 - tz
            elif best_f == "west":
                dx, dz = gx + D0 - 1 - tz, gz + tx
            else:
                dx, dz = gx + tz, gz + W0 - 1 - tx
            st = {"south": (0, 1), "north": (0, -1), "east": (1, 0), "west": (-1, 0)}[best_f]
            sx2, sz2 = dx + st[0], dz + st[1]
            ex2, ez2 = sx2, sz2
            for k in range(1, 18):
                ex2, ez2 = sx2 + st[0] * k, sz2 + st[1] * k
                if oget(ex2, ez2) == ROAD:
                    break
            plan["roads"].append({"a": [sx2 + 0.5, sz2 + 0.5], "b": [ex2 + 0.5, ez2 + 0.5], "w": 2, "k": 1})
            extra_fill += 1
            break

    # Оживляем Северную Замковую Площадь (0, -54), Восточную и Юго-Восточную площади:
    # колодец, торговые палатки, скамьи, ящики и уличные фонари!
    def add_city_prop(kind, x, z, f="south", lit=None):
        ix, iz = int(round(x)), int(round(z))
        if t_main(ix + 0.5, iz + 0.5) < 0.08 or oget(ix, iz) in (PLOT, PROP):
            return False
        plan["props"].append({"k": kind, "x": ix, "z": iz, "f": f, "lit": (kind in LIT) if lit is None else lit})
        oset(ix, iz, PROP)
        return True

    add_city_prop("well", NORTH_PLAZA[0], NORTH_PLAZA[1], "south")
    for ang, kind in ((35, "market_stall"), (145, "market_stall_fish"), (215, "market_stall"), (325, "market_stall")):
        a = math.radians(ang)
        add_city_prop(kind, NORTH_PLAZA[0] + math.cos(a) * 6.8, NORTH_PLAZA[1] + math.sin(a) * 6.8, "south")
    for ang in (0, 90, 180, 270):
        a = math.radians(ang)
        add_city_prop("lantern_post", NORTH_PLAZA[0] + math.cos(a) * 8.5, NORTH_PLAZA[1] + math.sin(a) * 8.5, "south", True)
        add_city_prop("bench", NORTH_PLAZA[0] + math.cos(a + 0.4) * 4.2, NORTH_PLAZA[1] + math.sin(a + 0.4) * 4.2, "south")

    add_city_prop("well", EAST_PLAZA[0], EAST_PLAZA[1], "south")
    add_city_prop("market_stall", EAST_PLAZA[0] + 5, EAST_PLAZA[1] - 3, "west")
    add_city_prop("lantern_post", EAST_PLAZA[0] - 5, EAST_PLAZA[1] + 4, "east", True)

    add_city_prop("fire_pit", SOUTH_EAST_PLAZA[0], SOUTH_EAST_PLAZA[1], "south", True)
    add_city_prop("bench", SOUTH_EAST_PLAZA[0] + 3, SOUTH_EAST_PLAZA[1], "west")
    add_city_prop("bench", SOUTH_EAST_PLAZA[0] - 3, SOUTH_EAST_PLAZA[1], "east")
    add_city_prop("lantern_post", SOUTH_EAST_PLAZA[0], SOUTH_EAST_PLAZA[1] + 5, "north", True)

    # Уличные фонари вдоль новых бульваров за замком и деревьев в уютных двориках между домами
    for r in extra_roads:
        ax, az = r[0]
        bx, bz = r[1]
        L = math.hypot(bx - ax, bz - az)
        if L >= 12:
            mx, mz = (ax + bx) * 0.5, (az + bz) * 0.5
            nx, nz = -(bz - az) / L * 3.2, (bx - ax) / L * 3.2
            add_city_prop("lantern_post", mx + nx, mz + nz, "south", True)

    added_trees = 0
    for gx in range(-78, 79, 7):
        for gz in range(-78, 79, 7):
            tx, tz = gx + rng.randint(0, 4), gz + rng.randint(0, 4)
            if t_main(tx + 0.5, tz + 0.5) < 0.08:
                continue
            if all(oget(tx + dx, tz + dz) == FREE for dx in range(-2, 3) for dz in range(-2, 3)):
                if rng.random() < 0.42:
                    kind = 0 if rng.random() < 0.55 else 1
                    h = rng.randint(7, 11) if kind == 0 else rng.randint(5, 8)
                    plan["trees"].append([tx, tz, kind, h, rng.randint(0, 1 << 30)])
                    oset(tx, tz, PROP)
                    added_trees += 1

    json.dump(plan, open(plan_path, "w", encoding="utf-8"), separators=(",", ":"))
    main_plots_total = sum(1 for p in plan["plots"] if math.hypot(p["x0"], p["z0"]) < 95)
    print(
        f"Updated plan.json: placed_north={placed_north}, placed_flank={placed_flank}, "
        f"extra_fill={extra_fill}, total_main_plots={main_plots_total}, added_trees={added_trees}"
    )


def main():
    cm = build_city_castle()
    em = build_extra_city_buildings()
    update_city_plan(cm, em)


if __name__ == "__main__":
    main()
