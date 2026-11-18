"""Blender background-only: replace sharp foliage with coherent rounded crowns.
Input: original 1.88 editable mainland, unchanged collision circles.
Output: editable vegetation scene and standard GLB, native grid units Z-up.
Never opens or mutates the user's interactive Blender session.
"""
import bpy,math,json,random
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];ART=ROOT/'art/source/grove-1.98';OUT=ROOT/'app/godot/assets/grove198'
assert bpy.app.background and not bpy.data.filepath,'Use --background --factory-startup'
bpy.context.preferences.filepaths.save_version=0
bpy.ops.wm.open_mainfile(filepath=str(ROOT/'art/source/garden-1.88/十倍新场景_溪谷原野.blend'),load_ui=False)
world=json.loads((ROOT/'art/source/garden-1.88/world.json').read_text('utf-8'))
scale=world['scale'];cx,cy=world['center']
def convert(v):return Vector((v.x*scale+1632-cx*scale,v.y*scale+1024-cy*scale,v.z*scale))
trees=[];shrubs=[]
for o in bpy.context.scene.objects:
    if o.name.startswith('Grove trunk'):
        p=convert(o.location);p.z-=o.dimensions.z*scale*.5
        trees.append((o.name,list(p),o.dimensions.z/1.4*1.5*scale))
    elif o.type=='MESH' and o.name.startswith(('Fern','Sage rosette')):
        vs=[convert(o.matrix_world@v.co) for v in o.data.vertices]
        if not vs:continue
        lo=Vector(tuple(min(v[k] for v in vs) for k in range(3)));hi=Vector(tuple(max(v[k] for v in vs) for k in range(3)))
        shrubs.append((o.name,[(lo.x+hi.x)*.5,(lo.y+hi.y)*.5,lo.z],max(hi.x-lo.x,hi.y-lo.y)*.5))
