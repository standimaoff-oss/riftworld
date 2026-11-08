#!/usr/bin/env python3
"""
3D-surface-aware pixel-art texture painter for Riftworld's Blockbench (.bbmodel) creatures:
  1) Liopleurodon (`riftworld:liopleurodon`) — 1024x1024 pelagic pliosaur atlas + emissive eye map
  2) Pegasus (`riftworld:pegasus`) — 512x512 celestial winged steed atlas

Reads the exact `.bbmodel` authored via Blockbench MCP (`groups`, `elements`, `outliner`),
evaluates exact Blockbench ZYX scene-graph transforms for every texel on all 6 faces of
every cuboid, and paints continuous 3D anatomical shading + crisp pixel-art surface detail.
"""
from __future__ import annotations

import json
import math
import pathlib
import sys
from typing import Dict, List, Optional, Tuple

import numpy as np
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[2]


# ==============================================================================
# Exact Blockbench ZYX Scene-Graph Math & Box-UV Unwrapping
# ==============================================================================
def euler_zyx(deg: Tuple[float, float, float]) -> np.ndarray:
    rx, ry, rz = (math.radians(deg[0]), math.radians(deg[1]), math.radians(deg[2]))
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    mx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]], dtype=np.float64)
    my = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]], dtype=np.float64)
    mz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]], dtype=np.float64)
    return mz @ (my @ mx)


def about_pivot(pivot: np.ndarray, rot_deg: Tuple[float, float, float]) -> Tuple[np.ndarray, np.ndarray]:
    m = euler_zyx(rot_deg)
    t = pivot - (m @ pivot)
    return m, t


def compose(a: Tuple[np.ndarray, np.ndarray], b: Tuple[np.ndarray, np.ndarray]) -> Tuple[np.ndarray, np.ndarray]:
    return a[0] @ b[0], (a[0] @ b[1]) + a[1]


def parse_bbmodel_transforms(doc: dict):
    groups = {g["uuid"]: g for g in doc.get("groups", [])}
    elements = {e["uuid"]: e for e in doc.get("elements", [])}
    parent_group: Dict[str, Optional[str]] = {}

    def walk(nodes: list, cur_parent: Optional[str]):
        for item in nodes:
            if isinstance(item, str):
                parent_group[item] = cur_parent
            elif isinstance(item, dict) and "uuid" in item:
                guuid = item["uuid"]
                parent_group[guuid] = cur_parent
                walk(item.get("children", []), guuid)

    walk(doc.get("outliner", []), None)

    g_cache: Dict[Optional[str], Tuple[np.ndarray, np.ndarray]] = {
        None: (np.eye(3, dtype=np.float64), np.zeros(3, dtype=np.float64))
    }

    def group_tf(guuid: Optional[str]) -> Tuple[np.ndarray, np.ndarray]:
        if guuid in g_cache:
            return g_cache[guuid]
        g = groups[guuid]
        piv = np.array(g.get("origin", [0, 0, 0]), dtype=np.float64)
        rot = tuple(g.get("rotation", [0, 0, 0]))
        loc = about_pivot(piv, rot)
        res = compose(group_tf(parent_group.get(guuid)), loc)
        g_cache[guuid] = res
        return res

    cube_infos = []
    for el in doc.get("elements", []):
        if el.get("type", "cube") != "cube":
            continue
        p_uuid = parent_group.get(el["uuid"])
        gtf = group_tf(p_uuid)
        piv = np.array(el.get("origin", [0, 0, 0]), dtype=np.float64)
        rot = tuple(el.get("rotation", [0, 0, 0]))
        ctf = compose(gtf, about_pivot(piv, rot))
        cube_infos.append((el, ctf, groups[p_uuid]["name"] if p_uuid in groups else "root"))
    return cube_infos


