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


def wave_frame(size, t, seed, deep):
    """Один кадр воды: полосы волн + мелкие блики, без «молочной» заливки."""
    rnd = random.Random(seed)
    im = Image.new("RGBA", (size, size))
    px = im.load()
    for y in range(size):
        for x in range(size):
            # две интерферирующие волны, медленно ползущие по кадру
            a = math.sin((x / size) * math.tau * 2 + t * math.tau / FRAMES)
            b = math.sin((y / size) * math.tau * 3 - t * math.tau / FRAMES * 1.5)
            c = math.sin(((x + y) / size) * math.tau * 1.5 + t * math.tau / FRAMES * 0.5)
            v = (a + b + c) / 3.0
            # база: тёмно-синяя бездна -> синий -> бирюзовый блик
            base = (34, 96, 152) if not deep else (26, 78, 130)
            top = (92, 176, 214)
            k = max(0.0, min(1.0, (v + 1) * 0.5))
            r = int(base[0] + (top[0] - base[0]) * k)
            g = int(base[1] + (top[1] - base[1]) * k)
            bl = int(base[2] + (top[2] - base[2]) * k)
            if k > 0.86:                                   # редкие яркие блики на гребне
                r, g, bl = min(255, r + 26), min(255, g + 22), min(255, bl + 16)
            if rnd.random() < 0.015:
                r, g, bl = min(255, r + 14), min(255, g + 12), min(255, bl + 10)
            px[x, y] = (r, g, bl, 196)
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
