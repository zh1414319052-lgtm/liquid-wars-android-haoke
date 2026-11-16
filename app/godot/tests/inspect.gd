extends SceneTree
func _initialize():
    var scene = load("res://assets/mainland.glb").instantiate()
    for n in scene.get_children():
        if n is MeshInstance3D:
            var a = n.mesh.surface_get_arrays(0)
            var m = n.mesh.surface_get_material(0)
            print(n.name," material=",m," vertex_color=",m.vertex_color_use_as_albedo," base=",m.albedo_color," color=",a[Mesh.ARRAY_COLOR][0]," position=",a[0][0]," norm=",a[1][0])
            if str(n.name).begins_with("River_"):break
    scene.free()
    quit()
