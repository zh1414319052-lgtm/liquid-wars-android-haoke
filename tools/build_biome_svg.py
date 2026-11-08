"""Pure vector biome artwork + hidden authoritative collision, PNG export.
Offline authoring only. Existing PyMuPDF renders vector paths; no APK dependency.
No embedded raster images, no game RNG, no alternative game renderer.
"""
from pathlib import Path
import argparse,json,hashlib,xml.etree.ElementTree as ET,html,time
import cv2,numpy as np,fitz
from PIL import Image,ImageEnhance
from scipy import ndimage as ndi
R=Path(__file__).resolve().parents[1];S=R/'art/source/biomes-1.62';O=R/'docs/game/biomes-1.62';A=R/'build/biome-assets/maps';W,H=1320,600
NS='http://www.w3.org/2000/svg'
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def render(svg,width=1980,height=900):
 doc=fitz.open(stream=svg.encode('utf8'),filetype='svg');page=doc[0];rect=page.rect;pix=page.get_pixmap(matrix=fitz.Matrix(width/rect.width,height/rect.height),alpha=False);im=Image.frombytes('RGB',[pix.width,pix.height],pix.samples);doc.close()
 assert im.size==(width,height),(im.size,width,height)
 return im

def vectorize(image):
 # Palette approximation is declared; original high-resolution image is retained.
 im=ImageEnhance.Color(image.resize((W,H),Image.Resampling.LANCZOS)).enhance(.85);im=ImageEnhance.Brightness(im).enhance(.95)
 indexed=im.quantize(colors=96,method=Image.Quantize.MEDIANCUT,dither=Image.Dither.NONE);labels=np.asarray(indexed);palette=np.array(indexed.getpalette(),np.uint8).reshape(-1,3);counts=np.bincount(labels.ravel(),minlength=len(palette));order=np.argsort(counts)[::-1]
 bg=palette[order[0]];parts=[f'<rect width="{W}" height="{H}" fill="#{bg[0]:02x}{bg[1]:02x}{bg[2]:02x}"/>'];subpaths=0;vertices=0
 for colour in order:
  if not counts[colour]:continue
  contours,_=cv2.findContours((labels==colour).astype(np.uint8),cv2.RETR_LIST,cv2.CHAIN_APPROX_SIMPLE);paths=[]
  for contour in contours:
   points=cv2.approxPolyDP(contour,.32,True).reshape(-1,2)
   if len(points)<3 or cv2.contourArea(contour)<.8:
    # Small colour components are essential surface texture. Do not silently
    # throw them away and replace most artwork by the background colour.
    for x,y in contour.reshape(-1,2):paths.append(f'M{x-.5},{y-.5}h1v1h-1Z')
    subpaths+=len(contour);vertices+=4*len(contour);continue
   paths.append('M'+'L'.join(f'{x},{y}' for x,y in points)+'Z');subpaths+=1;vertices+=len(points)
  if paths:
   rgb=palette[colour];fill=f'#{rgb[0]:02x}{rgb[1]:02x}{rgb[2]:02x}';parts.append(f'<path fill="{fill}" stroke="{fill}" stroke-width="0.9" stroke-linejoin="round" fill-rule="evenodd" d="{" ".join(paths)}"/>')
 return '\n'.join(parts),dict(palette_colours=int(np.count_nonzero(counts)),subpaths=subpaths,vertices=vertices,saturation_multiplier=.85,brightness_multiplier=.95,trace_size=[W,H])

def run_paths(mask):
 rows=[]
 for y in range(mask.shape[0]):
  pad=np.pad(mask[y].astype(np.int8),(1,1));starts=np.where(np.diff(pad)==1)[0];ends=np.where(np.diff(pad)==-1)[0]
  for x,end in zip(starts,ends):rows.append(f'M{x},{y}h{end-x}v1h{x-end}Z')
 return ''.join(rows)

