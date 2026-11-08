"""Offline asset/layout checks, not a game or device test. GPL-3.0-or-later."""
from pathlib import Path
import json,hashlib,math
import cv2,numpy as np
from PIL import Image
from scipy import ndimage as ndi
ROOT=Path(__file__).resolve().parents[1]
CHECK=ROOT/'docs/game/fullscreen-1.47'
SPAWNS=[(20,50),(20,430),(400,50),(400,430),(780,50),(780,430)]

def placement(wall,n):
    used=set();max_steps=0
    for sx,sy in SPAWNS:
        placed=0
        for c in range(80000):
            # Exact source formula's order and truncation; no game RNG or simulation runs.
            angle=c/50.;x=sx+int(.5*angle*math.cos(angle));y=sy+int(.5*angle*math.sin(angle))
            if not (0<=x<800 and 0<=y<480) or wall[479-y,x] or (x,y) in used:continue
            used.add((x,y));placed+=1
            if placed==n:max_steps=max(max_steps,c);break
        else:raise AssertionError(f'Could not place {n} at {sx},{sy}')
    assert len(used)==6*n
    return max_steps

def main():
    yy,xx=np.mgrid[0:480,0:800].astype('float32');rows=[]
    for i in range(46):
        art=Image.open(ROOT/f'app/src/main/assets/maps/{i}-image.png');mask=Image.open(ROOT/f'app/src/main/assets/maps/{i}-map.png');geometry=Image.open(ROOT/f'art/source/fullscreen-1.47/geometry/{i:02d}.png')
        assert art.size==(1980,900) and geometry.size==art.size and mask.size==(800,480)
        alpha=np.asarray(mask)[:,:,3];assert set(np.unique(alpha)).issubset({0,255});wall=alpha>3
        visible=cv2.remap(np.asarray(geometry),(xx+.5)*1980/800-.5,(yy+.5)*900/480-.5,cv2.INTER_LINEAR)>127
        union=(visible|wall).sum();iou=float((visible&wall).sum()/union) if union else 1.;assert iou>=.97,(i,iou)
        labels,count=ndi.label(~wall);assert count==1 and (~wall).sum()>70000
        safe=ndi.binary_erosion(~wall,iterations=4);safe_labels,safe_count=ndi.label(safe)
        spawn_labels=[int(safe_labels[479-y,x]) for x,y in SPAWNS];assert min(spawn_labels)>0 and len(set(spawn_labels))==1,(i,spawn_labels)
        areas=np.bincount(safe_labels.ravel())[1:];assert sum(int(n)>=100 for n in areas)==1,(i,'large pocket behind narrow passage')
        c={str(n):placement(wall,n) for n in [100,400,4500]}
        row=dict(id=i,art_size=list(art.size),rule_size=list(mask.size),geometry_iou=iou,free_cells=int((~wall).sum()),floor_components=count,spawn_core_connected=True,eroded_floor_components=safe_count,placed_counts=[600,2400,27000],spiral_last_index=c,image_sha256=hashlib.sha256((ROOT/f'app/src/main/assets/maps/{i}-image.png').read_bytes()).hexdigest(),mask_sha256=hashlib.sha256((ROOT/f'app/src/main/assets/maps/{i}-map.png').read_bytes()).hexdigest());rows.append(row)
        print(f'{i:02d} PASS geometry={iou:.5f}, free={(~wall).mean():.1%}, 27000 placed',flush=True)
    assert len({r['image_sha256'] for r in rows})==46
    assert len({r['mask_sha256'] for r in rows})==46
    result=dict(status='PASS',maps=46,geometry_threshold=.97,placement_method='Offline same-order spiral formula; no JNI/device run',runtime='NOT_TESTED',balance='NOT_TESTED',rows=rows)
    (CHECK/'layout-validation.json').write_text(json.dumps(result,indent=2),encoding='utf8')
if __name__=='__main__':main()
