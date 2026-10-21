#!/usr/bin/env python3
"""Runs the REAL Java tests of the mod without Minecraft, Gradle or the Forge maven.

How it works in a sandbox with no JDK and no access to Mojang/Forge:

  * a JVM comes from the `jdk4py` pypi wheel (Temurin, JRE only — that is enough to *run* javac's output);
  * a Java compiler comes from the `karellen-jdtls` pypi wheel: it ships `org.eclipse.jdt.core.compiler.batch`
    (ECJ), which is a complete Java 17 compiler and runs on any JVM;
  * the few Minecraft classes the tested code touches (`Vec3`, `BlockPos`, `AABB`, `Direction`) are replaced by
    tiny stubs in `tools/rift/jtests/stubs` — same public API, real maths.

So this is not a syntax check: the classes are compiled and executed.

Usage:
  python3 tools/rift/jtests/run.py            # compile + all checks
  python3 tools/rift/jtests/run.py --refresh  # re-download the compiler
  python3 tools/rift/jtests/run.py --keep     # keep build output

Exit code 0 only if every test passes.
"""
from __future__ import annotations

import glob
import os
import subprocess
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
BUILD = "/tmp/rift-jtests"
CACHE = os.path.join(HERE, ".cache")
ECJ_GLOB = "org.eclipse.jdt.core.compiler.batch_*.jar"
SRC = os.path.join(ROOT, "mod", "src", "main", "java")


# --------------------------------------------------------------------------- toolchain
def jvm() -> str:
    try:
        import jdk4py
    except ImportError:
        sys.exit("нет JVM: pip install --break-system-packages jdk4py")
    java = os.path.join(jdk4py.JAVA_HOME, "bin", "java")
    if not os.path.exists(java):
        sys.exit("jdk4py не содержит bin/java")
    return java


def ecj(refresh: bool = False) -> str:
    found = glob.glob(os.path.join(CACHE, "**", ECJ_GLOB), recursive=True)
    if found and not refresh:
        return found[0]
    os.makedirs(CACHE, exist_ok=True)
    whl_dir = os.path.join(CACHE, "wheel")
    os.makedirs(whl_dir, exist_ok=True)
    if not glob.glob(os.path.join(whl_dir, "*.whl")):
        subprocess.run([sys.executable, "-m", "pip", "download", "--no-deps", "-d", whl_dir,
                        "karellen-jdtls"], check=True, stdout=subprocess.DEVNULL)
    wheel = glob.glob(os.path.join(whl_dir, "*.whl"))[0]
    with zipfile.ZipFile(wheel) as z:
        for name in z.namelist():
            base = os.path.basename(name)
            if base.startswith("org.eclipse.jdt.core.compiler.batch") and base.endswith(".jar"):
                out = os.path.join(CACHE, base)
                with open(out, "wb") as f:
                    f.write(z.read(name))
                return out
    sys.exit("в wheel karellen-jdtls нет ECJ")


def run(cmd, **kw) -> subprocess.CompletedProcess:
    kw.setdefault("capture_output", True)
    return subprocess.run(cmd, text=True, **kw)


# --------------------------------------------------------------------------- build
def compile_all(java: str, compiler: str) -> None:
    os.makedirs(BUILD, exist_ok=True)
    out = os.path.join(BUILD, "classes")
    os.makedirs(out, exist_ok=True)
    sources = [
        os.path.join(SRC, "ru", "aiforge", "riftworld", "tree", "TreeGen.java"),
        os.path.join(SRC, "ru", "aiforge", "riftworld", "tree", "TreeSpecies.java"),
        os.path.join(SRC, "ru", "aiforge", "riftworld", "bridge", "BridgeDeckGeom.java"),
        os.path.join(SRC, "ru", "aiforge", "riftworld", "bridge", "BridgeStyle.java"),
        # DT soft dependency: the switch that decides whether our own tree engine may run at all
        os.path.join(SRC, "ru", "aiforge", "riftworld", "compat", "DynamicTreesCompat.java"),
    ]
    sources += glob.glob(os.path.join(HERE, "stubs", "**", "*.java"), recursive=True)
    sources += glob.glob(os.path.join(HERE, "src", "**", "*.java"), recursive=True)
    cmd = [java, "-jar", compiler, "-17", "-nowarn", "-proc:none", "-d", out,
           "-encoding", "UTF-8"] + sources
    res = run(cmd, capture_output=True)
    if res.returncode != 0:
        print((res.stdout + res.stderr)[-4000:])
        sys.exit("компиляция не прошла")
    warnings = [ln for ln in (res.stdout + res.stderr).splitlines() if "WARNING" in ln]
    if warnings:
        print("\n".join(warnings[:10]))
    print(f"compile: {len(sources)} файлов -> {out}")


