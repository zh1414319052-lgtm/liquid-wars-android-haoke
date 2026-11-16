extends Node3D
## Scene, shading and presentation only. All units/orders/rules come from C++.
const TEAM_COLORS = [Color(0.008,0.40,0.27),Color(0.08,0.23,0.65),
    Color(0.80,0.14,0.065),Color(0.70,0.67,0.52),Color(0.72,0.38,0.025),Color(0.33,0.10,0.5),Color(.7,.18,.34),Color(.18,.30,.4),Color(.32,.5,.08),Color(.45,.22,.1)]
var props := ExpeditionProps.new()
var effects := BattleVfx.new()
var bridge: Object
var particles: MultiMesh
var particle_node: MultiMeshInstance3D
var river_material: ShaderMaterial
var site_nodes: Array[MeshInstance3D] = []
var site_materials: Array[StandardMaterial3D] = []
var frame_count := 0
var capacity := 0
var timing_sum := 0.0
var timing_max := 0.0
var frame_samples := 0
var daylight: Environment
var terrain_material: ShaderMaterial
var foliage_material: ShaderMaterial
var particle_material: ShaderMaterial
var reference_190 := false
var perf_rows := PackedStringArray()
var perf_previous_us := 0
@onready var camera: Camera3D = $Camera

func _ready() -> void:
    RenderingServer.viewport_set_measure_render_time(get_viewport().get_viewport_rid(),true)
    var environment := Environment.new()
    environment.background_mode = Environment.BG_COLOR
    environment.background_color = Color(0.16,0.20,0.19)
    environment.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
    environment.ambient_light_color = Color(0.78,0.87,0.90)
    environment.ambient_light_energy = 0.4
    environment.tonemap_mode = Environment.TONE_MAPPER_LINEAR
    daylight = environment
    var world := WorldEnvironment.new()
    world.environment = environment
    add_child(world)
    river_material = ShaderMaterial.new()
    river_material.shader = preload("res://river.gdshader")
    terrain_material = ShaderMaterial.new()
    terrain_material.shader = preload("res://surface.gdshader")
    foliage_material = ShaderMaterial.new()
    foliage_material.shader = preload("res://surface.gdshader")
    foliage_material.set_shader_parameter("foliage",true)
    _bind_river($Mainland)
    var sphere := PlaneMesh.new()
    # Classic sprite: 5px body plus .9px gutter. Visible radius stays unchanged.
    sphere.size = Vector2(2.72, 2.72)
    particle_material = ShaderMaterial.new()
    particle_material.shader = preload("res://particles.gdshader")
    particle_material.set_shader_parameter("classic_atlas",preload("res://assets/classic-particles.png"))
    sphere.material = particle_material
    particles = MultiMesh.new()
    particles.transform_format = MultiMesh.TRANSFORM_3D
    particles.use_colors = true
    particles.mesh = sphere
    # Bounds are known from the authored world. Automatic MultiMesh bounds may
    # read back a freshly uploaded buffer in Compatibility/emulated GLES.
    # Native packet culling still submits every visible particle; this changes
    # no geometry, particle count, shadow setting, resolution or camera scale.
    particles.custom_aabb = AABB(Vector3(-2,-2,-2050),Vector3(3268,128,2052))
    particle_node = MultiMeshInstance3D.new()
    particle_node.multimesh = particles
    # Shared instanced geometry receives and casts real contact shadows.
    particle_node.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_ON
    add_child(particle_node)
    add_child(props)
    add_child(effects)
    _set_reference(false)
    if Engine.has_singleton("LiquidBattle"):
        bridge = Engine.get_singleton("LiquidBattle")
        bridge.ready()
        print("LW_GODOT_READY ",Engine.get_version_info().string," sea=0 land=static")
    else:
        print("LW_GODOT_ASSET_PREVIEW_ONLY: no native simulation bridge")

func _bind_river(node: Node) -> void:
    if node is MeshInstance3D and str(node.name).begins_with("River_"):
        node.material_override = river_material
        node.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
    elif node is MeshInstance3D:
        node.material_override = foliage_material if str(node.name).begins_with("StaticFoliage") else terrain_material
    for child in node.get_children():
        _bind_river(child)

func _set_reference(enabled: bool) -> void:
    reference_190 = enabled
    for mat in [terrain_material,foliage_material,particle_material,river_material]:
        mat.set_shader_parameter("reference_190",enabled)
    daylight.ambient_light_color = Color(0.78,0.87,0.90) if enabled else Color(0.87,0.94,1.0)
    daylight.ambient_light_energy = 0.4 if enabled else 0.50
    $Sun.light_color = Color(1,0.96,0.87) if enabled else Color(1,0.98,0.94)
    $Sun.light_energy = 0.8 if enabled else 0.65
    $Sun.shadow_opacity = 1.0 if enabled else 0.72
    print("LW_COLOR_PROFILE reference_190=",enabled," camera=unchanged radius=unchanged")

