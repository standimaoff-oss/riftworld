#!/usr/bin/env python3
"""v0.55.0 — генератор «гнилого» (rotten) набора дерева Мороковой Пади.

Морокова Падь — застывшая, мёрзлая чаща. Бледное дерево там ещё стоит,
но часть стволов сгнила: древесина потемнела, обросла мхом и костяной
трухой. Появились и «терновые» двери — из тех, что заросли колючками.

Набор гнилого дерева:
  rotten_morok_log / rotten_morok_wood        — тёмное гнилое бревно
  stripped_rotten_morok_log / _wood            — обтёсанное (светлее)
  rotten_morok_planks + ступени/плита         — тёмные гнилые доски
  rotten_morok_fence / fence_gate              — забор
  rotten_morok_door / trapdoor                 — гнилая дверь
  rotten_morok_pressure_plate / button         — нажимная плита, кнопка
  thorn_morok_door                             — терновая дверь
  thorn_morok_trapdoor                         — терновый люк

Все текстуры 16×16 RGBA, рисуются из палитры исходного бледного дерева:
тот же рисунок волокон, но темнее, с зелёным налётом и трухой.

Запуск:
    PYTHONPATH=.pylibs python tools/rift/gen_rotten_morok_wood.py
"""

from __future__ import annotations

import json
import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / ".pylibs"))

from PIL import Image  # noqa: E402

BLOCK_TEX = ROOT / "mod/src/main/resources/assets/riftworld/textures/block"
BLOCK_MODELS = ROOT / "mod/src/main/resources/assets/riftworld/models/block"
BLOCKSTATES = ROOT / "mod/src/main/resources/assets/riftworld/blockstates"

# Палитра «гнили»: тёмное, мокрое, подгнившее дерево.
ROTTEN_DARK = (46, 44, 33)        # самая тёмная гниль в тенях
ROTTEN_BASE = (74, 68, 48)        # основной гнилый тон
ROTTEN_MID = (92, 85, 60)         # светлее гниль
ROTTEN_PALE = (112, 104, 74)      # выветренные участки
MOSS_DARK = (44, 58, 38)         # тёмный мох
MOSS_LIGHT = (74, 92, 56)        # светлый мох
BONE_SPECK = (176, 172, 150)     # костяная труха
THORN_DARK = (38, 44, 30)        # тёрн колючки
THORN_LIGHT = (96, 110, 74)      # светлый бок колючки


def _load(name: str) -> Image.Image:
    return Image.open(BLOCK_TEX / f"{name}.png").convert("RGBA")


def _save(img: Image.Image, name: str) -> None:
    img.save(BLOCK_TEX / f"{name}.png")


def _map_pixels(src: Image.Image, mapping) -> Image.Image:
    """Перекрасить пиксели по функции mapping(r,g,b,a) -> (r,g,b,a)."""
    out = Image.new("RGBA", src.size)
    src_px = src.load()
    dst = out.load()
    w, h = src.size
    for y in range(h):
        for x in range(w):
            dst[x, y] = mapping(x, y, *src_px[x, y])
    return out


def rotten_palette(x: int, y: int, r: int, g: int, b: int, a: int):
    """Бледное дерево -> гнилое: темнее, зеленее, с трухой.

    Волокна исходника сохраняются: яркость пикселя задаёт тон, поэтому
    полосы на бревне и досках остаются на своих местах."""
    lum = (r * 299 + g * 587 + b * 114) / 1000.0
    if a == 0:
        return (0, 0, 0, 0)
    # Темнее и зеленее: гниль забирает свет и добавляет мох.
    # Контраст расширен намеренно: иначе волокна бревна сливаются в пятно.
    t = max(0.0, min(1.0, (lum - 120.0) / 90.0))
    shade = 0.34 + 0.78 * t
    nr = int(ROTTEN_BASE[0] * shade + MOSS_DARK[0] * (1 - shade) * 0.75)
    ng = int(ROTTEN_BASE[1] * shade + MOSS_DARK[1] * (1 - shade) * 0.75)
    nb = int(ROTTEN_BASE[2] * shade + MOSS_DARK[2] * (1 - shade) * 0.75)
    # Светлые волокна остаются светлее — иначе текстура станет кашей.
    if lum > 190:
        nr, ng, nb = ROTTEN_PALE
    elif lum > 168:
        nr = (nr + ROTTEN_MID[0]) // 2
        ng = (ng + ROTTEN_MID[1]) // 2
        nb = (nb + ROTTEN_MID[2]) // 2
    # Костяная труха: редкие светлые крапинки.
    if (x * 7 + y * 13) % 29 == 0 and lum > 150:
        nr, ng, nb = BONE_SPECK
    # Мох: зелёные пятна в низинах текстуры.
    if (x * 5 + y * 11) % 17 == 0:
        nr = (nr + MOSS_LIGHT[0]) // 2
        ng = (ng + MOSS_LIGHT[1]) // 2
        nb = (nb + MOSS_LIGHT[2]) // 2
    return (min(nr, 255), min(ng, 255), min(nb, 255), a)


