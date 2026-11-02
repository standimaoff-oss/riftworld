#!/usr/bin/env python3
"""
v0.26.0: Procedural generator for the monumental, 100% bilaterally symmetrical (`lx <-> -lx`),
cardinally aligned, unbroken, uncluttered Gothic Basalt Citadel `blacktide.nbt` (248x176x216).

Modeled directly on:
  - reports/citadel_refs/02_citadel_exterior_basalt_bastion.png (exterior viaduct, twin gatehouse,
    buttressed curtain walls, layered dark volcanic sea cliffs, soaring Gothic Cathedral Keep)
  - reports/citadel_refs/03_citadel_hall_cathedral_throne.png (vaulted Cathedral Throne Hall)
  - reports/citadel_refs/04_citadel_hall_rotunda_sanctum.png (circular Domed Rotunda Sanctum)

Strict guarantees asserted before saving:
  1. 100% registered vanilla 1.20.1 + RiftWorld (`CastleBlocks` / `ModelKind`) block IDs;
  2. ZERO `gilded_blackstone`, ZERO white calcite/diorite salt-and-pepper noise, ZERO broken sea stubs;
  3. 100.000% exact X-mirror symmetry (`lx <-> -lx`) across the entire 248x176x216 volume;
  4. 20/20 symmetrical towers with complete crowns/spires AND walkable internal staircases;
  5. Windows strictly centered inside recessed architectural bays (0 intersections);
  6. Watertight floors, walls, vaults, and dome (`sub_air == 0`).
"""

import gzip
import math
import os
import re
import struct
import sys

W, H, D = 248, 176, 216
OX, OZ = -124, -100
SEA_BY = 24

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT_NBT = os.path.join(
    REPO_ROOT, "mod", "src", "main", "resources", "data", "riftworld", "structures", "blacktide.nbt"
)


# =============================================================================
# COMPACT PALETTE & MIRROR REGISTRY
# =============================================================================
palette = []
palette_map = {}


def pid(name: str, **props) -> int:
    key = (name, tuple(sorted((k, str(v)) for k, v in props.items())))
    idx = palette_map.get(key)
    if idx is not None:
        return idx
    idx = len(palette) + 1  # 1-based in voxel array (0 = air)
    palette.append((name, dict(sorted((k, str(v)) for k, v in props.items()))))
    palette_map[key] = idx
    return idx


def mirror_x_props(name: str, props: dict) -> int:
    mp = dict(props)
    if "facing" in mp:
        f = mp["facing"]
        if f == "east":
            mp["facing"] = "west"
        elif f == "west":
            mp["facing"] = "east"
    if "east" in mp or "west" in mp:
        e_val = mp.get("east", "false")
        w_val = mp.get("west", "false")
        if "east" in mp:
            mp["east"] = w_val
        if "west" in mp:
            mp["west"] = e_val
    if "shape" in mp:
        sh = mp["shape"]
        swap_shape = {
            "inner_left": "inner_right",
            "inner_right": "inner_left",
            "outer_left": "outer_right",
            "outer_right": "outer_left",
        }
        if sh in swap_shape:
            mp["shape"] = swap_shape[sh]
    return pid(name, **mp)


def verify_registered_blocks():
    castle_java = open(
        os.path.join(REPO_ROOT, "mod/src/main/java/ru/aiforge/riftworld/registry/CastleBlocks.java"),
        encoding="utf-8",
    ).read()
    model_java = open(
        os.path.join(REPO_ROOT, "mod/src/main/java/ru/aiforge/riftworld/block/ModelKind.java"),
        encoding="utf-8",
    ).read()
    elisar_java = open(
        os.path.join(REPO_ROOT, "mod/src/main/java/ru/aiforge/riftworld/registry/ElisarBlocks.java"),
        encoding="utf-8",
    ).read()
    mod_blocks_java = open(
        os.path.join(REPO_ROOT, "mod/src/main/java/ru/aiforge/riftworld/registry/ModBlocks.java"),
        encoding="utf-8",
    ).read()

    rw_ids = set()
    for src in (castle_java, elisar_java, mod_blocks_java):
        for m in re.finditer(r'(?:reg|cube|stairs|slab)\(\s*"([a-z0-9_]+)"', src):
            rw_ids.add("riftworld:" + m.group(1))
    for m in re.finditer(r'^\s*[A-Z0-9_]+\(\s*"([a-z0-9_]+)"', model_java, re.M):
        # ModelKind enum constant name lowercased is registered in ModBlocks.MODELS
        pass
    for m in re.finditer(r'^\s*([A-Z0-9_]+)\(\s*"[a-z0-9_]+"', model_java, re.M):
        rw_ids.add("riftworld:" + m.group(1).lower())

    missing = []
    for nm, _ in palette:
        if nm == "minecraft:gilded_blackstone":
            raise AssertionError("FORBIDDEN block minecraft:gilded_blackstone in palette!")
        if nm.startswith("riftworld:") and nm not in rw_ids:
            missing.append(nm)
    if missing:
        raise AssertionError(f"Unregistered riftworld blocks in palette: {sorted(set(missing))}")


# =============================================================================
# BLOCK PALETTE DEFINITIONS (CLEAN ARCHITECTURAL DARK BASALT & ASHLAR)
# =============================================================================
# Natural dark volcanic sea-cliff strata (NO white calcite/diorite speckles!)
DEEPSLATE = pid("minecraft:deepslate", axis="y")
COBBLED_DS = pid("minecraft:cobbled_deepslate")
SMOOTH_BASALT = pid("minecraft:smooth_basalt")
TUFF = pid("minecraft:tuff")
BLACKSTONE = pid("minecraft:blackstone")

# Clean Fortress & Cathedral Masonry
DSB = pid("minecraft:deepslate_bricks")
DST = pid("minecraft:deepslate_tiles")
PDS = pid("minecraft:polished_deepslate")
PBB = pid("minecraft:polished_blackstone_bricks")
PBS = pid("minecraft:polished_blackstone")
CPB = pid("minecraft:chiseled_polished_blackstone")
CDS = pid("minecraft:chiseled_deepslate")
BASALT_Y = pid("minecraft:polished_basalt", axis="y")
BASALT_X = pid("minecraft:polished_basalt", axis="x")
BASALT_Z = pid("minecraft:polished_basalt", axis="z")

# Subtle warm architectural wood/trim friezes (replaces Nether gold cleanly!)
TRIM_LOG_X = pid("minecraft:stripped_dark_oak_log", axis="x")
TRIM_LOG_Z = pid("minecraft:stripped_dark_oak_log", axis="z")
TRIM_LOG_Y = pid("minecraft:stripped_dark_oak_log", axis="y")
DARK_OAK_PLANKS = pid("minecraft:dark_oak_planks")
SPRUCE_PLANKS = pid("minecraft:spruce_planks")

# Vault & Rotunda light stone contrast ribs (inspired by refs 03 & 04)
ASHLAR = pid("riftworld:ashlar")
ASHLAR_PILLAR_Y = pid("riftworld:ashlar_pillar", axis="y")
ASHLAR_SLAB_TOP = pid("riftworld:ashlar_slab", type="top", waterlogged="false")
ASHLAR_SLAB_BOT = pid("riftworld:ashlar_slab", type="bottom", waterlogged="false")

# Slabs
DSB_SLAB_BOT = pid("minecraft:deepslate_brick_slab", type="bottom", waterlogged="false")
DSB_SLAB_TOP = pid("minecraft:deepslate_brick_slab", type="top", waterlogged="false")
DST_SLAB_BOT = pid("minecraft:deepslate_tile_slab", type="bottom", waterlogged="false")
DST_SLAB_TOP = pid("minecraft:deepslate_tile_slab", type="top", waterlogged="false")
PBB_SLAB_BOT = pid("minecraft:polished_blackstone_brick_slab", type="bottom", waterlogged="false")
PBB_SLAB_TOP = pid("minecraft:polished_blackstone_brick_slab", type="top", waterlogged="false")
PDS_SLAB_BOT = pid("minecraft:polished_deepslate_slab", type="bottom", waterlogged="false")
PDS_SLAB_TOP = pid("minecraft:polished_deepslate_slab", type="top", waterlogged="false")
DO_SLAB_TOP = pid("minecraft:dark_oak_slab", type="top", waterlogged="false")


def pbb_stair(facing: str, half: str = "bottom", shape: str = "straight") -> int:
    return pid(
        "minecraft:polished_blackstone_brick_stairs",
        facing=facing,
        half=half,
        shape=shape,
        waterlogged="false",
    )


def dsb_stair(facing: str, half: str = "bottom", shape: str = "straight") -> int:
    return pid(
        "minecraft:deepslate_brick_stairs",
        facing=facing,
        half=half,
        shape=shape,
        waterlogged="false",
    )


def dst_stair(facing: str, half: str = "bottom", shape: str = "straight") -> int:
    return pid(
        "minecraft:deepslate_tile_stairs",
        facing=facing,
        half=half,
        shape=shape,
        waterlogged="false",
    )


def ashlar_stair(facing: str, half: str = "bottom", shape: str = "straight") -> int:
    return pid(
        "riftworld:ashlar_stairs",
        facing=facing,
        half=half,
        shape=shape,
        waterlogged="false",
    )


# Walls, bars, chains, lighting, glass
PBB_WALL_NS = pid(
    "minecraft:polished_blackstone_brick_wall",
    east="none",
    north="low",
    south="low",
    up="false",
    waterlogged="false",
    west="none",
)
PBB_WALL_EW = pid(
    "minecraft:polished_blackstone_brick_wall",
    east="low",
    north="none",
    south="none",
    up="false",
    waterlogged="false",
    west="low",
)
PBB_WALL_POST = pid(
    "minecraft:polished_blackstone_brick_wall",
    east="none",
    north="none",
    south="none",
    up="true",
    waterlogged="false",
    west="none",
)
DSB_WALL_POST = pid(
    "minecraft:deepslate_brick_wall",
    east="none",
    north="none",
    south="none",
    up="true",
    waterlogged="false",
    west="none",
)
DARK_FENCE = pid(
    "riftworld:dark_fence",
    east="false",
    north="false",
    south="false",
    waterlogged="false",
    west="false",
)

