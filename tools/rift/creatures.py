# Riftworld part A: the boss «Морграт, Хозяин Чертога», his guards («Тлеющие стражи») and their weapons.
#   blender -b --factory-startup --python-exit-code 1 --python tools/rift/creatures.py [-- boss,guard,scythe,guard_sickle]
# Rigid, detailed parts parented to an armature whose bones all have identity rest orientation (pointing +Y, roll 0),
# so pose rotations are in model axes: +X rot bends the torso forward / swings a hanging limb back, +Z turns towards +X
# (the model's left), a hanging limb on the +X side is raised outwards by -Y (+Y on the -X side).
# Every clip is an armature action -> one glTF animation (node TRS channels; the mod samples them with Pose.sample).
# 1 unit = 1 block, feet at z = 0, front = -Y (glTF +Z = Minecraft south for yaw 0).
import sys, os, math, random
_argv = sys.argv[:]
WANT = set(_argv[_argv.index("--") + 1].split(",")) if "--" in _argv else None
sys.argv = [_argv[0], "--", "__none__"]
exec(open("/home/user/tools/rift/models.py").read())          # helpers only (builds nothing)
sys.argv = _argv
from mathutils import Quaternion

FPS = 20
MATDEF.update({
    "b_hide": {"tex": "m_b_hide", "rough": 0.85},
    "b_ash": {"tex": "m_b_skin_ash", "rough": 0.85},
    "b_bone": {"tex": "m_b_bone", "rough": 0.7},
    "b_rag": {"tex": "m_b_rag", "rough": 1.0},
    "b_fur": {"tex": "m_b_fur", "rough": 1.0},
    "b_blade": {"tex": "m_b_blade", "metal": 0.75, "rough": 0.35},
    "b_snath": {"tex": "m_b_snath", "rough": 0.8},
    "b_eye": {"color": (1.0, 0.45, 0.08, 1.0), "emit": True},
    "b_gum": {"color": (0.32, 0.07, 0.06, 1.0), "rough": 0.5},
    "b_tooth": {"color": (0.86, 0.80, 0.66, 1.0), "rough": 0.5},
})


# ------------------------------------------------------------------------------------------------ geometry helpers
def taper(name, a, b, r0, r1, m, seg=8):
    a, b = Vector(a), Vector(b)
    d = b - a
    return cyl(name, r0, d.length, tuple((a + b) / 2), m, rot=d.to_track_quat('Z', 'Y').to_euler(), seg=seg, r2=r1)


def spike(name, base, tip, r, m, seg=6):
    return taper(name, base, tip, r, r * 0.08, m, seg)


def add(a, b, k=1.0):
    return tuple(a[i] + b[i] * k for i in range(3))


def lerp3(a, b, u):
    return tuple(a[i] + (b[i] - a[i]) * u for i in range(3))


def make_rig(name, bones):
    ad = bpy.data.armatures.new(name + "_rig")
    arm = bpy.data.objects.new(name + "_rig", ad)
    bpy.context.scene.collection.objects.link(arm)
    bpy.context.view_layer.objects.active = arm
    arm.select_set(True)
    bpy.ops.object.mode_set(mode='EDIT')
    eb = {}
    for bn, par, h in bones:
        b = ad.edit_bones.new(bn)
        b.head = h
        b.tail = (h[0], h[1] + 0.12, h[2])
        b.roll = 0.0
        if par:
            b.parent = eb[par]
            b.use_connect = False
        eb[bn] = b
    bpy.ops.object.mode_set(mode='OBJECT')
    return arm


def attach(name, arm, bn, pivot):
    """Join every part built since the last node into mesh node `name` (origin = pivot) and parent it to bone bn."""
    ob = node(name, pivot=pivot)
    if ob is None:
        return None
    bone = arm.data.bones[bn]
    mw = Matrix.Translation(Vector(pivot))
    ob.parent = arm
    ob.parent_type = 'BONE'
    ob.parent_bone = bn
    ob.matrix_parent_inverse = (bone.matrix_local @ Matrix.Translation((0, bone.length, 0))).inverted()
    ob.matrix_basis = mw
    return ob


# ------------------------------------------------------------------------------------------------ animation helpers
def Q(e):
    return Euler(tuple(math.radians(v) for v in e), 'XYZ').to_quaternion()


def bake_clips(arm, clips, chains):
    """clips: {name: [(t, pose)]}; pose = {bone: (rx, ry, rz) deg, 'root_loc': (x, y, z), '<weapon>': rel-to-chest euler}.
    chains: {weapon_bone: (pose key, [parent bones from the chest's child down to the hand])}."""
    arm.animation_data_create()
    for pb in arm.pose.bones:
        pb.rotation_mode = 'QUATERNION'
    sc = bpy.context.scene
    sc.render.fps = FPS
    sc.frame_start = 0
    for clip, keys in clips.items():
        act = bpy.data.actions.new(clip)
        act.use_fake_user = True
        arm.animation_data.action = act
        prev = {}
        for t, pose in keys:
            f = round(t * FPS)
            for pb in arm.pose.bones:
                bn = pb.name
                if bn in chains:
                    key, chain = chains[bn]
                    rel = Q(pose.get(key, (0, 0, 0)))
                    par = Quaternion()
                    for c in chain:
                        par = par @ Q(pose.get(c, (0, 0, 0)))
                    q = par.inverted() @ rel
                else:
                    q = Q(pose.get(bn, (0, 0, 0)))
                if bn in prev and prev[bn].dot(q) < 0:
                    q.negate()
                prev[bn] = q
                pb.rotation_quaternion = q
                pb.keyframe_insert("rotation_quaternion", frame=f, group=bn)
                if bn == "root":
                    pb.location = pose.get("root_loc", (0, 0, 0))
                    pb.keyframe_insert("location", frame=f, group=bn)
        tr = arm.animation_data.nla_tracks.new()
        tr.name = clip
        tr.strips.new(clip, 0, act)
        arm.animation_data.action = None
        print("CLIP", clip, "%.2fs" % keys[-1][0])


def export_anim(name):
    for o in bpy.context.scene.objects:
        o.select_set(True)
    path = os.path.join(OUT, name + ".glb")
    bpy.ops.export_scene.gltf(filepath=path, export_format='GLB', export_yup=True, export_apply=False,
                              export_animations=True, export_animation_mode='ACTIONS', export_force_sampling=True,
                              export_extras=False, export_cameras=False, export_lights=False, export_skins=False,
                              export_image_format='AUTO', use_selection=False)
    bpy.ops.wm.save_as_mainfile(filepath=os.path.join(BLEND, name + ".blend"), compress=True)
    tris = sum(len(o.data.polygons) for o in bpy.context.scene.objects if o.type == 'MESH')
    print("MODEL", name, os.path.getsize(path), "B", tris, "faces")


def P(base=None, **kw):
    d = dict(base or {})
    for k, v in kw.items():
        d[k] = v
    return d


# ================================================================================================ the scythe
def scythe_parts(tag, g):
    """Morgrath's war scythe; g = grip point (where the right hand holds the snath). Snath runs from g-0.8 up to g+1.8
    (ref: European scythe - long snath, mid nib grip, blade fixed at the end at a right angle, long curved thin blade)."""
    gx, gy, gz = g
    pts = [(gx, gy + 0.02, gz - 0.80), (gx, gy, gz - 0.05), (gx, gy - 0.03, gz + 0.85), (gx, gy - 0.07, gz + 1.78)]
    for k in range(3):
        rod(tag + "sn%d" % k, pts[k], pts[k + 1], 0.034, "b_snath", seg=8)
    for k, z in enumerate((-0.05, 0.12)):
        cyl(tag + "wrap%d" % k, 0.04, 0.12, (gx, gy, gz + z), "e_hide", seg=8)
    rod(tag + "nib", (gx, gy - 0.03, gz + 0.62), (gx, gy - 0.22, gz + 0.67), 0.022, "b_snath", seg=6)
    cyl(tag + "nibcap", 0.03, 0.04, (gx, gy - 0.23, gz + 0.675), "c_blacksteel", rot=(math.pi / 2, 0, 0), seg=6)
    for z in (-0.6, 0.35, 1.2):
        torus(tag + "band%d" % int(z * 10 + 10), 0.038, 0.009, (gx, gy - 0.01 - max(z, 0) * 0.04, gz + z), "c_blacksteel", seg=10, seg2=4)
    spike(tag + "butt", (gx, gy + 0.02, gz - 0.78), (gx, gy + 0.03, gz - 0.98), 0.035, "c_blacksteel")
    top = (gx, gy - 0.07, gz + 1.78)
    cyl(tag + "collar", 0.055, 0.16, add(top, (0, 0, -0.06)), "c_blacksteel", seg=8)
    cyl(tag + "cap", 0.05, 0.08, add(top, (0, 0, 0.06)), "c_blacksteel", seg=8, r2=0.025)
    spike(tag + "topspk", add(top, (0, 0, 0.09)), add(top, (0, 0.06, 0.32)), 0.03, "c_blacksteel")
    # small horned skull on the collar (back side) + hanging chain with a charm
    _skull(tag + "sk", gx, gy + 0.03, gz + 1.6, s=0.32)
    for k in range(4):
        torus(tag + "ch%d" % k, 0.022, 0.006, (gx + 0.05, gy - 0.07, gz + 1.68 - k * 0.04), "c_blacksteel",
              rot=(0, math.pi / 2 if k % 2 else 0, 0), seg=8, seg2=3)
    crystal(tag + "charm", (gx + 0.05, gy - 0.07, gz + 1.5), "c_ember", h=0.09, r=0.025, rot=(math.pi, 0, 0))
    # blade: 12 segments from the collar forward, back edge rising a little then curving down to a point
    A = Vector((gx, gy - 0.12, gz + 1.74))
    prev = None
    n = 12
    for k in range(n + 1):
        u = k / n
        p = A + Vector((0, -1.45 * u, 0.12 * math.sin(math.pi * u * 0.75) - 0.42 * u * u))
        if prev is not None:
            uu = (k - 0.5) / n
            w = 0.24 * (1 - uu) ** 0.8 + 0.025
            d = prev - p
            phi = math.atan2(d.z, d.y)
            mid = (p + prev) / 2
            down = Vector((0, -math.sin(phi), math.cos(phi))) * -1        # local -Z after rotation about X by phi
            box(tag + "bl%d" % k, (0.022, d.length + 0.012, w), tuple(mid + down * (w / 2)), "b_blade", (phi, 0, 0))
            box(tag + "eg%d" % k, (0.03, d.length + 0.012, 0.03), tuple(mid + down * (w + 0.008)), "c_ember", (phi, 0, 0))
            if k % 3 == 1:
                box(tag + "rv%d" % k, (0.03, 0.03, 0.03), tuple(mid + down * 0.05), "c_brass", (phi, 0, 0))
        prev = p
    box(tag + "tang", (0.03, 0.16, 0.12), (gx, gy - 0.12, gz + 1.70), "c_blacksteel")
    spike(tag + "beard", (gx, gy - 0.14, gz + 1.63), (gx, gy - 0.02, gz + 1.45), 0.03, "c_blacksteel")