def thorn_palette(x: int, y: int, r: int, g: int, b: int, a: int):
    """Гнилая дверь + терновые колючки поверх неё."""
    if a == 0:
        return (0, 0, 0, 0)
    lum = (r * 299 + g * 587 + b * 114) / 1000.0
    shade = 0.26 + 0.30 * (lum / 255.0)
    nr = int(ROTTEN_DARK[0] * 0.6 + ROTTEN_BASE[0] * shade * 0.8)
    ng = int(ROTTEN_DARK[1] * 0.6 + ROTTEN_BASE[1] * shade * 0.8)
    nb = int(ROTTEN_DARK[2] * 0.6 + ROTTEN_BASE[2] * shade * 0.8)
    # Колючки: тёмный стебель и светлый бок. Рисуем неровно — шаг зависит
    # от строки, иначе получается правильная сетка, а не колючки.
    step = 6 + (y % 3)
    if (x + y * 2) % step == 0:
        nr, ng, nb = THORN_LIGHT
    elif (x + y * 2) % step == 1 and lum > 110:
        nr, ng, nb = THORN_DARK
    # Кончики колючек — костяные.
    if (x * 3 + y * 5) % 23 == 0:
        nr, ng, nb = BONE_SPECK
    return (min(nr, 255), min(ng, 255), min(nb, 255), a)


def stripped_rotten_palette(x: int, y: int, r: int, g: int, b: int, a: int):
    """Обтёсанное гнилое дерево: светлее бревна, но всё равно гнилое.

    У обтёсанного бревна почти нет рисунка, поэтому волокна рисуем сами —
    вертикальные полосы, как у настоящего обтёсанного ствола."""
    if a == 0:
        return (0, 0, 0, 0)
    lum = (r * 299 + g * 587 + b * 114) / 1000.0
    t = max(0.0, min(1.0, (lum - 195.0) / 25.0))
    shade = 0.62 + 0.42 * t
    nr = int(ROTTEN_MID[0] * shade)
    ng = int(ROTTEN_MID[1] * shade)
    nb = int(ROTTEN_MID[2] * shade)
    # Вертикальные волокна: темная полоса на светлом стволе.
    if x % 5 == 0:
        nr, ng, nb = (nr * 6) // 10, (ng * 6) // 10, (nb * 6) // 10
    elif x % 5 == 2:
        nr, ng, nb = min(nr + 12, 255), min(ng + 12, 255), min(nb + 10, 255)
    # Мох и труха.
    if (x * 3 + y * 5) % 23 == 0:
        nr, ng, nb = MOSS_LIGHT
    elif (x * 7 + y * 3) % 31 == 0:
        nr, ng, nb = BONE_SPECK
    return (min(nr, 255), min(ng, 255), min(nb, 255), a)


