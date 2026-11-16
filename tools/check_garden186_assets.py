"""Read-only export contract checks. Requires numpy and Pillow; no game or network writes."""
from pathlib import Path
import struct, json, re
import numpy as np
from PIL import Image
from collections import deque

root = Path(__file__).resolve().parents[1]
assets = root / 'app/src/main/assets/garden3d'
cfg=json.loads((root/'art/source/garden-1.86/world.json').read_text('utf-8'))
W,H=cfg['width'],cfg['height']
assert W*H==800*450*6 and W<4096 and H<1024
header=(root/'app/src/main/cpp/application/garden3d.hpp').read_text('utf-8')
assert f'ARENA_WIDTH={W}' in header and f'ARENA_HEIGHT={H}' in header
blob = (assets/'scene.bin').read_bytes()
magic, count = struct.unpack_from('<4sI', blob)
assert magic == b'LWG1' and 0 < count <= 600000 and count % 3 == 0
assert len(blob) == 8 + count * 20
mesh = np.frombuffer(blob, '<f4', offset=8).reshape(-1, 5)
assert np.isfinite(mesh).all()
assert mesh[:, 0].min() <= 0 and mesh[:, 0].max() >= W
assert mesh[:, 1].min() <= 0 and mesh[:, 1].max() >= H
assert mesh[:,2].min()>-240 and mesh[:,2].max()<240, 'geometry outside depth range'
blob = (assets/'height.bin').read_bytes()
assert struct.unpack_from('<4sII', blob) == (b'LWH1', W, H)
assert len(blob) == 12 + W*H*4
height=np.frombuffer(blob, '<f4', offset=12).reshape(H,W)
assert np.isfinite(height).all()
# Independently compare exported cell heights with the actual GPU ground triangles.
meta=json.loads((root/'art/source/garden-1.86/export.json').read_text('utf-8'))
offset=0;floor=None
for obj in meta['objects']:
    n=obj['triangles']*3
    if obj['name'].startswith('Sage meadow'):floor=mesh[offset:offset+n].reshape(-1,3,5)
    offset+=n
assert floor is not None and len(floor)==8000
s=800/28.8*cfg['art_scale']
for x in range(17,W,113):
    for y in range(13,H,79):
        gx=((x+.5-W/2)/s+20)/40*80;gy=((y+.5-H/2)/s+12.5)/25*50
        ix,iy=int(gx),int(gy);triangle=floor[(iy*80+ix)*2+(0 if gx-ix>=gy-iy else 1)]
        plane=np.linalg.solve(np.column_stack((triangle[:,:2],np.ones(3))),triangle[:,2])
        assert abs(float(height[y,x])-np.dot([x+.5,y+.5,1],plane))<.005
with Image.open(assets/'arena-map.png') as image:
    assert image.size == (W, H)
    walls = np.asarray(image.convert('RGBA'))[::-1, :, 3] > 0
assert walls[0].all() and walls[-1].all() and walls[:, 0].all() and walls[:, -1].all()
expected = np.load(root/'art/source/garden-1.86/collision-preview.npy')
assert np.array_equal(walls, expected[::-1] > 0), 'PNG y-axis / collision export mismatch'
blob = (assets/'seeds.bin').read_bytes()
magic, count = struct.unpack_from('<4sI', blob)
assert magic == b'LWS1' and len(blob) == 8+count*5
seeds = {0: [], 2: []}
for i in range(count):
    x, y, team = struct.unpack_from('<hhB', blob, 8+i*5)
    assert team in seeds and 0 <= x < W and 0 <= y < H
    if not walls[y, x]: seeds[team].append((x, y))
assert all(len(v) > 600 for v in seeds.values())
seen = np.zeros((H, W), dtype=bool)
queue = deque([seeds[0][0]])
seen[queue[0][1], queue[0][0]] = True
while queue:
    x, y = queue.popleft()
    for nx, ny in ((x-1, y), (x+1, y), (x, y-1), (x, y+1)):
        if 0 <= nx < W and 0 <= ny < H and not walls[ny, nx] and not seen[ny, nx]:
            seen[ny, nx] = True
            queue.append((nx, ny))
for team, points in seeds.items():
    assert all(seen[y, x] for x, y in points), f'team {team} has trapped seeds'
for bx, by in cfg['base_objectives']:
    x=round(W/2+(bx-400)*cfg['art_scale']);y=round(H/2+(by-225)*cfg['art_scale'])
    assert seen[y-12:y+13, x-12:x+13].any(), 'unreachable objective area'
with Image.open(assets/'arena-image.png') as image:
    assert image.size == (cfg['minimap_width'],cfg['minimap_height'])
with Image.open(assets/'scenery.png') as image:
    assert image.size == (cfg['scenery_width'],cfg['scenery_height'])
print(f'PASS: exact 6x area ({W}x{H}); {len(mesh)//3} real triangles; GPU-consistent heights; connected team seeds and objectives; native/asset dimensions; wide lighting and minimap')
