#!/usr/bin/env python3
"""
Exact 3D software z-buffer preview renderer for Blockbench `.bbmodel` files.
Evaluates the exact Blockbench scene graph (`eulerZYX` on bone hierarchy + per-cube
pivots/rotations + `inflate` + Box-UV texel lookup) so we can inspect multi-angle
orthographic/perspective turnarounds and head/wing close-ups before/alongside the
real Minecraft 1.20.1 (`Xvfb` + `Mesa llvmpipe`) client screenshots.
"""
from __future__ import annotations

import base64
import io
import json
import math
import pathlib
import sys
from typing import Dict, List, Optional, Tuple

import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from paint_blockbench_textures import about_pivot, compose, euler_zyx  # noqa: E402


def load_bbmodel_texture(doc: dict, fallback_png: Optional[pathlib.Path] = None) -> np.ndarray:
    for tex in doc.get("textures", []):
        src = tex.get("source", "")
        if src.startswith("data:image/png;base64,"):
            raw = base64.b64decode(src.split(",", 1)[1])
            return np.array(Image.open(io.BytesIO(raw)).convert("RGBA"), dtype=np.uint8)
    if fallback_png and fallback_png.exists():
        return np.array(Image.open(fallback_png).convert("RGBA"), dtype=np.uint8)
    raise RuntimeError("No texture found in .bbmodel")


