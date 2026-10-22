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
}


class World:
    """Кусок мира в виде 3D-массива id блоков + таблица цветов."""

    def __init__(self, region_dir: Path):
        self.dir = region_dir
        self.ids: dict[str, int] = {}
        self.cols: list[tuple[int, int, int]] = [(0, 0, 0)]
        self.names: list[str] = ["minecraft:air"]
        self.tex: list[np.ndarray | None] = [None]
        self.alp: list[np.ndarray | None] = [None]

    def id_of(self, name: str) -> int:
        if name in self.ids:
            return self.ids[name]
        i = len(self.names)
        self.ids[name] = i
        self.names.append(name)
        self.cols.append(mod_color(name) or VANILLA.get(name, (150, 150, 150)))
        self.tex.append(mod_texture(name))
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

    def _chunk(self, grid, nbt, cx, cz, x0, z0, y0, y1):
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
           alphas: np.ndarray | None = None):
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

    img = np.zeros((dirs.shape[0], 3), dtype=np.float64)   # накопленный «за» лучом цвет
    alpha = np.zeros(dirs.shape[0], dtype=np.float64)      # его непрозрачность
    done = np.zeros(dirs.shape[0], dtype=bool)
    depth = np.zeros(dirs.shape[0], dtype=np.float64)
    # свет: имитация солнца сверху-слева, чтобы грани читались и был объём
    sun = np.array([-0.45, 0.82, 0.36])
    sun /= np.linalg.norm(sun)
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
        top, horizon = np.array([46, 68, 104])[None, :], np.array([128, 148, 168])[None, :]
        skycol = horizon * (1 - mixv) + top * mixv
        a = (1 - alpha[rest])[:, None]
        img[rest] = skycol[rest] * a + img[rest] * (1 - a)
    return np.clip(img, 0, 255).astype(np.uint8).reshape(H, W, 3)


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
    img = render(grid, colors, (ex - (cx - r), eye_y - args.ymin, ez - (cz - r)),
                 (tx - (cx - r), ty - args.ymin, tz - (cz - r)), (W * ss, H * ss), fov=args.fov, water=water,
                 textures=w.texture_table(), alphas=w.alpha_table())
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
