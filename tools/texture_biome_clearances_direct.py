"""Final portable vector ground material: no clipPath/pattern/use dependency.
Ground colour paths are intersected with the edit region before serialization.
An exact rendered-pixel guard rejects any repaint outside that region.
"""
from build_biome_svg import *
import re,zipfile,concurrent.futures as cf
def texture(i):
 meta=S/'collision'/f'{i:02}.json';r=json.loads(meta.read_text('utf8'));identity=digest(Path(__file__))
 if r.get('direct_ground_material_sha256')==identity or not (r['visible_connections'] or r.get('physical_gate_widening')):return
 path=S/'svg'/f'{i:02}.svg';svg=path.read_text('utf8');image=Image.open(A/f'{i}-image.png').convert('RGB');before=np.asarray(image);small=np.asarray(image.resize((800,480)));wall=np.asarray(Image.open(A/f'{i}-map.png'))[:,:,3]>3;parts=[]
 for repair in r['visible_connections']:
  a,b=repair['from_xy'],repair['to_xy'];mask=np.zeros((480,800),np.uint8);cv2.line(mask,tuple(a),tuple(b),1,repair['width']);cv2.circle(mask,tuple(a),repair['width']//2,1,-1);cv2.circle(mask,tuple(b),repair['width']//2,1,-1);parts.append(('connection',mask>0))
 for gate in r.get('physical_gate_widening',[]):
  mask=np.zeros((480,800),np.uint8);cv2.circle(mask,tuple(gate['image_xy']),gate['radius'],1,-1);parts.append(('gate',mask>0))
 excluded=np.logical_or.reduce([m for _,m in parts]);distance=ndi.distance_transform_edt(~wall&~excluded);radius=min(16,max(3,int(distance.max())-2));yy,xx=np.where(distance>=radius+1);assert len(xx)
 archive=O/'direct-material-before'/f'{i:02}.zip';archive.parent.mkdir(exist_ok=True);assert not archive.exists()
 with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
  for q in [meta,path,A/f'{i}-image.png']:z.write(q,q.relative_to(R).as_posix())
 groups={'connection':[],'gate':[]};allowed=np.zeros(before.shape[:2],bool);tiles=[];(S/'material-tiles').mkdir(exist_ok=True)
 for n,(kind,mask) in enumerate(parts):
  ys,xs=np.where(mask);cx,cy=float(xs.mean()),float(ys.mean());floor=mask&~wall;target=np.median(small[floor],axis=0) if floor.any() else np.median(small[~wall],axis=0)
  score=(xx-cx)**2+(yy-cy)**2+6*np.sum((small[yy,xx].astype(float)-target)**2,axis=1);at=int(score.argmin());sx,sy=int(xx[at]),int(yy[at]);left,top=sx-radius,sy-radius;right,bottom=sx+radius,sy+radius
  tile=image.crop((round(left*image.width/800),round(top*image.height/480),round(right*image.width/800),round(bottom*image.height/480)));tile_path=S/'material-tiles'/f'{i:02}-{n}.png';tile.save(tile_path,optimize=True)
  indexed=tile.quantize(colors=32,dither=Image.Dither.NONE);labels=np.asarray(indexed);palette=np.array(indexed.getpalette(),np.uint8).reshape(-1,3)
  # One source-grid pixel covers the old flat fill's antialias fringe. This is
  # a visual material margin only, never a new authority/collision region.
  paint=cv2.dilate(mask.astype(np.uint8),np.ones((3,3),np.uint8))>0
  hd=np.asarray(Image.fromarray(np.uint8(paint)*255).resize(image.size,Image.Resampling.NEAREST))>0;allowed|=hd
  hy,hx=np.where(hd);x0,x1,y0,y1=int(hx.min()),int(hx.max())+1,int(hy.min()),int(hy.max())+1;region=hd[y0:y1,x0:x1];pattern=labels[np.arange(y1-y0)[:,None]%tile.height,np.arange(x1-x0)[None,:]%tile.width];pieces=[]
  for colour in np.unique(labels):
   shape=(pattern==colour)&region
   if not shape.any():continue
   rgb=palette[colour];fill=f'#{rgb[0]:02x}{rgb[1]:02x}{rgb[2]:02x}';pieces.append(f'<path fill="{fill}" d="{run_paths(shape)}"/>')
  groups[kind].append(f'<g transform="translate({x0} {y0})">'+''.join(pieces)+'</g>');tiles.append(dict(source_native_rect=[left,top,right,bottom],target_kind=kind,tile=tile_path.relative_to(R).as_posix()))
 for kind,groupid in [('connection','visible_ground_connections'),('gate','tested_gate_clearance')]:
  if not groups[kind]:continue
  replacement=f'<g id="{groupid}" transform="scale(0.6666666666666666 0.6666666666666666)">'+''.join(groups[kind])+'</g>'
  svg,count=re.subn(r'<g id="'+groupid+r'"[^>]*>.*?</g>',lambda m:replacement,svg,count=1,flags=re.S);assert count==1,(i,groupid)
 output=render(svg);after=np.asarray(output);guard=cv2.dilate(allowed.astype(np.uint8),np.ones((7,7),np.uint8))>0;outside=int(np.count_nonzero(np.any(before!=after,axis=2)&~guard));assert outside==0,(i,'repaint outside material edit area',outside)
 assert r['collision_sha256']==digest(A/f'{i}-map.png')
 path.write_text(svg,encoding='utf8');output.save(A/f'{i}-image.png',optimize=True)
 r.update(svg_sha256=digest(path),image_sha256=digest(A/f'{i}-image.png'),svg_bytes=path.stat().st_size,png_bytes=(A/f'{i}-image.png').stat().st_size,direct_ground_material_sha256=identity,floor_texture_tiles=tiles,outside_material_edit_pixels=outside,collision_unchanged_by_material=True)
 meta.write_text(json.dumps(r,ensure_ascii=False,indent=2),encoding='utf8');print('DIRECT',i,len(parts),'outside edit',outside,'collision unchanged',flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--ids',nargs='*',type=int);args=p.parse_args()
 with cf.ThreadPoolExecutor(max_workers=2) as pool:list(pool.map(texture,args.ids if args.ids is not None else range(50)))
