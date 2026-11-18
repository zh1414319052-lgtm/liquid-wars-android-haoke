extends SceneTree
## Diagnostic shader swatch only: authored scene with synthetic, labelled test positions.
func _initialize() -> void:
    call_deferred("run_preview")
func run_preview() -> void:
    var battle=load("res://battle.tscn").instantiate()
    root.add_child(battle)
    await process_frame
    battle.camera.position=Vector3(1550,250,-960)
    battle.camera.size=150
    battle._set_sample_origin(Vector2(1550,960))
    battle.particles.instance_count=800
    var heights=FileAccess.open("D:/HaokeBuild/liquidwars-widescreen/app/src/main/assets/garden3d/height.bin",FileAccess.READ)
    for n in 800:
        var x=1510.0+(n%32)*1.1
        var y=950.0+(n/32)*1.1
        heights.seek(12+(int(y)*3264+int(x))*4)
        var z=heights.get_float()+0.13
        var t=Transform3D(Basis().scaled(Vector3(0.44,0.4,0.44)),Vector3(x,z,-y))
        battle.particles.set_instance_transform(n,t)
        battle.particles.set_instance_color(n,Color(1,1,1,0.32/16.0))
    battle.particles.visible_instance_count=800
    battle.gravity_node.visible=true
    battle.gravity_material.set_shader_parameter("affected_mask",1)
    battle.gravity_material.set_shader_parameter("pixel_world",150.0/720)
    for i in 30:await process_frame
    await RenderingServer.frame_post_draw
    root.get_texture().get_image().save_png("D:/HaokeBuild/liquidwars-widescreen/docs/game/black-hole-2.07/shader-probe.png")
    print("GRAVITY_SHADER_PROBE_COMPLETE synthetic test positions; not gameplay evidence")
    quit()
