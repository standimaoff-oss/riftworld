#!/usr/bin/env python3
"""
3D World-Space + Crisp Artisanal `(du, dv)` Pixel-Art Painters for Batch 05:
  - 13: `reef_seahorse`      (`Рифовый морской конёк`)
  - 14: `ghost_squid`        (`Глубоководный кальмар-призрак`)
  - 15: `silver_sea_serpent` (`Серебряный морской змей`)
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
# 13 — REEF SEAHORSE (`reef_seahorse`)
# ============================================================================

def paint_seahorse_face(cube, face_name, w, h, wx, wy, wz, wn, lu, lv, du, dv):
    mat = cube['mat']
    grain = (hash_noise(du, dv, seed=hash(cube['name'] + face_name) & 0xFFFF) - 0.5)
    top_shade = 0.5 + 0.5 * float(wn[1])

    c_teal_deep = rgb('#4D998B')
    c_teal_mid = rgb('#6AB9A8')
    c_teal_hi = rgb('#8BD4C4')
    c_coral_deep = rgb('#CE623C')
    c_coral_mid = rgb('#EB8055')
    c_coral_hi = rgb('#F79E76')
    c_pink_belly = rgb('#F3AAB9')
    c_cream_belly = rgb('#FBEED4')

    if mat == 'seahorse_bubble':
        edge = (du == 0) | (du == w - 1) | (dv == 0) | (dv == h - 1)
        col = np.broadcast_to(rgb('#F6FBFF'), (h, w, 3)).copy()
        return np.where(edge[..., None], rgb('#C5DFF2'), col)

    if mat == 'seahorse_eye':
        # Protruding Turret Eye: Butter-yellow upper/lower eyelids (`#DFCA6A`) framing dark-plum pupil (`#261A2E`)!
        if abs(float(wn[0])) > 0.45 or float(wn[2]) < -0.45:
            col = np.broadcast_to(rgb('#DEC868'), (h, w, 3)).copy()
            pupil = (dv == h // 2) & (du <= max(0, w - 2)) if abs(float(wn[0])) > 0.45 else (dv == h // 2)
            col = np.where(pupil[..., None], rgb('#24182C'), col)
            return col
        return mix(rgb('#C6B056'), rgb('#E6D47A'), np.full((h, w), top_shade))

    if mat == 'seahorse_trunk_flank':
        # 2-Tone Seahorse Flank (`orig_13_seahorse.png`): Coral-orange ventral keel (`wz < -0.2`) + Seafoam-teal dorsal armor (`wz >= -0.2`)!
        t_n = hash_noise(du, dv, seed=1307 + (hash(cube['name'] + face_name) & 0xFF))
        coral_c = mix(c_coral_deep, c_coral_mid, np.clip(0.38 + 0.46 * top_shade + (t_n - 0.5) * 0.18, 0.0, 1.0))
        coral_c = mix(coral_c, c_coral_hi, np.clip((top_shade - 0.45) * 1.4, 0.0, 0.70))
        teal_c = mix(c_teal_deep, c_teal_mid, np.clip(0.36 + 0.46 * top_shade + (t_n - 0.5) * 0.22, 0.0, 1.0))
        teal_c = mix(teal_c, c_teal_hi, np.clip((top_shade - 0.42) * 1.5, 0.0, 0.75))
        band_sep = ((np.floor(wy * 0.72).astype(np.int64) % 2) == 0)
        teal_c = np.where(band_sep[..., None], teal_c, teal_c * 0.90)
        is_coral_keel = wz < -0.3
        return np.where(is_coral_keel[..., None], coral_c, teal_c)

    if mat in ('seahorse_snout_tip', 'seahorse_coral'):
        p_n = hash_noise(du, dv, seed=1301 + (hash(cube['name'] + face_name) & 0xFF))
        col = mix(c_coral_deep, c_coral_mid, np.clip(0.36 + 0.46 * top_shade + (p_n - 0.5) * 0.20, 0.0, 1.0))
        col = mix(col, rgb('#FAB694'), np.clip((top_shade - 0.42) * 1.6, 0.0, 0.80))
        return col

    if mat in ('seahorse_coral_crest', 'seahorse_antler'):
        # Coral-orange base grading to pastel-pink polyp tips!
        tip_t = 1.0 - lv
        col = mix(rgb('#DC6E48'), rgb('#EF96B0'), np.clip((tip_t - 0.35) * 1.8, 0.0, 1.0))
        return col * (0.90 + 0.16 * top_shade)

    if mat == 'seahorse_fin':
        # Translucent Cream-and-Blush-Pink Rayed Seahorse Fin (`orig_13_seahorse.png`)!
        ray = ((dv % 2) == 0)
        outer = (du >= w - 2) if (face_name in ('east', 'west') and w >= 4) else (lv < 0.30)
        col = np.where(ray[..., None], rgb('#F0BEB0'), rgb('#F7DDD0'))
        col = np.where(np.broadcast_to(outer, (h, w))[..., None], c_cream_belly, col)
        return col * (0.92 + 0.12 * top_shade)

    if mat == 'seahorse_belly':
        # Alternating Pastel-Pink & Warm-Cream Horizontal Seahorse Belly Plates Framed by Coral-Orange Keels!
        band = (np.floor(wy * 0.72).astype(np.int64) % 2) == 0
        plate = np.where(band[..., None], c_pink_belly, c_cream_belly)
        if face_name == 'north' and w >= 5:
            keel = (du == 0) | (du == w - 1)
            plate = np.where(keel[..., None], c_coral_mid, plate)
        elif abs(float(wn[0])) > 0.55:
            plate = mix(c_coral_deep, c_coral_mid, np.clip(0.42 + 0.44 * top_shade + grain * 0.12, 0.0, 1.0))
        return plate

    if mat == 'seahorse_tail_dual':
        # Prehensile Spiral Tail: Seafoam-Teal Outer Armor + Warm Coral-Orange Inner Curl!
        meta = cube.get('meta') or {}
        orange_dir = meta.get('orangeSide', 'north')
        is_orange = (face_name == orange_dir)
        if abs(float(wn[0])) > 0.55:
            # Lateral side of spiral bar: split inner half coral-orange, outer half seafoam-teal!
            # Check distance from spiral center `(wy=5.4, wz=0.4)`:
            r_in = np.hypot(wy - 5.6, wz - 0.3)
            is_inner = r_in < 3.0
            teal_c = mix(c_teal_deep, c_teal_mid, np.clip(0.40 + 0.45 * top_shade + grain * 0.12, 0.0, 1.0))
            oran_c = mix(c_coral_deep, c_coral_mid, np.clip(0.40 + 0.45 * top_shade + grain * 0.12, 0.0, 1.0))
            return np.where(is_inner[..., None], oran_c, teal_c)
        if is_orange:
            return mix(c_coral_deep, c_coral_hi, np.clip(0.42 + 0.44 * top_shade + grain * 0.12, 0.0, 1.0))
        t_n = hash_noise(du, dv, seed=1319 + (hash(cube['name'] + face_name) & 0xFF))
        return mix(c_teal_deep, c_teal_hi, np.clip(0.38 + 0.46 * top_shade + (t_n - 0.5) * 0.20, 0.0, 1.0))

    # Seafoam-Teal Armored Seahorse Plates (`seahorse_teal`)!
    t_n = hash_noise(du, dv, seed=1327 + (hash(cube['name'] + face_name) & 0xFF))
    col = mix(c_teal_deep, c_teal_mid, np.clip(0.36 + 0.46 * top_shade + (t_n - 0.5) * 0.24, 0.0, 1.0))
    col = mix(col, c_teal_hi, np.clip((top_shade - 0.42) * 1.55, 0.0, 0.80))
    if w >= 3 and h >= 3:
        hi = (dv == 0)
        lo = (dv == h - 1)
        col = np.where(hi[..., None], mix(col, c_teal_hi, np.full((h, w), 0.45)), col)
        col = np.where(lo[..., None], mix(col, c_teal_deep, np.full((h, w), 0.35)), col)
    return col


# ============================================================================
# 14 — DEEP-SEA GHOST SQUID (`ghost_squid`)
# ============================================================================

def paint_squid_face(cube, face_name, w, h, wx, wy, wz, wn, lu, lv, du, dv):
    mat = cube['mat']
    edge_px = np.minimum(np.minimum(du, w - 1 - du), np.minimum(dv, h - 1 - dv))
    top_shade = 0.5 + 0.5 * float(wn[1])
    p_n = hash_noise(du, dv, seed=1409 + (hash(cube['name'] + face_name) & 0xFF))

    c_abyss_dark = rgb('#0B1022')
    c_abyss_mid = rgb('#162140')
    c_abyss_mottle = rgb('#213058')
    c_rim_ghost = rgb('#6C84BA')
    c_cyan_dot = rgb('#62FAFF')
    c_cyan_white = rgb('#C8FFFF')
    c_cyan_halo = rgb('#1E587A')

    if mat == 'squid_fin':
        # Translucent Ghostly Periwinkle-Slate Stabilizer Fins (`orig_14_squid.png`)!
        col = mix(rgb('#4D6394'), rgb('#7F98CA'), np.clip(0.38 + 0.45 * top_shade + (p_n - 0.5) * 0.18, 0.0, 1.0))
        if w >= 2 and h >= 2:
            rim = (edge_px == 0)
            col = np.where(rim[..., None], rgb('#AEBEF2'), col)
        return col

    if mat == 'squid_eye':
        # Large Luminous Pale-Lavender / Ghost-Lilac Abyssal Eye (`w=5, h=5` on lateral face)!
        if abs(float(wn[0])) > 0.45:
            cx = (w - 1) / 2.0
            cy = (h - 1) / 2.0
            dist = np.hypot(du - cx, dv - cy)
            col = mix(c_abyss_dark, c_abyss_mid, np.full((h, w), 0.5))
            in_iris = dist <= 2.1
            in_core = (np.abs(du - cx) <= 0.6) & (np.abs(dv - cy) <= 1.4)
            col = np.where(in_iris[..., None], rgb('#B8B6F0'), col)
            col = np.where(in_core[..., None], rgb('#F0EEFF'), col)
            if w >= 3 and h >= 3:
                col = np.where((edge_px == 0)[..., None], rgb('#46598C'), col)
            return col
        return c_abyss_mid

    # Base Deep-Abyssal Midnight-Indigo Skin with Crisp Ghostly Rim Outlines!
    col = mix(c_abyss_dark, c_abyss_mid, np.clip(0.32 + 0.38 * top_shade + (p_n - 0.5) * 0.34, 0.0, 1.0))
    col = np.where((p_n > 0.78)[..., None], mix(col, c_abyss_mottle, np.full((h, w), 0.55)), col)

    # Signature Subtle Ghostly Periwinkle Silhouette Rim Outline (`orig_14_squid.png`!)
    if w >= 3 and h >= 3:
        is_rim = (edge_px == 0)
        col = np.where(is_rim[..., None], mix(col, c_rim_ghost, np.full((h, w), 0.52)), col)
    elif w >= 2 and h >= 4:
        is_rim = (du == 0)
        col = np.where(is_rim[..., None], mix(col, c_rim_ghost, np.full((h, w), 0.45)), col)

    # Bioluminescent Cyan Photophore Rows!
    if mat == 'squid_mantle':
        # `mantle` segments built with `addSegmentBox`: local `dv` runs along length (`h`), `du` across width (`w`)!
        if w >= 5 and h >= 6:
            mid_u = w // 2
            is_dot = (du == mid_u) & ((dv % 3) == 1) & (dv >= 1) & (dv <= h - 2)
            is_halo = (np.abs(du - mid_u) <= 1) & ((dv % 3) == 1) & (~is_dot) & (edge_px >= 1)
            col = np.where(is_halo[..., None], mix(col, c_cyan_halo, np.full((h, w), 0.75)), col)
            col = np.where(is_dot[..., None], mix(c_cyan_dot, c_cyan_white, np.full((h, w), 0.45)), col)

    elif mat == 'squid_head':
        # Twin Dorsal Rows of Glowing Cyan Photophores on Head Collar (`up` / dorsal face)!
        if float(wn[1]) > 0.35 and w >= 7 and h >= 5:
            row_u = (du == 2) | (du == w - 3)
            is_dot = row_u & ((dv % 2) == 1) & (dv >= 1) & (dv <= h - 2)
            col = np.where(is_dot[..., None], c_cyan_dot, col)

    elif mat in ('squid_arm_glow', 'squid_tentacle', 'squid_club'):
        # Spaced Glowing Cyan Photophore Dots along outer faces of squid tentacles (`orig_14_squid.png`)!
        if mat == 'squid_club' and h >= 4 and w >= 2:
            is_dot = (du == w // 2) & (dv == h // 2)
            col = np.where(is_dot[..., None], c_cyan_dot, col)
        elif h >= 6 and w >= 2:
            is_dot = (du == w // 2) & ((dv % 6) == 2) & (dv >= 1) & (dv <= h - 2)
            col = np.where(is_dot[..., None], c_cyan_dot, col)

    return col


# ============================================================================
# 15 — SILVER SEA SERPENT (`silver_sea_serpent`)
# ============================================================================

def paint_serpent_face(cube, face_name, w, h, wx, wy, wz, wn, lu, lv, du, dv):
    mat = cube['mat']
    grain = (hash_noise(du, dv, seed=hash(cube['name'] + face_name) & 0xFFFF) - 0.5)
    top_shade = 0.5 + 0.5 * float(wn[1])

    c_silver_dark = rgb('#788E9B')
    c_silver_mid = rgb('#9EB2BE')
    c_silver_light = rgb('#C4D5DF')
    c_silver_bright = rgb('#E6F1F6')
    c_teal_stripe = rgb('#21606A')
    c_teal_mid = rgb('#357C87')

    if mat == 'serpent_eye':
        # Glowing Pale-Ice-Aqua Sea-Serpent Eye under Slate Brow!
        if abs(float(wn[0])) > 0.45 or float(wn[2]) < -0.45:
            brow = (dv == 0)
            glow = mix(rgb('#BAFAF2'), rgb('#F6FFFE'), np.clip(1.0 - np.abs(lu - 0.5), 0.0, 1.0))
            return np.where(brow[..., None], rgb('#5E7684'), glow)
        return c_silver_dark

    if mat in ('serpent_fin_crest', 'serpent_fin'):
        # Scalloped Translucent Pale-Seafoam-Silver Dorsal Fin Crest (`orig_15_serpent.png`)!
        f_n = hash_noise(du, dv, seed=1511 + (hash(cube['name']) & 0xFF))
        ray_band = ((dv % 2) == 0)
        base_f = np.where(ray_band[..., None], rgb('#C4E2DE'), rgb('#9EC0BC'))
        col = mix(base_f, rgb('#EAFAF7'), np.clip(0.25 + (1.0 - lv) * 0.45 + (f_n - 0.5) * 0.20, 0.0, 1.0))
        if h >= 2:
            tip = (dv == 0) | ((du == 0) & (face_name == 'east')) | ((du == w - 1) & (face_name == 'west'))
            col = np.where(tip[..., None], rgb('#F2FFFD'), col)
        return col

    if mat == 'serpent_horn':
        return mix(c_silver_mid, c_silver_bright, np.clip(0.45 + (1.0 - lv) * 0.45 + top_shade * 0.20, 0.0, 1.0))

    if mat == 'serpent_flipper':
        s_n = hash_noise(du, dv, seed=1523 + (hash(cube['name'] + face_name) & 0xFF))
        col = mix(c_silver_dark, c_silver_light, np.clip(0.40 + 0.42 * top_shade + (s_n - 0.5) * 0.24, 0.0, 1.0))
        return mix(col, rgb('#DCECEB'), np.clip((1.0 - lv) * 0.55, 0.0, 0.65))

    s_n = hash_noise(du, dv, seed=1531 + (hash(cube['name'] + face_name) & 0xFF))
    col = mix(c_silver_dark, c_silver_mid, np.clip(0.36 + 0.42 * top_shade + (s_n - 0.5) * 0.28, 0.0, 1.0))
    col = mix(col, c_silver_light, np.clip((top_shade - 0.36) * 1.45, 0.0, 0.85))
    col = mix(col, c_silver_bright, np.clip((top_shade - 0.68) * 2.2, 0.0, 0.78))

    if mat in ('serpent_head', 'serpent_snout'):
        if float(wn[1]) > 0.55:
            col = mix(c_silver_light, c_silver_bright, np.clip(0.55 + (s_n - 0.5) * 0.22, 0.0, 1.0))
        if mat == 'serpent_snout' and face_name == 'north' and w >= 5 and h >= 3:
            nostril = (dv == 1) & ((du == 1) | (du == w - 2))
            col = np.where(nostril[..., None], rgb('#465864'), col)
        elif mat == 'serpent_snout' and face_name in ('east', 'west') and h >= 3:
            mouth = (dv == h - 1)
            col = np.where(mouth[..., None], rgb('#546874'), col)
        return col

    if mat in ('serpent_body_seg', 'serpent_body'):
        # `addSegmentBox` with `upHint = [0, 1, 0]` (or `[0, 0, -1]` on vertical neck):
        # Local `z` (`north`/`south`) is dorsal/ventral! Wait: let's check Basis in `builder_lib.mjs`!
        # `yAxis = normalize(pEnd - pStart)` (along spine `+Z`!).
        # `xAxis = normalize(yAxis × upHint)`: for `yAxis ~ (0, 0, 1)` and `upHint = (0, 1, 0)`,
        # `xAxis = (0, 0, 1) × (0, 1, 0) = (-1, 0, 0)` (lateral!) and `zAxis = xAxis × yAxis = (0, 1, 0)` (`+Z` local = `south` = DORSAL TOP, `-Z` local = `north` = VENTRAL BELLY!!)!
        if face_name == 'south':
            # Dorsal top of serpent segment: bright frost-silver center flanked by twin Deep-Ocean-Teal stripes along left & right edges (`du == 0, 1` & `du == w-2, w-1`)!
            col = mix(c_silver_light, c_silver_bright, np.clip(0.55 + (s_n - 0.5) * 0.22, 0.0, 1.0))
            if w >= 5:
                teal_band = (du == 1) | (du == w - 2) | (((du == 0) | (du == w - 1)) & (s_n > 0.35))
                col = np.where(teal_band[..., None], mix(c_teal_stripe, c_teal_mid, s_n), col)
            elif w >= 3:
                teal_band = (du == 0) | (du == w - 1)
                col = np.where(teal_band[..., None], c_teal_stripe, col)
            return col

        if face_name == 'north':
            # Ventral ribbed silver-cream belly scutes!
            scute = (dv % 2) == 0
            belly = mix(rgb('#B6C8D2'), rgb('#DCE8EE'), np.clip(0.5 + (s_n - 0.5) * 0.16, 0.0, 1.0))
            return np.where(scute[..., None], belly, mix(belly, c_silver_mid, np.full((h, w), 0.32)))

        if face_name in ('east', 'west'):
            # Lateral flanks! Notice: on `east`, `lz = fz1 - lu * sz` (`lu = 0 -> du = 0` is `+Z` local = DORSAL TOP!);
            # on `west`, `lz = fz0 + lu * sz` (`lu = 1 -> du = w - 1` is `+Z` local = DORSAL TOP!)!
            dorsal_dist = du if face_name == 'east' else (w - 1 - du)
            is_dorsal_teal = (dorsal_dist == 0) | ((dorsal_dist == 1) & (s_n > 0.42))
            col = np.where(is_dorsal_teal[..., None], mix(c_teal_stripe, c_teal_mid, s_n), col)
            return col

    return col
