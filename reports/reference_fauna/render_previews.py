#!/usr/bin/env python3
"""Software previews of the actual generated source rigs (not game screenshots)."""
import json
from pathlib import Path
import sys
import numpy as np
from PIL import Image
HERE=Path(__file__).resolve().parent
sys.path.insert(0,str(HERE.parent/'el_gran_maja_model'))
from render_el_gran_maja_shots import load_posed_quads,render_scene,make_underwater_bg

def main():
    w,h=1100,650
    for name in ('liopleurodon','pegasus'):
        geo=json.loads((HERE/(name+'.blockbench.geo.json')).read_text())
        tex=np.asarray(Image.open(HERE/(name+'.png')).convert('RGB'))
        # Preview the ordinary unsaddled horse and its raised, resting wings.
        if name == 'pegasus':
            geo['minecraft:geometry'][0]['bones'] = [b for b in geo['minecraft:geometry'][0]['bones'] if b['name'] != 'saddle']
            quads=load_posed_quads(geo,pose_overrides={'wing_l':(0,0,30),'wing_r':(0,0,-30)})
        else:
            quads=load_posed_quads(geo)
        if name=='liopleurodon':
            bg=make_underwater_bg(w,h)
            img=render_scene(quads,tex,w,h,(-280,145,-180),(0,0,-4),ortho_half_h=68,bg_img=bg,
                             fog_color=None,ssaa=1)
        else:
            # Neutral sky gradient, not a photograph or Minecraft capture.
            y=np.linspace(0,1,h)[:,None,None]
            top=np.array([190,208,225])[None,None,:];bottom=np.array([226,221,213])[None,None,:]
            bg=np.broadcast_to(top*(1-y)+bottom*y,(h,w,3)).copy()
            img=render_scene(quads,tex,w,h,(-125,105,-100),(0,30,0),ortho_half_h=56,bg_img=bg,
                             fog_color=None,ssaa=1)
        img.save(HERE/(name+'_software_preview.png'))
        print(name,len(quads),'faces')
if __name__=='__main__': main()
