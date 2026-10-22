#!/usr/bin/env python3
"""Полный «настоящий» прогон: собрать мод -> поднять сервер Forge -> поиграть через RCON -> остановить -> отчёт.

Сервер запускается как дочерний процесс, поэтому stdin у него всегда открыт: закрытие stdin Minecraft
трактует как команду `stop`, и сервер молча умирает посреди теста (на это легко наступить).

Usage:
    python3 tools/rift/realbuild/run_live.py [--jar dist/riftworld-<v>.jar] [--log-dir reports/rev8_playtest]
    python3 tools/rift/realbuild/run_live.py --no-playtest      # просто поднять и остановить
    python3 tools/rift/realbuild/run_live.py --keep-world       # не пересоздавать мир (быстрее, но биомы старые)

Мир по умолчанию пересоздаётся: сгенерированные биомы и чанки хранятся в сохранении и НЕ обновляются вместе с
кодом генератора, поэтому старая карта легко «доказывает» то, чего в моде уже нет (и наоборот).
"""
import argparse
import os
import shutil
import subprocess
import sys
import time
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
SERVER = Path(os.environ.get("MC_SERVER_DIR", "/tmp/mcserver"))
JAVA = Path(os.environ.get("JAVA", "/usr/local/lib/python3.11/dist-packages/jdk4py/java-runtime/bin/java"))


def args_file():
    hits = sorted(SERVER.glob("libraries/net/minecraftforge/forge/*/unix_args.txt"))
    if not hits:
        raise SystemExit("нет unix_args.txt — сначала fetch_server.py и run_server.py --prepare")
    return hits[-1]


def start_server(jar):
    target = SERVER / "mods" / Path(jar).name
    target.parent.mkdir(exist_ok=True)
    shutil.copy2(jar, target)
    log = SERVER / "live.log"
    handle = open(log, "w", encoding="utf-8")
    proc = subprocess.Popen([str(JAVA), "-Xms512M", "-Xmx1400M", f"@{args_file()}", "nogui"],
                            cwd=SERVER, stdout=handle, stderr=subprocess.STDOUT, stdin=subprocess.PIPE)
    print(f"[сервер] pid {proc.pid}, мод {target.name}, лог {log}")

    import re
    deadline = time.time() + 300
    while time.time() < deadline:
        if proc.poll() is not None:
            print(log.read_text(encoding="utf-8", errors="replace")[-4000:])
            raise SystemExit(f"сервер упал с кодом {proc.returncode}")
        text = log.read_text(encoding="utf-8", errors="replace")
        m = re.search(r"Done \([\d.]+s\)!", text)
        if m:
            print(f"[сервер] {m.group(0)}")
            return proc, log
        time.sleep(2)
    proc.kill()
    raise SystemExit("сервер не дошёл до «Done» за 5 минут")


def stop_server(proc):
    if proc.poll() is not None:
        return
    try:
        proc.stdin.write(b"stop\n")
        proc.stdin.flush()
    except (BrokenPipeError, ValueError):
        pass
    try:
        proc.wait(timeout=180)
    except subprocess.TimeoutExpired:
        proc.kill()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--jar", default=None)
    ap.add_argument("--log-dir", default=str(ROOT / "reports/rev8_playtest"))
    ap.add_argument("--no-playtest", action="store_true")
    ap.add_argument("--keep-world", action="store_true", help="не пересоздавать world/ (биомы останутся старыми)")
    args = ap.parse_args()

    jar = args.jar or str(ROOT / "dist" / "riftworld.jar")
    if not args.keep_world:
        world = SERVER / "world"
        if world.exists():
            backup = SERVER / ("world-old-" + time.strftime("%Y%m%d-%H%M%S"))
            world.rename(backup)
            print(f"[мир] старый мир отложен в {backup.name}: биомы в сохранении не обновляются вместе с кодом")
    if not Path(jar).exists():
        build = HERE / "build_jar.py"
        subprocess.run([sys.executable, str(build), "--out", jar], check=True)

    with zipfile.ZipFile(jar) as z:
        names = z.namelist()
    print(f"[jar] {Path(jar).name}: {len(names)} файлов, классов {sum(n.endswith('.class') for n in names)}")

    proc, log = start_server(jar)
    rc = 0
    try:
        if not args.no_playtest:
            Path(args.log_dir).mkdir(parents=True, exist_ok=True)
            res = subprocess.run([sys.executable, str(HERE / "playtest.py"),
                                  "--json", str(Path(args.log_dir) / "playtest.json")])
            rc = res.returncode
    finally:
        stop_server(proc)

    out = Path(args.log_dir)
    out.mkdir(parents=True, exist_ok=True)
    if log.exists():
        shutil.copy2(log, out / "server_run.log")
    print(f"[отчёт] логи: {out/'server_run.log'}")
    return rc


if __name__ == "__main__":
    sys.exit(main())
