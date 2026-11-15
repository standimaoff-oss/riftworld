#!/usr/bin/env python3
"""Сгенерировать текстуры двух новых руд рифт-кристалла.

Пользователь попросил «добавить 2 руду рифт кристалла». Делаем две:

  * deepslate_riftcrystal_ore — сланцевый вариант (глубже, темнее,
    кристаллы тусклые): встречается в глубоком камне Элисара;
  * riftcrystal_ore_glow    — «светящаяся» жила: кристаллы яркие и
    светятся (блок даёт свет), встречается редко, в разломах.

Обе текстуры 16x16, в той же палитре, что и исходная riftcrystal_ore.

Запуск:
    python3 tools/rift/gen_riftcrystal_ores.py
"""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '.pylibs'))

from PIL import Image

TEX = 'mod/src/main/resources/assets/riftworld/textures/block'


def scale_rgb(rgb, k):
    return tuple(max(0, min(255, round(c * k))) for c in rgb)


def deepslate_variant(src, dst):
    """Глубоководный вариант: общий тон темнее, кристаллы тусклее."""
    im = Image.open(src).convert('RGBA')
    out = []
    for r, g, b, a in im.getdata():
        # Кристаллы в исходнике — холодные сине-фиолетовые и светлее фона.
        bright = (r + g + b) / 3.0
        if bright > 60:
            # кристалл: гасим, но оставляем холодный оттенок
            out.append(scale_rgb((r, g, b), 0.78) + (a,))
        else:
            # порода: сильнее темнеет и уходит в холод
            out.append(scale_rgb((r, g, b), 0.62) + (a,))
    canvas = Image.new('RGBA', im.size)
    canvas.putdata(out)
    canvas.save(dst)


def glow_variant(src, dst):
    """Светящаяся жила: кристаллы ярче и с бирюзовым отливом."""
    im = Image.open(src).convert('RGBA')
    out = []
    for r, g, b, a in im.getdata():
        bright = (r + g + b) / 3.0
        if bright > 60:
            lift = min(255, round(bright * 1.7))
            out.append((min(255, round(r * 1.5 + 30)),
                        min(255, round(g * 1.7 + 40)),
                        min(255, round(b * 1.5 + 35)), a))
        else:
            # порода остаётся тёмной, но чуть холоднее
            out.append(scale_rgb((r, g, b), 0.88) + (a,))
    canvas = Image.new('RGBA', im.size)
    canvas.putdata(out)
    canvas.save(dst)


def main():
    src = os.path.join(TEX, 'riftcrystal_ore.png')
    deepslate_variant(src, os.path.join(TEX, 'deepslate_riftcrystal_ore.png'))
    glow_variant(src, os.path.join(TEX, 'riftcrystal_ore_glow.png'))
    for name in ('deepslate_riftcrystal_ore.png', 'riftcrystal_ore_glow.png'):
        im = Image.open(os.path.join(TEX, name))
        print('  %-32s %s' % (name, im.size))


if __name__ == '__main__':
    main()
