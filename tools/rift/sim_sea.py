#!/usr/bin/env python3
"""Оффлайн-симуляция генератора моря Элисара (порт математики ElisarSea.java).

Считает те же шумы/пороги и печатает статистику + рисует карты 256x256:
  * depth.png  — глубина дна (ущелья)
  * reef.png   — пятна рифов
  * caves.png  — плотность пещер по колонке (сколько воздуха под дном)
  * holes.png  — карта «синих дыр»
Плюс проверяет, что дно не проваливается ниже y=9 и что пещеры реально есть.

usage: python3 tools/rift/sim_sea.py [--out DIR]
"""
import math
import os
import sys

from PIL import Image

OCEAN = 62
N = 512          # размер карты в блоках (переопределяется --size)


def _mix(h):
    h &= 0xFFFFFFFFFFFFFFFF
    h ^= h >> 33
    h = (h * 0xFF51AFD7ED558CCD) & 0xFFFFFFFFFFFFFFFF
    h ^= h >> 33
    h = (h * 0xC4CEB9FE1A85EC53) & 0xFFFFFFFFFFFFFFFF
    h ^= h >> 33
    return (h >> 11) * (2.0 ** -53)


def _s64(v):
    v &= 0xFFFFFFFFFFFFFFFF
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


def reefness(x, z):
    n = vnoise(x * 0.028, z * 0.028, 421) * 0.7 + vnoise(x * 0.075, z * 0.075, 422) * 0.3
    return max(0.0, min(1.0, (n - 0.56) / 0.24))


def trench(x, z):
    n = vnoise(x * 0.011, z * 0.011, 423) * 0.75 + vnoise(x * 0.035, z * 0.035, 424) * 0.25
    return 0 if n < 0.60 else round((n - 0.60) / 0.40 * 12.0)


def ocean_floor(x, z):
    n = vnoise(x * 0.016, z * 0.016, 77) * 0.72 + vnoise(x * 0.052, z * 0.052, 78) * 0.28
    return max(9, OCEAN - 1 - 41 - round(n * 8) - trench(x, z))


def cave(x, y, z, floor):
    if y < 1 or y > floor - 2:
        return False
    w1 = vnoise3(x * 0.055, y * 0.085, z * 0.055, 431)
    w2 = vnoise3(x * 0.055, y * 0.085, z * 0.055, 432)
    d = math.hypot(w1 - 0.5, w2 - 0.5)
    width = 0.082 + 0.065 * vnoise3(x * 0.012, y * 0.02, z * 0.012, 433)
    if d < width:
        return True
    return vnoise3(x * 0.030, y * 0.055, z * 0.030, 434) > 0.742 and y > 2


def blue_hole(x, y, z, floor):
    gx, gz = x // 26, z // 26
    if hash2(gx, gz, 441) < 0.42:
        return False
    cx = gx * 26 + 5 + hash2(gx, gz, 442) * 16
    cz = gz * 26 + 5 + hash2(gx, gz, 443) * 16
    r = 2.0 + hash2(gx, gz, 444) * 2.2
    depth = 9 + int(hash2(gx, gz, 445) * 9)
    d = math.hypot(x + 0.5 - cx, z + 0.5 - cz)
    if y > floor or y < floor - depth - 5:
        return False
    if d <= r:
        return True
    return y < floor - depth and d <= r + 3.5 and y > floor - depth - 5


