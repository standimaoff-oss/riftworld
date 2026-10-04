# Riftworld block-entity models (Blender 5.x, run headless with --factory-startup).
#   blender -b --factory-startup --python tools/rift/models.py [-- name1,name2]
# Units: 1 Blender unit = 1 block. Master block centre at (0,0), floor z = 0. Front of every model faces -Y
# (glTF +Z = Minecraft south = towards the player for FACING=south); +X = the viewer's right.
# Animated parts are separate nodes whose origin is the pivot (Pose.rotate / translate in the renderer).
import bpy, bmesh, math, os, sys
from mathutils import Vector, Matrix, Euler

TEX = "/home/user/tools/rift/tex_models"
OUT = "/home/user/mods/riftworld/src/main/resources/assets/riftworld/models/glb"
BLEND = "/home/user/models/rift"
os.makedirs(OUT, exist_ok=True)
os.makedirs(BLEND, exist_ok=True)
ONLY = set(sys.argv[sys.argv.index("--") + 1].split(",")) if "--" in sys.argv else None

MATS = {}
PARTS = []          # objects of the node being built


def reset():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    MATS.clear()
    PARTS.clear()


def mat(name):
    if name in MATS:
        return MATS[name]
    spec = MATDEF[name]
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    nt = m.node_tree
    bsdf = nt.nodes.get("Principled BSDF")
    bsdf.inputs["Roughness"].default_value = spec.get("rough", 0.7)
    bsdf.inputs["Metallic"].default_value = spec.get("metal", 0.0)
    col = spec.get("color", (1, 1, 1, 1))
    if "tex" in spec:
        im = bpy.data.images.load(os.path.join(TEX, spec["tex"] + ".png"), check_existing=True)
        im.pack()
        tn = nt.nodes.new("ShaderNodeTexImage")
        tn.image = im
        tn.interpolation = 'Closest'
        nt.links.new(tn.outputs["Color"], bsdf.inputs["Base Color"])
        if spec.get("emit"):
            nt.links.new(tn.outputs["Color"], bsdf.inputs["Emission Color"])
            bsdf.inputs["Emission Strength"].default_value = 1.0
    else:
        bsdf.inputs["Base Color"].default_value = col
        if spec.get("emit"):
            bsdf.inputs["Emission Color"].default_value = col
            bsdf.inputs["Emission Strength"].default_value = 1.0
    if col[3] < 1.0:
        bsdf.inputs["Alpha"].default_value = col[3]
        if "tex" in spec:
            bsdf.inputs["Base Color"].default_value = col
        m.blend_method = 'BLEND' if hasattr(m, "blend_method") else None
        try:
            m.surface_render_method = 'BLENDED'
        except Exception:
            pass
    MATS[name] = m
    return m


MATDEF = {
    "white": {"tex": "m_labwhite", "rough": 0.6},
    "dark": {"tex": "m_darkmetal", "metal": 0.6, "rough": 0.5},
    "steel": {"tex": "m_steel", "metal": 0.8, "rough": 0.35},
    "rust": {"tex": "m_rust", "metal": 0.3, "rough": 0.9},
    "wood": {"tex": "m_wood", "rough": 0.9},
    "teal": {"tex": "m_teal", "metal": 0.3, "rough": 0.5},
    "screen": {"tex": "m_screen", "emit": True, "rough": 0.2},
    "screen_dead": {"tex": "m_screen_dead", "rough": 0.2},
    "hazard": {"tex": "m_hazard", "rough": 0.7},
    "plant": {"tex": "m_deadplant", "rough": 0.9},
    "olive": {"tex": "m_crate_olive", "rough": 0.8},
    "rubber": {"tex": "m_rubber", "rough": 0.95},
    "copper": {"tex": "m_copper", "metal": 0.9, "rough": 0.35},
    "glass": {"color": (0.78, 0.92, 0.95, 0.22), "rough": 0.05},
    "liquid": {"color": (0.42, 0.40, 0.25, 0.55), "rough": 0.1},
    "cardboard": {"color": (0.62, 0.50, 0.34, 1.0), "rough": 0.95},
    "paper": {"color": (0.90, 0.88, 0.80, 1.0), "rough": 0.95},
    "red": {"color": (0.75, 0.10, 0.08, 1.0), "rough": 0.5},
    "black": {"color": (0.05, 0.05, 0.06, 1.0), "rough": 0.6},
    "led_g": {"color": (0.2, 1.0, 0.45, 1.0), "emit": True},
    "led_r": {"color": (1.0, 0.18, 0.12, 1.0), "emit": True},
    "led_off": {"color": (0.12, 0.13, 0.13, 1.0)},
    "crystal": {"color": (0.22, 0.27, 0.33, 1.0), "rough": 0.25, "metal": 0.2},
    "core": {"color": (0.45, 0.95, 1.0, 1.0), "emit": True},
    "yellow": {"color": (0.88, 0.70, 0.12, 1.0), "rough": 0.6},
}


def uvbox(ob, scale=0.5):
    me = ob.data
    bm = bmesh.new()
    bm.from_mesh(me)
    uv = bm.loops.layers.uv.verify()
    for f in bm.faces:
        n = f.normal
        ax = max(range(3), key=lambda i: abs(n[i]))
        for l in f.loops:
            co = l.vert.co
            if ax == 0:
                u, v = co.y, co.z
            elif ax == 1:
                u, v = co.x, co.z
            else:
                u, v = co.x, co.y
            l[uv].uv = (u * scale, v * scale)
    bm.to_mesh(me)
    bm.free()


def _obj(name, bm, m, loc, rot):
    me = bpy.data.meshes.new(name)
    bm.to_mesh(me)
    bm.free()
    ob = bpy.data.objects.new(name, me)
    bpy.context.scene.collection.objects.link(ob)
    me.materials.append(mat(m))
    ob.location = loc
    ob.rotation_euler = rot
    bpy.context.view_layer.update()
    # bake transform into the mesh (world coordinates) so joins/pivots are simple
    me.transform(ob.matrix_world)
    ob.matrix_world = Matrix.Identity(4)
    uvbox(ob)
    PARTS.append(ob)
    return ob


