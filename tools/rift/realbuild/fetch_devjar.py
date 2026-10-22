#!/usr/bin/env python3
"""Достаёт dev-артефакт Forge 1.20.1 (клиент+сервер, официальные имена, AT применены) из git-кэша ForgeGradle.

Зачем: Mojang и maven.minecraftforge.net из песочницы недоступны, а для компиляции клиентских классов мода
нужен jar с клиентом. Некоторые люди коммитят в git весь кэш ForgeGradle — там лежит готовый
`forge-<ver>_mapped_official_1.20.1.jar`.

Usage:
    python3 tools/rift/realbuild/fetch_devjar.py [--repo owner/name] [--out /tmp/mcserver/forge-mapped-official.jar]
    python3 tools/rift/realbuild/fetch_devjar.py --search      # найти репозитории-кандидаты
"""
import argparse
import base64
import json
import os
import subprocess
import sys

DEFAULT_REPO = "Cruderdragon835/the_medical_devices"
PATH_TEMPLATE = ("forge-1.20.1-47.3.0-mdk/DotGradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/"
                 "1.20.1-47.3.0_mapped_official_1.20.1/forge-1.20.1-47.3.0_mapped_official_1.20.1.jar")


def gh_api(path):
    res = subprocess.run(["gh", "api", path], capture_output=True, text=True)
    if res.returncode != 0:
        raise SystemExit(f"gh api {path}: {res.stderr.strip()[:400]}")
    return json.loads(res.stdout)


def search():
    """Показывает репозитории, где лежит собранный dev-jar Forge (поиск по имени файла)."""
    out = subprocess.run(["gh", "api", "-X", "GET", "search/code",
                          "-f", "q=filename:forge-1.20.1-47.3.0_mapped_official_1.20.1.jar",
                          "-f", "per_page=10", "--jq",
                          '.items[] | "\\(.repository.full_name) \\(.path)"'], capture_output=True, text=True)
    print(out.stdout or out.stderr)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", default=DEFAULT_REPO)
    ap.add_argument("--path", default=PATH_TEMPLATE)
    ap.add_argument("--out", default="/tmp/mcserver/forge-mapped-official.jar")
    ap.add_argument("--search", action="store_true")
    args = ap.parse_args()

    if args.search:
        return search()

    blob = gh_api(f"repos/{args.repo}/contents/{args.path}")
    data = base64.b64decode(gh_api(f"repos/{args.repo}/git/blobs/{blob['sha']}")["content"])
    out = args.out
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, "wb") as f:
        f.write(data)
    print(f"{out}: {len(data) / 1e6:.1f} МБ")


if __name__ == "__main__":
    sys.exit(main())