def topology(image,map_id):
 small=np.asarray(image.resize((800,480),Image.Resampling.LANCZOS));gray=cv2.cvtColor(small,cv2.COLOR_RGB2GRAY);smooth=cv2.GaussianBlur(gray,(3,3),0)
 threshold,_=cv2.threshold(smooth,0,255,cv2.THRESH_BINARY+cv2.THRESH_OTSU)
 config_path=S/'mask-calibration.json';cal=json.loads(config_path.read_text('utf8')) if config_path.exists() else {}
 if str(map_id) in cal:threshold=float(cal[str(map_id)]['threshold'])
 wall=smooth>threshold
 if map_id==0:
  manual=json.loads((S/'ruins-manual-collision.json').read_text('utf8'));wall=np.zeros((480,800),np.uint8)
  for path in manual['paths']:
   pts=np.array(path['points'],np.int32);cv2.polylines(wall,[pts],False,1,path['width'],cv2.LINE_8)
   for pt in [pts[0],pts[-1]]:cv2.circle(wall,tuple(pt),path['width']//2,1,-1)
  wall=wall>0;threshold=-1

 # Discard only tiny texture flecks, not meaningful rock footprints.
 lab,n=ndi.label(wall);sizes=np.bincount(lab.ravel());wall&=sizes[lab]>=12
 wall=cv2.morphologyEx(wall.astype(np.uint8),cv2.MORPH_CLOSE,np.ones((3,3),np.uint8))>0
 lab,n=ndi.label(~wall);sizes=np.bincount(lab.ravel());edge=set(np.concatenate([lab[0],lab[-1],lab[:,0],lab[:,-1]]).tolist());tiny=np.zeros_like(wall)
 for k in range(1,n+1):
  # Closed hollows inside obstacle caps are decorative depressions, not doors.
  if sizes[k]<96 or (k not in edge and sizes[k]<4500):tiny|=lab==k
 wall|=tiny
 wall[:3,:]=wall[-3:,:]=True;wall[:,:3]=True;wall[:,787:]=True
 repairs=[]
 plan=json.loads((S/'plan.json').read_text('utf8'))['maps'][map_id]
 if plan.get('topology_mode')=='single_visible_floor_no_carves':
  labels,count=ndi.label(~wall);sizes=np.bincount(labels.ravel());sizes[0]=0;main=int(sizes.argmax());sealed=(labels!=main)&~wall
  # New images are authored with an open visible floor network. Dark isolated
  # depressions in solid caps are nonplayable shading, never spawn pockets.
  # Do not connect them by painting an artificial bar across the rock.
  wall|=sealed;tiny|=sealed
 for _ in range(64):
  lab,n=ndi.label(~wall)
  if n==1:break
  sizes=np.bincount(lab.ravel());sizes[0]=0;main=int(sizes.argmax());others=[k for k in range(1,n+1) if k!=main]
  smalls=[k for k in others if sizes[k]<96]
  if smalls:
   wall[np.isin(lab,smalls)]=True;continue
  second=max(others,key=lambda k:sizes[k]);distance,indices=ndi.distance_transform_edt(lab!=main,return_indices=True);yy,xx=np.where(lab==second);which=int(np.argmin(distance[yy,xx]));y,x=int(yy[which]),int(xx[which]);my,mx=int(indices[0,y,x]),int(indices[1,y,x]);length=float(distance[y,x]);width=14
  cut=np.zeros((480,800),np.uint8);cv2.line(cut,(x,y),(mx,my),1,width);cv2.circle(cut,(x,y),width//2,1,-1);cv2.circle(cut,(mx,my),width//2,1,-1);wall[cut>0]=False
  repairs.append(dict(from_xy=[x,y],to_xy=[mx,my],width=width,length=round(length,2),reason='Visible same-plane terrain breach to connect separated floor; not spawn/fairness correction'))
 else:raise ValueError('More than64 disconnected ground components')
 wall[:3,:]=wall[-3:,:]=True;wall[:,:3]=True;wall[:,787:]=True
 assert ndi.label(~wall)[1]==1,'Ground still disconnected';assert (~wall).sum()>27000,'Insufficient total floor'
 floor_rgb=np.median(small[~wall],axis=0).astype(int);cap_rgb=np.median(small[wall],axis=0).astype(int);return wall,repairs,threshold,floor_rgb,cap_rgb,int(tiny.sum())

def build(map_id):
 started=time.time();source=S/'originals'/f'{map_id:02}.png';plan=json.loads((S/'plan.json').read_text('utf8'))['maps'][map_id];image=Image.open(source).convert('RGB');art,stats=vectorize(image)
 header=f'<svg xmlns="{NS}" width="1980" height="900" viewBox="0 0 {W} {H}"><title>{html.escape(plan["name_en"])}</title><desc>Pure vector palette tracing of individually generated artwork;96colour approximation. Collision is an independent hidden vector layer.</desc>'
 first=header+'<g id="vector_art">'+art+'</g></svg>';initial=render(first);wall,repairs,threshold,floor,cap,tiny=topology(initial,map_id)
 colour=f'#{floor[0]:02x}{floor[1]:02x}{floor[2]:02x}'
 defs=f'<defs><pattern id="floor_material" width="32" height="32" patternUnits="userSpaceOnUse"><rect width="32" height="32" fill="{colour}"/><path d="M3,7L12,9L17,5M21,24L30,27M7,25L10,31" fill="none" stroke="black" stroke-opacity=".055" stroke-width=".5"/></pattern></defs>'
 cuts=[]
 for p in repairs:
  a,b=p['from_xy'],p['to_xy'];cuts.append(f'<path d="M{a[0]},{a[1]}L{b[0]},{b[1]}" fill="none" stroke="{colour}" stroke-width="{p["width"]}" stroke-linecap="round"/>')
 patch='<g id="visible_ground_connections" transform="scale(1.65 1.25)">'+''.join(cuts)+'</g>'
 collision=run_paths(wall);layer=f'<g id="gameplay_collision" opacity="0" style="display:none" transform="scale(1.65 1.25)"><path fill="white" d="{collision}"/></g>'
 svg=header+defs+'<g id="vector_art">'+art+'</g>'+patch+layer+'</svg>';tree=ET.fromstring(svg);assert not tree.findall('.//{'+NS+'}image')
 output=render(svg);A.mkdir(parents=True,exist_ok=True);output.save(A/f'{map_id}-image.png',optimize=True)
 rgba=np.zeros((480,800,4),np.uint8);rgba[wall]=255;Image.fromarray(rgba).save(A/f'{map_id}-map.png',optimize=True)
 (S/'svg'/f'{map_id:02}.svg').write_text(svg,encoding='utf8');(S/'collision'/f'{map_id:02}.png').write_bytes((A/f'{map_id}-map.png').read_bytes())
 # An independent SVG-only rasterization verifies exact mask cells, not the painting's RGB.
 mask_svg=f'<svg xmlns="{NS}" width="800" height="480" viewBox="0 0 800 480"><rect width="800" height="480" fill="black"/><path fill="white" d="{collision}"/></svg>';derived=np.asarray(render(mask_svg,800,480))[:,:,0]>127;assert np.array_equal(derived,wall),'SVG collision cells differ from runtime'
 # Hidden vector metadata must not repaint the game image.
 assert np.array_equal(np.asarray(output),np.asarray(render(svg.replace(layer,'')))),'Hidden collision renders visibly'
 previews=O/'overlays';previews.mkdir(exist_ok=True);im=np.asarray(output.resize((800,480))).copy();im[wall]=(im[wall]*.55+np.array([220,65,80])*.45).astype(np.uint8);Image.fromarray(im).resize((1100,500)).save(previews/f'{map_id:02}.jpg',quality=84)
 record=dict(id=map_id,name_zh=plan['name_zh'],name_en=plan['name_en'],status='READY_FOR_VISUAL_REVIEW',source_sha256=digest(source),svg_sha256=digest(S/'svg'/f'{map_id:02}.svg'),image_sha256=digest(A/f'{map_id}-image.png'),collision_sha256=digest(A/f'{map_id}-map.png'),svg_bytes=(S/'svg'/f'{map_id:02}.svg').stat().st_size,png_bytes=(A/f'{map_id}-image.png').stat().st_size,wall_fraction=float(wall.mean()),walkable_pixels=int((~wall).sum()),ground_components=1,threshold=float(threshold),tiny_texture_cells_sealed=tiny,visible_connections=repairs,vector=stats,render='Existing offline PyMuPDF; no runtime dependency',embedded_images=0,elapsed_s=round(time.time()-started,2),pipeline_sha256=digest(Path(__file__)))
 (S/'collision'/f'{map_id:02}.json').write_text(json.dumps(record,ensure_ascii=False,indent=2),encoding='utf8');print(json.dumps({k:record[k] for k in ['id','wall_fraction','walkable_pixels','svg_bytes','png_bytes','elapsed_s']})+f' repairs={len(repairs)}',flush=True);return record
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--id',type=int,required=True);a=p.parse_args();build(a.id)
