#!/usr/bin/env python3
"""Проверка переработанных моделей Лиоплевродона и Пегаса.

Проверяет не «цифры из README», а сами файлы: игровую геометрию, PNG-атлас,
Blockbench-проекты и Bedrock-geometry экспорт. Зависимостей, кроме stdlib, нет.
"""
import base64
import json
import pathlib
import struct
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
ASSETS = ROOT / "mod/src/main/resources/assets/riftworld"
REPORTS = ROOT / "reports/hero_models"
NAMES = ("liopleurodon", "pegasus")


def png_size(data: bytes):
    assert data[:8] == b"\x89PNG\r\n\x1a\n", "не PNG"
    w, h = struct.unpack(">II", data[16:24])
    return w, h


def face_rects(u, v, w, h, d):
    return {
        "up": (u + d, v, w, d),
        "down": (u + d + w, v, w, d),
        "east": (u, v + d, d, h),
        "north": (u + d, v + d, w, h),
        "west": (u + d + w, v + d, d, h),
        "south": (u + d + w + d, v + d, w, h),
    }


def check(name):
    geo_path = ASSETS / "geo" / f"{name}.json"
    tex_path = ASSETS / "textures" / "entity" / f"{name}.png"
    geo = json.loads(geo_path.read_text(encoding="utf-8"))
    a = geo["texture_width"]
    assert a == geo["texture_height"] == 1024, "атлас должен быть 1024²"
    assert geo["render_scale"] > 0, "render_scale должен быть положительным"
    assert png_size(tex_path.read_bytes()) == (a, a), "размер PNG не совпадает с атласом"

    used = bytearray(a * a)
    names = set()
    cubes = 0
    for b in geo["bones"]:
        assert b["name"] not in names, f"дубликат кости {b['name']}"
        names.add(b["name"])
        assert len(b["pivot"]) == 3, "pivot должен быть [x,y,z]"
        for c in b["cubes"]:
            cubes += 1
            o, s, uv = c["origin"], c["size"], c["uv"]
            assert len(o) == len(s) == 3 and len(uv) == 2
            assert all(int(x) == x and x >= 1 for x in s), f"размер кубоида: {s}"
            w, h, d = map(int, s)
            u, v = map(int, uv)
            for face, (fx, fy, fw, fh) in face_rects(u, v, w, h, d).items():
                assert 0 <= fx and 0 <= fy and fx + fw <= a and fy + fh <= a, \
                    f"UV за атласом: {name}/{b['name']}/{face}"
                for yy in range(fy, fy + fh):
                    for xx in range(fx, fx + fw):
                        i = yy * a + xx
                        assert not used[i], f"наложение UV: {name} в ({xx},{yy})"
                        used[i] = 1
    for b in geo["bones"]:
        if "parent" in b:
            assert b["parent"] in names, f"неизвестный родитель {b['parent']}"

    bb_path = REPORTS / f"{name}.bbmodel"
    bb = json.loads(bb_path.read_text(encoding="utf-8"))
    assert bb["meta"]["format_version"] == "4.10"
    assert bb["meta"]["model_format"] == "free"
    assert len(bb["elements"]) == cubes, "в .bbmodel не все кубоиды"
    assert len(bb["textures"]) == 1, ".bbmodel должен содержать встроенную текстуру"
    src = bb["textures"][0]["source"]
    assert src.startswith("data:image/png;base64,"), "текстура не встроена"
    assert png_size(base64.b64decode(src.split(",", 1)[1])) == (a, a)
    # иерархия костей: все UUID элементов должны встречаться в outliner
    def collect(node, out):
        if isinstance(node, str):
            out.add(node)
        elif isinstance(node, dict):
            out.add(node.get("uuid"))
            for child in node.get("children", []):
                collect(child, out)
    refs = set()
    for node in bb["outliner"]:
        collect(node, refs)
    element_ids = {e["uuid"] for e in bb["elements"]}
    assert element_ids <= refs, "в outliner отсутствуют элементы"

    bedrock_path = REPORTS / f"{name}.blockbench.geo.json"
    bedrock = json.loads(bedrock_path.read_text(encoding="utf-8"))
    g = bedrock["minecraft:geometry"][0]
    assert g["description"]["texture_width"] == a
    assert sum(len(b["cubes"]) for b in g["bones"]) == cubes
    stats = json.loads((REPORTS / f"{name}_stats.json").read_text(encoding="utf-8"))
    assert stats["cubes"] == cubes and stats["bones"] == len(geo["bones"])
    return {"name": name, "bones": len(geo["bones"]), "cubes": cubes,
            "atlas": a, "uv_px": sum(used), "render_scale": geo["render_scale"]}


def main():
    for name in NAMES:
        info = check(name)
        print(f"OK {name}: {info['bones']} костей, {info['cubes']} кубоидов, "
              f"атлас {info['atlas']}², занято UV {info['uv_px']} px, "
              f"render_scale={info['render_scale']}")
    print("OK: игровые geo/PNG, .bbmodel и Blockbench geometry согласованы")
    return 0


if __name__ == "__main__":
    sys.exit(main())
