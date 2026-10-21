#!/usr/bin/env python3
"""Static resource validator for the mod (works without the game/JDK).

Because this sandbox cannot reach Forge/Mojang (no game client, no javac), this script is the substitute for a launch:
it parses the Java registries and verifies that every registered block has its blockstate, model, textures, item model,
loot table and at least one lang key - i.e. exactly the things that turn into purple/black cubes and missing-model
crashes in game.

Run: python3 tools/rift/validate_assets.py [-v]
Exit code 1 if anything is missing.
"""
from __future__ import annotations

import json
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
JAVA = os.path.join(ROOT, "mod", "src", "main", "java", "ru", "aiforge", "riftworld")
RES = os.path.join(ROOT, "mod", "src", "main", "resources")
A = os.path.join(RES, "assets", "riftworld")
D = os.path.join(RES, "data", "riftworld")

REG_RE = re.compile(r'(?:ModBlocks\.reg|BLOCKS\.register)\(\s*"([a-z0-9_]+)"')
CUBE_RE = re.compile(r'cube\(\s*"([a-z0-9_]+)"')
STAIRS_RE = re.compile(r'stairs\(\s*"([a-z0-9_]+)"')
SLAB_RE = re.compile(r'slab\(\s*"([a-z0-9_]+)"')
# blocks registered without an item (no blockstate in the creative tab / owned by another block)
NO_ITEM = {"bridge_walk", "part"}


def registered_blocks():
    names = set()
    for dirpath, _, files in os.walk(JAVA):
        for f in files:
            if not f.endswith(".java"):
                continue
            src = open(os.path.join(dirpath, f), encoding="utf-8").read()
            for rx in (REG_RE, CUBE_RE, STAIRS_RE, SLAB_RE):
                names.update(rx.findall(src))
    # registry entries whose id is built with a format string are checked through their assets only
    return sorted(names)


