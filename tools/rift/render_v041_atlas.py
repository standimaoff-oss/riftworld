#!/usr/bin/env python3
"""Render a nearest-neighbour contact sheet of the actual v0.41 item textures."""
from pathlib import Path
import importlib.util

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("armory", ROOT / "tools/rift/gen_v041_elisar_armory.py")
g = importlib.util.module_from_spec(spec)
spec.loader.exec_module(g)


def blit(dst, src, ox, oy, scale):
    for y in range(src.h):
        for x in range(src.w):
            color = src.get(x, y)
            if color[3]:
                dst.rect(ox + x * scale, oy + y * scale, ox + (x + 1) * scale - 1, oy + (y + 1) * scale - 1, color)


sheet = g.Canvas(800, 704, (20, 23, 30, 255))
rows = [("aurite", g.AURITE), ("riftcrystal", g.RIFT)]
for row, (material, palette) in enumerate(rows):
    for subrow in range(2):
        y = row * 352 + subrow * 176
        sheet.rect(0, y, 799, y + 7, palette["accent"] if subrow == 0 else palette["base"])
        for col in range(5):
            x = col * 160
            sheet.rect(x + 8, y + 16, x + 151, y + 167, (31, 35, 45, 255))
            sheet.rect(x + 10, y + 18, x + 149, y + 20, palette["shadow"])
            if subrow == 0:
                item = [g.make_sword, g.make_pickaxe, g.make_axe, g.make_bow, g.make_shield][col](palette)
            else:
                item = g.make_armor_icon(("helmet", "chestplate", "leggings", "boots")[col], palette) if col < 4 else g.make_ingot(palette, material == "riftcrystal")
            blit(sheet, item, x + 16, y + 24, 4)

sheet.save(ROOT / "reports/elisar_armory/equipment_atlas.png")
print(ROOT / "reports/elisar_armory/equipment_atlas.png")
