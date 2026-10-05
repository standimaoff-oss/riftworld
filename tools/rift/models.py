# Riftworld block-entity models (Blender 5.x, run headless with --factory-startup).
#   blender -b --factory-startup --python tools/rift/models.py [-- name1,name2]
# Units: 1 Blender unit = 1 block. Master block centre at (0,0), floor z = 0. Front of every model faces -Y
# (glTF +Z = Minecraft south = towards the player for FACING=south); +X = the viewer's right.
# Animated parts are separate nodes whose origin is the pivot (Pose.rotate / translate in the renderer).
import bpy, bmesh, math, os, sys, random
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
        if spec.get("mask"):
            mn = nt.nodes.new("ShaderNodeMath")
            mn.operation = 'GREATER_THAN'
            mn.inputs[1].default_value = 0.5
            nt.links.new(tn.outputs["Alpha"], mn.inputs[0])
            nt.links.new(mn.outputs[0], bsdf.inputs["Alpha"])
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
    "e_wood": {"tex": "m_e_wood", "rough": 0.85},
    "e_wood_dark": {"tex": "m_e_wood_dark", "rough": 0.85},
    "e_sizo": {"tex": "m_e_sizo", "rough": 0.85},
    "e_bark": {"tex": "m_e_bark", "rough": 0.95},
    "e_stone": {"tex": "m_e_stone", "rough": 0.9},
    "e_rock": {"tex": "m_e_rock", "rough": 0.9},
    "e_iron": {"tex": "m_e_iron", "metal": 0.6, "rough": 0.55},
    "e_canvas": {"tex": "m_e_canvas", "rough": 0.95},
    "e_stripe": {"tex": "m_e_stripe", "rough": 0.9},
    "e_stripe_blue": {"tex": "m_e_stripe_blue", "rough": 0.9},
    "e_parch": {"tex": "m_e_parchment", "rough": 0.95},
    "e_hide": {"tex": "m_e_hide", "rough": 0.9},
    "e_cloth": {"tex": "m_e_cloth", "rough": 0.9},
    "e_embers": {"tex": "m_e_embers", "emit": True},
    "e_fire": {"tex": "m_e_fire", "emit": True},
    "e_lamp": {"tex": "m_e_lampglow", "emit": True},
    "e_rug": {"tex": "m_e_rug", "rough": 0.95},
    "e_fish": {"tex": "m_e_fish", "rough": 0.4},
    "e_fish2": {"tex": "m_e_fish2", "rough": 0.4},
    "e_fur": {"tex": "m_e_fur", "rough": 1.0},
    "e_gold": {"tex": "m_e_gold", "metal": 0.8, "rough": 0.35},
    "e_foliage": {"tex": "m_e_foliage", "rough": 0.9, "mask": True},
    "e_flowers": {"tex": "m_e_flowers", "rough": 0.9, "mask": True},
    "e_produce": {"tex": "m_e_produce", "rough": 0.7},
    "e_rope": {"color": (0.78, 0.73, 0.62, 1.0), "rough": 0.95},
    "e_bone": {"color": (0.88, 0.84, 0.74, 1.0), "rough": 0.8},
    "e_water": {"color": (0.64, 0.83, 0.91, 0.8), "rough": 0.05},
    "e_ice": {"color": (0.82, 0.93, 0.97, 1.0), "rough": 0.2},
    "e_candle": {"color": (0.95, 0.90, 0.78, 1.0), "rough": 0.6},
    "e_flame": {"color": (1.0, 0.72, 0.30, 1.0), "emit": True},
    "e_glow": {"color": (0.45, 1.0, 0.92, 1.0), "emit": True},
    "e_coal": {"color": (0.06, 0.05, 0.05, 1.0), "rough": 0.9},
    "e_cork": {"color": (0.78, 0.62, 0.40, 1.0), "rough": 0.9},
    "e_red": {"color": (0.42, 0.37, 0.33, 1.0), "rough": 0.6},
    "e_leaf_cyan": {"color": (0.66, 0.67, 0.56, 1.0), "rough": 0.9},
    "roof_r": {"tex": "m_e_roofred", "rough": 0.8},
    "e_paint_islands": {"tex": "m_paint_islands", "rough": 0.9},
    "e_paint_portrait": {"tex": "m_paint_portrait", "rough": 0.9},
    "e_paint_eye": {"tex": "m_paint_eye", "rough": 0.9},
    "e_ceramic": {"tex": "m_e_ceramic", "rough": 0.6},
    "e_plate": {"tex": "m_e_plate", "rough": 0.4},
    "e_books": {"tex": "m_e_books", "rough": 0.9},
    "e_logend": {"tex": "m_e_logend", "rough": 0.9},
    "wanted1": {"tex": "m_wanted1", "rough": 0.95},
    "wanted2": {"tex": "m_wanted2", "rough": 0.95},
    "wanted3": {"tex": "m_wanted3", "rough": 0.95},
    "sign_shop": {"tex": "m_sign_shop", "rough": 0.85},
    "sign_tavern": {"tex": "m_sign_tavern", "rough": 0.85},
    "sign_forge": {"tex": "m_sign_forge", "rough": 0.85},
    "sign_fish": {"tex": "m_sign_fish", "rough": 0.85},
    "sign_police": {"tex": "m_sign_police", "rough": 0.85},
    "sign_hunter": {"tex": "m_sign_hunter", "rough": 0.85},
    "sign_market": {"tex": "m_sign_market", "rough": 0.85},
    "sign_temple": {"tex": "m_sign_temple", "rough": 0.85},
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
    "notes": {"tex": "m_notes", "rough": 0.95},
    "cork": {"tex": "m_cork", "rough": 0.95},
    "trash": {"tex": "m_trash", "rough": 0.95},
    "barrel": {"tex": "m_barrel", "metal": 0.4, "rough": 0.7},
    "siren": {"tex": "m_siren", "metal": 0.4, "rough": 0.7},
    "extinguisher": {"tex": "m_extinguisher", "metal": 0.3, "rough": 0.5},
    "cardbox": {"tex": "m_cardboard", "rough": 0.95},
    "crt": {"tex": "m_crt", "rough": 0.7},
    "capdeck": {"tex": "m_capdeck", "metal": 0.7, "rough": 0.45},
    "hatch": {"tex": "m_hatch", "metal": 0.6, "rough": 0.5, "mask": True},
    "bulb_on": {"color": (1.0, 0.86, 0.55, 1.0), "emit": True},
    "bulb_off": {"color": (0.55, 0.53, 0.48, 1.0), "rough": 0.3},
    "beacon_off": {"color": (0.35, 0.08, 0.06, 1.0), "rough": 0.3},
    "puddle": {"color": (0.10, 0.09, 0.07, 1.0), "rough": 0.1},
    # ---- rev 3 castle
    "c_brick": {"tex": "m_c_brick", "rough": 0.9},
    "c_ashlar": {"tex": "m_c_ashlar", "rough": 0.9},
    "c_dark": {"tex": "m_c_darkwood", "rough": 0.8},
    "c_brass": {"tex": "m_c_brass", "metal": 0.85, "rough": 0.35},
    "c_velvet": {"tex": "m_c_velvet", "rough": 1.0},
    "c_wax": {"tex": "m_c_wax", "rough": 0.6},
    "c_horn": {"tex": "m_c_horn", "rough": 0.6},
    "c_wick": {"color": (0.04, 0.03, 0.03, 1.0), "rough": 0.9},
    # ---- rev 3 castle rooms (C-2)
    "c_tapestry": {"tex": "m_c_tapestry", "rough": 1.0, "mask": True},
    "c_linen": {"tex": "m_c_linen", "rough": 0.95},
    "c_blacksteel": {"tex": "m_c_blacksteel", "metal": 0.7, "rough": 0.45},
    "c_pages": {"tex": "m_c_pages", "rough": 0.95},
    "c_coins": {"tex": "m_c_coins", "metal": 0.85, "rough": 0.35},
    "c_bread": {"color": (0.55, 0.30, 0.10, 1.0), "rough": 0.9},
    "c_roast": {"color": (0.30, 0.10, 0.04, 1.0), "rough": 0.6},
    "c_apple": {"color": (0.45, 0.05, 0.04, 1.0), "rough": 0.5},
    "c_wine": {"color": (0.18, 0.01, 0.03, 1.0), "rough": 0.1},
    "c_soup": {"color": (0.28, 0.17, 0.06, 1.0), "rough": 0.2},
    "c_copper": {"color": (0.55, 0.22, 0.10, 1.0), "metal": 0.8, "rough": 0.35},
    "c_glow_red": {"color": (0.9, 0.12, 0.08, 1.0), "emit": True},
    "c_glow_green": {"color": (0.25, 0.9, 0.3, 1.0), "emit": True},
    "c_glow_blue": {"color": (0.2, 0.45, 1.0, 1.0), "emit": True},
    "c_glow_amber": {"color": (1.0, 0.65, 0.15, 1.0), "emit": True},
    "c_ember": {"color": (1.0, 0.36, 0.10, 1.0), "emit": True},
    "c_ember_core": {"color": (1.0, 0.82, 0.45, 1.0), "emit": True},
    "c_ember_dim": {"color": (0.42, 0.10, 0.05, 1.0), "emit": True},
    "c_crack": {"color": (0.08, 0.02, 0.01, 1.0), "rough": 0.9},
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


def cyl(name, r, h, loc, m, rot=(0, 0, 0), seg=10, r2=None):
    bm = bmesh.new()
    bmesh.ops.create_cone(bm, cap_ends=True, cap_tris=False, segments=seg, radius1=r, radius2=r if r2 is None else r2, depth=h)
    return _obj(name, bm, m, loc, rot)


def rod(name, a, b, r, m, seg=6):
    a, b = Vector(a), Vector(b)
    d = b - a
    q = d.to_track_quat('Z', 'Y')
    return cyl(name, r, d.length, tuple((a + b) / 2), m, rot=q.to_euler(), seg=seg)


def torus(name, R, r, loc, m, rot=(0, 0, 0), seg=24, seg2=6):
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


# ====================================================================================== r2 decor
def fit_uv(ob, x0, x1, y0, y1, axis=2):
    """UV of faces facing +-axis mapped to the [0,1] rect over (x0..x1, y0..y1)."""
    me = ob.data
    bm = bmesh.new()
    bm.from_mesh(me)
    uv = bm.loops.layers.uv.verify()
    for f in bm.faces:
        if abs(f.normal[axis]) > 0.9:
            for l in f.loops:
                co = l.vert.co
                l[uv].uv = ((co.x - x0) / (x1 - x0), (co.y - y0) / (y1 - y0))
    bm.to_mesh(me)
    bm.free()


def xform_parts(rot, offset=(0, 0, 0), ground=True):
    """Rotate every not-yet-noded part about the origin, then drop it onto z=0 (fallen furniture)."""
    M = Euler(rot).to_matrix().to_4x4()
    for o in PARTS:
        o.data.transform(M)
    if ground:
        mz = min((v.co.z for o in PARTS for v in o.data.vertices))
        offset = (offset[0], offset[1], offset[2] - mz)
    for o in PARTS:
        o.data.transform(Matrix.Translation(Vector(offset)))
        o.data.update()


def m_trash_bin_tipped():
    rot = (0, math.radians(90), math.radians(25))
    d = Euler(rot).to_matrix() @ Vector((0, 0, 1))
    c = Vector((0.12, 0.18, 0.22))
    cyl("bin", 0.22, 0.62, tuple(c), "teal", rot, seg=16)
    for k in (-1, 1):
        torus("rim%d" % k, 0.22, 0.018, tuple(c + d * 0.3 * k), "steel", rot, seg=16, seg2=4)
    cyl("hole", 0.2, 0.012, tuple(c - d * 0.312), "black", rot, seg=16)
    cyl("lid", 0.235, 0.03, (-0.32, 0.3, 0.015), "teal", (0, 0, 0), seg=16)
    cyl("lidk", 0.05, 0.04, (-0.32, 0.3, 0.04), "dark", seg=8)
    r = random.Random(5)
    o = c - d * 0.36
    for k in range(9):
        p = o + Vector((-r.random() * 0.45, -r.random() * 0.5 + 0.1, 0))
        box("tr%d" % k, (0.12 + r.random() * 0.14, 0.1 + r.random() * 0.12, 0.03 + r.random() * 0.04),
            (p.x, p.y, 0.025), "trash", (0, 0, r.random() * 3))
    for k in range(4):
        box("pp%d" % k, (0.2, 0.15, 0.006), (-0.1 - r.random() * 0.3, -0.25 - r.random() * 0.2, 0.004 + k * 0.002), "notes", (0, 0, r.random() * 3))
    for k in range(3):
        cyl("can%d" % k, 0.033, 0.12, (-0.25 + k * 0.17, -0.05 - k * 0.12, 0.033), ["red", "steel", "yellow"][k],
            (math.radians(90), 0, r.random() * 3), seg=8)
    node("body")
    export("trash_bin_tipped")


def m_chandelier():
    box("plate", (0.32, 0.32, 0.04), (0, 0, 0.98), "dark", bevel=0.01)
    rod("chain", (0, 0, 0.96), (0, 0, 0.5), 0.015, "steel", seg=6)
    cyl("hub", 0.07, 0.12, (0, 0, 0.46), "dark", seg=10)
    torus("ring", 0.34, 0.022, (0, 0, 0.42), "steel", seg=28, seg2=6)
    bulbs_on, bulbs = [], []
    for k in range(6):
        a = 2 * math.pi * k / 6
        e = Vector((math.cos(a) * 0.34, math.sin(a) * 0.34, 0.42))
        rod("arm%d" % k, (0, 0, 0.44), tuple(e), 0.012, "steel", seg=5)
        rod("chn%d" % k, (0, 0, 0.5), tuple(e + Vector((0, 0, 0.0))), 0.006, "dark", seg=4)
        cyl("shade%d" % k, 0.04, 0.09, tuple(e - Vector((0, 0, 0.06))), "white", seg=10, r2=0.085)
        cyl("sock%d" % k, 0.025, 0.04, tuple(e - Vector((0, 0, 0.03))), "dark", seg=8)
    node("body")
    for k in range(6):
        a = 2 * math.pi * k / 6
        e = Vector((math.cos(a) * 0.34, math.sin(a) * 0.34, 0.33))
        cyl("bo%d" % k, 0.035, 0.06, tuple(e), "bulb_off", seg=8, r2=0.02)
    node("bulbs")
    for k in range(6):
        a = 2 * math.pi * k / 6
        e = Vector((math.cos(a) * 0.34, math.sin(a) * 0.34, 0.33))
        cyl("bn%d" % k, 0.038, 0.064, tuple(e), "bulb_on", seg=8, r2=0.022)
    node("lamp_on")
    export("chandelier")


def m_siren():
    box("plate", (0.34, 0.06, 0.4), (0, 0.47, 0.55), "dark", bevel=0.01)
    for sx in (-1, 1):
        for sz in (-1, 1):
            cyl("bolt%d%d" % (sx, sz), 0.02, 0.02, (sx * 0.12, 0.435, 0.55 + sz * 0.15), "steel", (math.radians(90), 0, 0), seg=6)
    rod("arm", (0, 0.45, 0.55), (0, 0.2, 0.55), 0.04, "dark")
    rx = (math.radians(90), 0, 0)
    cyl("motor", 0.13, 0.22, (0, 0.12, 0.55), "siren", rx, seg=14)
    cyl("motorc", 0.135, 0.03, (0, 0.24, 0.55), "dark", rx, seg=14)
    for k in range(6):
        box("fin%d" % k, (0.02, 0.18, 0.04), (0, 0.12, 0.55), "dark", (0, math.radians(60 * k), 0)) if False else None
    cyl("stator", 0.19, 0.12, (0, -0.04, 0.55), "siren", rx, seg=16)
    for k in range(8):
        a = 2 * math.pi * k / 8
        box("slot%d" % k, (0.06, 0.125, 0.05), (math.cos(a) * 0.17, -0.04, 0.55 + math.sin(a) * 0.17), "black", (0, -a, 0))
    cyl("domef", 0.16, 0.05, (0, -0.12, 0.55), "siren", rx, seg=16, r2=0.12)
    cyl("cable", 0.02, 0.4, (0.08, 0.45, 0.3), "rubber", seg=6)
    cyl("lampb", 0.07, 0.04, (0, 0.12, 0.7), "dark", seg=10)
    cyl("lampoff", 0.06, 0.08, (0, 0.12, 0.76), "beacon_off", seg=10, r2=0.04)
    node("body")
    for k in range(4):
        a = math.pi * k / 2
        box("blade%d" % k, (0.3, 0.025, 0.06), (math.cos(a) * 0.0, -0.165, 0.55), "steel", (0, -a, 0))
    cyl("rcap", 0.05, 0.03, (0, -0.18, 0.55), "dark", rx, seg=10)
    node("rotor", pivot=(0, -0.165, 0.55))
    cyl("lampon", 0.062, 0.082, (0, 0.12, 0.76), "led_r", seg=10, r2=0.042)
    node("lamp_on")
    export("siren")


def m_shelf_fallen():
    # 2x3 storage shelf lying on its back along X, one end propped on a crate
    for y in (-0.22, 0.22):
        for z in (0.04, 0.56):
            rod("up%d%d" % (int(y * 10), int(z * 10)), (-1.45, y, z), (1.4, y, z), 0.025, "dark")
    for x in (-1.35, -0.45, 0.45, 1.3):
        box("board%d" % int(x * 10), (0.03, 0.46, 0.56), (x, 0, 0.3), "steel")
        for y in (-0.22, 0.22):
            rod("x%d%d" % (int(x * 10), int(y * 10)), (x, y, 0.04), (x, y, 0.56), 0.02, "dark")
    r = random.Random(9)
    for k in range(7):
        sx = -1.3 + k * 0.42
        box("bx%d" % k, (0.26, 0.22, 0.2), (sx + r.random() * 0.1, -0.45 - r.random() * 0.1, 0.1), "cardbox", (0, 0, r.random() * 0.8))
    box("bxopen", (0.3, 0.25, 0.04), (0.6, 0.4, 0.02), "cardbox", (0, 0, 0.4))
    for k in range(5):
        box("pp%d" % k, (0.2, 0.15, 0.006), (-1 + r.random() * 2, 0.32 + r.random() * 0.1, 0.004), "notes", (0, 0, r.random() * 3))
    xform_parts((0, math.radians(-6), 0))
    node("body")
    export("shelf_fallen")


def m_wall_notes():
    box("board", (0.92, 0.04, 0.62), (0, 0.475, 0.5), "cork", bevel=0.005)
    box("frame", (0.96, 0.03, 0.66), (0, 0.49, 0.5), "wood")
    r = random.Random(3)
    spots = [(-0.3, 0.62), (-0.05, 0.66), (0.22, 0.6), (-0.32, 0.36), (0.02, 0.38), (0.3, 0.34)]
    pins = []
    for k, (x, z) in enumerate(spots):
        w, h = 0.18 + r.random() * 0.06, 0.2 + r.random() * 0.06
        rot = (0, r.uniform(-0.25, 0.25), 0)
        box("note%d" % k, (w, 0.006, h), (x, 0.452, z), "notes" if k != 4 else "black", rot)
        if k == 4:
            box("photo", (w - 0.03, 0.007, h - 0.03), (x, 0.449, z), "crt", rot)
        cyl("pin%d" % k, 0.012, 0.02, (x, 0.44, z + h / 2 - 0.02), "red", (math.radians(90), 0, 0), seg=6)
        pins.append(Vector((x, 0.438, z + h / 2 - 0.02)))
    rod("string1", tuple(pins[0]), tuple(pins[4]), 0.003, "red", seg=4)
    rod("string2", tuple(pins[4]), tuple(pins[2]), 0.003, "red", seg=4)
    box("loose", (0.2, 0.006, 0.26), (0.36, 0.45, 0.12), "notes", (0, 0.6, 0))
    node("body")
    export("wall_notes")


def m_desk_computer():
    box("top", (1.9, 0.75, 0.05), (0.5, 0.05, 0.76), "wood", bevel=0.01)
    box("drawers", (0.45, 0.68, 0.72), (1.2, 0.05, 0.37), "white", bevel=0.01)
    for k in range(3):
        box("dr%d" % k, (0.4, 0.01, 0.2), (1.2, -0.29, 0.6 - k * 0.23), "white")
        box("dh%d" % k, (0.12, 0.02, 0.02), (1.2, -0.3, 0.62 - k * 0.23), "steel")
    for (x, y) in ((-0.38, -0.27), (-0.38, 0.37), (0.9, 0.37)):
        box("leg%d%d" % (int(x * 10), int(y * 10)), (0.05, 0.05, 0.74), (x, y, 0.37), "dark")
    box("modesty", (1.25, 0.02, 0.4), (0.25, 0.4, 0.5), "white")
    live, dead = [], []
    box("crt", (0.48, 0.45, 0.4), (0.25, 0.17, 1.0), "crt", bevel=0.02)
    box("crtback", (0.36, 0.25, 0.3), (0.25, 0.4, 0.98), "crt", bevel=0.02)
    box("crtbase", (0.28, 0.25, 0.04), (0.25, 0.17, 0.8), "crt")
    a, b = screen_pair("crt", (0.38, 0.3), (0.25, -0.06, 1.01), (0, 0, 0))
    for o in (a, b):
        PARTS.remove(o)
    live.append(a)
    dead.append(b)
    box("kbd", (0.45, 0.16, 0.025), (0.25, -0.18, 0.795), "crt", (math.radians(5), 0, 0))
    for k in range(4):
        box("keys%d" % k, (0.4, 0.025, 0.006), (0.25, -0.235 + k * 0.035, 0.81), "dark", (math.radians(5), 0, 0))
    box("mouse", (0.05, 0.08, 0.025), (0.6, -0.15, 0.795), "crt")
    cyl("mug", 0.04, 0.09, (0.85, 0.15, 0.83), "white", seg=10)
    box("phone", (0.2, 0.18, 0.06), (-0.25, 0.15, 0.81), "black", (0, 0, 0.3))
    r = random.Random(7)
    for k in range(5):
        box("pp%d" % k, (0.21, 0.29, 0.004), (-0.25 + r.random() * 0.4, -0.12 + r.random() * 0.15, 0.786 + k * 0.003), "notes", (0, 0, r.uniform(-0.6, 0.6)))
    box("folder", (0.24, 0.31, 0.02), (1.05, -0.1, 0.795), "teal", (0, 0, 0.2))
    node("body")
    finish_screens(live, dead)
    export("desk_computer")


