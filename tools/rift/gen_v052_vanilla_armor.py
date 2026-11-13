#!/usr/bin/env python3
"""v0.52.0 — броня мода с ВАНИЛЬНОЙ геометрией и ВАНИЛЬНЫМИ текстурами.

Ключевой принцип (пользователь: «в прошлый раз броня была сплошной без геометрии огромной»):

1. ГЕОМЕТРИЯ — ровно ванильная. Никаких кастомных 3D-моделей: броня рендерится
   стандартным ванильным HumanoidArmorLayer (ModelLayers.PLAYER_INNER_ARMOR /
   PLAYER_OUTER_ARMOR). Кубы, UV-развёртка и посадка на тело — один в один
   ванильные, как у diamond/netherite/iron брони.

2. ТЕКСТУРЫ — по принципу ванильных. Ванильная броня красится как
   «*_layer_1.png» / «*_layer_2.png» (64x32, раскладка player-skin), где каждая
   грань имеет своё освещение (верх светлее, бок темнее). Здесь мы НЕ рисуем
   текстуры с нуля и НЕ делаем «сплошную заливку»: мы берём настоящие
   ванильные атласы (diamond / netherite / leather) и перекрашиваем их в
   палитры мода, сохраняя покадровое освещение и все пиксели.

3. МАТЕРИАЛ — реализация net.minecraft.world.item.ArmorMaterial (Forge 1.20.1
   это интерфейс). getName() возвращает "riftworld:<материал>", поэтому Forge
   сам резолвит текстуру как riftworld:textures/models/armor/<материал>_layer_1.png
   — ровно тот же механизм, что у ванильной брони.

Результат:
  mod/src/main/resources/assets/riftworld/textures/models/armor/<mat>_layer_1.png
  mod/src/main/resources/assets/riftworld/textures/models/armor/<mat>_layer_2.png
  mod/src/main/resources/assets/riftworld/textures/item/<mat>_<part>.png  (16x16)
  mod/src/main/resources/assets/riftworld/models/item/<mat>_<part>.json

Запуск:  PYTHONPATH=.pylibs python3 tools/rift/gen_v052_vanilla_armor.py
"""
from __future__ import annotations

import json
import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / ".pylibs"))

from PIL import Image  # noqa: E402

RES = ROOT / "mod/src/main/resources/assets/riftworld"
ARMOR_TEX = RES / "textures/models/armor"
ITEM_TEX = RES / "textures/item"
ITEM_MODEL = RES / "models/item"

VANILLA = ROOT / "tools/armor_editor/data/vanilla_armor"
if not VANILLA.exists():
    VANILLA = Path("/tmp/vanilla_assets")

# --------------------------------------------------------------------------
# Палитры мода. Базовые цвета заданы ярко; перекраска идёт по HSV:
# сохраняем ОТНОСИТЕЛЬНУЮ светлоту исходного пикселя (ванильное освещение граней),
# но меняем тон/насыщенность на фирменный цвет материала.
# --------------------------------------------------------------------------
SETS = {
    # --- Аурит: тёплое золото-медь (по мотивам ванильной netherite-подложки,
    #     у которой сложное пятнистое освещение граней — так броня выглядит «слоями»).
    #     netherite сама по себе очень тёмная, поэтому светлоту приводим к
    #     собственному диапазону [lo, hi] — иначе иконка получается «грязной».
    "aurite": {
        "base_vanilla": "netherite",
        "hue": 0.098,        # ~35 градусов = золото/медь
        "sat": 0.80,
        "lo": 0.20,          # нижняя граница светлоты (тёмные грани)
        "hi": 1.00,          # верхняя граница (блики)
        "icon_lo": 0.34,
        "icon_hi": 1.00,
        "label": "Ауритовая броня",
    },
    # --- Рифт-кристалл: холодный изумрудно-циановый кристалл (по мотивам
    #     ванильной diamond-подложки: гладкие крупные блики).
    "riftcrystal": {
        "base_vanilla": "diamond",
        "hue": 0.445,        # ~160 градусов = изумруд/бирюза
        "sat": 0.74,
        "lo": 0.28,
        "hi": 1.00,
        "icon_lo": 0.40,
        "icon_hi": 1.00,
        "label": "Броня рифт-кристалла",
    },
    # --- Кожаная: тёплая коричневая (по мотивам ванильной leather-подложки).
    "leather": {
        "base_vanilla": "leather",
        "hue": 0.075,
        "sat": 0.66,
        "lo": 0.22,
        "hi": 0.92,
        "icon_lo": 0.30,
        "icon_hi": 0.95,
        "label": "Кожаная броня",
    },
}