def build_posed_quads(doc: dict, pose_rot: Optional[Dict[str, Tuple[float, float, float]]] = None):
    """
    Returns list of (verts_world[4, 3], normal_world[3], uv_rect(x0, y0, x1, y1), flip_u, flip_v)
    for all 6 faces of every cube in the .bbmodel.
    """
    pose_rot = pose_rot or {}
    groups = {g["uuid"]: g for g in doc.get("groups", [])}
    parent_group: Dict[str, Optional[str]] = {}

    def walk(nodes: list, cur_p: Optional[str]):
        for item in nodes:
            if isinstance(item, str):
                parent_group[item] = cur_p
            elif isinstance(item, dict) and "uuid" in item:
                guuid = item["uuid"]
                parent_group[guuid] = cur_p
                walk(item.get("children", []), guuid)

    walk(doc.get("outliner", []), None)

    g_cache: Dict[Optional[str], Tuple[np.ndarray, np.ndarray]] = {
        None: (np.eye(3, dtype=np.float64), np.zeros(3, dtype=np.float64))
    }

    def group_tf(guuid: Optional[str]):
        if guuid in g_cache:
            return g_cache[guuid]
        g = groups[guuid]
        piv = np.array(g.get("origin", [0, 0, 0]), dtype=np.float64)
        r0 = g.get("rotation", [0, 0, 0])
        dr = pose_rot.get(g["name"], (0.0, 0.0, 0.0))
        rot = (r0[0] + dr[0], r0[1] + dr[1], r0[2] + dr[2])
        res = compose(group_tf(parent_group.get(guuid)), about_pivot(piv, rot))
        g_cache[guuid] = res
        return res

    quads = []
    for el in doc.get("elements", []):
        if el.get("type", "cube") != "cube" or el.get("visibility") is False:
            continue
        p_uuid = parent_group.get(el["uuid"])
        # Check group visibility
        vis = True
        cur = p_uuid
        while cur is not None:
            g = groups[cur]
            if g.get("visibility") is False or pose_rot.get(f"hide:{g['name']}"):
                vis = False
                break
            cur = parent_group.get(cur)
        if not vis:
            continue

        gtf = group_tf(p_uuid)
        piv = np.array(el.get("origin", [0, 0, 0]), dtype=np.float64)
        rot = tuple(el.get("rotation", [0, 0, 0]))
        m, t = compose(gtf, about_pivot(piv, rot))

        inf = float(el.get("inflate", 0.0))
        fr = np.array(el["from"], dtype=np.float64) - inf
        to = np.array(el["to"], dtype=np.float64) + inf
        raw_fr = np.array(el["from"], dtype=np.float64)
        raw_to = np.array(el["to"], dtype=np.float64)
        sx = max(1, int(math.floor(raw_to[0] - raw_fr[0] + 1e-7)))
        sy = max(1, int(math.floor(raw_to[1] - raw_fr[1] + 1e-7)))
        sz = max(1, int(math.floor(raw_to[2] - raw_fr[2] + 1e-7)))
        u0, v0 = int(el["uv_offset"][0]), int(el["uv_offset"][1])
        mir = bool(el.get("mirror_uv", False))

        x0, y0_, z0 = fr
        x1, y1_, z1 = to

        # Vertices ordered CCW from outside; UV corners (u,v) for each of the 4 vertices
        face_defs = [
            # north (-Z)
            (
                np.array([[x1, y1_, z0], [x0, y1_, z0], [x0, y0_, z0], [x1, y0_, z0]]),
                np.array([0.0, 0.0, -1.0]),
                (u0 + sz + sx, v0 + sz, u0 + sz, v0 + sz + sy) if mir else (u0 + sz, v0 + sz, u0 + sz + sx, v0 + sz + sy),
            ),
            # south (+Z)
            (
                np.array([[x0, y1_, z1], [x1, y1_, z1], [x1, y0_, z1], [x0, y0_, z1]]),
                np.array([0.0, 0.0, 1.0]),
                (u0 + 2 * sz + 2 * sx, v0 + sz, u0 + 2 * sz + sx, v0 + sz + sy)
                if mir
                else (u0 + 2 * sz + sx, v0 + sz, u0 + 2 * sz + 2 * sx, v0 + sz + sy),
            ),
            # east (+X)
            (
                np.array([[x1, y1_, z1], [x1, y1_, z0], [x1, y0_, z0], [x1, y0_, z1]]),
                np.array([1.0, 0.0, 0.0]),
                (u0 + 2 * sz + sx, v0 + sz, u0 + sz + sx, v0 + sz + sy) if mir else (u0, v0 + sz, u0 + sz, v0 + sz + sy),
            ),
            # west (-X)
            (
                np.array([[x0, y1_, z0], [x0, y1_, z1], [x0, y0_, z1], [x0, y0_, z0]]),
                np.array([-1.0, 0.0, 0.0]),
                (u0 + sz, v0 + sz, u0, v0 + sz + sy) if mir else (u0 + sz + sx, v0 + sz, u0 + 2 * sz + sx, v0 + sz + sy),
            ),
            # up (+Y)
            (
                np.array([[x1, y1_, z1], [x0, y1_, z1], [x0, y1_, z0], [x1, y1_, z0]]),
                np.array([0.0, 1.0, 0.0]),
                (u0 + sz + sx, v0, u0 + sz, v0 + sz) if mir else (u0 + sz, v0, u0 + sz + sx, v0 + sz),
            ),
            # down (-Y)
            (
                np.array([[x1, y0_, z0], [x0, y0_, z0], [x0, y0_, z1], [x1, y0_, z1]]),
                np.array([0.0, -1.0, 0.0]),
                (u0 + sz + 2 * sx, v0, u0 + sz + sx, v0 + sz) if mir else (u0 + sz + sx, v0, u0 + sz + 2 * sx, v0 + sz),
            ),
        ]

        for lverts, lnrm, (ua, va, ub, vb) in face_defs:
            wverts = (lverts @ m.T) + t
            wnrm = m @ lnrm
            wnrm = wnrm / (np.linalg.norm(wnrm) + 1e-12)
            uv_verts = np.array([[ua, va], [ub, va], [ub, vb], [ua, vb]], dtype=np.float64)
            quads.append((wverts, wnrm, uv_verts))
    return quads


