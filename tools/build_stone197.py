"""Original reproducible stone/earth sample assets. No game state or network writes.
Uses the existing authored route graph and solid polygons; does not change navigation.
Run with Python + numpy/scipy/Pillow. Output textures are editable PNGs.
"""
from pathlib import Path
import ast, json, hashlib
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import gaussian_filter, zoom, distance_transform_edt, map_coordinates
from scipy.spatial import cKDTree

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'art/source/garden-1.88'
OUT=ROOT/'app/godot/assets/stone197'
ART=ROOT/'art/source/stone-1.97'
OUT.mkdir(parents=True,exist_ok=True);ART.mkdir(parents=True,exist_ok=True)
rng=np.random.default_rng(197)

def smooth(a,b,v):
    t=np.clip((v-a)/(b-a),0,1);return t*t*(3-2*t)

def noise(shape,grid,seed):
    r=np.random.default_rng(seed)
    raw=r.random((grid,grid)).astype(np.float32)
    yy,xx=np.mgrid[0:shape[0],0:shape[1]].astype(np.float32)
    q=map_coordinates(raw,[yy/shape[0]*grid,xx/shape[1]*grid],order=3,mode='grid-wrap')
    return np.clip(q,0,1)

def save(name,a):
    Image.fromarray(np.uint8(np.clip(a,0,1)*255+.5)).save(OUT/name)

def spline(points,steps=18):
    p=[points[0]]+points+[points[-1]];out=[]
    for i in range(1,len(p)-2):
        a,b,c,d=[np.array(x) for x in p[i-1:i+3]]
        for t in np.linspace(0,1,steps,endpoint=False):
            out.append(.5*((2*b)+(-a+c)*t+(2*a-5*b+4*c-d)*t*t+(-a+3*b-3*c+d)*t*t*t))
    return out