def face_texel_grid(el: dict, face: str):
    """
    Returns (x0, y0, fw, fh, local_pts[fh, fw, 3], local_nrm[3]) matching Blockbench computeBoxUv
    and Minecraft ModelPart.Cube 1:1.
    """
    fr = np.array(el["from"], dtype=np.float64)
    to = np.array(el["to"], dtype=np.float64)
    sx = max(1, int(math.floor(to[0] - fr[0] + 1e-7)))
    sy = max(1, int(math.floor(to[1] - fr[1] + 1e-7)))
    sz = max(1, int(math.floor(to[2] - fr[2] + 1e-7)))
    u0, v0 = int(el["uv_offset"][0]), int(el["uv_offset"][1])
    mirror = bool(el.get("mirror_uv", False))

    if face == "up":
        x0, y0, fw, fh = u0 + sz, v0, sx, sz
        us = (np.arange(fw) + 0.5) / fw
        vs = (np.arange(fh) + 0.5) / fh
        uu, vv = np.meshgrid(us, vs)
        if not mirror:
            lx = to[0] - uu * (to[0] - fr[0])
        else:
            lx = fr[0] + uu * (to[0] - fr[0])
        ly = np.full_like(uu, to[1])
        lz = to[2] - vv * (to[2] - fr[2])
        nrm = np.array([0.0, 1.0, 0.0])
    elif face == "down":
        x0, y0, fw, fh = u0 + sz + sx, v0, sx, sz
        us = (np.arange(fw) + 0.5) / fw
        vs = (np.arange(fh) + 0.5) / fh
        uu, vv = np.meshgrid(us, vs)
        if not mirror:
            lx = to[0] - uu * (to[0] - fr[0])
        else:
            lx = fr[0] + uu * (to[0] - fr[0])
        ly = np.full_like(uu, fr[1])
        lz = fr[2] + vv * (to[2] - fr[2])
        nrm = np.array([0.0, -1.0, 0.0])
    elif face == "east":
        # +X side in Blockbench (when mirror=True, east/west swap in computeBoxUv)
        x0, y0, fw, fh = (u0 + sz + sx if mirror else u0), v0 + sz, sz, sy
        us = (np.arange(fw) + 0.5) / fw
        vs = (np.arange(fh) + 0.5) / fh
        uu, vv = np.meshgrid(us, vs)
        lx = np.full_like(uu, to[0])
        ly = to[1] - vv * (to[1] - fr[1])
        lz = (fr[2] + uu * (to[2] - fr[2])) if mirror else (to[2] - uu * (to[2] - fr[2]))
        nrm = np.array([1.0, 0.0, 0.0])
    elif face == "west":
        # -X side in Blockbench
        x0, y0, fw, fh = (u0 if mirror else u0 + sz + sx), v0 + sz, sz, sy
        us = (np.arange(fw) + 0.5) / fw
        vs = (np.arange(fh) + 0.5) / fh
        uu, vv = np.meshgrid(us, vs)
        lx = np.full_like(uu, fr[0])
        ly = to[1] - vv * (to[1] - fr[1])
        lz = (to[2] - uu * (to[2] - fr[2])) if mirror else (fr[2] + uu * (to[2] - fr[2]))
        nrm = np.array([-1.0, 0.0, 0.0])
    elif face == "north":
        # -Z (front / snout-facing)
        x0, y0, fw, fh = u0 + sz, v0 + sz, sx, sy
        us = (np.arange(fw) + 0.5) / fw
        vs = (np.arange(fh) + 0.5) / fh
        uu, vv = np.meshgrid(us, vs)
        lx = (fr[0] + uu * (to[0] - fr[0])) if mirror else (to[0] - uu * (to[0] - fr[0]))
        ly = to[1] - vv * (to[1] - fr[1])
        lz = np.full_like(uu, fr[2])
        nrm = np.array([0.0, 0.0, -1.0])
    elif face == "south":
        # +Z (back / tail-facing)
        x0, y0, fw, fh = u0 + 2 * sz + sx, v0 + sz, sx, sy
        us = (np.arange(fw) + 0.5) / fw
        vs = (np.arange(fh) + 0.5) / fh
        uu, vv = np.meshgrid(us, vs)
        lx = (to[0] - uu * (to[0] - fr[0])) if mirror else (fr[0] + uu * (to[0] - fr[0]))
        ly = to[1] - vv * (to[1] - fr[1])
        lz = np.full_like(uu, to[2])
        nrm = np.array([0.0, 0.0, 1.0])
    else:
        raise ValueError(face)

    pts = np.stack([lx, ly, lz], axis=-1)
    return x0, y0, fw, fh, pts, nrm


# ==============================================================================
# Deterministic Vectorized Noise & Pixel-Art Helpers
# ==============================================================================
def hash2(ix: np.ndarray, iy: np.ndarray, seed: int = 0) -> np.ndarray:
    n = (ix.astype(np.int64) * 374761393 + iy.astype(np.int64) * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    n = n ^ (n >> 16)
    return (n & 0xFFFFFF).astype(np.float64) / float(0x1000000)


def hash3(ix: np.ndarray, iy: np.ndarray, iz: np.ndarray, seed: int = 0) -> np.ndarray:
    n = (
        ix.astype(np.int64) * 374761393
        + iy.astype(np.int64) * 668265263
        + iz.astype(np.int64) * 2246822519
        + seed * 3266489917
    ) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    n = n ^ (n >> 16)
    return (n & 0xFFFFFF).astype(np.float64) / float(0x1000000)


def value_noise3(x: np.ndarray, y: np.ndarray, z: np.ndarray, scale: float, seed: int) -> np.ndarray:
    sx, sy, sz = x / scale, y / scale, z / scale
    ix, iy, iz = np.floor(sx).astype(np.int64), np.floor(sy).astype(np.int64), np.floor(sz).astype(np.int64)
    fx, fy, fz = sx - ix, sy - iy, sz - iz
    ux = fx * fx * (3.0 - 2.0 * fx)
    uy = fy * fy * (3.0 - 2.0 * fy)
    uz = fz * fz * (3.0 - 2.0 * fz)
    c000 = hash3(ix, iy, iz, seed)
    c100 = hash3(ix + 1, iy, iz, seed)
    c010 = hash3(ix, iy + 1, iz, seed)
    c110 = hash3(ix + 1, iy + 1, iz, seed)
    c001 = hash3(ix, iy, iz + 1, seed)
    c101 = hash3(ix + 1, iy, iz + 1, seed)
    c011 = hash3(ix, iy + 1, iz + 1, seed)
    c111 = hash3(ix + 1, iy + 1, iz + 1, seed)
    x00 = c000 + (c100 - c000) * ux
    x10 = c010 + (c110 - c010) * ux
    x01 = c001 + (c101 - c001) * ux
    x11 = c011 + (c111 - c011) * ux
    y0 = x00 + (x10 - x00) * uy
    y1 = x01 + (x11 - x01) * uy
    return y0 + (y1 - y0) * uz


def rgb(hex_str: str) -> np.ndarray:
    s = hex_str.lstrip("#")
    return np.array([int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16)], dtype=np.float64)


