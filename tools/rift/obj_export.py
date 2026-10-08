# Export Elisar prop .blend files as Forge OBJ block models (baked into chunk meshes, no block entity).
#   blender -b --factory-startup --python tools/rift/obj_export.py -- name1,name2,...
# Blender: 1 unit = 1 block, master cell centre (0,0), floor z=0, front -Y.  OBJ (Forge): block corner origin, Y up,
# south = +Z  ->  obj = (bx + 0.5, bz, -by + 0.5).  Faces >4 verts are fanned; tris/quads kept.  Writes:
#   models/block/obj/<name>.obj (+ <name>_off.obj without the lamp_on node) and one shared .mtl per model,
#   textures/block/obj/<tex>.png (power-of-two, copied from the material image), Ka 1 = emissive (fullbright).
import math
import bpy, os, sys, math
import numpy as np

A = "/home/user/riftworld/mod/src/main/resources/assets/riftworld"
OBJD = A + "/models/block/obj"
TEXD = A + "/textures/block/obj"
BLEND = "/home/user/models/rift"
os.makedirs(OBJD, exist_ok=True)
os.makedirs(TEXD, exist_ok=True)
NAMES = sys.argv[sys.argv.index("--") + 1].split(",")


import json
TEXSRC = "/home/user/tools/rift/tex_models"
MANI = {}


def material_info(m):
    """-> (texture key, emissive bool)"""
    nt = m.node_tree
    bsdf = nt.nodes.get("Principled BSDF")
    emit = bsdf is not None and bsdf.inputs["Emission Strength"].default_value > 0 and (
        bsdf.inputs["Emission Color"].is_linked or tuple(bsdf.inputs["Emission Color"].default_value)[:3] != (0, 0, 0))
    img = None
    for n in nt.nodes:
        if n.type == "TEX_IMAGE" and n.image is not None:
            img = n.image
            break
    if img is not None:
        base = os.path.splitext(os.path.basename(img.filepath or img.name))[0]
        key = base.lower().replace(".", "_")
        MANI[key] = {"src": os.path.join(TEXSRC, base + ".png")}
        return key, emit
    col = bsdf.inputs["Base Color"].default_value if bsdf else (1, 1, 1, 1)
    key = "c_" + m.name.lower().replace(".", "_")
    MANI[key] = {"col": [float(c) for c in col[:3]]}
    return key, emit


def export(name):
    bpy.ops.wm.open_mainfile(filepath=os.path.join(BLEND, name + ".blend"))
    dg = bpy.context.evaluated_depsgraph_get()
    mats = {}
    lines_all, lines_off = [], []
    vbase, tbase = 1, 1
    out_v, out_t = [], []
    faces_all, faces_off = [], []
    for ob in bpy.context.scene.objects:
        if ob.type != 'MESH':
            continue
        lamp = ob.name.startswith("lamp_on")
        ev = ob.evaluated_get(dg)
        me = ev.to_mesh()
        mw = ob.matrix_world
        uvl = me.uv_layers.active
        vidx = {}
        for v in me.vertices:
            c = mw @ v.co
            out_v.append((c.x + 0.5, c.z, -c.y + 0.5))
            vidx[v.index] = vbase + v.index
        vbase += len(me.vertices)
        for poly in me.polygons:
            m = ob.material_slots[poly.material_index].material if ob.material_slots else None
            if m is None:
                continue
            if m.name not in mats:
                mats[m.name] = material_info(m)
            loops = list(poly.loop_indices)
            ts = []
            raw = [((uvl.data[li].uv[0], 1.0 - uvl.data[li].uv[1]) if uvl else (0.0, 0.0)) for li in loops]
            # atlas sprites do not tile: bring every face's UVs into [0,1] (shift by whole tiles, squeeze if it spans more than one tile)
            cols = []
            for k in (0, 1):
                c = [t[k] for t in raw]
                lo, hi = min(c), max(c)
                off = math.floor(lo + 1e-6)
                c = [x - off for x in c]
                if max(c) > 1.0 + 1e-6:
                    span = hi - lo
                    c = [(x - lo) / span if span > 1 else x - lo for x in [y + off for y in c]]
                cols.append([min(1.0, max(0.0, x)) for x in c])
            for u, v in zip(*cols):
                out_t.append((u, v))
                ts.append(tbase)
                tbase += 1
            vs = [vidx[me.loops[li].vertex_index] for li in loops]
            polys = [list(range(len(vs)))] if len(vs) <= 4 else [[0, i, i + 1] for i in range(1, len(vs) - 1)]
            for pp in polys:
                f = (m.name, [(vs[i], ts[i]) for i in pp])
                faces_all.append(f)
                if not lamp:
                    faces_off.append(f)
        ev.to_mesh_clear()
    mtl = name + ".mtl"
    with open(os.path.join(OBJD, mtl), "w") as f:
        for mn, (key, emit) in mats.items():
            f.write("newmtl %s\nKa %s\nKd 1 1 1\nmap_Kd riftworld:block/obj/%s\n\n" % (mn.replace(" ", "_"), "1 1 1" if emit else "0 0 0", key))

    def write(path, faces):
        with open(path, "w") as f:
            f.write("mtllib %s\no %s\n" % (mtl, name))
            for v in out_v:
                f.write("v %.5f %.5f %.5f\n" % v)
            for t in out_t:
                f.write("vt %.5f %.5f\n" % t)
            cur = None
            for mn, vt in faces:
                if mn != cur:
                    f.write("usemtl %s\n" % mn.replace(" ", "_"))
                    cur = mn
                f.write("f " + " ".join("%d/%d" % p for p in vt) + "\n")
    write(os.path.join(OBJD, name + ".obj"), faces_all)
    has_lamp = len(faces_off) != len(faces_all)
    if has_lamp:
        write(os.path.join(OBJD, name + "_off.obj"), faces_off)
    print("OBJ", name, len(out_v), "v", len(faces_all), "f", "lamp" if has_lamp else "")


for n in NAMES:
    export(n)
_P = "/home/user/tools/rift/obj_textures.json"
_OLD = json.load(open(_P)) if os.path.exists(_P) else {}
_OLD.update(MANI)
json.dump(_OLD, open(_P, "w"), indent=0)
print("OBJ_DONE")
