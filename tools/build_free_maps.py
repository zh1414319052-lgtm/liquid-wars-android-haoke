"""Independent random cover amount/placement/brush mixtures,1.60. GPL-3.0-or-later.
No room family, common skeleton, spawn table, wall relief or fair-spawn gates.
Uses the existing high-quality art exporter, not a gameplay renderer.
"""
from pathlib import Path
import random,math,json,re,hashlib,zipfile,argparse
import numpy as np
from PIL import Image,ImageDraw,ImageFont
from scipy import ndimage as ndi
import build_fullscreen_maps as art
from fullscreen_map_designs import rect,oval,line,poly
from tactical_map_designs import convert
R=Path(__file__).resolve().parents[1];S=R/'art/source/free-maps-1.60';A=R/'build/free-map-assets/maps';O=R/'docs/game/strategy-1.60'
ZH=['漂流','孤舟','雨痕','落沙','远岸','斜阳','浅滩','砂砾','旧码头','逆风','遗落花园','长影','裂隙','石屿','迷雾','半岛','潮线','碎星','门廊','断桥','静海','夜泊','小径','藏风','雨林','余烬','青石','回声','碎岸','暗流','残垣','无名港','落叶','天井','游沙','礁盘','旷野','云隙','斑驳','长堤','漏光','临渊','荒庭','湾流','飞白','北窗','赤道','浅梦','未央','归潮']
EN=['Drift','Lone Boat','Rain Marks','Falling Sand','Far Shore','Slanting Sun','Shoal','Gravel','Old Wharf','Headwind','Lost Garden','Long Shadows','Fissure','Stone Islet','Haze','Peninsula','Tideline','Broken Stars','Porch','Broken Bridge','Still Sea','Night Mooring','Footpath','Wind Shelter','Rainforest','Embers','Bluestone','Echo','Broken Coast','Undercurrent','Ruins','Nameless Port','Leaves','Lightwell','Moving Sand','Reef','Open Country','Cloud Break','Patina','Long Dyke','Light Leak','Precipice','Wild Court','Gulf Stream','Dry Brush','North Window','Equator','Daydream','Unfinished','Returning Tide']
EXTRA=['organic','meander','coral','starstone','rift','elbow','fork','spiral','crescent','isthmus','pebbles','trapezium','stair','notched','terraces','ruin','blade','fan','capsule','ribbon','crystal','hook','brackets','lozenge','bank','crooked','radialcrack','comma','steps','teardrop']
STYLE_USE={}

