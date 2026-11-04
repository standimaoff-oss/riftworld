#!/usr/bin/env python3
"""Build two new articulated, reference-guided Minecraft models and their loot.

Source coordinates are Y-up, nose toward -Z, one unit is 1/16 block.
The game export flips Y; its dedicated renderers flip Y back, just like Maja.
The source Bedrock geometry is directly importable into Blockbench. Repeated
identical parts intentionally share UV nets to keep texture and draw costs low.
"""
from __future__ import annotations
import hashlib
import json
import math
import random
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'mod/src/main/resources/assets/riftworld'
REPORT = ROOT / 'reports/reference_fauna'
REPORT.mkdir(parents=True, exist_ok=True)

# top, side, front, underside (all in the reference's restrained palette)
MATS = {
    'lio_back': ('#536979','#3c505f','#495d6a','#70818a'),
    'lio_side': ('#506473','#435866','#465a68','#89969b'),
    'lio_belly': ('#84959b','#9daaaa','#87979a','#b5c0bd'),
    'lio_fin': ('#506777','#3d5363','#415665','#85979a'),
    'lio_fin_edge': ('#6e8290','#546a78','#536a78','#9baeb0'),
    'scar': ('#c0b9a4','#a5a695','#bcb5a1','#8c9996'),
    'mouth': ('#463c3d','#40383a','#43393a','#514548'),
    'gum': ('#8a6965','#725652','#6b504d','#8a6e67'),
    'tooth': ('#ded8c3','#c4c2b2','#e7e4d5','#b5b7a9'),
    'lio_eye': ('#111c24','#101b22','#101b22','#101b22'),
    'glint': ('#ced9d6','#ced9d6','#ced9d6','#ced9d6'),
    'coat': ('#f4f3ef','#e7e7e4','#efefec','#c9cbc9'),
    'coat_light': ('#faf9f5','#f1f1ed','#f8f7f3','#dbddda'),
    'coat_shade': ('#e3e4e1','#d5d7d4','#e6e7e3','#bfc3c0'),
    'mane': ('#f4f4f0','#dfdfdb','#f2f2ef','#c5c9c6'),
    'mane_dark': ('#dbdeda','#c9cecb','#e2e5e0','#afb7b3'),
    'wing': ('#fbfaf6','#e7e8e4','#f4f4ef','#c9cecb'),
    'wing_shadow': ('#e7eae6','#cdd2cf','#e1e4df','#aeb7b3'),
    'wing_tip': ('#f7f7f2','#dedfdb','#f1f2ed','#bfc7c3'),
    'hoof': ('#4d4c48','#343432','#454440','#252625'),
    'muzzle': ('#bbbdb8','#a4a8a3','#c4c6c1','#8d9390'),
    'eye': ('#242a29','#111816','#101513','#181e1b'),
    'eye_light': ('#f1f3ee','#f1f3ee','#f1f3ee','#f1f3ee'),
    'saddle': ('#886644','#6b4c33','#94754d','#483629'),
    'leather': ('#604833','#483629','#5d4531','#392a20'),
    'gold': ('#d7b86f','#ae8d49','#d3b363','#8c7039'),
}


def faces(u, v, w, h, d):
    return {'up': (u+d,v,w,d), 'down': (u+d+w,v,w,d),
            'east': (u,v+d,d,h), 'north': (u+d,v+d,w,h),
            'west': (u+d+w,v+d,d,h), 'south': (u+2*d+w,v+d,w,h)}


def rgb(color):
    return tuple(int(color[k:k+2],16) for k in (1,3,5))


