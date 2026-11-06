#!/usr/bin/env python3
"""Совместимая точка входа: модель Пегаса теперь собирается в v0.41.0.

Старый скрипт v0.34.0 давал 54 кубоида и атлас 256². Чтобы случайный запуск
исторического генератора не откатывал новую модель, он перенаправляет на
tools/rift/gen_v041_pegasus_model.py.
"""
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import gen_v041_pegasus_model as hero  # noqa: E402


def main():
    print("[compat] gen_v034_pegasus_model.py → gen_v041_pegasus_model.py")
    hero.main()


if __name__ == "__main__":
    main()
