"""Rev 3 / part A: lang for the boss, guards, weapons, spawn eggs, Ashen Lands (merges; run after the others)."""
import json, os

A = os.path.join(os.path.dirname(__file__), "../../mods/riftworld/src/main/resources/assets/riftworld")


def merge(path, obj):
    d = {}
    if os.path.exists(path):
        with open(path, encoding="utf-8") as f:
            d = json.load(f)
    d.update(obj)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(d, f, ensure_ascii=False, indent=2)


L = {
    "item.riftworld.castle_compass": ("Compass to the Hall of the Smouldering Horn", "Компас к Чертогу Тлеющего Рога"),
    "riftworld.castle_compass.got": ("A compass found in your pocket points to the Ashen Lands - to the Hall of the Smouldering Horn (40, 470).",
                                     "В кармане оказался компас: он указывает на Адские угодья — к Чертогу Тлеющего Рога (40, 470)."),
    "riftworld.compass.ruins": ("The compass needle swings towards the nearest Smouldering Ruins (%s, %s).",
                                "Стрелка компаса качнулась к ближайшим Тлеющим руинам (%s, %s)."),
    "riftworld.compass.castle": ("The compass points to the Hall of the Smouldering Horn again.",
                                 "Компас снова указывает на Чертог Тлеющего Рога."),
    "riftworld.compass.none": ("No Smouldering Ruins nearby.", "Поблизости нет Тлеющих руин."),
    "item.riftworld.castle_compass.tip": ("Sneak + right click: castle / nearest Smouldering Ruins",
                                          "Присед + ПКМ: замок / ближайшие Тлеющие руины"),
    "entity.riftworld.morgrath": ("Morgrath, Master of the Hall", "Морграт, Хозяин Чертога"),
    "entity.riftworld.ember_guard": ("Smouldering Warden", "Тлеющий страж"),
    "entity.riftworld.scythe_item": ("Morgrath's Scythe", "Коса Морграта"),
    "item.riftworld.morgrath_scythe": ("Morgrath's Scythe", "Коса Морграта"),
    "item.riftworld.morgrath_scythe.tip1": ("Right click - super attacks:", "ПКМ - суператаки:"),
    "item.riftworld.morgrath_scythe.tip2": ("  standing - Reaping (spin around)", "  на месте - «Жатва» (круговой взмах)"),
    "item.riftworld.morgrath_scythe.tip3": ("  sneaking - Fire Furrow (fire along the ground)", "  присед - «Огненная борозда» (огонь по земле)"),
    "item.riftworld.morgrath_scythe.tip4": ("  sprinting - Reaper's Dash", "  бег - «Рывок жнеца»"),
    "item.riftworld.morgrath_scythe.tip5": ("  in the air - Comet Fall (shock wave)", "  в прыжке - «Падение кометы» (ударная волна)"),
    "item.riftworld.morgrath_scythe.combo": ("LMB combo: Slanting Sweep → Cleaving Chop → Ashen Reaping (ring of embers)", "Комбо ЛКМ: «Косой мах» → «Рубящий удар» → «Пепельная жатва» (кольцо углей)"),
    "item.riftworld.guard_sickle.combo": ("LMB combo: Slash → Hook (pulls the foe) → Cross Cut (slows, bleeds)", "Комбо ЛКМ: «Рассечение» → «Крюк» (притягивает) → «Крестовый разрез» (замедляет, кровотечение)"),
    "item.riftworld.guard_sickle": ("Warden's Hook-Sickle", "Крюк-серп стража"),
    "item.riftworld.morgrath_spawn_egg": ("Morgrath Spawn Egg", "Яйцо призыва: Морграт"),
    "item.riftworld.ember_guard_spawn_egg": ("Smouldering Warden Spawn Egg", "Яйцо призыва: Тлеющий страж"),
    "riftworld.arena.defeated": ("The Master of the Hall has fallen. His scythe lies on the floor...", "Хозяин Чертога повержен. Его коса лежит на полу…"),
    "biome.riftworld.ashen_lands": ("Ashen Lands", "Адские угодья"),
    "biome.riftworld.elisar_city": ("Elisar", "Элисар"),
    "block.riftworld.magma_meander": ("Lava Meander", "Лавовая извилина"),
}
merge(A + "/lang/en_us.json", {k: v[0] for k, v in L.items()})
merge(A + "/lang/ru_ru.json", {k: v[1] for k, v in L.items()})
print("CREATURE_RES_OK")