def build_textures() -> list[str]:
    made = []

    def make(src_name: str, out_name: str, mapping) -> None:
        _save(_map_pixels(_load(src_name), mapping), out_name)
        made.append(out_name)

    # Бревно и древесина.
    make("morok_log", "rotten_morok_log", rotten_palette)
    make("morok_log_top", "rotten_morok_log_top", rotten_palette)
    make("morok_log", "rotten_morok_wood", rotten_palette)
    make("morok_log_top", "rotten_morok_wood_top", rotten_palette)

    # Обтёсанное — светлее.
    make("stripped_morok_log", "stripped_rotten_morok_log", stripped_rotten_palette)
    make("stripped_morok_log_top", "stripped_rotten_morok_log_top", stripped_rotten_palette)
    make("stripped_morok_log", "stripped_rotten_morok_wood", stripped_rotten_palette)
    make("stripped_morok_log_top", "stripped_rotten_morok_wood_top", stripped_rotten_palette)

    # Доски и всё, что из них сделано.
    for src, dst in (
        ("morok_planks", "rotten_morok_planks"),
        ("morok_door_bottom", "rotten_morok_door_bottom"),
        ("morok_door_top", "rotten_morok_door_top"),
        ("morok_trapdoor", "rotten_morok_trapdoor"),
    ):
        make(src, dst, rotten_palette)

    # Терновые: гнилая дверь, но с колючками.
    make("morok_door_bottom", "thorn_morok_door_bottom", thorn_palette)
    make("morok_door_top", "thorn_morok_door_top", thorn_palette)
    make("morok_trapdoor", "thorn_morok_trapdoor", thorn_palette)

    return made


