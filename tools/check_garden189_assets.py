"""Read-only runtime geometry and unchanged gameplay-space checks."""
from pathlib import Path
import struct,json,hashlib
import numpy as np
r=Path(__file__).resolve().parents[1];o=r/'docs/game/garden-1.89'
raw=(r/'app/src/main/assets/garden3d/live/meshes.bin').read_bytes()
magic,count=struct.unpack_from('<4sI',raw);assert magic==b'LWD2';at=8;tri=0;types={}
for i in range(count):
    n,k,*bounds=struct.unpack_from('<II6f',raw,at);at+=32
    a=np.frombuffer(raw,'<f4',n*11,at).reshape(n,11);at+=n*44
    assert n%3==0 and np.isfinite(a).all() and k in (0,1,2)
    assert np.max(np.abs(np.linalg.norm(a[:,3:6],axis=1)-1))<.01
    assert a[:,9].min()>=0 and a[:,9].max()<=1
    tri+=n//3;types[k]=types.get(k,0)+n//3
assert at==len(raw)
previous={k.replace('\\','/'):v for k,v in json.loads((r/'docs/game/garden-1.88/asset-manifest.json').read_text(encoding='utf-8')).items()}
unchanged=[]
for p in ['height.bin','arena-map.png','sites.bin','bridges.bin']:
    key='app/src/main/assets/garden3d/'+p
    assert hashlib.sha256((r/key).read_bytes()).hexdigest()==previous[key]['sha256']
    unchanged.append(p)
report={'result':'PASS','chunks':count,'triangles':tri,'triangles_by_kind':types,'normals':'finite unit vectors','collision_height_sites_bridges_unchanged':unchanged,'geometry_bytes':len(raw)}
(o/'assets-check.json').write_text(json.dumps(report,indent=2));print(report)
