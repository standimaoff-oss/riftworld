#!/usr/bin/env python3
"""Objective, vision-free verification of the El Gran Maja head details.

Checks that the third-pass head sculpt actually reads in renders:

1. every one of the 8 eyes is the FIRST surface hit along a front view ray and
   along a 3/4 view ray (i.e. no eye is buried inside the carapace);
2. the rear occipital shell and the dorsal skull are hole free (ray cast);
3. the wide crimson oral cheek window (u = 0.255..0.445) exists in geometry
   (``mouth_cheek`` cubes) AND is painted crimson on the mouth sheet;
4. the outer masseter cheek swell (``cheek_swell``) is present and prominent
   relative to the neighbouring skull width.

Run:  .venv/bin/python reports/el_gran_maja_model/verify_head_details.py
"""

from __future__ import annotations

import json
import sys

import numpy as np
from PIL import Image

import build_el_gran_maja_model as B

HERE = B.HERE


def cube_boxes(cubes):
    """Return (N, 2, 3) float array of min/max corners in rest pose."""
    out = []
    meta = []
    for cb in cubes:
        o = np.asarray(cb["origin"], dtype=np.float64)
        s = np.asarray(cb["size"], dtype=np.float64)
        out.append(np.stack([np.minimum(o, o + s), np.maximum(o, o + s)]))
        meta.append(cb)
    return np.stack(out), meta


def first_hit(boxes, meta, origin, direction, limit=4000.0, step=2.0):
    """March a ray and return the material of the first cube it enters."""
    t = 0.0
    while t < limit:
        p = origin + direction * t
        inside = np.all((p >= boxes[:, 0] - 0.5) & (p <= boxes[:, 1] + 0.5), axis=1)
        if inside.any():
            return meta[int(np.argmax(inside))], float(t)
        t += step
    return None, None


