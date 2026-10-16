#!/usr/bin/env python3
"""Офлайн-превью оригами-дракона: точное зеркало математики PaperDragonRenderer.java
(позвоночник по -Z, модули-пирамиды, стоячий хребет, голова с рогами/усами/чернилами,
лапки с когтями, алая хвостовая метка, фальш-свет по нормали). Используется только там,
 где нет игровой камеры; настоящие кадры даёт /riftshot и CI-джоба gameshots.
Применение: python3 tools/rift/shot_dragon.py [T] [out.png]"""
import math
import sys

import numpy as np
from PIL import Image

W, H = 1280, 860
SEGS, STEP = 15, 0.55
PAPER, PAPER_D, PAPER_L, INK, RED = 0xF4F0E4, 0xD8D3C2, 0xFBF8EE, 0x33343E, 0xD22E28
LIGHT = np.array([0.35, 0.86, 0.30]); LIGHT = LIGHT / np.linalg.norm(LIGHT)
T = float(sys.argv[1]) if len(sys.argv) > 1 else 12.0
OUT = sys.argv[2] if len(sys.argv) > 2 else "reports/shots_rev7/shot_dragon_preview.png"


def rgb(c, k):
    r = min(255, int(((c >> 16) & 255) * k)); g = min(255, int(((c >> 8) & 255) * k)); b = min(255, int((c & 255) * k))
    return np.array([r, g, b], float)


tris = []


def V(x, y, z):
    return np.array([x, y, z], float)


def tri(p1, p2, p3, color, back=True):
    p1, p2, p3 = V(*p1), V(*p2), V(*p3)
    n = np.cross(p2 - p1, p3 - p1)
    ln = np.linalg.norm(n)
    if ln < 1e-10:
        return
    n = n / ln
    k = 0.60 + 0.40 * max(0.0, float(n @ LIGHT))
    tris.append((p1, p2, p3, rgb(color, k), 0))
    if back:
        tris.append((p3, p2, p1, rgb(color, 0.70), 1))


def quad(a, b, c, d, color):
    tri(a, b, c, color, False); tri(a, c, d, color, False)


# ------------------------------------------------ позвоночник и ширины (1:1 из рендерера)
c = []
for i in range(SEGS + 1):
    grow = min(1.0, 0.15 + i * 0.16)
    lat = math.sin(T * 0.10 - i * 0.52) * 0.16 * (0.3 + grow * 1.4)
    lift = math.sin(T * 0.13 - i * 0.45) * 0.15 * grow
    if i > SEGS - 3:
        lift += math.sin(T * 0.07) * (i - SEGS + 3) * 0.10
    c.append(V(lat, lift, -i * STEP))
w = [0.16 + i * 0.09 if i < 3 else max(0.04, 0.34 * (1.0 - (i - 3) / (SEGS - 1))) for i in range(SEGS + 1)]


def sidev(a, b):
    d = b - a
    s = np.array([-d[2], 0.0, d[0]])
    ln = np.linalg.norm(s)
    return s / ln if ln > 1e-8 else V(1, 0, 0)


# ------------------------------------------------ модули-пирамиды тела
for i in range(SEGS):
    a, b, wa, wb = c[i], c[i + 1], w[i], w[i + 1]
    mid = (a + b) * 0.5; wm = (wa + wb) * 0.5
    up, dn = V(0, wm * 0.85, 0), V(0, -wm * 0.85, 0)
    sv = sidev(a, b) * wm
    aL, aR, bL, bR = a - sv, a + sv, b - sv, b + sv
    aT, bT, aB, bB = a + up, b + up, a + dn, b + dn
    tri(aT, bT, mid + up * 0.55, PAPER)
    tri(bB, aB, mid + dn * 0.55, PAPER_D)
    tri(bL, aL, mid - sv * 0.5, PAPER if i % 2 == 0 else PAPER_D)
    tri(aR, bR, mid + sv * 0.5, PAPER_D if i % 2 == 0 else PAPER)
    tri(aT, aL, aB, PAPER_D); tri(aT, aB, aR, PAPER_D)
# ------------------------------------------------ спинной хребет
for i in range(2, SEGS - 1):
    wave = math.sin(T * 0.10 - i * 0.55) * 0.05
    base = c[i] + V(0, w[i] * 0.80, 0); nxt = c[i + 1] + V(0, w[i + 1] * 0.80, 0)
    tip = (base + nxt) * 0.5 + V(wave, w[i] * 1.15, -w[i] * 0.45)
    tri(base, nxt, tip, PAPER_L if i % 2 == 0 else PAPER)
# ------------------------------------------------ голова
h = c[0]; hw = w[0] * 1.15
snout = h + V(0, 0, hw * 1.9); snoutD = h + V(0, -hw * 0.35, hw * 1.6)
topL = h + V(-hw * 0.7, hw * 0.75, -hw * 0.2); topR = h + V(hw * 0.7, hw * 0.75, -hw * 0.2)
botL = h + V(-hw * 0.7, -hw * 0.4, -hw * 0.2); botR = h + V(hw * 0.7, -hw * 0.4, -hw * 0.2)
tri(topL, topR, snout, PAPER_L)
tri(topL, botL, snout, PAPER); tri(botR, topR, snout, PAPER_D)
tri(botL, botR, snoutD, PAPER_D)
tri(botL, botR, h + V(0, -hw * 0.15, hw * 0.7), INK)
for s, ex in ((-1, topL[0] - 0.001), (1, topR[0] + 0.001)):
    quad((ex, topL[1] - hw * 0.18, topL[2] + hw * 0.5), (ex, topL[1] - hw * 0.30, topL[2] + hw * 0.72),
         (ex, topL[1] - hw * 0.34, topL[2] + hw * 0.86), (ex, topL[1] - hw * 0.24, topL[2] + hw * 0.95), INK)