# --------------------------------------------------------------------------- python cross-check
def python_dump(path: str) -> None:
    sys.path.insert(0, os.path.join(ROOT, "tools", "rift"))
    import tree_gen as TG  # noqa: E402

    species = [("sizo", TG.SIZO, 10, 13), ("ambra", TG.AMBRA, 7, 10), ("sizo_giant", TG.SIZO_GIANT, 17, 22),
               ("lilac", TG.LILAC, 12, 17), ("fern", TG.FERN, 7, 10), ("mist_pine", TG.MIST_PINE, 14, 20),
               ("oak", TG.OAK, 8, 12)]
    with open(path, "w", encoding="utf-8") as f:
        for name, kind, hmin, hmax in species:
            for h in range(hmin, hmax + 1):
                for seed in (1, 424242, 1000 + kind, 987654321):
                    nodes = sorted((nodes_of(TG, kind, h, seed)), key=lambda n: (n[1], n[2], n[0], n[3]))
                    for n in nodes:
                        f.write(f"{name} {h} {seed} {n[0]} {n[1]} {n[2]} {n[3]} {n[4]}\n")
                    f.write(f"# {name} {h} {seed} {len(nodes)}\n")


def nodes_of(TG, kind, h, seed):
    return [(dx, dy, dz, k, ax) for dx, dy, dz, k, ax in TG.build(kind, h, seed)]


def compare_dumps(java_file: str, py_file: str) -> None:
    with open(java_file, encoding="utf-8") as f:
        j = f.read().splitlines()
    with open(py_file, encoding="utf-8") as f:
        p = f.read().splitlines()
    if j == p:
        trees = sum(1 for ln in j if ln.startswith("#"))
        print(f"parity: Java == Python, {trees} деревьев, {len(j)} строк совпали байт в байт")
        return
    diff = next((i for i, (a, b) in enumerate(zip(j, p)) if a != b), min(len(j), len(p)))
    print(f"parity: РАСХОЖДЕНИЕ на строке {diff}:")
    print("  java  :", j[diff] if diff < len(j) else "<eof>")
    print("  python:", p[diff] if diff < len(p) else "<eof>")
    print(f"  всего строк: java {len(j)}, python {len(p)}")
    sys.exit(1)


# --------------------------------------------------------------------------- main
def main() -> None:
    refresh = "--refresh" in sys.argv
    java = jvm()
    compiler = ecj(refresh)
    print(f"toolchain: {java} + ECJ {os.path.basename(compiler)}")
    compile_all(java, compiler)
    cp = os.path.join(BUILD, "classes")

    print("\n== tree engine: структурные инварианты")
    r = run([java, "-cp", cp, "riftcheck.TreeGenCheck", "check"])
    print(r.stdout.strip())
    if r.returncode != 0:
        sys.exit(1)

    print("\n== bridge geometry: инварианты под любым углом")
    r = run([java, "-cp", cp, "riftcheck.BridgeGeomCheck"])
    print(r.stdout.strip())
    if r.returncode != 0:
        sys.exit(1)

    print("\n== soft dependency: DT есть / нет / Forge нет")
    r = run([java, "-cp", cp, "riftcheck.DtCompatCheck"])
    print(r.stdout.strip())
    if r.returncode != 0:
        if r.stderr.strip():
            print(r.stderr.strip()[-2000:])
        sys.exit(1)

    print("\n== parity: Java-движок против Python-эталона (на нём сняты фото)")
    jf = os.path.join(BUILD, "tree_dump_java.txt")
    pf = os.path.join(BUILD, "tree_dump_py.txt")
    run([java, "-cp", cp, "riftcheck.TreeGenCheck", "dump", jf], check=True)
    python_dump(pf)
    compare_dumps(jf, pf)

    print("\nВСЕ JAVA-ТЕСТЫ ПРОЙДЕНЫ")


if __name__ == "__main__":
    main()
