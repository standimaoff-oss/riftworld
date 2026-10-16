#!/usr/bin/env python3
"""Собирает команду запуска headless Forge-клиента 1.20.1 из version json установщика.
usage: ci_client_cmd.py <clientdir> [server_host] [server_port] > cmd.sh"""
import json
import os
import sys

cli = os.path.abspath(sys.argv[1])
host = sys.argv[2] if len(sys.argv) > 2 else "127.0.0.1"
port = sys.argv[3] if len(sys.argv) > 3 else "25565"

vdir = os.path.join(cli, "versions")
vj = None
for root, _, files in os.walk(vdir):
    for f in files:
        if f.endswith(".json"):
            vj = os.path.join(root, f)
d = json.load(open(vj))
ver = os.path.basename(vj)[:-5]

def lib_path(name):
    g, a, v = name.split(":")[:3]
    cls = None
    if "@" in v:
        v, cls = v.split("@", 1)
    fn = "%s-%s%s.jar" % (a, v, ("-" + cls) if cls else "")
    return os.path.join(cli, "libraries", *g.split("."), a, v, fn)

cp = []
for lib in d.get("libraries", []):
    p = lib_path(lib["name"])
    if os.path.exists(p):
        cp.append(p)
main_jar = os.path.join(vdir, ver, ver + ".jar")
if os.path.exists(main_jar):
    cp.append(main_jar)
classpath = ":".join(cp)

SUB = {
    "auth_player_name": "DevPlayer", "version_name": ver, "game_directory": cli,
    "assets_root": os.path.join(cli, "assets"), "assets_index_name": "1.20.1",
    "auth_uuid": "00000000-0000-0000-0000-000000000000", "auth_access_token": "0",
    "user_type": "legacy", "natives_directory": os.path.join(cli, "natives"),
    "library_directory": os.path.join(cli, "libraries"), "classpath_separator": ":",
    "classpath": classpath, "version_type": "forge", "primary_jar": main_jar,
    "launcher_name": "ci", "launcher_version": "1", "resolution_width": "1600",
    "resolution_height": "1000",
}

def sub(tok):
    inner = tok[2:-1]
    return SUB.get(inner, "")

args = ["java", "-Xmx2G", "-Driftworld.shotauto=1"]
for a in d.get("arguments", {}).get("jvm", []):
    if isinstance(a, dict):
        continue
    v = sub(a) if a.startswith("${") else a
    if v:
        args.append(v)
args.append(d.get("mainClass", "cpw.mods.bootstraplauncher.Launcher"))
for a in d.get("arguments", {}).get("game", []):
    if isinstance(a, dict):
        continue
    v = sub(a) if a.startswith("${") else a
    if v:
        args.append(v)
args += ["--server", host, "--port", port]
os.makedirs(os.path.join(cli, "natives"), exist_ok=True)
print("cd %s" % cli)
print(" ".join('"%s"' % a if (" " in a or a.startswith("-D")) else a for a in args))
