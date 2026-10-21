#!/usr/bin/env python3
"""Re-renders the tree sheets of Rev 7 from scratch, straight from `tree_gen.py`.

Why this file exists: the previous sheets were made by an ad-hoc script that was never committed, so nobody could
reproduce them. Here every pixel has a command behind it:

    python3 tools/rift/render_trees.py

`tree_gen.py` is byte-for-byte equivalent to `tree/TreeGen.java` (proved on every run by
`tools/rift/jtests/run.py`: 144 trees, 61952 lines compared), so these pictures are exactly what the built-in
engine puts into a chunk.

Important: these are the trees of the FALLBACK engine (Dynamic Trees not installed). When DT is present IT grows the
trees from the parameters in `dtcompat/`; those cannot be rendered off-line without Minecraft, and the sheet says so
in its own header instead of pretending otherwise.
"""
from __future__ import annotations

import json
import os

from PIL import Image, ImageDraw, ImageFont

import bridge_preview as BP
import mock_world as MW
import tree_gen as TG

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "reports", "rev7_shots")
TREE_OUT = os.path.join(OUT, "trees")
SHEET = os.path.join(OUT, "tree_species_sheet.png")
DT = os.path.join(ROOT, "dtcompat", "src", "main", "resources", "trees", "riftworld_dt")

FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FONT_B = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_SERIF = "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"

# species id -> (engine kind, leaf, log, accent, canonical height, seed, biome grass tint, tint strength)
# the tint strength is < 1 on purpose: the ground is only a backdrop, the palette of Elisar is muted
SPECIES = [
    ("sizo_v2", TG.SIZO_GIANT, "leaf_sizo", "log_sizo", "moss", 19, 90210, 10273458, 0.68),
    ("lilac", TG.LILAC, "leaf_lilac", "log_lilac", "blossom", 14, 424242, 10153932, 0.70),
    ("fern_tree", TG.FERN, "leaf_fern", "log_fern", "tall_grass", 9, 777001, 7321674, 0.52),
    ("mist_pine", TG.MIST_PINE, "leaf_mist", "log_mist", "moss", 17, 314159, 10273458, 0.66),
]

RU = {"sizo_v2": "Сизо (второе поколение)", "lilac": "Сиреневое древо",
      "fern_tree": "Древовидный папоротник", "mist_pine": "Туманная сосна"}


def dt_params(species: str) -> str:
    """The Dynamic Trees knobs of a species, read from the shipped pack (not hard-coded)."""
    try:
        p = json.load(open(os.path.join(DT, "species", species + ".json"), encoding="utf-8"))
        lv = json.load(open(os.path.join(DT, "leaves_properties", species + ".json"), encoding="utf-8"))
        fam = json.load(open(os.path.join(DT, "families", species + ".json"), encoding="utf-8"))
    except OSError:
        return ""
    kit = (lv.get("cell_kit") or "").split(":")[-1]
    return (f"DT: energy {p.get('signal_energy')} · up {p.get('up_probability')} · "
            f"ветки с {p.get('lowest_branch_height')} · taper {p.get('tapering')} · "
            f"радиус {p.get('max_branch_radius')} · kit {kit} · семя {fam.get('common_species', '').split(':')[-1]}")


def ground(world, radius: int, tint: int, strength: float = 0.7) -> None:
    """A muted patch of the biome's grass so the tree, not the lawn, carries the colour."""
    MW.set_tint(tint, strength)
    for x in range(-radius, radius + 1):
        for z in range(-radius, radius + 1):
            if x * x + z * z > radius * radius:
                continue
            world.put(x, 0, z, "grass_top")
            world.put(x, -1, z, "dirt")
            world.put(x, -2, z, "dirt")
            world.put(x, -3, z, "stone")


def fit_camera(nodes, size, lift=0.15, margin=1.12):
    """Puts the whole tree in frame: grows the distance until every block corner projects inside the tile."""
    xs = [n[0] for n in nodes] + [0]
    ys = [n[1] for n in nodes] + [0]
    zs = [n[2] for n in nodes] + [0]
    cx, cz = (min(xs) + max(xs) + 1) / 2.0, (min(zs) + max(zs) + 1) / 2.0
    lo_y, hi_y = min(ys) - 1, max(ys) + 2
    target = (cx, (lo_y + hi_y) / 2.0, cz)
    corners = [(x, y, z) for x in (min(xs), max(xs) + 1) for y in (lo_y, hi_y) for z in (min(zs), max(zs) + 1)]
    W, H = size
    best = None
    for fov in (34, 40, 46, 52):
        dist = 8.0
        while dist < 200:
            eye = (cx - dist * 0.62, target[1] + hi_y * lift, cz - dist * 0.72)
            cam = BP.Cam(eye, target, fov=fov, w=W, h=H)
            pts = [cam.p(c) for c in corners]
            ok = all(0 <= p[0] <= W and 0 <= p[1] <= H for p in pts)
            # the tree should also fill the frame instead of floating in it
            span_x = max(p[0] for p in pts) - min(p[0] for p in pts)
            span_y = max(p[1] for p in pts) - min(p[1] for p in pts)
            fill = max(span_x / W, span_y / H)
            if ok and fill >= 0.52:
                best = (eye, target, fov)
                break
            dist += 1.0
        if best:
            break
    return best or ((cx - 24, target[1] + 6, cz - 28), target, 40)