def m_table_flipped():
    # lab table knocked onto its side: top is now a vertical wall at the back, legs point at the viewer
    box("top", (1.9, 0.75, 0.05), (0.5, 0.05, 0.76), "white", bevel=0.01)
    box("edge", (1.92, 0.77, 0.03), (0.5, 0.05, 0.73), "teal")
    for x in (-0.4, 1.4):
        for y in (-0.28, 0.38):
            box("leg%d%d" % (int(x * 10), int(y * 10)), (0.05, 0.05, 0.72), (x, y, 0.37), "steel")
    box("rail", (1.8, 0.03, 0.06), (0.5, 0.38, 0.12), "steel")
    xform_parts((math.radians(-90), 0, 0), (0, 0.35, 0))
    r = random.Random(11)
    for k in range(4):
        cyl("bk%d" % k, 0.035, 0.1, (r.random() * 1.6 - 0.3, -0.2 - r.random() * 0.25, 0.035), "glass",
            (math.radians(90), 0, r.random() * 3), seg=8)
    for k in range(4):
        box("pp%d" % k, (0.21, 0.29, 0.004), (r.random() * 1.6 - 0.3, -0.1 - r.random() * 0.3, 0.003 + k * 0.002), "notes", (0, 0, r.random() * 3))
    box("glassbits", (0.2, 0.15, 0.01), (0.9, -0.35, 0.005), "glass", (0, 0, 0.7))
    node("body")
    export("table_flipped")


def m_barrels():
    cyl("b1", 0.26, 0.86, (-0.2, 0.22, 0.43), "barrel", seg=16)
    for z in (0.22, 0.64):
        torus("b1r%d" % int(z * 10), 0.262, 0.015, (-0.2, 0.22, z), "dark", seg=16, seg2=4)
    cyl("b1t", 0.24, 0.01, (-0.2, 0.22, 0.865), "dark", seg=16)
    cyl("b1c", 0.04, 0.02, (-0.1, 0.3, 0.87), "steel", seg=8)
    rot = (0, math.radians(90), math.radians(-12))
    d = Euler(rot).to_matrix() @ Vector((0, 0, 1))
    c = Vector((0.06, -0.24, 0.26))
    cyl("b2", 0.26, 0.8, tuple(c), "rust", rot, seg=16)
    for k in (-1, 1):
        torus("b2r%d" % k, 0.262, 0.015, tuple(c + d * 0.2 * k), "dark", rot, seg=16, seg2=4)
    cyl("puddle", 0.32, 0.004, (0.3, -0.05, 0.003), "puddle", seg=14)
    node("body")
    export("barrels")


def m_fire_extinguisher():
    box("plate", (0.22, 0.03, 0.12), (0, 0.48, 0.55), "dark")
    rod("strap", (-0.1, 0.42, 0.5), (0.1, 0.42, 0.5), 0.012, "dark", seg=4)
    cyl("tank", 0.09, 0.46, (0, 0.36, 0.38), "extinguisher", seg=14)
    cyl("dome", 0.09, 0.05, (0, 0.36, 0.635), "extinguisher", seg=14, r2=0.04)
    cyl("valve", 0.03, 0.08, (0, 0.36, 0.69), "steel", seg=8)
    box("handle", (0.12, 0.03, 0.02), (0.03, 0.36, 0.74), "black", (0, math.radians(-10), 0))
    cyl("gauge", 0.025, 0.02, (0.03, 0.32, 0.69), "white", (math.radians(90), 0, 0), seg=10)
    rod("hose1", (-0.03, 0.36, 0.7), (-0.1, 0.3, 0.6), 0.012, "rubber", seg=5)
    rod("hose2", (-0.1, 0.3, 0.6), (-0.11, 0.27, 0.3), 0.012, "rubber", seg=5)
    cyl("nozzle", 0.02, 0.06, (-0.11, 0.27, 0.26), "black", seg=6)
    node("body")
    export("fire_extinguisher")


def m_box_pile():
    r = random.Random(13)
    specs = [((0.5, 0.42, 0.36), (-0.18, 0.15, 0.18), 0.1), ((0.42, 0.38, 0.3), (0.22, 0.2, 0.15), -0.15),
             ((0.38, 0.34, 0.3), (0.0, 0.16, 0.51), 0.35), ((0.3, 0.28, 0.24), (0.18, -0.22, 0.12), 0.6)]
    for k, (sz, loc, rz) in enumerate(specs):
        box("bx%d" % k, sz, loc, "cardbox", (0, 0, rz), bevel=0.008)
        box("tape%d" % k, (0.06, sz[1] + 0.004, sz[2] + 0.004), loc, "yellow" if k % 2 else "white", (0, 0, rz))
        box("lbl%d" % k, (0.12, 0.004, 0.08), (loc[0], loc[1] - sz[1] / 2 - 0.003, loc[2]), "notes", (0, 0, 0)) if rz == 0 else None
    box("flap", (0.28, 0.16, 0.01), (0.25, -0.3, 0.25), "cardbox", (math.radians(40), 0, 0.6))
    node("body")
    export("box_pile")


def m_paper_pile():
    r = random.Random(17)
    for k in range(14):
        box("pp%d" % k, (0.21, 0.29, 0.004), (r.uniform(-0.4, 0.4), r.uniform(-0.4, 0.4), 0.003 + k * 0.0025), "notes", (0, 0, r.random() * 3))
    for k, c in enumerate(("teal", "red", "olive")):
        box("fold%d" % k, (0.25, 0.32, 0.015), (r.uniform(-0.3, 0.3), r.uniform(-0.3, 0.3), 0.04 + k * 0.016), c, (0, 0, r.random() * 3))
    box("binder", (0.08, 0.3, 0.26), (0.3, 0.25, 0.04), "dark", (math.radians(90), 0, 0.5))
    node("body")
    export("paper_pile")


def m_chair_fallen():
    for k in range(5):
        a = 2 * math.pi * k / 5
        rod("leg%d" % k, (0, 0, 0.1), (math.cos(a) * 0.3, math.sin(a) * 0.3, 0.06), 0.02, "dark")
        cyl("wheel%d" % k, 0.035, 0.03, (math.cos(a) * 0.3, math.sin(a) * 0.3, 0.035), "black", (0, math.radians(90), a), seg=8)
    cyl("gas", 0.03, 0.35, (0, 0, 0.28), "steel", seg=10)
    box("seat", (0.48, 0.46, 0.08), (0, 0, 0.5), "black", bevel=0.03)
    rod("back", (0, 0.2, 0.5), (0, 0.24, 0.75), 0.025, "dark")
    box("backrest", (0.44, 0.07, 0.42), (0, 0.25, 0.92), "black", (math.radians(-8), 0, 0), bevel=0.03)
    for sx in (-1, 1):
        rod("arm%d" % sx, (sx * 0.24, 0.05, 0.5), (sx * 0.24, 0.05, 0.68), 0.015, "dark")
        box("armp%d" % sx, (0.06, 0.25, 0.03), (sx * 0.24, 0.02, 0.69), "black")
    xform_parts((0, math.radians(84), math.radians(30)), (0.1, 0, 0))
    node("body")
    export("chair_fallen")


# ====================================================================================== capsule / shaft
def m_capsule():
    live, dead = [], []
    S, C, R = 10.4, Vector((0, 0, 4.6)), 4.5
    box("deck", (S, S, 0.35), (0, 0, 0.825), "capdeck", bevel=0.03)
    for k, (x, y, w, d) in enumerate(((0, -S / 2 + 0.2, S - 0.1, 0.3), (0, S / 2 - 0.2, S - 0.1, 0.3),
                                       (-S / 2 + 0.2, 0, 0.3, S - 0.1), (S / 2 - 0.2, 0, 0.3, S - 0.1))):
        box("hz%d" % k, (w, d, 0.02), (x, y, 1.005), "hazard")
    for k in range(-2, 3):
        box("beamx%d" % k, (S - 0.4, 0.3, 0.5), (0, k * 2.2, 0.35), "dark")
    box("beamy", (0.3, S - 0.4, 0.5), (0, 0, 0.3), "dark")
    for sx in (-1, 1):
        for sy in (-1, 1):
            cyl("roll%d%d" % (sx, sy), 0.16, 0.9, (sx * (S / 2 - 0.2), sy * (S / 2 - 0.2), 0.45), "steel", seg=12)
    torus("base", 2.75, 0.13, (0, 0, 1.06), "steel", seg=48, seg2=8)
    torus("equator", R + 0.02, 0.07, tuple(C), "dark", seg=64, seg2=6)
    for k in range(4):
        torus("mer%d" % k, R + 0.02, 0.055, tuple(C), "steel", (math.radians(90), 0, math.radians(45 * k + 22.5)), seg=64, seg2=6)
    cyl("cap", 0.75, 0.3, (0, 0, C.z + R - 0.05), "dark", seg=24)
    cyl("cap2", 0.45, 0.2, (0, 0, C.z + R + 0.15), "white", seg=20)
    cyl("beacon0", 0.2, 0.25, (0, 0, C.z + R + 0.35), "beacon_off", seg=12, r2=0.12)
    # vestibule with two sliding iron doors (front = -Y)
    VY0, VY1, VW, VH = -2.3, -4.05, 1.3, 4.0
    for sx in (-1, 1):
        box("vwall%d" % sx, (0.14, VY0 - VY1, VH - 1.0), (sx * VW, (VY0 + VY1) / 2, (VH + 1.0) / 2), "steel")
        box("pocket%d" % sx, (1.3, 0.32, VH - 1.0), (sx * (VW + 0.65), VY1 + 0.05, (VH + 1.0) / 2), "white", bevel=0.02)
        box("pband%d" % sx, (1.32, 0.34, 0.12), (sx * (VW + 0.65), VY1 + 0.05, 2.0), "teal")
    box("vroof", (2 * VW + 2.6, (VY0 - VY1) + 0.2, 0.18), (0, (VY0 + VY1) / 2, VH + 0.05), "white", bevel=0.02)
    box("vstripe", (2 * VW + 2.6, 0.05, 0.1), (0, VY1 - 0.1, VH + 0.05), "hazard")
    box("vlamp0", (0.5, 0.06, 0.08), (0, VY1 - 0.15, VH + 0.05), "beacon_off")
    # console with monitor + PC (back)
    box("desk", (1.5, 0.6, 0.85), (0, 1.75, 1.43), "white", bevel=0.03)
    box("deskband", (1.52, 0.62, 0.07), (0, 1.75, 1.6), "teal")
    box("deskslope", (1.5, 0.45, 0.06), (0, 1.55, 1.9), "dark", (math.radians(-18), 0, 0))
    monitor("mon", 0.8, 0.5, (0, 1.9, 2.35), tilt=-8, live=live, dead=dead)
    rod("monpost", (0, 2.0, 1.86), (0, 2.0, 2.15), 0.04, "steel")
    box("pc", (0.28, 0.5, 0.55), (1.05, 1.75, 1.28), "dark", bevel=0.01)
    box("pcled", (0.02, 0.01, 0.02), (1.0, 1.495, 1.45), "led_g")
    box("kbd", (0.5, 0.16, 0.025), (0, 1.48, 1.94), "crt", (math.radians(-18), 0, 0))
    cyl("stool", 0.22, 0.06, (0, 0.95, 1.55), "black", seg=14)
    cyl("stoolp", 0.04, 0.5, (0, 0.95, 1.27), "steel", seg=8)
    cyl("stoolb", 0.25, 0.04, (0, 0.95, 1.02), "dark", seg=14)
    node("body")
    # glass sphere with a hole where the vestibule joins
    bm = bmesh.new()
    bmesh.ops.create_uvsphere(bm, u_segments=40, v_segments=20, radius=R)
    kill = [f for f in bm.faces if (abs(f.calc_center_median().x) < VW and f.calc_center_median().y < -1.0
                                    and f.calc_center_median().z + C.z < VH)
            or f.calc_center_median().z + C.z < 0.95]
    bmesh.ops.delete(bm, geom=kill, context='FACES')
    _obj("sphere", bm, "glass", tuple(C), (0, 0, 0))
    node("glass")
    for sx, nm in ((-1, "door_l"), (1, "door_r")):
        x = sx * VW / 2
        box("leaf" + nm, (VW - 0.02, 0.09, VH - 1.05), (x, VY1 + 0.05, (VH + 1.0) / 2), "steel", bevel=0.01)
        box("band" + nm, (VW, 0.1, 0.1), (x, VY1 + 0.05, 2.0), "dark")
        cyl("win" + nm, 0.18, 0.1, (x, VY1 + 0.05, 2.9), "glass", (math.radians(90), 0, 0), seg=16)
        torus("winr" + nm, 0.19, 0.03, (x, VY1 + 0.0, 2.9), "dark", (math.radians(90), 0, 0), seg=16, seg2=4)
        box("seal" + nm, (0.03, 0.11, VH - 1.05), (sx * 0.015, VY1 + 0.05, (VH + 1.0) / 2), "rubber")
        node(nm, pivot=(x, VY1 + 0.05, 2.5))
    torus("ringlight", 2.62, 0.04, (0, 0, 1.08), "core", seg=48, seg2=4)
    cyl("beacon1", 0.21, 0.26, (0, 0, C.z + R + 0.35), "core", seg=12, r2=0.13)
    box("vlamp1", (0.52, 0.065, 0.085), (0, VY1 - 0.155, VH + 0.05), "led_g")
    node("lights_on")
    finish_screens(live, dead)
    export("capsule")


def m_shaft_hatch():
    H = 5.45
    for sx, nm in ((-1, "leaf_l"), (1, "leaf_r")):
        xc = sx * H / 2
        ob = box("plate" + nm, (H, 2 * H, 0.1), (xc, 0, 0.95), "hatch")
        fit_uv(ob, min(0, sx * H), max(0, sx * H), -H, H)
        for k in range(3):
            box("rib%s%d" % (nm, k), (0.12, 2 * H - 0.2, 0.22), (sx * (0.9 + k * 1.75), 0, 0.79), "dark")
        for k in range(4):
            box("cross%s%d" % (nm, k), (H - 0.2, 0.12, 0.18), (xc, -H + 1.4 + k * 2.75, 0.81), "dark")
        box("edge" + nm, (0.1, 2 * H, 0.18), (sx * 0.05, 0, 0.9), "hazard")
        for k in range(4):
            cyl("hinge%s%d" % (nm, k), 0.08, 1.2, (sx * H, -H + 1.2 + k * 2.9, 0.92), "steel", (math.radians(90), 0, 0), seg=10)
        node(nm, pivot=(sx * H, 0, 0.92))
    export("shaft_hatch")


def m_scan_ring():
    for k, (r, m) in enumerate(((1.0, "core"),)):
        bm = bmesh.new()
        bmesh.ops.create_cone(bm, cap_ends=False, cap_tris=False, segments=96, radius1=r, radius2=r, depth=0.14)
        inner = [f.copy() for f in list(bm.faces)]
        for f in inner:
            f.normal_flip()
        _obj("ring%d" % k, bm, m, (0, 0, 0.07), (0, 0, 0))
    node("ring")
    export("scan_ring")



# ====================================================================================== Elisar (city «Странники»)
def fit_face(ob, axis, sign, u0, u1, v0, v1, flip=False):
    """Map faces whose normal is sign*axis to the full texture: u over (u0,u1), v over (v0,v1)."""
    me = ob.data
    bm = bmesh.new()
    bm.from_mesh(me)
    uv = bm.loops.layers.uv.verify()
    for f in bm.faces:
        if f.normal[axis] * sign > 0.9:
            for l in f.loops:
                co = l.vert.co
                if axis == 0:
                    u, v = co.y, co.z
                elif axis == 1:
                    u, v = co.x, co.z
                else:
                    u, v = co.x, co.y
                uu = (u - u0) / (u1 - u0)
                l[uv].uv = (1 - uu if flip else uu, (v - v0) / (v1 - v0))
    bm.to_mesh(me)
    bm.free()


def plane(name, w, h, loc, m, rot=(0, 0, 0)):
    """Two-sided quad (w along local X, h along local Z) with full-texture UV, for foliage/posters."""
    bm = bmesh.new()
    vs = [bm.verts.new(p) for p in ((-w / 2, 0, -h / 2), (w / 2, 0, -h / 2), (w / 2, 0, h / 2), (-w / 2, 0, h / 2))]
    f1 = bm.faces.new(vs)
    f2 = bm.faces.new(list(reversed([bm.verts.new(v.co) for v in vs])))
    uv = bm.loops.layers.uv.verify()
    for f, rev in ((f1, False), (f2, True)):
        for l, t in zip(f.loops, ((0, 0), (1, 0), (1, 1), (0, 1)) if not rev else ((0, 1), (1, 1), (1, 0), (0, 0))):
            l[uv].uv = t
    me = bpy.data.meshes.new(name)
    bm.to_mesh(me)
    bm.free()
    ob = bpy.data.objects.new(name, me)
    bpy.context.scene.collection.objects.link(ob)
    me.materials.append(mat(m))
    ob.location = loc
    ob.rotation_euler = rot
    bpy.context.view_layer.update()
    me.transform(ob.matrix_world)
    ob.matrix_world = Matrix.Identity(4)
    PARTS.append(ob)
    return ob


def tuft(name, loc, s=0.4, m="e_foliage", n=2, rz=0.0):
    for k in range(n):
        plane("%s%d" % (name, k), s, s, (loc[0], loc[1], loc[2] + s / 2), m, (0, 0, rz + math.pi * k / n))


def barrel(name, loc, r=0.26, h=0.8, m="e_wood", band="e_iron", open_top=False):
    x, y, z = loc
    cyl(name, r, h, (x, y, z + h / 2), m, seg=14, r2=None)
    cyl(name + "b", r * 1.06, h * 0.55, (x, y, z + h / 2), m, seg=14)
    for t in (0.12, 0.88):
        torus(name + "r%d" % int(t * 10), r * 1.03, 0.012, (x, y, z + h * t), band, seg=14, seg2=4)
    if open_top:
        cyl(name + "in", r * 0.92, 0.01, (x, y, z + h - 0.06), "e_coal", seg=14)


def spoked_wheel(name, loc, R, rot, spokes=8, broken=False):
    torus(name + "rim", R, 0.035, loc, "e_wood_dark", rot, seg=20, seg2=4)
    torus(name + "tyre", R + 0.03, 0.018, loc, "e_iron", rot, seg=20, seg2=4)
    cyl(name + "hub", 0.07, 0.16, loc, "e_wood_dark", (rot[0] + math.radians(90), rot[1], rot[2]), seg=10)
    M = Euler(rot).to_matrix()
    for k in range(spokes):
        if broken and k in (2, 3):
            continue
        a = 2 * math.pi * k / spokes
        d = M @ Vector((math.cos(a), math.sin(a), 0))
        rod(name + "sp%d" % k, tuple(Vector(loc) + d * 0.07), tuple(Vector(loc) + d * (R - 0.02)), 0.018, "e_wood_dark", seg=5)


# ------------------------------------------------------------------ street
def m_street_lamp():
    cyl("base", 0.22, 0.25, (0, 0, 0.125), "e_stone", seg=8)
    cyl("base2", 0.15, 0.12, (0, 0, 0.31), "e_iron", seg=8)
    rod("post", (0, 0, 0.3), (0, 0, 2.55), 0.055, "e_iron", seg=8)
    for z in (0.9, 1.8):
        torus("ring%d" % int(z * 10), 0.07, 0.015, (0, 0, z), "e_iron", seg=10, seg2=4)
    rod("arm", (0, 0, 2.45), (0, -0.42, 2.6), 0.03, "e_iron", seg=6)
    rod("brace", (0, 0, 2.1), (0, -0.3, 2.56), 0.018, "e_iron", seg=5)
    cyl("finial", 0.06, 0.12, (0, 0, 2.62), "e_iron", seg=8, r2=0.0)
    rod("hook", (0, -0.42, 2.6), (0, -0.42, 2.48), 0.012, "e_iron", seg=4)
    cyl("cap", 0.16, 0.12, (0, -0.42, 2.42), "e_iron", seg=6, r2=0.04)
    cyl("bottom", 0.12, 0.05, (0, -0.42, 2.05), "e_iron", seg=6)
    for k in range(6):
        a = 2 * math.pi * k / 6 + math.pi / 6
        rod("frame%d" % k, (math.cos(a) * 0.12, -0.42 + math.sin(a) * 0.12, 2.07), (math.cos(a) * 0.14, -0.42 + math.sin(a) * 0.14, 2.37), 0.008, "e_iron", seg=4)
    node("body")
    cyl("glow", 0.115, 0.29, (0, -0.42, 2.22), "e_lamp", seg=6)
    cyl("flame", 0.03, 0.1, (0, -0.42, 2.16), "e_flame", seg=6, r2=0.0)
    node("lamp_on")
    export("street_lamp")


def m_lantern_hanging():
    # rustic iron ring chandelier (ref: wrought-iron tavern chandeliers): ceiling plate, 3 chains, ring, 4 crowned candle cups
    cyl("plate", 0.09, 0.03, (0, 0, 0.985), "e_iron", seg=8)
    rod("hook_top", (0, 0, 0.97), (0, 0, 0.9), 0.012, "e_iron", seg=4)
    for k in range(7):  # short centre chain (links overlap -> no floating parts)
        torus("cc%d" % k, 0.022, 0.006, (0, 0, 0.895 - k * 0.032), "e_iron", (0, math.radians(90) if k % 2 else 0, 0), seg=6, seg2=3)
    cyl("hub", 0.03, 0.04, (0, 0, 0.68), "e_iron", seg=6)
    R, zr = 0.3, 0.42
    for k in range(3):
        a = 2 * math.pi * k / 3 + math.pi / 6
        rod("chain%d" % k, (0, 0, 0.67), (math.cos(a) * R, math.sin(a) * R, zr + 0.01), 0.007, "e_iron", seg=4)
    torus("ring", R, 0.016, (0, 0, zr), "e_iron", seg=20, seg2=4)
    torus("ring_low", R * 0.55, 0.01, (0, 0, zr - 0.08), "e_iron", seg=14, seg2=4)
    for k in range(4):
        a = 2 * math.pi * k / 4
        cx, cy = math.cos(a) * R, math.sin(a) * R
        rod("spoke%d" % k, (cx * 0.55, cy * 0.55, zr - 0.08), (cx, cy, zr), 0.008, "e_iron", seg=4)
        cyl("cup%d" % k, 0.04, 0.05, (cx, cy, zr + 0.03), "e_iron", seg=6, r2=0.05)
        cyl("drip%d" % k, 0.012, 0.09, (cx, cy, zr - 0.05), "e_iron", seg=5, r2=0.0)
        for j in range(4):  # crown spikes around the candle
            b = 2 * math.pi * j / 4 + math.pi / 4
            rod("sp%d%d" % (k, j), (cx + math.cos(b) * 0.045, cy + math.sin(b) * 0.045, zr + 0.05), (cx + math.cos(b) * 0.06, cy + math.sin(b) * 0.06, zr + 0.16), 0.006, "e_iron", seg=3)
        cyl("candle%d" % k, 0.026, 0.1, (cx, cy, zr + 0.1), "e_candle", seg=6)
    node("body")
    for k in range(4):
        a = 2 * math.pi * k / 4
        cyl("flame%d" % k, 0.016, 0.06, (math.cos(a) * R, math.sin(a) * R, zr + 0.18), "e_flame", seg=5, r2=0.0)
    node("lamp_on")
    export("lantern_hanging")


