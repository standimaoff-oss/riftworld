"""Rev 6 «Храм с НУЛЯ»: Великий храм перехода V2 - детализированные фасады, 3D колонны, четкий ритм.
-> data/riftworld/structures/city/temple_great.nbt (перезаписывает старый)

План: x восток 0..70, z юг 0..84, земля y=30 (G), портику на юге (z ~68..83 смотрит на +z = south).
Базилика: гекстастильный портик, пилястровые боковые фасады с двухъярусными окнами, апсида-свеве,
двухъярусный интерьер (трёхневая), купольный дром над средокрестием, над-башни, крипта.
"""
import json, os, re, sys

HERE = os.path.dirname(os.path.abspath(__file__))
src = open(os.path.join(HERE, "gen_castle.py"), encoding="utf-8").read()
exec(src[:src.index("# ================================================================== foundation / floors")])
rooms = open(os.path.join(HERE, "castle_rooms.py"), encoding="utf-8").read()
exec(rooms[:rooms.index("def tower_door")])

OUT = MOD + "/src/main/resources/data/riftworld/structures/city"
os.makedirs(OUT, exist_ok=True)
W, D, H = 71, 85, 78
G = 30

TB   = S("temple_bricks")
TG   = S("temple_gold")
TF   = S("temple_floor")
GCOL = lambda a: S("temple_grand_column", axis=a)
ROSE = S("temple_rose_window")
RUNE = S("temple_gold_rune")
MOS_T = S("temple_mosaic", stage=0)
CORE  = S("mosaic_core", stage=0)
CANDLE= S("ritual_candle", lit=False)
TBS = lambda f, **kw: S("temple_bricks_stairs", facing=f, **kw)
TSL = lambda t="bottom": S("temple_bricks_slab", type=t)
GL  = S("minecraft:light_blue_stained_glass")
WEB = S("castle_cobweb")
SPIR= S("temple_spirit")

# ================================================================== 1) ПОДИУМ
fill(2, G - 2, 2, 68, G - 1, 82, TB)                              # массивный стилобат
band(2, 2, 68, 82, G - 1, TG)                                     # золотой карниз цоколя
for x in range(2, 69, 4):                                         # кирпичная отливка
    put(x, G - 2, 2, TSL("top")); put(x, G - 2, 82, TSL("top"))
# южная лестница (5 маршей ступенек по 3 блока глубиной)
for k, zz in enumerate((83, 82, 81)):
    for x in range(22 - k, 50 + k):
        put(x, G - 4 + k, zz, TBS("south"))
fill(24, G - 1, 80, 46, G - 1, 80, TSL("top"))

# ================================================================== 2) БОКОВЫЕ СТЕНЫ НЕФА (пилястры + два яруса окон + слепые арки)
walls(14, 14, 56, 66, G + 1, G + 19, t=2, mat=lambda x, y, z: TB)
fill(15, G, 15, 55, G, 65, TF)                                    # внутренний пол
for z in range(18, 63, 6):                                        # пилястры (полуколонны) по фасадам
    fill(14, G + 1, z, 13, G + 12, z, GCOL("y"))
    fill(56, G + 1, z, 57, G + 12, z, GCOL("y"))
for z in range(20, 60, 14):                                       # низкие розы-петли между пилястрами
    for x in (13, 55):
        fill(x, G + 3, z, x + 2, G + 5, z + 4, ROSE)
for z in range(17, 63, 5):                                        # двухъярусные ланцеты
    for o, i in ((13, [14]), (55, [56])):
        lancet("z", z, G + 8, 4, o, i)
        lancet("z", z, G + 14, 3, o, i)
for z in range(14, 67):                                           # невыступающий карниз
    put(13, G + 19, z, TSL("top")); put(55, G + 19, z, TSL("top"))
band(14, 14, 56, 66, G + 19, TSL())                               # карнизная строчка

# ================================================================== 3 ГЕКСТАСТИЛЬНЫЙ ПОРТИК (юг, z 66..79)
PC_x = (19, 27, 35, 35 + 8, 43 + 8, 43)
COLX = (18, 25, 32, 39, 46, 53)
for x in COLX:
    for z in (68, 71, 76):                                        # пиндель колонн (глубину трёхъярусную)
        if z == 71 and x in (18, 53): continue                     # фасадная колонна у стены
        fill(x, G + 1, z, x, G + 11, z, GCOL("y"))
        put(x, G + 12, z, TG)                                     # золото капители
# антаблемент: архитрав + фриз-золото + карниз
fill(16, G + 13, 66, 54, G + 13, 78, TB)
fill(16, G + 14, 66, 54, G + 14, 78, TG)
fill(16, G + 15, 66, 54, G + 15, 78, TSL())
# фронтон (меняющейся ширины треугольник) + роза
for k in range(12):
    fill(16 + k, G + 16 + k, 66, 54 - k, G + 16 + k, 68, TB)
