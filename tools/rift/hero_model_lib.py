#!/usr/bin/env python3
"""
hero_model_lib — общий сборщик «витринных» моделей мобов для Riftworld.

Модели собираются в удобной системе координат (Y растёт ВВЕРХ, 0 — земля под
лапами / осевая линия туловища, нос смотрит в -Z), а для игры выгружаются с
`flip_y=True`, потому что рендер Minecraft/Forge считает +Y вниз.

Что даёт библиотека:
  * скелет из костей с parent/anim и кубоидов с материалом и узором;
  * skyline-упаковку честных ванильных UV-островков без наложений;
  * процедурный атлас 1024²: чешуя, яблоки, пряди, бородки перьев, стёжка,
    золотой кант, блики на рёбрах кубоидов, мягкий шум и градиент;
  * экспорт в три формата: игровой `assets/riftworld/geo/*.json`,
    `*.blockbench.geo.json` (Bedrock geometry, импортируется в Blockbench),
    и самодостаточный `*.bbmodel` с встроенной текстурой;
  * самопроверку UV, размеров, родителей и габаритов.

Авторы моделей: tools/rift/gen_v041_liopleurodon_model.py и
tools/rift/gen_v041_pegasus_model.py. Зависимостей, кроме stdlib, нет.
"""
from __future__ import annotations

import base64
import json
import math
import pathlib
import struct
import uuid
import zlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
ASSETS = ROOT / "mod/src/main/resources/assets/riftworld"
REPORTS = ROOT / "reports/hero_models"
PAD = 1


def hx(s: str):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def clamp(v):
    return max(0, min(255, int(v)))


def rnd(seed: int):
    st = seed & 0xFFFFFFFF

    def nxt():
        nonlocal st
        st = (1103515245 * st + 12345) & 0x7FFFFFFF
        return st / 0x7FFFFFFF
    return nxt


def stable_seed(*parts):
    return zlib.crc32("|".join(str(p) for p in parts).encode("utf-8")) & 0xFFFFFFFF


def make_uuid(*parts):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, "riftworld/" + "/".join(str(p) for p in parts)))


def footprint(w: int, h: int, d: int):
    """Ванильная развёртка коробки: ширина 2d+2w, высота d+h."""
    return 2 * d + 2 * w, d + h


def face_rects(u, v, w, h, d):
    return {
        "up": (u + d, v, w, d),
        "down": (u + d + w, v, w, d),
        "east": (u, v + d, d, h),
        "north": (u + d, v + d, w, h),
        "west": (u + d + w, v + d, d, h),
        "south": (u + d + w + d, v + d, w, h),
    }


class Cube:
    __slots__ = ("origin", "size", "mat", "pattern", "name", "bone", "u", "v")

    def __init__(self, origin, size, mat, pattern, name, bone, y_stretch=1.0):
        self.origin = [round(float(origin[0]), 2),
                       round(float(origin[1]) * y_stretch, 2),
                       round(float(origin[2]), 2)]
        self.size = [max(1, int(round(float(size[0])))),
                     max(1, int(round(float(size[1]) * y_stretch))),
                     max(1, int(round(float(size[2]))))]
        self.mat = mat
        self.pattern = pattern
        self.name = name
        self.bone = bone
        self.u = 0
        self.v = 0