def _hanging_sign(name, tex, shape="rect"):
    # wall at +Y; iron bracket reaches out along -Y, board hangs below it, faces +-X
    box("plate", (0.16, 0.04, 0.34), (0, 0.48, 0.78), "e_iron")
    rod("bar", (0, 0.46, 0.9), (0, -0.62, 0.9), 0.022, "e_iron", seg=6)
    rod("strut", (0, 0.46, 0.64), (0, -0.05, 0.9), 0.016, "e_iron", seg=5)
    for k in range(3):
        torus("curl%d" % k, 0.06, 0.01, (0, 0.25 - k * 0.22, 0.97), "e_iron", (0, math.radians(90), 0), seg=10, seg2=4)
    cyl("knob", 0.03, 0.05, (0, -0.64, 0.9), "e_iron", (math.radians(90), 0, 0), seg=8)
    for y in (0.22, -0.48):
        for k in range(3):
            torus("ch%d%d" % (int(y * 10), k), 0.022, 0.006, (0, y, 0.86 - k * 0.04), "e_iron", (0, math.radians(90) if k % 2 else 0, 0), seg=6, seg2=3)
    node("bracket")
    W, H, zc = 0.82, 0.41, 0.53
    ob = box("board", (0.05, W, H), (0, -0.13, zc), tex)
    for sgn in (1, -1):
        fit_face(ob, 0, sgn, -0.54 if sgn > 0 else 0.28, 0.28 if sgn > 0 else -0.54, zc - H / 2, zc + H / 2)
    box("frame_t", (0.06, W + 0.03, 0.03), (0, -0.13, zc + H / 2), "e_wood_dark")
    box("frame_b", (0.06, W + 0.03, 0.03), (0, -0.13, zc - H / 2), "e_wood_dark")
    node("board", pivot=(0, -0.13, 0.86))
    export(name)


def m_sign_shop(): _hanging_sign("sign_shop", "sign_shop")
def m_sign_tavern(): _hanging_sign("sign_tavern", "sign_tavern")
def m_sign_forge(): _hanging_sign("sign_forge", "sign_forge")
def m_sign_fish(): _hanging_sign("sign_fish", "sign_fish")
def m_sign_police(): _hanging_sign("sign_police", "sign_police")
def m_sign_hunter(): _hanging_sign("sign_hunter", "sign_hunter")
def m_sign_market(): _hanging_sign("sign_market", "sign_market")
def m_sign_temple(): _hanging_sign("sign_temple", "sign_temple")


def m_bounty_board():
    # free standing, 2 cells wide (x 0..1), front -Y
    for x in (-0.38, 1.38):
        box("post%d" % int(x * 10), (0.12, 0.12, 2.2), (x, 0.1, 1.1), "e_wood_dark")
        box("foot%d" % int(x * 10), (0.16, 0.5, 0.1), (x, 0.1, 0.05), "e_wood_dark")
    box("board", (1.72, 0.06, 1.15), (0.5, 0.1, 1.25), "e_wood")
    box("rim_t", (1.8, 0.09, 0.07), (0.5, 0.08, 1.84), "e_wood_dark")
    box("rim_b", (1.8, 0.09, 0.07), (0.5, 0.08, 0.66), "e_wood_dark")
    for sgn in (-1, 1):
        box("roof%d" % sgn, (2.0, 0.42, 0.05), (0.5, 0.1 + sgn * 0.17, 2.22), "e_wood_dark", (math.radians(28) * -sgn, 0, 0))
    box("ridge", (2.02, 0.07, 0.07), (0.5, 0.1, 2.32), "e_wood_dark")
    r = random.Random(4)
    spots = [(-0.1, 1.42, "wanted1"), (0.35, 1.38, "wanted2"), (0.82, 1.43, "wanted3"), (1.18, 1.2, "wanted1"), (0.1, 0.98, "parch"), (0.6, 0.95, "wanted3")]
    for k, (x, z, t) in enumerate(spots):
        w, h = (0.34, 0.47) if t.startswith("wanted") else (0.3, 0.24)
        tilt = r.uniform(-0.12, 0.12)
        ob = box("poster%d" % k, (w, 0.006, h), (x, 0.064 - k * 0.002, z), "e_parch" if t == "parch" else t, (0, tilt, 0))
        if t != "parch":
            fit_face(ob, 1, -1, -1e3, 1e3, -1e3, 1e3)
            mn = min(v.co.x for v in ob.data.vertices); mx = max(v.co.x for v in ob.data.vertices)
            zn = min(v.co.z for v in ob.data.vertices); zx = max(v.co.z for v in ob.data.vertices)
            fit_face(ob, 1, -1, mn, mx, zn, zx)
        cyl("nail%d" % k, 0.012, 0.02, (x, 0.055 - k * 0.002, z + h / 2 - 0.03), "e_iron", (math.radians(90), 0, 0), seg=5)
    # dagger stuck through a poster
    rod("blade", (0.48, -0.02, 1.34), (0.48, 0.12, 1.34), 0.012, "e_iron", seg=4)
    box("guard", (0.12, 0.015, 0.02), (0.48, -0.03, 1.34), "e_gold")
    rod("grip", (0.48, -0.03, 1.34), (0.48, -0.15, 1.34), 0.018, "e_wood_dark", seg=6)
    cyl("pommel", 0.022, 0.02, (0.48, -0.16, 1.34), "e_gold", (math.radians(90), 0, 0), seg=6)
    node("body")
    export("bounty_board")


def _stall(name, stripe, goods):
    # 3 wide (x -1..1), 2 deep (back row), front -Y
    for x in (-1.35, 1.35):
        for y in (-0.4, 1.35):
            box("post%d%d" % (int(x * 10), int(y * 10)), (0.1, 0.1, 2.3 if y > 0 else 2.0), (x, y, 1.15 if y > 0 else 1.0), "e_wood_dark")
    ob = box("canopy", (2.95, 2.2, 0.03), (0, 0.45, 2.15), stripe, (math.radians(-9), 0, 0))
    fit_face(ob, 2, 1, -1.5, 1.5, -0.7, 1.6)
    fit_face(ob, 2, -1, -1.5, 1.5, -0.7, 1.6)
    for k in range(10):
        x = -1.35 + k * 0.3
        box("flap%d" % k, (0.28, 0.02, 0.22), (x + 0.14, -0.66, 1.92), stripe, (math.radians(-4), 0, 0))
    box("counter", (2.6, 0.6, 0.85), (0, -0.15, 0.43), "e_wood", bevel=0.02)
    box("ctop", (2.75, 0.72, 0.05), (0, -0.15, 0.88), "e_wood_dark")
    box("back", (2.6, 0.05, 1.1), (0, 1.3, 0.75), "e_wood")
    box("shelf", (2.5, 0.3, 0.04), (0, 1.15, 1.25), "e_wood_dark")
    goods()
    node("body")
    export(name)


def _goods_produce():
    r = random.Random(8)
    for k, x in enumerate((-0.95, -0.35, 0.3, 0.9)):
        box("crate%d" % k, (0.5, 0.42, 0.14), (x, -0.15, 0.98), "e_wood_dark")
        for j in range(7):
            cyl("fr%d%d" % (k, j), 0.05, 0.08, (x + r.uniform(-0.18, 0.18), -0.15 + r.uniform(-0.14, 0.14), 1.08), "e_produce", seg=6, r2=0.03)
    for k in range(5):
        cyl("jar%d" % k, 0.06, 0.16, (-1.0 + k * 0.48, 1.15, 1.35), "e_gold" if k % 2 else "glass", seg=8)
    for k in range(3):
        rod("hang%d" % k, (-0.6 + k * 0.6, -0.4, 1.95), (-0.6 + k * 0.6, -0.4, 1.7), 0.006, "e_rope", seg=3)
        cyl("onion%d" % k, 0.07, 0.16, (-0.6 + k * 0.6, -0.4, 1.62), "e_produce", seg=6, r2=0.02)


def _goods_fish():
    r = random.Random(9)
    box("ice", (2.4, 0.55, 0.06), (0, -0.15, 0.93), "e_ice")
    for k in range(9):
        x = -1.05 + k * 0.26
        _fish("f%d" % k, (x, -0.15 + r.uniform(-0.12, 0.12), 0.99), 0.32, "e_fish" if k % 3 else "e_fish2", rz=math.radians(90) + r.uniform(-0.3, 0.3))
    for k in range(3):
        _fish("hf%d" % k, (-0.7 + k * 0.7, -0.45, 1.6), 0.36, "e_fish", rx=math.radians(90))
        rod("hr%d" % k, (-0.7 + k * 0.7, -0.45, 1.95), (-0.7 + k * 0.7, -0.45, 1.78), 0.006, "e_rope", seg=3)
    barrel("bar", (1.0, 1.0, 0), 0.22, 0.7, open_top=True)


def _fish(name, loc, L, m, rz=0.0, rx=0.0):
    """Alien fish: fat lens body, double tail fin, dorsal crest and two glowing spots."""
    x, y, z = loc
    R = Euler((rx, 0, rz)).to_matrix()
    def at(dx, dy, dz):
        return tuple(Vector((x, y, z)) + R @ Vector((dx, dy, dz)))
    cyl(name + "body", L * 0.22, L * 0.62, at(0, 0, 0), m, (Euler((rx, 0, rz)).to_matrix() @ Euler((0, math.radians(90), 0)).to_matrix()).to_euler(), seg=8, r2=L * 0.1)
    for s in (1, -1):
        box(name + "tail%d" % s, (L * 0.22, 0.01, L * 0.12), at(-L * 0.42, 0, s * L * 0.07), m, (rx + 0, s * 0.5, rz) if False else (rx, s * 0.5, rz))
    box(name + "crest", (L * 0.3, 0.008, L * 0.1), at(0, 0, L * 0.2), "e_fish2" if m == "e_fish" else "e_fish", (rx, 0, rz))
    for s in (1, -1):
        cyl(name + "eye%d" % s, L * 0.035, 0.01, at(L * 0.22, s * L * 0.12, L * 0.03), "e_glow", (rx + math.radians(90), 0, rz), seg=5)


def m_market_stall(): _stall("market_stall", "e_stripe", _goods_produce)
def m_market_stall_fish(): _stall("market_stall_fish", "e_stripe_blue", _goods_fish)


def m_wagon_overgrown():
    # abandoned cart, no horse: 3 long (x -1..1), 2 wide (back row), nose of the bed resting on the ground
    rot = (0, math.radians(-7), 0)
    def T(p):
        return tuple(Euler(rot).to_matrix() @ Vector(p) + Vector((0, 0.45, 0.12)))
    box("bed", (2.5, 1.2, 0.07), T((0.1, 0, 0.6)), "e_wood", rot)
    for y in (-0.6, 0.6):
        box("side%d" % int(y * 10), (2.5, 0.06, 0.32), T((0.1, y, 0.8)), "e_wood", rot)
    box("tail", (0.06, 1.2, 0.32), T((1.35, 0, 0.8)), "e_wood", rot)
    box("frontb", (0.06, 1.2, 0.5), T((-1.15, 0, 0.88)), "e_wood_dark", rot)
    for x in (-0.5, 0.7):
        box("axle%d" % int(x * 10), (0.1, 1.5, 0.1), T((x, 0, 0.45)), "e_wood_dark", rot)
    spoked_wheel("w1", T((0.7, -0.78, 0.45)), 0.42, (math.radians(90), 0, 0))
    spoked_wheel("w2", T((0.7, 0.78, 0.45)), 0.42, (math.radians(90), 0, 0))
    spoked_wheel("w3", T((-0.5, -0.78, 0.4)), 0.38, (math.radians(90), 0, 0))
    spoked_wheel("w4", (-0.75, 1.45, 0.06), 0.38, (0, math.radians(8), 0.4), broken=True)     # fallen off, lying in the grass
    # drawbar lying on the ground where the horse once stood
    rod("shaft1", T((-1.15, -0.35, 0.45)), (-1.95, 0.12, 0.05), 0.04, "e_wood_dark", seg=6)
    rod("shaft2", T((-1.15, 0.35, 0.45)), (-1.95, 0.8, 0.05), 0.04, "e_wood_dark", seg=6)
    rod("yoke", (-1.95, 0.12, 0.05), (-1.95, 0.8, 0.05), 0.035, "e_wood_dark", seg=6)
    # canvas hoops, one broken, torn canvas
    for k, x in enumerate((-0.8, 0.1, 1.0)):
        for s in range(9):
            a0, a1 = math.pi * s / 9, math.pi * (s + 1) / 9
            if k == 2 and s > 4:
                continue
            rod("hoop%d_%d" % (k, s), T((x, 0.62 * math.cos(a0), 0.85 + 0.65 * math.sin(a0))), T((x, 0.62 * math.cos(a1), 0.85 + 0.65 * math.sin(a1))), 0.018, "e_wood_dark", seg=4)
    for s in range(7):
        a0, a1 = math.pi * s / 9, math.pi * (s + 1) / 9
        p0 = T((-0.35, 0.63 * math.cos(a0), 0.85 + 0.66 * math.sin(a0)))
        p1 = T((-0.35, 0.63 * math.cos(a1), 0.85 + 0.66 * math.sin(a1)))
        mid = tuple((Vector(p0) + Vector(p1)) / 2)
        ln = (Vector(p1) - Vector(p0)).length
        ang = math.atan2(p1[2] - p0[2], p1[1] - p0[1])
        box("canv%d" % s, (0.95 - (s % 3) * 0.12, ln + 0.02, 0.012), mid, "e_canvas", (ang, 0, 0))
    box("rag", (0.5, 0.012, 0.6), T((0.55, 0.64, 0.95)), "e_canvas", (0.2, 0.1, 0))
    # overgrowth: grass tufts, flowers through the planks, ivy on the wheels, a sapling growing in the bed
    r = random.Random(12)
    for k in range(14):
        p = (r.uniform(-1.9, 1.6), r.uniform(-0.3, 1.5), 0.0)
        tuft("g%d" % k, p, r.uniform(0.35, 0.65), "e_foliage" if k % 3 else "e_flowers", 2, r.random() * 3)
    for k in range(5):
        p = T((r.uniform(-0.9, 1.1), r.uniform(-0.45, 0.45), 0.64))
        tuft("bg%d" % k, p, r.uniform(0.35, 0.6), "e_foliage" if k % 2 else "e_flowers", 2, r.random() * 3)
    for (wx, wy) in ((0.7, -0.84), (-0.5, -0.84)):
        plane("ivy%d" % int(wx * 10), 0.7, 0.8, T((wx, wy - 0.02, 0.42)), "e_foliage", (0, 0, 0))
    rod("sapling", T((0.2, 0.1, 0.62)), T((0.25, 0.15, 1.5)), 0.025, "e_bark", seg=5)
    for k in range(4):
        cyl("crown%d" % k, 0.25 - k * 0.04, 0.18, T((0.25, 0.15, 1.45 + k * 0.14)), "e_leaf_cyan", seg=7, r2=0.1)
    node("body")
    export("wagon_overgrown")


def m_bench():
    box("seat", (1.7, 0.4, 0.06), (0.5, 0.0, 0.45), "e_wood")
    box("back", (1.7, 0.05, 0.3), (0.5, 0.2, 0.75), "e_wood", (math.radians(-10), 0, 0))
    for x in (-0.25, 1.25):
        box("leg%d" % int(x * 10), (0.08, 0.42, 0.44), (x, 0.0, 0.22), "e_iron")
        rod("arm%d" % int(x * 10), (x, 0.2, 0.9), (x, -0.18, 0.62), 0.025, "e_iron", seg=5)
        rod("bk%d" % int(x * 10), (x, 0.18, 0.44), (x, 0.24, 0.9), 0.025, "e_iron", seg=5)
    node("body")
    export("bench")


def m_well():
    # 2x2 (x 0..1, back row), stone ring centred between the 4 cells
    c = (0.5, 0.5)
    cyl("ring", 0.85, 0.75, (c[0], c[1], 0.375), "e_stone", seg=16)
    cyl("water", 0.68, 0.02, (c[0], c[1], 0.55), "e_water", seg=16)
    torus("lip", 0.8, 0.07, (c[0], c[1], 0.76), "e_rock", seg=16, seg2=5)
    for x in (-0.35, 1.35):
        box("post%d" % int(x * 10), (0.12, 0.12, 2.0), (x, c[1], 1.0), "e_wood_dark")
    rod("axle", (-0.4, c[1], 1.45), (1.4, c[1], 1.45), 0.06, "e_wood", seg=8)
    cyl("rope_coil", 0.1, 0.5, (c[0], c[1], 1.45), "e_rope", (0, math.radians(90), 0), seg=10)
    rod("crank1", (1.42, c[1], 1.45), (1.42, c[1] - 0.25, 1.45), 0.02, "e_iron", seg=4)
    rod("crank2", (1.42, c[1] - 0.25, 1.45), (1.55, c[1] - 0.25, 1.45), 0.02, "e_iron", seg=4)
    rod("rope", (c[0], c[1] - 0.08, 1.4), (c[0], c[1] - 0.08, 1.0), 0.012, "e_rope", seg=4)
    cyl("bucket", 0.12, 0.2, (c[0], c[1] - 0.08, 0.9), "e_wood", seg=10, r2=0.1)
    for s in (-1, 1):
        box("roof%d" % s, (2.1, 0.75, 0.05), (c[0], c[1] + s * 0.32, 2.12), "roof_r", (math.radians(32) * -s, 0, 0))
    box("ridge", (2.15, 0.08, 0.08), (c[0], c[1], 2.32), "e_wood_dark")
    node("body")
    export("well")


# ------------------------------------------------------------------ fishermen
def m_fish_barrel():
    barrel("b", (0, 0.02, 0), 0.3, 0.78, open_top=True)
    r = random.Random(14)
    for k in range(6):
        _fish("f%d" % k, (r.uniform(-0.12, 0.12), r.uniform(-0.1, 0.12), 0.78 + k * 0.012), 0.34, "e_fish" if k % 2 else "e_fish2",
              rz=r.random() * 3, rx=math.radians(r.uniform(40, 70)))
    _fish("ground", (0.22, -0.32, 0.03), 0.3, "e_fish2", rz=0.7)
    node("body")
    export("fish_barrel")


def m_fish_rack():
    # drying rack 2 wide (x 0..1)
    for x in (-0.4, 1.4):
        rod("a%d" % int(x * 10), (x, -0.3, 0), (x, 0, 1.6), 0.035, "e_wood_dark", seg=5)
        rod("b%d" % int(x * 10), (x, 0.3, 0), (x, 0, 1.6), 0.035, "e_wood_dark", seg=5)
    rod("top", (-0.45, 0, 1.55), (1.45, 0, 1.55), 0.03, "e_wood_dark", seg=6)
    rod("mid", (-0.4, 0, 1.0), (1.4, 0, 1.0), 0.025, "e_wood_dark", seg=6)
    for k in range(7):
        x = -0.25 + k * 0.25
        rod("s%d" % k, (x, 0, 1.55), (x, 0, 1.42), 0.005, "e_rope", seg=3)
        _fish("f%d" % k, (x, 0, 1.28), 0.3, "e_fish" if k % 2 else "e_fish2", rx=math.radians(90))
    for k in range(5):
        x = -0.15 + k * 0.33
        rod("t%d" % k, (x, 0, 1.0), (x, 0, 0.88), 0.005, "e_rope", seg=3)
        _fish("g%d" % k, (x, 0, 0.74), 0.28, "e_fish2", rx=math.radians(90))
    node("body")
    export("fish_rack")


def m_fishing_net():
    # wall piece 2 wide (x 0..1), wall at +Y
    for k in range(3):
        cyl("peg%d" % k, 0.025, 0.12, (-0.3 + k * 0.8, 0.44, 1.6), "e_wood_dark", (math.radians(90), 0, 0), seg=6)
    for i in range(9):
        x = -0.35 + i * 0.21
        sag = 0.12 * math.sin(math.pi * i / 8)
        rod("v%d" % i, (x, 0.4, 1.6 - sag), (x + 0.05 * math.sin(i), 0.36, 0.25), 0.006, "e_rope", seg=3)
    for j in range(7):
        z = 1.55 - j * 0.2
        pts = [(-0.35 + i * 0.21 + 0.05 * math.sin(i) * (1.55 - z) / 1.3, 0.38, z - 0.1 * math.sin(math.pi * i / 8)) for i in range(9)]
        for i in range(8):
            rod("h%d%d" % (j, i), pts[i], pts[i + 1], 0.005, "e_rope", seg=3)
    for k, (x, z, m) in enumerate(((0.0, 0.6, "e_cork"), (0.45, 0.4, "e_red"), (0.9, 0.7, "e_cork"), (1.25, 0.45, "e_red"))):
        cyl("float%d" % k, 0.06, 0.18, (x, 0.33, z), m, seg=8, r2=0.04)
    node("body")
    export("fishing_net")


def hull_loft(name, secs, m):
    """secs: list of (x, [(y,z),...]) open U sections -> quad strip shell (two-sided via thickness-free faces)."""
    bm = bmesh.new()
    rings = [[bm.verts.new((x, y, z)) for (y, z) in pts] for x, pts in secs]
    inner = [[bm.verts.new((x, y * 0.95, z - 0.025)) for (y, z) in pts] for x, pts in secs]
    for i in range(len(rings) - 1):
        for j in range(len(rings[i]) - 1):
            bm.faces.new((rings[i][j], rings[i + 1][j], rings[i + 1][j + 1], rings[i][j + 1]))
            bm.faces.new((inner[i][j + 1], inner[i + 1][j + 1], inner[i + 1][j], inner[i][j]))
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces[:])
    return _obj(name, bm, m, (0, 0, 0), (0, 0, 0))


