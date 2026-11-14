#!/usr/bin/env python3
"""v0.53.0 — инструменты и оружие мода по ВАНИЛЬНОМУ принципу.

Так же, как броня в v0.52.0: кастомных 3D-моделей больше нет.

  * Модель предмета — ванильная:
        меч/топор/кирка/лопата/мотыга  ->  "minecraft:item/handheld"
        лук/щит                         ->  "minecraft:item/generated"
    Никаких "elements" и никаких выдуманных палитр ("pal") — клиент рисует
    предмет ровно так же, как ванильный, из одной плоской текстуры.
  * Текстура — настоящая ванильная 16×16 (diamond/netherite/iron/bow),
    перекрашенная в палитру мода с СОХРАНЕНИЕМ освещения граней:
    тёмная грань остаётся тёмной, блик остаётся бликом.

Запуск: python3 tools/rift/gen_v052_vanilla_tools.py
"""
from __future__ import annotations

import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
MOD = ROOT / "mod/src/main/resources"
ASSETS = MOD / "assets/riftworld"
ITEM_MODEL = ASSETS / "models/item"
ITEM_TEX = ASSETS / "textures/item"
ENTITY_TEX = ASSETS / "textures/entity"
VANILLA = ROOT / "tools/rift/vanilla_items"
GEAR_SRC = ROOT / "tools/rift/gear_sources"

# Палитры — те же, что у брони v0.52.0, чтобы комплект читался единым.
PALETTES = {
    "aurite": {
        "hue": 0.098, "sat": 0.80, "lo": 0.20, "hi": 1.00,
        "icon_lo": 0.34, "icon_hi": 1.00,
    },
    "riftcrystal": {
        "hue": 0.445, "sat": 0.74, "lo": 0.28, "hi": 1.00,
        "icon_lo": 0.40, "icon_hi": 1.00,
    },
}

# Ванильная основа для каждого предмета мода:
#   предмет мода            -> (ванильный предмет, родитель модели)
TOOLS = [
    # (предмет мода, ванильная основа, handheld? )
    ("aurite_sword", "diamond_sword", True),
    ("aurite_pickaxe", "diamond_pickaxe", True),
    ("aurite_axe", "diamond_axe", True),
    ("aurite_shovel", "diamond_shovel", True),
    ("aurite_hoe", "diamond_hoe", True),
    ("riftcrystal_sword", "netherite_sword", True),
    ("riftcrystal_pickaxe", "netherite_pickaxe", True),
    ("riftcrystal_axe", "netherite_axe", True),
    ("riftcrystal_shovel", "netherite_shovel", True),
    ("riftcrystal_hoe", "netherite_hoe", True),
    # Лук — ванильная основа (16x16), перекрашивается в палитру мода.
    ("elisar_bow", "bow", True),
]

# Палитра для каждого предмета
PALETTE_OF = {
    "aurite_sword": "aurite", "aurite_pickaxe": "aurite", "aurite_axe": "aurite",
    "aurite_shovel": "aurite", "aurite_hoe": "aurite",
    "riftcrystal_sword": "riftcrystal", "riftcrystal_pickaxe": "riftcrystal",
    "riftcrystal_axe": "riftcrystal", "riftcrystal_shovel": "riftcrystal",
    "riftcrystal_hoe": "riftcrystal",
    "elisar_bow": "aurite",
}


def _hsv_to_rgb(h: float, s: float, v: float):
    import colorsys
    r, g, b = colorsys.hsv_to_rgb(h % 1.0, max(0.0, min(1.0, s)), max(0.0, min(1.0, v)))
    return int(round(r * 255)), int(round(g * 255)), int(round(b * 255))


