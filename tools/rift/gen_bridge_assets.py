#!/usr/bin/env python3
"""Rev 7 bridge assets generator.

Writes, deterministically:
  * models/block/bridge_post_<style>.json   - the 2.5-block tower (plinth, shaft, braces, cap, lantern)
  * blockstates/bridge_post.json            - style=0..4 variants
  * models/item/bridge_post.json            - inventory model
  * models/block/bridge_walk.json           - invisible collision cell
  * blockstates/bridge_walk.json            - top=1..16 variants
  * textures/entity/bridge/*.png            - planks per style, cables (rope/chain), diamond safety net

Run:  python3 tools/rift/gen_bridge_assets.py
Palette rule from the user: muted, never bright.
"""
from __future__ import annotations

import json
import os
import random

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ASSETS = os.path.join(ROOT, "mod", "src", "main", "resources", "assets", "riftworld")

# ----------------------------------------------------------------------------- palettes
STYLES = {
    # id: (key, base tex, shaft tex, cap tex, knot tex, glow tex)
    0: ("plains", "ashlar", "dark_beam", "ashlar", "rope_post", "elisar_lantern"),
    1: ("garden", "elisar_bricks_mossy", "sizo_log", "elisar_cobble", "rope_post", "elisar_lantern"),
    2: ("grove", "keep_stone_mossy", "sizo_log", "keep_stone", "rope_post", "elisar_lantern"),
    3: ("burnt", "castle_bricks_cracked", "charred_log", "scorched_stone", "elisar_chain", "magma_meander"),
    4: ("mist", "keep_stone", "keep_stone_mossy", "keep_stone_cracked", "elisar_chain", "elisar_lantern"),
}


def element(frm, to, faces):
    return {"from": [round(v, 2) for v in frm], "to": [round(v, 2) for v in to], "faces": faces}


def all_faces(tex):
    return {d: {"texture": tex} for d in ("north", "south", "east", "west", "up", "down")}


def tower(key, base, shaft, cap, knot, glow):
    """One post: 1x1 footprint, 2 blocks tall model (0..32 px = the whole pylon + lantern).

    Proportions follow the reference photo: a squat plinth, a sturdy shaft with a rope knot, knee braces and a cap,
    and a small lantern on top. Height is kept inside the vanilla 0..32 px range so chunk sections never clip it.
    """
    textures = {
        "particle": base,
        "base": base,
        "shaft": shaft,
        "cap": cap,
        "knot": knot,
        "glow": glow,
    }
    t = {k: "#" + k for k in ("base", "shaft", "cap", "knot", "glow")}
    els = []
    # plinth
    els.append(element([2, 0, 2], [14, 5, 14], all_faces(t["base"])))
    # shaft
    els.append(element([6, 5, 6], [10, 25, 10], all_faces(t["shaft"])))
    # the rope knot / chain wrap where the cable is tied
    els.append(element([5, 13, 5], [11, 16, 11], all_faces(t["knot"])))
    # iron bandage near the cap
    els.append(element([5, 21, 5], [11, 23, 11], all_faces(t["cap"])))
    # knee braces (diagonal look, made of two stepped slabs per corner)
    for dx in (-1, 1):
        for dz in (-1, 1):
            xs = 8 + dx * 3.0
            zs = 8 + dz * 3.0
            els.append(element([min(xs, 8 + dx * 1.5), 18, min(zs, 8 + dz * 1.5)],
                               [max(xs, 8 + dx * 1.5), 22, max(zs, 8 + dz * 1.5)], all_faces(t["shaft"])))
            els.append(element([min(xs, 8 + dx * 2.0), 22, min(zs, 8 + dz * 2.0)],
                               [max(xs, 8 + dx * 2.0), 25, max(zs, 8 + dz * 2.0)], all_faces(t["shaft"])))
    # cap plate
    els.append(element([3, 25, 3], [13, 27, 13], all_faces(t["cap"])))
    # lantern housing + glowing core + little roof
    els.append(element([6, 27, 6], [10, 30, 10], all_faces(t["glow"])))
    els.append(element([5, 30, 5], [11, 32, 11], all_faces(t["cap"])))
    # corner ribs of the lantern cage
    for x in (5.5, 9.5):
        for z in (5.5, 9.5):
            els.append(element([x, 27, z], [x + 1.0, 30, z + 1.0], all_faces(t["cap"])))
    return {
        "credit": "riftworld rev7 bridge generator",
        "textures": textures,
        "elements": els,
    }


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


# ----------------------------------------------------------------------------- textures
def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