def box(name, size, loc, m, rot=(0, 0, 0), bevel=0.0):
    bm = bmesh.new()
    bmesh.ops.create_cube(bm, size=1.0)
    for v in bm.verts:
        v.co = Vector((v.co.x * size[0], v.co.y * size[1], v.co.z * size[2]))
    if bevel > 0:
        bmesh.ops.bevel(bm, geom=list(bm.edges), offset=bevel, segments=1, affect='EDGES', profile=0.5)
    return _obj(name, bm, m, loc, rot)


def cyl(name, r, h, loc, m, rot=(0, 0, 0), seg=16, r2=None):
    bm = bmesh.new()
    bmesh.ops.create_cone(bm, cap_ends=True, cap_tris=False, segments=seg, radius1=r, radius2=r if r2 is None else r2, depth=h)
    return _obj(name, bm, m, loc, rot)


def rod(name, a, b, r, m, seg=8):
    a, b = Vector(a), Vector(b)
    d = b - a
    q = d.to_track_quat('Z', 'Y')
    return cyl(name, r, d.length, tuple((a + b) / 2), m, rot=q.to_euler(), seg=seg)


def torus(name, R, r, loc, m, rot=(0, 0, 0), seg=32, seg2=8):
    bm = bmesh.new()
    verts = []
    for i in range(seg):
        a = 2 * math.pi * i / seg
        ring = []
        for j in range(seg2):
            b = 2 * math.pi * j / seg2
            x = (R + r * math.cos(b)) * math.cos(a)
            y = (R + r * math.cos(b)) * math.sin(a)
            z = r * math.sin(b)
            ring.append(bm.verts.new((x, y, z)))
        verts.append(ring)
    for i in range(seg):
        for j in range(seg2):
            bm.faces.new((verts[i][j], verts[(i + 1) % seg][j], verts[(i + 1) % seg][(j + 1) % seg2], verts[i][(j + 1) % seg2]))
    return _obj(name, bm, m, loc, rot)


def crystal(name, loc, m, h=0.22, r=0.07, rot=(0, 0, 0)):
    """Resonance stone: heptagonal bipyramid (longer top)."""
    bm = bmesh.new()
    ring = [bm.verts.new((r * math.cos(2 * math.pi * i / 7), r * math.sin(2 * math.pi * i / 7), 0)) for i in range(7)]
    top = bm.verts.new((0, 0, h * 0.62))
    bot = bm.verts.new((0, 0, -h * 0.38))
    for i in range(7):
        bm.faces.new((ring[i], ring[(i + 1) % 7], top))
        bm.faces.new((ring[(i + 1) % 7], ring[i], bot))
    return _obj(name, bm, m, loc, rot)


def node(name, objs=None, pivot=(0, 0, 0)):
    """Join objs (default: everything built since the last node) into one mesh node with origin at pivot."""
    objs = list(PARTS) if objs is None else objs
    for o in objs:
        if o in PARTS:
            PARTS.remove(o)
    if not objs:
        return None
    bpy.ops.object.select_all(action='DESELECT')
    for o in objs:
        o.select_set(True)
    bpy.context.view_layer.objects.active = objs[0]
    if len(objs) > 1:
        bpy.ops.object.join()
    ob = bpy.context.view_layer.objects.active
    ob.name = name
    ob.data.name = name + "_mesh"
    pv = Vector(pivot)
    ob.data.transform(Matrix.Translation(-pv))
    ob.location = pv
    return ob


def parent(child, par):
    mw = child.matrix_world.copy()
    child.parent = par
    child.matrix_world = mw


def export(name):
    for o in bpy.context.scene.objects:
        o.select_set(True)
    path = os.path.join(OUT, name + ".glb")
    bpy.ops.export_scene.gltf(filepath=path, export_format='GLB', export_yup=True, export_apply=False,
                              export_animations=False, export_extras=False, export_cameras=False, export_lights=False,
                              export_image_format='AUTO', use_selection=False)
    bpy.ops.wm.save_as_mainfile(filepath=os.path.join(BLEND, name + ".blend"), compress=True)
    tris = sum(len(o.data.polygons) for o in bpy.context.scene.objects if o.type == 'MESH')
    print("MODEL", name, os.path.getsize(path), "B", tris, "faces")


def screen_pair(tag, size, loc, rot):
    """Coplanar live/dead screen faces as separate nodes (renderer hides one by power state)."""
    a = box(tag + "_live", (size[0], 0.01, size[1]), loc, "screen", rot)
    b = box(tag + "_dead", (size[0], 0.01, size[1]), loc, "screen_dead", rot)
    return a, b


def monitor(tag, w, h, loc, yaw=0.0, tilt=0.0, live=None, dead=None):
    rot = (math.radians(tilt), 0, math.radians(yaw))
    R = Euler(rot).to_matrix()
    c = Vector(loc)
    box(tag + "_frame", (w + 0.06, 0.06, h + 0.06), tuple(c), "dark", rot, bevel=0.01)
    box(tag + "_back", (w * 0.5, 0.08, h * 0.5), tuple(c + R @ Vector((0, 0.07, 0))), "dark", rot)
    a, b = screen_pair(tag, (w, h), tuple(c + R @ Vector((0, -0.033, 0))), rot)
    live.append(a)
    dead.append(b)
    PARTS.remove(a)
    PARTS.remove(b)


def finish_screens(live, dead):
    if live:
        node("screens_live", live)
        node("screens_dead", dead)


