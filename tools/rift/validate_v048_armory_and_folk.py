#!/usr/bin/env python3
"""
v0.48.0 Static Validator — Armory, Dual Tool Sets, 4-Biome Rare Ore & Elisar Settlement Folk.
Runs in <1s locally and in GitHub Actions before Gradle compilation.
"""
from __future__ import annotations

import json
import math
import re
from pathlib import Path
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
MOD = ROOT / "mod/src/main"
JAVA = MOD / "java/ru/aiforge/riftworld"
ASSETS = MOD / "resources/assets/riftworld"
DATA = MOD / "resources/data"


def extract_method_boxes(java_src: str, anchor: str):
    pos = java_src.index(anchor)
    start = java_src.index("MeshDefinition mesh", pos)
    end = java_src.index("return LayerDefinition.create", start)
    sub = java_src[start:end]
    boxes = []
    for m in re.finditer(r'addOrReplaceChild\(\s*"([^"]+)"\s*,\s*CubeListBuilder\.create\(\)(.*?),\s*PartPose\.(?:offset|offsetAndRotation)', sub, re.S):
        pname, body_txt = m.groups()
        u = v = 0
        for tok in re.finditer(r'\.texOffs\((\d+),\s*(\d+)\)|\.addBox\(([^)]+)\)', body_txt):
            if tok.group(1) is not None:
                u, v = int(tok.group(1)), int(tok.group(2))
            else:
                vals = [float(x) for x in re.findall(r'-?\d+(?:\.\d+)?', tok.group(3))]
                w, h, d = int(vals[3]), int(vals[4]), int(vals[5])
                boxes.append((pname, u, v, w, h, d))
    return boxes


def check_box_uvs(name: str, boxes, png_path: Path) -> None:
    assert png_path.exists(), f"Missing atlas {png_path}"
    img = np.array(Image.open(png_path).convert("RGBA"))
    assert img.shape[:2] == (128, 128), f"{png_path} must be 128x128, got {img.shape[:2]}"
    occ = np.zeros((128, 128), dtype=np.int32)
    assert len(boxes) >= 5, f"Too few boxes in {name}: {len(boxes)}"
    for pname, u, v, w, h, d in boxes:
        faces = [
            ("up", u + d, v, w, d),
            ("down", u + d + w, v, w, d),
            ("right", u, v + d, d, h),
            ("front", u + d, v + d, w, h),
            ("left", u + d + w, v + d, d, h),
            ("back", u + 2 * d + w, v + d, w, h),
        ]
        for fname, fx, fy, fw, fh in faces:
            assert 0 <= fx and fx + fw <= 128 and 0 <= fy and fy + fh <= 128, (
                f"{name}:{pname}:{fname} out of 128x128 bounds: ({fx},{fy},{fw},{fh})"
            )
            if fw > 0 and fh > 0:
                overlap = int(np.count_nonzero(occ[fy:fy + fh, fx:fx + fw]))
                assert overlap == 0, f"UV overlap in {name}:{pname}:{fname} at ({fx},{fy},{fw},{fh})"
                occ[fy:fy + fh, fx:fx + fw] = 1
                alpha_min = int(img[fy:fy + fh, fx:fx + fw, 3].min())
                assert alpha_min == 255, f"Transparent pixel inside {name}:{pname}:{fname}: min_alpha={alpha_min}"


def check_no_missing_magenta(png_path: Path) -> None:
    arr = np.array(Image.open(png_path).convert("RGBA"))
    opaque = arr[:, :, 3] > 0
    if not np.any(opaque):
        return
    rgb = arr[opaque, :3].astype(np.float32)
    # Distance to #F800F8 (248, 0, 248) normalized to [0..1]
    dist = np.sqrt(((rgb[:, 0] - 248.0) ** 2 + (rgb[:, 1] - 0.0) ** 2 + (rgb[:, 2] - 248.0) ** 2) / (3.0 * 255.0 ** 2))
    min_dist = float(dist.min())
    assert min_dist > 0.10, f"{png_path.name} has pixel too close to missing-texture #F800F8 (dist={min_dist:.4f})"


