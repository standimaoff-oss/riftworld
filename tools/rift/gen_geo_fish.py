#!/usr/bin/env python3
"""Рев 11 — РЫБЫ НА JSON: модели и текстуры, а не рисованные квады «без текстур».

Для каждого из 11 видов пишет:
  * `assets/riftworld/geo/<вид>.json` — скелет из костей и КОРОБОК (как в Blockbench, но в ванильном
    формате ModelPart), с пивотами и покадровой анимацией (хвост машет, плавники трепещут, щупальца колышутся);
  * `assets/riftworld/textures/entity/<вид>.png` — атлас 64x64 (или 128x128 для крупных), раскрашенный
    по РАЗВЁРТКЕ каждой коробки: спина темнее, брюхо светлее, глаза, зубы, полосы, крапины.

Развёртка коробки — ванильная (ModelPart.addBox + texOffs):
    up    : (u+d,      v)        .. (u+d+w,      v+d)
    down  : (u+d+w,    v)        .. (u+2d+w,     v+d)
    east  : (u,        v+d)      .. (u+d,        v+d+h)
    north : (u+d,      v+d)      .. (u+d+w,      v+d+h)
    west  : (u+d+w,    v+d)      .. (u+2d+w,     v+d+h)
    south : (u+2d+w,   v+d)      .. (u+2d+2w,    v+d+h)
"""
import json
import math
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
A = ROOT + "/mod/src/main/resources/assets/riftworld"

# ---------------------------------------------------------------- упаковка UV-прямоугольников
class Atlas:
    """Очень простой полочный упаковщик: коробки кладутся рядами в атлас tw x th."""

    def __init__(self, tw=64, th=64, pad=1):
        self.tw, self.th, self.pad = tw, th, pad
        self.x = pad
        self.y = pad
        self.row_h = 0
        self.ok = True

    def place(self, w, h):
        if self.x + w + self.pad > self.tw:
            self.x = self.pad
            self.y += self.row_h + self.pad
            self.row_h = 0
        if self.y + h + self.pad > self.th:
            self.ok = False
            return 0, 0
        pos = (self.x, self.y)
        self.x += w + self.pad
        self.row_h = max(self.row_h, h)
        return pos


def faces(rect, w, h, d):
    """Шесть прямоугольников развёртки: имя -> (x0,y0,x1,y1)."""
    u, v = rect
    return {
        "up": (u + d, v, u + d + w, v + d),
        "down": (u + d + w, v, u + d + w + w, v + d),
        "east": (u, v + d, u + d, v + d + h),
        "north": (u + d, v + d, u + d + w, v + d + h),
        "west": (u + d + w, v + d, u + d + w + d, v + d + h),
        "south": (u + d + w + d, v + d, u + d + w + d + w, v + d + h),
    }


