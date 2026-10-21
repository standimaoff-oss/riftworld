"""Rev 7 part 2: reference implementation of the new tree engine (mirrors tree/TreeGen.java 1:1).

Pure geometry: returns nodes (dx, dy, dz, kind, axis) where kind 0 = log, 1 = leaf, 2 = accent block.
Used by the offline photo renderer (tools/rift/mock_world.py); the Java class must stay formula-identical.
"""
from __future__ import annotations

import math

LOG, LEAF, ACCENT = 0, 1, 2
AXIS_Y, AXIS_X, AXIS_Z = 0, 1, 2

# species ids (see tree/TreeSpecies.java)
SIZO, AMBRA, DEAD, SIZO_GIANT, LILAC, FERN, MIST_PINE, OAK = 0, 1, 2, 3, 4, 5, 6, 7


def h(a: int, b: int, c: int) -> int:
    x = (a * 374761393 + b * 668265263 + c * 1274126177) & 0xFFFFFFFF
    x = ((x ^ (x >> 13)) * 1274126177) & 0xFFFFFFFF
    return (x ^ (x >> 16)) & 0xFFFFFFFF


def rnd(a: int, b: int, c: int) -> float:
    return (h(a, b, c) & 0xFFFFFF) / float(0xFFFFFF)


class Spec:
    def __init__(self, hmin, hmax, trunk, lean, tiers, crown, squash, droop, branches,
                 accent, skirt, vines, fronds=0, cone=False):
        self.hmin, self.hmax = hmin, hmax
        self.trunk = trunk          # trunk half-width in blocks (0 => 1x1, 0.5 => 2x2)
        self.lean = lean            # max horizontal drift of the top
        self.tiers = tiers          # crown tiers (disc/cluster centres), 0 = fronds or cone
        self.crown = crown          # list of tier radii
        self.squash = squash        # vertical squash of the crown clusters
        self.droop = droop          # hanging skirt below the crown rim
        self.branches = branches    # visible branches per tier
        self.accent = accent        # accent block chance on the rim
        self.skirt = skirt          # ragged crown edge strength
        self.vines = vines          # chance of a hanging vine under the rim
        self.fronds = fronds        # fern: number of fronds
        self.cone = cone            # conifer silhouette


SPECS = {
    SIZO: Spec(10, 13, 0, 1.0, 3, [3.4, 2.7, 1.8], 0.30, 1.2, 3, 0.06, 0.34, 0.05),
    SIZO_GIANT: Spec(17, 22, 1.5, 1.4, 4, [5.6, 4.8, 3.8, 2.2], 0.26, 2.0, 4, 0.05, 0.42, 0.10),
    LILAC: Spec(12, 17, 0, 1.6, 4, [4.6, 4.2, 3.4, 2.2], 0.30, 1.6, 4, 0.20, 0.34, 0.03),
    FERN: Spec(7, 10, 0, 1.6, 0, [3.4], 0.55, 0.8, 0, 0.10, 0.22, 0.22, fronds=7),
    MIST_PINE: Spec(14, 20, 0, 0.8, 0, [3.8], 0.60, 0.0, 0, 0.03, 0.14, 0.0, cone=True),
    AMBRA: Spec(7, 10, 0, 1.0, 3, [3.4, 2.6, 1.8], 0.36, 0.5, 2, 0.02, 0.26, 0.0),
    OAK: Spec(8, 12, 0, 1.2, 3, [3.9, 3.3, 2.3], 0.34, 1.0, 3, 0.03, 0.32, 0.02),
}


