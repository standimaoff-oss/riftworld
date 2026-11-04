#!/usr/bin/env python3
"""Generate v0.22.0 PNG textures using pure Python stdlib (struct + zlib, no PIL)."""
import math
import os
import struct
import zlib


def write_png(path: str, w: int, h: int, rgba_bytes: bytes) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    raw = bytearray()
    stride = w * 4
    for y in range(h):
        raw.append(0)  # filter type 0 (None)
        raw.extend(rgba_bytes[y * stride : (y + 1) * stride])

    def chunk(tag: bytes, data: bytes) -> bytes:
        return (
            struct.pack(">I", len(data))
            + tag
            + data
            + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
        )

    ihdr = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)


def clamp8(v: float) -> int:
    return 0 if v <= 0 else (255 if v >= 255 else int(round(v)))


def hash2(ix: int, iy: int, seed: int = 0) -> float:
    n = (ix * 374761393 + iy * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFFFF) / float(0x1000000)


def gen_white_spatial_rift(path: str) -> None:
    """Pure white / Elisar alabaster-pearl geometric breach texture (ZERO cyan/blue)."""
    w, h = 256, 256
    buf = bytearray(w * h * 4)
    for y in range(h):
        v = y / float(h)
        ang_v = v * math.tau
        for x in range(w):
            u = x / float(w)
            # Distance from center spine (u = 0.5) with crisp angular zig-zag geometric facets!
            tri1 = abs(((v * 6.0) % 1.0) * 2.0 - 1.0)
            tri2 = abs(((v * 13.0 + 0.35) % 1.0) * 2.0 - 1.0)
            zig = (tri1 - 0.5) * 0.055 + (tri2 - 0.5) * 0.028
            d = abs(u - 0.5 + zig) * 2.0  # 0 at center, ~1 at outer edge

            # Sharp geometric facet cells (Voronoi-like angular planes)
            gx = u * 9.0 + (tri1 - 0.5) * 0.8
            gy = v * 14.0
            cx_i, cy_i = int(math.floor(gx)), int(math.floor(gy))
            fx, fy = gx - cx_i, gy - cy_i
            facet_edge = min(fx, 1.0 - fx, fy, 1.0 - fy, abs(fx - fy) * 0.72, abs(fx + fy - 1.0) * 0.72)
            crease = max(0.0, 1.0 - facet_edge / 0.085)
            cell_tone = (hash2(cx_i % 9, cy_i % 14, 17) - 0.5) * 14.0

            # Outer jagged geometric edge mask
            edge_limit = 0.76 + 0.16 * (tri1 - 0.5) + 0.08 * math.sin(ang_v * 3.0)
            if d >= edge_limit:
                alpha = 0.0
            else:
                t = d / max(0.05, edge_limit)
                # Crisp faceted silhouette rather than blurry smoke
                alpha = min(1.0, max(0.0, (1.0 - t ** 2.4) * 1.18))

            # Pure incandescent white core -> warm alabaster/Elisar pearl facets -> crisp silver-ivory bevel
            core = max(0.0, 1.0 - (d / 0.34) ** 1.5)
            r = 244.0 + 11.0 * core + cell_tone * 0.55 - crease * 20.0
            g = 240.0 + 15.0 * core + cell_tone * 0.52 - crease * 22.0
            b = 231.0 + 24.0 * core + cell_tone * 0.48 - crease * 25.0

            # Subtle luminous white fracture veins
            if crease > 0.55 and d < 0.55:
                r += 14.0
                g += 13.0
                b += 11.0

            idx = (y * w + x) * 4
            buf[idx] = clamp8(r)
            buf[idx + 1] = clamp8(g)
            buf[idx + 2] = clamp8(b)
            buf[idx + 3] = clamp8(alpha * 255.0)
    write_png(path, w, h, bytes(buf))


