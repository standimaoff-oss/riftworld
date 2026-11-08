#!/usr/bin/env python3
"""
3D-Aware High-Density Pixel-Art Atlas Painter (V2) for Riftworld Blockbench MCP Models.

Paints crisp 32px/block Hytale-grade textures mapped onto Blockbench's exact Box-UV
layout using each texel's 3D world position `(wx, wy, wz)` and world normal `(wn)`.
"""

import json
import math
import sys
from pathlib import Path
import numpy as np
from PIL import Image
from painter_batch_02 import paint_fox_face, paint_leafwing_face, paint_frog_face
from painter_batch_03 import paint_snail_face, paint_wolf_face, paint_gecko_face


def hash_noise(ix, iy, seed=0):
    n = (ix.astype(np.int64) * 374761393 + iy.astype(np.int64) * 668265263 + int(seed) * 1442695041) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    n = (n ^ (n >> 16)) & 0xFFFFFFFF
    return n.astype(np.float32) / 4294967295.0


def value_noise_3d(x, y, z, scale=1.0, seed=1):
    sx = x * scale + seed * 17.3
    sy = y * scale + seed * 31.7
    sz = z * scale + seed * 47.1
    ix = np.floor(sx).astype(np.int64)
    iy = np.floor(sy).astype(np.int64)
    iz = np.floor(sz).astype(np.int64)
    fx = sx - ix
    fy = sy - iy
    fz = sz - iz
    ux = fx * fx * (3.0 - 2.0 * fx)
    uy = fy * fy * (3.0 - 2.0 * fy)
    uz = fz * fz * (3.0 - 2.0 * fz)

    def h3(a, b, c):
        q = (a * 73856093 ^ b * 19349663 ^ c * 83492791 ^ (seed * 2654435761)) & 0xFFFFFFFF
        q = ((q ^ (q >> 13)) * 1274126177) & 0xFFFFFFFF
        return ((q ^ (q >> 16)) & 0xFFFFFFFF).astype(np.float32) / 4294967295.0

    c000 = h3(ix, iy, iz)
    c100 = h3(ix + 1, iy, iz)
    c010 = h3(ix, iy + 1, iz)
    c110 = h3(ix + 1, iy + 1, iz)
    c001 = h3(ix, iy, iz + 1)
    c101 = h3(ix + 1, iy, iz + 1)
    c011 = h3(ix, iy + 1, iz + 1)
    c111 = h3(ix + 1, iy + 1, iz + 1)

    x00 = c000 + (c100 - c000) * ux
    x10 = c010 + (c110 - c010) * ux
    x01 = c001 + (c101 - c001) * ux
    x11 = c011 + (c111 - c011) * ux
    y0 = x00 + (x10 - x00) * uy
    y1 = x01 + (x11 - x01) * uy
    return y0 + (y1 - y0) * uz


def fbm3(x, y, z, base_scale=0.15, seed=1):
    v = 0.55 * value_noise_3d(x, y, z, base_scale, seed)
    v += 0.30 * value_noise_3d(x, y, z, base_scale * 2.1, seed + 11)
    v += 0.15 * value_noise_3d(x, y, z, base_scale * 4.3, seed + 23)
    return v


def rgb(hex_str):
    h = hex_str.lstrip('#')
    return np.array([int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)], dtype=np.float32)


def mix(c1, c2, t):
    t_clamped = np.clip(t, 0.0, 1.0)[..., None]
    return c1 * (1.0 - t_clamped) + c2 * t_clamped


