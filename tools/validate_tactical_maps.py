"""Focused 50-map geometry and route checks, not an alternate game renderer."""
from pathlib import Path
import json,math,xml.etree.ElementTree as ET
import cv2,numpy as np
from PIL import Image
from scipy import ndimage as ndi
from scipy.sparse import coo_matrix
from scipy.sparse.csgraph import dijkstra
R=Path(__file__).resolve().parents[1];OUT=R/'docs/game/tactical-1.59'

def main():
    results=[];route_failures=[];floor_samples=[];yy,xx=np.mgrid[0:480,0:800].astype('float32')
    plans=json.loads((R/'art/source/tactical-1.59/maps.json').read_text('utf8'))['maps']
    old=json.loads((OUT/'baseline-1.58.json').read_text('utf8'));hashes=set()
    import hashlib
    for i in range(50):
        CENTERS=plans[i]['deployment_centers']
        path=R/f'app/src/main/assets/maps/{i}-map.png';digest=hashlib.sha256(path.read_bytes()).hexdigest();hashes.add(digest)
        if i<46:assert digest!=old[path.relative_to(R).as_posix()],'old map unchanged'
        im=Image.open(path);assert im.size==(800,480)
        alpha=np.asarray(im)[:,:,3];assert set(np.unique(alpha))=={0,255}
        wall=alpha>3;floor=~wall;assert ndi.label(floor)[1]==1
        floor_samples.append(floor[::8,::8])
        art=Image.open(R/f'app/src/main/assets/maps/{i}-image.png');geo=Image.open(R/f'art/source/tactical-1.59/geometry/{i:02d}.png')
        assert art.size==geo.size==(1980,900)
        visible=cv2.remap(np.asarray(geo),(xx+.5)*1980/800-.5,(yy+.5)*900/480-.5,cv2.INTER_LINEAR)>127
        assert np.array_equal(visible,wall),'visible coverage/collision shifted'
        # All nine runtime candidates in each of six sectors have a 99x99
        # empty core. Actual 4500-dot sparse spiral capacity is tested in C++.
        for cx,cy in CENTERS:
            for dx in [-16,0,16]:
                for dy in [-12,0,12]:
                    assert floor[479-(cy+dy+49):480-(cy+dy-49),cx+dx-49:cx+dx+50].all(),(i,cx,cy,dx,dy)
        safe=ndi.binary_erosion(floor,iterations=6);labels,_=ndi.label(safe)
        core=[int(labels[479-y,x]) for x,y in CENTERS];assert min(core)>0 and len(set(core))==1,(i,'narrow gate')
        los=[]
        for a in range(6):
            for b in range(a+1,6):
                x0,y0=CENTERS[a];x1,y1=CENTERS[b];n=int(math.hypot(x1-x0,y1-y0))*2+1
                xs=np.linspace(x0,x1,n).astype(int);ys=np.linspace(y0,y1,n).astype(int)
                if not wall[479-ys,xs].any():los.append([a,b])
        assert not los,(i,'direct spawn sightlines',los)
        # Conservative four-pixel grid: a cell is navigable only when all its
        # underlying rule pixels are floor. Orthogonal physical-distance routes.
        walk=floor.reshape(120,4,200,4).all(axis=(1,3));ids=np.arange(24000).reshape(120,200);sources=[];targets=[];weights=[]
        for dy,dx,cost in [(0,1,4*2.75),(1,0,4*1000/480)]:
            a=walk[:120-dy,:200-dx];b=walk[dy:,dx:];good=a&b
            u=ids[:120-dy,:200-dx][good];v=ids[dy:,dx:][good]
            sources.extend([u,v]);targets.extend([v,u]);weights.extend([np.full(len(u),cost)]*2)
        graph=coo_matrix((np.concatenate(weights),(np.concatenate(sources),np.concatenate(targets))),shape=(24000,24000)).tocsr()
        nodes=[(479-y)//4*200+x//4 for x,y in CENTERS];dist=dijkstra(graph,indices=nodes)
        ratios=[]
        for a,b in [(0,1),(2,3),(4,5),(0,2),(2,4),(1,3),(3,5)]:
            x,y=CENTERS[a];u,v=CENTERS[b];straight=math.hypot((u-x)*2.75,(v-y)*1000/480)
            length=float(dist[a,nodes[b]]);assert math.isfinite(length)
            ratios.append(length/straight)
        if not(min(ratios)>1.03 and np.median(ratios)>1.12):route_failures.append(dict(id=i,ratios=ratios))
        results.append(dict(id=i,wall_fraction=float(wall.mean()),free_cells=int(floor.sum()),geometry_exact=True,
             spawn_clear_candidates=54,spawn_direct_sightlines=los,route_detour_ratios_orthogonal=ratios,
             gates_connected_after_6px_erosion=True))
        print(f'{i+1:02d} PASS no direct sightlines; detour min/median={min(ratios):.2f}/{np.median(ratios):.2f}',flush=True)
    assert len(hashes)==50
    similarities=[]
    for a in range(50):
        for b in range(a+1,50):
            u,v=floor_samples[a],floor_samples[b];score=float((u&v).sum()/(u|v).sum())
            similarities.append((score,a,b))
    closest=sorted(similarities,reverse=True)[:10]
    assert closest[0][0]<.94,('too similar geometries',closest)
    locales=[]
    for path in (R/'app/src/main/res').glob('values*/strings.xml'):
        array=ET.parse(path).getroot().find("string-array[@name='maps_array']")
        if array is not None:assert len(array)==51,path;locales.append(path.parent.name)
    report=dict(status='FAIL' if route_failures else 'PASS',maps=50,locales=locales,route_failures=route_failures,closest_floor_jaccard=closest,
        route_method='Conservative 4px grid, orthogonal physical-distance paths; not exact diagonal shortest path or performance',
        actualGame='separate native/Android evidence',rows=results)
    (OUT/('layout-diagnostic.json' if route_failures else 'layout-validation.json')).write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
    assert not route_failures,route_failures

if __name__=='__main__':main()
