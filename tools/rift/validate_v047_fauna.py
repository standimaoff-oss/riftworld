#!/usr/bin/env python3
"""Static integrity validator for Riftworld v0.47.0 15-creature fauna expansion."""
from __future__ import annotations

import json
import re
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
MOD = ROOT / "mod"
JAVA = MOD / "src/main/java/ru/aiforge/riftworld"
ASSETS = MOD / "src/main/resources/assets/riftworld"

FAUNA = [
    ("porcelain_crane", "PorcelainCrane"),
    ("basalt_beetle", "BasaltBeetle"),
    ("ash_moth", "AshMoth"),
    ("amethyst_fox", "AmethystFox"),
    ("sizo_leafwing", "SizoLeafwing"),
    ("azure_frog", "AzureFrog"),
    ("blossom_snail", "BlossomSnail"),
    ("phantom_wolf", "PhantomWolf"),
    ("crystal_gecko", "CrystalGecko"),
    ("faceted_owl", "FacetedOwl"),
    ("bone_raven", "BoneRaven"),
    ("coral_hermit_crab", "CoralHermitCrab"),
    ("reef_seahorse", "ReefSeahorse"),
    ("ghost_squid", "GhostSquid"),
    ("silver_sea_serpent", "SilverSeaSerpent"),
]

ITEMS = [
    "reef_seahorse_bucket",
    "porcelain_feather",
    "basalt_scute",
    "ash_wing_dust",
    "phantom_fang",
    "bone_pinion",
    "abyssal_ink_sac",
]

GLOW_IDS = [
    "basalt_beetle",
    "ash_moth",
    "bone_raven",
    "blossom_snail",
    "phantom_wolf",
    "faceted_owl",
    "ghost_squid",
    "silver_sea_serpent",
]

props = (MOD / "gradle.properties").read_text(encoding="utf-8")
assert "mod_version=0.47.0" in props, "mod_version must be 0.47.0"

en = json.loads((ASSETS / "lang/en_us.json").read_text(encoding="utf-8"))
ru = json.loads((ASSETS / "lang/ru_ru.json").read_text(encoding="utf-8"))


def check_java_file(path: Path, expected_pkg: str) -> str:
    assert path.exists(), f"Missing Java file: {path}"
    txt = path.read_text(encoding="utf-8")
    assert f"package {expected_pkg};" in txt, f"Wrong package in {path}"
    # Strip comments and string literals before bracket balance check
    clean = re.sub(r"//.*", "", txt)
    clean = re.sub(r"/\*[\s\S]*?\*/", "", clean)
    clean = re.sub(r'"(?:\\.|[^"\\])*"', '""', clean)
    for op, cl in [("{", "}"), ("(", ")"), ("[", "]")]:
        assert clean.count(op) == clean.count(cl), f"Unbalanced {op}{cl} in {path}"
    cls_name = path.stem
    assert re.search(rf"\bclass\s+{cls_name}\b", clean), f"Class {cls_name} not declared in {path}"
    return txt


check_java_file(JAVA / "entity/fauna/ElisarFaunaUtil.java", "ru.aiforge.riftworld.entity.fauna")
check_java_file(JAVA / "client/model/fauna/FaunaBaseModel.java", "ru.aiforge.riftworld.client.model.fauna")
renderers_txt = check_java_file(JAVA / "client/ElisarFaunaRenderers.java", "ru.aiforge.riftworld.client")
keeper_txt = check_java_file(JAVA / "world/ElisarBiomeFaunaKeeper.java", "ru.aiforge.riftworld.world")
entities_reg = check_java_file(JAVA / "registry/ModEntities.java", "ru.aiforge.riftworld.registry")
items_reg = check_java_file(JAVA / "registry/ModItems.java", "ru.aiforge.riftworld.registry")
tabs_reg = check_java_file(JAVA / "registry/ModTabs.java", "ru.aiforge.riftworld.registry")
hooks_txt = check_java_file(JAVA / "client/ClientHooks.java", "ru.aiforge.riftworld.client")

assert "ElisarFaunaRenderers.registerLayers(e);" in hooks_txt
assert "ElisarFaunaRenderers.registerRenderers(e);" in hooks_txt

for fid, pascal in FAUNA:
    ent_txt = check_java_file(JAVA / f"entity/fauna/{pascal}Entity.java", "ru.aiforge.riftworld.entity.fauna")
    mod_txt = check_java_file(JAVA / f"client/model/fauna/{pascal}Model.java", "ru.aiforge.riftworld.client.model.fauna")

    # Verify every entity getter called in setupAnim exists on the entity class (or Mob/LivingEntity/TamableAnimal)
    for m in re.finditer(r"entity\.(\w+)\s*\(", mod_txt):
        method = m.group(1)
        if method in {"getId", "getDeltaMovement", "isInSittingPose"}:
            continue
        assert f" {method}(" in ent_txt, f"{pascal}Model calls entity.{method}() missing from {pascal}Entity"

    const_name = fid.upper()
    assert f" {const_name} =" in entities_reg, f"ModEntities missing {const_name}"
    assert f"e.put({const_name}.get()," in entities_reg, f"ModEntities.attributes missing {const_name}"
    assert f" {const_name}_EGG =" in items_reg, f"ModItems missing {const_name}_EGG"
    assert f"ModItems.{const_name}_EGG.get()" in tabs_reg, f"ModTabs missing {const_name}_EGG"
    assert f"ModEntities.{const_name}.get()" in renderers_txt, f"Renderers missing {const_name}"
    assert f"ModEntities.{const_name}.get()" in keeper_txt, f"Keeper missing {const_name}"

    tex = Image.open(ASSETS / f"textures/entity/{fid}.png")
    assert tex.size == (512, 512), f"{fid}.png size {tex.size}"
    egg_model = json.loads((ASSETS / f"models/item/{fid}_spawn_egg.json").read_text(encoding="utf-8"))
    assert egg_model["parent"] == "minecraft:item/template_spawn_egg"
    geo = json.loads((ASSETS / f"geo/{fid}.json").read_text(encoding="utf-8"))
    assert len(geo["bones"]) >= 7

    for lang_name, lang in [("en_us", en), ("ru_ru", ru)]:
        assert f"entity.riftworld.{fid}" in lang, f"{lang_name} missing entity {fid}"
        assert f"item.riftworld.{fid}_spawn_egg" in lang, f"{lang_name} missing egg {fid}"

for gid in GLOW_IDS:
    gtex = Image.open(ASSETS / f"textures/entity/{gid}_glow.png")
    assert gtex.size == (512, 512), f"{gid}_glow.png size {gtex.size}"

for item_id in ITEMS:
    const_name = item_id.upper()
    assert f" {const_name} =" in items_reg, f"ModItems missing {const_name}"
    assert f"ModItems.{const_name}.get()" in tabs_reg, f"ModTabs missing {const_name}"
    im = json.loads((ASSETS / f"models/item/{item_id}.json").read_text(encoding="utf-8"))
    assert im["textures"]["layer0"] == f"riftworld:item/{item_id}"
    itex = Image.open(ASSETS / f"textures/item/{item_id}.png")
    assert itex.size == (16, 16), f"{item_id}.png size {itex.size}"
    for lang_name, lang in [("en_us", en), ("ru_ru", ru)]:
        assert f"item.riftworld.{item_id}" in lang, f"{lang_name} missing item {item_id}"

print("OK: v0.47.0 static validation passed across all 15 entities, 15 models, renderers, keepers, items, textures, and localizations!")
