"""Blender-only fixed-top-view game asset export. Source .blend is never saved.
Run Blender --background --factory-startup --python tools/export_garden186.py.
Exports real geometry + camera-projected final-color light texture, collision
from the same boulders, and particle seeds. Lighting must not be applied twice.
"""
import bpy, struct, json, math, sys
import numpy as np
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'app/src/main/assets/garden3d';OUT.mkdir(parents=True,exist_ok=True)
ART=ROOT/'art/source/garden-1.86'
bpy.ops.wm.open_mainfile(filepath=str(ART/'source.blend'),load_ui=False)
s=bpy.context.scene
config=json.loads((ART/'world.json').read_text('utf-8'))
W,H=config['width'],config['height']
assert W*H==800*450*config['area_multiplier']
S=800/28.8*config['art_scale']
# A real 40x25 environment apron covers wide phones without stretching edge texels.
ground=bpy.data.objects['Sage meadow fading into clear stone-dust lanes']
for v in ground.data.vertices:v.co.x*=1.25;v.co.y*=1.25
s.camera.data.ortho_scale=40
def uv(x,y):return (.5+(x-W/2)/(40*S),.5-(y-H/2)/(25*S))
def xy(v):return (v.x*S+W/2,v.y*S+H/2,v.z*S)
env=set()
for c in s.collection.children:
    if c.name[:2] in ['01','02','03']:
        env.update(o.name for o in c.all_objects)
    elif c.name[:2] in ['04','05','06']:
        c.hide_render=True

seeds=[]
for name,t in [('Jade | branching streamlines',0),('Coral | counterflow and retreat',2)]:
    o=bpy.data.objects[name]
    for v in o.data.vertices:
        x,y,z=xy(o.matrix_world@v.co);seeds.append((round(x),round(y),t))
with (OUT/'seeds.bin').open('wb') as f:
    f.write(struct.pack('<4sI',b'LWS1',len(seeds)))
    for x,y,t in seeds:f.write(struct.pack('<hhB',x,y,t))

def hull(points):
    p=sorted(set(points))
    def cross(a,b,c):return (b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0])
    a=[]
    for v in p:
        while len(a)>1 and cross(a[-2],a[-1],v)<=0:a.pop()
        a.append(v)
    b=[]
    for v in reversed(p):
        while len(b)>1 and cross(b[-2],b[-1],v)<=0:b.pop()
        b.append(v)
    return a[:-1]+b[:-1]

# Realize procedural small meshes only in this independent export process.
for name in env:
    o=bpy.data.objects[name]
    for m in o.modifiers:
        if m.type=='NODES':
            m.node_group=m.node_group.copy();g=m.node_group
            out=next(n for n in g.nodes if n.bl_idname=='NodeGroupOutput')
            if out.inputs['Geometry'].links:
                source=out.inputs['Geometry'].links[0].from_socket
                realize=g.nodes.new('GeometryNodeRealizeInstances')
                g.links.new(source,realize.inputs['Geometry']);g.links.new(realize.outputs['Geometry'],out.inputs['Geometry'])
