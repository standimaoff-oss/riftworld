#!/usr/bin/env python3
"""Rev 7: offline preview of the suspension-bridge geometry (no Minecraft needed).

Mirrors BridgeDeckGeom + BridgePostRenderer math (deck anchor, sag, cable anchor, hangers, net, broken span) and
rasterises a 3/4 view, so the shape can be checked before a game run. Writes reports/rev7_bridge_preview.png.

Run: python3 tools/rift/bridge_preview.py
"""
from __future__ import annotations

import math
import os

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "reports", "rev7_bridge_preview.png")

DECK_ANCHOR = 0.22
CABLE_ANCHOR = 1.62
PLANK_STEP = 0.5
PLANK_TH = 0.11

STYLES = {
    "plains": dict(plank=(150, 130, 104), edge=(120, 103, 82), rope=(168, 143, 102), post=(146, 143, 138),
                   net=(150, 150, 150), holes=0.03, sagmul=1.00, chain=False, lantern=(255, 214, 140)),
    "garden": dict(plank=(166, 150, 136), edge=(132, 118, 106), rope=(148, 126, 92), post=(120, 132, 106),
                   net=(160, 160, 158), holes=0.06, sagmul=1.08, chain=False, lantern=(255, 208, 130)),
    "burnt":  dict(plank=(78, 66, 58), edge=(54, 46, 40), rope=(126, 120, 112), post=(74, 66, 60),
                   net=(110, 108, 104), holes=0.34, sagmul=1.28, chain=True, lantern=(255, 138, 74)),
}


class V:
    def __init__(self, a, b, a2, b2, style):
        self.a, self.b, self.a2, self.b2 = a, b, a2, b2
        self.s = style

    def pt(self, u, w=0.5):
        p = lerp(self.a, self.b, u)
        p2 = lerp(self.a2, self.b2, u)
        p = lerp(p, p2, w)
        return (p[0], p[1] - self.sag() * 4 * u * (1 - u), p[2])

    def cable(self, u, side):
        f = self.a if side == 0 else self.a2
        t = self.b if side == 0 else self.b2
        f = (f[0], f[1] + CABLE_ANCHOR, f[2])
        t = (t[0], t[1] + CABLE_ANCHOR, t[2])
        p = lerp(f, t, u)
        return (p[0], p[1] - self.sag() * 0.78 * 4 * u * (1 - u), p[2])

    def edge(self, u, side):
        return self.pt(u, 0.0 if side == 0 else 1.0)

    def len(self):
        return dist(self.a, self.b)

    def sag(self):
        return min(1.25, max(0.16, self.len() * 0.0205 * self.s["sagmul"]))

    def plank(self, u):
        h = int(math.floor(u * max(2, math.ceil(self.len() / PLANK_STEP))))
        r = (h * 374761393 + 7717) & 0xFFFFFF
        return (r & 0xFF) / 255.0 >= self.s["holes"]


def lerp(a, b, t):
    return (a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t)


def dist(a, b):
    return math.dist(a, b)


class Cam:
    def __init__(self, eye, target, fov=42, w=900, h=520):
        self.eye = eye
        self.w, self.h = w, h
        f = norm((target[0] - eye[0], target[1] - eye[1], target[2] - eye[2]))
        r = norm(cross(f, (0, 1, 0)))
        u = cross(r, f)
        self.f, self.r, self.u = f, r, u
        self.fl = (h / 2) / math.tan(math.radians(fov) / 2)

    def p(self, pt):
        d = (pt[0] - self.eye[0], pt[1] - self.eye[1], pt[2] - self.eye[2])
        z = dot(d, self.f)
        if z < 0.05:
            z = 0.05
        x = dot(d, self.r) / z * self.fl
        y = dot(d, self.u) / z * self.fl
        return (self.w / 2 + x, self.h / 2 - y, z)


def dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def norm(a):
    l = math.sqrt(dot(a, a)) or 1.0
    return (a[0] / l, a[1] / l, a[2] / l)


def box(draw, cam, x0, y0, z0, x1, y1, z1, color, shade=True):
    pts = [(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1),
           (x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)]
    faces = [((0, 1, 2, 3), 0.55), ((4, 5, 6, 7), 1.0), ((0, 1, 5, 4), 0.8),
             ((1, 2, 6, 5), 0.68), ((2, 3, 7, 6), 0.6), ((3, 0, 4, 7), 0.5)]
    out = []
    for idx, k in faces:
        p = [cam.p(pts[i]) for i in idx]
        out.append((sum(q[2] for q in p) / 4, p, k))
    for _, p, k in sorted(out, reverse=True):
        c = tuple(int(v * k) for v in color) if shade else color
        draw.polygon([(q[0], q[1]) for q in p], fill=c)


def line(draw, cam, a, b, color, w=1):
    pa, pb = cam.p(a), cam.p(b)
    draw.line([pa[0], pa[1], pb[0], pb[1]], fill=color, width=w)


