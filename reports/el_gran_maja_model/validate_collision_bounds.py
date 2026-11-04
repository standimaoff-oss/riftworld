#!/usr/bin/env python3
"""Exercise the full animation range against the envelopes shipped to the server."""
from pathlib import Path
import json, math, random
import numpy as np
import build_collision_bounds as c

HERE=Path(__file__).resolve().parent
ROOT=HERE.parent.parent

def main():
    data=json.loads((HERE/'collision_bounds.json').read_text())
    installed=ROOT/'mod/src/main/resources/assets/riftworld/geo'
    assert (installed/'el_gran_maja_bounds.json').read_bytes()==(HERE/'collision_bounds.json').read_bytes()
    assert (installed/'el_gran_maja.json').read_bytes()==(HERE/'el_gran_maja.riftworld_geo.json').read_bytes()
    assert (ROOT/'mod/src/main/resources/assets/riftworld/textures/entity/el_gran_maja.png').read_bytes()==(HERE/'el_gran_maja.png').read_bytes()
    bones=json.loads((HERE/'el_gran_maja.blockbench.geo.json').read_text())['minecraft:geometry'][0]['bones']
    by_name={b['name']:b for b in bones}; boxes={x['bone']:x for x in data['boxes']}
    assert len(boxes)==len(bones)==13 and data['jaw_degrees']==[-13,8]
    rng=random.Random(5349)
    samples=1200
    for bone in bones:
        base=c.corners(bone); bounds=boxes[bone['name']]
        lo=np.array(bounds['min']); hi=np.array(bounds['max'])
        chain=[]; cur=bone
        while cur:
            chain.append(cur);cur=by_name.get(cur.get('parent'))
        for n in range(samples):
            phase=rng.uniform(0,2*math.pi); head=rng.uniform(-.8,.8); jaw=rng.uniform(-13,8)
            p=base.copy()
            for ancestor in chain:
                pivot=np.array(ancestor['pivot'],dtype=float)/16
                if ancestor['name']=='jaw': p=c.xrot(p,jaw,pivot)
                elif ancestor['name']=='head': p=c.yrot(p,head,pivot)
                else:
                    a=ancestor.get('anim',{}); p=c.yrot(p,a.get('amp',0)*math.sin(phase+a.get('phase',0)),pivot)
            p[:,2]-=data['center_z']
            assert np.all(p>=lo-1e-5) and np.all(p<=hi+1e-5), (bone['name'],n,p.min(axis=0),p.max(axis=0),lo,hi)
            # Independently verify the world's four-corner AABB at arbitrary entity yaw.
            yaw=rng.uniform(-180,180); angle=math.radians(180-yaw); ca,sa=math.cos(angle),math.sin(angle)
            corners=np.array([[x,z] for x in (lo[0],hi[0]) for z in (lo[2],hi[2])])
            def world(v): return np.column_stack((ca*v[:,0]+sa*v[:,1],-sa*v[:,0]+ca*v[:,1]))
            transformed=world(p[:,[0,2]]); envelope=world(corners)
            assert np.all(transformed>=envelope.min(axis=0)-1e-5) and np.all(transformed<=envelope.max(axis=0)+1e-5)
            # Vanilla's packet yaw is quantised to 1/256 of a turn. Test the
            # client's possible orientation against the server's 6-block X/Z margin.
            qangle=math.radians(180-(yaw+rng.uniform(-360/256,360/256)))
            qc,qs=math.cos(qangle),math.sin(qangle)
            xz=p[:,[0,2]]
            quantized=np.column_stack((qc*xz[:,0]+qs*xz[:,1],-qs*xz[:,0]+qc*xz[:,1]))
            assert np.all(quantized>=envelope.min(axis=0)-6.0) and np.all(quantized<=envelope.max(axis=0)+6.0)
    print(f'OK: {len(bones)} collision envelopes contain {samples} sampled hierarchical poses per bone; assets match.')
    print('NOTE: this checks geometry bounds, not interactions with a running game or external mods.')

if __name__=='__main__': main()
