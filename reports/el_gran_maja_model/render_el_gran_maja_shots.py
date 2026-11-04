#!/usr/bin/env python3
"""
Standalone software renderer for the actual El Gran Maja cuboids and atlas.
Its box-UV and mirror conventions match the Bedrock/Java model export.
"""
from __future__ import annotations

import json
import math
import pathlib
import numpy as np
from PIL import Image, ImageDraw, ImageFont

DIR = pathlib.Path(__file__).resolve().parent
GEO_PATH = DIR / "el_gran_maja.blockbench.geo.json"
TEX_PATH = DIR / "el_gran_maja.png"
REF_PATH = DIR.parent / "el_gran_maja_refs" / "03_ref_el_gran_maja_full_body_model.png"


def face_rects(u: int, v: int, w: int, h: int, d: int):
    return {
        "up":    (u + d,         v,     w, d),
        "down":  (u + d + w,     v,     w, d),
        "east":  (u,             v + d, d, h),
        "north": (u + d,         v + d, w, h),
        "west":  (u + d + w,     v + d, d, h),
        "south": (u + 2 * d + w, v + d, w, h),
    }


def rot_x(deg: float) -> np.ndarray:
    r = math.radians(deg)
    c, s = math.cos(r), math.sin(r)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]], dtype=np.float64)


def rot_y(deg: float) -> np.ndarray:
    r = math.radians(deg)
    c, s = math.cos(r), math.sin(r)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]], dtype=np.float64)


def rot_z(deg: float) -> np.ndarray:
    r = math.radians(deg)
    c, s = math.cos(r), math.sin(r)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]], dtype=np.float64)


def load_posed_quads(geo_data: dict, pose_overrides: dict[str, tuple[float, float, float]] | None = None,
                     anim_time: float | None = None) -> list[dict]:
    bones = geo_data["minecraft:geometry"][0]["bones"]
    bmap = {b["name"]: b for b in bones}
    pose_overrides = pose_overrides or {}
    transforms: dict[str, tuple[np.ndarray, np.ndarray, np.ndarray]] = {}

    def get_tf(name: str):
        if name in transforms:
            return transforms[name]
        b = bmap[name]
        piv = np.array(b["pivot"], dtype=np.float64)
        rx, ry, rz = pose_overrides.get(name, (0.0, 0.0, 0.0))
        if anim_time is not None and b.get("anim"):
            an = b["anim"]
            ang = an["amp"] * math.sin(anim_time * 0.18 * an.get("speed", 1.0) + an.get("phase", 0.0))
            ax = an.get("axis", "y")
            if ax == "x":
                rx += ang
            elif ax == "z":
                rz += ang
            else:
                ry += ang
        R_loc = rot_y(ry) @ rot_x(rx) @ rot_z(rz)
        parent = b.get("parent")
        if parent is None:
            R_w, t_w = R_loc, piv
        else:
            R_p, piv_p, t_p = get_tf(parent)
            R_w = R_p @ R_loc
            t_w = R_p @ (piv - piv_p) + t_p
        transforms[name] = (R_w, piv, t_w)
        return transforms[name]

    quads = []
    for b in bones:
        R_w, piv, t_w = get_tf(b["name"])
        for cb in b.get("cubes", []):
            ox, oy, oz = cb["origin"]
            w, h, d = cb["size"]
            u0, v0 = cb["uv"]
            rects = face_rects(u0, v0, w, h, d)
            raw_faces = [
                ("north", [(ox, oy + h, oz), (ox + w, oy + h, oz), (ox + w, oy, oz), (ox, oy, oz)], (0, 0, -1)),
                ("south", [(ox + w, oy + h, oz + d), (ox, oy + h, oz + d), (ox, oy, oz + d), (ox + w, oy, oz + d)], (0, 0, 1)),
                ("east",  [(ox + w, oy + h, oz), (ox + w, oy + h, oz + d), (ox + w, oy, oz + d), (ox + w, oy, oz)], (1, 0, 0)),
                ("west",  [(ox, oy + h, oz + d), (ox, oy + h, oz), (ox, oy, oz), (ox, oy, oz + d)], (-1, 0, 0)),
                ("up",    [(ox, oy + h, oz), (ox + w, oy + h, oz), (ox + w, oy + h, oz + d), (ox, oy + h, oz + d)], (0, 1, 0)),
                ("down",  [(ox, oy, oz + d), (ox + w, oy, oz + d), (ox + w, oy, oz), (ox, oy, oz)], (0, -1, 0)),
            ]
            for fname, v_list, n_loc in raw_faces:
                mirror = cb.get("mirror", False)
                # Bedrock imports negate X. In JSON coordinates the east/west
                # box-UV islands therefore swap; mirror swaps them back and
                # reverses U. The up face also reverses V, as Blockbench does.
                rect_name = {"east": "west", "west": "east"}.get(fname, fname)
                if mirror:
                    rect_name = {"east": "west", "west": "east"}.get(rect_name, rect_name)
                fx, fy, fw, fh = rects[rect_name]
                pts_loc = np.array(v_list, dtype=np.float64)
                pts_w = (pts_loc - piv) @ R_w.T + t_w
                n_w = np.array(n_loc, dtype=np.float64) @ R_w.T
                eps = 1e-3
                u_left, u_right = fx + eps, fx + fw - eps
                if mirror:
                    u_left, u_right = u_right, u_left
                v_top, v_bottom = fy + eps, fy + fh - eps
                if fname == "up":
                    v_top, v_bottom = v_bottom, v_top
                uvs = np.array([
                    [u_left,  v_top],
                    [u_right, v_top],
                    [u_right, v_bottom],
                    [u_left,  v_bottom],
                ], dtype=np.float64)
                quads.append({"pts": pts_w, "nrm": n_w, "uvs": uvs, "fname": fname})
    return quads


