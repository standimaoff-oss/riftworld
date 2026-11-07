#!/usr/bin/env python3
"""v0.46.0 — объёмное оружие и инструменты Элисара.

В проекте оружие рисуются не плоским спрайтом 16x16, а настоящей GLB-моделью
(так сделаны morgrath_scythe, guard_sickle, crystal_katar). Здесь тем же путём
собираются два комплекта Элисара — ауритовый и рифт-кристальный.

Система координат повторяет остальные GLB-оружия проекта: рукоять в начале
координат, древко уходит вверх по +Y, клинок/голова развёрнуты в +Z.
Единица — блок. Все границы ниже проверены на совпадение с guard_sickle.

Палитры взяты k-средними прямо с референсов reports/v040_refs/01..03:
    01_aurite_medium_armor   — тёмная сталь, кожаные ремни, золотая окантовка
    02_riftcrystal_strong_armor — почти чёрный кристалл, бирюзовое свечение
    03_riftworld_weapon_tools_set — рукояти тёмного дерева

Запуск:  python tools/rift/gen_v046_weapons.py
Выход:   mod/src/main/resources/assets/riftworld/models/glb/<имя>.glb
"""
from __future__ import annotations

import io
import json
import struct
import zlib
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parents[2]
OUT = HERE / "mod/src/main/resources/assets/riftworld/models/glb"

ATLAS = 256          # сторона атласа в текселях
FACE_TEX = 16        # сторона квадрата одной грани в атласе

# ------------------------------------------------------------------ палитры
PAL = {
    "aurite": {
        "wood":  (0x48, 0x30, 0x11),
        "metal": (0xc2, 0x90, 0x48),
        "edge":  (0xe8, 0xcf, 0x8f),
        "dark":  (0x33, 0x2d, 0x24),
    },
    "riftcrystal": {
        "wood":  (0x2b, 0x1b, 0x09),
        "metal": (0x35, 0x3a, 0x43),
        "edge":  (0x52, 0xc0, 0xbd),
        "dark":  (0x0a, 0x12, 0x14),
    },
}
# Затемнение грани: верх светлее, низ темнее — читается объём даже без теней.
FACE_SHADE = {"+Y": 1.20, "-Y": 0.58, "+X": 0.90, "-X": 0.78, "+Z": 0.98, "-Z": 0.72}

# ------------------------------------------------------------------ геометрия
B = lambda mat, x0, y0, z0, x1, y1, z1: (mat, (x0, y0, z0), (x1, y1, z1))

WEAPONS = {
    # длина в блоках «от рукояти до кончика» — её же ждёт GlbItem.length()
    "sword": (1.05, [
        B("wood",  -0.055, -0.30, -0.055,  0.055,  0.00,  0.055),  # рукоять
        B("metal", -0.085, -0.37, -0.085,  0.085, -0.30,  0.085),  # навершие
        B("metal", -0.170,  0.00, -0.055,  0.170,  0.085, 0.055),  # гарда
        B("edge",  -0.062,  0.085, -0.020, 0.062,  0.55,  0.020),  # клинок, низ
        B("edge",  -0.050,  0.55,  -0.017, 0.050,  0.86,  0.017),  # клинок, середина
        B("edge",  -0.030,  0.86,  -0.013, 0.030,  1.02,  0.013),  # остриё
    ]),
    "pickaxe": (0.95, [
        B("wood",  -0.050, -0.28, -0.050,  0.050,  0.82,  0.050),  # древко
        B("metal", -0.420,  0.74, -0.045, -0.300,  0.83,  0.045),  # головка: дуга
        B("metal", -0.300,  0.79, -0.045, -0.100,  0.88,  0.045),
        B("metal", -0.100,  0.83, -0.050,  0.100,  0.90,  0.050),
        B("metal",  0.100,  0.79, -0.045,  0.300,  0.88,  0.045),
        B("metal",  0.300,  0.74, -0.045,  0.420,  0.83,  0.045),
        B("edge",  -0.470,  0.70, -0.035, -0.400,  0.77,  0.035),  # зубья
        B("edge",   0.400,  0.70, -0.035,  0.470,  0.77,  0.035),
    ]),
    "axe": (0.92, [
        B("wood",  -0.050, -0.28, -0.050,  0.050,  0.80,  0.050),  # топорище
        B("metal", -0.070,  0.50, -0.050,  0.090,  0.60,  0.050),  # щека
        B("metal", -0.140,  0.62, -0.045, -0.030,  0.76,  0.045),  # обух
        B("metal",  0.030,  0.58, -0.050,  0.140,  0.80,  0.050),  # лезвие, корень
        B("edge",   0.140,  0.62, -0.045,  0.300,  0.86,  0.045),  # лезвие
        B("edge",   0.300,  0.62, -0.035,  0.340,  0.80,  0.035),  # режущая кромка
    ]),
    "shovel": (0.92, [
        B("wood",  -0.050, -0.28, -0.050,  0.050,  0.80,  0.050),  # черенок
        B("metal", -0.060,  0.78, -0.050,  0.060,  0.85,  0.050),  # шейка
        B("metal", -0.160,  0.85, -0.028,  0.160,  0.94,  0.028),  # полотно
        B("edge",  -0.130,  0.94, -0.022,  0.130,  1.00,  0.022),  # кромка
    ]),
    "hoe": (0.92, [
        B("wood",  -0.050, -0.28, -0.050,  0.050,  0.80,  0.050),  # черенок
        B("metal", -0.080,  0.76, -0.045,  0.060,  0.82,  0.045),  # воротник
        B("metal",  0.040,  0.66, -0.030,  0.220,  0.80,  0.030),  # поперечная пластина
        B("edge",   0.220,  0.66, -0.028,  0.280,  0.74,  0.028),  # кромка
    ]),
}

