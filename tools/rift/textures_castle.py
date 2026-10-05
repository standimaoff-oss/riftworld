"""Rev 3 / C-1 textures: the castle «Чертог Тлеющего Рога» and the scorched temporary site (all 16x16, generated).

Palette after the references (Malbork brick gothic, Tantallon red ruins, Malbork Grand Refectory):
  * castle_bricks(_cracked/_mossy) - deep red-orange brick, mixed burnt headers, pale lime mortar
  * ashlar(+pillar top/side)        - grey dressed stone: plinths, string courses, window frames, columns
  * dark_planks / dark_beam          - near-black oak of floors, galleries and trusses
  * brass_block / castle_mosaic      - the rare yellow accents
  * stained_glass                    - lead cames with red / amber / deep-blue cells (translucent pane)
  * castle_roof                      - dark red-brown scalloped tiles
  * ash_block / burnt_soil / scorched_stone / charred_log - surface of the scorched site (preview of the Hell lands)
Also writes model textures (tex_models/) used by the Blender props candle_stand / boss_throne.
"""
import os
import numpy as np
from PIL import Image

ROOT = "/home/user/mods/riftworld/src/main/resources/assets/riftworld/textures/block"
TM = "/home/user/tools/rift/tex_models"
os.makedirs(ROOT, exist_ok=True)
os.makedirs(TM, exist_ok=True)


def rgb(h):
    h = h.lstrip('#')
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], float)


def save(a, name, d=ROOT):
    Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(d, name + ".png"))


def base(col, seed, amp=6, n=16):
    r = np.random.RandomState(seed)
    a = np.zeros((n, n, 4))
    a[..., :3] = rgb(col) + ((r.rand(n, n) - 0.5) * 2 * amp)[..., None]
    a[..., 3] = 255
    return a


