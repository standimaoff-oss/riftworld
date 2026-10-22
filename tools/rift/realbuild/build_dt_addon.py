#!/usr/bin/env python3
"""Собирает data-only аддон Dynamic Trees (`riftworld_dt-<ver>.jar`) без Gradle и без сети.

Аддон целиком состоит из ресурсов: `trees/riftworld_dt/` (дерево-пак DT), `assets/…` (семена, переводы) и
`META-INF/mods.toml`. Java-классов нет, поэтому обычная сборка ForgeGradle здесь не нужна — достаточно
упаковать `dtcompat/src/main/resources` в jar и проверить, что внутри лежит всё обязательное.

Почему `modLoader = "lowcodefml"`: у мода нет ни одного класса, а `javafml` без `@Mod` Forge отвергает
(«Mod file … has no mod class»). `lowcodefml` — штатный загрузчик для data-only модов, классы ему не нужны.

Usage:
    python3 tools/rift/realbuild/build_dt_addon.py                 # -> dist/riftworld_dt-1.0.0.jar
    python3 tools/rift/realbuild/build_dt_addon.py --check         # только проверить содержимое
    python3 tools/rift/realbuild/build_dt_addon.py --out /tmp/x.jar
"""
from __future__ import annotations

import argparse
import json
import sys
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
SRC = ROOT / "dtcompat/src/main/resources"
GRADLE = (ROOT / "dtcompat/gradle.properties").read_text(encoding="utf-8")


def prop(name: str, default: str) -> str:
    for line in GRADLE.splitlines():
        line = line.strip()
        if line.startswith(name + "="):
            return line.split("=", 1)[1].strip()
    return default


REQUIRED = [
    "META-INF/mods.toml",
    "pack.mcmeta",
    "trees/riftworld_dt/world_gen/riftworld.json",
]
SPECIES = ["lilac", "sizo_v2", "fern_tree", "mist_pine"]


def collect() -> list[Path]:
    return sorted(p for p in SRC.rglob("*") if p.is_file())


def check(files: list[Path], verbose: bool = True) -> list[str]:
    have = {p.relative_to(SRC).as_posix() for p in files}
    problems = [f"нет обязательного файла {r}" for r in REQUIRED if r not in have]
    for sp in SPECIES:
        for kind in ("species", "families", "leaves_properties"):
            rel = f"trees/riftworld_dt/{kind}/{sp}.json"
            if rel not in have:
                problems.append(f"нет {rel}")
    # mods.toml обязан быть data-only загрузчиком, иначе Forge ищет класс и падает
    toml = (SRC / "META-INF/mods.toml").read_text(encoding="utf-8")
    if 'modLoader = "lowcodefml"' not in toml:
        problems.append('mods.toml: modLoader должен быть "lowcodefml" (у аддона нет классов)')
    for must in ('modId = "riftworld_dt"', 'modId = "riftworld"', 'modId = "dynamictrees"'):
        if must not in toml:
            problems.append(f"mods.toml: нет {must}")
    # Forge 1.20.1 отвергает мод без `mandatory` в каждой зависимости («Missing required field mandatory») —
    # поймано живым запуском сервера, валидатор пака этого не видел.
    if toml.count("[[dependencies.riftworld_dt]]") != toml.count("mandatory ="):
        problems.append("mods.toml: у каждой зависимости должно быть mandatory = true|false (Forge 1.20.1)")
    # json-ы должны парситься, иначе DT молча выбросит пак
    bad = 0
    for p in files:
        if p.suffix != ".json":
            continue
        try:
            json.loads(p.read_text(encoding="utf-8"))
        except Exception as e:
            problems.append(f"{p.relative_to(SRC)}: {e}")
            bad += 1
    if verbose:
        print(f"[пак] файлов {len(files)}, видов {len(SPECIES)}, json-ошибок {bad}")
    return problems


def build(out: Path) -> int:
    files = collect()
    problems = check(files)
    if problems:
        for p in problems:
            print("  ! " + p)
        return 1
    version = prop("dt_addon_version", "1.0.0")
    out.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        for p in files:
            z.write(p, p.relative_to(SRC).as_posix())
        # манифест как у обычного мода: Forge читает его для отчёта о загрузке
        z.writestr("META-INF/MANIFEST.MF",
                   "Manifest-Version: 1.0\nSpecification-Title: riftworld_dt\n"
                   f"Implementation-Version: {version}\nImplementation-Vendor: rift\n")
    print(f"[аддон] {out} ({out.stat().st_size // 1024} КБ, файлов {len(files)}, версия {version})")
    with zipfile.ZipFile(out) as z:
        names = z.namelist()
    print(f"[аддон] внутри: species {sum(1 for n in names if '/species/' in n)}, "
          f"families {sum(1 for n in names if '/families/' in n)}, "
          f"текстуры семян {sum(1 for n in names if n.endswith('.png'))}")
    return 0


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=None)
    ap.add_argument("--check", action="store_true")
    args = ap.parse_args()
    if args.check:
        problems = check(collect())
        for p in problems:
            print("  ! " + p)
        print("ПАК В ПОРЯДКЕ" if not problems else f"проблем: {len(problems)}")
        return 0 if not problems else 1
    version = prop("dt_addon_version", "1.0.0")
    out = Path(args.out or (ROOT / "dist" / f"riftworld_dt-{version}.jar"))
    return build(out)


if __name__ == "__main__":
    sys.exit(main())