# New artwork, not a backup of the mainland. Only this isolated process is reset.
for o in list(bpy.data.objects):bpy.data.objects.remove(o,do_unlink=True)
scene=bpy.context.scene;scene.name='Grove198 | soft broadleaf runtime replacement'
scene.world=None
chunks={}
def emit(name,verts,faces,colors):
    # Spatial batches preserve culling; vertices/normals stay real 3D.
    mx=sum(v[0] for v in verts)/len(verts);my=sum(v[1] for v in verts)/len(verts)
    key=(int(mx//256),int(my//256));data=chunks.setdefault(key,([],[],[],[]))
    offset=len(data[0]);data[0].extend(verts);data[1].extend([tuple(i+offset for i in f) for f in faces]);data[2].extend(colors)
    span=max(max(v[0] for v in verts)-min(v[0] for v in verts),max(v[1] for v in verts)-min(v[1] for v in verts))
    angle=(mx*.19+my*.31)%math.tau
    for x,y,z in verts:
        dx=(x-mx)/span;dy=(y-my)/span
        data[3].append((.5+(dx*math.cos(angle)-dy*math.sin(angle))*.7,
                        .5+(dx*math.sin(angle)+dy*math.cos(angle))*.7))

def crown(center,radius,seed):
    rr=random.Random(seed);phase=rr.random()*math.tau
    verts=[];colors=[];faces=[];segments=24;rings=10
    # Overlapping branch masses, not one flat green dome or separate pointy leaves.
    clusters=[(0,0,.58,.97,.43)]
    for j in range(8):
        a=j*math.tau/8+phase+rr.uniform(-.08,.08)
        distance=rr.uniform(.55,.60);r=1.0-distance
        clusters.append((math.cos(a)*distance,math.sin(a)*distance,r,
                         rr.uniform(.82,1.00),rr.uniform(.29,.40)))
    for ci,(dx,dy,r,z,h) in enumerate(clusters):
        start=len(verts);shade=rr.uniform(-.016,.016)
        for j in range(rings+1):
            phi=math.pi*j/rings;rho=math.sin(phi);up=math.cos(phi)
            for i in range(segments):
                theta=math.tau*i/segments
                lobe=1+.045*math.sin(5*theta+phase+ci)*rho
                xx=dx+r*rho*math.cos(theta)*lobe
                yy=dy+r*rho*math.sin(theta)*lobe
                zz=z+h*up+.022*math.sin(theta*3+ci)*rho
                verts.append((center[0]+xx*radius,center[1]+yy*radius,center[2]+zz*radius))
                top=max(0,up);small=.008*math.sin(theta*7+j*2+phase)
                colors.append((.215+.044*top+shade+small,
                               .280+.060*top+shade+small,
                               .186+.035*top+shade+small*.7,1))
        for j in range(rings):
            for i in range(segments):
                a=start+j*segments+i;b=start+j*segments+(i+1)%segments;c=b+segments;d=a+segments
                faces.extend([(a,c,b),(a,d,c)])
    emit('Tree crown',verts,faces,colors)

def bush(center,radius,seed):
    rr=random.Random(seed);verts=[];faces=[];colors=[]
    for leaf in range(7):
        angle=leaf*math.tau/7+rr.uniform(-.12,.12);length=radius*rr.uniform(.68,.98);width=length*.29
        start=len(verts)
        # Broad rounded elliptical leaves, with a soft midrib and blunt tips.
        for ring in range(7):
            t=ring/6;w=width*math.sin(math.pi*t)**.65
            for side in [-1,0,1]:
                x=length*t;y=w*side;z=radius*(.14+.23*math.sin(math.pi*t)+.07*(1-abs(side)))
                verts.append((center[0]+x*math.cos(angle)-y*math.sin(angle),center[1]+x*math.sin(angle)+y*math.cos(angle),center[2]+z))
                colors.append((.22+leaf*.004,.31+leaf*.005,.15+leaf*.003,1))
        for ring in range(6):
            for side in range(2):
                a=start+ring*3+side;faces.extend([(a,a+3,a+4),(a,a+4,a+1)])
    emit('Low broadleaf',verts,faces,colors)

for i,(_,pos,r) in enumerate(sorted(trees)):crown(pos,r,19800+i)
for i,(_,pos,r) in enumerate(sorted(shrubs)):bush(pos,r,29800+i)
mat=bpy.data.materials.new('Soft broadleaf | matte authored albedo');mat.use_nodes=True
p=mat.node_tree.nodes.get('Principled BSDF');p.inputs['Roughness'].default_value=.95;p.inputs['Specular IOR Level'].default_value=.15
col=mat.node_tree.nodes.new('ShaderNodeVertexColor');col.layer_name='CROWN';mat.node_tree.links.new(col.outputs['Color'],p.inputs['Base Color'])
for key,(verts,faces,colors,uvs) in sorted(chunks.items()):
    mesh=bpy.data.meshes.new(f'Crown geometry {key}');mesh.from_pydata(verts,[],faces);mesh.update()
    attr=mesh.color_attributes.new(name='CROWN',type='FLOAT_COLOR',domain='POINT')
    attr.data.foreach_set('color',[v for c in colors for v in c]);mesh.color_attributes.active_color=attr
    uv=mesh.uv_layers.new(name='Canopy UV')
    for loop in mesh.loops:uv.data[loop.index].uv=uvs[loop.vertex_index]
    for poly in mesh.polygons:poly.use_smooth=True
    ob=bpy.data.objects.new(f'Grove198_{key[0]}_{key[1]}',mesh);scene.collection.objects.link(ob);mesh.materials.append(mat)
    ob['source']='Original mainland tree/plant coordinates; visual replacement only'
scene['trees']=len(trees);scene['shrubs']=len(shrubs);scene['collision']='Unmodified native circles and polygons'
bpy.ops.object.select_all(action='SELECT')
bpy.ops.export_scene.gltf(filepath=str(OUT/'groves.glb'),export_format='GLB',use_selection=True,export_materials='EXPORT',export_yup=True)
bpy.ops.wm.save_as_mainfile(filepath=str(ART/'Soft_Broadleaf_Groves.blend'))
manifest={'trees':len(trees),'shrubs':len(shrubs),'chunks':len(chunks),'triangles':sum(len(v[1]) for v in chunks.values()),'units':'native grid; Blender Z up; glTF Y up','treeFootprints':'Original 1.5*size*scale circles; eight peripheral branch masses; maximum scallop overshoot <=2.1% radius','generator':'tools/build_grove198.py','license':'Project-original GPL-3.0-or-later','treesSource':trees}
(ART/'geometry.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),'utf-8')
print('GROVE198',json.dumps({k:v for k,v in manifest.items() if k!='treesSource'}),flush=True)