def brush(r,cx,cy,spread,kind):
 x=max(20,min(770,r.gauss(cx,spread*800)));y=max(20,min(460,r.gauss(cy,spread*480)));a=r.uniform(0,math.tau)
 def p(u,v):return (x+u*math.cos(a)-v*math.sin(a),y+u*math.sin(a)+v*math.cos(a))
 def bar(points,w):return line([p(u,v) for u,v in points],w)
 def box(u,v,w,h,cut=False):return poly([p(u,v),p(u+w,v),p(u+w,v+h),p(u,v+h)],cut)
 result=[];portals=[]
 if kind in EXTRA:
  if kind in ['organic','starstone','crystal','lozenge','teardrop']:
   n=r.randint(8,18);rx=r.uniform(26,100);ry=r.uniform(18,75);radii=[r.uniform(.65,1.12) for _ in range(n)]
   if kind=='starstone':radii=[r.uniform(.45,.62) if k%2 else r.uniform(.88,1.15) for k in range(n)]
   if kind=='lozenge':n=6;radii=[1,.7,1,.8,1,.9]
   pts=[p(rx*radii[k]*math.cos(k*math.tau/n),ry*radii[k]*math.sin(k*math.tau/n)) for k in range(n)]
   result=[poly(pts)]
  elif kind in ['meander','ribbon','isthmus','rift','crooked','bank']:
   length=r.uniform(95,330);amp=r.uniform(15,60);freq=r.uniform(1.2,3.8);phase=r.random()*6
   pts=[p(float(u),amp*math.sin(u/length*freq+phase)+amp*.3*math.sin(u/length*7)) for u in np.linspace(-length/2,length/2,35)]
   result=[line(pts,r.uniform(10,38) if kind!='rift' else r.uniform(5,12))]
   if kind=='bank':result.append(line([p(u,-40+amp*.5*math.sin(u/length*freq)) for u in np.linspace(-length/3,length/3,20)],r.uniform(9,17)))
  elif kind in ['coral','fork','radialcrack','fan']:
   arms=r.randint(3,7);length=r.uniform(35,100);spreadangle=math.tau if kind in ['coral','radialcrack'] else r.uniform(.8,2.5)
   for k in range(arms):
    ang=k*spreadangle/max(1,arms-1)+r.uniform(-.12,.12);l=length*r.uniform(.55,1.3);end=(l*math.cos(ang),l*math.sin(ang))
    result.append(bar([(0,0),(end[0]*.45+r.uniform(-10,10),end[1]*.45),end],r.uniform(8,23)))
  elif kind in ['spiral','crescent','hook','comma']:
   radius=r.uniform(35,100);sweep=r.uniform(2.8,5.7) if kind!='spiral' else r.uniform(6.3,9.2)
   pts=[]
   for k,t in enumerate(np.linspace(0,sweep,55)):
    rad=radius*(.28+.72*k/54) if kind=='spiral' else radius*(1-.35*k/54)
    pts.append(p(rad*math.cos(t),rad*.72*math.sin(t)))
   result=[line(pts,r.uniform(9,22) if kind!='comma' else r.uniform(24,46))]
  elif kind in ['pebbles','capsule']:
   for k in range(r.randint(2,6)):
    u,v=p(r.uniform(-80,80),r.uniform(-50,50));result.append(oval(u,v,r.uniform(9,27),r.uniform(8,24)))
  elif kind in ['stair','steps','terraces']:
   count=r.randint(3,6);step=r.uniform(19,37);width=r.uniform(28,72)
   if kind=='terraces':
    for k in range(count):result.append(bar([(-width+r.uniform(-15,15),k*step),(0,k*step+12),(width+r.uniform(-15,15),k*step-8)],r.uniform(6,12)))
   else:
    pts=[(-width,0)];u=-width;v=0
    for k in range(count):u+=step;pts.extend([(u,v),(u,v+step)]);v+=step
    result=[bar(pts,r.uniform(12,32))]
  elif kind in ['elbow','brackets']:
   l=r.uniform(55,140);w=r.uniform(14,37);result=[bar([(-l/2,0),(l/2,0),(l/2,l*.65)],w)]
   if kind=='brackets':result.append(bar([(-l/2,-35),(-l/2,-l*.6),(l*.2,-l*.6)],w*.6))
  elif kind in ['notched','ruin']:
   w=r.uniform(70,165);h=r.uniform(45,105);n=r.randint(3,6);pts=[(-w/2,-h/2),(w/2,-h/2),(w/2,h/2)]
   for k in range(n):u=w/2-(k+1)*w/n;pts.extend([(u+5,h/2),(u+5,h/2-r.uniform(15,h*.7)),(u-5,h/2-r.uniform(15,h*.7)),(u-5,h/2)])
   pts.append((-w/2,-h/2));result=[poly([p(u,v) for u,v in pts])]
  else:
   w=r.uniform(65,180);h=r.uniform(25,65);result=[poly([p(-w/2,-h*.35),p(w*.36,-h/2),p(w/2,h*.15),p(w*.1,h/2),p(-w/2,h*.3)])]
 elif kind=='reef':
  for k in range(r.randint(1,5)):
   u,v=p(r.uniform(-28,28),r.uniform(-22,22));result.append(oval(u,v,r.uniform(6,26),r.uniform(5,22)))
 elif kind=='short':result=[bar([(-r.uniform(18,48),0),(r.uniform(18,48),0)],r.uniform(7,19))]
 elif kind=='posts':
  spacing=r.uniform(27,47);radius=r.uniform(5,9)
  for k in range(r.randint(4,10)):
   u,v=p((k-3)*spacing,r.uniform(-5,5));result.append(oval(u,v,radius,radius))
 elif kind=='broken':
  length=r.uniform(240,510);w=r.uniform(9,22);gaps=sorted([r.uniform(-length*.35,length*.35) for _ in range(r.randint(1,3))]);last=-length/2
  for gap in gaps+[length/2]:
   half=r.choice([6,8,12,20]) if gap!=length/2 else 0
   if gap-half>last:result.append(bar([(last,0),(gap-half,0)],w))
   if half:portals.append(dict(center=p(gap,0),normal=p(gap,55),width_requested=half*2,brush=kind))
   last=gap+half
 elif kind=='arc':
  radius=r.uniform(30,110);sweep=r.uniform(1.0,4.5);pts=[p(radius*math.cos(t),radius*math.sin(t)) for t in np.linspace(0,sweep,24)]
  result=[line(pts,r.uniform(8,19))]
 elif kind=='solid':
  w,h=r.uniform(50,210),r.uniform(35,145);result=[box(-w/2,-h/2,w,h)]
 elif kind=='comb':
  length=r.uniform(100,240);step=r.uniform(28,50);result=[bar([(-length/2,0),(length/2,0)],r.uniform(7,14))]
  for k in np.arange(-length/2+15,length/2,step):result.append(bar([(k,0),(k,r.uniform(24,75))],r.uniform(6,12)))
 elif kind=='rooms':
  for k in range(r.randint(1,3)):
   u=k*r.uniform(65,100);w,h=r.uniform(48,95),r.uniform(48,90);gap=r.choice([12,16,24,40])
   result += [bar([(u-w/2,-h/2),(u-w/2,h/2),(u+w/2,h/2),(u+w/2,-h/2)],9),bar([(u-w/2,-h/2),(u-gap/2,-h/2)],9),bar([(u+gap/2,-h/2),(u+w/2,-h/2)],9)]
   portals.append(dict(center=p(u,-h/2),normal=p(u,-h/2-55),width_requested=gap,brush=kind))
 elif kind=='funnel':
  gap=r.choice([12,16,28]);length=r.uniform(65,130);mouth=r.uniform(60,110)
  result=[bar([(-length,-mouth),(0,-gap/2)],9),bar([(-length,mouth),(0,gap/2)],9)]
  portals.append(dict(center=p(0,0),normal=p(60,0),width_requested=gap,brush=kind))
 else:
  result=[poly([p(-70,-40),p(80,-20),p(-20,70)])]
 return result,portals