# ====================================================================================== models
def m_portal_helm():
    live, dead = [], []
    box("plinth", (2.95, 1.95, 0.12), (0, 0.5, 0.06), "dark", bevel=0.02)
    for sx in (-1, 1):
        box("hz%d" % sx, (0.06, 1.9, 0.125), (sx * 1.44, 0.5, 0.065), "hazard")
    box("hzf", (2.9, 0.06, 0.125), (0, -0.44, 0.065), "hazard")
    box("desk", (2.8, 0.75, 0.82), (0, 0.05, 0.53), "white", bevel=0.03)
    box("deskband", (2.82, 0.77, 0.08), (0, 0.05, 0.36), "teal")
    box("kick", (2.7, 0.08, 0.2), (0, -0.33, 0.22), "dark")
    # slanted socket panel
    P = Vector((0, -0.18, 1.02))
    rot = (math.radians(-32), 0, 0)
    R = Euler(rot).to_matrix()
    box("panel", (2.75, 0.62, 0.09), tuple(P), "dark", rot, bevel=0.015)
    box("panelt", (2.78, 0.05, 0.1), tuple(P + R @ Vector((0, -0.32, 0))), "steel", rot)
    for i in range(5):
        x = -1.0 + 0.5 * i
        c = P + R @ Vector((x, 0.02, 0.05))
        torus("sock%d" % i, 0.11, 0.025, tuple(c), "steel", rot, seg=24, seg2=6)
        cyl("sockh%d" % i, 0.095, 0.02, tuple(c + R @ Vector((0, 0, -0.005))), "black", rot, seg=16)
        cyl("sockl%d" % i, 0.025, 0.02, tuple(P + R @ Vector((x, -0.2, 0.05))), "led_off", rot, seg=8)
        box("lbl%d" % i, (0.16, 0.05, 0.01), tuple(P + R @ Vector((x, 0.2, 0.05))), "paper", rot)
    # buttons row
    for k in range(10):
        box("btn%d" % k, (0.06, 0.06, 0.03), tuple(P + R @ Vector((-1.2 + k * 0.08 if k < 5 else 0.88 + (k - 5) * 0.08, -0.2, 0.055))),
            "red" if k % 3 == 0 else ("yellow" if k % 3 == 1 else "steel"), rot)
    # wheel column
    box("col", (0.45, 0.45, 1.55), (0, 0.85, 0.85), "white", bevel=0.03)
    box("colband", (0.47, 0.47, 0.1), (0, 0.85, 1.45), "teal")
    box("colhead", (0.55, 0.5, 0.35), (0, 0.78, 1.8), "dark", (math.radians(-20), 0, 0), bevel=0.03)
    for sx in (-1, 1):
        rod("cable%d" % sx, (sx * 0.15, 1.05, 0.12), (sx * 0.12, 1.08, 1.6), 0.035, "rubber")
    # side monitors + top monitor
    for sx in (-1, 1):
        rod("post%d" % sx, (sx * 1.18, 0.95, 0.12), (sx * 1.18, 0.95, 1.55), 0.05, "steel")
        monitor("mon%d" % sx, 0.75, 0.5, (sx * 1.18, 0.88, 1.85), yaw=-sx * 25, tilt=-8, live=live, dead=dead)
        box("ped%d" % sx, (0.3, 0.3, 0.1), (sx * 1.18, 0.95, 0.17), "dark")
    rod("toppost", (0, 1.18, 0.12), (0, 1.18, 2.6), 0.045, "steel")
    monitor("montop", 0.9, 0.4, (0, 1.1, 2.75), tilt=-15, live=live, dead=dead)
    node("body")
    # the giant wheel (node 'wheel', pivot at the hub, axis = local Y tilted)
    hub = Vector((0, 0.62, 1.95))
    wrot = (math.radians(70), 0, 0)          # wheel plane faces the operator, tilted back 20 deg
    WR = Euler(wrot).to_matrix()
    torus("rim", 0.78, 0.055, tuple(hub), "dark", wrot, seg=40, seg2=8)
    torus("rim2", 0.62, 0.02, tuple(hub + WR @ Vector((0, 0, 0.0))), "steel", wrot, seg=40, seg2=6)
    cyl("hub", 0.13, 0.16, tuple(hub), "steel", wrot, seg=18)
    cyl("hubc", 0.07, 0.18, tuple(hub + WR @ Vector((0, 0, 0.02))), "teal", wrot, seg=12)
    for k in range(8):
        a = 2 * math.pi * k / 8
        d = WR @ Vector((math.cos(a), math.sin(a), 0))
        rod("spoke%d" % k, tuple(hub + d * 0.1), tuple(hub + d * 0.76), 0.03, "steel")
        rod("grip%d" % k, tuple(hub + d * 0.8), tuple(hub + d * 1.0), 0.04, "rubber")
        cyl("gripk%d" % k, 0.05, 0.05, tuple(hub + d * 1.02), "dark", d.to_track_quat('Z', 'Y').to_euler(), seg=10)
    node("wheel", pivot=tuple(hub))
    # resonance stones in the sockets
    for i in range(5):
        x = -1.0 + 0.5 * i
        c = P + R @ Vector((x, 0.02, 0.13))
        crystal("stone_%d" % i, tuple(c), "crystal", rot=rot)
        cyl("stone_core_%d" % i, 0.02, 0.18, tuple(c + R @ Vector((0, 0, 0.02))), "core", rot, seg=6)
        node("stone_%d" % i, pivot=tuple(c))
        cyl("sockon%d" % i, 0.026, 0.022, tuple(P + R @ Vector((x, -0.2, 0.051))), "led_g", rot, seg=8)
        node("led_%d" % i)
    finish_screens(live, dead)
    export("portal_helm")


