"""Stage original 1.60 map assets without replacing running baseline maps.
GPL-3.0-or-later. Uses the existing exact coverage/art export pipeline.
"""
from pathlib import Path
import argparse,json,math,hashlib
import numpy as np
from scipy import ndimage as ndi
from scipy.sparse import coo_matrix
from scipy.sparse.csgraph import dijkstra
from PIL import Image,ImageDraw,ImageFont
import build_fullscreen_maps as art
from strategy_map_designs import design
from tactical_map_designs import convert
from fullscreen_map_designs import rect,line
R=Path(__file__).resolve().parents[1];S=R/'art/source/strategy-1.60';O=R/'docs/game/strategy-1.60';A=R/'build/strategy-new-assets/maps'

def graph(floor):
 walk=floor.reshape(120,4,200,4).all(axis=(1,3));ids=np.arange(24000).reshape(120,200);us=[];vs=[];ws=[]
 for dy,dx,c in [(0,1,4),(1,0,4),(1,1,math.sqrt(32)),(1,-1,math.sqrt(32))]:
  y0,y1=max(0,-dy),min(120,120-dy);x0,x1=max(0,-dx),min(200,200-dx)
  good=walk[y0:y1,x0:x1]&walk[y0+dy:y1+dy,x0+dx:x1+dx]
  if dy and dx:good &= walk[y0+dy:y1+dy,x0:x1]&walk[y0:y1,x0+dx:x1+dx]
  u=ids[y0:y1,x0:x1][good];v=ids[y0+dy:y1+dy,x0+dx:x1+dx][good]
  us.extend([u,v]);vs.extend([v,u]);ws.extend([np.full(len(u),c)]*2)
 return coo_matrix((np.concatenate(ws),(np.concatenate(us),np.concatenate(vs))),shape=(24000,24000)).tocsr(),walk