def make_scale_props_quads(offset_xyz: tuple[float, float, float]) -> list[dict]:
    bx, by, bz = offset_xyz
    boxes = [
        ([bx - 12, by,      bz - 21], [24, 3, 42], (138, 103, 59)),
        ([bx - 14, by + 3,  bz - 21], [ 3, 6, 42], (156, 118, 69)),
        ([bx + 11, by + 3,  bz - 21], [ 3, 6, 42], (156, 118, 69)),
        ([bx - 12, by + 3,  bz - 23], [24, 6,  3], (145, 108, 62)),
        ([bx - 12, by + 3,  bz + 20], [24, 6,  3], (145, 108, 62)),
        ([bx - 4,  by + 3,  bz - 2],  [ 8, 11, 4], (48, 72, 148)),
        ([bx - 4,  by + 14, bz - 2],  [ 8, 11, 4], (0, 158, 168)),
        ([bx - 8,  by + 14, bz - 2],  [ 4, 11, 4], (182, 137, 104)),
        ([bx + 4,  by + 14, bz - 2],  [ 4, 11, 4], (182, 137, 104)),
        ([bx - 4,  by + 25, bz - 4],  [ 8,  8, 8], (192, 145, 112)),
        ([bx - 4,  by + 31, bz - 4],  [ 8,  2, 8], (58, 39, 25)),
        ([bx - 3,  by + 9,  bz - 22], [ 6,  7, 6], (248, 198, 92)),
    ]
    out = []
    for (ox, oy, oz), (w, h, d), col in boxes:
        raw_faces = [
            ([(ox, oy + h, oz), (ox + w, oy + h, oz), (ox + w, oy, oz), (ox, oy, oz)], (0, 0, -1)),
            ([(ox + w, oy + h, oz + d), (ox, oy + h, oz + d), (ox, oy, oz + d), (ox + w, oy, oz + d)], (0, 0, 1)),
            ([(ox + w, oy + h, oz), (ox + w, oy + h, oz + d), (ox + w, oy, oz + d), (ox + w, oy, oz)], (1, 0, 0)),
            ([(ox, oy + h, oz + d), (ox, oy + h, oz), (ox, oy, oz), (ox, oy, oz + d)], (-1, 0, 0)),
            ([(ox, oy + h, oz), (ox + w, oy + h, oz), (ox + w, oy + h, oz + d), (ox, oy + h, oz + d)], (0, 1, 0)),
            ([(ox, oy, oz + d), (ox + w, oy, oz + d), (ox + w, oy, oz), (ox, oy, oz)], (0, -1, 0)),
        ]
        for v_list, n_loc in raw_faces:
            out.append({
                "pts": np.array(v_list, dtype=np.float64),
                "nrm": np.array(n_loc, dtype=np.float64),
                "uvs": np.zeros((4, 2), dtype=np.float64),
                "solid_rgb": np.array(col, dtype=np.float32),
            })
    return out


