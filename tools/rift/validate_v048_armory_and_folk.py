#!/usr/bin/env python3
"""
v0.49.0 Static Validator — 2x HD Armors, 3D Voxel Weapons/Tools/Bow/Shields, Volumetric 3D
Armor Items, Dual Tool Sets, 4-Biome Rare Ore & Elisar Settlement Folk.

Runs in <1s locally and in GitHub Actions before Gradle compilation.

v0.49.0 changes over v0.48.0:
  * Worn armour atlases are now 256x256 (2x HD) with nested "hd" child bones.
  * All 8 armour items now have TRUE 3D volumetric item models (not flat item/generated sprites).
  * Weapons/tools/bow/shields use a 5-tier depth map (DEPTH_Z 0..4).
  * The El Gran Maja island devourer is always enabled and guarded to the Elisar dimension.
"""
from __future__ import annotations

import json
import re
from pathlib import Path
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
MOD = ROOT / "mod/src/main"
JAVA = MOD / "java/ru/aiforge/riftworld"
ASSETS = MOD / "resources/assets/riftworld"
DATA = MOD / "resources/data"

ARMOR_ATLAS = 256
FOLK_ATLAS = 128


def extract_method_boxes(java_src: str, anchor: str):
    """Extracts every (part, u, v, w, h, d) Box-UV cube of a layer method, including nested child bones."""
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