def lerp_c(a: np.ndarray, b: np.ndarray, t: np.ndarray) -> np.ndarray:
    t3 = np.clip(t, 0.0, 1.0)[..., None]
    return a + (b - a) * t3


def edge_frame(fh: int, fw: int) -> np.ndarray:
    yy, xx = np.meshgrid(np.arange(fh), np.arange(fw), indexing="ij")
    if fh <= 2 or fw <= 2:
        return np.zeros((fh, fw), dtype=np.float64)
    on_edge = (xx == 0) | (xx == fw - 1) | (yy == 0) | (yy == fh - 1)
    return on_edge.astype(np.float64)


# ==============================================================================
# 1. LIOPLEURODON — Abyssal Pliosaur Leviathan (1024x1024 + Emissive Eyes)
# ==============================================================================
# Continuous 3D Battle Scars across flanks & shoulders: (p0, p1, radius)
LIO_SCARS = [
    # Three parallel giant kraken/rival-pliosaur rake scars across left shoulder & ribs
    ((-18.5, 23.0, -24.0), (-15.5, 16.5, -9.0), 0.72),
    ((-18.2, 21.0, -22.5), (-15.0, 14.5, -7.5), 0.68),
    ((-17.8, 19.0, -21.0), (-14.5, 12.5, -6.0), 0.62),
    # Right flank diagonal battle gash
    ((17.5, 24.5, -8.0), (14.5, 16.0, 11.0), 0.78),
    ((17.0, 22.2, -6.5), (14.2, 14.0, 12.2), 0.65),
    # Snout & brow combat scars
    ((-8.5, 23.5, -74.0), (-5.0, 25.5, -63.0), 0.60),
    ((7.5, 22.0, -78.0), (9.5, 19.5, -68.0), 0.58),
    # Front paddle nick/scar
    ((28.0, 14.5, -20.0), (42.0, 14.0, -14.0), 0.68),
    ((-32.0, 14.5, -18.0), (-46.0, 14.0, -13.0), 0.68),
]


def dist_to_segment(pts: np.ndarray, a: Tuple[float, float, float], b: Tuple[float, float, float]) -> np.ndarray:
    pa = pts - np.array(a, dtype=np.float64)
    ba = np.array(b, dtype=np.float64) - np.array(a, dtype=np.float64)
    h = np.clip(np.sum(pa * ba, axis=-1) / (np.sum(ba * ba) + 1e-9), 0.0, 1.0)
    diff = pa - ba * h[..., None]
    return np.sqrt(np.sum(diff * diff, axis=-1))


