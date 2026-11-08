if __name__ == "__main__":
 raise SystemExit("REJECTED authoring experiment: see docs/game/biomes-1.62/FAILURES.md. Use final SVG export; direct material authoring is texture_biome_clearances_direct.py.")
"""Replace flat fill in four small authored clearances with actual SVG floor.
Vector <use> + clipping only; no embedded bitmap, no screenshot edit and no
collision change. Added after a real App screenshot exposed the flat patches.
"""
from build_biome_svg import *
import re,zipfile
def main():
 meta=S/'collision/00.json';record=json.loads(meta.read_text('utf8'));path=S/'svg/00.svg';svg=path.read_text('utf8');archive=O/'ruins-flat-patches-before-texture.zip';assert not archive.exists()
 with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
  for q in [meta,path,A/'0-image.png',O/'device/ruins-playing.png']:z.write(q,q.relative_to(R).as_posix())
 def copy_floor(mask,centre,source,name):
  dx,dy=centre[0]-source[0],centre[1]-source[1]
  return f'<defs><clipPath id="{name}"><path d="{run_paths(mask)}"/></clipPath></defs><g clip-path="url(#{name})"><use href="#vector_art" transform="translate({dx} {dy}) scale(0.6060606060606061 0.8)"/></g>'
 copies=[]
 for n,repair in enumerate(record['visible_connections']):
  a,b=repair['from_xy'],repair['to_xy'];area=np.zeros((480,800),np.uint8);cv2.line(area,tuple(a),tuple(b),1,repair['width']);cv2.circle(area,tuple(a),repair['width']//2,1,-1);cv2.circle(area,tuple(b),repair['width']//2,1,-1)
  copies.append(copy_floor(area>0,((a[0]+b[0])/2,(a[1]+b[1])/2),(400+n*4,200+n*2),f'ruins_floor_copy{n}'))
 group='<g id="visible_ground_connections" transform="scale(1.65 1.25)">'+''.join(copies)+'</g>'
 svg,count=re.subn(r'<g id="visible_ground_connections"[^>]*>.*?</g>',lambda m:group,svg,count=1,flags=re.S);assert count==1
 copies=[]
 for n,gate in enumerate(record['physical_gate_widening']):
  xy=gate['image_xy'];area=np.zeros((480,800),np.uint8);cv2.circle(area,tuple(xy),gate['radius'],1,-1);copies.append(copy_floor(area>0,xy,(400,200),f'ruins_gate_floor{n}'))
 group='<g id="tested_gate_clearance" transform="scale(1.65 1.25)">'+''.join(copies)+'</g>'
 svg,count=re.subn(r'<g id="tested_gate_clearance"[^>]*>.*?</g>',lambda m:group,svg,count=1,flags=re.S);assert count==1
 path.write_text(svg,encoding='utf8');render(svg).save(A/'0-image.png',optimize=True)
 record.update(svg_sha256=digest(path),image_sha256=digest(A/'0-image.png'),svg_bytes=path.stat().st_size,png_bytes=(A/'0-image.png').stat().st_size,clearance_texture='Existing vector ground copied with clipped SVG use; collision byte-identical',clearance_texture_script_sha256=digest(Path(__file__)))
 assert record['collision_sha256']==digest(A/'0-map.png')
 meta.write_text(json.dumps(record,ensure_ascii=False,indent=2),encoding='utf8');print('Textured4 clearances, collision unchanged',flush=True)
if __name__=='__main__':main()
