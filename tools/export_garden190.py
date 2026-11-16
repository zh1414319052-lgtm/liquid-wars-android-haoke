"""Convert authored Blender runtime triangles to an editable Godot glTF scene.
Geometry, scale and river footprint are preserved. No baked scene image or sea.
The upstream editable .blend and its export script stay in art/source/garden-1.89.
"""
from pathlib import Path
import json, struct
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / "app/godot/assets"
DEST.mkdir(parents=True, exist_ok=True)
raw = (ROOT / "app/src/main/assets/garden3d/live/meshes.bin").read_bytes()
assert raw[:4] == b"LWD2"
_, scale, cx, cy, design_scale, ox, oy = struct.unpack(
    "<4s6f", (ROOT / "app/src/main/assets/garden3d/live/river.bin").read_bytes())
data = bytearray()
ground = np.load(ROOT/'art/source/color-1.91/authored-ground-color.npz')
ground_xyz, ground_rgb = ground['xyz'], ground['rgb']
ground_lookup = {tuple(np.round(v,1)): i for i,v in enumerate(ground_xyz)}
ground_matches = 0
ground_max_error = 0.0
doc = {
    "asset": {"version": "2.0", "generator": "Liquid Wars Godot 1.90 / authored Blender geometry"},
    "scene": 0, "scenes": [{"nodes": []}], "nodes": [], "meshes": [],
    "buffers": [], "bufferViews": [], "accessors": [],
    "materials": [
        {"name": "Static earth and stone", "pbrMetallicRoughness": {"metallicFactor": 0, "roughnessFactor": .9}, "doubleSided": True},
        {"name": "Static foliage", "pbrMetallicRoughness": {"metallicFactor": 0, "roughnessFactor": .85}, "doubleSided": True},
        {"name": "River only", "pbrMetallicRoughness": {"metallicFactor": 0, "roughnessFactor": .42}, "doubleSided": True},
    ],
}
def accessor(a, kind, component=5126):
    a = np.ascontiguousarray(a)
    while len(data) % 4: data.append(0)
    start = len(data); data.extend(a.tobytes())
    view = len(doc["bufferViews"])
    doc["bufferViews"].append({"buffer": 0, "byteOffset": start, "byteLength": a.nbytes})
    entry = {"bufferView": view, "componentType": component, "count": len(a), "type": kind}
    if kind == "VEC3":
        entry.update(min=a.min(axis=0).tolist(), max=a.max(axis=0).tolist())
    doc["accessors"].append(entry)
    return len(doc["accessors"]) - 1

offset = 8; triangles = 0
for chunk in range(struct.unpack_from("<I", raw, 4)[0]):
    count, kind, *bounds = struct.unpack_from("<II6f", raw, offset); offset += 32
    assert kind in (0, 1, 2), "Unexpected geometry, including any sea, is rejected."
    a = np.frombuffer(raw, dtype="<f4", count=count * 11, offset=offset).reshape(-1, 11).copy()
    offset += count * 44
    # Deduplicate within spatial chunks, preserving hard normals and vertex colors.
    a, inv = np.unique(a, axis=0, return_inverse=True)
    positions = a[:, [0, 2, 1]].copy(); positions[:, 2] *= -1
    normals = a[:, [3, 5, 4]].copy(); normals[:, 2] *= -1
    colors = np.column_stack([a[:, 6:9], np.ones(len(a), dtype="<f4")]).astype("<f4")
    # The old exporter looked for 'tint'; the original mainland uses
    # 'ground_tint'. Restore its real path/grass color variation, not a decal.
    for row in np.flatnonzero(np.abs(a[:,10]-.94)<.001):
        pos = a[row,:3]
        idx = ground_lookup.get(tuple(np.round(pos,1)))
        if idx is None or np.linalg.norm(pos-ground_xyz[idx]) > .002:
            idx = np.argmin(np.sum((ground_xyz-pos)**2,axis=1))
        error = float(np.linalg.norm(pos-ground_xyz[idx]))
        assert error < .002, ('Unmatched authored ground vertex',pos,error)
        colors[row,:3] = ground_rgb[idx]
        ground_matches += 1
        ground_max_error = max(ground_max_error,error)
    # River UV follows the authored curved centerline; longitudinal phase is
    # independent of screen/camera coordinates and only exists on river meshes.
    bx = (a[:, 0] - 1632) / scale + cx
    by = (a[:, 1] - 1024) / scale + cy
    q = (by - oy) / design_scale
    center = design_scale * (14 + 3.5 * np.sin(q * .19) + 1.7 * np.sin(q * .41)) + ox
    uv = np.column_stack([bx - center, by]).astype("<f4")
    # glTF uses counter-clockwise faces; Godot importer performs its conversion.
    indices = inv.astype("<u4")
    attributes = {"POSITION": accessor(positions, "VEC3"), "NORMAL": accessor(normals, "VEC3"),
                  "COLOR_0": accessor(colors, "VEC4"), "TEXCOORD_0": accessor(uv, "VEC2")}
    # Original authored roughness identifies ground/stone/bridge/trunk, preserving
    # spatial batches and every triangle. No runtime position-based guessing.
    attributes['TEXCOORD_1'] = accessor(np.column_stack([a[:,10],np.full(len(a),kind)]).astype('<f4'), 'VEC2')
    mesh = {"primitives": [{"attributes": attributes, "indices": accessor(indices, "SCALAR", 5125), "material": kind}]}
    name = ("StaticTerrain" if kind == 0 else "StaticFoliage" if kind == 1 else "River") + f"_{chunk:03d}"
    doc["meshes"].append(mesh)
    doc["nodes"].append({"name": name, "mesh": len(doc["meshes"]) - 1})
    doc["scenes"][0]["nodes"].append(len(doc["nodes"]) - 1)
    triangles += count // 3
assert offset == len(raw)
while len(data) % 4: data.append(0)
doc["buffers"] = [{"byteLength": len(data)}]
manifest = json.dumps(doc, separators=(",", ":")).encode()
manifest += b" " * (-len(manifest) % 4)
glb = struct.pack("<4sII", b"glTF", 2, 12 + 8 + len(manifest) + 8 + len(data))
glb += struct.pack("<I4s", len(manifest), b"JSON") + manifest
glb += struct.pack("<I4s", len(data), b"BIN\0") + data
(DEST / "mainland.glb").write_bytes(glb)
print(json.dumps({"chunks": len(doc["nodes"]), "triangles": triangles, "bytes": len(glb), "sea_meshes": 0}))
print(json.dumps({'authored_ground_vertices':ground_matches,'max_coordinate_error':ground_max_error}))
