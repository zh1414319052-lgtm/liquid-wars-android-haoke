"""Locally widen physically tested choke points; no rule/spawn changes."""
from build_biome_svg import *
import csv,zipfile,concurrent.futures as cf
def main():
 gates=list(csv.DictReader((O/'gates/physical-gates-final.csv').open()))
 results=list(csv.DictReader((O/'gates/passage-final-v2.csv').open()))
 assert len(results)==2*len(gates)
 selected={}
 for index,row in enumerate(results):
  if float(row['t90_s'])>=0:continue
  gate=gates[index//2];i=int(gate['map']);xy=(int(gate['door_x']),479-int(gate['door_y']));selected.setdefault(i,set()).add(xy)
 def widen(item):
  i,points=item;meta=S/'collision'/f'{i:02}.json';record=json.loads(meta.read_text('utf8'));archive=O/'gates'/f'{i:02}-before-local-widen-v2.zip';assert not archive.exists()
  with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
   for q in [meta,S/'svg'/f'{i:02}.svg',A/f'{i}-map.png',A/f'{i}-image.png']:z.write(q,q.relative_to(R).as_posix())
  wall=np.asarray(Image.open(A/f'{i}-map.png'))[:,:,3]>3;image=Image.open(A/f'{i}-image.png').convert('RGB');rgb=np.asarray(image.resize((800,480)));patches=[]
  for x,y in sorted(points):
   area=np.zeros((480,800),np.uint8);cv2.circle(area,(x,y),36,1,-1);sample=(area>0)&~wall;colour=np.median(rgb[sample],axis=0).astype(int);fill=f'#{colour[0]:02x}{colour[1]:02x}{colour[2]:02x}'
   wall[area>0]=False;patches.append(f'<circle cx="{x}" cy="{y}" r="36" fill="{fill}"/>')
  wall[:3,:]=wall[-3:,:]=True;wall[:,:3]=True;wall[:,787:]=True;assert ndi.label(~wall)[1]==1
  svg=(S/'svg'/f'{i:02}.svg').read_text('utf8');start=svg.index('<g id="gameplay_collision"');end=svg.index('</g>',start)+4
  layer=f'<g id="gameplay_collision" opacity="0" style="display:none" transform="scale(1.65 1.25)"><path fill="white" d="{run_paths(wall)}"/></g>'
  svg=svg[:start]+'<g id="tested_gate_clearance" transform="scale(1.65 1.25)">'+''.join(patches)+'</g>'+layer+svg[end:]
  (S/'svg'/f'{i:02}.svg').write_text(svg,encoding='utf8');render(svg).save(A/f'{i}-image.png',optimize=True)
  rgba=np.zeros((480,800,4),np.uint8);rgba[wall]=255;Image.fromarray(rgba).save(A/f'{i}-map.png',optimize=True);(S/'collision'/f'{i:02}.png').write_bytes((A/f'{i}-map.png').read_bytes())
  for key,q in [('svg_sha256',S/'svg'/f'{i:02}.svg'),('image_sha256',A/f'{i}-image.png'),('collision_sha256',A/f'{i}-map.png')]:record[key]=digest(q)
  record.update(physical_gate_widening=[{'image_xy':xy,'radius':36} for xy in sorted(points)],gate_widening_script_sha256=digest(Path(__file__)),wall_fraction=float(wall.mean()),walkable_pixels=int((~wall).sum()),svg_bytes=(S/'svg'/f'{i:02}.svg').stat().st_size,png_bytes=(A/f'{i}-image.png').stat().st_size)
  meta.write_text(json.dumps(record,ensure_ascii=False,indent=2),encoding='utf8');print('WIDENED',i,sorted(points),flush=True)
 with cf.ThreadPoolExecutor(max_workers=2) as pool:list(pool.map(widen,selected.items()))
if __name__=='__main__':main()
