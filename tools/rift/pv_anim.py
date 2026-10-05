# blender -b X.blend --python pv_anim.py -- outprefix yaw pitch clip:t1,t2,... [clip:...]
# Renders workbench stills of an armature clip at the given times (seconds) -> outprefix_<clip>_<i>.png
import bpy, sys, math, mathutils
a = sys.argv[sys.argv.index("--") + 1:]
pre, yaw, pitch = a[0], float(a[1]), float(a[2])
sc = bpy.context.scene
arm = [o for o in sc.objects if o.type == 'ARMATURE'][0]
for tr in arm.animation_data.nla_tracks:
    tr.mute = True
arm.animation_data.action = None
sc.frame_set(0)
bpy.context.view_layer.update()
pts = [o.matrix_world @ mathutils.Vector(c) for o in sc.objects if o.type == 'MESH' for c in o.bound_box]
mn = mathutils.Vector([min(p[i] for p in pts) for i in range(3)]); mx = mathutils.Vector([max(p[i] for p in pts) for i in range(3)])
ctr = (mn + mx) / 2
ctr.z = 1.3 if mx.z > 2.5 else 0.9
rad = max((mx - mn).length / 2, 1.6)
d = mathutils.Vector((math.sin(math.radians(yaw)) * math.cos(math.radians(pitch)), -math.cos(math.radians(yaw)) * math.cos(math.radians(pitch)), math.sin(math.radians(pitch))))
cam = bpy.data.objects.new("C", bpy.data.cameras.new("C")); sc.collection.objects.link(cam)
cam.data.lens = 50
cam.location = ctr + d * rad * float(__import__("os").environ.get("PVD", "3.0"))
cam.rotation_euler = (-d).to_track_quat('-Z', 'Y').to_euler()
sc.camera = cam
# floor plane for reference
bpy.ops.mesh.primitive_plane_add(size=6, location=(0, -1, 0))
sc.render.engine = 'BLENDER_WORKBENCH'
sh = sc.display.shading
sh.light = 'STUDIO'; sh.color_type = 'TEXTURE'; sh.show_shadows = False; sh.show_cavity = True
sc.render.resolution_x = 320; sc.render.resolution_y = 360
w = bpy.data.worlds.new("W"); sc.world = w
for spec in a[3:]:
    clip, ts = spec.split(":")
    arm.animation_data.action = bpy.data.actions[clip]
    try:
        arm.animation_data.action_slot = bpy.data.actions[clip].slots[0]
    except Exception:
        pass
    for i, t in enumerate(ts.split(",")):
        sc.frame_set(int(round(float(t) * 20)))
        sc.render.filepath = "%s_%s_%d.png" % (pre, clip, i)
        bpy.ops.render.render(write_still=True)
print("PV_DONE")