def generate():
    # Packed map data: red road; green rock-root vegetation; blue river bank.
    w,h=1632,1024
    yy,xx=np.mgrid[0:h,0:w].astype(np.float32);xx=xx*2+1;yy=yy*2+1
    world=json.loads((SRC/'world.json').read_text('utf-8'))
    spec=json.loads((SRC/'map_spec.json').read_text('utf-8'))
    scale=world['scale'];cx,cy=world['center'];ds=spec['design_coordinate_scale'];ox,oy=spec['design_coordinate_offset']
    code=ast.parse((SRC/'build_scene.py').read_text('utf-8'))
    roads=next(ast.literal_eval(n.value) for n in code.body if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='road_raw' for t in n.targets))
    road_d=np.full((h,w),1e4,np.float32)
    for path in roads:
        pts=[((x*ds+ox)*scale+1632-cx*scale,(y*ds+oy)*scale+1024-cy*scale,r*scale*.58) for x,y,r in path]
        for x,y,rad in spline(pts)[::2]:
            road_d=np.minimum(road_d,np.hypot(xx-x,yy-y)-rad)
    macro=noise((h,w),36,111)
    road=1-smooth(-3,19,road_d+(macro-.5)*11)
    solid_img=Image.new('L',(w,h));pen=ImageDraw.Draw(solid_img)
    collision=json.loads((SRC/'collision-source.json').read_text('utf-8'))
    for polygon in collision['solids']:
        pen.polygon([(x/2,y/2) for x,y in polygon],fill=255)
    near=distance_transform_edt(np.asarray(solid_img)==0)*2
    roots=(1-smooth(2,30,near))*smooth(.28,.76,noise((h,w),79,302))
    import struct
    # Bank based on exported river geometry, with bridge continuity; nearest X/Z.
    mesh=(ROOT/'app/src/main/assets/garden3d/live/meshes.bin').read_bytes()
    chunks=struct.unpack_from('<I',mesh,4)[0];offset=8;water=[]
    for _ in range(chunks):
        count,kind,*bounds=struct.unpack_from('<II6f',mesh,offset);offset+=32
        data=np.frombuffer(mesh,np.float32,count=count*11,offset=offset).reshape(count,11);offset+=count*44
        if kind==2:water.append(data[:,:2].copy())
    q=np.concatenate(water)
    # Rasterize actual water triangles (not only vertices) before shore distance.
    wi=Image.new('L',(w,h));wd=ImageDraw.Draw(wi)
    for tri in q.reshape(-1,3,2):wd.polygon([(float(x)/2,float(y)/2) for x,y in tri],fill=255)
    bank=(1-smooth(0,24,distance_transform_edt(np.asarray(wi)==0)*2))
    save('terrain-zones.png',np.stack([road,roots,bank],axis=-1))

    # Periodic, warped limestone plates: broad mineral planes and fine worn joints.
    n=1024;y,x=np.mgrid[0:n,0:n].astype(np.float32);x/=n;y/=n
    a=noise((n,n),7,18);b=noise((n,n),18,29);c=noise((n,n),57,40)
    uv=np.stack([(x+(a-.5)*.016)%1,(y+(noise((n,n),9,21)-.5)*.016)%1],-1)
    points=rng.random((85,2));points[:,1]*=.86;points[:,1]%=1
    tree=cKDTree(points,boxsize=1)
    dist,indices=tree.query(uv.reshape(-1,2),k=2)
    # Actual distance to the nearest bisector. F2-F1 alone creates broad dark
    # wedges for close seed pairs and makes stone look like a wrinkled quilt.
    between=(points[indices[:,1]]-points[indices[:,0]]+.5)%1-.5
    seam=((dist[:,1]**2-dist[:,0]**2)/(2*np.maximum(.001,np.linalg.norm(between,axis=1)))).reshape(n,n)*256
    cell=indices[:,0].reshape(n,n)
    palette=rng.uniform(-.035,.035,85)[cell]
    rock=np.array([.76,.755,.705])[None,None,:]+palette[:,:,None]
    rock=rock+((a-.5)*.055+(b-.5)*.022+(c-.5)*.010)[:,:,None]
    joint=1-smooth(.18,.95,seam)
    # A few joints fade into the body, avoiding a uniform paving-tile grid.
    joint*=smooth(.29,.68,a)*(.4+.6*smooth(.18,.70,b))
    rock=rock*(1-joint[:,:,None]*.23)
    bevel=(smooth(.65,1.2,seam)-smooth(1.2,1.8,seam))*.006
    rock+=bevel[:,:,None]
    height=.53-joint*.09+(a-.5)*.025
    save('limestone.png',np.dstack([rock,height]))
    gy,gx=np.gradient(height);normal=np.stack([-gx*8,gy*8,np.ones_like(gx)],-1)
    normal/=np.linalg.norm(normal,axis=-1)[:,:,None]
    save('limestone-normal.png',normal*.5+.5)
    earth=np.array([.55,.49,.455])+((a-.5)*.015+(b-.5)*.009+(c-.5)*.003)[:,:,None]
    # Sparse quiet flecks, no photographic white grass/gravel noise.
    earth+=(smooth(.72,.9,c)*.012)[:,:,None]
    save('earth.png',earth)
    save('material-fields.png',np.stack([a,b,c],axis=-1))
    manifest={'license':'Original project assets; GPL-3.0-or-later with project',
      'inspiration':'Cartoon Terrain Studio right-bottom worlds.jpg; no screenshot used as texture',
      'map_channels':{'r':'existing authored roads','g':'existing solid roots vegetation','b':'actual exported river bank'},
      'geometry_changed':False,'world_dimensions':[3264,2048],'map_resolution':[w,h],
      'tile_world_size':256,'source':'tools/build_stone197.py',
      'files':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in OUT.glob('*.png')}}
    (ART/'assets.json').write_text(json.dumps(manifest,indent=2),'utf-8')
    print(json.dumps(manifest,indent=2))

if __name__=='__main__':generate()
