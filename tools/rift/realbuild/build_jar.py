#!/usr/bin/env python3
"""Собирает настоящий мод-джар без Gradle: ECJ-компиляция + реобфускация ART + упаковка ресурсов.

Почему не Gradle: в песочнице нет JDK (только JRE 17 из jdk4py), а maven.minecraftforge.net недоступен,
поэтому ForgeGradle не может скачать ни MC, ни Forge. Зато есть:
  * компилятор ECJ из колеса `karellen-jdtls` (полноценный javac-совместимый фронтенд);
  * dev-артефакт Forge `forge-...-mapped_official_1.20.1.jar` (клиент+сервер, официальные имена, AT применены),
    скачанный `fetch_devjar.py` из git-кэша ForgeGradle на GitHub;
  * ForgeAutoRenamingTool + маппинги official<->srg, которые `make_mappings.py` собирает из файлов серверной
    установки Forge.

Пайплайн:
  1. ant-file: список всех .java в mod/src/main/java;
  2. `java -jar ecj.jar ...` -> build/classes (ошибки = провал сборки);
  3. ART: mojmap -> srg (два прохода, см. make_mappings.py: с маппингом «класс в себя» ART не трогает члены);
  4. упаковка: классы + src/main/resources + mods.toml с подставленными значениями из gradle.properties.

Usage:
    python3 tools/rift/realbuild/build_jar.py [--out dist/riftworld-<version>.jar] [--skip-reobf]
"""
import argparse
import os
import re
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
MOD = ROOT / "mod"
SERVER = Path(os.environ.get("MC_SERVER_DIR", "/tmp/mcserver"))
BUILD = Path(os.environ.get("RIFT_BUILD_DIR", "/tmp/riftbuild"))
JAVA = Path(os.environ.get("JAVA", "/usr/local/lib/python3.11/dist-packages/jdk4py/java-runtime/bin/java"))
ECJ = Path(os.environ.get(
    "ECJ",
    "/usr/local/lib/karellen-jdtls-kotlin/plugins/org.eclipse.jdt.core.compiler.batch_3.46.0.v20260512-0752.jar"))
DEVJAR = Path(os.environ.get("MC_DEVJAR", str(SERVER / "forge-mapped-official.jar")))
ART = SERVER / "libraries/net/minecraftforge/ForgeAutoRenamingTool/0.1.22/ForgeAutoRenamingTool-0.1.22-all.jar"


def sh(cmd, **kw):
    print("+", " ".join(str(c) for c in cmd) if isinstance(cmd, list) else cmd)
    res = subprocess.run(cmd, capture_output=True, text=True, **kw)
    if res.returncode != 0:
        print(res.stdout[-4000:])
        print(res.stderr[-2000:])
        raise SystemExit(f"команда завершилась с кодом {res.returncode}")
    return res


def gradle_props():
    props = {}
    for line in (MOD / "gradle.properties").read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            k, v = line.split("=", 1)
            props[k.strip()] = v.strip()
    return props


def libs_classpath():
    """Все jar'ы серверной установки, кроме вторых копий Minecraft и самого ART."""
    skip = ("/net/minecraft/server/", "forge-mapped-official.jar", "forge-", "ForgeAutoRenamingTool")
    jars = []
    for jar in (SERVER / "libraries").rglob("*.jar"):
        s = str(jar)
        if any(x in s for x in skip):
            continue
        jars.append(str(jar))
    return jars


def source_list():
    srcs = sorted(str(p) for p in (MOD / "src/main/java").rglob("*.java"))
    argfile = BUILD / "sources.txt"
    argfile.write_text("\n".join(srcs), encoding="utf-8")
    return argfile, len(srcs)


def compile_java(argfile):
    classes = BUILD / "classes"
    shutil.rmtree(classes, ignore_errors=True)
    classes.mkdir(parents=True)
    cp = ":".join([str(DEVJAR)] + libs_classpath())
    res = subprocess.run([str(JAVA), "-jar", str(ECJ), "-17", "-nowarn", "-proc:none",
                          "-d", str(classes), "-encoding", "UTF-8", "-cp", cp, f"@{argfile}"],
                         capture_output=True, text=True)
    errors = [ln for ln in res.stdout.splitlines() if re.match(r"\d+\. ERROR", ln)]
    if errors:
        print("\n".join(res.stdout.splitlines()[:120]))
        raise SystemExit(f"ECJ: {len(errors)} ошибок компиляции")
    count = sum(1 for _ in classes.rglob("*.class"))
    return classes, count