def main():
    out = "/tmp/sea_sim"
    if "--out" in sys.argv:
        out = sys.argv[sys.argv.index("--out") + 1]
    global N
    if "--size" in sys.argv:
        N = int(sys.argv[sys.argv.index("--size") + 1])
    os.makedirs(out, exist_ok=True)

    floors = [[0] * N for _ in range(N)]
    reefs = [[0.0] * N for _ in range(N)]
    depth_img = Image.new("RGB", (N, N))
    reef_img = Image.new("RGB", (N, N))
    cave_img = Image.new("RGB", (N, N))
    hole_img = Image.new("RGB", (N, N))
    hole_ph = hole_img.load()

    air_total = 0
    cave_cols = 0
    hole_cols = 0
    reef_cols = 0
    min_floor, max_floor = 999, -999
    for x in range(N):
        for z in range(N):
            f = ocean_floor(x, z)
            floors[z][x] = f
            reefs[z][x] = reefness(x, z)
            min_floor, max_floor = min(min_floor, f), max(max_floor, f)
            if reefs[z][x] > 0.5:
                reef_cols += 1
            air = 0
            hole_here = False
            for y in range(1, f):
                if cave(x, y, z, f):
                    air += 1
                if blue_hole(x, y, z, f):
                    air += 1
                    hole_here = True
            air_total += air
            if air >= 8:
                cave_cols += 1
            if hole_here:
                hole_cols += 1
                hole_ph[x, z] = (90, 220, 220)
            depth_img.putpixel((x, z), (int(30 + (f - 9) * 6), 70, int(120 + (62 - f) * 2)))
            reef_img.putpixel((x, z), (int(40 + reefs[z][x] * 200), 60, 90))
            v = min(255, air * 12)
            cave_img.putpixel((x, z), (v, int(v * 0.7), 40))

    # вертикальный разрез по z = N/2: порода / пещеры / вода / сад
    slice_img = Image.new("RGB", (N, 68))
    sp = slice_img.load()
    zc = N // 2
    for x in range(N):
        f = ocean_floor(x, zc)
        for y in range(68):
            yy = 67 - y                     # сверху вниз: воздух, вода, порода
            if yy > OCEAN:
                c = (18, 20, 26)
            elif yy > f:
                c = (44, 90, 150)
            else:
                c = (96, 92, 110)
                if any(cave(x, yy, zc + dz, f) or blue_hole(x, yy, zc + dz, f) for dz in (-4, -2, 0, 2, 4)):
                    c = (16, 12, 22)
                elif yy == f:
                    r = reefness(x, zc)
                    c = (222, 130, 150) if r > 0.5 else (170, 160, 130)
            sp[x, 67 - yy] = c
    slice_img.resize((N * 2, 68 * 4), Image.NEAREST).save(f"{out}/slice.png")
    depth_img.resize((N * 2, N * 2), Image.NEAREST).save(f"{out}/depth.png")
    reef_img.resize((N * 2, N * 2), Image.NEAREST).save(f"{out}/reef.png")
    cave_img.resize((N * 2, N * 2), Image.NEAREST).save(f"{out}/caves.png")
    hole_img.resize((N * 2, N * 2), Image.NEAREST).save(f"{out}/holes.png")

    total = N * N
    print(f"дно: y {min_floor}..{max_floor} (ущелья до {OCEAN - min_floor} блоков воды)")
    print(f"рифовые колонки: {reef_cols * 100.0 / total:.1f}%")
    print(f"колонки с пещерами (>=8 воздуха): {cave_cols * 100.0 / total:.1f}%")
    print(f"колонки с синей дырой: {hole_cols * 100.0 / total:.1f}%")
    print(f"воздуха под дном в среднем на колонку: {air_total / total:.1f} блоков")
    print("карты:", out)

    fails = []
    if min_floor < 9:
        fails.append("дно ниже y=9")
    if cave_cols * 100.0 / total < 5:
        fails.append("пещер слишком мало")
    if reef_cols * 100.0 / total < 3:
        fails.append("рифов слишком мало")
    if hole_cols == 0:
        fails.append("нет синих дыр")
    print("ПРОВАЛЫ:", fails if fails else "нет")
    return 1 if fails else 0




