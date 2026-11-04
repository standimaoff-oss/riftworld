#!/usr/bin/env python3
"""
Обёртка совместимости: модель Морока с v0.40.0 собирает `gen_v040_morok_model.py`.

Раньше здесь лежала версия v0.39.13: серый «мешок» с рёбрами-конфетти и угольками глаз
размером в два пикселя (кадр 152 прогонов #404–#423).
"""
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from gen_v040_morok_model import main  # noqa: E402

if __name__ == "__main__":
    main()
