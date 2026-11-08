"""Four independent paintings per map, overlapping minimal-error joins, separate collision."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import numpy as np,cv2,json,hashlib
from scipy import ndimage as ndi
R=Path(__file__).resolve().parents[1];S=R/'art/source/world-1.72';A=R/'app/src/main/assets/maps';O=R/'docs/game/world-1.72';W,H,G=1600,960,8
for d in ['composites','collision','layout']:(S/d).mkdir(exist_ok=True)
PROFILES={52:74,53:73,54:104,55:80,56:69,57:88,58:82,59:82}
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def wallof(rgb,ident):
 b=cv2.GaussianBlur(rgb,(5,5),0).astype(np.int16);grey=cv2.cvtColor(b.astype(np.uint8),cv2.COLOR_RGB2GRAY)
 if ident==56:return (grey>52)&(b[:,:,0]>=b[:,:,1]-5)&(b[:,:,0]-b[:,:,2]>17)
 if ident==55:return (grey>95)|((grey>50)&(b[:,:,0]>55)&(b[:,:,0]-b[:,:,2]>8))
 return grey>PROFILES[ident]
def horizontal(a,b,overlap,ident):
 b=b.copy();am=wallof(a,ident);bm=wallof(b,ident)
 # Match only floor base colour, not its fine texture. Keep obstacle colours intact.
 if (~am).sum()>100 and (~bm).sum()>100:
  delta=np.clip(np.median(a[~am],0)-np.median(b[~bm],0),-14,14)
  b[~bm]=np.clip(b[~bm].astype(float)+delta,0,255).astype(np.uint8)
 aa=a[:,-overlap:];bb=b[:,:overlap];ma=wallof(aa,ident);mb=wallof(bb,ident)
 cost=np.mean(np.abs(aa.astype(np.float32)-bb.astype(np.float32)),2)+250*(ma!=mb)+12*(ma&mb)
 cost+=np.abs(np.arange(overlap)-overlap/2)[None,:]*.015
 # Minimal error vertical seam in the overlap, with locally smooth steps.
 total=cost[0].copy();back=np.empty(cost.shape,np.int8)
 for y in range(1,len(cost)):
  options=np.stack([np.r_[np.inf,total[:-1]],total,np.r_[total[1:],np.inf]])
  choice=np.argmin(options,axis=0);back[y]=choice-1;total=cost[y]+options[choice,np.arange(overlap)]
 path=np.empty(len(cost),np.int32);path[-1]=int(total.argmin())
 for y in range(len(cost)-1,0,-1):path[y-1]=path[y]+back[y,path[y]]
 mask=(np.arange(overlap)[None,:]>=path[:,None]).astype(np.float32)
 mask=cv2.GaussianBlur(mask,(7,7),0)[:,:,None]
 out=np.concatenate([a[:,:-overlap],np.clip(aa*(1-mask)+bb*mask,0,255).astype(np.uint8),b[:,overlap:]],1)
 return out
def export(ident):
 sources={q:(S/f'revisions/{ident}-{q}.png' if (S/f'revisions/{ident}-{q}.png').exists() else S/f'originals/{ident}-{q}.png') for q in ['nw','ne','sw','se']}
 ims=[Image.open(sources[q]).convert('RGB') for q in ['nw','ne','sw','se']]
 tw=min(i.width for i in ims);th=min(i.height for i in ims);ims=[np.array(i.resize((tw,th),Image.Resampling.LANCZOS)) if i.size!=(tw,th) else np.array(i) for i in ims]
 ox=round(tw*.2/2)*2;oy=round(th*.2/2)*2
 top=horizontal(ims[0],ims[1],ox,ident);bottom=horizontal(ims[2],ims[3],ox,ident)
 rgb=horizontal(top.transpose(1,0,2),bottom.transpose(1,0,2),oy,ident).transpose(1,0,2).copy()
 hh,ww=rgb.shape[:2];assert ww%2==0 and hh%2==0
 Image.fromarray(rgb).save(S/f'composites/{ident}.png',optimize=True)
 Image.fromarray(rgb).resize((ww//2,hh//2),Image.Resampling.LANCZOS).save(A/f'{ident}-image.png',optimize=True)
 pad=np.pad(rgb,((G,G),(G,G),(0,0)),mode='edge')
 for k in range(4):
  x=k%2*(ww//2);y=k//2*(hh//2);Image.fromarray(pad[y:y+hh//2+2*G,x:x+ww//2+2*G]).save(A/f'{ident}-tile{k}.png',optimize=True)
 small=np.array(Image.fromarray(rgb).resize((W,H),Image.Resampling.LANCZOS));wall=wallof(small,ident)
 wall=cv2.morphologyEx(wall.astype(np.uint8),cv2.MORPH_CLOSE,np.ones((1 if ident==59 else 3,1 if ident==59 else 3),np.uint8))>0
 labels,_=ndi.label(wall);sizes=np.bincount(labels.ravel());wall&=sizes[labels]>=32
 wall[:4]=wall[-4:]=True;wall[:,:4]=True;wall[:,-13:]=True
 labels,n=ndi.label(~wall);sizes=np.bincount(labels.ravel());sizes[0]=0;largest=sizes.argmax();pockets=sorted([int(v) for k,v in enumerate(sizes) if k and k!=largest],reverse=True)
 sealed=(labels!=largest)&~wall;wall|=sealed;assert ndi.label(~wall)[1]==1
 free=ndi.binary_erosion(~wall,iterations=3);labels,_=ndi.label(free);sizes=np.bincount(labels.ravel());sizes[0]=0;parts=[int(n) for n in sizes[1:] if n>1000];clearance=max(parts)/sum(parts)
 rgba=np.zeros((H,W,4),np.uint8);rgba[wall]=255;Image.fromarray(rgba).save(A/f'{ident}-map.png',optimize=True);(S/f'collision/{ident}.png').write_bytes((A/f'{ident}-map.png').read_bytes())
 meta=dict(id=ident,width=W,height=H,area_ratio=4,source_tiles=4,source_size=[tw,th],composite_size=[ww,hh],runtime_core_size=[ww//2,hh//2],stitch='20percent overlap, minimal-error seam;7px local feather;floor colour calibration',overlap=[ox,oy],runtime_tile_gutter=G,ground_components=1,wall_fraction=float(wall.mean()),walkable_pixels=int((~wall).sum()),sealed_pit_pixels=int(sealed.sum()),unconnected_pocket_sizes_before_seal=pockets,clearance3_main_fraction=clearance,image_sha256=sha(A/f'{ident}-image.png'),collision_sha256=sha(A/f'{ident}-map.png'),tile_sha256={str(k):sha(A/f'{ident}-tile{k}.png') for k in range(4)},source_files={q:str(p.relative_to(R)) for q,p in sources.items()},source_sha256={q:sha(p) for q,p in sources.items()})
 (S/f'collision/{ident}.json').write_text(json.dumps(meta,ensure_ascii=False,indent=2),encoding='utf8')
 Image.fromarray(rgb).resize((1280,768),Image.Resampling.LANCZOS).save(O/f'map{ident}-overview.jpg',quality=86)
 overlay=small.copy();overlay[wall]=(overlay[wall]*.65+np.array([200,50,100])*.35).astype(np.uint8);Image.fromarray(overlay).resize((1280,768)).save(O/f'map{ident}-collision.jpg',quality=84)
 svg='<svg xmlns="http://www.w3.org/2000/svg" width="1600" height="960" viewBox="0 0 1600 960"><desc>Four source paintings, overlap seams exported by build_large172_maps.py. Collision separate.</desc>'
 svg+='<image href="../composites/'+str(ident)+'.png" width="1600" height="960"/>'
 (S/f'layout/{ident}.svg').write_text(svg+'</svg>',encoding='utf8')
 print(json.dumps({k:meta[k] for k in ['id','wall_fraction','sealed_pit_pixels','unconnected_pocket_sizes_before_seal','clearance3_main_fraction','composite_size']},ensure_ascii=False),flush=True)
if __name__=='__main__':
 import sys
 for ident in map(int,sys.argv[1:]) if len(sys.argv)>1 else range(52,60):export(ident)
