#!/usr/bin/env python3
"""CPU-рендер настоящего мира Minecraft: без видеокарты, без OpenGL, без шейдеров.

Что делает: читает региональные файлы мира (Anvil, `.mca`), разбирает NBT вручную, собирает воксельную
модель куска мира, и **трассирует лучи на процессоре** (numpy-векторизованный DDA по сетке блоков),
закрашивая грани по нормали, с туманом по расстоянию. Ни GPU, ни GL, ни Minecraft при рендере не нужны —
только numpy + pillow, поэтому это работает в песочнице и на любом сервере без графики.

Зачем: сервер в песочнице безоконный, «скриншот из игры» из него не сделать. Этот рендер берёт **те же самые
блоки, что сгенерировал мод** (`bridge_post`, `lilac_leaves`, `fern_log`, …) прямо из сохранения мира, поэтому
на картинке видно настоящую генерацию, а не нарисованную от руки сцену.

Usage:
    python3 tools/rift/render_cpu.py --centre 1374 240 --radius 112 --size 960x540 --out shots/violet.png
    python3 tools/rift/render_cpu.py --check            # самопроверка: CPU-only, детерминизм, покрытие

Опции:
    --dim PATH   каталог измерения (по умолчанию world/dimensions/riftworld/elisar/region)
    --eye X Y Z  положение камеры (по умолчанию над центром, смотрит вниз под углом 32°)
    --yaw DEG    поворот камеры вокруг вертикали
    --pitch DEG  наклон
"""
from __future__ import annotations

import argparse
import gzip
import hashlib
import io
import math
import os
import struct
import sys
import time
import zlib
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
TEX = ROOT / "mod/src/main/resources/assets/riftworld/textures/block"
DEFAULT_REGION = Path("/tmp/mcserver/world/dimensions/riftworld/elisar/region")

# ----------------------------------------------------------------------------- NBT (минимальный, без зависимостей)
TAG_END, TAG_BYTE, TAG_SHORT, TAG_INT, TAG_LONG = 0, 1, 2, 3, 4
TAG_FLOAT, TAG_DOUBLE, TAG_BYTE_ARRAY, TAG_STRING = 5, 6, 7, 8
TAG_LIST, TAG_COMPOUND, TAG_INT_ARRAY, TAG_LONG_ARRAY = 9, 10, 11, 12


def _read(f, fmt):
    n = struct.calcsize(fmt)
    return struct.unpack(fmt, f.read(n))[0]


def nbt_read(f, tag=None, named=True):
    """named=False — для элементов списка: у них тега-имени нет (частая ошибка в самодельных NBT-парсерах)."""
    if tag is None:
        tag = _read(f, ">B")
    if tag == TAG_END:
        return None
    if named:
        name = _read(f, ">H")
        f.read(name)
    if tag == TAG_BYTE:
        return _read(f, ">b")
    if tag == TAG_SHORT:
        return _read(f, ">h")
    if tag == TAG_INT:
        return _read(f, ">i")
    if tag == TAG_LONG:
        return _read(f, ">q")
    if tag == TAG_FLOAT:
        return _read(f, ">f")
    if tag == TAG_DOUBLE:
        return _read(f, ">d")
    if tag == TAG_BYTE_ARRAY:
        n = _read(f, ">i")
        return f.read(n)
    if tag == TAG_STRING:
        n = _read(f, ">H")
        return f.read(n).decode("utf-8", "replace")
    if tag == TAG_LIST:
        it, n = _read(f, ">B"), _read(f, ">i")
        return [nbt_read(f, it, named=False) for _ in range(n)]
    if tag == TAG_COMPOUND:
        out = {}
        while True:
            t = _read(f, ">B")
            if t == TAG_END:
                return out
            out_key = _read(f, ">H")
            key = f.read(out_key).decode("utf-8", "replace")
            # имя ключа уже прочитано выше — значение парсим без имени, иначе парсер уезжает на байты
            out[key] = nbt_read(f, t, named=False)
    if tag == TAG_INT_ARRAY:
        n = _read(f, ">i")
        return list(struct.unpack(f">{n}i", f.read(4 * n)))
    if tag == TAG_LONG_ARRAY:
        n = _read(f, ">i")
        return list(struct.unpack(f">{n}q", f.read(8 * n)))
    raise ValueError(f"неизвестный NBT-тег {tag}")