def check_box_uvs(name: str, boxes, png_path: Path, atlas: int) -> None:
    assert png_path.exists(), f"Missing atlas {png_path}"
    img = np.array(Image.open(png_path).convert("RGBA"))
    assert img.shape[:2] == (atlas, atlas), f"{png_path} must be {atlas}x{atlas}, got {img.shape[:2]}"
    occ = np.zeros((atlas, atlas), dtype=np.int32)
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
            assert 0 <= fx and fx + fw <= atlas and 0 <= fy and fy + fh <= atlas, (
                f"{name}:{pname}:{fname} out of {atlas}x{atlas} bounds: ({fx},{fy},{fw},{fh})"
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
    dist = np.sqrt(((rgb[:, 0] - 248.0) ** 2 + (rgb[:, 1] - 0.0) ** 2 + (rgb[:, 2] - 248.0) ** 2) / (3.0 * 255.0 ** 2))
    min_dist = float(dist.min())
    assert min_dist > 0.10, f"{png_path.name} has pixel too close to missing-texture #F800F8 (dist={min_dist:.4f})"


def check_layer_size(java_src: str, anchor: str, expected: int) -> None:
    """Asserts the LayerDefinition.create(mesh, W, H) declared by the layer method."""
    pos = java_src.index(anchor)
    window = java_src[pos:pos + 8000]
    m = re.search(r"LayerDefinition\.create\(mesh,\s*(\d+),\s*(\d+)\)", window)
    assert m, f"Cannot find LayerDefinition.create in {anchor}"
    assert int(m.group(1)) == expected and int(m.group(2)) == expected, (
        f"{anchor} must declare a {expected}x{expected} atlas, got {m.group(1)}x{m.group(2)}"
    )


def check_3d_item_model(path: Path, min_elements: int, max_elements: int) -> dict:
    mjson = json.loads(path.read_text(encoding="utf-8"))
    elems = mjson.get("elements", [])
    assert isinstance(elems, list) and len(elems) >= min_elements, (
        f"{path.name}: too few elements ({len(elems)} < {min_elements})")
    assert len(elems) <= max_elements, f"{path.name}: too many elements ({len(elems)} > {max_elements})"
    assert "textures" in mjson and mjson["textures"], f"{path.name}: missing textures"
    for el in elems:
        frm, to = el["from"], el["to"]
        for axis in range(3):
            assert 0.0 <= frm[axis] <= 16.0 and 0.0 <= to[axis] <= 16.0, (
                f"{path.name}: element out of [0..16]^3: {frm}..{to}")
            assert frm[axis] < to[axis], f"{path.name}: degenerate element {frm}..{to}"
        faces = el.get("faces", {})
        assert faces, f"{path.name}: element without faces"
        for face in faces.values():
            uv = face.get("uv")
            assert uv and len(uv) == 4, f"{path.name}: bad uv {uv}"
            # Element-model UVs are absolute texture pixels (the palette atlas is 64x64).
            assert 0.0 <= uv[0] <= uv[2] <= 64.0 and 0.0 <= uv[1] <= uv[3] <= 64.0, f"{path.name}: uv out of range {uv}"
            assert uv[2] > uv[0] and uv[3] > uv[1], f"{path.name}: degenerate uv {uv}"
    display = mjson.get("display", {})
    for key in ("thirdperson_righthand", "thirdperson_lefthand", "firstperson_righthand", "gui", "ground"):
        assert key in display, f"{path.name}: missing display transform {key}"
    return mjson


def main() -> None:
    # 1. Verify no lingering references to deleted obsolete classes
    for jf in JAVA.rglob("*.java"):
        src = jf.read_text(encoding="utf-8")
        for banned in ("GlbSwordItem", "GlbPickaxeItem", "GlbAxeItem", "GlbShovelItem", "GlbHoeItem", "ElisarFolkRenderer<"):
            assert banned not in src, f"Obsolete symbol {banned} still referenced in {jf}"

    # 2. Check 256x256 Box-UV armour atlases & 0 overlaps
    armor_java = (JAVA / "client/model/ElisarArmorModels.java").read_text(encoding="utf-8")
    folk_java = (JAVA / "client/model/ElisarFolkModels.java").read_text(encoding="utf-8")

    aur_boxes = extract_method_boxes(armor_java, "createAuriteInnerLayer()") + extract_method_boxes(armor_java, "createAuriteOuterLayer()")
    rc_boxes = extract_method_boxes(armor_java, "createRiftcrystalInnerLayer()") + extract_method_boxes(armor_java, "createRiftcrystalOuterLayer()")
    check_box_uvs("aurite_armor", aur_boxes, ASSETS / "textures/models/armor/aurite_armor.png", ARMOR_ATLAS)
    check_box_uvs("riftcrystal_armor", rc_boxes, ASSETS / "textures/models/armor/riftcrystal_armor.png", ARMOR_ATLAS)
    for anchor in ("createAuriteInnerLayer()", "createAuriteOuterLayer()",
                   "createRiftcrystalInnerLayer()", "createRiftcrystalOuterLayer()"):
        check_layer_size(armor_java, anchor, ARMOR_ATLAS)
    # The 2x HD generator emits an "hd" child bone on every armoured part.
    assert '"hd"' in armor_java, "ElisarArmorModels.java must contain the 2x HD 'hd' child bone"
    assert "pauldron_r" in armor_java and "pauldron_l" in armor_java, "Tilted pauldrons must exist"
    assert "rc_horn_r" in armor_java and "rc_horn_l" in armor_java, "Riftcrystal horns must exist"

    glow_path = ASSETS / "textures/models/armor/riftcrystal_armor_glow.png"
    assert glow_path.exists()
    glow_arr = np.array(Image.open(glow_path).convert("RGBA"))
    assert glow_arr.shape[:2] == (ARMOR_ATLAS, ARMOR_ATLAS) and int(np.count_nonzero(glow_arr[:, :, 3])) >= 40
    # Eyes-blend glow must stay dim (additive blending) and must never blow out to white.
    gop = glow_arr[glow_arr[:, :, 3] > 0, :3].astype(np.float32)
    if gop.size:
        lum = gop.max(axis=1) / 255.0
        assert float(lum.min()) >= 0.20 and float(lum.max()) <= 0.55, (
            f"riftcrystal_armor_glow.png brightness out of the eyes-blend range: {lum.min():.3f}..{lum.max():.3f}")

    # 3. The Elisar settlement folk atlases stay untouched at 128x128.
    for anchor, png in (
        ("class Villager", "elisar_villager.png"),
        ("class Guard", "elisar_guard.png"),
        ("class Archer", "elisar_archer.png"),
    ):
        check_layer_size(folk_java, anchor, FOLK_ATLAS)
        check_box_uvs(png.replace(".png", ""), extract_method_boxes(folk_java, anchor), ASSETS / "textures/entity" / png, FOLK_ATLAS)

    # 4. Check all 3D voxel weapon/tool/bow/shield JSON models & mipmap-proof 64x64 palettes
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
        check_3d_item_model(mp, 20, 250)
        sarr = np.array(Image.open(sp).convert("RGBA"))
        assert sarr.shape[:2] == (32, 32), f"{vid}.png must be a crisp 32x32 sprite, got {sarr.shape[:2]}"
        check_no_missing_magenta(sp)

    for blk_model in ("elisar_shield_blocking.json", "elisar_guard_shield_blocking.json"):
        bp = ASSETS / "models/item" / blk_model
        assert bp.exists(), f"Missing {blk_model}"
        bjson = json.loads(bp.read_text(encoding="utf-8"))
        assert "parent" in bjson, f"{blk_model} must inherit from its parent model"

    # 5. The 8 armour items must now be TRUE 3D volumetric models, not flat sprites.
    armor_items = [
        "aurite_helmet", "aurite_chestplate", "aurite_leggings", "aurite_boots",
        "riftcrystal_helmet", "riftcrystal_chestplate", "riftcrystal_leggings", "riftcrystal_boots",
    ]
    for aid in armor_items:
        mp = ASSETS / "models/item" / f"{aid}.json"
        sp = ASSETS / "textures/item" / f"{aid}.png"
        assert mp.exists() and sp.exists(), f"Missing 3D armour model or sprite for {aid}"
        mjson = json.loads(mp.read_text(encoding="utf-8"))
        assert mjson.get("parent") is None and "elements" in mjson, (
            f"{aid}.json must be a standalone 3D element model")
        assert "minecraft:item/generated" not in json.dumps(mjson), (
            f"{aid}.json must not be a flat item/generated sprite model")
        check_3d_item_model(mp, 20, 200)
        sarr = np.array(Image.open(sp).convert("RGBA"))
        assert sarr.shape[:2] == (32, 32), f"{aid}.png must be a crisp 32x32 sprite, got {sarr.shape[:2]}"
        check_no_missing_magenta(sp)

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

    # 6. Check coin-ore smelting determinism + 2-way ducat <-> aurite_ingot + recipes
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

    # 7. Check the strict 4-biome riftcrystal_ore rule & settlement population hook
    cg_src = (JAVA / "world/ElisarChunkGenerator.java").read_text(encoding="utf-8")
    rc_assigns = re.findall(r"=\s*riftcrystalOre\s*;", cg_src)
    assert len(rc_assigns) == 4, f"Expected strictly 4 biome placements of riftcrystalOre, found {len(rc_assigns)}"
    assert "if (morok && s.is(stone.getBlock())" in cg_src, "3rd riftcrystalOre placement must be gated strictly on morok"
    assert "ElisarSettlementKeeper.populateChunkRegion(region)" in cg_src

    # 8. v0.49.0 Maja island-devouring rules
    maja_cfg = (JAVA / "MajaConfig.java").read_text(encoding="utf-8")
    assert 'define("allowIslandEating", true)' in maja_cfg, "allowIslandEating must default to true in v0.49.0"
    policy = (JAVA / "world/MajaSafetyPolicy.java").read_text(encoding="utf-8")
    assert "placedBlocks == 0" in policy, "The feeding gate must abort on ANY player placement"
    devourer = (JAVA / "world/MajaIslandDevourer.java").read_text(encoding="utf-8")
    assert "ELISAR_DIM" in devourer, "The island devourer must be guarded to the Elisar dimension"
    assert "riftworlddt" in devourer, "Dynamic Trees foliage on wild islands must be treated as natural"
    assert "S_BARE" in devourer, "Bare stone rims of bowl islets must be edible"
    assert "MAX_WHOLE_ISLAND_RADIUS" in devourer, "The whole-island radius limit must exist"

    # 9. Check en_us.json & ru_ru.json translations
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
        f"riftcrystal_boxes={len(rc_boxes)} voxel_models={len(voxel_items)} "
        f"armor_3d_items={len(armor_items)} atlas={ARMOR_ATLAS}x{ARMOR_ATLAS} rare_ore_biomes=4"
    )


if __name__ == "__main__":
    main()