def plank_texture(base, seed, grain=1.0, moss=0.0, char=0.0):
    """16x16 muted board texture: 4 boards across, subtle grain, optional moss speckles / charring."""
    rnd = random.Random(seed)
    img = [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]
    for y in range(16):
        board = y // 4
        seam = (y % 4) == 0
        for x in range(16):
            n = rnd.random()
            c = shade(base, 0.90 + 0.16 * n)
            if seam:
                c = shade(base, 0.62 + 0.08 * n)
            elif rnd.random() < 0.14 * grain:
                c = shade(base, 0.78 + 0.12 * n)
            if char > 0 and rnd.random() < char:
                c = shade((34, 28, 25), 0.8 + 0.5 * n)
            if moss > 0 and rnd.random() < moss:
                c = shade((92, 112, 74), 0.85 + 0.3 * n)
            img[y][x] = (*c, 255)
    # plank nail dots at both ends of each board
    for board in range(4):
        for x in (2, 13):
            y = board * 4 + 2
            img[y][x] = (52, 44, 38, 255) if char < 0.3 else (24, 20, 18, 255)
    return img


def cable_texture(base, seed, links=False):
    rnd = random.Random(seed)
    img = [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]
    for y in range(16):
        for x in range(16):
            if links:
                # chain: horizontal links alternating with vertical connectors
                hit = (y % 4 in (0, 3) and 3 <= x <= 12) or (y % 4 in (1, 2) and x in (6, 7, 8, 9))
                c = shade(base, 0.72 + 0.35 * rnd.random())
            else:
                # rope: three strands twisting (diagonal braid)
                band = (x + y * 2) % 6
                hit = band in (0, 1)
                c = shade(base, 0.78 + 0.30 * rnd.random())
            img[y][x] = (*c, 255) if hit else (0, 0, 0, 0)
    return img


def net_texture(seed):
    """Diamond safety net: transparent cells with a 1px lattice, as on the reference photo."""
    rnd = random.Random(seed)
    img = [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]
    for y in range(16):
        for x in range(16):
            dx = (x + y) % 8
            dy = (x - y) % 8
            if dx in (0,) or dy in (0,):
                g = 118 + int(38 * rnd.random())
                img[y][x] = (g, g, g - 4, 255)
    # a slightly thicker weave every 8 px so it reads as a mesh from afar
    for y in range(16):
        for x in range(16):
            if (x % 8 == 0) or (y % 8 == 0):
                g = 150 + int(30 * rnd.random())
                img[y][x] = (g, g, g - 4, 235)
    return img


def save_png(img, path):
    try:
        from PIL import Image  # noqa: F401
    except ImportError:  # pragma: no cover
        raise SystemExit("Pillow is required: pip install pillow")
    from PIL import Image
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im = Image.new("RGBA", (16, 16))
    im.putdata([px for row in img for px in row])
    im.save(path)


PLANKS = {
    "planks_plains": ((139, 121, 98), 11, 0.0, 0.0),
    "planks_garden": ((155, 139, 126), 21, 0.03, 0.0),
    "planks_grove": ((196, 191, 173), 31, 0.05, 0.0),
    "planks_burnt": ((74, 62, 54), 41, 0.0, 0.35),
    "planks_mist": ((151, 155, 152), 51, 0.02, 0.0),
}


def main():
    # ---- post models + blockstate
    variants = {}
    for sid, (key, base, shaft, cap, knot, glow) in STYLES.items():
        write_json(os.path.join(ASSETS, "models", "block", f"bridge_post_{key}.json"),
                   tower(key, base, shaft, cap, knot, glow))
        variants[f"style={sid}"] = {"model": f"riftworld:block/bridge_post_{key}"}
    write_json(os.path.join(ASSETS, "blockstates", "bridge_post.json"), {"variants": variants})
    write_json(os.path.join(ASSETS, "models", "item", "bridge_post.json"),
               {"parent": "riftworld:block/bridge_post_plains"})

    # ---- invisible collision cells (16 top heights)
    write_json(os.path.join(ASSETS, "models", "block", "bridge_walk.json"),
               {"textures": {"particle": "riftworld:block/bridge_post"}})
    walk_variants = {f"top={i}": {"model": "riftworld:block/bridge_walk"} for i in range(1, 17)}
    write_json(os.path.join(ASSETS, "blockstates", "bridge_walk.json"), {"variants": walk_variants})

    # ---- old cube model is gone (the tower replaces it)
    old = os.path.join(ASSETS, "models", "block", "bridge_post.json")
    if os.path.exists(old):
        os.remove(old)

    # ---- entity textures
    tex = os.path.join(ASSETS, "textures", "entity", "bridge")
    for name, (base, seed, moss, char) in PLANKS.items():
        save_png(plank_texture(base, seed, moss=moss, char=char), os.path.join(tex, name + ".png"))
    save_png(cable_texture((156, 132, 96), 61), os.path.join(tex, "cable_rope.png"))
    save_png(cable_texture((150, 148, 142), 71, links=True), os.path.join(tex, "cable_chain.png"))
    save_png(net_texture(81), os.path.join(tex, "net.png"))
    print("bridge assets written to", ASSETS)


if __name__ == "__main__":
    main()
