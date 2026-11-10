#!/usr/bin/env python3
"""Проверка кадров из игры: в кадре должно быть видно МОБА, а не пустое небо.

v0.49.0: «дальние» кадры бумажного дракона оказались чистым градиентом неба —
клиент вычищал дракона из кадра (renderDistance был 16, а дракон стоял в 55
блоках). Кадр формально существовал, но не показывал ничего. Такой провал
не видно по размеру файла, поэтому нужен отдельный валидатор.

Проверка для каждого кадра:
  * кадр не одноцветный (небо/пустота отбрасываются);
  * в кадре есть пиксели, заметно отличающиеся от доминирующего цвета
    (значит, объект действительно нарисован).

Запуск:  python3 tools/rift/validate_v049_frames.py [каталог]
Выход:   0 — все кадры содержат объект, 1 — есть пустые кадры.
"""
from __future__ import annotations

import pathlib
import sys

try:
    from PIL import Image
except ImportError:  # pragma: no cover - зависит от окружения
    print("Нужен Pillow: pip install pillow")
    sys.exit(2)

# Кадры, где в кадре законно почти ничего нет (ночь, далёкий горизонт).
ALLOW_FLAT = {
    "frame-paper-dragon-far-a.png",
    "frame-paper-dragon-far-b.png",
}


def analyse(path: pathlib.Path) -> tuple[bool, str]:
    """Вернуть (годен ли кадр, описание)."""
    with Image.open(path) as im:
        rgb = im.convert("RGB")
        # Уменьшаем: детализация не нужна, а картинка становится быстрее.
        small = rgb.resize((160, 90), Image.BILINEAR)
        pixels = list(small.convert("RGB").tobytes())
        pixels = [tuple(pixels[i:i + 3]) for i in range(0, len(pixels), 3)]
    total = len(pixels)
    if total == 0:
        return False, "пустой файл"
    # Доминирующий цвет — «фон» кадра.
    counts: dict[tuple[int, int, int], int] = {}
    for p in pixels:
        key = (p[0] // 24, p[1] // 24, p[2] // 24)
        counts[key] = counts.get(key, 0) + 1
    bg = max(counts, key=counts.get)
    bg_count = counts[bg]
    # Пиксели, заметно отличающиеся от фона.
    distinct = 0
    for p in pixels:
        if max(abs(p[0] - bg[0] * 24), abs(p[1] - bg[1] * 24), abs(p[2] - bg[2] * 24)) > 26:
            distinct += 1
    ratio = distinct / total
    if bg_count / total > 0.995:
        return False, f"кадр одноцветный (фон {bg_count}/{total})"
    if ratio < 0.004:
        return False, f"объекта не видно: отличающихся пикселей {ratio:.4%}"
    return True, f"объект виден: {ratio:.2%} пикселей"


def main(argv: list[str]) -> int:
    root = pathlib.Path(argv[1]) if len(argv) > 1 else pathlib.Path("reports/ingame/0.49.0")
    if not root.is_dir():
        print(f"Каталог с кадрами не найден: {root}")
        return 2
    frames = sorted(root.glob("*.png"))
    if not frames:
        print(f"В {root} нет кадров")
        return 2
    bad: list[str] = []
    for frame in frames:
        if frame.name in ALLOW_FLAT:
            continue
        ok, note = analyse(frame)
        status = "ok " if ok else "BAD"
        print(f"{status} {frame.name}: {note}")
        if not ok:
            bad.append(frame.name)
    if bad:
        print("\nFAIL: в кадрах ничего не видно: " + ", ".join(bad))
        return 1
    print(f"\nOK: проверено {len(frames)} кадров — во всех виден объект.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