def paint_liopleurodon(bbmodel_path: pathlib.Path, out_png: pathlib.Path, out_eyes_png: pathlib.Path):
    doc = json.loads(bbmodel_path.read_text(encoding="utf-8"))
    W, H = doc["resolution"]["width"], doc["resolution"]["height"]
    atlas = np.zeros((H, W, 4), dtype=np.uint8)
    eyes_atlas = np.zeros((H, W, 4), dtype=np.uint8)
    used = np.zeros((H, W), dtype=bool)

    c_abyss = rgb("#141F2C")
    c_dorsal_dark = rgb("#1E2E3E")
    c_dorsal_mid = rgb("#2C4357")
    c_teal_slate = rgb("#3D5B70")
    c_flank_steel = rgb("#567487")
    c_belly_mid = rgb("#9FB0B9")
    c_belly_light = rgb("#CAD5DA")
    c_belly_cream = rgb("#DFE6E8")
    c_scar_core = rgb("#D8CFC2")
    c_scar_rim = rgb("#182430")
    c_scute_top = rgb("#162330")
    c_scute_hi = rgb("#4F6F84")
    c_gum_deep = rgb("#4A1E24")
    c_gum_wet = rgb("#7A333B")
    c_gum_hi = rgb("#9E4B52")
    c_tongue = rgb("#8C3B47")
    c_tooth_root = rgb("#9E8D70")
    c_tooth_mid = rgb("#DDD4BE")
    c_tooth_tip = rgb("#F7F4EA")
    c_orbit = rgb("#0C131B")

    cube_infos = parse_bbmodel_transforms(doc)
    for el, (rot_m, trans_t), group_name in cube_infos:
        name = el.get("name", "")
        yy_idx_cache = None
        for face in ("up", "down", "east", "west", "north", "south"):
            x0, y0, fw, fh, lpts, lnrm = face_texel_grid(el, face)
            assert 0 <= x0 and 0 <= y0 and x0 + fw <= W and y0 + fh <= H, f"UV out of bounds: {name} {face}"
            assert not np.any(used[y0:y0 + fh, x0:x0 + fw]), f"UV overlap on {name} {face} at ({x0},{y0})"
            used[y0:y0 + fh, x0:x0 + fw] = True

            wpts = np.einsum("ij,...j->...i", rot_m, lpts) + trans_t
            wnrm = rot_m @ lnrm
            wx, wy, wz = wpts[..., 0], wpts[..., 1], wpts[..., 2]

            gy, gx = np.meshgrid(np.arange(fh), np.arange(fw), indexing="ij")
            pix_jit = (hash2(gx + x0, gy + y0, 91) - 0.5) * 0.08
            border = edge_frame(fh, fw)

            # ---- Specialized Material Branches by Semantic Cube Name ----
            if "tooth_" in name or "fang_" in name or "tusk_" in name:
                # Root-to-tip enamel shading based on local height inside the fang
                fr_y, to_y = el["from"][1], el["to"][1]
                ly_norm = np.clip((lpts[..., 1] - fr_y) / max(1.0, to_y - fr_y), 0.0, 1.0)
                # Upper fangs hang down (-Y is tip), lower fangs point up (+Y is tip)
                tip_t = (1.0 - ly_norm) if "upper" in name or "rosette_u" in name else ly_norm
                col = lerp_c(c_tooth_root, c_tooth_mid, tip_t * 1.5)
                col = lerp_c(col, c_tooth_tip, np.clip((tip_t - 0.45) / 0.55, 0.0, 1.0))
                # Longitudinal enamel flute lines
                flute = ((gx % 2 == 0).astype(np.float64) - 0.5) * 10.0
                col = col + flute[..., None] + pix_jit[..., None] * 90.0
                col = col * (1.0 - border[..., None] * 0.08)

            elif "eye_" in name and "socket" not in name and "brow" not in name:
                # Fierce Abyssal-Gold Reptile Eye with vertical slit pupil & specular glint
                u_f = (gx + 0.5) / max(1, fw)
                v_f = (gy + 0.5) / max(1, fh)
                if face in ("east", "west", "north"):
                    dx = (u_f - 0.5) * 2.0
                    dy = (v_f - 0.5) * 2.0
                    rad = np.sqrt(dx * dx + dy * dy)
                    iris = lerp_c(rgb("#FFCC38"), rgb("#D96B14"), np.clip(rad * 0.95, 0.0, 1.0))
                    # Dark scleral rim
                    col = lerp_c(iris, rgb("#121A22"), np.clip((rad - 0.78) / 0.25, 0.0, 1.0))
                    # Vertical reptile slit pupil
                    slit = (np.abs(dx) < 0.22) & (np.abs(dy) < 0.78)
                    col = np.where(slit[..., None], rgb("#090C10"), col)
                    # Crisp white-cyan catchlight in upper-front quadrant
                    glint = (dx > -0.38) & (dx < 0.05) & (dy > -0.65) & (dy < -0.20)
                    col = np.where(glint[..., None], rgb("#FFFDF4"), col)

                    # Emissive glow pass (subtle warm gold iris + bright glint, zero on dark pupil)
                    em = np.where(slit[..., None], np.zeros(3), iris * 0.82)
                    em = np.where(glint[..., None], rgb("#FFF5CC"), em)
                    em = np.where((rad > 0.92)[..., None], np.zeros(3), em)
                    eyes_block = np.zeros((fh, fw, 4), dtype=np.uint8)
                    em_mask = np.max(em, axis=-1) > 8.0
                    eyes_block[..., :3] = np.clip(em, 0, 255).astype(np.uint8)
                    eyes_block[..., 3] = np.where(em_mask, 255, 0).astype(np.uint8)
                    eyes_atlas[y0:y0 + fh, x0:x0 + fw] = eyes_block
                else:
                    col = np.broadcast_to(c_orbit, (fh, fw, 3)).copy()

            elif "palate" in name or "gum" in name or "tongue" in name:
                n1 = value_noise3(wx, wy, wz, 2.5, 404)
                base_g = c_tongue if "tongue" in name else c_gum_wet
                col = lerp_c(c_gum_deep, base_g, n1)
                # Transverse palatal rugae (ridges on roof of mouth)
                rugae = (np.sin(wz * 2.1) > 0.45).astype(np.float64)
                col = lerp_c(col, c_gum_hi, rugae * 0.35)
                col = col + pix_jit[..., None] * 70.0

            elif "orbit" in name or "nostril_pit" in name:
                col = lerp_c(c_orbit, c_abyss, hash2(gx + x0, gy + y0, 17))

            elif "scute" in name or "keel" in name or "brow" in name:
                # Armored dark osteoderm ridges with weathered mineral-teal bevels
                n_sc = value_noise3(wx, wy, wz, 3.0, 777)
                col = lerp_c(c_scute_top, c_dorsal_mid, n_sc * 0.7)
                top_bevel = np.clip(wnrm[1] * 0.5 + 0.5, 0.0, 1.0)
                col = lerp_c(col, c_scute_hi, border * 0.42 * top_bevel)
                col = col + pix_jit[..., None] * 85.0

            else:
                # ---- Continuous 3D Pelagic Countershading & Reptilian Hide ----
                # Normalize height relative to local body centerline (~11..30, tapered on tail/jaw)
                local_bot = np.where(wz < -48.0, 12.5, np.where(wz > 42.0, 13.0, 6.5))
                local_top = np.where(wz < -48.0, 26.5, np.where(wz > 42.0, 25.5, 31.0))
                h_t = np.clip((wy - local_bot) / np.maximum(4.0, local_top - local_bot), 0.0, 1.0)

                # Organic wavy boundary along the lateral countershading line (like great white / tiger shark / orca-pliosaur)
                wave = (value_noise3(wx * 0.7, wy * 0.7, wz * 0.45, 5.5, 101) - 0.5) * 0.26
                mottle = value_noise3(wx, wy, wz, 3.8, 202)
                fine = value_noise3(wx, wy, wz, 1.6, 303)

                shade_t = np.clip(h_t + wave + wnrm[1] * 0.14, 0.0, 1.0)
                if "fin_" in group_name:
                    # Marine-reptile / orca hydrofoil countershading:
                    # dorsal (+Y) & leading (-Z) surfaces are dark abyssal slate-teal;
                    # ventral (-Y) underside is pale cream-slate with dark swept wingtips!
                    span_f = np.clip((np.abs(wx) - 14.0) / 56.0, 0.0, 1.0)
                    if wnrm[1] < -0.25:
                        shade_t = np.clip(0.10 + 0.58 * span_f + 0.14 * (mottle - 0.5), 0.04, 0.78)
                    else:
                        shade_t = np.clip(0.62 + 0.24 * span_f + 0.14 * wnrm[1] + 0.12 * (mottle - 0.5), 0.44, 0.96)

                # Ventral cream -> steel flank -> teal-slate -> abyssal indigo back
                belly_c = lerp_c(c_belly_cream, c_belly_light, np.clip(shade_t / 0.25, 0.0, 1.0))
                mid_c = lerp_c(c_belly_mid, c_flank_steel, np.clip((shade_t - 0.22) / 0.26, 0.0, 1.0))
                upper_c = lerp_c(c_teal_slate, c_dorsal_mid, np.clip((shade_t - 0.45) / 0.28, 0.0, 1.0))
                crest_c = lerp_c(c_dorsal_dark, c_abyss, np.clip((shade_t - 0.72) / 0.28, 0.0, 1.0))

                col = np.where(
                    (shade_t < 0.25)[..., None],
                    belly_c,
                    np.where(
                        (shade_t < 0.48)[..., None],
                        lerp_c(belly_c, mid_c, (shade_t - 0.25) / 0.23),
                        np.where(
                            (shade_t < 0.74)[..., None],
                            lerp_c(mid_c, upper_c, (shade_t - 0.48) / 0.26),
                            lerp_c(upper_c, crest_c, (shade_t - 0.74) / 0.26),
                        ),
                    ),
                )

                # Pelagic tiger-bars & reticulated abyssal blotches on dorsal/flank zone
                bars = np.sin(wz * 0.42 + mottle * 3.4) * 0.5 + 0.5
                blotch_mask = np.clip((shade_t - 0.30) / 0.30, 0.0, 1.0) * (bars > 0.64).astype(np.float64)
                col = lerp_c(col, c_abyss, blotch_mask * 0.42)

                # Pale oceanic dapples along the lateral transition zone
                dapple_mask = ((shade_t > 0.24) & (shade_t < 0.56) & (mottle > 0.66)).astype(np.float64)
                col = lerp_c(col, c_belly_light, dapple_mask * 0.45)

                # Hexagonal / diamond reptilian dermal scale grid in 3D space
                sc_u = np.round(wz * 1.15)
                sc_v = np.round((wy + wx * 0.5) * 1.15 + (sc_u % 2) * 0.5)
                sc_edge = (
                    (np.abs(wz * 1.15 - sc_u) > 0.38) | (np.abs((wy + wx * 0.5) * 1.15 + (sc_u % 2) * 0.5 - sc_v) > 0.38)
                ).astype(np.float64)
                scale_strength = np.where(shade_t > 0.28, 0.14, 0.06)
                col = col * (1.0 - sc_edge[..., None] * scale_strength[..., None])

                # Ventral gular throat pleats under neck and chest
                if "neck" in group_name or "jaw" in group_name or "body_front" in group_name:
                    pleat = ((shade_t < 0.26) & (wz < -8.0) & (np.abs(np.sin(wx * 1.65)) > 0.86)).astype(np.float64)
                    col = col * (1.0 - pleat[..., None] * 0.18)

                # Flipper hydrofoil fin-rays & weathered pale trailing edge
                if "fin_" in group_name or "fluke" in group_name:
                    # Fin rays radiating outward along paddle span
                    ray_phase = np.sin((wz * 1.35 + wx * 0.45) + fine * 1.2)
                    rays = ((ray_phase > 0.55) & (np.abs(wx) > 19.0)).astype(np.float64)
                    col = lerp_c(col, c_teal_slate, rays * 0.25)
                    # Barnacle / pale hydrodynamic trailing edge highlight
                    if "trail" in name or "tip" in name or "lobe" in name:
                        col = lerp_c(col, c_belly_mid, (0.28 + 0.22 * fine))

                # Continuous 3D Battle Scars across multiple adjacent cuboids!
                for sa, sb, srad in LIO_SCARS:
                    d_seg = dist_to_segment(wpts, sa, sb)
                    core_m = np.clip(1.0 - d_seg / (srad * 0.58), 0.0, 1.0)
                    rim_m = np.clip(1.0 - np.abs(d_seg - srad * 0.75) / (srad * 0.42), 0.0, 1.0)
                    col = lerp_c(col, c_scar_rim, rim_m * 0.45)
                    col = lerp_c(col, c_scar_core, core_m * 0.85)

                # Directional 3D top-light / belly-AO + subtle cuboid micro-bevel
                top_ao = 0.90 + 0.14 * np.clip(wnrm[1], -1.0, 1.0)
                col = col * top_ao + pix_jit[..., None] * 95.0
                col = col * (1.0 - border[..., None] * 0.05)

            block = np.zeros((fh, fw, 4), dtype=np.uint8)
            block[..., :3] = np.clip(col, 0, 255).astype(np.uint8)
            block[..., 3] = 255
            atlas[y0:y0 + fh, x0:x0 + fw] = block

    out_png.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(atlas, "RGBA").save(out_png, compress_level=7)
    Image.fromarray(eyes_atlas, "RGBA").save(out_eyes_png, compress_level=7)
    occ = int(np.sum(used))
    print(f"[paint] Liopleurodon {W}x{H}: {len(cube_infos)} cubes, {occ} texels ({100.0*occ/(W*H):.1f}%), 0 UV overlaps")


