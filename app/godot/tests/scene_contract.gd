extends SceneTree

func _initialize() -> void:
    call_deferred("_run")

func _run() -> void:
    var battle = load("res://battle.tscn").instantiate()
    root.add_child(battle)
    await process_frame
    assert(battle.gravity_node.multimesh == battle.particles)
    assert(not battle.gravity_node.visible)
    assert(battle.gravity_node.cast_shadow == GeometryInstance3D.SHADOW_CASTING_SETTING_OFF)
    assert(battle.gravity_material.shader.resource_path == "res://gravity.gdshader")
    for screen in [Vector2(2340,1080),Vector2(1920,1080),Vector2(1600,1200)]:
        assert(is_equal_approx(battle._particle_scale(480,screen),0.75))
        var classic := maxf(2.25,minf(screen.x,screen.y*2.2)/800.0*1.28+0.7)
        var near_body: float=2.72*0.45*(5.0/6.8)*screen.x/200.0*battle._particle_scale(200,screen)
        assert(near_body <= classic+0.001)
        assert(is_equal_approx(battle._particle_scale(3264,screen),0.75))
    assert(is_equal_approx(battle._particle_scale(0,Vector2.ZERO),0.75))
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
    var grove_meshes := 0
    for node in battle.groves.get_children():
        if not node is MeshInstance3D:
            continue
        grove_meshes += 1
        transforms[node] = node.transform
        assert(node.material_override == battle.foliage_material)
        for i in node.mesh.get_surface_count():
            var arrays = node.mesh.surface_get_arrays(i)
            assert(arrays[Mesh.ARRAY_COLOR].size() > 0)
            assert(arrays[Mesh.ARRAY_TEX_UV].size() == arrays[Mesh.ARRAY_VERTEX].size())
            assert(arrays[Mesh.ARRAY_NORMAL].size() == arrays[Mesh.ARRAY_VERTEX].size())
        var box = node.global_transform * node.mesh.get_aabb()
        assert(box.position.x > -100 and box.end.x < 3364)
        assert(box.position.z > -2148 and box.end.z < 100)
    assert(grove_meshes > 0 and grove_meshes < 60)
    for node in battle.get_node("Mainland").get_children():
        if str(node.name).begins_with("StaticFoliage"):
            assert(not node.visible)
    assert(not battle.sample_bound)
    battle._set_sample_origin(Vector2(1540,950))
    assert(battle.sample_bound)
    assert(battle.terrain_material.get_shader_parameter("sample_origin")==Vector2(1540,950))
    assert(battle.foliage_material.get_shader_parameter("sample_origin")==Vector2(1540,950))
    battle.camera.position+=Vector3(300,0,-100)
    battle.river_material.set_shader_parameter("game_time",7.5)
    for frame in 6:
        await process_frame
        assert(is_equal_approx(battle.river_material.get_shader_parameter("game_time"),7.5))
        for node in transforms:
            assert(node.transform == transforms[node])
        assert(battle.terrain_material.get_shader_parameter("sample_origin")==Vector2(1540,950))
    assert(battle.camera.projection == Camera3D.PROJECTION_ORTHOGONAL)
    assert(battle.camera.global_basis.z.dot(Vector3.UP) > 0.999)
    assert(battle.get_node("Sun").shadow_enabled)
    battle._set_reference(true)
    assert(not battle.groves.visible)
    assert(is_equal_approx(battle.daylight.ambient_light_energy,0.4))
    battle._set_reference(false)
    assert(battle.groves.visible)
    assert(battle.daylight.ambient_light_energy > 0.4)
    print("static_meshes=",static_meshes," grove_meshes=",grove_meshes," river_meshes=",rivers,
        " authored_colors=on sea=0 static_transforms=stable paused_shader_clock=stable")
    print("GAME_TEST_PASS")
    quit()
