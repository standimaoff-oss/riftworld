#!/usr/bin/env python3
"""Превращает cam= из /riftstruct в шесть чисел для lookat.

usage: model_shot_camera.py "<cx,cy,cz:tx,ty,tz>" <factor> "<at=x,y,z>"
factor > 1 — отодвинуть камеру от цели, factor < 1 — приблизить.
Если cam= не пришёл, строит запасную диагональную камеру вокруг at=.
"""
import sys

cam = sys.argv[1] if len(sys.argv) > 1 else ""
factor = float(sys.argv[2]) if len(sys.argv) > 2 else 1.0
at = sys.argv[3] if len(sys.argv) > 3 and sys.argv[3] else "0,110,0"

try:
    px, py, pz = (float(v) for v in at.split(",")[:3])
except Exception:
    px, py, pz = 0.0, 110.0, 0.0

if ":" in cam:
    try:
        p, t = cam.split(":", 1)
        cx, cy, cz = (float(v) for v in p.split(",")[:3])
        tx, ty, tz = (float(v) for v in t.split(",")[:3])
        cx = tx + (cx - tx) * factor
        cy = ty + (cy - ty) * factor
        cz = tz + (cz - tz) * factor
    except Exception:
        cx, cy, cz = px + 12 * factor, py + 8 * factor, pz + 12 * factor
        tx, ty, tz = px, py + 2, pz
else:
    cx, cy, cz = px + 12 * factor, py + 8 * factor, pz + 12 * factor
    tx, ty, tz = px, py + 2, pz

print(f"{cx:.3f} {cy:.3f} {cz:.3f} {tx:.3f} {ty:.3f} {tz:.3f}")