def m_rowboat():
    # upside down on two trestles, 3 long (x -1..1); hull = lofted planked shell, keel up
    secs = []
    for k in range(13):
        t = k / 12
        x = -1.35 + 2.7 * t
        w = 0.56 * math.sin(math.pi * (0.04 + 0.92 * t)) ** 0.6 + 0.02
        d = 0.36 * (0.55 + 0.45 * math.sin(math.pi * t))
        sheer = 0.1 * (2 * t - 1) ** 2            # gunwale rises toward bow/stern (points down when flipped)
        pts = []
        for j in range(9):
            a = math.pi * j / 8
            y = -w * math.cos(a)
            z = 0.75 + d * math.sin(a) ** 0.8 - sheer
            pts.append((y, z))
        secs.append((x, pts))
    hull_loft("hull", secs, "e_wood")
    for j in (1, 3, 5, 7):          # strakes: thin battens along the planking
        pts = [(x, p[j][0] * 1.01, p[j][1] + 0.005) for x, p in secs]
        for i in range(len(pts) - 1):
            rod("strk%d_%d" % (j, i), pts[i], pts[i + 1], 0.012, "e_wood_dark", seg=4)
    gun = [(x, p[0][0], p[0][1]) for x, p in secs]
    gun2 = [(x, p[-1][0], p[-1][1]) for x, p in secs]
    for i in range(len(gun) - 1):
        rod("gw%d" % i, gun[i], gun[i + 1], 0.025, "e_wood_dark", seg=4)
        rod("gx%d" % i, gun2[i], gun2[i + 1], 0.025, "e_wood_dark", seg=4)
    keel = [(x, 0, p[4][1] + 0.03) for x, p in secs]
    for i in range(len(keel) - 1):
        rod("keel%d" % i, keel[i], keel[i + 1], 0.03, "e_wood_dark", seg=4)
    box("stem", (0.06, 0.06, 0.4), (-1.37, 0, 0.8), "e_wood_dark")
    box("transom", (0.05, 0.6, 0.32), (1.35, 0, 0.88), "e_wood_dark")
    for x in (-0.7, 0.7):
        for s_ in (1, -1):
            rod("tr%d%d" % (int(x * 10), s_), (x - 0.25 * s_, 0.0, 0), (x, 0.0, 0.66), 0.03, "e_wood_dark", seg=5)
        rod("trt%d" % int(x * 10), (x, -0.62, 0.68), (x, 0.62, 0.68), 0.035, "e_wood_dark", seg=5)
    rod("oar", (-1.2, -0.75, 0.04), (1.0, -0.6, 0.04), 0.025, "e_wood", seg=5)
    box("blade", (0.4, 0.14, 0.02), (1.1, -0.59, 0.04), "e_wood")
    node("body")
    export("rowboat")


# ------------------------------------------------------------------ hunters
def m_trophy_antlers():
    box("plaque", (0.42, 0.04, 0.5), (0, 0.48, 1.0), "e_wood_dark", bevel=0.01)
    cyl("skull", 0.1, 0.22, (0, 0.38, 1.02), "e_bone", (math.radians(90), 0, 0), seg=8, r2=0.06)
    for s in (1, -1):
        p0 = Vector((s * 0.07, 0.36, 1.1))
        p1 = p0 + Vector((s * 0.22, -0.05, 0.25)); p2 = p1 + Vector((s * 0.12, 0.0, 0.25))
        rod("a%d" % s, tuple(p0), tuple(p1), 0.02, "e_bone", seg=5); rod("b%d" % s, tuple(p1), tuple(p2), 0.017, "e_bone", seg=5)
        for k, (q, d) in enumerate(((p1, Vector((s * -0.05, -0.08, 0.15))), (p1 + (p2 - p1) * 0.5, Vector((s * 0.12, -0.04, 0.1))), (p2, Vector((s * -0.02, -0.06, 0.14))))):
            rod("t%d%d" % (s, k), tuple(q), tuple(q + d), 0.012, "e_bone", seg=4)
            cyl("tip%d%d" % (s, k), 0.016, 0.02, tuple(q + d), "e_glow", seg=5)
    node("body")
    export("trophy_antlers")


def m_hide_rack():
    for x in (-0.42, 0.42):
        rod("p%d" % int(x * 10), (x, 0.05, 0), (x, 0.05, 1.85), 0.04, "e_wood_dark", seg=6)
    for z in (0.35, 1.7):
        rod("h%d" % int(z * 10), (-0.45, 0.05, z), (0.45, 0.05, z), 0.03, "e_wood_dark", seg=6)
    ob = box("hide", (0.7, 0.015, 1.2), (0, 0.05, 1.02), "e_hide")
    for k in range(10):
        a = 2 * math.pi * k / 10
        rod("lace%d" % k, (0.33 * math.cos(a), 0.05, 1.02 + 0.58 * math.sin(a)), (0.42 * math.cos(a) / max(abs(math.cos(a)), 0.01) * min(1, abs(math.cos(a)) * 3) if False else (0.42 if math.cos(a) > 0 else -0.42), 0.05, 1.02 + 0.6 * math.sin(a)), 0.004, "e_rope", seg=3)
    box("fur", (0.35, 0.3, 0.2), (0.3, -0.25, 0.1), "e_fur", (0, 0, 0.4), bevel=0.04)
    node("body")
    export("hide_rack")


def m_weapon_rack():
    box("base", (0.8, 0.3, 0.12), (0, 0.15, 0.06), "e_wood_dark")
    box("top", (0.8, 0.3, 0.06), (0, 0.25, 1.4), "e_wood_dark")
    for x in (-0.38, 0.38):
        box("side%d" % int(x * 10), (0.05, 0.08, 1.4), (x, 0.38, 0.7), "e_wood_dark")
    for k, x in enumerate((-0.25, -0.08, 0.09)):
        rod("spear%d" % k, (x, 0.18, 0.1), (x + 0.02, 0.28, 1.9), 0.018, "e_wood", seg=5)
        cyl("tip%d" % k, 0.035, 0.18, (x + 0.02, 0.29, 1.98), "e_iron", seg=4, r2=0.0)
    rod("bow", (0.25, 0.3, 0.2), (0.25, 0.3, 1.5), 0.015, "e_wood_dark", seg=4)
    for k in range(6):
        a0, a1 = -0.9 + k * 0.3, -0.9 + (k + 1) * 0.3
        rod("limb%d" % k, (0.25 + 0.0, 0.3 - 0.16 * math.cos(a0), 0.85 + 0.65 * math.sin(a0)), (0.25, 0.3 - 0.16 * math.cos(a1), 0.85 + 0.65 * math.sin(a1)), 0.02, "e_wood", seg=4)
    cyl("quiver", 0.07, 0.5, (0.32, 0.05, 0.37), "e_hide", seg=8)
    for k in range(4):
        rod("arrow%d" % k, (0.30 + k * 0.012, 0.05, 0.55), (0.28 + k * 0.02, 0.03, 0.8), 0.006, "e_wood", seg=3)
    node("body")
    export("weapon_rack")


# ------------------------------------------------------------------ forge
def m_forge_hearth():
    # 3 wide (x -1..1), back row; stone hearth, glowing coals, hood + chimney, bellows on the left
    box("hearth", (1.8, 1.2, 0.8), (0, 0.4, 0.4), "e_stone", bevel=0.02)
    box("bed", (1.4, 0.85, 0.06), (0, 0.4, 0.82), "e_rock")
    node("body0")
    box("coals", (1.1, 0.6, 0.06), (0, 0.4, 0.86), "e_embers")
    for k in range(5):
        box("coal%d" % k, (0.12, 0.1, 0.06), (-0.4 + k * 0.2, 0.3 + (k % 2) * 0.15, 0.9), "e_embers", (0, 0, k))
    cyl("flame", 0.15, 0.3, (0, 0.45, 1.02), "e_fire", seg=6, r2=0.0)
    node("fire")
    for x in (-0.85, 0.85):
        box("pill%d" % int(x * 10), (0.15, 0.15, 1.0), (x, 0.9, 1.3), "e_stone")
    bm = bmesh.new()
    bmesh.ops.create_cone(bm, cap_ends=False, segments=4, radius1=1.15, radius2=0.42, depth=0.9)
    _obj("hood", bm, "e_stone", (0, 0.45, 2.15), (0, 0, math.radians(45)))
    box("chimney", (0.55, 0.55, 1.0), (0, 0.45, 3.0), "e_stone")
    box("cap", (0.7, 0.7, 0.08), (0, 0.45, 3.5), "e_rock")
    # bellows
    box("bel_top", (0.5, 0.75, 0.04), (-1.25, 0.25, 0.75), "e_wood", (0, math.radians(6), 0))
    box("bel_bot", (0.5, 0.75, 0.04), (-1.25, 0.25, 0.5), "e_wood")
    box("bel_leather", (0.44, 0.7, 0.22), (-1.25, 0.25, 0.62), "e_hide")
    rod("nozzle", (-1.0, 0.25, 0.6), (-0.85, 0.25, 0.65), 0.03, "e_iron", seg=6)
    rod("handle", (-1.25, -0.15, 0.78), (-1.25, -0.45, 0.85), 0.02, "e_wood_dark", seg=4)
    for k in range(3):
        rod("tong%d" % k, (0.75, 0.0 + k * 0.08, 0.84), (0.25, 0.1 + k * 0.08, 0.95), 0.012, "e_iron", seg=4)
    node("body")
    export("forge_hearth")


def m_anvil_stump():
    cyl("stump", 0.3, 0.5, (0, 0, 0.25), "e_bark", seg=10)
    cyl("stumptop", 0.28, 0.01, (0, 0, 0.505), "e_wood", seg=10)
    box("foot", (0.36, 0.22, 0.08), (0, 0, 0.55), "e_iron")
    box("waist", (0.18, 0.14, 0.12), (0, 0, 0.65), "e_iron")
    box("face", (0.5, 0.2, 0.1), (0, 0, 0.76), "e_iron", bevel=0.01)
    cyl("horn", 0.09, 0.28, (0.37, 0, 0.77), "e_iron", (0, math.radians(90), 0), seg=8, r2=0.01)
    rod("hammer_h", (-0.15, -0.05, 0.82), (0.15, -0.25, 0.83), 0.015, "e_wood_dark", seg=4)
    box("hammer", (0.06, 0.06, 0.12), (-0.17, -0.04, 0.85), "e_iron", (0, math.radians(90), 0.6))
    node("body")
    export("anvil_stump")


def m_quench_barrel():
    barrel("b", (0, 0, 0), 0.3, 0.7)
    cyl("water", 0.27, 0.01, (0, 0, 0.65), "e_water", seg=14)
    for k in range(2):
        rod("tong%d" % k, (0.05 + k * 0.05, 0.0, 0.5), (0.15 + k * 0.05, -0.1, 1.0), 0.012, "e_iron", seg=4)
    node("body")
    export("quench_barrel")


def m_tool_wall():
    box("board", (1.8, 0.04, 0.9), (0.5, 0.47, 1.4), "e_wood_dark")
    tools = [(-0.2, "hammer"), (0.1, "tongs"), (0.4, "hammer"), (0.7, "saw"), (1.0, "tongs"), (1.25, "file")]
    for k, (x, t) in enumerate(tools):
        cyl("peg%d" % k, 0.015, 0.08, (x, 0.42, 1.75), "e_wood", (math.radians(90), 0, 0), seg=5)
        if t == "hammer":
            rod("h%d" % k, (x, 0.4, 1.72), (x, 0.4, 1.2), 0.016, "e_wood", seg=4); box("hh%d" % k, (0.16, 0.06, 0.06), (x, 0.4, 1.19), "e_iron")
        elif t == "tongs":
            rod("t%da" % k, (x, 0.4, 1.72), (x - 0.04, 0.4, 1.15), 0.01, "e_iron", seg=4); rod("t%db" % k, (x, 0.4, 1.72), (x + 0.04, 0.4, 1.15), 0.01, "e_iron", seg=4)
        elif t == "saw":
            box("s%d" % k, (0.12, 0.01, 0.45), (x, 0.4, 1.45), "e_iron"); box("sh%d" % k, (0.12, 0.04, 0.1), (x, 0.4, 1.72), "e_wood")
        else:
            rod("f%d" % k, (x, 0.4, 1.72), (x, 0.4, 1.3), 0.012, "e_iron", seg=4)
    node("body")
    export("tool_wall")


# ------------------------------------------------------------------ temple
def m_idol():
    # carved log god, 4 tall: three stacked faces, carved belt, cap
    cyl("log", 0.3, 3.6, (0, 0, 1.8), "e_bark", seg=10)
    cyl("cap", 0.36, 0.25, (0, 0, 3.72), "e_wood_dark", seg=10, r2=0.1)
    for k, z in enumerate((3.15, 2.25, 1.35)):
        box("brow%d" % k, (0.5, 0.14, 0.09), (0, -0.28, z + 0.14), "e_wood_dark", bevel=0.02)
        for s in (1, -1):
            box("eye%d%d" % (k, s), (0.13, 0.06, 0.09), (s * 0.13, -0.285, z + 0.03), "e_glow" if k == 0 else "e_coal")
            box("cheek%d%d" % (k, s), (0.1, 0.08, 0.14), (s * 0.2, -0.27, z - 0.12), "e_wood_dark", bevel=0.02)
        box("nose%d" % k, (0.11, 0.16, 0.26), (0, -0.34, z - 0.08), "e_wood_dark", bevel=0.02)
        box("mouth%d" % k, (0.3, 0.07, 0.07), (0, -0.285, z - 0.27), "e_coal")
        box("chin%d" % k, (0.26, 0.1, 0.08), (0, -0.28, z - 0.36), "e_wood_dark", bevel=0.02)
        torus("band%d" % k, 0.305, 0.03, (0, 0, z - 0.42), "e_wood_dark", seg=10, seg2=4)
    for s in (1, -1):
        rod("arm%d" % s, (s * 0.28, -0.1, 1.95), (s * 0.12, -0.3, 1.7), 0.06, "e_wood_dark", seg=6)
    cyl("stone", 0.45, 0.2, (0, 0, 0.1), "e_rock", seg=8)
    for k in range(5):
        cyl("cl%d" % k, 0.02, 0.5, (0.25 * math.cos(k), -0.31, 0.9 + 0.08 * k), "e_cloth", seg=4)
    node("body")
    export("idol")


def m_altar_stone():
    for x in (-0.25, 1.25):
        box("leg%d" % int(x * 10), (0.4, 0.55, 0.55), (x, 0, 0.275), "e_rock", bevel=0.04)
    box("slab", (2.0, 0.85, 0.22), (0.5, 0, 0.66), "e_rock", bevel=0.03)
    box("cloth", (0.6, 0.87, 0.012), (0.5, 0, 0.775), "e_cloth")
    box("clothf", (0.6, 0.012, 0.35), (0.5, -0.43, 0.6), "e_cloth")
    for k, x in enumerate((-0.2, 0.1, 0.9, 1.2)):
        cyl("cnd%d" % k, 0.04, 0.12 + 0.06 * (k % 2), (x, 0.2, 0.83 + 0.03 * (k % 2)), "e_candle", seg=8)
    cyl("bowl", 0.16, 0.08, (0.5, 0.1, 0.81), "e_gold", seg=12, r2=0.1)
    for k in range(5):
        cyl("fruit%d" % k, 0.04, 0.06, (0.45 + 0.04 * k, 0.08 + 0.02 * (k % 2), 0.87), "e_produce", seg=6)
    cyl("skullb", 0.08, 0.13, (1.05, -0.15, 0.84), "e_bone", seg=8, r2=0.06)
    node("body")
    for k, x in enumerate((-0.2, 0.1, 0.9, 1.2)):
        cyl("fl%d" % k, 0.018, 0.05, (x, 0.2, 0.92 + 0.06 * (k % 2)), "e_flame", seg=5, r2=0.0)
    node("lamp_on")
    export("altar_stone")


def m_fire_bowl():
    for k in range(3):
        a = 2 * math.pi * k / 3
        rod("leg%d" % k, (0.35 * math.cos(a), 0.35 * math.sin(a), 0), (0.12 * math.cos(a), 0.12 * math.sin(a), 0.8), 0.025, "e_iron", seg=5)
    cyl("bowl", 0.34, 0.2, (0, 0, 0.9), "e_iron", seg=12, r2=0.18)
    torus("rim", 0.34, 0.02, (0, 0, 1.0), "e_iron", seg=12, seg2=4)
    box("coals", (0.4, 0.4, 0.04), (0, 0, 0.99), "e_embers", (0, 0, 0.3))
    node("body")
    for k in range(3):
        cyl("fl%d" % k, 0.1 - k * 0.02, 0.16 + k * 0.05, (0.07 * math.cos(k * 2), 0.07 * math.sin(k * 2), 1.08 + k * 0.03), "e_fire", (0, 0, k), seg=5, r2=0.0)
    node("lamp_on")
    export("fire_bowl")


def m_sacred_poles():
    # crossed poles with cloth strips (like the taiga sanctuaries)
    rod("p1", (-0.45, 0.05, 0), (0.35, 0.0, 2.7), 0.06, "e_bark", seg=6)
    rod("p2", (0.45, -0.05, 0), (-0.35, 0.0, 2.7), 0.06, "e_bark", seg=6)
    torus("tie", 0.08, 0.025, (0.08, 0.0, 1.75), "e_rope", (math.radians(90), 0, 0), seg=8, seg2=4)
    for k in range(6):
        x = -0.25 + k * 0.1
        box("strip%d" % k, (0.07, 0.01, 0.5 + 0.1 * (k % 3)), (x, -0.07, 1.4 - 0.05 * (k % 3)), "e_cloth" if k % 2 else "e_canvas", (0, 0.05 * k, 0))
    cyl("skull", 0.08, 0.12, (0.0, -0.08, 2.0), "e_bone", (math.radians(90), 0, 0), seg=8, r2=0.05)
    node("body")
    export("sacred_poles")


# ------------------------------------------------------------------ furniture
def m_table_wood():
    box("top", (1.85, 0.85, 0.07), (0.5, 0, 0.78), "e_wood", bevel=0.01)
    for x in (-0.33, 1.33):
        for y in (-0.33, 0.33):
            box("leg%d%d" % (int(x * 10), int(y * 10)), (0.08, 0.08, 0.75), (x, y, 0.375), "e_wood_dark")
    box("str", (1.6, 0.05, 0.05), (0.5, 0, 0.2), "e_wood_dark")
    cyl("mug", 0.045, 0.1, (0.1, -0.1, 0.87), "e_wood_dark", seg=8)
    cyl("plate", 0.12, 0.015, (0.7, 0.1, 0.825), "e_bone", seg=12)
    cyl("candle", 0.03, 0.12, (1.1, 0.05, 0.88), "e_candle", seg=6)
    node("body")
    cyl("fl", 0.015, 0.04, (1.1, 0.05, 0.96), "e_flame", seg=5, r2=0.0)
    node("lamp_on")
    export("table_wood")


def m_chair_wood():
    box("seat", (0.45, 0.45, 0.05), (0, 0, 0.46), "e_wood")
    for x in (-0.19, 0.19):
        for y in (-0.19, 0.19):
            box("l%d%d" % (int(x * 100), int(y * 100)), (0.05, 0.05, 0.45 if y < 0 else 1.0), (x, y, 0.225 if y < 0 else 0.5), "e_wood_dark")
    for z in (0.75, 0.92):
        box("rail%d" % int(z * 100), (0.4, 0.04, 0.06), (0, 0.19, z), "e_wood")
    node("body")
    export("chair_wood")


def m_stool():
    cyl("seat", 0.2, 0.06, (0, 0, 0.62), "e_wood", seg=12)
    for k in range(3):
        a = 2 * math.pi * k / 3
        rod("l%d" % k, (0.16 * math.cos(a), 0.16 * math.sin(a), 0), (0.1 * math.cos(a), 0.1 * math.sin(a), 0.6), 0.025, "e_wood_dark", seg=5)
    torus("ring", 0.13, 0.012, (0, 0, 0.25), "e_wood_dark", seg=10, seg2=4)
    node("body")
    export("stool")


def m_bed_wood():
    # 1 wide, 2 long (back row): headboard at the back (+Y)
    box("frame", (0.9, 1.9, 0.25), (0, 0.5, 0.25), "e_wood_dark")
    box("mattress", (0.82, 1.82, 0.16), (0, 0.5, 0.45), "e_canvas", bevel=0.03)
    box("blanket", (0.86, 1.2, 0.05), (0, 0.18, 0.54), "e_cloth", bevel=0.02)
    box("pillow", (0.6, 0.32, 0.12), (0, 1.22, 0.58), "e_canvas", bevel=0.04)
    box("head", (0.95, 0.08, 0.95), (0, 1.43, 0.48), "e_wood_dark")
    box("foot", (0.95, 0.08, 0.6), (0, -0.43, 0.3), "e_wood_dark")
    box("pelt", (0.7, 0.5, 0.03), (0.05, -0.15, 0.57), "e_fur", (0, 0, 0.2))
    node("body")
    export("bed_wood")


def m_wardrobe():
    box("body", (0.9, 0.55, 1.9), (0, 0.18, 0.95), "e_wood_dark", bevel=0.02)
    box("crown", (0.98, 0.62, 0.08), (0, 0.18, 1.92), "e_wood")
    for s in (-1, 1):
        box("door%d" % s, (0.42, 0.03, 1.55), (s * 0.22, -0.11, 1.0), "e_wood")
        cyl("knob%d" % s, 0.02, 0.04, (s * 0.04, -0.13, 1.0), "e_gold", (math.radians(90), 0, 0), seg=6)
    box("drawer", (0.84, 0.03, 0.18), (0, -0.11, 0.13), "e_wood")
    node("body")
    export("wardrobe")


def m_bar_counter():
    # 3 wide (x -1..1); customers stand at the front (-Y)
    box("front", (2.9, 0.12, 1.0), (0, -0.25, 0.5), "e_wood_dark")
    for k in range(6):
        box("panel%d" % k, (0.4, 0.02, 0.7), (-1.2 + k * 0.48, -0.32, 0.5), "e_wood")
    box("top", (3.0, 0.62, 0.07), (0, -0.05, 1.04), "e_wood", bevel=0.01)
    box("shelf", (2.9, 0.45, 0.05), (0, 0.15, 0.55), "e_wood_dark")
    rod("rail", (-1.45, -0.42, 0.15), (1.45, -0.42, 0.15), 0.025, "e_gold", seg=6)
    for k, x in enumerate((-0.9, -0.5)):
        cyl("tapb%d" % k, 0.05, 0.25, (x, 0.05, 1.2), "e_gold", seg=8)
        rod("tap%d" % k, (x, 0.05, 1.3), (x, -0.12, 1.25), 0.015, "e_gold", seg=4)
    for k in range(5):
        cyl("mug%d" % k, 0.045, 0.11, (0.1 + k * 0.25, -0.12 + 0.05 * (k % 2), 1.13), "e_wood_dark" if k % 2 else "glass", seg=8)
    for k in range(4):
        cyl("bot%d" % k, 0.035, 0.22, (-1.2 + k * 0.12, 0.2, 0.69), ["e_gold", "glass", "e_red", "glass"][k], seg=6)
    node("body")
    export("bar_counter")


def m_keg_rack():
    box("cradle", (1.8, 0.7, 0.08), (0.5, 0.05, 0.3), "e_wood_dark")
    for x in (-0.3, 1.3):
        box("leg%d" % int(x * 10), (0.1, 0.7, 0.3), (x, 0.05, 0.15), "e_wood_dark")
    for k, x in enumerate((-0.05, 0.5, 1.05)):
        cyl("keg%d" % k, 0.27, 0.6, (x, 0.05, 0.62), "e_wood", (math.radians(90), 0, 0), seg=12)
        for y in (-0.15, 0.15):
            torus("hp%d%d" % (k, int(y * 10)), 0.275, 0.012, (x, 0.05 + y, 0.62), "e_iron", (math.radians(90), 0, 0), seg=12, seg2=4)
        rod("tap%d" % k, (x, -0.26, 0.55), (x, -0.36, 0.55), 0.02, "e_gold", seg=4)
    cyl("keg_top", 0.24, 0.5, (0.22, 0.05, 1.15), "e_wood", (math.radians(90), 0, 0), seg=12)
    node("body")
    export("keg_rack")


