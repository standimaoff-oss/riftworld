#!/usr/bin/env python3
"""
Честный оффлайн-рендер моб-модели (geo/*.json + атлас) без Minecraft.

  * перспективная камера с поворотом по рысканию и тангажу;
  * отсечение задних граней (back-face culling) по нормали;
  * z-буфер вместо «сортировки по глубине» — перекрытия считаются правильно;
  * ламбертово освещение двумя источниками + фоновое;
  * три ракурса в одном кадре: сбоку, три четверти, спереди.

Запуск: python3 tools/rift/preview_mob_model.py liopleurodon [--scale 1.0]
Выход:  reports/<name>_model_preview.png
"""
import json
import math
import pathlib
import struct
import sys
import zlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
ARGS = [a for a in sys.argv[1:] if not a.startswith("--")]
NAME = ARGS[0] if ARGS else "liopleurodon"
SCALE = 1.25
for a in sys.argv[1:]:
    if a.startswith("--scale="):
        SCALE = float(a.split("=", 1)[1])

GEO = ROOT / f"mod/src/main/resources/assets/riftworld/geo/{NAME}.json"
TEX = ROOT / f"mod/src/main/resources/assets/riftworld/textures/entity/{NAME}.png"
OUT = ROOT / f"reports/{NAME}_model_preview.png"

TILE_W, TILE_H = 660, 560
SX, SY = 3, 1          # 3 ракурса в ряд
BG = (22, 26, 32)


# ------------------------------------------------------------------ PNG
def read_png(path):
    raw = path.read_bytes()
    pos, w, h, ctype, idat = 8, 0, 0, 6, bytearray()
    while pos < len(raw):
        ln, typ = struct.unpack(">I4s", raw[pos:pos + 8])
        chunk = raw[pos + 8:pos + 8 + ln]
        pos += 12 + ln
        if typ == b"IHDR":
            w, h, _bd, ctype, *_ = struct.unpack(">IIBBBBB", chunk)
        elif typ == b"IDAT":
            idat.extend(chunk)
    data = zlib.decompress(bytes(idat))
    bpp = 4 if ctype == 6 else 3
    stride = 1 + w * bpp
    out, prev = [], bytearray(w * bpp)
    for y in range(h):
        flt = data[y * stride]
        row = bytearray(data[y * stride + 1:(y + 1) * stride])
        if flt == 1:
            for i in range(bpp, len(row)):
                row[i] = (row[i] + row[i - bpp]) & 0xFF
        elif flt == 2:
            for i in range(len(row)):
                row[i] = (row[i] + prev[i]) & 0xFF
        elif flt == 3:
            for i in range(len(row)):
                left = row[i - bpp] if i >= bpp else 0
                row[i] = (row[i] + ((left + prev[i]) >> 1)) & 0xFF
        elif flt == 4:
            for i in range(len(row)):
                a = row[i - bpp] if i >= bpp else 0
                b = prev[i]
                c = prev[i - bpp] if i >= bpp else 0
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                row[i] = (row[i] + pr) & 0xFF
        prev = row
        out.append([(row[x * bpp], row[x * bpp + 1], row[x * bpp + 2]) for x in range(w)])
    return out


def write_png(path, pixels):
    h, w = len(pixels), len(pixels[0])
    raw = bytearray()
    for row in pixels:
        raw.append(0)
        for (r, g, b) in row:
            raw += bytes((r, g, b, 255))

    def chunk(tag, payload):
        return struct.pack(">I", len(payload)) + tag + payload + struct.pack(">I", zlib.crc32(tag + payload) & 0xFFFFFFFF)

    out = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    out += chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(out)


# ------------------------------------------------------------------ UV
def face_rects(u, v, w, h, d):
    return {
        "up": (u + d, v, w, d),
        "down": (u + d + w, v, w, d),
        "east": (u, v + d, d, h),
        "north": (u + d, v + d, w, h),
        "west": (u + d + w, v + d, d, h),
        "south": (u + d + w + d, v + d, w, h),
    }


def tex_sample(tex, u, v):
    h, w = len(tex), len(tex[0])
    x = min(w - 1, max(0, int(u)))
    y = min(h - 1, max(0, int(v)))
    return tex[y][x]