def build(kind: int, height: int, seed: int):
    """Node list for one tree rooted at (0, 0, 0). dy = 0 is the block above the ground."""
    sp = SPECS.get(kind)
    if sp is None:
        return []
    r = lambda i: rnd(seed, i, 977)          # deterministic per-tree stream
    nodes = {}
    H = max(sp.hmin, min(sp.hmax, height))

    def put(x, y, z, k, ax=AXIS_Y):
        nodes[(x, y, z)] = (k, ax)          # later writes win (leaves never overwrite logs)

    def put_soft(x, y, z, k, ax=AXIS_Y):
        cur = nodes.get((x, y, z))
        if cur is not None and cur[0] == LOG and k == LEAF:
            return
        nodes[(x, y, z)] = (k, ax)

    # ---------------------------------------------------------------- trunk (cone + lean + wobble)
    lean_x = (r(1) - 0.5) * 2 * sp.lean
    lean_z = (r(2) - 0.5) * 2 * sp.lean
    trunk_top = H + 1
    centers = []
    for y in range(0, trunk_top + 1):
        t = y / float(max(1, trunk_top))
        wob_x = 0.45 * math.sin(t * 3.4 + r(3) * 6.283)
        wob_z = 0.45 * math.cos(t * 3.0 + r(4) * 6.283)
        cx = lean_x * t + wob_x * (1 - t * 0.35)
        cz = lean_z * t + wob_z * (1 - t * 0.35)
        centers.append((cx, cz))
        base_rad = sp.trunk if sp.trunk >= 1.0 else (0.75 if H >= 12 else 0.55)
        rad = base_rad * (1.0 - 0.52 * t) + 0.08
        r_cells = int(math.ceil(rad)) if rad > 0.2 else 0
        if r_cells == 0:
            put(int(round(cx)), y, int(round(cz)), LOG)          # 1x1 trunk core always exists
        else:
            for ox in range(-r_cells, r_cells + 1):
                for oz in range(-r_cells, r_cells + 1):
                    if (ox + 0.5) ** 2 + (oz + 0.5) ** 2 <= max(0.30, rad * rad + 0.35):
                        put(int(round(cx)) + ox, y, int(round(cz)) + oz, LOG)
    top_x, top_z = int(round(centers[-1][0])), int(round(centers[-1][1]))

    def crown(cy: int, rx: float, rz: float, squash: float, ragged: float, tier: int, accent: float,
              jx: float = 0.0, jz: float = 0.0):
        ry = max(1.0, rx * squash)
        R = int(math.ceil(max(rx, rz))) + 1
        cx = centers[min(len(centers) - 1, max(0, cy))][0] + jx
        cz = centers[min(len(centers) - 1, max(0, cy))][1] + jz
        for oy in range(-int(ry) - 1, int(ry) + 2):
            for ox in range(-R, R + 1):
                for oz in range(-R, R + 1):
                    q = (ox * ox) / (rx * rx) + (oy * oy) / (ry * ry) + (oz * oz) / (rz * rz)
                    if q > 1.0:
                        continue
                    edge = q > 0.64
                    # canopy = a THIN SHELL (like Dynamic Trees' cloud layers): the hollow core keeps the trunk visible
                    if q < 0.30:
                        continue
                    if edge and rnd(seed, ox * 31 + cy * 7 + tier, oz * 17 + oy) < ragged:
                        continue
                    if not edge and rnd(seed, ox * 7 + cy * 3 + tier, oz * 11 + oy * 5) < 0.32:
                        continue
                    k = LEAF
                    if edge and accent > 0 and rnd(seed, ox * 13 + tier, oz * 29 + oy) < accent:
                        k = ACCENT
                    put_soft(int(round(cx)) + ox, cy + oy, int(round(cz)) + oz, k)
        # drooping skirt (the ragged hanging edge of the canopy)
        if sp.droop > 0:
            n = 12 + int(rnd(seed, tier, 41) * 8)
            for i in range(n):
                a = i * 2 * math.pi / n + rnd(seed, tier, 42) * 0.4
                rr = rx * (0.72 + 0.22 * rnd(seed, tier, 43 + i))
                bx = int(round(cx + math.cos(a) * rr))
                bz = int(round(cz + math.sin(a) * rr))
                ln = 1 + int(rnd(seed, tier, 44 + i) * sp.droop)
                for k2 in range(1, ln + 1):
                    put_soft(bx, cy - k2, bz, LEAF)
        # hanging vines / aerial roots
        if sp.vines > 0:
            for i in range(8):
                if rnd(seed, tier, 51 + i) > sp.vines:
                    continue
                a = i * 2 * math.pi / 8
                bx = int(round(cx + math.cos(a) * rx * 0.85))
                bz = int(round(cz + math.sin(a) * rz * 0.85))
                ln = 1 + int(rnd(seed, tier, 61 + i) * 3)
                for k2 in range(1, ln + 1):
                    put_soft(bx, cy - int(sp.droop) - k2, bz, ACCENT)

    def puff(px: float, py: int, pz: float, rx: float, ry: float, rz: float, tag: int, ragged: float = 0.42):
        """A small ragged leaf cloud - one per branch tip."""
        ix, iy, iz = int(px), int(py), int(pz)
        for oy in range(-int(ry), int(ry) + 1):
            for ox in range(-int(rx), int(rx) + 1):
                for oz in range(-int(rz), int(rz) + 1):
                    if (ox * ox) / (rx * rx) + (oy * oy) / (ry * ry) + (oz * oz) / (rz * rz) > 1.0:
                        continue
                    if rnd(seed, ix + ox * 23 + tag, (iy + oy * 31) * 7919 + iz + oz * 19) < ragged:
                        continue
                    put_soft(ix + ox, iy + oy, iz + oz, LEAF)

    def blob(px: float, py: float, pz: float, rx: float, ry: float, rz: float, tag: int, accent: float):
        """A ragged leaf cloud: the crown is a union of these, one per limb tip (Dynamic Trees silhouette)."""
        ix, iy, iz = int(round(px)), int(round(py)), int(round(pz))
        for oy in range(-int(math.ceil(ry)), int(math.ceil(ry)) + 1):
            for ox in range(-int(math.ceil(rx)), int(math.ceil(rx)) + 1):
                for oz in range(-int(math.ceil(rz)), int(math.ceil(rz)) + 1):
                    q = (ox * ox) / (rx * rx) + (oy * oy) / (ry * ry) + (oz * oz) / (rz * rz)
                    if q > 1.0:
                        continue
                    if q > 0.45 and rnd(seed, ix + ox * 17 + tag, (iz + oz * 13) * 7919 + iy + oy * 29) < 0.40:
                        continue                                  # ragged rim
                    if q < 0.22 and rnd(seed, ix + ox * 7, (iz + oz * 11) * 7919 + iy + oy * 5) < 0.30:
                        continue                                  # small sky holes inside
                    k = ACCENT if (accent > 0 and q > 0.45 and rnd(seed, ox * 13 + tag, oz * 29 + oy) < accent) else LEAF
                    put_soft(ix + ox, iy + oy, iz + oz, k)

    # ---------------------------------------------------------------- crown: limbs (broadleaf)
    if sp.tiers:
        n = sp.tiers
        limb_no = 0
        for i in range(n):
            frac = 0.40 + 0.52 * (i / float(max(1, n - 1))) if n > 1 else 0.75
            y0 = int(round(trunk_top * frac))
            rad = sp.crown[min(i, len(sp.crown) - 1)]
            count = sp.branches if sp.branches else 3
            for b in range(count):
                a = (b * 2 * math.pi / count) + (0.0 if i % 2 == 0 else math.pi / count) + rnd(seed, i, 70 + b) * 0.55
                L = max(2, int(round(rad * (0.72 + 0.30 * rnd(seed, i, 71 + b)))))
                ax0 = centers[min(len(centers) - 1, max(0, y0))][0]
                az0 = centers[min(len(centers) - 1, max(0, y0))][1]
                rise = 0.50 + 0.35 * rnd(seed, i, 75 + b)
                for k in range(1, L + 1):
                    bx = int(round(ax0 + math.cos(a) * k))
                    bz = int(round(az0 + math.sin(a) * k))
                    by = y0 + int(round(k * rise))
                    ax = AXIS_X if abs(math.cos(a)) > abs(math.sin(a)) else AXIS_Z
                    put(bx, by, bz, LOG, ax)                       # the limb is a real log, not hidden
                tipx = ax0 + math.cos(a) * L
                tipz = az0 + math.sin(a) * L
                tipy = y0 + L * rise
                br = 1.9 + 0.85 * rnd(seed, i, 76 + b) * (rad / 4.0)
                blob(tipx, tipy, tipz, br, max(1.4, br * (0.65 + sp.squash)), br * (0.85 + 0.3 * rnd(seed, i, 77 + b)),
                     i * 31 + b, sp.accent)
                limb_no += 1
                # a fork: a shorter side limb with its own smaller cloud
                if rnd(seed, i, 78 + b) < 0.55:
                    a2 = a + (0.7 if rnd(seed, i, 79 + b) > 0.5 else -0.7)
                    k0 = max(1, L // 2)
                    for k in range(1, L - k0 + 1):
                        bx = int(round(ax0 + math.cos(a) * k0 + math.cos(a2) * k))
                        bz = int(round(az0 + math.sin(a) * k0 + math.sin(a2) * k))
                        by = y0 + int(round((k0 + k) * rise))
                        ax = AXIS_X if abs(math.cos(a2)) > abs(math.sin(a2)) else AXIS_Z
                        put(bx, by, bz, LOG, ax)
                    fx = ax0 + math.cos(a) * k0 + math.cos(a2) * (L - k0)
                    fz = az0 + math.sin(a) * k0 + math.sin(a2) * (L - k0)
                    fy = y0 + L * rise
                    fr = br * 0.72
                    blob(fx, fy, fz, fr, max(1.2, fr * 0.8), fr, i * 31 + b + 500, sp.accent)
        # the leader: a cloud right on top of the trunk
        blob(centers[-1][0], trunk_top + 1, centers[-1][1], sp.crown[-1] * 0.95,
             max(1.6, sp.crown[-1] * 0.8), sp.crown[-1] * 0.95, 999, sp.accent)

    # ---------------------------------------------------------------- crown: fern fronds
    if sp.fronds:
        fx, fz = top_x, top_z
        for i in range(sp.fronds):
            a = i * 2 * math.pi / sp.fronds + rnd(seed, i, 901) * 0.5
            L = int(round(2.8 + rnd(seed, i, 902) * 1.8))
            for k in range(1, L + 1):
                bx = int(round(fx + math.cos(a) * k))
                bz = int(round(fz + math.sin(a) * k))
                by = trunk_top - 1 + int(round(math.sin(math.pi * k / (L + 1.0)) * 2.0)) - (k * 2) // 3
                ax = AXIS_X if abs(math.cos(a)) > abs(math.sin(a)) else AXIS_Z
                put(bx, by, bz, LOG, ax)
                # leaf blade of the frond: one block on each side, drooping
                for s in (-1, 1):
                    px = int(round(fx + math.cos(a) * k + math.cos(a + math.pi / 2) * s * 0.9))
                    pz = int(round(fz + math.sin(a) * k + math.sin(a + math.pi / 2) * s * 0.9))
                    put_soft(px, by - (1 if k > L - 2 else 0), pz, LEAF if rnd(seed, i * 7 + k, s + 3) > 0.12 else ACCENT)
                put_soft(bx, by + 1 if k < L - 1 else by, bz, LEAF)
        crown(trunk_top, 1.6, 1.6, 0.6, 0.3, 98, sp.accent)

    # ---------------------------------------------------------------- crown: conifer cone
    if sp.cone:
        base_y = int(trunk_top * 0.30)
        step = 2
        y = base_y
        tier = 0
        while y <= trunk_top + 1:
            t = (y - base_y) / float(max(1, trunk_top + 1 - base_y))
            rad = sp.crown[0] * (1.0 - t) + 0.9
            crown(y, rad, rad, sp.squash, sp.skirt, 200 + tier, sp.accent)
            y += step
            tier += 1
        crown(trunk_top + 2, 0.9, 0.9, 0.8, 0.1, 299, sp.accent)

    out = []
    for (x, y, z), (k, ax) in nodes.items():
        out.append((x, y, z, k, ax))
    return out


def trunk_height(kind: int, h: int, seed: int) -> int:
    sp = SPECS.get(kind)
    if sp is None:
        return h
    return max(sp.hmin, min(sp.hmax, h))