def m_fireplace():
    # 3 wide (x -1..1), against a wall at +Y
    box("back", (2.2, 0.5, 1.3), (0, 0.25, 0.65), "e_stone")
    box("mantel", (2.4, 0.62, 0.12), (0, 0.2, 1.36), "e_wood_dark")
    box("chimney", (1.4, 0.45, 1.6), (0, 0.28, 2.2), "e_stone")
    box("hole", (1.1, 0.05, 0.8), (0, 0.0, 0.5), "e_coal")
    for k, x in enumerate((-0.25, 0.0, 0.25)):
        rod("log%d" % k, (x - 0.3, -0.05, 0.12), (x + 0.3, 0.1, 0.16), 0.06, "e_bark", seg=6)
    box("cand1", (0.07, 0.07, 0.15), (-0.9, 0.2, 1.5), "e_candle")
    box("jar", (0.1, 0.1, 0.16), (0.8, 0.2, 1.5), "e_gold")
    node("body")
    box("embers", (0.8, 0.3, 0.05), (0, 0.0, 0.1), "e_embers")
    for k in range(3):
        cyl("fl%d" % k, 0.12, 0.35 + 0.1 * k, (-0.2 + 0.2 * k, 0.03, 0.3), "e_fire", (0, 0, k), seg=5, r2=0.0)
    node("lamp_on")
    export("fireplace")


def m_shop_counter():
    box("body", (1.85, 0.7, 0.95), (0.5, 0, 0.475), "e_wood_dark", bevel=0.01)
    box("top", (1.95, 0.78, 0.06), (0.5, 0, 0.98), "e_wood")
    for k in range(3):
        box("pan%d" % k, (0.5, 0.02, 0.6), (-0.1 + k * 0.6, -0.36, 0.48), "e_wood")
    # scales
    cyl("sbase", 0.08, 0.03, (0.0, 0.05, 1.025), "e_gold", seg=10)
    rod("spost", (0.0, 0.05, 1.03), (0.0, 0.05, 1.35), 0.012, "e_gold", seg=5)
    rod("sbeam", (-0.2, 0.05, 1.35), (0.2, 0.05, 1.35), 0.008, "e_gold", seg=4)
    for s in (1, -1):
        cyl("span%d" % s, 0.08, 0.015, (s * 0.2, 0.05, 1.15), "e_gold", seg=10)
        rod("sch%d" % s, (s * 0.2, 0.05, 1.35), (s * 0.2, 0.05, 1.16), 0.004, "e_gold", seg=3)
    for k in range(6):
        cyl("coin%d" % k, 0.03, 0.006, (0.5 + 0.035 * k, -0.1 + 0.01 * k, 1.015 + 0.006 * k), "e_gold", seg=8)
    box("ledger", (0.3, 0.22, 0.04), (1.0, -0.05, 1.03), "e_hide")
    cyl("jar1", 0.07, 0.2, (1.3, 0.15, 1.11), "glass", seg=8)
    cyl("candy", 0.06, 0.12, (1.3, 0.15, 1.08), "e_produce", seg=8)
    node("body")
    export("shop_counter")


def m_shop_shelf():
    # 2 wide (x 0..1), 2 tall, against a wall at +Y
    box("back", (1.9, 0.04, 1.95), (0.5, 0.45, 0.98), "e_wood_dark")
    for x in (-0.43, 1.43):
        box("side%d" % int(x * 10), (0.05, 0.45, 1.95), (x, 0.24, 0.98), "e_wood_dark")
    r = random.Random(21)
    for j, z in enumerate((0.05, 0.55, 1.05, 1.55)):
        box("sh%d" % j, (1.86, 0.44, 0.04), (0.5, 0.24, z), "e_wood")
        x = -0.32
        while x < 1.3:
            t = r.randint(0, 4)
            if t == 0:
                cyl("j%d_%d" % (j, int(x * 100)), 0.06, 0.18, (x, 0.22, z + 0.11), "glass", seg=8); cyl("jc%d_%d" % (j, int(x * 100)), 0.05, 0.12, (x, 0.22, z + 0.08), "e_produce", seg=8); x += 0.16
            elif t == 1:
                box("bag%d_%d" % (j, int(x * 100)), (0.2, 0.18, 0.24), (x + 0.04, 0.22, z + 0.14), "e_canvas", (0, 0, r.random()), bevel=0.04); x += 0.26
            elif t == 2:
                cyl("bt%d_%d" % (j, int(x * 100)), 0.035, 0.24, (x, 0.2, z + 0.14), ["e_red", "glass", "e_gold"][r.randint(0, 2)], seg=6); x += 0.1
            elif t == 3:
                box("box%d_%d" % (j, int(x * 100)), (0.22, 0.25, 0.16), (x + 0.05, 0.22, z + 0.1), "e_wood", (0, 0, r.uniform(-0.2, 0.2))); x += 0.28
            else:
                cyl("roll%d_%d" % (j, int(x * 100)), 0.07, 0.3, (x + 0.08, 0.2, z + 0.09), "e_cloth", (0, math.radians(90), 0), seg=8); x += 0.34
    node("body")
    export("shop_shelf")


def m_rug():
    box("rug", (1.9, 1.9, 0.015), (0.5, 0.5, 0.008), "e_rug")
    ob = PARTS[-1]
    fit_face(ob, 2, 1, -0.45, 1.45, -0.45, 1.45)
    for s in (-1, 1):
        for k in range(10):
            box("fr%d%d" % (s, k), (0.02, 0.08, 0.006), (-0.4 + k * 0.2, 0.5 + s * 1.0, 0.005), "e_gold")
    node("body")
    export("rug")


def m_cooking_pot():
    for k in range(3):
        a = 2 * math.pi * k / 3
        rod("leg%d" % k, (0.4 * math.cos(a), 0.4 * math.sin(a), 0), (0, 0, 1.2), 0.025, "e_wood_dark", seg=5)
    rod("chain", (0, 0, 1.2), (0, 0, 0.75), 0.01, "e_iron", seg=4)
    cyl("pot", 0.24, 0.3, (0, 0, 0.55), "e_iron", seg=12, r2=0.2)
    cyl("stew", 0.2, 0.01, (0, 0, 0.7), "e_produce", seg=12)
    for k in range(6):
        a = 2 * math.pi * k / 6
        box("stone%d" % k, (0.14, 0.12, 0.1), (0.3 * math.cos(a), 0.3 * math.sin(a), 0.05), "e_rock", (0, 0, a))
    node("body")
    box("embers", (0.35, 0.35, 0.05), (0, 0, 0.06), "e_embers", (0, 0, 0.4))
    cyl("fl", 0.12, 0.25, (0, 0, 0.2), "e_fire", seg=5, r2=0.0)
    node("lamp_on")
    export("cooking_pot")


def m_wanted_wall():
    for k, (x, z, t, tilt) in enumerate(((-0.18, 1.35, "wanted2", 0.08), (0.2, 1.25, "wanted1", -0.1))):
        ob = box("p%d" % k, (0.34, 0.008, 0.47), (x, 0.49, z), t, (0, tilt, 0))
        mn = min(v.co.x for v in ob.data.vertices); mx = max(v.co.x for v in ob.data.vertices)
        zn = min(v.co.z for v in ob.data.vertices); zx = max(v.co.z for v in ob.data.vertices)
        fit_face(ob, 1, -1, mn, mx, zn, zx)
        cyl("n%d" % k, 0.012, 0.02, (x, 0.485, z + 0.2), "e_iron", (math.radians(90), 0, 0), seg=5)
    node("body")
    export("wanted_wall")


def m_crate_stack():
    box("c1", (0.7, 0.7, 0.55), (-0.05, 0.05, 0.275), "e_wood", bevel=0.01)
    box("c2", (0.55, 0.55, 0.45), (0.05, 0.0, 0.775), "e_wood_dark", (0, 0, 0.3), bevel=0.01)
    for k in range(4):
        box("sl%d" % k, (0.72, 0.03, 0.06), (-0.05, -0.31, 0.08 + k * 0.14), "e_wood_dark")
    cyl("sack", 0.18, 0.35, (0.3, -0.3, 0.17), "e_canvas", seg=8, r2=0.1)
    node("body")
    export("crate_stack")






# ====================================================================================== Elisar v2: lanterns + interiors
def _lantern(tag, cx, cy, z0, s=1.0):
    """Tapered iron box lantern (ref: village wrought-iron lanterns) standing at z0; returns the top z."""
    w, h = 0.24 * s, 0.30 * s
    box(tag + "base", (w + 0.04 * s, w + 0.04 * s, 0.035 * s), (cx, cy, z0 + 0.0175 * s), "e_iron")
    box(tag + "glass", (w - 0.03 * s, w - 0.03 * s, h), (cx, cy, z0 + 0.035 * s + h / 2), "e_lamp")
    for dx in (-1, 1):
        for dy in (-1, 1):
            box(tag + "p%d%d" % (dx, dy), (0.025 * s, 0.025 * s, h), (cx + dx * w / 2, cy + dy * w / 2, z0 + 0.035 * s + h / 2), "e_iron")
    zt = z0 + 0.035 * s + h
    box(tag + "rim", (w + 0.05 * s, w + 0.05 * s, 0.03 * s), (cx, cy, zt + 0.015 * s), "e_iron")
    cyl(tag + "cap", (w + 0.06 * s) * 0.72, 0.12 * s, (cx, cy, zt + 0.09 * s), "e_iron", seg=4, r2=0.03 * s, rot=(0, 0, math.radians(45)))
    torus(tag + "ring", 0.035 * s, 0.009 * s, (cx, cy, zt + 0.18 * s), "e_iron", (math.radians(90), 0, 0), seg=8, seg2=3)
    cyl(tag + "flame", 0.025 * s, 0.08 * s, (cx, cy, z0 + 0.1 * s), "e_flame", seg=5, r2=0.0)
    return zt + 0.21 * s


def _chain(tag, cx, cy, z1, z0, step=0.045):
    k, z = 0, z1
    while z > z0:
        torus(tag + "c%d" % k, 0.026, 0.007, (cx, cy, z), "e_iron", (0, math.radians(90) if k % 2 else 0, 0), seg=6, seg2=3)
        z -= step
        k += 1


def m_lantern_hang():
    cyl("plate", 0.07, 0.025, (0, 0, 0.99), "e_iron", seg=6)
    top = 0.32 + 0.035 + 0.30 + 0.21
    _chain("ch", 0, 0, 0.965, top - 0.02)
    _lantern("l", 0, 0, 0.32)
    node("body")
    export("lantern_hang")


def m_lantern_post():
    box("post", (0.2, 0.2, 1.28), (0, 0, 0.64), "e_sizo", bevel=0.02)
    box("foot", (0.3, 0.3, 0.1), (0, 0, 0.05), "e_rock")
    box("capb", (0.28, 0.28, 0.05), (0, 0, 1.305), "e_wood_dark")
    _lantern("l", 0, 0, 1.33, 1.05)
    node("body")
    export("lantern_post")


def m_lantern_wall():
    box("plate", (0.16, 0.035, 0.3), (0, 0.48, 0.85), "e_iron")
    rod("arm", (0, 0.47, 0.92), (0, 0.02, 0.92), 0.018, "e_iron", seg=6)
    rod("brace", (0, 0.47, 0.74), (0, 0.18, 0.92), 0.013, "e_iron", seg=5)
    torus("curl", 0.05, 0.009, (0, 0.3, 0.98), "e_iron", (0, math.radians(90), 0), seg=10, seg2=3)
    _chain("ch", 0, 0.02, 0.9, 0.78)
    _lantern("l", 0, 0.02, 0.36, 0.95)
    node("body")
    export("lantern_wall")


def _painting(name, matname, w, h, cx=0.0):
    zc = 1.45
    box("back", (w + 0.08, 0.04, h + 0.08), (cx, 0.47, zc), "e_wood_dark")
    ob = box("canvas", (w, 0.03, h), (cx, 0.445, zc), matname)
    fit_face(ob, 1, -1, cx - w / 2, cx + w / 2, zc - h / 2, zc + h / 2)
    for sz in (-1, 1):
        box("fh%d" % sz, (w + 0.1, 0.05, 0.05), (cx, 0.44, zc + sz * (h / 2 + 0.03)), "e_wood")
        box("fv%d" % sz, (0.05, 0.05, h + 0.1), (cx + sz * (w / 2 + 0.03), 0.44, zc), "e_wood")
    rod("nail", (cx, 0.47, zc + h / 2 + 0.2), (cx, 0.43, zc + h / 2 + 0.2), 0.01, "e_iron", seg=4)
    rod("wl", (cx, 0.43, zc + h / 2 + 0.2), (cx - w * 0.35, 0.44, zc + h / 2 + 0.03), 0.004, "e_rope", seg=3)
    rod("wr", (cx, 0.43, zc + h / 2 + 0.2), (cx + w * 0.35, 0.44, zc + h / 2 + 0.03), 0.004, "e_rope", seg=3)
    node("body")
    export(name)


def m_painting_islands(): _painting("painting_islands", "e_paint_islands", 0.78, 0.62)
def m_painting_portrait(): _painting("painting_portrait", "e_paint_portrait", 0.56, 0.7)
def m_painting_eye(): _painting("painting_eye", "e_paint_eye", 1.7, 0.82, cx=0.5)


def m_kitchen_stove():
    # 1x1 masonry stove with an iron hob, pot and kettle; flue pipe through the cell above
    box("body", (0.94, 0.86, 0.78), (0, 0.05, 0.39), "e_stone", bevel=0.02)
    box("hob", (0.96, 0.88, 0.05), (0, 0.05, 0.805), "e_iron")
    box("mouth", (0.42, 0.04, 0.3), (0, -0.385, 0.32), "e_coal")
    box("door", (0.46, 0.03, 0.06), (0, -0.4, 0.5), "e_iron")
    cyl("fire", 0.13, 0.12, (0, -0.25, 0.25), "e_fire", seg=6, r2=0.02)
    box("embers", (0.36, 0.2, 0.05), (0, -0.25, 0.2), "e_embers")
    cyl("pot", 0.17, 0.2, (-0.2, 0.0, 0.93), "e_iron", seg=12)
    torus("potlip", 0.17, 0.015, (-0.2, 0.0, 1.03), "e_iron", seg=12, seg2=4)
    cyl("stew", 0.155, 0.01, (-0.2, 0.0, 1.0), "e_produce", seg=12)
    cyl("kettle", 0.1, 0.16, (0.24, -0.05, 0.91), "e_ceramic", seg=10, r2=0.07)
    rod("spout", (0.32, -0.05, 0.92), (0.4, -0.05, 0.98), 0.015, "e_ceramic", seg=5)
    cyl("flue", 0.09, 1.2, (0.25, 0.3, 1.43), "e_iron", seg=8)
    box("hook", (0.6, 0.03, 0.03), (0, 0.35, 1.3), "e_iron")
    for k, x in enumerate((-0.2, 0.0, 0.2)):
        rod("ut%d" % k, (x, 0.34, 1.29), (x, 0.32, 1.08), 0.01, "e_iron", seg=4)
        cyl("utl%d" % k, 0.04, 0.02, (x, 0.32, 1.07), "e_iron", seg=6)
    node("body")
    export("kitchen_stove")


def m_kitchen_counter():
    box("top", (0.98, 0.7, 0.06), (0, 0.12, 0.88), "e_wood")
    box("cab", (0.92, 0.62, 0.82), (0, 0.14, 0.42), "e_sizo")
    for x in (-0.22, 0.22):
        box("dr%d" % int(x * 100), (0.4, 0.02, 0.6), (x, -0.18, 0.44), "e_wood")
        cyl("kn%d" % int(x * 100), 0.02, 0.03, (x * 0.4, -0.2, 0.55), "e_iron", (math.radians(90), 0, 0), seg=6)
    # (Rev 4 item 14: dishes/food are a separate block `kitchen_set`)
    node("body")
    export("kitchen_counter")


def m_dish_cupboard():
    # 1x1x2 open dresser with plates, cups and jugs (ref: cottage kitchen shelves)
    box("back", (0.92, 0.04, 1.9), (0, 0.44, 0.95), "e_wood")
    for x in (-0.45, 0.45):
        box("side%d" % int(x * 10), (0.05, 0.42, 1.9), (x, 0.25, 0.95), "e_wood_dark")
    box("base", (0.92, 0.42, 0.7), (0, 0.25, 0.35), "e_sizo")
    box("dr", (0.8, 0.02, 0.5), (0, 0.035, 0.38), "e_wood")
    for z in (0.72, 1.18, 1.6, 1.9):
        box("sh%d" % int(z * 10), (0.9, 0.36, 0.035), (0, 0.27, z), "e_wood_dark")
    for k in range(4):
        x = -0.3 + k * 0.2
        cyl("pl%d" % k, 0.085, 0.012, (x, 0.37, 0.83), "e_plate", (math.radians(78), 0, 0), seg=12)
    box("rail1", (0.88, 0.015, 0.02), (0, 0.3, 0.8), "e_wood_dark")
    for k in range(3):
        x = -0.28 + k * 0.28
        cyl("cup%d" % k, 0.045, 0.08, (x, 0.25, 1.24), "e_plate", seg=8)
        torus("ch%d" % k, 0.025, 0.006, (x + 0.05, 0.25, 1.24), "e_plate", (math.radians(90), 0, 0), seg=6, seg2=3)
    cyl("jug", 0.08, 0.22, (-0.24, 0.27, 1.73), "e_ceramic", seg=10, r2=0.05)
    cyl("jug2", 0.07, 0.18, (0.05, 0.27, 1.71), "e_ceramic", seg=10, r2=0.055)
    cyl("bowl2", 0.1, 0.05, (0.27, 0.27, 1.645), "e_plate", seg=10, r2=0.07)
    node("body")
    export("dish_cupboard")


def m_bookshelf_e():
    box("back", (0.94, 0.04, 1.9), (0, 0.44, 0.95), "e_wood_dark")
    for x in (-0.46, 0.46):
        box("side%d" % int(x * 10), (0.04, 0.4, 1.9), (x, 0.26, 0.95), "e_wood")
    r = random.Random(77)
    for z in (0.04, 0.5, 0.96, 1.42, 1.88):
        box("sh%d" % int(z * 10), (0.9, 0.38, 0.04), (0, 0.27, z), "e_wood")
    for z in (0.06, 0.52, 0.98, 1.44):
        x = -0.42
        k = 0
        while x < 0.4:
            w = r.uniform(0.05, 0.09)
            h = r.uniform(0.28, 0.4)
            if r.random() < 0.12 and x < 0.2:
                ob = box("bk%d_%d" % (int(z * 10), k), (w, 0.26, h), (x + 0.1, 0.27, z + w / 2 + 0.01), "e_books", (0, math.radians(84), 0))
                x += h + 0.02
            else:
                box("bk%d_%d" % (int(z * 10), k), (w, 0.26, h), (x + w / 2, 0.27, z + h / 2), "e_books")
                x += w + 0.005
            k += 1
    node("body")
    export("bookshelf_e")


def m_sacks():
    for k, (x, y, z, rz) in enumerate(((-0.2, 0.1, 0.0, 0.2), (0.18, 0.12, 0.0, -0.3), (0.0, 0.05, 0.42, 1.2))):
        cyl("s%d" % k, 0.2, 0.46, (x, y, z + 0.23), "e_canvas", seg=10, r2=0.17, rot=(0, 0, rz))
        cyl("t%d" % k, 0.07, 0.08, (x, y, z + 0.5), "e_canvas", seg=8, r2=0.03)
        torus("tie%d" % k, 0.06, 0.012, (x, y, z + 0.47), "e_rope", seg=8, seg2=3)
    box("spill", (0.3, 0.2, 0.02), (0.2, -0.3, 0.01), "e_produce")
    node("body")
    export("sacks")


def m_firewood_stack():
    # split logs stacked against the wall (+Y), bark sides + ring ends (ref: firewood against a stone wall)
    r = random.Random(5)
    rows = [(0.11, 4), (0.31, 4), (0.51, 3), (0.71, 3), (0.89, 2)]
    for ri, (z, n) in enumerate(rows):
        for k in range(n):
            x = -0.36 + (k + (0.5 if ri % 2 else 0)) * 0.24
            if abs(x) > 0.42:
                continue
            ob = cyl("lg%d_%d" % (ri, k), 0.11, 0.86, (x + r.uniform(-0.02, 0.02), 0.04, z), "e_bark", (math.radians(90), 0, 0), seg=7)
            fit_face(ob, 1, -1, x - 0.11, x + 0.11, z - 0.11, z + 0.11)
            fit_face(ob, 1, 1, x - 0.11, x + 0.11, z - 0.11, z + 0.11)
    for x in (-0.47, 0.47):
        box("st%d" % int(x * 10), (0.05, 0.05, 1.05), (x, -0.36, 0.52), "e_wood_dark")
        box("sb%d" % int(x * 10), (0.05, 0.05, 1.05), (x, 0.44, 0.52), "e_wood_dark")
    box("roof", (1.0, 0.95, 0.04), (0, 0.04, 1.08), "e_wood", (math.radians(-6), 0, 0))
    node("body")
    export("firewood_stack")


def m_fire_pit():
    # plaza fire pit: ring of pale stone blocks, logs and a fire (ref struct1 village)
    for k in range(10):
        a = 2 * math.pi * k / 10
        box("st%d" % k, (0.2, 0.17, 0.24), (math.cos(a) * 0.4, math.sin(a) * 0.4, 0.12), "e_stone", (0, 0, a), bevel=0.02)
    cyl("ash", 0.32, 0.03, (0, 0, 0.03), "e_coal", seg=10)
    box("embers", (0.36, 0.36, 0.04), (0, 0, 0.06), "e_embers", (0, 0, 0.6))
    for k in range(4):
        a = k * math.pi / 2 + 0.4
        rod("lg%d" % k, (math.cos(a) * 0.28, math.sin(a) * 0.28, 0.07), (math.cos(a) * 0.02, math.sin(a) * 0.02, 0.28), 0.045, "e_bark", seg=6)
    cyl("fire1", 0.16, 0.42, (0, 0, 0.3), "e_fire", seg=6, r2=0.0)
    cyl("fire2", 0.1, 0.32, (0.08, -0.05, 0.26), "e_fire", seg=5, r2=0.0, rot=(0, 0, 0.5))
    cyl("fire3", 0.09, 0.28, (-0.08, 0.06, 0.24), "e_fire", seg=5, r2=0.0)
    node("body")
    export("fire_pit")


