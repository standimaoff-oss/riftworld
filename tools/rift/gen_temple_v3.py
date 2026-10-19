"""Rev 6 «Храм строго по фотке» v5: НИКАКОГО глаза и ничего над зданием. Квадратное белое
здание на каменной юбке; уходящие вниз лестницы СО ВСЕХ 4 СТОРОН (спереди пара); башни
РАЗНОЙ ВЫСОТЫ вразброс слева и справа от здания (4 отдельных звонницы + 4 причастных
углобашенки); навесы-беседки НА ОПОРАХ; стекло только СВЕТЛО-ГОЛУБОЕ; золотая дверь со
стеклянным швом, арочное окно над ней -> data/riftworld/structures/city/temple_great.nbt
"""
import json, os, re, sys, math

HERE = os.path.dirname(os.path.abspath(__file__))
src = open(os.path.join(HERE, "gen_castle.py"), encoding="utf-8").read()
exec(src[:src.index("# ================================================================== foundation / floors")])
rooms = open(os.path.join(HERE, "castle_rooms.py"), encoding="utf-8").read()
exec(rooms[:rooms.index("def tower_door")])

OUT = MOD + "/src/main/resources/data/riftworld/structures/city"
os.makedirs(OUT, exist_ok=True)
W, H, D = 77, 112, 84
G = 30

TB   = S("temple_bricks")
TG   = S("temple_gold")
TF   = S("temple_floor")
KS   = S("keep_stone")
KSC  = S("keep_stone_cracked")
GL   = S("minecraft:light_blue_stained_glass")          # стекло — ТОЛЬКО светло-голубое
TBSl = lambda t="bottom": S("temple_bricks_slab", type=t)
TBS  = lambda f, **kw: S("temple_bricks_stairs", facing=f, **kw)
GCOL = lambda a: S("temple_grand_column", axis=a)

CX = W // 2                                             # 36