# ---------------------------------------------------------------- отрисовка кожи
def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def paint_box(px, rect, w, h, d, base, belly, back, role, rnd, spec):
    """Заливает все шесть граней коробки с градиентом спина→брюхо и узором вида."""
    fs = faces(rect, w, h, d)
    # бока: вертикальный градиент (верх — спина, низ — брюхо)
    for side in ("east", "west"):
        x0, y0, x1, y1 = fs[side]
        for y in range(y0, y1):
            t = (y - y0) / max(1, (y1 - y0 - 1))
            col = blend(back, base, min(1.0, t * 1.6)) if t < 0.35 else blend(base, belly, (t - 0.35) / 0.65)
            for x in range(x0, x1):
                px[x, y] = col
    # перед/зад — спина сверху, брюхо снизу
    for face in ("north", "south"):
        x0, y0, x1, y1 = fs[face]
        for y in range(y0, y1):
            t = (y - y0) / max(1, (y1 - y0 - 1))
            col = blend(back, base, min(1.0, t * 1.8)) if t < 0.3 else blend(base, belly, (t - 0.3) / 0.7)
            for x in range(x0, x1):
                px[x, y] = col
    # верх/низ
    for face, col in (("up", back), ("down", belly)):
        x0, y0, x1, y1 = fs[face]
        for y in range(y0, y1):
            for x in range(x0, x1):
                px[x, y] = col if rnd.random() > 0.12 else shade(col, 0.9)

    pattern = spec.get("pattern")
    if role == "body" and pattern:
        for side in ("east", "west"):
            x0, y0, x1, y1 = fs[side]
            for x in range(x0, x1):
                if pattern == "stripes" and (x - x0) % 3 == 0:
                    col = spec["pattern_color"]
                    for y in range(y0, y1):
                        px[x, y] = col
                elif pattern == "bands" and (x - x0) % 5 in (0, 1):
                    for y in range(y0 + 1, y1 - 1):
                        px[x, y] = spec["pattern_color"]
                elif pattern == "spots" and rnd.random() < 0.22:
                    for y in range(y0 + 1, y1 - 1):
                        if rnd.random() < 0.35:
                            px[x, y] = spec["pattern_color"]
        # мелкая чешуя: редкие ровные блики (раньше их было втрое больше — тело «рябило»)
        for side in ("east", "west"):
            x0, y0, x1, y1 = fs[side]
            for _ in range((x1 - x0) * (y1 - y0) // 14):
                x, y = rnd.randrange(x0, x1), rnd.randrange(y0, y1)
                px[x, y] = shade(px[x, y], 1.10)

    if role == "head":
        # глаза на боках
        for side in ("east", "west"):
            x0, y0, x1, y1 = fs[side]
            ex = x0 + max(1, (x1 - x0) // 3)
            ey = y0 + max(1, (y1 - y0) // 3)
            for dx in range(2):
                for dy in range(2):
                    px[ex + dx, ey + dy] = (18, 22, 28)
            px[ex, ey] = (245, 248, 255)
    if role == "mouth":
        # пасть: тёмная полость и зубы
        for face in ("north", "down", "up"):
            if face not in fs:
                continue
            x0, y0, x1, y1 = fs[face]
            for x in range(x0, x1):
                for y in range(y0, y1):
                    px[x, y] = (46, 18, 28) if (x % 2 == 0) else (238, 234, 214)
    if role == "glow":
        for face, (x0, y0, x1, y1) in fs.items():
            for x in range(x0, x1):
                for y in range(y0, y1):
                    px[x, y] = (250, 255, 200) if (x + y) % 2 else (216, 255, 140)
    if role == "shell":
        for face in ("up", "down"):
            x0, y0, x1, y1 = fs[face]
            for x in range(x0, x1):
                for y in range(y0, y1):
                    px[x, y] = spec["pattern_color"] if ((x - x0) // 3 + (y - y0) // 3) % 2 else base


def blend(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


# ---------------------------------------------------------------- виды
def fish_spec(name, body, belly, back, pattern=None, pattern_color=(0, 0, 0), scale=0.72, translucent=False):
    return {
        "name": name, "base": body, "belly": belly, "back": back,
        "pattern": pattern, "pattern_color": pattern_color, "render_scale": scale, "translucent": translucent,
    }


SPECS = {
    "silver": fish_spec("silver", (198, 212, 222), (238, 244, 248), (126, 146, 164), scale=0.75),
    "gold": fish_spec("gold", (233, 194, 90), (249, 233, 172), (168, 128, 46), scale=0.78),
    "striped": fish_spec("striped", (234, 228, 214), (250, 246, 236), (150, 148, 140),
                         pattern="bands", pattern_color=(58, 64, 76), scale=0.72),
    "surgeon": fish_spec("surgeon", (47, 111, 208), (242, 210, 76), (24, 62, 128),
                         pattern="stripes", pattern_color=(18, 30, 58), scale=0.74),
    "jelly": fish_spec("jelly", (159, 230, 255), (216, 247, 255), (108, 190, 226), scale=1.05, translucent=True),
    "ray": fish_spec("ray", (86, 104, 122), (220, 230, 236), (52, 66, 82), pattern="spots",
                     pattern_color=(206, 216, 224), scale=0.85),
    "angler": fish_spec("angler", (74, 104, 128), (176, 196, 210), (38, 56, 74), scale=0.9),
    "eel": fish_spec("eel", (214, 207, 191), (238, 233, 220), (150, 142, 124),
                     pattern="bands", pattern_color=(120, 112, 96), scale=0.9),
    "turtle": fish_spec("turtle", (78, 122, 74), (184, 196, 138), (52, 88, 50),
                        pattern="spots", pattern_color=(94, 138, 86), scale=1.35),
    "gloom": fish_spec("gloom", (76, 54, 98), (168, 126, 196), (36, 24, 50), scale=1.15),
    "sting": fish_spec("sting", (176, 90, 214), (230, 180, 255), (110, 46, 150), scale=1.0, translucent=True),
}


def uv_offset(atlas, w, h, d):
    return atlas.place(2 * d + 2 * w, d + h)


# ---------------------------------------------------------------- сборка моделей
class Builder:
    def __init__(self, name, tw=64, th=64):
        self.name = name
        self.spec = SPECS[name]
        self.atlas = Atlas(tw, th)
        self.bones = []
        self.boxes = []          # (rect, w, h, d, role)

    def bone(self, name, pivot, rotation=(0, 0, 0), parent=None, anim=None):
        b = {"name": name, "pivot": list(pivot)}
        if any(rotation):
            b["rotation"] = list(rotation)
        if parent:
            b["parent"] = parent
        if anim:
            b["anim"] = anim
        b["cubes"] = []
        self.bones.append(b)
        return b

    def cube(self, bone, origin, size, role="body"):
        # ванильный addBox берёт только целые размеры: доли округляем, минимум 1
        w, h, d = (max(1, int(round(v))) for v in size)
        rect = uv_offset(self.atlas, w, h, d)
        if not self.atlas.ok:
            raise RuntimeError(f"{self.name}: атлас 64x64 переполнен (коробка {size})")
        bone["cubes"].append({"origin": list(origin), "size": [w, h, d], "uv": list(rect)})
        self.boxes.append((rect, w, h, d, role))

    def save(self):
        sp = self.spec
        os.makedirs(A + "/geo", exist_ok=True)
        os.makedirs(A + "/textures/entity", exist_ok=True)
        json.dump({
            "texture_width": self.atlas.tw, "texture_height": self.atlas.th,
            "render_scale": sp["render_scale"], "translucent": sp["translucent"],
            "bones": self.bones,
        }, open(f"{A}/geo/{self.name}.json", "w", encoding="utf-8"), ensure_ascii=False, indent=1)

        img = Image.new("RGBA", (self.atlas.tw, self.atlas.th), (0, 0, 0, 0))
        px = img.load()
        rnd = random.Random(sum(map(ord, self.name)))
        for rect, w, h, d, role in self.boxes:
            paint_box(px, rect, w, h, d, sp["base"], sp["belly"], sp["back"], role, rnd, sp)
        img.save(f"{A}/textures/entity/{self.name}.png")
        print(f"  geo/{self.name}.json: {len(self.bones)} костей, {len(self.boxes)} кубов, атлас {self.atlas.tw}x{self.atlas.th}")
        return len(self.bones), len(self.boxes)


# ---------------------------------------------------------------- конкретные виды
def _fin(b, parent, origin, size, role="fin"):
    b.cube(parent, origin, size, role=role)


def build(name, size=64):
    """Рев 13: тела рыб пересобраны «по профилю», а не из одного-двух ящиков.

    Правила, которые вывели по кадрам превью и живой игры:
    * тело — цепочка из 3–6 сегментов, каждый уже предыдущего (силуэт сужается к хвосту);
    * хвост и плавники — отдельные кости, они машут, а не висят;
    * у медуз купол ступенчатый (3 кольца) и «юбка», щупальца — ЦЕПОЧКИ из 3 костей,
      поэтому волна идёт по всей длине, а не одна палка качается вокруг купола;
    * у ската крылья из двух перьев с сужением и хлыст-хвост из трёх костей;
    * у удильщика пасть — тёмная полость, а зубы стоят только по краю, иначе получалось «пианино».
    """
    b = Builder(name, size, size)
    sp = SPECS[name]
    scale = sp["render_scale"]

    def tail_chain(parent_name, pivot, z0, segs, w, h, d, amp, speed, tex="fin", shrink=0.75):
        """Цепочка костей по +Z (хвост): каждая следующая уже и сильнее машет."""
        parent = parent_name
        z = z0
        for i in range(segs):
            nm = f"tailseg{i}"
            pb = b.bone(nm, (0, pivot[1], z), parent=parent,
                        anim={"axis": "y", "amp": amp + i * 4, "speed": speed, "phase": -i * 0.55})
            ww, hh, dd = max(1, int(round(w * shrink ** i))), max(1, int(round(h * shrink ** i))), max(1, int(round(d)))
            b.cube(pb, (-ww / 2, pivot[1] - hh / 2, z), (ww, hh, dd), role=tex)
            parent = nm
            z += d

    if name in ("silver", "gold", "striped", "surgeon"):
        # мирные рыбы: торпеда из четырёх сегментов + хвост-веер + спинной и грудные плавники
        body = b.bone("body", (0, 0, 0))
        b.cube(body, (-2.5, -3.0, -7.0), (5, 6, 5), role="body")      # грудь
        b.cube(body, (-2.2, -2.7, -2.0), (4.4, 5.4, 4), role="body")  # середина
        b.cube(body, (-1.6, -2.1, 2.0), (3.2, 4.2, 3), role="body")   # брюхо к стеблю
        b.cube(body, (-0.9, -1.4, 5.0), (1.8, 2.8, 2), role="body")   # стебель
        head = b.bone("head", (0, 0, -7.0), parent="body")
        b.cube(head, (-2.2, -2.6, -11.0), (4.4, 5.2, 4), role="head")  # голова
        b.cube(head, (-1.3, -1.7, -13.0), (2.6, 3.4, 2), role="mouth") # рыло
        # глаза — отдельными пиксельными коробками по бокам головы
        for sx in (-1, 1):
            b.cube(head, (sx * 2.2 - (0.35 if sx > 0 else 0), 0.1, -9.6), (0.35, 1.2, 1.2), role="eye")
        # хвост: два пера, расходящихся веером
        tail = b.bone("tail", (0, 0, 7.0), parent="body",
                      anim={"axis": "y", "amp": 26, "speed": 1.7, "phase": 0.0})
        b.cube(tail, (-0.5, -2.4, 7.0), (1.0, 4.8, 3.4), role="fin")
        b.cube(tail, (-0.5, 1.4, 9.4), (1.0, 2.2, 2.6), role="fin")
        b.cube(tail, (-0.5, -3.6, 9.4), (1.0, 2.2, 2.6), role="fin")
        dorsal = b.bone("dorsal", (0, -3.0, -1), parent="body",
                        anim={"axis": "x", "amp": 6, "speed": 2.1, "phase": 0.6})
        b.cube(dorsal, (-0.4, 2.6, -6.0), (0.8, 2.4, 7), role="fin")   # спинной (сверху: -Y)
        b.cube(dorsal, (-0.4, -5.2, -2.0), (0.8, 2.0, 4), role="fin")  # киль (снизу: +Y)
        for nm, sx in (("finL", -1), ("finR", 1)):
            fb = b.bone(nm, (sx * 2.5, -0.4, -4.0), parent="body",
                        anim={"axis": "z", "amp": 15 * sx, "speed": 2.4, "phase": 0.0})
            b.cube(fb, (0 if sx > 0 else -3.2, -3.2, -5.0), (3.2, 0.7, 3.2), role="fin")
        pel = b.bone("pelvic", (0, -2.6, 1.5), parent="body",
                     anim={"axis": "x", "amp": 8, "speed": 2.4, "phase": 0.3})
        b.cube(pel, (-0.4, -4.6, 0.5), (0.8, 2.0, 3.0), role="fin")

    elif name in ("jelly", "sting"):
        # Рев 13.1: в координатах МОДЕЛИ +Y — вверх. Купол растёт вверх от нуля,
        # а щупальца идут ВНИЗ (отрицательный Y). В прошлой партии они смотрели вверх.
        bell = b.bone("bell", (0, 0, 0), anim={"axis": "x", "amp": 5, "speed": 1.1, "phase": 0.0})
        # Рев 13.1: купол — ступенчатая СФЕРА, а не «слоёный торт»: каждое кольцо уже и выше,
        # поэтому силуэт круглый, а не плоская тарелка.
        b.cube(bell, (-6.0, 0.0, -6.0), (12.0, 1.2, 12.0), role="body")   # обод
        b.cube(bell, (-5.4, 1.2, -5.4), (10.8, 0.8, 10.8), role="glow")   # светящаяся полоса
        b.cube(bell, (-4.6, 2.0, -4.6), (9.2, 1.2, 9.2), role="body")
        b.cube(bell, (-3.6, 3.2, -3.6), (7.2, 1.2, 7.2), role="glow")
        b.cube(bell, (-2.4, 4.4, -2.4), (4.8, 1.0, 4.8), role="body")
        b.cube(bell, (-1.2, 5.4, -1.2), (2.4, 0.9, 2.4), role="glow")      # макушка
        # ротовые лопасти — вниз от центра, короткие
        for i, (tx, tz) in enumerate(((-1.6, -1.6), (1.6, -1.6), (-1.6, 1.6), (1.6, 1.6))):
            ob = b.bone(f"oral{i}", (tx, 0.0, tz), anim={"axis": "z", "amp": 9, "speed": 1.6,
                                                          "phase": i * 1.1}, parent="bell")
            b.cube(ob, (tx - 0.4, -3.0, tz - 0.4), (0.8, 3.0, 0.8), role="fin")
        # щупальца: цепочка из трёх сегментов ВНИЗ, волна идёт по всей длине
        ring = ((-4.4, -4.4), (4.4, -4.4), (-4.4, 4.4), (4.4, 4.4), (0, 0))
        seg_len = 3 if name == "jelly" else 5
        for i, (tx, tz) in enumerate(ring):
            parent = "bell"
            top = 0.0
            for seg in range(3):
                nm = f"tent{i}_{seg}"
                tb = b.bone(nm, (tx, top, tz), anim={"axis": "x", "amp": 8 + seg * 4, "speed": 1.15,
                                                    "phase": i * 0.6 + seg * 0.9}, parent=parent)
                b.cube(tb, (tx - 0.4, top - seg_len, tz - 0.4), (0.8, seg_len, 0.8), role="fin")
                parent = nm
                top -= seg_len

    elif name == "ray":
        # скат: ромбовидный диск из трёх слоёв + крылья из двух перьев + хлыст из трёх костей
        body = b.bone("body", (0, 0, 0), anim={"axis": "z", "amp": 2, "speed": 1.1, "phase": 0.0})
        b.cube(body, (-5, -0.8, -7), (10, 1.6, 12), role="body")
        b.cube(body, (-3.6, -1.2, -6), (7.2, 0.8, 10), role="body")
        b.cube(body, (-2.2, -1.5, -4), (4.4, 0.6, 7), role="body")        # утолщение диска
        head = b.bone("head", (0, 0, -7), parent="body")
        b.cube(head, (-3.4, -0.8, -10), (6.8, 1.6, 3), role="head")
        b.cube(head, (-2.2, -1.1, -11), (4.4, 0.6, 1), role="mouth")
        for sx in (-1, 1):                                               # глаза — на спине, не на краю
            b.cube(head, (sx * 1.9 - (0.5 if sx > 0 else 0), 0.8, -8.2), (0.9, 0.5, 0.9), role="eye")
        # крылья: два пера, внешнее тоньше и отстаёт по фазе — крыло «дышит»
        for nm, sx in (("wingL", -1), ("wingR", 1)):
            wb = b.bone(nm, (sx * 5, 0, -3), parent="body",
                        anim={"axis": "z", "amp": 11 * sx, "speed": 1.05, "phase": 0.0})
            b.cube(wb, (0 if sx > 0 else -5, -0.6, -4.0), (5, 0.9, 9), role="fin")
            wb2 = b.bone(nm + "tip", (sx * 10, 0, -3), parent=nm,
                         anim={"axis": "z", "amp": 9 * sx, "speed": 1.05, "phase": -0.55})
            b.cube(wb2, (0 if sx > 0 else -4, -0.5, -3.0), (4, 0.7, 6), role="fin")
        # хлыст: три сегмента, сужение
        tail_chain("body", (0, 0, 0), 5, 3, 1.0, 1.0, 5, 8, 1.0, shrink=0.8)

    elif name == "angler":
        body = b.bone("body", (0, 0, 0))
        b.cube(body, (-3.2, -3.2, -2), (6.4, 6.4, 7), role="body")
        b.cube(body, (-2.4, -2.4, 5), (4.8, 4.8, 4), role="body")         # сужение к хвосту
        head = b.bone("head", (0, 0, -2), parent="body")
        b.cube(head, (-4, -4, -7), (8, 8, 5), role="head")
        # пасть: тёмная полость вперёд + зубы ТОЛЬКО по краю (4 сверху, 3 снизу)
        b.cube(head, (-3.4, -1.0, -9.4), (6.8, 2.0, 2.6), role="mouth")
        for i in range(4):
            b.cube(head, (-3.2 + i * 1.7, -0.9, -10.0), (0.7, 1.3, 0.7), role="tooth")
        jaw = b.bone("jaw", (0, -1.0, -8.0), parent="head",
                     anim={"axis": "x", "amp": 8, "speed": 0.9, "phase": 0.0})
        b.cube(jaw, (-3.2, -2.2, -8.8), (6.4, 1.4, 2.2), role="mouth")
        for i in range(3):
            b.cube(jaw, (-2.6 + i * 1.8, -2.4, -9.4), (0.7, 1.1, 0.7), role="tooth")
        for sx in (-1, 1):
            b.cube(head, (sx * 3.4 - (0.4 if sx > 0 else 0), 1.0, -6.4), (0.5, 1.4, 1.4), role="eye")
        # приманка над головой
        lure = b.bone("lure", (0, 4.0, -6.0), parent="head",
                      anim={"axis": "x", "amp": 8, "speed": 1.2, "phase": 0.0})
        b.cube(lure, (-0.3, 4.0, -10.5), (0.6, 0.6, 5), role="fin")
        b.cube(lure, (-1.0, 4.8, -11.6), (2.0, 2.0, 2.0), role="glow")
        b.cube(body, (-0.4, 3.2, -1), (0.8, 2.0, 4), role="fin")
        tail_chain("body", (0, 0, 0), 9, 3, 4.4, 4.4, 2, 20, 1.4, shrink=0.8)

    elif name == "eel":
        # угорь: шесть сегментов, каждый всё тоньше; спинная складка идёт по всей длине
        parent = None
        sizes = [(3.4, 3.4, 5), (3.0, 3.2, 5), (2.6, 2.8, 5), (2.2, 2.4, 5), (1.8, 2.0, 5), (1.4, 1.6, 5)]
        z = -12
        for i, (w, h, d) in enumerate(sizes):
            nm = f"seg{i}"
            pb = b.bone(nm, (0, 0, z), parent=parent,
                        anim={"axis": "y", "amp": 10 + i * 3, "speed": 1.9, "phase": -i * 0.7})
            b.cube(pb, (-w / 2, -h / 2, z), (w, h, d), role="body")
            b.cube(pb, (-0.3, h / 2 - 0.2, z + 0.4), (0.6, 1.2, d - 0.8), role="fin")  # складка сверху
            parent = nm
            z += d
        head = b.bone("head", (0, 0, -12), parent="seg0")
        b.cube(head, (-1.9, -1.8, -16), (3.8, 3.6, 4), role="head")
        b.cube(head, (-1.4, -1.0, -17.4), (2.8, 1.6, 1.6), role="mouth")
        for i in range(4):
            b.cube(head, (-1.4 + i * 0.9, -1.2, -17.6), (0.5, 1.0, 0.5), role="tooth")
        for sx in (-1, 1):
            b.cube(head, (sx * 1.9 - (0.35 if sx > 0 else 0), 0.2, -16.2), (0.35, 0.9, 0.9), role="eye")
        tail_chain("seg5", (0, 0, 0), 18, 2, 3.0, 3.4, 3, 24, 1.9, shrink=0.7)

    elif name == "turtle":
        body = b.bone("body", (0, 0, 0))
        # панцирь: низкая трёхступенчатая гора (а не куб)
        b.cube(body, (-5, 0.2, -5.5), (10, 1.0, 11), role="shell")
        b.cube(body, (-4, 1.2, -4.5), (8, 1.0, 9), role="shell")
        b.cube(body, (-2.5, 2.2, -3.0), (5, 1.0, 6), role="shell")
        b.cube(body, (-1.0, 3.0, -1.5), (2, 0.6, 3), role="shell")
        b.cube(body, (-4, -1.0, -4.5), (8, 1.0, 9), role="body")     # пластрон
        neck = b.bone("neck", (0, 0.4, -5.5), parent="body")
        b.cube(neck, (-1.2, -0.8, -8.0), (2.4, 2.0, 2.5), role="body")
        head = b.bone("head", (0, 0.4, -8.0), parent="neck",
                      anim={"axis": "y", "amp": 7, "speed": 0.45, "phase": 0.0})
        b.cube(head, (-1.8, -1.2, -11.4), (3.6, 3.0, 3.4), role="head")
        b.cube(head, (-1.0, -1.0, -12.2), (2.0, 1.4, 0.8), role="mouth")
        for sx in (-1, 1):
            b.cube(head, (sx * 1.8 - (0.35 if sx > 0 else 0), 0.4, -10.6), (0.35, 1.0, 1.0), role="eye")
        # ласты: два пера каждое (плечо + лопасть), все выступают за панцирь
        for nm, (fx, fz, amp, w, d) in {"fl0": (-5.2, -3.0, 14, 3.4, 3.4), "fl1": (1.8, -3.0, -14, 3.4, 3.4),
                                        "fl2": (-4.6, 2.6, 12, 2.6, 2.6), "fl3": (2.0, 2.6, -12, 2.6, 2.6)}.items():
            fb = b.bone(nm, (fx, -0.4, fz), parent="body",
                        anim={"axis": "y", "amp": amp, "speed": 0.8, "phase": 0.0})
            b.cube(fb, (fx, -1.0, fz), (w, 0.8, d), role="fin")
            tip = b.bone(nm + "tip", (fx + (w if amp > 0 else -w), -0.4, fz), parent=nm,
                         anim={"axis": "y", "amp": amp * 0.6, "speed": 0.8, "phase": -0.5})
            b.cube(tip, (0 if amp > 0 else -2.2, -0.9, fz), (2.2, 0.7, max(2.0, d - 0.8)), role="fin")
        b.cube(body, (-0.6, -0.8, 5.5), (1.2, 1.2, 1.8), role="fin")   # хвостик

    elif name == "gloom":
        body = b.bone("body", (0, 0, 2))
        b.cube(body, (-3.2, -3.2, 0), (6.4, 6.4, 5), role="body")
        b.cube(body, (-2.4, -2.4, 5), (4.8, 4.8, 4), role="body")
        head = b.bone("head", (0, 0, 0), parent="body")
        b.cube(head, (-4.2, -4.2, -6), (8.4, 8.4, 6), role="head")
        b.cube(head, (-4.0, -1.2, -9.2), (8.0, 2.6, 3.2), role="mouth")
        for i in range(4):
            b.cube(head, (-3.6 + i * 2.1, -1.2, -9.8), (0.8, 1.5, 0.8), role="tooth")
        jaw = b.bone("jaw", (0, -1.2, -9.0), parent="head",
                     anim={"axis": "x", "amp": 7, "speed": 0.8, "phase": 0.0})
        b.cube(jaw, (-3.8, -3.0, -9.0), (7.6, 1.4, 2.8), role="mouth")
        for sx in (-1, 1):
            b.cube(head, (sx * 4.2 - (0.4 if sx > 0 else 0), 0.8, -5.2), (0.4, 1.2, 1.2), role="eye")
        lure = b.bone("lure", (0, -4.0, -5), parent="head",
                      anim={"axis": "x", "amp": 10, "speed": 1.0, "phase": 0.3})
        b.cube(lure, (-0.3, 4.2, -10.0), (0.6, 0.6, 5), role="fin")
        b.cube(lure, (-1.0, 5.0, -11.0), (2.0, 2.0, 2.0), role="glow")
        for nm, sx in (("finL", -1), ("finR", 1)):
            fb = b.bone(nm, (sx * 3.2, 0, 1), parent="body",
                        anim={"axis": "z", "amp": 12 * sx, "speed": 1.4, "phase": 0.0})
            b.cube(fb, (0 if sx > 0 else -4.6, -3.6, -0.5), (4.6, 0.8, 3.6), role="fin")
        for i in range(3):                       # биолюминесцентные точки на боках
            b.cube(body, (-3.4 if i % 2 == 0 else 2.6, -1.2 + i, 1.0 + i), (0.8, 0.8, 0.8), role="glow")
        tail_chain("body", (0, 0, 0), 9, 3, 4.4, 5.2, 2, 18, 1.2, shrink=0.8)

    return b.save()


def main():
    total_b, total_c = 0, 0
    for name in SPECS:
        try:
            nb, nc = build(name, 64)
        except RuntimeError:                    # не влезло в 64x64 — берём атлас побольше
            nb, nc = build(name, 128)
        total_b += nb
        total_c += nc
        print(f"  {name}: {nb} костей, {nc} коробок")
    print("видов: %d, костей: %d, коробок: %d" % (len(SPECS), total_b, total_c))


if __name__ == "__main__":
    main()