def m_flower_pot_e():
    cyl("pot", 0.13, 0.22, (0, 0, 0.11), "e_ceramic", seg=10, r2=0.16)
    torus("lip", 0.16, 0.02, (0, 0, 0.22), "e_ceramic", seg=10, seg2=4)
    cyl("soil", 0.14, 0.01, (0, 0, 0.21), "e_coal", seg=10)
    for k in range(6):
        a = k * math.pi / 3
        ob = plane("lf%d" % k, 0.22, 0.34, (math.cos(a) * 0.05, math.sin(a) * 0.05, 0.38), "e_flowers", (math.radians(12), 0, a))
    node("body")
    export("flower_pot_e")


def m_herbs_hanging():
    rod("beam", (-0.45, 0, 0.96), (0.45, 0, 0.96), 0.025, "e_wood_dark", seg=6)
    r = random.Random(3)
    for k in range(5):
        x = -0.34 + k * 0.17
        rod("str%d" % k, (x, 0, 0.95), (x, 0, 0.8), 0.004, "e_rope", seg=3)
        for j in range(3):
            plane("h%d_%d" % (k, j), 0.12, 0.26, (x, 0, 0.66), "e_foliage" if k % 2 else "e_flowers", (math.radians(180), 0, j * math.pi / 3))
    node("body")
    export("herbs_hanging")


# ------------------------------------------------------------------ rev 3: castle «Чертог Тлеющего Рога»
def m_candle_stand():
    # wrought-iron floor pricket stand (refs: 18th c. hammered iron floor prickets): curled tripod, twisted square stem with
    # knops, scalloped drip pan, thick church candle with wax runs; the flame is the lamp_on node (off model = unlit)
    for k in range(3):
        a = math.radians(90 + k * 120)
        fx, fy = math.cos(a) * 0.32, math.sin(a) * 0.32
        rod("leg%d" % k, (0, 0, 0.24), (fx * 0.8, fy * 0.8, 0.05), 0.018, "e_iron", seg=5)
        rod("toe%d" % k, (fx * 0.8, fy * 0.8, 0.05), (fx, fy, 0.03), 0.016, "e_iron", seg=5)
        torus("curl%d" % k, 0.035, 0.011, (fx * 1.05, fy * 1.05, 0.05), "e_iron", (math.radians(90), 0, a + math.pi / 2), seg=8, seg2=3)
        cyl("pad%d" % k, 0.03, 0.02, (fx * 1.08, fy * 1.08, 0.01), "e_iron", seg=6)
    cyl("hub", 0.045, 0.08, (0, 0, 0.25), "e_iron", seg=8)
    n = 14
    for k in range(n):                                       # twisted square bar: each segment turned a little further
        z0 = 0.29 + k * (1.0 / n)
        box("tw%d" % k, (0.042, 0.042, 1.0 / n + 0.004), (0, 0, z0 + 0.5 / n), "e_iron", (0, 0, math.radians(k * 13)))
    for z in (0.62, 1.02):
        torus("knop%d" % int(z * 100), 0.04, 0.018, (0, 0, z), "e_iron", seg=10, seg2=5)
        cyl("kc%d" % int(z * 100), 0.03, 0.05, (0, 0, z), "e_iron", seg=8)
    cyl("cup", 0.05, 0.05, (0, 0, 1.31), "e_iron", seg=8, r2=0.08)
    cyl("pan", 0.17, 0.02, (0, 0, 1.345), "e_iron", seg=16)
    torus("rim", 0.17, 0.012, (0, 0, 1.36), "e_iron", seg=16, seg2=4)
    for k in range(6):
        a = k * math.pi / 3
        box("leaf%d" % k, (0.07, 0.03, 0.012), (math.cos(a) * 0.19, math.sin(a) * 0.19, 1.35), "e_iron", (0, math.radians(-20), a))
    cyl("puddle", 0.1, 0.012, (0, 0, 1.36), "c_wax", seg=10)
    rod("spike", (0, 0, 1.35), (0, 0, 1.4), 0.008, "e_iron", seg=4)
    cyl("candle", 0.062, 0.22, (0, 0, 1.47), "c_wax", seg=12)
    cyl("melt", 0.055, 0.015, (0, 0, 1.585), "c_wax", seg=12, r2=0.045)
    for k, (a, L) in enumerate(((0.3, 0.12), (2.2, 0.07), (4.1, 0.15), (5.3, 0.05))):
        cyl("drip%d" % k, 0.014, L, (math.cos(a) * 0.062, math.sin(a) * 0.062, 1.58 - L / 2), "c_wax", seg=5, r2=0.008)
    rod("wick", (0, 0, 1.58), (0, 0, 1.615), 0.005, "c_wick", seg=4)
    node("body")
    cyl("fl1", 0.026, 0.1, (0, 0, 1.655), "e_flame", seg=6, r2=0.0)
    cyl("fl2", 0.016, 0.06, (0, 0, 1.64), "e_lamp", seg=5, r2=0.0)
    node("lamp_on")
    export("candle_stand")


def _horn(tag, side, base, s=1.0):
    """Bison horn: short thick base sweeping out, then up and slightly forward to a dark tip (chain of tapering segments)."""
    pts = [(0, 0, 0), (0.16, 0.0, 0.02), (0.30, -0.02, 0.09), (0.38, -0.05, 0.21), (0.40, -0.09, 0.33), (0.36, -0.13, 0.42)]
    rads = [0.075, 0.068, 0.058, 0.045, 0.032, 0.018]
    for k in range(len(pts) - 1):
        a = tuple(base[i] + (pts[k][i] * side if i == 0 else pts[k][i]) * s for i in range(3))
        b = tuple(base[i] + (pts[k + 1][i] * side if i == 0 else pts[k + 1][i]) * s for i in range(3))
        cyl(tag + "%d" % k, rads[k] * s, math.dist(a, b) + 0.02, tuple((a[i] + b[i]) / 2 for i in range(3)), "c_horn",
            rot=(Vector(b) - Vector(a)).to_track_quat('Z', 'Y').to_euler(), seg=8, r2=rads[k + 1] * s)


def _skull(tag, cx, cy, cz, s=1.0):
    """Long-snouted beast skull with bison horns (crest of the throne / arm heads)."""
    box(tag + "cr", (0.26 * s, 0.22 * s, 0.2 * s), (cx, cy, cz), "e_bone", bevel=0.03 * s)
    box(tag + "sn", (0.15 * s, 0.26 * s, 0.12 * s), (cx, cy - 0.2 * s, cz - 0.05 * s), "e_bone", bevel=0.02 * s)
    for sx in (-1, 1):
        box(tag + "ey%d" % sx, (0.06 * s, 0.02 * s, 0.05 * s), (cx + sx * 0.065 * s, cy - 0.11 * s, cz + 0.02 * s), "c_wick")
        _horn(tag + "h%d" % sx, sx, (cx + sx * 0.12 * s, cy, cz + 0.04 * s), s)
    for k in range(3):
        box(tag + "t%d" % k, (0.02 * s, 0.02 * s, 0.04 * s), (cx - 0.04 * s + k * 0.04 * s, cy - 0.3 * s, cz - 0.12 * s), "e_bone")


def m_boss_throne():
    # 3x2x4 high seat for a 3 m beast (ref: gothic carved throne: pointed back with pinnacles, tufted velvet, beast-head arms)
    box("plinth", (3.0, 2.0, 0.25), (0, 0.5, 0.125), "c_ashlar", bevel=0.02)
    box("step", (2.6, 0.4, 0.12), (0, -0.3, 0.31), "c_ashlar", bevel=0.015)
    box("trim", (3.02, 0.04, 0.05), (0, -0.5, 0.22), "c_brass")
    # carcass with blind tracery
    box("carcass", (2.1, 1.25, 0.8), (0, 0.15, 0.65), "c_dark", bevel=0.02)
    for k in range(5):
        x = -0.8 + k * 0.4
        box("arch%d" % k, (0.26, 0.03, 0.5), (x, -0.485, 0.6), "c_velvet")
        box("ap%d" % k, (0.2, 0.031, 0.2), (x, -0.487, 0.85), "c_velvet", (0, math.radians(45), 0))
        box("mul%d" % k, (0.05, 0.05, 0.62), (x + 0.2, -0.49, 0.62), "c_dark")
    box("rail", (2.12, 0.08, 0.06), (0, -0.48, 1.02), "c_brass")
    # seat
    box("seat", (2.0, 1.2, 0.18), (0, 0.15, 1.14), "c_velvet", bevel=0.05)
    box("seatfront", (2.02, 0.1, 0.1), (0, -0.44, 1.06), "c_dark")
    # arms with beast heads
    for sx in (-1, 1):
        x = sx * 1.2
        box("armbase%d" % sx, (0.34, 1.35, 1.3), (x, 0.18, 0.9), "c_dark", bevel=0.03)
        box("armpad%d" % sx, (0.4, 1.4, 0.14), (x, 0.15, 1.62), "c_velvet", bevel=0.04)
        box("armcap%d" % sx, (0.44, 0.3, 0.12), (x, -0.42, 1.66), "c_dark", bevel=0.02)
        _skull("ah%d" % sx, x, -0.55, 1.82, 0.65)
        for k in range(3):
            box("panel%d_%d" % (sx, k), (0.02, 0.32, 0.6), (x + sx * 0.171, -0.2 + k * 0.42, 0.85), "c_velvet")
            box("pst%d_%d" % (sx, k), (0.03, 0.04, 0.03), (x + sx * 0.18, -0.2 + k * 0.42, 1.2), "c_brass")
    # back: tall frame, pointed arch top, tufted velvet inset
    box("back", (2.1, 0.3, 2.1), (0, 0.98, 2.2), "c_dark", bevel=0.03)
    for k in range(6):                                       # stepped gable of the pointed arch
        w = 2.1 - k * 0.34
        box("gab%d" % k, (w, 0.3, 0.16), (0, 0.98, 3.33 + k * 0.15), "c_dark")
    box("inset", (1.6, 0.04, 1.75), (0, 0.81, 2.15), "c_velvet")
    box("insettop", (1.13, 0.041, 1.13), (0, 0.81, 3.0), "c_velvet", (0, math.radians(45), 0))
    for z in (1.35, 2.95):
        box("hb%d" % int(z * 10), (1.7, 0.06, 0.06), (0, 0.79, z), "c_brass")
    torus("rose", 0.22, 0.03, (0, 0.78, 2.55), "c_brass", (math.radians(90), 0, 0), seg=16, seg2=4)
    for k in range(8):
        a = k * math.pi / 4
        rod("sp%d" % k, (0, 0.78, 2.55), (math.cos(a) * 0.21, 0.78, 2.55 + math.sin(a) * 0.21), 0.012, "c_brass", seg=4)
    # pinnacles
    for sx, x, z1 in ((-1, -1.2, 3.25), (1, 1.2, 3.25), (-1, -0.62, 3.6), (1, 0.62, 3.6)):
        box("pin%d_%d" % (sx, int(abs(x) * 10)), (0.2, 0.2, z1 - 0.25), (x, 0.98, 0.25 + (z1 - 0.25) / 2), "c_dark")
        cyl("cap%d_%d" % (sx, int(abs(x) * 10)), 0.15, 0.35, (x, 0.98, z1 + 0.175), "c_dark", seg=4, r2=0.0, rot=(0, 0, math.radians(45)))
        box("cb%d_%d" % (sx, int(abs(x) * 10)), (0.24, 0.24, 0.05), (x, 0.98, z1 + 0.02), "c_brass")
        for zz in (z1 - 0.6, z1 - 1.3):
            box("crk%d_%d_%d" % (sx, int(abs(x) * 10), int(zz * 10)), (0.08, 0.08, 0.14), (x + sx * 0.11, 0.98, zz), "c_dark", (0, math.radians(-25 * sx), 0))
    # crest: horned skull
    _skull("cr", 0, 0.8, 3.68, 1.15)
    for sx in (-1, 1):
        for k in range(4):
            box("stud%d_%d" % (sx, k), (0.04, 0.04, 0.04), (sx * 1.03, 0.81, 1.45 + k * 0.5), "c_brass")
    node("body")
    export("boss_throne")



# ======================================================================================== Rev 3 / C-2: castle rooms
def _candle(tag, x, y, z, h=0.16, r=0.035, flame=True):
    """Short church candle standing at z with wax drips; the flame goes to FLAMES (joined into lamp_on later)."""
    cyl(tag + "c", r, h, (x, y, z + h / 2), "c_wax", seg=8)
    cyl(tag + "d", r * 0.9, 0.01, (x, y, z + h), "c_wax", seg=8, r2=r * 0.7)
    rod(tag + "w", (x, y, z + h), (x, y, z + h + 0.025), 0.004, "c_wick", seg=3)
    if flame:
        FLAMES.append((x, y, z + h + 0.03))


def _flames(scale=1.0):
    for k, (x, y, z) in enumerate(FLAMES):
        cyl("fl%d" % k, 0.018 * scale, 0.07 * scale, (x, y, z + 0.035 * scale), "e_flame", seg=5, r2=0.0)
        cyl("fc%d" % k, 0.01 * scale, 0.04 * scale, (x, y, z + 0.02 * scale), "e_lamp", seg=4, r2=0.0)
    FLAMES.clear()


FLAMES = []


def m_castle_chandelier():
    # forged ring chandelier (ref: hand-forged iron ring chandelier, 3-4 chains to a top ring, scroll arms, drip cups)
    R, zr = 0.62, 0.22
    torus("ring", R, 0.03, (0, 0, zr), "e_iron", seg=24, seg2=5)
    torus("ring2", R - 0.08, 0.016, (0, 0, zr + 0.03), "e_iron", seg=24, seg2=4)
    for k in range(8):                                       # little twisted bars between the rings
        a = k * math.pi / 4 + math.pi / 8
        rod("tw%d" % k, (math.cos(a) * (R - 0.08), math.sin(a) * (R - 0.08), zr + 0.03), (math.cos(a) * R, math.sin(a) * R, zr), 0.01, "e_iron", seg=4)
    for k in range(6):
        a = k * math.pi / 3
        cx, cy = math.cos(a) * R, math.sin(a) * R
        torus("scr%d" % k, 0.06, 0.012, (math.cos(a) * (R + 0.04), math.sin(a) * (R + 0.04), zr - 0.06), "e_iron", (math.radians(90), 0, a + math.pi / 2), seg=10, seg2=3)
        cyl("cup%d" % k, 0.05, 0.03, (cx, cy, zr + 0.045), "e_iron", seg=8, r2=0.065)
        _candle("cd%d" % k, cx, cy, zr + 0.06, h=0.14 + 0.03 * (k % 2))
    # chains to a crown hook at the top of the cell
    for k in range(4):
        a = k * math.pi / 2 + math.pi / 4
        p0 = (math.cos(a) * (R - 0.04), math.sin(a) * (R - 0.04), zr + 0.02)
        p1 = (0, 0, 0.82)
        n = 9
        for i in range(n):
            t0, t1 = i / n, (i + 0.8) / n
            a0 = tuple(p0[j] + (p1[j] - p0[j]) * t0 for j in range(3))
            a1 = tuple(p0[j] + (p1[j] - p0[j]) * t1 for j in range(3))
            rod("ch%d_%d" % (k, i), a0, a1, 0.009, "e_iron", seg=4)
    torus("crown", 0.07, 0.015, (0, 0, 0.84), "e_iron", seg=10, seg2=4)
    rod("hook", (0, 0, 0.86), (0, 0, 1.0), 0.015, "e_iron", seg=5)
    cyl("rose", 0.09, 0.03, (0, 0, 0.985), "e_iron", seg=8)
    # centre boss with a hanging finial
    for k in range(3):
        a = k * 2 * math.pi / 3
        rod("sp%d" % k, (math.cos(a) * (R - 0.02), math.sin(a) * (R - 0.02), zr), (0, 0, zr - 0.12), 0.012, "e_iron", seg=4)
    cyl("boss", 0.05, 0.08, (0, 0, zr - 0.14), "e_iron", seg=8)
    cyl("fin", 0.04, 0.16, (0, 0, zr - 0.26), "e_iron", seg=6, r2=0.0, rot=(math.pi, 0, 0))
    node("body")
    _flames()
    node("lamp_on")
    export("castle_chandelier")


def m_wall_sconce():
    # wrought-iron wall sconce (refs: forged leaf back-plate, curled arm, drip pan with a thick candle)
    box("plate", (0.16, 0.02, 0.42), (0, 0.48, 0.55), "e_iron", bevel=0.01)
    for sz in (-1, 1):
        cyl("tip%d" % sz, 0.05, 0.12, (0, 0.48, 0.55 + sz * 0.27), "e_iron", seg=4, r2=0.0, rot=(0 if sz > 0 else math.pi, 0, 0))
        rod("nail%d" % sz, (0, 0.49, 0.55 + sz * 0.15), (0, 0.46, 0.55 + sz * 0.15), 0.014, "e_iron", seg=5)
    rod("arm1", (0, 0.47, 0.45), (0, 0.25, 0.42), 0.016, "e_iron", seg=5)
    rod("arm2", (0, 0.25, 0.42), (0, 0.12, 0.5), 0.016, "e_iron", seg=5)
    torus("curl", 0.05, 0.012, (0, 0.33, 0.36), "e_iron", (0, math.radians(90), 0), seg=10, seg2=3)
    rod("brace", (0, 0.47, 0.3), (0, 0.3, 0.41), 0.012, "e_iron", seg=4)
    cyl("pan", 0.1, 0.02, (0, 0.12, 0.51), "e_iron", seg=12)
    torus("rim", 0.1, 0.008, (0, 0.12, 0.52), "e_iron", seg=12, seg2=3)
    _candle("c", 0, 0.12, 0.52, h=0.2, r=0.04)
    for k, a in enumerate((0.4, 2.5, 4.4)):
        cyl("drip%d" % k, 0.01, 0.07, (math.cos(a) * 0.04, 0.12 + math.sin(a) * 0.04, 0.68), "c_wax", seg=4, r2=0.006)
    node("body")
    _flames(1.3)
    node("lamp_on")
    export("wall_sconce")


def _trestle_table(L=2.9, W=0.9, H=0.82, top="c_dark"):
    box("top", (L, W, 0.08), (0, 0, H - 0.04), top, bevel=0.012)
    box("apron_f", (L - 0.3, 0.04, 0.1), (0, -W / 2 + 0.08, H - 0.13), "c_dark")
    box("apron_b", (L - 0.3, 0.04, 0.1), (0, W / 2 - 0.08, H - 0.13), "c_dark")
    for sx in (-1, 1):                                       # trestle ends: foot, post, cross-head, chamfered
        x = sx * (L / 2 - 0.3)
        box("foot%d" % sx, (0.12, W - 0.05, 0.1), (x, 0, 0.05), "c_dark", bevel=0.015)
        box("post%d" % sx, (0.1, 0.16, H - 0.25), (x, 0, 0.1 + (H - 0.25) / 2), "c_dark")
        box("head%d" % sx, (0.12, W - 0.15, 0.08), (x, 0, H - 0.12), "c_dark")
        for sy in (-1, 1):
            rod("brc%d%d" % (sx, sy), (x, sy * 0.35, 0.1), (x, sy * 0.07, 0.35), 0.025, "c_dark", seg=4)
    box("stretch", (L - 0.6, 0.07, 0.07), (0, 0, 0.28), "c_dark")
    for sx in (-1, 1):
        box("peg%d" % sx, (0.03, 0.12, 0.05), (sx * (L / 2 - 0.22), 0, 0.28), "c_dark")


def _plate(tag, x, y, z):
    cyl(tag + "p", 0.12, 0.015, (x, y, z + 0.008), "e_iron", seg=12, r2=0.1)
    torus(tag + "r", 0.11, 0.008, (x, y, z + 0.016), "e_iron", seg=12, seg2=3)


def _goblet(tag, x, y, z):
    cyl(tag + "f", 0.035, 0.01, (x, y, z + 0.005), "c_brass", seg=8)
    rod(tag + "s", (x, y, z), (x, y, z + 0.08), 0.008, "c_brass", seg=5)
    cyl(tag + "b", 0.022, 0.06, (x, y, z + 0.11), "c_brass", seg=8, r2=0.038)
    cyl(tag + "w", 0.03, 0.005, (x, y, z + 0.13), "c_wine", seg=8)


def m_feast_table():
    # long trestle table of the refectory (ref: oak trestle table + benches in a 14th c. hall, castle feast tables)
    _trestle_table()
    z = 0.82
    box("runner", (2.5, 0.34, 0.006), (0, 0, z + 0.003), "c_velvet")
    for k, x in enumerate((-1.1, -0.4, 0.4, 1.1)):
        for sy in (-1, 1):
            _plate("pl%d%d" % (k, sy), x, sy * 0.3, z)
            _goblet("gb%d%d" % (k, sy), x + 0.17, sy * 0.24, z)
    # roast on a platter, bread loaves, a jug, apples, a knife
    cyl("platter", 0.22, 0.02, (-0.75, 0.0, z + 0.016), "e_iron", seg=14, r2=0.19)
    box("roast", (0.26, 0.17, 0.12), (-0.75, 0.0, z + 0.09), "c_roast", bevel=0.05)
    rod("bone1", (-0.9, 0.03, z + 0.12), (-0.98, 0.05, z + 0.2), 0.016, "e_bone", seg=5)
    rod("bone2", (-0.62, -0.03, z + 0.1), (-0.52, -0.06, z + 0.16), 0.014, "e_bone", seg=5)
    for k, (x, y) in enumerate(((0.75, 0.02), (0.92, -0.05))):
        box("bread%d" % k, (0.2, 0.1, 0.08), (x, y, z + 0.045), "c_bread", (0, 0, 0.3 * k), bevel=0.035)
    cyl("jug", 0.06, 0.18, (0.3, 0.05, z + 0.1), "e_ceramic", seg=10, r2=0.04)
    torus("jh", 0.04, 0.01, (0.36, 0.05, z + 0.12), "e_ceramic", (math.radians(90), 0, 0), seg=8, seg2=3)
    for k in range(3):
        cyl("apl%d" % k, 0.03, 0.045, (0.55 + k * 0.05, -0.05 + (k % 2) * 0.05, z + 0.025), "c_apple", seg=7)
    box("knife", (0.18, 0.02, 0.006), (-0.5, 0.15, z + 0.006), "e_iron", (0, 0, 0.4))
    # three-armed brass candelabrum in the middle
    cyl("cbase", 0.08, 0.03, (0, 0, z + 0.015), "c_brass", seg=10)
    rod("cstem", (0, 0, z), (0, 0, z + 0.28), 0.014, "c_brass", seg=6)
    torus("carm", 0.12, 0.01, (0, 0, z + 0.26), "c_brass", (math.radians(90), 0, 0), seg=12, seg2=3)
    for sx in (-1, 0, 1):
        cyl("ccup%d" % sx, 0.025, 0.03, (sx * 0.12, 0, z + 0.28 + (0.06 if sx == 0 else 0)), "c_brass", seg=8)
        _candle("cc%d" % sx, sx * 0.12, 0, z + 0.295 + (0.06 if sx == 0 else 0), h=0.12, r=0.02)
    node("body")
    _flames()
    node("lamp_on")
    export("feast_table")


