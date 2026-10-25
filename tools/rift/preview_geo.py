#!/usr/bin/env python3
"""Оффлайн-превью geo-моделей рыб: ортографический рендер по JSON + текстуре.

Зачем: каждая проверка модели «в живом клиенте» — это ~20 минут CI. Этот скрипт рисует те же
коробки с теми же UV на Python за секунду, поэтому форму можно поправить ДО прогона.

usage: python3 tools/rift/preview_geo.py [--yaw 25] [--pitch 18] [--out /tmp/preview]
"""
import argparse
import json
import math
import os

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
A = os.path.join(ROOT, 'mod', 'src', 'main', 'resources', 'assets', 'riftworld')

NAMES = ['silver', 'gold', 'striped', 'jelly', 'ray', 'angler', 'eel', 'surgeon', 'turtle', 'gloom', 'sting']


def faces(rect, w, h, d):
    """Шесть прямоугольников развёртки в координатах атласа (как в gen_geo_fish)."""
    u, v = rect
    return {
        'up': (u + d, v, u + d + w, v + d),
        'down': (u + d + w, v, u + d + w + w, v + d),
        'east': (u, v + d, u + d, v + d + h),
        'north': (u + d, v + d, u + d + w, v + d + h),
        'west': (u + d + w, v + d, u + d + w + d, v + d + h),
        'south': (u + d + w + d, v + d, u + d + w + d + w, v + d + h),
    }


# углы каждой грани в локальных координатах коробки (x0..x1, y0..y1, z0..z1)
def corners(face, x0, y0, z0, x1, y1, z1):
    if face == 'up':
        return [(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)]
    if face == 'down':
        return [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)]
    if face == 'north':
        return [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)]
    if face == 'south':
        return [(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]
    if face == 'west':
        return [(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)]
    return [(x1, y0, z0), (x1, y0, z1), (x1, y1, z1), (x1, y1, z0)]


NORMALS = {'up': (0, -1, 0), 'down': (0, 1, 0), 'north': (0, 0, -1),
           'south': (0, 0, 1), 'west': (-1, 0, 0), 'east': (1, 0, 0)}


def rot(p, yaw, pitch):
    x, y, z = p
    # В клиенте +Y смотрит ВВЕРХ (проверено кадром 11.22: щупальца при y=+3 торчали над куполом),
    # а в координатах коробок JSON «вниз» — это +Y. Поэтому для превью инвертируем Y.
    y = -y
    cy, sy = math.cos(yaw), math.sin(yaw)
    x, z = x * cy - z * sy, x * sy + z * cy
    cp, sp = math.cos(pitch), math.sin(pitch)
    y, z = y * cp - z * sp, y * sp + z * cp
    return x, y, z


def render(name, yaw_deg=25.0, pitch_deg=18.0, size=420, bg=(24, 40, 60), axis=False):
    model = json.load(open(os.path.join(A, 'geo', name + '.json'), encoding='utf-8'))
    if axis:
        # маркеры ориентации: красный куб на y=-16 (должен быть СВЕРХУ), зелёный на y=+16 (снизу)
        model = json.loads(json.dumps(model))
        model['bones'] += [
            {'name': 'axisUp', 'pivot': [0, 0, 0],
             'cubes': [{'origin': [-1, -17, -1], 'size': [2, 2, 2], 'uv': [0, 0]}]},
            {'name': 'axisDown', 'pivot': [0, 0, 0],
             'cubes': [{'origin': [-1, 17, -1], 'size': [2, 2, 2], 'uv': [0, 0]}]},
        ]
    tex = Image.open(os.path.join(A, 'textures', 'entity', name + '.png')).convert('RGBA')
    if axis:
        tex = tex.copy()
        tex.putpixel((1, 1), (255, 0, 0, 255))
    tpx = tex.load()
    tw, th = tex.size
    yaw, pitch = math.radians(yaw_deg), math.radians(pitch_deg)

    quads = []
    for bone in model['bones']:
        marker = None
        if bone['name'] == 'axisUp':
            marker = (255, 60, 60)
        elif bone['name'] == 'axisDown':
            marker = (60, 255, 60)
        for c in bone.get('cubes', []):
            ox, oy, oz = c['origin']
            w, h, d = c['size']
            fs = faces(c['uv'], w, h, d)
            for face, (u0, v0, u1, v1) in fs.items():
                cw, ch = u1 - u0, v1 - v0
                base = corners(face, ox, oy, oz, ox + w, oy + h, oz + d)
                # свет: верх светлее, низ темнее
                n = NORMALS[face]
                lum = 1.0 + 0.18 * (-n[1]) + 0.10 * (-n[2]) - 0.10 * abs(n[0])
                for i in range(cw):
                    for j in range(ch):
                        col = tpx[u0 + i, v0 + j]
                        if marker is not None:
                            col = (marker[0], marker[1], marker[2], 255)
                        if col[3] < 24:
                            continue
                        def bil(a, b):
                            # билинейная точка внутри грани по (a,b) в [0,1]
                            top = [base[0][k] + (base[1][k] - base[0][k]) * a for k in range(3)]
                            bot = [base[3][k] + (base[2][k] - base[3][k]) * a for k in range(3)]
                            return [top[k] + (bot[k] - top[k]) * b for k in range(3)]
                        a0, a1 = i / cw, (i + 1) / cw
                        b0, b1 = j / ch, (j + 1) / ch
                        vs = [bil(a0, b0), bil(a1, b0), bil(a1, b1), bil(a0, b1)]
                        rotv = [rot(v, yaw, pitch) for v in vs]
                        depth = sum(v[2] for v in rotv) / 4.0
                        shade = tuple(max(0, min(255, int(ch_ * lum))) for ch_ in col[:3])
                        quads.append((depth, rotv, shade))
    quads.sort(key=lambda q: -q[0])

    # экран
    xs = [v[0] for _, vs, _ in quads for v in vs]
    ys = [v[1] for _, vs, _ in quads for v in vs]
    if not xs:
        raise SystemExit('пустая модель ' + name)
    cx, cy = (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2
    span = max(max(xs) - min(xs), max(ys) - min(ys)) or 1.0
    k = size * 0.86 / span

    img = Image.new('RGB', (size, size), bg)
    dr = ImageDraw.Draw(img)
    for _, vs, col in quads:
        pts = [((v[0] - cx) * k + size / 2, (v[1] - cy) * k + size / 2) for v in vs]
        dr.polygon(pts, fill=col)
    return img


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--yaw', type=float, default=25.0)
    ap.add_argument('--pitch', type=float, default=18.0)
    ap.add_argument('--out', default='/tmp/geo_preview')
    ap.add_argument('--axis', action='store_true')
    ap.add_argument('names', nargs='*', default=None)
    a = ap.parse_args()
    names = a.names or NAMES
    os.makedirs(a.out, exist_ok=True)
    imgs = []
    for n in names:
        im = render(n, a.yaw, a.pitch, axis=a.axis)
        im.save(os.path.join(a.out, n + '.png'))
        imgs.append((n, im))
        print('preview', n)
    # контактный лист 4 в ряд
    cols = 4
    rows = (len(imgs) + cols - 1) // cols
    sheet = Image.new('RGB', (cols * 420, rows * 420), (12, 18, 26))
    for i, (_, im) in enumerate(imgs):
        sheet.paste(im, ((i % cols) * 420, (i // cols) * 420))
    sheet.save(os.path.join(a.out, 'sheet.png'))
    print('sheet:', os.path.join(a.out, 'sheet.png'))


if __name__ == '__main__':
    main()