# ==============================================================================
# 2. PEGASUS — Celestial Winged Steed of Elisar (512x512)
# ==============================================================================
def paint_pegasus(bbmodel_path: pathlib.Path, out_png: pathlib.Path):
    doc = json.loads(bbmodel_path.read_text(encoding="utf-8"))
    W, H = doc["resolution"]["width"], doc["resolution"]["height"]
    atlas = np.zeros((H, W, 4), dtype=np.uint8)
    used = np.zeros((H, W), dtype=bool)

    # Celestial Alabaster, Pearl-Silver Dapple, Silk Mane, Swan/Eagle Remiges, Royal Elisar Tack
    c_coat_pure = rgb("#FAF8F4")
    c_coat_warm = rgb("#F0ECE4")
    c_coat_silver = rgb("#D9DEE6")
    c_coat_dapple = rgb("#BFC7D4")
    c_coat_shadow = rgb("#A7B1C2")
    c_muzzle_velvet = rgb("#D8C0BE")
    c_muzzle_dark = rgb("#9E8284")
    c_inner_ear = rgb("#E5CECC")

    c_mane_bright = rgb("#FFFFFF")
    c_mane_pearl = rgb("#E6ECF4")
    c_mane_silver = rgb("#BCC7D8")
    c_mane_gold_tip = rgb("#EFE3C4")

    c_feather_bright = rgb("#FDFCFA")
    c_feather_mid = rgb("#EAEFF6")
    c_feather_blue = rgb("#C6D2E3")
    c_feather_deep = rgb("#9BAAC0")
    c_feather_gold = rgb("#E9D9AE")

    c_hoof_top = rgb("#4B4742")
    c_hoof_dark = rgb("#242220")
    c_hoof_gold = rgb("#CBB06A")

    c_leather = rgb("#6A3929")
    c_leather_dark = rgb("#432117")
    c_leather_hi = rgb("#8B503B")
    c_pad_cream = rgb("#EFE8D5")
    c_gold_bright = rgb("#F5D778")
    c_gold_mid = rgb("#C99E3B")
    c_gold_dark = rgb("#8B681E")
    c_gem_bright = rgb("#88F4DF")
    c_gem_deep = rgb("#2E9B8C")

    cube_infos = parse_bbmodel_transforms(doc)
    for el, (rot_m, trans_t), group_name in cube_infos:
        name = el.get("name", "")
        for face in ("up", "down", "east", "west", "north", "south"):
            x0, y0, fw, fh, lpts, lnrm = face_texel_grid(el, face)
            assert 0 <= x0 and 0 <= y0 and x0 + fw <= W and y0 + fh <= H, f"UV out of bounds: {name} {face} ({x0},{y0},{fw},{fh})"
            assert not np.any(used[y0:y0 + fh, x0:x0 + fw]), f"UV overlap on {name} {face} at ({x0},{y0})"
            used[y0:y0 + fh, x0:x0 + fw] = True

            wpts = np.einsum("ij,...j->...i", rot_m, lpts) + trans_t
            wnrm = rot_m @ lnrm
            wx, wy, wz = wpts[..., 0], wpts[..., 1], wpts[..., 2]

            gy, gx = np.meshgrid(np.arange(fh), np.arange(fw), indexing="ij")
            pix_jit = (hash2(gx + x0, gy + y0, 53) - 0.5) * 0.06
            border = edge_frame(fh, fw)

            if "eye_" in name and "lash" not in name:
                u_f = (gx + 0.5) / max(1, fw)
                v_f = (gy + 0.5) / max(1, fh)
                if face in ("east", "west"):
                    # Expressive noble equine eye: deep twilight-sapphire iris, luminous lower crescent & bright catchlight
                    col = lerp_c(rgb("#13203A"), rgb("#3B6BA5"), np.clip(v_f * 1.15, 0.0, 1.0))
                    pupil = (np.abs(u_f - 0.48) < 0.28) & (np.abs(v_f - 0.48) < 0.24)
                    col = np.where(pupil[..., None], rgb("#0A1020"), col)
                    sclera = (u_f > 0.78) if face == "west" else (u_f < 0.22)
                    col = np.where(sclera[..., None], rgb("#EEF2F8"), col)
                    glint = (gy == 0) & (gx == (1 if face == "west" else max(0, fw - 2)))
                    col = np.where(glint[..., None], rgb("#FFFFFF"), col)
                else:
                    col = np.broadcast_to(rgb("#D8DDE6"), (fh, fw, 3)).copy()

            elif "lash" in name:
                # Subtle warm silver-taupe equine upper eyelid fold
                col = lerp_c(rgb("#8E95A5"), rgb("#C8CFDC"), np.clip(wnrm[1] * 0.5 + 0.5, 0.0, 1.0)) + pix_jit[..., None] * 35.0

            elif "hoof" in name and "fetlock" not in name and "pastern" not in name:
                # Polished dark obsidian-slate hoof with a subtle golden-ivory coronet band & vertical horn Keratin striations
                fr_y, to_y = el["from"][1], el["to"][1]
                ly_t = np.clip((lpts[..., 1] - fr_y) / max(1.0, to_y - fr_y), 0.0, 1.0)
                col = lerp_c(c_hoof_dark, c_hoof_top, ly_t)
                coronet = (ly_t > 0.78).astype(np.float64)
                col = lerp_c(col, c_hoof_gold, coronet * 0.75)
                striation = ((gx % 2 == 0).astype(np.float64) - 0.5) * 9.0
                col = col + striation[..., None] + pix_jit[..., None] * 60.0

            elif "gem" in name:
                # Faceted glowing Celadon-Turquoise Elisar crystal medallion
                u_f = (gx + 0.5) / max(1, fw)
                v_f = (gy + 0.5) / max(1, fh)
                d_c = np.sqrt((u_f - 0.42) ** 2 + (v_f - 0.38) ** 2)
                col = lerp_c(rgb("#D8FFF6"), lerp_c(c_gem_bright, c_gem_deep, np.clip(d_c * 1.4, 0.0, 1.0)), np.clip(d_c * 1.6, 0.0, 1.0))
                col = lerp_c(col, c_gold_mid, border * 0.85)

            elif "tack" in group_name and any(k in name for k in ("pommel", "cantle", "stirrup", "bridle", "buckle", "medallion")):
                # Royal Aurite Gold filigree
                sh = np.clip(0.55 + 0.45 * wnrm[1] - 0.25 * (gy / max(1, fh)), 0.0, 1.0)
                col = lerp_c(c_gold_dark, lerp_c(c_gold_mid, c_gold_bright, sh), sh)
                col = col * (1.0 - border[..., None] * 0.14) + pix_jit[..., None] * 70.0

            elif "tack" in group_name and "saddle_cloth" in name:
                # Quilted cream-ivory royal blanket with golden border trim
                quilt = (((gx + gy) % 3 == 0) | ((gx - gy) % 3 == 0)).astype(np.float64)
                col = c_pad_cream * (1.0 - quilt[..., None] * 0.07)
                trim = ((gy == fh - 1) | (border > 0.5)).astype(np.float64)
                col = lerp_c(col, c_gold_mid, trim * 0.80) + pix_jit[..., None] * 55.0

            elif "tack" in group_name and any(k in name for k in ("saddle", "girth", "skirt", "breastcollar", "strap")):
                # Tooled mahogany leather with brass edge stitching
                n_l = value_noise3(wx, wy, wz, 2.0, 612)
                col = lerp_c(c_leather_dark, lerp_c(c_leather, c_leather_hi, n_l), 0.65 + 0.35 * np.clip(wnrm[1] * 0.5 + 0.5, 0, 1))
                stitch = (border > 0.5) & (((gx + gy) % 2) == 0)
                col = np.where(stitch[..., None], c_gold_mid, col * (1.0 - border[..., None] * 0.12))
                col = col + pix_jit[..., None] * 60.0

            elif any(k in name for k in ("primary_", "secondary_", "covert_", "alula", "wing_spar", "fetlock_feather")):
                # Sculpted Angelic Swan/Eagle Feathers: central ivory rachis shaft, diagonal barbules,
                # cool silver-blue shadow at base/underside, luminous pearl-white body & faint champagne tip!
                fr_x, to_x = el["from"][0], el["to"][0]
                fr_z, to_z = el["from"][2], el["to"][2]
                span_t = np.clip((np.abs(wx) - 5.0) / 36.0, 0.0, 1.0)
                len_z_t = np.clip((lpts[..., 2] - fr_z) / max(1.0, to_z - fr_z), 0.0, 1.0)

                # Dorsal (+Y) brighter pearl, ventral (-Y) soft silver-blue reflection
                dorsal_t = np.clip(wnrm[1] * 0.5 + 0.5, 0.0, 1.0)
                base_f = lerp_c(c_feather_blue, c_feather_bright, dorsal_t * 0.8 + 0.2)
                # Shingled AO shadow near leading root of each feather, luminous toward trailing tip
                feather_grad = lerp_c(c_feather_deep, base_f, np.clip(0.32 + 0.78 * len_z_t, 0.0, 1.0))
                # Subtle golden-pearl iridescence at extreme primary tips
                tip_glow = np.clip((len_z_t - 0.76) / 0.24, 0.0, 1.0) * np.clip(span_t, 0.2, 1.0)
                col = lerp_c(feather_grad, c_feather_gold, tip_glow * 0.38)

                # Crisp central quill shaft (rachis) + diagonal feather barbules
                if fw >= 3 and fh >= 3:
                    mid_x = fw // 2
                    rachis = (gx == mid_x).astype(np.float64)
                    barb = (((gx + gy) % 3 == 0) if face in ("up", "east") else ((gx - gy) % 3 == 0)).astype(np.float64)
                    col = col * (1.0 - barb[..., None] * 0.065)
                    col = lerp_c(col, c_mane_bright, rachis * 0.55)
                col = lerp_c(col, c_feather_blue, border * 0.28) + pix_jit[..., None] * 50.0

            elif any(k in name for k in ("mane_", "forelock", "tail_")):
                # Cascading Silky Silver-Pearl Mane & Tail Plume
                strand = ((gx % 2 == 0).astype(np.float64) * 0.55 + (hash2(gx + x0, np.zeros_like(gy), 88) * 0.45))
                fr_y, to_y = el["from"][1], el["to"][1]
                ly_t = np.clip((lpts[..., 1] - fr_y) / max(1.0, to_y - fr_y), 0.0, 1.0)
                col = lerp_c(c_mane_silver, c_mane_pearl, strand)
                col = lerp_c(col, c_mane_bright, np.clip(ly_t * 0.7 + wnrm[1] * 0.25, 0.0, 1.0))
                # Delicate warm champagne sheen at flowing hair tips
                col = lerp_c(col, c_mane_gold_tip, np.clip((1.0 - ly_t - 0.65) / 0.35, 0.0, 1.0) * 0.35)
                col = col * (1.0 - border[..., None] * 0.08) + pix_jit[..., None] * 55.0

            elif "muzzle" in name or "nostril" in name:
                # Soft velvety pink-grey Arabian horse muzzle
                mz_t = np.clip((-wz - 20.5) / 4.0, 0.0, 1.0)
                col = lerp_c(c_coat_warm, c_muzzle_velvet, mz_t * 0.85)
                if "nostril" in name and face in ("north", "east", "west"):
                    naris_hole = (gx > 0) & (gy > 0)
                    col = np.where(naris_hole[..., None], c_muzzle_dark, col)
                col = col + pix_jit[..., None] * 50.0

            elif "ear_" in name:
                col = lerp_c(c_coat_silver, c_coat_pure, np.clip((wy - 36.0) / 4.5, 0.0, 1.0))
                if face == "north":
                    inner = (gx > 0) & (gx < max(1, fw - 1)) & (gy > 0)
                    col = np.where(inner[..., None], c_inner_ear, col)
                col = col + pix_jit[..., None] * 45.0

            else:
                # ---- Sculpted Alabaster-Silver Dapple Equine Coat ----
                # Vertical shading from legs/underbelly up to sunlit back & crest
                h_coat = np.clip((wy - 4.0) / 23.0, 0.0, 1.0)
                top_sun = np.clip(0.55 + 0.45 * wnrm[1], 0.0, 1.0)
                col = lerp_c(c_coat_silver, c_coat_warm, h_coat)
                col = lerp_c(col, c_coat_pure, top_sun * h_coat)

                # Soft silver-grey Andalusian dapples across haunches, barrel & lower shoulders
                dap_n1 = value_noise3(wx * 1.1, wy * 1.1, wz * 1.1, 2.6, 501)
                dap_n2 = value_noise3(wx * 1.1, wy * 1.1, wz * 1.1, 1.35, 502)
                # Ring-shaped silver dapples (pale center surrounded by soft pearl-grey ring!)
                ring = np.clip(1.0 - np.abs(dap_n1 - 0.56) / 0.11, 0.0, 1.0) * (0.5 + 0.5 * dap_n2)
                dapple_zone = np.clip((wy - 11.0) / 5.0, 0.0, 1.0) * np.clip((14.5 - wz * -0.4) / 14.0, 0.25, 1.0)
                col = lerp_c(col, c_coat_dapple, ring * dapple_zone * 0.52)

                # Subtle lower-leg silver-stocking gradient toward fetlocks, bright white socks above hooves
                sock = np.clip((9.0 - wy) / 6.0, 0.0, 1.0)
                col = lerp_c(col, c_coat_pure, sock * 0.65)

                # Muscular groove AO (under belly, armpits, behind shoulder & stifle)
                under_ao = np.clip(-wnrm[1], 0.0, 1.0) * 0.55
                col = lerp_c(col, c_coat_shadow, under_ao)
                col = col * (1.0 - border[..., None] * 0.04) + pix_jit[..., None] * 55.0

            block = np.zeros((fh, fw, 4), dtype=np.uint8)
            block[..., :3] = np.clip(col, 0, 255).astype(np.uint8)
            block[..., 3] = 255
            atlas[y0:y0 + fh, x0:x0 + fw] = block

    out_png.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(atlas, "RGBA").save(out_png, compress_level=7)
    occ = int(np.sum(used))
    print(f"[paint] Pegasus {W}x{H}: {len(cube_infos)} cubes, {occ} texels ({100.0*occ/(W*H):.1f}%), 0 UV overlaps")


def main():
    mode = sys.argv[1] if len(sys.argv) > 1 else "all"
    bb_dir = ROOT / "reports/blockbench_models"
    tex_dir = ROOT / "mod/src/main/resources/assets/riftworld/textures/entity"
    if mode in ("all", "liopleurodon"):
        paint_liopleurodon(
            bb_dir / "liopleurodon.bbmodel",
            tex_dir / "liopleurodon.png",
            tex_dir / "liopleurodon_eyes.png",
        )
    if mode in ("all", "pegasus"):
        paint_pegasus(
            bb_dir / "pegasus.bbmodel",
            tex_dir / "pegasus.png",
        )


if __name__ == "__main__":
    main()
