from PIL import Image, ImageDraw
from pathlib import Path
import random, json
root=Path('/home/user/riftworld/mod/src/main/resources/assets/riftworld')
tex=root/'textures'
(tex/'item').mkdir(parents=True,exist_ok=True); (tex/'block').mkdir(parents=True,exist_ok=True); (tex/'models/armor').mkdir(parents=True,exist_ok=True)
# Hand-authored pixel palettes inspired by the supplied Aurite and Riftcrystal concepts.
PALETTES={
 'aurite': {'dark':'#38220f','shadow':'#684016','base':'#b87520','light':'#e6ad3d','shine':'#ffe078','accent':'#20c4b5','edge':'#1c160f'},
 'riftcrystal': {'dark':'#10171d','shadow':'#26343d','base':'#087f86','light':'#21c3c0','shine':'#a2fff1','accent':'#db48e7','edge':'#080b10'}
}
def rgb(s):
 s=s.lstrip('#'); return tuple(int(s[i:i+2],16) for i in (0,2,4))+(255,)
def texture_item(name, material, kind):
 p=PALETTES[material]; im=Image.new('RGBA',(16,16),(0,0,0,0)); d=ImageDraw.Draw(im)
 if kind in ('ingot','gem'):
  cols=[p['edge'],p['dark'],p['shadow'],p['base'],p['light'],p['shine']]
  # Faceted, beveled metal slab / crystalline shard.
  if kind=='ingot':
   d.polygon([(3,5),(10,3),(14,6),(13,12),(6,14),(2,11)],fill=rgb(p['shadow']))
   d.polygon([(3,5),(10,3),(12,5),(6,7),(2,9)],fill=rgb(p['shine']))
   d.polygon([(2,9),(6,7),(6,12),(3,11)],fill=rgb(p['base']))
   d.polygon([(6,7),(12,5),(13,10),(6,12)],fill=rgb(p['light']))
   d.line([(4,5),(10,4)],fill=rgb(p['shine']))
  else:
   d.polygon([(7,1),(10,5),(14,6),(11,10),(10,15),(6,12),(2,11),(5,7),(4,4)],fill=rgb(p['dark']))
   d.polygon([(7,1),(9,6),(5,7),(4,4)],fill=rgb(p['shine']))
   d.polygon([(9,6),(14,6),(10,10)],fill=rgb(p['light']))
   d.polygon([(5,7),(9,6),(10,10),(6,12),(2,11)],fill=rgb(p['base']))
   d.polygon([(10,10),(14,6),(12,12),(10,15)],fill=rgb(p['accent']))
   d.point((7,5),fill=rgb(p['shine']))
 elif kind in ('sword','pickaxe','axe','shovel'):
  # stepped pixel-art tool silhouettes, dark outline then metallic face highlights.
  d.line([(3,14),(12,5)],fill=rgb(p['edge']),width=4)
  d.line([(4,13),(11,6)],fill=rgb(p['dark']),width=2)
  d.line([(4,13),(10,7)],fill=rgb(p['base']),width=1)
  if kind=='sword':
   d.polygon([(10,1),(14,5),(11,9),(8,6)],fill=rgb(p['edge'])); d.polygon([(10,2),(13,5),(11,7),(9,6)],fill=rgb(p['light'])); d.line([(8,8),(12,12)],fill=rgb(p['accent']),width=2)
  elif kind=='pickaxe':
   d.line([(4,3),(7,1),(12,2),(14,5),(12,6),(9,4),(6,6)],fill=rgb(p['edge']),width=3); d.line([(5,3),(8,2),(12,3)],fill=rgb(p['light']),width=1)
  elif kind=='axe':
   d.polygon([(8,2),(13,1),(15,4),(14,8),(10,8),(8,6)],fill=rgb(p['edge'])); d.polygon([(9,3),(13,2),(14,4),(13,6),(10,6)],fill=rgb(p['light']))
  else:
   d.polygon([(10,1),(14,2),(13,7),(11,9),(8,7)],fill=rgb(p['edge'])); d.polygon([(11,2),(13,3),(12,6),(10,7)],fill=rgb(p['light']))
  d.line([(3,13),(5,15)],fill=rgb(p['accent']),width=1)
 elif kind=='ore':
  pass
 im.save(tex/'item'/f'{name}.png')

