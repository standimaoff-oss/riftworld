#!/usr/bin/env python3
"""Second-pass articulated fauna, authored afresh from the supplied images.

Y-up, forward -Z; Model exports matching runtime and editable Blockbench rigs.
"""
from build_reference_fauna import Model


def liopleurodon():
    m = Model('liopleurodon', .80)
    m.bone('body', [0,0,0])
    # Long, low barrel, not a stack of square armour plates. Overlapping central
    # and flank volumes approximate the round silhouette in the side reference.
    for z,d,w,h in [(-54,18,29,18),(-38,20,34,21),(-20,20,37,22),(-2,20,38,23),(16,20,35,22),(34,19,30,20),(51,17,25,18),(66,13,20,15)]:
        m.box('body',[-w//2,-h//2,z],[w,h,d],'lio_side',z%2)
        m.box('body',[-w//2+3,-h//2-2,z],[w-6,5,d],'lio_belly')
        m.box('body',[-w//2+4,h//2-1,z],[w-8,2,d],'lio_back')
        for side in (-1,1):
            x=(w//2-2) if side>0 else -w//2
            m.box('body',[x,-h//2+1,z],[2,4,d],'lio_belly')
    # Pale irregular scars curve from back down the flank.
    for z,side in [(-28,1),(-7,-1),(20,1),(43,-1)]:
        for i in range(8):
            x=side*(3+i*2)
            m.box('body',[x-1,10-i//3,z+i],[2,1,3],'scar')
        for i in range(5):
            x=17 if side>0 else -18
            m.box('body',[x,7-i*2,z+7+i],[1,2,2],'scar')
    m.bone('head',[0,0,-52],'body',{'axis':'y','amp':3,'speed':.7,'phase':.2})
    # Tapered, flattened crocodile-like skull, with the eye behind the snout.
    for z,d,w,h in [(-71,20,27,16),(-89,20,23,12),(-106,19,19,10),(-121,17,16,8)]:
        m.box('head',[-w//2,-4,z],[w,h,d],'lio_side')
        m.box('head',[-w//2+2,h-5,z],[w-4,2,d],'lio_back')
    m.pair('head',13,4,-68,1,3,4,'lio_eye')
    m.pair('head',14,5,-67,1,1,1,'glint')
    m.pair('head',6,2,-119,2,2,2,'lio_eye')
    m.box('head',[-8,-5,-120],[16,2,49],'gum')
    for side in (-1,1):
        for i in range(15):
            z=-118+i*3
            edge=7 if z < -104 else (9 if z < -87 else 11)
            x=edge-1 if side>0 else -edge-1
            h=3+(i%3==0)
            m.box('head',[x,-5-h,z],[2,h,2],'tooth',i%2)
            m.box('head',[x,-6-h,z],[1,1,1],'tooth')
    m.bone('jaw',[0,-4,-58],'head',{'axis':'x','amp':3,'speed':.55,'phase':1})
    for z,d,w in [(-120,17,16),(-105,18,19),(-89,18,22),(-73,17,25)]:
        m.box('jaw',[-w//2,-11,z],[w,5,d],'lio_belly')
        m.box('jaw',[-w//2+1,-6,z],[w-2,1,d],'gum')
    for side in (-1,1):
        for i in range(12):
            z=-117+i*4
            edge=7 if z < -104 else (8 if z < -88 else 10)
            x=edge-1 if side>0 else -edge-1
            m.box('jaw',[x,-5,z],[2,3+(i%3==0),2],'tooth',i%2)
    # Wide swept tapered paddles, not skinny rectangular feet.
    for front,z in ((True,-34),(False,35)):
        for side,label in ((1,'l'),(-1,'r')):
            name=('fin_front_' if front else 'fin_rear_')+label
            m.bone(name,[side*14,-5,z+5],'body',{'axis':'z','amp':side*(16 if front else 12),'speed':1.0,'phase':0 if front else .8})
            for i in range(9):
                x=14+i*4; w=5; depth=max(4,22-i*2); zz=z+i*2
                if not front: x=14+i*3; w=4; depth=max(4,18-i*2)
                xx=x if side>0 else -x-w
                m.box(name,[xx,-8-i//2,zz],[w,5 if i<4 else 3,depth],'lio_fin' if i<6 else 'lio_fin_edge',i%2)
                if i<6: m.box(name,[xx,-3-i//2,zz+2],[w,1,max(1,depth-4)],'lio_fin_edge',i%2)
    m.bone('tail_1',[0,0,72],'body',{'axis':'y','amp':13,'speed':1.1,'phase':.5})
    for z,d,w,h in [(77,12,17,13),(88,11,13,11),(98,10,9,9)]:
        m.box('tail_1',[-w//2,-h//2,z],[w,h,d],'lio_side')
        m.box('tail_1',[-w//2+2,-h//2-2,z],[w-4,3,d],'lio_belly')
    m.bone('tail_tip',[0,0,106],'tail_1',{'axis':'y','amp':20,'speed':1.1,'phase':1.2})
    m.box('tail_tip',[-4,-4,106],[8,8,13],'lio_side')
    for i in range(5): m.box('tail_tip',[-2,3+i*2,111+i],[4,3,7-i],'lio_fin_edge')
    m.export()


def pegasus():
    m=Model('pegasus',.90)
    m.bone('body',[0,26,0])
    # Horse proportions: long barrel, distinct shoulders, rounded rump.
    for origin,size,mat in [((-7,19,-14),(14,14,29),'coat'),((-6,31,-13),(12,4,27),'coat_light'),((-6,16,-10),(12,4,22),'coat_shade'),((-8,20,-14),(16,12,9),'coat'),((-8,20,7),(16,12,8),'coat')]: m.box('body',origin,size,mat)
    m.bone('neck',[0,30,-12],'body',{'axis':'x','amp':2,'speed':.45,'phase':0})
    for origin,size,mat in [((-5,28,-21),(10,14,11),'coat'),((-4,34,-25),(8,13,10),'coat_light'),((-4,40,-28),(8,7,9),'coat')]: m.box('neck',origin,size,mat)
    # Long, individual locks follow the sloping neck.
    for i in range(11):
        z=-26+i*2; y=42-i
        m.box('neck',[-2,y,z],[4,4,3],'mane',i%2)
        for side in (-1,1):
            x=4 if side>0 else -6
            m.box('neck',[x,y-7-i//3,z],[2,8+i//3,3],'mane' if i%3 else 'mane_dark',i%2)
    m.bone('head',[0,42,-26],'neck',{'axis':'x','amp':2,'speed':.7,'phase':.7})
    for origin,size,mat in [((-4,38,-31),(8,9,8),'coat'),((-3,35,-37),(6,7,8),'coat_light'),((-3,34,-39),(6,4,3),'muzzle')]: m.box('head',origin,size,mat)
    m.pair('head',4,42,-30,1,3,3,'coat_shade'); m.pair('head',5,43,-30,1,2,2,'eye')
    m.pair('head',2,35,-39,1,1,2,'eye'); m.pair('head',2,47,-28,2,6,3,'coat'); m.pair('head',2,49,-29,1,3,1,'muzzle')
    for i in range(6): m.box('head',[-3+i,47-i//2,-32+i],[2,3,4],'mane')
    for front,z in ((True,-10),(False,10)):
        for side,label in ((1,'l'),(-1,'r')):
            name=('leg_front_' if front else 'leg_rear_')+label; x=side*5
            m.bone(name,[x,21,z],'body')
            for origin,size,mat in [((x-2,11,z-2),(4,11,5),'coat'),((x-2,7,z-2),(4,5,4),'coat_shade'),((x-1,2,z-1),(3,7,3),'coat_light'),((x-2,0,z-2),(5,3,5),'hoof')]: m.box(name,origin,size,mat)
    m.bone('tail',[0,29,13],'body',{'axis':'y','amp':6,'speed':.65,'phase':.4})
    m.box('tail',[-3,23,13],[6,7,6],'coat_shade')
    for i in range(9):
        x=-4+i
        for y,z,h in [(18,17,8),(12,20,8),(8,23,6)]: m.box('tail',[x,y+i%2,z],[2,h,4],'mane' if i%3 else 'mane_dark',i%2)
    for side,label in ((1,'l'),(-1,'r')):
        name='wing_'+label; m.bone(name,[side*7,32,-5],'body')
        def box(x,y,z,w,h,d,mat,variant=0): m.box(name,[x if side>0 else -x-w,y,z],[w,h,d],mat,variant)
        # Broad overlapping feather fan: primaries sweep back and have staggered tips.
        box(7,30,-10,13,6,20,'wing_shadow'); box(8,35,-10,18,3,22,'wing')
        for i in range(12):
            z=-10+i*3; length=26+min(i,10-i)*2 if i<11 else 22
            box(18,35,z,length,3,4,'wing',i%2)
            box(18+length-9,37,z+1,9,2,3,'wing_tip',i%2)
            box(21,38,z+1,length-5,1,1,'wing_shadow',i%2)
            box(11+i//2,38,z,13,2,3,'wing',i%2)
            box(14+i//2,32,z+1,12,2,3,'wing_shadow',i%2)
    m.bone('saddle',[0,32,-3],'body')
    m.box('saddle',[-6,33,-10],[12,2,15],'saddle'); m.box('saddle',[-5,35,-9],[10,1,12],'leather')
    m.box('saddle',[-5,34,3],[10,3,2],'saddle'); m.pair('saddle',6,26,-8,1,9,2,'leather'); m.pair('saddle',7,25,-8,1,2,2,'gold')
    m.export()

if __name__ == '__main__':
    liopleurodon()
    pegasus()
