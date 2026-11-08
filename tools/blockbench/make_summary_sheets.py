#!/usr/bin/env python3
"""Create two numbered contact sheets from approved concepts and BB photographs.

No generated artwork and no recreated models: all input files already exist in
this repository. Identical 3x5 positions/IDs make the sheets easy to compare;
concepts without a model retain an explicitly labelled empty slot.
Requires ImageMagick 6 and DejaVu Sans.
"""
import json
import re
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CONCEPTS = ROOT / 'reports/biome_mob_concepts'
OUTPUT = ROOT / 'reports/mob_summary'
REPORT = ROOT / 'reports/blockbench_first_batch/generation_report.json'
PHOTOS = ROOT / 'reports/blockbench_first_batch/photos/photos_report.json'
FONT = 'DejaVu-Sans'


def run(*cmd):
    subprocess.run([str(x) for x in cmd], check=True)


def make():
    headings = dict(re.findall(r'^## (\d\d) — (.+)$', (CONCEPTS / 'README.md').read_text(), re.MULTILINE))
    headings = {n: s.split(' · ')[0] for n, s in headings.items()}
    concept_files = {}
    for n in range(1, 16):
        matches = list(CONCEPTS.glob(f'{n:02d}_*.png'))
        if len(matches) != 1 or f'{n:02d}' not in headings:
            raise ValueError(f'Expected exactly one approved concept and heading for {n:02d}: {matches}')
        concept_files[n] = matches[0]

    photos = {p['id']: ROOT / p['file'] for p in json.loads(PHOTOS.read_text())['photos']}
    models = {}
    for item in json.loads(REPORT.read_text())['models']:
        n = int(Path(item['stats']['concept']).name[:2])
        if n in models or item['id'] not in photos or not photos[item['id']].is_file():
            raise ValueError(f'Duplicate or missing photo for {n:02d}: {item["id"]}')
        models[n] = photos[item['id']]
    OUTPUT.mkdir(parents=True, exist_ok=True)

    with tempfile.TemporaryDirectory(prefix='riftworld-summary-') as temp:
        temp = Path(temp)
        for kind in ('references', 'models'):
            tiles = []
            for n in range(1, 16):
                tile = temp / f'{kind}_{n:02d}.png'
                title = f'{n:02d} — {headings[f"{n:02d}"]}'
                if kind == 'models' and n not in models:
                    run('convert', '-size', '940x492', 'xc:#f2f4f8', '-fill', '#606d7c',
                        '-font', FONT, '-pointsize', '28', '-gravity', 'center',
                        '-annotate', '+0+0', 'Модель пока не создана',
                        '-background', '#ffffff', '-gravity', 'south', '-splice', '0x60',
                        '-fill', '#202a35', '-font', FONT, '-pointsize', '23',
                        '-annotate', '+0+15', title, tile)
                else:
                    src = concept_files[n] if kind == 'references' else models[n]
                    cmd = ['convert', src]
                    if kind == 'models':
                        cmd += ['-fuzz', '4%', '-trim', '+repage', '-bordercolor', '#f2f4f8', '-border', '16']
                    cmd += ['-resize', '920x482', '-background', '#ffffff', '-gravity', 'center',
                            '-extent', '940x492', '-gravity', 'south', '-splice', '0x60',
                            '-fill', '#202a35', '-font', FONT, '-pointsize', '23',
                            '-annotate', '+0+15', title, tile]
                    run(*cmd)
                tiles.append(tile)
            montage = temp / f'{kind}_grid.png'
            run('montage', *tiles, '-background', '#e9edf3', '-tile', '3x5', '-geometry', '+8+8', montage)
            title = 'ВСЕ 15 РЕФЕРЕНСОВ' if kind == 'references' else 'ВСЕ 11 МОДЕЛЕЙ BLOCKBENCH'
            subtitle = 'Концепты, не скриншоты игры' if kind == 'references' else 'Позиции 07–10: модели ещё не созданы'
            file = OUTPUT / ('01_all_references.png' if kind == 'references' else '02_all_models.png')
            run('convert', montage, '-background', '#e9edf3', '-gravity', 'north', '-splice', '0x100',
                '-font', FONT, '-fill', '#202a35', '-pointsize', '37', '-annotate', '+0+16', title,
                '-fill', '#5c6672', '-pointsize', '23', '-annotate', '+0+64', subtitle,
                '-depth', '8', file)
            print(file.relative_to(ROOT))


if __name__ == '__main__':
    make()