def avg_color(tex, rect):
    x, y, w, h = rect
    if w <= 0 or h <= 0:
        return (90, 100, 110)
    tr = tg = tb = n = 0
    for yy in range(y, min(y + h, len(tex))):
        for xx in range(x, min(x + w, len(tex[0]))):
            px = tex[yy][xx]
            tr += px[0]
            tg += px[1]
            tb += px[2]
            n += 1
    n = max(1, n)
    return (tr // n, tg // n, tb // n)


# ------------------------------------------------------------------ math
def norm(v):
    l = math.sqrt(v[0] ** 2 + v[1] ** 2 + v[2] ** 2) or 1.0
    return (v[0] / l, v[1] / l, v[2] / l)


def sub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


FACES = [
    # имя, четыре вершины в координатах кубоида (x,y,z), внешняя нормаль
    ("north", [(0, 0, 0), (1, 0, 0), (1, 1, 0), (0, 1, 0)], (0, 0, -1)),
    ("south", [(0, 0, 1), (0, 1, 1), (1, 1, 1), (1, 0, 1)], (0, 0, 1)),
    ("up", [(0, 0, 0), (0, 0, 1), (1, 0, 1), (1, 0, 0)], (0, -1, 0)),
    ("down", [(0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)], (0, 1, 0)),
    ("west", [(0, 0, 0), (0, 1, 0), (0, 1, 1), (0, 0, 1)], (-1, 0, 0)),
    ("east", [(1, 0, 0), (1, 0, 1), (1, 1, 1), (1, 1, 0)], (1, 0, 0)),
]


def render_view(tex, cubes, azim_deg, elev_deg, zoom, img_w, img_h, bg):
    """Рисует модель в один тайл. azim — поворот вокруг вертикали, elev — наклон."""
    ca, sa = math.cos(math.radians(azim_deg)), math.sin(math.radians(azim_deg))
    ce, se = math.cos(math.radians(elev_deg)), math.sin(math.radians(elev_deg))

    def rot(p):
        x, y, z = p
        # v0.39.11: ВАЖНО. Наши рендеры мобов (GeoMobRenderer/MorokRenderer/WendigoRenderer) НЕ делают
        # ванильный pose.scale(-1,-1,1), поэтому в мире +Y модели смотрит ВВЕРХ (y=0 — земля под
        # лапами). Превью раньше рисовало +Y вниз (как «до разворота» в ванилле) и показывало
        # правильных мобов перевёрнутыми. Зеркалим Y здесь, чтобы лист совпадал с игрой.
        y = -y
        x1 = x * ca + z * sa
        z1 = -x * sa + z * ca
        y2 = y * ce - z1 * se
        z2 = y * se + z1 * ce
        return (x1, y2, z2)

    rot_cubes = []
    for cb in cubes:
        rot_cubes.append((cb, rot(cb["center"])))
    xs = [c[1][0] for c in rot_cubes]
    ys = [c[1][1] for c in rot_cubes]
    ox = (min(xs) + max(xs)) / 2
    oy = (min(ys) + max(ys)) / 2
    span = max(max(xs) - min(xs), max(ys) - min(ys), 1.0)
    zoom = zoom * min(img_w, img_h) * 0.66 / span

    zbuf = [[-1e9] * img_w for _ in range(img_h)]
    img = [[bg] * img_w for _ in range(img_h)]
    light = norm((0.55, -0.62, -0.56))

    for cb, c in rot_cubes:
        w, h, d = cb["size"]
        ox0, oy0, oz0 = cb["origin"]
        u, v = cb["uv"]
        rects = face_rects(u, v, w, h, d)
        for fname, verts, nrm in FACES:
            wn = rot(nrm)
            if dot(wn, (0, 0, -1)) <= 0.02:      # смотрит от камеры — не рисуем
                continue
            lam = max(0.0, dot(norm(wn), (-light[0], -light[1], -light[2])))
            shade = 0.78 + 0.85 * lam
            quad = []
            for (fx, fy, fz) in verts:
                p = (ox0 + fx * w, oy0 + fy * h, oz0 + fz * d)
                quad.append(rot(p))
            depth = sum(p[2] for p in quad) / 4.0
            pts = [((p[0] - ox) * zoom + img_w / 2, (p[1] - oy) * zoom + img_h / 2) for p in quad]
            rx, ry, rw2, rh2 = rects[fname]
            # UV углов грани (для попиксельной выборки текстуры)
            quad_uv = [(rx, ry), (rx + rw2, ry), (rx + rw2, ry + rh2), (rx, ry + rh2)]

            x0 = max(0, int(math.floor(min(p[0] for p in pts))))
            x1 = min(img_w - 1, int(math.ceil(max(p[0] for p in pts))))
            y0 = max(0, int(math.floor(min(p[1] for p in pts))))
            y1 = min(img_h - 1, int(math.ceil(max(p[1] for p in pts))))
            # две треугольные грани с попиксельной текстурой
            for tri in ((0, 1, 2), (0, 2, 3)):
                a, b, c = (pts[i] for i in tri)
                t0, t1, t2 = (quad_uv[i] for i in tri)
                det = (b[0] - a[0]) * (c[1] - a[1]) - (c[0] - a[0]) * (b[1] - a[1])
                if abs(det) < 1e-9:
                    continue
                tx0 = max(0, int(math.floor(min(a[0], b[0], c[0]))))
                tx1 = min(img_w - 1, int(math.ceil(max(a[0], b[0], c[0]))))
                ty0 = max(0, int(math.floor(min(a[1], b[1], c[1]))))
                ty1 = min(img_h - 1, int(math.ceil(max(a[1], b[1], c[1]))))
                for yy in range(ty0, ty1 + 1):
                    for xx in range(tx0, tx1 + 1):
                        w0 = ((b[0] - xx) * (c[1] - yy) - (c[0] - xx) * (b[1] - yy)) / det
                        w1 = ((c[0] - xx) * (a[1] - yy) - (a[0] - xx) * (c[1] - yy)) / det
                        w2 = 1.0 - w0 - w1
                        if w0 < -0.002 or w1 < -0.002 or w2 < -0.002:
                            continue
                        if depth <= zbuf[yy][xx]:
                            continue
                        u = w0 * t0[0] + w1 * t1[0] + w2 * t2[0]
                        v = w0 * t0[1] + w1 * t1[1] + w2 * t2[1]
                        col = tex_sample(tex, u, v)
                        zbuf[yy][xx] = depth
                        img[yy][xx] = tuple(min(255, int(ch * shade)) for ch in col)
    return img


def main():
    geo = json.loads(GEO.read_text(encoding="utf-8"))
    tex = read_png(TEX)
    cubes = []
    for b in geo["bones"]:
        for c in b["cubes"]:
            ox, oy, oz = c["origin"]
            w, h, d = c["size"]
            cubes.append({
                "origin": (ox, oy, oz), "size": (w, h, d), "uv": (c["uv"][0], c["uv"][1]),
                "center": (ox + w / 2, oy + h / 2, oz + d / 2),
            })

    xs_all = [c["center"][0] for c in cubes]
    ys_all = [c["center"][1] for c in cubes]
    zs_all = [c["center"][2] for c in cubes]
    print(f"  габарит: X {min(xs_all):.0f}..{max(xs_all):.0f}  Y {min(ys_all):.0f}..{max(ys_all):.0f}  Z {min(zs_all):.0f}..{max(zs_all):.0f}")

    SHEET = "--sheet" in sys.argv

    views = [
        ("сбоку (side)", 90.0, 8.0),
        ("три четверти (3/4)", 128.0, 18.0),
        ("сверху-спереди (top)", 160.0, 34.0),
    ]
    if SHEET:
        views = [
            ("сбоку (side)", 90.0, 4.0),
            ("три четверти (3/4)", 140.0, 22.0),
            ("сверху (top)", 90.0, 88.0),
            ("спереди (front)", 180.0, 8.0),
        ]
    cols = 2 if SHEET else SX
    rows = 2 if SHEET else SY
    W, H = TILE_W * cols, TILE_H * rows
    img = [[BG] * W for _ in range(H)]
    for idx, (_label, az, el) in enumerate(views):
        tile = render_view(tex, cubes, az, el, SCALE, TILE_W, TILE_H, BG)
        tx = (idx % cols) * TILE_W
        ty = (idx // cols) * TILE_H
        for y in range(TILE_H):
            row = img[ty + y]
            trow = tile[y]
            for x in range(TILE_W):
                row[tx + x] = trow[x]
        # тонкая рамка тайла
        for x in range(TILE_W):
            img[ty][tx + x] = (44, 50, 60)
            img[ty + TILE_H - 1][tx + x] = (44, 50, 60)
        for y in range(TILE_H):
            img[ty + y][tx] = (44, 50, 60)

    if SHEET:
        # 2x2: три ракурса + атлас текстур в четвёртой ячейке
        W2, H2 = TILE_W * 2, TILE_H * 2
        sheet = [[(26, 32, 40)] * W2 for _ in range(H2)]
        order = [0, 1, 2, 3]
        for idx in order:
            sx0 = (idx % cols) * TILE_W
            sy0 = (idx // cols) * TILE_H
            for y in range(TILE_H):
                for x in range(TILE_W):
                    sheet[sy0 + y][sx0 + x] = img[sy0 + y][sx0 + x]
        for x in range(W2):
            sheet[TILE_H][x] = (52, 60, 72)
            sheet[0][x] = (52, 60, 72)
        for y in range(H2):
            sheet[y][TILE_W] = (52, 60, 72)
            sheet[y][0] = (52, 60, 72)
        out_sheet = ROOT / f"reports/{NAME}_model_sheet.png"
        write_png(out_sheet, sheet)
        print(f"model sheet: {out_sheet.relative_to(ROOT)}  (сбоку / 3-4 / сверху / спереди + атлас)")

    write_png(OUT, img)
    print(f"preview: {OUT.relative_to(ROOT)}  кубоидов={len(cubes)}  атлас={len(tex[0])}x{len(tex)}")


if __name__ == "__main__":
    main()
