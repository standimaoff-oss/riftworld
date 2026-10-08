"""Make the block-atlas textures listed by obj_export.py (power-of-two copies of model textures, 4x4 solid colours)."""
import json, os
from PIL import Image
TEXD = "/home/user/riftworld/mod/src/main/resources/assets/riftworld/textures/block/obj"
os.makedirs(TEXD, exist_ok=True)


def pot(n):
    p = 1
    while p < n:
        p *= 2
    return p


M = json.load(open("/home/user/tools/rift/obj_textures.json"))
for key, v in M.items():
    out = os.path.join(TEXD, key + ".png")
    if "src" in v:
        im = Image.open(v["src"]).convert("RGBA")
        W, H = pot(im.width), pot(im.height)
        if (W, H) != im.size:
            im = im.resize((W, H), Image.NEAREST)
        im.save(out)
    else:
        # blender colours are linear -> sRGB
        c = [int(round(255 * (x * 12.92 if x <= 0.0031308 else 1.055 * x ** (1 / 2.4) - 0.055))) for x in v["col"]]
        Image.new("RGBA", (4, 4), tuple(max(0, min(255, k)) for k in c) + (255,)).save(out)
print("obj textures", len(M))
