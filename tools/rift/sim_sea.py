#!/usr/bin/env python3
"""Рев 10 «Живое море 2»: оффлайн-симуляция генератора моря Элисара.

Это точный порт математики `ElisarSea.java` (те же шумы, пороги и порядок операций),
поэтому цифры здесь — это цифры настоящей игры. Проверяет и рисует:

  * floor.png    — карта кромки дна (дюны, желоба, абиссальные впадины)
  * reef.png     — пятна рифов
  * caves.png    — сколько воды/воздуха вырезано под дном в колонке
  * holes.png    — карта «синих дыр»
  * slice.png    — вертикальный разрез: порода / затопленные пещеры / воздушные карманы / вода / сад
  * slice2.png   — второй разрез (другой z), для сравнения

и печатает статистику, по которой видно, что пещеры действительно ЗАТОПЛЕНЫ,
а не стоят сухими столбами (баг Рев 8: 93% объёма было воздухом).

usage: python3 tools/rift/sim_sea.py [--out DIR] [--size N]
"""
import math
import os
import sys

from PIL import Image

OCEAN = 62
BOTTOM = -62            # ElisarSea.BOTTOM
DEEPEST_FLOOR = -34     # ElisarSea.DEEPEST_FLOOR
N = 256

M64 = 0xFFFFFFFFFFFFFFFF


def _mix(h):
    h &= M64
    h ^= h >> 33
    h = (h * 0xFF51AFD7ED558CCD) & M64
    h ^= h >> 33
    h = (h * 0xC4CEB9FE1A85EC53) & M64
    h ^= h >> 33
    return (h >> 11) * (2.0 ** -53)


def _s64(v):
    v &= M64
    return v - (1 << 64) if v >= (1 << 63) else v


C1, C2, C3, C4 = 0x9E3779B97F4A7C15, 0xC2B2AE3D27D4EB4F, 0x165667B19E3779F9, 0xD6E8FEB86659FD93


def hash2(x, z, s):
    return _mix(_s64(x * C1) ^ _s64(z * C2) ^ _s64(s * C3))


def hash3(x, y, z, s):
    return _mix(_s64(x * C1) ^ _s64(y * C4) ^ _s64(z * C2) ^ _s64(s * C3))


def smooth(t):
    return t * t * (3 - 2 * t)


def vnoise(x, z, s):
    xi, zi = math.floor(x), math.floor(z)
    fx, fz = smooth(x - xi), smooth(z - zi)
    a, b = hash2(xi, zi, s), hash2(xi + 1, zi, s)
    c, d = hash2(xi, zi + 1, s), hash2(xi + 1, zi + 1, s)
    ab, cd = a + (b - a) * fx, c + (d - c) * fx
    return ab + (cd - ab) * fz


def vnoise3(x, y, z, s):
    xi, yi, zi = math.floor(x), math.floor(y), math.floor(z)
    fx, fy, fz = smooth(x - xi), smooth(y - yi), smooth(z - zi)
    c000, c100 = hash3(xi, yi, zi, s), hash3(xi + 1, yi, zi, s)
    c010, c110 = hash3(xi, yi + 1, zi, s), hash3(xi + 1, yi + 1, zi, s)
    c001, c101 = hash3(xi, yi, zi + 1, s), hash3(xi + 1, yi, zi + 1, s)
    c011, c111 = hash3(xi, yi + 1, zi + 1, s), hash3(xi + 1, yi + 1, zi + 1, s)
    x00, x10 = c000 + (c100 - c000) * fx, c010 + (c110 - c010) * fx
    x01, x11 = c001 + (c101 - c001) * fx, c011 + (c111 - c011) * fx
    y0, y1 = x00 + (x10 - x00) * fy, x01 + (x11 - x01) * fy
    return y0 + (y1 - y0) * fz


# ------------------------------------------------------------------ рельеф
def reefness(x, z):
    n = vnoise(x * 0.028, z * 0.028, 421) * 0.7 + vnoise(x * 0.075, z * 0.075, 422) * 0.3
    return max(0.0, min(1.0, (n - 0.54) / 0.24))


def trench(x, z):
    n = vnoise(x * 0.011, z * 0.011, 423) * 0.72 + vnoise(x * 0.035, z * 0.035, 424) * 0.28
    if n < 0.58:
        return 0
    t = (n - 0.58) / 0.42
    return round(t * t * 16.0)