def render_tile(
    quads,
    tex: np.ndarray,
    width: int,
    height: int,
    yaw_deg: float,
    pitch_deg: float,
    bg_top: Tuple[int, int, int],
    bg_bot: Tuple[int, int, int],
    target: Optional[np.ndarray] = None,
    span_override: Optional[float] = None,
) -> Image.Image:
    # Supersample 2x for crisp edges
    W, H = width * 2, height * 2
    yy = np.linspace(0.0, 1.0, H)[:, None]
    img = (np.array(bg_top) * (1.0 - yy) + np.array(bg_bot) * yy).astype(np.uint8)
    img = np.broadcast_to(img[:, None, :], (H, W, 3)).copy()
    zbuf = np.full((H, W), -1e12, dtype=np.float64)

    # Camera rotation: yaw around Y, then pitch around X
    yr, pr = math.radians(yaw_deg), math.radians(pitch_deg)
    cy, sy = math.cos(yr), math.sin(yr)
    cp, sp = math.cos(pr), math.sin(pr)
    ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]], dtype=np.float64)
    rx = np.array([[1, 0, 0], [0, cp, -sp], [0, sp, cp]], dtype=np.float64)
    cam_m = rx @ ry

    all_pts = np.concatenate([q[0] for q in quads], axis=0)
    if target is None:
        target = 0.5 * (all_pts.min(axis=0) + all_pts.max(axis=0))
    c_pts = (all_pts - target) @ cam_m.T
    if span_override is None:
        span_x = max(1.0, c_pts[:, 0].max() - c_pts[:, 0].min())
        span_y = max(1.0, c_pts[:, 1].max() - c_pts[:, 1].min())
        scale = min(W * 0.84 / span_x, H * 0.82 / span_y)
    else:
        scale = min(W, H) * 0.84 / span_override

    mid_cx = 0.5 * (c_pts[:, 0].min() + c_pts[:, 0].max()) if span_override is None else 0.0
    mid_cy = 0.5 * (c_pts[:, 1].min() + c_pts[:, 1].max()) if span_override is None else 0.0

    key_dir = np.array([0.46, 0.72, 0.52], dtype=np.float64)
    key_dir /= np.linalg.norm(key_dir)
    rim_dir = np.array([-0.58, 0.28, -0.76], dtype=np.float64)
    rim_dir /= np.linalg.norm(rim_dir)

    th, tw = tex.shape[0], tex.shape[1]

    for wverts, wnrm, uv_verts in quads:
        cnrm = cam_m @ wnrm
        if cnrm[2] <= 1e-4:
            continue
        cv = (wverts - target) @ cam_m.T
        sx = (cv[:, 0] - mid_cx) * scale + W * 0.5
        sy_ = H * 0.5 - (cv[:, 1] - mid_cy) * scale
        sz = cv[:, 2]

        shade = 0.56 + 0.42 * max(0.0, float(np.dot(cnrm, key_dir))) + 0.16 * max(0.0, float(np.dot(cnrm, rim_dir)))

        # Rasterize two triangles (0,1,2) and (0,2,3)
        for i0, i1, i2 in ((0, 1, 2), (0, 2, 3)):
            x0, y0, z0 = sx[i0], sy_[i0], sz[i0]
            x1, y1, z1 = sx[i1], sy_[i1], sz[i1]
            x2, y2, z2 = sx[i2], sy_[i2], sz[i2]
            min_x = max(0, int(math.floor(min(x0, x1, x2))))
            max_x = min(W - 1, int(math.ceil(max(x0, x1, x2))))
            min_y = max(0, int(math.floor(min(y0, y1, y2))))
            max_y = min(H - 1, int(math.ceil(max(y0, y1, y2))))
            if min_x > max_x or min_y > max_y:
                continue
            denom = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
            if abs(denom) < 1e-6:
                continue
            px = np.arange(min_x, max_x + 1) + 0.5
            py = np.arange(min_y, max_y + 1) + 0.5
            gxx, gyy = np.meshgrid(px, py)
            w0 = ((y1 - y2) * (gxx - x2) + (x2 - x1) * (gyy - y2)) / denom
            w1 = ((y2 - y0) * (gxx - x2) + (x0 - x2) * (gyy - y2)) / denom
            w2 = 1.0 - w0 - w1
            inside = (w0 >= -1e-5) & (w1 >= -1e-5) & (w2 >= -1e-5)
            if not np.any(inside):
                continue
            zz = w0 * z0 + w1 * z1 + w2 * z2
            sub_z = zbuf[min_y:max_y + 1, min_x:max_x + 1]
            vis = inside & (zz > sub_z)
            if not np.any(vis):
                continue
            uu = w0 * uv_verts[i0, 0] + w1 * uv_verts[i1, 0] + w2 * uv_verts[i2, 0]
            vv = w0 * uv_verts[i0, 1] + w1 * uv_verts[i1, 1] + w2 * uv_verts[i2, 1]
            tu = np.clip(np.floor(uu).astype(np.int32), 0, tw - 1)
            tv = np.clip(np.floor(vv).astype(np.int32), 0, th - 1)
            sampled = tex[tv, tu]
            vis_opaque = vis & (sampled[..., 3] > 16)
            if not np.any(vis_opaque):
                continue
            sub_z[vis_opaque] = zz[vis_opaque]
            lit = np.clip(sampled[..., :3].astype(np.float64) * shade, 0, 255).astype(np.uint8)
            sub_img = img[min_y:max_y + 1, min_x:max_x + 1]
            sub_img[vis_opaque] = lit[vis_opaque]

    return Image.fromarray(img, "RGB").resize((width, height), Image.Resampling.LANCZOS)