PARTS = ["helmet", "chestplate", "leggings", "boots"]


def _hsv_to_rgb(h: float, s: float, v: float):
    import colorsys
    r, g, b = colorsys.hsv_to_rgb(h % 1.0, max(0.0, min(1.0, s)), max(0.0, min(1.0, v)))
    return int(round(r * 255)), int(round(g * 255)), int(round(b * 255))


def recolor(src: Path, dst: Path, hue: float, sat: float, lo: float, hi: float) -> None:
    """Перекрашивает PNG, сохраняя ванильное освещение граней.

    Для каждого непрозрачного пикселя:
      * берём относительную светлоту пикселя внутри этой карты
        (V пикселя, нормированная по самому яркому непрозрачному пикселю);
      * переносим её в целевой диапазон [lo, hi] — так тёмная ванильная
        подложка (netherite) не остаётся «грязной», а блики остаются бликами;
      * сохраняем альфу.
    """
    if not src.exists():
        raise SystemExit("НЕТ ВАНИЛЬНОГО ФАЙЛА: %s" % src)
    im = Image.open(src).convert("RGBA")
    px = im.load()
    w, h = im.size

    # 1) диапазон светлоты среди непрозрачных пикселей
    vmin, vmax = 1.0, 0.0
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a > 0:
                v = max(r, g, b) / 255.0
                if v < vmin:
                    vmin = v
                if v > vmax:
                    vmax = v
    span = vmax - vmin
    if span < 1e-6:
        vmin, span = 0.0, 1.0

    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            v = (max(r, g, b) / 255.0 - vmin) / span     # 0..1 относительная светлота
            v = lo + (hi - lo) * (v ** 1.08)               # мягкий контраст
            nr, ng, nb = _hsv_to_rgb(hue, sat, v)
            px[x, y] = (nr, ng, nb, a)

    dst.parent.mkdir(parents=True, exist_ok=True)
    im.save(dst, optimize=True)


def recolor_icon(src: Path, dst: Path, hue: float, sat: float, lo: float, hi: float) -> None:
    """Иконка 16x16: та же схема перекраски, но диапазон светлоты шире —
    иконка должна читаться в инвентаре (тёмные пиксели не «съедают» форму)."""
    recolor(src, dst, hue, sat, lo, hi)


def write_item_model(mat: str, part: str) -> None:
    """Ванильная модель предмета брони: parent = minecraft:item/generated."""
    path = ITEM_MODEL / f"{mat}_{part}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    data = {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"riftworld:item/{mat}_{part}"},
    }
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    ARMOR_TEX.mkdir(parents=True, exist_ok=True)
    ITEM_TEX.mkdir(parents=True, exist_ok=True)

    made = []
    for mat, cfg in SETS.items():
        base = cfg["base_vanilla"]
        # --- слои брони (layer_1 = шлем/нагрудник/ботинки, layer_2 = поножи)
        for idx in (1, 2):
            src = VANILLA / f"{base}_layer_{idx}.png"
            dst = ARMOR_TEX / f"{mat}_layer_{idx}.png"
            recolor(src, dst, cfg["hue"], cfg["sat"], cfg["lo"], cfg["hi"])
            made.append(dst)
        # --- иконки предметов 16x16
        for part in PARTS:
            src = VANILLA / f"{base}_{part}.png"
            dst = ITEM_TEX / f"{mat}_{part}.png"
            recolor_icon(src, dst, cfg["hue"], cfg["sat"], cfg["icon_lo"], cfg["icon_hi"])
            made.append(dst)
            write_item_model(mat, part)
            made.append(ITEM_MODEL / f"{mat}_{part}.json")

    print("OK v0.52.0 vanilla armor: %d файлов" % len(made))
    for mat, cfg in SETS.items():
        print("  %-12s <- %-9s (%s)" % (mat, cfg["base_vanilla"], cfg["label"]))


if __name__ == "__main__":
    main()
