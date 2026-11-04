"""Рев 15: оффлайн-модель биома «Фиолетовые холмы» (референс reports/refs_rev4/04_violet_hills.png).

Скрипт повторяет ТУ ЖЕ математику, что и Java-генератор (ElisarChunkGenerator + VioletHills),
и рисует картинку «как из игры»: перспективный рендер heightmap-а (Voxel Space) + вид сверху.
Так дизайн холмов можно крутить за секунды, не гоняя 30-минутный CI с настоящим клиентом.

usage: python3 tools/rift/sim_violet.py  -> reports/preview_violet/*.png
"""
import math
import os
import numpy as np
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "../../reports/preview_violet")
M64 = np.uint64(0xFFFFFFFFFFFFFFFF)

# ---------------------------------------------------------------- хеши/шум 1:1 с Java
K1 = np.uint64(0x9E3779B97F4A7C15)
K2 = np.uint64(0xC2B2AE3D27D4EB4F)
K3 = np.uint64(0x165667B19E3779F9)
C1 = np.uint64(0xFF51AFD7ED558CCD)
C2 = np.uint64(0xC4CEB9FE1A85EC53)


def _u64(x):
    return np.asarray(x).astype(np.uint64)


def hashv(x, z, s):
    h = (_u64(x) * K1) ^ (_u64(z) * K2) ^ (np.uint64(s) * K3)
    h ^= h >> np.uint64(33)
    h = h * C1
    h ^= h >> np.uint64(33)
    h = h * C2
    h ^= h >> np.uint64(33)
    return (h >> np.uint64(11)).astype(np.float64) * (2.0 ** -53)


def vnoise(x, z, s):
    xi = np.floor(x)
    zi = np.floor(z)
    fx = x - xi
    fz = z - zi
    fx = fx * fx * (3 - 2 * fx)
    fz = fz * fz * (3 - 2 * fz)
    xi = xi.astype(np.int64)
    zi = zi.astype(np.int64)
    a = hashv(xi, zi, s)
    b = hashv(xi + 1, zi, s)
    c = hashv(xi, zi + 1, s)
    d = hashv(xi + 1, zi + 1, s)
    ab = a + (b - a) * fx
    cd = c + (d - c) * fx
    return ab + (cd - ab) * fz


def smooth(t):
    t = np.clip(t, 0.0, 1.0)
    return t * t * (3 - 2 * t)


# ---------------------------------------------------------------- остров (CityPlan.Island)
class Island:
    def __init__(self, cx, cz, R, base, depth, p1, p2, p3, kind=7, hill=0.9):
        self.cx, self.cz, self.R, self.base, self.depth = cx, cz, R, base, depth
        self.p1, self.p2, self.p3, self.kind, self.hill = p1, p2, p3, kind, hill
        self.elong, self.elongAng = 0.0, 0.0

    def shapeR(self, th):
        return self.R * (1 + 0.07 * np.sin(3 * th + self.p1) + 0.045 * np.sin(5 * th + self.p2)
                         + 0.025 * np.sin(11 * th + self.p3))

    def t(self, x, z):
        dx, dz = x - self.cx, z - self.cz
        if self.elong == 0.0:
            return 1 - np.hypot(dx, dz) / self.shapeR(np.arctan2(dz, dx))
        ca, sa = np.cos(self.elongAng), np.sin(self.elongAng)
        u = (dx * ca + dz * sa) / (1 + self.elong)
        v = (-dx * sa + dz * ca) * (1 + self.elong)
        return 1 - np.hypot(u, v) / self.shapeR(np.arctan2(v, u))


OCEAN = 62

# ---------------------------------------------------------------- ПАРАМЕТРЫ ДИЗАЙНА (крутить здесь)
P = {
    "R": 100.0,            # радиус луга (Java: 92..112)
    "rise": 30.0,          # высота холма над морем (Java: base = ocean+26..34)
    "dome_pow": 1.02,
    "wave1": 6.2,          # крупные волны рельефа
    "wave1_f": 0.0060,
    "wave2": 2.4,
    "wave2_f": 0.0165,
    "wave3": 0.8,
    "wave3_f": 0.047,
    "step": 2,             # высота яруса террасы (VIOLET_STEP)
    "row_h": 1.15,         # одна гряда на 1.15 блока подъёма (полосы по контуру)
    "row_cover": 0.70,
    "field_f": 0.0068,     # масштаб пятен полей
    "field_thr": 0.34,
    "edge_grass": 2,       # ширина травяной кромки яруса
}


