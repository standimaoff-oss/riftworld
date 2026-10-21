#!/usr/bin/env python3
"""Rebuilds `dt_schema.json` — the list of JSON keys Dynamic Trees 1.20.1 really understands.

A regex over the loader sources alone is not enough: some keys are never registered as appliers (species `family` is
resolved by the registry loader, not by `SpeciesResourceLoader`), so they would look illegal while DT ships them in its
own pack. Therefore the schema is the UNION of two independent sources:

  1. every `loadAppliers/gatherDataAppliers/reloadAppliers/commonAppliers.register("...")` call in the four loaders;
  2. every top-level key used by Dynamic Trees' OWN shipped data pack
     (`src/main/resources/trees/dynamictrees/{species,families,leaves_properties,soil_properties}/*.json`).

Kits are taken from `CellKits.java`, `GrowthLogicKits.java` (and the shipped leaves/soil files as a cross-check).

Network: api.github.com only (that is what works in this sandbox). Cached under tools/rift/.cache/dt_src so re-runs are
offline. Token from GITHUB_TOKEN if present (60 req/h anonymous, 5000 with a token).

Run: python3 tools/rift/fetch_dt_schema.py [--refresh]
"""
from __future__ import annotations

import json
import os
import re
import subprocess
import sys
import urllib.request

REF = "release/1.20.1"
REPO = "DynamicTreesTeam/DynamicTrees"
HERE = os.path.dirname(os.path.abspath(__file__))
CACHE = os.path.join(HERE, ".cache", "dt_src")
OUT = os.path.join(HERE, "dt_schema.json")

LOADERS = {
    "species": "src/main/java/com/ferreusveritas/dynamictrees/resources/loader/SpeciesResourceLoader.java",
    "family": "src/main/java/com/ferreusveritas/dynamictrees/resources/loader/FamilyResourceLoader.java",
    "leaves_properties": "src/main/java/com/ferreusveritas/dynamictrees/resources/loader/LeavesPropertiesResourceLoader.java",
    "soil_properties": "src/main/java/com/ferreusveritas/dynamictrees/resources/loader/SoilPropertiesResourceLoader.java",
}
SOIL_HELPER = "src/main/java/com/ferreusveritas/dynamictrees/block/rooty/SoilHelper.java"
KITS = {
    "cell_kits": "src/main/java/com/ferreusveritas/dynamictrees/cell/CellKits.java",
    "growth_logic_kits": "src/main/java/com/ferreusveritas/dynamictrees/growthlogic/GrowthLogicKits.java",
}
# extra keys that live in the shared registry loader / codec and are used by DT's own pack
EXTRA_KEYS = {"species": {"family"}}
DATA_DIRS = ["species", "families", "leaves_properties", "soil_properties"]


def fetch(path: str, refresh: bool) -> str:
    local = os.path.join(CACHE, path.replace("/", os.sep))
    if os.path.exists(local) and not refresh:
        return open(local, encoding="utf-8").read()
    os.makedirs(os.path.dirname(local), exist_ok=True)
    url = f"https://api.github.com/repos/{REPO}/contents/{path}?ref={REF}"
    try:
        out = subprocess.run(["gh", "api", f"repos/{REPO}/contents/{path}?ref={REF}", "--jq", ".content"],
                             check=True, capture_output=True, text=True).stdout
        import base64
        text = base64.b64decode(out).decode("utf-8")
    except (FileNotFoundError, subprocess.CalledProcessError):
        req = urllib.request.Request(url, headers={"Accept": "application/vnd.github.raw"})
        tok = os.environ.get("GITHUB_TOKEN")
        if tok:
            req.add_header("Authorization", "Bearer " + tok)
        with urllib.request.urlopen(req, timeout=60) as r:
            text = r.read().decode("utf-8")
    open(local, "w", encoding="utf-8").write(text)
    return text


def listing(refresh: bool) -> list[str]:
    local = os.path.join(CACHE, "_tree.txt")
    if os.path.exists(local) and not refresh:
        return open(local, encoding="utf-8").read().split("\n")
    out = subprocess.run(["gh", "api", f"repos/{REPO}/git/trees/{REF}?recursive=1", "--jq", ".tree[].path"],
                         check=True, capture_output=True, text=True).stdout
    open(local, "w", encoding="utf-8").write(out)
    return out.split("\n")


def applier_keys(src: str) -> set[str]:
    return set(re.findall(r'\.register(?:Array|Map)?Applier\(\s*"([a-z0-9_]+)"', src)) | \
           set(re.findall(r'\.register\(\s*"([a-z0-9_]+)"', src))