# Block texture: subdued rock with restrained luminous mineral flecks; low-res vanilla-compatible 16x16.
for material, name in [('riftcrystal','riftcrystal_ore')]:
 p=PALETTES[material]; random.seed(441)
 im=Image.new('RGBA',(16,16)); px=im.load()
 stone=['#3a3e45','#444952','#50545a','#343941','#5a5e63']
 for y in range(16):
  for x in range(16): px[x,y]=rgb(random.choice(stone))
 d=ImageDraw.Draw(im)
 for coords in [[(3,3),(4,3),(4,4),(5,4)],[(11,2),(12,3),(12,4)],[(8,8),(9,8),(9,9),(10,9),(10,10)],[(2,11),(3,10),(4,10)],[(13,12),(12,13),(13,14)]]:
  for i,(x,y) in enumerate(coords):
   d.point((x,y),fill=rgb([p['dark'],p['accent'],p['light'],p['shine']][min(i,3)]))
 im.save(tex/'block'/f'{name}.png')

# Tool / material item icons.
for name,kind in [('aurite_ingot','ingot'),('riftcrystal','gem')]: texture_item(name,'aurite' if name.startswith('aurite') else 'riftcrystal',kind)
for mat in PALETTES:
 for kind in ('sword','pickaxe','axe','shovel'):
  texture_item(f'{mat}_{kind}',mat,kind)
 # Tiny inventory emblems complement the detailed 64x32 player armor atlases.
 p=PALETTES[mat]
 for part in ('helmet','chestplate','leggings','boots'):
  im=Image.new('RGBA',(16,16),(0,0,0,0)); d=ImageDraw.Draw(im)
  if part=='helmet':
   d.rectangle((3,3,12,11),fill=rgb(p['edge'])); d.rectangle((4,4,11,9),fill=rgb(p['base'])); d.rectangle((4,10,11,12),fill=rgb(p['dark'])); d.line((5,5,10,5),fill=rgb(p['shine'])); d.point((6,8),fill=rgb(p['accent'])); d.point((9,8),fill=rgb(p['accent']))
  elif part=='chestplate':
   d.polygon([(3,3),(6,2),(8,4),(10,2),(13,3),(14,7),(11,8),(12,14),(4,14),(5,8),(2,7)],fill=rgb(p['edge'])); d.polygon([(4,4),(7,4),(8,6),(10,4),(12,4),(12,7),(10,8),(11,13),(5,13),(6,8),(4,7)],fill=rgb(p['base'])); d.line((7,5,9,7),fill=rgb(p['shine'])); d.point((8,8),fill=rgb(p['accent']))
  elif part=='leggings':
   d.polygon([(3,2),(13,2),(12,8),(10,8),(10,14),(8,14),(8,8),(6,8),(6,14),(4,14),(4,8)],fill=rgb(p['edge'])); d.polygon([(4,3),(12,3),(11,7),(9,7),(9,13),(8,13),(8,7),(6,7),(5,13)],fill=rgb(p['base'])); d.line((5,4,11,4),fill=rgb(p['shine'])); d.point((8,6),fill=rgb(p['accent']))
  else:
   d.rectangle((3,4,7,11),fill=rgb(p['edge'])); d.rectangle((9,4,13,11),fill=rgb(p['edge'])); d.rectangle((2,11,8,14),fill=rgb(p['dark'])); d.rectangle((8,11,14,14),fill=rgb(p['dark'])); d.rectangle((4,5,6,10),fill=rgb(p['base'])); d.rectangle((10,5,12,10),fill=rgb(p['base'])); d.line((3,12,7,12),fill=rgb(p['shine'])); d.point((5,7),fill=rgb(p['accent'])); d.point((11,7),fill=rgb(p['accent']))
  im.save(tex/'item'/f'{mat}_{part}.png')