def terrain(x, z, isl, p=P):
    """Возвращает (top, soft, t) — ровно как ElisarChunkGenerator.violetProfile + violetTerrace."""
    t = np.clip(isl.t(x, z), 0.0, 1.0)
    rim = OCEAN + 2.5
    dome = rim + (isl.base - rim) * np.power(t, p["dome_pow"])
    dome += p["wave1"] * (vnoise(x * p["wave1_f"], z * p["wave1_f"], 71) - 0.5) * np.sqrt(t)
    dome += p["wave2"] * (vnoise(x * p["wave2_f"], z * p["wave2_f"], 72) - 0.5) * t
    dome += p["wave3"] * (vnoise(x * p["wave3_f"], z * p["wave3_f"], 73) - 0.5) * t
    top = np.floor(dome / p["step"]) * p["step"] + 2.0
    return top, dome, t


# ---------------------------------------------------------------- карта блока для колонки
COL = {
    "grass":   (118, 162, 84),
    "grass2":  (104, 146, 74),
    "lav":     (138, 106, 196),
    "lav2":    (122, 92, 180),
    "lav_hi":  (166, 138, 220),
    "soil":    (118, 92, 66),
    "water":   (70, 146, 184),
    "rock":    (198, 190, 172),
    "canopy":  (178, 148, 216),
    "trunk":   (92, 70, 54),
}


def rim_mask(top, step, width=3):
    """Полоса травы вдоль кромки яруса: клетки, у которых в пределах `width` блоков есть сосед
    уровнем ниже (то есть рядом обрыв). Так же Java-генератор ищет кромку по сетке колонок."""
    m = np.zeros_like(top, dtype=bool)
    for r in range(1, width + 1):
        for dx, dz in ((r, 0), (-r, 0), (0, r), (0, -r)):
            nb = np.roll(np.roll(top, dz, axis=0), dx, axis=1)
            m |= nb <= top - (step - 1)
    return m


def surface_color(x, z, top, soft, t, isl, p=P):
    """Цвет верха колонки + высота доп. блоков (лаванда 2 блока) — как в Java."""
    h = np.zeros(x.shape, dtype=np.float64)
    col = np.zeros(x.shape + (3,), dtype=np.float64)

    lip = rim_mask(top, p["step"], p["edge_grass"])
    meadow = vnoise(x * p["field_f"], z * p["field_f"], 92) < p["field_thr"]

    rp = soft / p["row_h"]
    frac = rp - np.floor(rp)
    row = frac < p["row_cover"]

    lav = (~lip) & (~meadow) & row
    furrow = (~lip) & (~meadow) & (~row)
    grass = ~lav & ~furrow

    col[grass] = np.array(COL["grass"])
    col[furrow] = np.array(COL["soil"])
    col[lav] = np.array(COL["lav"])
    v2 = vnoise(x * 0.35, z * 0.35, 97)
    col[grass & (v2 > 0.55)] = np.array(COL["grass2"])
    col[lav & (v2 > 0.62)] = np.array(COL["lav2"])
    col[lav & (v2 > 0.90)] = np.array(COL["lav_hi"])
    # редкие пятна лаванды на пастбище
    m_lav = grass & (vnoise(x * 0.9, z * 0.9, 96) > 0.86)
    col[m_lav] = np.array(COL["lav2"])
    h[m_lav] = 1.0
    h[grass & (vnoise(x * 1.7, z * 1.7, 99) > 0.93)] = 0.6
    h[lav] = 0.85          # в симуляторе лаванда — низкий ковёр (в игре это крестики-растения)
    return col, h, lav