fill(31, G + 17, 67, 39, G + 26, 67, TB)                          # шпинг-верхан
for (x, y) in ((34, G + 18), (35, G + 18), (36, G + 18), (35, G + 19)):
    put(x, y, 67, ROSE)
put(35, G + 28, 67, TG)                                           # золотой вершин
model("wall_sconce", 19, G + 4, 67, "west", powered=True)
model("wall_sconce", 52, G + 4, 67, "east", powered=True)

# ================================================================== 4) ДВЕ ЮЖНЫЕ БАШНИ (x 2..12 / 58..68, z 56..80) с выступами
for xa, xb in ((2, 12), (58, 68)):
    walls(xa, 56, xb, 80, G + 1, G + 34, t=1, mat=lambda x, y, z: TB)
    fill(xa + 1, G, 57, xb - 1, G, 79, TF)
    # выступы-стенки по ярусам (декор профиля)
    band(xa - 1, 56, xb + 1, 80, G + 8, TB)
    band(xa - 1, 56, xb + 1, 80, G + 16, TB)
    band(xa - 1, 56, xb + 1, 80, G + 24, TB)
    for xx in (xa + 3, xa + 7):                                   # ланцетки нижнего яруса
        lancet("x", xx, G + 3, 3, 80, [])
        lancet("x", xx, G + 3, 3, 56, [])
    for xx in (xa + 3, xa + 6):                                   # звонница: парные арки
        lancet("x", xx, G + 19, 6, 80, [])
        lancet("x", xx, G + 19, 6, 56, [])
    for z in range(60, 78, 4):                                    # боковые окошки
        lancet("z", z, G + 20, 4, xa, [xa + 1])
        lancet("z", z, G + 20, 4, xb, [xb - 1])
    fill(xa, G + 35, 56, xb, G + 35, 80, TB)                      # карниз
    # ярусные пилястры на внешки башни (зализанные гладкие стены -> ритм)
    for xx in (xa + 2, xa + 5, xa + 8):
        for zz in (80, 56):
            if xx <= xb - 2:
                fill(xx, G + 9, zz, xx, G + 15, zz, GCOL("y"))
    for zz in (62, 68, 74):
        fill(xa - 1, G + 9, zz, xa - 1, G + 15, zz, GCOL("y")) if False else fill(xa, G + 9, zz, xa, G + 15, zz, GCOL("y"))
        fill(xb, G + 9, zz, xb, G + 15, zz, GCOL("y"))
    # низкие розы-глазки башен через ярусы (гармония нефа с башнями)
    fill(xa + 3, G + 11, 80, xa + 7, G + 12, 80, ROSE)
    fill(xa + 3, G + 11, 56, xa + 7, G + 12, 56, ROSE)

    # шатёр-пирамидка со сходом
    for k in range(1, 6):
        band(xa + k, 56 + k, xb - k, 80 - k, G + 35 + k, TB)
    xm = (xa + xb) // 2
    fill(xm, G + 41, 68, xm, G + 44, 68, TB)
    put(xm, G + 45, 68, TG)
    # пинакли по четырём углам
    for xx in (xa, xb):
        for zz in (56, 80):
            fill(xx, G + 35, zz, xx, G + 39, zz, TB)
            put(xx, G + 40, zz, TG)

# ================================================================== 5) АПСИДА-SEVET (север, z 2..14) — многоугольный хор
walls(18, 2, 52, 14, G + 1, G + 13, t=2, mat=lambda x, y, z: TB)
# скосы углов (Okтогон усырение)
for (x1, x2, zz) in ((16, 18, 6), (52, 54, 6), (14, 16, 10), (54, 56, 10), (16, 18, 2), (52, 54, 2)):
    fill(x1, G + 1, 1 if zz == 2 else zz - 5, x2, G + 10, zz, TB)
fill(20, G, 4, 49, G, 13, TF)
for x in range(19, 52, 4):                                        # высокие светлые ланцеты хора
    lancet("x", x, G + 3, 6, 2, [])
for z in (5, 9):
    lancet("z", z, G + 3, 5, 18, [19]); lancet("z", z, G + 3, 5, 52, [53])
# радиусные капеллы (низкие ниши) вокруг апсиды
for (nx, nz) in ((15, 7), (53, 7), (35, 1)):
    walls(nx - 2, nz, nx + 2, nz + 3, G + 1, G + 5, t=1, mat=lambda x, y, z: TB)
    put(nx, G + 1, nz + 1, GCOL("y"))