def face_world_grid(cube, face_name):
    sx, sy, sz = cube['size']
    uo, vo = cube['uv_offset']
    fx0, fy0, fz0 = cube['from']
    fx1, fy1, fz1 = cube['to']

    if face_name == 'east':
        u0, v0, w, h = uo, vo + sz, sz, sy
    elif face_name == 'north':
        u0, v0, w, h = uo + sz, vo + sz, sx, sy
    elif face_name == 'west':
        u0, v0, w, h = uo + sz + sx, vo + sz, sz, sy
    elif face_name == 'south':
        u0, v0, w, h = uo + 2 * sz + sx, vo + sz, sx, sy
    elif face_name == 'up':
        u0, v0, w, h = uo + sz, vo, sx, sz
    elif face_name == 'down':
        u0, v0, w, h = uo + sz + sx, vo, sx, sz
    else:
        raise ValueError(face_name)

    du, dv = np.meshgrid(np.arange(w, dtype=np.float32), np.arange(h, dtype=np.float32))
    lu = (du + 0.5) / max(w, 1)
    lv = (dv + 0.5) / max(h, 1)

    if face_name == 'east':
        lx = np.full_like(lu, fx1)
        ly = fy1 - lv * sy
        lz = fz1 - lu * sz
        ln = np.array([1.0, 0.0, 0.0], dtype=np.float32)
    elif face_name == 'west':
        lx = np.full_like(lu, fx0)
        ly = fy1 - lv * sy
        lz = fz0 + lu * sz
        ln = np.array([-1.0, 0.0, 0.0], dtype=np.float32)
    elif face_name == 'north':
        lx = fx1 - lu * sx
        ly = fy1 - lv * sy
        lz = np.full_like(lu, fz0)
        ln = np.array([0.0, 0.0, -1.0], dtype=np.float32)
    elif face_name == 'south':
        lx = fx0 + lu * sx
        ly = fy1 - lv * sy
        lz = np.full_like(lu, fz1)
        ln = np.array([0.0, 0.0, 1.0], dtype=np.float32)
    elif face_name == 'up':
        lx = fx1 - lu * sx
        ly = np.full_like(lu, fy1)
        lz = fz1 - lv * sz
        ln = np.array([0.0, 1.0, 0.0], dtype=np.float32)
    else:
        lx = fx1 - lu * sx
        ly = np.full_like(lu, fy0)
        lz = fz0 + lv * sz
        ln = np.array([0.0, -1.0, 0.0], dtype=np.float32)

    m = np.array(cube['world_m'], dtype=np.float32).reshape(3, 3)
    t = np.array(cube['world_t'], dtype=np.float32)

    wx = m[0, 0] * lx + m[0, 1] * ly + m[0, 2] * lz + t[0]
    wy = m[1, 0] * lx + m[1, 1] * ly + m[1, 2] * lz + t[1]
    wz = m[2, 0] * lx + m[2, 1] * ly + m[2, 2] * lz + t[2]
    wn = m @ ln
    return int(u0), int(v0), int(w), int(h), wx, wy, wz, wn, lu, lv, du, dv


# ============================ 01: PORCELAIN CRANE ============================

def kintsugi_crack_line(wx, wy, wz, density=1.0, seed=19):
    """
    Delicate, museum-authentic ~1-texel Kintsugi gold inlay cracks on glazed porcelain.
    Spaced widely (~15-18 units apart) so pure white/celadon glaze dominates!
    """
    ax = np.abs(wx)
    warp = (fbm3(wx, wy, wz, 0.09, seed) - 0.5) * 0.55
    p1 = (wy * 0.052 + wz * 0.046 - ax * 0.031) * density + warp
    p2 = (wy * 0.041 - wz * 0.058 + ax * 0.038) * density - warp * 0.85
    # Gradient norm ~ 0.076 * density -> divide by grad norm for true world-unit distance!
    gn = 0.076 * density
    d1 = np.abs(p1 - np.rint(p1)) / gn
    d2 = np.abs(p2 - np.rint(p2)) / gn
    main_line = np.clip(1.0 - d1 / 0.62, 0.0, 1.0)
    branch_gate = np.clip(1.0 - d1 / 5.5, 0.0, 1.0)
    sub_line = np.clip(1.0 - d2 / 0.55, 0.0, 1.0) * branch_gate
    return np.maximum(main_line, sub_line)


