#!/usr/bin/env python3
"""
3D World-Space + Crisp Artisanal `(du, dv)` Pixel-Art Painters for Batch 05 (V5 Masterpiece):
  - 13: `reef_seahorse`      (`Рифовый морской конёк` — без пузырьков, рельефные панцирные щитки)
  - 14: `ghost_squid`        (`Глубоководный кальмар-призрак` — БЕЗ контурных рамок, бархатная абиссальная кожа + фотофоры)
  - 15: `silver_sea_serpent` (`Серебряный морской змей` — детализированная чешуя морского дракона, плавниковые лучи, пасть и клыки)
"""

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
# 13 — REEF SEAHORSE (`reef_seahorse`) — ZERO BUBBLES, SCULPTED REEF ARMOR
# ============================================================================

def paint_seahorse_face(cube, face_name, w, h, wx, wy, wz, wn, lu, lv, du, dv):
    mat = cube['mat']
    grain = (hash_noise(du, dv, seed=hash(cube['name'] + face_name) & 0xFFFF) - 0.5)
    top_shade = 0.5 + 0.5 * float(wn[1])

    c_teal_deep = rgb('#468E80')
    c_teal_mid = rgb('#67B7A6')
    c_teal_hi = rgb('#8CD6C6')
    c_coral_deep = rgb('#CA5E38')
    c_coral_mid = rgb('#EA7E53')
    c_coral_hi = rgb('#F89F78')
    c_pink_belly = rgb('#F3AAB9')
    c_cream_belly = rgb('#FBEED4')

    if mat == 'seahorse_eye':
        if abs(float(wn[0])) > 0.45 or float(wn[2]) < -0.45:
            col = np.broadcast_to(rgb('#DEC868'), (h, w, 3)).copy()
            pupil = (dv == h // 2) & (du <= max(0, w - 2)) if abs(float(wn[0])) > 0.45 else (dv == h // 2)
            col = np.where(pupil[..., None], rgb('#24182C'), col)
            return col
        return mix(rgb('#C6B056'), rgb('#E6D47A'), np.full((h, w), top_shade))

    if mat == 'seahorse_trunk_flank':
        t_n = hash_noise(du, dv, seed=1307 + (hash(cube['name'] + face_name) & 0xFF))
        coral_c = mix(c_coral_deep, c_coral_mid, np.clip(0.38 + 0.46 * top_shade + (t_n - 0.5) * 0.18, 0.0, 1.0))
        coral_c = mix(coral_c, c_coral_hi, np.clip((top_shade - 0.45) * 1.4, 0.0, 0.70))
        teal_c = mix(c_teal_deep, c_teal_mid, np.clip(0.36 + 0.46 * top_shade + (t_n - 0.5) * 0.22, 0.0, 1.0))
        teal_c = mix(teal_c, c_teal_hi, np.clip((top_shade - 0.42) * 1.5, 0.0, 0.75))
        ring_ridge = ((np.floor(wy * 0.72).astype(np.int64) % 2) == 0)
        teal_c = np.where(ring_ridge[..., None], teal_c, teal_c * 0.88)
        coral_c = np.where(ring_ridge[..., None], coral_c, coral_c * 0.90)
        is_coral_keel = wz < -0.3
        return np.where(is_coral_keel[..., None], coral_c, teal_c)

    if mat in ('seahorse_snout_tip', 'seahorse_coral'):
        p_n = hash_noise(du, dv, seed=1301 + (hash(cube['name'] + face_name) & 0xFF))
        col = mix(c_coral_deep, c_coral_mid, np.clip(0.36 + 0.46 * top_shade + (p_n - 0.5) * 0.20, 0.0, 1.0))
        col = mix(col, rgb('#FAB694'), np.clip((top_shade - 0.42) * 1.6, 0.0, 0.80))
        return col

    if mat in ('seahorse_coral_crest', 'seahorse_antler'):
        tip_t = 1.0 - lv
        col = mix(rgb('#DC6E48'), rgb('#F19AB4'), np.clip((tip_t - 0.32) * 1.8, 0.0, 1.0))
        return col * (0.90 + 0.16 * top_shade)

    if mat == 'seahorse_fin':
        ray = ((dv % 2) == 0)
        outer = (du >= w - 2) if (face_name in ('east', 'west') and w >= 4) else (lv < 0.30)
        col = np.where(ray[..., None], rgb('#F0BEB0'), rgb('#F7DDD0'))
        col = np.where(np.broadcast_to(outer, (h, w))[..., None], c_cream_belly, col)
        return col * (0.92 + 0.12 * top_shade)

    if mat == 'seahorse_belly':
        band = (np.floor(wy * 0.72).astype(np.int64) % 2) == 0
        plate = np.where(band[..., None], c_pink_belly, c_cream_belly)
        if face_name == 'north' and w >= 4:
            keel = (du == 0) | (du == w - 1)
            plate = np.where(keel[..., None], mix(plate, c_coral_mid, np.full((h, w), 0.55)), plate)
        elif abs(float(wn[0])) > 0.55:
            plate = mix(c_coral_deep, c_coral_mid, np.clip(0.42 + 0.44 * top_shade + grain * 0.12, 0.0, 1.0))
        return plate

    t_n = hash_noise(du, dv, seed=1327 + (hash(cube['name'] + face_name) & 0xFF))
    col = mix(c_teal_deep, c_teal_mid, np.clip(0.36 + 0.46 * top_shade + (t_n - 0.5) * 0.24, 0.0, 1.0))
    col = mix(col, c_teal_hi, np.clip((top_shade - 0.42) * 1.55, 0.0, 0.80))
    if w >= 3 and h >= 3:
        hi = (dv == 0)
        lo = (dv == h - 1)
        col = np.where(hi[..., None], mix(col, c_teal_hi, np.full((h, w), 0.38)), col)
        col = np.where(lo[..., None], mix(col, c_teal_deep, np.full((h, w), 0.32)), col)
    return col


# ============================================================================
# 14 — DEEP-SEA GHOST SQUID (`ghost_squid`) — ZERO CONTOUR OUTLINES!
# ============================================================================

def paint_squid_face(cube, face_name, w, h, wx, wy, wz, wn, lu, lv, du, dv):
    mat = cube['mat']
    top_shade = 0.5 + 0.5 * float(wn[1])
    p_n = hash_noise(du, dv, seed=1409 + (hash(cube['name'] + face_name) & 0xFF))
    p_n2 = hash_noise(du // 2, dv // 2, seed=1451 + (hash(cube['name']) & 0xFF))

    c_abyss_deep = rgb('#080D1C')
    c_abyss_mid = rgb('#121C38')
    c_abyss_velvet = rgb('#1B2A50')
    c_abyss_sheen = rgb('#263B6B')
    c_cyan_dot = rgb('#5CF8FF')
    c_cyan_white = rgb('#D6FFFF')
    c_cyan_halo = rgb('#19587A')

    if mat == 'squid_fin':
        # Smooth Translucent Pearlescent Abyssal Fin Membrane — NO hard border outline!
        span_t = np.clip(np.abs(wx) / 9.5, 0.0, 1.0)
        col = mix(rgb('#263964'), rgb('#6D88BE'), np.clip(0.25 + 0.55 * span_t + 0.25 * top_shade + (p_n - 0.5) * 0.14, 0.0, 1.0))
        # Subtle internal fin-cartilage striations
        ray = ((dv % 2) == 0)
        col = np.where(ray[..., None], mix(col, rgb('#8AA4D8'), np.full((h, w), 0.22)), col)
        return col

    if mat == 'squid_eye':
        # Large Luminous Pale-Lavender Abyssal Eye — NO rectangular frame outline!
        if abs(float(wn[0])) > 0.45:
            cx = (w - 1) / 2.0
            cy = (h - 1) / 2.0
            dist = np.hypot(du - cx, dv - cy)
            col = mix(c_abyss_deep, c_abyss_mid, np.clip(0.35 + 0.35 * top_shade + (p_n - 0.5) * 0.18, 0.0, 1.0))
            in_outer = dist <= 2.35
            in_iris = dist <= 1.85
            in_core = (np.abs(du - cx) <= 0.65) & (np.abs(dv - cy) <= 1.25)
            col = np.where(in_outer[..., None], rgb('#5A629E'), col)
            col = np.where(in_iris[..., None], rgb('#B8B6F2'), col)
            col = np.where(in_core[..., None], rgb('#F2F0FF'), col)
            return col
        return mix(c_abyss_deep, c_abyss_mid, np.full((h, w), top_shade))

    # Rich Organic Velvet Midnight-Indigo Skin (ZERO cuboid edge outlines!)
    grad = np.clip(0.24 + 0.44 * top_shade + (p_n - 0.5) * 0.22 + (p_n2 - 0.5) * 0.24, 0.0, 1.0)
    col = mix(c_abyss_deep, c_abyss_mid, np.clip(grad * 1.25, 0.0, 1.0))
    col = mix(col, c_abyss_velvet, np.clip((grad - 0.38) * 1.45, 0.0, 0.85))
    col = np.where((p_n > 0.82)[..., None], mix(col, c_abyss_sheen, np.full((h, w), 0.42)), col)

    # Bioluminescent Cyan Photophore Organs with Soft Deep-Sea Glow Halos!
    if mat == 'squid_mantle':
        if w >= 5 and h >= 6:
            mid_u = w // 2
            is_dot = (du == mid_u) & ((dv % 3) == 1) & (dv >= 1) & (dv <= h - 2)
            dist_ph = np.hypot(du - mid_u, ((dv - 1) % 3) - 0.0)
            is_halo = (np.abs(du - mid_u) <= 1) & (np.abs(((dv - 1) % 3)) <= 0) & (~is_dot) & (dv >= 1) & (dv <= h - 2)
            col = np.where(is_halo[..., None], mix(col, c_cyan_halo, np.full((h, w), 0.72)), col)
            col = np.where(is_dot[..., None], mix(c_cyan_dot, c_cyan_white, np.full((h, w), 0.42)), col)
            _ = dist_ph

    elif mat == 'squid_head':
        if float(wn[1]) > 0.35 and w >= 7 and h >= 5:
            row_u = (du == 2) | (du == w - 3)
            is_dot = row_u & ((dv % 2) == 1) & (dv >= 1) & (dv <= h - 2)
            is_halo = ((np.abs(du - 2) <= 1) | (np.abs(du - (w - 3)) <= 1)) & ((dv % 2) == 1) & (~is_dot) & (dv >= 1) & (dv <= h - 2)
            col = np.where(is_halo[..., None], mix(col, c_cyan_halo, np.full((h, w), 0.58)), col)
            col = np.where(is_dot[..., None], c_cyan_dot, col)

    elif mat in ('squid_arm_glow', 'squid_tentacle', 'squid_club'):
        if mat == 'squid_club' and h >= 4 and w >= 2:
            is_dot = (du == w // 2) & ((dv == h // 2) | (dv == max(1, h // 2 - 2)))
            is_halo = (du == w // 2) & (np.abs(dv - h // 2) == 1) & (~is_dot)
            col = np.where(is_halo[..., None], mix(col, c_cyan_halo, np.full((h, w), 0.60)), col)
            col = np.where(is_dot[..., None], c_cyan_dot, col)
        elif h >= 5 and w >= 2:
            is_dot = (du == w // 2) & ((dv % 5) == 2) & (dv >= 1) & (dv <= h - 2)
            is_halo = (du == w // 2) & ((dv % 5) == 1) & (dv >= 1) & (dv <= h - 2)
            col = np.where(is_halo[..., None], mix(col, c_cyan_halo, np.full((h, w), 0.48)), col)
            col = np.where(is_dot[..., None], c_cyan_dot, col)

    return col


# ============================================================================
# 15 — SILVER SEA SERPENT / LEVIATHAN (`silver_sea_serpent`)
# ============================================================================

def paint_serpent_face(cube, face_name, w, h, wx, wy, wz, wn, lu, lv, du, dv):
    mat = cube['mat']
    top_shade = 0.5 + 0.5 * float(wn[1])
    s_n = hash_noise(du, dv, seed=1531 + (hash(cube['name'] + face_name) & 0xFF))

    c_scale_crevice = rgb('#526A79')
    c_silver_shadow = rgb('#6E8796')
    c_silver_mid = rgb('#97AFB9')
    c_silver_light = rgb('#C5D8E2')
    c_silver_pearl = rgb('#EDF7FB')

    c_teal_abyss = rgb('#164B56')
    c_teal_deep = rgb('#216570')
    c_teal_irid = rgb('#388C96')
    c_teal_seafoam = rgb('#6CC2C4')

    if mat == 'serpent_fang':
        return mix(rgb('#CBDCE4'), rgb('#F8FFFF'), np.clip(0.45 + (1.0 - lv) * 0.55, 0.0, 1.0))

    if mat == 'serpent_mouth':
        return mix(rgb('#1B3440'), rgb('#2F5666'), np.clip(0.35 + (s_n - 0.5) * 0.25, 0.0, 1.0))

    if mat == 'serpent_eye':
        # Fierce Glowing Cyan-Ice Sea-Dragon Eye with Razor Dark Vertical Slit Pupil!
        if abs(float(wn[0])) > 0.45 or float(wn[2]) < -0.45:
            slit = (du == w // 2) & (h >= 2) & (w >= 3)
            socket = (dv == 0) & (h >= 3)
            glow = mix(rgb('#5FF0E8'), rgb('#EEFFFD'), np.clip(1.0 - np.abs(lu - 0.5) * 1.4, 0.0, 1.0))
            col = np.where(slit[..., None], rgb('#133844'), glow)
            col = np.where(socket[..., None], c_scale_crevice, col)
            return col
        return c_scale_crevice

    if mat in ('serpent_fin_crest', 'serpent_fin', 'serpent_gill_web'):
        # Translucent Iridescent Seafoam-Aquamarine Fin Membrane with Ray Striations!
        f_n = hash_noise(du, dv, seed=1511 + (hash(cube['name'] + face_name) & 0xFF))
        ray_band = ((dv % 2) == 0)
        base_f = np.where(ray_band[..., None], rgb('#B8E0DC'), rgb('#86B8B6'))
        col = mix(base_f, rgb('#EAFAF8'), np.clip(0.22 + (1.0 - lv) * 0.52 + (f_n - 0.5) * 0.18, 0.0, 1.0))
        if h >= 2 and w >= 2:
            outer_rim = (dv == 0) | ((du == 0) & (face_name == 'east')) | ((du == w - 1) & (face_name == 'west'))
            col = np.where(outer_rim[..., None], rgb('#F2FFFD'), col)
        return col

    if mat == 'serpent_fin_ray':
        # Rigid Metallic Frost-Silver & Aquamarine Fin Spine Ray!
        return mix(rgb('#6FA2A8'), c_silver_pearl, np.clip(0.35 + (1.0 - lv) * 0.55 + top_shade * 0.20, 0.0, 1.0))

    if mat == 'serpent_horn':
        # Swept Metallic-Silver Dragon Horns & Flowing Sensory Barbels!
        ring = ((dv % 2) == 0)
        base = mix(c_silver_shadow, c_silver_pearl, np.clip(0.32 + (1.0 - lv) * 0.58 + top_shade * 0.18, 0.0, 1.0))
        return np.where(ring[..., None], base, base * 0.90)

    if mat == 'serpent_dorsal_scute':
        # Raised Frost-Silver & Deep-Teal Armored Spine Scutes along the Dragon's Back!
        edge = (du == 0) | (du == w - 1)
        core = mix(c_silver_light, c_silver_pearl, np.clip(0.55 + (1.0 - lv) * 0.35 + (s_n - 0.5) * 0.18, 0.0, 1.0))
        teal_trim = mix(c_teal_deep, c_teal_irid, s_n)
        return np.where((edge & (w >= 3))[..., None], teal_trim, core)

    if mat == 'serpent_flipper':
        # Scaled Fore/Hind Paddle Flippers with Interlocking Silver-Aqua Scales!
        stagger = (dv % 2)
        sc_groove = (((du + stagger) % 2) == 0) & ((dv % 2) == 1)
        col = mix(c_silver_shadow, c_silver_light, np.clip(0.38 + 0.45 * top_shade + (s_n - 0.5) * 0.22, 0.0, 1.0))
        col = mix(col, rgb('#CBE8E8'), np.clip((1.0 - lv) * 0.48, 0.0, 0.65))
        col = np.where(sc_groove[..., None], mix(col, c_scale_crevice, np.full((h, w), 0.38)), col)
        return col

    # Subtle Pearlescent Sea-Dragon Scale Relief (`orig_15_serpent.png` — refined metallic shimmer, zero noisy checkerboard!)
    row_idx = dv // 2
    stagger = row_idx % 2
    in_crevice = (((du + stagger) % 2) == 0) & ((dv % 2) == 1)
    in_crown = (((du + stagger) % 2) == 1) & ((dv % 2) == 0)

    col = mix(rgb('#7D94A3'), rgb('#A6BAC5'), np.clip(0.34 + 0.46 * top_shade + (s_n - 0.5) * 0.22, 0.0, 1.0))
    col = mix(col, rgb('#CEE0E9'), np.clip((top_shade - 0.30) * 1.45, 0.0, 0.85))
    col = np.where(in_crown[..., None], mix(col, c_silver_pearl, np.full((h, w), 0.32)), col)
    col = np.where(in_crevice[..., None], mix(col, rgb('#6B8392'), np.full((h, w), 0.24)), col)

    if mat in ('serpent_head', 'serpent_snout', 'serpent_brow'):
        if float(wn[1]) > 0.50:
            col = mix(col, c_silver_pearl, np.full((h, w), 0.38))
            # Subtle ocean-teal crown accent along outer edges of cranium
            if mat == 'serpent_head' and w >= 6:
                crown_teal = (du == 0) | (du == w - 1)
                col = np.where(crown_teal[..., None], mix(col, c_teal_deep, np.full((h, w), 0.65)), col)
        if mat == 'serpent_snout' and face_name == 'north' and w >= 3 and h >= 2:
            nostril = (dv == 0) & ((du == 0) | (du == w - 1)) if w <= 4 else (dv == 1) & ((du == 1) | (du == w - 2))
            col = np.where(nostril[..., None], rgb('#1D3B47'), col)
        return col

    if mat in ('serpent_body_seg', 'serpent_body'):
        if face_name == 'south':
            # Dorsal Top: Gleaming Pearl-Silver Spine Ridge Flanked by Iridescent Deep-Ocean-Teal Stripes!
            col = mix(c_silver_light, c_silver_pearl, np.clip(0.55 + (s_n - 0.5) * 0.24, 0.0, 1.0))
            col = np.where(in_crevice[..., None], mix(col, c_silver_mid, np.full((h, w), 0.35)), col)
            if w >= 5:
                outer_teal = (du == 0) | (du == w - 1)
                inner_teal = (du == 1) | (du == w - 2)
                col = np.where(inner_teal[..., None], mix(c_teal_irid, c_teal_seafoam, s_n * 0.6), col)
                col = np.where(outer_teal[..., None], mix(c_teal_abyss, c_teal_deep, s_n), col)
            elif w >= 3:
                outer_teal = (du == 0) | (du == w - 1)
                col = np.where(outer_teal[..., None], mix(c_teal_abyss, c_teal_irid, s_n), col)
            return col

        if face_name == 'north':
            # Ventral Armored Belly Scutes: Soft Pearlescent Silver Plates (`orig_15_serpent.png`!)
            plate_row = dv % 2
            belly_base = mix(rgb('#B8CBD5'), rgb('#DFECEF'), np.clip(0.54 + (s_n - 0.5) * 0.14, 0.0, 1.0))
            belly_seam = mix(rgb('#9BB0BC'), belly_base, np.full((h, w), 0.45))
            col_b = np.where((plate_row == 0)[..., None], mix(belly_base, c_silver_pearl, np.full((h, w), 0.35)), belly_seam)
            if w >= 4:
                side_bevel = (du == 0) | (du == w - 1)
                col_b = np.where(side_bevel[..., None], mix(col_b, rgb('#8EA4B1'), np.full((h, w), 0.30)), col_b)
            return col_b

        if face_name in ('east', 'west'):
            # Lateral Dragon Flanks: Rich Silver Scales + Deep-Ocean-Teal Dorsal Saddle + Iridescent Seafoam Border!
            dorsal_dist = du if face_name == 'east' else (w - 1 - du)
            ventral_dist = (w - 1 - du) if face_name == 'east' else du
            if w >= 5:
                is_deep_teal = (dorsal_dist == 0) | ((dorsal_dist == 1) & (s_n > 0.25))
                is_irid_border = (dorsal_dist == 2) & (s_n > 0.38)
                is_belly_edge = (ventral_dist == 0)
                col = np.where(is_irid_border[..., None], mix(col, c_teal_seafoam, np.full((h, w), 0.65)), col)
                col = np.where(is_deep_teal[..., None], mix(c_teal_abyss, c_teal_irid, s_n * 0.85), col)
                col = np.where(is_belly_edge[..., None], mix(col, rgb('#DCEAF0'), np.full((h, w), 0.40)), col)
            elif w >= 3:
                is_deep_teal = (dorsal_dist == 0)
                col = np.where(is_deep_teal[..., None], mix(c_teal_abyss, c_teal_irid, s_n), col)
            return col

    return col