def make_underwater_bg(w: int, h: int, top_rgb=(36, 56, 84), bot_rgb=(12, 20, 34), rays: bool = True) -> np.ndarray:
    yy = np.linspace(0.0, 1.0, h, dtype=np.float32)[:, None]
    xx = np.linspace(0.0, 1.0, w, dtype=np.float32)[None, :]
    top = np.array(top_rgb, dtype=np.float32)[None, None, :]
    bot = np.array(bot_rgb, dtype=np.float32)[None, None, :]
    bg = top * (1.0 - yy[..., None]) + bot * yy[..., None]
    if rays:
        beam_coord = xx - 0.18 * yy
        beams = (
            np.exp(-((beam_coord - 0.28) / 0.06) ** 2) * 0.45
            + np.exp(-((beam_coord - 0.52) / 0.09) ** 2) * 0.55
            + np.exp(-((beam_coord - 0.74) / 0.05) ** 2) * 0.35
        )
        fade = np.clip(1.0 - yy * 1.15, 0.0, 1.0)
        ray_col = np.array([70.0, 108.0, 145.0], dtype=np.float32)[None, None, :]
        bg = bg + ray_col * (beams * fade)[..., None] * 0.28
    return np.clip(bg, 0, 255).astype(np.float32)


def clip_poly_near(verts_cam: np.ndarray, uvs: np.ndarray, near: float = 4.0):
    out_v, out_uv = [], []
    n = len(verts_cam)
    for i in range(n):
        v1, uv1 = verts_cam[i], uvs[i]
        v2, uv2 = verts_cam[(i + 1) % n], uvs[(i + 1) % n]
        in1, in2 = v1[2] >= near, v2[2] >= near
        if in1 and in2:
            out_v.append(v2)
            out_uv.append(uv2)
        elif in1 and not in2:
            t = (near - v1[2]) / (v2[2] - v1[2])
            out_v.append(v1 + t * (v2 - v1))
            out_uv.append(uv1 + t * (uv2 - uv1))
        elif not in1 and in2:
            t = (near - v1[2]) / (v2[2] - v1[2])
            out_v.append(v1 + t * (v2 - v1))
            out_uv.append(uv1 + t * (uv2 - uv1))
            out_v.append(v2)
            out_uv.append(uv2)
    return out_v, out_uv