# алтарь на свете
fill(30, G + 1, 6, 40, G + 1, 9, TF)
model("chapel_altar", 35, G + 2, 6, "south", powered=True)
model("candle_stand", 33, G + 2, 6, "south", powered=True)
model("candle_stand", 37, G + 2, 6, "south", powered=True)

# ================================================================== 6) ИНТЕРЬЕР: аркады 3-нава + свод + мозаичный центр
for z in range(20, 60, 7):                                        # аркады колоны
    for x in (26, 44):
        fill(x, G + 1, z, x, G + 10, z, GCOL("y"))
        put(x, G + 11, z, TG)
for z in range(20, 60, 7):                                        # арки между линиями
    fill(26, G + 11, z, 44, G + 11, z, TB)
for z in range(22, 59, 2):                                        # темные рельефы лица
    put(15, G + 3, z, SPIR); put(55, G + 3, z, SPIR)
# свод-череп: V-образный ряд степеней над линиями колон
for s in range(6):
    k = 15 + s
    fill(k, G + 16 + s, 15, 55 - s, G + 16 + s, 65, TSL())
# мозаика и ритуальный ядро
fill(31, G, 30, 35, G, 34, MOS_T)
put(33, G, 32, CORE)
for (cx, cz) in ((29, 28), (37, 28), (29, 36), (37, 36), (33, 27), (33, 37), (28, 32), (38, 32)):
    put(cx, G + 1, cz, CANDLE)
for k in range(28, 39, 2):
    put(k, G, 26, TG); put(k, G, 38, TG)
for k in range(27, 38, 2):
    put(27, G, k, TG); put(39, G, k, TG)
# изумрудный ковровый путь
fill(34, G, 35, 36, G, 63, S("castle_carpet"))
# парапет залив светильники
model("castle_chandelier", 35, G + 14, 24, "south", powered=True)
model("castle_chandelier", 35, G + 14, 44, "south", powered=True)
model("castle_chandelier", 35, G + 14, 56, "south", powered=True)
for z in (20, 26, 40, 46):
    model("castle_bench", 22, G + 1, z, "east")
    model("castle_bench", 48, G + 1, z, "west")
# крипта (как прежде, резерв LaunchCore)
fill(28, G - 6, 28, 38, G - 1, 40, AIR)
fill(28, G - 7, 28, 38, G - 7, 40, S("ashlar"))
for x in (30, 36):
    for z in (31, 35):
        put(x, G - 7, z, S("ashlar")); put(x, G - 6, z, TSL("top"))
model("candle_stand", 28, G - 6, 29, "east", powered=True)
model("candle_stand", 38, G - 6, 29, "west", powered=True)
for k in range(1, 7):
    zz = 40 - k
    put(26, G - k, zz, TBS("north")); fill(26, G - k + 1, zz, 27, G + 2, zz, AIR)
fill(26, G + 1, 40, 27, G + 2, 40, AIR)
fill(25, G - 7, 38, 28, G - 7, 42, S("ashlar"))
fill(31, G - 6, 34, 35, G - 3, 40, AIR)
put(33, G - 7, 37, TG); put(33, G - 6, 37, S("launch_core"))
model("wall_sconce", 30, G - 5, 35, "east", powered=True)

# ================================================================== 7) КУПОЛЬНЫЙ ДРОМ НАД СРЕДОКРЕСТЬЕМ (z 28..38)
walls(30, 28, 40, 38, G + 20, G + 27, t=1, mat=lambda x, y, z: TB)
for x in range(32, 39, 3):
    lancet("x", x, G + 22, 4, 28, []); lancet("x", x, G + 22, 4, 38, [])
for z in range(30, 37, 3):
    lancet("z", z, G + 22, 4, 30, []); lancet("z", z, G + 22, 4, 40, [])
for k in range(6):                                                # луковичная пирамида
    fill(30 + (k // 2), G + 28 + k, 28 + (k // 2), 40 - (k // 2), G + 28 + k, 38 - (k // 2), TB)
fill(35, G + 34, 33, 35, G + 35, 33, TB)
put(35, G + 36, 33, TG)

# ================================================================== 8) КРЫША НЕФА (двуск. конёк x=35)
for s in range(21):
    y = G + 20 + s // 2
    xi = 14 + s
    for z in range(15, 67):
        put(xi, y, z, TBS("east"))
        put(56 - s, y, z, TBS("west"))
for z in range(15, 67):
    put(35, G + 30, z, TG if z % 6 == 2 else TSL())
    put(34, G + 30, z, TSL())

# ================================================================== write
n, pal = write_template(OUT + "/temple_great.nbt", (W, H, D), B, {k: v for k, v in NBT.items() if k in B})
print("temple_great V2", W, H, D, "blocks", n, "palette", pal, "skips", len(SKIPPED))
for k in list(SKIPPED)[:6]:
    print("  SKIP", k)
