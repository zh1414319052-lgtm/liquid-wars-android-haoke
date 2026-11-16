"""Read authored ground_tint from the original Blender source, without saving it.
Run in a new Blender --background --factory-startup process, not a live editor.
Only exports a color sidecar; does not regenerate collision or geometry.
"""
from pathlib import Path
import bpy, json, numpy as np
assert bpy.app.background and not bpy.data.filepath
ROOT = Path(__file__).resolve().parents[1]
src = ROOT / 'art/source/garden-1.88'
bpy.ops.wm.open_mainfile(filepath=str(src/'十倍新场景_溪谷原野.blend'), load_ui=False)
cfg = json.loads((src/'world.json').read_text())
ob = next(o for o in bpy.data.objects if o.name.startswith('Mainland |'))
attr = ob.data.color_attributes['ground_tint']
assert attr.domain == 'POINT'
points = np.array([ob.matrix_world @ v.co for v in ob.data.vertices])
points[:,0] = (points[:,0]-cfg['center'][0])*cfg['scale'] + cfg['width']/2
points[:,1] = (points[:,1]-cfg['center'][1])*cfg['scale'] + cfg['height']/2
points[:,2] *= cfg['scale']
rgb = np.array([v.color[:3] for v in attr.data], dtype=np.float32)
out = ROOT/'art/source/color-1.91'
out.mkdir(exist_ok=True)
np.savez_compressed(out/'authored-ground-color.npz', xyz=points.astype(np.float32), rgb=rgb)
print('AUTHORED_GROUND_COLOR',len(points),'linear_rgb_range',rgb.min(),rgb.max())
