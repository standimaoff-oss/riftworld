#!/usr/bin/env python3
"""Sanity checks for the standalone model, atlas and software-renderer UVs.

The oracle below implements the six box polygons and mirror behavior used by
Minecraft's ModelPart.Cube. It is intentionally independent from the renderer.
"""
from __future__ import annotations

import json
from pathlib import Path

import numpy as np
from PIL import Image

import build_el_gran_maja_model as b
import render_el_gran_maja_shots as r

HERE = Path(__file__).resolve().parent


def java_cube_uvs(origin, size, uv, mirror):
    """World positions/UVs after Java's Y-down cube is flipped to Y-up."""
    ox,oy,oz=origin; w,h,d=size; u,v=uv
    x0,x1=ox,ox+w
    if mirror:
        x0,x1=x1,x0
    y0,y1=-(oy+h),-oy  # physical top / bottom in Java Y-down
    z0,z1=oz,oz+d
    vs={
        4:(x0,y0,z0),5:(x1,y0,z0),6:(x1,y1,z0),7:(x0,y1,z0),
        8:(x0,y0,z1),9:(x1,y0,z1),10:(x1,y1,z1),11:(x0,y1,z1),
    }
    # Rectangle and vertex order follow ModelPart.Cube, not our renderer.
    # Java DOWN is the physical upper face after converting Y.
    polygons=[
        ("up",    [9,8,4,5],     u+d,v,     u+d+w,v+d),
        ("down",  [6,7,11,10],   u+d+w,v+d, u+d+2*w,v),
        ("west",  [4,8,11,7],    u,v+d,     u+d,v+d+h),
        ("north", [5,4,7,6],     u+d,v+d,   u+d+w,v+d+h),
        ("east",  [9,5,6,10],    u+d+w,v+d, u+2*d+w,v+d+h),
        ("south", [8,9,10,11],   u+2*d+w,v+d,u+2*d+2*w,v+d+h),
    ]
    out={}
    for face,vertices,u0,v0,u1,v1 in polygons:
        if mirror and face in ("east","west"):
            face={"east":"west","west":"east"}[face]
        for index,(uu,vv) in zip(vertices,[(u1,v0),(u0,v0),(u0,v1),(u1,v1)]):
            x,y,z=vs[index]
            out[(face,(x,-y,z))]=(uu,vv)
    return out


def test_renderer_box_uvs():
    for mirror in (False,True):
        origin=[13,-27,-51]; size=[17,21,29]; uv=[100,200]
        cube={"origin":origin,"size":size,"uv":uv}
        if mirror: cube["mirror"]=True
        geo={"minecraft:geometry":[{"bones":[{"name":"test","pivot":[0,0,0],"cubes":[cube]}]}]}
        quads=r.load_posed_quads(geo)
        oracle=java_cube_uvs(origin,size,uv,mirror)
        assert len(quads)==6 and len(oracle)==24
        for face in quads:
            for point,uv_actual in zip(face["pts"],face["uvs"]):
                key=(face["fname"],tuple(int(round(i)) for i in point))
                expected=oracle[key]
                assert np.max(np.abs(uv_actual-np.array(expected)))<.002, (mirror,key,uv_actual,expected)


def intersects(a,c):
    ax,ay,aw,ah=a; bx,by,bw,bh=c
    return ax<bx+bw and bx<ax+aw and ay<by+bh and by<ay+ah