def candidates(floor,count):
 # Farthest sampling in real walking distance. Candidate cores need not be the
 # old 99x99 rectangles; actual spiral placement and passage are separate gates.
 clearance=ndi.distance_transform_edt(floor);g,walk=graph(floor)
 safe=walk&(clearance[2::4,2::4]>=24);nodes=np.flatnonzero(safe);assert len(nodes)>count
 rng=np.random.default_rng(160);first=int(nodes[rng.integers(len(nodes))]);chosen=[];rows=[];nearest=np.full(24000,np.inf)
 for k in range(count):
  node=first if not chosen else int(nodes[np.argmax(nearest[nodes])]);chosen.append(node)
  d=dijkstra(g,indices=node);assert np.isfinite(d[nodes]).all(),'Disconnected conservative grid'
  rows.append(d);nearest=np.minimum(nearest,d)
 # Offline authoring search: distribute candidate zones and avoid straight
 # sightline starts. This never runs in simulation or chooses game actions.
 pool=np.unique(np.concatenate([np.asarray(chosen),rng.choice(nodes,min(180,len(nodes)),replace=False)]))
 distances_all=dijkstra(g,indices=pool)[:,pool];xy=np.column_stack((pool%200*4+2,pool//200*4+2));euclidean=np.linalg.norm(xy[:,None]-xy[None,:],axis=2)
 ratios=np.divide(distances_all,euclidean,out=np.ones_like(distances_all),where=euclidean>0)
 best=np.array([int(np.flatnonzero(pool==n)[0]) for n in chosen]);upper=np.triu_indices(count,1)
 def cost(ids):
  matrix=distances_all[np.ix_(ids,ids)];rs=ratios[np.ix_(ids,ids)][upper];q=np.percentile(rs,[10,50,90]);near=np.where(np.eye(count,dtype=bool),np.inf,matrix).min(axis=1)
  return max(0,1.15-q[0])*25+max(0,1.35-q[1])*20+max(0,q[2]-3.5)*3+max(0,100-float(near.min()))*.04+max(0,float(near.max()/near.min())-2)
 score=cost(best)
 for attempt in range(1800):
  test=best.copy();test[rng.integers(count)]=rng.integers(len(pool))
  if len(set(test))!=count:continue
  value=cost(test)
  if value<score:best,score=test,value
 chosen=pool[best];pts=[[int(n%200*4+2),int(479-(n//200*4+2))] for n in chosen]
 distances=np.rint(distances_all[np.ix_(best,best)]).astype(int).tolist()
 return pts,distances

def repair(p):
 for iteration in range(24):
  wall=art.collision(p['ops']);labels,n=ndi.label(~wall)
  if n==1:return wall
  sizes=np.bincount(labels.ravel());sizes[0]=0;main=int(sizes.argmax())
  other=min((k for k in range(1,n+1) if k!=main),key=lambda k:sizes[k]);yy,xx=np.where(labels==other)
  if sizes[other]<20:
   for x,y in zip(xx,yy):p['ops'].append(convert(rect(float(x),float(479-y),1,1)))
   p['connectivity_adjustments'].append(dict(kind='seal_subpixel_floor',area=int(sizes[other])))
  else:
   d,nearest=ndi.distance_transform_edt(labels!=main,return_indices=True);j=int(np.argmin(d[yy,xx]));y,x=int(yy[j]),int(xx[j]);u,v=int(nearest[1,y,x]),int(nearest[0,y,x])
   p['ops'].append(convert(line([(x,479-y),(u,479-v)],48,True)))
   p['connectivity_adjustments'].append(dict(kind='explicit_48px_join',a=[x,479-y],b=[u,479-v]))
 raise ValueError(('Floor repairs did not converge',p['id']))

def relieve_walls(p):
 # Large solid dead zones become explicit connected alcoves. Keep the macro
 # routes and preserve every adjustment in editable JSON (no bitmap patch).
 for k in range(40):
  wall=repair(p);distance=ndi.distance_transform_edt(np.pad(wall,1,constant_values=False))[1:-1,1:-1]
  y,x=np.unravel_index(distance.argmax(),distance.shape)
  if distance[y,x]<=44:
   walk=(~wall).reshape(120,4,200,4).all(axis=(1,3));labels,n=ndi.label(walk)
   sizes=np.bincount(labels.ravel());sizes[0]=0;main=int(sizes.argmax())
   relevant=[j for j in range(1,n+1) if j!=main and sizes[j]>8]
   if not relevant:return wall
   j=relevant[0];ys,xs=np.where(labels==j);dd,nearest=ndi.distance_transform_edt(labels!=main,return_indices=True);at=int(np.argmin(dd[ys,xs]));qy,qx=int(ys[at]),int(xs[at]);ny,nx=int(nearest[0,qy,qx]),int(nearest[1,qy,qx])
   a=[qx*4+2,479-(qy*4+2)];b=[nx*4+2,479-(ny*4+2)];p['ops'].append(convert(line([a,b],58,True)));p['connectivity_adjustments'].append(dict(kind='widen_conservative_grid_join',a=a,b=b));continue
  _,nearest=ndi.distance_transform_edt(wall,return_indices=True);ny,nx=int(nearest[0,y,x]),int(nearest[1,y,x])
  from fullscreen_map_designs import oval
  p['ops'].append(convert(oval(float(x),float(479-y),36,32,True)))
  p['ops'].append(convert(line([(float(x),float(479-y)),(float(nx),float(479-ny))],42,True)))
  p['connectivity_adjustments'].append(dict(kind='wall_relief_alcove',center=[int(x),int(479-y)],join=[nx,479-ny],diameter_before=float(distance[y,x]*2)))
 raise ValueError(('Wall relief did not converge',p['id']))

def gallery(plans,label):
 assert art.FONT.exists(),'Noto Sans SC SIL OFL1.1 required'
 font=ImageFont.truetype(str(art.FONT),20);rows=math.ceil(len(plans)/5);page=Image.new('RGB',(2200,rows*245+55),(25,32,36));d=ImageDraw.Draw(page)
 d.text((15,10),label+' · 素材总览，非游戏截图',font=font,fill=(190,206,207))
 for k,p in enumerate(plans):
  im=Image.open(A/f'{p["id"]}-image.png');im.thumbnail((430,196));x=k%5*440+5;y=k//5*245+55;page.paste(im,(x,y));d.text((x+5,y+199),f'{p["id"]+1:02d} {p["name_zh"]}',font=font,fill=(190,206,207))
 page.save(O/(label+'.png'));page.thumbnail((1536,1536));page.save(O/(label+'-preview.jpg'),quality=85)

def main():
 ap=argparse.ArgumentParser();ap.add_argument('--all',action='store_true');ap.add_argument('--revision',default='v2');args=ap.parse_args()
 for path in [S/'svg',S/'geometry',A,O]:path.mkdir(parents=True,exist_ok=True)
 art.SOURCE=S;art.load_palette(R/'art/source/comfort-1.48/palette.json')
 art.PALETTES={k:(floor,tuple(round(c*.80) for c in base),rim) for k,(floor,base,rim) in art.PALETTES.items()}
 plans=[]
 for i in (range(50) if args.all else range(0,50,5)):
  p=design(i);wall=relieve_walls(p);p['candidates'],p['walking_distances']=candidates(~wall,8+i%7)
  art.save_svg(p);image,geo=art.artwork(dict(p,id=-100));image.save(A/f'{i}-image.png',optimize=True);geo.save(S/'geometry'/f'{i:02d}.png',optimize=True)
  mask=np.zeros((480,800,4),np.uint8);mask[wall]=255;Image.fromarray(mask).save(A/f'{i}-map.png',optimize=True)
  plans.append(p);print(i,p['name_en'],len(p['candidates']),f'wall={wall.mean():.3f}',flush=True)
 label='all-50-artwork' if args.all else 'samples-'+args.revision
 (S/('maps.json' if args.all else label+'.json')).write_text(json.dumps(dict(license='GPL-3.0-or-later',canonical_size=[2200,1000],runtime_image_size=list(art.SIZE),rule_mask_size=[800,480],maps=plans),ensure_ascii=False,indent=2),encoding='utf8')
 gallery(plans,label)
 (S/'README.md').write_text('# 1.60 原创战略地图\n\nmaps.json/样板JSON和svg为可编辑原件，geometry为实际导出覆盖；运行图和权威掩码由同一覆盖采样。GPL-3.0-or-later，非外购素材。复用comfort-1.48色板与原美术导出管线。总览使用Noto Sans SC（SIL OFL1.1，允许商用），许可见art/source/ui-1.52/NotoSansSC-OFL.txt。样板和失败诊断保留；素材总览不是实机画面。\n',encoding='utf8')
if __name__=='__main__':main()
