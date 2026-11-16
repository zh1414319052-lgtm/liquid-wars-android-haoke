"""Export real meshes/normals/albedo to the Android programmable renderer.
Run in an isolated Blender background process. Never saves the 1.88 original.
"""
import bpy, numpy as np, math, json, struct
from pathlib import Path
from mathutils import Vector, Euler
R=Path(__file__).resolve().parents[1];A=R/'art/source/garden-1.89';O=R/'app/src/main/assets/garden3d/live'
A.mkdir(exist_ok=True);O.mkdir(exist_ok=True)
bpy.ops.wm.open_mainfile(filepath=str(R/'art/source/garden-1.88/十倍新场景_溪谷原野.blend'),load_ui=False)
bpy.context.preferences.filepaths.save_version=0
config=json.loads((R/'art/source/garden-1.88/world.json').read_text());W,H=config['width'],config['height'];S=config['scale'];cx,cy=config['center']
def xyz(v):return (v[0]*S+W/2-cx*S,v[1]*S+H/2-cy*S,v[2]*S)
def mat(m):
    p=next((n for n in m.node_tree.nodes if n.type=='BSDF_PRINCIPLED'),None) if m and m.use_nodes else None
    return (list(p.inputs['Base Color'].default_value[:3]),float(p.inputs['Roughness'].default_value)) if p else ([.35,.42,.28],.8)
chunks={};counts={};exclude=('Seed station','Station ','Coastal waters','Winding river')
def emit(verts,tag,wind):
    a=np.asarray(verts,dtype=np.float32).reshape(-1,3,11)
    faces=np.cross(a[:,1,:3]-a[:,0,:3],a[:,2,:3]-a[:,0,:3]);length=np.linalg.norm(faces,axis=1)
    valid=length>1e-7;a=a[valid];faces=faces[valid]/length[valid,None]
    if not len(a):return
    normals=np.linalg.norm(a[:,:,3:6],axis=2);missing=normals<1e-6
    a[:,:,3:6][missing]=np.repeat(faces[:,None,:],3,axis=1)[missing]
    a[:,:,3:6]/=np.linalg.norm(a[:,:,3:6],axis=2)[:,:,None]
    centers=a[:,:,:2].mean(axis=1)
    keys=np.floor(centers/256).astype(int)
    for tx,ty in np.unique(keys,axis=0):
        chunk=chunks.setdefault((int(tx),int(ty),wind),[])
        chunk.append(a[(keys[:,0]==tx)&(keys[:,1]==ty)].reshape(-1,11))
    counts[tag]=counts.get(tag,0)+len(a)
for col in bpy.context.scene.collection.children:
    if col.name[:2] in ('04','05'):col.hide_render=True
for ob in bpy.data.objects:
    if ob.name.startswith(('Seed station','Station ','Coastal waters')):ob.hide_render=True
    if ob.name.startswith(('Fern','Sage rosette')):
        for mod in ob.modifiers:
            if mod.type=='SOLIDIFY':mod.show_viewport=False;mod.show_render=False
        dec=ob.modifiers.new('Mobile leaf geometry','DECIMATE');dec.ratio=.42
