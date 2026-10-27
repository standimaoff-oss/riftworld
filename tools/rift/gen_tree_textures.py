#!/usr/bin/env python3
"""Текстуры листвы и коры для деревьев Элисара (16x16, ванильный стиль).

Рев 12: вернули исходную БЕЛУЮ/ХОЛОДНУЮ гамму. В 11.18 листву увели в сине-сиреневый и
янтарно-оранжевый — на скриншотах деревья перестали читаться как «белые деревья Элисара»
и спорили с рифом. Теперь у обоих видов светлая холодная листва: у сизо — белая с
голубоватой тенью, у амбры — костяная с тёплой тенью (различие в подтоне, а не в яркости).
Дырки умеренные (6–8%), чтобы крона не превращалась в «кашу в дырках».

usage: python3 tools/rift/gen_tree_textures.py
"""
import os
import random

from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), '..', '..',
                   'mod', 'src', 'main', 'resources', 'assets', 'riftworld', 'textures', 'block')

# палитра: тень -> блик, плюс доля каждого тона
LEAVES = {
    'sizo_leaves': {           # «сизо» — белая листва с холодной голубой тенью
        'tones': [(0xa8, 0xb6, 0xc4), (0xc6, 0xd2, 0xdd), (0xdd, 0xe5, 0xec), (0xf2, 0xf6, 0xf9)],
        'weights': [20, 32, 30, 18],
        'holes': 0.07,
    },
    'ambra_leaves': {          # «амбра» — костяная листва, тень тёплая (янтарь только в подтоне)
        'tones': [(0xb2, 0xab, 0x9c), (0xcf, 0xc8, 0xb8), (0xe6, 0xe1, 0xd6), (0xf7, 0xf4, 0xee)],
        'weights': [20, 32, 30, 18],
        'holes': 0.07,
    },
}

LOGS = {
    'sizo_log': {              # кора сизо: светлая серо-голубая, как выбеленное морем дерево
        'base': (0x96, 0xa1, 0xab), 'dark': (0x6e, 0x79, 0x83), 'light': (0xba, 0xc3, 0xcb),
    },
    'ambra_log': {             # кора амбры: светлое костяное дерево
        'base': (0xa5, 0x9d, 0x8e), 'dark': (0x7b, 0x74, 0x66), 'light': (0xc6, 0xbf, 0xb1),
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
            r = max(0, min(255, t[0] + k + rng.randint(-6, 6)))
            g = max(0, min(255, t[1] + k + rng.randint(-6, 6)))
            b = max(0, min(255, t[2] + k + rng.randint(-6, 6)))
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