for s in (-1, 1):
    horn0 = h + V(s * hw * 0.55, hw * 0.75, -hw * 0.25)
    horn1 = horn0 + V(s * hw * 0.35, hw * 0.55, -hw * 1.0)
    hornT = horn1 + V(s * hw * 0.15, hw * 0.05, -hw * 0.9)
    tri(horn0, horn1, hornT, PAPER)
    tri(horn1, hornT, horn1 + V(0, hw * 0.3, -hw * 0.2), INK)
    ear0 = h + V(s * hw * 0.75, hw * 0.55, -hw * 0.35)
    earT = ear0 + V(s * hw * 0.75, hw * 0.15, -hw * 0.9)
    ear1 = ear0 + V(s * hw * 0.2, -hw * 0.25, -hw * 1.1)
    tri(ear0, ear1, earT, PAPER_D)
    w0 = h + V(s * hw * 0.35, -hw * 0.05, hw * 1.3)
    w1 = w0 + V(s * hw * 1.3, -hw * 0.4, hw * 1.0)
    wd = 0.018
    quad((w0[0] - wd, w0[1], w0[2]), (w0[0] + wd, w0[1], w0[2]), (w1[0] + wd, w1[1], w1[2]), (w1[0] - wd, w1[1], w1[2]), INK)
# ------------------------------------------------ лапки
for li, i in enumerate((3, 8)):
    paddle = 0.0
    for s in (-1, 1):
        hip = c[i] + V(s * w[i] * 0.9, -w[i] * 0.4, 0)
        knee = hip + V(s * w[i] * 1.1, -w[i] * 1.15, 0)
        foot = knee + V(s * w[i] * 0.35, -w[i] * 0.85, w[i] * 0.55)
        tri(hip, knee, hip + V(s * 0.03, 0.02, 0.05), PAPER)
        tri(knee, foot, knee + V(s * 0.03, 0.01, 0.05), PAPER_D)
        for k in (-1, 0, 1):
            clawT = foot + V(s * (0.02 + k * 0.025), -w[i] * 0.42, 0.05 + abs(k) * 0.03)
            tri(foot, foot + V(s * 0.015, -0.02, -0.01), clawT, PAPER_L)
# ------------------------------------------------ хвост и алая метка
tip = c[SEGS]
end = tip + V(math.sin(T * 0.10 + 7.8) * 0.10, math.sin(T * 0.05) * 0.06, -STEP * 1.2)
tri(tuple(tip + V(0, w[SEGS], 0)), tuple(tip - V(0, w[SEGS], 0)), tuple(end), PAPER_D)
flap = math.sin(T * 0.45) * 0.10
tag1 = end + V(0.0, 0.03 + flap, -0.22)
quad((end[0] - 0.035, end[1], end[2]), (end[0] + 0.035, end[1], end[2]),
     (tag1[0] + 0.035, tag1[1], tag1[2]), (tag1[0] - 0.035, tag1[1], tag1[2]), RED)

# ------------------------------------------------ камера и растер
allp = np.array([tuple(p) for t in tris for p in t[:3]], float)
ctr = (allp.min(0) + allp.max(0)) * 0.5
eye = ctr + np.array([3.4, 2.4, 7.6])
fwd = ctr - eye; fwd = fwd / np.linalg.norm(fwd)
rt = np.cross(fwd, [0, 1, 0]); rt = rt / np.linalg.norm(rt)
upv = np.cross(rt, fwd)
fl = 2.15


def proj(p):
    d = p - eye
    z = d @ fwd
    if z < 0.05:
        return None, z
    x = (d @ rt) * fl / z; y = (d @ upv) * fl / z
    return (W * 0.5 + x * W * 0.5, H * 0.5 - y * W * 0.5), z


order = sorted(range(len(tris)), key=lambda i: (-np.linalg.norm((tris[i][0] + tris[i][1] + tris[i][2]) / 3 - eye), -tris[i][4]))
img = np.zeros((H, W, 3), float)
yy = np.linspace(0, 1, H)[:, None]
img[:] = ((1 - yy) * np.array([26, 24, 44]) + yy * np.array([232, 156, 104]))[:, None, :]  # закатное небо
for i in order:
    a, b, cc, col, _bk = tris[i]
    pa, za = proj(a); pb, zb = proj(b); pc, zc = proj(cc)
    if pa is None or pb is None or pc is None:
        continue
    xs = [pa[0], pb[0], pc[0]]; ys = [pa[1], pb[1], pc[1]]
    x0, x1 = int(max(0, min(xs))), int(min(W - 1, max(xs) + 1))
    y0, y1 = int(max(0, min(ys))), int(min(H - 1, max(ys) + 1))
    if x1 < x0 or y1 < y0:
        continue
    gx = np.arange(x0, x1 + 1)[None, :]; gy = np.arange(y0, y1 + 1)[:, None]
    d = ((pb[0] - pa[0]) * (pc[1] - pa[1]) - (pb[1] - pa[1]) * (pc[0] - pa[0]))
    if abs(d) < 1e-9:
        continue
    w0 = ((pb[0] - gx) * (pc[1] - gy) - (pb[1] - gy) * (pc[0] - gx)) / d
    w1 = ((gx - pa[0]) * (pc[1] - gy) - (gy - pa[1]) * (pc[0] - gx)) / d
    w2 = 1 - w0 - w1
    ins = (w0 >= -0.002) & (w1 >= -0.002) & (w2 >= -0.002)
    img[y0:y1 + 1, x0:x1 + 1][ins] = col
Image.fromarray(img.astype(np.uint8)).save(OUT)
print("dragon tris:", len(tris), "->", OUT)
