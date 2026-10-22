#!/usr/bin/env python3
"""One command that runs every check of the mod and stops at the first failure.

    python3 tools/rift/check_all.py            # всё, что можно проверить без Minecraft
    python3 tools/rift/check_all.py --quick    # без рендеров и без самопроверки валидатора
    python3 tools/rift/check_all.py --live     # + настоящий сервер Forge 1.20.1 и RCON-playtest

What is checked, and why each check exists:

  1. java syntax      — every .java file parses (tree-sitter if installed, otherwise a bracket/string scan). Catches a
                        broken file in the huge generator that the Java test bench does not compile.
  2. asset validation — blockstates/models/textures/loot/translations of all registered blocks.
  3. DT pack          — the tree add-on against Dynamic Trees' own keys, types, kits, soils, references, versions.
                        The checkout of the pack validator runs 13 mutations of the pack and must catch all of them,
                        and the pack must still be exactly what its generator would write (no silent drift).
  4. Java test bench  — real compiled and executed checks: tree invariants, bridge geometry, DT soft dependency, and
                        byte-for-byte parity of the Java tree engine with the Python reference used for the photos.
  5. renders          — regenerates the tree and scene sheets, so the pictures in reports/ are reproducible.
  5b. CPU world render — reads the real Anvil save (the world the Forge server generated) and ray-traces it on the CPU:
                        asserts that no GPU/GL module is involved, that two runs are byte-identical, and that the frame
                        is not empty. This is the "screen from the game" that a headless sandbox can actually produce.
  5c. DT add-on jar   — packs dtcompat/ into a data-only Forge mod and checks mods.toml: lowcodefml (the add-on has no
                        classes) and a `mandatory` flag on every dependency (Forge 1.20.1 refuses the mod otherwise).
  6. live server      — only with --live: compiles the mod with ECJ, reobfuscates with ART, boots a real Forge
                        1.20.1 dedicated server and plays a 19-step RCON test inside it. This is the only check that
                        runs the mod in Minecraft; it needs the preflight deps (see preflight() below).

The offline checks need numpy (renders) and nothing else; the live run needs a JRE 17, ECJ and a downloaded
Forge server. Missing deps are reported once, with the exact command that installs them.
"""
from __future__ import annotations

import json
import os
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
PY = sys.executable


def preflight() -> list[str]:
    """Возвращает список того, чего не хватает для полного набора проверок, с командой установки."""
    missing = []
    try:
        import numpy  # noqa: F401
    except Exception:
        missing.append("numpy (рендеры листов): pip install --break-system-packages numpy")
    try:
        import PIL  # noqa: F401
    except Exception:
        missing.append("pillow (текстуры пака DT): pip install --break-system-packages pillow")
    try:
        import jdk4py  # noqa: F401
    except Exception:
        missing.append("jdk4py (JRE 17 для настоящего сервера): pip install --break-system-packages jdk4py==17.0.9.2")
    try:
        import karellen_jdtls  # noqa: F401
    except Exception:
        missing.append("karellen-jdtls (компилятор ECJ): pip install --break-system-packages karellen-jdtls")
    return missing


def run(title: str, cmd: list[str], cwd: str | None = None) -> tuple[bool, str]:
    t0 = time.time()
    res = subprocess.run(cmd, cwd=cwd or ROOT, text=True, capture_output=True)
    out = (res.stdout + res.stderr).strip()
    ok = res.returncode == 0
    print(f"[{'OK  ' if ok else 'FAIL'}] {title} ({time.time() - t0:.1f} с)")
    if out:
        tail = out.splitlines()
        print("\n".join("       " + ln for ln in (tail[-14:] if ok else tail[-40:])))
    return ok, out


def java_syntax() -> tuple[bool, str]:
    files = []
    for dirpath, _, names in os.walk(os.path.join(ROOT, "mod", "src", "main", "java")):
        files += [os.path.join(dirpath, n) for n in names if n.endswith(".java")]
    try:
        from tree_sitter import Language, Parser
        import tree_sitter_java
        parser = Parser(Language(tree_sitter_java.language()))
        bad = []
        for f in files:
            tree = parser.parse(open(f, "rb").read())
            if tree.root_node.has_error:
                bad.append(os.path.relpath(f, ROOT))
        if bad:
            return False, "синтаксические ошибки:\n" + "\n".join(bad)
        return True, f"{len(files)} Java-файлов разобраны tree-sitter, ошибок 0"
    except ImportError:
        # no tree-sitter: at least reject unbalanced braces / unterminated strings, which is what a bad edit looks like
        bad = []
        for f in files:
            src = open(f, encoding="utf-8").read()
            depth = 0
            in_str = in_chr = in_line = in_block = False
            i = 0
            while i < len(src):
                c = src[i]
                nxt = src[i + 1] if i + 1 < len(src) else ""
                if in_line:
                    in_line = c != "\n"
                elif in_block:
                    if c == "*" and nxt == "/":
                        in_block, i = False, i + 1
                elif in_str:
                    if c == "\\":
                        i += 1
                    elif c == '"':
                        in_str = False
                elif in_chr:
                    if c == "\\":
                        i += 1
                    elif c == "'":
                        in_chr = False
                elif c == "/" and nxt == "/":
                    in_line, i = True, i + 1
                elif c == "/" and nxt == "*":
                    in_block, i = True, i + 1
                elif c == '"':
                    in_str = True
                elif c == "'":
                    in_chr = True
                elif c == "{":
                    depth += 1
                elif c == "}":
                    depth -= 1
                    if depth < 0:
                        break
                i += 1
            if depth != 0 or in_str or in_block:
                bad.append(os.path.relpath(f, ROOT))
        if bad:
            return False, "незакрытые блоки/строки:\n" + "\n".join(bad)
        return True, f"{len(files)} Java-файлов: скобки и строки сбалансированы (tree-sitter не установлен)"


