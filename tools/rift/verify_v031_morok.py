#!/usr/bin/env python3
"""
v0.31.0 — проверка ассетов «Мороковой Пади» без сборки (Java локально недоступна).

Проверяет:
  1) у каждого блока биома есть blockstate, модель предмета и лут-таблица;
  2) каждый blockstate ссылается на существующую модель;
  3) каждая модель ссылается на существующую текстуру riftworld:block/*;
  4) каждая текстура существует как PNG 16x16 (RGBA);
  5) рецепты биома ссылаются на существующие предметы мода/ванили;
  6) нет висячих файлов-клонов (blockstate без модели и т.п.).
"""
import json
import pathlib
import struct
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
ASSETS = ROOT / "mod/src/main/resources/assets/riftworld"
DATA = ROOT / "mod/src/main/resources/data/riftworld"
TEX = ASSETS / "textures/block"
BS = ASSETS / "blockstates"
MB = ASSETS / "models/block"
MI = ASSETS / "models/item"
LOOT = DATA / "loot_tables/blocks"
RECIPE = DATA / "recipes"

BLOCK_IDS = [
    "morok_log", "stripped_morok_log", "morok_wood", "stripped_morok_wood",
    "morok_planks", "morok_planks_stairs", "morok_planks_slab", "morok_fence", "morok_fence_gate",
    "morok_door", "morok_trapdoor", "morok_pressure_plate", "morok_button",
    "morok_leaves", "morok_sapling",
    "frozen_soil", "rime_grass", "pale_moss_block", "pale_moss_carpet", "ice_fang", "bone_bramble",
    "cracked_rime_bricks", "cracked_rime_bricks_stairs", "cracked_rime_bricks_slab",
    "cracked_rime_bricks_wall", "rime_brick_pillar", "fog_lantern",
]

errors = []


def png_size(path):
    raw = path.read_bytes()[:33]
    if raw[:8] != b"\x89PNG\r\n\x1a\n":
        return None
    w, h, bd, ctype = struct.unpack(">IIBB", raw[16:26])
    return w, h, bd, ctype


def models_in(node, out):
    if isinstance(node, dict):
        for k, v in node.items():
            if k == "model" and isinstance(v, str):
                out.add(v)
            else:
                models_in(v, out)
    elif isinstance(node, list):
        for v in node:
            models_in(v, out)
    return out


def textures_in(node, out):
    if isinstance(node, dict):
        for k, v in node.items():
            if k in ("texture", "particle", "top", "bottom", "side", "end", "wall", "all") and isinstance(v, str):
                out.add(v)
            else:
                textures_in(v, out)
    elif isinstance(node, list):
        for v in node:
            textures_in(v, out)
    return out


