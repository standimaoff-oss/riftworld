#!/usr/bin/env python3
"""Текстуры листвы и коры для деревьев Элисара (16x16, ванильный стиль).

Рев 11.18: прежние листья были почти белыми с 17% прозрачных пикселей — издалека крона
читалась как «серая каша в дырках». Здесь у каждого вида свой цвет, четыре тона и
умеренные дырки (10–12%), а кора получила тёмные борозды для объёма.

usage: python3 tools/rift/gen_tree_textures.py
"""
import os
import random

from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), '..', '..',
                   'mod', 'src', 'main', 'resources', 'assets', 'riftworld', 'textures', 'block')

# палитра: тень -> блик, плюс доля каждого тона
LEAVES = {
    'sizo_leaves': {           # «сизо» — сине-сиреневая листва
        'tones': [(0x3b, 0x46, 0x74), (0x4c, 0x5a, 0x92), (0x63, 0x74, 0xb0), (0x86, 0x96, 0xcd)],
        'weights': [30, 34, 24, 12],
        'holes': 0.10,
    },
    'ambra_leaves': {          # «амбра» — тёплая янтарная листва
        'tones': [(0x6f, 0x4c, 0x1c), (0x8f, 0x66, 0x27), (0xb4, 0x86, 0x3a), (0xd8, 0xad, 0x5e)],
        'weights': [30, 34, 24, 12],
        'holes': 0.10,
    },
}

LOGS = {
    'sizo_log': {              # кора сизо: холодный серо-оливковый
        'base': (0x6b, 0x67, 0x58), 'dark': (0x47, 0x45, 0x3a), 'light': (0x8b, 0x86, 0x74),
    },
    'ambra_log': {             # кора амбры: тёплое дерево
        'base': (0x6a, 0x4c, 0x33), 'dark': (0x4a, 0x33, 0x21), 'light': (0x8c, 0x6a, 0x48),
    },
}


def pick_leaf(rng, spec):
    tones, weights = spec['tones'], spec['weights']
    return rng.choices(tones, weights=weights, k=1)[0]


def gen_leaves(name, spec, seed):
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    # «кустики»: несколько плотных пятен, между ними просветы — так листва читается объёмом
    clusters = [(rng.randint(0, 15), rng.randint(0, 15)) for _ in range(7)]
    for x in range(16):
        for y in range(16):
            near = min(abs(x - cx) + abs(y - cy) for cx, cy in clusters)
            hole = rng.random() < spec['holes'] * (1.0 if near > 3 else 0.35)
            if hole:
                continue
            t = pick_leaf(rng, spec)
            # тень в нижней части кустика, блик — сверху
            k = -12 if near <= 2 else 0
            r = max(0, min(255, t[0] + k + rng.randint(-8, 8)))
            g = max(0, min(255, t[1] + k + rng.randint(-8, 8)))
            b = max(0, min(255, t[2] + k + rng.randint(-8, 8)))
            px[x, y] = (r, g, b, 255)
    img.save(os.path.join(OUT, name + '.png'))


def gen_bark(name, spec, seed, top=False):
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    if top:
        # спил: годовые кольца
        for x in range(16):
            for y in range(16):
                d = max(abs(x - 7.5), abs(y - 7.5))
                ring = int(d) % 2
                t = spec['light'] if ring else spec['base']
                px[x, y] = (t[0] + rng.randint(-6, 6), t[1] + rng.randint(-6, 6), t[2] + rng.randint(-6, 6), 255)
        img.save(os.path.join(OUT, name + '_top.png'))
        return
    # борозды: вертикальные линии с рваным краем
    grooves = {2, 3, 8, 12, 13}
    for x in range(16):
        groove = x in grooves
        for y in range(16):
            if groove and rng.random() < 0.85:
                t = spec['dark']
            elif rng.random() < 0.18:
                t = spec['light']
            else:
                t = spec['base']
            px[x, y] = (max(0, min(255, t[0] + rng.randint(-10, 10))),
                        max(0, min(255, t[1] + rng.randint(-10, 10))),
                        max(0, min(255, t[2] + rng.randint(-10, 10))), 255)
    img.save(os.path.join(OUT, name + '.png'))


def main():
    for i, (name, spec) in enumerate(sorted(LEAVES.items())):
        gen_leaves(name, spec, 1000 + i)
        print('leaves', name)
    for i, (name, spec) in enumerate(sorted(LOGS.items())):
        gen_bark(name, spec, 2000 + i)
        gen_bark(name, spec, 2000 + i, top=True)
        print('bark  ', name)


if __name__ == '__main__':
    main()