class Model:
    def __init__(self, name, scale):
        self.name, self.scale, self.bones, self.by_name = name, scale, [], {}

    def bone(self, name, pivot, parent=None, anim=None):
        assert name not in self.by_name and (parent is None or parent in self.by_name)
        b = {'name':name, 'pivot':list(pivot), 'parent':parent, 'anim':anim, 'cubes':[]}
        self.bones.append(b); self.by_name[name] = b

    def box(self, bone, origin, size, material, variant=0):
        assert material in MATS and len(origin)==len(size)==3 and all(isinstance(n,int) and n>0 for n in size)
        c = {'origin':list(origin), 'size':list(size), 'mat':material, 'variant':variant}
        self.by_name[bone]['cubes'].append(c)

    def pair(self, bone, x, y, z, w, h, d, mat, variant=0):
        self.box(bone, [x,y,z], [w,h,d], mat, variant)
        self.box(bone, [-x-w,y,z], [w,h,d], mat, variant)

    def export(self):
        # Use one UV net for each (material, size, variation), not one for each
        # identical tooth/feather. Units remain one texel per model unit.
        templates = {}
        for b in self.bones:
            for c in b['cubes']:
                key=(c['mat'], tuple(c['size']), c['variant'])
                templates[key] = (2*c['size'][2]+2*c['size'][0], c['size'][2]+c['size'][1])
        atlas=512
        while True:
            skyline=[0]*atlas; placed={}; failed=False
            for key,(w,h) in sorted(templates.items(),key=lambda it:(-it[1][1],-it[1][0],str(it[0]))):
                w+=4; h+=4
                best=None
                for x in range(atlas-w+1):
                    y=max(skyline[x:x+w])
                    if y+h <= atlas and (best is None or y<best[1]):
                        best=(x,y)
                        if y==0: break
                if best is None: failed=True; break
                x,y=best
                for i in range(x,x+w): skyline[i]=y+h
                placed[key]=(x+2,y+2)
            if not failed: break
            atlas*=2
            if atlas>1024: raise RuntimeError('texture budget exceeded')
        image=Image.new('RGBA',(atlas,atlas),(0,0,0,0)); d=ImageDraw.Draw(image)
        for key,(u,v) in placed.items():
            mat,size,variant=key; w,h,dep=size
            seed=int.from_bytes(hashlib.sha256(repr(key).encode()).digest()[:8],'big')
            rng=random.Random(seed)
            for face,(fx,fy,fw,fh) in faces(u,v,w,h,dep).items():
                color=rgb(MATS[mat][0 if face=='up' else 3 if face=='down' else 2 if face in ('north','south') else 1])
                d.rectangle((fx,fy,fx+fw-1,fy+fh-1),fill=(*color,255))
                # Low-contrast irregular pixel clusters, never a noisy overlay.
                if mat in ('lio_back','lio_side','lio_fin','lio_fin_edge','lio_belly'):
                    for _ in range(int(fw*fh*.025)):
                        xx=fx+rng.randrange(fw); yy=fy+rng.randrange(fh)
                        f=rng.choice((.83,.88,1.08,1.12))
                        c=tuple(max(0,min(255,int(q*f))) for q in color)
                        d.rectangle((xx,yy,min(fx+fw-1,xx+rng.randrange(1,4)),min(fy+fh-1,yy+rng.randrange(1,3))),fill=(*c,255))
                elif mat in ('coat','coat_shade'):
                    for _ in range(int(fw*fh*.012)):
                        xx=fx+rng.randrange(fw); yy=fy+rng.randrange(fh)
                        c=tuple(int(q*.90) for q in color)
                        d.rectangle((xx,yy,min(fx+fw-1,xx+1),min(fy+fh-1,yy+1)),fill=(*c,255))
                elif mat.startswith('wing') or mat.startswith('mane'):
                    # Fine broken barbs/strands; the shape itself remains 3D.
                    for xx in range(fx+2,fx+fw,5):
                        c=tuple(int(q*.91) for q in color)
                        start=fy+rng.randrange(max(1,fh//3))
                        d.line((xx,start,xx,min(fy+fh-1,start+max(2,fh//2))),fill=(*c,255),width=1)
        source=[]; runtime=[]
        for b in self.bones:
            src={'name':b['name'],'pivot':b['pivot'],'cubes':[]}
            dst={'name':b['name'],'pivot':[b['pivot'][0],-b['pivot'][1],b['pivot'][2]],'cubes':[]}
            if b['parent']: src['parent']=dst['parent']=b['parent']
            if b['anim']:
                src['anim']=dict(b['anim']); dst['anim']=dict(b['anim'])
                if dst['anim'].get('axis') in ('x','z'): dst['anim']['amp']=-dst['anim']['amp']
            for c in b['cubes']:
                x,y,z=c['origin']; w,h,dep=c['size']; uv=list(placed[(c['mat'],tuple(c['size']),c['variant'])])
                src['cubes'].append({'origin':[x,y,z],'size':[w,h,dep],'uv':uv})
                dst['cubes'].append({'origin':[x,-y-h,z],'size':[w,h,dep],'uv':uv})
            source.append(src); runtime.append(dst)
        game={'texture_width':atlas,'texture_height':atlas,'render_scale':self.scale,'translucent':False,'bones':runtime}
        bb={'format_version':'1.12.0','minecraft:geometry':[{'description':{
            'identifier':'geometry.riftworld.'+self.name,'texture_width':atlas,'texture_height':atlas,
            'visible_bounds_width':16,'visible_bounds_height':8,'visible_bounds_offset':[0,1,0]},'bones':source}]}
        (ASSETS/'geo'/f'{self.name}.json').write_text(json.dumps(game,indent=1)+'\n')
        image.save(ASSETS/'textures/entity'/f'{self.name}.png',compress_level=9)
        (REPORT/f'{self.name}.blockbench.geo.json').write_text(json.dumps(bb,indent=1)+'\n')
        (REPORT/f'{self.name}.png').write_bytes((ASSETS/'textures/entity'/f'{self.name}.png').read_bytes())
        cubes=[c for b in self.bones for c in b['cubes']]
        bounds=[[min(c['origin'][i] for c in cubes),max(c['origin'][i]+c['size'][i] for c in cubes)] for i in range(3)]
        stats={'bones':len(self.bones),'cuboids':len(cubes),'unique_uv_nets':len(placed),'atlas':atlas,
               'scale':self.scale,'bounds_units':bounds,
               'dimensions_blocks':[round((hi-lo)*self.scale/16,2) for lo,hi in bounds]}
        (REPORT/f'{self.name}.stats.json').write_text(json.dumps(stats,indent=2)+'\n')
        print(self.name,stats)


def liopleurodon():
    m=Model('liopleurodon',.80)
    m.bone('body',[0,0,0])
    # Shoulder, long barrel and taper are layered on a stepped elliptical section.
    for z,d,w,h in [(-60,22,30,21),(-40,25,38,24),(-16,26,40,25),(9,25,37,24),(33,24,30,21),(56,19,23,17)]:
        m.box('body',[-w//2,-h//2,z],[w,h,d],'lio_side',z%2)
        m.box('body',[-(w-8)//2,h//2-1,z+1],[w-8,4,max(1,d-2)],'lio_back')
        m.box('body',[-(w-7)//2,-h//2-3,z+1],[w-7,4,max(1,d-2)],'lio_belly')
        # A pale belly extends up the lower flanks, as in the reference.
        m.pair('body',w//2+0.15,-h//2,z+1,1,5,max(1,d-2),'lio_belly')
    # Dark upper flanks, pale jagged transition to the belly as in the concept.
    for z in range(-43,60,13):
        w=19 if z<20 else (17 if z<40 else 13)
        m.pair('body',w-1,-7,z,2,4,10,'lio_belly',(z//13)&1)
    # Four crooked old ivory scars on the back and flanks.
    for z,side in [(-22,1),(2,-1),(28,1),(47,-1)]:
        for i in range(5):
            x=side*(6+i*2)
            m.box('body',[min(x,x+side),11-i//3,z+i*2],[2,1,3],'scar')
    # A few longer, crooked healed scars descend over the dark flanks.
    for z,edge,side in [(-28,19,1),(-28,19,-1),(2,20,-1),(23,19,1),(43,15,-1)]:
        for i in range(5):
            x=edge+0.2 if side>0 else -edge-1.2
            m.box('body',[x,7-i*2,z+i*2],[1,2,3],'scar')
    m.bone('head',[0,0,-56],'body',{'axis':'y','amp':2.3,'speed':.65,'phase':.1})
    m.box('head',[-16,-10,-75],[32,21,20],'lio_side')
    m.box('head',[-12,9,-74],[24,4,20],'lio_back')
    m.box('head',[-14,-5,-95],[28,12,22],'lio_side')
    m.box('head',[-12,5,-95],[24,4,21],'lio_back')
    m.box('head',[-10,-4,-116],[20,9,23],'lio_side')
    m.box('head',[-8,4,-115],[16,3,20],'lio_back')
    m.box('head',[-10,-5,-116],[20,2,39],'gum')
    # Black mouth cavity recessed between the two jaws, and raised eyes/nostrils.
    m.box('head',[-12,-6,-95],[24,1,25],'mouth')
    m.pair('head',15,2,-72,1,4,5,'lio_eye')
    m.pair('head',16,4,-71,1,1,1,'glint')
    m.pair('head',7,3,-114,2,2,2,'lio_eye')
    # Upper pointed teeth, separate roots and tips for an uneven predatory edge.
    for side in (-1,1):
        for i in range(12):
            z=-113+i*3
            edge=9 if z < -96 else (12 if z < -76 else 14)
            x=edge-1 if side>0 else -edge-1
            h=3+(i%3==0)
            m.box('head',[x,-5-h,z],[2,h,2],'tooth',i%2)
            m.box('head',[x+(1 if side<0 else 0),-6-h,z],[1,1,1],'tooth')
    m.bone('jaw',[0,-5,-70],'head',{'axis':'x','amp':2.8,'speed':.48,'phase':1.0})
    m.box('jaw',[-12,-12,-96],[24,6,26],'lio_belly')
    m.box('jaw',[-9,-11,-117],[18,5,23],'lio_belly')
    m.box('jaw',[-11,-6,-96],[22,1,26],'gum')
    m.box('jaw',[-8,-6,-116],[16,1,21],'gum')
    for side in (-1,1):
        for i in range(10):
            z=-111+i*4
            edge=8 if z < -96 else 10
            x=edge-1 if side>0 else -edge-1
            m.box('jaw',[x,-5,z],[2,3+(i%4==0),2],'tooth',i%2)
    # Four broad, slightly rear-swept paddles, each with its own swimming joint.
    for front,z in ((True,-37),(False,30)):
        for side,label in ((1,'l'),(-1,'r')):
            name=('fin_front_' if front else 'fin_rear_')+label
            m.bone(name,[side*15,-4,z+7],'body',{'axis':'z','amp':side*(12 if front else 9),'speed':1.0,'phase':0 if front else .9})
            for x,y,zz,w,h,d,mat in [
                (12,-8,z,15,7,24,'lio_fin'),(24,-12,z+2,15,6,23,'lio_fin'),
                (37,-16,z+4,12,5,19,'lio_fin'),(48,-19,z+7,9,4,14,'lio_fin_edge')]:
                if not front: x=12+(x-12)*4//5; w=max(4,w*4//5)
                xx=x if side>0 else -x-w
                m.box(name,[xx,y,zz],[w,h,d],mat)
            for j in range(3):
                x=25+j*7
                if not front: x=12+(x-12)*4//5
                xx=x if side>0 else -x-5
                m.box(name,[xx,(-3.85 if j<2 else -9.85),z+4+j*2],[5,1,10-j*2],'lio_fin_edge',j%2)
    m.bone('tail_1',[0,0,70],'body',{'axis':'y','amp':12,'speed':1.1,'phase':.4})
    for z,d,w,h in [(73,13,20,15),(85,12,15,13),(96,12,10,10)]:
        m.box('tail_1',[-w//2,-h//2,z],[w,h,d],'lio_side')
        m.box('tail_1',[-max(2,w//2-2),-h//2-2,z],[max(4,w-4),3,d],'lio_belly')
    m.bone('tail_tip',[0,0,104],'tail_1',{'axis':'y','amp':20,'speed':1.1,'phase':1.3})
    m.box('tail_tip',[-4,-4,105],[8,8,9],'lio_side')
    for i in range(4):
        m.box('tail_tip',[-2,3+i*3,108+i],[4,4,6-i],'lio_fin_edge')
    m.export()


def pegasus():
    m=Model('pegasus',.90)
    m.bone('body',[0,26,0])
    # Deep horse barrel, rounded shoulders and muscular hindquarters.
    m.box('body',[-7,19,-14],[14,13,29],'coat')
    m.box('body',[-6,29,-13],[12,5,27],'coat_light')
    m.box('body',[-6,16,-11],[12,4,23],'coat_shade')
    m.box('body',[-8,20,-14],[16,12,8],'coat')
    m.box('body',[-8,20,7],[16,12,8],'coat')
    m.pair('body',7,20,-12,2,10,8,'coat_shade')
    m.pair('body',7,20,8,2,10,7,'coat_shade')
    m.bone('neck',[0,30,-12],'body',{'axis':'x','amp':2,'speed':.45,'phase':0})
    m.box('neck',[-5,29,-21],[10,12,11],'coat')
    m.box('neck',[-4,34,-25],[8,12,10],'coat_light')
    m.box('neck',[-4,40,-28],[8,7,8],'coat')
    # Layered mane flowing down both sides of the crest.
    for i in range(9):
        z=-25+i*2; y=41-i
        m.box('neck',[-2,y,z],[4,5,3],'mane',i%2)
        for side in (-1,1):
            x=4 if side>0 else -6
            m.box('neck',[x,y-5-i//3,z],[2,6+i//3,3],'mane' if i%3 else 'mane_dark',i%2)
    m.bone('head',[0,42,-26],'neck',{'axis':'x','amp':1.8,'speed':.7,'phase':.7})
    m.box('head',[-4,38,-31],[8,9,8],'coat')
    m.box('head',[-3,35,-37],[6,7,8],'coat_light')
    m.box('head',[-3,34,-39],[6,4,3],'muzzle')
    m.pair('head',4,42,-30,1,3,3,'coat_shade')
    m.pair('head',5,43,-30,1,2,2,'eye')
    m.pair('head',2,35,-39,1,1,2,'eye')
    m.pair('head',2,47,-28,2,6,3,'coat')
    m.pair('head',2,49,-29,1,3,1,'muzzle')
    for i in range(5):
        m.box('head',[-3+i,47-i//2,-32+i],[2,3,4],'mane')
    # Individually jointed legs: alternating walking gait comes from renderer.
    for front,z in ((True,-10),(False,10)):
        for side,label in ((1,'l'),(-1,'r')):
            name=('leg_front_' if front else 'leg_rear_')+label
            x=side*5
            m.bone(name,[x,21,z],'body')
            m.box(name,[x-2,11,z-2],[4,11,5],'coat')
            m.box(name,[x-2,7,z-2],[4,5,4],'coat_shade')
            m.box(name,[x-1,2,z-1],[3,7,3],'coat_light')
            m.box(name,[x-2,0,z-2],[5,3,5],'hoof')
            m.box(name,[x-2,2,z-2],[5,1,5],'coat_shade')
    m.bone('tail',[0,29,13],'body',{'axis':'y','amp':5,'speed':.65,'phase':.4})
    m.box('tail',[-3,23,13],[6,7,6],'coat_shade')
    for i in range(7):
        x=-4+i
        m.box('tail',[x,17+(i%2),18],[2,8,4],'mane',i%2)
        m.box('tail',[x,10+i%3,21+(i%2)],[2,8,4],'mane' if i%3 else 'mane_dark',i%2)
        m.box('tail',[x,8+i%3,24+(i%2)],[2,4,3],'mane')
    # Each feather is a three-segment, tapering volume. All wings are a single
    # bone per side rather than hundreds of animated tiny joints.
    for side,label in ((1,'l'),(-1,'r')):
        name='wing_'+label
        m.bone(name,[side*7,32,-5],'body')
        def sidebox(x,y,z,w,h,d,mat,variant=0):
            m.box(name,[x if side>0 else -x-w,y,z],[w,h,d],mat,variant)
        sidebox(7,30,-10,13,7,19,'wing_shadow')
        sidebox(9,35,-10,16,4,19,'wing')
        sidebox(17,37,-9,11,4,21,'wing')
        for i in range(11):
            z=-10+i*3
            # Outer primary tips form a fan: long near the front, shorter at rear.
            extra=int(9*math.sin(math.pi*(i+1)/12))
            sidebox(18,34,z,13,3,3,'wing',i%2)
            sidebox(29,37,z+1,11+extra//2,3,3,'wing',i%2)
            sidebox(39+extra//2,40,z+2,16+extra,2,2,'wing_tip',i%2)
            # Fine raised shafts separate the primaries instead of leaving a
            # featureless white plane; all of these still share UV templates.
            sidebox(30,40.15,z+1,10+extra//2,1,1,'wing_shadow',i%2)
        # Two rows of overlapping secondary coverts form the layered root.
        for i in range(8):
            sidebox(10+i,37,-9+i*3,12,2,3,'wing',i%2)
            sidebox(16+i,34,-7+i*3,9,2,3,'wing_shadow',i%2)
    # Saddle exists in the rig but is only shown when the entity is saddled.
    m.bone('saddle',[0,32,-3],'body')
    m.box('saddle',[-6,33,-10],[12,2,15],'saddle')
    m.box('saddle',[-5,35,-9],[10,1,12],'leather')
    m.box('saddle',[-5,34,3],[10,3,2],'saddle')
    m.pair('saddle',6,26,-8,1,9,2,'leather')
    m.pair('saddle',7,25,-8,1,2,2,'gold')
    m.export()


def loot():
    out=ASSETS/'textures/item'; models=ASSETS/'models/item'
    out.mkdir(exist_ok=True,parents=True); models.mkdir(exist_ok=True,parents=True)
    # Hand-drawn 32px silhouettes: high contrast outline, faceted interior.
    im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
    d.polygon([(8,29),(11,22),(10,15),(6,11),(5,5),(7,4),(9,9),(13,12),(14,7),(13,3),(16,2),(18,9),(18,12),(22,9),(24,3),(27,3),(25,11),(21,15),(20,20),(26,17),(27,12),(30,12),(29,19),(22,23),(19,30)],fill='#51493d')
    d.polygon([(10,28),(13,22),(12,14),(8,10),(7,5),(8,9),(15,14),(16,8),(15,3),(17,9),(16,16),(23,11),(25,4),(26,4),(24,12),(19,16),(18,23),(26,19),(28,14),(28,19),(20,22),(17,29)],fill='#d0c2a1')
    d.line([(12,27),(15,22),(15,15),(20,12),(23,10)],fill='#f2e7c9',width=1)
    d.line([(16,25),(18,22),(22,20)],fill='#f2e7c9',width=1)
    im.save(out/'wendigo_antler.png')
    im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
    d.polygon([(3,29),(8,24),(10,19),(13,15),(14,9),(18,5),(26,2),(29,2),(29,7),(25,12),(23,17),(19,20),(16,23),(10,25),(5,30)],fill='#7c8888')
    d.polygon([(5,28),(10,23),(11,17),(15,14),(16,8),(20,5),(28,3),(26,8),(23,11),(24,12),(20,15),(21,17),(16,20),(16,22),(10,24)],fill='#f4f4ef')
    d.line([(5,29),(24,8),(28,3)],fill='#cbd3cf',width=2)
    for a,b in [((12,17),(10,16)),((15,14),(12,12)),((18,11),(15,9)),((21,8),(19,6)),((14,21),(19,19)),((17,18),(21,16)),((20,15),(24,13))]:
        d.line((a,b),fill='#b8c2bf',width=1)
    im.save(out/'pegasus_feather.png')
    im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
    d.polygon([(7,28),(8,22),(10,18),(11,14),(14,9),(19,5),(23,3),(22,7),(19,11),(19,16),(17,21),(15,26),(14,29),(10,30)],fill='#756d5a')
    d.polygon([(9,27),(10,22),(12,17),(13,13),(16,8),(21,5),(19,9),(17,13),(17,18),(15,24),(13,28)],fill='#e0dac4')
    d.line([(10,25),(12,18),(14,13),(17,9)],fill='#f7f3df',width=1)
    d.line([(10,27),(13,28),(15,26)],fill='#a9a38c',width=1)
    im.save(out/'liopleurodon_tooth.png')
    for name in ('wendigo_antler','pegasus_feather','liopleurodon_tooth'):
        (models/f'{name}.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'riftworld:item/'+name}},indent=2)+'\n')

if __name__=='__main__':
    liopleurodon(); pegasus(); loot()
