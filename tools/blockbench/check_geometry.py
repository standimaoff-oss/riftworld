#!/usr/bin/env python3
"""Check static Blockbench -> runtime transforms and beetle appendage clearance.

No Minecraft runtime required. The generator uses at most one static Euler axis
per group, so order differences cannot hide a conversion error. The clearance
check encloses every rotated appendage in an AABB and samples the *entire*
antenna yaw range in 0.25 degree steps; separated Z intervals prove no overlap
at those samples (not a substitute for in-game testing).
"""
import itertools
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
IDS = ('porcelain_crane', 'basalt_beetle', 'ash_moth', 'amethyst_fox', 'sizo_leafwing', 'azure_frog', 'bone_raven', 'coral_hermit_crab', 'reef_seahorse', 'ghost_squid', 'silver_sea_serpent')


def rotate(point, origin, degrees):
    x, y, z = (point[i] - origin[i] for i in range(3))
    # These models intentionally use a single non-zero axis per group.
    if sum(abs(n) > 1e-8 for n in degrees) > 1:
        raise AssertionError(f'multi-axis group needs Euler-order handling: {degrees}')
    for axis, angle in enumerate(degrees):
        a = math.radians(angle)
        c, s = math.cos(a), math.sin(a)
        if axis == 0:
            y, z = c*y - s*z, s*y + c*z
        elif axis == 1:
            x, z = c*x + s*z, -s*x + c*z
        else:
            x, y = c*x - s*y, s*x + c*y
    return [x + origin[0], y + origin[1], z + origin[2]]


def transformed(point, chain, swing=None):
    for group in reversed(chain):
        rot = list(group.get('rotation', [0, 0, 0]))
        if swing and group['name'] == swing[0]:
            rot[1] += swing[1]
        point = rotate(point, group.get('origin', group.get('pivot')), rot)
    return point


def load_model(id):
    bb = json.loads((ROOT / f'blockbench/riftworld_fauna/{id}.bbmodel').read_text())
    geo = json.loads((ROOT / f'mod/src/main/resources/assets/riftworld/geo/{id}.json').read_text())
    groups = {x['uuid']: x for x in bb['groups']}
    elements = {x['uuid']: x for x in bb['elements']}
    bones = {x['name']: x for x in geo['bones']}
    assert len(bones) == len(geo['bones'])
    seen = set()
    group_cubes = {}

    def walk(node, chain):
        group = groups[node['uuid']]
        bone = bones[group['name']]
        assert bone.get('parent') == (chain[-1]['name'] if chain else None)
        # .bbmodel serialization rounds floats to five decimals; raw Bedrock
        # compilation retains full precision. Allow only that tiny difference.
        assert all(abs(a-b) < 1e-4 for a,b in zip(bone['pivot'], [-group['origin'][0], -group['origin'][1], group['origin'][2]]))
        bb_rot = group.get('rotation', [0, 0, 0])
        assert all(abs(a-b) < 1e-4 for a,b in zip(bone.get('rotation', [0, 0, 0]), [-bb_rot[0], -bb_rot[1], bb_rot[2]]))
        parent_chain = chain + [group]
        runtime_chain = [bones[g['name']] for g in parent_chain]
        children = [elements[c] for c in node['children'] if isinstance(c, str)]
        assert len(children) == len(bone.get('cubes', [])), group['name']
        group_cubes[group['name']] = (parent_chain, children)
        for element, cube in zip(children, bone.get('cubes', [])):
            assert all(isinstance(v, int) and v > 0 for v in cube['size'])
            w, h, d = cube['size']; u, v = cube['uv']
            assert 0 <= u and 0 <= v and u + 2*w + 2*d <= geo['texture_width'] and v + h + d <= geo['texture_height']
            # Compare every transformed vertex, not just centers. Reflections
            # exchange min/max corners on X and Y but preserve all dimensions.
            for bbpoint in itertools.product(*zip(element['from'], element['to'])):
                rtpoint = [-bbpoint[0], -bbpoint[1], bbpoint[2]]
                assert all(cube['origin'][i] <= rtpoint[i] <= cube['origin'][i] + cube['size'][i]
                           for i in range(3)), (id, group['name'], rtpoint, cube)
                bb_world = transformed(bbpoint, parent_chain)
                rt_world = transformed(rtpoint, runtime_chain)
                expected = [-bb_world[0], -bb_world[1], bb_world[2]]
                assert all(abs(a - b) < 1e-4 for a, b in zip(rt_world, expected)), (id, group['name'], rt_world, expected)
            seen.add(element['uuid'])
        for child in node['children']:
            if isinstance(child, dict): walk(child, parent_chain)

    for node in bb['outliner']: walk(node, [])
    assert len(seen) == len(bb['elements'])
    print(f'{id}: {len(bones)} bones, {len(seen)} cubes, rotated runtime vertices match')
    return group_cubes


def z_interval(chain, elements, swing=None):
    points = []
    for el in elements:
        for corner in itertools.product(*zip(el['from'], el['to'])):
            points.append(transformed(corner, chain, swing)[2])
    return min(points), max(points)

