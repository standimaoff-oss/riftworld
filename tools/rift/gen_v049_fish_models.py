#!/usr/bin/env python3
"""
v0.49.0 — «улучшенные» модели рыб.

Пользователь попросил улучшить ВСЕ модели рыб, включая те, у которых не было
ванильных яиц призыва.

Рев 13 (tools/rift/gen_geo_fish.py) собрал тела «по профилю» из 3–6 сегментов,
но крупные виды (серебряная, золотая, полосатая, хирург) оставались слишком
гладкими: одна чешуйка-коробка на сегмент, без жаберных крышек, без обруча
челюсти, без лучей на плавниках.

Здесь каждая рыба дорабатывается:
  * ЖАБЕРНЫЕ КРЫШКИ — отдельная коробка на каждой стороне головы + тёмная
    жаберная щель;
  * ГУБЫ/ЧЕЛЮСТЬ — ободок пасти, а не просто тёмная дыра;
  * ЛУЧИ ПЛАВНИКОВ — тонкие полоски-рёбра на спинном, хвостовом и грудных;
  * БРЮШНЫЕ ЧЕШУЙКИ — отдельная светлая полоса вдоль живота;
  * СПИННАЯ ПЛАСТИНКА/КРЕСТЕЦ — переход от головы к телу;
  * ГЛАЗ — с выпуклым белком и тёмным зрачком (а не плоский чёрный квадрат).

Атлас увеличен до 128x128, чтобы всё это влезло.
"""
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import gen_geo_fish as G  # noqa: E402


def _gill_cover(b, bone, head_z, w, h, y_top=None, y_bot=None):
    """Жаберная крышка + щель на каждой стороне головы."""
    for sx in (-1, 1):
        x = sx * w / 2 - (0.2 if sx > 0 else 0)
        b.cube(bone, (x, (y_bot if y_bot is not None else -h / 2), head_z),
               (0.3, h * 0.62, h * 0.52), role="body")
        # тёмная щель за крышкой
        b.cube(bone, (x - (0.05 if sx > 0 else 0), (y_bot if y_bot is not None else -h / 2) + h * 0.16,
                     head_z + h * 0.52), (0.25, h * 0.22, h * 0.16), role="mouth")


def _fin_rays(b, bone, x, y, z, w, h, d, count=3, axis="x"):
    """Тонкие лучи на плавнике — плавник перестаёт быть «лопатой»."""
    for i in range(count):
        t = (i + 1) / (count + 1)
        if axis == "x":
            b.cube(bone, (x + (w - 0.6) * t, y, z + d * 0.15), (0.3, max(0.6, h * 0.8), d * 0.7), role="fin")
        else:
            b.cube(bone, (x + w * 0.15, y + (h - 0.6) * t, z + d * 0.15), (w * 0.7, 0.3, d * 0.7), role="fin")


def _belly_scales(b, bone, z0, z1, w):
    """Светлая чешуйная полоса вдоль живота."""
    z = z0
    while z < z1:
        seg = min(2.0, z1 - z)
        b.cube(bone, (-w * 0.36, -w * 0.40, z), (w * 0.72, 0.25, max(0.6, seg - 0.4)), role="belly")
        z += 2.0


def _lip_ring(b, bone, w, h, z):
    """Ободок пасти: светлая рамка вокруг рта."""
    b.cube(bone, (-w / 2, h / 2 - 0.5, z), (w, 0.4, 0.5), role="belly")
    b.cube(bone, (-w / 2, -h / 2, z), (w, 0.4, 0.5), role="belly")


def _bulging_eye(b, bone, x, y, z, r=1.2):
    """Выпуклый глаз: белок + зрачок."""
    b.cube(bone, (x, y, z), (r * 0.8, r, r), role="eye")
    b.cube(bone, (x + (r * 0.5 if x > 0 else -r * 0.2), y + r * 0.25, z - r * 0.5),
           (r * 0.4, r * 0.4, r * 0.4), role="mouth")