def applier_types(src: str) -> dict[str, dict[str, list[str]]]:
    """key -> {"single"/"array"/"map": [declared Java types]}.

    DT deserialises each key into a declared class; a value of the wrong JSON shape is rejected with a log line and
    the parameter is simply not applied. Knowing the types turns that into a static check.
    """
    out: dict[str, dict[str, list[str]]] = {}

    def add(key: str, shape: str, typ: str) -> None:
        out.setdefault(key, {}).setdefault(shape, []).append(typ)

    for m in re.finditer(r'\.register\(\s*"([a-z0-9_]+)"\s*,\s*([A-Za-z0-9_.$]+)\.class', src):
        add(m.group(1), "single", m.group(2).split(".")[-1])
    for m in re.finditer(r'\.registerArrayApplier\(\s*"([a-z0-9_]+)"\s*,\s*([A-Za-z0-9_.$]+)\.class', src):
        add(m.group(1), "array", m.group(2).split(".")[-1])
    for m in re.finditer(r'\.registerMapApplier\(\s*"([a-z0-9_]+)"\s*,\s*([A-Za-z0-9_.$]+)\.class', src):
        add(m.group(1), "map", m.group(2).split(".")[-1])
    return out


def kit_names(src: str) -> list[str]:
    """Kits are code objects: `new CellKit(DynamicTrees.location("deciduous")) {...}` and friends."""
    return sorted(set(re.findall(r'DynamicTrees\.location\(\s*"([a-z0-9_]+)"', src)))


def main() -> int:
    refresh = "--refresh" in sys.argv
    schema: dict[str, object] = {}
    provenance: dict[str, object] = {}
    for kind, path in LOADERS.items():
        src = fetch(path, refresh)
        keys = applier_keys(src) | EXTRA_KEYS.get(kind, set())
        types = applier_types(src)
        dirname = {"species": "species", "family": "families",
                   "leaves_properties": "leaves_properties", "soil_properties": "soil_properties"}[kind]
        pack_files = [p for p in listing(refresh)
                      if p.startswith(f"src/main/resources/trees/dynamictrees/{dirname}/") and p.endswith(".json")]
        from_pack = set()
        numeric: dict[str, list[float]] = {}
        for p in pack_files:
            try:
                data = json.loads(fetch(p, refresh))
            except (json.JSONDecodeError, subprocess.CalledProcessError):
                continue
            from_pack |= set(data.keys())
            for k, v in data.items():
                if isinstance(v, (int, float)) and not isinstance(v, bool):
                    numeric.setdefault(k, []).append(float(v))
        missing = sorted(from_pack - keys)     # documented gap: keys DT ships but the loader never registers
        keys |= from_pack
        schema[kind] = {"keys": sorted(keys), "types": dict(sorted(types.items())),
                        # what DT itself uses: a parameter far outside this band is almost certainly a typo
                        "reference_ranges": {k: [min(v), max(v), len(v)] for k, v in sorted(numeric.items())
                                             if len(v) >= 3}}
        provenance[kind] = {"loader": path, "pack_files": len(pack_files),
                            "only_in_pack": missing}
    # soil tag names: the constants of SoilHelper plus everything DT's own species accept
    helper = fetch(SOIL_HELPER, refresh)
    soil_tags = set(re.findall(r'public static final String [A-Z_]+\s*=\s*"([a-z0-9_]+)"', helper))
    for p in listing(refresh):
        if p.startswith("src/main/resources/trees/dynamictrees/species/") and p.endswith(".json"):
            try:
                data = json.loads(fetch(p, refresh))
            except (json.JSONDecodeError, subprocess.CalledProcessError):
                continue
            for key in ("acceptable_soils", "world_gen_acceptable_soils"):
                soil_tags |= set(data.get(key) or [])
    schema["soil_tags"] = sorted(soil_tags)
    provenance["soil_tags"] = {"source": SOIL_HELPER,
                               "note": "константы SoilHelper + все значения acceptable_soils в паке DT"}

    for kind, path in KITS.items():
        src = fetch(path, refresh)
        schema[kind] = kit_names(src)
        provenance[kind] = {"source": path}
    schema["_meta"] = {
        "dynamic_trees": REF,
        "note": "объединение ключей из код-лоадеров DT и из его собственного пака; см. fetch_dt_schema.py",
        "provenance": provenance,
    }
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(schema, f, ensure_ascii=False, indent=1)
        f.write("\n")
    for kind in LOADERS:
        print(f"{kind}: {len(schema[kind]['keys'])} ключей "
              f"({provenance[kind]['pack_files']} файлов пака DT, {len(provenance[kind]['only_in_pack'])} только из пака)")
    for kind in KITS:
        print(f"{kind}: {', '.join(schema[kind])}")
    print(f"soil_tags: {', '.join(schema['soil_tags'])}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
