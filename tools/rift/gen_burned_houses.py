#!/usr/bin/env python3
"""
Generates 6 authentic burned, shattered, smoke-plumed village house .nbt templates
(data/riftworld/structures/ruins/burned_house_0..5.nbt) directly from the White City's
small house templates (house_f, house_a, fisher_house, house_h, hunter_house, forge),
for the Burned Destroyed Villages in Ashen Lands (riftworld:ashen_lands).
"""
import gzip
import io
import math
import os
import random
import struct
import sys

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, SCRIPT_DIR)
from nbtio import write_template

ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, "..", ".."))
CITY_DIR = os.path.join(ROOT, "mod", "src", "main", "resources", "data", "riftworld", "structures", "city")
RUINS_DIR = os.path.join(ROOT, "mod", "src", "main", "resources", "data", "riftworld", "structures", "ruins")


def read_nbt_gz(path):
    data = gzip.open(path, "rb").read()
    buf = io.BytesIO(data)

    def rstr():
        ln = struct.unpack(">H", buf.read(2))[0]
        return buf.read(ln).decode("utf-8")

    def rpay(t):
        if t == 1:
            return struct.unpack(">b", buf.read(1))[0]
        if t == 2:
            return struct.unpack(">h", buf.read(2))[0]
        if t == 3:
            return struct.unpack(">i", buf.read(4))[0]
        if t == 4:
            return struct.unpack(">q", buf.read(8))[0]
        if t == 5:
            return struct.unpack(">f", buf.read(4))[0]
        if t == 6:
            return struct.unpack(">d", buf.read(8))[0]
        if t == 7:
            ln = struct.unpack(">i", buf.read(4))[0]
            return list(buf.read(ln))
        if t == 8:
            return rstr()
        if t == 9:
            et, ln = struct.unpack(">bi", buf.read(5))
            return [rpay(et) for _ in range(ln)]
        if t == 10:
            out = {}
            while True:
                tb = buf.read(1)
                if not tb or tb[0] == 0:
                    break
                k = rstr()
                out[k] = rpay(tb[0])
            return out
        if t == 11:
            ln = struct.unpack(">i", buf.read(4))[0]
            return [struct.unpack(">i", buf.read(4))[0] for _ in range(ln)]
        if t == 12:
            ln = struct.unpack(">i", buf.read(4))[0]
            return [struct.unpack(">q", buf.read(8))[0] for _ in range(ln)]
        raise ValueError(f"Unknown NBT tag {t}")

    t0 = buf.read(1)[0]
    assert t0 == 10
    _ = rstr()
    return rpay(10)


SAFE_SOLIDS = {
    "riftworld:elisar_plaster",
    "riftworld:elisar_bricks",
    "riftworld:elisar_bricks_mossy",
    "riftworld:elisar_stone",
    "riftworld:elisar_stone_mossy",
    "riftworld:elisar_cobble",
    "riftworld:timber_frame_cross",
    "riftworld:timber_frame_post",
    "riftworld:sizo_planks",
    "riftworld:ambra_planks",
    "riftworld:thatch",
    "riftworld:roof_red",
    "riftworld:roof_blue",
    "riftworld:sizo_log",
    "riftworld:ambra_log",
}

STAIR_SLAB_PREFIXES = (
    "riftworld:roof_red_stairs",
    "riftworld:roof_blue_stairs",
    "riftworld:thatch_stairs",
    "riftworld:sizo_planks_stairs",
    "riftworld:elisar_bricks_stairs",
    "riftworld:elisar_cobble_stairs",
    "riftworld:roof_red_slab",
    "riftworld:roof_blue_slab",
    "riftworld:thatch_slab",
    "riftworld:sizo_planks_slab",
    "riftworld:ambra_planks_slab",
    "riftworld:elisar_cobble_slab",
)