# ------------------------------------------------------------------ 0) КАМЕННАЯ ЮБКА-ПОДНОЖИЕ под всей площадкой
for k in range(1, 9):
    band(13 - k // 3, 18 - k // 3, 61 + k // 3, 63 + k // 3, G - k, TB)

# ------------------------------------------------------------------ 1) ГЛАВНОЕ ЗДАНИЕ 40×38 h=34 (фасад на z=58)
walls(17, 21, 57, 59, G + 1, G + 34, t=2, mat=lambda x, y, z: TB)
fill(19, G, 23, 55, G, 57, TF)
band(16, 20, 58, 60, G, TB)                                 # цоколь
band(16, 20, 58, 60, G + 33, TB)                            # карниз
band(16, 20, 58, 60, G + 34, TB)
for (x, z) in ((16, 20), (58, 20), (16, 60), (58, 60)):     # золотые набалдачники
    put(x, G + 35, z, TG); put(x, G + 36, z, TG)

# боковые арочные ниши с голубым светом
for z in (27, 35, 43, 51):
    fill(17, G + 8, z, 17, G + 12, z + 2, GL)
    fill(57, G + 8, z, 57, G + 12, z + 2, GL)
    for dy in (-1, 5):
        for dz in (-1, 3):
            put(17, G + 8 + dy, z + dz, KSC); put(57, G + 8 + dy, z + dz, KSC)

# ------------------------------------------------------------------ 2) ФАСАД: БЕЗ ГЛАЗА. Дверь + высокое арочное окно над ней
# высокое арочное светло-голубое ОКНО над дверью (без ликов и глаз!)
fill(CX - 2, G + 12, 59, CX + 2, G + 19, 59, GL)
put(CX - 1, G + 20, 59, GL); put(CX + 1, G + 20, 59, GL); put(CX, G + 21, 59, GL)
for y in range(G + 11, G + 20):
    put(CX - 3, y, 59, KSC); put(CX + 3, y, 59, KSC)
fill(CX - 3, G + 11, 59, CX + 3, G + 11, 59, KSC)
put(CX - 2, G + 21, 59, KSC); put(CX + 2, G + 21, 59, KSC)

# фасадные арочные ниши по бокам от окна (как боковые, но на фасаде)
for x in (23, 47):
    fill(x, G + 9, 59, x + 2, G + 14, 59, GL)
    for dy in (-1, 6):
        for dx in (-1, 3):
            put(x + dx, G + 9 + dy, 59, KSC)

# портал: каменный косяк + арка + плита-порог
for y in range(G + 1, G + 8):
    put(CX - 3, y, 59, KSC); put(CX + 3, y, 59, KSC)
fill(CX - 3, G + 8, 59, CX + 3, G + 8, 59, KSC)
put(CX - 2, G + 9, 59, KSC); put(CX + 2, G + 9, 59, KSC); put(CX, G + 10, 59, KSC)
fill(CX - 9, G, 59, CX + 9, G, 61, TF)

# золотая дверь со СВЕТЛО-ГОЛУБЫМ стеклянным швом посредине
fill(CX - 2, G + 1, 59, CX + 2, G + 7, 59, TG)
fill(CX, G + 1, 59, CX, G + 7, 59, GL)

# ------------------------------------------------------------------ 3) ЧЕТЫРЕ ПРИЧАСТНЫЕ УГЛОБАШЕНКИ на самом здании (шатёр + золотая игла)
def turret(xa, za):
    xb, zbb = xa + 6, za + 6
    walls(xa, za, xb, zbb, G + 1, G + 40, t=1, mat=lambda x, y, z: TB)
    fill(xa + 1, G, za + 1, xb - 1, G, zbb - 1, TF)
    band(xa - 1, za - 1, xb + 1, zbb + 1, G + 20, TB)
    mx, mz = xa + 3, za + 3
    for y in (G + 8, G + 24):
        fill(mx, y, zbb, mx, y + 4, zbb, GL)
        fill(mx, y, za, mx, y + 4, za, GL)
    fill(xa, G + 10, mz, xa, G + 14, mz, GL)
    fill(xb, G + 10, mz, xb, G + 14, mz, GL)
    band(xa - 1, za - 1, xb + 1, zbb + 1, G + 40, TB)
    band(xa - 1, za - 1, xb + 1, zbb + 1, G + 41, TB)
    for k, off in enumerate((-1, 0, 1, 1, 2, 2, 3)):
        band(xa + off, za + off, xb - off, zbb - off, G + 42 + k, TB)
    fill(mx, G + 49, mz, mx, G + 53, mz, TG)
    fill(mx, G + 52, mz - 1, mx, G + 52, mz + 1, TG)
    fill(mx - 1, G + 52, mz, mx + 1, G + 52, mz, TG)

turret(14, 52); turret(54, 52); turret(14, 16); turret(54, 16)

# ------------------------------------------------------------------ 4) ОТДЕЛЬНЫЕ БАШНИ РАЗНОЙ ВЫСОТЫ ВРАЗБРОС слева и справа (звонницы: шахта + крылья-окна + купол + золотая игла)
def stand_tower(xa, za, shaft):
    xb, zbb = xa + 6, za + 6
    for k in range(1, 5):                                  # свой подножик-юбка
        band(xa - 2 + k // 2, za - 2 + k // 2, xb + 2 - k // 2, zbb + 2 - k // 2, G - k, TB)
    walls(xa, za, xb, zbb, G + 1, G + shaft, t=1, mat=lambda x, y, z: TB)
    fill(xa + 1, G, za + 1, xb - 1, G, zbb - 1, TF)
    band(xa - 1, za - 1, xb + 1, zbb + 1, G + shaft // 2, TB)
    band(xa - 1, za - 1, xb + 1, zbb + 1, G + shaft - 2, TB)
    mx, mz = xa + 3, za + 3
    fill(mx, G + 10, zbb, mx, G + 13, zbb, GL)
    fill(mx, G + shaft - 12, zbb, mx, G + shaft - 9, zbb, GL)
    walls(xa - 1, za - 1, xb + 1, zbb + 1, G + shaft, G + shaft + 10, t=1, mat=lambda x, y, z: TB)   # звонница шире шахты
    for k in (1, 5):                                       # парные арочные окна 4 граней
        fill(xa - 1 + k, G + shaft + 2, za - 1, xa + k, G + shaft + 7, za - 1, GL)
        fill(xa - 1 + k, G + shaft + 2, zbb + 1, xa + k, G + shaft + 7, zbb + 1, GL)
        fill(xa - 1, G + shaft + 2, za - 1 + k, xa - 1, G + shaft + 7, za + k, GL)
        fill(xb + 1, G + shaft + 2, za - 1 + k, xb + 1, G + shaft + 7, za + k, GL)
    band(xa - 2, za - 2, xb + 2, zbb + 2, G + shaft + 10, TB)
    for k, off in enumerate((-1, 0, 0, 1, 1, 2, 2, 3)):    # ребристый купол
        band(xa + 1 + off, za + 1 + off, xb - 1 - off, zbb - 1 - off, G + shaft + 11 + k, TB)
    fill(mx, G + shaft + 19, mz, mx, G + shaft + 25, mz, TG)  # золотая игла
    fill(mx, G + shaft + 23, mz - 1, mx, G + shaft + 23, mz + 1, TG)
    fill(mx - 1, G + shaft + 23, mz, mx + 1, G + shaft + 23, mz, TG)

stand_tower(4, 8, 52)      # дальняя левая — САМАЯ ВЫСОКАЯ (top G+77)
stand_tower(62, 5, 40)     # дальняя правая (top G+65)
stand_tower(2, 44, 30)     # ближняя левая (top G+55)
stand_tower(64, 40, 22)    # ближняя правая низкая (top G+47)

# ------------------------------------------------------------------ 5) НАВЕСЫ-БЕСЕДКИ НА ОПОРАХ (4 стойки + белый шатёр)
def canopy(xa, za):
    for (x, z) in ((xa, za), (xa + 6, za), (xa, za + 6), (xa + 6, za + 6)):
        fill(x, G, z, x, G, z, TF)
        fill(x, G + 1, z, x, G + 5, z, GCOL("y"))          # стойки-опоры
        put(x, G + 6, z, TG)
    fill(xa, G, za, xa + 6, G, za + 6, TF)                 # плита пола
    for (x, z) in ((xa, za), (xa + 6, za), (xa, za + 6), (xa + 6, za + 6)):
        for y in range(G + 1, G + 6):
            put(x, y, z, GCOL("y"))                        # опоры заново поверх
    for k, off in enumerate((-1, 0, 1, 2, 3)):             # шатёр 9->1
        band(xa + off, za + off, xa + 6 - off, za + 6 - off, G + 7 + k, TB)
    fill(xa + 3, G + 12, za + 3, xa + 3, G + 14, za + 3, TG)

canopy(6, 66)          # слева у лестницы
canopy(60, 68)         # справа у лестницы

# ------------------------------------------------------------------ 6) ЛЕСТНИЦЫ СО ВСЕХ 4 СТОРОН
def stairs_z(x0, x1, z0, dz, drift):     # пролёт вдоль оси z (dz=+1 вниз на юг, dz=-1 вниз на север)
    for k in range(17):
        s = k // 3
        xa, xb = x0 + drift * s, x1 + drift * s
        y = G - 1 - k
        z = z0 + dz * k
        for x in range(xa, xb + 1):
            put(x, y, z, TBS("south" if dz > 0 else "north"))
        put(xa - 1, y + 1, z, TB); put(xb + 1, y + 1, z, TB)
        if k % 3 == 0:
            put(xa - 1, y + 2, z, TBSl("bottom"))
            put(xb + 1, y + 2, z, TBSl("bottom"))
    put(x0 + drift * 6 - 1, G - 15, z0 + dz * 17, TB)
    put(x1 + drift * 6 + 1, G - 15, z0 + dz * 17, TB)

def stairs_x(z0, z1, x0, dx, drift):     # пролёт вдоль оси x
    for k in range(17):
        s = k // 3
        za, zb = z0 + drift * s, z1 + drift * s
        y = G - 1 - k
        x = x0 + dx * k
        for z in range(za, zb + 1):
            put(x, y, z, TBS("east" if dx < 0 else "west"))
        put(x, y + 1, za - 1, TB); put(x, y + 1, zb + 1, TB)
        if k % 3 == 0:
            put(x, y + 2, za - 1, TBSl("bottom"))
            put(x, y + 2, zb + 1, TBSl("bottom"))
    put(x0 + dx * 17, G - 15, z0 + drift * 6 - 1, TB)
    put(x0 + dx * 17, G - 15, z1 + drift * 6 + 1, TB)

stairs_z(28, 34, 62, 1, -1)    # СПЕРЕДИ — пара разбегающихся
stairs_z(43, 49, 62, 1, 1)
stairs_z(35, 41, 19, -1, 0)    # СЗАДИ одиночный пролёт на север
stairs_x(36, 42, 16, -1, 0)    # ЗАПАД
stairs_x(36, 42, 60, 1, 0)     # ВОСТОК

# ------------------------------------------------------------------ 7) СВЯТИЛИЩЕ РИТУАЛА (восстановлено после V5)
# Без него в храме нет мозаики, ядра перехода, свечей и ядра запуска (полёт на Элисар и мост ритуала не работали).
# Пол зала на G, интерьер на G+1 пуст. Мозаика 5x5 на полу, сигил-ядро в центре, 8 свечей на кольце r=3,
# ядро запуска на оси зала (z=SC_Z+8) на золотом постаменте.
MOS_T  = S("temple_mosaic", stage=0)
CORE_T = S("mosaic_core", stage=0)
CANDLE = S("ritual_candle", lit=False)
LCORE  = S("launch_core", facing="north")
SC_X, SC_Z = CX, 38
for x in range(SC_X - 2, SC_X + 3):
    for z in range(SC_Z - 2, SC_Z + 3):
        put(x, G, z, MOS_T)
put(SC_X, G, SC_Z, CORE_T)
for dx, dz in ((0, -3), (3, 0), (0, 3), (-3, 0), (-3, -3), (3, -3), (3, 3), (-3, 3)):
    put(SC_X + dx, G + 1, SC_Z + dz, CANDLE)
put(SC_X, G, SC_Z + 8, TG)
put(SC_X, G + 1, SC_Z + 8, LCORE)

# ------------------------------------------------------------------ write
n, pal = write_template(OUT + "/temple_great.nbt", (W, H, D), B, {k: v for k, v in NBT.items() if k in B})
print("temple_great V5 (строго по фото, без глаза/гало) ", W, H, D, "blocks", n, "palette", pal, "skips", len(SKIPPED))
for k in list(SKIPPED)[:6]:
    print("  SKIP", k)
