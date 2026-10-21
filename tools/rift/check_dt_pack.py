#!/usr/bin/env python3
"""Validates the Dynamic Trees add-on against Dynamic Trees' OWN source.

The pack is pure JSON, so a typo is silent: DT simply ignores an unknown key and the tree grows with defaults — or,
worse, a species references a block that does not exist and the tree never appears. This checker closes that hole
without launching the game:

  * `tools/rift/dt_schema.json` is extracted from the DT 1.20.1 sources
    (`SpeciesResourceLoader`, `FamilyResourceLoader`, `LeavesPropertiesResourceLoader`,
    `SoilPropertiesResourceLoader`, `CellKits`, `GrowthLogicKits`) — see `fetch_dt_schema.py`;
  * every key of every pack file is checked against that schema;
  * cell kits / growth logic kits are checked against the kits DT really registers;
  * primitive blocks, biomes and textures are checked against this mod's registry and resources;
  * seed assets, world-gen selectors and mods.toml dependencies are checked for completeness.

Run: python3 tools/rift/check_dt_pack.py [-v]
Exit code 1 on any problem.
"""
from __future__ import annotations

import json
import os
import re
import sys
import tomllib

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
PACK_ID = "riftworld_dt"
DT_RES = os.path.join(ROOT, "dtcompat", "src", "main", "resources")
PACK = os.path.join(DT_RES, "trees", PACK_ID)
ASSETS = os.path.join(DT_RES, "assets", PACK_ID)
MOD_RES = os.path.join(ROOT, "mod", "src", "main", "resources")
JAVA = os.path.join(ROOT, "mod", "src", "main", "java")

# keys whose DT sample is too small / whose check lives elsewhere
REFERENCE_EXEMPT = {"max_branch_radius", "lowest_branch_height"}

problems: list[str] = []
warnings: list[str] = []


# declared Java type -> acceptable JSON kinds. Types DT deserialises with its own deserialisers (Block, Item, CellKit,
# GrowthLogicKitConfiguration, DTBiomeHolderSet, ...) are not constrained here: those accept strings and objects.
JAVA_KINDS = {
    "Boolean": {"bool"},
    "Integer": {"int"},
    "Float": {"number"},
    "Double": {"number"},
    "String": {"str"},
    "ResourceLocation": {"str"},
    "JsonObject": {"dict"},
}


def json_kind(value) -> str:
    if isinstance(value, bool):
        return "bool"
    if isinstance(value, int):
        return "int"
    if isinstance(value, float):
        return "number"
    if isinstance(value, str):
        return "str"
    if isinstance(value, list):
        return "list"
    if isinstance(value, dict):
        return "dict"
    return "?"


def check_types(kind: str, path: str, data: dict, schema: dict) -> None:
    """DT deserialises every key into a declared Java class; a wrong JSON shape is logged and the value dropped."""
    types = schema[kind].get("types") or {}
    name = f"{kind}/{os.path.basename(path)}"
    for key, value in data.items():
        decl = types.get(key)
        if not decl:                        # key DT reads in its own code path (e.g. species `family`): no type info
            continue
        got = json_kind(value)
        # pick the shape the file uses, if DT knows that shape at all
        shape = "array" if isinstance(value, list) and "array" in decl else \
                "map" if isinstance(value, dict) and "map" in decl else \
                "single" if "single" in decl else None
        if shape is None:
            want = "/".join(k for k in ("single", "array", "map") if k in decl)
            err(f"{name}: «{key}» — {got}, а DT принимает только {want}")
            continue
        java_types = decl[shape]
        known = {j for j in java_types if j in JAVA_KINDS}
        if not known:                       # custom deserialiser: any shape it wants
            continue
        allowed = set().union(*(JAVA_KINDS[j] for j in known))
        if shape == "array":
            if got != "list":
                err(f"{name}: «{key}» — {got}, а DT ждёт массив ({', '.join(java_types)})")
            elif got == "list" and value:
                bad = [v for v in value if json_kind(v) not in allowed]
                if bad:
                    err(f"{name}: «{key}» — в массиве {json_kind(bad[0])}, а DT ждёт {', '.join(java_types)}")
        elif shape == "map":
            if got != "dict":
                err(f"{name}: «{key}» — {got}, а DT ждёт объект ({', '.join(java_types)})")
            elif got == "dict":
                bad = [v for v in value.values() if json_kind(v) not in allowed]
                if bad:
                    err(f"{name}: «{key}» — значение {json_kind(bad[0])}, а DT ждёт {', '.join(java_types)}")
        elif got not in allowed:
            err(f"{name}: «{key}» — {got}, а DT ждёт {', '.join(java_types)}")