def overlap_yz(poly, rect):
    """Convex quad/axis-aligned rectangle overlap in the YZ plane (SAT)."""
    y0, z0, y1, z1 = rect
    box = [(y0,z0), (y0,z1), (y1,z1), (y1,z0)]
    axes = [(1,0), (0,1)]
    for i in range(4):
        a, b = poly[i], poly[(i+1) % 4]
        axes.append((a[1]-b[1], b[0]-a[0]))
    for ay, az in axes:
        if abs(ay)+abs(az) < 1e-9:
            continue
        ps = [y*ay+z*az for y,z in poly]
        qs = [y*ay+z*az for y,z in box]
        if min(max(ps), max(qs)) - max(min(ps), min(qs)) <= 1e-7:
            return False
    return True


def check_serpent_crest(groups):
    import subprocess
    from collections import deque
    _, body = groups['body']
    strips = [e for e in body if e['name'].startswith('crest_slice_')]
    assert len(strips) > 20
    cells = set()
    rects = []
    atlas = subprocess.check_output(['convert', str(ROOT / 'mod/src/main/resources/assets/riftworld/textures/entity/silver_sea_serpent.png'), '-depth', '8', 'rgba:-'])
    assert len(atlas) == 2048 * 2048 * 4
    pixels = 0
    for e in strips:
        f, t = e['from'], e['to']
        assert f[0] == -1 and t[0] == 0 and t[2]-f[2] == 2
        assert f[1] % 2 == t[1] % 2 == f[2] % 2 == 0
        rects.append((f[1], f[2], t[1], t[2]))
        for y in range(f[1],t[1],2):
            for z in range(f[2],t[2],2):
                assert (y,z) not in cells, 'overlapping coplanar crest slices'
                cells.add((y,z))
        # Standard box-UV: the two big +/-X faces have identical YZ pixels.
        u,v = e['uv_offset']; d = t[2]-f[2]; h=t[1]-f[1]; w=1
        for iy in range(h):
            for iz in range(d):
                a = 4*((v+d+iy)*2048+u+iz)
                b = 4*((v+d+iy)*2048+u+d+w+iz)
                assert atlas[a:a+4] == atlas[b:b+4], (e['name'],iy,iz)
                assert atlas[a+3] == 255
                pixels += 1
    # A single 4-connected component means there are no floating fins.
    pending = set(cells); queue = deque([pending.pop()]); count=0
    while queue:
        y,z=queue.popleft(); count+=1
        for p in ((y+2,z),(y-2,z),(y,z+2),(y,z-2)):
            if p in pending:
                pending.remove(p); queue.append(p)
    assert not pending, f'disconnected crest: {len(pending)} cells'
    # Every dorsal stripe segment intersects the sheet, not just its neighbours.
    stripes = [name for name in groups if name.startswith('dorsal_stripe_')]
    assert len(stripes) == 14
    for name in stripes:
        chain, elements = groups[name]; assert len(elements) == 1
        e = elements[0]; f,t=e['from'],e['to']; x=(f[0]+t[0])/2
        points = [(f[1],f[2]), (f[1],t[2]), (t[1],t[2]), (t[1],f[2])]
        quad = [(p[1],p[2]) for p in (transformed([x,y,z],chain) for y,z in points)]
        assert any(overlap_yz(quad,r) for r in rects), name+' does not meet crest sheet'
        # The curved body is rotated too. Transform the stripe's corners into
        # the body's own local YZ frame and test the resulting convex quad.
        body_chain, body_cubes = groups['curve_'+name.removeprefix('dorsal_stripe_')]
        assert len(body_cubes) == 1
        body_cube = body_cubes[0]; origin=body_chain[-1]['origin']; rot=body_chain[-1].get('rotation', [0,0,0])
        local = [rotate([0,y,z],origin,[-v for v in rot]) for y,z in quad]
        qlocal = [(p[1],p[2]) for p in local]
        bf,bt=body_cube['from'],body_cube['to']
        assert overlap_yz(qlocal,(bf[1],bf[2],bt[1],bt[2])), name+' misses body'
    print(f'silver_sea_serpent crest: {len(strips)} thin slices, {count} connected cells, {pixels} matching face pixels, {len(stripes)} stripe/body intersections')


if __name__ == '__main__':
    models = {id: load_model(id) for id in IDS}
    check_serpent_crest(models['silver_sea_serpent'])
    groups = models['basalt_beetle']
    for side in ('left', 'right'):
        horn = groups[f'horn_{side}_tilt']
        antenna = groups[f'antenna_{side}_stem']
        _, horn_max = z_interval(*horn)
        min_gap = min(z_interval(*antenna, (f'antenna_{side}', i / 4))[0] - horn_max
                      for i in range(-16, 17))
        assert min_gap > 0, (side, min_gap)
        print(f'{side}: horn/antenna Z gap >= {min_gap:.2f} authoring units, sampled yaw -4..+4 degrees')
