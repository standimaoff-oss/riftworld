#!/usr/bin/env python3
"""
Скачивает ванильные текстуры предметов Minecraft 1.20.1 в локальный кэш,
чтобы генератор v0.49.0 мог перекрашивать их в фирменные цвета мода.

Источник: InventivetalentDev/minecraft-assets, ветка 1.20.1.
Кэш: ~/.cache/riftworld/vanilla-assets/assets/minecraft/textures/item/

Запуск: python3 tools/rift/fetch_vanilla_item_textures.py
"""
import base64
import json
import pathlib
import subprocess
import sys

CACHE = pathlib.Path.home() / ".cache/riftworld/vanilla-assets"
DEST = CACHE / "assets/minecraft/textures/item"
REPO = "InventivetalentDev/minecraft-assets"
REF = "1.20.1"

WANTED = [
    # инструменты и оружие
    "iron_axe.png", "iron_pickaxe.png", "iron_shovel.png", "iron_hoe.png", "iron_sword.png",
    "diamond_axe.png", "diamond_pickaxe.png", "diamond_shovel.png", "diamond_hoe.png", "diamond_sword.png",
    "netherite_axe.png", "netherite_pickaxe.png", "netherite_shovel.png", "netherite_hoe.png", "netherite_sword.png",
    "wooden_axe.png", "stone_axe.png", "golden_axe.png",
    # слитки, порошки, самоцветы
    "iron_ingot.png", "gold_ingot.png", "copper_ingot.png", "netherite_ingot.png", "netherite_scrap.png",
    "diamond.png", "emerald.png", "lapis_lazuli.png", "quartz.png", "amethyst_shard.png",
    "gunpowder.png", "blaze_powder.png", "glowstone_dust.png", "bone_meal.png",
    # еда и мелочь
    "feather.png", "bone.png", "prismarine_shard.png", "prismarine_crystals.png",
    "ink_sac.png", "potion.png", "splash_potion.png", "glass_bottle.png", "bowl.png",
    "bucket.png", "milk_bucket.png", "water_bucket.png", "lava_bucket.png", "powder_snow_bucket.png",
    "apple.png", "bread.png", "cooked_beef.png", "dried_kelp.png", "kelp.png", "seagrass.png",
    "string.png", "leather.png", "ender_pearl.png", "slime_ball.png",
    "phantom_membrane.png", "nether_star.png", "echo_shard.png", "disc_fragment_5.png",
    "scute.png", "heart_of_the_sea.png", "nautilus_shell.png",
]


def fetch(name: str) -> bool:
    path = f"assets/minecraft/textures/item/{name}"
    out = DEST / name
    if out.exists():
        return True
    url = f"repos/{REPO}/contents/{path}?ref={REF}"
    r = subprocess.run(["gh", "api", url], capture_output=True, text=True)
    if r.returncode != 0:
        print("  ! нет", name)
        return False
    try:
        data = json.loads(r.stdout)
        content = base64.b64decode(data["content"])
    except Exception as e:  # noqa: BLE001
        print("  ! битый ответ для", name, e)
        return False
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(content)
    return True


def main():
    DEST.mkdir(parents=True, exist_ok=True)
    ok = sum(1 for n in WANTED if fetch(n))
    print(f"OK: {ok}/{len(WANTED)} ванильных текстур в {DEST}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
