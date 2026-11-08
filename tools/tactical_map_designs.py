"""50 original tactical battlefields. SPDX-License-Identifier: GPL-3.0-or-later.
Coordinates are native rule coordinates, with y upwards. Geometry is exported
through the same normalized transform for the visible art and collision mask.
"""
import math, random
from fullscreen_map_designs import line, rect, oval, poly, W, H

def convert(op):
    op=dict(op)
    if 'points' in op:op['points']=[[x*W/800,(480-y)*H/480] for x,y in op['points']]
    if 'box' in op:
        x,y,r,b=op['box'];op['box']=[x*W/800,(480-b)*H/480,r*W/800,(480-y)*H/480]
        op['radius']=op.get('radius',0)*H/480
    if 'width' in op:op['width']*=H/480
    return op

FAMILIES = [('边境环线','Frontier Loop'),('双环堡垒','Twin Loops'),('中庭辐射','Courtyard Spokes'),('蛇形远征','Serpentine March'),('三桥运河','Three Bridges'),('分岔街区','Branching Streets'),('内环哨站','Inner Ring'),('双岸要塞','Twin Banks'),('交错回廊','Braided Galleries'),('群岛战线','Island Front')]

def designs():
    out=[]
    for i in range(50):
        family,variant=divmod(i,5);rng=random.Random(59100+i)
        centers=[(x+rng.randint(-18,18),y+rng.randint(-10,10)) for x in [115,390,665] for y in [100,380]]
        ops=[rect(0,0,800,480)]
        width=54+variant*3
        # Different macro graphs: ring, paired loops, hub, snake, bridges,
        # randomized streets, nested ring, river banks, braid, island basins.
        ring=[(0,2),(2,4),(4,5),(5,3),(3,1),(1,0)]
        graphs=[ring,[(0,2),(2,3),(3,1),(1,0),(2,4),(4,5),(5,3)],[],
          [(0,1),(1,3),(3,2),(2,4),(4,5),(0,5)],
          [(0,1),(2,3),(4,5),(0,2),(3,5),(1,4)],
          ring+([(0,3)] if variant%2 else [(1,4)]),ring+[(0,3),(2,5)],
          [(0,1),(0,2),(1,2),(3,4),(4,5),(3,5),(2,3),(1,4)],
          [(0,3),(3,4),(4,1),(1,2),(2,5),(0,5)],ring+[(0,3),(1,4)]]
        routes=[]
        if family==2:
            for a,(x,y) in enumerate(centers):routes.append([(x,y),(x+(35 if a%2 else -35),240),(390,240)])
            routes += [[centers[0],(30,240),centers[1]],[centers[4],(750,240),centers[5]]]
        else:
            permutation=[[0,1,2,3,4,5],[0,2,1,4,3,5],[0,3,4,1,2,5],[2,0,4,1,5,3],[1,4,0,5,2,3]][variant]
            edges=[(permutation[a],permutation[b]) for a,b in graphs[family]]
            if family==5:
                rng.shuffle(edges);edges=edges[:-1]+[(0,1)]
            if variant in [2,4] and family not in [0,3]:edges.append((variant//2,5-variant//2))
            for k,(a,b) in enumerate(edges):
                x,y=centers[a];u,v=centers[b]
                if abs(y-v)<60:
                    detour=(205 if y<240 else 275)+rng.randint(-14,14)
                    route=[(x,y),(x,detour),(u,detour),(u,v)]
                elif abs(x-u)<80:
                    side=1 if x<200 else -1 if x>550 else (-1 if (variant+k)%2 else 1)
                    q=x+side*(105+variant*4)
                    route=[(x,y),(q,y),(q,v),(u,v)]
                elif family in [0,6]:
                    # Broad offset curves instead of orthogonal street joints.
                    route=[(x,y),((x+u)/2-35,210 if y<240 else 270),((x+u)/2+35,270 if y<240 else 210),(u,v)]
                else:
                    mid=(x+u)/2+rng.randint(-28,28)
                    route=[(x,y),(mid,y),(mid,v),(u,v)]
                routes.append(route)
        for route in routes:ops.append(line(route,width,True))
        # Six deployment basins are individually positioned, rather than common
        # partition cells. Rectangular cores support the sparse 4500-dot spiral.
        for k,(x,y) in enumerate(centers):
            ops.append(rect(x-75,y-73,150,146,10,True))
            if family in [0,2,6,9]:ops.append(oval(x,y,95+variant*2,82,True))
            elif family in [1,4,7]:ops.append(rect(x-90,y-80,180,160,25,True))
            elif family in [3,5,8]:ops.append(poly([(x-94,y-35),(x-66,y-84),(x+65,y-84),(x+94,y-35),(x+94,y+35),(x+66,y+84),(x-65,y+84),(x-94,y+35)],True))
        if family in [2,4,6,8,9]:
            ops.append(oval(390,240,76+variant*3,55,True))
            if family in [6,9]:ops.append(oval(390,240,25+variant*2,18))
        if i in [9,16,17,19,23,24,35,36,37,38,39,40,42]:
            # Measured short flanks in these plans need substantial screens,
            # with an explicit U bypass instead of a tiny LOS-only pebble.
            for a,b in [(0,2),(2,4),(1,3),(3,5)]:
                x,y=centers[a];u,v=centers[b];mx=(x+u)/2;cy=(y+v)/2;side=1 if cy<240 else -1
                ops.append(line([(mx,cy-60),(mx,cy+60)],16))
                ops.append(line([(mx-28,cy),(mx-28,cy+side*86),(mx+28,cy+side*86),(mx+28,cy)],50,True))
        # Cover screens break direct basin-to-basin sightlines. Search the
        # geometric midpoint or its near neighbors, always outside spawn cores.
        import build_fullscreen_maps as art
        import numpy as np
        for attempt in range(24):
            wall=art.collision([convert(p) for p in ops]);blocked=False
            for a in range(6):
                for b in range(a+1,6):
                    x,y=centers[a];u,v=centers[b];n=int(math.hypot(u-x,v-y))*2+1
                    xs=np.linspace(x,u,n).astype(int);ys=np.linspace(y,v,n).astype(int)
                    if wall[479-ys,xs].any():continue
                    choices=[((x+(u-x)*f),(y+(v-y)*f)) for f in [.5,.4,.6,.3,.7]]
                    px,py=next(((px,py) for px,py in choices if all(abs(px-cx)>78 or abs(py-cy)>77 for cx,cy in centers)),choices[0])
                    ops.append(oval(px,py,12+variant,12));blocked=True;break
                if blocked:break
            if not blocked:break
        else:raise AssertionError(('sightlines did not converge',i))
        ops += [rect(0,0,3,480),rect(787,0,13,480),rect(0,0,800,3),rect(0,477,800,3)]
        out.append(dict(id=i,name_zh=f'{FAMILIES[family][0]} {variant+1}',name_en=f'{FAMILIES[family][1]} {variant+1}',
          palette=['jade','slate','porcelain','copper','violet'][variant],ops=[convert(p) for p in ops],
          deployment_centers=centers,family=family,route_edges=graphs[family],
          design_note='不同宏观路径骨架与独立部署盆地；错位通道、掩体与多路线争夺。',connectivity_adjustments=[]))
    return out