def m_long_table():
    # plain oak work table (library / guard rooms): books, a candle in a dish, parchment, ink pot
    _trestle_table()
    z = 0.82
    for k, (x, y, rz, hgt) in enumerate(((-1.0, 0.1, 0.1, 0.07), (-0.95, 0.12, -0.15, 0.06), (-0.98, 0.1, 0.3, 0.05))):
        box("bk%d" % k, (0.3, 0.22, hgt), (x, y, z + 0.035 + k * 0.065), "e_books", (0, 0, rz), bevel=0.008)
    ob = box("open", (0.42, 0.28, 0.03), (0.15, -0.05, z + 0.015), "c_pages", (0, 0, 0.12))
    box("cover", (0.44, 0.3, 0.012), (0.15, -0.05, z + 0.004), "c_velvet", (0, 0, 0.12))
    for k in range(3):
        box("parch%d" % k, (0.22, 0.3, 0.004), (0.7 + k * 0.05, 0.1 - k * 0.03, z + 0.002 + k * 0.004), "e_parch", (0, 0, 0.3 * k - 0.2))
    cyl("ink", 0.03, 0.05, (0.55, -0.25, z + 0.025), "c_wick", seg=8)
    rod("quill", (0.55, -0.25, z + 0.04), (0.62, -0.18, z + 0.22), 0.006, "e_bone", seg=4)
    cyl("dish", 0.07, 0.015, (-0.4, -0.2, z + 0.008), "e_iron", seg=10)
    _candle("c", -0.4, -0.2, z + 0.015, h=0.12, r=0.03)
    node("body")
    _flames()
    node("lamp_on")
    export("long_table")


def m_castle_bench():
    box("seat", (2.9, 0.34, 0.06), (0, 0, 0.45), "c_dark", bevel=0.01)
    for sx in (-1, 1):
        x = sx * 1.25
        box("slab%d" % sx, (0.07, 0.32, 0.42), (x, 0, 0.21), "c_dark")
        box("cut%d" % sx, (0.075, 0.12, 0.12), (x, 0, 0.06), "e_coal")        # foot cut-out (dark notch)
    box("stretch", (2.4, 0.05, 0.08), (0, 0, 0.2), "c_dark")
    box("apron", (2.6, 0.03, 0.07), (0, -0.15, 0.38), "c_dark")
    node("body")
    export("castle_bench")


def m_high_chair():
    # gothic high-back chair (ref: french gothic oak armchair - pointed crest, tracery panel, leather seat)
    for sx in (-1, 1):
        for sy in (-1, 1):
            hgt = 1.95 if sy > 0 else 0.7
            box("leg%d%d" % (sx, sy), (0.07, 0.07, hgt), (sx * 0.26, sy * 0.22, hgt / 2), "c_dark")
        box("arm%d" % sx, (0.07, 0.5, 0.05), (sx * 0.27, 0.0, 0.72), "c_dark", bevel=0.01)
        cyl("fin%d" % sx, 0.045, 0.12, (sx * 0.26, 0.22, 2.01), "c_dark", seg=4, r2=0.0, rot=(0, 0, math.radians(45)))
        box("rail%d" % sx, (0.05, 0.4, 0.05), (sx * 0.26, 0, 0.12), "c_dark")
    box("seat", (0.56, 0.5, 0.06), (0, 0, 0.47), "c_dark")
    box("cush", (0.5, 0.44, 0.06), (0, -0.01, 0.52), "c_velvet", bevel=0.02)
    box("apron", (0.56, 0.03, 0.1), (0, -0.24, 0.41), "c_dark")
    box("backp", (0.46, 0.04, 1.2), (0, 0.23, 1.15), "c_velvet")
    box("backf", (0.46, 0.05, 0.08), (0, 0.23, 0.58), "c_dark")
    box("backt", (0.46, 0.05, 0.08), (0, 0.23, 1.76), "c_dark")
    for k in range(4):                                       # pointed crest
        box("crest%d" % k, (0.46 - k * 0.12, 0.05, 0.06), (0, 0.23, 1.83 + k * 0.06), "c_dark")
    for x in (-0.11, 0.11):
        box("mull%d" % int(x * 100), (0.03, 0.05, 1.1), (x, 0.215, 1.17), "c_dark")
    torus("ros", 0.08, 0.012, (0, 0.205, 1.5), "c_brass", (math.radians(90), 0, 0), seg=10, seg2=3)
    node("body")
    export("high_chair")


def _glaive(tag, x, y, z0, lean=0.0, h=2.6):
    """Guards' weapon: long black-steel glaive with a hooked, serrated blade and a spiked butt."""
    M = Matrix.Translation(Vector((x, y, z0))) @ Euler((lean, 0, 0)).to_matrix().to_4x4()
    parts = []
    parts.append(rod(tag + "p", (0, 0, 0.05), (0, 0, h - 0.55), 0.022, "c_dark", seg=6))
    for zz in (0.9, 1.3):
        parts.append(cyl(tag + "g%d" % int(zz * 10), 0.028, 0.12, (0, 0, zz), "e_hide", seg=6))
    parts.append(cyl(tag + "sk", 0.03, 0.1, (0, 0, h - 0.55), "e_iron", seg=6))
    parts.append(box(tag + "bl", (0.05, 0.012, 0.5), (0.03, 0, h - 0.27), "c_blacksteel"))
    parts.append(box(tag + "tip", (0.05, 0.012, 0.12), (0.045, 0, h - 0.02), "c_blacksteel", (0, math.radians(-25), 0)))
    parts.append(box(tag + "hk", (0.16, 0.012, 0.04), (-0.08, 0, h - 0.42), "c_blacksteel", (0, math.radians(-30), 0)))
    for k in range(4):
        parts.append(box(tag + "sr%d" % k, (0.03, 0.012, 0.03), (0.065, 0, h - 0.45 + k * 0.11), "c_blacksteel", (0, math.radians(45), 0)))
    parts.append(cyl(tag + "bt", 0.025, 0.08, (0, 0, 0.04), "e_iron", seg=5, r2=0.0, rot=(math.pi, 0, 0)))
    for o in parts:
        o.data.transform(M)


def _sickle(tag, x, y, z, flip=1):
    """Hooked claw-sickle of the wall-running guards, hung on a peg."""
    rod(tag + "h", (x, y, z), (x, y, z - 0.3), 0.02, "e_hide", seg=5)
    cyl(tag + "pm", 0.03, 0.04, (x, y, z - 0.32), "e_iron", seg=6)
    for k in range(6):
        a0, a1 = k / 6 * 2.4, (k + 1) / 6 * 2.4
        p0 = (x + flip * math.sin(a0) * 0.18, y, z + 0.18 - math.cos(a0) * 0.18)
        p1 = (x + flip * math.sin(a1) * 0.18, y, z + 0.18 - math.cos(a1) * 0.18)
        rod(tag + "b%d" % k, p0, p1, 0.022 - k * 0.003, "c_blacksteel", seg=4)


def m_guard_weapon_rack():
    # wall rack of the guards' arms (ref: medieval armoury pole-arm racks): uprights, notched rails, 3 glaives, 2 claw
    # sickles on pegs, a spiked round buckler; brass caps
    for sx in (-1, 1):
        box("up%d" % sx, (0.12, 0.12, 2.9), (sx * 1.35, 0.4, 1.45), "c_dark", bevel=0.01)
        cyl("cap%d" % sx, 0.08, 0.1, (sx * 1.35, 0.4, 2.95), "c_brass", seg=4, r2=0.0, rot=(0, 0, math.radians(45)))
        box("ft%d" % sx, (0.18, 0.5, 0.08), (sx * 1.35, 0.25, 0.04), "c_dark")
    box("rail_lo", (2.6, 0.18, 0.08), (0, 0.38, 0.12), "c_dark")
    box("rail_hi", (2.6, 0.12, 0.08), (0, 0.42, 1.95), "c_dark")
    box("rail_top", (2.6, 0.08, 0.1), (0, 0.44, 2.75), "c_dark")
    for k in range(5):
        box("notch%d" % k, (0.05, 0.13, 0.09), (-0.9 + k * 0.45, 0.35, 1.95), "e_coal")
    for k, x in enumerate((-0.9, 0.0, 0.9)):
        _glaive("gl%d" % k, x, 0.33, 0.1, lean=-0.05, h=2.75)
    for k, (x, f) in enumerate(((-0.45, 1), (0.45, -1))):
        rod("peg%d" % k, (x, 0.45, 2.45), (x, 0.32, 2.45), 0.018, "c_dark", seg=5)
        _sickle("sk%d" % k, x, 0.32, 2.42, f)
    cyl("shield", 0.36, 0.05, (1.0, 0.3, 1.1), "c_dark", seg=14, rot=(math.radians(90), 0, 0))
    torus("srim", 0.36, 0.025, (1.0, 0.27, 1.1), "e_iron", (math.radians(90), 0, 0), seg=14, seg2=4)
    cyl("boss", 0.09, 0.08, (1.0, 0.24, 1.1), "e_iron", seg=8, r2=0.03, rot=(math.radians(90), 0, 0))
    cyl("spike", 0.03, 0.12, (1.0, 0.16, 1.1), "c_blacksteel", seg=5, r2=0.0, rot=(math.radians(90), 0, 0))
    node("body")
    export("guard_weapon_rack")


def m_guard_armor():
    # spare harness of the guards on a stand: horned helm, spiked pauldrons, black plate, torn red tabard
    cyl("base", 0.3, 0.06, (0, 0, 0.03), "c_dark", seg=8)
    rod("pole", (0, 0, 0.05), (0, 0, 1.55), 0.03, "c_dark", seg=6)
    box("cross", (0.62, 0.06, 0.06), (0, 0, 1.4), "c_dark")
    box("chest", (0.46, 0.28, 0.5), (0, -0.02, 1.15), "c_blacksteel", bevel=0.06)
    box("fauld", (0.48, 0.3, 0.12), (0, -0.02, 0.86), "c_blacksteel", bevel=0.03)
    box("tabard", (0.3, 0.02, 0.62), (0, -0.17, 0.82), "c_velvet")
    box("belt", (0.5, 0.31, 0.05), (0, -0.02, 0.93), "e_hide")
    cyl("buckle", 0.035, 0.02, (0, -0.18, 0.93), "c_brass", seg=6, rot=(math.radians(90), 0, 0))
    for sx in (-1, 1):
        box("paul%d" % sx, (0.22, 0.3, 0.12), (sx * 0.3, -0.02, 1.38), "c_blacksteel", (0, math.radians(sx * 20), 0), bevel=0.04)
        for k in range(3):
            cyl("psp%d%d" % (sx, k), 0.025, 0.12, (sx * (0.25 + k * 0.06), -0.08 + k * 0.06, 1.48), "e_iron", seg=5, r2=0.0)
    cyl("gorget", 0.12, 0.08, (0, -0.02, 1.47), "c_blacksteel", seg=10)
    cyl("helm", 0.14, 0.24, (0, -0.02, 1.64), "c_blacksteel", seg=10, r2=0.12)
    box("visor", (0.2, 0.02, 0.03), (0, -0.155, 1.66), "e_coal")
    box("snout", (0.1, 0.1, 0.1), (0, -0.16, 1.58), "c_blacksteel", bevel=0.02)
    for sx in (-1, 1):
        _horn("hn%d" % sx, sx, (sx * 0.12, -0.02, 1.7), 0.45)
    node("body")
    export("guard_armor")


def m_canopy_bed():
    # four-poster bed with a red canopy and hangings (refs: castle bedchamber, Williamsburg red bed hangings)
    # footprint: x -0.5..1.5, y -0.5..2.5 (foot at the front)
    cx, cy = 0.5, 1.0
    for sx in (-1, 1):
        for sy in (-1, 1):
            x, y = cx + sx * 0.92, cy + sy * 1.42
            box("post%d%d" % (sx, sy), (0.1, 0.1, 2.7), (x, y, 1.35), "c_dark")
            for zz in (0.5, 1.1):
                cyl("turn%d%d%d" % (sx, sy, int(zz * 10)), 0.075, 0.1, (x, y, zz), "c_dark", seg=8)
            cyl("fin%d%d" % (sx, sy), 0.06, 0.18, (x, y, 2.8), "c_dark", seg=6, r2=0.0)
    box("frame", (1.9, 2.9, 0.2), (cx, cy, 0.35), "c_dark")
    box("matt", (1.76, 2.7, 0.18), (cx, cy - 0.02, 0.54), "c_linen", bevel=0.05)
    box("blanket", (1.82, 1.9, 0.08), (cx, cy - 0.42, 0.63), "c_velvet", bevel=0.03)
    box("fold", (1.82, 0.18, 0.1), (cx, cy + 0.5, 0.65), "c_linen", bevel=0.04)
    for sx in (-1, 1):
        box("pillow%d" % sx, (0.7, 0.36, 0.14), (cx + sx * 0.42, cy + 1.08, 0.7), "c_linen", bevel=0.06)
        box("drape%d" % sx, (0.03, 1.9, 0.32), (cx + sx * 0.92, cy - 0.42, 0.45), "c_velvet")
    box("head", (1.84, 0.08, 1.5), (cx, cy + 1.42, 1.2), "c_dark")
    box("headp", (1.5, 0.02, 1.0), (cx, cy + 1.37, 1.25), "c_velvet")
    for k in range(4):
        box("hcr%d" % k, (1.84 - k * 0.4, 0.08, 0.08), (cx, cy + 1.42, 1.98 + k * 0.08), "c_dark")
    box("foot", (1.84, 0.08, 0.55), (cx, cy - 1.42, 0.55), "c_dark")
    # canopy: tester frame + valance + roof cloth
    box("tester", (2.0, 3.0, 0.06), (cx, cy, 2.62), "c_velvet")
    for sy in (-1, 1):
        box("valf%d" % sy, (2.02, 0.03, 0.26), (cx, cy + sy * 1.5, 2.5), "c_velvet")
        box("valtrim%d" % sy, (2.04, 0.035, 0.03), (cx, cy + sy * 1.5, 2.38), "c_brass")
    for sx in (-1, 1):
        box("vals%d" % sx, (0.03, 3.02, 0.26), (cx + sx * 1.0, cy, 2.5), "c_velvet")
        box("valstrim%d" % sx, (0.035, 3.04, 0.03), (cx + sx * 1.0, cy, 2.38), "c_brass")
        # curtains gathered at the head posts and tied at the foot posts
        box("curh%d" % sx, (0.16, 0.22, 2.0), (cx + sx * 0.94, cy + 1.25, 1.4), "c_velvet", bevel=0.05)
        box("curf%d" % sx, (0.14, 0.18, 1.9), (cx + sx * 0.94, cy - 1.3, 1.45), "c_velvet", bevel=0.05)
        torus("tie%d" % sx, 0.1, 0.015, (cx + sx * 0.94, cy - 1.3, 1.2), "c_brass", seg=10, seg2=3)
    box("curback", (1.7, 0.03, 2.0), (cx, cy + 1.48, 1.4), "c_velvet")
    node("body")
    export("canopy_bed")


