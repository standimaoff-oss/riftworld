# blender -b X.blend --python preview.py -- out.png [yaw] [pitch]
import bpy, sys, math, mathutils
a = sys.argv[sys.argv.index("--") + 1:]
out = a[0]; yaw = float(a[1]) if len(a) > 1 else 30; pitch = float(a[2]) if len(a) > 2 else 20
sc = bpy.context.scene
for o in sc.objects:
    if o.type == 'MESH' and (o.name.endswith("_dead") or o.name in ("glass_broken",)):
        o.hide_render = True
pts = [o.matrix_world @ mathutils.Vector(c) for o in sc.objects if o.type == 'MESH' and not o.hide_render for c in o.bound_box]
mn = mathutils.Vector([min(p[i] for p in pts) for i in range(3)]); mx = mathutils.Vector([max(p[i] for p in pts) for i in range(3)])
ctr = (mn + mx) / 2; rad = (mx - mn).length / 2
d = mathutils.Vector((math.sin(math.radians(yaw)) * math.cos(math.radians(pitch)), -math.cos(math.radians(yaw)) * math.cos(math.radians(pitch)), math.sin(math.radians(pitch))))
cam = bpy.data.objects.new("C", bpy.data.cameras.new("C")); sc.collection.objects.link(cam)
cam.data.lens = 50
cam.location = ctr + d * rad * 3.2
cam.rotation_euler = (-d).to_track_quat('-Z', 'Y').to_euler()
sc.camera = cam
sc.render.engine = 'BLENDER_WORKBENCH'
sh = sc.display.shading
sh.light = 'STUDIO'; sh.color_type = 'TEXTURE'; sh.show_shadows = True; sh.show_cavity = True
sc.render.resolution_x = 480; sc.render.resolution_y = 400
sc.render.film_transparent = False
w = bpy.data.worlds.new("W"); sc.world = w
sc.render.filepath = out
bpy.ops.render.render(write_still=True)
