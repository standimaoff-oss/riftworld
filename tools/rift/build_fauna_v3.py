#!/usr/bin/env python3
"""v0.45.0 - rebuilt reference fauna: Pegasus (winged horse) + Liopleurodon.

The two models are rebuilt from the new AI reference sheets in
``reports/reference_fauna/refs_v2`` with a denser, rounded cuboid construction:

* cross-sections are real ellipses sampled every few model units, so the
  silhouette is not a barrel made of six huge boxes;
* anatomical parts (shoulder/withers/haunch, knee, fetlock, hoof, muzzle,
  nostrils, lips, ears, eyelids, wing coverts/secondaries/primaries, dorsal
  keel, throat folds, flipper roots) are separate volumes;
* every surface is painted with vertical ambient gradients, fur/feather/scale
  detail and per-material palettes instead of flat fills;
* the rigs expose the joints the client animation actually bends.

Source coordinates: Y up, nose toward -Z, one unit = 1/16 block, feet at y=0.
The game export mirrors Y (as the dedicated renderers do), so the in-game
model is upright.
"""
from __future__ import annotations

import hashlib
import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'mod/src/main/resources/assets/riftworld'
REPORT = ROOT / 'reports/reference_fauna'
REFS = REPORT / 'refs_v2'
REPORT.mkdir(parents=True, exist_ok=True)

# material -> (up, side/east-west, north-south/front, down, style)
MATS = {
    # ---- pegasus: ivory coat with cool grey shading, very restrained palette
    'coat':        ('#f6f5f1', '#e8e7e2', '#f1f0eb', '#cfd0cb', 'fur'),
    'coat_light':  ('#fcfbf7', '#f1f1ec', '#f8f7f2', '#dcddd8', 'fur'),
    'coat_shade':  ('#e6e6e1', '#d4d4cf', '#dededa', '#bcbdb8', 'fur'),
    'coat_warm':   ('#f7f4ec', '#eae6dc', '#f2eee5', '#d3cfc5', 'fur'),
    'mane':        ('#fbfaf6', '#f2f1ec', '#f8f7f2', '#deddd7', 'hair'),
    'mane_dark':   ('#eeece6', '#dedcd5', '#e7e5de', '#c8c6bf', 'hair'),
    'muzzle':      ('#cfcfc9', '#bdbdb7', '#c8c8c2', '#a7a8a2', 'smooth'),
    'nostril':     ('#4a4844', '#3a3835', '#423f3c', '#302e2b', 'smooth'),
    'lip':         ('#c3bdb6', '#aea8a1', '#bbb5ae', '#978f88', 'smooth'),
    'ear_inner':   ('#d9bdb6', '#c9a9a2', '#d3b4ad', '#b08f88', 'smooth'),
    'eye':         ('#1b2320', '#121916', '#151c19', '#0c1210', 'eye'),
    'eye_glint':   ('#f4f7f1', '#eef1eb', '#f2f5ef', '#dfe4dc', 'eye'),
    'hoof':        ('#4a4844', '#3a3835', '#43413d', '#2a2926', 'horn'),
    'wing':        ('#fbfaf6', '#eef0ec', '#f6f6f2', '#d4d9d5', 'feather'),
    'wing_mid':    ('#f4f4ef', '#e6e8e3', '#efefe9', '#cbd0cc', 'feather'),
    'wing_tip':    ('#f7f6f1', '#e3e5e0', '#efefe9', '#c4cac6', 'feather'),
    'wing_shadow': ('#e9ebe6', '#d8dbd6', '#e2e4df', '#bfc4c0', 'feather'),
    'wing_root':   ('#f2f2ed', '#e2e4df', '#eaeae5', '#c8ccc8', 'feather'),
    'saddle':      ('#7a5334', '#63432a', '#714c31', '#4a3220', 'leather'),
    'saddle_dark': ('#5b3d26', '#4a311e', '#553a24', '#382515', 'leather'),
    'leather':     ('#4f3724', '#3f2b1b', '#49321f', '#2e1f13', 'leather'),
    'gold':        ('#dfc07a', '#bf9c4f', '#d8b968', '#94773a', 'metal'),
    # ---- liopleurodon: cold slate back, pale mottled belly
    'back':        ('#5d7181', '#46596a', '#526576', '#374856', 'scale'),
    'back_dark':   ('#42535f', '#334450', '#3c4d59', '#26343e', 'scale'),
    'flank':       ('#536979', '#3f5261', '#4a5e6d', '#313f49', 'scale'),
    'flank_pale':  ('#8d9a9c', '#77858a', '#85939a', '#62707a', 'scale'),
    'belly':       ('#b4bdb8', '#9da8a6', '#a9b3b0', '#8b9694', 'scale'),
    'belly_shade': ('#98a3a0', '#87928f', '#919c99', '#76817f', 'scale'),
    'fin':         ('#5a7080', '#455868', '#506373', '#364654', 'scale'),
    'fin_edge':    ('#8e9d9f', '#7b8a8d', '#879699', '#67767a', 'scale'),
    'fin_pale':    ('#a8b0aa', '#939c98', '#9fa7a3', '#848d8a', 'scale'),
    'scar':        ('#c6bfa8', '#b3ac96', '#c0b9a2', '#9d9782', 'scar'),
    'gum':         ('#8a615c', '#74504c', '#805a56', '#63423f', 'smooth'),
    'mouth':       ('#3a2c2c', '#2c2121', '#332626', '#241a1a', 'smooth'),
    'tongue':      ('#95696a', '#7d5657', '#8b6161', '#6c4a4b', 'smooth'),
    'tooth':       ('#f0ead6', '#ddd6bf', '#e8e2cd', '#c3bca8', 'tooth'),
    'lio_eye':     ('#111b22', '#0c141a', '#0f171d', '#080e12', 'eye'),
    'glint':       ('#e8efe9', '#e2e9e3', '#e5ece6', '#d5dcd6', 'eye'),
}

FACE_ORDER = ('up', 'down', 'east', 'west', 'north', 'south')