def burn_template(src_name, out_name, seed):
    rng = random.Random(seed)
    nbt = read_nbt_gz(os.path.join(CITY_DIR, f"{src_name}.nbt"))
    W, H, D = nbt["size"]
    pal = nbt["palette"]

    # Collapse blast center on one of the upper corners so half the roof & upper wall are blown open
    bx = rng.uniform(W * 0.20, W * 0.80)
    bz = rng.uniform(D * 0.20, D * 0.75)
    by = H * rng.uniform(0.58, 0.78)
    blast_r = max(W, D) * rng.uniform(0.46, 0.62)

    B = {}
    NBT = {}

    for blk in nbt["blocks"]:
        x, y, z = blk["pos"]
        st = pal[blk["state"]]
        nm = st["Name"]
        props = dict(st.get("Properties", {}))

        if nm == "minecraft:air":
            continue

        # Strip fragile/multiblock city props, doors, windows, lanterns so zero orphaned parts remain
        if nm not in SAFE_SOLIDS and not nm.startswith(STAIR_SLAB_PREFIXES) and nm != "riftworld:elisar_fence":
            continue

        # Collapse carving: upper roof and blasted corner crumble away cleanly
        dist = math.sqrt((x - bx) ** 2 + ((y - by) * 1.15) ** 2 + (z - bz) ** 2)
        jitter = rng.uniform(-0.9, 0.9)
        if y >= 1 and dist + jitter < blast_r:
            # Leave occasional sticking charred rafters even inside blast zone!
            if ("log" in nm or "timber_frame_post" in nm) and y <= 4 and rng.random() < 0.45:
                B[(x, y, z)] = ("riftworld:charred_log", (("axis", props.get("axis", "y")),))
            continue

        # Also ragged roof holes elsewhere on y >= 4
        if y >= 4 and rng.random() < (0.38 if y >= 6 else 0.22):
            continue

        # Transform surviving blocks into burned/scorched materials
        if nm in ("riftworld:elisar_plaster", "riftworld:elisar_bricks", "riftworld:elisar_bricks_mossy",
                  "riftworld:elisar_stone", "riftworld:elisar_stone_mossy", "riftworld:elisar_cobble"):
            r = rng.random()
            if r < 0.44:
                B[(x, y, z)] = ("riftworld:scorched_stone", ())
            elif r < 0.74:
                B[(x, y, z)] = ("riftworld:castle_bricks_cracked", ())
            elif r < 0.90:
                B[(x, y, z)] = ("minecraft:blackstone", ())
            else:
                B[(x, y, z)] = ("riftworld:elisar_bricks", ())  # rare soot-stained remnant of original white house!
        elif nm in ("riftworld:timber_frame_cross", "riftworld:timber_frame_post", "riftworld:sizo_log", "riftworld:ambra_log"):
            ax = props.get("axis", "y")
            B[(x, y, z)] = ("riftworld:charred_log", (("axis", ax),))
        elif nm in ("riftworld:sizo_planks", "riftworld:ambra_planks", "riftworld:thatch", "riftworld:roof_red", "riftworld:roof_blue"):
            if y == 0:
                B[(x, y, z)] = ("riftworld:burnt_soil" if rng.random() < 0.45 else "riftworld:dark_planks", ())
            else:
                B[(x, y, z)] = ("riftworld:dark_planks" if rng.random() < 0.65 else "riftworld:charred_log",
                                (("axis", "x"),) if rng.random() >= 0.65 else ())
                if B[(x, y, z)][0] == "riftworld:dark_planks":
                    B[(x, y, z)] = ("riftworld:dark_planks", ())
        elif nm == "riftworld:elisar_fence":
            if rng.random() < 0.65:
                fprops = tuple(sorted((k, props.get(k, "false")) for k in ("east", "north", "south", "waterlogged", "west")))
                B[(x, y, z)] = ("riftworld:dark_fence", fprops)
        elif nm.endswith("_stairs"):
            use_stone = "bricks" in nm or "cobble" in nm or rng.random() < 0.35
            target = "riftworld:castle_bricks_stairs" if use_stone else "riftworld:dark_planks_stairs"
            sprops = tuple(sorted((k, props[k]) for k in ("facing", "half", "shape", "waterlogged") if k in props))
            B[(x, y, z)] = (target, sprops)
        elif nm.endswith("_slab"):
            use_stone = "cobble" in nm or rng.random() < 0.35
            target = "riftworld:castle_bricks_slab" if use_stone else "riftworld:dark_planks_slab"
            sprops = tuple(sorted((k, props[k]) for k in ("type", "waterlogged") if k in props))
            B[(x, y, z)] = (target, sprops)

    # Ensure solid scorched floor at y=0 inside footprint + scatter ash, debris, embers, and 1-2 smoking campfires at y=1!
    interior_spots = []
    for x in range(1, W - 1):
        for z in range(1, D - 1):
            if (x, 0, z) in B:
                if (x, 1, z) not in B and (x, 2, z) not in B:
                    interior_spots.append((x, z))
                    rv = rng.random()
                    if rv < 0.22:
                        B[(x, 1, z)] = ("riftworld:debris", ())
                    elif rv < 0.34:
                        B[(x, 1, z)] = ("riftworld:ash_block", ())
                    elif rv < 0.40:
                        B[(x, 0, z)] = ("riftworld:magma_meander", ())
                    elif rv < 0.44:
                        B[(x, 1, z)] = ("riftworld:castle_cobweb", ())

    rng.shuffle(interior_spots)
    fires = 0
    for fx, fz in interior_spots:
        # Place smoking campfire where sky above is open or high enough
        blocked = any((fx, yy, fz) in B for yy in range(1, min(H, 6)))
        if not blocked:
            B[(fx, 0, fz)] = ("riftworld:scorched_stone", ())
            B[(fx, 1, fz)] = ("minecraft:campfire", (
                ("facing", "south"),
                ("lit", "true"),
                ("signal_fire", "false"),
                ("waterlogged", "false"),
            ))
            fires += 1
            if fires >= 2:
                break
    if fires == 0 and interior_spots:
        fx, fz = interior_spots[0]
        for yy in range(1, H):
            B.pop((fx, yy, fz), None)
        B[(fx, 0, fz)] = ("riftworld:scorched_stone", ())
        B[(fx, 1, fz)] = ("minecraft:campfire", (
            ("facing", "south"),
            ("lit", "true"),
            ("signal_fire", "false"),
            ("waterlogged", "false"),
        ))

    os.makedirs(RUINS_DIR, exist_ok=True)
    out_path = os.path.join(RUINS_DIR, f"{out_name}.nbt")
    nb_cnt, pal_cnt = write_template(out_path, (W, H, D), B, NBT)
    return W, H, D, nb_cnt, pal_cnt


def main():
    sources = [
        ("house_f", "burned_house_0", 9101),
        ("house_a", "burned_house_1", 9102),
        ("fisher_house", "burned_house_2", 9103),
        ("house_h", "burned_house_3", 9104),
        ("hunter_house", "burned_house_4", 9105),
        ("forge", "burned_house_5", 9106),
    ]
    for src, dst, sd in sources:
        W, H, D, nb, pl = burn_template(src, dst, sd)
        print(f"Generated {dst}.nbt from {src}.nbt -> size=({W},{H},{D}), blocks={nb}, palette={pl}")


if __name__ == "__main__":
    main()
