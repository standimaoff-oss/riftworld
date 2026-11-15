#!/usr/bin/env python3
"""v0.55.0 — генератор `ElisarProfessionData` из `ElisarProfession`.

Класс `ElisarProfessionData` — это копия торговых списков профессий, но
без Minecraft: только строки-идентификаторы предметов. Он нужен, чтобы
проверка экономики запускалась обычным `javac + java` без Forge.

Чтобы две копии не разошлись, этот скрипт генерирует класс прямо из
исходника `ElisarProfession.java` и рядом сверяет, что в нём не появилось
новых профессий.

Запуск:
    python tools/rift/gen_elisar_profession_data.py [--check]
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MOD = ROOT / "mod/src/main/java/ru/aiforge/riftworld/world"
PROF_SRC = MOD / "ElisarProfession.java"
DATA_SRC = MOD / "ElisarProfessionData.java"
BLOCKS_SRC = ROOT / "mod/src/main/java/ru/aiforge/riftworld/registry/ElisarBlocks.java"
ITEMS_SRC = ROOT / "mod/src/main/java/ru/aiforge/riftworld/registry/ModItems.java"

HEADER = """package ru.aiforge.riftworld.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * v0.55.0 — торговые списки профессий без Minecraft.
 *
 * <p>Профессии нужны в двух местах: в самом мире (жители торгуют настоящими
 * {@code ItemStack}) и в проверке экономики, которая должна запускаться
 * обычным {@code javac + java} без Forge. Этот класс — общая часть:
 * здесь лежат те же списки, но обычными строками-идентификаторами.</p>
 *
 * <p>Файл генерируется скриптом {@code tools/rift/gen_elisar_profession_data.py}
 * прямо из {@link ElisarProfession}: если торговля в мире и проверка
 * экономики разойдутся, тест будет врать, поэтому список правится
 * только через исходник.</p>
 */
public final class ElisarProfessionData {

    private static final Map<String, List<String>> GOODS = new LinkedHashMap<>();
    private static final Map<String, List<String>> BUYS = new LinkedHashMap<>();

    private ElisarProfessionData() {
    }

    /** Товары, которые торговец продаёт. */
    public static List<String> goods(String profession) {
        List<String> list = GOODS.get(profession);
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    /** Товары, которые торговец покупает у игрока. */
    public static List<String> buys(String profession) {
        List<String> list = BUYS.get(profession);
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    /** Все профессии — по именам, в порядке объявления. */
    public static List<String> professions() {
        return Collections.unmodifiableList(new ArrayList<>(GOODS.keySet()));
    }

    static {
"""


def registry_ids(path: Path, kind: str) -> dict[str, str]:
    """Имя переменной -> зарегистрированный id (без namespace)."""
    src = path.read_text(encoding="utf-8")
    ids: dict[str, str] = {}
    if kind == "item":
        pattern = (r"public static final RegistryObject<Item>\s+(\w+)\s*=\s*"
                   r"ITEMS\.register\(\s*\"([^\"]+)\"")
    else:
        pattern = (r"public static final RegistryObject<[^>]*>\s+(\w+)\s*=\s*"
                   r"(?:ModBlocks\.reg|cube|stoneWall)\(\s*\"([^\"]+)\"")
    for m in re.finditer(pattern, src):
        ids[m.group(1)] = m.group(2)
    return ids


def parse_stack_list(segment: str) -> list[str]:
    """Из куска Java-кода вытащить список предметов как item-id."""
    out: list[str] = []
    for m in re.finditer(r"new ItemStack\(([^,]+?),\s*(\d+)\)|new ItemStack\(([^)]+?)\)\s*,?\s*(\d+)?",
                         segment):
        expr = (m.group(1) or m.group(3)).strip()
        out.append(expr)
    return out


def parse_professions() -> list[tuple[str, list[str], list[str]]]:
    src = PROF_SRC.read_text(encoding="utf-8")
    blocks = re.split(r"\n    (?=[A-Z_]+\(\")", src)
    profs = []
    for block in blocks:
        m = re.match(r"\s*([A-Z_]+)\(\"", block)
        if not m:
            continue
        name = m.group(1)

        def grab(marker: str) -> list[str]:
            i = block.find(marker)
            if i < 0:
                return []
            seg = block[i:]
            j = seg.find(";")
            if j > 0:
                seg = seg[:j]
            return parse_stack_list(seg)

        profs.append((name, grab("goods()"), grab("buys()")))
    return profs


def item_id(expr: str, blocks: dict[str, str], items: dict[str, str]) -> str:
    if expr.startswith("Items."):
        return "minecraft:" + expr[len("Items."):].lower()
    m = re.match(r"ElisarBlocks\.(\w+)\.get\(\)", expr)
    if m:
        rid = blocks.get(m.group(1))
        if rid is None:
            raise SystemExit(f"Не найден id блока {m.group(1)}")
        return "riftworld:" + rid
    m = re.match(r"ModItems\.(\w+)\.get\(\)", expr)
    if m:
        rid = items.get(m.group(1))
        if rid is None:
            raise SystemExit(f"Не найден id предмета {m.group(1)}")
        return "riftworld:" + rid
    raise SystemExit(f"Не распознан предмет: {expr}")


def build() -> str:
    blocks = registry_ids(BLOCKS_SRC, "block")
    items = registry_ids(ITEMS_SRC, "item")
    profs = parse_professions()

    out = [HEADER]
    for name, goods, buys in profs:
        out.append(f'        GOODS.put("{name}", List.of(\n')
        for expr in goods:
            out.append(f'                "{item_id(expr, blocks, items)}",\n')
        out.append("        ));\n")
        out.append(f'        BUYS.put("{name}", List.of(\n')
        for expr in buys:
            out.append(f'                "{item_id(expr, blocks, items)}",\n')
        out.append("        ));\n")
    out.append("    }\n}\n")
    return "".join(out)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true",
                    help="только проверить, что файл совпадает с исходником")
    args = ap.parse_args()

    generated = build()
    if args.check:
        if not DATA_SRC.exists():
            print(f"Нет файла {DATA_SRC}", file=sys.stderr)
            return 1
        current = DATA_SRC.read_text(encoding="utf-8")
        if current != generated:
            print("ElisarProfessionData разошёлся с ElisarProfession — "
                  "перегенерируй: python tools/rift/gen_elisar_profession_data.py",
                  file=sys.stderr)
            return 1
        print("OK: ElisarProfessionData совпадает с ElisarProfession")
        return 0

    DATA_SRC.write_text(generated, encoding="utf-8")
    count = len(parse_professions())
    print(f"OK: ElisarProfessionData обновлён, профессий: {count}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
