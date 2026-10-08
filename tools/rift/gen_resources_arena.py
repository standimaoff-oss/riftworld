"""Rev 3 / C-3 throne-hall mechanics: blockstates / models / lang for boss_column + arena_core (merges; run after the others)."""
import json, os

R = "/home/user/riftworld/mod/src/main/resources"
A = R + "/assets/riftworld"
NS = "riftworld:block/"


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)


def merge(path, obj):
    old = json.load(open(path, encoding="utf-8")) if os.path.exists(path) else {}
    old.update(obj)
    w(path, old)


# the column is drawn by its block-entity renderer: the block model only carries the break-particle texture
w(A + "/blockstates/boss_column.json", {"variants": {"": {"model": NS + "boss_column"}}})
w(A + "/models/block/boss_column.json", {"textures": {"particle": NS + "ashlar"}})
# the arena heart sits inside the throne dais and looks like its ashlar
w(A + "/blockstates/arena_core.json", {"variants": {"": {"model": NS + "ashlar"}}})

L = {
    "block.riftworld.boss_column": ("Healing Column", "Целительная колонна"),
    "block.riftworld.arena_core": ("Heart of the Hall", "Сердце Чертога"),
    "riftworld.arena.enter": ("The hall falls silent… something stirs in the dark…", "Зал затихает… во тьме что-то шевелится."),
    "riftworld.arena.awaken": ("The Lord of the Hall awakens! The columns feed him.", "Хозяин Чертога пробуждается! Колонны питают его силой."),
    "riftworld.arena.columns_left": ("Healing columns left: %s", "Целительных колонн осталось: %s"),
    "riftworld.arena.columns_down": ("All columns are broken — nothing heals him now", "Все колонны разрушены — его больше ничто не лечит"),
    "riftworld.column.crack": ("The crystal cracks (%s/%s)", "Кристалл треснул (%s/%s)"),
    "riftworld.column.hint": ("Strike the crystal at the top — or use a pickaxe", "Бейте по кристаллу на вершине — или киркой"),
}
merge(A + "/lang/en_us.json", {k: v[0] for k, v in L.items()})
merge(A + "/lang/ru_ru.json", {k: v[1] for k, v in L.items()})
print("arena resources ok", len(L))