func _create_site() -> void:
    var mesh := CylinderMesh.new()
    mesh.top_radius = 4.8
    mesh.bottom_radius = 6.0
    mesh.height = 2.0
    mesh.radial_segments = 12
    var material := StandardMaterial3D.new()
    material.roughness = 0.75
    var node := MeshInstance3D.new()
    node.mesh = mesh
    node.material_override = material
    add_child(node)
    site_nodes.append(node)
    site_materials.append(material)

func _process(delta: float) -> void:
    if not is_instance_valid(bridge):
        return
    var started_us := Time.get_ticks_usec()
    var frame: PackedFloat32Array = bridge.frame()
    var bridge_us := Time.get_ticks_usec()
    if frame.size() < 16:
        particles.visible_instance_count = 0
        return
    var count := int(frame[0])
    var sites := int(frame[3])
    Engine.max_fps = int(frame[9])
    if frame.size() != 16 + count * 16 + sites * 8 + int(frame[14])*10:
        push_error("Invalid native presentation frame")
        return
    # Reserved presentation header only; release hosts always leave this zero.
    if reference_190 != (frame[10] > 0.5):
        _set_reference(frame[10] > 0.5)
    var needed := maxi(count,int(frame[15]))
    if needed > capacity:
        capacity = maxi(1024, int(pow(2,ceil(log(float(needed))/log(2.0)))))
        particles.instance_count = capacity
    if count > 0:
        var transforms := frame.slice(16,16+count*16)
        transforms.resize(capacity*16)
        particles.buffer = transforms
    particles.visible_instance_count = count
    var submitted_us := Time.get_ticks_usec()
    camera.position = Vector3(frame[4]+frame[6]*0.5,250,-frame[5]-frame[7]*0.5)
    camera.size = frame[7]
    river_material.set_shader_parameter("game_time",frame[1])
    while site_nodes.size() < sites:
        _create_site()
    for i in site_nodes.size():
        site_nodes[i].visible = i < sites and frame[14] == 0
        if i >= sites:
            continue
        var at := 16+count*16+i*8
        var owner := int(frame[at+3])
        site_nodes[i].position = Vector3(frame[at],frame[at+1]+1,frame[at+2])
        site_materials[i].albedo_color = TEAM_COLORS[owner] if owner >= 0 else Color(0.48,0.51,0.37)
        # Pulse is caused only by a real spawn event in the original economy.
        var pulse := clampf(1.0-frame[at+7]/450.0,0,1)
        site_materials[i].emission_enabled = pulse > 0
        site_materials[i].emission = site_materials[i].albedo_color * pulse * 0.25
    props.update_frame(frame,16+count*16+sites*8,int(frame[14]),frame[1])
    effects.update_frame(frame,16+count*16+sites*8,int(frame[14]),frame[1])
    frame_count += 1
    # Raw frame intervals include stalls; Engine delta may be smoothed/clamped.
    # Batch logging avoids a logcat call for every rendered frame.
    if perf_previous_us > 0:
        perf_rows.append("%d,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%d,%d,%.3f,%.3f,%.3f" % [frame_count,count,
            (started_us-perf_previous_us)/1000.0,(bridge_us-started_us)/1000.0,
            frame[11],frame[12],(submitted_us-bridge_us)/1000.0,int(frame[6]),int(frame[7]),
            RenderingServer.get_frame_setup_time_cpu(),
            RenderingServer.viewport_get_measured_render_time_cpu(get_viewport().get_viewport_rid()),
            RenderingServer.viewport_get_measured_render_time_gpu(get_viewport().get_viewport_rid())])
    perf_previous_us = started_us
    if perf_rows.size() >= 10:
        print("LW_PERF192 ",";".join(perf_rows))
        perf_rows.clear()
    if frame_count > 120:
        frame_samples += 1
        timing_sum += delta*1000.0
        timing_max = maxf(timing_max,delta*1000.0)
    if frame_count % 300 == 0:
        print("LW_GODOT_METRICS frames=",frame_count," visible=",count," fps=",Engine.get_frames_per_second(),
            " mean_ms=",timing_sum/maxi(1,frame_samples)," max_ms=",timing_max,
            " objects=",Performance.get_monitor(Performance.RENDER_TOTAL_OBJECTS_IN_FRAME),
            " primitives=",Performance.get_monitor(Performance.RENDER_TOTAL_PRIMITIVES_IN_FRAME))
