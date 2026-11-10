#!/usr/bin/env python3
"""
3D World-Space + Crisp Artisanal `(du, dv)` Pixel-Art Painters for Batch 03 (V3 Masterpiece):
  - 07: `blossom_snail` (`Цветочная улитка`)
  - 08: `phantom_wolf`  (`Туманный волк-призрак`)
  - 09: `crystal_gecko` (`Кристальный ящер`)
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
# 07 — BLOSSOM SNAIL (`blossom_snail`)
# ============================================================================

def paint_snail_face(cube, face_name, w, h, wx, wy, wz, wn, lu, lv, du, dv):
    mat = cube['mat']
    grain = (hash_noise(du, dv, seed=hash(cube['name'] + face_name) & 0xFFFF) - 0.5)
    top_shade = 0.5 + 0.5 * float(wn[1])

    c_mint_bright = rgb('#66E2CE')
    c_mint_mid = rgb('#3EBFA9')
    c_mint_deep = rgb('#26968A')
    c_mint_belly = rgb('#9AF0E0')

    if mat == 'snail_eye_bulb':
        # `size = [3, 3, 2]` -> front face `w=3, h=3`: crisp white sclera with centered dark pupil!
        if face_name == 'north':
            col = np.broadcast_to(rgb('#F6FCF9'), (h, w, 3)).copy()
            pupil = (du == w // 2) & (dv >= 1)
            top_lid = (dv == 0)
            col = np.where(top_lid[..., None], c_mint_bright, col)
            col = np.where(pupil[..., None], rgb('#1E3434'), col)
            return col
        return mix(c_mint_mid, c_mint_bright, np.clip(0.45 + 0.45 * top_shade + grain * 0.08, 0.0, 1.0))

    if mat == 'snail_pollen':
        return mix(rgb('#FFE27A'), rgb('#FFFAD0'), np.clip(0.5 + 0.4 * top_shade + grain * 0.10, 0.0, 1.0))

    if mat == 'snail_moss':
        moss_n = hash_noise(du, dv, seed=701 + (hash(cube['name']) & 0xFF))
        t = np.clip(0.32 + 0.44 * top_shade + (moss_n - 0.5) * 0.34, 0.0, 1.0)
        return mix(rgb('#488834'), rgb('#88CA58'), t)

    if mat == 'snail_calyx':
        wood_n = hash_noise(du, dv, seed=709 + (hash(cube['name']) & 0xFF))
        col = mix(rgb('#A67B5E'), rgb('#D2A888'), np.clip(0.38 + 0.45 * top_shade + (wood_n - 0.5) * 0.24, 0.0, 1.0))
        has_moss = (wy > 19.5) & (wood_n > 0.72)
        return np.where(has_moss[..., None], mix(col, rgb('#64A844'), np.full((h, w), 0.85)), col)

    if mat == 'snail_rose_core':
        # Radiant Glowing Golden-Peach Flower Heart (`r = 0..5.2`)!
        r_core = np.hypot(wy - 18.0, wz - 3.5)
        col = np.broadcast_to(rgb('#FFA048'), (h, w, 3)).copy()
        halo = np.exp(-((r_core - 2.6) / 1.35) ** 2)
        col = mix(col, rgb('#FFFDE4'), halo * 0.98)
        inner_bud = r_core < 1.35
        col = np.where(inner_bud[..., None], rgb('#FF9A42'), col)
        outer_t = np.clip((r_core - 3.6) / 2.2, 0.0, 1.0)
        col = mix(col, rgb('#FFC088'), outer_t * 0.88)
        col = mix(col, rgb('#FA9CB6'), np.clip((r_core - 4.9) / 1.6, 0.0, 1.0) * 0.90)
        return col

    if mat == 'snail_rose_petal':
        r_core = np.hypot(wy - 18.0, wz - 3.5)
        warm_glow = np.clip(1.0 - (r_core - 4.4) / 4.2, 0.0, 1.0)
        p_noise = hash_noise(du, dv, seed=727 + (hash(cube['name']) & 0xFF))
        pink_base = mix(rgb('#E2789A'), rgb('#FCB2C9'), np.clip(0.36 + 0.46 * top_shade + (p_noise - 0.5) * 0.22, 0.0, 1.0))
        if w >= 2 and h >= 2:
            rim = (dv == 0) | (du == 0)
            pink_base = np.where(rim[..., None], mix(pink_base, rgb('#FFE0EB'), np.full((h, w), 0.62)), pink_base)
        col = mix(pink_base, rgb('#FFD69C'), warm_glow * 0.68)
        spark = (p_noise > 0.94) & (wy < 16.5)
        col = np.where(spark[..., None], rgb('#FFF6B2'), col)
        return col

    if mat == 'snail_shell_outer':
        ang = np.arctan2(wy - 18.0, wz - 3.5)
        rib = np.sin(ang * 14.0) * 0.5 + 0.5
        s_noise = hash_noise(np.floor(wx), np.floor(wy + wz), seed=739)
        col = mix(rgb('#EB8FAA'), rgb('#FDC2D6'), np.clip(0.36 + 0.42 * top_shade + 0.16 * rib + (s_noise - 0.5) * 0.16, 0.0, 1.0))
        # Fine scattered single-pixel moss speckles on front shoulder of shell!
        moss_px = hash_noise(np.floor(wx * 1.1), np.floor(wy * 1.1 + wz * 1.3), seed=743)
        moss_zone = (wz < -0.5) & (wy > 20.5) & (wy < 27.5) & (moss_px > 0.72)
        moss_col = mix(rgb('#4E8E38'), rgb('#84C656'), np.clip(0.40 + 0.45 * top_shade + grain * 0.12, 0.0, 1.0))
        return np.where(moss_zone[..., None], moss_col, col)

    # Soft Mint-Turquoise Snail Body!
    b_noise = fbm3(wx, wy, wz, 0.28, seed=751)
    skin = mix(c_mint_deep, c_mint_mid, np.clip(0.38 + 0.45 * top_shade + 0.20 * b_noise + grain * 0.06, 0.0, 1.0))
    skin = mix(skin, c_mint_bright, np.clip((top_shade - 0.40) * 1.4, 0.0, 0.78))

    if mat == 'snail_belly':
        seg_line = (dv % 2) == 0
        belly = mix(rgb('#7CE2CD'), c_mint_belly, np.clip(0.46 + 0.42 * top_shade + grain * 0.05, 0.0, 1.0))
        return np.where(seg_line[..., None], belly, mix(belly, c_mint_mid, np.full((h, w), 0.28)))

    if mat == 'snail_head':
        # Crisp 1-pixel artisanal smile on front face (`w = 7, h = 5`, center `du = 3`)!
        if face_name == 'north':
            cx = w // 2
            smile_mid = (dv == h - 2) & (np.abs(du - cx) <= 1)
            cheek_blush = (dv == h - 2) & (np.abs(du - cx) == 2)
            col = np.where(cheek_blush[..., None], mix(skin, rgb('#8CF2DF'), np.full((h, w), 0.45)), skin)
            col = np.where(smile_mid[..., None], rgb('#155A54'), col)
            return col
        return skin

    is_sole_rim = wy < 1.9
    sole_col = mix(rgb('#7CE2CD'), c_mint_belly, np.clip(0.45 + 0.4 * top_shade + grain * 0.06, 0.0, 1.0))
    speck = hash_noise(np.floor(wx), np.floor(wy + wz), seed=761)
    is_dot = (wy >= 2.2) & (wy < 6.8) & (speck > 0.87)
    col = np.where(is_dot[..., None], c_mint_deep, skin)
    col = np.where(is_sole_rim[..., None], sole_col, col)
    return col


# ============================================================================
# 08 — PHANTOM MIST WOLF (`phantom_wolf`)
# ============================================================================

def paint_wolf_face(cube, face_name, w, h, wx, wy, wz, wn, lu, lv, du, dv):
    mat = cube['mat']
    grain = (hash_noise(du, dv, seed=hash(cube['name'] + face_name) & 0xFFFF) - 0.5)
    top_shade = 0.5 + 0.5 * float(wn[1])

    c_ash_dark = rgb('#586268')
    c_mist_mid = rgb('#86959B')
    c_mist_pale = rgb('#BACACE')
    c_mist_white = rgb('#E2ECEF')
    c_cyan_glow = rgb('#9EF8F8')
    c_cyan_bright = rgb('#EEFFFF')
    c_cyan_mid = rgb('#66CACE')

    if mat == 'wolf_eye_cyan':
        # Blazing Piercing Ice-Cyan Spectral Eyes on front & outer-cheek faces!
        if face_name in ('north', 'east', 'west'):
            return mix(c_cyan_glow, c_cyan_bright, np.clip(0.55 + (1.0 - lv) * 0.45, 0.0, 1.0))
        return c_ash_dark

    if mat == 'wolf_nose':
        return mix(rgb('#343B3F'), rgb('#505A5F'), np.clip(0.35 + 0.45 * top_shade + grain * 0.10, 0.0, 1.0))

    if mat == 'wolf_spirit_core':
        pulse = fbm3(wx, wy, wz, 0.32, seed=809)
        vert = 1.0 - np.clip(np.abs(wy - 22.0) / 5.5, 0.0, 1.0)
        col = mix(rgb('#62B8BE'), c_cyan_glow, np.clip(0.45 + 0.45 * vert + 0.25 * pulse + grain * 0.06, 0.0, 1.0))
        col = mix(col, c_cyan_bright, np.clip((vert * pulse - 0.28) * 2.2, 0.0, 0.85))
        return col

    if mat == 'wolf_rib_bone':
        m_noise = fbm3(wx, wy, wz, 0.35, seed=817)
        bone = mix(rgb('#96B2B7'), rgb('#DCF8F8'), np.clip(0.45 + 0.35 * top_shade + 0.25 * m_noise + grain * 0.06, 0.0, 1.0))
        cyan_tint = np.clip(1.0 - np.abs(wy - 22.0) / 5.2, 0.0, 1.0)
        return mix(bone, c_cyan_glow, cyan_tint * 0.55)

    if mat == 'wolf_wisp':
        w_noise = hash_noise(du, dv, seed=823 + (hash(cube['name']) & 0xFF))
        t = np.clip(0.44 + 0.36 * top_shade + (w_noise - 0.5) * 0.30, 0.0, 1.0)
        col = mix(c_mist_mid, c_mist_pale, np.clip(t * 1.45, 0.0, 1.0))
        col = mix(col, c_mist_white, np.clip((t - 0.50) * 2.1, 0.0, 1.0))
        return col

    cell_n = hash_noise(du, dv, seed=829 + (hash(cube['name'] + face_name) & 0xFF))
    mist_f = fbm3(wx, wy, wz, 0.26, seed=839)
    t = np.clip(0.30 + 0.34 * top_shade + 0.26 * mist_f + (cell_n - 0.5) * 0.28, 0.0, 1.0)
    col = mix(c_ash_dark, c_mist_mid, np.clip(t * 1.55, 0.0, 1.0))
    col = mix(col, c_mist_pale, np.clip((t - 0.48) * 1.85, 0.0, 1.0))
    col = mix(col, c_mist_white, np.clip((t - 0.78) * 2.5, 0.0, 1.0))

    if mat == 'wolf_leg':
        ankle_fog = np.clip((5.5 - wy) / 4.8, 0.0, 1.0)
        col = mix(col, c_mist_white, ankle_fog * 0.68)
        return col

    # Subtle cyan spirit spill around ribcage & throat fissures!
    rib_prox = np.exp(-((wz - 1.5) / 5.8) ** 2) * np.exp(-((wy - 21.8) / 4.8) ** 2)
    throat_prox = np.exp(-((wz + 8.5) / 2.8) ** 2) * np.exp(-((wy - 23.0) / 3.5) ** 2)
    col = mix(col, c_cyan_mid, np.clip(rib_prox * 0.45 + throat_prox * 0.35, 0.0, 0.65))
    return col


# ============================================================================
# 09 — CRYSTAL GECKO (`crystal_gecko`)
# ============================================================================

def paint_gecko_face(cube, face_name, w, h, wx, wy, wz, wn, lu, lv, du, dv):
    mat = cube['mat']
    grain = (hash_noise(du, dv, seed=hash(cube['name'] + face_name) & 0xFFFF) - 0.5)
    top_shade = 0.5 + 0.5 * float(wn[1])
    edge_px = np.minimum(np.minimum(du, w - 1 - du), np.minimum(dv, h - 1 - dv))

    c_ice_pale = rgb('#DFF6FF')
    c_ice_light = rgb('#B2E4FC')
    c_ice_mid = rgb('#7EC5F0')
    c_ice_deep = rgb('#5AA0D8')
    c_cyan_glow = rgb('#6CF4FF')
    c_cyan_white = rgb('#F0FFFF')

    if mat == 'gecko_eye':
        # Incandescent Glowing Ice-Lantern Gecko Eye (`w=5, h=4`):
        # Soft sky-blue outer rim (`edge_px == 0`), two blazing white-cyan vertical bars (`du=1, 3`), aqua slit (`du=2`)!
        if abs(float(wn[0])) > 0.45:
            rim = (du == 0) | (du == w - 1) | (dv == 0) | (dv == h - 1)
            slit = (du == w // 2) & (~rim)
            bars = (~rim) & (~slit)
            col = np.broadcast_to(rgb('#6EB8EA'), (h, w, 3)).copy()
            col = np.where(slit[..., None], rgb('#7AE2FA'), col)
            col = np.where(bars[..., None], rgb('#F4FFFF'), col)
            return col
        return mix(c_ice_mid, c_ice_pale, np.full((h, w), 0.78))

    if mat == 'gecko_dorsal_crystal':
        meta = cube.get('meta') or {}
        t0 = float(meta.get('crysT0', 0.0))
        t1 = float(meta.get('crysT1', 1.0))
        prog = t0 + (1.0 - lv) * (t1 - t0) if face_name in ('north', 'south', 'east', 'west') else np.full((h, w), t1 if face_name == 'up' else t0, dtype=np.float32)
        col = mix(rgb('#66D0FA'), rgb('#B6F2FF'), np.clip(prog * 1.6, 0.0, 1.0))
        col = mix(col, rgb('#F4FFFF'), np.clip((prog - 0.40) * 2.0, 0.0, 1.0))
        if w >= 2 and h >= 2:
            bevel = (dv == 0) | (du == 0)
            col = np.where(bevel[..., None], rgb('#FAFFFF'), col)
        return col

    if mat == 'gecko_scales_body':
        # Crisp Artisanal `(du, dv)` Glowing Aqua-Cyan Diamond Scales on `7x18` Flanks (`orig_09_gecko.png`)!
        if abs(float(wn[0])) > 0.55:
            col = mix(c_ice_mid, c_ice_light, np.clip(0.50 + 0.35 * top_shade + grain * 0.04, 0.0, 1.0))
            # Staggered diamond centers every 4 columns across `du = 0..17` and `dv = 2, 4`:
            best_d = np.full((h, w), 99.0, dtype=np.float32)
            for cy, u_start in ((2, 2), (4, 4), (2, 6), (4, 8), (2, 10), (4, 12), (2, 14), (4, 16)):
                md = np.abs(du - u_start) + np.abs(dv - cy)
                best_d = np.minimum(best_d, md)

            ring_1 = (best_d == 1)
            core_0 = (best_d == 0)
            lattice = (best_d == 2)
            col = np.where(lattice[..., None], rgb('#68B4E8'), col)
            col = np.where(ring_1[..., None], c_cyan_glow, col)
            col = np.where(core_0[..., None], c_cyan_white, col)
            border_tb = (dv == 0) | (dv == h - 1)
            col = np.where(border_tb[..., None], c_ice_pale, col)
            return col

        col = mix(c_ice_light, c_ice_pale, np.clip(0.50 + 0.40 * top_shade + grain * 0.05, 0.0, 1.0))
        if w >= 3 and h >= 3:
            col = np.where((edge_px == 0)[..., None], mix(col, c_cyan_white, np.full((h, w), 0.65)), col)
        return col

    if mat == 'gecko_frill':
        chk = ((du + dv) % 2) == 0
        col = np.where(chk[..., None], c_cyan_glow, c_ice_light)
        if w >= 3 and h >= 3:
            col = np.where((edge_px == 0)[..., None], c_ice_pale, col)
        return col

    facet_n = fbm3(wx, wy, wz, 0.32, seed=911)
    inner = mix(c_ice_mid, c_ice_light, np.clip(0.44 + 0.44 * top_shade + (facet_n - 0.5) * 0.16 + grain * 0.05, 0.0, 1.0))
    inner = mix(inner, c_ice_pale, np.clip((top_shade - 0.46) * 1.6, 0.0, 0.74))

    if w >= 3 and h >= 3:
        hi_rim = (dv == 0) | (du == 0)
        lo_rim = (dv == h - 1) | (du == w - 1)
        inner = np.where(hi_rim[..., None], mix(inner, c_cyan_white, np.full((h, w), 0.72)), inner)
        inner = np.where(lo_rim[..., None], mix(inner, c_ice_deep, np.full((h, w), 0.30)), inner)

    if mat == 'gecko_head':
        snout_glow = (wy > 12.0) & (np.abs(wx) < 2.8) & (wz < -13.5)
        inner = np.where(snout_glow[..., None], mix(inner, c_cyan_white, np.full((h, w), 0.70)), inner)
        jaw_line = (wz < -13.2) & (np.abs(wy - 10.1) < 0.35)
        nostril = (wz < -20.2) & (np.abs(wy - 11.6) < 0.42) & (np.abs(np.abs(wx) - 1.6) < 0.42)
        inner = np.where((jaw_line | nostril)[..., None], c_ice_deep, inner)

    if mat == 'gecko_toe':
        is_tip = (lv < 0.30) if (face_name in ('up', 'down')) else (lu > 0.70)
        inner = np.where(np.broadcast_to(is_tip, (h, w))[..., None], mix(inner, c_cyan_white, np.full((h, w), 0.80)), inner)

    return inner