# ------------------------------------------------------------------ подбор точек для съёмки
def pick_spots(size=192, origin=(600, 600), write=None):
    """Возвращает координаты «красивых» мест для скриншотов и RCON-проб."""
    ox, oz = origin
    best_reef, best_reef_v = None, -1
    best_deep, best_deep_v = None, 999
    holes = []
    caves = []
    for x in range(ox, ox + size):
        for z in range(oz, oz + size):
            f = ocean_floor(x, z)
            r = reefness(x, z)
            if r > best_reef_v:
                best_reef_v, best_reef = r, (x, z, f)
            if f < best_deep_v:
                best_deep_v, best_deep = f, (x, z, f)
            if len(holes) < 400:
                for y in range(f - 1, max(1, f - 22), -1):
                    if blue_hole(x, y, z, f):
                        holes.append((x, z, f, y))
                        break
            if len(caves) < 400:
                air = [y for y in range(3, f - 1) if cave(x, y, z, f)]
                if len(air) >= 6 and f - air[-1] >= 6:      # есть полость с запасом породы сверху
                    caves.append((x, z, f, air[0], air[-1]))
    spots = {}
    spots["reef"] = {"x": best_reef[0], "z": best_reef[1], "floor": best_reef[2]}
    spots["deep"] = {"x": best_deep[0], "z": best_deep[1], "floor": best_deep[2]}
    print(f"REEF={best_reef[0]} {best_reef[2] + 3} {best_reef[1]} REEFV={best_reef_v:.2f}")
    print(f"DEEP={best_deep[0]} {best_deep[2] + 4} {best_deep[1]} FLOOR={best_deep[2]}")
    if holes:
        hx, hz, hf, hy = holes[len(holes) // 2]
        spots["hole"] = {"x": hx, "z": hz, "floor": hf, "top": hy}
        print(f"HOLE={hx} {hy} {hz} FLOOR={hf}")
    if caves:
        cx, cz, cf, lo, hi = caves[len(caves) // 2]
        spots["cave"] = {"x": cx, "z": cz, "floor": cf, "low": lo, "high": hi}
        print(f"CAVE={cx} {lo + 1} {cz} LOW={lo} HIGH={hi} FLOOR={cf}")
    # центр области и вертикальная колонка для проб
    # виста рифа: центр самого большого пятна рифа (максимум суммы «рифовости» по 13x13)
    best_vista, best_vista_v = None, -1
    for x in range(ox, ox + size, 3):
        for z in range(oz, oz + size, 3):
            if reefness(x, z) < 0.6:
                continue
            acc = 0.0
            for dx in range(-6, 7, 2):
                for dz in range(-6, 7, 2):
                    acc += reefness(x + dx, z + dz)
            if acc > best_vista_v:
                best_vista_v, best_vista = acc, (x, z, ocean_floor(x, z))
    if best_vista:
        spots["reef_vista"] = {"x": best_vista[0], "z": best_vista[1], "floor": best_vista[2]}
        print(f"VISTA={best_vista[0]} {best_vista[2]} {best_vista[1]} SCORE={best_vista_v:.1f}")

    # ламинария: колонки с густым лесом (там, где reefness низкая, а шум ламинарии высокий)
    best_kelp, best_kelp_v = None, -1
    for x in range(ox, ox + size):
        for z in range(oz, oz + size):
            if reefness(x, z) >= 0.35:
                continue
            if vnoise(x * 0.05, z * 0.05, 465) <= 0.66:
                continue
            cnt = sum(1 for dx in range(-4, 5) for dz in range(-4, 5)
                      if reefness(x + dx, z + dz) < 0.35 and vnoise((x + dx) * 0.05, (z + dz) * 0.05, 465) > 0.66)
            if cnt > best_kelp_v:
                best_kelp_v, best_kelp = cnt, (x, z, ocean_floor(x, z))
    if best_kelp:
        spots["kelp"] = {"x": best_kelp[0], "z": best_kelp[1], "floor": best_kelp[2]}
        print(f"KELP={best_kelp[0]} {best_kelp[2]} {best_kelp[1]} N={best_kelp_v}")

    spots["origin"] = {"x": ox + size // 2, "z": oz + size // 2}
    cx, cz = ox + size // 2, oz + size // 2
    f = ocean_floor(cx, cz)
    spots["column"] = {"x": cx, "z": cz, "floor": f, "water_bottom": f + 1, "water_top": OCEAN}
    print(f"COLUMN={cx} {cz} FLOOR={f} WATER={f + 1}..{OCEAN}")
    if write:
        import json
        with open(write, "w", encoding="utf-8") as fh:
            json.dump(spots, fh, ensure_ascii=False, indent=2)
        print("spots ->", write)


if __name__ == "__main__":
    if "--spots" in sys.argv:
        wr = None
        if "--json" in sys.argv:
            wr = sys.argv[sys.argv.index("--json") + 1]
        pick_spots(write=wr)
    else:
        sys.exit(main())