def frame():return [rect(0,0,3,480),rect(787,0,13,480),rect(0,0,800,3),rect(0,477,800,3)]
INTERIOR=np.zeros((480,800),bool);INTERIOR[3:477,3:787]=True
def fast(ops):return np.asarray(art.raster([convert(p) for p in ops],(800,480)))>127
def generate(i,tier,target,kinds):
 frame_mask=fast(frame())
 for revision in range(8):
  r=random.Random(1607000+i*101+revision*7919);cx=r.uniform(80,720);cy=r.uniform(50,430);spread=r.uniform(.12,.38)
  ops=[];portals=[];rejected=0
  current=np.zeros((480,800),bool);buffer=current.copy()
  for attempt in range(1100):
   if attempt>120:spread=min(.65,spread+.002)
   fraction=float(current[INTERIOR].mean())
   if fraction>=max(0,target-.004):break
   selected=r.choice(kinds);new,doors=brush(r,cx,cy,spread,selected)
   if tier==3:
    # Heavy cover needs broad authored masses, not hundreds of overlapping
    # tiny glyphs. Scale each independent stroke about its own center.
    points=[]
    for op in new:
     if 'points' in op:points+=op['points']
     else:
      x0,y0,x1,y1=op['box'];points += [(x0,y0),(x1,y1)]
    bx=sum(q[0] for q in points)/len(points);by=sum(q[1] for q in points)/len(points);scale=r.uniform(1.4,2.2)
    for op in new:
     if 'points' in op:op['points']=[(bx+(u-bx)*scale,by+(v-by)*scale) for u,v in op['points']]
     else:
      u,v,w,h=op['box'];op['box']=[bx+(u-bx)*scale,by+(v-by)*scale,bx+(w-bx)*scale,by+(h-by)*scale]
     if 'width' in op:op['width']*=scale*1.5
    for door in doors:
     door['center']=[bx+(door['center'][0]-bx)*scale,by+(door['center'][1]-by)*scale];door['normal']=[bx+(door['normal'][0]-bx)*scale,by+(door['normal'][1]-by)*scale]
   new_mask=fast(new);trial=current|new_mask;value=float(trial[INTERIOR].mean())
   if value>target+.006 or value<=fraction+.00005:rejected+=1;continue
   # Avoid the rejected "pile of repeated glyphs". Each authored stroke has
   # breathing room; connections intrinsic to a brush remain intentional.
   if (new_mask&buffer).sum()>max(2,new_mask.sum()*.05):rejected+=1;continue
   # Reject disconnected floor rather than opening a generic repair corridor.
   if ndi.label(~(trial|frame_mask))[1]!=1:rejected+=1;continue
   ops+=new;portals+=doors;current=trial;buffer=ndi.binary_dilation(current,iterations=5)
  if abs(float(current[INTERIOR].mean())-target)>.012:
   print('RETRY cover',i,revision,kinds,float(current[INTERIOR].mean()),target,flush=True);continue
  canonical=[convert(p) for p in ops+frame()];wall=art.collision(canonical)
  labels,count=ndi.label(~wall);seals=[]
  if count>1:
   sizes=np.bincount(labels.ravel());sizes[0]=0;main=int(sizes.argmax());other=[j for j in range(1,count+1) if j!=main]
   # Rasterized stroke joints can enclose a1-4pixel antialias pinhole. Seal
   # those exact cells in editable geometry; never join/reshape real rooms.
   if all(sizes[j]<=4 for j in other):
    for j in other:
     for y,x in zip(*np.where(labels==j)):canonical.append(convert(rect(float(x),float(479-y),1,1)));seals.append([int(x),int(479-y)])
    wall=art.collision(canonical)
  if ndi.label(~wall)[1]!=1:
   print('RETRY coverage-connectivity',i,revision,kinds,flush=True);continue
  if (~wall).sum()<60000:continue
  return dict(id=i,name_zh=ZH[i],name_en=EN[i],palette=r.choice(['jade','slate','porcelain','copper','violet']),
   cover_band=tier,target_wall_fraction=target,wall_fraction=float(wall[INTERIOR].mean()),position_draw=[cx/800,cy/480],spread_draw=spread,
   brushes=kinds,revision=revision,rejected_strokes=rejected,subpixel_seals=seals,ops=canonical,portals=portals),wall
 raise ValueError(('Could not generate valid independent map',i,tier,target))

