#!/usr/bin/env python3
"""Static guardrails for the rebuilt fauna geometry and loot assets.

This verifies the actual runtime models/atlases and the Y-up editable sources;
it is not a substitute for inspecting the real Forge client.
"""
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSET = ROOT / 'mod/src/main/resources/assets/riftworld'
REPORT = ROOT / 'reports/reference_fauna'


def require(value, message):
    if not value:
        raise AssertionError(message)


def check_model(name, required_bones, min_cubes=300):
    game = json.loads((ASSET / 'geo' / f'{name}.json').read_text())
    source = json.loads((REPORT / f'{name}.blockbench.geo.json').read_text())['minecraft:geometry'][0]['bones']
    game_bones = game['bones']
    require(len(game_bones) == len(source), f'{name}: mismatched bone count')
    atlas = Image.open(ASSET / 'textures/entity' / f'{name}.png').convert('RGBA')
    require(atlas.size == (game['texture_width'], game['texture_height']), f'{name}: atlas size mismatch')
    seen = set()
    cube_count = 0
    for ref, bone in zip(source, game_bones):
        n = bone['name']
        require(n not in seen and n == ref['name'], f'{name}: duplicate/mismatched bone {n}')
        if bone.get('parent'):
            require(bone['parent'] in seen and bone['parent'] == ref.get('parent'), f'{name}: invalid parent on {n}')
        seen.add(n)
        x, y, z = ref['pivot']
        require(bone['pivot'] == [x, -y, z], f'{name}/{n}: pivot was not reflected')
        a, b = ref['cubes'], bone['cubes']
        require(len(a) == len(b), f'{name}/{n}: cube count mismatch')
        for source_cube, cube in zip(a, b):
            x, y, z = source_cube['origin']
            w, h, d = source_cube['size']
            require(cube['origin'] == [x, -y-h, z] and cube['size'] == [w, h, d], f'{name}/{n}: bad reflection')
            require(cube['uv'] == source_cube['uv'], f'{name}/{n}: UV mismatch')
            u, v = cube['uv']
            faces = [(u+d,v,w,d), (u+d+w,v,w,d), (u,v+d,d,h), (u+d,v+d,w,h),
                     (u+d+w,v+d,d,h), (u+2*d+w,v+d,w,h)]
            for fx, fy, fw, fh in faces:
                require(0 <= fx and 0 <= fy and fx+fw <= atlas.width and fy+fh <= atlas.height,
                        f'{name}/{n}: UV outside atlas')
                require(atlas.getchannel('A').crop((fx,fy,fx+fw,fy+fh)).getextrema() == (255,255),
                        f'{name}/{n}: transparent texture face')
            cube_count += 1
        if 'anim' in ref:
            a, b = ref['anim'], bone['anim']
            amp = -a['amp'] if a['axis'] in ('x', 'z') else a['amp']
            require(b['amp'] == amp and b['axis'] == a['axis'], f'{name}/{n}: animation reflection mismatch')
    require(required_bones <= seen, f'{name}: missing required joints {required_bones-seen}')
    require(cube_count >= min_cubes, f'{name}: geometry unexpectedly lost detail')
    print(f'{name}: {len(seen)} bones, {cube_count} cuboids, opaque UV faces, correct coordinate reflection')


def check_item(name):
    model = json.loads((ASSET / 'models/item' / f'{name}.json').read_text())
    require(model.get('parent') == 'minecraft:item/generated', f'{name}: wrong item parent')
    tex = model['textures']['layer0']
    require(tex == f'riftworld:item/{name}', f'{name}: wrong texture reference')
    im = Image.open(ASSET / 'textures/item' / f'{name}.png').convert('RGBA')
    require(im.width >= 16 and im.height >= 16 and im.getchannel('A').getbbox() is not None, f'{name}: empty icon')


def main():
    # v0.45.0 rigs (rebuilt from reports/reference_fauna/refs_v2)
    check_model('liopleurodon', {'body_front','body_mid','body_rear','tail_1','tail_2','neck','head','jaw',
                                 'flipper_front_l','flipper_front_r','flipper_rear_l','flipper_rear_r'})
    # v0.49.0: Пегас и Вендиго удалены из мода, их пробы убраны.
    # Вместо них проверяем новый хоррор-моб Пади — Смотрителя Пращи.
    # Смотритель — тонкая, высокая фигура: кубов намеренно меньше, чем у Лиоплевродона.
    check_model('hollowwatcher', {'root','shroud','neck','head','arm_l','arm_r','leg_l','leg_r'}, min_cubes=40)
    for name in ('liopleurodon_tooth','sea_scale','morok_ember'):
        check_item(name)
    for name in ('liopleurodon','hollowwatcher'):
        item = json.loads((ASSET/'models/item'/f'{name}_spawn_egg.json').read_text())
        # v0.49.0: ванильные яйца рисуются через item/generated + своя текстура
        # (раньше был template_spawn_egg с окраской сущности).
        require(item.get('parent') in ('minecraft:item/template_spawn_egg', 'minecraft:item/generated'),
                f'{name}: missing egg model')
        if item.get('parent') == 'minecraft:item/generated':
            require(item['textures']['layer0'] == f'riftworld:item/{name}_spawn_egg',
                    f'{name}: wrong egg texture reference')
    print('Loot and spawn-egg model/texture references are present.')

if __name__ == '__main__':
    main()
