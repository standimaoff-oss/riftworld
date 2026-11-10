#!/usr/bin/env python3
"""Проверка синхронизированных данных сущностей Riftworld.

`SynchedEntityData.defineId(...)` только ОБЪЯВЛЯЕТ ключ, а `entityData.set(...)`
к нему обращается. Если ключ не зарегистрирован через `entityData.define(...)`,
то обращение даёт NullPointerException прямо в конструкторе сущности — и моб
просто не появляется в игре, хотя код компилируется и «выглядит» правильно.

Именно так сломался страж Элисара в v0.49.0. Этот скрипт ловит такую ошибку
до сборки: для каждого `EntityDataAccessor` в классе проверяется, что он
зарегистрирован в `defineSynchedData()`.

Запуск:  python3 tools/rift/validate_v049_synched_data.py
Выход:   0 — все ключи зарегистрированы, 1 — есть нерегистрированные.
"""
from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
SRC = ROOT / "mod" / "src" / "main" / "java"

ACCESSOR_RE = re.compile(
    r"EntityDataAccessor<\s*\w+\s*>\s+(\w+)\s*=\s*"
    r"(?:\n\s*)?SynchedEntityData\.defineId",
)
DEFINE_RE = re.compile(r"entityData\.define\(\s*(\w+)")
DEFINE_METHOD_RE = re.compile(
    r"protected void defineSynchedData\(\)\s*\{(.*?)\n    \}", re.S
)


def check(path: pathlib.Path) -> list[str]:
    """Вернуть список проблем одного файла (пустой список — всё в порядке)."""
    text = path.read_text(encoding="utf-8")
    accessors = set(ACCESSOR_RE.findall(text))
    if not accessors:
        return []
    match = DEFINE_METHOD_RE.search(text)
    if not match:
        return [f"{path.name}: есть EntityDataAccessor, но нет defineSynchedData()"]
    defined = set(DEFINE_RE.findall(match.group(1)))
    missing = sorted(accessors - defined)
    if missing:
        return [
            f"{path.name}: не зарегистрированы в defineSynchedData(): "
            + ", ".join(missing)
        ]
    return []


def main() -> int:
    problems: list[str] = []
    checked = 0
    for path in sorted(SRC.rglob("*.java")):
        checked += 1
        problems.extend(check(path))
    if problems:
        print("FAIL: синхронизированные данные сущностей объявлены, но не заданы:")
        for problem in problems:
            print(f"  - {problem}")
        return 1
    print(f"OK: проверено {checked} файлов — все EntityDataAccessor зарегистрированы.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
