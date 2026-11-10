#!/usr/bin/env python3
"""Generate v0.47.0 item models, 16x16 item textures, and selective emissive entity glow overlays."""
from __future__ import annotations

import json
from pathlib import Path
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "mod/src/main/resources/assets/riftworld"
ITEM_MODELS = ASSETS / "models/item"
ITEM_TEX = ASSETS / "textures/item"
ENT_TEX = ASSETS / "textures/entity"

ITEM_MODELS.mkdir(parents=True, exist_ok=True)
ITEM_TEX.mkdir(parents=True, exist_ok=True)
ENT_TEX.mkdir(parents=True, exist_ok=True)

FAUNA_IDS = [
    "porcelain_crane",
    "basalt_beetle",
    "ash_moth",
    "amethyst_fox",
    "sizo_leafwing",
    "azure_frog",
    "blossom_snail",
    "phantom_wolf",
    "crystal_gecko",
    "faceted_owl",
    "bone_raven",
    "coral_hermit_crab",
    "reef_seahorse",
    "ghost_squid",
    "silver_sea_serpent",
]

for fid in FAUNA_IDS:
    (ITEM_MODELS / f"{fid}_spawn_egg.json").write_text(
        '{"parent":"minecraft:item/template_spawn_egg"}\n', encoding="utf-8"
    )