def m_portal_lever():
    box("base", (0.8, 0.8, 0.1), (0, 0, 0.05), "dark", bevel=0.02)
    box("basehz", (0.82, 0.82, 0.04), (0, 0, 0.12), "hazard")
    cyl("ped", 0.26, 0.85, (0, 0, 0.55), "white", seg=8)
    cyl("pedb", 0.27, 0.08, (0, 0, 0.75), "teal", seg=8)
    box("house", (0.5, 0.42, 0.26), (0, 0, 1.08), "dark", bevel=0.03)
    box("slot", (0.08, 0.36, 0.02), (0, 0, 1.215), "black")
    for sx in (-1, 1):
        cyl("boss%d" % sx, 0.09, 0.06, (sx * 0.22, 0, 1.12), "steel", (0, math.radians(90), 0), seg=14)
    box("plate", (0.3, 0.01, 0.12), (0, -0.215, 1.05), "paper")
    cyl("ind_off", 0.03, 0.03, (0.17, -0.215, 1.15), "led_off", (math.radians(90), 0, 0), seg=10)
    node("body")
    cyl("ind_on", 0.031, 0.031, (0.17, -0.216, 1.15), "led_r", (math.radians(90), 0, 0), seg=10)
    node("lamp_on")
    # handle: pivot at the bosses, rest position tilted 35 deg towards the front (-Y) = "off"
    pv = Vector((0, 0, 1.12))
    cyl("axle", 0.035, 0.5, tuple(pv), "steel", (0, math.radians(90), 0), seg=10)
    d = Vector((0, -math.sin(math.radians(35)), math.cos(math.radians(35))))
    rod("arm", tuple(pv), tuple(pv + d * 0.62), 0.032, "steel")
    rod("grip", tuple(pv + d * 0.5), tuple(pv + d * 0.68), 0.045, "rubber")
    bm_sphere = cyl("knob", 0.07, 0.1, tuple(pv + d * 0.72), "red", d.to_track_quat('Z', 'Y').to_euler(), seg=14, r2=0.05)
    node("handle", pivot=tuple(pv))
    export("portal_lever")