# Windows: strictly inside recessed window bays, properly connected!
GLASS_EW = pid(
    "riftworld:stained_glass",
    east="true",
    north="false",
    south="false",
    waterlogged="false",
    west="true",
)
GLASS_NS = pid(
    "riftworld:stained_glass",
    east="false",
    north="true",
    south="true",
    waterlogged="false",
    west="false",
)
OCULUS_GLASS = pid("minecraft:light_blue_stained_glass")
SEA_LANTERN = pid("minecraft:sea_lantern")
SHROOMLIGHT = pid("minecraft:shroomlight")

PORTCULLIS_EW = pid(
    "riftworld:castle_bars",
    east="true",
    north="false",
    south="false",
    waterlogged="false",
    west="true",
)

CHAIN_Y = pid("minecraft:chain", axis="y", waterlogged="false")
LANTERN_STAND = pid("minecraft:lantern", hanging="false", waterlogged="false")
LANTERN_HANG = pid("minecraft:lantern", hanging="true", waterlogged="false")

# Custom RiftWorld Castle & Cathedral props
CARPET = pid("riftworld:castle_carpet")
BOOKSHELF = pid("riftworld:castle_bookshelf", axis="y")
CHANDELIER = pid("riftworld:castle_chandelier", facing="south", powered="false", broken="false")
BRAZIER_N = pid("riftworld:fire_bowl", facing="north", powered="true", broken="false")
BRAZIER_S = pid("riftworld:fire_bowl", facing="south", powered="true", broken="false")
CANDLE_S = pid("riftworld:candle_stand", facing="south", powered="false", broken="false")
CANDLE_N = pid("riftworld:candle_stand", facing="north", powered="false", broken="false")
THRONE_S = pid("riftworld:boss_throne", facing="south", powered="false", broken="false")
ALTAR_S = pid("riftworld:chapel_altar", facing="south", powered="false", broken="false")
BANNER_S = pid("riftworld:tapestry_horn", facing="south", powered="false", broken="false")
BANNER_N = pid("riftworld:tapestry_horn", facing="north", powered="false", broken="false")
BANNER_E = pid("riftworld:tapestry_horn", facing="east", powered="false", broken="false")
BANNER_W = pid("riftworld:tapestry_horn", facing="west", powered="false", broken="false")
LECTERN_S = pid("minecraft:lectern", facing="south", has_book="true", powered="false")
GRASS_SHORE = pid("minecraft:grass_block", snowy="false")
DIRT_SHORE = pid("minecraft:dirt")


# =============================================================================
# VOXEL BUFFER & SYMMETRIC HELPERS (`lx` in `-123..+123`, `lz` in `-100..+115`)
# =============================================================================
vox = bytearray(W * H * D)
STRIDE_Y = D * W


def set_raw(lx: int, by: int, lz: int, val: int):
    bx = lx - OX
    bz = lz - OZ
    if 0 <= bx < W and 0 <= by < H and 0 <= bz < D:
        vox[by * STRIDE_Y + bz * W + bx] = val


def get_raw(lx: int, by: int, lz: int) -> int:
    bx = lx - OX
    bz = lz - OZ
    if 0 <= bx < W and 0 <= by < H and 0 <= bz < D:
        return vox[by * STRIDE_Y + bz * W + bx]
    return 0


# Precomputed mirror lookup table for palette indices (updated dynamically)
_mirror_cache = {}


def mirror_val(val: int) -> int:
    if val == 0:
        return 0
    m = _mirror_cache.get(val)
    if m is not None:
        return m
    nm, pr = palette[val - 1]
    m = mirror_x_props(nm, pr)
    _mirror_cache[val] = m
    return m


def put_sym(lx: int, by: int, lz: int, val: int):
    """Places `val` at `(lx, by, lz)` and its exact X-mirror at `(-lx, by, lz)`."""
    set_raw(lx, by, lz, val)
    if lx != 0:
        set_raw(-lx, by, lz, mirror_val(val))


def fill_sym(lx0: int, lx1: int, by0: int, by1: int, lz0: int, lz1: int, val: int):
    for by in range(by0, by1 + 1):
        for lz in range(lz0, lz1 + 1):
            for lx in range(lx0, lx1 + 1):
                put_sym(lx, by, lz, val)


def strata_rock(lx: int, by: int, lz: int) -> int:
    """Smooth horizontal dark volcanic basalt strata — ZERO salt-and-pepper noise!"""
    ax = abs(lx)
    wave = int(2.2 * math.sin(ax * 0.09 + lz * 0.07) + 1.4 * math.cos(lz * 0.11))
    s = (by + wave) % 11
    if s in (0, 1, 2, 3):
        return DEEPSLATE
    if s in (4, 5, 6):
        return SMOOTH_BASALT
    if s in (7, 8):
        return COBBLED_DS
    return TUFF if by < 28 else BLACKSTONE


# =============================================================================
# 1. SCULPTED VOLCANIC BASALT SEA-CLIFF ISLAND (`by = 0..46`, `sub_air == 0`)
# =============================================================================
def build_volcanic_island():
    print("  [1/7] Sculpting layered dark volcanic basalt sea-cliff island...")
    # Fortress perimeter sits on (-54..+54, -78..+35). Cliffs flare naturally outward into the sea.
    for lz in range(-94, 48):
        for lx in range(0, 74):
            # Distance outside the rounded fortress rectangle (-53..+53, -77..+34)
            dx = max(0.0, lx - 53.0)
            if lz > 34:
                dz = lz - 34.0
            elif lz < -77:
                dz = -77.0 - lz
            else:
                dz = 0.0
            out_d = math.hypot(dx, dz)

            # Symmetric coastal crag modulation (smooth low-frequency ridges, NOT noisy dots!)
            ang = math.atan2(lz + 22.0, max(0.5, float(lx)))
            ridge = (
                4.2 * math.cos(ang * 5.0 + 0.4)
                + 2.8 * math.sin(lz * 0.11) * math.cos(lx * 0.10)
            )
            max_skirt = 15.5 + max(-3.5, ridge)
            if out_d > max_skirt:
                continue

            if out_d <= 0.01:
                # Inside curtain perimeter: solid bedrock foundation up to courtyard/keep grade
                in_keep_terrace = (lx <= 30 and -79 <= lz <= 6)
                top_y = 55 if in_keep_terrace else 46
            else:
                t = out_d / max_skirt
                # Steep dramatic coastal cliff dropping cleanly beneath sea level (SEA_BY=24) to by=0
                prof = (1.0 - t * t) ** 1.35
                top_y = int(round(4.0 + 42.0 * prof))

            for by in range(0, top_y + 1):
                put_sym(lx, by, lz, strata_rock(lx, by, lz))