def recolor(src: Path, dst: Path, hue: float, sat: float, lo: float, hi: float) -> None:
    """Перекрашивает PNG, сохраняя ванильное освещение граней.

    Та же схема, что у брони (gen_v052_vanilla_armor.py): относительная
    светлота каждого пикселя переносится в целевой диапазон [lo, hi],
    поэтому тёмная ванильная подложка не остаётся «грязной», а блики
    остаются бликами.
    """
    if not src.exists():
        raise SystemExit("НЕТ ВАНИЛЬНОГО ФАЙЛА: %s" % src)
    im = Image.open(src).convert("RGBA")
    px = im.load()
    w, h = im.size

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
            v = (max(r, g, b) / 255.0 - vmin) / span
            v = lo + (hi - lo) * (v ** 1.08)
            nr, ng, nb = _hsv_to_rgb(hue, sat, v)
            px[x, y] = (nr, ng, nb, a)

    dst.parent.mkdir(parents=True, exist_ok=True)
    im.save(dst, optimize=True)


# Прочая экипировка стражи и «другая броня»: щиты, меч стражи, кувалда.
# У ванильного щита нет плоской иконки (он рисуется entity-моделью), поэтому
# здесь своя форма, но:
#   * приводится к ВАНИЛЬНОМУ размеру иконки 16x16;
#   * перекрашивается в те же палитры мода, что броня и инструменты.
#   * модель — ванильная "minecraft:item/generated", без elements.
# Щиты (наследники ShieldItem) — их 3D-текстура в руке.
SHIELD_ENTITY = [
    # (предмет мода, палитра)
    ("elisar_shield", "aurite"),
    ("elisar_guard_shield", "riftcrystal"),
]

GEAR = [
    # (предмет мода, палитра)
    ("elisar_shield", "aurite"),
    ("elisar_guard_shield", "riftcrystal"),
    ("elisar_guard_sword", "aurite"),
]


def shrink_to_vanilla_icon(src: Path, dst: Path) -> None:
    """Приводит иконку к ванильным 16x16 (сглаженно, с сохранением формы)."""
    im = Image.open(src).convert("RGBA")
    if im.size == (16, 16):
        im.save(dst, optimize=True)
        return
    # Ближайший сосед по прозрачности: форма должна остаться «пиксельной»,
    # но без рваных краёв после уменьшения.
    alpha = im.getchannel("A")
    small = im.resize((16, 16), Image.LANCZOS)
    small.putalpha(alpha.resize((16, 16), Image.LANCZOS))
    small.save(dst, optimize=True)


def write_shield_models(item_id: str) -> None:
    """Модель щита — ванильная плоская иконка + состояние блокирования.

    В Forge 1.20.1 ванильный рендерер щита рисует только minecraft:shield,
    поэтому кастомный щит рисует собственный рендерер
    (ru.aiforge.riftworld.client.ShieldItemRenderer) — с ванильной геометрией
    и текстурой entity/<имя>_base_nopattern. Модель предмета нужна для
    инвентаря и для состояния "blocking" (предикат, который регистрируется
    в ClientHooks).
    """
    data = {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"riftworld:item/{item_id}"},
        "overrides": [
            {"predicate": {"blocking": 1}, "model": f"riftworld:item/{item_id}_blocking"},
        ],
    }
    (ITEM_MODEL / f"{item_id}.json").write_text(
        json.dumps(data, indent=2) + "\n", encoding="utf-8")

    block = {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"riftworld:item/{item_id}"},
    }
    (ITEM_MODEL / f"{item_id}_blocking.json").write_text(
        json.dumps(block, indent=2) + "\n", encoding="utf-8")


def write_item_model(item_id: str, handheld: bool) -> None:
    """Ванильная модель предмета: без elements, без выдуманных палитр."""
    parent = "minecraft:item/handheld" if handheld else "minecraft:item/generated"
    data = {
        "parent": parent,
        "textures": {"layer0": f"riftworld:item/{item_id}"},
    }
    path = ITEM_MODEL / f"{item_id}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


# Ракурсы лука в руке — ровно ванильные (item/bow.json, 1.20.1).
BOW_DISPLAY = {
    "thirdperson_righthand": {
        "rotation": [-80, 260, -40], "translation": [-1, -2, 2.5], "scale": [0.9, 0.9, 0.9],
    },
    "thirdperson_lefthand": {
        "rotation": [-80, -280, 40], "translation": [-1, -2, 2.5], "scale": [0.9, 0.9, 0.9],
    },
    "firstperson_righthand": {
        "rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68],
    },
    "firstperson_lefthand": {
        "rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68],
    },
}


