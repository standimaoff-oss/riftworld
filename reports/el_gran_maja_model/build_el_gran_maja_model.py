#!/usr/bin/env python3
"""Standalone El Gran Maja model. No files in mod/ are read or written.

Coordinates are integer sixteenths of a Minecraft block. Every box uses normal
box UVs, one atlas pixel for one model unit (16 texels per metre on each axis).
The principal front/side contours are traced from reference 03, with a unified
smoothly curved cranial/mantle carapace, sealed occipital-to-neck collar,
prominent outer masseter cheeks & inner oral-corner cheeks, and 5-tier rounded
serpent body segments.
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

# Smoothly curved front outlines traced from Reference 01 & Reference 03:
# u = abs(x)/HALF; v = 0 at crown apex, v = 1 at lowest point of the chin.
FRONT_TOP = [
    (0.00, 0.000), (0.06, 0.006), (0.12, 0.020), (0.18, 0.042),
    (0.24, 0.078), (0.30, 0.132), (0.36, 0.198), (0.42, 0.252),
    (0.49, 0.286), (0.58, 0.298), (0.70, 0.284), (0.82, 0.298),
    (0.92, 0.322), (1.00, 0.343),
]
# Enlarged, rounded outer masseter/jowl cheek bulge around u = 0.34..0.62!
FRONT_BOTTOM = [
    (0.00, 0.996), (0.10, 0.994), (0.18, 0.968), (0.25, 0.926),
    (0.32, 0.878), (0.39, 0.862), (0.47, 0.868), (0.54, 0.838),
    (0.61, 0.760), (0.69, 0.712), (0.78, 0.648), (0.88, 0.572),
    (0.95, 0.505), (1.00, 0.386),
]
# S-curved pale celadon cephalic rim sweeping from brow-cheek out to wingtips
FRONT_RIM = [
    (0.29, 0.345), (0.36, 0.392), (0.44, 0.438), (0.56, 0.468),
    (0.72, 0.470), (0.84, 0.442), (0.93, 0.396), (1.00, 0.350),
]
UPPER_LIP = [(0.00, 0.392), (0.12, 0.408), (0.22, 0.448), (0.32, 0.502), (0.40, 0.548), (0.45, 0.575)]
# Diagonal muscular cheek-jowl ledge framing the lower-outer boundary of the oral cheek pouch (Reference 03!)
CHEEK_LEDGE = [(0.26, 0.875), (0.32, 0.805), (0.38, 0.720), (0.43, 0.635), (0.46, 0.585)]
GUM_BOTTOM = [(0.00, 0.500), (0.16, 0.505), (0.28, 0.515), (0.30, 0.515)]
LOWER_GUM = [(0.00, 0.900), (0.12, 0.880), (0.20, 0.820), (0.27, 0.720), (0.33, 0.625), (0.41, 0.595)]
LOWER_TIP = [(0.00, 0.800), (0.12, 0.790), (0.20, 0.720), (0.27, 0.620), (0.30, 0.580)]

# Side profile contours (t = (z - NOSE) / LENGTH in [0, 1])
SIDE_DOME_TOP = [
    (0.000, 0.355), (0.008, 0.255), (0.022, 0.185), (0.048, 0.112),
    (0.082, 0.052), (0.118, 0.012), (0.146, 0.000), (0.178, 0.032),
    (0.208, 0.115), (0.235, 0.215), (0.265, 0.228),
]
JAW_SIDE_BOTTOM = [
    (0.000, 0.996), (0.025, 0.985), (0.055, 0.952), (0.105, 0.922),
    (0.155, 0.855), (0.190, 0.772), (0.220, 0.708), (0.250, 0.684),
]


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
    return -865 + 365 * min(1.0, u / 0.30) ** 1.75


def lower_z(u):
    return -875 + 445 * min(1.0, u / 0.40) ** 1.62


def head_front_z(u):
    """Smooth, continuous plan-view leading edge from snout (-902) across cheeks to wingtips (+270)."""
    if u <= 0.32:
        # Smooth organic parabolic snout nose (NOSE = -902 at u = 0 -> -678 at u = 0.32)
        return -902.0 + 224.0 * (u / 0.32) ** 1.82
    return interp([
        (0.32, -678.0), (0.40, -595.0), (0.50, -455.0),
        (0.62, -280.0), (0.75, -90.0), (0.88, 95.0), (1.00, 272.0),
    ], u)


def head_back_z(u):
    """Continuous sealed posterior skull/mantle margin overlapping neck_1 (z >= 311) with zero holes."""
    return interp([
        (0.00, 432.0), (0.18, 436.0), (0.32, 444.0),
        (0.52, 446.0), (0.74, 428.0), (0.90, 396.0), (1.00, 358.0),
    ], u)


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
        positive = self.cube(bone, [x0, y, z], [w, h, d], material, key, False, meta)
        negative = self.cube(bone, [-x0 - w, y, z], [w, h, d], material, key, True, meta)
        return positive, negative

    def centered(self, bone, y, z, w, h, d, material, key=None, meta=None):
        w = max(2, q(w, 2))
        return self.cube(bone, [-w // 2, y, z], [w, h, d], material, key, False, meta)


def tiled_pair(m, bone, x, y, z0, w, h, z1, material, key, tile=80, variants=2, meta=None):
    z0, z1 = q(z0), q(z1)
    z = z0
    i = 0
    while z < z1:
        d = min(tile, z1 - z)
        m.pair(m_bone := bone, x, y, z, w, h, d, material,
               key=(key, i % variants), meta=meta)
        z += d
        i += 1


def build_model():
    m = Builder()
    m.bone("head", [0, 0, -250], anim={"axis": "y", "amp": 0.8, "speed": 0.45, "phase": 0})
    m.bone("jaw", [0, -20, 100], parent="head",
           anim={"axis": "x", "amp": 2, "speed": 0.55, "phase": 0.4})

    # =========================================================================
    # 1. UNIFIED SMOOTH CRANIAL DOME, BROW, OCCIPITAL COLLAR, CHEEKS & MANTLE
    # =========================================================================
    # Fine 12-unit lateral strips across snout, dome, occiput & cheeks (cx < 552),
    # 16-unit mid-wing strips (552..664), and 20-unit outer wing strips (664..928,
    # also satisfying render_el_gran_maja_shots.py's w >= 20 16x16 macro patch!).
    x_strips = []
    cx = 0
    while cx < HALF:
        step = 12 if cx < 552 else (16 if cx < 664 else 20)
        w = min(step, HALF - cx)
        x_strips.append((cx, w))
        cx += w

    for x, w in x_strips:
        u = u_of(x + w * 0.5)
        zf_f = head_front_z(u)
        zb_f = head_back_z(u)
        zf = q(zf_f)
        zb = q(zb_f)
        span = max(32.0, float(zb - zf))

        v_peak_front = interp(FRONT_TOP, u)
        v_rim = interp(FRONT_RIM, max(0.29, u))
        v_bot_front = interp(FRONT_BOTTOM, u)

        # Smooth transition weight between central oral visor (u <= 0.34) and outer wing rim (u >= 0.45)
        rim_blend = min(1.0, max(0.0, (u - 0.34) / 0.11))
        rim_blend = rim_blend * rim_blend * (3.0 - 2.0 * rim_blend)

        n_seg = max(2, math.ceil((zb - zf) / 60.0))
        dz_f = (zb - zf) / n_seg

        for j in range(n_seg):
            z0 = q(zf + j * dz_f)
            z1 = zb if j == n_seg - 1 else q(zf + (j + 1) * dz_f)
            if z1 <= z0:
                continue
            zm = 0.5 * (z0 + z1)
            s = (zm - zf) / span
            t_side = (zm - NOSE) / LENGTH

            # --- Smooth upper surface v_top(u, zm) ---
            dome_w = max(0.0, 1.0 - (u / 0.48) ** 1.7)
            v_side_dome = interp(SIDE_DOME_TOP, t_side)
            lead_frac = min(1.0, max(0.0, (zm - zf_f) / max(44.0, 0.28 * span)))
            lead_arch = 1.0 - (1.0 - lead_frac) ** 2.2

            v_lip_edge = interp(UPPER_LIP, min(0.45, u)) - 0.018
            v_rim_edge = v_rim - 0.018
            v_edge_start = v_lip_edge * (1.0 - rim_blend) + v_rim_edge * rim_blend

            v_crown_target = v_peak_front + dome_w * max(0.0, v_side_dome - v_peak_front * 0.30) * 0.86
            v_top = v_edge_start + (v_crown_target - v_edge_start) * lead_arch

            # Concave dorsal basin on outer cephalic wings + upturned posterior wingtip (Reference 03!)
            if u > 0.40:
                wing_u = min(1.0, (u - 0.40) / 0.38)
                wing_dish = 0.028 * wing_u * math.sin(math.pi * min(1.0, s * 1.15))
                wing_upturn = -0.068 * wing_u * max(0.0, (s - 0.48) / 0.52) ** 1.4
                v_top += wing_dish + wing_upturn

            # Smooth posterior occipital taper into neck_1 (z >= 311, v_neck_top = 0.225) for central skull
            if zm > 140.0 and u < 0.52:
                occ_u = 1.0 - min(1.0, max(0.0, (u - 0.30) / 0.22))
                occ_f = (min(1.0, (zm - 140.0) / max(1.0, zb_f - 140.0)) ** 1.35) * occ_u
                v_occ_target = 0.225 + 0.085 * (u / 0.52)
                v_top = v_top * (1.0 - occ_f) + v_occ_target * occ_f

            # --- Smooth lower surface of upper skull + wide oral cheek window + diagonal jowl cheek! ---
            v_side_bot = interp(JAW_SIDE_BOTTOM, min(0.250, t_side))
            cheek_prominence = max(0.0, 1.0 - abs((u - 0.46) / 0.18) ** 2) * max(0.0, 1.0 - s * 1.1)
            v_full_bot = min(v_bot_front, v_side_bot + 0.095 * cheek_prominence)

            if u > 0.46:
                wing_sweep_u = min(1.0, (u - 0.46) / 0.40)
                rear_lift = max(0.0, (s - 0.26) / 0.74) ** 1.25 * wing_sweep_u
                v_wing_tail = v_top + 0.068 + 0.04 * (1.0 - wing_sweep_u)
                v_full_bot = v_full_bot * (1.0 - rear_lift) + min(v_full_bot, v_wing_tail) * rear_lift

            if zm > 205.0 and u <= 0.48:
                occ_u = 1.0 - max(0.0, (u - 0.32) / 0.16)
                occ_b = (min(1.0, (zm - 205.0) / max(1.0, zb_f - 205.0)) ** 1.35) * occ_u
                v_full_bot = v_full_bot * (1.0 - occ_b) + 0.678 * occ_b

            # Check if this (u, zm) sits in front of the oral cavity / fleshy cheek pouch!
            z_pouch_wall = -175.0 if u < 0.29 else interp(
                [(0.29, -455.0), (0.35, -475.0), (0.41, -495.0), (0.45, -510.0)], min(0.45, u)
            )
            in_oral_window = (u < 0.445) and (zm < z_pouch_wall)

            if in_oral_window:
                # Upper carapace arch over the mouth & wide cheek pouch (smoothly follows UPPER_LIP!)
                v_bot = interp(UPPER_LIP, min(0.45, u))
                # AND for u in 0.28..0.445, add the lower diagonal muscular cheek-jowl ledge BELOW the pouch!
                if u >= 0.28:
                    v_ledge_top = interp(CHEEK_LEDGE, min(0.45, u))
                    if v_full_bot > v_ledge_top + 0.015:
                        yl_bot = q(vy(v_full_bot))
                        yl_top = q(vy(v_ledge_top))
                        if yl_top > yl_bot + 3:
                            m.pair("head", x, yl_bot, z0, w, yl_top - yl_bot, z1 - z0, "wing_under_top",
                                   key=("cheek_ledge", w, yl_top - yl_bot, z1 - z0, j % 2))
            else:
                v_bot = v_full_bot

            y_top = q(vy(v_top))
            y_bot = q(vy(v_bot))
            if y_top <= y_bot + 3:
                continue

            total_h = y_top - y_bot
            top_mat = "crown_top" if u < 0.34 else ("visor_top" if u < 0.44 else "wing_top")

            if u >= 0.36 and y_top > q(vy(v_rim)) + 6:
                y_split = max(y_bot + 6, y_top - min(140, max(18, y_top - q(vy(v_rim)))))
            elif total_h > 144:
                y_split = y_top - 136
            else:
                y_split = y_bot

            cap_h = y_top - y_split
            if cap_h > 0:
                m.pair("head", x, y_split, z0, w, cap_h, z1 - z0, top_mat,
                       key=(top_mat, w, cap_h, z1 - z0, j % 2))
            low_h = y_split - y_bot
            if low_h > 0:
                low_mat = "wing_under_top" if (zm > -260 or u >= 0.35) else "visor_face"
                if low_mat == "wing_under_top" and low_h > 320:
                    mid_y = y_bot + low_h // 2
                    m.pair("head", x, y_bot, z0, w, mid_y - y_bot, z1 - z0, low_mat,
                           key=(low_mat, w, mid_y - y_bot, z1 - z0, 0))
                    m.pair("head", x, mid_y, z0, w, y_split - mid_y, z1 - z0, low_mat,
                           key=(low_mat, w, y_split - mid_y, z1 - z0, 1))
                else:
                    m.pair("head", x, y_bot, z0, w, low_h, z1 - z0, low_mat,
                           key=(low_mat, w, low_h, z1 - z0, j % 2))

        # Sculpted S-curved pale celadon lateral cephalic rim (u >= 0.34)
        if u >= 0.34:
            y_rim_c = q(vy(v_rim))
            if y_rim_c > q(vy(v_bot_front)) + 6:
                rim_h = 16 if u < 0.65 else 14
                m.pair("head", x, y_rim_c - rim_h // 2, zf - 7, w, rim_h, 28, "wing_rim",
                       key=("rim", w, rim_h))

        # Smoothly integrated 3D rounded outer masseter cheek swell (u = 0.34..0.62)
        if 0.34 <= u <= 0.62:
            cheek_u_bell = math.sin(math.pi * (u - 0.34) / 0.28)
            v_ch_top = max(v_rim + 0.045, interp(CHEEK_LEDGE, min(0.46, u)) - 0.015)
            y_cheek_top = q(vy(v_ch_top))
            y_cheek_bot = q(vy(v_bot_front - 0.025))
            ch_h = max(14, y_cheek_top - y_cheek_bot)
            if ch_h >= 14 and cheek_u_bell > 0.12:
                z_ch = q(zf + 4 - 18 * cheek_u_bell)
                ch_d = q(52 + 46 * cheek_u_bell, 4)
                m.pair("head", x, y_cheek_bot, z_ch, w, ch_h, ch_d, "wing_under_top",
                       key=("cheek_swell", w, ch_h, ch_d))

    # =========================================================================
    # 2. ORAL CAVITY, WIDE FLESHY RICTUS CHEEKS (`mouth_cheek`), GUMS & JAW
    # =========================================================================
    # Throat wall (u <= 0.25) + Wide, unmistakable crimson-slate oral cheeks (u = 0.25..0.445)!
    for x in range(0, q(0.445 * HALF), 12):
        w = min(12, q(0.445 * HALF) - x)
        u = u_of(x + w * 0.5)
        if u <= 0.255:
            v0 = interp(UPPER_LIP, u) + 0.008
            v1 = min(interp(LOWER_GUM, u) - 0.015, 0.875)
            mat = "throat"
            zf_m = -205
            depth_m = 150
        else:
            # Wide, sculpted fleshy cheek pouch spanning between UPPER_LIP and CHEEK_LEDGE!
            v0 = interp(UPPER_LIP, min(0.45, u)) - 0.012
            v1 = interp(CHEEK_LEDGE, min(0.46, u)) + 0.025
            mat = "mouth_cheek"
            # Curved bowl recessing smoothly from the oral corners (-625 -> -515)
            zf_m = q(interp([(0.255, -625.0), (0.33, -575.0), (0.39, -535.0), (0.445, -505.0)], u))
            depth_m = 185
        if v1 > v0 + 0.02:
            m.pair("head", x, q(vy(v1)), zf_m, w, max(8, q(HEIGHT * (v1 - v0))), depth_m,
                   mat, key=(mat, w, q(HEIGHT * (v1 - v0), 4)))

    # Sagittal pharyngeal septum for dark interior mouth depth from side profile
    for z0 in range(-848, -185, 60):
        z1 = min(-185, z0 + 60)
        t = (z0 + z1 + 1696) / 2 / 663
        vtop = 0.505 + 0.06 * t
        vbottom = 0.885 - 0.22 * t
        m.centered("head", q(vy(vbottom)), z0, 4, q(HEIGHT * (vbottom - vtop)),
                   z1 - z0, "mouth_wall", key=("wall", z1 - z0))

    # Smooth 12-unit striated upper gum arch
    for x in range(0, q(0.305 * HALF), 12):
        w = min(12, q(0.305 * HALF) - x)
        u = u_of(x + w * 0.5)
        v0 = interp(UPPER_LIP, u)
        v1 = interp(GUM_BOTTOM, u)
        if v1 <= v0:
            continue
        y0, y1 = q(vy(v1)), q(vy(v0))
        zf = q(upper_z(u))
        tiled_pair(m, "head", x, y0, zf, w, max(6, y1 - y0), -230, "gum_upper",
                   ("uppergum", w, max(6, y1 - y0)), tile=84, variants=1)

    # Smoothly rounded 12-unit U-arched lower jaw (`jaw`) + tongue floor + lower gums
    jaw_outer = q(0.405 * HALF)
    for x in range(0, jaw_outer, 12):
        w = min(12, jaw_outer - x)
        u = u_of(x + w * 0.5)
        z0 = q(lower_z(u))
        zmax = q(tz(0.222))
        for j, z in enumerate(range(z0, zmax, 72)):
            d = min(72, zmax - z)
            t = (z + d * 0.5 - NOSE) / LENGTH
            # Rounded chin transverse cross-section (avoids flat boxy jaw underside)
            chin_round = 0.016 * (u / 0.405) ** 2
            v_bottom = min(interp(FRONT_BOTTOM, u) - chin_round, interp(JAW_SIDE_BOTTOM, t))
            # Smoothlyround the leading symphysis tip of the lower jaw
            if j == 0:
                v_bottom -= 0.012
            v_mouth = min(interp(LOWER_GUM, u), v_bottom - 0.048)
            if v_bottom <= v_mouth:
                continue
            yb, yt = q(vy(v_bottom)), q(vy(v_mouth))
            m.pair("jaw", x, yb, z, w, max(4, yt - yb), d, "chin_top",
                   key=("chin", w, max(4, yt - yb), d, j % 2))
            m.pair("jaw", x, yt - 2, z, w, 8, d, "tongue", key=("tongue", w, d, j % 2))

        vgum = interp(LOWER_GUM, u)
        yg = q(vy(vgum))
        if u < 0.375:
            m.pair("jaw", x, yg - 8, z0 - 6, w, 18, 56, "gum_lower", key=("lowergum", w))

    # =========================================================================
    # 3. RECESSED EYE CLUSTERS (4 glossy eyes per side, 8 total) & ORGANIC BROWS
    # =========================================================================
    # Compute the exact frontmost carapace surface Z at each eye's (xc, yc) so every eye sits
    # cleanly proud of its orbital socket from front/3-4 view yet behind the midline side profile!
    carapace_boxes = [c for c in m.by_name["head"]["cubes"] if not c["mirror"] and c["material"] in ("crown_top", "visor_top", "visor_face")]
    eyes = [(0.075, 0.206, 32), (0.122, 0.246, 32), (0.168, 0.207, 44), (0.208, 0.238, 20)]
    for u, v, diam in eyes:
        xc = q(u * HALF)
        yc = q(vy(v))
        surf_zs = [
            c["origin"][2]
            for c in carapace_boxes
            if c["origin"][0] <= xc + diam // 2 and xc - diam // 2 <= c["origin"][0] + c["size"][0]
            and c["origin"][1] <= yc + diam // 2 and yc - diam // 2 <= c["origin"][1] + c["size"][1]
        ]
        z_surf = min(surf_zs) if surf_zs else q(head_front_z(u) + 95)
        zf = max(q(head_front_z(0.0)) + 28, z_surf - 14)
        m.pair("head", xc - (diam + 4) // 2, yc - (diam + 4) // 2, zf + 6,
               diam + 4, diam + 4, 68, "eye_socket", key=("socket", diam))
        m.pair("head", xc - diam // 2, yc - diam // 2, zf,
               diam, diam, 24, "eye", key=("eye", diam))
        # Individual supraorbital carapace brow crease directly overhanging each eye!
        m.pair("head", max(4, xc - (diam + 8) // 2), yc + diam // 2 - 2, zf - 6,
               diam + 8, 10, 42, "brow", key=("brow", diam + 8))

    # =========================================================================
    # 4. CURVED 4-SECTION TAPERING NEEDLE TEETH (60 teeth, >=12 root width)
    # =========================================================================
    def add_tooth(bone, xc, zf, root_y, tip_y, down, rear, i):
        length = max(18, q(abs(root_y - tip_y), 4))
        root_w = (12 if rear else 16) + 2 * ((i * 3 + rear) % 3)
        root_d = (11 if rear else 14) + 2 * ((i + rear) % 2)
        lean = (-1 if i % 3 == 0 else 1) * (3 + (i * 5) % 8)
        sweep = ((i * 7 + rear * 3) % 9) - 4
        fractions = (0.0, 0.28, 0.56, 0.80, 1.0)
        widths = (root_w, max(9, root_w - 4), max(6, root_w - 9), 3 + i % 3)
        depths = (root_d, max(8, root_d - 3), max(5, root_d - 7), 4)
        sign = -1 if down else 1
        for section in range(4):
            t0, t1 = fractions[section], fractions[section + 1]
            y0 = q(root_y + sign * length * t0)
            y1 = q(root_y + sign * length * t1)
            xoff = q(lean * (t0 ** 1.3))
            zoff = q(sweep * (t0 ** 1.2))
            w, d = widths[section], depths[section]
            mat = "tooth_root" if section == 0 else ("tooth_tip" if section == 3 else "tooth_mid")
            m.pair(bone, xc + xoff - w // 2, min(y0, y1), zf + zoff,
                   w, max(1, abs(y1 - y0) + 1), d, mat,
                   key=(mat, "down" if down else "up", rear, i % 3),
                   meta={"dir": "down" if down else "up"})

    tooth_end = q(0.282 * HALF)
    for i, xc in enumerate(range(10, tooth_end, 20)):
        u = u_of(xc)
        gum_v = interp(GUM_BOTTOM, u)
        jitter = (((i * i * 3 + i * 7 + 3) % 13) - 6) * 0.0048
        zf = q(upper_z(u) + 5 + ((i * 5) % 7) - 3)
        add_tooth("head", xc, zf, vy(gum_v) + 4, vy(0.648 + jitter), True, 0, i)
        if i in (2, 6, 10):
            add_tooth("head", xc + 5, zf + 42, vy(gum_v) + 1,
                      vy(0.620 + jitter * 0.8), True, 1, i)

    for i, xc in enumerate(range(11, q(0.295 * HALF), 23)):
        u = u_of(xc)
        vbase = interp(LOWER_GUM, u)
        vtip = interp(LOWER_TIP, u) + (((i * i * 5 + i * 3 + 2) % 13) - 6) * 0.0045
        zf = q(lower_z(u) + 8 + ((i * 5) % 7) - 3)
        add_tooth("jaw", xc, zf, vy(vbase) + 4, vy(vtip), False, 0, i)
        if i in (1, 6, 10):
            add_tooth("jaw", xc + 5, zf + 38, vy(vbase) + 2,
                      vy(vtip + 0.025), False, 1, i)

    # =========================================================================
    # 5. ROUNDED 5-TIER SERPENT BODY, DORSAL/VENTRAL SAILS & LATERAL KEELS
    # =========================================================================
    top_core = [(0.23, 0.225), (0.30, 0.242), (0.35, 0.222), (0.43, 0.190), (0.50, 0.190), (0.60, 0.205),
                (0.70, 0.225), (0.80, 0.250), (0.90, 0.290), (0.97, 0.350), (1.00, 0.430)]
    bottom_core = [(0.23, 0.690), (0.30, 0.665), (0.40, 0.660), (0.50, 0.640), (0.60, 0.625), (0.70, 0.610),
                   (0.80, 0.590), (0.90, 0.560), (0.97, 0.520), (1.00, 0.470)]
    sail_top = [(0.23, 0.225), (0.32, 0.242), (0.35, 0.200), (0.40, 0.125), (0.43, 0.083), (0.50, 0.090),
                (0.60, 0.150), (0.70, 0.170), (0.80, 0.220), (0.90, 0.280), (0.97, 0.350), (1.00, 0.430)]
    vent_bottom = [(0.23, 0.690), (0.40, 0.660), (0.44, 0.810), (0.48, 0.760), (0.55, 0.710), (0.65, 0.690),
                   (0.75, 0.650), (0.85, 0.610), (0.95, 0.550), (1.00, 0.470)]
    core_width = [(0.23, 352), (0.30, 304), (0.38, 290), (0.48, 272), (0.58, 236), (0.68, 196),
                  (0.78, 150), (0.88, 100), (0.95, 52), (1.00, 8)]
    names = ["neck_1", "neck_2", "trunk_1", "trunk_2", "trunk_3",
             "tail_1", "tail_2", "tail_3", "tail_4", "tail_5", "tail_6"]
    knots = np.linspace(0.235, 1.0, len(names) + 1)
    parent = "head"
    for bi, name in enumerate(names):
        z0, z1 = q(tz(knots[bi])), q(tz(knots[bi + 1]))
        m.bone(name, [0, 0, z0], parent=parent,
               anim={"axis": "y", "amp": 1.0 + 0.6 * bi, "speed": 0.65, "phase": -0.48 * bi})
        parent = name
        z = z0
        while z < z1:
            d = min(96, z1 - z)
            t = (z + d * 0.5 - NOSE) / LENGTH
            yt = q(vy(interp(top_core, t)), 8)
            yb = q(vy(interp(bottom_core, t)), 8)
            wm = max(8, q(interp(core_width, t), 16))
            height = max(24, yt - yb)
            # 5-tier rounded octagonal cross-section instead of a boxy 3-slab cross
            h1 = max(4, q(height * 0.15, 4))
            h2 = max(4, q(height * 0.18, 4))
            h5 = max(4, q(height * 0.15, 4))
            h4 = max(4, q(height * 0.18, 4))
            h3 = max(4, height - h1 - h2 - h4 - h5)
            w_cap = max(8, q(wm * 0.58, 8))
            w_shd = max(8, q(wm * 0.84, 8))
            mat = "neck" if bi < 2 else "body"
            variant = bi // 4
            m.centered(name, yb + h1 + h2 + h3 + h4, z, w_cap, h5, d, mat, key=(mat, "cap", variant))
            m.centered(name, yb + h1 + h2 + h3, z, w_shd, h4, d, mat, key=(mat, "shd_t", variant))
            m.centered(name, yb + h1 + h2, z, wm, h3, d, mat, key=(mat, "mid", variant))
            m.centered(name, yb + h1, z, w_shd, h2, d, "belly", key=("belly_s", variant))
            m.centered(name, yb, z, w_cap, h1, d, "belly", key=("belly_b", variant))

            ysail = q(vy(interp(sail_top, t)), 8)
            if ysail > yt + 8:
                sw = max(8, q(min(52, wm * 0.28), 8))
                m.centered(name, yt, z, sw, ysail - yt, d, "fin", key=("dorsal", variant))
            yfin = q(vy(interp(vent_bottom, t)), 8)
            if yfin < yb - 8:
                fw = max(8, q(min(38, wm * 0.22), 8))
                m.centered(name, yfin, z, fw, yb - yfin, d, "fin", key=("ventral", variant))
            if wm >= 64 and 0.29 < t < 0.83:
                keel_w = max(8, q(min(48, wm * 0.16), 8))
                keel_h = max(8, q(h3 * 0.28, 8))
                m.pair(name, wm // 2, yb + h1 + h2 + q(h3 * 0.45), z, keel_w, keel_h, d, "keel",
                       key=("keel", variant))
            z += d
    return m


def box_footprint(w, h, d):
    return 2 * d + 2 * w, d + h


def face_rects(u, v, w, h, d):
    return {"up": (u + d, v, w, d), "down": (u + d + w, v, w, d),
            "east": (u, v + d, d, h), "north": (u + d, v + d, w, h),
            "west": (u + d + w, v + d, d, h), "south": (u + 2 * d + w, v + d, w, h)}


ATLAS_W = ATLAS_H = 4096
HEAD_SHEET = (0, 0, 1408, 1024)
MOUTH_SHEET = (1408, 0, 1408, 1024)
SMALL_SHEET = (2816, 0, 1280, 1024)
TOP_SHEETS = [(0, 1024, 1408, 1024), (1408, 1024, 1408, 1024)]
TONGUE_SHEET = (2816, 1024, 1280, 1024)
DARK_SHEET = (0, 3584, 1408, 512)
BODY_SHEETS = [(0, 2048, 2048, 768), (2048, 2048, 2048, 768),
               (0, 2816, 2048, 768), (2048, 2816, 2048, 768)]
PAD = 160
BODY_PAD = 128
BODY_START = q(tz(0.235))
BODY_STRIDE = 1000
TOP_START = -910
TOP_STRIDE = 700
SKIN_MATS = {"crown_face", "visor_face", "wing_face", "wing_under_face", "wing_rim", "chin_face", "brow"}
TOP_MATS = {"crown_top", "visor_top", "wing_top"}
DARK_MATS = {"wing_under_top", "chin_top"}
FLESH_MATS = {"throat", "mouth_cheek", "mouth_wall", "gum_upper", "gum_lower"}
BODY_MATS = {"body", "neck", "belly", "fin", "keel"}
SMALL_MATS = {"eye", "eye_socket", "tooth_root", "tooth_mid", "tooth_tip"}


def hash2(x, y, seed=0):
    a = x.astype(np.int64) * 374761393 + y.astype(np.int64) * 668265263 + seed * 1442695041
    a = (a ^ (a >> 13)) * 1274126177
    a = (a ^ (a >> 16)) & 0x7fffffff
    return a.astype(np.float32) / 2147483647.0


def small_templates(cubes):
    reps = {}
    for cb in cubes:
        if cb["material"] in SMALL_MATS:
            reps.setdefault(cb["key"], cb)
    items = []
    for cb in reps.values():
        w, h, d = cb["size"]
        fw, fh = box_footprint(w, h, d)
        items.append((cb, fw + 2, fh + 2))
    items.sort(key=lambda t: (-t[2], -t[1]))
    sx, sy, sw, sh = SMALL_SHEET
    shelves = []
    placed = []
    for cb, w, h in items:
        found = False
        for shelf in shelves:
            if h <= shelf[2] and shelf[1] + w <= sw:
                x, y = shelf[1], shelf[0]
                shelf[1] += w
                found = True
                break
        if not found:
            y = sum(s[2] for s in shelves)
            if y + h > sh or w > sw:
                raise RuntimeError("Small-part atlas region is full")
            x = 0
            shelves.append([y, w, h])
        placed.append((cb, sx + x + 1, sy + y + 1))
    return placed


def uv_for(cb, template_uv):
    mat = cb["material"]
    ox, oy, oz = cb["origin"]
    w, h, d = cb["size"]
    if mat in SMALL_MATS:
        return template_uv[cb["key"]], SMALL_SHEET
    if mat in SKIN_MATS or mat in FLESH_MATS:
        sheet = HEAD_SHEET if mat in SKIN_MATS else MOUTH_SHEET
        sx, sy, _, _ = sheet
        x0 = -ox - w if cb["mirror"] else ox
        return (sx + PAD + x0 - d, sy + PAD + YTOP - (oy + h) - d), sheet
    if mat in TOP_MATS:
        index = max(0, min(1, (oz - TOP_START) // TOP_STRIDE))
        sheet = TOP_SHEETS[index]
        sx, sy, _, _ = sheet
        x0 = -ox - w if cb["mirror"] else ox
        z_end = TOP_START + TOP_STRIDE * index + 840
        return (sx + PAD + x0 - d, sy + z_end - (oz + d)), sheet
    if mat in DARK_MATS:
        sx, sy, _, sh = DARK_SHEET
        x0 = -ox - w if cb["mirror"] else ox
        v0 = (oz + 900) % max(1, sh - d - h + 1)
        return (sx + PAD + x0 - d, sy + v0), DARK_SHEET
    if mat == "tongue":
        sx, sy, _, _ = TONGUE_SHEET
        x0 = -ox - w if cb["mirror"] else ox
        local_z = (oz + 900) % 800
        return (sx + PAD + x0 - d, sy + 900 - local_z - d), TONGUE_SHEET
    if mat in BODY_MATS:
        index = max(0, min(len(BODY_SHEETS) - 1, (oz - BODY_START) // BODY_STRIDE))
        sheet = BODY_SHEETS[index]
        sx, sy, _, _ = sheet
        zstart = BODY_START + BODY_STRIDE * index
        return (sx + BODY_PAD + zstart + 1100 - (oz + d), sy + BODY_PAD + YTOP - (oy + h) - d), sheet
    raise ValueError(f"No texture region for {mat}")


def all_uvs(cubes, placed):
    template_uv = {cb["key"]: (u, v) for cb, u, v in placed}
    result = {}
    for cb in cubes:
        (u, v), sheet = uv_for(cb, template_uv)
        sx, sy, sw, sh = sheet
        fw, fh = box_footprint(*cb["size"])
        if not (sx <= u and sy <= v and u + fw <= sx + sw and v + fh <= sy + sh):
            raise ValueError(f"UV net outside material sheet: {cb['material']} {cb['origin']} {cb['size']} uv={(u, v)} sheet={sheet}")
        result[id(cb)] = (u, v)
    return result


def clean_front(ref):
    im = ref.copy()
    for left, right in [(287, 435)]:
        for y in range(83, 116):
            left_col = im[y, max(4, left - 3)].astype(np.float32)
            right_col = im[y, min(717, right + 3)].astype(np.float32)
            mix = np.linspace(0, 1, right - left, endpoint=False, dtype=np.float32)[:, None]
            im[y, left:right] = np.clip(left_col[None, :] * (1 - mix) + right_col[None, :] * mix, 0, 255).astype(np.uint8)
    return im


def make_head_region(ref, flesh=False):
    sx, sy, sw, sh = MOUTH_SHEET if flesh else HEAD_SHEET
    xx = np.arange(sw, dtype=np.float32)[None, :] - PAD
    yy = np.arange(sh, dtype=np.float32)[:, None] - PAD
    px = np.clip(np.rint(361 + xx / HALF * 357), 4, 717).astype(np.int32)
    py = np.clip(np.rint(40 + yy / HEIGHT * 277), 0, ref.shape[0] - 1).astype(np.int32)
    clean = clean_front(ref)
    sample = clean[py, px].astype(np.float32)
    bg = ref[py, 748].astype(np.float32)
    inside = np.linalg.norm(sample - bg, axis=2) > 15
    u = np.abs(xx / HALF)
    v = yy / HEIGHT
    ref_top = np.interp(u, [a for a, _ in FRONT_TOP], [b for _, b in FRONT_TOP])
    ref_bottom = np.interp(u, [a for a, _ in FRONT_BOTTOM], [b for _, b in FRONT_BOTTOM])
    mouth_top = np.interp(u, [a for a, _ in UPPER_LIP], [b for _, b in UPPER_LIP])
    mouth_bottom = np.interp(u, [a for a, _ in LOWER_GUM], [b for _, b in LOWER_GUM])
    silhouette = (u <= 1.0) & (v >= ref_top - 0.005) & (v <= ref_bottom + 0.005)
    cellx = np.floor(xx / 26).astype(np.int32)
    celly = np.floor(yy / 22).astype(np.int32)
    pat = hash2(np.broadcast_to(cellx, (sh, sw)), np.broadcast_to(celly, (sh, sw)), 13)
    large = hash2(np.broadcast_to(np.floor(xx / 83).astype(np.int32), (sh, sw)),
                  np.broadcast_to(np.floor(yy / 70).astype(np.int32), (sh, sw)), 29)
    if not flesh:
        base = np.empty((sh, sw, 3), dtype=np.float32)
        base[:] = [55, 75, 71]
        shadow = np.clip((v - 0.36) * 3.1, 0, 0.62)
        base = base * (1 - shadow[..., None])
        mouth_area = (u < 0.42) & (v > mouth_top - 0.006) & (v < mouth_bottom + 0.025)
        warm = (sample[..., 0] > sample[..., 1] * 1.07) & (v > 0.37) & (u < 0.44)
        valid = inside & silhouette & (~mouth_area) & (~warm)
        col = np.where(valid[..., None], sample, base)
        col = col * (0.96 + 0.08 * large[..., None])

        # Reference-03 carapace scute seams on brow, snout & outer cheek bosses
        row = np.floor(yy / 96).astype(np.int32)
        stagger = (row % 2) * 42
        edge_x = np.mod(xx + stagger + np.floor(yy / 28) * 3, 132)
        edge_y = np.mod(yy + np.floor(xx / 66) * 4, 96)
        seams = ((edge_x < 2.5) | (edge_y < 2.5)) & (v < 0.84) & (u < 0.64)
        col = np.where(seams[..., None], col * 0.68, col)

        # Sculpted volumetric shading & subtle celadon-slate sheen on the enlarged outer cheek lobes!
        cheek_zone = (u >= 0.31) & (u <= 0.62) & (v >= 0.44) & (v <= 0.88)
        cheek_crown = np.exp(-(((u - 0.45) / 0.11) ** 2 + ((v - 0.62) / 0.14) ** 2))
        col = np.where(cheek_zone[..., None],
                       col * (0.92 + 0.24 * cheek_crown[..., None]) + np.array([76, 98, 92], np.float32) * (0.20 * cheek_crown[..., None]),
                       col)

        fleck = (pat > 0.88) & (large > 0.42) & (v > 0.12) & (v < 0.65)
        col = np.where(fleck[..., None], col * 0.70 + np.array([92, 114, 106], np.float32) * 0.30, col)

        # Crisp pale-celadon highlight along the S-curved cephalic rim (Reference 01 & 03)
        rim_v = np.interp(np.clip(np.broadcast_to(u, (sh, sw)), 0.29, 1.0),
                          [a for a, _ in FRONT_RIM], [b for _, b in FRONT_RIM])
        rim_band = (u > 0.29) & (u < 1.02) & (np.abs(v - rim_v) < 0.016)
        col = np.where(rim_band[..., None], col * 0.62 + np.array([108, 130, 120], np.float32) * 0.38, col)
    else:
        base = np.empty((sh, sw, 3), dtype=np.float32)
        base[:] = [52, 30, 34]
        central = (u < 0.28) & (v > 0.51) & (v < 0.86)
        base = np.where(central[..., None], np.array([24, 15, 18], np.float32), base)
        gum = (u < 0.31) & (v >= 0.385) & (v < 0.525)
        base = np.where(gum[..., None], np.array([78, 51, 53], np.float32), base)
        lower = (u < 0.39) & (v > 0.78)
        base = np.where(lower[..., None], np.array([64, 44, 46], np.float32), base)
        # Prominent fleshy crimson-mauve oral-corner cheek highlight (`mouth_cheek` u = 0.255..0.45)
        rictus = (u >= 0.255) & (u <= 0.45) & (v >= 0.43) & (v <= 0.82)
        rictus_ridge = np.exp(-(((u - 0.345) / 0.075) ** 2 + ((v - 0.63) / 0.14) ** 2))
        POUCH = np.array([116, 58, 64], np.float32)
        base = np.where(rictus[..., None],
                        base * 0.70 + POUCH * (0.30 + 0.18 * rictus_ridge[..., None]),
                        base)

        toothlike = (sample[..., 0] > 58) & (sample[..., 0] - sample[..., 1] < 18) & (sample[..., 1] > sample[..., 2] * 1.04) & (v > 0.495) & (u < 0.33)
        valid = inside & silhouette & (v > 0.37) & (v < 0.98) & (u < 0.45) & (~toothlike)
        col = np.where(valid[..., None], sample * 0.72 + base * 0.28, base)
        # The reference photo is deep in shadow here, so lift the pouch back to an
        # unmistakable crimson-mauve while keeping its photographed mottling.
        lift = (0.42 + 0.20 * rictus_ridge)[..., None]
        col = np.where(rictus[..., None], col * (1.0 - lift) + POUCH * lift, col)
        vertical = (np.mod(xx + np.floor(yy / 90) * 3, 23) < 2) & (gum | lower | rictus)
        broad = (np.mod(xx + np.floor(yy / 65) * 4, 68) < 6) & (gum | lower | rictus)
        col = np.where(vertical[..., None], col * 0.78, col)
        col = np.where(broad[..., None], col * 1.12, col)
        col = col * (0.94 + 0.12 * large[..., None])

    grain = (pat - 0.5) * 3.0
    col = np.clip(col + grain[..., None], 0, 255).astype(np.uint8)
    return col


def make_top_region(index):
    """Paint world-continuous weathered carapace scutes & sea-lichen plates matching Reference 03."""
    _, _, sw, sh = TOP_SHEETS[index]
    xx = np.arange(sw, dtype=np.float32)[None, :] - PAD
    zz = TOP_START + TOP_STRIDE * index + 840 - np.arange(sh, dtype=np.float32)[:, None]
    xfull = np.broadcast_to(xx, (sh, sw))
    zfull = np.broadcast_to(zz, (sh, sw))
    row = np.floor((zfull + 1050) / 136).astype(np.int32)
    stagger = (row % 2) * 56
    bend = np.floor((zfull + 1000) / 49) * 3 + np.floor(xfull / 148) * 2
    col_idx = np.floor((xfull + stagger + bend) / 152).astype(np.int32)
    n = hash2(col_idx, row, 41)
    pal0 = np.array([50, 70, 66], np.float32)
    pal1 = np.array([72, 92, 85], np.float32)
    out = pal0[None, None, :] * (1 - n[..., None] * 0.74) + pal1[None, None, :] * (n[..., None] * 0.74)

    xp = np.mod(xfull + stagger + bend, 152)
    zp = np.mod(zfull + 1050 + np.floor(xfull / 152) * 13, 136)
    seam = (xp < 3.5) | (zp < 3.5)
    bevel = ((xp >= 4) & (xp < 8)) | ((zp >= 4) & (zp < 8))
    out = np.where(seam[..., None], out * 0.64, out)
    out = np.where(bevel[..., None], out * 1.12, out)

    # Subtle cranial midline ridge & celadon wing-margin tint
    u_world = np.abs(xfull) / HALF
    rim_glow = np.exp(-((u_world - 0.88) / 0.12) ** 2) * 0.22
    out = out * (1.0 - rim_glow[..., None]) + np.array([98, 120, 110], np.float32) * rim_glow[..., None]

    gridx = np.floor(xfull / 35).astype(np.int32)
    gridz = np.floor(zfull / 31).astype(np.int32)
    mott = hash2(gridx, gridz, 76)
    localx = np.mod(xfull, 35)
    localz = np.mod(zfull, 31)
    blot = (mott > 0.71) & (localx > 4) & (localx < 28) & (localz > 3) & (localz < 22)
    shadow_blot = (mott < 0.15) & (localx > 8) & (localx < 30) & (localz > 7) & (localz < 27)
    out = np.where(blot[..., None], out * 0.63 + np.array([98, 118, 107], np.float32) * 0.37, out)
    out = np.where(shadow_blot[..., None], out * 0.68 + np.array([30, 49, 43], np.float32) * 0.32, out)

    chips = hash2(np.floor((xfull + zfull * 0.12) / 9).astype(np.int32),
                  np.floor(zfull / 11).astype(np.int32), 64)
    out = out * (0.93 + 0.15 * chips[..., None])
    scuff = (np.mod(xfull * 2 + zfull + np.floor(zfull / 70) * 13, 157) < 2) & (chips > 0.6)
    out = np.where(scuff[..., None], out * 0.80 + np.array([106, 125, 113], np.float32) * 0.20, out)
    small = hash2(np.floor(xfull / 11).astype(np.int32), np.floor(zfull / 9).astype(np.int32), 91)
    flakes = (small > 0.94) & (np.mod(xfull, 11) < 6) & (np.mod(zfull, 9) < 4)
    out = np.where(flakes[..., None], out * 0.55 + np.array([123, 138, 125], np.float32) * 0.45, out)
    ax = np.mod(xfull, 80) - 40
    az = np.mod(zfull, 75) - 37
    colony = hash2(np.floor(xfull / 80).astype(np.int32), np.floor(zfull / 75).astype(np.int32), 118)
    r2 = ax * ax + az * az
    ring = (colony > 0.84) & (r2 > 16) & (r2 < 55)
    out = np.where(ring[..., None], out * 0.53 + np.array([113, 128, 115], np.float32) * 0.47, out)
    return np.clip(out, 0, 255).astype(np.uint8)


def make_dark_region():
    _, _, sw, sh = DARK_SHEET
    xx = np.broadcast_to(np.arange(sw, dtype=np.float32)[None, :] - PAD, (sh, sw))
    yy = np.broadcast_to(np.arange(sh, dtype=np.float32)[:, None], (sh, sw))
    cx = np.floor(xx / 42).astype(np.int32)
    cy = np.floor(yy / 36).astype(np.int32)
    n = hash2(cx, cy, 55)
    u_abs = np.abs(xx) / HALF
    # Slightly warmer/lighter slate across the outer cheek jowl band (u = 0.32..0.60) so cheeks read clearly!
    cheek_lift = np.exp(-((u_abs - 0.45) / 0.13) ** 2) * 0.35
    base = np.array([27, 34, 35], np.float32)[None, None, :] * (1.0 - cheek_lift[..., None]) + \
           np.array([46, 61, 59], np.float32)[None, None, :] * cheek_lift[..., None]
    out = base * (0.91 + 0.18 * n[..., None])
    seam = (np.mod(xx + np.floor(yy / 72) * 13, 136) < 2) | (np.mod(yy, 112) < 2)
    out = np.where(seam[..., None], out * 0.80, out)
    return np.clip(out, 0, 255).astype(np.uint8)


def make_tongue_region():
    _, _, sw, sh = TONGUE_SHEET
    xx = np.arange(sw, dtype=np.float32)[None, :] - PAD
    zz = 900 - np.arange(sh, dtype=np.float32)[:, None]
    xfull = np.broadcast_to(xx, (sh, sw))
    zfull = np.broadcast_to(zz, (sh, sw))
    cellx = np.floor(xfull / 29).astype(np.int32)
    cellz = np.floor(zfull / 43).astype(np.int32)
    n = hash2(cellx, cellz, 39)
    a = np.array([51, 31, 34], np.float32)
    b = np.array([79, 49, 49], np.float32)
    out = a[None, None, :] * (1 - n[..., None] * 0.32) + b[None, None, :] * (n[..., None] * 0.32)
    folds = np.mod(zfull + np.floor(xfull / 75) * 3, 90)
    out = np.where((folds < 3)[..., None], out * 0.78, out)
    out = np.where(((folds >= 5) & (folds < 8))[..., None], out * 1.12, out)
    lines = np.mod(xfull + np.floor(zfull / 60) * 2, 27) < 2
    out = np.where(lines[..., None], out * 0.90, out)
    return np.clip(out, 0, 255).astype(np.uint8)


def make_body_region(ref, index):
    _, _, sw, sh = BODY_SHEETS[index]
    zz = BODY_START + BODY_STRIDE * index + 1100 - (np.arange(sw, dtype=np.float32)[None, :] - BODY_PAD)
    yy = np.arange(sh, dtype=np.float32)[:, None] - BODY_PAD
    px = np.clip(np.rint(778 + (zz - NOSE) / LENGTH * 1978), 778, 2756).astype(np.int32)
    py = np.clip(np.rint(40 + yy / HEIGHT * 277), 0, ref.shape[0] - 1).astype(np.int32)
    sample = ref[py, px].astype(np.float32)
    bg = ref[py, 748].astype(np.float32)
    side_t = (zz - NOSE) / LENGTH
    side_top = np.interp(side_t, [0.235, 0.30, 0.35, 0.40, 0.43, 0.50, 0.60, 0.70, 0.80, 0.90, 0.97, 1],
                         [0.22, 0.242, 0.20, 0.125, 0.083, 0.09, 0.15, 0.17, 0.22, 0.28, 0.35, 0.43])
    side_bottom = np.interp(side_t, [0.235, 0.40, 0.44, 0.48, 0.55, 0.65, 0.75, 0.85, 0.95, 1],
                            [0.69, 0.66, 0.81, 0.76, 0.71, 0.69, 0.65, 0.61, 0.55, 0.47])
    inside = (np.linalg.norm(sample - bg, axis=2) > 17) & (yy / HEIGHT >= side_top - 0.01) & (yy / HEIGHT <= side_bottom + 0.01)
    fallback = np.array([26, 29, 34], np.float32)
    col = np.where(inside[..., None], sample * 1.05 + fallback * 0.03, fallback)
    zx = np.broadcast_to(np.floor(zz / 24).astype(np.int32), (sh, sw))
    jy = np.broadcast_to(np.floor(yy / 28).astype(np.int32), (sh, sw))
    n = hash2(zx, jy, 72)
    phase = np.mod(zz + yy * 0.33, 112)
    seam = (phase < 3) & inside
    highlight = (phase >= 5) & (phase < 9) & inside
    col = np.where(seam[..., None], col * 0.75, col)
    col = np.where(highlight[..., None], col * 1.10, col)
    ray = np.mod(zz + yy * 0.70, 37)
    sail = (yy < HEIGHT * 0.23) | (yy > HEIGHT * 0.65)
    col = np.where(((ray < 2) & sail & inside)[..., None], col * 1.25, col)
    col = col * (0.96 + 0.09 * n[..., None])
    return np.clip(col, 0, 255).astype(np.uint8)


def paint_small(atlas, placed):
    for cb, u0, v0 in placed:
        w, h, d = cb["size"]
        mat = cb["material"]
        meta = cb["meta"]
        for name, (x, y, fw, fh) in face_rects(u0, v0, w, h, d).items():
            xx = np.arange(fw, dtype=np.float32)[None, :]
            yy = np.arange(fh, dtype=np.float32)[:, None]
            if mat == "eye_socket":
                col = np.empty((fh, fw, 3), np.float32)
                col[:] = [20, 27, 27]
                col *= 0.90 + 0.17 * hash2(np.broadcast_to(xx.astype(np.int32), (fh, fw)),
                                           np.broadcast_to(yy.astype(np.int32), (fh, fw)), 9)[..., None]
            elif mat == "eye":
                col = np.empty((fh, fw, 3), np.float32)
                col[:] = [22, 23, 26]
                r = np.sqrt(((xx - (fw - 1) / 2) / max(1, fw * 0.53)) ** 2 + ((yy - (fh - 1) / 2) / max(1, fh * 0.53)) ** 2)
                col = np.where((r > 0.80)[..., None], np.array([9, 12, 14], np.float32), col)
                if name == "north":
                    gl = (xx > fw * 0.25) & (xx < fw * 0.44) & (yy > fh * 0.22) & (yy < fh * 0.38)
                    col = np.where(gl[..., None], np.array([82, 88, 88], np.float32), col)
            else:
                direction = meta.get("dir", "down")
                t = np.broadcast_to(yy / max(1, fh - 1), (fh, fw))
                if direction == "up":
                    t = 1 - t
                if name in ("up", "down"):
                    t = np.zeros((fh, fw), np.float32) + 0.5
                if mat == "tooth_root":
                    a, b_col = np.array([109, 99, 88], np.float32), np.array([161, 151, 135], np.float32)
                elif mat == "tooth_mid":
                    a, b_col = np.array([139, 130, 116], np.float32), np.array([187, 179, 162], np.float32)
                else:
                    a, b_col = np.array([151, 143, 128], np.float32), np.array([204, 198, 182], np.float32)
                col = a[None, None, :] * (1 - t[..., None]) + b_col[None, None, :] * t[..., None]
                if fw > 3:
                    col *= np.where((xx < 1) | (xx >= fw - 1), 0.82, 1.0)[..., None]
            atlas[y:y + fh, x:x + fw, :3] = np.clip(col, 0, 255).astype(np.uint8)
            atlas[y:y + fh, x:x + fw, 3] = 255


def paint_atlas(placed):
    ref = np.array(Image.open(REF).convert("RGB"))
    atlas = np.zeros((ATLAS_H, ATLAS_W, 4), np.uint8)
    for sheet, flesh in [(HEAD_SHEET, False), (MOUTH_SHEET, True)]:
        x, y, w, h = sheet
        print("Painting", "mouth" if flesh else "crown and wing", flush=True)
        atlas[y:y + h, x:x + w, :3] = make_head_region(ref, flesh)
        atlas[y:y + h, x:x + w, 3] = 255
    for i, (x, y, w, h) in enumerate(TOP_SHEETS):
        print("Painting dorsal plates", i + 1, "/", len(TOP_SHEETS), flush=True)
        atlas[y:y + h, x:x + w, :3] = make_top_region(i)
        atlas[y:y + h, x:x + w, 3] = 255
    x, y, w, h = TONGUE_SHEET
    atlas[y:y + h, x:x + w, :3] = make_tongue_region()
    atlas[y:y + h, x:x + w, 3] = 255
    x, y, w, h = DARK_SHEET
    atlas[y:y + h, x:x + w, :3] = make_dark_region()
    atlas[y:y + h, x:x + w, 3] = 255
    for i, (x, y, w, h) in enumerate(BODY_SHEETS):
        print("Painting body", i + 1, "/", len(BODY_SHEETS), flush=True)
        atlas[y:y + h, x:x + w, :3] = make_body_region(ref, i)
        atlas[y:y + h, x:x + w, 3] = 255
    paint_small(atlas, placed)
    return atlas


def export_all(plan=False):
    m = build_model()
    cubes = [c for b in m.bones for c in b["cubes"]]
    placed = small_templates(cubes)
    uvs = all_uvs(cubes, placed)
    lo = [min(c["origin"][i] for c in cubes) for i in range(3)]
    hi = [max(c["origin"][i] + c["size"][i] for c in cubes) for i in range(3)]
    spans = [hi[i] - lo[i] for i in range(3)]
    mats = Counter(c["material"] for c in cubes)
    stats = {
        "bones_count": len(m.bones), "cubes_count": len(cubes),
        "upper_teeth": sum(c["material"] == "tooth_root" for c in m.by_name["head"]["cubes"]),
        "lower_teeth": sum(c["material"] == "tooth_root" for c in m.by_name["jaw"]["cubes"]),
        "upper_tooth_cuboids": sum(c["material"].startswith("tooth") for c in m.by_name["head"]["cubes"]),
        "lower_tooth_cuboids": sum(c["material"].startswith("tooth") for c in m.by_name["jaw"]["cubes"]),
        "eyes": "2 clusters, 4 raised eyes per side",
        "atlas_size": [ATLAS_W, ATLAS_H], "render_scale": 1.0,
        "texels_per_block_per_axis": 16,
        "uv_layout": "Full-density box UV. Symmetric/repeated parts and reference-projection sheets intentionally reuse/overlap atlas pixels.",
        "small_templates": len(placed),
        "bounds_units": {"x": [lo[0], hi[0]], "y": [lo[1], hi[1]], "z": [lo[2], hi[2]]},
        "size_units": {"width_x": spans[0], "height_y": spans[1], "length_z": spans[2]},
        "size_blocks_meters": {"width_x": round(spans[0] / 16, 3), "height_y": round(spans[1] / 16, 3), "length_z": round(spans[2] / 16, 3)},
        "cuboids_by_material": dict(sorted(mats.items())),
        "front_contours_source": "03_ref_el_gran_maja_full_body_model.png, x=4..717 y=40..316",
        "side_contours_source": "03_ref_el_gran_maja_full_body_model.png, x=778..2756 y=40..316",
    }
    print(json.dumps(stats, ensure_ascii=False, indent=2), flush=True)
    if plan:
        return
    atlas = paint_atlas(placed)
    Image.fromarray(atlas).save(HERE / "el_gran_maja.png", compress_level=7)
    bb_bones = []
    rw_bones = []
    for b in m.bones:
        px, py, pz = b["pivot"]
        ordered = sorted(b["cubes"], key=lambda c: c["mirror"])
        bb_cubes = []
        rw_cubes = []
        for cb in ordered:
            ox, oy, oz = cb["origin"]
            w, h, d = cb["size"]
            u, v = uvs[id(cb)]
            a = {"origin": [ox, oy, oz], "size": [w, h, d], "uv": [u, v]}
            r = {"origin": [ox, -(oy + h), oz], "size": [w, h, d], "uv": [u, v]}
            if cb["mirror"]:
                a["mirror"] = True
                r["mirror"] = True
            bb_cubes.append(a)
            rw_cubes.append(r)
        a = {"name": b["name"], "pivot": [px, py, pz], "cubes": bb_cubes}
        r = {"name": b["name"], "pivot": [px, -py, pz], "cubes": rw_cubes}
        if b["parent"]:
            a["parent"] = r["parent"] = b["parent"]
        if b["anim"]:
            a["anim"] = dict(b["anim"])
            r["anim"] = dict(b["anim"])
            if r["anim"].get("axis") in ("x", "z"):
                r["anim"]["amp"] = -r["anim"]["amp"]
        bb_bones.append(a)
        rw_bones.append(r)
    rw = {"texture_width": ATLAS_W, "texture_height": ATLAS_H, "render_scale": 1.0,
          "translucent": False, "bones": rw_bones}
    bb = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.riftworld.el_gran_maja",
                        "texture_width": ATLAS_W, "texture_height": ATLAS_H,
                        "visible_bounds_width": round(max(spans[0], spans[2]) / 16, 2),
                        "visible_bounds_height": round(spans[1] / 16, 2),
                        "visible_bounds_offset": [0, round((lo[1] + hi[1]) / 32, 2), round((lo[2] + hi[2]) / 32, 2)]},
        "bones": bb_bones}]}
    (HERE / "el_gran_maja.riftworld_geo.json").write_text(json.dumps(rw, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    (HERE / "el_gran_maja.blockbench.geo.json").write_text(json.dumps(bb, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    (HERE / "model_stats.json").write_text(json.dumps(stats, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("Wrote texture, models and statistics to", HERE, flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--plan", action="store_true", help="validate UV nets and print statistics without painting")
    args = parser.parse_args()
    export_all(plan=args.plan)