def m_castle_chest():
    # iron-bound oak strongbox (refs: 17th c. iron-bound oak chest, 1700's treasure chest): straps, corner plates,
    # hasp + brass lock plate, ring handles, low round lid
    box("body", (0.86, 0.56, 0.44), (0, 0, 0.24), "c_dark", bevel=0.01)
    cyl("lid", 0.28, 0.86, (0, 0, 0.46), "c_dark", rot=(0, math.radians(90), 0), seg=12)
    box("lidcut", (0.88, 0.58, 0.02), (0, 0, 0.46), "e_coal")
    for x in (-0.32, 0.0, 0.32):
        box("strap%d" % int(x * 100), (0.06, 0.58, 0.46), (x, 0, 0.24), "e_iron")
        torus("lstrap%d" % int(x * 100), 0.285, 0.02, (x, 0, 0.46), "e_iron", (0, math.radians(90), 0), seg=12, seg2=3)
    for sx in (-1, 1):
        for sy in (-1, 1):
            box("corner%d%d" % (sx, sy), (0.1, 0.1, 0.1), (sx * 0.42, sy * 0.27, 0.07), "e_iron")
        torus("ring%d" % sx, 0.06, 0.012, (sx * 0.445, 0, 0.36), "e_iron", (0, math.radians(90), 0), seg=10, seg2=3)
    box("lock", (0.14, 0.03, 0.14), (0, -0.29, 0.38), "c_brass", bevel=0.01)
    box("hasp", (0.05, 0.03, 0.16), (0, -0.305, 0.48), "e_iron")
    box("keyh", (0.02, 0.035, 0.05), (0, -0.3, 0.37), "e_coal")
    for k in range(6):
        cyl("rivet%d" % k, 0.012, 0.01, (-0.32 + (k % 3) * 0.32, -0.29, 0.12 + (k // 3) * 0.22), "e_iron", seg=5, rot=(math.radians(90), 0, 0))
    box("plinth", (0.9, 0.6, 0.04), (0, 0, 0.02), "c_dark")
    node("body")
    export("castle_chest")


def m_chained_lectern():
    # library lectern with a chained folio (ref: monastic chained library, gothic reading desk)
    box("base", (0.5, 0.5, 0.08), (0, 0, 0.04), "c_dark", bevel=0.01)
    for k in range(4):
        a = k * math.pi / 2
        box("foot%d" % k, (0.08, 0.3, 0.06), (math.cos(a) * 0.22, math.sin(a) * 0.22, 0.03), "c_dark", (0, 0, a))
    cyl("col", 0.07, 0.95, (0, 0.05, 0.55), "c_dark", seg=8)
    for zz in (0.25, 0.7, 1.0):
        torus("knop%d" % int(zz * 10), 0.08, 0.02, (0, 0.05, zz), "c_dark", seg=10, seg2=4)
    desk = box("desk", (0.62, 0.46, 0.05), (0, 0.02, 1.13), "c_dark", (math.radians(-22), 0, 0))
    box("lip", (0.62, 0.04, 0.05), (0, -0.2, 1.06), "c_dark")
    # (Rev 4 item 14: the chained folio is a separate block `chained_books`)
    node("body")
    export("chained_lectern")


def m_chapel_altar():
    # chapel altar (ref: gothic stone altar, brass altar candlesticks): ashlar mensa on a blind-arcade front, red frontal,
    # two tall brass candlesticks, a horned beast skull relic on a brass stand, open book
    box("mensa", (2.9, 0.95, 0.12), (0, 0, 1.0), "c_ashlar", bevel=0.02)
    box("body", (2.7, 0.8, 0.94), (0, 0.03, 0.47), "c_ashlar")
    for k in range(5):
        x = -1.08 + k * 0.54
        box("arc%d" % k, (0.3, 0.02, 0.55), (x, -0.375, 0.45), "c_brick")
        box("arct%d" % k, (0.22, 0.021, 0.22), (x, -0.376, 0.72), "c_brick", (0, math.radians(45), 0))
    box("frontal", (1.2, 0.02, 0.7), (0, -0.39, 0.62), "c_velvet")
    box("fringe", (1.22, 0.025, 0.04), (0, -0.395, 0.27), "c_brass")
    box("cloth", (2.92, 0.97, 0.01), (0, 0, 1.065), "c_linen")
    for sx in (-1, 1):
        x = sx * 1.15
        cyl("cb%d" % sx, 0.1, 0.05, (x, 0.1, 1.095), "c_brass", seg=10, r2=0.07)
        rod("cs%d" % sx, (x, 0.1, 1.1), (x, 0.1, 1.55), 0.022, "c_brass", seg=6)
        for zz in (1.25, 1.45):
            torus("ck%d%d" % (sx, int(zz * 10)), 0.03, 0.012, (x, 0.1, zz), "c_brass", seg=8, seg2=3)
        cyl("cp%d" % sx, 0.06, 0.02, (x, 0.1, 1.56), "c_brass", seg=10)
        _candle("cd%d" % sx, x, 0.1, 1.57, h=0.3, r=0.035)
    cyl("relb", 0.13, 0.06, (0, 0.18, 1.1), "c_brass", seg=10)
    rod("rels", (0, 0.18, 1.12), (0, 0.18, 1.35), 0.03, "c_brass", seg=6)
    _skull("rel", 0, 0.16, 1.47, 0.7)
    box("book", (0.36, 0.26, 0.03), (-0.5, -0.1, 1.085), "c_pages", (0, 0, 0.15))
    box("bookc", (0.38, 0.28, 0.012), (-0.5, -0.1, 1.074), "c_velvet", (0, 0, 0.15))
    box("step", (3.0, 0.4, 0.1), (0, -0.6, 0.05), "c_ashlar")
    node("body")
    _flames(1.2)
    node("lamp_on")
    export("chapel_altar")


def m_hearth_cauldron():
    # kitchen fire with a cauldron on an iron tripod (ref: medieval castle kitchen hearth, small roasting spit)
    for k in range(9):
        a = k * 2 * math.pi / 9
        box("stone%d" % k, (0.2, 0.16, 0.12), (math.cos(a) * 0.4, math.sin(a) * 0.4, 0.06), "c_ashlar", (0, 0, a), bevel=0.03)
    cyl("ash", 0.36, 0.03, (0, 0, 0.015), "e_coal", seg=12)
    for k in range(4):
        a = k * math.pi / 2 + 0.3
        rod("log%d" % k, (math.cos(a) * 0.3, math.sin(a) * 0.3, 0.05), (math.cos(a) * 0.04, math.sin(a) * 0.04, 0.16), 0.05, "e_bark", seg=6)
    for k in range(3):                                        # tripod
        a = k * 2 * math.pi / 3 + 0.5
        rod("leg%d" % k, (math.cos(a) * 0.48, math.sin(a) * 0.48, 0.0), (0, 0, 1.3), 0.018, "e_iron", seg=5)
    cyl("top", 0.04, 0.06, (0, 0, 1.3), "e_iron", seg=6)
    for k in range(8):
        rod("ch%d" % k, (0, 0, 1.28 - k * 0.07), (0, 0, 1.23 - k * 0.07), 0.01, "e_iron", seg=4)
    rod("bail", (-0.22, 0, 0.62), (0, 0, 0.72), 0.01, "e_iron", seg=4)
    rod("bail2", (0.22, 0, 0.62), (0, 0, 0.72), 0.01, "e_iron", seg=4)
    cyl("pot", 0.22, 0.3, (0, 0, 0.47), "c_blacksteel", seg=14, r2=0.25)
    cyl("potb", 0.18, 0.08, (0, 0, 0.29), "c_blacksteel", seg=14, r2=0.22)
    torus("lip", 0.25, 0.02, (0, 0, 0.62), "c_blacksteel", seg=14, seg2=4)
    cyl("soup", 0.235, 0.01, (0, 0, 0.6), "c_soup", seg=14)
    rod("ladle", (0.1, 0.05, 0.6), (0.28, 0.12, 0.9), 0.012, "c_dark", seg=4)
    node("body")
    cyl("emb", 0.28, 0.02, (0, 0, 0.04), "e_embers", seg=12)
    for k in range(5):
        a = k * 2 * math.pi / 5
        cyl("fl%d" % k, 0.07, 0.28 - 0.04 * (k % 2), (math.cos(a) * 0.08, math.sin(a) * 0.08, 0.2), "e_fire", seg=5, r2=0.0)
    cyl("flc", 0.09, 0.34, (0, 0, 0.2), "e_flame", seg=6, r2=0.0)
    node("lamp_on")
    export("hearth_cauldron")


def m_alchemy_table():
    # alchemist's bench (ref: alchemy workbench with flasks, alembic, books, candles); 2 wide, hutch at the back
    box("top", (1.9, 0.8, 0.07), (0.5, 0, 0.82), "c_dark", bevel=0.01)
    for sx in (-1, 1):
        for sy in (-1, 1):
            box("leg%d%d" % (sx, sy), (0.08, 0.08, 0.78), (0.5 + sx * 0.88, sy * 0.33, 0.39), "c_dark")
    box("shelf_lo", (1.8, 0.7, 0.04), (0.5, 0, 0.2), "c_dark")
    box("hutch_b", (1.9, 0.04, 0.9), (0.5, 0.38, 1.3), "c_dark")
    for sx in (-1, 1):
        box("hutch_s%d" % sx, (0.04, 0.26, 0.9), (0.5 + sx * 0.93, 0.26, 1.3), "c_dark")
    for zz in (1.2, 1.55, 1.73):
        box("hs%d" % int(zz * 100), (1.86, 0.26, 0.03), (0.5, 0.26, zz), "c_dark")
    # flasks on the hutch shelves (glowing liquids = lamp node), jars, skull
    # (Rev 4 item 14: the flasks/alembic/books decor is a separate block `alchemy_set`)
    node("body")
    node("lamp_on")
    export("alchemy_table")


def m_wall_shackles():
    # prison wall shackles (ref: hand-forged dungeon shackles, chained cuffs)
    for sx in (-1, 1):
        x = sx * 0.28
        box("plate%d" % sx, (0.12, 0.02, 0.12), (x, 0.48, 1.6), "e_iron")
        torus("eye%d" % sx, 0.035, 0.01, (x, 0.45, 1.6), "e_iron", (math.radians(90), 0, 0), seg=8, seg2=3)
        for k in range(8):
            z = 1.56 - k * 0.075
            torus("ch%d%d" % (sx, k), 0.025, 0.007, (x + sx * 0.01 * k, 0.43, z), "e_iron",
                  ((math.radians(90) if k % 2 else 0), 0, 0), seg=8, seg2=3)
        torus("cuff%d" % sx, 0.06, 0.016, (x + sx * 0.09, 0.42, 0.93), "e_iron", (0, math.radians(90), 0), seg=12, seg2=4)
        box("hinge%d" % sx, (0.03, 0.04, 0.04), (x + sx * 0.09, 0.42, 0.87), "e_iron")
    box("plate_lo", (0.12, 0.02, 0.1), (0, 0.48, 0.2), "e_iron")
    for k in range(10):
        torus("fc%d" % k, 0.03, 0.008, (-0.25 + k * 0.05, 0.38 - k * 0.02, 0.02 + (0.18 if k == 0 else 0.0) if k < 2 else 0.015), "e_iron",
              ((math.radians(90) if k % 2 else 0), 0, 0), seg=8, seg2=3)
    node("body")
    export("wall_shackles")


def m_gold_hoard():
    # treasury hoard: coin heap, spilled coins, goblet, a crown, gems
    cyl("heap", 0.48, 0.16, (0, 0, 0.08), "c_coins", seg=14, r2=0.4)
    cyl("heap_m", 0.4, 0.16, (0.02, 0.02, 0.24), "c_coins", seg=12, r2=0.28)
    cyl("heap_t", 0.28, 0.14, (0.03, 0.03, 0.39), "c_coins", seg=10, r2=0.1)
    cyl("heap2", 0.26, 0.18, (0.22, 0.2, 0.09), "c_coins", seg=10, r2=0.12)
    cyl("heap3", 0.22, 0.14, (-0.24, -0.18, 0.07), "c_coins", seg=10, r2=0.1)
    rnd = random.Random(11)
    for k in range(16):
        a, d = rnd.random() * 6.28, 0.3 + rnd.random() * 0.18
        cyl("coin%d" % k, 0.035, 0.008, (math.cos(a) * d, math.sin(a) * d, 0.005), "c_brass", seg=7)
    _goblet("gob", -0.3, 0.25, 0.0)
    cyl("crown", 0.1, 0.06, (0.05, -0.08, 0.47), "c_brass", seg=10, rot=(0.25, 0.1, 0))
    for k in range(5):
        a = k * 2 * math.pi / 5
        cyl("cp%d" % k, 0.018, 0.06, (0.05 + math.cos(a) * 0.1, -0.08 + math.sin(a) * 0.1, 0.53), "c_brass", seg=4, r2=0.0)
    for k, (m, x, y, z) in enumerate((("c_glow_red", 0.25, -0.2, 0.06), ("c_glow_green", -0.2, -0.25, 0.04), ("c_glow_blue", 0.0, 0.2, 0.42))):
        crystal("gem%d" % k, (x, y, z), m, h=0.09, r=0.035)
    node("body")
    export("gold_hoard")


def m_tapestry_horn():
    # red wall hanging with the horned-skull arms of the castle, on an iron rod with finials
    w, h, zc = 0.92, 2.5, 1.62
    ob = box("cloth", (w, 0.02, h), (0, 0.46, zc), "c_tapestry")
    fit_face(ob, 1, -1, -w / 2, w / 2, zc - h / 2, zc + h / 2)
    rod("rod", (-0.55, 0.44, zc + h / 2 + 0.03), (0.55, 0.44, zc + h / 2 + 0.03), 0.018, "e_iron", seg=6)
    for sx in (-1, 1):
        cyl("fin%d" % sx, 0.03, 0.08, (sx * 0.58, 0.44, zc + h / 2 + 0.03), "c_brass", seg=6, r2=0.0, rot=(0, math.radians(90 * sx), 0))
        rod("brk%d" % sx, (sx * 0.4, 0.5, zc + h / 2 + 0.03), (sx * 0.4, 0.43, zc + h / 2 + 0.03), 0.012, "e_iron", seg=4)
    node("body")
    export("tapestry_horn")


def m_bone_pile():
    # gnawed bones and a skull in a corner (prison, guards' lair)
    rnd = random.Random(5)
    for k in range(9):
        a = rnd.random() * 6.28
        x, y = (rnd.random() - 0.5) * 0.6, (rnd.random() - 0.5) * 0.6
        L = 0.2 + rnd.random() * 0.2
        p0 = (x - math.cos(a) * L / 2, y - math.sin(a) * L / 2, 0.03 + (k % 3) * 0.03)
        p1 = (x + math.cos(a) * L / 2, y + math.sin(a) * L / 2, 0.03 + ((k + 1) % 3) * 0.03)
        rod("b%d" % k, p0, p1, 0.022, "e_bone", seg=5)
        cyl("k%da" % k, 0.035, 0.04, p0, "e_bone", seg=6)
        cyl("k%db" % k, 0.035, 0.04, p1, "e_bone", seg=6)
    box("skull", (0.2, 0.24, 0.18), (0.1, -0.05, 0.12), "e_bone", (0, 0, 0.5), bevel=0.06)
    box("jaw", (0.14, 0.1, 0.06), (0.05, -0.15, 0.04), "e_bone", (0, 0, 0.5))
    for sx in (-1, 1):
        box("eye%d" % sx, (0.05, 0.02, 0.05), (0.1 + sx * 0.05, -0.16, 0.14), "c_wick", (0, 0, 0.5))
    node("body")
    export("bone_pile")


def _spin_new(before, ang, centre=(0, 0)):
    """Rotate the objects created since `before` (index into PARTS) about the vertical axis through centre."""
    m = Matrix.Translation((centre[0], centre[1], 0)) @ Matrix.Rotation(ang, 4, 'Z') @ Matrix.Translation((-centre[0], -centre[1], 0))
    for o in PARTS[before:]:
        o.data.transform(m)


def _talon(tag, base, tip, r0, r1, bend, m="c_blacksteel", n=5):
    """Curved claw from base to tip, bowing outwards by `bend` (chain of tapering segments)."""
    b, t = Vector(base), Vector(tip)
    out = Vector((b.x, b.y, 0)).normalized() if (b.x or b.y) else Vector((1, 0, 0))
    pts = []
    for k in range(n + 1):
        u = k / n
        pts.append(b.lerp(t, u) + out * (math.sin(u * math.pi) * bend))
    for k in range(n):
        a, c = pts[k], pts[k + 1]
        ra, rc = r0 + (r1 - r0) * k / n, r0 + (r1 - r0) * (k + 1) / n
        cyl(tag + "%d" % k, ra, (c - a).length + 0.012, tuple((a + c) / 2), m, rot=(c - a).to_track_quat('Z', 'Y').to_euler(), seg=6, r2=rc)


def m_boss_column():
    # healing column of the throne hall (refs: Romanesque cushion capitals with carved masks on round ashlar shafts,
    # brass-banded pier bases; a black iron claw holding an ember crystal). 1 x 3 blocks; the crystal (top block) is the
    # weak spot. Nodes: plinth | shaft, runes, crystal, crack_1, crack_2 (intact) | stub, rubble, shards (broken).
    box("pl0", (0.98, 0.98, 0.12), (0, 0, 0.06), "c_ashlar", bevel=0.02)
    box("pl1", (0.84, 0.84, 0.12), (0, 0, 0.18), "c_ashlar", bevel=0.02)
    box("pl_trim", (0.86, 0.86, 0.025), (0, 0, 0.125), "c_brass")
    torus("pl_torus", 0.31, 0.045, (0, 0, 0.27), "c_ashlar", seg=16, seg2=5)
    cyl("pl_scotia", 0.3, 0.06, (0, 0, 0.33), "c_ashlar", seg=16, r2=0.275)
    for k in range(4):                                     # griffes: leaf spurs from the torus to the plinth corners
        a = math.radians(45 + 90 * k)
        box("griffe%d" % k, (0.2, 0.07, 0.05), (math.cos(a) * 0.38, math.sin(a) * 0.38, 0.255), "c_ashlar", (0, math.radians(-18), a), bevel=0.012)
    node("plinth")
    # shaft: ashlar drums, red brick flutes, brass bands
    cyl("shaft", 0.27, 1.6, (0, 0, 1.16), "c_ashlar", seg=16)
    for k in range(8):
        a = math.radians(k * 45 + 22.5)
        box("flute%d" % k, (0.045, 0.02, 1.44), (math.cos(a) * 0.262, math.sin(a) * 0.262, 1.16), "c_brick", (0, 0, a + math.pi / 2))
    for z in (0.42, 0.95, 1.5, 1.9):
        torus("band%d" % int(z * 100), 0.278, 0.022, (0, 0, z), "c_brass", seg=16, seg2=4)
    for z in (0.95, 1.5):
        for k in range(4):                                 # rivets on the middle bands
            a = math.radians(k * 90)
            cyl("riv%d_%d" % (int(z * 100), k), 0.022, 0.03, (math.cos(a) * 0.3, math.sin(a) * 0.3, z), "c_brass", (0, math.radians(90), a), seg=6)
    # cushion capital (octagon -> square) with four horned masks
    cyl("necking", 0.3, 0.06, (0, 0, 1.99), "c_brass", seg=16)
    cyl("cap", 0.29, 0.26, (0, 0, 2.15), "c_ashlar", rot=(0, 0, math.radians(22.5)), seg=8, r2=0.48)
    for k in range(4):
        i0 = len(PARTS)
        _skull("mk%d_" % k, 0, -0.45, 2.12, s=0.4)
        _spin_new(i0, math.radians(90 * k))
    box("abacus", (0.88, 0.88, 0.1), (0, 0, 2.33), "c_ashlar", bevel=0.015)
    box("abacus_trim", (0.9, 0.9, 0.025), (0, 0, 2.29), "c_brass")
    # iron claw: four talons from the abacus corners curling over the crystal, knuckle rings
    for k in range(4):
        a = math.radians(45 + 90 * k)
        c, s_ = math.cos(a), math.sin(a)
        cyl("socket%d" % k, 0.05, 0.06, (c * 0.34, s_ * 0.34, 2.41), "c_brass", seg=6)
        _talon("tal%d_" % k, (c * 0.34, s_ * 0.34, 2.42), (c * 0.07, s_ * 0.07, 2.98), 0.042, 0.008, 0.12)
        torus("knk%d" % k, 0.045, 0.012, (c * 0.37, s_ * 0.37, 2.62), "c_brass", (math.radians(90), 0, a + math.pi / 2), seg=8, seg2=4)
    cyl("cradle", 0.16, 0.05, (0, 0, 2.41), "c_blacksteel", seg=8, r2=0.1)
    node("shaft")
    # runes: glowing glyph strokes between the bands (dim ember)
    rnd = random.Random(7)
    for k in range(4):
        a = math.radians(k * 90)
        for j in range(4):
            z = 1.05 + j * 0.1
            w = 0.04 + rnd.random() * 0.05
            box("rn%d_%d" % (k, j), (w, 0.012, 0.022), (math.cos(a) * 0.275 + math.sin(a) * (rnd.random() - 0.5) * 0.06,
                                                       math.sin(a) * 0.275 - math.cos(a) * (rnd.random() - 0.5) * 0.06, z),
                "c_ember", (0, rnd.random() * 0.8 - 0.4, a + math.pi / 2))
            box("rv%d_%d" % (k, j), (0.016, 0.012, 0.06), (math.cos(a) * 0.275, math.sin(a) * 0.275, z + 0.04), "c_ember", (0, 0, a + math.pi / 2))
    node("runes")
    # ember crystal (pivot at its centre: bobs and turns)
    crystal("cr_main", (0, 0, 2.62), "c_ember", h=0.84, r=0.2)
    crystal("cr_core", (0, 0, 2.64), "c_ember_core", h=0.5, r=0.1, rot=(0, 0, 0.4))
    for k in range(3):
        a = k * 2.1
        crystal("cr_side%d" % k, (math.cos(a) * 0.11, math.sin(a) * 0.11, 2.5), "c_ember", h=0.24, r=0.05,
                rot=(math.cos(a) * 0.5, math.sin(a) * -0.5, 0))
    node("crystal", pivot=(0, 0, 2.6))
    for stage, pts in ((1, [((0.13, -0.08, 2.7), 0.4), ((0.1, -0.11, 2.61), -0.6), ((0.12, -0.09, 2.52), 0.3)]),
                       (2, [((-0.12, -0.09, 2.74), -0.5), ((-0.14, -0.04, 2.64), 0.7), ((-0.08, 0.12, 2.58), -0.2),
                            ((0.02, 0.15, 2.68), 0.5), ((0.1, 0.1, 2.48), -0.7)])):
        for j, (p, r) in enumerate(pts):
            a = math.atan2(p[1], p[0])
            p = (p[0] * 1.22, p[1] * 1.22, p[2] + 0.02)
            box("ck%d_%d" % (stage, j), (0.014, 0.14, 0.016), p, "c_crack", (r, 0, a))
        node("crack_%d" % stage, pivot=(0, 0, 2.6))
    # ---- broken state: jagged stump, fallen masonry, scattered dim shards
    cyl("stub", 0.27, 0.62, (0, 0, 0.65), "c_ashlar", seg=16)
    torus("stub_band", 0.278, 0.022, (0, 0, 0.42), "c_brass", seg=16, seg2=4)
    for k in range(7):
        a = k * 0.9
        h = 0.08 + (k * 37 % 11) / 11 * 0.22
        box("jag%d" % k, (0.16, 0.12, h), (math.cos(a) * 0.17, math.sin(a) * 0.17, 0.96 + h / 2), "c_ashlar", (0.2 * math.sin(k), 0.25 * math.cos(k), a))
    box("flute_st", (0.045, 0.02, 0.5), (0.262, 0, 0.62), "c_brick", (0, 0, math.pi / 2))
    node("stub")
    rnd = random.Random(3)
    for k in range(9):
        a = rnd.random() * 6.28
        d = 0.35 + rnd.random() * 0.5
        sz = 0.12 + rnd.random() * 0.16
        z = 0.32 if d < 0.42 else sz / 2
        box("rub%d" % k, (sz, sz * 0.8, sz * 0.7), (math.cos(a) * d, math.sin(a) * d, z), "c_ashlar" if k % 3 else "c_brick",
            (rnd.random(), rnd.random(), rnd.random()), bevel=0.01)
    i0 = len(PARTS)
    box("fallen_abacus", (0.6, 0.5, 0.1), (0, 0, 0.05), "c_ashlar", (0, 0, 0.3), bevel=0.012)
    _talon("ftal", (0.15, -0.1, 0.1), (0.5, 0.2, 0.06), 0.04, 0.01, 0.1)
    for o in PARTS[i0:]:
        o.data.transform(Matrix.Translation((0.75, 0.45, 0)))
    node("rubble")
    for k in range(7):
        a = rnd.random() * 6.28
        d = 0.4 + rnd.random() * 0.6
        crystal("sh%d" % k, (math.cos(a) * d, math.sin(a) * d, 0.03), "c_ember_dim", h=0.1 + rnd.random() * 0.08, r=0.03,
                rot=(1.3 + rnd.random() * 0.4, 0, a))
    node("shards")
    export("boss_column")



def m_alchemy_set():
    # Rev 4 item 14: the tabletop/hutch decor of the alchemist's bench as its own 2x2-tall topper block
    # flasks on the hutch shelves (glowing liquids = lamp node), jars, skull
    cols = ["c_glow_red", "c_glow_green", "c_glow_blue", "c_glow_amber"]
    rnd = random.Random(7)
    for k in range(7):
        x = -0.3 + k * 0.26
        r = 0.04 + rnd.random() * 0.03
        cyl("fl%d" % k, r, 0.14, (x, 0.26, 1.29), "e_ice", seg=8, r2=r * 0.5)
        cyl("liq%d" % k, r * 0.9, 0.06, (x, 0.26, 1.25), cols[k % 4], seg=8)
    for k in range(5):
        cyl("jar%d" % k, 0.05, 0.12, (-0.25 + k * 0.32, 0.26, 1.63), "e_ceramic", seg=8)
        cyl("jc%d" % k, 0.04, 0.02, (-0.25 + k * 0.32, 0.26, 1.7), "e_cork", seg=8)
    _skull("sk", 1.2, 0.0, 0.95, 0.35)
    # alembic: round boiler on a little brazier + bent copper pipe to a receiver flask
    cyl("braz", 0.1, 0.08, (-0.15, -0.05, 0.9), "e_iron", seg=8, r2=0.12)
    cyl("boil", 0.12, 0.2, (-0.15, -0.05, 1.05), "c_copper", seg=10, r2=0.07)
    rod("neck", (-0.15, -0.05, 1.15), (-0.15, -0.05, 1.3), 0.025, "c_copper", seg=6)
    rod("pipe", (-0.15, -0.05, 1.3), (0.25, -0.1, 1.0), 0.015, "c_copper", seg=5)
    cyl("recv", 0.07, 0.14, (0.28, -0.1, 0.93), "e_ice", seg=8, r2=0.035)
    cyl("recvl", 0.06, 0.05, (0.28, -0.1, 0.885), "c_glow_green", seg=8)
    box("book", (0.34, 0.26, 0.05), (0.75, -0.12, 0.88), "e_books", (0, 0, 0.3))
    cyl("mortar", 0.07, 0.07, (1.0, -0.2, 0.89), "c_ashlar", seg=8, r2=0.05)
    rod("pestle", (1.0, -0.2, 0.9), (1.05, -0.15, 1.02), 0.012, "c_ashlar", seg=4)
    _candle("cn", 1.25, -0.25, 0.855, h=0.1, r=0.03)
    node("body")
    for k in range(7):
        x = -0.3 + k * 0.26
        cyl("gl%d" % k, 0.025, 0.02, (x, 0.26, 1.29), cols[k % 4], seg=6)
    _flames()
    node("lamp_on")
    export("alchemy_set")


def m_chained_books():
    # Rev 4 item 14: chained folio topper for the bare lectern
    bk = box("book", (0.5, 0.36, 0.035), (0, 0.0, 1.17), "c_pages", (math.radians(-22), 0, 0))
    box("bcov", (0.54, 0.38, 0.015), (0, 0.005, 1.15), "c_velvet", (math.radians(-22), 0, 0))
    for k in range(10):                                      # chain from the book board to a ring on the column
        t0, t1 = k / 10, (k + 0.75) / 10
        a = (0.22, 0.12, 1.19); b = (0.06, 0.08, 0.72)
        sag = lambda t: 0.08 * math.sin(math.pi * t)
        p0 = (a[0] + (b[0] - a[0]) * t0 + sag(t0), a[1] + (b[1] - a[1]) * t0, a[2] + (b[2] - a[2]) * t0)
        p1 = (a[0] + (b[0] - a[0]) * t1 + sag(t1), a[1] + (b[1] - a[1]) * t1, a[2] + (b[2] - a[2]) * t1)
        rod("ch%d" % k, p0, p1, 0.008, "e_iron", seg=4)
    node("body")
    export("chained_books")


def m_kitchen_set():
    # Rev 4 item 14: dishes & food topper for the bare counter
    box("board", (0.36, 0.24, 0.03), (-0.15, 0.05, 0.925), "e_wood_dark")
    cyl("bread", 0.09, 0.16, (-0.15, 0.05, 0.98), "e_produce", (0, math.radians(90), 0), seg=8)
    box("knife", (0.18, 0.025, 0.01), (-0.05, -0.05, 0.945), "e_iron")
    for k, (x, y, h) in enumerate(((0.22, 0.3, 0.2), (0.34, 0.28, 0.14), (0.3, 0.12, 0.16))):
        cyl("jar%d" % k, 0.055, h, (x, y, 0.91 + h / 2), "e_ceramic", seg=8)
        cyl("lid%d" % k, 0.04, 0.02, (x, y, 0.91 + h + 0.01), "e_cork", seg=8)
    cyl("bowl", 0.11, 0.06, (0.12, -0.05, 0.94), "e_plate", seg=10, r2=0.07)
    for k in range(3):
        cyl("fr%d" % k, 0.035, 0.05, (0.08 + k * 0.04, -0.05 + (k % 2) * 0.03, 0.98), "e_produce", seg=6)
    node("body")
    export("kitchen_set")

ALL = {k[2:]: v for k, v in list(globals().items()) if k.startswith("m_") and callable(v)}
for name, fn in ALL.items():
    if ONLY and name not in ONLY:
        continue
    reset()
    fn()
print("MODELS_DONE")
