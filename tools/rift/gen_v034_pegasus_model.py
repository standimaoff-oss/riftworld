#!/usr/bin/env python3
"""
Обёртка совместимости: модель Пегаса с v0.40.0 собирает `gen_v040_pegasus_model.py`.

Файл оставлен, чтобы старые команды/скрипты (`python3 tools/rift/gen_v034_pegasus_model.py`)
не перезаписывали модель устаревшей геометрией: раньше здесь лежала версия v0.39.11 с
крыльями-«досками» и жирафьей шеей.
"""
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from gen_v040_pegasus_model import main  # noqa: E402

if __name__ == "__main__":
    main()