def write_bow_model(item_id: str) -> None:
    """Модель лука — ванильная: parent item/generated + overrides для натяжения.

    Клиент сам выбирает bow_pulling_N по предикату pulling/pull, поэтому
    состояния натяжения работают без единой строки кода.
    """
    data = {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"riftworld:item/{item_id}"},
        "display": BOW_DISPLAY,
        "overrides": [
            {"predicate": {"pulling": 1}, "model": f"riftworld:item/{item_id}_pulling_0"},
            {"predicate": {"pulling": 1, "pull": 0.65}, "model": f"riftworld:item/{item_id}_pulling_1"},
            {"predicate": {"pulling": 1, "pull": 0.9}, "model": f"riftworld:item/{item_id}_pulling_2"},
        ],
    }
    path = ITEM_MODEL / f"{item_id}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")

    # Дочерние модели состояний натяжения — тоже ванильные.
    for i in (0, 1, 2):
        child = {
            "parent": f"riftworld:item/{item_id}",
            "textures": {"layer0": f"riftworld:item/{item_id}_pulling_{i}"},
        }
        (ITEM_MODEL / f"{item_id}_pulling_{i}.json").write_text(
            json.dumps(child, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    made = 0
    for item_id, vanilla_id, handheld in TOOLS:
        pal = PALETTES[PALETTE_OF[item_id]]
        src = VANILLA / f"{vanilla_id}.png"
        dst = ITEM_TEX / f"{item_id}.png"
        recolor(src, dst, pal["hue"], pal["sat"], pal["icon_lo"], pal["icon_hi"])
        if item_id == "elisar_bow":
            write_bow_model(item_id)
            # Состояния натяжения — ванильные bow_pulling_N, перекрашенные так же.
            for i in (0, 1, 2):
                recolor(VANILLA / f"bow_pulling_{i}.png",
                        ITEM_TEX / f"{item_id}_pulling_{i}.png",
                        pal["hue"], pal["sat"], pal["icon_lo"], pal["icon_hi"])
        else:
            write_item_model(item_id, handheld)
        made += 1
        print("  %-24s <- vanilla %s" % (item_id, vanilla_id))
    # Щиты — наследники ванильного ShieldItem, поэтому в руке они рисуются
    # ванильной сущностью, и текстура берётся из entity/shield_base_nopattern
    # в пространстве имён мода. Перекрашиваем её в палитру щита.
    for item_id, pal_name in SHIELD_ENTITY:
        pal = PALETTES[pal_name]
        # Имя текстуры — как у ванильного щита и как у mOres:
        # <имя предмета>_base / <имя предмета>_base_nopattern.
        for vanilla_name, suffix in (("shield_base_nopattern", "_base_nopattern"),
                                     ("shield_base", "_base")):
            recolor(VANILLA / f"{vanilla_name}.png",
                    ENTITY_TEX / f"{item_id}{suffix}.png",
                    pal["hue"], pal["sat"], pal["lo"], pal["hi"])

    # Прочая экипировка: своя форма, но ванильный размер и палитра мода.
    # Источники лежат в tools/rift/gear_sources (неизменённые оригиналы),
    # поэтому генератор идемпотентен: повторный прогон даёт тот же результат.
    for item_id, pal_name in GEAR:
        pal = PALETTES[pal_name]
        src = GEAR_SRC / f"{item_id}.png"
        if not src.exists():
            raise SystemExit("НЕТ ИСХОДНОЙ ИКОНКИ: %s" % src)
        dst = ITEM_TEX / f"{item_id}.png"
        shrink_to_vanilla_icon(src, dst)
        recolor(dst, dst, pal["hue"], pal["sat"], pal["icon_lo"], pal["icon_hi"])
        if item_id in ("elisar_shield", "elisar_guard_shield"):
            write_shield_models(item_id)
        elif item_id == "elisar_guard_sword":
            # Меч в руке — как ванильный меч: handheld.
            write_item_model(item_id, True)
        else:
            write_item_model(item_id, False)
        made += 1
        print("  %-24s <- своя форма, 16x16, палитра %s" % (item_id, pal_name))
    print("OK v0.53.0 vanilla tools: %d" % made)


if __name__ == "__main__":
    main()