def main() -> None:
    # 1. Verify no lingering references to deleted obsolete classes
    for jf in JAVA.rglob("*.java"):
        src = jf.read_text(encoding="utf-8")
        for banned in ("GlbSwordItem", "GlbPickaxeItem", "GlbAxeItem", "GlbShovelItem", "GlbHoeItem", "ElisarFolkRenderer<"):
            assert banned not in src, f"Obsolete symbol {banned} still referenced in {jf}"

    # 2. Check 128x128 Box-UV atlases & 0 overlaps
    armor_java = (JAVA / "client/model/ElisarArmorModels.java").read_text(encoding="utf-8")
    folk_java = (JAVA / "client/model/ElisarFolkModels.java").read_text(encoding="utf-8")

    aur_boxes = extract_method_boxes(armor_java, "createAuriteInnerLayer()") + extract_method_boxes(armor_java, "createAuriteOuterLayer()")
    rc_boxes = extract_method_boxes(armor_java, "createRiftcrystalInnerLayer()") + extract_method_boxes(armor_java, "createRiftcrystalOuterLayer()")
    check_box_uvs("aurite_armor", aur_boxes, ASSETS / "textures/models/armor/aurite_armor.png")
    check_box_uvs("riftcrystal_armor", rc_boxes, ASSETS / "textures/models/armor/riftcrystal_armor.png")

    glow_path = ASSETS / "textures/models/armor/riftcrystal_armor_glow.png"
    assert glow_path.exists()
    glow_arr = np.array(Image.open(glow_path).convert("RGBA"))
    assert glow_arr.shape[:2] == (128, 128) and int(np.count_nonzero(glow_arr[:, :, 3])) >= 40

    check_box_uvs("elisar_villager", extract_method_boxes(folk_java, "class Villager"), ASSETS / "textures/entity/elisar_villager.png")
    check_box_uvs("elisar_guard", extract_method_boxes(folk_java, "class Guard"), ASSETS / "textures/entity/elisar_guard.png")
    check_box_uvs("elisar_archer", extract_method_boxes(folk_java, "class Archer"), ASSETS / "textures/entity/elisar_archer.png")

    # 3. Check all 3D voxel weapon/tool/bow/shield JSON models & mipmap-proof 64x64 palettes
    for pal_fn in ("v048_aurite_pal.png", "v048_riftcrystal_pal.png", "v048_guard_pal.png"):
        pp = ASSETS / "textures/item" / pal_fn
        assert pp.exists(), f"Missing palette {pp}"
        parr = np.array(Image.open(pp).convert("RGBA"))
        assert parr.shape == (64, 64, 4) and int(parr[:, :, 3].min()) == 255, f"{pal_fn} must be 100% opaque 64x64"
        check_no_missing_magenta(pp)

    voxel_items = [
        "aurite_pickaxe", "aurite_axe", "aurite_sword", "aurite_shovel", "aurite_hoe",
        "riftcrystal_pickaxe", "riftcrystal_axe", "riftcrystal_sword", "riftcrystal_shovel", "riftcrystal_hoe",
        "elisar_bow", "elisar_bow_pulling_0", "elisar_bow_pulling_1", "elisar_bow_pulling_2",
        "elisar_shield", "elisar_guard_shield", "elisar_guard_sword",
    ]
    for vid in voxel_items:
        mp = ASSETS / "models/item" / f"{vid}.json"
        sp = ASSETS / "textures/item" / f"{vid}.png"
        assert mp.exists() and sp.exists(), f"Missing model or sprite for {vid}"
        mjson = json.loads(mp.read_text(encoding="utf-8"))
        elems = mjson.get("elements", [])
        assert 20 <= len(elems) <= 250, f"Unexpected element count for {vid}: {len(elems)}"
        assert "thirdperson_righthand" in mjson.get("display", {})
        assert "thirdperson_lefthand" in mjson.get("display", {})
        check_no_missing_magenta(sp)

    for blk_model in ("elisar_shield_blocking.json", "elisar_guard_shield_blocking.json"):
        assert (ASSETS / "models/item" / blk_model).exists()

    for check_png in [
        ASSETS / "textures/models/armor/aurite_armor.png",
        ASSETS / "textures/models/armor/riftcrystal_armor.png",
        ASSETS / "textures/models/armor/riftcrystal_armor_glow.png",
        ASSETS / "textures/entity/elisar_villager.png",
        ASSETS / "textures/entity/elisar_guard.png",
        ASSETS / "textures/entity/elisar_archer.png",
        ASSETS / "textures/block/riftcrystal_ore.png",
    ]:
        check_no_missing_magenta(check_png)

    # 4. Check coin-ore smelting determinism + 2-way ducat <-> aurite_ingot + recipes
    rec_dir = DATA / "riftworld/recipes"
    assert not (rec_dir / "aurite_ingot_from_ore.json").exists(), "Duplicate aurite_ore smelting recipe must be removed"
    assert not (rec_dir / "aurite_ingot_from_ore_blasting.json").exists(), "Duplicate aurite_ore blasting recipe must be removed"
    for req_rec in (
        "aurite_ingot_from_ducats.json",
        "elisar_ducat_from_aurite_ingot.json",
        "elisar_bow.json",
        "elisar_shield.json",
        "aurite_helmet.json", "aurite_chestplate.json", "aurite_leggings.json", "aurite_boots.json",
        "riftcrystal_helmet.json", "riftcrystal_chestplate.json", "riftcrystal_leggings.json", "riftcrystal_boots.json",
        "aurite_pickaxe.json", "aurite_axe.json", "aurite_sword.json", "aurite_shovel.json", "aurite_hoe.json",
        "riftcrystal_pickaxe.json", "riftcrystal_axe.json", "riftcrystal_sword.json", "riftcrystal_shovel.json", "riftcrystal_hoe.json",
    ):
        rp = rec_dir / req_rec
        assert rp.exists(), f"Missing recipe {req_rec}"
        json.loads(rp.read_text(encoding="utf-8"))

    # 5. Check strict 4-biome riftcrystal_ore rule & settlement population hook in ElisarChunkGenerator.java
    cg_src = (JAVA / "world/ElisarChunkGenerator.java").read_text(encoding="utf-8")
    rc_assigns = re.findall(r"=\s*riftcrystalOre\s*;", cg_src)
    assert len(rc_assigns) == 4, f"Expected strictly 4 biome placements of riftcrystalOre, found {len(rc_assigns)}"
    assert "if (morok && s.is(stone.getBlock())" in cg_src, "3rd riftcrystalOre placement must be gated strictly on morok"
    assert "ElisarSettlementKeeper.populateChunkRegion(region)" in cg_src

    # 6. Check en_us.json & ru_ru.json translations
    for lang_fn in ("en_us.json", "ru_ru.json"):
        ldata = json.loads((ASSETS / "lang" / lang_fn).read_text(encoding="utf-8"))
        for key in (
            "item.riftworld.elisar_bow",
            "item.riftworld.elisar_shield",
            "item.riftworld.elisar_guard_sword",
            "item.riftworld.elisar_guard_shield",
            "entity.riftworld.elisar_villager",
            "entity.riftworld.elisar_guard",
            "entity.riftworld.elisar_archer",
        ):
            assert key in ldata and ldata[key].strip(), f"Missing {key} in {lang_fn}"

    print(
        f"OK validate_v048_armory_and_folk: aurite_boxes={len(aur_boxes)} "
        f"riftcrystal_boxes={len(rc_boxes)} voxel_models={len(voxel_items)} rare_ore_biomes=4"
    )


if __name__ == "__main__":
    main()
