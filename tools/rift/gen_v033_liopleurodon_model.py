#!/usr/bin/env python3
"""Совместимая точка входа: модель Лиоплевродона теперь собирается в v0.41.0.

Старый скрипт v0.33.0 рисовал 66 кубоидов и атлас 512². Чтобы случайный
запуск исторического генератора не откатывал новую модель, он просто
перенаправляет на tools/rift/gen_v041_liopleurodon_model.py.
"""
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import gen_v041_liopleurodon_model as hero  # noqa: E402


def main():
    print("[compat] gen_v033_liopleurodon_model.py → gen_v041_liopleurodon_model.py")
    hero.main()


if __name__ == "__main__":
    main()