def sickle_parts(tag, g):
    """Guards' hooked claw-sickle: short wrapped haft held at g, rising 0.75, crescent blade + back hook + butt spike."""
    gx, gy, gz = g
    rod(tag + "haft", (gx, gy, gz - 0.18), (gx, gy, gz + 0.62), 0.024, "c_dark", seg=6)
    for k in range(3):
        cyl(tag + "w%d" % k, 0.03, 0.06, (gx, gy, gz - 0.08 + k * 0.08), "e_hide", seg=6)
    cyl(tag + "pm", 0.035, 0.05, (gx, gy, gz - 0.2), "c_blacksteel", seg=6)
    spike(tag + "bt", (gx, gy, gz - 0.22), (gx, gy, gz - 0.36), 0.03, "c_blacksteel")
    cyl(tag + "sk", 0.03, 0.08, (gx, gy, gz + 0.64), "c_blacksteel", seg=6)
    c = Vector((gx, gy - 0.30, gz + 0.62))
    prev = None
    for k in range(9):
        a = math.radians(-10 + k * 25)
        p = c + Vector((0, 0.30 * math.cos(a), 0.30 * math.sin(a)))
        if prev is not None:
            rod(tag + "b%d" % k, tuple(prev), tuple(p), 0.03 - k * 0.0025, "b_blade", seg=4)
            if k > 2:
                rod(tag + "e%d" % k, tuple(prev + (prev - c) * 0.12), tuple(p + (p - c) * 0.12), 0.008, "c_ember", seg=3)
        prev = p
    spike(tag + "hook", (gx, gy, gz + 0.62), (gx, gy + 0.2, gz + 0.5), 0.026, "b_blade")
    for k in range(3):
        spike(tag + "sr%d" % k, (gx, gy - 0.02, gz + 0.3 + k * 0.1), (gx, gy - 0.1, gz + 0.34 + k * 0.1), 0.012, "c_blacksteel", seg=4)


# ================================================================================================ the boss
BOSS_BONES = [
    ("root", None, (0, 0, 0)),
    ("hips", "root", (0, 0.10, 1.25)),
    ("spine", "hips", (0, 0.05, 1.50)),
    ("chest", "spine", (0, -0.10, 1.85)),
    ("neck", "chest", (0, -0.42, 2.26)),
    ("head", "neck", (0, -0.62, 2.40)),
    ("jaw", "head", (0, -0.72, 2.31)),
    ("arm_L", "chest", (0.50, -0.30, 2.12)), ("fore_L", "arm_L", (0.66, -0.44, 1.52)), ("hand_L", "fore_L", (0.72, -0.56, 0.98)),
    ("arm_R", "chest", (-0.50, -0.30, 2.12)), ("fore_R", "arm_R", (-0.66, -0.44, 1.52)), ("hand_R", "fore_R", (-0.72, -0.56, 0.98)),
    ("scythe_b", "hand_R", (-0.72, -0.60, 0.92)),
    ("thigh_L", "hips", (0.27, 0.10, 1.20)), ("shin_L", "thigh_L", (0.32, -0.22, 0.70)), ("foot_L", "shin_L", (0.32, 0.14, 0.24)),
    ("thigh_R", "hips", (-0.27, 0.10, 1.20)), ("shin_R", "thigh_R", (-0.32, -0.22, 0.70)), ("foot_R", "shin_R", (-0.32, 0.14, 0.24)),
]


