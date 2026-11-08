if __name__ == "__main__":
 raise SystemExit("REJECTED authoring experiment: see docs/game/biomes-1.62/FAILURES.md. Use final SVG export; direct material authoring is texture_biome_clearances_direct.py.")
"""Texture every remaining flat authoring clearance using its own ground.
Edits final SVG vector material only; PNG is exported by the real asset pipeline.
No collision change, image-model call, embedded raster or screenshot retouch.
"""
from build_biome_svg import *
import re,zipfile,concurrent.futures as cf
def texture(i):
 meta=S/'collision'/f'{i:02}.json';r=json.loads(meta.read_text('utf8'))
 if i==0 or r.get('textured_clearance_script') or not (r['visible_connections'] or r.get('physical_gate_widening')):return
 path=S/'svg'/f'{i:02}.svg';svg=path.read_text('utf8');image=Image.open(A/f'{i}-image.png').convert('RGB');small=np.asarray(image.resize((800,480)));wall=np.asarray(Image.open(A/f'{i}-map.png'))[:,:,3]>3;parts=[]
 for repair in r['visible_connections']:
  a,b=repair['from_xy'],repair['to_xy'];mask=np.zeros((480,800),np.uint8);cv2.line(mask,tuple(a),tuple(b),1,repair['width']);cv2.circle(mask,tuple(a),repair['width']//2,1,-1);cv2.circle(mask,tuple(b),repair['width']//2,1,-1);parts.append(('connection',mask>0))
 for gate in r.get('physical_gate_widening',[]):
  mask=np.zeros((480,800),np.uint8);cv2.circle(mask,tuple(gate['image_xy']),gate['radius'],1,-1);parts.append(('gate',mask>0))
 excluded=np.logical_or.reduce([m for _,m in parts]);distance=ndi.distance_transform_edt(~wall&~excluded);radius=min(16,max(3,int(distance.max())-2));yy,xx=np.where(distance>=radius+1);assert len(xx)
 archive=O/'flat-material-before-texture'/f'{i:02}.zip';archive.parent.mkdir(exist_ok=True);assert not archive.exists()
 with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
  for q in [meta,path,A/f'{i}-image.png']:z.write(q,q.relative_to(R).as_posix())
 groups={'connection':[],'gate':[]};tiles=[];(S/'material-tiles').mkdir(exist_ok=True)
 for n,(kind,mask) in enumerate(parts):
  ys,xs=np.where(mask);cx,cy=float(xs.mean()),float(ys.mean());floor=mask&~wall;target=np.median(small[floor],axis=0) if floor.any() else np.median(small[~wall],axis=0)
  score=(xx-cx)**2+(yy-cy)**2+6*np.sum((small[yy,xx].astype(float)-target)**2,axis=1);at=int(score.argmin());sx,sy=int(xx[at]),int(yy[at]);left,top=sx-radius,sy-radius;right,bottom=sx+radius,sy+radius
  tile=image.crop((round(left*image.width/800),round(top*image.height/480),round(right*image.width/800),round(bottom*image.height/480)));tile_path=S/'material-tiles'/f'{i:02}-{n}.png';tile.save(tile_path,optimize=True)
  indexed=tile.quantize(colors=32,dither=Image.Dither.NONE);labels=np.asarray(indexed);palette=np.array(indexed.getpalette(),np.uint8).reshape(-1,3);colour_paths=[]
  for colour in np.unique(labels):
   rgb=palette[colour];fill=f'#{rgb[0]:02x}{rgb[1]:02x}{rgb[2]:02x}';colour_paths.append(f'<path fill="{fill}" d="{run_paths(labels==colour)}"/>')
  tile_id=f'ground_texture_{i}_{n}';clip_id=f'ground_clip_{i}_{n}';definition=f'<defs><g id="{tile_id}">'+''.join(colour_paths)+f'</g><clipPath id="{clip_id}"><path d="{run_paths(mask)}"/></clipPath></defs>'
  side=radius*2;uses=[]
  for ty in range(int(ys.min()),int(ys.max())+1,side):
   for tx in range(int(xs.min()),int(xs.max())+1,side):uses.append(f'<use href="#{tile_id}" transform="translate({tx} {ty}) scale({side/tile.width} {side/tile.height})"/>')
  groups[kind].append(definition+f'<g clip-path="url(#{clip_id})">'+''.join(uses)+'</g>');tiles.append(dict(source_native_rect=[left,top,right,bottom],target_kind=kind,tile=tile_path.relative_to(R).as_posix()))
 for kind,groupid in [('connection','visible_ground_connections'),('gate','tested_gate_clearance')]:
  if not groups[kind]:continue
  replacement=f'<g id="{groupid}" transform="scale(1.65 1.25)">'+''.join(groups[kind])+'</g>'
  svg,count=re.subn(r'<g id="'+groupid+r'"[^>]*>.*?</g>',lambda m:replacement,svg,count=1,flags=re.S);assert count==1,(i,groupid)
 path.write_text(svg,encoding='utf8');render(svg).save(A/f'{i}-image.png',optimize=True);assert r['collision_sha256']==digest(A/f'{i}-map.png')
 r.update(svg_sha256=digest(path),image_sha256=digest(A/f'{i}-image.png'),svg_bytes=path.stat().st_size,png_bytes=(A/f'{i}-image.png').stat().st_size,textured_clearance_script=digest(Path(__file__)),floor_texture_tiles=tiles)
 meta.write_text(json.dumps(r,ensure_ascii=False,indent=2),encoding='utf8');print('TEXTURED',i,len(parts),'collision unchanged',flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--ids',nargs='*',type=int);args=p.parse_args()
 with cf.ThreadPoolExecutor(max_workers=2) as pool:list(pool.map(texture,args.ids if args.ids is not None else range(50)))