def region_chunks(mca: Path):
    """Все чанки региона: { (cx, cz): nbt-словарь }. Сжатие 1=gzip, 2=zlib, 3=raw, 4=lz4 (не поддержан)."""
    data = mca.read_bytes()
    out = {}
    for i in range(1024):
        entry = struct.unpack(">I", data[i * 4:i * 4 + 4])[0]
        if entry == 0:
            continue
        sector, count = entry >> 8, entry & 0xFF
        start = sector * 4096
        if start + 5 > len(data):
            continue
        length = struct.unpack(">I", data[start:start + 4])[0]
        comp = data[start + 4]
        payload = data[start + 5:start + 4 + length]
        try:
            if comp == 1:
                payload = gzip.decompress(payload)
            elif comp == 2:
                payload = zlib.decompress(payload)
            elif comp == 3:
                pass
            else:
                continue
        except Exception:
            continue
        try:
            nbt = nbt_read(io.BytesIO(payload))
        except Exception:
            continue
        if nbt and "Level" in nbt:
            nbt = nbt["Level"]
        if nbt:
            out[(i % 32, i // 32)] = nbt
    return out


# ----------------------------------------------------------------------------- блоки -> цвет
TEX_CACHE: dict[str, np.ndarray] = {}
ALPHA_CACHE: dict[str, np.ndarray] = {}


# Биомный тинт: текстуры травы и листвы в моде сделаны почти серыми (как и в ваниле), цвет им даёт клиент по
# биому. Без этого CPU-кадры выглядят выцветшими. Палитры приглушённые — «не делать ярко».
TINT = (
    ("_leaves", (0.52, 0.60, 0.44)),
    ("_blossom", (0.60, 0.52, 0.68)),
    ("_fronds", (0.50, 0.62, 0.46)),
    ("_grass", (0.48, 0.60, 0.38)),
    ("_moss", (0.44, 0.56, 0.34)),
    ("_wheat", (0.74, 0.70, 0.50)),
)


def biome_tint(name: str, tex: np.ndarray | None = None) -> np.ndarray | None:
    """Цвет биома для «серых» текстур (трава, часть листвы). Уже окрашенные текстуры не трогаем.

    В ванильном пайплайне клиент умножает почти белую текстуру травы на цвет биома, а у нас в моде часть листвы
    (sizo, ambra) тоже сделана полу-серой. Определяем по насыщенности: если текстура серая и светлая — это как раз
    такая «тинтуемая» текстура, иначе оставляем как есть (иначе зелёная листва позеленеет дважды).
    """
    path = name.split(":", 1)[-1]
    if tex is not None:
        m = tex.reshape(-1, 3).mean(axis=0)
        if (m.max() - m.min()) / 255.0 > 0.12 or m.mean() < 120:      # уже цветная или тёмная — не тинтуем
            return None
    for suffix, rgb in TINT:
        if path.endswith(suffix):
            if path.startswith("sizo_") or path.startswith("mist_"):
                return np.array([0.60, 0.66, 0.68], dtype=np.float64)   # сизые породы: холодный серо-зелёный
            if path.startswith("lilac_"):
                return np.array([0.62, 0.55, 0.70], dtype=np.float64)   # сирень: пыльно-лиловый
            if path.startswith("fern_"):
                return np.array([0.62, 0.78, 0.56], dtype=np.float64)
            return np.array(rgb, dtype=np.float64)
    return None


def mod_texture(name: str) -> np.ndarray | None:
    """16x16 RGB-тексели блока (реальная текстура мода), либо None."""
    if name in TEX_CACHE:
        return TEX_CACHE[name]
    ns, path = (name.split(":", 1) if ":" in name else ("minecraft", name))
    cands = [f"{path}.png"] if ns == "riftworld" else []
    if path.endswith("_water"):
        cands = [f"{path}_still.png", f"{path}.png"]
    for cand in cands:
        p = TEX / cand
        if p.exists():
            a = np.asarray(Image.open(p).convert("RGBA"), dtype=np.float64)
            if a.shape[0] != 16:
                a = np.asarray(Image.fromarray(a.astype(np.uint8)).resize((16, 16)), dtype=np.float64)
            TEX_CACHE[name] = a[..., :3]
            ALPHA_CACHE[name] = a[..., 3]
            return TEX_CACHE[name]
    TEX_CACHE[name] = None
    return None


def mod_alpha(name: str) -> np.ndarray | None:
    if name not in TEX_CACHE:
        mod_texture(name)
    return ALPHA_CACHE.get(name)


def mod_color(name: str) -> tuple[int, int, int] | None:
    if ":" in name:
        ns, path = name.split(":", 1)
    else:
        ns, path = "minecraft", name
    if ns == "riftworld":
        for cand in (f"{path}.png", f"{path}_top.png"):
            p = TEX / cand
            if p.exists():
                im = Image.open(p).convert("RGBA")
                a = np.asarray(im.resize((16, 16)), dtype=np.float64)
                w = a[..., 3:4] / 255.0
                rgb = (a[..., :3] * w).sum(axis=(0, 1)) / max(1e-6, w.sum())
                return tuple(int(c) for c in np.clip(rgb, 0, 255))
    return None


VANILLA = {
    "minecraft:stone": (125, 125, 125), "minecraft:cobblestone": (110, 110, 110),
    "minecraft:dirt": (134, 96, 67), "minecraft:grass_block": (110, 145, 70),
    "minecraft:water": (52, 92, 160), "minecraft:sand": (219, 207, 163),
    "minecraft:gravel": (135, 130, 128), "minecraft:oak_log": (110, 88, 55),
    "minecraft:oak_leaves": (70, 110, 50), "minecraft:bedrock": (60, 60, 60),
    "minecraft:snow_block": (240, 245, 250), "minecraft:air": (0, 0, 0),
    # ад: без этих цветов netherrack в CPU-кадре выглядел серым пластиком
    "minecraft:netherrack": (92, 48, 45), "minecraft:soul_sand": (78, 60, 52),
    "minecraft:soul_soil": (69, 55, 47), "minecraft:basalt": (74, 72, 78),
    "minecraft:blackstone": (46, 41, 45), "minecraft:magma_block": (140, 66, 30),
    "minecraft:glowstone": (196, 160, 96), "minecraft:nether_bricks": (44, 22, 26),
    "minecraft:crimson_nylium": (120, 44, 52), "minecraft:warped_nylium": (44, 108, 100),
    "minecraft:lava": (214, 106, 30), "minecraft:quartz_block": (222, 214, 202),
    "minecraft:nether_quartz_ore": (196, 188, 176), "minecraft:nether_gold_ore": (124, 76, 52),
    "minecraft:warped_wart_block": (42, 88, 84), "minecraft:warped_stem": (58, 82, 74),
    "minecraft:warped_nylium": (44, 108, 100), "minecraft:shroomlight": (222, 160, 92),
    "minecraft:warped_roots": (46, 104, 96), "minecraft:nether_sprouts": (60, 124, 104),
}


# ----------------------------------------------------------------------------- мосты (порт BridgeDeckGeom)
DECK_ANCHOR, CABLE_ANCHOR, PLANK_STEP, PLANK_TH = 0.22, 1.62, 0.5, 0.11

# style id -> (текстура настила, текстура троса, доля дыр, множитель провиса)
STYLES = {
    0: ("planks_plains", "cable_rope", 0.03, 1.00),
    1: ("planks_garden", "cable_rope", 0.06, 1.08),
    2: ("planks_grove", "cable_rope", 0.09, 1.14),
    3: ("planks_burnt", "cable_chain", 0.34, 1.28),
    4: ("planks_mist", "cable_chain", 0.14, 1.18),
}
BRIDGE_TEX = ROOT / "mod/src/main/resources/assets/riftworld/textures/entity/bridge"


def bridge_texture(name: str, rgba: bool = False):
    """Текстура моста 16x16. rgba=True — вместе с альфой (тросы и сетка нарисованы тонкими линиями)."""
    p = BRIDGE_TEX / f"{name}.png"
    if p.exists():
        a = np.asarray(Image.open(p).convert("RGBA"), dtype=np.float32)
        if a.shape[0] != 16:
            a = np.asarray(Image.fromarray(a.astype(np.uint8)).resize((16, 16)), dtype=np.float32)
        return a if rgba else a[..., :3]
    return np.full((16, 16, 4 if rgba else 3), 120.0, dtype=np.float32)


class Deck:
    """Один подвесной мост: 4 опоры + провисание, как в BridgeDeckGeom. Всё считается в локальном базисе моста."""

    def __init__(self, a, b, a2, b2, style_id: int, break_from: float = 0.0, break_to: float = 0.0):
        self.a, self.b, self.a2, self.b2 = (np.array(v, dtype=np.float64) + 0.5 for v in (a, b, a2, b2))
        self.style_id = style_id
        self.break_from, self.break_to = break_from, break_to
        tex, cable_tex, holes, sag_mul = STYLES.get(style_id, STYLES[0])
        self.holes, self.sag_mul = holes, sag_mul
        self.plank_tex = bridge_texture(tex)
        # трос — тонкая светлая нить на прозрачном фоне: усредняем только видимые тексели, иначе он чёрный
        ca = bridge_texture(cable_tex, rgba=True).reshape(-1, 4)
        vis = ca[:, 3] > 96
        self.cable_col = (ca[vis, :3].mean(axis=0) if vis.any() else ca[:, :3].mean(axis=0))
        self.cable_col = np.clip(self.cable_col, 60, 210)

        # локальный базис: e1 — вдоль пролёта, e2 — по ширине, e3 — вверх
        d = self.b - self.a
        self.len = float(np.linalg.norm(d))
        self.e1 = d / max(1e-9, self.len)
        w = self.a2 - self.a
        w[1] = 0.0
        self.width = float(np.linalg.norm(w))
        e2 = w / max(1e-9, self.width) if self.width > 1e-6 else np.array([1.0, 0.0, 0.0])
        e3 = np.cross(e2, self.e1)
        if e3[1] < 0:                       # «верх» всегда вверх, иначе мост перевернётся
            e3 = -e3
        e3 /= max(1e-9, np.linalg.norm(e3))
        self.e3 = e3
        self.e2 = np.cross(self.e1, self.e3)
        # край a2 может оказаться и в +z, и в -z локальной системы — запоминаем диапазон ширины
        zsign = 1.0 if float(np.dot(w, self.e2)) >= 0 else -1.0
        self.z0, self.z1 = (0.0, self.width) if zsign > 0 else (-self.width, 0.0)

    def key(self):
        return tuple(int(v) for v in np.concatenate([self.a, self.b, self.a2, self.b2])) + (self.style_id,)

    # --- форма
    def sag(self) -> float:
        s = self.len * 0.0205 * self.sag_mul
        if self.break_to > self.break_from:
            s *= 1.30
        return float(min(1.25, max(0.16, s)))

    def cable_sag(self) -> float:
        return self.sag() * 0.78

    def open_at(self, u: float) -> bool:
        return self.break_to > self.break_from and self.break_from < u < self.break_to

    def deck_y(self, u: float) -> float:
        return -self.sag() * 4 * u * (1 - u)

    def plank_at(self, u: float) -> bool:
        if self.open_at(u):
            return False
        n = max(2, int(np.ceil(self.len / PLANK_STEP)))
        h = int(np.floor(u * n))
        r = _mix(h, int(self.a[0]) * 31 + int(self.a[2]) * 17, self.style_id)
        return ((r & 0xFF) / 255.0) >= self.holes

    def bbox_world(self):
        pts = np.concatenate([self.a[None, :], self.b[None, :], self.a2[None, :], self.b2[None, :]])
        lo = pts.min(axis=0) - 1.0
        hi = pts.max(axis=0) + np.array([1.0, CABLE_ANCHOR + 0.6, 1.0])
        return lo, hi

    def to_local(self, o: np.ndarray, d: np.ndarray):
        base = self.a + np.array([0.0, DECK_ANCHOR - 0.5, 0.0])
        rel = o - base
        ol = np.array([rel @ self.e1, rel @ self.e3, rel @ self.e2])
        dl = np.stack([d @ self.e1, d @ self.e3, d @ self.e2], axis=-1)
        return ol, dl


def _mix(a: int, b: int, c: int) -> int:
    h = (a * 374761393 + b * 668265263 + c * 2147483647) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return h ^ (h >> 16)


def deck_boxes(deck: Deck):
    """Коробки моста в ЛОКАЛЬНОМ базисе: (x0,x1,y0,y1,z0,z1, kind, u0, u1).

    kind: 0 — доска настила, 1 — трос, 2 — подвеска, 3 — продольная балка.
    """
    boxes = []
    L, Wd = deck.len, deck.width
    za, zb = deck.z0, deck.z1
    n = max(2, int(np.ceil(L / PLANK_STEP)))
    for i in range(n):
        u0, u1 = i / n, (i + 1) / n
        if not deck.plank_at((u0 + u1) / 2):
            continue
        y0 = deck.deck_y((u0 + u1) / 2)
        boxes.append((u0 * L, u1 * L + 0.01, y0 - PLANK_TH, y0, za, zb, 0, u0, u1))
    # продольные балки под краями настила
    m = 48
    for k in range(m):
        u0, u1 = k / m, (k + 1) / m
        if deck.open_at((u0 + u1) / 2):
            continue
        for z in (za + 0.02, zb - 0.02):
            y0 = deck.deck_y((u0 + u1) / 2) - PLANK_TH - 0.1
            boxes.append((u0 * L, u1 * L + 0.02, y0, y0 + 0.1, z - 0.04, z + 0.04, 3, u0, u1))
    # главный трос по каждой стороне + подвески
    h = CABLE_ANCHOR - DECK_ANCHOR
    for z in (za, zb):
        for k in range(m):
            u0, u1 = k / m, (k + 1) / m
            if deck.open_at((u0 + u1) / 2):
                continue
            ya = h - deck.cable_sag() * 4 * u0 * (1 - u0)
            yb = h - deck.cable_sag() * 4 * u1 * (1 - u1)
            boxes.append((u0 * L, u1 * L + 0.02, min(ya, yb) - 0.06, max(ya, yb) + 0.06, z - 0.06, z + 0.06, 1, u0, u1))
    nh = max(2, int(np.round(L / 1.25)))
    for k in range(nh + 1):
        u = k / nh
        if deck.open_at(u):
            continue
        ytop = h - deck.cable_sag() * 4 * u * (1 - u)
        ybot = deck.deck_y(u)
        for z in (za, zb):
            boxes.append((u * L - 0.035, u * L + 0.035, ybot, ytop, z - 0.035, z + 0.035, 2, u, u))
    return boxes


def _pos(v) -> tuple[float, float, float]:
    """BlockPos в NBT: либо compound {X,Y,Z}, либо список/массив из трёх чисел."""
    if isinstance(v, dict):
        return (float(v["X"]), float(v["Y"]), float(v["Z"]))
    if hasattr(v, "tolist"):
        v = v.tolist()
    return tuple(float(x) for x in v[:3])


class World:
    """Кусок мира в виде 3D-массива id блоков + таблица цветов."""

    def __init__(self, region_dir: Path):
        self.dir = region_dir
        self.ids: dict[str, int] = {}
        self.cols: list[tuple[int, int, int]] = [(0, 0, 0)]
        self.names: list[str] = ["minecraft:air"]
        self.tex: list[np.ndarray | None] = [None]
        self.alp: list[np.ndarray | None] = [None]
        self.decks: dict[tuple, Deck] = {}          # мосты из блок-энтити: у опоры лежит весь Deck

    def id_of(self, name: str) -> int:
        if name in self.ids:
            return self.ids[name]
        i = len(self.names)
        self.ids[name] = i
        self.names.append(name)
        col = mod_color(name) or VANILLA.get(name, (150, 150, 150))
        # «воздух» в мире бывает не только minecraft:air (cave_air, void_air) — иначе пещеры рисуются серым
        # пластиком; настил моста (bridge_walk) вообще невидим: его геометрию рисует composite_decks
        if name.endswith("air") or name == "riftworld:bridge_walk":
            col = (0, 0, 0)
        self.cols.append(col)
        t = mod_texture(name)
        tint = biome_tint(name, t)
        if t is not None and tint is not None:
            t = np.clip(t * tint[None, None, :], 0, 255)     # цвет травы/листвы, как его даёт клиент
        self.tex.append(t)
        self.alp.append(mod_alpha(name))
        return i

    def alpha_table(self) -> np.ndarray | None:
        """(N,16,16) непрозрачность текселя; 255 — обычный непрозрачный блок."""
        if not any(a is not None for a in self.alp):
            return None
        out = np.full((len(self.names), 16, 16), 255, dtype=np.uint8)
        for i, a in enumerate(self.alp):
            if a is not None:
                out[i] = np.clip(a, 0, 255).astype(np.uint8)
        return out

    def texture_table(self) -> np.ndarray:
        """(N,16,16,3): для блоков без текстуры — плоский цвет, чтобы индексация была равномерной."""
        out = np.zeros((len(self.names), 16, 16, 3), dtype=np.float32)
        for i, t in enumerate(self.tex):
            out[i] = t if t is not None else np.array(self.cols[i], dtype=np.float32)[None, None, :]
        return out

    def load(self, x0: int, z0: int, x1: int, z1: int, y0: int = 40, y1: int = 176) -> np.ndarray:
        sx, sz, sy = x1 - x0 + 1, z1 - z0 + 1, y1 - y0 + 1
        grid = np.zeros((sy, sx, sz), dtype=np.uint16)
        c0x, c1x = x0 >> 4, x1 >> 4
        c0z, c1z = z0 >> 4, z1 >> 4
        for rx in range(c0x >> 5, (c1x >> 5) + 1):
            for rz in range(c0z >> 5, (c1z >> 5) + 1):
                mca = self.dir / f"r.{rx}.{rz}.mca"
                if not mca.exists():
                    continue
                for (lx, lz), nbt in region_chunks(mca).items():
                    cx, cz = rx * 32 + lx, rz * 32 + lz
                    if not (c0x <= cx <= c1x and c0z <= cz <= c1z):
                        continue
                    self._chunk(grid, nbt, cx, cz, x0, z0, y0, y1)
        return grid

    def _collect_decks(self, nbt):
        """У каждой опоры моста (BridgePostBE) в NBT лежит весь Deck: a, b, a2, b2, style, breakFrom/To.

        Поэтому рендер берёт мост из мира как есть — ничего не восстанавливаем по четырём столбам.
        """
        for be in nbt.get("block_entities") or []:
            if not isinstance(be, dict) or be.get("id") != "riftworld:bridge_post":
                continue
            d = be.get("deck")
            if not isinstance(d, dict):
                continue
            try:
                deck = Deck(_pos(d["a"]), _pos(d["b"]), _pos(d["a2"]), _pos(d["b2"]),
                            int(d.get("style", 0)), float(d.get("breakFrom", 0.0)),
                            float(d.get("breakTo", 0.0)))
            except (KeyError, TypeError, ValueError):
                continue
            self.decks.setdefault(deck.key(), deck)

    def _chunk(self, grid, nbt, cx, cz, x0, z0, y0, y1):
        self._collect_decks(nbt)
        sections = nbt.get("sections", [])
        if isinstance(sections, dict):
            sections = [sections]
        for sec in sections:
            sy = sec.get("Y", 0)
            bs = sec.get("block_states")
            if not bs:
                continue
            palette = [p.get("Name") for p in bs.get("palette", []) if isinstance(p, dict)]
            ids = np.array([self.id_of(n or "minecraft:air") for n in palette], dtype=np.uint16)
            base_y = sy * 16
            if base_y + 15 < y0 or base_y > y1:
                continue
            data = bs.get("data")
            if data is None:            # палитра из одного блока: данных нет вовсе
                cells = np.zeros(4096, dtype=np.int32)
            else:
                bits = max(4, (int(len(palette)) - 1).bit_length())
                per = 64 // bits                       # блоков в одном long
                mask = np.uint64((1 << bits) - 1)
                # в NBT это знаковые long: отрицательные значения надо переинтерпретировать, а не конвертировать
                longs = np.asarray(data, dtype=np.int64).view(np.uint64)
                idx = np.arange(4096)
                cells = ((longs[idx // per] >> ((idx % per) * bits).astype(np.uint64)) & mask).astype(np.int32)
                cells = np.minimum(cells, max(0, len(ids) - 1))
            vals = ids[cells] if len(ids) else np.zeros(4096, dtype=np.uint16)
            vals = vals.reshape(16, 16, 16)  # (y, z, x) порядок Minecraft: y*256 + z*16 + x
            vals = np.transpose(vals, (0, 2, 1))  # -> (y, x, z)
            for yy in range(16):
                wy = base_y + yy
                if not (y0 <= wy <= y1):
                    continue
                gx = cx * 16 - x0
                gz = cz * 16 - z0
                sx0, sx1 = max(0, gx), min(grid.shape[1], gx + 16)
                sz0, sz1 = max(0, gz), min(grid.shape[2], gz + 16)
                if sx0 >= sx1 or sz0 >= sz1:
                    continue
                grid[wy - y0, sx0:sx1, sz0:sz1] = vals[yy, sx0 - gx:sx1 - gx, sz0 - gz:sz1 - gz]


def find_blocks(region: Path, names: tuple[str, ...], cx: int, cz: int, radius: int) -> list[tuple[int, int, int]]:
    """Где реально стоят блоки с такими именами (по палитрам секций) — чтобы камера наводилась на объект."""
    out = []
    rx0, rx1 = (cx - radius) >> 9, (cx + radius) >> 9
    rz0, rz1 = (cz - radius) >> 9, (cz + radius) >> 9
    want = tuple(f"riftworld:{n}" if ":" not in n else n for n in names)
    for rx in range(rx0, rx1 + 1):
        for rz in range(rz0, rz1 + 1):
            mca = region / f"r.{rx}.{rz}.mca"
            if not mca.exists():
                continue
            for (lx, lz), nbt in region_chunks(mca).items():
                chx, chz = rx * 32 + lx, rz * 32 + lz
                if abs(chx * 16 + 8 - cx) > radius + 16 or abs(chz * 16 + 8 - cz) > radius + 16:
                    continue
                for sec in nbt.get("sections") or []:
                    sy = sec.get("Y", 0)
                    pal = [p.get("Name") for p in ((sec.get("block_states") or {}).get("palette") or [])]
                    if any(n in want for n in pal):
                        out.append((chx * 16 + 8, sy * 16 + 8, chz * 16 + 8))
    return out


# ----------------------------------------------------------------------------- CPU-трассировка
def render(grid: np.ndarray, colors: np.ndarray, eye, target, size, fov=52.0, fog=(0.0, 0.0, 0.0),
           water: np.ndarray | None = None, textures: np.ndarray | None = None,
           alphas: np.ndarray | None = None, decks=(), deck_offset=(0.0, 0.0, 0.0),
           sky=((46, 68, 104), (128, 148, 168))):
    """numpy-DDA: каждый пиксель — луч, идём по сетке блоков, берём первую непрозрачную клетку.

    Вода (и прочие «полупрозрачные» id из water) не останавливает луч, а подмешивается к тому, что за ней — иначе
    океан выглядит серым листом жести, а на самом деле он прозрачный и в нём видно дно.
    """
    W, H = size
    sy, sx, sz = grid.shape
    air = np.flatnonzero(colors.sum(axis=1) == 0)
    solid = np.ones(len(colors), dtype=bool)
    solid[air] = False
    is_water = np.zeros(len(colors), dtype=bool)
    if water is not None:
        is_water = water
    opaque = solid & ~is_water

    ex, ey, ez = eye
    tx, ty, tz = target
    fwd = np.array([tx - ex, ty - ey, tz - ez], dtype=np.float64)
    fwd /= np.linalg.norm(fwd)
    right = np.cross(fwd, [0.0, 1.0, 0.0])
    right /= np.linalg.norm(right)
    up = np.cross(right, fwd)

    aspect = W / H
    th = math.tan(math.radians(fov) / 2)
    u = (np.arange(W) + 0.5) / W * 2 - 1
    v = 1 - (np.arange(H) + 0.5) / H * 2
    uu, vv = np.meshgrid(u * th * aspect, v * th)
    dirs = fwd[None, None, :] + uu[..., None] * right[None, None, :] + vv[..., None] * up[None, None, :]
    dirs = dirs.reshape(-1, 3)
    dirs /= np.linalg.norm(dirs, axis=1, keepdims=True)

    pos = np.tile(np.array([ex, ey, ez], dtype=np.float64), (dirs.shape[0], 1))
    cell = np.floor(pos).astype(np.int64)
    step = np.where(dirs > 0, 1, -1)
    with np.errstate(divide="ignore", invalid="ignore"):
        tdelta = np.abs(1.0 / np.where(dirs == 0, 1e-12, dirs))
        frac = pos - np.floor(pos)
        tmax = np.where(dirs > 0, (1.0 - frac) * tdelta, frac * tdelta)

    t_world = np.full(dirs.shape[0], np.inf)
    img = np.zeros((dirs.shape[0], 3), dtype=np.float64)   # накопленный «за» лучом цвет
    alpha = np.zeros(dirs.shape[0], dtype=np.float64)      # его непрозрачность
    done = np.zeros(dirs.shape[0], dtype=bool)
    depth = np.zeros(dirs.shape[0], dtype=np.float64)
    # свет: имитация солнца сверху-слева, чтобы грани читались и был объём
    sun = np.array([-0.45, 0.82, 0.36])
    sun /= np.linalg.norm(sun)
    tex_mean = textures.mean(axis=(1, 2)) if textures is not None else None
    active = np.arange(dirs.shape[0])
    maxsteps = int(max(sx, sy, sz) * 3)
    for _ in range(maxsteps):
        if active.size == 0:
            break
        c = cell[active]
        inside = ((c[:, 0] >= 0) & (c[:, 0] < sx) & (c[:, 1] >= 0) & (c[:, 1] < sy)
                  & (c[:, 2] >= 0) & (c[:, 2] < sz))
        hitid = np.zeros(active.size, dtype=np.uint16)
        if inside.any():
            ii = np.flatnonzero(inside)
            hitid[ii] = grid[c[ii, 1], c[ii, 0], c[ii, 2]]
        hid = np.clip(hitid, 0, len(solid) - 1)
        hit_o = inside & opaque[hid]
        hit_w = inside & is_water[hid] & ~hit_o
        if alphas is not None:
            # cutout: трава, цветы, листва — если тексель прозрачный, луч идёт дальше
            cand = np.flatnonzero(hit_o)
            if cand.size:
                iv, iu = hit_uv(active[cand])
                a = alphas[hid[cand], iv, iu]
                see = a < 128
                if see.any():
                    hit_o[cand[see]] = False

        step_t = np.min(tmax[active], axis=1)

        def shade_of(idx_active, ids):
            """Освещение грани по нормали + настоящий тексель из текстуры блока в точке попадания."""
            ax = np.argmin(tmax[idx_active], axis=1)
            normals = np.zeros((idx_active.size, 3), dtype=np.float64)
            normals[np.arange(idx_active.size), ax] = -step[idx_active, ax]
            diff = np.clip(normals @ sun, 0, 1)
            face = 0.58 + 0.42 * diff
            top = (normals[:, 1] > 0.5)
            face = np.where(top, 0.78 + 0.22 * diff, face)
            if textures is None:
                return colors[ids] * face[:, None]
            # точка попадания: шаг + оставшееся расстояние до грани вдоль главной оси
            t_hit = tmax[idx_active, ax]
            p = pos[idx_active] + dirs[idx_active] * t_hit[:, None]
            u = np.where(ax == 0, p[:, 2], p[:, 0])
            v = np.where(ax == 1, p[:, 2], p[:, 1])
            iu = np.clip(((u - np.floor(u)) * 16).astype(np.int64), 0, 15)
            iv = np.clip(((v - np.floor(v)) * 16).astype(np.int64), 0, 15)
            tex = textures[ids, iv, iu]
            # «мип-уровень»: вдали 16x16 тексель на пиксель даёт белую рябь — подмешиваем средний цвет блока,
            # как это делает сама игра (у нас нет мипмапов, поэтому считаем грубо, по расстоянию)
            m = np.clip((t_hit - 4.0) / 26.0, 0.0, 0.85)[:, None]
            tex = tex * (1 - m) + tex_mean[ids] * m
            return tex * face[:, None]

        def hit_uv(idx_active):
            ax = np.argmin(tmax[idx_active], axis=1)
            t_hit = tmax[idx_active, ax]
            p = pos[idx_active] + dirs[idx_active] * t_hit[:, None]
            u = np.where(ax == 0, p[:, 2], p[:, 0])
            v = np.where(ax == 1, p[:, 2], p[:, 1])
            iu = np.clip(((u - np.floor(u)) * 16).astype(np.int64), 0, 15)
            iv = np.clip(((v - np.floor(v)) * 16).astype(np.int64), 0, 15)
            return iv, iu

        if hit_w.any():
            widx = active[hit_w]
            ids = hid[hit_w]
            a = 0.30                                          # вода: подмешиваем и идём дальше
            col = shade_of(widx, ids)
            # лёгкий голубой сдвиг воды, иначе дно просвечивает «серым»
            col = col * 0.75 + np.array([40, 90, 140])[None, :] * 0.25
            img[widx] = img[widx] * (1 - a) + col * a
            alpha[widx] = alpha[widx] * (1 - a) + a
            depth[widx] = step_t[hit_w]

        if hit_o.any():
            oidx = active[hit_o]
            ids = hid[hit_o]
            base = shade_of(oidx, ids)
            t = step_t[hit_o]
            fogf = np.clip(1.0 - np.exp(-(t / 300.0) ** 2), 0, 1)
            base = base * (1 - fogf[:, None]) + np.array(fog)[None, :] * fogf[:, None]
            a = 1 - alpha[oidx]
            img[oidx] = base * a[:, None] + img[oidx] * (1 - a[:, None])
            alpha[oidx] = 1.0
            done[oidx] = True
            t_world[oidx] = t

        blocked = hit_o | hit_w
        active = active[~blocked]
        if active.size == 0:
            break
        ax = np.argmin(tmax[active], axis=1)
        cell[active, ax] += step[active, ax]
        tmax[active, ax] += tdelta[active, ax]

    # небо — под то, что осталось непрозрачным не до конца (например, после воды)
    rest = ~done
    if rest.any():
        vv_flat = vv.reshape(-1)[:, None]
        mixv = (vv_flat * 0.5 + 0.5)
        top, horizon = np.array(sky[0], dtype=np.float64)[None, :], np.array(sky[1], dtype=np.float64)[None, :]
        skycol = horizon * (1 - mixv) + top * mixv
        a = (1 - alpha[rest])[:, None]
        img[rest] = skycol[rest] * a + img[rest] * (1 - a)
    # мосты рисуем ПОСЛЕ неба: иначе alpha<1 в небе затрёт настил
    if len(decks):
        img = composite_decks(img, t_world, dirs,
                              np.array([ex, ey, ez], dtype=np.float64) + np.array(deck_offset, dtype=np.float64),
                              decks)
    return np.clip(img, 0, 255).astype(np.uint8).reshape(H, W, 3)


def composite_decks(img: np.ndarray, t_world: np.ndarray, dirs: np.ndarray, origin: np.ndarray, decks) -> np.ndarray:
    """Второй проход: аналитическая геометрия мостов (доски, балки, тросы, подвески) с проверкой глубины.

    Всё считается в локальном базисе моста, поэтому доски и балки — обычные axis-aligned боксы, а тросы —
    цепочка боксов вдоль катенары. Мир при этом остаётся воксельным: мост кладётся поверх только там,
    где он ближе блоков.
    """
    out = img.copy()
    n_pix = dirs.shape[0]
    for deck in decks:
        lo, hi = deck.bbox_world()
        with np.errstate(divide="ignore", invalid="ignore"):
            inv = 1.0 / np.where(dirs == 0, 1e-12, dirs)
            t0 = (lo[None, :] - origin[None, :]) * inv
            t1 = (hi[None, :] - origin[None, :]) * inv
            tnear = np.minimum(t0, t1).max(axis=1)
            tfar = np.maximum(t0, t1).min(axis=1)
        cand = np.flatnonzero((tfar > np.maximum(tnear, 0.0)) & (tnear < t_world))
        if cand.size == 0:
            continue
        d = dirs[cand]
        ol, dl = deck.to_local(origin, d)   # ol — точка (3,), dl — направления (N,3)
        with np.errstate(divide="ignore", invalid="ignore"):
            invl = 1.0 / np.where(dl == 0, 1e-12, dl)

        best_t = np.full(cand.size, np.inf)
        best_box = np.full(cand.size, -1, dtype=np.int32)
        best_axis = np.zeros(cand.size, dtype=np.int8)
        boxes = deck_boxes(deck)
        for bi, (x0, x1, y0, y1, z0, z1, kind, u0, u1) in enumerate(boxes):
            with np.errstate(divide="ignore", invalid="ignore"):
                ax0 = (x0 - ol[0]) * invl[:, 0]
                ax1 = (x1 - ol[0]) * invl[:, 0]
                ay0 = (y0 - ol[1]) * invl[:, 1]
                ay1 = (y1 - ol[1]) * invl[:, 1]
                az0 = (z0 - ol[2]) * invl[:, 2]
                az1 = (z1 - ol[2]) * invl[:, 2]
                nx = np.minimum(ax0, ax1)
                ny = np.minimum(ay0, ay1)
                nz = np.minimum(az0, az1)
                fx = np.maximum(ax0, ax1)
                fy = np.maximum(ay0, ay1)
                fz = np.maximum(az0, az1)
                near = np.maximum(np.maximum(nx, ny), nz)
                far = np.minimum(np.minimum(fx, fy), fz)
            hit = (far > np.maximum(near, 0.0)) & (near > 0) & (near < best_t)
            if not hit.any():
                continue
            axes = np.argmax(np.stack([nx, ny, nz], axis=1), axis=1)
            best_t = np.where(hit, near, best_t)
            best_box = np.where(hit, bi, best_box)
            best_axis = np.where(hit, axes, best_axis)

        painted = best_box >= 0
        if not painted.any():
            continue
        pidx = cand[painted]
        bi = best_box[painted]
        tt = best_t[painted]
        ax = best_axis[painted]
        loc = ol[None, :] + dl[painted] * tt[:, None]
        u = np.clip(loc[:, 0] / max(1e-9, deck.len), 0, 1)
        w = np.clip((loc[:, 2] - deck.z0) / max(1e-9, deck.width), 0, 1)
        kind = np.array([boxes[b][6] for b in bi])
        # свет: верх настила светлее, бока темнее; трос и подвески — тёмный металл/пенька
        shade = np.where(ax == 1, 1.0, np.where(ax == 0, 0.82, 0.74))
        # какая грань: если луч идёт вниз и попал по y — это верхняя поверхность
        top_face = (ax == 1) & (dl[painted][:, 1] < 0)
        shade = np.where(top_face, 1.05, shade)
        col = np.zeros((pidx.size, 3))
        plank = kind == 0
        if plank.any():
            n = max(2, int(np.ceil(deck.len / PLANK_STEP)))
            iu = np.clip((w[plank] * 16).astype(int), 0, 15)
            iv = np.clip((((u[plank] * n) % 1.0) * 16).astype(int), 0, 15)
            col[plank] = deck.plank_tex[iv, iu]
        col[~plank] = deck.cable_col[None, :]
        out[pidx] = np.clip(col * shade[:, None], 0, 255)
    return out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--centre", nargs=2, type=int, default=[1374, 240])
    ap.add_argument("--radius", type=int, default=112)
    ap.add_argument("--size", default="960x540")
    ap.add_argument("--out", default=None)
    ap.add_argument("--dim", default=str(DEFAULT_REGION))
    ap.add_argument("--eye", nargs=3, type=float, default=None)
    ap.add_argument("--eye-y", type=float, default=146.0)
    ap.add_argument("--target-y", type=float, default=100.0)
    ap.add_argument("--dist", type=float, default=150.0, help="расстояние камеры от центра кадра")
    ap.add_argument("--fov", type=float, default=52.0)
    ap.add_argument("--yaw", type=float, default=40.0, help="поворот вокруг вертикали, градусы")
    ap.add_argument("--ss", type=int, default=1, help="суперсэмплинг: 2 = рендер вдвое крупнее и усреднение")
    ap.add_argument("--ymin", type=int, default=0)
    ap.add_argument("--ymax", type=int, default=176)
    ap.add_argument("--focus", default=None,
                    help="имена блоков через запятую: камера сама наведётся на них (например bridge_post,bridge_walk)")
    ap.add_argument("--dimension", choices=("overworld", "nether"), default="overworld",
                    help="небо и дымка под измерение (в аду сизо-бурое небо без солнца)")
    ap.add_argument("--deck-cam", action="store_true",
                    help="кадрировать мост: камера ставится сбоку от ближайшего настила, а не от центра карты")
    ap.add_argument("--check", action="store_true", help="самопроверка рендерера (CPU-only, детерминизм)")
    args = ap.parse_args()

    if args.check:
        return check(Path(args.dim))

    W, H = (int(v) for v in args.size.lower().split("x"))
    cx, cz = args.centre
    if args.focus:
        hits = find_blocks(Path(args.dim), tuple(args.focus.split(",")), cx, cz, args.radius)
        if hits:
            xs = [h[0] for h in hits]; ys = [h[1] for h in hits]; zs = [h[2] for h in hits]
            cx, cz = (min(xs) + max(xs)) // 2, (min(zs) + max(zs)) // 2
            args.target_y = max(ys) - 2
            if not args.eye:
                args.eye_y = max(ys) + 10
                print(f"[фокус] {len(hits)} секций с {args.focus}: центр {cx},{cz}, верх y={max(ys)}")
        else:
            print(f"[фокус] блоки {args.focus} в радиусе не найдены — камера по центру")
    r = args.radius
    t0 = time.time()
    w = World(Path(args.dim))
    grid = w.load(cx - r, cz - r, cx + r, cz + r, args.ymin, args.ymax)
    colors = np.array(w.cols, dtype=np.float64)
    hist = np.bincount(grid.ravel(), minlength=len(w.names))
    top = np.argsort(hist)[::-1][:8]
    print(f"[мир] {grid.shape[2]}x{grid.shape[1]} блоков, блоков не-air: {int((hist[1:] > 0).sum())} видов")
    for i in top:
        if hist[i]:
            print(f"       {w.names[i]}: {hist[i]}")
    yaw = math.radians(args.yaw)
    dist = args.dist
    if args.eye:
        ex, eye_y, ez = args.eye
    else:
        ex, eye_y, ez = cx + math.cos(yaw) * dist, args.eye_y, cz + math.sin(yaw) * dist
    tx, ty, tz = cx, args.target_y, cz
    print(f"[камера] глаз {ex:.0f},{eye_y:.0f},{ez:.0f} -> цель {tx},{ty},{tz}, fov {args.fov}")
    water = np.zeros(len(w.names), dtype=bool)
    for i, n in enumerate(w.names):
        water[i] = n.endswith("_water") or n == "minecraft:water"
    ss = max(1, args.ss)
    near = [d for d in w.decks.values()
            if abs((d.a[0] + d.b[0]) / 2 - cx) < r + 32 and abs((d.a[2] + d.b[2]) / 2 - cz) < r + 32]
    if args.deck_cam and near:
        # камера «как фотограф»: сбоку от пролёта, чуть выше настила, мост занимает кадр целиком
        d0 = min(near, key=lambda dd: float(np.linalg.norm((dd.a + dd.b) / 2 - np.array([cx, 0, cz]))))
        c = (d0.a + d0.b + d0.a2 + d0.b2) / 4
        side = np.cross(d0.e1, np.array([0.0, 1.0, 0.0]))
        side /= max(1e-9, float(np.linalg.norm(side)))
        if float(np.dot(side, np.array([ex - c[0], 0, ez - c[2]]))) < 0:   # остаёмся на той же стороне, что и раньше
            side = -side
        # длинный пролёт (50 блоков) снимаем не «с дистанции длины»: между камерой и мостом оказывается склон,
        # поэтому держимся рядом и расширяем угол, а не отъезжаем
        dist_deck = max(7.0, min(26.0, d0.len * 0.72))
        e = (c + side * dist_deck + d0.e1 * (d0.len * 0.08)
             + np.array([0.0, max(2.4, min(6.5, d0.len * 0.19)), 0.0]))
        ex, ez, eye_y = float(e[0]), float(e[2]), float(e[1])
        tx, ty, tz = float(c[0]), float(c[1] - 0.25), float(c[2])
        args.fov = max(args.fov, 55.0)
        print(f"[мост-камера] пролёт {d0.len:.1f}×{d0.width:.1f}, глаз {ex:.1f},{eye_y:.1f},{ez:.1f} -> "
              f"центр настила {tx:.1f},{ty:.1f},{tz:.1f}")
    if near:
        print(f"[мосты] в кадре {len(near)}: " + ", ".join(
            f"style {d.style_id} {int(d.len)}×{int(d.width)}" for d in near))
    sky = ((46, 68, 104), (128, 148, 168)) if args.dimension == "overworld" else ((58, 26, 22), (150, 74, 42))
    img = render(grid, colors, (ex - (cx - r), eye_y - args.ymin, ez - (cz - r)),
                 (tx - (cx - r), ty - args.ymin, tz - (cz - r)), (W * ss, H * ss), fov=args.fov, water=water,
                 textures=w.texture_table(), alphas=w.alpha_table(), decks=near,
                 deck_offset=(cx - r, args.ymin, cz - r), sky=sky)
    if ss > 1:
        from PIL import Image as _I
        img = np.asarray(_I.fromarray(img).resize((W, H), _I.LANCZOS))
    out = Path(args.out or f"shots/cpu_{cx}_{cz}.png")
    out.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(img).save(out)
    print(f"[рендер] {out} ({out.stat().st_size // 1024} КБ) за {time.time() - t0:.1f} с на CPU")
    return 0


def check(dim: Path) -> int:
    """Самопроверка: рендерер действительно процессорный, детерминированный и видит мир."""
    print("== самопроверка CPU-рендерера")
    bad = [m for m in ("OpenGL", "moderngl", "pyglet", "glfw", "vulkan", "torch.cuda")
           if m in sys.modules]
    print(f"[OK] графика не импортирована: {bad or 'ни одного GL/Vulkan-модуля'}" if not bad else f"[FAIL] {bad}")
    ok = not bad
    w = World(dim)
    grid = w.load(-1216 - 48, -1217 - 48, -1216 + 48, -1217 + 48)
    colors = np.array(w.cols, dtype=np.float64)
    water = np.array([n.endswith("_water") or n == "minecraft:water" for n in w.names])
    a = render(grid, colors, (0, 120, 0), (60, 80, 60), (160, 90), water=water)
    b = render(grid, colors, (0, 120, 0), (60, 80, 60), (160, 90), water=water)
    same = hashlib.sha256(a.tobytes()).hexdigest() == hashlib.sha256(b.tobytes()).hexdigest()
    print(f"[{'OK' if same else 'FAIL'}] детерминизм: два прогона дают одинаковый кадр")
    ok &= same
    nonempty = len(np.unique(a.reshape(-1, 3), axis=0)) > 20
    print(f"[{'OK' if nonempty else 'FAIL'}] кадр не пустой: {len(np.unique(a.reshape(-1, 3), axis=0))} цветов")
    ok &= nonempty
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
