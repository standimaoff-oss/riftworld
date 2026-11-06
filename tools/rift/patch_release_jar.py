#!/usr/bin/env python3
"""Обновляет model/PNG ассеты внутри уже собранного release-jar без JDK/Gradle.

Нужен только для того, чтобы `release/riftworld-0.40.0.jar` не отставал от
исходников после переработки моделей. Сборка CI всё равно делает jar заново.

usage: python3 tools/rift/patch_release_jar.py [path/to.jar]
"""
import pathlib
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
ASSETS = ROOT / "mod/src/main/resources/assets/riftworld"
DEFAULT_JAR = ROOT / "release/riftworld-0.40.0.jar"

REPLACE = {
    "assets/riftworld/geo/liopleurodon.json": ASSETS / "geo/liopleurodon.json",
    "assets/riftworld/geo/pegasus.json": ASSETS / "geo/pegasus.json",
    "assets/riftworld/textures/entity/liopleurodon.png": ASSETS / "textures/entity/liopleurodon.png",
    "assets/riftworld/textures/entity/pegasus.png": ASSETS / "textures/entity/pegasus.png",
}


def main():
    jar = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_JAR
    if not jar.is_file():
        raise SystemExit(f"нет jar: {jar}")
    data = {k: v.read_bytes() for k, v in REPLACE.items()}
    tmp = jar.with_suffix(jar.suffix + ".tmp")
    found = set()
    with zipfile.ZipFile(jar, "r") as src, zipfile.ZipFile(tmp, "w") as dst:
        for item in src.infolist():
            payload = data.get(item.filename)
            if payload is not None:
                found.add(item.filename)
            else:
                payload = src.read(item.filename)
            dst.writestr(item, payload)
    missing = set(REPLACE) - found
    if missing:
        tmp.unlink(missing_ok=True)
        raise SystemExit("в jar не найдены записи: " + ", ".join(sorted(missing)))
    tmp.replace(jar)
    print(f"обновлено {len(found)} ассетов в {jar.relative_to(ROOT)}")
    for name in sorted(found):
        print(" -", name)


if __name__ == "__main__":
    main()
