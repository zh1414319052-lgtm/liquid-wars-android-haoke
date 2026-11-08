"""Original 1.60 macro layouts. Rule coordinates, y upwards. GPL-3.0-or-later.
Each family constructs its own topology; there is no shared six-room template.
"""
import math, random
from fullscreen_map_designs import rect, oval, line, poly
from tactical_map_designs import convert

FAMILIES=[('中庭外环','Atrium and Rim'),('流沙河道','Braided Channels'),
 ('断层桥群','Fault Bridges'),('百叶小室','Shutter Chambers'),
 ('礁石海峡','Reef Straits'),('旋臂风车','Windmill Arms'),
 ('错位环城','Offset Rings'),('曲廊捷径','Switchback Cuts'),
 ('非对称要塞','Unequal Citadels'),('斜街棋城','Oblique City')]

def design(i):
 f,v=divmod(i,5);r=random.Random(160000+i);ops=[]
 def road(points,w=65):ops.append(line(points,w,True))
 def room(x,y,w,h):ops.append(rect(x-w/2,y-h/2,w,h,8,True))
 def ellipse(x,y,rx,ry):ops.append(oval(x,y,rx,ry,True))
 def wall(points,w=26):ops.append(line(points,w))
 def circle(cx,cy,rx,ry,n=36):return [(cx+rx*math.cos(k*2*math.pi/n),cy+ry*math.sin(k*2*math.pi/n)) for k in range(n+1)]
 # Start solid, then explicitly excavate each different circulation system.
 ops.append(rect(0,0,800,480))
 if f==0:
  cx,cy=375+r.randint(-35,35),240+r.randint(-20,20)
  n=7+v%3;ring=circle(cx,cy,310-v*9,170+v*2);road(ring,55+v*3)
  room(cx,cy,190+v*14,145-v*5)
  for k in range(n):
   a=(k+.18*v)*2*math.pi/n;x=cx+(285-v*8)*math.cos(a);y=cy+155*math.sin(a)
   room(x,y,100+r.randint(0,35),75+r.randint(0,25))
   if k%2==v%2:road([(x,y),(cx+85*math.cos(a),cy+65*math.sin(a))],28 if k%3==0 else 52)
 elif f==1:
  lanes=3+(v==4);ys=[65+k*(350/(lanes-1)) for k in range(lanes)]
  for k,y in enumerate(ys):
   pts=[(40,y),(210,y+(-1)**k*(20+v*3)),(410,y-(-1)**k*18),(600,y+(-1)**k*15),(750,y)]
   road(pts,68+v*3)
   for j in range(4):ellipse(85+j*210+r.randint(-20,20),y,66,48)
  for k in range(lanes-1):
   for j,x in enumerate([70+v*9,370-v*15,690-v*6]):road([(x,ys[k]),(x+(-1)**j*40,ys[k+1])],[26,50,94][(j+k+v)%3])
 elif f==2:
  # Alternating banks of a diagonal rift, connected by staggered bridges.
  road([(45,100),(190,90),(370,170),(550,230),(745,290)],95)
  road([(55,260),(230,290),(420,335),(580,390),(745,400)],95)
  for k,x in enumerate([90,275,460,675]):
   top=115+x*.25;bot=top+140
   road([(x,top),(x+35+v*3,bot)],[28,52,96,48][(k+v)%4])
  road([(40,100),(40,390),(230,390)],60)
  road([(600,65),(750,65),(750,400)],62)
  road([(200,70),(450,65),(600,65)],55)
  for k in range(9):
   x=55+(k%5)*165;y=95+(k//5)*270+(k%2)*22
   room(x,y,100+v*4,82)
 elif f==3:
  nx,ny=[(4,3),(5,3),(4,4),(3,3),(5,2)][v]
  xs=[65+k*660/(nx-1) for k in range(nx)];ys=[55+k*365/(ny-1) for k in range(ny)]
  for y in ys:
   for x in xs:room(x,y,115 if nx<5 else 96,95 if ny<4 else 76)
  for k,y in enumerate(ys):
   for j in range(nx-1):road([(xs[j],y+(-1)**j*20),(xs[j+1],y+(-1)**j*20)],[26,50,92][(j+k+v)%3])
  for j,x in enumerate(xs):
   for k in range(ny-1):road([(x+(-1)**k*23,ys[k]),(x+(-1)**k*23,ys[k+1])],[28,48,94][(j+2*k+v)%3])
 elif f==4:
  # Broad connected sea with overlapping elongated reefs and offset passes.
  room(393,240,740,420)
  for k in range(3):
   x=190+k*205+r.randint(-20,20);gap=105+((k+v)%3)*125
   wall([(x,35),(x+20, max(35,gap-48))],64+v*3)
   wall([(x+20,gap+48),(x-10,445)],64+v*3)
  for k in range(4):
   x=85+k*195;y=210+(-1)**k*85
   ops.append(oval(x,y,43+v*2,30))
 elif f==5:
  cx=370+v*13;cy=225-v*4;ellipse(cx,cy,76,65)
  for k in range(4):
   a=k*math.pi/2+v*.11
   pts=[]
   for t in [0,.35,.7,1]:
    b=a+t*.65;pts.append((cx+(55+260*t)*math.cos(b),cy+(40+155*t)*math.sin(b)))
   road(pts,66+v*2);x,y=pts[-1];room(x,y,105,86)
   b=a+.65;road([(x,y),(cx+320*math.cos(b+.65),cy+175*math.sin(b+.65))],48)
  road(circle(cx,cy,315,173),48+v*3)
 elif f==6:
  cx=380+r.randint(-28,28);cy=240+r.randint(-12,12)
  for j,(rx,ry) in enumerate([(320,175),(205,102),(88,38)]):
   road(circle(cx,cy,rx,ry),42+v*3)
   for k in range(3+(v%2)):
    a=(k*2*math.pi/(3+v%2))+(j*.72)+(v*.21)
    if j<2:
     nrx,nry=[(205,102),(88,38)][j]
     road([(cx+rx*math.cos(a),cy+ry*math.sin(a)),(cx+nrx*math.cos(a),cy+nry*math.sin(a))],[26,50,94][(k+j+v)%3])
  ellipse(cx,cy,60,34)
 elif f==7:
  n=3+(v%3);ys=[55+k*360/(n-1) for k in range(n)]
  pts=[]
  for k,y in enumerate(ys):pts.extend([(45,y),(740,y)] if k%2==0 else [(740,y),(45,y)])
  road(pts,60 if n<5 else 50)
  for k in range(n-1):
   x=210+((k+v)%3)*170;road([(x,ys[k]),(x+45*(-1)**k,ys[k+1])],28 if k%2 else 52)
  for k in range(n):ellipse(160+(k*223+v*31)%480,ys[k],72,48)
 elif f==8:
  room(220,240,355,330);room(595,125,270,140);room(610,370,250,140)
  wall([(220,95),(220,200)],48);wall([(220,280),(220,380)],48)
  wall([(80,240),(170,240)],40);wall([(270,240),(380,240)],40)
  road([(350,140),(455,75),(620,80)],54+v*3)
  road([(350,340),(460,420),(620,420)],58)
  road([(610,125),(745,240),(610,370)],50)
  road([(370,240),(500,240),(600,240),(610,370)],26+v*4)
  road([(500,240),(595,125)],44)
  ellipse(500,240,65,50)
 elif f==9:
  # Street grid is skewed and has a different block count in every variant.
  nx=4+(v%2);ny=3+(v==3)
  xs=[50+k*690/(nx-1) for k in range(nx)];ys=[50+k*365/(ny-1) for k in range(ny)]
  for k,y in enumerate(ys):road([(x,y+(x-400)*(.08+.025*v)) for x in [30,760]],54+(k%2)*26)
  for j,x in enumerate(xs):road([(x+(y-240)*(.15+.03*v),y) for y in [25,455]],38+(j%2)*30)
  road([(60,60),(730,410)],26+v*4)
  for k in range(3):room(160+k*220,100+((k+v)%3)*125,110,88)
 # Variant transforms alter the actual circulation positions, not only color.
 angle=r.uniform(-.075,.075);cx,cy=395,240;dx=r.uniform(-12,12);dy=r.uniform(-9,9)
 def point(x,y):
  return [cx+(x-cx)*math.cos(angle)-(y-cy)*math.sin(angle)+dx,cy+(x-cx)*math.sin(angle)+(y-cy)*math.cos(angle)+dy]
 resolved=[]
 for op in ops:
  op=dict(op)
  if 'points' in op:op['points']=[point(x,y) for x,y in op['points']]
  elif 'box' in op:
   # Ellipses/rounded rooms translate; global stroke rotation supplies variation.
   a,b,c,d=op['box'];x,y=point((a+c)/2,(b+d)/2);op['box']=[x-(c-a)/2,y-(d-b)/2,x+(c-a)/2,y+(d-b)/2]
  resolved.append(op)
 # Explicit playable bounds, including the unchanged narrow score sidebar.
 resolved += [rect(0,0,5,480),rect(787,0,13,480),rect(0,0,800,5),rect(0,475,800,5)]
 return dict(id=i,family=f,variant=v,name_zh=f'{FAMILIES[f][0]} {v+1}',name_en=f'{FAMILIES[f][1]} {v+1}',
  palette=['jade','slate','porcelain','copper','violet'][(v+f)%5],ops=[convert(op) for op in resolved],
  design_note='独立宏观结构；出口、宽窄与环线来自实际几何。未经过门和对战实测不得视为验收。',connectivity_adjustments=[])

def designs():return [design(i) for i in range(50)]
