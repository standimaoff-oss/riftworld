#!/usr/bin/env python3
"""Standalone El Gran Maja model. No files in mod/ are read or written.

Coordinates are integer sixteenths of a Minecraft block. Every box uses normal
box UVs, one atlas pixel for one model unit (16 texels per metre on each axis).
The principal front/side contours are traced from reference 03. Material sheets
and small-part templates do not overlap, but many cube faces intentionally reuse
or partially overlap those sheets. See README.md for the model's limitations.
"""
from __future__ import annotations

import argparse
import json
import math
import pathlib
from collections import Counter

import numpy as np
from PIL import Image

HERE = pathlib.Path(__file__).resolve().parent
REF = HERE.parent / "el_gran_maja_refs" / "03_ref_el_gran_maja_full_body_model.png"
HALF = 928                    # half of 116 blocks
HEIGHT = 720                  # 45 blocks, front silhouette
YTOP = HEIGHT // 2
NOSE, TAIL = -900, 4252       # 322 blocks in the traced profile
LENGTH = TAIL - NOSE

# Normalized, traced front outlines, from the 714x277-pixel reference head.
# u=abs(x)/half-width; v=0 at crown, v=1 at lowest point of the chin.
FRONT_TOP = [(0,0),(.12,.022),(.20,.043),(.28,.094),(.36,.217),
             (.44,.282),(.56,.303),(.72,.282),(.88,.310),(1,.343)]
FRONT_BOTTOM = [(0,.996),(.16,.996),(.24,.946),(.32,.884),(.40,.805),
                (.52,.812),(.53,.812),(.60,.715),(.72,.715),
                (.80,.643),(.92,.556),(.96,.527),(1,.386)]
FRONT_RIM = [(.30,.35),(.42,.42),(.56,.47),(.78,.47),(.90,.42),(1,.35)]
UPPER_LIP = [(0,.39),(.16,.42),(.28,.49),(.41,.55)]
GUM_BOTTOM = [(0,.50),(.16,.505),(.28,.515),(.30,.515)]
LOWER_GUM = [(0,.90),(.12,.88),(.20,.82),(.27,.72),(.32,.62),(.40,.60)]
LOWER_TIP = [(0,.80),(.12,.79),(.20,.72),(.27,.62),(.30,.58)]
# Traced side silhouette, t=0 at snout and t=1 at tail tip. The side and
# front of a painted reference are not a mathematically identical projection.
DOME_FRONT = [(0.0,.116),(.011,.114),(.029,.10),(.069,.075),(.112,.05),
              (.162,.030),(.24,.014),(.36,0),(.55,0)]
DOME_BACK = [(0,.124),(.011,.126),(.022,.15),(.058,.175),(.116,.20),
             (.199,.225),(.224,.25),(.40,.26),(.55,.26)]
JAW_SIDE_BOTTOM = [(0,1.0),(.025,.985),(.05,.94),(.10,.93),(.15,.87),
                   (.18,.80),(.21,.72),(.245,.68)]


def interp(points, x):
    return float(np.interp(x, [a for a, _ in points], [b for _, b in points]))


def q(value, step=1):
    return int(round(value / step) * step)


def vy(v):
    return YTOP - HEIGHT * v


def tz(t):
    return NOSE + LENGTH * t


def u_of(x):
    return abs(x) / HALF


def upper_z(u):
    return -865 + 365 * min(1.0, u / .30) ** 1.75


def lower_z(u):
    return -875 + 450 * min(1.0, u / .40) ** 1.65


def outer_wing_z(u):
    return interp([(.31,-690),(.42,-590),(.55,-415),(.72,-185),
                   (.86,35),(1.0,280)], u)


def dome_width(v):
    # Inverse of the crown part of the front outline; stop at the wing root.
    a = [(vv, uu) for uu, vv in FRONT_TOP if uu <= .36]
    return HALF * min(.36, interp(a, v))


