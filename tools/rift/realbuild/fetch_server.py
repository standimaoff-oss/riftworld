#!/usr/bin/env python3
"""Downloads a real Forge 1.20.1 dedicated server out of a GitHub repo (only the files needed to boot).

Mojang and the Forge maven are unreachable from this sandbox, but some people commit their whole server
directory into git. We take only `libraries/` (jars + the Forge args file) and the launcher scripts.
"""
import base64
import json
import os
import sys
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor

DEFAULT_OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".cache", "mcserver")
# --help must not download anything, so flags are parsed before any work
_ARGS = [a for a in sys.argv[1:] if not a.startswith("-")]
if "-h" in sys.argv[1:] or "--help" in sys.argv[1:]:
    print(__doc__.strip())
    print(f"\n  REPO={DEFAULT_OUT}  (default output: {DEFAULT_OUT})")
    print("  positional: [owner/repo] [ref] [out-dir]")
    sys.exit(0)
REPO = _ARGS[0] if len(_ARGS) > 0 else "chisaato/BACraft-Server"
REF = _ARGS[1] if len(_ARGS) > 1 else "HEAD"
OUT = _ARGS[2] if len(_ARGS) > 2 else DEFAULT_OUT
TOKEN = os.popen("gh auth token").read().strip()

# a dev checkout only needs the Forge libraries; the world and the mods stay out
KEEP_PREFIX = ("libraries/",)
KEEP_EXACT = {"run.sh", "run.bat", "user_jvm_args.txt"}


def api(path: str) -> dict:
    req = urllib.request.Request(f"https://api.github.com/{path}",
                                 headers={"Authorization": f"Bearer {TOKEN}",
                                          "Accept": "application/vnd.github+json"})
    for attempt in range(5):
        try:
            with urllib.request.urlopen(req, timeout=120) as r:
                return json.load(r)
        except Exception as e:                                   # rate limit / transient
            if attempt == 4:
                raise
            time.sleep(2 + attempt * 3)
    return {}


def tree() -> list[dict]:
    ref = api(f"repos/{REPO}/git/trees/{REF}?recursive=1")
    return [t for t in ref["tree"] if t["type"] == "blob"]


def wanted(path: str) -> bool:
    if path.endswith(".cache"):
        return False
    return path.startswith(KEEP_PREFIX) or path in KEEP_EXACT


def grab(item: dict) -> tuple[str, int]:
    rel, sha, size = item["path"], item["sha"], item["size"]
    dest = os.path.join(OUT, rel)
    if os.path.exists(dest) and os.path.getsize(dest) == size:
        return rel, size
    blob = api(f"repos/{REPO}/git/blobs/{sha}")
    data = base64.b64decode(blob["content"])
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    with open(dest, "wb") as f:
        f.write(data)
    return rel, len(data)


def main() -> None:
    files = [t for t in tree() if wanted(t["path"])]
    total = sum(f["size"] for f in files)
    print(f"{REPO}: {len(files)} файлов, {total / 1048576:.1f} МБ -> {OUT}")
    done = 0
    with ThreadPoolExecutor(max_workers=6) as pool:
        for rel, size in pool.map(grab, files):
            done += size
            print(f"  {done / 1048576:6.1f}/{total / 1048576:.1f} МБ  {rel}")
    print("готово")


if __name__ == "__main__":
    main()