def render_scene(
    quads: list[dict],
    tex_rgb: np.ndarray,
    width: int,
    height: int,
    cam_pos: tuple[float, float, float],
    look_at: tuple[float, float, float],
    fov_deg: float = 55.0,
    ortho_half_h: float | None = None,
    world_up_hint: tuple[float, float, float] = (0.0, 1.0, 0.0),
    bg_img: np.ndarray | None = None,
    fog_color: tuple[float, float, float] | None = (18.0, 30.0, 48.0),
    fog_start: float = 650.0,
    fog_end: float = 2600.0,
    ssaa: int = 2,
) -> Image.Image:
    W = width * ssaa
    H = height * ssaa

    if bg_img is not None:
        if ssaa > 1:
            bg_pil = Image.fromarray(bg_img.astype(np.uint8)).resize((W, H), Image.Resampling.BILINEAR)
            fb = np.array(bg_pil, dtype=np.float32)
        else:
            fb = bg_img.copy()
    else:
        fb = make_underwater_bg(W, H)

    zbuf = np.full((H, W), np.inf, dtype=np.float64)

    cpos = np.array(cam_pos, dtype=np.float64)
    tgt = np.array(look_at, dtype=np.float64)
    fwd = tgt - cpos
    fwd /= np.linalg.norm(fwd)
    world_up = np.array(world_up_hint, dtype=np.float64)
    world_up /= np.linalg.norm(world_up)
    if abs(np.dot(fwd, world_up)) > 0.995:
        world_up = np.array([0.0, 0.0, 1.0], dtype=np.float64)
    right = np.cross(fwd, world_up)
    right /= np.linalg.norm(right)
    up = np.cross(right, fwd)
    up /= np.linalg.norm(up)
    R_cam = np.vstack([right, up, fwd])

    sun_dir = np.array([-0.42, 0.76, -0.49], dtype=np.float64)
    sun_dir /= np.linalg.norm(sun_dir)
    fill_dir = np.array([0.52, -0.25, -0.81], dtype=np.float64)
    fill_dir /= np.linalg.norm(fill_dir)

    tex_h, tex_w, _ = tex_rgb.shape
    f_len = (H * 0.5) / math.tan(math.radians(fov_deg * 0.5))
    ortho_scale = (H * 0.5) / ortho_half_h if ortho_half_h else 1.0
    fog_c = np.array(fog_color, dtype=np.float32) if fog_color is not None else None

    for q in quads:
        pts_w = q["pts"]
        n_w = q["nrm"]

        if ortho_half_h is not None:
            if np.dot(n_w, -fwd) <= 1e-4:
                continue
        else:
            center_w = pts_w.mean(axis=0)
            if np.dot(n_w, cpos - center_w) <= 1e-4:
                continue

        ndotl = max(0.0, float(np.dot(n_w, sun_dir)))
        ndotfill = max(0.0, float(np.dot(n_w, fill_dir)))
        shade = 0.56 + 0.50 * ndotl + 0.18 * ndotfill

        pts_c = (pts_w - cpos) @ R_cam.T
        uvs = q["uvs"]

        if ortho_half_h is None:
            poly_v, poly_uv = clip_poly_near(pts_c, uvs, near=6.0)
            if len(poly_v) < 3:
                continue
        else:
            poly_v = list(pts_c)
            poly_uv = list(uvs)

        scr, inv_z, uv_over_z = [], [], []
        for vc, uv in zip(poly_v, poly_uv):
            if ortho_half_h is None:
                iz = 1.0 / vc[2]
                sx = W * 0.5 + vc[0] * f_len * iz
                sy = H * 0.5 - vc[1] * f_len * iz
                scr.append((sx, sy, vc[2]))
                inv_z.append(iz)
                uv_over_z.append((uv[0] * iz, uv[1] * iz))
            else:
                sx = W * 0.5 + vc[0] * ortho_scale
                sy = H * 0.5 - vc[1] * ortho_scale
                scr.append((sx, sy, vc[2]))
                inv_z.append(1.0)
                uv_over_z.append((uv[0], uv[1]))

        solid_rgb = q.get("solid_rgb")

        for k in range(1, len(scr) - 1):
            (ax, ay, az), (bx, by, bz), (cx, cy, cz) = scr[0], scr[k], scr[k + 1]
            det = (bx - ax) * (cy - ay) - (cx - ax) * (by - ay)
            if abs(det) < 1e-5:
                continue

            x0 = max(0, int(math.floor(min(ax, bx, cx))))
            x1 = min(W - 1, int(math.ceil(max(ax, bx, cx))))
            y0 = max(0, int(math.floor(min(ay, by, cy))))
            y1 = min(H - 1, int(math.ceil(max(ay, by, cy))))
            if x0 > x1 or y0 > y1:
                continue

            xs = np.arange(x0, x1 + 1, dtype=np.float64) + 0.5
            ys = np.arange(y0, y1 + 1, dtype=np.float64) + 0.5
            XX, YY = np.meshgrid(xs, ys)

            inv_det = 1.0 / det
            w0 = ((bx - XX) * (cy - YY) - (cx - XX) * (by - YY)) * inv_det
            w1 = ((cx - XX) * (ay - YY) - (ax - XX) * (cy - YY)) * inv_det
            w2 = 1.0 - w0 - w1

            inside = (w0 >= -1e-5) & (w1 >= -1e-5) & (w2 >= -1e-5)
            if not np.any(inside):
                continue

            if ortho_half_h is None:
                iz0, iz1, iz2 = inv_z[0], inv_z[k], inv_z[k + 1]
                iz_interp = w0 * iz0 + w1 * iz1 + w2 * iz2
                z_pix = 1.0 / np.maximum(iz_interp, 1e-9)
            else:
                z_pix = w0 * az + w1 * bz + w2 * cz

            z_sub = zbuf[y0:y1 + 1, x0:x1 + 1]
            vis = inside & (z_pix < z_sub)
            if not np.any(vis):
                continue

            vy, vx = np.where(vis)
            z_vals = z_pix[vy, vx]
            z_sub[vy, vx] = z_vals

            if solid_rgb is not None:
                lit = np.clip(solid_rgb * shade, 0, 255)
                lit = np.broadcast_to(lit, (len(vy), 3)).copy()
            else:
                (uz0, vz0), (uz1, vz1), (uz2, vz2) = uv_over_z[0], uv_over_z[k], uv_over_z[k + 1]
                w0_v, w1_v, w2_v = w0[vy, vx], w1[vy, vx], w2[vy, vx]
                if ortho_half_h is None:
                    u_exact = (w0_v * uz0 + w1_v * uz1 + w2_v * uz2) * z_vals
                    v_exact = (w0_v * vz0 + w1_v * vz1 + w2_v * vz2) * z_vals
                else:
                    u_exact = w0_v * uz0 + w1_v * uz1 + w2_v * uz2
                    v_exact = w0_v * vz0 + w1_v * vz1 + w2_v * vz2
                ui = np.clip(np.floor(u_exact).astype(np.int32), 0, tex_w - 1)
                vi = np.clip(np.floor(v_exact).astype(np.int32), 0, tex_h - 1)
                sampled = tex_rgb[vi, ui]
                lit = np.clip(sampled * shade, 0, 255)

            if fog_c is not None and ortho_half_h is None:
                fog_f = np.clip((z_vals - fog_start) / max(1.0, fog_end - fog_start), 0.0, 0.70).astype(np.float32)[:, None]
                lit = lit * (1.0 - fog_f) + fog_c[None, :] * fog_f

            fb_sub = fb[y0:y1 + 1, x0:x1 + 1]
            fb_sub[vy, vx] = lit

    out_img = Image.fromarray(np.clip(fb, 0, 255).astype(np.uint8), mode="RGB")
    if ssaa > 1:
        out_img = out_img.resize((width, height), Image.Resampling.LANCZOS)
    return out_img


