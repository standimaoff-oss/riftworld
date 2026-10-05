# Rev 4 / B-1: Minecraft-style ("Blockbench") geometry for the boss, the guards and their weapons.
# exec()'d by creatures.py inside Blender.  Every part is an axis-aligned cuboid (optionally rotated), sized in whole
# pixels (16 px = 1 block) with a box-UV layout in one per-model pixel-art atlas painted here (nearest filtering):
# no cylinders, no bevels, no round shapes - a clear, readable silhouette like vanilla / Blockbench mobs.
import math, random
from mathutils import Vector, Matrix, Euler, Quaternion

PX = 1.0 / 16.0

PAL = {
    "hide":  [(26, 21, 21), (40, 31, 29), (55, 42, 38), (72, 55, 48)],
    "ash":   [(50, 48, 47), (68, 64, 62), (88, 84, 80), (108, 102, 96)],
    "bone":  [(146, 134, 108), (180, 168, 138), (206, 196, 166), (230, 222, 196)],
    "horn":  [(24, 20, 18), (42, 36, 31), (64, 55, 46), (90, 78, 64)],
    "fur":   [(28, 24, 24), (42, 37, 35), (58, 51, 48), (78, 69, 63)],
    "rag":   [(66, 13, 13), (94, 21, 19), (124, 31, 25), (152, 45, 33)],
    "steel": [(32, 34, 38), (50, 53, 58), (74, 78, 84), (106, 112, 118)],
    "blade": [(70, 74, 80), (108, 114, 122), (148, 154, 162), (198, 204, 212)],
    "wood":  [(46, 29, 19), (66, 43, 27), (86, 57, 35), (108, 72, 45)],
    "brass": [(108, 78, 30), (148, 110, 40), (188, 148, 58), (224, 188, 88)],
    "ember": [(214, 70, 12), (244, 118, 22), (255, 172, 44), (255, 226, 120)],
    "mouth": [(30, 6, 6), (48, 10, 9), (66, 16, 13), (84, 22, 17)],
    "dark":  [(12, 10, 10), (18, 15, 14), (24, 20, 18), (30, 25, 22)],
}


def _h(*a):
    v = 0x811C9DC5
    for x in a:
        v = ((v ^ (int(x) & 0xffffffff)) * 0x01000193) & 0xffffffff
    v ^= v >> 15
    v = (v * 0x2C1B3C6D) & 0xffffffff
    v ^= v >> 12
    return v / 4294967296.0


class Atlas:
    def __init__(self, name, W=256, H=256):
        self.name, self.W, self.H = name, W, H
        self.px = [0.0] * (W * H * 4)
        self.x = self.y = self.row = 1
        self.img = bpy.data.images.new("m_mc_" + name, W, H, alpha=True)
        self.mats = {}
        self.seed = 0

    def alloc(self, w, h):
        if self.x + w + 1 > self.W:
            self.x, self.y, self.row = 1, self.y + self.row + 1, 1
        if self.y + h + 1 > self.H:
            raise RuntimeError("atlas %s full" % self.name)
        x, y = self.x, self.y
        self.x += w + 1
        self.row = max(self.row, h)
        return x, y

    def put(self, x, y, c):
        i = ((self.H - 1 - y) * self.W + x) * 4          # Blender pixel rows start at the bottom
        self.px[i:i + 4] = [c[0] / 255.0, c[1] / 255.0, c[2] / 255.0, 1.0]

    def finish(self):
        self.img.pixels = self.px
        path = os.path.join(TEX, "m_mc_%s.png" % self.name)
        self.img.filepath_raw = path
        self.img.file_format = 'PNG'
        self.img.save()
        self.img.pack()

    def mat(self, emit):
        if emit in self.mats:
            return self.mats[emit]
        m = bpy.data.materials.new("mc_%s%s" % (self.name, "_e" if emit else ""))
        m.use_nodes = True
        nt = m.node_tree
        bsdf = nt.nodes.get("Principled BSDF")
        bsdf.inputs["Roughness"].default_value = 0.9
        tn = nt.nodes.new("ShaderNodeTexImage")
        tn.image = self.img
        tn.interpolation = 'Closest'
        nt.links.new(tn.outputs["Color"], bsdf.inputs["Base Color"])
        if emit:
            nt.links.new(tn.outputs["Color"], bsdf.inputs["Emission Color"])
            bsdf.inputs["Emission Strength"].default_value = 1.0
        self.mats[emit] = m
        return m


