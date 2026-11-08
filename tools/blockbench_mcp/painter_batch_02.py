#!/usr/bin/env python3
"""
3D World-Space + Face-UV Pixel-Art Painters for Batch 02 (V2 Master Polish):
  - 04: `amethyst_fox`   (Аметистовый лис)
  - 05: `sizo_leafwing`  (Сизый листокрыл)
  - 06: `azure_frog`     (Лазурный древесный лягуш)
"""

import math
import numpy as np


def rgb(hex_str):
    hex_str = hex_str.lstrip('#')
    return np.array([int(hex_str[0:2], 16), int(hex_str[2:4], 16), int(hex_str[4:6], 16)], dtype=np.float32)


def mix(c1, c2, t):
    t = np.clip(t, 0.0, 1.0)[..., None]
    return c1 * (1.0 - t) + c2 * t


def hash_noise(ix, iy, seed=0):
    n = (ix.astype(np.int64) * 374761393 + iy.astype(np.int64) * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    n = (n ^ (n >> 16)) & 0xFFFFFFFF
    return n.astype(np.float32) / 4294967295.0


def value_noise_3d(x, y, z, scale=0.25, seed=1):
    sx = x * scale
    sy = y * scale
    sz = z * scale
    ix = np.floor(sx).astype(np.int64)
    iy = np.floor(sy).astype(np.int64)
    iz = np.floor(sz).astype(np.int64)
    fx = sx - ix
    fy = sy - iy
    fz = sz - iz
    ux = fx * fx * (3.0 - 2.0 * fx)
    uy = fy * fy * (3.0 - 2.0 * fy)
    uz = fz * fz * (3.0 - 2.0 * fz)

    def h3(dx, dy, dz):
        q = ((ix + dx) * 73856093 ^ (iy + dy) * 19349663 ^ (iz + dz) * 83492791 ^ (seed * 2654435761)) & 0xFFFFFFFF
        q = ((q ^ (q >> 13)) * 1274126177) & 0xFFFFFFFF
        return ((q ^ (q >> 16)) & 0xFFFFFFFF).astype(np.float32) / 4294967295.0

    c000 = h3(0, 0, 0)
    c100 = h3(1, 0, 0)
    c010 = h3(0, 1, 0)
    c110 = h3(1, 1, 0)
    c001 = h3(0, 0, 1)
    c101 = h3(1, 0, 1)
    c011 = h3(0, 1, 1)
    c111 = h3(1, 1, 1)

    x00 = c000 + (c100 - c000) * ux
    x10 = c010 + (c110 - c010) * ux
    x01 = c001 + (c101 - c001) * ux
    x11 = c011 + (c111 - c011) * ux
    y0 = x00 + (x10 - x00) * uy
    y1 = x01 + (x11 - x01) * uy
    return y0 + (y1 - y0) * uz


def fbm3(x, y, z, base_scale=0.2, seed=7):
    v = 0.55 * value_noise_3d(x, y, z, base_scale, seed)
    v += 0.30 * value_noise_3d(x, y, z, base_scale * 2.03, seed + 17)
    v += 0.15 * value_noise_3d(x, y, z, base_scale * 4.07, seed + 43)
    return v


# ============================================================================
# 04 — AMETHYST FOX (`amethyst_fox`)
# ============================================================================

def paint_fox_face(cube, face_name, w, h, wx, wy, wz, wn, lu, lv, du, dv):
    mat = cube['mat']
    grain = (hash_noise(du, dv, seed=hash(cube['name'] + face_name) & 0xFFFF) - 0.5)
    top_shade = 0.5 + 0.5 * float(wn[1])

    c_cream = rgb('#FAF6F0')
    c_cream_sh = rgb('#E6E0D6')
    c_lilac_pale = rgb('#CDB2FC')
    c_lilac_light = rgb('#B692F6')
    c_lilac_mid = rgb('#9668E3')
    c_violet_deep = rgb('#7240C4')
    c_plum_dark = rgb('#4B1E8C')

    if mat == 'amethyst_crystal':
        meta = cube.get('meta') or {}
        t0 = float(meta.get('crysT0', 0.0))
        t1 = float(meta.get('crysT1', 1.0))
        if face_name in ('north', 'south', 'east', 'west'):
            prog = t0 + (1.0 - lv) * (t1 - t0)
        elif face_name == 'up':
            prog = np.full((h, w), t1, dtype=np.float32)
        else:
            prog = np.full((h, w), t0, dtype=np.float32)

        facet_n = fbm3(np.floor(wx * 1.5), np.floor(wy * 1.5), np.floor(wz * 1.5), 0.55, seed=401)
        prog_j = np.clip(prog + (facet_n - 0.5) * 0.14, 0.0, 1.0)

        col = mix(rgb('#4A1886'), rgb('#813AD6'), np.clip(prog_j * 2.1, 0.0, 1.0))
        col = mix(col, rgb('#BC77FA'), np.clip((prog_j - 0.35) * 2.2, 0.0, 1.0))
        col = mix(col, rgb('#F0C4FF'), np.clip((prog_j - 0.68) * 2.8, 0.0, 1.0))
        col = mix(col, rgb('#FDF5FF'), np.clip((prog_j - 0.86) * 6.0, 0.0, 1.0))

        facet_light = 0.5 + 0.35 * float(wn[1]) - 0.20 * float(wn[2]) + 0.15 * abs(float(wn[0]))
        col = col * (0.86 + 0.28 * facet_light)
        if w >= 2 and h >= 2:
            hi_bevel = (dv == 0) | (du == 0)
            col = np.where(hi_bevel[..., None], mix(col, rgb('#FCF2FF'), np.full((h, w), 0.44)), col)
        return col

    if mat == 'fox_eye':
        # Bright glowing white-lavender eye with rich violet upper brow!
        col = mix(rgb('#F6ECFF'), rgb('#FFFFFF'), np.clip(1.0 - lv * 0.6, 0.0, 1.0))
        brow = (dv == 0)
        col = np.where(brow[..., None], c_violet_deep, col)
        outer = (lu > 0.66) & (lv > 0.30)
        col = np.where(outer[..., None], rgb('#CFA8FF'), col)
        return col

    if mat == 'fox_snout':
        # Sleek lilac upper snout bridge with dark plum nose button at front-top & cream lower edge!
        col = mix(c_lilac_mid, c_lilac_pale, np.clip(0.35 + 0.5 * top_shade + grain * 0.10, 0.0, 1.0))
        is_nose = (wz < -21.2) & (wy > 27.2) & (np.abs(wx) < 1.5)
        col = np.where(is_nose[..., None], c_plum_dark, col)
        return col

    if mat == 'fox_chest_white':
        tuft = hash_noise(np.floor(wx * 1.2), np.floor(wy * 1.0), seed=77)
        t = np.clip(0.42 + 0.42 * top_shade + (tuft - 0.5) * 0.22 + grain * 0.08, 0.0, 1.0)
        return mix(c_cream_sh, c_cream, t)

    if mat == 'fox_ear':
        fur = hash_noise(np.floor(wx * 1.5), np.floor(wy * 1.3), seed=93)
        col = mix(c_violet_deep, c_lilac_light, np.clip(0.35 + 0.45 * lv + (fur - 0.5) * 0.22, 0.0, 1.0))
        if float(wn[2]) < -0.40:
            # Broad creamy-white inner ear triangle on front (-Z) face!
            in_tri = (lu > 0.16) & (lu < 0.84) & (lv > 0.14)
            col = np.where(in_tri[..., None], mix(c_cream_sh, c_cream, np.full((h, w), 0.84)), col)
        return col

    if mat == 'fox_paw':
        meta = cube.get('meta') or {}
        tip_z = float(meta.get('toeTipZ', -10.0))
        is_toe = wz < (tip_z + 1.7)
        cream_col = mix(c_cream_sh, c_cream, np.clip(0.45 + 0.42 * top_shade + grain * 0.08, 0.0, 1.0))
        toe_col = mix(c_lilac_mid, c_lilac_pale, np.clip(0.35 + 0.5 * top_shade + grain * 0.10, 0.0, 1.0))
        divider = is_toe & (np.abs(np.abs(wx) - 3.1) < 0.35)
        res = np.where(is_toe[..., None], toe_col, cream_col)
        res = np.where(divider[..., None], c_violet_deep, res)
        return res

    # Luminous Lilac-Violet Fox Fur Coat!
    fur_cell = hash_noise(np.floor(wx * 1.2), np.floor(wz * 0.9 + wy * 0.6), seed=211)
    fur_streak = fbm3(wx * 1.0, wy * 1.0, wz * 0.6, 0.36, seed=219)
    coat_t = np.clip(0.34 + 0.34 * top_shade + 0.26 * fur_streak + (fur_cell - 0.5) * 0.26, 0.0, 1.0)
    coat_col = mix(c_violet_deep, c_lilac_mid, np.clip(coat_t * 1.55, 0.0, 1.0))
    coat_col = mix(coat_col, c_lilac_light, np.clip((coat_t - 0.45) * 1.85, 0.0, 1.0))
    coat_col = mix(coat_col, c_lilac_pale, np.clip((coat_t - 0.74) * 2.4, 0.0, 1.0))

    if mat == 'fox_leg':
        jag = (hash_noise(np.floor(wx * 1.7), np.floor(wz * 1.7), seed=305) - 0.5) * 2.4
        is_white = (wy + jag) < 9.8
        white_col = mix(c_cream_sh, c_cream, np.clip(0.44 + 0.42 * top_shade + grain * 0.08, 0.0, 1.0))
        return np.where(is_white[..., None], white_col, coat_col)

    if mat == 'fox_head':
        # Ragged creamy-white lower cheek muffs (`wy < 27.2 + jag`), soft pale-lilac forehead crown!
        jag = (hash_noise(np.floor(wx * 1.5), np.floor(wz * 1.5), seed=312) - 0.5) * 1.6
        is_cheek_white = ((wy + jag) < 27.2) & ((wz > -16.8) | (wy < 26.2))
        white_col = mix(c_cream_sh, c_cream, np.clip(0.46 + 0.42 * top_shade + grain * 0.08, 0.0, 1.0))
        crown_boost = np.clip((wy - 29.8) / 3.2, 0.0, 1.0)
        head_violet = mix(coat_col, c_lilac_pale, crown_boost * 0.58)
        return np.where(is_cheek_white[..., None], white_col, head_violet)

    if mat == 'fox_tail':
        tail_prog = np.clip((wz - 10.5) / 19.0, 0.0, 1.0)
        fluff = mix(coat_col, c_lilac_pale, np.clip((tail_prog - 0.25) * 0.72, 0.0, 0.52))
        geode_rim = np.clip((tail_prog - 0.82) * 5.0, 0.0, 1.0)
        return mix(fluff, c_plum_dark, geode_rim * 0.65)

    # `fox_fur_body`: violet dorsal/flank + creamy-white underbelly & front chest!
    jag = (hash_noise(np.floor(wx * 1.4), np.floor(wz * 1.1), seed=331) - 0.5) * 2.0
    is_belly = ((wy + jag) < 16.4) & (np.abs(wx) < 4.5)
    is_front_chest = (wz < -7.2) & ((wy + jag) < 25.5) & (np.abs(wx) < 4.3)
    white_col = mix(c_cream_sh, c_cream, np.clip(0.44 + 0.44 * top_shade + grain * 0.08, 0.0, 1.0))
    return np.where((is_belly | is_front_chest)[..., None], white_col, coat_col)


# ============================================================================
# 05 — SIZO LEAFWING (`sizo_leafwing`)
# ============================================================================

def paint_leafwing_face(cube, face_name, w, h, wx, wy, wz, wn, lu, lv, du, dv):
    mat = cube['mat']
    grain = (hash_noise(du, dv, seed=hash(cube['name'] + face_name) & 0xFFFF) - 0.5)
    top_shade = 0.5 + 0.5 * float(wn[1])

    c_mint_pale = rgb('#D4E6DF')
    c_mint_mid = rgb('#ABC8BE')
    c_mint_shadow = rgb('#7FA099')
    c_slate_vein = rgb('#607A8C')
    c_cream_fur = rgb('#F4F7EE')
    c_cream_sh = rgb('#DAE3D8')

    if mat == 'leafwing_antler':
        bark = hash_noise(np.floor(wx * 2.0), np.floor(wy * 1.6 + wz), seed=501)
        t = np.clip(0.32 + 0.45 * top_shade + (bark - 0.5) * 0.22 + grain * 0.10, 0.0, 1.0)
        return mix(rgb('#5E4838'), rgb('#8E745E'), t)

    if mat == 'leafwing_claw':
        return mix(rgb('#263238'), rgb('#46575F'), np.clip(0.3 + 0.5 * top_shade + grain * 0.10, 0.0, 1.0))

    if mat == 'leafwing_flower':
        cx = np.hypot(lu - 0.5, lv - 0.5)
        col = mix(rgb('#6272EC'), rgb('#9BA9FF'), np.clip(0.4 + 0.5 * top_shade + grain * 0.12, 0.0, 1.0))
        center_dot = cx < 0.22
        col = np.where(center_dot[..., None], rgb('#FAF0A0'), col)
        return col

    if mat == 'leafwing_vine':
        return mix(rgb('#4E8060'), rgb('#76B086'), np.clip(0.35 + 0.5 * top_shade + grain * 0.12, 0.0, 1.0))

    if mat == 'leafwing_eye':
        # Large Expressive Warm Honey-Amber Dragon Eye!
        if abs(float(wn[0])) > 0.35 or float(wn[2]) < -0.35:
            dx = (lu - 0.50) * 2.0
            dy = (lv - 0.52) * 2.0
            r = np.hypot(dx * 0.88, dy * 1.0)
            skin = mix(c_mint_mid, c_mint_pale, np.full((h, w), 0.65))
            iris_t = np.clip((lv - 0.15) * 1.25, 0.0, 1.0)
            iris = mix(rgb('#B8560A'), rgb('#F39F1D'), iris_t)
            iris = mix(iris, rgb('#FFD966'), np.clip((lv - 0.62) * 2.6, 0.0, 1.0))
            col = np.where((r < 1.05)[..., None], iris, skin)
            pupil = (np.abs(dx) < 0.32) & (np.abs(dy) < 0.64)
            col = np.where(pupil[..., None], rgb('#221510'), col)
            lash = (dv == 0)
            col = np.where(lash[..., None], rgb('#2B1C17'), col)
            glint = (du == max(0, w // 3)) & (dv == min(h - 1, max(1, h // 3)))
            col[glint] = rgb('#FFFFFF')
            return col
        return mix(c_mint_mid, c_mint_pale, np.full((h, w), 0.65))

    if mat == 'leafwing_chest_fur':
        # Layered cream-white chest plumage with jagged furry V-tips at lower border (`lv > 0.78`)!
        tuft = fbm3(wx, wy, wz, 0.32, seed=521)
        t = np.clip(0.44 + 0.42 * top_shade + (tuft - 0.5) * 0.22 + grain * 0.08, 0.0, 1.0)
        col = mix(c_cream_sh, c_cream_fur, t)
        v_tooth = np.abs(((wx * 0.55) % 1.0) - 0.5) * 0.35
        tip_shade = np.clip((lv - 0.72 + v_tooth) * 4.0, 0.0, 1.0)
        return mix(col, rgb('#C4D5CC'), tip_shade * 0.55)

    if mat == 'leafwing_wing_blade':
        # CLEAN LUMINOUS HYTALE LEAF-WING MEMBRANE (matches orig_05_leafwing.png 1:1!):
        # Pure warm ivory-cream inner leaf lamina + 4 crisp slate-blue radiating veins + sage-green outer leaf tips!
        ax = np.abs(wx)
        # Leading edge runs from wrist `(ax=20, wy=42)` out to wingtip `(ax=51, wy=51)`!
        lead_y = np.where(ax < 20.0, 27.0 + (ax - 5.0) * 1.0, 42.0 + (ax - 20.0) * 0.29)
        chord_down = lead_y - wy

        max_chord = 7.0 + 16.0 * np.sin(np.clip((ax - 7.0) / 44.0, 0.0, 1.0) * math.pi * 0.80)
        rel_chord = chord_down / np.maximum(max_chord, 4.5)

        mottle = fbm3(ax, wy, wz, 0.22, seed=547)
        # Smooth, luminous warm cream-ivory leaf lamina!
        col = mix(rgb('#EAEEE5'), rgb('#F8FAF3'), np.clip(0.45 + 0.40 * mottle + grain * 0.06, 0.0, 1.0))

        # Organic Leaf-Shaped Silvery-Sage Green Outer Margin (like painted botanical leaf tips in orig_05_leafwing.png!)
        leaf_lobe = np.sin(ax * 0.42 + wy * 0.28) * 0.09
        tip_green = np.clip((ax - 37.5) / 8.5 + leaf_lobe, 0.0, 1.0)
        edge_green = np.clip((rel_chord - 0.64 + leaf_lobe) * 4.8, 0.0, 1.0)
        green_mask = np.clip(np.maximum(tip_green, edge_green), 0.0, 1.0)
        sage_col = mix(rgb('#7D9B8F'), rgb('#9DBAAE'), np.clip(0.38 + 0.48 * top_shade + grain * 0.08, 0.0, 1.0))
        col = mix(col, sage_col, green_mask)

        # 4 Crisp Radiating Slate-Blue Primary Leaf Veins from wrist `(ax=20.2, wy=41.5)`!
        vx = ax - 20.2
        vy = wy - 41.5
        vein_angles = [-0.02, -0.46, -0.94, -1.44]
        v_mask = np.zeros((h, w), dtype=np.float32)
        for va in vein_angles:
            perp = np.abs(-vx * math.sin(va) + vy * math.cos(va))
            along = vx * math.cos(va) + vy * math.sin(va)
            ray = np.clip(1.0 - perp / 0.62, 0.0, 1.0) * (along > -1.2).astype(np.float32)
            v_mask = np.maximum(v_mask, ray)

        col = mix(col, c_slate_vein, v_mask * 0.88)
        return col * (0.93 + 0.11 * top_shade)

    if mat == 'leafwing_tail_leaf':
        # Broad sculpted Mint-Jade Leaf Paddle at tip of curling tail!
        mottle = fbm3(wx, wy, wz, 0.30, seed=563)
        c_leaf_dark = rgb('#52967A')
        c_leaf_mid = rgb('#7BC4A3')
        c_leaf_light = rgb('#A5E2C3')
        col = mix(c_leaf_mid, c_leaf_light, np.clip(0.38 + 0.45 * top_shade + (mottle - 0.5) * 0.22 + grain * 0.08, 0.0, 1.0))
        midrib = np.clip(1.0 - np.abs(lv - 0.5) / 0.075, 0.0, 1.0)
        # 3 clean herringbone leaf vein pairs!
        herr = np.abs(((lu * 3.0 - np.abs(lv - 0.5) * 1.6) % 1.0) - 0.5)
        side_v = np.clip(1.0 - herr / 0.055, 0.0, 1.0) * (np.abs(lv - 0.5) < 0.42).astype(np.float32)
        vein = np.maximum(midrib, side_v * 0.72)
        return mix(col, c_leaf_dark, vein * 0.82)

    # Silvery-mint dragon skin/fur
    soft_n = fbm3(wx, wy, wz, 0.24, seed=577)
    t = np.clip(0.36 + 0.42 * top_shade + 0.20 * soft_n + grain * 0.08, 0.0, 1.0)
    col = mix(c_mint_shadow, c_mint_mid, np.clip(t * 1.5, 0.0, 1.0))
    col = mix(col, c_mint_pale, np.clip((t - 0.46) * 1.8, 0.0, 1.0))

    if mat == 'leafwing_tail':
        is_ventral = (float(wn[1]) < -0.35) | ((wy < 9.5) & (np.abs(wx) < 2.2))
        col = np.where(np.broadcast_to(is_ventral, (h, w))[..., None], mix(col, c_cream_fur, np.full((h, w), 0.72)), col)

    if mat == 'leafwing_head':
        # Gentle soft-mint smile contour along lateral cheek!
        is_side_smile = (abs(float(wn[0])) > 0.5) & (wz < -13.2) & (np.abs(wy - 33.4) < 0.38)
        col = np.where(is_side_smile[..., None], rgb('#7B9D96'), col)

    return col


# ============================================================================
# 06 — AZURE TREE FROG (`azure_frog`)
# ============================================================================

def paint_frog_face(cube, face_name, w, h, wx, wy, wz, wn, lu, lv, du, dv):
    mat = cube['mat']
    grain = (hash_noise(du, dv, seed=hash(cube['name'] + face_name) & 0xFFFF) - 0.5)
    top_shade = 0.5 + 0.5 * float(wn[1])

    c_azure_bright = rgb('#2CC4D6')
    c_azure_mid = rgb('#189EC0')
    c_cobalt_spot = rgb('#1668B2')
    c_aqua_belly = rgb('#88DCE4')
    c_aqua_pale = rgb('#AAEBF0')
    c_lime_spot = rgb('#8FD986')
    c_toe_mint = rgb('#9BE3A2')

    if mat == 'frog_eye_gold':
        # Strictly ONE golden iris + `+` cross pupil per eye (`north` face angled 26 deg outward-forward), wrapped in turquoise eyelid skin!
        if face_name == 'north':
            iris = mix(rgb('#E59E0C'), rgb('#FFD42E'), np.clip(0.40 + 0.50 * (1.0 - lv) + grain * 0.08, 0.0, 1.0))
            cu = (w - 1) / 2.0
            cv = (h - 1) / 2.0
            in_bounds = (du >= 1) & (du <= w - 2) & (dv >= 1) & (dv <= h - 2)
            cross_arm = (np.abs(du - cu) <= 0.55) | (np.abs(dv - cv) <= 0.55)
            col = np.where((in_bounds & cross_arm)[..., None], rgb('#111318'), iris)
            return col
        return mix(c_azure_mid, c_azure_bright, np.clip(0.38 + 0.45 * top_shade + grain * 0.08, 0.0, 1.0))

    if mat == 'frog_petal_collar':
        # Soft sakura-pink lotus collar petal with cute V-shaped lime-mint sepal base!
        r_neck = np.hypot(wx, wz - (-2.8))
        petal_t = np.clip((r_neck - 7.4) / 6.0, 0.0, 1.0)
        v_shape = np.abs(lu - 0.5) * 0.32
        is_sepal = (petal_t - v_shape) < 0.30

        pink_col = mix(rgb('#F59CB6'), rgb('#FFCAD8'), np.clip(0.35 + 0.50 * petal_t + 0.2 * top_shade + grain * 0.08, 0.0, 1.0))
        sepal_col = mix(rgb('#78C47E'), rgb('#A6E4A0'), np.clip(0.40 + 0.45 * top_shade + grain * 0.10, 0.0, 1.0))
        return np.where(is_sepal[..., None], sepal_col, pink_col)

    if mat == 'frog_bud_stem':
        return mix(rgb('#58A464'), rgb('#84CE88'), np.clip(0.35 + 0.5 * top_shade + grain * 0.10, 0.0, 1.0))

    if mat == 'frog_bud_pink':
        return mix(rgb('#F492AE'), rgb('#FFD2DF'), np.clip(0.40 + 0.5 * top_shade + grain * 0.10, 0.0, 1.0))

    if mat == 'frog_toe_pad':
        return mix(rgb('#7ECE8A'), c_toe_mint, np.clip(0.40 + 0.52 * top_shade + grain * 0.10, 0.0, 1.0))

    if mat == 'frog_belly':
        return mix(c_aqua_belly, c_aqua_pale, np.clip(0.40 + 0.48 * top_shade + grain * 0.08, 0.0, 1.0))

    # Smooth Tropical Azure-Turquoise Skin with tasteful pixel spots on back & outer limbs!
    skin_n = fbm3(wx, wy, wz, 0.24, seed=607)
    base = mix(c_azure_mid, c_azure_bright, np.clip(0.35 + 0.45 * top_shade + 0.22 * skin_n + grain * 0.08, 0.0, 1.0))

    if mat == 'frog_head':
        # Clean, smooth smiling turquoise-aqua frog face (matching orig_06_frog.png!)
        # Gentle mint-lime speckles ONLY on the upper crown/bridge between the eyes (`wy > 22.8, abs(wx) < 3.8`)!
        crown_cell = hash_noise(np.floor(wx * 0.9), np.floor(wz * 0.9), seed=631)
        crown_mint = (wy > 22.6) & (np.abs(wx) < 3.8) & (wz > -10.2) & (wz < -4.2) & (crown_cell < 0.48)
        col = np.where(crown_mint[..., None], mix(base, c_lime_spot, np.full((h, w), 0.80)), base)

        # Lower cheeks, upper lip rim, and chin transition smoothly into luminous pale sky-aqua!
        aqua_blend = np.clip((20.8 - wy) / 2.2, 0.0, 1.0)
        belly_col = mix(c_aqua_belly, c_aqua_pale, np.clip(0.42 + 0.45 * top_shade + grain * 0.06, 0.0, 1.0))
        col = mix(col, belly_col, aqua_blend * 0.88)

        # Crisp, unbroken happy upward-curved frog smile seam + two tiny nostrils on top-front of snout!
        smile_y = 19.65 + 0.042 * (wx ** 2)
        is_smile = (wz < -7.8) & (np.abs(wy - smile_y) < 0.58) & (np.abs(wx) < 6.4)
        is_nostril = (wz < -11.4) & (np.abs(wy - 21.8) < 0.42) & (np.abs(np.abs(wx) - 2.1) < 0.45)
        col = np.where((is_smile | is_nostril)[..., None], rgb('#0B526E'), col)
        return col

    # Dorsal & Limb Spots (clean 2-unit pixel clusters on back & outer legs, never cluttering the chest!)
    cell_s = hash_noise(np.floor(wx * 0.62), np.floor(wy * 0.62 + wz * 0.62), seed=619)
    in_spot_zone = (wy > 4.5) & ((wz > -4.5) | (np.abs(wx) > 5.5))
    cobalt_mask = in_spot_zone & (cell_s > 0.80)
    lime_mask = in_spot_zone & (cell_s < 0.15)
    col = np.where(cobalt_mask[..., None], mix(base, c_cobalt_spot, np.full((h, w), 0.72)), base)
    col = np.where(lime_mask[..., None], mix(col, c_lime_spot, np.full((h, w), 0.82)), col)

    if mat == 'frog_skin_body':
        is_ventral = ((wz < -3.2) & (np.abs(wx) < 6.4) & (wy < 15.8)) | (wy < 4.8)
        belly_col = mix(c_aqua_belly, c_aqua_pale, np.clip(0.40 + 0.48 * top_shade + grain * 0.08, 0.0, 1.0))
        col = np.where(is_ventral[..., None], belly_col, col)

    return col
