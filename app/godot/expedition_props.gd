class_name ExpeditionProps extends Node3D
## Original Blender models. State is supplied by C++; removed obstacles leave no collision/render ghost.
const ASSETS = [preload("res://assets/expedition/freeze.glb"),preload("res://assets/expedition/cleanse.glb"),
 preload("res://assets/expedition/teleport.glb"),preload("res://assets/expedition/anchor.glb"),
 preload("res://assets/expedition/devour.glb"),preload("res://assets/expedition/fortify.glb"),
 preload("res://assets/expedition/death.glb"),preload("res://assets/expedition/life.glb"),
 preload("res://assets/expedition/coin.glb"),preload("res://assets/expedition/shoes.glb"),
 preload("res://assets/expedition/armor.glb"),preload("res://assets/expedition/site_production.glb"),
 preload("res://assets/expedition/site_attack.glb"),preload("res://assets/expedition/obstacle_rock.glb"),
 preload("res://assets/expedition/obstacle_log.glb")]
var pool: Dictionary = {}
var seen: Dictionary = {}
var automatic_icons: Array[Texture2D] = []
var last_epoch := -1
func _ready() -> void:
 for k in 20:
  automatic_icons.append(load("res://assets/classic-items/item_%02d.svg" % k))
func update_frame(frame: PackedFloat32Array, offset: int, count: int, seconds: float) -> void:
 if int(frame[2]) != last_epoch:
  for node in pool.values():
   node.queue_free()
  pool.clear()
  last_epoch=int(frame[2])
 seen.clear()
 for i in count:
  var at := offset+i*10
  var kind := int(frame[at])
  var id := int(frame[at+1])
  if kind < 0 or (kind >= ASSETS.size() and (kind < 20 or kind >= 40)):
   continue
  seen[id] = true
  if not pool.has(id):
   var fresh: Node3D
   if kind >= 20:
    var sprite := Sprite3D.new()
    sprite.texture = automatic_icons[kind-20]
    sprite.pixel_size = 0.035
    sprite.rotation_degrees.x = -90
    sprite.no_depth_test = false
    fresh = Node3D.new()
    fresh.add_child(sprite)
   else:
    fresh = ASSETS[kind].instantiate()
   fresh.set_meta("kind",kind)
   add_child(fresh)
   pool[id] = fresh
  var node: Node3D = pool[id]
  node.position = Vector3(frame[at+2],frame[at+3],frame[at+4])
  node.scale = Vector3.ONE*frame[at+5]
  if kind < 9 or kind >= 20:
   # Authoritative HP controls erosion. Idling never changes size or the ground.
   var remaining := clampf(frame[at+8],0,1)
   node.scale *= .35+.65*sqrt(remaining)
   node.position.y += .6 + .12*sin(seconds*1.8+id)
   node.rotation.y = seconds*.25+id
  else:
   node.rotation.y = id*0.77
  if kind == 13 or kind == 14:
   # Small, local overlay indicates a real fortification. It never changes the ground shader.
   _ward(node,frame[at+7] > 0.5)
 for id in pool.keys():
  if not seen.has(id):
   pool[id].queue_free()
   pool.erase(id)
func _ward(node: Node, enabled: bool) -> void:
 if node.get_meta("ward",false) == enabled:
  return
 node.set_meta("ward",enabled)
 var overlay: StandardMaterial3D
 if enabled:
  overlay = StandardMaterial3D.new()
  overlay.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
  overlay.albedo_color = Color(.5,.75,.9,.23)
 _apply_overlay(node,overlay)
func _apply_overlay(node: Node, material: Material) -> void:
 if node is MeshInstance3D:
  node.material_overlay = material
 for child in node.get_children():
  _apply_overlay(child,material)