def smooth_noise(n, seed, scale=4):
    r = np.random.RandomState(seed)
    g = r.rand(n // scale + 2, n // scale + 2)
    out = np.zeros((n, n))
    for y in range(n):
        for x in range(n):
            gx, gy = x / scale, y / scale
            x0, y0 = int(gx), int(gy)
            fx, fy = gx - x0, gy - y0
            out[y, x] = (g[y0, x0] * (1 - fx) * (1 - fy) + g[y0, x0 + 1] * fx * (1 - fy) + g[y0 + 1, x0] * (1 - fx) * fy
                         + g[y0 + 1, x0 + 1] * fx * fy)
    return out - 0.5


# ------------------------------------------------------------------ red brick
BRICK_COLS = ['#93402c', '#8a3826', '#a2492f', '#7d3122', '#9a4530', '#6c2c20', '#a85236']
MORTAR = '#a39684'


def bricks(seed, cracked=False, mossy=False):
    r = np.random.RandomState(seed)
    a = np.zeros((16, 16, 4))
    a[..., 3] = 255
    a[..., :3] = rgb(MORTAR)
    for row in range(4):
        y0 = row * 4
        off = 0 if row % 2 == 0 else 4
        for k in range(3):
            x0 = off + k * 8 - 8
            col = rgb(BRICK_COLS[r.randint(len(BRICK_COLS))])
            burnt = r.rand() < 0.18
            for y in range(y0 + 1, y0 + 4):
                for x in range(x0 + 1, x0 + 8):
                    if 0 <= x < 16:
                        c = col * (0.72 if burnt else 1.0) + (r.rand() - 0.5) * 14
                        if y == y0 + 1:
                            c = c * 1.08                       # top edge catches the light
                        if y == y0 + 3:
                            c = c * 0.9
                        a[y, x % 16, :3] = c
            # wrap the brick that crosses the edge
            if x0 < 0:
                for y in range(y0 + 1, y0 + 4):
                    for x in range(16 + x0 + 1, 16):
                        a[y, x, :3] = col * (0.72 if burnt else 1.0) + (r.rand() - 0.5) * 14
    # mortar speckle
    m = (a[..., 0] > 160)
    a[m, :3] += ((r.rand(16, 16) - 0.5) * 18)[m][:, None]
    if cracked:
        x, y = r.randint(3, 13), 0
        while y < 16:
            a[y, x % 16, :3] = rgb('#3a1d16')
            if r.rand() < 0.4:
                a[y, (x + 1) % 16, :3] *= 0.75
            x += r.choice((-1, 0, 0, 1)); y += 1
        for _ in range(3):                                   # chipped corners
            cx, cy = r.randint(0, 16), r.randint(0, 16)
            for dx in range(2):
                for dy in range(2):
                    a[(cy + dy) % 16, (cx + dx) % 16, :3] = rgb('#5a2a1e') + r.rand() * 10
    if mossy:
        n = smooth_noise(16, seed + 9, 4)
        for y in range(16):
            for x in range(16):
                if n[y, x] + (0.12 if y > 10 else 0) > 0.24 or (a[y, x, 0] > 150 and y > 11 and r.rand() < 0.3):
                    a[y, x, :3] = rgb('#55613a') + (r.rand() - 0.5) * 20
    return a


save(bricks(3101), "castle_bricks")
save(bricks(3102, cracked=True), "castle_bricks_cracked")
save(bricks(3103, mossy=True), "castle_bricks_mossy")


# ------------------------------------------------------------------ grey ashlar (two courses of big dressed blocks)
def ashlar(seed):
    r = np.random.RandomState(seed)
    a = base('#8d8a85', seed, 5)
    a[..., :3] += (smooth_noise(16, seed + 1, 4) * 14)[..., None]
    for y in (0, 8):
        a[y, :, :3] = rgb('#5d5b57')
    for y0, xs in ((0, (5,)), (8, (12,))):
        for x in xs:
            a[y0:y0 + 8, x, :3] = rgb('#5d5b57')
    for y in (1, 9):
        a[y, :, :3] *= 1.08
    for _ in range(26):                                      # tooling marks
        a[r.randint(0, 16), r.randint(0, 16), :3] *= 0.88
    return a


save(ashlar(3201), "ashlar")
# pillar: side = vertical drum courses, top = round shaft section
ps = base('#8f8c86', 3202, 4)
for y in (0, 15):
    ps[y, :, :3] = rgb('#5f5d59')
for x in (0, 15):
    ps[:, x, :3] = rgb('#6d6a66')
for x in (4, 8, 12):
    ps[:, x, :3] *= 0.86                                     # flutes
for x in (5, 9, 13):
    ps[:, x, :3] *= 1.07
save(ps, "ashlar_pillar")
pt = base('#8a8782', 3203, 4)
yy, xx = np.mgrid[0:16, 0:16]
d = np.hypot(xx - 7.5, yy - 7.5)
pt[(d > 6.2) & (d < 7.3), :3] = rgb('#605e5a')
pt[d < 2.0, :3] *= 0.92
save(pt, "ashlar_pillar_top")


# ------------------------------------------------------------------ dark oak
def planks(seed):
    r = np.random.RandomState(seed)
    a = base('#3d2c22', seed, 4)
    for b in range(4):
        y0 = b * 4
        tone = 1 + (r.rand() - 0.5) * 0.16
        a[y0:y0 + 4, :, :3] *= tone
        a[y0, :, :3] = rgb('#1f1612')
        jx = r.randint(0, 16)
        a[y0:y0 + 4, jx, :3] = rgb('#241a14')
        for _ in range(4):                                   # grain streaks
            gy = y0 + 1 + r.randint(0, 3)
            gx = r.randint(0, 16)
            for k in range(r.randint(3, 8)):
                a[gy, (gx + k) % 16, :3] *= 0.85
        a[y0 + 1 + r.randint(0, 2), (jx + 3) % 16, :3] = rgb('#5a4130')   # nail head
    return a


save(planks(3301), "dark_planks")
bs = base('#3a2a1f', 3302, 4)
r = np.random.RandomState(3302)
for x in range(16):
    if r.rand() < 0.35:
        bs[:, x, :3] *= 0.82
for _ in range(3):
    x = r.randint(0, 16)
    bs[r.randint(0, 10):, x, :3] = rgb('#21170f')
save(bs, "dark_beam")
bt = base('#4a3628', 3303, 4)
for rr, c in ((6.6, '#2a1d15'), (4.6, '#3a2a1f'), (2.6, '#2f2219')):
    bt[(d > rr - 0.6) & (d < rr + 0.4), :3] = rgb(c)
bt[(d > 7.0), :3] = rgb('#2a1d15')
save(bt, "dark_beam_top")


# ------------------------------------------------------------------ brass + mosaic (the rare yellow)
br = base('#b98d34', 3401, 5)
br[..., :3] += (smooth_noise(16, 3402, 4) * 22)[..., None]
br[0, :, :3] = br[15, :, :3] = br[:, 0, :3] = br[:, 15, :3] = rgb('#6e4f1a')
br[1, 1:15, :3] *= 1.18
br[1:15, 1, :3] *= 1.12
for y, x in ((3, 3), (3, 12), (12, 3), (12, 12)):
    br[y, x, :3] = rgb('#f0d27a'); br[y + 1, x, :3] = rgb('#5e4416')
for k in range(5, 11):                                       # engraved lozenge
    br[k, 7 - (k - 5) if k < 8 else 2 + k - 5, :3] *= 0.7
    br[k, 8 + (k - 5) if k < 8 else 13 - (k - 5), :3] *= 0.7
save(br, "brass_block")

mo = np.zeros((16, 16, 4)); mo[..., 3] = 255
r = np.random.RandomState(3403)
TILE = {'R': '#8a2e22', 'Y': '#c39a3e', 'G': '#7b7771', 'D': '#3a2f2a'}
pat = ["GGRRGG RR", ]
for ty in range(8):
    for tx in range(8):
        cx, cy = tx - 3.5, ty - 3.5
        dd = abs(cx) + abs(cy)
        k = 'Y' if dd < 1.5 else 'R' if dd < 3.5 else 'G' if dd < 5.5 else 'D'
        if (tx + ty) % 2 == 0 and k == 'G':
            k = 'R' if r.rand() < 0.2 else 'G'
        c = rgb(TILE[k]) + (r.rand() - 0.5) * 18
        mo[ty * 2:ty * 2 + 2, tx * 2:tx * 2 + 2, :3] = c
        mo[ty * 2 + 1, tx * 2 + 1, :3] = c * 0.82            # grout shadow
save(mo, "castle_mosaic")


# ------------------------------------------------------------------ stained glass pane (translucent)
sg = np.zeros((16, 16, 4))
r = np.random.RandomState(3501)
LEAD = rgb('#26221f')
cells = {}
CELLC = [('#a8261d', 175), ('#c98d22', 165), ('#2c4a8c', 185), ('#a8261d', 175), ('#7b1c2a', 185), ('#c98d22', 165)]
for y in range(16):
    for x in range(16):
        cid = (x // 4 + (1 if (y // 4) % 2 else 0)) // 1, y // 4
        if cid not in cells:
            cells[cid] = CELLC[r.randint(len(CELLC))]
        c, al = cells[cid]
        sg[y, x, :3] = rgb(c) + (r.rand() - 0.5) * 20
        sg[y, x, 3] = al
# lead cames: frame + diamond lattice
for y in range(16):
    for x in range(16):
        if x in (0, 15) or y in (0, 15) or (x + y) % 6 == 0 or (x - y) % 6 == 0:
            sg[y, x, :3] = LEAD; sg[y, x, 3] = 255
yy, xx = np.mgrid[0:16, 0:16]
cent = (np.hypot(xx - 7.5, yy - 7.5) < 2.4)
sg[cent, :3] = rgb('#e4b640'); sg[cent, 3] = 170             # amber rose in the middle
save(sg, "stained_glass")


# ------------------------------------------------------------------ roof tiles
rf = base('#5c2a22', 3601, 5)
for row in range(4):
    y0 = row * 4
    off = 0 if row % 2 == 0 else 2
    for x in range(16):
        ph = ((x + off) % 4)
        edge = y0 + 3 - (1 if ph in (1, 2) else 0)
        for y in range(y0, min(16, edge + 1)):
            rf[y, x, :3] *= 1.0 + 0.06 * (y - y0)
        if edge < 16:
            rf[edge, x, :3] = rgb('#2c130f')
save(rf, "castle_roof")


# ------------------------------------------------------------------ scorched site
ash = base('#7a7671', 3701, 6)
ash[..., :3] += (smooth_noise(16, 3702, 4) * 22)[..., None]
r = np.random.RandomState(3703)
for _ in range(16):
    ash[r.randint(0, 16), r.randint(0, 16), :3] = rgb('#3e3a37')
for _ in range(6):
    ash[r.randint(0, 16), r.randint(0, 16), :3] = rgb('#a8a39c')
save(ash, "ash_block")
so = base('#2f2723', 3704, 6)
so[..., :3] += (smooth_noise(16, 3705, 4) * 14)[..., None]
for _ in range(8):
    so[r.randint(0, 16), r.randint(0, 16), :3] = rgb('#57504a')
for _ in range(2):
    so[r.randint(0, 16), r.randint(0, 16), :3] = rgb('#9a3b1c')    # a dying ember
save(so, "burnt_soil")
ss = base('#3e3835', 3706, 7)
ss[..., :3] += (smooth_noise(16, 3707, 4) * 20)[..., None]
for _ in range(3):
    x, y = r.randint(0, 16), r.randint(0, 16)
    for k in range(r.randint(3, 7)):
        ss[y % 16, x % 16, :3] = rgb('#2a2522'); x += r.choice((-1, 1)); y += r.choice((0, 1))
save(ss, "scorched_stone")
cl = base('#231e1b', 3708, 5)
for y in range(16):                                          # alligator char: cracked blocks
    for x in range(16):
        if y % 4 == 0 or (x + (y // 4) * 2) % 5 == 0:
            cl[y, x, :3] = rgb('#0f0c0b')
        elif (x * 7 + y * 3) % 11 == 0:
            cl[y, x, :3] = rgb('#3a3330')
save(cl, "charred_log")
ct = base('#2a2420', 3709, 4)
for rr in (6.5, 4.5, 2.5):
    ct[(d > rr - 0.5) & (d < rr + 0.4), :3] = rgb('#161210')
ct[d > 7.0, :3] = rgb('#0f0c0b')
save(ct, "charred_log_top")


# ------------------------------------------------------------------ model textures (Blender props)
def big(a, n=64):
    return np.kron(a, np.ones((n // a.shape[0], n // a.shape[1], 1)))


save(bricks(3101), "m_c_brick", TM)
save(ashlar(3201), "m_c_ashlar", TM)
save(planks(3301), "m_c_darkwood", TM)
save(br, "m_c_brass", TM)
vel = base('#5e1418', 3801, 5)
vel[..., :3] += (smooth_noise(16, 3802, 4) * 26)[..., None]
for y in range(0, 16, 4):                                    # tufted upholstery buttons
    for x in range(2 if (y // 4) % 2 else 0, 16, 4):
        vel[y, x, :3] = rgb('#6a4a22')
        if y + 1 < 16:
            vel[y + 1, x, :3] *= 0.6
save(vel, "m_c_velvet", TM)
wax = base('#d8cfb6', 3803, 4)
for x in (3, 9, 13):                                         # drips
    for y in range(0, 5 + (x % 4) * 2):
        wax[y, x, :3] = rgb('#efe7d0')
save(wax, "m_c_wax", TM)
horn = base('#bfb39a', 3804, 6)
for y in range(0, 16, 3):
    horn[y, :, :3] *= 0.82                                   # growth rings
horn[12:, :, :3] = rgb('#3a322a') + 0                        # dark tips
save(horn, "m_c_horn", TM)
print("castle textures ok")