def m_boss():
    H = {b: h for b, _, h in BOSS_BONES}
    arm = make_rig("boss", BOSS_BONES)
    rnd = random.Random(21)
    # ---- hips: pelvis, belt with skull buckle, ragged loincloth front and back
    box("pelvis", (0.58, 0.40, 0.30), (0, 0.08, 1.25), "b_hide", bevel=0.06)
    for s in (-1, 1):
        box("hipbone%d" % s, (0.12, 0.30, 0.14), (s * 0.30, 0.05, 1.33), "b_bone", (0, s * 0.3, 0), bevel=0.03)
    box("belt", (0.64, 0.46, 0.07), (0, 0.08, 1.39), "e_hide", bevel=0.02)
    _skull("buckle", 0, -0.19, 1.38, s=0.34)
    for k in range(6):
        x = -0.25 + k * 0.1
        L = 0.42 + rnd.random() * 0.25
        box("lf%d" % k, (0.09, 0.02, L), (x, -0.17 - k % 2 * 0.01, 1.36 - L / 2), "b_rag", (0.06 * math.sin(k), 0, 0.12 * math.cos(k * 1.7)))
        L = 0.5 + rnd.random() * 0.3
        box("lb%d" % k, (0.09, 0.02, L), (x, 0.31 + k % 2 * 0.01, 1.36 - L / 2), "b_rag", (-0.08, 0, 0.1 * math.sin(k * 2.1)))
    for s in (-1, 1):
        for k in range(3):
            L = 0.35 + rnd.random() * 0.2
            box("ls%d%d" % (s, k), (0.02, 0.09, L), (s * 0.31, -0.06 + k * 0.12, 1.36 - L / 2), "b_rag", (0, 0.1 * s, 0))
    attach("m_hips", arm, "hips", H["hips"])
    # ---- spine: belly + exposed ribs + vertebrae
    taper("belly", (0, 0.10, 1.32), (0, -0.02, 1.76), 0.22, 0.28, "b_hide", seg=10)
    for s in (-1, 1):
        for k in range(4):
            z = 1.50 + k * 0.08
            rod("rib%d%d" % (s, k), (s * 0.08, -0.24, z), (s * 0.27, -0.06, z - 0.03), 0.022, "b_bone", seg=5)
            rod("ribb%d%d" % (s, k), (s * 0.27, -0.06, z - 0.03), (s * 0.24, 0.13, z + 0.01), 0.02, "b_bone", seg=5)
    for k in range(3):
        box("vert%d" % k, (0.07, 0.06, 0.06), (0, 0.27 - k * 0.04, 1.42 + k * 0.12), "b_bone", bevel=0.015)
    attach("m_spine", arm, "spine", H["spine"])
    # ---- chest: hunched ribcage, trapezius hump, sternum, ember cracks, dorsal spikes, mane, bone pauldrons, rag cape
    box("ribcage", (0.80, 0.52, 0.62), (0, -0.12, 1.95), "b_hide", (0.45, 0, 0), bevel=0.1)
    box("hump", (0.88, 0.46, 0.36), (0, 0.0, 2.2), "b_hide", (0.6, 0, 0), bevel=0.1)
    box("sternum", (0.16, 0.04, 0.44), (0, -0.42, 1.93), "b_bone", (0.45, 0, 0), bevel=0.01)
    for s in (-1, 1):
        for k in range(3):
            rod("crib%d%d" % (s, k), (s * 0.06, -0.44, 1.78 + k * 0.11), (s * 0.36, -0.30, 1.74 + k * 0.12), 0.02, "b_bone", seg=5)
    for k, (x, z, a) in enumerate(((-0.18, 2.02, 0.5), (0.12, 1.95, -0.7), (0.22, 2.1, 0.2), (-0.05, 1.86, 1.2), (0.3, 1.86, -0.3))):
        box("ember%d" % k, (0.2, 0.012, 0.014), (x, -0.43 + abs(x) * 0.25, z), "c_ember", (0.45, a, 0))
    for k in range(7):
        u = k / 6
        p = lerp3((0, 0.24, 1.72), (0, -0.08, 2.44), u)
        L = 0.14 + math.sin(u * math.pi) * 0.22
        tip = add(p, (0, 0.6 * L, 0.8 * L))
        spike("dsp%d" % k, p, tip, 0.045 + math.sin(u * math.pi) * 0.03, "c_horn")
        if 2 <= k <= 4:
            crystal("dspg%d" % k, add(tip, (0, -0.01, -0.02)), "c_ember", h=0.07, r=0.02, rot=(-0.6, 0, 0))
    for k in range(18):
        x = -0.32 + (k % 9) * 0.08
        row = k // 9
        a = (x * 0.9, -0.16 + row * 0.12, 2.42 - row * 0.06)
        b = (x * 1.25 + rnd.uniform(-0.04, 0.04), 0.30 + row * 0.04, 1.98 - row * 0.12 - rnd.random() * 0.1)
        rod("mane%d" % k, a, b, 0.032, "b_fur", seg=4)
    for s in (-1, 1):
        box("paul%d" % s, (0.32, 0.34, 0.08), (s * 0.48, -0.24, 2.32), "b_bone", (0.15, s * -0.55, 0), bevel=0.03)
        box("paul2%d" % s, (0.26, 0.28, 0.06), (s * 0.56, -0.26, 2.24), "b_bone", (0.15, s * -0.85, 0), bevel=0.02)
        for k in range(3):
            spike("pspk%d%d" % (s, k), (s * (0.42 + k * 0.07), -0.34 + k * 0.08, 2.36), (s * (0.5 + k * 0.1), -0.30 + k * 0.1, 2.62 - k * 0.04), 0.03, "c_horn")
    for k in range(7):
        x = -0.36 + k * 0.12
        L = 0.75 + rnd.random() * 0.45
        box("cape%d" % k, (0.11, 0.02, L), (x, 0.30 + abs(x) * 0.1, 2.12 - L / 2), "b_rag", (-0.22, 0, 0.08 * math.sin(k * 1.3)))
    attach("m_chest", arm, "chest", H["chest"])
    # ---- neck
    taper("neck", (0, -0.26, 2.16), (0, -0.66, 2.40), 0.17, 0.12, "b_hide", seg=8)
    for k in range(3):
        spike("nsp%d" % k, (0, -0.32 - k * 0.11, 2.28 + k * 0.04), (0, -0.28 - k * 0.11, 2.42 + k * 0.04), 0.03, "b_bone")
    for k in range(6):
        x = -0.12 + k * 0.05
        rod("nmane%d" % k, (x, -0.5, 2.48), (x * 1.6, -0.18, 2.3), 0.03, "b_fur", seg=4)
    attach("m_neck", arm, "neck", H["neck"])
    # ---- head: long canine bone skull (wendigo-like), brow, glowing eyes in deep sockets, fangs, torn ears, bison horns
    box("cranium", (0.32, 0.30, 0.26), (0, -0.70, 2.48), "b_bone", bevel=0.05)
    taper("snout", (0, -0.80, 2.44), (0, -1.24, 2.38), 0.125, 0.06, "b_bone", seg=8)
    box("snoutridge", (0.08, 0.40, 0.05), (0, -1.0, 2.5), "b_bone", (-0.1, 0, 0), bevel=0.015)
    box("nose", (0.10, 0.06, 0.07), (0, -1.25, 2.40), "c_wick", bevel=0.015)
    box("brow", (0.36, 0.08, 0.07), (0, -0.85, 2.56), "b_bone", (-0.25, 0, 0), bevel=0.02)
    for s in (-1, 1):
        box("sock%d" % s, (0.08, 0.04, 0.06), (s * 0.10, -0.865, 2.50), "c_wick")
        box("eye%d" % s, (0.045, 0.02, 0.032), (s * 0.10, -0.88, 2.50), "b_eye")
        box("cheek%d" % s, (0.05, 0.22, 0.08), (s * 0.13, -0.86, 2.40), "b_bone", (0, 0, s * 0.18), bevel=0.015)
        for k in range(5):
            crystal("ft%d%d" % (s, k), (s * 0.075, -0.92 - k * 0.065, 2.335), "b_tooth", h=0.075 if k else 0.11, r=0.013, rot=(math.pi, 0, 0))
        box("ear%d" % s, (0.035, 0.12, 0.2), (s * 0.16, -0.56, 2.62), "b_hide", (-0.6, s * 0.45, 0))
        box("eart%d" % s, (0.03, 0.06, 0.08), (s * 0.19, -0.5, 2.73), "b_hide", (-0.9, s * 0.45, 0.3))
        _horn("horn%d" % s, s, (s * 0.15, -0.66, 2.56), s=1.3)
        cyl("hornring%d" % s, 0.1, 0.05, (s * 0.17, -0.66, 2.57), "c_blacksteel", rot=(0, math.pi / 2, 0), seg=10)
    for k in range(5):
        rod("hmane%d" % k, (-0.1 + k * 0.05, -0.64, 2.61), (-0.14 + k * 0.07, -0.42, 2.45), 0.028, "b_fur", seg=4)
    attach("m_head", arm, "head", H["head"])
    # ---- jaw
    taper("jawb", (0, -0.74, 2.30), (0, -1.16, 2.31), 0.075, 0.045, "b_bone", seg=8)
    box("gum", (0.11, 0.38, 0.02), (0, -0.96, 2.345), "b_gum")
    for s in (-1, 1):
        for k in range(4):
            crystal("lt%d%d" % (s, k), (s * 0.055, -0.95 - k * 0.06, 2.35), "b_tooth", h=0.06 if k else 0.09, r=0.011)
    attach("m_jaw", arm, "jaw", H["jaw"])
    # ---- arms (long, reaching the knees), bone pauldron spikes, black-steel bracers, long clawed fingers
    for side, s in (("L", 1), ("R", -1)):
        sh, el, wr = H["arm_" + side], H["fore_" + side], H["hand_" + side]
        taper("ua" + side, sh, el, 0.13, 0.095, "b_hide", seg=8)
        box("uaw" + side, (0.2, 0.2, 0.08), lerp3(sh, el, 0.55), "b_rag", (0.25, s * 0.25, 0), bevel=0.02)
        attach("m_arm_" + side, arm, "arm_" + side, sh)
        taper("fa" + side, el, wr, 0.095, 0.068, "b_hide", seg=8)
        spike("elb" + side, el, add(el, (s * 0.05, 0.22, -0.02)), 0.045, "b_bone")
        m = lerp3(el, wr, 0.62)
        d = Vector(wr) - Vector(el)
        cyl("brac" + side, 0.088, 0.24, m, "c_blacksteel", rot=d.to_track_quat('Z', 'Y').to_euler(), seg=10)
        for k in range(3):
            p = lerp3(el, wr, 0.5 + k * 0.1)
            spike("bsp%s%d" % (side, k), add(p, (s * 0.07, 0.03, 0)), add(p, (s * 0.2, 0.08, 0.02)), 0.02, "c_blacksteel", seg=4)
        attach("m_fore_" + side, arm, "fore_" + side, el)
        box("palm" + side, (0.12, 0.15, 0.16), add(wr, (0, -0.03, -0.09)), "b_hide", bevel=0.03)
        for k in range(4):
            o = (k - 1.5) * 0.034
            base = add(wr, (s * 0.02 + o, -0.07, -0.16))
            if side == "R":
                tip = add(wr, (o * 1.2 + 0.02, -0.12, -0.26))           # curled round the snath
            else:
                tip = add(wr, (s * 0.04 + o * 1.4, -0.24, -0.42))
            _talon("fg%s%d" % (side, k), base, tip, 0.024, 0.006, 0.04, m="b_hide", n=3)
            spike("cl%s%d" % (side, k), tip, add(tip, (0, -0.07, -0.08)), 0.012, "c_horn", seg=4)
        _talon("th" + side, add(wr, (-s * 0.06, -0.06, -0.1)), add(wr, (-s * 0.08, -0.2, -0.22)), 0.026, 0.008, 0.03, m="b_hide", n=3)
        attach("m_hand_" + side, arm, "hand_" + side, wr)
    # ---- legs: digitigrade, thick thighs, backward hock, long clawed feet, knee spikes
    for side, s in (("L", 1), ("R", -1)):
        hp, kn, an = H["thigh_" + side], H["shin_" + side], H["foot_" + side]
        taper("th" + side, hp, kn, 0.17, 0.115, "b_hide", seg=8)
        box("thw" + side, (0.24, 0.26, 0.1), lerp3(hp, kn, 0.4), "b_rag", (-0.4, 0, 0), bevel=0.02)
        attach("m_thigh_" + side, arm, "thigh_" + side, hp)
        taper("sh" + side, kn, an, 0.105, 0.068, "b_hide", seg=8)
        cyl("kneecap" + side, 0.1, 0.08, add(kn, (0, -0.06, 0)), "b_bone", rot=(math.pi / 2, 0, 0), seg=8)
        spike("knsp" + side, add(kn, (0, -0.08, 0.02)), add(kn, (s * 0.02, -0.3, 0.12)), 0.045, "b_bone")
        box("shplate" + side, (0.1, 0.03, 0.26), add(lerp3(kn, an, 0.5), (0, -0.05, 0)), "b_bone", (-0.65, 0, 0), bevel=0.01)
        attach("m_shin_" + side, arm, "shin_" + side, kn)
        toe = (an[0], -0.20, 0.06)
        taper("ft" + side, an, toe, 0.075, 0.06, "b_hide", seg=8)
        for k in range(3):
            o = (k - 1) * 0.06
            spike("toe%s%d" % (side, k), (an[0] + o, -0.22, 0.05), (an[0] + o * 1.3, -0.44, 0.0), 0.034, "c_horn", seg=5)
        spike("spur" + side, add(an, (0, 0.04, -0.02)), add(an, (0, 0.22, -0.18)), 0.03, "c_horn", seg=5)
        attach("m_foot_" + side, arm, "foot_" + side, an)
    # ---- scythe (own node "scythe" so the renderer can hide it the moment it drops)
    scythe_parts("scy", (-0.72, -0.60, 0.92))
    attach("scythe", arm, "scythe_b", H["scythe_b"])

    # ------------------------------------------------------------------------------------ clips
    I = {"chest": (4, 0, 0), "jaw": (6, 0, 0), "arm_L": (-6, -8, 0), "fore_L": (-12, 0, 0), "arm_R": (-8, 6, 0), "fore_R": (-16, 0, 0)}
    DEAD = {"root": (90, 0, 0), "root_loc": (0, 0, 0.47), "neck": (-25, 0, 0), "head": (0, 0, 60), "jaw": (22, 0, 0),
            "arm_L": (-25, -75, 0), "fore_L": (-30, 0, 0), "arm_R": (-165, 15, 0), "fore_R": (-10, 0, 0),
            "thigh_L": (12, -12, 0), "shin_L": (-15, 0, 0), "thigh_R": (2, 16, 0), "foot_R": (30, 0, 0), "scythe": (0, 0, 90)}
    OVER_HIT = {"root_loc": (0, -0.15, -0.12), "chest": (40, 0, 0), "neck": (-22, 0, 0), "head": (-12, 0, 0), "jaw": (10, 0, 0),
                "arm_R": (-62, 8, 0), "fore_R": (-6, 0, 0), "arm_L": (-58, 24, 0), "fore_L": (-10, 0, 0), "scythe": (46, 0, 0),
                "thigh_L": (-28, 0, 0), "shin_L": (14, 0, 0), "thigh_R": (16, 0, 0)}
    DIAG_HIT = {"root_loc": (0, -0.1, -0.1), "chest": (36, 0, -28), "hips": (0, 0, -10), "neck": (-18, 0, 15), "jaw": (12, 0, 0),
                "arm_R": (-58, 0, -22), "fore_R": (-5, 0, 0), "arm_L": (-52, 22, -16), "fore_L": (-8, 0, 0), "scythe": (52, -20, 0),
                "thigh_L": (-22, 0, 0), "thigh_R": (14, 0, 0)}
    clips = {
        "idle": [(0, I), (1.2, P(I, chest=(0, 0, 1.5), head=(4, 0, -3), jaw=(13, 0, 0), arm_L=(-3, -10, 0), arm_R=(-6, 8, 0), root_loc=(0, 0, 0.02))), (2.4, I)],
        "walk": [],
        "awaken": [
            (0, DEAD), (0.6, DEAD),
            (0.68, P(DEAD, arm_R=(-120, 25, 0), head=(0, 0, 30), chest=(-6, 0, 0))), (0.85, DEAD), (1.4, DEAD),
            (1.5, P(DEAD, root=(78, 0, 0), root_loc=(0, 0, 0.56), chest=(-22, 0, 0), arm_L=(-20, -50, 0), head=(-20, 0, 12), jaw=(36, 0, 0))),
            (1.72, DEAD),
            (2.3, P(DEAD, root=(64, 0, 0), root_loc=(0, 0.1, 0.62), head=(-35, 0, 0), neck=(-20, 0, 0), arm_L=(-85, -10, 0), fore_L=(-20, 0, 0),
                    arm_R=(-85, 10, 0), fore_R=(-20, 0, 0), thigh_L=(18, 0, 0), thigh_R=(18, 0, 0), scythe=(0, 0, 60))),
            (3.1, {"root": (24, 0, 0), "root_loc": (0, 0, -0.2), "chest": (10, 0, 0), "head": (-25, 0, 0), "thigh_L": (-75, 0, 0), "shin_L": (65, 0, 0),
                   "thigh_R": (-25, 0, 0), "shin_R": (30, 0, 0), "arm_L": (-25, -10, 0), "arm_R": (-30, 10, 0), "fore_R": (-30, 0, 0), "jaw": (20, 0, 0)}),
            (3.8, I), (3.95, P(I, head=(0, 0, 38), neck=(0, 0, 10))), (4.2, P(I, head=(0, 0, -32), neck=(0, 0, -8))), (4.45, P(I, head=(-5, 0, 0))), (4.6, I)],
        "swing_over": [
            (0, I),
            (0.45, P(I, chest=(-8, 0, 0), arm_R=(-95, 10, 0), arm_L=(-90, 22, 0), scythe=(-25, 0, 0), jaw=(20, 0, 0))),
            (0.9, {"root_loc": (0, 0.1, 0.05), "chest": (-26, 0, 0), "neck": (-10, 0, 0), "head": (-14, 0, 0), "jaw": (32, 0, 0),
                   "arm_R": (-165, 10, 0), "fore_R": (-10, 0, 0), "arm_L": (-160, 22, 0), "fore_L": (-12, 0, 0), "scythe": (-82, 0, 0),
                   "thigh_L": (-12, 0, 0), "thigh_R": (8, 0, 0)}),
            (1.0, P(OVER_HIT, chest=(8, 0, 0), arm_R=(-125, 8, 0), arm_L=(-120, 22, 0), scythe=(5, 0, 0), root_loc=(0, -0.05, 0))),
            (1.1, OVER_HIT), (1.8, P(OVER_HIT, chest=(37, 0, 0), arm_R=(-60, 8, 0))), (2.4, OVER_HIT)],
        "pull": [
            (0, OVER_HIT), (0.3, P(OVER_HIT, chest=(26, 0, 0), root_loc=(0, -0.05, -0.08), arm_R=(-74, 8, 0), jaw=(30, 0, 0))),
            (0.5, P(OVER_HIT, chest=(38, 0, 0))), (0.75, P(OVER_HIT, chest=(18, 0, 0), arm_R=(-80, 8, 0), head=(-25, 0, 0), jaw=(35, 0, 0))),
            (0.9, P(I, chest=(-10, 0, 0), arm_R=(-120, 8, 0), arm_L=(-100, 20, 0), scythe=(-15, 0, 0))), (1.5, I)],
        "swing_side": [
            (0, I),
            (0.8, {"chest": (6, 0, 55), "hips": (0, 0, 16), "neck": (0, 0, -30), "head": (0, 0, -15), "jaw": (25, 0, 0),
                   "arm_R": (-82, 0, 42), "fore_R": (-8, 0, 0), "arm_L": (-62, 0, 52), "fore_L": (-20, 0, 0), "scythe": (0, 90, 0),
                   "thigh_L": (-14, 0, 0), "thigh_R": (10, 0, 0)}),
            (1.0, {"root_loc": (0, -0.1, -0.03), "chest": (8, 0, -68), "hips": (0, 0, -20), "neck": (0, 0, 25), "jaw": (12, 0, 0),
                   "arm_R": (-82, 0, -20), "fore_R": (-5, 0, 0), "arm_L": (-70, 0, -8), "fore_L": (-12, 0, 0), "scythe": (0, 90, -8),
                   "thigh_L": (-20, 0, 0), "thigh_R": (14, 0, 0)}),
            (1.25, {"chest": (10, 0, -82), "hips": (0, 0, -24), "neck": (0, 0, 30), "arm_R": (-70, 0, -30), "arm_L": (-60, 0, -20),
                    "scythe": (0, 90, -14), "thigh_L": (-18, 0, 0), "thigh_R": (12, 0, 0)}),
            (1.8, I)],
        "swing_diag": [
            (0, I),
            (0.85, {"chest": (-16, 0, 40), "hips": (0, 0, 12), "neck": (-8, 0, -20), "jaw": (30, 0, 0), "arm_R": (-150, 0, 36),
                    "fore_R": (-10, 0, 0), "arm_L": (-140, 26, 30), "fore_L": (-12, 0, 0), "scythe": (-60, 40, 0), "thigh_L": (-10, 0, 0)}),
            (1.0, P(DIAG_HIT, chest=(12, 0, 0), arm_R=(-110, 0, 10), arm_L=(-100, 24, 8), scythe=(20, 10, 0))),
            (1.08, DIAG_HIT), (1.7, P(DIAG_HIT, chest=(33, 0, -26))), (2.2, DIAG_HIT)],
        "pull_diag": [
            (0, DIAG_HIT), (0.3, P(DIAG_HIT, chest=(22, 0, -20), arm_R=(-70, 0, -22))), (0.5, P(DIAG_HIT, chest=(36, 0, -28))),
            (0.75, P(DIAG_HIT, chest=(14, 0, -15), arm_R=(-80, 0, -10), jaw=(35, 0, 0))),
            (0.9, P(I, chest=(-8, 0, 0), arm_R=(-115, 8, 0), scythe=(-15, 0, 0))), (1.5, I)],
        "roar": [
            (0, I), (0.4, P(I, chest=(26, 0, 0), head=(16, 0, 0), arm_L=(-20, -20, 0), arm_R=(-20, 20, 0))),
            (0.9, {"root_loc": (0, 0.05, 0.08), "chest": (-36, 0, 0), "neck": (-25, 0, 0), "head": (-30, 0, 0), "jaw": (48, 0, 0),
                   "arm_L": (-40, -72, 0), "fore_L": (-30, 0, 0), "arm_R": (-40, 72, 0), "fore_R": (-30, 0, 0), "thigh_L": (-10, 0, 0), "thigh_R": (8, 0, 0)}),
            (1.15, {"root_loc": (0, 0.05, 0.08), "chest": (-34, 0, 0), "neck": (-25, 0, 0), "head": (-30, 0, 9), "jaw": (46, 0, 0),
                    "arm_L": (-42, -70, 0), "fore_L": (-30, 0, 0), "arm_R": (-42, 70, 0), "fore_R": (-30, 0, 0), "thigh_L": (-10, 0, 0), "thigh_R": (8, 0, 0)}),
            (1.4, {"root_loc": (0, 0.05, 0.08), "chest": (-36, 0, 0), "neck": (-25, 0, 0), "head": (-30, 0, -9), "jaw": (48, 0, 0),
                   "arm_L": (-40, -72, 0), "fore_L": (-30, 0, 0), "arm_R": (-40, 72, 0), "fore_R": (-30, 0, 0), "thigh_L": (-10, 0, 0), "thigh_R": (8, 0, 0)}),
            (1.65, {"root_loc": (0, 0.05, 0.08), "chest": (-34, 0, 0), "neck": (-25, 0, 0), "head": (-30, 0, 8), "jaw": (44, 0, 0),
                    "arm_L": (-40, -70, 0), "fore_L": (-30, 0, 0), "arm_R": (-40, 70, 0), "fore_R": (-30, 0, 0), "thigh_L": (-10, 0, 0), "thigh_R": (8, 0, 0)}),
            (2.2, I)],
        "climb": [
            (0, {"chest": (-12, 0, 0), "head": (-30, 0, 0), "arm_L": (-172, -12, 0), "fore_L": (-15, 0, 0), "arm_R": (-105, 12, 0), "fore_R": (-40, 0, 0),
                 "thigh_L": (-25, 0, 0), "shin_L": (20, 0, 0), "thigh_R": (-65, 0, 0), "shin_R": (55, 0, 0), "scythe": (-20, 0, 0)}),
            (0.5, {"chest": (-12, 0, 0), "head": (-30, 0, 0), "arm_L": (-105, -12, 0), "fore_L": (-40, 0, 0), "arm_R": (-172, 12, 0), "fore_R": (-15, 0, 0),
                   "thigh_L": (-65, 0, 0), "shin_L": (55, 0, 0), "thigh_R": (-25, 0, 0), "shin_R": (20, 0, 0), "scythe": (-20, 0, 0), "root_loc": (0, 0, 0.1)}),
            (1.0, {"chest": (-12, 0, 0), "head": (-30, 0, 0), "arm_L": (-172, -12, 0), "fore_L": (-15, 0, 0), "arm_R": (-105, 12, 0), "fore_R": (-40, 0, 0),
                   "thigh_L": (-25, 0, 0), "shin_L": (20, 0, 0), "thigh_R": (-65, 0, 0), "shin_R": (55, 0, 0), "scythe": (-20, 0, 0)})],
        # «Выпад жнеца»: deep crouch with the scythe drawn back (readable), then a long leap with a low forward cut
        "lunge": [
            (0, I),
            (0.55, {"root_loc": (0, 0.18, -0.38), "chest": (34, 0, 0), "neck": (-26, 0, 0), "head": (-18, 0, 0), "jaw": (26, 0, 0),
                    "thigh_L": (-62, 0, 0), "shin_L": (74, 0, 0), "foot_L": (-12, 0, 0), "thigh_R": (-38, 0, 0), "shin_R": (64, 0, 0),
                    "arm_R": (-28, 14, 0), "fore_R": (-30, 0, 0), "arm_L": (-24, 26, 0), "fore_L": (-26, 0, 0), "scythe": (-48, 0, 0)}),
            (0.62, {"root_loc": (0, 0.2, -0.42), "chest": (37, 0, 0), "neck": (-28, 0, 0), "head": (-20, 0, 0), "jaw": (30, 0, 0),
                    "thigh_L": (-66, 0, 0), "shin_L": (78, 0, 0), "foot_L": (-12, 0, 0), "thigh_R": (-40, 0, 0), "shin_R": (66, 0, 0),
                    "arm_R": (-26, 14, 0), "fore_R": (-32, 0, 0), "arm_L": (-22, 26, 0), "fore_L": (-28, 0, 0), "scythe": (-52, 0, 0)}),
            (0.78, {"root_loc": (0, -0.35, 0.12), "chest": (22, 0, 0), "neck": (-14, 0, 0), "head": (-10, 0, 0), "jaw": (44, 0, 0),
                    "thigh_L": (-48, 0, 0), "shin_L": (12, 0, 0), "thigh_R": (34, 0, 0), "shin_R": (14, 0, 0), "foot_R": (20, 0, 0),
                    "arm_R": (-102, 0, 0), "fore_R": (-2, 0, 0), "arm_L": (-96, 20, 0), "fore_L": (-6, 0, 0), "scythe": (58, 0, 0)}),
            (1.05, {"root_loc": (0, -0.3, -0.05), "chest": (30, 0, 0), "neck": (-18, 0, 0), "jaw": (30, 0, 0),
                    "thigh_L": (-40, 0, 0), "shin_L": (30, 0, 0), "thigh_R": (22, 0, 0), "shin_R": (20, 0, 0),
                    "arm_R": (-84, 0, 0), "fore_R": (-6, 0, 0), "arm_L": (-80, 20, 0), "fore_L": (-8, 0, 0), "scythe": (52, 0, 0)}),
            (1.7, I)],
        # stagger: a heavy blow knocks him off balance - reels back, scythe arm flung out, head snaps aside, recovers
        "stagger": [
            (0, I),
            (0.12, {"root_loc": (0, 0.22, -0.06), "chest": (-24, 0, 14), "hips": (-6, 0, 4), "neck": (-20, 0, -18), "head": (-26, 0, -34), "jaw": (44, 0, 0),
                    "arm_L": (-30, -62, 0), "fore_L": (-34, 0, 0), "arm_R": (-36, 58, 0), "fore_R": (-28, 0, 0), "scythe": (-10, 0, 40),
                    "thigh_L": (12, 0, 0), "shin_L": (14, 0, 0), "thigh_R": (-30, 0, 0), "shin_R": (34, 0, 0)}),
            (0.3, {"root_loc": (0, 0.34, -0.12), "chest": (-14, 0, 20), "hips": (-4, 0, 6), "neck": (-12, 0, -22), "head": (-18, 0, -26), "jaw": (36, 0, 0),
                   "arm_L": (-20, -70, 0), "fore_L": (-30, 0, 0), "arm_R": (-30, 62, 0), "fore_R": (-24, 0, 0), "scythe": (-6, 0, 46),
                   "thigh_L": (20, 0, 0), "shin_L": (20, 0, 0), "thigh_R": (-42, 0, 0), "shin_R": (52, 0, 0)}),
            (0.55, {"root_loc": (0, 0.2, -0.22), "chest": (30, 0, -6), "neck": (-30, 0, 8), "head": (-10, 0, 12), "jaw": (30, 0, 0),
                    "arm_L": (-40, -20, 0), "fore_L": (-40, 0, 0), "arm_R": (-20, 20, 0), "fore_R": (-30, 0, 0), "scythe": (20, 0, 10),
                    "thigh_L": (-50, 0, 0), "shin_L": (64, 0, 0), "thigh_R": (-30, 0, 0), "shin_R": (40, 0, 0)}),
            (0.75, P(I, head=(-6, 0, 26), neck=(0, 0, 8), jaw=(20, 0, 0))), (0.82, P(I, head=(-4, 0, -18))), (1.0, I)],
        # «Огненный разрез»: the blade drags through the floor on his right, then rips across - a wave of fire runs at the player
        "scrape": [
            (0, I),
            (0.5, P(DIAG_HIT, root_loc=(0, 0, -0.12), chest=(30, 0, 46), hips=(0, 0, 14), neck=(-16, 0, -26), jaw=(22, 0, 0),
                    arm_R=(-38, 0, 46), fore_R=(-12, 0, 0), arm_L=(-42, 18, 36), fore_L=(-16, 0, 0), scythe=(48, 44, 0))),
            (0.85, P(DIAG_HIT, root_loc=(0, 0, -0.16), chest=(34, 0, 52), hips=(0, 0, 16), neck=(-18, 0, -30), jaw=(30, 0, 0),
                     arm_R=(-34, 0, 50), fore_R=(-12, 0, 0), arm_L=(-40, 18, 40), fore_L=(-16, 0, 0), scythe=(50, 48, 0))),
            (1.05, P(DIAG_HIT, root_loc=(0, -0.12, -0.14), chest=(30, 0, -44), hips=(0, 0, -16), neck=(-14, 0, 28), jaw=(46, 0, 0),
                     arm_R=(-62, 0, -34), fore_R=(-4, 0, 0), arm_L=(-56, 22, -26), fore_L=(-8, 0, 0), scythe=(56, -34, 0))),
            (1.4, P(DIAG_HIT, chest=(28, 0, -50), hips=(0, 0, -18), neck=(-12, 0, 30), jaw=(34, 0, 0),
                    arm_R=(-58, 0, -40), arm_L=(-52, 22, -30), scythe=(54, -40, 0))),
            (2.0, I)],
        "death": [
            (0, I), (0.3, P(I, chest=(-26, 0, 0), head=(-35, 0, 0), jaw=(42, 0, 0), arm_L=(-30, -40, 0), arm_R=(-30, 40, 0))),
            (1.0, {"root": (14, 0, 0), "root_loc": (0, 0, -0.5), "chest": (12, 0, 0), "head": (10, 0, 0), "jaw": (30, 0, 0),
                   "thigh_L": (-85, 0, 0), "shin_L": (75, 0, 0), "thigh_R": (-80, 0, 0), "shin_R": (70, 0, 0), "arm_L": (-10, -15, 0), "arm_R": (-10, 15, 0)}),
            (1.7, {"root": (45, 0, 0), "root_loc": (0, -0.1, -0.35), "chest": (30, 0, 0), "head": (25, 0, 0), "jaw": (30, 0, 0),
                   "thigh_L": (-60, 0, 0), "shin_L": (60, 0, 0), "thigh_R": (-55, 0, 0), "shin_R": (55, 0, 0), "arm_L": (-40, -20, 0), "arm_R": (-40, 20, 0)}),
            (2.3, DEAD), (3.0, DEAD)],
        "dead": [(0, DEAD), (2.0, DEAD)],
    }
    # walk: 1 cycle = 2 steps (the renderer drives it by distance walked)
    W0 = {"chest": (10, 0, -5), "hips": (0, 0, 6), "head": (-6, 0, 4), "jaw": (8, 0, 0),
          "thigh_L": (-28, 0, 0), "shin_L": (5, 0, 0), "foot_L": (-8, 0, 0), "thigh_R": (22, 0, 0), "shin_R": (8, 0, 0), "foot_R": (12, 0, 0),
          "arm_L": (22, -8, 0), "fore_L": (-10, 0, 0), "arm_R": (-14, 6, 0), "fore_R": (-18, 0, 0)}
    W1 = {"chest": (12, 0, 0), "hips": (0, 0, 0), "head": (-2, 0, 0), "jaw": (12, 0, 0), "root_loc": (0, 0, 0.06),
          "thigh_L": (-4, 0, 0), "shin_L": (0, 0, 0), "thigh_R": (-12, 0, 0), "shin_R": (38, 0, 0), "foot_R": (-15, 0, 0),
          "arm_L": (0, -8, 0), "fore_L": (-12, 0, 0), "arm_R": (-8, 6, 0), "fore_R": (-16, 0, 0)}

    def mirror(p):
        q = {}
        for k, v in p.items():
            if k.endswith("_L"):
                q[k[:-2] + "_R"] = v if not k.startswith(("arm", "fore")) else (v[0], -v[1], v[2])
            elif k.endswith("_R"):
                q[k[:-2] + "_L"] = v if not k.startswith(("arm", "fore")) else (v[0], -v[1], v[2])
            elif k in ("chest", "hips", "head"):
                q[k] = (v[0], v[1], -v[2])
            else:
                q[k] = v
        # the scythe arm swings less
        if "arm_R" in q:
            q["arm_R"] = (q["arm_R"][0] * 0.55 - 6, q["arm_R"][1], q["arm_R"][2])
        return q
    clips["walk"] = [(0, W0), (0.3, W1), (0.6, mirror(W0)), (0.9, mirror(W1)), (1.2, W0)]
    chains = {"scythe_b": ("scythe", ["arm_R", "fore_R", "hand_R"])}
    bake_clips(arm, clips, chains)
    export_anim("boss")


