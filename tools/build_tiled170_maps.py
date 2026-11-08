"""Stitch four explicitly requested source images per map; preserve native grain, independent collision."""
from pathlib import Path
from PIL import Image
import numpy as np,cv2,json,hashlib
from scipy import ndimage as ndi
R=Path(__file__).resolve().parents[1];S=R/'art/source/world-1.70';A=R/'app/src/main/assets/maps';O=R/'docs/game/world-1.70';W,H=1600,960;G=8
for d in ['composites','collision','layout']:(S/d).mkdir(exist_ok=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def wallof(rgb,ident):
 a=cv2.GaussianBlur(rgb,(5,5),0).astype(np.int16)
 return cv2.cvtColor(a.astype(np.uint8),cv2.COLOR_RGB2GRAY)>93 if ident==50 else (a[:,:,0]-a[:,:,1]>30)&(a[:,:,0]>105)
for ident in [50,51]:
 ims=[Image.open(S/'flat'/f'{ident}-{q}.png').convert('RGB') for q in ['nw','ne','sw','se']];tw=min(im.width for im in ims);th=min(im.height for im in ims);ims=[im.resize((tw,th),Image.Resampling.LANCZOS) if im.size!=(tw,th) else im for im in ims]
 top=np.concatenate([np.array(ims[0]),np.array(ims[1])],1);bottom=np.concatenate([np.array(ims[2]),np.array(ims[3])],1);rgb=np.concatenate([top,bottom],0);raw=rgb.copy()
 # Only reconcile the low-frequency colour discontinuity across floor seams.
 # Keep every source texture pixel's grain; no whole-map blur or sharpening.
 for axis,seam in [(1,tw),(0,th)]:
  a=rgb if axis==1 else rgb.transpose(1,0,2);wall=wallof(rgb,ident);mask=wall if axis==1 else wall.T
  delta=np.median(a[:,seam:seam+6].astype(float),1)-np.median(a[:,seam-6:seam].astype(float),1);delta=cv2.GaussianBlur(delta.astype(np.float32),(1,31),0);safe=~mask[:,seam-6:seam+6].any(1)
  for k in range(16):
   amount=(16-k)/32
   for x,sign in [(seam-1-k,1),(seam+k,-1)]:a[safe,x]=np.clip(a[safe,x].astype(float)+sign*delta[safe]*amount,0,255).astype(np.uint8)
 Image.fromarray(raw).save(S/'composites'/f'{ident}-hard-join.png',optimize=True);Image.fromarray(rgb).save(S/'composites'/f'{ident}.png',optimize=True)
 Image.fromarray(rgb).resize((tw,th),Image.Resampling.LANCZOS).save(A/f'{ident}-image.png',optimize=True)
 # Eight-texel cross-tile gutters contain actual neighbouring artwork. No padding seams.
 padded=np.pad(rgb,((G,G),(G,G),(0,0)),mode='edge')
 for k in range(4):
  x=(k%2)*tw;y=(k//2)*th;Image.fromarray(padded[y:y+th+2*G,x:x+tw+2*G]).save(A/f'{ident}-tile{k}.png',optimize=True)
 small=np.array(Image.fromarray(rgb).resize((W,H),Image.Resampling.LANCZOS));wall=wallof(small,ident);wall=cv2.morphologyEx(wall.astype(np.uint8),cv2.MORPH_CLOSE,np.ones((3,3),np.uint8))>0;lab,n=ndi.label(wall);mass=np.bincount(lab.ravel());wall &= mass[lab]>=32
 wall[:4]=wall[-4:]=True;wall[:,:4]=True;wall[:,-13:]=True
 lab,n=ndi.label(~wall);mass=np.bincount(lab.ravel());mass[0]=0;sealed=(lab!=mass.argmax())&~wall;wall|=sealed;assert ndi.label(~wall)[1]==1
 clear=ndi.binary_erosion(~wall,iterations=3);lab,n=ndi.label(clear);mass=np.bincount(lab.ravel());mass[0]=0;parts=[int(x) for x in mass[1:] if x>1000];frac=max(parts)/sum(parts);assert frac>.98,(ident,'narrow disconnect',frac)
 rgba=np.zeros((H,W,4),np.uint8);rgba[wall]=255;Image.fromarray(rgba).save(A/f'{ident}-map.png',optimize=True);(S/'collision'/f'{ident}.png').write_bytes((A/f'{ident}-map.png').read_bytes())
 meta=dict(id=ident,width=W,height=H,area_ratio=4,source_tiles=4,tile_source_size=[tw,th],composite_size=[tw*2,th*2],runtime_tile_gutter=G,world_units_per_tile=[800,480],ground_components=1,wall_fraction=float(wall.mean()),walkable_pixels=int((~wall).sum()),sealed_pit_pixels=int(sealed.sum()),clearance3_main_fraction=frac,seam_modified_pixels=int((rgb!=raw).any(2).sum()),image_sha256=sha(A/f'{ident}-image.png'),collision_sha256=sha(A/f'{ident}-map.png'),tile_sha256={str(k):sha(A/f'{ident}-tile{k}.png') for k in range(4)},original_sha256={q:sha(S/'flat'/f'{ident}-{q}.png') for q in ['nw','ne','sw','se']},full_original_grain=True)
 (S/'collision'/f'{ident}.json').write_text(json.dumps(meta,ensure_ascii=False,indent=2),encoding='utf8')
 Image.fromarray(rgb).resize((1280,768),Image.Resampling.LANCZOS).save(O/f'map{ident}-overview.jpg',quality=88)
 overlay=small.copy();overlay[wall]=(overlay[wall]*.65+np.array([200,50,100])*.35).astype(np.uint8);Image.fromarray(overlay).resize((1280,768)).save(O/f'map{ident}-collision.jpg',quality=85)
 # Editable raster assembly, explicitly not a pure-vector illustration.
 svg='<svg xmlns="http://www.w3.org/2000/svg" width="1600" height="960" viewBox="0 0 1600 960"><desc>Four original raster paintings; collision separate. Export script performs local seam colour reconciliation.</desc>'
 for k,q in enumerate(['nw','ne','sw','se']):svg+=f'<image href="../flat/{ident}-{q}.png" x="{k%2*800}" y="{k//2*480}" width="800" height="480"/>'
 (S/'layout'/f'{ident}.svg').write_text(svg+'</svg>',encoding='utf8');print(json.dumps(meta,ensure_ascii=False),flush=True)