def main() -> int:
    m = B.build_model()
    for b in m.bones:
        for c in b["cubes"]:
            c["bone"] = b["name"]
    cubes = [c for b in m.bones for c in b["cubes"]]
    boxes, meta = cube_boxes(cubes)

    eyes = [c for c in cubes if c["material"] == "eye"]
    failures = []

    print(f"cuboids: {len(cubes)}   eyes: {len(eyes)}")

    # ---------------------------------------------------------------- 1. eyes
    if len(eyes) != 8:
        failures.append(f"expected 8 eye cuboids, found {len(eyes)}")

    # front view: camera far in front of the snout looking back along +Z.
    front_dir = np.array([0.0, 0.0, 1.0], dtype=np.float64)
    # 3/4 view: same elevation, yawed 35 degrees to the right.
    a = np.deg2rad(35.0)
    tq_dir = np.array([np.sin(a), -0.18, np.cos(a)], dtype=np.float64)
    tq_dir /= np.linalg.norm(tq_dir)

    visible_front = 0
    visible_tq = 0
    for cb in eyes:
        o = np.asarray(cb["origin"], dtype=np.float64)
        s = np.asarray(cb["size"], dtype=np.float64)
        centre = o + s * 0.5
        for name, direction, dist in (("front", front_dir, 1200.0),
                                      ("3/4", tq_dir, 1600.0)):
            start = centre - direction * dist
            hit, _t = first_hit(boxes, meta, start, direction, limit=dist * 2.0)
            mat = hit["material"] if hit else None
            if mat == "eye":
                if name == "front":
                    visible_front += 1
                else:
                    visible_tq += 1
            else:
                failures.append(
                    f"eye at {centre.round(1).tolist()} is occluded in {name} view "
                    f"by '{mat}'")

    print(f"eyes visible from the front view : {visible_front} / {len(eyes)}")
    print(f"eyes visible from the 3/4 view   : {visible_tq} / {len(eyes)}")
    if visible_front < len(eyes) or visible_tq < len(eyes):
        failures.append("some eyes are not the first surface along a view ray")

    # ------------------------------------------------- 2. hole free skull roof
    down = np.array([0.0, -1.0, 0.0], dtype=np.float64)
    holes = 0
    for x in range(-420, 421, 30):
        for z in range(-480, 405, 30):
            hit, _t = first_hit(boxes, meta, np.array([x, 600.0, z]), down, limit=1400.0)
            if hit is None:
                holes += 1
    print(f"dorsal skull holes (top-down rays): {holes}")
    if holes:
        failures.append(f"{holes} dorsal skull holes")

    back = np.array([0.0, 0.0, -1.0], dtype=np.float64)
    rear_holes = 0
    for x in range(-360, 361, 30):
        for y in range(-40, 145, 20):
            hit, _t = first_hit(boxes, meta, np.array([x, float(y), 900.0]), back, limit=1600.0)
            if hit is None:
                rear_holes += 1
    print(f"posterior occipital holes        : {rear_holes}")
    if rear_holes:
        failures.append(f"{rear_holes} posterior occipital holes")

    # ------------------------------------------------------- 3. oral cheeks
    def tagged(name):
        out = []
        for c in cubes:
            k = c.get("key")
            while isinstance(k, tuple) and len(k) == 3 and isinstance(k[2], tuple):
                k = k[2]
            if isinstance(k, tuple) and k and k[0] == name:
                out.append(c)
        return out

    cheek = [c for c in cubes if c["material"] == "mouth_cheek"]
    ledge = tagged("cheek_ledge")
    swell = tagged("cheek_swell")
    print(f"mouth_cheek cuboids: {len(cheek)}   cheek_ledge: {len(ledge)}   "
          f"cheek_swell: {len(swell)}")
    if len(cheek) < 20:
        failures.append(f"oral cheek window too small: {len(cheek)} cuboids")
    if len(ledge) < 10:
        failures.append(f"cheek-jowl ledge too small: {len(ledge)} cuboids")
    if len(swell) < 20:
        failures.append(f"outer masseter cheek swell too small: {len(swell)} cuboids")

    us = sorted({B.u_of(abs(c['origin'][0]) + c['size'][0] / 2.0) for c in cheek})
    print(f"oral cheek window u-range: {min(us):.3f} .. {max(us):.3f}")
    if not (min(us) <= 0.27 and max(us) >= 0.435):
        failures.append(f"oral cheek window spans only u = {min(us):.3f}..{max(us):.3f}, "
                        "expected 0.255..0.445")

    # Crimson oral paint: measure "redness" (r - (g+b)/2) on the mouth sheet inside
    # the cheek band and on the celadon carapace sheet it must contrast against.
    atlas = np.asarray(Image.open(HERE / "el_gran_maja.png").convert("RGB")).astype(np.float32)

    def redness(sheet, u0, u1, v0, v1):
        sx, sy, sw, sh = sheet
        sub = atlas[sy:sy + sh, sx:sx + sw]
        xx = np.arange(sw, dtype=np.float32)[None, :] - B.PAD
        yy = np.arange(sh, dtype=np.float32)[:, None] - B.PAD
        uu = np.abs(xx / B.HALF)
        vv = yy / B.HEIGHT
        m = (uu >= u0) & (uu <= u1) & (vv >= v0) & (vv <= v1)
        p = sub[m]
        return float((p[:, 0] - (p[:, 1] + p[:, 2]) * 0.5).mean())

    cheek_red = redness(B.MOUTH_SHEET, 0.255, 0.45, 0.43, 0.82)
    throat_red = redness(B.MOUTH_SHEET, 0.0, 0.20, 0.55, 0.80)
    crown_red = redness(B.HEAD_SHEET, 0.26, 0.45, 0.05, 0.30)
    print(f"redness  cheek {cheek_red:+.1f}   throat {throat_red:+.1f}   "
          f"carapace {crown_red:+.1f}")
    if cheek_red < 22.0:
        failures.append(f"oral cheek pouch is not crimson enough: redness {cheek_red:+.1f}")
    if cheek_red < crown_red + 20.0:
        failures.append("oral cheeks do not contrast against the celadon carapace")

    # ------------------------------------------- 4. outer cheek prominence
    head = [c for c in cubes if c["bone"] == "head"]
    # widest head half-width at the cheek station vs. at the snout station
    def half_width(z):
        best = 0.0
        for c in head:
            o = np.asarray(c["origin"], np.float64)
            s = np.asarray(c["size"], np.float64)
            if o[2] <= z <= o[2] + s[2]:
                best = max(best, abs(o[0]) + s[0] if o[0] >= 0 else abs(o[0] + s[0]))
        return best

    snout = half_width(-820.0)
    cheek_w = half_width(-560.0)
    print(f"head half width: snout {snout:.0f}  cheek {cheek_w:.0f}  "
          f"ratio {cheek_w / max(1.0, snout):.2f}")
    if cheek_w < snout * 1.15:
        failures.append("outer cheeks do not read as wider than the snout")

    print()
    if failures:
        for f in failures:
            print("FAIL:", f)
        return 1
    print("OK: smooth skull, unclipped eyes, bold inner + outer cheeks.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
