"""Blender export of the new mainland. Never changes the saved art project.
Static art uses camera-projected final color; real ground/rocks/bridges preserve
depth. Leaf detail is baked and its canopies block traversal. No authored armies.
"""
import bpy, numpy as np, json, struct, math, sys
from pathlib import Path
R=Path(__file__).resolve().parents[1];A=R/'art/source/garden-1.88';O=R/'app/src/main/assets/garden3d';O.mkdir(exist_ok=True)
bpy.ops.wm.open_mainfile(filepath=str(A/'十倍新场景_溪谷原野.blend'),load_ui=False)
s=bpy.context.scene;spec=json.loads((A/'map_spec.json').read_text())
W,H,S=3264,2048,800/28.8;cx,cy=spec['camera_center'];VW,VH=192*S,96*S
def xy(v):return np.array([v.x*S+W/2-cx*S,v.y*S+H/2-cy*S,v.z*S])
def uv(v):return np.array([.5+(v[0]-W/2)/VW,.5-(v[1]-H/2)/VH])
def hull(points):
    p=sorted(set(points));a=[];b=[]
    def cross(a,b,c):return (b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0])
    for v in p:
        while len(a)>1 and cross(a[-2],a[-1],v)<=0:a.pop()
        a.append(v)
    for v in p[::-1]:
        while len(b)>1 and cross(b[-2],b[-1],v)<=0:b.pop()
        b.append(v)
    return a[:-1]+b[:-1]
for c in s.collection.children:
    if c.name[:2] in ['04','05']:c.hide_render=True
for o in s.objects:
    if o.name.startswith(('Seed station','Station ')):o.hide_render=True
# Extend only outer ocean vertices. Landforms and all object scales are unchanged.
o=bpy.data.objects['Coastal waters | soft depth gradient']
for v in o.data.vertices:
    if abs(v.co.x-cx)>spec['camera_width']*.50:v.co.x=cx+math.copysign(105,v.co.x-cx)
    if abs(v.co.y-cy)>spec['camera_width']*9/16*.50:v.co.y=cy+math.copysign(53,v.co.y-cy)
dg=bpy.context.evaluated_depsgraph_get();tris=[];solids=[];bridges=[];bridge_top=[];trees=[];sites=[];ground=None
for o in sorted(s.objects,key=lambda o:o.name):
    if o.name.startswith('Seed station') and o.name.endswith('| foot'):sites.append(list(xy(o.location)[:2]))
    if o.name.startswith('Grove trunk'):
        v=xy(o.location);trees.append([float(v[0]),float(v[1]),float(o.dimensions.z/1.4*1.5*S)])
    if o.type!='MESH' or o.hide_render:continue
    group={c.name[:2] for c in o.users_collection}
    keep=bool(group&{'01','02','06','08','09'})
    if not keep:continue
    ev=o.evaluated_get(dg);me=ev.to_mesh();me.calc_loop_triangles()
    points=np.asarray([xy(o.matrix_world@v.co) for v in me.vertices]);ids=np.array([list(t.vertices) for t in me.loop_triangles]);tt=points[ids]
    if o.name.startswith('Mainland |'):
        ground=tt.copy()
    elif o.name.startswith('Coastal waters'):
        # Only one background quad is required at the locked vertical angle.
        points=np.array([[W/2-VW/2,H/2-VH/2,-.31*S],[W/2+VW/2,H/2-VH/2,-.31*S],[W/2+VW/2,H/2+VH/2,-.31*S],[W/2-VW/2,H/2+VH/2,-.31*S]])
        tt=points[[[0,1,2],[0,2,3]]]
    elif o.name.startswith('Winding river'):
        # Top projection is identical with a simple plane; ground occludes it.
        ev.to_mesh_clear();continue
    elif '02' in group:solids.append(hull([tuple(p[:2]) for p in points]))
    elif o.name.startswith('Crossing'):
        poly=hull([tuple(p[:2]) for p in points])
        if 'inset stone' in o.name:bridges.append(poly);bridge_top.append([poly,float(points[:,2].max())])
        else:solids.append(poly)
    elif o.name.startswith(('Broken garden wall','Remnant square plinth','Broken fluted pier')):
        solids.append(hull([tuple(p[:2]) for p in points]))
    tris.extend(tt);ev.to_mesh_clear()
assert ground is not None
# Water surface clipped to the real river depression on the mainland mesh.
river=ground[(ground[:,:,2].min(axis=1)<-.08*S)].copy();river[:,:,2]=-.10*S;tris.extend(river)
np.savez_compressed(A/'surface.npz',ground=ground)
(A/'collision-source.json').write_text(json.dumps({'solids':solids,'bridges':bridges,'bridge_top':bridge_top,'trees':trees,'sites':sites}),encoding='utf-8')

# Clip triangles at texture tile seams; no seams stretch/clamp a neighboring tile.
tiles=[[] for _ in range(8)]
def clip(poly,axis,bound,greater):
    out=[]
    for a,b in zip(poly,poly[1:]+poly[:1]):
        da=a[axis]-bound;db=b[axis]-bound;ia=da>=-1e-6 if greater else da<=1e-6;ib=db>=-1e-6 if greater else db<=1e-6
        if ia:out.append(a)
        if ia!=ib:out.append(a+(b-a)*(da/(da-db)))
    return out
for tri in tris:
    vv=[np.r_[p,uv(p)] for p in tri]
    lo=np.floor(np.min([v[3:]*[4,2] for v in vv],axis=0)).astype(int);hi=np.floor(np.max([v[3:]*[4,2] for v in vv],axis=0)).astype(int)
    for ty in range(max(0,lo[1]),min(1,hi[1])+1):
        for tx in range(max(0,lo[0]),min(3,hi[0])+1):
            p=vv
            for axis,bound,greater in [(3,tx/4,True),(3,(tx+1)/4,False),(4,ty/2,True),(4,(ty+1)/2,False)]:
                if not p:break
                p=clip(p,axis,bound,greater)
            for k in range(1,len(p)-1):
                for v in [p[0],p[k],p[k+1]]:tiles[ty*4+tx].append([*v[:3],v[3]*4-tx,v[4]*2-ty])
for i,vertices in enumerate(tiles):
    with (O/f'tile{i}.bin').open('wb') as f:f.write(struct.pack('<4sI',b'LWG1',len(vertices)));f.write(np.asarray(vertices,dtype='<f4').tobytes())
config={'width':W,'height':H,'scale':S,'center':[cx,cy],'visual_width':VW,'visual_height':VH,'tiles':[4,2],'scenery':[8192,4096],'area_multiplier':10,'source':'art/source/garden-1.88/十倍新场景_溪谷原野.blend','particle_birth':'random compact clusters; no artwork seeds','triangles':sum(len(t)//3 for t in tiles)}
(A/'world.json').write_text(json.dumps(config,ensure_ascii=False,indent=2),encoding='utf-8')
s.camera.location.x=cx;s.camera.location.y=cy;s.camera.data.ortho_scale=192
s.render.resolution_x=8192;s.render.resolution_y=4096;s.render.resolution_percentage=100
s.cycles.samples=48;s.cycles.use_denoising=True
prefs=bpy.context.preferences.addons['cycles'].preferences;prefs.compute_device_type='CUDA';prefs.refresh_devices()
for d in prefs.devices:d.use=d.type=='CUDA'
s.cycles.device='GPU';s.render.filepath=str(A/'scenery.png')
print('EXPORTED',json.dumps(config),flush=True)
if '--geometry-only' not in sys.argv:bpy.ops.render.render(write_still=True);print('RENDER_DONE',flush=True)