def main():
    model=b.build_model()
    cubes=[c for bone in model.bones for c in bone["cubes"]]
    placements=b.small_templates(cubes)
    uvs=b.all_uvs(cubes,placements)
    regions=[b.HEAD_SHEET,b.MOUTH_SHEET,b.SMALL_SHEET,*b.TOP_SHEETS,
             b.TONGUE_SHEET,*b.BODY_SHEETS,b.DARK_SHEET]
    for i,a in enumerate(regions):
        for c in regions[i+1:]:
            assert not intersects(a,c), (a,c)
    # The small templates are uniquely packed; repeated parts intentionally
    # share them and the large reference-projection sheets.
    boxes=[]
    for cb,u,v in placements:
        fw,fh=b.box_footprint(*cb["size"])
        rect=(u,v,fw,fh)
        assert not any(intersects(rect,other) for other in boxes), (cb,rect)
        boxes.append(rect)

    atlas=np.asarray(Image.open(HERE/"el_gran_maja.png").convert("RGBA"))
    assert atlas.shape==(4096,4096,4)
    for cb in cubes:
        u,v=uvs[id(cb)]; w,h,d=cb["size"]
        for name,(x,y,fw,fh) in b.face_rects(u,v,w,h,d).items():
            expected={"north":(w,h),"south":(w,h),"east":(d,h),
                      "west":(d,h),"up":(w,d),"down":(w,d)}[name]
            assert (fw,fh)==expected and fw>0 and fh>0
            assert 0<=x and 0<=y and x+fw<=4096 and y+fh<=4096
            for px,py in [(x,y),(x+fw-1,y),(x,y+fh-1),(x+fw-1,y+fh-1),
                          (x+fw//2,y+fh//2)]:
                assert atlas[py,px,3]==255, (cb["material"],name,px,py)
    test_renderer_box_uvs()

    bb=json.loads((HERE/"el_gran_maja.blockbench.geo.json").read_text())
    rw=json.loads((HERE/"el_gran_maja.riftworld_geo.json").read_text())
    stats=json.loads((HERE/"model_stats.json").read_text())
    assert rw["render_scale"]==1.0 and (rw["texture_width"],rw["texture_height"])==(4096,4096)
    assert stats["texels_per_block_per_axis"]==16
    assert stats["cubes_count"]==len(cubes)
    bb_bones=bb["minecraft:geometry"][0]["bones"]
    assert len(bb_bones)==len(rw["bones"])==13
    previous=set()
    for bb_bone,rw_bone in zip(bb_bones,rw["bones"]):
        assert bb_bone["name"]==rw_bone["name"]
        assert not bb_bone.get("parent") or bb_bone["parent"] in previous
        previous.add(bb_bone["name"])
        assert rw_bone["pivot"]==[bb_bone["pivot"][0],-bb_bone["pivot"][1],bb_bone["pivot"][2]]
        seen_mirror=False
        for cb,rcb in zip(bb_bone["cubes"],rw_bone["cubes"]):
            mirror=bool(cb.get("mirror",False))
            # GeoModel's mirror() remains enabled for following cubes.
            assert mirror or not seen_mirror, "Unmirrored cube after mirrored one"
            seen_mirror|=mirror
            ox,oy,oz=cb["origin"]; w,h,d=cb["size"]
            assert rcb["origin"]==[ox,-(oy+h),oz]
            assert rcb["size"]==cb["size"] and rcb["uv"]==cb["uv"]
            assert bool(rcb.get("mirror",False))==mirror

    s=stats["size_blocks_meters"]
    assert 300<=s["length_z"]<=350 and 105<=s["width_x"]<=120 and 40<=s["height_y"]<=46
    assert sum(c["material"]=="eye" for c in cubes)==8
    roots=[c for c in cubes if c["material"]=="tooth_root"]
    mids=[c for c in cubes if c["material"]=="tooth_mid"]
    tips=[c for c in cubes if c["material"]=="tooth_tip"]
    assert 48 <= len(roots) <= 80, len(roots)       # fewer than the old 136
    assert len(mids)==2*len(roots) and len(tips)==len(roots)
    assert min(c["size"][0] for c in roots)>=12    # thicker than the old 5–8 units
    assert len({c["size"][1] for c in roots})>=5  # deliberately irregular heights
    assert stats["upper_teeth"] + stats["lower_teeth"] == len(roots)
    # The jaw opens downward when its X rotation is negative.
    pivot=np.array(model.by_name["jaw"]["pivot"],dtype=float)
    point=np.array([0.,-210.,-750.])
    posed=r.rot_x(-13)@(point-pivot)+pivot
    assert posed[1]<point[1]

    # Automated watertightness check across the posterior skull & head-to-neck junction:
    # verify zero dorsal holes (X, Z) and zero rear occipital holes (X, Y)!
    head_neck_cubes = [
        c for bn in ("head", "neck_1") for c in model.by_name[bn]["cubes"]
    ]
    for gx in range(-420, 421, 8):
        for gz in range(-480, 405, 16):
            hit_top = any(
                c["origin"][0] <= gx < c["origin"][0] + c["size"][0]
                and c["origin"][2] <= gz < c["origin"][2] + c["size"][2]
                and c["origin"][1] + c["size"][1] > 40
                for c in head_neck_cubes
            )
            assert hit_top, f"Dorsal skull/neck gap detected at x={gx}, z={gz}"
    for gx in range(-360, 361, 8):
        for gy in range(-40, 145, 12):
            hit_rear = any(
                c["origin"][0] <= gx < c["origin"][0] + c["size"][0]
                and c["origin"][1] <= gy < c["origin"][1] + c["size"][1]
                and c["origin"][2] + c["size"][2] >= 280
                for c in head_neck_cubes
            )
            assert hit_rear, f"Posterior occipital skull hole detected at x={gx}, y={gy}"

    print(f"OK: {len(cubes)} cuboids, 13 bones, 4096×4096 atlas, 16 texels/block/axis.")
    print(f"OK: bounds {s['length_z']} × {s['width_x']} × {s['height_y']} blocks; jaw opens downward; 0 rear skull holes.")
    print("OK: Java box-UV and mirror oracle, atlas regions, templates, alpha and export ordering.")
    print("NOTE: projection sheets and symmetric/repeated parts deliberately reuse UV pixels.")


if __name__=="__main__":
    main()