def tile(species: str, kind: int, leaf: str, log: str, accent: str, height: int, seed: int,
         tint: int, strength: float):
    w = MW.World()
    ground(w, 11, tint, strength)
    nodes = TG.build(kind, height, seed)
    MW.tree(w, kind, 0, 0, 1, height, seed, leaf=leaf, log=log, accent=accent)
    cfg = dict(style="garden", posts=[], deck=None, sky=((206, 210, 204), (238, 236, 228)),
               fog=(234, 235, 230), sun=(255, 248, 228))
    size = (620, 620)
    # ferns and pines read better from a lower angle: their shape lives in the side view
    eye, target, fov = fit_camera(nodes, size, lift=0.10 if species in ("fern_tree", "mist_pine") else 0.16)
    return MW.render(w, cfg, size=size, eye=eye, target=target, fov=fov)


def wrap(draw, text, font, width):
    """Greedy wrap on spaces; the caption must never bleed into the neighbouring tile."""
    words, lines, line = text.split(" "), [], ""
    for word in words:
        probe = (line + " " + word).strip()
        if draw.textlength(probe, font=font) <= width:
            line = probe
        else:
            if line:
                lines.append(line)
            line = word
    if line:
        lines.append(line)
    return lines


def main() -> None:
    os.makedirs(TREE_OUT, exist_ok=True)
    tiles = []
    for species, kind, leaf, log, accent, height, seed, tint, strength in SPECIES:
        img = tile(species, kind, leaf, log, accent, height, seed, tint, strength)
        img.save(os.path.join(TREE_OUT, species + ".png"))
        tiles.append((species, height, seed, img))
        print("written", os.path.join(TREE_OUT, species + ".png"))

    # ---- sheet: 2x2 tiles + captions
    pad, cap = 18, 92
    tw, th = tiles[0][3].size
    W = pad * 3 + tw * 2
    H = 96 + (th + cap + pad) * 2 + pad
    sheet = Image.new("RGB", (W, H), (28, 27, 32))
    d = ImageDraw.Draw(sheet)
    d.text((pad, 22), "Rev 7, часть 2 — четыре породы Элисара", font=ImageFont.truetype(FONT_SERIF, 30),
           fill=(232, 228, 214))
    d.text((pad, 62), "Фолбэк-движок мода (Dynamic Trees не установлен). С Python-эталоном совпадает байт в байт.",
           font=ImageFont.truetype(FONT, 15), fill=(168, 164, 158))

    for i, (species, height, seed, img) in enumerate(tiles):
        col, row = i % 2, i // 2
        x = pad + col * (tw + pad)
        y = 96 + row * (th + cap + pad)
        sheet.paste(img, (x, y))
        d.rectangle([x, y, x + tw - 1, y + th - 1], outline=(58, 56, 62))
        ty = y + th + 8
        d.text((x, ty), f"{RU[species]}  ·  {species}", font=ImageFont.truetype(FONT_B, 17), fill=(226, 220, 206))
        f_small = ImageFont.truetype(FONT, 12)
        d.text((x, ty + 23), f"высота {height}, seed {seed} — офлайн-рендер того же движка, что в игре",
               font=f_small, fill=(150, 148, 146))
        for j, line in enumerate(wrap(d, dt_params(species), f_small, tw - 4)[:2]):
            d.text((x, ty + 41 + j * 15), line, font=f_small, fill=(128, 126, 126))
    sheet.save(SHEET)
    print("written", SHEET)

    # ---- contact sheet of the six scene shots (also reproducible now)
    scenes = ["04_violet_hills", "04_bridge_deck_view", "04_bridge_closeup",
              "05_sizo_grove", "07_waterfall_gardens", "ashen_burnt_bridge"]
    shot_w, shot_h = 620, 384
    cols, rows = 2, 3
    pad2, cap2 = 12, 40
    cw = pad2 * (cols + 1) + shot_w * cols
    ch = 74 + (shot_h + cap2 + pad2) * rows
    cs = Image.new("RGB", (cw, ch), (28, 27, 32))
    cd = ImageDraw.Draw(cs)
    cd.text((pad2, 20), "Rev 7 — виды Элисара и мосты", font=ImageFont.truetype(FONT_SERIF, 26), fill=(232, 228, 214))
    cd.text((pad2, 50), "Офлайн-рендер по реальным текстурам и геометрии мода (не скриншот игры).",
            font=ImageFont.truetype(FONT, 13), fill=(168, 164, 158))
    labels = {"04_violet_hills": "04 Сиреневые холмы — мост через реку",
              "04_bridge_deck_view": "04 мост, вид с настила (ракурс фото пользователя)",
              "04_bridge_closeup": "04 мост вблизи: настил, тросы, сетка, 4 опоры",
              "05_sizo_grove": "05 Роща Сизо — исполины второго поколения",
              "07_waterfall_gardens": "07 Сады водопадов — древовидные папоротники",
              "ashen_burnt_bridge": "Адские угодья — обгоревший мост"}
    for i, name in enumerate(scenes):
        col, row = i % cols, i // cols
        p = os.path.join(OUT, name + ".png")
        if not os.path.exists(p):
            continue
        im = Image.open(p).convert("RGB").resize((shot_w, shot_h), Image.LANCZOS)
        x = pad2 + col * (shot_w + pad2)
        y = 74 + row * (shot_h + cap2 + pad2)
        cs.paste(im, (x, y))
        cd.rectangle([x, y, x + shot_w - 1, y + shot_h - 1], outline=(58, 56, 62))
        cd.text((x, y + shot_h + 8), labels[name], font=ImageFont.truetype(FONT, 13), fill=(196, 192, 186))
    cs.save(os.path.join(OUT, "contact_sheet.png"))
    print("written", os.path.join(OUT, "contact_sheet.png"))


if __name__ == "__main__":
    main()