def build(cx, cz, size=340, p=P):
    isl = Island(cx, cz, p["R"], OCEAN + p["rise"], 20, 1.3, 4.1, 4.3, 7, 0.9)
    isl.elong, isl.elongAng = 0.34, 0.6
    xs = np.arange(cx - size, cx + size, dtype=np.float64) + 0.5
    zs = np.arange(cz - size, cz + size, dtype=np.float64) + 0.5
    X, Z = np.meshgrid(xs, zs)
    top, soft, t = terrain(X, Z, isl, p)
    inside = isl.t(X, Z) > 0
    top = np.where(inside, top, OCEAN)
    soft = np.where(inside, soft, OCEAN)
    col, h, lav = surface_color(X, Z, top, soft, t, isl, p)
    # море вокруг
    sea = ~inside
    col[sea] = np.array(COL["water"])
    return dict(X=X, Z=Z, top=top, soft=soft, col=col, h=h, lav=lav, sea=sea, isl=isl, size=size)


def render_perspective(sc, path, cam=(0, 0, 0), yaw=0.0, pitch=-0.06, fov=1.05, W=1280, H=640,
                       fog=0.0022, fogcol=(238, 198, 196)):
    """Классический Voxel Space: идём по колонке от ближнего к дальнему и рисуем вертикальные
    отрезки, пока верх экрана не заполнен. Даёт правильное перекрытие холмов."""
    top = sc["top"] + sc["h"]
    col = sc["col"]
    Hs, Ws = top.shape
    xs0, zs0 = sc["X"][0, 0], sc["Z"][0, 0]
    img = np.zeros((H, W, 3), dtype=np.float64)
    sky_top = np.array([188, 162, 212], dtype=np.float64)
    sky_bot = np.array([250, 210, 198], dtype=np.float64)
    f = (np.arange(H) / H)[:, None]
    img[:] = (sky_top[None, :] * (1 - f) + sky_bot[None, :] * f)[:, None, :]
    cx, cy, cz = cam
    horizon = H * 0.5 - pitch * H
    focal = (W * 0.5) / math.tan(fov * 0.5)
    fogc = np.array(fogcol, dtype=np.float64)
    for sx in range(W):
        ang = yaw + math.atan((sx - W * 0.5) / focal)
        dx, dz = math.cos(ang), math.sin(ang)
        ybuf = H
        d = 2.0
        while d < 460.0 and ybuf > 0:
            px = cx + dx * d
            pz = cz + dz * d
            ix = int(px - xs0)
            iz = int(pz - zs0)
            if 0 <= ix < Ws and 0 <= iz < Hs:
                hh = top[iz, ix]
                py = horizon - (hh - cy) * focal / d
                if py < ybuf:
                    y_from = max(0, int(round(py)))
                    y_to = min(H, int(round(ybuf)))
                    if y_to > y_from:
                        ff = min(1.0, d * fog)
                        shade = 0.70 + 0.30 * min(1.0, max(0.0, (hh - OCEAN) / 34.0))
                        c = col[iz, ix] * shade
                        c = c * (1 - ff) + fogc * ff
                        img[y_from:y_to, sx, :] = c
                    ybuf = y_from
            d += 0.9
    Image.fromarray(np.clip(img, 0, 255).astype(np.uint8)).save(path)
    return path


def render_top(sc, path, scale=2):
    col = sc["col"]
    top = sc["top"]
    shade = np.clip(0.55 + (top - OCEAN) / 60.0, 0.5, 1.15)
    img = np.clip(col * shade[..., None], 0, 255).astype(np.uint8)
    im = Image.fromarray(img).resize((img.shape[1] // scale, img.shape[0] // scale), Image.LANCZOS)
    im.save(path)
    return path


def surface_at(sc, x, z):
    ix = int(x - sc["X"][0, 0])
    iz = int(z - sc["Z"][0, 0])
    return float(sc["top"][iz, ix])


def shot(sc, name, x, z, yaw_deg, eye=9.0, pitch=-0.12, W=1280, H=640):
    y = surface_at(sc, x, z) + eye
    return render_perspective(sc, os.path.join(OUT, name), cam=(x, y, z), yaw=math.radians(yaw_deg),
                              pitch=pitch, W=W, H=H)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    sc = build(0, 0, size=300)
    render_top(sc, os.path.join(OUT, "top.png"))
    shot(sc, "persp_slope.png", 70, 95, 250, eye=12)      # с пологого склона вверх, по грядам
    shot(sc, "persp_close.png", 20, 55, 265, eye=6)       # вблизи, среди лаванды
    shot(sc, "persp_ridge.png", -85, -40, 40, eye=14)     # с гребня вниз через поля
    print("OK")
