#!/usr/bin/env python3
"""
3D World-Space + Crisp Artisanal `(du, dv)` Pixel-Art Painters for Batch 04 (V2 Masterpiece):
  - 10: `faceted_owl`       (`Гранёная сова`)
  - 11: `bone_raven`        (`Костяной ворон`)
  - 12: `coral_hermit_crab` (`Коралловый рак-отшельник`)
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


# ============================================================================
# 10 — FACETED OWL (`faceted_owl`)
# ============================================================================

def paint_owl_face(cube, face_name, w, h, wx, wy, wz, wn, lu, lv, du, dv):
    mat = cube['mat']
    grain = (hash_noise(du, dv, seed=hash(cube['name'] + face_name) & 0xFFFF) - 0.5)
    top_shade = 0.5 + 0.5 * float(wn[1])
    edge_px = np.minimum(np.minimum(du, w - 1 - du), np.minimum(dv, h - 1 - dv))

    c_ice_white = rgb('#F2FCFF')
    c_ice_pale = rgb('#CFEBFB')
    c_ice_mid = rgb('#94C7EC')
    c_ice_deep = rgb('#5B94CB')
    c_cyan_vivid = rgb('#20D4E2')
    c_cyan_glow = rgb('#92FCFF')
    c_cobalt_deep = rgb('#154894')
    c_cobalt_mid = rgb('#2C72C6')

    if mat == 'owl_eye':
        # Large Round Luminous Glowing Cyan-Turquoise Owl Eye (`w=4, h=4` on `north`)!
        if face_name == 'north':
            cx = (w - 1) / 2.0
            cy = (h - 1) / 2.0
            dist = np.hypot(du - cx, dv - cy)
            col = mix(rgb('#1AD4EC'), rgb('#F2FFFF'), np.clip(1.0 - dist / 1.85, 0.0, 1.0))
            is_corner = ((du == 0) | (du == w - 1)) & ((dv == 0) | (dv == h - 1))
            col = np.where(is_corner[..., None], rgb('#1E5696'), col)
            return col
        return rgb('#1E5696')

    if mat == 'owl_beak':
        ridge = np.abs(du - (w - 1) / 2.0) < 0.6
        col = mix(rgb('#0C2F6C'), c_cobalt_mid, np.clip(0.32 + 0.48 * top_shade + (1.0 - lv) * 0.25, 0.0, 1.0))
        col = np.where(ridge[..., None], mix(col, rgb('#72BEF8'), np.full((h, w), 0.58)), col)
        return col

    if mat == 'owl_face_head':
        # Artisanal Rounded Owl Facial Discs on `north` face (`w = 13, h = 10`)!
        if face_name == 'north':
            d_left = np.hypot((du - 3.3) * 0.95, (dv - 5.2) * 1.0)
            d_right = np.hypot((du - (w - 1 - 3.3)) * 0.95, (dv - 5.2) * 1.0)
            d_disc = np.minimum(d_left, d_right)

            col = mix(c_ice_mid, c_ice_pale, np.clip(0.48 + (1.0 - lv) * 0.44 + grain * 0.06, 0.0, 1.0))
            in_pale_disc = d_disc < 3.6
            col = np.where(in_pale_disc[..., None], mix(c_ice_pale, c_ice_white, np.clip(1.0 - d_disc / 3.6, 0.0, 1.0) * 0.88), col)
            disc_rim = (d_disc >= 3.45) & (d_disc <= 4.45) & (dv >= 2)
            col = np.where(disc_rim[..., None], rgb('#2E66A4'), col)
            eye_socket = d_disc < 2.1
            col = np.where(eye_socket[..., None], rgb('#194A88'), col)
            return col

        facet = hash_noise(du, dv, seed=1009 + (hash(face_name) & 0xFF))
        col = mix(c_ice_mid, c_ice_pale, np.clip(0.38 + 0.42 * top_shade + (facet - 0.5) * 0.24, 0.0, 1.0))
        col = mix(col, c_ice_white, np.clip((top_shade - 0.50) * 1.6, 0.0, 0.76))
        return col

    if mat == 'owl_crest_lavender':
        prog = 1.0 - lv
        col = mix(rgb('#7AA4EA'), rgb('#D4E8FF'), np.clip(0.32 + prog * 0.65 + top_shade * 0.25, 0.0, 1.0))
        if w >= 2 and h >= 2:
            bevel = (dv == 0) | (du == 0)
            col = np.where(bevel[..., None], rgb('#F6FCFF'), col)
        return col

    if mat == 'owl_diamond_breast':
        # 45-degree rotated diamond crystal feather! World `wy` decreases toward the sharp bottom diamond tip!
        meta = cube.get('meta') or {}
        cy = float(meta.get('cy', 19.5))
        tip_prog = np.clip((cy + 1.8 - wy) / 3.8, 0.0, 1.0)
        col = mix(c_ice_white, c_cyan_vivid, np.clip(tip_prog * 1.25, 0.0, 1.0))
        col = mix(col, rgb('#15A8BC'), np.clip((tip_prog - 0.72) * 2.2, 0.0, 0.65))
        if w >= 3 and h >= 3:
            rim = (edge_px == 0)
            rim_col = np.where((wy > cy)[..., None], c_ice_white, c_cyan_glow)
            col = np.where(rim[..., None], mix(col, rim_col, np.full((h, w), 0.65)), col)
        return col

    if mat == 'owl_crystal_cyan':
        meta = cube.get('meta') or {}
        mode = meta.get('tipDir', 'down')
        tip_t = lv if mode == 'down' else (1.0 - lv)
        col = mix(rgb('#9CECF8'), c_cyan_vivid, np.clip(0.25 + tip_t * 0.95, 0.0, 1.0))
        col = mix(col, rgb('#149FB4'), np.clip((tip_t - 0.62) * 1.8, 0.0, 0.70))
        if w >= 2 and h >= 2:
            hi_edge = (edge_px == 0) & ((du == 0) | (dv == 0))
            lo_edge = (edge_px == 0) & ((du == w - 1) | (dv == h - 1))
            col = np.where(hi_edge[..., None], mix(col, rgb('#DCFFFF'), np.full((h, w), 0.78)), col)
            col = np.where(lo_edge[..., None], mix(col, rgb('#14849E'), np.full((h, w), 0.45)), col)
        return col

    if mat == 'owl_crystal_cobalt':
        col = mix(c_cobalt_deep, c_cobalt_mid, np.clip(0.35 + 0.45 * top_shade + (1.0 - lv) * 0.25 + grain * 0.08, 0.0, 1.0))
        if w >= 2 and h >= 2:
            frost_edge = (du == 0) | (dv == h - 1)
            col = np.where(frost_edge[..., None], mix(col, c_ice_white, np.full((h, w), 0.78)), col)
        return col

    if mat == 'owl_claw_dark':
        return mix(rgb('#0D306A'), rgb('#2662B2'), np.clip(0.35 + 0.45 * top_shade + grain * 0.08, 0.0, 1.0))

    if mat == 'owl_tarsus':
        chk = ((du + dv) % 2) == 0
        col = mix(rgb('#467CB8'), rgb('#86BCE8'), np.clip(0.38 + 0.42 * top_shade + grain * 0.08, 0.0, 1.0))
        return np.where(chk[..., None], col, mix(col, c_cobalt_deep, np.full((h, w), 0.22)))

    facet_n = hash_noise(du, dv, seed=1031 + (hash(cube['name'] + face_name) & 0xFF))
    col = mix(c_ice_mid, c_ice_pale, np.clip(0.38 + 0.42 * top_shade + (facet_n - 0.5) * 0.22, 0.0, 1.0))
    col = mix(col, c_ice_white, np.clip((top_shade - 0.34) * 1.40 + (1.0 - lv) * 0.22, 0.0, 0.88))
    if w >= 3 and h >= 3:
        hi_rim = (dv == 0) | (du == 0)
        lo_rim = (dv == h - 1)
        col = np.where(hi_rim[..., None], mix(col, c_ice_white, np.full((h, w), 0.65)), col)
        col = np.where(lo_rim[..., None], mix(col, c_ice_deep, np.full((h, w), 0.28)), col)
    return col


# ============================================================================
# 11 — BONE RAVEN (`bone_raven`)
# ============================================================================

def paint_raven_face(cube, face_name, w, h, wx, wy, wz, wn, lu, lv, du, dv):
    mat = cube['mat']
    grain = (hash_noise(du, dv, seed=hash(cube['name'] + face_name) & 0xFFFF) - 0.5)
    top_shade = 0.5 + 0.5 * float(wn[1])

    c_bone_shadow = rgb('#ABA38B')
    c_bone_mid = rgb('#CFC8B0')
    c_bone_light = rgb('#E6E1CC')
    c_bone_hi = rgb('#F3EFE0')

    if mat == 'raven_eye_ember':
        # Deep Blackened Eye Socket (`w=4, h=3` on `east`/`west`) with Smoldering Orange Ember Core!
        if abs(float(wn[0])) > 0.45:
            rim = (du == 0) | (du == w - 1) | (dv == 0) | (dv == h - 1)
            hot = (~rim) & (du == w // 2)
            warm = (~rim) & (~hot)
            col = np.broadcast_to(rgb('#171210'), (h, w, 3)).copy()
            col = np.where(warm[..., None], rgb('#9C4316'), col)
            col = np.where(hot[..., None], rgb('#EA7A26'), col)
            return col
        return rgb('#171210')

    if mat == 'raven_nostril':
        return np.broadcast_to(rgb('#151311'), (h, w, 3)).copy()

    if mat == 'raven_beak':
        col = mix(c_bone_mid, c_bone_light, np.clip(0.44 + 0.44 * top_shade + grain * 0.08, 0.0, 1.0))
        col = mix(col, c_bone_hi, np.clip((top_shade - 0.52) * 1.6, 0.0, 0.68))
        # Crisp 2-px dark elongated corvid nostril slit near base of upper beak on lateral faces!
        if abs(float(wn[0])) > 0.55:
            nostril = (wz > -16.0) & (wz < -13.6) & (du >= 1) & (du <= max(1, w - 2))
            col = np.where(nostril[..., None], rgb('#181512'), col)
        return col

    if mat == 'raven_feather_dark':
        barb_n = hash_noise(du, dv, seed=1117 + (hash(cube['name'] + face_name) & 0xFF))
        t = np.clip(0.30 + 0.34 * top_shade + (barb_n - 0.5) * 0.36 + (1.0 - lv) * 0.12, 0.0, 1.0)
        col = mix(rgb('#141619'), rgb('#2A2E35'), np.clip(t * 1.5, 0.0, 1.0))
        col = mix(col, rgb('#3B4049'), np.clip((t - 0.62) * 2.0, 0.0, 0.75))
        if w >= 2 and h >= 3:
            shaft = (du == 0)
            col = np.where(shaft[..., None], mix(col, rgb('#0E1012'), np.full((h, w), 0.55)), col)
        return col

    b_noise = hash_noise(du, dv, seed=1129 + (hash(cube['name'] + face_name) & 0xFF))
    col = mix(c_bone_shadow, c_bone_mid, np.clip(0.36 + 0.46 * top_shade + (b_noise - 0.5) * 0.20, 0.0, 1.0))
    col = mix(col, c_bone_light, np.clip((top_shade - 0.36) * 1.45, 0.0, 0.88))
    if w >= 2 and h >= 2:
        hi = (dv == 0) | (du == 0)
        lo = (dv == h - 1)
        col = np.where(hi[..., None], mix(col, c_bone_hi, np.full((h, w), 0.48)), col)
        col = np.where(lo[..., None], mix(col, c_bone_shadow, np.full((h, w), 0.32)), col)
    return col


# ============================================================================
# 12 — CORAL HERMIT CRAB (`coral_hermit_crab`)
# ============================================================================

def paint_crab_face(cube, face_name, w, h, wx, wy, wz, wn, lu, lv, du, dv):
    mat = cube['mat']
    grain = (hash_noise(du, dv, seed=hash(cube['name'] + face_name) & 0xFFFF) - 0.5)
    top_shade = 0.5 + 0.5 * float(wn[1])

    c_coral_deep = rgb('#CE5248')
    c_coral_mid = rgb('#ED7468')
    c_coral_hi = rgb('#FA988A')
    c_orange_deep = rgb('#C85E20')
    c_orange_mid = rgb('#ED8B36')
    c_orange_hi = rgb('#F9A84E')

    if mat == 'crab_eye':
        # Single Crisp `1x1` Pure White Specular Corner Glint strictly on `north` face (`orig_12_crab.png`!)
        col = mix(rgb('#221516'), rgb('#342224'), np.clip(0.35 + top_shade * 0.35, 0.0, 1.0))
        if face_name == 'north':
            glint = (du == 0) & (dv == 0)
            col = np.where(glint[..., None], rgb('#FFFFFF'), col)
        return col

    if mat == 'crab_orange':
        p_n = hash_noise(du, dv, seed=1201 + (hash(cube['name'] + face_name) & 0xFF))
        col = mix(c_orange_deep, c_orange_mid, np.clip(0.36 + 0.44 * top_shade + (p_n - 0.5) * 0.24, 0.0, 1.0))
        col = mix(col, c_orange_hi, np.clip((top_shade - 0.48) * 1.5, 0.0, 0.72))
        return col

    if mat == 'crab_orange_tip':
        p_n = hash_noise(du, dv, seed=1207 + (hash(cube['name']) & 0xFF))
        return mix(rgb('#AC4A18'), rgb('#DA6E28'), np.clip(0.35 + 0.45 * top_shade + (p_n - 0.5) * 0.22, 0.0, 1.0))

    if mat == 'crab_carapace':
        p_n = hash_noise(du, dv, seed=1213 + (hash(face_name) & 0xFF))
        pink = mix(c_coral_deep, c_coral_mid, np.clip(0.38 + 0.44 * top_shade + (p_n - 0.5) * 0.26, 0.0, 1.0))
        pink = mix(pink, c_coral_hi, np.clip((top_shade - 0.45) * 1.5, 0.0, 0.78))

        orange = mix(c_orange_deep, c_orange_mid, np.clip(0.42 + 0.42 * top_shade + (p_n - 0.5) * 0.24, 0.0, 1.0))
        is_lower_orange = (wy + (p_n - 0.5) * 1.6) < 8.8
        col = np.where(is_lower_orange[..., None], orange, pink)

        if face_name == 'north':
            cx = w // 2
            mouth_t = ((dv == h - 3) & (np.abs(du - cx) <= 1)) | ((dv == h - 2) & (du == cx))
            col = np.where(mouth_t[..., None], c_coral_deep, col)
        return col

    if mat == 'crab_pink':
        p_n = hash_noise(du, dv, seed=1223 + (hash(cube['name'] + face_name) & 0xFF))
        col = mix(c_coral_deep, c_coral_mid, np.clip(0.36 + 0.44 * top_shade + (p_n - 0.5) * 0.28, 0.0, 1.0))
        col = mix(col, c_coral_hi, np.clip((top_shade - 0.42) * 1.55, 0.0, 0.82))
        return col

    if mat == 'crab_shell_cream':
        s_n = hash_noise(du, dv, seed=1231 + (hash(cube['name'] + face_name) & 0xFF))
        c_sand_lo = rgb('#D2C098')
        c_sand_mid = rgb('#EAE0BE')
        c_cream_hi = rgb('#FBF4DC')
        t = np.clip(0.34 + 0.48 * top_shade + (s_n - 0.5) * 0.24, 0.0, 1.0)
        col = mix(c_sand_lo, c_sand_mid, np.clip(t * 1.45, 0.0, 1.0))
        col = mix(col, c_cream_hi, np.clip((t - 0.48) * 1.75, 0.0, 0.92))
        if w >= 4 and h >= 4:
            hi_rim = (dv == 0) | (du == 0)
            col = np.where(hi_rim[..., None], mix(col, c_cream_hi, np.full((h, w), 0.55)), col)
        return col

    if mat == 'crab_coral_pink':
        p_n = hash_noise(du, dv, seed=1237 + (hash(cube['name']) & 0xFF))
        return mix(rgb('#D4566C'), rgb('#FA8FA2'), np.clip(0.38 + 0.46 * top_shade + (p_n - 0.5) * 0.22, 0.0, 1.0))

    if mat == 'crab_coral_mint':
        p_n = hash_noise(du, dv, seed=1249 + (hash(cube['name']) & 0xFF))
        return mix(rgb('#35A694'), rgb('#6ADCC8'), np.clip(0.38 + 0.46 * top_shade + (p_n - 0.5) * 0.22, 0.0, 1.0))

    if mat == 'crab_coral_lavender':
        p_n = hash_noise(du, dv, seed=1259 + (hash(cube['name']) & 0xFF))
        return mix(rgb('#8866CA'), rgb('#C0A2F4'), np.clip(0.38 + 0.46 * top_shade + (p_n - 0.5) * 0.22, 0.0, 1.0))

    if mat == 'crab_anemone_body':
        col = mix(rgb('#5FB29E'), rgb('#8CD8C4'), np.clip(0.40 + 0.45 * top_shade + grain * 0.12, 0.0, 1.0))
        if face_name == 'up':
            cx = (w - 1) / 2.0
            cy = (h - 1) / 2.0
            d = np.hypot(du - cx, dv - cy)
            mouth = d <= 1.35
            inner = d <= 0.75
            col = np.where(mouth[..., None], rgb('#387868'), col)
            col = np.where(inner[..., None], rgb('#20483E'), col)
        return col

    if mat == 'crab_anemone_tentacle':
        return mix(rgb('#78CCB6'), rgb('#B2F0E2'), np.clip(0.45 + 0.45 * top_shade + (1.0 - lv) * 0.20, 0.0, 1.0))

    return np.broadcast_to(c_coral_mid, (h, w, 3)).copy()
