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
    # ВНИМАНИЕ: имена методов — Мojang-овские для 1.20.1 (Forge 1.20.1
    # использует официальные маппинги Mojang, а не yarn).
    #   getDurabilityForType / getDefenseForType / getEnchantmentValue
    # Проверка нужна, чтобы в CI ловить несовпадение сигнатур ДО javac.
    for method in ("getDurabilityForType", "getDefenseForType", "getEnchantmentValue",
                   "getEquipSound", "getRepairIngredient", "getName",
                   "getToughness", "getKnockbackResistance"):
        assert method in mat_src, f"ArmorMaterial.{method}() is not implemented"
    # 1.20.1 API: getDurabilityForType/getDefenseForType принимают ArmorItem.Type
    assert "getDurabilityForType(ArmorItem.Type type)" in mat_src, (
        "getDurabilityForType() must take ArmorItem.Type (1.20.1 Mojmap API)")
    assert "getDefenseForType(ArmorItem.Type type)" in mat_src, (
        "getDefenseForType() must take ArmorItem.Type (1.20.1 Mojmap API)")
    # yarn-имена в 1.20.1 не существуют — их не должно остаться
    for wrong in ("getDurability(", "getProtection(", "getEnchantability("):
        assert wrong not in mat_src, (
            f"ArmorMaterial: {wrong} is not a 1.20.1 Mojmap method")
    # каждый материал обязан называться "riftworld:<mat>" — тогда Forge сам найдёт текстуру
    for set_name in ARMOR_SETS:
        assert f'"riftworld:{set_name}"' in mat_src, (
            f"Material {set_name} must be named riftworld:{set_name} so Forge resolves its texture")

    # 2a-2. Ровно как в ванильном HumanoidArmorLayer#getArmorLocation (1.20.1):
    #     "textures/models/armor/" + material.getName() + "_layer_" + (1|2) + ".png"
    # затем ResourceLocation.decompose(str, ':') режет по ПЕРВОМУ двоеточию:
    #     "textures/models/armor/riftworld:aurite_layer_1.png"
    #        -> namespace "riftworld", path "textures/models/armor/aurite_layer_1.png"
    # Значит getName() обязан быть "riftworld:<mat>" (с двоеточием!), а текстуры лежать
    # в assets/riftworld/textures/models/armor/<mat>_layer_{1,2}.png
    for set_name in ARMOR_SETS:
        for layer in (1, 2):
            tex = (ASSETS / "textures/models/armor"
                   / f"{set_name}_layer_{layer}.png")
            assert tex.exists(), (
                f"armour texture missing: {tex} (vanilla path from getName())")
            with Image.open(tex) as im:
                assert im.size == (64, 32), (
                    f"{tex.name} must be 64x32 (vanilla player-skin armour layout), "
                    f"got {im.size}")

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

    # Ванильные раскладки брони 1.20.1 (data/minecraft/recipes, 1.20.1):
    # одинаковы для кожи и металлов, отличается только материал.
    VANILLA_ARMOR_LAYOUT = {
        "helmet": ["XXX", "X X"],
        "chestplate": ["X X", "XXX", "XXX"],
        "leggings": ["XXX", "X X", "X X"],
        "boots": ["X X", "X X"],
    }

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
            # Раскладка должна совпадать с ванильной
            assert pat == VANILLA_ARMOR_LAYOUT[part], (
                f"{rp.name}: pattern {pat} must equal vanilla 1.20.1 "
                f"{VANILLA_ARMOR_LAYOUT[part]}")
            # Каждый символ раскладки обязан быть объявлен в key, иначе
            # Minecraft падает с "Pattern references symbol 'L' but it's not
            # defined in the key" и рецепт не грузится вообще.
            keys = rjson.get("key") or {}
            assert isinstance(keys, dict) and keys, f"{rp.name}: key must be a non-empty object"
            used = set("".join(pat)) - {" "}
            missing = used - set(keys)
            assert not missing, (
                f"{rp.name}: pattern uses {sorted(missing)} but key defines {sorted(keys)}")
            # Ровно один ингредиент на комплект
            assert len(keys) == 1, f"{rp.name}: expected exactly one ingredient, got {sorted(keys)}"

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

    # 4. Инструменты, оружие, лук и щиты — по ВАНИЛЬНОМУ принципу (v0.53.0).
    #
    #   * никаких самодельных 3D-моделей: parent = minecraft:item/handheld
    #     (мечи/топоры/кирки/лопаты/мотыги/лук) или minecraft:item/generated
    #     (щиты, меч стражи) — ровно как у ванильных предметов;
    #   * никаких выдуманных палитр "pal": плоская текстура layer0;
    #   * иконка — ванильного размера 16x16.
    #
    # Старые «воксельные» модели (32x32 elements + палитра 64x64) удалены:
    # именно из-за них инструменты выглядели «не ванильно».
    assert not (ASSETS / "textures/item/v048_aurite_pal.png").exists(), (
        "Old flat palette v048_aurite_pal.png must be gone (tools are vanilla now)")
    assert not (ASSETS / "textures/item/v048_riftcrystal_pal.png").exists(), (
        "Old flat palette v048_riftcrystal_pal.png must be gone")
    assert not (ASSETS / "textures/item/v048_guard_pal.png").exists(), (
        "Old flat palette v048_guard_pal.png must be gone")

    handheld_items = [
        "aurite_pickaxe", "aurite_axe", "aurite_sword", "aurite_shovel", "aurite_hoe",
        "riftcrystal_pickaxe", "riftcrystal_axe", "riftcrystal_sword",
        "riftcrystal_shovel", "riftcrystal_hoe",
    ]
    for vid in handheld_items:
        mp = ASSETS / "models/item" / f"{vid}.json"
        sp = ASSETS / "textures/item" / f"{vid}.png"
        assert mp.exists() and sp.exists(), f"Missing model or sprite for {vid}"
        mjson = json.loads(mp.read_text(encoding="utf-8"))
        assert mjson.get("parent") == "minecraft:item/handheld", (
            f"{vid}.json must use the vanilla parent minecraft:item/handheld")
        assert not mjson.get("elements"), (
            f"{vid}.json must not contain custom 3D elements (vanilla tools are flat)")
        tex = (mjson.get("textures") or {})
        assert list(tex.keys()) == ["layer0"], (
            f"{vid}.json must have exactly one texture layer0, got {list(tex.keys())}")
        assert tex["layer0"] == f"riftworld:item/{vid}", (
            f"{vid}.json layer0 must point at its own sprite")
        sarr = np.array(Image.open(sp).convert("RGBA"))
        assert sarr.shape[:2] == (16, 16), (
            f"{vid}.png must be a vanilla-sized 16x16 sprite, got {sarr.shape[:2]}")
        check_no_missing_magenta(sp)

    # Лук — ванильная модель с состояниями натяжения (overrides по предикату
    # pulling/pull), как у ванильного item/bow.json.
    bow_model = json.loads((ASSETS / "models/item/elisar_bow.json").read_text(encoding="utf-8"))
    assert bow_model.get("parent") == "minecraft:item/generated", (
        "elisar_bow.json must use the vanilla parent minecraft:item/generated")
    assert not bow_model.get("elements"), "elisar_bow.json must not contain custom 3D elements"
    overrides = bow_model.get("overrides") or []
    assert len(overrides) == 3, (
        f"elisar_bow.json must have the 3 vanilla pulling overrides, got {len(overrides)}")
    for i, ov in enumerate(overrides):
        pred = ov.get("predicate") or {}
        assert pred.get("pulling") == 1, f"bow override {i} must require pulling=1"
        assert ov.get("model") == f"riftworld:item/elisar_bow_pulling_{i}", (
            f"bow override {i} must point at elisar_bow_pulling_{i}")
        pm = ASSETS / ("models/item/elisar_bow_pulling_%d.json" % i)
        assert pm.exists(), f"Missing elisar_bow_pulling_%d.json" % i
        pjson = json.loads(pm.read_text(encoding="utf-8"))
        assert not pjson.get("elements"), "bow pulling models must be flat"
        psp = ASSETS / ("textures/item/elisar_bow_pulling_%d.png" % i)
        assert psp.exists(), f"Missing elisar_bow_pulling_%d.png" % i
        parr = np.array(Image.open(psp).convert("RGBA"))
        assert parr.shape[:2] == (16, 16), (
            f"elisar_bow_pulling_{i}.png must be 16x16, got {parr.shape[:2]}")

    # Щиты — наследники ванильного ShieldItem: модель builtin/entity,
    # состояние блокирования по предикату "blocking", а 3D-текстура в руке
    # лежит в entity/shield_base_nopattern в пространстве имён мода.
    for shield in ("elisar_shield", "elisar_guard_shield"):
        mp = ASSETS / "models/item" / f"{shield}.json"
        sp = ASSETS / "textures/item" / f"{shield}.png"
        assert mp.exists() and sp.exists(), f"Missing model or sprite for {shield}"
        mjson = json.loads(mp.read_text(encoding="utf-8"))
        assert mjson.get("parent") == "minecraft:item/generated", (
            f"{shield}.json must use the vanilla parent minecraft:item/generated")
        assert not mjson.get("elements"), (
            f"{shield}.json must not contain custom 3D elements")
        ov = mjson.get("overrides") or []
        assert len(ov) == 1 and (ov[0].get("predicate") or {}).get("blocking") == 1, (
            f"{shield}.json must have the vanilla blocking override")
        assert ov[0].get("model") == f"riftworld:item/{shield}_blocking", (
            f"{shield}.json blocking override must point at {shield}_blocking")
        blk = ASSETS / "models/item" / f"{shield}_blocking.json"
        assert blk.exists(), f"Missing {shield}_blocking.json"
        bjson = json.loads(blk.read_text(encoding="utf-8"))
        assert bjson.get("parent") == "minecraft:item/generated", (
            f"{shield}_blocking.json must use the vanilla parent minecraft:item/generated")
        assert not bjson.get("elements"), (
            f"{shield}_blocking.json must not contain custom 3D elements")
        sarr = np.array(Image.open(sp).convert("RGBA"))
        assert sarr.shape[:2] == (16, 16), (
            f"{shield}.png must be a vanilla-sized 16x16 icon, got {sarr.shape[:2]}")
        check_no_missing_magenta(sp)
        # 3D-текстура щита в руке: entity/<имя>_base и _base_nopattern,
        # ровно как у ванильного щита (entity/shield_base_nopattern).
        for suffix in ("_base.png", "_base_nopattern.png"):
            tp = ASSETS / "textures/entity" / f"{shield}{suffix}"
            assert tp.exists(), (
                f"Missing {tp} — the vanilla ShieldRenderer needs it in the mod namespace")
            tarr = np.array(Image.open(tp).convert("RGBA"))
            assert tarr.shape[:2] == (64, 64), (
                f"{shield}{suffix} must be a 64x64 vanilla shield texture, got {tarr.shape[:2]}")
            check_no_missing_magenta(tp)

    # Меч стражи — как у обычного ванильного предмета: плоский, 16x16.
    gs_model = json.loads((ASSETS / "models/item/elisar_guard_sword.json").read_text(encoding="utf-8"))
    assert gs_model.get("parent") == "minecraft:item/handheld", (
        "elisar_guard_sword.json must use the vanilla parent minecraft:item/handheld")
    assert not gs_model.get("elements"), "elisar_guard_sword.json must not contain custom 3D elements"
    gs_sprite = np.array(Image.open(ASSETS / "textures/item/elisar_guard_sword.png").convert("RGBA"))
    assert gs_sprite.shape[:2] == (16, 16), (
        f"elisar_guard_sword.png must be 16x16, got {gs_sprite.shape[:2]}")

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

    # 6a. ЛЮБОЙ shaped-рецепт мода: каждый символ раскладки обязан быть
    # объявлен в key. Иначе сервер падает при загрузке рецептов с
    # "Pattern references symbol 'L' but it's not defined in the key"
    # и рецепт молча не работает.
    for rp in sorted(rec_dir.glob("*.json")):
        rj = json.loads(rp.read_text(encoding="utf-8"))
        if rj.get("type") != "minecraft:crafting_shaped":
            continue
        pat = rj.get("pattern")
        if pat is None:
            continue
        rows = pat if isinstance(pat, list) else [pat]
        keys = rj.get("key") or {}
        assert isinstance(keys, dict), f"{rp.name}: key must be an object"
        used = set("".join(rows)) - {" "}
        missing = used - set(keys)
        assert not missing, (
            f"{rp.name}: pattern uses {sorted(missing)} but key defines {sorted(keys)} "
            f"— Minecraft would refuse to load this recipe")

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
        f"editor_sets=3 editor_parts=4 "
        f"vanilla_tools={len(handheld_items)} vanilla_bow=1 vanilla_shields=2 "
        f"rare_ore_biomes=4"
    )


if __name__ == "__main__":
    main()