def overview(plans):
 assert art.FONT.exists();font=ImageFont.truetype(str(art.FONT),20);order=list(plans);random.Random(160600).shuffle(order)
 page=Image.new('RGB',(2200,2505),(25,32,36));d=ImageDraw.Draw(page);d.text((14,10),'50 张独立地图 · 打乱顺序 · 素材总览，非游戏截图',font=font,fill=(205,219,219))
 for j,p in enumerate(order):
  im=Image.open(A/f'{p["id"]}-image.png');im.thumbnail((430,195));x=j%5*440+5;y=j//5*245+50;page.paste(im,(x,y));d.text((x+4,y+198),f'{p["id"]+1:02d} {p["name_zh"]}  {p["wall_fraction"]:.0%}',font=font,fill=(195,211,212))
 page.save(O/'free-all-50-shuffled.png');page.thumbnail((1536,1800));page.save(O/'free-all-50-preview.jpg',quality=85)
 old=Image.open(R/'docs/game/tactical-1.59/all-50-artwork.png').convert('RGB');new=Image.open(O/'free-all-50-shuffled.png');old.thumbnail((1500,1800));new.thumbnail((1500,1800));side=Image.new('RGB',(3000,1855),(25,32,36));d=ImageDraw.Draw(side);d.text((12,10),'1.59 固定盆地（旧）',font=font,fill=(205,219,219));d.text((1512,10),'1.60 独立抽取（新） · 两者均为素材预览',font=font,fill=(205,219,219));side.paste(old,(0,50));side.paste(new,(1500,50));side.save(O/'maps-159-vs-160.png')