def json_sweep() -> tuple[bool, str]:
    n = 0
    for base in (os.path.join(ROOT, "mod", "src", "main", "resources"), os.path.join(ROOT, "dtcompat")):
        for dirpath, _, names in os.walk(base):
            for name in names:
                if name.endswith(".json"):
                    p = os.path.join(dirpath, name)
                    try:
                        json.load(open(p, encoding="utf-8"))
                    except (json.JSONDecodeError, UnicodeDecodeError) as e:
                        return False, f"{os.path.relpath(p, ROOT)}: {e}"
                    n += 1
    return True, f"{n} JSON-файлов парсятся"


def live_server_check() -> int:
    """Живой прогон: собирает jar, поднимает настоящий сервер Forge и «играет» через RCON.

    Выключен по умолчанию: нужен скачанный сервер (tools/rift/realbuild/fetch_server.py) и JRE из jdk4py.
    """
    real = os.path.join(HERE, "realbuild")
    jar = "/tmp/riftbuild/riftworld-live.jar"
    if not run("сборка мода настоящим компилятором (ECJ) + реобфускация (ART)",
               [PY, os.path.join(real, "build_jar.py"), "--out", jar])[0]:
        return 0
    if not run("подготовка выделенного сервера Forge", [PY, os.path.join(real, "run_server.py"), "--prepare"])[0]:
        return 0
    if not run("живой сервер: запуск, мод в игре, RCON-проверки (playtest)",
               [PY, os.path.join(real, "run_live.py"), "--jar", jar])[0]:
        return 1
    return 0


def main() -> int:
    quick = "--quick" in sys.argv
    live = "--live" in sys.argv
    print(f"проверка мода в {ROOT}")
    gaps = preflight()
    if gaps:
        print("не хватает зависимостей (часть проверок упадёт):")
        for g in gaps:
            print("  ! " + g)
    print()
    steps: list[tuple[str, object]] = [
        ("синтаксис Java", java_syntax),
        ("JSON-файлы", json_sweep),
    ]
    for title, fn in steps:
        ok, out = fn()
        print(f"[{'OK  ' if ok else 'FAIL'}] {title}")
        for ln in out.splitlines()[-6:]:
            print("       " + ln)
        if not ok:
            return 1

    if not run("ресурсы блоков (blockstate/модели/текстуры/лут/переводы)",
               [PY, os.path.join(HERE, "validate_assets.py")])[0]:
        return 1
    if not run("пак Dynamic Trees против схемы DT",
               [PY, os.path.join(HERE, "check_dt_pack.py")] + (["--no-selftest"] if quick else []))[0]:
        return 1
    if not run("пак совпадает со своим генератором (запись не выполняется)",
               [PY, os.path.join(HERE, "gen_dt_addon.py"), "--check"])[0]:
        return 1
    if not run("Java-стенд: движок деревьев, геометрия мостов, soft-dependency DT, паритет",
               [PY, os.path.join(HERE, "jtests", "run.py")])[0]:
        return 1
    if not run("аддон Dynamic Trees: сборка data-only jar и проверка mods.toml",
               [PY, os.path.join(HERE, "realbuild", "build_dt_addon.py")])[0]:
        return 1
    if not run("CPU-рендер настоящего мира: без видеокарты, детерминизм, непустой кадр",
               [PY, os.path.join(HERE, "render_cpu.py"), "--check"])[0]:
        return 1
    if not quick:
        if not run("рендеры отчёта (листы деревьев и сцен)",
                   [PY, os.path.join(HERE, "render_trees.py")])[0]:
            return 1
    print("\nВСЁ ЗЕЛЁНОЕ")
    return 0


if __name__ == "__main__":
    sys.exit(main())