# Armor overlay UV sheets. Pixel geometry remains Minecraft's standard humanoid armor model;
# shaded panels, bevel pixels and gem/coin piping give both sets distinctive detail.
def armor_texture(mat, layer):
 p=PALETTES[mat]; im=Image.new('RGBA',(64,32),(0,0,0,0)); d=ImageDraw.Draw(im)
 # Canonical UV regions: helmet and torso/arms on layer 1, leggings/boots/arms on layer 2.
 if layer==1:
  regions=[(0,0,16,16),(16,0,32,16),(32,0,48,16),(48,0,64,16),(16,16,32,32),(32,16,48,32)]
 else:
  regions=[(0,0,16,16),(16,0,32,16),(32,0,48,16),(48,0,64,16),(0,16,16,32),(16,16,32,32),(32,16,48,32),(48,16,64,32)]
 for idx,(x0,y0,x1,y1) in enumerate(regions):
  # Broken plates: leave the UV background transparent, draw inset metallic panels.
  pad=1 if idx%3 else 2
  d.rectangle((x0+pad,y0+pad,x1-pad-1,y1-pad-1),fill=rgb(p['dark']))
  d.rectangle((x0+pad+1,y0+pad+1,x1-pad-2,y1-pad-2),fill=rgb(p['base']))
  d.line((x0+pad,y0+pad,x1-pad-1,y0+pad),fill=rgb(p['shine']),width=1)
  d.line((x0+pad,y0+pad,x0+pad,y1-pad-1),fill=rgb(p['light']),width=1)
  d.line((x0+pad,y1-pad-1,x1-pad-1,y1-pad-1),fill=rgb(p['shadow']),width=1)
  # Distinctive inlaid seams and jewel studs.
  if mat=='aurite':
   for xx in range(x0+pad+3,x1-pad-1,5): d.point((xx,y0+pad+3),fill=rgb(p['accent']))
   d.point((x0+(x1-x0)//2,y0+(y1-y0)//2),fill=rgb(p['shine']))
  else:
   for off in range(3,min(x1-x0,y1-y0)-2,5):
    if x0+off<x1-pad and y0+off<y1-pad: d.point((x0+off,y0+off),fill=rgb(p['accent']))
   d.point((x1-pad-2,y0+pad+2),fill=rgb(p['shine']))
 return im
for mat in PALETTES:
 armor_texture(mat,1).save(tex/'models/armor'/f'{mat}_layer_1.png')
 armor_texture(mat,2).save(tex/'models/armor'/f'{mat}_layer_2.png')
print('Wrote pixel-art gear and armor assets')
# 16x16 station textures: coin-stamp bench, teal guard stand and ranger's bow-and-arrow fletching surface.
for name, base, detail in [('aurite_exchange','#513719','#e6ad3d'),('city_guard_post','#263c42','#20c4b5'),('ranger_fletching_table','#60452e','#af7f4e')]:
 random.seed(name); im=Image.new('RGBA',(16,16)); px=im.load()
 palette=[base,'#302a27','#716052','#8c7454']
 for y in range(16):
  for x in range(16): px[x,y]=rgb(random.choice(palette))
 d=ImageDraw.Draw(im)
 if name=='aurite_exchange':
  d.rectangle((3,3,12,12),fill=rgb('#362719')); d.rectangle((4,4,11,11),fill=rgb(detail)); d.ellipse((6,6,9,9),fill=rgb('#ffe078')); d.rectangle((7,4,8,11),fill=rgb('#b87520'))
 elif name=='city_guard_post':
  d.rectangle((2,3,13,12),fill=rgb('#18242a')); d.rectangle((4,4,11,11),fill=rgb(detail)); d.line((4,8,11,8),fill=rgb('#a2fff1'),width=2); d.line((8,4,8,11),fill=rgb('#087f86'),width=2)
 else:
  d.rectangle((2,3,13,12),fill=rgb('#38291e')); d.rectangle((3,4,12,11),fill=rgb('#96724d')); d.line((4,5,11,10),fill=rgb('#f0d7a4'),width=1); d.line((4,10,11,5),fill=rgb('#4b3121'),width=1); d.line((2,3,13,3),fill=rgb('#c0945a'),width=2)
 im.save(tex/'block'/f'{name}.png')