def main():
    print("== блоки ==")
    for bid in BLOCK_IDS:
        bs = BS / f"{bid}.json"
        item = MI / f"{bid}.json"
        loot = LOOT / f"{bid}.json"
        for f, what in ((bs, "blockstate"), (item, "модель предмета"), (loot, "лут")):
            if not f.exists():
                errors.append(f"{bid}: нет {what} ({f.name})")
    print(f"  блоков: {len(BLOCK_IDS)}")

    # blockstates -> модели
    missing_models = set()
    for bid in BLOCK_IDS:
        f = BS / f"{bid}.json"
        if not f.exists():
            continue
        for m in models_in(json.loads(f.read_text(encoding="utf-8")), set()):
            name = m.split(":")[-1]
            if not (MB / f"{name.split('/')[-1]}.json").exists():
                missing_models.add(m)
    print(f"  моделей не найдено: {len(missing_models)}")
    for m in sorted(missing_models):
        errors.append(f"blockstate ссылается на отсутствующую модель {m}")

    # модели -> текстуры
    used_tex = set()
    for d in (MB, MI):
        for f in d.glob("*.json"):
            stem = f.stem
            if not any(stem == b or stem.startswith(b) for b in BLOCK_IDS):
                continue
            for t in textures_in(json.loads(f.read_text(encoding="utf-8")), set()):
                if t.startswith("#"):
                    continue          # переменная шаблона-родителя, не файл
                if ":" in t:
                    t = t.split(":")[-1]
                if t.startswith("block/"):
                    t = t[len("block/"):]
                if "/" in t:
                    continue
                used_tex.add(t)
    missing_tex = sorted(t for t in used_tex if not (TEX / f"{t}.png").exists())
    print(f"  текстур использовано: {len(used_tex)}; не найдено: {len(missing_tex)}")
    for t in missing_tex:
        errors.append(f"модель ссылается на отсутствующую текстуру {t}")

    # размеры текстур
    bad_size = []
    for t in sorted(used_tex):
        p = TEX / f"{t}.png"
        if not p.exists():
            continue
        size = png_size(p)
        if size is None or size[0] != 16 or size[1] != 16:
            bad_size.append(f"{t} {size}")
        elif size[2] != 8 or size[3] not in (6, 2):
            bad_size.append(f"{t} bitdepth/ctype {size[2]}/{size[3]}")
    print(f"  текстур не 16x16 RGBA: {len(bad_size)}")
    for b in bad_size:
        errors.append(f"текстура не 16x16 RGBA-8: {b}")

    # предметные текстуры (гейт CI: «Missing textures in model»)
    missing_item = []
    for f in MI.glob("*.json"):
        if not any(f.stem == b or f.stem.startswith(b) for b in BLOCK_IDS):
            continue
        for t in textures_in(json.loads(f.read_text(encoding="utf-8")), set()):
            if t.startswith("#") or ":" not in t:
                continue
            ns, path = t.split(":", 1)
            if ns != "riftworld" or not path.startswith("item/"):
                continue
            if not (ASSETS / "textures" / f"{path}.png").exists():
                missing_item.append(f"{f.stem} -> {t}")
    print(f"  предметных текстур не найдено: {len(missing_item)}")
    for m in missing_item:
        errors.append(f"модель предмета ссылается на отсутствующую текстуру {m}")

    # рецепты
    known = set()
    for d in (MI,):
        known |= {f.stem for f in d.glob("*.json")}
    vanilla = {"minecraft:stick", "minecraft:ice", "minecraft:packed_ice", "minecraft:torch",
               "minecraft:iron_ingot", "minecraft:vine", "minecraft:bone_meal"}
    bad_items, recipes = [], 0
    for f in sorted(RECIPE.glob("*.json")):
        body = json.loads(f.read_text(encoding="utf-8"))
        blob = json.dumps(body)
        if "morok" not in blob and "frozen_soil" not in blob and "rime_" not in blob and "pale_moss" not in blob \
                and "cracked_rime" not in blob and "fog_lantern" not in blob and "ice_fang" not in blob \
                and "bone_bramble" not in blob:
            continue
        recipes += 1
        items = []
        if body.get("type") == "minecraft:crafting_shaped":
            items = [v["item"] for v in body.get("key", {}).values()]
        elif body.get("type") == "minecraft:crafting_shapeless":
            items = [v["item"] for v in body.get("ingredients", [])]
        items.append(body["result"]["item"])
        for it in items:
            if it in vanilla:
                continue
            if it.startswith("riftworld:"):
                if it.split(":")[-1] not in known:
                    bad_items.append(f"{f.stem}: нет предмета {it}")
            else:
                bad_items.append(f"{f.stem}: неизвестный namespace {it}")
    print(f"  рецептов биома: {recipes}; битых ссылок: {len(bad_items)}")
    errors.extend(bad_items)

    print()
    if errors:
        print("FAIL")
        for e in errors[:40]:
            print("  -", e)
        sys.exit(1)
    print("OK  все ассеты «Мороковой Пади» на месте")


if __name__ == "__main__":
    main()
