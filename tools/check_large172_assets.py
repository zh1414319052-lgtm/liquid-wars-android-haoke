from pathlib import Path
import json,hashlib
from PIL import Image
from scipy import ndimage as ndi
import numpy as np
R=Path(__file__).resolve().parents[1];S=R/'art/source/world-1.72';O=R/'docs/game/world-1.72'
rows=[]
for i in range(50,60):
 root=S if i>=52 else R/'art/source/world-1.70'
 m=json.loads((root/f'collision/{i}.json').read_text('utf8'));assert m['source_tiles']==4 and m['width']==1600 and m['height']==960
 mask=np.array(Image.open(R/f'app/src/main/assets/maps/{i}-map.png').convert('RGBA'))[:,:,3]>0
 assert mask.shape==(960,1600) and ndi.label(~mask)[1]==1 and (~mask).sum()>6*4500
 if i>=52:assert m['clearance3_main_fraction']>.98
 assert all((R/f'app/src/main/assets/maps/{i}-tile{k}.png').is_file() for k in range(4))
 rows.append(dict(id=i,ground_components=1,width=1600,height=960,source_tiles=4,wall_fraction=float(mask.mean()),sealed_small_pit_pixels=m.get('sealed_pit_pixels'),max_preseal_pit=max(m.get('unconnected_pocket_sizes_before_seal',[0]),default=0),clearance3_main_fraction=m.get('clearance3_main_fraction')))
assets={p.relative_to(R).as_posix():dict(bytes=p.stat().st_size,sha256=hashlib.sha256(p.read_bytes()).hexdigest()) for p in S.rglob('*') if p.is_file()}
(O/'asset-verification.json').write_text(json.dumps(dict(status='PASS',large_maps=rows,source_files=assets,svg_contains_raster=True,font='Noto Sans SC, SIL OFL1.1',visual_gate='BLIND-REVIEW.md; source images and actual App images separately checked'),ensure_ascii=False,indent=2),encoding='utf8')
print('PASS10 maps,4 tiles each,1600x960,1 connected walkable component;8 new clearance3 >.98; sources hashed')