def paint_crane_face(cube, face_name, w, h, wx, wy, wz, wn, lu, lv, du, dv):
    mat = cube['mat']
    grain = (hash_noise(du, dv, seed=hash(cube['name'] + face_name) & 0xFFFF) - 0.5) * 0.032
    edge_px = np.minimum(np.minimum(du, w - 1 - du), np.minimum(dv, h - 1 - dv))
    top_shade = 0.5 + 0.5 * float(wn[1])

    if mat == 'obsidian_eye':
        col = np.broadcast_to(rgb('#12151C'), (h, w, 3)).copy()
        glint = (du == 0) & (dv == 0)
        col[glint] = rgb('#FFFFFF')
        return col

    if mat.startswith('imperial_gold'):
        c_top = rgb('#F7DA7E') if 'bright' in mat else rgb('#E7BC4E')
        c_low = rgb('#A37520') if 'dark' in mat else rgb('#BD8B2A')
        base = mix(c_low, c_top, np.clip(0.35 + 0.45 * top_shade + (0.5 - lv) * 0.35 + grain, 0.0, 1.0))
        if w >= 3 and h >= 2:
            bevel_hi = (dv == 0)
            base = np.where(bevel_hi[..., None], mix(base, rgb('#FFF2BA'), np.full((h, w), 0.45)), base)
        return base

    if mat.startswith('crane_leg'):
        c_hi = rgb('#EFE8DC')
        c_lo = rgb('#D4C9B8')
        flute = 0.5 + 0.5 * np.sin(du * 1.8)
        base = mix(c_lo, c_hi, np.clip(0.38 + 0.38 * top_shade + 0.18 * flute + grain, 0.0, 1.0))
        if mat == 'crane_leg_joint':
            band = (dv == h // 2) | (dv == 0)
            base = np.where(band[..., None], mix(base, rgb('#BBAEA0'), np.full((h, w), 0.50)), base)
        return base

    if mat == 'porcelain_kintsugi':
        c_pure = rgb('#FBFCFD')
        c_cool = rgb('#E3E8F0')
        t_glaze = np.clip(0.48 + 0.42 * top_shade + (0.5 - lv) * 0.18 + grain, 0.0, 1.0)
        base = mix(c_cool, c_pure, t_glaze)

        # Crisp thin 1-texel Kintsugi gold seams
        vein = kintsugi_crack_line(wx, wy, wz, seed=19)
        gold_col = mix(rgb('#C2902E'), rgb('#F6D87A'), np.clip(0.5 + top_shade * 0.5 + grain * 4.0, 0.0, 1.0))
        base = mix(base, gold_col, vein * 0.92)
        return base

    if mat.startswith('celadon'):
        c_jade_deep = rgb('#6D9F90')
        c_jade_mid = rgb('#97C8B8')
        c_jade_pale = rgb('#C4E4DA')
        c_porcelain = rgb('#F6F9F8')

        tip_t = 1.0 - lv
        barb_wave = 0.5 + 0.5 * np.sin((du - w * 0.5) * 1.2 + dv * 0.7)
        t_cel = np.clip(0.32 + 0.36 * top_shade + 0.20 * tip_t + 0.12 * barb_wave + grain, 0.0, 1.0)
        base = mix(c_jade_deep, c_jade_mid, np.clip(t_cel * 1.35, 0.0, 1.0))
        base = mix(base, c_jade_pale, np.clip((t_cel - 0.55) * 1.7, 0.0, 1.0))

        if mat == 'celadon_kintsugi':
            # Scalloped upper wing coverts with delicate Kintsugi golden cracks!
            vein = kintsugi_crack_line(wx, wy, wz, density=1.45, seed=43)
            gold_col = mix(rgb('#BD8B2B'), rgb('#F5D778'), np.clip(0.45 + 0.5 * top_shade, 0.0, 1.0))
            base = mix(base, gold_col, vein * 0.90)
        else:
            # Pure luminous celadon flight feathers with delicate pale shaft & crisp jade edge
            if w >= 3 and face_name in ('east', 'west', 'north', 'south'):
                shaft = np.abs(du - (w - 1) * 0.5) < 0.52
                base = np.where(shaft[..., None], mix(base, c_jade_pale, np.full((h, w), 0.55)), base)
            is_edge = (edge_px == 0)
            base = np.where(is_edge[..., None], mix(base, c_jade_deep, np.full((h, w), 0.35)), base)
            tip_hi = (dv == 0)
            base = np.where(tip_hi[..., None], mix(base, c_jade_pale, np.full((h, w), 0.55)), base)
        return base

    if mat.startswith('tail_plume'):
        # lv=0 is feather tip (pEnd), lv=1 is feather root (pStart)
        dist_tip = 1.0 - lv
        c_white = rgb('#F8FAFA')
        c_mint = rgb('#A3D2C3')
        c_deep = rgb('#72A696')
        c_gold = rgb('#DCB046')
        c_gold_hi = rgb('#F6DA7E')

        if 'deep' in mat:
            # Lower drooping jade feathers with white-to-jade gradient & delicate gold rachis + V-border
            base = mix(c_white, c_mint, np.clip(dist_tip * 1.4, 0.0, 1.0))
            base = mix(base, c_deep, np.clip((dist_tip - 0.35) * 1.6, 0.0, 1.0))
        elif 'mint' in mat:
            base = mix(c_white, c_mint, np.clip((dist_tip - 0.20) * 1.35, 0.0, 1.0))
        else:
            # Upper porcelain-white lotus plumes with soft celadon blush near base/center
            base = mix(c_mint, c_white, np.clip(0.45 + dist_tip * 0.65, 0.0, 1.0))

        base = base * (0.91 + 0.13 * top_shade + grain)[..., None]

        # Crisp thin 1-px golden center rachis + thin gold rim at the very tip
        if face_name in ('east', 'west'):
            # Broad face of lateral/vertical plume has width `sz` (`du` across `w`)
            center_dist = np.abs(du - (w - 1) * 0.5)
            shaft = (center_dist < 0.52) & (w >= 4)
            tip_chevron = (dv <= 1) | ((edge_px == 0) & (dist_tip > 0.72))
            gold_m = shaft | tip_chevron
        elif face_name in ('north', 'south', 'up', 'down'):
            center_dist = np.abs(du - (w - 1) * 0.5)
            shaft = (center_dist < 0.52) & (w >= 4)
            tip_chevron = (dv == 0)
            gold_m = shaft | tip_chevron
        else:
            gold_m = np.zeros((h, w), dtype=bool)

        gold_grad = mix(c_gold, c_gold_hi, np.clip(dist_tip, 0.0, 1.0))
        base = np.where(gold_m[..., None], mix(base, gold_grad, np.full((h, w), 0.85)), base)
        return base

    return np.broadcast_to(rgb('#F5F7FA'), (h, w, 3)).copy()


# ============================ 02: BASALT BEETLE ============================

def paint_beetle_face(cube, face_name, w, h, wx, wy, wz, wn, lu, lv, du, dv):
    mat = cube['mat']
    meta = cube.get('meta') or {}
    grain = (hash_noise(du, dv, seed=hash(cube['name'] + face_name) & 0xFFFF) - 0.5)
    edge_px = np.minimum(np.minimum(du, w - 1 - du), np.minimum(dv, h - 1 - dv))
    top_shade = 0.5 + 0.5 * float(wn[1])

    if mat == 'magma_eye':
        rad = np.hypot((lu - 0.5) * 2.0, (lv - 0.5) * 2.0)
        col = mix(rgb('#FFF5A6'), rgb('#FF7700'), np.clip(rad * 0.85, 0.0, 1.0))
        col = mix(col, rgb('#B82600'), np.clip((rad - 0.72) * 2.2, 0.0, 1.0))
        return col

    if mat == 'molten_magma':
        flow = fbm3(wx * 1.1, wy * 1.1, wz * 1.1, 0.20, seed=77)
        hot_vein = 1.0 - np.abs(np.sin(wx * 0.30 + wz * 0.26 + flow * 4.4))
        t_heat = np.clip(0.38 * flow + 0.78 * (hot_vein ** 2) + grain * 0.07, 0.0, 1.0)
        col = mix(rgb('#BF2600'), rgb('#FF6200'), np.clip(t_heat * 1.6, 0.0, 1.0))
        col = mix(col, rgb('#FFBA1C'), np.clip((t_heat - 0.34) * 1.9, 0.0, 1.0))
        col = mix(col, rgb('#FFF7B2'), np.clip((t_heat - 0.70) * 3.2, 0.0, 1.0))
        return col

    if mat == 'molten_horn':
        seg_t0 = float(meta.get('hornT0', 0.0))
        seg_t1 = float(meta.get('hornT1', 1.0))
        axial_t = seg_t0 + (seg_t1 - seg_t0) * (1.0 - lv)
        if face_name == 'up' and seg_t1 >= 0.9:
            axial_t = np.ones_like(lu) * 0.98
        elif face_name == 'down' and seg_t0 <= 0.1:
            axial_t = np.zeros_like(lu)

        t_h = np.clip(axial_t + grain * 0.05, 0.0, 1.0)
        col = mix(rgb('#2E1813'), rgb('#A82808'), np.clip(t_h * 2.5, 0.0, 1.0))
        col = mix(col, rgb('#F76306'), np.clip((t_h - 0.24) * 2.2, 0.0, 1.0))
        col = mix(col, rgb('#FFB41C'), np.clip((t_h - 0.58) * 2.3, 0.0, 1.0))
        col = mix(col, rgb('#FFF18F'), np.clip((t_h - 0.84) * 4.5, 0.0, 1.0))
        if w >= 3 and h >= 3:
            is_edge = (edge_px == 0)
            col = np.where(is_edge[..., None], col * np.where(t_h[..., None] > 0.5, 1.10, 0.88), col)
        return col

    if mat == 'basalt_mandible':
        c_lo = rgb('#2E211B')
        c_hi = rgb('#5C4437')
        t = np.clip(0.35 + 0.45 * top_shade + (1.0 - lv) * 0.2 + grain * 0.14, 0.0, 1.0)
        col = mix(c_lo, c_hi, t)
        if w >= 3 and h >= 3:
            col = np.where((edge_px == 0)[..., None], col * 1.12, col)
        return col

    # Pitted dark volcanic basalt rock!
    rock_macro = fbm3(wx, wy, wz, 0.26, seed=105)
    speck = hash_noise(du, dv, seed=(hash(cube['name']) + ord(face_name[0])) & 0xFFFF)

    c_dark = rgb('#141316')
    c_mid = rgb('#232126')
    c_light = rgb('#38353D')
    c_warm_ash = rgb('#443936')

    t_rock = np.clip(0.24 + 0.40 * top_shade + 0.30 * rock_macro + (speck - 0.5) * 0.24, 0.0, 1.0)
    col = mix(c_dark, c_mid, np.clip(t_rock * 1.5, 0.0, 1.0))
    col = mix(col, c_light, np.clip((t_rock - 0.52) * 1.9, 0.0, 1.0))
    col = np.where((speck > 0.87)[..., None], mix(col, c_warm_ash, np.full((h, w), 0.50)), col)

    if mat == 'basalt_plate':
        # Determine whether this face points OUTWARD from beetle center `(0, 19, 3)` or INWARD/SIDEWAYS into a fissure!
        cx_mean = float(np.mean(wx))
        cy_mean = float(np.mean(wy)) - 17.5
        cz_mean = float(np.mean(wz)) - 2.5
        out_vec = np.array([cx_mean, cy_mean * 1.3, cz_mean * 0.6], dtype=np.float32)
        out_norm = out_vec / (np.linalg.norm(out_vec) + 1e-6)
        outwardness = float(np.dot(wn, out_norm))

        if outwardness > 0.45:
            # Outer armor surface! Pure dark pitted basalt with crisp chiseled stone edge bevel!
            bevel = (edge_px == 0)
            col = np.where(bevel[..., None], mix(col, rgb('#4B4550'), np.full((h, w), 0.48)), col)
        elif outwardness < -0.35:
            # Inner underside facing directly into molten magma core
            col = mix(col, rgb('#FF5E00'), np.full((h, w), 0.70))
        else:
            # Thin fissure wall between neighboring plates! Subtle warm magma rim only on the inner 1.5 texels!
            # Compute distance to magma core center to glow only on inner half of thickness:
            r_shell = np.hypot(wx / 15.5, (wy - 17.5) / 13.5)
            inner_glow = np.clip((1.04 - r_shell) * 3.5, 0.0, 1.0)
            col = mix(col, rgb('#FF6600'), inner_glow * 0.78)
            if w >= 3 and h >= 3:
                col = np.where((edge_px == 0)[..., None], col * 1.08, col)

    elif mat == 'basalt_underbelly':
        vent = np.clip(1.0 - np.abs(np.sin(wz * 0.42)) / 0.13, 0.0, 1.0) * np.clip(1.0 - np.abs(wx) / 9.0, 0.0, 1.0)
        col = mix(col, rgb('#FF6200'), vent * 0.72)

    elif mat in ('basalt_leg', 'basalt_horn'):
        if w >= 3 and h >= 3:
            hi_edge = (dv == 0) | (du == 0)
            lo_edge = (dv == h - 1) | (du == w - 1)
            col = np.where(hi_edge[..., None], col * 1.16, col)
            col = np.where(lo_edge[..., None], col * 0.84, col)

    return col


# ============================ 03: ASH MOTH ============================

def paint_moth_face(cube, face_name, w, h, wx, wy, wz, wn, lu, lv, du, dv):
    mat = cube['mat']
    grain = (hash_noise(du, dv, seed=hash(cube['name'] + face_name) & 0xFFFF) - 0.5)
    top_shade = 0.5 + 0.5 * float(wn[1])

    if mat == 'moth_eye':
        rad = np.hypot((lu - 0.42) * 2.0, (lv - 0.38) * 2.0)
        col = mix(rgb('#2A2B30'), rgb('#111115'), np.clip(rad * 0.95, 0.0, 1.0))
        glint = (du == max(0, w // 3)) & (dv == max(0, h // 4))
        col[glint] = rgb('#7A7C85')
        return col

    if mat.startswith('moth_antenna'):
        c1 = rgb('#3D3B39') if 'shaft' in mat else rgb('#524F4C')
        c2 = rgb('#615E5A') if 'shaft' in mat else rgb('#73706C')
        return mix(c1, c2, np.clip(0.4 + 0.4 * top_shade + grain * 0.18, 0.0, 1.0))

    if mat == 'moth_leg_dark':
        return mix(rgb('#242322'), rgb('#42403E'), np.clip(0.35 + 0.45 * top_shade + grain * 0.16, 0.0, 1.0))

    if mat == 'ash_mote':
        return mix(rgb('#6E6C68'), rgb('#9C9A95'), np.clip(0.35 + 0.5 * top_shade + grain * 0.2, 0.0, 1.0))

    if mat.startswith('moth_wool') or mat == 'moth_abdomen':
        tuft = 0.5 + 0.5 * np.sin(wx * 1.3 + np.sin(wz * 1.1) * 1.5) * np.cos(wy * 1.2)
        if mat == 'moth_wool_light':
            c_lo, c_hi = rgb('#9E9C97'), rgb('#CFCBC5')
        elif mat == 'moth_wool_mid':
            c_lo, c_hi = rgb('#888681'), rgb('#B8B6B0')
        else:
            c_lo, c_hi = rgb('#75736E'), rgb('#A8A6A0')
        t = np.clip(0.30 + 0.38 * top_shade + 0.24 * tuft + grain * 0.16, 0.0, 1.0)
        col = mix(c_lo, c_hi, t)
        if mat == 'moth_abdomen':
            band = np.clip(np.sin(lv * math.pi * 2.0) * 0.5 + 0.5, 0.0, 1.0)
            col = col * (0.86 + 0.17 * band[..., None])
        return col

    # Wing-plane projection: wings are pitched forward-upward by ~55 deg around X!
    # Wing chord axis `v_chord` points steeply down-and-back: `(0, -0.82, 0.57)` from shoulder `(5.5, 30.0, -7.0)`!
    ax = np.abs(wx)
    chord_coord = -(wy - 30.0) * 0.82 + (wz - (-7.0)) * 0.57

    if mat.startswith('moth_forewing'):
        span_t = np.clip((ax - 5.5) / 40.5, 0.0, 1.0)
        lead_c = -8.0 * span_t
        local_chord = chord_coord - lead_c

        mottle = fbm3(ax, wy, wz, 0.28, seed=303)
        c_ash_pale = rgb('#BEBDB8')
        c_ash_mid = rgb('#9D9B96')
        c_ash_dark = rgb('#706E69')
        c_margin_dark = rgb('#52504C')

        col = mix(c_ash_mid, c_ash_pale, np.clip(0.25 + 0.55 * mottle + grain * 0.18, 0.0, 1.0))

        # Pale costal leading bar
        costa_mask = np.clip(1.0 - local_chord / 3.2, 0.0, 1.0)
        col = mix(col, rgb('#CCCAC4'), costa_mask * 0.50)

        # Radiating Saturniid wing venation in tilted wing plane
        ang = np.arctan2(chord_coord + 1.5, np.maximum(ax - 5.0, 1.0))
        vein_wave = np.abs(np.sin(ang * 12.0 + 0.22 * np.sin(ax * 0.25)))
        vein_mask = np.clip(1.0 - vein_wave / 0.13, 0.0, 1.0) * np.clip((ax - 7.5) / 7.0, 0.0, 1.0)
        col = mix(col, c_ash_dark, vein_mask * 0.44)

        # Two wavy transverse ash bands across forewing!
        wiggle = np.sin(local_chord * 0.80) * 1.6
        band1 = np.exp(-(((ax + wiggle) - 18.0) / 2.0) ** 2)
        band2 = np.exp(-(((ax + wiggle * 1.1) - 27.5) / 2.3) ** 2)
        col = mix(col, c_ash_dark, np.clip(band1 * 0.52 + band2 * 0.56, 0.0, 0.68))

        # Scalloped darker trailing edge rim
        max_chord = 13.5 + 6.5 * np.sin(span_t * math.pi * 0.85)
        trail_rim = np.clip((local_chord - (max_chord - 3.0)) / 3.0, 0.0, 1.0)
        col = mix(col, c_margin_dark, trail_rim * 0.68)

        # SMOLDERING VOLCANIC EMBER WINGTIP (matches orig_03_moth.png!)
        tip_prog = (ax - 33.5 - np.maximum(0.0, local_chord) * 0.25 + (mottle - 0.5) * 2.0) / 10.8
        if np.any(tip_prog > -0.18):
            soot_band = np.exp(-((tip_prog - 0.04) / 0.15) ** 2)
            col = mix(col, rgb('#3B2016'), soot_band * 0.85)

            ember_t = np.clip(tip_prog, 0.0, 1.0)
            ember_col = mix(rgb('#96260B'), rgb('#EC5E11'), np.clip(ember_t * 2.1, 0.0, 1.0))
            ember_col = mix(ember_col, rgb('#FF9F1A'), np.clip((ember_t - 0.36) * 2.2, 0.0, 1.0))
            ember_col = mix(ember_col, rgb('#FFE066'), np.clip((ember_t - 0.72) * 3.2, 0.0, 1.0))
            cell_flicker = (hash_noise(np.floor(ax), np.floor(chord_coord), seed=91) - 0.5) * 0.16
            ember_col = np.clip(ember_col * (1.0 + cell_flicker[..., None]), 0.0, 255.0)
            col = mix(col, ember_col, np.clip(ember_t * 1.48, 0.0, 1.0))

        col = col * (0.90 + 0.14 * top_shade)
        return col

    if mat == 'moth_hindwing_eye':
        # 3D world-space symmetrical Ocellus Eyespot projected cleanly onto broad flush hindwing!
        mottle = fbm3(ax, wy, wz, 0.30, seed=409)
        col = mix(rgb('#94928D'), rgb('#B5B3AE'), np.clip(0.3 + 0.5 * mottle + grain * 0.16, 0.0, 1.0))

        # Hindwing eyespot center in `(ax, chord_coord)`: `(15.5, 17.8)` — centered cleanly below forewing trailing edge!
        dx = (ax - 15.5)
        dc = (chord_coord - 17.8)
        r_eye = np.hypot(dx, dc)

        # Outer hindwing dark charcoal margin
        dist_root = np.hypot(ax - 6.5, chord_coord - 13.0)
        margin_t = np.clip((dist_root - 15.0) / 3.8, 0.0, 1.0)
        col = mix(col, rgb('#4F4D49'), margin_t * 0.82)

        # Pale buff outer ring around eyespot (r = 4.6 .. 5.9)
        halo = np.clip(1.0 - np.abs(r_eye - 5.1) / 0.90, 0.0, 1.0)
        col = mix(col, rgb('#CCCAC3'), halo * 0.72)

        # Velvety black outer ocellus ring (r <= 4.65)
        in_black = np.clip((4.7 - r_eye) * 1.9, 0.0, 1.0)
        col = mix(col, rgb('#19191C'), in_black)

        # Smoky slate-umber inner iris disc (r <= 2.95)
        in_iris = np.clip((3.0 - r_eye) * 1.9, 0.0, 1.0)
        col = mix(col, rgb('#4D4947'), in_iris)

        # Dark central pupil (r <= 1.45)
        in_pupil = np.clip((1.5 - r_eye) * 2.0, 0.0, 1.0)
        col = mix(col, rgb('#121215'), in_pupil)

        # Pale crescent specular highlight in upper-inner iris quadrant!
        crescent = (r_eye > 1.7) & (r_eye < 3.1) & (dx < -0.1) & (dc < 0.2)
        col = np.where(crescent[..., None], mix(col, rgb('#DFDDD7'), np.full((h, w), 0.78)), col)

        col = col * (0.90 + 0.14 * top_shade)
        return col

    return np.broadcast_to(rgb('#B0AEAA'), (h, w, 3)).copy()


def paint_atlas(spec_path, out_png_path):
    spec = json.loads(Path(spec_path).read_text(encoding='utf-8'))
    model_id = spec['id']
    size = int(spec['atlas_size'])
    atlas = np.zeros((size, size, 4), dtype=np.uint8)

    if model_id == 'porcelain_crane':
        painter = paint_crane_face
    elif model_id == 'basalt_beetle':
        painter = paint_beetle_face
    elif model_id == 'ash_moth':
        painter = paint_moth_face
    elif model_id == 'amethyst_fox':
        painter = paint_fox_face
    elif model_id == 'sizo_leafwing':
        painter = paint_leafwing_face
    elif model_id == 'azure_frog':
        painter = paint_frog_face
    elif model_id == 'blossom_snail':
        painter = paint_snail_face
    elif model_id == 'phantom_wolf':
        painter = paint_wolf_face
    elif model_id == 'crystal_gecko':
        painter = paint_gecko_face
    else:
        raise ValueError(f'Unknown model_id for painter: {model_id}')

    for cube in spec['cubes']:
        for fname in ('east', 'west', 'up', 'down', 'south', 'north'):
            u0, v0, w, h, wx, wy, wz, wn, lu, lv, du, dv = face_world_grid(cube, fname)
            rgb_patch = painter(cube, fname, w, h, wx, wy, wz, wn, lu, lv, du, dv)
            rgb_u8 = np.clip(np.rint(rgb_patch), 0, 255).astype(np.uint8)
            atlas[v0:v0 + h, u0:u0 + w, 0:3] = rgb_u8
            atlas[v0:v0 + h, u0:u0 + w, 3] = 255

    out = Path(out_png_path)
    out.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(atlas, mode='RGBA').save(out, optimize=True)
    print(f'[painter] Wrote {out} ({size}x{size}, {len(spec["cubes"])} cubes)')


if __name__ == '__main__':
    if len(sys.argv) != 3:
        print('Usage: atlas_painter.py <spec.json> <out.png>', file=sys.stderr)
        sys.exit(2)
    paint_atlas(sys.argv[1], sys.argv[2])