def faces(u, v, w, h, d):
    """Minecraft/ModelPart box net rectangles for size (w, h, d)."""
    return {'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d),
            'east': (u, v + d, d, h), 'north': (u + d, v + d, w, h),
            'west': (u + d + w, v + d, d, h), 'south': (u + 2 * d + w, v + d, w, h)}


def rgb(color):
    return tuple(int(color[k:k + 2], 16) for k in (1, 3, 5))


def even(value, minimum=2):
    return max(minimum, 2 * int(round(value / 2.0)))


def ell_rows(center_y, half_h, half_w, step=3):
    """Integer boxes approximating one elliptical cross-section row by row."""
    out = []
    hh = max(2, int(round(half_h)))
    k = -hh
    while k < hh:
        y = center_y + k
        row_h = min(step, hh - k)
        f = min(1.0, abs(k + row_h / 2.0) / max(1.0, hh))
        width = even(half_w * math.sqrt(max(0.02, 1.0 - f * f)) * 2)
        out.append((int(round(y)), row_h, width, (k + row_h / 2.0) / max(1.0, hh)))
        k += row_h
    return out


class Model:
    def __init__(self, name, scale, max_atlas=2048):
        self.name, self.scale, self.max_atlas = name, scale, max_atlas
        self.bones, self.by_name, self.order = [], {}, 0

    def bone(self, name, pivot, parent=None, anim=None):
        assert name not in self.by_name and (parent is None or parent in self.by_name), (name, parent)
        bone = {'name': name, 'pivot': [int(round(p)) for p in pivot], 'parent': parent,
                'anim': anim, 'cubes': []}
        self.bones.append(bone)
        self.by_name[name] = bone
        return bone

    def box(self, bone, origin, size, material, variant=0):
        assert material in MATS, material
        assert len(origin) == len(size) == 3
        x, y, z = (int(round(v)) for v in origin)
        w, h, d = (max(1, int(round(v))) for v in size)
        assert w > 0 and h > 0 and d > 0
        self.by_name[bone]['cubes'].append({'origin': [x, y, z], 'size': [w, h, d],
                                            'material': material, 'variant': variant})
        return self.by_name[bone]['cubes'][-1]

    def sym(self, bone, x, y, z, w, h, d, material, variant=0):
        """Mirrored pair around x=0 (geometry only; textures stay readable)."""
        self.box(bone, [x, y, z], [w, h, d], material, variant)
        self.box(bone, [-x - w, y, z], [w, h, d], material, variant)

    # ---------------------------------------------------------------- bodies
    def tube(self, bone, sections, material_side, material_top=None, material_belly=None,
             step=3, mats_rows=True, material_trans=None):
        """Rounded body from (z, center_y, half_w, half_h) sections."""
        for i in range(len(sections) - 1):
            z0, y0, w0, h0 = sections[i]
            z1, y1, w1, h1 = sections[i + 1]
            depth = z1 - z0
            if depth <= 0:
                continue
            zc, yc, ww, hh = (z0 + z1) / 2.0, (y0 + y1) / 2.0, (w0 + w1) / 2.0, (h0 + h1) / 2.0
            for (row_y, row_h, width, rel) in ell_rows(yc, hh, ww, step):
                mat = material_side
                if mats_rows and rel > 0.52 and material_top:
                    mat = material_top
                elif mats_rows and rel < -0.34 and material_belly:
                    mat = material_belly
                elif mats_rows and rel < -0.20 and material_trans:
                    mat = material_trans
                self.box(bone, [-width // 2, row_y, z0], [width, row_h, depth], mat, 0)

    # ---------------------------------------------------------------- export
    def export(self):
        templates = {}
        for b in self.bones:
            for c in b['cubes']:
                key = (c['material'], tuple(c['size']), c['variant'])
                templates[key] = (2 * c['size'][2] + 2 * c['size'][0], c['size'][2] + c['size'][1])
        atlas = 512
        while True:
            skyline = [0] * atlas
            placed, failed = {}, False
            for key, (w0, h0) in sorted(templates.items(), key=lambda it: (-it[1][1], -it[1][0], str(it[0]))):
                w, h = w0 + 4, h0 + 4
                best = None
                for x in range(atlas - w + 1):
                    y = max(skyline[x:x + w])
                    if y + h <= atlas and (best is None or y < best[1]):
                        best = (x, y)
                        if y == 0:
                            break
                if best is None:
                    failed = True
                    break
                x, y = best
                for i in range(x, x + w):
                    skyline[i] = y + h
                placed[key] = (x + 2, y + 2)
            if not failed:
                break
            atlas *= 2
            if atlas > self.max_atlas:
                raise RuntimeError(f'{self.name}: texture budget exceeded ({atlas})')

        image = Image.new('RGBA', (atlas, atlas), (0, 0, 0, 0))
        draw = ImageDraw.Draw(image)
        for key, (u, v) in placed.items():
            mat, size, variant = key
            w, h, d = size
            seed = int.from_bytes(hashlib.sha256(repr(key).encode()).digest()[:8], 'big')
            paint_template(draw, MATS[mat], u, v, w, h, d, random.Random(seed), variant)

        source, runtime = [], []
        for b in self.bones:
            src_cubes, run_cubes = [], []
            for c in b['cubes']:
                x, y, z = c['origin']
                w, h, d = c['size']
                uv = list(placed[(c['material'], tuple(c['size']), c['variant'])])
                src_cubes.append({'origin': [x, y, z], 'size': [w, h, d], 'uv': uv})
                run_cubes.append({'origin': [x, -y - h, z], 'size': [w, h, d], 'uv': uv})
            src = {'name': b['name'], 'pivot': b['pivot'], 'cubes': src_cubes}
            dst = {'name': b['name'], 'pivot': [b['pivot'][0], -b['pivot'][1], b['pivot'][2]],
                   'cubes': run_cubes}
            if b['parent']:
                src['parent'] = dst['parent'] = b['parent']
            if b['anim']:
                src['anim'] = dict(b['anim'])
                an = dict(b['anim'])
                if an.get('axis') in ('x', 'z'):
                    an['amp'] = -an['amp']
                dst['anim'] = an
            source.append(src)
            runtime.append(dst)

        game = {'texture_width': atlas, 'texture_height': atlas, 'render_scale': self.scale,
                'translucent': False, 'bones': runtime}
        bb = {'format_version': '1.12.0', 'minecraft:geometry': [{
            'description': {'identifier': 'geometry.riftworld.' + self.name,
                            'texture_width': atlas, 'texture_height': atlas,
                            'visible_bounds_width': 18, 'visible_bounds_height': 10,
                            'visible_bounds_offset': [0, 1, 0]}, 'bones': source}]}
        (ASSETS / 'geo' / f'{self.name}.json').write_text(json.dumps(game, indent=1) + '\n')
        (ASSETS / 'textures/entity' / f'{self.name}.png').parent.mkdir(parents=True, exist_ok=True)
        image.save(ASSETS / 'textures/entity' / f'{self.name}.png', compress_level=9)
        (REPORT / f'{self.name}.blockbench.geo.json').write_text(json.dumps(bb, indent=1) + '\n')
        (REPORT / f'{self.name}.png').write_bytes((ASSETS / 'textures/entity' / f'{self.name}.png').read_bytes())

        cubes = [c for b in self.bones for c in b['cubes']]
        bounds = [[min(c['origin'][i] for c in cubes), max(c['origin'][i] + c['size'][i] for c in cubes)]
                  for i in range(3)]
        stats = {'bones': len(self.bones), 'cuboids': len(cubes), 'unique_uv_nets': len(placed),
                 'atlas': atlas, 'scale': self.scale, 'bounds_units': bounds,
                 'dimensions_blocks': [round((hi - lo) * self.scale / 16.0, 2) for lo, hi in bounds]}
        (REPORT / f'{self.name}.stats.json').write_text(json.dumps(stats, indent=2) + '\n')
        print(self.name, stats)
        return stats


# --------------------------------------------------------------------- texture painting
def shade(color, factor):
    return tuple(max(0, min(255, int(c * factor))) for c in color)


def paint_template(draw, palette, u, v, w, h, d, rng, variant):
    up_c, side_c, front_c, down_c, style = palette
    for face, (fx, fy, fw, fh) in faces(u, v, w, h, d).items():
        base = rgb({'up': up_c, 'down': down_c, 'north': front_c, 'south': front_c,
                    'east': side_c, 'west': side_c}[face])
        draw.rectangle((fx, fy, fx + fw - 1, fy + fh - 1), fill=(*base, 255))
        if style == 'feather':
            paint_feather(draw, fx, fy, fw, fh, base, rng)
        elif style == 'hair':
            paint_hair(draw, fx, fy, fw, fh, base, rng)
        elif style == 'fur':
            paint_fur(draw, fx, fy, fw, fh, base, rng)
        elif style == 'scale':
            paint_scale(draw, fx, fy, fw, fh, base, rng)
        elif style == 'scar':
            paint_scar(draw, fx, fy, fw, fh, base, rng)
        elif style == 'leather':
            paint_leather(draw, fx, fy, fw, fh, base, rng)
        elif style == 'metal':
            paint_metal(draw, fx, fy, fw, fh, base)
        elif style == 'horn':
            paint_horn(draw, fx, fy, fw, fh, base, rng)
        elif style == 'tooth':
            paint_tooth(draw, fx, fy, fw, fh, base, rng)
        elif style == 'eye':
            paint_eye(draw, fx, fy, fw, fh, base)
        else:
            paint_smooth(draw, fx, fy, fw, fh, base, rng)
        # every face gets a soft top-lit / bottom-shadowed gradient
        if face in ('east', 'west', 'north', 'south'):
            for row in range(fh):
                k = row / max(1, fh - 1)
                f = 1.028 - 0.062 * k
                draw.line((fx, fy + row, fx + fw - 1, fy + row), fill=(*shade(base, f), 255))
        elif face == 'up':
            for col in range(fw):
                f = 0.995 + 0.02 * math.sin(math.pi * col / max(1, fw - 1))
                draw.line((fx + col, fy, fx + col, fy + fh - 1), fill=(*shade(base, f), 255))
        else:
            for row in range(fh):
                f = 0.985 - 0.025 * row / max(1, fh - 1)
                draw.line((fx, fy + row, fx + fw - 1, fy + row), fill=(*shade(base, f), 255))
        if variant and style in ('fur', 'scale', 'feather') and min(fw, fh) > 3:
            # second variant: a slightly warmer/cooler cast keeps repeated parts
            # from reading as one duplicated stamp.
            f = 0.985 if variant % 2 else 1.012
            for row in range(fh):
                draw.line((fx, fy + row, fx + fw - 1, fy + row), fill=(*shade(base, f), 255))


def paint_fur(draw, fx, fy, fw, fh, base, rng):
    for _ in range(max(1, fw * fh // 26)):
        x = fx + rng.randrange(fw)
        y = fy + rng.randrange(fh)
        f = rng.choice((0.93, 0.96, 1.03, 1.06))
        draw.point((x, y), fill=(*shade(base, f), 255))
        if rng.random() < 0.35:
            draw.point((x, min(fy + fh - 1, y + 1)), fill=(*shade(base, f * 0.99), 255))


def paint_hair(draw, fx, fy, fw, fh, base, rng):
    for x in range(fx, fx + fw, 2):
        start = fy + rng.randrange(max(1, fh // 4))
        end = min(fy + fh - 1, start + max(2, int(fh * 0.65) + rng.randrange(3)))
        f = rng.choice((0.93, 0.97, 1.04))
        draw.line((x, start, x, end), fill=(*shade(base, f), 255))
        if x + 1 < fx + fw:
            draw.line((x + 1, start, x + 1, min(end, start + max(1, (end - start) // 2))),
                      fill=(*shade(base, f * 1.02), 255))


def paint_feather(draw, fx, fy, fw, fh, base, rng):
    # barbs run along the long axis of the feather box
    along_x = fw >= fh
    length = fw if along_x else fh
    for i in range(2, length - 1, 3):
        f = rng.choice((0.92, 0.95, 0.98))
        if along_x:
            y = fy + 1 + (i * 7 + rng.randrange(2)) % max(1, fh - 1)
            draw.line((fx + i, y, fx + i, min(fy + fh - 1, y + rng.randrange(2, 4))),
                      fill=(*shade(base, f), 255))
        else:
            x = fx + 1 + (i * 5 + rng.randrange(2)) % max(1, fw - 1)
            draw.line((x, fy + i, min(fx + fw - 1, x + rng.randrange(2, 3)), fy + i),
                      fill=(*shade(base, f), 255))
    # pale shaft down the middle
    if along_x and fh > 2:
        shaft = fy + fh // 2
        draw.line((fx + 1, shaft, fx + fw - 2, shaft), fill=(*shade(base, 1.045), 255))


def paint_scale(draw, fx, fy, fw, fh, base, rng):
    for _ in range(max(1, fw * fh // 30)):
        x = fx + rng.randrange(fw)
        y = fy + rng.randrange(fh)
        f = rng.choice((0.88, 0.92, 0.96, 1.02, 1.05))
        sw = 1 + rng.randrange(2)
        sh = 1 + rng.randrange(2)
        draw.rectangle((x, y, min(fx + fw - 1, x + sw), min(fy + fh - 1, y + sh)),
                       fill=(*shade(base, f), 255))
    # soft mottling between the back and the belly
    if fh > 4:
        for y in range(fy + 1, fy + fh, 3):
            if rng.random() < 0.35:
                f = rng.choice((0.9, 1.06))
                draw.line((fx, y, fx + fw - 1, y), fill=(*shade(base, f), 255))


def paint_scar(draw, fx, fy, fw, fh, base, rng):
    draw.rectangle((fx, fy, fx + fw - 1, fy + fh - 1), fill=(*base, 255))
    for _ in range(max(1, fw * fh // 12)):
        x = fx + rng.randrange(fw)
        y = fy + rng.randrange(fh)
        draw.point((x, y), fill=(*shade(base, rng.choice((0.88, 1.05))), 255))


def paint_leather(draw, fx, fy, fw, fh, base, rng):
    for _ in range(max(1, fw * fh // 22)):
        x = fx + rng.randrange(fw)
        y = fy + rng.randrange(fh)
        draw.point((x, y), fill=(*shade(base, rng.choice((0.88, 0.93, 1.06))), 255))
    if fh > 3:
        draw.line((fx, fy, fx + fw - 1, fy), fill=(*shade(base, 1.12), 255))
        draw.line((fx, fy + fh - 1, fx + fw - 1, fy + fh - 1), fill=(*shade(base, 0.78), 255))


def paint_metal(draw, fx, fy, fw, fh, base):
    for y in range(fy, fy + fh):
        f = 0.90 + 0.22 * (0.5 + 0.5 * math.sin((y - fy) * 1.7))
        draw.line((fx, y, fx + fw - 1, y), fill=(*shade(base, f), 255))
    draw.line((fx, fy, fx + fw - 1, fy), fill=(*shade(base, 1.25), 255))


def paint_horn(draw, fx, fy, fw, fh, base, rng):
    for y in range(fy, fy + fh):
        f = 0.92 + 0.18 * (1.0 - (y - fy) / max(1, fh - 1))
        draw.line((fx, y, fx + fw - 1, y), fill=(*shade(base, f), 255))
    draw.line((fx, fy, fx + fw - 1, fy), fill=(*shade(base, 1.2), 255))
    for x in range(fx + 1, fx + fw, 3):
        draw.line((x, fy + 1, x, fy + max(1, fh // 2)), fill=(*shade(base, 0.85), 255))


def paint_tooth(draw, fx, fy, fw, fh, base, rng):
    for y in range(fy, fy + fh):
        f = 1.0 - 0.08 * (y - fy) / max(1, fh - 1)
        draw.line((fx, y, fx + fw - 1, y), fill=(*shade(base, f), 255))
    for _ in range(max(1, fw * fh // 18)):
        draw.point((fx + rng.randrange(fw), fy + rng.randrange(fh)),
                   fill=(*shade(base, rng.choice((0.94, 1.04))), 255))


def paint_eye(draw, fx, fy, fw, fh, base):
    draw.rectangle((fx, fy, fx + fw - 1, fy + fh - 1), fill=(*base, 255))
    if fw > 1 and fh > 1:
        draw.point((fx + fw - 2, fy + 1), fill=(238, 244, 236, 255))
    draw.line((fx, fy, fx + fw - 1, fy), fill=(*shade(base, 1.35), 255))


def paint_smooth(draw, fx, fy, fw, fh, base, rng):
    for _ in range(max(1, fw * fh // 34)):
        draw.point((fx + rng.randrange(fw), fy + rng.randrange(fh)),
                   fill=(*shade(base, rng.choice((0.94, 0.98, 1.03))), 255))


# --------------------------------------------------------------------- geometry helpers
def stack(m, bone, levels, mats, chamfer=2):
    """Vertical limb segment with chamfered X-Z corners: two crossed boxes.

    levels: (y, center_x, center_z, width, height, depth).
    """
    for i, (y, cx, cz, w, h, d) in enumerate(levels):
        mat = mats[i % len(mats)]
        w, d = max(4, int(round(w))), max(4, int(round(d)))
        c = max(1, min(chamfer, w // 3, d // 3))
        m.box(bone, [cx - w // 2, y, cz - d // 2 + c], [w, h, d - 2 * c], mat, i & 1)
        m.box(bone, [cx - w // 2 + c, y, cz - d // 2], [w - 2 * c, h, d], mat, i & 1)


def paddle(m, bone, side, slices, mats):
    """Rounded flipper/wing blade: slices along X, chamfered in Y-Z."""
    for i, (x, y, z, length, thick, depth) in enumerate(slices):
        mat = mats[0] if i < len(slices) - 1 else mats[1]
        w = max(2, int(round(length)))
        yy = int(round(y))
        zz = int(round(z))
        t = max(1, int(round(thick)))
        d = max(3, int(round(depth)))
        x0 = x if side > 0 else -x - w
        # pale underside, darker rounded top, plus a chamfer slice through the middle
        m.box(bone, [x0, yy, zz + 1], [w, max(1, t - 1), d - 2], mat, i & 1)
        m.box(bone, [x0, yy + 1, zz], [w, max(1, t - 2), d], mat, i & 1)
        if i % 2 == 0:
            m.box(bone, [x0, yy - 1, zz + 1], [w, 1, d - 2], mats[2], (i + 1) & 1)


# --------------------------------------------------------------------- pegasus
def pegasus():
    m = Model('pegasus', 0.66)

    # ---- torso: chest, high withers, deep barrel, muscular haunch
    m.bone('body', [0, 33, 0])
    torso = [(-29, 35, 4, 5), (-26, 34, 6, 6.5), (-23, 33, 8.5, 8.5), (-19, 32, 10, 10),
             (-14, 32, 11, 11), (-9, 32, 11, 11.5), (-4, 32, 11, 11.5), (1, 32, 11, 11),
             (6, 32, 11, 11), (11, 33, 10.5, 10.5), (16, 34, 9.5, 9.5), (21, 35, 8, 8),
             (25, 36, 6, 6), (28, 37, 4, 4)]
    m.tube('body', torso, 'coat', 'coat', 'coat_shade', step=2)
    # withers ridge and the croup behind the saddle
    for i in range(6):
        m.box('body', [-4, 42 - i % 2, -18 + i * 6], [8, 2, 6], 'coat_light', i % 2)
    # chest, shoulder and haunch muscle volumes
    m.sym('body', 2, 24, -23, 9, 9, 9, 'coat')
    m.sym('body', 3, 29, -17, 8, 10, 10, 'coat_light')
    m.sym('body', 2, 24, 13, 10, 11, 11, 'coat')
    m.box('body', [-5, 21, -10], [10, 3, 24], 'coat_shade')
    m.box('body', [-4, 20, 0], [8, 2, 14], 'coat_shade')
    m.box('body', [-4, 38, 25], [8, 4, 6], 'coat_light')

    # ---- neck: rises and arches forward; thick crest + throat
    m.bone('neck_low', [0, 38, -19], 'body', {'axis': 'x', 'amp': 1.0, 'speed': .4})
    m.bone('neck_up', [0, 46, -25], 'neck_low', {'axis': 'x', 'amp': .9, 'speed': .5, 'phase': .6})
    stack(m, 'neck_low', [(36, 0, -19, 16, 4, 14), (40, 0, -21, 15, 4, 13),
                          (44, 0, -23, 14, 4, 12)], ['coat', 'coat_light', 'coat'])
    stack(m, 'neck_up', [(47, 0, -25, 13, 4, 11), (50, 0, -27, 12, 4, 10),
                         (53, 0, -28, 11, 3, 9)], ['coat', 'coat_light', 'coat'])
    m.box('neck_low', [-5, 33, -17], [10, 4, 12], 'coat_shade')
    m.box('neck_up', [-4, 43, -24], [8, 4, 9], 'coat_shade')
    m.box('neck_up', [-4, 47, -27], [8, 6, 6], 'coat_light')

    # ---- head: explicit wedge (skull, brow, face, muzzle, jaw) so the
    # silhouette reads as a horse head instead of a tapered tube
    m.bone('head', [0, 53, -28], 'neck_up', {'axis': 'x', 'amp': .8, 'speed': .62, 'phase': .2})
    m.box('head', [-5, 48, -37], [10, 10, 11], 'coat')            # cranium
    m.box('head', [-4, 47, -40], [8, 9, 9], 'coat', 1)            # chamfer
    m.box('head', [-5, 56, -40], [10, 3, 8], 'coat_light')        # brow ridge
    m.box('head', [-4, 45, -45], [8, 9, 7], 'coat')                # face
    m.box('head', [-3, 43, -49], [6, 6, 5], 'coat')                # muzzle
    m.box('head', [-2, 44, -51], [4, 4, 2], 'muzzle')              # nose pad
    m.box('head', [-4, 42, -50], [8, 3, 6], 'coat_shade')          # lower jaw
    m.box('head', [-3, 43, -51], [6, 1, 2], 'lip')                 # lower lip
    m.box('head', [-6, 49, -36], [2, 7, 9], 'coat_shade')          # cheek
    m.box('head', [4, 49, -36], [2, 7, 9], 'coat_shade')           # cheek
    m.sym('head', 1, 44, -51, 1, 2, 2, 'nostril')
    m.box('head', [-2, 56, -43], [4, 1, 7], 'coat_light')          # blaze
    m.sym('head', 4, 53, -42, 2, 2, 3, 'coat_shade')               # eye socket
    m.sym('head', 5, 53, -42, 1, 1, 2, 'eye')
    m.sym('head', 6, 54, -42, 1, 1, 1, 'eye_glint')
    m.sym('head', 4, 51, -40, 2, 1, 4, 'coat_light')
    m.bone('ear_l', [3, 58, -35], 'head')
    m.bone('ear_r', [-4, 58, -35], 'head')
    m.box('ear_l', [2, 57, -36], [3, 5, 2], 'coat')
    m.box('ear_l', [3, 59, -36], [1, 3, 1], 'ear_inner')
    m.box('ear_r', [-5, 57, -36], [3, 5, 2], 'coat')
    m.box('ear_r', [-4, 59, -36], [1, 3, 1], 'ear_inner')
    for i in range(3):
        m.box('head', [-2 + i % 2, 57 - i, -36 - i], [2, 3, 2], 'mane' if i % 2 else 'mane_dark', i % 2)

    # ---- mane: a raised, flowing crest along the back of the neck
    m.bone('mane_1', [0, 40, -21], 'neck_low', {'axis': 'z', 'amp': 3, 'speed': .7})
    m.bone('mane_2', [0, 47, -25], 'neck_up', {'axis': 'z', 'amp': 4, 'speed': .62, 'phase': .5})
    m.bone('mane_3', [0, 52, -28], 'neck_up', {'axis': 'z', 'amp': 5, 'speed': .55, 'phase': 1.0})
    for bone, y0, z0, n in (('mane_1', 39, -17, 5), ('mane_2', 45, -22, 5), ('mane_3', 50, -26, 4)):
        for i in range(n):
            y = y0 + i * 2 + 1
            z = z0 - i * 2 - 1
            m.box(bone, [-3, y, z], [6, 4, 4], 'mane' if i % 2 else 'mane_dark', i % 2)
            m.box(bone, [-2, y + 3, z - 1], [4, 3, 3], 'mane_dark' if i % 2 else 'mane', i % 2)
            m.box(bone, [2, y - 1, z + 1], [2, 6, 3], 'mane', (i + 1) % 2)
            m.box(bone, [-5, y - 1, z + 1], [2, 7, 3], 'mane_dark', i % 2)

    # ---- legs: shoulder/hip, forearm/gaskin, cannon, fetlock, hoof
    for front, z_leg, top in ((True, -16, 33), (False, 16, 34)):
        for side, label in ((1, 'l'), (-1, 'r')):
            cx = (6 if front else 7) * side
            upper = ('leg_front_' if front else 'leg_rear_') + label
            lower = upper + '_low'
            m.bone(upper, [cx, top - 1, z_leg], 'body')
            m.bone(lower, [cx, 21, z_leg], upper)
            if front:
                stack(m, upper, [(24, cx, z_leg, 8, 6, 10), (28, cx, z_leg - 1, 8, 5, 9),
                                 (32, cx, z_leg - 1, 7, 3, 8)], ['coat', 'coat'])
                stack(m, lower, [(18, cx, z_leg, 5, 4, 6), (14, cx, z_leg, 4.5, 4, 5.5),
                                 (21, cx, z_leg, 6, 3, 7)], ['coat', 'coat_shade'])
            else:
                stack(m, upper, [(24, cx, z_leg, 10, 6, 12), (29, cx, z_leg - 1, 10, 5, 11),
                                 (33, cx, z_leg - 1, 9, 3, 10)], ['coat', 'coat'])
                stack(m, lower, [(18, cx, z_leg, 5.5, 4, 7), (14, cx, z_leg, 5, 4, 6),
                                 (21, cx, z_leg, 7, 3, 8)], ['coat', 'coat_shade'])
            m.box(lower, [cx - 3, 10, z_leg - 3], [6, 4, 6], 'coat_shade')   # knee/hock
            m.box(lower, [cx - 2, 5, z_leg - 2], [5, 6, 5], 'coat')          # cannon
            m.box(lower, [cx - 3, 3, z_leg - 3], [6, 3, 6], 'coat_light')    # fetlock feathering
            m.box(lower, [cx - 3, 0, z_leg - 4], [6, 3, 8], 'hoof')          # hoof
            m.box(lower, [cx - 3, 2, z_leg - 4], [6, 1, 8], 'coat_shade')    # coronet band

    # ---- wings: one continuous swept airfoil per side (no gaps), with a
    # layered feather fringe, raised coverts and a pale underside
    for side, label in ((1, 'l'), (-1, 'r')):
        def X(x, w=0):
            return x if side > 0 else -x - w
        wing = 'wing_' + label
        mid = wing + '_mid'
        tip = wing + '_tip'
        m.bone(wing, [side * 7, 38, -6], 'body')
        m.bone(mid, [side * 22, 41, -4], wing)
        m.bone(tip, [side * 36, 44, -1], mid)
        # shoulder mass, humerus, forearm, hand
        m.box(wing, [X(4, 12), 33, -11], [12, 11, 15], 'wing_root')
        m.box(wing, [X(6, 16), 36, -10], [16, 8, 14], 'wing_shadow')
        m.box(wing, [X(8, 18), 39, -9], [18, 5, 12], 'wing')
        m.box(wing, [X(9, 15), 42, -8], [15, 3, 9], 'wing_mid')
        m.box(mid, [X(22, 16), 39, -8], [16, 5, 11], 'wing')
        m.box(mid, [X(23, 14), 42, -7], [14, 3, 9], 'wing_mid')
        m.box(mid, [X(24, 13), 37, -7], [13, 3, 10], 'wing_shadow')
        m.box(tip, [X(36, 14), 42, -6], [14, 4, 10], 'wing')
        m.box(tip, [X(44, 12), 43, -3], [12, 3, 9], 'wing')
        m.box(tip, [X(52, 10), 44, 1], [10, 2, 8], 'wing_tip')
        m.box(tip, [X(58, 8), 44, 5], [8, 2, 7], 'wing_tip')
        # pale underside plates (thin, inset by one texel)
        m.box(tip, [X(38, 12), 41, -5], [12, 1, 8], 'wing_shadow')
        m.box(mid, [X(23, 14), 38, -6], [14, 1, 9], 'wing_shadow')
        # feather fringe: scalloped, individually sized outer primaries
        for i in range(7):
            x = 56 + i * 1.6
            y = 43 - i // 3
            z = 2 + i * 2.2
            m.box(tip, [X(int(x), 8 - i // 3), int(y), int(z)], [max(3, 8 - i // 3), 2, 4],
                  'wing_tip' if i % 2 else 'wing', i % 2)
            m.box(tip, [X(int(x + 4), 4), int(y - 1), int(z + 2)], [4, 1, 3], 'wing_shadow', i % 2)
        # secondaries: fringe behind the forearm
        for i in range(5):
            x = 24 + i * 3
            m.box(mid, [X(x, 4), 37 - i // 3, 4 + i * 2], [4, 2, 4], 'wing_mid', i % 2)
        # coverts: two raised rows on top of the arm
        for i in range(7):
            m.box(wing, [X(9 + i * 2, 4), 43 + (i % 2), -8 + i], [4, 3, 7], 'wing_mid', i % 2)
        for i in range(5):
            m.box(mid, [X(23 + i * 2, 4), 43 + (i % 2), -7 + i * 2], [4, 2, 6], 'wing_shadow', i % 2)
        # scapulars at the wing root
        for i in range(3):
            m.box(wing, [X(3, 3), 36 + i, -7 + i * 2], [3, 4, 5], 'wing_root', i % 2)

    # ---- tail: dock + a plume that hangs down and slightly back
    m.bone('tail_1', [0, 39, 26], 'body', {'axis': 'y', 'amp': 4, 'speed': .5, 'phase': .3})
    m.bone('tail_2', [0, 35, 31], 'tail_1', {'axis': 'y', 'amp': 6, 'speed': .55, 'phase': .9})
    m.bone('tail_3', [0, 29, 34], 'tail_2', {'axis': 'y', 'amp': 8, 'speed': .6, 'phase': 1.5})
    m.box('tail_1', [-3, 34, 25], [6, 8, 8], 'coat')
    for i in range(4):
        m.box('tail_1', [-5 + i % 2, 30 - i, 29 + i], [5, 7, 5], 'mane' if i % 2 else 'mane_dark', i % 2)
        m.box('tail_2', [-6 + i % 2, 25 - i, 31 + i], [5, 8, 5], 'mane' if i % 2 else 'mane_dark', i % 2)
        m.box('tail_3', [-6 + i % 2, 19 - i, 33 + i], [5, 9, 5], 'mane_dark' if i % 2 else 'mane', i % 2)
    for i in range(3):
        m.box('tail_3', [-4 + i % 2, 12 + i, 34 + i], [4, 5, 4], 'mane', i % 2)

    # ---- saddle (hidden unless the entity is saddled)
    m.bone('saddle', [0, 42, -2], 'body')
    m.box('saddle', [-7, 41, -11], [14, 2, 15], 'saddle')
    m.box('saddle', [-6, 43, -10], [12, 2, 13], 'saddle_dark')
    m.box('saddle', [-5, 45, -2], [10, 2, 4], 'saddle')
    m.box('saddle', [-5, 40, -12], [10, 2, 2], 'saddle_dark')
    m.sym('saddle', 7, 30, -9, 2, 12, 3, 'leather')
    m.sym('saddle', 8, 28, -9, 2, 2, 3, 'gold')
    m.box('saddle', [-7, 33, -2], [14, 2, 3], 'leather')
    m.box('saddle', [-1, 46, -7], [2, 3, 2], 'gold')

    return m.export()


# --------------------------------------------------------------------- liopleurodon
def liopleurodon():
    m = Model('liopleurodon', 0.78)

    # ---- six-bone axial chain: the renderer bends it into a travelling wave
    m.bone('body_front', [0, 0, -20])
    m.bone('body_mid', [0, 0, 18], 'body_front')
    m.bone('body_rear', [0, 0, 58], 'body_mid')
    m.bone('tail_1', [0, 0, 96], 'body_rear')
    m.bone('tail_2', [0, 0, 116], 'tail_1')

    front = [(-82, -2, 3.5, 4), (-76, -1, 6, 6.5), (-68, 0, 9.5, 9.5), (-58, 0, 13.5, 13),
             (-48, 1, 16.5, 16), (-37, 1, 18, 18), (-26, 1, 19, 18.5), (-14, 1, 19, 18),
             (-2, 1, 18.5, 17), (12, 1, 18, 16)]
    m.tube('body_front', front, 'flank', 'back', 'belly', step=3, material_trans='flank_pale')
    mid = [(10, 1, 18, 16), (22, 1, 17.5, 15), (34, 0, 16, 14), (46, 0, 14.5, 13), (58, 0, 13, 11)]
    m.tube('body_mid', mid, 'flank', 'back', 'belly', step=3, material_trans='flank_pale')
    rear = [(54, 0, 13.5, 12), (66, 0, 12, 10), (78, -1, 10.5, 9), (88, -2, 9, 7.5), (98, -3, 7, 6.5)]
    m.tube('body_rear', rear, 'flank', 'back', 'belly', step=3, material_trans='flank_pale')
    m.tube('tail_1', [(94, -3, 9, 8), (102, -4, 7, 6), (110, -5, 6, 5), (118, -6, 4, 4)],
           'flank', 'back', 'belly', step=3, material_trans='flank_pale')
    m.tube('tail_2', [(114, -6, 5, 4), (122, -7, 3.5, 3), (129, -7, 2.5, 2)],
           'flank', 'back', 'belly', step=3, material_trans='flank_pale')

    # one continuous low dorsal keel, highest above the hips
    for z in range(-60, 100, 8):
        bone = 'body_front' if z < 16 else ('body_mid' if z < 56 else 'body_rear')
        h = 2 + max(0, 3 - abs(z - 42) // 24)
        m.box(bone, [-1, 3, z], [2, h, 8], 'back_dark', 0)
    # hooked sail above the hips
    m.box('body_mid', [-3, 8, 38], [6, 5, 14], 'back_dark')
    m.box('body_mid', [-2, 12, 41], [4, 5, 10], 'flank')
    m.box('body_mid', [-1, 16, 44], [2, 4, 6], 'back_dark')
    # throat folds under the neck
    for i in range(4):
        m.box('body_front', [-7 + i, -12 - i % 2, -66 + i * 4], [14 - i * 2, 2, 4], 'belly_shade', 0)
    # pale mottled flank transition and a few healed scars
    for z in range(-54, 98, 10):
        bone = 'body_front' if z < 16 else ('body_mid' if z < 56 else 'body_rear')
        w = 18 if z < 20 else (15 if z < 60 else 10)
        m.sym(bone, w - 2, -2 + (z // 10) % 2, z, 2, 2, 6, 'flank_pale', 0)
    for (z, edge, n) in ((-42, 14, 5), (-24, 17, 6), (-2, 18, 5), (24, 14, 4), (48, 11, 4)):
        bone = 'body_front' if z < 16 else ('body_mid' if z < 56 else 'body_rear')
        for i in range(n):
            side = 1 if (z + i) % 2 == 0 else -1
            x = edge - i if side > 0 else -edge - 1 + i
            m.box(bone, [x, 5 - i // 2, z + i * 2], [1, 1, 3], 'scar', 0)

    # ---- neck and skull
    m.bone('neck', [0, 0, -62], 'body_front', {'axis': 'y', 'amp': 1.6, 'speed': .5, 'phase': .2})
    m.tube('neck', [(-92, -2, 6, 6), (-86, -1, 9, 9), (-78, 0, 12, 10), (-70, 0, 13, 11),
                    (-62, 0, 14, 11)], 'flank', 'back', 'belly', step=3, material_trans='flank_pale')
    m.sym('neck', 6, -7, -72, 3, 3, 8, 'belly_shade')
    m.bone('head', [0, 1, -88], 'neck', {'axis': 'y', 'amp': 1.4, 'speed': .55, 'phase': .7})
    # cranium: wide, rounded, with a low dome and heavy brows
    m.box('head', [-7, -2, -102], [14, 10, 16], 'flank')
    m.box('head', [-6, 6, -100], [12, 3, 12], 'back')
    m.box('head', [-6, -3, -104], [12, 2, 18], 'back')
    m.sym('head', 6, 6, -100, 2, 3, 10, 'back_dark')          # brow ridges
    m.sym('head', 7, 5, -96, 1, 2, 3, 'lio_eye')               # high-set eyes
    m.sym('head', 7, 7, -96, 1, 1, 2, 'glint')
    m.sym('head', 3, -4, -100, 4, 2, 6, 'flank_pale')          # cheek / jugal
    # long tapering crocodile snout: four stacked sections, deep enough to read
    for (w, h, z0, d) in ((10, 6, -106, 8), (9, 5, -113, 8), (8, 4, -120, 8), (6, 3, -127, 7)):
        x0 = -(w // 2)
        m.box('head', [x0, -2, z0], [w, h, d], 'flank')
        m.box('head', [x0 + 1, h - 2, z0], [max(2, w - 2), 2, d], 'back')
    m.box('head', [-3, -1, -131], [6, 3, 3], 'flank')          # rounded nose
    m.box('head', [-3, 1, -130], [6, 1, 3], 'back_dark')
    m.sym('head', 1, 2, -129, 2, 1, 2, 'mouth')                # nostril bumps
    # pale upper lip band and the dark mouth slit
    m.box('head', [-5, -3, -130], [10, 1, 27], 'flank_pale')
    m.box('head', [-5, -4, -131], [10, 1, 29], 'mouth')
    # upper teeth hang from the lip line, inset so they read inside the jaw
    for side in (1, -1):
        for i in range(13):
            z = -129 + i * 2
            x = (3 if z > -118 else 2)
            x0 = x if side > 0 else -x - 1
            h = 2 + (i % 3) + (1 if i < 4 else 0)
            m.box('head', [x0, -4 - h, z], [1, h, 1], 'tooth', i % 2)
    # ---- lower jaw: pale chin, dark inside, interlocking lower teeth
    m.bone('jaw', [0, -5, -104], 'head', {'axis': 'x', 'amp': 3.0, 'speed': .45, 'phase': .9})
    for (w, h, z0, d) in ((9, 4, -106, 8), (8, 3, -113, 8), (7, 3, -120, 8), (5, 2, -127, 6)):
        m.box('jaw', [-(w // 2), -8, z0], [w, h, d], 'flank')
        m.box('jaw', [-(w // 2) + 1, -9, z0], [max(2, w - 2), 1, d], 'belly_shade')
    m.box('jaw', [-3, -8, -131], [6, 2, 3], 'belly')
    m.box('jaw', [-4, -5, -128], [8, 1, 26], 'gum')
    m.box('jaw', [-3, -4, -126], [6, 1, 22], 'tongue')
    m.box('jaw', [-5, -8, -104], [10, 4, 8], 'belly_shade')
    for side in (1, -1):
        for i in range(12):
            z = -128 + i * 2
            x = (3 if z > -118 else 2)
            x0 = x if side > 0 else -x - 1
            h = 2 + (i % 3)
            m.box('jaw', [x0, -5, z], [1, h, 1], 'tooth', i % 2)

    # ---- flippers: rounded paddles, pale undersides, no ladder steps
    for front_pair in (True, False):
        z_root = -48 if front_pair else 40
        for side, label in ((1, 'l'), (-1, 'r')):
            base = ('flipper_front_' if front_pair else 'flipper_rear_') + label
            tip = base + '_tip'
            m.bone(base, [side * 12, -9, z_root], 'body_front' if front_pair else 'body_mid',
                   {'axis': 'z', 'amp': side * (9 if front_pair else 7), 'speed': 1.0,
                    'phase': 0 if front_pair else .9})
            m.bone(tip, [side * (34 if front_pair else 28), -14, z_root + 8], base)
            spread = 1.0 if front_pair else 0.8
            slices = []
            for i in range(7):
                slices.append((11 + i * 5, -9 - i * 1.0, z_root - 13 + i * 3.4 * spread, 6, 5 - i * 0.6,
                               (36 - i * 4.2) * spread))
            paddle(m, base, side, slices, ['fin', 'fin_edge', 'fin_pale'])
            tips = []
            for i in range(5):
                tips.append(((34 if front_pair else 29) + i * 5, -15 - i * 0.8, z_root - 3 + i * 2.6 * spread,
                             max(2, 5 - i * 0.8), max(1, 4 - i * 0.7), max(2, (19 - i * 3) * spread)))
            paddle(m, tip, side, tips, ['fin', 'fin_edge', 'fin_pale'])
            # pale root and a scarred leading edge
            m.box(base, [14 if side > 0 else -20, -7, z_root - 8], [5, 2, 8], 'fin_pale')
            m.box(base, [20 if side > 0 else -24, -8, z_root - 4], [3, 1, 7], 'scar')

    # ---- vertical tail fluke: long upper lobe, short lower lobe
    m.box('tail_2', [-2, -12, 118], [4, 7, 8], 'flank')
    for i in range(5):
        m.box('tail_2', [-1, -13 - i * 2, 121 + i], [2, 3, 5 - i // 2], 'fin_edge', i % 2)
    for i in range(5):
        m.box('tail_2', [-1, 1 + i * 3, 120 + i], [2, 4, 5 - i // 2], 'fin', i % 2)
    m.box('tail_2', [-1, 15, 121], [2, 3, 3], 'fin_edge')

    return m.export()


if __name__ == '__main__':
    pegasus()
    liopleurodon()
