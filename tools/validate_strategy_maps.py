"""Unrelaxed 1.60 geometry gates. Failed measurements are retained, not hidden.
Art pipeline only; does not render gameplay. GPL-3.0-or-later.
"""
from pathlib import Path
import json,math,argparse
import numpy as np
from scipy import ndimage as ndi
from scipy.sparse.csgraph import dijkstra
from PIL import Image,ImageDraw,ImageFont
from build_strategy_maps import graph
R=Path(__file__).resolve().parents[1];O=R/'docs/game/strategy-1.60'

def main():
 ap=argparse.ArgumentParser();ap.add_argument('--plans',default='art/source/strategy-1.60/samples-v1.json');ap.add_argument('--label',default='samples-v1');a=ap.parse_args()
 plans=json.loads((R/a.plans).read_text('utf8'))['maps'];floors=[];rows=[]
 for p in plans:
  i=p['id'];wall=np.asarray(Image.open(R/f'build/strategy-new-assets/maps/{i}-map.png'))[:,:,3]>3;floor=~wall;floors.append(floor[::8,::8]);g,walk=graph(floor)
  points=p['candidates'];nodes=[(479-y)//4*200+x//4 for x,y in points];dist=dijkstra(g,indices=nodes)
  ratios=[float(dist[k,nodes[j]]/math.hypot(points[k][0]-points[j][0],points[k][1]-points[j][1])) for k in range(len(nodes)) for j in range(k+1,len(nodes))]
  diameter=float(ndi.distance_transform_edt(np.pad(wall,1,constant_values=False)).max()*2)
  q=[float(x) for x in np.percentile(ratios,[10,50,90])]
  checks=dict(connected=ndi.label(floor)[1]==1,wall_diameter_max120=diameter<=120,detour_p10_115=q[0]>=1.15,detour_median135=q[1]>=1.35,detour_p90_max35=q[2]<=3.5)
  row=dict(id=i,family=p['family'],floor_fraction=float(floor.mean()),wall_diameter=diameter,detour_p10_p50_p90=q,candidates=len(points),checks=checks);rows.append(row)
  print(i,checks,f'wall_diameter={diameter:.1f}, detour={q}',flush=True)
 stack=np.stack(floors);common_floor=float((stack.sum(axis=0)>=math.ceil(.9*len(plans))).mean());inner=np.ones(stack.shape[1:],bool);inner[:1]=False;inner[-1:]=False;inner[:,:1]=False;inner[:,-2:]=False
 common_wall=float(((~stack).sum(axis=0)[inner]>=math.ceil(.9*len(plans))).mean())
 pairs=[]
 for i,u in enumerate(floors):
  for j in range(i+1,len(floors)):
   v=floors[j];value=float((u&v).sum()/(u|v).sum());limit=.85 if plans[i]['family']==plans[j]['family'] else .70
   pairs.append(dict(a=plans[i]['id'],b=plans[j]['id'],jaccard=value,limit=limit,pass_gate=value<=limit))
 global_checks=dict(common_floor_max10=common_floor<=.10,common_wall_max5=common_wall<=.05,wall_diameter_median90=bool(np.median([r['wall_diameter'] for r in rows])<=90),similarities=all(p['pass_gate'] for p in pairs))
 report=dict(status='PASS' if all(global_checks.values()) and all(all(r['checks'].values()) for r in rows) else 'FAIL',sampling='Conservative4px8-neighbor shortest paths, no diagonal corner cuts. 8px Jaccard. Rule coordinates, not displayed physical distances. Wall circle lies inside arena: pad outside with nonwall; v1 unpadded overestimated edge-connected solids, failed report retained.',common_floor=common_floor,common_wall_excluding_frame=common_wall,checks=global_checks,rows=rows,pairs=sorted(pairs,key=lambda p:p['jaccard'],reverse=True),pending_gates=['Navigation topology/exit visibility/edge-disjoint paths','128seed spawn fairness','Actual C++ passage/battles/capacity','MuMu actual sample scenes'])
 (O/(a.label+'-geometry.json')).write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
 # Frequency heatmap is a diagnostic, not gameplay imagery. Noto font checked.
 fontpath=Path('C:/Windows/Fonts/NotoSansSC-VF.ttf');assert fontpath.exists();font=ImageFont.truetype(str(fontpath),18)
 frequency=stack.mean(axis=0);heat=np.zeros((*frequency.shape,3),np.uint8);heat[:,:,0]=(255*(1-frequency)).astype('uint8');heat[:,:,2]=(255*frequency).astype('uint8');heat[:,:,1]=30
 im=Image.fromarray(heat).resize((1200,720),Image.Resampling.NEAREST);page=Image.new('RGB',(1200,770),(20,27,32));page.paste(im,(0,50));d=ImageDraw.Draw(page);d.text((10,10),'地面频率：蓝色经常空地 / 红色经常墙体 · 诊断图，非游戏截图',font=font,fill=(210,220,225));page.save(O/(a.label+'-heatmap.png'))
 print('STATUS',report['status'],'global',global_checks,'common',common_floor,common_wall,flush=True)
if __name__=='__main__':main()