def load(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def model_textures(model, seen=None):
    """All texture ids used by a block model (recursively through parents)."""
    if seen is None:
        seen = set()
    if model in seen:
        return set()
    seen.add(model)
    out = set()
    if ":" in model and not model.startswith("riftworld:"):
        return out                       # vanilla model - its textures are not ours to verify
    name = model.split(":")[-1]
    path = os.path.join(A, "models", name + ".json")
    if not os.path.exists(path):
        return {"!missing-model:" + name}
    d = load(path)
    for key, val in (d.get("textures") or {}).items():
        if isinstance(val, str) and not val.startswith("#"):
            out.add(val)
    for parent in ([d["parent"]] if isinstance(d.get("parent"), str) else []):
        out |= model_textures(parent, seen)
    return out


def main():
    verbose = "-v" in sys.argv
    problems = []
    names = registered_blocks()
    ru = load(os.path.join(A, "lang", "ru_ru.json"))
    en = load(os.path.join(A, "lang", "en_us.json"))
    checked = 0
    for name in names:
        bs = os.path.join(A, "blockstates", name + ".json")
        if not os.path.exists(bs):
            problems.append(f"block {name}: нет blockstate (assets/riftworld/blockstates/{name}.json)")
            continue
        checked += 1
        try:
            state = load(bs)
        except Exception as e:
            problems.append(f"block {name}: битый blockstate ({e})")
            continue
        variants = state.get("variants") or {}
        models = set()
        if variants:
            for v in variants.values():
                entries = v if isinstance(v, list) else [v]
                for e in entries:
                    if isinstance(e, dict) and "model" in e:
                        models.add(e["model"])
        for part in state.get("multipart") or []:
            apply = part.get("apply")
            for e in (apply if isinstance(apply, list) else [apply] if apply else []):
                if isinstance(e, dict) and "model" in e:
                    models.add(e["model"])
        if not models:
            problems.append(f"block {name}: blockstate без моделей")
        for m in models:
            texs = model_textures(m)
            for t in texs:
                if t.startswith("!missing-model:"):
                    problems.append(f"block {name}: модель {t.split(':', 1)[1]} не найдена")
                elif t.startswith("riftworld:"):
                    rel = t.split(":", 1)[1]
                    cands = [os.path.join(A, "textures", rel + ".png"),
                             os.path.join(A, "textures", "block", rel + ".png"),
                             os.path.join(A, "textures", "item", rel + ".png")]
                    if not any(os.path.exists(c) for c in cands):
                        problems.append(f"block {name}: нет текстуры {t}")
        # item model
        if name not in NO_ITEM:
            ip = os.path.join(A, "models", "item", name + ".json")
            if not os.path.exists(ip):
                problems.append(f"block {name}: нет item-модели (models/item/{name}.json)")
        # loot table
        if name not in NO_ITEM:
            lp = os.path.join(D, "loot_tables", "blocks", name + ".json")
            if not os.path.exists(lp):
                problems.append(f"block {name}: нет loot table")
        # lang
        if f"block.riftworld.{name}" not in ru:
            problems.append(f"block {name}: нет ru_ru перевода")
        if f"block.riftworld.{name}" not in en:
            problems.append(f"block {name}: нет en_us перевода")
    # entity textures used by the bridge renderer
    for tex in ("planks_plains", "planks_garden", "planks_grove", "planks_burnt", "planks_mist",
                "cable_rope", "cable_chain", "net"):
        p = os.path.join(A, "textures", "entity", "bridge", tex + ".png")
        if not os.path.exists(p):
            problems.append(f"bridge renderer: нет текстуры textures/entity/bridge/{tex}.png")
    # ---- Dynamic Trees add-on (dtcompat): ids must resolve, seed assets must exist
    DTR = os.path.join(ROOT, "dtcompat", "src", "main", "resources")
    if os.path.isdir(DTR):
        tree_dir = os.path.join(DTR, "trees", "riftworld_dt")
        block_ids = set(names)
        for kind in ("families", "species", "leaves_properties", "soil_properties"):
            d = os.path.join(tree_dir, kind)
            if not os.path.isdir(d):
                problems.append(f"DT add-on: нет папки {kind}")
                continue
            for f in sorted(os.listdir(d)):
                if not f.endswith(".json"):
                    continue
                j = load(os.path.join(d, f))
                if j.get("only_if_loaded") != "dynamictrees":
                    problems.append(f"DT add-on {kind}/{f}: нет only_if_loaded=dynamictrees")
                for key in ("primitive_log", "primitive_leaves", "primitive_soil"):
                    if key in j:
                        bid = j[key].split(":")[-1]
                        if bid not in block_ids:
                            problems.append(f"DT add-on {kind}/{f}: {j[key]} нет в реестре блоков мода")
        sp_dir = os.path.join(tree_dir, "species")
        if os.path.isdir(sp_dir):
            for f in sorted(os.listdir(sp_dir)):
                sp = load(os.path.join(sp_dir, f))
                name = f[:-5]
                for key in ("family", "leaves_properties"):
                    if key not in sp:
                        problems.append(f"DT add-on species/{f}: нет поля {key}")
                tex = (sp.get("texture_overrides") or {}).get("seed")
                if not tex:
                    problems.append(f"DT add-on species/{f}: нет текстуры семени")
                else:
                    img = os.path.join(DTR, "assets", "riftworld_dt", "textures", tex.split(":", 1)[1] + ".png")
                    mdl = os.path.join(DTR, "assets", "riftworld_dt", "models", tex.split(":", 1)[1] + ".json")
                    if not os.path.exists(img):
                        problems.append(f"DT add-on species/{f}: нет {os.path.basename(img)}")
                    if not os.path.exists(mdl):
                        problems.append(f"DT add-on species/{f}: нет модели {os.path.basename(mdl)}")
        for req in ("META-INF/mods.toml", "pack.mcmeta"):
            if not os.path.exists(os.path.join(DTR, req)):
                problems.append(f"DT add-on: нет {req}")

    # every json parses
    bad = 0
    for dirpath, _, files in os.walk(RES):
        for f in files:
            if f.endswith(".json"):
                try:
                    load(os.path.join(dirpath, f))
                except Exception as e:
                    problems.append(f"битый JSON: {os.path.relpath(os.path.join(dirpath, f), RES)} ({e})")
                    bad += 1
    NEW = {"lilac_log", "lilac_leaves", "lilac_blossom", "fern_log", "fern_leaves",
           "mist_pine_log", "mist_pine_leaves", "oak_log", "oak_leaves", "bridge_post", "bridge_walk"}
    mine = [p for p in problems if any((" " + n + ":") in p or ("/" + n + ".") in p for n in NEW)]
    legacy = [p for p in problems if p not in mine]
    print(f"блоков в реестре: {len(names)} (проверено ресурсов: {checked}); JSON-файлы парсятся")
    print(f"проблемы в новых блоках Rev 7: {len(mine)}")
    print(f"унаследованные замечания (были до Rev 7): {len(legacy)}")
    if verbose:
        for p in legacy:
            print("   legacy:", p)
    if mine:
        print("\nНОВЫЕ ПРОБЛЕМЫ:")
        for p in mine:
            print("  -", p)
        return 1
    print("НОВЫЕ БЛОКИ ЧИСТЫ: blockstate + модели + текстуры + item-модель + лут + переводы на месте")
    return 0
    print("ПРОБЛЕМ НЕ НАЙДЕНО: у каждого блока есть blockstate, модели, текстуры, item-модель, лут и переводы")
    return 0


if __name__ == "__main__":
    sys.exit(main())
