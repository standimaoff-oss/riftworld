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
        # мелкая чешуя: светлые точки
        for side in ("east", "west"):
            x0, y0, x1, y1 = fs[side]
            for _ in range((x1 - x0) * (y1 - y0) // 6):
                x, y = rnd.randrange(x0, x1), rnd.randrange(y0, y1)
                px[x, y] = shade(px[x, y], 1.18)

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
def build(name, size=64):
    b = Builder(name, size, size)
    sp = SPECS[name]
    scale = sp["render_scale"]

    if name in ("silver", "gold", "striped", "surgeon"):
        # Тело из ТРЁХ сегментов с сужением к хвосту (как у ванильной трески), а не один куб.
        # Координаты — абсолютные, в «блоках-пикселях»: нос в -Z, хвост в +Z, спина -Y.
        body = b.bone("body", (0, 0, 0))
        b.cube(body, (-2.5, -3.0, -6.0), (5, 6, 5), role="body")        # грудь
        b.cube(body, (-2.0, -2.5, -1.0), (4, 5, 4), role="body")        # середина
        b.cube(body, (-1.2, -1.6, 3.0), (2, 3, 4), role="body")         # хвостовой стебель
        head = b.bone("head", (0, 0, -6.0), parent="body")
        b.cube(head, (-2.0, -2.5, -11.0), (4, 5, 5), role="head")       # голова
        b.cube(head, (-1.2, -1.6, -13.0), (2.4, 3, 2), role="head")     # рыло
        tail = b.bone("tail", (0, 0, 7.0), parent="body",
                      anim={"axis": "y", "amp": 24, "speed": 1.7, "phase": 0.0})
        b.cube(tail, (-0.4, -2.4, 7.0), (0.8, 4.8, 4.4), role="fin")    # хвостовой плавник
        dorsal = b.bone("dorsal", (0, -3.0, -1), parent="body",
                        anim={"axis": "x", "amp": 7, "speed": 2.1, "phase": 0.6})
        b.cube(dorsal, (-0.4, 2.6, -5.5), (0.8, 2.6, 8), role="fin")    # спинной (сверху: -Y)
        b.cube(dorsal, (-0.4, -5.4, -3.0), (0.8, 2.4, 5), role="fin")   # брюшной киль (снизу: +Y)
        for side, px_ in (("finL", -2.5), ("finR", 2.5)):
            fb = b.bone(side, (px_, -0.5, -3.0), parent="body",
                        anim={"axis": "z", "amp": 16 if px_ > 0 else -16, "speed": 2.4, "phase": 0.0})
            b.cube(fb, ((px_ if px_ > 0 else px_ - 3.0), -1.0, -4.5), (3, 0.8, 3.6), role="fin")

    elif name == "jelly":
        bell = b.bone("bell", (0, 4, 0), anim={"axis": "x", "amp": 6, "speed": 1.1, "phase": 0.0})
        b.cube(bell, (-6, -3, -6), (12, 6, 12), role="body")
        b.cube(bell, (-4, -4.5, -4), (8, 2, 8), role="glow")
        for i, (tx, tz) in enumerate(((-3.5, -3.5), (3.5, -3.5), (-3.5, 3.5), (3.5, 3.5), (0, 0))):
            tb = b.bone(f"tentacle{i}", (tx, -3, tz), anim={"axis": "x", "amp": 12 + i * 2, "speed": 1.3,
                                                          "phase": i * 0.7}, parent="bell")
            b.cube(tb, (tx - 0.5, -13, tz - 0.5), (1, 10, 1), role="fin")
        for i, (tx, tz) in enumerate(((-5.5, 0), (5.5, 0), (0, -5.5), (0, 5.5))):
            ob = b.bone(f"oral{i}", (tx, -3, tz), anim={"axis": "z", "amp": 10, "speed": 1.7,
                                                       "phase": i * 1.1}, parent="bell")
            b.cube(ob, (tx - 0.4, -8, tz - 0.4), (0.8, 5, 0.8), role="fin")

    elif name == "ray":
        # скат: плоское тело-диск, ступенчатые «крылья»（силуэт ромба) и длинный хлыст-хвост
        body = b.bone("body", (0, 0, 0), anim={"axis": "z", "amp": 3, "speed": 1.2, "phase": 0.0})
        b.cube(body, (-4, -0.7, -6), (8, 1.4, 12), role="body")
        b.cube(body, (-2.5, -1.1, -4.5), (5, 0.6, 9), role="body")       # утолщение диска
        head = b.bone("head", (0, 0, -6), parent="body")
        b.cube(head, (-3, -0.7, -8.5), (6, 1.4, 3), role="head")
        b.cube(head, (-0.5, 0.6, -9.5), (1, 0.6, 1.6), role="glow")      # «рожки»-электрорецепторы (сверху)
        for side, sx in (("wingL", -1), ("wingR", 1)):
            wb = b.bone(side, (sx * 4, 0, -2), anim={"axis": "z", "amp": 13 * sx, "speed": 1.15, "phase": 0.0},
                        parent="body")
            # два «пера» на каждое крыло: ближнее к телу шире, внешнее уже — ступенчатый край
            b.cube(wb, (0 if sx > 0 else -6, -0.5, -4), (6, 1.0, 8), role="fin")
            b.cube(wb, (0 if sx > 0 else -11, -0.4, -2), (5, 0.8, 5), role="fin")
        # глаза сверху на голове
        b.cube(head, (-2.4, 0.6, -7.6), (1, 0.8, 1), role="head")
        b.cube(head, (1.4, 0.6, -7.6), (1, 0.8, 1), role="head")
        tail = b.bone("tail", (0, 0, 6), parent="body", anim={"axis": "y", "amp": 11, "speed": 1.0, "phase": 0.0})
        b.cube(tail, (-0.4, -0.4, 6), (0.8, 0.8, 16), role="fin")        # хлыст

    elif name == "angler":
        body = b.bone("body", (0, 0, 0))
        b.cube(body, (-3.5, -3.5, -2), (7, 7, 8), role="body")
        head = b.bone("head", (0, 0, -2), parent="body")
        b.cube(head, (-4, -4.5, -7), (8, 8, 5), role="head")
        # пасть — впереди головы, а не внутри неё
        jaw = b.bone("jaw", (0, -0.8, -7), parent="head", anim={"axis": "x", "amp": 9, "speed": 0.9, "phase": 0.0})
        b.cube(jaw, (-3.6, 0.6, -9.6), (7.2, 3.0, 2.6), role="mouth")
        for i in range(4):                                              # клыки по краю пасти
            b.cube(head, (-3.2 + i * 1.9, -0.6, -10.2), (1, 1.8, 1), role="mouth")
        # приманка: стебель идёт ВПЕРЁД от лба, светящийся шарик на конце
        lure = b.bone("lure", (0, 3.6, -7), anim={"axis": "x", "amp": 10, "speed": 1.3, "phase": 0.0}, parent="head")
        b.cube(lure, (-0.3, 3.6, -12.5), (0.6, 0.6, 6), role="fin")
        b.cube(lure, (-1.1, 4.4, -13.6), (2.2, 2.2, 2.2), role="glow")
        tail = b.bone("tail", (0, 0, 6), parent="body", anim={"axis": "y", "amp": 18, "speed": 1.5, "phase": 0.0})
        b.cube(tail, (-2.2, -2.4, 6), (4.4, 4.8, 1), role="fin")
        b.cube(tail, (-0.5, -1.8, 6.6), (1, 3.6, 3), role="fin")
        b.cube(body, (-0.4, 3.5, -1), (0.8, 2.4, 5), role="fin")         # спинной (сверху)

    elif name == "eel":
        # длинное тело: пять сегментов 3x3x5, каждый СВОЙ участок по +Z (раньше все коробки
        # были в z=0 и угорь «схлопывался» в кирпич)
        b.cube(b.bone("body0", (0, 0, -10)), (-1.6, -1.6, -10), (3.2, 3.2, 5), role="body")
        head = b.bone("head", (0, 0, -10), parent="body0")
        b.cube(head, (-1.8, -1.8, -14), (3.6, 3.6, 4), role="head")          # голова впереди
        b.cube(head, (-1.2, -1.4, -15.2), (2.4, 2.2, 1.4), role="mouth")     # пасть
        parent = "body0"
        for i in range(1, 5):
            z0 = -10 + i * 5
            pb = b.bone(f"body{i}", (0, 0, z0), parent=parent,
                        anim={"axis": "y", "amp": 12, "speed": 1.9, "phase": -i * 0.8})
            b.cube(pb, (-1.5, -1.5, z0), (3.0, 3.0, 5), role="body")
            b.cube(pb, (-0.3, 1.4, z0 + 0.4), (0.6, 1.2, 4.2), role="fin")    # спинная складка (сверху)
            parent = f"body{i}"
        tail = b.bone("tail", (0, 0, 15), parent="body4", anim={"axis": "y", "amp": 26, "speed": 1.9, "phase": -4.2})
        b.cube(tail, (-0.4, -2.2, 15), (0.8, 4.4, 3.2), role="fin")

    elif name == "turtle":
        body = b.bone("body", (0, 0, 0))
        # купол панциря: три ступени, каждая уже предыдущей — силуэт черепахи, а не кирпич
        b.cube(body, (-5, 0.4, -5), (10, 1, 10), role="shell")
        b.cube(body, (-4, 1.4, -4), (8, 1, 8), role="shell")
        b.cube(body, (-2, 2.4, -2), (4, 1, 4), role="shell")
        # пластрон снизу
        b.cube(body, (-4, -1.2, -4), (8, 1, 8), role="body")
        # голова вынесена вперёд на шее
        neck = b.bone("neck", (0, 0.2, -5), parent="body")
        b.cube(neck, (-1.2, -0.8, -7.5), (2.4, 2.0, 2.5), role="body")
        head = b.bone("head", (0, 0.2, -7.5), parent="neck",
                      anim={"axis": "y", "amp": 9, "speed": 0.5, "phase": 0.0})
        b.cube(head, (-1.8, -1.3, -11), (3.6, 3.0, 3.6), role="head")
        b.cube(head, (-1.0, -1.0, -11.8), (2.0, 1.6, 1.0), role="mouth")
        # ласты: передние длинные, задние короткие, все ВЫСТУПАЮТ за панцирь
        for nm, (fx, fz, w, d, amp) in {"fl0": (-6.5, -3.5, 3.0, 4.0, 18), "fl1": (3.5, -3.5, 3.0, 4.0, -18),
                                        "fl2": (-5.5, 3.0, 2.0, 3.0, 16), "fl3": (3.5, 3.0, 2.0, 3.0, -16)}.items():
            fb = b.bone(nm, (fx, -0.5, fz), anim={"axis": "y", "amp": amp, "speed": 0.9, "phase": 0.0},
                        parent="body")
            b.cube(fb, (fx, -2.2, fz), (w, 1.0, d), role="fin")
        b.cube(body, (-0.6, -1.2, 5), (1.2, 1.2, 2.0), role="fin")            # хвостик

    elif name == "gloom":
        # глубоководный хищник: узкое тело, огромная голова с пастью ВПЕРЕДИ (раньше пасть была
        # внутри головы, а зубы торчали из боков — получался фиолетовый ящик)
        body = b.bone("body", (0, 0, 2))
        b.cube(body, (-3.5, -3.5, 0), (7, 7, 5), role="body")
        b.cube(body, (-2.5, -2.5, 5), (5, 5, 4), role="body")             # сужение к хвосту
        head = b.bone("head", (0, 0, 0), parent="body")
        b.cube(head, (-4.5, -4.5, -6), (9, 9, 6), role="head")
        # верхняя челюсть и нижняя с зубами — обе впереди головы
        b.cube(head, (-4.5, -0.2, -9), (9, 1.6, 3.4), role="mouth")       # верхняя пасть
        jaw = b.bone("jaw", (0, -1.8, -9), parent="head", anim={"axis": "x", "amp": 8, "speed": 0.8, "phase": 0.0})
        b.cube(jaw, (-4.0, -3.4, -9), (8, 1.6, 3.0), role="mouth")        # нижняя челюсть
        for i in range(4):                                                # клыки верхней челюсти
            b.cube(head, (-3.6 + i * 2.2, -1.6, -9.4), (1, 1.4, 1), role="mouth")
        for i in range(4):                                                # клыки нижней
            b.cube(jaw, (-3.6 + i * 2.2, -2.4, -9.4), (1, 1.4, 1), role="mouth")
        # приманка на стебле вперёд-вверх
        lure = b.bone("lure", (0, -4.5, -5), anim={"axis": "x", "amp": 12, "speed": 1.0, "phase": 0.3},
                      parent="head")
        b.cube(lure, (-0.3, 4.4, -10.5), (0.6, 0.6, 5.5), role="fin")
        b.cube(lure, (-1.1, 5.2, -11.6), (2.2, 2.2, 2.2), role="glow")
        # боковые плавники-крылья и хвост
        for side, sx in (("finL", -1), ("finR", 1)):
            fb = b.bone(side, (sx * 3.5, 0, 1), anim={"axis": "z", "amp": 13 * sx, "speed": 1.4, "phase": 0.0},
                        parent="body")
            b.cube(fb, (0 if sx > 0 else -5, -0.8, -1), (5, 1.0, 4), role="fin")
        tail = b.bone("tail", (0, 0, 9), parent="body", anim={"axis": "y", "amp": 16, "speed": 1.2, "phase": 0.0})
        b.cube(tail, (-2.2, -2.8, 9), (4.4, 5.6, 1), role="fin")
        b.cube(tail, (-0.5, -1.8, 9.6), (1, 3.6, 2.6), role="fin")
        # светящиеся точки на боках (биолюминесценция)
        for i in range(3):
            b.cube(body, (-3.7 if i % 2 == 0 else 2.9, -1.5 + i, 1.0 + i), (0.8, 0.8, 0.8), role="glow")

    elif name == "sting":
        bell = b.bone("bell", (0, 3, 0), anim={"axis": "x", "amp": 7, "speed": 1.0, "phase": 0.0})
        b.cube(bell, (-5.5, -3, -5.5), (11, 5.5, 11), role="body")
        b.cube(bell, (-3.5, -4.2, -3.5), (7, 2, 7), role="glow")
        for i, (tx, tz) in enumerate(((-4, -4), (4, -4), (-4, 4), (4, 4), (-1.5, 0), (1.5, 0))):
            tb = b.bone(f"tentacle{i}", (tx, -3, tz), anim={"axis": "x", "amp": 15, "speed": 1.1,
                                                          "phase": i * 0.9}, parent="bell")
            b.cube(tb, (tx - 0.5, -18, tz - 0.5), (1, 15, 1), role="fin")

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