def gen_portal_open_seal(path: str) -> None:
    """Ornate concentric Elisar sacred-geometry & runic pattern around the opening/open white breach."""
    w, h = 256, 256
    buf = bytearray(w * h * 4)
    for y in range(h):
        dy = (y + 0.5) / h * 2.0 - 1.0
        for x in range(w):
            dx = (x + 0.5) / w * 2.0 - 1.0
            r = math.hypot(dx, dy)
            idx = (y * w + x) * 4
            if r > 0.99 or r < 0.03:
                continue
            ang = math.atan2(dy, dx)

            val = 0.0
            # Concentric sacred circles
            for rr, w_ring in ((0.95, 0.016), (0.88, 0.011), (0.70, 0.015), (0.62, 0.010), (0.42, 0.014), (0.22, 0.016), (0.11, 0.018)):
                val = max(val, max(0.0, 1.0 - abs(r - rr) / w_ring))

            # 12-pointed geometric star polygon (2 overlapping hexagrams / dodecagram)
            for rot in (0.0, math.pi / 6.0, math.pi / 12.0):
                k_ang = ((ang + rot) % (math.pi / 3.0)) - (math.pi / 6.0)
                poly_r = 0.56 / max(0.25, math.cos(k_ang))
                if r <= 0.89:
                    val = max(val, max(0.0, 1.0 - abs(r - poly_r * 0.88) / 0.013) * 0.92)

            # 16 radial astrolabe spokes
            spoke = abs(math.sin(ang * 8.0))
            if 0.22 <= r <= 0.88:
                val = max(val, max(0.0, 1.0 - (spoke * r) / 0.011) * 0.82)

            # Ornate Elisar rune glyphs in outer ring (0.72 .. 0.86) and mid ring (0.45 .. 0.59)
            for r_lo, r_hi, sectors in ((0.73, 0.85, 24), (0.45, 0.59, 16)):
                if r_lo <= r <= r_hi:
                    u_sec = ((ang + math.pi) / math.tau) * sectors
                    s_id = int(math.floor(u_sec))
                    su = u_sec - s_id
                    sv = (r - r_lo) / (r_hi - r_lo)
                    if 0.16 <= su <= 0.84 and 0.14 <= sv <= 0.86:
                        gx = int((su - 0.16) / 0.68 * 5.0)
                        gy = int((sv - 0.14) / 0.72 * 5.0)
                        sym_x = min(gx, 4 - gx)
                        on = hash2(sym_x + s_id * 3, gy, 91) > 0.42
                        if on:
                            val = max(val, 0.90)

            # Inner luminous halo near the breach stem
            halo = max(0.0, 1.0 - r / 0.26) ** 2.0 * 0.65
            a = min(1.0, val + halo)
            if a <= 0.01:
                continue

            # Warm Elisar pearl-white & soft pale gold filigree
            gold_mix = min(1.0, max(0.0, (r - 0.18) * 1.1))
            cr = 252.0 - 8.0 * gold_mix
            cg = 247.0 - 18.0 * gold_mix
            cb = 235.0 - 44.0 * gold_mix

            buf[idx] = clamp8(cr)
            buf[idx + 1] = clamp8(cg)
            buf[idx + 2] = clamp8(cb)
            buf[idx + 3] = clamp8(a * 242.0)
    write_png(path, w, h, bytes(buf))


def gen_origami_washi(path: str) -> None:
    """Handmade cream-ivory Japanese washi paper texture with crisp fold crease & tuck-pocket shading."""
    w, h = 128, 128
    buf = bytearray(w * h * 4)
    for y in range(h):
        v = y / float(h - 1)  # v=0..1 across module width; v=0.5 is crisp mountain fold ridge!
        for x in range(w):
            u = x / float(w - 1)  # u=0 base tucked in previous pocket, u=1 pointed module tip!
            # Subtle warm paper fiber grain
            f1 = (hash2(x, y, 3) - 0.5) * 5.5
            f2 = (hash2(x // 2, y // 4, 7) - 0.5) * 4.5
            fiber = (f1 + f2) * 0.75

            # Crisp central mountain-fold highlight along v = 0.5
            ridge = math.exp(-((v - 0.5) / 0.065) ** 2) * 8.0
            # Delicate paper edge bevel along v=0 and v=1 + base pocket shadow near u=0
            edge_dist = min(v, 1.0 - v)
            edge_shadow = max(0.0, 1.0 - edge_dist / 0.09) * 18.0
            pocket_ao = max(0.0, 1.0 - u / 0.22) * 22.0

            base_r, base_g, base_b = 248.0, 243.0, 231.0
            r = base_r + ridge - edge_shadow - pocket_ao + fiber
            g = base_g + ridge - edge_shadow * 1.04 - pocket_ao * 1.05 + fiber * 0.96
            b = base_b + ridge - edge_shadow * 1.12 - pocket_ao * 1.14 + fiber * 0.90

            idx = (y * w + x) * 4
            buf[idx] = clamp8(r)
            buf[idx + 1] = clamp8(g)
            buf[idx + 2] = clamp8(b)
            buf[idx + 3] = 255
    write_png(path, w, h, bytes(buf))


if __name__ == "__main__":
    root = "mod/src/main/resources/assets/riftworld/textures"
    gen_white_spatial_rift(f"{root}/environment/spatial_rift.png")
    gen_portal_open_seal(f"{root}/environment/portal_open_seal.png")
    gen_origami_washi(f"{root}/entity/origami_washi.png")
    print("Generated v0.22.0 textures OK")
