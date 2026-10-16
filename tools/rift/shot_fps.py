#!/usr/bin/env python3
"""Кадр «из игры» без GPU: программный растеризатор от первого лица (по ТЗ: съёмка без граф.
процессора —Software-путь). Берёт ВОКСЕЛИ РЕАЛЬНОГО мира (NBT-сцена того же генератора, что в игре)
и РЕАЛЬНЫЕ текстуры мода 16×16 из assets/riftworld/textures/block; z-буфер, перспектива,
пейзажный туман и градиент неба — как кадр MC-клиента, но на чистом CPU.
Применение: shot_fps.py scene.nbt out.png X Y Z YAW PITCH [FOV] [--res W H] [--sky day|dusk|night] [--fog R,G,B] [--far N]
YAW: 0 = смотреть на -Z, 90 = на +X (как в MC); PITCH: + вверх."""
import math
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from shot_vox import load, TEX_ALIAS, ALPHA  # noqa: E402
TEX_ALIAS = dict(TEX_ALIAS, bridge_walk="ambra_planks", bridge_post="ambra_log")

TEX = os.path.normpath(os.path.join(HERE, "../../mod/src/main/resources/assets/riftworld/textures/block"))
SHADE = {(0, 1, 0): 1.0, (0, -1, 0): 0.5, (0, 0, 1): 0.8, (0, 0, -1): 0.8, (1, 0, 0): 0.62, (-1, 0, 0): 0.62}
SKIES = {
    "day": ((96, 140, 200), (196, 220, 240), (205, 225, 240)),
    "dusk": ((26, 24, 44), (120, 78, 84), (232, 156, 104)),
    "night": ((6, 8, 18), (12, 16, 34), (24, 30, 52)),
}
FACES = [
    ((1, 0, 0), [(1, 0, 1), (1, 0, 0), (1, 1, 0), (1, 1, 1)], [(1, 0), (0, 0), (0, 1), (1, 1)]),
    ((-1, 0, 0), [(0, 0, 0), (0, 0, 1), (0, 1, 1), (0, 1, 0)], [(1, 0), (0, 0), (0, 1), (1, 1)]),
    ((0, 1, 0), [(0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)], [(0, 0), (1, 0), (1, 1), (0, 1)]),
    ((0, -1, 0), [(0, 0, 0), (0, 0, 1), (1, 0, 1), (1, 0, 0)], [(0, 1), (1, 1), (1, 0), (0, 0)]),
    ((0, 0, 1), [(0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)], [(0, 0), (1, 0), (1, 1), (0, 1)]),
    ((0, 0, -1), [(1, 0, 0), (0, 0, 0), (0, 1, 0), (1, 1, 0)], [(0, 0), (1, 0), (1, 1), (0, 1)]),
]


def tex_of(name, cache={}):
    if name in cache:
        return cache[name]
    n = TEX_ALIAS.get(name, name)
    p = os.path.join(TEX, n + ".png")
    if not os.path.exists(p):
        p = os.path.join(TEX, n.replace("_stairs", "").replace("_slab", "").replace("_wall", "") + ".png")
    if os.path.exists(p):
        im = Image.open(p).convert("RGBA").resize((16, 16), Image.NEAREST)
        a = np.asarray(im).astype(np.float32)
    else:
        a = np.zeros((16, 16, 4), np.float32); a[..., :3] = 160; a[..., 3] = 255
    cache[name] = a
    return a


