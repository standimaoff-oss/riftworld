#!/usr/bin/env python3
"""Рев 8: новая вода Элисара — глубокая синева вместо молочной плёнки.

Пишет две анимированные текстуры:
  elisar_water_still.png  16x256 (16 кадров 16x16)
  elisar_water_flow.png   32x512 (16 кадров 32x32)
Палитра: от бирюзового блика на гребне до тёмно-синего в толще.
"""
import math
import os
import random

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "mod/src/main/resources/assets/riftworld/textures/block")
FRAMES = 16


def _hash(x, y, seed):
    h = (x * 374761393 + y * 668265263 + seed * 2246822519) & 0xFFFFFFFF
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0


def _vnoise(x, y, seed):
    xi, yi = math.floor(x), math.floor(y)
    fx, fy = x - xi, y - yi
    fx = fx * fx * (3 - 2 * fx)
    fy = fy * fy * (3 - 2 * fy)
    a, b = _hash(xi, yi, seed), _hash(xi + 1, yi, seed)
    c, d = _hash(xi, yi + 1, seed), _hash(xi + 1, yi + 1, seed)
    ab = a + (b - a) * fx
    cd = c + (d - c) * fx
    return ab + (cd - ab) * fy


def wave_frame(size, t, seed, deep):
    """Кадр воды: органическое поле из трёх октав шума, медленно плывущее по кадру.

    Никаких решёток — только мягкие пятна блика и тени, поэтому вода не «плиткой».
    """
    im = Image.new("RGBA", (size, size))
    px = im.load()
    ph = t / float(FRAMES)
    lo = (35, 92, 138) if deep else (38, 100, 146)       # глубина
    mid = (44, 112, 160)                                 # толща
    hi = (86, 168, 204)                                  # блик (мягкий, без резкой сетки)
    for y in range(size):
        for x in range(size):
            u = (x / size) * 2.0 + ph * 0.55
            v = (y / size) * 2.0 + ph * 0.30
            n = _vnoise(u * 2.0, v * 2.0, seed) * 0.60
            n += _vnoise(u * 4.0 + 3.1, v * 4.0 - 1.7, seed + 7) * 0.28
            n += _vnoise(u * 8.0 - 2.3, v * 8.0 + 5.4, seed + 13) * 0.12
            n = max(0.0, min(1.0, (n - 0.30) / 0.55))
            # мягкая кривая: много спокойной воды, редкие яркие гребни
            k = n ** 1.7
            if k < 0.55:
                f = k / 0.55
                r = int(lo[0] + (mid[0] - lo[0]) * f)
                g = int(lo[1] + (mid[1] - lo[1]) * f)
                b = int(lo[2] + (mid[2] - lo[2]) * f)
            else:
                f = (k - 0.55) / 0.45
                r = int(mid[0] + (hi[0] - mid[0]) * f)
                g = int(mid[1] + (hi[1] - mid[1]) * f)
                b = int(mid[2] + (hi[2] - mid[2]) * f)
            px[x, y] = (r, g, b, 255)
    return im


def main():
    for name, size, seed, deep in (("elisar_water_still", 16, 71, False), ("elisar_water_flow", 32, 72, True)):
        sheet = Image.new("RGBA", (size, size * FRAMES))
        for t in range(FRAMES):
            sheet.paste(wave_frame(size, t, seed + t, deep), (0, t * size))
        path = os.path.join(OUT, name + ".png")
        sheet.save(path)
        with open(path + ".mcmeta", "w", encoding="utf-8") as f:
            f.write('{"animation": {"frametime": 4, "interpolate": true}}\n')
        print("  ", name, sheet.size)


if __name__ == "__main__":
    main()