def main():
 ap=argparse.ArgumentParser();ap.add_argument('--resume',action='store_true');args=ap.parse_args()
 cache={}
 if args.resume and (S/'progress.json').exists():cache={p['id']:p for p in json.loads((S/'progress.json').read_text('utf8'))['maps']}
 for path in [S/'svg',S/'geometry',A,O]:path.mkdir(parents=True,exist_ok=True)
 art.SOURCE=S;art.load_palette(R/'art/source/comfort-1.48/palette.json');art.PALETTES={k:(f,tuple(round(c*.8) for c in b),rim) for k,(f,b,rim) in art.PALETTES.items()}
 ranges=[(0,.028),(.035,.095),(.11,.245),(.26,.44)];r=random.Random(16061106);plans=[]
 # Stratify COVER AMOUNT only, then shuffle. Geometry/placement/style are all
 # independent draws; these are not level families or repeated structures.
 tiers=[0]*5+[1]*15+[2]*20+[3]*10;r.shuffle(tiers)
 for i,tier in enumerate(tiers):
  target=r.uniform(*ranges[tier]);available=EXTRA+['reef','short','posts','broken','arc','solid','comb','rooms','funnel']
  # Balance brush reuse, not map topology. A map receives its own combination;
  # dominant shapes are no longer the same triangles/comb/frame everywhere.
  kinds=[]
  for k in range(r.choice([2,2,3])):
   choices=[b for b in available if b not in kinds];weights=[1/(1+STYLE_USE.get(b,0))**3 for b in choices];b=r.choices(choices,weights)[0];kinds.append(b);STYLE_USE[b]=STYLE_USE.get(b,0)+1
  if tier==3 and not any(b in kinds for b in ['organic','starstone','crystal','lozenge','teardrop','notched','ruin','solid','trapezium','blade']):
   choices=['organic','starstone','crystal','notched','ruin','solid','trapezium'];b=min(choices,key=lambda b:STYLE_USE.get(b,0)+r.random());kinds[0]=b;STYLE_USE[b]=STYLE_USE.get(b,0)+1
  cached=cache.get(i)
  if cached and cached['brushes']==kinds and abs(cached['target_wall_fraction']-target)<1e-10:
   p=cached;print('RESUME',i,flush=True)
  else:
   p,wall=generate(i,tier,target,kinds);art.save_svg(p);image,geo=art.artwork(dict(p,id=-100));image.save(A/f'{i}-image.png',optimize=True);geo.save(S/'geometry'/f'{i:02d}.png',optimize=True)
   mask=np.zeros((480,800,4),np.uint8);mask[wall]=255;Image.fromarray(mask).save(A/f'{i}-map.png',optimize=True)
  plans.append(p)
  (S/'progress.json').write_text(json.dumps(dict(done=len(plans),maps=plans),ensure_ascii=False),encoding='utf8');print(i,ZH[i],tier,f'{p["wall_fraction"]:.3f}',p['revision'],flush=True)
 (S/'maps.json').write_text(json.dumps(dict(license='GPL-3.0-or-later',canonical_size=[2200,1000],runtime_image_size=list(art.SIZE),rule_mask_size=[800,480],cover_definition='Interior excludes unchanged HUD/frame strips; cover bands describe quantity only, never structural families',maps=plans),ensure_ascii=False,indent=2),encoding='utf8')
 (S/'README.md').write_text('# 自由出生地图原件\n\n50张独立抽取遮挡位置/笔刷混合及密度。maps.json与SVG可编辑，geometry为实际美术覆盖；全部GPL-3.0-or-later原创。保留既有柔和色板，导出器tools/build_free_maps.py复用原美术管线，不是游戏渲染器。5/15/20/10仅分层抽取遮挡量，形状无家族或共用房间。Noto Sans SC总览字体，SIL OFL1.1，可商用；许可见art/source/ui-1.52/NotoSansSC-OFL.txt。旧不合格samples-v1/v2另存档，不参与此版。\n',encoding='utf8')
 overview(plans)
if __name__=='__main__':main()