# Порядок обхода грани: первые две вершины — нижнее ребро (низу соответствует
# больший v, потому что в glTF v=0 — верх картинки).
FACES = {
    "+X": ((1, 0, 0), lambda a, b: ((b[0], a[1], a[2]), (b[0], a[1], b[2]), (b[0], b[1], b[2]), (b[0], b[1], a[2]))),
    "-X": ((-1, 0, 0), lambda a, b: ((a[0], a[1], b[2]), (a[0], a[1], a[2]), (a[0], b[1], a[2]), (a[0], b[1], b[2]))),
    "+Y": ((0, 1, 0), lambda a, b: ((a[0], b[1], b[2]), (b[0], b[1], b[2]), (b[0], b[1], a[2]), (a[0], b[1], a[2]))),
    "-Y": ((0, -1, 0), lambda a, b: ((a[0], a[1], a[2]), (b[0], a[1], a[2]), (b[0], a[1], b[2]), (a[0], a[1], b[2]))),
    "+Z": ((0, 0, 1), lambda a, b: ((a[0], a[1], b[2]), (b[0], a[1], b[2]), (b[0], b[1], b[2]), (a[0], b[1], b[2]))),
    "-Z": ((0, 0, -1), lambda a, b: ((b[0], a[1], a[2]), (a[0], a[1], a[2]), (a[0], b[1], a[2]), (b[0], b[1], a[2]))),
}


def paint_face(atlas, slot, mat_rgb, shade, rng):
    """Кладёт одну грань в атлас: цвет материала, градиент, шум, тёмная рамка."""
    cols = ATLAS // FACE_TEX
    cx, cy = slot % cols, slot // cols
    x0, y0 = cx * FACE_TEX, cy * FACE_TEX

    g = np.linspace(0.0, 1.0, FACE_TEX, dtype=np.float32)[None, :]      # сверху вниз
    vgrad = (1.16 - 0.30 * g).T                                          # верх светлее
    hgrad = (1.05 - 0.10 * np.linspace(0.0, 1.0, FACE_TEX, dtype=np.float32))[None, :]
    noise = 1.0 + 0.045 * rng.standard_normal((FACE_TEX, FACE_TEX)).astype(np.float32)

    k = (shade * vgrad * hgrad * noise)[..., None]
    px = np.clip(np.array(mat_rgb, dtype=np.float32)[None, None, :] * k, 0, 255).astype(np.uint8)

    # Рамка на 1 тексель: без неё соседние грани сливаются в одно пятно.
    px[0, :] = (px[0, :] * 0.62).astype(np.uint8)
    px[-1, :] = (px[-1, :] * 0.62).astype(np.uint8)
    px[:, 0] = (px[:, 0] * 0.62).astype(np.uint8)
    px[:, -1] = (px[:, -1] * 0.62).astype(np.uint8)

    atlas[y0:y0 + FACE_TEX, x0:x0 + FACE_TEX] = px
    # UV с отступом в полтекселя: при NEAREST соседняя грань не подхватится.
    i = 0.5 / ATLAS
    u0, v0 = x0 / ATLAS + i, y0 / ATLAS + i
    u1, v1 = (x0 + FACE_TEX) / ATLAS - i, (y0 + FACE_TEX) / ATLAS - i
    return (u0, v0, u1, v1)