def main():
    a = sys.argv[1:]
    nbt, out = a[0], a[1]
    cx, cy, cz, yaw, pitch = (float(v) for v in a[2:7])
    fov = float(a[7]) if len(a) > 7 and not a[7].startswith("--") else 72.0
    W, H, sky, far = 1280, 720, "dusk", 110.0
    fog = None
    i = 0
    while i < len(a):
        if a[i] == "--res":
            W, H = int(a[i + 1]), int(a[i + 2]); i += 3; continue
        if a[i] == "--sky":
            sky = a[i + 1]; i += 2; continue
        if a[i] == "--fog":
            fog = np.array([float(v) for v in a[i + 1].split(",")]); i += 2; continue
        if a[i] == "--far":
            far = float(a[i + 1]); i += 2; continue
        i += 1
    size, vox = load(nbt)
    opaque = {n: (n not in ALPHA) for n in set(vox.values())}
    yr = yaw * math.pi / 180.0; pr = pitch * math.pi / 180.0
    f = np.array([math.sin(yr) * math.cos(pr), math.sin(pr), -math.cos(yr) * math.cos(pr)])
    r = np.array([math.cos(yr), 0.0, math.sin(yr)])
    u = np.cross(f, r) * -1.0
    eye = np.array([cx, cy, cz])
    fl = (W * 0.5) / math.tan(fov * math.pi / 360.0)
    top, mid, hor = (np.array(v, np.float32) for v in SKIES[sky])
    tt = np.linspace(0, 1, H)[:, None, None]
    grad = np.where(tt < 0.55, (1 - tt / 0.55) * np.array(top) + (tt / 0.55) * np.array(mid),
                    (1 - (tt - 0.55) / 0.45) * np.array(mid) + ((tt - 0.55) / 0.45) * np.array(hor))
    img = np.broadcast_to(grad, (H, W, 3)).copy()
    if fog is None:
        fog = mid * 0.72 + hor * 0.28
    zbuf = np.full((H, W), 1e9, np.float32)
    near = 0.12
    faces = []
    for (x, y, z), n in vox.items():
        for nrm, corners, uvs in FACES:
            nb = vox.get((x + nrm[0], y + nrm[1], z + nrm[2]))
            if nb is not None and opaque.get(nb, True) and n not in ALPHA:
                continue
            c = np.array([x + 0.5 + nrm[0] * 0.5, y + 0.5 + nrm[1] * 0.5, z + 0.5 + nrm[2] * 0.5])
            d = eye - c
            dist = np.linalg.norm(d)
            if dist > far or d @ np.array(nrm) <= 0.0:
                continue
            pts = [np.array([x + o[0], y + o[1], z + o[2]], np.float64) for o in corners]
            faces.append((dist, n, nrm, pts, uvs, ALPHA.get(n, 1.0)))
    faces.sort(key=lambda q: -q[0])

    def cam(p):
        d = p - eye
        return np.array([d @ r, d @ u, d @ f])

    def clip(poly):
        outp = []
        for i in range(len(poly)):
            cur, nxt = poly[i], poly[(i + 1) % len(poly)]
            ci, cn = cur[2] >= near, nxt[2] >= near
            if ci:
                outp.append(cur)
            if ci != cn:
                t_ = (near - cur[2]) / (nxt[2] - cur[2])
                outp.append(cur + (nxt - cur) * t_)
        return outp

    def raster(tri_pts, tri_uv, tex, sh, alpha, blend):
        tri_uv = [np.array(t, np.float64) for t in tri_uv]
        pc = [cam(p) for p in tri_pts]
        poly = clip(pc)
        if len(poly) < 3:
            return
        uvw = []
        for i, q in enumerate(poly):
            if i < 3:
                uvw.append(tri_uv[i] / max(q[2], near))
            else:
                uvw.append(tri_uv[2] / max(q[2], near))
        scr = [(W * 0.5 + q[0] * fl / q[2], H * 0.5 - q[1] * fl / q[2], q[2]) for q in poly]
        xs = [s[0] for s in scr]; ys = [s[1] for s in scr]
        x0, x1 = int(max(0, min(xs))), int(min(W - 1, math.ceil(max(xs))))
        y0, y1 = int(max(0, min(ys))), int(min(H - 1, math.ceil(max(ys))))
        if x1 < x0 or y1 < y0:
            return
        for k in range(len(poly) - 2):
            tri(scrt=[scr[0], scr[k + 1], scr[k + 2]], uv3=[uvw[0], uvw[k + 1], uvw[k + 2]],
                tex=tex, sh=sh, alpha=alpha, blend=blend, bb=(x0, y0, x1, y1))

    def tri(scrt, uv3, tex, sh, alpha, blend, bb):
        (ax, ay, az), (bx, by, bz), (cx2, cy2, cz2) = scrt
        d = (bx - ax) * (cy2 - ay) - (by - ay) * (cx2 - ax)
        if abs(d) < 1e-9:
            return
        x0, y0, x1, y1 = bb
        gx = np.arange(x0, x1 + 1)[None, :] + 0.5
        gy = np.arange(y0, y1 + 1)[:, None] + 0.5
        w0 = ((bx - gx) * (cy2 - gy) - (by - gy) * (cx2 - gx)) / d
        w1 = ((gx - ax) * (cy2 - gy) - (gy - ay) * (cx2 - gx)) / d
        w2 = 1.0 - w0 - w1
        ins = (w0 >= 0) & (w1 >= 0) & (w2 >= 0)
        if not ins.any():
            return
        iz = w0 / az + w1 / bz + w2 / cz2
        with np.errstate(divide="ignore", invalid="ignore"):
            z = 1.0 / iz
        uu = (w0 * uv3[0][0] + w1 * uv3[1][0] + w2 * uv3[2][0]) * z
        vv = (w0 * uv3[0][1] + w1 * uv3[1][1] + w2 * uv3[2][1]) * z
        ti = np.clip(vv.astype(int), 0, 15); tj = np.clip(uu.astype(int), 0, 15)
        col = tex[ti, tj][:, :, :3] * sh
        al = tex[ti, tj][:, :, 3] / 255.0 * alpha
        fm = np.clip((z - far * 0.45) / (far * 0.55), 0, 1)[:, :, None]
        col = col * (1 - fm) + fog * fm
        zb = zbuf[y0:y1 + 1, x0:x1 + 1]
        m = ins & (z < zb)
        if blend and al is not None:
            m2 = m & (al > 0.02)
            dst = img[y0:y1 + 1, x0:x1 + 1]
            aa = np.where(m2, al, 0.0)[:, :, None]
            dst[:] = dst * (1 - aa) + col * aa
            zbuf[y0:y1 + 1, x0:x1 + 1] = np.where(m2 & (al > 0.6), z, zb)
        else:
            img[y0:y1 + 1, x0:x1 + 1] = np.where(m[:, :, None], col, img[y0:y1 + 1, x0:x1 + 1])
            zbuf[y0:y1 + 1, x0:x1 + 1] = np.where(m, z, zb)

    nf = 0
    for dist, n, nrm, pts, uvs, al in faces:
        tex = tex_of(n)
        sh = SHADE[nrm]
        cp = [cam(p) for p in pts]
        if all(q[2] < near for q in cp):
            continue
        uv3 = [(uvs[0][0] * 16, uvs[0][1] * 16), (uvs[1][0] * 16, uvs[1][1] * 16), (uvs[2][0] * 16, uvs[2][1] * 16)]
        blend = al < 1.0
        raster(pts, uv3, tex, sh, al, blend)
        nf += 1
    Image.fromarray(np.clip(img, 0, 255).astype(np.uint8)).save(out)
    print("fps frame:", out, "faces:", nf, "res:", W, H)


if __name__ == "__main__":
    main()