def load(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def err(msg: str) -> None:
    problems.append(msg)


def warn(msg: str) -> None:
    warnings.append(msg)


def registered_blocks() -> set[str]:
    rx = re.compile(r'(?:ModBlocks\.reg|BLOCKS\.register)\(\s*"([a-z0-9_]+)"')
    cube = re.compile(r'(?:cube|stairs|slab)\(\s*"([a-z0-9_]+)"')
    out = set()
    for dirpath, _, files in os.walk(JAVA):
        for f in files:
            if f.endswith(".java"):
                src = open(os.path.join(dirpath, f), encoding="utf-8").read()
                out |= set(rx.findall(src)) | set(cube.findall(src))
    return out


def biomes() -> set[str]:
    d = os.path.join(MOD_RES, "data", "riftworld", "worldgen", "biome")
    return {"riftworld:" + f[:-5] for f in os.listdir(d) if f.endswith(".json")}


def texture_exists(res: str) -> bool:
    """res = 'namespace:path/to/texture' (namespace may be omitted)."""
    ns, _, path = res.partition(":")
    if not path:
        ns, path = "minecraft", ns
    if ns == PACK_ID:
        return os.path.exists(os.path.join(ASSETS, "textures", path.replace("/", os.sep) + ".png"))
    if ns == "riftworld":
        return os.path.exists(os.path.join(MOD_RES, "assets", "riftworld", "textures",
                                           path.replace("/", os.sep) + ".png"))
    return True          # vanilla / other mods: not ours to verify


def check_keys(kind: str, path: str, data: dict, schema: dict) -> None:
    allowed = set(schema[kind]["keys"])
    for key in data:
        if key.startswith("_comment"):
            continue
        if key not in allowed:
            err(f"{kind}/{os.path.basename(path)}: неизвестный DT-ключ «{key}» "
                f"(DT его молча проигнорирует — параметр не подействует)")


def main() -> int:
    global problems, warnings
    problems, warnings = [], []
    verbose = "-v" in sys.argv
    schema = load(os.path.join(HERE, "dt_schema.json"))
    blocks = registered_blocks()
    biome_ids = biomes()
    families = {f[:-5] for f in os.listdir(os.path.join(PACK, "families")) if f.endswith(".json")}
    species = {f[:-5] for f in os.listdir(os.path.join(PACK, "species")) if f.endswith(".json")}
    leaves = {f[:-5] for f in os.listdir(os.path.join(PACK, "leaves_properties")) if f.endswith(".json")}

    if len(species) != 4:
        err(f"пород должно быть ровно 4 (решение пользователя), найдено {len(species)}: {sorted(species)}")

    # ---------------------------------------------------------------- families
    for name in sorted(families):
        path = os.path.join(PACK, "families", name + ".json")
        d = load(path)
        check_keys("family", path, d, schema)
        check_types("family", path, d, schema)
        if d.get("only_if_loaded") != "dynamictrees":
            err(f"families/{name}.json: без only_if_loaded=dynamictrees мод упадёт при отсутствии DT")
        log = d.get("primitive_log", "")
        if not log.startswith("riftworld:"):
            err(f"families/{name}.json: primitive_log должен быть нашим блоком, а не {log!r}")
        elif log.split(":")[1] not in blocks:
            err(f"families/{name}.json: блока {log} нет в реестре мода")
        for k, v in (d.get("texture_overrides") or {}).items():
            if k not in ("branch", "branch_top", "roots_side", "roots_top", "stripped_branch", "stripped_branch_top"):
                err(f"families/{name}.json: texture_overrides.{k} — неизвестный ключ текстуры")
            if not texture_exists(v):
                err(f"families/{name}.json: нет текстуры {v}")
        if d.get("common_species") not in [f"{PACK_ID}:{s}" for s in species]:
            err(f"families/{name}.json: common_species {d.get('common_species')} не объявлена в паке")
        if d.get("common_leaves") not in [f"{PACK_ID}:{s}" for s in leaves]:
            err(f"families/{name}.json: common_leaves {d.get('common_leaves')} нет в паке")
        # DT clamps a species' branch radius to the family's: a species asking for more would silently lose it
        fam_radius = d.get("max_branch_radius")
        if fam_radius is not None:
            for sp in species:
                spd = load(os.path.join(PACK, "species", sp + ".json"))
                if spd.get("family") == f"{PACK_ID}:{name}" and spd.get("max_branch_radius", 0) > fam_radius:
                    err(f"species/{sp}.json: max_branch_radius={spd['max_branch_radius']} больше, чем у семейства "
                        f"{name} ({fam_radius}) — DT обрежет его молча")
        # children of a family must be listed in its common species/leaves; check the reverse direction too
        kids = [sp for sp in species
                if load(os.path.join(PACK, "species", sp + ".json")).get("family") == f"{PACK_ID}:{name}"]
        if not kids:
            err(f"families/{name}.json: ни одна порода не ссылается на это семейство")

    # ---------------------------------------------------------------- leaves
    for name in sorted(leaves):
        path = os.path.join(PACK, "leaves_properties", name + ".json")
        d = load(path)
        check_keys("leaves_properties", path, d, schema)
        check_types("leaves_properties", path, d, schema)
        if d.get("only_if_loaded") != "dynamictrees":
            err(f"leaves_properties/{name}.json: нет only_if_loaded")
        kit = (d.get("cell_kit") or "").split(":")[-1]
        if kit and kit not in schema["cell_kits"]:
            err(f"leaves_properties/{name}.json: DT не знает cell_kit «{kit}» "
                f"(есть: {', '.join(schema['cell_kits'])})")
        pleaves = d.get("primitive_leaves", "")
        if pleaves.split(":")[-1] not in blocks:
            err(f"leaves_properties/{name}.json: блока листвы {pleaves} нет в реестре мода")

    # ---------------------------------------------------------------- species
    for name in sorted(species):
        path = os.path.join(PACK, "species", name + ".json")
        d = load(path)
        check_keys("species", path, d, schema)
        check_types("species", path, d, schema)
        if d.get("only_if_loaded") != "dynamictrees":
            err(f"species/{name}.json: нет only_if_loaded")
        if "family" not in d:
            err(f"species/{name}.json: нет family — DT не свяжет породу с семейством (ветки/семена не появятся)")
        if d.get("family") not in [f"{PACK_ID}:{f}" for f in families]:
            err(f"species/{name}.json: family {d.get('family')} не объявлена в паке")
        if d.get("leaves_properties") not in [f"{PACK_ID}:{f}" for f in leaves]:
            err(f"species/{name}.json: leaves_properties {d.get('leaves_properties')} нет в паке")
        logic = d.get("growth_logic_kit")
        if logic and logic.split(":")[-1] not in schema["growth_logic_kits"]:
            err(f"species/{name}.json: DT не знает growth_logic_kit «{logic}»")
        # shape parameters must be present and sane: this is exactly what the player asked us to tune
        for key, lo, hi in (("signal_energy", 3, 40), ("up_probability", 1, 8), ("tapering", 0.05, 0.9),
                            ("growth_rate", 0.1, 2.0), ("lowest_branch_height", 1, 12),
                            ("max_branch_radius", 3, 24)):
            if key not in d:
                err(f"species/{name}.json: нет параметра формы «{key}»")
            elif not isinstance(d[key], (int, float)) or isinstance(d[key], bool):
                err(f"species/{name}.json: {key} должно быть числом, а не {json_kind(d[key])}")
            elif not lo <= d[key] <= hi:
                err(f"species/{name}.json: {key}={d[key]} вне разумного диапазона {lo}..{hi}")
        # parameters outside the band DT itself uses are not illegal, but they are almost always a typo
        # (max_branch_radius is compared against the family instead: only a handful of DT's species override it, so
        #  their values say nothing about the legal range)
        for key, ref in (schema["species"].get("reference_ranges") or {}).items():
            if key in d and isinstance(d[key], (int, float)) and key not in REFERENCE_EXEMPT:
                lo, hi, n = ref
                if n >= 8 and not lo <= d[key] <= hi:
                    warn(f"species/{name}.json: {key}={d[key]} вне диапазона всех {n} видов DT ({lo}…{hi}); "
                         f"осознанные отклонения перечислены в комментарии tools/rift/gen_dt_addon.py")
        # a conifer-looking species with the wrong cell kit would grow round instead of conical
        kit = (load(os.path.join(PACK, "leaves_properties", name + ".json")).get("cell_kit") or "").split(":")[-1]
        if name == "mist_pine" and kit != "conifer":
            err("species/mist_pine.json: туманная сосна должна использовать cell_kit conifer")
        # biome selectors
        pb = (d.get("perfect_biomes") or {}).get("name")
        if pb:
            names = pb if isinstance(pb, list) else [pb]
            for b in names:
                if b not in biome_ids and not b.startswith("minecraft:"):
                    err(f"species/{name}.json: биома {b} нет в data/riftworld/worldgen/biome")
        # soil tags DT knows: an unknown one is silently ignored and the tree never grows on the mod's ground
        for key in ("acceptable_soils", "world_gen_acceptable_soils"):
            for tag in d.get(key, []) or []:
                if tag.split(":")[-1] not in schema["soil_tags"]:
                    err(f"species/{name}.json: {key}: DT не знает тип почвы «{tag}» "
                        f"(есть: {', '.join(schema['soil_tags'])})")
        # growth blocks
        for b in d.get("acceptable_growth_blocks", []):
            if b.startswith("riftworld:") and b.split(":")[1] not in blocks:
                err(f"species/{name}.json: acceptable_growth_blocks: блока {b} нет в реестре мода")
        # seeds: DT generates the item, we must ship its model, texture and translations
        if d.get("generate_seed"):
            tex = (d.get("texture_overrides") or {}).get("seed")
            if not tex:
                err(f"species/{name}.json: generate_seed=true, но нет texture_overrides.seed")
            else:
                if not texture_exists(tex):
                    err(f"species/{name}.json: нет спрайта семени {tex}")
                model = os.path.join(ASSETS, "models", "item", d.get("seed_name", name + "_seed") + ".json")
                if not os.path.exists(model):
                    err(f"species/{name}.json: нет модели семени {os.path.relpath(model, DT_RES)}")
            for lang in ("ru_ru", "en_us"):
                lp = os.path.join(ASSETS, "lang", lang + ".json")
                sid = d.get("seed_name", name + "_seed")
                if not os.path.exists(lp) or f"item.{PACK_ID}.{sid}" not in load(lp):
                    err(f"species/{name}.json: нет перевода семени ({lang})")
        if d.get("generate_sapling"):
            err(f"species/{name}.json: generate_sapling=true потребует модель саженца, которой у нас нет")

    # ---------------------------------------------------------------- soils
    soil_dir = os.path.join(PACK, "soil_properties")
    if not os.path.isdir(soil_dir) or not os.listdir(soil_dir):
        err("нет soil_properties: DT не сможет расти на земле мода")
    else:
        for f in sorted(os.listdir(soil_dir)):
            if not f.endswith(".json"):
                continue
            path = os.path.join(soil_dir, f)
            d = load(path)
            check_keys("soil_properties", path, d, schema)
            check_types("soil_properties", path, d, schema)
            if d.get("only_if_loaded") != "dynamictrees":
                err(f"soil_properties/{f}: нет only_if_loaded")
            if d.get("primitive_soil", "").split(":")[-1] not in blocks:
                err(f"soil_properties/{f}: блока {d.get('primitive_soil')} нет в реестре мода")

    # ---------------------------------------------------------------- world gen
    wg = load(os.path.join(PACK, "world_gen", "riftworld.json"))
    coverage = set()
    for entry in wg:
        sel = entry.get("select")
        if isinstance(sel, str) and sel.startswith("riftworld:"):
            coverage.add(sel)
            if sel not in biome_ids:
                err(f"world_gen: биома {sel} нет в моде")
        apply = entry.get("apply", {})
        sp = apply.get("species")
        if sp not in ("null", None) and sp not in [f"{PACK_ID}:{s}" for s in species]:
            err(f"world_gen: вид {sp} не объявлен в паке")
        if "density" not in apply:
            err(f"world_gen: у записи {sel} нет density")
        elif [float(x) for x in (apply["density"] if isinstance(apply["density"], list) else [apply["density"]])] != [1.0]:
            warn(f"world_gen: у {sel} density={apply['density']}; собственный пак DT советует для аддонов ровно [1.0]")
        if "chance" in apply and float(apply["chance"]) != 1.0:
            warn(f"world_gen: у {sel} chance={apply['chance']}; для аддонов советуют 1.0")
    for name, d in ((s, load(os.path.join(PACK, "species", s + ".json"))) for s in species):
        for b in (d.get("perfect_biomes") or {}).get("name", []) if isinstance(
                (d.get("perfect_biomes") or {}).get("name"), list) else [(d.get("perfect_biomes") or {}).get("name")]:
            if b and b.startswith("riftworld:") and b not in coverage:
                err(f"world_gen: биом {b} породы {name} не описан — деревья там не появятся")
    if "riftworld:ashen_lands" not in coverage:
        err("world_gen: для ashen_lands нужна запись species=null (там растут только свои обгоревшие деревья)")

    # ---------------------------------------------------------------- mod metadata
    toml = tomllib.load(open(os.path.join(DT_RES, "META-INF", "mods.toml"), "rb"))
    deps = {d["modId"]: d for d in (toml.get("dependencies", {}).get(PACK_ID) or [])}
    if "dynamictrees" not in deps:
        err("mods.toml: нет зависимости от dynamictrees")
    elif deps["dynamictrees"].get("type") != "optional":
        err("mods.toml: dynamictrees должна быть optional (мод обязан работать и без DT)")
    if toml["mods"][0]["modId"] != PACK_ID:
        err("mods.toml: modId не совпадает с именем пака")
    pmeta = load(os.path.join(DT_RES, "pack.mcmeta"))
    if pmeta["pack"]["pack_format"] != 15:
        err(f"pack.mcmeta: pack_format={pmeta['pack']['pack_format']}, для 1.20.1 нужен 15")

    # ---------------------------------------------------------------- report
    print(f"пак {PACK_ID}: {len(species)} пород, {len(families)} семейств, {len(leaves)} листв, "
          f"{len(os.listdir(soil_dir)) if os.path.isdir(soil_dir) else 0} типов почвы, {len(wg)} записей world_gen")
    if verbose:
        for name in sorted(species):
            d = load(os.path.join(PACK, "species", name + ".json"))
            print(f"  {name:10s} energy={d.get('signal_energy')} up={d.get('up_probability')} "
                  f"branch_from={d.get('lowest_branch_height')} taper={d.get('tapering')} "
                  f"radius={d.get('max_branch_radius')} kit={(load(os.path.join(PACK, 'leaves_properties', name + '.json')).get('cell_kit'))}")
    if warnings:
        print(f"\nпредупреждения ({len(warnings)}):")
        for w in warnings:
            print("  ~", w)
    if problems:
        print(f"\nПРОБЛЕМ: {len(problems)}")
        for p in problems:
            print("  -", p)
        return 1
    print("ПАК ВАЛИДЕН: все ключи известны DT, ссылки на блоки/биомы/текстуры на месте, soft-dependency настроена")
    return 0


# --------------------------------------------------------------------------- mutation self-test
def _pack_snapshot() -> dict[str, dict]:
    """All pack JSON files, path (relative to PACK) -> parsed object."""
    out = {}
    for dirpath, _, files in os.walk(PACK):
        for f in files:
            if f.endswith(".json"):
                full = os.path.join(dirpath, f)
                out[os.path.relpath(full, PACK)] = load(full)
    return out


def _write_pack(root: str, files: dict[str, dict]) -> None:
    for rel, data in files.items():
        full = os.path.join(root, rel)
        os.makedirs(os.path.dirname(full), exist_ok=True)
        with open(full, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)


def selftest() -> int:
    """Breaks a copy of the pack on purpose and requires the checker to notice every single break.

    A validator that has never failed is not evidence of anything: these mutations are exactly the mistakes this
    pack has already made once (unknown key, family key in species, wrong JSON type, bad reference).
    """
    import contextlib
    import copy
    import io
    import shutil
    import tempfile

    global PACK
    original = _pack_snapshot()
    real_pack = PACK
    tmp = tempfile.mkdtemp(prefix="rift-dt-selftest-")

    def mutate_species(name: str, **changes):
        return lambda files: files[f"species/{name}.json"].update(changes)

    def drop_species_key(name: str, key: str):
        return lambda files: files[f"species/{name}.json"].pop(key)

    def bad_ref(files):
        files["families/lilac.json"]["primitive_log"] = "riftworld:no_such_block"

    def family_radius(files):
        files["families/sizo_v2.json"]["max_branch_radius"] = 6

    def missing_seed_tex(files):
        files["species/lilac.json"]["texture_overrides"]["seed"] = f"{PACK_ID}:item/nope_seed"

    def bad_worldgen(files):
        files["world_gen/riftworld.json"][0]["apply"]["species"] = f"{PACK_ID}:ghost"

    def no_soil(files):
        for k in [k for k in files if k.startswith("soil_properties/")]:
            files.pop(k)

    def sapling(files):
        files["species/fern_tree.json"]["generate_sapling"] = True

    # (title, mutation, must the checker fail?)
    cases = [
        ("неизвестный ключ DT", mutate_species("lilac", definately_not_a_key=1), True),
        ("ключ family в species убран", drop_species_key("lilac", "family"), True),
        ("строка вместо числа", mutate_species("sizo_v2", tapering="0.26"), True),
        ("строка вместо массива", mutate_species("sizo_v2", acceptable_soils="dirt_like"), True),
        ("ссылка на несуществующий блок", bad_ref, True),
        ("радиус вида больше семейства", family_radius, True),
        ("нет спрайта семени", missing_seed_tex, True),
        ("world_gen ссылается на несуществующий вид", bad_worldgen, True),
        ("удалены soil_properties", no_soil, True),
        ("включён generate_sapling без модели", sapling, True),
        ("плотность 0.2 (только предупреждение)", lambda files: files["world_gen/riftworld.json"][0]["apply"].update(density=[0.2]), False),
    ]
    caught = 0
    for i, (title, mutation, must_fail) in enumerate(cases, 1):
        files = copy.deepcopy(original)
        mutation(files)
        pack_dir = os.path.join(tmp, f"case{i}", "trees", PACK_ID)
        shutil.rmtree(os.path.dirname(pack_dir), ignore_errors=True)
        _write_pack(pack_dir, files)
        PACK = pack_dir
        buf = io.StringIO()
        with contextlib.redirect_stdout(buf):
            code = main()
        PACK = real_pack
        ok = (code != 0) == must_fail
        caught += ok
        detail = "поймано" if code != 0 else "пропущено"
        print(f"  {'OK ' if ok else 'ПРОВАЛ'} {title}: {detail}")
    shutil.rmtree(tmp, ignore_errors=True)
    print(f"самопроверка валидатора: {caught}/{len(cases)} мутаций распознаны")
    return 0 if caught == len(cases) else 1


if __name__ == "__main__":
    ok = 0
    if "--no-selftest" not in sys.argv:
        print("== самопроверка валидатора (мутации пака)")
        ok = selftest()
        print()
    if "--selftest" in sys.argv:
        sys.exit(ok)
    code = main()
    sys.exit(code or ok)