# ================================================================================================ guards
GUARD_BONES = [
    ("root", None, (0, 0, 0)),
    ("hips", "root", (0, 0.05, 0.92)),
    ("chest", "hips", (0, -0.08, 1.24)),
    ("neck", "chest", (0, -0.30, 1.56)),
    ("head", "neck", (0, -0.40, 1.64)),
    ("jaw", "head", (0, -0.47, 1.58)),
    ("arm_L", "chest", (0.30, -0.18, 1.50)), ("fore_L", "arm_L", (0.40, -0.28, 1.04)), ("hand_L", "fore_L", (0.44, -0.40, 0.60)),
    ("arm_R", "chest", (-0.30, -0.18, 1.50)), ("fore_R", "arm_R", (-0.40, -0.28, 1.04)), ("hand_R", "fore_R", (-0.44, -0.40, 0.60)),
    ("wpn_b", "hand_R", (-0.44, -0.44, 0.55)),
    ("thigh_L", "hips", (0.16, 0.05, 0.90)), ("shin_L", "thigh_L", (0.19, -0.16, 0.50)), ("foot_L", "shin_L", (0.19, 0.08, 0.16)),
    ("thigh_R", "hips", (-0.16, 0.05, 0.90)), ("shin_R", "thigh_R", (-0.19, -0.16, 0.50)), ("foot_R", "shin_R", (-0.19, 0.08, 0.16)),
]


