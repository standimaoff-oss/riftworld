#!/usr/bin/env python3
"""One command that runs every check of the mod and stops at the first failure.

    python3 tools/rift/check_all.py            # всё, что можно проверить без Minecraft
    python3 tools/rift/check_all.py --quick    # без рендеров и без самопроверки валидатора

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

There is no game launch here: it needs a JDK, the Forge maven and a display, none of which exist in this environment
(see reports/rev7_part2_trees.md, "Чем это проверено").
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


def main() -> int:
    quick = "--quick" in sys.argv
    print(f"проверка мода в {ROOT}\n")
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
    if not quick:
        if not run("рендеры отчёта (листы деревьев и сцен)",
                   [PY, os.path.join(HERE, "render_trees.py")])[0]:
            return 1
    print("\nВСЁ ЗЕЛЁНОЕ")
    return 0


if __name__ == "__main__":
    sys.exit(main())