class Builder:
    def __init__(self):
        self.bones = []
        self.by_name = {}
        self.serial = 0

    def bone(self, name, pivot, parent=None, anim=None):
        b = {"name": name, "pivot": [q(v) for v in pivot], "parent": parent,
             "anim": anim, "cubes": []}
        self.bones.append(b)
        self.by_name[name] = b

    def cube(self, bone, origin, size, material, key=None, mirror=False, meta=None):
        origin = [q(v) for v in origin]
        size = [max(1, q(v)) for v in size]
        self.serial += 1
        if key is None:
            key = ("unique", self.serial)
        else:
            # UV templates can only be shared by boxes with the exact same net.
            key = (material, tuple(size), key)
        cb = {"origin": origin, "size": size, "material": material, "key": key,
              "mirror": bool(mirror), "meta": meta or {}}
        self.by_name[bone]["cubes"].append(cb)
        return cb

    def pair(self, bone, x0, y, z, w, h, d, material, key=None, meta=None):
        x0, w = q(x0), max(1, q(w))
        if key is None:
            self.serial += 1
            key = ("pair", self.serial)
        positive = self.cube(bone, [x0,y,z], [w,h,d], material, key, False, meta)
        negative = self.cube(bone, [-x0-w,y,z], [w,h,d], material, key, True, meta)
        return positive, negative

    def centered(self, bone, y, z, w, h, d, material, key=None, meta=None):
        w = max(2, q(w, 2))
        return self.cube(bone, [-w//2,y,z], [w,h,d], material, key, False, meta)


def tiled_pair(m, bone, x, y, z0, w, h, z1, material, key, tile=96, variants=2, meta=None):
    """Split long strips. Reusing a small number of *full-size* nets retains
    texel density while keeping an enormous creature's atlas practical."""
    z0, z1 = q(z0), q(z1)
    z = z0
    i = 0
    while z < z1:
        d = min(tile, z1-z)
        m.pair(bone, x, y, z, w, h, d, material,
               key=(key, i % variants), meta=meta)
        z += d
        i += 1


def build_model():
    m = Builder()
    m.bone("head", [0, 0, -250], anim={"axis":"y", "amp":.8, "speed":.45, "phase":0})
    m.bone("jaw", [0, -20, 100], parent="head",
           anim={"axis":"x", "amp":2, "speed":.55, "phase":.4})

    # Skull: horizontal layers bounded independently by the front and side
    # reference trace. Three overlapping depth sections softly round each layer.
    n_layers = 26
    for i in range(n_layers):
        v0, v1 = .39*i/n_layers, .39*(i+1)/n_layers
        vm = (v0+v1)/2
        y0, y1 = q(vy(v1)), q(vy(v0))
        hw = max(8, q(dome_width(v1)))
        zf, zb = q(tz(interp(DOME_FRONT, vm))), q(tz(interp(DOME_BACK, vm)))
        if zb <= zf:
            continue
        # Decrease the width near either end of each layered slab. The widest
        # section preserves the traced front envelope.
        cuts = [zf, q(zf+(zb-zf)*.22), q(zf+(zb-zf)*.78), zb]
        for j, factor in enumerate((.87, 1.0, .89)):
            if cuts[j+1] <= cuts[j]:
                continue
            width = max(6, q(hw*factor))
            # Splitting the deep central part keeps box UV footprints small.
            tiled_pair(m, "head", 0, y0, cuts[j], width, y1-y0,
                       cuts[j+1]+(2 if j < 2 else 0), "crown_top", (i,j), tile=128, variants=2)
            if j < 2:
                # A very thin textured front layer keeps the traced front
                # projection, while the actual upper surface has X/Z paint.
                m.pair("head", 0, y0, cuts[j]-2, width, y1-y0, 2,
                       "crown_face", key=("crown_face", i,j))

    # Defined overhanging upper visor; its bottom edge is the reference arch.
    # The mouth is substantially narrower than the wing span.
    for x in range(0, q(.41*HALF), 16):
        w = min(16, q(.41*HALF)-x)
        u = u_of(x+w/2)
        v_top = max(interp(FRONT_TOP,u), .30 + .035*min(1,u/.41))
        v_bottom = interp(UPPER_LIP,u)
        if v_bottom <= v_top:
            continue
        y0, y1 = q(vy(v_bottom)), q(vy(v_top))
        zf = q(-900 + 425*(u/.41)**1.65)
        tiled_pair(m,"head",x,y0,zf,w,y1-y0,300,"visor_top",("visor",x),tile=96,variants=2)
        m.pair("head",x,y0,zf-2,w,y1-y0,2,"visor_face",key=("visor_face",x))

    # Deep, dished lateral wings. Top and underside are distinct volumes;
    # the front relief has a pale raised rim, and the traced bottom retains the
    # two ledges at either side of the mouth.
    x_start = 296
    for x in range(x_start, HALF, 16):
        w = min(16, HALF-x)
        u = u_of(x+w/2)
        ytop = q(vy(interp(FRONT_TOP,u)))
        yrim = q(vy(interp(FRONT_RIM,u)))
        ybot = q(vy(interp(FRONT_BOTTOM,u)))
        zfront = q(outer_wing_z(u))
        zback = q(interp([(.31,420),(.6,440),(.84,420),(1,360)],u))
        n = max(1, math.ceil((zback-zfront)/96))
        # Each strip has a raised edge and a gently lowered basin.
        for j in range(n):
            z0 = zfront+j*96
            z1 = min(zback,z0+96)
            if z1 <= z0:
                continue
            f = (j+.5)/n
            dish = q(24*math.sin(math.pi*f), 8)
            cap_top = max(yrim+5, ytop-dish)
            if cap_top > yrim:
                m.pair("head",x,yrim,z0,w,cap_top-yrim,z1-z0,"wing_top",
                       key=("wing_cap",x,dish,j%2))
                if j == 0:
                    m.pair("head",x,yrim,z0-2,w,cap_top-yrim,2,"wing_face",
                           key=("wing_face",x))
            # Under the inner wing the frontal red mouth pouch remains visible.
            inset = q(max(0, (.49-u)/.18)*195)
            low_front = max(z0, zfront+inset)
            # Preserve the front outline but lift the underside toward the
            # throat according to the independently traced side silhouette.
            tside=((low_front+z1)*.5-NOSE)/LENGTH
            ybot_side=max(ybot,q(vy(interp(JAW_SIDE_BOTTOM,tside))))
            if z1 > low_front and yrim > ybot_side:
                m.pair("head",x,ybot_side,low_front,w,yrim-ybot_side,z1-low_front,"wing_under_top",
                       key=("wing_under",x,j%2))
                if z0 <= zfront+inset < z1:
                    m.pair("head",x,ybot_side,low_front-2,w,yrim-ybot_side,2,"wing_under_face",
                           key=("wing_under_face",x))
        # Separate sculpted, pale lip/rim, just ahead of the surface.
        if yrim > ybot+8:
            m.pair("head",x,yrim-7,zfront-9,w,15,30,"wing_rim",key=("rim",x))

    # Rear occiput wall. The traced skull stops at the back of the dome, which
    # left two open windows behind the head (they read as holes in game); this
    # solid wall closes them from the inside, hidden by the neck in front of it.
    occ_z0 = q(tz(.205))
    for x in range(0, HALF, 24):
        w = min(24, HALF-x)
        u = u_of(x+w/2)
        ytop = q(vy(interp(FRONT_TOP,u)))
        ylow = q(vy(interp(FRONT_BOTTOM,u)))
        if ytop - ylow < 8:
            continue
        for k in range(0, ytop-ylow, 96):
            h = min(96, ytop-ylow-k)
            m.pair("head",x,ylow+k,occ_z0,w,h,72,"crown_top",key=("occiput",x,k//96))

    # Back wall and lateral warm pouches. Both are hidden behind the gums and
    # teeth; they are actual volumes, not a billboard or a photograph.
    for x in range(0, q(.41*HALF), 16):
        w = min(16,q(.41*HALF)-x)
        u = u_of(x+w/2)
        if u <= .30:
            v0 = interp(UPPER_LIP,u)+.012
            v1 = min(interp(LOWER_GUM,u)-.02, .87)
            mat = "throat"
            zf = -210
        else:
            v0 = interp(UPPER_LIP,u)+.01
            v1 = interp([(.30,.72),(.34,.69),(.38,.63),(.41,.57)],u)
            mat = "mouth_cheek"
            # The dark inner cheek must lie in front of the dark underside of
            # the wing, otherwise a flat green wedge hides the reference pouch.
            wing_inset = max(0, (.49-u)/.18)*195
            zf = q(min(-630+300*(u-.30)/.11, outer_wing_z(u)+wing_inset-36))
        if v1 > v0:
            m.pair("head",x,q(vy(v1)),zf,w,q(HEIGHT*(v1-v0)),160,mat,key=(mat,x))

    # Thick, vertically striated upper gum pad, from the traced upper lip down
    # to the nearly level root line of the needle curtain.
    for x in range(0, q(.30*HALF), 20):
        w = min(20,q(.30*HALF)-x)
        u = u_of(x+w/2)
        v0 = interp(UPPER_LIP,u)
        v1 = interp(GUM_BOTTOM,u)
        if v1 <= v0:
            continue
        y0, y1 = q(vy(v1)), q(vy(v0))
        zf = q(upper_z(u))
        tiled_pair(m,"head",x,y0,zf,w,y1-y0,-240,"gum_upper",("uppergum",x),tile=96,variants=1)

    # Sculpted lower jaw: its front is the traced U-shaped arc; toward the
    # hinge the underside follows the other reference's side silhouette.
    jaw_outer = q(.40*HALF)
    for x in range(0,jaw_outer,16):
        w = min(16,jaw_outer-x)
        u = u_of(x+w/2)
        z0 = q(lower_z(u))
        zmax = q(tz(.215))
        for j,z in enumerate(range(z0,zmax,96)):
            d = min(96,zmax-z)
            t = (z+d*.5-NOSE)/LENGTH
            v_bottom = min(interp(FRONT_BOTTOM,u),interp(JAW_SIDE_BOTTOM,t))
            v_mouth = min(interp(LOWER_GUM,u),v_bottom-.045)
            if v_bottom <= v_mouth:
                continue
            yb, yt = q(vy(v_bottom)), q(vy(v_mouth))
            m.pair("jaw",x,yb,z,w,max(1,yt-yb),d,"chin_top",key=("chin",x,j%2))
            if j == 0:
                m.pair("jaw",x,yb,z-2,w,max(1,yt-yb),2,"chin_face",key=("chin_face",x))
            # Internal floor. Dark, shaded tissue, continuous to the hinge.
            m.pair("jaw",x,yt-2,z,w,7,d,"tongue",key=("tongue",x,j%2))
        # Gum rolls only around the front arch; they meet the raised cheek.
        vgum = interp(LOWER_GUM,u)
        y = q(vy(vgum))
        if u < .37:
            m.pair("jaw",x,y-8,z0-8,w,18,60,"gum_lower",key=("lowergum",x))

    # Three small wet eyes per cluster (as reference 03) plus a subtler fourth
    # bead (visible in reference 01), each genuinely raised above a socket.
    eyes = [(.073,.206,20),(.120,.247,20),(.166,.207,26),(.205,.238,12)]
    for i,(u,v,diam) in enumerate(eyes):
        xc = q(u*HALF); yc = q(vy(v)) - 6
        # The dome surface at this height: both beads are mostly embedded, so a
        # small dark eye sits flush in a shallow socket instead of a raised cube.
        surf = q(tz(interp(DOME_FRONT,v)))
        # Flat, thin beads lying on the dome: they follow the sloped surface far
        # more closely than a tall block, which used to stick out like a wart.
        m.pair("head",xc-(diam+6)//2,yc-8,surf-14,
               diam+6,8,18,"eye_socket",key=("socket",diam))
        m.pair("head",xc-diam//2,yc-6,surf-7,
               diam,6,11,"eye",key=("eye",diam))
    # Smooth brow ridge: many thin overlapping wedges follow the dome instead
    # of three tall blocks, so the forehead reads as one continuous curve.
    for k in range(28):
        x = 30 + k * 9
        if x > q(.30*HALF) - 6:
            break
        u = u_of(x)
        topv = interp(FRONT_TOP, u) + .012
        thick = 4 if abs(u - .14) < .05 else 3
        m.pair("head", x, q(vy(topv) - thick), -246 + k * 6, 9, thick, 16,
               "brow", key=("brow", k % 4))

    # Fewer, wider individual teeth: four joined, tapering 3-D sections per
    # tooth, with independent lean, bend, depth and length. The sparse rear
    # teeth are staggered rather than an even second comb.
    def add_tooth(bone, xc, zf, root_y, tip_y, down, rear, i):
        length = max(18, q(abs(root_y - tip_y), 4))
        root_w = (12 if rear else 16) + 2 * ((i * 3 + rear) % 3)
        root_d = (11 if rear else 14) + 2 * ((i + rear) % 2)
        lean = (-1 if i % 3 == 0 else 1) * (3 + (i * 5) % 8)
        sweep = ((i * 7 + rear * 3) % 9) - 4
        fractions = (0.0, .28, .56, .80, 1.0)
        widths = (root_w, max(9, root_w - 4), max(6, root_w - 9), 3 + i % 3)
        depths = (root_d, max(8, root_d - 3), max(5, root_d - 7), 4)
        sign = -1 if down else 1
        for section in range(4):
            t0, t1 = fractions[section], fractions[section + 1]
            y0 = q(root_y + sign * length * t0)
            y1 = q(root_y + sign * length * t1)
            # The offset grows towards the tip, so each tooth bends instead of
            # reading as two perfectly vertical prisms.
            xoff = q(lean * (t0 ** 1.3))
            zoff = q(sweep * (t0 ** 1.2))
            w, d = widths[section], depths[section]
            mat = "tooth_root" if section == 0 else ("tooth_tip" if section == 3 else "tooth_mid")
            m.pair(bone, xc + xoff - w // 2, min(y0, y1), zf + zoff,
                   w, max(1, abs(y1 - y0) + 1), d, mat,
                   key=(mat, "down" if down else "up", rear, i % 3),
                   meta={"dir": "down" if down else "up"})

    tooth_end = q(.282 * HALF)
    for i, xc in enumerate(range(10, tooth_end, 20)):
        u = u_of(xc)
        gum_v = interp(GUM_BOTTOM, u)
        jitter = (((i * i * 3 + i * 7 + 3) % 13) - 6) * .0048
        zf = q(upper_z(u) + 5 + ((i * 5) % 7) - 3)
        add_tooth("head", xc, zf, vy(gum_v) + 4, vy(.648 + jitter), True, 0, i)
        if i in (2, 6, 10):
            add_tooth("head", xc + 5, zf + 42, vy(gum_v) + 1,
                      vy(.620 + jitter * .8), True, 1, i)
    for i, xc in enumerate(range(11, q(.295 * HALF), 23)):
        u = u_of(xc)
        vbase = interp(LOWER_GUM, u)
        vtip = interp(LOWER_TIP, u) + (((i * i * 5 + i * 3 + 2) % 13) - 6) * .0045
        zf = q(lower_z(u) + 8 + ((i * 5) % 7) - 3)
        add_tooth("jaw", xc, zf, vy(vbase) + 4, vy(vtip), False, 0, i)
        if i in (1, 6, 10):
            add_tooth("jaw", xc + 5, zf + 38, vy(vbase) + 2,
                      vy(vtip + .025), False, 1, i)

    # Body, dorsal/ventral sails and thin lateral keels traced along side view.
    top_core = [(.23,.225),(.30,.245),(.35,.225),(.43,.19),(.50,.19),(.60,.205),
                (.70,.225),(.80,.25),(.90,.29),(.97,.35),(1,.43)]
    bottom_core = [(.23,.69),(.30,.665),(.40,.66),(.50,.64),(.60,.625),(.70,.61),
                   (.80,.59),(.90,.56),(.97,.52),(1,.47)]
    sail_top = [(.23,.225),(.32,.245),(.35,.20),(.40,.125),(.43,.083),(.50,.09),
                (.60,.15),(.70,.17),(.80,.22),(.90,.28),(.97,.35),(1,.43)]
    vent_bottom = [(.23,.69),(.40,.66),(.44,.81),(.48,.76),(.55,.71),(.65,.69),
                   (.75,.65),(.85,.61),(.95,.55),(1,.47)]
    core_width = [(.23,345),(.30,300),(.38,288),(.48,272),(.58,235),(.68,195),
                  (.78,150),(.88,100),(.95,52),(1,8)]
    names = ["neck_1","neck_2","trunk_1","trunk_2","trunk_3",
             "tail_1","tail_2","tail_3","tail_4","tail_5","tail_6"]
    knots = np.linspace(.235,1.0,len(names)+1)
    parent = "head"
    for bi,name in enumerate(names):
        z0,z1 = q(tz(knots[bi])),q(tz(knots[bi+1]))
        m.bone(name,[0,0,z0],parent=parent,
               anim={"axis":"y","amp":1.0+.6*bi,"speed":.65,"phase":-.48*bi})
        parent = name
        z = z0
        while z < z1:
            d = min(96,z1-z)
            t = (z+d*.5-NOSE)/LENGTH
            yt = q(vy(interp(top_core,t)),8)
            yb = q(vy(interp(bottom_core,t)),8)
            wm = max(8,q(interp(core_width,t),16))
            height = max(16,yt-yb)
            hb = q(height*.24,8); ht = q(height*.24,8)
            hm = height-hb-ht
            wt = max(8,q(wm*.76,16)); wb = max(8,q(wm*.76,16))
            mat = "neck" if bi<2 else "body"
            # Reused only if the net dimensions match exactly; the pattern
            # deliberately has three broad longitudinal variations.
            variant = bi//4
            m.centered(name,yb+hb+hm,z,wt,ht,d,mat,key=(mat,"top",variant))
            m.centered(name,yb+hb,z,wm,hm,d,mat,key=(mat,"mid",variant))
            m.centered(name,yb,z,wb,hb,d,"belly",key=("belly",variant))
            ysail = q(vy(interp(sail_top,t)),8)
            if ysail > yt+8:
                sw = max(8,q(min(56,wm*.30),8))
                m.centered(name,yt,z,sw,ysail-yt,d,"fin",key=("dorsal",variant))
            yfin = q(vy(interp(vent_bottom,t)),8)
            if yfin < yb-8:
                fw = max(8,q(min(40,wm*.23),8))
                m.centered(name,yfin,z,fw,yb-yfin,d,"fin",key=("ventral",variant))
            if wm >= 64 and .29<t<.83:
                keel_w = max(8,q(min(48,wm*.16),8))
                keel_h = max(8,q(hm*.18,8))
                m.pair(name,wm//2,yb+hb+q(hm*.53),z,keel_w,keel_h,d,"keel",
                       key=("keel",variant))
            z += d
    return m


def box_footprint(w,h,d):
    return 2*d+2*w,d+h


def face_rects(u,v,w,h,d):
    return {"up":(u+d,v,w,d), "down":(u+d+w,v,w,d),
            "east":(u,v+d,d,h), "north":(u+d,v+d,w,h),
            "west":(u+d+w,v+d,d,h), "south":(u+2*d+w,v+d,w,h)}


# Shared projection sheets. They intentionally overlap between different cubes:
# a different UV origin is chosen for every cube so each north/east face still
# samples one pixel per model unit. This is texture reuse, not downscaling.
ATLAS_W = ATLAS_H = 4096
HEAD_SHEET = (0, 0, 1408, 1024)
MOUTH_SHEET = (1408, 0, 1408, 1024)
SMALL_SHEET = (2816, 0, 1280, 1024)
TOP_SHEETS = [(0,1024,1408,1024),(1408,1024,1408,1024)]
TONGUE_SHEET = (2816,1024,1280,1024)
DARK_SHEET = (0,3584,1408,512)
BODY_SHEETS = [(0,2048,2048,768),(2048,2048,2048,768),
               (0,2816,2048,768),(2048,2816,2048,768)]
PAD = 160
BODY_PAD = 128
BODY_START = q(tz(.235))
BODY_STRIDE = 1000
TOP_START = -910
TOP_STRIDE = 700
SKIN_MATS = {"crown_face","visor_face","wing_face","wing_under_face","wing_rim","chin_face","brow"}
TOP_MATS = {"crown_top","visor_top","wing_top"}
DARK_MATS = {"wing_under_top","chin_top"}
FLESH_MATS = {"throat","mouth_cheek","mouth_wall","gum_upper","gum_lower"}
BODY_MATS = {"body","neck","belly","fin","keel"}
SMALL_MATS = {"eye","eye_socket","tooth_root","tooth_mid","tooth_tip"}


def hash2(x,y,seed=0):
    # Stable integer pattern, not randomized Python hashes.
    a = x.astype(np.int64)*374761393 + y.astype(np.int64)*668265263 + seed*1442695041
    a = (a ^ (a >> 13))*1274126177
    a = (a ^ (a >> 16)) & 0x7fffffff
    return a.astype(np.float32)/2147483647.0


def small_templates(cubes):
    reps = {}
    for cb in cubes:
        if cb["material"] in SMALL_MATS:
            reps.setdefault(cb["key"],cb)
    items=[]
    for cb in reps.values():
        w,h,d=cb["size"]
        fw,fh=box_footprint(w,h,d)
        items.append((cb,fw+2,fh+2))
    items.sort(key=lambda t:(-t[2],-t[1]))
    sx,sy,sw,sh=SMALL_SHEET
    shelves=[]; placed=[]
    for cb,w,h in items:
        found=False
        for shelf in shelves:
            if h<=shelf[2] and shelf[1]+w<=sw:
                x,y=shelf[1],shelf[0]; shelf[1]+=w; found=True; break
        if not found:
            y=sum(s[2] for s in shelves)
            if y+h>sh or w>sw: raise RuntimeError("Small-part atlas region is full")
            x=0; shelves.append([y,w,h])
        placed.append((cb,sx+x+1,sy+y+1))
    return placed


def uv_for(cb,template_uv):
    mat=cb["material"]; ox,oy,oz=cb["origin"]; w,h,d=cb["size"]
    if mat in SMALL_MATS:
        return template_uv[cb["key"]], SMALL_SHEET
    if mat in SKIN_MATS or mat in FLESH_MATS:
        sheet=HEAD_SHEET if mat in SKIN_MATS else MOUTH_SHEET
        sx,sy,_,_=sheet
        # All these boxes are paired. Mirror the positive half via normal box
        # UV mirroring, with the same source texels as its partner.
        x0 = -ox-w if cb["mirror"] else ox
        return (sx+PAD+x0-d, sy+PAD+YTOP-(oy+h)-d),sheet
    if mat in TOP_MATS:
        index=max(0,min(1,(oz-TOP_START)//TOP_STRIDE))
        sheet=TOP_SHEETS[index]
        sx,sy,_,_=sheet
        x0=-ox-w if cb["mirror"] else ox
        # Bedrock's up face reverses V. The sheet's Z axis is painted in
        # reverse too, so an exposed top retains continuous world X/Z paint.
        z_end=TOP_START+TOP_STRIDE*index+840
        return (sx+PAD+x0-d,sy+z_end-(oz+d)),sheet
    if mat in DARK_MATS:
        sx,sy,_,sh=DARK_SHEET
        x0=-ox-w if cb["mirror"] else ox
        v0=(oz+900)%max(1,sh-d-h+1)
        return (sx+PAD+x0-d,sy+v0),DARK_SHEET
    if mat == "tongue":
        sx,sy,_,_=TONGUE_SHEET
        x0=-ox-w if cb["mirror"] else ox
        local_z=(oz+900)%800
        return (sx+PAD+x0-d,sy+900-local_z-d),TONGUE_SHEET
    if mat in BODY_MATS:
        index = max(0,min(len(BODY_SHEETS)-1,(oz-BODY_START)//BODY_STRIDE))
        sheet=BODY_SHEETS[index]
        sx,sy,_,_=sheet
        zstart=BODY_START+BODY_STRIDE*index
        # The west face of a Bedrock box reads U in decreasing Z order.
        return (sx+BODY_PAD+zstart+1100-(oz+d),sy+BODY_PAD+YTOP-(oy+h)-d),sheet
    raise ValueError(f"No texture region for {mat}")


def all_uvs(cubes,placed):
    template_uv={cb["key"]:(u,v) for cb,u,v in placed}
    result={}
    for cb in cubes:
        (u,v),sheet=uv_for(cb,template_uv)
        sx,sy,sw,sh=sheet
        fw,fh=box_footprint(*cb["size"])
        if not (sx<=u and sy<=v and u+fw<=sx+sw and v+fh<=sy+sh):
            raise ValueError(f"UV net outside material sheet: {cb['material']} {cb['origin']} {cb['size']} uv={(u,v)} sheet={sheet}")
        result[id(cb)]=(u,v)
    return result


def clean_front(ref):
    """Remove painted eyes and pale needle streaks: the model adds genuine
    three-dimensional eyes/teeth instead of relying on fake image details."""
    im=ref.copy()
    # Small eye zones: use adjacent reference skin to restore the dark socket.
    for left,right in [(287,435)]:
        for y in range(83,116):
            left_col=im[y,max(4,left-3)].astype(np.float32)
            right_col=im[y,min(717,right+3)].astype(np.float32)
            mix=np.linspace(0,1,right-left,endpoint=False,dtype=np.float32)[:,None]
            im[y,left:right]=np.clip(left_col[None,:]*(1-mix)+right_col[None,:]*mix,0,255).astype(np.uint8)
    return im


def make_head_region(ref, flesh=False):
    sx,sy,sw,sh = MOUTH_SHEET if flesh else HEAD_SHEET
    xx=np.arange(sw,dtype=np.float32)[None,:]-PAD
    yy=np.arange(sh,dtype=np.float32)[:,None]-PAD
    # At a north face, atlas coords correspond exactly to physical X and Y.
    px=np.clip(np.rint(361+xx/HALF*357),4,717).astype(np.int32)
    py=np.clip(np.rint(40+yy/HEIGHT*277),0,ref.shape[0]-1).astype(np.int32)
    clean=clean_front(ref)
    sample=clean[py,px].astype(np.float32)
    bg=ref[py,748].astype(np.float32)
    inside=(np.linalg.norm(sample-bg,axis=2)>15)
    u=np.abs(xx/HALF); v=yy/HEIGHT
    ref_top=np.interp(u,[a for a,b in FRONT_TOP],[b for a,b in FRONT_TOP])
    ref_bottom=np.interp(u,[a for a,b in FRONT_BOTTOM],[b for a,b in FRONT_BOTTOM])
    mouth_top=np.interp(u,[a for a,b in UPPER_LIP],[b for a,b in UPPER_LIP])
    mouth_bottom=np.interp(u,[a for a,b in LOWER_GUM],[b for a,b in LOWER_GUM])
    silhouette=(u<=1)&(v>=ref_top-.005)&(v<=ref_bottom+.005)
    # Blocky, spatially coherent markings: large plates, fine fissures, sea
    # growth and flecks, all derived from the sampled reference palette.
    cellx=np.floor(xx/26).astype(np.int32)
    celly=np.floor(yy/22).astype(np.int32)
    pat=hash2(np.broadcast_to(cellx,(sh,sw)),np.broadcast_to(celly,(sh,sw)),13)
    large=hash2(np.broadcast_to(np.floor(xx/83).astype(np.int32),(sh,sw)),
                np.broadcast_to(np.floor(yy/70).astype(np.int32),(sh,sw)),29)
    if not flesh:
        base=np.empty((sh,sw,3),dtype=np.float32)
        base[:]=[53,72,69]                       # sampled crown / wing slate
        shadow=np.clip((v-.35)*3.6,0,.66)
        base=base*(1-shadow[...,None])
        # Never transfer the reference's mouth, teeth, water, fish or plants
        # to armour. Those areas are replaced by the sampled slate palette.
        mouth_area=(u<.41)&(v>mouth_top-.006)&(v<mouth_bottom+.025)
        warm=(sample[...,0]>sample[...,1]*1.07)&(v>.37)&(u<.43)
        valid=inside&silhouette&(~mouth_area)&(~warm)
        col=np.where(valid[...,None],sample,base)
        col=col*(.965+.07*large[...,None])
        # Squared, irregular masonry-like carapace plates rather than flat noise.
        row=np.floor(yy/108).astype(np.int32)
        stagger=(row%2)*39
        edge_x=np.mod(xx+stagger+np.floor(yy/30)*3,145)
        edge_y=np.mod(yy+np.floor(xx/70)*4,108)
        seams=((edge_x<2)|(edge_y<2))&(v<.38)&(u<.38)&inside
        col=np.where(seams[...,None],col*.70,col)
        # Pale marine mottles / barnacle strokes on the mantle and crown.
        fleck=(pat>.89)&(large>.43)&(v>.13)&(v<.49)
        col=np.where(fleck[...,None],col*.73+np.array([83,104,97],np.float32)*.27,col)
        # A broken highlight along the prominent wing rim; sampled from the
        # pale blue-green strip in the reference, not an unrelated hue.
        rim_v=np.interp(np.clip(np.broadcast_to(u,(sh,sw)),.30,1.0),
                        [a for a,b in FRONT_RIM],[b for a,b in FRONT_RIM])
        rim=(u>.32)&(u<1.02)&(np.abs(v-rim_v)<.011)&inside
        col=np.where(rim[...,None],col*.78+np.array([91,111,103],np.float32)*.22,col)
    else:
        base=np.empty((sh,sw,3),dtype=np.float32)
        base[:]=[42,25,28]                        # side pouch / tongue
        central=(u<.29)&(v>.51)&(v<.86)
        base=np.where(central[...,None],np.array([24,15,18],np.float32),base)
        gum=(u<.31)&(v>=.385)&(v<.525)
        base=np.where(gum[...,None],np.array([73,49,51],np.float32),base)
        lower=(u<.38)&(v>.79)
        base=np.where(lower[...,None],np.array([60,42,44],np.float32),base)
        # Teeth in the original are deliberately removed from this image: the
        # separated cuboids get their own UVs and real silhouette.
        toothlike=(sample[...,0]>58)&(sample[...,0]-sample[...,1]<18)&(sample[...,1]>sample[...,2]*1.04)&(v>.495)&(u<.33)
        valid=inside&silhouette&(v>.37)&(v<.98)&(u<.44)&(~toothlike)
        # Reject sampled blue water where a traced volume extends beyond the
        # painted original; blend instead into the red-black material.
        col=np.where(valid[...,None],sample*.79+base*.21,base)
        vertical=(np.mod(xx+np.floor(yy/90)*3,23)<2)&(gum|lower)
        broad=(np.mod(xx+np.floor(yy/65)*4,68)<6)&(gum|lower)
        col=np.where(vertical[...,None],col*.78,col)
        col=np.where(broad[...,None],col*1.10,col)
        col=col*(.94+.12*large[...,None])
    # Tiny restrained mineral grain. The broad drawn patterns remain dominant.
    grain=(pat-.5)*3.0
    col=np.clip(col+grain[...,None],0,255).astype(np.uint8)
    return col


def make_top_region(index):
    """Paint actual X/Z surfaces: broad weathered plates and irregular
    blue-green lichen. Palette comes from the reference crown and wing."""
    _,_,sw,sh=TOP_SHEETS[index]
    xx=np.arange(sw,dtype=np.float32)[None,:]-PAD
    zz=TOP_START+TOP_STRIDE*index+840-np.arange(sh,dtype=np.float32)[:,None]
    xfull=np.broadcast_to(xx,(sh,sw)); zfull=np.broadcast_to(zz,(sh,sw))
    row=np.floor((zfull+1050)/145).astype(np.int32)
    stagger=(row%2)*58
    # Low-frequency shifts break up the otherwise too-regular grid.
    bend=np.floor((zfull+1000)/53)*3 + np.floor(xfull/160)*2
    col=np.floor((xfull+stagger+bend)/164).astype(np.int32)
    n=hash2(col,row,41)
    pal0=np.array([53,72,68],np.float32)
    pal1=np.array([68,86,80],np.float32)
    out=pal0[None,None,:]*(1-n[...,None]*.70)+pal1[None,None,:]*(n[...,None]*.70)
    # Angular joints between carapace plates: double dark seam and a thin lip.
    xp=np.mod(xfull+stagger+bend,164)
    zp=np.mod(zfull+1050+np.floor(xfull/164)*13,145)
    seam=(xp<3)|(zp<3)
    bevel=((xp>=4)&(xp<7))|((zp>=4)&(zp<7))
    out=np.where(seam[...,None],out*.68,out)
    out=np.where(bevel[...,None],out*1.10,out)
    # Irregular, blocky patches and short pale sea-growth streaks, not uniform
    # per-pixel noise. They are continuous in world coordinates across boxes.
    gridx=np.floor(xfull/35).astype(np.int32); gridz=np.floor(zfull/31).astype(np.int32)
    mott=hash2(gridx,gridz,76)
    localx=np.mod(xfull,35); localz=np.mod(zfull,31)
    # Angular lichen islands with offset centres and irregular sizes; use
    # step-like pixels rather than a continuous single-colour/noise fill.
    blot=(mott>.72)&(localx>4)&(localx<28)&(localz>3)&(localz<22)
    shadow_blot=(mott<.15)&(localx>8)&(localx<30)&(localz>7)&(localz<27)
    out=np.where(blot[...,None],out*.65+np.array([94,114,103],np.float32)*.35,out)
    out=np.where(shadow_blot[...,None],out*.70+np.array([32,52,45],np.float32)*.30,out)
    # Smaller mottled stones and short weathering marks provide an actual
    # drawing within each plate rather than a flat fill. They remain anchored
    # to the same world coordinates across adjacent cuboids.
    chips=hash2(np.floor((xfull+zfull*.12)/9).astype(np.int32),
                np.floor(zfull/11).astype(np.int32),64)
    out=out*(.93+.15*chips[...,None])
    scuff=(np.mod(xfull*2+zfull+np.floor(zfull/70)*13,157)<2)&(chips>.6)
    out=np.where(scuff[...,None],out*.80+np.array([104,123,111],np.float32)*.20,out)
    small=hash2(np.floor(xfull/11).astype(np.int32),np.floor(zfull/9).astype(np.int32),91)
    flakes=(small>.94)&(np.mod(xfull,11)<6)&(np.mod(zfull,9)<4)
    out=np.where(flakes[...,None],out*.55+np.array([121,135,122],np.float32)*.45,out)
    # A few broken barnacle rings / pits on a larger grid.
    ax=np.mod(xfull,80)-40; az=np.mod(zfull,75)-37
    colony=hash2(np.floor(xfull/80).astype(np.int32),np.floor(zfull/75).astype(np.int32),118)
    r2=ax*ax+az*az
    ring=(colony>.84)&(r2>16)&(r2<55)
    out=np.where(ring[...,None],out*.53+np.array([111,126,113],np.float32)*.47,out)
    return np.clip(out,0,255).astype(np.uint8)


def make_dark_region():
    _,_,sw,sh=DARK_SHEET
    xx=np.broadcast_to(np.arange(sw,dtype=np.float32)[None,:]-PAD,(sh,sw))
    yy=np.broadcast_to(np.arange(sh,dtype=np.float32)[:,None],(sh,sw))
    cx=np.floor(xx/45).astype(np.int32); cy=np.floor(yy/38).astype(np.int32)
    n=hash2(cx,cy,55)
    base=np.array([24,29,31],np.float32)
    out=base[None,None,:]*(.91+.18*n[...,None])
    seam=(np.mod(xx+np.floor(yy/72)*13,145)<2)|(np.mod(yy,120)<2)
    out=np.where(seam[...,None],out*.83,out)
    return np.clip(out,0,255).astype(np.uint8)


def make_tongue_region():
    _,_,sw,sh=TONGUE_SHEET
    xx=np.arange(sw,dtype=np.float32)[None,:]-PAD
    zz=900-np.arange(sh,dtype=np.float32)[:,None]
    xfull=np.broadcast_to(xx,(sh,sw)); zfull=np.broadcast_to(zz,(sh,sw))
    cellx=np.floor(xfull/29).astype(np.int32); cellz=np.floor(zfull/43).astype(np.int32)
    n=hash2(cellx,cellz,39)
    a=np.array([51,31,34],np.float32); b=np.array([79,49,49],np.float32)
    out=a[None,None,:]*(1-n[...,None]*.32)+b[None,None,:]*(n[...,None]*.32)
    folds=np.mod(zfull+np.floor(xfull/75)*3,90)
    out=np.where((folds<3)[...,None],out*.78,out)
    out=np.where(((folds>=5)&(folds<8))[...,None],out*1.12,out)
    lines=(np.mod(xfull+np.floor(zfull/60)*2,27)<2)
    out=np.where(lines[...,None],out*.90,out)
    return np.clip(out,0,255).astype(np.uint8)


def make_body_region(ref,index):
    _,_,sw,sh=BODY_SHEETS[index]
    zz=BODY_START+BODY_STRIDE*index+1100-(np.arange(sw,dtype=np.float32)[None,:]-BODY_PAD)
    yy=np.arange(sh,dtype=np.float32)[:,None]-BODY_PAD
    px=np.clip(np.rint(778+(zz-NOSE)/LENGTH*1978),778,2756).astype(np.int32)
    py=np.clip(np.rint(40+yy/HEIGHT*277),0,ref.shape[0]-1).astype(np.int32)
    sample=ref[py,px].astype(np.float32)
    bg=ref[py,748].astype(np.float32)
    side_t=(zz-NOSE)/LENGTH
    side_top=np.interp(side_t,[.235,.30,.35,.40,.43,.50,.60,.70,.80,.90,.97,1],
                       [.22,.245,.20,.125,.083,.09,.15,.17,.22,.28,.35,.43])
    side_bottom=np.interp(side_t,[.235,.40,.44,.48,.55,.65,.75,.85,.95,1],
                          [.69,.66,.81,.76,.71,.69,.65,.61,.55,.47])
    inside=(np.linalg.norm(sample-bg,axis=2)>17)&(yy/HEIGHT>=side_top-.01)&(yy/HEIGHT<=side_bottom+.01)
    # Reference sampling captures myomere silhouettes, horizontal keels and
    # the dark fin membranes. Continue the same palette beyond the silhouette.
    fallback=np.array([26,29,34],np.float32)
    col=np.where(inside[...,None],sample*1.05+fallback*.03,fallback)
    zx=np.broadcast_to(np.floor(zz/24).astype(np.int32),(sh,sw))
    jy=np.broadcast_to(np.floor(yy/28).astype(np.int32),(sh,sw))
    n=hash2(zx,jy,72)
    # Broad diagonal muscle bands and fine fin rays, deliberately drawn on the
    # atlas instead of a single noisy colour.
    phase=np.mod(zz + yy*.33,112)
    seam=(phase<3)&inside
    highlight=(phase>=5)&(phase<9)&inside
    col=np.where(seam[...,None],col*.75,col)
    col=np.where(highlight[...,None],col*1.10,col)
    ray=np.mod(zz+yy*.70,37)
    sail=(yy<HEIGHT*.23)|(yy>HEIGHT*.65)
    col=np.where(((ray<2)&sail&inside)[...,None],col*1.25,col)
    col=col*(.96+.09*n[...,None])
    return np.clip(col,0,255).astype(np.uint8)


def paint_small(atlas,placed):
    for cb,u0,v0 in placed:
        w,h,d=cb["size"]; mat=cb["material"]; meta=cb["meta"]
        for name,(x,y,fw,fh) in face_rects(u0,v0,w,h,d).items():
            xx=np.arange(fw,dtype=np.float32)[None,:]
            yy=np.arange(fh,dtype=np.float32)[:,None]
            if mat=="eye_socket":
                col=np.empty((fh,fw,3),np.float32); col[:]=[22,29,29]
                col*=.90+.17*hash2(np.broadcast_to(xx.astype(np.int32),(fh,fw)),
                                   np.broadcast_to(yy.astype(np.int32),(fh,fw)),9)[...,None]
            elif mat=="eye":
                col=np.empty((fh,fw,3),np.float32); col[:]=[25,24,27]
                r=np.sqrt(((xx-(fw-1)/2)/max(1,fw*.53))**2+((yy-(fh-1)/2)/max(1,fh*.53))**2)
                col=np.where((r>.82)[...,None],np.array([10,13,15],np.float32),col)
                if name=="north":
                    gl=(xx>fw*.27)&(xx<fw*.42)&(yy>fh*.24)&(yy<fh*.36)
                    col=np.where(gl[...,None],np.array([64,66,67],np.float32),col)
            else:
                direction=meta.get("dir","down")
                t=np.broadcast_to(yy/max(1,fh-1),(fh,fw))
                if direction=="up": t=1-t
                if name in ("up","down"):
                    t=np.zeros((fh,fw),np.float32)+.5
                if mat=="tooth_root": a,b=np.array([109,99,88],np.float32),np.array([161,151,135],np.float32)
                elif mat=="tooth_mid": a,b=np.array([139,130,116],np.float32),np.array([187,179,162],np.float32)
                else: a,b=np.array([151,143,128],np.float32),np.array([200,194,178],np.float32)
                col=a[None,None,:]*(1-t[...,None])+b[None,None,:]*t[...,None]
                if fw>3:
                    col*=np.where((xx<1)|(xx>=fw-1),.82,1.0)[...,None]
            atlas[y:y+fh,x:x+fw,:3]=np.clip(col,0,255).astype(np.uint8)
            atlas[y:y+fh,x:x+fw,3]=255


def paint_atlas(placed):
    ref=np.array(Image.open(REF).convert("RGB"))
    atlas=np.zeros((ATLAS_H,ATLAS_W,4),np.uint8)
    for sheet,flesh in [(HEAD_SHEET,False),(MOUTH_SHEET,True)]:
        x,y,w,h=sheet
        print("Painting", "mouth" if flesh else "crown and wing", flush=True)
        atlas[y:y+h,x:x+w,:3]=make_head_region(ref,flesh)
        atlas[y:y+h,x:x+w,3]=255
    for i,(x,y,w,h) in enumerate(TOP_SHEETS):
        print("Painting dorsal plates",i+1,"/",len(TOP_SHEETS),flush=True)
        atlas[y:y+h,x:x+w,:3]=make_top_region(i)
        atlas[y:y+h,x:x+w,3]=255
    x,y,w,h=TONGUE_SHEET
    atlas[y:y+h,x:x+w,:3]=make_tongue_region()
    atlas[y:y+h,x:x+w,3]=255
    x,y,w,h=DARK_SHEET
    atlas[y:y+h,x:x+w,:3]=make_dark_region()
    atlas[y:y+h,x:x+w,3]=255
    for i,(x,y,w,h) in enumerate(BODY_SHEETS):
        print("Painting body",i+1,"/",len(BODY_SHEETS),flush=True)
        atlas[y:y+h,x:x+w,:3]=make_body_region(ref,i)
        atlas[y:y+h,x:x+w,3]=255
    paint_small(atlas,placed)
    return atlas


def export_all(plan=False):
    m=build_model()
    cubes=[c for b in m.bones for c in b["cubes"]]
    placed=small_templates(cubes)
    uvs=all_uvs(cubes,placed)
    lo=[min(c["origin"][i] for c in cubes) for i in range(3)]
    hi=[max(c["origin"][i]+c["size"][i] for c in cubes) for i in range(3)]
    spans=[hi[i]-lo[i] for i in range(3)]
    mats=Counter(c["material"] for c in cubes)
    stats={
        "bones_count":len(m.bones), "cubes_count":len(cubes),
        "upper_teeth":sum(c["material"]=="tooth_root" for c in m.by_name["head"]["cubes"]),
        "lower_teeth":sum(c["material"]=="tooth_root" for c in m.by_name["jaw"]["cubes"]),
        "upper_tooth_cuboids":sum(c["material"].startswith("tooth") for c in m.by_name["head"]["cubes"]),
        "lower_tooth_cuboids":sum(c["material"].startswith("tooth") for c in m.by_name["jaw"]["cubes"]),
        "eyes":"2 clusters, 4 raised eyes per side",
        "atlas_size":[ATLAS_W,ATLAS_H], "render_scale":1.0,
        "texels_per_block_per_axis":16,
        "uv_layout":"Full-density box UV. Symmetric/repeated parts and reference-projection sheets intentionally reuse/overlap atlas pixels.",
        "small_templates":len(placed),
        "bounds_units":{"x":[lo[0],hi[0]],"y":[lo[1],hi[1]],"z":[lo[2],hi[2]]},
        "size_units":{"width_x":spans[0],"height_y":spans[1],"length_z":spans[2]},
        "size_blocks_meters":{"width_x":round(spans[0]/16,3),"height_y":round(spans[1]/16,3),"length_z":round(spans[2]/16,3)},
        "cuboids_by_material":dict(sorted(mats.items())),
        "front_contours_source":"03_ref_el_gran_maja_full_body_model.png, x=4..717 y=40..316",
        "side_contours_source":"03_ref_el_gran_maja_full_body_model.png, x=778..2756 y=40..316"
    }
    print(json.dumps(stats,ensure_ascii=False,indent=2),flush=True)
    if plan:
        return
    atlas=paint_atlas(placed)
    Image.fromarray(atlas).save(HERE/"el_gran_maja.png",compress_level=7)
    bb_bones=[]; rw_bones=[]
    for b in m.bones:
        px,py,pz=b["pivot"]
        # GeoModel's CubeListBuilder.mirror() persists. Always emit ordinary
        # boxes first and mirrored boxes last for each bone.
        ordered=sorted(b["cubes"],key=lambda c:c["mirror"])
        bb_cubes=[]; rw_cubes=[]
        for cb in ordered:
            ox,oy,oz=cb["origin"]; w,h,d=cb["size"]; u,v=uvs[id(cb)]
            a={"origin":[ox,oy,oz],"size":[w,h,d],"uv":[u,v]}
            r={"origin":[ox,-(oy+h),oz],"size":[w,h,d],"uv":[u,v]}
            if cb["mirror"]: a["mirror"]=True; r["mirror"]=True
            bb_cubes.append(a); rw_cubes.append(r)
        a={"name":b["name"],"pivot":[px,py,pz],"cubes":bb_cubes}
        r={"name":b["name"],"pivot":[px,-py,pz],"cubes":rw_cubes}
        if b["parent"]: a["parent"]=r["parent"]=b["parent"]
        if b["anim"]:
            a["anim"]=dict(b["anim"])
            r["anim"]=dict(b["anim"])
            if r["anim"].get("axis") in ("x","z"):
                r["anim"]["amp"]=-r["anim"]["amp"]
        bb_bones.append(a); rw_bones.append(r)
    rw={"texture_width":ATLAS_W,"texture_height":ATLAS_H,"render_scale":1.0,
        "translucent":False,"bones":rw_bones}
    bb={"format_version":"1.12.0","minecraft:geometry":[{
        "description":{"identifier":"geometry.riftworld.el_gran_maja",
          "texture_width":ATLAS_W,"texture_height":ATLAS_H,
          "visible_bounds_width":round(max(spans[0],spans[2])/16,2),
          "visible_bounds_height":round(spans[1]/16,2),
          "visible_bounds_offset":[0,round((lo[1]+hi[1])/32,2),round((lo[2]+hi[2])/32,2)]},
        "bones":bb_bones}]}
    (HERE/"el_gran_maja.riftworld_geo.json").write_text(json.dumps(rw,ensure_ascii=False,indent=1)+"\n",encoding="utf-8")
    (HERE/"el_gran_maja.blockbench.geo.json").write_text(json.dumps(bb,ensure_ascii=False,indent=1)+"\n",encoding="utf-8")
    (HERE/"model_stats.json").write_text(json.dumps(stats,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print("Wrote texture, models and statistics to",HERE,flush=True)


if __name__=="__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("--plan",action="store_true",help="validate UV nets and print statistics without painting")
    args=parser.parse_args()
    export_all(plan=args.plan)