def m_guard():
    H = {b: h for b, _, h in GUARD_BONES}
    arm = make_rig("guard", GUARD_BONES)
    rnd = random.Random(7)
    # hips: bony pelvis, rope belt, ragged red tabard skirt
    box("pelvis", (0.34, 0.24, 0.18), (0, 0.04, 0.93), "b_ash", bevel=0.04)
    for s in (-1, 1):
        box("ilium%d" % s, (0.08, 0.18, 0.1), (s * 0.18, 0.03, 0.98), "b_bone", (0, s * 0.35, 0), bevel=0.02)
    torus("belt", 0.2, 0.02, (0, 0.04, 1.02), "e_rope", seg=14, seg2=4)
    for k in range(5):
        x = -0.14 + k * 0.07
        L = 0.38 + rnd.random() * 0.22
        box("tf%d" % k, (0.065, 0.015, L), (x, -0.13, 1.02 - L / 2), "b_rag", (0.05, 0, 0.12 * math.sin(k * 2.3)))
        L = 0.42 + rnd.random() * 0.2
        box("tb%d" % k, (0.065, 0.015, L), (x, 0.2, 1.02 - L / 2), "b_rag", (-0.06, 0, 0.1 * math.cos(k * 1.9)))
    attach("m_hips", arm, "hips", H["hips"])
    # chest: emaciated ribcage, spine knobs, tabard with brass horn emblem, collar of chains, back spikes
    box("torso", (0.40, 0.26, 0.40), (0, -0.06, 1.34), "b_ash", (0.35, 0, 0), bevel=0.06)
    taper("waist", (0, 0.04, 0.98), (0, -0.02, 1.2), 0.11, 0.15, "b_ash", seg=8)
    for s in (-1, 1):
        for k in range(5):
            z = 1.17 + k * 0.065
            rod("rib%d%d" % (s, k), (s * 0.04, -0.21 + k * 0.012, z), (s * 0.21, -0.07, z - 0.02), 0.014, "b_bone", seg=4)
            rod("ribb%d%d" % (s, k), (s * 0.21, -0.07, z - 0.02), (s * 0.18, 0.08, z + 0.01), 0.013, "b_bone", seg=4)
    box("tabard", (0.26, 0.02, 0.34), (0, -0.215, 1.24), "b_rag", (0.3, 0, 0))
    box("emb", (0.07, 0.01, 0.07), (0, -0.235, 1.33), "c_brass", (0.3, 0, 0.785))
    for k in range(5):
        u = k / 4
        p = lerp3((0, 0.14, 1.12), (0, 0.04, 1.56), u)
        spike("bsp%d" % k, p, add(p, (0, 0.13, 0.08)), 0.025, "b_bone")
    for k in range(6):
        a = k / 6 * math.pi * 2
        torus("col%d" % k, 0.03, 0.008, (math.cos(a) * 0.17, -0.2 + math.sin(a) * 0.1, 1.55), "c_blacksteel", rot=(0, math.pi / 2 * (k % 2), a), seg=8, seg2=3)
    for s in (-1, 1):
        box("shp%d" % s, (0.18, 0.2, 0.05), (s * 0.27, -0.16, 1.58), "b_bone", (0.1, s * -0.6, 0), bevel=0.015)
        spike("shs%d" % s, (s * 0.3, -0.16, 1.6), (s * 0.4, -0.1, 1.8), 0.025, "c_horn")
    attach("m_chest", arm, "chest", H["chest"])
    taper("neck", (0, -0.18, 1.5), (0, -0.42, 1.65), 0.075, 0.055, "b_ash", seg=6)
    attach("m_neck", arm, "neck", H["neck"])
    # head: bone mask with eye slits glowing, short curled horns, hood rag
    box("skull", (0.2, 0.22, 0.2), (0, -0.44, 1.7), "b_ash", bevel=0.04)
    box("mask", (0.22, 0.05, 0.26), (0, -0.56, 1.68), "b_bone", (-0.1, 0, 0), bevel=0.02)
    taper("masksn", (0, -0.58, 1.62), (0, -0.70, 1.56), 0.06, 0.03, "b_bone", seg=6)
    for s in (-1, 1):
        box("slit%d" % s, (0.06, 0.02, 0.018), (s * 0.05, -0.59, 1.72), "b_eye", (0, 0, s * -0.3))
        _horn("horn%d" % s, s, (s * 0.09, -0.46, 1.79), s=0.55)
    for k in range(5):
        x = -0.1 + k * 0.05
        box("hood%d" % k, (0.06, 0.02, 0.3), (x, -0.32, 1.66), "b_rag", (-0.25, 0, 0.1 * (k - 2)))
    box("hoodtop", (0.24, 0.24, 0.04), (0, -0.42, 1.81), "b_rag", (-0.1, 0, 0))
    attach("m_head", arm, "head", H["head"])
    box("jawb", (0.12, 0.16, 0.04), (0, -0.55, 1.56), "b_bone", bevel=0.01)
    for k in range(4):
        crystal("jt%d" % k, (-0.045 + k * 0.03, -0.62, 1.585), "b_tooth", h=0.04, r=0.008)
    attach("m_jaw", arm, "jaw", H["jaw"])
    for side, s in (("L", 1), ("R", -1)):
        sh, el, wr = H["arm_" + side], H["fore_" + side], H["hand_" + side]
        taper("ua" + side, sh, el, 0.065, 0.05, "b_ash", seg=6)
        cyl("uaw" + side, 0.07, 0.12, lerp3(sh, el, 0.5), "b_rag", rot=(Vector(el) - Vector(sh)).to_track_quat('Z', 'Y').to_euler(), seg=6)
        attach("m_arm_" + side, arm, "arm_" + side, sh)
        taper("fa" + side, el, wr, 0.05, 0.038, "b_ash", seg=6)
        spike("elb" + side, el, add(el, (s * 0.02, 0.14, 0)), 0.025, "b_bone")
        torus("fw" + side, 0.048, 0.012, lerp3(el, wr, 0.7), "e_rope", rot=(Vector(wr) - Vector(el)).to_track_quat('Z', 'Y').to_euler(), seg=8, seg2=3)
        attach("m_fore_" + side, arm, "fore_" + side, el)
        box("palm" + side, (0.07, 0.09, 0.09), add(wr, (0, -0.02, -0.05)), "b_ash", bevel=0.02)
        for k in range(4):
            o = (k - 1.5) * 0.02
            base = add(wr, (o, -0.05, -0.09))
            tip = add(wr, (o * 1.4, -0.1, -0.16)) if side == "R" else add(wr, (s * 0.02 + o * 1.6, -0.17, -0.3))
            _talon("fg%s%d" % (side, k), base, tip, 0.014, 0.004, 0.025, m="b_ash", n=3)
            spike("cl%s%d" % (side, k), tip, add(tip, (0, -0.05, -0.06)), 0.008, "c_horn", seg=4)
        attach("m_hand_" + side, arm, "hand_" + side, wr)
    for side, s in (("L", 1), ("R", -1)):
        hp, kn, an = H["thigh_" + side], H["shin_" + side], H["foot_" + side]
        taper("th" + side, hp, kn, 0.08, 0.06, "b_ash", seg=6)
        attach("m_thigh_" + side, arm, "thigh_" + side, hp)
        taper("sh" + side, kn, an, 0.055, 0.04, "b_ash", seg=6)
        cyl("kn" + side, 0.06, 0.05, add(kn, (0, -0.04, 0)), "b_bone", rot=(math.pi / 2, 0, 0), seg=6)
        for k in range(3):
            torus("lw%s%d" % (side, k), 0.05, 0.01, lerp3(kn, an, 0.3 + k * 0.2), "b_rag", rot=(Vector(an) - Vector(kn)).to_track_quat('Z', 'Y').to_euler(), seg=8, seg2=3)
        attach("m_shin_" + side, arm, "shin_" + side, kn)
        toe = (an[0], -0.16, 0.04)
        taper("ft" + side, an, toe, 0.045, 0.035, "b_ash", seg=6)
        for k in range(3):
            o = (k - 1) * 0.035
            spike("toe%s%d" % (side, k), (an[0] + o, -0.17, 0.035), (an[0] + o * 1.3, -0.32, 0.0), 0.02, "c_horn", seg=4)
        attach("m_foot_" + side, arm, "foot_" + side, an)
    sickle_parts("wp", H["wpn_b"])
    attach("weapon", arm, "wpn_b", H["wpn_b"])

    I = {"chest": (24, 0, 0), "neck": (-18, 0, 0), "head": (-10, 0, 0), "jaw": (8, 0, 0), "arm_L": (-12, -6, 0), "fore_L": (-18, 0, 0),
         "arm_R": (-20, 6, 0), "fore_R": (-30, 0, 0), "thigh_L": (-14, 0, 0), "shin_L": (18, 0, 0), "thigh_R": (-10, 0, 0), "shin_R": (14, 0, 0),
         "root_loc": (0, 0, -0.06), "wpn": (70, 0, 0)}
    R0 = {"chest": (48, 0, -6), "neck": (-35, 0, 0), "head": (-15, 0, 4), "jaw": (14, 0, 0), "root_loc": (0, 0, -0.12),
          "thigh_L": (-48, 0, 0), "shin_L": (20, 0, 0), "foot_L": (-10, 0, 0), "thigh_R": (30, 0, 0), "shin_R": (20, 0, 0), "foot_R": (25, 0, 0),
          "arm_L": (40, -8, 0), "fore_L": (-20, 0, 0), "arm_R": (30, 8, 0), "fore_R": (-50, 0, 0), "wpn": (110, 0, 0)}
    R1 = {"chest": (52, 0, 0), "neck": (-38, 0, 0), "head": (-15, 0, 0), "jaw": (18, 0, 0), "root_loc": (0, 0, -0.02),
          "thigh_L": (-10, 0, 0), "shin_L": (10, 0, 0), "thigh_R": (-20, 0, 0), "shin_R": (70, 0, 0), "foot_R": (-20, 0, 0),
          "arm_L": (0, -8, 0), "fore_L": (-30, 0, 0), "arm_R": (25, 8, 0), "fore_R": (-50, 0, 0), "wpn": (110, 0, 0)}

    def mir(p):
        q = {}
        for k, v in p.items():
            if k.endswith("_L") and not k.startswith(("arm", "fore")):
                q[k[:-2] + "_R"] = v
            elif k.endswith("_R") and not k.startswith(("arm", "fore")):
                q[k[:-2] + "_L"] = v
            elif k in ("chest", "head"):
                q[k] = (v[0], v[1], -v[2])
            else:
                q[k] = v
        q["arm_L"] = (-p["arm_L"][0] * 0.8, p["arm_L"][1], 0)
        return q
    C0 = {"chest": (50, 0, 0), "hips": (22, 0, 0), "neck": (-55, 0, 0), "head": (-15, 0, 0), "jaw": (15, 0, 0), "root_loc": (0, 0.1, -0.25),
          "arm_L": (-70, -10, 0), "fore_L": (-10, 0, 0), "arm_R": (-30, 10, 0), "fore_R": (-30, 0, 0),
          "thigh_L": (-75, 0, 0), "shin_L": (60, 0, 0), "thigh_R": (-35, 0, 0), "shin_R": (40, 0, 0), "wpn": (90, 0, 0)}
    C1 = {"chest": (50, 0, 0), "hips": (22, 0, 0), "neck": (-55, 0, 0), "head": (-15, 0, 0), "jaw": (15, 0, 0), "root_loc": (0, 0.1, -0.3),
          "arm_L": (-30, -10, 0), "fore_L": (-30, 0, 0), "arm_R": (-75, 10, 0), "fore_R": (-10, 0, 0),
          "thigh_L": (-35, 0, 0), "shin_L": (40, 0, 0), "thigh_R": (-75, 0, 0), "shin_R": (60, 0, 0), "wpn": (90, 0, 0)}
    WU = {"chest": (58, 0, 28), "hips": (0, 0, 8), "neck": (-55, 0, -20), "head": (-20, 0, 0), "jaw": (34, 0, 0), "root_loc": (0, 0.05, -0.3),
          "thigh_L": (-70, 0, 0), "shin_L": (65, 0, 0), "thigh_R": (-20, 0, 0), "shin_R": (55, 0, 0), "foot_R": (20, 0, 0),
          "arm_L": (-40, -30, 0), "fore_L": (-30, 0, 0), "arm_R": (45, 35, 0), "fore_R": (-40, 0, 0), "wpn": (150, 0, 30)}
    clips = {
        "idle": [(0, I), (0.4, P(I, head=(-10, 0, 22))), (0.48, P(I, head=(-14, 0, -18))), (0.8, P(I, chest=(20, 0, 0), jaw=(14, 0, 0), root_loc=(0, 0, -0.04))),
                 (1.2, P(I, head=(-6, 0, 0))), (1.6, I)],
        "run": [(0, R0), (0.15, R1), (0.3, mir(R0)), (0.45, mir(R1)), (0.6, R0)],
        "crawl": [(0, C0), (0.3, C1), (0.6, C0)],
        # leap from the balcony: sinks into a spring crouch (claw on the railing), launches stretched out, tucks for the fall
        "leap": [(0, I),
                 (0.25, {"chest": (62, 0, 0), "hips": (18, 0, 0), "neck": (-58, 0, 0), "head": (-12, 0, 0), "jaw": (22, 0, 0), "root_loc": (0, 0.08, -0.42),
                         "thigh_L": (-88, 0, 0), "shin_L": (96, 0, 0), "foot_L": (-10, 0, 0), "thigh_R": (-82, 0, 0), "shin_R": (92, 0, 0),
                         "arm_L": (-62, -12, 0), "fore_L": (-12, 0, 0), "arm_R": (30, 20, 0), "fore_R": (-40, 0, 0), "wpn": (120, 0, 0)}),
                 (0.33, {"chest": (66, 0, 0), "hips": (20, 0, 0), "neck": (-60, 0, 0), "head": (-14, 0, 0), "jaw": (34, 0, 0), "root_loc": (0, 0.1, -0.46),
                         "thigh_L": (-92, 0, 0), "shin_L": (100, 0, 0), "foot_L": (-10, 0, 0), "thigh_R": (-86, 0, 0), "shin_R": (96, 0, 0),
                         "arm_L": (-64, -12, 0), "fore_L": (-12, 0, 0), "arm_R": (34, 20, 0), "fore_R": (-42, 0, 0), "wpn": (125, 0, 0)}),
                 (0.45, {"chest": (18, 0, 0), "neck": (-22, 0, 0), "head": (-10, 0, 0), "jaw": (40, 0, 0), "root_loc": (0, -0.1, 0.1),
                         "arm_L": (-150, -30, 0), "fore_L": (-8, 0, 0), "arm_R": (-140, 30, 0), "fore_R": (-15, 0, 0),
                         "thigh_L": (14, 0, 0), "shin_L": (8, 0, 0), "thigh_R": (22, 0, 0), "shin_R": (14, 0, 0), "foot_R": (25, 0, 0), "wpn": (40, 0, 0)}),
                 (0.8, {"chest": (34, 0, 0), "neck": (-36, 0, 0), "jaw": (22, 0, 0), "arm_L": (-95, -45, 0), "fore_L": (-20, 0, 0), "arm_R": (-85, 45, 0),
                        "fore_R": (-30, 0, 0), "thigh_L": (-70, 0, 0), "shin_L": (75, 0, 0), "thigh_R": (-58, 0, 0), "shin_R": (62, 0, 0), "wpn": (100, 0, 0)}),
                 (1.4, {"chest": (30, 0, 0), "neck": (-32, 0, 0), "jaw": (26, 0, 0), "arm_L": (-105, -50, 0), "fore_L": (-15, 0, 0), "arm_R": (-95, 50, 0),
                        "fore_R": (-25, 0, 0), "thigh_L": (-55, 0, 0), "shin_L": (55, 0, 0), "thigh_R": (-45, 0, 0), "shin_R": (45, 0, 0), "wpn": (100, 0, 0)})],
        # landing: absorbs the drop in a deep crouch with the free claw on the floor, head snapping up to find the player
        "land": [(0, {"chest": (68, 0, 0), "hips": (24, 0, 0), "neck": (-62, 0, 0), "head": (-20, 0, 0), "jaw": (30, 0, 0), "root_loc": (0, 0.12, -0.5),
                      "thigh_L": (-95, 0, 0), "shin_L": (105, 0, 0), "foot_L": (-12, 0, 0), "thigh_R": (-70, 0, 0), "shin_R": (110, 0, 0), "foot_R": (10, 0, 0),
                      "arm_L": (-48, -8, 0), "fore_L": (-30, 0, 0), "arm_R": (20, 40, 0), "fore_R": (-50, 0, 0), "wpn": (140, 0, 20)}),
                 (0.3, {"chest": (64, 0, 0), "hips": (22, 0, 0), "neck": (-60, 0, 0), "head": (-30, 0, 18), "jaw": (36, 0, 0), "root_loc": (0, 0.1, -0.46),
                        "thigh_L": (-90, 0, 0), "shin_L": (100, 0, 0), "foot_L": (-12, 0, 0), "thigh_R": (-68, 0, 0), "shin_R": (104, 0, 0), "foot_R": (10, 0, 0),
                        "arm_L": (-50, -8, 0), "fore_L": (-30, 0, 0), "arm_R": (24, 40, 0), "fore_R": (-50, 0, 0), "wpn": (140, 0, 20)}),
                 (0.38, {"chest": (60, 0, 0), "hips": (20, 0, 0), "neck": (-58, 0, 0), "head": (-28, 0, -16), "jaw": (30, 0, 0), "root_loc": (0, 0.1, -0.44),
                         "thigh_L": (-88, 0, 0), "shin_L": (96, 0, 0), "thigh_R": (-66, 0, 0), "shin_R": (100, 0, 0),
                         "arm_L": (-50, -8, 0), "fore_L": (-30, 0, 0), "arm_R": (24, 40, 0), "fore_R": (-50, 0, 0), "wpn": (140, 0, 20)}),
                 (0.85, I)],
        "windup": [(0, I), (0.35, WU), (0.6, P(WU, head=(-24, 0, 14))), (0.66, P(WU, head=(-18, 0, -12))), (0.9, WU),
                   (1.2, P(WU, chest=(60, 0, 30), jaw=(40, 0, 0))), (1.26, P(WU, head=(-24, 0, 16))), (1.32, WU), (1.6, P(WU, chest=(62, 0, 32)))],
        "dash": [(0, P(WU, chest=(62, 0, 32))),
                 (0.15, {"chest": (65, 0, -25), "neck": (-55, 0, 15), "jaw": (40, 0, 0), "root_loc": (0, -0.1, -0.2),
                         "arm_R": (-125, 0, -20), "fore_R": (-5, 0, 0), "arm_L": (30, -20, 0), "thigh_L": (-60, 0, 0), "shin_L": (30, 0, 0),
                         "thigh_R": (45, 0, 0), "shin_R": (30, 0, 0), "wpn": (120, 0, -20)}),
                 (0.35, {"chest": (60, 0, -35), "neck": (-50, 0, 20), "jaw": (20, 0, 0), "root_loc": (0, -0.1, -0.2),
                         "arm_R": (-40, 0, -35), "fore_R": (-5, 0, 0), "arm_L": (20, -20, 0), "thigh_L": (-50, 0, 0), "shin_L": (40, 0, 0),
                         "thigh_R": (35, 0, 0), "shin_R": (40, 0, 0), "wpn": (130, 0, -30)}),
                 (0.6, I)],
        "slash": [(0, I), (0.25, P(I, chest=(18, 0, 25), arm_R=(-150, 20, 0), fore_R=(-20, 0, 0), jaw=(30, 0, 0), wpn=(40, 0, 0))),
                  (0.4, P(I, chest=(40, 0, -25), arm_R=(-35, 0, -20), fore_R=(-5, 0, 0), wpn=(120, 0, -10), root_loc=(0, -0.08, -0.08))),
                  (0.8, I)],
        "death": [(0, I), (0.25, P(I, chest=(-10, 0, 0), head=(-40, 0, 0), jaw=(40, 0, 0), arm_L=(-40, -50, 0), arm_R=(-40, 50, 0))),
                  (0.7, {"root": (20, 0, 0), "root_loc": (0, 0, -0.38), "chest": (25, 0, 0), "head": (20, 0, 0), "thigh_L": (-85, 0, 0), "shin_L": (80, 0, 0),
                         "thigh_R": (-80, 0, 0), "shin_R": (75, 0, 0), "arm_L": (-10, 0, 0), "arm_R": (-10, 0, 0), "wpn": (70, 0, 0)}),
                  (1.1, {"root": (90, 0, 0), "root_loc": (0, 0, 0.26), "neck": (-20, 0, 0), "head": (0, 0, 50), "arm_L": (-30, -60, 0), "arm_R": (-150, 20, 0),
                         "thigh_L": (10, -10, 0), "thigh_R": (0, 12, 0), "wpn": (0, 0, 90)}),
                  (1.4, {"root": (90, 0, 0), "root_loc": (0, 0, 0.26), "neck": (-20, 0, 0), "head": (0, 0, 50), "arm_L": (-30, -60, 0), "arm_R": (-150, 20, 0),
                         "thigh_L": (10, -10, 0), "thigh_R": (0, 12, 0), "wpn": (0, 0, 90)})],
    }
    chains = {"wpn_b": ("wpn", ["arm_R", "fore_R", "hand_R"])}
    bake_clips(arm, clips, chains)
    export_anim("guard")


# ================================================================================================ weapon items
def m_scythe():
    scythe_parts("scy", (0, 0, 0))
    node("scythe")
    export("scythe")


def m_guard_sickle():
    sickle_parts("wp", (0, 0, 0))
    node("guard_sickle")
    export("guard_sickle")


for name, fn in (("boss", m_boss), ("guard", m_guard), ("scythe", m_scythe), ("guard_sickle", m_guard_sickle)):
    if WANT and name not in WANT:
        continue
    reset()
    fn()
print("CREATURES_DONE")