def pit(x, z):
    n = vnoise(x * 0.0055, z * 0.0055, 481) * 0.8 + vnoise(x * 0.019, z * 0.019, 482) * 0.2
    if n < 0.70:
        return 0
    t = (n - 0.70) / 0.30
    return round(t * t * 22.0)


def floor_y(x, z, ocean=OCEAN):
    n = (vnoise(x * 0.016, z * 0.016, 77) * 0.66 + vnoise(x * 0.052, z * 0.052, 78) * 0.26
         + vnoise(x * 0.075, z * 0.075, 79) * 0.08)
    f = ocean - 45 - round(n * 13)
    f -= trench(x, z)
    f -= pit(x, z)
    return max(DEEPEST_FLOOR, f)


# ------------------------------------------------------------------ пещеры
def cave_value(x, y, z, fl, step=1.0):
    deep = max(0.0, min(1.0, (fl - y) / 58.0))
    s = 0.028 + 0.012 * deep
    w1 = vnoise3(x * s, y * 0.048, z * s, 431)
    w2 = vnoise3(x * s, y * 0.048, z * s, 432)
    d = math.hypot(w1 - 0.5, w2 - 0.5)
    width = 0.086 + 0.070 * deep + 0.034 * vnoise3(x * 0.008, y * 0.011, z * 0.008, 433)
    tunnel = width - d
    if tunnel > 0:
        return tunnel
    hall = vnoise3(x * 0.022, y * 0.038, z * 0.022, 434)
    return (hall - (0.745 - 0.045 * deep)) * 0.5


def air_pocket(x, y, z):
    return vnoise3(x * 0.043, y * 0.062, z * 0.043, 435) > 0.70


def ceiling_pocket(x, y, z):
    return vnoise3(x * 0.043, y * 0.062, z * 0.043, 435) > 0.585


def cave(x, y, z, fl):
    if y <= BOTTOM + 1 or y > fl - 3:
        return False
    return cave_value(x, y, z, fl) > 0


def blue_hole(x, y, z, fl):
    gx, gz = x // 26, z // 26
    if hash2(gx, gz, 441) < 0.40:
        return False
    cx = gx * 26 + 5 + hash2(gx, gz, 442) * 16
    cz = gz * 26 + 5 + hash2(gx, gz, 443) * 16
    r = 2.0 + hash2(gx, gz, 444) * 2.4
    depth = 12 + int(hash2(gx, gz, 445) * 14)
    d = math.hypot(x + 0.5 - cx, z + 0.5 - cz)
    if y > fl or y < fl - depth - 6:
        return False
    if d <= r:
        return True
    return y < fl - depth and d <= r + 3.8 and y > fl - depth - 6


def column_kind(x, z, fl):
    """Возвращает (вода, воздух, порода) в колонке — ровно как ElisarSea.column()."""
    wet = air = 0
    hole_column = False
    for y in range(BOTTOM, fl - 2):
        shaft = blue_hole(x, y, z, fl)
        if not shaft and not cave(x, y, z, fl):
            continue
        if shaft:
            hole_column = True
        open_above = y + 1 > fl - 3 or (not cave(x, y + 1, z, fl) and not blue_hole(x, y + 1, z, fl))
        pocket = (not blue_hole(x, y, z, fl)) and (air_pocket(x, y, z) or (open_above and ceiling_pocket(x, y, z)))
        if pocket:
            air += 1
        else:
            wet += 1
    return wet, air, hole_column