dg=bpy.context.evaluated_depsgraph_get()
vertices=[];rock_hulls=[];objects=[]
for name in sorted(env):
    o=bpy.data.objects[name]
    if o.type!='MESH':continue
    ev=o.evaluated_get(dg);me=ev.to_mesh();me.calc_loop_triangles()
    matrix=o.matrix_world
    pos=[xy(matrix@v.co) for v in me.vertices]
    if '02' in [c.name[:2] for c in o.users_collection]:rock_hulls.append(hull([(v[0],v[1]) for v in pos]))
    count=0
    if name.startswith('Sage meadow'):
        # Original regular 321x201 ground; retain shape at 81x51, UVs carry detail.
        nx,ny=321,201
        for j in range(0,ny-1,4):
            for i in range(0,nx-1,4):
                a=j*nx+i;b=a+4;c=(j+4)*nx+i;d=c+4
                for ids in [(a,b,d),(a,d,c)]:
                    for idx in ids:
                        x,y,z=pos[idx];u,v=uv(x,y);vertices.extend((x,y,z,u,v));count+=1
    else:
        for tri in me.loop_triangles:
            for idx in tri.vertices:
                x,y,z=pos[idx];u,v=uv(x,y);vertices.extend((x,y,z,u,v));count+=1
    objects.append({'name':name,'triangles':count//3});ev.to_mesh_clear()
with (OUT/'scene.bin').open('wb') as f:
    f.write(struct.pack('<4sI',b'LWG1',len(vertices)//5));f.write(np.asarray(vertices,dtype='<f4').tobytes())

# Pixel centers correspond to native simulation cells. PNG rows start at top.
Y,X=np.mgrid[0:H,0:W];Y=H-1-Y
mask=(X<3)|(X>=W-3)|(Y<3)|(Y>=H-3)
for poly in rock_hulls:
    inside=np.ones((H,W),dtype=bool)
    for a,b in zip(poly,poly[1:]+poly[:1]):
        inside&=((b[0]-a[0])*(Y+.5-a[1])-(b[1]-a[1])*(X+.5-a[0]))>=-1e-5
    mask|=inside
rgba=np.ones((H,W,4),dtype=np.float32);rgba[:,:,3]=mask.astype(np.float32)
im=bpy.data.images.new('garden collision',width=W,height=H,alpha=True)
# Blender Image pixels start at the bottom; resulting PNG starts at the top.
im.pixels.foreach_set(rgba[::-1].ravel());im.filepath_raw=str(OUT/'arena-map.png');im.file_format='PNG';im.save()
np.save(ART/'collision-preview.npy',mask)

# Sample the exact rendered 81x51 triangular ground, at particle cell centers.
# Bilinear sampling the high-detail source would submerge small particles after scaling.
ground=bpy.data.objects['Sage meadow fading into clear stone-dust lanes']
z=np.asarray([v.co.z*S for v in ground.data.vertices]).reshape(201,321)[::4,::4]
gx=((np.arange(W)+.5-W/2)/S+20)/40*80
gy=((np.arange(H)+.5-H/2)/S+12.5)/25*50
ix=np.clip(gx.astype(int),0,79);iy=np.clip(gy.astype(int),0,49)
fx=np.clip(gx-ix,0,1)[None,:];fy=np.clip(gy-iy,0,1)[:,None]
a=z[iy[:,None],ix];b=z[iy[:,None],ix+1];c=z[iy[:,None]+1,ix];d=z[iy[:,None]+1,ix+1]
height=np.where(fx>=fy,a*(1-fx)+b*(fx-fy)+d*fy,a*(1-fy)+c*(fy-fx)+d*fx).astype('<f4')
with (OUT/'height.bin').open('wb') as f:f.write(struct.pack('<4sII',b'LWH1',W,H));f.write(height.tobytes())

meta={'source':'art/source/garden-1.86/source.blend','world':[W,H],'area_multiplier':config['area_multiplier'],'art_scale':config['art_scale'],'view':'vertical orthographic',
      'geometry_vertices':len(vertices)//5,'triangles':len(vertices)//15,'objects':objects,
      'texture':'final color with baked static light; runtime unlit on actual geometry',
      'collision':'same exported boulder hulls; decoration passable','spawn_seeds':len(seeds),
      'dynamic':'game simulation owns particles and capture sites; these are excluded from static lighting image'}
(ART/'export.json').write_text(json.dumps(meta,ensure_ascii=False,indent=2),encoding='utf-8')
s.render.resolution_x=config['scenery_width'];s.render.resolution_y=config['scenery_height'];s.render.resolution_percentage=100
s.cycles.samples=96;s.cycles.use_denoising=True
prefs=bpy.context.preferences.addons['cycles'].preferences
try:
    prefs.compute_device_type='CUDA';prefs.refresh_devices()
    for d in prefs.devices:d.use=d.type=='CUDA'
    s.cycles.device='GPU'
except Exception:s.cycles.device='CPU'
s.render.filepath=str(OUT/'scenery.png')
print('EXPORT',json.dumps(meta,ensure_ascii=False),flush=True)
if '--geometry-only' not in sys.argv:
    bpy.ops.render.render(write_still=True)
    print('EXPORT_RENDER_DONE',s.render.filepath,flush=True)