def add_badge(img: Image.Image, text: str, xy=(20, 16), font_size: int = 19):
    draw = ImageDraw.Draw(img, "RGBA")
    try:
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", font_size)
    except Exception:
        font = ImageFont.load_default()
    bbox = draw.textbbox(xy, text, font=font)
    pad = 9
    draw.rounded_rectangle(
        [bbox[0] - pad, bbox[1] - pad // 2, bbox[2] + pad, bbox[3] + pad // 2],
        radius=7,
        fill=(10, 16, 26, 205),
        outline=(78, 114, 142, 225),
        width=2,
    )
    draw.text(xy, text, fill=(232, 242, 250, 255), font=font)


def reference_background(ref: Image.Image, w: int, h: int) -> np.ndarray:
    """An unobstructed column of the reference, without drawing any creature."""
    a = np.asarray(ref.convert("RGB"), dtype=np.float32)
    rows = a[:, 748:755, :].mean(axis=1)
    if len(rows) != h:
        rows = np.asarray(Image.fromarray(rows.astype(np.uint8)).resize((3,h),Image.Resampling.BILINEAR))[:,0,:]
    return np.broadcast_to(rows[:,None,:], (h,w,3)).copy()


def project_ortho(point, camera, target, width, height, half_height):
    fwd=np.array(target,dtype=np.float64)-np.array(camera,dtype=np.float64)
    fwd/=np.linalg.norm(fwd)
    up=np.array([0.,1.,0.]); right=np.cross(fwd,up); right/=np.linalg.norm(right)
    up=np.cross(right,fwd); up/=np.linalg.norm(up)
    rel=np.array(point,dtype=np.float64)-np.array(camera,dtype=np.float64)
    scale=height/(2*half_height)
    return width*.5+np.dot(rel,right)*scale, height*.5-np.dot(rel,up)*scale


def pixel_grid_patch(atlas: Image.Image):
    """Select 16x16 texels inside a real wing up-face (one block square)."""
    import build_el_gran_maja_model as b
    model=b.build_model()
    cubes=[c for bone in model.bones for c in bone["cubes"]]
    uvs=b.all_uvs(cubes,b.small_templates(cubes))
    rgb=np.asarray(atlas.convert("RGB"))
    best=None
    for cb in cubes:
        if cb["material"]!="wing_top" or cb["mirror"]:
            continue
        w,h,d=cb["size"]
        if w<20 or d<32:
            continue
        u,v=uvs[id(cb)]
        fx,fy,fw,fh=b.face_rects(u,v,w,h,d)["up"]
        for off in range(0,fh-15,8):
            left=fx+(fw-16)//2
            patch=rgb[fy+off:fy+off+16,left:left+16]
            # Prefer a sample with a drawn plate edge or mineral flecks.
            score=float(np.std(patch.astype(np.float32),axis=(0,1)).mean())
            if best is None or score>best[0]:
                best=(score,left,fy+off,patch.copy())
    if best is None:
        raise ValueError("No 16x16 wing face available")
    return Image.fromarray(best[3]),(best[1],best[2])


def macro_panel(atlas: Image.Image, sample: Image.Image) -> Image.Image:
    panel=Image.new("RGB",(840,480),(22,31,43))
    draw=ImageDraw.Draw(panel,"RGBA")
    pix=sample.resize((320,320),Image.Resampling.NEAREST)
    panel.paste(pix,(26,112))
    for i in range(17):
        p=26+i*20
        draw.line((p,112,p,432),fill=(10,14,19,125),width=1)
        p=112+i*20
        draw.line((26,p,346,p),fill=(10,14,19,125),width=1)
    draw.rectangle((25,111,346,432),outline=(143,174,167,255),width=2)
    region=atlas.crop((610,1350,738,1478)).resize((320,320),Image.Resampling.NEAREST)
    panel.paste(region,(474,112))
    draw.rectangle((473,111,794,432),outline=(143,174,167,255),width=2)
    add_badge(panel,"16×16 пикселей = 1×1 блок",(26,24),font_size=18)
    add_badge(panel,"Фрагмент атласа 128×128 px",(474,68),font_size=14)
    return panel


def main():
    geo_data=json.loads(GEO_PATH.read_text(encoding="utf-8"))
    atlas=Image.open(TEX_PATH).convert("RGBA")
    tex_rgb=np.asarray(atlas.convert("RGB"),dtype=np.float32)
    ref=Image.open(REF_PATH).convert("RGB")
    rest=load_posed_quads(geo_data)
    opened=load_posed_quads(geo_data,pose_overrides={"jaw":(-13.,0.,0.)})

    # Same units-per-pixel in the two views, matching the reference's layout.
    scale=714/1856
    half_h=368/(2*scale)
    front=render_scene(rest,tex_rgb,714,368,(0.,-14.,-6000.),(0.,-14.,0.),
        ortho_half_h=half_h,bg_img=reference_background(ref,714,368),fog_color=None,ssaa=2)
    side=render_scene(rest,tex_rgb,1985,368,(-9000.,-14.,1675.),(0.,-14.,1675.),
        ortho_half_h=half_h,bg_img=reference_background(ref,1985,368),fog_color=None,ssaa=2)
    model_row=Image.fromarray(reference_background(ref,2771,368).astype(np.uint8))
    model_row.paste(front,(4,0)); model_row.paste(side,(775,0))
    sheet=Image.new("RGB",(2771,820),(14,20,30))
    sheet.paste(ref,(0,42)); sheet.paste(model_row,(0,452))
    add_badge(sheet,"РЕФЕРЕНС 03  ·  АНФАС И ПРОФИЛЬ",(20,8),font_size=26)
    add_badge(sheet,"3D-МОДЕЛЬ  ·  322,1 × 116 × 44,6 БЛОКА  ·  МАСШТАБ КАК У РЕФЕРЕНСА",(20,418),font_size=26)
    sheet.save(DIR/"01_model_front_and_side_ortho.png")
    print("Saved 01: aligned reference and orthographic model",flush=True)

    # Three-quarter close-up: the camera sees the real volume of the mouth.
    head=render_scene(rest,tex_rgb,1680,960,(-750.,180.,-2550.),(0.,-30.,-340.),
                      fov_deg=28,fog_color=None,ssaa=2)
    add_badge(head,"ГОЛОВА 3/4  ·  ГЛАЗА, ДЁСНЫ И ОТДЕЛЬНЫЕ ЗУБЫ",(25,20),font_size=22)
    head.save(DIR/"02_model_head_jaws_eyes_closeup.png")
    print("Saved 02: head",flush=True)

    # Scale scene: every prop is made of actual-size cubes in the same world.
    boat=(-1250.,-350.,-1020.)
    props=make_scale_props_quads(boat)
    cam=(-6500.,2800.,-3200.); target=(0.,0.,1550.); half=1360.
    whole=render_scene(rest+props,tex_rgb,1680,960,cam,target,
                       ortho_half_h=half,fog_color=None,ssaa=2)
    add_badge(whole,"ВСЯ МОДЕЛЬ  ·  322 × 116 × 44,6 БЛОКА  ·  ИГРОК И ЛОДКА В МАСШТАБЕ",(24,20),font_size=21)
    sx,sy=project_ortho((boat[0],boat[1]+15,boat[2]),cam,target,1680,960,half)
    overlay=ImageDraw.Draw(whole,"RGBA")
    if 0<=sx<1680 and 0<=sy<960:
        overlay.ellipse((sx-15,sy-15,sx+15,sy+15),outline=(220,235,221,240),width=2)
        overlay.line((sx+15,sy,1306,706),fill=(185,211,206,180),width=2)
    inset=render_scene(props,tex_rgb,340,200,
        (boat[0]-160,boat[1]+105,boat[2]-200),(boat[0],boat[1]+13,boat[2]),
        ortho_half_h=36,fog_color=None,ssaa=2)
    overlay.rectangle((1303,680,1662,932),fill=(12,21,31,230),outline=(117,157,154,255),width=2)
    whole.paste(inset,(1313,722))
    add_badge(whole,"ИГРОК И ЛОДКА  ·  УВЕЛИЧЕНО",(1315,688),font_size=14)
    whole.save(DIR/"03_model_three_quarter_scale_with_player.png")
    print("Saved 03: entire model and scale",flush=True)

    open_img=render_scene(opened,tex_rgb,1680,960,(0.,-180.,-2900.),(0.,-100.,-300.),
                          fov_deg=25,fog_color=None,ssaa=2)
    add_badge(open_img,"ОТКРЫТАЯ ПАСТЬ  ·  ПОВОРОТ НИЖНЕЙ ЧЕЛЮСТИ −13°",(24,20),font_size=22)
    open_img.save(DIR/"04_model_front_open_jaws.png")
    print("Saved 04: open jaw",flush=True)

    top=render_scene(rest,tex_rgb,840,480,(0.,6500.,1675.),(0.,0.,1675.),
        ortho_half_h=1670.,world_up_hint=(1.,0.,0.),fog_color=None,ssaa=2)
    add_badge(top,"СВЕРХУ  ·  322 × 116 БЛОКОВ",(14,14),font_size=16)
    profile=render_scene(rest,tex_rgb,840,480,(-6000.,-14.,-350.),(0.,-14.,-350.),
        ortho_half_h=500.,fog_color=None,ssaa=2)
    add_badge(profile,"ПРОФИЛЬ ГОЛОВЫ И ЧЕЛЮСТИ",(14,14),font_size=16)
    body=render_scene(rest,tex_rgb,840,480,(-6000.,500.,2100.),(0.,0.,2100.),
        ortho_half_h=650.,fog_color=None,ssaa=2)
    add_badge(body,"ТЕЛО  ·  ПАРУС И ЛУЧИ ПЛАВНИКА",(14,14),font_size=16)
    sample,sample_uv=pixel_grid_patch(atlas)
    macro=macro_panel(atlas,sample)
    technical=Image.new("RGB",(1680,960),(14,20,30))
    technical.paste(top,(0,0)); technical.paste(profile,(840,0))
    technical.paste(body,(0,480)); technical.paste(macro,(840,480))
    technical.save(DIR/"05_model_4view_and_16x16_macro.png")
    print("Saved 05: technical views and 16x16 face",sample_uv,flush=True)

    # Crops are identical rectangles from the aligned views above; no stretching
    # of either model or reference relative to the other.
    crop=(130,130,590,316)
    left=ref.crop(crop).resize((1380,558),Image.Resampling.NEAREST)
    right=model_row.crop(crop).resize((1380,558),Image.Resampling.NEAREST)
    comparison=Image.new("RGB",(2760,640),(14,20,30))
    comparison.paste(left,(0,72)); comparison.paste(right,(1380,72))
    add_badge(comparison,"РЕФЕРЕНС 03  ·  ПАСТЬ",(18,17),font_size=25)
    add_badge(comparison,"МОДЕЛЬ  ·  ТОТ ЖЕ МАСШТАБ И РАКУРС",(1398,17),font_size=25)
    comparison.save(DIR/"06_mouth_reference_comparison.png")
    print("Saved 06: mouth comparison",flush=True)

    # Material close-ups are direct nearest-neighbour crops from the atlas.
    texsheet=Image.new("RGB",(1680,620),(17,26,37))
    draw=ImageDraw.Draw(texsheet,"RGBA")
    crops=[((610,1350,738,1478),"ПАНЦИРЬ / ПЯТНА И ШВЫ"),
           ((1580,455,1708,583),"ДЁСНЫ / СКЛАДКИ"),
           ((3012,2250,3140,2378),"ТЕЛО / ЛУЧИ ПЛАВНИКА")]
    for i,(box,label) in enumerate(crops):
        x=25+i*415
        patch=atlas.crop(box).convert("RGB").resize((360,360),Image.Resampling.NEAREST)
        texsheet.paste(patch,(x,125))
        draw.rectangle((x-1,124,x+360,485),outline=(121,155,147,255),width=2)
        add_badge(texsheet,label,(x,82),font_size=15)
    grid=sample.resize((320,320),Image.Resampling.NEAREST)
    texsheet.paste(grid,(1290,145))
    for i in range(17):
        draw.line((1290+i*20,145,1290+i*20,465),fill=(8,14,18,135),width=1)
        draw.line((1290,145+i*20,1610,145+i*20),fill=(8,14,18,135),width=1)
    draw.rectangle((1289,144,1610,465),outline=(121,155,147,255),width=2)
    add_badge(texsheet,"16×16 px = 1×1 БЛОК",(1290,95),font_size=16)
    add_badge(texsheet,"РЕАЛЬНЫЕ ФРАГМЕНТЫ АТЛАСА  ·  УВЕЛИЧЕНИЕ БЕЗ СГЛАЖИВАНИЯ",(24,20),font_size=20)
    texsheet.save(DIR/"07_texture_paint_and_density.png")
    print("Saved 07: texture details",flush=True)


if __name__=="__main__":
    main()