dg=bpy.context.evaluated_depsgraph_get()
# Leaf instances retain their authored positions, rotation and scale, with a
# compact closed leaf mesh rather than 80-triangle icospheres per instance.
leaf_v=np.array([[0,0,.25],[0,0,-.16],[-1,0,0],[0,-.8,0],[1,0,0],[0,.8,0]])
leaf_f=[(0,2,3),(0,3,4),(0,4,5),(0,5,2),(1,3,2),(1,4,3),(1,5,4),(1,2,5)]
for ob in sorted(bpy.context.scene.objects,key=lambda o:o.name):
    group=ob.users_collection[0].name[:2] if ob.users_collection else ''
    if ob.type!='MESH' or group in ('04','05','07') or ob.name.startswith(exclude):continue
    if ob.name.startswith('Tree crowns'):
        m=next(n for n in ob.modifiers[0].node_group.nodes if n.bl_idname=='GeometryNodeSetMaterial').inputs['Material'].default_value
        color,rough=mat(m);verts=[]
        for i,p in enumerate(ob.data.vertices):
            scale=np.array(ob.data.attributes['unit_scale'].data[i].vector);rot=Euler(ob.data.attributes['unit_rotation'].data[i].vector).to_matrix()
            vv=[ob.matrix_world@(p.co+rot@Vector(v*scale)) for v in leaf_v]
            for f in leaf_f:
                for k in f:
                    normal=(rot@Vector(leaf_v[k]/np.maximum(scale,[.001,.001,.001]))).normalized()
                    verts.append((*xyz(vv[k]),*normal,*color,1.0,rough))
        emit(verts,'canopy',1);continue
    ev=ob.evaluated_get(dg);me=ev.to_mesh();me.calc_loop_triangles()
    if not me.loop_triangles:ev.to_mesh_clear();continue
    matrix=ob.matrix_world;nm=matrix.to_3x3().inverted().transposed()
    attrib=me.color_attributes.get('tint');colors=[mat(m) for m in me.materials]
    wind=group=='03' and not ob.name.startswith('Grove trunk')
    verts=[];base=min(v.co.z for v in me.vertices);height=max(v.co.z for v in me.vertices)-base
    for tri in me.loop_triangles:
        color,rough=colors[min(tri.material_index,len(colors)-1)] if colors else ([.35,.40,.3],.85)
        for index,loop in zip(tri.vertices,tri.loops):
            v=me.vertices[index];pos=matrix@v.co
            normal=(nm@(v.normal if me.polygons[tri.polygon_index].use_smooth else tri.normal)).normalized()
            c=attrib.data[index if attrib.domain=='POINT' else loop].color[:3] if attrib else color
            weight=min(1,max(0,(v.co.z-base)/max(.1,height))) if wind else 0
            verts.append((*xyz(pos),*normal,*c,weight,rough))
    emit(verts,'foliage' if wind else ('ground' if ob.name.startswith('Mainland |') else 'solid'),int(wind))
    if ob.name.startswith('Mainland |'):
        ground=np.asarray(verts).reshape(-1,3,11)
        water=ground[ground[:,:,2].min(axis=1)<-.08*S].copy()
        water[:,:,2]=-.10*S;water[:,:,3:6]=[0,0,1];water[:,:,6:9]=[.17,.38,.36];water[:,:,9]=0;water[:,:,10]=.25
        emit(water.reshape(-1,11),'river',2)
    ev.to_mesh_clear()
manifest=[]
with (O/'meshes.bin').open('wb') as f:
    f.write(struct.pack('<4sI',b'LWD2',len(chunks)))
    for (x,y,wind),parts in sorted(chunks.items()):
        a=np.concatenate(parts).astype('<f4');lo=a[:,:3].min(axis=0);hi=a[:,:3].max(axis=0)
        f.write(struct.pack('<II6f',len(a),wind,*lo,*hi));f.write(a.tobytes())
        manifest.append({'tile':[x,y],'wind':wind,'vertices':len(a),'bounds':[*map(float,lo),*map(float,hi)]})
# River geometry spans the depressed actual ground. The ground and bridges depth
# test it; the shader follows the same analytical winding centerline as Blender.
spec=json.loads((R/'art/source/garden-1.88/map_spec.json').read_text())
world={'world':config,'design_scale':spec['design_coordinate_scale'],'design_offset':spec['design_coordinate_offset'],'counts':counts,'chunks':manifest,'format':'LWD2: xyz normal linear-albedo wind roughness; 11 float32 per vertex','renderer':'GLES2, dynamic shader lighting/water/wind; no full-scene color plate'}
(A/'scene.json').write_text(json.dumps(world,ensure_ascii=False,indent=2),encoding='utf-8')
(O/'river.bin').write_bytes(struct.pack('<4s6f',b'LWR2',S,cx,cy,spec['design_coordinate_scale'],*spec['design_coordinate_offset']))
bpy.context.scene['runtime_export']='GLES2 LWD2 geometry/normals/albedo; live shaders in app/assets/garden3d/live'
bpy.ops.wm.save_as_mainfile(filepath=str(A/'dynamic-mainland.blend'))
print('EXPORTED_REAL_MESHES',json.dumps(counts),'chunks',len(chunks),flush=True)
