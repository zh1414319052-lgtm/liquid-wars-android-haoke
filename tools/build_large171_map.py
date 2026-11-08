"""Eight independently painted tiles; real 3200x960 floor grid, not an enlarged preview."""
from pathlib import Path
from PIL import Image
import numpy as np,cv2,json,hashlib
from scipy import ndimage as ndi
R=Path(__file__).resolve().parents[1];S=R/'art/source/world-1.71';A=R/'app/src/main/assets/maps';O=R/'docs/game/world-1.71';W,H,G=3200,960,8
for d in ['composites','collision','layout']:(S/d).mkdir(exist_ok=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
ims=[Image.open(S/'originals'/f'52-{k}.png').convert('RGB') for k in range(8)]
tw=min(im.width for im in ims);th=min(im.height for im in ims)
ims=[im.resize((tw,th),Image.Resampling.LANCZOS) if im.size!=(tw,th) else im for im in ims]
rgb=np.concatenate([np.concatenate([np.array(i) for i in ims[row*4:row*4+4]],1) for row in range(2)],0)
raw=rgb.copy()
if (S/'seam-paints/0.png').exists():
 for k in range(4):
  patch=np.array(Image.open(S/f'seam-paints/{k}.png').convert('RGB').resize((tw,th),Image.Resampling.LANCZOS))
  y=th-th//2;old=rgb[y:y+th,k*tw:(k+1)*tw]
  yy=np.arange(th);fade=np.minimum(1,np.minimum(yy,(th-1)-yy)/96).clip(0,1)[:,None,None]
  old[:]=np.clip(old*(1-fade)+patch*fade,0,255).astype(np.uint8)
if (S/'vertical-paints/0.png').exists():
 for row in range(2):
  for seam in range(1,4):
   k=row*3+seam-1;patch=np.array(Image.open(S/f'vertical-paints/{k}.png').convert('RGB').resize((tw,th),Image.Resampling.LANCZOS));x=seam*tw-tw//2
   old=rgb[row*th:(row+1)*th,x:x+tw];xx=np.arange(tw);yy=np.arange(th)
   fx=np.minimum(1,np.minimum(xx,tw-1-xx)/96).clip(0,1)[None,:,None];fy=np.minimum(1,np.minimum(yy,th-1-yy)/64).clip(0,1)[:,None,None];fade=fx*fy
   old[:]=np.clip(old*(1-fade)+patch*fade,0,255).astype(np.uint8)
def walls(a):
 b=cv2.GaussianBlur(a,(5,5),0).astype(np.int16)
 # Cold blue-green floor has much less red than blue; muted stone retains red.
 # Avoid classifying only the bright top facets as solid.
 return (b[:,:,0]-b[:,:,2]>-14)&(b[:,:,0]>42)&(cv2.cvtColor(b.astype(np.uint8),cv2.COLOR_RGB2GRAY)>55)
# Reconcile only floor colour at immediate joins, retaining high-frequency source grain.
for axis,seam in [(1,tw),(1,tw*2),(1,tw*3),(0,th)]:
 a=rgb if axis==1 else rgb.transpose(1,0,2);mask=walls(rgb);mask=mask if axis==1 else mask.T
 delta=np.median(a[:,seam:seam+6].astype(float),1)-np.median(a[:,seam-6:seam].astype(float),1)
 delta=cv2.GaussianBlur(delta.astype(np.float32),(1,31),0);safe=~mask[:,seam-6:seam+6].any(1)
 for k in range(20):
  for x,sign in [(seam-1-k,1),(seam+k,-1)]:a[safe,x]=np.clip(a[safe,x].astype(float)+sign*delta[safe]*(20-k)/40,0,255).astype(np.uint8)
Image.fromarray(raw).save(S/'composites/52-hard-join.png',optimize=True)
Image.fromarray(rgb).save(S/'composites/52.png',optimize=True)
Image.fromarray(rgb).resize((1600,480),Image.Resampling.LANCZOS).save(A/'52-image.png',optimize=True)
pad=np.pad(rgb,((G,G),(G,G),(0,0)),mode='edge')
for k in range(8):
 x=k%4*tw;y=k//4*th;Image.fromarray(pad[y:y+th+2*G,x:x+tw+2*G]).save(A/f'52-tile{k}.png',optimize=True)
small=np.array(Image.fromarray(rgb).resize((W,H),Image.Resampling.LANCZOS));wall=walls(small)
wall=cv2.morphologyEx(wall.astype(np.uint8),cv2.MORPH_CLOSE,np.ones((3,3),np.uint8))>0
labels,_=ndi.label(wall);sizes=np.bincount(labels.ravel());wall&=sizes[labels]>=32
wall[:4]=wall[-4:]=True;wall[:,:4]=True;wall[:,-13:]=True
labels,_=ndi.label(~wall);sizes=np.bincount(labels.ravel());sizes[0]=0;sealed=(labels!=sizes.argmax())&~wall;wall|=sealed
assert ndi.label(~wall)[1]==1
free=ndi.binary_erosion(~wall,iterations=3);labels,_=ndi.label(free);sizes=np.bincount(labels.ravel());sizes[0]=0
parts=[int(n) for n in sizes[1:] if n>1000];clearance=max(parts)/sum(parts);assert clearance>.98,clearance
rgba=np.zeros((H,W,4),np.uint8);rgba[wall]=255;Image.fromarray(rgba).save(A/'52-map.png',optimize=True)
(S/'collision/52.png').write_bytes((A/'52-map.png').read_bytes())
meta=dict(id=52,width=W,height=H,area_ratio=8,source_tiles=8,tile_columns=4,tile_rows=2,tile_source_size=[tw,th],composite_size=[tw*4,th*2],runtime_tile_gutter=G,world_units_per_tile=[800,480],ground_components=1,wall_fraction=float(wall.mean()),walkable_pixels=int((~wall).sum()),sealed_pit_pixels=int(sealed.sum()),clearance3_main_fraction=clearance,seam_modified_pixels=int((rgb!=raw).any(2).sum()),image_sha256=sha(A/'52-image.png'),collision_sha256=sha(A/'52-map.png'),tile_sha256={str(k):sha(A/f'52-tile{k}.png') for k in range(8)},original_sha256={str(k):sha(S/f'originals/52-{k}.png') for k in range(8)})
(S/'collision/52.json').write_text(json.dumps(meta,ensure_ascii=False,indent=2),encoding='utf8')
Image.fromarray(rgb).resize((1536,461),Image.Resampling.LANCZOS).save(O/'map52-overview.jpg',quality=88)
overlay=small.copy();overlay[wall]=(overlay[wall]*.65+np.array([200,50,100])*.35).astype(np.uint8)
Image.fromarray(overlay).resize((1536,461)).save(O/'map52-collision.jpg',quality=85)
svg='<svg xmlns="http://www.w3.org/2000/svg" width="3200" height="960" viewBox="0 0 3200 960"><desc>Eight original bitmap paintings, editable assembly; independent collision PNG.</desc>'
for k in range(8):svg+=f'<image href="../originals/52-{k}.png" x="{k%4*800}" y="{k//4*480}" width="800" height="480"/>'
(S/'layout/52.svg').write_text(svg+'</svg>',encoding='utf8')
print(json.dumps(meta,ensure_ascii=False),flush=True)