def post(draw, cam, p, s):
    x, y, z = p
    base = s["post"]
    box(draw, cam, x + 0.14, y, z + 0.14, x + 0.86, y + 0.31, z + 0.86, base)          # plinth
    box(draw, cam, x + 0.38, y + 0.31, z + 0.38, x + 0.62, y + 1.56, z + 0.62, base)   # shaft
    box(draw, cam, x + 0.31, y + 0.81, z + 0.31, x + 0.69, y + 1.0, z + 0.69, s["rope"])  # knot
    box(draw, cam, x + 0.19, y + 1.56, z + 0.19, x + 0.81, y + 1.69, z + 0.81, base)   # cap
    box(draw, cam, x + 0.38, y + 1.69, z + 0.38, x + 0.62, y + 1.88, z + 0.62, s["lantern"])
    box(draw, cam, x + 0.31, y + 1.88, z + 0.31, x + 0.69, y + 2.0, z + 0.69, base)


def render(style_name, broken=False):
    s = STYLES[style_name]
    a = (0.0, 0.0, 0.0)
    b = (13.0, 1.6, 5.2)          # diagonal span, second end higher - the "под углом" case
    a2 = (0.0, 0.0, 2.0)
    b2 = (13.0, 1.6, 7.2)
    v = V(a, b, a2, b2, s)
    img = Image.new("RGB", (900, 560), (196, 208, 218))
    d = ImageDraw.Draw(img)
    cam = Cam((-7.2, 5.1, -7.4), (6.6, 1.1, 3.2), fov=40)

    # faint horizon band
    for yy in range(0, 560, 40):
        pass
    # ground hint
    for gx in range(-2, 18, 2):
        line(d, cam, (gx, -0.02, -3), (gx, -0.02, 12), (176, 190, 200), 1)
    for gz in range(-2, 13, 2):
        line(d, cam, (-2, -0.02, gz), (17, -0.02, gz), (176, 190, 200), 1)

    n = max(2, int(math.ceil(v.len() / PLANK_STEP)))
    lo, hi = (0.44, 0.63) if broken else (2.0, -1.0)

    # far-to-near painter order: cables, net, planks
    for i in range(n):
        u = (i + 0.5) / n
        if broken and lo < u < hi:
            continue
        if not v.plank(u):
            continue
        u0, u1 = max(0.0, (i + 0.02) / n), min(1.0, (i + 0.98) / n)
        p00, p01 = v.pt(u0, 0), v.pt(u0, 1)
        p10, p11 = v.pt(u1, 0), v.pt(u1, 1)
        top = [cam.p(q) for q in (p00, p10, p11, p01)]
        d.polygon([(q[0], q[1]) for q in top], fill=s["plank"])
        # thickness lip
        lip = [cam.p((q[0], q[1] - PLANK_TH, q[2])) for q in (p00, p10, p11, p01)]
        d.polygon([(q[0], q[1]) for q in (top[0], top[1], lip[1], lip[0])], fill=s["edge"])
    for side in (0, 1):
        prev = None
        for i in range(0, 61):
            u = i / 60.0
            if broken and lo < u < hi:
                prev = None
                continue
            c = v.cable(u, side)
            if prev:
                line(d, cam, prev, c, s["rope"], 3 if not s["chain"] else 2)
            prev = c
        hang_n = max(2, int(round(v.len() / 1.25)))
        for i in range(hang_n + 1):
            u = i / hang_n
            if broken and lo < u < hi:
                continue
            top = v.cable(u, side)
            bot = v.edge(u, side)
            line(d, cam, top, bot, s["rope"] if not s["chain"] else (150, 148, 142), 1)
            if not s["chain"] and i < hang_n:
                u2 = (i + 1) / hang_n
                if broken and lo < u2 < hi:
                    continue
                t2 = v.cable(u2, side)
                b2p = v.edge(u2, side)
                mid1 = lerp(bot, top, 0.5)
                mid2 = lerp(b2p, t2, 0.5)
                line(d, cam, bot, mid2, s["net"], 1)
                line(d, cam, mid1, t2, s["net"], 1)
                line(d, cam, mid1, mid2, s["net"], 1)
    if broken:
        for k in range(3):
            u = 0.44 + 0.004 * k
            p = v.edge(min(1.0, u), k % 2)
            line(d, cam, p, (p[0], p[1] - 1.0 - k * 0.55, p[2]), s["rope"], 2)
    for p in (a, b, a2, b2):
        if broken and (p is a or p is b2):
            continue
        post(d, cam, p, s)
    return img


def main():
    tiles = [render("plains"), render("garden"), render("burnt", broken=True)]
    w, h = tiles[0].size
    sheet = Image.new("RGB", (w * 3 + 40, h + 70), (245, 245, 245))
    d = ImageDraw.Draw(sheet)
    titles = ["PLAINS style - ordinary world / city", "GARDEN style - violet hills (04) / waterfall gardens (07)",
              "BURNT style - Ashen Lands: chains, missing planks, collapsed span"]
    for i, t in enumerate(tiles):
        sheet.paste(t, (10 + i * (w + 10), 50))
        d.text((12 + i * (w + 10), 22), titles[i], fill=(30, 30, 30))
    d.text((12, h + 52), "rev7 geometry preview: deck 0.22 above the post base, cable 1.62, sag ~2% of the span, "
                         "hangers every 1.25, diamond net; span at an angle with a 1.6-block rise.", fill=(60, 60, 60))
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    sheet.save(OUT)
    print("written", OUT)


if __name__ == "__main__":
    main()