def reobfuscate(classes):
    """srg-имена членов для рантайма. Два прохода: ART не переименовывает члены, если класс маппится «в себя»."""
    mappings = BUILD / "mappings"
    mappings.mkdir(exist_ok=True)
    env = dict(os.environ, MC_SERVER_DIR=str(SERVER), MAP_OUT=str(mappings))
    sh(["python3", str(ROOT / "tools/rift/realbuild/make_mappings.py")], env=env)

    jar = BUILD / "classes.jar"
    with zipfile.ZipFile(jar, "w", zipfile.ZIP_DEFLATED) as z:
        for p in classes.rglob("*"):
            if p.is_file():
                z.write(p, p.relative_to(classes).as_posix())

    stage1, stage2 = BUILD / "reobf1.jar", BUILD / "reobf2.jar"
    sh([str(JAVA), "-jar", str(ART), "--input", str(jar), "--output", str(stage1),
        "--map", str(mappings / "reobf_pass1.tsrg"), "--lib", str(DEVJAR), "--threads", "1"])
    sh([str(JAVA), "-jar", str(ART), "--input", str(stage1), "--output", str(stage2),
        "--map", str(mappings / "reobf_pass2.tsrg"), "--threads", "1"])
    return stage2


def assemble(contents_jar, props, out: Path):
    stage = BUILD / "stage"
    shutil.rmtree(stage, ignore_errors=True)
    stage.mkdir()
    with zipfile.ZipFile(contents_jar) as z:
        for info in z.infolist():
            if info.is_dir() or info.filename == "fernflower_abstract_parameter_names.txt":
                continue
            target = stage / info.filename
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(z.read(info))

    resources = MOD / "src/main/resources"
    for p in resources.rglob("*"):
        if p.is_file():
            target = stage / p.relative_to(resources)
            target.parent.mkdir(parents=True, exist_ok=True)
            text = p.read_text(encoding="utf-8") if p.suffix in (".toml", ".mcmeta", ".json") else None
            if p.name == "mods.toml":
                text = re.sub(r"\$\{(\w+)\}", lambda m: props.get(m.group(1), m.group(0)), text)
            if text is not None and "${" in text and p.name == "mods.toml":
                raise SystemExit("mods.toml: остались неподставленные плейсхолдеры")
            if text is not None:
                target.write_text(text, encoding="utf-8")
            else:
                shutil.copy2(p, target)

    out.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        for p in sorted(stage.rglob("*")):
            if p.is_file():
                z.write(p, p.relative_to(stage).as_posix())
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=None, help="путь к готовому jar")
    ap.add_argument("--skip-reobf", action="store_true", help="оставить mojmap-имена (не запустится в игре)")
    args = ap.parse_args()

    for needed, hint in ((JAVA, "pip install --break-system-packages jdk4py==17.0.9.2"),
                         (ECJ, "pip install --break-system-packages karellen-jdtls"),
                         (DEVJAR, "python3 tools/rift/realbuild/fetch_devjar.py"),
                         (ART, "python3 tools/rift/realbuild/fetch_server.py")):
        if not needed.exists():
            raise SystemExit(f"нет {needed}\n  -> {hint}")

    props = gradle_props()
    BUILD.mkdir(parents=True, exist_ok=True)
    argfile, nsrc = source_list()
    classes, nclass = compile_java(argfile)
    print(f"[compile] {nsrc} файлов -> {nclass} классов, ошибок 0")

    if args.skip_reobf:
        shutil.copy2(BUILD / "classes.jar" if (BUILD / "classes.jar").exists() else "",
                     BUILD / "unused.jar") if False else None
        with zipfile.ZipFile(BUILD / "classes.jar", "w", zipfile.ZIP_DEFLATED) as z:
            for p in classes.rglob("*"):
                if p.is_file():
                    z.write(p, p.relative_to(classes).as_posix())
        payload = BUILD / "classes.jar"
    else:
        payload = reobfuscate(classes)
        print("[reobf] члены переименованы в srg-имена")

    out = Path(args.out) if args.out else ROOT / "dist" / f"riftworld-{props.get('mod_version', 'dev')}.jar"
    assemble(payload, props, out)
    size = out.stat().st_size
    with zipfile.ZipFile(out) as z:
        n = len(z.namelist())
    print(f"[jar] {out} ({size / 1e6:.1f} MB, файлов {n})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