def build(name: str, tier: str):
    length, boxes = WEAPONS[name]
    pal = PAL[tier]
    rng = np.random.default_rng(abs(hash((name, tier))) % (1 << 31))

    atlas = np.zeros((ATLAS, ATLAS, 3), dtype=np.uint8)
    pos, nrm, uv, idx = [], [], [], []
    slot = 0

    for mat, a, b in boxes:
        rgb = pal[mat]
        for face, (normal, corners) in FACES.items():
            c = [tuple(float(v) for v in p) for p in corners(a, b)]
            # Проверяем обход и при необходимости разворачиваем: glTF ждёт CCW.
            e1 = np.subtract(c[1], c[0])
            e2 = np.subtract(c[2], c[0])
            n = np.cross(e1, e2)
            if float(np.dot(n, normal)) < 0.0:
                c = c[::-1]
            u0, v0, u1, v1 = paint_face(atlas, slot, rgb, FACE_SHADE[face], rng)
            slot += 1
            base = len(pos)
            for p in c:
                pos.append(p)
                nrm.append(tuple(float(v) for v in normal))
            uv.extend([(u0, v1), (u1, v1), (u1, v0), (u0, v0)])
            idx.extend([base, base + 1, base + 2, base, base + 2, base + 3])

    assert slot <= (ATLAS // FACE_TEX) ** 2, f"{name}: атлас переполнен ({slot} граней)"
    return length, atlas, pos, nrm, uv, idx


def png_bytes(atlas: np.ndarray) -> bytes:
    buf = io.BytesIO()
    Image.fromarray(atlas, "RGB").save(buf, "PNG", optimize=True)
    return buf.getvalue()


def write_glb(path: Path, atlas: np.ndarray, pos, nrm, uv, idx):
    img = png_bytes(atlas)
    pos_b = np.asarray(pos, dtype="<f4").tobytes()
    nrm_b = np.asarray(nrm, dtype="<f4").tobytes()
    uv_b = np.asarray(uv, dtype="<f4").tobytes()
    idx_b = np.asarray(idx, dtype="<u4").tobytes()

    def pad4(b: bytes) -> bytes:
        return b + b"\x00" * ((-len(b)) % 4)

    chunks = [pos_b, nrm_b, uv_b, idx_b, img]
    bin_blob = b"".join(pad4(c) for c in chunks)

    off, views, accessors = 0, [], []

    def view(data: bytes, target=None):
        nonlocal off
        v = {"buffer": 0, "byteOffset": off, "byteLength": len(data)}
        if target is not None:
            v["target"] = target
        views.append(v)
        off += len(pad4(data))
        return len(views) - 1

    def acc(v, comp_type, comps, type_name, count, minmax=None):
        a = {"bufferView": v, "componentType": comp_type, "count": count, "type": type_name}
        if minmax:
            a["min"], a["max"] = minmax, minmax and None
        accessors.append(a)
        return len(accessors) - 1

    vp = view(pos_b, 34962)
    vn = view(nrm_b, 34962)
    vu = view(uv_b, 34962)
    vi = view(idx_b, 34963)
    vim = view(img)

    lo = [float(min(p[i] for p in pos)) for i in range(3)]
    hi = [float(max(p[i] for p in pos)) for i in range(3)]
    ap = acc(vp, 5126, 3, "VEC3", len(pos), lo)
    accessors[-1]["max"] = hi
    an = acc(vn, 5126, 3, "VEC3", len(nrm))
    au = acc(vu, 5126, 2, "VEC2", len(uv))
    ai = acc(vi, 5125, 1, "SCALAR", len(idx))

    gltf = {
        "asset": {"version": "2.0", "generator": "riftworld gen_v046_weapons.py"},
        "scene": 0,
        "scenes": [{"nodes": [0]}],
        "nodes": [{"name": "root", "mesh": 0}],
        "meshes": [{
            "name": path.stem,
            "primitives": [{
                "attributes": {"POSITION": ap, "NORMAL": an, "TEXCOORD_0": au},
                "indices": ai,
                "material": 0,
                "mode": 4,
            }],
        }],
        "materials": [{
            "name": path.stem,
            "pbrMetallicRoughness": {
                "baseColorTexture": {"index": 0},
                "metallicFactor": 0.0,
                "roughnessFactor": 1.0,
            },
        }],
        "textures": [{"sampler": 0, "source": 0}],
        "samplers": [{"magFilter": 9728, "minFilter": 9728, "wrapS": 33071, "wrapT": 33071}],
        "images": [{"bufferView": vim, "mimeType": "image/png", "name": path.stem}],
        "accessors": accessors,
        "bufferViews": views,
        "buffers": [{"byteLength": len(bin_blob)}],
    }

    js = json.dumps(gltf, separators=(",", ":")).encode("utf-8")
    js += b" " * ((-len(js)) % 4)
    bin_blob += b"\x00" * ((-len(bin_blob)) % 4)
    total = 12 + 8 + len(js) + 8 + len(bin_blob)

    with path.open("wb") as f:
        f.write(b"glTF" + struct.pack("<II", 2, total))
        f.write(struct.pack("<II", len(js), 0x4E4F534A) + js)
        f.write(struct.pack("<II", len(bin_blob), 0x004E4942) + bin_blob)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for tier in ("aurite", "riftcrystal"):
        for name in WEAPONS:
            length, atlas, pos, nrm, uv, idx = build(name, tier)
            target = OUT / f"{tier}_{name}.glb"
            write_glb(target, atlas, pos, nrm, uv, idx)
            print(f"{target.name:28s} {len(pos):4d} вершин, {len(idx)//3:4d} треугольников, "
                  f"длина {length:.2f} блока, атлас {ATLAS}x{ATLAS}")


if __name__ == "__main__":
    main()
