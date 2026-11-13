#!/usr/bin/env python3
"""Собирает одностраничный HTML-редактор брони.

Все данные (ванильные иконки 16×16 и 3D-модели) встраиваются в один
файл как base64 — поэтому страница открывается откуда угодно: с
GitHub, из архива релиза, с локального диска. Никакого сервера.

Запуск:  python3 tools/armor_editor/build_single.py
Результат: tools/armor_editor/riftworld-armor-editor.html
"""
from __future__ import annotations

import base64
import json
import pathlib

HERE = pathlib.Path(__file__).resolve().parent
DATA = HERE / "data"

SETS = ["leather", "netherite", "diamond"]
PARTS = ["helmet", "chestplate", "leggings", "boots"]


def b64(path: pathlib.Path) -> str:
    return base64.b64encode(path.read_bytes()).decode("ascii")


LAYERS = ["layer_1", "layer_2"]


def build_data() -> dict:
    """Данные для страницы: иконки и карты брони (PNG, base64) + 3D-модели (JSON)."""
    out = {"icons": {}, "models": {}}
    for s in SETS:
        for p in PARTS:
            png = DATA / f"{s}_{p}.png"
            if png.exists():
                out["icons"][f"{s}_{p}"] = b64(png)
        for L in LAYERS:
            png = DATA / f"{s}_{L}.png"
            if png.exists():
                out["icons"][f"{s}_{L}"] = b64(png)
    for p in PARTS:
        mj = DATA / f"model3d_{p}.json"
        if mj.exists():
            out["models"][p] = json.loads(mj.read_text(encoding="utf-8"))
    return out


def main() -> None:
    html = (HERE / "index.html").read_text(encoding="utf-8")
    js = (HERE / "editor.js").read_text(encoding="utf-8")
    pixels = (HERE / "pixels.js").read_text(encoding="utf-8")

    # Убираем внешние <script src=...> — всё пойдёт inline.
    html = html.replace('<script src="pixels.js"></script>\n', "")
    html = html.replace('<script src="editor.js"></script>\n', "")

    # editor.js сам берёт данные из ARMOR_DATA, если тот есть (одностраничная сборка),
    # иначе грузит файлы из data/ (обычная разработка). Поэтому init() править не нужно.

    payload = json.dumps(build_data(), ensure_ascii=False, separators=(",", ":"))

    # Вставляем данные и скрипты перед </body>.
    inject = (
        "<script>\n"
        "var ARMOR_DATA = " + payload + ";\n"
        "</script>\n"
        "<script>\n" + pixels + "\n</script>\n"
        "<script>\n" + js + "\n</script>\n"
        "</body>"
    )
    html = html.replace("</body>", inject, 1)

    out = HERE / "riftworld-armor-editor.html"
    out.write_text(html, encoding="utf-8")
    size_kb = out.stat().st_size / 1024
    print(f"OK: {out} ({size_kb:.0f} KB, картинок: {len(SETS) * (len(PARTS) + len(LAYERS))}, моделей: {len(PARTS)})")


if __name__ == "__main__":
    main()