def _write_json(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def build_models() -> None:
    """Модели гнилого набора — по образцу бледного."""
    # Бревно/древесина — колонна. Торец (end) берёт отдельную текстуру
    # _top: у бревна срез другой, чем бок, иначе торцы выглядят неправильно.
    for base, side, end in (
        ("rotten_morok_log", "rotten_morok_log", "rotten_morok_log_top"),
        ("rotten_morok_wood", "rotten_morok_wood", "rotten_morok_log_top"),
        ("stripped_rotten_morok_log", "stripped_rotten_morok_log", "stripped_rotten_morok_log_top"),
        ("stripped_rotten_morok_wood", "stripped_rotten_morok_wood", "stripped_rotten_morok_log_top"),
    ):
        _write_json(BLOCK_MODELS / f"{base}.json", {
            "parent": "minecraft:block/cube_column",
            "textures": {"end": f"riftworld:block/{end}", "side": f"riftworld:block/{side}"},
        })

    # Горизонтальное бревно.
    for base, side, end in (
        ("rotten_morok_log_horizontal", "rotten_morok_log", "rotten_morok_log_top"),
        ("rotten_morok_wood_horizontal", "rotten_morok_wood", "rotten_morok_log_top"),
        ("stripped_rotten_morok_log_horizontal", "stripped_rotten_morok_log", "stripped_rotten_morok_log_top"),
        ("stripped_rotten_morok_wood_horizontal", "stripped_rotten_morok_wood", "stripped_rotten_morok_log_top"),
    ):
        _write_json(BLOCK_MODELS / f"{base}.json", {
            "parent": "minecraft:block/cube_column_horizontal",
            "textures": {"end": f"riftworld:block/{end}", "side": f"riftworld:block/{side}"},
        })

    # Доски.
    _write_json(BLOCK_MODELS / "rotten_morok_planks.json", {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": "riftworld:block/rotten_morok_planks"},
    })

    # Ступени и плита — по образцу бледных.
    _write_json(BLOCK_MODELS / "rotten_morok_planks_stairs.json", {
        "parent": "minecraft:block/stairs",
        "textures": {"bottom": "riftworld:block/rotten_morok_planks",
                     "top": "riftworld:block/rotten_morok_planks",
                     "side": "riftworld:block/rotten_morok_planks"},
    })
    _write_json(BLOCK_MODELS / "rotten_morok_planks_stairs_inner.json", {
        "parent": "minecraft:block/inner_stairs",
        "textures": {"bottom": "riftworld:block/rotten_morok_planks",
                     "top": "riftworld:block/rotten_morok_planks",
                     "side": "riftworld:block/rotten_morok_planks"},
    })
    _write_json(BLOCK_MODELS / "rotten_morok_planks_stairs_outer.json", {
        "parent": "minecraft:block/outer_stairs",
        "textures": {"bottom": "riftworld:block/rotten_morok_planks",
                     "top": "riftworld:block/rotten_morok_planks",
                     "side": "riftworld:block/rotten_morok_planks"},
    })
    _write_json(BLOCK_MODELS / "rotten_morok_planks_slab.json", {
        "parent": "minecraft:block/slab",
        "textures": {"bottom": "riftworld:block/rotten_morok_planks",
                     "top": "riftworld:block/rotten_morok_planks",
                     "side": "riftworld:block/rotten_morok_planks"},
    })
    _write_json(BLOCK_MODELS / "rotten_morok_planks_slab_top.json", {
        "parent": "minecraft:block/slab_top",
        "textures": {"bottom": "riftworld:block/rotten_morok_planks",
                     "top": "riftworld:block/rotten_morok_planks",
                     "side": "riftworld:block/rotten_morok_planks"},
    })

    # Дверь: восемь частей, как у бледной.
    for half in ("bottom", "top"):
        for hinge in ("left", "right"):
            for open_ in (False, True):
                name = f"rotten_morok_door_{half}_{hinge}" + ("_open" if open_ else "")
                _write_json(BLOCK_MODELS / f"{name}.json", {
                    "parent": f"minecraft:block/door_{half}_{hinge}" + ("_open" if open_ else ""),
                    "render_type": "minecraft:cutout",
                    "textures": {"bottom": "riftworld:block/rotten_morok_door_bottom",
                                 "top": "riftworld:block/rotten_morok_door_top"},
                })

    # Терновая дверь — то же, но с колючками.
    for half in ("bottom", "top"):
        for hinge in ("left", "right"):
            for open_ in (False, True):
                name = f"thorn_morok_door_{half}_{hinge}" + ("_open" if open_ else "")
                _write_json(BLOCK_MODELS / f"{name}.json", {
                    "parent": f"minecraft:block/door_{half}_{hinge}" + ("_open" if open_ else ""),
                    "render_type": "minecraft:cutout",
                    "textures": {"bottom": "riftworld:block/thorn_morok_door_bottom",
                                 "top": "riftworld:block/thorn_morok_door_top"},
                })

    # Люки.
    for name, parent in (
        ("rotten_morok_trapdoor_bottom", "minecraft:block/template_orientable_trapdoor_bottom"),
        ("rotten_morok_trapdoor_top", "minecraft:block/template_orientable_trapdoor_top"),
        ("rotten_morok_trapdoor_open", "minecraft:block/template_orientable_trapdoor_open"),
        ("thorn_morok_trapdoor_bottom", "minecraft:block/template_orientable_trapdoor_bottom"),
        ("thorn_morok_trapdoor_top", "minecraft:block/template_orientable_trapdoor_top"),
        ("thorn_morok_trapdoor_open", "minecraft:block/template_orientable_trapdoor_open"),
    ):
        tex = "thorn_morok_trapdoor" if name.startswith("thorn") else "rotten_morok_trapdoor"
        _write_json(BLOCK_MODELS / f"{name}.json", {
            "parent": parent,
            "render_type": "minecraft:cutout",
            "textures": {"texture": f"riftworld:block/{tex}"},
        })

    # Забор.
    for name in ("rotten_morok_fence_post", "rotten_morok_fence_side"):
        _write_json(BLOCK_MODELS / f"{name}.json", {
            "parent": f"minecraft:block/{'fence_post' if name.endswith('post') else 'fence_side'}",
            "textures": {"texture": "riftworld:block/rotten_morok_planks"},
        })

    # Калитка.
    for name in ("rotten_morok_fence_gate", "rotten_morok_fence_gate_open",
                 "rotten_morok_fence_gate_wall", "rotten_morok_fence_gate_wall_open"):
        _write_json(BLOCK_MODELS / f"{name}.json", {
            "parent": f"minecraft:block/{name.replace('rotten_morok_', '')}",
            "textures": {"texture": "riftworld:block/rotten_morok_planks"},
        })

    # Нажимная плита и кнопка.
    for name, parent in (
        ("rotten_morok_pressure_plate", "minecraft:block/pressure_plate"),
        ("rotten_morok_pressure_plate_down", "minecraft:block/pressure_plate_down"),
        ("rotten_morok_button", "minecraft:block/button"),
        ("rotten_morok_button_pressed", "minecraft:block/button_pressed"),
        ("rotten_morok_button_inventory", "minecraft:block/button_inventory"),
    ):
        _write_json(BLOCK_MODELS / f"{name}.json", {
            "parent": parent,
            "textures": {"texture": "riftworld:block/rotten_morok_planks"},
        })


def _door_blockstate(block: str, prefix: str) -> dict:
    variants = {}
    for facing in ("east", "north", "south", "west"):
        for half in ("lower", "upper"):
            for hinge in ("left", "right"):
                for open_ in (False, True):
                    name = f"{prefix}_door_{'bottom' if half == 'lower' else 'top'}_{hinge}"
                    if open_:
                        name += "_open"
                    key = f"facing={facing},half={half},hinge={hinge},open={'true' if open_ else 'false'}"
                    entry = {"model": f"riftworld:block/{name}"}
                    if open_:
                        entry["y"] = {"left": 90, "right": 270}[hinge]
                    variants[key] = entry
    return {"variants": variants}


def build_blockstates() -> None:
    # Бревно и древесина.
    for base in ("rotten_morok_log", "rotten_morok_wood",
                 "stripped_rotten_morok_log", "stripped_rotten_morok_wood"):
        _write_json(BLOCKSTATES / f"{base}.json", {
            "variants": {
                "axis=y": {"model": f"riftworld:block/{base}"},
                "axis=x": {"model": f"riftworld:block/{base}_horizontal", "x": 90},
                "axis=z": {"model": f"riftworld:block/{base}_horizontal", "x": 90, "y": 90},
            }
        })

    # Доски.
    _write_json(BLOCKSTATES / "rotten_morok_planks.json", {
        "variants": {"": {"model": "riftworld:block/rotten_morok_planks"}}
    })
    _write_json(BLOCKSTATES / "rotten_morok_planks_stairs.json", {
        "variants": {
            "facing=east,half=bottom": {"model": "riftworld:block/rotten_morok_planks_stairs"},
            "facing=east,half=top": {"model": "riftworld:block/rotten_morok_planks_stairs", "x": 180},
            "facing=west,half=bottom": {"model": "riftworld:block/rotten_morok_planks_stairs", "y": 270},
            "facing=west,half=top": {"model": "riftworld:block/rotten_morok_planks_stairs", "x": 180, "y": 270},
            "facing=south,half=bottom": {"model": "riftworld:block/rotten_morok_planks_stairs", "y": 90},
            "facing=south,half=top": {"model": "riftworld:block/rotten_morok_planks_stairs", "x": 180, "y": 90},
            "facing=north,half=bottom": {"model": "riftworld:block/rotten_morok_planks_stairs", "y": 180},
            "facing=north,half=top": {"model": "riftworld:block/rotten_morok_planks_stairs", "x": 180, "y": 180},
        }
    })
    _write_json(BLOCKSTATES / "rotten_morok_planks_slab.json", {
        "variants": {
            "type=bottom": {"model": "riftworld:block/rotten_morok_planks_slab"},
            "type=top": {"model": "riftworld:block/rotten_morok_planks_slab_top"},
        }
    })

    # Двери.
    _write_json(BLOCKSTATES / "rotten_morok_door.json",
                _door_blockstate("rotten_morok_door", "rotten_morok"))
    _write_json(BLOCKSTATES / "thorn_morok_door.json",
                _door_blockstate("thorn_morok_door", "thorn_morok"))

    # Люки.
    for prefix in ("rotten_morok", "thorn_morok"):
        variants = {}
        for facing in ("north", "east", "south", "west"):
            for half in ("bottom", "top"):
                for open_ in (False, True):
                    model = f"{prefix}_trapdoor_open" if open_ else f"{prefix}_trapdoor_{half}"
                    key = f"facing={facing},half={half},open={'true' if open_ else 'false'}"
                    entry = {"model": f"riftworld:block/{model}"}
                    if facing == "east":
                        entry["y"] = 90
                    elif facing == "south":
                        entry["y"] = 180
                    elif facing == "west":
                        entry["y"] = 270
                    variants[key] = entry
        _write_json(BLOCKSTATES / f"{prefix}_trapdoor.json", {"variants": variants})

    # Забор — multipart, как у бледного.
    _write_json(BLOCKSTATES / "rotten_morok_fence.json", {
        "multipart": [
            {"apply": {"model": "riftworld:block/rotten_morok_fence_post"}},
            {"when": {"north": "true"},
             "apply": {"model": "riftworld:block/rotten_morok_fence_side", "uvlock": True}},
            {"when": {"east": "true"},
             "apply": {"model": "riftworld:block/rotten_morok_fence_side", "uvlock": True, "y": 90}},
            {"when": {"south": "true"},
             "apply": {"model": "riftworld:block/rotten_morok_fence_side", "uvlock": True, "y": 180}},
            {"when": {"west": "true"},
             "apply": {"model": "riftworld:block/rotten_morok_fence_side", "uvlock": True, "y": 270}},
        ]
    })

    # Калитка.
    _write_json(BLOCKSTATES / "rotten_morok_fence_gate.json", {
        "variants": {
            "facing=north,in_wall=true,open=false":
                {"model": "riftworld:block/rotten_morok_fence_gate_wall"},
            "facing=north,in_wall=true,open=true":
                {"model": "riftworld:block/rotten_morok_fence_gate_wall_open", "y": 180},
            "facing=north,in_wall=false,open=false":
                {"model": "riftworld:block/rotten_morok_fence_gate"},
            "facing=north,in_wall=false,open=true":
                {"model": "riftworld:block/rotten_morok_fence_gate_open", "y": 180},
            "facing=east,in_wall=true,open=false":
                {"model": "riftworld:block/rotten_morok_fence_gate_wall", "y": 90},
            "facing=east,in_wall=true,open=true":
                {"model": "riftworld:block/rotten_morok_fence_gate_wall_open", "y": 270},
            "facing=east,in_wall=false,open=false":
                {"model": "riftworld:block/rotten_morok_fence_gate", "y": 90},
            "facing=east,in_wall=false,open=true":
                {"model": "riftworld:block/rotten_morok_fence_gate_open", "y": 270},
            "facing=south,in_wall=true,open=false":
                {"model": "riftworld:block/rotten_morok_fence_gate_wall", "y": 180},
            "facing=south,in_wall=true,open=true":
                {"model": "riftworld:block/rotten_morok_fence_gate_wall_open"},
            "facing=south,in_wall=false,open=false":
                {"model": "riftworld:block/rotten_morok_fence_gate", "y": 180},
            "facing=south,in_wall=false,open=true":
                {"model": "riftworld:block/rotten_morok_fence_gate_open"},
            "facing=west,in_wall=true,open=false":
                {"model": "riftworld:block/rotten_morok_fence_gate_wall", "y": 270},
            "facing=west,in_wall=true,open=true":
                {"model": "riftworld:block/rotten_morok_fence_gate_wall_open", "y": 90},
            "facing=west,in_wall=false,open=false":
                {"model": "riftworld:block/rotten_morok_fence_gate", "y": 270},
            "facing=west,in_wall=false,open=true":
                {"model": "riftworld:block/rotten_morok_fence_gate_open", "y": 90},
        }
    })

    # Нажимная плита и кнопка.
    _write_json(BLOCKSTATES / "rotten_morok_pressure_plate.json", {
        "variants": {
            "powered=false": {"model": "riftworld:block/rotten_morok_pressure_plate"},
            "powered=true": {"model": "riftworld:block/rotten_morok_pressure_plate_down"},
        }
    })
    _write_json(BLOCKSTATES / "rotten_morok_button.json", {
        "variants": {
            "face=floor,facing=north,powered=false": {"model": "riftworld:block/rotten_morok_button"},
            "face=floor,facing=north,powered=true": {"model": "riftworld:block/rotten_morok_button_pressed"},
            "face=floor,facing=south,powered=false": {"model": "riftworld:block/rotten_morok_button", "y": 180},
            "face=floor,facing=south,powered=true": {"model": "riftworld:block/rotten_morok_button_pressed", "y": 180},
            "face=floor,facing=east,powered=false": {"model": "riftworld:block/rotten_morok_button", "y": 90},
            "face=floor,facing=east,powered=true": {"model": "riftworld:block/rotten_morok_button_pressed", "y": 90},
            "face=floor,facing=west,powered=false": {"model": "riftworld:block/rotten_morok_button", "y": 270},
            "face=floor,facing=west,powered=true": {"model": "riftworld:block/rotten_morok_button_pressed", "y": 270},
            "face=wall,facing=north,powered=false": {"model": "riftworld:block/rotten_morok_button", "x": 90},
            "face=wall,facing=north,powered=true": {"model": "riftworld:block/rotten_morok_button_pressed", "x": 90},
            "face=wall,facing=south,powered=false": {"model": "riftworld:block/rotten_morok_button", "x": 90, "y": 180},
            "face=wall,facing=south,powered=true": {"model": "riftworld:block/rotten_morok_button_pressed", "x": 90, "y": 180},
            "face=wall,facing=west,powered=false": {"model": "riftworld:block/rotten_morok_button", "x": 90, "y": 270},
            "face=wall,facing=west,powered=true": {"model": "riftworld:block/rotten_morok_button_pressed", "x": 90, "y": 270},
            "face=wall,facing=east,powered=false": {"model": "riftworld:block/rotten_morok_button", "x": 90, "y": 90},
            "face=wall,facing=east,powered=true": {"model": "riftworld:block/rotten_morok_button_pressed", "x": 90, "y": 90},
            "face=ceiling,facing=north,powered=false": {"model": "riftworld:block/rotten_morok_button", "x": 180},
            "face=ceiling,facing=north,powered=true": {"model": "riftworld:block/rotten_morok_button_pressed", "x": 180},
            "face=ceiling,facing=south,powered=false": {"model": "riftworld:block/rotten_morok_button", "x": 180, "y": 180},
            "face=ceiling,facing=south,powered=true": {"model": "riftworld:block/rotten_morok_button_pressed", "x": 180, "y": 180},
            "face=ceiling,facing=east,powered=false": {"model": "riftworld:block/rotten_morok_button", "x": 180, "y": 270},
            "face=ceiling,facing=east,powered=true": {"model": "riftworld:block/rotten_morok_button_pressed", "x": 180, "y": 270},
            "face=ceiling,facing=west,powered=false": {"model": "riftworld:block/rotten_morok_button", "x": 180, "y": 90},
            "face=ceiling,facing=west,powered=true": {"model": "riftworld:block/rotten_morok_button_pressed", "x": 180, "y": 90},
        }
    })


def build_item_models() -> None:
    """Модели предметов: у бревна — как у бледного, у остального — item/generated."""
    # Бревно и древесина — как у бледного дерева: ссылка на block-модель,
    # у которой свои текстуры (верх/бок). Иначе торец бревна в инвентаре
    # был бы боком, и это видно сразу.
    for name in ("rotten_morok_log", "rotten_morok_wood",
                 "stripped_rotten_morok_log", "stripped_rotten_morok_wood"):
        _write_json(ROOT / "mod/src/main/resources/assets/riftworld/models/item" / f"{name}.json", {
            "parent": f"riftworld:block/{name}",
        })
    for name in ("rotten_morok_planks", "rotten_morok_planks_stairs", "rotten_morok_planks_slab",
                 "rotten_morok_fence", "rotten_morok_fence_gate", "rotten_morok_door",
                 "rotten_morok_trapdoor", "rotten_morok_pressure_plate", "rotten_morok_button",
                 "thorn_morok_door", "thorn_morok_trapdoor"):
        _write_json(ROOT / "mod/src/main/resources/assets/riftworld/models/item" / f"{name}.json", {
            "parent": f"riftworld:block/{name}",
        })


def main() -> int:
    tex = build_textures()
    build_models()
    build_blockstates()
    build_item_models()
    print(f"OK: гнилое дерево Морока — текстур: {len(tex)}, модели и blockstate — созданы")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
