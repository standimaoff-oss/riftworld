"""Rev 6 «Храм строго по фотке» v6: ОГРОМНЫЙ ВЕЛИКИЙ ХРАМ ИЗ НОВЫХ БЛОКОВ.
Включает полный комплекс:
- Величественный экстерьер: квадратное здание на каменном стилобате-юбке;
  лестницы со всех 4 сторон (спереди пара); 8 башен разной высоты вразброс
  (4 углобашенки + 4 отдельные звонницы); навесы на гранд-колоннах;
  только светло-голубое стекло; золотой портал;
- Полный грандиозный интерьер:
  - Трёхневая базилика: 12 гранд-колонн temple_grand_column с золотыми капителями;
  - Продольные и поперечные арки нефа;
  - Витражные розы temple_rose_window и окна в верхнем ярусе (клерестории);
  - Сводчатый нервюрный потолок и двускатная крыша с золотым гребнем;
  - Световой барабан-купол над средокрестием со шпилем;
  - Спящие духи храма temple_spirit в стенах;
  - Висячие люстры keep_cage_lantern под сводами;
  - Процессионный рунный проход temple_gold_rune;
  - Возвышенное святилище-апсида: ритуальный круг temple_mosaic, ядро mosaic_core,
    8 ритуальных свечей ritual_candle;
  - За алтарём — монументальные Врата Разлома из demon_frame и золота,
    где ритуал разрывает пространство и открывает ЧЕРНЫЙ ПРОЛОМ ПРОСТРАНСТВА;
  - Подземный склеп-крипта с саркофагами, древними ликами и таинственным светом.
Все блоки — строго новые блоки мода!
-> data/riftworld/structures/city/temple_great.nbt
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
KSM  = S("keep_stone_mossy")
GL   = S("minecraft:light_blue_stained_glass")          # стекло — ТОЛЬКО светло-голубое
TBSl = lambda t="bottom": S("temple_bricks_slab", type=t)
TBS  = lambda f, **kw: S("temple_bricks_stairs", facing=f, **kw)
GCOL = lambda a: S("temple_grand_column", axis=a)
PIL  = lambda a: S("temple_pillar", axis=a)
ROSE = S("temple_rose_window")
RUNE = lambda lit=True: S("temple_gold_rune", lit=lit)
MOS_T= lambda st=0: S("temple_mosaic", stage=st)
CORE = lambda st=0: S("mosaic_core", stage=st)
CANDLE = lambda lit=False: S("ritual_candle", lit=lit)
SPIR = S("temple_spirit")
DFRAME = S("demon_frame")
LANTERN = S("keep_cage_lantern")

CX = 37                                                 # геометрический центр главного здания

# ------------------------------------------------------------------ 0) КАМЕННАЯ ЮБКА-ПОДНОЖИЕ под всей площадкой
for k in range(1, 9):
    band(13 - k // 3, 18 - k // 3, 61 + k // 3, 63 + k // 3, G - k, TB)

# ------------------------------------------------------------------ 1) ГЛАВНОЕ ЗДАНИЕ 40×38 h=34 (фасад на z=59)
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

# ------------------------------------------------------------------ 2) ФАСАД: Дверь + высокое арочное окно над ней
# высокое арочное светло-голубое ОКНО над дверью
fill(CX - 2, G + 12, 59, CX + 2, G + 19, 59, GL)
put(CX - 1, G + 20, 59, GL); put(CX + 1, G + 20, 59, GL); put(CX, G + 21, 59, GL)
for y in range(G + 11, G + 20):
    put(CX - 3, y, 59, KSC); put(CX + 3, y, 59, KSC)
fill(CX - 3, G + 11, 59, CX + 3, G + 11, 59, KSC)
put(CX - 2, G + 21, 59, KSC); put(CX + 2, G + 21, 59, KSC)

# фасадные арочные ниши по бокам от окна
for x in (23, 51):
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

# ------------------------------------------------------------------ 3) ЧЕТЫРЕ ПРИЧАСТНЫЕ УГЛОБАШЕНКИ на самом здании
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

# ------------------------------------------------------------------ 4) 4 ОТДЕЛЬНЫЕ БАШНИ РАЗНОЙ ВЫСОТЫ ВРАЗБРОС (звонницы)
def stand_tower(xa, za, shaft):
    xb, zbb = xa + 6, za + 6
    for k in range(1, 5):
        band(xa - 2 + k // 2, za - 2 + k // 2, xb + 2 - k // 2, zbb + 2 - k // 2, G - k, TB)
    walls(xa, za, xb, zbb, G + 1, G + shaft, t=1, mat=lambda x, y, z: TB)
    fill(xa + 1, G, za + 1, xb - 1, G, zbb - 1, TF)
    band(xa - 1, za - 1, xb + 1, zbb + 1, G + shaft // 2, TB)
    band(xa - 1, za - 1, xb + 1, zbb + 1, G + shaft - 2, TB)
    mx, mz = xa + 3, za + 3
    fill(mx, G + 10, zbb, mx, G + 13, zbb, GL)
    fill(mx, G + shaft - 12, zbb, mx, G + shaft - 9, zbb, GL)
    walls(xa - 1, za - 1, xb + 1, zbb + 1, G + shaft, G + shaft + 10, t=1, mat=lambda x, y, z: TB)
    for k in (1, 5):
        fill(xa - 1 + k, G + shaft + 2, za - 1, xa + k, G + shaft + 7, za - 1, GL)
        fill(xa - 1 + k, G + shaft + 2, zbb + 1, xa + k, G + shaft + 7, zbb + 1, GL)
        fill(xa - 1, G + shaft + 2, za - 1 + k, xa - 1, G + shaft + 7, za + k, GL)
        fill(xb + 1, G + shaft + 2, za - 1 + k, xb + 1, G + shaft + 7, za + k, GL)
    band(xa - 2, za - 2, xb + 2, zbb + 2, G + shaft + 10, TB)
    for k, off in enumerate((-1, 0, 0, 1, 1, 2, 2, 3)):
        band(xa + 1 + off, za + 1 + off, xb - 1 - off, zbb - 1 - off, G + shaft + 11 + k, TB)
    fill(mx, G + shaft + 19, mz, mx, G + shaft + 25, mz, TG)
    fill(mx, G + shaft + 23, mz - 1, mx, G + shaft + 23, mz + 1, TG)
    fill(mx - 1, G + shaft + 23, mz, mx + 1, G + shaft + 23, mz, TG)

stand_tower(4, 8, 52)      # дальняя левая — САМАЯ ВЫСОКАЯ (top G+77)
stand_tower(62, 5, 40)     # дальняя правая (top G+65)
stand_tower(2, 44, 30)     # ближняя левая (top G+55)
stand_tower(64, 40, 22)    # ближняя правая низкая (top G+47)

# ------------------------------------------------------------------ 5) НАВЕСЫ-БЕСЕДКИ НА ОПОРАХ
def canopy(xa, za):
    for (x, z) in ((xa, za), (xa + 6, za), (xa, za + 6), (xa + 6, za + 6)):
        fill(x, G, z, x, G, z, TF)
        fill(x, G + 1, z, x, G + 5, z, GCOL("y"))
        put(x, G + 6, z, TG)
    fill(xa, G, za, xa + 6, G, za + 6, TF)
    for (x, z) in ((xa, za), (xa + 6, za), (xa, za + 6), (xa + 6, za + 6)):
        for y in range(G + 1, G + 6):
            put(x, y, z, GCOL("y"))
    for k, off in enumerate((-1, 0, 1, 2, 3)):
        band(xa + off, za + off, xa + 6 - off, za + 6 - off, G + 7 + k, TB)
    fill(xa + 3, G + 12, za + 3, xa + 3, G + 14, za + 3, TG)

canopy(6, 66)          # слева у лестницы
canopy(60, 68)         # справа у лестницы

# ------------------------------------------------------------------ 6) ЛЕСТНИЦЫ СО ВСЕХ 4 СТОРОН
def stairs_z(x0, x1, z0, dz, drift):
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

def stairs_x(z0, z1, x0, dx, drift):
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
stairs_z(35, 41, 19, -1, 0)    # СЗАДИ
stairs_x(36, 42, 16, -1, 0)    # ЗАПАД
stairs_x(36, 42, 60, 1, 0)     # ВОСТОК

# ================================================================== 7) ВЕЛИЧЕСТВЕННЫЙ ИНТЕРЬЕР: КОЛОННАДЫ, СВОДЫ, АРКИ
# Колоннада главного нефа: 12 grand column (x=27 и x=47)
col_z = (26, 32, 38, 44, 50, 56)
for z in col_z:
    for x in (27, 47):
        put(x, G + 1, z, TB)                               # пьедестал
        fill(x, G + 2, z, x, G + 14, z, GCOL("y"))         # ствол гранд-колонны
        put(x, G + 15, z, TG)                              # золотая капитель

# Продольные аркады нефа вдоль оси Z
for x in (27, 47):
    fill(x, G + 15, 23, x, G + 15, 57, TB)                 # антаблемент над колоннами
    for i in range(len(col_z) - 1):
        za, zb = col_z[i], col_z[i + 1]
        zm = (za + zb) // 2
        put(x, G + 14, zm - 1, TBSl("top"))
        put(x, G + 14, zm,     TBSl("top"))
        put(x, G + 14, zm + 1, TBSl("top"))

# Поперечные нервюрные арки через неф (от x=27 до x=47)
for z in col_z:
    fill(28, G + 16, z, 31, G + 16, z, TBSl("bottom"))
    fill(32, G + 17, z, 34, G + 17, z, TBSl("bottom"))
    fill(35, G + 18, z, 39, G + 18, z, TG)                 # золотой замковый камень
    fill(40, G + 17, z, 42, G + 17, z, TBSl("bottom"))
    fill(43, G + 16, z, 46, G + 16, z, TBSl("bottom"))

# Клересторий (верхний ярус нефа над колоннами): стены + витражные розы
for x in (27, 47):
    walls_facing = "west" if x == 27 else "east"
    fill(x, G + 16, 23, x, G + 24, 57, TB)
    for z in (29, 35, 41, 47, 53):
        put(x, G + 19, z, ROSE)                            # светящаяся витражная роза
        fill(x, G + 21, z, x, G + 23, z, GL)               # стрельчатое стекло над розой
        put(x, G + 24, z, TG)

# Сводчатый нервюрный потолок центрального нефа
for z in range(23, 58):
    for s in range(5):
        y = G + 24 + s
        put(28 + s, y, z, TBSl("bottom"))
        put(46 - s, y, z, TBSl("bottom"))
    fill(33, G + 28, z, 41, G + 28, z, TBSl("bottom"))

# Потолок боковых нефов
for z in range(23, 58):
    fill(19, G + 16, z, 26, G + 16, z, TBSl("bottom"))
    fill(48, G + 16, z, 55, G + 16, z, TBSl("bottom"))

# ================================================================== 8) КРЫША И СВЕТОВОЙ КУПОЛ-ДРОМ
# Двускатная крыша над главным зданием
for s in range(12):
    y = G + 34 + s
    for z in range(20, 60):
        put(16 + s, y, z, TBS("east"))
        put(58 - s, y, z, TBS("west"))
# Золотой конёк крыши
fill(28, G + 45, 20, 46, G + 45, 60, TBSl("bottom"))
fill(CX, G + 46, 20, CX, G + 46, 60, TG)

# Купольный световой дром над средокрестием (z 35..45, x 32..42)
walls(32, 35, 42, 45, G + 35, G + 42, t=1, mat=lambda x, y, z: TB)
for z in (37, 40, 43):
    fill(32, G + 37, z, 32, G + 40, z, GL)
    fill(42, G + 37, z, 42, G + 40, z, GL)
for x in (34, 37, 40):
    fill(x, G + 37, 35, x, G + 40, 35, GL)
    fill(x, G + 37, 45, x, G + 40, 45, GL)
band(31, 34, 43, 46, G + 42, TB)
band(31, 34, 43, 46, G + 43, TG)
for k in range(5):
    fill(33 + k, G + 44 + k, 36 + k, 41 - k, G + 44 + k, 44 - k, TB)
fill(CX, G + 49, 40, CX, G + 56, 40, TG)                   # венчающий золотой шпиль

# ================================================================== 9) ДЕКОР СТЕН, СПЯЩИЕ ДУХИ И ЛЮСТРЫ
# Спящие духи храма в боковых стенах (просыпаются при приближении игрока)
for z in (26, 32, 38, 44, 50, 56):
    put(18, G + 4, z, SPIR); put(56, G + 4, z, SPIR)
    put(18, G + 10, z, SPIR); put(56, G + 10, z, SPIR)
    put(18, G + 3, z, TG);   put(56, G + 3, z, TG)
    put(18, G + 5, z, TG);   put(56, G + 5, z, TG)

# Огромные кованые люстры в нефе
for z in (32, 44, 52):
    fill(CX, G + 18, z, CX, G + 27, z, PIL("y"))
    put(CX, G + 17, z, LANTERN)
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        put(CX + dx, G + 17, z + dz, LANTERN)
        put(CX + dx * 2, G + 18, z + dz * 2, LANTERN)

# Процессионная рунная дорожка к алтарю
fill(CX - 1, G, 32, CX + 1, G, 58, RUNE(lit=True))
fill(CX - 2, G, 32, CX - 2, G, 58, TG)
fill(CX + 2, G, 32, CX + 2, G, 58, TG)

# Скамьи для прихожан в боковых пролётах
for z in (34, 36, 40, 42, 46, 48, 52, 54):
    for x in (21, 23, 25):
        put(x, G + 1, z, TBS("south"))
    for x in (49, 51, 53):
        put(x, G + 1, z, TBS("south"))

# ================================================================== 10) СВЯТИЛИЩЕ: АЛТАРЬ, РИТУАЛЬНЫЙ КРУГ, ВРАТА РАЗЛОМА
# Возвышенная площадка святилища (апсида)
fill(31, G + 1, 23, 43, G + 1, 31, TF)
fill(31, G + 1, 31, 43, G + 1, 31, TBS("south"))           # парадная ступень
band(31, 23, 43, 31, G + 1, TG)                             # золотой сигиловый бордюр

# Ритуальный мозаичный круг 5×5 со светящимися рунами
fill(CX - 2, G + 1, 26, CX + 2, G + 1, 30, MOS_T(0))
put(CX, G + 1, 28, CORE(0))                                # ЯДРО РИТУАЛА (MosaicCore)

# 8 ритуальных свечей в священном круге
candle_coords = (
    (CX - 2, 26), (CX, 26), (CX + 2, 26),
    (CX - 2, 28),           (CX + 2, 28),
    (CX - 2, 30), (CX, 30), (CX + 2, 30)
)
for (cx, cz) in candle_coords:
    put(cx, G + 1, cz, RUNE(lit=True))                     # рунное подножие свечи
    put(cx, G + 2, cz, CANDLE(lit=False))                  # незажжённая ритуальная свеча

# Окантовка алтаря
for x in range(CX - 3, CX + 4):
    put(x, G + 1, 25, TG)
for z in range(26, 31):
    put(CX - 3, G + 1, z, TG); put(CX + 3, G + 1, z, TG)

# Монументальные ВРАТА РАЗЛОМА из demon_frame и золота
# Алтарный постамент перед вратами
fill(CX - 2, G + 2, 23, CX + 2, G + 2, 24, TG)
fill(CX - 1, G + 2, 24, CX + 1, G + 2, 24, KS)

# Портальная рама из demon_frame (портал открывается здесь при завершении ритуала!)
# Левый и правый пилоны
fill(CX - 2, G + 2, 23, CX - 2, G + 8, 23, DFRAME)
fill(CX + 2, G + 2, 23, CX + 2, G + 8, 23, DFRAME)
# Верхняя арка
fill(CX - 2, G + 8, 23, CX + 2, G + 8, 23, DFRAME)
put(CX - 1, G + 7, 23, DFRAME); put(CX + 1, G + 7, 23, DFRAME)
put(CX, G + 7, 23, TG)

# Золотые демонические рога-шипы на вратах
put(CX - 2, G + 9, 23, TG); put(CX - 2, G + 10, 23, TG)
put(CX - 3, G + 10, 23, TG)
put(CX + 2, G + 9, 23, TG); put(CX + 2, G + 10, 23, TG)
put(CX + 3, G + 10, 23, TG)
put(CX, G + 9, 23, TG)

# Светильники у алтаря
put(CX - 3, G + 3, 24, LANTERN)
put(CX + 3, G + 3, 24, LANTERN)

# ================================================================== 11) ПОДЗЕМНЫЙ СКЛЕП-КРИПТА
# Лестница вниз из восточного нефа
for k in range(1, 8):
    zz = 32 + k
    put(49, G - k + 1, zz, TBS("north"))
    fill(49, G - k + 2, zz, 50, G + 2, zz, AIR)
fill(49, G + 1, 32, 50, G + 2, 32, AIR)

# Зал крипты (y G-6 .. G-1)
fill(26, G - 6, 23, 48, G - 1, 39, AIR)
fill(25, G - 7, 22, 49, G - 7, 40, KS)                     # пол из утёсного камня
walls(25, 22, 49, 40, G - 6, G - 1, t=1, mat=lambda x, y, z: KSC) # стены с трещинами
fill(26, G, 23, 48, G, 39, TF)                             # потолок крипты / пол храма

# Опорные колонны крипты
for x in (32, 42):
    for z in (28, 35):
        fill(x, G - 6, z, x, G - 1, z, KSC)
        put(x, G - 6, z - 1, TBSl("bottom")); put(x, G - 6, z + 1, TBSl("bottom"))

# Древние саркофаги с резными ликами духов
sarcophagi = ((28, 26), (46, 26), (28, 32), (46, 32), (28, 37), (46, 37))
for (sx, sz) in sarcophagi:
    fill(sx, G - 6, sz, sx + 1, G - 6, sz + 1, KS)
    put(sx, G - 5, sz, TBSl("top")); put(sx + 1, G - 5, sz, TBSl("top"))
    put(sx, G - 5, sz + 1, TBSl("top")); put(sx + 1, G - 5, sz + 1, TBSl("top"))
    put(sx, G - 5, sz, SPIR)                               # спящий дух на саркофаге

# Освещение крипты
put(32, G - 4, 28, LANTERN); put(42, G - 4, 28, LANTERN)
put(32, G - 4, 35, LANTERN); put(42, G - 4, 35, LANTERN)

# ------------------------------------------------------------------ write
n, pal = write_template(OUT + "/temple_great.nbt", (W, H, D), B, {k: v for k, v in NBT.items() if k in B})
print("temple_great V6 (ОГРОМНЫЙ ХРАМ ИЗ НОВЫХ БЛОКОВ + РИТУАЛ) ", W, H, D, "blocks", n, "palette", pal, "skips", len(SKIPPED))
for k in list(SKIPPED)[:6]:
    print("  SKIP", k)