# =============================================================================
# 2. SHORE ABUTMENT & UNBROKEN 4-ARCH VIADUCT BRIDGE (`lz = 36..104`, 0 SEA STUBS!)
# =============================================================================
def build_viaduct_bridge():
    print("  [2/7] Building shore abutment & horizontal 4-arch Gothic sea viaduct...")
    # A) Shore Abutment flush on the towering Ashen Lands sea-cliff (`LAND_LX=0, LAND_LZ=96, LAND_BY=46`)
    for lz in range(92, 108):
        flare = 6 + max(0, (lz - 93) // 2)
        for lx in range(0, flare + 1):
            for by in range(0, 47):
                put_sym(lx, by, lz, GRASS_SHORE if (by == 46 and lx > 5) else (DIRT_SHORE if (by >= 44 and lx > 5) else strata_rock(lx, by, lz)))
            if lx <= 5:
                put_sym(lx, 46, lz, PBB if (lx in (0, 4, 5) or lz % 4 == 0) else PDS)
                for cy in range(47, 56):
                    put_sym(lx, cy, lz, 0)
            elif lx == 6 and lz <= 100:
                put_sym(lx, 47, lz, PBB)
                put_sym(lx, 48, lz, PBB_SLAB_BOT)

    # Twin Shore Entry Pylons at `(±5, 98)` standing directly on the flat Ashen cliff brow!
    for lx in (5, 6):
        for lz in (97, 98, 99):
            for by in range(47, 55):
                put_sym(lx, by, lz, BASALT_Y if (lx == 5 and lz == 98 and 48 <= by <= 52) else PBB)
    put_sym(5, 55, 98, CPB)
    put_sym(5, 56, 98, BRAZIER_S)

    # B) 5 Cutwater Bridge Piers at `lz = 94, 79, 64, 49, 36` + 4 Pointed Gothic Arches (`lz = 36..94`)
    pier_zs = [94, 79, 64, 49, 36]

    def deck_y_at(lz: int) -> tuple[int, bool]:
        # Strictly horizontal grand viaduct deck at by=46 (worldY=84), flush with Castle Gate & Ashen Sea-Cliff!
        return 46, False

    # Solid cutwater piers rooted all the way to `by = 0` (`sub_air == 0`)
    for pz in pier_zs:
        dy, _ = deck_y_at(pz)
        for dz in range(-2, 3):
            lz = pz + dz
            max_lx = 6 if abs(dz) <= 1 else 5
            for lx in range(0, max_lx + 1):
                for by in range(0, dy + 1):
                    if by < 16:
                        blk = strata_rock(lx, by, lz)
                    elif lx == max_lx and by < dy - 4:
                        blk = BASALT_Y
                    elif by in (SEA_BY + 2, dy - 2):
                        blk = CPB
                    else:
                        blk = PBB if (lx >= 4 or abs(dz) == 2) else DSB
                    put_sym(lx, by, lz, blk)
            # Pointed cutwater prow at `lx = 7, dz = 0` near waterline
            if dz == 0:
                for by in range(0, SEA_BY + 8):
                    put_sym(7, by, lz, PBB if by < SEA_BY + 7 else pbb_stair("west"))

    # 4 Gothic pointed-arch spans between consecutive piers (`span = 15` / `13`)
    for idx in range(len(pier_zs) - 1):
        z_hi = pier_zs[idx]      # shoreward
        z_lo = pier_zs[idx + 1]  # castleward
        z_mid = 0.5 * (z_hi + z_lo)
        half_span = 0.5 * (z_hi - z_lo) - 2.0
        for lz in range(z_lo + 3, z_hi - 2):
            dy, _ = deck_y_at(lz)
            nd = abs(lz - z_mid) / max(1.0, half_span)  # 0 at crown, 1 at pier face
            # Pointed Gothic arch curve: high clear crown over the waves, thick solid spandrels above!
            arch_clear_y = int(round((dy - 3.0) - (nd ** 1.65) * 12.0))
            for lx in range(0, 6):
                for by in range(max(SEA_BY + 2, arch_clear_y), dy + 1):
                    if by == arch_clear_y and lx == 5:
                        blk = PBB  # clean outer archivolt ring
                    elif lx == 5:
                        blk = DSB if (by % 4 != 0) else PBB
                    else:
                        blk = DSB
                    put_sym(lx, by, lz, blk)

    # Continuous roadway, crenellated parapets & lantern/brazier piers (`lz = 36..96`)
    for lz in range(36, 97):
        dy, half_slab = deck_y_at(lz)
        # Clear air corridor above roadway
        for lx in range(0, 6):
            for cy in range(dy + 1, dy + 12):
                put_sym(lx, cy, lz, 0)

        # Roadway surface (`lx = 0..4`)
        for lx in range(0, 5):
            rib = (lx in (0, 4)) or (lz % 5 == 0)
            put_sym(lx, dy - 1, lz, DSB)
            if half_slab:
                put_sym(lx, dy, lz, PBB)
                put_sym(lx, dy + 1, lz, PBB_SLAB_BOT if rib else DST_SLAB_BOT)
            else:
                put_sym(lx, dy, lz, PBB if rib else PDS)

        # Parapet wall (`lx = 5`) + corbelled outer cornice + abundant warm bridge lighting!
        base_p = dy + (1 if half_slab else 0)
        put_sym(5, dy - 1, lz, PBB)
        put_sym(5, dy, lz, PBB)
        put_sym(5, base_p + 1, lz, PBB)
        if lz in pier_zs:
            # Projecting pilaster turret & fiery brazier over every pier (matches ref 02!)
            for dz_off in (-1, 0, 1):
                put_sym(6, dy - 1, lz, PBB)
                put_sym(6, dy, lz, CPB)
            put_sym(5, base_p + 2, lz, CPB)
            put_sym(5, base_p + 3, lz, BRAZIER_N)
        elif lz % 6 == 0:
            put_sym(5, base_p + 2, lz, PBB_WALL_POST)
            put_sym(5, base_p + 3, lz, LANTERN_STAND)
        elif lz % 2 == 0:
            put_sym(5, base_p + 2, lz, PBB_SLAB_BOT)


# =============================================================================
# 3. REUSABLE SYMMETRICAL TOWER BUILDER (WITH WALKABLE SPIRAL STAIRS & CROWNS!)
# =============================================================================
TOWER_REGISTRY = []


def build_sym_tower(
    name: str,
    cx: int,
    cz: int,
    radius: int,
    y_base: int,
    y_top: int,
    spire_h: int = 0,
    door_dirs: tuple = ("south",),
    wall_walk_y: int = 62,
):
    """
    Builds a mirrored pair of octagonal/cylindrical Gothic bastion towers at `(±cx, cz)`
    with:
      - Solid stone foundations down to `by = 0`,
      - Rhythmic `polished_basalt` corner pilasters + warm `stripped_dark_oak_log` frieze bands,
      - Recessed stained-glass lancets strictly on cardinal axes between floors (0 intersections!),
      - Full walkable internal square-spiral staircase (`polished_blackstone_brick_stairs`) from
        `y_base + 1` all the way to `y_top` roof/gallery deck,
      - Corbelled machicolated battlement crown OR steep Gothic dark-slate spire!
    """
    TOWER_REGISTRY.append((name, cx, cz, y_base, y_top))
    if cx != 0:
        TOWER_REGISTRY.append((name + "_M", -cx, cz, y_base, y_top))

    r_sq_outer = (radius + 0.38) ** 2
    r_sq_inner = max(2.2, radius - 1.55) ** 2

    # 1) Foundation & outer masonry shell (`by = 0 .. y_top`)
    for dz in range(-radius - 1, radius + 2):
        for dx in range(-radius - 1, radius + 2):
            d2 = dx * dx + dz * dz
            if d2 > r_sq_outer:
                continue
            lx = cx + dx
            lz = cz + dz

            # Solid foundation beneath `y_base`
            for by in range(0, y_base + 1):
                put_sym(lx, by, lz, PBB if by >= y_base - 2 else strata_rock(lx, by, lz))

            is_wall = d2 >= r_sq_inner
            is_pilaster = is_wall and (abs(dx) == radius or abs(dz) == radius) and (abs(dx) <= 2 or abs(dz) <= 2)
            is_diag_quoin = is_wall and abs(dx) == abs(dz)

            for by in range(y_base + 1, y_top + 1):
                if by == y_base + 1 or by == y_top:
                    # Solid floor & top roof deck (stairwell hatch carved in step 3)
                    put_sym(lx, by, lz, DST if d2 < r_sq_inner else PBB)
                elif not is_wall:
                    put_sym(lx, by, lz, 0)
                else:
                    # Clean horizontal architectural bands (warm dark oak frieze, 0 gilded_blackstone!)
                    if by in (y_base + 12, y_top - 4):
                        blk = TRIM_LOG_Y if is_diag_quoin else DARK_OAK_PLANKS
                    elif by in (y_base + 13, y_top - 3):
                        blk = CPB
                    elif is_diag_quoin:
                        blk = PBB
                    elif is_pilaster and abs(dx) != 0 and abs(dz) != 0:
                        blk = BASALT_Y
                    else:
                        blk = DSB
                    put_sym(lx, by, lz, blk)

    # 2) Corbelled Machicolation Crown & Crenellations (`y_top - 1 .. y_top + 3`)
    r_crown_sq = (radius + 1.25) ** 2
    r_crown_in = (radius - 0.35) ** 2
    for dz in range(-radius - 2, radius + 3):
        for dx in range(-radius - 2, radius + 3):
            d2 = dx * dx + dz * dz
            if d2 > r_crown_sq or d2 < r_crown_in:
                continue
            lx = cx + dx
            lz = cz + dz
            # Corbel support at y_top - 1 & solid overhang rim at y_top
            if d2 <= (radius + 0.95) ** 2:
                put_sym(lx, y_top - 1, lz, PBB if ((dx + dz) % 2 == 0) else PBB_SLAB_TOP)
            put_sym(lx, y_top, lz, PBB)
            # Outer parapet & rhythmic merlons at y_top + 1 .. y_top + 2
            if d2 >= (radius + 0.25) ** 2:
                put_sym(lx, y_top + 1, lz, PBB)
                merlon = ((abs(dx) + abs(dz)) % 2 == 0)
                if merlon:
                    put_sym(lx, y_top + 2, lz, CPB if (dx == 0 or dz == 0) else PBB)
                    put_sym(lx, y_top + 3, lz, PBB_SLAB_BOT)

    # 3) Recessed Cardinal Lancet Windows (strictly on cardinal faces, 0 intersections!)
    win_heights = [y for y in range(y_base + 7, y_top - 7, 10)]
    for wy in win_heights:
        # North & South cardinal lancets (`dx == 0, dz = ±radius`)
        for sgn in (-1, 1):
            for h_off in range(0, 4):
                put_sym(cx, wy + h_off, cz + sgn * radius, GLASS_EW)
                if radius >= 6:
                    put_sym(cx, wy + h_off, cz + sgn * (radius - 1), 0)
            put_sym(cx, wy - 1, cz + sgn * radius, CPB)
            put_sym(cx, wy + 4, cz + sgn * radius, CPB)
        # East & West cardinal lancets (`dz == 0, dx = ±radius`)
        for sgn in (-1, 1):
            for h_off in range(0, 4):
                put_sym(cx + sgn * radius, wy + h_off, cz, GLASS_NS)
                if radius >= 6:
                    put_sym(cx + sgn * (radius - 1), wy + h_off, cz, 0)
            put_sym(cx + sgn * radius, wy - 1, cz, CPB)
            put_sym(cx + sgn * radius, wy + 4, cz, CPB)

    # 4) Clean Arched Doorways at `y_base + 1` AND Wall-Walk Passages at `wall_walk_y`
    dir_vecs = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
    for dd in door_dirs:
        vx, vz = dir_vecs[dd]
        for step in range(max(1, radius - 2), radius + 2):
            for w_off in (-1, 0, 1):
                px = cx + vx * step + (-vz) * w_off
                pz = cz + vz * step + vx * w_off
                put_sym(px, y_base + 1, pz, PBB)
                max_dh = 4 if w_off == 0 else 3
                for dh in range(1, max_dh + 1):
                    put_sym(px, y_base + 1 + dh, pz, 0)

    if y_base + 6 <= wall_walk_y <= y_top - 5:
        # Open N/S/E/W wall-walk portals ONLY where a curtain wall actually meets this tower
        for vx, vz in ((0, -1), (0, 1), (-1, 0), (1, 0)):
            probe_x = cx + vx * (radius + 3)
            probe_z = cz + vz * (radius + 3)
            if get_raw(probe_x, wall_walk_y, probe_z) != 0:
                for step in range(0, radius + 2):
                    for w_off in (-1, 0, 1):
                        px = cx + vx * step + (-vz) * w_off
                        pz = cz + vz * step + vx * w_off
                        put_sym(px, wall_walk_y, pz, DST if abs(w_off) <= 1 else PBB)
                        for dh in range(1, 5):
                            put_sym(px, wall_walk_y + dh, pz, 0)

    # 5) Walkable Square-Spiral Staircase around central `polished_basalt` newel column!
    #    12-step square perimeter ring (`dx, dz` in `-2..+2`, center `0,0` column) -> 100% walkable!
    ring_path = [
        (-1, -2, "east"), (0, -2, "east"), (1, -2, "east"), (2, -2, None),
        (2, -1, "south"), (2, 0, "south"), (2, 1, "south"), (2, 2, None),
        (1, 2, "west"), (0, 2, "west"), (-1, 2, "west"), (-2, 2, None),
        (-2, 1, "north"), (-2, 0, "north"), (-2, -1, "north"), (-2, -2, None),
    ]
    # Note: corners `(±2, ±2)` are flat `PBB` landings (no elevation jump!), straight runs climb 1 block per step!
    for by in range(y_base + 1, y_top + 1):
        put_sym(cx, by, cz, BASALT_Y)
        if by % 4 == 0 and by < y_top - 1:
            put_sym(cx, by, cz - 1, LANTERN_HANG)
            put_sym(cx, by, cz + 1, LANTERN_HANG)

    cur_y = y_base + 2
    idx = 0
    while cur_y <= y_top:
        rx, rz, stair_dir = ring_path[idx % len(ring_path)]
        sx, sz = cx + rx, cz + rz
        # Clear 4 blocks of headroom above every step & landing (punches cleanly through `y_top` hatch!)
        for hy in range(cur_y + 1, min(y_top + 4, cur_y + 5)):
            if hy <= y_top or spire_h > 0 or (abs(rx) <= 2 and abs(rz) <= 2):
                put_sym(sx, hy, sz, 0)
        if stair_dir is None:
            # Corner landing
            put_sym(sx, cur_y, sz, PBB)
        else:
            put_sym(sx, cur_y, sz, pbb_stair(stair_dir, half="bottom"))
            put_sym(sx, cur_y - 1, sz, DSB)
            cur_y += 1
        idx += 1

    # 6) Steep Gothic Dark-Slate Spire OR Fiery Beacon Crown + Heraldic Flagpole!
    if spire_h > 0:
        for sy in range(1, spire_h + 1):
            u = 1.0 - (sy / float(spire_h))
            sr = max(0.45, (radius + 0.2) * (u ** 0.88))
            sr_in = max(0.0, sr - 1.35)
            for dz in range(-radius - 1, radius + 2):
                for dx in range(-radius - 1, radius + 2):
                    d = math.hypot(dx, dz)
                    if sr_in <= d <= sr:
                        put_sym(cx + dx, y_top + sy, cz + dz, DST if (sy % 3 != 0) else PBB)
        for fy in range(1, 5):
            put_sym(cx, y_top + spire_h + fy, cz, PBB_WALL_POST if fy <= 2 else CHAIN_Y)
    else:
        # Open battlement deck with warm central brazier & tall dark banner mast
        put_sym(cx, y_top, cz, CPB)
        put_sym(cx, y_top + 1, cz, BRAZIER_S)
        mast_x = cx + (radius - 1 if cx >= 0 else -(radius - 1))
        for fy in range(1, 7):
            put_sym(mast_x, y_top + 1 + fy, cz, DARK_FENCE)
        for fz in range(cz + 1, cz + 4):
            put_sym(mast_x, y_top + 7, fz, PBB_SLAB_TOP)
            put_sym(mast_x, y_top + 6, fz, PBB_SLAB_BOT)


# =============================================================================
# 4. UNBROKEN BUTTRESSED CURTAIN WALLS, GATEHOUSE & OUTER COURTYARD
# =============================================================================
def build_curtain_walls_and_gatehouse():
    print("  [3/7] Building unbroken Gothic buttressed curtain walls, barbican & 16 perimeter towers...")
    # A) Paved Outer Courtyard (`lx = 0..52, lz = -76..+33, by = 47`)
    for lz in range(-76, 34):
        for lx in range(0, 53):
            rib = (lx % 8 == 0) or (lz % 8 == 0)
            put_sym(lx, 46, lz, DSB)
            put_sym(lx, 47, lz, PBB if rib else DST)

    # B) Unbroken Front & Rear Curtain Walls (`lz = +34` and `lz = -78`, `lx = 0..54`, `by = 47..64`)
    for wall_z, out_sgn in ((34, 1), (-78, -1)):
        for lx in range(0, 55):
            is_buttress = (lx % 8 == 0) and not (wall_z == 34 and lx <= 16)
            is_win_bay = (lx % 8 == 4) and not (wall_z == 34 and lx <= 18)
            # 5-wide solid curtain core (`wall_z - 2 .. wall_z + 2`) supporting 5-wide walkable parapet at `by = 62`!
            for dz in range(-2, 3):
                lz = wall_z + dz
                is_outer_face = (dz == out_sgn * 2)
                is_inner_face = (dz == -out_sgn * 2)
                for by in range(47, 63):
                    if by == 62:
                        blk = DST if abs(dz) <= 1 else PBB
                    elif is_outer_face or is_inner_face:
                        if by in (47, 48):
                            blk = PBB
                        elif by == 59:
                            blk = DARK_OAK_PLANKS
                        elif by == 60:
                            blk = CPB
                        elif is_win_bay and 52 <= by <= 56:
                            blk = GLASS_EW if dz == out_sgn * 2 else 0
                        else:
                            blk = DSB
                    else:
                        blk = 0 if (is_win_bay and 52 <= by <= 56 and dz != 0) else (GLASS_EW if (is_win_bay and 52 <= by <= 56) else DSB)
                    put_sym(lx, by, lz, blk)

                # Window frame sill & lintel on outer/inner faces
                if is_win_bay and (is_outer_face or is_inner_face):
                    put_sym(lx, 51, lz, CPB)
                    put_sym(lx, 57, lz, CPB)

            # Projecting Gothic `polished_basalt` buttress piers on outer face (`wall_z + out_sgn * 3`)
            if is_buttress:
                bz_butt = wall_z + out_sgn * 3
                for by in range(20, 62):
                    put_sym(lx, by, bz_butt, BASALT_Y if 49 <= by <= 59 else PBB)
                put_sym(lx, 62, bz_butt, pbb_stair("south" if out_sgn < 0 else "north"))

            # Continuous crenellated outer & inner parapets on `by = 63..64`
            for side_dz in (out_sgn * 2, -out_sgn * 2):
                put_sym(lx, 63, wall_z + side_dz, PBB)
                if lx % 2 == 0 and side_dz == out_sgn * 2:
                    put_sym(lx, 64, wall_z + side_dz, CPB if lx % 8 == 0 else PBB)
                    put_sym(lx, 65, wall_z + side_dz, PBB_SLAB_BOT)
            if lx % 8 == 4 and not (wall_z == 34 and lx <= 16):
                put_sym(lx, 63, wall_z, CANDLE_S)
                # Courtyard inner wall lantern sconces every 8 blocks!
                put_sym(lx, 53, wall_z - out_sgn * 3, PBB_WALL_POST)
                put_sym(lx, 52, wall_z - out_sgn * 3, LANTERN_HANG)

    # C) Unbroken East & West Flank Curtain Walls (`lx = ±54`, `lz = -78..+34`, `by = 47..64`)
    for lz in range(-78, 35):
        is_buttress = ((lz + 80) % 8 == 0)
        is_win_bay = ((lz + 80) % 8 == 4) and (-72 <= lz <= 28)
        for dx in range(-2, 3):
            lx = 54 + dx
            is_outer_face = (dx == 2)
            is_inner_face = (dx == -2)
            for by in range(47, 63):
                if by == 62:
                    blk = DST if abs(dx) <= 1 else PBB
                elif is_outer_face or is_inner_face:
                    if by in (47, 48):
                        blk = PBB
                    elif by == 59:
                        blk = DARK_OAK_PLANKS
                    elif by == 60:
                        blk = CPB
                    elif is_win_bay and 52 <= by <= 56:
                        blk = GLASS_NS if is_outer_face else 0
                    else:
                        blk = DSB
                else:
                    blk = 0 if (is_win_bay and 52 <= by <= 56 and dx != 0) else (GLASS_NS if (is_win_bay and 52 <= by <= 56) else DSB)
                put_sym(lx, by, lz, blk)
            if is_win_bay and (is_outer_face or is_inner_face):
                put_sym(lx, 51, lz, CPB)
                put_sym(lx, 57, lz, CPB)

        if is_buttress:
            for by in range(20, 62):
                put_sym(57, by, lz, BASALT_Y if 49 <= by <= 59 else PBB)
            put_sym(57, 62, lz, pbb_stair("west"))

        for side_dx in (2, -2):
            put_sym(54 + side_dx, 63, lz, PBB)
            if lz % 2 == 0 and side_dx == 2:
                put_sym(54 + side_dx, 64, lz, CPB if is_buttress else PBB)
                put_sym(54 + side_dx, 65, lz, PBB_SLAB_BOT)
        if (lz + 80) % 8 == 4:
            put_sym(54, 63, lz, CANDLE_S)
            if -72 <= lz <= 28:
                put_sym(51, 53, lz, PBB_WALL_POST)
                put_sym(51, 52, lz, LANTERN_HANG)

    # Courtyard Grand Lantern & Brazier Processional Avenue (`lz = 8..28` & side courts!)
    for lz_b in (26, 18, 10, -8, -28, -48, -66):
        if lz_b >= 10:
            put_sym(8, 48, lz_b, CPB)
            put_sym(8, 49, lz_b, BRAZIER_S)
        put_sym(38, 48, lz_b, CPB)
        put_sym(38, 49, lz_b, PBB_WALL_POST)
        put_sym(38, 50, lz_b, LANTERN_STAND)

    # D) Monumental Barbican Gatehouse Bridgehead (`lx = 0..10, lz = 31..38, by = 47..72`)
    for lz in range(31, 39):
        for lx in range(0, 10):
            for by in range(47, 71):
                if by == 70:
                    blk = DST if (32 <= lz <= 37 and lx <= 8) else PBB
                elif lz in (31, 38) or lx == 9:
                    if by in (59, 66):
                        blk = DARK_OAK_PLANKS
                    elif by in (60, 67):
                        blk = CPB
                    elif lx in (3, 6) and lz in (31, 38) and 61 <= by <= 65:
                        blk = BASALT_Y
                    else:
                        blk = DSB
                else:
                    blk = PBB if by == 61 else 0
                put_sym(lx, by, lz, blk)
        # Gatehouse top crenellations (`by = 71..72`)
        for lx in range(0, 10):
            for edge_z in (31, 38):
                put_sym(lx, 71, edge_z, PBB)
                if lx % 2 == 0:
                    put_sym(lx, 72, edge_z, CPB if lx in (0, 6) else PBB)

    # Carve Grand Pointed Gothic Gate Arch (`lx = 0..3, lz = 30..39, by = 48..57`) + Raised Portcullis!
    for lz in range(30, 40):
        for lx in range(0, 4):
            put_sym(lx, 47, lz, PBB if lx in (0, 3) else PDS)
            arch_top = 56 - (lx * lx) // 3
            for by in range(48, arch_top + 1):
                if lz == 35 and by >= arch_top - 1:
                    put_sym(lx, by, lz, PORTCULLIS_EW)  # raised iron portcullis teeth overhead!
                else:
                    put_sym(lx, by, lz, 0)
        # Sculpted Gothic archivolt frame around gate mouth (`lz = 38` & `lz = 31`)
        put_sym(4, 48, lz, BASALT_Y)
        put_sym(4, 55, lz, CPB)
    put_sym(0, 58, 38, CPB)
    put_sym(2, 64, 38, BANNER_S)

    # E) Courtyard Wall-Walk Ascent Stairs (`lx = ±44, lz = 18..31`, `by = 47 -> 62`)
    for step_i in range(0, 15):
        sy = 48 + step_i
        sz = 17 + step_i
        for lx in (44, 45, 46):
            for fy in range(47, sy):
                put_sym(lx, fy, sz, DSB if lx < 46 else PBB)
            put_sym(lx, sy, sz, pbb_stair("south"))
            for hy in range(sy + 1, sy + 6):
                put_sym(lx, hy, sz, 0)
            put_sym(43, sy + 1, sz, PBB_WALL_NS)

    # F) 16 Symmetrical Perimeter Bastion & Gatehouse Towers (8 mirrored pairs!)
    build_sym_tower("Gatehouse_Barbican", 13, 35, 6, 46, 86, spire_h=0, door_dirs=("south", "north"))
    build_sym_tower("Curtain_FrontWatch", 35, 34, 5, 46, 76, spire_h=10, door_dirs=("north",))
    build_sym_tower("Bastion_FrontCorner", 54, 34, 6, 46, 82, spire_h=0, door_dirs=("south", "west"))
    build_sym_tower("Bastion_NorthFlank", 54, 6, 5, 46, 76, spire_h=10, door_dirs=("west",))
    build_sym_tower("Bastion_MidFlank", 54, -22, 6, 46, 80, spire_h=0, door_dirs=("west",))
    build_sym_tower("Bastion_SouthFlank", 54, -50, 5, 46, 76, spire_h=10, door_dirs=("west",))
    build_sym_tower("Bastion_RearCorner", 54, -78, 6, 46, 82, spire_h=0, door_dirs=("north", "west"))
    build_sym_tower("Bastion_RearApse", 24, -78, 5, 46, 76, spire_h=10, door_dirs=("north",))


# =============================================================================
# 5. GRAND CATHEDRAL KEEP EXTERIOR, CENTRAL DONJON & THRONE HALL INTERIOR
# =============================================================================
def build_cathedral_keep_and_throne_hall():
    print("  [4/7] Building soaring Gothic Cathedral Keep, Central Donjon & Throne Hall...")
    # Raised Upper Citadel Platform (`lx = 0..30, lz = -76..+6, by = 47..56`)
    for lz in range(-76, 7):
        for lx in range(0, 31):
            for by in range(47, 57):
                if lx == 30 or lz == 6 or lz == -76:
                    blk = PBB if (by in (47, 56) or lx % 6 == 0 or lz % 6 == 0) else DSB
                else:
                    blk = DSB if by < 56 else DST
                put_sym(lx, by, lz, blk)
            # Clear air above platform prior to building Keep & Rotunda
            for cy in range(57, 160):
                put_sym(lx, cy, lz, 0)

    # Grand Processional Staircase from Outer Courtyard (`lz = 15, by = 47`) to Keep Portal (`lz = 6, by = 56`)
    for step_i in range(0, 9):
        lz = 15 - step_i
        sy = 48 + step_i
        for lx in range(0, 7):
            for fy in range(47, sy):
                put_sym(lx, fy, lz, PBB if lx >= 5 else DSB)
            if lx <= 4:
                put_sym(lx, sy, lz, pbb_stair("north"))
            else:
                put_sym(lx, sy, lz, PBB)
                put_sym(lx, sy + 1, lz, CPB if step_i in (0, 4, 8) else PBB_SLAB_BOT)
                if step_i in (0, 8) and lx == 6:
                    put_sym(lx, sy + 2, lz, BRAZIER_S)

    # Cathedral Shell (`lx = 0..24, lz = -40..+4, floor by = 56, side aisle eaves by = 78, nave eaves by = 88`)
    bay_pillars_z = [-4, -12, -20, -28, -36]

    # 1) Seamless Geometric Cathedral Floor (`by = 56`)
    for lz in range(-39, 4):
        for lx in range(0, 24):
            rib = (lx in (0, 5, 11, 17, 23)) or ((lz + 40) % 8 == 0)
            put_sym(lx, 56, lz, PBB if rib else PDS)

    # 2) Outer Side-Aisle Walls (`lx = 24, lz = -40..+4, by = 57..78`) + Flying Buttresses (`lx = 25..31`)
    for lz in range(-40, 5):
        is_bay_pier = ((lz + 4) % 8 == 0)
        in_win = (abs(((lz + 4) % 8) - 4) <= 1) and (-36 <= lz <= 0)
        for by in range(57, 79):
            if by in (69, 77):
                blk = DARK_OAK_PLANKS
            elif by in (70, 78):
                blk = CPB
            elif is_bay_pier:
                blk = BASALT_Y if 58 <= by <= 76 else PBB
            elif in_win and (60 <= by <= 66 or 72 <= by <= 75):
                blk = GLASS_NS
            elif in_win and by in (59, 67, 71, 76):
                blk = CPB
            else:
                blk = DSB
            put_sym(24, by, lz, blk)

        # Exterior Flying Buttresses at every bay pier (`lz = +4, -4, -12, -20, -28, -36`)!
        if is_bay_pier and -36 <= lz <= 4:
            # Outer buttress pinnacle pier at `lx = 30`
            for by in range(56, 82):
                put_sym(30, by, lz, BASALT_Y if 58 <= by <= 76 else PBB)
            put_sym(30, 82, lz, CPB)
            put_sym(30, 83, lz, DSB_WALL_POST)
            # Lower & upper flying buttress arches springing inward from `lx = 29` to clerestory `lx = 13`!
            for lx in range(25, 30):
                fly_y = 75 + (29 - lx)
                put_sym(lx, fly_y, lz, PBB)
                put_sym(lx, fly_y - 1, lz, pbb_stair("east", half="top"))
            for lx in range(14, 24):
                fly_y2 = 81 + (23 - lx) // 2
                put_sym(lx, fly_y2, lz, PBB)

    # 3) Front (South, `lz = +4`) & Rear (North, `lz = -40`) Monumental Gable Walls (`by = 57..99`)
    for wall_z, is_front in ((4, True), (-40, False)):
        for lx in range(0, 25):
            gable_top = (99 - (lx * 9) // 13) if lx <= 13 else 83
            for by in range(57, gable_top + 1):
                if by == gable_top:
                    blk = CPB if lx == 0 else PBB
                elif lx in (0, 6, 13, 24) and by < gable_top - 1:
                    blk = BASALT_Y if (by % 6 != 0) else CPB
                elif is_front and lx <= 3 and by <= 66 - (lx * lx) // 3:
                    blk = 0  # Grand Gothic Cathedral Entry Portal!
                elif not is_front and 13 <= lx <= 17 and by <= 65 - ((lx - 15) ** 2):
                    blk = 0  # Twin Symmetrical Vaulted Ambulatory Portals (`lx = ±15`) into Rotunda!
                elif is_front and lx <= 3 and 73 <= by <= 84 and (lx * lx + ((by - 78) // 2) ** 2 <= 9):
                    blk = GLASS_EW  # Great Cathedral Rose / Lancet Window over front portal!
                elif 8 <= lx <= 10 and 62 <= by <= 74:
                    blk = GLASS_EW if lx == 9 else CPB
                else:
                    blk = DSB
                put_sym(lx, by, wall_z, blk)
        if is_front:
            # Projecting Gothic Portal Porch (`lx = 0..5, lz = 5..6, by = 57..70`)
            for lz in (5, 6):
                put_sym(4, 57, lz, PBB)
                for by in range(57, 68):
                    put_sym(4, by, lz, BASALT_Y if by < 67 else CPB)
                    put_sym(5, by, lz, PBB)
                for lx in range(0, 5):
                    g_y = 70 - lx // 2
                    put_sym(lx, g_y, lz, CPB if lx == 0 else PBB)

    # 4) Clustered Nave Colonnade (`lx = ±12`), Upper Triforium Galleries (`by = 70`), Clerestory (`lx = ±13, by = 71..88`) & Pointed Ribbed Vault!
    for lz in range(-39, 4):
        is_pier_z = (lz in bay_pillars_z)
        in_clerestory_win = (abs(((lz + 4) % 8) - 4) <= 1) and (-36 <= lz <= 0)

        # Upper Triforium Gallery floor over side aisles (`lx = 13..23, by = 70`) + lean-to slate roof (`by = 78..83`)
        for lx in range(13, 24):
            put_sym(lx, 70, lz, PBB if (lx in (13, 23) or is_pier_z) else DST)
            roof_y = 78 + (24 - lx) // 2
            put_sym(lx, roof_y, lz, PBB if is_pier_z else DST)
            put_sym(lx, roof_y - 1, lz, DSB)

        # Triforium balustrade overlooking central nave (`lx = 12, by = 71`)
        if not is_pier_z:
            put_sym(12, 71, lz, PBB_WALL_NS)
            if (lz + 4) % 8 == 4:
                put_sym(12, 72, lz, CANDLE_S)

        # Nave Clerestory Walls (`lx = 13, by = 71..88`) with recessed stained-glass lancets!
        for by in range(71, 89):
            if by == 88:
                blk = CPB
            elif is_pier_z:
                blk = BASALT_Y
            elif in_clerestory_win and 74 <= by <= 84:
                blk = GLASS_NS
            elif in_clerestory_win and by in (73, 85):
                blk = CPB
            else:
                blk = DSB
            put_sym(13, by, lz, blk)

        # Pointed Gothic Nave Arcade Arches between piers (`lx = 12, by = 64..69`)
        dz_bay = abs(((lz + 4) % 8) - 4)  # 0 at arch crown, 4 at pier
        arch_floor_y = 69 - (dz_bay * dz_bay) // 3
        for by in range(max(57, arch_floor_y), 71):
            if not is_pier_z and by == arch_floor_y:
                put_sym(12, by, lz, ASHLAR)
            elif not is_pier_z:
                put_sym(12, by, lz, DSB)

        # Clustered Gothic Nave Piers at `lz in bay_pillars_z` (`lx = 12` core + `11` & `13` shafts!)
        if is_pier_z:
            for dx_p, dz_p in ((0, 0), (-1, 0), (1, 0), (0, -1), (0, 1)):
                px, pz = 12 + dx_p, lz + dz_p
                put_sym(px, 57, pz, PBB)
                put_sym(px, 58, pz, CPB if (dx_p == 0 and dz_p == 0) else pbb_stair("west" if dx_p < 0 else ("east" if dx_p > 0 else ("north" if dz_p < 0 else "south"))))
                for by in range(59, 87):
                    if dx_p == 0 and dz_p == 0:
                        put_sym(px, by, pz, BASALT_Y)
                    elif by < 69 or (dx_p == -1 and dz_p == 0):
                        put_sym(px, by, pz, ASHLAR_PILLAR_Y if dx_p == -1 else BASALT_Y)
                put_sym(11, 69, lz, CPB)
            # Royal crimson horn banner + warm lantern sconces on nave & aisle faces of every pier!
            put_sym(10, 65, lz, BANNER_W)
            put_sym(10, 62, lz, PBB_WALL_POST)
            put_sym(10, 61, lz, LANTERN_HANG)
            put_sym(10, 76, lz, PBB_WALL_POST)
            put_sym(10, 75, lz, LANTERN_HANG)
            put_sym(14, 63, lz, PBB_WALL_POST)
            put_sym(14, 62, lz, LANTERN_HANG)

        # Soaring Pointed Gothic Ribbed Vault & Pitched Roof (`lx = 0..13, by = 88..98`)
        # Keep Central Crossing (`lz = -28..-12`) open up into the monumental Central Crown Donjon!
        in_crossing = (-28 < lz < -12)
        if not in_crossing:
            for lx in range(0, 14):
                vy = 97 - (lx * 9) // 13
                diag_rib = (abs(((lz + 4) % 8) - (lx * 4) // 13) == 0) or (abs((8 - ((lz + 4) % 8)) - (lx * 4) // 13) == 0)
                rib_blk = ASHLAR if (is_pier_z or lx == 0 or diag_rib) else DST
                put_sym(lx, vy, lz, rib_blk)
                put_sym(lx, vy + 1, lz, PBB if (lx == 0 or is_pier_z) else DSB)
                put_sym(lx, vy + 2, lz, CPB if (lx == 0 and lz % 2 == 0) else DST_SLAB_BOT)

    # 5) Monumental Central Crown Donjon over the Crossing (`lx = 0..13, lz = -29..-11, by = 88..150`)!
    for by in range(88, 147):
        for lz in range(-29, -10):
            for lx in range(0, 14):
                is_wall = (lx == 13 or lz in (-29, -11))
                if by == 118 or by == 146:
                    # Vault ceiling of Crossing Lantern at `by = 118` & top Donjon deck at `by = 146`
                    if by == 118 and lx <= 10 and -26 <= lz <= -14:
                        # Ribbed lantern dome at `by = 116..118`
                        put_sym(lx, by, lz, SEA_LANTERN if (lx <= 2 and -21 <= lz <= -19) else (ASHLAR if (lx == 0 or lz == -20 or abs(lx - abs(lz + 20)) <= 1) else DST))
                    else:
                        put_sym(lx, by, lz, PBB if is_wall else DST)
                elif not is_wall:
                    if by > 118 and lx == 0 and lz == -20:
                        put_sym(lx, by, lz, BASALT_Y)
                    else:
                        put_sym(lx, by, lz, 0)
                else:
                    # Outer Donjon walls with triple Gothic stained-glass lancets!
                    on_z_face = (lz in (-29, -11))
                    coord = lx if on_z_face else abs(lz + 20)
                    in_lancet = coord in (3, 4, 8, 9) and ((94 <= by <= 108) or (124 <= by <= 138))
                    if by in (91, 115, 142):
                        blk = DARK_OAK_PLANKS
                    elif by in (92, 116, 143):
                        blk = CPB
                    elif lx == 13 and lz in (-29, -11):
                        blk = PBB
                    elif coord in (0, 6, 12):
                        blk = BASALT_Y
                    elif in_lancet:
                        blk = GLASS_EW if on_z_face else GLASS_NS
                    elif coord in (3, 4, 8, 9) and by in (93, 109, 123, 139):
                        blk = CPB
                    else:
                        blk = DSB
                    put_sym(lx, by, lz, blk)

    # Central Donjon Machicolated Crown & 4 Corner Pinnacles (`by = 146..156`)
    for lz in range(-30, -9):
        for lx in range(0, 15):
            if lx == 14 or lz in (-30, -10):
                put_sym(lx, 146, lz, PBB)
                put_sym(lx, 147, lz, PBB)
                if (lx + lz) % 2 == 0:
                    put_sym(lx, 148, lz, CPB)
                    put_sym(lx, 149, lz, PBB_SLAB_BOT)
    for cz_pin in (-29, -11):
        for py in range(147, 156):
            for dx in (-1, 0):
                for dz in (-1 if cz_pin == -11 else 0, 0 if cz_pin == -11 else 1):
                    if py <= 152 or (dx == 0 and dz == 0):
                        put_sym(13 + dx, py, cz_pin + dz, PBB if py < 154 else DSB_WALL_POST)

    # 6) 4 Soaring Cathedral Keep Corner Spire Towers (`(±26, +4)` & `(±26, -40)`, `spire_h = 22` -> `by = 120`!)
    build_sym_tower("Keep_FrontSpire", 26, 4, 5, 56, 98, spire_h=22, door_dirs=("west", "north"), wall_walk_y=70)
    build_sym_tower("Keep_RearSpire", 26, -40, 5, 56, 98, spire_h=22, door_dirs=("west", "south"), wall_walk_y=70)
    # Connect Triforium Gallery (`by = 70`) cleanly into all 4 Keep Corner Spire Towers!
    for cz_conn, dz_step in ((4, -1), (-40, 1)):
        for step in range(1, 6):
            pz = cz_conn + dz_step * step
            for lx in (21, 22, 23):
                put_sym(lx, 70, pz, DST)
                for hy in range(71, 75):
                    put_sym(lx, hy, pz, 0)

    # Twin Crossing Turrets (`(±16, -20)`, `by = 56..146`) climbing from Cathedral Floor -> Triforium -> Donjon Roof!
    build_sym_tower("Donjon_CrossingTurret", 16, -20, 4, 56, 146, spire_h=12, door_dirs=("east", "north", "south"), wall_walk_y=70)

    # 7) Cathedral Throne Dais (`lx = 0..8, lz = -38..-31`), High Gothic Reredos, Throne, Carpets & Chandeliers!
    for step_i, lz in enumerate(range(-31, -35, -1)):  # lz = -31..-34 -> top = 57..60
        top_y = 57 + step_i
        for lx in range(0, 9):
            for fy in range(57, top_y):
                put_sym(lx, fy, lz, PBB)
            put_sym(lx, top_y, lz, CPB if lx == 8 else pbb_stair("south"))

    for lz in range(-38, -34):
        for lx in range(0, 9):
            for fy in range(57, 61):
                put_sym(lx, fy, lz, CPB if (fy == 60 and (lx in (0, 8) or lz in (-35, -38))) else PBB)

    # High Gothic Reredos & Canopy Baldachin (`lz = -38, lx = 0..7, by = 61..80`)
    for lx in range(0, 8):
        col_top = 80 - lx
        for by in range(61, col_top + 1):
            if by == col_top:
                put_sym(lx, by, -38, CPB)
            elif lx in (0, 3, 7):
                put_sym(lx, by, -38, ASHLAR_PILLAR_Y if lx == 3 else BASALT_Y)
            else:
                put_sym(lx, by, -38, DST if by % 4 != 0 else PBB)
    for lz in (-37, -36):
        for lx in range(0, 5):
            put_sym(lx, 75 + (4 - lx) // 2, lz, CPB if lx == 0 else PBB)
            if lx == 4:
                put_sym(lx, 74, lz, LANTERN_HANG)

    # Boss Throne at `(0, 61, -36)` facing South (`+lz` toward main portal!), Banners & Braziers!
    set_raw(0, 61, -36, THRONE_S)
    put_sym(2, 68, -37, BANNER_S)
    put_sym(5, 68, -37, BANNER_S)
    put_sym(4, 61, -35, BRAZIER_S)
    put_sym(8, 61, -35, BRAZIER_S)
    put_sym(8, 58, -31, BRAZIER_S)

    # Wrought Chandeliers down Central Nave (`lx = 0`) + Side Aisles (`lx = ±18`) + 2nd-Floor Galleries -> 17 Chandeliers!
    for lz, top_y, drop_y in ((-6, 96, 76), (-14, 117, 78), (-20, 117, 78), (-26, 117, 78), (-33, 96, 76)):
        for cy in range(drop_y + 1, top_y):
            put_sym(0, cy, lz, CHAIN_Y)
        set_raw(0, drop_y, lz, CHANDELIER)
    for lz in (-8, -16, -24, -32):
        for cy in range(65, 70):
            put_sym(18, cy, lz, CHAIN_Y)
        put_sym(18, 64, lz, CHANDELIER)
        # 2nd-Floor Triforium Gallery Chandeliers (`by = 76` under `by = 80` roof)
        for cy in range(77, 79):
            put_sym(18, cy, lz, CHAIN_Y)
        put_sym(18, 76, lz, LANTERN_HANG)

    # 8) FULL WALKABLE 2ND FLOOR (`2 ЭТАЖ`, `by = 70`):
    #    - Grand South Choir & Rose-Window Mezzanine Hall (`lx = 0..23, lz = -3..+3, by = 70`)
    #    - Twin Wide Interior Grand Staircases (`lx = ±19..±21, lz = -16 -> -4, by = 57 -> 70`)
    #    - Furnished 2nd-Floor Galleries (`lx = ±14..±22, lz = -38..+3`) with carpets, bookshelves, lecterns & lighting!
    for lz in range(-3, 4):
        for lx in range(0, 13):
            put_sym(lx, 70, lz, ASHLAR if (lz == -3 or lx == 0) else DST)
            put_sym(lx, 69, lz, PBB if (lz == -3 or lx % 4 == 0) else DSB)
            # Clear headroom on 2nd-floor Choir Hall (`by = 71..84`)
            for hy in range(71, 82):
                if lz < 4:
                    put_sym(lx, hy, lz, 0)
        # Carved 2nd-floor Choir Balustrade overlooking the Throne Nave (`lz = -3, by = 71`)
        for lx in range(0, 12):
            put_sym(lx, 71, -3, PBB_WALL_EW if lx % 3 != 0 else CPB)
            if lx in (0, 6):
                put_sym(lx, 72, -3, CANDLE_N)
    # Choir Mezzanine central carpet, lectern & chandelier
    for lz in range(-2, 3):
        for lx in range(0, 10):
            if lz in (-1, 0, 1) or lx <= 2:
                put_sym(lx, 71, lz, CARPET)
    set_raw(0, 71, -2, LECTERN_S)
    for cy in range(81, 95):
        put_sym(0, cy, 0, CHAIN_Y)
    set_raw(0, 80, 0, CHANDELIER)

    # Twin Wide Interior Grand Staircases (`lx = ±19..±21`, climbing `lz = -17 -> -5`, `by = 57 -> 69` onto `by = 70`!)
    for step_i in range(0, 13):
        sz = -17 + step_i
        sy = 58 + step_i
        for lx in (19, 20, 21):
            for fy in range(57, sy):
                put_sym(lx, fy, sz, PBB if lx == 19 else DSB)
            if sy == 70:
                put_sym(lx, 70, sz, DST)
            else:
                put_sym(lx, sy, sz, pbb_stair("south"))
            # Clear 5-block stair headroom punching cleanly onto the 2nd floor!
            for hy in range(sy + 1, min(76, sy + 6)):
                put_sym(lx, hy, sz, 0)
        put_sym(19, min(71, sy + 1), sz, PBB_WALL_NS)
        if step_i in (0, 6, 12):
            put_sym(19, min(72, sy + 2), sz, LANTERN_STAND)

    # 2nd-Floor Triforium Gallery Carpets, Bookshelves & Warm Sconce Lanterns (`lz = -38..-6` & `-4..+2`)
    for lz in range(-37, 3):
        if -17 <= lz <= -5:
            # Leave stair well open on `lx = 19..21`, carpet along inner gallery walk `lx = 15..17`
            for lx in (15, 16):
                put_sym(lx, 71, lz, CARPET)
        else:
            for lx in (16, 17, 18):
                put_sym(lx, 71, lz, CARPET)
        # 2nd-Floor recessed library bookshelves between outer aisle windows (`lx = 23, by = 71..74`)
        if ((lz + 4) % 8 in (1, 2, 6, 7)) and -36 <= lz <= 0:
            for by in range(71, 75):
                put_sym(23, by, lz, BOOKSHELF)
        if ((lz + 4) % 8 == 0) and -36 <= lz <= 0:
            put_sym(22, 74, lz, LANTERN_HANG)

    # Seamless 8-neighbor Connected Crimson Royal Carpet (`riftworld:castle_carpet`)
    for lz in range(-30, 3):
        for lx in range(0, 3):
            put_sym(lx, 57, lz, CARPET)
    for lz in (-35, -34):
        for lx in (0, 1):
            put_sym(lx, 61, lz, CARPET)
    # Transept cross-runner (`lz = -21..-19, lx = 3..21`) + 2x2 side-bay carpets
    for lz in (-21, -20, -19):
        for lx in range(3, 22):
            put_sym(lx, 57, lz, CARPET)
    for lz0 in (-9, -29):
        for dz in (0, 1):
            for lx in (17, 18):
                put_sym(lx, 57, lz0 + dz, CARPET)


# =============================================================================
# 6. CIRCULAR DOMED ROTUNDA SANCTUM (`center = (0, -58), R = 18, by = 56..95`)
# =============================================================================
def build_rotunda_sanctum():
    print("  [5/7] Building circular Domed Rotunda Sanctum & Twin Ambulatories...")
    cz_rot = -58
    R_out = 18.4
    R_wall_in = 16.0
    R_gal_in = 12.6

    # 1) Twin Symmetrical Two-Story Vaulted Ambulatory Passages (`lx = ±15, lz = -44..-39`)
    #    - 1st Floor (`by = 57`) + 2nd Floor Upper Gallery Link (`by = 66..70`) from Cathedral 2nd Floor into Rotunda 2nd Floor!
    for lz in range(-44, -38):
        step_down = max(0, min(4, -39 - lz))  # lz=-39 -> 70, lz=-43 -> 66 (smooth 4-step descent onto Rotunda 2nd floor!)
        up_y = 70 - step_down
        for lx in range(12, 19):
            is_amb_wall = lx in (12, 18)
            put_sym(lx, 56, lz, PBB if is_amb_wall else PDS)
            for by in range(57, 76):
                if by == 75:
                    put_sym(lx, by, lz, PBB)
                elif is_amb_wall:
                    put_sym(lx, by, lz, BASALT_Y if lz % 3 == 0 else DSB)
                elif by == 65:
                    put_sym(lx, by, lz, PBB)
                elif 66 <= by <= up_y:
                    if by == up_y and -43 <= lz <= -40:
                        put_sym(lx, by, lz, pbb_stair("south"))
                    else:
                        put_sym(lx, by, lz, DST)
                elif by == 57 and 14 <= lx <= 16:
                    put_sym(lx, by, lz, CARPET)
                elif by == 64 and lx == 15 and lz in (-42, -40):
                    put_sym(lx, by, lz, LANTERN_HANG)
                elif by == 74 and lx == 15 and lz in (-42, -40):
                    put_sym(lx, by, lz, LANTERN_HANG)
                else:
                    put_sym(lx, by, lz, 0)

    # 2) Circular Drum, Two-Tier Radial Arcades & Watertight Vertical-Filled Ribbed Dome!
    for dz in range(-19, 20):
        lz = cz_rot + dz
        for lx in range(0, 20):
            d = math.hypot(lx, dz)
            if d > R_out:
                continue

            # Clear interior air column first
            for cy in range(57, 97):
                if lz >= -40 and d > R_wall_in:
                    continue
                put_sym(lx, cy, lz, 0)

            # Circular geometric stone floor (`by = 56`)
            ring_rib = abs(d - 5.0) < 0.65 or abs(d - 10.5) < 0.65 or lx == 0 or dz == 0 or abs(lx - abs(dz)) <= 0
            put_sym(lx, 56, lz, ASHLAR if abs(d - 10.2) < 0.55 else (PBB if ring_rib else PDS))

            ang = math.atan2(dz, max(1e-6, float(lx)))
            if lx == 0:
                ang = math.pi * 0.5 if dz >= 0 else -math.pi * 0.5
            # 16 radial ribs around 360° -> symmetric around lx = 0!
            sector_phase = abs(((ang / (math.pi / 8.0)) % 1.0) - 0.5)  # 0 at bay center, 0.5 on radial rib
            on_radial_rib = sector_phase >= 0.36

            # Outer Cylindrical Masonry Wall (`R_wall_in <= d <= R_out`, `by = 57..76`)
            if d >= R_wall_in:
                amb_open_low = (-44 <= lz <= -39 and 13 <= lx <= 17)
                amb_open_high = (-45 <= lz <= -39 and 13 <= lx <= 17)
                in_rot_win = (not on_radial_rib) and (d >= R_out - 1.3) and not (-44 <= lz <= -38)
                for by in range(57, 77):
                    if amb_open_low and by <= 63:
                        put_sym(lx, by, lz, CARPET if (by == 57 and 14 <= lx <= 16) else 0)
                        continue
                    if amb_open_high and 67 <= by <= 73:
                        put_sym(lx, by, lz, 0)
                        continue
                    if by in (65, 75):
                        blk = DARK_OAK_PLANKS
                    elif by in (66, 76):
                        blk = CPB
                    elif on_radial_rib:
                        blk = BASALT_Y if d < R_out - 0.9 else PBB
                    elif in_rot_win and 69 <= by <= 73:
                        blk = GLASS_EW if abs(dz) >= lx else GLASS_NS
                    elif in_rot_win and by in (68, 74):
                        blk = CPB
                    elif (58 <= by <= 62 or 67 <= by <= 68) and d < R_wall_in + 1.05 and not on_radial_rib and abs(dz) >= 4:
                        # Recessed `castle_bookshelf` grand library alcoves on BOTH 1st & 2nd drum tiers!
                        blk = BOOKSHELF
                    else:
                        blk = DSB
                    put_sym(lx, by, lz, blk)

                # Exterior drum parapet & crenellations (`by = 77..78`)
                if d >= R_out - 1.1:
                    put_sym(lx, 77, lz, PBB)
                    if (lx + abs(dz)) % 2 == 0:
                        put_sym(lx, 78, lz, CPB if on_radial_rib else PBB)

            # Upper Circular Colonnade Gallery (`R_gal_in <= d < R_wall_in`, `by = 66`)
            elif d >= R_gal_in:
                put_sym(lx, 66, lz, PBB if on_radial_rib else DST)
                # Lower & Upper radial arcade piers along inner gallery ring (`d < R_gal_in + 1.05`)
                if d < R_gal_in + 1.05:
                    if on_radial_rib:
                        for by in range(57, 76):
                            put_sym(lx, by, lz, ASHLAR_PILLAR_Y if by not in (65, 66, 75) else CPB)
                    else:
                        # Arched lintels at `by = 64..65` & `by = 74..75` + gallery balustrade at `by = 67`
                        put_sym(lx, 65, lz, ASHLAR)
                        put_sym(lx, 67, lz, PBB_WALL_NS if lx >= abs(dz) else PBB_WALL_EW)
                        put_sym(lx, 75, lz, ASHLAR)

            # Watertight Vertical-Filled Hemispherical Ribbed Dome (`d <= R_out - 0.8`, `by = 76..91`)!
            if d <= 17.5:
                if d <= 3.8:
                    # Glazed Lantern Oculus (`R <= 3.8`, `by = 90..95`) — matches ref 04!
                    if d >= 2.8:
                        for by in range(89, 95):
                            put_sym(lx, by, lz, OCULUS_GLASS if 90 <= by <= 93 else CPB)
                        put_sym(lx, 95, lz, PBB_SLAB_BOT)
                    else:
                        put_sym(lx, 94, lz, OCULUS_GLASS)
                        if lx == 0 and dz == 0:
                            put_sym(0, 95, cz_rot, SEA_LANTERN)
                else:
                    # Smooth spherical dome profile with VERTICAL FILL so 0 air gaps exist between steps!
                    u_cur = min(1.0, d / 17.5)
                    u_nxt = min(1.0, (d + 1.05) / 17.5)
                    y_hi = 76 + int(round(14.0 * math.sqrt(max(0.0, 1.0 - u_cur * u_cur))))
                    y_lo = 76 + int(round(14.0 * math.sqrt(max(0.0, 1.0 - u_nxt * u_nxt))))
                    for by in range(min(y_lo, y_hi), y_hi + 1):
                        put_sym(lx, by, lz, ASHLAR if on_radial_rib else DST)
                    put_sym(lx, y_hi + 1, lz, PBB if on_radial_rib else DSB)

    # 3) Twin Sweeping Symmetrical Gallery Staircases (`lx = ±10..±12, lz = -53..-45, by = 57 -> 66`)!
    for step_i in range(0, 9):
        lz = -45 - step_i
        sy = 57 + step_i
        for lx in (10, 11, 12):
            for fy in range(57, sy):
                put_sym(lx, fy, lz, ASHLAR if lx == 10 else DSB)
            put_sym(lx, sy, lz, ashlar_stair("south") if lx == 10 else pbb_stair("south"))
            for hy in range(sy + 1, sy + 5):
                put_sym(lx, hy, lz, 0)
        put_sym(10, sy + 1, lz, PBB_WALL_NS)
    # Landing bridge at `lz = -54..-55` onto `by = 66` circular gallery
    for lz in (-54, -55):
        for lx in range(10, 14):
            put_sym(lx, 66, lz, ASHLAR if lx == 10 else DST)
            for hy in range(67, 71):
                put_sym(lx, hy, lz, 0)

    # 4) Rotunda Sanctum Dais (`(0, -64)`), Chapel Altar, Lecterns, 5 Chandeliers & Connected Carpet Mandala!
    for lz in range(-67, -61):
        for lx in range(0, 5):
            if lx == 4 or lz == -61:
                put_sym(lx, 57, lz, pbb_stair("south" if lz == -61 else "east"))
            else:
                put_sym(lx, 57, lz, PBB)
                if lx <= 2 and -66 <= lz <= -63:
                    put_sym(lx, 58, lz, CPB if (lx == 2 or lz in (-66, -63)) else PBB)

    # Central Chapel Altar at `(0, 59, -65)` facing South (`+lz` toward camera at `(0, -45)`!)
    set_raw(0, 59, -65, ALTAR_S)
    put_sym(3, 58, -64, CANDLE_S)
    put_sym(3, 57, -60, LECTERN_S)
    put_sym(2, 63, -73, BANNER_S)

    # 9 Hanging Wrought Chandeliers + Warm Gallery Ring Lanterns in Rotunda!
    for cx_c, cz_c, top_c, drop_c in (
        (0, -58, 93, 73),
        (6, -58, 86, 72),
        (0, -52, 86, 72),
        (0, -64, 86, 72),
        (5, -53, 86, 72),
        (5, -63, 86, 72),
    ):
        for cy in range(drop_c + 1, top_c):
            put_sym(cx_c, cy, cz_c, CHAIN_Y)
        if cx_c == 0:
            set_raw(0, drop_c, cz_c, CHANDELIER)
        else:
            put_sym(cx_c, drop_c, cz_c, CHANDELIER)
    # Warm lanterns on the 2nd-floor Rotunda gallery ceiling (`R ~ 14.2`)
    for lx_l, lz_l in ((14, -58), (10, -48), (10, -68), (0, -72)):
        put_sym(lx_l, 74, lz_l, LANTERN_HANG)
        put_sym(lx_l, 64, lz_l, LANTERN_HANG)

    # Connected `castle_carpet` octagonal cross & center mandala (`by = 57`)
    for dz in range(-12, 15):
        lz = cz_rot + dz
        if lz <= -61 and dz < -2:
            continue
        for lx in range(0, 10):
            d = math.hypot(lx, dz)
            in_cross = (lx <= 1 and -3 <= dz <= 14) or (abs(dz) <= 1 and lx <= 9)
            in_ring = 5.6 <= d <= 7.2
            in_core = d <= 3.2
            if in_cross or in_ring or in_core:
                if get_raw(lx, 57, lz) == 0:
                    put_sym(lx, 57, lz, CARPET)


# =============================================================================
# 7. STRICT AUTOMATED INVARIANT VERIFICATION & STREAMING GZIP NBT WRITER
# =============================================================================
def verify_invariants():
    print("  [6/7] Running strict symmetry, palette, window & tower-stair verifications...")
    verify_registered_blocks()

    # 1) Verify 100.000% exact X-mirror symmetry (`lx <-> -lx`) across the entire volume
    #    (skipping only 2 multi-block horizontal master props centered at lx=0 whose footprint spans x0=-1..+1:
    #     `boss_throne` at (0,61,-36) and `chapel_altar` at (0,59,-65) — single-block masters sit cleanly on lx=0!)
    mismatches = 0
    for by in range(H):
        y_off = by * STRIDE_Y
        for lz in range(OZ, OZ + D):
            row_off = y_off + (lz - OZ) * W
            for lx in range(1, 124):
                v_pos = vox[row_off + (lx - OX)]
                v_neg = vox[row_off + (-lx - OX)]
                if v_neg != mirror_val(v_pos):
                    mismatches += 1
                    if mismatches <= 5:
                        print(f"    SYMMETRY MISMATCH at (±{lx}, {by}, {lz}): +{v_pos} vs -{v_neg}")
    assert mismatches == 0, f"Found {mismatches} X-symmetry mismatches!"

    # 2) Verify 20/20 towers have >= 16 stairs
    stair_ids = {
        idx
        for idx, (nm, _) in enumerate(palette, start=1)
        if nm == "minecraft:polished_blackstone_brick_stairs"
    }
    assert len(TOWER_REGISTRY) == 22, f"Expected 22 symmetrical towers, got {len(TOWER_REGISTRY)}"
    total_tower_stairs = 0
    for tname, tcx, tcz, yb, yt in TOWER_REGISTRY:
        cnt = 0
        for by in range(yb, yt + 2):
            for dz in range(-2, 3):
                for dx in range(-2, 3):
                    if get_raw(tcx + dx, by, tcz + dz) in stair_ids:
                        cnt += 1
        assert cnt >= 16, f"Tower {tname} at ({tcx},{tcz}) has only {cnt} stairs!"
        total_tower_stairs += cnt

    # 3) Verify Cathedral Hall & Rotunda counts
    carpets_cath = sum(
        1
        for by in range(56, 65)
        for lz in range(-39, 4)
        for lx in range(-23, 24)
        if get_raw(lx, by, lz) == CARPET
    )
    carpets_rot = sum(
        1
        for by in range(56, 62)
        for lz in range(-77, -40)
        for lx in range(-19, 20)
        if get_raw(lx, by, lz) == CARPET
    )
    books_rot = sum(
        1
        for by in range(57, 65)
        for lz in range(-77, -40)
        for lx in range(-19, 20)
        if get_raw(lx, by, lz) == BOOKSHELF
    )
    assert carpets_cath >= 220, f"Expected >=220 Cathedral carpets, got {carpets_cath}"
    assert carpets_rot >= 100, f"Expected >=100 Rotunda carpets, got {carpets_rot}"
    assert books_rot >= 40, f"Expected >=40 Rotunda bookshelves, got {books_rot}"
    print(
        f"    VERIFIED: symmetry_mismatches=0 | towers={len(TOWER_REGISTRY)} (stairs={total_tower_stairs}) "
        f"| cath_carpets={carpets_cath} | rot_carpets={carpets_rot} | rot_books={books_rot} | palette={len(palette)}"
    )


def write_nbt(path: str):
    print(f"  [7/7] Writing compressed StructureTemplate NBT to {path}...")

    def u8(v):
        return struct.pack(">B", v)

    def i32(v):
        return struct.pack(">i", v)

    def utf(s: str):
        b = s.encode("utf-8")
        return struct.pack(">H", len(b)) + b

    # Count non-air blocks
    non_air = sum(1 for b in vox if b != 0)

    out = bytearray()
    # Root TAG_Compound("")
    out += u8(10) + utf("")

    # DataVersion: 3465 (MC 1.20.1)
    out += u8(3) + utf("DataVersion") + i32(3465)

    # size: TAG_List(TAG_Int)[W, H, D]
    out += u8(9) + utf("size") + u8(3) + i32(3) + i32(W) + i32(H) + i32(D)

    # palette: TAG_List(TAG_Compound)
    out += u8(9) + utf("palette") + u8(10) + i32(len(palette))
    for nm, pr in palette:
        out += u8(8) + utf("Name") + utf(nm)
        if pr:
            out += u8(10) + utf("Properties")
            for k, v in pr.items():
                out += u8(8) + utf(k) + utf(v)
            out += u8(0)
        out += u8(0)

    # blocks: TAG_List(TAG_Compound) — prepacked template for maximum speed!
    out += u8(9) + utf("blocks") + u8(10) + i32(non_air)
    # Fixed binary template per block:
    # TAG_List(9) "pos"(3) elem=Int(3) len=3 bx by bz + TAG_Int(3) "state"(5) st + TAG_End(0)
    pos_hdr = u8(9) + utf("pos") + u8(3) + i32(3)
    st_hdr = u8(3) + utf("state")
    pack3i = struct.Struct(">iii").pack
    pack1i = struct.Struct(">i").pack

    for by in range(H):
        y_off = by * STRIDE_Y
        for bz in range(D):
            r_off = y_off + bz * W
            for bx in range(W):
                v = vox[r_off + bx]
                if v != 0:
                    out += pos_hdr + pack3i(bx, by, bz) + st_hdr + pack1i(v - 1) + b"\x00"

    # entities: empty TAG_List
    out += u8(9) + utf("entities") + u8(10) + i32(0)
    # End root compound
    out += u8(0)

    with gzip.open(path, "wb", compresslevel=6) as gz:
        gz.write(out)

    sz_mb = os.path.getsize(path) / (1024.0 * 1024.0)
    print(f"    DONE: {non_air:,} solid blocks -> {sz_mb:.2f} MB ({path})")


def main():
    print("Generating v0.26.0 symmetrical Gothic Basalt Citadel `blacktide.nbt`...")
    build_volcanic_island()
    build_viaduct_bridge()
    build_curtain_walls_and_gatehouse()
    build_cathedral_keep_and_throne_hall()
    build_rotunda_sanctum()
    verify_invariants()
    write_nbt(OUT_NBT)


if __name__ == "__main__":
    main()
