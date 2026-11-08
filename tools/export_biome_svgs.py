"""Reproduce runtime PNGs from FINAL checked-in SVGs, offline only.
Final SVGs are authoritative artwork+hidden collision; do not rerun rejected
experimental geometry pipelines over them. PyMuPDF version is in provenance.
"""
from build_biome_svg import *
ACTIVE_IDS=[m['id'] for m in json.loads((R/'art/source/catalog-1.64.json').read_text('utf8'))['maps']]
def export(i):
 assert i in ACTIVE_IDS,'Retired map is not part of the runtime catalog'
 path=S/'svg'/f'{i:02}.svg';svg=path.read_text('utf8');root=ET.fromstring(svg)
 assert not root.findall('.//{'+NS+'}image')
 output=render(svg);output.save(A/f'{i}-image.png',optimize=True)
 group=root.find('.//{'+NS+'}g[@id="gameplay_collision"]');assert group is not None
 path=group.find('{'+NS+'}path');data=path.get('d');assert data
 mask=f'<svg xmlns="{NS}" width="800" height="480" viewBox="0 0 800 480"><rect width="800" height="480" fill="black"/><path fill="white" d="{data}"/></svg>'
 wall=np.asarray(render(mask,800,480))[:,:,0]>127;rgba=np.zeros((480,800,4),np.uint8);rgba[wall]=255;Image.fromarray(rgba).save(A/f'{i}-map.png',optimize=True)
 meta=json.loads((S/'collision'/f'{i:02}.json').read_text('utf8'))
 assert digest(A/f'{i}-image.png')==meta['image_sha256'],'Artwork export changed'
 assert digest(A/f'{i}-map.png')==meta['collision_sha256'],'Collision export changed'
 print(i,'SVG export matches verified PNG hashes',flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--id',type=int);args=p.parse_args()
 for i in ([args.id] if args.id is not None else ACTIVE_IDS):export(i)
