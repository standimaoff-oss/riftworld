#!/usr/bin/env python3
"""
v0.52.0 Static Validator — Vanilla-geometry armour + 3D Voxel Weapons/Tools/Bow/Shields,
Volumetric 3D Items, Dual Tool Sets, 4-Biome Rare Ore & Elisar Settlement Folk.

Runs in <1s locally and in GitHub Actions before Gradle compilation.

v0.52.0 changes over v0.51.0:
  * Mod armour is BACK, but with VANILLA GEOMETRY — no custom 3D armour models.
    The armour must be plain vanilla HumanoidArmorLayer (PLAYER_INNER/OUTER_ARMOR),
    so the mod ships only *_layer_1/_layer_2.png (64x32 player-skin layout).
  * Armour atlases must be 64x32 (vanilla armour UV layout), NOT 256x256 Box-UV.
  * Every armour material name must be "riftworld:<mat>" so Forge resolves the
    texture as <ns>:textures/models/armor/<mat>_layer_N.png automatically.
  * Item icons must be vanilla-shaped 16x16 sprites.
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

ARMOR_ATLAS = 64          # ванильная раскладка текстур брони
FOLK_ATLAS = 128

ARMOR_SETS = ("leather", "aurite", "riftcrystal")
ARMOR_PARTS = ("helmet", "chestplate", "leggings", "boots")


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
            ("west", u, v + d, d, h),
            ("north", u + d, v + d, w, h),
            ("east", u + d + w, v + d, d, h),
            ("south", u + d + w + d, v + d, w, h),
        ]
        for _fname, fu, fv, fw, fh in faces:
            assert fu + fw <= atlas and fv + fh <= atlas, f"{name}/{pname}: face overflows {atlas}px atlas"
            for yy in range(fv, fv + fh):
                for xx in range(fu, fu + fw):
                    occ[yy, xx] += 1
    assert occ.max() <= 1, f"{name}: overlapping UV faces in the atlas"
    used = int((occ > 0).sum())
    assert used >= 0.5 * atlas * atlas * 0.05, f"{name}: suspiciously little of the atlas is used ({used}px)"


def check_layer_size(java_src: str, anchor: str, atlas: int) -> None:
    """Checks the LayerDefinition.create(mesh, W, H) size of a specific model class."""
    pos = java_src.index(anchor)
    chunk = java_src[pos:pos + 9000]
    m = re.search(r'LayerDefinition\.create\(\s*mesh,\s*(\d+)\s*,\s*(\d+)\s*\)', chunk)
    assert m, f"{anchor}: no LayerDefinition.create() found"
    w, h = int(m.group(1)), int(m.group(2))
    assert (w, h) == (atlas, atlas), f"{anchor}: atlas must be {atlas}x{atlas}, got {w}x{h}"


def check_no_missing_magenta(path: Path) -> None:
    """Detects the 'missing texture' magenta placeholder."""
    arr = np.array(Image.open(path).convert("RGBA"))
    if arr.shape[2] < 4:
        return
    r, g, b, a = arr[:, :, 0], arr[:, :, 1], arr[:, :, 2], arr[:, :, 3]
    magenta = (r > 200) & (b > 200) & (g < 60) & (a > 200)
    assert not magenta.any(), f"{path.name}: contains magenta missing-texture pixels"


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
            # Element-model UVs are NORMALISED 0..16, NOT absolute texture pixels.
            # Minecraft scales them by (texture_width / 16), so absolute pixel
            # indices make every model render as black noise.
            assert 0.0 <= uv[0] <= uv[2] <= 16.0 and 0.0 <= uv[1] <= uv[3] <= 16.0, (
                f"{path.name}: uv out of the 0..16 grid {uv}")
            assert uv[2] > uv[0] and uv[3] > uv[1], f"{path.name}: degenerate uv {uv}"
    display = mjson.get("display", {})
    for key in ("thirdperson_righthand", "thirdperson_lefthand", "firstperson_righthand", "gui", "ground"):
        assert key in display, f"{path.name}: missing display transform {key}"
    return mjson


def check_vanilla_armor_layers(set_name: str) -> None:
    """The worn-armour atlases must be exactly the vanilla 64x32 player-skin layout.

    Vanilla armour layer maps have the head at (8..16, 0..8), the body at
    (16..40, 16..24) and the limbs below — we verify the silhouette matches
    the reference vanilla atlas so the geometry really is vanilla.
    """
    ref = ROOT / "tools/armor_editor/data/vanilla_armor"
    for idx in (1, 2):
        path = ASSETS / "textures/models/armor" / f"{set_name}_layer_{idx}.png"
        assert path.exists(), f"Missing armour layer {path}"
        arr = np.array(Image.open(path).convert("RGBA"))
        # numpy shape = (height, width); ванильная раскладка брони = 64x32 (ширина x высота)
        assert arr.shape[:2] == (32, ARMOR_ATLAS), (
            f"{path.name}: must be {ARMOR_ATLAS}x32 (vanilla armour layout), got {arr.shape[1]}x{arr.shape[0]}")
        alpha = arr[:, :, 3]
        assert (alpha > 0).any(), f"{path.name}: fully transparent"
        # силуэт обязан совпадать с ванильным (геометрия = ванильная!)
        base = set_name_to_vanilla(set_name)
        ref_path = ref / f"{base}_layer_{idx}.png"
        assert ref_path.exists(), f"Missing vanilla reference {ref_path}"
        ref_alpha = np.array(Image.open(ref_path).convert("RGBA"))[:, :, 3]
        assert np.array_equal(alpha > 0, ref_alpha > 0), (
            f"{path.name}: silhouette differs from the vanilla {base} atlas — "
            f"the armour geometry must be vanilla, not custom")
        check_no_missing_magenta(path)


def set_name_to_vanilla(set_name: str) -> str:
    return {"leather": "leather", "aurite": "netherite", "riftcrystal": "diamond"}[set_name]


def main() -> None:
    # 1. Verify no lingering references to deleted obsolete classes
    for jf in JAVA.rglob("*.java"):
        src = jf.read_text(encoding="utf-8")
        for banned in ("GlbSwordItem", "GlbPickaxeItem", "GlbAxeItem", "GlbShovelItem", "GlbHoeItem", "ElisarFolkRenderer<"):
            assert banned not in src, f"Obsolete symbol {banned} still referenced in {jf}"

    # ------------------------------------------------------------------
    # 2. v0.52.0: броня мода вернулась — с ВАНИЛЬНОЙ геометрией.
    #    Кастомных 3D-моделей брони быть НЕ должно.
    # ------------------------------------------------------------------
    for banned_class in ("ElisarArmorItem", "ElisarArmorModels", "RiftcrystalArmorGlowLayer"):
        assert not list(JAVA.rglob(f"{banned_class}.java")), (
            f"Custom-geometry armour class {banned_class} must not exist — armour is vanilla now")
        for jf in JAVA.rglob("*.java"):
            src = jf.read_text(encoding="utf-8")
            assert banned_class not in src, (
                f"Custom-geometry armour class {banned_class} still referenced in {jf}")

    # 2a. Материал брони реализует net.minecraft.world.item.ArmorMaterial (Forge 1.20.1 interface)
    mat_java = (JAVA / "item/RiftArmorMaterials.java")
    assert mat_java.exists(), "RiftArmorMaterials.java is missing"
    mat_src = mat_java.read_text(encoding="utf-8")
    assert "implements ArmorMaterial" in mat_src, "RiftArmorMaterials must implement ArmorMaterial"
    for method in ("getDurability", "getProtection", "getEnchantability",
                   "getEquipSound", "getRepairIngredient", "getName",
                   "getToughness", "getKnockbackResistance"):
        assert method in mat_src, f"ArmorMaterial.{method}() is not implemented"
    # 1.20.1 API: getDurability/getProtection принимают ArmorItem.Type
    assert "getDurability(ArmorItem.Type type)" in mat_src, (
        "getDurability() must take ArmorItem.Type (1.20.1 API)")
    assert "getProtection(ArmorItem.Type type)" in mat_src, (
        "getProtection() must take ArmorItem.Type (1.20.1 API)")
    # каждый материал обязан называться "riftworld:<mat>" — тогда Forge сам найдёт текстуру
    for set_name in ARMOR_SETS:
        assert f'"riftworld:{set_name}"' in mat_src, (
            f"Material {set_name} must be named riftworld:{set_name} so Forge resolves its texture")

    # 2b. Регистрация 12 предметов брони
    items_java = (JAVA / "registry/ModItems.java").read_text(encoding="utf-8")
    for set_name in ARMOR_SETS:
        for part in ARMOR_PARTS:
            reg = f'ITEMS.register("{set_name}_{part}"'
            assert reg in items_java, f"Armour item {set_name}_{part} is not registered in ModItems"
    # 1.20.1: ArmorItem(material, ArmorItem.Type, Properties)
    assert "new ArmorItem(" in items_java, "Armour items must be vanilla ArmorItem instances"
    for t in ("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS"):
        assert f"ArmorItem.Type.{t}" in items_java, (
            f"Armour item must be built with ArmorItem.Type.{t} (1.20.1 API)")
    assert "EquipmentSlot.HEAD, new Item.Properties()" not in items_java, (
        "Armour must not use EquipmentSlot in the ArmorItem constructor (1.20.1 takes ArmorItem.Type)")

    # 2c. Слои брони — ванильный формат 64x32 и ванильный силуэт
    for set_name in ARMOR_SETS:
        check_vanilla_armor_layers(set_name)

    # 2d. Иконки предметов — ванильные 16x16
    for set_name in ARMOR_SETS:
        for part in ARMOR_PARTS:
            ipath = ASSETS / "textures/item" / f"{set_name}_{part}.png"
            assert ipath.exists(), f"Missing armour icon {ipath}"
            iarr = np.array(Image.open(ipath).convert("RGBA"))
            assert iarr.shape[:2] == (16, 16), (
                f"{ipath.name}: must be a vanilla-shaped 16x16 sprite, got {iarr.shape[:2]}")
            check_no_missing_magenta(ipath)
            mpath = ASSETS / "models/item" / f"{set_name}_{part}.json"
            assert mpath.exists(), f"Missing armour item model {mpath}"
            mjson = json.loads(mpath.read_text(encoding="utf-8"))
            assert mjson.get("parent") == "minecraft:item/generated", (
                f"{mpath.name}: must use the vanilla item/generated parent")
            assert mjson.get("textures", {}).get("layer0") == f"riftworld:item/{set_name}_{part}", (
                f"{mpath.name}: wrong layer0 texture")

    # 2e. Старые «плоские» атласы брони (aurite_armor.png и т.п.) удалены
    for old in ("aurite_armor.png", "riftcrystal_armor.png", "riftcrystal_armor_glow.png"):
        assert not (ASSETS / "textures/models/armor" / old).exists(), (
            f"Old flat armour atlas {old} must be gone")

    # 2f. Рецепты брони — ванильные раскладки
    rec_dir = DATA / "riftworld/recipes"
    for set_name in ARMOR_SETS:
        for part in ARMOR_PARTS:
            rp = rec_dir / f"{set_name}_{part}.json"
            assert rp.exists(), f"Missing armour recipe {rp}"
            rjson = json.loads(rp.read_text(encoding="utf-8"))
            assert rjson.get("type") == "minecraft:crafting_shaped", f"{rp.name}: must be crafting_shaped"
            pat = rjson.get("pattern", [])
            assert pat and all(isinstance(row, str) for row in pat), (
                f"{rp.name}: pattern rows must be plain strings")
            assert rjson.get("result", {}).get("item") == f"riftworld:{set_name}_{part}", (
                f"{rp.name}: wrong result item")

    # 2g. Переводы
    for lang in ("en_us.json", "ru_ru.json"):
        lang_json = json.loads((ASSETS / "lang" / lang).read_text(encoding="utf-8"))
        for set_name in ARMOR_SETS:
            for part in ARMOR_PARTS:
                key = f"item.riftworld.{set_name}_{part}"
                assert key in lang_json and lang_json[key].strip(), f"Missing {key} in {lang}"

    # 2h. Вкладка креатива: броня должна быть доступна
    tabs_java = (JAVA / "registry/ModTabs.java").read_text(encoding="utf-8")
    for set_name in ARMOR_SETS:
        for part in ARMOR_PARTS:
            assert f"ModItems.{set_name.upper()}_{part.upper()}.get()" in tabs_java, (
                f"Armour {set_name}_{part} is missing from the creative tab")

    # Редактор брони: сайт с ванильной бронёй и экспортом в JSON.
    editor = ROOT / "tools" / "armor_editor"
    assert (editor / "index.html").exists(), "armour editor site is missing"
    assert (editor / "editor.js").exists(), "armour editor script is missing"
    for sset in ("leather", "netherite", "diamond"):
        for part in ("helmet", "chestplate", "leggings", "boots"):
            assert (editor / "data" / f"{sset}_{part}.png").exists(), (
                f"vanilla {sset} {part} reference icon is missing")
            assert (editor / "data" / f"model3d_{part}.json").exists(), (
                f"3D preview model for {part} is missing")

    # 3. The Elisar settlement folk atlases stay untouched at 128x128.
    folk_java = (JAVA / "client/model/ElisarFolkModels.java").read_text(encoding="utf-8")
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

    # 5. Атласы сущностей/блоков не должны содержать «отсутствующую текстуру».
    for check_png in [
        ASSETS / "textures/entity/elisar_villager.png",
        ASSETS / "textures/entity/elisar_guard.png",
        ASSETS / "textures/entity/elisar_archer.png",
        ASSETS / "textures/block/riftcrystal_ore.png",
    ]:
        check_no_missing_magenta(check_png)

    # 6. Check coin-ore smelting determinism + 2-way ducat <-> aurite_ingot + recipes
    assert not (rec_dir / "aurite_ingot_from_ore.json").exists(), "Duplicate aurite_ore smelting recipe must be removed"
    assert not (rec_dir / "aurite_ingot_from_ore_blasting.json").exists(), "Duplicate aurite_ore blasting recipe must be removed"
    for req_rec in (
        "aurite_ingot_from_ducats.json",
        "elisar_ducat_from_aurite_ingot.json",
        "elisar_bow.json",
        "elisar_shield.json",
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
        f"OK validate_v048_armory_and_folk: armour_vanilla=yes "
        f"armour_sets={len(ARMOR_SETS)} armour_parts={len(ARMOR_PARTS)} "
        f"editor_sets=3 editor_parts=4 voxel_models={len(voxel_items)} "
        f"rare_ore_biomes=4"
    )


if __name__ == "__main__":
    main()
