#!/usr/bin/env python3
"""Builds official<->srg mappings for Minecraft 1.20.1 from the files the Forge server install ships.

`srv-...-mappings.txt`  — Mojang official mappings: official class -> obf class, and members with official-namespace
                          descriptors (dots!). The srg jar Forge runs uses those very official class names, so those
                          descriptors need no translation at all — only dots become slashes.
`mcp_config-...-mappings.txt` — TSRG2 obf -> srg. Its member keys are (obf member name, descriptor in the same
                          official namespace), which makes a direct (name, descriptor) join possible.

Obfuscated method names are heavily overloaded, so the join key must include the descriptor.
"""
import os
import re
import sys

MC = os.environ.get("MC_SERVER_DIR", "/tmp/mcserver")
OUT = os.environ.get("MAP_OUT", MC)          # куда положить готовые .tsrg
OFFICIAL = (f"{MC}/libraries/net/minecraft/server/1.20.1-20230612.114412/"
            f"server-1.20.1-20230612.114412-mappings.txt")
MCP = (f"{MC}/libraries/de/oceanlabs/mcp/mcp_config/1.20.1-20230612.114412/"
       f"mcp_config-1.20.1-20230612.114412-mappings.txt")

PRIM = {"void": "V", "boolean": "Z", "byte": "B", "char": "C", "short": "S", "int": "I", "long": "J",
        "float": "F", "double": "D"}


def to_desc(jtype: str) -> str:
    jtype = jtype.strip()
    dims = 0
    while jtype.endswith("[]"):
        dims += 1
        jtype = jtype[:-2]
    if jtype in PRIM:
        return "[" * dims + PRIM[jtype]
    return "[" * dims + "L" + jtype.replace(".", "/") + ";"


def parse_official():
    """-> (mojmap class -> obf class, mojmap class -> {(obf member, None, official desc|None): mojmap member})"""
    classes, members = {}, {}
    cur = None
    rx_class = re.compile(r"^(\S+) -> (\S+):$")
    for raw in open(OFFICIAL, encoding="utf-8"):
        line = raw.rstrip("\n")
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        if not line[0].isspace():
            m = rx_class.match(line)
            if m:
                cur = m.group(1).replace(".", "/")
                classes[cur] = m.group(2).replace(".", "/")
                members.setdefault(cur, {})
            continue
        if " -> " not in line or cur is None:
            continue
        left, _, obf = line.strip().rpartition(" -> ")
        left = re.sub(r"^\d+:\d+:", "", left)
        if "(" in left:                                   # method: RET name(ARGS)
            head, args = left.split("(", 1)
            name = head.split()[-1]
            if name in ("<init>", "<clinit>"):
                continue
            ret = head[: head.rfind(name)].strip()
            args = args.rsplit(")", 1)[0]
            desc = "(" + "".join(to_desc(a) for a in args.split(",") if a.strip()) + ")" + to_desc(ret)
            members[cur][(obf, None, desc)] = name
        else:                                             # field: TYPE name
            members[cur][(obf, None, None)] = left.split()[-1]
    return classes, members


def parse_mcp():
    """-> (obf class -> srg class, obf class -> {(obf member, None, official desc|None): srg member})"""
    classes, members = {}, {}
    cur = None
    for raw in open(MCP, encoding="utf-8"):
        line = raw.rstrip("\n")
        if not line.strip() or line.startswith("tsrg2"):
            continue
        if not line.startswith("\t"):
            p = line.split(" ")
            classes[p[0]] = p[1]
            cur = members.setdefault(p[0], {})
            continue
        p = line.strip().split(" ")
        if cur is None or len(p) < 2 or p[0] in ("<init>", "<clinit>"):
            continue
        if p[1].startswith("("):
            if len(p) < 3:
                continue
            cur[(p[0], None, p[1])] = p[2]
        else:
            cur[(p[0], None, None)] = p[1]
    return classes, members


def rename_desc(desc, cls_map):
    """Rewrites every `Lpkg/Cls;` inside a descriptor through cls_map."""
    out, i = [], 0
    while i < len(desc):
        if desc[i] == "L":
            j = desc.index(";", i)
            out.append("L" + cls_map.get(desc[i + 1:j], desc[i + 1:j]) + ";")
            i = j + 1
        else:
            out.append(desc[i])
            i += 1
    return "".join(out)


def build():
    off_c, off_m = parse_official()
    mcp_c, mcp_m = parse_mcp()
    obf2moj = {obf: moj for moj, obf in off_c.items()}

    moj2srg, pairs_by_cls = {}, {}
    joined = missing = 0
    for moj_cls, obf_cls in off_c.items():
        srg_cls = mcp_c.get(obf_cls)
        if not srg_cls:
            continue
        moj2srg[moj_cls] = srg_cls
        pairs = []
        for (obf_name, _, desc), moj_name in off_m.get(moj_cls, {}).items():
            # mcp_config пишет дескрипторы в обф-пространстве, а сам srg-jar — в официальном
            key = rename_desc(desc, off_c) if desc else None
            srg_name = mcp_m.get(obf_cls, {}).get((obf_name, None, key))
            if not srg_name:
                if desc:
                    missing += 1
                continue
            joined += 1
            pairs.append((srg_name, moj_name, desc))  # в маппинг идёт официальный дескриптор
        pairs_by_cls[moj_cls] = pairs
    print(f"классов: {len(moj2srg)} | членов: {joined} | методов без пары: {missing}")
    return moj2srg, pairs_by_cls


def write(path, moj2srg, pairs_by_cls, direction):
    """direction: 'srg2moj' (compile pass 1) or 'moj2srg' (reobfuscation pass 1)."""
    with open(path, "w", encoding="utf-8") as f:
        f.write("tsrg2 left right\n")
        for moj_cls in sorted(moj2srg):
            f.write(f"{moj_cls} {moj2srg[moj_cls]}\n")
            for srg_name, moj_name, desc in pairs_by_cls[moj_cls]:
                left, right = ((srg_name, moj_name) if direction == "srg2moj" else (moj_name, srg_name))
                f.write(f"\t{left} {desc} {right}\n" if desc else f"\t{left} {right}\n")


def main():
    if not (os.path.exists(OFFICIAL) and os.path.exists(MCP)):
        sys.exit("нет файлов маппингов в /tmp/mcserver/libraries")
    moj2srg, pairs_by_cls = build()
    os.makedirs(OUT, exist_ok=True)
    write(f"{OUT}/compile_pass1.tsrg", moj2srg, pairs_by_cls, "srg2moj")
    write(f"{OUT}/reobf_pass1.tsrg", moj2srg, pairs_by_cls, "moj2srg")
    for name in ("compile_pass2.tsrg", "reobf_pass2.tsrg"):
        with open(f"{OUT}/{name}", "w", encoding="utf-8") as f:
            f.write("tsrg2 left right\n")
            for moj_cls, srg_cls in sorted(moj2srg.items()):
                f.write(f"{srg_cls} {moj_cls}\n")
    with open(f"{OUT}/member_map.tsv", "w", encoding="utf-8") as f:
        for moj_cls, pairs in pairs_by_cls.items():
            for srg_name, moj_name, _desc in pairs:
                f.write(f"{moj_cls}\t{srg_name}\t{moj_name}\n")
    print("готово: compile_pass1/2.tsrg, reobf_pass1/2.tsrg, member_map.tsv")


if __name__ == "__main__":
    main()
