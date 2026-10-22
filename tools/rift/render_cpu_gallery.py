#!/usr/bin/env python3
"""Галерея CPU-рендеров настоящего мира: камера наводится на объект, кадры складываются в контактный лист.

Это витрина `render_cpu.py` (numpy-трассировка лучей, без видеокарты, без OpenGL и без запущенного клиента):
на кадрах ровно те блоки, что лежат в сохранении мира после генерации модом — мостовые опоры, сирень, террасы,
храм. Высота камеры считается от реального рельефа, поэтому вид не «висит в небе».

Usage:
    python3 tools/rift/render_cpu_gallery.py                 # все сюжеты, 1024x576
    python3 tools/rift/render_cpu_gallery.py --quick         # 640x360 (быстрая проверка кадров)
    python3 tools/rift/render_cpu_gallery.py --only bridge   # один сюжет
"""
from __future__ import annotations

import argparse
import subprocess
import sys
import time
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
OUT = ROOT / "reports/rev9_cpu"
DEFAULT_REGION = Path("/tmp/mcserver/world/dimensions/riftworld/elisar/region")

# id, центр (x,z), радиус обзора, дистанция камеры, добавка к высоте глаз, поворот, подпись
SUBJECTS = [
    ("01_violet_hills", 1327, 225, 110, 95, 45, 205,
     "Биом 04 «Фиолетовые холмы»: сиреневые деревья, церкви и река"),
    ("02_violet_bridge", 1384, 240, 44, 26, 20, 315,
     "Мост через реку в биоме 04: 4 опоры, настил (рендер-геометрия) и коллизия"),
    ("03_sizo_grove", 836, -2316, 110, 100, 45, 200,
     "Биом 05 «Роща Сизо»: сизые деревья на острове"),
    ("04_gardens", -1200, -1210, 120, 110, 50, 195,
     "Биом 07 «Висячие сады»: ступенчатые террасы и перьевые деревья"),
    ("05_gardens_bridge", 3510, -4184, 62, 40, 26, 160,
     "Мост через промоину в биоме 07"),
    ("06_mist_keep", 1837, 1120, 64, 46, 26, 200,
     "Туманный дозор: сосны на каменных останцах"),
    ("07_temple", 8, 8, 96, 105, 40, 215,
     "Великий храм перехода (центр карты)"),
]


def surface_span(region: Path, cx: int, cz: int, radius: int) -> tuple[int, int]:
    """Куда ставить камеру: ПО ТИПИЧНОЙ высоте столбцов, а не по максимуму.

    Максимум обманывает: в этих чанках летает мелкий мусор (отдельные блоки над островом), и камера,
    наведённая на самый верхний блок, смотрит в пустое небо, а остров уезжает под нижний край кадра.
    Берём 90-й процентиль высот столбцов — это и есть «крыша» объекта.
    """
    sys.path.insert(0, str(HERE))
    import render_cpu as R
    w = R.World(region)
    g = w.load(cx - radius, cz - radius, cx + radius, cz + radius, 40, 200)
    air = np.array([n.endswith("air") for n in w.names])
    occ = ~air[g]
    tops = []
    for i in range(occ.shape[1]):
        for j in range(occ.shape[2]):
            col = np.flatnonzero(occ[:, i, j])
            if col.size:
                tops.append(40 + int(col[-1]))
    if not tops:
        return 90, 110
    tops = np.array(tops)
    lo = 40 + int(np.flatnonzero(occ.any(axis=(1, 2)))[0])
    hi = int(np.percentile(tops, 90))
    return lo, hi


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--quick", action="store_true")
    ap.add_argument("--only", default=None)
    ap.add_argument("--region", default=str(DEFAULT_REGION))
    ap.add_argument("--out", default=str(OUT))
    args = ap.parse_args()

    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    W, H = (640, 360) if args.quick else (1024, 576)
    made = []
    t_all = time.time()
    for name, cx, cz, radius, dist, eye_dy, yaw, caption in SUBJECTS:
        if args.only and args.only not in name:
            continue
        lo, hi = surface_span(Path(args.region), cx, cz, radius)
        target_y = hi - 2
        eye_y = hi + eye_dy
        out_png = out / f"{name}.png"
        cmd = [sys.executable, str(HERE / "render_cpu.py"), "--dim", args.region,
               "--centre", str(cx), str(cz), "--radius", str(radius), "--size", f"{W}x{H}",
               "--yaw", str(yaw), "--dist", str(dist), "--eye-y", str(eye_y),
               "--target-y", str(target_y), "--out", str(out_png)]
        t0 = time.time()
        r = subprocess.run(cmd, capture_output=True, text=True)
        if r.returncode != 0:
            print(f"[FAIL] {name}: {r.stdout[-400:]} {r.stderr[-600:]}")
            return 1
        made.append((out_png, caption))
        print(f"[OK] {name}: {out_png.name} ({out_png.stat().st_size // 1024} КБ, {time.time() - t0:.0f} с, "
              f"рельеф y {lo}..{hi}, глаз {eye_y})")

    if len(made) > 1:
        cols = 2
        tw, th = 640, 360
        pad, cap_h = 8, 26
        rows = (len(made) + cols - 1) // cols
        sheet = Image.new("RGB", (cols * tw + (cols + 1) * pad, rows * (th + cap_h + pad) + pad), (18, 20, 24))
        d = ImageDraw.Draw(sheet)
        try:
            font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 15)
        except Exception:
            font = ImageFont.load_default()
        for i, (p, cap) in enumerate(made):
            r_i, c_i = divmod(i, cols)
            x = pad + c_i * (tw + pad)
            y = pad + r_i * (th + cap_h + pad)
            sheet.paste(Image.open(p).convert("RGB").resize((tw, th), Image.LANCZOS), (x, y))
            d.text((x + 2, y + th + 4), cap, fill=(226, 226, 230), font=font)
        sheet_path = out / "contact_sheet.png"
        sheet.save(sheet_path)
        print(f"[лист] {sheet_path} ({sheet_path.stat().st_size // 1024} КБ)")
    print(f"всего {time.time() - t_all:.0f} с на CPU (без видеокарты)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