class HeroRig:
    def __init__(self, name: str, atlas: int = 1024, render_scale: float = 1.0,
                 y_stretch: float = 1.0):
        self.name = name
        self.atlas = atlas
        self.render_scale = render_scale
        self.y_stretch = y_stretch
        self.bones = []
        self.by_name = {}
        self.current = None

    def bone(self, name, pivot, anim=None, parent=None):
        if name in self.by_name:
            raise ValueError(f"дубликат кости: {name}")
        if parent is not None and parent not in self.by_name:
            raise ValueError(f"кость {name}: родитель {parent} ещё не объявлен")
        b = {
            "name": name,
            "pivot": [round(float(pivot[0]), 2),
                      round(float(pivot[1]) * self.y_stretch, 2),
                      round(float(pivot[2]), 2)],
            "anim": anim,
            "parent": parent,
            "cubes": [],
        }
        self.bones.append(b)
        self.by_name[name] = b
        self.current = b
        return self

    def cube(self, origin, size, mat, pattern=None, name=None, bone=None):
        target = self.by_name[bone] if bone else self.current
        if target is None:
            raise ValueError("кубоид добавлен до объявления кости")
        cb = Cube(origin, size, mat, pattern,
                  name or f"{target['name']}_{len(target['cubes']):03d}", target["name"],
                  self.y_stretch)
        target["cubes"].append(cb)
        return self

    def all_cubes(self):
        return [cb for b in self.bones for cb in b["cubes"]]

    # ------------------------------------------------------------------ UV
    def pack(self):
        items = []
        for cb in self.all_cubes():
            w, h, d = cb.size
            fw, fh = footprint(w, h, d)
            items.append((cb, fw + 2 * PAD, fh + 2 * PAD))
        items.sort(key=lambda t: (-t[2], -t[1], -t[0].size[2]))
        sky = [0] * self.atlas
        placed = []
        for cb, bw, bh in items:
            best_x, best_y = -1, 1 << 30
            for x in range(0, self.atlas - bw + 1):
                y = max(sky[x:x + bw])
                if y + bh <= self.atlas and y < best_y:
                    best_x, best_y = x, y
                    if y == 0:
                        break
            if best_x < 0:
                raise RuntimeError(f"атлас {self.atlas} переполнен на кубоиде {cb.name}")
            for x in range(best_x, best_x + bw):
                sky[x] = best_y + bh
            cb.u, cb.v = best_x + PAD, best_y + PAD
            placed.append((cb, cb.u, cb.v))
        return placed

    # ------------------------------------------------------------------ атлас
    @staticmethod
    def _paint_face(img, atlas, rect, color_hex, mat, pattern, face, seed):
        x, y, fw, fh = rect
        if fw <= 0 or fh <= 0:
            return
        r = rnd(seed)
        base = hx(color_hex)
        face_k = {
            "up": 1.10, "down": 0.84, "north": 1.0, "south": 0.96,
            "east": 0.92, "west": 0.94,
        }.get(face, 1.0)
        for yy in range(fh):
            for xx in range(fw):
                k = face_k * (1.0 + (r() - 0.5) * 0.11)
                # мягкий вертикальный градиент на боковых гранях
                if face in ("north", "south", "east", "west") and fh > 2:
                    k *= 1.0 + (0.5 - yy / (fh - 1)) * 0.11
                # очень лёгкая фаска: кубоиды читаются как гранёные пластины
                if fw > 2 and fh > 2:
                    if xx == 0 or yy == 0:
                        k *= 1.055
                    if xx == fw - 1 or yy == fh - 1:
                        k *= 0.925
                if pattern == "scales":
                    if (xx + 2 * yy) % 12 == 0 or (xx - 2 * yy) % 12 == 0:
                        k *= 0.77
                    elif (xx + 2 * yy) % 12 in (1, 11):
                        k *= 1.07
                    if r() < 0.018:
                        k *= 0.62
                elif pattern == "plates":
                    if yy % 6 == 0:
                        k *= 0.75
                    elif yy % 6 == 1:
                        k *= 1.09
                    if r() < 0.012:
                        k *= 0.86
                elif pattern == "rays":
                    if (yy + xx // 5) % 4 == 0:
                        k *= 0.79
                    if xx % 8 == 0:
                        k *= 1.11
                elif pattern == "strands":
                    if xx % 3 == 0:
                        k *= 0.80
                    elif xx % 3 == 1:
                        k *= 1.09
                    if r() < 0.02:
                        k *= 0.88
                elif pattern == "feather":
                    if (xx + yy // 2) % 5 == 0:
                        k *= 0.82
                    if xx % 9 == 0:
                        k *= 1.10
                    if yy % 7 == 0:
                        k *= 0.94
                elif pattern == "dapple":
                    cell = (xx // 6) * 7 + (yy // 5) * 13 + (seed & 31)
                    if cell % 17 < 4 and (xx % 6) ** 2 + (yy % 5) ** 2 < 22:
                        k *= 0.84
                    if r() < 0.01:
                        k *= 1.12
                elif pattern == "quilt":
                    if (xx + yy) % 9 == 0 or (xx - yy) % 9 == 0:
                        k *= 0.76
                    elif (xx + yy) % 9 == 1:
                        k *= 1.07
                elif pattern == "gold":
                    if xx % 4 == 0:
                        k *= 1.14
                    if yy % 7 == 0:
                        k *= 0.87
                    if r() < 0.012:
                        k *= 1.35
                elif pattern == "mottle":
                    if r() < 0.045:
                        k *= 0.76
                elif pattern == "stripe":
                    if (yy // 3) % 2 == 0:
                        k *= 0.88
                elif pattern == "horn":
                    if xx % 4 == 0:
                        k *= 0.83
                    if r() < 0.02:
                        k *= 1.14
                elif pattern == "eye":
                    k = 1.0 + 0.10 * math.sin((xx + yy) * 0.8)
                ax, ay = x + xx, y + yy
                if 0 <= ax < atlas and 0 <= ay < atlas:
                    idx = (ay * atlas + ax) * 4
                    img[idx] = clamp(base[0] * k)
                    img[idx + 1] = clamp(base[1] * k)
                    img[idx + 2] = clamp(base[2] * k)
                    img[idx + 3] = 255

    def texture(self, mats, patterns=None, placed=None):
        patterns = patterns or {}
        placed = placed or self.pack()
        a = self.atlas
        img = bytearray(a * a * 4)
        for cb, u, v in placed:
            w, h, d = cb.size
            top_c, side_c, front_c, bottom_c = mats[cb.mat]
            for face, rect in face_rects(u, v, w, h, d).items():
                color = {"up": top_c, "down": bottom_c, "north": front_c,
                         "south": front_c, "east": side_c, "west": side_c}[face]
                pattern = cb.pattern or patterns.get(cb.mat)
                seed = stable_seed(self.name, cb.name, cb.mat, face, u, v)
                self._paint_face(img, a, rect, color, cb.mat, pattern, face, seed)
        return img

    # ------------------------------------------------------------------ валидация
    def validate(self, placed):
        a = self.atlas
        used = bytearray(a * a)
        for cb, u, v in placed:
            w, h, d = cb.size
            for face, (fx, fy, fw, fh) in face_rects(u, v, w, h, d).items():
                assert fx >= 0 and fy >= 0 and fx + fw <= a and fy + fh <= a, \
                    f"UV за атласом: {cb.name}/{face}"
                for yy in range(fy, fy + fh):
                    for xx in range(fx, fx + fw):
                        i = yy * a + xx
                        assert not used[i], f"наложение UV: {cb.name}/{face} в ({xx},{yy})"
                        used[i] = 1
        for b in self.bones:
            if b["parent"] is not None:
                assert b["parent"] in self.by_name, f"нет родителя {b['parent']}"
        return sum(used)

    def extents(self):
        lo = [1 << 30] * 3
        hi = [-1 << 30] * 3
        for cb in self.all_cubes():
            for i in range(3):
                lo[i] = min(lo[i], cb.origin[i])
                hi[i] = max(hi[i], cb.origin[i] + cb.size[i])
        return lo, hi, [hi[i] - lo[i] for i in range(3)]

    # ------------------------------------------------------------------ выгрузка
    def write_game_geo(self, mats, texture_rel, patterns=None, flip_y=True):
        placed = self.pack()
        occupied = self.validate(placed)
        bones_out = []
        for b in self.bones:
            cubes_out = []
            for cb in b["cubes"]:
                o = list(cb.origin)
                if flip_y:
                    o[1] = -(cb.origin[1] + cb.size[1])
                cubes_out.append({
                    "origin": [round(float(x), 2) for x in o],
                    "size": cb.size,
                    "uv": [cb.u, cb.v],
                })
            pv = list(b["pivot"])
            if flip_y:
                pv[1] = -b["pivot"][1]
            entry = {"name": b["name"], "pivot": [round(float(x), 2) for x in pv],
                     "cubes": cubes_out}
            if b["parent"]:
                entry["parent"] = b["parent"]
            if b["anim"]:
                entry["anim"] = b["anim"]
            bones_out.append(entry)
        geo = {
            "texture_width": self.atlas,
            "texture_height": self.atlas,
            "render_scale": round(float(self.render_scale), 4),
            "translucent": False,
            "bones": bones_out,
        }
        geo_path = ASSETS / "geo" / f"{self.name}.json"
        geo_path.write_text(json.dumps(geo, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")

        png = png_bytes(self.texture(mats, patterns, placed), self.atlas)
        tex_path = ASSETS / texture_rel
        tex_path.parent.mkdir(parents=True, exist_ok=True)
        tex_path.write_bytes(png)

        lo, hi, size = self.extents()
        stats = {
            "name": self.name,
            "bones": len(self.bones),
            "cubes": len(self.all_cubes()),
            "atlas": self.atlas,
            "occupied_uv_px": occupied,
            "render_scale": geo["render_scale"],
            "design_extents_units": {"lo": lo, "hi": hi, "size": size},
            "size_blocks": [round(s * geo["render_scale"] / 16.0, 3) for s in size],
            "game_geo": str(geo_path.relative_to(ROOT)),
            "texture": str(tex_path.relative_to(ROOT)),
            "flip_y": flip_y,
        }
        REPORTS.mkdir(parents=True, exist_ok=True)
        (REPORTS / f"{self.name}_stats.json").write_text(
            json.dumps(stats, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        self.write_blockbench_geo(size)
        self.write_bbmodel(png, placed)
        return stats

    def write_blockbench_geo(self, size):
        """Bedrock geometry 1.12: импортируется в Blockbench как готовая модель."""
        bones = []
        for b in self.bones:
            cubes = []
            for cb in b["cubes"]:
                cubes.append({"origin": cb.origin, "size": cb.size, "uv": [cb.u, cb.v]})
            e = {"name": b["name"], "pivot": b["pivot"], "cubes": cubes}
            if b["parent"]:
                e["parent"] = b["parent"]
            bones.append(e)
        bb = {
            "format_version": "1.12.0",
            "minecraft:geometry": [{
                "description": {
                    "identifier": f"geometry.riftworld.{self.name}_hero",
                    "texture_width": self.atlas,
                    "texture_height": self.atlas,
                    "visible_bounds_width": round(max(size[0], size[2]) / 16.0, 2),
                    "visible_bounds_height": round(size[1] / 16.0, 2),
                    "visible_bounds_offset": [0, 0, 0],
                },
                "bones": bones,
            }],
        }
        out = REPORTS / f"{self.name}.blockbench.geo.json"
        out.write_text(json.dumps(bb, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")

    def write_bbmodel(self, png, placed):
        """Самодостаточный .bbmodel: элементы, группы-кости и текстура внутри файла."""
        a = self.atlas
        cu = {}
        elements = []
        for b in self.bones:
            for i, cb in enumerate(b["cubes"]):
                uid = make_uuid(self.name, b["name"], i, cb.name)
                cu[id(cb)] = uid
                w, h, d = cb.size
                x, y, z = cb.origin
                faces = {}
                for face, (fx, fy, fw, fh) in face_rects(cb.u, cb.v, w, h, d).items():
                    faces[face] = {"uv": [fx, fy, fx + fw, fy + fh], "texture": 0}
                elements.append({
                    "name": cb.name,
                    "type": "cube",
                    "uuid": uid,
                    "box_uv": False,
                    "rescale": False,
                    "locked": False,
                    "light_emission": 0,
                    "render_order": "default",
                    "allow_mirror_modeling": True,
                    "from": [x, y, z],
                    "to": [x + w, y + h, z + d],
                    "autouv": 0,
                    "color": 0,
                    "origin": b["pivot"],
                    "faces": faces,
                    "visibility": True,
                    "export": True,
                })
        gu = {b["name"]: make_uuid(self.name, "bone", b["name"]) for b in self.bones}

        def group(b):
            node = {
                "name": b["name"],
                "origin": b["pivot"],
                "rotation": [0, 0, 0],
                "color": 0,
                "uuid": gu[b["name"]],
                "export": True,
                "mirror_uv": False,
                "isOpen": True,
                "locked": False,
                "visibility": True,
                "autouv": 0,
                "selected": False,
                "children": [cu[id(cb)] for cb in b["cubes"]],
            }
            for child in self.bones:
                if child["parent"] == b["name"]:
                    node["children"].append(group(child))
            return node

        outliner = [group(b) for b in self.bones if not b["parent"]]
        tex_uuid = make_uuid(self.name, "texture")
        tex = {
            "path": "",
            "name": f"{self.name}.png",
            "folder": "entity",
            "namespace": "riftworld",
            "id": "0",
            "particle": False,
            "render_mode": "default",
            "visible": True,
            "mode": "bitmap",
            "saved": True,
            "uuid": tex_uuid,
            "source": "data:image/png;base64," + base64.b64encode(png).decode("ascii"),
            "width": a,
            "height": a,
        }
        project = {
            "meta": {"format_version": "4.10", "model_format": "free", "box_uv": False},
            "name": f"{self.name}_hero",
            "model_identifier": f"riftworld.{self.name}",
            "front_gui_light": False,
            "visible_box": [1, 1, 0],
            "variable_placeholders": "",
            "variable_placeholder_buttons": [],
            "timeline_setups": [],
            "resolution": {"width": a, "height": a},
            "elements": elements,
            "outliner": outliner,
            "textures": [tex],
            "animations": [],
        }
        out = REPORTS / f"{self.name}.bbmodel"
        out.write_text(json.dumps(project, ensure_ascii=False, separators=(",", ":")) + "\n",
                       encoding="utf-8")


def png_bytes(img, atlas):
    raw = bytearray()
    for y in range(atlas):
        raw.append(0)
        raw.extend(img[y * atlas * 4:(y + 1) * atlas * 4])

    def chunk(tag, payload):
        return (struct.pack(">I", len(payload)) + tag + payload
                + struct.pack(">I", zlib.crc32(tag + payload) & 0xFFFFFFFF))

    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", atlas, atlas, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(bytes(raw), 6))
            + chunk(b"IEND", b""))


# --------------------------------------------------------------------- helpers
def x_origin(side, inner, width):
    """origin X для симметричной коробки, уходящей от центра наружу."""
    return min(side * inner, side * (inner + width))


def box_x(side, inner, y, z, width, height, depth):
    return [x_origin(side, inner, width), y, z], [int(width), int(height), int(depth)]


def pair(rig, bone, inner, y, z, size, mat, pattern=None):
    w, h, d = size
    for s in (-1, 1):
        o, _ = box_x(s, inner, y, z, w, h, d)
        rig.cube(o, [int(w), int(h), int(d)], mat, pattern, bone=bone)


def barrel(rig, bone, z0, z1, step, levels, profile, material_fn, cx=0.0):
    """Фасочное бочкообразное сечение: на каждый отрезок Z — levels пластин.

    profile(t) -> (half_width, half_height, center_y). material_fn(norm, t, j)
    -> (mat, pattern). norm=-1 — низ, norm=+1 — верх.
    """
    total = z1 - z0
    for z in range(int(z0), int(z1), int(step)):
        d = min(int(step), int(z1) - z)
        t = (z + d / 2.0 - z0) / float(total)
        rx, ry, cy = profile(t)
        for j in range(levels):
            ya = cy - ry + 2.0 * ry * j / levels
            yb = cy - ry + 2.0 * ry * (j + 1) / levels
            ym = (ya + yb) / 2.0
            norm = (ym - cy) / max(0.001, ry)
            half_w = rx * math.sqrt(max(0.0, 1.0 - norm * norm))
            w = max(1, int(round(2.0 * half_w)))
            h = max(1, int(round(yb - ya)))
            if w < 2:
                continue
            mat, pattern = material_fn(norm, t, j)
            rig.cube([cx - w / 2.0, ya, z], [w, h, d], mat, pattern, bone=bone)