def rgba(hex_str: str, a: int = 255) -> tuple[int, int, int, int]:
    h = hex_str.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def save_icon(name: str, grid: list[str], palette: dict[str, tuple[int, int, int, int]]) -> None:
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    assert len(grid) == 16, name
    for y, row in enumerate(grid):
        assert len(row) == 16, f"{name} row {y}: {len(row)}"
        for x, ch in enumerate(row):
            if ch != ".":
                px[x, y] = palette[ch]
    img.save(ITEM_TEX / f"{name}.png")
    (ITEM_MODELS / f"{name}.json").write_text(
        json.dumps(
            {"parent": "minecraft:item/generated", "textures": {"layer0": f"riftworld:item/{name}"}},
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )


# 1. reef_seahorse_bucket
save_icon(
    "reef_seahorse_bucket",
    [
        "......hhhh......",
        ".....h....h.....",
        "....h..ss..h....",
        "...bbbsSSsbbb...",
        "..bBwwwwSSsBwb..",
        "..bBwwwsSsswwb..",
        "..bBwwwwSSwwwb..",
        "..bBwwwsSSswwb..",
        "..bBmwwsS.swwb..",
        "..bBmmwss..mwb..",
        "...bBmwwwwmmb...",
        "...bBmmmmmmmb...",
        "....bBBBBBBb....",
        "....bBmmmmBb....",
        ".....bbbbbb.....",
        "................",
    ],
    {
        "h": rgba("#6d7682"),
        "b": rgba("#434b56"),
        "B": rgba("#aeb8c4"),
        "m": rgba("#7f8a98"),
        "w": rgba("#2fb7d6"),
        "s": rgba("#d86928"),
        "S": rgba("#ffd166"),
    },
)

# 2. porcelain_feather
save_icon(
    "porcelain_feather",
    [
        "...........gg...",
        "..........gGGg..",
        ".........gGccGg.",
        "........gGcCCcg.",
        ".......oPcCCpPo.",
        "......oPpCCppPo.",
        ".....oPppCppPo..",
        "....oPppgppPo...",
        "...oPppgpPo.....",
        "...oPpgpPo......",
        "..oPgpPo........",
        "..ogPo..........",
        "..gGo...........",
        ".gG.............",
        "g...............",
        "................",
    ],
    {
        "g": rgba("#9b7832"),
        "G": rgba("#e5c16c"),
        "c": rgba("#6ab8ab"),
        "C": rgba("#9ee2d5"),
        "o": rgba("#bdb8ae"),
        "p": rgba("#ebe7de"),
        "P": rgba("#fcfaf5"),
    },
)

# 3. basalt_scute
save_icon(
    "basalt_scute",
    [
        "................",
        ".....dddddd.....",
        "...ddBBBBBBdd...",
        "..dBBbbbbbbBBd..",
        "..dBbbmMMmbbBd..",
        ".dBbbmMFFMmbbBd.",
        ".dBbbmFYYFmbbBd.",
        ".dBbbbmFFmbbbBd.",
        ".dBbbbbmMbbbbBd.",
        "..dBbbmMmbbbBd..",
        "..dBBbmMbbbbBd..",
        "...ddBBBBBBdd...",
        ".....dddddd.....",
        "................",
        "................",
        "................",
    ],
    {
        "d": rgba("#16181d"),
        "B": rgba("#2b2f38"),
        "b": rgba("#3d434f"),
        "m": rgba("#8f2d14"),
        "M": rgba("#d9531e"),
        "F": rgba("#ff8c2b"),
        "Y": rgba("#ffd466"),
    },
)

# 4. ash_wing_dust
save_icon(
    "ash_wing_dust",
    [
        "................",
        ".......e........",
        ".....a.E..a.....",
        "....aAaaaAa.....",
        "...aAAeFEAAa....",
        "..aAAeFYYFeAa...",
        "..aAaaEFFeaAa...",
        ".aaAAaaeEaaAAa..",
        ".aAAAaAaaAaAAa..",
        "..aAAeEaAaeAAa..",
        "..aaAaEaaEaAaa..",
        "...aaaaAaaaaa...",
        ".....aaaaaa.....",
        "................",
        "................",
        "................",
    ],
    {
        "a": rgba("#2a2726"),
        "A": rgba("#4a4543"),
        "e": rgba("#a13b18"),
        "E": rgba("#e86824"),
        "F": rgba("#ff9d3b"),
        "Y": rgba("#ffe082"),
    },
)

# 5. phantom_fang
save_icon(
    "phantom_fang",
    [
        "................",
        "..........cc....",
        "........ccCCc...",
        ".......cCCWWCc..",
        "......cCWFFWWc..",
        ".....cCWFffWCc..",
        "....cCWFffWCc...",
        "....cCWFfWCc....",
        "...cCWFfWCc.....",
        "...cCWfWCc......",
        "...cCWWCc.......",
        "...ccCCc........",
        "....ccc.........",
        "....cc..........",
        "................",
        "................",
    ],
    {
        "c": rgba("#2b4f60", 215),
        "C": rgba("#57a6bd", 230),
        "W": rgba("#a4ecf7", 245),
        "f": rgba("#d6fbff", 255),
        "F": rgba("#f2ffff", 255),
    },
)

# 6. bone_pinion
save_icon(
    "bone_pinion",
    [
        "...........dd...",
        "..........dBBd..",
        ".........dBbbBd.",
        "........dBbbbbd.",
        ".......dBbbssbd.",
        "......dBbbsbbbd.",
        ".....dBbbsbbd...",
        "....dBbbsbbd....",
        "...dBbbsbd......",
        "...dBbsbd.......",
        "..dBbsbd........",
        "..dbsbd.........",
        "..dsd...........",
        ".ds.............",
        "s...............",
        "................",
    ],
    {
        "d": rgba("#24262b"),
        "s": rgba("#6e685f"),
        "b": rgba("#cfc7b8"),
        "B": rgba("#ede7da"),
    },
)

# 7. abyssal_ink_sac
save_icon(
    "abyssal_ink_sac",
    [
        "................",
        "......nnnn......",
        ".....nNNNNn.....",
        ".....nNCCNn.....",
        "......nNNn......",
        "....nnNNNNnn....",
        "...nNNiCCiNNn...",
        "..nNNiCGGCiiNn..",
        "..nNiCGFFGCiNn..",
        "..nNiCCGGCCiNn..",
        "..nNNiiCCiiNNn..",
        "...nNNNNNNNNn...",
        "....nnNNNNnn....",
        "......nnnn......",
        "................",
        "................",
    ],
    {
        "n": rgba("#0f172a"),
        "N": rgba("#1c2d4f"),
        "i": rgba("#235b7e"),
        "C": rgba("#33c3e8"),
        "G": rgba("#7bf4ff"),
        "F": rgba("#d8ffff"),
    },
)


def make_emissive_overlay(entity_id: str, mode: str) -> int:
    src = Image.open(ENT_TEX / f"{entity_id}.png").convert("RGBA")
    arr = np.array(src, dtype=np.float32)
    r, g, b, a = arr[..., 0], arr[..., 1], arr[..., 2], arr[..., 3]
    lum = np.maximum(np.maximum(r, g), b)
    sat = (lum - np.minimum(np.minimum(r, g), b)) / np.maximum(lum, 1.0)

    if mode == "fire":
        mask = (a > 20) & (r > 195) & (g > 70) & ((r - b) > 90) & (sat > 0.48)
    elif mode == "cyan":
        mask = (a > 20) & ((g > 185) | (b > 195)) & ((np.maximum(g, b) - r) > 55) & (lum > 195)
    elif mode == "abyss":
        cyan = (a > 20) & (b > 185) & (g > 150) & ((b - r) > 45)
        lavender_eye = (a > 20) & (r > 185) & (b > 210) & (lum > 210)
        mask = cyan | lavender_eye
    elif mode == "blossom":
        mask = (a > 20) & (r > 235) & (g > 195) & (b > 110) & ((r - b) > 55) & (lum > 235)
    else:
        raise ValueError(mode)

    out = np.zeros_like(arr, dtype=np.uint8)
    out[mask, 0] = np.clip(r[mask] * 1.05, 0, 255).astype(np.uint8)
    out[mask, 1] = np.clip(g[mask] * 1.05, 0, 255).astype(np.uint8)
    out[mask, 2] = np.clip(b[mask] * 1.05, 0, 255).astype(np.uint8)
    out[mask, 3] = 230
    Image.fromarray(out, mode="RGBA").save(ENT_TEX / f"{entity_id}_glow.png")
    return int(mask.sum())


glow_specs = {
    "basalt_beetle": "fire",
    "ash_moth": "fire",
    "bone_raven": "fire",
    "blossom_snail": "blossom",
    "phantom_wolf": "cyan",
    "faceted_owl": "cyan",
    "ghost_squid": "abyss",
    "silver_sea_serpent": "cyan",
}

for eid, mode in glow_specs.items():
    n_px = make_emissive_overlay(eid, mode)
    print(f"Generated {eid}_glow.png ({mode}): {n_px} emissive texels")
print("Generated 15 spawn egg JSONs and 7 item icons + JSONs.")
