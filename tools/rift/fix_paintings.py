#!/usr/bin/env python3
"""Переделать картины Элисара: ровно 16x16 (один блок) и текстура 16x16.

Задача:
  * полотно каждой картины занимает ровно один блок (16x16) и прижато к стене;
  * рамка и гвоздь рисуются в тех же пределах блока (ничего не торчит);
  * текстура самого полотна ужимается до 16x16 — «слишком большое разрешение»
    было 64x64 и 128x64;
  * модель переписывается в том же формате OBJ, что и исходная.

Геометрия строится с нуля: полотно — квадрат 1x1 в плоскости стены,
рамка — четыре бруска по краю, гвоздь — маленький кубик сверху по центру.

Запуск:
    python3 tools/rift/fix_paintings.py
"""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '.pylibs'))

OBJ_DIR = 'mod/src/main/resources/assets/riftworld/models/block/obj'
TEX_DIR = 'mod/src/main/resources/assets/riftworld/textures/block/obj'

# Имя картины -> текстура полотна.
PAINTINGS = {
    'painting_islands': 'm_paint_islands',
    'painting_portrait': 'm_paint_portrait',
    'painting_eye': 'm_paint_eye',
}

# Геометрия в единицах блока (1.0 = блок).
FRAME = 0.06        # ширина рамки
FACE = 0.56         # Z полотна (ближе к стене — 0, дальше — к зрителю)
Z_WALL = 0.5
CANVAS_Z = Z_WALL + FACE * 0.5
NAIL = 0.03         # половина ребра гвоздя


def box(x0, y0, z0, x1, y1, z1):
    """Возвращает 6 граней (каждая — 4 вершины) для параллелепипеда."""
    v = [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0),
         (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]
    # Обход против часовой стрелки снаружи -> нормали наружу.
    quads = [
        (0, 3, 2, 1),  # -Z
        (4, 5, 6, 7),  # +Z
        (0, 1, 5, 4),  # -Y
        (3, 7, 6, 2),  # +Y
        (0, 4, 7, 3),  # -X
        (1, 2, 6, 5),  # +X
    ]
    return [(v[i], v[j], v[k], v[l]) for i, j, k, l in quads]


class ObjBuilder:
    """Собирает OBJ из готовых четырёхугольников.

    Для полотна UV растягивается на всю грань 0..1 — так текстура 16x16
    попадает ровно на квадрат 1x1 блока. Для рамки и гвоздя берётся
    «уголок» текстуры (0..0.25), чтобы не было растянутых полос.
    """

    # UV-квадрат для обычных (не полотенных) материалов.
    TILE = (0.0, 0.0, 0.25, 0.25)

    def __init__(self):
        self.verts = []
        self.uvs = []
        self.groups = []   # (material, [[(vi, u, v), ...], ...])

    def add(self, material, quads, full_uv=False):
        """quads — список квадов; каждый квад — кортеж из 4 вершин (x, y, z)."""
        faces = []
        for quad in quads:
            pts = []
            for (x, y, z) in quad:
                self.verts.append((round(x, 5), round(y, 5), round(z, 5)))
                pts.append(len(self.verts))
            face = []
            for k, vi in enumerate(pts):
                if full_uv:
                    # 0..1 по всей грани: левый-нижний -> (0,1) и т.д.
                    u = 0.0 if k in (0, 3) else 1.0
                    v = 0.0 if k in (0, 1) else 1.0
                else:
                    u0, v0, u1, v1 = self.TILE
                    u = u0 if k in (0, 3) else u1
                    v = v0 if k in (0, 1) else v1
                self.uvs.append((round(u, 5), round(v, 5)))
                face.append((vi, len(self.uvs)))
            faces.append(face)
        self.groups.append((material, faces))

    def write(self, path, name):
        lines = ['mtllib %s.mtl' % name, 'o %s' % name]
        for v in self.verts:
            lines.append('v %.5f %.5f %.5f' % v)
        for t in self.uvs:
            lines.append('vt %.5f %.5f' % t)
        for mat, faces in self.groups:
            lines.append('usemtl %s' % mat)
            for face in faces:
                lines.append('f ' + ' '.join('%d/%d/%d' % (vi, ti, vi) for vi, ti in face))
        open(path, 'w', encoding='utf-8').write('\n'.join(lines) + '\n')


def build_painting(name, tex):
    """Строит картину: полотно 1x1, рамка по краю, гвоздь сверху."""
    b = ObjBuilder()

    # --- полотно: плоскость 1x1 у стены
    canvas_quad = [((0.0, 0.0, CANVAS_Z), (1.0, 0.0, CANVAS_Z),
                    (1.0, 1.0, CANVAS_Z), (0.0, 1.0, CANVAS_Z))]
    b.add('e_paint_' + tex.split('m_paint_')[-1], canvas_quad, full_uv=True)

    # --- рамка: четыре бруска по периметру полотна
    z0 = CANVAS_Z - 0.02
    z1 = CANVAS_Z + 0.02
    dark = []
    for bounds in (
            (0.0, 0.0, z0, 1.0, FRAME, z1),                       # низ
            (0.0, 1.0 - FRAME, z0, 1.0, 1.0, z1),                 # верх
            (0.0, FRAME, z0, FRAME, 1.0 - FRAME, z1),             # лево
            (1.0 - FRAME, FRAME, z0, 1.0, 1.0 - FRAME, z1)):       # право
        dark.extend(box(*bounds))
    b.add('e_wood_dark', dark)

    # подложка чуть шире — тёмная окантовка поверх светлой рамы
    light = []
    for bounds in (
            (-0.02, -0.02, z0 - 0.01, 1.02, FRAME + 0.02, z1 + 0.01),
            (-0.02, 1.0 - FRAME - 0.02, z0 - 0.01, 1.02, 1.02, z1 + 0.01),
            (-0.02, FRAME, z0 - 0.01, FRAME + 0.02, 1.0 - FRAME, z1 + 0.01),
            (1.0 - FRAME - 0.02, FRAME, z0 - 0.01, 1.02, 1.0 - FRAME, z1 + 0.01)):
        light.extend(box(*bounds))
    b.add('e_wood', light)

    # --- гвоздь/крючок по центру верхней стороны
    b.add('e_iron', box(0.5 - NAIL, 1.0 - FRAME, CANVAS_Z - 0.01,
                        0.5 + NAIL, 1.0 - FRAME + NAIL, CANVAS_Z + 0.03))
    return b


def shrink_texture(tex_name, size=16):
    """Ужимает текстуру полотна до 16x16, сохраняя пропорции рисунка."""
    from PIL import Image
    path = os.path.join(TEX_DIR, tex_name + '.png')
    im = Image.open(path).convert('RGBA')
    if im.size == (size, size):
        return False
    w, h = im.size
    scale = size / max(w, h)
    new = (max(1, round(w * scale)), max(1, round(h * scale)))
    im = im.resize(new, Image.LANCZOS)
    canvas = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    canvas.paste(im, ((size - new[0]) // 2, (size - new[1]) // 2))
    canvas.save(path)
    return True


def main():
    for name, tex in PAINTINGS.items():
        builder = build_painting(name, tex)
        builder.write(os.path.join(OBJ_DIR, name + '.obj'), name)
        changed = shrink_texture(tex)
        print('%-18s построена заново (1x1 блок)  текстура %s' % (
            name, 'ужата до 16x16' if changed else 'уже была 16x16'))


if __name__ == '__main__':
    main()