def m_control_console():
    live, dead = [], []
    c = 0.5
    box("desk", (1.9, 0.75, 0.8), (c, 0.05, 0.4), "white", bevel=0.03)
    box("band", (1.92, 0.77, 0.07), (c, 0.05, 0.62), "teal")
    rot = (math.radians(-25), 0, 0)
    R = Euler(rot).to_matrix()
    P = Vector((c, -0.12, 0.86))
    box("kb", (1.85, 0.5, 0.06), tuple(P), "dark", rot, bevel=0.01)
    for k in range(24):
        box("key%d" % k, (0.05, 0.05, 0.02), tuple(P + R @ Vector((-0.6 + (k % 12) * 0.06, -0.08 + (k // 12) * 0.07, 0.035))), "black", rot)
    for k in range(4):
        cyl("knob%d" % k, 0.035, 0.04, tuple(P + R @ Vector((0.5 + (k % 2) * 0.12, -0.08 + (k // 2) * 0.12, 0.04))), "steel", rot, seg=10)
    box("deskb", (1.9, 0.25, 0.25), (c, 0.32, 0.92), "white", bevel=0.02)
    for k, x in enumerate((c - 0.47, c + 0.47)):
        monitor("m%d" % k, 0.8, 0.5, (x, 0.3, 1.38), yaw=(1 if k == 0 else -1) * 8, tilt=-6, live=live, dead=dead)
        box("stand%d" % k, (0.08, 0.08, 0.25), (x, 0.36, 1.1), "dark")
    box("mug", (0.08, 0.08, 0.1), (c + 0.82, -0.05, 0.85), "red")
    box("papers", (0.25, 0.32, 0.01), (c - 0.75, -0.02, 0.81), "paper", (0, 0, 0.2))
    node("body")
    finish_screens(live, dead)
    export("control_console")


def m_lathe():
    c = 0.5
    for k, x in enumerate((c - 0.75, c + 0.7)):
        box("leg%d" % k, (0.42, 0.55, 0.62), (x, 0, 0.31), "teal", bevel=0.02)
    box("tray", (1.9, 0.62, 0.06), (c, 0, 0.64), "dark")
    box("bed", (1.8, 0.32, 0.18), (c, 0.04, 0.76), "steel", bevel=0.01)
    box("bedw1", (1.8, 0.04, 0.03), (c, -0.08, 0.865), "steel")
    box("bedw2", (1.8, 0.04, 0.03), (c, 0.16, 0.865), "steel")
    box("head", (0.5, 0.45, 0.45), (c - 0.62, 0.04, 1.08), "teal", bevel=0.03)
    for k in range(3):
        cyl("hknob%d" % k, 0.03, 0.05, (c - 0.75 + k * 0.13, -0.2, 1.15), "black", (math.radians(90), 0, 0), seg=8)
    cyl("chuck", 0.14, 0.12, (c - 0.31, 0.04, 1.05), "steel", (0, math.radians(90), 0), seg=18)
    for k in range(3):
        a = 2 * math.pi * k / 3
        box("jaw%d" % k, (0.04, 0.04, 0.08), (c - 0.24, 0.04 + 0.09 * math.cos(a), 1.05 + 0.09 * math.sin(a)), "dark", (a, 0, 0))
    box("tail", (0.25, 0.3, 0.3), (c + 0.68, 0.04, 1.02), "teal", bevel=0.02)
    cyl("quill", 0.04, 0.2, (c + 0.48, 0.04, 1.05), "steel", (0, math.radians(90), 0), seg=10)
    torus("tailwheel", 0.08, 0.012, (c + 0.83, 0.04, 1.05), "steel", (0, math.radians(90), 0), seg=16, seg2=5)
    box("carr", (0.32, 0.4, 0.1), (c + 0.1, 0.0, 0.92), "teal")
    box("apron", (0.32, 0.06, 0.25), (c + 0.1, -0.2, 0.8), "teal")
    box("post", (0.08, 0.08, 0.1), (c + 0.1, -0.02, 1.02), "dark")
    torus("apronwheel", 0.07, 0.012, (c + 0.1, -0.25, 0.8), "steel", (math.radians(90), 0, 0), seg=16, seg2=5)
    rod("leadscrew", (c - 0.85, -0.17, 0.7), (c + 0.85, -0.17, 0.7), 0.012, "steel")
    rod("coolant", (c - 0.4, 0.2, 0.86), (c - 0.2, 0.05, 1.25), 0.012, "copper")
    box("lamp", (0.06, 0.06, 0.3), (c - 0.8, 0.22, 1.4), "dark")
    box("chips", (0.4, 0.3, 0.02), (c, 0, 0.675), "rust")
    node("body")
    export("lathe")


def m_milling_machine():
    box("base", (0.8, 0.85, 0.12), (0, 0.02, 0.06), "dark")
    box("column", (0.45, 0.42, 1.55), (0, 0.18, 0.88), "teal", bevel=0.02)
    box("knee", (0.5, 0.45, 0.35), (0, -0.14, 0.62), "teal", bevel=0.02)
    box("saddle", (0.45, 0.35, 0.08), (0, -0.16, 0.84), "steel")
    box("table", (0.92, 0.28, 0.07), (0, -0.16, 0.92), "steel")
    for k in range(3):
        box("tslot%d" % k, (0.92, 0.02, 0.01), (0, -0.24 + 0.08 * k, 0.957), "black")
    box("vice", (0.18, 0.14, 0.08), (0.05, -0.16, 0.99), "dark")
    box("ram", (0.3, 0.6, 0.22), (0, -0.1, 1.55), "teal", bevel=0.02)
    box("headb", (0.24, 0.24, 0.34), (0, -0.33, 1.35), "teal", bevel=0.02)
    cyl("spindle", 0.04, 0.16, (0, -0.33, 1.1), "steel", seg=10)
    cyl("cutter", 0.035, 0.06, (0, -0.33, 1.0), "dark", seg=8)
    torus("hw1", 0.08, 0.012, (0.32, -0.16, 0.92), "steel", (0, math.radians(90), 0), seg=16, seg2=5)
    torus("hw2", 0.08, 0.012, (0, -0.4, 0.62), "steel", (math.radians(90), 0, 0), seg=16, seg2=5)
    box("ctrl", (0.2, 0.08, 0.3), (0.3, 0.12, 1.25), "dark")
    cyl("estop", 0.03, 0.03, (0.3, 0.07, 1.3), "red", (math.radians(90), 0, 0), seg=10)
    node("body")
    export("milling_machine")


def m_specimen_tank():
    cyl("base", 0.46, 0.32, (0, 0, 0.16), "dark", seg=20)
    cyl("baseband", 0.47, 0.05, (0, 0, 0.28), "teal", seg=20)
    for k in range(6):
        a = 2 * math.pi * k / 6
        cyl("bl%d" % k, 0.02, 0.01, (0.465 * math.cos(a), 0.465 * math.sin(a), 0.16), "led_off",
            (math.radians(90), 0, a + math.pi / 2), seg=6)
    cyl("cap", 0.46, 0.3, (0, 0, 2.7), "dark", seg=20)
    cyl("capband", 0.47, 0.05, (0, 0, 2.58), "teal", seg=20)
    for k in range(3):
        a = 2 * math.pi * k / 3 + 0.4
        rod("pipe%d" % k, (0.25 * math.cos(a), 0.25 * math.sin(a), 2.85), (0.25 * math.cos(a), 0.25 * math.sin(a), 3.0), 0.04, "steel")
    for k in range(4):
        a = 2 * math.pi * k / 4 + math.pi / 4
        rod("rib%d" % k, (0.43 * math.cos(a), 0.43 * math.sin(a), 0.32), (0.43 * math.cos(a), 0.43 * math.sin(a), 2.55), 0.022, "steel")
    box("plate", (0.22, 0.01, 0.1), (0, -0.465, 0.16), "paper")
    # dead plant: twisted stalk, drooping leaves, roots on the bottom
    pts = [(0, 0, 0.32), (0.05, 0.02, 0.8), (-0.04, 0.05, 1.3), (0.06, -0.03, 1.75), (0.0, 0.0, 2.05)]
    for k in range(len(pts) - 1):
        rod("stalk%d" % k, pts[k], pts[k + 1], 0.035 - k * 0.006, "plant")
    for k in range(9):
        a = k * 2.4
        z = 0.6 + k * 0.17
        L = 0.25 - k * 0.015
        c = Vector((0.03 * math.sin(k), 0.03 * math.cos(k), z))
        d = Vector((math.cos(a), math.sin(a), -0.6)).normalized()
        box("leaf%d" % k, (0.09, L, 0.008), tuple(c + d * L * 0.5), "plant", d.to_track_quat('Y', 'Z').to_euler())
    for k in range(5):
        a = k * 1.3
        rod("root%d" % k, (0, 0, 0.34), (0.3 * math.cos(a), 0.3 * math.sin(a), 0.33), 0.012, "plant")
    node("body")
    cyl("liquid", 0.385, 0.75, (0, 0, 0.32 + 0.375), "liquid", seg=20)
    node("liquid")
    cyl("glass", 0.41, 2.26, (0, 0, 0.32 + 1.13), "glass", seg=24)
    node("glass")
    # broken variant: jagged shards still standing on the base ring
    for k in range(9):
        a = 2 * math.pi * k / 9
        h = 0.15 + 0.35 * ((k * 7) % 5) / 4
        box("shard%d" % k, (0.22, 0.012, h), (0.405 * math.cos(a), 0.405 * math.sin(a), 0.32 + h / 2), "glass",
            (0.15 * ((k % 3) - 1), 0, a + math.pi / 2))
    for k in range(5):
        a = 2 * math.pi * k / 5 + 0.3
        box("shardt%d" % k, (0.18, 0.012, 0.12), (0.405 * math.cos(a), 0.405 * math.sin(a), 2.5), "glass", (0, 0, a + math.pi / 2))
    node("glass_broken")
    export("specimen_tank")


def m_electrical_cabinet():
    box("body", (0.92, 0.6, 1.85), (0, 0.15, 0.95), "white", bevel=0.02)
    box("plinth", (0.94, 0.62, 0.1), (0, 0.15, 0.05), "dark")
    box("doorl", (0.44, 0.02, 1.7), (-0.225, -0.155, 0.97), "white", bevel=0.005)
    box("doorr", (0.44, 0.02, 1.7), (0.225, -0.155, 0.97), "white", bevel=0.005)
    for sx in (-1, 1):
        box("handle%d" % sx, (0.03, 0.04, 0.18), (sx * 0.04, -0.175, 1.05), "dark")
        for k in range(6):
            box("vent%d_%d" % (sx, k), (0.3, 0.015, 0.025), (sx * 0.225, -0.168, 0.25 + k * 0.05), "dark")
    box("warn", (0.18, 0.01, 0.16), (0.225, -0.168, 1.45), "hazard", (0, 0, 0))
    cyl("gauge", 0.09, 0.03, (-0.225, -0.175, 1.5), "dark", (math.radians(90), 0, 0), seg=16)
    cyl("gaugef", 0.075, 0.01, (-0.225, -0.19, 1.5), "paper", (math.radians(90), 0, 0), seg=16)
    box("needle", (0.004, 0.003, 0.07), (-0.215, -0.196, 1.52), "red", (0, math.radians(30), 0))
    for k in range(3):
        rod("conduit%d" % k, (-0.3 + 0.3 * k, 0.2, 1.87), (-0.3 + 0.3 * k, 0.2, 2.0), 0.04, "steel")
    node("body")
    export("electrical_cabinet")


def m_generator():
    box("skid", (2.9, 1.8, 0.14), (0, 0.5, 0.07), "dark", bevel=0.02)
    for sx in (-1, 1):
        box("rail%d" % sx, (2.9, 0.12, 0.08), (0, 0.5 + sx * 0.85, 0.18), "hazard")
    # alternator drum + engine block
    cyl("drum", 0.68, 1.3, (-0.6, 0.5, 0.9), "teal", (0, math.radians(90), 0), seg=28)
    for k in range(9):
        cyl("fin%d" % k, 0.72, 0.03, (-1.15 + k * 0.135, 0.5, 0.9), "steel", (0, math.radians(90), 0), seg=28)
    cyl("drumend", 0.5, 0.06, (-1.28, 0.5, 0.9), "dark", (0, math.radians(90), 0), seg=24)
    box("engine", (1.2, 1.2, 1.1), (0.75, 0.5, 0.75), "teal", bevel=0.04)
    box("enginetop", (1.0, 0.9, 0.2), (0.75, 0.5, 1.4), "dark", bevel=0.02)
    for k in range(6):
        cyl("valve%d" % k, 0.07, 0.12, (0.45 + (k % 3) * 0.3, 0.3 + (k // 3) * 0.4, 1.56), "steel", seg=10)
    rod("exh1", (1.2, 0.95, 1.2), (1.3, 1.2, 1.4), 0.08, "rust")
    rod("exh2", (1.3, 1.2, 1.4), (1.3, 1.2, 1.95), 0.08, "rust")
    cyl("exhcap", 0.11, 0.06, (1.3, 1.2, 1.97), "dark", seg=12)
    box("panel", (0.5, 0.15, 0.6), (0.75, -0.22, 0.95), "dark", bevel=0.02)
    for k in range(3):
        cyl("dial%d" % k, 0.06, 0.02, (0.6 + k * 0.15, -0.3, 1.1), "paper", (math.radians(90), 0, 0), seg=12)
    cyl("estop", 0.045, 0.04, (0.75, -0.31, 0.85), "red", (math.radians(90), 0, 0), seg=12)
    # copper bus bars to the floor
    for k in range(3):
        rod("bus%d" % k, (-0.6, 1.05 + 0.0, 1.25 - k * 0.2), (-0.6, 1.35, 1.25 - k * 0.2), 0.03, "copper")
    node("body")
    export("generator")


def m_transformer():
    c = 0.5
    box("tank", (1.3, 0.75, 1.2), (c, 0.05, 0.7), "teal", bevel=0.03)
    box("base", (1.5, 0.85, 0.1), (c, 0.05, 0.05), "dark")
    for sx in (-1, 1):
        for k in range(7):
            box("radf%d_%d" % (sx, k), (0.18, 0.025, 0.9), (c + sx * 0.75, -0.25 + k * 0.083, 0.7), "steel")
    for k in range(3):
        x = c - 0.4 + k * 0.4
        for j in range(4):
            cyl("ins%d_%d" % (k, j), 0.09 - j * 0.004, 0.05, (x, 0.05, 1.36 + j * 0.07), "white", seg=12)
        cyl("insc%d" % k, 0.035, 0.12, (x, 0.05, 1.62), "copper", seg=8)
    box("sign", (0.3, 0.01, 0.25), (c, -0.33, 0.85), "hazard")
    box("plate", (0.25, 0.01, 0.12), (c + 0.4, -0.33, 0.55), "paper")
    node("body")
    export("transformer")


def m_server_rack():
    box("rack", (0.8, 0.9, 1.9), (0, 0.1, 0.95), "black", bevel=0.015)
    box("frame", (0.82, 0.04, 1.92), (0, -0.35, 0.96), "dark")
    leds_g, leds_r = [], []
    for k in range(14):
        z = 0.2 + k * 0.115
        box("unit%d" % k, (0.7, 0.02, 0.1), (0, -0.37, z), "dark")
        for j in range(4):
            box("vent%d_%d" % (k, j), (0.12, 0.005, 0.015), (-0.2 + j * 0.13, -0.382, z + 0.02), "black")
        box("ledoff%d" % k, (0.02, 0.005, 0.02), (0.3, -0.382, z), "led_off")
    node("body")
    for k in range(14):
        z = 0.2 + k * 0.115
        box("led%d" % k, (0.021, 0.006, 0.021), (0.3, -0.383, z), "led_g" if k % 4 else "led_r")
    node("leds_on")
    export("server_rack")


def m_security_desk():
    live, dead = [], []
    box("top", (2.9, 0.85, 0.06), (0, 0.05, 0.78), "white", bevel=0.01)
    box("front", (2.9, 0.06, 0.75), (0, -0.33, 0.38), "white")
    box("frontband", (2.92, 0.07, 0.08), (0, -0.33, 0.65), "teal")
    for sx in (-1, 1):
        box("side%d" % sx, (0.06, 0.8, 0.75), (sx * 1.42, 0.05, 0.38), "white")
    box("riser", (2.6, 0.3, 0.25), (0, 0.32, 0.93), "dark")
    for k in range(4):
        x = -1.05 + k * 0.7
        monitor("m%d" % k, 0.62, 0.4, (x, 0.3, 1.32), yaw=(-1.5 + k) * -6, tilt=-5, live=live, dead=dead)
    for k in range(2):
        monitor("u%d" % k, 0.62, 0.4, (-0.35 + k * 0.7, 0.36, 1.78), tilt=-10, live=live, dead=dead)
    box("kb", (0.5, 0.18, 0.02), (0, -0.12, 0.82), "black")
    box("phone", (0.2, 0.22, 0.06), (0.9, -0.1, 0.84), "black", (0, 0, 0.3))
    box("log", (0.3, 0.22, 0.02), (-0.8, -0.1, 0.82), "paper", (0, 0, -0.2))
    box("radio", (0.12, 0.08, 0.2), (1.25, 0.1, 0.91), "dark")
    rod("antenna", (1.25, 0.12, 1.0), (1.25, 0.12, 1.25), 0.008, "black")
    node("body")
    finish_screens(live, dead)
    export("security_desk")


def m_crate_wood():
    s = 0.92
    box("core", (s - 0.04, s - 0.04, s - 0.04), (0, 0, s / 2), "wood")
    for sx in (-1, 1):
        for sy in (-1, 1):
            box("post%d%d" % (sx, sy), (0.1, 0.1, s), (sx * (s / 2 - 0.05), sy * (s / 2 - 0.05), s / 2), "wood", bevel=0.01)
    for z in (0.05, s - 0.05):
        for sx in (-1, 1):
            box("bx%d_%d" % (sx, int(z * 100)), (0.1, s, 0.1), (sx * (s / 2 - 0.05), 0, z), "wood")
            box("by%d_%d" % (sx, int(z * 100)), (s, 0.1, 0.1), (0, sx * (s / 2 - 0.05), z), "wood")
    for sy in (-1, 1):
        box("diag%d" % sy, (0.08, 0.02, s * 1.2), (0, sy * (s / 2 - 0.0), s / 2), "wood", (0, math.radians(45 * sy), 0))
    for sx in (-1, 1):
        for sy in (-1, 1):
            for z in (0.0, s):
                box("corner%d%d%d" % (sx, sy, int(z)), (0.12, 0.12, 0.06), (sx * (s / 2 - 0.04), sy * (s / 2 - 0.04), z + (0.03 if z == 0 else -0.03)), "steel")
    box("stencil", (0.36, 0.005, 0.14), (0, -s / 2 - 0.012, s * 0.7), "paper")
    node("body")
    export("crate_wood")


def m_crate_metal():
    s = 0.9
    box("core", (s, s * 0.8, s * 0.7), (0, 0, s * 0.35), "olive", bevel=0.02)
    box("lid", (s + 0.03, s * 0.8 + 0.03, 0.08), (0, 0, s * 0.7), "olive", bevel=0.015)
    for sx in (-1, 1):
        box("rib%d" % sx, (0.04, s * 0.82, s * 0.7), (sx * 0.28, 0, s * 0.35), "olive")
        box("latch%d" % sx, (0.1, 0.02, 0.12), (sx * 0.28, -s * 0.4 - 0.015, s * 0.62), "steel")
        rod("handle%d" % sx, (sx * (s / 2 + 0.02), -0.12, s * 0.45), (sx * (s / 2 + 0.02), 0.12, s * 0.45), 0.018, "dark")
    box("label", (0.3, 0.005, 0.12), (0, -s * 0.4 - 0.012, s * 0.4), "paper")
    node("body")
    export("crate_metal")


def m_lab_door():
    # frame: posts + header (doorway 2 wide, 3 high), leaves slide sideways (nodes door_l / door_r)
    c = 0.5
    for sx in (-1, 1):
        box("post%d" % sx, (0.14, 0.4, 3.0), (c + sx * 0.93, 0, 1.5), "dark", bevel=0.01)
        box("posthz%d" % sx, (0.02, 0.41, 0.4), (c + sx * 0.93 - sx * 0.07, 0, 0.2), "hazard")
    box("header", (2.0, 0.4, 0.36), (c, 0, 2.82), "dark", bevel=0.01)
    box("headerband", (1.7, 0.01, 0.1), (c, -0.205, 2.82), "hazard")
    box("sill", (1.72, 0.4, 0.04), (c, 0, 0.02), "steel")
    box("lamp_off", (0.16, 0.06, 0.06), (c, -0.22, 2.72), "led_off")
    box("sign", (0.4, 0.005, 0.12), (c + 0.6, -0.205, 2.82), "paper")
    node("body")
    box("lamp_on", (0.161, 0.061, 0.061), (c, -0.221, 2.72), "led_g")
    node("lamp_on")
    for k, sx in enumerate((-1, 1)):
        x = c + sx * 0.43
        box("leaf%d" % k, (0.86, 0.1, 2.62), (x, 0, 1.33), "white", bevel=0.01)
        box("band%d" % k, (0.86, 0.105, 0.1), (x, 0, 1.0), "teal")
        box("win%d" % k, (0.18, 0.11, 0.9), (x - sx * 0.2, 0, 1.85), "glass")
        box("winf%d" % k, (0.22, 0.104, 0.94), (x - sx * 0.2, 0, 1.85), "dark")
        box("edge%d" % k, (0.03, 0.12, 2.62), (c + sx * 0.015, 0, 1.33), "rubber")
        box("pull%d" % k, (0.03, 0.14, 0.3), (c + sx * 0.12, 0, 1.2), "steel")
        node("door_" + ("l" if sx < 0 else "r"), pivot=(x, 0, 1.33))
    export("lab_door")


def m_lab_table():
    c = 0.5
    box("top", (1.9, 0.8, 0.05), (c, 0, 0.85), "white", bevel=0.01)
    box("cab", (0.5, 0.7, 0.8), (c - 0.65, 0, 0.42), "white", bevel=0.01)
    for k in range(3):
        box("drawer%d" % k, (0.44, 0.01, 0.22), (c - 0.65, -0.355, 0.2 + k * 0.25), "white")
        box("dh%d" % k, (0.12, 0.02, 0.02), (c - 0.65, -0.365, 0.25 + k * 0.25), "steel")
    for sx in (-1, 1):
        box("leg%d" % sx, (0.05, 0.05, 0.82), (c + 0.85, sx * 0.33, 0.41), "steel")
    rod("bar", (c + 0.85, -0.33, 0.15), (c + 0.85, 0.33, 0.15), 0.02, "steel")
    # microscope
    box("mbase", (0.18, 0.24, 0.04), (c - 0.5, 0.05, 0.895), "dark")
    rod("marm", (c - 0.5, 0.14, 0.9), (c - 0.5, 0.12, 1.2), 0.025, "dark")
    rod("mtube", (c - 0.5, 0.1, 1.2), (c - 0.5, -0.02, 1.08), 0.03, "white")
    box("mstage", (0.14, 0.12, 0.015), (c - 0.5, 0.0, 1.0), "dark")
    # beakers + flasks + rack
    for k in range(3):
        cyl("beaker%d" % k, 0.05, 0.13, (c - 0.05 + k * 0.13, -0.1, 0.94), "glass", seg=12)
        cyl("beakerl%d" % k, 0.045, 0.05, (c - 0.05 + k * 0.13, -0.1, 0.9), "liquid", seg=12)
    cyl("flask", 0.08, 0.14, (c + 0.45, 0.1, 0.945), "glass", seg=14, r2=0.03)
    cyl("flaskn", 0.025, 0.1, (c + 0.45, 0.1, 1.06), "glass", seg=10)
    box("rack", (0.3, 0.1, 0.06), (c + 0.15, 0.22, 0.905), "steel")
    for k in range(4):
        cyl("tube%d" % k, 0.015, 0.14, (c + 0.04 + k * 0.07, 0.22, 0.97), "glass", seg=8)
    box("notes", (0.22, 0.3, 0.008), (c + 0.68, -0.1, 0.879), "paper", (0, 0, 0.3))
    box("notes2", (0.22, 0.3, 0.008), (c + 0.72, -0.05, 0.886), "paper", (0, 0, -0.1))
    node("body")
    export("lab_table")


def m_shelf():
    c = 0.5
    for sx in (-1, 1):
        for sy in (-1, 1):
            box("up%d%d" % (sx, sy), (0.05, 0.05, 2.9), (c + sx * 0.93, sy * 0.38, 1.45), "hazard" if False else "dark")
    for k in range(4):
        z = 0.12 + k * 0.85
        box("shelf%d" % k, (1.9, 0.8, 0.04), (c, 0, z), "steel")
        box("lip%d" % k, (1.9, 0.02, 0.08), (c, -0.4, z), "yellow")
    rnd = [0.31, 0.77, 0.12, 0.58, 0.93, 0.44, 0.66, 0.21, 0.85, 0.05, 0.5, 0.38]
    n = 0
    for k in range(3):
        z0 = 0.14 + k * 0.85
        x = c - 0.85
        while x < c + 0.8:
            w = 0.3 + 0.25 * rnd[n % 12]
            h = 0.25 + 0.35 * rnd[(n + 3) % 12]
            if rnd[(n + 5) % 12] > 0.15:
                box("bx%d" % n, (w * 0.95, 0.6, h), (x + w / 2, 0.02, z0 + h / 2), "cardboard" if n % 3 else "olive", (0, 0, 0.05 * (rnd[(n + 7) % 12] - 0.5)))
                box("bxt%d" % n, (w * 0.3, 0.005, 0.06), (x + w / 2, -0.285, z0 + h * 0.6), "paper")
            x += w + 0.04
            n += 1
    node("body")
    export("shelf")


def m_office_chair():
    for k in range(5):
        a = 2 * math.pi * k / 5
        rod("leg%d" % k, (0, 0, 0.1), (0.3 * math.cos(a), 0.3 * math.sin(a), 0.07), 0.025, "dark")
        cyl("wheel%d" % k, 0.035, 0.03, (0.3 * math.cos(a), 0.3 * math.sin(a), 0.035), "black", (0, math.radians(90), a), seg=8)
    cyl("gas", 0.03, 0.35, (0, 0, 0.28), "steel", seg=10)
    box("seat", (0.48, 0.46, 0.08), (0, 0, 0.5), "black", bevel=0.03)
    rod("back", (0, 0.2, 0.5), (0, 0.24, 0.75), 0.025, "dark")
    box("backrest", (0.44, 0.07, 0.42), (0, 0.25, 0.92), "black", (math.radians(-8), 0, 0), bevel=0.03)
    for sx in (-1, 1):
        rod("arm%d" % sx, (sx * 0.24, 0.05, 0.5), (sx * 0.24, 0.05, 0.68), 0.015, "dark")
        box("armp%d" % sx, (0.06, 0.25, 0.03), (sx * 0.24, 0.02, 0.69), "black")
    node("body")
    export("office_chair")


def m_locker():
    box("body", (0.9, 0.55, 1.95), (0, 0.18, 0.975), "teal", bevel=0.01)
    for sx in (-1, 1):
        box("door%d" % sx, (0.43, 0.02, 1.8), (sx * 0.22, -0.1, 1.0), "teal", bevel=0.004)
        for k in range(5):
            box("v%d_%d" % (sx, k), (0.25, 0.012, 0.02), (sx * 0.22, -0.112, 1.65 + k * 0.04), "dark")
        box("h%d" % sx, (0.02, 0.03, 0.12), (sx * 0.04, -0.12, 1.0), "steel")
        box("tag%d" % sx, (0.12, 0.005, 0.06), (sx * 0.22, -0.112, 1.4), "paper")
    node("body")
    export("locker")


ALL = {k[2:]: v for k, v in list(globals().items()) if k.startswith("m_") and callable(v)}
for name, fn in ALL.items():
    if ONLY and name not in ONLY:
        continue
    reset()
    fn()
print("MODELS_DONE")
