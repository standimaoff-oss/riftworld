#!/usr/bin/env python3
"""Conservative local-space envelopes for *all* animated Maja bones.

The tail bones share a phase (with different phase offsets).  We sample that
phase densely and bound the interpolation error using the chain's angular
velocity and maximum pivot distances.  Head yaw is bounded independently;
the jaw is sampled over its entire commanded -13..+8 degree interval.
The server uses these deliberately roomy boxes to reject movement before any
visible part of the stock model intersects terrain.
"""
from __future__ import annotations
import json, math
from pathlib import Path
import numpy as np

HERE=Path(__file__).resolve().parent
CENTER_Z=(-902+4252)/32.0
TAIL_SAMPLES=720
JAW_SAMPLES=169


def yrot(points, angle, pivot):
    a=math.radians(angle); c,s=math.cos(a),math.sin(a)
    out=points.copy(); x=points[:,0]-pivot[0]; z=points[:,2]-pivot[2]
    out[:,0]=c*x+s*z+pivot[0]; out[:,2]=-s*x+c*z+pivot[2]
    return out


def xrot(points, angle, pivot):
    a=math.radians(angle); c,s=math.cos(a),math.sin(a)
    out=points.copy(); y=points[:,1]-pivot[1]; z=points[:,2]-pivot[2]
    out[:,1]=c*y-s*z+pivot[1]; out[:,2]=s*y+c*z+pivot[2]
    return out


def corners(bone):
    boxes=bone['cubes']; low=np.min([c['origin'] for c in boxes],axis=0)/16.0
    high=np.max([np.array(c['origin'])+c['size'] for c in boxes],axis=0)/16.0
    return np.array([[x,y,z] for x in (low[0],high[0]) for y in (low[1],high[1]) for z in (low[2],high[2])])


def main():
    model=json.loads((HERE/'el_gran_maja.blockbench.geo.json').read_text())['minecraft:geometry'][0]
    bones={b['name']:b for b in model['bones']}; output=[]
    for bone in model['bones']:
        name=bone['name']; points=corners(bone); chain=[]; cur=bone
        while cur:
            chain.append(cur)
            cur=bones.get(cur.get('parent'))
        lo=np.full(3,np.inf); hi=np.full(3,-np.inf)
        def record(p):
            nonlocal lo,hi
            lo=np.minimum(lo,p.min(axis=0));hi=np.maximum(hi,p.max(axis=0))
        if name=='jaw':
            pivot=np.array(bone['pivot'],dtype=float)/16
            for angle in np.linspace(-13,8,JAW_SAMPLES): record(xrot(points,angle,pivot))
            # Lipschitz bound between samples, including head yaw.
            r=float(np.max(np.linalg.norm(points[:,1:3]-pivot[1:3],axis=1)))
            phase_error=r*math.radians(21/(JAW_SAMPLES-1)/2)
        elif name=='head':
            record(points);phase_error=0.0
        else:
            # All non-head Y bones have the same animation speed. Their phases
            # can therefore be expressed by one cyclic variable.
            for phase in np.linspace(0,2*math.pi,TAIL_SAMPLES,endpoint=False):
                p=points.copy()
                for ancestor in chain:
                    if ancestor['name']=='head': break
                    anim=ancestor.get('anim',{}); amp=anim.get('amp',0)
                    pivot=np.array(ancestor['pivot'],dtype=float)/16
                    p=yrot(p,amp*math.sin(phase+anim.get('phase',0)),pivot)
                record(p)
            # Upper bound on point speed in block/radian of phase. Successive
            # pivots can move, so account for all inner rotations conservatively.
            inner_move=0.0; velocity=0.0
            for ancestor in chain:
                if ancestor['name']=='head': break
                pivot=np.array(ancestor['pivot'],dtype=float)/16
                r=max(float(np.max(np.linalg.norm(points[:,[0,2]]-pivot[[0,2]],axis=1)))+inner_move,0)
                amp=math.radians(abs(ancestor.get('anim',{}).get('amp',0)))
                velocity+=r*amp
                inner_move+=2*r*math.sin(amp/2)
            phase_error=velocity*math.pi/TAIL_SAMPLES
        # The head independently yaws at most 0.8 degrees. A point's maximum
        # displacement is the chord length at its distance from the head pivot.
        hp=np.array(bones['head']['pivot'],dtype=float)/16
        radius=max(math.hypot(x-hp[0],z-hp[2]) for x in (lo[0],hi[0]) for z in (lo[2],hi[2]))
        head_pad=2*radius*math.sin(math.radians(.8)/2)
        # Extra half block covers floating-point rounding and packet interpolation.
        pad=phase_error+0.5
        lo-=pad;hi+=pad;lo[[0,2]]-=head_pad;hi[[0,2]]+=head_pad
        lo[2]-=CENTER_Z;hi[2]-=CENTER_Z
        output.append({'bone':name,'min':[round(v,4) for v in lo], 'max':[round(v,4) for v in hi],
                       'sampling_error':round(phase_error,4),'head_pad':round(head_pad,4)})
    out={'center_z':CENTER_Z,'tail_samples':TAIL_SAMPLES,'jaw_degrees':[-13,8],'boxes':output}
    (HERE/'collision_bounds.json').write_text(json.dumps(out,indent=2)+'\n')
    print('center_z',CENTER_Z,'boxes',len(output))
    for box in output: print(box['bone'],box['min'],box['max'])

if __name__=='__main__': main()
