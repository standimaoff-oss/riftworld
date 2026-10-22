#!/usr/bin/env python3
"""Готовит и запускает настоящий выделенный сервер Forge 1.20.1 в песочнице.

Что чинится по дороге (всё это реальные ограничения песочницы, а не мода):
  * JDK нет — берём JRE 17 из `jdk4py==17.0.9.2` (Java 25 из свежих колёс Forge 47 не подходит);
  * в JRE нет модуля `jdk.dynalink`, поэтому Forge-овский JS-coremod падает с FindException. Лечится удалением
    `META-INF/coremods.json` из `forge-...-universal.jar` — это выключает только JS-coremods самого Forge
    (сам Forge и мод от этого не страдают);
  * Mojang-сессии недоступны -> `online-mode=false`, версия-чекер просто пишет WARN;
  * включаем RCON, чтобы «играть» командами из скрипта и проверять мир headless.

Usage:
    python3 tools/rift/realbuild/run_server.py --prepare          # только подготовить каталог
    python3 tools/rift/realbuild/run_server.py --start            # подготовить и запустить (logs/latest.log)
"""
import argparse
import os
import re
import shutil
import subprocess
import sys
import time
import zipfile
from pathlib import Path

SERVER = Path(os.environ.get("MC_SERVER_DIR", "/tmp/mcserver"))
JAVA = Path(os.environ.get("JAVA", "/usr/local/lib/python3.11/dist-packages/jdk4py/java-runtime/bin/java"))
PROPS = {
    "online-mode": "false",
    "level-seed": "424242",
    "view-distance": "4",
    "simulation-distance": "4",
    "max-tick-time": "-1",
    "motd": "Rift",
    "enable-rcon": "true",
    "rcon.password": "rift",
    "rcon.port": "25575",
    "spawn-protection": "0",
    "sync-chunk-writes": "false",
}


def universal_jar():
    hits = sorted(SERVER.glob("libraries/net/minecraftforge/forge/*/forge-*-universal.jar"))
    return hits[-1] if hits else None


def strip_js_coremods():
    """Убирает META-INF/coremods.json (JS-coremods Forge) — иначе старт падает на отсутствии jdk.dynalink."""
    jar = universal_jar()
    if jar is None:
        raise SystemExit("не найден forge-*-universal.jar — сначала fetch_server.py")
    with zipfile.ZipFile(jar) as z:
        names = z.namelist()
        if "META-INF/coremods.json" not in names:
            return "уже вычищен"
        keep = {n: z.read(n) for n in names if n != "META-INF/coremods.json"}
    backup = jar.with_suffix(".jar.orig")
    if not backup.exists():
        shutil.copy2(jar, backup)
    with zipfile.ZipFile(jar, "w", zipfile.ZIP_DEFLATED) as z:
        for name, data in keep.items():
            z.writestr(name, data)
    return "coremods.json удалён"


def read_props():
    path = SERVER / "server.properties"
    props = {}
    if path.exists():
        for line in path.read_text(encoding="utf-8").splitlines():
            if "=" in line and not line.lstrip().startswith("#"):
                k, v = line.split("=", 1)
                props[k.strip()] = v
    return props


def prepare():
    if not JAVA.exists():
        raise SystemExit(f"нет java: {JAVA}\n  -> pip install --break-system-packages jdk4py==17.0.9.2")
    if not (SERVER / "libraries").exists():
        raise SystemExit(f"нет {SERVER}/libraries\n  -> python3 tools/rift/realbuild/fetch_server.py")
    (SERVER / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    props = read_props()
    props.update(PROPS)
    (SERVER / "server.properties").write_text(
        "\n".join(f"{k}={v}" for k, v in props.items()) + "\n", encoding="utf-8")
    (SERVER / "mods").mkdir(exist_ok=True)
    return strip_js_coremods()


def args_file():
    hits = sorted(SERVER.glob("libraries/net/minecraftforge/forge/*/unix_args.txt"))
    if not hits:
        raise SystemExit("не найден unix_args.txt — установка сервера неполная")
    return hits[-1]


def fresh_world():
    """Пересоздать мир: сохранённые биомы/чанки — это ДАННЫЕ, они не обновляются вместе с кодом генератора."""
    world = SERVER / "world"
    if not world.exists():
        return "мир ещё не создан"
    stamp = time.strftime("%Y%m%d-%H%M%S")
    backup = SERVER / f"world-old-{stamp}"
    world.rename(backup)
    return f"старый мир отложен в {backup.name}"


def start(mod_jar=None, wait_done=180, new_world=False):
    if new_world:
        print("[world]", fresh_world())
    if mod_jar:
        target = SERVER / "mods" / Path(mod_jar).name
        shutil.copy2(mod_jar, target)
        # an older riftworld jar left in mods/ would win the mod-id clash and the new classes would never load
        # ВАЖНО: только сам мод, аддон riftworld_dt-*.jar — это другой мод и его трогать нельзя
        stale = [j for j in (SERVER / "mods").glob("riftworld-*.jar") if j != target]
        stale += [j for j in (SERVER / "mods").glob("riftworld_live*.jar") if j != target]
        for j in stale:
            j.unlink()
            print(f"[mods] убран старый {j.name} (иначе Forge грузит его вместо нового)")
        print(f"[mods] {target}")
    log = SERVER / "run.log"
    handled = open(log, "w", encoding="utf-8")
    proc = subprocess.Popen([str(JAVA), "-Xms512M", "-Xmx1400M", f"@{args_file()}", "nogui"],
                            cwd=SERVER, stdout=handled, stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL)
    print(f"[server] pid {proc.pid}, лог {log}")
    deadline = time.time() + wait_done
    while time.time() < deadline:
        if proc.poll() is not None:
            raise SystemExit(f"сервер упал с кодом {proc.returncode}, смотри {log}")
        text = log.read_text(encoding="utf-8", errors="replace")
        if re.search(r"Done \([\d.]+s\)!", text):
            print("[server] " + re.search(r"Done \([\d.]+s\)!.*", text).group(0))
            return proc
        time.sleep(2)
    raise SystemExit(f"сервер не дошёл до «Done» за {wait_done} с, смотри {log}")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--prepare", action="store_true", help="только подготовить каталог")
    ap.add_argument("--start", action="store_true", help="запустить сервер и дождаться «Done»")
    ap.add_argument("--mod", default=None, help="путь к собранному jar мода")
    ap.add_argument("--fresh-world", action="store_true",
                    help="отложить world/ в сторону: биомы и чанки в сохранении не обновляются вместе с генератором")
    args = ap.parse_args()

    print("[prepare]", prepare())
    if args.start:
        proc = start(args.mod, new_world=args.fresh_world)
        print("[server] работает, порт 25565. Ctrl-C/stop_process для остановки.")
        try:
            proc.wait()
        except KeyboardInterrupt:
            proc.terminate()
    return 0


if __name__ == "__main__":
    sys.exit(main())
