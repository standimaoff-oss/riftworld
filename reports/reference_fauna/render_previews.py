#!/usr/bin/env python3
"""Software previews of the rebuilt v0.45.0 Pégasos and Liopleurodon rigs.

These are renders of the *actual* Blockbench source geometry that ships to the
game (same JSON, same atlas, same joint hierarchy), lit by the same offline
rasteriser used for the Maja sheets. They are reference/progress images, not
in-game screenshots.
"""
from pathlib import Path
import json
import sys

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / 'el_gran_maja_model'))
from render_el_gran_maja_shots import load_posed_quads, render_scene, make_underwater_bg


def sky(w, h):
    y = np.linspace(0, 1, h)[:, None, None]
    top = np.array([196, 212, 228])[None, None, :]
    bottom = np.array([228, 223, 214])[None, None, :]
    return np.broadcast_to(top * (1 - y) + bottom * y, (h, w, 3)).copy()


def rig(name):
    geo = json.loads((HERE / f'{name}.blockbench.geo.json').read_text())
    geo['minecraft:geometry'][0]['bones'] = [b for b in geo['minecraft:geometry'][0]['bones']
                                             if b['name'] != 'saddle']
    tex = np.asarray(Image.open(HERE / f'{name}.png').convert('RGB'))
    return geo, tex


def pose_folded():
    # a bird-like tuck: the wrist swings backwards along the flank, primaries lay
    # over the tail, the whole wing lifts a little off the barrel
    return {'wing_l': (0, -14, 40), 'wing_l_mid': (0, 0, -12), 'wing_l_tip': (0, 0, -6),
            'wing_r': (0, 14, -40), 'wing_r_mid': (0, 0, 12), 'wing_r_tip': (0, 0, 6),
            'neck_up': (-5, 0, 0), 'head': (4, 0, 0)}


def pose_flight():
    return {'wing_l': (0, -8, -26), 'wing_l_mid': (0, 0, 10), 'wing_l_tip': (0, 0, 16),
            'wing_r': (0, 8, 26), 'wing_r_mid': (0, 0, -10), 'wing_r_tip': (0, 0, -16),
            'neck_low': (-8, 0, 0), 'neck_up': (-10, 0, 0), 'head': (6, 0, 0),
            'leg_front_l': (-45, 0, 0), 'leg_front_l_low': (-55, 0, 0),
            'leg_front_r': (-45, 0, 0), 'leg_front_r_low': (-55, 0, 0),
            'leg_rear_l': (30, 0, 0), 'leg_rear_l_low': (40, 0, 0),
            'leg_rear_r': (30, 0, 0), 'leg_rear_r_low': (40, 0, 0),
            'tail_1': (-24, 0, 0), 'tail_2': (-8, 6, 0), 'tail_3': (0, 10, 0)}


def pose_swim(front_lift, rear_lift):
    return {'flipper_front_l': (0, 0, -front_lift), 'flipper_front_r': (0, 0, front_lift),
            'flipper_rear_l': (0, 0, -rear_lift), 'flipper_rear_r': (0, 0, rear_lift),
            'body_mid': (0, 5, 0), 'body_rear': (0, 8, 0), 'tail_1': (0, 12, 0),
            'tail_2': (0, 16, 0), 'neck': (0, -3, 0), 'head': (0, -4, 4)}


def shot(name, pose, cam, look, half, bg, out):
    geo, tex = rig(name)
    quads = load_posed_quads(geo, pose_overrides=pose)
    img = render_scene(quads, tex, 1100, 720, cam, look, ortho_half_h=half,
                       bg_img=bg, fog_color=None, ssaa=1)
    img.save(HERE / out)
    print('wrote', out, len(quads), 'faces')


def main():
    pegasus_bg = sky(1100, 720)
    water = make_underwater_bg(1100, 720,
                               top_rgb=(30, 62, 92), bot_rgb=(8, 18, 30))
    # --- pegasus: standing with folded wings, three-quarter; then a downstroke
    shot('pegasus', pose_folded(), (-300, 170, -260), (0, 30, -2), 58, pegasus_bg,
         'pegasus_preview_folded.png')
    shot('pegasus', pose_folded(), (430, 40, -10), (0, 32, -4), 56, pegasus_bg,
         'pegasus_preview_side.png')
    shot('pegasus', pose_flight(), (-300, 200, -300), (0, 34, 0), 74, pegasus_bg,
         'pegasus_preview_flight.png')
    shot('pegasus', pose_flight(), (0, 60, -520), (0, 34, -6), 74, pegasus_bg,
         'pegasus_preview_front.png')
    # --- liopleurodon: gliding side view, a three-quarter swim and a head-on pass
    shot('liopleurodon', pose_swim(24, 14), (640, 30, -10), (0, 0, -10), 82, water,
         'liopleurodon_preview_side.png')
    shot('liopleurodon', pose_swim(-18, 26), (-420, 180, -420), (0, 0, -6), 88, water,
         'liopleurodon_preview_swim.png')
    shot('liopleurodon', pose_swim(10, 10), (0, 40, -640), (0, 0, -20), 82, water,
         'liopleurodon_preview_front.png')

    # keep the legacy filenames current as well
    Image.open(HERE / 'pegasus_preview_folded.png').save(HERE / 'pegasus_software_preview.png')
    Image.open(HERE / 'liopleurodon_preview_side.png').save(HERE / 'liopleurodon_software_preview.png')


if __name__ == '__main__':
    main()