AT = None


def atlas(name, W=256, H=256):
    global AT
    AT = Atlas(name, W, H)
    return AT


# ------------------------------------------------------------------------------------------------ painting
def shade(pal, k):
    return pal[max(0, min(3, k))]


def paint_face(x0, y0, w, h, pal, pat, face, seed):
    """Paint one w x h face at atlas (x0, y0) (top-left origin, row 0 = top of the face = +Z side for side faces)."""
    lift = {"top": 1, "bottom": -1}.get(face, 0)
    for j in range(h):
        for i in range(w):
            n = _h(seed, i, j, x0)
            if pat == "plain":
                k = 2
            elif pat == "bone":
                k = 3 if n > 0.35 else 2
                if n < 0.06:
                    k = 1
            elif pat == "fur":
                k = 1 + int(_h(seed, i) * 2.6) - (1 if (j + int(_h(seed, i, 7) * 4)) % 4 == 0 else 0)
            elif pat == "cloth":
                k = 2 if (j % 3) else 1
                if n > 0.85:
                    k = 3
                if j == h - 1 and n > 0.45:
                    k = 0                                    # torn hem
            elif pat == "metal":
                k = 2 if n > 0.2 else 1
                if j == 0:
                    k = 3
            elif pat == "ribs":                              # bone ribs over dark hide (front of the ribcage)
                k = 1 if n > 0.4 else 0
                if j % 3 == 1 and 1 <= i < w - 1 and abs(i - w / 2 + 0.5) > 0.8:
                    k = 3 if n > 0.3 else 2
            elif pat == "ember":
                k = 3 if n > 0.6 else 2 if n > 0.25 else 1
            elif pat == "wood":
                k = 2 if (i + int(_h(seed, j // 3) * 3)) % 3 else 1
                if n > 0.9:
                    k = 3
            else:                                            # noise
                k = 2 if n > 0.45 else 1
                if n > 0.92:
                    k = 3
                if n < 0.05:
                    k = 0
            if pat not in ("ember",) and (i == 0 or j == 0 or i == w - 1 or j == h - 1) and w > 2 and h > 2:
                k -= 1                                       # darker rim -> readable block edges
            AT.put(x0 + i, y0 + j, shade(pal, k + lift))


# ------------------------------------------------------------------------------------------------ cuboids
def cube(name, size, center, pal, rot=(0, 0, 0), pat="noise", emit=False, faces=None):
    """size = (w, d, h) in whole pixels (x, y, z); center in blocks; rot = euler degrees (XYZ) about the centre."""
    w, d, h = (max(1, int(round(s))) for s in size)
    p = PAL[pal] if isinstance(pal, str) else pal
    W2, H2 = 2 * d + 2 * w, d + h
    ax, ay = AT.alloc(W2, H2)
    AT.seed += 1
    sd = AT.seed * 7919
    faces = faces or {}
    # box-UV regions (Blockbench layout): row 0: [d gap][top w][bottom w]; row 1: [right d][front w][left d][back w]
    R = {"top": (ax + d, ay, w, d), "bottom": (ax + d + w, ay, w, d),
         "east": (ax, ay + d, d, h), "front": (ax + d, ay + d, w, h), "west": (ax + d + w, ay + d, d, h), "back": (ax + 2 * d + w, ay + d, w, h)}
    for f, (fx, fy, fw, fh) in R.items():
        paint_face(fx, fy, fw, fh, p, faces.get(f, pat), f, sd + hash(f) % 1000)
    hx, hy, hz = w * PX / 2, d * PX / 2, h * PX / 2
    bm = bmesh.new()
    uvl = bm.loops.layers.uv.new()

    def V(x, y, z):
        return bm.verts.new((x, y, z))

    def uv(fx, fy, fw, fh):          # corners: top-left, top-right, bottom-right, bottom-left (atlas, y down)
        u0, u1 = fx / AT.W, (fx + fw) / AT.W
        v0, v1 = 1 - fy / AT.H, 1 - (fy + fh) / AT.H
        return [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]

    # face quads listed TL, TR, BR, BL as seen from outside
    quads = {
        "front": [(-hx, -hy, hz), (hx, -hy, hz), (hx, -hy, -hz), (-hx, -hy, -hz)],            # -Y
        "back": [(hx, hy, hz), (-hx, hy, hz), (-hx, hy, -hz), (hx, hy, -hz)],                 # +Y
        "east": [(-hx, hy, hz), (-hx, -hy, hz), (-hx, -hy, -hz), (-hx, hy, -hz)],             # -X
        "west": [(hx, -hy, hz), (hx, hy, hz), (hx, hy, -hz), (hx, -hy, -hz)],                 # +X
        "top": [(-hx, hy, hz), (hx, hy, hz), (hx, -hy, hz), (-hx, -hy, hz)],                  # +Z
        "bottom": [(-hx, -hy, -hz), (hx, -hy, -hz), (hx, hy, -hz), (-hx, hy, -hz)],           # -Z
    }
    for f, q in quads.items():
        vs = [V(*c) for c in q]
        fc = bm.faces.new(vs)
        for l, t in zip(fc.loops, uv(*R[f])):
            l[uvl].uv = t
    me = bpy.data.meshes.new(name)
    bm.normal_update()
    bm.to_mesh(me)
    bm.free()
    ob = bpy.data.objects.new(name, me)
    bpy.context.scene.collection.objects.link(ob)
    me.materials.append(AT.mat(emit))
    ob.location = center
    ob.rotation_euler = tuple(math.radians(a) for a in rot)
    bpy.context.view_layer.update()
    me.transform(ob.matrix_world)
    ob.matrix_world = Matrix.Identity(4)
    PARTS.append(ob)
    return ob


def seg(name, a, b, w, d, pal, pat="noise", extra=0.0, emit=False, faces=None):
    """Cuboid whose height axis runs from a to b (limbs); w x d pixels cross-section."""
    a, b = Vector(a), Vector(b)
    v = b - a
    L = v.length + extra
    q = v.normalized().to_track_quat('Z', 'Y')
    e = q.to_euler('XYZ')
    return cube(name, (w, d, L / PX), tuple((a + b) / 2), pal, tuple(math.degrees(x) for x in e), pat, emit, faces)


def mid(a, b, u=0.5):
    return tuple(a[i] + (b[i] - a[i]) * u for i in range(3))


def off(a, o):
    return tuple(a[i] + o[i] for i in range(3))


# ================================================================================================ weapons
def mc_scythe(tag, g):
    """Morgrath's war scythe, blocky: long dark snath, steel collar with a horned skull, stepped crescent blade with a
    glowing ember edge (readable from far away), grip wraps, mid nib handle, butt spike."""
    gx, gy, gz = g
    cube(tag + "snath", (2, 2, 42), (gx, gy, gz + 0.5), "wood", pat="wood")
    for k, z in enumerate((-0.04, 0.14)):
        cube(tag + "wrap%d" % k, (3, 3, 2), (gx, gy, gz + z), "rag", pat="cloth")
    cube(tag + "nib", (1, 4, 1), (gx, gy - 0.15, gz + 0.66), "steel", pat="metal")
    cube(tag + "nibg", (2, 2, 2), (gx, gy - 0.27, gz + 0.66), "steel", pat="metal")
    cube(tag + "butt", (2, 2, 3), (gx, gy, gz - 0.89), "steel", pat="metal")
    top = gz + 1.81
    cube(tag + "collar", (4, 4, 4), (gx, gy, top - 0.06), "steel", pat="metal")
    cube(tag + "skull", (3, 3, 3), (gx, gy + 0.19, top - 0.07), "bone", pat="bone")
    cube(tag + "skh", (4, 1, 1), (gx, gy + 0.19, top + 0.03), "horn", pat="plain")
    cube(tag + "spk", (1, 1, 4), (gx, gy, top + 0.18), "steel", pat="metal")
    # blade: 11 columns stepping forward (-Y) and curving down; height tapers 6 -> 1 px; ember edge underneath
    prof = [(0, 6), (0, 6), (-1, 6), (-1, 5), (-2, 5), (-3, 4), (-4, 4), (-5, 3), (-7, 3), (-9, 2), (-11, 1)]
    for k, (dz, hh) in enumerate(prof):
        y = gy - 0.16 - k * 3 * PX
        zc = top + 0.04 + dz * PX - hh * PX / 2
        cube(tag + "bl%d" % k, (1, 3, hh), (gx, y, zc), "blade", pat="metal")
        cube(tag + "eg%d" % k, (1, 3, 1), (gx, y, zc - hh * PX / 2 - PX / 2), "ember", pat="ember", emit=True)
    cube(tag + "tang", (2, 3, 4), (gx, gy - 0.1, top - 0.02), "steel", pat="metal")


def mc_sickle(tag, g):
    """Guards' hooked claw-sickle: wrapped haft, stepped crescent blade with ember edge, back hook, butt spike."""
    gx, gy, gz = g
    cube(tag + "haft", (1, 1, 13), (gx, gy, gz + 0.2), "wood", pat="wood")
    cube(tag + "wrap", (2, 2, 4), (gx, gy, gz), "rag", pat="cloth")
    cube(tag + "pommel", (2, 2, 2), (gx, gy, gz - 0.2), "steel", pat="metal")
    cube(tag + "head", (2, 2, 2), (gx, gy, gz + 0.62), "steel", pat="metal")
    prof = [(0, 0), (-1, 2), (-2, 3), (-2, 5), (-1, 7), (1, 8), (3, 8), (5, 7)]       # (dy px forward, dz px)
    for k in range(len(prof) - 1):
        (y0, z0), (y1, z1) = prof[k], prof[k + 1]
        seg(tag + "bl%d" % k, (gx, gy + y0 * PX - 0.06, gz + 0.62 + z0 * PX - 0.12), (gx, gy + y1 * PX - 0.06, gz + 0.62 + z1 * PX - 0.12),
            1, 2, "blade", pat="metal", extra=PX)
    cube(tag + "edge", (1, 1, 4), (gx, gy - 0.2, gz + 0.62), "ember", pat="ember", emit=True)
    cube(tag + "hook", (1, 3, 1), (gx, gy + 0.12, gz + 0.62), "blade", pat="metal")


# ================================================================================================ the boss
def mc_boss_geo(arm, H):
    atlas("boss", 256, 256)
    # ---- hips: pelvis, steel belt with skull buckle, torn red loincloths
    cube("pelvis", (14, 9, 6), (0, 0.08, 1.25), "hide")
    cube("belt", (15, 10, 2), (0, 0.08, 1.40), "steel", pat="metal")
    cube("buckle", (4, 1, 4), (0, -0.25, 1.38), "bone", pat="bone")
    cube("buckle_eyes", (2, 1, 1), (0, -0.285, 1.40), "ember", pat="ember", emit=True)
    cube("loin_f", (10, 1, 10), (0, -0.23, 1.05), "rag", pat="cloth", rot=(4, 0, 0))
    cube("loin_b", (12, 1, 12), (0, 0.38, 1.0), "rag", pat="cloth", rot=(-8, 0, 0))
    attach("m_hips", arm, "hips", H["hips"])
    # ---- spine / belly with rib bars
    cube("belly", (12, 8, 6), (0, 0.04, 1.56), "hide")
    for k in range(3):
        cube("rib%d" % k, (10, 1, 1), (0, -0.22, 1.49 + k * 0.08), "bone", pat="bone")
    attach("m_spine", arm, "spine", H["spine"])
    # ---- chest: big hunched torso (ribs painted on the front), furred hump, bone pauldrons with spikes,
    #      three dorsal spikes tipped with embers, torn cape
    cube("torso", (18, 12, 10), (0, -0.10, 1.98), "hide", rot=(25, 0, 0), faces={"front": "ribs"})
    cube("hump", (16, 8, 6), (0, 0.13, 2.22), "fur", pat="fur", rot=(35, 0, 0))
    for s in (-1, 1):
        cube("paul%d" % s, (8, 9, 3), (s * 0.62, -0.26, 2.33), "bone", pat="bone", rot=(8, s * -22, 0))
        cube("paul2%d" % s, (7, 8, 2), (s * 0.68, -0.26, 2.22), "bone", pat="bone", rot=(8, s * -40, 0))
        cube("pspk%d" % s, (2, 2, 6), (s * 0.66, -0.20, 2.52), "horn", pat="plain", rot=(0, s * 18, 0))
    for k, (hh, z) in enumerate(((6, 2.02), (8, 2.17), (6, 2.32))):
        y = 0.27 - k * 0.07
        cube("dsp%d" % k, (2, 2, hh), (0, y, z + hh * PX / 2), "horn", pat="plain", rot=(-30, 0, 0))
        cube("dspe%d" % k, (2, 2, 2), (0, y + 0.12 + hh * 0.012, z + hh * PX * 0.95), "ember", pat="ember", emit=True, rot=(-30, 0, 0))
    cube("cape", (16, 1, 17), (0, 0.42, 1.78), "rag", pat="cloth", rot=(-12, 0, 0))
    attach("m_chest", arm, "chest", H["chest"])
    # ---- neck
    seg("neck", (0, -0.22, 2.14), (0, -0.66, 2.42), 6, 6, "hide", extra=0.06)
    attach("m_neck", arm, "neck", H["neck"])
    # ---- head: long canine bone skull, brow, glowing eyes, fangs, ears, bison horns (L-shaped blocks)
    cube("cranium", (8, 8, 7), (0, -0.68, 2.50), "bone", pat="bone")
    cube("snout", (5, 8, 4), (0, -1.10, 2.45), "bone", pat="bone")
    cube("nose", (3, 1, 2), (0, -1.36, 2.49), "dark", pat="plain")
    cube("brow", (9, 2, 2), (0, -0.95, 2.62), "bone", pat="bone")
    for s in (-1, 1):
        cube("sock%d" % s, (3, 1, 2), (s * 0.13, -0.935, 2.53), "dark", pat="plain")
        cube("eye%d" % s, (2, 1, 1), (s * 0.13, -0.95, 2.53), "ember", pat="ember", emit=True)
        for k in range(3):
            cube("ft%d%d" % (s, k), (1, 1, 2 if k == 0 else 1), (s * 0.12, -0.95 - k * 0.12, 2.31 - (0.03 if k == 0 else 0)), "bone", pat="plain")
        cube("ear%d" % s, (2, 2, 4), (s * 0.22, -0.58, 2.76), "hide", rot=(-15, s * 20, 0))
        cube("hring%d" % s, (2, 4, 4), (s * 0.28, -0.66, 2.60), "steel", pat="metal")
        cube("horn1%d" % s, (5, 3, 3), (s * 0.43, -0.66, 2.62), "horn", pat="plain")
        cube("horn2%d" % s, (3, 3, 5), (s * 0.58, -0.68, 2.80), "horn", pat="plain", rot=(0, s * 12, 0))
        cube("horn3%d" % s, (2, 2, 4), (s * 0.57, -0.76, 3.0), "bone", pat="bone", rot=(25, s * -18, 0))
    cube("hmane", (6, 6, 3), (0, -0.52, 2.66), "fur", pat="fur")
    attach("m_head", arm, "head", H["head"])
    # ---- jaw
    cube("jawb", (5, 8, 2), (0, -1.03, 2.31), "bone", pat="bone")
    cube("gum", (3, 6, 1), (0, -1.02, 2.335), "mouth", pat="plain")
    for s in (-1, 1):
        for k in range(2):
            cube("lt%d%d" % (s, k), (1, 1, 1), (s * 0.11, -0.98 - k * 0.14, 2.37), "bone", pat="plain")
    attach("m_jaw", arm, "jaw", H["jaw"])
    # ---- arms: long, blocky, rag wrap, steel bracers with spikes, clawed hands
    for side, s in (("L", 1), ("R", -1)):
        sh, el, wr = H["arm_" + side], H["fore_" + side], H["hand_" + side]
        seg("ua" + side, sh, el, 5, 5, "hide", extra=0.08)
        seg("uaw" + side, mid(sh, el, 0.45), mid(sh, el, 0.62), 6, 6, "rag", pat="cloth")
        attach("m_arm_" + side, arm, "arm_" + side, sh)
        seg("fa" + side, el, wr, 4, 4, "hide", extra=0.06)
        seg("brac" + side, mid(el, wr, 0.45), mid(el, wr, 0.8), 5, 5, "steel", pat="metal")
        cube("bsp" + side, (1, 1, 3), off(mid(el, wr, 0.6), (s * 0.18, 0.0, 0.0)), "steel", pat="metal", rot=(0, s * 70, 0))
        seg("elb" + side, el, off(el, (s * 0.02, 0.2, -0.02)), 2, 2, "bone", pat="bone")
        attach("m_fore_" + side, arm, "fore_" + side, el)
        cube("palm" + side, (5, 4, 4), off(wr, (0, -0.02, -0.1)), "hide")
        if side == "R":                      # fingers curled round the snath
            for k in range(3):
                cube("fg%s%d" % (side, k), (1, 3, 1), off(wr, (s * 0.0 + (k - 1) * 0.07, -0.14, -0.17)), "horn", pat="plain")
        else:
            for k in range(3):
                cube("fg%s%d" % (side, k), (1, 1, 5), off(wr, ((k - 1) * 0.08, -0.12, -0.3)), "horn", pat="plain", rot=(-15, 0, 0))
        cube("th" + side, (1, 1, 3), off(wr, (-s * 0.17, -0.08, -0.16)), "horn", pat="plain")
        attach("m_hand_" + side, arm, "hand_" + side, wr)
    # ---- legs: thick blocky thighs with rag wraps, bone kneecaps + spikes, long clawed feet
    for side, s in (("L", 1), ("R", -1)):
        hp, kn, an = H["thigh_" + side], H["shin_" + side], H["foot_" + side]
        seg("th" + side, hp, kn, 6, 6, "hide", extra=0.08)
        seg("thw" + side, mid(hp, kn, 0.3), mid(hp, kn, 0.5), 7, 7, "rag", pat="cloth")
        attach("m_thigh_" + side, arm, "thigh_" + side, hp)
        seg("sh" + side, kn, an, 5, 5, "hide", extra=0.06)
        cube("knee" + side, (5, 2, 4), off(kn, (0, -0.17, 0.0)), "bone", pat="bone")
        cube("knsp" + side, (2, 4, 2), off(kn, (0, -0.33, 0.06)), "bone", pat="bone", rot=(-20, 0, 0))
        attach("m_shin_" + side, arm, "shin_" + side, kn)
        cube("foot" + side, (6, 9, 3), (an[0], -0.06, 0.09), "hide")
        for k in range(3):
            cube("toe%s%d" % (side, k), (1, 3, 2), (an[0] + (k - 1) * 0.12, -0.40, 0.06), "horn", pat="plain")
        cube("spur" + side, (2, 3, 2), (an[0], 0.28, 0.1), "horn", pat="plain")
        attach("m_foot_" + side, arm, "foot_" + side, an)
    mc_scythe("scy", (-0.72, -0.60, 0.92))
    attach("scythe", arm, "scythe_b", H["scythe_b"])
    AT.finish()


# ================================================================================================ guards
def mc_guard_geo(arm, H):
    atlas("guard", 128, 128)
    cube("pelvis", (8, 5, 4), (0, 0.04, 0.93), "ash")
    cube("belt", (9, 6, 1), (0, 0.04, 1.02), "steel", pat="metal")
    cube("tab_f", (6, 1, 8), (0, -0.14, 0.78), "rag", pat="cloth")
    cube("tab_b", (7, 1, 9), (0, 0.22, 0.76), "rag", pat="cloth", rot=(-6, 0, 0))
    attach("m_hips", arm, "hips", H["hips"])
    cube("torso", (9, 6, 7), (0, -0.06, 1.33), "ash", rot=(20, 0, 0), faces={"front": "ribs"})
    cube("tabard", (5, 1, 6), (0, -0.25, 1.24), "rag", pat="cloth", rot=(20, 0, 0))
    cube("emb", (2, 1, 2), (0, -0.27, 1.30), "brass", pat="metal", rot=(20, 0, 0))
    for k in range(3):
        cube("bsp%d" % k, (1, 1, 3), (0, 0.16 - k * 0.03, 1.2 + k * 0.12), "bone", pat="bone", rot=(-35, 0, 0))
    for s in (-1, 1):
        cube("shp%d" % s, (4, 5, 2), (s * 0.29, -0.15, 1.57), "bone", pat="bone", rot=(0, s * -25, 0))
        cube("shs%d" % s, (1, 1, 4), (s * 0.34, -0.14, 1.70), "horn", pat="plain", rot=(0, s * 20, 0))
    attach("m_chest", arm, "chest", H["chest"])
    seg("neck", (0, -0.16, 1.5), (0, -0.42, 1.66), 3, 3, "ash", extra=0.04)
    attach("m_neck", arm, "neck", H["neck"])
    cube("skull", (5, 5, 5), (0, -0.44, 1.71), "ash")
    cube("mask", (5, 1, 6), (0, -0.61, 1.69), "bone", pat="bone")
    for s in (-1, 1):
        cube("eye%d" % s, (1, 1, 1), (s * 0.07, -0.65, 1.73), "ember", pat="ember", emit=True)
        cube("horn%d" % s, (1, 1, 3), (s * 0.13, -0.47, 1.92), "horn", pat="plain", rot=(-20, s * 25, 0))
    cube("hood", (6, 5, 2), (0, -0.40, 1.88), "rag", pat="cloth")
    attach("m_head", arm, "head", H["head"])
    cube("jawb", (4, 3, 1), (0, -0.56, 1.555), "bone", pat="bone")
    attach("m_jaw", arm, "jaw", H["jaw"])
    for side, s in (("L", 1), ("R", -1)):
        sh, el, wr = H["arm_" + side], H["fore_" + side], H["hand_" + side]
        seg("ua" + side, sh, el, 3, 3, "ash", extra=0.05)
        attach("m_arm_" + side, arm, "arm_" + side, sh)
        seg("fa" + side, el, wr, 2, 2, "ash", extra=0.05)
        seg("wr" + side, mid(el, wr, 0.55), mid(el, wr, 0.85), 3, 3, "rag", pat="cloth")
        attach("m_fore_" + side, arm, "fore_" + side, el)
        cube("hand" + side, (3, 3, 3), off(wr, (0, -0.02, -0.07)), "ash")
        for k in range(3):
            cube("cl%s%d" % (side, k), (1, 1, 3), off(wr, ((k - 1) * 0.06, -0.09, -0.19)), "horn", pat="plain")
        attach("m_hand_" + side, arm, "hand_" + side, wr)
    for side, s in (("L", 1), ("R", -1)):
        hp, kn, an = H["thigh_" + side], H["shin_" + side], H["foot_" + side]
        seg("th" + side, hp, kn, 3, 3, "ash", extra=0.05)
        attach("m_thigh_" + side, arm, "thigh_" + side, hp)
        seg("sh" + side, kn, an, 3, 3, "ash", extra=0.05)
        cube("kn" + side, (3, 1, 2), off(kn, (0, -0.11, 0)), "bone", pat="bone")
        attach("m_shin_" + side, arm, "shin_" + side, kn)
        cube("foot" + side, (3, 5, 2), (an[0], -0.04, 0.06), "ash")
        for k in range(2):
            cube("toe%s%d" % (side, k), (1, 2, 1), (an[0] + (k - 0.5) * 0.1, -0.24, 0.03), "horn", pat="plain")
        attach("m_foot_" + side, arm, "foot_" + side, an)
    mc_sickle("wp", H["wpn_b"])
    attach("weapon", arm, "wpn_b", H["wpn_b"])
    AT.finish()


def mc_weapon_item(kind):
    atlas(kind, 128, 128)
    if kind == "scythe":
        mc_scythe("scy", (0, 0, 0))
    else:
        mc_sickle("wp", (0, 0, 0))
    node(kind)
    AT.finish()
