#!/usr/bin/env python3
"""Печатает yaw и pitch (в градусах Minecraft) так, чтобы камера из точки c смотрела в точку t.

usage: lookat_angles.py <cx> <cy> <cz> <tx> <ty> <tz>
yaw: 0 = +z (юг), 90 = -x (запад), 180 = -z (север), 270 = +x (восток)
pitch: >0 — взгляд вниз.
"""
import math
import sys


def main(argv):
    if len(argv) != 7:
        print("usage: lookat_angles.py cx cy cz tx ty tz", file=sys.stderr)
        return 2
    cx, cy, cz, tx, ty, tz = (float(v) for v in argv[1:7])
    dx, dy, dz = tx - cx, ty - cy, tz - cz
    h = math.hypot(dx, dz)
    yaw = math.degrees(math.atan2(-dx, -dz))
    pitch = math.degrees(math.atan2(-dy, h))
    print("%.1f %.1f" % (yaw, pitch))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
