"""Rev 7 «Великий Храм Разлома: Архитектурный Шедевр»:
Грандиозный собор базиликального типа исключительно из новых блоков мода.
- Многоярусная архитектура: высокий центральный неф, боковые нефы с аркбутанами,
  полукруглая апсида, трансепт, световой купольный барабан-дром со шпилем;
- 8 стройных готических башен разной высоты с открытыми звонницами и золотыми шпилями;
- Фасад с глубокими порталами, архивольтами и витражной розой;
- Роскошный интерьер: 12 гранд-колонн с золотыми капителями, нервюрные крестовые своды,
  верхний ярус (клересторий) с розами и голубыми стрельчатыми окнами,
  висячие люстры, процессионная рунная дорожка;
- Возвышенное святилище-апсида со ступенями, где установлена УНИКАЛЬНАЯ 3D МОЗАИКА
  (MosaicCoreRenderer) и 8 ритуальных свечей;
- В воздухе над алтарём парит УНИКАЛЬНЫЙ 3D ЧЁРНЫЙ ПРОЛОМ ПРОСТРАНСТВА (DemonGateRenderer) —
  органический разлом реальности, свободный от блочных рамок;
- Подземная сводчатая крипта с саркофагами и древними тайнами.
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
GL   = S("minecraft:light_blue_stained_glass")
TBSl = lambda t="bottom": S("temple_bricks_slab", type=t)
TBS  = lambda f, **kw: S("temple_bricks_stairs", facing=f, **kw)
GCOL = lambda a="y": S("temple_grand_column", axis=a)
PIL  = lambda a="y": S("temple_pillar", axis=a)
ROSE = S("temple_rose_window")
RUNE = lambda lit=True: S("temple_gold_rune", lit=lit)
MOS_T= lambda st=0: S("temple_mosaic", stage=st)
CORE = lambda st=0: S("mosaic_core", stage=st)
CANDLE = lambda lit=False: S("ritual_candle", lit=lit)
SPIR = S("temple_spirit")
DFRAME = S("demon_frame")
DGATE = S("demon_gate")
LANTERN = S("keep_cage_lantern")

CX = 37                                                 # центральная ось храма

# ------------------------------------------------------------------ 0) МОНУМЕНТАЛЬНЫЙ СТИЛОБАТ-ПОДИУМ
for k in range(1, 9):
    band(13 - k // 3, 18 - k // 3, 61 + k // 3, 63 + k // 3, G - k, TB)
fill(15, G, 20, 59, G, 61, TF)

# ------------------------------------------------------------------ 1) ГЛАВНЫЙ НЕФ И БОКОВЫЕ НЕФЫ
# Внешние стены боковых нефов (x=17..26 и x=48..57, z=23..59)
walls(17, 23, 57, 59, G + 1, G + 17, t=2, mat=lambda x, y, z: TB)
band(16, 22, 58, 60, G, TB)
band(16, 22, 58, 60, G + 16, TG)
band(16, 22, 58, 60, G + 17, TB)

# Стены высокого центрального нефа (x=27..47, y=G+18..G+35)
walls(27, 23, 47, 59, G + 17, G + 35, t=1, mat=lambda x, y, z: TB)
band(26, 22, 48, 60, G + 34, TG)
band(26, 22, 48, 60, G + 35, TB)

# Полукруглая апсида на севере (z=17..23, x=29..45)
for z in range(17, 24):
    w = int(math.sqrt(max(0, 36 - (23 - z) ** 2)))
    fill(CX - w, G, z, CX + w, G, z, TF)
    walls(CX - w - 1, z, CX + w + 1, z + 1, G + 1, G + 28, t=1, mat=lambda x, y, z: TB)
    if z in (18, 20, 22):
        fill(CX - w - 1, G + 6, z, CX - w - 1, G + 12, z, GL)
        fill(CX + w + 1, G + 6, z, CX + w + 1, G + 12, z, GL)

# ------------------------------------------------------------------ 2) АРКБУТАНЫ И КОНТРФОРСЫ (Gothic Flying Buttresses)
# Контрфорсы вдоль боковых фасадов
for z in (27, 33, 39, 45, 51, 57):
    # Пилоны контрфорсов на боковых стенах
    for x in (16, 58):
        fill(x, G + 1, z - 1, x, G + 20, z + 1, TB)
        put(x, G + 21, z, TG)
        put(x, G + 22, z, TBSl("bottom"))
        put(x, G + 23, z, TG)                          # пинакль
    # Летящие арки (аркбутаны) от боковых пилонов к стене верхнего нефа
    for step in range(5):
        xx1 = 17 + step * 2
        xx2 = 57 - step * 2
        yy = G + 17 + step * 3
        fill(xx1, yy, z, xx1 + 1, yy, z, TBSl("bottom"))
        fill(xx2 - 1, yy, z, xx2, yy, z, TBSl("bottom"))

# ------------------------------------------------------------------ 3) ДВУХЪЯРУСНЫЕ ОКНА И ВИТРАЖНЫЕ РОЗЫ
# Нижний ярус: стрельчатые окна в боковых нефах
for z in (29, 35, 41, 47, 53):
    fill(17, G + 6, z - 1, 17, G + 11, z + 1, GL)
    fill(57, G + 6, z - 1, 57, G + 11, z + 1, GL)
    put(17, G + 12, z, GL); put(57, G + 12, z, GL)

# Верхний ярус (клересторий): розы и витражи нефа
for z in (29, 35, 41, 47, 53):
    put(27, G + 22, z, ROSE); put(47, G + 22, z, ROSE)
    fill(27, G + 24, z, 27, G + 28, z, GL)
    fill(47, G + 24, z, 47, G + 28, z, GL)
    put(27, G + 29, z, TG);   put(47, G + 29, z, TG)

# ------------------------------------------------------------------ 4) ПАРАДНЫЙ ФАСАД С ПОРТАЛАМИ (Юг, z=59)
# Центральный заглубленный портал с архивольтами
for step in range(3):
    d = 59 + step
    w = 4 - step
    walls(CX - w, d, CX + w, d, G + 1, G + 10 - step, t=1, mat=lambda x, y, z: TB)
    put(CX - w, G + 1, d, TG); put(CX + w, G + 1, d, TG)

# Входной проём (открытый парадный проход)
fill(CX - 2, G + 1, 59, CX + 2, G + 7, 61, AIR)
fill(CX - 2, G, 59, CX + 2, G, 63, TF)

# Парадная роза фасада и арочные окна
fill(CX - 2, G + 16, 59, CX + 2, G + 22, 59, GL)
put(CX, G + 19, 59, ROSE)
fill(CX - 1, G + 23, 59, CX + 1, G + 23, 59, GL)
put(CX, G + 24, 59, TG)

# Боковые ниши фасада
for x in (22, 52):
    fill(x - 1, G + 6, 59, x + 1, G + 11, 59, GL)
    put(x, G + 12, 59, TG)

# Треугольный фронтон фасада
for k in range(11):
    fill(CX - 10 + k, G + 35 + k, 59, CX + 10 - k, G + 35 + k, 59, TB)
fill(CX, G + 46, 59, CX, G + 50, 59, TG)

# ------------------------------------------------------------------ 5) 8 ГОТИЧЕСКИХ БАШЕН РАЗНОЙ ВЫСОТЫ СО ЗВОННИЦАМИ
def gothic_bell_tower(xa, za, shaft, has_bells=True):
    xb, zbb = xa + 6, za + 6
    mx, mz = xa + 3, za + 3
    # База и цоколь
    for k in range(1, 5):
        band(xa - 2 + k // 2, za - 2 + k // 2, xb + 2 - k // 2, zbb + 2 - k // 2, G - k, TB)
    # Ствол башни
    walls(xa, za, xb, zbb, G + 1, G + shaft, t=1, mat=lambda x, y, z: TB)
    fill(xa + 1, G, za + 1, xb - 1, G, zbb - 1, TF)
    band(xa - 1, za - 1, xb + 1, zbb + 1, G + shaft // 2, TG)
    band(xa - 1, za - 1, xb + 1, zbb + 1, G + shaft - 2, TB)

    # Стрельчатые окна на стволе
    fill(mx, G + 8, zbb, mx, G + 12, zbb, GL)
    fill(mx, G + shaft - 14, zbb, mx, G + shaft - 10, zbb, GL)

    # Открытый ярус звонницы с аркадами
    walls(xa - 1, za - 1, xb + 1, zbb + 1, G + shaft, G + shaft + 10, t=1, mat=lambda x, y, z: TB)
    # Арочные открытые проёмы звонницы со всех 4 сторон
    for dx in (1, 5):
        fill(xa - 1 + dx, G + shaft + 2, za - 1, xa + dx, G + shaft + 8, za - 1, AIR)
        fill(xa - 1 + dx, G + shaft + 2, zbb + 1, xa + dx, G + shaft + 8, zbb + 1, AIR)
        fill(xa - 1, G + shaft + 2, za - 1 + dx, xa - 1, G + shaft + 8, za + dx, AIR)
        fill(xb + 1, G + shaft + 2, za - 1 + dx, xb + 1, G + shaft + 8, za + dx, AIR)

    # Золотые колокола в звоннице
    if has_bells:
        fill(mx, G + shaft + 7, mz, mx, G + shaft + 8, mz, TG)
        put(mx, G + shaft + 6, mz, LANTERN)

    # Венчающий карниз и балюстрада
    band(xa - 2, za - 2, xb + 2, zbb + 2, G + shaft + 10, TG)
    for (cx, cz) in ((xa - 2, za - 2), (xb + 2, za - 2), (xa - 2, zbb + 2), (xb + 2, zbb + 2)):
        put(cx, G + shaft + 11, cz, TG); put(cx, G + shaft + 12, cz, TBSl("bottom"))

    # Высокий восьмигранный шатёр со шпилем
    for k, off in enumerate((-1, 0, 0, 1, 1, 2, 2, 3)):
        band(xa + 1 + off, za + 1 + off, xb - 1 - off, zbb - 1 - off, G + shaft + 11 + k, TB)
    # Золотой шпиль
    fill(mx, G + shaft + 19, mz, mx, G + shaft + 27, mz, TG)
    fill(mx - 1, G + shaft + 24, mz, mx + 1, G + shaft + 24, mz, TG)
    fill(mx, G + shaft + 24, mz - 1, mx, G + shaft + 24, mz + 1, TG)

# 4 высокие отдельно стоящие звонницы разной высоты
gothic_bell_tower(4, 8, 54, has_bells=True)       # Север-Запад: высшая звонница (top G+81)
gothic_bell_tower(62, 5, 42, has_bells=True)      # Север-Восток (top G+69)
gothic_bell_tower(2, 44, 32, has_bells=True)      # Юг-Запад (top G+59)
gothic_bell_tower(64, 40, 24, has_bells=True)     # Юг-Восток (top G+51)

# 4 угловые башенки на самом здании
def corner_turret(xa, za, h):
    xb, zbb = xa + 5, za + 5
    mx, mz = xa + 2, za + 2
    walls(xa, za, xb, zbb, G + 1, G + h, t=1, mat=lambda x, y, z: TB)
    band(xa - 1, za - 1, xb + 1, zbb + 1, G + h, TG)
    for k in range(5):
        band(xa + k // 2, za + k // 2, xb - k // 2, zbb - k // 2, G + h + 1 + k, TB)
    fill(mx, G + h + 6, mz, mx, G + h + 11, mz, TG)

corner_turret(14, 52, 36); corner_turret(54, 52, 36)
corner_turret(14, 18, 32); corner_turret(54, 18, 32)

# ------------------------------------------------------------------ 6) ПАРАДНЫЕ ЛЕСТНИЦЫ СО ВСЕХ 4 СТОРОН
def grand_stairs_z(x0, x1, z0, dz, drift):
    for k in range(17):
        s = k // 3
        xa, xb = x0 + drift * s, x1 + drift * s
        y = G - 1 - k
        z = z0 + dz * k
        for x in range(xa, xb + 1):
            put(x, y, z, TBS("south" if dz > 0 else "north"))
        put(xa - 1, y + 1, z, TB); put(xb + 1, y + 1, z, TB)
        if k % 3 == 0:
            put(xa - 1, y + 2, z, TG)
            put(xb + 1, y + 2, z, TG)

def grand_stairs_x(z0, z1, x0, dx, drift):
    for k in range(17):
        s = k // 3
        za, zb = z0 + drift * s, z1 + drift * s
        y = G - 1 - k
        x = x0 + dx * k
        for z in range(za, zb + 1):
            put(x, y, z, TBS("east" if dx < 0 else "west"))
        put(x, y + 1, za - 1, TB); put(x, y + 1, zb + 1, TB)
        if k % 3 == 0:
            put(x, y + 2, za - 1, TG)
            put(x, y + 2, zb + 1, TG)

grand_stairs_z(27, 34, 62, 1, -1)   # Спереди пара широких
grand_stairs_z(40, 47, 62, 1, 1)
grand_stairs_z(33, 41, 18, -1, 0)   # Сзади
grand_stairs_x(36, 44, 15, -1, 0)   # Запад
grand_stairs_x(36, 44, 59, 1, 0)    # Восток

# ------------------------------------------------------------------ 7) ВЕЛИЧЕСТВЕННЫЙ ИНТЕРЬЕР: КОЛОННАДЫ, СВОДЫ
# 12 Гранд-колонн нефа (x=27 и x=47)
col_z = (26, 32, 38, 44, 50, 56)
for z in col_z:
    for x in (27, 47):
        # База колонны
        fill(x - 1, G + 1, z - 1, x + 1, G + 1, z + 1, TB)
        put(x, G + 1, z, TG)
        # Ствол гранд-колонны
        fill(x, G + 2, z, x, G + 15, z, GCOL("y"))
        # Золотая коринфская капитель
        fill(x - 1, G + 16, z - 1, x + 1, G + 16, z + 1, TG)

# Продольные аркады над колоннами
for x in (27, 47):
    fill(x, G + 16, 23, x, G + 16, 58, TB)
    for i in range(len(col_z) - 1):
        za, zb = col_z[i], col_z[i + 1]
        zm = (za + zb) // 2
        put(x, G + 15, zm - 1, TBSl("top"))
        put(x, G + 15, zm,     TG)
        put(x, G + 15, zm + 1, TBSl("top"))

# Поперечные нервюрные арки нефа (стрельчатые)
for z in col_z:
    fill(28, G + 17, z, 30, G + 17, z, TBSl("bottom"))
    fill(31, G + 18, z, 33, G + 18, z, TBSl("bottom"))
    fill(34, G + 19, z, 35, G + 19, z, TBSl("bottom"))
    fill(36, G + 20, z, 38, G + 20, z, TG)                # золотой замковый камень
    fill(39, G + 19, z, 40, G + 19, z, TBSl("bottom"))
    fill(41, G + 18, z, 43, G + 18, z, TBSl("bottom"))
    fill(44, G + 17, z, 46, G + 17, z, TBSl("bottom"))

# Крестовые нервюрные своды нефа
for z in range(23, 58):
    for s in range(6):
        y = G + 25 + s
        put(28 + s, y, z, TBSl("bottom"))
        put(46 - s, y, z, TBSl("bottom"))
    fill(34, G + 31, z, 40, G + 31, z, TBSl("bottom"))

# Своды боковых нефов
for z in range(23, 58):
    fill(19, G + 16, z, 26, G + 16, z, TBSl("bottom"))
    fill(48, G + 16, z, 55, G + 16, z, TBSl("bottom"))

# ------------------------------------------------------------------ 8) КРОВЛЯ И ЦЕНТРАЛЬНЫЙ СВЕТОВОЙ КУПОЛ
# Скатная крыша верхнего нефа
for s in range(11):
    y = G + 35 + s
    for z in range(22, 60):
        put(26 + s, y, z, TBS("east"))
        put(48 - s, y, z, TBS("west"))
fill(CX, G + 46, 22, CX, G + 46, 60, TG)                 # золотой конёк

# Центральный восьмигранный световой купол-дром (z 35..45, x 32..42)
fill(33, G + 31, 36, 41, G + 31, 44, AIR)                # открытый колодец света
walls(32, 35, 42, 45, G + 35, G + 45, t=1, mat=lambda x, y, z: TB)
# 16 арочных витражных окон в барабане купола
for z in (37, 40, 43):
    fill(32, G + 38, z, 32, G + 42, z, GL)
    fill(42, G + 38, z, 42, G + 42, z, GL)
for x in (34, 37, 40):
    fill(x, G + 38, 35, x, G + 42, 35, GL)
    fill(x, G + 38, 45, x, G + 42, 45, GL)
# Купол и золотой шпиль
band(31, 34, 43, 46, G + 45, TG)
for k in range(5):
    fill(33 + k, G + 46 + k, 36 + k, 41 - k, G + 46 + k, 44 - k, TB)
fill(CX, G + 51, 40, CX, G + 62, 40, TG)                 # венчающий золотой шпиль

# ------------------------------------------------------------------ 9) ДЕКОР, СПЯЩИЕ ДУХИ, ЛЮСТРЫ И ПРОЦЕССИОННЫЙ НЕФ
# Спящие духи в арочных стенных нишах
for z in (26, 32, 38, 44, 50, 56):
    put(18, G + 4, z, SPIR); put(56, G + 4, z, SPIR)
    put(18, G + 5, z, TG);   put(56, G + 5, z, TG)
    put(18, G + 9, z, SPIR); put(56, G + 9, z, SPIR)

# Массивные кованые люстры в нефе
for z in (32, 44, 52):
    fill(CX, G + 20, z, CX, G + 30, z, PIL("y"))
    put(CX, G + 19, z, LANTERN)
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        put(CX + dx, G + 19, z + dz, LANTERN)
        put(CX + dx * 2, G + 20, z + dz * 2, LANTERN)

# Процессионная рунная дорожка
fill(CX - 1, G, 31, CX + 1, G, 58, RUNE(lit=True))
fill(CX - 2, G, 31, CX - 2, G, 58, TG)
fill(CX + 2, G, 31, CX + 2, G, 58, TG)

# ------------------------------------------------------------------ 10) СВЯТИЛИЩЕ: АПСИДА, 3D МОЗАИКА И 3D ПРОЛОМ
# Возвышенная полукруглая площадка святилища (3 ступени)
for step in range(3):
    y = G + 1 + step
    zs = 32 - step * 2
    fill(30 + step, y, 20, 44 - step, y, zs, TF)
    band(30 + step, 20, 44 - step, zs, y, TG)
    for x in range(30 + step, 45 - step):
        put(x, y, zs, TBS("south"))

# ЦЕНТРАЛЬНОЕ МЕСТО РИТУАЛА: 3D МОЗАИКА НА ПОЛУ
# Ядро мозаики (на котором рендерится MosaicCoreRenderer - 3D модель мозаики)
put(CX, G + 3, 26, CORE(0))

# 8 ритуальных свечей вокруг ядра в соответствии с 3D моделью
candle_coords = (
    (CX - 2, 24), (CX, 24), (CX + 2, 24),
    (CX - 2, 26),           (CX + 2, 26),
    (CX - 2, 28), (CX, 28), (CX + 2, 28)
)
for (cx, cz) in candle_coords:
    put(cx, G + 3, cz, RUNE(lit=True))
    put(cx, G + 4, cz, CANDLE(lit=False))

# Монументальная алтарная апсидная арка из demon_frame и золота
fill(CX - 4, G + 3, 20, CX - 4, G + 12, 20, DFRAME)
fill(CX + 4, G + 3, 20, CX + 4, G + 12, 20, DFRAME)
fill(CX - 4, G + 12, 20, CX + 4, G + 12, 20, DFRAME)
put(CX - 3, G + 11, 20, DFRAME); put(CX + 3, G + 11, 20, DFRAME)
put(CX, G + 11, 20, TG)

# В воздухе перед апсидой парит ТОЧКА РАЗЛОМА (где открывается 3D ЧЁРНЫЙ ПРОЛОМ)
# Свободно парящее пространство:
fill(CX - 2, G + 4, 21, CX + 2, G + 10, 21, AIR)
# Портал открывается прямо здесь — свободный органический разлом, не зажатый в блоки!

# ------------------------------------------------------------------ 11) ПОДЗЕМНАЯ СВОДЧАТАЯ КРИПТА
for k in range(1, 8):
    zz = 32 + k
    put(49, G - k + 1, zz, TBS("north"))
    fill(49, G - k + 2, zz, 50, G + 2, zz, AIR)
fill(49, G + 1, 32, 50, G + 2, 32, AIR)

# Зал крипты
fill(26, G - 6, 21, 48, G - 1, 39, AIR)
fill(25, G - 7, 20, 49, G - 7, 40, KS)
walls(25, 20, 49, 40, G - 6, G - 1, t=1, mat=lambda x, y, z: KSC)
fill(26, G, 21, 48, G, 39, TF)

# Колонны и саркофаги крипты
for x in (32, 42):
    for z in (26, 35):
        fill(x, G - 6, z, x, G - 1, z, KSC)
        put(x, G - 4, z, LANTERN)

sarcophagi = ((28, 25), (46, 25), (28, 31), (46, 31), (28, 36), (46, 36))
for (sx, sz) in sarcophagi:
    fill(sx, G - 6, sz, sx + 1, G - 6, sz + 1, KS)
    put(sx, G - 5, sz, TBSl("top")); put(sx + 1, G - 5, sz, TBSl("top"))
    put(sx, G - 5, sz + 1, TBSl("top")); put(sx + 1, G - 5, sz + 1, TBSl("top"))
    put(sx, G - 5, sz, SPIR)

# ------------------------------------------------------------------ write
n, pal = write_template(OUT + "/temple_great.nbt", (W, H, D), B, {k: v for k, v in NBT.items() if k in B})
print("temple_great V7 (АРХИТЕКТУРНЫЙ ШЕДЕВР: 3D МОЗАИКА + 3D ПРОЛОМ) ", W, H, D, "blocks", n, "palette", pal, "skips", len(SKIPPED))
