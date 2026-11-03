#!/usr/bin/env python3
"""
Быстрый превью-рендер модели Лиоплевродона без Minecraft: берёт geo/liopleurodon.json
и атлас, рисует кубоиды изометрией с раскраской по среднему цвету грани из UV.
Нужен только для глазной проверки силуэта (`reports/liopleurodon_model_preview.png`).
"""
import json
import pathlib
import struct
import zlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
GEO = ROOT / "mod/src/main/resources/assets/riftworld/geo/liopleurodon.json"
TEX = ROOT / "mod/src/main/resources/assets/riftworld/textures/entity/liopleurodon.png"
OUT = ROOT / "reports/liopleurodon_model_preview.png"

W, H = 1100, 460


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
    path.write_bytes(out)


def face_rects(u, v, w, h, d):
    return {
        "up": (u + d, v, w, d),
        "down": (u + d + w, v, w, d),
        "east": (u, v + d, d, h),
        "north": (u + d, v + d, w, h),
        "west": (u + d + w, v + d, d, h),
        "south": (u + d + w + d, v + d, w, h),
    }


def avg(tex, r):
    x, y, w, h = r
    if w <= 0 or h <= 0:
        return (80, 90, 100)
    tot = [0, 0, 0]
    n = 0
    for yy in range(y, min(y + h, len(tex))):
        for xx in range(x, min(x + w, len(tex[0]))):
            px = tex[yy][xx]
            tot[0] += px[0]; tot[1] += px[1]; tot[2] += px[2]
            n += 1
    return tuple(c // max(1, n) for c in tot)


def main():
    geo = json.loads(GEO.read_text(encoding="utf-8"))
    tex = read_png(TEX)
    img = [[(26, 32, 40)] * W for _ in range(H)]

    # камера: изометрия сбоку, длинная ось (z) — горизонтально, нос влево
    cx, cy = W / 2, H / 2 + 30
    sc = 5.4

    def project(x, y, z):
        # y в модели растёт ВНИЗ; x — поперёк, z — вдоль (нос в отрицательном z)
        px = cx + (z * 1.0 + x * 0.34) * sc
        py = cy + (y * 0.92 - x * 0.26) * sc
        return px, py

    faces = []
    for b in geo["bones"]:
        for c in b["cubes"]:
            ox, oy, oz = c["origin"]
            w, h, d = c["size"]
            u, v = c["uv"]
            rects = face_rects(u, v, w, h, d)
            face_defs = [
                ("up", [(ox, oy, oz), (ox + w, oy, oz), (ox + w, oy, oz + d), (ox, oy, oz + d)], 0.30),
                ("down", [(ox, oy + h, oz), (ox + w, oy + h, oz), (ox + w, oy + h, oz + d), (ox, oy + h, oz + d)], 0.02),
                ("east", [(ox + w, oy, oz), (ox + w, oy, oz + d), (ox + w, oy + h, oz + d), (ox + w, oy + h, oz)], 0.20),
                ("west", [(ox, oy, oz), (ox, oy, oz + d), (ox, oy + h, oz + d), (ox, oy + h, oz)], 0.12),
                ("north", [(ox, oy, oz), (ox + w, oy, oz), (ox + w, oy + h, oz), (ox, oy + h, oz)], 0.16),
                ("south", [(ox, oy, oz + d), (ox + w, oy, oz + d), (ox + w, oy + h, oz + d), (ox, oy + h, oz + d)], 0.08),
            ]
            for name, quad, shade in face_defs:
                col = avg(tex, rects[name])
                col = tuple(min(255, int(c * (0.62 + shade))) for c in col)
                pts = [project(*p) for p in quad]
                # чем больше проекция «в глубину», тем раньше рисуем
                depth = -(sum(q[2] for q in quad) / 4) * 0.45 + (sum(q[0] for q in quad) / 4) * 0.9
                faces.append((depth, pts, col))
    faces.sort(key=lambda t: t[0])

    def fill_poly(pts, col):
        xs = [p[0] for p in pts]; ys = [p[1] for p in pts]
        x0, x1 = int(min(xs)), int(max(xs))
        y0, y1 = int(min(ys)), int(max(ys))
        n = len(pts)
        for yy in range(max(0, y0), min(H, y1 + 1)):
            for xx in range(max(0, x0), min(W, x1 + 1)):
                inside = False
                j = n - 1
                for i in range(n):
                    xi, yi = pts[i]; xj, yj = pts[j]
                    if ((yi > yy) != (yj > yy)) and (xx < (xj - xi) * (yy - yi) / (yj - yi + 1e-9) + xi):
                        inside = not inside
                    j = i
                if inside:
                    img[yy][xx] = col

    for depth, pts, col in faces:
        fill_poly(pts, col)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    write_png(OUT, img)
    print("preview:", OUT.relative_to(ROOT), f"{len(faces)} граней")


if __name__ == "__main__":
    main()