def render_sheet(bbmodel_path: pathlib.Path, out_path: pathlib.Path, mode: str):
    doc = json.loads(bbmodel_path.read_text(encoding="utf-8"))
    tex = load_bbmodel_texture(doc)

    if mode == "liopleurodon":
        bg_t, bg_b = (14, 29, 46), (7, 15, 26)
        # Mild natural S-curve swim pose for hero sheet
        swim_pose = {
            "neck_base": (0, 3, 0),
            "neck_front": (0, 4, 0),
            "head": (2, 4, -2),
            "jaw": (-4, 0, 0),
            "body_mid": (0, -3, 0),
            "body_rear": (0, -5, 0),
            "tail_1": (0, -7, 0),
            "tail_2": (0, 8, 0),
            "tail_3": (0, 11, 0),
            "tail_fluke": (0, 10, 0),
            "fin_front_1_l": (4, -4, -6),
            "fin_front_1_r": (-4, 4, -8),
        }
        q_swim = build_posed_quads(doc, swim_pose)
        q_roar = build_posed_quads(doc, {"jaw": (-14, 0, 0), "head": (3, 0, 0)})
        t1 = render_tile(q_swim, tex, 640, 460, 142, 16, bg_t, bg_b)
        t2 = render_tile(q_swim, tex, 640, 460, 90, 6, bg_t, bg_b)
        t3 = render_tile(
            q_roar, tex, 640, 460, 152, 10, bg_t, bg_b, target=np.array([0.0, 19.0, -68.0]), span_override=50.0
        )
        t4 = render_tile(q_swim, tex, 640, 460, 158, 42, bg_t, bg_b)
        labels = [
            "1. 3/4 ПЕРЕДНИЙ РАКУРС (BLOCKBENCH .BBMODEL)",
            "2. ПРОФИЛЬ СБОКУ (12.2 БЛОКА)",
            "3. КРУПНЫЙ ПЛАН: ЧЕРЕП, РОЗЕТКА КЛЫКОВ И ГЛАЗА",
            "4. ВИД СВЕРХУ-СБОКУ: ГИДРОФОЙЛЫ И ХВОСТ",
        ]
    else:
        bg_t, bg_b = (32, 36, 52), (17, 20, 30)
        # Ground folded-wing noble pose vs full spread-wing flight pose!
        folded_pose = {
            "wing_1_l": (62, -78, -6),
            "wing_2_l": (-8, 12, 8),
            "wing_3_l": (-6, 18, 6),
            "wing_1_r": (62, 78, 6),
            "wing_2_r": (-8, -12, -8),
            "wing_3_r": (-6, -18, -6),
            "neck_lower": (-2, 0, 0),
            "head": (2, -5, 0),
        }
        flight_pose = {
            "wing_1_l": (4, 6, 16),
            "wing_2_l": (2, 4, 10),
            "wing_3_l": (0, 2, 8),
            "wing_1_r": (4, -6, -16),
            "wing_2_r": (2, -4, -10),
            "wing_3_r": (0, -2, -8),
            "leg_front_upper_l": (-32, 0, 0),
            "leg_front_cannon_l": (42, 0, 0),
            "leg_front_upper_r": (-26, 0, 0),
            "leg_front_cannon_r": (36, 0, 0),
            "leg_back_thigh_l": (30, 0, 0),
            "leg_back_cannon_l": (22, 0, 0),
            "leg_back_thigh_r": (36, 0, 0),
            "leg_back_cannon_r": (26, 0, 0),
            "tail_1": (-10, 0, 0),
            "tail_2": (-8, 0, 0),
            "hide:saddle_tack": True,
            "hide:head_tack": True,
        }
        wild_folded = dict(folded_pose)
        wild_folded["hide:saddle_tack"] = True
        wild_folded["hide:head_tack"] = True

        q_wild = build_posed_quads(doc, wild_folded)
        q_saddled = build_posed_quads(doc, folded_pose)
        q_flight = build_posed_quads(doc, flight_pose)
        t1 = render_tile(q_wild, tex, 640, 460, 142, 14, bg_t, bg_b)
        t2 = render_tile(q_flight, tex, 640, 460, 148, 22, bg_t, bg_b)
        t3 = render_tile(q_saddled, tex, 640, 460, 102, 10, bg_t, bg_b)
        t4 = render_tile(
            q_saddled, tex, 640, 460, 146, 12, bg_t, bg_b, target=np.array([0.0, 29.5, -14.5]), span_override=26.0
        )
        labels = [
            "1. ДИКИЙ ПЕГАС НА ЗЕМЛЕ (КРЫЛЬЯ СЛОЖЕНЫ)",
            "2. В ПОЛЁТЕ (РАЗМАХ ТРЁХЪЯРУСНЫХ КРЫЛЬЕВ 5.3 БЛОКА)",
            "3. КОРОЛЕВСКОЕ СЕДЛО И УЗДЕЧКА ЭЛИСАРА",
            "4. ПОРТРЕТ: ГОЛОВА, ГРИВА, МЕДАЛЬОН И ПЕРЬЯ",
        ]

    font = None
    for fp in (
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
    ):
        if pathlib.Path(fp).exists():
            font = ImageFont.truetype(fp, 13)
            break

    sheet = Image.new("RGB", (1280, 920), (12, 15, 22))
    for idx, (tile, lbl) in enumerate(zip((t1, t2, t3, t4), labels)):
        ox, oy = (idx % 2) * 640, (idx // 2) * 460
        sheet.paste(tile, (ox, oy))
        dr = ImageDraw.Draw(sheet)
        tw_ = int(dr.textlength(lbl, font=font)) if font else len(lbl) * 7
        dr.rectangle([ox + 12, oy + 10, ox + 28 + tw_, oy + 34], fill=(10, 14, 22))
        dr.text((ox + 20, oy + 14), lbl, fill=(228, 236, 246), font=font)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out_path, compress_level=7)
    print(f"[preview] Saved {out_path.relative_to(ROOT)}")


def main():
    bb_dir = ROOT / "reports/blockbench_models"
    render_sheet(bb_dir / "liopleurodon.bbmodel", bb_dir / "01_liopleurodon_blockbench_sheet.png", "liopleurodon")
    render_sheet(bb_dir / "pegasus.bbmodel", bb_dir / "02_pegasus_blockbench_sheet.png", "pegasus")


if __name__ == "__main__":
    main()
