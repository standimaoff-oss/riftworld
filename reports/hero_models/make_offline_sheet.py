#!/usr/bin/env python3
"""Собирает два оффлайн-листа моделей в одну подписанную витрину.

Требует Pillow; запускать из корня репозитория после gen_v041_*.py и
preview_mob_model.py <name> --scale=1.15 --sheet.
"""
import json
import pathlib

from PIL import Image, ImageDraw, ImageFont

ROOT = pathlib.Path(__file__).resolve().parents[2]
HERE = ROOT / "reports/hero_models"
OUT = HERE / "01_offline_model_sheets.png"

W, HEADER, ROW, FOOT = 1680, 120, 730, 70
H = HEADER + ROW * 2 + FOOT
BG = (18, 23, 30)
PANEL = (26, 33, 42)
TEXT = (235, 239, 242)
MUTED = (150, 164, 176)
ACCENT = (201, 162, 74)


def font(size, bold=False):
    name = "DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"
    return ImageFont.truetype(f"/usr/share/fonts/truetype/dejavu/{name}", size)


def main():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, W, HEADER), fill=PANEL)
    d.text((40, 28), "RIFTWORLD · ПЕРЕРАБОТКА МОДЕЛЕЙ", font=font(38, True), fill=TEXT)
    d.text((42, 78), "Оффлайн-рендер игровых geo/атласов · 4 ракурса на модель",
           font=font(20), fill=MUTED)

    for i, name in enumerate(("pegasus", "liopleurodon")):
        y = HEADER + i * ROW
        d.rectangle((24, y + 12, W - 24, y + ROW - 12), fill=PANEL)
        stats = json.loads((HERE / f"{name}_stats.json").read_text(encoding="utf-8"))
        title = "ПЕГАС" if name == "pegasus" else "ЛИОПЛЕВРОДОН"
        d.text((48, y + 24), title, font=font(28, True), fill=ACCENT)
        d.text((48, y + 62),
               f"{stats['bones']} костей · {stats['cubes']} кубоидов · атлас "
               f"{stats['atlas']}×{stats['atlas']} · {stats['size_blocks'][0]}×"
               f"{stats['size_blocks'][1]}×{stats['size_blocks'][2]} блоков",
               font=font(19), fill=MUTED)
        sheet = Image.open(ROOT / f"reports/{name}_model_sheet.png").convert("RGB")
        sheet.thumbnail((W - 90, ROW - 130), Image.Resampling.LANCZOS)
        px = (W - sheet.width) // 2
        py = y + 105 + (ROW - 130 - sheet.height) // 2
        img.paste(sheet, (px, py))

    d.text((40, H - 48),
           "Проекты: pegasus.bbmodel / liopleurodon.bbmodel · Bedrock geometry: *.blockbench.geo.json",
           font=font(18), fill=MUTED)
    img.save(OUT, optimize=True)
    print(OUT.relative_to(ROOT))


if __name__ == "__main__":
    main()
