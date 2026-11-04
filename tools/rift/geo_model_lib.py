#!/usr/bin/env python3
"""
Общая библиотека для JSON-моделей мобов (`assets/riftworld/geo/*.json`).

Даёт: сборку костей/кубоидов, честную ванильную развёртку коробки, полковую
упаковку UV-островков без наложений, рисование атласа по «материалам»
(каждая грань получает свой оттенок) и самопроверку.

Используется генераторами `gen_v033_liopleurodon_model.py`, `gen_v034_pegasus_model.py`,
`gen_v035_wendigo_model.py`.
"""
import json
import pathlib
import struct
import zlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
ASSETS = ROOT / "mod/src/main/resources/assets/riftworld"
PAD = 1


def hx(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def rnd(seed):
    st = seed & 0xFFFFFFFF

    def nxt():
        nonlocal st
        st = (1103515245 * st + 12345) & 0x7FFFFFFF
        return st / 0x7FFFFFFF
    return nxt


def write_png(path, pixels):
    h, w = len(pixels), len(pixels[0])
    raw = bytearray()
    for row in pixels:
        raw.append(0)
        for (r, g, b, a) in row:
            raw += bytes((r, g, b, a))

    def chunk(tag, payload):
        return struct.pack(">I", len(payload)) + tag + payload + struct.pack(">I", zlib.crc32(tag + payload) & 0xFFFFFFFF)

    out = b"\x89PNG\r\n\x1a\n"
    out += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    out += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    out += chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(out)


def footprint(w, h, d):
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


class Rig:
    """Скелет: кости с pivot/anim + кубоиды с материалом и доп. «крапинами»."""

    def __init__(self, atlas=256):
        self.atlas = atlas
        self.bones = []

    def bone(self, name, pivot, anim=None, parent=None):
        self.bones.append({"name": name, "pivot": pivot, "anim": anim, "parent": parent, "cubes": []})
        return self

    def cube(self, origin, size, mat, speck=None):
        self.bones[-1]["cubes"].append({"origin": origin, "size": size, "mat": mat, "speck": speck})
        return self

    # ------------------------------------------------------------------ упаковка
    def pack(self):
        items = []
        for b in self.bones:
            for cb in b["cubes"]:
                w, h, d = cb["size"]
                fw, fh = footprint(w, h, d)
                items.append((cb, fw + 2 * PAD, fh + 2 * PAD))
        items.sort(key=lambda t: -t[1])
        placed, shelf_y, shelf_h, cur_x = [], 0, 0, 0
        for cb, fw, fh in items:
            if cur_x + fw > self.atlas:
                shelf_y += shelf_h
                shelf_h, cur_x = 0, 0
            if shelf_y + fh > self.atlas:
                raise SystemExit(f"атлас {self.atlas} переполнен")
            placed.append((cb, cur_x + PAD, shelf_y + PAD))
            cur_x += fw
            shelf_h = max(shelf_h, fh)
        return placed

    # ------------------------------------------------------------------ атлас
    def texture(self, mats, placed, scar_color=None, scar_mats=(), patterns=None):
        A = self.atlas
        img = [[(0, 0, 0, 0)] * A for _ in range(A)]
        for cb, u, v in placed:
            w, h, d = cb["size"]
            top_c, side_c, front_c, bottom_c = mats[cb["mat"]]
            seed = hash((u, v, cb["mat"])) & 0x7FFF
            for name, (fx, fy, fw, fh) in face_rects(u, v, w, h, d).items():
                color = {"up": top_c, "down": bottom_c, "north": front_c, "south": front_c,
                         "east": side_c, "west": side_c}[name]
                self._paint(img, fx, fy, fw, fh, color, seed + ord(name[0]), cb, patterns)
        if scar_color:
            for cb, u, v in placed:
                if cb["mat"] not in scar_mats:
                    continue
                w, h, d = cb["size"]
                r = rnd(hash((u, v)) & 0x7FFF)
                for _ in range(max(1, (w + h) // 12)):
                    sx = u + int(r() * (2 * d + w))
                    sy = v + d + int(r() * max(1, h))
                    for k in range(2 + int(r() * 4)):
                        ay, ax = sy + (k % 2), sx + (1 if r() < 0.5 else -1) * k
                        if 0 <= ax < A and 0 <= ay < A:
                            img[ay][ax] = (*hx(scar_color), 255)
        return img

    @staticmethod
    def _paint(img, x, y, fw, fh, color, seed, cb, patterns=None):
        A = len(img)
        r = rnd(seed)
        base = hx(color)
        specks = cb.get("speck") or []
        pattern = (patterns or {}).get(cb["mat"])
        for yy in range(fh):
            for xx in range(fw):
                k = 1.0 + (r() - 0.5) * 0.12
                px = [max(0, min(255, int(c * k))) for c in base]
                if specks and r() < 0.06:
                    sc = specks[int(r() * len(specks)) % len(specks)]
                    px = list(hx(sc))
                # v0.39.3: узоры — без них текстуры мобов выглядели «залитыми краской»
                if pattern == "dapple" and ((xx % 5 == 1 and yy % 5 == 2) or (xx % 7 == 4 and yy % 6 == 5)):
                    px = [max(0, int(c * 0.80)) for c in px]          # яблоки на шкуре коня
                elif pattern == "felt" and ((xx * 3 + yy * 5) % 11 < 3):
                    px = [max(0, int(c * 0.72)) for c in px]
                elif pattern == "bars" and (yy % 3 == 0):
                    px = [max(0, int(c * 0.84)) for c in px]          # ряды перьев
                elif pattern == "bare" and r() < 0.18:
                    px = list(hx("#2B2320"))                          # просветы кожи между костями
                elif pattern == "strands" and xx % 2 == 0:
                    px = [max(0, int(c * 0.86)) for c in px]          # пряди гривы
                elif pattern == "vein" and (xx + yy) % 6 == 0:
                    px = [min(255, int(c * 1.25)) for c in px]        # прожилки
                ax, ay = x + xx, y + yy
                if 0 <= ax < A and 0 <= ay < A:
                    img[ay][ax] = (px[0], px[1], px[2], 255)

    # ------------------------------------------------------------------ выгрузка
    def write(self, name, mats, texture_rel, scale_to=None, length_axis=2, length_blocks=None,
              scar_color=None, scar_mats=(), extra=None, patterns=None, flip_y=False):
        """flip_y — ВАЖНО для мобов: в Minecraft ось Y модели смотрит ВНИЗ.

        Модели удобно писать «вверх» (0 = земля под лапами), но в игре такая модель встаёт
        на голову — именно это случилось с Пегасом и Вендиго (они появлялись перевёрнутыми
        и на кадрах выглядели кучей плит). При flip_y=True координаты зеркалятся по Y.
        """
        placed = self.pack()
        pos = {id(cb): (u, v) for cb, u, v in placed}
        bones_out = []
        for b in self.bones:
            cubes = []
            for cb in b["cubes"]:
                u, v = pos[id(cb)]
                o = list(cb["origin"])
                if flip_y:
                    o[1] = -(cb["origin"][1] + cb["size"][1])
                cubes.append({"origin": o, "size": cb["size"], "uv": [u, v]})
            pv = list(b["pivot"])
            if flip_y:
                pv[1] = -b["pivot"][1]
            entry = {"name": b["name"], "pivot": pv, "cubes": cubes}
            if b.get("parent"):
                entry["parent"] = b["parent"]
            if b.get("anim"):
                entry["anim"] = b["anim"]
            bones_out.append(entry)

        lo = [1 << 30] * 3
        hi = [-1 << 30] * 3
        for b in self.bones:
            for cb in b["cubes"]:
                for i in range(3):
                    lo[i] = min(lo[i], cb["origin"][i])
                    hi[i] = max(hi[i], cb["origin"][i] + cb["size"][i])
        scale = 1.0
        if length_blocks and scale_to:
            scale = round(length_blocks / ((hi[length_axis] - lo[length_axis]) / 16.0), 3)

        geo = {
            "texture_width": self.atlas,
            "texture_height": self.atlas,
            "render_scale": scale if scale_to else 1.0,
            "translucent": False,
            "bones": bones_out,
        }
        if extra:
            geo.update(extra)
        geo_path = ASSETS / "geo" / f"{name}.json"
        geo_path.write_text(json.dumps(geo, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
        tex_path = ASSETS / texture_rel
        write_png(tex_path, self.texture(mats, placed, scar_color, scar_mats, patterns))

        # самопроверка: UV внутри атласа и без наложений
        used = [[False] * self.atlas for _ in range(self.atlas)]
        n_cubes = 0
        for cb, u, v in placed:
            n_cubes += 1
            w, h, d = cb["size"]
            for name, (fx, fy, fw, fh) in face_rects(u, v, w, h, d).items():
                assert fx >= 0 and fy >= 0 and fx + fw <= self.atlas and fy + fh <= self.atlas, f"UV за атласом: {cb}"
                for yy in range(fy, fy + fh):
                    for xx in range(fx, fx + fw):
                        assert not used[yy][xx], f"наложение UV у {cb} ({xx},{yy})"
                        used[yy][xx] = True
        size = [hi[i] - lo[i] for i in range(3)]
        return {"bones": len(bones_out), "cubes": n_cubes, "scale": geo["render_scale"],
                "size_units": size, "size_blocks": [round(s * geo["render_scale"] / 16.0, 2) for s in size],
                "geo": str(geo_path.relative_to(ROOT)), "tex": str(tex_path.relative_to(ROOT))}