def main():
    out = "/tmp/sea_sim"
    if "--out" in sys.argv:
        out = sys.argv[sys.argv.index("--out") + 1]
    global N
    if "--size" in sys.argv:
        N = int(sys.argv[sys.argv.index("--size") + 1])
    os.makedirs(out, exist_ok=True)

    floor_img = Image.new("RGB", (N, N))
    reef_img = Image.new("RGB", (N, N))
    cave_img = Image.new("RGB", (N, N))
    hole_img = Image.new("RGB", (N, N))
    fp, rp, cp, hp = floor_img.load(), reef_img.load(), cave_img.load(), hole_img.load()

    floors = [[0] * N for _ in range(N)]
    reef = [[0.0] * N for _ in range(N)]
    stats = {"rock": 0, "wet": 0, "air": 0, "cave_cols": 0, "hole_cols": 0, "reef_cols": 0, "steps": 0, "edges": 0}
    minf, maxf = 999, -999

    for x in range(N):
        for z in range(N):
            f = floor_y(x, z)
            floors[z][x] = f
            reef[z][x] = reefness(x, z)
            minf, maxf = min(minf, f), max(maxf, f)
            if reef[z][x] > 0.5:
                stats["reef_cols"] += 1
            if x + 1 < N:
                stats["edges"] += 1
                if floors[z][x + 1] != f:
                    stats["steps"] += 1
            wet, air, holecol = column_kind(x, z, f)
            stats["rock"] += max(0, f - BOTTOM)
            stats["wet"] += wet
            stats["air"] += air
            if wet + air >= 8:
                stats["cave_cols"] += 1
            if holecol:
                stats["hole_cols"] += 1
                hp[x, z] = (90, 220, 220)
            else:
                hp[x, z] = (12, 18, 26)
            depth = OCEAN - f
            fp[x, z] = (int(20 + (f + 34) * 3.2), int(70 + depth * 1.4), int(140 + depth * 1.2))
            rp[x, z] = (int(40 + reef[z][x] * 200), 60, 90)
            v = min(255, (wet + air) * 9)
            cp[x, z] = (v, int(v * 0.7), int(air * 14))

    floor_img.resize((N * 2, N * 2), Image.NEAREST).save(f"{out}/floor.png")
    reef_img.resize((N * 2, N * 2), Image.NEAREST).save(f"{out}/reef.png")
    cave_img.resize((N * 2, N * 2), Image.NEAREST).save(f"{out}/caves.png")
    hole_img.resize((N * 2, N * 2), Image.NEAREST).save(f"{out}/holes.png")

    # ---------------------------------------------------------------- разрезы
    def slice_at(zc, path):
        top, bot = OCEAN + 4, BOTTOM - 2
        h = top - bot + 1
        img = Image.new("RGB", (N, h))
        px = img.load()
        for x in range(N):
            f = floors[zc][x] if zc < N else floor_y(x, zc)
            for y in range(bot, top + 1):
                yy = top - y
                if y > OCEAN:
                    c = (16, 18, 26)
                elif y > f:
                    c = (46, 96, 158)
                elif y == f:
                    c = (222, 150, 160) if reef[zc][x] > 0.5 else (204, 190, 150)
                else:
                    if blue_hole(x, y, zc, f):
                        c = (74, 170, 210)
                    elif cave(x, y, zc, f):
                        open_above = y + 1 > f - 3 or (not cave(x, y + 1, zc, f) and not blue_hole(x, y + 1, zc, f))
                        c = (170, 150, 90) if (open_above and air_pocket(x, y, zc)) else (40, 120, 170)
                    else:
                        c = (92, 88, 104)
                px[x, yy] = c
        img.resize((N * 2, h * 3), Image.NEAREST).save(path)

    slice_at(N // 2, f"{out}/slice.png")
    slice_at(min(N - 1, N // 2 + 37), f"{out}/slice2.png")

    total = N * N
    cave_vol = max(1, stats["wet"] + stats["air"])
    print(f"дно: y {minf}..{maxf}  → воды {OCEAN - maxf}..{OCEAN - minf} блоков")
    print(f"породы под дном: {BOTTOM}..{minf} (минимум {-minf + BOTTOM} блоков в самой глубокой впадине)")
    print(f"перепад дна: {stats['steps'] * 100.0 / stats['edges']:.1f}% рёбер (было 6.8%)")
    print(f"рифовые колонки: {stats['reef_cols'] * 100.0 / total:.1f}%")
    print(f"колонки с пещерами (>=8): {stats['cave_cols'] * 100.0 / total:.1f}%")
    print(f"колонки с синей дырой: {stats['hole_cols'] * 100.0 / total:.1f}%")
    print(f"вырезано от породы: {(stats['wet'] + stats['air']) * 100.0 / max(1, stats['rock']):.1f}%")
    print(f"  из них ВОДА: {stats['wet'] * 100.0 / cave_vol:.1f}%   ВОЗДУХ (карманы): {stats['air'] * 100.0 / cave_vol:.1f}%")
    print(f"воздуха под дном в среднем на колонку: {stats['air'] / total:.1f} блоков")
    print("карты:", out)
    bad = []
    if minf < DEEPEST_FLOOR:
        bad.append("дно провалилось ниже DEEPEST_FLOOR")
    if stats["wet"] == 0:
        bad.append("пещеры не затоплены")
    if stats["air"] * 100.0 / cave_vol > 35:
        bad.append("слишком много воздуха в пещерах (должно быть <=35%)")
    print("ПРОВАЛЫ: " + (", ".join(bad) if bad else "нет"))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