# ---------------------------------------------------------------- перекрытие build
def build_v049(name, size=128):
    b = G.Builder(name, size, size)
    sp = G.SPECS[name]

    def tail_chain(parent_name, pivot, z0, segs, w, h, d, amp, speed, tex="fin", shrink=0.75):
        parent = parent_name
        z = z0
        for i in range(segs):
            nm = f"tailseg{i}"
            pb = b.bone(nm, (0, pivot[1], z), parent=parent,
                        anim={"axis": "y", "amp": amp + i * 4, "speed": speed, "phase": -i * 0.55})
            ww, hh, dd = (max(1, int(round(w * shrink ** i))),
                          max(1, int(round(h * shrink ** i))),
                          max(1, int(round(d))))
            b.cube(pb, (-ww / 2, pivot[1] - hh / 2, z), (ww, hh, dd), role=tex)
            parent = nm
            z += d

    if name in ("silver", "gold", "striped", "surgeon"):
        # ---------------- мирная рыба, полностью пересобранная
        body = b.bone("body", (0, 0, 0))
        b.cube(body, (-2.6, -3.2, -7.4), (5.2, 6.4, 5.2), role="body")       # грудь
        b.cube(body, (-2.4, -2.9, -2.4), (4.8, 5.8, 4.4), role="body")       # середина
        b.cube(body, (-1.8, -2.3, 1.8), (3.6, 4.6, 3.2), role="body")        # брюхо к стеблю
        b.cube(body, (-1.0, -1.6, 4.8), (2.0, 3.2, 2.2), role="body")       # стебель
        _belly_scales(b, body, -6.6, 4.2, 4.2)

        head = b.bone("head", (0, 0, -7.4), parent="body")
        b.cube(head, (-2.4, -2.8, -11.4), (4.8, 5.6, 4.2), role="head")
        b.cube(head, (-1.4, -1.9, -13.4), (2.8, 3.6, 2.0), role="mouth")     # рыло
        _lip_ring(b, head, 2.8, 3.6, -13.4)
        _gill_cover(b, head, -9.4, 4.8, 5.6)
        # крестец — переход от головы к телу
        b.cube(head, (-2.2, -2.6, -7.6), (4.4, 5.2, 1.0), role="body")
        for sx in (-1, 1):
            _bulging_eye(b, head, sx * 2.4 - (0.45 if sx > 0 else 0), 0.9, -10.2, 1.25)

        # хвост: веер из трёх перьев + лучи
        tail = b.bone("tail", (0, 0, 7.0), parent="body",
                      anim={"axis": "y", "amp": 26, "speed": 1.7, "phase": 0.0})
        b.cube(tail, (-0.55, -2.6, 7.0), (1.1, 5.2, 3.6), role="fin")
        b.cube(tail, (-0.55, 1.4, 9.6), (1.1, 2.4, 2.8), role="fin")
        b.cube(tail, (-0.55, -3.8, 9.6), (1.1, 2.4, 2.8), role="fin")
        _fin_rays(b, tail, -0.55, -3.6, 9.6, 1.1, 2.4, 2.8, count=3, axis="y")

        # спинной плавник: основание + лучи + киль
        dorsal = b.bone("dorsal", (0, -3.0, -1), parent="body",
                        anim={"axis": "x", "amp": 6, "speed": 2.1, "phase": 0.6})
        b.cube(dorsal, (-0.45, 2.4, -6.2), (0.9, 2.6, 7.2), role="fin")
        _fin_rays(b, dorsal, -0.45, 2.4, -6.2, 0.9, 2.6, 7.2, count=4, axis="z")
        b.cube(dorsal, (-0.45, -5.4, -2.0), (0.9, 2.2, 4.2), role="fin")     # киль
        # второй (жировой) спинной плавник — у настоящей рыбы он есть
        dorsal2 = b.bone("dorsal2", (0, -2.2, 4.2), parent="body",
                         anim={"axis": "x", "amp": 8, "speed": 2.3, "phase": 1.0})
        b.cube(dorsal2, (-0.4, 1.8, 3.6), (0.8, 2.0, 2.4), role="fin")

        # грудные плавники: два пера + лучи
        for nm, sx in (("finL", -1), ("finR", 1)):
            fb = b.bone(nm, (sx * 2.7, -0.4, -4.4), parent="body",
                        anim={"axis": "z", "amp": 15 * sx, "speed": 2.4, "phase": 0.0})
            b.cube(fb, (0 if sx > 0 else -3.4, -3.4, -5.2), (3.4, 0.8, 3.4), role="fin")
            b.cube(fb, (0 if sx > 0 else -2.4, -4.0, -4.2), (2.4, 0.7, 2.4), role="fin")
            _fin_rays(b, fb, 0 if sx > 0 else -3.4, -3.4, -5.2, 3.4, 0.8, 3.4, count=3, axis="z")
        # брюшные плавники
        pel = b.bone("pelvic", (0, -2.8, 1.5), parent="body",
                     anim={"axis": "x", "amp": 8, "speed": 2.4, "phase": 0.3})
        b.cube(pel, (-0.45, -4.8, 0.4), (0.9, 2.2, 3.2), role="fin")
        # анальный плавник
        anal = b.bone("anal", (0, -2.4, 3.4), parent="body",
                      anim={"axis": "x", "amp": 7, "speed": 2.2, "phase": 0.8})
        b.cube(anal, (-0.4, -4.4, 3.0), (0.8, 2.0, 2.4), role="fin")

    elif name in ("jelly", "sting"):
        # ---------------- медуза: колокол + венчик + щупальца
        bell = b.bone("bell", (0, 0, 0), anim={"axis": "x", "amp": 5, "speed": 1.1, "phase": 0.0})
        b.cube(bell, (-6.0, 0.0, -6.0), (12.0, 1.2, 12.0), role="body")
        b.cube(bell, (-5.4, 1.2, -5.4), (10.8, 0.8, 10.8), role="glow")
        b.cube(bell, (-4.6, 2.0, -4.6), (9.2, 1.2, 9.2), role="body")
        b.cube(bell, (-3.6, 3.2, -3.6), (7.2, 1.2, 7.2), role="glow")
        b.cube(bell, (-2.4, 4.4, -2.4), (4.8, 1.0, 4.8), role="body")
        b.cube(bell, (-1.2, 5.4, -1.2), (2.4, 0.9, 2.4), role="glow")
        # венчик по краю колокола — «зубцы» бахромы
        for i in range(8):
            a = i * (3.14159 * 2 / 8)
            cx, cz = 5.2 * (a and 1) * 0, 0
            x = 5.2 * (1 if i else 0)
            # круговой обход: 8 точек по краю
            import math
            x = 5.4 * math.cos(a)
            z = 5.4 * math.sin(a)
            b.cube(bell, (x - 0.5, -1.2, z - 0.5), (1.0, 1.4, 1.0), role="fin")
        for i, (tx, tz) in enumerate(((-1.6, -1.6), (1.6, -1.6), (-1.6, 1.6), (1.6, 1.6))):
            ob = b.bone(f"oral{i}", (tx, 0.0, tz), anim={"axis": "z", "amp": 9, "speed": 1.6, "phase": i * 1.1},
                        parent="bell")
            b.cube(ob, (tx - 0.4, -3.0, tz - 0.4), (0.8, 3.0, 0.8), role="fin")
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
        # ---------------- скат: диск + крылья + хлыст
        body = b.bone("body", (0, 0, 0), anim={"axis": "z", "amp": 2, "speed": 1.1, "phase": 0.0})
        b.cube(body, (-5, -0.8, -7), (10, 1.6, 12), role="body")
        b.cube(body, (-3.6, -1.2, -6), (7.2, 0.8, 10), role="body")
        b.cube(body, (-2.2, -1.5, -4), (4.4, 0.6, 7), role="body")
        _belly_scales(b, body, -6.0, 4.0, 6.0)
        head = b.bone("head", (0, 0, -7), parent="body")
        b.cube(head, (-3.4, -0.8, -10), (6.8, 1.6, 3), role="head")
        b.cube(head, (-2.2, -1.1, -11), (4.4, 0.6, 1), role="mouth")
        for sx in (-1, 1):
            _bulging_eye(b, head, sx * 1.9 - (0.5 if sx > 0 else 0), 0.8, -8.2, 0.9)
        for nm, sx in (("wingL", -1), ("wingR", 1)):
            wb = b.bone(nm, (sx * 5, 0, -3), parent="body",
                        anim={"axis": "z", "amp": 11 * sx, "speed": 1.05, "phase": 0.0})
            b.cube(wb, (0 if sx > 0 else -5, -0.6, -4.0), (5, 0.9, 9), role="fin")
            wb2 = b.bone(nm + "tip", (sx * 10, 0, -3), parent=nm,
                         anim={"axis": "z", "amp": 9 * sx, "speed": 1.05, "phase": -0.55})
            b.cube(wb2, (0 if sx > 0 else -4, -0.5, -3.0), (4, 0.7, 6), role="fin")
        tail_chain("body", (0, 0, 0), 5, 3, 1.0, 1.0, 5, 8, 1.0, shrink=0.8)

    elif name == "angler":
        # ---------------- удильщик: зубастая пасть, жабры, приманка
        body = b.bone("body", (0, 0, 0))
        b.cube(body, (-3.2, -3.2, -2), (6.4, 6.4, 7), role="body")
        b.cube(body, (-2.4, -2.4, 5), (4.8, 4.8, 4), role="body")
        _belly_scales(b, body, -1.0, 4.0, 4.8)
        head = b.bone("head", (0, 0, -2), parent="body")
        b.cube(head, (-4, -4, -7), (8, 8, 5), role="head")
        b.cube(head, (-3.4, -1.0, -9.4), (6.8, 2.0, 2.6), role="mouth")
        for i in range(4):
            b.cube(head, (-3.2 + i * 1.7, -0.9, -10.0), (0.7, 1.3, 0.7), role="tooth")
        jaw = b.bone("jaw", (0, -1.0, -8.0), parent="head",
                     anim={"axis": "x", "amp": 8, "speed": 0.9, "phase": 0.0})
        b.cube(jaw, (-3.2, -2.2, -8.8), (6.4, 1.4, 2.2), role="mouth")
        for i in range(3):
            b.cube(jaw, (-2.6 + i * 1.8, -2.4, -9.4), (0.7, 1.1, 0.7), role="tooth")
        for sx in (-1, 1):
            _bulging_eye(b, head, sx * 3.4 - (0.5 if sx > 0 else 0), 1.0, -6.4, 1.3)
        _gill_cover(b, head, -4.4, 8.0, 8.0)
        lure = b.bone("lure", (0, 4.0, -6.0), parent="head",
                      anim={"axis": "x", "amp": 8, "speed": 1.2, "phase": 0.0})
        b.cube(lure, (-0.3, 4.0, -10.5), (0.6, 0.6, 5), role="fin")
        b.cube(lure, (-1.0, 4.8, -11.6), (2.0, 2.0, 2.0), role="glow")
        b.cube(body, (-0.4, 3.2, -1), (0.8, 2.0, 4), role="fin")
        tail_chain("body", (0, 0, 0), 9, 3, 4.4, 4.4, 2, 20, 1.4, shrink=0.8)

    elif name == "eel":
        # ---------------- угорь: сегменты, складка, жабры, зубы
        parent = None
        sizes = [(3.4, 3.4, 5), (3.0, 3.2, 5), (2.6, 2.8, 5), (2.2, 2.4, 5), (1.8, 2.0, 5), (1.4, 1.6, 5)]
        z = -12
        for i, (w, h, d) in enumerate(sizes):
            nm = f"seg{i}"
            pb = b.bone(nm, (0, 0, z), parent=parent,
                        anim={"axis": "y", "amp": 10 + i * 3, "speed": 1.9, "phase": -i * 0.7})
            b.cube(pb, (-w / 2, -h / 2, z), (w, h, d), role="body")
            b.cube(pb, (-0.3, h / 2 - 0.2, z + 0.4), (0.6, 1.2, d - 0.8), role="fin")
            parent = nm
            z += d
        head = b.bone("head", (0, 0, -12), parent="seg0")
        b.cube(head, (-1.9, -1.8, -16), (3.8, 3.6, 4), role="head")
        b.cube(head, (-1.4, -1.0, -17.4), (2.8, 1.6, 1.6), role="mouth")
        for i in range(4):
            b.cube(head, (-1.4 + i * 0.9, -1.2, -17.6), (0.5, 1.0, 0.5), role="tooth")
        for sx in (-1, 1):
            _bulging_eye(b, head, sx * 1.9 - (0.4 if sx > 0 else 0), 0.2, -16.2, 0.9)
        _gill_cover(b, head, -14.0, 3.8, 3.6)
        tail_chain("seg5", (0, 0, 0), 18, 2, 3.0, 3.4, 3, 24, 1.9, shrink=0.7)

    elif name == "turtle":
        # ---------------- черепаха: рёбра панциря, борозды, лапы
        body = b.bone("body", (0, 0, 0))
        b.cube(body, (-5, 0.2, -5.5), (10, 1.0, 11), role="shell")
        b.cube(body, (-4, 1.2, -4.5), (8, 1.0, 9), role="shell")
        b.cube(body, (-2.5, 2.2, -3.0), (5, 1.0, 6), role="shell")
        b.cube(body, (-1.0, 3.0, -1.5), (2, 0.6, 3), role="shell")
        # рёбра панциря — тонкие валики поверх
        for i in range(5):
            z = -4.4 + i * 2.0
            b.cube(body, (-4.2, 1.2, z), (8.4, 0.25, 0.3), role="body")
        b.cube(body, (-0.2, 2.2, -3.0), (0.4, 0.25, 5.6), role="body")
        b.cube(body, (-4, -1.0, -4.5), (8, 1.0, 9), role="body")
        neck = b.bone("neck", (0, 0.4, -5.5), parent="body")
        b.cube(neck, (-1.2, -0.8, -8.0), (2.4, 2.0, 2.5), role="body")
        head = b.bone("head", (0, 0.4, -8.0), parent="neck",
                      anim={"axis": "y", "amp": 7, "speed": 0.45, "phase": 0.0})
        b.cube(head, (-1.8, -1.2, -11.4), (3.6, 3.0, 3.4), role="head")
        b.cube(head, (-1.0, -1.0, -12.2), (2.0, 1.4, 0.8), role="mouth")
        for sx in (-1, 1):
            _bulging_eye(b, head, sx * 1.8 - (0.4 if sx > 0 else 0), 0.4, -10.6, 1.0)
        for nm, (fx, fz, amp, w, d) in {"fl0": (-5.2, -3.0, 14, 3.4, 3.4), "fl1": (1.8, -3.0, -14, 3.4, 3.4),
                                        "fl2": (-4.6, 2.6, 12, 2.6, 2.6), "fl3": (2.0, 2.6, -12, 2.6, 2.6)}.items():
            fb = b.bone(nm, (fx, -0.4, fz), parent="body",
                        anim={"axis": "y", "amp": amp, "speed": 0.8, "phase": 0.0})
            b.cube(fb, (fx, -1.0, fz), (w, 0.8, d), role="fin")
            # когти на лапе
            for k in range(3):
                b.cube(fb, (fx + w * 0.4, -1.4, fz - 1.2 + k * 1.2), (0.5, 0.5, 0.8), role="tooth")
            tip = b.bone(nm + "tip", (fx + (w if amp > 0 else -w), -0.4, fz), parent=nm,
                         anim={"axis": "y", "amp": amp * 0.6, "speed": 0.8, "phase": -0.5})
            b.cube(tip, (0 if amp > 0 else -2.2, -0.9, fz), (2.2, 0.7, max(2.0, d - 0.8)), role="fin")
        b.cube(body, (-0.6, -0.8, 5.5), (1.2, 1.2, 1.8), role="fin")

    elif name == "gloom":
        # ---------------- глумнорыб: биолюминесценция, жабры, зубы
        body = b.bone("body", (0, 0, 2))
        b.cube(body, (-3.2, -3.2, 0), (6.4, 6.4, 5), role="body")
        b.cube(body, (-2.4, -2.4, 5), (4.8, 4.8, 4), role="body")
        _belly_scales(b, body, 0.0, 4.0, 4.8)
        head = b.bone("head", (0, 0, 0), parent="body")
        b.cube(head, (-4.2, -4.2, -6), (8.4, 8.4, 6), role="head")
        b.cube(head, (-4.0, -1.2, -9.2), (8.0, 2.6, 3.2), role="mouth")
        for i in range(4):
            b.cube(head, (-3.6 + i * 2.1, -1.2, -9.8), (0.8, 1.5, 0.8), role="tooth")
        jaw = b.bone("jaw", (0, -1.2, -9.0), parent="head",
                     anim={"axis": "x", "amp": 7, "speed": 0.8, "phase": 0.0})
        b.cube(jaw, (-3.8, -3.0, -9.0), (7.6, 1.4, 2.8), role="mouth")
        for sx in (-1, 1):
            _bulging_eye(b, head, sx * 4.2 - (0.5 if sx > 0 else 0), 0.8, -5.2, 1.2)
        _gill_cover(b, head, -3.2, 8.4, 8.4)
        lure = b.bone("lure", (0, -4.0, -5), parent="head",
                      anim={"axis": "x", "amp": 10, "speed": 1.0, "phase": 0.3})
        b.cube(lure, (-0.3, 4.2, -10.0), (0.6, 0.6, 5), role="fin")
        b.cube(lure, (-1.0, 5.0, -11.0), (2.0, 2.0, 2.0), role="glow")
        for nm, sx in (("finL", -1), ("finR", 1)):
            fb = b.bone(nm, (sx * 3.2, 0, 1), parent="body",
                        anim={"axis": "z", "amp": 12 * sx, "speed": 1.4, "phase": 0.0})
            b.cube(fb, (0 if sx > 0 else -4.6, -3.6, -0.5), (4.6, 0.8, 3.6), role="fin")
            _fin_rays(b, fb, 0 if sx > 0 else -4.6, -3.6, -0.5, 4.6, 0.8, 3.6, count=3, axis="z")
        for i in range(4):
            b.cube(body, (-3.4 if i % 2 == 0 else 2.6, -1.2 + i, 1.0 + i), (0.8, 0.8, 0.8), role="glow")
        tail_chain("body", (0, 0, 0), 9, 3, 4.4, 5.2, 2, 18, 1.2, shrink=0.8)

    return b.save()


def main():
    total_b = total_c = 0
    for name in G.SPECS:
        try:
            nb, nc = build_v049(name, 128)
        except RuntimeError:
            nb, nc = build_v049(name, 256)
        total_b += nb
        total_c += nc
        print(f"  {name}: {nb} костей, {nc} коробок")
    print("видов: %d, костей: %d, коробок: %d" % (len(G.SPECS), total_b, total_c))


if __name__ == "__main__":
    main()
