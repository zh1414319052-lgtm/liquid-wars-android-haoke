extends SceneTree

func _initialize() -> void:
    call_deferred("_run")

func _run() -> void:
    var battle = load("res://battle.tscn").instantiate()
    root.add_child(battle)
    await process_frame
    var rivers := 0
    var static_meshes := 0
    var transforms := {}
    for node in battle.get_node("Mainland").get_children():
        if not node is MeshInstance3D:
            continue
        assert(not "Sea" in str(node.name) and not "Ocean" in str(node.name))
        if str(node.name).begins_with("River_"):
            rivers += 1
            assert(node.material_override == battle.river_material)
            assert(node.mesh.get_aabb().size.y < 0.01)
        else:
            static_meshes += 1
            transforms[node] = node.transform
            assert(node.material_override in [battle.terrain_material,battle.foliage_material])
            for i in node.mesh.get_surface_count():
                assert(node.mesh.surface_get_arrays(i)[Mesh.ARRAY_COLOR].size() > 0)
                assert(node.mesh.surface_get_arrays(i)[Mesh.ARRAY_TEX_UV2].size() > 0)
    assert(rivers > 0 and static_meshes > 100)
    battle.river_material.set_shader_parameter("game_time",7.5)
    for frame in 6:
        await process_frame
        assert(is_equal_approx(battle.river_material.get_shader_parameter("game_time"),7.5))
        for node in transforms:
            assert(node.transform == transforms[node])
    assert(battle.camera.projection == Camera3D.PROJECTION_ORTHOGONAL)
    assert(battle.camera.global_basis.z.dot(Vector3.UP) > 0.999)
    assert(battle.get_node("Sun").shadow_enabled)
    battle._set_reference(true)
    assert(is_equal_approx(battle.daylight.ambient_light_energy,0.4))
    battle._set_reference(false)
    assert(battle.daylight.ambient_light_energy > 0.4)
    print("static_meshes=",static_meshes," river_meshes=",rivers,
        " authored_colors=on sea=0 static_transforms=stable paused_shader_clock=stable")
    print("GAME_TEST_PASS")
    quit()
