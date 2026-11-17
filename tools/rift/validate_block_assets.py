#!/usr/bin/env python3
"""v0.55.0 — проверка ассетов блоков Riftworld.

Мод уже переживал жалобы «у блоков нет текстур в инвентаре и в мире».
Проверка ищет настоящую причину: не сами файлы PNG, а **ссылки** на них.

Что проверяется для каждого зарегистрированного блока:

1. `blockstates/<id>.json` существует.
2. Все модели, на которые ссылается blockstate, существуют в `models/block/`.
3. Все текстуры, на которые ссылается модель (включая родительские цепочки),
   существуют в `textures/`.
4. `models/item/<id>.json` существует — иначе в инвентаре будет
   фиолетовый квадрат.
5. Ни одна текстура не «переименована в двух словах» (частая ошибка:
   в модели `morok_planks`, а на диске `morok_plank`).

Запуск:
    python tools/rift/validate_block_assets.py
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "mod/src/main/resources/assets/riftworld"
BLOCKS = ASSETS / "blockstates"
MODELS = ASSETS / "models/block"
ITEMS = ASSETS / "models/item"
TEXTURES = ASSETS / "textures"
JAVA_ROOT = ROOT / "mod/src/main/java/ru/aiforge/riftworld"

# Файлы, в которых регистрируются блоки.
REGISTRY_FILES = [
    JAVA_ROOT / "registry/ElisarBlocks.java",
    JAVA_ROOT / "registry/MorokBlocks.java",
    JAVA_ROOT / "registry/ModBlocks.java",
    JAVA_ROOT / "registry/CastleBlocks.java",
    JAVA_ROOT / "registry/BlacktideBlocks.java",
    JAVA_ROOT / "registry/KeepBlocks.java",
    JAVA_ROOT / "registry/LabBlocks.java",
]


def registered_block_ids() -> list[str]:
    """Все id блоков, зарегистрированных через ModBlocks.reg/cube/stairs/... .

    Берём прямо из исходников реестра: так проверка не расходится с модом.
    """
    ids: set[str] = set()
    call = re.compile(
        r"\b(?:ModBlocks\.reg|cube|stairs|slab|wall|stoneWall|woodPillar|woodDoor|"
        r"woodTrapdoor|woodFence|woodGate|woodPlate|woodButton|strippableWood)"
        r"\s*\(\s*\"([a-z0-9_]+)\"")
    for path in REGISTRY_FILES:
        if not path.exists():
            continue
        src = path.read_text(encoding="utf-8")
        for m in call.finditer(src):
            ids.add(m.group(1))
    return sorted(ids)


def load_json(path: Path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None


def collect_model_refs(node, out: set[str]) -> None:
    """Все значения "model" из blockstate/multipart."""
    if isinstance(node, dict):
        model = node.get("model")
        if isinstance(model, str):
            out.add(model)
        for value in node.values():
            collect_model_refs(value, out)
    elif isinstance(node, list):
        for item in node:
            collect_model_refs(item, out)


def collect_texture_refs(node, out: set[str]) -> None:
    """Все значения из секций "textures" (и "particle"/"all"/"top"/...)."""
    if isinstance(node, dict):
        for key, value in node.items():
            if key == "textures" and isinstance(value, dict):
                for tex in value.values():
                    if isinstance(tex, str):
                        out.add(tex)
            else:
                collect_texture_refs(value, out)
    elif isinstance(node, list):
        for item in node:
            collect_texture_refs(item, out)


def model_name(model_id: str) -> str:
    """Имя модели без namespace и без папки block/: riftworld:block/x -> x."""
    return model_id.split(":")[-1].split("/")[-1]


def model_textures(model_id: str, seen: set[str] | None = None) -> tuple[set[str], list[str]]:
    """Текстуры модели вместе с её родителями. Возвращает (текстуры, ошибки)."""
    seen = seen if seen is not None else set()
    errors: list[str] = []
    if model_id in seen:
        return set(), errors
    seen.add(model_id)

    name = model_name(model_id)
    path = MODELS / f"{name}.json"
    if not path.exists():
        return set(), [f"нет модели {model_id}"]

    data = load_json(path)
    if data is None:
        return set(), [f"модель {model_id} не читается как JSON"]

    textures: set[str] = set()
    collect_texture_refs(data, textures)

    parent = data.get("parent")
    if isinstance(parent, str) and not parent.startswith("minecraft:"):
        parent_tex, parent_err = model_textures(parent, seen)
        textures |= parent_tex
        errors += parent_err
    return textures, errors


def main() -> int:
    ids = registered_block_ids()
    if not ids:
        print("Не нашлось ни одного зарегистрированного блока", file=sys.stderr)
        return 1

    problems: list[str] = []
    checked = 0

    for bid in ids:
        # Модели блоков бывают двух видов: одна на весь блок (cube_all) или
        # набор частей (дверь, забор, стена) — тогда верхнего models/block/<id>.json
        # может не быть, и это нормально. Проверяем то, что реально нужно.
        bs_path = BLOCKS / f"{bid}.json"
        if not bs_path.exists():
            problems.append(f"{bid}: нет blockstates/{bid}.json")
            continue

        bs = load_json(bs_path)
        if bs is None:
            problems.append(f"{bid}: blockstates/{bid}.json не читается как JSON")
            continue

        refs: set[str] = set()
        collect_model_refs(bs, refs)
        if not refs:
            problems.append(f"{bid}: blockstate не ссылается ни на одну модель")
            continue

        for ref in sorted(refs):
            checked += 1
            name = model_name(ref)
            if not (MODELS / f"{name}.json").exists():
                problems.append(f"{bid}: blockstate ссылается на несуществующую модель {ref}")
                continue
            textures, errs = model_textures(ref)
            for err in errs:
                problems.append(f"{bid}: {err}")
            for tex in sorted(textures):
                if tex.startswith("#"):
                    continue
                if not tex.startswith("riftworld:"):
                    # Ванильная текстура — её проверять нечем.
                    continue
                # riftworld:block/ambra_log -> textures/block/ambra_log.png
                rel = tex.split(":", 1)[1] + ".png"
                if not (TEXTURES / rel).exists():
                    problems.append(f"{bid}: модель {ref} ссылается на несуществующую текстуру {tex}")

        item_model = ITEMS / f"{bid}.json"
        if not item_model.exists():
            problems.append(f"{bid}: нет models/item/{bid}.json — в инвентаре будет пустота")

    print(f"Проверено блоков: {len(ids)} (моделей: {checked})")
    if problems:
        print(f"ОШИБОК: {len(problems)}")
        for p in problems:
            print("  -", p)
        return 1
    print("OK: у всех блоков есть blockstate, модели, предмет и текстуры.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
